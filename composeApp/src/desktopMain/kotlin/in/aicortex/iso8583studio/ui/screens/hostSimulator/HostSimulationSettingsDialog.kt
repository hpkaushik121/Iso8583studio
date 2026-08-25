package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import `in`.aicortex.iso8583studio.data.model.HostSimulationConfig
import `in`.aicortex.iso8583studio.data.model.ResponseCodeSimulationConfig
import `in`.aicortex.iso8583studio.data.model.WeightedResponseCode
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.RampPattern
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.simulation.ResponseDelayType
import `in`.aicortex.iso8583studio.ui.screens.components.SimulationEnumDropdown
import `in`.aicortex.iso8583studio.ui.screens.components.SimulationNumberField
import `in`.aicortex.iso8583studio.ui.screens.components.SimulationRateSlider
import `in`.aicortex.iso8583studio.ui.screens.components.SimulationSection
import `in`.aicortex.iso8583studio.ui.screens.components.SimulationSliderRow
import `in`.aicortex.iso8583studio.ui.screens.components.SimulationToggleRow

/**
 * Runtime tuning for the Host Simulator's simulated impairments, opened from the Transaction
 * Simulator header.
 *
 * Applying takes effect on the running simulator immediately — `GatewayConfig` is shared by
 * reference and the socket path re-reads it per response — and then persists through the screen's
 * existing Save.
 */
