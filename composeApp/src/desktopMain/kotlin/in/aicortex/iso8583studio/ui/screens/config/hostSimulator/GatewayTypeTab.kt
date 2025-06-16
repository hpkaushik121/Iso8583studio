package `in`.aicortex.iso8583studio.ui.screens.config.hostSimulator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.data.model.TransmissionType
import `in`.aicortex.iso8583studio.domain.utils.Utils
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PrimaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SecondaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SectionHeader
import `in`.aicortex.iso8583studio.ui.screens.components.StyledTextField
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentChip

// Response delay modes
private enum class ResponseDelay(val displayName: String, val delayMs: Long) {
    INSTANT("Instant", 0),
    REALISTIC("Realistic", 500),
    SLOW("Slow Network", 2000),
    CUSTOM("Custom", -1)
}

/**
 * Host Simulator Gateway Type Configuration Tab
 * Focused on host simulation specific settings
 */
@Composable
fun GatewayTypeTab(config: GatewayConfig, onConfigChange: (GatewayConfig) -> Unit) {
    var responseDelay by remember { mutableStateOf(ResponseDelay.REALISTIC) }
    var customDelayMs by remember { mutableStateOf("1000") }
    var maxConcurrentTransactions by remember { mutableStateOf("50") }
    var enableLogging by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Gateway Type Selection
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = "Gateway Type",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    SectionHeader(title = "GATEWAY TYPE")
                }

                Text(
                    "Select how the host simulator will operate in your network",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GatewayTypeOption(
                        selected = config.gatewayType == GatewayType.SERVER,
                        onSelect = {
                            onConfigChange(
                                config.copy(
                                    gatewayType = GatewayType.SERVER,
                                    transmissionType = TransmissionType.SYNCHRONOUS
                                )
                            )
                        },
                        title = "Server",
                        description = "Accept incoming connections",
                        icon = Icons.Default.Computer,
                        modifier = Modifier.weight(1f)
                    )

                    GatewayTypeOption(
                        selected = config.gatewayType == GatewayType.CLIENT,
                        onSelect = {
                            onConfigChange(
                                config.copy(
                                    gatewayType = GatewayType.CLIENT,
                                    transmissionType = TransmissionType.SYNCHRONOUS
                                )
                            )
                        },
                        title = "Client",
                        description = "Connect to external host",
                        icon = Icons.Default.SettingsInputComponent,
                        modifier = Modifier.weight(1f),
                        isUnderDevelopment = true
                    )

                    GatewayTypeOption(
                        selected = config.gatewayType == GatewayType.PROXY,
                        onSelect = {
                            onConfigChange(config.copy(gatewayType = GatewayType.PROXY))
                        },
                        title = "Proxy",
                        description = "Bridge between systems",
                        icon = Icons.Default.Router,
                        modifier = Modifier.weight(1f),
                        isUnderDevelopment = true
                    )
                }
            }
        }

        // Basic Configuration
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuration",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    SectionHeader(title = "BASIC CONFIGURATION")
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Simulator Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Simulator Name",
                            modifier = Modifier.width(140.dp),
                            fontWeight = FontWeight.Medium
                        )
                        StyledTextField(
                            value = config.name,
                            onValueChange = { onConfigChange(config.copy(name = it)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Description
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "Description",
                            modifier = Modifier.width(140.dp).padding(top = 16.dp),
                            fontWeight = FontWeight.Medium
                        )
                        StyledTextField(
                            value = config.description,
                            onValueChange = { onConfigChange(config.copy(description = it)) },
                            modifier = Modifier.weight(1f),
                            singleLine = false,
                            maxLines = 3
                        )
                    }

                    // Created Date (Read-only)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Created Date",
                            modifier = Modifier.width(140.dp),
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            border = ButtonDefaults.outlinedBorder,
                            color = MaterialTheme.colors.surface.copy(alpha = 0.5f)
                        ) {
                            Text(
                                Utils.formatDate(config.createDate),
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Response Configuration
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Response Settings",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    SectionHeader(title = "RESPONSE SETTINGS")
                }

                // Response Delay
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Response Delay",
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResponseDelay.values().forEach { delay ->
                            FilterChip(
                                onClick = { responseDelay = delay },
                                selected = responseDelay == delay,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    delay.displayName,
                                    style = MaterialTheme.typography.caption
                                )
                            }
                        }
                    }

                    if (responseDelay == ResponseDelay.CUSTOM) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Custom Delay (ms)",
                                modifier = Modifier.width(140.dp),
                                fontWeight = FontWeight.Medium
                            )
                            StyledTextField(
                                value = customDelayMs,
                                onValueChange = { customDelayMs = it },
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }

                // Max Concurrent Transactions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Max Concurrent",
                        modifier = Modifier.width(140.dp),
                        fontWeight = FontWeight.Medium
                    )
                    StyledTextField(
                        value = maxConcurrentTransactions,
                        onValueChange = { maxConcurrentTransactions = it },
                        modifier = Modifier.width(120.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Maximum parallel transactions",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Advanced Options
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Advanced Options",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    SectionHeader(title = "ADVANCED OPTIONS")
                }

                // Enable detailed logging
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableLogging,
                        onCheckedChange = { enableLogging = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colors.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Enable Detailed Logging",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Log all transactions and responses for debugging",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Auto-start checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = config.enable,
                        onCheckedChange = { onConfigChange(config.copy(enable = it)) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colors.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Auto-start Simulator",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Start this simulator when application launches",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Simulator Actions
        Panel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Simulator Tools",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    SectionHeader(title = "SIMULATOR TOOLS")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                        text = "Test Connection",
                        onClick = { /* Test connection logic */ },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Wifi
                    )

                    SecondaryButton(
                        text = "Validate Config",
                        onClick = { /* Validate configuration logic */ },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle
                    )

                    PrimaryButton(
                        text = "Preview Response",
                        onClick = { /* Preview response logic */ },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Preview
                    )
                }

                // Status information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Configuration ready. Use 'Launch Simulator' to start accepting connections.",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GatewayTypeOption(
    selected: Boolean,
    onSelect: () -> Unit,
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isUnderDevelopment: Boolean = false
) {
    Surface(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = if (selected) 2.dp else 0.dp,
        border = ButtonDefaults.outlinedBorder,
        color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface
    ) {
        Box {
            if (isUnderDevelopment) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    UnderDevelopmentChip(
                        status = DevelopmentStatus.UNDER_DEVELOPMENT,
                        modifier = Modifier
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) Color.White else MaterialTheme.colors.primary,
                    modifier = Modifier.size(32.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) Color.White else MaterialTheme.colors.onSurface
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.caption,
                    color = if (selected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Extension for FilterChip (if not available in your Material version)
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FilterChip(
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        border = ButtonDefaults.outlinedBorder,
        elevation = if (selected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (selected) Color.White else MaterialTheme.colors.onSurface
            ) {
                content()
            }
        }
    }
}