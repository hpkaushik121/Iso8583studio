package io.cryptocalc.crypto.engines.encryption.models

import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import io.cryptocalc.crypto.engines.encryption.models.EncryptionEngineParameters

data class SymmetricEncryptionEngineParameters(
    val data: ByteArray,
    val key: ByteArray,
    val keySize: Int = 256,
    val iv: ByteArray? = ByteArray(8),
    val mode: CipherMode = CipherMode.CBC
) : EncryptionEngineParameters<AlgorithmType.SYMMETRIC_BLOCK> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SymmetricEncryptionEngineParameters

        if (!data.contentEquals(other.data)) return false
        if (!key.contentEquals(other.key)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (mode != other.mode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + key.contentHashCode()
        result = 31 * result + (iv?.contentHashCode() ?: 0)
        result = 31 * result + mode.hashCode()
        return result
    }
}