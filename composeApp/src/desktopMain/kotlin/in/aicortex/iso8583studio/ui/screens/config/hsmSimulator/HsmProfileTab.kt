package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.AuthenticationPolicy
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMCapability
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMVendor
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.OperatingMode
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.TamperResistanceLevel



@Composable
fun HSMProfileTab(
    config: HSMSimulatorConfig,
    onProfileUpdated: (HSMSimulatorConfig) -> Unit
) {
    var currentConfig by remember { mutableStateOf(config) }

    LaunchedEffect(currentConfig) {
        onProfileUpdated(currentConfig)
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Basic Information Section
        ProfileSection(
            title = "Basic Information",
            icon = Icons.Default.Info
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // HSM Name
                OutlinedTextField(
                    value = config.name,
                    onValueChange = { currentConfig = currentConfig.copy(name = it) },
                    label = { Text("HSM Name") },
                    placeholder = { Text("Enter HSM configuration name") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.primary,
                        focusedLabelColor = MaterialTheme.colors.primary
                    )
                )

                // Description
                OutlinedTextField(
                    value = config.description,
                    onValueChange = { currentConfig = currentConfig.copy(description = it) },
                    label = { Text("Description") },
                    placeholder = { Text("Optional description for this HSM configuration") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Serial Number
                    OutlinedTextField(
                        value = currentConfig.deviceInfo.serialNumber,
                        onValueChange = { currentConfig = currentConfig.copy(deviceInfo = config.deviceInfo.copy(serialNumber = it)) },
                        label = { Text("Serial Number") },
                        placeholder = { Text("HSM Serial Number") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }
        }

        // Vendor and Model Selection
        ProfileSection(
            title = "Vendor & Model Configuration",
            icon = Icons.Default.Business
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Vendor Selection
                VendorSelectionCard(
                    selectedVendor = currentConfig.vendor,
                    onVendorSelected = { vendor ->
                        currentConfig = currentConfig.copy(
                            vendor = vendor,
                            model = vendor.models.firstOrNull() ?: ""
                        )
                    }
                )

                // Model Selection
                if (currentConfig.vendor.models.isNotEmpty()) {
                    ModelSelectionDropdown(
                        vendor = currentConfig.vendor,
                        selectedModel = currentConfig.model,
                        onModelSelected = { model ->
                            currentConfig = currentConfig.copy(model = model)
                        }
                    )
                }

                // Firmware Version
                OutlinedTextField(
                    value = currentConfig.deviceInfo.firmwareVersion,
                    onValueChange = { currentConfig = currentConfig.copy(deviceInfo = currentConfig.deviceInfo.copy(firmwareVersion = it)) },
                    label = { Text("Firmware Version") },
                    placeholder = { Text("e.g., 2.70.0, 7.4.0") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }

        // Operational Configuration
        ProfileSection(
            title = "Operational Configuration",
            icon = Icons.Default.Settings
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Operational Mode
                DropdownSelector(
                    label = "Operational Mode",
                    options = OperatingMode.values().toList(),
                    selectedOption = currentConfig.operatingMode,
                    onOptionSelected = { currentConfig = currentConfig.copy(operatingMode = it) },
                    displayName = { it.displayName },
                    icon = Icons.Default.PlayArrow
                )

                // Authentication Mode
                DropdownSelector(
                    label = "Authentication Mode",
                    options = AuthenticationPolicy.values().toList(),
                    selectedOption = currentConfig.authenticationPolicy,
                    onOptionSelected = { currentConfig = currentConfig.copy(authenticationPolicy = it) },
                    displayName = { it.displayName },
                    icon = Icons.Default.Security
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Max Sessions
                    OutlinedTextField(
                        value = currentConfig.maxSessions.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { sessions ->
                                if (sessions > 0) {
                                    currentConfig = currentConfig.copy(maxSessions = sessions)
                                }
                            }
                        },
                        label = { Text("Max Sessions") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Key Storage Slots
                    OutlinedTextField(
                        value = currentConfig.slotConfiguration.totalSlots.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { slots ->
                                if (slots > 0) {
                                    currentConfig = currentConfig.copy(slotConfiguration = currentConfig.slotConfiguration.copy(totalSlots = slots))
                                }
                            }
                        },
                        label = { Text("Key Storage Slots") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        // Security and Compliance
        ProfileSection(
            title = "Security & Compliance",
            icon = Icons.Default.Shield
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Tamper Resistance Level
                DropdownSelector(
                    label = "Tamper Resistance Level",
                    options = TamperResistanceLevel.values().toList(),
                    selectedOption = currentConfig.securitySettings.tamperResistance,
                    onOptionSelected = { currentConfig = currentConfig.copy(securitySettings = currentConfig.securitySettings.copy(tamperResistance = it as TamperResistanceLevel)) },
                    displayName = { it.displayName },
                    icon = Icons.Default.Shield
                )
            }
        }

        // HSM Capabilities
        ProfileSection(
            title = "HSM Capabilities",
            icon = Icons.Default.Speed
        ) {
            HSMCapabilitiesSelector(
                selectedCapabilities = currentConfig.deviceInfo.capabilities,
                onCapabilitiesChanged = {
                    currentConfig = currentConfig.copy(deviceInfo = currentConfig.deviceInfo.copy(capabilities = it))
                }
            )
        }
    }
}

@Composable
private fun ProfileSection(
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
private fun VendorSelectionCard(
    selectedVendor: HSMVendor,
    onVendorSelected: (HSMVendor) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Select HSM Vendor",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(min = 200.dp, max=600.dp)
        ) {
            val list = HSMVendor.values()
            items(list.size) { index ->
                VendorCard(
                    vendor = list[index],
                    isSelected = list[index] == selectedVendor,
                    onClick = { onVendorSelected(list[index]) }
                )
            }
        }
    }
}

@Composable
private fun VendorCard(
    vendor: HSMVendor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
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
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Business,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    vendor.displayName,
                    style = MaterialTheme.typography.caption,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ModelSelectionDropdown(
    vendor: HSMVendor,
    selectedModel: String,
    onModelSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedModel,
            onValueChange = { },
            label = { Text("${vendor.displayName} Model") },
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
                    imageVector = Icons.Default.Computer,
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
            vendor.models.forEach { model ->
                DropdownMenuItem(
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    }
                ) {
                    Text(model)
                }
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
    icon: ImageVector
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
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


@Composable
private fun SecurityOption(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colors.primary
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary,
                checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun HSMCapabilitiesSelector(
    selectedCapabilities: Set<HSMCapability>,
    onCapabilitiesChanged: (Set<HSMCapability>) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.heightIn(min = 200.dp, max=600.dp)
    ) {
        val list = HSMCapability.values()
        items(list.size) { index ->
            CapabilityChip(
                capability = list[index],
                isSelected = list[index] in selectedCapabilities,
                onClick = {
                    val newCapabilities = if (list[index] in selectedCapabilities) {
                        selectedCapabilities - list[index]
                    } else {
                        selectedCapabilities + list[index]
                    }
                    onCapabilitiesChanged(newCapabilities)
                }
            )
        }
    }
}

@Composable
private fun CapabilityChip(
    capability: HSMCapability,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        elevation = if (isSelected) 2.dp else 0.dp,
        backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.elevation(0.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    capability.displayName,
                    style = MaterialTheme.typography.caption,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}