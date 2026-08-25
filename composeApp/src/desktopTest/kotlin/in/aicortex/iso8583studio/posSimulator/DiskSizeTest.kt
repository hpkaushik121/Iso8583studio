package `in`.aicortex.iso8583studio.posSimulator

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.DiskSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiskSizeTest {

    @Test
    fun `parses the forms that appear in real config ini files`() {
        // Studio writes sdcard.size = 512M but disk.dataPartition.size = 6442450944.
        assertEquals(512L * DiskSize.MB, DiskSize.parse("512M")?.bytes)
        assertEquals(6L * DiskSize.GB, DiskSize.parse("6442450944")?.bytes)
        // hardware-properties.ini declares disk.cachePartition.size default = 66MB.
        assertEquals(66L * DiskSize.MB, DiskSize.parse("66MB")?.bytes)
        assertEquals(0L, DiskSize.parse("0")?.bytes)
    }

    @Test
    fun `is case insensitive and tolerates whitespace and a trailing B`() {
        val expected = 9L * DiskSize.MB
        for (form in listOf("9m", "9M", "9mb", "9MB", " 9M ", "9 M".replace(" ", ""))) {
            assertEquals(expected, DiskSize.parse(form)?.bytes, "failed on: '$form'")
        }
        assertEquals(2L * DiskSize.GB, DiskSize.parse("2g")?.bytes)
        assertEquals(1L * DiskSize.KB, DiskSize.parse("1k")?.bytes)
        assertEquals(1L * DiskSize.TB, DiskSize.parse("1t")?.bytes)
    }

    @Test
    fun `renders the most compact exact unit`() {
        assertEquals("6G", DiskSize(6L * DiskSize.GB).toString())
        assertEquals("512M", DiskSize(512L * DiskSize.MB).toString())
        assertEquals("66M", DiskSize(66L * DiskSize.MB).toString())
        assertEquals("0", DiskSize(0).toString())
        // 1536M is 1.5G, not exactly divisible by G, so it stays in M.
        assertEquals("1536M", DiskSize(1536L * DiskSize.MB).toString())
        // Not a multiple of any unit -> raw bytes.
        assertEquals("12345", DiskSize(12345).toString())
    }

    @Test
    fun `round trips through parse and render`() {
        for (form in listOf("512M", "6G", "1T", "4K", "0")) {
            assertEquals(form, DiskSize.parse(form)?.toString())
        }
    }

    @Test
    fun `rejects unparseable input rather than defaulting`() {
        // A mistyped partition size must surface as a validation error, not a mysterious boot failure.
        for (bad in listOf("", "   ", "abc", "M", "B", "-5M", "5X", "1.5G", "512MMB")) {
            assertNull(DiskSize.parse(bad), "should not have parsed: '$bad'")
        }
    }

    @Test
    fun `guards against overflow on absurd input`() {
        assertNull(DiskSize.parse("99999999999999T"))
    }

    @Test
    fun `exposes megabytes for RAM style comparisons`() {
        assertEquals(2048L, DiskSize.ofMegabytes(2048).megabytes)
        assertEquals(6144L, DiskSize.parse("6G")?.megabytes)
    }
}
