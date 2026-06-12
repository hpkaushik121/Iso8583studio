package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.PcscReaders
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.SerialPorts
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig

/**
 * Transport tab — picks the concrete reader / port for the selected mode. Loopback shows nothing
 * to configure.
 */
@Composable
fun TransportTab(config: APDUSimulatorConfig, onConfigUpdate: (APDUSimulatorConfig) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (config.transportMode) {
            "LOOPBACK" -> SectionCard(
                title = "Loopback",
                subtitle = "Nothing to configure — the runtime runs in this JVM.",
            ) {
                Text(
                    "Switch to Reader or Card emulator on the Mode tab to attach hardware.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }

            "PCSC" -> SectionCard(
                title = "PC/SC reader",
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
            }

            "SERIAL" -> SectionCard(
                title = "USB-CDC port",
                subtitle = "Pick the port the STM32 firmware is enumerated on.",
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
                    PickerDropdown(
                        label = "Baud rate",
                        value = config.serialBaudRate.toString(),
                        options = listOf("9600", "19200", "38400", "57600", "115200", "230400"),
                        onPick = { onConfigUpdate(config.copy(serialBaudRate = it.toInt())) },
                        empty = "—",
                        modifier = Modifier.width(220.dp),
                    )
                }
            }

            else -> SectionCard(title = "Unknown mode", subtitle = config.transportMode) {}
        }
    }
}

@Composable
internal fun PickerDropdown(
    label: String,
    value: String?,
    options: List<String>,
    onPick: (String) -> Unit,
    empty: String,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.caption)
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                value ?: empty,
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Monospace,
            )
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(onClick = { open = false }, enabled = false) { Text(empty) }
            } else {
                options.forEach { opt ->
                    DropdownMenuItem(onClick = { onPick(opt); open = false }) {
                        Text(opt, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
