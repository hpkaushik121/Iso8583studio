package `in`.aicortex.iso8583studio.hsm.payshield10k

import `in`.aicortex.iso8583studio.hsm.HsmConfig
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AcquirerProfile
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditEntry
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditLog
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditType
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuthActivity
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuthorizationRecord
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmCommandResult
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmErrorCodes
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmState
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkPair
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkSet
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkStorage
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.TerminalKeyProfile
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Main HSM Simulator class
 * Implements PayShield 10K command set
 */
class PayShield10KFeatures(val hsmConfig: HsmConfig,hsmLogsListener: HsmLogsListener) {

    // State Management
    var currentState: HsmState = HsmState.OFFLINE
    val lmkStorage = hsmConfig.lmkStorage
    private val authorizations = mutableMapOf<String, MutableList<AuthorizationRecord>>()
    val auditLog = AuditLog(hsmLogsListener)
    val slotManager = HsmSlotManager()

    // Security Configuration
    private var enabledCommands = mutableSetOf<String>()
    private var blockedCommands = mutableSetOf<String>()
    private var enabledPinBlockFormats = mutableSetOf<String>("01", "03", "47", "48")

    // System Configuration
    private var messageHeaderLength = 4
    private var systemDateTime = LocalDateTime.now()
    private var selfTestTime = "09:00"


    // Random number generator
    val secureRandom = SecureRandom()

    init {
        // Initialize with default enabled commands
        initializeDefaultCommands()
    }

    private fun initializeDefaultCommands() {
        // Enable core commands by default
        enabledCommands.addAll(listOf(
            "NC", "VR", "GK", "LK", "VT", "A", "C",
            "GC", "GS", "EC", "FK", "KG", "IK", "KE", "CK"
        ))
    }

    // ====================================================================================================
    // CONSOLE COMMANDS - CONFIGURATION
    // ====================================================================================================

    /**
     * CONFIGCMDS - Configure enabled/disabled commands
     */
    fun executeConfigCmds(operation: String = "VIEW"): HsmCommandResult {
        return when {
            operation == "VIEW" -> {
                val hostCmds = enabledCommands.filter { it.length == 2 && it[0].isLetterOrDigit() }
                val consoleCmds = enabledCommands.filter { it.length > 2 }
                HsmCommandResult.Success(
                    "Enabled Host: ${hostCmds.joinToString(" ")}\nEnabled Console: ${consoleCmds.joinToString(" ")}"
                )
            }
            operation.startsWith("+H") -> {
                val cmd = operation.substring(2)
                enabledCommands.add(cmd)
                HsmCommandResult.Success("Command $cmd enabled")
            }
            operation.startsWith("-H") -> {
                val cmd = operation.substring(2)
                enabledCommands.remove(cmd)
                HsmCommandResult.Success("Command $cmd disabled")
            }
            else -> HsmCommandResult.Error("15", "Invalid operation")
        }
    }

    /**
     * CONFIGPB - Configure PIN block formats
     */
    fun executeConfigPinBlock(format: String? = null, enable: Boolean = true): HsmCommandResult {
        return if (format == null) {
            // View enabled formats
            HsmCommandResult.Success("Enabled PIN Block Formats: ${enabledPinBlockFormats.joinToString(", ")}")
        } else {
            if (enable) {
                enabledPinBlockFormats.add(format)
            } else {
                enabledPinBlockFormats.remove(format)
            }
            HsmCommandResult.Success("PIN Block format $format ${if (enable) "enabled" else "disabled"}")
        }
    }

    // ====================================================================================================
    // CONSOLE COMMANDS - DIAGNOSTIC
    // ====================================================================================================

    /**
     * NC - Diagnostic Test
     */
    fun executeDiagnosticTest(): HsmCommandResult {
        auditLog.addEntry(
            AuditEntry(
            entryType = AuditType.CONSOLE_COMMAND,
            command = "NC",
            result = "SUCCESS",
            details = "Diagnostic test passed"
        )
        )

        val lmkCheckValue = lmkStorage.getLmk(lmkStorage.defaultLmkId)?.checkValue ?: "000000000000000"
        val firmwareVersion = "0010-E000"

        return HsmCommandResult.Success(
            response = "NC${HsmErrorCodes.SUCCESS}${lmkCheckValue}${firmwareVersion}",
            data = mapOf(
                "lmkCheckValue" to lmkCheckValue,
                "firmwareVersion" to firmwareVersion
            )
        )
    }

