package `in`.aicortex.iso8583studio.hsm.payshield10k

/**
 * ========================================================================================================
 * PAYSHIELD 10K - STRING COMMAND INTERFACE
 * Complete Command Processor with Raw String Commands
 * ========================================================================================================
 *
 * This provides a simple string-based interface to send commands to the HSM simulator
 * exactly as they would be sent over TCP/IP in a real PayShield 10K
 *
 * Command Format:
 * [Message Header][Command Code][Data Fields]%[LMK_ID][Trailer]
 *
 * Examples:
 * - "0000NC" - Diagnostic test
 * - "0000BA..." - Encrypt clear PIN
 * - "0000CA...%01" - Translate PIN using LMK slot 01
 *
 * ========================================================================================================
 */

import `in`.aicortex.iso8583studio.hsm.payshield10k.*
import ai.cortex.core.IsoUtil
import `in`.aicortex.iso8583studio.hsm.HsmConfig
import `in`.aicortex.iso8583studio.hsm.payshield10k.commands.A0GenerateKeyCommand
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditEntry
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditType
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmCommandResult
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.KeyLength
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.KeyType
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkPair
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkSet
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.PinBlockFormat
import kotlinx.coroutines.runBlocking
import java.security.SecureRandom

// ====================================================================================================
// STRING COMMAND PROCESSOR
// ====================================================================================================

/**
 * Main interface for processing raw string commands
 */
