package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.AuditConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.AuditEventType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.AuditLogLevel
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.AuthenticationConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.AuthenticationMethod
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.CipherSuite
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.ComplianceFramework
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.EncryptionConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.KeyExchangeProtocol
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.MFAMethod
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.PasswordPolicy
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.RetentionPeriod
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.RoleBasedAccessConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.SecurityConfiguration
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.SessionManagementConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.UserRole


@Composable
fun SecurityConfigTab(
    securityConfig: SecurityConfiguration,
    onConfigUpdated: (SecurityConfiguration) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentConfig by remember { mutableStateOf(securityConfig) }

    LaunchedEffect(currentConfig) {
        onConfigUpdated(currentConfig)
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Authentication Configuration
        SecuritySection(
            title = "Authentication Configuration",
            icon = Icons.Default.Lock
        ) {
            AuthenticationConfigSection(
                config = currentConfig.authenticationConfig,
                onConfigChanged = {
                    currentConfig = currentConfig.copy(authenticationConfig = it)
                }
            )
        }

        // Role-Based Access Control
        SecuritySection(
            title = "Role-Based Access Control",
            icon = Icons.Default.Group
        ) {
            RoleBasedAccessSection(
                config = currentConfig.roleBasedAccessConfig,
                onConfigChanged = {
                    currentConfig = currentConfig.copy(roleBasedAccessConfig = it)
                }
            )
        }

        // Session Management
        SecuritySection(
            title = "Session Management",
            icon = Icons.Default.Schedule
        ) {
            SessionManagementSection(
                config = currentConfig.sessionManagement,
                onConfigChanged = {
                    currentConfig = currentConfig.copy(sessionManagement = it)
                }
            )
        }

        // Encryption Configuration
        SecuritySection(
            title = "Encryption Configuration",
            icon = Icons.Default.Security
        ) {
            EncryptionConfigSection(
                config = currentConfig.encryptionConfig,
                onConfigChanged = {
                    currentConfig = currentConfig.copy(encryptionConfig = it)
                }
            )
        }

        // Audit Configuration
        SecuritySection(
            title = "Audit Configuration",
            icon = Icons.Default.Assessment
        ) {
            AuditConfigSection(
                config = currentConfig.auditConfig,
                onConfigChanged = {
                    currentConfig = currentConfig.copy(auditConfig = it)
                }
            )
        }
    }
}

