package `in`.aicortex.iso8583studio.hsm.payshield10k

/**
 * ========================================================================================================
 * PAYSHIELD 10K HSM SIMULATOR - USAGE EXAMPLES & DOCUMENTATION
 * ========================================================================================================
 */

import `in`.aicortex.iso8583studio.hsm.HsmConfig
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.*
import kotlinx.coroutines.runBlocking

/**
 * Comprehensive examples demonstrating all major features of the PayShield 10K HSM Simulator
 */
class PayShield10KExamples : HsmLogsListener {

    private val hsm = PayShield10KFeatures(HsmConfig(),this)
    private val commands = PayShield10KCommandProcessor(hsm,this)
    private val advanced = PayShield10KAdvancedFeatures(hsm, commands)

    /**
     * EXAMPLE 1: Basic HSM Setup - Generate and Load LMK
     */
    suspend fun example1_BasicHsmSetup() {
        println("=" * 80)
        println("EXAMPLE 1: Basic HSM Setup")
        println("=" * 80)

        // Step 1: Generate LMK components
        println("\n--- Generating LMK Components ---")
        val component1 = hsm.executeGenerateLmkComponent(lmkId = "00", componentNumber = 1)
        println("Component 1: ${(component1 as HsmCommandResult.Success).data["clearComponent"]}")

        val component2 = hsm.executeGenerateLmkComponent(lmkId = "00", componentNumber = 2)
        println("Component 2: ${(component2 as HsmCommandResult.Success).data["clearComponent"]}")

        val component3 = hsm.executeGenerateLmkComponent(lmkId = "00", componentNumber = 3)
        println("Component 3: ${(component3 as HsmCommandResult.Success).data["clearComponent"]}")

        // Step 2: Load LMK from components
        println("\n--- Loading LMK ---")
        val components = listOf(
            hsm.hexToBytes(component1.data["clearComponent"] as String),
            hsm.hexToBytes(component2.data["clearComponent"] as String),
            hsm.hexToBytes(component3.data["clearComponent"] as String)
        )

        val loadResult = hsm.executeLoadLmk("00", components)
        println((loadResult as HsmCommandResult.Success))
        println("LMK Check Value: ${loadResult.data["checkValue"]}")

        // Step 3: View LMK table
        println("\n--- LMK Table ---")
        val tableResult = hsm.executeViewLmkTable()
        println((tableResult as HsmCommandResult.Success))

        // Step 4: Run diagnostic test
        println("\n--- Diagnostic Test ---")
        val diagResult = hsm.executeDiagnosticTest()
        println((diagResult as HsmCommandResult.Success))
    }

    /**
     * EXAMPLE 2: Key Management - Generate, Import, Export Keys
     */
    suspend fun example2_KeyManagement() {
        println("\n" + "=" * 80)
        println("EXAMPLE 2: Key Management")
        println("=" * 80)

        // Generate ZMK
        println("\n--- Generate Zone Master Key (ZMK) ---")
        val zmkResult = commands.executeGenerateKey(
            lmkId = "00",
            keyType = KeyType.TYPE_000,
            keyLength = KeyLength.DOUBLE,
            algorithm = CipherAlgorithm.TRIPLE_DES
        )
        println("ZMK Generated")
        println("Encrypted: ${(zmkResult as HsmCommandResult.Success).data["encryptedKey"]}")
        println("KCV: ${zmkResult.data["kcv"]}")

        // Generate TPK
        println("\n--- Generate Terminal PIN Key (TPK) ---")
        val tpkResult = commands.executeGenerateKey(
            lmkId = "00",
            keyType = KeyType.TYPE_002,
            keyLength = KeyLength.DOUBLE
        )
        println("TPK Generated")
        println("KCV: ${(tpkResult  as HsmCommandResult.Success).data["kcv"]}")

        // Form key from components
        println("\n--- Form Key from Components ---")
        val comp1 = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val comp2 = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val comp3 = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }

