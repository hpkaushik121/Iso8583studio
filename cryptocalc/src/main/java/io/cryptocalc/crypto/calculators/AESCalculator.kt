package io.cryptocalc.crypto.calculators

import io.cryptocalc.core.*
import io.cryptocalc.core.types.*
import io.cryptocalc.crypto.CryptoEngine
import io.cryptocalc.crypto.engines.DefaultCryptoEngine

class AESCalculator(
    private val cryptoEngine: CryptoEngine = DefaultCryptoEngine()
) : BaseCalculator() {

    override val id = "aes-calculator"
    override val name = "AES Calculator"
    override val category = CalculatorCategory.CRYPTOGRAPHIC
    override val version = "1.0.0"

    override suspend fun executeOperation(input: CalculatorInput): CalculatorResult {
        val key = hexToBytes(input.parameters["key"]!!)
        val data = hexToBytes(input.parameters["data"]!!)
        val mode = input.options["mode"] ?: "CBC"
        val padding = input.options["padding"] ?: "PKCS7"

        val algorithm = "AES"
        val options = mapOf(
            "mode" to mode,
            "padding" to padding,
            "iv" to (input.options["iv"] ?: "")
        )

        return when (input.operation) {
            OperationType.ENCRYPT -> {
                val encrypted = cryptoEngine.encrypt(algorithm, data, key, options)
                val kcv = calculateKCV(key)
                CalculatorResult(
                    success = true,
                    data = mapOf(
                        "encrypted" to bytesToHex(encrypted),
                        "kcv" to bytesToHex(kcv)
                    ),
                    metadata = ResultMetadata(algorithm = "AES-${key.size * 8}")
                )
            }
            OperationType.DECRYPT -> {
                val decrypted = cryptoEngine.decrypt(algorithm, data, key, options)
                CalculatorResult(
                    success = true,
                    data = mapOf("decrypted" to bytesToHex(decrypted)),
                    metadata = ResultMetadata(algorithm = "AES-${key.size * 8}")
                )
            }
            else -> CalculatorResult(
                success = false,
                error = "Unsupported operation for AES: ${input.operation}"
            )
        }
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
            supportedPadding = listOf(PaddingScheme.PKCS7, PaddingScheme.NONE),
            supportedKeySizes = listOf(KeySize.AES_128, KeySize.AES_192, KeySize.AES_256)
        )
    }

    private suspend fun calculateKCV(key: ByteArray): ByteArray {
        val zeroBlock = ByteArray(16) // 16 bytes of zeros
        val encrypted = cryptoEngine.encrypt("AES", zeroBlock, key, mapOf("mode" to "ECB"))
        return encrypted.sliceArray(0..2) // First 3 bytes
    }
}
