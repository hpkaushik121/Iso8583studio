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
    val bdk: String? = null,
    // KeyBlock attribute fields (parsed from # delimiter section)
    val keyBlockUsage: String? = null,           // 2 chars, e.g. "P0", "K0", "51"
    val keyBlockAlgorithm: String? = null,       // 2 chars, e.g. "T2" (TDES double), "A1" (AES-128)
    val keyBlockModeOfUse: Char? = null,         // 1 char, e.g. 'E', 'N', 'B'
    val keyBlockKeyVersionNumber: String? = null, // 2 chars, e.g. "00"
    val keyBlockExportability: Char? = null,      // 1 char, e.g. 'E', 'N', 'S'
    val keyBlockNumOptionalBlocks: String? = null  // 2 chars, e.g. "00"
)
