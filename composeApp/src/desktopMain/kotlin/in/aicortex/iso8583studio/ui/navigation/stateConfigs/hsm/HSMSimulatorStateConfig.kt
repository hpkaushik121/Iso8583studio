package `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Token
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import kotlinx.serialization.Serializable



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
    val maxSessions: Int = 8,
    val authenticationPolicy: AuthenticationPolicy = AuthenticationPolicy.PASSWORD,

    val deviceInfo: HSMDeviceInfo = HSMDeviceInfo(),
    val slotConfiguration: SlotConfiguration = SlotConfiguration(),
    val securitySettings: SecuritySettings = SecuritySettings(),

    val vendor: HSMVendor = HSMVendor.THALES,
    val model: String = "",
    var simulatedCommandsToSource:  List<HsmCommand> =emptyList(),
    val network: NetworkConfig = NetworkConfig(),
    val security: SecurityConfiguration = SecurityConfiguration(),
    val keyManagement: KeyManagementConfiguration = KeyManagementConfiguration(),
    val advanced: AdvancedOptionsConfiguration = AdvancedOptionsConfiguration(),
    val operatingMode: OperatingMode = OperatingMode.MAINTENANCE
) : SimulatorConfig


// HSM Data Models
@Serializable
data class HsmCommand(
    val id: String,
    val commandCode: String,
    val commandName: String,
    val description: String,
    val parameters: MutableList<HsmParameter>? = null,
    val keyMatching: KeyMatching = KeyMatching(),
    val responseMapping: HsmResponseMapping = HsmResponseMapping()
)

@Serializable
data class HsmParameter(
    val name: String,
    val type: HsmParameterType,
    val required: Boolean = false,
    val description: String = "",
    val value: String = "",
    val maxLength: Int = 0,
    val format: HsmDataFormat = HsmDataFormat.HEX
)

@Serializable
data class KeyMatching(
    val enabled: Boolean = false,
    val keyIdMatching: KeyIdMatching = KeyIdMatching(),
    val sessionMatching: List<SessionMatcher> = emptyList(),
    val responseTemplate: String = "",
    val priority: Int = 0
)

@Serializable
data class KeyIdMatching(
    val enabled: Boolean = false,
    val keyId: String = "",
    val keyType: String = "",
    val exactMatch: Boolean = true
)

@Serializable
data class SessionMatcher(
    val attribute: String,
    val value: String,
    val operator: HsmMatchOperator = HsmMatchOperator.EQUALS
)

@Serializable
data class HsmResponseMapping(
    val enabled: Boolean = false,
    val responseFields: List<HsmResponseField> = emptyList(),
)

@Serializable
data class HsmResponseField(
    val value: String,
    val targetParameter: String? = null,
    val targetHeader: String? = null,
)

@Serializable
enum class HsmParameterType(val displayName: String) {
    KEY_ID("Key Identifier"),
    KEY_DATA("Key Data"),
    DATA_BLOCK("Data Block"),
    PIN_BLOCK("PIN Block"),
    MAC_DATA("MAC Data"),
    CERTIFICATE("Certificate"),
    SESSION_ID("Session ID"),
    COMMAND_DATA("Command Data"),
    STATUS_CODE("Status Code"),
    ERROR_CODE("Error Code"),
    TIMESTAMP("Timestamp"),
    RANDOM_DATA("Random Data")
}

@Serializable
enum class HsmDataFormat(val displayName: String) {
    HEX("Hexadecimal"),
    BASE64("Base64"),
    ASCII("ASCII"),
    BINARY("Binary"),
    DER("DER Encoded"),
    PEM("PEM Format")
}

@Serializable
enum class HsmMatchOperator(val displayName: String, val symbol: String) {
    EQUALS("Equals", "=="),
    CONTAINS("Contains", "contains"),
    STARTS_WITH("Starts With", "startsWith"),
    ENDS_WITH("Ends With", "endsWith"),
    REGEX("Regex", "regex"),
    NOT_EQUALS("Not Equals", "!="),
    LENGTH_EQUALS("Length Equals", "length=="),
    LENGTH_GREATER("Length Greater", "length>")
}


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
 * Security configuration
 */
@Serializable
data class SecurityConfiguration(
    val authenticationConfig: AuthenticationConfig = AuthenticationConfig(),
    val roleBasedAccessConfig: RoleBasedAccessConfig = RoleBasedAccessConfig(),
    val encryptionConfig: EncryptionConfig = EncryptionConfig(),
    val sessionManagement: SessionManagementConfig = SessionManagementConfig(),
    val auditConfig: AuditConfig = AuditConfig()
)


// Security Configuration Data Classes
enum class AuthenticationMethod(val displayName: String, val icon: ImageVector) {
    PASSWORD("Password Authentication", Icons.Default.Lock),
    SMART_CARD("Smart Card", Icons.Default.CreditCard),
    BIOMETRIC("Biometric Authentication", Icons.Default.Fingerprint),
    CERTIFICATE("Certificate-based", Icons.Default.Notes),
    TOKEN("Hardware Token", Icons.Default.Token),
    OAUTH2("OAuth 2.0", Icons.Default.Security),
    SAML("SAML Authentication", Icons.Default.AccountCircle),
    LDAP("LDAP/Active Directory", Icons.Default.Group)
}

@Serializable
enum class MFAMethod(val displayName: String) {
    SMS("SMS Code"),
    EMAIL("Email Code"),
    TOTP("Time-based OTP (TOTP)"),
    HOTP("HMAC-based OTP (HOTP)"),
    PUSH_NOTIFICATION("Push Notification"),
    HARDWARE_TOKEN("Hardware Token"),
    BACKUP_CODES("Backup Codes")
}

@Serializable
enum class UserRole(val displayName: String, val permissions: List<String>) {
    ADMIN("Administrator", listOf("All Permissions")),
    SECURITY_OFFICER("Security Officer", listOf("User Management", "Security Settings", "Audit Access")),
    CRYPTO_OFFICER("Crypto Officer", listOf("Key Management", "Crypto Operations", "Certificate Management")),
    CRYPTO_USER("Crypto User", listOf("Basic Crypto Operations", "Key Usage")),
    AUDITOR("Auditor", listOf("Read-only Access", "Audit Log Access")),
    OPERATOR("Operator", listOf("Transaction Processing", "Basic Operations")),
    GUEST("Guest", listOf("Read-only Access"))
}

@Serializable
enum class CipherSuite(val displayName: String, val strength: String) {
    TLS_AES_256_GCM_SHA384("TLS_AES_256_GCM_SHA384", "High"),
    TLS_AES_128_GCM_SHA256("TLS_AES_128_GCM_SHA256", "High"),
    TLS_CHACHA20_POLY1305_SHA256("TLS_CHACHA20_POLY1305_SHA256", "High"),
    TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "High"),
    TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "High"),
    TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305("TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305", "High"),
    TLS_RSA_WITH_AES_256_GCM_SHA384("TLS_RSA_WITH_AES_256_GCM_SHA384", "Medium"),
    TLS_RSA_WITH_AES_128_GCM_SHA256("TLS_RSA_WITH_AES_128_GCM_SHA256", "Medium")
}

@Serializable
enum class KeyExchangeProtocol(val displayName: String) {
    ECDHE("Elliptic Curve Diffie-Hellman Ephemeral"),
    DHE("Diffie-Hellman Ephemeral"),
    RSA("RSA Key Exchange"),
    ECDH("Elliptic Curve Diffie-Hellman"),
    DH("Diffie-Hellman"),
    PSK("Pre-Shared Key")
}

@Serializable
enum class ComplianceFramework(val displayName: String, val description: String) {
    FIPS_140_2("FIPS 140-2", "Federal Information Processing Standard"),
    COMMON_CRITERIA("Common Criteria", "International security evaluation standard"),
    PCI_DSS("PCI DSS", "Payment Card Industry Data Security Standard"),
    SOX("Sarbanes-Oxley", "Financial reporting compliance"),
    GDPR("GDPR", "General Data Protection Regulation"),
    HIPAA("HIPAA", "Health Insurance Portability and Accountability Act"),
    ISO_27001("ISO 27001", "Information Security Management"),
    NIST_CYBERSECURITY("NIST Cybersecurity Framework", "Risk-based cybersecurity guidance")
}

@Serializable
enum class AuditEventType(val displayName: String) {
    AUTHENTICATION("Authentication Events"),
    AUTHORIZATION("Authorization Events"),
    KEY_MANAGEMENT("Key Management Operations"),
    CRYPTOGRAPHIC_OPERATIONS("Cryptographic Operations"),
    CONFIGURATION_CHANGES("Configuration Changes"),
    ADMINISTRATIVE_ACTIONS("Administrative Actions"),
    DATA_ACCESS("Data Access Events"),
    SECURITY_VIOLATIONS("Security Violations"),
    SYSTEM_EVENTS("System Events"),
    NETWORK_EVENTS("Network Events")
}

@Serializable
enum class AuditLogLevel(val displayName: String) {
    ERROR("Error Events Only"),
    WARN("Warning and Error Events"),
    INFO("Informational Events"),
    DEBUG("Debug Events"),
    TRACE("All Events (Trace)")
}

@Serializable
enum class RetentionPeriod(val displayName: String, val days: Int) {
    ONE_MONTH("1 Month", 30),
    THREE_MONTHS("3 Months", 90),
    SIX_MONTHS("6 Months", 180),
    ONE_YEAR("1 Year", 365),
    TWO_YEARS("2 Years", 730),
    FIVE_YEARS("5 Years", 1825),
    SEVEN_YEARS("7 Years", 2555),
    TEN_YEARS("10 Years", 3650),
    PERMANENT("Permanent", -1)
}

@Serializable
data class AuthenticationConfig(
    val primaryMethod: AuthenticationMethod = AuthenticationMethod.PASSWORD,
    val enabledMethods: Set<AuthenticationMethod> = setOf(AuthenticationMethod.PASSWORD),
    val passwordPolicy: PasswordPolicy = PasswordPolicy(),
    val mfaEnabled: Boolean = false,
    val mfaMethods: Set<MFAMethod> = emptySet(),
    val sessionTimeout: Int = 3600, // seconds
    val maxConcurrentSessions: Int = 5,
    val accountLockoutEnabled: Boolean = true,
    val maxFailedAttempts: Int = 3,
    val lockoutDuration: Int = 900 // seconds
)

@Serializable
data class PasswordPolicy(
    val minLength: Int = 8,
    val maxLength: Int = 64,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireNumbers: Boolean = true,
    val requireSpecialChars: Boolean = true,
    val historySize: Int = 5,
    val maxAge: Int = 90 // days
)

@Serializable
data class RoleBasedAccessConfig(
    val rbacEnabled: Boolean = true,
    val defaultRole: UserRole = UserRole.GUEST,
    val roleHierarchy: Boolean = true,
    val inheritanceEnabled: Boolean = true,
    val sessionValidation: Boolean = true,
    val privilegeEscalation: Boolean = false
)

@Serializable
data class EncryptionConfig(
    val supportedCipherSuites: Set<CipherSuite> = setOf(
        CipherSuite.TLS_AES_256_GCM_SHA384,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
    ),
    val preferredCipherSuite: CipherSuite = CipherSuite.TLS_AES_256_GCM_SHA384,
    val keyExchangeProtocols: Set<KeyExchangeProtocol> = setOf(KeyExchangeProtocol.ECDHE),
    val preferredKeyExchange: KeyExchangeProtocol = KeyExchangeProtocol.ECDHE,
    val trustedCAs: List<String> = emptyList(),
    val hardwareSecurityEnabled: Boolean = true,
    val hsmIntegration: Boolean = false,
    val certificateValidation: Boolean = true,
    val ocspValidation: Boolean = true,
    val crlValidation: Boolean = true
)

@Serializable
data class SessionManagementConfig(
    val sessionTimeout: Int = 3600, // seconds
    val idleTimeout: Int = 1800, // seconds
    val maxConcurrentSessions: Int = 5,
    val sessionTokenExpiry: Int = 7200, // seconds
    val requireReauthentication: Boolean = true,
    val sessionCookieSecure: Boolean = true,
    val sessionCookieHttpOnly: Boolean = true,
    val enableSessionMonitoring: Boolean = true
)

@Serializable
data class AuditConfig(
    val auditEnabled: Boolean = true,
    val enabledEventTypes: Set<AuditEventType> = AuditEventType.values().toSet(),
    val logLevel: AuditLogLevel = AuditLogLevel.INFO,
    val retentionPeriod: RetentionPeriod = RetentionPeriod.SEVEN_YEARS,
    val complianceFrameworks: Set<ComplianceFramework> = emptySet(),
    val realTimeMonitoring: Boolean = true,
    val logEncryption: Boolean = true,
    val logSigning: Boolean = true,
    val tamperProtection: Boolean = true,
    val logPath: String = "./security_audit.log",
    val maxLogSize: Long = 1073741824L, // 1GB
    val alertOnViolations: Boolean = true,
    val syslogForwarding: Boolean = false
)


// Key Management Data Classes
@Serializable
enum class KeyStorageType(val displayName: String, val description: String) {
    PKCS11("PKCS#11", "Standard cryptographic token interface"),
    HARDWARE_HSM("Hardware HSM", "Dedicated hardware security module"),
    SOFTWARE_HSM("Software HSM", "Software-based secure key storage"),
    FILE_BASED("File-based", "Encrypted file system storage"),
    DATABASE("Database", "Encrypted database storage"),
    CLOUD_HSM("Cloud HSM", "Cloud-based hardware security module"),
    TPM("TPM 2.0", "Trusted Platform Module storage"),
    SECURE_ENCLAVE("Secure Enclave", "Hardware secure enclave storage")
}

@Serializable
enum class KeyHierarchyLevel(val displayName: String, val level: Int) {
    ROOT("Root Key", 0),
    MASTER("Master Key", 1),
    KEY_ENCRYPTION("Key Encryption Key (KEK)", 2),
    DATA_ENCRYPTION("Data Encryption Key (DEK)", 3),
    SESSION("Session Key", 4),
    TRANSACTION("Transaction Key", 5)
}

@Serializable
enum class CryptographicAlgorithm(
    val displayName: String,
    val type: AlgorithmType,
    val keySizes: List<Int>,
    val description: String
) {
    AES("AES", AlgorithmType.SYMMETRIC, listOf(128, 192, 256), "Advanced Encryption Standard"),
    DES("DES", AlgorithmType.SYMMETRIC, listOf(56), "Data Encryption Standard (Legacy)"),
    TRIPLE_DES(
        "3DES",
        AlgorithmType.SYMMETRIC,
        listOf(112, 168),
        "Triple Data Encryption Standard"
    ),
    RSA("RSA", AlgorithmType.ASYMMETRIC, listOf(1024, 2048, 3072, 4096), "Rivest-Shamir-Adleman"),
    ECC_P256("ECC P-256", AlgorithmType.ASYMMETRIC, listOf(256), "Elliptic Curve P-256"),
    ECC_P384("ECC P-384", AlgorithmType.ASYMMETRIC, listOf(384), "Elliptic Curve P-384"),
    ECC_P521("ECC P-521", AlgorithmType.ASYMMETRIC, listOf(521), "Elliptic Curve P-521"),
    ED25519("Ed25519", AlgorithmType.ASYMMETRIC, listOf(256), "Edwards-curve Digital Signature"),
    X25519("X25519", AlgorithmType.ASYMMETRIC, listOf(256), "Curve25519 Key Exchange"),
    HMAC_SHA256(
        "HMAC-SHA256",
        AlgorithmType.MAC,
        listOf(256),
        "Hash-based Message Authentication Code"
    ),
    HMAC_SHA512(
        "HMAC-SHA512",
        AlgorithmType.MAC,
        listOf(512),
        "Hash-based Message Authentication Code"
    )
}

@Serializable
enum class AlgorithmType(val displayName: String) {
    SYMMETRIC("Symmetric"),
    ASYMMETRIC("Asymmetric"),
    MAC("Message Authentication Code"),
    HASH("Hash Function")
}

@Serializable
enum class RandomNumberGenerator(val displayName: String, val description: String) {
    HARDWARE_RNG("Hardware RNG", "True random number generator from hardware entropy"),
    DRBG_CTR("DRBG-CTR-AES", "Deterministic Random Bit Generator using AES Counter mode"),
    DRBG_HASH("DRBG-Hash", "Hash-based Deterministic Random Bit Generator"),
    DRBG_HMAC("DRBG-HMAC", "HMAC-based Deterministic Random Bit Generator"),
    FORTUNA("Fortuna", "Cryptographically secure pseudorandom number generator"),
    YARROW("Yarrow", "Cryptographically secure pseudorandom number generator"),
    OS_RANDOM("OS Random", "Operating system provided random number generator")
}

@Serializable
enum class KeyGenerationPolicy(val displayName: String) {
    MANUAL("Manual Generation"),
    SCHEDULED("Scheduled Generation"),
    THRESHOLD_BASED("Threshold-based"),
    EVENT_DRIVEN("Event-driven"),
    CONTINUOUS("Continuous")
}

@Serializable
enum class KeyRotationSchedule(val displayName: String, val days: Int) {
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30),
    QUARTERLY("Quarterly", 90),
    SEMI_ANNUALLY("Semi-annually", 180),
    ANNUALLY("Annually", 365),
    BIANNUALLY("Biannually", 730),
    CUSTOM("Custom", 0)
}

@Serializable
enum class KeyArchivalPolicy(val displayName: String) {
    NO_ARCHIVAL("No Archival"),
    IMMEDIATE("Immediate Archival"),
    DELAYED("Delayed Archival"),
    CONDITIONAL("Conditional Archival"),
    COMPLIANCE_DRIVEN("Compliance-driven")
}

@Serializable
enum class KeyDestructionPolicy(val displayName: String) {
    IMMEDIATE("Immediate"),
    SCHEDULED("Scheduled"),
    MANUAL("Manual"),
    COMPLIANCE_BASED("Compliance-based"),
    NEVER("Never")
}

@Serializable
data class KeyStoreConfig(
    val storageType: KeyStorageType = KeyStorageType.HARDWARE_HSM,
    val keyHierarchy: Map<KeyHierarchyLevel, Boolean> = mapOf(
        KeyHierarchyLevel.ROOT to true,
        KeyHierarchyLevel.MASTER to true,
        KeyHierarchyLevel.KEY_ENCRYPTION to true
    ),
    val masterKeyConfig: MasterKeyConfig = MasterKeyConfig(),
    val backupSettings: KeyBackupSettings = KeyBackupSettings(),
    val maxKeys: Int = 10000,
    val keySlots: Int = 1000,
    val enableHardwareProtection: Boolean = true,
    val enableKeyWrapping: Boolean = true,
    val keyStorePassword: String = "",
    val keyStorePath: String = "./keystore"
)

@Serializable
data class MasterKeyConfig(
    val algorithm: CryptographicAlgorithm = CryptographicAlgorithm.AES,
    val keySize: Int = 256,
    val enableSplitKnowledge: Boolean = false,
    val requiredShares: Int = 3,
    val totalShares: Int = 5,
    val enableDualControl: Boolean = true,
    val autoGenerate: Boolean = false,
    val rotationEnabled: Boolean = true,
    val rotationPeriod: KeyRotationSchedule = KeyRotationSchedule.ANNUALLY
)

@Serializable
data class KeyBackupSettings(
    val enableBackup: Boolean = true,
    val backupLocation: String = "./backup",
    val encryptBackups: Boolean = true,
    val backupFrequency: KeyRotationSchedule = KeyRotationSchedule.DAILY,
    val retainBackups: Int = 30, // days
    val verifyBackups: Boolean = true,
    val offSiteBackup: Boolean = false,
    val redundantBackups: Int = 3
)

@Serializable
data class CryptographicConfig(
    val supportedAlgorithms: Set<CryptographicAlgorithm> = setOf(
        CryptographicAlgorithm.AES,
        CryptographicAlgorithm.RSA,
        CryptographicAlgorithm.ECC_P256
    ),
    val defaultSymmetricAlgorithm: CryptographicAlgorithm = CryptographicAlgorithm.AES,
    val defaultAsymmetricAlgorithm: CryptographicAlgorithm = CryptographicAlgorithm.RSA,
    val defaultSymmetricKeySize: Int = 256,
    val defaultAsymmetricKeySize: Int = 2048,
    val randomNumberGenerator: RandomNumberGenerator = RandomNumberGenerator.HARDWARE_RNG,
    val enableHardwareAcceleration: Boolean = true,
    val validateKeyStrength: Boolean = true,
    val enforceFipsCompliance: Boolean = false,
    val allowWeakAlgorithms: Boolean = false
)

@Serializable
data class KeyLifecycleConfig(
    val generationPolicy: KeyGenerationPolicy = KeyGenerationPolicy.MANUAL,
    val rotationSchedule: KeyRotationSchedule = KeyRotationSchedule.ANNUALLY,
    val customRotationDays: Int = 90,
    val archivalPolicy: KeyArchivalPolicy = KeyArchivalPolicy.DELAYED,
    val archivalGracePeriod: Int = 30, // days
    val destructionPolicy: KeyDestructionPolicy = KeyDestructionPolicy.COMPLIANCE_BASED,
    val destructionGracePeriod: Int = 90, // days
    val enableAutomaticRotation: Boolean = false,
    val enableKeyEscrow: Boolean = false,
    val enableKeyRecovery: Boolean = true,
    val auditKeyOperations: Boolean = true
)

@Serializable
data class KeyManagementConfiguration(
    val keyStoreConfig: KeyStoreConfig = KeyStoreConfig(),
    val cryptographicConfig: CryptographicConfig = CryptographicConfig(),
    val keyLifecycleConfig: KeyLifecycleConfig = KeyLifecycleConfig()
)


// Advanced Options Data Classes
@Serializable
enum class ResponseDelayType(val displayName: String, val description: String) {
    NONE("No Delay", "Respond immediately without any delay"),
    FIXED("Fixed Delay", "Fixed delay for all responses"),
    RANDOM("Random Delay", "Random delay within specified range"),
    PROGRESSIVE("Progressive Delay", "Increasing delay based on load"),
    REALISTIC("Realistic Simulation", "Realistic network and processing delays"),
    CUSTOM("Custom Pattern", "Custom delay patterns based on message type")
}

@Serializable
enum class ErrorInjectionType(val displayName: String, val description: String) {
    NETWORK_TIMEOUT("Network Timeout", "Simulate network timeout errors"),
    CONNECTION_FAILURE("Connection Failure", "Simulate connection failures"),
    MALFORMED_RESPONSE("Malformed Response", "Send invalid or corrupted responses"),
    AUTHENTICATION_FAILURE("Authentication Failure", "Simulate authentication errors"),
    INSUFFICIENT_FUNDS("Insufficient Funds", "Simulate transaction decline scenarios"),
    SYSTEM_ERROR("System Error", "Simulate internal system errors"),
    CARD_BLOCKED("Card Blocked", "Simulate blocked card scenarios"),
    INVALID_PIN("Invalid PIN", "Simulate PIN verification failures"),
    EXPIRED_CARD("Expired Card", "Simulate expired card scenarios"),
    CUSTOM_ERROR("Custom Error", "User-defined error scenarios")
}

@Serializable
enum class LoadTestingPattern(val displayName: String) {
    CONSTANT("Constant Load"),
    RAMP_UP("Ramp Up"),
    SPIKE("Spike Testing"),
    STRESS("Stress Testing"),
    VOLUME("Volume Testing"),
    ENDURANCE("Endurance Testing"),
    BURST("Burst Pattern")
}

@Serializable
enum class FailureSimulationType(val displayName: String) {
    HARDWARE_FAILURE("Hardware Failure"),
    SOFTWARE_CRASH("Software Crash"),
    MEMORY_EXHAUSTION("Memory Exhaustion"),
    DISK_FULL("Disk Full"),
    NETWORK_PARTITION("Network Partition"),
    POWER_FAILURE("Power Failure"),
    CERTIFICATE_EXPIRY("Certificate Expiry"),
    KEY_CORRUPTION("Key Corruption")
}

@Serializable
enum class PluginType(val displayName: String, val description: String) {
    CRYPTO_PROVIDER("Crypto Provider", "Custom cryptographic algorithm implementations"),
    MESSAGE_PROCESSOR("Message Processor", "Custom message processing logic"),
    AUTHENTICATION("Authentication", "Custom authentication mechanisms"),
    AUDIT_LOGGER("Audit Logger", "Custom audit logging implementations"),
    DATA_CONVERTER("Data Converter", "Custom data format converters"),
    PROTOCOL_HANDLER("Protocol Handler", "Custom protocol implementations"),
    MONITORING("Monitoring", "Custom monitoring and metrics"),
    VALIDATION("Validation", "Custom validation rules")
}

@Serializable
enum class EventHookType(val displayName: String,val description: String) {
    PRE_TRANSACTION("Pre-Transaction", "Before transaction processing"),
    POST_TRANSACTION("Post-Transaction", "After transaction processing"),
    PRE_AUTHENTICATION("Pre-Authentication", "Before authentication"),
    POST_AUTHENTICATION("Post-Authentication", "After authentication"),
    KEY_GENERATION("Key Generation", "During key generation"),
    ERROR_OCCURRED("Error Occurred", "When errors occur"),
    CONNECTION_ESTABLISHED("Connection Established", "When connections are made"),
    CONNECTION_CLOSED("Connection Closed", "When connections are closed"),
    SYSTEM_STARTUP("System Startup", "During system initialization"),
    SYSTEM_SHUTDOWN("System Shutdown", "During system shutdown")
}

@Serializable
enum class ExternalIntegrationType(val displayName: String, val icon: ImageVector) {
    REST_API("REST API", Icons.Default.Http),
    SOAP_WEBSERVICE("SOAP Web Service", Icons.Default.Cloud),
    DATABASE("Database", Icons.Default.Storage),
    MESSAGE_QUEUE("Message Queue", Icons.Default.Queue),
    FILE_SYSTEM("File System", Icons.Default.Folder),
    EMAIL_SERVICE("Email Service", Icons.Default.Email),
    SMS_GATEWAY("SMS Gateway", Icons.Default.Sms),
    LDAP_DIRECTORY("LDAP Directory", Icons.Default.Group),
    FTP_SERVER("FTP Server", Icons.Default.CloudUpload),
    WEBHOOK("Webhook", Icons.Default.Webhook)
}

@Serializable
enum class LogLevel(val displayName: String, val severity: Int) {
    TRACE("Trace", 0),
    DEBUG("Debug", 1),
    INFO("Info", 2),
    WARN("Warning", 3),
    ERROR("Error", 4),
    FATAL("Fatal", 5)
}

@Serializable
enum class TestDataType(val displayName: String, val description: String) {
    CARD_NUMBERS("Card Numbers", "Generate valid credit/debit card numbers"),
    EXPIRY_DATES("Expiry Dates", "Generate realistic expiry dates"),
    CVV_CODES("CVV Codes", "Generate valid CVV/CVC codes"),
    MERCHANT_DATA("Merchant Data", "Generate merchant information"),
    TRANSACTION_AMOUNTS("Transaction Amounts", "Generate realistic transaction amounts"),
    CUSTOMER_DATA("Customer Data", "Generate customer profiles"),
    CERTIFICATES("Certificates", "Generate test certificates"),
    CRYPTOGRAPHIC_KEYS("Cryptographic Keys", "Generate test keys")
}

@Serializable
enum class MockResponseType(val displayName: String,val description: String) {
    APPROVAL("Approval", "Approved transaction responses"),
    DECLINE("Decline", "Declined transaction responses"),
    REFERRAL("Referral", "Referral required responses"),
    PICKUP("Card Pickup", "Card pickup responses"),
    RETRY("Retry", "Retry transaction responses"),
    ERROR("Error", "Error responses"),
    TIMEOUT("Timeout", "Timeout responses"),
    CUSTOM("Custom", "User-defined responses")
}

@Serializable
data class ResponseDelayConfig(
    val delayType: ResponseDelayType = ResponseDelayType.NONE,
    val fixedDelayMs: Int = 100,
    val minDelayMs: Int = 50,
    val maxDelayMs: Int = 500,
    val networkLatencyMs: Int = 20,
    val processingDelayMs: Int = 80,
    val enableJitter: Boolean = false,
    val jitterPercentage: Int = 10
)

@Serializable
data class ErrorInjectionConfig(
    val enableErrorInjection: Boolean = false,
    val enabledErrorTypes: Set<ErrorInjectionType> = emptySet(),
    val errorRate: Double = 0.05, // 5% error rate
    val errorBurstMode: Boolean = false,
    val errorBurstDuration: Int = 30, // seconds
    val errorBurstRate: Double = 0.5, // 50% during burst
    val customErrorCodes: Map<String, String> = emptyMap()
)

@Serializable
data class LoadTestingConfig(
    val enableLoadTesting: Boolean = false,
    val testingPattern: LoadTestingPattern = LoadTestingPattern.CONSTANT,
    val baseTransactionRate: Int = 10, // TPS
    val maxTransactionRate: Int = 100, // TPS
    val rampUpDuration: Int = 300, // seconds
    val testDuration: Int = 3600, // seconds
    val concurrentUsers: Int = 50,
    val thinkTime: Int = 1000, // ms between transactions
    val enableMetrics: Boolean = true
)

@Serializable
data class FailureSimulationConfig(
    val enableFailureSimulation: Boolean = false,
    val enabledFailureTypes: Set<FailureSimulationType> = emptySet(),
    val failureRate: Double = 0.01, // 1% failure rate
    val meanTimeToFailure: Int = 3600, // seconds
    val meanTimeToRepair: Int = 300, // seconds
    val enableCascadingFailures: Boolean = false,
    val failureRecoveryEnabled: Boolean = true
)

@Serializable
data class PluginConfig(
    val enablePlugins: Boolean = false,
    val pluginDirectory: String = "./plugins",
    val loadedPlugins: Map<PluginType, List<String>> = emptyMap(),
    val enableHotReloading: Boolean = false,
    val enableSandboxing: Boolean = true,
    val maxPluginMemory: Int = 256, // MB
    val pluginTimeout: Int = 30 // seconds
)

@Serializable
data class CustomCommandHandler(
    val name: String,
    val command: String,
    val handler: String,
    val description: String,
    val enabled: Boolean = true
)

@Serializable
data class EventHookConfig(
    val enableEventHooks: Boolean = false,
    val enabledHooks: Map<EventHookType, Boolean> = emptyMap(),
    val hookScriptDirectory: String = "./hooks",
    val enableAsynchronousHooks: Boolean = true,
    val hookTimeout: Int = 10, // seconds
    val enableHookChaining: Boolean = false
)

@Serializable
data class ExternalIntegrationConfig(
    val enabledIntegrations: Set<ExternalIntegrationType> = emptySet(),
    val restApiConfig: RestApiIntegration = RestApiIntegration(),
    val databaseConfig: DatabaseIntegration = DatabaseIntegration(),
    val messageQueueConfig: MessageQueueIntegration = MessageQueueIntegration(),
    val emailConfig: EmailIntegration = EmailIntegration(),
    val webhookConfig: WebhookIntegration = WebhookIntegration()
)

@Serializable
data class RestApiIntegration(
    val baseUrl: String = "",
    val authToken: String = "",
    val timeout: Int = 30,
    val retryAttempts: Int = 3
)

@Serializable
data class DatabaseIntegration(
    val connectionString: String = "",
    val username: String = "",
    val password: String = "",
    val maxConnections: Int = 10
)

@Serializable
data class MessageQueueIntegration(
    val brokerUrl: String = "",
    val queueName: String = "",
    val username: String = "",
    val password: String = ""
)

@Serializable
data class EmailIntegration(
    val smtpServer: String = "",
    val smtpPort: Int = 587,
    val username: String = "",
    val password: String = "",
    val enableTLS: Boolean = true
)

@Serializable
data class WebhookIntegration(
    val webhookUrl: String = "",
    val secret: String = "",
    val retryAttempts: Int = 3,
    val timeout: Int = 30
)

@Serializable
data class DebugConfig(
    val enableDebugMode: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO,
    val enableVerboseLogging: Boolean = false,
    val enableNetworkTracing: Boolean = false,
    val enableMemoryProfiling: Boolean = false,
    val enablePerformanceProfiling: Boolean = false,
    val debugPort: Int = 8001,
    val enableRemoteDebugging: Boolean = false,
    val logToFile: Boolean = true,
    val logFilePath: String = "./debug.log",
    val maxLogFileSize: Int = 100, // MB
    val enableStackTraces: Boolean = true
)

@Serializable
data class TestDataGenerationConfig(
    val enableTestDataGeneration: Boolean = false,
    val enabledDataTypes: Set<TestDataType> = emptySet(),
    val seedValue: Long = 12345,
    val generateRealisticData: Boolean = true,
    val dataSetSize: Int = 1000,
    val exportFormat: String = "JSON",
    val outputDirectory: String = "./testdata"
)

@Serializable
data class MockResponseConfig(
    val enableMockResponses: Boolean = false,
    val defaultResponseType: MockResponseType = MockResponseType.APPROVAL,
    val responseDistribution: Map<MockResponseType, Double> = mapOf(
        MockResponseType.APPROVAL to 0.8,
        MockResponseType.DECLINE to 0.15,
        MockResponseType.REFERRAL to 0.03,
        MockResponseType.ERROR to 0.02
    ),
    val customResponses: Map<String, String> = emptyMap(),
    val enableResponseVariation: Boolean = true,
    val enableRealisticTiming: Boolean = true
)

@Serializable
data class AdvancedOptionsConfiguration(
    val responseDelayConfig: ResponseDelayConfig = ResponseDelayConfig(),
    val errorInjectionConfig: ErrorInjectionConfig = ErrorInjectionConfig(),
    val loadTestingConfig: LoadTestingConfig = LoadTestingConfig(),
    val failureSimulationConfig: FailureSimulationConfig = FailureSimulationConfig(),
    val pluginConfig: PluginConfig = PluginConfig(),
    val customCommandHandlers: List<CustomCommandHandler> = emptyList(),
    val eventHookConfig: EventHookConfig = EventHookConfig(),
    val externalIntegrationConfig: ExternalIntegrationConfig = ExternalIntegrationConfig(),
    val debugConfig: DebugConfig = DebugConfig(),
    val testDataGenerationConfig: TestDataGenerationConfig = TestDataGenerationConfig(),
    val mockResponseConfig: MockResponseConfig = MockResponseConfig()
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
    val operatingMode: OperatingMode = OperatingMode.MAINTENANCE,
    val supportedCommands: List<HsmCommand> = emptyList(),
    val capabilities: Set<HSMCapability> = setOf<HSMCapability>(),
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

@Serializable
enum class AuthenticationPolicy(val displayName: String) {
    PASSWORD("Password Authentication"),
    SMART_CARD("Smart Card"),
    BIOMETRIC("Biometric"),
    MULTI_FACTOR("Multi-Factor Authentication")
}


@Serializable
data class PerformanceSettings(
    val maxOperationsPerSecond: Int = 1000,
    val enablePerformanceLogging: Boolean = false,
    val operationTimeout: Long = 30000L // 30 seconds
)