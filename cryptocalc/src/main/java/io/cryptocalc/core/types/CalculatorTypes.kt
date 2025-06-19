package io.cryptocalc.core.types

import kotlinx.serialization.Serializable

@Serializable
enum class CalculatorCategory {
    CRYPTOGRAPHIC,
    EMV_PAYMENT,
    MAC_HASHING,
    PIN_KEY_MANAGEMENT,
    DATA_FORMAT
}

@Serializable
enum class OperationType {
    ENCRYPT, DECRYPT, SIGN, VERIFY, HASH, DERIVE, ENCODE, DECODE, GENERATE, VALIDATE
}

@Serializable
data class CalculatorInput(
    val operation: OperationType,
    val parameters: Map<String, String>,
    val options: Map<String, String> = emptyMap()
)

@Serializable
data class CalculatorResult(
    val success: Boolean,
    val data: Map<String, String> = emptyMap(),
    val error: String? = null,
    val metadata: ResultMetadata = ResultMetadata()
)

@Serializable
data class ResultMetadata(
    val executionTimeMs: Long = 0,
    val algorithm: String = "",
    val version: String = "1.0.0"
)

@Serializable
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)
