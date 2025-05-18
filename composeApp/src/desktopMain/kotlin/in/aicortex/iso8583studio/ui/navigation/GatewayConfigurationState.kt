package `in`.aicortex.iso8583studio.ui.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.ConnectionStatus
import `in`.aicortex.iso8583studio.data.model.GatewayConfig

/**
 * Application state for the Gateway Configuration
 */
data class GatewayConfigurationState(
    val configList: MutableState<List<GatewayConfig>> = mutableStateOf(emptyList()),
    var selectedConfigIndex: Int = -1,
    val selectedTabIndex: Int = 0,
    var panelWidth: Dp = 300.dp,
    var connectionStatus: ConnectionStatus? = null
) {
    val currentConfig: GatewayConfig?
        get() = if (selectedConfigIndex >= 0 && selectedConfigIndex < configList.value.size) {
            configList.value[selectedConfigIndex]
        } else {
            null
        }
}

