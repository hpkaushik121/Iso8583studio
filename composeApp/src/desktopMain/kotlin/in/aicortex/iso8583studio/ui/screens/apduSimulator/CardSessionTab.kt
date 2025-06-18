package `in`.aicortex.iso8583studio.ui.screens.apduSimulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.CardServiceImpl
import `in`.aicortex.iso8583studio.ui.navigation.CardType
import `in`.aicortex.iso8583studio.ui.navigation.ConnectionInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Card Session Control Tab - Primary interface for EMV card session management
 * Follows the exact two-panel layout pattern from HostSimulator (30% left, 70% right)
 */
@Composable
fun CardSessionTab(
    cardService: CardServiceImpl,
    isStarted: Boolean,
    onStartStopClick: () -> Unit,
    onClearClick: () -> Unit,
    transactionCount: String,
    isCardPresent: Boolean,
    cardAtr: String,
    processingTime: String,
    lastStatusWord: String,
    command: String,
    rawCommand: String,
    response: String,
    rawResponse: String,
    modifier: Modifier = Modifier
) {
    var showQuickConfig by remember { mutableStateOf(false) }
    var errorInjectionEnabled by remember { mutableStateOf(false) }
    var responseMode by remember { mutableStateOf("Automatic") }
    var sessionDuration by remember { mutableStateOf("00:00:00") }
    var commandsPerSecond by remember { mutableStateOf("0") }
    var errorRate by remember { mutableStateOf("0.0%") }
    var avgResponseTime by remember { mutableStateOf("0ms") }
    var connectionStrength by remember { mutableStateOf(100) }

    // Session timing
    LaunchedEffect(isStarted) {
        if (isStarted) {
            var seconds = 0
            while (isStarted) {
                delay(1000)
                seconds++
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60
                sessionDuration = String.format("%02d:%02d:%02d", hours, minutes, secs)
            }
        } else {
            sessionDuration = "00:00:00"
        }
    }

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // LEFT PANEL - Control Section (30%)
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight(),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Information Card
                ProfileInformationCard(
                    cardService = cardService,
                    isCardPresent = isCardPresent,
                    cardAtr = cardAtr
                )

                // Session Control Buttons
                SessionControlButtons(
                    isStarted = isStarted,
                    onStartStopClick = onStartStopClick,
                    onClearClick = onClearClick,
                    isCardPresent = isCardPresent
                )

                // Connection Management
                ConnectionManagementCard(
                    cardService = cardService,
                    connectionStrength = connectionStrength
                )

                // Quick Configuration
                QuickConfigurationCard(
                    showQuickConfig = showQuickConfig,
                    onToggleQuickConfig = { showQuickConfig = !showQuickConfig },
                    errorInjectionEnabled = errorInjectionEnabled,
                    onErrorInjectionToggle = { errorInjectionEnabled = !errorInjectionEnabled },
                    responseMode = responseMode,
                    onResponseModeChange = { responseMode = it }
                )

                // Real-time Statistics Summary
                StatisticsSummaryCard(
                    transactionCount = transactionCount,
                    errorRate = errorRate,
                    sessionDuration = sessionDuration,
                    avgResponseTime = avgResponseTime
                )
            }
        }

        // RIGHT PANEL - Monitoring Section (70%)
        Card(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Transaction Display
                LiveTransactionDisplay(
                    command = command,
                    rawCommand = rawCommand,
                    response = response,
                    rawResponse = rawResponse,
                    lastStatusWord = lastStatusWord,
                    processingTime = processingTime
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Connection Status Panel
                    ConnectionStatusPanel(
                        modifier = Modifier.weight(1f),
                        cardService = cardService,
                        isCardPresent = isCardPresent,
                        connectionStrength = connectionStrength
                    )

                    // Performance Metrics
                    PerformanceMetricsPanel(
                        modifier = Modifier.weight(1f),
                        commandsPerSecond = commandsPerSecond,
                        avgResponseTime = avgResponseTime,
                        errorRate = errorRate
                    )
                }

                // Active Session Info
                ActiveSessionInfoPanel(
                    cardService = cardService,
                    isStarted = isStarted,
                    sessionDuration = sessionDuration,
                    lastStatusWord = lastStatusWord
                )
            }
        }
    }
}

/**
 * Profile Information Card - Display loaded card profile details
 */
