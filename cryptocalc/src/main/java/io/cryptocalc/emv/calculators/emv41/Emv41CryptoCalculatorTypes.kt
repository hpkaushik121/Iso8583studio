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
/**
 * Session key types as per EMV specification
 */
@Serializable
enum class SessionKeyType {
    APPLICATION_CRYPTOGRAM,  // For AC and ARPC generation/verification
    SECURE_MESSAGING_MAC,    // For secure messaging MAC
    SECURE_MESSAGING_ENC     // For secure messaging encryption
}
@Serializable
data class SessionKeyInput(
    val masterKey: ByteArray ,
    val sessionKeyType: SessionKeyType,
    val iv: ByteArray = ByteArray(8),
    val atc: String = "0001",
    val branchFactor: Int = 0,
    val height: Int = 0,
    val keyParity: KeyParity
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SessionKeyInput

        if (branchFactor != other.branchFactor) return false
        if (height != other.height) return false
        if (!masterKey.contentEquals(other.masterKey)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (atc != other.atc) return false
        if (keyParity != other.keyParity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = branchFactor
        result = 31 * result + height
        result = 31 * result + masterKey.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + atc.hashCode()
        result = 31 * result + keyParity.hashCode()
        return result
    }
}


@Serializable
data class UdkDerivation(
    val udk: String,
    val kcv: String
)

@Serializable
data class SessionDerivation(
    val sessionKey: String,
    val kcv: String
)


@Serializable
data class EMVCalculatorInput(
    override val operation: OperationType,
    val udkDerivationInput: UdkDerivationInput? = null,
    val sessionKeyInput: SessionKeyInput? = null
) : CalculatorInput

@Serializable
data class EMVCalculatorResult(
    override val success: Boolean,
    override val error: String? = null,
    val key: String? = null,
    val udkDerivation: UdkDerivation? = null,
    val sessionDerivation: SessionDerivation? = null,
) : CalculatorResult
