package `in`.aicortex.iso8583studio.logging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

/**
 * A flat, virtualizable row model for the log panel.
 *
 * [rowAt] returns null when a row exists but is not decoded yet; the caller must draw a blank row of
 * the same height rather than skipping it, so that a cache miss never changes layout.
 */
class LogRows(
    val count: Int,
    val truncatedBefore: Boolean,
    private val getter: (Int) -> RenderedLine?
) {
    fun rowAt(index: Int): RenderedLine? = getter(index)
}

/**
 * Rows for the log panel, from the on-disk history when one is configured and from the in-memory
 * list otherwise.
 *
 * The history path is what makes the whole retained log scrollable: row count comes from the index,
 * so the scrollbar spans everything, and rows decode lazily as they come into view. The in-memory
 * path is the fallback for views with no log file (the calculator screens, and any simulator with
 * logging to file switched off) — those lists are user-triggered and small, so rendering them
 * eagerly is fine.
 */
@Composable
fun rememberLogRows(
    history: LogHistory?,
    entries: List<LogEntry>,
    selected: Set<LogType>,
    liveEntryCap: Int = 0
): LogRows {
    if (history != null) {
        // Opening a tab mid-run must show what is already on disk, not just what arrives next.
        androidx.compose.runtime.LaunchedEffect(history) {
            history.start()
            history.refresh()
        }

        val index by history.index
        val revision by history.revision
        val currentIndex = index

        val view = remember(currentIndex, revision, selected) {
            currentIndex?.let { LogView(it, selected) }
        }

        return remember(view, revision) {
            val v = view
            if (v == null) {
                LogRows(0, false) { null }
            } else {
                LogRows(v.lineCount, false) { ordinal ->
                    v.resolve(ordinal)?.let { locator ->
                        history.cache.lineAt(locator.recordIndex, locator.lineWithinRecord)
                    }
                }
            }
        }
    }

    val rendered = remember(entries.size, entries.lastOrNull()?.id, selected) {
        entries.asSequence()
            .filter { it.type in selected }
            .flatMap { LogLineRenderer.render(it).asSequence() }
            .toList()
    }
    val truncated = liveEntryCap > 0 && entries.size >= liveEntryCap
    return remember(rendered, truncated) {
        LogRows(rendered.size, truncated) { rendered.getOrNull(it) }
    }
}
