package `in`.aicortex.iso8583studio.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        // Transmission Type group
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Transmission Type", style = MaterialTheme.typography.subtitle1)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        RadioButton(
                            selected = config.transmissionType == TransmissionType.SYNCHRONOUS,
                            onClick = {
                                onConfigChange(config.copy(transmissionType = TransmissionType.SYNCHRONOUS))
                            })
                    }
                    Text("Synchronous")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = config.transmissionType == TransmissionType.ASYNCHRONOUS,
                        onClick = {
                            onConfigChange(config.copy(transmissionType = TransmissionType.ASYNCHRONOUS))
                        }
                    )
                    Text("Asynchronous")
                }
            }
        }
    }

    // Incoming Options group
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Incoming Options", style = MaterialTheme.typography.subtitle1)

            // Connection type selection
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    RadioButton(
                        selected = config.serverConnectionType == ConnectionType.TCP_IP,
                        onClick = {
                            onConfigChange(config.copy(serverConnectionType = ConnectionType.TCP_IP))
                        }
                    )
                    Text("TCP/IP")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    RadioButton(
                        selected = config.serverConnectionType == ConnectionType.COM,
                        onClick = {
                            onConfigChange(config.copy(serverConnectionType = ConnectionType.COM))
                        }
                    )
                    Text("RS232")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = config.serverConnectionType == ConnectionType.DIAL_UP,
                        onClick = {
                            onConfigChange(config.copy(serverConnectionType = ConnectionType.DIAL_UP))
                        }
                    )
                    Text("DIAL UP")
                }
            }

            // TCP/IP settings
            if (config.serverConnectionType == ConnectionType.TCP_IP) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = 1.dp,
                    backgroundColor = Color.LightGray.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("IP Address", modifier = Modifier.width(100.dp))
                            TextField(
                                value = config.serverAddress,
                                onValueChange = {
                                    onConfigChange(config.copy(serverAddress = it))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Port", modifier = Modifier.width(100.dp))
                            TextField(
                                value = config.serverPort.toString(),
                                onValueChange = {
                                    val port = it.toIntOrNull() ?: config.serverPort
                                    onConfigChange(config.copy(serverPort = port))
                                },
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }
            }

            // RS232 settings
            else if (config.serverConnectionType == ConnectionType.COM) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = 1.dp,
                    backgroundColor = Color.LightGray.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("COMPORT", modifier = Modifier.width(100.dp))
                            var expanded by remember { mutableStateOf(false) }
                            val comPorts = listOf(
                                "COM1",
                                "COM2",
                                "COM3",
                                "COM4",
                                "COM5",
                                "COM6",
                                "COM7",
                                "COM8",
                                "COM9"
                            )

                            Box {
                                Button(onClick = { expanded = true }) {
                                    Text(config.serverAddress.ifEmpty { "Select COM port" })
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    comPorts.forEach { port ->
                                        DropdownMenuItem(onClick = {
                                            onConfigChange(config.copy(serverAddress = port))
                                            expanded = false
                                        }) {
                                            Text(port)
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Baud rate", modifier = Modifier.width(100.dp))
                            var expanded by remember { mutableStateOf(false) }
                            val baudRates = listOf(
                                "115200",
                                "9600",
                                "14400",
                                "19200",
                                "28800",
                                "38400",
                                "57600"
                            )

                            Box {
                                Button(onClick = { expanded = true }) {
                                    Text(config.serverPort.toString())
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    baudRates.forEach { baud ->
                                        DropdownMenuItem(onClick = {
                                            onConfigChange(config.copy(serverPort = baud.toInt()))
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

            // DIAL UP settings
            else if (config.serverConnectionType == ConnectionType.DIAL_UP) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = 1.dp,
                    backgroundColor = Color.LightGray.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tel number", modifier = Modifier.width(100.dp))
                            TextField(
                                value = config.serverAddress,
                                onValueChange = {
                                    onConfigChange(config.copy(serverAddress = it))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Outcoming Options group
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Outcoming Options", style = MaterialTheme.typography.subtitle1)

            // Connection type selection
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    RadioButton(
                        selected = config.destinationConnectionType == ConnectionType.TCP_IP,
                        onClick = {
                            onConfigChange(config.copy(destinationConnectionType = ConnectionType.TCP_IP))
                        }
                    )
                    Text("TCP/IP")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    RadioButton(
                        selected = config.destinationConnectionType == ConnectionType.COM,
                        onClick = {
                            onConfigChange(config.copy(destinationConnectionType = ConnectionType.COM))
                        }
                    )
                    Text("RS232")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = config.destinationConnectionType == ConnectionType.DIAL_UP,
                        onClick = {
                            onConfigChange(config.copy(destinationConnectionType = ConnectionType.DIAL_UP))
                        }
                    )
                    Text("DIAL UP")
                }
            }

            // TCP/IP settings
            if (config.destinationConnectionType == ConnectionType.TCP_IP) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = 1.dp,
                    backgroundColor = Color.LightGray.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("IP Address", modifier = Modifier.width(100.dp))
                            TextField(
                                value = config.destinationServer,
                                onValueChange = {
                                    onConfigChange(config.copy(destinationServer = it))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Port", modifier = Modifier.width(100.dp))
                            TextField(
                                value = config.destinationPort.toString(),
                                onValueChange = {
                                    val port = it.toIntOrNull() ?: config.destinationPort
                                    onConfigChange(config.copy(destinationPort = port))
                                },
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }
            }

            // RS232 settings would go here (similar to incoming options)
            // DIAL UP settings would go here (similar to incoming options)
        }
    }

    // Additional settings group
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Time out", modifier = Modifier.width(150.dp))
                TextField(
                    value = config.transactionTimeOut.toString(),
                    onValueChange = {
                        val timeout = it.toIntOrNull() ?: config.transactionTimeOut
                        onConfigChange(config.copy(transactionTimeOut = timeout))
                    },
                    modifier = Modifier.width(100.dp)
                )
                Text("s", modifier = Modifier.padding(start = 4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Max concurrent connections", modifier = Modifier.width(150.dp))
                TextField(
                    value = config.maxConcurrentConnection.toString(),
                    onValueChange = {
                        val connections = it.toIntOrNull() ?: config.maxConcurrentConnection
                        onConfigChange(config.copy(maxConcurrentConnection = connections))
                    },
                    modifier = Modifier.width(100.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = config.terminateWhenError,
                    onCheckedChange = {
                        onConfigChange(config.copy(terminateWhenError = it))
                    }
                )
                Text("Terminate connection when error")
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Message Length Type", modifier = Modifier.width(150.dp))
                var expanded by remember { mutableStateOf(false) }
                val lengthTypes = MessageLengthType.values()

                Box {
                    Button(onClick = { expanded = true }) {
                        Text(config.messageLengthType.name)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        lengthTypes.forEachIndexed { index, type ->
                            DropdownMenuItem(onClick = {
                                onConfigChange(config.copy(messageLengthType = type))
                                expanded = false
                            }) {
                                Text(MessageLengthType.values()[index].name)
                            }
                        }
                    }
                }
            }
        }
    }
}
