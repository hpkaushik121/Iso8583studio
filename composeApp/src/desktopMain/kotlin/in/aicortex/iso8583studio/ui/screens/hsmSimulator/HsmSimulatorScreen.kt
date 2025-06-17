package `in`.aicortex.iso8583studio.ui.screens.hsmSimulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogTab
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// --- DATA & SERVICE LAYER FOR HSM SIMULATOR ---

data class HsmConfig(
    var name: String = "Default HSM",
    var serverAddress: String = "127.0.0.1",
    var serverPort: Int = 9999
)

data class HsmKey(val id: String, val type: String, val value: String, val kcv: String)

object HsmLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() { _logEntries.clear() }

    fun addLog(entry: LogEntry) {
        _logEntries.add(0, entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, _logEntries.size)
    }

    fun logInfo(message: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        addLog(LogEntry(timestamp, LogType.INFO, message, message))
    }

    fun logCommand(command: String, request: String, response: String, isError: Boolean = false) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = "Request:\n$request\n\nResponse:\n$response"
        val logType = if (isError) LogType.ERROR else LogType.TRANSACTION
        val message = if (isError) "$command Failed" else "$command Executed"
        addLog(LogEntry(timestamp, logType, message, details))
    }
}

class HsmSimulatorService(val config: HsmConfig) {
    private val _keys = mutableStateListOf(
        HsmKey("BDK_01", "BDK", "0123456789ABCDEFFEDCBA9876543210", "1A2B3C"),
        HsmKey("TMK_01", "TMK", "112233445566778899AABBCCDDEEFF00", "F0E1D2")
    )
    val keys: List<HsmKey> get() = _keys

    private var serverSocket: ServerSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var isRunning by mutableStateOf(false)
    val activeClients = mutableStateListOf<Socket>()

    var onRequestReceived: ((String) -> Unit)? = null
    var onResponseSent: ((String) -> Unit)? = null



    private fun processCommand(command: String): Pair<String, Boolean> {
        Thread.sleep(Random.nextLong(50, 200)) // Simulate HSM processing time
        return when {
            command.startsWith("GenerateKey", ignoreCase = true) -> {
                val newKey = generateRandomHex(32)
                val kcv = generateRandomHex(6)
                _keys.add(HsmKey("ZMK_${_keys.size + 1}", "ZMK", newKey, kcv))
                "Response: OK;Key=${newKey};KCV=${kcv}" to false
            }
            command.startsWith("Encrypt", ignoreCase = true) -> {
                "Response: OK;Ciphertext=${generateRandomHex(64)}" to false
            }
            command.startsWith("CalculateMAC", ignoreCase = true) -> {
                "Response: OK;MAC=${generateRandomHex(16)}" to false
            }
            else -> "Response: Error;Code=01;Message=Unknown Command" to true
        }
    }

    private fun generateRandomHex(length: Int): String {
        return (1..length).map { Random.nextInt(0, 16).toString(16) }.joinToString("").uppercase()
    }
}


enum class HsmSimulatorTabs(val title: String, val icon: ImageVector) {
    HANDLER("HSM Handler", Icons.Default.CompareArrows),
    KEY_MANAGEMENT("Key Management", Icons.Default.VpnKey),
    LOGS("Logs", Icons.Default.Article),
    SETTINGS("Settings", Icons.Default.Settings),
}

// --- MAIN HSM SIMULATOR SCREEN ---

