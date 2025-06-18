package `in`.aicortex.iso8583studio.ui.screens.config.hostSimulator

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.config.ConfigTab
import `in`.aicortex.iso8583studio.ui.screens.config.ContainerConfig
import `in`.aicortex.iso8583studio.ui.screens.config.SimulatorConfigLayout

@Composable
fun HostSimulatorConfigScreen(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
) {
    val tabsList = listOf(
        ConfigTab(
            label = "Gateway Type",
            content = {
                GatewayTypeTab(
                    config = appState.currentConfig(SimulatorType.HOST)!! as GatewayConfig
                ) { updatedConfig ->
                    appState.updateConfig(updatedConfig)
                }
            }
        ),
        ConfigTab(
            label = "Transmission Settings",
            content = {
                TransmissionSettingsTab(
                    config = appState.currentConfig(SimulatorType.HOST)!! as GatewayConfig
                ) { updatedConfig ->
                    appState.updateConfig(updatedConfig)
                }
            }
        ),
        ConfigTab(
            label = "Log Settings",
            content = {
                LogSettingsTab(
                    config = appState.currentConfig(SimulatorType.HOST)!! as GatewayConfig
                ) { updatedConfig ->
                    appState.updateConfig(updatedConfig)
                }
            }
        ),
        ConfigTab(
            label = "Advanced Options",
            content = { AdvancedOptionsTab() }
        )
    )
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "Host Simulator Configuration",
                onBackClick = { navigationController.goBack() })
        },
        backgroundColor = MaterialTheme.colors.background
    ) {
        SimulatorConfigLayout(
            config = ContainerConfig(
                tabs = tabsList,
                icon = Icons.Default.Computer,
                label = "Host Simulator",
                simulatorConfigs = appState.hostConfigs.value,
                currentConfig = { appState.currentConfig(SimulatorType.HOST) }
            ),
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
                appState.deleteConfig(it.id)
            },
            onLaunchSimulator = {
                appState.selectConfig(it.id)
                navigationController.navigateTo(Destination.HostSimulator)
            },
            onSaveAllConfigs = {
                appState.updateConfig(
                    (appState.currentConfig(SimulatorType.HOST) as GatewayConfig).copy(
                        modifiedDate = System.currentTimeMillis()
                    )
                )
                appState.save()
            }

        )

    }
}