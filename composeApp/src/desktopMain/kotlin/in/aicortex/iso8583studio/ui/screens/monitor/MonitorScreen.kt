package `in`.aicortex.iso8583studio.ui.screens.monitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack

/**
 * Modern Monitor screen for viewing gateway activity
 */
@Composable
fun MonitorScreen(
    config: GatewayConfig?,
    onBack: () -> Unit
) {
    // Mock data for demonstration purposes
    val mockLogs = remember {
        mutableStateListOf(
            LogEntry("2024-05-21 10:15:23", "Connection established", LogLevel.INFO),
            LogEntry("2024-05-21 10:15:24", "Client connected: 192.168.1.45", LogLevel.INFO),
            LogEntry("2024-05-21 10:15:28", "Received transaction request: 0200", LogLevel.TRANSACTION),
            LogEntry("2024-05-21 10:15:28", "Processing transaction...", LogLevel.INFO),
            LogEntry("2024-05-21 10:15:29", "Transaction approved: 000000", LogLevel.SUCCESS),
            LogEntry("2024-05-21 10:15:35", "Received invalid message format", LogLevel.WARNING),
            LogEntry("2024-05-21 10:15:40", "Connection timeout with client 192.168.1.50", LogLevel.ERROR)
        )
    }

    // Stats for demonstration
    var connectionCount by remember { mutableStateOf(3) }
    var activeConnections by remember { mutableStateOf(1) }
    var transactionCount by remember { mutableStateOf(14) }
    var bytesReceived by remember { mutableStateOf(28456L) }
    var bytesSent by remember { mutableStateOf(14224L) }

    // Add periodic updates for demonstration
//    LaunchedEffect(Unit) {
//        while (true) {
//            delay(3000)
//            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
//            val random = Random()
//
//            when (random.nextInt(4)) {
//                0 -> {
//                    mockLogs.add(0, LogEntry(timestamp, "Received heartbeat signal", LogLevel.INFO))
//                }
//                1 -> {
//                    mockLogs.add(0, LogEntry(timestamp, "Received transaction request: 0200", LogLevel.TRANSACTION))
//                    mockLogs.add(0, LogEntry(timestamp, "Transaction approved: 000000", LogLevel.SUCCESS))
//                    transactionCount++
//                }
//                2 -> {
//                    if (random.nextBoolean()) {
//                        mockLogs.add(0, LogEntry(timestamp, "New client connected: 192.168.1.${random.nextInt(100)}", LogLevel.INFO))
//                        activeConnections++
//                        connectionCount++
//                    } else {
//                        mockLogs.add(0, LogEntry(timestamp, "Client disconnected", LogLevel.INFO))
//                        if (activeConnections > 0) activeConnections--
//                    }
//                }
//                3 -> {
//                    if (random.nextBoolean()) {
//                        mockLogs.add(0, LogEntry(timestamp, "Connection timeout with client", LogLevel.WARNING))
//                    } else {
//                        mockLogs.add(0, LogEntry(timestamp, "Failed to parse message: Invalid format", LogLevel.ERROR))
//                    }
//                }
//            }
//
//            // Update traffic stats
//            bytesReceived += random.nextInt(1000)
//            bytesSent += random.nextInt(500)
//
//            // Keep log size manageable
//            if (mockLogs.size > 100) {
//                mockLogs.removeAt(mockLogs.size - 1)
//            }
//        }
//    }

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "Monitor - ${config?.name ?: "Unknown"}",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { /* Clear logs */ }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear logs")
                    }
                    IconButton(onClick = { /* Export logs */ }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export logs")
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Dashboard stats cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connections stats
                StatCard(
                    title = "Connections",
                    mainValue = activeConnections.toString(),
                    secondaryValue = "Total: $connectionCount",
                    icon = Icons.Default.DeviceHub,
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )

                // Transactions stats
                StatCard(
                    title = "Transactions",
                    mainValue = transactionCount.toString(),
                    secondaryValue = "Success: ${(transactionCount * 0.93).toInt()}",
                    icon = Icons.Default.CompareArrows,
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )

                // Traffic stats
                StatCard(
                    title = "Traffic",
                    mainValue = "${formatBytes(bytesReceived + bytesSent)}",
                    secondaryValue = "↓ ${formatBytes(bytesReceived)} ↑ ${formatBytes(bytesSent)}",
                    icon = Icons.Default.DataUsage,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Log filtering and controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Logs",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                // Log level filter
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filter: ", style = MaterialTheme.typography.body2)

                    var selectedFilter by remember { mutableStateOf("All") }
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = Color.Transparent,
                                contentColor = MaterialTheme.colors.onSurface
                            )
                        ) {
                            Text(selectedFilter)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select filter"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf("All", "Info", "Transaction", "Warning", "Error").forEach { level ->
                                DropdownMenuItem(onClick = {
                                    selectedFilter = level
                                    expanded = false
                                }) {
                                    Text(level)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Auto-scroll toggle
                var autoScroll by remember { mutableStateOf(true) }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-scroll")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoScroll,
                        onCheckedChange = { autoScroll = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colors.primary
                        )
                    )
                }
            }

            // Log display area
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    reverseLayout = true
                ) {
                    items(mockLogs) { log ->
                        LogEntryRow(log)
                        Divider(
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    mainValue: String,
    secondaryValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = mainValue,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = secondaryValue,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

enum class LogLevel {
    INFO, TRANSACTION, SUCCESS, WARNING, ERROR
}

data class LogEntry(
    val timestamp: String,
    val message: String,
    val level: LogLevel
)

@Composable
private fun LogEntryRow(log: LogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timestamp
        Text(
            text = log.timestamp,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(140.dp)
        )

        // Level indicator
        val (color, icon) = when (log.level) {
            LogLevel.INFO -> Pair(Color.Gray, Icons.Default.Info)
            LogLevel.TRANSACTION -> Pair(PrimaryBlue, Icons.Default.CompareArrows)
            LogLevel.SUCCESS -> Pair(SuccessGreen, Icons.Default.CheckCircle)
            LogLevel.WARNING -> Pair(WarningYellow, Icons.Default.Warning)
            LogLevel.ERROR -> Pair(ErrorRed, Icons.Default.Error)
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(20.dp)
                .padding(end = 8.dp)
        )

        // Message
        Text(
            text = log.message,
            style = MaterialTheme.typography.body2,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}