package `in`.aicortex.iso8583studio.domain.service.simulation

import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ConnectionChaosConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ErrorInjectionConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ErrorInjectionType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.RampPattern
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ResponseDelayConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ResponseDelayType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ResponseRampConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.TimeoutSimulationConfig
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.random.Random

/**
 * The impairments a simulator applies to live traffic, as one immutable snapshot.
 *
 * Held separately from any simulator's persisted config because a running simulator generally owns a
 * *snapshot* of that config, so later config edits are never observed. Settings UIs swap this object
 * instead, and the socket path re-reads it per request — which is what makes a change take effect on
 * live traffic without a restart. Each simulator supplies its own mapping to and from its config.
 */
data class SimulationSettings(
    val enabled: Boolean = false,
    val delay: ResponseDelayConfig = ResponseDelayConfig(),
    val ramp: ResponseRampConfig = ResponseRampConfig(),
    val timeouts: TimeoutSimulationConfig = TimeoutSimulationConfig(),
    val connection: ConnectionChaosConfig = ConnectionChaosConfig(),
    val errors: ErrorInjectionConfig = ErrorInjectionConfig(),
    val randomSeed: Long = 0,
) {
    /** True when anything at all would change the simulator's behaviour. */
    val anyActive: Boolean
        get() = enabled && (
            delay.enabled ||
                ramp.enabled ||
                timeouts.enabled ||
                connection.enabled ||
                errors.enableErrorInjection
            )

    /** Extension point for each simulator's own config mapping (see `fromHsmAdvanced`). */
    companion object
}

/** What to do with a freshly accepted socket. */
sealed interface ConnectionDecision {
    object Accept : ConnectionDecision
    data class Refuse(val reason: String) : ConnectionDecision
}

/** What to do with a response that has already been computed. */
sealed interface ResponseDecision {
    object Send : ResponseDecision
    data class Drop(val reason: String, val closeConnection: Boolean) : ResponseDecision
    data class SubstituteError(val errorCode: String, val reason: String) : ResponseDecision
    data class ResetConnection(val reason: String) : ResponseDecision
}

/**
 * Decides — and enacts the timing of — the simulator's impairments.
 *
 * [settingsProvider] is read once per decision so a change made mid-test takes effect on the next
 * request while never letting a single decision see half-old, half-new settings. [clock] and
 * [randomFactory] are injectable so the whole engine is deterministic under test.
 *
 * [errorCodeSelector] is the one protocol-specific hook: it turns an abstract [ErrorInjectionType]
 * into a wire error code. Simulators whose errors are expressed some other way — the Host Simulator
 * uses a response-code placeholder — leave it returning null and never enable error injection.
 */
