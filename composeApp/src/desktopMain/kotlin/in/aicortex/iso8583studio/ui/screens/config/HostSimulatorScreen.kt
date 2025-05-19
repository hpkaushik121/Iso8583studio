package `in`.aicortex.iso8583studio.ui.screens.config

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random



/**
 * A Kotlin Composable implementation of the Host Simulator
 * Inspired by the decompiled C# version from TI.SecurityGateway.FormHostSimulator
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HostSimulator(
    gw: GatewayServiceImpl,
    onSaveClick: () -> Unit = {}
) {
    var isStarted by remember { mutableStateOf(false) }
    var transactionCount by remember { mutableStateOf(0) }
    var bytesOutgoing by remember { mutableStateOf(0) }
    var bytesIncoming by remember { mutableStateOf(0) }
    var connectionCount by remember { mutableStateOf(0) }
    var isHoldMessage by remember { mutableStateOf(false) }
    var holdMessageTime by remember { mutableStateOf("0") }
    var useAscii by remember { mutableStateOf(false) }
    var dontUseTPDU by remember { mutableStateOf(false) }
    var respondIfDontRecognize by remember { mutableStateOf(false) }
    var useCustomizedMessage by remember { mutableStateOf(false) }
    var isMetfoneMessage by remember { mutableStateOf(false) }
    var notUpdateScreen by remember { mutableStateOf(false) }
    var ignoreHeaderLength by remember { mutableStateOf("5") }
    var fixedResponseHeader by remember { mutableStateOf("") }
    var bitNumber by remember { mutableStateOf("88") }

    var rawRequest by remember { mutableStateOf("") }
    var request by remember { mutableStateOf("") }
    var rawResponse by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var logText by remember { mutableStateOf("") }

    var waitingRemain by remember { mutableStateOf("0") }
    var sendHoldMessage by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Simulating the timer_tick effect
    gw.onReceiveFromSource{ client,request ->
        rawRequest = IsoUtil.bytesToHexString(request)

        return@onReceiveFromSource request
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
                if (!sendHoldMessage) {
                    // Would send the message here in real implementation
                    sendHoldMessage = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Text("Host Simulator - ${gw.configuration.name}")

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
                listOf("Iso8583 TXN", "Log", "Settings", "Iso8583 Template", "Unit Test", "Unsolicited Message").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTabIndex) {
                0 -> {
                    // Iso8583 TXN Tab
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
                                        coroutineScope.launch {
                                            if(!isStarted){
                                                gw.start()
                                                isStarted = true
                                            }else{
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
                                    onCheckedChange = { isHoldMessage = it }
                                )
                                Text("Hold Message")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(waitingRemain)
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = holdMessageTime,
                                    onValueChange = { holdMessageTime = it },
                                    modifier = Modifier.width(60.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { sendHoldMessage = true }
                                ) {
                                    Text("Send")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Request/Response area
                        Row(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.weight(1f)) {
                                TextField(
                                    value = request,
                                    onValueChange = { /* read-only */ },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.62f),
                                    readOnly = true,
                                    label = { Text("Request") }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextField(
                                    value = rawRequest,
                                    onValueChange = { /* read-only */ },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.38f),
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
                                        .weight(0.62f),
                                    readOnly = true,
                                    label = { Text("Response") }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextField(
                                    value = rawResponse,
                                    onValueChange = { /* read-only */ },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.38f),
                                    readOnly = true,
                                    label = { Text("Raw Response") }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Log Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        TextField(
                            value = logText,
                            onValueChange = { /* read-only */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            readOnly = true
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
                                        StatisticRow("Connections:", connectionCount.toString())
                                        StatisticRow("Concurrent Connections:", "0")
                                        StatisticRow("Authentications fail:", "0")
                                        StatisticRow("Successful trans:", "0")
                                    }

                                    Column {
                                        StatisticRow("Bytes transferred from Source:", bytesIncoming.toString())
                                        StatisticRow("Bytes transferred from Destination:", bytesOutgoing.toString())
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
                2 -> {
                    // Settings Tab (Known Transactions)
                    ISO8583SettingsScreen(gw = gw)
                    // This would contain the dataGridView for KnownTransaction and FieldsResponded
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

                                Iso8583TemplateScreen(config = gw.configuration)
                                // Property grid would go here in a real implementation
                            }
                        }
                    }
                }
                4 -> {
                    // Unit Test Tab
                    Text("Unit Test Tab - Test Configuration and Results would go here")
                }
                5 -> {
                    var rawMessageBytes by remember { mutableStateOf(byteArrayOf()) }
                    var rawMessageString by remember { mutableStateOf("") }
                    var parsedMessageCreated by remember { mutableStateOf("") }
                    // Unsolicited Message Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxSize()) {
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
                                            rawMessageBytes = IsoUtil.stringToBcd(rawMessageString)
                                            val isoData = Iso8583Data(gw.configuration)
                                            isoData.unpack(rawMessageBytes)
                                            parsedMessageCreated = isoData.logFormat()
                                        }
                                    ) {
                                        Text("Unpack")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = false,
                                            onCheckedChange = { /* ANSI checkbox logic */ }
                                        )
                                        Text("ANSI")
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Text("Message Length Type")

                                    Spacer(modifier = Modifier.width(8.dp))

                                    var lengthTypeExpanded by remember { mutableStateOf(false) }
                                    var selectedLengthType by remember { mutableStateOf("BCD") }

                                    ExposedDropdownMenuBox(
                                        expanded = lengthTypeExpanded,
                                        onExpandedChange = { lengthTypeExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedLengthType,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lengthTypeExpanded) },
                                            modifier = Modifier
                                                .width(100.dp)
                                        )

                                        ExposedDropdownMenu(
                                            expanded = lengthTypeExpanded,
                                            onDismissRequest = { lengthTypeExpanded = false }
                                        ) {
                                            listOf("BCD", "High Low", "Low High", "None").forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        selectedLengthType = option
                                                        lengthTypeExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = false,
                                        onCheckedChange = { /* Smartlink Template checkbox logic */ }
                                    )
                                    Text("SmartlinkMessage")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextField(
                                    value = parsedMessageCreated,
                                    onValueChange = { /* Logic for parsedText */ },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    readOnly = true,
                                    label = { Text("Parsed Text") }
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))
                            var showCreateIsoDialog by remember { mutableStateOf(false) }

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

                            if(showCreateIsoDialog){
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
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.Gray)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            HostSimulator(
                gw = GatewayServiceImpl(config!!),
                onSaveClick = {

                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text("Back to Configuration")
        }
    }
}
