package io.cryptocalc.crypto.engines

import ai.cortex.core.CryptoUtils
import ai.cortex.core.IsoUtil
import io.cryptocalc.core.hexToBytes
import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.KcvType
import ai.cortex.core.types.Key
import ai.cortex.core.types.KeyParity
import ai.cortex.core.types.UdkDerivationType
import io.cryptocalc.crypto.CryptoEngine
import io.cryptocalc.crypto.HashingParameter
import io.cryptocalc.crypto.Parameter
import io.cryptocalc.crypto.SymmetricParameter
import io.cryptocalc.emv.calculators.emv41.UdkDerivationInput

class DefaultCryptoEngine() : CryptoEngine {
    override suspend fun <T : AlgorithmType> encrypt(
        algorithm: CryptoAlgorithm<T>,
        parameter: Parameter<T>
    ): ByteArray {
        return when (algorithm) {
            is CryptoAlgorithm.AES -> {
                val parameter = parameter as SymmetricParameter
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

            is CryptoAlgorithm.TDES -> {
                val parameter = parameter as SymmetricParameter
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

            is CryptoAlgorithm.SHA1 -> {
                byteArrayOf()
            }

            else -> TODO("Not Yet Implemented ${algorithm::class.java}")
        }
    }

    override suspend fun <T : AlgorithmType> decrypt(
        algorithm: CryptoAlgorithm<T>,
        data: ByteArray,
        key: ByteArray,
        mode: CipherMode
    ): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun calculateKcv(
        key: Key,
        kcvType: KcvType
    ): ByteArray {
        val zeroBlock = ByteArray(8)
        val encrypted = encrypt(
            algorithm = key.cryptoAlgorithm,
            parameter = SymmetricParameter(
                data = zeroBlock,
                key = key.value,
                mode = CipherMode.ECB
            )
        )
        return when (kcvType) {
            KcvType.STANDARD -> encrypted.copyOfRange(0, 3) // First 3 bytes (6 hex chars)
            KcvType.VISA -> encrypted.copyOfRange(0, 4)     // First 4 bytes (8 hex chars)
        }
    }

    override suspend fun <T : AlgorithmType> generateKey(
        algorithm: CryptoAlgorithm<T>,
        keySize: Int
    ): Key {
        return when(algorithm){
            CryptoAlgorithm.TDES ->{
                Key(value = TdesCalculatorEngine.generateKey(keySize),
                    cryptoAlgorithm = algorithm as CryptoAlgorithm.TDES)
            }
            else -> TODO("Not yet implemented")
        }
    }

    override suspend fun deriveKey(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        udkDerivationInput: UdkDerivationInput
    ): Key {
        val mdk = hexToBytes(udkDerivationInput.masterKey)
        val pan = udkDerivationInput.pan
        val panDigits = pan.filter { it.isDigit() }
        val panSeq = udkDerivationInput.panSequence.padStart(2, '0')
        val result = when (udkDerivationInput.udkDerivationType) {
            UdkDerivationType.OPTION_A -> {
                deriveUdkOptionA(algorithm, mdk, panDigits, panSeq)
            }

            UdkDerivationType.OPTION_B -> {
                deriveUdkOptionB(algorithm, mdk, panDigits, panSeq)
            }
        }
        return Key(
            when (udkDerivationInput.keyParity) {
                KeyParity.ODD -> CryptoUtils.applyParity(result, isOdd = true)
                KeyParity.EVEN -> CryptoUtils.applyParity(result, isOdd = false)
                else -> result
            },
            algorithm
        )
    }

    private suspend fun deriveUdkOptionA(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        mdk: ByteArray,
        panDigits: String,
        panSeq: String
    ): ByteArray {
        val combinedPan = (panDigits + panSeq).padStart(16, '0').takeLast(16)
        val y = IsoUtil.stringToBcd(combinedPan)
        return encrypt(
            algorithm = algorithm,
            parameter = SymmetricParameter(
                data = y,
                key = mdk
            )
        )
    }

    private suspend fun deriveUdkOptionB(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        mdk: ByteArray,
        panDigits: String,
        panSeq: String
    ): ByteArray {
        var n = panDigits + panSeq

        if (n.length % 2 == 1) {
            n = "0$n"
        }

        val bytes = ByteArray(n.length / 2)
        for (i in bytes.indices) {
            val hexPair = n.substring(i * 2, i * 2 + 2)
            bytes[i] = hexPair.toInt(16).toByte()
        }
        val sha1Hash = encrypt(
            algorithm = CryptoAlgorithm.SHA1,
            parameter = HashingParameter(bytes)
        )
        val sha1Hex = sha1Hash.joinToString("") { "%02X".format(it) }
        val decimalizedString = CryptoUtils.decimalize(sha1Hex)
        val y16 = decimalizedString.take(16)
        val y = IsoUtil.stringToBcd(y16)
        return encrypt(
            algorithm = algorithm,
            parameter = SymmetricParameter(
                data = y,
                key = mdk
            )
        )
    }

}
