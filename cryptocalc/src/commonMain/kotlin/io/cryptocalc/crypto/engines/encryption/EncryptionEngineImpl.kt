package io.cryptocalc.crypto.engines.encryption

import ai.cortex.core.toHexUpper

import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.PaddingMethods
import io.cryptocalc.crypto.engines.encryption.models.DecryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.EncryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.HashingEncryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricDecryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters

internal class EncryptionEngineImpl(override val emvEngines: EMVEngines) : EncryptionEngine {

    private val logger: CryptoLogger? get() = emvEngines.logger

    private fun toHex(bytes: ByteArray): String =
        bytes.toHexUpper()

    private fun logSymmetricParams(
        operation: String,
        algorithm: String,
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray?,
        mode: CipherMode,
        padding: PaddingMethods
    ) {
        logger?.log("EMVEngine ► $operation")
        logger?.log("Algorithm........ = $algorithm")
        logger?.log("Cipher Mode...... = $mode")
        logger?.log("Padding.......... = $padding")
        logger?.log("Key (${key.size} bytes).. = ${toHex(key)}")
        if (iv != null && mode != CipherMode.ECB) {
            logger?.log("IV (${iv.size} bytes)... = ${toHex(iv)}")
        }
        logger?.log("Data (${data.size} bytes). = ${toHex(data)}")
    }

    private fun logResult(operation: String, result: ByteArray) {
        logger?.log("EMVEngine ► $operation Result")
        logger?.log("Output (${result.size} bytes) = ${toHex(result)}")
    }

    override suspend fun <T : AlgorithmType> encrypt(
        algorithm: CryptoAlgorithm<T>,
        encryptionEngineParameters: EncryptionEngineParameters<T>
    ): ByteArray {
        return when (algorithm) {
            is CryptoAlgorithm.AES -> {
                val parameter = encryptionEngineParameters as SymmetricEncryptionEngineParameters
                logSymmetricParams(
                    "Encrypt", "AES",
                    parameter.data, parameter.key, parameter.iv,
                    parameter.mode, parameter.padding
                )
                val result = AesCalculatorEngine.encrypt(parameter.data, parameter.key, parameter.mode, parameter.iv, parameter.padding)
                logResult("Encrypt", result)
                result
            }

            // Single DES is TDES with an 8-byte key; the engine handles both.
            is CryptoAlgorithm.DES, is CryptoAlgorithm.TDES -> {
                val parameter = encryptionEngineParameters as SymmetricEncryptionEngineParameters
                logSymmetricParams(
                    "Encrypt", if (algorithm is CryptoAlgorithm.DES) "DES" else "TDES",
                    parameter.data, parameter.key, parameter.iv,
                    parameter.mode, parameter.padding
                )
                val result = TdesCalculatorEngine.encrypt(parameter.data, parameter.key, parameter.mode, parameter.iv, parameter.padding)
                logResult("Encrypt", result)
                result
            }

            is CryptoAlgorithm.SHA1 -> SHA1CalculatorEngine.calculateSHA1(data = (encryptionEngineParameters as HashingEncryptionEngineParameters).data)

            else -> TODO("Not Yet Implemented for encryption ${algorithm::class.simpleName}")
        }
    }

    override suspend fun <T : AlgorithmType> decrypt(
        algorithm: CryptoAlgorithm<T>,
        decryptionEngineParameters: DecryptionEngineParameters<T>
    ): ByteArray {
        return when (algorithm) {
            is CryptoAlgorithm.DES, is CryptoAlgorithm.TDES -> {
                val parameter = decryptionEngineParameters as SymmetricDecryptionEngineParameters
                logSymmetricParams(
                    "Decrypt", if (algorithm is CryptoAlgorithm.DES) "DES" else "TDES",
                    parameter.data, parameter.key, parameter.iv,
                    parameter.mode, parameter.padding
                )
                val result = TdesCalculatorEngine.decrypt(parameter.data, parameter.key, parameter.mode, parameter.iv, parameter.padding)
                logResult("Decrypt", result)
                result
            }

            is CryptoAlgorithm.AES -> {
                val parameter = decryptionEngineParameters as SymmetricDecryptionEngineParameters
                logSymmetricParams(
                    "Decrypt", "AES",
                    parameter.data, parameter.key, parameter.iv,
                    parameter.mode, parameter.padding
                )
                val result = AesCalculatorEngine.decrypt(parameter.data, parameter.key, parameter.mode, parameter.iv, parameter.padding)
                logResult("Decrypt", result)
                result
            }

            else -> TODO("Not Yet Implemented for decryption ${algorithm::class.simpleName}")
        }
    }
}
