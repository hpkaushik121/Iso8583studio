package `in`.aicortex.iso8583studio.logging

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/** The end-to-end path a live log view depends on: append -> flush -> index -> visible row count. */
class LogHistoryLiveTest {

    @Test
    fun `appended entries become visible rows`() = runBlocking {
        val dir = Files.createTempDirectory("loglive").toFile()
        val file = File(dir, "logs.txt")
        try {
            val history = LogHistory.of(file.path, 10)!!
            history.start()
            repeat(1_000) {
                history.append(
                    LogEntry("12:00:00.000", LogType.INFO, "m $it", null, source = "T")
                )
            }
            history.refresh()

            val reached = withTimeoutOrNull(10_000) {
                while ((history.index.value?.recordCount ?: 0) < 1_000) delay(25)
                true
            }
            assertEquals(true, reached, "index never caught up; got ${history.index.value?.recordCount}")
        } finally {
            dir.deleteRecursively()
        }
    }
}
