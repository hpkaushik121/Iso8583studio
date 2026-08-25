package `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService

import `in`.aicortex.iso8583studio.domain.service.simulation.SimulationSettings
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmErrorCodes
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.AdvancedOptionsConfiguration
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ErrorInjectionType

/**
 * The payShield-specific half of simulated error injection.
 *
 * The shared [`SimulationBehaviorEngine`][`in`.aicortex.iso8583studio.domain.service.simulation.SimulationBehaviorEngine]
 * decides *whether* to answer with an error; this decides *which code* and *how to write it onto the
 * wire*, both of which are protocol-specific.
 */
object HsmErrorInjection {

    /**
     * Maps an injectable error type onto a payShield error code.
     *
     * Types that describe a transport event rather than a command result (`NETWORK_TIMEOUT`,
     * `CONNECTION_FAILURE`, `MALFORMED_RESPONSE`) return null — those are handled by the timeout and
     * connection-chaos sections, not by swapping an error code.
     *
     * `INSUFFICIENT_FUNDS`, `CARD_BLOCKED` and `EXPIRED_CARD` are ISO 8583 host concepts with no HSM
     * meaning; they map to an input-data error and are hidden in the HSM settings dialog.
     */
    fun errorCodeFor(type: ErrorInjectionType): String? = when (type) {
        ErrorInjectionType.AUTHENTICATION_FAILURE -> HsmErrorCodes.HSM_NOT_IN_AUTHORIZED_STATE
        ErrorInjectionType.SYSTEM_ERROR -> HsmErrorCodes.INVALID_LMK_TYPE
        ErrorInjectionType.INVALID_PIN -> HsmErrorCodes.VERIFICATION_FAILURE
        ErrorInjectionType.INSUFFICIENT_FUNDS,
        ErrorInjectionType.CARD_BLOCKED,
        ErrorInjectionType.EXPIRED_CARD,
        -> HsmErrorCodes.INVALID_INPUT_DATA
        ErrorInjectionType.CUSTOM_ERROR -> HsmErrorCodes.INVALID_INPUT_DATA
        ErrorInjectionType.NETWORK_TIMEOUT,
        ErrorInjectionType.CONNECTION_FAILURE,
        ErrorInjectionType.MALFORMED_RESPONSE,
        -> null
    }

    /** Error types that describe an HSM command result, so are meaningful in the HSM dialog. */
    val HSM_MEANINGFUL_ERROR_TYPES: List<ErrorInjectionType> = listOf(
        ErrorInjectionType.AUTHENTICATION_FAILURE,
        ErrorInjectionType.SYSTEM_ERROR,
        ErrorInjectionType.INVALID_PIN,
        ErrorInjectionType.CUSTOM_ERROR,
    )

    /**
     * Rewrites a response to carry [errorCode].
     *
     * The wire layout is `header(4) + responseCode(2) + errorCode(2) + data`; a real payShield
     * returns no payload alongside an error, so the data portion is dropped. Responses too short to
     * carry an error code are left untouched.
     */
    fun applyErrorCode(response: String?, errorCode: String): String? {
        if (response == null || response.length < 8) return response
        return response.substring(0, 6) + errorCode
    }
}

/** Reads the HSM simulator's persisted advanced options into live simulation settings. */
fun SimulationSettings.Companion.fromHsmAdvanced(
    advanced: AdvancedOptionsConfiguration,
) = SimulationSettings(
    enabled = advanced.simulationEnabled,
    delay = advanced.responseDelayConfig,
    ramp = advanced.rampConfig,
    timeouts = advanced.timeoutConfig,
    connection = advanced.connectionChaosConfig,
    errors = advanced.errorInjectionConfig,
    randomSeed = advanced.randomSeed,
)

/** Folds live settings back into [base] for persistence, leaving unrelated sections untouched. */
fun SimulationSettings.toHsmAdvanced(base: AdvancedOptionsConfiguration) = base.copy(
    simulationEnabled = enabled,
    responseDelayConfig = delay,
    rampConfig = ramp,
    timeoutConfig = timeouts,
    connectionChaosConfig = connection,
    errorInjectionConfig = errors,
    randomSeed = randomSeed,
)
