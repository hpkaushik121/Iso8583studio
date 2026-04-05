package `in`.aicortex.iso8583studio.ui.screens.hsmCommand

enum class FieldType { HEX, DEC, ASCII, CODE, FLAG }

enum class FieldRequirement { MANDATORY, OPTIONAL, CONDITIONAL }

enum class CommandCategory(val displayName: String) {
    DIAGNOSTICS("Diagnostics"),
    KEY_MANAGEMENT("Key Management"),
    PIN_OPERATIONS("PIN Operations"),
    DUKPT("DUKPT"),
    MAC_OPERATIONS("MAC Operations"),
    CVV_OPERATIONS("CVV Operations"),
    DATA_ENCRYPTION("Data Encryption"),
    RSA("RSA Operations"),
    EMV("EMV"),
    AS2805("AS2805"),
    TERMINAL_KEYS("Terminal Keys"),
    HMAC("HMAC"),
    ADMIN("Admin / Other"),
}

data class CodeOption(val value: String, val label: String)

data class FieldCondition(val dependsOnField: String, val values: Set<String>)

data class ThalesCommandField(
    val id: String,
    val name: String,
    val type: FieldType = FieldType.HEX,
    val length: Int = 0,
    val requirement: FieldRequirement = FieldRequirement.MANDATORY,
    val defaultValue: String = "",
    val description: String = "",
    val options: List<CodeOption>? = null,
    val condition: FieldCondition? = null,
    val conditions: List<FieldCondition> = emptyList(),
    /** When true, this field is omitted from the wire string if its value is blank (cleared in the UI). */
    val omitFromWireWhenBlank: Boolean = false,
)

data class ThalesCommandDefinition(
    val code: String,
    val responseCode: String,
    val name: String,
    val description: String,
    val category: CommandCategory,
    val requestFields: List<ThalesCommandField>,
    val responseFields: List<ThalesCommandField>,
    /** When true, the command builder lists each field on its own row (no side-by-side dropdown pairing). */
    val forceVerticalFieldLayout: Boolean = false,
)

