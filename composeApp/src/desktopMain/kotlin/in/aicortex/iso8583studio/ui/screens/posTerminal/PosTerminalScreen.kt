package `in`.aicortex.iso8583studio.ui.screens.posTerminal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.navigation.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// --- DATA & SERVICE LAYER FOR POS SIMULATOR ---

// Dedicated service for POS client logic
class POSSimulatorService(
    private val isoConfig: POSSimulatorConfig // For packing/unpacking messages
) {
    var hostAddress by mutableStateOf("127.0.0.1")
    var hostPort by mutableStateOf(8080)

    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var isConnected by mutableStateOf(false)
        private set

    // --- UI Callbacks ---
    var onLog: (LogEntry) -> Unit = {}
    var onRequestSent: (String) -> Unit = {}
    var onResponseReceived: (String) -> Unit = {}
    var onConnectionStateChange: (Boolean) -> Unit = {}




    private fun createLog(type: LogType, message: String, details: String? = null): LogEntry {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        return LogEntry(timestamp, type, message, details)
    }
}

private enum class POSTerminalSimulatorTabs(val title: String, val icon: ImageVector) {
    TRANSACTIONS("Transactions", Icons.Default.SwapHoriz),
    LOGS("Logs", Icons.Default.Article),
    SETTINGS("Settings", Icons.Default.Settings),
    TEMPLATE("Template", Icons.Default.Code),
}

// --- MAIN SCREEN & UI COMPOSABLES ---

@Composable
fun POSTerminalSimulatorScreen(
    window: ComposeWindow, // Assuming this is needed for file dialogs etc.
    config: POSSimulatorConfig?, // This now defines the ISO8583 message structure
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
) {
    if (config == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No POS Terminal configuration selected")
        }
        return
    }

    // Initialize the dedicated service for the POS terminal
    val posService = remember { POSSimulatorService(config) }


    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "POS Terminal Simulator - ${config.name}",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = onSaveClick) {
                        Icon(Icons.Default.Save, contentDescription = "Save Configuration")
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        POSTerminalSimulator(
            posService = posService,
            isoConfig = config,
            onSaveClick = onSaveClick,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun POSTerminalSimulator(
    posService: POSSimulatorService,
    isoConfig: POSSimulatorConfig,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isConnected by remember { mutableStateOf(posService.isConnected) }
    var request by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    val logEntries = remember { mutableStateListOf<LogEntry>() }

    // Wire up service callbacks to UI state
    posService.onConnectionStateChange = { isConnected = it }
    posService.onRequestSent = { request = it }
    posService.onResponseReceived = { response = it }
    posService.onLog = { logEntries.add(0, it) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = POSTerminalSimulatorTabs.values().toList()

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabList.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                    text = { Text(tab.title) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (tabList[selectedTabIndex]) {
                POSTerminalSimulatorTabs.TRANSACTIONS -> POSTransactionTab(
                    posService = posService,
                    isoConfig = isoConfig,
                    isConnected = isConnected,
                    request = request,
                    response = response,
                    onClearClick = { request = ""; response = "" }
                )
                POSTerminalSimulatorTabs.LOGS -> LogTab(
                    logEntries = logEntries,
                    onClearClick = { logEntries.clear() },
                    connectionCount = if (isConnected) 1 else 0,
                    concurrentConnections = if (isConnected) 1 else 0,
                    bytesIncoming = 0, // Simplified for this example
                    bytesOutgoing = 0, // Simplified for this example
                )
                POSTerminalSimulatorTabs.SETTINGS -> POSSettingsTab(
                    posService = posService,
                    isoConfig = isoConfig,
                    onSaveClick = onSaveClick
                )
                POSTerminalSimulatorTabs.TEMPLATE -> Text("PENDING") /*Iso8583TemplateScreen(
                    config = isoConfig,
                    onSaveClick = onSaveClick
                )*/
            }
        }
    }
}

@Composable
fun POSTransactionTab(
    posService: POSSimulatorService,
    isoConfig: POSSimulatorConfig,
    isConnected: Boolean,
    request: String,
    response: String,
    onClearClick: () -> Unit
) {
    val transactions = remember { isoConfig.simulatedTransactionsToDest.toMutableStateList() }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Control Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {  },
                    colors = ButtonDefaults.buttonColors(backgroundColor = if (isConnected) MaterialTheme.colors.error else SuccessGreen)
                ) {
                    Icon(if (isConnected) Icons.Default.PowerOff else Icons.Default.Power, contentDescription = "Connect/Disconnect", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isConnected) "Disconnect" else "Connect", color = Color.White)
                }
                OutlinedButton(onClick = onClearClick) { Text("Clear Displays") }
            }
            if (isConnected) {
                StatusIndicator(text = "Connected to ${posService.hostAddress}:${posService.hostPort}", color = SuccessGreen, icon = Icons.Default.CheckCircle)
            } else {
                StatusIndicator(text = "Disconnected", color = Color.Gray, icon = Icons.Default.Cancel)
            }
        }

        // Main content area
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left Panel: Transaction List & Send Button
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column {
                        Text("Select Transaction", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.h6)
                        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(transactions) { transaction ->
                                TransactionListItem(
                                    transaction = transaction,
                                    isSelected = selectedTransaction?.id == transaction.id,
                                    onClick = { selectedTransaction = transaction }
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        selectedTransaction?.let {
                            coroutineScope.launch {
                                isSending = true
                                delay(500) // UI feedback delay
                                isSending = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = isConnected && selectedTransaction != null && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send Transaction")
                        Spacer(Modifier.width(8.dp))
                        Text("Send Transaction")
                    }
                }
            }

            // Right Panel: Request and Response
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Panel(modifier = Modifier.weight(1f)) {
                    Text("Request Sent", fontWeight = FontWeight.Bold)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        SelectionContainer { Text(request, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    }
                }
                Panel(modifier = Modifier.weight(1f)) {
                    Text("Response Received", fontWeight = FontWeight.Bold)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        SelectionContainer { Text(response, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun POSSettingsTab(posService: POSSimulatorService, isoConfig: POSSimulatorConfig, onSaveClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- Connection Settings ---
        var address by remember { mutableStateOf(posService.hostAddress) }
        var port by remember { mutableStateOf(posService.hostPort.toString()) }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Host Connection Settings", style = MaterialTheme.typography.h6)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Host IP Address") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !posService.isConnected
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Host Port") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !posService.isConnected
                )
                Button(
                    onClick = {
                        posService.hostAddress = address
                        posService.hostPort = port.toIntOrNull() ?: 8080
                    },
                    enabled = !posService.isConnected
                ) {
                    Text("Apply Connection Settings")
                }
            }
        }

        // --- Transaction Template Management ---
        // For simplicity, we pass the GatewayConfig to the existing settings screen.
        // A more advanced implementation would have a dedicated settings screen for POS transactions.
//        ISO8583SettingsScreen(gw = GatewayServiceImpl(isoConfig), onSaveClick = onSaveClick)
    }
}

@Composable
fun TransactionListItem(transaction: Transaction, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = if (isSelected) 4.dp else 1.dp,
        backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else MaterialTheme.colors.surface,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colors.primary) else null
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(transaction.description, fontWeight = FontWeight.Bold)
                Text("MTI: ${transaction.mti}", style = MaterialTheme.typography.caption)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colors.primary)
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
