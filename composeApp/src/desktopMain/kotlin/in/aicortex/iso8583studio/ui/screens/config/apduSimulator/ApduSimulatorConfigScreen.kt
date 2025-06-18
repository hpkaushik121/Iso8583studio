package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.ui.navigation.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.CardType
import `in`.aicortex.iso8583studio.ui.navigation.ConnectionInterface
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack

@Composable
fun ApduSimulatorConfigScreen(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
) {

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "APDU Simulator Configuration",
                onBackClick = { navigationController.goBack() })
        },
        backgroundColor = MaterialTheme.colors.background
    ) {
        ApduSimulatorScreen(
            appState = appState,
            onAddProfile = {
                val newProfile = APDUSimulatorConfig(name = "New APDU Profile", status = ProfileStatus.Issues,
                    cardType = CardType.CUSTOM,
                    id = appState.generateConfigId(),
                    connectionInterface = ConnectionInterface.PC_SC)
                appState.addConfig(newProfile)

            },
            onDeleteProfile = {
                appState.currentConfig(SimulatorType.APDU)?.id?.let { appState.deleteConfig(it) }
                    },
            onSaveChanges = {
                appState.save()
            },
            onProfileUpdate = {
                appState.updateConfig(it)
            },
            onLaunchSimulator = {
                navigationController.navigateTo(Destination.ApduSimulator)
            },
            onSelectProfile = {
                appState.selectConfig(it.id)
            }
        )
    }
}