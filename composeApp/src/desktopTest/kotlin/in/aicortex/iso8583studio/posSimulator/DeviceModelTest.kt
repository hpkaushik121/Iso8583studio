package `in`.aicortex.iso8583studio.posSimulator

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.HardwarePropertiesSchema
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageRef
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdProperties
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdSpec
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DalFlavor
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceFeature
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceVendor
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.HardwareProfile
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ProfileOrigin
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.Props
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.SkinKind
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.SkinRef
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.SpecConfidence
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.SpecProvenance
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.TerminalDeviceProfile
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.density
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ramMb
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.resolutionSummary
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.screenHeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceModelTest {

    private val schema = HardwarePropertiesSchema.bundled()

    private val a910sHardware = HardwareProfile(
        id = "pax-a910s-hw",
        name = "PAX A910S",
        manufacturer = "PAX Technology",
        origin = ProfileOrigin.BUILTIN,
        properties = mapOf(
            Props.RAM to "2048",
            Props.LCD_WIDTH to "720",
            Props.LCD_HEIGHT to "1440",
            Props.LCD_DENSITY to "320",
            Props.CPU_CORES to "4",
        ),
        provenance = SpecProvenance(SpecConfidence.FROM_DATASHEET, "paxtechnology.com/a910s"),
    )

    private val spec = AvdSpec(
        avdName = "ISO8583_PAX_A910S",
        hardwareProfileId = a910sHardware.id,
        systemImage = SystemImageRef(30, "google_apis", "arm64-v8a"),
    )

    // ---------- typed accessors over the property map ----------

    @Test
    fun `typed accessors read through to the property map`() {
        assertEquals(2048, a910sHardware.ramMb)
        assertEquals(1440, a910sHardware.screenHeight)
        assertEquals(320, a910sHardware.density)
        assertEquals("720 x 1440 @ 320 dpi", a910sHardware.resolutionSummary)
        assertNull(HardwareProfile(id = "x", name = "x").ramMb)
        assertNull(HardwareProfile(id = "x", name = "x").resolutionSummary)
    }

    // ---------- fork semantics ----------

    @Test
    fun `forking a builtin records where it came from so a catalog fix can still be offered`() {
        val forked = a910sHardware.fork("my-a910s", "My A910S")
        assertEquals(ProfileOrigin.USER, forked.origin)
        assertEquals("pax-a910s-hw", forked.derivedFrom)
        assertEquals("My A910S", forked.name)
        // The original is untouched — built-ins are compiled in and never mutated.
        assertEquals(ProfileOrigin.BUILTIN, a910sHardware.origin)

        // Forking a fork keeps pointing at the original builtin, not the intermediate.
        assertEquals("pax-a910s-hw", forked.fork("my-a910s-2").derivedFrom)
    }

    // ---------- effective vs config.ini overlay ----------

    @Test
    fun `effective layers schema defaults under the profile and overrides`() {
        val withOverride = spec.copy(overrides = mapOf(Props.RAM to "4096"))
        val effective = AvdProperties.effective(withOverride, a910sHardware, schema)

        assertEquals("4096", effective[Props.RAM], "spec override must beat the hardware profile")
        assertEquals("720", effective[Props.LCD_WIDTH], "hardware profile value survives")
        // The profile sets 4 cores; the schema default is 2. The profile must win.
        assertEquals("2", schema["hw.cpu.ncore"]!!.default)
        assertEquals("4", effective[Props.CPU_CORES], "hardware profile must beat the schema default")
        // A schema default the profile never mentions is still present for validation.
        assertEquals("multi-touch", effective["hw.screen"])
    }

    @Test
    fun `config ini overlay excludes schema defaults so the file stays small`() {
        val overlay = AvdProperties.configIniOverlay(spec, a910sHardware)
        assertFalse("hw.screen" in overlay, "schema defaults must stay implicit on disk")
        assertTrue(Props.RAM in overlay)
        // Real config.ini files carry ~50 keys, not the schema's 153.
        assertTrue(overlay.size < 60, "overlay unexpectedly large: ${overlay.size}")
    }

    // ---------- derived keys ----------

    @Test
    fun `image defining keys are derived from the system image and beat stale overrides`() {
        // A leftover abi.type override must not survive, or the AVD boots to a black screen with
        // an ABI that disagrees with its image directory.
        val stale = spec.copy(overrides = mapOf(Props.ABI to "x86_64", Props.SYSDIR to "nonsense/"))
        val overlay = AvdProperties.configIniOverlay(stale, a910sHardware)

        assertEquals("arm64-v8a", overlay[Props.ABI])
        assertEquals("system-images/android-30/google_apis/arm64-v8a/", overlay[Props.SYSDIR])
        assertEquals("google_apis", overlay[Props.TAG_ID])
        assertEquals("Google APIs", overlay[Props.TAG_DISPLAY])
        assertEquals("false", overlay[Props.PLAY_STORE])
    }

    @Test
    fun `play store flag follows the image tag`() {
        val playStore = spec.copy(systemImage = SystemImageRef(30, "google_apis_playstore", "arm64-v8a"))
        val overlay = AvdProperties.configIniOverlay(playStore, a910sHardware)
        assertEquals("true", overlay[Props.PLAY_STORE])
        assertEquals("Google Play", overlay[Props.TAG_DISPLAY])
    }

    @Test
    fun `cold boot and quick boot are mutually exclusive`() {
        val cold = AvdProperties.configIniOverlay(spec.copy(coldBoot = true), a910sHardware)
        assertEquals("yes", cold[Props.COLD_BOOT])
        assertEquals("no", cold[Props.FAST_BOOT])

        val quick = AvdProperties.configIniOverlay(spec.copy(coldBoot = false), a910sHardware)
        assertEquals("no", quick[Props.COLD_BOOT])
        assertEquals("yes", quick[Props.FAST_BOOT])
    }

    @Test
    fun `an empty sd card size disables the sd card rather than writing an invalid one`() {
        val none = AvdProperties.configIniOverlay(spec.copy(sdCardSize = ""), a910sHardware)
        assertEquals("no", none[Props.SDCARD_PRESENT])
        assertFalse(Props.SDCARD_SIZE in none)

        val sized = AvdProperties.configIniOverlay(spec.copy(sdCardSize = "512M"), a910sHardware)
        assertEquals("yes", sized[Props.SDCARD_PRESENT])
        assertEquals("512M", sized[Props.SDCARD_SIZE])
    }

    @Test
    fun `no skin means no device frame`() {
        val overlay = AvdProperties.configIniOverlay(spec, a910sHardware)
        assertEquals("no", overlay[Props.SHOW_DEVICE_FRAME])
        assertFalse(Props.SKIN_PATH in overlay)
    }

    @Test
    fun `a real skin wires name path and frame`() {
        val skinned = a910sHardware.copy(
            skin = SkinRef(SkinKind.CUSTOM, "pax_a910s", "/skins/pax_a910s", showDeviceFrame = true)
        )
        val overlay = AvdProperties.configIniOverlay(spec, skinned)
        assertEquals("pax_a910s", overlay[Props.SKIN_NAME])
        assertEquals("/skins/pax_a910s", overlay[Props.SKIN_PATH])
        assertEquals("yes", overlay[Props.SHOW_DEVICE_FRAME])
        assertEquals("no", overlay[Props.SKIN_DYNAMIC])
    }

    // ---------- managed-name guard ----------

    @Test
    fun `managed prefix guards the user's own AVDs`() {
        assertTrue(AvdProperties.isManaged("ISO8583_PAX_A910S"))
        // These are real AVDs sitting in ~/.android/avd on this machine. Delete must refuse them.
        assertFalse(AvdProperties.isManaged("Kozen_N2"))
        assertFalse(AvdProperties.isManaged("Pixel_6"))
        assertFalse(AvdProperties.isManaged("KIOSK_27"))
    }

    @Test
    fun `suggested avd names are sanitised to what avdmanager accepts`() {
        val terminal = TerminalDeviceProfile(
            id = "pax-a910s", vendor = DeviceVendor.PAX, model = "A910S",
            hardwareProfileId = a910sHardware.id,
        )
        assertEquals("ISO8583_PAX_A910S", AvdProperties.suggestAvdName(terminal))

        // A model with characters avdmanager rejects, e.g. the real `KIOSK 27"` device definition.
        val awkward = terminal.copy(vendor = DeviceVendor.GENERIC, model = "KIOSK 27\"")
        val name = AvdProperties.suggestAvdName(awkward)
        assertTrue(Regex("[A-Za-z0-9._-]+").matches(name), "not avdmanager-safe: $name")
        assertTrue(AvdProperties.isManaged(name))
    }

    // ---------- fingerprint / drift ----------

    @Test
    fun `fingerprint is stable for identical specs and changes with on-disk content`() {
        assertEquals(
            AvdProperties.fingerprint(spec, a910sHardware),
            AvdProperties.fingerprint(spec.copy(), a910sHardware),
        )
        assertNotEquals(
            AvdProperties.fingerprint(spec, a910sHardware),
            AvdProperties.fingerprint(spec.copy(overrides = mapOf(Props.RAM to "4096")), a910sHardware),
        )
    }

    @Test
    fun `fingerprint ignores fields that never reach disk`() {
        // Bookkeeping and notes must not report spurious drift.
        val noisy = spec.copy(lastPreparedAt = 12345, lastAppliedFingerprint = "old", ephemeral = true)
        assertEquals(
            AvdProperties.fingerprint(spec, a910sHardware),
            AvdProperties.fingerprint(noisy, a910sHardware),
        )
        val annotated = a910sHardware.copy(notes = "some note", provenance = SpecProvenance())
        assertEquals(
            AvdProperties.fingerprint(spec, a910sHardware),
            AvdProperties.fingerprint(spec, annotated),
        )
    }

    // ---------- terminal profile ----------

    @Test
    fun `terminal boot props spoof the vendor identity and can be overridden`() {
        val terminal = TerminalDeviceProfile(
            id = "pax-a910s", vendor = DeviceVendor.PAX, model = "A910S",
            hardwareProfileId = a910sHardware.id,
            dal = DalFlavor.PAX_NEPTUNE,
            features = setOf(DeviceFeature.ICC, DeviceFeature.PICC, DeviceFeature.PRINTER),
            bootProps = mapOf("ro.product.name" to "custom_override"),
        )

        val props = terminal.effectiveBootProps(serialNumber = "TEST123")
        assertEquals("PAX Technology", props["ro.product.manufacturer"])
        assertEquals("PAX", props["ro.product.brand"])
        assertEquals("A910S", props["ro.product.model"])
        assertEquals("TEST123", props["ro.serialno"])
        // An explicit bootProps entry wins over the derived default.
        assertEquals("custom_override", props["ro.product.name"])
        // No serial supplied -> the key is simply absent rather than empty.
        assertFalse("ro.serialno" in terminal.effectiveBootProps())

        assertEquals("PAX Technology A910S", terminal.label)
        assertTrue(terminal.has(DeviceFeature.PRINTER))
        assertFalse(terminal.has(DeviceFeature.SCANNER))
    }

    @Test
    fun `every vendor has a distinct dal flavor and detection prefix`() {
        val vendorsWithDal = DeviceVendor.entries.filter { it.defaultDal != DalFlavor.NONE }
        // A vendor's DAL must be identifiable on a real device via `pm list packages`, which is how
        // DeviceProbe both confirms the flavor and tells recon which package to pull apart.
        for (vendor in vendorsWithDal) {
            assertTrue(
                vendor.defaultDal.detectPackagePrefix.isNotEmpty(),
                "${vendor.displayName} has no detectable package prefix",
            )
        }
        assertEquals(
            vendorsWithDal.size,
            vendorsWithDal.map { it.defaultDal }.toSet().size,
            "two vendors share a DAL flavor",
        )
    }

    @Test
    fun `unverified specs are flagged so guessed geometry is never presented as fact`() {
        assertTrue(SpecConfidence.FROM_DATASHEET.warnsInUi)
        assertTrue(SpecConfidence.PROVISIONAL.warnsInUi)
        assertFalse(SpecConfidence.VERIFIED_FROM_DEVICE.warnsInUi)
        assertFalse(a910sHardware.provenance.isTrustworthy)
        assertTrue(
            a910sHardware.provenance.copy(confidence = SpecConfidence.VERIFIED_FROM_DEVICE).isTrustworthy
        )
    }
}
