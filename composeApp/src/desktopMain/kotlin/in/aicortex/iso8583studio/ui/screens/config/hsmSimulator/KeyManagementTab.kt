package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.AlgorithmType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.CryptographicAlgorithm
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.CryptographicConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.KeyArchivalPolicy
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.KeyBackupSettings
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.KeyDestructionPolicy
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.KeyGenerationPolicy
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.KeyHierarchyLevel
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.KeyLifecycleConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.KeyManagementConfiguration
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.KeyRotationSchedule
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.KeyStorageType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.KeyStoreConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.MasterKeyConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.RandomNumberGenerator


@Composable
fun KeyManagementTab(
    keyManagementConfig: KeyManagementConfiguration,
    onConfigUpdated: (KeyManagementConfiguration) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentConfig by remember { mutableStateOf(keyManagementConfig) }

    LaunchedEffect(currentConfig) {
        onConfigUpdated(currentConfig)
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Key Store Configuration
        KeyManagementSection(
            title = "Key Store Configuration",
            icon = Icons.Default.Storage
        ) {
            KeyStoreConfigSection(
                config = currentConfig.keyStoreConfig,
                onConfigChanged = {
                    currentConfig = currentConfig.copy(keyStoreConfig = it)
                }
            )
        }

        // Cryptographic Algorithms
        KeyManagementSection(
            title = "Cryptographic Algorithms",
            icon = Icons.Default.Security
        ) {
            CryptographicConfigSection(
                config = currentConfig.cryptographicConfig,
                onConfigChanged = {
                    currentConfig = currentConfig.copy(cryptographicConfig = it)
                }
            )
        }

        // Key Lifecycle Management
        KeyManagementSection(
            title = "Key Lifecycle Management",
            icon = Icons.Default.Autorenew
        ) {
            KeyLifecycleSection(
                config = currentConfig.keyLifecycleConfig,
                onConfigChanged = {
                    currentConfig = currentConfig.copy(keyLifecycleConfig = it)
                }
            )
        }
    }
}

@Composable
private fun KeyManagementSection(
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
private fun KeyStoreConfigSection(
    config: KeyStoreConfig,
    onConfigChanged: (KeyStoreConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Key Storage Type Selection
        DropdownSelector(
            label = "Key Storage Type",
            options = KeyStorageType.values().toList(),
            selectedOption = config.storageType,
            onOptionSelected = { onConfigChanged(config.copy(storageType = it)) },
            displayName = { it.displayName },
            icon = Icons.Default.Storage
        )

        // Storage Type Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 0.dp,
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    config.storageType.description,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Key Store Capacity Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = config.maxKeys.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { max ->
                        if (max > 0) {
                            onConfigChanged(config.copy(maxKeys = max))
                        }
                    }
                },
                label = { Text("Maximum Keys") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Numbers, null, modifier = Modifier.size(20.dp))
                }
            )

            OutlinedTextField(
                value = config.keySlots.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { slots ->
                        if (slots > 0) {
                            onConfigChanged(config.copy(keySlots = slots))
                        }
                    }
                },
                label = { Text("Key Slots") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Inventory, null, modifier = Modifier.size(20.dp))
                }
            )
        }

        // Key Hierarchy Setup
        KeyHierarchySelector(
            selectedLevels = config.keyHierarchy,
            onLevelsChanged = { onConfigChanged(config.copy(keyHierarchy = it)) }
        )

        // Master Key Configuration
        MasterKeyConfigSection(
            masterKeyConfig = config.masterKeyConfig,
            onConfigChanged = { onConfigChanged(config.copy(masterKeyConfig = it)) }
        )

        // Key Store Security Settings
        KeyStoreSecuritySettings(
            config = config,
            onConfigChanged = onConfigChanged
        )

        // Key Backup Settings
        KeyBackupSettingsSection(
            backupSettings = config.backupSettings,
            onConfigChanged = { onConfigChanged(config.copy(backupSettings = it)) }
        )

        // Key Store Path Configuration
        if (config.storageType in listOf(KeyStorageType.FILE_BASED, KeyStorageType.DATABASE)) {
            KeyStorePathConfiguration(
                config = config,
                onConfigChanged = onConfigChanged
            )
        }
    }
}

