package `in`.aicortex.iso8583studio.hsm.payshield10k.data

import kotlinx.serialization.Serializable
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


/**
 * LMK encryption algorithm — determines how keys are encrypted/decrypted under LMK.
 */
enum class LmkAlgorithm(val display: String, val keyBytes: Int, val blockSize: Int) {
    TDES_2KEY("3DES(2key)", 16, 8),
    TDES_3KEY("3DES(3key)", 24, 8),
    AES_128("AES-128", 16, 16),
    AES_192("AES-192", 24, 16),
    AES_256("AES-256", 32, 16);

    val isAes: Boolean get() = this == AES_128 || this == AES_192 || this == AES_256
    val isTdes: Boolean get() = this == TDES_2KEY || this == TDES_3KEY

    companion object {
        fun fromDisplay(display: String): LmkAlgorithm = entries.firstOrNull { it.display == display } ?: TDES_2KEY
        fun fromName(name: String): LmkAlgorithm = try { valueOf(name) } catch (_: Exception) { TDES_2KEY }
    }
}

/**
 * Complete LMK Set (14 pairs)
 */
@Serializable
data class LmkSet(
    val identifier: String,                              // LMK ID (00-99)
    val pairs: MutableMap<Int, LmkPair> = mutableMapOf(), // 14 LMK pairs (00-27)
    val createdAt: Long = System.currentTimeMillis(),
    val scheme: String = "VARIANT",                       // VARIANT or KEY_BLOCK
    val algorithm: String = "TDES_2KEY"                   // LmkAlgorithm enum name
) {
    val lmkAlgorithm: LmkAlgorithm get() = LmkAlgorithm.fromName(algorithm)

    fun getPair(pairNumber: Int): LmkPair? = pairs[pairNumber]

    fun isComplete(): Boolean = pairs.size == 14
    val checkValue: String                          // LMK check value
        get() {
            // Calculate check value from all pairs
            val allKeys = pairs.values.flatMap { it.getCombinedKey().toList() }.toByteArray()
            return calculateCheckValue(allKeys)
        }

    private fun calculateCheckValue(data: ByteArray): String {
        // Use first 16 bytes, encrypt with DES
        val cipher = Cipher.getInstance("DES/ECB/NoPadding")
        val keySpec = SecretKeySpec(data.copyOf(8), "DES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(ByteArray(8))
        return encrypted.take(6).joinToString("") { "%02X".format(it) }
    }
}
