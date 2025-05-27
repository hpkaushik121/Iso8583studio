package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.getValue
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.rememberIsoCoroutineScope
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PrimaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SecondaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SectionHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.text.ifEmpty

/**
 * Modern Host Simulator screen
 */
@Composable
fun HostSimulatorScreen(
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
            if (onError != null) {
                gw.setShowErrorListener(onError)
            }
            HostSimulator(
                gw = gw,
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
    var logText by remember { mutableStateOf("") }

    var waitingRemain by remember { mutableStateOf("0") }
    var sendHoldMessage by remember { mutableStateOf(true) }
    val coroutineScope = rememberIsoCoroutineScope(gw)

    // Set up event handlers
    gw.onReceiveFromSource { iso ->
        rawRequest = IsoUtil.bcdToString(iso?.pack() ?: byteArrayOf())
        request = iso?.logFormat() ?: ""
        response = ""
        rawResponse = ""
    }

    gw.onReceiveFromDest { iso ->
        response = iso?.logFormat() ?: ""
        rawResponse = IsoUtil.bcdToString(iso?.pack() ?: byteArrayOf())
    }

    gw.onSentToSource { iso ->
        response = iso?.logFormat() ?: ""
        rawResponse = IsoUtil.bcdToString(iso?.pack() ?: byteArrayOf())
    }

    gw.onSentToDest { iso ->
        rawRequest = IsoUtil.bcdToString(iso?.pack() ?: byteArrayOf())
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
        logText += it + "\n"
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
            listOf(
                "ISO8583 Transaction" to Icons.Default.CompareArrows,
                "Logs" to Icons.Default.Article,
                "Settings" to Icons.Default.Settings,
                "ISO8583 Template" to Icons.Default.Code,
                "Unsolicited Message" to Icons.Default.Send
            ).forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                title,
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
            when (selectedTabIndex) {
                0 -> ISO8583TransactionTab(
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

                1 -> LogTab(
                    logText = logText,
                    onClearClick = { logText = "" },
                    connectionCount = connectionCount.get(),
                    bytesIncoming = bytesIncoming.get().toLong(),
                    bytesOutgoing = bytesOutgoing.get().toLong(),
                    concurrentConnections = concurrentConnections
                )

                2 -> {
                    ISO8583SettingsScreen(
                        gw = gw.configuration,
                        onSaveClick = onSaveClick
                    )
                }

                3 -> {
                    Iso8583TemplateScreen(
                        config = gw.configuration,
                        onSaveClick = onSaveClick
                    )
                }

                4 -> UnsolicitedMessageTab(
                    gw = gw,
                    logText = logText,
                    onClearClick = { logText = "" },
                )
            }
        }
    }
}

/**
 * ISO8583 Transaction Tab
 */
@Composable
private fun ISO8583TransactionTab(
    isStarted: Boolean,
    onStartStopClick: () -> Unit,
    onClearClick: () -> Unit,
    transactionCount: String,
    isHoldMessage: Boolean,
    onHoldMessageChange: (Boolean) -> Unit,
    holdMessageTime: String,
    onHoldMessageTimeChange: (String) -> Unit,
    waitingRemain: String,
    onSendClick: () -> Unit,
    request: String,
    rawRequest: String,
    response: String,
    rawResponse: String
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Control panel
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Request controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            "REQUEST",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.subtitle2
                        )
                        Text(
                            "Transaction Count: $transactionCount",
                            style = MaterialTheme.typography.caption
                        )
                    }

                    PrimaryButton(
                        text = if (isStarted) "Stop" else "Start",
                        onClick = onStartStopClick,
                        icon = if (isStarted) Icons.Default.Stop else Icons.Default.PlayArrow
                    )

                    SecondaryButton(
                        text = "Clear",
                        onClick = onClearClick,
                        icon = Icons.Default.Clear
                    )
                }

                // Response controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "RESPONSE",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.subtitle2
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isHoldMessage,
                            onCheckedChange = onHoldMessageChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.primary
                            )
                        )
                        Text("Hold Message")
                    }

                    if (waitingRemain != "0") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = WarningYellow.copy(alpha = 0.2f)
                        ) {
                            Text(
                                waitingRemain,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = WarningYellow,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    OutlinedTextField(
                        value = holdMessageTime,
                        onValueChange = onHoldMessageTimeChange,
                        modifier = Modifier.width(60.dp),
                        label = { Text("Seconds") },
                        singleLine = true
                    )

                    SecondaryButton(
                        text = "Send",
                        onClick = onSendClick,
                        icon = Icons.Default.Send
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Request/Response area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Request column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Formatted request
                Surface(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth(),
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Text(
                            "Formatted Request",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.primary
                        )

                        val scrollState = rememberScrollState()
                        TextField(
                            value = request,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .background(Color.Transparent),
                        )
                    }
                }

                // Raw request
                Surface(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth(),
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Text(
                            "Raw Request",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.primary
                        )

                        val scrollState = rememberScrollState()
                        TextField(
                            value = rawRequest,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .background(Color.Transparent),
                        )
                    }
                }
            }

            // Response column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Formatted response
                Surface(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth(),
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Text(
                            "Formatted Response",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.secondary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.secondary
                        )

                        val scrollState = rememberScrollState()
                        TextField(
                            value = response,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .background(Color.Transparent),
                        )
                    }
                }

                // Raw response
                Surface(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth(),
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Text(
                            "Raw Response",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.secondary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.secondary
                        )

                        val scrollState = rememberScrollState()

                        TextField(
                            value = rawResponse,
                            readOnly = true,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .background(Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}


/**
 * Enhanced Log Tab with Auto-Scroll functionality
 */
@Composable
private fun LogTab(
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
private fun LogPanelWithAutoScroll(
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


/**
 * Enhanced Unsolicited Message Tab with animated log panel transition
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun UnsolicitedMessageTab(
    gw: GatewayServiceImpl,
    logText: String,
    onClearClick: () -> Unit = {},
) {
    var rawMessageBytes by remember { mutableStateOf(byteArrayOf()) }
    var rawMessageString by remember { mutableStateOf("") }
    var parsedMessageCreated by remember { mutableStateOf("") }
    var showCreateIsoDialog by remember { mutableStateOf(false) }
    var savedMessages =
        remember { gw.configuration.simulatedTransactionsToDest.toMutableStateList() }
    var selectedMessage by remember { mutableStateOf<Transaction?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var currentMessage by remember { mutableStateOf<Iso8583Data?>(null) }

    // Animation state for panel transition
    var showLogPanel by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - Message content (expanded)
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Raw message input
                Surface(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Raw Message (Hex)",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.primary
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { showCreateIsoDialog = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Create ISO8583 Message",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colors.primary
                                    )
                                }

                                if (rawMessageString.isNotEmpty()) {
                                    IconButton(
                                        onClick = { showSaveDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Save,
                                            contentDescription = "Save Message",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colors.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            rawMessageString = ""
                                            parsedMessageCreated = ""
                                            rawMessageBytes = byteArrayOf()
                                            selectedMessage = null
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Clear Message",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colors.primary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showInfoDialog = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Information",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colors.primary
                                    )
                                }
                            }
                        }

                        TextField(
                            value = rawMessageString,
                            onValueChange = { rawMessageString = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = MaterialTheme.colors.primary,
                                cursorColor = MaterialTheme.colors.primary
                            )
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryButton(
                        text = "Unpack",
                        onClick = {
                            try {
                                rawMessageBytes = IsoUtil.hexStringToBinary(rawMessageString)
                                val isoData = Iso8583Data(gw.configuration, isFirst = false)
                                isoData.unpack(
                                    rawMessageBytes)
                                parsedMessageCreated = isoData.logFormat()
                            } catch (e: Exception) {
                                gw.resultDialogInterface?.onError {
                                    Text("Error parsing data: ${e.message}")
                                }
                            }
                        },
                        icon = Icons.Default.UnfoldMore,
                        enabled = rawMessageString.isNotEmpty()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    PrimaryButton(
                        text = if (isSending) "Sending..." else "Send Message",
                        onClick = {
                            if (rawMessageString.isNotEmpty()) {
                                // Start sending process
                                isSending = true
                                showLogPanel = true

                                coroutineScope.launch {
                                    try {
                                        // Convert string to bytes
                                        rawMessageBytes = IsoUtil.hexStringToBinary(rawMessageString)

                                        // Create ISO8583 data object
                                        val isoData = Iso8583Data(
                                            config = gw.configuration,
                                            isFirst = false
                                        )
                                        isoData.unpack(rawMessageBytes)

                                        // Update parsed message
                                        parsedMessageCreated = isoData.logFormat()

                                        // Send the message
                                        gw.sendToSecondConnection(isoData)


                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isSending = false
                                    }
                                }
                            }
                        },
                        icon = if (isSending) Icons.Default.Schedule else Icons.Default.Send,
                        enabled = rawMessageString.isNotEmpty() && !isSending
                    )
                }

                // Parsed message output
                Surface(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Text(
                            "Parsed Message",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.secondary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.secondary
                        )

                        val scrollState = rememberScrollState()
                        Text(
                            parsedMessageCreated,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(scrollState),
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
            }

            // Right panel - Animated transition between Saved Messages and Log Panel
            AnimatedContent(
                targetState = showLogPanel,
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight(),
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { if (targetState) it else -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(
                        animationSpec = tween(300)
                    ) with slideOutHorizontally(
                        targetOffsetX = { if (targetState) -it else it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(
                        animationSpec = tween(300)
                    )
                },
                label = "panel_transition"
            ) { showLog ->
                if (showLog) {
                    // Log Panel
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = onClearClick,
                            logText = logText,
                            onBack = {
                                showLogPanel = false
                                isSending = false
                            }

                        )
                    }


                } else {
                    // Saved Messages Panel
                    SavedMessagesPanel(
                        savedMessages = savedMessages,
                        selectedMessage = selectedMessage,
                        onMessageSelected = { item ->
                            selectedMessage = item
                            val message = Iso8583Data(
                                template = item.fields!!.toTypedArray(),
                                config = gw.configuration,
                                isFirst = false,
                            )
                            message.messageType = item.mti
                            rawMessageBytes = message.pack()
                            rawMessageString = IsoUtil.bytesToHexString(rawMessageBytes)
                            parsedMessageCreated = message.logFormat()
                            currentMessage = message
                        },
                        onDeleteMessage = { message ->
                            val messageToDelete = savedMessages.firstOrNull { it.id == message.id }
                            if (selectedMessage?.id == message.id) {
                                selectedMessage = null
                            }
                            savedMessages.remove(messageToDelete)
                            gw.configuration.simulatedTransactionsToDest = savedMessages
                        },
                        onImportMessages = { showImportDialog = true },
                        onExportMessages = { showExportDialog = true }
                    )
                }
            }
        }

        // All your existing dialogs remain the same...
        // ISO8583 Editor Dialog
        if (showCreateIsoDialog) {
            Iso8583EditorDialog(
                initialMessage = currentMessage,
                gw = gw,
                onDismiss = { showCreateIsoDialog = false },
                onConfirm = {
                    showCreateIsoDialog = false
                    rawMessageBytes = it.pack()
                    currentMessage = it
                    rawMessageString = IsoUtil.bcdToString(rawMessageBytes)

                    // Auto-parse the created message
                    try {
                        parsedMessageCreated = it.logFormat()
                    } catch (e: Exception) {
                        parsedMessageCreated = "Error parsing message: ${e.message}"
                    }
                }
            )
        }

        // Save Message Dialog
        if (showSaveDialog && currentMessage != null) {
            SaveMessageDialog(
                bitAttribute = currentMessage!!.bitAttributes,
                mti = currentMessage!!.messageType,
                processingCode = currentMessage!!.bitAttributes.getOrNull(2)?.getValue() ?: "",
                onDismiss = { showSaveDialog = false },
                onSave = { savedMessage ->
                    savedMessages.add(savedMessage)
                    showSaveDialog = false
                    gw.configuration.simulatedTransactionsToDest = savedMessages
                },
            )
        }

        // Import Dialog
        if (showImportDialog) {
            ImportMessagesDialog(
                onDismiss = { showImportDialog = false },
                onImport = { importedMessages ->
                    savedMessages.addAll(importedMessages)
                    showImportDialog = false
                    gw.configuration.simulatedTransactionsToDest = savedMessages
                }
            )
        }

        // Export Dialog
        if (showExportDialog) {
            ExportMessagesDialog(
                messages = savedMessages,
                onDismiss = { showExportDialog = false },
                onExport = {
                    showExportDialog = false
                }
            )
        }

        // Information Dialog
        if (showInfoDialog) {
            InformationDialog(
                onDismiss = { showInfoDialog = false }
            )
        }
    }
}


/**
 * Information Dialog Component
 */
@Composable
fun InformationDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "Unsolicited Messages",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "This tab allows you to construct and send unsolicited ISO8583 messages to clients.",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InformationItem(
                        icon = Icons.Default.Add,
                        title = "Create Messages",
                        description = "Use the '+' icon to build messages with the built-in editor"
                    )

                    InformationItem(
                        icon = Icons.Default.Edit,
                        title = "Manual Entry",
                        description = "Enter raw hexadecimal data directly in the input field"
                    )

                    InformationItem(
                        icon = Icons.Default.Save,
                        title = "Save Messages",
                        description = "Save frequently used messages for quick access"
                    )

                    InformationItem(
                        icon = Icons.Default.ImportExport,
                        title = "Import/Export",
                        description = "Import and export message collections for backup"
                    )

                    InformationItem(
                        icon = Icons.Default.Send,
                        title = "Send Messages",
                        description = "Send unsolicited messages to test client behavior"
                    )
                }

                Text(
                    text = "Saved messages appear in the right panel for quick access and reuse.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Got it")
                    }
                }
            }
        }
    }
}

