package `in`.aicortex.iso8583studio.hsm.payshield10k.data

import kotlinx.serialization.Serializable
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


/**
 * Complete LMK Set (14 pairs)
 */
@Serializable
data class LmkSet(
    val identifier: String,                              // LMK ID (00-99)
    val pairs: MutableMap<Int, LmkPair> = mutableMapOf(), // 14 LMK pairs (00-27)
    val createdAt: Long = System.currentTimeMillis(),
    val scheme: String = "VARIANT"                        // VARIANT or KEY_BLOCK
) {
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