object ThalesErrorCodes {
    private val descriptions = mapOf(
    "00" to "No Error",
    "01" to "Verification failure or warning of imported key parity error",
    "04" to "Invalid key type code",
    "05" to "Invalid key length flag",
    "10" to "Source key parity error",
    "11" to "Destination key parity error or key all zeros",
    "12" to "Contents of user storage not available. Reset, power-down or overwrite",
    "13" to "Invalid LMK Identifier",
    "14" to "PIN encrypted under LMK pair 02-03 is invalid",
    "15" to "Invalid input data (invalid format, invalid characters, or not enough data provided)",
    "16" to "Console or printer not ready or not connected",
    "17" to "HSM not in the Authorised state, or not enabled for clear PIN output, or both",
    "18" to "Document format definition not loaded",
    "19" to "Specified Diebold Table is invalid",
    "20" to "PIN block does not contain valid values",
    "21" to "Invalid index value, or index/block count would cause an overflow condition",
    "22" to "Invalid account number",
    "23" to "Invalid PIN block format code",
    "24" to "PIN is fewer than 4 or more than 12 digits in length",
    "25" to "Decimalisation Table error",
    "26" to "Invalid key scheme",
    "27" to "Incompatible key length",
    "28" to "Invalid key type",
    "29" to "Key function not permitted",
    "30" to "Invalid reference number",
    "31" to "Insufficient solicitation entries for batch",
    "33" to "LMK key change storage is corrupted",
    "39" to "Fraud detection",
    "40" to "Invalid checksum",
    "41" to "Internal hardware/software error: bad RAM, invalid error codes, etc.",
    "42" to "DES failure",
    "47" to "Algorithm not licensed",
    "49" to "Private key error, report to supervisor",
    "51" to "Invalid message header",
    "65" to "Transaction Key Scheme set to None",
    "67" to "Command not licensed",
    "68" to "Command has been disabled",
    "69" to "PIN block format has been disabled",
    "74" to "Invalid digest info syntax (no hash mode only)",
    "75" to "Single length key masquerading as double or triple length key",
    "76" to "Public key length error",
    "77" to "Clear data block error",
    "78" to "Private key length error",
    "79" to "Hash algorithm object identifier error",
    "80" to "Data length error. The amount of MAC data (or other data) is greater than or less than the expected amount.",
    "81" to "Invalid certificate header",
    "82" to "Invalid check value length",
    "83" to "Key block format error",
    "84" to "Key block check value error",
    "85" to "Invalid OAEP Mask Generation Function",
    "86" to "Invalid OAEP MGF Hash Function",
    "87" to "OAEP Parameter Error",
    "90" to "Data parity error in the request message received by the HSM",
    "91" to "Longitudinal Redundancy Check (LRC) character does not match the value computed over the input data (when the HSM has received a transparent async packet)",
    "92" to "The Count value (for the Command/Data field) is not between limits, or is not correct (when the HSM has received a transparent async packet)",
    "A1" to "Incompatible LMK schemes",
    "A2" to "Incompatible LMK identifiers",
    "A3" to "Incompatible keyblock LMK identifiers",
    "A4" to "Key block authentication failure",
    "A5" to "Incompatible key length",
    "A6" to "Invalid key usage",
    "A7" to "Invalid algorithm",
    "A8" to "Invalid mode of use",
    "A9" to "Invalid key version number",
    "AA" to "Invalid export field",
    "AB" to "Invalid number of optional blocks",
    "AC" to "Optional header block error",
    "AD" to "Key status optional block error",
    "AE" to "Invalid start date/time",
    "AF" to "Invalid end date/time",
    "B0" to "Invalid encryption mode",
    "B1" to "Invalid authentication mode",
    "B2" to "Miscellaneous keyblock error",
    "B3" to "Invalid number of optional blocks",
    "B4" to "Optional block data error",
    "B5" to "Incompatible components",
    "B6" to "Incompatible key status optional blocks",
    "B7" to "Invalid change field",
    "B8" to "Invalid old value",
    "B9" to "Invalid new value",
    "BA" to "No key status block in the keyblock",
    "BB" to "Invalid wrapping key",
    "BC" to "Repeated optional block",
    "BD" to "Incompatible key types"
    )

    fun getDescription(code: String): String = descriptions[code] ?: "Unknown error code"

    fun isSuccess(code: String): Boolean = code == "00"

    val allCodes: Map<String, String> get() = descriptions

    val errorCodeOptions: List<CodeOption> = descriptions.map { (code, desc) ->
        CodeOption(code, "$code - $desc")
    }
}

