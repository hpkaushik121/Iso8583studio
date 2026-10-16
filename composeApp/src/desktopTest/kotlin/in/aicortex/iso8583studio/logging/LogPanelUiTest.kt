package `in`.aicortex.iso8583studio.logging

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Composes the real log panel over a real on-disk history.
 *
 * The layout it exercises is the risky part of the virtualized renderer: a `LazyColumn` nested
 * inside a horizontally scrollable parent, which is how every row shares one horizontal offset.
 * A regression there breaks the entire Logs tab, and no unit test on the index would catch it.
 */
@OptIn(ExperimentalTestApi::class)
class LogPanelUiTest {

    private fun entry(i: Int) = LogEntry(
        timestamp = "12:00:00.000",
        type = LogType.TRANSACTION,
        message = "row marker $i",
        details = "detail for $i",
        source = "Test"
    )

    @Test
    fun `panel renders rows from a large on-disk history`() {
        val dir = Files.createTempDirectory("logpanel").toFile()
        val file = File(dir, "logs.txt")
        try {
            val history = LogHistory.of(file.path, 10)!!
            history.start()
            repeat(20_000) { history.append(entry(it)) }
            history.refresh()

            // Wait for the index outside the Compose test clock: the panel reads it as snapshot
            // state, and the test harness does not observe background snapshot writes on its own.
            val indexed = runBlocking {
                withTimeoutOrNull(20_000) {
                    while ((history.index.value?.recordCount ?: 0) < 20_000) delay(25)
                    true
                }
            }
            assertTrue(indexed == true, "index did not build")

            runDesktopComposeUiTest(width = 1400, height = 900) {
                setContent {
                    MaterialTheme {
                        LogPanelWithAutoScroll(
                            label = "Logs",
                            onClearClick = {},
                            logEntries = mutableStateListOf(),
                            history = history
                        )
                    }
                }

                waitForIdle()

                waitForIdle()

                // Following the tail must survive opening the panel: the newest row is on screen.
                waitUntil(timeoutMillis = 30_000) {
                    onAllNodesWithText("row marker 19999", substring = true)
                        .fetchSemanticsNodes().isNotEmpty()
                }

                // Only a viewport's worth of rows may exist, never all 40k of them.
                val allRendered = onAllNodesWithText("row marker", substring = true)
                    .fetchSemanticsNodes()
                assertTrue(
                    allRendered.size < 200,
                    "rendered ${allRendered.size} rows; the list is not virtualized"
                )
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
