package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.StudioVersion
import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.ui.BorderLight
import `in`.aicortex.iso8583studio.ui.navigation.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.CardType
import `in`.aicortex.iso8583studio.ui.navigation.ConnectionInterface
import `in`.aicortex.iso8583studio.ui.navigation.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.UnifiedSimulatorState
import java.awt.Cursor
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

// --- DATA MODELS & ENUMS ---


enum class ProfileStatus(val color: Color, val icon: ImageVector) {
    Valid(Color(0xFF4CAF50), Icons.Default.CheckCircle),
    Issues(Color(0xFFFFC107), Icons.Default.Warning),
    InUse(Color.Unspecified, Icons.Default.PlayCircleFilled), // Use primary color
    Test(Color(0xFF03A9F4), Icons.Default.Science)
}

enum class ConnectionType {
    PCS, NFC, Serial, Network
}

private enum class ApduSimulatorConfigTabs(val label: String, val icon: ImageVector) {
    BASIC("Basic", Icons.Default.Tune),
    APPLICATIONS("Applications", Icons.Default.Apps),
    FILE_SYSTEM("File System", Icons.Default.Folder),
    CRYPTO("Cryptography", Icons.Default.VpnKey),
    BEHAVIOR("Behavior", Icons.Default.Rule)
}


// --- SCREEN ENTRY POINT (FOR PREVIEW/TESTING) ---

@Composable
fun ApduSimulatorScreen(
    appState: UnifiedSimulatorState,
    onSelectProfile: (APDUSimulatorConfig) -> Unit,
    onAddProfile: () -> Unit,
    onDeleteProfile: () -> Unit,
    onProfileUpdate: (APDUSimulatorConfig) -> Unit,
    onLaunchSimulator: () -> Unit,
    onSaveChanges: () -> Unit
) {


    ApduSimulatorConfigContainer(
        profiles = appState.apduConfigs.value,
        selectedProfile = appState.currentConfig(SimulatorType.APDU)?.let { it as APDUSimulatorConfig },
        onSelectProfile = onSelectProfile,
        onAddProfile = onAddProfile,
        onDeleteProfile = onDeleteProfile,
        onProfileUpdate = onProfileUpdate,
        onLaunchSimulator = onLaunchSimulator,
        onSaveChanges = onSaveChanges
    )
}


// --- MAIN CONFIGURATION CONTAINER ---

@Composable
fun ApduSimulatorConfigContainer(
    profiles: List<APDUSimulatorConfig>,
    selectedProfile: APDUSimulatorConfig?,
    onSelectProfile: (APDUSimulatorConfig) -> Unit,
    onAddProfile: () -> Unit,
    onDeleteProfile: () -> Unit,
    onProfileUpdate: (APDUSimulatorConfig) -> Unit,
    onLaunchSimulator: () -> Unit,
    onSaveChanges: () -> Unit
) {
    val tabs = ApduSimulatorConfigTabs.values().toList()
    var selectedTab by remember(selectedProfile) { mutableStateOf(ApduSimulatorConfigTabs.BASIC) }
    var leftPanelWidth by remember { mutableStateOf(380.dp) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Panel - Profile Management
            ConfigProfilePanel(
                modifier = Modifier.width(leftPanelWidth),
                profiles = profiles,
                selectedProfile = selectedProfile,
                onSelectProfile = onSelectProfile,
                onAddProfile = onAddProfile,
                onDeleteProfile = onDeleteProfile,
                onSaveChanges = onSaveChanges,
                onLaunchSimulator = onLaunchSimulator
            )

            // Resizable Divider
            ResizableDivider { dragAmount ->
                leftPanelWidth = (leftPanelWidth + dragAmount.x.dp).coerceIn(350.dp, 600.dp)
            }

            // Right Panel - Configuration Editor
            ConfigEditorPanel(
                modifier = Modifier.weight(1f),
                selectedProfile = selectedProfile,
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onAddProfile = onAddProfile,
                onProfileUpdate = onProfileUpdate
            )
        }
    }
}

// --- LEFT PANEL & ITS COMPONENTS ---

@Composable
private fun ConfigProfilePanel(
    modifier: Modifier,
    profiles: List<APDUSimulatorConfig>,
    selectedProfile: APDUSimulatorConfig?,
    onSelectProfile: (APDUSimulatorConfig) -> Unit,
    onAddProfile: () -> Unit,
    onDeleteProfile: () -> Unit,
    onSaveChanges: () -> Unit,
    onLaunchSimulator: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxHeight().padding(12.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            ScreenHeader("APDU Simulator", "Configuration Profiles", Icons.Default.SimCard)

            // Profile list
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (profiles.isEmpty()) {
                        item { EmptyProfileListPrompt() }
                    } else {
                        items(profiles, key = { it.id }) { profile ->
                            APDUSimulatorConfigItem(
                                profile = profile,
                                isSelected = profile.id == selectedProfile?.id,
                                onClick = { onSelectProfile(profile) }
                            )
                        }
                    }
                }
            }

            // Management and Connection
            ProfileManagementButtons(
                onAddConfig = onAddProfile,
                onDeleteConfig = onDeleteProfile,
                onSaveChanges = onSaveChanges,
                isDeleteEnabled = selectedProfile != null
            )
            ConnectionStatusCard()


            // Launch Section
            if (selectedProfile != null) {
                LaunchSimulatorCard(
                    configName = selectedProfile.name,
                    onLaunchSimulator = onLaunchSimulator
                )
            }
        }
    }
}

