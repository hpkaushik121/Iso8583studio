package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.BorderLight
import `in`.aicortex.iso8583studio.ui.navigation.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.PaymentMethod
import `in`.aicortex.iso8583studio.ui.navigation.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.UnifiedSimulatorState
import `in`.aicortex.iso8583studio.ui.screens.components.themedScrollbarStyle
import org.springframework.util.Assert.state
import java.awt.Cursor
import java.util.UUID

// --- DATA MODELS FOR POS SIMULATOR CONFIG ---


private enum class POSSimulatorConfigTabs(val label: String, val icon: ImageVector) {
    HARDWARE("Hardware", Icons.Default.Memory),
    TRANSACTION("Transaction", Icons.Default.SwapHoriz),
    SECURITY("Security", Icons.Default.Security),
    NETWORK_SOFTWARE("Network & SW", Icons.Default.Lan)
}

// --- MAIN CONFIGURATION CONTAINER ---

@Composable
fun POSSimulatorConfigContainer(
    // Simulating the structure from the reference
    // navigationController: NavigationController,
    appState: UnifiedSimulatorState,
    onLaunchSimulator: () -> Unit,
    onSaveAllConfigs: () -> Unit,
    onSelectConfig: (POSSimulatorConfig) -> Unit,
    onAddConfig: () -> Unit,
    onDeleteConfig: () -> Unit,
    onConfigUpdate: (POSSimulatorConfig) -> Unit
) {
    val tabs = POSSimulatorConfigTabs.values().toList()
    var selectedTab by remember { mutableStateOf(POSSimulatorConfigTabs.HARDWARE) }
    var leftPanelWidth by remember { mutableStateOf(appState.panelWidth) }
    var isResizing by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Panel - Profile Management
            ConfigProfilePanel(
                modifier = Modifier.width(leftPanelWidth),
                appState = appState,
                onSelectConfig = onSelectConfig,
                onAddConfig = onAddConfig,
                onDeleteConfig = onDeleteConfig,
                onSaveAllConfigs = onSaveAllConfigs,
                onLaunchSimulator = onLaunchSimulator
            )

            // Resizable Divider
            ResizableDivider(isResizing) { dragAmount ->
                leftPanelWidth = (leftPanelWidth + dragAmount.x.dp).coerceIn(350.dp, 600.dp)
                appState.panelWidth = leftPanelWidth
            }

            // Right Panel - Configuration Editor
            if (appState.posConfigs.value.isNotEmpty() && appState.currentConfig(SimulatorType.POS) != null) {
                ConfigEditorPanel(
                    modifier = Modifier.weight(1f),
                    currentConfig = appState.currentConfig(SimulatorType.POS) as POSSimulatorConfig,
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onAddConfig = onAddConfig,
                    onConfigUpdate = onConfigUpdate
                )
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
                                Text("Create POS Terminal Profile")
                            }
                        }
                    }
                }
            }

        }
    }
}

// --- LEFT PANEL & ITS COMPONENTS ---

