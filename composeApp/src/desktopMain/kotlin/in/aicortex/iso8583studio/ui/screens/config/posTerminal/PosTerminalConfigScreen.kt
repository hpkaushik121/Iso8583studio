package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack

@Composable
fun PosTerminalConfigScreen(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
) {
    appState.selectedSimulatorType = SimulatorType.POS
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "POS Terminal Configuration",
                onBackClick = { navigationController.goBack() })
        },
        backgroundColor = MaterialTheme.colors.background
    ) {
        POSSimulatorConfigContainer(
            appState = appState,
            onSaveAllConfigs = {
                appState.updateConfig(
                    (appState.currentConfig as POSSimulatorConfig).copy(
                        modifiedDate = System.currentTimeMillis()
                    )
                )
                appState.save()
            },
            onLaunchSimulator = {
                navigationController.navigateTo(Destination.POSTerminal)
            },
            onDeleteConfig = {
                appState.currentConfig?.id?.let { appState.deleteConfig(it) }
            },
            onAddConfig = {
                appState.addConfig(
                    POSSimulatorConfig(
                        id = appState.generateConfigId(),
                        name = "POS - ${appState.hostConfigs.value.size + 1}",
                        description = "",
                        createdDate = System.currentTimeMillis(),
                        modifiedDate = System.currentTimeMillis(),
                        terminalid = 0,
                        merchantid = 0,
                        acquirerid = 0
                    )
                )
            },
            onConfigUpdate = {

            },
            onSelectConfig = {
                appState.selectConfig(it.id)
            }
        )
    }
}