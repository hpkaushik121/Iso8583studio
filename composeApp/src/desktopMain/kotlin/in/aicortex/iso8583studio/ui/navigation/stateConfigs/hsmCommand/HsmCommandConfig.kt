package `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand

import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.CipherSuite
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.CertificateType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.SSLTLSVersion
import kotlinx.serialization.Serializable

@Serializable
data class HsmCommandConfig(
    override val id: String,
    override val name: String,
    override val description: String = "",
    override val simulatorType: SimulatorType = SimulatorType.HSM_COMMAND,
    override val enabled: Boolean = true,
    override val createdDate: Long = System.currentTimeMillis(),
    override val modifiedDate: Long = System.currentTimeMillis(),
    override val version: String = "1.0",

    val ipAddress: String = "127.0.0.1",
    val port: Int = 1500,
    val timeout: Int = 30,
    val headerValue: String = "0000",
    val trailerValue: String = "",
    val tcpLengthHeaderEnabled: Boolean = true,
    val messageHeaderLength: Int = 4,

    val hsmVendor: HsmVendorType = HsmVendorType.THALES_PAYSHIELD,

    val sslConfig: HsmCommandSslConfig = HsmCommandSslConfig(),

    val loadTestConcurrentConnections: Int = 1,
    val loadTestCommandsPerSecond: Int = 10,
    val loadTestDurationSeconds: Int = 60,
    val loadTestPattern: LoadTestPattern = LoadTestPattern.CONSTANT,
) : SimulatorConfig {
    override val serverAddress: String get() = ipAddress
    override val serverPort: Int get() = port
}

@Serializable
data class HsmCommandSslConfig(
    val enabled: Boolean = false,
    val tlsVersion: SSLTLSVersion = SSLTLSVersion.TLS_1_2,
    val certificateVerification: CertVerificationMethod = CertVerificationMethod.NONE,
    val caAuthorityPath: String = "",
    val clientPublicCertPath: String = "",
    val clientPrivateKeyPath: String = "",
    val keyStorePassword: String = "",
    val certificateType: CertificateType = CertificateType.PKCS12,
    val cipherSuites: Set<CipherSuite> = setOf(
        CipherSuite.TLS_AES_256_GCM_SHA384,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
    )
)

@Serializable
enum class CertVerificationMethod(val displayName: String) {
    NONE("No Verification"),
    TRUST_ALL("Trust All Certificates"),
    CA_SIGNED("CA-Signed Only"),
    CUSTOM_CA("Custom CA Authority")
}

@Serializable
enum class HsmVendorType(
    val displayName: String,
    val defaultPort: Int,
    val headerFormat: HeaderFormat,
    val description: String
) {
    THALES_PAYSHIELD("Thales payShield", 1500, HeaderFormat.TWO_BYTE_LENGTH, "Thales payShield 9000/10K"),
    FUTUREX("Futurex Excrypt", 2000, HeaderFormat.TWO_BYTE_LENGTH, "Futurex KMES Series 3"),
    SAFENET_LUNA("SafeNet Luna", 1500, HeaderFormat.TWO_BYTE_LENGTH, "Thales Luna Network HSM"),
    UTIMACO("Utimaco CryptoServer", 3001, HeaderFormat.TWO_BYTE_LENGTH, "Utimaco CryptoServer Se/CP5"),
    NCIPHER("nCipher nShield", 9004, HeaderFormat.FOUR_BYTE_ASCII_LENGTH, "Entrust nShield Connect/Solo"),
    ATALLA("Utimaco Atalla", 7000, HeaderFormat.STX_ETX, "Utimaco Atalla AT1000"),
    GENERIC("Generic HSM", 1500, HeaderFormat.TWO_BYTE_LENGTH, "Custom/Other HSM"),
}

@Serializable
enum class HeaderFormat(val displayName: String) {
    TWO_BYTE_LENGTH("2-byte Binary Length"),
    FOUR_BYTE_ASCII_LENGTH("4-byte ASCII Length"),
    STX_ETX("STX/ETX Framing"),
    NONE("No Header/Framing"),
    CUSTOM("Custom Header"),
}

@Serializable
enum class LoadTestPattern(val displayName: String) {
    CONSTANT("Constant Rate"),
    RAMP_UP("Ramp Up"),
    SPIKE("Spike Test"),
    BURST("Burst Pattern"),
}