data class KeyBlockInfo(
    val schemePrefix: Char,
    val version: String,
    val blockLength: Int,
    val keyUsage: String,
    val algorithm: String,
    val modeOfUse: String,
    val keyVersionNumber: String,
    val exportability: String,
    val numOptionalBlocks: Int,
    val reserved: String,
    val encryptedKeyAndMac: String,
) {
    val keyUsageDesc: String get() = keyUsageDescriptions[keyUsage] ?: keyUsage
    val algorithmDesc: String get() = algorithmDescriptions[algorithm] ?: algorithm
    val modeOfUseDesc: String get() = modeOfUseDescriptions[modeOfUse] ?: modeOfUse
    val exportabilityDesc: String get() = exportabilityDescriptions[exportability] ?: exportability

    /** Inferred clear-text key size based on algorithm and payload size */
    val clearKeyBits: Int get() {
        val payloadBytes = encryptedKeyAndMac.length / 2
        return when (algorithm) {
            "D" -> 64
            "T" -> if (payloadBytes <= 40) 128 else 192
            "A" -> when {
                payloadBytes <= 40 -> 128
                payloadBytes <= 48 -> 192
                else -> 256
            }
            else -> payloadBytes * 8
        }
    }
    val clearKeyBytes: Int get() = clearKeyBits / 8
    val clearKeyHexChars: Int get() = clearKeyBytes * 2

    // Version 0 (TDES): 4-byte MAC = 8 hex; Version 1 (AES): 8-byte MAC = 16 hex
    private val macSizeHex: Int get() = if (version == "0") 8 else 16

    /** AES-encrypted key (PKCS#7 padded, AES-CBC encrypted) */
    val encryptedKeyHex: String get() {
        val payload = encryptedKeyAndMac
        return if (payload.length > macSizeHex) payload.substring(0, payload.length - macSizeHex) else payload
    }
    val encryptedKeyBytes: Int get() = encryptedKeyHex.length / 2

    /** 8-byte integrity MAC at the end of the payload */
    val macHex: String get() {
        val payload = encryptedKeyAndMac
        return if (payload.length > macSizeHex) payload.substring(payload.length - macSizeHex) else ""
    }
    val macBytes: Int get() = macHex.length / 2

    companion object {
        private val keyUsageDescriptions = mapOf(
            "P0" to "PIN Encryption",
            "K0" to "Key Encryption / Wrapping",
            "K1" to "TR-31 Key Block Protection Key",
            "D0" to "Data Encryption",
            "D1" to "Data Encryption (Decimalize)",
            "M0" to "ISO 9797-1 MAC Alg 1",
            "M1" to "ISO 9797-1 MAC Alg 1",
            "M3" to "ISO 9797-1 MAC Alg 3",
            "M6" to "ISO 9797-1:2011 CMAC",
            "M7" to "HMAC",
            "C0" to "CVV / CVC / CSC",
            "B0" to "BDK Base Derivation Key",
            "B1" to "Initial DUKPT Key",
            "V0" to "PIN Verification (IBM)",
            "V1" to "PIN Verification (IBM other)",
            "V2" to "PIN Verification (VISA PVV)",
            "E0" to "EMV/chip Issuer Master-Derive",
            "E1" to "EMV/chip Issuer Master-Session",
            "E2" to "EMV/chip Issuer Master-Common",
            "E4" to "EMV/chip PIN Change",
            "00" to "No Restrictions / Not Used",
        )

        private val algorithmDescriptions = mapOf(
            "D" to "DES",
            "T" to "TDES (Double or Triple)",
            "A" to "AES",
        )

        private val modeOfUseDescriptions = mapOf(
            "N" to "No Restrictions",
            "B" to "Both Encrypt & Decrypt",
            "E" to "Encrypt / Wrap Only",
            "D" to "Decrypt / Unwrap Only",
            "G" to "Generate (MAC/sign)",
            "V" to "Verify (MAC/sign)",
            "C" to "Compute / Generate",
            "S" to "Signature Only",
            "T" to "Both Sign & Decrypt",
            "X" to "Derive Key",
        )

        private val exportabilityDescriptions = mapOf(
            "E" to "Exportable (under trusted key)",
            "N" to "Not Exportable",
            "S" to "Sensitive (exportable under KEK)",
        )

        fun parse(value: String): KeyBlockInfo? {
            if (value.length < 17) return null
            val schemePrefix = value[0]
            if (schemePrefix != 'S' && schemePrefix != 'R') return null
            val inner = value.substring(1)
            if (inner.length < 16) return null

            val ver = inner.substring(0, 1)
            val lenStr = inner.substring(1, 5)
            val blockLen = lenStr.toIntOrNull() ?: return null
            val keyUsage = inner.substring(5, 7)
            val algo = inner.substring(7, 8)
            val mode = inner.substring(8, 9)
            val keyVer = inner.substring(9, 11)
            val export = inner.substring(11, 12)
            val numOptStr = inner.substring(12, 14)
            val numOpt = numOptStr.toIntOrNull() ?: return null
            val reserved = inner.substring(14, 16)
            val payload = if (inner.length > 16) inner.substring(16) else ""

            return KeyBlockInfo(
                schemePrefix = schemePrefix,
                version = ver,
                blockLength = blockLen,
                keyUsage = keyUsage,
                algorithm = algo,
                modeOfUse = mode,
                keyVersionNumber = keyVer,
                exportability = export,
                numOptionalBlocks = numOpt,
                reserved = reserved,
                encryptedKeyAndMac = payload,
            )
        }
    }
}

