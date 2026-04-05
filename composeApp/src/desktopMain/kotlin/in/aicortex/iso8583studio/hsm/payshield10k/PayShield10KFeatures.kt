package `in`.aicortex.iso8583studio.hsm.payshield10k

import ai.cortex.core.types.CryptoAlgorithm
import `in`.aicortex.iso8583studio.hsm.HsmConfig
import `in`.aicortex.iso8583studio.hsm.HsmFeatures
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AcquirerProfile
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditEntry
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditLog
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditType
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuthActivity
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuthorizationRecord
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmCommandResult
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmErrorCodes
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmState
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkAlgorithm
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkPair
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkSet
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkStorage
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.TerminalKeyProfile
import io.cryptocalc.crypto.engines.encryption.CryptoLogger
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.crypto.engines.encryption.models.SymmetricDecryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Main HSM Simulator class
 * Implements PayShield 10K command set
 */
class PayShield10KFeatures(val hsmConfig: HsmConfig,val hsmLogsListener: HsmLogsListener): HsmFeatures {

    // State Management
    var currentState: HsmState = HsmState.OFFLINE
    val lmkStorage = hsmConfig.lmkStorage
    private val authorizations = mutableMapOf<String, MutableList<AuthorizationRecord>>()
    val auditLog = AuditLog(hsmLogsListener)
    private val slotManager = HsmSlotManager()
    private fun engine() = EMVEngines(CryptoLogger { message -> hsmLogsListener.log(message) })

    // Security Configuration
    private var enabledCommands = mutableSetOf<String>()
    private var blockedCommands = mutableSetOf<String>()
    private var enabledPinBlockFormats = mutableSetOf<String>("01", "03", "47", "48")

    // System Configuration
    private var messageHeaderLength = hsmConfig.messageHeaderLength
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
    suspend fun executeGenerateLmkComponent(
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
     * Generate a random LMK and load it into the specified slot.
     * This is a simulator management operation — no state check is enforced.
     *
     * @param slotId  LMK slot identifier (00-99)
     * @param scheme  "VARIANT" or "KEY_BLOCK"
     * @param isDefault whether this should become the default LMK
     * @param algorithm LmkAlgorithm name — determines how keys are encrypted under this LMK
     */
    fun generateRandomLmkForSlot(
        slotId: String,
        scheme: String = "VARIANT",
        isDefault: Boolean = false,
        algorithm: String = LmkAlgorithm.TDES_2KEY.name
    ): HsmCommandResult {
        // Generate 3 random 16-byte components and XOR them
        val components = List(3) { ByteArray(16).also { secureRandom.nextBytes(it) } }
        val lmkKey = components.reduce { acc, c ->
            acc.zip(c) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
        }

        // Create LMK set with derived pairs
        val lmkSet = LmkSet(identifier = slotId, scheme = scheme, algorithm = algorithm)
        for (pairNum in 0 until 40) {
            val derivedLeft = deriveKeyComponent(lmkKey, pairNum * 2)
            val derivedRight = deriveKeyComponent(lmkKey, pairNum * 2 + 1)
            lmkSet.pairs[pairNum] = LmkPair(derivedLeft, derivedRight)
        }

        lmkStorage.addLmk(lmkSet)
        val result = slotManager.allocateLmkSlot(slotId, lmkSet, isDefault = isDefault)
        if (result is HsmCommandResult.Error) {
            lmkStorage.deleteLmk(slotId)
            auditLog.addEntry(AuditEntry(
                entryType = AuditType.KEY_OPERATION,
                command = "GEN-LMK",
                lmkId = slotId,
                result = "FAILED",
                details = "Slot allocation failed: ${result.message}"
            ))
            return result
        }

        if (currentState == HsmState.OFFLINE) {
            currentState = HsmState.ONLINE
        }

        val algo = LmkAlgorithm.fromName(algorithm)
        auditLog.addEntry(AuditEntry(
            entryType = AuditType.KEY_OPERATION,
            command = "GEN-LMK",
            lmkId = slotId,
            result = "SUCCESS",
            details = "Random LMK generated for slot $slotId (scheme=$scheme, algorithm=${algo.display})"
        ))

        return HsmCommandResult.Success(
            response = "LMK generated for slot $slotId\nCheck Value: ${lmkSet.checkValue}",
            data = mapOf(
                "lmkId" to slotId,
                "checkValue" to lmkSet.checkValue,
                "scheme" to scheme,
                "algorithm" to algorithm,
                "pairCount" to lmkSet.pairs.size.toString()
            )
        )
    }

    /**
     * Delete an LMK from a slot and from storage.
     * Simulator management operation — no state check.
     */
    fun deleteLmkFromSlot(slotId: String): HsmCommandResult {
        lmkStorage.deleteLmk(slotId)
        val result = slotManager.deleteLmkSlot(slotId)
        auditLog.addEntry(AuditEntry(
            entryType = AuditType.KEY_OPERATION,
            command = "DEL-LMK",
            lmkId = slotId,
            result = if (result is HsmCommandResult.Success) "SUCCESS" else "FAILED",
            details = "LMK slot $slotId deleted"
        ))
        return result
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
     * Resolve the [CryptoAlgorithm] and key bytes for the given LMK.
     * For AES algorithms the LMK pair key is expanded/trimmed to the required AES key size.
     * For TDES algorithms the combined 16-byte pair key is used as-is (2-key) or the first
     * 24 bytes of the pair key are used (3-key).
     */
    /**
     * Pad data to the algorithm's block size (8 for TDES, 16 for AES).
     */
    private fun padToBlock(data: ByteArray, blockSize: Int): ByteArray {
        if (data.size % blockSize == 0) return data
        return data.copyOf(((data.size + blockSize - 1) / blockSize) * blockSize)
    }

    /**
     * Encrypt data under LMK — algorithm is determined by the LmkSet's algorithm field.
     */
    suspend fun encryptUnderLmk(data: ByteArray, lmkId: String, pairNumber: Int): ByteArray {
        val lmk = lmkStorage.getLmk(lmkId) ?: return data
        val pair = lmk.getPair(pairNumber) ?: return data
        val combined = pair.getCombinedKey()
        val lmkAlgo = lmk.lmkAlgorithm

        val (cryptoAlgo, key) = if (lmkAlgo.isAes) {
            CryptoAlgorithm.AES to if (combined.size >= lmkAlgo.keyBytes) combined.copyOf(lmkAlgo.keyBytes) else (combined + combined).copyOf(lmkAlgo.keyBytes)
        } else {
            CryptoAlgorithm.TDES to if (lmkAlgo == LmkAlgorithm.TDES_3KEY) {
                if (combined.size >= 24) combined.copyOf(24) else (combined + combined).copyOf(24)
            } else combined.copyOf(16)
        }

        return try {
            val padded = padToBlock(data, lmkAlgo.blockSize)
            engine().encryptionEngine.encrypt(
                algorithm = cryptoAlgo,
                encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                    key = key,
                    data = padded,
                    mode = ai.cortex.core.types.CipherMode.ECB
                )
            )
        } catch (e: Exception) {
            data
        }
    }

    /**
     * Decrypt data under LMK — algorithm is determined by the LmkSet's algorithm field.
     */
    suspend fun decryptUnderLmk(data: ByteArray, lmkId: String, pairNumber: Int): ByteArray {
        val lmk = lmkStorage.getLmk(lmkId) ?: return data
        val pair = lmk.getPair(pairNumber) ?: return data
        val combined = pair.getCombinedKey()
        val lmkAlgo = lmk.lmkAlgorithm

        val (cryptoAlgo, key) = if (lmkAlgo.isAes) {
            CryptoAlgorithm.AES to if (combined.size >= lmkAlgo.keyBytes) combined.copyOf(lmkAlgo.keyBytes) else (combined + combined).copyOf(lmkAlgo.keyBytes)
        } else {
            CryptoAlgorithm.TDES to if (lmkAlgo == LmkAlgorithm.TDES_3KEY) {
                if (combined.size >= 24) combined.copyOf(24) else (combined + combined).copyOf(24)
            } else combined.copyOf(16)
        }

        return try {
            val padded = padToBlock(data, lmkAlgo.blockSize)
            engine().encryptionEngine.decrypt(
                algorithm = cryptoAlgo,
                decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                    key = key,
                    data = padded,
                    mode = ai.cortex.core.types.CipherMode.ECB
                )
            )
        } catch (e: Exception) {
            data
        }
    }

