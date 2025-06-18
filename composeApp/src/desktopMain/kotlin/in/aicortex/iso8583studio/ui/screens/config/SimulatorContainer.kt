package `in`.aicortex.iso8583studio.ui.screens.config

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.ui.BorderLight
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentChip
import java.awt.Cursor

data class ConfigTab(
    val id: Int = (Math.random() * 10000).toInt(),
    val label: String,
    val content: @Composable () -> Unit
)

data class ContainerConfig(
    val tabs: List<ConfigTab>,
    val icon: ImageVector,
    val label: String,
    val simulatorConfigs: List<SimulatorConfig>,
    val currentConfig: () -> SimulatorConfig?,
    val containerStatus: DevelopmentStatus = DevelopmentStatus.UNDER_DEVELOPMENT
)

@Composable
fun SimulatorConfigLayout(
    config: ContainerConfig,
    onSelectConfig: (SimulatorConfig) -> Unit,
    createNewConfig: () -> Unit,
    onDeleteConfig: (SimulatorConfig) -> Unit,
    onSaveAllConfigs: () -> Unit,
    onLaunchSimulator: (SimulatorConfig) -> Unit,
) {
    // State for the left panel width
    var leftPanelWidth by remember { mutableStateOf(380.dp) }
    // State for tracking if user is currently resizing
    var isResizing by remember { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        var changeCount by remember { mutableStateOf(0) }
        key(changeCount) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left panel - Host Simulator Configurations
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
                        // Header with simulator icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = config.icon,
                                contentDescription = config.label,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colors.primary
                            )
                            Column {
                                Text(
                                    config.label,
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

                        UnderDevelopmentChip(
                            status = config.containerStatus
                        )

                        // Configuration list
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
                                if (config.simulatorConfigs.isEmpty()) {
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
                                                imageVector = Icons.Default.Terminal,
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
                                                "Create your first configuration to begin",
                                                style = MaterialTheme.typography.caption,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                } else {
                                    config.simulatorConfigs.forEachIndexed { index, simConfig ->
                                        val isSelected =
                                            simConfig.id == config.currentConfig()?.id
                                        SimulatorConfigItem(
                                            config = simConfig,
                                            isSelected = isSelected,
                                            onClick = {
                                                onSelectConfig(simConfig)
                                                changeCount += 1
                                            }
                                        )
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
                                        onClick = {
                                            createNewConfig()
                                            changeCount += 1
                                        },
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
                                        onClick = {
                                            config.currentConfig()?.let {
                                                onDeleteConfig(it)
                                                changeCount += 1
                                            }

                                        },
                                        modifier = Modifier.weight(1f),
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
                                    Text("Save All Configurations")
                                }
                            }
                        }

                        // Launch Simulator section
                        if (config.currentConfig() != null) {
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
                                        "Configuration: ${config.currentConfig()?.name}",
                                        style = MaterialTheme.typography.caption,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )

                                    Button(
                                        onClick = { config.currentConfig()?.let { onLaunchSimulator(it) } },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color.White,
                                            contentColor = MaterialTheme.colors.secondary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Terminal,
                                            contentDescription = "Launch Simulator",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Launch ${config.label}",
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

                // Right panel - Configuration Editor
                if (config.simulatorConfigs.isNotEmpty() && config.currentConfig() != null) {
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
                                    selectedTabIndex = selectedTabIndex,
                                    backgroundColor = MaterialTheme.colors.surface,
                                    contentColor = MaterialTheme.colors.primary,
                                    divider = {
                                        Divider(
                                            color = BorderLight,
                                            thickness = 1.dp
                                        )
                                    }
                                ) {
                                    config.tabs.forEachIndexed { index, tab ->
                                        Tab(
                                            selected = selectedTabIndex == index,
                                            onClick = { selectedTabIndex = index },
                                            text = {
                                                Text(
                                                    tab.label,
                                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                                    style = MaterialTheme.typography.caption
                                                )
                                            },
                                            selectedContentColor = MaterialTheme.colors.primary,
                                            unselectedContentColor = MaterialTheme.colors.onSurface.copy(
                                                alpha = 0.7f
                                            )
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
                                        config.tabs[selectedTabIndex].content()
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
                                    imageVector = Icons.Default.Computer,
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
                                    onClick = {
                                        createNewConfig()
                                        changeCount += 1
                                    },
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
            }
        }
    }
}

/**
 * Individual configuration item component
 */
@Composable
private fun SimulatorConfigItem(
    config: SimulatorConfig,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
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
                            imageVector = if (isSelected) Icons.Default.Terminal else Icons.Default.Computer,
                            contentDescription = "Configuration",
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else MaterialTheme.colors.primary
                        )
                        Text(
                            config.name,
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (config.description.isNotEmpty()) {
                        Text(
                            config.description,
                            style = MaterialTheme.typography.caption,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colors.onSurface.copy(
                                alpha = 0.6f
                            )
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

/**
 * Extension function to set the cursor for horizontal resize
 */
fun Modifier.cursorForHorizontalResize(): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this.hoverable(interactionSource)
        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
}