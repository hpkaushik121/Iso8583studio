package `in`.aicortex.iso8583studio.logging

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * The complete log history for one log file: the writer that appends to it, the index that makes it
 * seekable, and the window cache that decodes the rows currently on screen.
 *
 * The file — not the in-memory list in the UI — is the authoritative record. That inversion is what
 * lets the live view be capped without losing history: anything trimmed from the tail ring is still
 * reachable through this.
 *
 * Instances are shared per absolute path via [of], because the producer (a simulator service, deep
 * in the network path) and the consumer (a log tab) have no other common owner.
 */
class LogHistory private constructor(
    private val logFile: File,
    maxSegmentBytes: Long
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshSignal = Channel<Boolean>(Channel.CONFLATED)

    private val _index = mutableStateOf<LogIndex?>(null)
    val index: State<LogIndex?> get() = _index

    /** Bumped whenever decoded rows change, so rows that were blank on a miss repaint once loaded. */
    private val _revision = mutableStateOf(0)
    val revision: State<Int> get() = _revision

    val cache = LogWindowCache(scope) { _revision.value = _revision.value + 1 }

    private val writer = LogFileWriter(
        logFile = logFile,
        maxSegmentBytes = maxSegmentBytes,
        onRotated = { refreshSignal.trySend(true) },
        onAppended = { refreshSignal.trySend(false) }
    )

    @Volatile
    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        writer.start()
        scope.launch {
            rebuild()
            for (rotated in refreshSignal) {
                if (rotated) rebuild() else extend()
            }
        }
    }

    fun append(entry: LogEntry) {
        if (!started) start()
        writer.append(entry)
    }

    /**
     * Purges the whole history: every rotated segment and the live one.
     *
     * The log view renders from this rather than from the in-memory list, so "Clear" has to reach
     * the files — clearing only the live list would leave the panel looking unchanged.
     */
    @Synchronized
    fun clear() {
        start()
        writer.reset()
        cache.rebind(LogIndex(emptyList()))
        _index.value = null
        _revision.value = _revision.value + 1
        // A rebuild may already be in flight against the files just deleted; queue another so the
        // published index cannot end up describing segments that no longer exist.
        refreshSignal.trySend(true)
    }

    /** Forces the reader to catch up now, e.g. when a log tab is opened mid-run. */
    fun refresh() {
        writer.flush()
        refreshSignal.trySend(false)
    }

    /**
     * A rotation renames every segment, so the cheapest correct response is a fresh scan rather than
     * rebasing offsets. It runs off the UI thread and is published by reference swap; at 10 MB per
     * rotation this is rare enough that the scan cost does not matter.
     */
    private suspend fun rebuild() {
        val segments = LogFileWriter.segmentsFor(logFile)
        if (segments.isEmpty()) return
        val built = LogIndex.build(segments, writer.flushedBytes)
        cache.rebind(built)
        _index.value = built
        _revision.value = _revision.value + 1
    }

    private suspend fun extend() {
        val current = _index.value ?: return rebuild()
        val before = current.recordCount
        current.extendLive(writer.flushedBytes)
        if (current.recordCount != before) {
            cache.invalidateTail(before)
            _revision.value = _revision.value + 1
        }
    }

    companion object {
        private val instances = ConcurrentHashMap<String, LogHistory>()

        fun of(logFilePath: String, maxSizeMB: Int): LogHistory? {
            if (logFilePath.isBlank()) return null
            val file = File(logFilePath).absoluteFile
            val maxBytes = (maxSizeMB.coerceAtLeast(1)).toLong() * 1024 * 1024
            return instances.computeIfAbsent(file.path) { LogHistory(file, maxBytes) }
        }
    }
}