/**
 * Information Item Component
 */
@Composable
fun InformationItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colors.primary.copy(alpha = 0.7f)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}


/**
 * Saved Messages Panel Component
 */
@Composable
fun SavedMessagesPanel(
    savedMessages: List<Transaction>,
    selectedMessage: Transaction?,
    onMessageSelected: (Transaction) -> Unit,
    onDeleteMessage: (Transaction) -> Unit,
    onImportMessages: () -> Unit,
    onExportMessages: () -> Unit
) {
    Panel(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Saved Messages (${savedMessages.size})",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primary,
                    fontSize = 14.sp
                )

                Row {
                    IconButton(
                        onClick = onImportMessages,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = "Import Messages",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colors.primary
                        )
                    }

                    IconButton(
                        onClick = onExportMessages,
                        modifier = Modifier.size(24.dp),
                        enabled = savedMessages.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Export Messages",
                            modifier = Modifier.size(14.dp),
                            tint = if (savedMessages.isNotEmpty()) MaterialTheme.colors.primary
                            else MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Divider()

            // Messages list
            if (savedMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Message,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            "No saved messages",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Create and save messages to see them here",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(savedMessages.size) { i ->
                        SavedMessageItem(
                            message = savedMessages[i],
                            isSelected = selectedMessage?.id == savedMessages[i].id,
                            onSelect = { onMessageSelected(savedMessages[i]) },
                            onDelete = { onDeleteMessage(savedMessages[i]) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Saved Message Item
 */
@Composable
fun SavedMessageItem(
    message: Transaction,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onSelect() },
        elevation = if (isSelected) 4.dp else 1.dp,
        backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f)
        else MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = message.description,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (message.mti.isNotEmpty()) {
                        Text(
                            text = "MTI: ${message.mti}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }

                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Message",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colors.error
                )
            }
        }

    }
}


/**
 * Save Message Dialog
 */
@Composable
fun SaveMessageDialog(
    bitAttribute: Array<BitAttribute>,
    mti: String,
    processingCode: String,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var messageName by remember { mutableStateOf("") }
    var mti by remember { mutableStateOf(mti) }
    var procCode by remember { mutableStateOf(processingCode) }


    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Save Message",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = messageName,
                    onValueChange = { messageName = it },
                    label = { Text("Message Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row {
                    OutlinedTextField(
                        value = mti,
                        onValueChange = { mti = it },
                        label = { Text("MTI") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    OutlinedTextField(
                        value = procCode,
                        onValueChange = { procCode = it },
                        label = { Text("Processing Code") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val savedMessage = Transaction(
                                id = UUID.randomUUID().toString(),
                                description = messageName,
                                mti = mti,
                                proCode = procCode,
                                fields = bitAttribute.toList()

                            )
                            onSave(savedMessage)
                        },
                        enabled = messageName.isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Import Messages Dialog
 */
@Composable
fun ImportMessagesDialog(
    onDismiss: () -> Unit,
    onImport: (List<Transaction>) -> Unit
) {
    var importedContent by remember { mutableStateOf("") }
    var importedParsedContent by remember { mutableStateOf<List<Transaction>?>(null) }
    val coroutineScope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Import Messages",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Paste JSON content or select a file to import saved messages.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = "Choose File",
                        onClick = {

                            coroutineScope.launch {
                                val fileChooser = JFileChooser().apply {
                                    fileSelectionMode = JFileChooser.FILES_ONLY
                                    dialogTitle = "Select Configuration File"
                                    currentDirectory = File(System.getProperty("user.home"))

                                    // Add file filters
                                    addChoosableFileFilter(
                                        FileNameExtensionFilter(
                                            "Json files",
                                            "json"
                                        )
                                    )
                                }

                                val result = fileChooser.showOpenDialog(null)
                                val file = if (result == JFileChooser.APPROVE_OPTION) {
                                    fileChooser.selectedFile
                                } else {
                                    null
                                }
                                file?.let {
                                    val content = it.readText()
                                    importedParsedContent = parseImportedMessages(content)
                                    importedContent = content

                                }
                            }


                        },
                        icon = Icons.Default.FileOpen,
                        modifier = Modifier.weight(1f)
                    )

                    SecondaryButton(
                        text = "Clear",
                        onClick = { importedContent = "" },
                        icon = Icons.Default.Clear,
                        modifier = Modifier.weight(1f),
                        enabled = importedContent.isNotEmpty()
                    )
                }

                OutlinedTextField(
                    value = importedContent,
                    onValueChange = { importedContent = it },
                    label = { Text("Json Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            try {


                                importedParsedContent?.let { onImport(it) }
                            } catch (e: Exception) {
                                // Show error
                                println("Error selecting file: ${e.message}")
                            }
                        },
                        enabled = importedContent.isNotEmpty()
                    ) {
                        Text("Import")
                    }
                }
            }
        }
    }
}

/**
 * Export Messages Dialog
 */
@Composable
fun ExportMessagesDialog(
    messages: List<Transaction>,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    val exportContent = remember { generateExportContent(messages) }
    val coroutineScope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Export Messages",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Copy the content below or save it to a file to backup your saved messages.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = "Copy to Clipboard",
                        onClick = {
                            // Copy to clipboard logic
                        },
                        icon = Icons.Default.ContentCopy,
                        modifier = Modifier.weight(1f)
                    )

                    SecondaryButton(
                        text = "Save to File",
                        onClick = {
                            // Save to file logic
                            coroutineScope.launch {
                                try {
                                    val fileChooser = JFileChooser().apply {
                                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                        dialogTitle = "Select Export Directory"
                                        currentDirectory = File(System.getProperty("user.home"))
                                    }

                                    val result = fileChooser.showOpenDialog(null)
                                    val directoryPath = if (result == JFileChooser.APPROVE_OPTION) {
                                        fileChooser.selectedFile.absolutePath
                                    } else {
                                        null
                                    }

                                    val content = generateExportContent(messages)
                                    val fileName = "messages.json"
                                    val filePath = File(directoryPath, fileName)

                                    filePath.writeText(content)
                                    onExport()
                                } catch (e: Exception) {
                                    println("Error selecting directory: ${e.message}")
                                }
                            }


                        },
                        icon = Icons.Default.Save,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = exportContent,
                    onValueChange = { },
                    label = { Text("Export Content (${messages.size} messages)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    readOnly = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

// Helper functions
private fun parseImportedMessages(content: String): List<Transaction> {
    // Implementation for parsing JSON content to SavedMessage list
    // This would use your preferred JSON library
    return Json.decodeFromString(content)
}

private fun generateExportContent(messages: List<Transaction>): String {
    return Json.encodeToString(messages)
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