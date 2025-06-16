package `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.BorderLight
import `in`.aicortex.iso8583studio.ui.navigation.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentChip
import java.awt.Cursor
import kotlin.random.Random

private enum class HSMSimulatorConfigTabs(val label: String) {
    HSM_SETUP("HSM Setup"),
    KEY_MANAGEMENT("Key Management"),
    CRYPTO_CONFIG("Crypto Config"),
    SECURITY_POLICIES("Security Policies")
}

/**
 * HSM Simulator Configuration Container
 * Left panel: HSM configuration profiles and management
 * Right panel: Selected profile editing with simulator launch
 */
@Composable
fun HSMSimulatorConfigContainer(
    navigationController: NavigationController,
    appState: UnifiedSimulatorState,
    onSelectConfig: (HSMSimulatorConfig) -> Unit,
    onAddConfig: () -> Unit,
    onDeleteConfig: () -> Unit,
    onSaveAllConfigs: () -> Unit,
    onLaunchSimulator: () -> Unit,
) {
    val tabs = HSMSimulatorConfigTabs.values().toList()
    var selectedTab by remember { mutableStateOf(HSMSimulatorConfigTabs.HSM_SETUP) }

    // State for the left panel width
    var leftPanelWidth by remember { mutableStateOf(appState.panelWidth) }
    // State for tracking if user is currently resizing
    var isResizing by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel - HSM Simulator Configuration Profiles
            Card(
                modifier = Modifier
                    .width(leftPanelWidth)
                    .fillMaxHeight()
                    .padding(12.dp),
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
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
                                "Configuration Profiles",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    UnderDevelopmentChip(
                        status = DevelopmentStatus.EXPERIMENTAL
                    )

                    // Profile list
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        elevation = 0.dp,
                        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .verticalScroll(scrollState),
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
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "No HSM Profiles",
                                            style = MaterialTheme.typography.subtitle2,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            "Create your first HSM configuration profile",
                                            style = MaterialTheme.typography.caption,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            } else {
                                appState.hsmConfigs.value.forEachIndexed { index, config ->
                                    val isSelected = index == appState.selectedConfigIndex
                                    HSMConfigProfileItem(
                                        config = config,
                                        isSelected = isSelected,
                                        onClick = { onSelectConfig(config) }
                                    )
                                }
                            }
                        }
                    }

                    // Profile management section
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
                                "Profile Management",
                                style = MaterialTheme.typography.subtitle2,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onAddConfig() },
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
                                    onClick = { onDeleteConfig() },
                                    modifier = Modifier.weight(1f),
                                    enabled = appState.selectedConfigIndex >= 0,
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
                                onClick = { onSaveAllConfigs() },
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
                                Text("Save All Profiles")
                            }
                        }
                    }

                    // HSM Status and Launch section
                    if (appState.currentConfig != null) {
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

                                Text(
                                    "Profile: ${Random.nextInt()}",
                                    style = MaterialTheme.typography.caption,
                                    color = Color.White.copy(alpha = 0.9f)
                                )

                                // HSM Status indicators
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HSMStatusChip(
                                        label = "Keys",
                                        count = "24",
                                        icon = Icons.Default.VpnKey
                                    )
                                    HSMStatusChip(
                                        label = "Slots",
                                        count = "8",
                                        icon = Icons.Default.Storage
                                    )
                                }

                                Button(
                                    onClick = { onLaunchSimulator() },
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

            // Resizable divider
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isResizing = true },
                            onDragEnd = { isResizing = false },
                            onDragCancel = { isResizing = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                leftPanelWidth = (leftPanelWidth + dragAmount.x.toDp())
                                    .coerceIn(350.dp, 600.dp)
                                appState.panelWidth = leftPanelWidth
                            }
                        )
                    }
                    .cursorForHorizontalResize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(4.dp)
                        .height(32.dp)
                        .shadow(1.dp, RoundedCornerShape(2.dp))
                        .background(
                            color = if (isResizing) MaterialTheme.colors.primary else BorderLight,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }

            // Right panel - HSM Configuration Editor
            if (appState.hsmConfigs.value.isNotEmpty() && appState.currentConfig != null) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(12.dp),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Tab selection
                            TabRow(
                                selectedTabIndex = selectedTab.ordinal,
                                backgroundColor = MaterialTheme.colors.surface,
                                contentColor = MaterialTheme.colors.primary,
                                divider = {
                                    Divider(
                                        color = BorderLight,
                                        thickness = 1.dp
                                    )
                                }
                            ) {
                                tabs.forEachIndexed { index, tab ->
                                    Tab(
                                        selected = selectedTab.ordinal == index,
                                        onClick = { selectedTab = tabs[index] },
                                        text = {
                                            Text(
                                                tab.label,
                                                fontWeight = if (selectedTab.ordinal == index) FontWeight.Bold else FontWeight.Normal,
                                                style = MaterialTheme.typography.caption
                                            )
                                        },
                                        selectedContentColor = MaterialTheme.colors.primary,
                                        unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Tab content
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
                                        HSMSimulatorConfigTabs.HSM_SETUP -> HSMSetupTab(
                                            config = appState.currentConfig!! as HSMSimulatorConfig
                                        ) { updatedConfig ->
//                                            navigationController.saveConfig(updatedConfig)
                                        }
                                        HSMSimulatorConfigTabs.KEY_MANAGEMENT -> KeyManagementTab(
                                            config = appState.currentConfig!! as HSMSimulatorConfig
                                        ) { updatedConfig ->
//                                            navigationController.saveConfig(updatedConfig)
                                        }
                                        HSMSimulatorConfigTabs.CRYPTO_CONFIG -> CryptoConfigTab(
                                            config = appState.currentConfig!! as HSMSimulatorConfig
                                        ) { updatedConfig ->
//                                            navigationController.saveConfig(updatedConfig)
                                        }
                                        HSMSimulatorConfigTabs.SECURITY_POLICIES -> SecurityPoliciesTab(
                                            config = appState.currentConfig!! as HSMSimulatorConfig
                                        ) { updatedConfig ->
//                                            navigationController.saveConfig(updatedConfig)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty state when no configuration selected
                Card(
                    modifier = Modifier
                        .weight(1f)
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
                                "No HSM Profile Selected",
                                style = MaterialTheme.typography.h6,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Create or select an HSM configuration profile to begin",
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { onAddConfig() },
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
                                Text("Create HSM Profile")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual HSM configuration profile item component
 */
@Composable
private fun HSMConfigProfileItem(
    config: HSMSimulatorConfig, // Replace with your actual HSM config type
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = if (isSelected) 4.dp else 1.dp,
        backgroundColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
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
                            imageVector = if (isSelected) Icons.Default.Security else Icons.Default.VpnKey,
                            contentDescription = "HSM Profile",
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else MaterialTheme.colors.primary
                        )
                        Text(
                            config.deviceInfo.deviceName,
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "PKCS#11",
                            style = MaterialTheme.typography.caption,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            "•",
                            style = MaterialTheme.typography.caption,
                            color = if (isSelected) Color.White.copy(alpha = 0.6f) else MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            "24 Keys",
                            style = MaterialTheme.typography.caption,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun HSMStatusChip(
    label: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
            Text(
                "$count $label",
                style = MaterialTheme.typography.caption,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}




@Composable
private fun MonitoringTab(config: Any, onConfigUpdate: (Any) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Monitoring Configuration",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Configure logging, auditing, and performance monitoring settings",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        // Add monitoring specific UI components here
    }
}

/**
 * Extension function to set the cursor for horizontal resize
 */
fun Modifier.cursorForHorizontalResize(): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this.hoverable(interactionSource)
        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
}