@Composable
fun SimulationSettingsDialog(
    simulation: HostSimulationConfig,
    onDismiss: () -> Unit,
    onApply: (HostSimulationConfig) -> Unit,
) {
    var draft by remember(simulation) { mutableStateOf(simulation) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(680.dp).heightIn(max = 720.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = 8.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Header ──────────────────────────────────────────────
                Surface(color = MaterialTheme.colors.surface, elevation = 1.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (draft.anyActive) WarningYellow else PrimaryBlue,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Simulation Behaviour",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                summarise(draft),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        if (draft.anyActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = WarningYellow.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    "SIM",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.overline,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningYellow,
                                )
                            }
                        }
                    }
                }
                Divider()

                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Master switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Enable simulation",
                                    style = MaterialTheme.typography.body2,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    "Applies to responses this simulator originates, in SERVER mode only.",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                )
                            }
                            Switch(
                                checked = draft.enabled,
                                onCheckedChange = { draft = draft.copy(enabled = it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = WarningYellow),
                            )
                        }

                        val dimmed = !draft.enabled

                        SimulationSection(
                            title = "Response Latency & Jitter",
                            icon = Icons.Default.Timer,
                            enabled = draft.delay.enabled,
                            dimmed = dimmed,
                            onEnabledChange = { draft = draft.copy(delay = draft.delay.copy(enabled = it)) },
                        ) {
                            SimulationEnumDropdown(
                                label = "Delay type",
                                options = ResponseDelayType.entries,
                                selected = draft.delay.delayType,
                                optionLabel = { it.displayName },
                                optionDescription = { it.description },
                                onSelected = { draft = draft.copy(delay = draft.delay.copy(delayType = it)) },
                            )
                            when (draft.delay.delayType) {
                                ResponseDelayType.RANDOM -> Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    SimulationNumberField(
                                        "Min delay (ms)", draft.delay.minDelayMs, Modifier.weight(1f),
                                    ) { draft = draft.copy(delay = draft.delay.copy(minDelayMs = it)) }
                                    SimulationNumberField(
                                        "Max delay (ms)", draft.delay.maxDelayMs, Modifier.weight(1f),
                                    ) { draft = draft.copy(delay = draft.delay.copy(maxDelayMs = it)) }
                                }

                                ResponseDelayType.REALISTIC -> Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    SimulationNumberField(
                                        "Network latency (ms)", draft.delay.networkLatencyMs, Modifier.weight(1f),
                                    ) { draft = draft.copy(delay = draft.delay.copy(networkLatencyMs = it)) }
                                    SimulationNumberField(
                                        "Processing (ms)", draft.delay.processingDelayMs, Modifier.weight(1f),
                                    ) { draft = draft.copy(delay = draft.delay.copy(processingDelayMs = it)) }
                                }

                                ResponseDelayType.NONE -> Unit

                                else -> SimulationNumberField(
                                    if (draft.delay.delayType == ResponseDelayType.PROGRESSIVE)
                                        "Delay per connection (ms)" else "Fixed delay (ms)",
                                    draft.delay.fixedDelayMs,
                                ) { draft = draft.copy(delay = draft.delay.copy(fixedDelayMs = it)) }
                            }

                            SimulationToggleRow(
                                label = "Add jitter",
                                checked = draft.delay.enableJitter,
                            ) { draft = draft.copy(delay = draft.delay.copy(enableJitter = it)) }
                            if (draft.delay.enableJitter) {
                                SimulationSliderRow(
                                    label = "Jitter",
                                    value = draft.delay.jitterPercentage.toFloat(),
                                    range = 0f..100f,
                                    valueText = "±${draft.delay.jitterPercentage}%",
                                ) {
                                    draft = draft.copy(delay = draft.delay.copy(jitterPercentage = it.toInt()))
                                }
                            }
                        }

                        SimulationSection(
                            title = "Ramp Profile",
                            icon = Icons.Default.TrendingUp,
                            enabled = draft.ramp.enabled,
                            dimmed = dimmed,
                            subtitle = "Scales impairment over time since the simulator was started",
                            onEnabledChange = { draft = draft.copy(ramp = draft.ramp.copy(enabled = it)) },
                        ) {
                            SimulationEnumDropdown(
                                label = "Pattern",
                                options = RampPattern.entries,
                                selected = draft.ramp.pattern,
                                optionLabel = { it.displayName },
                                optionDescription = { it.description },
                                onSelected = { draft = draft.copy(ramp = draft.ramp.copy(pattern = it)) },
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SimulationNumberField(
                                    "Ramp up (s)", draft.ramp.rampUpSeconds, Modifier.weight(1f),
                                ) { draft = draft.copy(ramp = draft.ramp.copy(rampUpSeconds = it)) }
                                SimulationNumberField(
                                    "Hold (s)", draft.ramp.holdSeconds, Modifier.weight(1f),
                                ) { draft = draft.copy(ramp = draft.ramp.copy(holdSeconds = it)) }
                                SimulationNumberField(
                                    "Ramp down (s)", draft.ramp.rampDownSeconds, Modifier.weight(1f),
                                ) { draft = draft.copy(ramp = draft.ramp.copy(rampDownSeconds = it)) }
                            }
                            SimulationSliderRow(
                                label = "Peak multiplier",
                                value = draft.ramp.peakMultiplier.toFloat(),
                                range = 1f..20f,
                                valueText = "×${"%.1f".format(draft.ramp.peakMultiplier)}",
                            ) { draft = draft.copy(ramp = draft.ramp.copy(peakMultiplier = it.toDouble())) }
                            SimulationToggleRow(
                                label = "Repeat the profile",
                                checked = draft.ramp.repeat,
                            ) { draft = draft.copy(ramp = draft.ramp.copy(repeat = it)) }
                            SimulationToggleRow(
                                label = "Also scale failure rates",
                                checked = draft.ramp.applyToFailureRates,
                            ) { draft = draft.copy(ramp = draft.ramp.copy(applyToFailureRates = it)) }
                        }

                        SimulationSection(
                            title = "Timeouts / No Response",
                            icon = Icons.Default.HourglassEmpty,
                            enabled = draft.timeouts.enabled,
                            dimmed = dimmed,
                            subtitle = "Exercises the client's timeout and retry handling",
                            onEnabledChange = { draft = draft.copy(timeouts = draft.timeouts.copy(enabled = it)) },
                        ) {
                            SimulationRateSlider(
                                label = "Drop response (no reply at all)",
                                rate = draft.timeouts.dropResponseRate,
                            ) { draft = draft.copy(timeouts = draft.timeouts.copy(dropResponseRate = it)) }
                            SimulationRateSlider(
                                label = "Answer very late",
                                rate = draft.timeouts.hangResponseRate,
                            ) { draft = draft.copy(timeouts = draft.timeouts.copy(hangResponseRate = it)) }
                            if (draft.timeouts.hangResponseRate > 0.0) {
                                SimulationNumberField(
                                    "Late-answer delay (ms)", draft.timeouts.hangDelayMs,
                                ) { draft = draft.copy(timeouts = draft.timeouts.copy(hangDelayMs = it)) }
                            }
                            SimulationToggleRow(
                                label = "Close the connection when dropping",
                                checked = draft.timeouts.closeConnectionOnDrop,
                            ) { draft = draft.copy(timeouts = draft.timeouts.copy(closeConnectionOnDrop = it)) }
                        }

                        SimulationSection(
                            title = "Connection Chaos",
                            icon = Icons.Default.Cable,
                            enabled = draft.connection.enabled,
                            dimmed = dimmed,
                            subtitle = "Misbehaviour at the socket level rather than the response level",
                            onEnabledChange = { draft = draft.copy(connection = draft.connection.copy(enabled = it)) },
                        ) {
                            SimulationNumberField(
                                "Delay before accepting (ms)", draft.connection.acceptDelayMs,
                            ) { draft = draft.copy(connection = draft.connection.copy(acceptDelayMs = it)) }
                            SimulationRateSlider(
                                label = "Refuse connection",
                                rate = draft.connection.refuseRate,
                            ) { draft = draft.copy(connection = draft.connection.copy(refuseRate = it)) }
                            SimulationRateSlider(
                                label = "Reset an open connection",
                                rate = draft.connection.resetMidStreamRate,
                            ) { draft = draft.copy(connection = draft.connection.copy(resetMidStreamRate = it)) }
                            SimulationNumberField(
                                "Write throttle (bytes/sec, 0 = off)",
                                draft.connection.slowWriteBytesPerSecond,
                            ) {
                                draft = draft.copy(
                                    connection = draft.connection.copy(slowWriteBytesPerSecond = it),
                                )
                            }
                        }

                        ResponseCodeSection(
                            config = draft.responseCode,
                            dimmed = dimmed,
                            onChanged = { draft = draft.copy(responseCode = it) },
                        )

                        SimulationSection(
                            title = "Reproducibility",
                            icon = Icons.Default.Casino,
                            enabled = draft.randomSeed != 0L,
                            dimmed = dimmed,
                            subtitle = "Fix the random seed so a run can be repeated exactly",
                            onEnabledChange = { on -> draft = draft.copy(randomSeed = if (on) 12345L else 0L) },
                        ) {
                            SimulationNumberField("Random seed", draft.randomSeed.toInt()) {
                                draft = draft.copy(randomSeed = it.toLong())
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { draft = HostSimulationConfig() }) {
                            Text("Reset defaults")
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onApply(draft) },
                            colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                        ) {
                            Text("Apply", color = MaterialTheme.colors.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

/** Response-code simulation — the section that drives the `[SIMRC]` placeholder. */
@Composable
private fun ResponseCodeSection(
    config: ResponseCodeSimulationConfig,
    dimmed: Boolean,
    onChanged: (ResponseCodeSimulationConfig) -> Unit,
) {
    SimulationSection(
        title = "Response Code Simulation",
        icon = Icons.Default.ErrorOutline,
        enabled = config.enabled,
        dimmed = dimmed,
        subtitle = "Drives the [SIMRC] placeholder — put it on DE 39 in a transaction template",
        onEnabledChange = { onChanged(config.copy(enabled = it)) },
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = PrimaryBlue.copy(alpha = 0.06f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Set a response field's value to [SIMRC] and it resolves to the success code, or to " +
                        "one of the decline codes below at the configured rate.",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
        }

        FixedOutlinedSuccessCode(config.successCode) { onChanged(config.copy(successCode = it)) }

        SimulationRateSlider(label = "Decline rate", rate = config.errorRate) {
            onChanged(config.copy(errorRate = it))
        }

        Text(
            "Decline codes (drawn by weight)",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
        )
        config.errorCodes.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField(
                    value = entry.code,
                    onValueChange = { text ->
                        val updated = config.errorCodes.toMutableList()
                        updated[index] = entry.copy(code = text.take(3))
                        onChanged(config.copy(errorCodes = updated))
                    },
                    label = { Text("Code", style = MaterialTheme.typography.caption) },
                    modifier = Modifier.width(96.dp),
                    singleLine = true,
                )
                SimulationNumberField("Weight", entry.weight, Modifier.weight(1f)) { weight ->
                    val updated = config.errorCodes.toMutableList()
                    updated[index] = entry.copy(weight = weight)
                    onChanged(config.copy(errorCodes = updated))
                }
                IconButton(
                    onClick = {
                        onChanged(config.copy(errorCodes = config.errorCodes.filterIndexed { i, _ -> i != index }))
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Delete, "Remove", Modifier.size(16.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                    )
                }
            }
        }
        TextButton(onClick = {
            onChanged(config.copy(errorCodes = config.errorCodes + WeightedResponseCode()))
        }) {
            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add decline code", style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
private fun FixedOutlinedSuccessCode(value: String, onValueChange: (String) -> Unit) {
    `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(3)) },
        label = { Text("Success code", style = MaterialTheme.typography.caption) },
        modifier = Modifier.width(140.dp),
        singleLine = true,
    )
}

/** One-line description of what simulation is currently doing, for the collapsed header. */
private fun summarise(simulation: HostSimulationConfig): String {
    if (!simulation.anyActive) return "Off — responses are returned as fast as possible"

    val parts = mutableListOf<String>()
    if (simulation.delay.enabled && simulation.delay.delayType != ResponseDelayType.NONE) {
        val label = when (simulation.delay.delayType) {
            ResponseDelayType.RANDOM -> "${simulation.delay.minDelayMs}–${simulation.delay.maxDelayMs} ms"
            ResponseDelayType.REALISTIC ->
                "${simulation.delay.networkLatencyMs + simulation.delay.processingDelayMs} ms"
            else -> "${simulation.delay.fixedDelayMs} ms"
        }
        parts += "latency $label"
    }
    if (simulation.ramp.enabled) parts += "ramp ${simulation.ramp.pattern.displayName.lowercase()}"
    if (simulation.timeouts.enabled && simulation.timeouts.dropResponseRate > 0) {
        parts += "${(simulation.timeouts.dropResponseRate * 100).toInt()}% dropped"
    }
    if (simulation.connection.enabled) parts += "connection chaos"
    if (simulation.responseCode.enabled && simulation.responseCode.errorRate > 0) {
        parts += "${(simulation.responseCode.errorRate * 100).toInt()}% decline"
    }
    return parts.joinToString(" · ").ifEmpty { "On" }
}