    /**
     * VR - View Software Revision
     */
    fun executeViewRevision(): HsmCommandResult {
        val versionInfo = """
            payShield 10K Console Version: 1.8a
            HSM Software: 1.0.10
            Bootstrap Version: 1.1.40
            Licenses: Premium Package
            - Premium Key Management
            - EMV Chip Issuing & Processing
            - Data Protection
            - DUKPT Support
        """.trimIndent()

        return HsmCommandResult.Success(versionInfo)
    }

    /**
     * GETTIME - Query current time
     */
    fun executeGetTime(): HsmCommandResult {
        val formatter = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss yyyy")
        return HsmCommandResult.Success("System date and time: ${systemDateTime.format(formatter)}")
    }

    /**
     * SETTIME - Set system time
     */
    fun executeSetTime(dateTime: LocalDateTime): HsmCommandResult {
        if (currentState != HsmState.SECURE || !isAuthorized(lmkStorage.defaultLmkId, AuthActivity.ADMIN_CONSOLE)) {
            return HsmCommandResult.Error(HsmErrorCodes.CONSOLE_NOT_AUTHORIZED, "Command only allowed from Secure-Authorized")
        }

        systemDateTime = dateTime
        auditLog.addEntry(AuditEntry(
            entryType = AuditType.CONFIGURATION_CHANGE,
            command = "SETTIME",
            result = "SUCCESS",
            details = "Time changed to $dateTime"
        ))

        return HsmCommandResult.Success("System time has been modified")
    }

    // ====================================================================================================
    // CONSOLE COMMANDS - LMK MANAGEMENT
    // ====================================================================================================

    /**
     * GK - Generate LMK Component
     * Generates a random key component for LMK creation
     */
    fun executeGenerateLmkComponent(
        lmkId: String = "00",
        componentNumber: Int = 1
    ): HsmCommandResult {
        if (currentState != HsmState.OFFLINE && currentState != HsmState.SECURE) {
            return HsmCommandResult.Error(HsmErrorCodes.COMMAND_ONLY_ALLOWED_IN_SECURE_STATE, "Must be in Offline or Secure state")
        }

        // Generate random 16-byte key component (double-length DES)
        val component = ByteArray(16)
        secureRandom.nextBytes(component)

        // Apply odd parity to each byte
        val parityComponent = applyOddParity(component)

        // Calculate encrypted form under existing LMK (if any)
        val encryptedComponent = if (lmkStorage.getLmk(lmkId) != null) {
            encryptUnderLmk(parityComponent, lmkId, 0) // LMK Pair 00-01
        } else {
            parityComponent
        }

        auditLog.addEntry(AuditEntry(
            entryType = AuditType.KEY_OPERATION,
            command = "GK",
            lmkId = lmkId,
            result = "SUCCESS",
            details = "Component $componentNumber generated"
        ))

        return HsmCommandResult.Success(
            response = "Component ${componentNumber}: ${bytesToHex(parityComponent)}",
            data = mapOf(
                "clearComponent" to bytesToHex(parityComponent),
                "encryptedComponent" to bytesToHex(encryptedComponent),
                "checkValue" to calculateKeyCheckValue(parityComponent)
            )
        )
    }

