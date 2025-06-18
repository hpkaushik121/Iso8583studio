package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
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
//    appState.selectedSimulatorType = SimulatorType.HSM
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "APDU Simulator Configuration",
                onBackClick = { navigationController.goBack() })
        },
        backgroundColor = MaterialTheme.colors.background
    ) {
        HSMSimulatorConfigContainer(
            navigationController = navigationController,
            appState = appState,
            onConfigSelected = { appState.selectConfig(it.id) },
            onCreateNew = {
                appState.addConfig(
                    HSMSimulatorConfig(
                        id = appState.generateConfigId(),
                        name = "HSM-${appState.hsmConfigs.value.size + 1}"
                    )
                )
            },
            onDelete = {
                appState.currentConfig(SimulatorType.HSM)?.id?.let { appState.deleteConfig(it) }
            },
//            onSaveAllConfigs = {
//                appState.updateConfig(
//                    (appState.currentConfig(SimulatorType.HSM) as HSMSimulatorConfig).copy(
//                        modifiedDate = System.currentTimeMillis()
//                    )
//                )
//                appState.save()
//            },
            onLaunch = { navigationController.navigateTo(Destination.HSMSimulator) },
        )
    }
}