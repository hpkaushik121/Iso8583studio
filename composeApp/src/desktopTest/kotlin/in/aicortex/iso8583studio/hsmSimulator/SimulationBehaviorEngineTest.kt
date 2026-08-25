package `in`.aicortex.iso8583studio.hsmSimulator

import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.HsmErrorInjection
import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.fromHsmAdvanced
import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.toHsmAdvanced
import `in`.aicortex.iso8583studio.domain.service.simulation.ConnectionDecision
import `in`.aicortex.iso8583studio.domain.service.simulation.ResponseDecision
import `in`.aicortex.iso8583studio.domain.service.simulation.SimulationBehaviorEngine
import `in`.aicortex.iso8583studio.domain.service.simulation.SimulationSettings
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ConnectionChaosConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ErrorInjectionConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ErrorInjectionType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.RampPattern
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ResponseDelayConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ResponseDelayType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ResponseRampConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.TimeoutSimulationConfig
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SimulationBehaviorEngineTest {

    /** A clock the test drives by hand, so ramp behaviour is exact rather than timing-dependent. */
    private class FakeClock(var nowMs: Long = 0) : () -> Long {
        override fun invoke(): Long = nowMs
    }

    private fun engine(
        settings: SimulationSettings,
        clock: FakeClock = FakeClock(),
        seed: Long = 42,
    ) = SimulationBehaviorEngine(
        settingsProvider = { settings },
        clock = clock,
        randomFactory = { Random(seed) },
        errorCodeSelector = HsmErrorInjection::errorCodeFor,
    )

    private fun latencySettings(delay: ResponseDelayConfig, ramp: ResponseRampConfig = ResponseRampConfig()) =
        SimulationSettings(enabled = true, delay = delay, ramp = ramp)

    // ── Latency ───────────────────────────────────────────────────────────

    @Test
    fun `fixed delay returns exactly the configured value`() {
        val settings = latencySettings(
            ResponseDelayConfig(enabled = true, delayType = ResponseDelayType.FIXED, fixedDelayMs = 250),
        )
        assertEquals(250L, engine(settings).latencyMs(settings))
    }

    @Test
    fun `random delay stays inside the configured range`() {
        val settings = latencySettings(
            ResponseDelayConfig(
                enabled = true, delayType = ResponseDelayType.RANDOM,
                minDelayMs = 50, maxDelayMs = 500,
            ),
        )
        val e = engine(settings)
        repeat(200) {
            val ms = e.latencyMs(settings)
            assertTrue(ms in 50L..500L, "delay $ms outside 50..500")
        }
    }

    @Test
    fun `realistic delay is network plus processing`() {
        val settings = latencySettings(
            ResponseDelayConfig(
                enabled = true, delayType = ResponseDelayType.REALISTIC,
                networkLatencyMs = 20, processingDelayMs = 80,
            ),
        )
        assertEquals(100L, engine(settings).latencyMs(settings))
    }

    @Test
    fun `progressive delay grows with the number of open connections`() {
        val settings = latencySettings(
            ResponseDelayConfig(enabled = true, delayType = ResponseDelayType.PROGRESSIVE, fixedDelayMs = 10),
        )
        val e = engine(settings)
        assertEquals(10L, e.latencyMs(settings, activeConnections = 0))
        assertEquals(50L, e.latencyMs(settings, activeConnections = 4))
    }

    @Test
    fun `jitter stays within the configured percentage`() {
        val settings = latencySettings(
            ResponseDelayConfig(
                enabled = true, delayType = ResponseDelayType.FIXED, fixedDelayMs = 1000,
                enableJitter = true, jitterPercentage = 10,
            ),
        )
        val e = engine(settings)
        repeat(200) {
            val ms = e.latencyMs(settings)
            assertTrue(abs(ms - 1000L) <= 100L, "jittered delay $ms strayed beyond ±10% of 1000")
        }
    }

    @Test
    fun `latency is zero when its own section is off`() {
        val settings = latencySettings(
            ResponseDelayConfig(enabled = false, delayType = ResponseDelayType.FIXED, fixedDelayMs = 900),
        )
        assertEquals(0L, engine(settings).latencyMs(settings))
    }

    // ── Ramp ──────────────────────────────────────────────────────────────

    @Test
    fun `constant pattern never scales`() {
        val ramp = ResponseRampConfig(enabled = true, pattern = RampPattern.CONSTANT)
        val e = engine(SimulationSettings(enabled = true, ramp = ramp))
        assertEquals(1.0, e.rampMultiplier(0, ramp))
        assertEquals(1.0, e.rampMultiplier(999_999, ramp))
    }

    @Test
    fun `ramp up walks from start to peak then pins at peak`() {
        val ramp = ResponseRampConfig(
            enabled = true, pattern = RampPattern.RAMP_UP,
            rampUpSeconds = 100, startMultiplier = 1.0, peakMultiplier = 5.0,
        )
        val e = engine(SimulationSettings(enabled = true, ramp = ramp))
        assertEquals(1.0, e.rampMultiplier(0, ramp), 0.001)
        assertEquals(3.0, e.rampMultiplier(50_000, ramp), 0.001)   // halfway
        assertEquals(5.0, e.rampMultiplier(100_000, ramp), 0.001)  // end of ramp
        assertEquals(5.0, e.rampMultiplier(500_000, ramp), 0.001)  // long after — pinned
    }

    @Test
    fun `ramp up and down returns to the starting multiplier`() {
        val ramp = ResponseRampConfig(
            enabled = true, pattern = RampPattern.RAMP_UP_DOWN,
            rampUpSeconds = 10, holdSeconds = 10, rampDownSeconds = 10,
            startMultiplier = 1.0, peakMultiplier = 3.0,
        )
        val e = engine(SimulationSettings(enabled = true, ramp = ramp))
        assertEquals(1.0, e.rampMultiplier(0, ramp), 0.001)
        assertEquals(3.0, e.rampMultiplier(10_000, ramp), 0.001)   // top of the ramp
        assertEquals(3.0, e.rampMultiplier(15_000, ramp), 0.001)   // holding
        assertEquals(2.0, e.rampMultiplier(25_000, ramp), 0.001)   // halfway down
        assertEquals(1.0, e.rampMultiplier(35_000, ramp), 0.001)   // finished, pinned at start
    }

    @Test
    fun `repeating ramp cycles instead of pinning`() {
        val ramp = ResponseRampConfig(
            enabled = true, pattern = RampPattern.RAMP_UP, rampUpSeconds = 10,
            startMultiplier = 1.0, peakMultiplier = 5.0, repeat = true,
        )
        val e = engine(SimulationSettings(enabled = true, ramp = ramp))
        // 25s into a 10s cycle is 5s in — the same point as 5s.
        assertEquals(e.rampMultiplier(5_000, ramp), e.rampMultiplier(25_000, ramp), 0.001)
    }

    @Test
    fun `ramp scales the latency it is applied to`() {
        val clock = FakeClock(0)
        val ramp = ResponseRampConfig(
            enabled = true, pattern = RampPattern.RAMP_UP, rampUpSeconds = 100,
            startMultiplier = 1.0, peakMultiplier = 4.0, applyToLatency = true,
        )
        val settings = latencySettings(
            ResponseDelayConfig(enabled = true, delayType = ResponseDelayType.FIXED, fixedDelayMs = 100),
            ramp,
        )
        val e = engine(settings, clock)
        e.onSimulatorStarted()
        assertEquals(100L, e.latencyMs(settings))
        clock.nowMs = 100_000                       // end of ramp-up
        assertEquals(400L, e.latencyMs(settings))
    }

    // ── Master toggle ─────────────────────────────────────────────────────

    @Test
    fun `master toggle off short-circuits everything`(): Unit = runBlocking {
        val settings = SimulationSettings(
            enabled = false,
            delay = ResponseDelayConfig(enabled = true, delayType = ResponseDelayType.FIXED, fixedDelayMs = 5000),
            timeouts = TimeoutSimulationConfig(enabled = true, dropResponseRate = 1.0),
            connection = ConnectionChaosConfig(enabled = true, refuseRate = 1.0),
            errors = ErrorInjectionConfig(
                enableErrorInjection = true, errorRate = 1.0,
                enabledErrorTypes = setOf(ErrorInjectionType.SYSTEM_ERROR),
            ),
        )
        val e = engine(settings)
        assertEquals(0L, e.latencyMs(settings))
        assertIs<ResponseDecision.Send>(e.beforeResponse("NC"))
        assertIs<ConnectionDecision.Accept>(e.onConnectionAccepted())
    }

    // ── Decisions ─────────────────────────────────────────────────────────

    @Test
    fun `a certain drop rate always withholds the response`(): Unit = runBlocking {
        val settings = SimulationSettings(
            enabled = true,
            timeouts = TimeoutSimulationConfig(
                enabled = true, dropResponseRate = 1.0, closeConnectionOnDrop = true,
            ),
        )
        val decision = engine(settings).beforeResponse("NC")
        val drop = assertIs<ResponseDecision.Drop>(decision)
        assertTrue(drop.closeConnection)
    }

    @Test
    fun `drop frequency tracks the configured rate`(): Unit = runBlocking {
        val settings = SimulationSettings(
            enabled = true,
            timeouts = TimeoutSimulationConfig(enabled = true, dropResponseRate = 0.25),
        )
        val e = engine(settings)
        val drops = (1..2000).count { e.beforeResponse("NC") is ResponseDecision.Drop }
        val observed = drops / 2000.0
        assertTrue(abs(observed - 0.25) < 0.05, "observed drop rate $observed is far from 0.25")
    }

    @Test
    fun `reset takes precedence over drop`(): Unit = runBlocking {
        val settings = SimulationSettings(
            enabled = true,
            connection = ConnectionChaosConfig(enabled = true, resetMidStreamRate = 1.0),
            timeouts = TimeoutSimulationConfig(enabled = true, dropResponseRate = 1.0),
        )
        assertIs<ResponseDecision.ResetConnection>(engine(settings).beforeResponse("NC"))
    }

    @Test
    fun `drop takes precedence over error injection`(): Unit = runBlocking {
        val settings = SimulationSettings(
            enabled = true,
            timeouts = TimeoutSimulationConfig(enabled = true, dropResponseRate = 1.0),
            errors = ErrorInjectionConfig(
                enableErrorInjection = true, errorRate = 1.0,
                enabledErrorTypes = setOf(ErrorInjectionType.SYSTEM_ERROR),
            ),
        )
        assertIs<ResponseDecision.Drop>(engine(settings).beforeResponse("NC"))
    }

    @Test
    fun `a certain refuse rate rejects the connection`(): Unit = runBlocking {
        val settings = SimulationSettings(
            enabled = true,
            connection = ConnectionChaosConfig(enabled = true, refuseRate = 1.0),
        )
        assertIs<ConnectionDecision.Refuse>(engine(settings).onConnectionAccepted())
    }

    // ── Error injection ───────────────────────────────────────────────────

    @Test
    fun `error injection substitutes the mapped code`(): Unit = runBlocking {
        val settings = SimulationSettings(
            enabled = true,
            errors = ErrorInjectionConfig(
                enableErrorInjection = true, errorRate = 1.0,
                enabledErrorTypes = setOf(ErrorInjectionType.INVALID_PIN),
            ),
        )
        val decision = assertIs<ResponseDecision.SubstituteError>(engine(settings).beforeResponse("NC"))
        assertEquals("01", decision.errorCode)
    }

    @Test
    fun `a custom code for a command overrides the type mapping`(): Unit = runBlocking {
        val settings = SimulationSettings(
            enabled = true,
            errors = ErrorInjectionConfig(
                enableErrorInjection = true, errorRate = 1.0,
                enabledErrorTypes = setOf(ErrorInjectionType.INVALID_PIN),
                customErrorCodes = mapOf("NC" to "68"),
            ),
        )
        val decision = assertIs<ResponseDecision.SubstituteError>(engine(settings).beforeResponse("NC"))
        assertEquals("68", decision.errorCode)
    }

    @Test
    fun `transport-only error types map to no code`() {
        assertNull(HsmErrorInjection.errorCodeFor(ErrorInjectionType.NETWORK_TIMEOUT))
        assertNull(HsmErrorInjection.errorCodeFor(ErrorInjectionType.CONNECTION_FAILURE))
        assertNull(HsmErrorInjection.errorCodeFor(ErrorInjectionType.MALFORMED_RESPONSE))
    }

    @Test
    fun `applying an error code keeps the header and drops the data`() {
        // header(4) + responseCode(2) + errorCode(2) + data
        val ok = "0000A100U1234567890ABCDEF"
        assertEquals("0000A142", HsmErrorInjection.applyErrorCode(ok, "42"))
    }

    @Test
    fun `a response too short to carry an error code is left alone`() {
        assertEquals("0000A1", HsmErrorInjection.applyErrorCode("0000A1", "42"))
        assertNull(HsmErrorInjection.applyErrorCode(null, "42"))
    }

    // ── Settings round-trip ───────────────────────────────────────────────

    @Test
    fun `settings survive a round trip through the advanced config`() {
        val original = SimulationSettings(
            enabled = true,
            delay = ResponseDelayConfig(enabled = true, fixedDelayMs = 321),
            ramp = ResponseRampConfig(enabled = true, peakMultiplier = 7.5),
            timeouts = TimeoutSimulationConfig(enabled = true, dropResponseRate = 0.3),
            connection = ConnectionChaosConfig(enabled = true, refuseRate = 0.2),
            errors = ErrorInjectionConfig(enableErrorInjection = true, errorRate = 0.4),
            randomSeed = 99,
        )
        val advanced = original.toHsmAdvanced(
            `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.AdvancedOptionsConfiguration(),
        )
        assertEquals(original, SimulationSettings.fromHsmAdvanced(advanced))
    }
}
