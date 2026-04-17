package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.data.model.*
import `in`.aicortex.iso8583studio.domain.service.hostSimulatorService.HostSimulator
import `in`.aicortex.iso8583studio.domain.utils.LoadTestHtmlExporter
import ai.cortex.core.IsoUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

// ─────────────────────────────────────────────────────────────────────────────
// Palette helpers
// ─────────────────────────────────────────────────────────────────────────────

private val Green400  = Color(0xFF4ADE80)
private val Red400    = Color(0xFFF87171)
private val Blue400   = Color(0xFF60A5FA)
private val Yellow400 = Color(0xFFFBBF24)
private val Purple400 = Color(0xFFC084FC)

// ─────────────────────────────────────────────────────────────────────────────
// Main composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun LoadTestTab(
    gw: HostSimulator,
    /** Shared with SendMessageTab so the composed message is available as default. */
    rawMessageStringState: MutableState<String>
) {
    val coroutineScope = rememberCoroutineScope()
    val isAsyncMode    = gw.configuration.transmissionType == TransmissionType.ASYNCHRONOUS
    val gatewayLabel   = "${gw.configuration.destinationServer}:${gw.configuration.destinationPort}"

    // ── Configuration state (local) ────────────────────────────────────────
    var config  by remember { mutableStateOf(LoadTestConfig(connectionPoolSize = if (isAsyncMode) 5 else 1)) }
    var testHex by remember(rawMessageStringState.value) { mutableStateOf(rawMessageStringState.value) }

    // ── Live status polled from HostSimulator ──────────────────────────────
    var progress  by remember { mutableStateOf(gw.loadTestProgress) }
    var results   by remember { mutableStateOf(gw.loadTestResults) }
    var isRunning by remember { mutableStateOf(gw.loadTestRunning) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(150)
            isRunning = gw.loadTestRunning
            progress  = gw.loadTestProgress
            results   = gw.loadTestResults
        }
    }

    // ── Derived stats ──────────────────────────────────────────────────────
    val stats by remember(results) {
        derivedStateOf {
            if (results.isEmpty()) LoadTestStats.EMPTY
            else LoadTestStats.compute(results, progress.elapsedMs.coerceAtLeast(1L))
        }
    }

    // ── Layout ─────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // (1) Header
        LoadTestHeader(isAsyncMode = isAsyncMode, gatewayLabel = gatewayLabel, isRunning = isRunning)

        // (2) Config
        SectionCard(title = "Test Configuration", icon = Icons.Default.Settings) {
            ConfigPanel(
                config = config,
                onConfigChange = { config = it },
                isAsyncMode = isAsyncMode,
                testHex = testHex,
                onTestHexChange = { testHex = it }
            )
        }

        // (3) Run / Stop
        SectionCard(title = "Run", icon = Icons.Default.PlayArrow) {
            RunControls(
                isRunning = isRunning,
                progress = progress,
                config = config,
                testHex = testHex,
                onRun = {
                    val norm = testHex.replace("\\s".toRegex(), "")
                    if (norm.isBlank() || norm.length % 2 != 0) return@RunControls
                    val bytes = IsoUtil.stringToBcd(norm, norm.length / 2)
                    gw.startLoadTest(bytes, config)
                    isRunning = true
                },
                onStop = { gw.stopLoadTest() }
            )
        }

        // (4) Results summary
        if (results.isNotEmpty() || isRunning) {
            SectionCard(title = "Results Summary", icon = Icons.Default.Assessment) {
                ResultsSummary(stats = stats, progress = progress, isRunning = isRunning)
            }
        }

        // (5) Export  — only visible once a run has completed
        if (results.isNotEmpty() && !isRunning) {
            SectionCard(title = "Export", icon = Icons.Default.Download) {
                ExportPanel(
                    gw = gw,
                    config = config,
                    stats = stats,
                    results = results,
                    isAsyncMode = isAsyncMode,
                    gatewayLabel = gatewayLabel,
                    coroutineScope = coroutineScope
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadTestHeader(isAsyncMode: Boolean, gatewayLabel: String, isRunning: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colors.primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text("Load Test", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${if (isAsyncMode) "Async · pool" else "Sync · new connection per request"} · $gatewayLabel",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
            if (isRunning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Yellow400)
                    Text("Running…", fontSize = 12.sp, color = Yellow400, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, tint = MaterialTheme.colors.secondary, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onSurface
                )
            }
            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Config panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConfigPanel(
    config: LoadTestConfig,
    onConfigChange: (LoadTestConfig) -> Unit,
    isAsyncMode: Boolean,
    testHex: String,
    onTestHexChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Row 1: requests · concurrent · ramp-up
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfigField(
                modifier = Modifier.weight(1f),
                label = "Total Requests",
                value = config.totalRequests.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> onConfigChange(config.copy(totalRequests = v.coerceAtLeast(1))) } }
            )
            ConfigField(
                modifier = Modifier.weight(1f),
                label = "Concurrent Users",
                value = config.concurrentUsers.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> onConfigChange(config.copy(concurrentUsers = v.coerceAtLeast(1))) } }
            )
            ConfigField(
                modifier = Modifier.weight(1f),
                label = "Ramp-up (s)",
                value = config.rampUpSeconds.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> onConfigChange(config.copy(rampUpSeconds = v.coerceAtLeast(0))) } }
            )
        }

        // Row 2: think time · warm-up · pool size (async only)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConfigField(
                modifier = Modifier.weight(1f),
                label = "Think Time (ms)",
                value = config.thinkTimeMs.toString(),
                onValueChange = { it.toLongOrNull()?.let { v -> onConfigChange(config.copy(thinkTimeMs = v.coerceAtLeast(0L))) } }
            )
            ConfigField(
                modifier = Modifier.weight(1f),
                label = "Warm-up Requests",
                value = config.warmUpRequests.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> onConfigChange(config.copy(warmUpRequests = v.coerceAtLeast(0))) } }
            )
            if (isAsyncMode) {
                ConfigField(
                    modifier = Modifier.weight(1f),
                    label = "Pool Size (connections)",
                    value = config.connectionPoolSize.toString(),
                    onValueChange = { it.toIntOrNull()?.let { v -> onConfigChange(config.copy(connectionPoolSize = v.coerceIn(1, 200))) } }
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Row 3: timeout · max duration · stop on error
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigField(
                modifier = Modifier.weight(1f),
                label = "Timeout (ms)",
                value = config.requestTimeoutMs.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> onConfigChange(config.copy(requestTimeoutMs = v.coerceAtLeast(100))) } }
            )
            ConfigField(
                modifier = Modifier.weight(1f),
                label = "Max Duration (s, 0=unlimited)",
                value = config.maxDurationSeconds.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> onConfigChange(config.copy(maxDurationSeconds = v.coerceAtLeast(0))) } }
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Checkbox(
                    checked = config.stopOnFirstError,
                    onCheckedChange = { onConfigChange(config.copy(stopOnFirstError = it)) }
                )
                Text("Stop on first error", style = MaterialTheme.typography.caption)
            }
        }

        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))

        // Test message hex
        Text(
            "Test Message (hex)",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
        )
        TextField(
            value = testHex,
            onValueChange = onTestHexChange,
            modifier = Modifier.fillMaxWidth().height(76.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            ),
            placeholder = {
                Text(
                    "Paste raw hex message here (e.g. 0056020030…)",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.background,
                focusedIndicatorColor = MaterialTheme.colors.primary,
                unfocusedIndicatorColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
            ),
            maxLines = 3
        )
        if (testHex.isNotBlank()) {
            val norm  = testHex.replace("\\s".toRegex(), "")
            val valid = norm.length % 2 == 0 && norm.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (valid) Green400 else Red400))
                Text(
                    if (valid) "${norm.length / 2} bytes — valid hex"
                    else "Invalid hex (odd length or non-hex chars)",
                    style = MaterialTheme.typography.caption,
                    color = if (valid) Green400 else Red400
                )
            }
        }
    }
}

