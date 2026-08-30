package `in`.aicortex.iso8583studio.data.model

import kotlinx.serialization.Serializable


/**
 * Security key model
 */
@Serializable
data class SecurityKey(
    val id: String,
    val name: String,
    val cipherType: CipherType = CipherType.AES_256,
    val cipherMode: Int = 0,
    val description: String = "",
    val keyValue: ByteArray = ByteArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecurityKey

        if (id != other.id) return false
        if (name != other.name) return false
        if (cipherType != other.cipherType) return false
        if (cipherMode != other.cipherMode) return false
        if (description != other.description) return false
        if (!keyValue.contentEquals(other.keyValue)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + cipherType.hashCode()
        result = 31 * result + cipherMode
        result = 31 * result + description.hashCode()
        result = 31 * result + keyValue.contentHashCode()
        return result
    }
}
