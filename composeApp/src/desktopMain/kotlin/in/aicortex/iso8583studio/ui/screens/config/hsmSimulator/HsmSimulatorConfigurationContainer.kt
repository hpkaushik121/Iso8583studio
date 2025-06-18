// HSMSimulatorScreen.kt
package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.BorderLight
import `in`.aicortex.iso8583studio.ui.navigation.ConnectionType
import `in`.aicortex.iso8583studio.ui.navigation.EncryptionLevel
import `in`.aicortex.iso8583studio.ui.navigation.HSMProfile
import `in`.aicortex.iso8583studio.ui.navigation.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.HSMStatus
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.NetworkConfig
import `in`.aicortex.iso8583studio.ui.navigation.OperatingMode
import `in`.aicortex.iso8583studio.ui.navigation.PerformanceConfig
import `in`.aicortex.iso8583studio.ui.navigation.SecurityConfig
import `in`.aicortex.iso8583studio.ui.navigation.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentChip
import org.jetbrains.exposed.v1.core.arrayParam

// ============================================================================
// CONFIGURATION SCREEN
// ============================================================================
/**
 * HSM Configuration Management Screen
 */
@Composable
fun HSMSimulatorConfigContainer(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
    onConfigSelected: (HSMSimulatorConfig) -> Unit,
    onCreateNew: () -> Unit,
    onDelete: () -> Unit,
    onLaunch: () -> Unit,
) {
    var changeCounter by remember { mutableStateOf(0) }
    var selectedTab by remember { mutableStateOf(0) }
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Panel - Configuration List
        HSMConfigListPanel(
            navigationController = navigationController,
            appState = appState,
            onConfigSelected = {
                onConfigSelected(it)
                changeCounter += 1
            },
            onCreateNew = onCreateNew,
            onDelete = onDelete,
            onLaunch = onLaunch,
            modifier = Modifier.width(350.dp)
        )

        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
        )
        key(changeCounter) {
            // Right Panel - Configuration Editor
            if (appState.hsmConfigs.value.isNotEmpty()) {
                HSMConfigEditorPanel(
                    config = appState.currentConfig(SimulatorType.HSM) as HSMSimulatorConfig,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onConfigUpdated = { hsmConfig ->
                        appState.updateConfig(hsmConfig)
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                EmptyConfigurationPanel(
                    onCreateNew = onCreateNew,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Left Panel - HSM Configuration List
 */
/**
 * Enhanced HSM Configuration List Panel with Host Simulator Theming
 */
@Composable
fun HSMConfigListPanel(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
    onConfigSelected: (HSMSimulatorConfig) -> Unit,
    onCreateNew: () -> Unit,
    onDelete: () -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var changeCounter by remember { mutableStateOf(0) }
    Card(
        modifier = modifier
            .fillMaxHeight()
            .padding(12.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with HSM icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "HSM Simulator",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colors.primary
                )
                Column {
                    Text(
                        "HSM Simulator",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Configurations",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Development status chip
            UnderDevelopmentChip(
                status = DevelopmentStatus.BETA
            )

            // Configuration list container
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (appState.hsmConfigs.value.isEmpty()) {
                        // Empty state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No Configs Found",
                                    style = MaterialTheme.typography.subtitle2,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    "Create your first HSM configuration to begin",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {

                        key(changeCounter) {
                            appState.hsmConfigs.value.forEachIndexed { index, config ->
                                val isSelected =
                                    index == appState.selectedConfigIndex.value[SimulatorType.HSM]
                                HSMConfigItem(
                                    config = config,
                                    isSelected = isSelected,
                                    onClick = {
                                        onConfigSelected(config)
                                        changeCounter += 1
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Configuration management section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                backgroundColor = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Management",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCreateNew,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colors.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", style = MaterialTheme.typography.caption)
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            enabled = appState.hsmConfigs.value.isNotEmpty(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colors.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", style = MaterialTheme.typography.caption)
                        }
                    }

                    Button(
                        onClick = { /* TODO: Implement save all */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save All",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save All Configurations")
                    }
                }
            }

            // Launch Simulator section
            if (appState.currentConfig(SimulatorType.HSM) != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    backgroundColor = MaterialTheme.colors.secondary,
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
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Launch",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Text(
                                "Ready to Launch",
                                style = MaterialTheme.typography.subtitle2,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        key(changeCounter) {


                            val currentConfig =
                                appState.currentConfig(SimulatorType.HSM) as HSMSimulatorConfig
                            Text(
                                "Configuration: ${currentConfig.name.ifEmpty { "Unnamed HSM" }}",
                                style = MaterialTheme.typography.caption,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            Text(
                                "Vendor: ${currentConfig.vendor.displayName} ${currentConfig.model}",
                                style = MaterialTheme.typography.caption,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            Button(
                                onClick = onLaunch,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color.White,
                                    contentColor = MaterialTheme.colors.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Launch HSM Simulator",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Launch HSM Simulator",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enhanced HSM Configuration Item with Host Simulator styling
 */
@Composable
fun HSMConfigItem(
    config: HSMSimulatorConfig,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = if (isSelected) Color.White else MaterialTheme.colors.onSurface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Shield else Icons.Default.Security,
                            contentDescription = "HSM Configuration",
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else MaterialTheme.colors.primary
                        )
                        Text(
                            config.name.ifEmpty { "Unnamed HSM" },
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Text(
                        "${config.vendor.displayName} ${config.model}",
                        style = MaterialTheme.typography.caption,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colors.onSurface.copy(
                            alpha = 0.6f
                        )
                    )

                    if (config.profile.firmwareVersion.isNotEmpty()) {
                        Text(
                            "Firmware: ${config.profile.firmwareVersion}",
                            style = MaterialTheme.typography.caption,
                            color = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colors.onSurface.copy(
                                alpha = 0.5f
                            )
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) Color.White.copy(alpha = 0.2f) else config.status.color,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = config.status.displayName,
                            style = MaterialTheme.typography.caption,
                            color = if (isSelected) Color.White else Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced Empty Configuration Panel to match Host Simulator style
 */
@Composable
fun EmptyConfigurationPanel(
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .padding(12.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Configuration Selected",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Create or select a configuration to start editing",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onCreateNew,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Configuration")
                }
            }
        }
    }
}


/**
 * Right Panel - Configuration Editor
 */
@Composable
fun HSMConfigEditorPanel(
    config: HSMSimulatorConfig,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onConfigUpdated: (HSMSimulatorConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        "Profile" to Icons.Default.Memory,
        "Network" to Icons.Default.NetworkWifi,
        "Security" to Icons.Default.Security,
        "Performance" to Icons.Default.Speed,
        "Keys" to Icons.Default.VpnKey,
        "Advanced" to Icons.Default.Settings
    )

    Card(
        modifier = modifier
            .fillMaxHeight()
            .padding(12.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Tab selection - matching host simulator right panel design
                TabRow(
                    selectedTabIndex = selectedTab,
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.primary,
                    divider = {
                        Divider(
                            color = BorderLight,
                            thickness = 1.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, (title, icon) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { onTabSelected(index) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.caption
                                    )
                                }
                            },
                            selectedContentColor = MaterialTheme.colors.primary,
                            unselectedContentColor = MaterialTheme.colors.onSurface.copy(
                                alpha = 0.7f
                            )
                        )
                    }
                }

                // Tab content - matching host simulator scroll behavior exactly
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        when (selectedTab) {
                            0 -> HSMProfileTab(config = config) {
                                onConfigUpdated(it)
                            }

                            1 -> NetworkConfigTab(config.network) {
                                onConfigUpdated(config.copy(network = it))
                            }

                            2 -> SecurityConfigTab(config.security) {
                                onConfigUpdated(config.copy(security = it))
                            }

                            3 -> PerformanceConfigTab(config.performance) {
                                onConfigUpdated(config.copy(performance = it))
                            }

//                            4 -> KeyManagementTab(config) { updatedConfig ->
//                                onConfigUpdated(updatedConfig)
//                            }

//                            5 -> AdvancedConfigTab(config) { updatedConfig ->
//                                onConfigUpdated(updatedConfig)
//                            }
                        }
                    }
                }
            }
        }
    }
}


// ============================================================================
// TAB IMPLEMENTATIONS (PLACEHOLDERS)
// ============================================================================

@Composable
fun SecurityConfigTab(
    security: SecurityConfig,
    onUpdate: (SecurityConfig) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Security Configuration",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )

        Card(elevation = 1.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Authentication", style = MaterialTheme.typography.subtitle1)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = security.authenticationRequired,
                        onCheckedChange = { onUpdate(security.copy(authenticationRequired = it)) }
                    )
                    Text("Require Authentication")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = security.auditEnabled,
                        onCheckedChange = { onUpdate(security.copy(auditEnabled = it)) }
                    )
                    Text("Enable Audit Trail")
                }

                Text("Encryption Level", style = MaterialTheme.typography.subtitle2)
                EncryptionLevel.values().forEach { level ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = security.encryptionLevel == level,
                            onClick = { onUpdate(security.copy(encryptionLevel = level)) }
                        )
                        Text(level.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        Card(elevation = 1.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Compliance Frameworks", style = MaterialTheme.typography.subtitle1)

                security.complianceFrameworks.forEach { framework ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = true, // TODO: Connect to actual state
                            onCheckedChange = { /* TODO: Implement */ }
                        )
                        Text(framework, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceConfigTab(
    performance: PerformanceConfig,
    onUpdate: (PerformanceConfig) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Performance Configuration",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )

        Card(elevation = 1.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Throughput Settings", style = MaterialTheme.typography.subtitle1)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = performance.maxTPS.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { tps ->
                                onUpdate(performance.copy(maxTPS = tps))
                            }
                        },
                        label = { Text("Max TPS") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = performance.responseTimeTarget.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { target ->
                                onUpdate(performance.copy(responseTimeTarget = target))
                            }
                        }
                    )
                }
            }
        }
    }

}