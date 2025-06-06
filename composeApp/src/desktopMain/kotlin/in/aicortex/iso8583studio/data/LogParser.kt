package `in`.aicortex.iso8583studio.logging

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enhanced Log Formatter for Plain Text (No Timestamp/Level)
 * Automatically detects content type and adds appropriate formatting
 */

data class PlainLogEntry(
    val originalText: String,
    val type: DetectedLogType,
    val structuredData: Map<String, String> = emptyMap()
)

enum class DetectedLogType(
    val displayName: String,
    val emoji: String
) {
    GATEWAY_START("Gateway Started", "🟢"),
    GATEWAY_STOP("Gateway Stopped", "🔴"),
    GATEWAY_CONFIG("Gateway Config", "⚙️"),
    SEPARATOR("Separator", "─"),
    TRANSACTION("Transaction", "💳"),
    CONNECTION("Connection", "🔗"),
    ERROR_EVENT("Error", "❌"),
    WARNING_EVENT("Warning", "⚠️"),
    GENERAL_INFO("Info", "ℹ️"),
    UNKNOWN("Unknown", "📝")
}

@Composable
fun DetectedLogType.getColor(): Color {
    return when (this) {
        DetectedLogType.GATEWAY_START -> MaterialTheme.colors.primary
        DetectedLogType.GATEWAY_STOP -> MaterialTheme.colors.error
        DetectedLogType.GATEWAY_CONFIG -> MaterialTheme.colors.primary
        DetectedLogType.SEPARATOR -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
        DetectedLogType.TRANSACTION -> MaterialTheme.colors.secondary
        DetectedLogType.CONNECTION -> MaterialTheme.colors.primary.copy(alpha = 0.8f)
        DetectedLogType.ERROR_EVENT -> MaterialTheme.colors.error
        DetectedLogType.WARNING_EVENT -> Color(0xFFFF9800) // Orange - not in MaterialTheme
        DetectedLogType.GENERAL_INFO -> MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        DetectedLogType.UNKNOWN -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
    }
}

object PlainTextLogParser {

    fun parseAndFormat(plainText: String): List<PlainLogEntry> {
        val lines = plainText.split("\n").filter { it.isNotBlank() }
        return lines.map { line ->
            val trimmedLine = line.trim()
            val type = detectLogType(trimmedLine)
            val structuredData = extractStructuredData(trimmedLine, type)

            PlainLogEntry(
                originalText = trimmedLine,
                type = type,
                structuredData = structuredData
            )
        }
    }

    private fun detectLogType(line: String): DetectedLogType {
        val upperLine = line.uppercase()

        return when {
            // Gateway events
            upperLine.contains("GATEWAY STARTED") || upperLine.contains("GATEWAY START") -> DetectedLogType.GATEWAY_START
            upperLine.contains("GATEWAY STOPPED") || upperLine.contains("GATEWAY STOP") -> DetectedLogType.GATEWAY_STOP

            // Configuration lines
            upperLine.contains("NAME =") || upperLine.contains("GATEWAY TYPE") ||
                    upperLine.contains("TRANSMISSION TYPE") || upperLine.contains("LOCAL ADDRESS") ||
                    upperLine.contains("REMOTE ADDRESS") || upperLine.contains("PORT =") -> DetectedLogType.GATEWAY_CONFIG

            // Separators
            line.matches(Regex("^=+$")) || line.matches(Regex("^-+$")) ||
                    line.matches(Regex("^\\*+$")) -> DetectedLogType.SEPARATOR

            // Transaction related
            upperLine.contains("TRANSACTION") || upperLine.contains("PAYMENT") ||
                    upperLine.contains("AUTHORIZATION") || upperLine.contains("MTI") ||
                    upperLine.contains("ISO8583") -> DetectedLogType.TRANSACTION

            // Connection events
            upperLine.contains("CONNECTION") || upperLine.contains("CONNECT") ||
                    upperLine.contains("DISCONNECT") || upperLine.contains("CLIENT") ||
                    upperLine.contains("SERVER") -> DetectedLogType.CONNECTION

            // Error indicators
            upperLine.contains("ERROR") || upperLine.contains("FAIL") ||
                    upperLine.contains("EXCEPTION") || upperLine.contains("TIMEOUT") -> DetectedLogType.ERROR_EVENT

            // Warning indicators
            upperLine.contains("WARNING") || upperLine.contains("WARN") ||
                    upperLine.contains("RETRY") || upperLine.contains("INVALID") -> DetectedLogType.WARNING_EVENT

            // Default
            else -> DetectedLogType.GENERAL_INFO
        }
    }

