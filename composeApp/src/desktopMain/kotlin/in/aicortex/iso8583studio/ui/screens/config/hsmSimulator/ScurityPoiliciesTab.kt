package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.HSMUser
import `in`.aicortex.iso8583studio.ui.navigation.SecurityPolicies
import `in`.aicortex.iso8583studio.ui.screens.components.StyledTextField




/**
 * Security Policies Tab
 * Centralizes all user, authentication, and access control settings.
 */
@Composable
fun SecurityPoliciesTab(config: HSMSimulatorConfig, onConfigUpdate: (HSMSimulatorConfig) -> Unit) {
    val policies = config.securityPolicies

    // Using a simple Column as this will be placed in a scrollable parent.
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        // --- User Management ---
        ConfigSectionCard(
            title = "User Management",
            icon = Icons.Default.AdminPanelSettings,
            description = "Manage users and their assigned roles within the HSM."
        ) {
            UserManagementContent(
                users = policies.users,
                onUsersUpdate = { updatedUsers ->
                    onConfigUpdate(config.copy(securityPolicies = policies.copy(users = updatedUsers)))
                }
            )
        }

        // --- PIN Policies ---
        ConfigSectionCard(
            title = "PIN Policies",
            icon = Icons.Default.Password,
            description = "Define security constraints for user and officer PINs."
        ) {
            PinPoliciesContent(
                policies = policies,
                onPoliciesUpdate = { updatedPolicies ->
                    onConfigUpdate(config.copy(securityPolicies = updatedPolicies))
                }
            )
        }

        // --- M-of-N / Dual Control ---
        ConfigSectionCard(
            title = "M-of-N Quorum (Dual Control)",
            icon = Icons.Default.People,
            description = "Require multiple administrators to authorize critical operations."
        ) {
            MofNContent(
                policies = policies,
                onPoliciesUpdate = { updatedPolicies ->
                    onConfigUpdate(config.copy(securityPolicies = updatedPolicies))
                }
            )
        }
    }
}

@Composable
private fun UserManagementContent(
    users: List<HSMUser>,
    onUsersUpdate: (List<HSMUser>) -> Unit
) {
    // This is a placeholder for a more interactive user management system.
    // In a real implementation, you would have dialogs to add/edit users.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            elevation = 0.dp,
            backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                users.forEachIndexed { index, user ->
                    UserRow(user)
                    if (index < users.size - 1) {
                        Divider(Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
        OutlinedButton(
            onClick = { /* TODO: Show Add User Dialog */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add User", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add New User")
        }
    }
}

@Composable
private fun UserRow(user: HSMUser) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(user.username, fontWeight = FontWeight.Bold)
            Text(user.role, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.primary)
        }
        IconButton(onClick = { /* TODO: Show Edit Dialog */ }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit User", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
        }
        IconButton(onClick = { /* TODO: Show Delete Confirmation */ }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = MaterialTheme.colors.error.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun PinPoliciesContent(
    policies: SecurityPolicies,
    onPoliciesUpdate: (SecurityPolicies) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ConfigField("Min PIN Length", Modifier.weight(1f)) {
                StyledTextField((policies.minPinLength).toString(), { v -> v.toIntOrNull()?.let { onPoliciesUpdate(policies.copy(minPinLength = it)) } })
            }
            ConfigField("Max PIN Length", Modifier.weight(1f)) {
                StyledTextField((policies.maxPinLength).toString(), { v -> v.toIntOrNull()?.let { onPoliciesUpdate(policies.copy(maxPinLength = it)) } })
            }
        }
        ConfigField("PIN Retry Limit (before lockout)") {
            StyledTextField((policies.pinRetryLimit).toString(), { v -> v.toIntOrNull()?.let { onPoliciesUpdate(policies.copy(pinRetryLimit = it)) } })
        }
        FeatureOption(
            title = "Enable PIN Lockout",
            description = "Lock the user account after the retry limit is exceeded.",
            enabled = policies.enablePinLockout,
            onToggle = { onPoliciesUpdate(policies.copy(enablePinLockout = it)) }
        )
    }
}

@Composable
private fun MofNContent(
    policies: SecurityPolicies,
    onPoliciesUpdate: (SecurityPolicies) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FeatureOption(
            title = "Enable M-of-N Quorum",
            description = "Require multiple administrators to approve critical operations.",
            enabled = policies.enableMofN,
            onToggle = { onPoliciesUpdate(policies.copy(enableMofN = it)) }
        )
        AnimatedVisibility(policies.enableMofN) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top=8.dp)) {
                ConfigField("M-Value (Required)", Modifier.weight(1f)) {
                    StyledTextField(policies.mValue.toString(), { v -> v.toIntOrNull()?.let { onPoliciesUpdate(policies.copy(mValue = it)) } })
                }
                ConfigField("N-Value (Total)", Modifier.weight(1f)) {
                    StyledTextField(policies.nValue.toString(), { v -> v.toIntOrNull()?.let { onPoliciesUpdate(policies.copy(nValue = it)) } })
                }
            }
        }
    }
}


// --- Helper & Reusable Components ---

@Composable
private fun ConfigSectionCard(
    title: String,
    icon: ImageVector,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colors.primary, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.h6)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 36.dp)
        )
        Spacer(Modifier.height(16.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}


@Composable
fun FeatureOption(
    title: String,
    enabled: Boolean,
    description: String? = null,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
        )
    }
}

@Composable
fun ConfigField(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
        content()
    }
}