@Composable
private fun KeyHierarchySelector(
    selectedLevels: Map<KeyHierarchyLevel, Boolean>,
    onLevelsChanged: (Map<KeyHierarchyLevel, Boolean>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Key Hierarchy Setup",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                "Select the key hierarchy levels to implement",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyHierarchyLevel.values().forEach { level ->
                    KeyHierarchyOption(
                        level = level,
                        enabled = selectedLevels[level] ?: false,
                        onEnabledChanged = { enabled ->
                            onLevelsChanged(selectedLevels + (level to enabled))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyHierarchyOption(
    level: KeyHierarchyLevel,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit
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
            // Visual hierarchy indicator
            repeat(level.level) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(8.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            Icon(
                imageVector = when (level) {
                    KeyHierarchyLevel.ROOT -> Icons.Default.Security
                    KeyHierarchyLevel.MASTER -> Icons.Default.VpnKey
                    KeyHierarchyLevel.KEY_ENCRYPTION -> Icons.Default.Key
                    KeyHierarchyLevel.DATA_ENCRYPTION -> Icons.Default.Lock
                    KeyHierarchyLevel.SESSION -> Icons.Default.Schedule
                    KeyHierarchyLevel.TRANSACTION -> Icons.Default.Payment
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colors.primary
            )

            Column {
                Text(
                    level.displayName,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Level ${level.level}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Checkbox(
            checked = enabled,
            onCheckedChange = onEnabledChanged,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colors.primary
            )
        )
    }
}

@Composable
private fun MasterKeyConfigSection(
    masterKeyConfig: MasterKeyConfig,
    onConfigChanged: (MasterKeyConfig) -> Unit
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
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Master Key Configuration",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DropdownSelector(
                    label = "Algorithm",
                    options = listOf(
                        CryptographicAlgorithm.AES,
                        CryptographicAlgorithm.TRIPLE_DES,
                        CryptographicAlgorithm.RSA
                    ),
                    selectedOption = masterKeyConfig.algorithm,
                    onOptionSelected = {
                        onConfigChanged(
                            masterKeyConfig.copy(
                                algorithm = it,
                                keySize = it.keySizes.first()
                            )
                        )
                    },
                    displayName = { it.displayName },
                    icon = Icons.Default.Security,
                    modifier = Modifier.weight(1f)
                )

                DropdownSelector(
                    label = "Key Size",
                    options = masterKeyConfig.algorithm.keySizes,
                    selectedOption = masterKeyConfig.keySize,
                    onOptionSelected = { onConfigChanged(masterKeyConfig.copy(keySize = it)) },
                    displayName = { "$it bits" },
                    icon = Icons.Default.Straighten,
                    modifier = Modifier.weight(1f)
                )
            }

            // Split Knowledge Configuration
            SplitKnowledgeSection(
                config = masterKeyConfig,
                onConfigChanged = onConfigChanged
            )

            // Master Key Rotation
            MasterKeyRotationSection(
                config = masterKeyConfig,
                onConfigChanged = onConfigChanged
            )

            // Master Key Security Options
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyManagementOption(
                    title = "Dual Control",
                    description = "Require two operators for master key operations",
                    checked = masterKeyConfig.enableDualControl,
                    onCheckedChange = { onConfigChanged(masterKeyConfig.copy(enableDualControl = it)) },
                    icon = Icons.Default.Group
                )

                KeyManagementOption(
                    title = "Auto-Generate",
                    description = "Automatically generate master key on initialization",
                    checked = masterKeyConfig.autoGenerate,
                    onCheckedChange = { onConfigChanged(masterKeyConfig.copy(autoGenerate = it)) },
                    icon = Icons.Default.AutoMode
                )
            }
        }
    }
}

@Composable
private fun SplitKnowledgeSection(
    config: MasterKeyConfig,
    onConfigChanged: (MasterKeyConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Column {
                    Text(
                        "Split Knowledge (Shamir's Secret)",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Distribute master key across multiple shares",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Switch(
                checked = config.enableSplitKnowledge,
                onCheckedChange = { onConfigChanged(config.copy(enableSplitKnowledge = it)) }
            )
        }

        if (config.enableSplitKnowledge) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = config.requiredShares.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { shares ->
                            if (shares > 0 && shares <= config.totalShares) {
                                onConfigChanged(config.copy(requiredShares = shares))
                            }
                        }
                    },
                    label = { Text("Required Shares") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Key, null, modifier = Modifier.size(20.dp))
                    }
                )

                OutlinedTextField(
                    value = config.totalShares.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { shares ->
                            if (shares >= config.requiredShares) {
                                onConfigChanged(config.copy(totalShares = shares))
                            }
                        }
                    },
                    label = { Text("Total Shares") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Group, null, modifier = Modifier.size(20.dp))
                    }
                )
            }
        }
    }
}

