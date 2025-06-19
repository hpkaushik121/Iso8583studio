package `in`.aicortex.iso8583studio.ui.screens.hsmSimulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun KeyManagementOverviewTab(
    hsmConfig: HSMSimulatorConfig,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Generate sample data based on configuration
    val statistics = remember { generateKeyStatistics(hsmConfig) }
    val recentOperations = remember { generateRecentOperations() }
    val managedKeys = remember { generateManagedKeys() }

    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Statistics Overview Section
        KeyStatisticsSection(
            statistics = statistics,
            hsmConfig = hsmConfig
        )

        // Recent Operations Section
        KeyOperationsSection(
            operations = recentOperations
        )

        // Key Tracking Section
        KeyTrackingSection(
            keys = managedKeys,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
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