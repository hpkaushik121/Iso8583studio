package `in`.aicortex.iso8583studio.ui.screens.config.hostSimulator

import RestAuthConfig
import RestSslConfig
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
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
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.SectionHeader
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentBanner
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentChip
import kotlinx.serialization.json.Json

/**
 * Reimagined Transmission Settings Tab with improved design and organization
 * Maintains all original functionality with enhanced UX
 */
@Composable
fun TransmissionSettingsTab(config: GatewayConfig, onConfigChange: (GatewayConfig) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Transmission Type Section (for PROXY)
        if (config.gatewayType == GatewayType.PROXY) {
            TransmissionTypeSection(
                transmissionType = config.transmissionType,
                onTransmissionTypeChange = {
                    onConfigChange(config.copy(transmissionType = it))
                }
            )
        }

        // Incoming Connection Section (SERVER and PROXY)
        if (config.gatewayType == GatewayType.SERVER || config.gatewayType == GatewayType.PROXY) {
            IncomingConnectionSection(
                config = config,
                onConfigChange = onConfigChange
            )
        }

        // Outgoing Connection Section (PROXY and CLIENT)
        if (config.gatewayType == GatewayType.PROXY || config.gatewayType == GatewayType.CLIENT) {
            OutgoingConnectionSection(
                config = config,
                onConfigChange = onConfigChange
            )
        }

        // Additional Settings Section
        AdditionalSettingsSection(
            config = config,
            onConfigChange = onConfigChange
        )
    }
}

@Composable
private fun TransmissionTypeSection(
    transmissionType: TransmissionType,
    onTransmissionTypeChange: (TransmissionType) -> Unit
) {
    Panel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Transmission Type",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                SectionHeader(title = "TRANSMISSION TYPE")
            }

            Text(
                "Configure how data flows through the proxy connection",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TransmissionOption(
                    selected = transmissionType == TransmissionType.SYNCHRONOUS,
                    onClick = { onTransmissionTypeChange(TransmissionType.SYNCHRONOUS) },
                    title = "Synchronous",
                    description = "Wait for response before next request",
                    icon = Icons.Default.SwapHoriz,
                    modifier = Modifier.weight(1f)
                )

                TransmissionOption(
                    selected = transmissionType == TransmissionType.ASYNCHRONOUS,
                    onClick = { onTransmissionTypeChange(TransmissionType.ASYNCHRONOUS) },
                    title = "Asynchronous",
                    description = "Process multiple requests concurrently",
                    icon = Icons.Default.MultipleStop,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun IncomingConnectionSection(
    config: GatewayConfig,
    onConfigChange: (GatewayConfig) -> Unit
) {
    Panel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallReceived,
                    contentDescription = "Incoming Connection",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                SectionHeader(title = "INCOMING CONNECTIONS")
            }

            Text(
                "Configure how clients connect to this host simulator",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Connection Type Selection
            ConnectionTypeSelector(
                selectedType = config.serverConnectionType,
                onTypeSelected = { onConfigChange(config.copy(serverConnectionType = it)) },
                availableTypes = listOf(
                    ConnectionType.TCP_IP to "TCP/IP",
                    ConnectionType.COM to "RS232",
                    ConnectionType.DIAL_UP to "Dial-up",
                    ConnectionType.REST to "REST API"
                )
            )

            // Connection Settings based on type
            when (config.serverConnectionType) {
                ConnectionType.TCP_IP, ConnectionType.REST -> {
                    TcpIpSettingsCard(
                        title = "Network Configuration",
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
                    ComSettingsCard(
                        comPort = config.serialPort,
                        baudRate = config.baudRate,
                        onComPortChange = { onConfigChange(config.copy(serialPort = it)) },
                        onBaudRateChange = { onConfigChange(config.copy(baudRate = it)) }
                    )
                }
                ConnectionType.DIAL_UP -> {
                    DialUpSettingsCard(
                        phoneNumber = config.dialupNumber,
                        onPhoneNumberChange = { onConfigChange(config.copy(dialupNumber = it)) }
                    )
                }
            }

            // Message Length Type
            MessageLengthSelector(
                title = "Incoming Message Length",
                selectedType = config.messageLengthTypeSource,
                onTypeSelected = { onConfigChange(config.copy(messageLengthTypeSource = it)) }
            )
        }
    }
}

@Composable
private fun OutgoingConnectionSection(
    config: GatewayConfig,
    onConfigChange: (GatewayConfig) -> Unit
) {
    Panel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallMade,
                    contentDescription = "Outgoing Connection",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                SectionHeader(title = "OUTGOING CONNECTIONS")
            }

            Text(
                "Configure connections to external hosts or services",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Connection Type Selection
            ConnectionTypeSelector(
                selectedType = config.destinationConnectionType,
                onTypeSelected = { onConfigChange(config.copy(destinationConnectionType = it)) },
                availableTypes = listOf(
                    ConnectionType.TCP_IP to "TCP/IP",
                    ConnectionType.COM to "RS232",
                    ConnectionType.DIAL_UP to "Dial-up",
                    ConnectionType.REST to "REST API"
                )
            )

            // Connection Settings based on type
            when (config.destinationConnectionType) {
                ConnectionType.TCP_IP -> {
                    TcpIpSettingsCard(
                        title = "Destination Server",
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
                }
                ConnectionType.DIAL_UP -> {
                    UnderDevelopmentBanner()
                }
                ConnectionType.REST -> {
                    var restConfig by remember {
                        mutableStateOf(config.restConfiguration ?: RestConfiguration())
                    }
                    RestSettingsCard(
                        restConfig = restConfig,
                        onRestConfigChange = { newRestConfig ->
                            restConfig = newRestConfig
                            onConfigChange(config.copy(restConfiguration = newRestConfig))
                        }
                    )
                }
            }

            // Message Length Type for destination
            MessageLengthSelector(
                title = "Outgoing Message Length",
                selectedType = config.messageLengthTypeDest,
                onTypeSelected = { onConfigChange(config.copy(messageLengthTypeDest = it)) }
            )
        }
    }
}