@Composable
private fun MasterKeyRotationSection(
    config: MasterKeyConfig,
    onConfigChanged: (MasterKeyConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Column {
                    Text(
                        "Master Key Rotation",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Automatically rotate master key",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Switch(
                checked = config.rotationEnabled,
                onCheckedChange = { onConfigChanged(config.copy(rotationEnabled = it)) }
            )
        }

        if (config.rotationEnabled) {
            DropdownSelector(
                label = "Rotation Period",
                options = KeyRotationSchedule.values().toList(),
                selectedOption = config.rotationPeriod,
                onOptionSelected = { onConfigChanged(config.copy(rotationPeriod = it)) },
                displayName = { it.displayName },
                icon = Icons.Default.Schedule
            )
        }
    }
}

@Composable
private fun KeyStoreSecuritySettings(
    config: KeyStoreConfig,
    onConfigChanged: (KeyStoreConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Key Store Security Settings",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            KeyManagementOption(
                title = "Hardware Protection",
                description = "Use hardware-based key protection",
                checked = config.enableHardwareProtection,
                onCheckedChange = { onConfigChanged(config.copy(enableHardwareProtection = it)) },
                icon = Icons.Default.Security
            )


            KeyManagementOption(
                title = "Key Wrapping",
                description = "Encrypt keys with key encryption keys",
                checked = config.enableKeyWrapping,
                onCheckedChange = { onConfigChanged(config.copy(enableKeyWrapping = it)) },
                icon = Icons.Default.Lock
            )
        }
    }
}

@Composable
private fun KeyBackupSettingsSection(
    backupSettings: KeyBackupSettings,
    onConfigChanged: (KeyBackupSettings) -> Unit
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Column {
                        Text(
                            "Key Backup Settings",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Configure automatic key backup and recovery",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = backupSettings.enableBackup,
                    onCheckedChange = { onConfigChanged(backupSettings.copy(enableBackup = it)) }
                )
            }

            if (backupSettings.enableBackup) {
                // Backup Location
                OutlinedTextField(
                    value = backupSettings.backupLocation,
                    onValueChange = { onConfigChanged(backupSettings.copy(backupLocation = it)) },
                    label = { Text("Backup Location") },
                    placeholder = { Text("./backup") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { /* File picker */ }) {
                            Icon(Icons.Default.FolderOpen, null)
                        }
                    }
                )

                // Backup Frequency and Retention
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DropdownSelector(
                        label = "Backup Frequency",
                        options = KeyRotationSchedule.values().toList(),
                        selectedOption = backupSettings.backupFrequency,
                        onOptionSelected = { onConfigChanged(backupSettings.copy(backupFrequency = it)) },
                        displayName = { it.displayName },
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = backupSettings.retainBackups.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { days ->
                                if (days > 0) {
                                    onConfigChanged(backupSettings.copy(retainBackups = days))
                                }
                            }
                        },
                        label = { Text("Retain (days)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }

                OutlinedTextField(
                    value = backupSettings.redundantBackups.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { copies ->
                            if (copies > 0) {
                                onConfigChanged(backupSettings.copy(redundantBackups = copies))
                            }
                        }
                    },
                    label = { Text("Redundant Backup Copies") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp))
                    }
                )

                // Backup Security Options
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyManagementOption(
                        title = "Encrypt Backups",
                        description = "Encrypt backup files for security",
                        checked = backupSettings.encryptBackups,
                        onCheckedChange = { onConfigChanged(backupSettings.copy(encryptBackups = it)) },
                        icon = Icons.Default.Lock
                    )

                    KeyManagementOption(
                        title = "Verify Backups",
                        description = "Verify backup integrity after creation",
                        checked = backupSettings.verifyBackups,
                        onCheckedChange = { onConfigChanged(backupSettings.copy(verifyBackups = it)) },
                        icon = Icons.Default.Verified
                    )

                    KeyManagementOption(
                        title = "Off-site Backup",
                        description = "Store backups in off-site location",
                        checked = backupSettings.offSiteBackup,
                        onCheckedChange = { onConfigChanged(backupSettings.copy(offSiteBackup = it)) },
                        icon = Icons.Default.Cloud
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyStorePathConfiguration(
    config: KeyStoreConfig,
    onConfigChanged: (KeyStoreConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = config.keyStorePath,
            onValueChange = { onConfigChanged(config.copy(keyStorePath = it)) },
            label = { Text("Key Store Path") },
            placeholder = { Text("./keystore") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Folder, null, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                IconButton(onClick = { /* File picker */ }) {
                    Icon(Icons.Default.FolderOpen, null)
                }
            }
        )

        OutlinedTextField(
            value = config.keyStorePassword,
            onValueChange = { onConfigChanged(config.copy(keyStorePassword = it)) },
            label = { Text("Key Store Password") },
            placeholder = { Text("Enter secure password") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp))
            },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )
    }
}

