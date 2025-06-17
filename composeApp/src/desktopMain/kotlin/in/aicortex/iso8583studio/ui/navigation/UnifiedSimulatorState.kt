package `in`.aicortex.iso8583studio.ui.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.data.model.ConnectionStatus
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.domain.ImportResult
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.Transaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.awt.SystemColor
import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private var isLoaded = false

/**
 * Enum representing different types of simulators
 */
@Serializable
enum class SimulatorType(val displayName: String, val description: String) {
    HOST("Host Simulator", "ISO8583 Host Response Simulator"),
    HSM("HSM Simulator", "Hardware Security Module Simulator"),
    APDU("APDU Simulator", "Smart Card APDU Command Simulator"),
    POS("POS Simulator", "Point of Sale Terminal Simulator"),
    ECR("ECR Simulator", "Point of Sale Terminal Simulator"),
    ATM("ATM Simulator", "Automated Teller Machine Simulator"),
    CARD("Card Simulator", "Payment Card Simulator"),
    SWITCH("Switch Simulator", "Payment Network Switch Simulator"),
    ACQUIRER("Acquirer Simulator", "Acquiring Bank Simulator"),
    ISSUER("Issuer Simulator", "Card Issuing Bank Simulator")
}

// --- Data Classes for Key Management ---
enum class KeyAlgorithm(val displayName: String) {
    RSA("RSA"),
    AES("AES"),
    ECC("ECC"),
    DES("3DES")
}

@Serializable
data class KeyManagementSettings(
    val defaultKeyAlgorithm: KeyAlgorithm = KeyAlgorithm.RSA,
    val defaultRsaKeySize: Int = 2048,
    val defaultAesKeySize: Int = 256,
    val defaultEccCurve: String = "secp256r1",
    val allowKeyExport: Boolean = false,
    val enableKeyRotation: Boolean = false,
    val rotationPeriodDays: Int = 90,
    val enableArchiving: Boolean = true,
    val wrappingAlgorithm: String = "AES-GCM",
    val allowPlaintextKeys: Boolean = false
)


/**
 * HSM Simulator Configuration
 */
@Serializable
data class HSMSimulatorConfig(
    override val id: String,
    override val name: String,
    override val description: String = "",
    override val simulatorType: SimulatorType = SimulatorType.HSM,
    override val enabled: Boolean = true,
    override val createdDate: Long = System.currentTimeMillis(),
    override val modifiedDate: Long = System.currentTimeMillis(),
    override val version: String = "1.0",

    // HSM-specific properties
    val slotCount: Int = 8,
    val pkcs11Compliance: Boolean = true,
    val fipsLevel: FIPSLevel = FIPSLevel.LEVEL_2,
    val supportedAlgorithms: List<CryptoAlgorithm> = emptyList(),
    val keyStorage: KeyStorageConfig = KeyStorageConfig(),
    val authenticationPolicy: AuthenticationPolicy = AuthenticationPolicy(),
    val auditConfig: AuditConfig = AuditConfig(),
    val performanceSettings: PerformanceSettings = PerformanceSettings(),

    val deviceInfo: HSMDeviceInfo = HSMDeviceInfo(),
    val keyManagementSettings: KeyManagementSettings = KeyManagementSettings(),
    val slotConfiguration: SlotConfiguration = SlotConfiguration(),
    val securitySettings: SecuritySettings = SecuritySettings(),
    val initializationSettings: InitializationSettings = InitializationSettings(),
    val complianceSettings: ComplianceSettings = ComplianceSettings(),
    val networkSettings: NetworkSettings = NetworkSettings(),
    val cryptoConfig: CryptoConfig = CryptoConfig(),
    val securityPolicies: SecurityPolicies = SecurityPolicies()
) : SimulatorConfig


