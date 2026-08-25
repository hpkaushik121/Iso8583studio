package `in`.aicortex.iso8583studio.posSimulator

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageRef
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DalStatus
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceFeature
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceVendor
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.PrinterSpec
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ProfileOrigin
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.Props
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.SpecConfidence
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.SpecProvenance
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.TerminalId
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.TerminalModel
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.TerminalVariant
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.density
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ramMb
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.screenHeight
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.screenWidth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalVariantTest {

    private val a910s = DeviceCatalog["pax-a910s"]!!

    // ---------- identity ----------

    @Test
    fun `terminal ids round trip through model and variant`() {
        assertEquals("pax-a910s:5p5in", TerminalId.of("pax-a910s", "5p5in"))
        assertEquals("pax-a910s", TerminalId.of("pax-a910s", null))
        assertEquals("pax-a910s", TerminalId.of("pax-a910s", ""))

        assertEquals("pax-a910s", TerminalId.modelOf("pax-a910s:5p5in"))
        assertEquals("5p5in", TerminalId.variantOf("pax-a910s:5p5in"))
        assertEquals("pax-a910s", TerminalId.modelOf("pax-a910s"))
        assertEquals("", TerminalId.variantOf("pax-a910s"))
    }

    // ---------- resolution ----------

    @Test
    fun `the 5 inch and 5 point 5 inch A910S resolve to different geometry`() {
        // This is the whole reason variants exist: the datasheet says "5\" HD with an optional
        // 5.5\"", and those are not the same device to emulate.
        val base = a910s.resolve("5in").hardware
        val big = a910s.resolve("5p5in").hardware

        assertEquals(720, base.screenWidth)
        assertEquals(1280, base.screenHeight)
        assertEquals(720, big.screenWidth)
        assertEquals(1440, big.screenHeight)
        assertEquals(320, big.density)
        // Everything the variant did not touch is inherited.
        assertEquals(base.ramMb, big.ramMb)
    }

    @Test
    fun `an unknown or null variant falls back to the default`() {
        val default = a910s.resolve(null)
        assertEquals("5in", default.variantId)
        assertEquals("5in", a910s.resolve("does-not-exist").variantId)
        assertEquals("5in", a910s.resolve("").variantId)
    }

    @Test
    fun `feature deltas add and remove`() {
        val cellular = a910s.resolve("5in").terminal
        assertTrue(cellular.has(DeviceFeature.CELLULAR))

        val wifiOnly = a910s.resolve("wifi-only").terminal
        assertFalse(wifiOnly.has(DeviceFeature.CELLULAR))
        // Removing one feature must not disturb the others.
        assertTrue(wifiOnly.has(DeviceFeature.ICC))
        assertTrue(wifiOnly.has(DeviceFeature.PRINTER))

        val kozen = DeviceCatalog["kozen-n2"]!!
        assertFalse(kozen.resolve("base").terminal.has(DeviceFeature.PRINTER))
        assertTrue(kozen.resolve("printer").terminal.has(DeviceFeature.PRINTER))
        assertEquals(384, kozen.resolve("printer").terminal.printer.dotsPerLine)
        assertFalse(kozen.resolve("base").terminal.printer.present)
    }

    @Test
    fun `an image override changes the android version for that variant only`() {
        // The probed unit runs Android 12 / API 31, and an arm64 image exists for it — so the
        // default variant matches the real API level exactly rather than substituting.
        assertEquals(31, a910s.resolve("5in").terminal.recommendedImage.apiLevel)
        assertEquals("12", a910s.resolve("5in").terminal.realAndroidVersion)

        // The datasheet-documented Android 10 units cannot be matched: API 29 has no arm64 image,
        // so that variant deliberately substitutes API 30.
        val a10 = a910s.resolve("android10").terminal
        assertEquals(30, a10.recommendedImage.apiLevel)
        assertEquals("10", a10.realAndroidVersion)
    }

    @Test
    fun `the probed A910S carries its measured geometry and corrected identity`() {
        // Regression guard on the values that came off real hardware. The datasheet-derived entry
        // had four of these wrong.
        val resolved = a910s.resolve("5in")
        assertEquals(720, resolved.hardware.screenWidth)
        assertEquals(1280, resolved.hardware.screenHeight)
        assertEquals(320, resolved.hardware.density)
        assertEquals(2048, resolved.hardware.ramMb)
        assertEquals("8", resolved.hardware.properties[Props.CPU_CORES])

        val props = resolved.terminal.effectiveBootProps()
        assertEquals("A910S", props["ro.product.model"])
        assertEquals("PAX", props["ro.product.manufacturer"])
        // brand is the SoC vendor, not PAX — only a probe reveals this.
        assertEquals("UNISOC", props["ro.product.brand"])
        assertEquals("uis8581e_5h10", props["ro.product.device"])
    }

    @Test
    fun `resolution produces the pre-existing types so nothing downstream changes`() {
        val resolved = a910s.resolve("5p5in")
        // The AVD layer only ever sees HardwareProfile + TerminalDeviceProfile.
        assertEquals(resolved.hardware.id, resolved.terminal.hardwareProfileId)
        assertEquals("pax-a910s:5p5in", resolved.terminal.id)
        assertEquals("pax-a910s:5p5in", resolved.id)
        assertEquals(DeviceVendor.PAX, resolved.terminal.vendor)
        assertEquals("A910S", resolved.terminal.model)
    }

    // ---------- provenance ----------

    @Test
    fun `provenance resolves to the weaker of model and variant`() {
        val model = TerminalModel(
            id = "m", vendor = DeviceVendor.GENERIC, model = "M",
            provenance = SpecProvenance(SpecConfidence.VERIFIED_FROM_DEVICE, "adb probe"),
            variants = listOf(
                TerminalVariant(id = "verified", label = "V",
                    provenance = SpecProvenance(SpecConfidence.VERIFIED_FROM_DEVICE, "adb probe")),
                TerminalVariant(id = "guessed", label = "G",
                    propertyDeltas = mapOf(Props.LCD_HEIGHT to "1440"),
                    provenance = SpecProvenance(SpecConfidence.PROVISIONAL, "inferred")),
            ),
        )

        // A device-verified base does not launder a guessed delta.
        assertEquals(
            SpecConfidence.VERIFIED_FROM_DEVICE,
            model.resolve("verified").hardware.provenance.confidence,
        )
        assertEquals(
            SpecConfidence.PROVISIONAL,
            model.resolve("guessed").hardware.provenance.confidence,
        )
        assertEquals(
            SpecConfidence.PROVISIONAL,
            model.resolve("guessed").terminal.provenance.confidence,
        )
    }

    @Test
    fun `a device-verified claim must cite the probe that produced it`() {
        // The guarantee is not "nothing is verified" — the A910S genuinely was measured over adb.
        // It is that VERIFIED_FROM_DEVICE can never be asserted without evidence, so the UI's green
        // badge always means someone actually read the numbers off hardware.
        fun check(id: String, p: SpecProvenance) {
            if (p.confidence == SpecConfidence.VERIFIED_FROM_DEVICE) {
                assertTrue(
                    p.source.contains("adb probe", ignoreCase = true),
                    "$id claims VERIFIED_FROM_DEVICE without citing a probe (source=\"${p.source}\")",
                )
            }
        }
        for (model in DeviceCatalog.models) {
            check(model.id, model.provenance)
            model.variants.forEach { check("${model.id}:${it.id}", it.provenance) }
        }
    }

    @Test
    fun `exactly the probed A910S is device-verified and everything else is graded lower`() {
        val verified = DeviceCatalog.models
            .filter { it.provenance.confidence == SpecConfidence.VERIFIED_FROM_DEVICE }
            .map { it.id }
        assertEquals(listOf("pax-a910s"), verified)

        // Its unmeasured variants stay honest: the 5.5" geometry was never seen on hardware.
        val bigScreen = a910s.variants.first { it.id == "5p5in" }
        assertEquals(SpecConfidence.PROVISIONAL, bigScreen.provenance.confidence)
        assertEquals(SpecConfidence.PROVISIONAL, a910s.resolve("5p5in").hardware.provenance.confidence)
    }

    // ---------- forking ----------

    @Test
    fun `forking a model records its origin and leaves the builtin untouched`() {
        val forked = a910s.fork("my-a910s", "My A910S")
        assertEquals(ProfileOrigin.USER, forked.origin)
        assertEquals("pax-a910s", forked.derivedFrom)
        assertEquals(a910s.variants.size, forked.variants.size, "variants come along with the fork")
        assertEquals(ProfileOrigin.BUILTIN, a910s.origin)
    }

    @Test
    fun `duplicating a variant copies its deltas but not its default flag or sku`() {
        val dx = DeviceCatalog["ingenico-dx8000"]!!
        val extended = dx.duplicateVariant("dx8005", "dx8005-custom", "DX8005 custom")

        assertEquals(dx.variants.size + 1, extended.variants.size)
        val copy = extended.variant("dx8005-custom")!!
        assertEquals("DX8005 custom", copy.label)
        assertFalse(copy.isDefault, "a duplicate must not steal the default flag")
        assertEquals("", copy.sku, "the vendor SKU must not be cloned onto a custom variant")

        // Duplicating something that does not exist is a no-op rather than a crash.
        assertEquals(extended.variants.size, extended.duplicateVariant("nope", "x", "X").variants.size)
    }

    @Test
    fun `an empty variant resolves to the base spec unchanged`() {
        val model = TerminalModel(
            id = "m", vendor = DeviceVendor.GENERIC, model = "M",
            baseProperties = mapOf(Props.RAM to "2048", Props.LCD_WIDTH to "720"),
            baseFeatures = setOf(DeviceFeature.ICC),
            variants = listOf(TerminalVariant(id = "plain", label = "Plain")),
        )
        val resolved = model.resolve("plain")
        assertEquals(2048, resolved.hardware.ramMb)
        assertEquals(720, resolved.hardware.screenWidth)
        assertEquals(setOf(DeviceFeature.ICC), resolved.terminal.features)
    }

    @Test
    fun `a model with no variants resolves to its base spec`() {
        val model = DeviceCatalog["pax-a920"]!!
        assertTrue(model.variants.isEmpty())
        val resolved = model.resolve()
        assertEquals("", resolved.variantId)
        assertEquals("pax-a920", resolved.terminal.id)
        assertNotNull(resolved.hardware.ramMb)
    }

    // ---------- catalog integrity ----------

    @Test
    fun `catalog covers every vendor that has terminals and ids are unique`() {
        val ids = DeviceCatalog.models.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate model id in the catalog")

        // Every vendor with a DAL flavor should have at least one model to pick.
        for (vendor in DeviceVendor.entries) {
            assertTrue(
                DeviceCatalog.byVendor(vendor).isNotEmpty(),
                "${vendor.displayName} has no models in the catalog",
            )
        }
    }

    @Test
    fun `variant ids are unique within a model and at most one is default`() {
        for (model in DeviceCatalog.models) {
            val vids = model.variants.map { it.id }
            assertEquals(vids.size, vids.toSet().size, "duplicate variant id in ${model.id}")
            assertTrue(
                model.variants.count { it.isDefault } <= 1,
                "${model.id} marks more than one variant as default",
            )
        }
    }

    @Test
    fun `every catalog entry resolves and carries usable geometry`() {
        for (model in DeviceCatalog.models) {
            val variantIds = model.variants.map { it.id }.ifEmpty { listOf("") }
            for (vid in variantIds) {
                val resolved = model.resolve(vid)
                val where = "${model.id}:$vid"
                assertNotNull(resolved.hardware.screenWidth, "$where has no width")
                assertNotNull(resolved.hardware.screenHeight, "$where has no height")
                assertNotNull(resolved.hardware.density, "$where has no density")
                assertNotNull(resolved.hardware.ramMb, "$where has no RAM")
                assertTrue(resolved.terminal.features.isNotEmpty(), "$where has no features")
            }
        }
    }

    @Test
    fun `catalog lookup and search work by id vendor model and sku`() {
        assertEquals("pax-a910s", DeviceCatalog.resolve("pax-a910s:5p5in")!!.modelId)
        assertEquals("5p5in", DeviceCatalog.resolve("pax-a910s:5p5in")!!.variantId)
        assertNull(DeviceCatalog.resolve("no-such-model"))

        assertTrue(DeviceCatalog.search("a910").any { it.id == "pax-a910s" })
        assertTrue(DeviceCatalog.search("ingenico").all { it.vendor == DeviceVendor.INGENICO })
        // Variant SKUs are searchable, which is how someone with a box in front of them finds it.
        assertTrue(DeviceCatalog.search("DX8005-USBLU01A").any { it.id == "ingenico-dx8000" })
        assertEquals(DeviceCatalog.models.size, DeviceCatalog.search("  ").size)
    }

    @Test
    fun `only PAX is flagged as having DAL work underway`() {
        // The UI shows "DAL implemented" vs "Hardware only" per model; nothing may claim a DAL that
        // does not exist yet.
        val claiming = DeviceCatalog.models.filter { it.dalStatus != DalStatus.NOT_IMPLEMENTED }
        assertEquals(listOf("pax-a910s"), claiming.map { it.id })
        assertEquals(DalStatus.IN_PROGRESS, DeviceCatalog["pax-a910s"]!!.dalStatus)
    }

    @Test
    fun `generic models are hardware only and usable without any vendor SDK`() {
        val generics = DeviceCatalog.byVendor(DeviceVendor.GENERIC)
        assertTrue(generics.isNotEmpty())
        for (model in generics) {
            assertEquals(DalStatus.NOT_IMPLEMENTED, model.dalStatus)
            assertEquals(PrinterSpec::class, model.printer::class)
        }
    }

    @Test
    fun `recommended images avoid api 29 which has no arm64 build`() {
        // Most of these terminals really run Android 10, but Google publishes no arm64 image for
        // API 29 — so no catalog entry may recommend one.
        for (model in DeviceCatalog.models) {
            assertTrue(model.recommendedImage.apiLevel >= 30, "${model.id} recommends API < 30")
            for (variant in model.variants) {
                variant.imageOverride?.let {
                    assertTrue(it.apiLevel >= 30, "${model.id}:${variant.id} overrides to API < 30")
                }
            }
        }
        assertEquals(SystemImageRef(30, "google_apis", ""), DeviceCatalog["pax-a920"]!!.recommendedImage)
    }
}
