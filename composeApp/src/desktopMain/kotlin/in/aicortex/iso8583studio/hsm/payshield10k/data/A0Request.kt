package `in`.aicortex.iso8583studio.hsm.payshield10k.data


/**
 * Data class for A0 request parameters
 */
data class A0Request(
    val mode: Char,
    val keyType: String,
    val keyScheme: Char,
    val keySchemeZmk: Char? = null,
    val zmk: String? = null,
    val ksn: String? = null,
    val lmkId: String = "00",
    val deriveKeyMode: Char? = null,
    val dukptMasterKeyType: String? = null,
    val bdk: String? = null
)