@Composable
private fun ConfigProfilePanel(
    modifier: Modifier,
    appState: UnifiedSimulatorState,
    onSelectConfig: (POSSimulatorConfig) -> Unit,
    onAddConfig: () -> Unit,
    onDeleteConfig: () -> Unit,
    onSaveAllConfigs: () -> Unit,
    onLaunchSimulator: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxHeight().padding(12.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PointOfSale,
                    contentDescription = "POS Simulator",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colors.primary
                )
                Column {
                    Text(
                        "POS Simulator",
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
                    if (appState.posConfigs.value.isEmpty()) {
                        EmptyProfileListPrompt()
                    } else {
                        var changeCounter by remember { mutableStateOf(0) }
                        key(changeCounter) {
                            appState.posConfigs.value.forEachIndexed { index, config ->
                                POSConfigProfileItem(
                                    config = config,
                                    isSelected = index == appState.selectedConfigIndex.value[SimulatorType.POS],
                                    onClick = {
                                        onSelectConfig(config)
                                        changeCounter += 1
                                    }
                                )
                            }
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
                ProfileManagementButtons(
                    onAddConfig = onAddConfig,
                    onDeleteConfig = onDeleteConfig,
                    onSaveAllConfigs = onSaveAllConfigs,
                    isDeleteEnabled = appState.selectedConfigIndex.value[SimulatorType.POS]!! >= 0
                )
            }


            // Launch Section
            if (appState.currentConfig(SimulatorType.POS) != null) {
                LaunchSimulatorCard(
                    configName = (appState.currentConfig(SimulatorType.POS)!! as POSSimulatorConfig).name,
                    onLaunchSimulator = onLaunchSimulator
                )
            }
        }
    }
}

@Composable
private fun EmptyProfileListPrompt() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.AddToQueue,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No POS Profiles",
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Text(
                "Create a profile to get started",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun POSConfigProfileItem(
    config: POSSimulatorConfig,
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
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Terminal else Icons.Default.PointOfSale,
                            contentDescription = "POS Profile",
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else MaterialTheme.colors.primary
                        )
                        Text(
                            config.name,
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Text(
                        config.cardReaderTypes,
                        style = MaterialTheme.typography.caption,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colors.onSurface.copy(
                            alpha = 0.6f
                        )
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        "Selected",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileManagementButtons(
    onAddConfig: () -> Unit,
    onDeleteConfig: () -> Unit,
    onSaveAllConfigs: () -> Unit,
    isDeleteEnabled: Boolean
) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                onClick = onAddConfig,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.primary)
            ) {
                Icon(Icons.Default.Add, "Add", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", style = MaterialTheme.typography.caption)
            }
            OutlinedButton(
                onClick = onDeleteConfig,
                modifier = Modifier.weight(1f),
                enabled = isDeleteEnabled,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.error)
            ) {
                Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete", style = MaterialTheme.typography.caption)
            }
        }
        Button(
            onClick = onSaveAllConfigs,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
        ) {
            Icon(Icons.Default.Save, "Save All", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save All Profiles")
        }
    }
}

