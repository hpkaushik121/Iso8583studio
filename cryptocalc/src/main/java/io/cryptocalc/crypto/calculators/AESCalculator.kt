package io.cryptocalc.crypto.calculators

import ai.cortex.core.types.AesCryptoInput
import ai.cortex.core.types.AesCryptoResult
import ai.cortex.core.types.CalculatorCategory
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.KeySize
import ai.cortex.core.types.OperationType
import ai.cortex.core.types.PaddingMethods
import io.cryptocalc.core.*
import io.cryptocalc.crypto.CryptoEngine
import io.cryptocalc.crypto.SymmetricParameter
import io.cryptocalc.crypto.engines.DefaultCryptoEngine

class AESCalculator(
    private val cryptoEngine: CryptoEngine = DefaultCryptoEngine()
) : BaseCalculator<AesCryptoInput, AesCryptoResult>() {

    override val id = "aes-calculator"
    override val name = "AES Calculator"
    override val category = CalculatorCategory.CRYPTOGRAPHIC
    override val version = "1.0.0"

    override suspend fun executeOperation(input: AesCryptoInput): AesCryptoResult {
        val key = hexToBytes(input.key)
        val data = hexToBytes(input.data)
        val mode = input.mode ?: "CBC"
        val padding = input.padding ?: "PKCS7"

        val algorithm = CryptoAlgorithm.AES
        val options = mapOf(
            "mode" to mode,
            "padding" to padding,
            "iv" to (input.iv ?: "")
        )

        return when (input.operation) {
            OperationType.ENCRYPT -> {
                val encrypted = cryptoEngine.encrypt(algorithm,
                    parameter = SymmetricParameter(
                        data = data,
                        key = key,
                        iv =  input.iv,
                    ))
                AesCryptoResult(
                    success = true,
                    encrypted = bytesToHex(encrypted),
                )
            }

            OperationType.DECRYPT -> {
                val decrypted = byteArrayOf()
                AesCryptoResult(
                    success = true,
                    decrypted = bytesToHex(decrypted),
                )
            }

            else -> AesCryptoResult(
                success = false,
                error = "Unsupported operation for AES: ${input.operation}"
            )
        }
    }

    override fun validate(input: AesCryptoInput): AesCryptoResult {
        return AesCryptoResult(
            success = true
        )

    }

    override fun getSchema(): CalculatorSchema {
        return CalculatorSchema(
            requiredParameters = listOf(
                ParameterSchema(
                    name = "key",
                    type = ParameterType.HEX_STRING,
                    description = "AES key (128, 192, or 256 bits)",
                    validation = ParameterValidation(
                        pattern = "^[0-9A-Fa-f]{32}|[0-9A-Fa-f]{48}|[0-9A-Fa-f]{64}$"
                    )
                ),
                ParameterSchema(
                    name = "data",
                    type = ParameterType.HEX_STRING,
                    description = "Data to encrypt/decrypt"
                )
            ),
            optionalParameters = listOf(
                ParameterSchema(
                    name = "iv",
                    type = ParameterType.HEX_STRING,
                    description = "Initialization Vector (required for non-ECB modes)",
                    validation = ParameterValidation(
                        pattern = "^[0-9A-Fa-f]{32}$"
                    )
                )
            ),
            supportedOperations = listOf(OperationType.ENCRYPT, OperationType.DECRYPT)
        )
    }

    override fun getCapabilities(): CalculatorCapabilities {
        return CalculatorCapabilities(
            supportedAlgorithms = listOf("AES"),
            supportedModes = listOf(CipherMode.ECB, CipherMode.CBC, CipherMode.CFB, CipherMode.OFB),
            supportedPadding = listOf(PaddingMethods.PKCS7, PaddingMethods.NONE),
            supportedKeySizes = listOf(KeySize.AES_128, KeySize.AES_192, KeySize.AES_256)
        )
    }

}