class PayShieldStringCommandProcessor(
    private val simulator: PayShield10KFeatures,
    private val hsmLogsListener: HsmLogsListener
) {
    private val commandProcessor = PayShield10KCommandProcessor(simulator,hsmLogsListener)
    private val advancedFeatures = PayShield10KAdvancedFeatures(simulator, commandProcessor)

    // Configuration
    private var messageHeaderLength = 4
    private var basePort = 1500
    private var defaultLmkId = "00"

    /**
     * Main method: Process raw string command
     *
     * @param command Raw command string (e.g., "0000NC" or "0000BA...%01")
     * @param sourcePort Optional TCP port for port-based LMK routing
     * @return Response string
     */
    suspend fun processCommand(command: String, sourcePort: Int? = null): String {
        return try {
            val parsed = parseCommand(command, sourcePort)
            val result = executeCommand(parsed)
            formatResponse(parsed, result)
        } catch (e: Exception) {
            formatError(command, "99", "Internal error: ${e.message}")
        }
    }

    /**
     * Parse command string
     */
    private fun parseCommand(command: String, sourcePort: Int?): ParsedCommand {
        var pos = 0

        // Message Header (default 4 chars)
        val header = if (command.length >= messageHeaderLength) {
            command.substring(0, messageHeaderLength).also { pos = messageHeaderLength }
        } else {
            "0000"
        }

        // Command Code (2 chars)
        if (pos + 2 > command.length) {
            throw IllegalArgumentException("Command too short")
        }
        val code = command.substring(pos, pos + 2)
        pos += 2

        // Find delimiter and extract LMK ID
        val delimiterIndex = command.indexOf('%', pos)
        val trailerIndex = command.indexOf('\u0019', pos)

        val dataEnd = when {
            delimiterIndex > 0 -> delimiterIndex
            trailerIndex > 0 -> trailerIndex
            else -> command.length
        }

        val data = if (pos < dataEnd) command.substring(pos, dataEnd) else ""

        var lmkId: String? = null
        var trailer: String? = null

        if (delimiterIndex > 0 && delimiterIndex + 2 < command.length) {
            lmkId = command.substring(delimiterIndex + 1, minOf(delimiterIndex + 3, command.length))
        }

        if (trailerIndex > 0 && trailerIndex + 1 < command.length) {
            trailer = command.substring(trailerIndex + 1)
        }

        // Determine effective LMK ID
        val effectiveLmkId = lmkId ?: (sourcePort?.let { portToLmkId(it) }) ?: defaultLmkId

        return ParsedCommand(header, code, data, effectiveLmkId, trailer, sourcePort)
    }

    /**
     * Execute parsed command
     */
    private suspend fun executeCommand(cmd: ParsedCommand): HsmCommandResult {
        // Get LMK from slot
        val lmk = simulator.getSlotManager().getLmkFromSlot(cmd.lmkId)
        if (lmk == null && cmd.code.uppercase() !in listOf("NC", "VR", "GK", "VT")) {
            return HsmCommandResult.Error("15", "LMK not loaded in slot ${cmd.lmkId}")
        }

        return when (cmd.code.uppercase()) {
            // Diagnostic & Info Commands
            "NC" -> executeNC(cmd)
            "VR" -> executeVR(cmd)
            "VT" -> executeVT(cmd)

            // Key Generation Commands
            "GK" -> executeGK(cmd)
            "GC" -> executeGC(cmd)
            "FK" -> executeFK(cmd)

            // PIN Processing Commands
            "BA" -> executeBA(cmd)
            "BC" -> executeBC(cmd)
            "CA" -> executeCA(cmd)
            "CI" -> executeCI(cmd)
            "DE" -> executeDE(cmd)
            "DG" -> executeDG(cmd)

            // MAC Commands
            "M0" -> executeM0(cmd)
            "M2" -> executeM2(cmd)

            // User Storage Commands
            "LA" -> executeLA(cmd)
            "LE" -> executeLE(cmd)
            "LD" -> executeLD(cmd)

            // Key Migration Commands
            "BW" -> executeBW(cmd)
            "BG" -> executeBG(cmd)

            "A0" -> A0GenerateKeyCommand(simulator).execute(cmd.data)

            // DUKPT Commands
            "G0" -> executeG0(cmd)

            // PIN ↔ LMK Translation Commands
            "JC" -> executeJC(cmd)
            "JE" -> executeJE(cmd)
            "JG" -> executeJG(cmd)

            // MAC Commands (DUKPT / ZAK variants)
            "M4" -> executeM4(cmd)
            "M6" -> executeM6(cmd)
            "M8" -> executeM8(cmd)
            "MY" -> executeMY(cmd)

            // Hash Command
            "GM" -> executeGM(cmd)

            // IBM PIN Commands
            "EE" -> executeEE(cmd)

            // PIN Utilities
            "NG" -> executeNG(cmd)

            // Dynamic CVV/CVC
            "PM" -> executePM(cmd)

            else -> HsmCommandResult.Error("80", "Unknown command: ${cmd.code}")
        }
    }

    /**
     * Format response
     */
    private fun formatResponse(cmd: ParsedCommand, result: HsmCommandResult): String {
        val responseCode = cmd.code[0] + (cmd.code[1] + 1).toString()
        simulator.auditLog.addEntry(
            AuditEntry(
                entryType = AuditType.HOST_COMMAND,
                command = cmd.header,
                result = if (result is HsmCommandResult.Success) "SUCCESS" else "FAILED",
                details = if (result is HsmCommandResult.Success) {
                    result.response
                } else {
                    (result as HsmCommandResult.Error).message
                }
            )
        )
        return when (result) {
            is HsmCommandResult.Success -> {
                buildString {
                    append(cmd.header)
                    append(responseCode)
                    append("00")
                    append(result.response)
                    if (cmd.trailer != null) {
                        append('\u0019')
                        append(cmd.trailer)
                    }
                }
            }

            is HsmCommandResult.Error -> {
                buildString {
                    append(cmd.header)
                    append(responseCode)
                    append(result.errorCode.padStart(2, '0'))
                }
            }
        }
    }

    /**
     * Format error response
     */
    private fun formatError(command: String, errorCode: String, message: String): String {
        val header = if (command.length >= 4) command.substring(0, 4) else "0000"
        val code = if (command.length >= 6) command.substring(4, 6) else "XX"
        val respCode = code[0] + (code.getOrNull(1)?.plus(1)?.toString() ?: "X")
        return "$header$respCode$errorCode"
    }

    /**
     * Convert TCP port to LMK ID
     */
    private fun portToLmkId(port: Int): String? {
        val offset = port - basePort - 1
        return if (offset in 0..99) {
            offset.toString().padStart(2, '0')
        } else null
    }

    // ====================================================================================================
    // COMMAND IMPLEMENTATIONS
    // ====================================================================================================

    /**
     * NC - Diagnostics
     * Command: 0000NC
     * Response: 0000ND00[LMK Check][Firmware Version]
     */
    private fun executeNC(cmd: ParsedCommand): HsmCommandResult {
        return simulator.executeDiagnosticTest()
    }

    /**
     * VR - Version
     * Command: 0000VR
     * Response: 0000VS00[Version Info]
     */
    private fun executeVR(cmd: ParsedCommand): HsmCommandResult {
        return HsmCommandResult.Success(
            response = "PayShield 10K Simulator v1.0.0",
            data = mapOf("version" to "1.0.0")
        )
    }

    /**
     * VT - View LMK Table
     * Command: 0000VT
     * Response: 0000VU00[LMK Table]
     */
    private fun executeVT(cmd: ParsedCommand): HsmCommandResult {
        val table = simulator.getSlotManager().viewLmkTable()
        return HsmCommandResult.Success(
            response = table,
            data = mapOf("table" to table)
        )
    }

    /**
     * GK - Generate LMK Component
     * Command: 0000GK[LMK_ID][Component_Number]
     * Example: 0000GK001
     * Response: 0000GL00[Component]
     */
    private suspend fun executeGK(cmd: ParsedCommand): HsmCommandResult {
        val lmkId = if (cmd.data.length >= 2) cmd.data.substring(0, 2) else cmd.lmkId
        val compNum = if (cmd.data.length >= 3) cmd.data.substring(2, 3).toIntOrNull() ?: 1 else 1

        return simulator.executeGenerateLmkComponent(lmkId, compNum)
    }

    /**
     * GC - Generate Key Component
     * Command: 0000GC[KEY_TYPE_3H][SCHEME_1A]%[LMK_ID]
     * Response: 0000GD00[Clear Component][Encrypted Component][KCV]
     */
    private suspend fun executeGC(cmd: ParsedCommand): HsmCommandResult {
        val data = cmd.data
        val keyTypeCode = if (data.length >= 3) data.substring(0, 3) else "001"
        val scheme      = if (data.length >= 4) data[3].toString() else "U"
        val keyLength   = when (scheme.uppercase()) {
            "T" -> KeyLength.SINGLE
            "X" -> KeyLength.TRIPLE
            else -> KeyLength.DOUBLE
        }
        val keyType = KeyType.values().find { it.code == keyTypeCode } ?: KeyType.TYPE_001
        return commandProcessor.executeGenerateKeyComponent(
            lmkId = cmd.lmkId,
            keyLength = keyLength,
            keyType = keyType
        )
    }

    /**
     * FK - Form Key from Components
     * Command: 0000FK[NUM_COMP_1N][KEY_TYPE_3H][SCHEME_1A][LMK_SCHEME_1A][COMP1][COMP2][COMP3?]
     * Response: 0000FL 00 [Encrypted Key][KCV_6H]
     *
     * SCHEME governs component length:
     *   T → 16 hex (single-length DES)
     *   U → 32 hex (double-length 3DES)  ← default
     *   X → 48 hex (triple-length 3DES)
     */
    private suspend fun executeFK(cmd: ParsedCommand): HsmCommandResult {
        val data = cmd.data
        var pos = 0

        // Number of components (1 digit)
        val numComponents = data.getOrNull(pos)?.digitToIntOrNull() ?: 2
        pos++

        // Key type code (3 hex chars, e.g. "001")
        val keyTypeCode = if (data.length >= pos + 3) data.substring(pos, pos + 3) else "001"
        pos += 3

        // Scheme (1 char)
        val scheme = if (data.length > pos) data[pos].toString().uppercase() else "U"
        pos++

        // LMK scheme flag (1 char, skip – variant LMK assumed)
        if (data.length > pos) pos++

        // Component hex length depends on scheme
        val compHexLen = when (scheme) {
            "T" -> 16
            "X" -> 48
            else -> 32   // "U" and everything else → double-length
        }

        val components = mutableListOf<ByteArray>()
        repeat(numComponents) {
            if (pos + compHexLen <= data.length) {
                val hex = data.substring(pos, pos + compHexLen)
                pos += compHexLen
                components.add(hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
            }
        }

        if (components.size < 2) {
            return HsmCommandResult.Error(
                "15",
                "FK needs at least 2 components; only ${components.size} found in command data"
            )
        }

        val keyType = KeyType.values().find { it.code == keyTypeCode } ?: KeyType.TYPE_001

        return commandProcessor.executeFormKeyFromComponents(
            lmkId = cmd.lmkId,
            keyType = keyType,
            components = components
        )
    }

    /**
     * BA - Encrypt Clear PIN
     * Command: 0000BA[TPK][PIN_Format][PIN][Account]%[LMK_ID]
     * Example: 0000BAU1234567890ABCDEF1234567890ABCDEF012345411111111111%00
     * Response: 0000BB00[Encrypted PIN Block]
     */
    private suspend fun executeBA(cmd: ParsedCommand): HsmCommandResult {
        var pos = 0
        val data = cmd.data

        // TPK scheme (1 char: U=unencrypted hex, others=variant)
        val tpkScheme = data[pos]
        pos++

        // TPK (32 hex chars = 16 bytes)
        val tpkHex = data.substring(pos, pos + 32)
        pos += 32
        val tpk = IsoUtil.hexToBytes(tpkHex)

        // PIN block format (2 chars)
        val formatCode = data.substring(pos, pos + 2)
        pos += 2

        // Clear PIN (variable length, digits only)
        val pinStart = pos
        while (pos < data.length && data[pos].isDigit()) pos++
        val clearPin = data.substring(pinStart, pos)

        // Account number (12 digits)
        val accountNumber = data.substring(pos, pos + 12)

        val format = when (formatCode) {
            "01" -> PinBlockFormat.ISO_FORMAT_0
            "05" -> PinBlockFormat.ISO_FORMAT_1
            "47" -> PinBlockFormat.ISO_FORMAT_3
            else -> PinBlockFormat.ISO_FORMAT_0
        }

        val result = commandProcessor.executeEncryptClearPin(
            lmkId = cmd.lmkId,
            clearPin = clearPin,
            accountNumber = accountNumber,
            encryptedTpk = tpk,
            pinBlockFormat = format
        )

        return if (result is HsmCommandResult.Success) {
            HsmCommandResult.Success(
                response = result.data["encryptedPinBlock"] as String,
                data = result.data
            )
        } else {
            result
        }
    }

    /**
     * BC - Verify Terminal PIN
     * Command: 0000BC[TPK][PVK][MaxPINLen][PIN Block][Format][Account]%[LMK_ID]
     * Response: 0000BD00
     */
    private fun executeBC(cmd: ParsedCommand): HsmCommandResult {
        // Implementation would parse and verify PIN
        return HsmCommandResult.Success(
            response = "",
            data = emptyMap()
        )
    }

    /**
     * CA - Translate PIN from TPK to ZPK
     * Command: 0000CA[ZPK][TPK][MaxPIN][PIN Block][SrcFmt][DstFmt][Account]%[LMK_ID]
     * Response: 0000CB00[Translated PIN Block]
     */
    private suspend fun executeCA(cmd: ParsedCommand): HsmCommandResult {
        var pos = 0
        val data = cmd.data

        // ZPK (33 chars with scheme)
        val zpkScheme = data[pos]
        pos++
        val zpkHex = data.substring(pos, pos + 32)
        pos += 32
        val zpk = IsoUtil.hexToBytes(zpkHex)

        // TPK (33 chars with scheme)
        val tpkScheme = data[pos]
        pos++
        val tpkHex = data.substring(pos, pos + 32)
        pos += 32
        val tpk = IsoUtil.hexToBytes(tpkHex)

        // Max PIN length (2 chars)
        val maxPinLen = data.substring(pos, pos + 2).toIntOrNull() ?: 12
        pos += 2

        // PIN block (16 hex chars = 8 bytes)
        val pinBlockHex = data.substring(pos, pos + 32)
        pos += 32
        val pinBlock = IsoUtil.hexToBytes(pinBlockHex)

        // Source format (2 chars)
        val srcFormat = data.substring(pos, pos + 2)
        pos += 2

        // Destination format (2 chars)
        val dstFormat = data.substring(pos, pos + 2)
        pos += 2

        // Account number (12 chars)
        val account = data.substring(pos, minOf(pos + 16, data.length))
        hsmLogsListener.onFormattedRequest(
            """
                zpkScheme: ${zpkScheme}
                zpkHex: ${zpkHex}
                tpkScheme: ${tpkScheme}
                tpkHex: ${tpkHex}
                maxPinLen: ${maxPinLen}
                pinBlockHex: ${pinBlockHex}
                srcFormat: ${srcFormat}
                dstFormat: ${dstFormat}
                
            """.trimIndent()
        )
        val result = commandProcessor.executeTranslatePinTpkToZpk(
            lmkId = cmd.lmkId,
            encryptedPinBlock = pinBlock,
            encryptedSourceTpk = tpk,
            encryptedDestZpk = zpk,
            accountNumber = account,
            sourcePinBlockFormat = if (srcFormat == "01") PinBlockFormat.ISO_FORMAT_0 else PinBlockFormat.ISO_FORMAT_1,
            destPinBlockFormat = if (dstFormat == "01") PinBlockFormat.ISO_FORMAT_0 else PinBlockFormat.ISO_FORMAT_1
        )

        return if (result is HsmCommandResult.Success) {
            HsmCommandResult.Success(
                response = result.data["encryptedPinBlock"] as String,
                data = result.data
            )
        } else {
            result
        }
    }

    /**
     * CI - Translate PIN from DUKPT to ZPK
     * Command: 0000CI[ZPK][KSN][PIN Block][SrcFmt][DstFmt][Account]%[LMK_ID]
     * Response: 0000CJ00[Translated PIN Block]
     */
    private suspend fun executeCI(cmd: ParsedCommand): HsmCommandResult {
        var pos = 0
        val data = cmd.data

        // ZPK
        val zpkScheme = data[pos]
        pos++
        val zpkHex = data.substring(pos, pos + 32)
        pos += 32
        val zpk = IsoUtil.hexToBytes(zpkHex)

        // KSN (20 hex chars = 10 bytes)
        val ksnHex = data.substring(pos, pos + 20)
        pos += 20

        // PIN block
        val pinBlockHex = data.substring(pos, pos + 16)
        pos += 16
        val pinBlock = IsoUtil.hexToBytes(pinBlockHex)

        // Formats
        val srcFormat = data.substring(pos, pos + 2)
        pos += 2
        val dstFormat = data.substring(pos, pos + 2)
        pos += 2

        // Account
        val account = data.substring(pos, minOf(pos + 12, data.length))

        val result = commandProcessor.executeTranslatePinDukptToZpk(
            lmkId = cmd.lmkId,
            encryptedDestZpk = zpk,
            ksn = ksnHex,
            encryptedPinBlock = pinBlock,
            accountNumber = account,
            sourcePinBlockFormat = if (srcFormat == "01") PinBlockFormat.ISO_FORMAT_0 else PinBlockFormat.ISO_FORMAT_1,
            destPinBlockFormat = if (dstFormat == "01") PinBlockFormat.ISO_FORMAT_0 else PinBlockFormat.ISO_FORMAT_1,
            encryptedBdk = "".toByteArray()
        )

        return if (result is HsmCommandResult.Success) {
            HsmCommandResult.Success(
                response = result.data["translatedPinBlock"] as String,
                data = result.data
            )
        } else {
            result
        }
    }

    /**
     * DE - Generate IBM Offset
     * Command: 0000DE[PVK][PIN Block][Format][CheckLen][Account]%[LMK_ID]
     * Response: 0000DF00[Offset]
     */
    private fun executeDE(cmd: ParsedCommand): HsmCommandResult {
        // Parse and generate offset
        return HsmCommandResult.Success(
            response = "1234",
            data = mapOf("offset" to "1234")
        )
    }

    /**
     * DG - Generate VISA PVV
     * Command: 0000DG[PVK][PIN Block][Format][Account][PVKI]%[LMK_ID]
     * Response: 0000DH00[PVV]
     */
    private fun executeDG(cmd: ParsedCommand): HsmCommandResult {
        // Parse and generate PVV
        return HsmCommandResult.Success(
            response = "5678",
            data = mapOf("pvv" to "5678")
        )
    }

    /**
     * M0 - Generate MAC (ISO 9797-1 Algorithm 3)
     * Command: 0000M0[TAK][Message Length][Message]%[LMK_ID]
     * Response: 0000M100[MAC]
     */
    private fun executeM0(cmd: ParsedCommand): HsmCommandResult {
        var pos = 0
        val data = cmd.data

        // TAK
        val takScheme = data[pos]
        pos++
        val takHex = data.substring(pos, pos + 32)
        pos += 32
        val tak = IsoUtil.hexToBytes(takHex)

        // Message length (4 hex chars)
        val msgLenHex = data.substring(pos, pos + 4)
        pos += 4
        val msgLen = msgLenHex.toInt(16)

        // Message
        val messageHex = data.substring(pos, pos + msgLen * 2)
        val message = IsoUtil.hexToBytes(messageHex)

        val result = commandProcessor.executeGenerateMac(
            data = message,
            tak = tak,
            algorithm = "ISO9797_ALG3"
        )

        return if (result is HsmCommandResult.Success) {
            HsmCommandResult.Success(
                response = result.data["mac"] as String,
                data = result.data
            )
        } else {
            result
        }
    }

    /**
     * M2 - Verify MAC
     * Command: 0000M2[TAK][Message Length][Message][MAC]%[LMK_ID]
     * Response: 0000M300
     */
    private fun executeM2(cmd: ParsedCommand): HsmCommandResult {
        // Parse and verify MAC
        return HsmCommandResult.Success(
            response = "",
            data = emptyMap()
        )
    }

    /**
     * LA - Load to User Storage
     * Command: 0000LA[Index][Data]%[LMK_ID]
     * Example: 0000LA0001234567890ABCDEF%00
     * Response: 0000LB00
     */
    private fun executeLA(cmd: ParsedCommand): HsmCommandResult {
        val index = cmd.data.substring(0, 3)
        val dataHex = cmd.data.substring(3)

        return simulator.getSlotManager().loadDataToUserStorage(
            index = index,
            data = IsoUtil.hexToBytes(dataHex),
            dataType = UserDataType.GENERIC,
            lmkId = cmd.lmkId,
            description = "User data"
        )
    }

    /**
     * LE - Read from User Storage
     * Command: 0000LE[Index]%[LMK_ID]
     * Example: 0000LE000%00
     * Response: 0000LF00[Data]
     */
    private fun executeLE(cmd: ParsedCommand): HsmCommandResult {
        val index = cmd.data.substring(0, 3)
        val result = simulator.getSlotManager().readDataFromUserStorage(index)

        return if (result is HsmCommandResult.Success) {
            val data = result.data["data"] as ByteArray
            HsmCommandResult.Success(
                response = IsoUtil.bytesToHexString(data),
                data = result.data
            )
        } else {
            result
        }
    }

    /**
     * LD - Delete from User Storage
     * Command: 0000LD[Index]%[LMK_ID]
     * Response: 0000LE00
     */
    private fun executeLD(cmd: ParsedCommand): HsmCommandResult {
        val index = cmd.data.substring(0, 3)
        return simulator.getSlotManager().deleteDataFromUserStorage(index)
    }

    /**
     * BW - Translate Key (Old to New LMK)
     * Command: 0000BW[KeyType][OldKey][NewKeyType]%[LMK_ID]
     * Response: 0000BX00[NewKey][KCV]
     */
    private fun executeBW(cmd: ParsedCommand): HsmCommandResult {
        // Key migration implementation
        return HsmCommandResult.Success(
            response = "1234567890ABCDEF1234567890ABCDEF123456",
            data = emptyMap()
        )
    }

    /**
     * BG - Translate PIN (Old to New LMK)
     * Command: 0000BG[Account][PIN]%[LMK_ID]
     * Response: 0000BH00[NewPIN]
     */
    private fun executeBG(cmd: ParsedCommand): HsmCommandResult {
        return HsmCommandResult.Success(
            response = "1234567890ABCDEF",
            data = emptyMap()
        )
    }

    // ====================================================================================================
    // G0 — Translate PIN Block from BDK (DUKPT) to ZPK
    // Command: G0[BDK_FLAG][BDK][ZPK][KSN_DESC][KSN][PIN_BLOCK][SRC_FMT][DST_FMT][ACCOUNT]
    // Response: G1 00 [PIN_LEN] [PIN_BLOCK] [DST_FMT]
    // BDK_FLAG: ~ = Type2 BDK
    // ====================================================================================================
    private suspend fun executeG0(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // BDK Flag (1 char: ~ = Type2)
            val bdkFlag = data[pos]
            pos++

            // BDK (scheme char + 32H = 33 chars)
            val bdkScheme = data[pos]
            pos++
            val bdkHex = data.substring(pos, pos + 32)
            pos += 32
            val bdk = IsoUtil.hexToBytes(bdkHex)

            // ZPK (scheme char + 32H = 33 chars)
            val zpkScheme = data[pos]
            pos++
            val zpkHex = data.substring(pos, pos + 32)
            pos += 32
            val zpk = IsoUtil.hexToBytes(zpkHex)

            // KSN Descriptor (3H)
            pos += 3 // skip KSN descriptor

            // KSN (20H = 10 bytes)
            val ksnHex = data.substring(pos, pos + 20)
            pos += 20

            // PIN Block (16H = 8 bytes)
            val pinBlockHex = data.substring(pos, pos + 16)
            pos += 16
            val pinBlock = IsoUtil.hexToBytes(pinBlockHex)

            // Source and dest format (2N each)
            val srcFmt = data.substring(pos, pos + 2)
            pos += 2
            val dstFmt = data.substring(pos, pos + 2)
            pos += 2

            // Account number (12N)
            val account = data.substring(pos, minOf(pos + 12, data.length))

            hsmLogsListener.onFormattedRequest("""
                G0 - DUKPT PIN Translation
                BDK Flag: $bdkFlag  BDK: $bdkHex
                ZPK: $zpkHex
                KSN: $ksnHex  PIN Block: $pinBlockHex
                Src Format: $srcFmt  Dst Format: $dstFmt  Account: $account
            """.trimIndent())

            val srcPinBlockFormat = parsePinBlockFormat(srcFmt)
            val dstPinBlockFormat = parsePinBlockFormat(dstFmt)

            val result = commandProcessor.executeTranslatePinDukptToZpk(
                lmkId = cmd.lmkId,
                encryptedDestZpk = zpk,
                ksn = ksnHex,
                encryptedPinBlock = pinBlock,
                accountNumber = account,
                sourcePinBlockFormat = srcPinBlockFormat,
                destPinBlockFormat = dstPinBlockFormat,
                encryptedBdk = bdk
            )

            if (result is HsmCommandResult.Success) {
                val outBlock = result.data["translatedPinBlock"] as? String ?: result.response
                HsmCommandResult.Success(
                    response = "04$outBlock$dstFmt",
                    data = result.data
                )
            } else result

        } catch (e: Exception) {
            HsmCommandResult.Error("15", "G0 failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // JC — Translate PIN Block from TPK to LMK Encryption
    // Command: JC[TPK_16H][PIN_BLOCK_16H][FMT_2N][ACCOUNT_12N]
    // Response: JD 00 [LMK_PIN]
    // ====================================================================================================
    private suspend fun executeJC(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // TPK (16H — single-length DES, no scheme prefix in classic form)
            // Handle both single-length (16H) and double-length (scheme + 32H)
            val tpkHex: String
            if (data[pos].isLetter() && pos + 33 <= data.length) {
                pos++ // skip scheme char
                tpkHex = data.substring(pos, pos + 32)
                pos += 32
            } else {
                tpkHex = data.substring(pos, pos + 16)
                pos += 16
            }
            val tpk = IsoUtil.hexToBytes(tpkHex)

            // Source PIN Block (16H)
            val pinBlockHex = data.substring(pos, pos + 16)
            pos += 16
            val pinBlock = IsoUtil.hexToBytes(pinBlockHex)

            // Format (2N)
            val fmt = data.substring(pos, pos + 2)
            pos += 2

            // Account (12N)
            val account = data.substring(pos, minOf(pos + 12, data.length))

            hsmLogsListener.onFormattedRequest("""
                JC - Translate PIN TPK→LMK
                TPK: $tpkHex  PIN Block: $pinBlockHex  Fmt: $fmt  Account: $account
            """.trimIndent())

            val lmkPin = commandProcessor.executeTranslatePinToLmk(
                lmkId = cmd.lmkId,
                encryptedTpk = tpk,
                encryptedPinBlock = pinBlock,
                accountNumber = account,
                pinBlockFormat = parsePinBlockFormat(fmt)
            )

            if (lmkPin is HsmCommandResult.Success) {
                HsmCommandResult.Success(
                    response = lmkPin.data["lmkPin"] as? String ?: lmkPin.response,
                    data = lmkPin.data
                )
            } else lmkPin

        } catch (e: Exception) {
            HsmCommandResult.Error("15", "JC failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // JE — Translate PIN Block from ZPK to LMK Encryption
    // Command: JE[ZPK_SCHEME+32H][PIN_BLOCK_16H][FMT_2N][ACCOUNT_12N]
    // Response: JF 00 [LMK_PIN]
    // ====================================================================================================
    private suspend fun executeJE(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // ZPK (scheme char + 32H)
            val zpkScheme = data[pos]
            pos++
            val zpkHex = data.substring(pos, pos + 32)
            pos += 32
            val zpk = IsoUtil.hexToBytes(zpkHex)

            // PIN Block (16H)
            val pinBlockHex = data.substring(pos, pos + 16)
            pos += 16
            val pinBlock = IsoUtil.hexToBytes(pinBlockHex)

            // Format (2N)
            val fmt = data.substring(pos, pos + 2)
            pos += 2

            // Account (12N)
            val account = data.substring(pos, minOf(pos + 12, data.length))

            hsmLogsListener.onFormattedRequest("""
                JE - Translate PIN ZPK→LMK
                ZPK: $zpkHex  PIN Block: $pinBlockHex  Fmt: $fmt  Account: $account
            """.trimIndent())

            val lmkPin = commandProcessor.executeTranslatePinZpkToLmk(
                lmkId = cmd.lmkId,
                encryptedZpk = zpk,
                encryptedPinBlock = pinBlock,
                accountNumber = account,
                pinBlockFormat = parsePinBlockFormat(fmt)
            )

            if (lmkPin is HsmCommandResult.Success) {
                HsmCommandResult.Success(
                    response = lmkPin.data["lmkPin"] as? String ?: lmkPin.response,
                    data = lmkPin.data
                )
            } else lmkPin

        } catch (e: Exception) {
            HsmCommandResult.Error("15", "JE failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // JG — Translate PIN from LMK to ZPK Encryption
    // Command: JG[ZPK_16H or SCHEME+32H][FMT_2N][ACCOUNT_12N][LMK_PIN]
    // Response: JH 00 [PIN_BLOCK_16H]
    // ====================================================================================================
    private suspend fun executeJG(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val zpkHex: String
            if (data[pos].isLetter() && pos + 33 <= data.length) {
                pos++ // skip scheme char
                zpkHex = data.substring(pos, pos + 32)
                pos += 32
            } else {
                zpkHex = data.substring(pos, pos + 16)
                pos += 16
            }
            val zpk = IsoUtil.hexToBytes(zpkHex)

            // Format (2N)
            val fmt = data.substring(pos, pos + 2)
            pos += 2

            // Account (12N)
            val account = data.substring(pos, pos + 12)
            pos += 12

            // LMK-encrypted PIN (5N by default; length varies with PIN Length setting)
            val lmkPin = data.substring(pos, minOf(pos + 12, data.length)).trimEnd()

            hsmLogsListener.onFormattedRequest("""
                JG - Translate PIN LMK→ZPK
                ZPK: $zpkHex  Fmt: $fmt  Account: $account  LMK PIN: $lmkPin
            """.trimIndent())

            val result = commandProcessor.executeTranslatePinLmkToZpk(
                lmkId = cmd.lmkId,
                encryptedZpk = zpk,
                lmkEncryptedPin = lmkPin,
                accountNumber = account,
                pinBlockFormat = parsePinBlockFormat(fmt)
            )

            if (result is HsmCommandResult.Success) {
                HsmCommandResult.Success(
                    response = result.data["zpkPinBlock"] as? String ?: result.response,
                    data = result.data
                )
            } else result

        } catch (e: Exception) {
            HsmCommandResult.Error("15", "JG failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // M4 — Translate Data Block (decrypt under one key, re-encrypt under another)
    // Command: M4[MODE_2N][INPUT_FMT_1N][OUTPUT_FMT_1N][SRC_KEY_TYPE_3H][SRC_KEY][DST_KEY_TYPE_3H][DST_KEY][KSN_DESC][KSN][DATA_LEN_4H][DATA]
    // Response: M5 00 [TRANSLATED_DATA_LEN_4H][TRANSLATED_DATA]
    // ====================================================================================================
    private suspend fun executeM4(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // Mode (2N)
            val mode = data.substring(pos, pos + 2)
            pos += 2

            // Input format (1N) / Output format (1N)
            val inputFmt = data.substring(pos, pos + 1)
            pos++
            val outputFmt = data.substring(pos, pos + 1)
            pos++

            // Source key type (3H) + key (scheme + 32H)
            pos += 3 // key type
            val srcScheme = data[pos]
            pos++
            val srcKeyHex = data.substring(pos, pos + 32)
            pos += 32
            val srcKey = IsoUtil.hexToBytes(srcKeyHex)

            // Dest key type (3H) + key (scheme + 32H)
            pos += 3 // key type
            val dstScheme = data[pos]
            pos++
            val dstKeyHex = data.substring(pos, pos + 32)
            pos += 32
            val dstKey = IsoUtil.hexToBytes(dstKeyHex)

            // KSN Descriptor (3H) + KSN (20H)
            pos += 3
            val ksnHex = data.substring(pos, pos + 20)
            pos += 20

            // Data length (4H) + data
            val dataLenHex = data.substring(pos, pos + 4)
            pos += 4
            val dataLen = dataLenHex.toInt(16)
            val encDataHex = data.substring(pos, pos + dataLen * 2)
            val encData = IsoUtil.hexToBytes(encDataHex)

            val result = commandProcessor.executeTranslateData(
                lmkId = cmd.lmkId,
                sourceKey = srcKey,
                destKey = dstKey,
                ksn = ksnHex,
                encryptedData = encData,
                mode = mode
            )

            if (result is HsmCommandResult.Success) {
                val outData = result.data["translatedData"] as? String ?: result.response
                val lenHex = (outData.length / 2).toString(16).padStart(4, '0').uppercase()
                HsmCommandResult.Success(
                    response = "$lenHex$outData",
                    data = result.data
                )
            } else result

        } catch (e: Exception) {
            HsmCommandResult.Error("15", "M4 failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // M6 — Generate MAC (with TAK or DUKPT BDK)
    // Command: M6[TAK_SCHEME+TAK][MODE_1N][MSG_LEN_4H][MSG]
    //       or M6[BDK_FLAG][BDK][KSN_DESC][KSN][MODE_1N][MSG_LEN_4H][MSG]
    // Response: M7 00 [MAC_8H]
    // ====================================================================================================
    private fun executeM6(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val tak: ByteArray
            val ksnHex: String?

            if (data[pos] == '~' || data[pos] == '!') {
                // DUKPT MAC mode — BDK + KSN
                pos++ // BDK flag
                val bdkScheme = data[pos]; pos++
                val bdkHex = data.substring(pos, pos + 32); pos += 32
                pos += 3 // KSN descriptor
                ksnHex = data.substring(pos, pos + 20); pos += 20
                tak = IsoUtil.hexToBytes(bdkHex) // will be derived from BDK
            } else {
                // TAK mode (variant scheme char + 32H hex)
                pos++ // scheme char
                val takHex = data.substring(pos, pos + 32); pos += 32
                tak = IsoUtil.hexToBytes(takHex)
                ksnHex = null
            }

            val mode = data[pos]; pos++

            // Message length (4H) + message
            val msgLenHex = data.substring(pos, pos + 4); pos += 4
            val msgLen = msgLenHex.toInt(16)
            val messageHex = data.substring(pos, pos + msgLen * 2)
            val message = IsoUtil.hexToBytes(messageHex)

            hsmLogsListener.onFormattedRequest("""
                M6 - Generate MAC
                KSN: ${ksnHex ?: "N/A"}  Mode: $mode  MsgLen: $msgLen
            """.trimIndent())

            val result = commandProcessor.executeGenerateMac(
                data = message,
                tak = tak,
                algorithm = "ISO9797_ALG3"
            )

            if (result is HsmCommandResult.Success) {
                HsmCommandResult.Success(
                    response = result.data["mac"] as? String ?: result.response,
                    data = result.data
                )
            } else result

        } catch (e: Exception) {
            HsmCommandResult.Error("15", "M6 failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // M8 — Verify MAC
    // Command: M8[TAK_SCHEME+TAK][MODE_1N][MSG_LEN_4H][MSG][MAC_8H]
    // Response: M9 00
    // ====================================================================================================
    private fun executeM8(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val tak: ByteArray

            if (data[pos] == '~' || data[pos] == '!') {
                pos++ // BDK flag
                pos++ // scheme char
                pos += 32 // BDK hex
                pos += 3 // KSN descriptor
                val bdkHex = data.substring(pos - 35, pos - 3) // already moved past
                pos += 20 // KSN
                tak = IsoUtil.hexToBytes(bdkHex)
            } else {
                pos++ // scheme char
                val takHex = data.substring(pos, pos + 32); pos += 32
                tak = IsoUtil.hexToBytes(takHex)
            }

            val mode = data[pos]; pos++

            val msgLenHex = data.substring(pos, pos + 4); pos += 4
            val msgLen = msgLenHex.toInt(16)
            val messageHex = data.substring(pos, pos + msgLen * 2); pos += msgLen * 2
            val message = IsoUtil.hexToBytes(messageHex)

            val macHex = data.substring(pos, pos + 16)
            val mac = IsoUtil.hexToBytes(macHex)

            val result = commandProcessor.executeVerifyMac(
                data = message,
                providedMac = mac,
                tak = tak,
                algorithm = "ISO9797_ALG3"
            )

            result

        } catch (e: Exception) {
            HsmCommandResult.Error("15", "M8 failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // MY — Verify and Translate MAC (streaming, supports first/middle/last blocks)
    // Simplified: treat as M8 (verify) + M6 (generate) with second key
    // ====================================================================================================
    private fun executeMY(cmd: ParsedCommand): HsmCommandResult {
        // For simulation purposes, return success with a generated MAC
        return HsmCommandResult.Success(
            response = "AABBCCDDEEFF0011",
            data = mapOf("mac" to "AABBCCDDEEFF0011")
        )
    }

    // ====================================================================================================
    // GM — Hash a Block of Data
    // Command: GM[HASH_ALG_2N][MSG_LEN_5N][MSG]
    //   Hash algorithms: 01=SHA-1, 02=SHA-224, 03=SHA-256, 04=SHA-384, 05=SHA-512
    // Response: GN 00 [HASH]
    // ====================================================================================================
    private fun executeGM(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val hashAlg = data.substring(pos, pos + 2).toIntOrNull() ?: 1
            pos += 2

            val msgLen = data.substring(pos, pos + 5).toIntOrNull() ?: 0
            pos += 5

            val msgData = if (data[pos] == '<') {
                // Binary hex data in angle brackets
                pos++
                val hexStr = data.substring(pos, data.indexOf('>', pos))
                IsoUtil.hexToBytes(hexStr)
            } else {
                data.substring(pos, pos + msgData_length(data, pos, msgLen)).toByteArray()
            }

            val algorithmName = when (hashAlg) {
                1 -> "SHA-1"
                2 -> "SHA-224"
                3 -> "SHA-256"
                4 -> "SHA-384"
                5 -> "SHA-512"
                else -> "SHA-256"
            }

            val digest = java.security.MessageDigest.getInstance(algorithmName)
            val hash = digest.digest(msgData)
            val hashHex = IsoUtil.bytesToHexString(hash)

            HsmCommandResult.Success(
                response = hashHex,
                data = mapOf("hash" to hashHex, "algorithm" to algorithmName)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "GM failed: ${e.message}")
        }
    }

    private fun msgData_length(data: String, pos: Int, declaredLen: Int): Int =
        minOf(declaredLen, data.length - pos)

    // ====================================================================================================
    // EE — Derive a PIN Using the IBM Offset Method
    // Command: EE[PVK_16H][OFFSET_12H][MIN_PIN_2N][ACCOUNT_12N][DEC_TABLE_16H][USER_DATA_12A]
    // Response: EF 00 [LMK_PIN]
    // ====================================================================================================
    private suspend fun executeEE(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // PVK (16H single-DES or 32H double-length)
            val pvkHex = data.substring(pos, pos + 16); pos += 16
            val pvk = IsoUtil.hexToBytes(pvkHex)

            // Offset (12H, F-padded)
            val offset = data.substring(pos, pos + 12); pos += 12

            // Min PIN length (2N)
            val minPinLen = data.substring(pos, pos + 2).toIntOrNull() ?: 4; pos += 2

            // Account number (12N)
            val account = data.substring(pos, pos + 12); pos += 12

            // Decimalization table (16H)
            val decTableHex = data.substring(pos, pos + 16); pos += 16

            // User-defined data (up to 12A, may contain 'N' for PAN placeholder)
            val userData = data.substring(pos, minOf(pos + 12, data.length))

            hsmLogsListener.onFormattedRequest("""
                EE - Derive IBM PIN
                PVK: $pvkHex  Offset: $offset  Account: $account
            """.trimIndent())

            val result = commandProcessor.executeDerivePinIbm(
                lmkId = cmd.lmkId,
                pvk = pvk,
                offset = offset,
                accountNumber = account,
                decimalizationTable = decTableHex,
                minPinLength = minPinLen
            )

            if (result is HsmCommandResult.Success) {
                HsmCommandResult.Success(
                    response = result.data["lmkPin"] as? String ?: result.response,
                    data = result.data
                )
            } else result

        } catch (e: Exception) {
            HsmCommandResult.Error("15", "EE failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // NG — Decrypt PIN from LMK-Encryption to Clear
    // Command: NG[ACCOUNT_12N][LMK_PIN]
    // Response: NH 00 [CLEAR_PIN]
    // ====================================================================================================
    private suspend fun executeNG(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // Account number (12N)
            val account = data.substring(pos, pos + 12); pos += 12

            // LMK-encrypted PIN
            val lmkPin = data.substring(pos).trimEnd()

            val result = commandProcessor.executeDecryptLmkPin(
                lmkId = cmd.lmkId,
                accountNumber = account,
                lmkEncryptedPin = lmkPin
            )

            if (result is HsmCommandResult.Success) {
                HsmCommandResult.Success(
                    response = result.data["clearPin"] as? String ?: result.response,
                    data = result.data
                )
            } else result

        } catch (e: Exception) {
            HsmCommandResult.Error("15", "NG failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // PM — Verify Dynamic CVV/CVC
    // Command: PM[SCHEME][FLAGS][PAN][EXPIRY][SERVICE_CODE][TRACK2][CVV]
    // Response: PN 00
    // ====================================================================================================
    private fun executePM(cmd: ParsedCommand): HsmCommandResult {
        // CVV/CVC verification - simplified implementation
        return HsmCommandResult.Success(
            response = "",
            data = mapOf("verified" to true)
        )
    }

    // ====================================================================================================
    // Helpers
    // ====================================================================================================
    private fun parsePinBlockFormat(code: String): PinBlockFormat {
        return when (code) {
            "01" -> PinBlockFormat.ISO_FORMAT_0
            "02" -> PinBlockFormat.ISO_FORMAT_1
            "03" -> PinBlockFormat.ISO_FORMAT_1
            "05" -> PinBlockFormat.ISO_FORMAT_1
            "47" -> PinBlockFormat.ISO_FORMAT_3
            else -> PinBlockFormat.ISO_FORMAT_0
        }
    }
}

/**
 * Parsed command structure
 */
data class ParsedCommand(
    val header: String,
    val code: String,
    val data: String,
    val lmkId: String,
    val trailer: String?,
    val sourcePort: Int?
)

// ====================================================================================================
// USAGE EXAMPLES
// ====================================================================================================

/**
 * Examples showing how to use string commands
 */
class StringCommandExamples {

    suspend fun runAllExamples() {
        val simulator = PayShield10KFeatures(HsmConfig(),object :HsmLogsListener{
            override fun onAuditLog(auditLog: AuditEntry) {
                TODO("Not yet implemented")
            }

            override fun log(auditLog: String) {
                TODO("Not yet implemented")
            }

            override fun onFormattedRequest(log: String) {
                TODO("Not yet implemented")
            }

            override fun onFormattedResponse(log: String) {
                TODO("Not yet implemented")
            }


        })
        val slotManager = HsmSlotManager()
        val processor = PayShieldStringCommandProcessor(simulator,object :HsmLogsListener{
            override fun onAuditLog(auditLog: AuditEntry) {
                TODO("Not yet implemented")
            }

            override fun log(auditLog: String) {
                TODO("Not yet implemented")
            }

            override fun onFormattedRequest(log: String) {
                TODO("Not yet implemented")
            }

            override fun onFormattedResponse(log: String) {
                TODO("Not yet implemented")
            }


        })

        // Setup LMK
        val lmk = createTestLmk("00")
        slotManager.allocateLmkSlot("00", lmk, isDefault = true)

        println("╔════════════════════════════════════════════════════════════════════════════╗")
        println("║         PayShield 10K String Command Examples                              ║")
        println("╚════════════════════════════════════════════════════════════════════════════╝\n")

        // Example 1: Diagnostic
        println("[1] NC - Diagnostic Test")
        val nc = "0000NC"
        println("    Command:  $nc")
        val ncResp = processor.processCommand(nc)
        println("    Response: $ncResp")
        println()

        // Example 2: Generate LMK Component
        println("[2] GK - Generate LMK Component")
        val gk = "0000GK001"
        println("    Command:  $gk")
        val gkResp = processor.processCommand(gk)
        println("    Response: ${gkResp.take(50)}...")
        println()

        // Example 3: Encrypt Clear PIN
        println("[3] BA - Encrypt Clear PIN")
        val ba = "0000BAU1234567890ABCDEF1234567890ABCDEF012345411111111111%00"
        println("    Command:  $ba")
        val baResp = processor.processCommand(ba)
        println("    Response: ${baResp.take(60)}...")
        println()

        // Example 4: Translate PIN TPK to ZPK
        println("[4] CA - Translate PIN from TPK to ZPK")
        val ca =
            "0000CAU1234567890ABCDEF1234567890ABCDEFU9876543210FEDCBA9876543210FEDCBA121234567890ABCDEF010141111111 11111%00"
        println("    Command:  ${ca.take(60)}...")
        val caResp = processor.processCommand(ca)
        println("    Response: ${caResp.take(60)}...")
        println()

        // Example 5: User Storage
        println("[5] LA - Load to User Storage")
        val la = "0000LA0001234567890ABCDEF1234567890ABCDEF%00"
        println("    Command:  $la")
        val laResp = processor.processCommand(la)
        println("    Response: $laResp")
        println()

        println("[6] LE - Read from User Storage")
        val le = "0000LE000%00"
        println("    Command:  $le")
        val leResp = processor.processCommand(le)
        println("    Response: ${leResp.take(60)}...")
        println()

        // Example 7: Generate MAC
        println("[7] M0 - Generate MAC")
        val m0 = "0000M0U1234567890ABCDEF1234567890ABCDEF0010001122334455667788"
        println("    Command:  $m0")
        val m0Resp = processor.processCommand(m0)
        println("    Response: ${m0Resp.take(60)}...")
        println()

        // Example 8: Multi-slot
        println("[8] Commands with Different LMK Slots")
        slotManager.allocateLmkSlot("01", createTestLmk("01"))

        val ba00 = "0000BAU1234567890ABCDEF1234567890ABCDEF012345411111111111%00"
        val ba01 = "0000BAU1234567890ABCDEF1234567890ABCDEF012345411111111111%01"

        println("    Slot 00:  ${processor.processCommand(ba00).take(40)}...")
        println("    Slot 01:  ${processor.processCommand(ba01).take(40)}...")
        println("    → Same command, different LMK slots = different results!")
        println()

        println("✓ All examples completed!\n")
    }

    private fun createTestLmk(id: String): LmkSet {
        val lmk = LmkSet(identifier = id, scheme = "VARIANT")
        for (i in 0 until 14) {
            lmk.pairs[i] = LmkPair(
                ByteArray(8).also { SecureRandom().nextBytes(it) },
                ByteArray(8).also { SecureRandom().nextBytes(it) }
            )
        }
        return lmk
    }
}

/**
 * Main entry point
 */
fun main() {
    val examples = StringCommandExamples()
    runBlocking { examples.runAllExamples() }
}