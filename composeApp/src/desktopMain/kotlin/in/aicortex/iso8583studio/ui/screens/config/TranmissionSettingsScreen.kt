package `in`.aicortex.iso8583studio.ui.screens.config

import RestAuthConfig
import RestSslConfig
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SecurityUpdate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.ConnectionType
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.data.model.HttpMethod
import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import `in`.aicortex.iso8583studio.data.model.RestConfiguration
import `in`.aicortex.iso8583studio.data.model.TransmissionType
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentBanner
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentChip
import kotlinx.serialization.json.Json


/**
 * Transmission Settings tab - Second tab in the Security Gateway configuration
 * Contains network and connection settings including REST API support
 */
@Composable
fun TransmissionSettingsTab(config: GatewayConfig, onConfigChange: (GatewayConfig) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (config.gatewayType == GatewayType.PROXY) {


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
        }
        // Incoming Connection section (only for SERVER and PROXY)
        if (config.gatewayType == GatewayType.SERVER || config.gatewayType == GatewayType.PROXY) {
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

                        RadioButton(
                            selected = config.serverConnectionType == ConnectionType.REST,
                            onClick = {
                                onConfigChange(config.copy(serverConnectionType = ConnectionType.REST))
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colors.primary
                            )
                        )
                        Text("REST", modifier = Modifier.padding(start = 8.dp))
                    }

                    // Connection type specific settings
                    when (config.serverConnectionType) {
                        ConnectionType.TCP_IP, ConnectionType.REST -> {
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
                                onBaudRateChange = { onConfigChange(config.copy(baudRate = it)) }
                            )
                        }

                        ConnectionType.DIAL_UP -> {
                            DialUpSettings(
                                phoneNumber = config.dialupNumber,
                                onPhoneNumberChange = { onConfigChange(config.copy(dialupNumber = it)) }
                            )
                        }
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
                                Text(config.messageLengthTypeSource.name)
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                MessageLengthType.values().forEach { type ->
                                    DropdownMenuItem(onClick = {
                                        onConfigChange(config.copy(messageLengthTypeSource = type))
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

        // Outgoing Connection section (only for PROXY and CLIENT)
        if (config.gatewayType == GatewayType.PROXY || config.gatewayType == GatewayType.CLIENT) {
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

                    // Connection type selection with REST option

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

                        Spacer(modifier = Modifier.width(16.dp))

                        RadioButton(
                            selected = config.destinationConnectionType == ConnectionType.REST,
                            onClick = {
                                onConfigChange(config.copy(destinationConnectionType = ConnectionType.REST))
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colors.primary
                            )
                        )
                        Text("REST API", modifier = Modifier.padding(start = 8.dp))
                    }


                    // Connection type specific settings
                    when (config.destinationConnectionType) {
                        ConnectionType.TCP_IP -> {
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

                        ConnectionType.COM -> {
                            UnderDevelopmentBanner()
//                            ComSettings(
//                                comPort = config.destinationSerialPort ?: "",
//                                baudRate = config.destinationBaudRate ?: "",
//                                onComPortChange = {
//                                    // You'll need to add destinationSerialPort to GatewayConfig
//                                    // onConfigChange(config.copy(destinationSerialPort = it))
//                                },
//                                onBaudRateChange = {
//                                    // You'll need to add destinationBaudRate to GatewayConfig
//                                    // onConfigChange(config.copy(destinationBaudRate = it))
//                                }
//                            )
                        }

                        ConnectionType.DIAL_UP -> {
                            UnderDevelopmentBanner()
//                            DialUpSettings(
//                                phoneNumber = config.destinationDialupNumber ?: "",
//                                onPhoneNumberChange = {
//                                    // You'll need to add destinationDialupNumber to GatewayConfig
//                                    // onConfigChange(config.copy(destinationDialupNumber = it))
//                                }
//                            )
                        }

                        // Uncomment when REST is added to ConnectionType enum

                        ConnectionType.REST -> {
                            var restConfig by remember { mutableStateOf(config.restConfiguration ?: RestConfiguration()) }
                            RestSettings(
                                restConfig = restConfig,
                                onRestConfigChange = { newRestConfig ->
                                    restConfig = newRestConfig
                                    onConfigChange(config.copy(restConfiguration = newRestConfig))
                                }
                            )
                        }

                    }


                    // Message Length Type Destination
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
                                Text(config.messageLengthTypeDest.name)
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                MessageLengthType.values().forEach { type ->
                                    DropdownMenuItem(onClick = {
                                        onConfigChange(config.copy(messageLengthTypeDest = type))
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

            }
        }
    }
}

// REST Settings Component
@Composable
private fun RestSettings(
    restConfig: RestConfiguration,
    onRestConfigChange: (RestConfiguration) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSections by remember { mutableStateOf(setOf("basic")) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colors.surface.copy(alpha = 0.7f),
        border = ButtonDefaults.outlinedBorder
    ) {
        Column {


        // Header with validation status
        Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "REST API Configuration",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (restConfig.isValid()) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (restConfig.isValid())
                                MaterialTheme.colors.primary
                            else
                                MaterialTheme.colors.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            if (restConfig.isValid()) "Valid" else "Invalid",
                            color = if (restConfig.isValid())
                                MaterialTheme.colors.primary
                            else
                                MaterialTheme.colors.error
                        )
                    }
                }

        }

        // Basic Configuration Section
        Column {
            ExpandableConfigSection(
                title = "Basic Configuration",
                icon = Icons.Default.Settings,
                isExpanded = expandedSections.contains("basic"),
                onToggle = {
                    expandedSections = if (expandedSections.contains("basic")) {
                        expandedSections - "basic"
                    } else {
                        expandedSections + "basic"
                    }
                }
            ) {
                BasicRestConfiguration(restConfig, onRestConfigChange)
            }
        }

        // Headers Configuration Section
        Column {
            ExpandableConfigSection(
                title = "Headers Configuration",
                icon = Icons.Default.List,
                isExpanded = expandedSections.contains("headers"),
                onToggle = {
                    expandedSections = if (expandedSections.contains("headers")) {
                        expandedSections - "headers"
                    } else {
                        expandedSections + "headers"
                    }
                }
            ) {
                HeadersConfiguration(restConfig, onRestConfigChange)
            }
        }

        // Authentication Section
        Column {
            ExpandableConfigSection(
                title = "Authentication",
                icon = Icons.Default.Security,
                isExpanded = expandedSections.contains("auth"),
                onToggle = {
                    expandedSections = if (expandedSections.contains("auth")) {
                        expandedSections - "auth"
                    } else {
                        expandedSections + "auth"
                    }
                }
            ) {
                AuthenticationConfiguration(restConfig, onRestConfigChange)
            }
        }

        // Retry Configuration Section
        Column {
            ExpandableConfigSection(
                title = "Retry Configuration",
                icon = Icons.Default.Refresh,
                isExpanded = expandedSections.contains("retry"),
                onToggle = {
                    expandedSections = if (expandedSections.contains("retry")) {
                        expandedSections - "retry"
                    } else {
                        expandedSections + "retry"
                    }
                }
            ) {
                RetryConfiguration(restConfig, onRestConfigChange)
            }
        }

        // SSL Configuration Section
        Column {
            ExpandableConfigSection(
                title = "SSL/TLS Configuration",
                icon = Icons.Default.Lock,
                isExpanded = expandedSections.contains("ssl"),
                onToggle = {
                    expandedSections = if (expandedSections.contains("ssl")) {
                        expandedSections - "ssl"
                    } else {
                        expandedSections + "ssl"
                    }
                }
            ) {
                SslConfiguration(restConfig, onRestConfigChange)
            }
        }

        // Configuration Preview
        Column {
            ExpandableConfigSection(
                title = "Configuration Preview",
                icon = Icons.Default.Visibility,
                isExpanded = expandedSections.contains("preview"),
                onToggle = {
                    expandedSections = if (expandedSections.contains("preview")) {
                        expandedSections - "preview"
                    } else {
                        expandedSections + "preview"
                    }
                }
            ) {
                ConfigurationPreview(restConfig)
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BasicRestConfiguration(
    restConfig: RestConfiguration,
    onRestConfigChange: (RestConfiguration) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // URL Configuration
        ConfigField(
            label = "Endpoint URL",
            isRequired = true,
            description = "The complete REST API endpoint URL"
        ) {
            OutlinedTextField(
                value = restConfig.url,
                onValueChange = {
                    onRestConfigChange(restConfig.copy(url = it))
                                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://api.example.com/v1/transactions") },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null)
                },
                isError = restConfig.url.isNotBlank() && !restConfig.isValid(),
            )
        }

        // HTTP Method Selection
        ConfigField(
            label = "HTTP Method",
            description = "HTTP method for API requests"
        ) {
            var methodExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = methodExpanded,
                onExpandedChange = { methodExpanded = it }
            ) {
                OutlinedTextField(
                    value = restConfig.method.name,
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = methodExpanded,
                    onDismissRequest = { methodExpanded = false }
                ) {
                    HttpMethod.values().forEach { method ->
                        DropdownMenuItem(
                            onClick = {
                                onRestConfigChange(restConfig.copy(method = method))
                                methodExpanded = false
                            },
                            content = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(method.name)
                                    if (method == restConfig.method) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }


        // Message Format Selection
        ConfigField(
            label = "Message Format",
            description = "Format for request/response payloads"
        ) {
            var formatExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = formatExpanded,
                onExpandedChange = { formatExpanded = it }
            ) {
                OutlinedTextField(
                    value = restConfig.messageFormat.name,
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = formatExpanded,
                    onDismissRequest = { formatExpanded = false }
                ) {
                    RestMessageFormat.values().forEach { format ->
                        DropdownMenuItem(
                            onClick = {
                                onRestConfigChange(restConfig.copy(messageFormat = format))
                                formatExpanded = false
                            },
                            content = {

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(format.name)
                                        if (format == restConfig.messageFormat) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        format.description,
                                        color = MaterialTheme.colors.onSurface
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadersConfiguration(
    restConfig: RestConfiguration,
    onRestConfigChange: (RestConfiguration) -> Unit
) {
    var newHeaderKey by remember { mutableStateOf("") }
    var newHeaderValue by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Custom Headers",
            fontWeight = FontWeight.Medium
        )

        // Existing headers
        restConfig.headers.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            key,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            value,
                            color = MaterialTheme.colors.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            val newHeaders = restConfig.headers.toMutableMap()
                            newHeaders.remove(key)
                            onRestConfigChange(restConfig.copy(headers = newHeaders))
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove header",
                            tint = MaterialTheme.colors.error
                        )
                    }
                }

        }

        // Add new header
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Add New Header",
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newHeaderKey,
                        onValueChange = { newHeaderKey = it },
                        label = { Text("Header Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newHeaderValue,
                        onValueChange = { newHeaderValue = it },
                        label = { Text("Header Value") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Button(
                    onClick = {
                        if (newHeaderKey.isNotBlank() && newHeaderValue.isNotBlank()) {
                            val newHeaders = restConfig.headers.toMutableMap()
                            newHeaders[newHeaderKey] = newHeaderValue
                            onRestConfigChange(restConfig.copy(headers = newHeaders))
                            newHeaderKey = ""
                            newHeaderValue = ""
                        }
                    },
                    enabled = newHeaderKey.isNotBlank() && newHeaderValue.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Header")
                }
            }

    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AuthenticationConfiguration(
    restConfig: RestConfiguration,
    onRestConfigChange: (RestConfiguration) -> Unit
) {
    val authConfig = restConfig.authConfig ?: RestAuthConfig()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Authentication Type Selection
        ConfigField(
            label = "Authentication Type",
            description = "Authentication method for API requests"
        ) {
            var authExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = authExpanded,
                onExpandedChange = { authExpanded = it }
            ) {
                OutlinedTextField(
                    value = authConfig.type.name,
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = authExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = authExpanded,
                    onDismissRequest = { authExpanded = false }
                ) {
                    RestAuthType.values().forEach { authType ->
                        DropdownMenuItem(
                            onClick = {
                                onRestConfigChange(
                                    restConfig.copy(
                                        authConfig = authConfig.copy(type = authType)
                                    )
                                )
                                authExpanded = false
                            },
                            content = {
                                Text(authType.name)
                            }
                        )
                    }
                }
            }
        }

        // Authentication fields based on type
        when (authConfig.type) {
            RestAuthType.BASIC -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = authConfig.username,
                        onValueChange = {
                            onRestConfigChange(
                                restConfig.copy(
                                    authConfig = authConfig.copy(username = it)
                                )
                            )
                        },
                        label = { Text("Username") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                    OutlinedTextField(
                        value = authConfig.password,
                        onValueChange = {
                            onRestConfigChange(
                                restConfig.copy(
                                    authConfig = authConfig.copy(password = it)
                                )
                            )
                        },
                        label = { Text("Password") },
                        modifier = Modifier.weight(1f),
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                    )
                }
            }
            RestAuthType.BEARER, RestAuthType.OAUTH2 -> {
                OutlinedTextField(
                    value = authConfig.token,
                    onValueChange = {
                        onRestConfigChange(
                            restConfig.copy(
                                authConfig = authConfig.copy(token = it)
                            )
                        )
                    },
                    label = { Text("Bearer Token") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            RestAuthType.API_KEY -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = authConfig.keyHeader,
                        onValueChange = {
                            onRestConfigChange(
                                restConfig.copy(
                                    authConfig = authConfig.copy(keyHeader = it)
                                )
                            )
                        },
                        label = { Text("Header Name") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("X-API-Key") }
                    )
                    OutlinedTextField(
                        value = authConfig.apiKey,
                        onValueChange = {
                            onRestConfigChange(
                                restConfig.copy(
                                    authConfig = authConfig.copy(apiKey = it)
                                )
                            )
                        },
                        label = { Text("API Key") },
                        modifier = Modifier.weight(1f),
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) }
                    )
                }
            }
            RestAuthType.NONE -> {
                    Text(
                        "No authentication configured",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colors.onSurface
                    )

            }
        }
    }
}

@Composable
private fun RetryConfiguration(
    restConfig: RestConfiguration,
    onRestConfigChange: (RestConfiguration) -> Unit
) {
    val retryConfig = restConfig.retryConfig

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Max Retries
        ConfigField(
            label = "Maximum Retries",
            description = "Number of retry attempts on failure"
        ) {
            var retriesText by remember { mutableStateOf(retryConfig.maxRetries.toString()) }

            OutlinedTextField(
                value = retriesText,
                onValueChange = { newValue ->
                    retriesText = newValue
                    newValue.toIntOrNull()?.let { retries ->
                        if (retries >= 0) {
                            onRestConfigChange(
                                restConfig.copy(
                                    retryConfig = retryConfig.copy(maxRetries = retries)
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
            )
        }

        // Retry Options
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = retryConfig.retryOnTimeout,
                    onCheckedChange = {
                        onRestConfigChange(
                            restConfig.copy(
                                retryConfig = retryConfig.copy(retryOnTimeout = it)
                            )
                        )
                    }
                )
                Text(
                    "Retry on timeout"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = retryConfig.retryOnServerError,
                    onCheckedChange = {
                        onRestConfigChange(
                            restConfig.copy(
                                retryConfig = retryConfig.copy(retryOnServerError = it)
                            )
                        )
                    }
                )
                Text(
                    "Retry on server errors (5xx)"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = retryConfig.exponentialBackoff,
                    onCheckedChange = {
                        onRestConfigChange(
                            restConfig.copy(
                                retryConfig = retryConfig.copy(exponentialBackoff = it)
                            )
                        )
                    }
                )
                Text(
                    "Use exponential backoff"
                )
            }
        }
    }
}

@Composable
private fun SslConfiguration(
    restConfig: RestConfiguration,
    onRestConfigChange: (RestConfiguration) -> Unit
) {
    val sslConfig = restConfig.sslConfig ?: RestSslConfig()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = sslConfig.trustAllCertificates,
                onCheckedChange = {
                    onRestConfigChange(
                        restConfig.copy(
                            sslConfig = sslConfig.copy(trustAllCertificates = it)
                        )
                    )
                }
            )
            Column {
                Text(
                    "Trust all certificates",
                )
                Text(
                    "⚠️ Only for development/testing",
                    color = MaterialTheme.colors.error
                )
            }
        }

        if (!sslConfig.trustAllCertificates) {
            OutlinedTextField(
                value = sslConfig.certificatePath,
                onValueChange = {
                    onRestConfigChange(
                        restConfig.copy(
                            sslConfig = sslConfig.copy(certificatePath = it)
                        )
                    )
                },
                label = { Text("Certificate Path") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("/path/to/certificate.pem") },
                leadingIcon = { Icon(Icons.Default.SecurityUpdate, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun ConfigurationPreview(restConfig: RestConfiguration) {
    val jsonString = remember(restConfig) {
        Json {
            prettyPrint = true
            encodeDefaults = true
        }.encodeToString(RestConfiguration.serializer(), restConfig)
    }

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Configuration JSON",
                )
                IconButton(
                    onClick = {
                        // Copy to clipboard functionality
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy to clipboard")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(12.dp)
            ) {
                item {
                    SelectionContainer {
                        Text(
                            jsonString,
                            color = MaterialTheme.colors.onSurface
                        )
                    }
                }
            }
        }

}

@Composable
private fun ExpandableConfigSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        title,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colors.onSurface
                )
            }

            // Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colors.secondaryVariant
                    )
                    content()
                }
            }
        }

}

@Composable
private fun ConfigField(
    label: String,
    isRequired: Boolean = false,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                fontWeight = FontWeight.Medium
            )
            if (isRequired) {
                Text(
                    "*",
                    color = MaterialTheme.colors.error,
                )
            }
        }

        description?.let {
            Text(
                it,
                color = MaterialTheme.colors.onSurface
            )
        }

        content()
    }
}

// Helper Components
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
            UnderDevelopmentChip()
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
                val comPorts =
                    listOf("COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9")

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
                val baudRates =
                    listOf("115200", "9600", "14400", "19200", "28800", "38400", "57600")

                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = MaterialTheme.colors.onSurface
                        )
                    ) {
                        Text(baudRate.ifEmpty { "Select baud rate" })
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
            UnderDevelopmentChip()
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