    /**
     * Encrypt [data] using the algorithm configured for the given LMK, with the provided
     * [key] bytes (which may already have a variant applied). This is useful for callers
     * that have the raw key bytes but need to respect the LMK's algorithm setting.
     */
    suspend fun encryptWithLmkAlgorithm(data: ByteArray, key: ByteArray, lmkId: String): ByteArray {
        val lmk = lmkStorage.getLmk(lmkId) ?: return data
        val algo = lmk.lmkAlgorithm
        val (cryptoAlgo, effectiveKey) = if (algo.isAes) {
            CryptoAlgorithm.AES to key.copyOf(algo.keyBytes)
        } else {
            CryptoAlgorithm.TDES to key.copyOf(minOf(key.size, algo.keyBytes))
        }
        return try {
            val padded = padToBlock(data, algo.blockSize)
            engine().encryptionEngine.encrypt(
                algorithm = cryptoAlgo,
                encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                    key = effectiveKey, data = padded, mode = ai.cortex.core.types.CipherMode.ECB
                )
            )
        } catch (e: Exception) { data }
    }

    /**
     * Decrypt [data] using the algorithm configured for the given LMK, with the provided
     * [key] bytes (which may already have a variant applied).
     */
    suspend fun decryptWithLmkAlgorithm(data: ByteArray, key: ByteArray, lmkId: String): ByteArray {
        val lmk = lmkStorage.getLmk(lmkId) ?: return data
        val algo = lmk.lmkAlgorithm
        val (cryptoAlgo, effectiveKey) = if (algo.isAes) {
            CryptoAlgorithm.AES to key.copyOf(algo.keyBytes)
        } else {
            CryptoAlgorithm.TDES to key.copyOf(minOf(key.size, algo.keyBytes))
        }
        return try {
            val padded = padToBlock(data, algo.blockSize)
            engine().encryptionEngine.decrypt(
                algorithm = cryptoAlgo,
                decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                    key = effectiveKey, data = padded, mode = ai.cortex.core.types.CipherMode.ECB
                )
            )
        } catch (e: Exception) { data }
    }

    // ====================================================================================================
    // THALES PAYSHIELD KEYBLOCK SUPPORT (LMK SCHEME "S")
    // ====================================================================================================

    /**
     * Build a PayShield-compliant Thales KeyBlock string (Scheme "S").
     *
     * Wire format: S + version(1) + blockLen(4) + keyUsage(2) + algorithm(1) + modeOfUse(1)
     *              + keyVersionNumber(2) + exportability(1) + numOptBlocks(2) + reserved(2)
     *              + encryptedKey(hex) + mac(hex)
     *
     * Version 0 (DES Keyblock LMK):
     *   - KBEK = KBPK XOR 0x45, expanded to triple-TDES (24 bytes)
     *   - KBAK = KBPK XOR 0x4D, expanded to triple-TDES (24 bytes)
     *   - Plaintext: [key_len_bits_2B_BE] + [key] + [zero_pad to 8B blocks]
     *   - Encrypt: TDES-ECB(KBEK, plaintext)
     *   - MAC: TDES-CBC-MAC(KBAK, header + encKeyHex), truncated to 4 bytes (8 hex)
     *
     * Version 1 (AES Keyblock LMK):
     *   - KBEK and KBMK cryptographically derived from LMK via NIST SP 800-108 CMAC-KDF
     *   - Encrypt: AES-CBC + PKCS#7 padding
     *   - IV: bytes 0-15 of header (ASCII)
     *   - MAC: AES-CMAC(KBMK, header + clearKeyPadded) truncated to 8 bytes
     */
    suspend fun buildKeyBlock(
        clearKey: ByteArray,
        lmkPairKey: ByteArray,
        keyUsage: String = "K0",
        modeOfUse: Char = 'N',
        exportability: Char = 'E',
        lmkId: String = "00",
        keyVersionNumber: String = "00",
        useKeyDirectlyAsKbpk: Boolean = false
    ): String {
        val lmk = lmkStorage.getLmk(lmkId)
        val lmkAlgo = lmk?.lmkAlgorithm ?: LmkAlgorithm.TDES_2KEY

        val algo = when (clearKey.size) {
            8 -> 'D'
            16, 24 -> 'T'
            else -> 'A'
        }

        val keyVersionNum = keyVersionNumber.take(2).padStart(2, '0')
        val numOptBlocks = "00"
        val reserved = lmkId.padStart(2, '0').take(2)

        hsmLogsListener.log(buildString {
            appendLine("========== Key Block Build: BEGIN ==========")
            appendLine("Step 1: Input parameters")
            appendLine("  Clear Key:\t\t${bytesToHex(clearKey)} (${clearKey.size * 8} bits)")
            if (useKeyDirectlyAsKbpk) {
                appendLine("  KBPK (direct):\t${bytesToHex(lmkPairKey)} (${lmkPairKey.size * 8} bits)")
            } else {
                appendLine("  LMK Pair Key:\t\t${bytesToHex(lmkPairKey)}")
            }
            appendLine("  LMK Algorithm:\t$lmkAlgo")
            appendLine("  Key Usage:\t\t$keyUsage")
            appendLine("  Algorithm:\t\t$algo")
            appendLine("  Mode of Use:\t\t$modeOfUse")
            appendLine("  Exportability:\t$exportability")
            appendLine("  LMK ID:\t\t$lmkId")
        })

        if (useKeyDirectlyAsKbpk) {
            // ZMK/TMK: use the key directly as KBPK — no derivation needed
            val kbpk = lmkPairKey
            val isAes = kbpk.size == 16 || kbpk.size == 24 || kbpk.size == 32
            hsmLogsListener.log(buildString {
                appendLine("Step 2: Using key directly as KBPK (no derivation)")
                appendLine("  KBPK:\t\t\t${bytesToHex(kbpk)} (${kbpk.size * 8} bits)")
            })
            return if (isAes) {
                // Determine AES algo from key size
                val aesAlgo = when (kbpk.size) {
                    16 -> LmkAlgorithm.AES_128
                    24 -> LmkAlgorithm.AES_192
                    else -> LmkAlgorithm.AES_256
                }
                buildKeyBlockVersion1(
                    clearKey, kbpk, aesAlgo, algo,
                    keyUsage, modeOfUse, keyVersionNum, exportability, numOptBlocks, reserved
                )
            } else {
                buildKeyBlockVersion0(
                    clearKey, kbpk, lmkAlgo, algo,
                    keyUsage, modeOfUse, keyVersionNum, exportability, numOptBlocks, reserved
                )
            }
        }

        // LMK path: derive a dedicated KBPK from the LMK pair key.
        // The LMK is NOT directly the KBPK — Thales derives a dedicated KBPK
        // from the LMK using a key derivation process.
        if (lmkAlgo.isAes) {
            val kbpk = deriveLmkToKbpkV1(lmkPairKey, lmkAlgo)
            hsmLogsListener.log(buildString {
                appendLine("Step 2: KBPK derivation (Version 1 — AES CMAC-KDF)")
                appendLine("  LMK (input):\t\t${bytesToHex(lmkPairKey)}")
                appendLine("  KDF method:\t\tNIST SP 800-108 CMAC-KDF, usage=0x0002 (KBPK)")
                appendLine("  Derived KBPK:\t\t${bytesToHex(kbpk)} (${kbpk.size * 8} bits)")
            })
            return buildKeyBlockVersion1(
                clearKey, kbpk, lmkAlgo, algo,
                keyUsage, modeOfUse, keyVersionNum, exportability, numOptBlocks, reserved
            )
        } else {
            val kbpk = deriveLmkToKbpkV0(lmkPairKey, lmkAlgo)
            hsmLogsListener.log(buildString {
                appendLine("Step 2: KBPK derivation (Version 0 — TDES-ECB KDF)")
                appendLine("  LMK (input):\t\t${bytesToHex(lmkPairKey)}")
                appendLine("  KDF method:\t\tTDES-ECB encrypt of 'KBPK' label + counter blocks")
                appendLine("  Derived KBPK:\t\t${bytesToHex(kbpk)} (${kbpk.size * 8} bits)")
            })
            return buildKeyBlockVersion0(
                clearKey, kbpk, lmkAlgo, algo,
                keyUsage, modeOfUse, keyVersionNum, exportability, numOptBlocks, reserved
            )
        }
    }

    /**
     * Version 0 (DES Keyblock LMK): TDES-ECB encryption, TDES-CBC-MAC authentication.
     *
     * Key derivation:
     *   KBEK = expandToTripleTdes(KBPK XOR 0x45)  — encryption key
     *   KBAK = expandToTripleTdes(KBPK XOR 0x4D)  — authentication key
     *
     * Plaintext: [key_length_bits_2B_BE] + [key_bytes] + [zero_padding to 8B blocks]
     * Encryption: TDES-ECB(KBEK, plaintext)
     * MAC: TDES-CBC-MAC(KBAK, header + encrypted_key_hex), truncated to 4 bytes (8 hex)
     */
    private suspend fun buildKeyBlockVersion0(
        clearKey: ByteArray, lmkPairKey: ByteArray, lmkAlgo: LmkAlgorithm, algo: Char,
        keyUsage: String, modeOfUse: Char, keyVersionNum: String,
        exportability: Char, numOptBlocks: String, reserved: String
    ): String {
        val effectiveKey = lmkPairKey.copyOf(minOf(lmkPairKey.size, lmkAlgo.keyBytes))

        // Derive KBEK (encryption) and KBAK (authentication) from KBPK
        val kbek = deriveVersion0Key(effectiveKey, 0x45)
        val kbak = deriveVersion0Key(effectiveKey, 0x4D)

        hsmLogsListener.log(buildString {
            appendLine("Step 3: Derive KBEK and KBAK from KBPK (Version 0)")
            appendLine("  KBPK (effective):\t${bytesToHex(effectiveKey)} (${effectiveKey.size * 8} bits)")
            appendLine("  KBEK = KBPK XOR 0x45, expanded to triple-TDES (24 bytes)")
            appendLine("  KBEK:\t\t\t${bytesToHex(kbek)}")
            appendLine("  KBAK = KBPK XOR 0x4D, expanded to triple-TDES (24 bytes)")
            appendLine("  KBAK:\t\t\t${bytesToHex(kbak)}")
        })

        // Plaintext: 2-byte key length in bits (BE) + clear key + zero-pad to 8-byte blocks
        val keyLenBits = clearKey.size * 8
        val lenPrefix = byteArrayOf((keyLenBits shr 8).toByte(), (keyLenBits and 0xFF).toByte())
        val plaintext = padToBlock(lenPrefix + clearKey, 8)

        hsmLogsListener.log(buildString {
            appendLine("Step 4: Build plaintext")
            appendLine("  Key length descriptor:\t${bytesToHex(lenPrefix)} ($keyLenBits bits)")
            appendLine("  Clear key:\t\t\t${bytesToHex(clearKey)}")
            appendLine("  Plaintext (len+key+pad):\t${bytesToHex(plaintext)} (${plaintext.size} bytes, padded to 8B blocks)")
        })

        // Build header first to derive IV for CBC encryption
        val encKeyHex0 = bytesToHex(plaintext) // placeholder for block length calc
        val macHexLen = 8  // 4 bytes = 8 hex chars for version 0
        val blockLen = 16 + encKeyHex0.length + macHexLen
        val blockLenStr = blockLen.toString().padStart(4, '0')
        val header = "0$blockLenStr$keyUsage$algo$modeOfUse${keyVersionNum}$exportability$numOptBlocks$reserved"

        // Encrypt: TDES-CBC(KBEK, plaintext), IV = first 8 bytes of header ASCII
        val iv = header.toByteArray(Charsets.US_ASCII).copyOf(8)
        val encryptedKey = try {
            engine().encryptionEngine.encrypt(
                algorithm = CryptoAlgorithm.TDES,
                encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                    data = plaintext, key = kbek,
                    mode = ai.cortex.core.types.CipherMode.CBC, iv = iv
                )
            )
        } catch (e: Exception) { clearKey }

        val encKeyHex = bytesToHex(encryptedKey)

        hsmLogsListener.log(buildString {
            appendLine("Step 5: Encrypt plaintext — TDES-CBC(KBEK, plaintext)")
            appendLine("  Header:\t\t$header")
            appendLine("  IV (header[0..7]):\t${bytesToHex(iv)}")
            appendLine("  Encrypted key:\t$encKeyHex")
        })

        // MAC: TDES-CBC-MAC(KBAK, header_ASCII_bytes || encrypted_key_BINARY_bytes), truncated to 4 bytes
        val macInput = header.toByteArray(Charsets.US_ASCII) + encryptedKey
        val fullMac = computeCbcMac(macInput, kbak, LmkAlgorithm.TDES_2KEY)
        val macHex = bytesToHex(fullMac).take(8)

        val result = "S$header$encKeyHex$macHex"

        hsmLogsListener.log(buildString {
            appendLine("Step 6: Compute MAC — TDES-CBC-MAC(KBAK, header || encryptedKey)")
            appendLine("  MAC input length:\t${macInput.size} bytes")
            appendLine("  Full MAC:\t\t${bytesToHex(fullMac)}")
            appendLine("  Truncated MAC (4B):\t$macHex")
            appendLine("Step 7: Final Key Block")
            appendLine("  Key Block:\t\t$result")
            appendLine("========== Key Block Build: COMPLETE ==========")
        })

        return result
    }

    /**
     * Derive a Version 0 key block key by XORing KBPK with a constant and expanding to triple-TDES (24 bytes).
     * KBEK uses xorByte=0x45, KBAK uses xorByte=0x4D.
     */
    private fun deriveVersion0Key(kbpk: ByteArray, xorByte: Int): ByteArray {
        val xored = ByteArray(kbpk.size) { i -> (kbpk[i].toInt() xor xorByte).toByte() }
        // Expand to 24-byte triple-TDES: K1|K2|K1
        return when {
            xored.size >= 24 -> xored.copyOf(24)
            xored.size == 16 -> xored + xored.copyOf(8)  // K1|K2|K1
            else -> (xored + xored + xored).copyOf(24)
        }
    }

    /**
     * Version 1 (AES Keyblock LMK): AES-CBC encryption, AES-CMAC authentication.
     * KBEK and KBMK are derived from LMK using NIST SP 800-108 Counter Mode KDF with AES-CMAC as PRF.
     */
    private fun buildKeyBlockVersion1(
        clearKey: ByteArray, lmkPairKey: ByteArray, lmkAlgo: LmkAlgorithm, algo: Char,
        keyUsage: String, modeOfUse: Char, keyVersionNum: String,
        exportability: Char, numOptBlocks: String, reserved: String
    ): String {
        // KBPK = LMK pair key expanded to AES key size
        val kbpk = lmkPairKey.copyOf(lmkAlgo.keyBytes)

        // Derive KBEK (encryption key) and KBMK (MAC key) via NIST SP 800-108 CMAC-KDF
        val kbek = deriveKeyBlockKey(kbpk, lmkAlgo, KEY_USAGE_ENCRYPTION)
        val kbmk = deriveKeyBlockKey(kbpk, lmkAlgo, KEY_USAGE_MAC)

        hsmLogsListener.log(buildString {
            appendLine("Step 3: Derive KBEK and KBMK from KBPK (Version 1 — CMAC-KDF)")
            appendLine("  KBPK:\t\t\t${bytesToHex(kbpk)} (${kbpk.size * 8} bits)")
            appendLine("  KBEK = CMAC-KDF(KBPK, usage=0x0000 ENCRYPTION)")
            appendLine("  KBEK:\t\t\t${bytesToHex(kbek)}")
            appendLine("  KBMK = CMAC-KDF(KBPK, usage=0x0001 MAC)")
            appendLine("  KBMK:\t\t\t${bytesToHex(kbmk)}")
        })

        // Build the clear payload: 2-byte key length (bits, BE) + clear key + random padding to 16B boundary
        val keyLenBits = clearKey.size * 8
        val lenPrefix = byteArrayOf((keyLenBits shr 8).toByte(), (keyLenBits and 0xFF).toByte())
        val unpadded = lenPrefix + clearKey
        val padLen = if (unpadded.size % 16 == 0) 0 else 16 - (unpadded.size % 16)
        val randomPad = ByteArray(padLen).also { secureRandom.nextBytes(it) }
        val clearPayload = unpadded + randomPad

        hsmLogsListener.log(buildString {
            appendLine("Step 4: Build plaintext")
            appendLine("  Key length descriptor:\t${bytesToHex(lenPrefix)} ($keyLenBits bits)")
            appendLine("  Clear key:\t\t\t${bytesToHex(clearKey)} (${clearKey.size * 8} bits)")
            appendLine("  Random pad length:\t\t$padLen bytes")
            appendLine("  Plaintext (len+key+pad):\t${bytesToHex(clearPayload)} (${clearPayload.size} bytes)")
        })

        // Build header (need encrypted key length first to compute block length)
        val encKeyHexLen = clearPayload.size * 2
        val macHexLen = 16  // 8 bytes = 16 hex (Thales S-block)
        val blockLen = 16 + encKeyHexLen + macHexLen
        val blockLenStr = blockLen.toString().padStart(4, '0')
        val header = "1$blockLenStr$keyUsage$algo$modeOfUse${keyVersionNum}$exportability$numOptBlocks$reserved"
        val headerBytes = header.toByteArray(Charsets.US_ASCII)

        // Thales S-block V1: Encrypt-then-MAC (same pattern as V0)
        // 1. Encrypt with IV = header[0..15]
        val iv = headerBytes.copyOf(16)
        val encryptedKey = try {
            io.cryptocalc.crypto.engines.encryption.AesCalculatorEngine.encryptCBC(clearPayload, kbek, iv)
        } catch (e: Exception) { clearKey }

        // 2. MAC over header + encrypted key bytes
        val macInput = headerBytes + encryptedKey
        val fullCmac = computeAesCmac(macInput, kbmk)
        val mac = fullCmac.copyOf(8)

        val encKeyHex = bytesToHex(encryptedKey)
        val macHex = bytesToHex(mac)

        val result = "S$header$encKeyHex$macHex"

        hsmLogsListener.log(buildString {
            appendLine("Step 5: Encrypt plaintext — AES-CBC(KBEK, IV=header, clearPayload)")
            appendLine("  Header:\t\t$header")
            appendLine("  IV (header[0..15]):\t${bytesToHex(iv)}")
            appendLine("  Encrypted key:\t$encKeyHex")
            appendLine("Step 6: Compute MAC — AES-CMAC(KBMK, header || encryptedKey) [Encrypt-then-MAC]")
            appendLine("  MAC input:\t\theader(${headerBytes.size}B) + encrypted(${encryptedKey.size}B) = ${macInput.size} bytes")
            appendLine("  Full CMAC (16B):\t${bytesToHex(fullCmac)}")
            appendLine("  Truncated MAC (8B):\t$macHex")
            appendLine("Step 7: Final Key Block")
            appendLine("  Key Block:\t\t$result")
            appendLine("========== Key Block Build: COMPLETE ==========")
        })

        return result
    }

    /**
     * NIST SP 800-108 Counter Mode KDF using AES-CMAC as PRF.
     * Derivation data format (8 bytes):
     *   counter(1) | key_usage(2, BE) | separator(1=0x00) | algorithm(2, BE) | length(2, BE)
     */
    private fun deriveKeyBlockKey(kbpk: ByteArray, lmkAlgo: LmkAlgorithm, keyUsageCode: Int): ByteArray {
        val keyBits = lmkAlgo.keyBytes * 8
        val (algoCode, lengthBits) = when (lmkAlgo) {
            LmkAlgorithm.AES_128 -> 0x0002 to 0x0080
            LmkAlgorithm.AES_192 -> 0x0003 to 0x00C0
            LmkAlgorithm.AES_256 -> 0x0004 to 0x0100
            else -> 0x0000 to 0x0080
        }

        val blocksNeeded = (lmkAlgo.keyBytes + 15) / 16  // AES-CMAC produces 16 bytes per block
        val derivedBytes = ByteArray(blocksNeeded * 16)

        for (counter in 1..blocksNeeded) {
            val derivationData = byteArrayOf(
                counter.toByte(),
                (keyUsageCode shr 8).toByte(), (keyUsageCode and 0xFF).toByte(),
                0x00,
                (algoCode shr 8).toByte(), (algoCode and 0xFF).toByte(),
                (lengthBits shr 8).toByte(), (lengthBits and 0xFF).toByte()
            )
            val block = computeAesCmac(derivationData, kbpk)
            block.copyInto(derivedBytes, (counter - 1) * 16)
        }

        return derivedBytes.copyOf(lmkAlgo.keyBytes)
    }

    companion object {
        private const val KEY_USAGE_ENCRYPTION = 0x0000
        private const val KEY_USAGE_MAC = 0x0001
        private const val KEY_USAGE_KBPK = 0x0002
    }

    /**
     * Derive KBPK from LMK pair key for Version 0 (TDES) key blocks.
     *
     * The LMK is NOT directly the KBPK. Thales derives a dedicated KBPK from the LMK
     * using TDES-ECB encryption of counter-based derivation blocks under the LMK.
     * The LMK never leaves the HSM and is never embedded in the key block.
     */
    private fun deriveLmkToKbpkV0(lmkKey: ByteArray, lmkAlgo: LmkAlgorithm): ByteArray {
        val targetSize = minOf(lmkKey.size, lmkAlgo.keyBytes)
        // Expand LMK to triple-TDES (24 bytes) for use as TDES key
        val expandedLmk = when {
            lmkKey.size >= 24 -> lmkKey.copyOf(24)
            lmkKey.size == 16 -> lmkKey + lmkKey.copyOf(8)
            else -> (lmkKey + lmkKey + lmkKey).copyOf(24)
        }

        val blocksNeeded = (targetSize + 7) / 8
        val derived = ByteArray(blocksNeeded * 8)

        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(expandedLmk, "DESede"))

        for (counter in 1..blocksNeeded) {
            // Derivation data: "KBPK" label (4 bytes) + 0x00 separator + 0x00 reserved + 0x00 reserved + counter
            val derivationData = byteArrayOf(
                0x4B, 0x42, 0x50, 0x4B,  // "KBPK"
                0x00,                      // separator
                0x00, 0x00,                // reserved
                counter.toByte()           // counter
            )
            val block = cipher.doFinal(derivationData)
            block.copyInto(derived, (counter - 1) * 8)
        }

        return derived.copyOf(targetSize)
    }

    /**
     * Derive KBPK from LMK pair key for Version 1 (AES) key blocks.
     *
     * Uses NIST SP 800-108 Counter Mode KDF with AES-CMAC as PRF,
     * with a dedicated KEY_USAGE_KBPK purpose code to produce a KBPK
     * that is cryptographically separated from the LMK.
     */
    private fun deriveLmkToKbpkV1(lmkKey: ByteArray, lmkAlgo: LmkAlgorithm): ByteArray {
        val lmk = lmkKey.copyOf(lmkAlgo.keyBytes)
        return deriveKeyBlockKey(lmk, lmkAlgo, KEY_USAGE_KBPK)
    }

    /** Public accessor for KBPK derivation (Version 0) — used by command classes for logging. */
    fun deriveLmkToKbpkV0Public(lmkKey: ByteArray, lmkAlgo: LmkAlgorithm): ByteArray = deriveLmkToKbpkV0(lmkKey, lmkAlgo)

    /** Public accessor for KBPK derivation (Version 1) — used by command classes for logging. */
    fun deriveLmkToKbpkV1Public(lmkKey: ByteArray, lmkAlgo: LmkAlgorithm): ByteArray = deriveLmkToKbpkV1(lmkKey, lmkAlgo)

    /**
     * Decrypt a Thales KeyBlock (Scheme "S") and return the clear key bytes.
     *
     * Version 0 (TDES):
     *   - MAC = 4 bytes (8 hex), KBEK = KBPK XOR 0x45 (triple-TDES), TDES-ECB
     *   - Plaintext = [key_len_bits_2B] + [key] + [zero_pad]
     *
     * Version 1 (AES):
     *   - MAC = 8 bytes (16 hex), KBEK derived via CMAC-KDF, AES-CBC + PKCS#7
     */
    suspend fun decryptKeyBlock(keyBlock: String, lmkPairKey: ByteArray, lmkId: String): ByteArray {
        require(keyBlock.isNotEmpty() && keyBlock[0] == 'S') { "Not an S-block key" }

        val version = keyBlock[1].digitToInt()
        val blockLen = keyBlock.substring(2, 6).toInt()
        val baseHeaderLen = 17  // 'S' + 16 chars header
        val totalBlockChars = 1 + blockLen  // 'S' prefix + block content
        val baseHeader = keyBlock.substring(1, baseHeaderLen)  // header without 'S' prefix

        // Parse header fields for logging
        val keyUsage = baseHeader.substring(5, 7)
        val algorithm = baseHeader.substring(7, 8)
        val modeOfUse = baseHeader.substring(8, 9)
        val keyVersionNum = baseHeader.substring(9, 11)
        val exportability = baseHeader.substring(11, 12)
        val numOptBlocks = baseHeader.substring(12, 14)
        val lmkIdField = baseHeader.substring(14, 16)

        // Account for optional blocks
        val numOpt = numOptBlocks.toIntOrNull() ?: 0
        var headerLen = baseHeaderLen
        var optPos = baseHeaderLen
        for (i in 0 until numOpt) {
            if (optPos + 4 > keyBlock.length) break
            val optBlockDataLen = keyBlock.substring(optPos + 2, optPos + 4).toIntOrNull() ?: 0
            optPos += 4 + optBlockDataLen
        }
        headerLen = optPos
        val header = keyBlock.substring(1, headerLen)

        val lmk = lmkStorage.getLmk(lmkId)
        val lmkAlgo = lmk?.lmkAlgorithm ?: LmkAlgorithm.TDES_2KEY

        hsmLogsListener.log(buildString {
            appendLine("========== Key Block Decode: BEGIN ==========")
            appendLine("Step 1: Parse key block")
            appendLine("  Key Block:\t\t$keyBlock")
            appendLine("  Version:\t\t$version (${if (version == 1) "AES" else "3DES"} KBPK)")
            appendLine("  Block Length:\t\t$blockLen")
            appendLine("  Header:\t\t$header")
            appendLine("  Key Usage:\t\t$keyUsage")
            appendLine("  Algorithm:\t\t$algorithm")
            appendLine("  Mode of Use:\t\t$modeOfUse")
            appendLine("  Key Version No.:\t$keyVersionNum")
            appendLine("  Exportability:\t$exportability")
            appendLine("  Num. of Opt. blocks:\t$numOptBlocks")
            appendLine("  LMK ID:\t\t$lmkIdField")
            appendLine("  LMK Pair Key:\t\t${bytesToHex(lmkPairKey)}")
            appendLine("  LMK Algorithm:\t$lmkAlgo")
        })

        return if (version == 1 && lmkAlgo.isAes) {
            // Version 1: MAC = 8 bytes (16 hex), AES-CBC + PKCS#7 (Thales S-block)
            val macHexLen = 16
            val encKeyHex = keyBlock.substring(headerLen, totalBlockChars - macHexLen)
            val macHex = keyBlock.substring(totalBlockChars - macHexLen, totalBlockChars)
            val encryptedKey = hexToBytes(encKeyHex)

            hsmLogsListener.log(buildString {
                appendLine("Step 2: Extract encrypted payload (Version 1)")
                appendLine("  Encrypted key hex:\t$encKeyHex")
                appendLine("  MAC hex:\t\t$macHex")
            })

            // Derive KBPK from LMK — the LMK is NOT directly the KBPK
            val kbpk = deriveLmkToKbpkV1(lmkPairKey, lmkAlgo)
            val kbek = deriveKeyBlockKey(kbpk, lmkAlgo, KEY_USAGE_ENCRYPTION)
            val kbmk = deriveKeyBlockKey(kbpk, lmkAlgo, KEY_USAGE_MAC)

            hsmLogsListener.log(buildString {
                appendLine("Step 3: Derive KBPK from LMK (AES CMAC-KDF)")
                appendLine("  LMK (input):\t\t${bytesToHex(lmkPairKey)}")
                appendLine("  KDF method:\t\tNIST SP 800-108 CMAC-KDF, usage=0x0002 (KBPK)")
                appendLine("  Derived KBPK:\t\t${bytesToHex(kbpk)} (${kbpk.size * 8} bits)")
                appendLine("Step 4: Derive KBEK and KBMK from KBPK (CMAC-KDF)")
                appendLine("  KBEK = CMAC-KDF(KBPK, usage=0x0000 ENCRYPTION)")
                appendLine("  KBEK:\t\t\t${bytesToHex(kbek)}")
                appendLine("  KBMK = CMAC-KDF(KBPK, usage=0x0001 MAC)")
                appendLine("  KBMK:\t\t\t${bytesToHex(kbmk)}")
            })

            // Thales S-block V1: IV = header[0..15]
            val iv = header.toByteArray(Charsets.US_ASCII).copyOf(16)
            val decrypted = io.cryptocalc.crypto.engines.encryption.AesCalculatorEngine.decryptCBC(encryptedKey, kbek, iv)
            // Extract key using 2-byte length prefix (padding is random, not PKCS#7)
            val keyLenBits = ((decrypted[0].toInt() and 0xFF) shl 8) or (decrypted[1].toInt() and 0xFF)
            val keyLenBytes = keyLenBits / 8
            require(keyLenBytes in 1..(decrypted.size - 2)) {
                "Key block decryption produced invalid key length ($keyLenBits bits). Check KBPK."
            }
            val clearKey = decrypted.copyOfRange(2, 2 + keyLenBytes)
            val kcv = calculateKeyCheckValue(clearKey)

            hsmLogsListener.log(buildString {
                appendLine("Step 5: Decrypt — AES-CBC(KBEK, IV=header, encryptedKey)")
                appendLine("  IV (header[0..15]):\t${bytesToHex(iv)}")
                appendLine("  Decrypted (raw):\t${bytesToHex(decrypted)}")
                appendLine("Step 6: Extract key using length prefix")
                appendLine("  Key length descriptor:\t${bytesToHex(decrypted.copyOf(2))} ($keyLenBits bits)")
                appendLine("  Clear Key:\t\t\t${bytesToHex(clearKey)} ($keyLenBits bits / $keyLenBytes bytes)")
                appendLine("  KCV:\t\t\t\t$kcv")
                appendLine("========== Key Block Decode: COMPLETE ==========")
            })

            clearKey
        } else {
            // Version 0: MAC = 4 bytes (8 hex), TDES-ECB
            val macHexLen = 8
            val encKeyHex = keyBlock.substring(headerLen, totalBlockChars - macHexLen)
            val macHex = keyBlock.substring(totalBlockChars - macHexLen, totalBlockChars)
            val encryptedKey = hexToBytes(encKeyHex)

            hsmLogsListener.log(buildString {
                appendLine("Step 2: Extract encrypted payload (Version 0)")
                appendLine("  Encrypted key hex:\t$encKeyHex")
                appendLine("  MAC hex:\t\t$macHex")
            })

            // Derive KBPK from LMK — the LMK is NOT directly the KBPK
            val kbpk = deriveLmkToKbpkV0(lmkPairKey, lmkAlgo)
            val kbek = deriveVersion0Key(kbpk, 0x45)
            val kbak = deriveVersion0Key(kbpk, 0x4D)

            hsmLogsListener.log(buildString {
                appendLine("Step 3: Derive KBPK from LMK (TDES-ECB KDF)")
                appendLine("  LMK (input):\t\t${bytesToHex(lmkPairKey)}")
                appendLine("  KDF method:\t\tTDES-ECB encrypt of 'KBPK' label + counter blocks")
                appendLine("  Derived KBPK:\t\t${bytesToHex(kbpk)} (${kbpk.size * 8} bits)")
                appendLine("Step 4: Derive KBEK and KBAK from KBPK")
                appendLine("  KBEK = KBPK XOR 0x45, expanded to triple-TDES (24 bytes)")
                appendLine("  KBEK:\t\t\t${bytesToHex(kbek)}")
                appendLine("  KBAK = KBPK XOR 0x4D, expanded to triple-TDES (24 bytes)")
                appendLine("  KBAK:\t\t\t${bytesToHex(kbak)}")
            })

            try {
                // V0 uses TDES-CBC with IV = first 8 bytes of header ASCII
                val iv = header.toByteArray(Charsets.US_ASCII).copyOf(8)
                val decrypted = engine().encryptionEngine.decrypt(
                    algorithm = CryptoAlgorithm.TDES,
                    decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                        key = kbek, data = encryptedKey,
                        mode = ai.cortex.core.types.CipherMode.CBC, iv = iv
                    )
                )
                // Plaintext = [key_len_bits_2B] + [key] + [zero_pad]
                val keyLenBits = ((decrypted[0].toInt() and 0xFF) shl 8) or (decrypted[1].toInt() and 0xFF)
                val keyLenBytes = keyLenBits / 8
                require(keyLenBytes in 1..(decrypted.size - 2)) {
                    "Key block decryption produced invalid key length ($keyLenBits bits). Check KBPK."
                }
                val clearKey = decrypted.copyOfRange(2, 2 + keyLenBytes)
                val kcv = calculateKeyCheckValue(clearKey)

                hsmLogsListener.log(buildString {
                    appendLine("Step 5: Decrypt — TDES-CBC(KBEK, encryptedKey)")
                    appendLine("  IV (header[0..7]):\t${bytesToHex(iv)}")
                    appendLine("  Decrypted (raw):\t${bytesToHex(decrypted)}")
                    appendLine("Step 6: Extract clear key from plaintext")
                    appendLine("  Key length descriptor:\t${bytesToHex(decrypted.copyOf(2))} ($keyLenBits bits)")
                    appendLine("  Clear Key:\t\t\t${bytesToHex(clearKey)} ($keyLenBits bits / $keyLenBytes bytes)")
                    appendLine("  Padding bytes:\t\t${decrypted.size - 2 - keyLenBytes}")
                    appendLine("  KCV:\t\t\t\t$kcv")
                    appendLine("========== Key Block Decode: COMPLETE ==========")
                })

                clearKey
            } catch (e: Exception) { encryptedKey }
        }
    }

    private fun removePkcs7Padding(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val padLen = data.last().toInt() and 0xFF
        if (padLen < 1 || padLen > data.size || padLen > 16) return data
        for (i in data.size - padLen until data.size) {
            if ((data[i].toInt() and 0xFF) != padLen) return data
        }
        return data.copyOf(data.size - padLen)
    }

    private fun applyPkcs7Padding(data: ByteArray, blockSize: Int): ByteArray {
        val padLen = blockSize - (data.size % blockSize)
        val padded = ByteArray(data.size + padLen)
        data.copyInto(padded)
        for (i in data.size until padded.size) {
            padded[i] = padLen.toByte()
        }
        return padded
    }

    /**
     * Build a KeyBlock for a given key type, looking up usage/mode/exportability automatically.
     * The LMK pair for [pairNumber] is used to encrypt.
     */
    suspend fun buildKeyBlockForKeyType(
        clearKey: ByteArray,
        lmkId: String,
        pairNumber: Int,
        keyTypeCode: String
    ): String {
        val lmk = lmkStorage.getLmk(lmkId) ?: return bytesToHex(clearKey)
        val pair = lmk.getPair(pairNumber) ?: return bytesToHex(clearKey)
        val (usage, mode, export) = keyTypeToBlockAttributes(keyTypeCode)
        return buildKeyBlock(clearKey, pair.getCombinedKey(), usage, mode, export, lmkId)
    }

    /** Map key type code → (keyUsage, modeOfUse, exportability) for KeyBlock header. */
    fun keyTypeToBlockAttributes(keyTypeCode: String): Triple<String, Char, Char> = when (keyTypeCode.uppercase()) {
        "000"       -> Triple("K0", 'N', 'E')   // ZMK
        "001"       -> Triple("P0", 'N', 'E')   // ZPK
        "002"       -> Triple("V0", 'V', 'N')   // PVK
        "003"       -> Triple("M0", 'G', 'N')   // TAK
        "006"       -> Triple("K0", 'N', 'N')   // Watchword
        "008"       -> Triple("M0", 'G', 'N')   // ZAK
        "009"       -> Triple("B0", 'X', 'N')   // BDK
        "109"       -> Triple("E0", 'N', 'N')   // MK-AC
        "209"       -> Triple("E1", 'N', 'N')   // MK-SMI
        "309"       -> Triple("E2", 'N', 'N')   // MK-SMC
        "409"       -> Triple("E2", 'V', 'N')   // MK-DAC
        "509"       -> Triple("E2", 'N', 'N')   // MK-DN
        "609"       -> Triple("B0", 'X', 'N')   // BDK-2
        "709"       -> Triple("C0", 'G', 'N')   // dCVV
        "809"       -> Triple("B0", 'X', 'N')   // BDK-3
        "909"       -> Triple("B0", 'X', 'N')   // BDK-4
        "00A"       -> Triple("D0", 'B', 'S')   // ZEK
        "00B"       -> Triple("D0", 'B', 'S')   // DEK
        "402"       -> Triple("C0", 'G', 'N')   // CVK
        "70D"       -> Triple("P0", 'N', 'E')   // TPK-PCI
        "80D"       -> Triple("K0", 'N', 'E')   // TMK-PCI
        "90D"       -> Triple("K0", 'N', 'E')   // TKR
        "302"       -> Triple("B1", 'X', 'N')   // IKEY
        "FFF"       -> Triple("K0", 'N', 'E')   // Generic / KeyBlock only
        else        -> Triple("K0", 'N', 'E')
    }

    /**
     * CBC-MAC over [data] using [key].
     * For TDES: 8-byte blocks, returns 8 bytes.
     * For AES: 16-byte blocks, returns 16 bytes.
     */
    private suspend fun computeCbcMac(data: ByteArray, key: ByteArray, lmkAlgo: LmkAlgorithm): ByteArray {
        val blockSize = lmkAlgo.blockSize
        val paddedLen = ((data.size + blockSize - 1) / blockSize) * blockSize
        val padded = data.copyOf(paddedLen)
        var mac = ByteArray(blockSize)
        for (i in 0 until padded.size step blockSize) {
            val block = padded.sliceArray(i until i + blockSize)
            val xored = ByteArray(blockSize) { j -> (mac[j].toInt() xor block[j].toInt()).toByte() }
            val params = SymmetricEncryptionEngineParameters(
                data = xored, key = key, mode = ai.cortex.core.types.CipherMode.ECB
            )
            mac = try {
                if (lmkAlgo.isAes) {
                    engine().encryptionEngine.encrypt(algorithm = CryptoAlgorithm.AES, encryptionEngineParameters = params)
                } else {
                    engine().encryptionEngine.encrypt(algorithm = CryptoAlgorithm.TDES, encryptionEngineParameters = params)
                }
            } catch (e: Exception) { xored }
        }
        return mac
    }

    /**
     * Compute AES-CMAC (RFC 4493) over [data] using [key].
     * Returns the full 16-byte CMAC. Caller can truncate as needed.
     */
    private fun computeAesCmac(data: ByteArray, key: ByteArray): ByteArray {
        return io.cryptocalc.crypto.engines.encryption.AesCalculatorEngine.computeCmac(data, key)
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
        return authList.any { it.activity == activity && it.expiresAt > now }
    }

    /**
     * Grant authorization for one or more activities.
     * In a real PayShield this requires custodian smart cards physically inserted.
     * The simulator grants it programmatically from the Secure Commands GUI.
     *
     * @param lmkId      Target LMK slot
     * @param activities Activities to authorize (defaults to all console activities)
     * @param durationMs How long the authorization lasts (default 12 hours)
     * @param officers   Simulated officer identifiers (for audit purposes)
     */
    fun authorizeActivities(
        lmkId: String,
        activities: List<AuthActivity> = AuthActivity.values().toList(),
        durationMs: Long = 12L * 60 * 60 * 1000,
        officers: List<String> = listOf("SIM-OFFICER-1")
    ) {
        val list = authorizations.getOrPut(lmkId) { mutableListOf() }
        val expiresAt = System.currentTimeMillis() + durationMs
        activities.forEach { activity ->
            list.removeAll { it.activity == activity }
            list.add(
                AuthorizationRecord(
                    lmkId = lmkId,
                    activity = activity,
                    expiresAt = expiresAt,
                    authorizedBy = officers
                )
            )
        }
    }

    /**
     * Revoke authorization for one or more activities immediately.
     */
    fun revokeAuthorizations(
        lmkId: String,
        activities: List<AuthActivity> = AuthActivity.values().toList()
    ) {
        val list = authorizations[lmkId] ?: return
        activities.forEach { activity -> list.removeAll { it.activity == activity } }
    }

    /**
     * Returns the earliest expiry epoch-ms across all [activities], or null if none are active.
     */
    fun getAuthorizationExpiry(lmkId: String, activities: List<AuthActivity>): Long? {
        val now = System.currentTimeMillis()
        return authorizations[lmkId]
            ?.filter { it.activity in activities && it.expiresAt > now }
            ?.minOfOrNull { it.expiresAt }
    }

    override fun getSlotManager(): HsmSlotManager {
        return slotManager
    }
}