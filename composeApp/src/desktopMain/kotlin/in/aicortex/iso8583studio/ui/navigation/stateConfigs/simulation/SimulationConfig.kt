package `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation

import kotlinx.serialization.Serializable

/**
 * Protocol-neutral simulation settings, shared by every simulator that can degrade its own responses.
 *
 * These describe *how* a simulator misbehaves — how slowly it answers, how that changes over a test,
 * how often it never answers, and how it mistreats connections — none of which depends on the wire
 * protocol. Anything protocol-specific (which error code a payShield returns, which ISO 8583 response
 * code a host returns) lives with that simulator instead.
 *
 * These are concrete, non-polymorphic `@Serializable` types, so persisted JSON is keyed on field
 * names rather than class names — this package can move without invalidating a saved config.
 */

@Serializable
enum class ResponseDelayType(val displayName: String, val description: String) {
    NONE("No Delay", "Respond immediately without any delay"),
    FIXED("Fixed Delay", "Fixed delay for all responses"),
    RANDOM("Random Delay", "Random delay within specified range"),
    PROGRESSIVE("Progressive Delay", "Increasing delay based on load"),
    REALISTIC("Realistic Simulation", "Realistic network and processing delays"),
    CUSTOM("Custom Pattern", "Custom delay patterns based on message type")
}

@Serializable
data class ResponseDelayConfig(
    /** Master toggle for this section, independent of [delayType]. */
    val enabled: Boolean = false,
    val delayType: ResponseDelayType = ResponseDelayType.NONE,
    val fixedDelayMs: Int = 100,
    val minDelayMs: Int = 50,
    val maxDelayMs: Int = 500,
    val networkLatencyMs: Int = 20,
    val processingDelayMs: Int = 80,
    val enableJitter: Boolean = false,
    val jitterPercentage: Int = 10
)

/**
 * Scales simulated impairment over time. A simulator answers load rather than generating it, so the
 * ramp multiplies the *latency* (and optionally the failure probabilities) as a test progresses,
 * letting a client system be watched degrading gradually instead of all at once.
 *
 * t=0 is the moment the simulator was started.
 */
@Serializable
data class ResponseRampConfig(
    val enabled: Boolean = false,
    val pattern: RampPattern = RampPattern.RAMP_UP,
    val rampUpSeconds: Int = 60,
    val holdSeconds: Int = 120,
    val rampDownSeconds: Int = 60,
    val startMultiplier: Double = 1.0,
    val peakMultiplier: Double = 5.0,
    /** When true the profile loops; otherwise it pins at its final value. */
    val repeat: Boolean = false,
    val applyToLatency: Boolean = true,
    val applyToFailureRates: Boolean = false,
)

@Serializable
enum class RampPattern(val displayName: String, val description: String) {
    CONSTANT("Constant", "No ramping — impairment stays at its base level"),
    RAMP_UP("Ramp Up", "Climb from start to peak, then stay there"),
    RAMP_UP_DOWN("Ramp Up & Down", "Climb to peak, hold, then return to start"),
    SPIKE("Spike", "Sit at start with periodic short excursions to peak"),
    SAWTOOTH("Sawtooth", "Climb to peak then snap back, repeatedly"),
}

/** Requests that never come back, or come back far too late — exercises client timeout and retry. */
@Serializable
data class TimeoutSimulationConfig(
    val enabled: Boolean = false,
    /** 0..1 — share of requests that get no response at all. */
    val dropResponseRate: Double = 0.0,
    /** 0..1 — share of requests answered only after [hangDelayMs]. */
    val hangResponseRate: Double = 0.0,
    val hangDelayMs: Int = 30_000,
    /** When true a dropped response also closes the connection instead of leaving it open. */
    val closeConnectionOnDrop: Boolean = false,
)

/** Misbehaviour at the connection level rather than the response level. */
@Serializable
data class ConnectionChaosConfig(
    val enabled: Boolean = false,
    val acceptDelayMs: Int = 0,
    /** 0..1 — share of connections accepted then immediately closed. */
    val refuseRate: Double = 0.0,
    /** 0..1 — per-request chance of dropping an established connection. */
    val resetMidStreamRate: Double = 0.0,
    /** Throttles response writes; 0 = unthrottled. */
    val slowWriteBytesPerSecond: Int = 0,
)

@Serializable
enum class ErrorInjectionType(val displayName: String, val description: String) {
    NETWORK_TIMEOUT("Network Timeout", "Simulate network timeout errors"),
    CONNECTION_FAILURE("Connection Failure", "Simulate connection failures"),
    MALFORMED_RESPONSE("Malformed Response", "Send invalid or corrupted responses"),
    AUTHENTICATION_FAILURE("Authentication Failure", "Simulate authentication errors"),
    INSUFFICIENT_FUNDS("Insufficient Funds", "Simulate transaction decline scenarios"),
    SYSTEM_ERROR("System Error", "Simulate internal system errors"),
    CARD_BLOCKED("Card Blocked", "Simulate blocked card scenarios"),
    INVALID_PIN("Invalid PIN", "Simulate PIN verification failures"),
    EXPIRED_CARD("Expired Card", "Simulate expired card scenarios"),
    CUSTOM_ERROR("Custom Error", "User-defined error scenarios")
}

/**
 * Rate at which a simulator answers with an error instead of the real result. The *codes* are
 * protocol-specific and supplied by the simulator; this only decides how often and which type.
 */
@Serializable
data class ErrorInjectionConfig(
    val enableErrorInjection: Boolean = false,
    val enabledErrorTypes: Set<ErrorInjectionType> = emptySet(),
    val errorRate: Double = 0.05, // 5% error rate
    val errorBurstMode: Boolean = false,
    val errorBurstDuration: Int = 30, // seconds
    val errorBurstRate: Double = 0.5, // 50% during burst
    val customErrorCodes: Map<String, String> = emptyMap()
)
