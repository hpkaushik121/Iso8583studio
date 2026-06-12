package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fault-injection rule applied to the card runtime when this simulator is acting as a card. Lets
 * a POS developer test edge cases like timeouts, wrong cryptograms, unexpected SWs, etc.
 *
 * Matched in registration order; first matching rule fires per APDU. A rule with all match fields
 * null applies to every APDU (use sparingly).
 */
@Serializable
data class BehaviorRule(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    /** Match by INS byte (0..255). null = any. */
    val whenIns: Int? = null,
    /** Match by P1 byte. null = any. */
    val whenP1: Int? = null,
    /** Match by P2 byte. null = any. */
    val whenP2: Int? = null,
    /** How many times this rule fires before disabling itself. -1 = forever. */
    val triggerLimit: Int = -1,
    val action: BehaviorAction,
)

@Serializable
sealed class BehaviorAction {
    /** Override the response status word (e.g. force 6985 once to test retry logic). */
    @Serializable @SerialName("returnSw")
    data class ReturnSw(val swHex: String, val responseDataHex: String = "") : BehaviorAction()

    /** Inject latency before returning (milliseconds). */
    @Serializable @SerialName("delay")
    data class Delay(val millis: Long) : BehaviorAction()

    /** Flip one byte of the application cryptogram in a GENERATE AC response. */
    @Serializable @SerialName("corruptAc")
    data class CorruptAc(val byteIndex: Int = 0, val xorMask: Int = 0xFF) : BehaviorAction()

    /** Force the card to return a specific cryptogram type regardless of what the terminal asked. */
    @Serializable @SerialName("forceCryptogram")
    data class ForceCryptogramType(val type: ForcedAcType) : BehaviorAction()

    /** Drop the response entirely (terminal will see a timeout). */
    @Serializable @SerialName("drop")
    data object DropResponse : BehaviorAction()
}

@Serializable
enum class ForcedAcType { AAC, TC, ARQC }
