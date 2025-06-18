package `in`.aicortex.iso8583studio.ui.screens.config.ecrSimulator

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack


@Composable
fun EcrSimulatorConfigScreen(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
) {
//    appState.selectedSimulatorType = SimulatorType.ECR
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "ECR Simulator Configuration",
                onBackClick = { navigationController.goBack() })
        },
        backgroundColor = MaterialTheme.colors.background
    ) {

    }
}