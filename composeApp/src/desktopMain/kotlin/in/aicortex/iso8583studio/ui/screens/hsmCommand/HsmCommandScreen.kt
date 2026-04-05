package `in`.aicortex.iso8583studio.ui.screens.hsmCommand

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.data.model.AppSettings
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.ConnectionState
import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.HsmCommandClientService
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmCommandConfig
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogTab
import kotlinx.coroutines.launch

enum class HsmCommandTabs(val title: String, val icon: ImageVector) {
    COMMAND_CONSOLE("Console", Icons.Default.Terminal),
    SCENARIO_BUILDER("Scenario", Icons.Default.AccountTree),
    LOAD_TESTER("Load Test", Icons.Default.Speed),
    LOGS("Logs", Icons.Default.Article),
}

@Composable
fun HsmCommandScreen(
    config: HsmCommandConfig,
    onBack: () -> Unit,
    service: HsmCommandClientService? = null,
    onSaveConfig: ((HsmCommandConfig) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val ownsService = service == null
    val hsmService = remember(config) { service ?: HsmCommandClientService(config) }

    val logText = remember { mutableStateListOf<LogEntry>() }

    LaunchedEffect(hsmService) {
        if (AppSettings.enableGlobalLogging) {
            hsmService.beforeWriteLog = { entry -> logText.add(entry) }
        }
    }

    if (ownsService) {
        DisposableEffect(hsmService) {
            onDispose {
                scope.launch { hsmService.disconnect() }
            }
        }
    }

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "HSM Host Console - ${config.name}",
                onBackClick = {
                    scope.launch {
                        hsmService.disconnect()
                        onBack()
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        HsmCommandContent(
            service = hsmService,
            logText = logText,
            onSaveConfig = onSaveConfig,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun HsmCommandContent(
    service: HsmCommandClientService,
    logText: SnapshotStateList<LogEntry>,
    onSaveConfig: ((HsmCommandConfig) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val connectionState by service.connectionState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = HsmCommandTabs.entries.toList()
    val vendorCommands = remember(service.config.hsmVendor) { getVendorCommands(service.config.hsmVendor) }
    val commandConsoleSession = remember { CommandConsoleSessionState() }
    val scenarioSession = remember { ScenarioSessionState() }
    var savedScenarios by remember { mutableStateOf(service.config.scenarios) }

    Row(modifier = modifier.fillMaxSize()) {
        NavigationRail(
            connectionState = connectionState,
            config = service.config,
            selectedTab = selectedTabIndex,
            tabs = tabList,
            onTabSelected = { selectedTabIndex = it },
            onConnect = {
                scope.launch {
                    try { service.connect() } catch (_: Exception) { }
                }
            },
            onDisconnect = {
                scope.launch { service.disconnect() }
            }
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            ConnectionHeader(
                connectionState = connectionState,
                config = service.config,
                onConnect = {
                    scope.launch {
                        try { service.connect() } catch (_: Exception) { }
                    }
                },
                onDisconnect = {
                    scope.launch { service.disconnect() }
                }
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (tabList[selectedTabIndex]) {
                    HsmCommandTabs.COMMAND_CONSOLE -> CommandConsoleTab(
                        service = service,
                        vendorCommands = vendorCommands,
                        session = commandConsoleSession,
                        exchangeLog = logText,
                        logFileSessionName = service.config.name,
                    )
                    HsmCommandTabs.SCENARIO_BUILDER -> ScenarioBuilderTab(
                        service = service,
                        session = scenarioSession,
                        savedScenarios = savedScenarios,
                        onSaveScenario = { scenario ->
                            val updated = savedScenarios.toMutableList()
                            val existingIdx = updated.indexOfFirst { it.id == scenario.id }
                            if (existingIdx >= 0) updated[existingIdx] = scenario else updated.add(scenario)
                            savedScenarios = updated
                            scenarioSession.currentScenarioId = scenario.id
                            val newConfig = service.config.copy(scenarios = updated, modifiedDate = System.currentTimeMillis())
                            onSaveConfig?.invoke(newConfig)
                        },
                        onDeleteScenario = { id ->
                            val updated = savedScenarios.filter { it.id != id }
                            savedScenarios = updated
                            val newConfig = service.config.copy(scenarios = updated, modifiedDate = System.currentTimeMillis())
                            onSaveConfig?.invoke(newConfig)
                        },
                    )
                    HsmCommandTabs.LOAD_TESTER -> HsmLoadTesterTab(
                        service = service,
                        scenarioSession = scenarioSession,
                        savedScenarios = savedScenarios,
                        onSaveConfig = onSaveConfig,
                    )
                    HsmCommandTabs.LOGS -> LogTab(
                        logEntries = logText,
                        onClearClick = { logText.clear() },
                        connectionCount = if (connectionState == ConnectionState.CONNECTED) 1 else 0,
                        concurrentConnections = if (connectionState == ConnectionState.CONNECTED) 1 else 0,
                        bytesIncoming = 0L,
                        bytesOutgoing = 0L,
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationRail(
    connectionState: ConnectionState,
    config: HsmCommandConfig,
    selectedTab: Int,
    tabs: List<HsmCommandTabs>,
    onTabSelected: (Int) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val statusColor = when (connectionState) {
        ConnectionState.CONNECTED -> Color(0xFF4CAF50)
        ConnectionState.CONNECTING -> Color(0xFFFF9800)
        ConnectionState.ERROR -> Color(0xFFF44336)
        ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E)
    }

    Surface(
        modifier = Modifier.fillMaxHeight().width(64.dp),
        color = MaterialTheme.colors.surface,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConnectionDot(
                state = connectionState,
                color = statusColor,
                onClick = {
                    when (connectionState) {
                        ConnectionState.DISCONNECTED, ConnectionState.ERROR -> onConnect()
                        ConnectionState.CONNECTED -> onDisconnect()
                        else -> {}
                    }
                }
            )

            Spacer(Modifier.height(16.dp))
            Divider(modifier = Modifier.width(40.dp), color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))

            tabs.forEachIndexed { index, tab ->
                RailItem(
                    icon = tab.icon,
                    label = tab.title,
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) }
                )
                if (index < tabs.lastIndex) Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.weight(1f))

            Text(
                config.hsmVendor.name.take(3),
                style = MaterialTheme.typography.overline,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ConnectionDot(
    state: ConnectionState,
    color: Color,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val displayAlpha = if (state == ConnectionState.CONNECTING) pulseAlpha else 1f
    val animatedColor by animateColorAsState(color, animationSpec = tween(300))

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(animatedColor.copy(alpha = 0.12f * displayAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = animatedColor.copy(alpha = displayAlpha)
        ) {}
    }
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        if (selected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(200)
    )
    val contentColor by animateColorAsState(
        if (selected) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
        animationSpec = tween(200)
    )

    Column(
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = contentColor)
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ConnectionHeader(
    connectionState: ConnectionState,
    config: HsmCommandConfig,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val (statusColor, statusText) = when (connectionState) {
        ConnectionState.CONNECTED -> Color(0xFF4CAF50) to "Connected"
        ConnectionState.CONNECTING -> Color(0xFFFF9800) to "Connecting..."
        ConnectionState.ERROR -> Color(0xFFF44336) to "Error"
        ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E) to "Disconnected"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface,
        elevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = statusColor.copy(alpha = 0.12f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(modifier = Modifier.size(7.dp), shape = CircleShape, color = statusColor) {}
                    Text(
                        statusText,
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }
            }

            Divider(modifier = Modifier.height(20.dp).width(1.dp), color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.Dns, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                Text(
                    "${config.hsmVendor.displayName}",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                Text(
                    "${config.ipAddress}:${config.port}",
                    style = MaterialTheme.typography.caption,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }

            if (config.sslConfig.enabled) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(11.dp), tint = Color(0xFF4CAF50))
                        Text("TLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            when (connectionState) {
                ConnectionState.DISCONNECTED, ConnectionState.ERROR -> {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Connect", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                ConnectionState.CONNECTED -> {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                    ) {
                        Icon(Icons.Default.PowerOff, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                ConnectionState.CONNECTING -> {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                }
            }
        }
    }
}
