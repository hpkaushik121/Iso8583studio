package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.PcscReaders
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.SerialPorts
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField

/**
 * Combined "Mode & Transport" tab. The mode picks what role the simulator plays; the transport
 * settings underneath adapt to the selected mode. Choosing a different mode does not clear the
 * other modes' settings — switching back restores them.
 */
@Composable
fun ModeTransportTab(config: APDUSimulatorConfig, onConfigUpdate: (APDUSimulatorConfig) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionCard(
            icon = Icons.Default.SettingsEthernet,
            title = "Operating mode",
            subtitle = "Pick the role this simulator plays. The transport section below adapts.",
        ) {
            ModeOptionCard(
                selected = config.transportMode == "LOOPBACK",
                title = "Loopback (software-only)",
                description = "EMV runtime in-process against the active card profile. No hardware. Ideal for developing test plans and personalization.",
                icon = Icons.Default.Memory,
                onSelect = { onConfigUpdate(config.copy(transportMode = "LOOPBACK")) },
            )
            ModeOptionCard(
                selected = config.transportMode == "PCSC",
                title = "Reader (PC/SC) — drive a real card",
                description = "Studio acts as the terminal. Drives a physical card via a PC/SC reader (e.g. ACS ACR39U-I1). Card profile is informational only — the real card is the source of truth.",
                icon = Icons.Default.Sensors,
                onSelect = { onConfigUpdate(config.copy(transportMode = "PCSC")) },
            )
            ModeOptionCard(
                selected = config.transportMode == "SERIAL",
                title = "Card emulator (USB-CDC to STM32)",
                description = "Studio pushes APDU responses to the Nucleo-L432KC firmware over USB-CDC. The firmware emulates a contact card on the XCRFID pinboard, readable by an external POS terminal.",
                icon = Icons.Default.Cable,
                onSelect = { onConfigUpdate(config.copy(transportMode = "SERIAL")) },
            )
        }

        when (config.transportMode) {
            "LOOPBACK" -> SectionCard(
                icon = Icons.Default.SwapHoriz,
                title = "Transport — Loopback",
                subtitle = "Nothing to configure.",
            ) {
                Text(
                    "The runtime runs in this JVM. Active profile, terminal profile, and behavior rules are honoured directly.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }

            "PCSC" -> SectionCard(
                icon = Icons.Default.Sensors,
                title = "Transport — PC/SC reader",
                subtitle = "Pick a connected smart-card reader.",
            ) {
                var readers by remember { mutableStateOf(emptyList<String>()) }
                LaunchedEffect(Unit) { readers = PcscReaders.list() }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PickerDropdown(
                        label = "Reader",
                        value = config.pcscReaderName.ifBlank { null },
                        options = readers,
                        onPick = { onConfigUpdate(config.copy(pcscReaderName = it)) },
                        empty = "No PC/SC readers detected",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { readers = PcscReaders.list() }) {
                        Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Rescan")
                    }
                }
                Text(
                    if (readers.isEmpty()) "Tip: ensure pcscd is running on Linux, or that the system PC/SC service is enabled."
                    else "${readers.size} reader(s) available.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }

            "SERIAL" -> SectionCard(
                icon = Icons.Default.Cable,
                title = "Transport — STM32 / USB-CDC",
                subtitle = "Pick the serial port the firmware enumerated on.",
            ) {
                var ports by remember { mutableStateOf(emptyList<String>()) }
                LaunchedEffect(Unit) { ports = SerialPorts.list() }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PickerDropdown(
                            label = "Port",
                            value = config.serialPortName.ifBlank { null },
                            options = ports,
                            onPick = { onConfigUpdate(config.copy(serialPortName = it)) },
                            empty = "No serial ports detected",
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { ports = SerialPorts.list() }) {
                            Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Rescan")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PickerDropdown(
                            label = "Baud rate",
                            value = config.serialBaudRate.toString(),
                            options = listOf("9600", "19200", "38400", "57600", "115200", "230400", "460800", "921600"),
                            onPick = { onConfigUpdate(config.copy(serialBaudRate = it.toInt())) },
                            empty = "—",
                            modifier = Modifier.width(220.dp),
                        )
                        FixedOutlinedTextField(
                            value = config.atr,
                            onValueChange = { onConfigUpdate(config.copy(atr = it.uppercase())) },
                            label = { Text("ATR override (hex, blank = use card profile)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(
                    "On macOS the device shows up as /dev/cu.usbmodemXXXX after the firmware boots. The default baud is 115200 but the firmware framing is binary — baud only affects throughput, not protocol.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
