package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
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
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.data.rememberIsoCoroutineScope
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.ExperimentalAtomicApi

enum class HostSimulatorTabs(val title: String, val icon: ImageVector) {
    HOST_HANDLER("ISO8583 Transaction", Icons.Default.CompareArrows),
    LOGS("Logs", Icons.Default.Article),
    SETTINGS("Settings", Icons.Default.Settings),
    TEMPLATE("ISO8583 Template", Icons.Default.Code),
    SEND_MESSAGE("Send Message", Icons.Default.Send),
    UNPACK_MESSAGE("Unsolicited Message", Icons.Default.MarkEmailRead),
}

/**
 * Modern Host Simulator screen
 */
@Composable
fun HostSimulatorScreen(
    window: ComposeWindow,
    config: GatewayConfig?,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
    onError: ResultDialogInterface? = null,
) {
    var gw: GatewayServiceImpl? = null
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "Host Simulator - ${config?.name ?: "Unknown"}",
                onBackClick = {
                    coroutineScope.launch {
                        gw?.stop()
                    }
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
            gw = GatewayServiceImpl(config)
            gw!!.composeWindow = window
            if (onError != null) {
                gw!!.setShowErrorListener(onError)
            }
            HostSimulator(
                gw = gw!!,
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
                Text("No configuration selected")
            }
        }
    }
}

/**
 * Modern Host Simulator implementation
 */
