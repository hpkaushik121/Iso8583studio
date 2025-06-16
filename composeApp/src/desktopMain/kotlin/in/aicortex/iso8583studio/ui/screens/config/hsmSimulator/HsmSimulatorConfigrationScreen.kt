package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.navigation.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack

@Composable
fun HsmSimulatorConfigScreen(
    navigationController: NavigationController, appState: UnifiedSimulatorState,
) {
    appState.selectedSimulatorType = SimulatorType.HSM
    Column {
        AppBarWithBack(
            title = "Host Simulator Configuration",
            onBackClick = { navigationController.goBack() })
        HSMSimulatorConfigContainer(
            navigationController = navigationController,
            appState = appState,
            onSelectConfig = { appState.selectConfig(it.id) },
            onAddConfig = {
                appState.addConfig(
                    HSMSimulatorConfig(
                        id = appState.generateConfigId(),
                        name = "Config_${appState.hsmConfigs.value.size + 1}"
                    )
                )
            },
            onDeleteConfig = {
                appState.currentConfig?.id?.let { appState.deleteConfig(it) }
            },
            onSaveAllConfigs = {
                appState.updateConfig(
                    (appState.currentConfig as HSMSimulatorConfig).copy(
                        modifiedDate = System.currentTimeMillis()
                    )
                )
                appState.save()
            },
            onLaunchSimulator = { navigationController.navigateTo(Destination.HSMSimulator) },
        )
    }
}