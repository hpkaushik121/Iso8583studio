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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.AppSettings
import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.HsmServiceImpl
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMSimulatorConfig
import kotlinx.coroutines.delay
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.SimulatorHandlerTab
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogTab
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.atomics.ExperimentalAtomicApi



enum class HsmSimulatorTabs(val title: String, val icon: ImageVector) {
    HANDLER("HSM Handler", Icons.Default.CompareArrows),
    KEY_MANAGEMENT("Key Management", Icons.Default.VpnKey),
    HOST_COMMANDS("Host Commands", Icons.Default.Terminal),
    SECURE_COMMANDS("Secure Commands", Icons.Default.Lock),
    LOGS("Logs", Icons.Default.Article),
}

// --- MAIN HSM SIMULATOR SCREEN ---

/**
 * @param service  Optional pre-created service from the session manager.
 *                 When provided, this screen does NOT own the service lifecycle —
 *                 [SimulatorSessionManager.closeSession] is responsible for stopping it.
 *                 When null (standalone use), the screen creates and owns the service.
 */
@Composable
fun HsmSimulatorScreen(
    config: HSMSimulatorConfig,
    onBack: () -> Unit,
    service: HsmServiceImpl? = null,
) {
    val scope = rememberCoroutineScope()
    // Use the injected service when available (session tab), otherwise own it locally.
    val ownsService = service == null
    val hsmService = remember(config) { service ?: HsmServiceImpl(config) }

    // Only stop on disposal when this screen owns the service (not managed by session manager).
    if (ownsService) {
        DisposableEffect(hsmService) {
            onDispose {
                scope.launch { hsmService.stop() }
            }
        }
    }

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "HSM Simulator - ${config.name}",
                onBackClick = {
                    scope.launch {
                        hsmService.stop()
                        onBack()
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        HsmSimulator(
            hsm = hsmService,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalAtomicApi::class)
@Composable
fun HsmSimulator(hsm: HsmServiceImpl, modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = HsmSimulatorTabs.values().toList()
    val hsmState = hsm.hsmState.collectAsState()
    var rawRequest by remember { mutableStateOf("") }
    var formattedRequest by remember { mutableStateOf("") }
    var rawResponse by remember { mutableStateOf("") }
    var formattedResponse by remember { mutableStateOf("") }

    hsm.receivedFromSource = {
        rawResponse = ""
        formattedResponse = ""
        formattedRequest = ""
        rawRequest = it + "\n"
    }

    hsm.receivedFromSourceFormatted = {
        formattedRequest = it + "\n"
    }

    hsm.sentToSource = {
        rawResponse = it + "\n"
    }
    hsm.sentToSourceFormatted = {
        formattedResponse = it + "\n"
    }


    // Auto-clear logs at configured interval (global setting)
    val autoClearEnabled = AppSettings.autoClearLogsEnabled
    val autoClearInterval = AppSettings.autoClearLogsIntervalMinutes
    LaunchedEffect(autoClearEnabled, autoClearInterval) {
        if (autoClearEnabled) {
            val intervalMs = autoClearInterval * 60 * 1000L
            while (true) {
                delay(intervalMs)
                if (AppSettings.autoClearLogsEnabled) {
                    hsm.clearLogs()
                    if (AppSettings.deleteLogFileOnClear) {
                        try {
                            val logFile = java.io.File(hsm.configuration.logFileName)
                            if (logFile.exists()) logFile.delete()
                            val parent = logFile.parentFile ?: java.io.File(".")
                            val baseName = logFile.nameWithoutExtension
                            val ext = logFile.extension
                            for (i in 1..10) {
                                val rotated = java.io.File(parent, "$baseName$i.$ext")
                                if (rotated.exists()) rotated.delete()
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        }
    }

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                tab.title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
            }
        }
        val scope = rememberCoroutineScope();

        // All tabs are kept alive in the composition tree to preserve their state on switch.
        // Only the selected tab is visible; others are collapsed to 0dp and clipped.
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            tabList.forEachIndexed { index, tab ->
                val isVisible = selectedTabIndex == index
                val tabModifier = if (isVisible)
                    Modifier.fillMaxSize()
                else
                    Modifier.requiredSize(0.dp).clipToBounds()

                Box(modifier = tabModifier) {
                    when (tab) {
                        HsmSimulatorTabs.HANDLER -> SimulatorHandlerTab(
                            simulator = hsm,
                            isStarted = hsmState.value.started,
                            onStartStopClick = {
                                if (hsmState.value.started) {
                                    scope.launch { hsm.stop() }
                                } else {
                                    scope.launch { hsm.start() }
                                }
                            },
                            onClearClick = {
                                rawRequest = ""
                                formattedRequest = ""
                                rawResponse = ""
                                formattedResponse = ""
                            },
                            transactionCount = "0",
                            isHoldMessage = false,
                            onHoldMessageChange = {},
                            holdMessageTime = "0",
                            onHoldMessageTimeChange = {},
                            waitingRemain = "0",
                            onSendClick = {},
                            request = formattedRequest,
                            rawRequest = rawRequest,
                            response = formattedResponse,
                            rawResponse = rawResponse,
                            connectedClients = hsmState.value.activeClients.size
                        )

                        HsmSimulatorTabs.KEY_MANAGEMENT -> KeyManagementOverviewTab(hsm = hsm)
                        HsmSimulatorTabs.HOST_COMMANDS  -> HsmHostCommandsTab(hsm = hsm)
                        HsmSimulatorTabs.SECURE_COMMANDS -> HsmSecureCommandsTab(hsm = hsm)
                        HsmSimulatorTabs.LOGS -> LogTab(
                            logEntries = hsmState.value.rawRequest,
                            onClearClick = { hsm.clearLogs() },
                            connectionCount = 0,
                            bytesIncoming = 0L,
                            bytesOutgoing = 0L,
                            concurrentConnections = 0
                        )
                    }
                }
            }
        }
    }
}


// Custom tab indicator offset extension
private fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier =
    composed {
        val indicatorWidth = 32.dp
        val currentTabWidth = currentTabPosition.width
        val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2

        fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = indicatorOffset)
            .width(indicatorWidth)
    }
