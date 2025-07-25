package io.cryptocalc.crypto.engines.encryption

import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import io.cryptocalc.crypto.engines.encryption.models.DecryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.EncryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.HashingEncryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricDecryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters

internal class EncryptionEngineImpl(override val emvEngines: EMVEngines) : EncryptionEngine {
    override suspend fun <T : AlgorithmType> encrypt(
        algorithm: CryptoAlgorithm<T>,
        encryptionEngineParameters: EncryptionEngineParameters<T>
    ): ByteArray {
        return when (algorithm) {
            is CryptoAlgorithm.AES -> {
                val parameter = encryptionEngineParameters as SymmetricEncryptionEngineParameters
                when (parameter.mode) {
                    CipherMode.ECB -> TODO()
                    CipherMode.CBC -> TODO()
                    CipherMode.CFB -> TODO()
                    CipherMode.OFB -> TODO()
                    CipherMode.GCM -> TODO()
                    CipherMode.CTR -> TODO()
                }
                byteArrayOf()
            }

            is CryptoAlgorithm.TDES  -> {
                val parameter = encryptionEngineParameters as SymmetricEncryptionEngineParameters
                when (parameter.mode) {
                    CipherMode.ECB -> TdesCalculatorEngine.encryptECB(parameter.data, parameter.key)
                    CipherMode.CBC -> TdesCalculatorEngine.encryptCBC(
                        parameter.data,
                        parameter.key,
                        parameter.iv
                    )

                    CipherMode.CFB -> TdesCalculatorEngine.encryptCBC(
                        parameter.data,
                        parameter.key,
                        parameter.iv!!
                    )
                    CipherMode.OFB -> TdesCalculatorEngine.encryptOFB(
                        parameter.data,
                        parameter.key,
                        parameter.iv!!
                    )
                    CipherMode.GCM -> TODO()
                    CipherMode.CTR -> TODO()
                }

            }

            is CryptoAlgorithm.SHA1 -> SHA1CalculatorEngine.calculateSHA1(data = (encryptionEngineParameters as HashingEncryptionEngineParameters).data)

            else -> TODO("Not Yet Implemented for encryption ${algorithm::class.java}")
        }
    }

    override suspend fun <T : AlgorithmType> decrypt(
        algorithm: CryptoAlgorithm<T>,
        decryptionEngineParameters: DecryptionEngineParameters<T>
    ): ByteArray {
        return when(algorithm){
            is CryptoAlgorithm.TDES ->{
                val parameter = decryptionEngineParameters as SymmetricDecryptionEngineParameters
                when (parameter.mode) {
                    CipherMode.ECB -> TdesCalculatorEngine.decryptECB(parameter.data,parameter.key)
                    CipherMode.CBC -> TdesCalculatorEngine.decryptCBC(parameter.data,parameter.key)
                    CipherMode.CFB -> TODO()
                    CipherMode.OFB -> TODO()
                    CipherMode.GCM -> TODO()
                    CipherMode.CTR -> TODO()
                }

            }
            else -> TODO("Not Yet Implemented for decryption ${algorithm::class.java}")
        }
    }



}
