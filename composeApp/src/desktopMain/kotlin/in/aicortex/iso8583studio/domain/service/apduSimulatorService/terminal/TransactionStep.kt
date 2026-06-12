package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu

/**
 * One observable event from the EMV Book 3 transaction state machine. Emitted as a Flow so the UI
 * can render a stepper / live timeline.
 */
sealed class TransactionStep {
    abstract val time: Long
    abstract val phase: Phase

    /** Phase started — render a section header in the timeline. */
    data class PhaseStart(override val time: Long, override val phase: Phase) : TransactionStep()

    /** APDU exchanged within the current phase. */
    data class Exchange(
        override val time: Long,
        override val phase: Phase,
        val command: CommandApdu,
        val response: ResponseApdu,
    ) : TransactionStep()

    /** Free-form note (decoded values, decisions made, computed fields). */
    data class Note(
        override val time: Long,
        override val phase: Phase,
        val message: String,
        val isError: Boolean = false,
    ) : TransactionStep()

    /** TVR / TSI updated. UI shows the live bit-flag display. */
    data class Flags(
        override val time: Long,
        override val phase: Phase,
        val tvr: ByteArray,
        val tsi: ByteArray,
    ) : TransactionStep()

    /** Phase completed. */
    data class PhaseEnd(override val time: Long, override val phase: Phase, val ok: Boolean) : TransactionStep()

    /** Final transaction outcome. */
    data class Outcome(
        override val time: Long,
        override val phase: Phase = Phase.OUTCOME,
        val acType: AcType,
        val ac: ByteArray,
        val atc: Int,
        val cid: Int,
        val iad: ByteArray,
        val tvr: ByteArray,
        val tsi: ByteArray,
    ) : TransactionStep()

    /** Terminal aborted before completion (e.g. card pulled, malformed response). */
    data class Aborted(override val time: Long, override val phase: Phase, val reason: String) : TransactionStep()
}

enum class Phase(val label: String) {
    APP_SELECTION("Application Selection"),
    INITIATE("Initiate Application"),
    READ_DATA("Read Application Data"),
    OFFLINE_AUTH("Offline Data Authentication"),
    PROCESSING_RESTRICTIONS("Processing Restrictions"),
    CARDHOLDER_VERIFY("Cardholder Verification"),
    TERMINAL_RISK("Terminal Risk Management"),
    TERMINAL_ACTION("Terminal Action Analysis"),
    FIRST_GAC("Card Action Analysis (1st GAC)"),
    ONLINE("Online (issuer)"),
    SECOND_GAC("Card Action Analysis (2nd GAC)"),
    OUTCOME("Outcome"),
}

enum class AcType { AAC, TC, ARQC }
