package `in`.aicortex.iso8583studio.ui.screens.apduSimulator

import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.ConnectionInterface
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogTab
import kotlinx.coroutines.CoroutineScope

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.rememberCardCoroutineScope
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.CardServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.ApduUtil
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.ExperimentalAtomicApi

enum class APDUSimulatorTabs(val title: String, val icon: ImageVector) {
    SESSION_CONTROL("Card Session", Icons.Default.CreditCard),
    APDU_LOG("APDU Monitor", Icons.Default.Article),
    TLV_PARSER("TLV Parser", Icons.Default.DeviceHub),
    FLOW_ANALYSIS("Flow Analysis", Icons.Default.Timeline),
}

/**
 * Modern APDU Card Simulator screen following HostSimulatorScreen architecture
 */
@Composable
fun APDUSimulatorScreen(
    window: ComposeWindow,
    navigationController: NavigationController,
    config: APDUSimulatorConfig?,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
    onError: ResultDialogInterface? = null,
) {
    var cardService: CardServiceImpl? = null
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            coroutineScope.launch {
                cardService?.stop()
            }
        }
    }

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "APDU Card Simulator - ${config?.name ?: "Unknown"}",
                onBackClick = {
                    onBack()
                },
                actions = {
                    IconButton(onClick = { onSaveClick() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save Configuration")
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        if (config != null) {
            cardService = CardServiceImpl(config)
            cardService.composeWindow = window
            if (onError != null) {
                cardService.setShowErrorListener(onError)
            }
            APDUSimulator(
                cardService = cardService,
                onSaveClick = onSaveClick,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No card configuration selected")
            }
        }
    }
}

/**
 * Modern APDU Card Simulator implementation following HostSimulator pattern
 */
@OptIn(ExperimentalAtomicApi::class)
@Composable
fun APDUSimulator(
    cardService: CardServiceImpl,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isStarted by remember { mutableStateOf(cardService.started.get()) }
    var transactionCount by remember { mutableStateOf("0") }
    var bytesOutgoing by remember { mutableStateOf(cardService.bytesOutgoing) }
    var bytesIncoming by remember { mutableStateOf(cardService.bytesIncoming) }
    var sessionCount by remember { mutableStateOf(cardService.sessionCount) }
    var activeReaders by remember { mutableStateOf(cardService.activeReaders.size) }
    var isCardPresent by remember { mutableStateOf(false) }
    var cardAtr by remember { mutableStateOf("") }

    var rawCommand by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var rawResponse by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    val logText = remember { mutableStateListOf<LogEntry>() }

    var processingTime by remember { mutableStateOf("0") }
    var lastStatusWord by remember { mutableStateOf("") }
    val coroutineScope = rememberCardCoroutineScope(cardService)

    var selectedTlvTag = remember { mutableStateOf<String?>(null) }
    var selectedTlvIndex = remember { mutableStateOf<Int?>(null) }
    var showTlvAnalysis = remember { mutableStateOf(false) }
    var showApduParser = remember { mutableStateOf(true) }
    var isReaderMode = remember {
        mutableStateOf(
            when (cardService.configuration.connectionInterface) {
                ConnectionInterface.PC_SC -> true
                ConnectionInterface.NFC -> true
                ConnectionInterface.MOCK -> false
                else -> true
            }
        )
    }
    var animationTrigger = remember { mutableStateOf(0) }
    var rawApdu = remember { mutableStateOf("") }
    var parseError = remember { mutableStateOf<String?>(null) }
    var currentTlvData = remember { mutableStateOf<Map<String, String>?>(null) }
    var currentApduFields = remember { mutableStateOf<Map<String, String>?>(null) }
    var searchQuery = remember { mutableStateOf("") }

    // Set up event handlers following HostSimulator pattern
    cardService.onReceiveCommand= { apdu ->
        rawCommand = ApduUtil.bytesToHexString(apdu?.rawCommand ?: byteArrayOf())
        command = apdu?.logFormat() ?: ""
        response = ""
        rawResponse = ""
    }

    cardService.onSendResponse = { apdu ->
        response = apdu?.logFormat() ?: ""
        rawResponse = ApduUtil.bytesToHexString(apdu?.rawResponse ?: byteArrayOf())
        lastStatusWord = apdu?.statusWord ?: ""
    }

    cardService.onCardInserted = { atr ->
        isCardPresent = true
        cardAtr = ApduUtil.bytesToHexString(atr ?: byteArrayOf())
    }

    cardService.onCardRemoved = {
        isCardPresent = false
        cardAtr = ""
    }

    cardService.beforeProcessCommand = {
        val startTime = System.currentTimeMillis()
        processingTime = "Processing..."

        // Update processing time after command completion
        coroutineScope.launch {
            delay(100) // Wait for processing
            val endTime = System.currentTimeMillis()
            processingTime = "${endTime - startTime}ms"
        }
    }

    cardService.beforeWriteLog = {
        logText.add(it)
    }

    // Update metrics periodically following HostSimulator pattern
    LaunchedEffect(key1 = isStarted) {
        if (isStarted) {
            while (isStarted) {
                delay(1000)
                bytesOutgoing = cardService.bytesOutgoing
                bytesIncoming = cardService.bytesIncoming
                sessionCount = cardService.sessionCount
                activeReaders = cardService.activeReaders.size
                transactionCount = (transactionCount.toIntOrNull() ?: 0).let {
                    if (it < cardService.sessionCount.get()) cardService.sessionCount.toString() else it.toString()
                }
            }
        }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }

    // Dynamic tab list based on connection interface (following gateway type pattern)
    val tabList = when (cardService.configuration.connectionInterface) {
        ConnectionInterface.PC_SC -> listOf(
            APDUSimulatorTabs.SESSION_CONTROL,
            APDUSimulatorTabs.APDU_LOG,
            APDUSimulatorTabs.TLV_PARSER,
            APDUSimulatorTabs.FLOW_ANALYSIS,
        )
        ConnectionInterface.NFC -> listOf(
            APDUSimulatorTabs.SESSION_CONTROL,
            APDUSimulatorTabs.APDU_LOG,
            APDUSimulatorTabs.TLV_PARSER,
            APDUSimulatorTabs.FLOW_ANALYSIS
        )
        ConnectionInterface.MOCK -> listOf(
            APDUSimulatorTabs.SESSION_CONTROL,
            APDUSimulatorTabs.APDU_LOG,
            APDUSimulatorTabs.TLV_PARSER,
        )
        else -> listOf(
            APDUSimulatorTabs.SESSION_CONTROL,
            APDUSimulatorTabs.APDU_LOG,
            APDUSimulatorTabs.TLV_PARSER
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Tab row with identical styling from HostSimulator
        TabRow(
            selectedTabIndex = selectedTabIndex,
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.customTabIndicatorOffset(tabPositions[selectedTabIndex]),
                    height = 3.dp,
                    color = MaterialTheme.colors.primary
                )
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
                    },
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Tab content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (tabList[selectedTabIndex]) {
                APDUSimulatorTabs.SESSION_CONTROL -> CardSessionTab(
                    cardService = cardService,
                    isStarted = isStarted,
                    onStartStopClick = {
                        coroutineScope.launchSafely {
                            if (!isStarted) {
                                cardService.start()
                                isStarted = true
                            } else {
                                cardService.stop()
                                isStarted = false
                            }
                        }
                    },
                    onClearClick = {
                        rawCommand = ""
                        command = ""
                        rawResponse = ""
                        response = ""
                    },
                    transactionCount = transactionCount,
                    isCardPresent = isCardPresent,
                    cardAtr = cardAtr,
                    processingTime = processingTime,
                    lastStatusWord = lastStatusWord,
                    command = command,
                    rawCommand = rawCommand,
                    response = response,
                    rawResponse = rawResponse
                )

                APDUSimulatorTabs.APDU_LOG -> LogTab(
                    logEntries = logText,
                    onClearClick = { logText.clear() },
//                    sessionCount = sessionCount.get(),
                    bytesIncoming = bytesIncoming.get().toLong(),
                    bytesOutgoing = bytesOutgoing.get().toLong(),
                    connectionCount = 0,
                    concurrentConnections = 0
//                    activeReaders = activeReaders
                )

                APDUSimulatorTabs.TLV_PARSER -> TlvParserTab(
                    cardService = cardService,
                    selectedTlvTag = selectedTlvTag,
                    selectedTlvIndex = selectedTlvIndex,
                    showTlvAnalysis = showTlvAnalysis,
                    showApduParser = showApduParser,
                    animationTrigger = animationTrigger,
                    rawMessage = rawApdu,
                    parseError = parseError,
                    currentTlvData = currentTlvData,
                    currentApduFields = currentApduFields,
                    searchQuery = searchQuery
                )

                APDUSimulatorTabs.FLOW_ANALYSIS -> FlowAnalysisTab(
                    cardService = cardService,
                    logEntries = logText
                )

            }
        }
    }
}

// Custom tab indicator offset extension (identical to HostSimulator)
fun Modifier.customTabIndicatorOffset(
    currentTabPosition: TabPosition
): Modifier = composed {
    val indicatorWidth = 32.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2

    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset)
        .width(indicatorWidth)
}


// Extension function for safe coroutine launching (following HostSimulator pattern)
fun CoroutineScope.launchSafely(block: suspend CoroutineScope.() -> Unit) {
    launch {
        try {
            block()
        } catch (e: Exception) {
            // Handle exceptions appropriately
            e.printStackTrace()
        }
    }
}