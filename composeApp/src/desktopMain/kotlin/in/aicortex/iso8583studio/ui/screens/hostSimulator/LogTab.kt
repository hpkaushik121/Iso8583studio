package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Checkbox
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.logging.formatLogDetails
import `in`.aicortex.iso8583studio.logging.formatLogMessage
import `in`.aicortex.iso8583studio.logging.formatPlainTextLogs
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.sequences.ifEmpty
import kotlin.text.toLong

/**
 * Enhanced Log Tab with Auto-Scroll functionality and floating stats overlay
 */
@Composable
fun LogTab(
    label: String? = null,
    onClearClick: () -> Unit,
    connectionCount: Int,
    concurrentConnections: Int,
    bytesIncoming: Long,
    bytesOutgoing: Long,
    logEntries: SnapshotStateList<LogEntry> // New parameter for structured logs
) {
    var isStatsVisible by remember { mutableStateOf(false) }
    var selectedLogTypes by remember { mutableStateOf(LogType.values().toSet()) }
    println(logEntries.size)

    Box(modifier = Modifier.fillMaxSize()) {
        // Log viewer takes entire window space
        Surface(
            modifier = Modifier.fillMaxSize(),
            elevation = 2.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            LogPanelWithAutoScroll(
                label = label,
                onClearClick = onClearClick,
                logEntries = logEntries,
                selectedLogTypes = selectedLogTypes,
                onLogTypesChanged = { selectedLogTypes = it },
                isStatsVisible = isStatsVisible,
                onToggleStats = { isStatsVisible = !isStatsVisible }
            )
        }

        // Floating statistics panel
        AnimatedVisibility(
            visible = isStatsVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentWidth()
                    .height(80.dp),
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colors.surface.copy(alpha = 0.95f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingStatItem(
                        icon = Icons.Default.Router,
                        value = connectionCount.toString(),
                        label = "Conn",
                        color = MaterialTheme.colors.primary
                    )

                    FloatingStatItem(
                        icon = Icons.Default.NetworkPing,
                        value = concurrentConnections.toString(),
                        label = "Active",
                        color = Color(0xFF00BCD4)
                    )

                    FloatingStatItem(
                        icon = Icons.Default.ArrowDownward,
                        value = formatBytesCompact(bytesIncoming),
                        label = "In",
                        color = SuccessGreen
                    )

                    FloatingStatItem(
                        icon = Icons.Default.ArrowUpward,
                        value = formatBytesCompact(bytesOutgoing),
                        label = "Out",
                        color = MaterialTheme.colors.secondary
                    )

                    FloatingStatItem(
                        icon = Icons.Default.NetworkCheck,
                        value = "1",
                        label = "Sess",
                        color = WarningYellow
                    )
                }
            }
        }
    }
}

