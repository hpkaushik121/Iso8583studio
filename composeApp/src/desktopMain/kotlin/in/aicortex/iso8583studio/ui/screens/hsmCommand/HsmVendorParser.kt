package `in`.aicortex.iso8583studio.ui.screens.hsmCommand

import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HeaderFormat
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmVendorType

/**
 * Vendor-aware decoding of HSM console traffic.
 *
 * Every request and response that passes through the console is run through the parser for the
 * vendor selected in `HsmCommandConfig.hsmVendor`, so the Formatted / Parsed views show the wire
 * broken down the way that vendor actually frames and delimits it.
 *
 * Depth differs per vendor and that is deliberate:
 * - **Thales payShield** is fully semantic — it reuses [thalesCommandDefinitions], so every field
 *   is named, dropdown values are labelled, key blocks are decoded and error codes described.
 * - **Every other vendor** is decoded *structurally* only (framing, command code, delimiters,
 *   tags, positional fields). This project carries no field dictionary for those protocols, so
 *   fields are labelled by position/tag rather than guessed at. Populate the per-vendor tables in
 *   this file as authoritative specs become available.
 */
interface HsmVendorParser {
    val vendor: HsmVendorType

    /** Decodes a full outbound frame (framing bytes included). */
    fun parseRequest(rawFrame: ByteArray, framing: HsmFraming): ParsedHsmMessage

    /**
     * Decodes a full inbound frame (framing bytes included). [request] is the already-parsed
     * request it answers, when available — Thales needs it to pick the right command definition.
     */
    fun parseResponse(
        rawFrame: ByteArray,
        framing: HsmFraming,
        request: ParsedHsmMessage? = null,
    ): ParsedHsmMessage

    /**
     * Success/error status of a response body (framing and message header already stripped), or
     * `null` when this vendor's status encoding is not known — in which case callers must not
     * infer failure from the payload.
     */
    fun statusOf(responseBody: String): HsmStatus?
}

/**
 * Wire framing needed to strip a frame before its payload can be decoded. Every value comes from
 * the HSM connection settings — none of it is inferred from the vendor.
 *
 * [messageHeader] is the header this end *sends*; [messageHeaderLength] is how many characters a
 * header occupies on the wire, which is what a response echoing a different header is measured by.
 */
data class HsmFraming(
    val headerFormat: HeaderFormat,
    val tcpLengthEnabled: Boolean,
    val messageHeader: String,
    val messageHeaderLength: Int = messageHeader.length,
    val trailer: String = "",
)

/** Command status decoded from a response. */
data class HsmStatus(
    val code: String,
    val description: String,
    val success: Boolean,
)

/**
 * A decoded request or response.
 *
 * [fields] reuses [ThalesCommandField] as the generic field descriptor so all vendors render
 * through the same Parsed-fields table; [formatted] is the BP-Tools style text shown in the
 * Formatted panels.
 */
data class ParsedHsmMessage(
    val commandCode: String,
    val commandName: String,
    val fields: List<Pair<ThalesCommandField, String>>,
    val formatted: String,
    val status: HsmStatus? = null,
)

object HsmVendorParsers {
    private val genericParsers = HsmVendorType.entries.associateWith { GenericAsciiParser(it) }

    fun forVendor(vendor: HsmVendorType): HsmVendorParser = when (vendor) {
        HsmVendorType.THALES_PAYSHIELD -> ThalesPayShieldParser
        HsmVendorType.FUTUREX -> FuturexExcryptParser
        HsmVendorType.ATALLA -> AtallaParser
        else -> genericParsers.getValue(vendor)
    }
}

/** Which side of an exchange a captured message belongs to. */
enum class HsmMessageDirection { REQUEST, RESPONSE }

/**
 * Guesses whether [rawFrame] is a request or a response, for pasted traffic that carries no
 * context. Returns `null` when it cannot be told apart, which is the honest answer for every
 * vendor except Thales — only payShield has disjoint command and response code sets to match on.
 */
