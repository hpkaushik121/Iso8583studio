package `in`.aicortex.iso8583studio.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.data.model.ConnectionStatus
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.domain.ImportResult
import `in`.aicortex.iso8583studio.domain.utils.ApduUtil
import `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.ProfileStatus
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.Transaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.awt.SystemColor
import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlin.String
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
    val maxSessions: Int = 8,
    val pkcs11Compliance: Boolean = true,
    val fipsLevel: FIPSLevel = FIPSLevel.LEVEL_2,
    val supportedAlgorithms: List<CryptoAlgorithm> = emptyList(),
    val keyStorage: KeyStorageConfig = KeyStorageConfig(),
    val authenticationPolicy: AuthenticationPolicy = AuthenticationPolicy.PASSWORD,
    val auditConfig: AuditConfig = AuditConfig(),
    val performanceSettings: PerformanceSettings = PerformanceSettings(),

    val deviceInfo: HSMDeviceInfo = HSMDeviceInfo(),
    val keyManagementSettings: KeyManagementSettings = KeyManagementSettings(),
    val slotConfiguration: SlotConfiguration = SlotConfiguration(),
    val securitySettings: SecuritySettings = SecuritySettings(),
    val initializationSettings: InitializationSettings = InitializationSettings(),
    val complianceSettings: ComplianceSettings = ComplianceSettings(),
    val cryptoConfig: CryptoConfig = CryptoConfig(),
    val securityPolicies: SecurityPolicies = SecurityPolicies(),

    val vendor: HSMVendor = HSMVendor.THALES,
    val model: String = "",
    val status: HSMStatus = HSMStatus.INACTIVE,
    val profile: HSMProfile = HSMProfile(),
    val network: NetworkConfig = NetworkConfig(),
    val security: SecurityConfig = SecurityConfig(),
    val performance: PerformanceConfig = PerformanceConfig(),
    val keyManagement: KeyManagementConfig = KeyManagementConfig(),
    val advanced: AdvancedConfig = AdvancedConfig(),
    val operatingMode: OperatingMode = OperatingMode.MAINTENANCE
) : SimulatorConfig


/**
 * HSM Vendor enumeration
 */
@Serializable
enum class HSMVendor(val displayName: String, val models: List<String>) {
    THALES("Thales", listOf("payShield 9000", "payShield 10K")),
    SAFENET("SafeNet Luna", listOf("Network Attached", "PCIe", "USB")),
    UTIMACO("Utimaco CryptoServer", listOf("Se", "CP5")),
    FUTUREX("Futurex Excrypt", listOf("KMES", "VirtuCrypt")),
    NCIPHER("nCipher nShield", listOf("Connect", "Solo", "Edge")),
    GENERIC("Generic/Custom HSM", listOf("Custom Model"))
}

/**
 * HSM Status enumeration
 */
@Serializable
enum class HSMStatus(val displayName: String, val color: Color) {
    ACTIVE("Active", Color(0xFF4CAF50)),
    INACTIVE("Inactive", Color(0xFF9E9E9E)),
    ERROR("Error", Color(0xFFF44336)),
    WARNING("Warning", Color(0xFFFF9800)),
    STARTING("Starting", Color(0xFF2196F3)),
    STOPPING("Stopping", Color(0xFFFF5722))
}

/**
 * HSM Profile configuration
 */
@Serializable
data class HSMProfile(
    val firmwareVersion: String = "1.0.0",
    val operatingMode: OperatingMode = OperatingMode.MAINTENANCE,
    val supportedCommands: List<HSMCommand> = emptyList(),
    val capabilities: Set<HSMCapability> = setOf<HSMCapability>()
)


/**
 * Security configuration
 */
@Serializable
data class SecurityConfig(
    val authenticationRequired: Boolean = true,
    val encryptionLevel: EncryptionLevel = EncryptionLevel.HIGH,
    val auditEnabled: Boolean = true,
    val complianceFrameworks: List<String> = listOf("FIPS-140-2")
)

/**
 * Performance configuration
 */
@Serializable
data class PerformanceConfig(
    val maxTPS: Int = 1000,
    val responseTimeTarget: Int = 100,
    val threadPoolSize: Int = 10,
    val enableMetrics: Boolean = true
)

/**
 * Key management configuration
 */
@Serializable
data class KeyManagementConfig(
    val maxKeys: Int = 10000,
    val keyStoreType: String = "PKCS11",
    val supportedAlgorithms: List<String> = listOf("AES", "RSA", "ECC")
)

/**
 * Advanced configuration
 */
@Serializable
data class AdvancedConfig(
    val debugMode: Boolean = false,
    val verboseLogging: Boolean = false,
    val pluginsEnabled: Boolean = false
)

/**
 * Supporting enums and data classes
 */
@Serializable
enum class OperatingMode(val displayName: String) {
    INITIALIZATION("Initialization"),
    OPERATIONAL("Operational"),
    MAINTENANCE("Maintenance"),
    SECURE_TRANSPORT("Secure Transport")
}
@Serializable
enum class ConnectionType { TCP_IP, SERIAL, REST_API, WEBSOCKET }
@Serializable
enum class EncryptionLevel { LOW, MEDIUM, HIGH, MAXIMUM }

@Serializable
enum class HSMCapability(val displayName: String) {
    SYMMETRIC_CRYPTO("Symmetric Cryptography"),
    ASYMMETRIC_CRYPTO("Asymmetric Cryptography"),
    HASH_FUNCTIONS("Hash Functions"),
    RANDOM_NUMBER_GEN("Random Number Generation"),
    KEY_DERIVATION("Key Derivation"),
    DIGITAL_SIGNING("Digital Signing"),
    SSL_TLS_ACCELERATION("SSL/TLS Acceleration"),
    CODE_SIGNING("Code Signing"),
    DATABASE_ENCRYPTION("Database Encryption"),
    PAYMENT_PROCESSING("Payment Processing")
}
@Serializable
data class HSMCommand(
    val code: String,
    val name: String,
    val category: String
)



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
    val tamperResistance: TamperResistanceLevel = TamperResistanceLevel.FIPS_140_2_LEVEL_2,
    val secureMessaging: Boolean = true
)

@Serializable
enum class TamperResistanceLevel(val displayName: String) {
    FIPS_140_2_LEVEL_1("FIPS 140-2 Level 1"),
    FIPS_140_2_LEVEL_2("FIPS 140-2 Level 2"),
    FIPS_140_2_LEVEL_3("FIPS 140-2 Level 3"),
    FIPS_140_2_LEVEL_4("FIPS 140-2 Level 4"),
    COMMON_CRITERIA_EAL4("Common Criteria EAL4+"),
    COMMON_CRITERIA_EAL5("Common Criteria EAL5+")
}

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
data class NetworkConfig(
    val connectionType: ConnectionType = ConnectionType.TCP_IP,
    val ipAddress: String = "127.0.0.1",
    val port: Int = 8080,
    val bindAddress: String = "0.0.0.0",
    val sslTlsConfig: SSLTLSConfig = SSLTLSConfig(),
    val performanceSettings: PerformanceSettings = PerformanceSettings(),
    val protocolSettings: ProtocolSettings = ProtocolSettings(),
    val serialConfig: SerialConfig = SerialConfig(),
    val restApiConfig: RestApiConfig = RestApiConfig(),
    val webSocketConfig: WebSocketConfig = WebSocketConfig()
)



@Serializable
enum class SSLTLSVersion(val displayName: String) {
    TLS_1_0("TLS 1.0"),
    TLS_1_1("TLS 1.1"),
    TLS_1_2("TLS 1.2"),
    TLS_1_3("TLS 1.3"),
    SSL_3_0("SSL 3.0 (Legacy)")
}
@Serializable
enum class CertificateType(val displayName: String) {
    X509("X.509 Certificate"),
    PKCS12("PKCS#12 Bundle"),
    PEM("PEM Format"),
    DER("DER Format"),
    JKS("Java KeyStore")
}
@Serializable
enum class CompressionType(val displayName: String) {
    NONE("None"),
    GZIP("GZIP"),
    DEFLATE("Deflate"),
    LZ4("LZ4"),
    SNAPPY("Snappy")
}
@Serializable
enum class MessageFraming(val displayName: String) {
    LENGTH_PREFIX("Length Prefix"),
    DELIMITER_BASED("Delimiter Based"),
    FIXED_LENGTH("Fixed Length"),
    HTTP_CHUNKED("HTTP Chunked"),
    WEBSOCKET_FRAMES("WebSocket Frames")
}
@Serializable
enum class ProtocolVersion(val displayName: String) {
    HTTP_1_0("HTTP/1.0"),
    HTTP_1_1("HTTP/1.1"),
    HTTP_2_0("HTTP/2.0"),
    HTTP_3_0("HTTP/3.0"),
    WEBSOCKET_13("WebSocket RFC 6455"),
    CUSTOM("Custom Protocol")
}
@Serializable
data class SSLTLSConfig(
    val enabled: Boolean = false,
    val version: SSLTLSVersion = SSLTLSVersion.TLS_1_2,
    val certificateType: CertificateType = CertificateType.X509,
    val certificatePath: String = "",
    val privateKeyPath: String = "",
    val keyStorePassword: String = "",
    val trustStoreEnabled: Boolean = false,
    val trustStorePath: String = "",
    val clientAuthRequired: Boolean = false,
    val cipherSuites: Set<String> = emptySet()
)

@Serializable
data class ProtocolSettings(
    val messageFraming: MessageFraming = MessageFraming.LENGTH_PREFIX,
    val protocolVersion: ProtocolVersion = ProtocolVersion.HTTP_1_1,
    val customHeaders: Map<String, String> = emptyMap(),
    val compressionType: CompressionType = CompressionType.NONE,
    val compressionLevel: Int = 6,
    val messageDelimiter: String = "\n",
    val fixedMessageLength: Int = 1024,
    val lengthFieldSize: Int = 4,
    val lengthFieldOffset: Int = 0
)
@Serializable
data class SerialConfig(
    val portName: String = "COM1",
    val baudRate: Int = 115200,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: String = "NONE",
    val flowControl: String = "NONE"
)


@Serializable
data class RestApiConfig(
    val baseUrl: String = "",
    val apiVersion: String = "v1",
    val authType: String = "Bearer",
    val apiKey: String = "",
    val rateLimitRpm: Int = 1000,
    val retryAttempts: Int = 3
)
@Serializable
data class WebSocketConfig(
    val subProtocols: List<String> = emptyList(),
    val maxFrameSize: Int = 65536,
    val pingIntervalMs: Int = 30000,
    val closeTimeoutMs: Int = 5000
)



/**
 * APDU Simulator Configuration
 */
@Serializable
data class APDUSimulatorConfig(
    override val id: String,
    override val name: String,
    override val description: String = "",
    override val simulatorType: SimulatorType = SimulatorType.APDU,
    override val enabled: Boolean = true,
    override val createdDate: Long = System.currentTimeMillis(),
    override val modifiedDate: Long = System.currentTimeMillis(),
    override val version: String = "1.0",

    // APDU-specific properties
    val cardType: CardType = CardType.EMV_CONTACT,
    val atr: String = "3B9F95801FC78031E073FE211B63004C45544F4E",
    val applications: List<CardApplication> = emptyList(),
    val fileSystem: CardFileSystem = CardFileSystem(),
    val securityDomain: SecurityDomain = SecurityDomain(),
    val scriptCommands: List<APDUScript> = emptyList(),

    val connectionInterface: ConnectionInterface,
    val readerName: String = "",
    val applicationAid: String = "",
    val maxSessionTime: Int = 300,
    val logFileName: String = "card_simulator.log",
    val maxLogSizeInMB: Int = 10,
    val tlvTemplate: Map<String, String> = emptyMap(),
    val apduCommandSet: List<ApduCommand> = emptyList(),
    var emvVersion: String = "EMV 4.3",
    var cardNumber: String = "",
    var expiryDate: String = "", // MM/YY format
    var cvv: String = "",
    var cardholderName: String = "",
    // Status
    var status: ProfileStatus,
    var isTestProfile: Boolean = false,
    var inUse: Boolean = false,
    // Interface Config
    var contactEnabled: Boolean = true,
    var contactlessEnabled: Boolean = true,
    var magstripeEnabled: Boolean = false,
    val initialState: String = "Active",
    val pinAttemptsRemaining: String = "3",
    val blockOnPinExhaustion: Boolean = true,
    val blockOnTransactionLimit: Boolean = false,
) : SimulatorConfig

enum class ConnectionInterface {
    PC_SC,     // PC/SC smart card readers
    NFC,       // NFC communication
    MOCK,      // Mock/simulated interface
    USB        // USB card readers
}

@Serializable
// Supporting data classes
data class ApduCommand(
    val cla: Byte,
    val ins: Byte,
    val p1: Byte,
    val p2: Byte,
    val lc: Int,
    val data: ByteArray,
    val le: Int,
    val raw: ByteArray
) {
    fun toHexString(): String = ApduUtil.bytesToHexString(raw)

    fun getInstructionName(): String = ApduUtil.getInstructionDescription(ins)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApduCommand

        if (cla != other.cla) return false
        if (ins != other.ins) return false
        if (p1 != other.p1) return false
        if (p2 != other.p2) return false
        if (lc != other.lc) return false
        if (!data.contentEquals(other.data)) return false
        if (le != other.le) return false
        if (!raw.contentEquals(other.raw)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cla.toInt()
        result = 31 * result + ins.toInt()
        result = 31 * result + p1.toInt()
        result = 31 * result + p2.toInt()
        result = 31 * result + lc
        result = 31 * result + data.contentHashCode()
        result = 31 * result + le
        result = 31 * result + raw.contentHashCode()
        return result
    }
}



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
    var terminalCapabilities: Set<String> = setOf(
        "Offline transaction processing",
        "Void and refund processing"
    ),
    var transactionLimits: String = "Standard Retail Limits",
    // Security
    var encryptionSecurity: Set<String> = setOf(
        "End-to-end encryption (E2EE)",
        "PCI DSS compliance level"
    ),
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
enum class AuthenticationPolicy(val displayName: String) {
    PASSWORD("Password Authentication"),
    SMART_CARD("Smart Card"),
    BIOMETRIC("Biometric"),
    MULTI_FACTOR("Multi-Factor Authentication")
}


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
    EMV_CONTACT,
    EMV_CONTACTLESS,
    MIFARE_CLASSIC,
    MIFARE_DESFIRE,
    JAVA_CARD,
    CUSTOM,
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
    val ecrConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),
    val atmConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),
    val cardConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),
    val switchConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),
    val acquirerConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),
    val issuerConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),

    // General state
    var resultDialogInterface: ResultDialogInterface? = null,
    var selectedConfigIndex: MutableState<MutableMap<SimulatorType, Int>> = mutableStateOf(mutableMapOf()),
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
        SimulatorType.values().forEach {
                type -> selectedConfigIndex.value[type] = 0
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
    fun currentConfig(simulatorType: SimulatorType): SimulatorConfig? {
        val configs = getConfigsByType(simulatorType)
        return if (selectedConfigIndex.value[simulatorType]!!>= 0 && selectedConfigIndex.value[simulatorType]!!< configs.size) {
            configs[selectedConfigIndex.value[simulatorType]!!]
        } else if (configs.isNotEmpty()) {
            selectedConfigIndex.value[simulatorType] = 0
            configs[0]
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
                selectedConfigIndex.value[config.simulatorType] = hostConfigs.value.size - 1
            }

            is HSMSimulatorConfig -> {
                hsmConfigs.value = hsmConfigs.value + config
                selectedConfigIndex.value[config.simulatorType] = hsmConfigs.value.size - 1
            }

            is APDUSimulatorConfig -> {
                apduConfigs.value = apduConfigs.value + config
                selectedConfigIndex.value[config.simulatorType] = apduConfigs.value.size - 1
            }

            is POSSimulatorConfig -> {
                posConfigs.value = posConfigs.value + config
                selectedConfigIndex.value[config.simulatorType] = posConfigs.value.size - 1
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
        val type = getConfigType(configId)
        when(type){
            SimulatorType.HOST -> {
                // Check HOST configs
                val hostIndex = hostConfigs.value.indexOfFirst { it.id == configId }
                if (hostIndex >= 0) {
                    hostConfigs.value = hostConfigs.value.filter { it.id != configId }
                    selectedConfigIndex.value[type] = if (hostConfigs.value.isNotEmpty()) {
                        minOf(selectedConfigIndex.value[type]!!, hostConfigs.value.size - 1)
                    } else -1
                    found = true
                }
            }
            SimulatorType.HSM -> {
                // Check HSM configs
                val hsmIndex = hsmConfigs.value.indexOfFirst { it.id == configId }
                if (hsmIndex >= 0) {
                    hsmConfigs.value = hsmConfigs.value.filter { it.id != configId }
                    selectedConfigIndex.value[type] = if (hsmConfigs.value.isNotEmpty()) {
                        minOf(selectedConfigIndex.value[type]!!, hsmConfigs.value.size - 1)
                    } else -1
                    found = true
                }
            }
            SimulatorType.APDU -> {

                // Check APDU configs
                val apduIndex = apduConfigs.value.indexOfFirst { it.id == configId }
                if (apduIndex >= 0) {
                    apduConfigs.value = apduConfigs.value.filter { it.id != configId }
                    selectedConfigIndex.value[type] = if (apduConfigs.value.isNotEmpty()) {
                        minOf(selectedConfigIndex.value[type]!!, apduConfigs.value.size - 1)
                    } else -1
                    found = true
                }
            }
            SimulatorType.POS -> {
                // Check POS configs
                val posIndex = posConfigs.value.indexOfFirst { it.id == configId }
                if (posIndex >= 0) {
                    posConfigs.value = posConfigs.value.filter { it.id != configId }
                    selectedConfigIndex.value[type] = if (posConfigs.value.isNotEmpty()) {
                        minOf(selectedConfigIndex.value[type]!!, posConfigs.value.size - 1)
                    } else -1
                    found = true
                }

            }
            SimulatorType.ECR -> TODO()
            SimulatorType.ATM -> TODO()
            SimulatorType.CARD -> TODO()
            SimulatorType.SWITCH -> TODO()
            SimulatorType.ACQUIRER -> TODO()
            SimulatorType.ISSUER -> TODO()
            null -> TODO()
        }


        if (found) {
            save()
        }

        return found
    }

    /**
     * Delete the currently selected configuration
     */
    fun deleteCurrentConfig(simulatorType: SimulatorType): Boolean {
        return currentConfig(simulatorType)?.let { config ->
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
        val configs = getConfigsByType(config.simulatorType)
        selectedConfigIndex.value[config.simulatorType] = configs.indexOfFirst { it.id == configId }

        return selectedConfigIndex.value[config.simulatorType]!! >= 0
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
                ecrConfigs = ecrConfigs.value,
                atmConfigs = atmConfigs.value,
                cardConfigs = cardConfigs.value,
                switchConfigs = switchConfigs.value,
                issuerConfigs = issuerConfigs.value,
                acquirerConfigs = acquirerConfigs.value,
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
                ecrConfigs = ecrConfigs.value,
                atmConfigs = atmConfigs.value,
                cardConfigs = cardConfigs.value,
                switchConfigs = switchConfigs.value,
                acquirerConfigs = acquirerConfigs.value,
                issuerConfigs = issuerConfigs.value,
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
            SimulatorType.values().forEach {
                when(it){
                    SimulatorType.HOST -> {
                        hostConfigs.value = configCollection.hostConfigs
                    }
                    SimulatorType.HSM -> {
                        hsmConfigs.value = configCollection.hsmConfigs
                    }
                    SimulatorType.APDU -> {
                        apduConfigs.value = configCollection.apduConfigs
                    }
                    SimulatorType.POS -> {
                        posConfigs.value = configCollection.posConfigs
                    }

                    SimulatorType.ECR -> {
                        ecrConfigs.value = configCollection.ecrConfigs
                    }
                    SimulatorType.ATM -> {
                        atmConfigs.value = configCollection.atmConfigs
                    }
                    SimulatorType.CARD -> {
                        cardConfigs.value = configCollection.cardConfigs
                    }
                    SimulatorType.SWITCH -> {
                        switchConfigs.value = configCollection.switchConfigs
                    }
                    SimulatorType.ACQUIRER -> {
                        acquirerConfigs.value = configCollection.acquirerConfigs
                    }
                    SimulatorType.ISSUER -> {
                        issuerConfigs.value = configCollection.issuerConfigs
                    }
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
    val hostConfigs: List<GatewayConfig> ,
    val hsmConfigs: List<HSMSimulatorConfig> ,
    val apduConfigs: List<APDUSimulatorConfig> ,
    val posConfigs: List<POSSimulatorConfig> ,
    val ecrConfigs: List<String> ,
    val atmConfigs: List<String> ,
    val cardConfigs: List<String> ,
    val switchConfigs: List<String> ,
    val acquirerConfigs: List<String> ,
    val issuerConfigs: List<String> ,
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