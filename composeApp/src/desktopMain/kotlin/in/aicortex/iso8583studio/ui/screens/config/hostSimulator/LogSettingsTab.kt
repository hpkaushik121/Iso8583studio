package `in`.aicortex.iso8583studio.ui.screens.config.hostSimulator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.LoggingOption
import `in`.aicortex.iso8583studio.ui.screens.components.IconActionButton
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.SecondaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SectionHeader
import `in`.aicortex.iso8583studio.ui.screens.components.StyledTextField

/**
 * Log Settings Tab - Fourth tab in the Security Gateway configuration
 * Contains logging options and settings
 */
@Composable
fun LogSettingsTab(config: GatewayConfig, onConfigChange: (GatewayConfig) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Logging Options panel
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader(title = "Logging Options")

                // Log file name input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Logfile name",
                        modifier = Modifier.width(120.dp),
                        fontWeight = FontWeight.Medium
                    )
                    StyledTextField(
                        value = config.logFileName,
                        onValueChange = {
                            onConfigChange(config.copy(_logFileName = it))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Max log size input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Max size (MB)",
                        modifier = Modifier.width(120.dp),
                        fontWeight = FontWeight.Medium
                    )
                    StyledTextField(
                        value = config.maxLogSizeInMB.toString(),
                        onValueChange = {
                            val size = it.toIntOrNull() ?: config.maxLogSizeInMB
                            onConfigChange(config.copy(maxLogSizeInMB = size))
                        },
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        }

        // Need to log panel
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader(title = "Logging Content")

                // Logging options in a selectableGroup
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Simple logging option
                    Row(
                        modifier = Modifier.selectable(
                            selected = config.logOptions == LoggingOption.SIMPLE.value,
                            onClick = { onConfigChange(config.copy(logOptions = LoggingOption.SIMPLE.value)) },
                            role = Role.RadioButton
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.logOptions == LoggingOption.SIMPLE.value,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colors.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simple")
                    }

                    // Raw data logging option
                    Row(
                        modifier = Modifier.selectable(
                            selected = config.logOptions == LoggingOption.RAW_DATA.value,
                            onClick = { onConfigChange(config.copy(logOptions = LoggingOption.RAW_DATA.value)) },
                            role = Role.RadioButton
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.logOptions == LoggingOption.RAW_DATA.value,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colors.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Write Raw data")
                    }

                    // Text data logging option
                    Row(
                        modifier = Modifier.selectable(
                            selected = config.logOptions == LoggingOption.TEXT_DATA.value,
                            onClick = { onConfigChange(config.copy(logOptions = LoggingOption.TEXT_DATA.value)) },
                            role = Role.RadioButton
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.logOptions == LoggingOption.TEXT_DATA.value,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colors.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Write Text data")
                    }

                    // Only show encoding options if text data is selected
                    if (config.logOptions == LoggingOption.TEXT_DATA.value) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Encoding",
                                modifier = Modifier.width(80.dp),
                                fontWeight = FontWeight.Medium
                            )

                            var expanded by remember { mutableStateOf(false) }

                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = MaterialTheme.colors.surface,
                                        contentColor = MaterialTheme.colors.onSurface
                                    )
                                ) {
                                    Text(config.textEncoding.name)
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    config.getEncodingList().forEachIndexed { index, encoding ->
                                        DropdownMenuItem(onClick = {
                                            onConfigChange(config.copy(textEncoding = encoding))
                                            expanded = false
                                        }) {
                                            Text(encoding.name)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Parsed data logging option
                    Row(
                        modifier = Modifier.selectable(
                            selected = config.logOptions == LoggingOption.PARSED_DATA.value,
                            onClick = { onConfigChange(config.copy(logOptions = LoggingOption.PARSED_DATA.value)) },
                            role = Role.RadioButton
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.logOptions == LoggingOption.PARSED_DATA.value,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colors.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Parsed data")
                    }

                    // Show protocol selection if parsed data is selected
                    if (config.logOptions == LoggingOption.PARSED_DATA.value) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Protocol",
                                modifier = Modifier.width(80.dp),
                                fontWeight = FontWeight.Medium
                            )

                            var expanded by remember { mutableStateOf(false) }
                            val protocols = listOf("ISO8583")

                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = MaterialTheme.colors.surface,
                                        contentColor = MaterialTheme.colors.onSurface
                                    )
                                ) {
                                    Text(protocols[0])
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    protocols.forEach { protocol ->
                                        DropdownMenuItem(onClick = { expanded = false }) {
                                            Text(protocol)
                                        }
                                    }
                                }
                            }
                        }

                        // Template file selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Template File",
                                modifier = Modifier.width(100.dp),
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            SecondaryButton(
                                text = "Change",
                                onClick = { /* Change template file logic */ },
                                icon = Icons.Default.FileOpen
                            )
                        }
                    }
                }
            }
        }

        // Advanced logging options could go here
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader(
                    title = "Advanced Options",
                    actionContent = {
                        IconActionButton(
                            icon = Icons.Default.Help,
                            onClick = { /* Show help info */ },
                            contentDescription = "Help with advanced options"
                        )
                    }
                )

                Text(
                    "Configure advanced logging parameters in this section to optimize performance and storage.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                // Additional advanced options would go here
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}