fun detectDirection(
    vendor: HsmVendorType,
    rawFrame: ByteArray,
    framing: HsmFraming,
): HsmMessageDirection? {
    if (vendor != HsmVendorType.THALES_PAYSHIELD) return null
    val payload = splitFrame(rawFrame, framing).payload
    if (payload.length < 2) return null
    val code = payload.substring(0, 2)
    return when {
        thalesCommandDefinitionMap.containsKey(code) -> HsmMessageDirection.REQUEST
        thalesCommandDefinitions.any { it.responseCode == code } -> HsmMessageDirection.RESPONSE
        else -> null
    }
}

// ─────────────────────────────────────────────────────────
//  THALES payShield
// ─────────────────────────────────────────────────────────

/**
 * payShield host commands are plain ASCII: `[2-char command][fields...]`, answered by
 * `[2-char response code][2-char error code][fields...]`. Field layouts come from
 * [thalesCommandDefinitions]; unknown command codes fall back to a code + data split.
 */
object ThalesPayShieldParser : HsmVendorParser {
    override val vendor = HsmVendorType.THALES_PAYSHIELD

    override fun parseRequest(rawFrame: ByteArray, framing: HsmFraming): ParsedHsmMessage {
        val (framingLines, payload, observedHeader) = splitFrame(rawFrame, framing)
        if (payload.length < 2) return rawMessage(framingLines, payload, isRequest = true)

        val code = payload.substring(0, 2)
        val definition = thalesCommandDefinitionMap[code]
            ?: return unknownCommand(framingLines, code, payload.substring(2), isRequest = true)

        val fields = ThalesWireBuilder.parseRequestFields(definition, payload)
        val formatted = ThalesWireBuilder.formatRequestBpStyle(
            definition,
            fields.associate { (field, value) -> field.id to value },
            rawFrame,
            framing.tcpLengthEnabled,
            observedHeader,
        )
        return ParsedHsmMessage(code, definition.name, fields, formatted)
    }

    override fun parseResponse(
        rawFrame: ByteArray,
        framing: HsmFraming,
        request: ParsedHsmMessage?,
    ): ParsedHsmMessage {
        val (framingLines, payload, observedHeader) = splitFrame(rawFrame, framing)
        if (payload.length < 2) return rawMessage(framingLines, payload, isRequest = false)

        val responseCode = payload.substring(0, 2)
        val status = statusOf(payload)
        // Prefer the definition of the command that was actually sent; a response code alone is
        // ambiguous for error responses that echo a generic code.
        val definition = request?.commandCode?.let { thalesCommandDefinitionMap[it] }
            ?: thalesCommandDefinitions.find { it.responseCode == responseCode }

        if (definition == null) {
            val msg = unknownCommand(framingLines, responseCode, payload.drop(2), isRequest = false)
            return msg.copy(status = status)
        }

        val fields = ThalesWireBuilder.parseResponseFields(
            definition,
            payload,
            requestFieldValues = request?.fields?.associate { (f, v) -> f.id to v }.orEmpty(),
        )
        val formatted = ThalesWireBuilder.formatResponseBpStyle(
            definition, fields, rawFrame, framing.tcpLengthEnabled, observedHeader,
        )
        return ParsedHsmMessage(responseCode, definition.name, fields, formatted, status)
    }

    override fun statusOf(responseBody: String): HsmStatus? {
        if (responseBody.length < 4) return null
        val code = responseBody.substring(2, 4)
        return HsmStatus(code, ThalesErrorCodes.getDescription(code), ThalesErrorCodes.isSuccess(code))
    }
}

// ─────────────────────────────────────────────────────────
//  FUTUREX EXCRYPT
// ─────────────────────────────────────────────────────────

/**
 * Excrypt frames a command between STX/ETX (often printed as `[` and `]`) and delimits fields
 * with `;`, each field being a 2-char tag followed by its value.
 *
 * Tag meanings are vendor-specific and not shipped with this project, so tags are reported
 * verbatim; add confirmed entries to [tagNames] to have them named instead.
 */
object FuturexExcryptParser : HsmVendorParser {
    override val vendor = HsmVendorType.FUTUREX

    /** Excrypt field tag → human name. Populate from the Excrypt Universal command manual. */
    private val tagNames: Map<String, String> = emptyMap()

