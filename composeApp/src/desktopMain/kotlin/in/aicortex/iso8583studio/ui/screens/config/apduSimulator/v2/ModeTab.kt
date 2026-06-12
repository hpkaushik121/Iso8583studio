package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig

/**
 * Standalone mode tab — kept for back-compat. The shipping screen uses ModeTransportTab.
 */
@Composable
fun ModeTab(config: APDUSimulatorConfig, onConfigUpdate: (APDUSimulatorConfig) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionCard(
            icon = Icons.Default.SettingsEthernet,
            title = "OPERATING MODE",
            subtitle = "Pick the role this simulator plays.",
        ) {
            ModeOptionCard(
                selected = config.transportMode == "LOOPBACK",
                title = "Loopback",
                description = "Software-only — runtime in-process",
                icon = Icons.Default.Memory,
                onSelect = { onConfigUpdate(config.copy(transportMode = "LOOPBACK")) },
            )
            ModeOptionCard(
                selected = config.transportMode == "PCSC",
                title = "Reader (PC/SC)",
                description = "Drives a physical card via USB reader",
                icon = Icons.Default.Sensors,
                onSelect = { onConfigUpdate(config.copy(transportMode = "PCSC")) },
            )
            ModeOptionCard(
                selected = config.transportMode == "SERIAL",
                title = "Card emulator (USB-CDC)",
                description = "Pushes APDU responses to STM32 firmware",
                icon = Icons.Default.Cable,
                onSelect = { onConfigUpdate(config.copy(transportMode = "SERIAL")) },
            )
        }
    }
}

/**
 * Themed section panel — mirrors the host-simulator config style:
 * a Card with an icon-prefixed all-caps title and an optional subtitle.
 */
@Composable
internal fun SectionCard(
    title: String,
    subtitle: String?,
    icon: ImageVector? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(6.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.overline,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                    )
                    subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            content()
        }
    }
}

/**
 * Themed mode-option card. Used for the operating-mode picker. Selected state fills with the
 * primary color and inverts the icon/title; unselected uses an outlined surface.
 */
@Composable
internal fun ModeOptionCard(
    selected: Boolean,
    title: String,
    description: String,
    icon: ImageVector,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        shape = RoundedCornerShape(6.dp),
        elevation = if (selected) 2.dp else 0.dp,
        border = ButtonDefaults.outlinedBorder,
        color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colors.primary,
                modifier = Modifier.size(16.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else MaterialTheme.colors.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                    color = if (selected) Color.White.copy(alpha = 0.85f)
                            else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
            RadioButton(
                selected = selected,
                onClick = onSelect,
                modifier = Modifier.size(20.dp),
                colors = androidx.compose.material.RadioButtonDefaults.colors(
                    selectedColor = Color.White,
                    unselectedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                ),
            )
        }
    }
}
