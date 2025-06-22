package io.cryptocalc.emv.calculators.masterCard

import ai.cortex.core.types.CalculatorInput
import ai.cortex.core.types.CalculatorResult
import ai.cortex.core.types.OperationType
import io.cryptocalc.emv.calculators.emv41.SessionDerivation
import io.cryptocalc.emv.calculators.emv41.UdkDerivation
import io.cryptocalc.emv.calculators.emv41.UdkDerivationInput
import kotlinx.serialization.Serializable



@Serializable
data class SessionKeyDerivationInput(
    val udk: String,
    val atc: String,
)

@Serializable
data class MasterCardCalculatorInput(
    override val operation: OperationType,
    val udkDerivationInput: UdkDerivationInput? = null,
    val sessionDerivation: SessionKeyDerivationInput? = null
) : CalculatorInput



@Serializable
data class MasterCardCalculatorCalculatorResult(
    override val success: Boolean,
    override val error: String? = null,
    val udkDerivation: UdkDerivation? = null,
    val sessionDerivation: SessionDerivation? = null,
) : CalculatorResult