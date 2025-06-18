package `in`.aicortex.iso8583studio.ui.screens.hsmSimulator

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.HsmServiceImpl
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogTab
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


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


enum class HsmSimulatorTabs(val title: String, val icon: ImageVector) {
    HANDLER("HSM Handler", Icons.Default.CompareArrows),
    KEY_MANAGEMENT("Key Management", Icons.Default.VpnKey),
    LOGS("Logs", Icons.Default.Article),
    HSM_RESPONSE_CONFIG("Hsm Response Config", Icons.Default.Settings),
}

// --- MAIN HSM SIMULATOR SCREEN ---

@Composable
fun HsmSimulatorScreen(
    config: HSMSimulatorConfig,
    onBack: () -> Unit
) {
    val hsmService = remember { HsmServiceImpl(config) }



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
fun HsmSimulator(hsm: HsmServiceImpl, modifier: Modifier = Modifier) {
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
                HsmSimulatorTabs.KEY_MANAGEMENT -> Text("Pending")
                HsmSimulatorTabs.LOGS -> LogTab(
                    logEntries = HsmLogManager.logEntries,
                    onClearClick = { HsmLogManager.clearLogs() },
                    connectionCount = 0,
                    bytesIncoming = 0L,
                    bytesOutgoing = 0L,
                    concurrentConnections = 0
                )
                HsmSimulatorTabs.HSM_RESPONSE_CONFIG -> HsmResponseConfigScreen(hsm = hsm) {

                }
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
