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
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.POSSimulatorService
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.navigation.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.ISO8583SettingsScreen
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.Iso8583TemplateScreen
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogTab
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.Transaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Aligned Enum with Icons, matching the HostSimulator theme
private enum class POSTerminalSimulatorTabs(val title: String, val icon: ImageVector) {
    TRANSACTIONS("Transactions", Icons.Default.SwapHoriz),
    LOGS("Logs", Icons.Default.Article),
    SETTINGS("Settings", Icons.Default.Settings),
    TEMPLATE("Template", Icons.Default.Code),
}

// --- MAIN SCREEN & UI COMPOSABLES ---

@Composable
fun POSTerminalSimulatorScreen(
    window: ComposeWindow,
    config: POSSimulatorConfig?,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
) {
    if (config == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No POS Terminal configuration selected")
        }
        return
    }

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
    val coroutineScope = rememberCoroutineScope()

    // Wire up service callbacks to UI state
    posService.onConnectionStateChange = { isConnected = it }
    posService.onRequestSent = { request = it }
    posService.onResponseReceived = { response = it }
    posService.onLog = { logEntries.add(0, it) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = POSTerminalSimulatorTabs.values().toList()

    Column(modifier = modifier.fillMaxSize()) {
        // TabRow aligned with HostSimulator's style
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
                                contentDescription = tab.title,
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

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (tabList[selectedTabIndex]) {
                POSTerminalSimulatorTabs.TRANSACTIONS -> POSTransactionTab(
                    posService = posService,
                    isConnected = isConnected,
                    onConnectDisconnect = {
                        coroutineScope.launch {
//                            if (posService.isConnected) posService.disconnect() else posService.connect()
                        }
                    },
                    onTransactionSend = { transaction ->
                        coroutineScope.launch {
//                            posService.sendTransaction(transaction)
                        }
                    },
                    request = request,
                    response = response,
                    onClearClick = { request = ""; response = "" },
                    isoConfig = isoConfig
                )
                // Re-using the LogTab composable from hostSimulator for consistency
                POSTerminalSimulatorTabs.LOGS -> LogTab(
                    logEntries = logEntries,
                    onClearClick = { logEntries.clear() },
                    connectionCount = if (isConnected) 1 else 0,
                    concurrentConnections = if (isConnected) 1 else 0,
                    bytesIncoming = 0, // Simplified for POS client
                    bytesOutgoing = 0, // Simplified for POS client
                )
                POSTerminalSimulatorTabs.SETTINGS -> POSSettingsTab(
                    posService = posService,
                    onSaveClick = onSaveClick
                )
                // Re-using the Iso8583TemplateScreen for consistency
                POSTerminalSimulatorTabs.TEMPLATE -> Text("Pending") /*Iso8583TemplateScreen(
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
    isConnected: Boolean,
    onConnectDisconnect: () -> Unit,
    onTransactionSend: (Transaction) -> Unit,
    request: String,
    response: String,
    onClearClick: () -> Unit,
    isoConfig: POSSimulatorConfig
) {
    val transactions = remember { isoConfig.simulatedTransactionsToDest.toMutableStateList() }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onConnectDisconnect,
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

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Panel(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text("Select Transaction", fontWeight = FontWeight.Bold)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(4.dp)) {
                        items(transactions) { transaction ->
                            TransactionListItem(
                                transaction = transaction,
                                isSelected = selectedTransaction?.id == transaction.id,
                                onClick = { selectedTransaction = transaction }
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        selectedTransaction?.let {
                            coroutineScope.launch {
                                isSending = true
                                onTransactionSend(it)
                                delay(500) // Visual feedback
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

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Panel(modifier = Modifier.weight(1f)) {
                    Text("Request Sent", fontWeight = FontWeight.Bold)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    SelectionContainer { Text(request, modifier = Modifier.verticalScroll(rememberScrollState()), fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                }
                Panel(modifier = Modifier.weight(1f)) {
                    Text("Response Received", fontWeight = FontWeight.Bold)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    SelectionContainer { Text(response, modifier = Modifier.verticalScroll(rememberScrollState()), fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
fun POSSettingsTab(posService: POSSimulatorService, onSaveClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        var address by remember(posService.hostAddress) { mutableStateOf(posService.hostAddress) }
        var port by remember(posService.hostPort) { mutableStateOf(posService.hostPort.toString()) }

        Card(modifier = Modifier.fillMaxWidth()) {
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
                        onSaveClick() // Persist the change
                    },
                    enabled = !posService.isConnected
                ) {
                    Text("Apply & Save Settings")
                }
            }
        }
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
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description, fontWeight = FontWeight.Bold)
                Text("MTI: ${transaction.mti}", style = MaterialTheme.typography.caption)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colors.primary, modifier = Modifier.padding(start = 8.dp))
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

// Custom tab indicator offset extension, adopted from HostSimulator
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