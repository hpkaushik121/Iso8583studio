package `in`.aicortex.iso8583studio.hsm.payshield10k.data

import kotlinx.serialization.Serializable

/**
 * LMK Key Pair - represents a pair of master keys
 */
@Serializable
data class LmkPair(
    val leftKey: ByteArray,     // Left half of LMK pair
    val rightKey: ByteArray,    // Right half of LMK pair
    val variant: Int = 0        // Variant number (0-7)
) {
    fun getCombinedKey(): ByteArray = leftKey + rightKey

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LmkPair) return false
        return leftKey.contentEquals(other.leftKey) &&
                rightKey.contentEquals(other.rightKey) &&
                variant == other.variant
    }

    override fun hashCode(): Int {
        return leftKey.contentHashCode() * 31 + rightKey.contentHashCode()
    }
}