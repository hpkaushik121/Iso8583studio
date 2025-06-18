package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig

@Composable
fun TransactionTab(
    config: POSSimulatorConfig,
    onConfigUpdate: (POSSimulatorConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ConfigSection(title = "Terminal Capabilities", icon = Icons.Default.Terminal) {
            ConfigMultiSelectChipGroup(
                label = "Processing Modes",
                allOptions = listOf(
                    "Offline transaction processing",
                    "Online-only processing",
                    "Store-and-forward capability",
                    "Void and refund processing",
                    "Partial approval support"
                ),
                selectedOptions = config.terminalCapabilities,
                onSelectionChanged = { onConfigUpdate(config.copy(terminalCapabilities = it)) }
            )
        }
        ConfigSection(title = "Payment Method Support", icon = Icons.Default.Payment) {
            ConfigMultiSelectChipGroup(
                label = "Accepted Methods",
                allOptions = listOf(
                    "Credit card processing",
                    "Debit card processing",
                    "EBT/SNAP benefits",
                    "Gift card processing",
                    "Digital wallet support"
                ),
                selectedOptions = config.paymentMethods.map { it.name }.toSet(),
                onSelectionChanged = { /*onConfigUpdate(config.copy(paymentMethods = PaymentMethod.valueOf(it)))*/ }
            )
        }
    }
}