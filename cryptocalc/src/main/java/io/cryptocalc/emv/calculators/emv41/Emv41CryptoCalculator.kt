package io.cryptocalc.emv.calculators.emv41

import io.cryptocalc.core.BaseCalculator
import io.cryptocalc.core.CalculatorCapabilities
import io.cryptocalc.core.CalculatorSchema
import io.cryptocalc.core.ParameterSchema
import io.cryptocalc.core.ParameterType
import io.cryptocalc.core.ParameterValidation
import io.cryptocalc.core.bytesToHex
import io.cryptocalc.core.hexToBytes
import ai.cortex.core.types.CalculatorCategory
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.OperationType
import io.cryptocalc.crypto.CryptoEngine
import io.cryptocalc.crypto.engines.DefaultCryptoEngine


class Emv41CryptoCalculator(
    private val cryptoEngine: CryptoEngine = DefaultCryptoEngine()
) : BaseCalculator<EMVCalculatorInput, EMVCalculatorResult>() {

    override val id = "mastercard-calculator"
    override val name = "MasterCard M/Chip Calculator"
    override val category = CalculatorCategory.EMV_PAYMENT
    override val version = "1.0.0"

    override suspend fun executeOperation(input: EMVCalculatorInput): EMVCalculatorResult {
        return try {
            when (input.operation) {
                OperationType.DERIVE -> {
                    val udkKey = cryptoEngine.deriveKey(
                        algorithm = CryptoAlgorithm.TDES,
                        udkDerivationInput = input.udkDerivationInput!!
                    )
                    EMVCalculatorResult(
                        success = true,
                        udkDerivation = UdkDerivation(
                            udk = bytesToHex(udkKey.value),
                            kcv = bytesToHex(cryptoEngine.calculateKcv(key = udkKey))
                        )
                    )
                }

                OperationType.GENERATE_KEY -> {
                    val key = cryptoEngine.generateKey(
                        algorithm = CryptoAlgorithm.TDES,
                        keySize = 16
                    )
                    EMVCalculatorResult(
                        success = true,
                        key = bytesToHex(key.value)
                    )
                }

                else -> EMVCalculatorResult(
                    success = false,
                    error = "Unsupported operation: ${input.operation}"
                )
            }
        } catch (e: Exception) {
            EMVCalculatorResult(
                success = false,
                error = "${e.message}"
            )
        }
    }

    override fun validate(input: EMVCalculatorInput): EMVCalculatorResult {
        when (input.operation) {
            OperationType.DERIVE -> {
                if (input.udkDerivationInput == null) {
                    return EMVCalculatorResult(
                        success = false,
                        error = "UDK Derivation Input is required"

                    )
                }
                val mdk = hexToBytes(input.udkDerivationInput.masterKey)
                if (mdk.size != 16) {
                    return EMVCalculatorResult(
                        success = false,
                        error = "Invalid MDK length"

                    )
                }


            }

            OperationType.SESSION -> {
                if (input.sessionDerivation == null) {
                    return EMVCalculatorResult(
                        success = false,
                        error = "Session Key Derivation Input is required"
                    )
                }
            }

            else -> EMVCalculatorResult(
                success = false,
                error = "Unsupported operation: ${input.operation}"
            )
        }
        return EMVCalculatorResult(
            success = true
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