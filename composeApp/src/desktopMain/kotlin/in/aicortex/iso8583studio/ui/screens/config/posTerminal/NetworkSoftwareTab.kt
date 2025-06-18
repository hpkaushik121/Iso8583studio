package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig

@Composable
fun NetworkSoftwareTab(
    config: POSSimulatorConfig,
    onConfigUpdate: (POSSimulatorConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ConfigSection(title = "Connectivity Options", icon = Icons.Default.Wifi) {
            ConfigDropdown(
                label = "Primary Connection",
                currentValue = config.connectivity,
                options = listOf(
                    "Ethernet/LAN connection",
                    "Wireless/Wi-Fi connectivity",
                    "Cellular/mobile data connection",
                    "Dial-up modem connection"
                ),
                onValueChange = { onConfigUpdate(config.copy(connectivity = it)) }
            )
        }
        ConfigSection(title = "Software and Application", icon = Icons.Default.DeveloperMode) {
            ConfigDropdown(
                label = "Operating System",
                currentValue = config.osType,
                options = listOf("Proprietary OS", "Linux-based", "Android", "Windows CE"),
                onValueChange = { onConfigUpdate(config.copy(osType = it)) }
            )
        }
    }
}