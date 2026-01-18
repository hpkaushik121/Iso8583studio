package `in`.aicortex.iso8583studio.hsm.payshield10k.data

/**
 * Key type codes (Variant LMK)
 */
enum class KeyType(val code: String, val description: String) {
    TYPE_000("000", "ZMK - Zone Master Key"),
    TYPE_001("001", "ZPK - Zone PIN Key"),
    TYPE_002("002", "PVK - PIN Verification Key / TPK"),
    TYPE_003("003", "CVK - Card Verification Key"),
    TYPE_008("008", "TAK - Terminal Authentication Key"),
    TYPE_009("009", "ZAK - Zone Authentication Key"),
    TYPE_109("109", "ZEK - Zone Encryption Key"),
    TYPE_209("209", "BDK - Base Derivation Key (DUKPT)")
}