package `in`.aicortex.iso8583studio.logging

/** Which palette role a span takes. Resolved to real colours at composition, where the theme is known. */
enum class SpanRole { TIMESTAMP, TYPE, MESSAGE, DETAILS }

class LineSpan(val text: String, val role: SpanRole)

/** One rendered row. Rows are uniform height, which is what makes the virtualized scrollbar exact. */
class RenderedLine(val spans: List<LineSpan>, val type: LogType)

/**
 * Splits a [LogEntry] into the rows it occupies on screen.
 *
 * This is the same layout the old single-`Text` renderer produced via `buildStructuredLogText`,
 * but emitted one row at a time so the list can be virtualized. The row count returned here must
 * match [LogRecordCodec.renderedLineCount], which the index relies on.
 */
object LogLineRenderer {

    private const val TYPE_COLUMN_WIDTH = 12

    fun render(entry: LogEntry): List<RenderedLine> {
        val out = ArrayList<RenderedLine>(LogRecordCodec.renderedLineCount(entry))
        val prefix = "[${entry.timestamp}] "
        val baseIndent = prefix.length + TYPE_COLUMN_WIDTH + 2
        val pad = " ".repeat(baseIndent)

        val messageLines = LogRecordCodec.splitLines(entry.message)

        out.add(
            RenderedLine(
                listOf(
                    LineSpan(prefix, SpanRole.TIMESTAMP),
                    LineSpan(
                        entry.type.displayName.uppercase().padEnd(TYPE_COLUMN_WIDTH) + ": ",
                        SpanRole.TYPE
                    ),
                    LineSpan(messageLines.first(), SpanRole.MESSAGE)
                ),
                entry.type
            )
        )

        messageLines.drop(1).forEach { line ->
            val trimmed = line.trim()
            val text = if (trimmed.isEmpty()) "" else pad + trimmed
            out.add(RenderedLine(listOf(LineSpan(text, SpanRole.MESSAGE)), entry.type))
        }

        entry.details?.let { details ->
            val detailLines = LogRecordCodec.splitLines(details)
            out.add(
                RenderedLine(
                    listOf(LineSpan("$pad└─ ${detailLines.first()}", SpanRole.DETAILS)),
                    entry.type
                )
            )
            detailLines.drop(1).forEach { line ->
                val trimmed = line.trim()
                val text = if (trimmed.isEmpty()) "" else "$pad   $trimmed"
                out.add(RenderedLine(listOf(LineSpan(text, SpanRole.DETAILS)), entry.type))
            }
        }

        return out
    }

    /** Character count of a row, without materialising the joined string. */
    fun charLength(line: RenderedLine): Int {
        var total = 0
        for (span in line.spans) total += span.text.length
        return total
    }

    /** Plain-text form of a row, for copy/export. */
    fun plainText(line: RenderedLine): String =
        line.spans.joinToString("") { it.text }
}
