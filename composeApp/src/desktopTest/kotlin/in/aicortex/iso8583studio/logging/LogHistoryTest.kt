package `in`.aicortex.iso8583studio.logging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers the write -> index -> read path that backs infinite log scrolling. The invariant that
 * matters is that the row count the scrollbar is sized from always matches the rows actually
 * readable, and that every row round-trips to the text it was written with.
 */
class LogHistoryTest {

    private fun tempDir(): File = Files.createTempDirectory("logtest").toFile().apply { deleteOnExit() }

    private fun entry(i: Int, type: LogType = LogType.INFO, details: String? = null) = LogEntry(
        timestamp = "12:00:%02d.000".format(i % 60),
        type = type,
        message = "message $i",
        details = details,
        source = "TestSource"
    )

    private fun writeAll(file: File, entries: List<LogEntry>, maxBytes: Long = 10L * 1024 * 1024): LogFileWriter {
        val writer = LogFileWriter(file, maxBytes, flushIntervalMs = 10_000)
        writer.start()
        entries.forEach { writer.append(it) }
        writer.flush()
        return writer
    }

    // ---------- codec ----------

    @Test
    fun `record round-trips through the codec`() {
        val original = LogEntry(
            timestamp = "01:02:03.004",
            type = LogType.ENCRYPTION,
            message = "line one\nline two",
            details = "detail one\ndetail two",
            source = "Crypto",
            correlationId = "STAN-1",
            metadata = mapOf("keyId" to "K1", "slot" to 3)
        )
        val decoded = LogRecordCodec.decode(LogRecordCodec.encode(original))
        assertNotNull(decoded)
        assertEquals(original.message, decoded.message)
        assertEquals(original.details, decoded.details)
        assertEquals(original.type, decoded.type)
        assertEquals(original.source, decoded.source)
        assertEquals(original.correlationId, decoded.correlationId)
        assertEquals("3", decoded.metadata["slot"])
    }

    @Test
    fun `encoded record occupies exactly one physical line`() {
        val encoded = LogRecordCodec.encode(
            entry(1, details = "hex:\r\n00 01 02\r\n03 04 05")
        )
        assertEquals(0, encoded.count { it == '\n' })
    }

    @Test
    fun `declared line count matches what the renderer produces`() {
        listOf(
            entry(1),
            entry(2, details = "one line"),
            entry(3, details = "a\nb\nc"),
            LogEntry("t", LogType.INFO, "m1\r\nm2\r\nm3", "d1\nd2")
        ).forEach {
            assertEquals(
                LogRecordCodec.renderedLineCount(it),
                LogLineRenderer.render(it).size,
                "line count mismatch for: ${it.message}"
            )
        }
    }

    // ---------- index ----------

    @Test
    fun `index row count equals the sum of rendered rows`() = runBlocking {
        val dir = tempDir()
        val file = File(dir, "logs.txt")
        val entries = (0 until 500).map {
            entry(it, details = if (it % 3 == 0) "d1\nd2" else null)
        }
        val writer = writeAll(file, entries)

        val index = LogIndex.build(LogFileWriter.segmentsFor(file), writer.flushedBytes)
        assertEquals(entries.size, index.recordCount)
        assertEquals(entries.sumOf { LogRecordCodec.renderedLineCount(it) }, index.lineCount)
        writer.stop()
    }

    @Test
    fun `every row resolves to the record that produced it`() = runBlocking {
        val dir = tempDir()
        val file = File(dir, "logs.txt")
        val entries = (0 until 200).map { entry(it, details = if (it % 2 == 0) "d" else null) }
        val writer = writeAll(file, entries)
        val index = LogIndex.build(LogFileWriter.segmentsFor(file), writer.flushedBytes)

        var expectedRecord = 0
        var seenInRecord = 0
        for (row in 0 until index.lineCount) {
            val locator = index.lineToRecord(row)
            assertNotNull(locator, "row $row did not resolve")
            assertEquals(expectedRecord, locator.recordIndex, "wrong record for row $row")
            assertEquals(seenInRecord, locator.lineWithinRecord)
            seenInRecord++
            if (seenInRecord == LogRecordCodec.renderedLineCount(entries[expectedRecord])) {
                expectedRecord++
                seenInRecord = 0
            }
        }
        assertEquals(entries.size, expectedRecord)
        writer.stop()
    }

    @Test
    fun `a partially written trailing record is ignored until complete`() = runBlocking {
        val dir = tempDir()
        val file = File(dir, "logs.txt")
        val entries = (0 until 10).map { entry(it) }
        val writer = writeAll(file, entries)
        writer.stop()

        // Simulate a hard kill mid-record: append a line with no terminating newline.
        file.appendText(LogRecordCodec.encode(entry(99)).substring(0, 12))

        val index = LogIndex.build(LogFileWriter.segmentsFor(file), file.length())
        assertEquals(entries.size, index.recordCount)
    }

