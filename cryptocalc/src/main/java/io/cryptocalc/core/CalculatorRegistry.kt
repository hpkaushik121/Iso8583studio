package io.cryptocalc.core

import io.cryptocalc.core.types.CalculatorCategory

class CalculatorRegistry {
    private val calculators = mutableMapOf<String, Calculator>()

    fun register(calculator: Calculator) {
        calculators[calculator.id] = calculator
    }

    fun unregister(id: String) {
        calculators.remove(id)
    }

    fun get(id: String): Calculator? = calculators[id]

    fun getByCategory(category: CalculatorCategory): List<Calculator> =
        calculators.values.filter { it.category == category }

    fun getAll(): List<Calculator> = calculators.values.toList()

    fun contains(id: String): Boolean = calculators.containsKey(id)
}
