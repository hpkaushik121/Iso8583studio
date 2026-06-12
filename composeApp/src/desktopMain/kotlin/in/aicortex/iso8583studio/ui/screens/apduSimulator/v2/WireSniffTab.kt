package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2.SectionCard

/**
 * Wire Sniff — TAYAL 24 MHz logic-analyzer capture overlay. Stub for now: imports a sigrok `.sr`
 * or CSV export and lines it up against the APDU exchange log so we can verify the firmware
 * actually emitted what the runtime claims to have sent on I/O / CLK / RST / VCC.
 */
@Composable
fun WireSniffTab() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionCard(
            icon = Icons.Default.Wifi,
            title = "Logic-analyzer capture",
            subtitle = "TAYAL 24 MHz — sniff I/O, CLK, RST, VCC and align against the APDU exchange log.",
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { /* TODO step 5 */ }, enabled = false) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Import .sr / .csv")
                }
                Text(
                    "Coming in step 5 of the runtime redesign.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        SectionCard(
            icon = Icons.Default.Wifi,
            title = "Why this exists",
            subtitle = null,
        ) {
            Text(
                buildString {
                    appendLine("APDU-level logs only prove what the runtime *thinks* happened. To certify the firmware,")
                    appendLine("we need to compare the byte stream the analyzer captured on the ISO-7816 wire against")
                    appendLine("what the trace log claims was exchanged. Discrepancies — wrong PPS, wrong ETU, missed")
                    append("PTS — only show up at the wire level.")
                },
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                fontFamily = FontFamily.Default,
            )
        }
    }
}
