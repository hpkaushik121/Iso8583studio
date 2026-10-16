package `in`.aicortex.iso8583studio.logging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

/**
 * Keeps the rows near the viewport decoded, and nothing else.
 *
 * Records are grouped into fixed chunks so one cache miss costs a single contiguous read rather
 * than a seek per row. A miss returns null immediately and loads off-thread; the caller draws a
 * blank row of identical height, so nothing reflows and the scrollbar never jumps. Neighbouring
 * chunks are prefetched, which is what keeps a fling from ever showing that blank row.
 *
 * Memory is bounded by [MAX_CHUNKS] × [CHUNK_RECORDS] decoded records regardless of history size.
 */
class LogWindowCache(
    private val scope: CoroutineScope,
    private val onChunkLoaded: () -> Unit
) {
    companion object {
        private const val CHUNK_RECORDS = 256
        private const val MAX_CHUNKS = 24
    }

    private class Chunk(val firstRecord: Int, val lines: Array<List<RenderedLine>>)

    private val lock = Any()
    private val chunks = LinkedHashMap<Int, Chunk>(32, 0.75f, true)
    private val inFlight = HashSet<Int>()
    private val ioMutex = Mutex()

    /** Index the cache is currently serving. Changing it invalidates everything. */
    @Volatile
    private var index: LogIndex? = null

    fun rebind(newIndex: LogIndex) {
        synchronized(lock) {
            index = newIndex
            chunks.clear()
            inFlight.clear()
        }
    }

    /** Drops decoded rows but keeps the binding — used when the live segment grew. */
    fun invalidateTail(fromRecord: Int) {
        synchronized(lock) {
            val stale = chunks.keys.filter { it * CHUNK_RECORDS + CHUNK_RECORDS > fromRecord }
            stale.forEach { chunks.remove(it) }
        }
    }

    /**
     * The row at [lineWithin] of [recordIndex], or null if not resident yet. Never blocks.
     * Callers resolve a scroll ordinal to a record via [LogView] first, so this stays independent
     * of whether a type filter is active.
     */
    fun lineAt(recordIndex: Int, lineWithin: Int): RenderedLine? {
        val idx = index ?: return null
        if (recordIndex < 0 || recordIndex >= idx.recordCount) return null
        val chunkId = recordIndex / CHUNK_RECORDS

        val chunk = synchronized(lock) { chunks[chunkId] }
        request(idx, chunkId + 1)
        if (chunkId > 0) request(idx, chunkId - 1)
        if (chunk == null) {
            request(idx, chunkId)
            return null
        }

        val rows = chunk.lines.getOrNull(recordIndex - chunk.firstRecord) ?: return null
        return rows.getOrNull(lineWithin)
    }

    private fun request(idx: LogIndex, chunkId: Int) {
        if (chunkId < 0) return
        val first = chunkId * CHUNK_RECORDS
        if (first >= idx.recordCount) return
        synchronized(lock) {
            if (chunks.containsKey(chunkId) || !inFlight.add(chunkId)) return
        }
        scope.launch(Dispatchers.IO) {
            val loaded = try {
                ioMutex.withLock { load(idx, chunkId) }
            } catch (_: Exception) {
                null
            }
            synchronized(lock) {
                inFlight.remove(chunkId)
                if (loaded != null && index === idx) {
                    chunks[chunkId] = loaded
                    while (chunks.size > MAX_CHUNKS) {
                        val eldest = chunks.keys.iterator()
                        if (!eldest.hasNext()) break
                        eldest.next()
                        eldest.remove()
                    }
                }
            }
            if (loaded != null) onChunkLoaded()
        }
    }

    private fun load(idx: LogIndex, chunkId: Int): Chunk? {
        val first = chunkId * CHUNK_RECORDS
        if (first >= idx.recordCount) return null
        val last = minOf(first + CHUNK_RECORDS, idx.recordCount) - 1

        val rows = arrayOfNulls<List<RenderedLine>>(last - first + 1)

        // Records are contiguous in the file, so read them per segment in one pass rather than
        // seeking per record.
        var i = first
        while (i <= last) {
            val ref = idx.refOf(i)
            var j = i
            var end = ref.offset + ref.length
            while (j + 1 <= last) {
                val next = idx.refOf(j + 1)
                if (next.file != ref.file) break
                j++
                end = next.offset + next.length
            }
            readRange(ref.file, ref.offset, (end - ref.offset).toInt()) { bytes ->
                var k = i
                while (k <= j) {
                    val r = idx.refOf(k)
                    val start = (r.offset - ref.offset).toInt()
                    val line = String(bytes, start, r.length, StandardCharsets.UTF_8)
                    val entry = LogRecordCodec.decode(line)
                    rows[k - first] = entry?.let { LogLineRenderer.render(it) } ?: emptyList()
                    k++
                }
            }
            i = j + 1
        }

        return Chunk(first, Array(rows.size) { rows[it] ?: emptyList() })
    }

    private inline fun readRange(file: File, offset: Long, length: Int, use: (ByteArray) -> Unit) {
        if (length <= 0) return
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            val bytes = ByteArray(length)
            raf.readFully(bytes)
            use(bytes)
        }
    }
}
