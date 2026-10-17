package ai.cortex.core.types

import kotlinx.serialization.Serializable

@Serializable
enum class CryptogramType(val methods: List<Methods>) {
    ARQC(emptyList()), TC(emptyList()), AAC(emptyList()), ARPC(listOf(Methods.METHODE_1, Methods.METHODE_2))
}

/**
 * Session key types as per EMV specification
 */
@Serializable
enum class SessionKeyType {
    APPLICATION_CRYPTOGRAM,  // For AC and ARPC generation/verification
    SECURE_MESSAGING_MAC,    // For secure messaging MAC
    SECURE_MESSAGING_ENC     // For secure messaging encryption
}

@Serializable
enum class KcvType{
    STANDARD,VISA
}

@Serializable
enum class Methods{
    METHODE_1,
    METHODE_2
}

@Serializable
enum class UdkDerivationType {
    OPTION_A,
    OPTION_B,
}


@Serializable
enum class CryptogramVersion {
    CVN_10, CVN_14, CVN_17, CVN_18
}

@Serializable
enum class KeyParity {
    NONE, EVEN, ODD
}

@Serializable
data class EMVData(
    val pan: String,
    val panSequenceNumber: String = "00",
    val atc: String,
    val unpredictableNumber: String,
    val amount: String = "",
    val otherAmount: String = "",
    val terminalCountryCode: String = "",
    val terminalVerificationResults: String = "",
    val transactionCurrencyCode: String = "",
    val transactionDate: String = "",
    val transactionType: String = ""
)
