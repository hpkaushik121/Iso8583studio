package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.ProfileStore
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.samples.SampleProfiles
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import kotlinx.coroutines.Job
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Runtime APDU simulator screen. Layout matches the host simulator standard:
 *   - Scaffold with [AppBarWithBack] (title + Save action)
 *   - [TabRow] with the centered indicator
 *   - Tab content in a [Box] padded 16.dp; the live tab carries its own compact status bar and
 *     control panel, exactly like the host simulator's `SimulatorHandlerTab`.
 *
 * No permanent status strip lives above the tabs — that's the host-sim pattern.
 */
private enum class RuntimeTab(val title: String, val icon: ImageVector) {
    CARD_SESSION("Card Session", Icons.Filled.SwapHoriz),
    TRACE_LOG("Trace Log", Icons.Filled.Article),
    TEST_PLANS("Test Plans", Icons.Filled.PlayArrow),
    WIRE_SNIFF("Wire Sniff", Icons.Filled.Wifi),
    L3_REPORT("L3 Report", Icons.Filled.Verified),
    FIRMWARE("Firmware", Icons.Filled.Memory),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@Composable
fun ApduSimulatorV2Screen(
    config: APDUSimulatorConfig,
    onBack: () -> Unit = {},
    onSaveClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val store = remember {
        val dir = Paths.get(System.getProperty("user.home"), ".iso8583studio", "card-profiles")
        Files.createDirectories(dir)
        ProfileStore(dir).also { s -> if (s.list().isEmpty()) SampleProfiles.all().forEach(s::save) }
    }
    val controller = remember { SimulatorController(scope) }

    // Hoisted state — kept here so it survives switching tabs and re-entering the runtime.
    val firmwareLog = remember { mutableStateListOf<LogEntry>() }
    val firmwareDir = remember { mutableStateOf<Path?>(detectFirmwareDir()) }
    val pioPath = remember { mutableStateOf<Path?>(detectPio()) }
    val firmwareJob = remember { mutableStateOf<Job?>(null) }
    // Explicit running flag (Job.isActive is not a Compose State — won't trigger recomposition).
    val firmwareRunning = remember { mutableStateOf(false) }

    LaunchedEffect(
        config.id,
        config.transportMode,
        config.activeProfileId,
        config.pcscReaderName,
        config.serialPortName,
        config.serialBaudRate,
    ) {
        val profile = config.activeProfileId.takeIf { it.isNotBlank() }
            ?.let { id -> runCatching { store.load(id) }.getOrNull() }
        controller.hydrateFromConfig(
            modeName = config.transportMode,
            pcscReaderName = config.pcscReaderName,
            serialPortName = config.serialPortName,
            baud = config.serialBaudRate,
            profile = profile,
        )
    }

    val visibleTabs = remember(controller.mode) {
        when (controller.mode) {
            TransportMode.LOOPBACK -> listOf(
                RuntimeTab.CARD_SESSION, RuntimeTab.TRACE_LOG, RuntimeTab.TEST_PLANS,
                RuntimeTab.L3_REPORT, RuntimeTab.SETTINGS,
            )
            TransportMode.PCSC -> listOf(
                RuntimeTab.CARD_SESSION, RuntimeTab.TRACE_LOG, RuntimeTab.TEST_PLANS,
                RuntimeTab.WIRE_SNIFF, RuntimeTab.SETTINGS,
            )
            TransportMode.SERIAL -> listOf(
                RuntimeTab.CARD_SESSION, RuntimeTab.TRACE_LOG, RuntimeTab.TEST_PLANS,
                RuntimeTab.WIRE_SNIFF, RuntimeTab.L3_REPORT,
                RuntimeTab.FIRMWARE, RuntimeTab.SETTINGS,
            )
        }
    }

    var selectedTab by remember { mutableStateOf(visibleTabs.first()) }
    if (selectedTab !in visibleTabs) selectedTab = visibleTabs.first()

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "APDU Simulator - ${config.name.ifBlank { "Unknown" }}",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = onSaveClick) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Configuration")
                    }
                },
            )
        },
        backgroundColor = MaterialTheme.colors.background,
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Tab row — centered indicator, same as HostSimulatorScreen
            val selectedIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)
            TabRow(
                selectedTabIndex = selectedIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary,
                indicator = { positions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.centeredTabIndicatorOffset(positions[selectedIndex]),
                        height = 3.dp,
                        color = MaterialTheme.colors.primary,
                    )
                },
            ) {
                visibleTabs.forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        selectedContentColor = MaterialTheme.colors.primary,
                        unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(tab.icon, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    tab.title,
                                    fontWeight = if (tab == selectedTab) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        },
                    )
                }
            }

            // Wrapping the tab body in SaveableStateProvider preserves each tab's primitive
            // rememberSaveable state (text fields, scroll positions, selected items) across
            // tab switches. Complex state (logs, exchanges, jobs) is hoisted above and passed
            // in by parameter so it persists too.
            val tabStateHolder = rememberSaveableStateHolder()
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
                tabStateHolder.SaveableStateProvider(selectedTab.name) {
                    when (selectedTab) {
                        RuntimeTab.CARD_SESSION ->
                            if (controller.mode == TransportMode.SERIAL) PassiveCardSession(controller, config)
                            else ActiveCardSession(controller, config)
                        RuntimeTab.TRACE_LOG -> LogsTab(controller)
                        RuntimeTab.TEST_PLANS -> TestPlanTab(controller)
                        RuntimeTab.WIRE_SNIFF -> WireSniffTab()
                        RuntimeTab.L3_REPORT -> L3ReportTab(config)
                        RuntimeTab.FIRMWARE -> FirmwareTab(
                            log = firmwareLog,
                            firmwareDir = firmwareDir,
                            pioPath = pioPath,
                            currentJob = firmwareJob,
                            running = firmwareRunning,
                        )
                        RuntimeTab.SETTINGS -> RuntimeSettingsTab(controller, config)
                    }
                }
            }
        }
    }
}

private fun Modifier.centeredTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 32.dp
    val tabWidth = currentTabPosition.width
    val offsetX = currentTabPosition.left + (tabWidth - indicatorWidth) / 2
    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = offsetX)
        .width(indicatorWidth)
}
