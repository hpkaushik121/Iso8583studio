package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.CertificateType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.CompressionType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.ConnectionType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.MessageFraming
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.NetworkConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.PerformanceSettings
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.ProtocolSettings
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.ProtocolVersion
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.RestApiConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.SSLTLSConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.SSLTLSVersion
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.SerialConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.WebSocketConfig


@Composable
fun NetworkConfigTab(
    networkConfig: NetworkConfig,
    onConfigUpdated: (NetworkConfig) -> Unit
) {
    var currentConfig by remember { mutableStateOf(networkConfig) }
    val scrollState = rememberScrollState()

    LaunchedEffect(currentConfig) {
        onConfigUpdated(currentConfig)
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Connection Type Selection
        NetworkSection(
            title = "Connection Configuration",
            icon = Icons.Default.Settings
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Connection Type Selection
                ConnectionTypeSelector(
                    selectedType = currentConfig.connectionType,
                    onTypeSelected = { type ->
                        currentConfig = currentConfig.copy(connectionType = type)
                    }
                )

                // Connection-specific configuration
                when (currentConfig.connectionType) {
                    ConnectionType.TCP_IP -> {
                        TCPIPConfiguration(
                            config = currentConfig,
                            onConfigChanged = { currentConfig = it }
                        )
                    }
                    ConnectionType.SERIAL -> {
                        SerialConfiguration(
                            config = currentConfig.serialConfig,
                            onConfigChanged = {
                                currentConfig = currentConfig.copy(serialConfig = it)
                            }
                        )
                    }
                    ConnectionType.REST_API -> {
                        RestApiConfiguration(
                            config = currentConfig.restApiConfig,
                            onConfigChanged = {
                                currentConfig = currentConfig.copy(restApiConfig = it)
                            }
                        )
                    }
                    ConnectionType.WEBSOCKET -> {
                        WebSocketConfiguration(
                            config = currentConfig.webSocketConfig,
                            onConfigChanged = {
                                currentConfig = currentConfig.copy(webSocketConfig = it)
                            }
                        )
                    }
                }
            }
        }

        // SSL/TLS Configuration
        if (currentConfig.connectionType in listOf(ConnectionType.TCP_IP, ConnectionType.REST_API, ConnectionType.WEBSOCKET)) {
            NetworkSection(
                title = "SSL/TLS Configuration",
                icon = Icons.Default.Security
            ) {
                SSLTLSConfiguration(
                    config = currentConfig.sslTlsConfig,
                    onConfigChanged = {
                        currentConfig = currentConfig.copy(sslTlsConfig = it)
                    }
                )
            }
        }

        // Performance Settings
        NetworkSection(
            title = "Performance Settings",
            icon = Icons.Default.Speed
        ) {
            PerformanceConfiguration(
                config = currentConfig.performanceSettings,
                onConfigChanged = {
                    currentConfig = currentConfig.copy(performanceSettings = it)
                }
            )
        }

        // Protocol Settings
        NetworkSection(
            title = "Protocol Settings",
            icon = Icons.Default.Code
        ) {
            ProtocolConfiguration(
                config = currentConfig.protocolSettings,
                onConfigChanged = {
                    currentConfig = currentConfig.copy(protocolSettings = it)
                }
            )
        }
    }
}

@Composable
private fun NetworkSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun ConnectionTypeSelector(
    selectedType: ConnectionType,
    onTypeSelected: (ConnectionType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Select Connection Type",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(160.dp)
        ) {
            items(ConnectionType.values()) { type ->
                ConnectionTypeCard(
                    type = type,
                    isSelected = type == selectedType,
                    onClick = { onTypeSelected(type) }
                )
            }
        }
    }
}

