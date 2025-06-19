package io.cryptocalc.core

import io.cryptocalc.core.types.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CalculatorOrchestrator(
    private val registry: CalculatorRegistry
) {

    suspend fun execute(calculatorId: String, input: CalculatorInput): CalculatorResult {
        val calculator = registry.get(calculatorId)
            ?: return CalculatorResult(
                success = false,
                error = "Calculator not found: $calculatorId"
            )

        return calculator.execute(input)
    }

    suspend fun batchExecute(
        requests: List<Pair<String, CalculatorInput>>
    ): List<CalculatorResult> = coroutineScope {
        requests.map { (calculatorId, input) ->
            async { execute(calculatorId, input) }
        }.awaitAll()
    }

    fun validateInput(calculatorId: String, input: CalculatorInput): ValidationResult {
        val calculator = registry.get(calculatorId)
            ?: return ValidationResult(
                isValid = false,
                errors = listOf("Calculator not found: $calculatorId")
            )

        return calculator.validate(input)
    }

    fun getCalculatorInfo(calculatorId: String): Map<String, Any>? {
        val calculator = registry.get(calculatorId) ?: return null

        return mapOf(
            "id" to calculator.id,
            "name" to calculator.name,
            "category" to calculator.category,
            "version" to calculator.version,
            "schema" to calculator.getSchema(),
            "capabilities" to calculator.getCapabilities()
        )
    }
}
