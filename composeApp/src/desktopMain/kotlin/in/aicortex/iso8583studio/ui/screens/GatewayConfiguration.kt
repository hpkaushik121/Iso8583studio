package `in`.aicortex.iso8583studio.ui.screens

import androidx.compose.runtime.*
import `in`.aicortex.iso8583studio.ui.navigation.GatewayConfigurationState
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.Screen
import `in`.aicortex.iso8583studio.ui.screens.config.AdvancedOptionsTab
import `in`.aicortex.iso8583studio.ui.screens.config.GatewayTypeTab
import `in`.aicortex.iso8583studio.ui.screens.config.HostSimulatorScreen
import `in`.aicortex.iso8583studio.ui.screens.config.KeysSettingTab
import `in`.aicortex.iso8583studio.ui.screens.config.LogSettingsTab
import `in`.aicortex.iso8583studio.ui.screens.config.MonitorScreen
import `in`.aicortex.iso8583studio.ui.screens.config.TabContainer
import `in`.aicortex.iso8583studio.ui.screens.config.TransmissionSettingsTab
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun GatewayConfiguration(navigationController: NavigationController,
                         appState: GatewayConfigurationState) {

    // Create navigation controller

    when (val screen = navigationController.currentScreen) {
        is Screen.GatewayType -> selectTab(navigationController = navigationController,
            appState = appState,
            index = 0,
            content = {
                appState.currentConfig?.let { config ->
                    GatewayTypeTab(config = config) { updatedConfig ->
                        navigationController.saveConfig(updatedConfig)
                    }
                }
            })
        is Screen.TransmissionSettings -> selectTab(navigationController = navigationController,
            appState = appState,
            index = 1,
            content = {
                appState.currentConfig?.let { config ->
                    TransmissionSettingsTab (config = config) { updatedConfig ->
                        navigationController.saveConfig(updatedConfig)
                    }
                }
            })
        is Screen.KeySettings -> selectTab(navigationController = navigationController,
            appState = appState,
            index = 2,
            content = {
                KeysSettingTab(keysList = emptyList()) { updatedKeys ->
//                    navigationController.updateKeys(updatedKeys)
                }
            })
        is Screen.LogSettings -> selectTab(navigationController = navigationController,
            appState = appState,
            index = 3,
            content = {
                appState.currentConfig?.let { config ->
                    LogSettingsTab(config = config) { updatedConfig ->
                        navigationController.saveConfig(updatedConfig)
                    }
                }
            })
        is Screen.AdvancedOptions -> selectTab(navigationController = navigationController,
            appState = appState,
            index = 4,
            content = {
                appState.currentConfig?.let { config ->
                    AdvancedOptionsTab()
                }
            })
        is Screen.Monitor -> MonitorScreen(
            config = appState.currentConfig,
            onBack = { navigationController.goBack() }
        )
        is Screen.HostSimulator -> HostSimulatorScreen(
            config = appState.currentConfig,
            onBack = { navigationController.goBack() }
        )
    }
}

@Composable
private fun selectTab(navigationController: NavigationController,index: Int,appState: GatewayConfigurationState,
                      content: @Composable () -> Unit){
    TabContainer(
        appState = appState,
        selectedTab = index,
        onSelectConfig = { navigationController.selectConfig(it) },
        onAddConfig = { navigationController.addNewConfig() },
        onDeleteConfig = { navigationController.deleteCurrentConfig() },
        onSaveAllConfigs = { navigationController.saveAllConfigs() },
        onOpenMonitor = { navigationController.openMonitor() },
        onOpenHostSimulator = { navigationController.openHostSimulator() },
        onTabSelected = { navigationController.selectTab(it) },
        content = content
    )
}