@Composable
private fun ProfileInformationCard(
    cardService: CardServiceImpl,
    isCardPresent: Boolean,
    cardAtr: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Card Profile",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ProfileInfoRow("Name", cardService.configuration.name)
            ProfileInfoRow("Type", getCardTypeDisplayName(cardService.configuration.cardType))
            ProfileInfoRow("Interface", getInterfaceDisplayName(cardService.configuration.connectionInterface))
            ProfileInfoRow("AID", cardService.configuration.applicationAid.takeIf { it.isNotEmpty() } ?: "N/A")

            if (isCardPresent && cardAtr.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                ProfileInfoRow("ATR", cardAtr, isMonospace = true)

                // ATR Analysis
                val atrAnalysis = analyzeAtr(cardAtr)
                if (atrAnalysis.isNotEmpty()) {
                    Text(
                        text = atrAnalysis,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Session Control Buttons - Large prominent control buttons
 */
@Composable
private fun SessionControlButtons(
    isStarted: Boolean,
    onStartStopClick: () -> Unit,
    onClearClick: () -> Unit,
    isCardPresent: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Session Control",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            // Primary Start/Stop Button
            Button(
                onClick = onStartStopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isStarted) Color(0xFFE57373) else MaterialTheme.colors.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(
                    imageVector = if (isStarted) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isStarted) "Stop Session" else "Start Session",
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pause Button
                OutlinedButton(
                    onClick = { /* Handle pause */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = isStarted,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pause")
                }

                // Reset Button
                OutlinedButton(
                    onClick = { /* Handle reset */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            // Clear Button
            OutlinedButton(
                onClick = onClearClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Display")
            }
        }
    }
}

/**
 * Connection Management Card
 */
@Composable
private fun ConnectionManagementCard(
    cardService: CardServiceImpl,
    connectionStrength: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Connection",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ProfileInfoRow("Interface", getInterfaceDisplayName(cardService.configuration.connectionInterface))
            ProfileInfoRow("Reader", cardService.configuration.readerName.takeIf { it.isNotEmpty() } ?: "Default")
            ProfileInfoRow("Protocol", getProtocolInfo(cardService.configuration.connectionInterface))

            // Connection Strength Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Signal:",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.width(60.dp)
                )

                LinearProgressIndicator(
                    progress = connectionStrength / 100f,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = when {
                        connectionStrength > 70 -> Color(0xFF4CAF50)
                        connectionStrength > 30 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$connectionStrength%",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Quick Configuration Card with collapsible sections
 */
@Composable
private fun QuickConfigurationCard(
    showQuickConfig: Boolean,
    onToggleQuickConfig: () -> Unit,
    errorInjectionEnabled: Boolean,
    onErrorInjectionToggle: (Boolean) -> Unit,
    responseMode: String,
    onResponseModeChange: (String) -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (showQuickConfig) 180f else 0f,
        animationSpec = tween(300)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleQuickConfig() }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quick Configuration",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle)
                )
            }

            // Collapsible content
            AnimatedVisibility(
                visible = showQuickConfig,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Error Injection Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Error Injection",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = errorInjectionEnabled,
                            onCheckedChange = onErrorInjectionToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.primary
                            )
                        )
                    }

                    // Response Mode Selection
                    Column {
                        Text(
                            text = "Response Mode",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val responseModes = listOf("Automatic", "Manual", "Script-based")
                        responseModes.forEach { mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onResponseModeChange(mode) }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = responseMode == mode,
                                    onClick = { onResponseModeChange(mode) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colors.primary
                                    )
                                )
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Statistics Summary Card
 */
@Composable
private fun StatisticsSummaryCard(
    transactionCount: String,
    errorRate: String,
    sessionDuration: String,
    avgResponseTime: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            StatisticItem("Commands", transactionCount, Icons.Default.SwapHoriz)
            StatisticItem("Error Rate", errorRate, Icons.Default.Error)
            StatisticItem("Duration", sessionDuration, Icons.Default.Timer)
            StatisticItem("Avg Response", avgResponseTime, Icons.Default.Speed)
        }
    }
}

/**
 * Live Transaction Display - Show current request/response
 */
@Composable
private fun LiveTransactionDisplay(
    command: String,
    rawCommand: String,
    response: String,
    rawResponse: String,
    lastStatusWord: String,
    processingTime: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Live Transaction",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                if (processingTime.isNotEmpty() && processingTime != "0ms") {
                    Chip(
                        text = processingTime,
                        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colors.primary
                    )
                }

                if (lastStatusWord.isNotEmpty()) {
                    val isSuccess = lastStatusWord == "9000"
                    Chip(
                        text = lastStatusWord,
                        backgroundColor = if (isSuccess) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f),
                        contentColor = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Command Section
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    TransactionSection(
                        title = "Command",
                        content = command,
                        rawContent = rawCommand,
                        icon = Icons.Default.ArrowForward,
                        color = Color(0xFF2196F3)
                    )
                }

                // Response Section
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    TransactionSection(
                        title = "Response",
                        content = response,
                        rawContent = rawResponse,
                        icon = Icons.Default.ArrowBack,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

/**
 * Connection Status Panel
 */
@Composable
private fun ConnectionStatusPanel(
    modifier: Modifier,
    cardService: CardServiceImpl,
    isCardPresent: Boolean,
    connectionStrength: Int
) {
    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Connection Status",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            StatusIndicator(
                label = "Reader",
                status = if (connectionStrength > 0) "Connected" else "Disconnected",
                isActive = connectionStrength > 0
            )

            StatusIndicator(
                label = "Card",
                status = if (isCardPresent) "Present" else "Absent",
                isActive = isCardPresent
            )

            StatusIndicator(
                label = "Protocol",
                status = getProtocolInfo(cardService.configuration.connectionInterface),
                isActive = true
            )
        }
    }
}

