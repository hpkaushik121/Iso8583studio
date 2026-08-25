package `in`.aicortex.iso8583studio.ui.screens.hsmCommand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.HsmCommandClientService
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HeaderFormat

private val Mono = FontFamily.Monospace
private val AccentGreen = Color(0xFF4CAF50)
private val AccentRed = Color(0xFFF44336)

/** How the pasted text should be read. */
enum class InputEncoding(val label: String) { AUTO("Auto"), HEX("Hex"), ASCII("ASCII") }

/** Which parser to run the pasted bytes through. */
enum class DirectionChoice(val label: String) { AUTO("Auto"), REQUEST("Request"), RESPONSE("Response") }

/** Survives HSM Commander tab switches, so a pasted capture is still there on return. */
class MessageParserSessionState {
    var input by mutableStateOf("")
    var encoding by mutableStateOf(InputEncoding.AUTO)
    var direction by mutableStateOf(DirectionChoice.AUTO)
    /** Whether the pasted bytes still carry the length/STX framing and message header. */
    var inputIncludesFraming by mutableStateOf(true)
    var outputTab by mutableStateOf(0)

    var parsed by mutableStateOf<ParsedHsmMessage?>(null)
    var resolvedDirection by mutableStateOf<HsmMessageDirection?>(null)
    var decoded by mutableStateOf<ByteArray?>(null)
    var error by mutableStateOf<String?>(null)
}

/** Framing for bytes that were already stripped down to the bare command payload. */
private val BARE_FRAMING = HsmFraming(
    headerFormat = HeaderFormat.NONE,
    tcpLengthEnabled = false,
    messageHeader = "",
    messageHeaderLength = 0,
)

/**
 * Decodes a request or response pasted as hex (or ASCII) using the configured vendor's parser —
 * the same decoding the Console applies to live traffic, for captures taken elsewhere.
 */
