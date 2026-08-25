package `in`.aicortex.iso8583studio.posSimulator

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.HardwarePropertiesSchema
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.PropertyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Asserts against the real emulator schema bundled at `resources/avd/hardware-properties.ini`
 * (emulator 36.3.10). The exact counts are deliberate: they are what makes a regression in the
 * record-delimiting logic visible, since a parser that split on blank lines instead of on the next
 * `name =` would still produce a plausible-looking but wrong map.
 */
class HardwarePropertiesSchemaTest {

    private val schema = HardwarePropertiesSchema.bundled()

    @Test
    fun `parses every record in the bundled schema`() {
        assertEquals(153, schema.size)
    }

    @Test
    fun `classifies types exactly as the file declares them`() {
        val byType = schema.properties.values.groupingBy { it.type }.eachCount()
        assertEquals(47, byType[PropertyType.BOOLEAN])
        assertEquals(56, byType[PropertyType.INTEGER])
        assertEquals(46, byType[PropertyType.STRING])
        assertEquals(4, byType[PropertyType.DISK_SIZE])
    }

    @Test
    fun `handles unstable key order within a record`() {
        // hw.screen lists `enum` BEFORE `default`...
        val screen = schema["hw.screen"]!!
        assertEquals("multi-touch", screen.default)
        assertEquals(listOf("touch", "multi-touch", "no-touch"), screen.enumValues)

        // ...while disk.cachePartition.size lists `abstract` before it.
        val cache = schema["disk.cachePartition.size"]!!
        assertEquals(PropertyType.DISK_SIZE, cache.type)
        assertEquals("66MB", cache.default)
        assertEquals("Cache partition size", cache.abstract)
    }

    @Test
    fun `tolerates an empty default and a missing description`() {
        val cpuModel = schema["hw.cpu.model"]!!
        assertEquals("", cpuModel.default)
        assertEquals(PropertyType.STRING, cpuModel.type)

        // Only 72 of the 153 records carry a description line at all.
        val touchpadWidth = schema["hw.touchpad0.width"]!!
        assertEquals("", touchpadWidth.description)
        assertEquals("Touchpad width", touchpadWidth.abstract)
        assertEquals("600", touchpadWidth.default)
    }

    @Test
    fun `finds all eleven enums and marks the three open ended ones`() {
        val enums = schema.properties.values.filter { it.enumValues.isNotEmpty() }
        assertEquals(11, enums.size)

        val openEnded = enums.filter { it.enumOpenEnded }.map { it.name }.toSet()
        // `emulated, none, webcam0, ...` on both cameras, and `freeform, ...` on display.settings.xml.
        assertEquals(setOf("hw.camera.back", "hw.camera.front", "display.settings.xml"), openEnded)

        val back = schema["hw.camera.back"]!!
        assertTrue(back.enumOpenEnded)
        assertFalse(back.isClosedEnum, "an open enum must not drive a strict dropdown")
        assertFalse(back.enumValues.contains("..."), "the ellipsis marker must not leak into values")
        assertEquals(listOf("emulated", "none", "webcam0"), back.enumValues)

        val orientation = schema["hw.initialOrientation"]!!
        assertTrue(orientation.isClosedEnum)
        assertEquals(listOf("portrait", "landscape"), orientation.enumValues)
    }

    @Test
    fun `exposes the properties the AVD builder actually edits`() {
        assertEquals(PropertyType.INTEGER, schema["hw.ramSize"]!!.type)
        assertEquals(PropertyType.INTEGER, schema["hw.lcd.width"]!!.type)
        assertEquals(PropertyType.INTEGER, schema["hw.lcd.density"]!!.type)
        assertEquals(PropertyType.INTEGER, schema["hw.cpu.ncore"]!!.type)
        assertEquals(PropertyType.BOOLEAN, schema["hw.keyboard"]!!.type)
        assertEquals(PropertyType.BOOLEAN, schema["hw.sdCard"]!!.type)
        assertEquals(PropertyType.DISK_SIZE, schema["disk.dataPartition.size"]!!.type)
        assertEquals(PropertyType.STRING, schema["hw.gpu.mode"]!!.type)
    }