@Serializable
data class SecurityPolicies(
    val minPinLength: Int = 4,
    val maxPinLength: Int = 8,
    val pinRetryLimit: Int = 3,
    val enablePinLockout: Boolean = true,
    val enableMofN: Boolean = false,
    val mValue: Int = 2,
    val nValue: Int = 3,
    val users: List<HSMUser> = listOf(
        HSMUser("so", "Security Officer"),
        HSMUser("user", "Crypto User")
    )
)
@Serializable
data class HSMUser(val username: String, val role: String)

@Serializable
data class CryptoConfig(
    val supportedAlgorithms: Set<String> = setOf("RSA", "AES", "ECC", "SHA-256"),
    val rsaKeySizes: Set<Int> = setOf(2048, 4096),
    val aesKeySizes: Set<Int> = setOf(128, 256),
    val eccCurves: Set<String> = setOf("secp256r1", "secp384r1"),
    val supportedMechanisms: Set<String> = setOf(
        "CKM_RSA_PKCS_KEY_PAIR_GEN",
        "CKM_RSA_PKCS",
        "CKM_AES_KEY_GEN"
    )
)

@Serializable
data class HSMDeviceInfo(
    val deviceName: String = "ISO8583Studio HSM Simulator",
    val manufacturerName: String = "Aicortex",
    val modelName: String = "HSM-SIM-v1",
    val serialNumber: String = "HSM001234567890",
    val firmwareVersion: String = "1.0.0",
    val hardwareVersion: String = "1.0",
    val deviceDescription: String = "Simulated Hardware Security Module for development and testing"
)

@Serializable
data class SlotConfiguration(
    val totalSlots: Int = 8,
    val activeSlots: Int = 4,
    val slotTokenPresent: Map<Int, Boolean> = mapOf(0 to true, 1 to true),
    val slotTokenWritable: Map<Int, Boolean> = mapOf(0 to false, 1 to true),
    val slotHardwareFeatures: Map<Int, List<String>> = mapOf(
        0 to listOf("RNG", "Clock"),
        1 to listOf("RNG", "Clock", "User PIN")
    )
)

@Serializable
data class SecuritySettings(
    val authenticationRequired: Boolean = true,
    val soPin: String = "123456",
    val userPin: String = "1234",
    val pinRetryLimit: Int = 3,
    val minPinLength: Int = 4,
    val maxPinLength: Int = 8,
    val tamperResistance: Boolean = true,
    val secureMessaging: Boolean = true
)

@Serializable
data class InitializationSettings(
    val autoInitialize: Boolean = true,
    val initializeTokens: Boolean = true,
    val generateDefaultKeys: Boolean = false,
    val loadTestCertificates: Boolean = false,
    val enableDebugMode: Boolean = false,
)

@Serializable
data class ComplianceSettings(
    val pkcs11Version: String = "2.40",
    val fipsMode: Boolean = false,
    val fipsLevel: Int = 2,
    val commonCriteria: Boolean = false,
    val ccLevel: String = "EAL4+",
    val validateCertificates: Boolean = true
)

@Serializable
data class NetworkSettings(
    val enableNetworkAccess: Boolean = true,
    val bindAddress: String = "127.0.0.1",
    val port: Int = 9999,
    val maxConnections: Int = 10,
    val connectionTimeout: Int = 30,
    val enableSSL: Boolean = false,
    val sslCertPath: String = "",
    val sslKeyPath: String = ""
)

/**
 * APDU Simulator Configuration
 */
@Serializable
data class APDUSimulatorConfig(
    override val id: String,
    override val name: String,
    override val description: String,
    override val simulatorType: SimulatorType = SimulatorType.APDU,
    override val enabled: Boolean = true,
    override val createdDate: Long,
    override val modifiedDate: Long,
    override val version: String = "1.0",

    // APDU-specific properties
    val cardType: CardType = CardType.EMV,
    val atr: String = "3B9F95801FC78031E073FE211B63004C45544F4E",
    val applications: List<CardApplication> = emptyList(),
    val fileSystem: CardFileSystem,
    val securityDomain: SecurityDomain,
    val scriptCommands: List<APDUScript> = emptyList()
) : SimulatorConfig

