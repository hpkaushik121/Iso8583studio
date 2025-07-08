package io.cryptocalc.emv.calculators.acCalculator

import ai.cortex.core.types.CalculatorInput
import ai.cortex.core.types.CalculatorResult
import ai.cortex.core.types.CryptogramType
import ai.cortex.core.types.OperationType
import ai.cortex.core.types.PaddingMethods
import ai.cortex.core.types.ResultMetadata
import kotlinx.serialization.Serializable

@Serializable
data class AcCalculatorInput(
    val sessionKey: ByteArray,
    val terminalData: ByteArray,
    val iccData: ByteArray,
    val cryptogramType: CryptogramType,
    val paddingMethods: PaddingMethods,
    override val operation: OperationType
): CalculatorInput {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AcCalculatorInput

        if (!sessionKey.contentEquals(other.sessionKey)) return false
        if (!terminalData.contentEquals(other.terminalData)) return false
        if (!iccData.contentEquals(other.iccData)) return false
        if (cryptogramType != other.cryptogramType) return false
        if (paddingMethods != other.paddingMethods) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sessionKey.contentHashCode()
        result = 31 * result + terminalData.contentHashCode()
        result = 31 * result + iccData.contentHashCode()
        result = 31 * result + cryptogramType.hashCode()
        result = 31 * result + paddingMethods.hashCode()
        return result
    }
}
data class AcCalculatorResult(
    val ac: ByteArray? = null,
    override val success: Boolean,
    override val error: String?,
    override var metadata: ResultMetadata = ResultMetadata()
): CalculatorResult {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AcCalculatorResult

        if (!ac.contentEquals(other.ac)) return false

        return true
    }

    override fun hashCode(): Int {
        return ac.contentHashCode()
    }
}
