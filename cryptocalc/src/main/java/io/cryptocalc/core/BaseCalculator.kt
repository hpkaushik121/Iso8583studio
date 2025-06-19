package io.cryptocalc.core

import io.cryptocalc.core.types.*
import io.cryptocalc.validation.ValidationEngine
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

abstract class BaseCalculator : Calculator {
    protected val validationEngine = ValidationEngine()

    @OptIn(ExperimentalTime::class)
    override suspend fun execute(input: CalculatorInput): CalculatorResult {
        val startTime = Clock.System.now()

        try {
            // Validate input
            val validationResult = validate(input)
            if (!validationResult.isValid) {
                return CalculatorResult(
                    success = false,
                    error = "Validation failed: ${validationResult.errors.joinToString(", ")}"
                )
            }

            // Execute operation
            val result = executeOperation(input)

            val endTime = Clock.System.now()
            val executionTime = endTime.minus(startTime).inWholeMilliseconds

            return result.copy(
                metadata = result.metadata.copy(
                    executionTimeMs = executionTime,
                    version = version
                )
            )

        } catch (e: Exception) {
            return CalculatorResult(
                success = false,
                error = "Execution failed: ${e.message}"
            )
        }
    }

    protected abstract suspend fun executeOperation(input: CalculatorInput): CalculatorResult

    override fun validate(input: CalculatorInput): ValidationResult {
        val errors = mutableListOf<String>()
        val schema = getSchema()

        // Check if operation is supported
        if (input.operation !in schema.supportedOperations) {
            errors.add("Unsupported operation: ${input.operation}")
        }

        // Check required parameters
        schema.requiredParameters.forEach { param ->
            if (param.name !in input.parameters) {
                errors.add("Missing required parameter: ${param.name}")
            } else {
                validateParameter(param, input.parameters[param.name]!!, errors)
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    private fun validateParameter(
        schema: ParameterSchema,
        value: String,
        errors: MutableList<String>
    ) {
        val validation = schema.validation ?: return

        validation.minLength?.let { min ->
            if (value.length < min) {
                errors.add("Parameter ${schema.name} too short (min: $min)")
            }
        }

        validation.maxLength?.let { max ->
            if (value.length > max) {
                errors.add("Parameter ${schema.name} too long (max: $max)")
            }
        }

        validation.pattern?.let { pattern ->
            if (!Regex(pattern).matches(value)) {
                errors.add("Parameter ${schema.name} doesn't match pattern")
            }
        }

        validation.allowedValues?.let { allowed ->
            if (value !in allowed) {
                errors.add("Parameter ${schema.name} not in allowed values")
            }
        }
    }
}
