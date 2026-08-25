package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Usb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.ProfileStore
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.PcscReaders
import `in`.aicortex.iso8583studio.domain.store.JsonDirStore
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField

/**
 * Where card data comes from, and where the terminal's online authorization goes.
 *
 * The three card sources are the ones already implemented in `apduSimulatorService.transport`, so
 * this tab is wiring rather than new machinery: LOOPBACK answers APDUs from a software card, PCSC
 * from a real card in a reader attached to *this machine*, SERIAL from the STM32 firmware.
 */
@Composable
fun TransactionTab(config: POSSimulatorConfig, onConfigUpdate: (POSSimulatorConfig) -> Unit) {
    val profiles = remember {
        runCatching {
            ProfileStore(JsonDirStore.appDir("card-profiles")).list()
        }.getOrDefault(emptyList())
    }
    val readers = remember { runCatching { PcscReaders.list() }.getOrDefault(emptyList()) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

        ConfigSection("Card source", Icons.Default.CreditCard) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "The emulator cannot see host USB. A physical card is read by this desktop app " +
                        "and its APDUs are relayed into the device, which is what makes a real card " +
                        "in a reader on your desk work for an app running in the emulator.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )

                CardSourceOption(
                    config, onConfigUpdate, "LOOPBACK",
                    "Software card",
                    "APDUs answered in-process by the card runtime. No hardware needed.",
                )
                CardSourceOption(
                    config, onConfigUpdate, "PCSC",
                    "Real card via USB reader",
                    if (readers.isEmpty()) "No PC/SC readers detected on this machine."
                    else "Readers: ${readers.joinToString(", ")}",
                )
                CardSourceOption(
                    config, onConfigUpdate, "SERIAL",
                    "STM32 card emulator",
                    "USB-CDC link to the firmware in firmware/stm32-card.",
                )
            }
        }

        when (config.transportMode) {
            "LOOPBACK" -> ConfigSection("Software card profile", Icons.Default.Memory) {
                if (profiles.isEmpty()) {
                    PosNoticeStrip(
                        "No card profiles found in ~/.iso8583studio/card-profiles. Create one in the " +
                            "APDU Simulator, then pick it here.",
                        WarningYellow,
                    )
                } else {
                    ConfigDropdown(
                        label = "Card profile",
                        currentValue = profiles.firstOrNull { it.id == config.activeProfileId }
                            ?.let { "${it.name}  (${it.scheme.name})" } ?: "",
                        options = profiles.map { "${it.name}  (${it.scheme.name})" },
                        onValueChange = { picked ->
                            profiles.firstOrNull { "${it.name}  (${it.scheme.name})" == picked }
                                ?.let { onConfigUpdate(config.copy(activeProfileId = it.id)) }
                        },
                    )
                }
            }

            "PCSC" -> ConfigSection("PC/SC reader", Icons.Default.Usb) {
                if (readers.isEmpty()) {
                    PosNoticeStrip("No readers detected. Plug one in and reopen this tab.", WarningYellow)
                } else {
                    ConfigDropdown(
                        label = "Reader",
                        currentValue = config.pcscReaderName,
                        options = readers,
                        onValueChange = { onConfigUpdate(config.copy(pcscReaderName = it)) },
                    )
                }
            }

            "SERIAL" -> ConfigSection("Serial port", Icons.Default.Usb) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FixedOutlinedTextField(
                        value = config.serialPortName,
                        onValueChange = { onConfigUpdate(config.copy(serialPortName = it)) },
                        label = { Text("Port") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    FixedOutlinedTextField(
                        value = config.serialBaudRate.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { baud -> onConfigUpdate(config.copy(serialBaudRate = baud)) }
                        },
                        label = { Text("Baud rate") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }

        ConfigSection("Acquirer host", Icons.Default.Dns) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Where the app's online authorization is sent. Point this at a Host Simulator " +
                        "session in this same app to see both ends of the 0200/0210.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FixedOutlinedTextField(
                        value = config.serverAddress,
                        onValueChange = { onConfigUpdate(config.copy(serverAddress = it)) },
                        label = { Text("Host") },
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                    )
                    FixedOutlinedTextField(
                        value = config.serverPort.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { p -> onConfigUpdate(config.copy(serverPort = p)) }
                        },
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FixedOutlinedTextField(
                        value = config.terminalid.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { v -> onConfigUpdate(config.copy(terminalid = v)) }
                        },
                        label = { Text("Terminal ID") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    FixedOutlinedTextField(
                        value = config.merchantid.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { v -> onConfigUpdate(config.copy(merchantid = v)) }
                        },
                        label = { Text("Merchant ID") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardSourceOption(
    config: POSSimulatorConfig,
    onConfigUpdate: (POSSimulatorConfig) -> Unit,
    mode: String,
    title: String,
    subtitle: String,
) {
    PosOptionCard(
        selected = config.transportMode == mode,
        title = title,
        subtitle = subtitle,
        onSelect = { onConfigUpdate(config.copy(transportMode = mode)) },
        modifier = Modifier.fillMaxWidth(),
    )
}
