package `in`.aicortex.iso8583studio.posSimulator

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.ConfigIni
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.HardwarePropertiesSchema
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigIniTest {

    /** Android Studio's newer style, taken verbatim from ~/.android/avd/Pixel_6.avd/config.ini. */
    private val spacedStyle = """
        AvdId = Pixel_6
        PlayStore.enabled = false
        abi.type = arm64-v8a
        avd.ini.displayname = Pixel 6
        disk.dataPartition.size = 6442450944
        hw.lcd.density = 420
        hw.ramSize = 2048
    """.trimIndent() + "\n"

    /** avdmanager's style, taken verbatim from ~/.android/avd/Kozen_N2.avd/config.ini. */
    private val compactStyle = """
        AvdId=Custom
        abi.type=arm64-v8a
        disk.dataPartition.size=6G
        fastboot.chosenSnapshotFile=
        hw.arc=false
        hw.lcd.density=160
        hw.lcd.height=480
        hw.lcd.width=480
        hw.ramSize=2048
    """.trimIndent() + "\n"

    @Test
    fun `detects and preserves both separator styles found in the wild`() {
        assertEquals(ConfigIni.SPACED_SEPARATOR, ConfigIni.parse(spacedStyle).separator)
        assertEquals(ConfigIni.COMPACT_SEPARATOR, ConfigIni.parse(compactStyle).separator)
    }

    @Test
    fun `round trips both styles byte for byte`() {
        // Both fixtures are already ASCII-sorted, as the real files are, so render() must reproduce
        // them exactly. This is what guarantees we never gratuitously rewrite an AVD we only read.
        assertEquals(spacedStyle, ConfigIni.parse(spacedStyle).render())
        assertEquals(compactStyle, ConfigIni.parse(compactStyle).render())
    }

    @Test
    fun `preserves empty values`() {
        val ini = ConfigIni.parse(compactStyle)
        assertEquals("", ini["fastboot.chosenSnapshotFile"])
        assertTrue("fastboot.chosenSnapshotFile" in ini)
        assertTrue(ini.render().contains("fastboot.chosenSnapshotFile=\n"))
    }

    @Test
    fun `keeps both raw disk size forms as written`() {
        // 6442450944 and 6G are the same size and both occur; reading must not normalise either.
        assertEquals("6442450944", ConfigIni.parse(spacedStyle)["disk.dataPartition.size"])
        assertEquals("6G", ConfigIni.parse(compactStyle)["disk.dataPartition.size"])
    }

    @Test
    fun `overlay preserves keys it does not mention`() {
        val original = ConfigIni.parse(compactStyle)
        val updated = original.withOverlay(mapOf("hw.ramSize" to "4096"))

        assertEquals("4096", updated["hw.ramSize"])
        // Everything else survives — this is the forward-compatibility guarantee for keys a newer
        // emulator adds that we do not model.
        assertEquals("false", updated["hw.arc"])
        assertEquals("480", updated["hw.lcd.width"])
        assertEquals(original.size, updated.size)
    }

    @Test
    fun `overlay can remove a key with null`() {
        val updated = ConfigIni.parse(compactStyle).withOverlay(mapOf("hw.arc" to null))
        assertNull(updated["hw.arc"])
        assertTrue("hw.ramSize" in updated)
    }

    @Test
    fun `overlay normalises through the schema but only for keys being changed`() {
        val schema = HardwarePropertiesSchema.bundled()
        val updated = ConfigIni.parse(compactStyle).withOverlay(
            mapOf(
                "hw.keyboard" to "true",                  // boolean -> yes
                "disk.dataPartition.size" to "6442450944", // diskSize -> 6G
            ),
            schema,
        )
        assertEquals("yes", updated["hw.keyboard"])
        assertEquals("6G", updated["disk.dataPartition.size"])
        // Untouched keys keep their original raw text even though `false` is not canonical.
        assertEquals("false", updated["hw.arc"])
    }

    @Test
    fun `render sorts keys ascii so uppercase leads`() {
        val ini = ConfigIni.of(
            linkedMapOf("hw.ramSize" to "2048", "AvdId" to "X", "abi.type" to "arm64-v8a")
        )
        assertEquals("AvdId=X\nabi.type=arm64-v8a\nhw.ramSize=2048\n", ini.render())
    }

    @Test
    fun `sorts by key not by whole line`() {
        // Subtle, and load-bearing for byte-identical round trips. Kozen_N2 on this machine contains
        // hw.keyboard immediately followed by hw.keyboard.lid. Sorting by KEY puts the shorter
        // prefix first, which is what the SDK tooling does. Sorting whole LINES would invert them,
        // because '.' (0x2E) sorts before '=' (0x3D):
        //     line sort:  hw.keyboard.lid=yes  <  hw.keyboard=yes
        //     key  sort:  hw.keyboard          <  hw.keyboard.lid
        val ini = ConfigIni.parse("hw.keyboard.lid=yes\nhw.keyboard=yes\n")
        assertEquals("hw.keyboard=yes\nhw.keyboard.lid=yes\n", ini.render())
    }

    @Test
    fun `diff reports only differing keys in both directions`() {
        val a = ConfigIni.parse("hw.ramSize=2048\nhw.lcd.width=480\nonly.in.a=1\n")
        val b = ConfigIni.parse("hw.ramSize=4096\nhw.lcd.width=480\nonly.in.b=2\n")

        val diff = a.diff(b)
        assertEquals(setOf("hw.ramSize", "only.in.a", "only.in.b"), diff.keys)
        assertEquals("2048" to "4096", diff["hw.ramSize"])
        assertEquals("1" to null, diff["only.in.a"])
        assertEquals(null to "2", diff["only.in.b"])
    }

    @Test
    fun `builds the pointer ini that makes an AVD visible to avdmanager and studio`() {
        val avdDir = Paths.get("/Users/x/.android/avd/ISO8583_PAX_A910S.avd")
        val rendered = ConfigIni.pointerIni(avdDir, apiLevel = 30).render()
        assertEquals(
            "avd.ini.encoding=UTF-8\n" +
                "path=/Users/x/.android/avd/ISO8583_PAX_A910S.avd\n" +
                "path.rel=avd/ISO8583_PAX_A910S.avd\n" +
                "target=android-30\n",
            rendered,
        )
    }

    @Test
    fun `round trips this machine's real AVDs when they are present`() {
        val avdHome = Paths.get(System.getProperty("user.home"), ".android", "avd")
        val realConfigs = listOf("Pixel_6", "Kozen_N2", "KIOSK_27")
            .map { avdHome.resolve("$it.avd/config.ini") }
            .filter { it.exists() }

        if (realConfigs.isEmpty()) return // No local AVDs; the synthetic fixtures above still cover it.

        for (path in realConfigs) {
            val text = path.readText()
            val rendered = ConfigIni.read(path).render()
            // Real files are ASCII-sorted already, so this is a true byte-for-byte round trip.
            assertEquals(text, rendered, "round trip changed $path")
        }
    }
}
