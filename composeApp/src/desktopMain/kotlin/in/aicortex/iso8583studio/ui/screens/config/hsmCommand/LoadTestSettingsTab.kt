package `in`.aicortex.iso8583studio.ui.screens.config.hsmCommand

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmCommandConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.LoadTestPattern

@Composable
fun LoadTestSettingsTab(
    config: HsmCommandConfig,
    onConfigChange: (HsmCommandConfig) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Load Test Defaults", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
        Text(
            "These settings are the defaults when running the load tester from the runtime screen.",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp,
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Pattern
                var patternExpanded by remember { mutableStateOf(false) }
                var patternFieldWidth by remember { mutableStateOf(0.dp) }
                val patternDensity = LocalDensity.current
                Box(modifier = Modifier.fillMaxWidth()) {
                    FixedOutlinedTextField(
                        value = config.loadTestPattern.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Load Pattern") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { patternFieldWidth = with(patternDensity) { it.size.width.toDp() } },
                        trailingIcon = {
                            IconButton(onClick = { patternExpanded = !patternExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { patternExpanded = !patternExpanded }
                    )
                    DropdownMenu(
                        expanded = patternExpanded,
                        onDismissRequest = { patternExpanded = false },
                        modifier = Modifier
                            .then(if (patternFieldWidth > 0.dp) Modifier.width(patternFieldWidth) else Modifier.fillMaxWidth())
                            .heightIn(max = 300.dp)
                    ) {
                        LoadTestPattern.entries.forEach { p ->
                            DropdownMenuItem(onClick = {
                                onConfigChange(config.copy(loadTestPattern = p))
                                patternExpanded = false
                            }) { Text(p.displayName) }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FixedOutlinedTextField(
                        value = config.loadTestConcurrentConnections.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> onConfigChange(config.copy(loadTestConcurrentConnections = v.coerceIn(1, 100))) } },
                        label = { Text("Concurrent Connections") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    FixedOutlinedTextField(
                        value = config.loadTestCommandsPerSecond.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> onConfigChange(config.copy(loadTestCommandsPerSecond = v.coerceIn(1, 10000))) } },
                        label = { Text("Commands / Second") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                FixedOutlinedTextField(
                    value = config.loadTestDurationSeconds.toString(),
                    onValueChange = { it.toIntOrNull()?.let { v -> onConfigChange(config.copy(loadTestDurationSeconds = v.coerceIn(1, 86400))) } },
                    label = { Text("Test Duration (seconds)") },
                    modifier = Modifier.width(250.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}
