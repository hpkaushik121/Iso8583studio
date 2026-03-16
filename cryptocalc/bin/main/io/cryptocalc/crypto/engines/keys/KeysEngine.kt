package io.cryptocalc.crypto.engines.keys

import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.KcvType
import ai.cortex.core.types.Key
import io.cryptocalc.crypto.engines.Engine
import io.cryptocalc.emv.calculators.emv41.SessionKeyInput
import io.cryptocalc.emv.calculators.emv41.UdkDerivationInput

interface KeysEngine: Engine {

    suspend fun calculateKcv(
        key: Key,
        kcvType: KcvType = KcvType.STANDARD
    ): ByteArray

    suspend fun <T : AlgorithmType> generateKey(
        algorithm: CryptoAlgorithm<T>,
        keySize: Int
    ): Key

    suspend fun deriveUdkKey(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        udkDerivationInput: UdkDerivationInput
    ): Key

    suspend fun deriveSessionKey(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        sessionKeyInput: SessionKeyInput
    ): Key

}