    override fun parseRequest(rawFrame: ByteArray, framing: HsmFraming) =
        decode(rawFrame, framing, isRequest = true)

    override fun parseResponse(rawFrame: ByteArray, framing: HsmFraming, request: ParsedHsmMessage?) =
        decode(rawFrame, framing, isRequest = false)

    /** Excrypt signals errors in a tag whose meaning is not documented here. */
    override fun statusOf(responseBody: String): HsmStatus? = null

    private fun decode(rawFrame: ByteArray, framing: HsmFraming, isRequest: Boolean): ParsedHsmMessage {
        val (framingLines, framed) = splitFrame(rawFrame, framing)
        val payload = stripBrackets(framed)
        if (payload.isEmpty()) return rawMessage(framingLines, payload, isRequest)

        // The console's own command list uses mnemonics longer than Excrypt's 2-char codes, so
        // match those first and only then fall back to the protocol's own code width.
        val known = matchKnownCommand(vendor, payload)
        val code = known?.code ?: payload.take(2)
        val rest = payload.drop(code.length)

        val fields = rest.split(';')
            .filter { it.isNotEmpty() }
            .mapIndexed { index, token ->
                if (token.length >= 2) {
                    val tag = token.take(2)
                    fieldOf(
                        id = "tag_${tag}_$index",
                        name = tagNames[tag] ?: "Tag $tag",
                        value = token.drop(2),
                    )
                } else {
                    fieldOf(id = "token_$index", name = "Token ${index + 1}", value = token)
                }
            }

        return ParsedHsmMessage(
            commandCode = code,
            commandName = known?.name.orEmpty(),
            fields = fields,
            formatted = formatBpStyle(framingLines, code, known?.name.orEmpty(), fields, isRequest),
        )
    }

    private fun stripBrackets(s: String): String =
        s.stripStxEtx().removeSurrounding("[", "]")
}

// ─────────────────────────────────────────────────────────
//  UTIMACO ATALLA
// ─────────────────────────────────────────────────────────

/**
 * Atalla commands are `#`-delimited positional tokens inside STX/ETX (sometimes written with
 * `<`/`>`): the first token is the command number, the rest are positional parameters.
 */
object AtallaParser : HsmVendorParser {
    override val vendor = HsmVendorType.ATALLA

    override fun parseRequest(rawFrame: ByteArray, framing: HsmFraming) =
        decode(rawFrame, framing, isRequest = true)

    override fun parseResponse(rawFrame: ByteArray, framing: HsmFraming, request: ParsedHsmMessage?) =
        decode(rawFrame, framing, isRequest = false)

    /** Atalla reports failures as dedicated response codes, which are not tabulated here. */
    override fun statusOf(responseBody: String): HsmStatus? = null

    private fun decode(rawFrame: ByteArray, framing: HsmFraming, isRequest: Boolean): ParsedHsmMessage {
        val (framingLines, framed) = splitFrame(rawFrame, framing)
        val payload = framed.stripStxEtx().removeSurrounding("<", ">")
        if (payload.isEmpty()) return rawMessage(framingLines, payload, isRequest)

        val known = matchKnownCommand(vendor, payload)
        val code = known?.code ?: (payload.substringBefore('#') + "#")
        val rest = payload.drop(code.length).split('#').dropLastWhile { it.isEmpty() }

        val fields = rest.mapIndexed { index, token ->
            fieldOf(id = "param_$index", name = "Parameter ${index + 1}", value = token)
        }

        return ParsedHsmMessage(
            commandCode = code,
            commandName = known?.name.orEmpty(),
            fields = fields,
            formatted = formatBpStyle(framingLines, code, known?.name.orEmpty(), fields, isRequest),
        )
    }
}

// ─────────────────────────────────────────────────────────
//  LUNA / UTIMACO / NCIPHER / GENERIC
// ─────────────────────────────────────────────────────────

/**
 * Fallback decoder for vendors whose console traffic is an opaque ASCII command word followed by
 * data. It reports the framing, the command word (matched against the vendor's command list when
 * possible) and the remaining payload — no field-level guessing.
 */