@Composable
fun HsmMessageParserTab(
    service: HsmCommandClientService,
    session: MessageParserSessionState,
) {
    val dividerColor = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
    val vendor = service.config.hsmVendor

    // Re-parse whenever the input or any option changes, so pasting is all it takes.
    LaunchedEffect(
        session.input, session.encoding, session.direction, session.inputIncludesFraming, vendor,
    ) {
        session.parsed = null
        session.resolvedDirection = null
        session.decoded = null
        session.error = null

        val text = session.input.trim()
        if (text.isEmpty()) return@LaunchedEffect

        val bytes = decodeInput(text, session.encoding).getOrElse { failure ->
            session.error = failure.message
            return@LaunchedEffect
        }
        session.decoded = bytes

        val framing = if (session.inputIncludesFraming) service.framing() else BARE_FRAMING
        val direction = when (session.direction) {
            DirectionChoice.REQUEST -> HsmMessageDirection.REQUEST
            DirectionChoice.RESPONSE -> HsmMessageDirection.RESPONSE
            DirectionChoice.AUTO ->
                detectDirection(vendor, bytes, framing) ?: HsmMessageDirection.REQUEST
        }
        session.resolvedDirection = direction

        session.parsed = try {
            when (direction) {
                HsmMessageDirection.REQUEST -> service.parser.parseRequest(bytes, framing)
                HsmMessageDirection.RESPONSE -> service.parser.parseResponse(bytes, framing, null)
            }
        } catch (e: Exception) {
            session.error = "Could not parse these bytes: ${e.message ?: e::class.simpleName}"
            null
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {

        // ╔══════════════════════════════════════╗
        // ║  INPUT                               ║
        // ╚══════════════════════════════════════╝
        Column(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
            InputPanel(session, service, Modifier.weight(1f))
        }

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = dividerColor)

        // ╔══════════════════════════════════════╗
        // ║  OUTPUT                              ║
        // ╚══════════════════════════════════════╝
        Column(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.surface) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabChip("Formatted", selected = session.outputTab == 0) { session.outputTab = 0 }
                    Spacer(Modifier.width(4.dp))
                    TabChip("Parsed", selected = session.outputTab == 1) { session.outputTab = 1 }
                    Spacer(Modifier.width(4.dp))
                    TabChip("Hex Dump", selected = session.outputTab == 2) { session.outputTab = 2 }
                    Spacer(Modifier.weight(1f))
                    ResultBadge(session)
                }
            }
            Divider(color = dividerColor)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (session.outputTab) {
                    0 -> MonospacePanel(
                        label = when (session.resolvedDirection) {
                            HsmMessageDirection.RESPONSE -> "DECODED RESPONSE"
                            else -> "DECODED REQUEST"
                        },
                        icon = if (session.resolvedDirection == HsmMessageDirection.RESPONSE)
                            Icons.Default.CallReceived else Icons.Default.CallMade,
                        accentColor = if (session.error != null) AccentRed else PrimaryBlue,
                        content = session.error ?: session.parsed?.formatted.orEmpty(),
                        isError = session.error != null,
                        modifier = Modifier.fillMaxSize(),
                    )
                    1 -> ParsedFieldsView(
                        parsedFields = session.parsed?.fields.orEmpty(),
                        modifier = Modifier.fillMaxSize(),
                    )
                    2 -> MonospacePanel(
                        label = "HEX DUMP",
                        icon = Icons.Default.DataArray,
                        accentColor = PrimaryBlue,
                        content = session.decoded?.let { hexDump(it) }.orEmpty(),
                        isError = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  INPUT PANEL
// ─────────────────────────────────────────────────────────

@Composable
private fun InputPanel(
    session: MessageParserSessionState,
    service: HsmCommandClientService,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(color = MaterialTheme.colors.surface) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Paste a captured message",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        service.config.hsmVendor.displayName,
                        fontSize = 10.sp, fontFamily = Mono,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                    )
                }

                OptionRow("Direction") {
                    DirectionChoice.entries.forEach { choice ->
                        TabChip(choice.label, selected = session.direction == choice) {
                            session.direction = choice
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }

                OptionRow("Encoding") {
                    InputEncoding.entries.forEach { choice ->
                        TabChip(choice.label, selected = session.encoding == choice) {
                            session.encoding = choice
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = session.inputIncludesFraming,
                        onCheckedChange = { session.inputIncludesFraming = it },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                    )
                    Column {
                        Text("Bytes include framing", fontSize = 11.sp)
                        Text(
                            if (session.inputIncludesFraming)
                                "Strip ${service.config.headerFormat.displayName.lowercase()} + " +
                                        "${service.config.messageHeaderLength}-char header"
                            else "Treat input as the bare command payload",
                            fontSize = 9.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                        )
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
            val scrollState = rememberScrollState()
            if (session.input.isEmpty()) {
                Text(
                    "e.g.  00 0A 30 30 30 30 4E 43\n" +
                            "or    00080000ND00\n" +
                            "or paste the ASCII command directly",
                    fontFamily = Mono, fontSize = 11.sp, lineHeight = 18.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.padding(10.dp),
                )
            }
            BasicTextField(
                value = session.input,
                onValueChange = { session.input = it },
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        1.dp,
                        MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                        RoundedCornerShape(6.dp),
                    )
                    .padding(10.dp)
                    .verticalScroll(scrollState),
                textStyle = TextStyle(
                    fontFamily = Mono, fontSize = 11.sp, lineHeight = 18.sp,
                    color = MaterialTheme.colors.onSurface,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue),
            )
        }

        if (session.input.isNotEmpty()) {
            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
            Surface(color = MaterialTheme.colors.surface.copy(alpha = 0.6f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        session.decoded?.let { "${it.size} bytes decoded" } ?: "not decoded",
                        fontSize = 10.sp, fontFamily = Mono,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { session.input = "" }) {
                        Text("Clear", fontSize = 10.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.width(64.dp),
            fontSize = 10.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
        )
        content()
    }
}

@Composable
private fun ResultBadge(session: MessageParserSessionState) {
    when {
        session.error != null -> Badge(Icons.Default.Cancel, "PARSE ERROR", AccentRed)
        session.parsed == null -> Unit
        else -> {
            val parsed = session.parsed!!
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (session.direction == DirectionChoice.AUTO) {
                    Text(
                        "auto: ${session.resolvedDirection?.name?.lowercase().orEmpty()}",
                        fontSize = 10.sp, fontFamily = Mono,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                    )
                }
                val status = parsed.status
                when {
                    status == null -> Badge(Icons.Default.Info, parsed.commandCode.ifEmpty { "—" }, PrimaryBlue)
                    status.success -> Badge(Icons.Default.CheckCircle, status.code, AccentGreen)
                    else -> Badge(Icons.Default.Cancel, status.code, AccentRed)
                }
            }
        }
    }
}

@Composable
private fun Badge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, null, Modifier.size(12.dp), tint = color)
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Mono, color = color)
    }
}

// ─────────────────────────────────────────────────────────
//  INPUT DECODING
// ─────────────────────────────────────────────────────────

private val SEPARATORS = Regex("[\\s:,\\-_]")
private val HEX_PREFIX = Regex("(?i)^0x")

/**
 * Turns pasted text into bytes. Hex is accepted with any mix of spaces, newlines, colons, dashes
 * or `0x` prefixes; [InputEncoding.AUTO] treats the text as hex only when it is unambiguously hex.
 */
internal fun decodeInput(text: String, encoding: InputEncoding): Result<ByteArray> {
    val cleaned = text.replace(SEPARATORS, "").replace(HEX_PREFIX, "")
    val looksLikeHex = cleaned.isNotEmpty() &&
            cleaned.length % 2 == 0 &&
            cleaned.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

    val useHex = when (encoding) {
        InputEncoding.HEX -> true
        InputEncoding.ASCII -> false
        InputEncoding.AUTO -> looksLikeHex
    }

    if (!useHex) return Result.success(text.toByteArray(Charsets.US_ASCII))

    if (cleaned.isEmpty()) return Result.failure(IllegalArgumentException("No hex digits found."))
    if (cleaned.length % 2 != 0) {
        return Result.failure(
            IllegalArgumentException("Hex input has an odd number of digits (${cleaned.length})."),
        )
    }
    val badIndex = cleaned.indexOfFirst { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }
    if (badIndex >= 0) {
        return Result.failure(
            IllegalArgumentException("'${cleaned[badIndex]}' is not a hex digit (position ${badIndex + 1})."),
        )
    }
    return Result.success(
        ByteArray(cleaned.length / 2) { i ->
            cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        },
    )
}

/** Classic offset / hex / printable-ASCII dump, 16 bytes per line. */
internal fun hexDump(bytes: ByteArray): String = buildString {
    for (offset in bytes.indices step 16) {
        val chunk = bytes.copyOfRange(offset, minOf(offset + 16, bytes.size))
        val hex = chunk.joinToString(" ") { "%02X".format(it) }.padEnd(16 * 3 - 1)
        val ascii = chunk.map { byte ->
            val code = byte.toInt() and 0xFF
            if (code in 32..126) code.toChar() else '.'
        }.joinToString("")
        appendLine("%04X  %s  %s".format(offset, hex, ascii))
    }
}.trimEnd()
