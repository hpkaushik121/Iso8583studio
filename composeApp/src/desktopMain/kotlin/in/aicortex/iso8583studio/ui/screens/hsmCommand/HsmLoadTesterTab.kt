package `in`.aicortex.iso8583studio.ui.screens.hsmCommand

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.ConnectionState
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.HsmCommandClientService
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.LoadTestStats
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import kotlinx.coroutines.launch

@Composable
fun HsmLoadTesterTab(service: HsmCommandClientService) {
    val scope = rememberCoroutineScope()
    val connectionState by service.connectionState.collectAsState()
    val stats by service.loadTestStats.collectAsState()

    var commandToSend by remember { mutableStateOf("4E43") }
    var concurrency by remember { mutableStateOf(service.config.loadTestConcurrentConnections.toString()) }
    var tps by remember { mutableStateOf(service.config.loadTestCommandsPerSecond.toString()) }
    var duration by remember { mutableStateOf(service.config.loadTestDurationSeconds.toString()) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- Left: Config + Controls ---
            Card(
                modifier = Modifier.weight(1f),
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = MaterialTheme.colors.surface,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = PrimaryBlue,
                        )
                        Text(
                            "Configuration",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    FixedOutlinedTextField(
                        value = commandToSend,
                        onValueChange = { commandToSend = it },
                        label = { Text("Command (Hex)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !stats.running,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FixedOutlinedTextField(
                            value = concurrency,
                            onValueChange = { concurrency = it },
                            label = { Text("Workers") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !stats.running,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        FixedOutlinedTextField(
                            value = tps,
                            onValueChange = { tps = it },
                            label = { Text("Cmd/sec") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !stats.running,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        FixedOutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            label = { Text("Duration (s)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !stats.running,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { service.startLoadTest(commandToSend, scope) },
                            enabled = connectionState == ConnectionState.CONNECTED && !stats.running && commandToSend.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Start Test", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { service.stopLoadTest() },
                            enabled = stats.running,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336)),
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                        ) {
                            Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Stop", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    if (!stats.running && connectionState != ConnectionState.CONNECTED) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFF9800).copy(alpha = 0.08f),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Default.Warning, null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF9800))
                                Text(
                                    "Connect to the HSM before starting a load test",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF9800),
                                )
                            }
                        }
                    }
                }
            }

            // --- Right: Progress Ring ---
            Card(
                modifier = Modifier.weight(1f),
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = MaterialTheme.colors.surface,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val durationSec = duration.toIntOrNull() ?: 60
                    val progress = if (stats.running && durationSec > 0)
                        (stats.elapsedSeconds.toFloat() / durationSec).coerceIn(0f, 1f) else 0f

                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(500, easing = FastOutSlowInEasing),
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ProgressRing(
                                progress = animatedProgress,
                                running = stats.running,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (stats.running) {
                                    Text(
                                        "%.1f".format(stats.currentTps),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = PrimaryBlue,
                                    )
                                    Text(
                                        "TPS",
                                        style = MaterialTheme.typography.overline,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    )
                                } else if (stats.totalSent > 0) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        modifier = Modifier.size(28.dp),
                                        tint = Color(0xFF4CAF50),
                                    )
                                    Text(
                                        "Done",
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50),
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Speed,
                                        null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                                    )
                                    Text(
                                        "Ready",
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                    )
                                }
                            }
                        }

                        if (stats.running) {
                            Text(
                                "${stats.elapsedSeconds}s / ${durationSec}s",
                                style = MaterialTheme.typography.caption,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }

        // --- Metrics Dashboard ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.BarChart, null, modifier = Modifier.size(18.dp), tint = PrimaryBlue)
            Text("Metrics", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            if (stats.running) {
                Spacer(Modifier.width(4.dp))
                PulsingDot(color = Color(0xFF4CAF50))
                Text("Live", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                label = "Total Sent",
                value = stats.totalSent.toString(),
                icon = Icons.Default.CallMade,
                accentColor = Color(0xFF2196F3),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "Received",
                value = stats.totalReceived.toString(),
                icon = Icons.Default.CallReceived,
                accentColor = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "Success",
                value = stats.successCount.toString(),
                icon = Icons.Default.CheckCircle,
                accentColor = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "Failures",
                value = stats.failureCount.toString(),
                icon = Icons.Default.Cancel,
                accentColor = Color(0xFFF44336),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                label = "Avg Latency",
                value = "%.1f ms".format(stats.avgResponseTimeMs),
                icon = Icons.Default.Timer,
                accentColor = Color(0xFFFF9800),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "Min Latency",
                value = if (stats.minResponseTimeMs == Long.MAX_VALUE) "-- ms" else "${stats.minResponseTimeMs} ms",
                icon = Icons.Default.ArrowDownward,
                accentColor = Color(0xFF009688),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "Max Latency",
                value = if (stats.maxResponseTimeMs == 0L) "-- ms" else "${stats.maxResponseTimeMs} ms",
                icon = Icons.Default.ArrowUpward,
                accentColor = Color(0xFFE91E63),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "Throughput",
                value = "%.1f tps".format(stats.currentTps),
                icon = Icons.Default.Speed,
                accentColor = Color(0xFF673AB7),
                modifier = Modifier.weight(1f),
            )
        }

        // --- Success Rate Bar ---
        if (stats.totalSent > 0) {
            SuccessRateBar(stats)
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    running: Boolean,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.06f)
    val progressColor = PrimaryBlue
    val sweepAngle = progress * 360f

    Canvas(modifier = modifier) {
        val strokeWidth = 10.dp.toPx()
        val padding = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(padding, padding)

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        if (sweepAngle > 0f) {
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Surface(
        modifier = Modifier.size(8.dp),
        shape = CircleShape,
        color = color.copy(alpha = alpha),
    ) {}
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = 1.dp,
        shape = RoundedCornerShape(10.dp),
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = accentColor,
            ) {}
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, null, tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                Text(
                    value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    label,
                    style = MaterialTheme.typography.overline,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SuccessRateBar(stats: LoadTestStats) {
    val total = stats.successCount + stats.failureCount
    val successRate = if (total > 0) stats.successCount.toFloat() / total else 0f
    val failRate = if (total > 0) stats.failureCount.toFloat() / total else 0f

    Card(
        elevation = 1.dp,
        shape = RoundedCornerShape(10.dp),
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Success Rate",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "%.1f%%".format(successRate * 100),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    color = if (successRate >= 0.95f) Color(0xFF4CAF50) else if (successRate >= 0.8f) Color(0xFFFF9800) else Color(0xFFF44336),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (successRate > 0f) {
                    Surface(
                        modifier = Modifier.weight(successRate).fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = if (failRate == 0f) 4.dp else 0.dp, bottomEnd = if (failRate == 0f) 4.dp else 0.dp),
                        color = Color(0xFF4CAF50),
                    ) {}
                }
                if (failRate > 0f) {
                    Surface(
                        modifier = Modifier.weight(failRate).fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = if (successRate == 0f) 4.dp else 0.dp, bottomStart = if (successRate == 0f) 4.dp else 0.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                        color = Color(0xFFF44336),
                    ) {}
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = Color(0xFF4CAF50)) {}
                    Text("${stats.successCount} passed", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = Color(0xFFF44336)) {}
                    Text("${stats.failureCount} failed", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}
