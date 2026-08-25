package `in`.aicortex.iso8583studio.ui.screens.hsmSimulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.HsmErrorInjection
import `in`.aicortex.iso8583studio.domain.service.simulation.SimulationSettings
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ErrorInjectionType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.RampPattern
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ResponseDelayType
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField

/**
 * Runtime tuning for the HSM simulator's simulated impairments.
 *
 * Apply takes effect on the running simulator immediately — the service re-reads these settings per
 * request — and also persists them, so a tuned load-test profile survives a restart.
 */
@Composable
fun SimulationSettingsDialog(
    initial: SimulationSettings,
    onDismiss: () -> Unit,
    onApply: (SimulationSettings) -> Unit,
) {
    var settings by remember { mutableStateOf(initial) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(680.dp).heightIn(max = 720.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = 8.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Header ──────────────────────────────────────────────
                Surface(color = MaterialTheme.colors.surface, elevation = 1.dp) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, null, Modifier.size(20.dp), tint = PrimaryBlue)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Simulation Behaviour",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = settings.enabled,
                                onCheckedChange = { settings = settings.copy(enabled = it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = WarningYellow),
                            )
                        }
                        Text(
                            "Applies to socket traffic only — commands issued from the Host Commands " +
                                    "and Secure Commands tabs bypass the network and are unaffected.",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
                Divider()

                // ── Sections ────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val dimmed = !settings.enabled

                    SettingsSection(
                        title = "Response Latency & Jitter",
                        icon = Icons.Default.Timer,
                        enabled = settings.delay.enabled,
                        dimmed = dimmed,
                        onEnabledChange = {
                            settings = settings.copy(delay = settings.delay.copy(enabled = it))
                        },
                    ) {
                        EnumDropdown(
                            label = "Delay type",
                            options = ResponseDelayType.entries,
                            selected = settings.delay.delayType,
                            optionLabel = { it.displayName },
                            optionDescription = { it.description },
                            onSelected = {
                                settings = settings.copy(delay = settings.delay.copy(delayType = it))
                            },
                        )
                        when (settings.delay.delayType) {
                            ResponseDelayType.RANDOM -> Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                NumberField(
                                    "Min delay (ms)", settings.delay.minDelayMs, Modifier.weight(1f),
                                ) { settings = settings.copy(delay = settings.delay.copy(minDelayMs = it)) }
                                NumberField(
                                    "Max delay (ms)", settings.delay.maxDelayMs, Modifier.weight(1f),
                                ) { settings = settings.copy(delay = settings.delay.copy(maxDelayMs = it)) }
                            }

                            ResponseDelayType.REALISTIC -> Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                NumberField(
                                    "Network latency (ms)", settings.delay.networkLatencyMs, Modifier.weight(1f),
                                ) { settings = settings.copy(delay = settings.delay.copy(networkLatencyMs = it)) }
                                NumberField(
                                    "Processing (ms)", settings.delay.processingDelayMs, Modifier.weight(1f),
                                ) { settings = settings.copy(delay = settings.delay.copy(processingDelayMs = it)) }
                            }

                            ResponseDelayType.NONE -> Unit

                            else -> NumberField(
                                if (settings.delay.delayType == ResponseDelayType.PROGRESSIVE)
                                    "Delay per connection (ms)" else "Fixed delay (ms)",
                                settings.delay.fixedDelayMs,
                            ) { settings = settings.copy(delay = settings.delay.copy(fixedDelayMs = it)) }
                        }

                        ToggleRow(
                            label = "Add jitter",
                            checked = settings.delay.enableJitter,
                            onCheckedChange = {
                                settings = settings.copy(delay = settings.delay.copy(enableJitter = it))
                            },
                        )
                        if (settings.delay.enableJitter) {
                            SliderRow(
                                label = "Jitter",
                                value = settings.delay.jitterPercentage.toFloat(),
                                range = 0f..100f,
                                valueText = "±${settings.delay.jitterPercentage}%",
                            ) {
                                settings = settings.copy(
                                    delay = settings.delay.copy(jitterPercentage = it.toInt()),
                                )
                            }
                        }
                    }

                    SettingsSection(
                        title = "Ramp Profile",
                        icon = Icons.Default.TrendingUp,
                        enabled = settings.ramp.enabled,
                        dimmed = dimmed,
                        subtitle = "Scales impairment over time since the simulator was started",
                        onEnabledChange = {
                            settings = settings.copy(ramp = settings.ramp.copy(enabled = it))
                        },
                    ) {
                        EnumDropdown(
                            label = "Pattern",
                            options = RampPattern.entries,
                            selected = settings.ramp.pattern,
                            optionLabel = { it.displayName },
                            optionDescription = { it.description },
                            onSelected = {
                                settings = settings.copy(ramp = settings.ramp.copy(pattern = it))
                            },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField("Ramp up (s)", settings.ramp.rampUpSeconds, Modifier.weight(1f)) {
                                settings = settings.copy(ramp = settings.ramp.copy(rampUpSeconds = it))
                            }
                            NumberField("Hold (s)", settings.ramp.holdSeconds, Modifier.weight(1f)) {
                                settings = settings.copy(ramp = settings.ramp.copy(holdSeconds = it))
                            }
                            NumberField("Ramp down (s)", settings.ramp.rampDownSeconds, Modifier.weight(1f)) {
                                settings = settings.copy(ramp = settings.ramp.copy(rampDownSeconds = it))
                            }
                        }
                        SliderRow(
                            label = "Peak multiplier",
                            value = settings.ramp.peakMultiplier.toFloat(),
                            range = 1f..20f,
                            valueText = "×${"%.1f".format(settings.ramp.peakMultiplier)}",
                        ) {
                            settings = settings.copy(
                                ramp = settings.ramp.copy(peakMultiplier = it.toDouble()),
                            )
                        }
                        ToggleRow(
                            label = "Repeat the profile",
                            checked = settings.ramp.repeat,
                            onCheckedChange = {
                                settings = settings.copy(ramp = settings.ramp.copy(repeat = it))
                            },
                        )
                        ToggleRow(
                            label = "Also scale failure rates",
                            checked = settings.ramp.applyToFailureRates,
                            onCheckedChange = {
                                settings = settings.copy(
                                    ramp = settings.ramp.copy(applyToFailureRates = it),
                                )
                            },
                        )
                    }

                    SettingsSection(
                        title = "Timeouts / No Response",
                        icon = Icons.Default.HourglassEmpty,
                        enabled = settings.timeouts.enabled,
                        dimmed = dimmed,
                        subtitle = "Exercises the client's timeout and retry handling",
                        onEnabledChange = {
                            settings = settings.copy(timeouts = settings.timeouts.copy(enabled = it))
                        },
                    ) {
                        RateSlider(
                            label = "Drop response (no reply at all)",
                            rate = settings.timeouts.dropResponseRate,
                        ) {
                            settings = settings.copy(
                                timeouts = settings.timeouts.copy(dropResponseRate = it),
                            )
                        }
                        RateSlider(
                            label = "Answer very late",
                            rate = settings.timeouts.hangResponseRate,
                        ) {
                            settings = settings.copy(
                                timeouts = settings.timeouts.copy(hangResponseRate = it),
                            )
                        }
                        if (settings.timeouts.hangResponseRate > 0.0) {
                            NumberField("Late-answer delay (ms)", settings.timeouts.hangDelayMs) {
                                settings = settings.copy(timeouts = settings.timeouts.copy(hangDelayMs = it))
                            }
                        }
                        ToggleRow(
                            label = "Close the connection when dropping",
                            checked = settings.timeouts.closeConnectionOnDrop,
                            onCheckedChange = {
                                settings = settings.copy(
                                    timeouts = settings.timeouts.copy(closeConnectionOnDrop = it),
                                )
                            },
                        )
                    }

                    SettingsSection(
                        title = "Connection Chaos",
                        icon = Icons.Default.Cable,
                        enabled = settings.connection.enabled,
                        dimmed = dimmed,
                        subtitle = "Misbehaviour at the socket level rather than the response level",
                        onEnabledChange = {
                            settings = settings.copy(connection = settings.connection.copy(enabled = it))
                        },
                    ) {
                        NumberField("Delay before accepting (ms)", settings.connection.acceptDelayMs) {
                            settings = settings.copy(connection = settings.connection.copy(acceptDelayMs = it))
                        }
                        RateSlider(
                            label = "Refuse connection",
                            rate = settings.connection.refuseRate,
                        ) {
                            settings = settings.copy(connection = settings.connection.copy(refuseRate = it))
                        }
                        RateSlider(
                            label = "Reset an open connection",
                            rate = settings.connection.resetMidStreamRate,
                        ) {
                            settings = settings.copy(
                                connection = settings.connection.copy(resetMidStreamRate = it),
                            )
                        }
                        NumberField(
                            "Write throttle (bytes/sec, 0 = off)",
                            settings.connection.slowWriteBytesPerSecond,
                        ) {
                            settings = settings.copy(
                                connection = settings.connection.copy(slowWriteBytesPerSecond = it),
                            )
                        }
                    }

                    SettingsSection(
                        title = "Error Injection",
                        icon = Icons.Default.ErrorOutline,
                        enabled = settings.errors.enableErrorInjection,
                        dimmed = dimmed,
                        subtitle = "Answers a share of commands with a real payShield error code",
                        onEnabledChange = {
                            settings = settings.copy(
                                errors = settings.errors.copy(enableErrorInjection = it),
                            )
                        },
                    ) {
                        RateSlider(label = "Error rate", rate = settings.errors.errorRate) {
                            settings = settings.copy(errors = settings.errors.copy(errorRate = it))
                        }
                        Text(
                            "Error types",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.SemiBold,
                        )
                        // Only types that mean something to an HSM are offered; the ISO 8583 host
                        // concepts in the shared enum would map to a misleading HSM code.
                        HsmErrorInjection.HSM_MEANINGFUL_ERROR_TYPES.forEach { type ->
                            ErrorTypeRow(
                                type = type,
                                checked = type in settings.errors.enabledErrorTypes,
                                onCheckedChange = { on ->
                                    val next = settings.errors.enabledErrorTypes.toMutableSet()
                                    if (on) next.add(type) else next.remove(type)
                                    settings = settings.copy(
                                        errors = settings.errors.copy(enabledErrorTypes = next),
                                    )
                                },
                            )
                        }
                        ToggleRow(
                            label = "Burst mode",
                            checked = settings.errors.errorBurstMode,
                            onCheckedChange = {
                                settings = settings.copy(errors = settings.errors.copy(errorBurstMode = it))
                            },
                        )
                        if (settings.errors.errorBurstMode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField(
                                    "Burst length (s)", settings.errors.errorBurstDuration, Modifier.weight(1f),
                                ) {
                                    settings = settings.copy(
                                        errors = settings.errors.copy(errorBurstDuration = it),
                                    )
                                }
                            }
                            RateSlider(label = "Rate during burst", rate = settings.errors.errorBurstRate) {
                                settings = settings.copy(errors = settings.errors.copy(errorBurstRate = it))
                            }
                        }
                    }

                    SettingsSection(
                        title = "Reproducibility",
                        icon = Icons.Default.Casino,
                        enabled = settings.randomSeed != 0L,
                        dimmed = dimmed,
                        subtitle = "Fix the random seed so a run can be repeated exactly",
                        onEnabledChange = { on ->
                            settings = settings.copy(randomSeed = if (on) 12345L else 0L)
                        },
                    ) {
                        NumberField("Random seed", settings.randomSeed.toInt()) {
                            settings = settings.copy(randomSeed = it.toLong())
                        }
                    }
                }

                Divider()

                // ── Actions ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { settings = SimulationSettings() }) {
                        Text("Reset defaults")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onApply(settings) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                    ) {
                        Text("Apply", color = MaterialTheme.colors.onPrimary)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  BUILDING BLOCKS
// ─────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
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

@Composable
private fun NumberField(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
) {
    FixedOutlinedTextField(
        value = value.toString(),
        onValueChange = { text ->
            text.toIntOrNull()?.let { if (it >= 0) onValueChange(it) }
        },
        label = { Text(label, style = MaterialTheme.typography.caption) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue),
        )
    }
}

@Composable
private fun SliderRow(
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

/** A 0..1 probability rendered as a percentage. */
@Composable
private fun RateSlider(label: String, rate: Double, onRateChange: (Double) -> Unit) {
    SliderRow(
        label = label,
        value = (rate * 100).toFloat(),
        range = 0f..100f,
        valueText = "${(rate * 100).toInt()}%",
    ) { onRateChange((it / 100f).toDouble()) }
}

@Composable
private fun ErrorTypeRow(
    type: ErrorInjectionType,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(type.displayName, style = MaterialTheme.typography.caption)
            HsmErrorInjection.errorCodeFor(type)?.let { code ->
                Text(
                    "Responds with error $code",
                    style = MaterialTheme.typography.overline,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun <T> EnumDropdown(
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
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
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
