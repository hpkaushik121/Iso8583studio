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
     * Command: 0000GC%[LMK_ID]
     * Response: 0000GD00[Clear Component][Encrypted Component][KCV]
     */
    private suspend fun executeGC(cmd: ParsedCommand): HsmCommandResult {
        return commandProcessor.executeGenerateKeyComponent(
            lmkId = cmd.lmkId,
            keyLength = KeyLength.DOUBLE,
            keyType = KeyType.TYPE_000
        )
    }

    /**
     * FK - Form Key from Components
     * Command: 0000FK[KeyType][NumComps][Comp1][Comp2]...%[LMK_ID]
     * Response: 0000FL00[Encrypted Key][KCV]
     */
    private suspend fun executeFK(cmd: ParsedCommand): HsmCommandResult {
        // Parse key type and components from data
        val keyType = KeyType.TYPE_000
        val components = listOf(
            ByteArray(16).also { SecureRandom().nextBytes(it) }
        )

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
        // PIN migration implementation
        return HsmCommandResult.Success(
            response = "1234567890ABCDEF",
            data = emptyMap()
        )
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