@Composable
private fun ConfigField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, fontSize = 10.sp) },
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
        singleLine = true,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.18f),
            focusedLabelColor = MaterialTheme.colors.primary
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Run controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RunControls(
    isRunning: Boolean,
    progress: LoadTestProgress,
    config: LoadTestConfig,
    testHex: String,
    onRun: () -> Unit,
    onStop: () -> Unit
) {
    val norm     = testHex.replace("\\s".toRegex(), "")
    val hexValid = norm.isNotBlank() && norm.length % 2 == 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status / description
            if (isRunning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${progress.sent} / ${config.totalRequests}  requests  ·  ${"%.2f".format(progress.currentTps)} TPS",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${(progress.elapsedMs / 1000.0).let { "%.1f".format(it) }}s elapsed",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
                    )
                }
            } else {
                Text(
                    text = when (progress.status) {
                        LoadTestStatus.COMPLETED -> "Last run: ${progress.sent} requests in ${progress.elapsedMs} ms"
                        LoadTestStatus.IDLE      -> "Configure parameters above, then click Run"
                        else -> progress.message
                    },
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
                )
            }

            // Run / Stop button
            if (!isRunning) {
                Button(
                    onClick = onRun,
                    enabled = hexValid,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        disabledBackgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Load Test", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                OutlinedButton(
                    onClick = onStop,
                    border = BorderStroke(1.dp, Red400),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp), tint = Red400)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Red400)
                }
            }
        }

        // Progress bar (only while running or warming up)
        if (isRunning || progress.status == LoadTestStatus.WARMING_UP) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = progress.progressFraction,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (progress.status == LoadTestStatus.WARMING_UP) Yellow400
                            else MaterialTheme.colors.primary,
                    backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${(progress.progressFraction * 100).toInt()}%  ·  ${progress.sent} sent",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LiveBadge("OK",   progress.success.toString(),                  Green400)
                        LiveBadge("FAIL", progress.failure.toString(),                  Red400)
                        LiveBadge("TPS",  "%.1f".format(progress.currentTps),           Yellow400)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBadge(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f))
        Text(value, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = color)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Results summary
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultsSummary(stats: LoadTestStats, progress: LoadTestProgress, isRunning: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // KPI cards
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "Total",    stats.totalRequests.toString(),      Blue400,   "requests")
            StatCard(Modifier.weight(1f), "Success",  stats.successCount.toString(),       Green400,
                "%.1f%%".format(if (stats.totalRequests > 0) stats.successCount * 100.0 / stats.totalRequests else 0.0))
            StatCard(Modifier.weight(1f), "Failed",   stats.failureCount.toString(),       Red400,    "%.1f%% err".format(stats.errorRate))
            StatCard(Modifier.weight(1f), "TPS",      "%.2f".format(stats.throughputTps), Yellow400, "req/sec")
            StatCard(Modifier.weight(1f), "Duration", "${progress.elapsedMs} ms",         MaterialTheme.colors.onSurface, "wall clock")
        }

        // Latency KPIs
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "Avg", "%.1f ms".format(stats.avgLatencyMs), Purple400, "average latency")
            StatCard(Modifier.weight(1f), "Min", "${stats.minLatencyMs} ms",           Green400,  "fastest")
            StatCard(Modifier.weight(1f), "Max", "${stats.maxLatencyMs} ms",           Red400,    "slowest")
        }

        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))

        // Percentile cards
        Text(
            "Percentiles",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PercentileCard(Modifier.weight(1f), "P50", stats.p50LatencyMs)
            PercentileCard(Modifier.weight(1f), "P75", stats.p75LatencyMs)
            PercentileCard(Modifier.weight(1f), "P90", stats.p90LatencyMs)
            PercentileCard(Modifier.weight(1f), "P95", stats.p95LatencyMs)
            PercentileCard(Modifier.weight(1f), "P99", stats.p99LatencyMs)
        }

        // Visual latency profile bar chart
        if (stats.totalRequests > 0) {
            LatencyHistogram(stats)
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, valueColor: Color, sub: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colors.background,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = valueColor)
            Text(sub, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun PercentileCard(modifier: Modifier, label: String, valueMs: Long) {
    val color = when (label) {
        "P50" -> Green400
        "P75" -> Color(0xFF86EFAC)
        "P90" -> Yellow400
        "P95" -> Color(0xFFFCA5A5)
        else  -> Red400
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text("$valueMs ms", style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LatencyHistogram(stats: LoadTestStats) {
    val buckets = listOf(
        "Min" to stats.minLatencyMs,
        "P50" to stats.p50LatencyMs,
        "P75" to stats.p75LatencyMs,
        "P90" to stats.p90LatencyMs,
        "P95" to stats.p95LatencyMs,
        "P99" to stats.p99LatencyMs,
        "Max" to stats.maxLatencyMs
    )
    val maxVal = buckets.maxOf { it.second }.coerceAtLeast(1L)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Latency Profile",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
        )
        buckets.forEach { (label, value) ->
            val fraction = (value.toFloat() / maxVal).coerceIn(0.02f, 1f)
            val barColor = when (label) {
                "Min", "P50" -> Green400
                "P75", "P90" -> Yellow400
                else         -> Red400
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    label,
                    modifier = Modifier.width(36.dp),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Box(
                    modifier = Modifier
                        .weight(fraction)
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor.copy(alpha = 0.75f))
                )
                if (1f - fraction > 0.02f) Spacer(modifier = Modifier.weight((1f - fraction).coerceAtLeast(0.01f)))
                Text("$value ms", style = MaterialTheme.typography.caption, fontFamily = FontFamily.Monospace, color = barColor)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Export panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExportPanel(
    gw: HostSimulator,
    config: LoadTestConfig,
    stats: LoadTestStats,
    results: List<RequestResult>,
    isAsyncMode: Boolean,
    gatewayLabel: String,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    var exportStatus by remember { mutableStateOf("") }
    var isExporting  by remember { mutableStateOf(false) }
    val window = gw.composeWindow

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Full HTML report: 4 interactive charts, percentile table, config, and per-request details.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
            )
            if (exportStatus.isNotBlank()) {
                Text(
                    exportStatus,
                    style = MaterialTheme.typography.caption,
                    color = if (exportStatus.startsWith("Saved")) Green400 else Red400
                )
            }
        }
        Button(
            onClick = {
                coroutineScope.launch {
                    isExporting = true
                    exportStatus = ""
                    try {
                        val file = withContext(Dispatchers.IO) { chooseHtmlSavePath(window) }
                        if (file != null) {
                            withContext(Dispatchers.IO) {
                                LoadTestHtmlExporter.export(
                                    file = file,
                                    config = config,
                                    stats = stats,
                                    results = results,
                                    isAsyncMode = isAsyncMode,
                                    gatewayLabel = gatewayLabel
                                )
                            }
                            exportStatus = "Saved → ${file.absolutePath}"
                        }
                    } catch (e: Exception) {
                        exportStatus = "Error: ${e.message}"
                    } finally {
                        isExporting = false
                    }
                }
            },
            enabled = !isExporting,
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
        ) {
            if (isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exporting…", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export HTML Report", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun chooseHtmlSavePath(window: java.awt.Window?): File? {
    val isMac       = System.getProperty("os.name").lowercase().contains("mac")
    val defaultName = "loadtest_report_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}.html"

    return if (isMac) {
        val fd = FileDialog(window as? java.awt.Frame, "Save Load Test Report", FileDialog.SAVE)
        fd.file = defaultName
        fd.isVisible = true
        if (fd.file != null) File(fd.directory, fd.file) else null
    } else {
        val chooser = JFileChooser().apply {
            dialogTitle          = "Save Load Test Report"
            fileSelectionMode    = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            fileFilter           = FileNameExtensionFilter("HTML files (*.html)", "html")
            selectedFile         = File(defaultName)
        }
        val result = chooser.showSaveDialog(window as? java.awt.Component)
        if (result == JFileChooser.APPROVE_OPTION) {
            var f = chooser.selectedFile
            if (!f.name.endsWith(".html", ignoreCase = true)) f = File(f.parent, f.name + ".html")
            f
        } else null
    }
}
