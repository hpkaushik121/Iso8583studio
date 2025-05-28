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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.VerticalAlignBottom
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
import `in`.aicortex.iso8583studio.ui.screens.components.SectionHeader
import kotlinx.coroutines.launch

/**
 * Enhanced Log Tab with Auto-Scroll functionality
 */
@Composable
fun LogTab(
    logText: String,
    onClearClick: () -> Unit,
    connectionCount: Int,
    concurrentConnections: Int,
    bytesIncoming: Long,
    bytesOutgoing: Long
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Log viewer
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            elevation = 2.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            LogPanelWithAutoScroll(
                onClearClick = onClearClick,
                logText = logText
            )
        }

        // Statistics panel (unchanged)
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader(title = "Statistics")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatisticItem(
                        icon = Icons.Default.Router,
                        title = "Connections",
                        value = connectionCount.toString(),
                        color = MaterialTheme.colors.primary
                    )

                    StatisticItem(
                        icon = Icons.Default.NetworkPing,
                        title = "Concurrent Connections",
                        value = concurrentConnections.toString(),
                        color = Color(0xFF00BCD4)
                    )

                    StatisticItem(
                        icon = Icons.Default.ArrowDownward,
                        title = "Bytes Received",
                        value = formatBytes(bytesIncoming),
                        color = SuccessGreen
                    )

                    StatisticItem(
                        icon = Icons.Default.ArrowUpward,
                        title = "Bytes Sent",
                        value = formatBytes(bytesOutgoing),
                        color = MaterialTheme.colors.secondary
                    )

                    StatisticItem(
                        icon = Icons.Default.NetworkCheck,
                        title = "Active Sessions",
                        value = "1", // This would be dynamic in a real implementation
                        color = WarningYellow
                    )
                }
            }
        }
    }
}

@Composable
internal fun LogPanelWithAutoScroll(
    onClearClick: () -> Unit,
    logText: String,
    onBack: (() -> Unit)? = null
) {
    var userHasScrolled by remember { mutableStateOf(false) }
    var previousLogLength by remember { mutableStateOf(0) }
    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope() // Add this for better lifecycle management
    val scrollState = rememberScrollState()

    // Auto-scroll effect when new log content is added
    LaunchedEffect(logText) {
        if (logText.length > previousLogLength && isAutoScrollEnabled) {
            // Use a slight delay to ensure the text is rendered before scrolling
            kotlinx.coroutines.delay(50)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        previousLogLength = logText.length
    }

    // Monitor scroll position to detect manual scrolling
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            val isAtBottom =
                scrollState.value >= scrollState.maxValue - 100 // 100px tolerance for better detection

            // If user manually scrolled up, disable auto-scroll
            if (!isAtBottom && !userHasScrolled && scrollState.value > 0) {
                userHasScrolled = true
                isAutoScrollEnabled = false
            }

            // If user scrolled back to bottom, enable auto-scroll
            if (isAtBottom && userHasScrolled) {
                userHasScrolled = false
                isAutoScrollEnabled = true
            }
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
                "Transaction Log",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.primary
            )

            Spacer(modifier = Modifier.weight(1f))

            // Auto-scroll toggle button
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isAutoScrollEnabled)
                    MaterialTheme.colors.primary.copy(alpha = 0.1f)
                else
                    MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                modifier = Modifier.clickable {
                    isAutoScrollEnabled = !isAutoScrollEnabled
                    if (isAutoScrollEnabled) {
                        // Scroll to bottom when re-enabled
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
                            "Auto-scroll enabled"
                        else
                            "Auto-scroll disabled",
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
            IconButton(onClick = onClearClick) {
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
            // Log content with custom scrollable text
            SelectionContainer {
                Text(
                    text = logText.ifEmpty { "No logs yet. Start the server to see transaction logs here." },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    style = MaterialTheme.typography.body2,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (logText.isEmpty())
                        MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colors.onSurface
                )
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
                "Lines: ${logText.count { it == '\n' }}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Text(
                "Size: ${formatBytes(logText.length.toLong())}",
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
private fun StatisticItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(32.dp)
                .padding(bottom = 8.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )
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