@Composable
private fun SecuritySection(
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
private fun AuthenticationConfigSection(
    config: AuthenticationConfig,
    onConfigChanged: (AuthenticationConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Primary Authentication Method
        DropdownSelector(
            label = "Primary Authentication Method",
            options = AuthenticationMethod.values().toList(),
            selectedOption = config.primaryMethod,
            onOptionSelected = { onConfigChanged(config.copy(primaryMethod = it)) },
            displayName = { it.displayName },
            icon = config.primaryMethod.icon
        )

        // Authentication Methods Selection
        AuthenticationMethodsSelector(
            selectedMethods = config.enabledMethods,
            onMethodsChanged = { onConfigChanged(config.copy(enabledMethods = it)) }
        )

        // Password Policy
        if (AuthenticationMethod.PASSWORD in config.enabledMethods) {
            PasswordPolicySection(
                policy = config.passwordPolicy,
                onPolicyChanged = { onConfigChanged(config.copy(passwordPolicy = it)) }
            )
        }

        // Multi-Factor Authentication
        MFAConfigSection(
            mfaEnabled = config.mfaEnabled,
            mfaMethods = config.mfaMethods,
            onMFAEnabledChanged = { onConfigChanged(config.copy(mfaEnabled = it)) },
            onMFAMethodsChanged = { onConfigChanged(config.copy(mfaMethods = it)) }
        )

        // Account Security Settings
        AccountSecuritySettings(
            config = config,
            onConfigChanged = onConfigChanged
        )
    }
}

@Composable
private fun AuthenticationMethodsSelector(
    selectedMethods: Set<AuthenticationMethod>,
    onMethodsChanged: (Set<AuthenticationMethod>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Enabled Authentication Methods",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(320.dp)
        ) {
            items(AuthenticationMethod.values()) { method ->
                AuthMethodCard(
                    method = method,
                    isSelected = method in selectedMethods,
                    onClick = {
                        val newMethods = if (method in selectedMethods) {
                            selectedMethods - method
                        } else {
                            selectedMethods + method
                        }
                        onMethodsChanged(newMethods)
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthMethodCard(
    method: AuthenticationMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        elevation = if (isSelected) 4.dp else 1.dp,
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
                    imageVector = if (isSelected) Icons.Default.CheckCircle else method.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    method.displayName,
                    style = MaterialTheme.typography.caption,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun PasswordPolicySection(
    policy: PasswordPolicy,
    onPolicyChanged: (PasswordPolicy) -> Unit
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
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Password Policy",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = policy.minLength.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { length ->
                            if (length > 0) {
                                onPolicyChanged(policy.copy(minLength = length))
                            }
                        }
                    },
                    label = { Text("Min Length") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Straighten, null, modifier = Modifier.size(20.dp))
                    }
                )

                OutlinedTextField(
                    value = policy.maxLength.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { length ->
                            if (length > 0) {
                                onPolicyChanged(policy.copy(maxLength = length))
                            }
                        }
                    },
                    label = { Text("Max Length") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Straighten, null, modifier = Modifier.size(20.dp))
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = policy.historySize.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { size ->
                            if (size >= 0) {
                                onPolicyChanged(policy.copy(historySize = size))
                            }
                        }
                    },
                    label = { Text("Password History") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.History, null, modifier = Modifier.size(20.dp))
                    }
                )

                OutlinedTextField(
                    value = policy.maxAge.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { age ->
                            if (age > 0) {
                                onPolicyChanged(policy.copy(maxAge = age))
                            }
                        }
                    },
                    label = { Text("Max Age (days)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(20.dp))
                    }
                )
            }

            // Password requirements
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecurityOption(
                    title = "Require Uppercase Letters",
                    description = "At least one uppercase character (A-Z)",
                    checked = policy.requireUppercase,
                    onCheckedChange = { onPolicyChanged(policy.copy(requireUppercase = it)) },
                    icon = Icons.Default.FormatSize
                )

                SecurityOption(
                    title = "Require Lowercase Letters",
                    description = "At least one lowercase character (a-z)",
                    checked = policy.requireLowercase,
                    onCheckedChange = { onPolicyChanged(policy.copy(requireLowercase = it)) },
                    icon = Icons.Default.FormatSize
                )

                SecurityOption(
                    title = "Require Numbers",
                    description = "At least one numeric character (0-9)",
                    checked = policy.requireNumbers,
                    onCheckedChange = { onPolicyChanged(policy.copy(requireNumbers = it)) },
                    icon = Icons.Default.Numbers
                )

                SecurityOption(
                    title = "Require Special Characters",
                    description = "At least one special character (!@#$%^&*)",
                    checked = policy.requireSpecialChars,
                    onCheckedChange = { onPolicyChanged(policy.copy(requireSpecialChars = it)) },
                    icon = Icons.Default.Key
                )
            }
        }
    }
}