/**
 * POS Simulator Configuration
 */
@Serializable
data class POSSimulatorConfig(
    override val id: String,
    override val name: String,
    override val description: String,
    override val simulatorType: SimulatorType = SimulatorType.POS,
    override val enabled: Boolean = true,
    override val createdDate: Long,
    override val modifiedDate: Long,
    override val version: String = "1.0",

    // POS-specific properties
    val terminalid: Int,
    val merchantid: Int,
    val acquirerid: Int,
    val supportedCards: List<CardType> = emptyList(),
    val simulatedTransactionsToDest: List<Transaction> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val emvConfig: EMVConfig = EMVConfig(),
    val contactlessConfig: ContactlessConfig = ContactlessConfig(),
    val pinpadConfig: PinpadConfig = PinpadConfig(),
    var profileName: String = "New POS Profile",
    // Hardware
    var pinEntryOptions: String = "Integrated PIN pad",
    var cardReaderTypes: String = "Triple-head reader (MSR + chip + contactless)",
    var displayConfig: String = "Dual displays (merchant + customer)",
    var receiptPrinting: String = "Thermal receipt printer",
    // Transaction
    var terminalCapabilities: Set<String> = setOf("Offline transaction processing", "Void and refund processing"),
    var transactionLimits: String = "Standard Retail Limits",
    // Security
    var encryptionSecurity: Set<String> = setOf("End-to-end encryption (E2EE)", "PCI DSS compliance level"),
    var authMethods: String = "PIN verification",
    // Network & Software
    var connectivity: String = "Ethernet/LAN connection",
    var osType: String = "Proprietary OS"
) : SimulatorConfig

// Supporting data classes for HSM
@Serializable
enum class FIPSLevel { LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4 }

@Serializable
enum class CryptoAlgorithm {
    AES_128, AES_192, AES_256,
    DES, TRIPLE_DES,
    RSA_1024, RSA_2048, RSA_4096,
    ECC_P256, ECC_P384, ECC_P521,
    SHA_1, SHA_256, SHA_384, SHA_512,
    HMAC_SHA_256, HMAC_SHA_512
}

@Serializable
data class KeyStorageConfig(
    val maxKeys: Int = 1000,
    val keyWrapping: Boolean = true,
    val keyBackup: Boolean = false,
    val keyRecovery: Boolean = false
)

@Serializable
data class AuthenticationPolicy(
    val requiresLogin: Boolean = true,
    val maxLoginAttempts: Int = 3,
    val sessionTimeout: Long = 3600000L, // 1 hour
    val strongAuthentication: Boolean = false
)

@Serializable
data class AuditConfig(
    val enableAuditLog: Boolean = true,
    val logAllOperations: Boolean = true,
    val auditLogPath: String = "./hsm_audit.log",
    val maxLogSize: Long = 100_000_000L // 100MB
)

@Serializable
data class PerformanceSettings(
    val maxOperationsPerSecond: Int = 1000,
    val enablePerformanceLogging: Boolean = false,
    val operationTimeout: Long = 30000L // 30 seconds
)

// Supporting data classes for APDU
@Serializable
enum class CardType {
    EMV, MIFARE_CLASSIC, MIFARE_DESFIRE, JAVACARD, MULTOS, CUSTOM
}

@Serializable
data class CardApplication(
    val aid: Int,
    val name: String,
    val version: String,
    val priority: Int = 0,
    val selectable: Boolean = true
)

@Serializable
data class CardFileSystem(
    val masterFile: String = "3F00",
    val dedicatedFiles: List<String> = emptyList(),
    val elementaryFiles: List<String> = emptyList()
)

@Serializable
data class SecurityDomain(
    val aid: String = "A000000003000000",
    val privileges: List<String> = emptyList(),
    val keyVersionNumber: Int = 1
)

@Serializable
data class APDUScript(
    val name: String,
    val description: String,
    val commands: List<APDUCommand>
)

