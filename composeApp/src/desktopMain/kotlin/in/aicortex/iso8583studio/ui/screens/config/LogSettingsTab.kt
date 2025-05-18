package `in`.aicortex.iso8583studio.ui.screens.config


import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.LoggingOption

/**
 * Log Settings Tab - Fourth tab in the Security Gateway configuration
 * Contains logging options and settings
 */
@Composable
fun LogSettingsTab(config: GatewayConfig, onConfigChange: (GatewayConfig) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        // Logging Options group
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Logging Options", style = MaterialTheme.typography.subtitle1)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Logfile name", modifier = Modifier.width(100.dp))
                    TextField(
                        value = config.logFileName,
                        onValueChange = {
                            onConfigChange(config.copy( _logFileName = it))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Max size(MB)", modifier = Modifier.width(100.dp))
                    TextField(
                        value = config.maxLogSizeInMB.toString(),
                        onValueChange = {
                            val size = it.toIntOrNull() ?: config.maxLogSizeInMB
                            onConfigChange(config.copy( maxLogSizeInMB = size))
                        },
                        modifier = Modifier.width(80.dp)
                    )
                }

                // Need to log group
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    elevation = 1.dp,
                    backgroundColor = Color.LightGray.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Need to log", style = MaterialTheme.typography.subtitle2)

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = config.logOptions == LoggingOption.SIMPLE.value,
                                onCheckedChange = {
                                    onConfigChange(config.copy(logOptions = LoggingOption.SIMPLE.value))
                                }
                            )
                            Text("Simple")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = config.logOptions == LoggingOption.RAW_DATA.value,
                                onCheckedChange = {
                                    onConfigChange(config.copy( logOptions = LoggingOption.RAW_DATA.value))
                                }
                            )
                            Text("Write Raw data")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = config.logOptions == LoggingOption.TEXT_DATA.value,
                                onCheckedChange = {
                                    onConfigChange(config.copy(logOptions = LoggingOption.TEXT_DATA.value))
                                }
                            )
                            Text("Write Text data")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Encoding", modifier = Modifier.width(80.dp))
                            var expanded by remember { mutableStateOf(false) }


                            Box {
                                Button(onClick = { expanded = true }) {
                                    Text(config.textEncoding.name)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    config.getEncodingList().forEachIndexed { index, encoding ->
                                        DropdownMenuItem(onClick = {
                                            onConfigChange(config.copy( textEncoding = encoding))
                                            expanded = false
                                        }) {
                                            Text(encoding.name)
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = config.logOptions == LoggingOption.PARSED_DATA.value,
                                onCheckedChange = {
                                    onConfigChange(config.copy(logOptions = LoggingOption.PARSED_DATA.value))
                                }
                            )
                            Text("Parsed data")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Protocol", modifier = Modifier.width(80.dp))
                            var expanded by remember { mutableStateOf(false) }
                            val protocols = listOf("ISO8583")

                            Box {
                                Button(onClick = { expanded = true }) {
                                    Text(protocols[0])
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    protocols.forEach { protocol ->
                                        DropdownMenuItem(onClick = {
                                            expanded = false
                                        }) {
                                            Text(protocol)
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Template File", modifier = Modifier.width(80.dp))
                            Spacer(modifier = Modifier.weight(1f))
                            Button(onClick = { /* Change template file logic */ }) {
                                Text("Change")
                            }
                        }
                    }
                }
            }
        }
    }
}