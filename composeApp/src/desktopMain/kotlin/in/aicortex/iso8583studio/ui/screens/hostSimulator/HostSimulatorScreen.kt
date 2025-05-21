package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import `in`.aicortex.iso8583studio.data.rememberIsoCoroutineScope
import kotlinx.coroutines.delay

/**
 * A Kotlin Composable implementation of the Host Simulator
 * Inspired by the decompiled C# version from TI.SecurityGateway.FormHostSimulator
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HostSimulator(
    gw: GatewayServiceImpl,
    onSaveClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var isStarted by remember { mutableStateOf(false) }
    var transactionCount by remember { mutableStateOf("0") }
    var bytesOutgoing by remember { mutableStateOf(gw.bytesOutgoing) }
    var bytesIncoming by remember { mutableStateOf(gw.bytesIncoming) }
    var connectionCount by remember { mutableStateOf(gw.connectionCount) }
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

    // Simulating the timer_tick effect
    gw.onReceiveFromSource { client, request ->
        rawRequest = IsoUtil.bytesToHexString(request)
        return@onReceiveFromSource request
    }

    gw.onReceivedFormattedData {
        request = it?.logFormat() ?: ""
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

    // Effect for hold message countdown
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
                    // Would send the message here in real implementation
                    sendHoldMessage = true
                    gw.sendHoldMessage?.invoke()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Row (
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ){
                Box (
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.primary)
                        .clickable(
                            onClick = onBack
                        ).padding(8.dp)
                ) {
                    Image(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "go Back"
                    )

                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Host Simulator - ${gw.configuration.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }

        }
    ) { paddingValues ->
        var selectedTabIndex by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                listOf(
                    "Iso8583 TXN",
                    "Log",
                    "Settings",
                    "Iso8583 Template",
                    "Unsolicited Message"
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content area with scrolling for each tab
            // Using a Box with verticalScroll for each tab content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Create scroll state for each tab
                val scrollState = rememberScrollState()

                when (selectedTabIndex) {
                    0 -> {
                        // Iso8583 TXN Tab
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Control panel
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "REQUEST",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Count: $transactionCount")
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Button(
                                            onClick = {
                                                coroutineScope.launchSafely {
                                                    if (!isStarted) {
                                                        gw.start()
                                                        isStarted = true
                                                    } else {
                                                        gw.stop()
                                                        isStarted = false
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(if (isStarted) "Stop" else "Start")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                rawRequest = ""
                                                request = ""
                                                rawResponse = ""
                                                response = ""
                                            }
                                        ) {
                                            Text("Clear")
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "RESPONSE",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Checkbox(
                                            checked = isHoldMessage,
                                            onCheckedChange = {
                                                isHoldMessage = it
                                                gw.holdMessage = it
                                            }
                                        )
                                        Text("Hold Message")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (waitingRemain != "0") {
                                            Text(waitingRemain)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedTextField(
                                            value = holdMessageTime,
                                            onValueChange = { holdMessageTime = it },
                                            modifier = Modifier.width(60.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                sendHoldMessage = true
                                                waitingRemain = "0"
                                                coroutineScope.launchSafely {
                                                    gw.sendHoldMessage?.invoke()
                                                }
                                            }
                                        ) {
                                            Text("Send")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Request/Response area
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        TextField(
                                            value = request,
                                            onValueChange = { /* read-only */ },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(300.dp),
                                            readOnly = true,
                                            label = { Text("Request") }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextField(
                                            value = rawRequest,
                                            onValueChange = { /* read-only */ },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            readOnly = true,
                                            label = { Text("Raw Request") }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        TextField(
                                            value = response,
                                            onValueChange = { /* read-only */ },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(300.dp),
                                            readOnly = true,
                                            label = { Text("Response") }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextField(
                                            value = rawResponse,
                                            onValueChange = { /* read-only */ },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            readOnly = true,
                                            label = { Text("Raw Response") }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Log Tab
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                TextField(
                                    value = logText,
                                    onValueChange = { /* read-only */ },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(500.dp),
                                    readOnly = true,
                                    label = { Text("Log") }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Statistics section
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text("Statistics", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                StatisticRow(
                                                    "Connections:",
                                                    connectionCount.toString()
                                                )
                                                StatisticRow("Concurrent Connections:", "0")
                                                StatisticRow("Authentications fail:", "0")
                                                StatisticRow("Successful trans:", "0")
                                            }

                                            Column {
                                                StatisticRow(
                                                    "Bytes transferred from Source:",
                                                    bytesIncoming.toString()
                                                )
                                                StatisticRow(
                                                    "Bytes transferred from Destination:",
                                                    bytesOutgoing.toString()
                                                )
                                            }

                                            Button(
                                                onClick = { logText = "" }
                                            ) {
                                                Text("Clear")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Settings Tab (Known Transactions)

                        ISO8583SettingsScreen(
                            gw = gw,
                            onSaveClick = onSaveClick
                        )

                    }

                    3 -> {
                        // Iso8583 Template Tab

                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // PropertyGrid for Bit Template
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    Iso8583TemplateScreen(
                                        config = gw.configuration,
                                        onSaveClick = onSaveClick
                                    )
                                }
                            }
                        }

                    }

//                    4 -> {
//                        // Unit Test Tab
//                        Box(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .verticalScroll(scrollState)
//                        ) {
//                            Text("Unit Test Tab - Test Configuration and Results would go here")
//                        }
//                    }

                    4 -> {
                        // Unsolicited Message Tab
                        var rawMessageBytes by remember { mutableStateOf(byteArrayOf()) }
                        var rawMessageString by remember { mutableStateOf("") }
                        var parsedMessageCreated by remember { mutableStateOf("") }
                        var showCreateIsoDialog by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        TextField(
                                            value = rawMessageString,
                                            onValueChange = {
                                                rawMessageString = it
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            label = { Text("Raw Message") }
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = {
                                                    try {
                                                        rawMessageBytes =
                                                            IsoUtil.stringToBcd(
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

                                                }
                                            ) {
                                                Text("Unpack")
                                            }

//                                            Spacer(modifier = Modifier.width(8.dp))
//
//                                            Row(verticalAlignment = Alignment.CenterVertically) {
//                                                Checkbox(
//                                                    checked = false,
//                                                    onCheckedChange = { /* ANSI checkbox logic */ }
//                                                )
//                                                Text("ANSI")
//                                            }
//
//                                            Spacer(modifier = Modifier.width(16.dp))

//                                            Text("Message Length Type")
//
//                                            Spacer(modifier = Modifier.width(8.dp))
//
//                                            var lengthTypeExpanded by remember { mutableStateOf(false) }
//                                            var selectedLengthType by remember { mutableStateOf("BCD") }
//
//                                            ExposedDropdownMenuBox(
//                                                expanded = lengthTypeExpanded,
//                                                onExpandedChange = { lengthTypeExpanded = it }
//                                            ) {
//                                                OutlinedTextField(
//                                                    value = selectedLengthType,
//                                                    onValueChange = {},
//                                                    readOnly = true,
//                                                    trailingIcon = {
//                                                        ExposedDropdownMenuDefaults.TrailingIcon(
//                                                            expanded = lengthTypeExpanded
//                                                        )
//                                                    },
//                                                    modifier = Modifier
//                                                        .width(100.dp)
//                                                )
//
//                                                ExposedDropdownMenu(
//                                                    expanded = lengthTypeExpanded,
//                                                    onDismissRequest = { lengthTypeExpanded = false }
//                                                ) {
//                                                    listOf(
//                                                        "BCD",
//                                                        "High Low",
//                                                        "Low High",
//                                                        "None"
//                                                    ).forEach { option ->
//                                                        DropdownMenuItem(
//                                                            text = { Text(option) },
//                                                            onClick = {
//                                                                selectedLengthType = option
//                                                                lengthTypeExpanded = false
//                                                            }
//                                                        )
//                                                    }
//                                                }
//                                            }
                                        }

//                                        Spacer(modifier = Modifier.height(8.dp))
//
//                                        Row(verticalAlignment = Alignment.CenterVertically) {
//                                            Checkbox(
//                                                checked = false,
//                                                onCheckedChange = { /* Smartlink Template checkbox logic */ }
//                                            )
//                                            Text("SmartlinkMessage")
//                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        TextField(
                                            value = parsedMessageCreated,
                                            onValueChange = { /* Logic for parsedText */ },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(300.dp),
                                            readOnly = true,
                                            label = { Text("Parsed Text") }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(
                                        modifier = Modifier.width(140.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Button(
                                            onClick = { showCreateIsoDialog = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Create ISO8583 message")
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = { /* Send unsolicited message logic */ },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Send")
                                        }
                                    }
                                }

                                if (showCreateIsoDialog) {
                                    Iso8583EditorDialog(
                                        gw = gw,
                                        onDismiss = {
                                            showCreateIsoDialog = false
                                        },
                                        onConfirm = {
                                            showCreateIsoDialog = false
                                            rawMessageBytes = it.pack()
                                            rawMessageString = IsoUtil.bcdToString(rawMessageBytes)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(label, modifier = Modifier.width(160.dp))
        Text(value)
    }
}

/**
 * Host Simulator screen
 */
@Composable
fun HostSimulatorScreen(
    config: GatewayConfig?,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
    onError: ResultDialogInterface? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            val gw = GatewayServiceImpl(config!!)
            if (onError != null) {
                gw.setShowErrorListener(onError)
            }
            HostSimulator(
                gw = gw,
                onSaveClick = onSaveClick,
                onBack = onBack
            )
        }
    }
}