@Composable
private fun AdditionalSettingsSection(
    config: GatewayConfig,
    onConfigChange: (GatewayConfig) -> Unit
) {
    Panel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Additional Settings",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                SectionHeader(title = "PERFORMANCE & ERROR HANDLING")
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Timeout Setting
                SettingRow(
                    label = "Connection Timeout",
                    description = "Maximum time to wait for responses"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = config.transactionTimeOut.toString(),
                            onValueChange = {
                                val timeout = it.toIntOrNull() ?: config.transactionTimeOut
                                onConfigChange(config.copy(transactionTimeOut = timeout))
                            },
                            modifier = Modifier.width(100.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Text(
                            "seconds",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Max Concurrent Connections
                SettingRow(
                    label = "Max Concurrent Connections",
                    description = "Maximum number of simultaneous connections"
                ) {
                    OutlinedTextField(
                        value = config.maxConcurrentConnection.toString(),
                        onValueChange = {
                            val connections = it.toIntOrNull() ?: config.maxConcurrentConnection
                            onConfigChange(config.copy(maxConcurrentConnection = connections))
                        },
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Error Handling Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = config.terminateWhenError,
                        onCheckedChange = { onConfigChange(config.copy(terminateWhenError = it)) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary)
                    )
                    Column {
                        Text(
                            "Terminate on Error",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Close connection when errors occur",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransmissionOption(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        elevation = if (selected) 4.dp else 1.dp,
        backgroundColor = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp),
        border = if (selected)
            ButtonDefaults.outlinedBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colors.primary))
        else
            ButtonDefaults.outlinedBorder
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ConnectionTypeSelector(
    selectedType: ConnectionType,
    onTypeSelected: (ConnectionType) -> Unit,
    availableTypes: List<Pair<ConnectionType, String>>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Connection Type",
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.subtitle2
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableTypes.size) { index ->
                val (type, label) = availableTypes[index]
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = label,
                    icon = when (type) {
                        ConnectionType.TCP_IP -> Icons.Default.Wifi
                        ConnectionType.COM -> Icons.Default.Cable
                        ConnectionType.DIAL_UP -> Icons.Default.Phone
                        ConnectionType.REST -> Icons.Default.Api
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        border = ButtonDefaults.outlinedBorder,
        elevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = if (selected) Color.White else MaterialTheme.colors.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = if (selected) Color.White else MaterialTheme.colors.onSurface
            )
        }
    }
}

@Composable
private fun TcpIpSettingsCard(
    title: String,
    address: String,
    port: String,
    onAddressChange: (String) -> Unit,
    onPortChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(2f)) {
                    Text(
                        "IP Address",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = onAddressChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Computer,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Port",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = onPortChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComSettingsCard(
    comPort: String,
    baudRate: String,
    onComPortChange: (String) -> Unit,
    onBaudRateChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Serial Communication (RS232)",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primary
                )
                UnderDevelopmentChip()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "COM Port",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                    DropdownSelector(
                        selectedValue = comPort.ifEmpty { "Select COM port" },
                        options = listOf("COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9"),
                        onSelectionChange = onComPortChange
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Baud Rate",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                    DropdownSelector(
                        selectedValue = baudRate.ifEmpty { "Select baud rate" },
                        options = listOf("115200", "9600", "14400", "19200", "28800", "38400", "57600"),
                        onSelectionChange = onBaudRateChange
                    )
                }
            }
        }
    }
}