    /**
     * LK - Load LMK from components
     */
    fun executeLoadLmk(
        lmkId: String,
        components: List<ByteArray>
    ): HsmCommandResult {
        if (currentState != HsmState.OFFLINE) {
            return HsmCommandResult.Error(HsmErrorCodes.COMMAND_ONLY_ALLOWED_IN_SECURE_STATE, "Must be in Offline state")
        }

        if (components.size < 2) {
            return HsmCommandResult.Error(HsmErrorCodes.INVALID_INPUT_DATA, "At least 2 components required")
        }

        // XOR all components to create LMK
        val lmkKey = components.reduce { acc, component ->
            acc.zip(component) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
        }

        // Create LMK pairs (14 pairs total)
        val lmkSet = LmkSet(identifier = lmkId)

        // Generate 14 pairs from the master key using key derivation
        for (pairNum in 0 until 40) {
            val derivedLeft = deriveKeyComponent(lmkKey, pairNum * 2)
            val derivedRight = deriveKeyComponent(lmkKey, pairNum * 2 + 1)
            lmkSet.pairs[pairNum] = LmkPair(derivedLeft, derivedRight)
        }

        lmkStorage.addLmk(lmkSet)
        currentState = HsmState.ONLINE
        val allocationResult = slotManager.allocateLmkSlot(lmkId, lmkSet, isDefault = true)
        if(allocationResult is HsmCommandResult.Error){
            auditLog.addEntry(AuditEntry(
                entryType = AuditType.KEY_OPERATION,
                command = "LK",
                lmkId = lmkId,
                result = "FAILED",
                details = "LMK allocation failed from (${allocationResult.errorCode}) ${allocationResult.message}"
            ))
            return allocationResult
        }

        auditLog.addEntry(AuditEntry(
            entryType = AuditType.KEY_OPERATION,
            command = "LK",
            lmkId = lmkId,
            result = "SUCCESS",
            details = "LMK loaded from ${components.size} components"
        ))

        return HsmCommandResult.Success(
            response = "LMK $lmkId loaded successfully\nCheck Value: ${lmkSet.checkValue}",
            data = mapOf(
                "lmkId" to lmkId,
                "checkValue" to lmkSet.checkValue
            )
        )
    }

    /**
     * VT - View LMK Table
     */
    fun executeViewLmkTable(): HsmCommandResult {
        val table = StringBuilder()
        table.appendLine("LMK IDENTIFIER | STATUS | CHECK VALUE | SCHEME")
        table.appendLine("------------------------------------------------")

        lmkStorage.liveLmks.forEach { (id, lmk) ->
            table.appendLine("$id | LOADED | ${lmk.checkValue} | ${lmk.scheme}")
        }

        if (lmkStorage.oldLmk != null) {
            table.appendLine("OLD | LOADED | ${lmkStorage.oldLmk!!.checkValue} | ${lmkStorage.oldLmk!!.scheme}")
        }

        if (lmkStorage.newLmk != null) {
            table.appendLine("NEW | LOADED | ${lmkStorage.newLmk!!.checkValue} | ${lmkStorage.newLmk!!.scheme}")
        }

        return HsmCommandResult.Success(table.toString())
    }

    /**
     * DM - Delete LMK
     */
    fun executeDeleteLmk(lmkId: String): HsmCommandResult {
        if (currentState != HsmState.OFFLINE) {
            return HsmCommandResult.Error(HsmErrorCodes.COMMAND_ONLY_ALLOWED_IN_SECURE_STATE, "Must be in Offline state")
        }

        if (!lmkStorage.liveLmks.containsKey(lmkId)) {
            return HsmCommandResult.Error(HsmErrorCodes.INVALID_LMK_IDENTIFIER, "LMK not found")
        }

        lmkStorage.deleteLmk(lmkId)

        auditLog.addEntry(AuditEntry(
            entryType = AuditType.KEY_OPERATION,
            command = "DM",
            lmkId = lmkId,
            result = "SUCCESS",
            details = "LMK deleted"
        ))

        return HsmCommandResult.Success("LMK $lmkId deleted")
    }

    // CONTINUED IN NEXT PART...

    // ====================================================================================================
    // UTILITY FUNCTIONS
    // ====================================================================================================

    /**
     * Apply odd parity to DES key bytes
     */
    fun applyOddParity(key: ByteArray): ByteArray {
        return key.map { byte ->
            var b = byte.toInt() and 0xFF
            var parity = 0
            for (i in 0 until 7) {
                parity = parity xor ((b shr i) and 1)
            }
            ((b and 0xFE) or parity).toByte()
        }.toByteArray()
    }

