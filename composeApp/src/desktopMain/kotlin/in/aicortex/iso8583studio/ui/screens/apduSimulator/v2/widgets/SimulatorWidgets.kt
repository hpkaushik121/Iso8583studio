package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Usb
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.Scheme
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2.ConnState
import `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2.TransportMode

/**
 * Shared visual atoms for the v2 simulator UI. Keeps tabs consistent: same status pill, same
 * scheme badge, same section header.
 */

/** Connection-state pill with a colored leading dot. Animatable color. */
@Composable
fun StatusPill(state: ConnState, modifier: Modifier = Modifier) {
    val (label, color) = when (state) {
        ConnState.IDLE -> "idle" to MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
        ConnState.CONNECTING -> "connecting…" to PrimaryBlue
        ConnState.CONNECTED -> "connected" to SuccessGreen
        ConnState.ERROR -> "error" to ErrorRed
    }
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Scheme-coloured rounded badge — Visa blue, MC red/orange, Amex teal, RuPay green, etc. */
@Composable
fun SchemeBadge(scheme: Scheme, modifier: Modifier = Modifier) {
    val color = schemeColor(scheme)
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.CreditCard, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(scheme.name, style = MaterialTheme.typography.caption, color = color, fontWeight = FontWeight.SemiBold)
    }
}

fun schemeColor(scheme: Scheme): Color = when (scheme) {
    Scheme.VISA -> Color(0xFF1A1F71)
    Scheme.MASTERCARD -> Color(0xFFEB001B)
    Scheme.AMEX -> Color(0xFF2E77BB)
    Scheme.RUPAY -> Color(0xFF098240)
    Scheme.UNIONPAY -> Color(0xFFE21836)
    Scheme.JCB -> Color(0xFF0E4C96)
    Scheme.DISCOVER -> Color(0xFFFF6000)
    Scheme.OTHER -> Color(0xFF607D8B)
}

/** Icon vector for a transport mode — used in tab labels and hero cards. */
fun modeIcon(mode: TransportMode): ImageVector = when (mode) {
    TransportMode.LOOPBACK -> Icons.Filled.Memory
    TransportMode.PCSC -> Icons.Filled.Usb
    TransportMode.SERIAL -> Icons.Filled.DeveloperBoard
}

fun modeColor(mode: TransportMode): Color = when (mode) {
    TransportMode.LOOPBACK -> Color(0xFF7E57C2)        // muted purple — software
    TransportMode.PCSC -> PrimaryBlue                  // brand blue — reader
    TransportMode.SERIAL -> WarningYellow              // amber — hardware bridge
}

fun modeLabel(mode: TransportMode): String = when (mode) {
    TransportMode.LOOPBACK -> "Loopback"
    TransportMode.PCSC -> "PC/SC reader"
    TransportMode.SERIAL -> "STM32 emulator"
}

/** Section header used inside cards: small icon + bold title + optional caption. */
@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    accent: Color = MaterialTheme.colors.primary,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                )
            }
        }
    }
}

/** Hero card with a tinted left rail — used for the Overview "current mode" summary. */
@Composable
fun ModeHeroCard(
    mode: TransportMode,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    rightSlot: (@Composable () -> Unit)? = null,
    body: @Composable () -> Unit,
) {
    val color = modeColor(mode)
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).background(color))
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(modeIcon(mode), null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                        )
                    }
                    rightSlot?.invoke()
                }
                body()
            }
        }
    }
}

/** Empty-state placeholder for tabs with nothing to show yet. */
@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
            }
            Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
            )
        }
    }
}

/** Compact key-value row used for summary blocks. */
@Composable
fun KeyValueRow(label: String, value: String, mono: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(140.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.caption,
            fontFamily = if (mono) androidx.compose.ui.text.font.FontFamily.Monospace else androidx.compose.ui.text.font.FontFamily.Default,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
        )
    }
}

/** Outline-tinted card with a colored top stripe. Used as a generic accented panel. */
@Composable
fun AccentCard(
    accent: Color = MaterialTheme.colors.primary,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(accent.copy(alpha = 0.20f)).padding(0.dp).size(2.dp))
            Box(modifier = Modifier.padding(14.dp)) { content() }
        }
    }
}

@Suppress("unused") // shared with config tabs that may reuse this icon set
val genericModeIcons: Map<TransportMode, ImageVector> = mapOf(
    TransportMode.LOOPBACK to Icons.Filled.Memory,
    TransportMode.PCSC to Icons.Filled.Cable,
    TransportMode.SERIAL to Icons.Filled.Computer,
)
