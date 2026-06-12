package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.BehaviorAction
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.BehaviorRule
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.ForcedAcType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import java.util.UUID

private val INITIAL_STATES = listOf("Active", "Locked", "Blocked", "Personalized")

@Composable
fun RiskBehaviorTab(config: APDUSimulatorConfig, onConfigUpdate: (APDUSimulatorConfig) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PinLifecycleSection(config, onConfigUpdate)
        BehaviorRulesSection(config, onConfigUpdate)
        ExamplesSection()
    }
}

@Composable
private fun PinLifecycleSection(
    config: APDUSimulatorConfig,
    onConfigUpdate: (APDUSimulatorConfig) -> Unit,
) {
    SectionCard(
        icon = Icons.Default.Pin,
        title = "PIN & lifecycle",
        subtitle = "Card-side PIN counter and initial lifecycle state.",
    ) {
        FixedOutlinedTextField(
            value = config.pinAttemptsRemaining,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(2)
                val clamped = digits.toIntOrNull()?.coerceIn(1, 15)?.toString() ?: digits
                onConfigUpdate(config.copy(pinAttemptsRemaining = clamped))
            },
            label = { Text("PIN attempts remaining (1..15)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SwitchRow(
            label = "Block card on PIN exhaustion",
            checked = config.blockOnPinExhaustion,
            onChange = { onConfigUpdate(config.copy(blockOnPinExhaustion = it)) },
        )
        SwitchRow(
            label = "Block card on transaction limit",
            checked = config.blockOnTransactionLimit,
            onChange = { onConfigUpdate(config.copy(blockOnTransactionLimit = it)) },
        )
        DropdownField(
            label = "Initial state",
            options = INITIAL_STATES,
            selected = config.initialState,
            onSelect = { onConfigUpdate(config.copy(initialState = it)) },
        )
    }
}

@Composable
private fun BehaviorRulesSection(
    config: APDUSimulatorConfig,
    onConfigUpdate: (APDUSimulatorConfig) -> Unit,
) {
    SectionCard(
        icon = Icons.Default.BugReport,
        title = "Behavior / fault-injection rules",
        subtitle = "Fired in registration order; first match wins. Empty match fields = any APDU.",
    ) {
        if (config.behaviorRules.isEmpty()) {
            Text(
                "No rules defined. Card runtime behaves normally.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
        }
        config.behaviorRules.forEachIndexed { index, rule ->
            BehaviorRuleRow(
                rule = rule,
                onChange = { updated ->
                    val list = config.behaviorRules.toMutableList()
                    list[index] = updated
                    onConfigUpdate(config.copy(behaviorRules = list))
                },
                onRemove = {
                    val list = config.behaviorRules.toMutableList()
                    list.removeAt(index)
                    onConfigUpdate(config.copy(behaviorRules = list))
                },
            )
        }
        OutlinedButton(
            onClick = {
                val n = config.behaviorRules.size + 1
                val newRule = BehaviorRule(
                    id = UUID.randomUUID().toString().take(8),
                    name = "Rule $n",
                    action = BehaviorAction.Delay(0),
                )
                onConfigUpdate(config.copy(behaviorRules = config.behaviorRules + newRule))
            },
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add rule")
        }
    }
}

@Composable
private fun BehaviorRuleRow(
    rule: BehaviorRule,
    onChange: (BehaviorRule) -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(6.dp),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    rule.name.ifBlank { "(unnamed)" },
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                EnabledBadge(rule.enabled)
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            }
            if (expanded) {
                Text(
                    "id: ${rule.id}",
                    style = MaterialTheme.typography.caption,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
                FixedOutlinedTextField(
                    value = rule.name,
                    onValueChange = { onChange(rule.copy(name = it)) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SwitchRow(
                    label = "Enabled",
                    checked = rule.enabled,
                    onChange = { onChange(rule.copy(enabled = it)) },
                )

                Text(
                    "Match",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HexByteField(
                        value = rule.whenIns?.let { hexByte(it) } ?: "",
                        onValueChange = { onChange(rule.copy(whenIns = parseHexByte(it))) },
                        label = "INS",
                        modifier = Modifier.weight(1f),
                    )
                    HexByteField(
                        value = rule.whenP1?.let { hexByte(it) } ?: "",
                        onValueChange = { onChange(rule.copy(whenP1 = parseHexByte(it))) },
                        label = "P1",
                        modifier = Modifier.weight(1f),
                    )
                    HexByteField(
                        value = rule.whenP2?.let { hexByte(it) } ?: "",
                        onValueChange = { onChange(rule.copy(whenP2 = parseHexByte(it))) },
                        label = "P2",
                        modifier = Modifier.weight(1f),
                    )
                }
                FixedOutlinedTextField(
                    value = rule.triggerLimit.toString(),
                    onValueChange = { input ->
                        val cleaned = input.filter { it.isDigit() || it == '-' }
                        val parsed = cleaned.toIntOrNull() ?: -1
                        onChange(rule.copy(triggerLimit = parsed))
                    },
                    label = { Text("Trigger limit (-1 = forever)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Action",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                )
                ActionEditor(
                    action = rule.action,
                    onChange = { onChange(rule.copy(action = it)) },
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Remove")
                    }
                }
            }
        }
    }
}

private enum class ActionKind { ReturnSw, Delay, CorruptAc, ForceCryptogram, DropResponse }

private fun BehaviorAction.kind(): ActionKind = when (this) {
    is BehaviorAction.ReturnSw -> ActionKind.ReturnSw
    is BehaviorAction.Delay -> ActionKind.Delay
    is BehaviorAction.CorruptAc -> ActionKind.CorruptAc
    is BehaviorAction.ForceCryptogramType -> ActionKind.ForceCryptogram
    BehaviorAction.DropResponse -> ActionKind.DropResponse
}

private fun defaultActionFor(kind: ActionKind): BehaviorAction = when (kind) {
    ActionKind.ReturnSw -> BehaviorAction.ReturnSw("9000", "")
    ActionKind.Delay -> BehaviorAction.Delay(0)
    ActionKind.CorruptAc -> BehaviorAction.CorruptAc(0, 0xFF)
    ActionKind.ForceCryptogram -> BehaviorAction.ForceCryptogramType(ForcedAcType.AAC)
    ActionKind.DropResponse -> BehaviorAction.DropResponse
}

@Composable
private fun ActionEditor(action: BehaviorAction, onChange: (BehaviorAction) -> Unit) {
    val kind = action.kind()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ActionKind.values().forEach { k ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = kind == k,
                    onClick = { if (kind != k) onChange(defaultActionFor(k)) },
                )
                Text(actionLabel(k), style = MaterialTheme.typography.body2)
            }
        }
        when (action) {
            is BehaviorAction.ReturnSw -> {
                FixedOutlinedTextField(
                    value = action.swHex,
                    onValueChange = {
                        val cleaned = it.filter { c -> c.isDigit() || c in 'a'..'f' || c in 'A'..'F' }
                            .uppercase().take(4)
                        onChange(action.copy(swHex = cleaned))
                    },
                    label = { Text("SW (4 hex chars)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FixedOutlinedTextField(
                    value = action.responseDataHex,
                    onValueChange = { onChange(action.copy(responseDataHex = it)) },
                    label = { Text("Response data (hex, optional)") },
                    minLines = 2,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is BehaviorAction.Delay -> {
                FixedOutlinedTextField(
                    value = action.millis.toString(),
                    onValueChange = {
                        val n = it.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L
                        onChange(BehaviorAction.Delay(n))
                    },
                    label = { Text("Delay (ms)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is BehaviorAction.CorruptAc -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FixedOutlinedTextField(
                        value = action.byteIndex.toString(),
                        onValueChange = {
                            val n = it.filter { c -> c.isDigit() }.toIntOrNull()?.coerceIn(0, 7) ?: 0
                            onChange(action.copy(byteIndex = n))
                        },
                        label = { Text("Byte index (0..7)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    FixedOutlinedTextField(
                        value = hexByte(action.xorMask),
                        onValueChange = {
                            val parsed = parseHexByte(it) ?: 0
                            onChange(action.copy(xorMask = parsed.coerceIn(0, 255)))
                        },
                        label = { Text("XOR mask (hex)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            is BehaviorAction.ForceCryptogramType -> {
                DropdownField(
                    label = "Cryptogram type",
                    options = ForcedAcType.values().map { it.name },
                    selected = action.type.name,
                    onSelect = { onChange(BehaviorAction.ForceCryptogramType(ForcedAcType.valueOf(it))) },
                )
            }

            BehaviorAction.DropResponse -> {
                Text(
                    "No response will be sent — the terminal will see a timeout.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

private fun actionLabel(k: ActionKind): String = when (k) {
    ActionKind.ReturnSw -> "Return SW"
    ActionKind.Delay -> "Delay"
    ActionKind.CorruptAc -> "Corrupt AC"
    ActionKind.ForceCryptogram -> "Force cryptogram"
    ActionKind.DropResponse -> "Drop response"
}

@Composable
private fun ExamplesSection() {
    SectionCard(
        icon = Icons.Default.Lightbulb,
        title = "Examples",
        subtitle = "Common recipes you can build with the rules above.",
    ) {
        val recipes = listOf(
            "Force AAC on first GAC: whenIns=AE whenP1=80 (or any GAC), action=ForceCryptogramType(AAC), triggerLimit=1",
            "Inject 600ms latency on READ RECORD: whenIns=B2, action=Delay(600)",
            "Corrupt cryptogram once: whenIns=AE, action=CorruptAc(0, 0xFF), triggerLimit=1",
            "Return 6985 once: whenIns=AE, action=ReturnSw(\"6985\"), triggerLimit=1",
        )
        recipes.forEach { line ->
            Text(
                "• $line",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Small helpers
// ---------------------------------------------------------------------------

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun EnabledBadge(enabled: Boolean) {
    val bg = if (enabled) MaterialTheme.colors.primary.copy(alpha = 0.15f)
    else MaterialTheme.colors.onSurface.copy(alpha = 0.10f)
    val fg = if (enabled) MaterialTheme.colors.primary
    else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
    Box(
        Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            if (enabled) "ENABLED" else "DISABLED",
            style = MaterialTheme.typography.overline,
            color = fg,
        )
    }
}

@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                    RoundedCornerShape(4.dp),
                )
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
                Text(
                    selected.ifBlank { "—" },
                    style = MaterialTheme.typography.body2,
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(onClick = {
                    onSelect(opt)
                    open = false
                }) {
                    Text(opt)
                }
            }
        }
    }
}

@Composable
private fun HexByteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    FixedOutlinedTextField(
        value = value,
        onValueChange = { input ->
            val cleaned = input.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                .uppercase().take(2)
            onValueChange(cleaned)
        },
        label = { Text(label) },
        placeholder = { Text("any") },
        singleLine = true,
        modifier = modifier,
    )
}

private fun hexByte(v: Int): String = "%02X".format(v and 0xFF)

private fun parseHexByte(s: String): Int? {
    val trimmed = s.trim()
    if (trimmed.isEmpty()) return null
    return runCatching { Integer.parseInt(trimmed, 16) and 0xFF }.getOrNull()
}
