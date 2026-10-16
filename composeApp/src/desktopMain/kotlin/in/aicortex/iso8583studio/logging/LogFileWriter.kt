package `in`.aicortex.iso8583studio.logging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/**
 * Append-only writer for the authoritative log history.
 *
 * Replaces the previous `File(path).appendText(...)` call per log line, which opened, wrote and
 * closed the file on every record on the caller's thread. This keeps one buffered handle open and
 * flushes on an interval, and it owns rotation so the handle is closed before files are renamed —
 * renaming an open file is unsafe on Windows.
 *
 * Records are written by [LogRecordCodec] as one line each, always UTF-8: the file is now parsed
 * back by [LogIndex]/[LogWindowCache], so the encoding must round-trip rather than follow the
 * gateway's configured wire encoding.
 *
 * Readers must only trust bytes below [flushedBytes]; anything above it is still in the buffer.
 */
class LogFileWriter(
    private val logFile: File,
    private val maxSegmentBytes: Long,
    private val flushIntervalMs: Long = 250L,
    private val onRotated: () -> Unit = {},
    private val onAppended: () -> Unit = {}
) {
    companion object {
        const val MAX_ROTATIONS = 10

        /**
         * Every retained segment, oldest first. Mirrors the rotation naming used by [rotate]:
         * `base1.ext` is the oldest rotation, `base10.ext` the newest, and the live file is last.
         */
        fun segmentsFor(logFile: File): List<File> {
            val parent = logFile.parentFile ?: File(".")
            val base = logFile.nameWithoutExtension.split('_').first()
            val ext = logFile.extension
            val rotations = (1..MAX_ROTATIONS)
                .map { File(parent, "$base$it.$ext") }
                .filter { it.exists() }
            return if (logFile.exists()) rotations + logFile else rotations
        }
    }

    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var out: BufferedOutputStream? = null
    private var segmentBytes: Long = 0
    private var pendingBytes: Long = 0

    @Volatile
    var flushedBytes: Long = 0
        private set

    private var flushJob: Job? = null

    fun start() {
        synchronized(lock) {
            if (out != null) return
            logFile.parentFile?.mkdirs()
            segmentBytes = if (logFile.exists()) logFile.length() else 0
            flushedBytes = segmentBytes
            out = BufferedOutputStream(FileOutputStream(logFile, true), 64 * 1024)
        }
        flushJob = scope.launch {
            while (isActive) {
                delay(flushIntervalMs)
                flush()
            }
        }
    }

    fun append(entry: LogEntry) {
        val bytes = (LogRecordCodec.encode(entry) + "\n").toByteArray(StandardCharsets.UTF_8)
        var rotated = false
        synchronized(lock) {
            val stream = out ?: return
            stream.write(bytes)
            segmentBytes += bytes.size
            pendingBytes += bytes.size
            if (segmentBytes >= maxSegmentBytes) {
                rotateLocked()
                rotated = true
            }
        }
        if (rotated) onRotated()
    }

    /**
     * Publishes buffered records to readers. Only called on the flush interval and on stop, never
     * per record — readers extend their index from the watermark this advances, so flushing more
     * often would just churn the index for no visible benefit.
     */
    fun flush() {
        val advanced = synchronized(lock) {
            val stream = out
            if (stream == null || pendingBytes == 0L) {
                false
            } else {
                stream.flush()
                flushedBytes = segmentBytes
                pendingBytes = 0
                true
            }
        }
        if (advanced) onAppended()
    }

    /**
     * Deletes every segment and starts a fresh live file, keeping this writer usable.
     * Distinct from [stop], which tears down the flush job for shutdown.
     */
    fun reset() {
        synchronized(lock) {
            try {
                out?.flush()
                out?.close()
            } catch (_: Exception) {
            }
            out = null
            segmentsFor(logFile).forEach {
                try {
                    it.delete()
                } catch (_: Exception) {
                }
            }
            segmentBytes = 0
            pendingBytes = 0
            flushedBytes = 0
            out = try {
                BufferedOutputStream(FileOutputStream(logFile, true), 64 * 1024)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun stop() {
        flushJob?.cancel()
        flushJob = null
        synchronized(lock) {
            out?.let {
                try {
                    it.flush()
                    it.close()
                } catch (_: Exception) {
                }
            }
            out = null
            flushedBytes = segmentBytes
        }
        scope.cancel()
    }

    /**
     * Same retention semantics as the previous `checkAndRotateLogFile`: fill `base1..base10`, then
     * drop the oldest and shift the rest down. Unlike the original this closes the handle first and
     * reopens after, so the live file is never renamed while open.
     */
    private fun rotateLocked() {
        val parent = logFile.parentFile ?: File(".")
        val base = logFile.nameWithoutExtension.split('_').first()
        val ext = logFile.extension

        try {
            out?.flush()
            out?.close()
        } catch (_: Exception) {
        }
        out = null

        try {
            var rotationIndex = 1
            while (File(parent, "$base$rotationIndex.$ext").exists() && rotationIndex != MAX_ROTATIONS + 1) {
                rotationIndex++
            }
            if (rotationIndex == MAX_ROTATIONS + 1) {
                File(parent, "${base}1.$ext").delete()
                for (i in 2..MAX_ROTATIONS) {
                    val old = File(parent, "$base$i.$ext")
                    val new = File(parent, "$base${i - 1}.$ext")
                    if (old.exists()) old.renameTo(new)
                }
                rotationIndex = MAX_ROTATIONS
            }
            logFile.renameTo(File(parent, "$base$rotationIndex.$ext"))
        } catch (_: Exception) {
        }

        segmentBytes = 0
        pendingBytes = 0
        flushedBytes = 0
        out = try {
            BufferedOutputStream(FileOutputStream(logFile, true), 64 * 1024)
        } catch (_: Exception) {
            null
        }
    }
}
