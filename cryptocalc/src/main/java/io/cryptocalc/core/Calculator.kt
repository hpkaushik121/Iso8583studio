package io.cryptocalc.core

import ai.cortex.core.types.CalculatorCategory
import ai.cortex.core.types.CalculatorInput
import ai.cortex.core.types.CalculatorResult
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.KeySize
import ai.cortex.core.types.OperationType
import ai.cortex.core.types.PaddingMethods

interface Calculator<T: CalculatorInput, R: CalculatorResult>  {
    val id: String
    val name: String
    val category: CalculatorCategory
    val version: String

    suspend fun execute(input: T): R
    fun validate(input: T): R
    fun getSchema(): CalculatorSchema
    fun getCapabilities(): CalculatorCapabilities
}

data class CalculatorSchema(
    val requiredParameters: List<ParameterSchema>,
    val optionalParameters: List<ParameterSchema> = emptyList(),
    val supportedOperations: List<OperationType>
)

data class ParameterSchema(
    val name: String,
    val type: ParameterType,
    val description: String,
    val validation: ParameterValidation? = null
)

enum class ParameterType {
    STRING, HEX_STRING, BASE64_STRING, INTEGER, BOOLEAN
}

data class ParameterValidation(
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val allowedValues: List<String>? = null
)

data class CalculatorCapabilities(
    val supportedAlgorithms: List<String> = emptyList(),
    val supportedModes: List<CipherMode> = emptyList(),
    val supportedPadding: List<PaddingMethods> = emptyList(),
    val supportedKeySizes: List<KeySize> = emptyList(),
    val maxConcurrentOperations: Int = 10
)
