package `in`.aicortex.iso8583studio.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.ConnectionType
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import `in`.aicortex.iso8583studio.data.model.TransmissionType

/**
 * Transmission Settings tab - Second tab in the Security Gateway configuration
 * Contains network and connection settings
 */
@Composable
fun TransmissionSettingsTab(config: GatewayConfig, onConfigChange: (GatewayConfig) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Transmission Type section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Transmission Type",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = config.transmissionType == TransmissionType.SYNCHRONOUS,
                        onClick = {
                            onConfigChange(config.copy(transmissionType = TransmissionType.SYNCHRONOUS))
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colors.primary
                        )
                    )
                    Text("Synchronous", modifier = Modifier.padding(start = 8.dp))

                    Spacer(modifier = Modifier.width(32.dp))

                    RadioButton(
                        selected = config.transmissionType == TransmissionType.ASYNCHRONOUS,
                        onClick = {
                            onConfigChange(config.copy(transmissionType = TransmissionType.ASYNCHRONOUS))
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colors.primary
                        )
                    )
                    Text("Asynchronous", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        // Incoming Connection section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Incoming Connection",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                // Connection type selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = config.serverConnectionType == ConnectionType.TCP_IP,
                        onClick = {
                            onConfigChange(config.copy(serverConnectionType = ConnectionType.TCP_IP))
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colors.primary
                        )
                    )
                    Text("TCP/IP", modifier = Modifier.padding(start = 8.dp))

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = config.serverConnectionType == ConnectionType.COM,
                        onClick = {
                            onConfigChange(config.copy(serverConnectionType = ConnectionType.COM))
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colors.primary
                        )
                    )
                    Text("RS232", modifier = Modifier.padding(start = 8.dp))

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = config.serverConnectionType == ConnectionType.DIAL_UP,
                        onClick = {
                            onConfigChange(config.copy(serverConnectionType = ConnectionType.DIAL_UP))
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colors.primary
                        )
                    )
                    Text("DIAL UP", modifier = Modifier.padding(start = 8.dp))
                }

                // Connection type specific settings
                when (config.serverConnectionType) {
                    ConnectionType.TCP_IP -> {
                        TcpIpSettings(
                            address = config.serverAddress,
                            port = config.serverPort.toString(),
                            onAddressChange = { onConfigChange(config.copy(serverAddress = it)) },
                            onPortChange = {
                                val port = it.toIntOrNull() ?: config.serverPort
                                onConfigChange(config.copy(serverPort = port))
                            }
                        )
                    }
                    ConnectionType.COM -> {
                        ComSettings(
                            comPort = config.serialPort,
                            baudRate = config.baudRate,
                            onComPortChange = { onConfigChange(config.copy(serialPort = it)) },
                            onBaudRateChange = {
                                onConfigChange(config.copy(baudRate = it))
                            }
                        )
                    }
                    ConnectionType.DIAL_UP -> {
                        DialUpSettings(
                            phoneNumber = config.dialupNumber,
                            onPhoneNumberChange = { onConfigChange(config.copy(dialupNumber = it)) }
                        )
                    }
                }
            }
        }

        // Outgoing Connection section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Outgoing Connection",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                // Connection type selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = config.destinationConnectionType == ConnectionType.TCP_IP,
                        onClick = {
                            onConfigChange(config.copy(destinationConnectionType = ConnectionType.TCP_IP))
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colors.primary
                        )
                    )
                    Text("TCP/IP", modifier = Modifier.padding(start = 8.dp))

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = config.destinationConnectionType == ConnectionType.COM,
                        onClick = {
                            onConfigChange(config.copy(destinationConnectionType = ConnectionType.COM))
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colors.primary
                        )
                    )
                    Text("RS232", modifier = Modifier.padding(start = 8.dp))

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = config.destinationConnectionType == ConnectionType.DIAL_UP,
                        onClick = {
                            onConfigChange(config.copy(destinationConnectionType = ConnectionType.DIAL_UP))
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colors.primary
                        )
                    )
                    Text("DIAL UP", modifier = Modifier.padding(start = 8.dp))
                }

                // Connection type specific settings
                if (config.destinationConnectionType == ConnectionType.TCP_IP) {
                    TcpIpSettings(
                        address = config.destinationServer,
                        port = config.destinationPort.toString(),
                        onAddressChange = { onConfigChange(config.copy(destinationServer = it)) },
                        onPortChange = {
                            val port = it.toIntOrNull() ?: config.destinationPort
                            onConfigChange(config.copy(destinationPort = port))
                        }
                    )
                }
            }
        }

        // Additional Settings section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Additional Settings",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                // Time out setting
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Time out",
                        modifier = Modifier.width(180.dp),
                        style = MaterialTheme.typography.body1
                    )
                    OutlinedTextField(
                        value = config.transactionTimeOut.toString(),
                        onValueChange = {
                            val timeout = it.toIntOrNull() ?: config.transactionTimeOut
                            onConfigChange(config.copy(transactionTimeOut = timeout))
                        },
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            cursorColor = MaterialTheme.colors.primary
                        )
                    )
                    Text("seconds", modifier = Modifier.padding(start = 8.dp))
                }

                // Max concurrent connections
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Max concurrent connections",
                        modifier = Modifier.width(180.dp),
                        style = MaterialTheme.typography.body1
                    )
                    OutlinedTextField(
                        value = config.maxConcurrentConnection.toString(),
                        onValueChange = {
                            val connections = it.toIntOrNull() ?: config.maxConcurrentConnection
                            onConfigChange(config.copy(maxConcurrentConnection = connections))
                        },
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            cursorColor = MaterialTheme.colors.primary
                        )
                    )
                }

                // Terminate when error
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = config.terminateWhenError,
                        onCheckedChange = {
                            onConfigChange(config.copy(terminateWhenError = it))
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colors.primary
                        )
                    )
                    Text(
                        "Terminate connection when error",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Message Length Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Message Length Type",
                        modifier = Modifier.width(180.dp),
                        style = MaterialTheme.typography.body1
                    )

                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = Color.Transparent,
                                contentColor = MaterialTheme.colors.onSurface
                            )
                        ) {
                            Text(config.messageLengthType.name)
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            MessageLengthType.values().forEach { type ->
                                DropdownMenuItem(onClick = {
                                    onConfigChange(config.copy(messageLengthType = type))
                                    expanded = false
                                }) {
                                    Text(type.name)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TcpIpSettings(
    address: String,
    port: String,
    onAddressChange: (String) -> Unit,
    onPortChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colors.surface.copy(alpha = 0.7f),
        border = ButtonDefaults.outlinedBorder
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "IP Address",
                    modifier = Modifier.width(100.dp),
                    style = MaterialTheme.typography.body1
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.primary,
                        cursorColor = MaterialTheme.colors.primary
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Port",
                    modifier = Modifier.width(100.dp),
                    style = MaterialTheme.typography.body1
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = onPortChange,
                    modifier = Modifier.width(120.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.primary,
                        cursorColor = MaterialTheme.colors.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun ComSettings(
    comPort: String,
    baudRate: String,
    onComPortChange: (String) -> Unit,
    onBaudRateChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colors.surface.copy(alpha = 0.7f),
        border = ButtonDefaults.outlinedBorder
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "COM Port",
                    modifier = Modifier.width(100.dp),
                    style = MaterialTheme.typography.body1
                )

                var expanded by remember { mutableStateOf(false) }
                val comPorts = listOf("COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9")

                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = MaterialTheme.colors.onSurface
                        )
                    ) {
                        Text(comPort.ifEmpty { "Select COM port" })
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        comPorts.forEach { port ->
                            DropdownMenuItem(onClick = {
                                onComPortChange(port)
                                expanded = false
                            }) {
                                Text(port)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Baud Rate",
                    modifier = Modifier.width(100.dp),
                    style = MaterialTheme.typography.body1
                )

                var expanded by remember { mutableStateOf(false) }
                val baudRates = listOf("115200", "9600", "14400", "19200", "28800", "38400", "57600")

                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = MaterialTheme.colors.onSurface
                        )
                    ) {
                        Text(baudRate)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        baudRates.forEach { baud ->
                            DropdownMenuItem(onClick = {
                                onBaudRateChange(baud)
                                expanded = false
                            }) {
                                Text(baud)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialUpSettings(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colors.surface.copy(alpha = 0.7f),
        border = ButtonDefaults.outlinedBorder
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Phone Number",
                    modifier = Modifier.width(100.dp),
                    style = MaterialTheme.typography.body1
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.primary,
                        cursorColor = MaterialTheme.colors.primary
                    )
                )
            }
        }
    }
}