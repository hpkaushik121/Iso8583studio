package `in`.aicortex.iso8583studio.apduSimulator

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.samples.SampleProfiles
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.CardRuntime
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GenerateAcHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GetChallengeHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GetDataHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GpoHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.ReadRecordHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.SelectHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.VerifyHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.AcType
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.Phase
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.TerminalProfiles
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.TerminalRuntime
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.TransactionRequest
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.TransactionStep
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.TransactionType
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.LoopbackTransport
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TerminalRuntimeIntegrationTest {

    private fun newTransport(): LoopbackTransport {
        val profile = SampleProfiles.visaCreditTest()
        val runtime = CardRuntime(
            profile,
            listOf(
                SelectHandler(),
                GpoHandler(),
                ReadRecordHandler(),
                GetDataHandler(),
                GetChallengeHandler(),
                VerifyHandler(),
                GenerateAcHandler(),
            ),
        )
        return LoopbackTransport(runtime)
    }

    @Test
    fun `full Visa purchase flow reaches Outcome with cryptogram`() = runBlocking {
        val transport = newTransport()
        transport.connect()

        val terminal = TerminalProfiles.attendedRetailIN()
        val rt = TerminalRuntime(transport, terminal)

        val steps = rt.run(
            TransactionRequest(
                amount = 50000, // 500.00 — well under the 2000.00 floor limit
                type = TransactionType.PURCHASE,
            ),
        ).toList()

        // No abort
        assertTrue(steps.none { it is TransactionStep.Aborted }, "aborted: ${steps.filterIsInstance<TransactionStep.Aborted>().joinToString { it.reason }}")

        // App Selection happened
        assertTrue(steps.any { it is TransactionStep.PhaseStart && it.phase == Phase.APP_SELECTION })

        // GPO + READ_DATA + FIRST_GAC all hit
        assertTrue(steps.any { it is TransactionStep.PhaseEnd && it.phase == Phase.INITIATE && it.ok })
        assertTrue(steps.any { it is TransactionStep.PhaseEnd && it.phase == Phase.READ_DATA && it.ok })
        assertTrue(steps.any { it is TransactionStep.PhaseEnd && it.phase == Phase.FIRST_GAC && it.ok })

        // Outcome emitted with a cryptogram
        val outcome = steps.filterIsInstance<TransactionStep.Outcome>().lastOrNull()
        assertNotNull(outcome, "no Outcome step")
        assertTrue(outcome.ac.size == 8, "AC must be 8 bytes")
        assertTrue(outcome.atc > 0, "ATC must increment")
        assertTrue(outcome.acType in setOf(AcType.AAC, AcType.TC, AcType.ARQC))

        transport.disconnect()
    }

    @Test
    fun `force online makes terminal request ARQC`() = runBlocking {
        val transport = newTransport()
        transport.connect()
        val rt = TerminalRuntime(transport, TerminalProfiles.attendedRetailIN())
        val steps = rt.run(TransactionRequest(amount = 100, forceOnline = true)).toList()
        val outcome = steps.filterIsInstance<TransactionStep.Outcome>().lastOrNull()
        assertNotNull(outcome)
        // The card decides the actual cryptogram; we asserted the *terminal* asked for ARQC.
        // Find the FIRST_GAC exchange and check P1 == 0x80.
        val gac = steps.filterIsInstance<TransactionStep.Exchange>().firstOrNull { it.phase == Phase.FIRST_GAC }
        assertNotNull(gac)
        assertTrue((gac.command.p1.toInt() and 0xC0) == 0x80, "Expected ARQC P1=0x80, got %02X".format(gac.command.p1))
        transport.disconnect()
    }
}
