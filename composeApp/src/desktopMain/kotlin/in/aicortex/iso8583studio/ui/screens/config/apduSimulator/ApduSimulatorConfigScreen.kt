package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.CardType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.ConnectionInterface
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.config.ConfigTab
import `in`.aicortex.iso8583studio.ui.screens.config.ContainerConfig
import `in`.aicortex.iso8583studio.ui.screens.config.SimulatorConfigLayout

@Composable
fun ApduSimulatorConfigScreen(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
) {
    val tabs = listOf(
        ConfigTab(
            label = "Basic",
            content = {
                BasicConfigTab()
            }
        ),
        ConfigTab(
            label = "Applications",
            content = {
                ApplicationAidTab()
            }
        ),
        ConfigTab(
            label = "File System",
            content = {
                FileSystemTab()
            }
        ),
        ConfigTab(
            label = "Cryptography",
            content = {
                CryptoManagementTab()
            }
        ),
        ConfigTab(
            label = "Behavior",
            content = {
                BehaviorRuleManagementTab()
            }
        )
    )
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "APDU Simulator Configuration",
                onBackClick = { navigationController.goBack() })
        },
        backgroundColor = MaterialTheme.colors.background
    ) {
        SimulatorConfigLayout(
            config = ContainerConfig(
                tabs = tabs,
                label = "APDU Simulator",
                currentConfig = {
                    appState.currentConfig(SimulatorType.APDU) as APDUSimulatorConfig?
                },
                simulatorConfigs = appState.apduConfigs.value,
                icon = Icons.Default.Security
            ),
            onSelectConfig = { appState.selectConfig(it.id) },
            createNewConfig = {
                appState.addConfig(
                    APDUSimulatorConfig(
                        name = "Card-${appState.apduConfigs.value.size + 1}",
                        cardType = CardType.CUSTOM,
                        id = appState.generateConfigId(),
                        connectionInterface = ConnectionInterface.PC_SC
                    ))
            },
            onDeleteConfig = {
                appState.currentConfig(SimulatorType.APDU)?.id?.let { appState.deleteConfig(it) }
            },
            onSaveAllConfigs = {
                appState.updateConfig(
                    (appState.currentConfig(SimulatorType.APDU) as APDUSimulatorConfig?)?.copy(
                        modifiedDate = System.currentTimeMillis()
                    )
                )
                appState.save()
            },
            onLaunchSimulator = {
                navigationController.navigateTo(Destination.HSMSimulator)
            }

        )
    }
}