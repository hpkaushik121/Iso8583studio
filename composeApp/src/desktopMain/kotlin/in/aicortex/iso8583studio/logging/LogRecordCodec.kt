package `in`.aicortex.iso8583studio.logging

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * On-disk representation of a [LogEntry].
 *
 * Field names are abbreviated because this is written once per log record and the file is the
 * authoritative history: at ~100 MB of retention the key names alone would otherwise cost several
 * megabytes. [LogEntry.metadata] is `Map<String, Any>`, which kotlinx-serialization cannot encode,
 * so values are flattened to their string form — metadata is diagnostic display data only.
 */
@Serializable
internal data class LogRecordDto(
    val ts: String,
    val ty: Int,
    val msg: String,
    val det: String? = null,
    val id: String = "",
    val src: String? = null,
    val sid: String? = null,
    val cid: String? = null,
    val meta: Map<String, String> = emptyMap()
)

/**
 * Encodes/decodes one log record per physical line.
 *
 * Wire format:  `<typeOrdinal>\t<renderedLineCount>\t<json>\n`
 *
 * The tab-delimited prefix exists so [LogIndex] can index a segment without parsing JSON — it only
 * needs the type (for filtering) and the rendered line count (for the prefix sum). That keeps
 * indexing at I/O speed instead of parse speed. JSON escapes newlines, so one record is always
 * exactly one physical line and `\n` is an unambiguous record separator even for hex dumps.
 */
object LogRecordCodec {

    private val json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    /** Number of rendered rows a record occupies. Must agree with [LogLineRenderer.render]. */
    fun renderedLineCount(entry: LogEntry): Int {
        val messageLines = splitLines(entry.message).size
        val detailLines = entry.details?.let { splitLines(it).size } ?: 0
        return messageLines + detailLines
    }

    fun encode(entry: LogEntry): String {
        val dto = LogRecordDto(
            ts = entry.timestamp,
            ty = entry.type.ordinal,
            msg = entry.message,
            det = entry.details,
            id = entry.id,
            src = entry.source,
            sid = entry.sessionId,
            cid = entry.correlationId,
            meta = entry.metadata.mapValues { (_, v) -> v.toString() }
        )
        return "${entry.type.ordinal}\t${renderedLineCount(entry)}\t${json.encodeToString(dto)}"
    }

    /** Returns null for a truncated or malformed line (e.g. the tail of a hard-killed process). */
    fun decode(line: String): LogEntry? {
        val jsonStart = prefixEnd(line) ?: return null
        return try {
            val dto = json.decodeFromString<LogRecordDto>(line.substring(jsonStart))
            LogEntry(
                timestamp = dto.ts,
                type = logTypeOf(dto.ty),
                message = dto.msg,
                details = dto.det,
                id = dto.id,
                source = dto.src,
                sessionId = dto.sid,
                correlationId = dto.cid,
                metadata = dto.meta
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reads just the `<typeOrdinal>\t<lineCount>` prefix. Returns null if the line is malformed,
     * which the indexer treats as a record to skip.
     */
    fun decodePrefix(line: String): Pair<Int, Int>? {
        val firstTab = line.indexOf('\t')
        if (firstTab <= 0) return null
        val secondTab = line.indexOf('\t', firstTab + 1)
        if (secondTab <= firstTab) return null
        val type = line.substring(0, firstTab).toIntOrNull() ?: return null
        val lines = line.substring(firstTab + 1, secondTab).toIntOrNull() ?: return null
        if (lines <= 0) return null
        return type to lines
    }

    fun logTypeOf(ordinal: Int): LogType =
        LogType.values().getOrElse(ordinal) { LogType.INFO }

    private fun prefixEnd(line: String): Int? {
        val firstTab = line.indexOf('\t')
        if (firstTab < 0) return null
        val secondTab = line.indexOf('\t', firstTab + 1)
        if (secondTab < 0) return null
        return secondTab + 1
    }

    /** Normalises CRLF/CR so line counting and rendering agree regardless of how a message was built. */
    fun splitLines(text: String): List<String> =
        text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
}