/**
 * M0 plaintext / M2 ciphertext message length field (4H hex): count of hex-encoded data when
 * [inputFormatFlag] is `1`, otherwise raw character count of [dataHex].
 */
fun computeM0MessageLengthField(dataHex: String, inputFormatFlag: String): String {
    val n = if (inputFormatFlag == "1") {
        dataHex.asSequence()
            .filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
            .count()
    } else {
        dataHex.length
    }
    return "%04X".format(n.coerceIn(0, 0xFFFF))
}

object ThalesWireBuilder {

    /**
     * Builds the plain ASCII command string (command code + field values concatenated).
     * This is exactly what gets converted to bytes and sent on the wire.
     * e.g. "A0" + "0" + "001" + "U" → sent as literal ASCII bytes.
     */
    fun buildPlainTextCommand(
        definition: ThalesCommandDefinition,
        fieldValues: Map<String, String>,
    ): String {
        val sb = StringBuilder()
        sb.append(definition.code)

        for (field in definition.requestFields) {
            if (!isFieldVisible(field, fieldValues)) continue
            val raw = fieldValues[field.id].orEmpty()
            if (raw.isBlank() && field.requirement == FieldRequirement.OPTIONAL) continue
            if (raw.isBlank() && field.omitFromWireWhenBlank) continue
            sb.append(raw)
        }
        return sb.toString()
    }

    /**
     * Parses response bytes into individual fields.
     * The raw response includes TCP framing + message header; [headerLengthBytes] indicates
     * how many leading bytes to skip to reach the response code.
     * The response body is plain ASCII: [response_code][field_data]
     */
    fun parseResponseFields(
        definition: ThalesCommandDefinition,
        responseBytes: ByteArray,
        headerLengthBytes: Int,
        requestFieldValues: Map<String, String> = emptyMap(),
    ): List<Pair<ThalesCommandField, String>> {
        val ascii = String(
            responseBytes,
            headerLengthBytes,
            (responseBytes.size - headerLengthBytes).coerceAtLeast(0),
            Charsets.US_ASCII,
        )

        if (ascii.length >= 2) {
            val responseCode = ascii.substring(0, 2)
            if ((definition.code == "M0" && responseCode == "M1") ||
                (definition.code == "M2" && responseCode == "M3") ||
                (definition.code == "M4" && responseCode == "M5")
            ) {
                parseM1M3DataBlockResponse(definition.responseFields, ascii)?.let { return it }
            }
        }

        val result = mutableListOf<Pair<ThalesCommandField, String>>()
        var pos = 2 // skip 2-char response code (e.g. "A1", "ND")
        val responseValues = mutableMapOf<String, String>()
        // Merge request field values so response field conditions can reference request fields (e.g. "mode")
        responseValues.putAll(requestFieldValues)
        val fields = definition.responseFields

        for ((idx, field) in fields.withIndex()) {
            if (!isFieldVisible(field, responseValues)) continue
            if (pos >= ascii.length) break

            val charLen = if (field.length > 0) {
                field.length
            } else {
                detectVariableFieldLength(ascii, pos, fields, idx)
            }
            if (charLen <= 0 && field.requirement == FieldRequirement.OPTIONAL) continue

            val endPos = minOf(pos + charLen, ascii.length)
            val value = ascii.substring(pos, endPos)

            responseValues[field.id] = value
            result.add(field to value)
            pos = endPos
        }
        return result
    }

