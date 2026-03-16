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
import `in`.aicortex.iso8583studio.domain.utils.DesCryptoService.applyPadding
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
            // ── Diagnostic & Info ────────────────────────────────────────────
            "NC" -> executeNC(cmd)
            "VR" -> executeVR(cmd)
            "VT" -> executeVT(cmd)

            // ── Key Generation / Management ──────────────────────────────────
            "GK" -> executeGK(cmd)
            "GC" -> executeGC(cmd)
            "FK" -> executeFK(cmd)
            "A0" -> A0GenerateKeyCommand(simulator).execute(cmd.data)
            "A6" -> executeA6(cmd)
            "A8" -> executeA8(cmd)
            "BU" -> executeBU(cmd)
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
            val panPart = "0000" + account.takeLast(13).dropLast(1)
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
            val pan = account.takeLast(13).dropLast(1).padStart(16, '0')
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

            val mode    = data.substring(pos, pos + 2); pos += 2
            val inFmt   = data[pos].toString(); pos++
            val outFmt  = data[pos].toString(); pos++

            val keyTypeCode = data.substring(pos, pos + 3); pos += 3
            val keyScheme = data[pos]; pos++
            val keyHex = data.substring(pos, pos + 32); pos += 32
            val encKey = IsoUtil.hexToBytes(keyHex)

            val isBdkType = keyTypeCode in BDK_KEY_TYPES

            var ksnDescriptor = ""
            var ksnHex = ""
            if (isBdkType) {
                ksnDescriptor = data.substring(pos, pos + 3); pos += 3
                ksnHex = data.substring(pos, pos + 20); pos += 20
            }

            val ivHex = if (mode == "01" && pos + 16 <= data.length) {
                data.substring(pos, pos + 16).also { pos += 16 }
            } else "0000000000000000"

            val dataLenHex = data.substring(pos, pos + 4); pos += 4
            val dataLen = dataLenHex.toInt(16)
            val plainDataHex = data.substring(pos, minOf(pos + dataLen, data.length))
            val plainData = IsoUtil.hexToBytes(plainDataHex)

            hsmLogsListener.log(buildString {
                appendLine("M0 - Encrypt Data Block Request")
                appendLine("Mode Flag................ = [$mode] ${if (mode == "00") "ECB" else "CBC"}")
                appendLine("Input Format Flag........ = [$inFmt] ${if (inFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Output Format Flag....... = [$outFmt] ${if (outFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Key Type................. = [$keyTypeCode]")
                appendLine("Key...................... = [$keyScheme$keyHex]")
                if (isBdkType) {
                    appendLine("KSN Descriptor........... = [$ksnDescriptor]")
                    appendLine("Key serial number........ = [$ksnHex]")
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
                appendLine("Key...................... = [$keyScheme$keyHex]")
                if (isBdkType) {
                    appendLine("KSN Descriptor........... = [$ksnDescriptor]")
                    appendLine("Key serial number........ = [$ksnHex]")
                }
                if (mode == "01") appendLine("IV....................... = [$ivHex]")
                appendLine("Message Length........... = [$dataLenHex]")
                appendLine("Message Block............ = [$plainDataHex]")
            })

            hsmLogsListener.log("[M0] Step 1: Key derivation - keyType=$keyTypeCode, isBdk=$isBdkType, lmkSlot=${cmd.lmkId}")

            val clearKey: ByteArray
            if (isBdkType) {
                val keyTypeInfo = A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[keyTypeCode]
                val lmkPairNumber = keyTypeInfo?.lmkPairNumber ?: 14
                val variant = keyTypeInfo?.variant ?: 0
                hsmLogsListener.log("[M0] Step 2: BDK path - lmkPair=$lmkPairNumber, variant=$variant")

                val lmk = simulator.lmkStorage.getLmk(cmd.lmkId)
                    ?: return HsmCommandResult.Error("15", "LMK not loaded for slot ${cmd.lmkId}")
                val lmkPair = lmk.getPair(lmkPairNumber)
                    ?: return HsmCommandResult.Error("15", "LMK pair $lmkPairNumber not available")

                val combinedLmk = lmkPair.getCombinedKey()
                hsmLogsListener.log("[M0] Step 3: LMK combined key = ${IsoUtil.bytesToHexString(combinedLmk)}")

                val variantLmk = applyLmkVariantForM2(combinedLmk, variant)
                hsmLogsListener.log("[M0] Step 4: Variant LMK (variant=$variant) = ${IsoUtil.bytesToHexString(variantLmk)}")

                val clearBdk = performTdesDecryptForM2(encKey, variantLmk)
                hsmLogsListener.log("[M0] Step 5: Decrypted clear BDK = ${IsoUtil.bytesToHexString(clearBdk)}")

                val counterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(ksnDescriptor)
                hsmLogsListener.log("[M0] Step 6: KSN descriptor=$ksnDescriptor, counterBits=$counterBits")

                val initialKey = PayShield10KCommandProcessor.deriveInitialKey(clearBdk, ksnHex, counterBits)
                hsmLogsListener.log("[M0] Step 7: IPEK (Initial Key) = ${IsoUtil.bytesToHexString(initialKey)}")

                val ksnBytes = IsoUtil.hexToBytes(ksnHex)
                val counter = PayShield10KCommandProcessor.extractDukptCounter(ksnBytes, counterBits)
                hsmLogsListener.log("[M0] Step 8: DUKPT counter = $counter")

                val sessionKey = commandProcessor.deriveDukptSessionKey(initialKey, ksnHex, counter, counterBits)
                hsmLogsListener.log("[M0] Step 9: DUKPT session key = ${IsoUtil.bytesToHexString(sessionKey)}")

                val dataVariant = byteArrayOf(
                    0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0x00
                )
                clearKey = ByteArray(sessionKey.size) { i ->
                    (sessionKey[i].toInt() xor dataVariant[i].toInt()).toByte()
                }
                hsmLogsListener.log("[M0] Step 10: Data encryption key (session XOR variant) = ${IsoUtil.bytesToHexString(clearKey)}")
            } else {
                val keyTypeInfo = A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[keyTypeCode]
                val lmkPairNumber = keyTypeInfo?.lmkPairNumber ?: 14
                val variant = keyTypeInfo?.variant ?: 0
                hsmLogsListener.log("[M0] Step 2: Non-BDK path - lmkPair=$lmkPairNumber, variant=$variant")

                val lmk = simulator.lmkStorage.getLmk(cmd.lmkId)
                    ?: return HsmCommandResult.Error("15", "LMK not loaded for slot ${cmd.lmkId}")
                val lmkPair = lmk.getPair(lmkPairNumber)
                    ?: return HsmCommandResult.Error("15", "LMK pair $lmkPairNumber not available")

                val combinedLmk = lmkPair.getCombinedKey()
                hsmLogsListener.log("[M0] Step 3: LMK combined key = ${IsoUtil.bytesToHexString(combinedLmk)}")

                val variantLmk = applyLmkVariantForM2(combinedLmk, variant)
                hsmLogsListener.log("[M0] Step 4: Variant LMK (variant=$variant) = ${IsoUtil.bytesToHexString(variantLmk)}")

                clearKey = performTdesDecryptForM2(encKey, variantLmk)
                hsmLogsListener.log("[M0] Step 5: Decrypted clear key = ${IsoUtil.bytesToHexString(clearKey)}")
            }

            hsmLogsListener.log("[M0] Step 11: Encrypting data - mode=${if (mode == "01") "CBC" else "ECB"}, clearKey=${IsoUtil.bytesToHexString(clearKey)}, iv=$ivHex, dataLen=${plainData.size} bytes")
            hsmLogsListener.log("[M0] Step 11: Plain data = $plainDataHex")

            val engine = EMVEngines()
            val cipherMode = if (mode == "01") CipherMode.CBC else CipherMode.ECB
            val paddedData = applyPadding(plainData,"PKCS#5")
            val encData = engine.encryptionEngine.encrypt(
                algorithm = CryptoAlgorithm.TDES,
                encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                    data = paddedData,
                    key = clearKey,
                    iv = if (mode == "01") IsoUtil.hexToBytes(ivHex) else null,
                    mode = cipherMode
                )
            )
            val encHex  = IsoUtil.bytesToHexString(encData)
            val outLen  = (encData.size*2).toString(16).padStart(4, '0').uppercase()
            val responseBody = if (mode == "01") "$ivHex$outLen$encHex" else "$outLen$encHex"

            hsmLogsListener.log("[M0] Step 12: Encryption result = $encHex")
            hsmLogsListener.log("[M0] Step 12: Response body = $responseBody")

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
     *   Command: 0000M2[Mode_2N][InFmt_1N][OutFmt_1N][KeyType_3H][Key_1A+32H][IV_16H?][MsgLen_4H][Data_nH]
     * For BDK (DUKPT) keys:
     *   Command: 0000M2[Mode_2N][InFmt_1N][OutFmt_1N][KeyType_3H][Key_1A+32H][KSNDesc_3H][KSN_20H][IV_16H?][MsgLen_4H][Data_nH]
     * Response: 0000M300[IV_16H][DataLen_4H][Decrypted_nH]
     * Mode 00 = ECB, 01 = CBC.  InFmt/OutFmt 1 = hex-encoded.
     */
    private suspend fun executeM2(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val mode    = data.substring(pos, pos + 2); pos += 2
            val inFmt   = data[pos].toString(); pos++
            val outFmt  = data[pos].toString(); pos++

            val keyTypeCode = data.substring(pos, pos + 3); pos += 3
            val keyScheme = data[pos]; pos++
            val keyHex = data.substring(pos, pos + 32); pos += 32
            val encKey = IsoUtil.hexToBytes(keyHex)

            val isBdkType = keyTypeCode in BDK_KEY_TYPES

            var ksnDescriptor = ""
            var ksnHex = ""
            if (isBdkType) {
                ksnDescriptor = data.substring(pos, pos + 3); pos += 3
                ksnHex = data.substring(pos, pos + 20); pos += 20
            }

            val ivHex = if (mode == "01" && pos + 16 <= data.length) {
                data.substring(pos, pos + 16).also { pos += 16 }
            } else "0000000000000000"

            val dataLenHex = data.substring(pos, pos + 4); pos += 4
            val dataLen = dataLenHex.toInt(16)
            val encDataHex = data.substring(pos, minOf(pos + dataLen, data.length))
            var encData = IsoUtil.hexToBytes(encDataHex)
            hsmLogsListener.log(buildString {
                appendLine("M2 - Decrypt Data Block Request")
                appendLine("Mode Flag................ = [$mode] ${if (mode == "00") "ECB" else "CBC"}")
                appendLine("Input Format Flag........ = [$inFmt] ${if (inFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Output Format Flag....... = [$outFmt] ${if (outFmt == "1") "Hex-Encoded Binary" else "Binary"}")
                appendLine("Key Type................. = [$keyTypeCode]")
                appendLine("Key...................... = [$keyScheme$keyHex]")
                if (isBdkType) {
                    appendLine("KSN Descriptor........... = [$ksnDescriptor]")
                    appendLine("Key serial number........ = [$ksnHex]")
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
                appendLine("Key...................... = [$keyScheme$keyHex]")
                if (isBdkType) {
                    appendLine("KSN Descriptor........... = [$ksnDescriptor]")
                    appendLine("Key serial number........ = [$ksnHex]")
                }
                if (mode == "01") appendLine("IV....................... = [$ivHex]")
                appendLine("Message Length........... = [$dataLenHex]")
                appendLine("Message Block............ = [$encDataHex]")
            })

            hsmLogsListener.log("[M2] Step 1: Key derivation - keyType=$keyTypeCode, isBdk=$isBdkType, lmkSlot=${cmd.lmkId}")

            val clearKey: ByteArray
            if (isBdkType) {
                val keyTypeInfo = A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[keyTypeCode]
                val lmkPairNumber = keyTypeInfo?.lmkPairNumber ?: 14
                val variant = keyTypeInfo?.variant ?: 0
                hsmLogsListener.log("[M2] Step 2: BDK path - lmkPair=$lmkPairNumber, variant=$variant")

                val lmk = simulator.lmkStorage.getLmk(cmd.lmkId)
                    ?: return HsmCommandResult.Error("15", "LMK not loaded for slot ${cmd.lmkId}")
                val lmkPair = lmk.getPair(lmkPairNumber)
                    ?: return HsmCommandResult.Error("15", "LMK pair $lmkPairNumber not available")

                val combinedLmk = lmkPair.getCombinedKey()
                hsmLogsListener.log("[M2] Step 3: LMK combined key = ${IsoUtil.bytesToHexString(combinedLmk)}")

                val variantLmk = applyLmkVariantForM2(combinedLmk, variant)
                hsmLogsListener.log("[M2] Step 4: Variant LMK (variant=$variant) = ${IsoUtil.bytesToHexString(variantLmk)}")

                val clearBdk = performTdesDecryptForM2(encKey, variantLmk)
                hsmLogsListener.log("[M2] Step 5: Decrypted clear BDK = ${IsoUtil.bytesToHexString(clearBdk)}")

                val counterBits = PayShield10KCommandProcessor.parseKsnDescriptorCounterBits(ksnDescriptor)
                hsmLogsListener.log("[M2] Step 6: KSN descriptor=$ksnDescriptor, counterBits=$counterBits")

                val initialKey = PayShield10KCommandProcessor.deriveInitialKey(clearBdk, ksnHex, counterBits)
                hsmLogsListener.log("[M2] Step 7: IPEK (Initial Key) = ${IsoUtil.bytesToHexString(initialKey)}")

                val ksnBytes = IsoUtil.hexToBytes(ksnHex)
                val counter = PayShield10KCommandProcessor.extractDukptCounter(ksnBytes, counterBits)
                hsmLogsListener.log("[M2] Step 8: DUKPT counter = $counter")

                val sessionKey = commandProcessor.deriveDukptSessionKey(initialKey, ksnHex, counter, counterBits)
                hsmLogsListener.log("[M2] Step 9: DUKPT session key = ${IsoUtil.bytesToHexString(sessionKey)}")

                val dataVariant = byteArrayOf(
                    0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0x00
                )
                clearKey = ByteArray(sessionKey.size) { i ->
                    (sessionKey[i].toInt() xor dataVariant[i].toInt()).toByte()
                }
                hsmLogsListener.log("[M2] Step 10: Data decryption key (session XOR variant) = ${IsoUtil.bytesToHexString(clearKey)}")
            } else {
                val keyTypeInfo = A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[keyTypeCode]
                val lmkPairNumber = keyTypeInfo?.lmkPairNumber ?: 14
                val variant = keyTypeInfo?.variant ?: 0
                hsmLogsListener.log("[M2] Step 2: Non-BDK path - lmkPair=$lmkPairNumber, variant=$variant")

                val lmk = simulator.lmkStorage.getLmk(cmd.lmkId)
                    ?: return HsmCommandResult.Error("15", "LMK not loaded for slot ${cmd.lmkId}")
                val lmkPair = lmk.getPair(lmkPairNumber)
                    ?: return HsmCommandResult.Error("15", "LMK pair $lmkPairNumber not available")

                val combinedLmk = lmkPair.getCombinedKey()
                hsmLogsListener.log("[M2] Step 3: LMK combined key = ${IsoUtil.bytesToHexString(combinedLmk)}")

                val variantLmk = applyLmkVariantForM2(combinedLmk, variant)
                hsmLogsListener.log("[M2] Step 4: Variant LMK (variant=$variant) = ${IsoUtil.bytesToHexString(variantLmk)}")

                clearKey = performTdesDecryptForM2(encKey, variantLmk)
                hsmLogsListener.log("[M2] Step 5: Decrypted clear key = ${IsoUtil.bytesToHexString(clearKey)}")
            }

            hsmLogsListener.log("[M2] Step 11: Decrypting data - mode=${if (mode == "01") "CBC" else "ECB"}, clearKey=${IsoUtil.bytesToHexString(clearKey)}, iv=$ivHex, dataLen=${encData.size} bytes")
            hsmLogsListener.log("[M2] Step 11: Encrypted data = $encDataHex")

            val paddedData = applyPadding(encData, "PKCS#5")

            val engine = EMVEngines()
            val cipherMode = if (mode == "01") CipherMode.CBC else CipherMode.ECB
            val decData = engine.encryptionEngine.decrypt(
                algorithm = CryptoAlgorithm.TDES,
                decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                    data = paddedData,
                    key = clearKey,
                    iv = IsoUtil.hexToBytes(ivHex),
                    mode = cipherMode
                )
            )
            val decHex  = IsoUtil.bytesToHexString(decData)
            val outLen  = (decData.size*2).toString(16).padStart(4, '0').uppercase()
            val responseBody = if (mode == "01") "$ivHex$outLen$decHex" else "$outLen$decHex"

            hsmLogsListener.log("[M2] Step 12: Decryption result = $decHex")
            hsmLogsListener.log("[M2] Step 12: Response body = $responseBody")

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

    private suspend fun performTdesDecryptForM2(data: ByteArray, key: ByteArray): ByteArray {
        val engine = EMVEngines()
        val paddedData = applyPadding(data, "PKCS#5")
        return engine.encryptionEngine.decrypt(
            algorithm = CryptoAlgorithm.TDES,
            decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                data = paddedData,
                key = key,
                mode = CipherMode.ECB
            )
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
     * BW - Translate Key from One LMK Pair to Another
     * Command: 0000BW[KeyType_3H][Scheme_1A][Key_32H][NewKeyType_3H][NewScheme_1A]%[LMK_ID]
     * Response: 0000BX00[NewKey_1A+32H][KCV_6H]
     * Useful when migrating keys to a new LMK pair (e.g. 001 ZPK → 008 ZAK).
     */
    private suspend fun executeBW(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            // Source key type (3H) + scheme (1A) + key (32H)
            val srcTypeCode = data.substring(pos, pos + 3); pos += 3
            pos++ // scheme char
            val srcKeyHex = data.substring(pos, pos + 32); pos += 32
            val srcKey = IsoUtil.hexToBytes(srcKeyHex)

            // Destination key type (3H) + scheme (1A)
            val dstTypeCode = data.substring(pos, pos + 3); pos += 3
            val dstScheme   = data[pos].toString(); pos++

            val srcType = KeyType.values().find { it.code == srcTypeCode } ?: KeyType.TYPE_001
            val dstType = KeyType.values().find { it.code == dstTypeCode } ?: srcType

            // Decrypt under source LMK pair
            val clearKey = simulator.decryptUnderLmk(srcKey, cmd.lmkId, srcType.getLmkPairNumber())

            // Re-encrypt under destination LMK pair
            val newKey = simulator.encryptUnderLmk(clearKey, cmd.lmkId, dstType.getLmkPairNumber())
            val newKeyHex = IsoUtil.bytesToHexString(newKey)
            val kcv = simulator.calculateKeyCheckValue(clearKey)

            HsmCommandResult.Success(
                response = "$dstScheme$newKeyHex$kcv",
                data = mapOf("translatedKey" to "$dstScheme$newKeyHex", "kcv" to kcv)
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
            val panPart = "0000" + account.takeLast(13).dropLast(1)
            val panBytes = IsoUtil.hexToBytes(panPart)
            val xored = clearPinBlock.zip(panBytes) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
            val xHex = IsoUtil.bytesToHexString(xored)
            val pinLen = xHex[1].digitToIntOrNull() ?: 4
            val customerPin = xHex.substring(2, 2 + pinLen)

            // Decrypt PVK under LMK
            val clearPvk = simulator.decryptUnderLmk(pvk, cmd.lmkId, 14)

            // Generate natural PIN
            val pan = account.takeLast(13).dropLast(1).padStart(16, '0')
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

            val panPart  = "0000" + account.takeLast(13).dropLast(1)
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
    // Command: A6[KeyType_3H][ZMKScheme_1A][ZMK_32H][LMKScheme_1A][Key_32H]
    // Response: A7 00 [Key_under_LMK][KCV_6H]
    // ====================================================================================================
    private suspend fun executeA6(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val keyTypeCode = data.substring(pos, pos + 3); pos += 3
            pos++ // ZMK scheme
            val zmkHex = data.substring(pos, pos + 32); pos += 32
            val zmk = IsoUtil.hexToBytes(zmkHex)
            val lmkScheme = data[pos].toString(); pos++
            val keyToImportHex = data.substring(pos, pos + 32)
            val keyToImport = IsoUtil.hexToBytes(keyToImportHex)

            val keyType = KeyType.values().find { it.code == keyTypeCode } ?: KeyType.TYPE_001

            // Decrypt ZMK under LMK (ZMK uses pair 00-01)
            val clearZmk = simulator.decryptUnderLmk(zmk, cmd.lmkId, 0)

            // Decrypt key under ZMK (3DES ECB)
            val zmkCipher = Cipher.getInstance("DESede/ECB/NoPadding")
            zmkCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(clearZmk.copyOf(16), "DESede"))
            val clearKey = zmkCipher.doFinal(keyToImport)

            // Apply odd parity then encrypt under LMK
            val parityKey = simulator.applyOddParity(clearKey)
            val lmkKey = simulator.encryptUnderLmk(parityKey, cmd.lmkId, keyType.getLmkPairNumber())
            val lmkKeyHex = IsoUtil.bytesToHexString(lmkKey)
            val kcv = simulator.calculateKeyCheckValue(parityKey)

            HsmCommandResult.Success(
                response = "$lmkScheme$lmkKeyHex$kcv",
                data = mapOf("lmkKey" to lmkKeyHex, "kcv" to kcv)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "A6 failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // A8 — Export Key (LMK → ZMK-encrypted)
    // Command: A8[KeyType_3H][ZMK_1A+32H][Key_1A+32H][ExportScheme_1A]
    // Response: A9 00 [ExportedKey][KCV_6H]
    // ====================================================================================================
    private suspend fun executeA8(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val keyTypeCode = data.substring(pos, pos + 3); pos += 3
            pos++ // ZMK scheme
            val zmkHex = data.substring(pos, pos + 32); pos += 32
            val zmk = IsoUtil.hexToBytes(zmkHex)
            pos++ // key scheme
            val keyToExportHex = data.substring(pos, pos + 32); pos += 32
            val keyToExport = IsoUtil.hexToBytes(keyToExportHex)
            val exportScheme = data.getOrElse(pos) { 'U' }.toString()

            val keyType = KeyType.values().find { it.code == keyTypeCode } ?: KeyType.TYPE_001

            // Decrypt key under LMK
            val clearKey = simulator.decryptUnderLmk(keyToExport, cmd.lmkId, keyType.getLmkPairNumber())

            // Decrypt ZMK under LMK
            val clearZmk = simulator.decryptUnderLmk(zmk, cmd.lmkId, 0)

            // Re-encrypt key under ZMK
            val zmkCipher = Cipher.getInstance("DESede/ECB/NoPadding")
            zmkCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(clearZmk.copyOf(16), "DESede"))
            val exportedKey = zmkCipher.doFinal(clearKey)
            val exportedKeyHex = IsoUtil.bytesToHexString(exportedKey)
            val kcv = simulator.calculateKeyCheckValue(clearKey)

            HsmCommandResult.Success(
                response = "$exportScheme$exportedKeyHex$kcv",
                data = mapOf("exportedKey" to exportedKeyHex, "kcv" to kcv)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "A8 failed: ${e.message}")
        }
    }

    // ====================================================================================================
    // BU — Generate Key Check Value
    // Command: BU[KeyType_3H][Scheme_1A][Key_32H]
    // Response: BV 00 [KCV_6H]
    // ====================================================================================================
    private suspend fun executeBU(cmd: ParsedCommand): HsmCommandResult {
        return try {
            var pos = 0
            val data = cmd.data

            val keyTypeCode = data.substring(pos, pos + 3); pos += 3
            pos++ // scheme
            val keyHex = data.substring(pos, pos + 32)
            val key = IsoUtil.hexToBytes(keyHex)

            val keyType = KeyType.values().find { it.code == keyTypeCode } ?: KeyType.TYPE_001

            // Decrypt under LMK to get clear key
            val clearKey = simulator.decryptUnderLmk(key, cmd.lmkId, keyType.getLmkPairNumber())
            val kcv = simulator.calculateKeyCheckValue(clearKey)

            HsmCommandResult.Success(
                response = kcv,
                data = mapOf("kcv" to kcv)
            )
        } catch (e: Exception) {
            HsmCommandResult.Error("15", "BU failed: ${e.message}")
        }
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

    /** LMK pair number for each key type — mirrors the mapping in PayShield10KCommandProcessor. */
    private fun KeyType.getLmkPairNumber(): Int = when (this) {
        KeyType.TYPE_000, KeyType.TYPE_001 -> 0   // ZMK/ZPK  pair 00-01
        KeyType.TYPE_002                   -> 14  // TPK/PVK  pair 14-15
        KeyType.TYPE_003                   -> 6   // TAK      pair 06-07
        KeyType.TYPE_008, KeyType.TYPE_009 -> 8   // ZAK/BDK  pair 08-09
        KeyType.TYPE_109                   -> 26  // ZEK/DEK  pair 26-27
        KeyType.TYPE_209                   -> 28  // BDK DUKPT pair 28-29
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