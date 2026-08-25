package `in`.aicortex.iso8583studio.ui.screens.posTerminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.POSSimulatorService
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceFeature
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack

/**
 * The POS device console.
 *
 * Replaces the previous four-tab stub whose Connect and Send handlers were commented out. Tabs are
 * filtered by the resolved variant's [DeviceFeature] set, so a printer-less SKU has no Receipts tab
 * and a model without a PIN pad has no PIN Pad tab — which is what makes "this is the 5.5\" A910S,
 * not the base one" visible rather than merely configured.
 *
 * Layout follows the house runtime pattern (`ApduSimulatorV2Screen`): `Scaffold` + `AppBarWithBack`,
 * a `TabRow` with the centred indicator, and per-tab state preserved by a `SaveableStateHolder`.
 */
enum class PosTerminalTabs(val title: String, val icon: ImageVector) {
    DEVICE("Device", Icons.Default.PhoneAndroid),
    CARD("Card", Icons.Default.CreditCard),
    TRACE("SDK Trace", Icons.Default.CompareArrows),
    PED("PIN Pad", Icons.Default.Dialpad),
    RECEIPTS("Receipts", Icons.Default.Receipt),
    SCANNER("Scanner", Icons.Default.QrCodeScanner),
    TRANSACTIONS("Transactions", Icons.Default.ReceiptLong),
    SCENARIOS("Scenarios", Icons.Default.Science),
    LOGS("Logs", Icons.Default.Article),
}

@Composable
fun POSTerminalSimulatorScreen(
    window: ComposeWindow,
    config: POSSimulatorConfig?,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
    /**
     * Supplied by the session so the emulator survives tab switches. When absent — the legacy
     * direct-navigation route — the screen owns the service and must stop it on dispose, or a 2 GB
     * VM outlives the screen that started it. Same contract as `HsmSimulatorScreen`.
     */
    service: POSSimulatorService? = null,
) {
    if (config == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No POS Terminal configuration selected.", style = MaterialTheme.typography.subtitle1)
        }
        return
    }

    val ownsService = service == null
    val posService = remember(config) { service ?: POSSimulatorService(config) }
    if (ownsService) {
        DisposableEffect(posService) { onDispose { posService.stop() } }
    }

    val resolved = remember(config.terminalProfileId) { DeviceCatalog.resolve(config.terminalProfileId) }
    val features = resolved?.terminal?.features ?: emptySet()

    // Feature-driven tab set. Anything the device does not have simply is not offered.
    val visibleTabs = remember(features) {
        PosTerminalTabs.entries.filter { tab ->
            when (tab) {
                PosTerminalTabs.PED -> DeviceFeature.PED in features
                PosTerminalTabs.RECEIPTS -> DeviceFeature.PRINTER in features
                PosTerminalTabs.SCANNER -> DeviceFeature.SCANNER in features
                PosTerminalTabs.CARD ->
                    features.any { it in setOf(DeviceFeature.ICC, DeviceFeature.PICC, DeviceFeature.MSR) }
                else -> true
            }
        }
    }

    var selectedTab by remember { mutableStateOf(PosTerminalTabs.DEVICE) }
    // Clamp when the variant changes underneath us and removes the current tab.
    if (selectedTab !in visibleTabs) selectedTab = visibleTabs.first()

    val tabStateHolder = rememberSaveableStateHolder()

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "POS Terminal - ${config.name}",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = onSaveClick) {
                        Icon(Icons.Default.Save, contentDescription = "Save configuration")
                    }
                },
            )
        },
        backgroundColor = MaterialTheme.colors.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0),
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary,
                indicator = { positions ->
                    val index = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)
                    TabRowDefaults.Indicator(
                        Modifier.customTabIndicatorOffset(positions[index]),
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
                                Icon(tab.icon, contentDescription = tab.title, Modifier.size(18.dp))
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

            Box(Modifier.fillMaxSize().padding(16.dp)) {
                tabStateHolder.SaveableStateProvider(selectedTab.name) {
                    when (selectedTab) {
                        PosTerminalTabs.DEVICE -> PosDeviceTab(posService, resolved)
                        PosTerminalTabs.CARD -> PosCardTab(config, resolved)
                        PosTerminalTabs.TRACE -> PosPendingTab(
                            "SDK call trace",
                            "Every DAL call the payment app makes — interface, method, arguments, " +
                                "latency and result — with the matching APDU exchange beside it.",
                            "Arrives with the device bridge (M4).",
                        )
                        PosTerminalTabs.PED -> PosPendingTab(
                            "PIN pad",
                            "Soft keypad, PIN block format selector, and the live clear block, " +
                                "encrypted block, KSN and KCV computed on this desktop.",
                            "Arrives with the PED handler (M5).",
                        )
                        PosTerminalTabs.RECEIPTS -> PosPendingTab(
                            "Receipts",
                            "A thermal-paper roll rendering what the app printed, at this device's " +
                                "dots-per-line, with paper-out and overheat fault injection.",
                            "Arrives with the printer handler (M5).",
                        )
                        PosTerminalTabs.SCANNER -> PosPendingTab(
                            "Scanner",
                            "Inject barcodes into the running app, with presets, history and a " +
                                "queue for scripted runs.",
                            "Arrives with the scanner handler (M5).",
                        )
                        PosTerminalTabs.TRANSACTIONS -> PosPendingTab(
                            "Transactions",
                            "Completed transactions with their EMV tags and the ISO 8583 request " +
                                "and response that carried them.",
                            "Arrives with the ISO 8583 uplink (M8).",
                        )
                        PosTerminalTabs.SCENARIOS -> PosPendingTab(
                            "Scenarios",
                            "Fault injection — card yanked mid-transaction, comm errors, PIN " +
                                "timeout, paper out, host no-response — plus scripted runs with " +
                                "deterministic replay.",
                            "Arrives with the scenario engine (M9).",
                        )
                        PosTerminalTabs.LOGS -> PosPendingTab(
                            "Logs",
                            "Boot log, bridge frames and DAL calls projected into the shared log " +
                                "panel with filtering and auto-scroll.",
                            "Arrives with the device bridge (M4).",
                        )
                    }
                }
            }
        }
    }
}

/** 32.dp indicator centred under the tab, matching the other simulator screens. */
internal fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 32.dp
    val indicatorOffset = currentTabPosition.left + (currentTabPosition.width - indicatorWidth) / 2
    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset)
        .width(indicatorWidth)
}
