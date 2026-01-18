package `in`.aicortex.iso8583studio.ui.screens.hsmSimulator

import ai.cortex.core.IsoUtil
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.*
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.HsmServiceImpl
import `in`.aicortex.iso8583studio.hsm.HsmSimulator
import `in`.aicortex.iso8583studio.hsm.payshield10k.HsmSlotManager
import `in`.aicortex.iso8583studio.hsm.payshield10k.LmkSlot
import `in`.aicortex.iso8583studio.hsm.payshield10k.SlotStatus
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkSet

// Data Models
data class KeyStatistics(
    val totalKeys: Int,
    val activeKeys: Int,
    val expiredKeys: Int,
    val expiringSoon: Int,
    val revokedKeys: Int,
    val storageUsedPercent: Double,
    val algorithmBreakdown: Map<String, Int>,
    val hierarchyBreakdown: Map<String, Int>
)

data class KeyOperation(
    val id: String,
    val keyName: String,
    val operation: String,
    val timestamp: Long,
    val user: String,
    val status: String,
    val details: String
)

data class ManagedKey(
    val id: String,
    val name: String,
    val algorithm: String,
    val keySize: Int,
    val status: String,
    val createdDate: Long,
    val expirationDate: Long?,
    val usageCount: Int,
    val location: String,
    val isBackedUp: Boolean
)

// ═══════════════════════════════════════════════════════════════════════════════════
// THEME COLORS - Matching ISO8583Studio Design System
// ═══════════════════════════════════════════════════════════════════════════════════

object HsmKeyTheme {
    val PrimaryBlue = Color(0xFF42A5F5)
    val SecurityPurple = Color(0xFF8E24AA)
    val SuccessGreen = Color(0xFF43A047)
    val WarningOrange = Color(0xFFFF9800)
    val ErrorRed = Color(0xFFF44336)
    val InfoCyan = Color(0xFF00ACC1)

    val KeySlotActive = Color(0xFF4CAF50)
    val KeySlotEmpty = Color(0xFF9E9E9E)
    val KeySlotDefault = Color(0xFF2196F3)
    val KeySlotManagement = Color(0xFF9C27B0)

    val LmkGradientStart = Color(0xFF5E35B1)
    val LmkGradientEnd = Color(0xFF8E24AA)
}

// ═══════════════════════════════════════════════════════════════════════════════════
// DATA MODELS
// ═══════════════════════════════════════════════════════════════════════════════════

data class LmkDisplayPair(
    val pairNumber: String,
    val leftKey: String,
    val rightKey: String,
    val combined: String,
    val kcv: String
)

data class UserStorageInfo(
    val index: String,
    val keyType: String,
    val algorithm: String,
    val status: String,
    val createdAt: Long,
    val lastAccessed: Long
)

data class KeyOperationLog(
    val timestamp: Long,
    val operation: String,
    val slotId: String,
    val status: String,
    val details: String
)

// ═══════════════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun KeyManagementOverviewTab(
    hsm: HsmServiceImpl,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(KeySection.OVERVIEW) }
    var selectedLmkSlot by remember { mutableStateOf<String?>(null) }
    var showRegenerateDialog by remember { mutableStateOf(false) }

    // Load LMK data from storage
    val loadedLmks by remember {
        mutableStateOf(hsm.configuration.hsmConfig.lmkStorage)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Selector
        SectionSelector(
            selectedSection = selectedSection,
            onSectionSelected = { selectedSection = it }
        )

        // Content based on selected section
        when (selectedSection) {
            KeySection.OVERVIEW -> {
                OverviewSection(
                    hsm = hsm.getHsm(),
                    loadedLmks = loadedLmks.liveLmks,
                    onSlotClick = { slotId ->
                        selectedLmkSlot = slotId
                        selectedSection = KeySection.LMK_DETAILS
                    }
                )
            }
            KeySection.LMK_DETAILS -> {
                LmkDetailsSection(
                    loadedLmks = loadedLmks.liveLmks,
                    selectedSlot = selectedLmkSlot,
                    onBackClick = { selectedSection = KeySection.OVERVIEW },
                    onRegenerateClick = { showRegenerateDialog = true }
                )
            }
            KeySection.USER_STORAGE -> {
                UserStorageSection(hsmConfig = hsm.configuration)
            }
            KeySection.OPERATIONS -> {
                OperationsLogSection()
            }
        }
    }

    // Regenerate Dialog
    if (showRegenerateDialog) {
        RegenerateLmkDialog(
            slotId = selectedLmkSlot ?: "00",
            onDismiss = { showRegenerateDialog = false },
            onConfirm = { slotId, params ->
                // TODO: Implement regeneration
                showRegenerateDialog = false
            }
        )
    }
}

