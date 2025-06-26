package io.cryptocalc.crypto.engines.keys

import ai.cortex.core.CryptoUtils
import ai.cortex.core.IsoUtil
import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.KcvType
import ai.cortex.core.types.Key
import ai.cortex.core.types.KeyParity
import ai.cortex.core.types.UdkDerivationType
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.crypto.engines.encryption.TdesCalculatorEngine
import io.cryptocalc.crypto.engines.encryption.models.HashingEncryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters
import io.cryptocalc.emv.calculators.emv41.SessionKeyInput
import io.cryptocalc.emv.calculators.emv41.UdkDerivationInput

class KeysEngineImpl(override val emvEngines: EMVEngines) : KeysEngine {
    override suspend fun calculateKcv(
        key: Key,
        kcvType: KcvType
    ): ByteArray {
        return when (key.cryptoAlgorithm) {
            is CryptoAlgorithm.TDES -> TdesCalculatorEngine.calculateKCV(key.value)

            else -> TODO("Not yet implemented for ${key.cryptoAlgorithm::class.java}")
        }
    }

    override suspend fun <T : AlgorithmType> generateKey(
        algorithm: CryptoAlgorithm<T>,
        keySize: Int
    ): Key {
        return when (algorithm) {
            CryptoAlgorithm.TDES -> {
                Key(
                    value = TdesCalculatorEngine.generateKey(keySize),
                    cryptoAlgorithm = algorithm as CryptoAlgorithm.TDES
                )
            }

            else -> TODO("Not yet implemented")
        }
    }

    override suspend fun deriveUdkKey(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        udkDerivationInput: UdkDerivationInput
    ): Key {
        val mdk = IsoUtil.hexToBytes(udkDerivationInput.masterKey)
        val pan = udkDerivationInput.pan
        val panDigits = pan.filter { it.isDigit() }
        val panSeq = udkDerivationInput.panSequence.padStart(2, '0')

        val desKey = if (mdk.size == 16) {
            mdk + mdk.copyOfRange(0, 8)
        } else {
            mdk
        }

        val result = when (udkDerivationInput.udkDerivationType) {
            UdkDerivationType.OPTION_A -> {
                deriveUdkOptionA(algorithm, desKey, panDigits, panSeq)
            }

            UdkDerivationType.OPTION_B -> {
                deriveUdkOptionB(algorithm, desKey, panDigits, panSeq)
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

    override suspend fun deriveSessionKey(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        sessionKeyInput: SessionKeyInput
    ): Key {
        val desKey = if (sessionKeyInput.masterKey.size == 16) {
            sessionKeyInput.masterKey + sessionKeyInput.masterKey.copyOfRange(0, 8)
        } else {
            sessionKeyInput.masterKey
        }
        // Create diversification data combining all parameters
        val diversificationData = createEMVDiversificationValue(
            atc = sessionKeyInput.atc.toByteArray(),
            sessionKeyType = sessionKeyInput.sessionKeyType,
            blockSize = algorithm.blockSize!!,
            applicationCryptogram = null
        )
        val rawSessionKey = performSessionKeyDerivation(
            masterKey = desKey,
            diversificationValue = diversificationData,
            algorithm = algorithm,
            keyBits = desKey.size * 8,
            blockSize = algorithm.blockSize!!,
            iv = sessionKeyInput.iv
        )

        return Key(
            when (sessionKeyInput.keyParity) {
                KeyParity.ODD -> CryptoUtils.applyParity(rawSessionKey, isOdd = true)
                KeyParity.EVEN -> CryptoUtils.applyParity(rawSessionKey, isOdd = false)
                else -> rawSessionKey
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
        val xorMask = IsoUtil.hexStringToBytes("FFFFFFFFFFFFFFFF")

        val yXor = IsoUtil.xorByteArray(y, xorMask)
        val zl = emvEngines.encryptionEngine.encrypt(
            algorithm = algorithm,
            encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                data = y,
                key = mdk,
                mode = CipherMode.ECB
            )
        )
        val zr = emvEngines.encryptionEngine.encrypt(
            algorithm = algorithm,
            encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                data = yXor,
                key = mdk,
                mode = CipherMode.ECB
            )
        )
        return zl + zr
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
        val sha1Hash = emvEngines.encryptionEngine.encrypt(
            algorithm = CryptoAlgorithm.SHA1,
            encryptionEngineParameters = HashingEncryptionEngineParameters(bytes)
        )
        val sha1Hex = sha1Hash.joinToString("") { "%02X".format(it) }
        val decimalizedString = CryptoUtils.decimalize(sha1Hex)
        val y16 = decimalizedString.take(16)
        val y = IsoUtil.stringToBcd(y16)

        val xorMask = IsoUtil.hexStringToBytes("FFFFFFFFFFFFFFFF")

        val yXor = IsoUtil.xorByteArray(y, xorMask)
        val zl = emvEngines.encryptionEngine.encrypt(
            algorithm = algorithm,
            encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                data = y,
                key = mdk,
                mode = CipherMode.ECB
            )
        )
        val zr = emvEngines.encryptionEngine.encrypt(
            algorithm = algorithm,
            encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                data = yXor,
                key = mdk,
                mode = CipherMode.ECB
            )
        )
        return zl + zr
    }
}