@Composable
private fun MFAConfigSection(
    mfaEnabled: Boolean,
    mfaMethods: Set<MFAMethod>,
    onMFAEnabledChanged: (Boolean) -> Unit,
    onMFAMethodsChanged: (Set<MFAMethod>) -> Unit
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
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Column {
                        Text(
                            "Multi-Factor Authentication",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Additional security layer beyond passwords",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = mfaEnabled,
                    onCheckedChange = onMFAEnabledChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary
                    )
                )
            }

            if (mfaEnabled) {
                Text(
                    "Select MFA Methods",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MFAMethod.values().forEach { method ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                method.displayName,
                                style = MaterialTheme.typography.body2
                            )
                            Checkbox(
                                checked = method in mfaMethods,
                                onCheckedChange = { checked ->
                                    val newMethods = if (checked) {
                                        mfaMethods + method
                                    } else {
                                        mfaMethods - method
                                    }
                                    onMFAMethodsChanged(newMethods)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colors.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSecuritySettings(
    config: AuthenticationConfig,
    onConfigChanged: (AuthenticationConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = config.sessionTimeout.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { timeout ->
                        if (timeout > 0) {
                            onConfigChanged(config.copy(sessionTimeout = timeout))
                        }
                    }
                },
                label = { Text("Session Timeout (seconds)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                }
            )

            OutlinedTextField(
                value = config.maxConcurrentSessions.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { sessions ->
                        if (sessions > 0) {
                            onConfigChanged(config.copy(maxConcurrentSessions = sessions))
                        }
                    }
                },
                label = { Text("Max Concurrent Sessions") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Group, null, modifier = Modifier.size(20.dp))
                }
            )
        }

        // Account Lockout Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 0.dp,
            backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
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
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colors.primary
                        )
                        Column {
                            Text(
                                "Account Lockout",
                                style = MaterialTheme.typography.subtitle2,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Lock accounts after failed login attempts",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Switch(
                        checked = config.accountLockoutEnabled,
                        onCheckedChange = { onConfigChanged(config.copy(accountLockoutEnabled = it)) }
                    )
                }

                if (config.accountLockoutEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = config.maxFailedAttempts.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { attempts ->
                                    if (attempts > 0) {
                                        onConfigChanged(config.copy(maxFailedAttempts = attempts))
                                    }
                                }
                            },
                            label = { Text("Max Failed Attempts") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(20.dp))
                            }
                        )

                        OutlinedTextField(
                            value = config.lockoutDuration.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { duration ->
                                    if (duration > 0) {
                                        onConfigChanged(config.copy(lockoutDuration = duration))
                                    }
                                }
                            },
                            label = { Text("Lockout Duration (seconds)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleBasedAccessSection(
    config: RoleBasedAccessConfig,
    onConfigChanged: (RoleBasedAccessConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // RBAC Enable/Disable
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
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Column {
                    Text(
                        "Enable Role-Based Access Control",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Control access based on user roles and permissions",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Switch(
                checked = config.rbacEnabled,
                onCheckedChange = { onConfigChanged(config.copy(rbacEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colors.primary
                )
            )
        }

        if (config.rbacEnabled) {
            // Default Role Selection
            DropdownSelector(
                label = "Default User Role",
                options = UserRole.values().toList(),
                selectedOption = config.defaultRole,
                onOptionSelected = { onConfigChanged(config.copy(defaultRole = it)) },
                displayName = { it.displayName },
                icon = Icons.Default.Person
            )

            // User Roles Overview
            UserRolesOverview()

            // RBAC Settings
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
                    Text(
                        "Access Control Settings",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )

                    SecurityOption(
                        title = "Role Hierarchy",
                        description = "Enable hierarchical role inheritance",
                        checked = config.roleHierarchy,
                        onCheckedChange = { onConfigChanged(config.copy(roleHierarchy = it)) },
                        icon = Icons.Default.AccountTree
                    )

                    SecurityOption(
                        title = "Permission Inheritance",
                        description = "Allow roles to inherit permissions from parent roles",
                        checked = config.inheritanceEnabled,
                        onCheckedChange = { onConfigChanged(config.copy(inheritanceEnabled = it)) },
                        icon = Icons.Default.Share
                    )

                    SecurityOption(
                        title = "Session Validation",
                        description = "Validate role permissions on each request",
                        checked = config.sessionValidation,
                        onCheckedChange = { onConfigChanged(config.copy(sessionValidation = it)) },
                        icon = Icons.Default.Verified
                    )

                    SecurityOption(
                        title = "Prevent Privilege Escalation",
                        description = "Block unauthorized privilege escalation attempts",
                        checked = !config.privilegeEscalation,
                        onCheckedChange = { onConfigChanged(config.copy(privilegeEscalation = !it)) },
                        icon = Icons.Default.Security
                    )
                }
            }
        }
    }
}

@Composable
private fun UserRolesOverview() {
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
                "Available User Roles",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            UserRole.values().forEach { role ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (role) {
                            UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                            UserRole.SECURITY_OFFICER -> Icons.Default.Security
                            UserRole.CRYPTO_OFFICER -> Icons.Default.VpnKey
                            UserRole.CRYPTO_USER -> Icons.Default.Key
                            UserRole.AUDITOR -> Icons.Default.Assessment
                            UserRole.OPERATOR -> Icons.Default.Person
                            UserRole.GUEST -> Icons.Default.PersonOutline
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colors.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            role.displayName,
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            role.permissions.take(3).joinToString(", ") +
                                    if (role.permissions.size > 3) "..." else "",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionManagementSection(
    config: SessionManagementConfig,
    onConfigChanged: (SessionManagementConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Session Timeouts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = config.sessionTimeout.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { timeout ->
                        if (timeout > 0) {
                            onConfigChanged(config.copy(sessionTimeout = timeout))
                        }
                    }
                },
                label = { Text("Session Timeout (seconds)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                }
            )

            OutlinedTextField(
                value = config.idleTimeout.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { timeout ->
                        if (timeout > 0) {
                            onConfigChanged(config.copy(idleTimeout = timeout))
                        }
                    }
                },
                label = { Text("Idle Timeout (seconds)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(20.dp))
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = config.maxConcurrentSessions.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { sessions ->
                        if (sessions > 0) {
                            onConfigChanged(config.copy(maxConcurrentSessions = sessions))
                        }
                    }
                },
                label = { Text("Max Concurrent Sessions") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Group, null, modifier = Modifier.size(20.dp))
                }
            )

            OutlinedTextField(
                value = config.sessionTokenExpiry.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { expiry ->
                        if (expiry > 0) {
                            onConfigChanged(config.copy(sessionTokenExpiry = expiry))
                        }
                    }
                },
                label = { Text("Token Expiry (seconds)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Token, null, modifier = Modifier.size(20.dp))
                }
            )
        }

        // Session Security Options
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
                Text(
                    "Session Security Options",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )

                SecurityOption(
                    title = "Require Reauthentication",
                    description = "Require password confirmation for sensitive operations",
                    checked = config.requireReauthentication,
                    onCheckedChange = { onConfigChanged(config.copy(requireReauthentication = it)) },
                    icon = Icons.Default.Lock
                )

                SecurityOption(
                    title = "Secure Session Cookies",
                    description = "Use secure flags on session cookies",
                    checked = config.sessionCookieSecure,
                    onCheckedChange = { onConfigChanged(config.copy(sessionCookieSecure = it)) },
                    icon = Icons.Default.Cookie
                )

                SecurityOption(
                    title = "HTTP-Only Cookies",
                    description = "Prevent client-side script access to cookies",
                    checked = config.sessionCookieHttpOnly,
                    onCheckedChange = { onConfigChanged(config.copy(sessionCookieHttpOnly = it)) },
                    icon = Icons.Default.Http
                )

                SecurityOption(
                    title = "Session Monitoring",
                    description = "Monitor and log session activities",
                    checked = config.enableSessionMonitoring,
                    onCheckedChange = { onConfigChanged(config.copy(enableSessionMonitoring = it)) },
                    icon = Icons.Default.Visibility
                )
            }
        }
    }
}

