package io.cryptocalc.emv.calculators.acCalculator

import ai.cortex.core.EMVTagParser
import ai.cortex.core.types.CalculatorCategory
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.CryptogramType
import ai.cortex.core.types.OperationType
import ai.cortex.core.types.PaddingMethods
import io.cryptocalc.core.BaseCalculator
import io.cryptocalc.core.CalculatorCapabilities
import io.cryptocalc.core.CalculatorSchema
import io.cryptocalc.core.ParameterSchema
import io.cryptocalc.core.ParameterType
import io.cryptocalc.core.ParameterValidation

class AcCalculator() : BaseCalculator<AcCalculatorInput, AcCalculatorResult>() {

    override val id = "AcCalculator"
    override val name = "AcCalculator"
    override val category: CalculatorCategory = CalculatorCategory.EMV_PAYMENT
    override val version: String = "1.0.0"
    private val arqcMandatoryTags = mapOf(
        "ICC" to setOf("9F36", "82"),           // ATC, AIP only
        "TERMINAL" to setOf(
            "9F02", "9F03", "5F2A", "9A", "9C", // Transaction data
            "9F37", "9F1A", "95"                // UN, Country, TVR
        )
    )
    override suspend fun executeOperation(input: AcCalculatorInput): AcCalculatorResult {
        return when(input.operation){
            OperationType.GENERATE ->{
                val ac = emvEngines.acEngine.generateAC(
                    algorithm = CryptoAlgorithm.TDES,
                    acCalculatorInput = input
                )
                AcCalculatorResult(
                    success = true,
                    ac = ac,
                    error = null
                )
            }
            else -> throw IllegalArgumentException("Unsupported operation: ${input.operation}")
        }
    }

    override fun validate(input: AcCalculatorInput): AcCalculatorResult {
        if(input.sessionKey.isEmpty()){
            return AcCalculatorResult(
                success = false,
                error = "Session Key is required"
            )
        }
        if(input.terminalData.isEmpty()) {
            return AcCalculatorResult(
                success = false,
                error = "Terminal Data is required"
            )
        }
        if(input.iccData.isEmpty()) {
            return AcCalculatorResult(
                success = false,
                error = "ICC Data is required"
            )
        }
        val iccTags = EMVTagParser.parseEMVTags(input.iccData)
        arqcMandatoryTags["ICC"]?.forEach { tag ->
            if(!iccTags.tags.any { it.tag == tag }){
                return AcCalculatorResult(
                    success = false,
                    error = "ICC Data must contain mandatory tags: $tag"
                )
            }
        }

        val terminalTags = EMVTagParser.parseEMVTags(input.terminalData)
        arqcMandatoryTags["TERMINAL"]?.forEach { tag ->
            if (!terminalTags.tags.any { it.tag == tag }) {
                return AcCalculatorResult(
                    success = false,
                    error = "Terminal Data must contain mandatory tags: $tag"
                )
            }
        }


        return AcCalculatorResult(
            success = true,
            ac = null,
            error = null
        )
    }

    override fun getSchema(): CalculatorSchema {

        return CalculatorSchema(
            requiredParameters = listOf(
                ParameterSchema(
                    name = "sessionKey",
                    type = ParameterType.BYTE_ARRAY,
                    description = "Session Key",
                    validation = ParameterValidation(minLength = 16, maxLength = 16)),
                ParameterSchema(
                    name = "terminalData",
                    type = ParameterType.BYTE_ARRAY,
                    description = "Terminal Data",
                    validation = ParameterValidation(minLength = 1, maxLength = 37)),
                ParameterSchema(
                    name = "iccData",
                    type = ParameterType.BYTE_ARRAY,
                    description = "ICC Data",
                    validation = ParameterValidation(minLength = 1, maxLength = 999)),
                ParameterSchema(
                    name = "cryptogramType",
                    type = ParameterType.STRING,
                    description = "Cryptogram Type",
                    validation = ParameterValidation(minLength = 1, maxLength = 1)),
                ParameterSchema(
                    name = "paddingMethods",
                    type = ParameterType.STRING,
                    description = "Padding Methods",
                    validation = ParameterValidation(minLength = 1, maxLength = 1))

            ),
            supportedOperations = listOf(OperationType.GENERATE)
        )
    }

    override fun getCapabilities(): CalculatorCapabilities {
        return CalculatorCapabilities(
            supportedAlgorithms = listOf("3DES")
        )
    }
}