package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import `in`.aicortex.iso8583studio.ui.screens.config.ConfigTab
import `in`.aicortex.iso8583studio.ui.screens.config.ContainerConfig
import `in`.aicortex.iso8583studio.ui.screens.config.SimulatorConfigLayout
import `in`.aicortex.iso8583studio.ui.session.SimulatorSessionManager

@Composable
fun PosTerminalConfigScreen(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
) {
    // Re-read on every recomposition rather than capturing once, matching
    // ApduSimulatorConfigScreen. The container re-invokes tab content after each edit.
    fun current(): POSSimulatorConfig? = appState.currentConfig(SimulatorType.POS) as POSSimulatorConfig?

    // Every tab receives the config it produced and persists THAT. The previous form discarded the
    // emitted config and re-saved the old one with only a new timestamp, so nothing the user
    // changed was ever written.
    val save: (POSSimulatorConfig) -> Unit = { updated ->
        appState.updateConfig(updated.copy(modifiedDate = System.currentTimeMillis()))
    }

    // Labels name what each tab now holds. The composables keep their original file names so the
    // diff stays reviewable; only their contents changed from display strings to real settings.
    val tabs = listOf(
        ConfigTab(label = "Device") { current()?.let { DeviceTab(it, save) } },
        ConfigTab(label = "Hardware") { current()?.let { HardwareTab(it, save) } },
        ConfigTab(label = "Peripherals") { current()?.let { SecurityTab(it, save) } },
        ConfigTab(label = "System & Boot") { current()?.let { NetworkSoftwareTab(it, save) } },
        ConfigTab(label = "Card & Host") { current()?.let { TransactionTab(it, save) } },
    )
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "POS Terminal Configuration",
                onBackClick = { navigationController.goBack() })
        },
        backgroundColor = MaterialTheme.colors.background
    ) {

        SimulatorConfigLayout(
            config = ContainerConfig(
                tabs = tabs,
                label = "POS Simulator",
                currentConfig = {
                    appState.currentConfig(SimulatorType.POS) as POSSimulatorConfig?
                },
                simulatorConfigs = appState.posConfigs.value,
                icon = Icons.Default.PhoneAndroid,
                containerStatus = DevelopmentStatus.EXPERIMENTAL,
            ),
            onSelectConfig = { appState.selectConfig(it.id) },
            createNewConfig = {
                appState.addConfig(
                    POSSimulatorConfig(
                        id = appState.generateConfigId(),
                        // Was hostConfigs — a copy-paste that numbered POS configs off the host list.
                        name = "POS - ${appState.posConfigs.value.size + 1}",
                        description = "",
                        createdDate = System.currentTimeMillis(),
                        modifiedDate = System.currentTimeMillis(),
                        terminalid = 0,
                        merchantid = 0,
                        acquirerid = 0
                    ))
            },
            onDeleteConfig = {
                appState.currentConfig(SimulatorType.POS)?.id?.let { appState.deleteConfig(it) }
            },
            onSaveAllConfigs = {
                appState.updateConfig(
                    (appState.currentConfig(SimulatorType.POS) as POSSimulatorConfig?)?.copy(
                        modifiedDate = System.currentTimeMillis()
                    )
                )
                appState.save()
            },
            onLaunchSimulator = {
                SimulatorSessionManager.launchSimulator(it)
                navigationController.navigateTo(Destination.Home)
            }
        )
    }
}