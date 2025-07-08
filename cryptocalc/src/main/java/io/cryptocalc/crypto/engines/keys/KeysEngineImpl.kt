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
import java.math.BigInteger
import java.nio.ByteBuffer
import kotlin.math.pow

internal class KeysEngineImpl(override val emvEngines: EMVEngines) : KeysEngine {
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
    fun byteArrayToBigEndianLong(bytes: ByteArray): Long {
        require(bytes.size <= 8) { "Byte array is too large for a Long." }
        // Create a buffer of 8 bytes, right-align the input bytes
        val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
        buffer.position(Long.SIZE_BYTES - bytes.size)
        buffer.put(bytes)
        buffer.rewind() // Reset position to the beginning for reading
        return buffer.long
    }

    override suspend fun deriveSessionKey(
        algorithm: CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK>,
        sessionKeyInput: SessionKeyInput
    ): Key {

        val atcBytes = IsoUtil.hexToBytes(sessionKeyInput.atc)
        val atcNum = BigInteger(1, atcBytes).toInt()
        print(atcNum)

        // Derive intermediate keys from bottom of tree to second-to-last level
        // because GP (grandparent) from that level is required for SK
        val (p, gp) = walk(algorithm,
            atcNum / (sessionKeyInput.branchFactor),
            (sessionKeyInput.height) - 1, sessionKeyInput.masterKey, sessionKeyInput.iv, sessionKeyInput.branchFactor)
        print(IsoUtil.bytesToHex(p))
        print(IsoUtil.bytesToHex(gp))
        // Derive SK from new IK at tree height XOR'd by GP
        val derivedKey = derive(algorithm,p, gp, atcNum, sessionKeyInput.branchFactor)
        val sessionKey = IsoUtil.xorByteArray(derivedKey, gp)
        print(IsoUtil.bytesToHex(sessionKey))

        return Key(
            when (sessionKeyInput.keyParity) {
                KeyParity.ODD -> CryptoUtils.applyParity(sessionKey, isOdd = true)
                KeyParity.EVEN -> CryptoUtils.applyParity(sessionKey, isOdd = false)
                else -> sessionKey
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