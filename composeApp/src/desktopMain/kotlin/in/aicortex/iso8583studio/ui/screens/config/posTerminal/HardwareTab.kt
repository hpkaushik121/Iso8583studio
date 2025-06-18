package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Print
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig

@Composable
fun HardwareTab(config: POSSimulatorConfig, onConfigUpdate: (POSSimulatorConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ConfigSection(title = "PIN Entry Options", icon = Icons.Default.Dialpad) {
            ConfigDropdown(
                label = "PIN Pad Type",
                currentValue = config.pinEntryOptions,
                options = listOf(
                    "Integrated PIN pad",
                    "External PIN pad",
                    "Soft PIN pad",
                    "No PIN pad"
                ),
                onValueChange = { onConfigUpdate(config.copy(pinEntryOptions = it)) }
            )
        }
        ConfigSection(title = "Card Reader Types", icon = Icons.Default.CreditCard) {
            ConfigDropdown(
                label = "Reader Configuration",
                currentValue = config.cardReaderTypes,
                options = listOf(
                    "Magnetic stripe reader (MSR)",
                    "EMV chip card reader (contact)",
                    "Triple-head reader (MSR + chip + contactless)"
                ),
                onValueChange = { onConfigUpdate(config.copy(cardReaderTypes = it)) }
            )
        }
        ConfigSection(title = "Display Configuration", icon = Icons.Default.DesktopWindows) {
            ConfigDropdown(
                label = "Display Setup",
                currentValue = config.displayConfig,
                options = listOf(
                    "Merchant-only display",
                    "Dual displays (merchant + customer)",
                    "Touch screen capability"
                ),
                onValueChange = { onConfigUpdate(config.copy(displayConfig = it)) }
            )
        }
        ConfigSection(title = "Receipt Printing", icon = Icons.Default.Print) {
            ConfigDropdown(
                label = "Printer Type",
                currentValue = config.receiptPrinting,
                options = listOf(
                    "Thermal receipt printer",
                    "Impact dot-matrix printer",
                    "No printer (electronic receipts only)"
                ),
                onValueChange = { onConfigUpdate(config.copy(receiptPrinting = it)) }
            )
        }
    }
}