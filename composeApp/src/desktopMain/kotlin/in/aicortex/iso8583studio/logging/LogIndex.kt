package `in`.aicortex.iso8583studio.logging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

/** Where a record lives on disk. */
class RecordRef(val file: File, val offset: Long, val length: Int)

/** Which record a global row belongs to, and which of that record's rows it is. */
class LineLocator(val recordIndex: Int, val lineWithinRecord: Int)

/**
 * Maps a scroll position to a byte offset without reading the log.
 *
 * One entry per *record* rather than per rendered row, with a prefix sum of row counts, so a row
 * ordinal resolves by binary search (~21 comparisons) and reading it is a single seek. That is what
 * lets the whole retained history sit under one continuous scrollbar with no paging step: the total
 * row count is known up front, so `LazyColumn` can size itself and jump anywhere.
 *
 * Indexing never parses JSON — [LogRecordCodec] writes a tab-delimited `type\tlineCount` prefix on
 * each line precisely so this scan runs at I/O speed. Cost is ~13 bytes per record, bounded by
 * retention (~1 MB for a typical 100 MB of ISO 8583 traffic).
 *
 * Not thread-safe: build and extend on one background thread, publish by reference swap.
 */
class LogIndex(val segments: List<File>) {

    private var recSeg = ByteArray(INITIAL_CAPACITY)
    private var recOffset = IntArray(INITIAL_CAPACITY)
    private var recLength = IntArray(INITIAL_CAPACITY)
    private var recLineStart = IntArray(INITIAL_CAPACITY)
    private var recType = ByteArray(INITIAL_CAPACITY)

    // Volatile because the index is extended on an IO thread while the UI reads it. Both counters are
    // written after the array stores in addRecord, so a reader that sees a new count also sees the
    // rows behind it; appends never move existing entries, so older indices stay valid either way.
    @Volatile
    var recordCount: Int = 0
        private set

    @Volatile
    var lineCount: Int = 0
        private set

    /** Bytes of the live (last) segment already indexed, so [extendLive] resumes rather than rescans. */
    var liveScannedBytes: Long = 0
        private set

    companion object {
        private const val INITIAL_CAPACITY = 4096
        private const val SCAN_BUFFER = 1 shl 20
        private const val MAX_PREFIX_BYTES = 40

        /**
         * Indexes every segment. [liveLimit] caps the last segment at the writer's flushed
         * watermark; bytes above it are still buffered and must not be read.
         */
        suspend fun build(segments: List<File>, liveLimit: Long): LogIndex =
            withContext(Dispatchers.IO) {
                val index = LogIndex(segments)
                segments.forEachIndexed { i, file ->
                    val isLive = i == segments.lastIndex
                    val limit = if (isLive) liveLimit else file.length()
                    val scanned = index.scanSegment(file, i, 0L, limit)
                    if (isLive) index.liveScannedBytes = scanned
                }
                index
            }
    }

    /** Appends records flushed to the live segment since the last scan. */
    suspend fun extendLive(liveLimit: Long) = withContext(Dispatchers.IO) {
        if (segments.isEmpty()) return@withContext
        if (liveLimit <= liveScannedBytes) return@withContext
        liveScannedBytes = scanSegment(segments.last(), segments.lastIndex, liveScannedBytes, liveLimit)
    }

    fun lineToRecord(lineOrdinal: Int): LineLocator? {
        if (lineOrdinal < 0 || lineOrdinal >= lineCount || recordCount == 0) return null
        var lo = 0
        var hi = recordCount - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (recLineStart[mid] <= lineOrdinal) lo = mid else hi = mid - 1
        }
        return LineLocator(lo, lineOrdinal - recLineStart[lo])
    }

    fun firstLineOf(recordIndex: Int): Int = recLineStart[recordIndex]

    fun typeOf(recordIndex: Int): LogType = LogRecordCodec.logTypeOf(recType[recordIndex].toInt())

    fun refOf(recordIndex: Int): RecordRef = RecordRef(
        file = segments[recSeg[recordIndex].toInt()],
        offset = recOffset[recordIndex].toLong(),
        length = recLength[recordIndex]
    )

    fun lineCountOf(recordIndex: Int): Int {
        val next = if (recordIndex + 1 >= recordCount) lineCount else recLineStart[recordIndex + 1]
        return next - recLineStart[recordIndex]
    }

    /**
     * Scans `[from, limit)` of one segment, appending a record per complete line.
     * Returns the offset just past the last complete line, so a partially written trailing record
     * (a hard kill mid-flush) is re-read next time rather than indexed as truncated.
     */
    private fun scanSegment(file: File, segmentIndex: Int, from: Long, limit: Long): Long {
        if (!file.exists() || limit <= from) return from
        var consumed = from
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(from)
            val buffer = ByteArray(SCAN_BUFFER)
            var bufferStart = from
            var lineStart = from
            val prefix = ByteArray(MAX_PREFIX_BYTES)
            var prefixLen = 0
            var tabs = 0

            while (bufferStart < limit) {
                val want = minOf(buffer.size.toLong(), limit - bufferStart).toInt()
                val read = raf.read(buffer, 0, want)
                if (read <= 0) break
                for (i in 0 until read) {
                    val b = buffer[i]
                    if (b == NEWLINE) {
                        val absolute = bufferStart + i
                        val length = (absolute - lineStart).toInt()
                        if (length > 0 && tabs >= 2) {
                            addRecord(
                                segmentIndex,
                                lineStart,
                                length,
                                String(prefix, 0, prefixLen, Charsets.US_ASCII)
                            )
                        }
                        lineStart = absolute + 1
                        consumed = lineStart
                        prefixLen = 0
                        tabs = 0
                    } else {
                        if (tabs < 2 && prefixLen < MAX_PREFIX_BYTES) prefix[prefixLen++] = b
                        if (b == TAB) tabs++
                    }
                }
                bufferStart += read
            }
        }
        return consumed
    }

    private fun addRecord(segmentIndex: Int, offset: Long, length: Int, prefix: String) {
        val parsed = LogRecordCodec.decodePrefix(prefix) ?: return
        ensureCapacity(recordCount + 1)
        recSeg[recordCount] = segmentIndex.toByte()
        recOffset[recordCount] = offset.toInt()
        recLength[recordCount] = length
        recLineStart[recordCount] = lineCount
        recType[recordCount] = parsed.first.toByte()
        recordCount++
        lineCount += parsed.second
    }

    private fun ensureCapacity(needed: Int) {
        if (needed <= recSeg.size) return
        val newSize = maxOf(needed, recSeg.size * 2)
        recSeg = recSeg.copyOf(newSize)
        recOffset = recOffset.copyOf(newSize)
        recLength = recLength.copyOf(newSize)
        recLineStart = recLineStart.copyOf(newSize)
        recType = recType.copyOf(newSize)
    }
}

private const val NEWLINE: Byte = 0x0A
private const val TAB: Byte = 0x09
