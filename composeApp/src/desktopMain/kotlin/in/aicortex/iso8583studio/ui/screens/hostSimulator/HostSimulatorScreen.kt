package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.animation.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.rememberIsoCoroutineScope
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import `in`.aicortex.iso8583studio.ui.components.*
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var gw : GatewayServiceImpl? = null
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
@Composable
fun HostSimulator(
    gw: GatewayServiceImpl,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isStarted by remember { mutableStateOf(false) }
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
    gw.onReceiveFromSource { client, request ->
        rawRequest = IsoUtil.bytesToHexString(request)
        rawResponse = ""
        return@onReceiveFromSource request
    }

    gw.onReceivedFormattedData {
        request = it?.logFormat() ?: ""
        response = ""
    }

    gw.onSentFormattedData { iso, byte ->
        response = iso?.logFormat() ?: ""
        rawResponse = IsoUtil.bytesToHexString(byte ?: byteArrayOf())
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
                    gw = gw
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
                            modifier =  Modifier
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
                            modifier =  Modifier
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
                            modifier =  Modifier
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
                            modifier =  Modifier
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
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope() // Add this for better lifecycle management
    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    var userHasScrolled by remember { mutableStateOf(false) }
    var previousLogLength by remember { mutableStateOf(0) }

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
            val isAtBottom = scrollState.value >= scrollState.maxValue - 100 // 100px tolerance for better detection

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


/**
 * Unsolicited Message Tab
 */
@Composable
private fun UnsolicitedMessageTab(
    gw: GatewayServiceImpl
) {
    var rawMessageBytes by remember { mutableStateOf(byteArrayOf()) }
    var rawMessageString by remember { mutableStateOf("") }
    var parsedMessageCreated by remember { mutableStateOf("") }
    var showCreateIsoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - Message content
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
                        Text(
                            "Raw Message (Hex)",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                                .padding(8.dp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.primary
                        )

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
                                rawMessageBytes = IsoUtil.stringToBcd(
                                    rawMessageString,
                                    rawMessageString.length / 2
                                )
                                val isoData = Iso8583Data(gw.configuration)
                                isoData.unpack(
                                    rawMessageBytes,
                                    2,
                                    rawMessageBytes.size - 2
                                )
                                parsedMessageCreated = isoData.logFormat()
                            } catch (e: Exception) {
                                gw.resultDialogInterface?.onError {
                                    Text("Error parsing data: ${e.message}")
                                }
                            }
                        },
                        icon = Icons.Default.UnfoldMore
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    SecondaryButton(
                        text = "Create ISO8583 Message",
                        onClick = { showCreateIsoDialog = true },
                        icon = Icons.Default.Add
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

            // Right panel - Actions and info
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Message actions panel
                Panel(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SectionHeader(title = "Message Actions")

                        PrimaryButton(
                            text = "Send Message",
                            onClick = { /* Send message logic */ },
                            icon = Icons.Default.Send,
                            modifier = Modifier.fillMaxWidth()
                        )

                        SecondaryButton(
                            text = "Load from File",
                            onClick = { /* Load from file logic */ },
                            icon = Icons.Default.FileOpen,
                            modifier = Modifier.fillMaxWidth()
                        )

                        SecondaryButton(
                            text = "Save to File",
                            onClick = { /* Save to file logic */ },
                            icon = Icons.Default.Save,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Help panel
                Panel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SectionHeader(title = "Information")

                        val scrollState = rememberScrollState()
                        Text(
                            "This tab allows you to construct and send unsolicited ISO8583 messages to clients. Use the 'Create ISO8583 Message' button to build a message using the built-in editor, or enter raw hexadecimal data in the input field.\n\n" +
                                    "Unsolicited messages are useful for testing client behavior when receiving unexpected messages from the server.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(scrollState),
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        if (showCreateIsoDialog) {
            Iso8583EditorDialog(
                gw = gw,
                onDismiss = { showCreateIsoDialog = false },
                onConfirm = {
                    showCreateIsoDialog = false
                    rawMessageBytes = it.pack()
                    rawMessageString = IsoUtil.bcdToString(rawMessageBytes)
                }
            )
        }
    }
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