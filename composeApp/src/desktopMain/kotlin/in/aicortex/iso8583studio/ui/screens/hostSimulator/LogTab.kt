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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import `in`.aicortex.iso8583studio.logging.LogHistory
import `in`.aicortex.iso8583studio.logging.LogLineRenderer
import `in`.aicortex.iso8583studio.logging.LogRows
import `in`.aicortex.iso8583studio.logging.RenderedLine
import `in`.aicortex.iso8583studio.logging.SpanRole
import `in`.aicortex.iso8583studio.logging.rememberLogRows
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.themedScrollbarStyle
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
    logEntries: SnapshotStateList<LogEntry>, // Live tail; full history comes from [history]
    history: LogHistory? = null,
    liveEntryCap: Int = 0
) {
    var isStatsVisible by remember { mutableStateOf(false) }
    var selectedLogTypes by remember { mutableStateOf(LogType.values().toSet()) }

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
                history = history,
                liveEntryCap = liveEntryCap,
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
    history: LogHistory? = null,
    liveEntryCap: Int = 0,
    selectedLogTypes: Set<LogType> = LogType.values().toSet(),
    onLogTypesChanged: (Set<LogType>) -> Unit = {},
    onBack: (() -> Unit)? = null,
    isStatsVisible: Boolean = false,
    onToggleStats: () -> Unit = {}
) {
    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Rows come from the on-disk index when one exists, so the scrollbar spans the whole retained
    // history rather than just what is still in memory.
    val rows = rememberLogRows(history, logEntries, selectedLogTypes, liveEntryCap)

    // Coarse key: stat-ing the segments on every appended row would be pointless syscall traffic.
    val historyBytes = remember(history, rows.count / 512) {
        history?.index?.value?.segments?.sumOf { it.length() }
            ?: logEntries.sumOf { it.message.length.toLong() }
    }

    // Follow the tail while pinned to the bottom. scrollToItem, not animateScrollTo: rows arrive
    // every flush interval and an animation would restart before it ever finished.
    LaunchedEffect(rows.count, isAutoScrollEnabled) {
        if (isAutoScrollEnabled && rows.count > 0) {
            listState.scrollToItem(rows.count - 1)
        }
    }

    // Leaving the bottom stops the follow; returning to it resumes.
    val isAtBottom by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            rows.count == 0 || last >= rows.count - 2
        }
    }

    // Only a scroll the user actually performed may switch following off. Without the guard this
    // effect fires once on first composition, when nothing has been laid out yet and isAtBottom is
    // still false, and the view stops following the tail the moment it opens.
    var hasScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            hasScrolled = true
        } else if (hasScrolled) {
            isAutoScrollEnabled = isAtBottom
        }
    }

    // Reset auto-scroll when logs are cleared
    LaunchedEffect(rows.count == 0) {
        if (rows.count == 0) {
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
                    modifier = Modifier.width(200.dp).heightIn(max = 300.dp)
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

                    // If enabling auto-scroll, immediately jump to the newest row
                    if (isAutoScrollEnabled && rows.count > 0) {
                        coroutineScope.launch {
                            listState.scrollToItem(rows.count - 1)
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

            // Clear logs button. When the panel renders from disk, clearing only the in-memory
            // list would look like nothing happened — the purge has to reach the files.
            IconButton(onClick = {
                history?.clear()
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
            if (rows.count == 0) {
                Text(
                    text = "No logs yet. Start using the application to see logs here.",
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    style = MaterialTheme.typography.body2,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Truncation is never silent. Only reachable with no log file configured — with
                    // one, trimmed entries are still on disk and still scrolled to.
                    if (rows.truncatedBefore) {
                        Text(
                            text = "Showing the most recent $liveEntryCap entries " +
                                "(no log file configured, so older entries were not retained)",
                            style = MaterialTheme.typography.caption,
                            color = WarningYellow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WarningYellow.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    VirtualizedLogList(
                        rows = rows,
                        listState = listState,
                        modifier = Modifier.fillMaxSize()
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
                "Rows: ${rows.count}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            // Read from the index rather than by walking the entries on every recomposition.
            Text(
                "History: ${formatBytes(historyBytes)}",
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

/**
 * The log surface: one fixed-height row per rendered line, virtualized.
 *
 * Uniform row height is deliberate. It lets `LazyColumn` compute total content height from the row
 * count alone, so the scrollbar thumb is exact and a fling to an arbitrary position resolves in one
 * index lookup instead of a measure pass over everything before it. It also means a row that has not
 * been decoded yet can be drawn as blank space of identical height — the list never reflows around a
 * cache miss.
 *
 * Horizontal scrolling is shared across rows via a single fixed content width rather than a
 * per-row scroll modifier, which would give each row its own conflicting scroll range. Because the
 * panel is monospaced, that width is derived from the longest visible line's character count and is
 * only ever allowed to grow, so it does not twitch while scrolling.
 */
@Composable
private fun VirtualizedLogList(
    rows: LogRows,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val style = MaterialTheme.typography.body2.copy(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
    )
    val density = LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()

    val charWidthPx = remember(style) {
        measurer.measure(AnnotatedString("0".repeat(100)), style).size.width / 100f
    }
    val rowHeight: Dp = remember(style, density) {
        with(density) { (style.fontSize.toPx() * 1.5f).toDp() }
    }

    val visibleMaxChars by remember(rows) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.maxOfOrNull { info ->
                rows.rowAt(info.index)?.let { LogLineRenderer.charLength(it) } ?: 0
            } ?: 0
        }
    }
    var maxChars by remember(rows.count == 0) { mutableStateOf(80) }
    LaunchedEffect(visibleMaxChars) {
        if (visibleMaxChars > maxChars) maxChars = visibleMaxChars
    }

    val hScroll = rememberScrollState()

    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val viewportWidth = maxWidth
        val contentWidth = maxOf(viewportWidth, with(density) { (maxChars * charWidthPx).toDp() })

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .horizontalScroll(hScroll)
        ) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.width(contentWidth).fillMaxHeight()
                ) {
                    items(rows.count) { rowIndex ->
                        LogRowView(
                            line = rows.rowAt(rowIndex),
                            rowHeight = rowHeight,
                            style = style
                        )
                    }
                }
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.align(Alignment.TopEnd),
            style = themedScrollbarStyle()
        )

        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(hScroll),
            modifier = Modifier.align(Alignment.BottomStart),
            style = themedScrollbarStyle()
        )
    }
}

@Composable
private fun LogRowView(
    line: RenderedLine?,
    rowHeight: Dp,
    style: androidx.compose.ui.text.TextStyle
) {
    if (line == null) {
        // Same height as a real row, so a pending decode cannot shift what is on screen.
        Spacer(modifier = Modifier.height(rowHeight).fillMaxWidth())
        return
    }

    val onSurface = MaterialTheme.colors.onSurface
    val text = buildAnnotatedString {
        line.spans.forEach { span ->
            withStyle(
                SpanStyle(
                    color = when (span.role) {
                        SpanRole.TIMESTAMP -> onSurface.copy(alpha = 0.7f)
                        SpanRole.TYPE -> line.type.color
                        SpanRole.MESSAGE -> onSurface
                        SpanRole.DETAILS -> onSurface.copy(alpha = 0.8f)
                    },
                    fontWeight = if (span.role == SpanRole.TYPE) FontWeight.Bold else null,
                    fontStyle = if (span.role == SpanRole.DETAILS) {
                        androidx.compose.ui.text.font.FontStyle.Italic
                    } else {
                        null
                    }
                )
            ) {
                append(span.text)
            }
        }
    }

    Text(
        text = text,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = Modifier.height(rowHeight)
    )
}
