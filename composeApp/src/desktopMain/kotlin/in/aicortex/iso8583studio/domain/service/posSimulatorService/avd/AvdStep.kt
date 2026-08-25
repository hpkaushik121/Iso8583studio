package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

/**
 * One observable step of a long-running AVD operation.
 *
 * Mirrors `apduSimulatorService.terminal.TransactionStep` deliberately, so the phase-stepper UI
 * built for EMV transactions renders AVD create and boot without modification. As there, failures
 * become a terminal [Aborted] step rather than a thrown exception — a half-finished sequence with a
 * visible reason is far more useful than a stack trace.
 */
sealed class AvdStep {
    abstract val time: Long
    abstract val phase: AvdPhase

    data class PhaseStart(override val time: Long, override val phase: AvdPhase) : AvdStep()

    /** A command about to run, shown verbatim so the user can reproduce it in a terminal. */
    data class Command(
        override val time: Long,
        override val phase: AvdPhase,
        val argv: List<String>,
    ) : AvdStep() {
        val commandLine: String get() = argv.joinToString(" ")
    }

    data class Line(
        override val time: Long,
        override val phase: AvdPhase,
        val text: String,
        val isError: Boolean = false,
    ) : AvdStep()

    data class PhaseEnd(
        override val time: Long,
        override val phase: AvdPhase,
        val ok: Boolean,
        val exitCode: Int? = null,
    ) : AvdStep()

    /** Terminal failure. Carries what to do about it, not just what went wrong. */
    data class Aborted(
        override val time: Long,
        override val phase: AvdPhase,
        val reason: String,
        val remediation: String = "",
    ) : AvdStep()

    data class Done(
        override val time: Long,
        override val phase: AvdPhase = AvdPhase.DONE,
        val summary: String,
    ) : AvdStep()
}

/**
 * Phases of both the prepare and boot pipelines. One enum covers both so a single stepper renders
 * either; [isPrepare] and [isBoot] partition them for display.
 */
enum class AvdPhase(val label: String) {
    // -- prepare --
    VALIDATE("Validate configuration"),
    RESOLVE_IMAGE("Resolve system image"),
    CREATE_AVD("Create AVD"),
    WRITE_CONFIG("Write config.ini"),
    WRITE_POINTER("Register AVD"),
    VERIFY("Verify on disk"),

    // -- boot --
    LOCATE_SDK("Locate Android SDK"),
    VERIFY_PREPARED("Check AVD is prepared"),
    START_EMULATOR("Start emulator"),
    WAIT_BOOT("Wait for boot"),
    VERIFY_IDENTITY("Verify device identity"),

    DONE("Ready");

    val isPrepare: Boolean
        get() = this in setOf(VALIDATE, RESOLVE_IMAGE, CREATE_AVD, WRITE_CONFIG, WRITE_POINTER, VERIFY)

    val isBoot: Boolean
        get() = this in setOf(LOCATE_SDK, VERIFY_PREPARED, START_EMULATOR, WAIT_BOOT, VERIFY_IDENTITY)
}

/** A phase folded up for display: its outcome plus the output it produced. */
data class AvdPhaseBlock(
    val phase: AvdPhase,
    val startTime: Long,
    /** null while still running. */
    val ok: Boolean?,
    val commands: List<AvdStep.Command>,
    val lines: List<AvdStep.Line>,
    val exitCode: Int?,
    val abortReason: String? = null,
    val remediation: String? = null,
)

/** Groups a step list into per-phase blocks, in the order the phases started. */
fun groupAvdSteps(steps: List<AvdStep>): List<AvdPhaseBlock> {
    val order = mutableListOf<AvdPhase>()
    val starts = mutableMapOf<AvdPhase, Long>()
    val commands = mutableMapOf<AvdPhase, MutableList<AvdStep.Command>>()
    val lines = mutableMapOf<AvdPhase, MutableList<AvdStep.Line>>()
    val results = mutableMapOf<AvdPhase, Boolean>()
    val exits = mutableMapOf<AvdPhase, Int?>()
    val aborts = mutableMapOf<AvdPhase, Pair<String, String>>()

    fun touch(phase: AvdPhase, time: Long) {
        if (phase !in starts) {
            starts[phase] = time
            order += phase
        }
    }

    for (step in steps) {
        touch(step.phase, step.time)
        when (step) {
            is AvdStep.PhaseStart -> Unit
            is AvdStep.Command -> commands.getOrPut(step.phase) { mutableListOf() } += step
            is AvdStep.Line -> lines.getOrPut(step.phase) { mutableListOf() } += step
            is AvdStep.PhaseEnd -> {
                results[step.phase] = step.ok
                exits[step.phase] = step.exitCode
            }
            is AvdStep.Aborted -> {
                results[step.phase] = false
                aborts[step.phase] = step.reason to step.remediation
            }
            is AvdStep.Done -> results[step.phase] = true
        }
    }

    return order.map { phase ->
        AvdPhaseBlock(
            phase = phase,
            startTime = starts.getValue(phase),
            ok = results[phase],
            commands = commands[phase].orEmpty(),
            lines = lines[phase].orEmpty(),
            exitCode = exits[phase],
            abortReason = aborts[phase]?.first,
            remediation = aborts[phase]?.second?.takeIf { it.isNotBlank() },
        )
    }
}
