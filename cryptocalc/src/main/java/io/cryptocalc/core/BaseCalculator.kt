package io.cryptocalc.core

import ai.cortex.core.types.CalculatorInput
import ai.cortex.core.types.CalculatorResult
import ai.cortex.core.types.ResultMetadata
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

abstract class BaseCalculator<T : CalculatorInput, R : CalculatorResult> : Calculator<T, R> {
    @OptIn(ExperimentalTime::class)
    override suspend fun execute(input: T): R {
        val startTime = Clock.System.now()

        try {
            // Validate input
            val validationResult = validate(input)
            if (!validationResult.success) {
                return validationResult as R
            }

            // Execute operation
            val result = executeOperation(input)

            val endTime = Clock.System.now()
            val executionTime = endTime.minus(startTime).inWholeMilliseconds
            result.metadata = ResultMetadata(
                executionTimeMs = executionTime,
                version = version
            )
            return result as R

        } catch (e: Exception) {
            val result = object : CalculatorResult {
                override val success: Boolean
                    get() = false
                override val error: String
                    get() = "Execution failed: ${e.message}"
                override var metadata: ResultMetadata
                    get() = ResultMetadata(
                        executionTimeMs = Clock.System.now()
                            .minus(startTime).inWholeMilliseconds,
                        version = version
                    )
                    set(value) {}
            }
            return result as R
        }
    }

    protected abstract suspend fun executeOperation(input: T): R

    abstract override fun validate(input: T): R
}
