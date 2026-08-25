package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceFeature
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField

/**
 * Peripherals and PIN security, driven by the resolved terminal variant.
 *
 * Replaces the old free-text "security protocols" chip list, which nothing read. What is shown here
 * is what the emulated device will actually expose to the payment SDK, and it changes when you pick
 * a different variant — a printer-less SKU says so instead of offering printer settings.
 */
@Composable
fun SecurityTab(config: POSSimulatorConfig, onConfigUpdate: (POSSimulatorConfig) -> Unit) {
    val resolved = PosConfigEditing.resolved(config)

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

        if (resolved == null) {
            PosNoticeStrip(
                "No terminal selected. Pick a model on the Device tab.",
                MaterialTheme.colors.error,
            )
            return@Column
        }

        val terminal = resolved.terminal

        ConfigSection("Peripherals", Icons.Default.Widgets) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Determined by the selected model and variant. These drive which tabs the " +
                        "running simulator shows and which SDK calls the device host answers.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
                DeviceFeature.entries.forEach { feature ->
                    PosSpecRow(feature.label, if (terminal.has(feature)) "Present" else "—")
                }
            }
        }

        if (terminal.has(DeviceFeature.PRINTER) && terminal.printer.present) {
            ConfigSection("Thermal printer", Icons.Default.Print) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PosSpecRow("Dots per line", terminal.printer.dotsPerLine.toString(), mono = true)
                    PosSpecRow("Paper width", "${terminal.printer.paperWidthMm} mm", mono = true)
                    PosSpecRow("Greyscale", if (terminal.printer.grayscale) "Yes" else "No")
                }
            }
        }

        if (terminal.has(DeviceFeature.SCANNER)) {
            ConfigSection("Barcode scanner", Icons.Default.QrCodeScanner) {
                Text(
                    "Scans are injected from the running simulator rather than read from a camera.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        if (terminal.has(DeviceFeature.PED)) {
            ConfigSection("PIN entry (PED)", Icons.Default.Dialpad) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PosNoticeStrip(
                        "Emulated PIN entry is a test artifact. There is no secure processor here " +
                            "and this is not PCI-PTS anything — never use it with live PANs or keys.",
                        WarningYellow,
                        icon = Icons.Default.Warning,
                    )
                    PosSpecRow(
                        "PIN block formats",
                        terminal.ped.supportedPinBlockFormats.joinToString(", "),
                        mono = true,
                    )
                    PosSpecRow("Key slots", terminal.ped.keySlots.toString(), mono = true)
                    PosSpecRow("DUKPT", if (terminal.ped.supportsDukpt) "Supported" else "No")
                    PosSpecRow("Offline PIN", if (terminal.ped.supportsOfflinePin) "Supported" else "No")

                    FixedOutlinedTextField(
                        value = config.pinpadConfig.encryptionKey,
                        onValueChange = { key ->
                            onConfigUpdate(
                                config.copy(pinpadConfig = config.pinpadConfig.copy(encryptionKey = key))
                            )
                        },
                        label = { Text("Terminal PIN key (TPK) / BDK — test keys only") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }
    }
}