class GenericAsciiParser(override val vendor: HsmVendorType) : HsmVendorParser {

    override fun parseRequest(rawFrame: ByteArray, framing: HsmFraming) =
        decode(rawFrame, framing, isRequest = true)

    override fun parseResponse(rawFrame: ByteArray, framing: HsmFraming, request: ParsedHsmMessage?) =
        decode(rawFrame, framing, isRequest = false)

    override fun statusOf(responseBody: String): HsmStatus? = null

    private fun decode(rawFrame: ByteArray, framing: HsmFraming, isRequest: Boolean): ParsedHsmMessage {
        val (framingLines, framed) = splitFrame(rawFrame, framing)
        val payload = framed.stripStxEtx()
        if (payload.isEmpty()) return rawMessage(framingLines, payload, isRequest)

        val known = matchKnownCommand(vendor, payload)
        val code = known?.code ?: payload.takeWhile { !it.isWhitespace() && it != ';' && it != ',' }.take(8)
        val data = payload.drop(code.length)

        val fields = if (data.isEmpty()) emptyList()
        else listOf(fieldOf(id = "data", name = "Data", value = data))

        return ParsedHsmMessage(
            commandCode = code,
            commandName = known?.name.orEmpty(),
            fields = fields,
            formatted = formatBpStyle(framingLines, code, known?.name.orEmpty(), fields, isRequest),
        )
    }
}

// ─────────────────────────────────────────────────────────
//  SHARED HELPERS
// ─────────────────────────────────────────────────────────

private const val STX = 0x02.toByte()
private const val ETX = 0x03.toByte()
private const val LABEL_WIDTH = 25

/** A frame split into the framing it carried and the ASCII payload inside it. */
internal data class FrameParts(
    val framingLines: List<Pair<String, String>>,
    val payload: String,
    /** The header actually present on the wire, which a response may echo differently. */
    val messageHeader: String = "",
)

/**
 * Strips length/STX-ETX framing, the message header and the trailer from [raw], returning both
 * the payload and a description of everything that was removed.
 *
 * The message header is stripped by *length* rather than by content, because a device may echo a
 * header that differs from the configured one.
 */
internal fun splitFrame(raw: ByteArray, framing: HsmFraming): FrameParts {
    if (raw.isEmpty()) return FrameParts(emptyList(), "")

    val lines = mutableListOf<Pair<String, String>>()
    var body = raw

    when (framing.headerFormat) {
        HeaderFormat.TWO_BYTE_LENGTH -> if (framing.tcpLengthEnabled && body.size >= 2) {
            val len = ((body[0].toInt() and 0xFF) shl 8) or (body[1].toInt() and 0xFF)
            lines += "TCP/IP Header" to "*[%04X] %d Bytes".format(len, len)
            body = body.copyOfRange(2, body.size)
        }
        HeaderFormat.FOUR_BYTE_ASCII_LENGTH -> if (body.size >= 4) {
            val lenStr = String(body, 0, 4, Charsets.US_ASCII)
            lines += "Length Header" to "*[$lenStr] ${lenStr.trim().toIntOrNull() ?: 0} Bytes"
            body = body.copyOfRange(4, body.size)
        }
        HeaderFormat.STX_ETX -> {
            var start = 0
            var end = body.size
            if (body[0] == STX) { start = 1; lines += "STX" to "[02]" }
            if (end > start && body[end - 1] == ETX) { end -= 1; lines += "ETX" to "[03]" }
            body = body.copyOfRange(start, end)
        }
        HeaderFormat.NONE, HeaderFormat.CUSTOM -> Unit
    }

    var ascii = String(body, Charsets.US_ASCII)

    // Outbound frames carry exactly the configured header, so match it verbatim; a response may
    // echo a different value, in which case fall back to the configured header length.
    val headerLength = if (framing.messageHeader.isNotEmpty() && ascii.startsWith(framing.messageHeader)) {
        framing.messageHeader.length
    } else {
        framing.messageHeaderLength.coerceAtLeast(0)
    }
    var observedHeader = ""
    if (headerLength > 0 && ascii.length >= headerLength) {
        observedHeader = ascii.substring(0, headerLength)
        lines += "Message Header" to "[$observedHeader]"
        ascii = ascii.substring(headerLength)
    }

    if (framing.trailer.isNotEmpty() && ascii.endsWith(framing.trailer)) {
        ascii = ascii.dropLast(framing.trailer.length)
        lines += "Trailer" to "[${framing.trailer}]"
    }

    return FrameParts(lines, ascii, observedHeader)
}