@Composable
private fun ConnectionTypeCard(
    type: ConnectionType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        elevation = if (isSelected) 4.dp else 1.dp,
        backgroundColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.elevation(0.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = if (isSelected) Color.White else MaterialTheme.colors.onSurface
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    type.name,
                    style = MaterialTheme.typography.caption,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TCPIPConfiguration(
    config: NetworkConfig,
    onConfigChanged: (NetworkConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = config.ipAddress,
                onValueChange = { onConfigChanged(config.copy(ipAddress = it)) },
                label = { Text("IP Address") },
                placeholder = { Text("127.0.0.1") },
                modifier = Modifier.weight(2f),
                leadingIcon = {
                    Icon(Icons.Default.Computer, null, modifier = Modifier.size(20.dp))
                }
            )

            OutlinedTextField(
                value = config.port.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { port ->
                        if (port in 1..65535) {
                            onConfigChanged(config.copy(port = port))
                        }
                    }
                },
                label = { Text("Port") },
                placeholder = { Text("8080") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Router, null, modifier = Modifier.size(20.dp))
                }
            )
        }

        OutlinedTextField(
            value = config.bindAddress,
            onValueChange = { onConfigChanged(config.copy(bindAddress = it)) },
            label = { Text("Bind Address") },
            placeholder = { Text("0.0.0.0 (all interfaces)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.NetworkWifi, null, modifier = Modifier.size(20.dp))
            }
        )
    }
}

@Composable
private fun SerialConfiguration(
    config: SerialConfig,
    onConfigChanged: (SerialConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = config.portName,
                onValueChange = { onConfigChanged(config.copy(portName = it)) },
                label = { Text("Port Name") },
                placeholder = { Text("COM1, /dev/ttyUSB0") },
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(Icons.Default.Cable, null, modifier = Modifier.size(20.dp))
                }
            )

            DropdownSelector(
                label = "Baud Rate",
                options = listOf(9600, 19200, 38400, 57600, 115200, 230400),
                selectedOption = config.baudRate,
                onOptionSelected = { onConfigChanged(config.copy(baudRate = it)) },
                displayName = { it.toString() },
                icon = Icons.Default.Speed,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DropdownSelector(
                label = "Data Bits",
                options = listOf(7, 8),
                selectedOption = config.dataBits,
                onOptionSelected = { onConfigChanged(config.copy(dataBits = it)) },
                displayName = { it.toString() },
                icon = Icons.Default.DataArray,
                modifier = Modifier.weight(1f)
            )

            DropdownSelector(
                label = "Stop Bits",
                options = listOf(1, 2),
                selectedOption = config.stopBits,
                onOptionSelected = { onConfigChanged(config.copy(stopBits = it)) },
                displayName = { it.toString() },
                icon = Icons.Default.Stop,
                modifier = Modifier.weight(1f)
            )

            DropdownSelector(
                label = "Parity",
                options = listOf("NONE", "EVEN", "ODD", "MARK", "SPACE"),
                selectedOption = config.parity,
                onOptionSelected = { onConfigChanged(config.copy(parity = it)) },
                displayName = { it },
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RestApiConfiguration(
    config: RestApiConfig,
    onConfigChanged: (RestApiConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = config.baseUrl,
            onValueChange = { onConfigChanged(config.copy(baseUrl = it)) },
            label = { Text("Base URL") },
            placeholder = { Text("https://api.example.com") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Http, null, modifier = Modifier.size(20.dp))
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = config.apiVersion,
                onValueChange = { onConfigChanged(config.copy(apiVersion = it)) },
                label = { Text("API Version") },
                placeholder = { Text("v1") },
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(Icons.Default.Tag, null, modifier = Modifier.size(20.dp))
                }
            )

            DropdownSelector(
                label = "Auth Type",
                options = listOf("Bearer", "Basic", "API Key", "OAuth2"),
                selectedOption = config.authType,
                onOptionSelected = { onConfigChanged(config.copy(authType = it)) },
                displayName = { it },
                icon = Icons.Default.Security,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = config.apiKey,
            onValueChange = { onConfigChanged(config.copy(apiKey = it)) },
            label = { Text("API Key / Token") },
            placeholder = { Text("Enter authentication key") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(20.dp))
            }
        )
    }
}

@Composable
private fun WebSocketConfiguration(
    config: WebSocketConfig,
    onConfigChanged: (WebSocketConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = config.maxFrameSize.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { size ->
                        if (size > 0) {
                            onConfigChanged(config.copy(maxFrameSize = size))
                        }
                    }
                },
                label = { Text("Max Frame Size") },
                placeholder = { Text("65536") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Memory, null, modifier = Modifier.size(20.dp))
                }
            )

            OutlinedTextField(
                value = config.pingIntervalMs.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { interval ->
                        if (interval > 0) {
                            onConfigChanged(config.copy(pingIntervalMs = interval))
                        }
                    }
                },
                label = { Text("Ping Interval (ms)") },
                placeholder = { Text("30000") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                }
            )
        }
    }
}

