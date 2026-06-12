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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2.SectionCard

/**
 * L3 Report — generate a certified-format report from the most recent test-plan run. Initial
 * scope is Visa VCPS and Mastercard M/Chip; other schemes can run plans but skip the certified
 * export until their report templates are in.
 */
@Composable
fun L3ReportTab(config: APDUSimulatorConfig) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionCard(
            icon = Icons.Default.Verified,
            title = "Certification report",
            subtitle = "Export the most recent test-plan run in the format required by the scheme.",
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { /* TODO step 6 */ }, enabled = false) {
                    Icon(Icons.Default.Description, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Export Visa VCPS")
                }
                OutlinedButton(onClick = { /* TODO step 6 */ }, enabled = false) {
                    Icon(Icons.Default.Description, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Export M/Chip")
                }
                Text(
                    "Coming in step 6 of the runtime redesign.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        SectionCard(
            icon = Icons.Default.Verified,
            title = "What the report contains",
            subtitle = null,
        ) {
            Text(
                buildString {
                    appendLine("• Per-test-case verdict (PASS / FAIL) with the expected vs. observed values.")
                    appendLine("• Full APDU transcript per case, hex + parsed TLV.")
                    appendLine("• Card profile fingerprint (PAN-suffix, AID list, key IDs — no UDK material).")
                    appendLine("• Terminal profile snapshot (TACs, floor limits, CAPK indices, kernel choice).")
                    appendLine("• Wire-level sniff alignment (when a logic-analyzer capture is attached).")
                    appendLine()
                    append("Active config for this report: ${config.name.ifBlank { config.id }}")
                },
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                fontFamily = FontFamily.Default,
            )
        }
    }
}