/**
 * Finds the longest command in the vendor's palette that [payload] starts with. The console sends
 * the mnemonics from [getVendorCommands], which are wider than several vendors' native command
 * codes, so this match takes priority over any protocol-specific code width.
 */
internal fun matchKnownCommand(vendor: HsmVendorType, payload: String): VendorCommand? =
    getVendorCommands(vendor)
        .filter { it.code.isNotEmpty() && payload.startsWith(it.code) }
        .maxByOrNull { it.code.length }

/**
 * Wraps a decoded value in a [ThalesCommandField] descriptor so non-Thales vendors can render
 * through the same Parsed-fields table. [label] is carried as a single-entry option list, which
 * is how that table shows a description next to a value.
 *
 * The id `errorCode` is reserved for Thales — the Parsed view looks Thales error codes up by it.
 */
internal fun fieldOf(
    id: String,
    name: String,
    value: String,
    description: String = "",
    label: String = "",
    type: FieldType = FieldType.ASCII,
): Pair<ThalesCommandField, String> =
    ThalesCommandField(
        id = id,
        name = name,
        type = type,
        length = value.length,
        requirement = FieldRequirement.OPTIONAL,
        description = description,
        options = if (label.isNotBlank()) listOf(CodeOption(value, label)) else null,
    ) to value

/** Renders a decoded message in the same BP-Tools style used for Thales commands. */
internal fun formatBpStyle(
    framingLines: List<Pair<String, String>>,
    commandCode: String,
    commandName: String,
    fields: List<Pair<ThalesCommandField, String>>,
    isRequest: Boolean,
): String = buildString {
    for ((label, value) in framingLines) {
        appendLine("${padLabel(label)} = $value")
    }
    if (commandCode.isNotEmpty()) {
        val label = if (isRequest) "Command Code" else "Response Code"
        val suffix = when {
            commandName.isBlank() -> ""
            isRequest -> " $commandName"
            else -> " $commandName Response"
        }
        appendLine("${padLabel(label)} = [$commandCode]$suffix")
    }
    for ((field, value) in fields) {
        val description = field.options?.find { it.value == value }?.label.orEmpty()
        val line = "${padLabel(field.name)} = [$value]"
        appendLine(if (description.isNotBlank()) "$line $description" else line)
    }
}.trimEnd()

private fun padLabel(name: String): String = name.padEnd(LABEL_WIDTH, '.')

/**
 * Drops STX/ETX bytes that survived framing — `readResponse` already removes them for
 * [HeaderFormat.STX_ETX], but a device may wrap its payload in them regardless of the
 * configured framing.
 */
private fun String.stripStxEtx(): String = trim { it == '\u0002' || it == '\u0003' }

/** Fallback for a payload too short to carry a command code. */
private fun rawMessage(
    framingLines: List<Pair<String, String>>,
    payload: String,
    isRequest: Boolean,
): ParsedHsmMessage {
    val fields = if (payload.isEmpty()) emptyList()
    else listOf(fieldOf(id = "data", name = "Data", value = payload))
    return ParsedHsmMessage("", "", fields, formatBpStyle(framingLines, "", "", fields, isRequest))
}

/** Fallback for a recognised code width whose command is not in this vendor's dictionary. */
private fun unknownCommand(
    framingLines: List<Pair<String, String>>,
    code: String,
    data: String,
    isRequest: Boolean,
): ParsedHsmMessage {
    val fields = if (data.isEmpty()) emptyList()
    else listOf(fieldOf(id = "data", name = "Data", value = data))
    return ParsedHsmMessage(
        commandCode = code,
        commandName = "",
        fields = fields,
        formatted = formatBpStyle(framingLines, code, "", fields, isRequest),
    )
}