@Composable
private fun CryptographicConfigSection(
    config: CryptographicConfig,
    onConfigChanged: (CryptographicConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Default Algorithm Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DropdownSelector(
                label = "Default Symmetric Algorithm",
                options = CryptographicAlgorithm.values()
                    .filter { it.type == AlgorithmType.SYMMETRIC },
                selectedOption = config.defaultSymmetricAlgorithm,
                onOptionSelected = {
                    onConfigChanged(
                        config.copy(
                            defaultSymmetricAlgorithm = it,
                            defaultSymmetricKeySize = it.keySizes.first()
                        )
                    )
                },
                displayName = { it.displayName },
                icon = Icons.Default.Lock,
                modifier = Modifier.weight(1f)
            )

            DropdownSelector(
                label = "Default Asymmetric Algorithm",
                options = CryptographicAlgorithm.values()
                    .filter { it.type == AlgorithmType.ASYMMETRIC },
                selectedOption = config.defaultAsymmetricAlgorithm,
                onOptionSelected = {
                    onConfigChanged(
                        config.copy(
                            defaultAsymmetricAlgorithm = it,
                            defaultAsymmetricKeySize = it.keySizes.first()
                        )
                    )
                },
                displayName = { it.displayName },
                icon = Icons.Default.VpnKey,
                modifier = Modifier.weight(1f)
            )
        }

        // Default Key Sizes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DropdownSelector(
                label = "Symmetric Key Size",
                options = config.defaultSymmetricAlgorithm.keySizes,
                selectedOption = config.defaultSymmetricKeySize,
                onOptionSelected = { onConfigChanged(config.copy(defaultSymmetricKeySize = it)) },
                displayName = { "$it bits" },
                icon = Icons.Default.Straighten,
                modifier = Modifier.weight(1f)
            )

            DropdownSelector(
                label = "Asymmetric Key Size",
                options = config.defaultAsymmetricAlgorithm.keySizes,
                selectedOption = config.defaultAsymmetricKeySize,
                onOptionSelected = { onConfigChanged(config.copy(defaultAsymmetricKeySize = it)) },
                displayName = { "$it bits" },
                icon = Icons.Default.Straighten,
                modifier = Modifier.weight(1f)
            )
        }

        // Supported Algorithms Selection
        SupportedAlgorithmsSelector(
            selectedAlgorithms = config.supportedAlgorithms,
            onAlgorithmsChanged = { onConfigChanged(config.copy(supportedAlgorithms = it)) }
        )

        // Random Number Generator Configuration
        RandomNumberGeneratorSection(
            config = config,
            onConfigChanged = onConfigChanged
        )

        // Cryptographic Policy Settings
        CryptographicPolicySettings(
            config = config,
            onConfigChanged = onConfigChanged
        )
    }
}

