package io.cryptocalc.emv.calculators

import io.cryptocalc.core.*
import io.cryptocalc.core.types.*
import io.cryptocalc.crypto.CryptoEngine
import io.cryptocalc.crypto.engines.DefaultCryptoEngine

class MasterCardCalculator(
    private val cryptoEngine: CryptoEngine = DefaultCryptoEngine()
) : BaseCalculator() {

    override val id = "mastercard-calculator"
    override val name = "MasterCard M/Chip Calculator"
    override val category = CalculatorCategory.EMV_PAYMENT
    override val version = "1.0.0"

    override suspend fun executeOperation(input: CalculatorInput): CalculatorResult {
        return when (input.operation) {
            OperationType.DERIVE -> deriveUDK(input)
            OperationType.GENERATE -> generateSessionKey(input)
            else -> CalculatorResult(
                success = false,
                error = "Unsupported operation: ${input.operation}"
            )
        }
    }

    private suspend fun deriveUDK(input: CalculatorInput): CalculatorResult {
        val masterKey = hexToBytes(input.parameters["masterKey"]!!)
        val pan = input.parameters["pan"]!!
        val panSeq = input.parameters["panSequence"] ?: "00"

        // MasterCard UDK derivation using rightmost 11 digits of PAN + PAN sequence
        val panData = (pan.takeLast(11) + panSeq).padEnd(16, 'F')
        val derivationData = hexToBytes(panData)

        // Encrypt derivation data with master key
        val udk = cryptoEngine.encrypt("3DES", derivationData, masterKey, mapOf("mode" to "ECB"))

        return CalculatorResult(
            success = true,
            data = mapOf(
                "udk" to bytesToHex(udk),
                "derivationData" to panData
            ),
            metadata = ResultMetadata(algorithm = "MasterCard UDK")
        )
    }

    private suspend fun generateSessionKey(input: CalculatorInput): CalculatorResult {
        val udk = hexToBytes(input.parameters["udk"]!!)
        val atc = input.parameters["atc"]!!.toInt(16)

        // Session key derivation data: ATC || F0 padding
        val sessionData = ByteArray(8)
        sessionData[0] = ((atc shr 8) and 0xFF).toByte()
        sessionData[1] = (atc and 0xFF).toByte()
        for (i in 2..7) sessionData[i] = 0xF0.toByte()

        val sessionKey = cryptoEngine.encrypt("3DES", sessionData, udk, mapOf("mode" to "ECB"))

        return CalculatorResult(
            success = true,
            data = mapOf(
                "sessionKey" to bytesToHex(sessionKey),
                "derivationData" to bytesToHex(sessionData)
            ),
            metadata = ResultMetadata(algorithm = "MasterCard Session Key")
        )
    }

    override fun getSchema(): CalculatorSchema {
        return CalculatorSchema(
            requiredParameters = listOf(
                ParameterSchema(
                    name = "masterKey",
                    type = ParameterType.HEX_STRING,
                    description = "MasterCard Master Key (16 bytes)",
                    validation = ParameterValidation(pattern = "^[0-9A-Fa-f]{32}$")
                ),
                ParameterSchema(
                    name = "pan",
                    type = ParameterType.STRING,
                    description = "Primary Account Number"
                ),
                ParameterSchema(
                    name = "atc",
                    type = ParameterType.HEX_STRING,
                    description = "Application Transaction Counter"
                )
            ),
            supportedOperations = listOf(OperationType.DERIVE, OperationType.GENERATE)
        )
    }

    override fun getCapabilities(): CalculatorCapabilities {
        return CalculatorCapabilities(
            supportedAlgorithms = listOf("3DES")
        )
    }
}
