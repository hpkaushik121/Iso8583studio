package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow

/**
 * One-line transport summary shown in the compact connection bar.
 */
internal fun transportSummary(c: SimulatorController): String = when (c.mode) {
    TransportMode.LOOPBACK -> "Loopback"
    TransportMode.PCSC -> "Read · ${c.pcscReader ?: "(no reader)"}"
    TransportMode.SERIAL -> "Emulate · ${c.serialPort ?: "(no port)"} @ ${c.serialBaud}"
}

/**
 * Host-simulator-styled compact bars used at the top of Card Session tabs.
 *
 *   - [CompactConnectionBar] mirrors the host sim's `CompactStatusBar`: a thin tinted strip,
 *     10sp text, 4dp vertical padding. Only shown when the transport is connected.
 *   - [CompactCardControlPanel] mirrors `CompactControlPanel`: a single dense row with all the
 *     connect/reset/disconnect/clear controls, 32dp button height, caption text.
 */

@Composable
fun CompactConnectionBar(
    state: ConnState,
    transport: String,
    profile: String,
    atrHex: String,
    exchanges: Int,
) {
    val (label, tint) = when (state) {
        ConnState.IDLE -> "Idle" to MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        ConnState.CONNECTING -> "Connecting…" to WarningYellow
        ConnState.CONNECTED -> "Connected" to SuccessGreen
        ConnState.ERROR -> "Error" to MaterialTheme.colors.error
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tint.copy(alpha = 0.08f),
        elevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(shape = CircleShape, color = tint, modifier = Modifier.size(5.dp)) {}
            Text(
                label,
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Medium,
                color = tint,
            )
            Text(
                transport,
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colors.onSurface,
            )
            if (atrHex.isNotEmpty()) {
                Text(
                    "ATR $atrHex",
                    style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                profile,
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
            )
            Text(
                "Exchanges: $exchanges",
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
            )
        }
    }
}

/**
 * One-row control panel: Connect/Stop, Reset, Disconnect, Clear, plus an optional trailing slot
 * for mode-specific extras (Hold switch in passive mode, status text in active mode).
 */
@Composable
fun CompactCardControlPanel(
    state: ConnState,
    canConnect: Boolean,
    onConnect: () -> Unit,
    onReset: () -> Unit,
    onDisconnect: () -> Unit,
    onClear: () -> Unit,
    lastError: String?,
    trailing: @Composable () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val isConnected = state == ConnState.CONNECTED
                // Allow retry from ERROR state — the controller's connect() handles it.
                val connectEnabled = canConnect && (state == ConnState.IDLE || state == ConnState.ERROR)
                Button(
                    onClick = if (isConnected) onDisconnect else onConnect,
                    enabled = if (isConnected) true else connectEnabled,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isConnected) MaterialTheme.colors.error else MaterialTheme.colors.primary,
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isConnected) "Disconnect"
                        else if (state == ConnState.ERROR) "Retry"
                        else "Connect",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium,
                    )
                }
                OutlinedButton(
                    onClick = onReset,
                    enabled = isConnected,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset", style = MaterialTheme.typography.caption)
                }
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.caption)
                }
                if (state == ConnState.IDLE && !canConnect) {
                    Text(
                        "Pick a profile or transport in config",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
                lastError?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colors.error.copy(alpha = 0.15f),
                    ) {
                        Text(
                            err,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) { trailing() }
        }
    }
}

/**
 * The "Hold next APDU" trailing widget for the passive mode control panel.
 */
@Composable
fun HoldSwitchTrailing(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            Icons.Default.Bolt,
            null,
            tint = if (checked) WarningYellow else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.height(20.dp),
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
        )
        Text("Hold next APDU", style = MaterialTheme.typography.caption)
    }
}
