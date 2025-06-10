package ai.cortex.core.crypto.data

// Key Check Value Types
enum class KcvType {
    STANDARD,    // Standard 3-byte KCV
    VISA         // VISA-specific KCV
}

enum class UdkDerivationOption(val displayName: String) {
    OPTION_A("Option A"),
    OPTION_B("Option B")
}

enum class KeyParity(val displayName: String) {
    NONE("None"),
    LEFT_ODD("Left odd"),
    RIGHT_ODD("Right odd"),
    LEFT_EVEN("Left even"),
    RIGHT_EVEN("Right even")
}

enum class SessionKeyDerivationMethod(val displayName: String) {
    COMMON_SESSION_KEY("Common Session Key"),
    EMV_SESSION_KEY("EMV Session Key"),
    MASTERCARD_SESSION_KEY("MasterCard Session Key"),
    VISA_HCE_LUK("VISA HCE LUK")
}

enum class CryptogramType(val displayName: String) {
    ARQC("ARQC"),
    TC("TC"),
    AAC("AAC"),
    AAR("AAR")
}

enum class PaddingMethod(val displayName: String) {
    METHOD_1_ISO_9797("Method 1 (ISO-9797)"),
    METHOD_2("Method 2"),
    METHOD_3("Method 3")
}

enum class ArpcMethod(val displayName: String) {
    METHOD_1("Method 1"),
    METHOD_2("Method 2")
}