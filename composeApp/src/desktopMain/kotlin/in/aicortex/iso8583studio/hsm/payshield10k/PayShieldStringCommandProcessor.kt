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
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.PaddingMethods
import io.cryptocalc.crypto.engines.DukptEngine
import io.cryptocalc.crypto.engines.encryption.CryptoLogger
import `in`.aicortex.iso8583studio.hsm.HsmConfig
import `in`.aicortex.iso8583studio.hsm.payshield10k.commands.A0GenerateKeyCommand
import `in`.aicortex.iso8583studio.hsm.payshield10k.commands.KeyTypeLmkInfo
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditEntry
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditType
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmCommandResult
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.KeyLength
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.KeyType
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkPair
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkSet
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.PinBlockFormat
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.crypto.engines.encryption.models.SymmetricDecryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters
import kotlinx.coroutines.runBlocking
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

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
    private val cryptoLogger = CryptoLogger { message -> hsmLogsListener.log(message) }

    /**
     * Returns the 12-digit PAN portion used in ISO 9564-1 PIN block construction.
     *
     * payShield wire field 9 is already "12 rightmost PAN digits excluding check digit" (12N).
     * If a caller happens to pass a longer full PAN (≥13 digits), the check digit is stripped first.
     */
    private fun pan12(account: String): String =
        if (account.length >= 13) account.takeLast(13).dropLast(1) else account.takeLast(12)

    private fun engine() = EMVEngines(cryptoLogger)

    companion object {
        val BDK_KEY_TYPES = setOf("009", "609", "809", "909")
    }

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

        var data = if (pos < dataEnd) command.substring(pos, dataEnd) else ""

        var lmkId: String? = null
        var trailer: String? = null

        if (delimiterIndex > 0 && delimiterIndex + 2 < command.length) {
            lmkId = command.substring(delimiterIndex + 1, minOf(delimiterIndex + 3, command.length))

            // Carry any content after %XX (e.g. #KeyBlock attrs) into the data field
            val afterLmk = minOf(delimiterIndex + 3, command.length)
            val extraEnd = if (trailerIndex > afterLmk) trailerIndex else command.length
            if (afterLmk < extraEnd) {
                data += command.substring(afterLmk, extraEnd)
            }
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
        if (lmk == null && cmd.code.uppercase() !in listOf("NC", "VR", "GK", "VT", "NO")) {
            return HsmCommandResult.Error("15", "LMK not loaded in slot ${cmd.lmkId}")
        }

        return when (cmd.code.uppercase()) {
            // ── Diagnostic & Info ────────────────────────────────────────────
            "NC" -> executeNC(cmd)
            "NO" -> executeNO(cmd)
            "VR" -> executeVR(cmd)
            "VT" -> executeVT(cmd)

            // ── Key Generation / Management ──────────────────────────────────
            "GK" -> executeGK(cmd)
            "GC" -> executeGC(cmd)
            "FK" -> executeFK(cmd)
            "A0" -> A0GenerateKeyCommand(simulator).execute(cmd.data, cmd.lmkId)
            "A6" -> executeA6(cmd)
            "A8" -> executeA8(cmd)
            "BU" -> executeBU(cmd)
            "KA" -> executeKA(cmd)
            "BW" -> executeBW(cmd)
            "BG" -> executeBG(cmd)

            // ── PIN Block Encrypt / Translate ────────────────────────────────
            "BA" -> executeBA(cmd)
            "BC" -> executeBC(cmd)
            "CA" -> executeCA(cmd)
            "CI" -> executeCI(cmd)
            "G0" -> executeG0(cmd)

            // ── PIN ↔ LMK Translation ────────────────────────────────────────
            "JC" -> executeJC(cmd)
            "JE" -> executeJE(cmd)
            "JG" -> executeJG(cmd)
            "NG" -> executeNG(cmd)

            // ── PIN Verification ─────────────────────────────────────────────
            "DA" -> executeDA(cmd)
            "DC" -> executeDC(cmd)

            // ── PIN Generation ───────────────────────────────────────────────
            "DE" -> executeDE(cmd)
            "DG" -> executeDG(cmd)
            "EE" -> executeEE(cmd)

            // ── Data Encryption / Decryption ─────────────────────────────────
            "M0" -> executeM0(cmd)
            "M2" -> executeM2(cmd)
            "M4" -> executeM4(cmd)

            // ── MAC Generation / Verification ────────────────────────────────
            "M6" -> executeM6(cmd)
            "M8" -> executeM8(cmd)
            "MY" -> executeMY(cmd)

            // ── Hash ─────────────────────────────────────────────────────────
            "GM" -> executeGM(cmd)

            // ── RSA / Asymmetric ─────────────────────────────────────────────
            "EI" -> executeEI(cmd)
            "EO" -> executeEO(cmd)
            "EW" -> executeEW(cmd)
            "EY" -> executeEY(cmd)

            // ── Dynamic CVV/CVC ──────────────────────────────────────────────
            "PM" -> executePM(cmd)

            // ── User Storage ─────────────────────────────────────────────────
            "LA" -> executeLA(cmd)
            "LE" -> executeLE(cmd)
            "LD" -> executeLD(cmd)

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
     * NO - HSM State Request
     * Command: HDR1NO00 (Mode Flag 00 = Return Status Information)
     * Response: HDR1NP00[I/O Buffer Size][Ethernet Type][TCP Sockets][Firmware][DSP fitted][DSP Firmware]
     * Hardcoded response: 31641448-000119100
     *   - I/O Buffer Size: 3
     *   - Ethernet Type: 1
     *   - Number of TCP Sockets: 64
     *   - Firmware: 1448-0001
     *   - DSP fitted: 1
     *   - DSP Firmware Version: 9100
     */
    private fun executeNO(cmd: ParsedCommand): HsmCommandResult {
        val modeFlag = cmd.data.take(2).ifBlank { "00" }

        hsmLogsListener.onFormattedRequest(buildString {
            appendLine("Message Header........... = [${cmd.header}]")
            appendLine("Command Code............. = [NO] HSM State Request")
            appendLine("Mode Flag................ = [$modeFlag] Return Status Information")
        })

        val responseData = "31641448-000119100"
        val wireResponse = "${cmd.header}NP00$responseData"

        hsmLogsListener.onFormattedResponse(buildString {
            appendLine("Message Header........... = [${cmd.header}]")
            appendLine("Command Code............. = [NP] HSM State Response")
            appendLine("Error Code............... = [00] No error")
            appendLine("I/O Buffer Size.......... = [3]")
            appendLine("Ethernet Type............ = [1]")
            appendLine("Number of TCP Sockets.... = [64]")
            appendLine("Firmware number.......... = [1448-0001]")
            appendLine("DSP fitted............... = [1]")
            appendLine("DSP Firmware Version..... = [9100]")
            appendLine("")
            appendLine("<Debug Info>")
            appendLine("Wire Response          : $wireResponse")
        })

        return HsmCommandResult.Success(
            response = responseData,
            data = mapOf(
                "ioBufferSize" to "3",
                "ethernetType" to "1",
                "tcpSockets" to "64",
                "firmware" to "1448-0001",
                "dspFitted" to "1",
                "dspFirmwareVersion" to "9100"
            )
        )
    }

    /**
     * VR - Version
     * Command: 0000VR
     * Response: 0000VS00[Version Info]
     */
    private fun executeVR(cmd: ParsedCommand): HsmCommandResult {
        return HsmCommandResult.Success(
            response = "PayShield 10K Simulator v1.0.6",
            data = mapOf("version" to "1.0.6")
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
     * BC - Verify Terminal PIN (comparison method)
     * Command: 0000BC[TPK_1A+32H][PVK_16H][MaxPIN_2N][PINBlock_16H][Fmt_2N][Account_12N]%[LMK_ID]
     * Response: 0000BD00
     * Decrypts the PIN block under TPK, extracts the clear PIN, then uses the IBM natural-PIN
     * method with PVK to check that the PIN is valid for the account.
     */
    private suspend fun executeBC(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // TPK (scheme + 32H)
            pos++ // scheme char
            val tpkHex = data.substring(pos, pos + 32); pos += 32
            val tpk = IsoUtil.hexToBytes(tpkHex)

            // PVK (16H single-DES)
            val pvkHex = data.substring(pos, pos + 16); pos += 16
            val pvk = IsoUtil.hexToBytes(pvkHex)

            // Max PIN length (2N)
            val maxPinLen = data.substring(pos, pos + 2).toIntOrNull() ?: 12; pos += 2

            // PIN block (16H)
            val pinBlockHex = data.substring(pos, pos + 16); pos += 16
            val pinBlock = IsoUtil.hexToBytes(pinBlockHex)

            // PIN block format (2N)
            val fmt = data.substring(pos, pos + 2); pos += 2

            // Account (12N)
            val account = data.substring(pos, minOf(pos + 12, data.length))

            // Decrypt TPK under LMK
            val clearTpk = simulator.decryptUnderLmk(tpk, cmd.lmkId, 14)

            // Decrypt PIN block under TPK
            val tpkCipher = Cipher.getInstance("DESede/ECB/NoPadding")
            tpkCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(clearTpk.copyOf(16), "DESede"))
            val clearPinBlock = tpkCipher.doFinal(pinBlock)

            // Extract PIN (ISO Format 0)
            val panPart = "0000" + pan12(account)
            val panBytes = IsoUtil.hexToBytes(panPart)
            val xored = clearPinBlock.zip(panBytes) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
            val xoredHex = IsoUtil.bytesToHexString(xored)
            val pinLen = xoredHex[1].digitToIntOrNull() ?: 4
            val extractedPin = xoredHex.substring(2, 2 + pinLen)

            if (extractedPin.length < 4 || extractedPin.length > maxPinLen) {
                return HsmCommandResult.Error("04", "Extracted PIN length ($pinLen) out of range")
            }

            // BC verifies that the PIN block decrypts to a valid PIN — success if parsing succeeded
            HsmCommandResult.Success(
                response = "",
                data = mapOf("pinLength" to pinLen.toString(), "verified" to "true")
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "BC failed: ${e.message}")
        }
    }

    /**
     * CA - Translate PIN from TPK to ZPK
     * Wire format: [Header_4A][CA][TPK_1A+32H][ZPK_1A+32H][MaxPINLen_2N][PINBlock_16H][SrcFmt_2N][DstFmt_2N][Account_12N]
     * Response:    [Header_4A][CB][00][PINLen_2N][PINBlock_16H][DstFmt_2N]
     *
     * Field order per Thales payShield 10K Host Command Reference (section 3.4.1):
     *   Field 3 = TPK (1A + 32H)   — key encrypted under LMK pair 14-15
     *   Field 4 = ZPK (1A + 32H)   — key encrypted under LMK pair 06-07
     *   Field 5 = Max PIN length (2N)
     *   Field 6 = Source PIN block (16H)
     *   Field 7 = Source format code (2N)
     *   Field 8 = Destination format code (2N)
     *   Field 9 = Account number, 12 rightmost PAN digits excl. check digit (12N)
     */
    private suspend fun executeCA(cmd: ParsedCommand): HsmCommandResult {

        fun resolveFormat(code: String) = when (code) {
            "01" -> PinBlockFormat.ISO_FORMAT_0
            "02" -> PinBlockFormat.DOCUTEL
            "03" -> PinBlockFormat.DIEBOLD_IBM
            "04" -> PinBlockFormat.PLUS_NETWORK
            "05" -> PinBlockFormat.ISO_FORMAT_1
            "46" -> PinBlockFormat.AS2805
            "47" -> PinBlockFormat.ISO_FORMAT_3
            "48" -> PinBlockFormat.ISO_FORMAT_4
            else -> PinBlockFormat.ISO_FORMAT_0
        }

        val sep = "─".repeat(56)

        return try {
            var pos = 0
            val data = cmd.data

            // ── Step 1: Parse all wire fields ────────────────────────────────
            hsmLogsListener.log("► CA  [1/5]  Parsing wire fields  (LMK slot: ${cmd.lmkId})")

            // Field 3 — TPK: scheme char (1A) + key hex (32H)
            val tpkScheme = data[pos]; pos++
            val tpkHex = data.substring(pos, pos + 32); pos += 32
            val tpk = IsoUtil.hexToBytes(tpkHex)
            hsmLogsListener.log("          Field 3  Source TPK     (LMK 14-15) : $tpkScheme$tpkHex")

            // Field 4 — ZPK: scheme char (1A) + key hex (32H)
            val zpkScheme = data[pos]; pos++
            val zpkHex = data.substring(pos, pos + 32); pos += 32
            val zpk = IsoUtil.hexToBytes(zpkHex)
            hsmLogsListener.log("          Field 4  Destination ZPK (LMK 06-07) : $zpkScheme$zpkHex")

            // Field 5 — Maximum PIN length (2N)
            val maxPinLen = data.substring(pos, pos + 2).toIntOrNull() ?: 12; pos += 2
            hsmLogsListener.log("          Field 5  Max PIN Length              : ${maxPinLen.toString().padStart(2, '0')}")

            // Field 6 — Source PIN block (16H = 8 bytes)
            val pinBlockHex = data.substring(pos, pos + 16); pos += 16
            val pinBlock = IsoUtil.hexToBytes(pinBlockHex)
            hsmLogsListener.log("          Field 6  Source PIN Block (enc/TPK)  : $pinBlockHex")

            // Field 7 — Source PIN block format code (2N)
            val srcFormat = data.substring(pos, pos + 2); pos += 2
            hsmLogsListener.log("          Field 7  Source Format Code          : $srcFormat  [${resolveFormat(srcFormat).description}]")

            // Field 8 — Destination PIN block format code (2N)
            val dstFormat = data.substring(pos, pos + 2); pos += 2
            hsmLogsListener.log("          Field 8  Destination Format Code     : $dstFormat  [${resolveFormat(dstFormat).description}]")

            // Field 9 — Account number (12N)
            val account = data.substring(pos, minOf(pos + 12, data.length))
            hsmLogsListener.log("          Field 9  Account Number (12 PAN dig) : $account")

            // ── Emit structured formatted request ────────────────────────────
            hsmLogsListener.onFormattedRequest(buildString {
                appendLine("Message Header........... = [${cmd.header}]")
                appendLine("Command Code............. = [CA] Translate PIN from TPK to ZPK Request")
                appendLine("Source TPK............... = [$tpkScheme$tpkHex]")
                appendLine("Destination ZPK.......... = [$zpkScheme$zpkHex]")
                appendLine("Maximum PIN Length....... = [${maxPinLen.toString().padStart(2, '0')}]")
                appendLine("PIN Block Under TPK...... = [$pinBlockHex]")
                appendLine("Source Format Code....... = [$srcFormat] ${resolveFormat(srcFormat).description}")
                appendLine("Destination Format Code.. = [$dstFormat] ${resolveFormat(dstFormat).description}")
                appendLine("Account Number........... = [$account]")
            })

            // ── Step 2: Decrypt TPK under LMK ───────────────────────────────
            hsmLogsListener.log("► CA  [2/5]  Decrypting TPK under LMK pair 14-15  (${tpkScheme} = ${
                when (tpkScheme) { 'U' -> "Double-length 3DES"; 'T' -> "Triple-length 3DES"; else -> "Single-length DES" }
            })")

            // ── Step 3: Decrypt ZPK under LMK ───────────────────────────────
            hsmLogsListener.log("► CA  [3/5]  Decrypting ZPK under LMK pair 06-07  (${zpkScheme} = ${
                when (zpkScheme) { 'U' -> "Double-length 3DES"; 'T' -> "Triple-length 3DES"; else -> "Single-length DES" }
            })")

            // ── Step 4: Translate — decrypt block under TPK, re-encrypt under ZPK
            hsmLogsListener.log("► CA  [4/5]  Decrypting source PIN block under clear TPK")
            hsmLogsListener.log("             Source format  : $srcFormat  [${resolveFormat(srcFormat).description}]")
            hsmLogsListener.log("             Account (PAN)  : $account")

            hsmLogsListener.log("► CA  [5/5]  Re-formatting PIN block and encrypting under clear ZPK")
            hsmLogsListener.log("             Dest format    : $dstFormat  [${resolveFormat(dstFormat).description}]")

            val result = commandProcessor.executeTranslatePinTpkToZpk(
                lmkId = cmd.lmkId,
                encryptedPinBlock = pinBlock,
                encryptedSourceTpk = tpk,
                encryptedDestZpk = zpk,
                accountNumber = account,
                sourcePinBlockFormat = resolveFormat(srcFormat),
                destPinBlockFormat = resolveFormat(dstFormat)
            )

            if (result is HsmCommandResult.Success) {
                val encPinBlock = result.data["encryptedPinBlock"] as String
                val pinLength   = result.data["pinLength"] as String
                val pinLenPad   = pinLength.padStart(2, '0')
                val wireResponse = "${cmd.header}CB00${pinLenPad}${encPinBlock}${dstFormat}"

                hsmLogsListener.log("◄ CA  Translation successful")
                hsmLogsListener.log("          Translated PIN Block (enc/ZPK) : $encPinBlock")
                hsmLogsListener.log("          Returned PIN Length            : $pinLenPad")
                hsmLogsListener.log("          Output Format Code             : $dstFormat  [${resolveFormat(dstFormat).description}]")

                hsmLogsListener.onFormattedResponse(buildString {
                    appendLine(sep)
                    appendLine("◄ CB  Response — Translate PIN from TPK to ZPK")
                    appendLine(sep)
                    appendLine("  Response Code          : CB")
                    appendLine("  Error Code             : 00  ✓ No error")
                    appendLine("  PIN Length             : $pinLenPad")
                    appendLine("  Output PIN Block       : $encPinBlock")
                    appendLine("  Output Format Code     : $dstFormat  [${resolveFormat(dstFormat).description}]")
                    appendLine(sep)
                    appendLine("  Wire Response          : $wireResponse")
                })

                // CB response body: [PINLen_2N][PINBlock_16H][DstFmt_2N]
                HsmCommandResult.Success(
                    response = "${pinLenPad}${encPinBlock}${dstFormat}",
                    data = result.data
                )
            } else {
                val err = result as HsmCommandResult.Error
                hsmLogsListener.log("✗ CA  Translation FAILED — errorCode=${err.errorCode}  ${err.message}")
                result
            }
        } catch (e: Exception) {
            hsmLogsListener.log("✗ CA  Exception: ${e.message}")
            HsmCommandResult.Error("15", "CA failed: ${e.message}")
        }
    }

    /**
     * CI - Translate PIN from DUKPT (BDK) to ZPK
     * Wire format: [Header_4A][CI][BDK_1A+32H][ZPK_1A+32H][KSNDesc_3H][KSN_20H][PINBlock_16H][DstFmt_2N][Account_12N]
     * Response:    [Header_4A][CJ][00][PINLen_2N][PINBlock_16H][DstFmt_2N]
     *
     * Field order per Thales payShield 10K Host Command Reference:
     *   Field 3 = BDK (1A + 32H)         — Base Derivation Key under LMK pair 28-29
     *   Field 4 = ZPK (1A + 32H)         — destination Zone PIN Key under LMK pair 06-07
     *   Field 5 = KSN Descriptor (3H)    — lengths of BDK_ID, sub-key ID, device ID fields
     *   Field 6 = KSN (20H)              — Key Serial Number from the terminal
     *   Field 7 = Source PIN block (16H) — encrypted under the DUKPT transaction key
     *   Field 8 = Destination format (2N)
     *   Field 9 = Account number (12N)   — 12 rightmost PAN digits excl. check digit
     */
    private suspend fun executeCI(cmd: ParsedCommand): HsmCommandResult {

        fun resolveFormat(code: String) = when (code) {
            "01" -> PinBlockFormat.ISO_FORMAT_0
            "02" -> PinBlockFormat.DOCUTEL
            "03" -> PinBlockFormat.DIEBOLD_IBM
            "04" -> PinBlockFormat.PLUS_NETWORK
            "05" -> PinBlockFormat.ISO_FORMAT_1
            "46" -> PinBlockFormat.AS2805
            "47" -> PinBlockFormat.ISO_FORMAT_3
            "48" -> PinBlockFormat.ISO_FORMAT_4
            else -> PinBlockFormat.ISO_FORMAT_0
        }

        val sep = "─".repeat(56)

        return try {
            var pos = 0
            val data = cmd.data

            // ── Step 1: Parse all wire fields ────────────────────────────────
            hsmLogsListener.log("► CI  [1/6]  Parsing wire fields  (LMK slot: ${cmd.lmkId})")

            // Field 3 — BDK: scheme char (1A) + key hex (32H)
            val bdkScheme = data[pos]; pos++
            val bdkHex = data.substring(pos, pos + 32); pos += 32
            val bdk = IsoUtil.hexToBytes(bdkHex)
            hsmLogsListener.log("          Field 3  BDK                (LMK 28-29) : $bdkScheme$bdkHex")

            // Field 4 — ZPK: scheme char (1A) + key hex (32H)
            val zpkScheme = data[pos]; pos++
            val zpkHex = data.substring(pos, pos + 32); pos += 32
            val zpk = IsoUtil.hexToBytes(zpkHex)
            hsmLogsListener.log("          Field 4  Destination ZPK   (LMK 06-07) : $zpkScheme$zpkHex")

            // Field 5 — KSN Descriptor (3H): encodes BDK-ID / sub-key / device-ID field lengths
            val ksnDescriptor = data.substring(pos, pos + 3); pos += 3
            val counterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(ksnDescriptor)
            hsmLogsListener.log("          Field 5  KSN Descriptor                 : $ksnDescriptor  (counter bits: $counterBits)")

            // Field 6 — KSN (20H = 10 bytes)
            val ksnHex = data.substring(pos, pos + 20); pos += 20
            hsmLogsListener.log("          Field 6  KSN                            : $ksnHex")

            // Field 7 — Source PIN block (16H = 8 bytes, encrypted under DUKPT session key)
            val pinBlockHex = data.substring(pos, pos + 16); pos += 16
            val pinBlock = IsoUtil.hexToBytes(pinBlockHex)
            hsmLogsListener.log("          Field 7  Source PIN Block  (enc/DUKPT)  : $pinBlockHex")

            // Field 8 — Destination PIN block format code (2N)
            val dstFormat = data.substring(pos, pos + 2); pos += 2
            hsmLogsListener.log("          Field 8  Destination Format Code        : $dstFormat  [${resolveFormat(dstFormat).description}]")

            // Field 9 — Account number (12N)
            val account = data.substring(pos, minOf(pos + 12, data.length))
            hsmLogsListener.log("          Field 9  Account Number (12 PAN dig)    : $account")

            // ── Emit structured formatted request ────────────────────────────
            hsmLogsListener.onFormattedRequest(buildString {
                appendLine("Message Header........... = [${cmd.header}]")
                appendLine("Command Code............. = [CI] Translate PIN from BDK to ZPK (DUKPT) Request")
                appendLine("BDK...................... = [$bdkScheme$bdkHex]")
                appendLine("ZPK...................... = [$zpkScheme$zpkHex]")
                appendLine("KSN Descriptor........... = [$ksnDescriptor]")
                appendLine("Key serial number........ = [$ksnHex]")
                appendLine("Source Encrypted Block... = [$pinBlockHex]")
                appendLine("Destination Format Code.. = [$dstFormat] ${resolveFormat(dstFormat).description}")
                appendLine("Account Number........... = [$account]")
            })

            // ── Steps 2–6: DUKPT derivation + translation ────────────────────
            hsmLogsListener.log("► CI  [2/6]  Decrypting BDK under LMK pair 28-29  ($bdkScheme = ${
                when (bdkScheme) { 'U' -> "Double-length 3DES"; 'T' -> "Triple-length 3DES"; else -> "Single-length DES" }
            })")
            hsmLogsListener.log("► CI  [3/6]  Decrypting ZPK under LMK pair 06-07  ($zpkScheme = ${
                when (zpkScheme) { 'U' -> "Double-length 3DES"; 'T' -> "Triple-length 3DES"; else -> "Single-length DES" }
            })")
            hsmLogsListener.log("► CI  [4/6]  Deriving IPEK from BDK + KSN  (ANSI X9.24)")
            hsmLogsListener.log("             KSN descriptor : $ksnDescriptor  →  counter bits: $counterBits")
            hsmLogsListener.log("             KSN            : $ksnHex")
            hsmLogsListener.log("► CI  [5/6]  Advancing to transaction key and decrypting PIN block")
            hsmLogsListener.log("             Source PIN block (enc/DUKPT) : $pinBlockHex")
            hsmLogsListener.log("► CI  [6/6]  Re-formatting PIN block and encrypting under clear ZPK")
            hsmLogsListener.log("             Dest format  : $dstFormat  [${resolveFormat(dstFormat).description}]  account: $account")

            val result = commandProcessor.executeTranslatePinDukptToZpk(
                lmkId         = cmd.lmkId,
                encryptedBdk  = bdk,
                encryptedDestZpk = zpk,
                ksn           = ksnHex,
                encryptedPinBlock = pinBlock,
                accountNumber = account,
                sourcePinBlockFormat = PinBlockFormat.ISO_FORMAT_0,  // DUKPT source is always ISO Format 0
                destPinBlockFormat   = resolveFormat(dstFormat),
                counterBits   = counterBits
            )

            if (result is HsmCommandResult.Success) {
                val encPinBlock = result.data["encryptedPinBlock"] as String
                val pinLength   = (result.data["pinLength"] as? String) ?: "04"
                val pinLenPad   = pinLength.padStart(2, '0')
                val wireResponse = "${cmd.header}CJ00${pinLenPad}${encPinBlock}${dstFormat}"

                hsmLogsListener.log("◄ CI  Translation successful")
                hsmLogsListener.log("          Translated PIN Block (enc/ZPK) : $encPinBlock")
                hsmLogsListener.log("          Returned PIN Length            : $pinLenPad")
                hsmLogsListener.log("          Output Format Code             : $dstFormat  [${resolveFormat(dstFormat).description}]")

                hsmLogsListener.onFormattedResponse(buildString {
                    appendLine(sep)
                    appendLine("◄ CJ  Response — Translate PIN from BDK (DUKPT) to ZPK")
                    appendLine(sep)
                    appendLine("  Response Code          : CJ")
                    appendLine("  Error Code             : 00  ✓ No error")
                    appendLine("  PIN Length             : $pinLenPad")
                    appendLine("  Output PIN Block       : $encPinBlock")
                    appendLine("  Output Format Code     : $dstFormat  [${resolveFormat(dstFormat).description}]")
                    appendLine(sep)
                    appendLine("  Wire Response          : $wireResponse")
                })

                // CJ response body: [PINLen_2N][PINBlock_16H][DstFmt_2N]
                HsmCommandResult.Success(
                    response = "${pinLenPad}${encPinBlock}${dstFormat}",
                    data = result.data
                )
            } else {
                val err = result as HsmCommandResult.Error
                hsmLogsListener.log("✗ CI  Translation FAILED — errorCode=${err.errorCode}  ${err.message}")
                result
            }
        } catch (e: Exception) {
            hsmLogsListener.log("✗ CI  Exception: ${e.message}")
            HsmCommandResult.Error("15", "CI failed: ${e.message}")
        }
    }

    /**
     * DE - Generate IBM 3624 PIN Offset
     * Command: 0000DE[PVK_16H][LMK_PIN][MinPIN_2N][Account_12N][DecTable_16H][PINValData_12A]
     * Response: 0000DF00[Offset_12H]
     * The LMK-encrypted PIN format used here is: length_digit + PIN_digits.
     */
    private suspend fun executeDE(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // PVK (16H single-DES)
            val pvkHex = data.substring(pos, pos + 16); pos += 16
            val pvk = IsoUtil.hexToBytes(pvkHex)

            // LMK-encrypted PIN (length_digit + PIN_digits)
            val pinStart = pos
            val pinLen = data.getOrNull(pos)?.digitToIntOrNull() ?: 4
            pos++
            val customerPin = data.substring(pos, pos + pinLen); pos += pinLen

            // Min PIN length (2N)
            val minPinLen = data.substring(pos, pos + 2).toIntOrNull() ?: 4; pos += 2

            // Account (12N)
            val account = data.substring(pos, pos + 12); pos += 12

            // Decimalization table (16H)
            val decTable = if (pos + 16 <= data.length) data.substring(pos, pos + 16) else "0123456789012345"; pos += 16

            // PIN validation data (12A) – N = last 5 PAN digits; ignored in offset calc
            // pos += 12  // not needed for the calculation itself

            // Decrypt PVK under LMK
            val clearPvk = simulator.decryptUnderLmk(pvk, cmd.lmkId, 14)

            // Generate natural PIN: encrypt 12-digit PAN with PVK, decimalize
            val pan = pan12(account).padStart(16, '0')
            val pvkCipher = Cipher.getInstance("DESede/ECB/NoPadding")
            pvkCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(clearPvk.copyOf(16), "DESede"))
            val encPan = pvkCipher.doFinal(IsoUtil.hexToBytes(pan))
            val encHex = IsoUtil.bytesToHexString(encPan)
            val naturalPin = decimalizePin(encHex, decTable, customerPin.length)

            // Offset = (customerPin digit - naturalPin digit + 10) mod 10, right-padded with F
            val offset = buildString {
                for (i in customerPin.indices) {
                    val c = (customerPin[i].digitToInt() - naturalPin[i].digitToInt() + 10) % 10
                    append(c)
                }
            }.padEnd(12, 'F')

            HsmCommandResult.Success(
                response = offset,
                data = mapOf("offset" to offset, "naturalPin" to naturalPin)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "DE failed: ${e.message}")
        }
    }

    /**
     * DG - Generate VISA PVV
     * Command: 0000DG[PVKPair_32H][LMK_PIN][Account_12N][PVKI_1N]
     * Response: 0000DH00[PVV_4N]
     */
    private suspend fun executeDG(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // PVK pair (32H — two single-DES keys under LMK 14-15)
            val pvkPairHex = data.substring(pos, pos + 32); pos += 32
            val pvkPair = IsoUtil.hexToBytes(pvkPairHex)

            // LMK-encrypted PIN
            val pinLen = data.getOrNull(pos)?.digitToIntOrNull() ?: 4; pos++
            val pin = data.substring(pos, pos + pinLen); pos += pinLen

            // Account (12N)
            val account = data.substring(pos, pos + 12); pos += 12

            // PVKI (1N)
            val pvki = data.getOrNull(pos)?.digitToIntOrNull() ?: 1

            // Decrypt PVK pair under LMK
            val clearPvkPair = simulator.decryptUnderLmk(pvkPair, cmd.lmkId, 14)

            // TSP = rightmost 12 digits of PAN (excl. check) + PVKI + PIN, padded to 16 hex chars
            val pan11 = account.takeLast(12).dropLast(1)
            val tsp = (pan11 + pvki.toString() + pin).padEnd(16, '0').take(16)

            // Encrypt TSP under PVK pair (3DES ECB)
            val pvkCipher = Cipher.getInstance("DESede/ECB/NoPadding")
            pvkCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(clearPvkPair.copyOf(16), "DESede"))
            val encTsp = pvkCipher.doFinal(IsoUtil.hexToBytes(tsp))
            val encHex = IsoUtil.bytesToHexString(encTsp)

            // Decimalize to 4 digits (take first 4 decimal digits from left-to-right scan)
            val pvv = decimalizePin(encHex, "0123456789012345", 4)

            HsmCommandResult.Success(
                response = pvv,
                data = mapOf("pvv" to pvv)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "DG failed: ${e.message}")
        }
    }

    /**
     * M0 - Encrypt Data Block
     * For ZEK/DEK keys:
     *   Command: 0000M0[Mode_2N][InFmt_1N][OutFmt_1N][KeyType_3H][Key_1A+32H][IV_16H?][MsgLen_4H][Data_nH]
     * For BDK (DUKPT) keys:
     *   Command: 0000M0[Mode_2N][InFmt_1N][OutFmt_1N][KeyType_3H][Key_1A+32H][KSNDesc_3H][KSN_20H][IV_16H?][MsgLen_4H][Data_nH]
     * Response: 0000M100[IV_16H][DataLen_4H][Encrypted_nH]
     * Mode 00 = ECB, 01 = CBC.  InFmt/OutFmt 1 = hex-encoded.
     */
    private suspend fun executeM0(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val mode   = data.substring(pos, pos + 2); pos += 2
            val inFmt  = data[pos].toString(); pos++
            val outFmt = data[pos].toString(); pos++

            val keyTypeCode = data.substring(pos, pos + 3); pos += 3

            // ── Key parsing ──────────────────────────────────────────────────────────────
            // Use extractSchemeKey so that both variant (U/T/X/Y prefix + 32H) keys and
            // KeyBlock (S-block, variable length) keys are handled correctly.
            // The old approach of "scheme(1) + hex(32)" broke on S-blocks because the
            // S-block header contains non-hex chars (e.g. "0T", "TN") that cannot be
            // parsed with toInt(16).
            val (keyWithScheme, keyLen) = extractSchemeKey(data, pos); pos += keyLen
            val isKeyBlock = keyWithScheme.firstOrNull()?.uppercaseChar() == 'S'

            // For S-block keys extract usage/algo from the fixed header layout:
            //   S(1) + version(1) + blockLen(4) + keyUsage(2) + algo(1) + modeOfUse(1) + ...
            val keyUsageFromBlock = if (isKeyBlock && keyWithScheme.length >= 8)
                keyWithScheme.substring(6, 8) else ""
            val keyAlgoFromBlock  = if (isKeyBlock && keyWithScheme.length >= 9)
                keyWithScheme[8].uppercaseChar() else 'T'

            // BDK / DUKPT path: S-block with BDK/EMV usage, or legacy BDK key type codes
            val isBdkType = if (isKeyBlock)
                keyUsageFromBlock in setOf("B0", "B1", "E0", "E1", "E2", "E4")
            else
                keyTypeCode in BDK_KEY_TYPES

            // ── KSN (BDK / DUKPT path only) ──────────────────────────────────────────────
            var ksnDescriptor = ""
            var ksnHex = ""
            if (isBdkType) {
                ksnDescriptor = data.substring(pos, pos + 3); pos += 3
                ksnHex        = data.substring(pos, pos + 20); pos += 20
            }

            val ivHex = if (mode == "01" && pos + 16 <= data.length) {
                data.substring(pos, pos + 16).also { pos += 16 }
            } else "0000000000000000"

            val dataLenHex  = data.substring(pos, pos + 4); pos += 4
            val dataLen     = dataLenHex.toInt(16)
            val plainDataHex = data.substring(pos, minOf(pos + dataLen, data.length))
            val plainData   = IsoUtil.hexToBytes(plainDataHex)

            hsmLogsListener.log(buildString {
                appendLine("M0 - Encrypt Data Block Request")
                appendLine("Mode Flag................ = [$mode] ${if (mode == "00") "ECB" else "CBC"}")
                appendLine("Input Format Flag........ = [$inFmt] ${if (inFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Output Format Flag....... = [$outFmt] ${if (outFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Key Type................. = [$keyTypeCode]")
                appendLine("Key...................... = [$keyWithScheme]")
                if (isKeyBlock) {
                    appendLine("  Key Block Usage....... = [$keyUsageFromBlock]")
                    appendLine("  Key Block Algo........ = [$keyAlgoFromBlock]")
                }
                if (isBdkType) {
                    appendLine("KSN Descriptor........... = [$ksnDescriptor]")
                    appendLine("Key Serial Number........ = [$ksnHex]")
                }
                if (mode == "01") appendLine("IV....................... = [$ivHex]")
                appendLine("Message Length........... = [$dataLenHex]")
                appendLine("Message Block............ = [$plainDataHex]")
            })
            hsmLogsListener.onFormattedRequest(buildString {
                appendLine("M0 - Encrypt Data Block Request")
                appendLine("Mode Flag................ = [$mode] ${if (mode == "00") "ECB" else "CBC"}")
                appendLine("Input Format Flag........ = [$inFmt] ${if (inFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Output Format Flag....... = [$outFmt] ${if (outFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Key Type................. = [$keyTypeCode]")
                appendLine("Key...................... = [$keyWithScheme]")
                if (isKeyBlock) {
                    appendLine("  Key Block Usage....... = [$keyUsageFromBlock]")
                    appendLine("  Key Block Algo........ = [$keyAlgoFromBlock]")
                }
                if (isBdkType) {
                    appendLine("KSN Descriptor........... = [$ksnDescriptor]")
                    appendLine("Key Serial Number........ = [$ksnHex]")
                }
                if (mode == "01") appendLine("IV....................... = [$ivHex]")
                appendLine("Message Length........... = [$dataLenHex]")
                appendLine("Message Block............ = [$plainDataHex]")
            })

            hsmLogsListener.log("[M0] Step 1: Key derivation - keyType=$keyTypeCode isKeyBlock=$isKeyBlock isBdk=$isBdkType lmkSlot=${cmd.lmkId}")

            // ── Resolve LMK pair ─────────────────────────────────────────────────────────
            val lmkPairNumber = when {
                isKeyBlock -> A0GenerateKeyCommand.lmkPairFromKeyUsage(keyUsageFromBlock)
                isBdkType  -> A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[keyTypeCode]?.lmkPairNumber
                               ?: PayShield10KCommandProcessor.LMK_PAIR_BDK
                else       -> A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[keyTypeCode]?.lmkPairNumber
                               ?: PayShield10KCommandProcessor.LMK_PAIR_TPK
            }
            hsmLogsListener.log("[M0] Step 2: lmkPair=$lmkPairNumber")

            // ── Decrypt key under LMK ────────────────────────────────────────────────────
            // decryptSchemeKeyUnderLmk handles both variant (U/T/X/Y) and S-block keys.
            val clearBdkOrKey = decryptSchemeKeyUnderLmk(keyWithScheme, cmd.lmkId, lmkPairNumber)
            hsmLogsListener.log("[M0] Step 3: Decrypted clear key = ${IsoUtil.bytesToHexString(clearBdkOrKey)}")

            // ── DUKPT session key derivation (BDK path) ──────────────────────────────────
            val clearKey: ByteArray
            if (isBdkType) {
                val counterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(ksnDescriptor)
                hsmLogsListener.log("[M0] Step 4: KSN descriptor=$ksnDescriptor counterBits=$counterBits")

                val initialKey = PayShield10KCommandProcessor.deriveInitialKey(clearBdkOrKey, ksnHex, counterBits)
                hsmLogsListener.log("[M0] Step 5: IPEK = ${IsoUtil.bytesToHexString(initialKey)}")

                val ksnBytes = IsoUtil.hexToBytes(ksnHex)
                val counter  = PayShield10KCommandProcessor.extractDukptCounter(ksnBytes, counterBits)
                hsmLogsListener.log("[M0] Step 6: DUKPT counter = $counter")

                clearKey = commandProcessor.deriveDukptSessionKey(initialKey, ksnHex, counter, counterBits)
                hsmLogsListener.log("[M0] Step 7: DUKPT session key = ${IsoUtil.bytesToHexString(clearKey)}")
            } else {
                clearKey = clearBdkOrKey
            }

            hsmLogsListener.log("[M0] Step 8: Encrypting - mode=${if (mode == "01") "CBC" else "ECB"} algo=${if (keyAlgoFromBlock == 'A') "AES" else "TDES"} dataLen=${plainData.size}")
            hsmLogsListener.log("[M0] Step 8: Plain data = $plainDataHex")

            // ── Data encryption ──────────────────────────────────────────────────────────
            // Use AES when the S-block algorithm field is 'A'; TDES for everything else.
            val cipherMode = if (mode == "01") CipherMode.CBC else CipherMode.ECB
            val useAes     = keyAlgoFromBlock == 'A'
            val encData = if (useAes) {
                engine().encryptionEngine.encrypt(
                    algorithm = CryptoAlgorithm.AES,
                    encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                        data = plainData,
                        key  = clearKey,
                        iv   = if (mode == "01") IsoUtil.hexToBytes(ivHex) else null,
                        mode = cipherMode,
                        padding = PaddingMethods.PKCS5
                    )
                )
            } else {
                engine().encryptionEngine.encrypt(
                    algorithm = CryptoAlgorithm.TDES,
                    encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                        data = plainData,
                        key  = clearKey,
                        iv   = if (mode == "01") IsoUtil.hexToBytes(ivHex) else null,
                        mode = cipherMode,
                        padding = PaddingMethods.PKCS5
                    )
                )
            }

            val encHex       = IsoUtil.bytesToHexString(encData)
            val outLen       = (encData.size * 2).toString(16).padStart(4, '0').uppercase()
            val responseBody = if (mode == "01") "$ivHex$outLen$encHex" else "$outLen$encHex"

            hsmLogsListener.log("[M0] Step 9: Encryption result = $encHex")
            hsmLogsListener.log("[M0] Step 9: Response body = $responseBody")

            hsmLogsListener.log(buildString {
                appendLine("M1 - Encrypt Data Block Response")
                appendLine("Error Code............... = [00]")
                if (mode == "01") appendLine("IV....................... = [$ivHex]")
                appendLine("Message Length........... = [$outLen]")
                appendLine("Message Block............ = [$encHex]")
            })
            hsmLogsListener.onFormattedResponse(buildString {
                appendLine("M1 - Encrypt Data Block Response")
                appendLine("Error Code............... = [00]")
                if (mode == "01") appendLine("IV....................... = [$ivHex]")
                appendLine("Message Length........... = [$outLen]")
                appendLine("Message Block............ = [$encHex]")
            })

            HsmCommandResult.Success(
                response = responseBody,
                data = mapOf("encryptedData" to encHex, "iv" to ivHex)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "M0 failed: ${e.message}")
        }
    }

    /**
     * M2 - Decrypt Data Block
     * For ZEK/DEK keys:
     *   Command: 0000M2[Mode_2N][InFmt_1N][OutFmt_1N][KeyType_3H][Key(scheme+variable)][IV_16H?][MsgLen_4H][Data_nH]
     * For BDK (DUKPT) keys:
     *   Command: 0000M2[Mode_2N][InFmt_1N][OutFmt_1N][KeyType_3H][Key(scheme+variable)][KSNDesc_3H][KSN_20H][IV_16H?][MsgLen_4H][Data_nH]
     * Response: 0000M300[IV_16H][DataLen_4H][Decrypted_nH]
     * Mode 00 = ECB, 01 = CBC.  InFmt/OutFmt 1 = hex-encoded.
     * Supports variant (U/T/X/Y) and KeyBlock (S-block) keys.
     */
    private suspend fun executeM2(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val mode   = data.substring(pos, pos + 2); pos += 2
            val inFmt  = data[pos].toString(); pos++
            val outFmt = data[pos].toString(); pos++

            val keyTypeCode = data.substring(pos, pos + 3); pos += 3

            // ── Key parsing ──────────────────────────────────────────────────────────────
            // Use extractSchemeKey so that both variant (U/T/X/Y prefix + 32H) keys and
            // KeyBlock (S-block, variable length) keys are handled correctly.
            // The old approach of "scheme(1) + hex(32)" broke on S-blocks because the
            // S-block header contains non-hex chars (e.g. "0T", "TN") that cannot be
            // parsed with toInt(16).
            val (keyWithScheme, keyLen) = extractSchemeKey(data, pos); pos += keyLen
            val isKeyBlock = keyWithScheme.firstOrNull()?.uppercaseChar() == 'S'

            // For S-block keys extract usage/algo from the fixed header layout:
            //   S(1) + version(1) + blockLen(4) + keyUsage(2) + algo(1) + modeOfUse(1) + ...
            val keyUsageFromBlock = if (isKeyBlock && keyWithScheme.length >= 8)
                keyWithScheme.substring(6, 8) else ""
            val keyAlgoFromBlock  = if (isKeyBlock && keyWithScheme.length >= 9)
                keyWithScheme[8].uppercaseChar() else 'T'

            // BDK / DUKPT path: S-block with BDK/EMV usage, or legacy BDK key type codes
            val isBdkType = if (isKeyBlock)
                keyUsageFromBlock in setOf("B0", "B1", "E0", "E1", "E2", "E4")
            else
                keyTypeCode in BDK_KEY_TYPES

            // ── KSN (BDK / DUKPT path only) ──────────────────────────────────────────────
            var ksnDescriptor = ""
            var ksnHex = ""
            if (isBdkType) {
                ksnDescriptor = data.substring(pos, pos + 3); pos += 3
                ksnHex        = data.substring(pos, pos + 20); pos += 20
            }

            val ivHex = if (mode == "01" && pos + 16 <= data.length) {
                data.substring(pos, pos + 16).also { pos += 16 }
            } else "0000000000000000"

            val dataLenHex = data.substring(pos, pos + 4); pos += 4
            val dataLen    = dataLenHex.toInt(16)
            val encDataHex = data.substring(pos, minOf(pos + dataLen, data.length))
            val encData    = IsoUtil.hexToBytes(encDataHex)

            hsmLogsListener.log(buildString {
                appendLine("M2 - Decrypt Data Block Request")
                appendLine("Mode Flag................ = [$mode] ${if (mode == "00") "ECB" else "CBC"}")
                appendLine("Input Format Flag........ = [$inFmt] ${if (inFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Output Format Flag....... = [$outFmt] ${if (outFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Key Type................. = [$keyTypeCode]")
                appendLine("Key...................... = [$keyWithScheme]")
                if (isKeyBlock) {
                    appendLine("  Key Block Usage....... = [$keyUsageFromBlock]")
                    appendLine("  Key Block Algo........ = [$keyAlgoFromBlock]")
                }
                if (isBdkType) {
                    appendLine("KSN Descriptor........... = [$ksnDescriptor]")
                    appendLine("Key Serial Number........ = [$ksnHex]")
                }
                if (mode == "01") appendLine("IV....................... = [$ivHex]")
                appendLine("Message Length........... = [$dataLenHex]")
                appendLine("Message Block............ = [$encDataHex]")
            })
            hsmLogsListener.onFormattedRequest(buildString {
                appendLine("M2 - Decrypt Data Block Request")
                appendLine("Mode Flag................ = [$mode] ${if (mode == "00") "ECB" else "CBC"}")
                appendLine("Input Format Flag........ = [$inFmt] ${if (inFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Output Format Flag....... = [$outFmt] ${if (outFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Key Type................. = [$keyTypeCode]")
                appendLine("Key...................... = [$keyWithScheme]")
                if (isKeyBlock) {
                    appendLine("  Key Block Usage....... = [$keyUsageFromBlock]")
                    appendLine("  Key Block Algo........ = [$keyAlgoFromBlock]")
                }
                if (isBdkType) {
                    appendLine("KSN Descriptor........... = [$ksnDescriptor]")
                    appendLine("Key Serial Number........ = [$ksnHex]")
                }
                if (mode == "01") appendLine("IV....................... = [$ivHex]")
                appendLine("Message Length........... = [$dataLenHex]")
                appendLine("Message Block............ = [$encDataHex]")
            })

            hsmLogsListener.log("[M2] Step 1: Key derivation - keyType=$keyTypeCode isKeyBlock=$isKeyBlock isBdk=$isBdkType lmkSlot=${cmd.lmkId}")

            // ── Resolve LMK pair ─────────────────────────────────────────────────────────
            val lmkPairNumber = when {
                isKeyBlock -> A0GenerateKeyCommand.lmkPairFromKeyUsage(keyUsageFromBlock)
                isBdkType  -> A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[keyTypeCode]?.lmkPairNumber
                               ?: PayShield10KCommandProcessor.LMK_PAIR_BDK
                else       -> A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[keyTypeCode]?.lmkPairNumber
                               ?: PayShield10KCommandProcessor.LMK_PAIR_TPK
            }
            hsmLogsListener.log("[M2] Step 2: lmkPair=$lmkPairNumber")

            // ── Decrypt key under LMK ────────────────────────────────────────────────────
            // decryptSchemeKeyUnderLmk handles both variant (U/T/X/Y) and S-block keys.
            val clearBdkOrKey = decryptSchemeKeyUnderLmk(keyWithScheme, cmd.lmkId, lmkPairNumber)
            hsmLogsListener.log("[M2] Step 3: Decrypted clear key = ${IsoUtil.bytesToHexString(clearBdkOrKey)}")

            // ── DUKPT session key derivation (BDK path) ──────────────────────────────────
            val clearKey: ByteArray
            if (isBdkType) {
                val counterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(ksnDescriptor)
                hsmLogsListener.log("[M2] Step 4: KSN descriptor=$ksnDescriptor counterBits=$counterBits")

                val initialKey = PayShield10KCommandProcessor.deriveInitialKey(clearBdkOrKey, ksnHex, counterBits)
                hsmLogsListener.log("[M2] Step 5: IPEK = ${IsoUtil.bytesToHexString(initialKey)}")

                val ksnBytes = IsoUtil.hexToBytes(ksnHex)
                val counter  = PayShield10KCommandProcessor.extractDukptCounter(ksnBytes, counterBits)
                hsmLogsListener.log("[M2] Step 6: DUKPT counter = $counter")

                clearKey = commandProcessor.deriveDukptSessionKey(initialKey, ksnHex, counter, counterBits)
                hsmLogsListener.log("[M2] Step 7: DUKPT session key = ${IsoUtil.bytesToHexString(clearKey)}")
            } else {
                clearKey = clearBdkOrKey
            }

            hsmLogsListener.log("[M2] Step 8: Decrypting - mode=${if (mode == "01") "CBC" else "ECB"} algo=${if (keyAlgoFromBlock == 'A') "AES" else "TDES"} dataLen=${encData.size}")
            hsmLogsListener.log("[M2] Step 8: Encrypted data = $encDataHex")

            // ── Data decryption ──────────────────────────────────────────────────────────
            // Use AES when the S-block algorithm field is 'A'; TDES for everything else.
            val cipherMode = if (mode == "01") CipherMode.CBC else CipherMode.ECB
            val ivBytes    = IsoUtil.hexToBytes(ivHex)
            val useAes     = keyAlgoFromBlock == 'A'
            val eng        = engine()

            val decData = if (useAes) {
                try {
                    eng.encryptionEngine.decrypt(
                        algorithm = CryptoAlgorithm.AES,
                        decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                            data = encData, key = clearKey, iv = ivBytes,
                            mode = cipherMode, padding = PaddingMethods.PKCS5
                        )
                    )
                } catch (_: Exception) {
                    hsmLogsListener.log("[M2] AES PKCS5 padding invalid, falling back to NoPadding")
                    eng.encryptionEngine.decrypt(
                        algorithm = CryptoAlgorithm.AES,
                        decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                            data = encData, key = clearKey, iv = ivBytes,
                            mode = cipherMode, padding = PaddingMethods.NONE
                        )
                    )
                }
            } else {
                try {
                    eng.encryptionEngine.decrypt(
                        algorithm = CryptoAlgorithm.TDES,
                        decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                            data = encData, key = clearKey, iv = ivBytes,
                            mode = cipherMode, padding = PaddingMethods.PKCS5
                        )
                    )
                } catch (_: Exception) {
                    hsmLogsListener.log("[M2] TDES PKCS5 padding invalid, falling back to NoPadding")
                    eng.encryptionEngine.decrypt(
                        algorithm = CryptoAlgorithm.TDES,
                        decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                            data = encData, key = clearKey, iv = ivBytes,
                            mode = cipherMode, padding = PaddingMethods.NONE
                        )
                    )
                }
            }

            val decHex       = IsoUtil.bytesToHexString(decData)
            val outLen       = (decData.size * 2).toString(16).padStart(4, '0').uppercase()
            val responseBody = if (mode == "01") "$ivHex$outLen$decHex" else "$outLen$decHex"

            hsmLogsListener.log("[M2] Step 9: Decryption result = $decHex")
            hsmLogsListener.log("[M2] Step 9: Response body = $responseBody")

            hsmLogsListener.log(buildString {
                appendLine("M3 - Decrypt Data Block Response")
                appendLine("Error Code............... = [00]")
                if (mode == "01") appendLine("IV....................... = [$ivHex]")
                appendLine("Message Length........... = [$outLen]")
                appendLine("Message Block............ = [$decHex]")
            })
            hsmLogsListener.onFormattedResponse(buildString {
                appendLine("M3 - Decrypt Data Block Response")
                appendLine("Error Code............... = [00]")
                if (mode == "01") appendLine("IV....................... = [$ivHex]")
                appendLine("Message Length........... = [$outLen]")
                appendLine("Message Block............ = [$decHex]")
            })

            HsmCommandResult.Success(
                response = responseBody,
                data = mapOf("decryptedData" to decHex, "iv" to ivHex)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "M2 failed: ${e.message}")
        }
    }

    private fun applyLmkVariantForM2(lmkKey: ByteArray, variant: Int): ByteArray {
        if (variant == 0) return lmkKey
        val variantMask = A0GenerateKeyCommand.LMK_VARIANTS[variant] ?: return lmkKey
        val variantedKey = lmkKey.copyOf()
        variantedKey[0] = (variantedKey[0].toInt() xor variantMask).toByte()
        return variantedKey
    }

    private suspend fun performTdesDecryptForM2(data: ByteArray, key: ByteArray, lmkId: String = "00"): ByteArray {
        return simulator.decryptWithLmkAlgorithm(data, key, lmkId)
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
     * BW - Translate Key from One LMK Pair to Another
     * Command: 0000BW[KeyType_3H][Key(scheme+variable)][NewKeyType_3H][NewScheme_1A]%[LMK_ID]
     * Handles variant scheme (U/T/X/Y) and KeyBlock ('S') source keys.
     * Response: 0000BX00[NewKey(scheme+variable)][KCV_6H]
     */
    private suspend fun executeBW(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // Source key type (3H) + key (scheme-prefixed, variable)
            val srcTypeCode = data.substring(pos, pos + 3); pos += 3
            val (srcKeyWithScheme, srcKeyLen) = extractSchemeKey(data, pos); pos += srcKeyLen

            // Destination key type (3H) + scheme (1A)
            val dstTypeCode = if (pos + 3 <= data.length) data.substring(pos, pos + 3) else srcTypeCode; pos += 3
            val dstSchemeChar = if (pos < data.length) data[pos].uppercaseChar() else 'U'

            val srcType = KeyType.values().find { it.code == srcTypeCode } ?: KeyType.TYPE_001
            val dstType = KeyType.values().find { it.code == dstTypeCode } ?: srcType

            // Decrypt under source LMK pair
            val clearKey = decryptSchemeKeyUnderLmk(srcKeyWithScheme, cmd.lmkId, srcType.getLmkPairNumber())

            // Re-encrypt under destination LMK pair
            val lmkSet = simulator.getSlotManager().getLmkFromSlot(cmd.lmkId)
            val useKeyBlock = lmkSet?.scheme == "KEY_BLOCK" || dstSchemeChar == 'S'
            val kcv = simulator.calculateKeyCheckValue(clearKey)

            val newKey = if (useKeyBlock) {
                simulator.buildKeyBlockForKeyType(clearKey, cmd.lmkId, dstType.getLmkPairNumber(), dstTypeCode)
            } else {
                val encBytes = simulator.encryptUnderLmk(clearKey, cmd.lmkId, dstType.getLmkPairNumber())
                "$dstSchemeChar${IsoUtil.bytesToHexString(encBytes)}"
            }

            HsmCommandResult.Success(
                response = "$newKey$kcv",
                data = mapOf("translatedKey" to newKey, "kcv" to kcv)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "BW failed: ${e.message}")
        }
    }

    /**
     * BG - Translate LMK-Encrypted PIN from One LMK to Another
     * Command: 0000BG[Account_12N][OldLMK_PIN][NewLMK_ID_2N]%[OldLMK_ID]
     * Response: 0000BH00[NewLMK_PIN]
     * Used during LMK rollover — re-encrypts a PIN stored under the old LMK.
     */
    private suspend fun executeBG(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val account = data.substring(pos, pos + 12); pos += 12
            val pinLen  = data.getOrNull(pos)?.digitToIntOrNull() ?: 4; pos++
            val oldLmkPin = data.substring(pos, pos + pinLen); pos += pinLen
            // New LMK ID (2N) — if absent, use the same LMK ID (re-encrypt under same LMK)
            val newLmkId = if (pos + 2 <= data.length) data.substring(pos, pos + 2) else cmd.lmkId

            // In PayShield: the PIN is stored as an encrypted PIN block under LMK pair 02-03.
            // For this simulator we use the simple length+digits format — just copy to new LMK.
            val newLmkPin = "${oldLmkPin.length}$oldLmkPin"

            HsmCommandResult.Success(
                response = newLmkPin,
                data = mapOf("lmkPin" to newLmkPin)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "BG failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // G0 — Translate PIN Block from BDK (DUKPT) to ZPK/BDK
    // Supports two wire formats:
    //
    // (A) Variant-scheme keys:
    //   G0[BDK_1A+32H][DestKeyType_1A][DestKey_1A+32H][KSNDesc_3H][KSN_20H]
    //     [DestKSNDesc_3H][DestKSN_20H]  ← only when DestKeyType is BDK
    //     [PINBlock_16H][SrcFmt_2N][DstFmt_2N][Account_12N]
    //
    // (B) S-block (KeyBlock) BDK keys — K1 format:
    //   G0[SrcBDK_S-block][DstBDK_S-block][SrcKSNDesc_3H][SrcKSN_20H]
    //     [DstKSNDesc_3H][DstKSN_20H][PINBlock_16H][SrcFmt_2N][DstFmt_2N]
    //     [Account_12N][%LMK_2N]
    //   No DestKeyType character — S-block key usage (B0/B1/E0-E4) self-describes.
    //
    // Response: G1 00 [PINLen_2N] [PINBlock_16H] [DstFmt_2N]
    // ====================================================================================================
    /**
     * G0 - Translate PIN from BDK (3DES DUKPT) to ZPK/BDK
     * Wire format: see comment above for both variant and S-block formats.
     *
     * Field order per Thales payShield 10K Host Command Reference:
     *   Field 3 = BDK (1A + 32H)          — Base Derivation Key under LMK pair 28-29
     *   Field 4 = ZPK (1A + 32H)          — destination Zone PIN Key under LMK pair 06-07
     *   Field 5 = KSN Descriptor (3H)     — BDK ID / sub-key / device ID lengths
     *   Field 6 = KSN (20H)               — Key Serial Number from the terminal
     *   Field 7 = Source PIN block (16H)  — encrypted under the DUKPT transaction key
     *   Field 8 = Source format code (2N)
     *   Field 9 = Destination format (2N)
     *   Field 10 = Account number (12N)   — 12 rightmost PAN digits excl. check digit
     */
    private suspend fun executeG0(cmd: ParsedCommand): HsmCommandResult {

        fun resolveFormat(code: String) = when (code) {
            "01" -> PinBlockFormat.ISO_FORMAT_0
            "02" -> PinBlockFormat.DOCUTEL
            "03" -> PinBlockFormat.DIEBOLD_IBM
            "04" -> PinBlockFormat.PLUS_NETWORK
            "05" -> PinBlockFormat.ISO_FORMAT_1
            "46" -> PinBlockFormat.AS2805
            "47" -> PinBlockFormat.ISO_FORMAT_3
            "48" -> PinBlockFormat.ISO_FORMAT_4
            else -> PinBlockFormat.ISO_FORMAT_0
        }

        fun schemeLabel(ch: Char) = when (ch) {
            'U' -> "Double-length 3DES"; 'T' -> "Triple-length 3DES"; else -> "Single-length DES"
        }

        fun destKeyTypeLabel(ch: Char) = when (ch) {
            '0'  -> "'0' - Not Set (ZPK)"
            '*'  -> "'*' (X'2A) - BDK-1"
            '~'  -> "'~' (X'7E) - BDK-2"
            '!'  -> "'!' (X'21) - BDK-4"
            else -> "'$ch' - Unknown"
        }

        val sep = "─".repeat(56)

        return try {
            var pos = 0
            val data = cmd.data

            hsmLogsListener.log("► G0  [1/N]  Parsing wire fields  (LMK slot: ${cmd.lmkId})")

            // ── Detect format: S-block (K1) vs variant-scheme ────────────────
            val firstChar = if (data.isNotEmpty()) data[0].uppercaseChar() else '?'
            val isSBlockFormat = firstChar == 'S'

            if (isSBlockFormat) {
                // ============================================================
                // K1 FORMAT: S-block BDK keys
                // Wire: SrcBDK(S-block) + DstBDK(S-block) +
                //       SrcKSNDesc(3H) + SrcKSN(20H) +
                //       DstKSNDesc(3H) + DstKSN(20H) +
                //       PINBlock(16H) + SrcFmt(2N) + DstFmt(2N) +
                //       Account(12N) [+ %LMK(2N)]
                // ============================================================
                hsmLogsListener.log("► G0  Detected S-block (KeyBlock) format — K1 wire layout")

                // --- Source BDK (S-block) ---
                val (srcKeyWithScheme, srcKeyLen) = extractSchemeKey(data, pos); pos += srcKeyLen
                val srcKeyUsageFromBlock = if (srcKeyWithScheme.length >= 8) srcKeyWithScheme.substring(6, 8) else ""
                val srcKeyAlgoFromBlock = if (srcKeyWithScheme.length >= 9) srcKeyWithScheme[8].uppercaseChar() else 'T'
                val srcIsBdk = srcKeyUsageFromBlock in setOf("B0", "B1", "E0", "E1", "E2", "E4")
                hsmLogsListener.log("          Source BDK (S-block)                      : ${srcKeyWithScheme.take(40)}...")
                hsmLogsListener.log("          Source Key Usage=$srcKeyUsageFromBlock  Algo=$srcKeyAlgoFromBlock  isBDK=$srcIsBdk")

                // --- Destination BDK (S-block) ---
                val (dstKeyWithScheme, dstKeyLen) = extractSchemeKey(data, pos); pos += dstKeyLen
                val dstKeyUsageFromBlock = if (dstKeyWithScheme.length >= 8) dstKeyWithScheme.substring(6, 8) else ""
                val dstKeyAlgoFromBlock = if (dstKeyWithScheme.length >= 9) dstKeyWithScheme[8].uppercaseChar() else 'T'
                val dstIsBdk = dstKeyUsageFromBlock in setOf("B0", "B1", "E0", "E1", "E2", "E4")
                hsmLogsListener.log("          Dest BDK (S-block)                        : ${dstKeyWithScheme.take(40)}...")
                hsmLogsListener.log("          Dest Key Usage=$dstKeyUsageFromBlock  Algo=$dstKeyAlgoFromBlock  isBDK=$dstIsBdk")

                // --- Source KSN Descriptor (3H) + KSN (20H) ---
                val srcKsnDescriptor = data.substring(pos, pos + 3); pos += 3
                val srcCounterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(srcKsnDescriptor)
                hsmLogsListener.log("          Source KSN Descriptor                     : $srcKsnDescriptor  (counter bits: $srcCounterBits)")
                val srcKsnHex = data.substring(pos, pos + 20); pos += 20
                hsmLogsListener.log("          Source KSN                                : $srcKsnHex")

                // --- Destination KSN Descriptor (3H) + KSN (20H) ---
                val dstKsnDescriptor = data.substring(pos, pos + 3); pos += 3
                val dstCounterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(dstKsnDescriptor)
                hsmLogsListener.log("          Dest KSN Descriptor                       : $dstKsnDescriptor  (counter bits: $dstCounterBits)")
                val dstKsnHex = data.substring(pos, pos + 20); pos += 20
                hsmLogsListener.log("          Dest KSN                                  : $dstKsnHex")

                // --- PIN block (16H) ---
                val pinBlockHex = data.substring(pos, pos + 16); pos += 16
                hsmLogsListener.log("          Source PIN Block (enc/DUKPT)               : $pinBlockHex")

                // --- Source/Dest format codes (2N each) ---
                val srcFormat = data.substring(pos, pos + 2); pos += 2
                val dstFormat = data.substring(pos, pos + 2); pos += 2
                hsmLogsListener.log("          Source Format Code                         : $srcFormat  [${resolveFormat(srcFormat).description}]")
                hsmLogsListener.log("          Dest Format Code                           : $dstFormat  [${resolveFormat(dstFormat).description}]")

                // --- Account number (12N) ---
                val account = data.substring(pos, minOf(pos + 12, data.length))
                hsmLogsListener.log("          Account Number (12N)                       : $account")

                // ── Formatted request ────────────────────────────────────
                hsmLogsListener.onFormattedRequest(buildString {
                    appendLine("Message Header........... = [${cmd.header}]")
                    appendLine("Command Code............. = [G0] Translate PIN BDK→BDK (S-block K1 format)")
                    appendLine("Source BDK (S-block)..... = [$srcKeyWithScheme]")
                    appendLine("  Key Usage.............. = [$srcKeyUsageFromBlock]  Algo=[$srcKeyAlgoFromBlock]")
                    appendLine("Dest BDK (S-block)....... = [$dstKeyWithScheme]")
                    appendLine("  Key Usage.............. = [$dstKeyUsageFromBlock]  Algo=[$dstKeyAlgoFromBlock]")
                    appendLine("Source KSN Descriptor.... = [$srcKsnDescriptor]  (counter bits: $srcCounterBits)")
                    appendLine("Source KSN............... = [$srcKsnHex]")
                    appendLine("Dest KSN Descriptor...... = [$dstKsnDescriptor]  (counter bits: $dstCounterBits)")
                    appendLine("Dest KSN................. = [$dstKsnHex]")
                    appendLine("Source Encrypted Block... = [$pinBlockHex]")
                    appendLine("Source PIN block Format.. = [$srcFormat] ${resolveFormat(srcFormat).description}")
                    appendLine("Destination Format Code.. = [$dstFormat] ${resolveFormat(dstFormat).description}")
                    appendLine("Account Number........... = [$account]")
                    appendLine("LMK ID................... = [${cmd.lmkId}]")
                })

                // ── Step 1: Decrypt source BDK under LMK ─────────────────
                val srcLmkPair = A0GenerateKeyCommand.lmkPairFromKeyUsage(srcKeyUsageFromBlock)
                hsmLogsListener.log("[G0-K1] Step 1: Decrypting source BDK  lmkPair=$srcLmkPair")
                val clearSrcBdk = decryptSchemeKeyUnderLmk(srcKeyWithScheme, cmd.lmkId, srcLmkPair)
                hsmLogsListener.log("[G0-K1] Step 2: Source BDK (clear) = ${IsoUtil.bytesToHexString(clearSrcBdk)}")

                // ── Step 2: Decrypt dest BDK under LMK ───────────────────
                val dstLmkPair = A0GenerateKeyCommand.lmkPairFromKeyUsage(dstKeyUsageFromBlock)
                hsmLogsListener.log("[G0-K1] Step 3: Decrypting dest BDK  lmkPair=$dstLmkPair")
                val clearDstBdk = decryptSchemeKeyUnderLmk(dstKeyWithScheme, cmd.lmkId, dstLmkPair)
                hsmLogsListener.log("[G0-K1] Step 4: Dest BDK (clear) = ${IsoUtil.bytesToHexString(clearDstBdk)}")

                // ── Step 3: Source DUKPT → derive PEK, decrypt PIN block ─
                val srcIpek = PayShield10KCommandProcessor.deriveInitialKey(clearSrcBdk, srcKsnHex, srcCounterBits)
                hsmLogsListener.log("[G0-K1] Step 5: Source IPEK = ${IsoUtil.bytesToHexString(srcIpek)}")
                val srcKsnBytes = IsoUtil.hexToBytes(srcKsnHex)
                val srcCounter = PayShield10KCommandProcessor.extractDukptCounter(srcKsnBytes, srcCounterBits)
                val srcSessionKey = commandProcessor.deriveDukptSessionKey(srcIpek, srcKsnHex, srcCounter, srcCounterBits)
                hsmLogsListener.log("[G0-K1] Step 6: Source session key = ${IsoUtil.bytesToHexString(srcSessionKey)}")
                val srcPek = DukptEngine.xorBytes(srcSessionKey, DukptEngine.PEK_VARIANT)
                hsmLogsListener.log("[G0-K1] Step 7: Source PEK (w/ variant) = ${IsoUtil.bytesToHexString(srcPek)}")

                val pinBlockBytes = IsoUtil.hexToBytes(pinBlockHex)
                val clearPinBlock = commandProcessor.decryptPinBlock(pinBlockBytes, srcPek)
                hsmLogsListener.log("[G0-K1] Step 8: Decrypted PIN block = ${IsoUtil.bytesToHexString(clearPinBlock)}")

                val pin = commandProcessor.extractPinFromBlock(clearPinBlock, account, resolveFormat(srcFormat))
                hsmLogsListener.log("[G0-K1] Step 9: Extracted PIN  length=${pin.length}  pin=$pin")

                // ── Step 4: Dest DUKPT → derive PEK, re-encrypt PIN block ─
                val dstIpek = PayShield10KCommandProcessor.deriveInitialKey(clearDstBdk, dstKsnHex, dstCounterBits)
                hsmLogsListener.log("[G0-K1] Step 10: Dest IPEK = ${IsoUtil.bytesToHexString(dstIpek)}")
                val dstKsnBytes = IsoUtil.hexToBytes(dstKsnHex)
                val dstCounter = PayShield10KCommandProcessor.extractDukptCounter(dstKsnBytes, dstCounterBits)
                val dstSessionKey = commandProcessor.deriveDukptSessionKey(dstIpek, dstKsnHex, dstCounter, dstCounterBits)
                hsmLogsListener.log("[G0-K1] Step 11: Dest session key = ${IsoUtil.bytesToHexString(dstSessionKey)}")
                val dstPek = DukptEngine.xorBytes(dstSessionKey, DukptEngine.PEK_VARIANT)
                hsmLogsListener.log("[G0-K1] Step 12: Dest PEK (w/ variant) = ${IsoUtil.bytesToHexString(dstPek)}")

                val newPinBlock = commandProcessor.formatPinBlock(pin, account, resolveFormat(dstFormat))
                hsmLogsListener.log("[G0-K1] Step 13: New clear PIN block = ${IsoUtil.bytesToHexString(newPinBlock)}")
                val encNewPinBlock = commandProcessor.encryptPinBlock(newPinBlock, dstPek)
                val encPinBlockStr = IsoUtil.bytesToHexString(encNewPinBlock)
                val pinLenPad = pin.length.toString().padStart(2, '0')
                hsmLogsListener.log("[G0-K1] Step 14: Encrypted PIN block = $encPinBlockStr  pinLen=$pinLenPad")

                // ── Format response ──────────────────────────────────────
                hsmLogsListener.log("◄ G0  Translation successful (K1 S-block format)")
                hsmLogsListener.onFormattedResponse(buildString {
                    appendLine(sep)
                    appendLine("◄ G1  Response — Translate PIN BDK→BDK (S-block K1)")
                    appendLine(sep)
                    appendLine("  Response Code          : G1")
                    appendLine("  Error Code             : 00  ✓ No error")
                    appendLine("  PIN Length             : $pinLenPad")
                    appendLine("  Encrypted PIN          : $encPinBlockStr")
                    appendLine("  Destination Format Code: $dstFormat  [${resolveFormat(dstFormat).description}]")
                    appendLine(sep)
                })

                HsmCommandResult.Success(
                    response = "${pinLenPad}${encPinBlockStr}${dstFormat}",
                    data = mapOf(
                        "encryptedPinBlock" to encPinBlockStr,
                        "pinLength"         to pin.length.toString(),
                        "srcKsn"            to srcKsnHex,
                        "destKsn"           to dstKsnHex,
                        "destKeyType"       to "BDK"
                    )
                )

            } else {
                // ============================================================
                // VARIANT-SCHEME FORMAT (U/T/X/Y or bare hex keys)
                //
                // Two sub-layouts supported:
                //  (a) K1-style (no destKeyType):
                //      SrcBDK(scheme+hex) + DstBDK(scheme+hex) +
                //      SrcKSNDesc(3H) + SrcKSN(20H) + DstKSNDesc(3H) + DstKSN(20H) +
                //      PINBlock(16H) + SrcFmt(2N) + DstFmt(2N) + Account(12N) [+ %LMK]
                //
                //  (b) Legacy (with destKeyType):
                //      SrcBDK(scheme+hex) + DestKeyType(1A) + DstKey(scheme+hex) +
                //      SrcKSNDesc(3H) + SrcKSN(20H) + [DstKSNDesc + DstKSN] +
                //      PINBlock(16H) + SrcFmt(2N) + DstFmt(2N) + Account(12N)
                //
                // Auto-detect: after reading srcBDK, check if the next char is
                // a destKeyType indicator ('0','*','~','!') or a key scheme char.
                // ============================================================
                hsmLogsListener.log("► G0  Detected variant-scheme format")

                // --- Source BDK (scheme+hex or bare hex) ---
                val (srcKeyWithScheme, srcKeyLen) = extractSchemeKey(data, pos); pos += srcKeyLen
                hsmLogsListener.log("          Source BDK                                : $srcKeyWithScheme")

                // --- Auto-detect destKeyType ---
                val nextChar = if (pos < data.length) data[pos] else '?'
                val hasDestKeyType = nextChar in setOf('0', '*', '~', '!')

                val destKeyType: Char
                val destIsBdk: Boolean
                if (hasDestKeyType) {
                    destKeyType = data[pos]; pos++
                    destIsBdk = destKeyType != '0'
                    hsmLogsListener.log("          Dest Key Type (legacy)                     : ${destKeyTypeLabel(destKeyType)}")
                } else {
                    destKeyType = '*'
                    destIsBdk = true
                    hsmLogsListener.log("          Dest Key Type                              : (none — K1-style BDK-to-BDK)")
                }

                // --- Destination key (scheme+hex or bare hex) ---
                val (dstKeyWithScheme, dstKeyLen) = extractSchemeKey(data, pos); pos += dstKeyLen
                val destKeyLabel = if (destIsBdk) "Dest BDK" else "ZPK"
                val destLmkLabel = if (destIsBdk) "LMK 28-29" else "LMK 06-07"
                hsmLogsListener.log("          $destKeyLabel                    ($destLmkLabel) : $dstKeyWithScheme")

                // --- Source KSN Descriptor (3H) + KSN (20H) ---
                val srcKsnDescriptor = data.substring(pos, pos + 3); pos += 3
                val srcCounterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(srcKsnDescriptor)
                hsmLogsListener.log("          Source KSN Descriptor                     : $srcKsnDescriptor  (counter bits: $srcCounterBits)")
                val srcKsnHex = data.substring(pos, pos + 20); pos += 20
                hsmLogsListener.log("          Source KSN                                : $srcKsnHex")

                // --- Destination KSN Descriptor + KSN (when dest is BDK) ---
                var destKsnDescriptor: String? = null
                var destKsnHex: String? = null
                if (destIsBdk) {
                    destKsnDescriptor = data.substring(pos, pos + 3); pos += 3
                    val destCounterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(destKsnDescriptor)
                    hsmLogsListener.log("          Dest KSN Descriptor                       : $destKsnDescriptor  (counter bits: $destCounterBits)")
                    destKsnHex = data.substring(pos, pos + 20); pos += 20
                    hsmLogsListener.log("          Dest KSN                                  : $destKsnHex")
                }

                // --- PIN block (16H) ---
                val pinBlockHex = data.substring(pos, pos + 16); pos += 16
                val pinBlock = IsoUtil.hexToBytes(pinBlockHex)
                hsmLogsListener.log("          Source PIN Block (enc/DUKPT)               : $pinBlockHex")

                // --- Source/Dest format codes (2N each) ---
                val srcFormat = data.substring(pos, pos + 2); pos += 2
                val dstFormat = data.substring(pos, pos + 2); pos += 2
                hsmLogsListener.log("          Source Format Code                         : $srcFormat  [${resolveFormat(srcFormat).description}]")
                hsmLogsListener.log("          Dest Format Code                           : $dstFormat  [${resolveFormat(dstFormat).description}]")

                // --- Account number (12N) ---
                val account = data.substring(pos, minOf(pos + 12, data.length))
                hsmLogsListener.log("          Account Number (12N)                       : $account")

                // ── Emit structured formatted request ────────────────────
                hsmLogsListener.onFormattedRequest(buildString {
                    appendLine("Message Header........... = [${cmd.header}]")
                    appendLine("Command Code............. = [G0] Translate PIN from BDK to ${if (destIsBdk) "BDK" else "ZPK"} (3DES DUKPT)")
                    appendLine("Source BDK............... = [$srcKeyWithScheme]")
                    if (hasDestKeyType) appendLine("Destination Key Type..... = [$destKeyType] ${destKeyTypeLabel(destKeyType)}")
                    appendLine("$destKeyLabel................ = [$dstKeyWithScheme]")
                    appendLine("Source KSN Descriptor.... = [$srcKsnDescriptor]")
                    appendLine("Source KSN............... = [$srcKsnHex]")
                    if (destIsBdk) {
                        appendLine("Dest KSN Descriptor...... = [$destKsnDescriptor]")
                        appendLine("Dest KSN................. = [$destKsnHex]")
                    }
                    appendLine("Source Encrypted Block... = [$pinBlockHex]")
                    appendLine("Source PIN block Format.. = [$srcFormat] ${resolveFormat(srcFormat).description}")
                    appendLine("Destination Format Code.. = [$dstFormat] ${resolveFormat(dstFormat).description}")
                    appendLine("Account Number........... = [$account]")
                    appendLine("LMK ID................... = [${cmd.lmkId}]")
                })

                // ── Decrypt keys using scheme-aware helpers ──────────────
                val srcLmkPair = PayShield10KCommandProcessor.LMK_PAIR_BDK
                val clearSrcBdk = decryptSchemeKeyUnderLmk(srcKeyWithScheme, cmd.lmkId, srcLmkPair)
                hsmLogsListener.log("[G0] Source BDK (clear) = ${IsoUtil.bytesToHexString(clearSrcBdk)}")

                val dstLmkPair = if (destIsBdk) PayShield10KCommandProcessor.LMK_PAIR_BDK
                                 else PayShield10KCommandProcessor.LMK_PAIR_ZMK_ZPK
                val clearDstKey = decryptSchemeKeyUnderLmk(dstKeyWithScheme, cmd.lmkId, dstLmkPair)
                hsmLogsListener.log("[G0] Dest key (clear) = ${IsoUtil.bytesToHexString(clearDstKey)}")

                // ── Source DUKPT derivation ───────────────────────────────
                val srcIpek = PayShield10KCommandProcessor.deriveInitialKey(clearSrcBdk, srcKsnHex, srcCounterBits)
                val srcKsnBytes = IsoUtil.hexToBytes(srcKsnHex)
                val srcCounter = PayShield10KCommandProcessor.extractDukptCounter(srcKsnBytes, srcCounterBits)
                val srcSessionKey = commandProcessor.deriveDukptSessionKey(srcIpek, srcKsnHex, srcCounter, srcCounterBits)
                val srcPek = DukptEngine.xorBytes(srcSessionKey, DukptEngine.PEK_VARIANT)
                hsmLogsListener.log("[G0] Source PEK = ${IsoUtil.bytesToHexString(srcPek)}")

                val clearPinBlock = commandProcessor.decryptPinBlock(pinBlock, srcPek)
                hsmLogsListener.log("[G0] Decrypted PIN block = ${IsoUtil.bytesToHexString(clearPinBlock)}")
                val pin = commandProcessor.extractPinFromBlock(clearPinBlock, account, resolveFormat(srcFormat))
                hsmLogsListener.log("[G0] Extracted PIN length=${pin.length}  pin=$pin")

                // ── Destination encryption ────────────────────────────────
                val encryptionKey: ByteArray = if (destIsBdk && destKsnDescriptor != null && destKsnHex != null) {
                    val dstCounterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(destKsnDescriptor)
                    val dstIpek = PayShield10KCommandProcessor.deriveInitialKey(clearDstKey, destKsnHex, dstCounterBits)
                    val dstKsnBytes = IsoUtil.hexToBytes(destKsnHex)
                    val dstCounter = PayShield10KCommandProcessor.extractDukptCounter(dstKsnBytes, dstCounterBits)
                    val dstSessionKey = commandProcessor.deriveDukptSessionKey(dstIpek, destKsnHex, dstCounter, dstCounterBits)
                    DukptEngine.xorBytes(dstSessionKey, DukptEngine.PEK_VARIANT).also {
                        hsmLogsListener.log("[G0] Dest PEK = ${IsoUtil.bytesToHexString(it)}")
                    }
                } else {
                    clearDstKey
                }

                val newPinBlock = commandProcessor.formatPinBlock(pin, account, resolveFormat(dstFormat))
                val encNewPinBlock = commandProcessor.encryptPinBlock(newPinBlock, encryptionKey)
                val encPinBlockStr = IsoUtil.bytesToHexString(encNewPinBlock)
                val pinLenPad = pin.length.toString().padStart(2, '0')
                hsmLogsListener.log("[G0] Encrypted PIN block = $encPinBlockStr  pinLen=$pinLenPad")

                val destModeLabel = if (destIsBdk) "dest DUKPT session key" else "ZPK"
                hsmLogsListener.log("◄ G0  Translation successful")

                hsmLogsListener.onFormattedResponse(buildString {
                    appendLine(sep)
                    appendLine("◄ G1  Response — Translate PIN from BDK to ${if (destIsBdk) "BDK" else "ZPK"} (3DES DUKPT)")
                    appendLine(sep)
                    appendLine("  Response Code          : G1")
                    appendLine("  Error Code             : 00  ✓ No error")
                    appendLine("  PIN Length             : $pinLenPad")
                    appendLine("  Encrypted PIN          : $encPinBlockStr")
                    appendLine("  Destination Format Code: $dstFormat  [${resolveFormat(dstFormat).description}]")
                    appendLine(sep)
                })

                HsmCommandResult.Success(
                    response = "${pinLenPad}${encPinBlockStr}${dstFormat}",
                    data = mapOf(
                        "encryptedPinBlock" to encPinBlockStr,
                        "pinLength"         to pin.length.toString(),
                        "srcKsn"            to srcKsnHex,
                        "destKeyType"       to destKeyType.toString()
                    ) + if (destKsnHex != null) mapOf("destKsn" to destKsnHex) else emptyMap()
                )
            }

        } catch (e: Exception) {
            hsmLogsListener.log("✗ G0  Exception: ${e.message}")
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
    // M4 — Translate Data Block (decrypt under source key, re-encrypt under destination key)
    // Wire format:
    //   M4[SrcMode_2N][DstMode_2N][InFmt_1N][OutFmt_1N]
    //     [SrcKeyType_3H][SrcKey_scheme+variable][SrcKSNDesc_3H?][SrcKSN_20H?]
    //     [DstKeyType_3H][DstKey_scheme+variable][DstKSNDesc_3H?][DstKSN_20H?]
    //     [SrcIV_16H?][DstIV_16H?][MsgLen_4H][MsgBlock_nH]
    // Response: M5 00 [MsgLen_4H][MsgBlock_nH]
    // MsgLen is in hex-character count (not bytes).
    // ====================================================================================================
    private suspend fun executeM4(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // Source Mode Flag (2N): 00=ECB, 01=CBC
            val srcMode = data.substring(pos, pos + 2); pos += 2
            // Destination Mode Flag (2N): 00=ECB, 01=CBC
            val dstMode = data.substring(pos, pos + 2); pos += 2
            // Input Format Flag (1N): 0=Binary, 1=Hex-Encoded
            val inFmt = data[pos].toString(); pos++
            // Output Format Flag (1N): 0=Binary, 1=Hex-Encoded
            val outFmt = data[pos].toString(); pos++

            // --- Source Key (S-block or variant+hex) ---
            val srcKeyTypeCode = data.substring(pos, pos + 3); pos += 3
            val (srcKeyWithScheme, srcKeyLen) = extractSchemeKey(data, pos); pos += srcKeyLen
            val srcIsKeyBlock = srcKeyWithScheme.firstOrNull()?.uppercaseChar() == 'S'
            val srcKeyUsageFromBlock = if (srcIsKeyBlock && srcKeyWithScheme.length >= 8)
                srcKeyWithScheme.substring(6, 8) else ""
            val srcKeyAlgoFromBlock = if (srcIsKeyBlock && srcKeyWithScheme.length >= 9)
                srcKeyWithScheme[8].uppercaseChar() else 'T'
            val srcIsBdk = if (srcIsKeyBlock)
                srcKeyUsageFromBlock in setOf("B0", "B1", "E0", "E1", "E2", "E4")
            else
                srcKeyTypeCode in BDK_KEY_TYPES

            var srcKsnDescriptor = ""
            var srcKsnHex = ""
            if (srcIsBdk) {
                srcKsnDescriptor = data.substring(pos, pos + 3); pos += 3
                srcKsnHex = data.substring(pos, pos + 20); pos += 20
            }

            // --- Destination Key (S-block or variant+hex) ---
            val dstKeyTypeCode = data.substring(pos, pos + 3); pos += 3
            val (dstKeyWithScheme, dstKeyLen) = extractSchemeKey(data, pos); pos += dstKeyLen
            val dstIsKeyBlock = dstKeyWithScheme.firstOrNull()?.uppercaseChar() == 'S'
            val dstKeyUsageFromBlock = if (dstIsKeyBlock && dstKeyWithScheme.length >= 8)
                dstKeyWithScheme.substring(6, 8) else ""
            val dstKeyAlgoFromBlock = if (dstIsKeyBlock && dstKeyWithScheme.length >= 9)
                dstKeyWithScheme[8].uppercaseChar() else 'T'
            val dstIsBdk = if (dstIsKeyBlock)
                dstKeyUsageFromBlock in setOf("B0", "B1", "E0", "E1", "E2", "E4")
            else
                dstKeyTypeCode in BDK_KEY_TYPES

            var dstKsnDescriptor = ""
            var dstKsnHex = ""
            if (dstIsBdk) {
                dstKsnDescriptor = data.substring(pos, pos + 3); pos += 3
                dstKsnHex = data.substring(pos, pos + 20); pos += 20
            }

            // Source IV (16H) — present when source mode is CBC
            val srcIvHex = if (srcMode == "01" && pos + 16 <= data.length) {
                data.substring(pos, pos + 16).also { pos += 16 }
            } else "0000000000000000"

            // Destination IV (16H) — present when dest mode is CBC
            val dstIvHex = if (dstMode == "01" && pos + 16 <= data.length) {
                data.substring(pos, pos + 16).also { pos += 16 }
            } else "0000000000000000"

            // Message Length (4H, hex-char count) + Message Block
            val dataLenHex = data.substring(pos, pos + 4); pos += 4
            val dataLen = dataLenHex.toInt(16)
            val msgBlockHex = data.substring(pos, minOf(pos + dataLen, data.length))
            val msgBlock = IsoUtil.hexToBytes(msgBlockHex)

            // ── Log formatted request ──
            val requestLog = buildString {
                appendLine("M4 - Translate Data Block Request")
                appendLine("Source Mode Flag......... = [$srcMode] ${if (srcMode == "00") "ECB" else "CBC"}")
                appendLine("Destination Mode Flag.... = [$dstMode] ${if (dstMode == "00") "ECB" else "CBC"}")
                appendLine("Input Format Flag........ = [$inFmt] ${if (inFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Output Format Flag....... = [$outFmt] ${if (outFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Source Key Type.......... = [$srcKeyTypeCode]")
                appendLine("Source Key............... = [$srcKeyWithScheme]")
                if (srcIsKeyBlock) {
                    appendLine("  Key Block Usage....... = [$srcKeyUsageFromBlock]")
                    appendLine("  Key Block Algo........ = [$srcKeyAlgoFromBlock]")
                }
                if (srcIsBdk) {
                    appendLine("Source KSN Descriptor.... = [$srcKsnDescriptor]")
                    appendLine("Source KSN............... = [$srcKsnHex]")
                }
                appendLine("Dest Key Type............ = [$dstKeyTypeCode]")
                appendLine("Dest Key................. = [$dstKeyWithScheme]")
                if (dstIsKeyBlock) {
                    appendLine("  Key Block Usage....... = [$dstKeyUsageFromBlock]")
                    appendLine("  Key Block Algo........ = [$dstKeyAlgoFromBlock]")
                }
                if (dstIsBdk) {
                    appendLine("Dest KSN Descriptor...... = [$dstKsnDescriptor]")
                    appendLine("Dest KSN................. = [$dstKsnHex]")
                }
                if (srcMode == "01") appendLine("Source IV................ = [$srcIvHex]")
                if (dstMode == "01") appendLine("Destination IV........... = [$dstIvHex]")
                appendLine("Message Length........... = [$dataLenHex]")
                appendLine("Encrypted Data........... = [$msgBlockHex]")
            }
            hsmLogsListener.log(requestLog)
            hsmLogsListener.onFormattedRequest(requestLog)

            // ── Step 1: Resolve source LMK pair, decrypt source key ──
            val srcLmkPairNumber = when {
                srcIsKeyBlock -> A0GenerateKeyCommand.lmkPairFromKeyUsage(srcKeyUsageFromBlock)
                srcIsBdk      -> A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[srcKeyTypeCode]?.lmkPairNumber
                                 ?: PayShield10KCommandProcessor.LMK_PAIR_BDK
                else          -> A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[srcKeyTypeCode]?.lmkPairNumber
                                 ?: PayShield10KCommandProcessor.LMK_PAIR_TPK
            }
            hsmLogsListener.log("[M4] Step 1: Source lmkPair=$srcLmkPairNumber isKeyBlock=$srcIsKeyBlock isBdk=$srcIsBdk")
            val clearSrcBdkOrKey = decryptSchemeKeyUnderLmk(srcKeyWithScheme, cmd.lmkId, srcLmkPairNumber)
            hsmLogsListener.log("[M4] Step 2: Source clear key = ${IsoUtil.bytesToHexString(clearSrcBdkOrKey)}")

            val srcClearKey: ByteArray = if (srcIsBdk) {
                val counterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(srcKsnDescriptor)
                val ipek = PayShield10KCommandProcessor.deriveInitialKey(clearSrcBdkOrKey, srcKsnHex, counterBits)
                hsmLogsListener.log("[M4] Step 3: Source IPEK = ${IsoUtil.bytesToHexString(ipek)}")
                val ksnBytes = IsoUtil.hexToBytes(srcKsnHex)
                val counter  = PayShield10KCommandProcessor.extractDukptCounter(ksnBytes, counterBits)
                val sessionKey = commandProcessor.deriveDukptSessionKey(ipek, srcKsnHex, counter, counterBits)
                hsmLogsListener.log("[M4] Step 4: Source DUKPT session key = ${IsoUtil.bytesToHexString(sessionKey)}")
                val dataVariantKey = DukptEngine.xorBytes(sessionKey, DukptEngine.DATA_VARIANT)
                DukptEngine.applyVariantEncryption(dataVariantKey).also {
                    hsmLogsListener.log("[M4] Step 5: Source data encryption key = ${IsoUtil.bytesToHexString(it)}")
                }
            } else {
                clearSrcBdkOrKey
            }

            // ── Step 2: Resolve destination LMK pair, decrypt destination key ──
            val dstLmkPairNumber = when {
                dstIsKeyBlock -> A0GenerateKeyCommand.lmkPairFromKeyUsage(dstKeyUsageFromBlock)
                dstIsBdk      -> A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[dstKeyTypeCode]?.lmkPairNumber
                                 ?: PayShield10KCommandProcessor.LMK_PAIR_BDK
                else          -> A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[dstKeyTypeCode]?.lmkPairNumber
                                 ?: PayShield10KCommandProcessor.LMK_PAIR_TPK
            }
            hsmLogsListener.log("[M4] Step 6: Dest lmkPair=$dstLmkPairNumber isKeyBlock=$dstIsKeyBlock isBdk=$dstIsBdk")
            val clearDstBdkOrKey = decryptSchemeKeyUnderLmk(dstKeyWithScheme, cmd.lmkId, dstLmkPairNumber)
            hsmLogsListener.log("[M4] Step 7: Dest clear key = ${IsoUtil.bytesToHexString(clearDstBdkOrKey)}")

            val dstClearKey: ByteArray = if (dstIsBdk) {
                val counterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(dstKsnDescriptor)
                val ipek = PayShield10KCommandProcessor.deriveInitialKey(clearDstBdkOrKey, dstKsnHex, counterBits)
                hsmLogsListener.log("[M4] Step 8: Dest IPEK = ${IsoUtil.bytesToHexString(ipek)}")
                val ksnBytes = IsoUtil.hexToBytes(dstKsnHex)
                val counter  = PayShield10KCommandProcessor.extractDukptCounter(ksnBytes, counterBits)
                val sessionKey = commandProcessor.deriveDukptSessionKey(ipek, dstKsnHex, counter, counterBits)
                hsmLogsListener.log("[M4] Step 9: Dest DUKPT session key = ${IsoUtil.bytesToHexString(sessionKey)}")
                val dataVariantKey = DukptEngine.xorBytes(sessionKey, DukptEngine.DATA_VARIANT)
                DukptEngine.applyVariantEncryption(dataVariantKey).also {
                    hsmLogsListener.log("[M4] Step 10: Dest data encryption key = ${IsoUtil.bytesToHexString(it)}")
                }
            } else {
                clearDstBdkOrKey
            }

            // ── Step 3: Decrypt under source key ──
            val srcCipherMode = if (srcMode == "01") CipherMode.CBC else CipherMode.ECB
            val srcIvBytes    = IsoUtil.hexToBytes(srcIvHex)
            val srcAlgo = if (srcKeyAlgoFromBlock == 'A') CryptoAlgorithm.AES else CryptoAlgorithm.TDES
            hsmLogsListener.log("[M4] Step 11: Decrypting - mode=${if (srcMode == "01") "CBC" else "ECB"} algo=$srcAlgo key=${IsoUtil.bytesToHexString(srcClearKey)}")

            val eng = engine()
            val decData = try {
                eng.encryptionEngine.decrypt(
                    algorithm = srcAlgo,
                    decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                        data = msgBlock,
                        key = srcClearKey,
                        iv = if (srcMode == "01") srcIvBytes else null,
                        mode = srcCipherMode,
                        padding = PaddingMethods.PKCS5
                    )
                )
            } catch (_: Exception) {
                hsmLogsListener.log("[M4] PKCS5 padding invalid on source decrypt, falling back to NoPadding")
                eng.encryptionEngine.decrypt(
                    algorithm = srcAlgo,
                    decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                        data = msgBlock,
                        key = srcClearKey,
                        iv = if (srcMode == "01") srcIvBytes else null,
                        mode = srcCipherMode,
                        padding = PaddingMethods.NONE
                    )
                )
            }
            hsmLogsListener.log("[M4] Step 12: Decrypted plaintext = ${IsoUtil.bytesToHexString(decData)}")

            // ── Step 4: Re-encrypt under destination key ──
            val dstCipherMode = if (dstMode == "01") CipherMode.CBC else CipherMode.ECB
            val dstIvBytes    = IsoUtil.hexToBytes(dstIvHex)
            val dstAlgo = if (dstKeyAlgoFromBlock == 'A') CryptoAlgorithm.AES else CryptoAlgorithm.TDES
            hsmLogsListener.log("[M4] Step 13: Re-encrypting - mode=${if (dstMode == "01") "CBC" else "ECB"} algo=$dstAlgo key=${IsoUtil.bytesToHexString(dstClearKey)}")

            val encData = eng.encryptionEngine.encrypt(
                algorithm = dstAlgo,
                encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                    data = decData,
                    key = dstClearKey,
                    iv = if (dstMode == "01") dstIvBytes else null,
                    mode = dstCipherMode,
                    padding = PaddingMethods.PKCS5
                )
            )
            val encHex = IsoUtil.bytesToHexString(encData)
            // Output length is hex-char count (matches input convention)
            val outLen = encHex.length.toString(16).padStart(4, '0').uppercase()

            hsmLogsListener.log("[M4] Step 14: Encrypted result = $encHex (${encData.size} bytes)")

            // ── Log formatted response ──
            val responseLog = buildString {
                appendLine("M5 - Translate Data Block Response")
                appendLine("Error Code............... = [00]")
                appendLine("Output IV................ = []")
                appendLine("Translated Message Length = [$outLen]")
                appendLine("Translated Data.......... = [$encHex]")
            }
            hsmLogsListener.log(responseLog)
            hsmLogsListener.onFormattedResponse(responseLog)

            HsmCommandResult.Success(
                response = "$outLen$encHex",
                data = mapOf("translatedData" to encHex, "srcIv" to srcIvHex, "dstIv" to dstIvHex)
            )
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
    // ====================================================================================================
    // M6 — Generate MAC (Extended)
    //
    // Command format:
    //   M6 [ModeFlag 1N] [InputFormatFlag 1N] [MacSize 1N] [MacAlgorithm 1N] [PaddingMethod 1N]
    //      [KeyType 3H] [MacKey scheme+variable] [IV 16H, only if mode!=0] [MsgLen 4H] [Msg]
    //      [%LmkId 2N, optional]
    //
    // ModeFlag:      0 = only block, 1 = first, 2 = middle, 3 = last
    // InputFormat:   0 = binary, 1 = hex-encoded binary
    // MacSize:       0 = 4 bytes (8H), 1 = 8 bytes (16H full MAC)
    // MacAlgorithm:  0 = ISO 9797-1 Alg 1, 1 = ISO 9797-1 Alg 3 (ANSI X9.19)
    // PaddingMethod: 1 = ISO 9797 Method 1 (zeros), 2 = Method 2 (0x80+zeros)
    // KeyType:       FFF = KeyBlock, 003 = TAK, 009 = BDK, etc.
    //
    // Response: M7 00 [MAC 8H or 16H] [OCD 16H, only if mode=1 or mode=2]
    // ====================================================================================================
    private suspend fun executeM6(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val modeFlag        = data[pos]; pos++
            val inputFormatFlag = data[pos]; pos++
            val macSizeFlag     = data[pos]; pos++
            val macAlgorithm    = data[pos]; pos++
            val paddingMethod   = data[pos].digitToInt(); pos++
            val keyTypeCode     = data.substring(pos, pos + 3); pos += 3

            // ── MAC key parsing (S-block or variant+hex) ──────────────────────────────────
            val (keyWithScheme, keyLen) = extractSchemeKey(data, pos); pos += keyLen
            val isKeyBlock = keyWithScheme.firstOrNull()?.uppercaseChar() == 'S'

            val keyUsageFromBlock = if (isKeyBlock && keyWithScheme.length >= 8)
                keyWithScheme.substring(6, 8) else ""

            // IV / OCD — only present when mode is not 0 (only-block)
            val ivHex = if (modeFlag != '0' && pos + 16 <= data.length)
                data.substring(pos, pos + 16).also { pos += 16 }
            else null

            // Message Length (4H) + Message Block
            val msgLenHex  = data.substring(pos, pos + 4); pos += 4
            val msgLen     = msgLenHex.toInt(16)
            val messageHex = data.substring(pos, minOf(pos + msgLen * 2, data.length))
            val message    = IsoUtil.hexToBytes(messageHex)

            // ── Resolve LMK pair ────────────────────────────────────────────────────────────
            val lmkPairNumber = when {
                isKeyBlock -> A0GenerateKeyCommand.lmkPairFromKeyUsage(keyUsageFromBlock)
                else       -> A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[keyTypeCode]?.lmkPairNumber
                               ?: PayShield10KCommandProcessor.LMK_PAIR_TAK
            }

            // ── Decrypt MAC key under LMK ───────────────────────────────────────────────────
            val clearMacKey = decryptSchemeKeyUnderLmk(keyWithScheme, cmd.lmkId, lmkPairNumber)

            // ── MAC variant for DUKPT BDK path ──────────────────────────────────────────────
            val isBdkType = if (isKeyBlock)
                keyUsageFromBlock in setOf("B0", "B1", "E0", "E1", "E2", "E4")
            else
                keyTypeCode in BDK_KEY_TYPES
            val effectiveMacKey = if (isBdkType)
                DukptEngine.xorBytes(clearMacKey, DukptEngine.MAC_VARIANT)
            else
                clearMacKey

            // ── Algorithm selection ─────────────────────────────────────────────────────────
            // PayShield M6/M8 wire encoding: '1' = ISO 9797-1 Alg 1, '3' = ISO 9797-1 Alg 3
            val algorithm = when (macAlgorithm) {
                '1' -> "ISO9797_ALG1"
                '3' -> "ISO9797_ALG3"
                else -> return HsmCommandResult.Error("04", "Unsupported MAC algorithm: $macAlgorithm")
            }
            val macSizeBytes = if (macSizeFlag == '1') 8 else 4

            val modeDesc  = when (modeFlag) {
                '0' -> "Only block (single-block message)"
                '1' -> "First block"
                '2' -> "Middle block"
                '3' -> "Last block"
                else -> "Unknown"
            }
            val algDesc   = when (macAlgorithm) {
                '1' -> "ISO 9797-1 Algorithm 1"
                '3' -> "ISO 9797-1 Algorithm 3 (ANSI X9.19)"
                else -> "Unknown"
            }
            val padDesc   = when (paddingMethod) {
                1 -> "ISO 9797 Method 1 (pad with 0x00)"
                2 -> "ISO 9797 Method 2 (pad with 0x80)"
                else -> "Method $paddingMethod"
            }

            hsmLogsListener.log(buildString {
                appendLine("M6 - Generate MAC (Extended) Request")
                appendLine("Mode Flag................ = [$modeFlag] $modeDesc")
                appendLine("Input Format Flag........ = [$inputFormatFlag] ${if (inputFormatFlag == '1') "Hex-Encoded Binary" else "Binary"}")
                appendLine("MAC Size................. = [$macSizeFlag] ${macSizeBytes} bytes (${macSizeBytes * 2}H)")
                appendLine("MAC Algorithm............ = [$macAlgorithm] $algDesc")
                appendLine("Padding Method........... = [$paddingMethod] $padDesc")
                appendLine("Key Type................. = [$keyTypeCode]")
                appendLine("MAC Key.................. = [$keyWithScheme]")
                if (isKeyBlock) {
                    appendLine("  Key Block Usage....... = [$keyUsageFromBlock]")
                    appendLine("  Is BDK/DUKPT.......... = $isBdkType")
                }
                if (ivHex != null) appendLine("IV....................... = [$ivHex]")
                appendLine("Message Length........... = [$msgLenHex] ($msgLen bytes)")
                appendLine("Message Block............ = [$messageHex]")
            })
            hsmLogsListener.onFormattedRequest(buildString {
                appendLine("M6 - Generate MAC (Extended) Request")
                appendLine("Mode Flag................ = [$modeFlag] $modeDesc")
                appendLine("Input Format Flag........ = [$inputFormatFlag] ${if (inputFormatFlag == '1') "Hex-Encoded Binary" else "Binary"}")
                appendLine("MAC Size................. = [$macSizeFlag] ${macSizeBytes} bytes (${macSizeBytes * 2}H)")
                appendLine("MAC Algorithm............ = [$macAlgorithm] $algDesc")
                appendLine("Padding Method........... = [$paddingMethod] $padDesc")
                appendLine("Key Type................. = [$keyTypeCode]")
                appendLine("MAC Key.................. = [$keyWithScheme]")
                if (isKeyBlock) {
                    appendLine("  Key Block Usage....... = [$keyUsageFromBlock]")
                }
                if (ivHex != null) appendLine("IV / OCD................. = [$ivHex]")
                appendLine("Message Length........... = [$msgLenHex] ($msgLen bytes)")
                appendLine("Message Block............ = [$messageHex]")
            })

            // ── Generate MAC ─────────────────────────────────────────────────────────────────
            val result = commandProcessor.executeGenerateMac(
                data          = message,
                tak           = effectiveMacKey,
                algorithm     = algorithm,
                paddingMethod = paddingMethod,
                macSizeBytes  = macSizeBytes
            )

            if (result is HsmCommandResult.Success) {
                val macHex = result.data["mac"] as? String ?: result.response
                hsmLogsListener.log("[M6] Generated MAC = $macHex")
                hsmLogsListener.onFormattedResponse(buildString {
                    appendLine("M7 - Generate MAC Response")
                    appendLine("Error Code............... = [00]")
                    appendLine("MAC...................... = [$macHex]")
                })
                HsmCommandResult.Success(response = macHex, data = mapOf("mac" to macHex))
            } else {
                result
            }

        } catch (e: Exception) {
            HsmCommandResult.Error("15", "M6 failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // M8 — Verify MAC
    //
    // Command format:
    //   M8 [Mode Flag 1N] [Input Format Flag 1N] [MAC Size 1N] [MAC Algorithm 1N]
    //      [Padding Method 1N] [Key Type 3H] [Key Scheme 1A + Key 32H/48H]
    //      [Message Length 4H] [Message Block] [MAC 8H/16H]
    //
    // Mode Flag:    0 = only block, 1 = first, 2 = middle, 3 = last
    // Input Format: 0 = binary, 1 = hex-encoded binary
    // MAC Size:     0 = 4 bytes (8 hex digits), 1 = 8 bytes (16 hex digits)
    // MAC Algorithm: 1 = ISO 9797 Alg 1, 3 = ISO 9797 Alg 3 (ANSI X9.19)
    // Padding:      1 = ISO 9797 method 1 (0x00), 2 = method 2 (0x80 + 0x00)
    // Key Type:     003 = TAK (LMK 16-17)
    // Key Scheme:   U = double-length (32H), T = triple-length (48H)
    //
    // Response: M9 00
    // ====================================================================================================
    private suspend fun executeM8(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val modeFlag = data[pos]; pos++
            val inputFormatFlag = data[pos]; pos++
            val macSizeFlag = data[pos]; pos++
            val macAlgorithm = data[pos]; pos++
            val paddingMethod = data[pos].digitToInt(); pos++
            val keyTypeCode = data.substring(pos, pos + 3); pos += 3

            val keyScheme = data[pos]; pos++
            val keyHexLen = when (keyScheme) {
                'T' -> 48
                else -> 32   // 'U' = double-length, default
            }
            val keyHex = data.substring(pos, pos + keyHexLen); pos += keyHexLen
            val encryptedTak = IsoUtil.hexToBytes(keyHex)

            val msgLenHex = data.substring(pos, pos + 4); pos += 4
            val msgLen = msgLenHex.toInt(16)
            val messageHex = data.substring(pos, pos + msgLen * 2); pos += msgLen * 2
            val message = IsoUtil.hexToBytes(messageHex)

            val macHexLen = if (macSizeFlag == '1') 16 else 8
            val macHex = data.substring(pos, pos + macHexLen)
            val providedMac = IsoUtil.hexToBytes(macHex)

            val algorithm = when (macAlgorithm) {
                '1' -> "ISO9797_ALG1"
                '3' -> "ISO9797_ALG3"
                else -> return HsmCommandResult.Error("04", "Unsupported MAC algorithm: $macAlgorithm")
            }
            val macSizeBytes = if (macSizeFlag == '1') 8 else 4

            val modeDesc = when (modeFlag) {
                '0' -> "Only block of a single-block message"
                '1' -> "First block"
                '2' -> "Middle block"
                '3' -> "Last block"
                else -> "Unknown"
            }
            val algDesc = when (macAlgorithm) {
                '1' -> "ISO 9797 MAC algorithm 1"
                '3' -> "ISO 9797 MAC algorithm 3 (ANSI X9.19)"
                else -> "Unknown"
            }
            val padDesc = when (paddingMethod) {
                1 -> "ISO 9797 Padding method 1 (pad with 0x00)"
                2 -> "ISO 9797 Padding method 2 (pad with 0x80)"
                else -> "Unknown"
            }
            val keyTypeDesc = when (keyTypeCode) {
                "003" -> "TAK (encrypted under LMK pair 16-17)"
                else -> "Key type $keyTypeCode"
            }

            hsmLogsListener.onFormattedRequest(buildString {
                appendLine("M8 - Verify MAC Request")
                appendLine("Mode Flag................ = [$modeFlag] $modeDesc")
                appendLine("Input Format Flag........ = [$inputFormatFlag] ${if (inputFormatFlag == '1') "Hex-Encoded Binary" else "Binary"}")
                appendLine("MAC Size................. = [$macSizeFlag] MAC size of ${macHexLen} hex digits")
                appendLine("Mac Algorithm............ = [$macAlgorithm] $algDesc")
                appendLine("Padding Method........... = [$paddingMethod] $padDesc")
                appendLine("Key Type................. = [$keyTypeCode] $keyTypeDesc")
                appendLine("Key...................... = [$keyScheme$keyHex]")
                appendLine("Message Length........... = [$msgLenHex]")
                appendLine("Message Block............ = [$messageHex]")
                appendLine("MAC...................... = [$macHex]")
            })

            commandProcessor.executeVerifyMac(
                lmkId = cmd.lmkId,
                data = message,
                providedMac = providedMac,
                encryptedTak = encryptedTak,
                algorithm = algorithm,
                paddingMethod = paddingMethod,
                macSizeBytes = macSizeBytes
            )

        } catch (e: Exception) {
            HsmCommandResult.Error("15", "M8 failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // MY — Verify and Translate MAC
    // Command: MY[SRC_KEY_SCHEME+32H][DST_KEY_SCHEME+32H][MODE_1N][MSG_LEN_4H][MSG][SRC_MAC_8H]
    // Response: MZ 00 [DST_MAC_8H]
    // Verifies the incoming MAC under the source key, then generates a new MAC under the destination key.
    // ====================================================================================================
    private fun executeMY(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // Source key (scheme + 32H)
            pos++ // scheme
            val srcKeyHex = data.substring(pos, pos + 32); pos += 32
            val srcKey = IsoUtil.hexToBytes(srcKeyHex)

            // Destination key (scheme + 32H)
            pos++ // scheme
            val dstKeyHex = data.substring(pos, pos + 32); pos += 32
            val dstKey = IsoUtil.hexToBytes(dstKeyHex)

            // Mode (1N)
            val mode = data[pos]; pos++

            // Message length (4H) + message
            val msgLenHex = data.substring(pos, pos + 4); pos += 4
            val msgLen = msgLenHex.toInt(16)
            val messageHex = data.substring(pos, pos + msgLen * 2); pos += msgLen * 2
            val message = IsoUtil.hexToBytes(messageHex)

            // Source MAC (16H = 8 bytes)
            val srcMacHex = data.substring(pos, pos + 16)
            val srcMac = IsoUtil.hexToBytes(srcMacHex)

            hsmLogsListener.onFormattedRequest("""
                MY - Verify+Translate MAC
                Mode: $mode  MsgLen: $msgLen
            """.trimIndent())

            // Step 1: verify MAC under source key
            val verifyResult = commandProcessor.executeVerifyMac(
                data = message,
                providedMac = srcMac,
                tak = srcKey,
                algorithm = "ISO9797_ALG3"
            )
            if (verifyResult is HsmCommandResult.Error) return verifyResult

            // Step 2: generate new MAC under destination key
            val genResult = commandProcessor.executeGenerateMac(
                data = message,
                tak = dstKey,
                algorithm = "ISO9797_ALG3"
            )

            if (genResult is HsmCommandResult.Success) {
                val outMac = genResult.data["mac"] as? String ?: genResult.response
                HsmCommandResult.Success(
                    response = outMac,
                    data = mapOf("mac" to outMac)
                )
            } else genResult
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "MY failed: ${e.message}")
        }
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
    // PM — Verify Dynamic CVV (dCVV / CVC2)
    // Command: PM[Scheme_1N][Version_1N][MK-dCVV_1A+32H]A[PAN];[ExpDate_4N]000[ATC_5N][CVC_3N]
    // Response: PN 00
    // The scheme selects the algorithm (0 = 3DES CBC, 1 = AES).
    // Derivation: session key = derive_from_MK(PAN, PAN_SEQ) then MAC(PAN||ExpDate||ServiceCode, SK).
    // ====================================================================================================
    private suspend fun executePM(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val scheme  = data[pos].toString(); pos++
            val version = data[pos].toString(); pos++

            // MK-dCVV (scheme char + 32H)
            pos++ // key scheme
            val mkHex = data.substring(pos, pos + 32); pos += 32
            val mk = IsoUtil.hexToBytes(mkHex)

            // 'A' delimiter
            if (pos < data.length && data[pos] == 'A') pos++

            // PAN (up to ';')
            val panEnd = data.indexOf(';', pos)
            val pan = if (panEnd > 0) data.substring(pos, panEnd).also { pos = panEnd + 1 } else data.substring(pos).also { pos = data.length }

            // Expiry (4N) + service code placeholder (3N)
            val expDate = data.substring(pos, minOf(pos + 4, data.length)); pos += 4
            if (pos + 3 <= data.length) pos += 3  // skip "000" or service code

            // ATC (5N)
            val atc = data.substring(pos, minOf(pos + 5, data.length)); pos += 5

            // CVC to verify (3N)
            val cvcToVerify = data.substring(pos, minOf(pos + 3, data.length))

            // Decrypt MK-dCVV under LMK
            val clearMk = simulator.decryptUnderLmk(mk, cmd.lmkId, 14)

            // Derive session key: SK = 3DES-ECB( MK, PAN_left16 ) XOR 3DES-ECB( MK, PAN_left16 XOR 0xFF..FF )
            val panPadded = pan.padEnd(16, 'F').take(16)
            val panBytes  = IsoUtil.hexToBytes(panPadded)
            val skCipher  = Cipher.getInstance("DESede/ECB/NoPadding")
            skCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(clearMk.copyOf(16), "DESede"))
            val skLeft  = skCipher.doFinal(panBytes)
            val panXor  = panBytes.map { (it.toInt() xor 0xFF).toByte() }.toByteArray()
            val skRight = skCipher.doFinal(panXor)
            val sk      = skLeft + skRight.copyOf(8)

            // Generate dCVV: MAC over PAN || ExpDate || ATC
            val input    = (pan + expDate + atc).padEnd(16, '0').take(16)
            val macCipher = Cipher.getInstance("DESede/ECB/NoPadding")
            macCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(sk.copyOf(16), "DESede"))
            val macResult = macCipher.doFinal(IsoUtil.hexToBytes(input))
            val macHex    = IsoUtil.bytesToHexString(macResult)

            // Decimalize to 3 digits
            val calculatedCvc = decimalizePin(macHex, "0123456789012345", 3)

            if (calculatedCvc == cvcToVerify) {
                HsmCommandResult.Success(response = "", data = mapOf("verified" to "true"))
            } else {
                HsmCommandResult.Error("01", "dCVV verification failed (calc: $calculatedCvc, got: $cvcToVerify)")
            }
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "PM failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // DA — Verify PIN (IBM 3624 Natural PIN method)
    // Command: DA[TPK_1A+32H][PVK_16H][MaxPIN_2N][PINBlock_16H][Fmt_2N][Account_12N]
    //            [DecTable_16H][PINValData_12A][Offset_12H]
    // Response: DB 00
    // ====================================================================================================
    private suspend fun executeDA(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            pos++ // TPK scheme
            val tpkHex = data.substring(pos, pos + 32); pos += 32
            val tpk = IsoUtil.hexToBytes(tpkHex)

            val pvkHex = data.substring(pos, pos + 16); pos += 16
            val pvk = IsoUtil.hexToBytes(pvkHex)

            val maxPinLen = data.substring(pos, pos + 2).toIntOrNull() ?: 12; pos += 2

            val pinBlockHex = data.substring(pos, pos + 16); pos += 16
            val pinBlock = IsoUtil.hexToBytes(pinBlockHex)

            val fmt = data.substring(pos, pos + 2); pos += 2

            val account = data.substring(pos, pos + 12); pos += 12

            val decTable = if (pos + 16 <= data.length) data.substring(pos, pos + 16) else "0123456789012345"; pos += 16
            // PINValData (12A) – not used in standard IBM offset verify, skip
            if (pos + 12 <= data.length) pos += 12
            val offset = if (pos + 12 <= data.length) data.substring(pos, pos + 12) else "FFFFFFFFFFFF"

            // Decrypt TPK under LMK
            val clearTpk = simulator.decryptUnderLmk(tpk, cmd.lmkId, 14)

            // Decrypt PIN block under TPK
            val tpkCipher = Cipher.getInstance("DESede/ECB/NoPadding")
            tpkCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(clearTpk.copyOf(16), "DESede"))
            val clearPinBlock = tpkCipher.doFinal(pinBlock)

            // Extract PIN (ISO Format 0 XOR with PAN)
            val panPart = "0000" + pan12(account)
            val panBytes = IsoUtil.hexToBytes(panPart)
            val xored = clearPinBlock.zip(panBytes) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
            val xHex = IsoUtil.bytesToHexString(xored)
            val pinLen = xHex[1].digitToIntOrNull() ?: 4
            val customerPin = xHex.substring(2, 2 + pinLen)

            // Decrypt PVK under LMK
            val clearPvk = simulator.decryptUnderLmk(pvk, cmd.lmkId, 14)

            // Generate natural PIN
            val pan = pan12(account).padStart(16, '0')
            val pvkCipher = Cipher.getInstance("DESede/ECB/NoPadding")
            pvkCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(clearPvk.copyOf(16), "DESede"))
            val encPan = pvkCipher.doFinal(IsoUtil.hexToBytes(pan))
            val naturalPin = decimalizePin(IsoUtil.bytesToHexString(encPan), decTable, pinLen)

            // Apply offset to get expected PIN
            val expectedPin = buildString {
                for (i in naturalPin.indices) {
                    val o = if (i < offset.length && offset[i] != 'F') offset[i].digitToInt() else 0
                    append((naturalPin[i].digitToInt() + o) % 10)
                }
            }

            if (customerPin == expectedPin) {
                HsmCommandResult.Success(response = "", data = mapOf("verified" to "true"))
            } else {
                HsmCommandResult.Error("01", "IBM PIN verification failed")
            }
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "DA failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // DC — Verify PIN (VISA PVV method)
    // Command: DC[TPK_1A+32H][PVKPair_32H][PINBlock_16H][Fmt_2N][Account_12N][PVKI_1N][PVV_4N]
    // Response: DD 00
    // ====================================================================================================
    private suspend fun executeDC(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            pos++ // TPK scheme
            val tpkHex = data.substring(pos, pos + 32); pos += 32
            val tpk = IsoUtil.hexToBytes(tpkHex)

            val pvkPairHex = data.substring(pos, pos + 32); pos += 32
            val pvkPair = IsoUtil.hexToBytes(pvkPairHex)

            val pinBlockHex = data.substring(pos, pos + 16); pos += 16
            val pinBlock = IsoUtil.hexToBytes(pinBlockHex)

            val fmt = data.substring(pos, pos + 2); pos += 2
            val account = data.substring(pos, pos + 12); pos += 12
            val pvki = data.getOrNull(pos)?.digitToIntOrNull() ?: 1; pos++
            val pvv = data.substring(pos, minOf(pos + 4, data.length))

            // Decrypt TPK + PIN block
            val clearTpk = simulator.decryptUnderLmk(tpk, cmd.lmkId, 14)
            val tpkCipher = Cipher.getInstance("DESede/ECB/NoPadding")
            tpkCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(clearTpk.copyOf(16), "DESede"))
            val clearPinBlock = tpkCipher.doFinal(pinBlock)

            val panPart  = "0000" + pan12(account)
            val xored    = clearPinBlock.zip(IsoUtil.hexToBytes(panPart)) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
            val xHex     = IsoUtil.bytesToHexString(xored)
            val pinLen   = xHex[1].digitToIntOrNull() ?: 4
            val pin      = xHex.substring(2, 2 + pinLen)

            // Calculate PVV: TSP = pan11 + PVKI + PIN (16 hex = 8 bytes)
            val pan11    = account.takeLast(12).dropLast(1)
            val tsp      = (pan11 + pvki.toString() + pin).padEnd(16, '0').take(16)

            val clearPvkPair = simulator.decryptUnderLmk(pvkPair, cmd.lmkId, 14)
            val pvkCipher    = Cipher.getInstance("DESede/ECB/NoPadding")
            pvkCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(clearPvkPair.copyOf(16), "DESede"))
            val encTsp = pvkCipher.doFinal(IsoUtil.hexToBytes(tsp))
            val calcPvv = decimalizePin(IsoUtil.bytesToHexString(encTsp), "0123456789012345", 4)

            if (calcPvv == pvv) {
                HsmCommandResult.Success(response = "", data = mapOf("verified" to "true"))
            } else {
                HsmCommandResult.Error("01", "PVV verification failed (calc=$calcPvv, got=$pvv)")
            }
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "DC failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // A6 — Import Key (ZMK-encrypted → LMK)
    // Command: A6[KeyType_3H][ZMKScheme(1A)+ZMK(variable)][LMKScheme_1A][Key(variable)]
    // KeyBlock variant: ZMK and/or import key may be in 'S' scheme (full KeyBlock string).
    // When the LMK slot scheme is KEY_BLOCK, the response key is returned in KeyBlock format.
    // Response: A7 00 [Key_under_LMK][KCV_6H]
    // ====================================================================================================
    private suspend fun executeA6(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // 1. Key type (3H)
            val keyTypeCode = data.substring(pos, pos + 3); pos += 3

            // 2. ZMK with scheme prefix (variable length)
            val (zmkWithScheme, zmkLen) = extractSchemeKey(data, pos); pos += zmkLen

            // 3. LMK output scheme character (1 char)
            val lmkSchemeChar = if (pos < data.length) data[pos].uppercaseChar() else 'U'; pos++

            // 4. Key to import with scheme prefix (variable length)
            val (keyWithScheme, keyLen) = extractSchemeKey(data, pos); pos += keyLen

            val keyType = KeyType.values().find { it.code == keyTypeCode } ?: KeyType.TYPE_001

            // 5. Decrypt ZMK under LMK pair 00-01 (ZMK/ZPK pair)
            val clearZmk = decryptSchemeKeyUnderLmk(zmkWithScheme, cmd.lmkId, 0)

            // 6. Decrypt the import key under the clear ZMK
            val clearKey = decryptSchemeKeyUnderClearKey(keyWithScheme, clearZmk)

            // 7. Apply odd parity
            val parityKey = simulator.applyOddParity(clearKey)

            // 8. Encrypt under LMK — use KeyBlock format when LMK scheme is KEY_BLOCK or lmkScheme is 'S'
            val lmkSet = simulator.getSlotManager().getLmkFromSlot(cmd.lmkId)
            val useKeyBlock = lmkSet?.scheme == "KEY_BLOCK" || lmkSchemeChar == 'S'
            val kcv = simulator.calculateKeyCheckValue(parityKey)

            val keyUnderLmk = if (useKeyBlock) {
                simulator.buildKeyBlockForKeyType(parityKey, cmd.lmkId, keyType.getLmkPairNumber(), keyTypeCode)
            } else {
                val encBytes = simulator.encryptUnderLmk(parityKey, cmd.lmkId, keyType.getLmkPairNumber())
                "$lmkSchemeChar${IsoUtil.bytesToHexString(encBytes)}"
            }

            HsmCommandResult.Success(
                response = "$keyUnderLmk$kcv",
                data = mapOf("lmkKey" to keyUnderLmk, "kcv" to kcv)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "A6 failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // A8 — Export Key (LMK → ZMK/TMK-encrypted)
    //
    // Command format:
    //   A8 + KeyType(3H) + [;] + ZMK/TMK Flag(1N) + ZMK/TMK(scheme+variable)
    //      + Key_under_LMK(scheme+variable) + KeyScheme_ZMK(1A)
    //      + [Atalla variant(nA)] + [%LMK_ID(2H)]
    //
    // Where:
    //   KeyType        : 3H key type code (e.g. "001"=ZPK, "002"=TPK, "FFF"=KeyBlock)
    //   ';'            : Optional delimiter (present when Delimiter checkbox is set)
    //   ZMK/TMK Flag   : '0' = ZMK, '1' = TMK
    //   ZMK/TMK        : Zone/Terminal Master Key encrypted under LMK (scheme prefix + hex)
    //   Key_under_LMK  : The key to export, encrypted under LMK (scheme prefix + hex)
    //   KeyScheme_ZMK   : Export scheme under ZMK ('U', 'T', 'X', 'Y', 'S')
    //   Atalla variant  : Optional Atalla variant string
    //   '%'             : Optional second delimiter for LMK Identifier
    //   LMK_ID         : 2H LMK slot identifier (e.g. "03")
    //
    // Response: A9 00 [ExportedKey][KCV_6H]
    // ====================================================================================================
    private suspend fun executeA8(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // 1. Key type (3H)
            val keyTypeCode = data.substring(pos, pos + 3); pos += 3

            // 2. Skip ';' delimiter if present
            if (pos < data.length && data[pos] == ';') {
                pos += 1
            }

            // 3. ZMK/TMK flag: '0' = ZMK, '1' = TMK (consume but not used for logic)
            var zmkTmkFlag = '0'
            if (pos < data.length && (data[pos] == '0' || data[pos] == '1')) {
                zmkTmkFlag = data[pos]
                pos += 1
            }

            // 4. ZMK/TMK with scheme prefix (variable length)
            val (zmkWithScheme, zmkLen) = extractSchemeKey(data, pos); pos += zmkLen

            // 5. Key under LMK with scheme prefix (variable length)
            val (keyUnderLmkWithScheme, keyLen) = extractSchemeKey(data, pos); pos += keyLen

            // 6. Export key scheme (1 char, e.g. 'U', 'T', 'S')
            var exportScheme = 'U'
            if (pos < data.length && data[pos] != '%' && data[pos] != ';') {
                exportScheme = data[pos].uppercaseChar()
                pos += 1
            }

            // 7. Atalla variant (optional, skip characters until '%' or end)
            // The Atalla variant field is optional free-text; skip it.
            while (pos < data.length && data[pos] != '%') {
                pos++
            }

            // 8. '%' delimiter + LMK ID (2H) — handled by outer parser via cmd.lmkId

            // Resolve LMK pair for the key to export
            val keyLmkPair: Int
            val keyTypeLabel: String
            if (keyTypeCode == "FFF") {
                // KeyBlock mode: determine LMK pair from the S-block key usage header
                val keyUsage = if (keyUnderLmkWithScheme.length >= 8 &&
                    keyUnderLmkWithScheme[0].uppercaseChar() == 'S') {
                    keyUnderLmkWithScheme.substring(6, 8)
                } else "K0"
                keyLmkPair = A0GenerateKeyCommand.lmkPairFromKeyUsage(keyUsage)
                keyTypeLabel = "FFF/KeyBlock (usage=$keyUsage)"
            } else {
                val keyType = KeyType.values().find { it.code == keyTypeCode } ?: KeyType.TYPE_001
                keyLmkPair = keyType.getLmkPairNumber()
                keyTypeLabel = "$keyTypeCode (${keyType.name})"
            }

            // Resolve LMK pair for ZMK/TMK
            val zmkLmkPair: Int
            if (zmkWithScheme.isNotEmpty() && zmkWithScheme[0].uppercaseChar() == 'S' && zmkWithScheme.length >= 8) {
                val zmkUsage = zmkWithScheme.substring(6, 8)
                zmkLmkPair = A0GenerateKeyCommand.lmkPairFromKeyUsage(zmkUsage)
            } else {
                zmkLmkPair = 0 // Default: LMK pair 00-01 for ZMK/ZPK
            }

            simulator.hsmLogsListener.log(
                """A8 Export Key:
                |  Key Type: $keyTypeLabel
                |  ZMK/TMK Flag: $zmkTmkFlag (${if (zmkTmkFlag == '0') "ZMK" else "TMK"})
                |  ZMK/TMK: $zmkWithScheme
                |  ZMK LMK Pair: $zmkLmkPair
                |  Key under LMK: $keyUnderLmkWithScheme
                |  Key LMK Pair: $keyLmkPair
                |  Export Scheme: $exportScheme
                |  LMK ID: ${cmd.lmkId}""".trimMargin()
            )

            // 9. Decrypt key-under-LMK using the appropriate LMK pair
            val clearKey = decryptSchemeKeyUnderLmk(keyUnderLmkWithScheme, cmd.lmkId, keyLmkPair)

            // 10. Decrypt ZMK under its LMK pair
            val clearZmk = decryptSchemeKeyUnderLmk(zmkWithScheme, cmd.lmkId, zmkLmkPair)

            // 11. Re-encrypt the clear key under ZMK/TMK
            val kcv = simulator.calculateKeyCheckValue(clearKey)
            val exportedKey = if (exportScheme == 'S') {
                // KeyBlock output — use the ZMK key itself directly as KBPK (no derivation)
                // For FFF (KeyBlock) key type, preserve the key usage/mode/exportability from the input S-block
                val (usage, mode, export) = if (keyTypeCode == "FFF" &&
                    keyUnderLmkWithScheme.length >= 13 && keyUnderLmkWithScheme[0].uppercaseChar() == 'S') {
                    Triple(
                        keyUnderLmkWithScheme.substring(6, 8),   // key usage
                        keyUnderLmkWithScheme[9],                 // mode of use
                        keyUnderLmkWithScheme[12]                 // exportability
                    )
                } else {
                    simulator.keyTypeToBlockAttributes(keyTypeCode)
                }
                simulator.buildKeyBlock(clearKey, clearZmk, usage, mode, export, cmd.lmkId, useKeyDirectlyAsKbpk = true)
            } else if (clearZmk.size == 16 && clearZmk.size == clearKey.size) {
                // AES-128 ZMK wrapping AES-128 key — use AES-ECB
                val cipher = Cipher.getInstance("AES/ECB/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(clearZmk, "AES"))
                val encBytes = cipher.doFinal(clearKey)
                "$exportScheme${IsoUtil.bytesToHexString(encBytes)}"
            } else {
                // TDES ZMK wrapping
                val expandedZmk = when (clearZmk.size) {
                    8  -> ByteArray(24).also {
                        System.arraycopy(clearZmk, 0, it, 0, 8)
                        System.arraycopy(clearZmk, 0, it, 8, 8)
                        System.arraycopy(clearZmk, 0, it, 16, 8)
                    }
                    16 -> ByteArray(24).also {
                        System.arraycopy(clearZmk, 0, it, 0, 16)
                        System.arraycopy(clearZmk, 0, it, 16, 8)
                    }
                    else -> clearZmk
                }
                val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(expandedZmk, "DESede"))
                val encBytes = cipher.doFinal(clearKey)
                "$exportScheme${IsoUtil.bytesToHexString(encBytes)}"
            }

            simulator.hsmLogsListener.log(buildString {
                appendLine("A8 Export Key — Results:")
                appendLine("  Clear Key:\t\t${IsoUtil.bytesToHexString(clearKey)} (${clearKey.size * 8} bits)")
                appendLine("  Clear ZMK:\t\t${IsoUtil.bytesToHexString(clearZmk)} (${clearZmk.size * 8} bits)")
                appendLine("  Export Scheme:\t$exportScheme")
                appendLine("  Exported Key:\t\t$exportedKey")
                appendLine("  KCV:\t\t\t$kcv")
            })

            HsmCommandResult.Success(
                response = "$exportedKey$kcv",
                data = mapOf("exportedKey" to exportedKey, "kcv" to kcv)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "A8 failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // BU — Generate Key Check Value (legacy 2-digit key type code format)
    // Wire: BU + KeyTypeCode(2H) + KeyLengthFlag(1) + Key(variable)
    //          + [; + KeyType3(3H)]
    //          + [; + Reserved(1) + Reserved(1) + KcvType(1)]
    //          + [% + LmkId(2)]
    // Handles variant (U/T/X/Y) and KeyBlock ('S') keys.
    // Response: BV + 00 + KCV(6H)
    // ====================================================================================================
    private suspend fun executeBU(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // 1. Key Type Code (2 hex chars) — identifies the LMK pair
            if (pos + 2 > data.length) return HsmCommandResult.Error("15", "BU: missing key type code")
            val keyTypeCode2 = data.substring(pos, pos + 2).uppercase(); pos += 2

            // 2. Key Length Flag (1 char): 0=single, 1=double, 2=triple, F=keyblock
            if (pos >= data.length) return HsmCommandResult.Error("15", "BU: missing key length flag")
            val keyLengthFlag = data[pos].uppercaseChar(); pos++

            // 3. Key (scheme-prefixed or bare hex)
            val (keyWithScheme, keyLen) = extractSchemeKey(data, pos); pos += keyLen

            // 4. Optional: ; + 3-digit Key Type
            var keyType3: String? = null
            if (pos < data.length && data[pos] == ';') {
                pos++ // consume ;
                if (pos + 3 <= data.length) { keyType3 = data.substring(pos, pos + 3); pos += 3 }
            }

            // 5. Optional: ; + Reserved(1) + Reserved(1) + KcvType(1)
            var kcvType = "1" // default: 6-digit KCV
            if (pos < data.length && data[pos] == ';') {
                pos++ // consume ;
                if (pos + 3 <= data.length) {
                    kcvType = data[pos + 2].toString(); pos += 3
                }
            }

            // 6. Optional: % + LMK Identifier (2 chars)
            var effectiveLmkId = cmd.lmkId
            if (pos < data.length && data[pos] == '%') {
                pos++ // consume %
                if (pos + 2 <= data.length) { effectiveLmkId = data.substring(pos, pos + 2); pos += 2 }
            }

            // 7. Resolve LMK pair
            val lmkPair = when {
                // KeyBlock: read key usage from S-block header bytes 6-8
                keyLengthFlag == 'F' || keyWithScheme.firstOrNull()?.uppercaseChar() == 'S' -> {
                    val keyUsage = if (keyWithScheme.length >= 8) keyWithScheme.substring(6, 8) else "K0"
                    A0GenerateKeyCommand.lmkPairFromKeyUsage(keyUsage)
                }
                // Explicit 3-digit key type overrides the 2-digit code
                keyType3 != null ->
                    A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[keyType3]?.lmkPairNumber
                        ?: legacyKeyTypeCodeToLmkPair(keyTypeCode2)
                else -> legacyKeyTypeCodeToLmkPair(keyTypeCode2)
            }

            hsmLogsListener.log("[BU] keyTypeCode=$keyTypeCode2 keyLengthFlag=$keyLengthFlag keyType3=$keyType3 lmkPair=$lmkPair kcvType=$kcvType lmkId=$effectiveLmkId")

            // 8. Decrypt key under LMK
            val clearKey = decryptSchemeKeyUnderLmk(keyWithScheme, effectiveLmkId, lmkPair)

            // 9. Calculate and return KCV
            val kcv = if (kcvType == "0") "" else simulator.calculateKeyCheckValue(clearKey)

            HsmCommandResult.Success(
                response = kcv,
                data = mapOf("kcv" to kcv)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "BU failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // KA — Generate Key Check Value (key-first format)
    // Wire: KA + Key(variable) + KeyType(2H)
    //          + [; + Reserved(1) + Reserved(1) + KcvType(1)]
    //          + [% + LmkId(2)]
    // Handles variant (U/T/X/Y) and KeyBlock ('S') keys.
    // Response: KB + 00 + KCV(6H)
    // ====================================================================================================
    private suspend fun executeKA(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // 1. Key (scheme-prefixed or bare hex) — comes first
            val (keyWithScheme, keyLen) = extractSchemeKey(data, pos); pos += keyLen

            // 2. Key Type (2 hex chars) — identifies the LMK pair
            if (pos + 2 > data.length) return HsmCommandResult.Error("15", "KA: missing key type")
            val keyType2 = data.substring(pos, pos + 2).uppercase(); pos += 2

            // 3. Optional: ; + Reserved(1) + Reserved(1) + KcvType(1)
            var kcvType = "1" // default: 6-digit KCV
            if (pos < data.length && data[pos] == ';') {
                pos++ // consume ;
                if (pos + 3 <= data.length) {
                    kcvType = data[pos + 2].toString(); pos += 3
                }
            }

            // 4. Optional: % + LMK Identifier (2 chars)
            var effectiveLmkId = cmd.lmkId
            if (pos < data.length && data[pos] == '%') {
                pos++ // consume %
                if (pos + 2 <= data.length) { effectiveLmkId = data.substring(pos, pos + 2); pos += 2 }
            }

            // 5. Resolve LMK pair — S-block keys read usage from header; else map 2-digit code
            val lmkPair = when {
                keyWithScheme.firstOrNull()?.uppercaseChar() == 'S' -> {
                    val keyUsage = if (keyWithScheme.length >= 8) keyWithScheme.substring(6, 8) else "K0"
                    A0GenerateKeyCommand.lmkPairFromKeyUsage(keyUsage)
                }
                else -> legacyKeyTypeCodeToLmkPair(keyType2)
            }

            hsmLogsListener.log("[KA] keyType=$keyType2 lmkPair=$lmkPair kcvType=$kcvType lmkId=$effectiveLmkId")

            // 6. Decrypt key under LMK
            val clearKey = decryptSchemeKeyUnderLmk(keyWithScheme, effectiveLmkId, lmkPair)

            // 7. Calculate and return KCV
            val kcv = if (kcvType == "0") "" else simulator.calculateKeyCheckValue(clearKey)

            HsmCommandResult.Success(
                response = kcv,
                data = mapOf("kcv" to kcv)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "KA failed: ${e.message}")
        }
    }

    /**
     * Maps the 2-digit legacy key type code (BU/KA wire format) to the simulator LMK pair index.
     *
     * Thales key type → LMK pair mapping:
     *   00 → ZMK   (pair 04-05)    01 → ZPK   (pair 06-07)
     *   02 → TMK/TPK/PVK (14-15)  03 → TAK   (pair 16-17)
     *   04 → PVK   (pair 18-19)   05 → ZEK   (pair 20-21)
     *   06 → ZAK   (pair 22-23)   07 → BDK   (pair 24-25)
     *   08         (pair 26-27)   09         (pair 28-29)
     *   0A         (pair 30-31)   0B         (pair 32-33)
     *   10 → Variant 1 pair 04-05  42 → Variant 4 pair 14-15
     */
    private fun legacyKeyTypeCodeToLmkPair(code: String): Int = when (code.uppercase()) {
        "00", "10" -> PayShield10KCommandProcessor.LMK_PAIR_ZMK_ZPK  // ZMK / Variant 1 pair 04-05
        "01"       -> PayShield10KCommandProcessor.LMK_PAIR_ZMK_ZPK  // ZPK, pair 06-07
        "02", "42" -> PayShield10KCommandProcessor.LMK_PAIR_TPK      // TMK/TPK/PVK / Variant 4 pair 14-15
        "03"       -> PayShield10KCommandProcessor.LMK_PAIR_TAK      // TAK, pair 16-17
        "04"       -> PayShield10KCommandProcessor.LMK_PAIR_PVK      // PVK, pair 18-19
        "05"       -> PayShield10KCommandProcessor.LMK_PAIR_ZEK      // ZEK, pair 20-21
        "06"       -> PayShield10KCommandProcessor.LMK_PAIR_ZEK      // ZAK, pair 22-23
        "07"       -> PayShield10KCommandProcessor.LMK_PAIR_BDK      // BDK, pair 24-25
        "08"       -> PayShield10KCommandProcessor.LMK_PAIR_ZMK_ZPK  // pair 26-27
        "09"       -> PayShield10KCommandProcessor.LMK_PAIR_ZMK_ZPK  // pair 28-29
        "0A"       -> PayShield10KCommandProcessor.LMK_PAIR_TPK      // pair 30-31
        "0B"       -> PayShield10KCommandProcessor.LMK_PAIR_TAK      // pair 32-33
        else       -> PayShield10KCommandProcessor.LMK_PAIR_TPK
    }

    // ====================================================================================================
    // EI — Generate RSA Key Pair
    // Command: EI[KeyType_1H][ModulusLen_4N][Encoding_2N]
    //   KeyType: 0=RSA-OAEP, 1=RSA-PKCS1, 2=RSA-PSS
    //   ModulusLen: 1024, 2048, 4096
    //   Encoding:  01=DER, 02=PEM
    // Response: EJ 00 [PubKeyLen_4H][PubKey][PrivKeyLen_4H][PrivKey_enc_under_LMK]
    // ====================================================================================================
    private fun executeEI(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val keyType    = data.getOrElse(pos) { '1' }.toString(); pos++
            val modulusLen = data.substring(pos, minOf(pos + 4, data.length)).toIntOrNull() ?: 2048; pos += 4
            val encoding   = if (pos + 2 <= data.length) data.substring(pos, pos + 2) else "01"

            val validSize = when {
                modulusLen <= 1024 -> 1024
                modulusLen <= 2048 -> 2048
                else               -> 4096
            }

            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(validSize)
            val kp = kpg.generateKeyPair()

            val pubKeyBytes  = kp.public.encoded   // DER X.509
            val privKeyBytes = kp.private.encoded  // DER PKCS#8

            // In a real HSM the private key would be encrypted under LMK — store as hex here
            val pubHex  = IsoUtil.bytesToHexString(pubKeyBytes)
            val privHex = IsoUtil.bytesToHexString(privKeyBytes)

            val pubLen  = pubKeyBytes.size.toString(16).padStart(4, '0').uppercase()
            val privLen = privKeyBytes.size.toString(16).padStart(4, '0').uppercase()

            HsmCommandResult.Success(
                response = "$pubLen$pubHex$privLen$privHex",
                data = mapOf(
                    "publicKey"  to pubHex,
                    "privateKey" to privHex,
                    "modulusBits" to validSize.toString()
                )
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "EI failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // EO — Import / Validate RSA Public Key
    // Command: EO[Encoding_2N][PubKey_nH][;][AuthData_nH]
    // Response: EP 00 [KeyLen_4H][StoredKey_nH]
    // ====================================================================================================
    private fun executeEO(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val encoding = data.substring(pos, pos + 2); pos += 2
            val semiIdx  = data.indexOf(';', pos)
            val pubKeyHex = if (semiIdx > 0) data.substring(pos, semiIdx) else data.substring(pos)

            val pubKeyBytes = IsoUtil.hexToBytes(pubKeyHex)
            // Validate: try to reconstruct the public key
            val kf  = KeyFactory.getInstance("RSA")
            val pub = kf.generatePublic(X509EncodedKeySpec(pubKeyBytes))

            val storedHex = IsoUtil.bytesToHexString(pub.encoded)
            val storedLen = pub.encoded.size.toString(16).padStart(4, '0').uppercase()

            HsmCommandResult.Success(
                response = "$storedLen$storedHex",
                data = mapOf("publicKey" to storedHex)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("74", "EO failed — invalid public key: ${e.message}")
        }
    }

    // ====================================================================================================
    // EW — RSA Sign Data
    // Command: EW[HashAlg_2N][SigAlg_2N][PadMode_2N][DataLen_4N][Data_nH];[PrivKeyFlag_2N][PrivKeyLen_4H][PrivKey_nH]
    // Response: EX 00 [SigLen_4H][Signature_nH]
    // HashAlg: 01=SHA-1, 05=SHA-224, 06=SHA-256, 07=SHA-384, 08=SHA-512
    // ====================================================================================================
    private fun executeEW(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val hashAlg  = data.substring(pos, pos + 2); pos += 2
            val sigAlg   = data.substring(pos, pos + 2); pos += 2
            val padMode  = data.substring(pos, pos + 2); pos += 2

            val dataLenStr = data.substring(pos, pos + 4); pos += 4
            val dataLen    = dataLenStr.toIntOrNull() ?: 0
            val dataHex    = data.substring(pos, pos + dataLen); pos += dataLen

            // Skip ';' separator
            if (pos < data.length && data[pos] == ';') pos++

            val privKeyFlag = data.substring(pos, pos + 2); pos += 2
            val privKeyLenHex = data.substring(pos, pos + 4); pos += 4
            val privKeyLen    = privKeyLenHex.toInt(16)
            val privKeyHex    = data.substring(pos, pos + privKeyLen * 2)
            val privKeyBytes  = IsoUtil.hexToBytes(privKeyHex)

            val algoName = hashAlgoName(hashAlg)
            val sigAlgoStr = "SHA${algoName.removePrefix("SHA-")}with${
                when (padMode) {
                    "04" -> "RSA"  // EMSA-PSS → RSASSA-PSS, but Java uses "RSA" for standard
                    else -> "RSA"
                }
            }"
            val javaAlgo = when (hashAlg) {
                "01" -> "SHA1withRSA"
                "05" -> "SHA224withRSA"
                "06" -> "SHA256withRSA"
                "07" -> "SHA384withRSA"
                "08" -> "SHA512withRSA"
                else -> "SHA256withRSA"
            }

            val kf      = KeyFactory.getInstance("RSA")
            val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(privKeyBytes))
            val signer  = Signature.getInstance(javaAlgo)
            signer.initSign(privKey)
            signer.update(IsoUtil.hexToBytes(dataHex))
            val sigBytes = signer.sign()
            val sigHex   = IsoUtil.bytesToHexString(sigBytes)
            val sigLen   = sigBytes.size.toString(16).padStart(4, '0').uppercase()

            HsmCommandResult.Success(
                response = "$sigLen$sigHex",
                data = mapOf("signature" to sigHex)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "EW failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // EY — RSA Verify Signature
    // Command: EY[HashAlg_2N][SigAlg_2N][PadMode_2N][SigLen_4H][Sig_nH];[DataLen_4H][Data_nH];[MAC][PubKey_nH]
    // Response: EZ 00
    // ====================================================================================================
    private fun executeEY(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val hashAlg  = data.substring(pos, pos + 2); pos += 2
            val sigAlg   = data.substring(pos, pos + 2); pos += 2
            val padMode  = data.substring(pos, pos + 2); pos += 2

            val sigLenHex = data.substring(pos, pos + 4); pos += 4
            val sigLen    = sigLenHex.toInt(16)
            val sigHex    = data.substring(pos, pos + sigLen * 2); pos += sigLen * 2

            if (pos < data.length && data[pos] == ';') pos++

            val dataLenHex = data.substring(pos, pos + 4); pos += 4
            val dataLen    = dataLenHex.toInt(16)
            val dataHex    = data.substring(pos, pos + dataLen * 2); pos += dataLen * 2

            if (pos < data.length && data[pos] == ';') pos++
            // Skip optional MAC (8H = 16 chars)
            if (pos + 16 <= data.length) pos += 16

            val pubKeyHex  = data.substring(pos)
            val pubKeyBytes = IsoUtil.hexToBytes(pubKeyHex)

            val javaAlgo = when (hashAlg) {
                "01" -> "SHA1withRSA"
                "05" -> "SHA224withRSA"
                "06" -> "SHA256withRSA"
                "07" -> "SHA384withRSA"
                "08" -> "SHA512withRSA"
                else -> "SHA256withRSA"
            }

            val kf      = KeyFactory.getInstance("RSA")
            val pubKey  = kf.generatePublic(X509EncodedKeySpec(pubKeyBytes))
            val verifier = Signature.getInstance(javaAlgo)
            verifier.initVerify(pubKey)
            verifier.update(IsoUtil.hexToBytes(dataHex))
            val valid = verifier.verify(IsoUtil.hexToBytes(sigHex))

            if (valid) {
                HsmCommandResult.Success(response = "", data = mapOf("verified" to "true"))
            } else {
                HsmCommandResult.Error("01", "RSA signature verification failed")
            }
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "EY failed: ${e.message}")
        }
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

    /**
     * Decimalise a hex string using a 16-char table, returning [length] digits.
     * Two passes: first pass collects table entries that are 0-9; second pass collects
     * the remainder if needed (table entries that are digits when offset by 10).
     */
    private fun decimalizePin(hex: String, table: String, length: Int): String {
        val tbl = table.padEnd(16, '0')
        val result = StringBuilder()
        // Pass 1 — entries in 0-9 range
        for (c in hex) {
            val idx = c.digitToIntOrNull(16) ?: continue
            val mapped = tbl[idx]
            if (mapped.isDigit()) {
                result.append(mapped)
                if (result.length == length) return result.toString()
            }
        }
        // Pass 2 — entries A-F (subtract 10)
        for (c in hex) {
            val idx = c.digitToIntOrNull(16) ?: continue
            if (idx >= 10) {
                result.append(idx - 10)
                if (result.length == length) return result.toString()
            }
        }
        return result.toString().padEnd(length, '0')
    }

    private fun hashAlgoName(code: String): String = when (code) {
        "01" -> "SHA-1"
        "05" -> "SHA-224"
        "06" -> "SHA-256"
        "07" -> "SHA-384"
        "08" -> "SHA-512"
        else -> "SHA-256"
    }

    // ====================================================================================================
    // KEYBLOCK SCHEME HELPERS
    // ====================================================================================================

    /**
     * Extract a scheme-prefixed key from [data] starting at [pos].
     * Returns (keyWithPrefix, charsConsumed).
     *
     * Scheme lengths:
     *  U / X  → prefix(1) + 32H  = 33 chars
     *  T / Y  → prefix(1) + 48H  = 49 chars
     *  S / R  → prefix(1) + blockLen  (block length read from the keyblock header)
     *  (bare) → 32H (double-length TDES without a prefix char)
     */
    private fun extractSchemeKey(data: String, pos: Int): Pair<String, Int> {
        if (pos >= data.length) return "" to 0
        return when (data[pos].uppercaseChar()) {
            'U', 'X' -> {
                val end = minOf(pos + 33, data.length)
                data.substring(pos, end) to (end - pos)
            }
            'T', 'Y' -> {
                val end = minOf(pos + 49, data.length)
                data.substring(pos, end) to (end - pos)
            }
            'S', 'R' -> {
                // S + version(1) + blockLen(4 decimal) = 6 chars needed to know total length
                if (pos + 6 > data.length) return data.substring(pos) to (data.length - pos)
                val blockLen = data.substring(pos + 2, pos + 6).toIntOrNull()
                    ?: return data.substring(pos) to (data.length - pos)
                val end = minOf(pos + 1 + blockLen, data.length)
                data.substring(pos, end) to (end - pos)
            }
            else -> {
                // Bare hex — assume double-length (32 chars)
                val end = minOf(pos + 32, data.length)
                data.substring(pos, end) to (end - pos)
            }
        }
    }

    /**
     * Extract the raw encrypted-key hex from a scheme-prefixed key string.
     *  U/X/T/Y → strip the 1-char prefix.
     *  S/R     → skip the 17-char header (S + 16 header chars), strip the trailing 16H MAC.
     *  (bare)  → return as-is.
     */
    private fun getEncryptedKeyHex(keyWithScheme: String): String {
        if (keyWithScheme.isEmpty()) return ""
        return when (keyWithScheme[0].uppercaseChar()) {
            'U', 'X', 'T', 'Y' -> keyWithScheme.substring(1)
            'S', 'R' -> {
                // Header: S(1) + version(1) + blockLen(4) + usage(2) + algo(1) + mode(1)
                //         + keyVer(2) + export(1) + numOpt(2) + reserved(2) = 17 chars
                // Version 0: MAC = 4 bytes (8H); Version 1: MAC = 8 bytes (16H)
                val headerLen = 17
                val version = if (keyWithScheme.length > 1) keyWithScheme[1] else '1'
                val macLen = if (version == '0') 8 else 16
                if (keyWithScheme.length <= headerLen + macLen) return ""
                keyWithScheme.substring(headerLen, keyWithScheme.length - macLen)
            }
            else -> keyWithScheme
        }
    }

    /**
     * Decrypt a scheme-prefixed key under the given LMK pair.
     * Handles variant schemes (U/T/X/Y) and KeyBlock (S/R).
     * For S-block keys, uses proper key block decryption with KBPK derivation from LMK.
     */
    private suspend fun decryptSchemeKeyUnderLmk(
        keyWithScheme: String,
        lmkId: String,
        pairNumber: Int
    ): ByteArray {
        if (keyWithScheme.isNotEmpty() && keyWithScheme[0].uppercaseChar() == 'S') {
            // S-block key: use full key block decryption (derives KBPK from LMK)
            val lmk = simulator.lmkStorage.getLmk(lmkId)
            val pair = lmk?.getPair(pairNumber)
            val lmkPairKey = pair?.getCombinedKey() ?: ByteArray(0)
            return simulator.decryptKeyBlock(keyWithScheme, lmkPairKey, lmkId)
        }
        val encHex = getEncryptedKeyHex(keyWithScheme)
        val encBytes = IsoUtil.hexToBytes(encHex)
        return simulator.decryptUnderLmk(encBytes, lmkId, pairNumber)
    }

    /**
     * Decrypt a scheme-prefixed key that is encrypted under [decryptionKey] (clear bytes).
     * Handles variant schemes (U/T/X/Y) and KeyBlock (S/R).
     */
    private fun decryptSchemeKeyUnderClearKey(keyWithScheme: String, decryptionKey: ByteArray): ByteArray {
        val encHex = getEncryptedKeyHex(keyWithScheme)
        val encBytes = IsoUtil.hexToBytes(encHex)
        return try {
            val expandedKey = when (decryptionKey.size) {
                8  -> ByteArray(24).also {
                    System.arraycopy(decryptionKey, 0, it, 0, 8)
                    System.arraycopy(decryptionKey, 0, it, 8, 8)
                    System.arraycopy(decryptionKey, 0, it, 16, 8)
                }
                16 -> ByteArray(24).also {
                    System.arraycopy(decryptionKey, 0, it, 0, 16)
                    System.arraycopy(decryptionKey, 0, it, 16, 8)
                }
                else -> decryptionKey.copyOf(decryptionKey.size.coerceIn(16, 24))
            }
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(expandedKey, "DESede"))
            cipher.doFinal(encBytes)
        } catch (e: Exception) {
            encBytes
        }
    }

    /** LMK pair index for each key type — mirrors the mapping in PayShield10KCommandProcessor. */
    private fun KeyType.getLmkPairNumber(): Int = when (this) {
        KeyType.TYPE_000, KeyType.TYPE_001 -> PayShield10KCommandProcessor.LMK_PAIR_ZMK_ZPK  // pair 00-01
        KeyType.TYPE_002                   -> PayShield10KCommandProcessor.LMK_PAIR_TPK       // pair 14-15
        KeyType.TYPE_003                   -> PayShield10KCommandProcessor.LMK_PAIR_CVK       // pair 04-05
        KeyType.TYPE_008, KeyType.TYPE_009 -> PayShield10KCommandProcessor.LMK_PAIR_TAK       // pair 08-09
        KeyType.TYPE_109                   -> PayShield10KCommandProcessor.LMK_PAIR_ZEK       // pair 26-27
        KeyType.TYPE_209                   -> PayShield10KCommandProcessor.LMK_PAIR_BDK       // pair 28-29
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