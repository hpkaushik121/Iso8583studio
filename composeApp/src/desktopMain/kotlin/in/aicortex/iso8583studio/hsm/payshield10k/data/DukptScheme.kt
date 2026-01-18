package `in`.aicortex.iso8583studio.hsm.payshield10k.data

/**
 * DUKPT scheme types
 */
enum class DukptScheme {
    ANSI_X9_24_3DES,    // Traditional 3DES DUKPT
    ANSI_X9_24_AES,     // AES DUKPT
    ISO_20038           // ISO standard DUKPT
}
