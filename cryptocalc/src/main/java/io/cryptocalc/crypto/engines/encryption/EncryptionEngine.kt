package io.cryptocalc.crypto.engines.encryption

import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import io.cryptocalc.crypto.engines.Engine
import io.cryptocalc.crypto.engines.encryption.models.EncryptionEngineParameters

interface EncryptionEngine : Engine {
    suspend fun <T : AlgorithmType> encrypt(
        algorithm: CryptoAlgorithm<T>,
        encryptionEngineParameters: EncryptionEngineParameters<T>,
    ): ByteArray

    suspend fun <T : AlgorithmType> decrypt(
        algorithm: CryptoAlgorithm<T>,
        data: ByteArray,
        key: ByteArray,
        mode: CipherMode = CipherMode.ECB,
    ): ByteArray

}