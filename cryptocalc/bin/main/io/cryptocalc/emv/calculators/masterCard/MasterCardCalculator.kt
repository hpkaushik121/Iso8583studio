package io.cryptocalc.emv.calculators.masterCard

import ai.cortex.core.IsoUtil
import ai.cortex.core.types.CalculatorCategory
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.OperationType
import io.cryptocalc.core.BaseCalculator
import io.cryptocalc.core.CalculatorCapabilities
import io.cryptocalc.core.CalculatorSchema
import io.cryptocalc.core.ParameterSchema
import io.cryptocalc.core.ParameterType
import io.cryptocalc.core.ParameterValidation
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters
import io.cryptocalc.emv.calculators.emv41.SessionDerivation
import io.cryptocalc.emv.calculators.emv41.UdkDerivation

class MasterCardCalculator() : BaseCalculator<MasterCardCalculatorInput, MasterCardCalculatorCalculatorResult>() {

    override val id = "mastercard-calculator"
    override val name = "MasterCard M/Chip Calculator"
    override val category = CalculatorCategory.EMV_PAYMENT
    override val version = "1.0.0"

    override suspend fun executeOperation(input: MasterCardCalculatorInput): MasterCardCalculatorCalculatorResult {
        return when (input.operation) {
            OperationType.DERIVE -> deriveUDK(input)
            OperationType.SESSION -> generateSessionKey(input)
            else -> MasterCardCalculatorCalculatorResult(
                success = false,
                error = "Unsupported operation: ${input.operation}"
            )
        }
    }

    override fun validate(input: MasterCardCalculatorInput): MasterCardCalculatorCalculatorResult {
        when (input.operation) {
            OperationType.DERIVE -> {
                if (input.udkDerivationInput == null) {
                    return MasterCardCalculatorCalculatorResult(
                        success = false,
                        error = "UDK Derivation Input is required"
                    )
                }
            }

            OperationType.SESSION -> {
                if (input.sessionDerivation == null) {
                    return MasterCardCalculatorCalculatorResult(
                        success =  false,
                        error = "Session Key Derivation Input is required"
                    )
                }
            }

            else -> MasterCardCalculatorCalculatorResult(
                success = false,
                error = "Unsupported operation: ${input.operation}"
            )
        }
        return MasterCardCalculatorCalculatorResult(
            success = true
        )
    }

    private suspend fun deriveUDK(input: MasterCardCalculatorInput): MasterCardCalculatorCalculatorResult {
        val masterKey = IsoUtil.hexToBytes(input.udkDerivationInput!!.masterKey)
        val pan = input.udkDerivationInput.pan
        val panSeq = input.udkDerivationInput.panSequence

        // MasterCard UDK derivation using rightmost 11 digits of PAN + PAN sequence
        val panData = (pan.takeLast(11) + panSeq).padEnd(16, 'F')
        val derivationData = IsoUtil.hexToBytes(panData)

        // Encrypt derivation data with master key
        val udk = emvEngines.encryptionEngine.encrypt(
            CryptoAlgorithm.TDES,
            encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                data = derivationData,
                key = masterKey,
            )
        )

        return MasterCardCalculatorCalculatorResult(
            success = true,
            udkDerivation = UdkDerivation(
                udk = IsoUtil.bytesToHex(udk),
                kcv = panData
            )
        )
    }

    private suspend fun generateSessionKey(input: MasterCardCalculatorInput): MasterCardCalculatorCalculatorResult {
        val udk = IsoUtil.hexToBytes(input.sessionDerivation!!.udk)
        val atc = input.sessionDerivation.atc.toInt(16)

        // Session key derivation data: ATC || F0 padding
        val sessionData = ByteArray(8)
        sessionData[0] = ((atc shr 8) and 0xFF).toByte()
        sessionData[1] = (atc and 0xFF).toByte()
        for (i in 2..7) sessionData[i] = 0xF0.toByte()

        val sessionKey = byteArrayOf()

        return MasterCardCalculatorCalculatorResult(
            success = true,
            sessionDerivation = SessionDerivation(
                sessionKey = IsoUtil.bytesToHex(sessionKey),
                kcv = IsoUtil.bytesToHex(sessionData)
            )
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
            supportedOperations = listOf(OperationType.DERIVE, OperationType.SESSION)
        )
    }

    override fun getCapabilities(): CalculatorCapabilities {
        return CalculatorCapabilities(
            supportedAlgorithms = listOf("3DES")
        )
    }
}