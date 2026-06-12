package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2.SectionCard

/**
 * Runtime settings — live, read-only-ish view of how the simulator is wired. To change anything,
 * the user goes back to the config screen. This tab exists so the running simulator's state is
 * visible without leaving the runtime.
 */
@Composable
fun RuntimeSettingsTab(controller: SimulatorController, config: APDUSimulatorConfig) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionCard(
            icon = Icons.Default.Settings,
            title = "Active wiring",
            subtitle = "Edit any of these on the config screen — they're frozen while connected.",
        ) {
            SettingRow(Icons.Default.Memory, "Mode", when (controller.mode) {
                TransportMode.LOOPBACK -> "Loopback (in-process)"
                TransportMode.PCSC -> "Read (PC/SC reader)"
                TransportMode.SERIAL -> "Emulate (USB-CDC → STM32)"
            })
            when (controller.mode) {
                TransportMode.LOOPBACK -> Unit
                TransportMode.PCSC -> SettingRow(
                    Icons.Default.Sensors,
                    "Reader",
                    controller.pcscReader ?: "(none — pick one in config)",
                    mono = true,
                )
                TransportMode.SERIAL -> {
                    SettingRow(
                        Icons.Default.Cable,
                        "Serial port",
                        controller.serialPort ?: "(none — pick one in config)",
                        mono = true,
                    )
                    SettingRow(
                        Icons.Default.Cable,
                        "Baud",
                        "${controller.serialBaud}",
                        mono = true,
                    )
                }
            }
            SettingRow(
                Icons.Default.CreditCard,
                "Card profile",
                controller.activeProfile?.let { "${it.name} · ${it.scheme.name} · ATR ${it.atr}" }
                    ?: if (controller.mode == TransportMode.PCSC) "(reader is the source of truth)"
                    else "(none — pick one in config)",
            )
        }

        SectionCard(
            icon = Icons.Default.Refresh,
            title = "Runtime actions",
            subtitle = "Forced operations that bypass the connect/disconnect flow.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { controller.reset(warm = false) },
                    enabled = controller.conn == ConnState.CONNECTED,
                ) { Text("Cold reset") }
                OutlinedButton(
                    onClick = { controller.reset(warm = true) },
                    enabled = controller.conn == ConnState.CONNECTED,
                ) { Text("Warm reset") }
                OutlinedButton(
                    onClick = { controller.exchanges.clear() },
                    enabled = controller.exchanges.isNotEmpty(),
                ) { Text("Clear trace") }
            }
        }
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            label,
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
        )
        Text(
            value,
            style = MaterialTheme.typography.body2,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        )
    }
}
