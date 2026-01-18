package `in`.aicortex.iso8583studio.hsm.payshield10k.data

/**
 * LMK Key Pair types (Variant LMK)
 */
enum class LmkPairType(val pairNumber: Int, val description: String) {
    PAIR_00_01(0, "Zone Master Key (ZMK) / Zone PIN Key (ZPK)"),
    PAIR_02_03(1, "PIN Verification Key (PVK)"),
    PAIR_04_05(2, "Card Verification Key (CVK)"),
    PAIR_06_07(3, "Decimalization Table"),
    PAIR_08_09(4, "Message Authentication Code Key (TAK/MAK)"),
    PAIR_10_11(5, "PIN Generation Key"),
    PAIR_14_15(6, "Terminal PIN Encryption Key (TPK)"),
    PAIR_16_17(7, "Data Encryption Key (DEK/TEK/ZEK)"),
    PAIR_18_19(8, "Key Encryption Key (KEK)"),
    PAIR_26_27(9, "Zone Key Encryption Key (ZEK)")
}