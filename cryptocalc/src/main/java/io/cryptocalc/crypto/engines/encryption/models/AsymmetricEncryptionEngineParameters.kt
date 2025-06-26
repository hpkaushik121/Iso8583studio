package io.cryptocalc.crypto.engines.encryption.models

import ai.cortex.core.types.AlgorithmType
import io.cryptocalc.crypto.engines.encryption.EncryptionEngineParameters

data class AsymmetricEncryptionEngineParameters(
    val data: ByteArray,
    val publicKey: ByteArray? = null,
    val privateKey: ByteArray? = null
) : EncryptionEngineParameters<AlgorithmType.ASYMMETRIC> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AsymmetricEncryptionEngineParameters

        if (!data.contentEquals(other.data)) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!privateKey.contentEquals(other.privateKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + (publicKey?.contentHashCode() ?: 0)
        result = 31 * result + (privateKey?.contentHashCode() ?: 0)
        return result
    }
}