    @Test
    fun `hw lcd density enum in the file is stale and must be overridden by the catalog`() {
        // The shipped enum stops at 320; real buckets go to 640. Recorded here so the catalog's
        // enumOverride has a test that will fail loudly if the file ever catches up.
        val density = schema["hw.lcd.density"]!!
        assertEquals(listOf("120", "160", "240", "213", "320"), density.enumValues)
        assertFalse(density.enumValues.contains("420"))
    }

    @Test
    fun `parses booleans leniently but formats them strictly`() {
        for (truthy in listOf("yes", "YES", "true", "1", "on", " yes ")) {
            assertEquals(true, HardwarePropertiesSchema.parseBoolean(truthy), "failed on '$truthy'")
        }
        // hw.arc = false appears in a hand-built AVD on this machine, so `false` must parse.
        for (falsy in listOf("no", "NO", "false", "0", "off")) {
            assertEquals(false, HardwarePropertiesSchema.parseBoolean(falsy), "failed on '$falsy'")
        }
        assertNull(HardwarePropertiesSchema.parseBoolean("maybe"))

        assertEquals("yes", HardwarePropertiesSchema.formatBoolean(true))
        assertEquals("no", HardwarePropertiesSchema.formatBoolean(false))
    }

    @Test
    fun `normalize canonicalises booleans and disk sizes and leaves everything else alone`() {
        assertEquals("no", schema.normalize("hw.arc", "false"))
        assertEquals("yes", schema.normalize("hw.keyboard", "true"))
        assertEquals("6G", schema.normalize("disk.dataPartition.size", "6442450944"))
        assertEquals("2048", schema.normalize("hw.ramSize", "2048"))
        // Unknown property: pass through untouched rather than destroying something we don't model.
        assertEquals("whatever", schema.normalize("hw.some.future.property", "whatever"))
        // Unparseable value of a known property: also pass through, for the validator to flag.
        assertEquals("banana", schema.normalize("disk.dataPartition.size", "banana"))
    }

    @Test
    fun `defaults omit empty values so they do not pollute the effective property map`() {
        val defaults = schema.defaults()
        assertEquals("2", defaults["hw.cpu.ncore"])
        assertEquals("multi-touch", defaults["hw.screen"])
        assertFalse(defaults.containsKey("hw.cpu.model"), "empty defaults must not be emitted")
    }

    @Test
    fun `ignores comments and blank lines and requires a name to open a record`() {
        val text = """
            # leading comment
            name        = hw.example
            type        = boolean

            # an interleaved comment inside the record
            default     = yes
            abstract    = Example

            type        = integer
            name        = hw.second
            default     = 7
        """.trimIndent()

        val parsed = HardwarePropertiesSchema.parse(text)
        assertEquals(2, parsed.size)
        assertEquals("yes", parsed["hw.example"]!!.default)
        assertEquals("Example", parsed["hw.example"]!!.abstract)

        // The blank line on 148 does NOT close the record: the `type = integer` that follows it is
        // still part of hw.example and overwrites its earlier `type = boolean`. Only the next
        // `name =` closes a record. This is the whole point of the test — a parser that split on
        // blank lines would give hw.example BOOLEAN and hand INTEGER to hw.second, which looks
        // plausible and is wrong.
        assertEquals(PropertyType.INTEGER, parsed["hw.example"]!!.type)
        assertEquals("7", parsed["hw.second"]!!.default)
        assertEquals(PropertyType.STRING, parsed["hw.second"]!!.type)
    }

    @Test
    fun `falls back to the bundled snapshot when no SDK is present`() {
        val loaded = HardwarePropertiesSchema.load(null)
        assertEquals(153, loaded.size)
    }
}