@Composable
private fun DialUpSettingsCard(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Dial-up Connection",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primary
                )
                UnderDevelopmentChip()
            }

            Column {
                Text(
                    "Phone Number",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun MessageLengthSelector(
    title: String,
    selectedType: MessageLengthType,
    onTypeSelected: (MessageLengthType) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium
        )

        DropdownSelector(
            selectedValue = selectedType.name,
            options = MessageLengthType.values().map { it.name },
            onSelectionChange = { selected ->
                MessageLengthType.values().find { it.name == selected }?.let(onTypeSelected)
            }
        )
    }
}

@Composable
private fun DropdownSelector(
    selectedValue: String,
    options: List<String>,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            },
            singleLine = true
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onSelectionChange(option)
                        expanded = false
                    }
                ) {
                    Text(option)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium
            )
            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        content()
    }
}

// REST Settings Component (Simplified version of the original)
@Composable
private fun RestSettingsCard(
    restConfig: RestConfiguration,
    onRestConfigChange: (RestConfiguration) -> Unit
) {
    var expandedSections by remember { mutableStateOf(setOf("basic")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with validation status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "REST API Configuration",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (restConfig.isValid()) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (restConfig.isValid()) MaterialTheme.colors.primary else MaterialTheme.colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        if (restConfig.isValid()) "Valid" else "Invalid",
                        style = MaterialTheme.typography.caption,
                        color = if (restConfig.isValid()) MaterialTheme.colors.primary else MaterialTheme.colors.error
                    )
                }
            }

            // Basic Configuration
            BasicRestConfiguration(restConfig, onRestConfigChange)

            // Expandable sections
            ExpandableSection(
                title = "Advanced Settings",
                isExpanded = expandedSections.contains("advanced"),
                onToggle = {
                    expandedSections = if (expandedSections.contains("advanced")) {
                        expandedSections - "advanced"
                    } else {
                        expandedSections + "advanced"
                    }
                }
            ) {
                Text(
                    "Additional REST configuration options will be available here",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
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
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Endpoint URL *",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )
            OutlinedTextField(
                value = restConfig.url,
                onValueChange = { onRestConfigChange(restConfig.copy(url = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://api.example.com/v1/transactions") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                isError = restConfig.url.isNotBlank() && !restConfig.isValid(),
                singleLine = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // HTTP Method Selection
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "HTTP Method",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Medium
                )

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
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
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
                                }
                            ) {
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
                        }
                    }
                }
            }

            // Message Format Selection
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Message Format",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Medium
                )

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
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
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
                                }
                            ) {
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
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                    )
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
private fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium
            )
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp)
            ) {
                content()
            }
        }
    }
}
