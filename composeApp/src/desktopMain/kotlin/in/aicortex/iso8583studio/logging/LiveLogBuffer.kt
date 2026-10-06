package `in`.aicortex.iso8583studio.logging

import androidx.compose.runtime.snapshots.SnapshotStateList
import `in`.aicortex.iso8583studio.data.model.AppSettings

/**
 * Appends to a live log view, dropping the oldest entries past the display cap.
 *
 * This is a *window*, not retention. Every entry trimmed here is already on disk via [LogHistory]
 * and stays reachable by scrolling, so the cap costs no history — it only stops the view from
 * growing without bound, which is what previously exhausted the heap on long simulator runs.
 */
fun SnapshotStateList<LogEntry>.addBounded(entry: LogEntry) {
    add(entry)
    val cap = AppSettings.maxLiveLogEntries
    if (size > cap) {
        removeRange(0, size - cap)
    }
}