        val formedKeyResult = commands.executeFormKeyFromComponents(
            lmkId = "00",
            keyType = KeyType.TYPE_001,
            components = listOf(comp1, comp2, comp3)
        )
        println("Key formed from 3 components")
        println("KCV: ${(formedKeyResult as HsmCommandResult.Success).data["kcv"]}")
    }

    /**
     * EXAMPLE 3: PIN Processing - Encrypt, Verify, Translate
     */
    suspend fun example3_PinProcessing() {
        println("\n" + "=" * 80)
        println("EXAMPLE 3: PIN Processing")
        println("=" * 80)

        val accountNumber = "4111111111111111"
        val clearPin = "1234"
        val tpk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }

        // Encrypt clear PIN
        println("\n--- Encrypt Clear PIN ---")
        val encryptResult = commands.executeEncryptClearPin(
            lmkId = "00",
            clearPin = clearPin,
            accountNumber = accountNumber,
            encryptedTpk = tpk,
            pinBlockFormat = PinBlockFormat.ISO_FORMAT_0
        )
        println("PIN Encrypted")
        println("Encrypted PIN Block: ${(encryptResult as HsmCommandResult.Success).data["encryptedPinBlock"]}")

        // Verify PIN
        println("\n--- Verify Terminal PIN ---")
        val encryptedPinBlock = hsm.hexToBytes(encryptResult.data["encryptedPinBlock"] as String)
        val verifyResult = commands.executeVerifyTerminalPin(
            encryptedPinBlock = encryptedPinBlock,
            accountNumber = accountNumber,
            tpk = tpk,
            expectedPin = clearPin,
            pinBlockFormat = PinBlockFormat.ISO_FORMAT_0
        )
        println((verifyResult as HsmCommandResult.Success))

        // Generate IBM PIN Offset
        println("\n--- Generate IBM PIN Offset ---")
        val pvk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val offsetResult = commands.executeGenerateIbmPinOffset(
            encryptedPin = encryptedPinBlock,
            pvk = pvk,
            accountNumber = accountNumber
        )
        println("PIN Offset: ${(offsetResult as HsmCommandResult.Success).data["offset"]}")
        println("Natural PIN: ${offsetResult.data["naturalPin"]}")

        // Generate VISA PVV
        println("\n--- Generate VISA PVV ---")
        val pvvResult = commands.executeGenerateVisaPvv(
            encryptedPin = encryptedPinBlock,
            pvk = pvk,
            accountNumber = accountNumber
        )
        println("PVV: ${(pvvResult as HsmCommandResult.Success).data["pvv"]}")
    }

    /**
     * EXAMPLE 4: DUKPT Implementation
     */
    suspend fun example4_DukptOperations() {
        println("\n" + "=" * 80)
        println("EXAMPLE 4: DUKPT Operations")
        println("=" * 80)

        val terminalId = "T123456"
        val bdk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }

        // Setup DUKPT
        println("\n--- Setup DUKPT for Terminal ---")
        val dukptProfile = commands.setupDukptForTerminal(
            terminalId = terminalId,
            bdk = bdk,
            scheme = DukptScheme.ANSI_X9_24_3DES
        )
        println("DUKPT Profile Created")
        println("KSN: ${dukptProfile.keySerialNumber}")
        println("Initial Counter: ${dukptProfile.currentCounter}")
        println("Max Counter: ${dukptProfile.maxCounter}")

        // Derive session keys for multiple transactions
        println("\n--- Derive DUKPT Session Keys ---")
        for (i in 0 until 5) {
            val sessionKey = commands.deriveDukptSessionKey(
                profile = dukptProfile.copy(currentCounter = i.toLong()),
                usageType = DukptKeyUsage.PIN_ENCRYPTION
            )
            println("Transaction $i - Session Key KCV: ${hsm.calculateKeyCheckValue(sessionKey)}")
        }

        // DUKPT PIN Translation
        println("\n--- DUKPT PIN Translation ---")
        val accountNumber = "4111111111111111"
        val clearPin = "5678"
        val tpk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }

        // First encrypt PIN under DUKPT
        val sessionKey0 = commands.deriveDukptSessionKey(
            profile = dukptProfile,
            usageType = DukptKeyUsage.PIN_ENCRYPTION
        )
        val encryptResult = commands.executeEncryptClearPin(
            lmkId = "00",
            clearPin = clearPin,
            accountNumber = accountNumber,
            encryptedTpk = sessionKey0,
            pinBlockFormat = PinBlockFormat.ISO_FORMAT_0
        )

        // Translate from DUKPT to ZPK
        val zpk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val translateResult = commands.executeTranslatePinDukptToZpk(
            encryptedPinBlock = hsm.hexToBytes((encryptResult as HsmCommandResult.Success).data["encryptedPinBlock"] as String),
            encryptedBdk = bdk,
            ksn = dukptProfile.keySerialNumber,
            encryptedDestZpk = zpk,
            accountNumber = accountNumber,
            lmkId =  "00",
            sourcePinBlockFormat = PinBlockFormat.ISO_FORMAT_0,
            destPinBlockFormat = PinBlockFormat.ISO_FORMAT_1
        )
        println("PIN Translated from DUKPT to ZPK")
        println("New PIN Block: ${(translateResult as HsmCommandResult.Success).data["encryptedPinBlock"]}")
    }

    /**
     * EXAMPLE 5: Terminal Onboarding with Multi-Acquirer Support
     */
    fun example5_TerminalOnboarding() {
        println("\n" + "=" * 80)
        println("EXAMPLE 5: Terminal Onboarding & Multi-Acquirer")
        println("=" * 80)

        // Register Acquirer 1
        println("\n--- Register Acquirer 1 (Visa) ---")
        val visaZmk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val visaCvk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val visaPvk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }

        val acquirer1Result = advanced.registerAcquirer(
            acquirerId = "ACQ001",
            acquirerName = "Visa Payment Services",
            zmk = visaZmk,
            cvk = visaCvk,
            pvk = visaPvk,
            pinBlockFormat = PinBlockFormat.ISO_FORMAT_0
        )
        println((acquirer1Result as HsmCommandResult.Success))

        // Register Acquirer 2
        println("\n--- Register Acquirer 2 (Mastercard) ---")
        val mcZmk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val mcCvk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }

        val acquirer2Result = advanced.registerAcquirer(
            acquirerId = "ACQ002",
            acquirerName = "Mastercard Processing",
            zmk = mcZmk,
            cvk = mcCvk,
            pinBlockFormat = PinBlockFormat.ISO_FORMAT_3
        )
        println((acquirer2Result as HsmCommandResult.Success))

        // Add BDK for DUKPT
        println("\n--- Add BDK to Acquirer ---")
        val bdk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val bdkResult = advanced.addBdkToAcquirer(
            acquirerId = "ACQ001",
            bdkId = "DEFAULT",
            bdk = bdk
        )
        println((bdkResult as HsmCommandResult.Success))

        // Onboard Terminal with Master Key Injection
        println("\n--- Onboard Terminal 1 (Master Key Injection) ---")
        val tmk1 = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val terminal1Result = advanced.onboardTerminal(
            terminalId = "TRM001",
            acquirerId = "ACQ001",
            keyInjectionMethod = KeyInjectionMethod.MasterKeyInjection(tmk1),
            enableDukpt = false
        )
        println((terminal1Result as HsmCommandResult.Success))

        // Onboard Terminal with Component-Based Injection
        println("\n--- Onboard Terminal 2 (Component-Based) ---")
        val components = listOf(
            ByteArray(16).also { hsm.secureRandom.nextBytes(it) },
            ByteArray(16).also { hsm.secureRandom.nextBytes(it) },
            ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        )
        val terminal2Result = advanced.onboardTerminal(
            terminalId = "TRM002",
            acquirerId = "ACQ001",
            keyInjectionMethod = KeyInjectionMethod.ComponentBased(components),
            enableDukpt = true
        )
        println((terminal2Result as HsmCommandResult.Success))

        // Onboard Terminal with Remote Key Loading
        println("\n--- Onboard Terminal 3 (Remote Key Loading) ---")
        val encryptedTmk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val terminal3Result = advanced.onboardTerminal(
            terminalId = "TRM003",
            acquirerId = "ACQ002",
            keyInjectionMethod = KeyInjectionMethod.RemoteKeyLoading(encryptedTmk),
            enableDukpt = false
        )
        println(terminal3Result)

        // View all terminals
        println("\n--- List All Terminals ---")
        val listResult = advanced.listAllTerminals()
        println(listResult)

        // View all acquirers
        println("\n--- List All Acquirers ---")
        val acquirersResult = advanced.listAllAcquirers()
        println(acquirersResult)
    }

    /**
     * EXAMPLE 6: Transaction Processing
     */
    suspend fun example6_TransactionProcessing() {
        println("\n" + "=" * 80)
        println("EXAMPLE 6: Transaction Processing")
        println("=" * 80)

        // Setup (assuming acquirer and terminal already registered)
        val tmk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val zmk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }

        advanced.registerAcquirer(
            acquirerId = "ACQ001",
            acquirerName = "Test Acquirer",
            zmk = zmk
        )

        advanced.onboardTerminal(
            terminalId = "TRM001",
            acquirerId = "ACQ001",
            keyInjectionMethod = KeyInjectionMethod.MasterKeyInjection(tmk),
            enableDukpt = true
        )

        // Process transaction
        println("\n--- Process Transaction ---")
        val accountNumber = "4111111111111111"
        val pin = "1234"

        // Simulate encrypted PIN block from terminal
        val sessionKey = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val encryptResult = commands.executeEncryptClearPin(
            lmkId = "00",
            clearPin = pin,
            accountNumber = accountNumber,
            encryptedTpk = sessionKey
        )

        val processResult = advanced.processIncomingTransaction(
            terminalId = "TRM001",
            encryptedPinBlock = hsm.hexToBytes((encryptResult as HsmCommandResult.Success).data["encryptedPinBlock"] as String),
            accountNumber = accountNumber,
            useDukpt = true,
            ksn = "1234567890000000000"
        )
        println(processResult)

        // Translate to another acquirer
        println("\n--- Translate to Another Acquirer ---")
        val zmk2 = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        advanced.registerAcquirer(
            acquirerId = "ACQ002",
            acquirerName = "Second Acquirer",
            zmk = zmk2
        )

        val translateResult = advanced.translateToAcquirer(
            lmkId = "00",
            sourceTerminalId = "TRM001",
            destinationAcquirerId = "ACQ002",
            encryptedPinBlock = hsm.hexToBytes(encryptResult.data["encryptedPinBlock"] as String),
            accountNumber = accountNumber
        )
        println("Translation: ${(translateResult as HsmCommandResult.Success).response}")
    }

    /**
     * EXAMPLE 7: Key Rotation and Management
     */
    fun example7_KeyRotation() {
        println("\n" + "=" * 80)
        println("EXAMPLE 7: Key Rotation")
        println("=" * 80)

        // Setup
        val tmk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val zmk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }

        advanced.registerAcquirer("ACQ001", "Test Acquirer", zmk)
        advanced.onboardTerminal(
            "TRM001",
            "ACQ001",
            KeyInjectionMethod.MasterKeyInjection(tmk)
        )

        // Get initial status
        println("\n--- Initial Terminal Status ---")
        val status1 = advanced.getTerminalStatus("TRM001")
        println(status1)

        // Rotate keys
        println("\n--- Rotate Terminal Keys ---")
        val rotateResult = advanced.rotateTerminalKeys(
            terminalId = "TRM001",
            rotationMethod = KeyRotationMethod.AUTOMATIC
        )
        println(rotateResult)

        // Get updated status
        println("\n--- Updated Terminal Status ---")
        val status2 = advanced.getTerminalStatus("TRM001")
        println(status2)
    }

    /**
     * EXAMPLE 8: Health Monitoring
     */
    fun example8_HealthMonitoring() {
        println("\n" + "=" * 80)
        println("EXAMPLE 8: Health Monitoring")
        println("=" * 80)

        // Setup multiple terminals with DUKPT
        val zmk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        advanced.registerAcquirer("ACQ001", "Test Acquirer", zmk)

        // Terminal with high counter
        val bdk1 = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        advanced.addBdkToAcquirer("ACQ001", "BDK1", bdk1)

        val tmk1 = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        advanced.onboardTerminal(
            "TRM001",
            "ACQ001",
            KeyInjectionMethod.MasterKeyInjection(tmk1),
            enableDukpt = true
        )

        // Check DUKPT health
        println("\n--- DUKPT Health Check ---")
        val healthResult = advanced.checkDukptHealth()
        println(healthResult)

        // Validate terminal keys
        println("\n--- Validate Terminal Keys ---")
        val validateResult = advanced.validateTerminalKeys("TRM001")
        println(validateResult)
    }

    /**
     * EXAMPLE 9: MAC Operations
     */
    fun example9_MacOperations() {
        println("\n" + "=" * 80)
        println("EXAMPLE 9: MAC Operations")
        println("=" * 80)

        val tak = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val data = "Hello World".toByteArray()

        // Generate MAC
        println("\n--- Generate MAC (ISO 9797 Alg 3) ---")
        val macResult = commands.executeGenerateMac(
            data = data,
            tak = tak,
            algorithm = "ISO9797_ALG3"
        )
        println("MAC: ${(macResult as HsmCommandResult.Success).data["mac"]}")

        // Verify MAC
        println("\n--- Verify MAC ---")
        val mac = hsm.hexToBytes(macResult.data["mac"] as String)
        val verifyResult = commands.executeVerifyMac(
            data = data,
            providedMac = mac,
            tak = tak,
            algorithm = "ISO9797_ALG3"
        )
        println(verifyResult)
    }

    /**
     * EXAMPLE 10: Complete End-to-End Workflow
     */
    suspend fun example10_CompleteWorkflow() {
        println("\n" + "=" * 80)
        println("EXAMPLE 10: Complete End-to-End Workflow")
        println("=" * 80)

        // 1. Initialize HSM
        println("\n[1] Initializing HSM...")
        val comp1 = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val comp2 = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        hsm.executeLoadLmk("00", listOf(comp1, comp2))
        println("✓ LMK Loaded")

        // 2. Register Acquirers
        println("\n[2] Registering Acquirers...")
        val visaZmk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val mcZmk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }

        advanced.registerAcquirer("VISA", "Visa", visaZmk)
        advanced.registerAcquirer("MC", "Mastercard", mcZmk)
        println("✓ Acquirers Registered")

        // 3. Setup BDKs
        println("\n[3] Setting up BDKs...")
        val visaBdk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
        val mcBdk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }

        advanced.addBdkToAcquirer("VISA", "DEFAULT", visaBdk)
        advanced.addBdkToAcquirer("MC", "DEFAULT", mcBdk)
        println("✓ BDKs Configured")

        // 4. Onboard Terminals
        println("\n[4] Onboarding Terminals...")
        for (i in 1..5) {
            val tmk = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
            val acquirer = if (i % 2 == 0) "VISA" else "MC"
            advanced.onboardTerminal(
                terminalId = "TRM00$i",
                acquirerId = acquirer,
                keyInjectionMethod = KeyInjectionMethod.MasterKeyInjection(tmk),
                enableDukpt = true
            )
        }
        println("✓ 5 Terminals Onboarded")

        // 5. Process Transactions
        println("\n[5] Processing Transactions...")
        for (i in 1..10) {
            val terminalId = "TRM00${(i % 5) + 1}"
            val account = "41111111111111${String.format("%02d", i)}"

            val sessionKey = ByteArray(16).also { hsm.secureRandom.nextBytes(it) }
            val pinResult = commands.executeEncryptClearPin(
                lmkId = "00",
                clearPin = "1234",
                accountNumber = account,
                encryptedTpk = sessionKey
            )

            advanced.processIncomingTransaction(
                terminalId = terminalId,
                encryptedPinBlock = hsm.hexToBytes((pinResult as HsmCommandResult.Success).data["encryptedPinBlock"] as String),
                accountNumber = account,
                useDukpt = true,
                ksn = "12345678900000000${String.format("%02d", i)}"
            )
        }
        println("✓ 10 Transactions Processed")

        // 6. Health Check
        println("\n[6] Running Health Checks...")
        val health = advanced.checkDukptHealth()
        println(health)

        // 7. Generate Reports
        println("\n[7] Generating Reports...")
        println("\nTerminals:")
        println(advanced.listAllTerminals())
        println("\nAcquirers:")
        println(advanced.listAllAcquirers())

        println("\n" + "=" * 80)
        println("✓ Complete Workflow Executed Successfully")
        println("=" * 80)
    }

    /**
     * Run all examples
     */
    fun runAllExamples() {
        runBlocking {
            example1_BasicHsmSetup()
            example2_KeyManagement()
            example3_PinProcessing()
            example4_DukptOperations()
            example5_TerminalOnboarding()
            example6_TransactionProcessing()
            example7_KeyRotation()
            example8_HealthMonitoring()
            example9_MacOperations()
            example10_CompleteWorkflow()
        }
    }

    override fun onAuditLog(auditLog: AuditEntry) {

    }

    override fun log(auditLog: String) {
    }

    override fun onFormattedRequest(log: String) {

    }

    override fun onFormattedResponse(log: String) {

    }

}

/**
 * Extension function for string repetition
 */
private operator fun String.times(count: Int): String = repeat(count)

/**
 * Main function to run examples
 */
fun main() {
    val examples = PayShield10KExamples()

    println("""
        ╔════════════════════════════════════════════════════════════════════════════════╗
        ║                                                                                ║
        ║                    PAYSHIELD 10K HSM SIMULATOR                                 ║
        ║                    Comprehensive Examples & Demo                               ║
        ║                                                                                ║
        ║  Professional-grade Hardware Security Module Simulator                         ║
        ║  Based on Thales PayShield 10K Specification                                   ║
        ║                                                                                ║
        ╚════════════════════════════════════════════════════════════════════════════════╝
    """.trimIndent())

    println("\nRunning all examples...")
    println("\nPress Enter to start...")
    readLine()

    examples.runAllExamples()

    println("\n\nAll examples completed successfully!")
    println("Check the output above for detailed results of each example.\n")
}