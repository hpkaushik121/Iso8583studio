package `in`.aicortex.iso8583studio.logging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Exercises the read path at the scale that used to exhaust the heap, and reports the numbers behind
 * the "scroll anywhere instantly" claim. Bounds are deliberately loose — this guards against an
 * accidental return to linear behaviour, not against machine-to-machine variation.
 */
class LogScaleTest {

    private fun realisticEntry(i: Int): LogEntry {
        // Roughly the shape of gateway traffic: a hex dump plus a parsed field breakdown.
        val hex = (0 until 24).joinToString(" ") { "%02X".format((i + it) and 0xFF) }
        return LogEntry(
            timestamp = "12:%02d:%02d.%03d".format((i / 60) % 60, i % 60, i % 1000),
            type = if (i % 7 == 0) LogType.ERROR else LogType.TRANSACTION,
            message = "ISO8583 message #$i\n$hex",
            details = "MTI: 0200\nSTAN: %06d\nField 4: %012d".format(i % 999999, i.toLong() * 17),
            source = "Gateway"
        )
    }

    @Test
    fun `large history stays seekable and bounded`() = runBlocking {
        val recordCount = 200_000
        val dir = Files.createTempDirectory("logscale").toFile()
        val file = File(dir, "logs.txt")

        try {
            val writer = LogFileWriter(file, 10L * 1024 * 1024, flushIntervalMs = 10_000)
            writer.start()
            val writeMs = measure {
                for (i in 0 until recordCount) writer.append(realisticEntry(i))
                writer.flush()
            }

            val segments = LogFileWriter.segmentsFor(file)
            val bytesOnDisk = segments.sumOf { it.length() }

            lateinit var index: LogIndex
            val indexMs = measure { index = LogIndex.build(segments, writer.flushedBytes) }

            val runtime = Runtime.getRuntime()
            System.gc()
            val heapBefore = runtime.totalMemory() - runtime.freeMemory()

            val cache = LogWindowCache(CoroutineScope(SupervisorJob() + Dispatchers.IO)) {}
            cache.rebind(index)

            // Jump to 200 random positions, the way a scrollbar drag does.
            val random = Random(1234)
            var resolved = 0
            val seekMs = measure {
                repeat(200) {
                    val row = random.nextInt(index.lineCount)
                    val locator = index.lineToRecord(row) ?: return@repeat
                    var line = cache.lineAt(locator.recordIndex, locator.lineWithinRecord)
                    var waited = 0
                    while (line == null && waited < 300) {
                        delay(2)
                        waited++
                        line = cache.lineAt(locator.recordIndex, locator.lineWithinRecord)
                    }
                    if (line != null) resolved++
                }
            }

            System.gc()
            val heapAfter = runtime.totalMemory() - runtime.freeMemory()
            val heapGrowthMb = (heapAfter - heapBefore) / (1024.0 * 1024.0)

            println(
                """
                |--- log scale ---
                |records         : $recordCount
                |rows            : ${index.lineCount}
                |segments        : ${segments.size}
                |on disk         : ${"%.1f".format(bytesOnDisk / 1024.0 / 1024.0)} MB
                |write           : $writeMs ms
                |index build     : $indexMs ms
                |200 random seeks: $seekMs ms (${"%.2f".format(seekMs / 200.0)} ms each)
                |heap for reads  : ${"%.1f".format(heapGrowthMb)} MB
                """.trimMargin()
            )

            assertTrue(resolved == 200, "only $resolved/200 random rows resolved")
            assertTrue(index.lineCount > recordCount, "expected multi-row records")
            // Reading 200 scattered rows must not pull the log into memory.
            assertTrue(
                heapGrowthMb < 96,
                "reading scattered rows retained ${"%.1f".format(heapGrowthMb)} MB; cache is not bounded"
            )
            writer.stop()
        } finally {
            dir.deleteRecursively()
        }
    }

    private inline fun measure(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000
    }
}
