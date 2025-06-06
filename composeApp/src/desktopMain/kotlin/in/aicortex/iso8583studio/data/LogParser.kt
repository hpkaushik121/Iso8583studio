package `in`.aicortex.iso8583studio.logging

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Robust log formatter with detailed debugging and simple logic
 */

object PlainTextLogParser {

    fun parseAndFormat(plainText: String): String {
        if (plainText.isBlank()) return plainText

        val lines = plainText.split("\n")
        val result = mutableListOf<String>()
        var currentIndentSize = 0

        for (i in lines.indices) {
            val line = lines[i]

            when {
                line.isBlank() -> {
                    result.add(line)
                }

                hasTimestamp(line) -> {
                    // This is a main log entry
                    result.add(line)
                    currentIndentSize = findContentStart(line)
                }

                else -> {
                    // This is a continuation line - add proper indentation
                    val trimmedLine = line.trim()
                    if (trimmedLine.isNotEmpty()) {
                        val spaces = " ".repeat(currentIndentSize)
                        result.add(spaces + trimmedLine)
                    } else {
                        result.add("")
                    }
                }
            }
        }

        return result.joinToString("\n")
    }

    internal fun hasTimestamp(line: String): Boolean {
        // Simple check: does the line start with [XX:XX:XX.XXX]
        return line.matches(Regex("^\\[\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\].*"))
    }

    internal fun findContentStart(line: String): Int {
        // For consistent alignment, we'll use a fixed position after the longest possible log type
        // This ensures all continuation lines align vertically regardless of log type length

        // Find where timestamp ends: [HH:mm:ss.SSS]
        val timestampEnd = line.indexOf(']')
        if (timestampEnd == -1) return 0

        // Standard format: [timestamp] LOG_TYPE: content
        // We'll align all content to start at a consistent position
        // Longest common log types: ENCRYPTION (10 chars), CONNECTION (10 chars)
        // So we'll align everything to position: timestamp + space + 12 chars + ": "

        val fixedLogTypeWidth = 12  // Accommodate longest log type names
        return timestampEnd + 1 + 1 + fixedLogTypeWidth + 2  // "] " + LOG_TYPE + ": "
    }
}

/**
 * Debug version that shows what's happening
 */
object DebugLogParser {

    fun parseAndFormatWithDebug(plainText: String): Pair<String, List<String>> {
        if (plainText.isBlank()) return Pair(plainText, emptyList())

        val lines = plainText.split("\n")
        val result = mutableListOf<String>()
        val debugInfo = mutableListOf<String>()
        var currentIndentSize = 0

        for (i in lines.indices) {
            val line = lines[i]

            when {
                line.isBlank() -> {
                    result.add(line)
                    debugInfo.add("Line $i: BLANK")
                }

                PlainTextLogParser.hasTimestamp(line) -> {
                    result.add(line)
                    currentIndentSize = PlainTextLogParser.findContentStart(line)
                    debugInfo.add("Line $i: MAIN ENTRY - indent size = $currentIndentSize")
                    debugInfo.add("  Content: '${line.substring(0, minOf(50, line.length))}${if(line.length > 50) "..." else ""}'")
                }

                else -> {
                    val trimmedLine = line.trim()
                    if (trimmedLine.isNotEmpty()) {
                        val spaces = " ".repeat(currentIndentSize)
                        result.add(spaces + trimmedLine)
                        debugInfo.add("Line $i: CONTINUATION - added $currentIndentSize spaces")
                        debugInfo.add("  Original: '$line'")
                        debugInfo.add("  Result: '${spaces}${trimmedLine}'")
                    } else {
                        result.add("")
                        debugInfo.add("Line $i: EMPTY")
                    }
                }
            }
        }

        return Pair(result.joinToString("\n"), debugInfo)
    }
}

/**
 * Enhanced formatter for plain text logs
 */