@Composable
private fun EncryptionConfigSection(
    config: EncryptionConfig,
    onConfigChanged: (EncryptionConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Preferred Cipher Suite and Key Exchange
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DropdownSelector(
                label = "Preferred Cipher Suite",
                options = CipherSuite.values().toList(),
                selectedOption = config.preferredCipherSuite,
                onOptionSelected = { onConfigChanged(config.copy(preferredCipherSuite = it)) },
                displayName = { "${it.displayName} (${it.strength})" },
                icon = Icons.Default.Security,
                modifier = Modifier.weight(1f)
            )

            DropdownSelector(
                label = "Key Exchange Protocol",
                options = KeyExchangeProtocol.values().toList(),
                selectedOption = config.preferredKeyExchange,
                onOptionSelected = { onConfigChanged(config.copy(preferredKeyExchange = it)) },
                displayName = { it.displayName },
                icon = Icons.Default.VpnKey,
                modifier = Modifier.weight(1f)
            )
        }

        // Supported Cipher Suites
        CipherSuitesSelector(
            title = "Supported Cipher Suites",
            selectedSuites = config.supportedCipherSuites,
            onSuitesChanged = { onConfigChanged(config.copy(supportedCipherSuites = it)) }
        )

        // Key Exchange Protocols
        KeyExchangeProtocolsSelector(
            selectedProtocols = config.keyExchangeProtocols,
            onProtocolsChanged = { onConfigChanged(config.copy(keyExchangeProtocols = it)) }
        )

        // Certificate and Hardware Security
        CertificateAndHardwareSection(
            config = config,
            onConfigChanged = onConfigChanged
        )
    }
}

