package `in`.aicortex.iso8583studio.ui.screens.config.hostSimulator

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun HostSimulatorConfigScreen(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
) {

    Column {
        AppBarWithBack(
            title = "Host Simulator Configuration",
            onBackClick = { navigationController.goBack() })
        HostSimulatorConfigContainer(
            navigationController = navigationController,
            appState = appState,
            onSelectConfig = { appState.selectConfig(it.id) },
            createNewConfig = {
                appState.addConfig(
                    GatewayConfig(
                        id = appState.generateConfigId(),
                        name = "Config - ${appState.hostConfigs.value.size + 1}"
                    )
                )
            },
            onDeleteConfig = {
                appState.currentConfig(SimulatorType.HOST)?.id?.let { appState.deleteConfig(it) }
            },
            onSaveAllConfigs = {
                appState.updateConfig(
                    (appState.currentConfig(SimulatorType.HOST) as GatewayConfig).copy(
                        modifiedDate = System.currentTimeMillis()
                    )
                )
                appState.save()
            },
            onLaunchSimulator = { navigationController.navigateTo(Destination.HostSimulator) },
        )
    }
}