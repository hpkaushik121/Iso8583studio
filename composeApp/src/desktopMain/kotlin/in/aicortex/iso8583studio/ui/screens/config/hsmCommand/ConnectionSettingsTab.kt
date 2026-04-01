package `in`.aicortex.iso8583studio.ui.screens.config.hsmCommand

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HeaderFormat
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmCommandConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmVendorType

@Composable
fun ConnectionSettingsTab(
    config: HsmCommandConfig,
    onConfigChange: (HsmCommandConfig) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Configuration Identity
        Text("Configuration", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp,
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FixedOutlinedTextField(
                    value = config.name,
                    onValueChange = { onConfigChange(config.copy(name = it)) },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Label, null, modifier = Modifier.size(18.dp)) }
                )
                FixedOutlinedTextField(
                    value = config.description,
                    onValueChange = { onConfigChange(config.copy(description = it)) },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Notes, null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // HSM Vendor Selection
        Text("HSM Vendor", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp,
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                var vendorExpanded by remember { mutableStateOf(false) }
                var vendorFieldWidth by remember { mutableStateOf(0.dp) }
                val vendorDensity = LocalDensity.current
                Box(modifier = Modifier.fillMaxWidth()) {
                    FixedOutlinedTextField(
                        value = config.hsmVendor.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("HSM Type") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { vendorFieldWidth = with(vendorDensity) { it.size.width.toDp() } },
                        trailingIcon = {
                            IconButton(onClick = { vendorExpanded = !vendorExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, "Select vendor")
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { vendorExpanded = !vendorExpanded }
                    )
                    DropdownMenu(
                        expanded = vendorExpanded,
                        onDismissRequest = { vendorExpanded = false },
                        modifier = Modifier
                            .then(if (vendorFieldWidth > 0.dp) Modifier.width(vendorFieldWidth) else Modifier.fillMaxWidth())
                            .heightIn(max = 300.dp)
                    ) {
                        HsmVendorType.entries.forEach { vendor ->
                            DropdownMenuItem(onClick = {
                                onConfigChange(config.copy(
                                    hsmVendor = vendor,
                                    port = vendor.defaultPort,
                                    tcpLengthHeaderEnabled = vendor.headerFormat == HeaderFormat.TWO_BYTE_LENGTH ||
                                            vendor.headerFormat == HeaderFormat.FOUR_BYTE_ASCII_LENGTH
                                ))
                                vendorExpanded = false
                            }) {
                                Column {
                                    Text(vendor.displayName, fontWeight = FontWeight.Medium)
                                    Text(vendor.description, style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Connection Settings
        Text("Connection", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp,
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FixedOutlinedTextField(
                        value = config.ipAddress,
                        onValueChange = { onConfigChange(config.copy(ipAddress = it)) },
                        label = { Text("IP Address") },
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Dns, null, modifier = Modifier.size(18.dp)) }
                    )
                    FixedOutlinedTextField(
                        value = config.port.toString(),
                        onValueChange = { it.toIntOrNull()?.let { p -> onConfigChange(config.copy(port = p)) } },
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                FixedOutlinedTextField(
                    value = config.timeout.toString(),
                    onValueChange = { it.toIntOrNull()?.let { t -> onConfigChange(config.copy(timeout = t)) } },
                    label = { Text("Timeout (seconds)") },
                    modifier = Modifier.width(200.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        // Message Framing
        Text("Message Framing", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp,
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = config.tcpLengthHeaderEnabled,
                        onCheckedChange = { onConfigChange(config.copy(tcpLengthHeaderEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("TCP Length Header Enabled")
                }

                var headerFormatExpanded by remember { mutableStateOf(false) }
                var headerFormatFieldWidth by remember { mutableStateOf(0.dp) }
                val headerFormatDensity = LocalDensity.current
                Box(modifier = Modifier.fillMaxWidth()) {
                    FixedOutlinedTextField(
                        value = config.hsmVendor.headerFormat.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Header Format") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { headerFormatFieldWidth = with(headerFormatDensity) { it.size.width.toDp() } },
                        trailingIcon = {
                            IconButton(onClick = { headerFormatExpanded = !headerFormatExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { headerFormatExpanded = !headerFormatExpanded }
                    )
                    DropdownMenu(
                        expanded = headerFormatExpanded,
                        onDismissRequest = { headerFormatExpanded = false },
                        modifier = Modifier
                            .then(if (headerFormatFieldWidth > 0.dp) Modifier.width(headerFormatFieldWidth) else Modifier.fillMaxWidth())
                            .heightIn(max = 300.dp)
                    ) {
                        HeaderFormat.entries.forEach { fmt ->
                            DropdownMenuItem(onClick = {
                                onConfigChange(config.copy(
                                    hsmVendor = config.hsmVendor,
                                ))
                                headerFormatExpanded = false
                            }) {
                                Text(fmt.displayName)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FixedOutlinedTextField(
                        value = config.headerValue,
                        onValueChange = { onConfigChange(config.copy(headerValue = it)) },
                        label = { Text("Message Header (Hex)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("e.g. 0000") }
                    )
                    FixedOutlinedTextField(
                        value = config.trailerValue,
                        onValueChange = { onConfigChange(config.copy(trailerValue = it)) },
                        label = { Text("Message Trailer (Hex)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Optional") }
                    )
                }

                FixedOutlinedTextField(
                    value = config.messageHeaderLength.toString(),
                    onValueChange = { it.toIntOrNull()?.let { l -> onConfigChange(config.copy(messageHeaderLength = l)) } },
                    label = { Text("Message Header Length") },
                    modifier = Modifier.width(200.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}
