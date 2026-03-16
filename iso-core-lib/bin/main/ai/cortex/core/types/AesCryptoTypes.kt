package ai.cortex.core.types

import kotlinx.serialization.Serializable

@Serializable
data class AesCryptoInput(
    override val operation: OperationType,
    val key: String,
    val data: String,
    val iv: ByteArray? = null,
    val mode: String? = null,
    val padding: String? = null
) : CalculatorInput {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AesCryptoInput

        if (operation != other.operation) return false
        if (key != other.key) return false
        if (data != other.data) return false
        if (!iv.contentEquals(other.iv)) return false
        if (mode != other.mode) return false
        if (padding != other.padding) return false

        return true
    }

    override fun hashCode(): Int {
        var result = operation.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + (iv?.contentHashCode() ?: 0)
        result = 31 * result + (mode?.hashCode() ?: 0)
        result = 31 * result + (padding?.hashCode() ?: 0)
        return result
    }
}

@Serializable
data class AesCryptoResult(
    override val success: Boolean,
    override val error: String? = null,
    val encrypted: String? = null,
    val decrypted: String? = null,
    val kcv: String? = null,
    override var metadata: ResultMetadata = ResultMetadata()
) : CalculatorResult