@Composable
private fun SSLTLSConfiguration(
    config: SSLTLSConfig,
    onConfigChanged: (SSLTLSConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // SSL/TLS Enable Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Enable SSL/TLS",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }
            Switch(
                checked = config.enabled,
                onCheckedChange = { onConfigChanged(config.copy(enabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colors.primary
                )
            )
        }

        if (config.enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DropdownSelector(
                        label = "SSL/TLS Version",
                        options = SSLTLSVersion.values().toList(),
                        selectedOption = config.version,
                        onOptionSelected = { onConfigChanged(config.copy(version = it)) },
                        displayName = { it.displayName },
                        icon = Icons.Default.Security,
                        modifier = Modifier.weight(1f)
                    )

                    DropdownSelector(
                        label = "Certificate Type",
                        options = CertificateType.values().toList(),
                        selectedOption = config.certificateType,
                        onOptionSelected = { onConfigChanged(config.copy(certificateType = it)) },
                        displayName = { it.displayName },
                        icon = Icons.Default.Notes,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = config.certificatePath,
                    onValueChange = { onConfigChanged(config.copy(certificatePath = it)) },
                    label = { Text("Certificate Path") },
                    placeholder = { Text("/path/to/certificate.crt") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.FileCopy, null, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { /* File picker */ }) {
                            Icon(Icons.Default.FolderOpen, null)
                        }
                    }
                )

                OutlinedTextField(
                    value = config.privateKeyPath,
                    onValueChange = { onConfigChanged(config.copy(privateKeyPath = it)) },
                    label = { Text("Private Key Path") },
                    placeholder = { Text("/path/to/private.key") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { /* File picker */ }) {
                            Icon(Icons.Default.FolderOpen, null)
                        }
                    }
                )

                // Client Authentication
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Require Client Authentication",
                        style = MaterialTheme.typography.body2
                    )
                    Switch(
                        checked = config.clientAuthRequired,
                        onCheckedChange = { onConfigChanged(config.copy(clientAuthRequired = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceConfiguration(
    config: PerformanceSettings,
    onConfigChanged: (PerformanceSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = config.maxOperationsPerSecond.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { connections ->
                        if (connections > 0) {
                            onConfigChanged(config.copy(maxOperationsPerSecond = connections))
                        }
                    }
                },
                label = { Text("Max Connections") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Group, null, modifier = Modifier.size(20.dp))
                }
            )

            OutlinedTextField(
                value = config.operationTimeout.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { timeout ->
                        if (timeout > 0) {
                            onConfigChanged(config.copy(operationTimeout = timeout.toLong()))
                        }
                    }
                },
                label = { Text("Connection Timeout (ms)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                }
            )
        }


    }
}

@Composable
private fun ProtocolConfiguration(
    config: ProtocolSettings,
    onConfigChanged: (ProtocolSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DropdownSelector(
                label = "Message Framing",
                options = MessageFraming.values().toList(),
                selectedOption = config.messageFraming,
                onOptionSelected = { onConfigChanged(config.copy(messageFraming = it)) },
                displayName = { it.displayName },
                icon = Icons.Default.Article,
                modifier = Modifier.weight(1f)
            )

            DropdownSelector(
                label = "Protocol Version",
                options = ProtocolVersion.values().toList(),
                selectedOption = config.protocolVersion,
                onOptionSelected = { onConfigChanged(config.copy(protocolVersion = it)) },
                displayName = { it.displayName },
                icon = Icons.Default.Code,
                modifier = Modifier.weight(1f)
            )
        }

        // Message Framing Specific Settings
        when (config.messageFraming) {
            MessageFraming.LENGTH_PREFIX -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = config.lengthFieldSize.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { size ->
                                if (size in 1..8) {
                                    onConfigChanged(config.copy(lengthFieldSize = size))
                                }
                            }
                        },
                        label = { Text("Length Field Size (bytes)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Straighten, null, modifier = Modifier.size(20.dp))
                        }
                    )

                    OutlinedTextField(
                        value = config.lengthFieldOffset.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { offset ->
                                if (offset >= 0) {
                                    onConfigChanged(config.copy(lengthFieldOffset = offset))
                                }
                            }
                        },
                        label = { Text("Length Field Offset") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }
            }

            MessageFraming.DELIMITER_BASED -> {
                OutlinedTextField(
                    value = config.messageDelimiter,
                    onValueChange = { onConfigChanged(config.copy(messageDelimiter = it)) },
                    label = { Text("Message Delimiter") },
                    placeholder = { Text("\\n, \\r\\n, |, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.LinearScale, null, modifier = Modifier.size(20.dp))
                    }
                )
            }

            MessageFraming.FIXED_LENGTH -> {
                OutlinedTextField(
                    value = config.fixedMessageLength.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { length ->
                            if (length > 0) {
                                onConfigChanged(config.copy(fixedMessageLength = length))
                            }
                        }
                    },
                    label = { Text("Fixed Message Length") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Straighten, null, modifier = Modifier.size(20.dp))
                    }
                )
            }

            else -> {
                // No additional settings for HTTP_CHUNKED and WEBSOCKET_FRAMES
                Text(
                    "No additional configuration required for ${config.messageFraming.displayName}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Compression Settings
        CompressionSettings(
            compressionType = config.compressionType,
            compressionLevel = config.compressionLevel,
            onCompressionTypeChanged = {
                onConfigChanged(config.copy(compressionType = it))
            },
            onCompressionLevelChanged = {
                onConfigChanged(config.copy(compressionLevel = it))
            }
        )

        // Custom Headers
        CustomHeadersSection(
            headers = config.customHeaders,
            onHeadersChanged = {
                onConfigChanged(config.copy(customHeaders = it))
            }
        )
    }
}

@Composable
private fun CompressionSettings(
    compressionType: CompressionType,
    compressionLevel: Int,
    onCompressionTypeChanged: (CompressionType) -> Unit,
    onCompressionLevelChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Compress,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Compression Settings",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }

            DropdownSelector(
                label = "Compression Type",
                options = CompressionType.values().toList(),
                selectedOption = compressionType,
                onOptionSelected = onCompressionTypeChanged,
                displayName = { it.displayName },
                icon = Icons.Default.Compress
            )

            if (compressionType != CompressionType.NONE) {
                OutlinedTextField(
                    value = compressionLevel.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { level ->
                            if (level in 1..9) {
                                onCompressionLevelChanged(level)
                            }
                        }
                    },
                    label = { Text("Compression Level (1-9)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Tune, null, modifier = Modifier.size(20.dp))
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomHeadersSection(
    headers: Map<String, String>,
    onHeadersChanged: (Map<String, String>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newHeaderKey by remember { mutableStateOf("") }
    var newHeaderValue by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Text(
                        "Custom Headers",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colors.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Header",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.caption)
                }
            }

            if (headers.isEmpty()) {
                Text(
                    "No custom headers configured",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    headers.forEach { (key, value) ->
                        HeaderItem(
                            key = key,
                            value = value,
                            onDelete = {
                                onHeadersChanged(headers - key)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Header Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newHeaderKey = ""
                newHeaderValue = ""
            },
            title = { Text("Add Custom Header") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newHeaderKey,
                        onValueChange = { newHeaderKey = it },
                        label = { Text("Header Name") },
                        placeholder = { Text("Content-Type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newHeaderValue,
                        onValueChange = { newHeaderValue = it },
                        label = { Text("Header Value") },
                        placeholder = { Text("application/json") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newHeaderKey.isNotBlank() && newHeaderValue.isNotBlank()) {
                            onHeadersChanged(headers + (newHeaderKey to newHeaderValue))
                            showAddDialog = false
                            newHeaderKey = ""
                            newHeaderValue = ""
                        }
                    },
                    enabled = newHeaderKey.isNotBlank() && newHeaderValue.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newHeaderKey = ""
                    newHeaderValue = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeaderItem(
    key: String,
    value: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    key,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    value,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Header",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}

@Composable
private fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    displayName: (T) -> String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayName(selectedOption),
            onValueChange = { },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                ) {
                    Text(displayName(option))
                }
            }
        }
    }
}