@Composable
private fun CipherSuitesSelector(
    title: String,
    selectedSuites: Set<CipherSuite>,
    onSuitesChanged: (Set<CipherSuite>) -> Unit
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
            Text(
                title,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CipherSuite.values().forEach { suite ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                suite.displayName,
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Strength: ${suite.strength}",
                                style = MaterialTheme.typography.caption,
                                color = when (suite.strength) {
                                    "High" -> Color(0xFF4CAF50)
                                    "Medium" -> Color(0xFFFF9800)
                                    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                }
                            )
                        }
                        Checkbox(
                            checked = suite in selectedSuites,
                            onCheckedChange = { checked ->
                                val newSuites = if (checked) {
                                    selectedSuites + suite
                                } else {
                                    selectedSuites - suite
                                }
                                onSuitesChanged(newSuites)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyExchangeProtocolsSelector(
    selectedProtocols: Set<KeyExchangeProtocol>,
    onProtocolsChanged: (Set<KeyExchangeProtocol>) -> Unit
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
            Text(
                "Key Exchange Protocols",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyExchangeProtocol.values().forEach { protocol ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            protocol.displayName,
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = protocol in selectedProtocols,
                            onCheckedChange = { checked ->
                                val newProtocols = if (checked) {
                                    selectedProtocols + protocol
                                } else {
                                    selectedProtocols - protocol
                                }
                                onProtocolsChanged(newProtocols)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CertificateAndHardwareSection(
    config: EncryptionConfig,
    onConfigChanged: (EncryptionConfig) -> Unit
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
            Text(
                "Certificate Management & Hardware Security",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            SecurityOption(
                title = "Certificate Validation",
                description = "Validate X.509 certificates during authentication",
                checked = config.certificateValidation,
                onCheckedChange = { onConfigChanged(config.copy(certificateValidation = it)) },
                icon = Icons.Default.Notes
            )

            SecurityOption(
                title = "OCSP Validation",
                description = "Online Certificate Status Protocol validation",
                checked = config.ocspValidation,
                onCheckedChange = { onConfigChanged(config.copy(ocspValidation = it)) },
                icon = Icons.Default.Verified
            )

            SecurityOption(
                title = "CRL Validation",
                description = "Certificate Revocation List validation",
                checked = config.crlValidation,
                onCheckedChange = { onConfigChanged(config.copy(crlValidation = it)) },
                icon = Icons.Default.Block
            )

            SecurityOption(
                title = "Hardware Security Module",
                description = "Use HSM for cryptographic operations",
                checked = config.hsmIntegration,
                onCheckedChange = { onConfigChanged(config.copy(hsmIntegration = it)) },
                icon = Icons.Default.Memory
            )

            SecurityOption(
                title = "Hardware Security Features",
                description = "Enable hardware-based security features",
                checked = config.hardwareSecurityEnabled,
                onCheckedChange = { onConfigChanged(config.copy(hardwareSecurityEnabled = it)) },
                icon = Icons.Default.Security
            )
        }
    }
}

@Composable
private fun AuditConfigSection(
    config: AuditConfig,
    onConfigChanged: (AuditConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Audit Enable/Disable
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
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Column {
                    Text(
                        "Enable Audit Logging",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Comprehensive security event logging and monitoring",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Switch(
                checked = config.auditEnabled,
                onCheckedChange = { onConfigChanged(config.copy(auditEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colors.primary
                )
            )
        }

        if (config.auditEnabled) {
            // Log Level and Retention
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DropdownSelector(
                    label = "Audit Log Level",
                    options = AuditLogLevel.values().toList(),
                    selectedOption = config.logLevel,
                    onOptionSelected = { onConfigChanged(config.copy(logLevel = it)) },
                    displayName = { it.displayName },
                    icon = Icons.Default.FilterList,
                    modifier = Modifier.weight(1f)
                )

                DropdownSelector(
                    label = "Retention Period",
                    options = RetentionPeriod.values().toList(),
                    selectedOption = config.retentionPeriod,
                    onOptionSelected = { onConfigChanged(config.copy(retentionPeriod = it)) },
                    displayName = { it.displayName },
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )
            }

            // Audit Event Types
            AuditEventTypesSelector(
                selectedTypes = config.enabledEventTypes,
                onTypesChanged = { onConfigChanged(config.copy(enabledEventTypes = it)) }
            )

            // Compliance Frameworks
            ComplianceFrameworksSelector(
                selectedFrameworks = config.complianceFrameworks,
                onFrameworksChanged = { onConfigChanged(config.copy(complianceFrameworks = it)) }
            )

            // Audit Settings
            AuditSettings(
                config = config,
                onConfigChanged = onConfigChanged
            )

            // Log File Configuration
            LogFileConfiguration(
                config = config,
                onConfigChanged = onConfigChanged
            )
        }
    }
}

@Composable
private fun AuditEventTypesSelector(
    selectedTypes: Set<AuditEventType>,
    onTypesChanged: (Set<AuditEventType>) -> Unit
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
                Text(
                    "Audit Event Types",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { onTypesChanged(AuditEventType.values().toSet()) }
                    ) {
                        Text("Select All", style = MaterialTheme.typography.caption)
                    }
                    TextButton(
                        onClick = { onTypesChanged(emptySet()) }
                    ) {
                        Text("Clear All", style = MaterialTheme.typography.caption)
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(AuditEventType.values()) { eventType ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Checkbox(
                            checked = eventType in selectedTypes,
                            onCheckedChange = { checked ->
                                val newTypes = if (checked) {
                                    selectedTypes + eventType
                                } else {
                                    selectedTypes - eventType
                                }
                                onTypesChanged(newTypes)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            eventType.displayName,
                            style = MaterialTheme.typography.caption,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComplianceFrameworksSelector(
    selectedFrameworks: Set<ComplianceFramework>,
    onFrameworksChanged: (Set<ComplianceFramework>) -> Unit
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
            Text(
                "Compliance Frameworks",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ComplianceFramework.values().forEach { framework ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                framework.displayName,
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                framework.description,
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Checkbox(
                            checked = framework in selectedFrameworks,
                            onCheckedChange = { checked ->
                                val newFrameworks = if (checked) {
                                    selectedFrameworks + framework
                                } else {
                                    selectedFrameworks - framework
                                }
                                onFrameworksChanged(newFrameworks)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditSettings(
    config: AuditConfig,
    onConfigChanged: (AuditConfig) -> Unit
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
            Text(
                "Audit Security Settings",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            SecurityOption(
                title = "Real-time Monitoring",
                description = "Monitor and alert on security events in real-time",
                checked = config.realTimeMonitoring,
                onCheckedChange = { onConfigChanged(config.copy(realTimeMonitoring = it)) },
                icon = Icons.Default.Visibility
            )

            SecurityOption(
                title = "Log Encryption",
                description = "Encrypt audit logs to prevent tampering",
                checked = config.logEncryption,
                onCheckedChange = { onConfigChanged(config.copy(logEncryption = it)) },
                icon = Icons.Default.Lock
            )

            SecurityOption(
                title = "Log Digital Signing",
                description = "Digitally sign logs for integrity verification",
                checked = config.logSigning,
                onCheckedChange = { onConfigChanged(config.copy(logSigning = it)) },
                icon = Icons.Default.Verified
            )

            SecurityOption(
                title = "Tamper Protection",
                description = "Detect and prevent log file tampering",
                checked = config.tamperProtection,
                onCheckedChange = { onConfigChanged(config.copy(tamperProtection = it)) },
                icon = Icons.Default.Shield
            )

            SecurityOption(
                title = "Alert on Violations",
                description = "Send alerts when security violations are detected",
                checked = config.alertOnViolations,
                onCheckedChange = { onConfigChanged(config.copy(alertOnViolations = it)) },
                icon = Icons.Outlined.Alarm
            )

            SecurityOption(
                title = "Syslog Forwarding",
                description = "Forward audit logs to external syslog servers",
                checked = config.syslogForwarding,
                onCheckedChange = { onConfigChanged(config.copy(syslogForwarding = it)) },
                icon = Icons.Default.Send
            )
        }
    }
}

@Composable
private fun LogFileConfiguration(
    config: AuditConfig,
    onConfigChanged: (AuditConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
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
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Log File Configuration",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedTextField(
                value = config.logPath,
                onValueChange = { onConfigChanged(config.copy(logPath = it)) },
                label = { Text("Log File Path") },
                placeholder = { Text("./security_audit.log") },
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
                value = (config.maxLogSize / 1024 / 1024).toString(), // Convert bytes to MB
                onValueChange = {
                    it.toLongOrNull()?.let { sizeMB ->
                        if (sizeMB > 0) {
                            onConfigChanged(config.copy(maxLogSize = sizeMB * 1024 * 1024))
                        }
                    }
                },
                label = { Text("Max Log Size (MB)") },
                placeholder = { Text("1024") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(Icons.Default.Storage, null, modifier = Modifier.size(20.dp))
                }
            )
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