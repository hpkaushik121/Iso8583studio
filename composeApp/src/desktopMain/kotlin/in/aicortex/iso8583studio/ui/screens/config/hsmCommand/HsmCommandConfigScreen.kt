package `in`.aicortex.iso8583studio.ui.screens.config.hsmCommand

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmCommandConfig
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import `in`.aicortex.iso8583studio.ui.screens.config.ConfigTab
import `in`.aicortex.iso8583studio.ui.screens.config.ContainerConfig
import `in`.aicortex.iso8583studio.ui.screens.config.SimulatorConfigLayout
import `in`.aicortex.iso8583studio.ui.session.SimulatorSessionManager

@Composable
fun HsmCommandConfigScreen(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
) {
    val tabsList = listOf(
        ConfigTab(
            label = "Connection Settings",
            content = {
                ConnectionSettingsTab(
                    config = appState.currentConfig(SimulatorType.HSM_COMMAND)!! as HsmCommandConfig
                ) { updatedConfig ->
                    appState.updateConfig(updatedConfig)
                }
            }
        ),
        ConfigTab(
            label = "SSL/TLS Configuration",
            content = {
                SslConfigurationTab(
                    config = appState.currentConfig(SimulatorType.HSM_COMMAND)!! as HsmCommandConfig
                ) { updatedConfig ->
                    appState.updateConfig(updatedConfig)
                }
            }
        ),
        ConfigTab(
            label = "Load Test Settings",
            content = {
                LoadTestSettingsTab(
                    config = appState.currentConfig(SimulatorType.HSM_COMMAND)!! as HsmCommandConfig
                ) { updatedConfig ->
                    appState.updateConfig(updatedConfig)
                }
            }
        )
    )

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "HSM Commander Configuration",
                onBackClick = { navigationController.goBack() }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) {
        SimulatorConfigLayout(
            config = ContainerConfig(
                tabs = tabsList,
                icon = Icons.Default.Terminal,
                label = "HSM Commander",
                simulatorConfigs = appState.hsmCommandConfigs.value,
                currentConfig = { appState.currentConfig(SimulatorType.HSM_COMMAND) },
                containerStatus = DevelopmentStatus.BETA
            ),
            onSelectConfig = { appState.selectConfig(it.id) },
            createNewConfig = {
                appState.addConfig(
                    HsmCommandConfig(
                        id = appState.generateConfigId(),
                        name = "HSM Commander - ${appState.hsmCommandConfigs.value.size + 1}"
                    )
                )
            },
            onDeleteConfig = {
                appState.deleteConfig(it.id)
            },
            onLaunchSimulator = {
                appState.selectConfig(it.id)
                SimulatorSessionManager.launchSimulator(it)
                navigationController.navigateTo(Destination.Home)
            },
            onSaveAllConfigs = {
                appState.updateConfig(
                    (appState.currentConfig(SimulatorType.HSM_COMMAND) as HsmCommandConfig).copy(
                        modifiedDate = System.currentTimeMillis()
                    )
                )
                appState.save()
            }
        )
    }
}
