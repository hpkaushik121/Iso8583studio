package `in`.aicortex.iso8583studio.hsm.payshield10k

import `in`.aicortex.iso8583studio.hsm.payshield10k.data.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Advanced HSM features including terminal management and acquirer support
 */
class PayShield10KAdvancedFeatures(
    private val simulator: PayShield10KFeatures,
    private val commandProcessor: PayShield10KCommandProcessor
) {

    val terminalProfiles = mutableMapOf<String, TerminalKeyProfile>()
    val acquirerProfiles = mutableMapOf<String, AcquirerProfile>()

    // ====================================================================================================
    // ACQUIRER MANAGEMENT
    // ====================================================================================================

    /**
     * Register a new acquirer with the HSM
     */
    fun registerAcquirer(
        acquirerId: String,
        acquirerName: String,
        zmk: ByteArray,
        cvk: ByteArray? = null,
        pvk: ByteArray? = null,
        pinBlockFormat: PinBlockFormat = PinBlockFormat.ISO_FORMAT_0
    ): HsmCommandResult {
        if (acquirerProfiles.containsKey(acquirerId)) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Acquirer $acquirerId already exists"
            )
        }

        val profile = AcquirerProfile(
            acquirerId = acquirerId,
            acquirerName = acquirerName,
            zmk = zmk,
            cvk = cvk,
            pvk = pvk,
            pinBlockFormat = pinBlockFormat
        )

        acquirerProfiles[acquirerId] = profile

        simulator.auditLog.addEntry(AuditEntry(
            entryType = AuditType.CONFIGURATION_CHANGE,
            command = "REGISTER_ACQUIRER",
            result = "SUCCESS",
            details = "Acquirer $acquirerName registered"
        ))

        return HsmCommandResult.Success(
            response = "Acquirer $acquirerName registered successfully",
            data = mapOf(
                "acquirerId" to acquirerId,
                "acquirerName" to acquirerName
            )
        )
    }

    /**
     * Add BDK to acquirer for DUKPT operations
     */
    fun addBdkToAcquirer(
        acquirerId: String,
        bdkId: String,
        bdk: ByteArray
    ): HsmCommandResult {
        val acquirer = acquirerProfiles[acquirerId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Acquirer not found"
            )

        acquirer.bdkRegistry[bdkId] = bdk

        return HsmCommandResult.Success(
            response = "BDK $bdkId added to acquirer $acquirerId",
            data = mapOf(
                "bdkId" to bdkId,
                "kcv" to simulator.calculateKeyCheckValue(bdk)
            )
        )
    }

    // ====================================================================================================
    // TERMINAL ONBOARDING
    // ====================================================================================================

    /**
     * Onboard a new terminal with complete key hierarchy
     */
    fun onboardTerminal(
        terminalId: String,
        acquirerId: String,
        keyInjectionMethod: KeyInjectionMethod,
        enableDukpt: Boolean = false,
        lmkId: String = "00"
    ): HsmCommandResult {
        val acquirer = acquirerProfiles[acquirerId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Acquirer not found"
            )

        if (terminalProfiles.containsKey(terminalId)) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Terminal already onboarded"
            )
        }

        // Generate or inject TMK based on method
        val tmk = when (keyInjectionMethod) {
            is KeyInjectionMethod.MasterKeyInjection -> {
                keyInjectionMethod.masterKey
            }
            is KeyInjectionMethod.ComponentBased -> {
                deriveFromComponents(keyInjectionMethod.components)
            }
            is KeyInjectionMethod.RemoteKeyLoading -> {
                decryptWithZmk(keyInjectionMethod.encryptedTmk, acquirer.zmk)
            }
        }

        // Derive working keys from TMK
        val tpk = deriveKey(tmk, KeyDerivationPurpose.PIN_ENCRYPTION)
        val tak = deriveKey(tmk, KeyDerivationPurpose.AUTHENTICATION)
        val tdk = deriveKey(tmk, KeyDerivationPurpose.DATA_ENCRYPTION)

        // Setup DUKPT if requested
        val dukptProfile = if (enableDukpt) {
            setupDukptForTerminal(terminalId, acquirer)
        } else null

        // Create terminal profile
        val profile = TerminalKeyProfile(
            terminalId = terminalId,
            acquirerId = acquirerId,
            tmk = tmk,
            zmk = acquirer.zmk,
            tpk = tpk,
            tak = tak,
            tdk = tdk,
            dukptProfile = dukptProfile,
            lmkId = lmkId
        )

        terminalProfiles[terminalId] = profile
        acquirer.terminals[terminalId] = profile

        simulator.auditLog.addEntry(AuditEntry(
            entryType = AuditType.KEY_OPERATION,
            command = "ONBOARD_TERMINAL",
            lmkId = lmkId,
            result = "SUCCESS",
            details = "Terminal $terminalId onboarded for acquirer $acquirerId"
        ))

        return HsmCommandResult.Success(
            response = """
                Terminal $terminalId onboarded successfully
                Acquirer: $acquirerId
                DUKPT Enabled: $enableDukpt
                ${if (dukptProfile != null) "KSN: ${dukptProfile.keySerialNumber}" else ""}
            """.trimIndent(),
            data = mapOf(
                "terminalId" to terminalId,
                "acquirerId" to acquirerId,
                "tpkKcv" to simulator.calculateKeyCheckValue(tpk),
                "takKcv" to simulator.calculateKeyCheckValue(tak),
                "tdkKcv" to simulator.calculateKeyCheckValue(tdk),
                "dukptEnabled" to enableDukpt.toString(),
                "ksn" to (dukptProfile?.keySerialNumber ?: "")
            )
        )
    }

    /**
     * Setup DUKPT for terminal with acquirer's BDK
     */
    private fun setupDukptForTerminal(
        terminalId: String,
        acquirer: AcquirerProfile
    ): DukptProfile {
        // Get or create default BDK
        val bdk = acquirer.bdkRegistry.getOrPut("DEFAULT") {
            ByteArray(16).also { simulator.secureRandom.nextBytes(it) }
        }

        // Generate KSN
        val ksn = commandProcessor.generateKsn(terminalId)

        // Derive Initial Key
        val ik = commandProcessor.deriveInitialKey(bdk, ksn)

        return DukptProfile(
            initialKey = ik,
            keySerialNumber = ksn,
            currentCounter = 0,
            bdk = bdk,
            scheme = DukptScheme.ANSI_X9_24_3DES
        )
    }

    /**
     * Rotate terminal keys
     */
    fun rotateTerminalKeys(
        terminalId: String,
        rotationMethod: KeyRotationMethod = KeyRotationMethod.AUTOMATIC
    ): HsmCommandResult {
        val terminal = terminalProfiles[terminalId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Terminal not found"
            )

        val acquirer = acquirerProfiles[terminal.acquirerId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Acquirer not found"
            )

        // Generate new TMK
        val newTmk = ByteArray(16).also { simulator.secureRandom.nextBytes(it) }

        // Derive new working keys
        val newTpk = deriveKey(newTmk, KeyDerivationPurpose.PIN_ENCRYPTION)
        val newTak = deriveKey(newTmk, KeyDerivationPurpose.AUTHENTICATION)
        val newTdk = deriveKey(newTmk, KeyDerivationPurpose.DATA_ENCRYPTION)

        // Create new profile with incremented version
        val newProfile = terminal.copy(
            tmk = newTmk,
            tpk = newTpk,
            tak = newTak,
            tdk = newTdk,
            keyVersion = terminal.keyVersion + 1,
            onboardedAt = System.currentTimeMillis()
        )

        terminalProfiles[terminalId] = newProfile
        acquirer.terminals[terminalId] = newProfile

        simulator.auditLog.addEntry(AuditEntry(
            entryType = AuditType.KEY_OPERATION,
            command = "ROTATE_KEYS",
            result = "SUCCESS",
            details = "Keys rotated for terminal $terminalId to version ${newProfile.keyVersion}"
        ))

        return HsmCommandResult.Success(
            response = "Terminal keys rotated to version ${newProfile.keyVersion}",
            data = mapOf(
                "terminalId" to terminalId,
                "keyVersion" to newProfile.keyVersion.toString(),
                "tpkKcv" to simulator.calculateKeyCheckValue(newTpk)
            )
        )
    }

    /**
     * Decommission terminal
     */
    fun decommissionTerminal(terminalId: String): HsmCommandResult {
        val terminal = terminalProfiles.remove(terminalId)
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Terminal not found"
            )

        // Remove from acquirer
        acquirerProfiles[terminal.acquirerId]?.terminals?.remove(terminalId)

        simulator.auditLog.addEntry(AuditEntry(
            entryType = AuditType.KEY_OPERATION,
            command = "DECOMMISSION_TERMINAL",
            result = "SUCCESS",
            details = "Terminal $terminalId decommissioned"
        ))

        return HsmCommandResult.Success("Terminal $terminalId decommissioned")
    }

    // ====================================================================================================
    // TRANSACTION PROCESSING
    // ====================================================================================================

    /**
     * Process incoming transaction with terminal-specific decryption
     */
    suspend fun processIncomingTransaction(
        terminalId: String,
        encryptedPinBlock: ByteArray,
        accountNumber: String,
        useDukpt: Boolean = false,
        ksn: String? = null
    ): HsmCommandResult {
        val terminal = terminalProfiles[terminalId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Terminal not registered"
            )

        val acquirer = acquirerProfiles[terminal.acquirerId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Acquirer not found"
            )

        return try {
            if (useDukpt && terminal.dukptProfile != null) {
                // DUKPT processing
                if (ksn == null) {
                    return HsmCommandResult.Error(
                        HsmErrorCodes.INVALID_INPUT_DATA,
                        "KSN required for DUKPT"
                    )
                }

                // Derive session key from KSN
                val sessionKey = commandProcessor.deriveDukptSessionKey(
                    terminal.dukptProfile,
                    DukptKeyUsage.PIN_ENCRYPTION
                )

                // Decrypt PIN
                val decryptedPin = commandProcessor.executeVerifyTerminalPin(
                    encryptedPinBlock,
                    accountNumber,
                    sessionKey,
                    "", // We don't have expected PIN here
                    acquirer.pinBlockFormat
                )

                // Increment DUKPT counter
                terminal.dukptProfile.currentCounter++

                HsmCommandResult.Success(
                    response = "Transaction processed using DUKPT",
                    data = mapOf(
                        "terminalId" to terminalId,
                        "ksn" to ksn,
                        "counter" to terminal.dukptProfile.currentCounter.toString()
                    )
                )
            } else {
                // Standard TPK processing
                val tpk = terminal.tpk
                    ?: return HsmCommandResult.Error(
                        HsmErrorCodes.INVALID_INPUT_DATA,
                        "TPK not available"
                    )

                HsmCommandResult.Success(
                    response = "Transaction processed using TPK",
                    data = mapOf(
                        "terminalId" to terminalId,
                        "keyVersion" to terminal.keyVersion.toString()
                    )
                )
            }
        } catch (e: Exception) {
            HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Transaction processing failed: ${e.message}"
            )
        }
    }

    /**
     * Translate encryption from one acquirer to another
     */
    suspend fun translateToAcquirer(
        lmkId: String,
        sourceTerminalId: String,
        destinationAcquirerId: String,
        encryptedPinBlock: ByteArray,
        accountNumber: String
    ): HsmCommandResult {
        val sourceTerminal = terminalProfiles[sourceTerminalId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Source terminal not found"
            )

        val sourceAcquirer = acquirerProfiles[sourceTerminal.acquirerId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Source acquirer not found"
            )

        val destAcquirer = acquirerProfiles[destinationAcquirerId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Destination acquirer not found"
            )

        return try {
            // Decrypt with source terminal's TPK
            val sourceTpk = sourceTerminal.tpk
                ?: return HsmCommandResult.Error(
                    HsmErrorCodes.INVALID_INPUT_DATA,
                    "Source TPK not available"
                )

            // Translate from TPK to destination ZPK
            commandProcessor.executeTranslatePinTpkToZpk(
                lmkId = lmkId,
                encryptedPinBlock,
                sourceTpk,
                destAcquirer.zmk,
                accountNumber,
                sourceAcquirer.pinBlockFormat,
                destAcquirer.pinBlockFormat
            )
        } catch (e: Exception) {
            HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Acquirer translation failed: ${e.message}"
            )
        }
    }

    // ====================================================================================================
    // KEY DERIVATION & ENCRYPTION
    // ====================================================================================================

    /**
     * Derive key from master key for specific purpose
     */
    private fun deriveKey(masterKey: ByteArray, purpose: KeyDerivationPurpose): ByteArray {
        val derivationConstant = when (purpose) {
            KeyDerivationPurpose.PIN_ENCRYPTION -> byteArrayOf(
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
            )
            KeyDerivationPurpose.AUTHENTICATION -> byteArrayOf(
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02
            )
            KeyDerivationPurpose.DATA_ENCRYPTION -> byteArrayOf(
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03
            )
        }

        return try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            val keySpec = SecretKeySpec(masterKey.copyOf(16), "DESede")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            cipher.doFinal(derivationConstant.copyOf(16))
        } catch (e: Exception) {
            ByteArray(16).also { simulator.secureRandom.nextBytes(it) }
        }
    }

    /**
     * Derive key from multiple components using XOR
     */
    private fun deriveFromComponents(components: List<ByteArray>): ByteArray {
        return components.reduce { acc, component ->
            acc.zip(component) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
        }
    }

    /**
     * Decrypt encrypted key using ZMK
     */
    private fun decryptWithZmk(encryptedKey: ByteArray, zmk: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            val keySpec = SecretKeySpec(zmk.copyOf(16), "DESede")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            cipher.doFinal(encryptedKey)
        } catch (e: Exception) {
            encryptedKey
        }
    }

    // ====================================================================================================
    // REPORTING & MONITORING
    // ====================================================================================================

    /**
     * Get terminal status report
     */
    fun getTerminalStatus(terminalId: String): HsmCommandResult {
        val terminal = terminalProfiles[terminalId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Terminal not found"
            )

        val report = buildString {
            appendLine("Terminal Status Report")
            appendLine("=====================")
            appendLine("Terminal ID: ${terminal.terminalId}")
            appendLine("Acquirer ID: ${terminal.acquirerId}")
            appendLine("LMK ID: ${terminal.lmkId}")
            appendLine("Key Version: ${terminal.keyVersion}")
            appendLine("Status: ${terminal.status}")
            appendLine("Onboarded: ${java.time.Instant.ofEpochMilli(terminal.onboardedAt)}")
            appendLine()
            appendLine("Key Check Values:")
            appendLine("  TPK: ${simulator.calculateKeyCheckValue(terminal.tpk ?: ByteArray(0))}")
            appendLine("  TAK: ${simulator.calculateKeyCheckValue(terminal.tak ?: ByteArray(0))}")
            appendLine("  TDK: ${simulator.calculateKeyCheckValue(terminal.tdk ?: ByteArray(0))}")

            if (terminal.dukptProfile != null) {
                appendLine()
                appendLine("DUKPT Configuration:")
                appendLine("  KSN: ${terminal.dukptProfile.keySerialNumber}")
                appendLine("  Counter: ${terminal.dukptProfile.currentCounter}")
                appendLine("  Max Counter: ${terminal.dukptProfile.maxCounter}")
                appendLine("  Scheme: ${terminal.dukptProfile.scheme}")
                val remaining = terminal.dukptProfile.maxCounter - terminal.dukptProfile.currentCounter
                appendLine("  Transactions Remaining: $remaining")

                if (remaining < 1000) {
                    appendLine("  ⚠️ WARNING: Low transaction count - key rotation recommended")
                }
            }
        }

        return HsmCommandResult.Success(report)
    }

    /**
     * Get acquirer summary
     */
    fun getAcquirerSummary(acquirerId: String): HsmCommandResult {
        val acquirer = acquirerProfiles[acquirerId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Acquirer not found"
            )

        val report = buildString {
            appendLine("Acquirer Summary")
            appendLine("================")
            appendLine("Acquirer ID: ${acquirer.acquirerId}")
            appendLine("Name: ${acquirer.acquirerName}")
            appendLine("Registered Terminals: ${acquirer.terminals.size}")
            appendLine("BDK Count: ${acquirer.bdkRegistry.size}")
            appendLine("PIN Block Format: ${acquirer.pinBlockFormat.description}")
            appendLine("MAC Algorithm: ${acquirer.macAlgorithm}")
            appendLine()
            appendLine("Terminals:")
            acquirer.terminals.forEach { (id, terminal) ->
                appendLine("  - $id (Version: ${terminal.keyVersion}, Status: ${terminal.status})")
            }

            if (acquirer.bdkRegistry.isNotEmpty()) {
                appendLine()
                appendLine("BDK Registry:")
                acquirer.bdkRegistry.forEach { (id, bdk) ->
                    appendLine("  - $id (KCV: ${simulator.calculateKeyCheckValue(bdk)})")
                }
            }
        }

        return HsmCommandResult.Success(report)
    }

    /**
     * List all terminals
     */
    fun listAllTerminals(): HsmCommandResult {
        if (terminalProfiles.isEmpty()) {
            return HsmCommandResult.Success("No terminals registered")
        }

        val report = buildString {
            appendLine("Registered Terminals")
            appendLine("===================")
            appendLine()
            terminalProfiles.forEach { (id, terminal) ->
                val dukpt = if (terminal.dukptProfile != null) "DUKPT" else "Standard"
                appendLine("$id | ${terminal.acquirerId} | v${terminal.keyVersion} | ${terminal.status} | $dukpt")
            }
        }

        return HsmCommandResult.Success(report)
    }

    /**
     * List all acquirers
     */
    fun listAllAcquirers(): HsmCommandResult {
        if (acquirerProfiles.isEmpty()) {
            return HsmCommandResult.Success("No acquirers registered")
        }

        val report = buildString {
            appendLine("Registered Acquirers")
            appendLine("===================")
            appendLine()
            acquirerProfiles.forEach { (id, acquirer) ->
                appendLine("$id | ${acquirer.acquirerName} | ${acquirer.terminals.size} terminals")
            }
        }

        return HsmCommandResult.Success(report)
    }

    // ====================================================================================================
    // HEALTH CHECKS
    // ====================================================================================================

    /**
     * Check DUKPT counter health across all terminals
     */
    fun checkDukptHealth(): HsmCommandResult {
        val warnings = mutableListOf<String>()
        val critical = mutableListOf<String>()

        terminalProfiles.values.forEach { terminal ->
            terminal.dukptProfile?.let { dukpt ->
                val remaining = dukpt.maxCounter - dukpt.currentCounter
                val percentRemaining = (remaining.toDouble() / dukpt.maxCounter) * 100

                when {
                    percentRemaining < 5 -> critical.add(
                        "${terminal.terminalId}: CRITICAL - ${remaining} transactions remaining"
                    )
                    percentRemaining < 10 -> warnings.add(
                        "${terminal.terminalId}: WARNING - ${remaining} transactions remaining"
                    )
                }
            }
        }

        val report = buildString {
            appendLine("DUKPT Health Check")
            appendLine("==================")

            if (critical.isNotEmpty()) {
                appendLine()
                appendLine("🔴 CRITICAL (< 5% remaining):")
                critical.forEach { appendLine("  - $it") }
            }

            if (warnings.isNotEmpty()) {
                appendLine()
                appendLine("⚠️ WARNING (< 10% remaining):")
                warnings.forEach { appendLine("  - $it") }
            }

            if (critical.isEmpty() && warnings.isEmpty()) {
                appendLine()
                appendLine("✅ All terminals healthy")
            }
        }

        return HsmCommandResult.Success(report)
    }

    /**
     * Validate terminal key integrity
     */
    fun validateTerminalKeys(terminalId: String): HsmCommandResult {
        val terminal = terminalProfiles[terminalId]
            ?: return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Terminal not found"
            )

        val issues = mutableListOf<String>()

        // Check if keys are present
        if (terminal.tpk == null) issues.add("TPK missing")
        if (terminal.tak == null) issues.add("TAK missing")
        if (terminal.tdk == null) issues.add("TDK missing")

        // Check key relationships (optional - implementation specific)
        // For example, verify keys are properly derived from TMK

        // Check DUKPT if enabled
        terminal.dukptProfile?.let { dukpt ->
            if (dukpt.currentCounter >= dukpt.maxCounter) {
                issues.add("DUKPT counter exhausted")
            }
        }

        return if (issues.isEmpty()) {
            HsmCommandResult.Success("Terminal keys validated successfully")
        } else {
            HsmCommandResult.Error(
                HsmErrorCodes.KEY_CHECK_VALUE_FAILURE,
                "Validation issues: ${issues.joinToString(", ")}"
            )
        }
    }
}