class SimulationBehaviorEngine(
    private val settingsProvider: () -> SimulationSettings,
    private val clock: () -> Long = System::currentTimeMillis,
    private val randomFactory: (Long) -> Random = { seed ->
        if (seed == 0L) Random.Default else Random(seed)
    },
    private val errorCodeSelector: (ErrorInjectionType) -> String? = { null },
) {
    @Volatile
    private var startedAtMs: Long = clock()

    @Volatile
    private var random: Random = randomFactory(settingsProvider().randomSeed)

    @Volatile
    private var seedInUse: Long = settingsProvider().randomSeed

    /** Error-burst window, as the timestamp at which the current burst ends (0 = not bursting). */
    @Volatile
    private var burstUntilMs: Long = 0

    /** Resets the ramp clock. Called when the simulator starts, so t=0 means "this test began". */
    fun onSimulatorStarted() {
        startedAtMs = clock()
        burstUntilMs = 0
        reseedIfNeeded(settingsProvider())
    }

    private fun reseedIfNeeded(settings: SimulationSettings) {
        if (settings.randomSeed != seedInUse) {
            seedInUse = settings.randomSeed
            random = randomFactory(settings.randomSeed)
        }
    }

    private fun elapsedMs(): Long = max(0L, clock() - startedAtMs)

    // ── Connection level ──────────────────────────────────────────────────

    /**
     * Applies any accept-time delay and decides whether to keep the connection. Callers must not
     * register a refused client — close its socket and move on.
     */
    suspend fun onConnectionAccepted(): ConnectionDecision {
        val settings = settingsProvider()
        reseedIfNeeded(settings)
        if (!settings.enabled || !settings.connection.enabled) return ConnectionDecision.Accept

        val chaos = settings.connection
        if (chaos.acceptDelayMs > 0) {
            delay(chaos.acceptDelayMs.toLong())
        }
        if (rollHit(chaos.refuseRate, settings)) {
            return ConnectionDecision.Refuse("connection chaos: refuse rate ${percent(chaos.refuseRate)}")
        }
        return ConnectionDecision.Accept
    }

    // ── Response level ────────────────────────────────────────────────────

    /**
     * Applies the simulated latency and decides the fate of the response for [commandCode].
     *
     * Precedence is fixed so an outcome is never ambiguous when several impairments are armed:
     * reset › drop › hang › error › send. The latency delay is applied before whichever of the last
     * three wins.
     */
    suspend fun beforeResponse(commandCode: String, activeConnections: Int = 0): ResponseDecision {
        val settings = settingsProvider()
        reseedIfNeeded(settings)
        if (!settings.enabled) return ResponseDecision.Send

        // Reset wins outright — there is no point delaying a connection we are about to drop.
        if (settings.connection.enabled &&
            rollHit(settings.connection.resetMidStreamRate, settings)
        ) {
            return ResponseDecision.ResetConnection(
                "connection chaos: mid-stream reset ${percent(settings.connection.resetMidStreamRate)}",
            )
        }

        val latencyMs = latencyMs(settings, activeConnections)
        if (latencyMs > 0) delay(latencyMs)

        if (settings.timeouts.enabled) {
            if (rollHit(settings.timeouts.dropResponseRate, settings)) {
                return ResponseDecision.Drop(
                    "timeout simulation: dropped ${percent(settings.timeouts.dropResponseRate)}",
                    closeConnection = settings.timeouts.closeConnectionOnDrop,
                )
            }
            if (rollHit(settings.timeouts.hangResponseRate, settings)) {
                delay(settings.timeouts.hangDelayMs.toLong().coerceAtLeast(0))
            }
        }

        injectedErrorCode(settings, commandCode)?.let { (code, reason) ->
            return ResponseDecision.SubstituteError(code, reason)
        }

        return ResponseDecision.Send
    }

    // ── Latency ───────────────────────────────────────────────────────────

    /** Base delay, jittered, then scaled by the ramp. Visible for tests. */
    internal fun latencyMs(settings: SimulationSettings, activeConnections: Int = 0): Long {
        if (!settings.enabled || !settings.delay.enabled) return 0

        val d = settings.delay
        val base = when (d.delayType) {
            ResponseDelayType.NONE -> 0
            ResponseDelayType.FIXED -> d.fixedDelayMs
            ResponseDelayType.RANDOM -> {
                val lo = minOf(d.minDelayMs, d.maxDelayMs)
                val hi = maxOf(d.minDelayMs, d.maxDelayMs)
                if (hi <= lo) lo else random.nextInt(lo, hi + 1)
            }
            ResponseDelayType.REALISTIC -> d.networkLatencyMs + d.processingDelayMs
            // Grows with how busy the simulator is — a queue forming in front of a real HSM.
            ResponseDelayType.PROGRESSIVE -> d.fixedDelayMs * (1 + activeConnections)
            ResponseDelayType.CUSTOM -> d.fixedDelayMs
        }.toLong()

        val jittered = if (d.enableJitter && d.jitterPercentage > 0 && base > 0) {
            val span = base * d.jitterPercentage / 100.0
            (base + (random.nextDouble() * 2.0 - 1.0) * span).toLong()
        } else {
            base
        }

        val scaled = if (settings.ramp.applyToLatency) {
            (jittered * rampMultiplier(elapsedMs(), settings.ramp)).toLong()
        } else {
            jittered
        }
        return scaled.coerceAtLeast(0)
    }

    // ── Ramp ──────────────────────────────────────────────────────────────

    /** Current impairment multiplier. Visible for tests. */
    internal fun rampMultiplier(elapsedMs: Long, ramp: ResponseRampConfig): Double {
        if (!ramp.enabled || ramp.pattern == RampPattern.CONSTANT) return 1.0

        val up = ramp.rampUpSeconds.coerceAtLeast(0) * 1000L
        val hold = ramp.holdSeconds.coerceAtLeast(0) * 1000L
        val down = ramp.rampDownSeconds.coerceAtLeast(0) * 1000L
        val start = ramp.startMultiplier
        val peak = ramp.peakMultiplier

        fun lerp(fraction: Double) = start + (peak - start) * fraction.coerceIn(0.0, 1.0)

        return when (ramp.pattern) {
            RampPattern.CONSTANT -> 1.0

            RampPattern.RAMP_UP -> {
                val t = cycled(elapsedMs, up, ramp.repeat) ?: return peak
                if (up <= 0) peak else lerp(t.toDouble() / up)
            }

            RampPattern.RAMP_UP_DOWN -> {
                val total = up + hold + down
                if (total <= 0L) return peak
                val t = cycled(elapsedMs, total, ramp.repeat) ?: return start
                when {
                    t < up -> if (up <= 0) peak else lerp(t.toDouble() / up)
                    t < up + hold -> peak
                    else -> if (down <= 0) start else lerp(1.0 - (t - up - hold).toDouble() / down)
                }
            }

            // Baseline, punctuated by short excursions to peak lasting [holdSeconds].
            RampPattern.SPIKE -> {
                val period = up + hold
                if (period <= 0L) return start
                val t = elapsedMs % period
                if (t >= up) peak else start
            }

            // Climb to peak then snap back, over and over.
            RampPattern.SAWTOOTH -> {
                if (up <= 0L) return peak
                lerp((elapsedMs % up).toDouble() / up)
            }
        }
    }

    /**
     * Position within a profile of [length]. Returns null once a non-repeating profile has run out,
     * signalling the caller to pin at the end value.
     */
    private fun cycled(elapsedMs: Long, length: Long, repeat: Boolean): Long? = when {
        length <= 0L -> null
        elapsedMs < length -> elapsedMs
        repeat -> elapsedMs % length
        else -> null
    }

    // ── Error injection ───────────────────────────────────────────────────

    private fun injectedErrorCode(
        settings: SimulationSettings,
        commandCode: String,
    ): Pair<String, String>? {
        val errors = settings.errors
        if (!errors.enableErrorInjection) return null

        val rate = currentErrorRate(errors)
        if (!rollHit(rate, settings)) return null

        // An explicit per-command override always wins over the type mapping.
        errors.customErrorCodes[commandCode]?.let { code ->
            return code to "error injection: custom code for $commandCode"
        }

        val type = errors.enabledErrorTypes.randomOrNull(random) ?: return null
        val code = errorCodeSelector(type) ?: return null
        return code to "error injection: ${type.displayName} (${percent(rate)})"
    }

    /** Burst mode raises the error rate for [ErrorInjectionConfig.errorBurstDuration] at a time. */
    private fun currentErrorRate(errors: ErrorInjectionConfig): Double {
        if (!errors.errorBurstMode) return errors.errorRate
        val now = clock()
        if (now < burstUntilMs) return errors.errorBurstRate
        // Outside a burst, the base rate also decides when the next burst opens.
        return if (random.nextDouble() < errors.errorRate) {
            burstUntilMs = now + errors.errorBurstDuration.coerceAtLeast(0) * 1000L
            errors.errorBurstRate
        } else {
            errors.errorRate
        }
    }

    private fun <T> Set<T>.randomOrNull(rnd: Random): T? =
        if (isEmpty()) null else elementAt(rnd.nextInt(size))

    private fun rollHit(rate: Double, settings: SimulationSettings): Boolean {
        if (rate <= 0.0) return false
        val effective = if (settings.ramp.applyToFailureRates) {
            rate * rampMultiplier(elapsedMs(), settings.ramp)
        } else {
            rate
        }
        if (effective >= 1.0) return true
        return random.nextDouble() < effective
    }

    private fun percent(rate: Double) = "${(rate * 100).toInt()}%"
}
