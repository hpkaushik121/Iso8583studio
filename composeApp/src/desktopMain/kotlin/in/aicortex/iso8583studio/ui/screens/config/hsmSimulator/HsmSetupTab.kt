package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.ComplianceSettings
import `in`.aicortex.iso8583studio.ui.navigation.HSMDeviceInfo
import `in`.aicortex.iso8583studio.ui.navigation.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.InitializationSettings
import `in`.aicortex.iso8583studio.ui.navigation.NetworkSettings
import `in`.aicortex.iso8583studio.ui.navigation.SecuritySettings
import `in`.aicortex.iso8583studio.ui.navigation.SlotConfiguration
import `in`.aicortex.iso8583studio.ui.screens.components.StyledTextField
import kotlin.math.min

// Re-imagined HSM Setup Tab using an expandable accordion layout
@Composable
fun HSMSetupTab(config: HSMSimulatorConfig, onConfigUpdate: (HSMSimulatorConfig) -> Unit) {
    var expandedSections by remember { mutableStateOf(setOf("DEVICE INFORMATION")) }

    val toggleSection: (String) -> Unit = { sectionTitle ->
        expandedSections = if (expandedSections.contains(sectionTitle)) {
            expandedSections - sectionTitle
        } else {
            expandedSections + sectionTitle
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExpandableSection(
            title = "DEVICE INFORMATION",
            icon = Icons.Default.Info,
            isExpanded = "DEVICE INFORMATION" in expandedSections,
            onToggle = { toggleSection("DEVICE INFORMATION") }
        ) {
            DeviceInformationContent(
                deviceInfo = config.deviceInfo,
                onDeviceInfoUpdate = { onConfigUpdate(config.copy(deviceInfo = it)) }
            )
        }
        ExpandableSection(
            title = "SLOT CONFIGURATION",
            icon = Icons.Default.Storage,
            isExpanded = "SLOT CONFIGURATION" in expandedSections,
            onToggle = { toggleSection("SLOT CONFIGURATION") }
        ) {
            SlotConfigurationContent(
                slotConfig = config.slotConfiguration,
                onSlotConfigUpdate = { onConfigUpdate(config.copy(slotConfiguration = it)) }
            )
        }
        ExpandableSection(
            title = "SECURITY SETTINGS",
            icon = Icons.Default.Security,
            isExpanded = "SECURITY SETTINGS" in expandedSections,
            onToggle = { toggleSection("SECURITY SETTINGS") }
        ) {
            SecuritySettingsContent(
                securitySettings = config.securitySettings,
                onSecurityUpdate = { onConfigUpdate(config.copy(securitySettings = it)) }
            )
        }
        ExpandableSection(
            title = "INITIALIZATION SETTINGS",
            icon = Icons.Default.PlayArrow,
            isExpanded = "INITIALIZATION SETTINGS" in expandedSections,
            onToggle = { toggleSection("INITIALIZATION SETTINGS") }
        ) {
            InitializationSettingsContent(
                initSettings = config.initializationSettings,
                onInitUpdate = { onConfigUpdate(config.copy(initializationSettings = it)) }
            )
        }
        ExpandableSection(
            title = "COMPLIANCE SETTINGS",
            icon = Icons.Default.VerifiedUser,
            isExpanded = "COMPLIANCE SETTINGS" in expandedSections,
            onToggle = { toggleSection("COMPLIANCE SETTINGS") }
        ) {
            ComplianceSettingsContent(
                complianceSettings = config.complianceSettings,
                onComplianceUpdate = { onConfigUpdate(config.copy(complianceSettings = it)) }
            )
        }
        ExpandableSection(
            title = "NETWORK SETTINGS",
            icon = Icons.Default.NetworkCheck,
            isExpanded = "NETWORK SETTINGS" in expandedSections,
            onToggle = { toggleSection("NETWORK SETTINGS") }
        ) {
            NetworkSettingsContent(
                networkSettings = config.networkSettings,
                onNetworkUpdate = { onConfigUpdate(config.copy(networkSettings = it)) }
            )
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

            Row(
                modifier = Modifier
                    .clickable(onClick = onToggle)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut()
            ) {
                Column {
                    Divider()
                    Box(modifier = Modifier.padding(16.dp)) {
                        content()
                    }
                }
            }
        }
    }
}


@Composable
private fun DeviceInformationContent(deviceInfo: HSMDeviceInfo, onDeviceInfoUpdate: (HSMDeviceInfo) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Configure the HSM device identity and hardware information.",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        ConfigField(label = "Device Name", isRequired = true) {
            StyledTextField(
                value = deviceInfo.deviceName,
                onValueChange = { onDeviceInfoUpdate(deviceInfo.copy(deviceName = it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SlotConfigurationContent(slotConfig: SlotConfiguration, onSlotConfigUpdate: (SlotConfiguration) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Configure HSM slots and token presence simulation.",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ConfigField(label = "Total Slots", modifier = Modifier.weight(1f)) {
                StyledTextField(
                    value = slotConfig.totalSlots.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let {
                            if (it in 1..32) onSlotConfigUpdate(slotConfig.copy(totalSlots = it))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ConfigField(label = "Active Slots", modifier = Modifier.weight(1f)) {
                StyledTextField(
                    value = slotConfig.activeSlots.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let {
                            if (it in 0..slotConfig.totalSlots) onSlotConfigUpdate(slotConfig.copy(activeSlots = it))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Text("Slot Status Configuration", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Medium)
        (0 until min(slotConfig.totalSlots, 8)).chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowItems.forEach { slotIndex ->
                    SlotStatusCard(
                        slotIndex = slotIndex,
                        tokenPresent = slotConfig.slotTokenPresent[slotIndex] ?: false,
                        tokenWritable = slotConfig.slotTokenWritable[slotIndex] ?: false,
                        onTokenPresentChange = { present ->
                            val newMap = slotConfig.slotTokenPresent.toMutableMap()
                            newMap[slotIndex] = present
                            onSlotConfigUpdate(slotConfig.copy(slotTokenPresent = newMap))
                        },
                        onTokenWritableChange = { writable ->
                            val newMap = slotConfig.slotTokenWritable.toMutableMap()
                            newMap[slotIndex] = writable
                            onSlotConfigUpdate(slotConfig.copy(slotTokenWritable = newMap))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}


@Composable
private fun SecuritySettingsContent(securitySettings: SecuritySettings, onSecurityUpdate: (SecuritySettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Configure authentication, PIN policies, and security features.",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        FeatureOption(
            title = "Require Authentication",
            description = "Users must authenticate before accessing HSM functions.",
            enabled = securitySettings.authenticationRequired,
            onToggle = { onSecurityUpdate(securitySettings.copy(authenticationRequired = it)) }
        )
        if (securitySettings.authenticationRequired) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConfigField("SO PIN", Modifier.weight(1f)) {
                        StyledTextField(securitySettings.soPin, { onSecurityUpdate(securitySettings.copy(soPin = it)) }, Modifier.fillMaxWidth())
                    }
                    ConfigField("User PIN", Modifier.weight(1f)) {
                        StyledTextField(securitySettings.userPin, { onSecurityUpdate(securitySettings.copy(userPin = it)) }, Modifier.fillMaxWidth())
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConfigField("Min PIN Length", Modifier.weight(1f)) {
                        StyledTextField(securitySettings.minPinLength.toString(), { v -> v.toIntOrNull()?.let { onSecurityUpdate(securitySettings.copy(minPinLength = it)) } }, Modifier.fillMaxWidth())
                    }
                    ConfigField("Max PIN Length", Modifier.weight(1f)) {
                        StyledTextField(securitySettings.maxPinLength.toString(), { v -> v.toIntOrNull()?.let { onSecurityUpdate(securitySettings.copy(maxPinLength = it)) } }, Modifier.fillMaxWidth())
                    }
                    ConfigField("Retry Limit", Modifier.weight(1f)) {
                        StyledTextField(securitySettings.pinRetryLimit.toString(), { v -> v.toIntOrNull()?.let { onSecurityUpdate(securitySettings.copy(pinRetryLimit = it)) } }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        FeatureOption("Tamper Resistance", "Simulate tamper detection and response mechanisms.", securitySettings.tamperResistance) {
            onSecurityUpdate(securitySettings.copy(tamperResistance = it))
        }
        FeatureOption("Secure Messaging", "Enable encrypted communication channels.", securitySettings.secureMessaging) {
            onSecurityUpdate(securitySettings.copy(secureMessaging = it))
        }
    }
}


@Composable
private fun InitializationSettingsContent(initSettings: InitializationSettings, onInitUpdate: (InitializationSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Configure automatic initialization and startup behavior.",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        FeatureOption("Auto Initialize", "Automatically initialize HSM on startup.", initSettings.autoInitialize) {
            onInitUpdate(initSettings.copy(autoInitialize = it))
        }
        FeatureOption("Initialize Tokens", "Automatically initialize tokens in active slots.", initSettings.initializeTokens) {
            onInitUpdate(initSettings.copy(initializeTokens = it))
        }
        FeatureOption("Generate Default Keys", "Create default cryptographic keys for testing.", initSettings.generateDefaultKeys) {
            onInitUpdate(initSettings.copy(generateDefaultKeys = it))
        }
        FeatureOption("Load Test Certificates", "Load sample certificates for development.", initSettings.loadTestCertificates) {
            onInitUpdate(initSettings.copy(loadTestCertificates = it))
        }
        FeatureOption("Enable Debug Mode", "Enable detailed debugging and verbose logging.", initSettings.enableDebugMode) {
            onInitUpdate(initSettings.copy(enableDebugMode = it))
        }
    }
}


@Composable
private fun ComplianceSettingsContent(complianceSettings: ComplianceSettings, onComplianceUpdate: (ComplianceSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Configure standards compliance and certification levels.",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        ConfigField("PKCS#11 Version") {
            PKCS11VersionSelector(complianceSettings.pkcs11Version) {
                onComplianceUpdate(complianceSettings.copy(pkcs11Version = it))
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        FeatureOption("Enable FIPS Mode", "Enforce FIPS 140-2 compliance.", complianceSettings.fipsMode) {
            onComplianceUpdate(complianceSettings.copy(fipsMode = it))
        }
        if (complianceSettings.fipsMode) {
            ConfigField("FIPS Level") {
                FIPSLevelSelector(complianceSettings.fipsLevel) {
                    onComplianceUpdate(complianceSettings.copy(fipsLevel = it))
                }
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        FeatureOption("Enable Common Criteria", "Enforce Common Criteria compliance.", complianceSettings.commonCriteria) {
            onComplianceUpdate(complianceSettings.copy(commonCriteria = it))
        }
        if (complianceSettings.commonCriteria) {
            ConfigField("Evaluation Assurance Level") {
                StyledTextField(complianceSettings.ccLevel, { onComplianceUpdate(complianceSettings.copy(ccLevel = it)) }, Modifier.fillMaxWidth())
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        FeatureOption("Certificate Validation", "Validate certificate chains and CRL checking.", complianceSettings.validateCertificates) {
            onComplianceUpdate(complianceSettings.copy(validateCertificates = it))
        }
    }
}

@Composable
private fun NetworkSettingsContent(networkSettings: NetworkSettings, onNetworkUpdate: (NetworkSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Configure network access, SSL/TLS, and connection limits.",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        FeatureOption("Enable Network Access", "Allow remote connections to the HSM.", networkSettings.enableNetworkAccess) {
            onNetworkUpdate(networkSettings.copy(enableNetworkAccess = it))
        }
        if (networkSettings.enableNetworkAccess) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ConfigField("Bind Address", Modifier.weight(2f)) {
                    StyledTextField(networkSettings.bindAddress, { onNetworkUpdate(networkSettings.copy(bindAddress = it)) }, Modifier.fillMaxWidth())
                }
                ConfigField("Port", Modifier.weight(1f)) {
                    StyledTextField(networkSettings.port.toString(), { v -> v.toIntOrNull()?.let { onNetworkUpdate(networkSettings.copy(port = it)) } }, Modifier.fillMaxWidth())
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ConfigField("Max Connections", Modifier.weight(1f)) {
                    StyledTextField(networkSettings.maxConnections.toString(), { v -> v.toIntOrNull()?.let { onNetworkUpdate(networkSettings.copy(maxConnections = it)) } }, Modifier.fillMaxWidth())
                }
                ConfigField("Timeout (s)", Modifier.weight(1f)) {
                    StyledTextField(networkSettings.connectionTimeout.toString(), { v -> v.toIntOrNull()?.let { onNetworkUpdate(networkSettings.copy(connectionTimeout = it)) } }, Modifier.fillMaxWidth())
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            FeatureOption("Enable SSL/TLS Encryption", "Secure network connections.", networkSettings.enableSSL) {
                onNetworkUpdate(networkSettings.copy(enableSSL = it))
            }
            if (networkSettings.enableSSL) {
                ConfigField("SSL Certificate Path") {
                    StyledTextField(networkSettings.sslCertPath, { onNetworkUpdate(networkSettings.copy(sslCertPath = it)) }, Modifier.fillMaxWidth())
                }
                ConfigField("SSL Private Key Path") {
                    StyledTextField(networkSettings.sslKeyPath, { onNetworkUpdate(networkSettings.copy(sslKeyPath = it)) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}


// --- Helper and Reusable Components ---

@Composable
private fun FeatureOption(title: String, description: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
        )
    }
}

@Composable
private fun ConfigField(label: String, modifier: Modifier = Modifier, isRequired: Boolean = false, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
            if (isRequired) Text("*", color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
        }
        content()
    }
}

@Composable
private fun SlotStatusCard(
    slotIndex: Int, tokenPresent: Boolean, tokenWritable: Boolean,
    onTokenPresentChange: (Boolean) -> Unit, onTokenWritableChange: (Boolean) -> Unit, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = if (tokenPresent) MaterialTheme.colors.primary.copy(alpha = 0.1f) else MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Slot $slotIndex", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(tokenPresent, onTokenPresentChange, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary))
                Text("Token Present", style = MaterialTheme.typography.caption)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(tokenWritable, onTokenWritableChange, enabled = tokenPresent, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary))
                Text("Writable", style = MaterialTheme.typography.caption, color = if (tokenPresent) LocalContentColor.current else LocalContentColor.current.copy(alpha = ContentAlpha.disabled))
            }
        }
    }
}

@Composable
private fun PKCS11VersionSelector(selectedVersion: String, onVersionSelected: (String) -> Unit) {
    val versions = listOf("2.20", "2.30", "2.40", "3.0")
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("v$selectedVersion")
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Version")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            versions.forEach { version ->
                DropdownMenuItem(onClick = {
                    onVersionSelected(version)
                    expanded = false
                }) {
                    Text("v$version")
                    if (version == selectedVersion) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colors.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FIPSLevelSelector(selectedLevel: Int, onLevelSelected: (Int) -> Unit) {
    val levels = mapOf(1 to "Level 1", 2 to "Level 2", 3 to "Level 3", 4 to "Level 4")
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(levels[selectedLevel] ?: "Select Level")
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Level")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            levels.forEach { (level, description) ->
                DropdownMenuItem(onClick = {
                    onLevelSelected(level)
                    expanded = false
                }) {
                    Text(description)
                    if (level == selectedLevel) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colors.primary)
                    }
                }
            }
        }
    }
}