@Composable
private fun EmptyProfileListPrompt() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.AddToQueue, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colors.primary.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No APDU Profiles", style = MaterialTheme.typography.subtitle2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
            Text("Create a profile to get started", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun APDUSimulatorConfigItem(profile: APDUSimulatorConfig, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface
    val contentColor = if (isSelected) Color.White else MaterialTheme.colors.onSurface

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = if (isSelected) 4.dp else 1.dp,
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile.name, fontWeight = FontWeight.Bold, color = contentColor)
                Text(
                    "Type: ${profile.cardType}",
                    style = MaterialTheme.typography.caption,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Text(
                    "Modified: ${SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT).format(Date(profile.modifiedDate))}",
                    style = MaterialTheme.typography.caption,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val statusColor = if(profile.status.color == Color.Unspecified) MaterialTheme.colors.primary else profile.status.color
                Icon(
                    imageVector = profile.status.icon,
                    contentDescription = profile.status.name,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) contentColor else statusColor
                )
                if (profile.isTestProfile) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "Test Profile",
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) contentColor else ProfileStatus.Test.color
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
    onSaveChanges: () -> Unit,
    isDeleteEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAddConfig, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, "Add", modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("New")
            }
            OutlinedButton(onClick = onDeleteConfig, modifier = Modifier.weight(1f), enabled = isDeleteEnabled, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.error)) {
                Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Delete")
            }
        }
        Button(onClick = onSaveChanges, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Save, "Save Changes", modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Save Changes")
        }
    }
}

@Composable
private fun ConnectionStatusCard() {
    var isConnected by remember { mutableStateOf(true) }
    Card(modifier = Modifier.fillMaxWidth(), elevation = 1.dp, border = BorderStroke(1.dp, BorderLight)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (isConnected) Icons.Default.Link else Icons.Default.LinkOff, "Status", tint = if (isConnected) Color.Green else Color.Red)
                Column {
                    Text(if (isConnected) "Connected" else "Disconnected", style = MaterialTheme.typography.body2, fontWeight = FontWeight.Bold)
                    Text("ACR122U Reader 0", style = MaterialTheme.typography.caption)
                }
            }
            TextButton(onClick = { isConnected = !isConnected }) {
                Text(if (isConnected) "Disconnect" else "Connect")
            }
        }
    }
}


// --- RIGHT PANEL & ITS COMPONENTS ---

@Composable
private fun ConfigEditorPanel(
    modifier: Modifier,
    selectedProfile: APDUSimulatorConfig?,
    tabs: List<ApduSimulatorConfigTabs>,
    selectedTab: ApduSimulatorConfigTabs,
    onTabSelected: (ApduSimulatorConfigTabs) -> Unit,
    onAddProfile: () -> Unit,
    onProfileUpdate: (APDUSimulatorConfig) -> Unit
) {
    if (selectedProfile == null) {
        EmptyEditorPanel(modifier, onAddProfile)
        return
    }

    val scrollState = rememberScrollState()
    Card(modifier = modifier.fillMaxHeight().padding(12.dp), elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        text = { Text(tab.label, style = MaterialTheme.typography.caption, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (selectedTab) {
                        ApduSimulatorConfigTabs.BASIC -> BasicConfigTab()
                        ApduSimulatorConfigTabs.APPLICATIONS -> ApplicationAidTab()
                        ApduSimulatorConfigTabs.FILE_SYSTEM -> FileSystemTab()
                        ApduSimulatorConfigTabs.CRYPTO -> CryptoManagementTab()
                        ApduSimulatorConfigTabs.BEHAVIOR -> BehaviorRuleManagementTab()
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyEditorPanel(modifier: Modifier, onAddConfig: () -> Unit) {
    Card(modifier = modifier.fillMaxHeight().padding(12.dp), elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.SimCard, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colors.primary.copy(alpha = 0.5f))
                Text("No Profile Selected", style = MaterialTheme.typography.h6, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                Text("Create or select a profile to begin configuration.", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onAddConfig) {
                    Icon(Icons.Default.Add, "Add", modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Create APDU Profile")
                }
            }
        }
    }
}


// --- TAB IMPLEMENTATIONS ---

@Composable
private fun PlaceholderTab(title: String, description: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
            Text(title, style = MaterialTheme.typography.h6, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f))
            Text(description, style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        }
    }
}


// --- GENERIC & HELPER COMPONENTS ---

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        content()
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.overline,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.primary
    )
}

@Composable
private fun ResizableDivider(onDrag: (dragAmount: Offset) -> Unit) {
    Box(
        modifier = Modifier.width(8.dp).fillMaxHeight().background(Color.Transparent)
            .pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onDrag(dragAmount) } }
            .cursorForHorizontalResize()
    ) {
        Box(
            modifier = Modifier.align(Alignment.Center).width(4.dp).height(48.dp)
                .shadow(1.dp, RoundedCornerShape(2.dp))
                .background(color = BorderLight, shape = RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = MaterialTheme.colors.primary)
        Column {
            Text(title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun LaunchSimulatorCard(configName: String, onLaunchSimulator: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, backgroundColor = MaterialTheme.colors.secondary, shape = RoundedCornerShape(8.dp)) {
        Button(
            onClick = onLaunchSimulator,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent, contentColor = Color.White),
            elevation = ButtonDefaults.elevation(0.dp,0.dp,0.dp,0.dp)
        ) {
            Row(modifier=Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, "Launch", modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier=Modifier.weight(1f)) {
                    Text("Launch Simulator", fontWeight = FontWeight.Bold)
                    Text("Profile: $configName", style = MaterialTheme.typography.caption, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

private fun Modifier.cursorForHorizontalResize(): Modifier = composed {
    pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
}