@Composable
internal fun LogPanelWithAutoScroll(
    label: String? = null,
    onClearClick: () -> Unit,
    logEntries: List<LogEntry> = emptyList(),
    selectedLogTypes: Set<LogType> = LogType.values().toSet(),
    onLogTypesChanged: (Set<LogType>) -> Unit = {},
    onBack: (() -> Unit)? = null,
    isStatsVisible: Boolean = false,
    onToggleStats: () -> Unit = {}
) {
    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Filter log entries
    val filteredLogEntries = logEntries.filter { it.type in selectedLogTypes }

    // Simple and reliable auto-scroll - triggers on ANY change to filtered entries
    LaunchedEffect(filteredLogEntries) {
        if (isAutoScrollEnabled && filteredLogEntries.isNotEmpty()) {
            // Wait a bit for UI to render
            kotlinx.coroutines.delay(50)

            // Try multiple times to ensure it works
            repeat(3) {
                if (scrollState.maxValue > 0) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                kotlinx.coroutines.delay(50)
            }
        }
    }

    // Detect manual scrolling with better logic
    var isUserScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            isUserScrolling = true
        } else {
            // When scrolling stops, check position after a delay
            kotlinx.coroutines.delay(100)

            if (scrollState.maxValue > 0) {
                val isNearBottom = scrollState.value >= (scrollState.maxValue - 100)

                if (!isNearBottom && isUserScrolling) {
                    // User scrolled away from bottom and stopped - disable auto-scroll
                    isAutoScrollEnabled = false
                }

                if (isNearBottom && !isAutoScrollEnabled) {
                    // User scrolled back to bottom - re-enable auto-scroll
                    isAutoScrollEnabled = true
                }
            }

            isUserScrolling = false
        }
    }

    // Reset auto-scroll when logs are cleared
    LaunchedEffect(logEntries.isEmpty()) {
        if (logEntries.isEmpty()) {
            isAutoScrollEnabled = true
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Article,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label ?: "Logs",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.primary
            )

            Spacer(modifier = Modifier.weight(1f))

            // Log type filter button
            Box {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    modifier = Modifier.clickable { showFilterMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter log types",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Filter (${selectedLogTypes.size})",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier.width(200.dp).heightIn(max = 250.dp)
                ) {
                    LogType.values().forEach { logType ->
                        DropdownMenuItem(
                            onClick = {
                                val newSelection = if (logType in selectedLogTypes) {
                                    selectedLogTypes - logType
                                } else {
                                    selectedLogTypes + logType
                                }
                                onLogTypesChanged(newSelection)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = logType in selectedLogTypes,
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = logType.icon,
                                    contentDescription = null,
                                    tint = logType.color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    logType.displayName,
                                    style = MaterialTheme.typography.body2
                                )
                            }
                        }
                    }

                    // Quick action buttons
                    DropdownMenuItem(onClick = {
                        onLogTypesChanged(LogType.values().toSet())
                        showFilterMenu = false
                    }) {
                        Text("Select All", fontWeight = FontWeight.Medium)
                    }

                    DropdownMenuItem(onClick = {
                        onLogTypesChanged(emptySet())
                        showFilterMenu = false
                    }) {
                        Text("Clear All", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Stats visibility toggle button
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isStatsVisible)
                    MaterialTheme.colors.primary.copy(alpha = 0.1f)
                else
                    MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                modifier = Modifier.clickable { onToggleStats() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isStatsVisible) Icons.Default.NetworkCheck else Icons.Default.NetworkPing,
                        contentDescription = if (isStatsVisible) "Hide stats" else "Show stats",
                        tint = if (isStatsVisible)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Stats",
                        style = MaterialTheme.typography.caption,
                        color = if (isStatsVisible)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Enhanced Auto-scroll toggle button
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isAutoScrollEnabled)
                    MaterialTheme.colors.primary.copy(alpha = 0.1f)
                else
                    MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                modifier = Modifier.clickable {
                    isAutoScrollEnabled = !isAutoScrollEnabled

                    // If enabling auto-scroll, immediately scroll to bottom
                    if (isAutoScrollEnabled) {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAutoScrollEnabled)
                            Icons.Default.VerticalAlignBottom
                        else
                            Icons.Default.PauseCircle,
                        contentDescription = if (isAutoScrollEnabled)
                            "Auto-scroll enabled - click to disable"
                        else
                            "Auto-scroll disabled - click to enable and scroll to bottom",
                        tint = if (isAutoScrollEnabled)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Auto",
                        style = MaterialTheme.typography.caption,
                        color = if (isAutoScrollEnabled)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Clear logs button
            IconButton(onClick = {
                onClearClick()
                isAutoScrollEnabled = true
            }) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear logs",
                    tint = MaterialTheme.colors.primary
                )
            }

            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Go Back",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            SelectionContainer {
                if(filteredLogEntries.isEmpty()) {
                    Text(
                        text = "No logs yet. Start using the application to see logs here.",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        style = MaterialTheme.typography.body2,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    Text(
                        text = buildStructuredLogText(filteredLogEntries),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        style = MaterialTheme.typography.body2,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }
        }

        // Log statistics bar at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Entries: ${filteredLogEntries.size}/${logEntries.size}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Text(
                "Size: ${formatBytes(logEntries.sumOf { it.message.length }.toLong())}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Auto-scroll: ",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    if (isAutoScrollEnabled) "ON" else "OFF",
                    style = MaterialTheme.typography.caption,
                    color = if (isAutoScrollEnabled) SuccessGreen else ErrorRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@Composable
private fun FloatingStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun buildStructuredLogText(logEntries: List<LogEntry>) = buildAnnotatedString {
    logEntries.forEach { entry ->
        // Timestamp
        withStyle(SpanStyle(color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))) {
            append("[${entry.timestamp}] ")
        }

        // Log type with color coding - use fixed width for alignment
        withStyle(
            SpanStyle(
                color = entry.type.color,
                fontWeight = FontWeight.Bold
            )
        ) {
            val logTypeName = entry.type.displayName.uppercase()
            val paddedLogType = logTypeName.padEnd(12) // Fixed width of 12 characters
            append("$paddedLogType: ")
        }

        // Calculate base indent size - now consistent for all log types
        val baseIndent = "[${entry.timestamp}] ".length + 12 + 2  // timestamp + padded log type + ": "

        // Message with proper indentation for multi-line content
        withStyle(SpanStyle(color = MaterialTheme.colors.onSurface)) {
            append(formatLogMessage(entry.message, baseIndent))
        }

        // Details if available with proper multi-line handling
        entry.details?.let { details ->
            append("\n")
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            ) {
                append(" ".repeat(baseIndent))
                append(formatLogDetails(details, baseIndent))
            }
        }

        append("\n")
    }
}
/**
 * Helper function to create log entries - can be used in your application
 */
fun createLogEntry(
    type: LogType,
    message: String,
    details: String? = null
): LogEntry {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
    return LogEntry(timestamp = timestamp, type = type, message = message, details = details)
}

/**
 * Sample log entries for demonstration
 */
fun getSampleLogEntries(): List<LogEntry> = listOf(
    createLogEntry(LogType.INFO, "ISO8583Studio started successfully"),
    createLogEntry(LogType.CONNECTION, "Server listening on port 8080", "Max connections: 100"),
    createLogEntry(LogType.CONNECTION, "Client connected", "IP: 192.168.1.45, Session: ABC123"),
    createLogEntry(
        LogType.TRANSACTION,
        "Authorization request received",
        "MTI: 0200, Amount: $50.00"
    ),
    createLogEntry(LogType.VERBOSE, "Processing field validation", "Fields: 2, 3, 4, 11, 12, 13"),
    createLogEntry(LogType.DEBUG, "ISO8583 message parsing", "Bitmap: F220000100000000"),
    createLogEntry(
        LogType.TRANSACTION,
        "Authorization approved",
        "Response Code: 00, Auth Code: 123456"
    ),
    createLogEntry(
        LogType.WARNING,
        "Connection timeout detected",
        "Client: 192.168.1.45, Timeout: 30s"
    ),
    createLogEntry(LogType.ERROR, "Invalid message format", "Missing required field 11 (STAN)"),
    createLogEntry(LogType.CONNECTION, "Client disconnected", "Session closed: ABC123"),
    createLogEntry(
        LogType.INFO,
        "Daily transaction summary",
        "Total: 1,247 | Success: 95% | Failed: 5%"
    )
)


/**
 * Function to format bytes into compact readable string
 */
private fun formatBytesCompact(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}K"
        else -> String.format("%.1fM", bytes / (1024.0 * 1024.0))
    }
}

/**
 * Function to format bytes into readable string
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}