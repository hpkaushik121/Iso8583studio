package `in`.aicortex.iso8583studio.posSimulator

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdValidationInput
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdValidator
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.ChangeClass
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.HardwarePropertiesSchema
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.HardwarePropertyCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.PropGroup
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.Severity
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SkinInfo
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageRef
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.ValidationIssue
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AvdValidatorTest {

    private val hostAbi = "arm64-v8a"
    private val installed = listOf(SystemImageRef(30, "google_apis", hostAbi))

    /** A clean PAX A910S-shaped profile: 720x1440 @ 320 dpi, 2 GB, API 30 google_apis. */
    private fun goodProps(vararg overrides: Pair<String, String>) = mutableMapOf(
        "hw.ramSize" to "2048",
        "vm.heapSize" to "228",
        "hw.lcd.width" to "720",
        "hw.lcd.height" to "1440",
        "hw.lcd.density" to "320",
        "disk.dataPartition.size" to "6G",
        "sdcard.size" to "512M",
        "hw.cpu.ncore" to "4",
        "hw.gpu.enabled" to "yes",
        "hw.gpu.mode" to "auto",
    ).apply { putAll(overrides) }

    private fun validate(
        props: Map<String, String>,
        name: String = "ISO8583_PAX_A910S",
        image: SystemImageRef = SystemImageRef(30, "google_apis", hostAbi),
        skin: SkinInfo? = null,
        hostRamMb: Long? = 32768,
    ): List<ValidationIssue> = AvdValidator.validate(
        AvdValidationInput(
            avdName = name,
            properties = props,
            image = image,
            installedImages = installed,
            skin = skin,
            hostAbi = hostAbi,
            hostRamMb = hostRamMb,
        )
    )

    private fun List<ValidationIssue>.errors() = filter { it.severity == Severity.ERROR }
    private fun List<ValidationIssue>.forKey(key: String) = filter { it.key == key }

    @Test
    fun `a well formed terminal profile produces no errors`() {
        assertTrue(validate(goodProps()).errors().isEmpty())
    }

    @Test
    fun `a 480x480 160dpi square terminal is legitimate and must not be flagged`() {
        // This is the real Kozen N2 shape sitting in ~/.android/avd on this machine. A naive
        // density-vs-resolution rule would reject it; 480x480 @ 160 dpi is a 4.2" screen.
        val issues = validate(
            goodProps("hw.lcd.width" to "480", "hw.lcd.height" to "480", "hw.lcd.density" to "160")
        )
        assertTrue(issues.errors().isEmpty())
        assertTrue(issues.forKey("hw.lcd.density").isEmpty())
    }

    @Test
    fun `rejects RAM below the boot minimum and offers a fix`() {
        val issue = validate(goodProps("hw.ramSize" to "256")).errors().single()
        assertEquals("hw.ramSize", issue.key)
        assertEquals("1024", issue.fix!!().getValue("hw.ramSize"))
    }

    @Test
    fun `warns about low RAM on modern api levels and about hogging host memory`() {
        val low = validate(goodProps("hw.ramSize" to "768"))
        assertEquals(Severity.WARNING, low.forKey("hw.ramSize").single().severity)

        val hoggy = validate(goodProps("hw.ramSize" to "8192"), hostRamMb = 8192)
        assertTrue(hoggy.forKey("hw.ramSize").any { it.message.contains("40%") })
    }

    @Test
    fun `warns when the vm heap exceeds half of RAM`() {
        val issues = validate(goodProps("hw.ramSize" to "1024", "vm.heapSize" to "768"))
        assertEquals(Severity.WARNING, issues.forKey("vm.heapSize").single().severity)
    }

    @Test
    fun `flags an absurd implied screen size`() {
        // 720x1440 at 30 dpi implies a 53" phone.
        val issues = validate(goodProps("hw.lcd.density" to "30"))
        assertTrue(issues.forKey("hw.lcd.density").any { it.severity == Severity.WARNING })
    }

    @Test
    fun `rejects an unparseable disk size rather than silently defaulting`() {
        val issue = validate(goodProps("disk.dataPartition.size" to "banana")).errors().single()
        assertEquals("disk.dataPartition.size", issue.key)
        assertTrue(issue.remediation.contains("512M"))
    }

    @Test
    fun `rejects an sd card under the emulator's 9M floor`() {
        val issue = validate(goodProps("sdcard.size" to "4M")).errors().single()
        assertEquals("sdcard.size", issue.key)
        assertEquals("512M", issue.fix!!().getValue("sdcard.size"))
    }

    @Test
    fun `warns about a cramped data partition on api 30 plus`() {
        val issues = validate(goodProps("disk.dataPartition.size" to "1G"))
        assertEquals(Severity.WARNING, issues.forKey("disk.dataPartition.size").single().severity)
    }

    @Test
    fun `treats a play store image as an error because it cannot be rooted`() {
        // This is the one that silently breaks everything downstream: no adb root means no
        // writable /system, so the DAL install and prop spoofing both fail.
        val image = SystemImageRef(30, "google_apis_playstore", hostAbi)
        val issues = validate(goodProps(), image = image)
        val rootIssue = issues.errors().first { it.message.contains("cannot be rooted") }
        assertEquals("google_apis", rootIssue.fix!!().getValue("tag.id"))
    }

    @Test
    fun `rejects PlayStore enabled on a non play store image`() {
        val issues = validate(goodProps("PlayStore.enabled" to "yes"))
        val issue = issues.forKey("PlayStore.enabled").single()
        assertEquals(Severity.ERROR, issue.severity)
        assertEquals("no", issue.fix!!().getValue("PlayStore.enabled"))
    }

    @Test
    fun `rejects a foreign abi because there is no acceleration for it`() {
        val issues = validate(goodProps(), image = SystemImageRef(30, "google_apis", "x86_64"))
        val issue = issues.errors().first { it.key == "abi.type" }
        assertTrue(issue.remediation.contains("software emulation"))
    }

    @Test
    fun `rejects an uninstalled image and names the sdkmanager package`() {
        val issues = validate(goodProps(), image = SystemImageRef(33, "google_apis", hostAbi))
        val issue = issues.errors().first { it.key == "image.sysdir.1" }
        assertTrue(issue.remediation.contains("system-images;android-33;google_apis;arm64-v8a"))
    }

    @Test
    fun `explains that android 10 terminals have no arm64 image`() {
        // A910S and DX8000 are both Android 10 (API 29), which has no arm64 image upstream.
        val issues = validate(goodProps(), image = SystemImageRef(29, "google_apis", "arm64-v8a"))
        assertTrue(issues.errors().any { it.remediation.contains("Use API 30") })
    }

    @Test
    fun `warns when gpu mode contradicts a disabled gpu`() {
        val issues = validate(goodProps("hw.gpu.enabled" to "no", "hw.gpu.mode" to "host"))
        assertEquals(Severity.WARNING, issues.forKey("hw.gpu.mode").single().severity)
    }

    @Test
    fun `rejects an avd name avdmanager cannot handle`() {
        // A real device definition on this machine is named `KIOSK 27"`, which is exactly why AVD
        // names are never derived from device-definition names.
        val issue = validate(goodProps(), name = "KIOSK 27\"").errors().single()
        assertTrue(issue.remediation.contains("letters, digits"))
        assertTrue(validate(goodProps(), name = "ISO8583_PAX_A910S.v2-test").errors().isEmpty())
    }

    @Test
    fun `warns when a device frame skin disagrees with the screen size`() {
        val pixelSkin = SkinInfo("pixel_6", Paths.get("/skins/pixel_6"), 1080, 2400)
        val mismatched = validate(goodProps("showDeviceFrame" to "yes"), skin = pixelSkin)
        assertEquals(Severity.WARNING, mismatched.forKey("skin.name").single().severity)

        // Turning the frame off removes the problem, and that is the offered fix.
        assertTrue(validate(goodProps("showDeviceFrame" to "no"), skin = pixelSkin).forKey("skin.name").isEmpty())

        // A skin with no declared size must not invent a warning.
        val sizeless = SkinInfo("custom", Paths.get("/skins/custom"))
        assertTrue(validate(goodProps(), skin = sizeless).forKey("skin.name").isEmpty())
    }

    // ---------- HardwarePropertyCatalog ----------

    @Test
    fun `curated facets outside the schema are exactly the known config-ini-only keys`() {
        val schema = HardwarePropertiesSchema.bundled()
        val notInSchema = HardwarePropertyCatalog.facets
            .map { it.key }
            .filterNot { schema.properties.containsKey(it) }
            .toSet()

        // hardware-properties.ini is NOT a complete list of config.ini keys. These are honoured by
        // the emulator and written by the SDK tooling, but never declared in the schema — verified
        // against ~/.android/avd/Pixel_6.avd/config.ini. Note the asymmetry: fastboot.forceColdBoot
        // IS declared while fastboot.forceFastBoot is not, despite both appearing in config.ini.
        //
        // The consequence for the Advanced editor is that it must union the schema with the keys
        // actually present in the file, or it will hide settings the user can see on disk.
        assertEquals(
            setOf(
                "sdcard.size",
                "skin.name", "skin.path", "skin.dynamic", "showDeviceFrame",
                "abi.type", "image.sysdir.1", "tag.id",
                "fastboot.forceFastBoot",
                "runtime.network.speed", "runtime.network.latency",
            ),
            notInSchema,
        )
        // Sanity: the ones we claim ARE declared really are.
        assertTrue(schema.properties.containsKey("fastboot.forceColdBoot"))
        assertTrue(schema.properties.containsKey("hw.ramSize"))
    }

    @Test
    fun `advanced keys are everything the catalog does not curate`() {
        val schema = HardwarePropertiesSchema.bundled()
        val advanced = HardwarePropertyCatalog.advancedKeys(schema)
        assertTrue(advanced.none { HardwarePropertyCatalog.isCurated(it) })
        assertTrue("hw.ramSize" !in advanced, "curated properties must not also appear in Advanced")
        assertTrue("hw.sensor.hinge.count" in advanced, "uncurated properties must remain reachable")
    }

    @Test
    fun `density enum is overridden because the shipped one is stale`() {
        val facet = HardwarePropertyCatalog["hw.lcd.density"]!!
        assertNotNull(facet.enumOverride)
        assertTrue(facet.enumOverride!!.contains("420"))
        assertTrue(facet.enumOverride!!.contains("640"))
    }

    @Test
    fun `image defining keys are cold and ordinary tuning is hot`() {
        val before = mapOf("hw.ramSize" to "2048", "abi.type" to "arm64-v8a")
        assertEquals(
            ChangeClass.HOT,
            HardwarePropertyCatalog.classify(setOf("hw.ramSize"), before, before + ("hw.ramSize" to "4096")),
        )
        assertEquals(
            ChangeClass.COLD,
            HardwarePropertyCatalog.classify(setOf("abi.type"), before, before + ("abi.type" to "x86_64")),
        )
    }

    @Test
    fun `growing storage is hot but shrinking it is cold`() {
        val before = mapOf("disk.dataPartition.size" to "6G")
        assertEquals(
            ChangeClass.HOT,
            HardwarePropertyCatalog.classify(
                setOf("disk.dataPartition.size"), before, mapOf("disk.dataPartition.size" to "8G")
            ),
        )
        // The emulator cannot truncate an existing userdata image, so this must force a recreate.
        assertEquals(
            ChangeClass.COLD,
            HardwarePropertyCatalog.classify(
                setOf("disk.dataPartition.size"), before, mapOf("disk.dataPartition.size" to "2G")
            ),
        )
    }

    @Test
    fun `groups are populated and ordered`() {
        for (group in PropGroup.entries) {
            val facets = HardwarePropertyCatalog.group(group)
            assertTrue(facets.isNotEmpty(), "group ${group.label} has no facets")
            assertEquals(facets.map { it.order }.sorted(), facets.map { it.order })
        }
        assertFalse(HardwarePropertyCatalog.isCurated("hw.sensor.hinge.count"))
    }
}
