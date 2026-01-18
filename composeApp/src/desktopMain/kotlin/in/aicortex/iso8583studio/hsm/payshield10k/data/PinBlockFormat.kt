package `in`.aicortex.iso8583studio.hsm.payshield10k.data


/**
 * PIN Block formats
 */
enum class PinBlockFormat(val code: String, val description: String) {
    ISO_FORMAT_0("01", "ISO 9564-1 Format 0"),
    ISO_FORMAT_1("05", "ISO 9564-1 Format 1"),
    ISO_FORMAT_2("02", "ISO 9564-1 Format 2"),
    ISO_FORMAT_3("47", "ISO 9564-1 Format 3"),
    ISO_FORMAT_4("48", "ISO 9564-1 Format 4 (AES)"),
    VISA_FORMAT("03", "Visa PIN Block"),
    DIEBOLD_FORMAT("04", "Diebold PIN Block"),
    MASTERCARD_FORMAT("35", "MasterCard Format")
}