    private fun extractStructuredData(line: String, type: DetectedLogType): Map<String, String> {
        val data = mutableMapOf<String, String>()

        when (type) {
            DetectedLogType.GATEWAY_CONFIG -> {
                // Extract key-value pairs from configuration lines
                extractKeyValue(line, "Name")?.let { data["name"] = it }
                extractKeyValue(line, "Gateway type")?.let { data["gatewayType"] = it }
                extractKeyValue(line, "Transmission Type")?.let { data["transmissionType"] = it }
                extractKeyValue(line, "Description")?.let { data["description"] = it }
                extractKeyValue(line, "Local Address")?.let { data["localAddress"] = it }
                extractKeyValue(line, "Remote Address")?.let { data["remoteAddress"] = it }
                extractKeyValue(line, "Port")?.let { data["port"] = it }
            }
            DetectedLogType.TRANSACTION -> {
                // Extract transaction related data
                extractKeyValue(line, "MTI")?.let { data["mti"] = it }
                extractKeyValue(line, "Amount")?.let { data["amount"] = it }
                extractKeyValue(line, "STAN")?.let { data["stan"] = it }
            }
            DetectedLogType.CONNECTION -> {
                // Extract connection data
                extractIpAddress(line)?.let { data["ipAddress"] = it }
                extractPort(line)?.let { data["port"] = it }
            }
            else -> {
                // For other types, just store the full text
                data["message"] = line
            }
        }

        return data
    }

