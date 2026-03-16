package io.cryptocalc.crypto.engines.encryption.models

import ai.cortex.core.types.AlgorithmType
import io.cryptocalc.crypto.engines.encryption.models.EncryptionEngineParameters

data class HashingEncryptionEngineParameters(
    val data: ByteArray
) : EncryptionEngineParameters<AlgorithmType.HASH> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HashingEncryptionEngineParameters

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}