@Composable
private fun LaunchSimulatorCard(configName: String, onLaunchSimulator: () -> Unit) {
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
                    Icons.Default.PlayArrow,
                    "Launch",
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
                "Profile: $configName",
                style = MaterialTheme.typography.caption,
                color = Color.White.copy(alpha = 0.9f)
            )
            Button(
                onClick = onLaunchSimulator,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.White,
                    contentColor = MaterialTheme.colors.secondary
                )
            ) {
                Icon(Icons.Default.Terminal, "Launch Simulator", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch POS Simulator", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResizableDivider(isResizing: Boolean, onDrag: (dragAmount: Offset) -> Unit) {
    Box(
        modifier = Modifier.width(8.dp).fillMaxHeight().background(Color.Transparent)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
            .cursorForHorizontalResize()
    ) {
        Box(
            modifier = Modifier.align(Alignment.Center).width(4.dp).height(48.dp)
                .shadow(1.dp, RoundedCornerShape(2.dp))
                .background(
                    color = if (isResizing) MaterialTheme.colors.primary else BorderLight,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

// --- RIGHT PANEL & ITS COMPONENTS ---

@Composable
private fun ConfigEditorPanel(
    modifier: Modifier,
    currentConfig: POSSimulatorConfig?,
    tabs: List<POSSimulatorConfigTabs>,
    selectedTab: POSSimulatorConfigTabs,
    onTabSelected: (POSSimulatorConfigTabs) -> Unit,
    onAddConfig: () -> Unit,
    onConfigUpdate: (POSSimulatorConfig) -> Unit
) {
    if (currentConfig == null) {
        EmptyEditorPanel(modifier, onAddConfig)
        return
    }

    Card(
        modifier = modifier.fillMaxHeight().padding(12.dp),
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
                    divider = { Divider(color = BorderLight, thickness = 1.dp) }
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { onTabSelected(tab) },
                            text = {
                                Text(
                                    tab.label,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.caption
                                )
                            },
                            selectedContentColor = MaterialTheme.colors.primary,
                            unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp)) {
                    val scrollState = rememberScrollState()
                    Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        when (selectedTab) {
                            POSSimulatorConfigTabs.HARDWARE -> HardwareTab(
                                currentConfig,
                                onConfigUpdate
                            )

                            POSSimulatorConfigTabs.TRANSACTION -> TransactionTab(
                                currentConfig,
                                onConfigUpdate
                            )

                            POSSimulatorConfigTabs.SECURITY -> SecurityTab(
                                currentConfig,
                                onConfigUpdate
                            )

                            POSSimulatorConfigTabs.NETWORK_SOFTWARE -> NetworkSoftwareTab(
                                currentConfig,
                                onConfigUpdate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyEditorPanel(modifier: Modifier, onAddConfig: () -> Unit) {
    Card(
        modifier = modifier.fillMaxHeight().padding(12.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.PointOfSale,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                )
                Text(
                    "No Profile Selected",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    "Create or select a profile to begin configuration.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAddConfig,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    Icon(Icons.Default.Add, "Add", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create POS Profile")
                }
            }
        }
    }
}

// --- TAB IMPLEMENTATIONS ---

@Composable
private fun HardwareTab(config: POSSimulatorConfig, onConfigUpdate: (POSSimulatorConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ConfigSection(title = "PIN Entry Options", icon = Icons.Default.Dialpad) {
            ConfigDropdown(
                label = "PIN Pad Type",
                currentValue = config.pinEntryOptions,
                options = listOf(
                    "Integrated PIN pad",
                    "External PIN pad",
                    "Soft PIN pad",
                    "No PIN pad"
                ),
                onValueChange = { onConfigUpdate(config.copy(pinEntryOptions = it)) }
            )
        }
        ConfigSection(title = "Card Reader Types", icon = Icons.Default.CreditCard) {
            ConfigDropdown(
                label = "Reader Configuration",
                currentValue = config.cardReaderTypes,
                options = listOf(
                    "Magnetic stripe reader (MSR)",
                    "EMV chip card reader (contact)",
                    "Triple-head reader (MSR + chip + contactless)"
                ),
                onValueChange = { onConfigUpdate(config.copy(cardReaderTypes = it)) }
            )
        }
        ConfigSection(title = "Display Configuration", icon = Icons.Default.DesktopWindows) {
            ConfigDropdown(
                label = "Display Setup",
                currentValue = config.displayConfig,
                options = listOf(
                    "Merchant-only display",
                    "Dual displays (merchant + customer)",
                    "Touch screen capability"
                ),
                onValueChange = { onConfigUpdate(config.copy(displayConfig = it)) }
            )
        }
        ConfigSection(title = "Receipt Printing", icon = Icons.Default.Print) {
            ConfigDropdown(
                label = "Printer Type",
                currentValue = config.receiptPrinting,
                options = listOf(
                    "Thermal receipt printer",
                    "Impact dot-matrix printer",
                    "No printer (electronic receipts only)"
                ),
                onValueChange = { onConfigUpdate(config.copy(receiptPrinting = it)) }
            )
        }
    }
}

@Composable
private fun TransactionTab(
    config: POSSimulatorConfig,
    onConfigUpdate: (POSSimulatorConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ConfigSection(title = "Terminal Capabilities", icon = Icons.Default.Terminal) {
            ConfigMultiSelectChipGroup(
                label = "Processing Modes",
                allOptions = listOf(
                    "Offline transaction processing",
                    "Online-only processing",
                    "Store-and-forward capability",
                    "Void and refund processing",
                    "Partial approval support"
                ),
                selectedOptions = config.terminalCapabilities,
                onSelectionChanged = { onConfigUpdate(config.copy(terminalCapabilities = it)) }
            )
        }
        ConfigSection(title = "Payment Method Support", icon = Icons.Default.Payment) {
            ConfigMultiSelectChipGroup(
                label = "Accepted Methods",
                allOptions = listOf(
                    "Credit card processing",
                    "Debit card processing",
                    "EBT/SNAP benefits",
                    "Gift card processing",
                    "Digital wallet support"
                ),
                selectedOptions = config.paymentMethods.map { it.name }.toSet(),
                onSelectionChanged = { /*onConfigUpdate(config.copy(paymentMethods = PaymentMethod.valueOf(it)))*/ }
            )
        }
    }
}

@Composable
private fun SecurityTab(config: POSSimulatorConfig, onConfigUpdate: (POSSimulatorConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ConfigSection(title = "Encryption and Security", icon = Icons.Default.VpnKey) {
            ConfigMultiSelectChipGroup(
                label = "Security Protocols",
                allOptions = listOf(
                    "End-to-end encryption (E2EE)",
                    "Point-to-point encryption (P2PE)",
                    "Triple DES encryption",
                    "AES encryption",
                    "Token substitution"
                ),
                selectedOptions = config.encryptionSecurity,
                onSelectionChanged = { onConfigUpdate(config.copy(encryptionSecurity = it)) }
            )
        }
        ConfigSection(title = "Authentication Methods", icon = Icons.Default.Fingerprint) {
            ConfigDropdown(
                label = "Cardholder Verification",
                currentValue = config.authMethods,
                options = listOf(
                    "PIN verification",
                    "Signature capture and verification",
                    "Biometric authentication",
                    "No cardholder verification method (No CVM)"
                ),
                onValueChange = { onConfigUpdate(config.copy(authMethods = it)) }
            )
        }
    }
}

@Composable
private fun NetworkSoftwareTab(
    config: POSSimulatorConfig,
    onConfigUpdate: (POSSimulatorConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ConfigSection(title = "Connectivity Options", icon = Icons.Default.Wifi) {
            ConfigDropdown(
                label = "Primary Connection",
                currentValue = config.connectivity,
                options = listOf(
                    "Ethernet/LAN connection",
                    "Wireless/Wi-Fi connectivity",
                    "Cellular/mobile data connection",
                    "Dial-up modem connection"
                ),
                onValueChange = { onConfigUpdate(config.copy(connectivity = it)) }
            )
        }
        ConfigSection(title = "Software and Application", icon = Icons.Default.DeveloperMode) {
            ConfigDropdown(
                label = "Operating System",
                currentValue = config.osType,
                options = listOf("Proprietary OS", "Linux-based", "Android", "Windows CE"),
                onValueChange = { onConfigUpdate(config.copy(osType = it)) }
            )
        }
    }
}


// --- GENERIC CONFIGURATION COMPONENTS ---

@Composable
private fun ConfigSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colors.primary)
            Text(
                title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold
            )
        }
        Card(
            elevation = 0.dp,
            border = BorderStroke(1.dp, BorderLight),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ConfigDropdown(
    label: String,
    currentValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = currentValue,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            readOnly = true,
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    "Dropdown"
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = { onValueChange(option); expanded = false }) {
                    Text(option)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ConfigMultiSelectChipGroup(
    label: String,
    allOptions: List<String>,
    selectedOptions: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.body2)
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allOptions.forEach { option ->
                FilterChip(
                    selected = option in selectedOptions,
                    onClick = {
                        val newSet = selectedOptions.toMutableSet()
                        if (option in newSet) newSet.remove(option) else newSet.add(option)
                        onSelectionChanged(newSet)
                    },
                    leadingIcon = {
                        if (option in selectedOptions) {
                            Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(18.dp))
                        }
                    },
                    colors = ChipDefaults.filterChipColors(
                        selectedBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                        selectedContentColor = MaterialTheme.colors.primary
                    )
                ) {
                    Text(option)
                }
            }

        }
        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.Start).fillMaxWidth(),
            style = themedScrollbarStyle()
        )
    }
}


/**
 * Extension function to set the cursor for horizontal resize
 */
private fun Modifier.cursorForHorizontalResize(): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this.hoverable(interactionSource)
        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
}