    /**
     * Convert bytes to hex string
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    /**
     * Convert hex string to bytes
     */
    fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    /**
     * Calculate key check value (KCV) for DES/TDES/AES keys
     *
     * Key sizes and algorithms:
     * - 8 bytes  → Single DES
     * - 16 bytes → Double-length TDES (2TDEA) - needs K1-K2-K1 expansion to 24 bytes
     * - 24 bytes → Triple-length TDES (3TDEA)
     * - 16/24/32 bytes → AES (if > 24 bytes)
     */
    fun calculateKeyCheckValue(key: ByteArray): String {
        try {
            val (cipher, keySpec) = when (key.size) {
                8 -> {
                    // Single DES
                    Cipher.getInstance("DES/ECB/NoPadding") to
                            SecretKeySpec(key, "DES")
                }
                16 -> {
                    // Double-length 3DES (2TDEA): K1-K2-K1 format
                    // Java DESede requires 24 bytes, so we expand 16 → 24
                    val expandedKey = ByteArray(24)
                    System.arraycopy(key, 0, expandedKey, 0, 16)  // K1-K2
                    System.arraycopy(key, 0, expandedKey, 16, 8)  // K1 again
                    Cipher.getInstance("DESede/ECB/NoPadding") to
                            SecretKeySpec(expandedKey, "DESede")
                }
                24 -> {
                    // Triple-length 3DES (3TDEA): K1-K2-K3
                    Cipher.getInstance("DESede/ECB/NoPadding") to
                            SecretKeySpec(key, "DESede")
                }
                else -> {
                    // AES: 16, 24, or 32 bytes
                    val aesKey = when {
                        key.size >= 32 -> key.copyOf(32)
                        key.size >= 24 -> key.copyOf(24)
                        else -> key.copyOf(16)
                    }
                    Cipher.getInstance("AES/ECB/NoPadding") to
                            SecretKeySpec(aesKey, "AES")
                }
            }

            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            // KCV = first 3 bytes of encryption of 8 or 16 zero bytes
            val encrypted = cipher.doFinal(ByteArray(cipher.blockSize))
            return encrypted.take(3).joinToString("") { "%02X".format(it) }

        } catch (e: Exception) {
            // Log for debugging
            println("KCV calculation failed for key size ${key.size}: ${e.message}")
            return "000000"
        }
    }

    /**
     * Encrypt data under LMK
     */
    fun encryptUnderLmk(data: ByteArray, lmkId: String, pairNumber: Int): ByteArray {
        val lmk = lmkStorage.getLmk(lmkId) ?: return data
        val pair = lmk.getPair(pairNumber) ?: return data

        return try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            val keySpec = SecretKeySpec(pair.getCombinedKey(), "DESede")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)

            // Pad data if necessary
            val paddedData = if (data.size % 8 != 0) {
                data + ByteArray(8 - (data.size % 8))
            } else {
                data
            }

            cipher.doFinal(paddedData)
        } catch (e: Exception) {
            data
        }
    }

    /**
     * Decrypt data under LMK
     */
    fun decryptUnderLmk(data: ByteArray, lmkId: String, pairNumber: Int): ByteArray {
        val lmk = lmkStorage.getLmk(lmkId) ?: return data
        val pair = lmk.getPair(pairNumber) ?: return data

        return try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            val keySpec = SecretKeySpec(pair.getCombinedKey(), "DESede")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            cipher.doFinal(data)
        } catch (e: Exception) {
            data
        }
    }

    /**
     * Derive key component using diversification
     */
    private fun deriveKeyComponent(masterKey: ByteArray, index: Int): ByteArray {
        val diversificationData = ByteArray(8) { index.toByte() }
        return try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            val keySpec = SecretKeySpec(masterKey.copyOf(16), "DESede")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            cipher.doFinal(diversificationData)
        } catch (e: Exception) {
            ByteArray(8).also { secureRandom.nextBytes(it) }
        }
    }

    /**
     * Check if HSM is authorized for specific activity
     */
    fun isAuthorized(lmkId: String, activity: AuthActivity): Boolean {
        val authList = authorizations[lmkId] ?: return false
        val now = System.currentTimeMillis()
        return authList.any {
            it.activity == activity && it.expiresAt > now
        }
    }
}