@Composable
fun HsmSimulatorScreen(onBack: () -> Unit) {
    val config = remember { HsmConfig() }
    val hsmService = remember { HsmSimulatorService(config) }



    Scaffold(
        topBar = { AppBarWithBack(title = "HSM Simulator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        HsmSimulator(
            hsm = hsmService,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun HsmSimulator(hsm: HsmSimulatorService, modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = HsmSimulatorTabs.values().toList()

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(modifier = Modifier.customTabIndicatorOffset(tabPositions[selectedTabIndex]))
            }
        ) {
            tabList.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(imageVector = tab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tab.title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (tabList[selectedTabIndex]) {
                HsmSimulatorTabs.HANDLER -> HsmHandlerTab(hsm = hsm)
                HsmSimulatorTabs.KEY_MANAGEMENT -> KeyManagementTab(keys = hsm.keys)
                HsmSimulatorTabs.LOGS -> LogTab(
                    logEntries = HsmLogManager.logEntries,
                    onClearClick = { HsmLogManager.clearLogs() },
                    connectionCount = 0,
                    bytesIncoming = 0L,
                    bytesOutgoing = 0L,
                    concurrentConnections = 0
                )
                HsmSimulatorTabs.SETTINGS -> SettingsTab(config = hsm.config, isRunning = hsm.isRunning)
            }
        }
    }
}

// --- TAB CONTENT COMPOSABLES ---

@Composable
private fun HsmHandlerTab(hsm: HsmSimulatorService) {
    var request by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }

    hsm.onRequestReceived = { request = it }
    hsm.onResponseSent = { response = it }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Control Panel
        CompactControlPanel(
            isStarted = hsm.isRunning,
            onStartStopClick = { },
            onClearClick = { request = ""; response = "" },
            config = hsm.config,
            connectionCount = hsm.activeClients.size
        )

        // Request/Response Panels
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuadrantPanel(modifier = Modifier.weight(1f), "Incoming Command", request)
            QuadrantPanel(modifier = Modifier.weight(1f), "Outgoing Response", response)
        }
    }
}

@Composable
private fun CompactControlPanel(
    isStarted: Boolean,
    onStartStopClick: () -> Unit,
    onClearClick: () -> Unit,
    config: HsmConfig,
    connectionCount: Int
) {
    Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartStopClick,
                    colors = ButtonDefaults.buttonColors(backgroundColor = if (isStarted) MaterialTheme.colors.error else SuccessGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(if (isStarted) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = "Start/Stop", tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text(if (isStarted) "Stop" else "Start", color = Color.White)
                }
                OutlinedButton(onClick = onClearClick, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
                }
            }

            if (isStarted) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusIndicator(text = "Listening on ${config.serverAddress}:${config.serverPort}", color = SuccessGreen, icon = Icons.Default.Sensors)
                    StatusIndicator(text = "$connectionCount Active Connections", color = MaterialTheme.colors.primary, icon = Icons.Default.People)
                }
            } else {
                StatusIndicator(text = "Server Stopped", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), icon = Icons.Default.SensorsOff)
            }
        }
    }
}

@Composable
private fun StatusIndicator(text: String, color: Color, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, color = color, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Medium)
    }
}


@Composable
private fun QuadrantPanel(modifier: Modifier, title: String, content: String) {
    Panel(modifier) {
        Column(Modifier.fillMaxSize()) {
            Text(title, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                SelectionContainer {
                    Text(content, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }
        }
    }
}


@Composable
private fun KeyManagementTab(keys: List<HsmKey>) {
    Panel(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Key Inventory", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            Text("${keys.size} keys", style = MaterialTheme.typography.caption)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(keys) { key ->
                KeyCard(key)
            }
        }
    }
}

@Composable
private fun KeyCard(key: HsmKey) {
    Card(elevation = 1.dp, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VpnKey, contentDescription = "Key", tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(key.id, fontWeight = FontWeight.Bold)
                Text("Type: ${key.type}", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(key.value.take(16) + "...", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Surface(color = MaterialTheme.colors.primary.copy(alpha = 0.1f), shape = CircleShape) {
                    Text("KCV: ${key.kcv}", style = MaterialTheme.typography.caption, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = MaterialTheme.colors.primary)
                }
            }
        }
    }
}


@Composable
private fun SettingsTab(config: HsmConfig, isRunning: Boolean) {
    var address by remember { mutableStateOf(config.serverAddress) }
    var port by remember { mutableStateOf(config.serverPort.toString()) }

    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Network Settings", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Server IP Address") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text("Server Port") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning
            )
            Button(
                onClick = {
                    config.serverAddress = address
                    config.serverPort = port.toIntOrNull() ?: 9999
                },
                enabled = !isRunning
            ) {
                Text("Apply Settings")
            }
            if(isRunning) {
                Text("Stop the server to change settings.", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
            }
        }
    }
}


// Custom tab indicator offset extension
private fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 32.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2

    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset)
        .width(indicatorWidth)
}
