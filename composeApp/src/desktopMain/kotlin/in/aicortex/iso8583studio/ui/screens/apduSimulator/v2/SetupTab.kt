package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardProfile
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.ProfileStore
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.PcscReaders
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.SerialPorts

/**
 * Setup tab: pick mode, configure transport, pick a profile, connect.
 */
@Composable
fun SetupTab(controller: SimulatorController, store: ProfileStore) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ModeCard(controller)
        TransportCard(controller)
        ProfileCard(controller, store)
        ConnectionCard(controller)
    }
}

@Composable
private fun ModeCard(controller: SimulatorController) {
    SectionCard(title = "Mode", subtitle = "What is connected to the Studio?") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ModeOption(
                selected = controller.mode == TransportMode.LOOPBACK,
                title = "Loopback (software-only)",
                description = "Studio runs the card runtime in-process against an active profile. No hardware required.",
                onSelect = { controller.mode = TransportMode.LOOPBACK },
                disabled = controller.conn != ConnState.IDLE,
            )
            ModeOption(
                selected = controller.mode == TransportMode.PCSC,
                title = "Reader (PC/SC, e.g. ACS ACR39U)",
                description = "Studio drives a real card via a USB smart-card reader. Use to read or test physical cards.",
                onSelect = { controller.mode = TransportMode.PCSC },
                disabled = controller.conn != ConnState.IDLE,
            )
            ModeOption(
                selected = controller.mode == TransportMode.SERIAL,
                title = "Card emulator (STM32 over USB-CDC)",
                description = "Studio sends APDUs to the Nucleo-L432KC firmware. The firmware acts as a card on the XCRFID pinboard, readable by an external POS terminal.",
                onSelect = { controller.mode = TransportMode.SERIAL },
                disabled = controller.conn != ConnState.IDLE,
            )
        }
    }
}

@Composable
private fun ModeOption(
    selected: Boolean,
    title: String,
    description: String,
    onSelect: () -> Unit,
    disabled: Boolean,
) {
    Row(verticalAlignment = Alignment.Top) {
        RadioButton(selected = selected, onClick = onSelect, enabled = !disabled)
        Spacer(Modifier.width(4.dp))
        Column(Modifier.padding(top = 12.dp)) {
            Text(title, style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun TransportCard(controller: SimulatorController) {
    SectionCard(
        title = "Transport",
        subtitle = when (controller.mode) {
            TransportMode.LOOPBACK -> "Loopback has no transport configuration."
            TransportMode.PCSC -> "Pick a connected smart-card reader."
            TransportMode.SERIAL -> "Pick the serial port the STM32 is enumerated on."
        },
    ) {
        when (controller.mode) {
            TransportMode.LOOPBACK -> Text(
                "Nothing to configure — the runtime runs in this JVM.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
            TransportMode.PCSC -> {
                var readers by remember { mutableStateOf(emptyList<String>()) }
                LaunchedListInit { readers = PcscReaders.list() }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PickerDropdown(
                        label = "Reader",
                        value = controller.pcscReader,
                        options = readers,
                        onPick = { controller.pcscReader = it },
                        empty = "No PC/SC readers detected",
                        modifier = Modifier.weight(1f),
                        enabled = controller.conn == ConnState.IDLE,
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { readers = PcscReaders.list() }) {
                        Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Rescan")
                    }
                }
            }
            TransportMode.SERIAL -> {
                var ports by remember { mutableStateOf(emptyList<String>()) }
                LaunchedListInit { ports = SerialPorts.list() }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PickerDropdown(
                            label = "Port",
                            value = controller.serialPort,
                            options = ports,
                            onPick = { controller.serialPort = it },
                            empty = "No serial ports detected",
                            modifier = Modifier.weight(1f),
                            enabled = controller.conn == ConnState.IDLE,
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { ports = SerialPorts.list() }) {
                            Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Rescan")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PickerDropdown(
                            label = "Baud",
                            value = controller.serialBaud.toString(),
                            options = listOf("9600", "19200", "38400", "57600", "115200", "230400"),
                            onPick = { controller.serialBaud = it.toInt() },
                            empty = "—",
                            modifier = Modifier.width(180.dp),
                            enabled = controller.conn == ConnState.IDLE,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(controller: SimulatorController, store: ProfileStore) {
    val needsProfile = controller.mode == TransportMode.LOOPBACK || controller.mode == TransportMode.SERIAL
    val title = if (controller.mode == TransportMode.PCSC) "Card profile (read from reader — informational only)"
                else "Card profile to emulate"
    SectionCard(title = "Profile", subtitle = title) {
        var profiles by remember { mutableStateOf(emptyList<CardProfile>()) }
        LaunchedListInit { profiles = store.list() }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PickerDropdown(
                label = "Active profile",
                value = controller.activeProfile?.let { "${it.name}  (${it.scheme.name})" },
                options = profiles.map { "${it.name}  (${it.scheme.name})" },
                onPick = { picked ->
                    controller.activeProfile = profiles.first { "${it.name}  (${it.scheme.name})" == picked }
                },
                empty = "No profiles — use the Profiles tab to add one",
                modifier = Modifier.weight(1f),
                enabled = needsProfile && controller.conn == ConnState.IDLE,
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { profiles = store.list() }) {
                Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Refresh")
            }
        }
        controller.activeProfile?.let { p ->
            Spacer(Modifier.width(0.dp))
            Text(
                buildString {
                    append("AIDs: ")
                    append(p.applications.joinToString { it.aid + " (" + it.label + ")" })
                },
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun ConnectionCard(controller: SimulatorController) {
    SectionCard(title = "Connection", subtitle = null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { controller.connect() },
                enabled = controller.conn == ConnState.IDLE && controller.canConnect(),
            ) { Text("Connect") }
            OutlinedButton(
                onClick = { controller.reset() },
                enabled = controller.conn == ConnState.CONNECTED,
            ) {
                Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Reset")
            }
            OutlinedButton(
                onClick = { controller.disconnect() },
                enabled = controller.conn == ConnState.CONNECTED || controller.conn == ConnState.ERROR,
            ) {
                Icon(Icons.Default.PowerSettingsNew, null); Spacer(Modifier.width(4.dp)); Text("Disconnect")
            }
            Spacer(Modifier.weight(1f))
            StatusChip(controller.conn)
        }
        if (controller.atrHex.isNotEmpty()) {
            Text(
                "ATR: ${controller.atrHex}",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
            )
        }
        controller.lastError?.let {
            Text(
                "Error: $it",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.error,
            )
        }
    }
}

@Composable
private fun StatusChip(state: ConnState) {
    val (label, color) = when (state) {
        ConnState.IDLE -> "idle" to MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        ConnState.CONNECTING -> "connecting…" to MaterialTheme.colors.primary
        ConnState.CONNECTED -> "connected" to MaterialTheme.colors.primary
        ConnState.ERROR -> "error" to MaterialTheme.colors.error
    }
    Card(
        elevation = 0.dp,
        backgroundColor = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.caption,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun SectionCard(title: String, subtitle: String?, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }
            content()
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
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.caption)
        OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
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

/**
 * Tiny helper to run a one-shot init in a composable without explicit LaunchedEffect ceremony.
 */
@Composable
private fun LaunchedListInit(block: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { block() }
}
