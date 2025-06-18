package `in`.aicortex.iso8583studio.ui.screens.config.issuerSystem

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack

@Composable
fun IssuerSystemConfigScreen(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
) {
//    appState.selectedSimulatorType = SimulatorType.ISSUER
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "Issuer System Configuration",
                onBackClick = { navigationController.goBack() })
        },
        backgroundColor = MaterialTheme.colors.background
    ) {

    }
}