package `in`.aicortex.iso8583studio.ui.screens.config

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
import `in`.aicortex.iso8583studio.ui.navigation.GatewayConfigurationState
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentBanner
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentChip
import java.awt.Cursor

/**
 * Tab container for configuration screens with adjustable panel divider
 */
@Composable
fun TabContainer(
    appState: GatewayConfigurationState,
    selectedTab: Int,
    tabTitles: List<String> = listOf("Gateway Type", "Transmission Settings", "Log Settings", "Advanced Options"),
    onTabSelected: (Int) -> Unit,
    onSelectConfig: (Int) -> Unit,
    onAddConfig: () -> Unit,
    onDeleteConfig: () -> Unit,
    onSaveAllConfigs: () -> Unit,
    onOpenMonitor: () -> Unit,
    onOpenHostSimulator: () -> Unit,
    content: @Composable () -> Unit
) {
    // State for the left panel width
    var leftPanelWidth by remember { mutableStateOf(appState.panelWidth) }
    // State for tracking if user is currently resizing
    var isResizing by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel - Available channels
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
                    // Header
                    Text(
                        "Available Channels",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )
                    UnderDevelopmentChip(
                        status = DevelopmentStatus.EXPERIMENTAL
                    )

                    // List of configs in scrollable container
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
                            appState.configList.value.forEachIndexed { index, config ->
                                val isSelected = index == appState.selectedConfigIndex
                                Button(
                                    onClick = { onSelectConfig(index) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = ButtonDefaults.elevation(
                                        defaultElevation = if (isSelected) 4.dp else 0.dp,
                                        pressedElevation = 4.dp
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                                        contentColor = if (isSelected) Color.White else MaterialTheme.colors.onSurface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            config.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Config management buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAddConfig() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add new",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add New")
                        }

                        Button(
                            onClick = { onDeleteConfig() },
                            modifier = Modifier.weight(1f),
                            enabled = appState.selectedConfigIndex >= 0,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
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
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save All")
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = BorderLight
                    )

                    // Monitor and Host Simulator buttons
                    Text(
                        "Tools",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { onOpenMonitor() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = appState.currentConfig != null,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primaryVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorHeart,
                            contentDescription = "Monitor",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Monitor")
                    }

                    Button(
                        onClick = { onOpenHostSimulator() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = appState.currentConfig != null,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = "Host Simulator",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Host Simulator")
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
                                // Update the width based on drag
                                leftPanelWidth = (leftPanelWidth + dragAmount.x.toDp())
                                    .coerceIn(350.dp, 600.dp) // Set min and max width
                                appState.panelWidth = leftPanelWidth
                            }
                        )
                    }
                    .cursorForHorizontalResize()
            ) {
                // Visual indicator in the middle of the divider
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

            // Right panel - Configuration
            if(appState.configList.value.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(12.dp),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Configuration: ${appState.currentConfig?.name}",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Tab selection with improved styling
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
                            tabTitles.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { onTabSelected(index) },
                                    text = {
                                        Text(
                                            title,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    selectedContentColor = MaterialTheme.colors.primary,
                                    unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Tab content with scrolling
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
                                content()
                            }
                        }
                    }
                }
            } else {
                // Empty state when no config selected
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
                                imageVector = Icons.Default.Settings,
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
                                "Create a new configuration or select an existing one",
                                style = MaterialTheme.typography.body1,
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
                                Text("Create New Configuration")
                            }
                        }
                    }
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