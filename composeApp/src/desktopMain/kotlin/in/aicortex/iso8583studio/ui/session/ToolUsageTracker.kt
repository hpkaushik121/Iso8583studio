package `in`.aicortex.iso8583studio.ui.session

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Tracks how often each StudioTool is opened and persists counts to disk.
 * Used by HomeScreenViewModel to build a dynamic "Quick Access" list.
 *
 * Storage: ~/.iso8583studio/tool_usage.properties (simple key=count format)
 */
object ToolUsageTracker {

    private val prefsDir = File(System.getProperty("user.home"), ".iso8583studio")
    private val prefsFile = File(prefsDir, "tool_usage.properties")

    /** Reactive map: toolName → openCount */
    val counts = mutableStateMapOf<String, Int>()

    init {
        load()
    }

    /** Call this every time a tool is opened (by label). */
    fun recordUsage(toolLabel: String) {
        counts[toolLabel] = (counts[toolLabel] ?: 0) + 1
        persistAsync()
    }

    /** Returns tool labels sorted by descending usage count (most-used first). */
    fun topLabels(n: Int): List<String> =
        counts.entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key }

    fun usageOf(toolLabel: String): Int = counts[toolLabel] ?: 0

    // ── Persistence ──────────────────────────────────────────────────────────

    private fun load() {
        if (!prefsFile.exists()) return
        try {
            prefsFile.readLines().forEach { line ->
                val parts = line.split("=")
                if (parts.size == 2) {
                    val label = parts[0].trim()
                    val count = parts[1].trim().toIntOrNull() ?: 0
                    if (label.isNotBlank() && count > 0) counts[label] = count
                }
            }
        } catch (_: Exception) {}
    }

    private fun persistAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                prefsDir.mkdirs()
                prefsFile.writeText(
                    counts.entries.joinToString("\n") { "${it.key}=${it.value}" }
                )
            } catch (_: Exception) {}
        }
    }
}