@OptIn(ExperimentalAtomicApi::class)
@Composable
fun HostSimulator(
    gw: GatewayServiceImpl,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isStarted by remember { mutableStateOf(gw.started.load()) }
    var transactionCount by remember { mutableStateOf("0") }
    var bytesOutgoing by remember { mutableStateOf(gw.bytesOutgoing) }
    var bytesIncoming by remember { mutableStateOf(gw.bytesIncoming) }
    var connectionCount by remember { mutableStateOf(gw.connectionCount) }
    var concurrentConnections by remember { mutableStateOf(gw.activeClients.size) }
    var isHoldMessage by remember { mutableStateOf(false) }
    var holdMessageTime by remember { mutableStateOf("60") }

    var rawRequest by remember { mutableStateOf("") }
    var request by remember { mutableStateOf("") }
    var rawResponse by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    val logText = remember { mutableStateListOf<LogEntry>() }

    var waitingRemain by remember { mutableStateOf("0") }
    var sendHoldMessage by remember { mutableStateOf(true) }
    val coroutineScope = rememberIsoCoroutineScope(gw)

    var selectedField = remember { mutableStateOf<BitAttribute?>(null) }
    var selectedFieldIndex = remember { mutableStateOf<Int?>(null) }
    var showBitmapAnalysis = remember { mutableStateOf(false) }
    var showMessageParser = remember { mutableStateOf(true) }
    var isFirst = remember {
        mutableStateOf(
            if (gw.configuration.gatewayType == GatewayType.CLIENT) {
                false
            } else if (gw.configuration.gatewayType == GatewayType.SERVER) {
                true
            }else{
                true
            }
        )
    }
    var animationTrigger = remember { mutableStateOf(0) }
    var rawMessage = remember { mutableStateOf("") }
    var parseError = remember { mutableStateOf<String?>(null) }
    var currentFields = remember { mutableStateOf<Array<BitAttribute>?>(null) }
    var currentBitmap = remember { mutableStateOf<ByteArray?>(null) }
    var searchQuery = remember { mutableStateOf("") }


    // Set up event handlers
    gw.onReceiveFromSource { iso ->
        rawRequest = IsoUtil.bytesToHexString(iso?.rawMessage ?: byteArrayOf())
        request = iso?.logFormat() ?: ""
        response = ""
        rawResponse = ""
    }

    gw.onReceiveFromDest { iso ->
        response = iso?.logFormat() ?: ""
        rawResponse = IsoUtil.bytesToHexString(iso?.rawMessage ?: byteArrayOf())
    }

    gw.onSentToSource { iso ->
        response = iso?.logFormat() ?: ""
        rawResponse = IsoUtil.bytesToHexString(iso?.rawMessage ?: byteArrayOf())
    }

    gw.onSentToDest { iso ->
        rawRequest = IsoUtil.bytesToHexString(iso?.rawMessage ?: byteArrayOf())
        request = iso?.logFormat() ?: ""
        response = ""
        rawResponse = ""
    }

    gw.beforeReceive {
        if (isHoldMessage) {
            waitingRemain = "0"
            sendHoldMessage = false
        }
    }

    gw.beforeWriteLog {
        logText.add(it)
    }

    // Handle hold message countdown
    LaunchedEffect(key1 = isHoldMessage, key2 = sendHoldMessage) {
        if (isHoldMessage && !sendHoldMessage) {
            val holdTimeValue = holdMessageTime.toIntOrNull() ?: 0
            if (holdTimeValue > 0) {
                for (i in 1..holdTimeValue) {
                    waitingRemain = "$i/$holdTimeValue"
                    delay(1000)
                    if (sendHoldMessage) break
                }
                waitingRemain = "0"
                if (!sendHoldMessage) {
                    sendHoldMessage = true
                    gw.sendHoldMessage?.invoke()
                }
            }
        }
    }

    // Update the bytesOutgoing, bytesIncoming, connectionCount values periodically
    LaunchedEffect(key1 = isStarted) {
        if (isStarted) {
            while (isStarted) {
                delay(1000)
                bytesOutgoing = gw.bytesOutgoing
                bytesIncoming = gw.bytesIncoming
                connectionCount = gw.connectionCount
                concurrentConnections = gw.activeClients.size
                transactionCount = (transactionCount.toIntOrNull() ?: 0).let {
                    if (it < gw.connectionCount.get()) gw.connectionCount.toString() else it.toString()
                }
            }
        }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList =
        if (gw.configuration.gatewayType == GatewayType.PROXY) {
            listOf(
                HostSimulatorTabs.HOST_HANDLER,
                HostSimulatorTabs.LOGS,
                HostSimulatorTabs.SETTINGS,
                HostSimulatorTabs.TEMPLATE,
                HostSimulatorTabs.SEND_MESSAGE,
                HostSimulatorTabs.UNPACK_MESSAGE
            )
        } else if (gw.configuration.gatewayType == GatewayType.CLIENT){
            listOf(
                HostSimulatorTabs.HOST_HANDLER,
                HostSimulatorTabs.LOGS,
                HostSimulatorTabs.TEMPLATE,
                HostSimulatorTabs.SEND_MESSAGE,
                HostSimulatorTabs.UNPACK_MESSAGE
            )
        } else {
            listOf(
                HostSimulatorTabs.HOST_HANDLER,
                HostSimulatorTabs.LOGS,
                HostSimulatorTabs.SETTINGS,
                HostSimulatorTabs.TEMPLATE,
                HostSimulatorTabs.UNPACK_MESSAGE
            )
        }
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Tab row
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
                HostSimulatorTabs.HOST_HANDLER -> ISO8583TransactionTab(
                    gw = gw,
                    isStarted = isStarted,
                    onStartStopClick = {
                        coroutineScope.launchSafely {
                            if (!isStarted) {
                                gw.start()
                                isStarted = true
                            } else {
                                gw.stop()
                                isStarted = false
                            }
                        }
                    },
                    onClearClick = {
                        rawRequest = ""
                        request = ""
                        rawResponse = ""
                        response = ""
                    },
                    transactionCount = transactionCount,
                    isHoldMessage = isHoldMessage,
                    onHoldMessageChange = {
                        isHoldMessage = it
                        gw.holdMessage = it
                    },
                    holdMessageTime = holdMessageTime,
                    onHoldMessageTimeChange = { holdMessageTime = it },
                    waitingRemain = waitingRemain,
                    onSendClick = {
                        sendHoldMessage = true
                        waitingRemain = "0"
                        coroutineScope.launchSafely {
                            gw.sendHoldMessage?.invoke()
                        }
                    },
                    request = request,
                    rawRequest = rawRequest,
                    response = response,
                    rawResponse = rawResponse
                )

                HostSimulatorTabs.LOGS -> LogTab(
                    logEntries = logText,
                    onClearClick = { logText.clear() },
                    connectionCount = connectionCount.get(),
                    bytesIncoming = bytesIncoming.get().toLong(),
                    bytesOutgoing = bytesOutgoing.get().toLong(),
                    concurrentConnections = concurrentConnections
                )

                HostSimulatorTabs.SETTINGS -> {
                    ISO8583SettingsScreen(
                        gw = gw,
                        onSaveClick = onSaveClick
                    )
                }

                HostSimulatorTabs.TEMPLATE -> {
                    Iso8583TemplateScreen(
                        config = gw.configuration,
                        onSaveClick = onSaveClick
                    )
                }

                HostSimulatorTabs.SEND_MESSAGE -> SendMessageTab(
                    gw = gw,
                    logText = logText,
                    onClearClick = { logText.clear() },
                )


                HostSimulatorTabs.UNPACK_MESSAGE -> UnsolicitedMessageTab(
                    gw = gw,
                    selectedField = selectedField,
                    selectedFieldIndex = selectedFieldIndex,
                    showBitmapAnalysis = showBitmapAnalysis,
                    showMessageParser = showMessageParser,
                    isFirst = isFirst,
                    animationTrigger = animationTrigger,
                    rawMessage = rawMessage,
                    parseError = parseError,
                    currentFields = currentFields,
                    currentBitmap = currentBitmap,
                    searchQuery = searchQuery
                )
            }
        }
    }
}


// Custom tab indicator offset extension
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