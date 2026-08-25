package `in`.aicortex.iso8583studio.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.PrimaryBlue

/**
 * Shared building blocks for the simulators' simulation-behaviour settings.
 *
 * Extracted so the HSM dialog and the Host Simulator's Settings section read as one feature rather
 * than two lookalikes that drift apart.
 */

/** A titled, individually toggleable group whose contents only appear when it is on. */
@Composable
fun SimulationSection(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    dimmed: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val active = enabled && !dimmed
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            1.dp,
            if (active) PrimaryBlue.copy(alpha = 0.3f)
            else MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
        ),
        color = MaterialTheme.colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, null, Modifier.size(16.dp),
                    tint = if (active) PrimaryBlue else MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    enabled = !dimmed,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue),
                )
            }
            if (active) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
            }
        }
    }
}

/** Non-negative integer entry. */
@Composable
fun SimulationNumberField(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
) {
    FixedOutlinedTextField(
        value = value.toString(),
        onValueChange = { text -> text.toIntOrNull()?.let { if (it >= 0) onValueChange(it) } },
        label = { Text(label, style = MaterialTheme.typography.caption) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
fun SimulationToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue),
        )
    }
}

@Composable
fun SimulationSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f))
            Text(
                valueText,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue,
            )
        }
        Slider(
            value = value.coerceIn(range),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue),
        )
    }
}

/** A 0..1 probability rendered and edited as a percentage. */
@Composable
fun SimulationRateSlider(label: String, rate: Double, onRateChange: (Double) -> Unit) {
    SimulationSliderRow(
        label = label,
        value = (rate * 100).toFloat(),
        range = 0f..100f,
        valueText = "${(rate * 100).toInt()}%",
    ) { onRateChange((it / 100f).toDouble()) }
}

@Composable
fun <T> SimulationEnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    optionDescription: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(optionLabel(selected), style = MaterialTheme.typography.body2)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = { onSelected(option); expanded = false }) {
                        Column {
                            Text(optionLabel(option), fontWeight = FontWeight.Medium)
                            Text(
                                optionDescription(option),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}
