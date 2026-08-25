package `in`.aicortex.iso8583studio.hostSimulator

import `in`.aicortex.iso8583studio.data.model.HostSimulationConfig
import `in`.aicortex.iso8583studio.data.model.ResponseCodeSimulationConfig
import `in`.aicortex.iso8583studio.data.model.WeightedResponseCode
import `in`.aicortex.iso8583studio.data.model.toSimulationSettings
import `in`.aicortex.iso8583studio.domain.service.hostSimulatorService.PlaceholderProcessor
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ResponseDelayConfig
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResponseCodeSimulationTest {

    private fun resolve(
        config: ResponseCodeSimulationConfig?,
        seed: Long = 42,
    ) = PlaceholderProcessor.processSimulatedResponseCode(config, Random(seed))

    // ── Registration ──────────────────────────────────────────────────────

    @Test
    fun `SIMRC is a registered placeholder`() {
        // Load-bearing: Iso8583Data consults holdersList to skip length validation, and the
        // transaction editor to accept a token in a fixed-length field. Unregistered, [SIMRC] would
        // be treated as literal field data and rejected.
        assertTrue("[SIMRC]" in PlaceholderProcessor.holdersList, PlaceholderProcessor.holdersList.toString())
    }

    // ── Resolution ────────────────────────────────────────────────────────

    @Test
    fun `no config approves`() {
        assertEquals("00", resolve(null))
    }

    @Test
    fun `disabled section approves even with a full error rate`() {
        val config = ResponseCodeSimulationConfig(enabled = false, errorRate = 1.0)
        repeat(50) { assertEquals("00", resolve(config, seed = it.toLong())) }
    }

    @Test
    fun `zero error rate always approves`() {
        val config = ResponseCodeSimulationConfig(enabled = true, errorRate = 0.0)
        repeat(50) { assertEquals("00", resolve(config, seed = it.toLong())) }
    }

    @Test
    fun `a certain error rate always declines, drawing only from the pool`() {
        val config = ResponseCodeSimulationConfig(
            enabled = true,
            errorRate = 1.0,
            errorCodes = listOf(WeightedResponseCode("05", 1), WeightedResponseCode("91", 1)),
        )
        val seen = (0L until 100L).map { resolve(config, seed = it) }.toSet()
        assertTrue(seen.all { it in setOf("05", "91") }, "unexpected codes: $seen")
        assertTrue("00" !in seen, "approved despite a 100% error rate")
    }

    @Test
    fun `a custom success code is honoured`() {
        val config = ResponseCodeSimulationConfig(enabled = true, successCode = "000", errorRate = 0.0)
        assertEquals("000", resolve(config))
    }

    @Test
    fun `an empty decline pool falls back to approving`() {
        val config = ResponseCodeSimulationConfig(enabled = true, errorRate = 1.0, errorCodes = emptyList())
        assertEquals("00", resolve(config))
    }

    @Test
    fun `zero-weight entries are never drawn`() {
        val config = ResponseCodeSimulationConfig(
            enabled = true,
            errorRate = 1.0,
            errorCodes = listOf(WeightedResponseCode("05", 0), WeightedResponseCode("91", 1)),
        )
        val seen = (0L until 100L).map { resolve(config, seed = it) }.toSet()
        assertEquals(setOf("91"), seen)
    }

    // ── Distribution ──────────────────────────────────────────────────────

    @Test
    fun `decline frequency tracks the configured rate`() {
        val config = ResponseCodeSimulationConfig(enabled = true, errorRate = 0.25)
        val random = Random(7)
        val declines = (1..4000).count {
            PlaceholderProcessor.processSimulatedResponseCode(config, random) != "00"
        }
        val observed = declines / 4000.0
        assertTrue(abs(observed - 0.25) < 0.03, "observed decline rate $observed is far from 0.25")
    }

    @Test
    fun `codes are drawn in proportion to their weights`() {
        val config = ResponseCodeSimulationConfig(
            enabled = true,
            errorRate = 1.0,
            errorCodes = listOf(WeightedResponseCode("05", 3), WeightedResponseCode("96", 1)),
        )
        val random = Random(11)
        val counts = mutableMapOf<String, Int>()
        repeat(4000) {
            val code = PlaceholderProcessor.processSimulatedResponseCode(config, random)
            counts[code] = (counts[code] ?: 0) + 1
        }
        val share05 = (counts["05"] ?: 0) / 4000.0
        assertTrue(abs(share05 - 0.75) < 0.03, "05 share was $share05, expected ~0.75 (weights 3:1)")
    }

    // ── Projection onto the shared engine ─────────────────────────────────

    @Test
    fun `host settings project onto the shared engine settings`() {
        val host = HostSimulationConfig(
            enabled = true,
            randomSeed = 77,
            delay = ResponseDelayConfig(enabled = true, fixedDelayMs = 250),
        )
        val shared = host.toSimulationSettings()

        assertEquals(true, shared.enabled)
        assertEquals(77L, shared.randomSeed)
        assertEquals(250, shared.delay.fixedDelayMs)
        // Host errors come from [SIMRC], never from the engine swapping a wire code.
        assertEquals(false, shared.errors.enableErrorInjection)
    }

    @Test
    fun `anyActive reflects each section independently`() {
        assertEquals(false, HostSimulationConfig().anyActive)
        // A section on but the master switch off must still read as inactive.
        assertEquals(
            false,
            HostSimulationConfig(enabled = false, delay = ResponseDelayConfig(enabled = true)).anyActive,
        )
        assertEquals(
            true,
            HostSimulationConfig(enabled = true, delay = ResponseDelayConfig(enabled = true)).anyActive,
        )
        assertEquals(
            true,
            HostSimulationConfig(
                enabled = true,
                responseCode = ResponseCodeSimulationConfig(enabled = true),
            ).anyActive,
        )
    }
}