    private fun extractKeyValue(line: String, key: String): String? {
        val patterns = listOf(
            "$key\\s*=\\s*([^\\t\\n]+?)(?:\\s*\\t|$)".toRegex(),
            "$key\\s*:\\s*([^\\t\\n]+?)(?:\\s*\\t|$)".toRegex(),
            "$key\\s+([^\\t\\n]+?)(?:\\s*\\t|$)".toRegex()
        )

        for (pattern in patterns) {
            pattern.find(line)?.let { match ->
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    private fun extractIpAddress(line: String): String? {
        val ipPattern = "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b".toRegex()
        return ipPattern.find(line)?.value
    }

    private fun extractPort(line: String): String? {
        val portPattern = "(?:port|Port|PORT)\\s*[:=]?\\s*(\\d+)".toRegex()
        return portPattern.find(line)?.groupValues?.get(1)
    }
}

/**
 * Enhanced formatter for plain text logs
 */
@Composable
fun PlainTextLogDisplay(
    plainText: String,
    modifier: Modifier = Modifier,
    autoDetectTypes: Boolean = true
) {
    val parsedEntries = remember(plainText, autoDetectTypes) {
        if (autoDetectTypes) {
            PlainTextLogParser.parseAndFormat(plainText)
        } else {
            // Fallback: treat each line as general info
            plainText.split("\n").filter { it.isNotBlank() }.map { line ->
                PlainLogEntry(
                    originalText = line.trim(),
                    type = DetectedLogType.GENERAL_INFO
                )
            }
        }
    }

    // Build the formatted text directly in the Composable context
    val formattedText = buildPlainTextAnnotatedString(parsedEntries)

    SelectionContainer {
        Text(
            text = formattedText,
            modifier = modifier,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.body2,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun buildPlainTextAnnotatedString(
    entries: List<PlainLogEntry>
): AnnotatedString {
    return buildAnnotatedString {
        entries.forEachIndexed { index, entry ->
            buildPlainLogEntry(entry)

            // Add spacing between entries
            if (index < entries.size - 1) {
                append("\n")
            }
        }
    }
}

@Composable
private fun AnnotatedString.Builder.buildPlainLogEntry(
    entry: PlainLogEntry
) {
    // Add emoji indicator
    withStyle(SpanStyle(fontSize = 14.sp)) {
        append("${entry.type.emoji} ")
    }

    // Format based on detected type
    when (entry.type) {
        DetectedLogType.GATEWAY_START -> buildGatewayStartEntry(entry)
        DetectedLogType.GATEWAY_STOP -> buildGatewayStopEntry(entry)
        DetectedLogType.GATEWAY_CONFIG -> buildGatewayConfigEntry(entry)
        DetectedLogType.SEPARATOR -> buildSeparatorEntry(entry)
        DetectedLogType.TRANSACTION -> buildTransactionEntry(entry)
        DetectedLogType.CONNECTION -> buildConnectionEntry(entry)
        DetectedLogType.ERROR_EVENT -> buildErrorEntry(entry)
        DetectedLogType.WARNING_EVENT -> buildWarningEntry(entry)
        else -> buildGeneralEntry(entry)
    }
}

@Composable
private fun AnnotatedString.Builder.buildGatewayStartEntry(entry: PlainLogEntry) {
    withStyle(SpanStyle(
        color = DetectedLogType.GATEWAY_START.getColor(),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )) {
        append("GATEWAY STARTED")
    }

    // Add visual separator
    append("\n")
    withStyle(SpanStyle(color = MaterialTheme.colors.primary.copy(alpha = 0.3f))) {
        append("    ")
        append("━".repeat(40))
    }
}

@Composable
private fun AnnotatedString.Builder.buildGatewayStopEntry(entry: PlainLogEntry) {
    withStyle(SpanStyle(
        color = DetectedLogType.GATEWAY_STOP.getColor(),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )) {
        append("GATEWAY STOPPED")
    }

    // Add visual separator
    append("\n")
    withStyle(SpanStyle(color = MaterialTheme.colors.error.copy(alpha = 0.3f))) {
        append("    ")
        append("━".repeat(40))
    }
}

@Composable
private fun AnnotatedString.Builder.buildGatewayConfigEntry(entry: PlainLogEntry) {
    val structuredData = entry.structuredData

    if (structuredData.isNotEmpty()) {
        // Format as structured configuration
        structuredData.forEach { (key, value) ->
            append("    ")
            withStyle(SpanStyle(
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Medium
            )) {
                append("${key.replaceFirstChar { it.uppercase() }}: ")
            }

            withStyle(SpanStyle(
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Bold
            )) {
                append(value)
            }

            append("  ")
        }
    } else {
        // Fallback to original text with highlighting
        val parts = entry.originalText.split(Regex("\\s*[=:]\\s*"))
        parts.forEachIndexed { index, part ->
            if (index == 0) {
                withStyle(SpanStyle(
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Medium
                )) {
                    append(part)
                }
                append(": ")
            } else {
                withStyle(SpanStyle(
                    color = MaterialTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold
                )) {
                    append(part)
                }
                if (index < parts.size - 1) append(" | ")
            }
        }
    }
}

@Composable
private fun AnnotatedString.Builder.buildSeparatorEntry(entry: PlainLogEntry) {
    withStyle(SpanStyle(color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f))) {
        append("    ")
        append("─".repeat(45))
    }
}

@Composable
private fun AnnotatedString.Builder.buildTransactionEntry(entry: PlainLogEntry) {
    withStyle(SpanStyle(
        color = DetectedLogType.TRANSACTION.getColor(),
        fontWeight = FontWeight.Medium
    )) {
        append("TRANSACTION: ")
    }

    withStyle(SpanStyle(color = MaterialTheme.colors.onSurface)) {
        append(entry.originalText)
    }
}

@Composable
private fun AnnotatedString.Builder.buildConnectionEntry(entry: PlainLogEntry) {
    withStyle(SpanStyle(
        color = DetectedLogType.CONNECTION.getColor(),
        fontWeight = FontWeight.Medium
    )) {
        append("CONNECTION: ")
    }

    // Highlight IP addresses and ports
    val text = entry.originalText
    val ipPattern = "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b".toRegex()
    val portPattern = "\\b\\d{4,5}\\b".toRegex()

    var lastIndex = 0

    // Find and highlight IP addresses
    ipPattern.findAll(text).forEach { match ->
        // Add text before IP
        append(text.substring(lastIndex, match.range.first))

        // Add highlighted IP
        withStyle(SpanStyle(
            color = MaterialTheme.colors.secondary,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )) {
            append(match.value)
        }

        lastIndex = match.range.last + 1
    }

    // Add remaining text
    append(text.substring(lastIndex))
}

@Composable
private fun AnnotatedString.Builder.buildErrorEntry(entry: PlainLogEntry) {
    withStyle(SpanStyle(
        color = DetectedLogType.ERROR_EVENT.getColor(),
        fontWeight = FontWeight.Bold
    )) {
        append("ERROR: ")
    }

    withStyle(SpanStyle(color = MaterialTheme.colors.onSurface)) {
        append(entry.originalText)
    }
}

@Composable
private fun AnnotatedString.Builder.buildWarningEntry(entry: PlainLogEntry) {
    withStyle(SpanStyle(
        color = DetectedLogType.WARNING_EVENT.getColor(),
        fontWeight = FontWeight.Bold
    )) {
        append("WARNING: ")
    }

    withStyle(SpanStyle(color = MaterialTheme.colors.onSurface)) {
        append(entry.originalText)
    }
}

@Composable
private fun AnnotatedString.Builder.buildGeneralEntry(entry: PlainLogEntry) {
    withStyle(SpanStyle(color = MaterialTheme.colors.onSurface)) {
        append(entry.originalText)
    }
}

/**
 * Demo screen showing plain text log formatting
 */
@Composable
fun PlainTextLogDemo() {
    val samplePlainText = """
===============GATEWAY STARTED===============
Name = Config_1	 Gateway type = SERVER
Transmission Type = SYNCHRONOUS	 Description: 
Local Address = 127.0.0.1	 Port = 8080
Remote Address = 	 Port = 0
=============================================
=============================================
===============GATEWAY STOPPED===============
    """.trimIndent()

    var autoDetect by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Plain Text Log Formatter",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(
                    checked = autoDetect,
                    onCheckedChange = { autoDetect = it }
                )
                Text("Auto-detect Types", style = MaterialTheme.typography.body2)
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            elevation = 2.dp,
            color = MaterialTheme.colors.surface
        ) {
            PlainTextLogDisplay(
                plainText = samplePlainText,
                autoDetectTypes = autoDetect,
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

/**
 * Utility function to format plain text logs - must be called from Composable context
 */
@Composable
fun formatPlainTextLogs(
    plainText: String,
    autoDetectTypes: Boolean = true
): AnnotatedString {
    val entries = remember(plainText, autoDetectTypes) {
        if (autoDetectTypes) {
            PlainTextLogParser.parseAndFormat(plainText)
        } else {
            plainText.split("\n").filter { it.isNotBlank() }.map { line ->
                PlainLogEntry(
                    originalText = line.trim(),
                    type = DetectedLogType.GENERAL_INFO
                )
            }
        }
    }

    // Build the formatted text directly without remember
    return buildPlainTextAnnotatedString(entries)
}