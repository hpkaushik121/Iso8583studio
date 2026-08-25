package `in`.aicortex.iso8583studio.data.model

import `in`.aicortex.iso8583studio.domain.service.simulation.SimulationSettings
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ConnectionChaosConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ErrorInjectionConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ResponseDelayConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ResponseRampConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.TimeoutSimulationConfig
import kotlinx.serialization.Serializable

/**
 * How the Host Simulator degrades its own responses, so a client system's timeout, retry and decline
 * handling can be exercised without a real misbehaving host.
 *
 * Everything except [responseCode] is protocol-neutral and shared with the HSM simulator. Response
 * codes are ISO 8583-specific, and rather than the simulator picking a field to rewrite, the
 * transaction template decides *which* field varies (by holding the `[SIMRC]` placeholder) while
 * these settings decide *how often* it comes back as a decline.
 */
@Serializable
data class HostSimulationConfig(
    /** Master kill switch — returns the simulator to a clean baseline with one toggle. */
    val enabled: Boolean = false,
    /** Non-zero makes the simulated randomness reproducible across runs. */
    val randomSeed: Long = 0,
    val delay: ResponseDelayConfig = ResponseDelayConfig(),
    val ramp: ResponseRampConfig = ResponseRampConfig(),
    val timeouts: TimeoutSimulationConfig = TimeoutSimulationConfig(),
    val connection: ConnectionChaosConfig = ConnectionChaosConfig(),
    val responseCode: ResponseCodeSimulationConfig = ResponseCodeSimulationConfig(),
) {
    /** True when anything at all would change the simulator's behaviour. */
    val anyActive: Boolean
        get() = enabled && (
            delay.enabled ||
                ramp.enabled ||
                timeouts.enabled ||
                connection.enabled ||
                responseCode.enabled
            )
}

/**
 * Drives the `[SIMRC]` placeholder: how often a response code comes back as a decline, and which
 * decline codes to draw from.
 */
@Serializable
data class ResponseCodeSimulationConfig(
    val enabled: Boolean = false,
    val successCode: String = "00",
    /** 0..1 — share of responses that carry a decline code instead of [successCode]. */
    val errorRate: Double = 0.0,
    val errorCodes: List<WeightedResponseCode> = DEFAULT_DECLINE_CODES,
) {
    companion object {
        /** A plausible default mix — the codes a client is most likely to have to handle. */
        val DEFAULT_DECLINE_CODES: List<WeightedResponseCode> = listOf(
            WeightedResponseCode("05", 3),   // Do not honour
            WeightedResponseCode("91", 2),   // Issuer or switch inoperative
            WeightedResponseCode("96", 1),   // System malfunction
        )
    }
}

/** One decline code and its relative likelihood within the pool. */
@Serializable
data class WeightedResponseCode(
    val code: String = "05",
    val weight: Int = 1,
)

/**
 * Projects onto the protocol-neutral settings the shared engine understands.
 *
 * `errors` stays at its default: the Host Simulator expresses errors as ISO 8583 response codes via
 * the `[SIMRC]` placeholder, not by the engine swapping a code onto the wire.
 */
fun HostSimulationConfig.toSimulationSettings() = SimulationSettings(
    enabled = enabled,
    delay = delay,
    ramp = ramp,
    timeouts = timeouts,
    connection = connection,
    errors = ErrorInjectionConfig(),
    randomSeed = randomSeed,
)