@Composable
private fun SupportedAlgorithmsSelector(
    selectedAlgorithms: Set<CryptographicAlgorithm>,
    onAlgorithmsChanged: (Set<CryptographicAlgorithm>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Text(
                        "Supported Algorithms",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { onAlgorithmsChanged(CryptographicAlgorithm.values().toSet()) }
                    ) {
                        Text("Select All", style = MaterialTheme.typography.caption)
                    }
                    TextButton(
                        onClick = { onAlgorithmsChanged(emptySet()) }
                    ) {
                        Text("Clear All", style = MaterialTheme.typography.caption)
                    }
                }
            }

            // Group algorithms by type
            AlgorithmType.values().forEach { type ->
                val algorithmsOfType = CryptographicAlgorithm.values().filter { it.type == type }
                if (algorithmsOfType.isNotEmpty()) {
                    AlgorithmTypeSection(
                        type = type,
                        algorithms = algorithmsOfType,
                        selectedAlgorithms = selectedAlgorithms,
                        onAlgorithmsChanged = onAlgorithmsChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun AlgorithmTypeSection(
    type: AlgorithmType,
    algorithms: List<CryptographicAlgorithm>,
    selectedAlgorithms: Set<CryptographicAlgorithm>,
    onAlgorithmsChanged: (Set<CryptographicAlgorithm>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            type.displayName,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.primary
        )

        algorithms.forEach { algorithm ->
            AlgorithmOption(
                algorithm = algorithm,
                selected = algorithm in selectedAlgorithms,
                onSelectionChanged = { selected ->
                    val newAlgorithms = if (selected) {
                        selectedAlgorithms + algorithm
                    } else {
                        selectedAlgorithms - algorithm
                    }
                    onAlgorithmsChanged(newAlgorithms)
                }
            )
        }
    }
}

@Composable
private fun AlgorithmOption(
    algorithm: CryptographicAlgorithm,
    selected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                algorithm.displayName,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    algorithm.description,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "Key sizes: ${algorithm.keySizes.joinToString(", ")} bits",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.7f)
                )
            }
        }
        Checkbox(
            checked = selected,
            onCheckedChange = onSelectionChanged,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colors.primary
            )
        )
    }
}

@Composable
private fun RandomNumberGeneratorSection(
    config: CryptographicConfig,
    onConfigChanged: (CryptographicConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Random Number Generation",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }

            DropdownSelector(
                label = "Random Number Generator",
                options = RandomNumberGenerator.values().toList(),
                selectedOption = config.randomNumberGenerator,
                onOptionSelected = { onConfigChanged(config.copy(randomNumberGenerator = it)) },
                displayName = { it.displayName },
                icon = Icons.Default.Casino
            )

            // RNG Description
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Text(
                        config.randomNumberGenerator.description,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CryptographicPolicySettings(
    config: CryptographicConfig,
    onConfigChanged: (CryptographicConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Cryptographic Policy Settings",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            KeyManagementOption(
                title = "Hardware Acceleration",
                description = "Use hardware acceleration for cryptographic operations",
                checked = config.enableHardwareAcceleration,
                onCheckedChange = { onConfigChanged(config.copy(enableHardwareAcceleration = it)) },
                icon = Icons.Default.Speed
            )

            KeyManagementOption(
                title = "Validate Key Strength",
                description = "Validate cryptographic key strength during generation",
                checked = config.validateKeyStrength,
                onCheckedChange = { onConfigChanged(config.copy(validateKeyStrength = it)) },
                icon = Icons.Default.Verified
            )

            KeyManagementOption(
                title = "FIPS Compliance Mode",
                description = "Enforce FIPS 140-2 compliance for all operations",
                checked = config.enforceFipsCompliance,
                onCheckedChange = { onConfigChanged(config.copy(enforceFipsCompliance = it)) },
                icon = Icons.Default.Security
            )

            KeyManagementOption(
                title = "Allow Weak Algorithms",
                description = "Allow deprecated or weak cryptographic algorithms",
                checked = config.allowWeakAlgorithms,
                onCheckedChange = { onConfigChanged(config.copy(allowWeakAlgorithms = it)) },
                icon = Icons.Default.Warning
            )
        }
    }
}

@Composable
private fun KeyLifecycleSection(
    config: KeyLifecycleConfig,
    onConfigChanged: (KeyLifecycleConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Key Generation Policy
        DropdownSelector(
            label = "Key Generation Policy",
            options = KeyGenerationPolicy.values().toList(),
            selectedOption = config.generationPolicy,
            onOptionSelected = { onConfigChanged(config.copy(generationPolicy = it)) },
            displayName = { it.displayName },
            icon = Icons.Default.Add
        )

        // Key Rotation Configuration
        KeyRotationConfiguration(
            config = config,
            onConfigChanged = onConfigChanged
        )

        // Key Archival Configuration
        KeyArchivalConfiguration(
            config = config,
            onConfigChanged = onConfigChanged
        )

        // Key Destruction Configuration
        KeyDestructionConfiguration(
            config = config,
            onConfigChanged = onConfigChanged
        )

        // Key Lifecycle Options
        KeyLifecycleOptions(
            config = config,
            onConfigChanged = onConfigChanged
        )
    }
}

@Composable
private fun KeyRotationConfiguration(
    config: KeyLifecycleConfig,
    onConfigChanged: (KeyLifecycleConfig) -> Unit
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
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Key Rotation Schedules",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DropdownSelector(
                    label = "Rotation Schedule",
                    options = KeyRotationSchedule.values().toList(),
                    selectedOption = config.rotationSchedule,
                    onOptionSelected = { onConfigChanged(config.copy(rotationSchedule = it)) },
                    displayName = { it.displayName },
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )

                if (config.rotationSchedule == KeyRotationSchedule.CUSTOM) {
                    OutlinedTextField(
                        value = config.customRotationDays.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { days ->
                                if (days > 0) {
                                    onConfigChanged(config.copy(customRotationDays = days))
                                }
                            }
                        },
                        label = { Text("Custom Days") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }
            }

            KeyManagementOption(
                title = "Automatic Rotation",
                description = "Enable automatic key rotation based on schedule",
                checked = config.enableAutomaticRotation,
                onCheckedChange = { onConfigChanged(config.copy(enableAutomaticRotation = it)) },
                icon = Icons.Default.Autorenew
            )
        }
    }
}