    /**
     * M1 / M3 response layout (PayShield):
     * - CBC: `[Error_2H][IV_16H][DataLen_4H][Data_nH]`
     * - ECB: `[Error_2H][DataLen_4H][Data_nH]` (no IV)
     * [DataLen_4H] is the hex character count of [Data_nH].
     */
    private fun parseM1M3DataBlockResponse(
        fields: List<ThalesCommandField>,
        ascii: String,
    ): List<Pair<ThalesCommandField, String>>? {
        if (ascii.length < 4) return null
        val errorCode = ascii.substring(2, 4)
        val payloadLen = ascii.length - 4

        val map = mutableMapOf<String, String>()
        map["errorCode"] = errorCode

        val ivField = fields.find { it.id == "iv" || it.id == "outputIV" } ?: return null
        val lenField = fields.find {
            it.id == "encryptedMessageLength" || it.id == "decryptedMessageLength" || it.id == "msgLength"
        } ?: return null
        val dataField = fields.find {
            it.id == "encryptedData" || it.id == "decryptedData" || it.id == "translatedData"
        } ?: return null

        fun buildOrdered(): List<Pair<ThalesCommandField, String>> =
            fields.mapNotNull { f -> map[f.id]?.let { f to it } }

        if (payloadLen < 4) {
            return buildOrdered()
        }

        // CBC path: fixed 16H IV + 4H length + data
        if (payloadLen >= 20) {
            val lenHex = ascii.substring(20, 24)
            val n = lenHex.toIntOrNull(16) ?: -1
            if (n >= 0 && 20 + n == payloadLen) {
                map[ivField.id] = ascii.substring(4, 20)
                map[lenField.id] = lenHex
                map[dataField.id] = ascii.substring(24, 24 + n)
                return buildOrdered()
            }
        }

        // ECB path: 4H length + data (no IV)
        val lenHex = ascii.substring(4, 8)
        val n = lenHex.toIntOrNull(16) ?: return null
        if (n >= 0 && 4 + n == payloadLen) {
            map[ivField.id] = ""
            map[lenField.id] = lenHex
            map[dataField.id] = ascii.substring(8, 8 + n)
            return buildOrdered()
        }

        return null
    }

    /**
     * Determines the length of a variable-length response field by:
     * 1. Detecting Thales key scheme prefixes (S/R key blocks, U/X/T/Y scheme keys)
     * 2. Falling back to subtracting trailing mandatory fixed-length fields
     */
    private fun detectVariableFieldLength(
        ascii: String,
        pos: Int,
        allFields: List<ThalesCommandField>,
        currentIdx: Int,
    ): Int {
        val remaining = ascii.length - pos
        if (remaining <= 0) return 0

        val keyLen = detectKeyLength(ascii, pos)
        if (keyLen != null && keyLen <= remaining) return keyLen

        val trailingFixedLen = allFields.drop(currentIdx + 1)
            .filter { it.requirement == FieldRequirement.MANDATORY && it.length > 0 }
            .sumOf { it.length }

        return (remaining - trailingFixedLen).coerceAtLeast(0)
    }

    /**
     * Detects the length of a Thales key value based on its scheme prefix:
     * - S/R + digit: Key block with encoded length (S + version + 4-digit block length)
     * - U/X: Double-length TDES (1 prefix + 32 hex = 33)
     * - T/Y: Triple-length TDES (1 prefix + 48 hex = 49)
     */
    private fun detectKeyLength(ascii: String, pos: Int): Int? {
        val remaining = ascii.length - pos
        if (remaining < 6) return null

        return when (ascii[pos]) {
            'S', 'R' -> {
                if (!ascii[pos + 1].isDigit()) return null
                val lenStr = ascii.substring(pos + 2, minOf(pos + 6, ascii.length))
                val blockLen = lenStr.toIntOrNull() ?: return null
                1 + blockLen // scheme prefix char + block content
            }
            'U', 'X' -> 33  // prefix + 32H double-length key
            'T', 'Y' -> 49  // prefix + 48H triple-length key
            else -> null
        }
    }

    fun isFieldVisible(field: ThalesCommandField, fieldValues: Map<String, String>): Boolean {
        if (field.condition != null) {
            val depVal = fieldValues[field.condition.dependsOnField].orEmpty()
            if (depVal !in field.condition.values) return false
        }
        for (cond in field.conditions) {
            val depVal = fieldValues[cond.dependsOnField].orEmpty()
            if (depVal !in cond.values) return false
        }
        return true
    }

    private const val PAD_WIDTH = 25

