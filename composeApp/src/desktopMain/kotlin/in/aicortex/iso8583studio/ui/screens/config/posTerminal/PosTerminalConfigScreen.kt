package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
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

    }
}