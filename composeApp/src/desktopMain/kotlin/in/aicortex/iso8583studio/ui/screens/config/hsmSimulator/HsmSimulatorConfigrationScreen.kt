package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.config.ConfigTab
import `in`.aicortex.iso8583studio.ui.screens.config.ContainerConfig
import `in`.aicortex.iso8583studio.ui.screens.config.SimulatorConfigLayout

@Composable
fun HsmSimulatorConfigScreen(
    navigationController: NavigationController, appState: UnifiedSimulatorState,
) {
    val tabsList = listOf(
        ConfigTab(
            label = "Profile",
            content = {
                HSMProfileTab(config = appState.currentConfig(SimulatorType.HSM) as HSMSimulatorConfig) {
                   appState.updateConfig(it)
                }
            }
        ),
        ConfigTab(
            label = "Network",
            content = {
                val config = appState.currentConfig(SimulatorType.HSM) as HSMSimulatorConfig
                NetworkConfigTab(networkConfig = config.network) {
                    appState.updateConfig(config.copy(network = it))
                }
            }
        ),
        ConfigTab(
            label = "Security",
            content = {
                val config = appState.currentConfig(SimulatorType.HSM) as HSMSimulatorConfig
                SecurityConfigTab(
                    securityConfig = config.security,
                    onConfigUpdated = {
                        appState.updateConfig(config.copy(security = it))
                    }
                )
            }
        ),
        ConfigTab(
            label = "Keys",
            content = {
                val config = appState.currentConfig(SimulatorType.HSM) as HSMSimulatorConfig
                KeyManagementTab(
                    keyManagementConfig = config.keyManagement,
                    onConfigUpdated = {
                        appState.updateConfig(config.copy(keyManagement = it))
                    }
                )
            }
        ),
        ConfigTab(
            label = "Advanced",
            content = {
                val config = appState.currentConfig(SimulatorType.HSM) as HSMSimulatorConfig
                AdvancedOptionsTab(
                    advancedConfig = config.advanced,
                    onConfigUpdated = {
                        appState.updateConfig(config.copy(advanced = it))
                    }
                )
            }
        ),
    )
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "HSM Simulator Configuration",
                onBackClick = { navigationController.goBack() })
        },
        backgroundColor = MaterialTheme.colors.background
    ) {
        SimulatorConfigLayout(
            config = ContainerConfig(
                tabs = tabsList,
                label = "HSM Simulator",
                currentConfig = {
                    appState.currentConfig(SimulatorType.HSM) as HSMSimulatorConfig?
                },
                simulatorConfigs = appState.hsmConfigs.value,
                icon = Icons.Default.Security
            ),
            onSelectConfig = { appState.selectConfig(it.id) },
            createNewConfig = {
                appState.addConfig(
                    HSMSimulatorConfig(
                        id = appState.generateConfigId(),
                        name = "HSM-${appState.hsmConfigs.value.size + 1}"
                    ))
            },
            onDeleteConfig = {
                appState.currentConfig(SimulatorType.HSM)?.id?.let { appState.deleteConfig(it) }
            },
            onSaveAllConfigs = {
                appState.updateConfig(
                    (appState.currentConfig(SimulatorType.HSM) as HSMSimulatorConfig).copy(
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