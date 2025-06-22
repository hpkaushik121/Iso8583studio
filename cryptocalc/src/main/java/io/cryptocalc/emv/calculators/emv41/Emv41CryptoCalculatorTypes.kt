package io.cryptocalc.emv.calculators.emv41

import ai.cortex.core.types.CalculatorInput
import ai.cortex.core.types.CalculatorResult
import ai.cortex.core.types.KeyParity
import ai.cortex.core.types.OperationType
import ai.cortex.core.types.UdkDerivationType
import kotlinx.serialization.Serializable


@Serializable
data class UdkDerivationInput(
    val udkDerivationType: UdkDerivationType,
    val keyParity: KeyParity,
    val masterKey: String,
    val pan: String,
    val panSequence: String,
)

@Serializable
data class SessionKeyDerivationInput(
    val udk: String,
    val atc: String,
)


@Serializable
data class UdkDerivation(
    val udk: String,
    val kcv: String
)

@Serializable
data class SessionDerivation(
    val sessionKey: String,
    val derivationData: String
)


@Serializable
data class EMVCalculatorInput(
    override val operation: OperationType,
    val udkDerivationInput: UdkDerivationInput? = null,
    val sessionDerivation: SessionKeyDerivationInput? = null
) : CalculatorInput

@Serializable
data class EMVCalculatorResult(
    override val success: Boolean,
    override val error: String? = null,
    val key: String? = null,
    val udkDerivation: UdkDerivation? = null,
    val sessionDerivation: SessionDerivation? = null,
) : CalculatorResult