// ====================================================================================================
// SUPPORTING DATA CLASSES
// ====================================================================================================

/**
 * Key injection methods
 */
sealed class KeyInjectionMethod {
    data class MasterKeyInjection(val masterKey: ByteArray) : KeyInjectionMethod()
    data class ComponentBased(val components: List<ByteArray>) : KeyInjectionMethod()
    data class RemoteKeyLoading(val encryptedTmk: ByteArray) : KeyInjectionMethod()
}

/**
 * Key derivation purposes
 */
enum class KeyDerivationPurpose {
    PIN_ENCRYPTION,
    AUTHENTICATION,
    DATA_ENCRYPTION
}

/**
 * Key rotation methods
 */
enum class KeyRotationMethod {
    AUTOMATIC,      // HSM generates new keys
    MANUAL,         // Keys provided externally
    SCHEDULED       // Time-based rotation
}

/**
 * Transaction context for processing
 */
data class TransactionContext(
    val terminalId: String,
    val accountNumber: String,
    val transactionAmount: String,
    val transactionType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val useDukpt: Boolean = false,
    val ksn: String? = null
)

/**
 * Terminal health metrics
 */
data class TerminalHealth(
    val terminalId: String,
    val keyVersion: Int,
    val dukptCounterRemaining: Long?,
    val lastTransaction: Long?,
    val healthStatus: HealthStatus
)

enum class HealthStatus {
    HEALTHY,
    WARNING,
    CRITICAL,
    OFFLINE
}