@Serializable
data class APDUCommand(
    val cla: String,
    val ins: String,
    val p1: String,
    val p2: String,
    val data: String = "",
    val le: String = "",
    val expectedSw: String = "9000"
)

// Supporting data classes for POS
@Serializable
enum class PaymentMethod {
    CONTACT_EMV, CONTACTLESS_EMV, MAGNETIC_STRIPE, QR_CODE, NFC, MOBILE_PAYMENT
}

@Serializable
data class EMVConfig(
    val terminalType: String = "22",
    val terminalCapabilities: String = "E0F8C8",
    val additionalTerminalCapabilities: String = "6000F0A001",
    val applicationVersionNumber: String = "0096"
)

@Serializable
data class ContactlessConfig(
    val enabled: Boolean = true,
    val transactionLimit: Long = 5000L, // in cents
    val clessCapabilities: String = "E0F8C8",
    val kernel2Support: Boolean = true,
    val kernel3Support: Boolean = true
)

@Serializable
data class PinpadConfig(
    val enabled: Boolean = true,
    val pinpadType: String = "INTERNAL",
    val encryptionKey: String = "",
    val offlinePinSupport: Boolean = true
)

/**
 * Unified Application State for all Simulator Configurations
 */
data class UnifiedSimulatorState(
    // All simulator configurations grouped by type
    val hostConfigs: MutableState<List<GatewayConfig>> = mutableStateOf(emptyList()),
    val hsmConfigs: MutableState<List<HSMSimulatorConfig>> = mutableStateOf(emptyList()),
    val apduConfigs: MutableState<List<APDUSimulatorConfig>> = mutableStateOf(emptyList()),
    val posConfigs: MutableState<List<POSSimulatorConfig>> = mutableStateOf(emptyList()),

    // General state
    var resultDialogInterface: ResultDialogInterface? = null,
    var selectedSimulatorType: SimulatorType = SimulatorType.HOST,
    var selectedConfigIndex: Int = -1,
    val selectedTabIndex: Int = 0,
    var panelWidth: Dp = 340.dp,
    var connectionStatus: ConnectionStatus? = null,
    var window: ComposeWindow? = null
) {
    private val name = "Iso8583Studio"

    // JSON configuration with polymorphic serialization
    private val json = Json {
        prettyPrint = true
        serializersModule = SerializersModule {
            polymorphic(SimulatorConfig::class) {
                subclass(GatewayConfig::class)
                subclass(HSMSimulatorConfig::class)
                subclass(APDUSimulatorConfig::class)
                subclass(POSSimulatorConfig::class)
            }
        }
    }

    init {
        if (!isLoaded) {
            load()
            isLoaded = true
        }
    }

    /**
     * Get all configurations as a unified list
     */
    fun getAllConfigs(): List<SimulatorConfig> {
        return hostConfigs.value + hsmConfigs.value + apduConfigs.value + posConfigs.value
    }

    /**
     * Get configurations by type
     */
    fun getConfigsByType(type: SimulatorType): List<SimulatorConfig> {
        return when (type) {
            SimulatorType.HOST -> hostConfigs.value
            SimulatorType.HSM -> hsmConfigs.value
            SimulatorType.APDU -> apduConfigs.value
            SimulatorType.POS -> posConfigs.value
            else -> emptyList() // For future simulator types
        }
    }

    /**
     * Get current selected configuration
     */
    val currentConfig: SimulatorConfig?
        get() {
            val configs = getConfigsByType(selectedSimulatorType)
            return if (selectedConfigIndex >= 0 && selectedConfigIndex < configs.size) {
                configs[selectedConfigIndex]
            } else {
                null
            }
        }

    /**
     * Add a new configuration (automatically detects type)
     */
    fun addConfig(config: SimulatorConfig) {
        when (config) {
            is GatewayConfig -> {
                hostConfigs.value = hostConfigs.value + config
                // Auto-select newly added config
                selectedSimulatorType = SimulatorType.HOST
                selectedConfigIndex = hostConfigs.value.size - 1
            }

            is HSMSimulatorConfig -> {
                hsmConfigs.value = hsmConfigs.value + config
                selectedSimulatorType = SimulatorType.HSM
                selectedConfigIndex = hsmConfigs.value.size - 1
            }

            is APDUSimulatorConfig -> {
                apduConfigs.value = apduConfigs.value + config
                selectedSimulatorType = SimulatorType.APDU
                selectedConfigIndex = apduConfigs.value.size - 1
            }

            is POSSimulatorConfig -> {
                posConfigs.value = posConfigs.value + config
                selectedSimulatorType = SimulatorType.POS
                selectedConfigIndex = posConfigs.value.size - 1
            }
        }
        save()
    }

    /**
     * Update an existing configuration (automatically detects type)
     */
    fun updateConfig(config: SimulatorConfig?) {
        when (config) {
            is GatewayConfig -> {
                val index = hostConfigs.value.indexOfFirst { it.id == config.id }
                if (index >= 0) {
                    val newList = hostConfigs.value.toMutableList()
                    newList[index] = config.copy(modifiedDate = System.currentTimeMillis())
                    hostConfigs.value = newList
                }
            }

            is HSMSimulatorConfig -> {
                val index = hsmConfigs.value.indexOfFirst { it.id == config.id }
                if (index >= 0) {
                    val newList = hsmConfigs.value.toMutableList()
                    newList[index] = config.copy(modifiedDate = System.currentTimeMillis())
                    hsmConfigs.value = newList
                }
            }

            is APDUSimulatorConfig -> {
                val index = apduConfigs.value.indexOfFirst { it.id == config.id }
                if (index >= 0) {
                    val newList = apduConfigs.value.toMutableList()
                    newList[index] = config.copy(modifiedDate = System.currentTimeMillis())
                    apduConfigs.value = newList
                }
            }

            is POSSimulatorConfig -> {
                val index = posConfigs.value.indexOfFirst { it.id == config.id }
                if (index >= 0) {
                    val newList = posConfigs.value.toMutableList()
                    newList[index] = config.copy(modifiedDate = System.currentTimeMillis())
                    posConfigs.value = newList
                }
            }
        }
        save()
    }

    /**
     * Delete a configuration by ID (automatically detects type)
     */
    fun deleteConfig(configId: String): Boolean {
        // Try to find and delete from each type
        var found = false

        // Check HOST configs
        val hostIndex = hostConfigs.value.indexOfFirst { it.id == configId }
        if (hostIndex >= 0) {
            hostConfigs.value = hostConfigs.value.filter { it.id != configId }
            if (selectedSimulatorType == SimulatorType.HOST) {
                selectedConfigIndex = if (hostConfigs.value.isNotEmpty()) {
                    minOf(selectedConfigIndex, hostConfigs.value.size - 1)
                } else -1
            }
            found = true
        }

        // Check HSM configs
        val hsmIndex = hsmConfigs.value.indexOfFirst { it.id == configId }
        if (hsmIndex >= 0) {
            hsmConfigs.value = hsmConfigs.value.filter { it.id != configId }
            if (selectedSimulatorType == SimulatorType.HSM) {
                selectedConfigIndex = if (hsmConfigs.value.isNotEmpty()) {
                    minOf(selectedConfigIndex, hsmConfigs.value.size - 1)
                } else -1
            }
            found = true
        }

        // Check APDU configs
        val apduIndex = apduConfigs.value.indexOfFirst { it.id == configId }
        if (apduIndex >= 0) {
            apduConfigs.value = apduConfigs.value.filter { it.id != configId }
            if (selectedSimulatorType == SimulatorType.APDU) {
                selectedConfigIndex = if (apduConfigs.value.isNotEmpty()) {
                    minOf(selectedConfigIndex, apduConfigs.value.size - 1)
                } else -1
            }
            found = true
        }

        // Check POS configs
        val posIndex = posConfigs.value.indexOfFirst { it.id == configId }
        if (posIndex >= 0) {
            posConfigs.value = posConfigs.value.filter { it.id != configId }
            if (selectedSimulatorType == SimulatorType.POS) {
                selectedConfigIndex = if (posConfigs.value.isNotEmpty()) {
                    minOf(selectedConfigIndex, posConfigs.value.size - 1)
                } else -1
            }
            found = true
        }

        if (found) {
            save()
        }

        return found
    }

    /**
     * Delete the currently selected configuration
     */
    fun deleteCurrentConfig(): Boolean {
        return currentConfig?.let { config ->
            deleteConfig(config.id)
        } ?: false
    }

    /**
     * Find configuration by ID across all types
     */
    fun findConfigById(configId: String): SimulatorConfig? {
        return getAllConfigs().find { it.id == configId }
    }

    /**
     * Check if configuration exists by ID
     */
    fun configExists(configId: String): Boolean {
        return findConfigById(configId) != null
    }

    /**
     * Get configuration type by ID
     */
    fun getConfigType(configId: String): SimulatorType? {
        return findConfigById(configId)?.simulatorType
    }

    /**
     * Enable/disable configuration by ID
     */
    fun setConfigEnabled(configId: String, enabled: Boolean): Boolean {
        val config = findConfigById(configId) ?: return false

        val updatedConfig = when (config) {
            is GatewayConfig -> config.copy(
                enabled = enabled,
                modifiedDate = System.currentTimeMillis()
            )

            is HSMSimulatorConfig -> config.copy(
                enabled = enabled,
                modifiedDate = System.currentTimeMillis()
            )

            is APDUSimulatorConfig -> config.copy(
                enabled = enabled,
                modifiedDate = System.currentTimeMillis()
            )

            is POSSimulatorConfig -> config.copy(
                enabled = enabled,
                modifiedDate = System.currentTimeMillis()
            )

            else -> null
        }

        updateConfig(updatedConfig)
        return true
    }

    /**
     * Duplicate a configuration with a new ID and name
     */
    fun duplicateConfig(configId: String, newName: String? = null): SimulatorConfig? {
        val originalConfig = findConfigById(configId) ?: return null
        val timestamp = System.currentTimeMillis()
        val duplicatedName = newName ?: "${originalConfig.name} (Copy)"

        val duplicatedConfig = when (originalConfig) {
            is GatewayConfig -> originalConfig.copy(
                id = generateConfigId(),
                name = duplicatedName,
                createdDate = timestamp,
                modifiedDate = timestamp
            )

            is HSMSimulatorConfig -> originalConfig.copy(
                id = generateConfigId(),
                name = duplicatedName,
                createdDate = timestamp,
                modifiedDate = timestamp
            )

            is APDUSimulatorConfig -> originalConfig.copy(
                id = generateConfigId(),
                name = duplicatedName,
                createdDate = timestamp,
                modifiedDate = timestamp
            )

            is POSSimulatorConfig -> originalConfig.copy(
                id = generateConfigId(),
                name = duplicatedName,
                createdDate = timestamp,
                modifiedDate = timestamp
            )

            else -> null
        }

        duplicatedConfig?.let { addConfig(it) }
        return duplicatedConfig
    }

    /**
     * Batch operations for multiple configurations
     */
    fun deleteConfigs(configIds: List<String>): Int {
        var deletedCount = 0
        configIds.forEach { configId ->
            if (deleteConfig(configId)) {
                deletedCount++
            }
        }
        return deletedCount
    }

    /**
     * Enable/disable multiple configurations
     */
    fun setConfigsEnabled(configIds: List<String>, enabled: Boolean): Int {
        var updatedCount = 0
        configIds.forEach { configId ->
            if (setConfigEnabled(configId, enabled)) {
                updatedCount++
            }
        }
        return updatedCount
    }

    /**
     * Get configurations by enabled status
     */
    fun getEnabledConfigs(): List<SimulatorConfig> {
        return getAllConfigs().filter { it.enabled }
    }

    fun getDisabledConfigs(): List<SimulatorConfig> {
        return getAllConfigs().filter { !it.enabled }
    }

    /**
     * Search configurations by name or description
     */
    fun searchConfigs(query: String): List<SimulatorConfig> {
        val lowercaseQuery = query.lowercase()
        return getAllConfigs().filter { config ->
            config.name.lowercase().contains(lowercaseQuery) ||
                    config.description.lowercase().contains(lowercaseQuery)
        }
    }

    /**
     * Get configurations sorted by various criteria
     */
    fun getConfigsSortedByName(): List<SimulatorConfig> {
        return getAllConfigs().sortedBy { it.name.lowercase() }
    }

    fun getConfigsSortedByType(): List<SimulatorConfig> {
        return getAllConfigs().sortedBy { it.simulatorType.name }
    }

    fun getConfigsSortedByModified(): List<SimulatorConfig> {
        return getAllConfigs().sortedByDescending { it.modifiedDate }
    }

    fun getConfigsSortedByCreated(): List<SimulatorConfig> {
        return getAllConfigs().sortedByDescending { it.createdDate }
    }

    /**
     * Select configuration by ID (automatically sets type and index)
     */
    fun selectConfig(configId: String): Boolean {
        val config = findConfigById(configId) ?: return false

        selectedSimulatorType = config.simulatorType
        val configs = getConfigsByType(config.simulatorType)
        selectedConfigIndex = configs.indexOfFirst { it.id == configId }

        return selectedConfigIndex >= 0
    }

    /**
     * Generate a unique configuration ID
     */
    @OptIn(ExperimentalUuidApi::class)
    fun generateConfigId(): String {
        return Uuid.random().toHexDashString()
    }

    /**
     * Validate configuration before saving
     */
    private fun validateConfig(config: SimulatorConfig): Boolean {
        return config.name.isNotBlank() && config.id.isNotEmpty()
    }

    /**
     * Export all configurations
     */
    fun export(): String {
        return try {
            save()
            val allConfigs = SimulatorConfigCollection(
                hostConfigs = hostConfigs.value,
                hsmConfigs = hsmConfigs.value,
                apduConfigs = apduConfigs.value,
                posConfigs = posConfigs.value,
                exportedAt = System.currentTimeMillis(),
                version = "1.0"
            )
            json.encodeToString(SimulatorConfigCollection.serializer(), allConfigs)
        } catch (e: Exception) {
            e.message ?: "Unable to export configurations"
        }
    }

    /**
     * Export specific simulator type configurations
     */
    fun exportByType(type: SimulatorType): String {
        return try {
            val configs = getConfigsByType(type)
            json.encodeToString(configs)
        } catch (e: Exception) {
            e.message ?: "Unable to export $type configurations"
        }
    }

    /**
     * Load configurations from file
     */
    fun load() {
        val file = File("${name}_simulators.json")
        import(file)
    }

    /**
     * Save all configurations to file
     */
    fun save(): Boolean {
        return try {
            val file = File("${name}_simulators.json")
            val allConfigs = SimulatorConfigCollection(
                hostConfigs = hostConfigs.value,
                hsmConfigs = hsmConfigs.value,
                apduConfigs = apduConfigs.value,
                posConfigs = posConfigs.value,
                exportedAt = System.currentTimeMillis(),
                version = "1.0"
            )
            val configJson = json.encodeToString(SimulatorConfigCollection.serializer(), allConfigs)
            file.writeBytes(configJson.toByteArray())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Import configurations from file
     */
    fun import(file: File): ImportResult {
        return try {
            if (!file.exists()) {
                return ImportResult.Success(
                    fileExtension = "json",
                    fileName = file.name,
                    fileSize = 0L,
                    fileContent = ByteArray(0)
                )
            }

            val fileContent = Files.readAllBytes(file.toPath())
            val configCollection =
                json.decodeFromString<SimulatorConfigCollection>(String(fileContent))

            // Load configurations
            hostConfigs.value = configCollection.hostConfigs
            hsmConfigs.value = configCollection.hsmConfigs
            apduConfigs.value = configCollection.apduConfigs
            posConfigs.value = configCollection.posConfigs

            // Set initial selection
            if (hostConfigs.value.isNotEmpty()) {
                selectedSimulatorType = SimulatorType.HOST
                selectedConfigIndex = 0
            } else if (hsmConfigs.value.isNotEmpty()) {
                selectedSimulatorType = SimulatorType.HSM
                selectedConfigIndex = 0
            } else if (apduConfigs.value.isNotEmpty()) {
                selectedSimulatorType = SimulatorType.APDU
                selectedConfigIndex = 0
            } else if (posConfigs.value.isNotEmpty()) {
                selectedSimulatorType = SimulatorType.POS
                selectedConfigIndex = 0
            }

            save()
            ImportResult.Success(
                fileExtension = file.extension,
                fileName = file.name,
                fileSize = file.length(),
                fileContent = fileContent
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult.Error(
                message = e.message ?: "Unknown error",
                exception = e
            )
        }
    }

    /**
     * Import specific simulator type configurations
     */
    fun importByType(file: File, type: SimulatorType): ImportResult {
        return try {
            val fileContent = Files.readAllBytes(file.toPath())

            when (type) {
                SimulatorType.HOST -> {
                    val configs = json.decodeFromString<List<GatewayConfig>>(String(fileContent))
                    hostConfigs.value = hostConfigs.value + configs
                }

                SimulatorType.HSM -> {
                    val configs =
                        json.decodeFromString<List<HSMSimulatorConfig>>(String(fileContent))
                    hsmConfigs.value = hsmConfigs.value + configs
                }

                SimulatorType.APDU -> {
                    val configs =
                        json.decodeFromString<List<APDUSimulatorConfig>>(String(fileContent))
                    apduConfigs.value = apduConfigs.value + configs
                }

                SimulatorType.POS -> {
                    val configs =
                        json.decodeFromString<List<POSSimulatorConfig>>(String(fileContent))
                    posConfigs.value = posConfigs.value + configs
                }

                else -> {
                    return ImportResult.Error(
                        "Unsupported simulator type: $type",
                        IllegalArgumentException()
                    )
                }
            }

            save()
            ImportResult.Success(
                fileExtension = file.extension,
                fileName = file.name,
                fileSize = file.length(),
                fileContent = fileContent
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult.Error(
                message = e.message ?: "Unknown error",
                exception = e
            )
        }
    }

    /**
     * Get configuration statistics
     */
    fun getStatistics(): ConfigurationStatistics {
        return ConfigurationStatistics(
            totalConfigs = getAllConfigs().size,
            hostConfigCount = hostConfigs.value.size,
            hsmConfigCount = hsmConfigs.value.size,
            apduConfigCount = apduConfigs.value.size,
            posConfigCount = posConfigs.value.size,
            enabledConfigs = getAllConfigs().count { it.enabled },
            lastModified = getAllConfigs().maxOfOrNull { it.modifiedDate } ?: 0L
        )
    }

    fun setComposableWindow(window: ComposeWindow) {
        this.window = window
    }
}

/**
 * Collection wrapper for serialization
 */
@Serializable
data class SimulatorConfigCollection(
    val hostConfigs: List<GatewayConfig> = emptyList(),
    val hsmConfigs: List<HSMSimulatorConfig> = emptyList(),
    val apduConfigs: List<APDUSimulatorConfig> = emptyList(),
    val posConfigs: List<POSSimulatorConfig> = emptyList(),
    val exportedAt: Long,
    val version: String
)

/**
 * Configuration statistics for dashboard/overview
 */
data class ConfigurationStatistics(
    val totalConfigs: Int,
    val hostConfigCount: Int,
    val hsmConfigCount: Int,
    val apduConfigCount: Int,
    val posConfigCount: Int,
    val enabledConfigs: Int,
    val lastModified: Long
)