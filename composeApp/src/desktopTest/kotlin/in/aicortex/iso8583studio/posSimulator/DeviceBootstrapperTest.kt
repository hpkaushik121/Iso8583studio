package `in`.aicortex.iso8583studio.posSimulator

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AndroidSdkLocator
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdPhase
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdStep
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.DeviceBootstrapper
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.EmulatorLauncher
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SdkResolution
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdSpec
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceCatalog
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceBootstrapperTest {

    private fun sdkOrNull() = (AndroidSdkLocator.locate() as? SdkResolution.Found)?.sdk
    private val a910s = DeviceCatalog["pax-a910s"]!!.resolve("5in")

    // ---------- build.prop rewriting: the part that actually makes identity work ----------

    private val bootstrapper get() = DeviceBootstrapper(sdkOrNull()!!)

    @Test
    fun `rewrites the per-partition key which is what actually resolves`() {
        val sdk = sdkOrNull() ?: return
        val original = """
            ro.product.vendor.model=Android SDK built for arm64
            ro.product.vendor.manufacturer=unknown
            some.other.prop=keep me
        """.trimIndent()

        val patched = DeviceBootstrapper(sdk).rewriteProps(
            original, "vendor",
            mapOf("ro.product.model" to "A910S", "ro.product.manufacturer" to "PAX"),
        )

        // The per-partition keys are the ones Android resolves from — patching only the plain key
        // changes nothing observable.
        assertTrue(patched.contains("ro.product.vendor.model=A910S"))
        assertTrue(patched.contains("ro.product.vendor.manufacturer=PAX"))
        assertFalse(patched.contains("Android SDK built for arm64"))
        assertTrue(patched.contains("some.other.prop=keep me"), "unrelated properties must survive")
    }

    @Test
    fun `replaces in place rather than appending because the first definition wins`() {
        val sdk = sdkOrNull() ?: return
        val original = "ro.product.system.model=mainline\nro.product.model=stock\n"
        val patched = DeviceBootstrapper(sdk)
            .rewriteProps(original, "system", mapOf("ro.product.model" to "A910S"))

        // For ro.* the FIRST definition wins and later ones are silently ignored, so a duplicate
        // appended at the end would do nothing at all.
        assertEquals(1, Regex("(?m)^ro\\.product\\.system\\.model=").findAll(patched).count())
        assertEquals(1, Regex("(?m)^ro\\.product\\.model=").findAll(patched).count())
        assertTrue(patched.contains("ro.product.system.model=A910S"))
        assertTrue(patched.contains("ro.product.model=A910S"))
    }

    @Test
    fun `adds a missing key rather than dropping it`() {
        val sdk = sdkOrNull() ?: return
        val patched = DeviceBootstrapper(sdk)
            .rewriteProps("unrelated=1\n", "vendor", mapOf("ro.product.brand" to "UNISOC"))
        assertTrue(patched.contains("ro.product.vendor.brand=UNISOC"))
        assertTrue(patched.contains("ro.product.brand=UNISOC"))
    }

    @Test
    fun `rewriting is idempotent`() {
        val sdk = sdkOrNull() ?: return
        val b = DeviceBootstrapper(sdk)
        val wanted = mapOf("ro.product.model" to "A910S")
        val once = b.rewriteProps("ro.product.vendor.model=x\n", "vendor", wanted)
        assertEquals(once, b.rewriteProps(once, "vendor", wanted))
    }

    // ---------- emulator argv ----------

    @Test
    fun `argv carries the flags a device replica needs and omits the ones that do not work`() {
        val sdk = sdkOrNull() ?: return
        val spec = AvdSpec(avdName = "ISO8583_PAX_A910S", writableSystem = true, selinuxPermissive = true)
        val argv = EmulatorLauncher.buildArgv(sdk, spec, 5584)

        assertEquals(sdk.emulator.toString(), argv.first())
        assertTrue(argv.containsAll(listOf("-avd", "ISO8583_PAX_A910S")))
        assertTrue(argv.containsAll(listOf("-port", "5584")))
        assertTrue("-writable-system" in argv, "needed for the identity patch")
        assertTrue(argv.containsAll(listOf("-selinux", "permissive")))
        // Snapshots discard the writable-system overlay, taking the identity patch with it.
        assertTrue("-no-snapshot-load" in argv)
        assertTrue("-no-snapshot-save" in argv)

        // -prop is deliberately absent: measured against android-31;default it does not set
        // ro.product.* at all, so including it would imply a guarantee we cannot keep.
        assertFalse(argv.any { it == "-prop" }, "-prop does not work for ro.product.* and must not be used")
    }

    @Test
    fun `headless is opt-in`() {
        val sdk = sdkOrNull() ?: return
        val spec = AvdSpec(avdName = "ISO8583_X")
        assertFalse("-no-window" in EmulatorLauncher.buildArgv(sdk, spec, 5554))
        assertTrue("-no-window" in EmulatorLauncher.buildArgv(sdk, spec.copy(headless = true), 5554))
    }

    // ---------- port allocation ----------

    @Test
    fun `allocates a free even console port`() {
        val port = EmulatorLauncher.allocatePort()
        assertTrue(port in 5554..5682, "port $port outside the emulator range")
        assertEquals(0, port % 2, "console ports must be even; adb uses the odd port above")
    }

    @Test
    fun `serial follows the emulator naming convention`() {
        assertEquals("emulator-5584", EmulatorLauncher.serialFor(5584))
    }

    // ---------- boot guards ----------

    @Test
    fun `booting a non-existent AVD aborts before starting anything`() = runBlocking {
        val sdk = sdkOrNull() ?: return@runBlocking
        val steps = DeviceBootstrapper(sdk)
            .boot(AvdSpec(avdName = "ISO8583_DOES_NOT_EXIST"), a910s)
            .toList()

        val aborted = steps.filterIsInstance<AvdStep.Aborted>().single()
        assertEquals(AvdPhase.VERIFY_PREPARED, aborted.phase)
        assertTrue(aborted.remediation.contains("Prepare"))
        // Critically, no emulator was launched.
        assertTrue(steps.none { it is AvdStep.Command })
    }
}