/**
 * Performance Metrics Panel
 */
@Composable
private fun PerformanceMetricsPanel(
    modifier: Modifier,
    commandsPerSecond: String,
    avgResponseTime: String,
    errorRate: String
) {
    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Performance",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            MetricItem("Throughput", "$commandsPerSecond cmd/s")
            MetricItem("Response Time", avgResponseTime)
            MetricItem("Error Rate", errorRate)
        }
    }
}

/**
 * Active Session Info Panel
 */
@Composable
private fun ActiveSessionInfoPanel(
    cardService: CardServiceImpl,
    isStarted: Boolean,
    sessionDuration: String,
    lastStatusWord: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Active Session Info",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SessionInfoItem("Status", if (isStarted) "Active" else "Inactive")
                    SessionInfoItem("Duration", sessionDuration)
                }

                Column(modifier = Modifier.weight(1f)) {
                    SessionInfoItem("Last SW", lastStatusWord.takeIf { it.isNotEmpty() } ?: "N/A")
                    SessionInfoItem("Card State", if (isStarted) "Ready" else "Idle")
                }
            }
        }
    }
}

// Helper Composables
@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2.copy(
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            ),
            maxLines = if (isMonospace) 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TransactionSection(
    title: String,
    content: String,
    rawContent: String,
    icon: ImageVector,
    color: Color
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Parsed content
        if (content.isNotEmpty()) {
            Card(
                backgroundColor = color.copy(alpha = 0.05f),
                elevation = 0.dp
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(8.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Raw content
        if (rawContent.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Raw: $rawContent",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Empty state
        if (content.isEmpty() && rawContent.isEmpty()) {
            Card(
                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                elevation = 0.dp
            ) {
                Text(
                    text = "Waiting for ${title.lowercase()}...",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    label: String,
    status: String,
    isActive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = status,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SessionInfoItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun Chip(
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Card(
        backgroundColor = backgroundColor,
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// Utility Functions
private fun getCardTypeDisplayName(cardType: CardType): String {
    return when (cardType) {
        CardType.EMV_CONTACT -> "EMV Contact"
        CardType.EMV_CONTACTLESS -> "EMV Contactless"
        CardType.MIFARE_CLASSIC -> "MIFARE Classic"
        CardType.MIFARE_DESFIRE -> "MIFARE DESFire"
        CardType.JAVA_CARD -> "Java Card"
        CardType.CUSTOM -> "Custom"
    }
}

private fun getInterfaceDisplayName(connectionInterface: ConnectionInterface): String {
    return when (connectionInterface) {
        ConnectionInterface.PC_SC -> "PC/SC"
        ConnectionInterface.NFC -> "NFC"
        ConnectionInterface.MOCK -> "Mock/Simulation"
        ConnectionInterface.USB -> "USB"
    }
}

private fun getProtocolInfo(connectionInterface: ConnectionInterface): String {
    return when (connectionInterface) {
        ConnectionInterface.PC_SC -> "T=0/T=1"
        ConnectionInterface.NFC -> "ISO 14443 Type A/B"
        ConnectionInterface.MOCK -> "Simulated"
        ConnectionInterface.USB -> "CCID"
    }
}

private fun analyzeAtr(atr: String): String {
    if (atr.length < 6) return ""

    return try {
        val ts = atr.substring(0, 2)
        val t0 = atr.substring(2, 4)

        val analysis = mutableListOf<String>()

        when (ts) {
            "3B" -> analysis.add("Direct convention")
            "3F" -> analysis.add("Inverse convention")
            else -> analysis.add("Unknown convention")
        }

        val t0Int = t0.toInt(16)
        val historyLength = t0Int and 0x0F
        if (historyLength > 0) {
            analysis.add("$historyLength history bytes")
        }

        if ((t0Int and 0x10) != 0) analysis.add("TA1 present")
        if ((t0Int and 0x20) != 0) analysis.add("TB1 present")
        if ((t0Int and 0x40) != 0) analysis.add("TC1 present")
        if ((t0Int and 0x80) != 0) analysis.add("TD1 present")

        analysis.joinToString(", ")
    } catch (e: Exception) {
        "Invalid ATR format"
    }
}

/**
 * Advanced Session Control Features
 */
@Composable
private fun AdvancedSessionControls(
    cardService: CardServiceImpl,
    modifier: Modifier = Modifier
) {
    var showAdvanced by remember { mutableStateOf(false) }
    var autoReconnect by remember { mutableStateOf(true) }
    var logLevel by remember { mutableStateOf("Info") }
    var simulationSpeed by remember { mutableStateOf(1.0f) }

    Card(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced }
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Advanced Controls",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = showAdvanced,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Auto-reconnect toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Auto-reconnect",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = autoReconnect,
                            onCheckedChange = { autoReconnect = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.primary
                            )
                        )
                    }

                    // Log level selection
                    Column {
                        Text(
                            text = "Log Level",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val logLevels = listOf("Error", "Warning", "Info", "Debug", "Trace")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            logLevels.forEach { level ->
                                FilterChip(
                                    selected = logLevel == level,
                                    onClick = { logLevel = level },
                                    text = level
                                )
                            }
                        }
                    }

                    // Simulation speed (for mock interface)
                    if (cardService.configuration.connectionInterface == ConnectionInterface.MOCK) {
                        Column {
                            Text(
                                text = "Simulation Speed: ${String.format("%.1fx", simulationSpeed)}",
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = simulationSpeed,
                                onValueChange = { simulationSpeed = it },
                                valueRange = 0.1f..5.0f,
                                steps = 49,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colors.primary,
                                    activeTrackColor = MaterialTheme.colors.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String
) {
    Card(
        backgroundColor = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        elevation = if (selected) 2.dp else 0.dp,
        border = if (!selected) BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)) else null,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = if (selected) Color.White else MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Real-time Activity Monitor
 */
@Composable
private fun RealTimeActivityMonitor(
    cardService: CardServiceImpl,
    modifier: Modifier = Modifier
) {
    var activityLog by remember { mutableStateOf(listOf<ActivityEvent>()) }

    // Simulate activity events
    LaunchedEffect(cardService) {
        while (true) {
            delay(2000)
            activityLog = activityLog.takeLast(4) + ActivityEvent(
                timestamp = System.currentTimeMillis(),
                type = ActivityType.APDU_EXCHANGE,
                description = "SELECT Application",
                success = true
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Activity Monitor",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activityLog.isEmpty()) {
                Text(
                    text = "No activity recorded",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                activityLog.takeLast(5).forEach { event ->
                    ActivityEventItem(event = event)
                    if (event != activityLog.last()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityEventItem(event: ActivityEvent) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = when (event.type) {
                ActivityType.CARD_INSERTION -> Icons.Default.CreditCard
                ActivityType.CARD_REMOVAL -> Icons.Default.RemoveCircle
                ActivityType.APDU_EXCHANGE -> Icons.Default.SwapHoriz
                ActivityType.ERROR -> Icons.Default.Error
                ActivityType.STATUS_CHANGE -> Icons.Default.Info
            },
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (event.success) Color(0xFF4CAF50) else Color(0xFFF44336)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.description,
                style = MaterialTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatTimestamp(event.timestamp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        )
    }
}

// Data classes for activity monitoring
data class ActivityEvent(
    val timestamp: Long,
    val type: ActivityType,
    val description: String,
    val success: Boolean
)

enum class ActivityType {
    CARD_INSERTION,
    CARD_REMOVAL,
    APDU_EXCHANGE,
    ERROR,
    STATUS_CHANGE
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 1000 -> "now"
        diff < 60000 -> "${diff / 1000}s ago"
        diff < 3600000 -> "${diff / 60000}m ago"
        else -> "${diff / 3600000}h ago"
    }
}