@Composable
fun PlainTextLogDisplay(
    plainText: String,
    modifier: Modifier = Modifier,
    enableFormatting: Boolean = true,
    showDebug: Boolean = false
) {
    val (formattedText, debugInfo) = remember(plainText, enableFormatting, showDebug) {
        if (enableFormatting && plainText.isNotBlank()) {
            if (showDebug) {
                DebugLogParser.parseAndFormatWithDebug(plainText)
            } else {
                Pair(PlainTextLogParser.parseAndFormat(plainText), emptyList<String>())
            }
        } else {
            Pair(plainText, emptyList<String>())
        }
    }

    Column {
        if (showDebug && debugInfo.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                elevation = 1.dp,
                color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "Debug Information:",
                        style = MaterialTheme.typography.caption,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    debugInfo.take(10).forEach { info ->
                        Text(
                            info,
                            style = MaterialTheme.typography.caption,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (debugInfo.size > 10) {
                        Text("... and ${debugInfo.size - 10} more lines", style = MaterialTheme.typography.caption)
                    }
                }
            }
        }

        SelectionContainer {
            Text(
                text = formattedText,
                modifier = modifier,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.body2,
                lineHeight = 18.sp,
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}

/**
 * Demo screen with debugging capabilities
 */
@Composable
fun PlainTextLogDemo() {
    val samplePlainText = """
[22:41:48.850] MESSAGE: Message Type =
TPDU Header =
[22:41:48.857] ERROR: in.aicortex.iso8583studio.data.model.VerificationException: INPUT DATA IS NULL OR EMPTY, CHECK IF CONFIGURATION IS AVAILABLE
[22:41:48.859] ENCRYPTION: SEND ENCRYPTED MESSAGE TO CLIENT INSTANCE: 19 BYTES
[22:41:48.861] CONNECTION: ===============CONNECTION TERMINATED===============
    └─ 
    """.trimIndent()

    var enableFormatting by remember { mutableStateOf(true) }
    var showDebug by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Debug Log Formatter",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(
                    checked = enableFormatting,
                    onCheckedChange = { enableFormatting = it }
                )
                Text("Enable Formatting", style = MaterialTheme.typography.body2)
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(
                    checked = showDebug,
                    onCheckedChange = { showDebug = it }
                )
                Text("Show Debug Info", style = MaterialTheme.typography.body2)
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            elevation = 2.dp,
            color = MaterialTheme.colors.surface
        ) {
            PlainTextLogDisplay(
                plainText = samplePlainText,
                enableFormatting = enableFormatting,
                showDebug = showDebug,
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

/**
 * Utility function to format plain text logs for use in composables
 */
@Composable
fun formatPlainTextLogs(
    plainText: String,
    enableFormatting: Boolean = true
): AnnotatedString {
    val formattedText = remember(plainText, enableFormatting) {
        if (enableFormatting && plainText.isNotBlank()) {
            PlainTextLogParser.parseAndFormat(plainText)
        } else {
            plainText
        }
    }

    return buildAnnotatedString {
        append(formattedText)
    }
}

/**
 * Format just the message part (for individual log entries without timestamps)
 * This handles multi-line messages that don't have timestamps on each line
 */
@Composable
fun formatLogMessage(
    message: String,
    baseIndentSize: Int = 0
): AnnotatedString {
    val formattedText = remember(message, baseIndentSize) {
        if (message.contains('\n')) {
            val lines = message.split('\n')
            val result = mutableListOf<String>()

            // First line stays as-is
            result.add(lines.first())

            // Subsequent lines get indented to align with first line
            lines.drop(1).forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty()) {
                    result.add(" ".repeat(baseIndentSize) + trimmedLine)
                } else {
                    result.add("")
                }
            }

            result.joinToString("\n")
        } else {
            message
        }
    }

    return buildAnnotatedString {
        append(formattedText)
    }
}

/**
 * Format details with proper multi-line indentation
 */
@Composable
fun formatLogDetails(
    details: String,
    baseIndentSize: Int
): AnnotatedString {
    val formattedText = remember(details, baseIndentSize) {
        if (details.contains('\n')) {
            val lines = details.split('\n')
            val result = mutableListOf<String>()

            // First line with tree indicator
            result.add("└─ ${lines.first()}")

            // Subsequent lines get additional indentation
            lines.drop(1).forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty()) {
                    // Align with content after the tree indicator
                    result.add(" ".repeat(baseIndentSize) + "   " + trimmedLine)  // 3 spaces for "└─ "
                } else {
                    result.add("")
                }
            }

            result.joinToString("\n")
        } else {
            "└─ $details"
        }
    }

    return buildAnnotatedString {
        append(formattedText)
    }
}

/**
 * Non-composable version for use outside of Compose context
 */
fun formatPlainTextLogsString(
    plainText: String,
    enableFormatting: Boolean = true
): String {
    return if (enableFormatting && plainText.isNotBlank()) {
        PlainTextLogParser.parseAndFormat(plainText)
    } else {
        plainText
    }
}

/**
 * Test function to verify the parser is working
 */
fun testLogParser() {
    val testLog = """
[22:41:48.850] MESSAGE: Message Type =
TPDU Header =
[22:41:48.857] ERROR: Exception occurred
[22:41:48.859] ENCRYPTION: SEND ENCRYPTED MESSAGE
    """.trimIndent()

    println("=== ORIGINAL ===")
    testLog.split("\n").forEachIndexed { i, line ->
        println("$i: '$line'")
    }

    println("\n=== ANALYSIS ===")
    testLog.split("\n").forEachIndexed { i, line ->
        val hasTs = PlainTextLogParser.hasTimestamp(line)
        val contentStart = if (hasTs) PlainTextLogParser.findContentStart(line) else -1
        println("$i: hasTimestamp=$hasTs, contentStart=$contentStart, line='$line'")
    }

    println("\n=== FORMATTED ===")
    val result = PlainTextLogParser.parseAndFormat(testLog)
    result.split("\n").forEachIndexed { i, line ->
        println("$i: '$line'")
    }
}