    private fun padField(name: String): String = name.padEnd(PAD_WIDTH, '.')

    /**
     * Formats a sent command in BP-Tools style, e.g.:
     * ```
     * TCP/IP Header............ =*[0008] 8 Bytes
     * Message Header........... = [0000]
     * Command Code............. = [NO] HSM State Request
     * Mode Flag................ = [00] Return Status Information
     * ```
     */
    fun formatRequestBpStyle(
        definition: ThalesCommandDefinition,
        fieldValues: Map<String, String>,
        rawRequest: ByteArray,
        tcpLengthEnabled: Boolean,
        messageHeader: String,
    ): String {
        val sb = StringBuilder()

        if (tcpLengthEnabled && rawRequest.size >= 2) {
            val tcpLen = ((rawRequest[0].toInt() and 0xFF) shl 8) or (rawRequest[1].toInt() and 0xFF)
            sb.appendLine("${padField("TCP/IP Header")} =*[%04X] %d Bytes".format(tcpLen, tcpLen))
        }

        if (messageHeader.isNotBlank()) {
            sb.appendLine("${padField("Message Header")} = [$messageHeader]")
        }

        sb.appendLine("${padField("Command Code")} = [${definition.code}] ${definition.name}")

        for (field in definition.requestFields) {
            if (!isFieldVisible(field, fieldValues)) continue
            val value = fieldValues[field.id].orEmpty()
            if (value.isBlank() && field.requirement == FieldRequirement.OPTIONAL) continue
            if (value.isBlank() && field.omitFromWireWhenBlank) continue
            val desc = field.options?.find { it.value == value }?.label ?: ""
            val line = "${padField(field.name)} = [$value]"
            sb.appendLine(if (desc.isNotBlank()) "$line $desc" else line)
            val kb = KeyBlockInfo.parse(value)
            if (kb != null) {
                val indent = "  "
                sb.appendLine("${indent}Key Block Decoded:")
                val versionDesc = if (kb.version == "0") "Thales DES Key Block" else "Thales AES Key Block"
                sb.appendLine("${indent}  Version............. = [${kb.version}] $versionDesc")
                sb.appendLine("${indent}  Block Length......... = [${kb.blockLength}]")
                sb.appendLine("${indent}  Key Usage............ = [${kb.keyUsage}] ${kb.keyUsageDesc}")
                sb.appendLine("${indent}  Algorithm............ = [${kb.algorithm}] ${kb.algorithmDesc}")
                sb.appendLine("${indent}  Mode of Use.......... = [${kb.modeOfUse}] ${kb.modeOfUseDesc}")
                sb.appendLine("${indent}  Key Version Number... = [${kb.keyVersionNumber}]")
                sb.appendLine("${indent}  Exportability........ = [${kb.exportability}] ${kb.exportabilityDesc}")
                sb.appendLine("${indent}  Opt Blocks........... = [${String.format("%02d", kb.numOptionalBlocks)}]")
                sb.appendLine("${indent}  Reserved/LMK ID...... = [${kb.reserved}]")
                sb.appendLine("${indent}  Clear Key Size....... = ${kb.clearKeyBits} bits (${kb.clearKeyBytes} bytes = ${kb.clearKeyHexChars}H clear)")
                sb.appendLine("${indent}  Encrypted Key........ = [${kb.encryptedKeyHex}]")
                val encDesc = if (kb.version == "0")
                    "${kb.encryptedKeyBytes} bytes (${kb.encryptedKeyHex.length}H) = 2B length + ${kb.clearKeyBytes}B key + ${kb.encryptedKeyBytes - kb.clearKeyBytes - 2}B zero pad, TDES-ECB encrypted"
                else
                    "${kb.encryptedKeyBytes} bytes (${kb.encryptedKeyHex.length}H) = ${kb.clearKeyBytes}B key + ${kb.encryptedKeyBytes - kb.clearKeyBytes}B PKCS#7 pad, AES-CBC encrypted"
                sb.appendLine("${indent}                          $encDesc")
                sb.appendLine("${indent}  MAC.................. = [${kb.macHex}] (${kb.macBytes} bytes)")
            }
        }
        return sb.toString().trimEnd()
    }

