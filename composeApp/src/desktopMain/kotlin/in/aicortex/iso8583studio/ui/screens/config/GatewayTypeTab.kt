package `in`.aicortex.iso8583studio.ui.screens.config


import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.domain.utils.Utils
import org.springframework.security.crypto.codec.Hex
import java.time.format.DateTimeFormatter

/**
 * Gateway Type tab - First tab in the Security Gateway configuration
 * Contains the basic gateway configuration settings
 */
@Composable
fun GatewayTypeTab(config: GatewayConfig, onConfigChange: (GatewayConfig) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        // Gateway Type group
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("GATEWAY TYPE", style = MaterialTheme.typography.subtitle1)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = config.gatewayType == GatewayType.SERVER,
                            onClick = {
                                onConfigChange(config.copy(gatewayType = GatewayType.SERVER))
                            }
                        )
                        Text("Server")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = config.gatewayType == GatewayType.CLIENT,
                            onClick = {
                                onConfigChange(config.copy(gatewayType = GatewayType.CLIENT))
                            }
                        )
                        Text("Client")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = config.gatewayType == GatewayType.PROXY,
                            onClick = {
                                onConfigChange(config.copy( gatewayType = GatewayType.PROXY))
                            }
                        )
                        Text("Proxy")
                    }
                }
            }
        }

        // Creating Info group
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Creating Info", style = MaterialTheme.typography.subtitle1)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Name", modifier = Modifier.width(100.dp))
                    TextField(
                        value = config.name,
                        onValueChange = {
                            onConfigChange(config.copy(name = it))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Modified date", modifier = Modifier.width(100.dp))
                    Text(
                        Utils.formatDate(config.createDate),
                        modifier = Modifier.weight(1f).border(1.dp, Color.LightGray).padding(8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Description", modifier = Modifier.width(100.dp))
                    TextField(
                        value = config.description,
                        onValueChange = {
                            onConfigChange(config.copy(description = it))
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = config.enable,
                        onCheckedChange = {
                            onConfigChange(config.copy( enable = it))
                        }
                    )
                    Text("Enable when Windows starts")
                }

                // Buttons for special tests
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { /* Test POS Gateway logic */ }) {
                        Text("Test PosGateway")
                    }

                    Button(onClick = { /* Test EVN Component logic */ }) {
                        Text("Test EVN Component")
                    }

                    Button(onClick = { /* Configure Switch logic */ }) {
                        Text("Configure Switch")
                    }
                }
            }
        }

        // Server Settings or Client Info based on gateway type
        if (config.gatewayType == GatewayType.SERVER) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Server security settings", style = MaterialTheme.typography.subtitle1)
                    Text("This section is configured in web based user interface")
                }
            }
        } else if (config.gatewayType == GatewayType.CLIENT) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Client info", style = MaterialTheme.typography.subtitle1)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Client ID", modifier = Modifier.width(100.dp))
                        TextField(
                            value = config.clientID,
                            onValueChange = {
                                onConfigChange(config.copy(clientID = it))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Location ID", modifier = Modifier.width(100.dp))
                        TextField(
                            value = config.locationID,
                            onValueChange = {
                                onConfigChange(config.copy(locationID = it))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Password", modifier = Modifier.width(100.dp))
                        TextField(
                            value = config.password?.let { String(Hex.encode(it)) } ?: "",
                            onValueChange = { newValue ->
                                try {
                                    // Only update if valid hex
                                    val newPasswordBytes = if (newValue.isEmpty()) null else Hex.decode(newValue)
                                    onConfigChange(config.copy(password = newPasswordBytes))
                                } catch (e: Exception) {
                                    // Handle invalid hex input if needed
                                    // You might want to show an error message or ignore the input
                                }
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}