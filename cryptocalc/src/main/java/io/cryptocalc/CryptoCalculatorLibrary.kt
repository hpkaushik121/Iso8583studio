package io.cryptocalc

import io.cryptocalc.core.*
import io.cryptocalc.core.types.*
import io.cryptocalc.crypto.calculators.*
import io.cryptocalc.emv.calculators.*

class CryptoCalculatorLibrary {
    private val registry = CalculatorRegistry()
    private val orchestrator = CalculatorOrchestrator(registry)

    init {
        // Register built-in calculators
        registerBuiltInCalculators()
    }

    private fun registerBuiltInCalculators() {
        // Cryptographic calculators
        registry.register(AESCalculator())

        // EMV calculators
        registry.register(MasterCardCalculator())

        // TODO: Add more calculators here as they are implemented
    }

    fun getOrchestrator(): CalculatorOrchestrator = orchestrator
    fun getRegistry(): CalculatorRegistry = registry

    suspend fun calculate(calculatorId: String, input: CalculatorInput): CalculatorResult =
        orchestrator.execute(calculatorId, input)

    fun listCalculators(): List<Map<String, Any>> =
        registry.getAll().map { calculator ->
            mapOf(
                "id" to calculator.id,
                "name" to calculator.name,
                "category" to calculator.category,
                "version" to calculator.version
            )
        }

    companion object {
        fun create(): CryptoCalculatorLibrary = CryptoCalculatorLibrary()
    }
}