@Composable
private fun KeyArchivalConfiguration(
    config: KeyLifecycleConfig,
    onConfigChanged: (KeyLifecycleConfig) -> Unit
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
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Key Archival Settings",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DropdownSelector(
                    label = "Archival Policy",
                    options = KeyArchivalPolicy.values().toList(),
                    selectedOption = config.archivalPolicy,
                    onOptionSelected = { onConfigChanged(config.copy(archivalPolicy = it)) },
                    displayName = { it.displayName },
                    icon = Icons.Default.Archive,
                    modifier = Modifier.weight(1f)
                )

                if (config.archivalPolicy in listOf(
                        KeyArchivalPolicy.DELAYED,
                        KeyArchivalPolicy.CONDITIONAL
                    )
                ) {
                    OutlinedTextField(
                        value = config.archivalGracePeriod.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { days ->
                                if (days >= 0) {
                                    onConfigChanged(config.copy(archivalGracePeriod = days))
                                }
                            }
                        },
                        label = { Text("Grace Period (days)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyDestructionConfiguration(
    config: KeyLifecycleConfig,
    onConfigChanged: (KeyLifecycleConfig) -> Unit
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
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.error
                )
                Text(
                    "Key Destruction Policies",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DropdownSelector(
                    label = "Destruction Policy",
                    options = KeyDestructionPolicy.values().toList(),
                    selectedOption = config.destructionPolicy,
                    onOptionSelected = { onConfigChanged(config.copy(destructionPolicy = it)) },
                    displayName = { it.displayName },
                    icon = Icons.Default.Delete,
                    modifier = Modifier.weight(1f)
                )

                if (config.destructionPolicy in listOf(
                        KeyDestructionPolicy.SCHEDULED,
                        KeyDestructionPolicy.COMPLIANCE_BASED
                    )
                ) {
                    OutlinedTextField(
                        value = config.destructionGracePeriod.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { days ->
                                if (days >= 0) {
                                    onConfigChanged(config.copy(destructionGracePeriod = days))
                                }
                            }
                        },
                        label = { Text("Grace Period (days)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                        }
                    )
                }
            }

            // Warning for destruction policy
            if (config.destructionPolicy != KeyDestructionPolicy.NEVER) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp,
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colors.error
                        )
                        Text(
                            "Warning: Key destruction is irreversible. Ensure proper backup and compliance procedures.",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyLifecycleOptions(
    config: KeyLifecycleConfig,
    onConfigChanged: (KeyLifecycleConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeyManagementOption(
                title = "Key Escrow",
                description = "Enable key escrow for regulatory compliance",
                checked = config.enableKeyEscrow,
                onCheckedChange = { onConfigChanged(config.copy(enableKeyEscrow = it)) },
                icon = Icons.Default.Security
            )

            KeyManagementOption(
                title = "Key Recovery",
                description = "Enable key recovery mechanisms",
                checked = config.enableKeyRecovery,
                onCheckedChange = { onConfigChanged(config.copy(enableKeyRecovery = it)) },
                icon = Icons.Default.Restore
            )

            KeyManagementOption(
                title = "Audit Key Operations",
                description = "Log all key lifecycle operations for audit",
                checked = config.auditKeyOperations,
                onCheckedChange = { onConfigChanged(config.copy(auditKeyOperations = it)) },
                icon = Icons.Default.Assessment
            )
        }
    }
}

@Composable
private fun KeyManagementOption(
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
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                focusedLabelColor = MaterialTheme.colors.primary
            )
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
