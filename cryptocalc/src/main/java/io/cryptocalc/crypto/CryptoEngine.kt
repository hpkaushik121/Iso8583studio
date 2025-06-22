package io.cryptocalc.crypto

import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.KcvType
import ai.cortex.core.types.Key
import io.cryptocalc.emv.calculators.emv41.UdkDerivationInput

sealed interface Parameter<T : AlgorithmType>


data class SymmetricParameter(
    val data: ByteArray,
    val key: ByteArray,
    val keySize: Int = 256,
    val iv: ByteArray? = ByteArray(8),
    val mode: CipherMode = CipherMode.CBC
) : Parameter<AlgorithmType.SYMMETRIC_BLOCK> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SymmetricParameter

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

data class AsymmetricParameter(
    val data: ByteArray,
    val publicKey: ByteArray? = null,
    val privateKey: ByteArray? = null
) : Parameter<AlgorithmType.ASYMMETRIC> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AsymmetricParameter

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


data class HashingParameter(
    val data: ByteArray
) : Parameter<AlgorithmType.HASH> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HashingParameter

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}


interface CryptoEngine {
    suspend fun <T : AlgorithmType> encrypt(
        algorithm: CryptoAlgorithm<T>,
        parameter: Parameter<T>,
    ): ByteArray

    suspend fun <T : AlgorithmType> decrypt(
        algorithm: CryptoAlgorithm<T>,
        data: ByteArray,
        key: ByteArray,
        mode: CipherMode = CipherMode.ECB,
    ): ByteArray

    suspend fun calculateKcv(
        key: Key,
        kcvType: KcvType = KcvType.STANDARD
    ): ByteArray

    suspend fun <T : AlgorithmType> generateKey(
        algorithm: CryptoAlgorithm<T>,
        keySize: Int
    ): Key

    suspend fun deriveKey(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        udkDerivationInput: UdkDerivationInput
    ): Key
}