    @Test
    fun `index only reads up to the flushed watermark`() = runBlocking {
        val dir = tempDir()
        val file = File(dir, "logs.txt")
        val writer = LogFileWriter(file, 10L * 1024 * 1024, flushIntervalMs = 10_000)
        writer.start()
        (0 until 20).forEach { writer.append(entry(it)) }
        writer.flush()
        val watermark = writer.flushedBytes

        // Buffered but not flushed: must be invisible to readers.
        (20 until 40).forEach { writer.append(entry(it)) }

        val index = LogIndex.build(LogFileWriter.segmentsFor(file), watermark)
        assertEquals(20, index.recordCount)

        writer.flush()
        index.extendLive(writer.flushedBytes)
        assertEquals(40, index.recordCount)
        writer.stop()
    }

    // ---------- rotation ----------

    @Test
    fun `rotation preserves every record across segments`() = runBlocking {
        val dir = tempDir()
        val file = File(dir, "logs.txt")
        // Tiny segments so a few hundred records force several rotations.
        val writer = LogFileWriter(file, 4_000L, flushIntervalMs = 10_000)
        writer.start()
        val entries = (0 until 400).map { entry(it) }
        entries.forEach { writer.append(it) }
        writer.flush()

        val segments = LogFileWriter.segmentsFor(file)
        assertTrue(segments.size > 1, "expected rotation to have produced multiple segments")

        val index = LogIndex.build(segments, writer.flushedBytes)
        assertEquals(index.lineCount, index.recordCount) // one row each, no details

        // Records must remain in chronological order across the segment boundary.
        val cache = LogWindowCache(CoroutineScope(SupervisorJob() + Dispatchers.IO)) {}
        cache.rebind(index)
        val texts = readAllRows(cache, index)
        val numbers = texts.map { it.substringAfter("message ").substringBefore(" ").trim().toInt() }
        assertEquals(numbers.sorted(), numbers, "rows are out of order across segments")
        writer.stop()
    }

    // ---------- read path ----------

    @Test
    fun `rows read back match what was written`() = runBlocking {
        val dir = tempDir()
        val file = File(dir, "logs.txt")
        val entries = (0 until 300).map { entry(it, details = if (it % 5 == 0) "detail $it" else null) }
        val writer = writeAll(file, entries)
        val index = LogIndex.build(LogFileWriter.segmentsFor(file), writer.flushedBytes)

        val cache = LogWindowCache(CoroutineScope(SupervisorJob() + Dispatchers.IO)) {}
        cache.rebind(index)

        val expected = entries.flatMap { LogLineRenderer.render(it) }.map { LogLineRenderer.plainText(it) }
        assertEquals(expected, readAllRows(cache, index))
        writer.stop()
    }

    // ---------- filtering ----------

    @Test
    fun `type filter narrows rows without touching unrelated records`() = runBlocking {
        val dir = tempDir()
        val file = File(dir, "logs.txt")
        val entries = (0 until 100).map {
            entry(it, type = if (it % 4 == 0) LogType.ERROR else LogType.INFO)
        }
        val writer = writeAll(file, entries)
        val index = LogIndex.build(LogFileWriter.segmentsFor(file), writer.flushedBytes)

        val all = LogView(index, LogType.values().toSet())
        assertEquals(index.lineCount, all.lineCount)

        val errorsOnly = LogView(index, setOf(LogType.ERROR))
        assertEquals(25, errorsOnly.lineCount)
        for (row in 0 until errorsOnly.lineCount) {
            val locator = errorsOnly.resolve(row)
            assertNotNull(locator)
            assertEquals(LogType.ERROR, index.typeOf(locator.recordIndex))
        }

        assertEquals(0, LogView(index, emptySet()).lineCount)
        writer.stop()
    }

    private suspend fun readAllRows(cache: LogWindowCache, index: LogIndex): List<String> {
        val out = ArrayList<String>(index.lineCount)
        for (row in 0 until index.lineCount) {
            val locator = index.lineToRecord(row) ?: error("row $row did not resolve")
            var line = cache.lineAt(locator.recordIndex, locator.lineWithinRecord)
            var waited = 0
            while (line == null && waited < 200) {
                delay(10)
                waited++
                line = cache.lineAt(locator.recordIndex, locator.lineWithinRecord)
            }
            out.add(LogLineRenderer.plainText(line ?: error("row $row never loaded")))
        }
        return out
    }
}