enum class KeySection(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.Dashboard),
    LMK_DETAILS("LMK Details", Icons.Default.VpnKey),
    USER_STORAGE("User Storage", Icons.Default.Storage),
    OPERATIONS("Operations Log", Icons.Default.History)
}

// ═══════════════════════════════════════════════════════════════════════════════════
// SECTION SELECTOR
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionSelector(
    selectedSection: KeySection,
    onSectionSelected: (KeySection) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeySection.values().forEach { section ->
                SectionButton(
                    section = section,
                    isSelected = selectedSection == section,
                    onClick = { onSectionSelected(section) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SectionButton(
    section: KeySection,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected)
                MaterialTheme.colors.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colors.surface.copy(alpha = 0.3f),
            contentColor = if (isSelected)
                MaterialTheme.colors.primary
            else
                MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.elevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                section.title,
                style = MaterialTheme.typography.caption,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// OVERVIEW SECTION
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun OverviewSection(
    hsm: HsmSimulator?,
    loadedLmks: Map<String, LmkSet>,
    onSlotClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Statistics Cards
        StatisticsRow(slotManager = hsm!!.getFeatures().getSlotManager())

        // LMK Slots Grid
        LmkSlotsGrid(
            slotManager = hsm!!.getFeatures().getSlotManager(),
            onSlotClick = onSlotClick
        )

        // Quick Actions
        QuickActionsCard()
    }
}

@Composable
private fun StatisticsRow(
    slotManager: HsmSlotManager
) {
    /*
    lmkLiveSlots
lmkKeyChangeSlots
userStorageSlots
maxLmkSlots
maxUserStorageSlots
storageMode
totalReads
totalWrites
totalDeletes
lmkUtilization
userStorageUtilization
     */
    val totalSlots = 99
    val activeSlots = slotManager.getStatistics()["lmkLiveSlots"] as Int
    val emptySlots = totalSlots - activeSlots

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Total Slots",
            value = totalSlots.toString(),
            icon = Icons.Default.GridView,
            color = HsmKeyTheme.InfoCyan,
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Loaded",
            value = activeSlots.toString(),
            icon = Icons.Default.CheckCircle,
            color = HsmKeyTheme.SuccessGreen,
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Empty",
            value = emptySlots.toString(),
            icon = Icons.Default.Circle,
            color = HsmKeyTheme.KeySlotEmpty,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 2.dp,
        backgroundColor = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                value,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LmkSlotsGrid(
    slotManager: HsmSlotManager,
    onSlotClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp)
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
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = HsmKeyTheme.SecurityPurple,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "LMK Slot Map",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    "${slotManager.lmkLiveSlots.size} / ${slotManager.maxLmkSlots} Loaded",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                modifier = Modifier.height(400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(slotManager.lmkLiveSlots.entries.sortedBy { it.key }.toList()) { (slotId, slotData) ->
                    LmkSlotItem(
                        slotId = slotId,
                        slotData = slotData,
                        onClick = { onSlotClick(slotId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LmkSlotItem(
    slotId: String,
    slotData: LmkSlot,
    onClick: () -> Unit
) {
    val color = when {
        slotData.isDefault -> HsmKeyTheme.KeySlotDefault
        slotData.isManagement -> HsmKeyTheme.KeySlotManagement
        else -> HsmKeyTheme.KeySlotActive
    }

    Card(
        modifier = Modifier
            .size(72.dp)
            .clickable(onClick = onClick),
        elevation = 4.dp,
        backgroundColor = color.copy(alpha =  0.2f ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Status Icon
                Icon(
                    imageVector = when {
                        slotData.isDefault -> Icons.Default.Star
                        slotData.isManagement -> Icons.Default.Settings
                        else -> Icons.Default.VpnKey
                    },
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Slot ID
                Text(
                    slotId,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                // Pair count badge
                Text(
                    "${slotData.lmkSet?.pairs?.size}p",
                    style = MaterialTheme.typography.caption,
                    fontSize = 9.sp,
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    title = "Generate LMK",
                    icon = Icons.Default.Add,
                    color = HsmKeyTheme.SuccessGreen,
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    title = "Export Keys",
                    icon = Icons.Default.Upload,
                    color = HsmKeyTheme.InfoCyan,
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    title = "Import Keys",
                    icon = Icons.Default.Download,
                    color = HsmKeyTheme.WarningOrange,
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    title = "Backup All",
                    icon = Icons.Default.Backup,
                    color = HsmKeyTheme.SecurityPurple,
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = color.copy(alpha = 0.1f),
            contentColor = color
        ),
        elevation = ButtonDefaults.elevation(0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                title,
                style = MaterialTheme.typography.caption,
                fontSize = 10.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// LMK DETAILS SECTION
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun LmkDetailsSection(
    loadedLmks: Map<String, LmkSet>,
    selectedSlot: String?,
    onBackClick: () -> Unit,
    onRegenerateClick: () -> Unit
) {
    val slotData = selectedSlot?.let { loadedLmks[it] }

    if (slotData == null) {
        EmptyState(message = "No slot selected")
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button
        LmkDetailsHeader(
            slotId = selectedSlot,
            slotData = slotData,
            onBackClick = onBackClick,
            onRegenerateClick = onRegenerateClick
        )

        // Slot Information Card
        SlotInformationCard(slotData = slotData)

        // LMK Pairs Display
        LmkPairsCard(slotData = slotData)
    }
}

@Composable
private fun LmkDetailsHeader(
    slotId: String,
    slotData: LmkSet,
    onBackClick: () -> Unit,
    onRegenerateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colors.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    HsmKeyTheme.LmkGradientStart,
                                    HsmKeyTheme.LmkGradientEnd
                                )
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        "LMK Slot $slotId",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Chip(
                            text = "Loaded",
                            color =  HsmKeyTheme.KeySlotActive
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onRegenerateClick,
                    modifier = Modifier
                        .background(
                            HsmKeyTheme.WarningOrange.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Regenerate",
                        tint = HsmKeyTheme.WarningOrange
                    )
                }

                IconButton(
                    onClick = { /* TODO: Export */ },
                    modifier = Modifier
                        .background(
                            HsmKeyTheme.InfoCyan.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Export",
                        tint = HsmKeyTheme.InfoCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.caption,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SlotInformationCard(
    slotData: LmkSet
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Slot Information",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))

            InfoRow("Slot ID", slotData.identifier)
            InfoRow("Status", "Loaded")
            InfoRow("Scheme", slotData.scheme)
            InfoRow("Pair Count", "${slotData.pairs.size} pairs")

            if (slotData.checkValue.isNotEmpty()) {
                InfoRow("Check Value", slotData.checkValue)
            }

            InfoRow("Created At", formatTimestamp(slotData.createdAt))
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LmkPairsCard(
    slotData: LmkSet
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp)
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
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = HsmKeyTheme.SecurityPurple,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "LMK Key Pairs (Hexadecimal)",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${slotData.pairs.size} pairs",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))

            LazyColumn(
                modifier = Modifier.height(500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(slotData.pairs.entries.sortedBy { it.key }.toList()) { (pairNum, pair) ->
                    LmkPairItem(
                        pairNumber = "$pairNum",
                        leftKey = IsoUtil.bytesToHex(pair.leftKey),
                        rightKey = IsoUtil.bytesToHex(pair.rightKey),
                        combinedKey = IsoUtil.bytesToHex(pair.getCombinedKey())
                    )
                }
            }
        }
    }
}

@Composable
private fun LmkPairItem(
    pairNumber: String,
    leftKey: String,
    combinedKey: String,
    rightKey: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = 1.dp,
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                HsmKeyTheme.SecurityPurple.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            pairNumber,
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = HsmKeyTheme.SecurityPurple
                        )
                    }

                    Column {
                        Text(
                            "Pair #$pairNumber",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "128-bit DES Keys",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colors.surface.copy(alpha = 0.8f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyValueDisplay(
                        label = "Left Key",
                        value = leftKey,
                        color = HsmKeyTheme.InfoCyan
                    )

                    KeyValueDisplay(
                        label = "Right Key",
                        value = rightKey,
                        color = HsmKeyTheme.WarningOrange
                    )

                    KeyValueDisplay(
                        label = "Combined",
                        value = combinedKey,
                        color = HsmKeyTheme.SecurityPurple
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyValueDisplay(
    label: String,
    value: String,
    color: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = color,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp
        )

        SelectionContainer {
            Text(
                value,
                style = MaterialTheme.typography.body2,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// USER STORAGE SECTION
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun UserStorageSection(
    hsmConfig: HSMSimulatorConfig
) {
    val scrollState = rememberScrollState()

    // Sample user storage data
    val userStorage = remember {
        generateSampleUserStorage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Storage Statistics
        UserStorageStats(userStorage = userStorage)

        // User Storage Items
        UserStorageList(userStorage = userStorage)
    }
}

@Composable
private fun UserStorageStats(
    userStorage: List<UserStorageInfo>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "User Storage Statistics",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Keys",
                    value = userStorage.size.toString(),
                    icon = Icons.Default.Storage,
                    color = HsmKeyTheme.InfoCyan,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Active",
                    value = userStorage.count { it.status == "Active" }.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = HsmKeyTheme.SuccessGreen,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Working Keys",
                    value = userStorage.count { it.keyType.contains("ZPK") || it.keyType.contains("TPK") }.toString(),
                    icon = Icons.Default.Key,
                    color = HsmKeyTheme.WarningOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun UserStorageList(
    userStorage: List<UserStorageInfo>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Stored Keys",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))

            LazyColumn(
                modifier = Modifier.height(500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(userStorage) { storage ->
                    UserStorageItem(storage = storage)
                }
            }
        }
    }
}

@Composable
private fun UserStorageItem(
    storage: UserStorageInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        HsmKeyTheme.InfoCyan.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = HsmKeyTheme.InfoCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        storage.keyType,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Medium
                    )

                    Chip(
                        text = storage.status,
                        color = if (storage.status == "Active") HsmKeyTheme.SuccessGreen else HsmKeyTheme.KeySlotEmpty
                    )
                }

                Text(
                    "Index: ${storage.index} • ${storage.algorithm}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    formatTimestamp(storage.createdAt),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// OPERATIONS LOG SECTION
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun OperationsLogSection() {
    val scrollState = rememberScrollState()
    val operations = remember { generateOperationLogs() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp,
            backgroundColor = MaterialTheme.colors.surface,
            shape = RoundedCornerShape(12.dp)
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
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = HsmKeyTheme.InfoCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Recent Operations",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${operations.size} operations",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))

                LazyColumn(
                    modifier = Modifier.height(500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(operations) { operation ->
                        OperationLogItem(operation = operation)
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationLogItem(
    operation: KeyOperationLog
) {
    val (icon, color) = getOperationStyle(operation.operation)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        operation.operation,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Medium
                    )

                    Chip(
                        text = operation.status,
                        color = if (operation.status == "Success") HsmKeyTheme.SuccessGreen else HsmKeyTheme.ErrorRed
                    )
                }

                Text(
                    "Slot: ${operation.slotId}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    operation.details,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }

            Text(
                formatRelativeTime(operation.timestamp),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// REGENERATE DIALOG
// ═══════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun RegenerateLmkDialog(
    slotId: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Map<String, Any>) -> Unit
) {
    var selectedScheme by remember { mutableStateOf("VARIANT") }
    var selectedAlgorithm by remember { mutableStateOf("3DES(2key)") }
    var selectedStatus by remember { mutableStateOf("Test") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = HsmKeyTheme.WarningOrange
                )
                Text("Regenerate LMK - Slot $slotId")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "This will generate a new LMK and replace the existing one. This operation cannot be undone.",
                    style = MaterialTheme.typography.body2,
                    color = HsmKeyTheme.WarningOrange
                )

                // Scheme selector
                Text("Scheme:", style = MaterialTheme.typography.subtitle2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("VARIANT", "KEY_BLOCK").forEach { scheme ->
                        FilterChip(
                            selected = selectedScheme == scheme,
                            onClick = { selectedScheme = scheme },
                            content = { Text(scheme) },

                        )
                    }
                }

                // Algorithm selector
                Text("Algorithm:", style = MaterialTheme.typography.subtitle2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("3DES(2key)", "3DES(3key)", "AES-256").forEach { algo ->
                        FilterChip(
                            selected = selectedAlgorithm == algo,
                            onClick = { selectedAlgorithm = algo },
                            content = { Text(algo) }
                        )
                    }
                }

                // Status selector
                Text("Status:", style = MaterialTheme.typography.subtitle2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Test", "Live").forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            content = { Text(status) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val params = mapOf(
                        "scheme" to selectedScheme,
                        "algorithm" to selectedAlgorithm,
                        "status" to selectedStatus
                    )
                    onConfirm(slotId, params)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = HsmKeyTheme.WarningOrange
                )
            ) {
                Text("Regenerate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════
// HELPER COMPOSABLES & FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )
            Text(
                message,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}



private fun ByteArray.toHexString(): String {
    return joinToString("") { String.format("%02X", it) }
}

private fun generateSampleUserStorage(): List<UserStorageInfo> {
    val keyTypes = listOf("ZPK", "TPK", "CVK", "TAK", "BDK", "PVK")
    val algorithms = listOf("3DES", "AES-128", "AES-256")
    val statuses = listOf("Active", "Inactive", "Expired")

    return (0..15).map { i ->
        UserStorageInfo(
            index = String.format("%03X", i * 16),
            keyType = keyTypes.random(),
            algorithm = algorithms.random(),
            status = statuses.random(),
            createdAt = System.currentTimeMillis() - (i * 86400000L),
            lastAccessed = System.currentTimeMillis() - (i * 3600000L)
        )
    }
}

private fun generateOperationLogs(): List<KeyOperationLog> {
    val operations = listOf(
        "LMK Generated", "Key Loaded", "Key Rotated", "Key Exported",
        "Key Deleted", "Backup Created", "Slot Authorized"
    )
    val statuses = listOf("Success", "Failed", "Pending")

    return (0..20).map { i ->
        KeyOperationLog(
            timestamp = System.currentTimeMillis() - (i * 1800000L),
            operation = operations.random(),
            slotId = String.format("%02d", (0..39).random()),
            status = statuses.random(),
            details = "Operation completed ${if (i % 3 == 0) "successfully" else "with warnings"}"
        )
    }.sortedByDescending { it.timestamp }
}

private fun getOperationStyle(operation: String): Pair<ImageVector, Color> {
    return when {
        operation.contains("Generated") -> Icons.Default.Add to HsmKeyTheme.SuccessGreen
        operation.contains("Loaded") -> Icons.Default.Upload to HsmKeyTheme.InfoCyan
        operation.contains("Rotated") -> Icons.Default.Autorenew to HsmKeyTheme.WarningOrange
        operation.contains("Exported") -> Icons.Default.Download to HsmKeyTheme.PrimaryBlue
        operation.contains("Deleted") -> Icons.Default.Delete to HsmKeyTheme.ErrorRed
        operation.contains("Backup") -> Icons.Default.Backup to HsmKeyTheme.SecurityPurple
        else -> Icons.Default.Info to Color.Gray
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}



@Composable
private fun KeyStatisticsSection(
    statistics: KeyStatistics,
    hsmConfig: HSMSimulatorConfig
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colors.primary
                )
                Column {
                    Text(
                        "Key Statistics Overview",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${hsmConfig.vendor.displayName} ${hsmConfig.model}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Key Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticCard(
                    title = "Total Keys",
                    value = statistics.totalKeys.toString(),
                    icon = Icons.Default.VpnKey,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = "Active",
                    value = statistics.activeKeys.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = "Expiring",
                    value = statistics.expiringSoon.toString(),
                    icon = Icons.Default.Warning,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = "Expired",
                    value = statistics.expiredKeys.toString(),
                    icon = Icons.Default.Error,
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
            }

            // Storage Utilization
            StorageUtilizationCard(
                usedPercent = statistics.storageUsedPercent,
                totalKeys = statistics.totalKeys,
                maxKeys = hsmConfig.keyManagement.keyStoreConfig.maxKeys
            )

            // Algorithm and Hierarchy Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BreakdownCard(
                    title = "By Algorithm",
                    data = statistics.algorithmBreakdown,
                    modifier = Modifier.weight(1f)
                )

                BreakdownCard(
                    title = "By Hierarchy",
                    data = statistics.hierarchyBreakdown,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatisticCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 0.dp,
        backgroundColor = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Text(
                value,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StorageUtilizationCard(
    usedPercent: Double,
    totalKeys: Int,
    maxKeys: Int
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Storage Utilization",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$totalKeys / $maxKeys keys",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            LinearProgressIndicator(
                progress = (usedPercent / 100).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    usedPercent > 90 -> Color(0xFFF44336)
                    usedPercent > 75 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colors.primary
                }
            )

            Text(
                "${String.format("%.1f", usedPercent)}% Used",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BreakdownCard(
    title: String,
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                fontWeight = FontWeight.Medium
            )

            if (data.isEmpty()) {
                Text(
                    "No data available",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                data.entries.take(4).forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            key,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            value.toString(),
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyOperationsSection(
    operations: List<KeyOperation>
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
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Recent Key Operations",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${operations.size} operations",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            if (operations.isEmpty()) {
                EmptyStateMessage(
                    icon = Icons.Default.History,
                    message = "No recent operations"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(operations) { operation ->
                        OperationItem(operation = operation)
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationItem(
    operation: KeyOperation
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Operation Icon
            val (icon, color) = getOperationIconAndColor(operation.operation)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
            }

            // Operation Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        operation.operation,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Medium
                    )

                    StatusBadge(status = operation.status)
                }

                Text(
                    "Key: ${operation.keyName}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )

                if (operation.details.isNotEmpty()) {
                    Text(
                        operation.details,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }

            // Timestamp and User
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    formatRelativeTime(operation.timestamp),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    operation.user,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, backgroundColor) = when (status.lowercase()) {
        "success" -> Color(0xFF4CAF50) to Color(0xFF4CAF50).copy(alpha = 0.2f)
        "failed" -> Color(0xFFF44336) to Color(0xFFF44336).copy(alpha = 0.2f)
        "pending" -> Color(0xFF2196F3) to Color(0xFF2196F3).copy(alpha = 0.2f)
        else -> Color(0xFF9E9E9E) to Color(0xFF9E9E9E).copy(alpha = 0.2f)
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            status,
            style = MaterialTheme.typography.caption,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun KeyTrackingSection(
    keys: List<ManagedKey>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
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
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    "Key Tracking",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Tab Selection
            val tabs = listOf("All", "Active", "Expiring", "Expired")
            TabRow(
                selectedTabIndex = selectedTab,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        text = {
                            Text(
                                title,
                                style = MaterialTheme.typography.caption,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Filtered Keys
            val filteredKeys = filterKeysByTab(keys, selectedTab)

            if (filteredKeys.isEmpty()) {
                EmptyStateMessage(
                    icon = Icons.Default.VpnKey,
                    message = "No keys found"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredKeys) { key ->
                        KeyItem(key = key)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyItem(key: ManagedKey) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Key Header
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
                        imageVector = getKeyStatusIcon(key.status),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = getKeyStatusColor(key.status)
                    )

                    Column {
                        Text(
                            key.name,
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${key.algorithm} ${key.keySize}-bit",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                StatusBadge(status = key.status)
            }

            // Key Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KeyDetailColumn(
                    label = "Created",
                    value = formatDate(key.createdDate)
                )

                if (key.expirationDate != null) {
                    KeyDetailColumn(
                        label = "Expires",
                        value = formatDate(key.expirationDate),
                        isWarning = isExpiringSoon(key.expirationDate)
                    )
                }

                KeyDetailColumn(
                    label = "Usage",
                    value = key.usageCount.toString()
                )

                KeyDetailColumn(
                    label = "Location",
                    value = key.location
                )
            }

            // Additional Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (key.isBackedUp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backed up",
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Text(
                            "Backed up",
                            style = MaterialTheme.typography.caption,
                            color = Color(0xFF4CAF50),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyDetailColumn(
    label: String,
    value: String,
    isWarning: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Text(
            value,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            color = if (isWarning) Color(0xFFFF9800) else MaterialTheme.colors.onSurface,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EmptyStateMessage(
    icon: ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
            Text(
                message,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// Helper Functions
private fun generateKeyStatistics(hsmConfig: HSMSimulatorConfig): KeyStatistics {
    val totalKeys = (50..150).random()
    val activeKeys = (totalKeys * 0.7).toInt()
    val expiredKeys = (totalKeys * 0.1).toInt()
    val expiringSoon = (totalKeys * 0.15).toInt()
    val revokedKeys = totalKeys - activeKeys - expiredKeys - expiringSoon

    return KeyStatistics(
        totalKeys = totalKeys,
        activeKeys = activeKeys,
        expiredKeys = expiredKeys,
        expiringSoon = expiringSoon,
        revokedKeys = revokedKeys,
        storageUsedPercent = (totalKeys.toDouble() / hsmConfig.keyManagement.keyStoreConfig.maxKeys) * 100,
        algorithmBreakdown = mapOf(
            "AES" to (totalKeys * 0.4).toInt(),
            "RSA" to (totalKeys * 0.3).toInt(),
            "ECC" to (totalKeys * 0.2).toInt(),
            "3DES" to (totalKeys * 0.1).toInt()
        ),
        hierarchyBreakdown = mapOf(
            "Data Keys" to (totalKeys * 0.4).toInt(),
            "Session Keys" to (totalKeys * 0.3).toInt(),
            "KEK" to (totalKeys * 0.2).toInt(),
            "Master Keys" to (totalKeys * 0.1).toInt()
        )
    )
}

private fun generateRecentOperations(): List<KeyOperation> {
    val operations = listOf("Generated", "Rotated", "Expired", "Backed up", "Accessed", "Revoked")
    val statuses = listOf("Success", "Failed", "Pending")
    val users = listOf("admin", "crypto_officer", "operator", "system")

    return (1..15).map { index ->
        KeyOperation(
            id = "op_$index",
            keyName = "Key_${(1..100).random()}",
            operation = operations.random(),
            timestamp = System.currentTimeMillis() - (0..86400000).random(),
            user = users.random(),
            status = statuses.random(),
            details = "Operation completed successfully"
        )
    }.sortedByDescending { it.timestamp }
}

private fun generateManagedKeys(): List<ManagedKey> {
    val algorithms = listOf("AES", "RSA", "ECC", "3DES")
    val keySizes = mapOf("AES" to listOf(128, 256), "RSA" to listOf(2048, 4096), "ECC" to listOf(256), "3DES" to listOf(168))
    val statuses = listOf("Active", "Expired", "Expiring", "Revoked")
    val locations = listOf("HSM Slot 1", "HSM Slot 2", "HSM Slot 3", "Software Store")

    return (1..25).map { index ->
        val algorithm = algorithms.random()
        val currentTime = System.currentTimeMillis()
        val createdDate = currentTime - (0..31536000000L).random() // Last year
        val expirationDate = createdDate + (31536000000L..63072000000L).random() // 1-2 years from creation

        ManagedKey(
            id = "key_$index",
            name = "Key_${String.format("%03d", index)}",
            algorithm = algorithm,
            keySize = keySizes[algorithm]?.random() ?: 256,
            status = statuses.random(),
            createdDate = createdDate,
            expirationDate = expirationDate,
            usageCount = (0..1000).random(),
            location = locations.random(),
            isBackedUp = (0..1).random() == 1
        )
    }
}

private fun filterKeysByTab(keys: List<ManagedKey>, tabIndex: Int): List<ManagedKey> {
    return when (tabIndex) {
        1 -> keys.filter { it.status.equals("Active", ignoreCase = true) }
        2 -> keys.filter { it.status.equals("Expiring", ignoreCase = true) }
        3 -> keys.filter { it.status.equals("Expired", ignoreCase = true) }
        else -> keys
    }
}

private fun getOperationIconAndColor(operation: String): Pair<ImageVector, Color> {
    return when (operation.lowercase()) {
        "generated" -> Icons.Default.Add to Color(0xFF4CAF50)
        "rotated" -> Icons.Default.Autorenew to Color(0xFF2196F3)
        "expired" -> Icons.Default.Schedule to Color(0xFFFF9800)
        "revoked" -> Icons.Default.Block to Color(0xFFF44336)
        "backed up" -> Icons.Default.Backup to Color(0xFF9C27B0)
        "accessed" -> Icons.Default.Visibility to Color(0xFF00BCD4)
        else -> Icons.Default.Info to Color(0xFF9E9E9E)
    }
}

private fun getKeyStatusIcon(status: String): ImageVector {
    return when (status.lowercase()) {
        "active" -> Icons.Default.CheckCircle
        "expired" -> Icons.Default.Error
        "expiring" -> Icons.Default.Warning
        "revoked" -> Icons.Default.Block
        else -> Icons.Default.VpnKey
    }
}

@Composable
private fun getKeyStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "active" -> Color(0xFF4CAF50)
        "expired" -> Color(0xFFF44336)
        "expiring" -> Color(0xFFFF9800)
        "revoked" -> Color(0xFF9E9E9E)
        else -> MaterialTheme.colors.onSurface
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
}

private fun isExpiringSoon(expirationDate: Long): Boolean {
    val now = System.currentTimeMillis()
    val thirtyDays = 30 * 24 * 60 * 60 * 1000L
    return expirationDate <= now + thirtyDays
}