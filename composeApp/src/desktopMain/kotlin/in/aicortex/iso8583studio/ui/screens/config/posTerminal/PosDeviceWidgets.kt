package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DalStatus
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.SpecConfidence
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.SpecProvenance
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow

/**
 * Small shared pieces for the POS device picker.
 *
 * Deliberately built from the same primitives as the rest of the app: tinted `Surface` pills at
 * 12–15% alpha, `caption`/`overline` typography, `RoundedCornerShape(6.dp)`. Semantic colours are
 * used as tints, never as fills.
 */

/** A compact tinted pill, the shape used for status throughout the app. */
@Composable
fun PosPill(
    text: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Medium,
                color = color,
            )
        }
    }
}

/**
 * Shows how much a spec can be trusted.
 *
 * Vendor datasheets publish `5" HD` but not the exact pixel dimensions or density, so an unverified
 * entry must never be presented as fact — only an adb device probe produces
 * [SpecConfidence.VERIFIED_FROM_DEVICE].
 */
@Composable
fun ProvenanceBadge(provenance: SpecProvenance, modifier: Modifier = Modifier) {
    val (color, icon) = when (provenance.confidence) {
        SpecConfidence.VERIFIED_FROM_DEVICE -> SuccessGreen to Icons.Default.CheckCircle
        SpecConfidence.FROM_DATASHEET -> WarningYellow to Icons.Default.Info
        SpecConfidence.PROVISIONAL -> WarningYellow to Icons.Default.Warning
    }
    PosPill(provenance.confidence.label, color, icon, modifier)
}

/** Whether the in-AVD device host can serve this vendor's SDK yet, or only its hardware shape. */
@Composable
fun DalStatusBadge(status: DalStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        DalStatus.IMPLEMENTED -> SuccessGreen
        DalStatus.IN_PROGRESS -> MaterialTheme.colors.primary
        DalStatus.NOT_IMPLEMENTED -> MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
    }
    PosPill(status.label, color, modifier = modifier)
}

/**
 * A selectable tile. Mirrors `ModeOptionCard` from the APDU simulator config: fills with `primary`
 * and flips content to white when selected, with a trailing radio.
 */
@Composable
fun PosOptionCard(
    selected: Boolean,
    title: String,
    subtitle: String?,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    badges: @Composable (Row0: Unit) -> Unit = {},
) {
    Surface(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        border = if (selected) null else ButtonDefaults.outlinedBorder,
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else MaterialTheme.colors.onSurface,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.caption,
                        color = if (selected) Color.White.copy(alpha = 0.85f)
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { badges(Unit) }
            }
            RadioButton(selected = selected, onClick = onSelect, modifier = Modifier.size(20.dp))
        }
    }
}

/** Label/value row with a fixed label column; monospace for hex, paths and geometry. */
@Composable
fun PosSpecRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(150.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style = if (mono) {
                MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.caption
            },
            color = MaterialTheme.colors.onSurface,
        )
    }
}

/**
 * A tinted advisory strip. Used for the "unverified geometry" notice and for validator output, so
 * a warning reads the same everywhere.
 */
@Composable
fun PosNoticeStrip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Warning,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.30f)), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        // (border comes from androidx.compose.foundation.border)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface)
        }
    }
}