    /**
     * Formats a response in BP-Tools style using already-parsed fields.
     */
    fun formatResponseBpStyle(
        definition: ThalesCommandDefinition,
        parsedFields: List<Pair<ThalesCommandField, String>>,
        rawResponse: ByteArray,
        tcpLengthEnabled: Boolean,
        messageHeader: String,
    ): String {
        val sb = StringBuilder()

        if (tcpLengthEnabled && rawResponse.size >= 2) {
            val tcpLen = ((rawResponse[0].toInt() and 0xFF) shl 8) or (rawResponse[1].toInt() and 0xFF)
            sb.appendLine("${padField("TCP/IP Header")} =*[%04X] %d Bytes".format(tcpLen, tcpLen))
        }

        if (messageHeader.isNotBlank()) {
            sb.appendLine("${padField("Message Header")} = [$messageHeader]")
        }

        sb.appendLine("${padField("Command Code")} = [${definition.responseCode}] ${definition.name} Response")

        for ((field, value) in parsedFields) {
            val desc = if (field.id == "errorCode") {
                val errDesc = ThalesErrorCodes.getDescription(value)
                if (ThalesErrorCodes.isSuccess(value)) errDesc else "** $errDesc **"
            } else {
                field.options?.find { it.value == value }?.label ?: ""
            }
            val line = "${padField(field.name)} = [$value]"
            sb.appendLine(if (desc.isNotBlank()) "$line $desc" else line)

            val kb = KeyBlockInfo.parse(value)
            if (kb != null) {
                val indent = "  "
                sb.appendLine("${indent}Key Block Decoded:")
                val versionDesc = if (kb.version == "0") "Thales DES Key Block" else "Thales AES Key Block"
                sb.appendLine("${indent}  Version............. = [${kb.version}] $versionDesc")
                sb.appendLine("${indent}  Block Length......... = [${kb.blockLength}]")
                sb.appendLine("${indent}  Key Usage............ = [${kb.keyUsage}] ${kb.keyUsageDesc}")
                sb.appendLine("${indent}  Algorithm............ = [${kb.algorithm}] ${kb.algorithmDesc}")
                sb.appendLine("${indent}  Mode of Use.......... = [${kb.modeOfUse}] ${kb.modeOfUseDesc}")
                sb.appendLine("${indent}  Key Version Number... = [${kb.keyVersionNumber}]")
                sb.appendLine("${indent}  Exportability........ = [${kb.exportability}] ${kb.exportabilityDesc}")
                sb.appendLine("${indent}  Opt Blocks........... = [${String.format("%02d", kb.numOptionalBlocks)}]")
                sb.appendLine("${indent}  Reserved/LMK ID...... = [${kb.reserved}]")
                sb.appendLine("${indent}  Clear Key Size....... = ${kb.clearKeyBits} bits (${kb.clearKeyBytes} bytes = ${kb.clearKeyHexChars}H clear)")
                sb.appendLine("${indent}  Encrypted Key........ = [${kb.encryptedKeyHex}]")
                val encDesc = if (kb.version == "0")
                    "${kb.encryptedKeyBytes} bytes (${kb.encryptedKeyHex.length}H) = 2B length + ${kb.clearKeyBytes}B key + ${kb.encryptedKeyBytes - kb.clearKeyBytes - 2}B zero pad, TDES-ECB encrypted"
                else
                    "${kb.encryptedKeyBytes} bytes (${kb.encryptedKeyHex.length}H) = ${kb.clearKeyBytes}B key + ${kb.encryptedKeyBytes - kb.clearKeyBytes}B PKCS#7 pad, AES-CBC encrypted"
                sb.appendLine("${indent}                          $encDesc")
                sb.appendLine("${indent}  MAC.................. = [${kb.macHex}] (${kb.macBytes} bytes)")
            }
        }
        return sb.toString().trimEnd()
    }
}
