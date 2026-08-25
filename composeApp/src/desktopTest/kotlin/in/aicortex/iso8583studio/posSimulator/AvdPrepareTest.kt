package `in`.aicortex.iso8583studio.posSimulator

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AndroidSdkLocator
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdManagerService
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdPhase
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdStep
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SdkResolution
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageRef
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.groupAvdSteps
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdSpec
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceCatalog
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AvdPrepareTest {

    private fun sdkOrNull() = (AndroidSdkLocator.locate() as? SdkResolution.Found)?.sdk

    private val a910s = DeviceCatalog["pax-a910s"]!!.resolve("5in")

    // ---------- guards: these must fail before anything touches disk ----------

    @Test
    fun `an unmanaged avd name is refused before any command runs`() = runBlocking {
        val sdk = sdkOrNull() ?: return@runBlocking
        val steps = AvdManagerService(sdk)
            .prepare(AvdSpec(avdName = "Kozen_N2"), a910s.hardware)
            .toList()

        val aborted = steps.filterIsInstance<AvdStep.Aborted>().single()
        assertEquals(AvdPhase.VALIDATE, aborted.phase)
        assertTrue(aborted.reason.contains("not a managed AVD"))
        assertTrue(aborted.remediation.contains("ISO8583_"))
        // Nothing was executed — the guard fires in validation, not after creating something.
        assertTrue(steps.none { it is AvdStep.Command }, "a command ran despite the name guard")
    }

    @Test
    fun `an empty avd name aborts`() = runBlocking {
        val sdk = sdkOrNull() ?: return@runBlocking
        val steps = AvdManagerService(sdk).prepare(AvdSpec(avdName = ""), a910s.hardware).toList()
        assertEquals(AvdPhase.VALIDATE, steps.filterIsInstance<AvdStep.Aborted>().single().phase)
    }

    @Test
    fun `validation errors abort before creation`() = runBlocking {
        val sdk = sdkOrNull() ?: return@runBlocking
        // An image that is definitely not installed.
        val steps = AvdManagerService(sdk).prepare(
            AvdSpec(
                avdName = "ISO8583_TEST_NOT_INSTALLED",
                systemImage = SystemImageRef(apiLevel = 99, tag = "google_apis", abi = "arm64-v8a"),
            ),
            a910s.hardware,
        ).toList()

        val aborted = steps.filterIsInstance<AvdStep.Aborted>().single()
        assertEquals(AvdPhase.VALIDATE, aborted.phase)
        assertTrue(steps.none { it is AvdStep.Command })
    }

    @Test
    fun `delete refuses an unmanaged avd`() {
        val sdk = sdkOrNull() ?: return
        // The nine hand-built AVDs in ~/.android/avd must be unreachable from this service.
        val error = assertFailsWith<IllegalArgumentException> {
            AvdManagerService(sdk).delete("Pixel_6")
        }
        assertTrue(error.message!!.contains("Refusing to delete"))
    }

    // ---------- step grouping (no SDK needed) ----------

    @Test
    fun `grouping folds steps into phases in start order`() {
        val steps = listOf(
            AvdStep.PhaseStart(1, AvdPhase.VALIDATE),
            AvdStep.Line(2, AvdPhase.VALIDATE, "checking"),
            AvdStep.PhaseEnd(3, AvdPhase.VALIDATE, ok = true),
            AvdStep.PhaseStart(4, AvdPhase.CREATE_AVD),
            AvdStep.Command(5, AvdPhase.CREATE_AVD, listOf("avdmanager", "create", "avd")),
            AvdStep.Line(6, AvdPhase.CREATE_AVD, "done"),
            AvdStep.PhaseEnd(7, AvdPhase.CREATE_AVD, ok = true, exitCode = 0),
        )

        val blocks = groupAvdSteps(steps)
        assertEquals(listOf(AvdPhase.VALIDATE, AvdPhase.CREATE_AVD), blocks.map { it.phase })
        assertEquals(true, blocks[0].ok)
        assertEquals(1, blocks[1].commands.size)
        assertEquals("avdmanager create avd", blocks[1].commands.single().commandLine)
        assertEquals(0, blocks[1].exitCode)
    }

    @Test
    fun `a running phase has a null outcome so the stepper can show a spinner`() {
        val blocks = groupAvdSteps(
            listOf(AvdStep.PhaseStart(1, AvdPhase.CREATE_AVD), AvdStep.Line(2, AvdPhase.CREATE_AVD, "working"))
        )
        assertNull(blocks.single().ok)
    }

    @Test
    fun `an abort marks its phase failed and carries the remediation`() {
        val blocks = groupAvdSteps(
            listOf(
                AvdStep.PhaseStart(1, AvdPhase.VALIDATE),
                AvdStep.Aborted(2, AvdPhase.VALIDATE, "bad thing", "do this instead"),
            )
        )
        val block = blocks.single()
        assertEquals(false, block.ok)
        assertEquals("bad thing", block.abortReason)
        assertEquals("do this instead", block.remediation)
    }

    @Test
    fun `phases partition into prepare and boot`() {
        assertTrue(AvdPhase.CREATE_AVD.isPrepare)
        assertFalse(AvdPhase.CREATE_AVD.isBoot)
        assertTrue(AvdPhase.WAIT_BOOT.isBoot)
        assertFalse(AvdPhase.WAIT_BOOT.isPrepare)
        // DONE belongs to neither — it terminates both pipelines.
        assertFalse(AvdPhase.DONE.isPrepare)
        assertFalse(AvdPhase.DONE.isBoot)
    }

    // ---------- the real thing ----------

    @Test
    fun `prepares a real AVD from the probed A910S profile`() = runBlocking {
        val sdk = sdkOrNull() ?: return@runBlocking
        val service = AvdManagerService(sdk)
        val avdName = "ISO8583_TEST_A910S"

        // Always start clean so the assertions mean something.
        runCatching { service.delete(avdName) }

        val spec = AvdSpec(
            avdName = avdName,
            hardwareProfileId = a910s.hardware.id,
            systemImage = a910s.terminal.recommendedImage,
        )

        try {
            val steps = service.prepare(spec, a910s.hardware).toList()
            // No tolerance for aborts here: creation is now entirely our own code, so a failure
            // is a real defect rather than a missing SDK tool.
            val aborted = steps.filterIsInstance<AvdStep.Aborted>().firstOrNull()
            assertNull(aborted, "prepare aborted at ${aborted?.phase}: ${aborted?.reason}")

            assertNotNull(steps.filterIsInstance<AvdStep.Done>().firstOrNull(), "no Done step")
            assertTrue(service.exists(avdName), "AVD directory was not created")

            // The measured geometry actually reached disk.
            val config = `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.ConfigIni
                .read(sdk.avdHome.resolve("$avdName.avd/config.ini"))
            assertEquals("720", config["hw.lcd.width"])
            assertEquals("1280", config["hw.lcd.height"])
            assertEquals("320", config["hw.lcd.density"])
            assertEquals("2048", config["hw.ramSize"])
            assertEquals("8", config["hw.cpu.ncore"])
            // And no hw.device.* keys, because we never pass -d.
            assertNull(config["hw.device.hash2"], "hw.device.hash2 would drift against our model")

            // Re-preparing is a verified no-op rather than a rebuild.
            val again = service.prepare(spec, a910s.hardware).toList()
            assertTrue(again.none { it is AvdStep.Command }, "re-prepare re-ran avdmanager")
            assertTrue(again.filterIsInstance<AvdStep.Done>().single().summary.contains("up to date"))
        } finally {
            runCatching { service.delete(avdName) }
        }
    }
}
