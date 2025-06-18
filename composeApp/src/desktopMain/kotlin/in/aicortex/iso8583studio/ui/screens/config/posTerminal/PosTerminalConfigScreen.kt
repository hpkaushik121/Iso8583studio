package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.runtime.Composable
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.config.ConfigTab
import `in`.aicortex.iso8583studio.ui.screens.config.ContainerConfig
import `in`.aicortex.iso8583studio.ui.screens.config.SimulatorConfigLayout

@Composable
fun PosTerminalConfigScreen(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
) {
    val currentConfig = appState.currentConfig(SimulatorType.POS) as POSSimulatorConfig?
    val tabs = listOf(
        ConfigTab(
            label = "Hardware",
            content = {
                currentConfig?.let {
                    HardwareTab(
                        it,
                    ) {
                        appState.updateConfig(
                            currentConfig.copy(
                                modifiedDate = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        ),
        ConfigTab(
            label = "Transaction",
            content = {
                currentConfig?.let {
                    TransactionTab(
                        it,
                    ) {
                        appState.updateConfig(
                            currentConfig.copy(
                                modifiedDate = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        ),
        ConfigTab(
            label = "Security",
            content = {
                currentConfig?.let {
                    SecurityTab(
                        it,
                    ) {
                        appState.updateConfig(
                            currentConfig.copy(
                                modifiedDate = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        ),
        ConfigTab(
            label = "Network & SW",
            content = {
                currentConfig?.let {
                    NetworkSoftwareTab(
                        it,
                    ) {
                        appState.updateConfig(
                            currentConfig.copy(
                                modifiedDate = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        ),
    )
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "POS Terminal Configuration",
                onBackClick = { navigationController.goBack() })
        },
        backgroundColor = MaterialTheme.colors.background
    ) {

        SimulatorConfigLayout(
            config = ContainerConfig(
                tabs = tabs,
                label = "POS Simulator",
                currentConfig = {
                    appState.currentConfig(SimulatorType.POS) as POSSimulatorConfig?
                },
                simulatorConfigs = appState.posConfigs.value,
                icon = Icons.Default.PhoneAndroid
            ),
            onSelectConfig = { appState.selectConfig(it.id) },
            createNewConfig = {
                appState.addConfig(
                    POSSimulatorConfig(
                        id = appState.generateConfigId(),
                        name = "POS - ${appState.hostConfigs.value.size + 1}",
                        description = "",
                        createdDate = System.currentTimeMillis(),
                        modifiedDate = System.currentTimeMillis(),
                        terminalid = 0,
                        merchantid = 0,
                        acquirerid = 0
                    ))
            },
            onDeleteConfig = {
                appState.currentConfig(SimulatorType.POS)?.id?.let { appState.deleteConfig(it) }
            },
            onSaveAllConfigs = {
                appState.updateConfig(
                    (appState.currentConfig(SimulatorType.POS) as POSSimulatorConfig?)?.copy(
                        modifiedDate = System.currentTimeMillis()
                    )
                )
                appState.save()
            },
            onLaunchSimulator = {
                navigationController.navigateTo(Destination.POSTerminal)
            }

        )
    }
}