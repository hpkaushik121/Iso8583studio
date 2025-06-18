package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig

@Composable
fun SecurityTab(config: POSSimulatorConfig, onConfigUpdate: (POSSimulatorConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ConfigSection(title = "Encryption and Security", icon = Icons.Default.VpnKey) {
            ConfigMultiSelectChipGroup(
                label = "Security Protocols",
                allOptions = listOf(
                    "End-to-end encryption (E2EE)",
                    "Point-to-point encryption (P2PE)",
                    "Triple DES encryption",
                    "AES encryption",
                    "Token substitution"
                ),
                selectedOptions = config.encryptionSecurity,
                onSelectionChanged = { onConfigUpdate(config.copy(encryptionSecurity = it)) }
            )
        }
        ConfigSection(title = "Authentication Methods", icon = Icons.Default.Fingerprint) {
            ConfigDropdown(
                label = "Cardholder Verification",
                currentValue = config.authMethods,
                options = listOf(
                    "PIN verification",
                    "Signature capture and verification",
                    "Biometric authentication",
                    "No cardholder verification method (No CVM)"
                ),
                onValueChange = { onConfigUpdate(config.copy(authMethods = it)) }
            )
        }
    }
}