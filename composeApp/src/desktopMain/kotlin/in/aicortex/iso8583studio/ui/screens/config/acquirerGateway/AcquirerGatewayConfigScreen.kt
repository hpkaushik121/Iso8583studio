package `in`.aicortex.iso8583studio.ui.screens.config.acquirerGateway

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack

@Composable
fun AcquiringGatewayConfigScreen(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
) {
    appState.selectedSimulatorType = SimulatorType.ACQUIRER
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "Acquiring Gateway Configuration",
                onBackClick = { navigationController.goBack() })
        },
        backgroundColor = MaterialTheme.colors.background
    ) {

    }
}