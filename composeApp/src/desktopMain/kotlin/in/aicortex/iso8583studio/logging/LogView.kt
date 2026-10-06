package `in`.aicortex.iso8583studio.logging

/**
 * An immutable scroll model over a [LogIndex], optionally narrowed by log type.
 *
 * A type filter changes the total row count, so it needs its own ordinal mapping. Because the index
 * already carries each record's type, that mapping is built from memory alone — no disk reads and
 * no re-index — which is what keeps toggling a filter instant even at full retention. When every
 * type is selected the filter arrays are skipped entirely and this delegates straight to the index.
 */
class LogView(
    val index: LogIndex,
    val selected: Set<LogType>
) {
    private val filteredRecords: IntArray?
    private val filteredLineStart: IntArray?

    val lineCount: Int

    init {
        val all = selected.size == LogType.values().size
        if (all) {
            filteredRecords = null
            filteredLineStart = null
            lineCount = index.lineCount
        } else if (selected.isEmpty()) {
            filteredRecords = IntArray(0)
            filteredLineStart = IntArray(0)
            lineCount = 0
        } else {
            val mask = BooleanArray(LogType.values().size)
            selected.forEach { mask[it.ordinal] = true }

            var matches = 0
            for (r in 0 until index.recordCount) {
                if (mask[index.typeOf(r).ordinal]) matches++
            }
            val records = IntArray(matches)
            val starts = IntArray(matches)
            var cursor = 0
            var lines = 0
            for (r in 0 until index.recordCount) {
                if (!mask[index.typeOf(r).ordinal]) continue
                records[cursor] = r
                starts[cursor] = lines
                lines += index.lineCountOf(r)
                cursor++
            }
            filteredRecords = records
            filteredLineStart = starts
            lineCount = lines
        }
    }

    /** Maps a scroll row ordinal to the record that owns it. */
    fun resolve(lineOrdinal: Int): LineLocator? {
        if (lineOrdinal < 0 || lineOrdinal >= lineCount) return null
        val records = filteredRecords ?: return index.lineToRecord(lineOrdinal)
        val starts = filteredLineStart ?: return null
        if (records.isEmpty()) return null

        var lo = 0
        var hi = records.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (starts[mid] <= lineOrdinal) lo = mid else hi = mid - 1
        }
        return LineLocator(records[lo], lineOrdinal - starts[lo])
    }

    fun typeOf(recordIndex: Int): LogType = index.typeOf(recordIndex)
}
