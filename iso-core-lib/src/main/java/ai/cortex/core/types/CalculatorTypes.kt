package ai.cortex.core.types

import kotlinx.serialization.Serializable

@Serializable
enum class CalculatorCategory {
    CRYPTOGRAPHIC,
    EMV_PAYMENT,
    MAC_HASHING,
    PIN_KEY_MANAGEMENT,
    DATA_FORMAT
}

@Serializable
enum class OperationType {
    ENCRYPT, DECRYPT, SIGN, VERIFY, HASH, DERIVE, ENCODE, DECODE, SESSION, GENERATE_KEY, VALIDATE
}

data class Key(
    val value: ByteArray,
    val cryptoAlgorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Key

        if (!value.contentEquals(other.value)) return false
        if (cryptoAlgorithm != other.cryptoAlgorithm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.contentHashCode()
        result = 31 * result + cryptoAlgorithm.hashCode()
        return result
    }
}

interface CalculatorInput {
    val operation: OperationType
}

interface CalculatorResult {
    val success: Boolean
    val error: String?
    var metadata: ResultMetadata
        get() = ResultMetadata()
        set(value) {}
}

@Serializable
data class ResultMetadata(
    val executionTimeMs: Long = 0,
    val version: String = "1.0.0"
)
