package `in`.aicortex.iso8583studio.ui.screens.config

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.navigation.GatewayConfigurationState
import java.awt.Cursor

/**
 * Tab container for configuration screens with adjustable panel divider
 */
@Composable
fun TabContainer(
    appState: GatewayConfigurationState,
    selectedTab: Int,
    tabTitles: List<String> = listOf("Gateway Type", "Transmission Settings", "Keys Setting", "Log Settings", "Advanced Options"),
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


    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel - Available channels
            Box(
                modifier = Modifier
                    .width(leftPanelWidth)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .border(1.dp, Color.Gray)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Available channels", style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Names", style = MaterialTheme.typography.body1)

                    // List of configs
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray)
                            .padding(4.dp)
                    ) {
                        appState.configList.value.forEachIndexed { index, config ->
                            Button(
                                onClick = { onSelectConfig(index) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (index == appState.selectedConfigIndex) Color.LightGray else Color.White
                                )
                            ) {
                                Text(config.name)
                            }
                        }
                    }

                    // Buttons for config management
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(onClick = { onAddConfig() }) {
                            Text("Add new")
                        }

                        Button(onClick = { onDeleteConfig() }) {
                            Text("Delete")
                        }
                    }

                    Button(
                        onClick = { onSaveAllConfigs() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save all")
                    }

                    // Monitor and Host Simulator buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { onOpenMonitor() },
                            enabled = appState.currentConfig != null
                        ) {
                            Text("Monitor")
                        }

                        Button(
                            onClick = { onOpenHostSimulator() },
                            enabled = appState.currentConfig != null
                        ) {
                            Text("Host Simulator")
                        }
                    }
                }
            }

            // Resizable divider
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(Color.LightGray)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isResizing = true },
                            onDragEnd = { isResizing = false },
                            onDragCancel = { isResizing = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // Update the width based on drag
                                leftPanelWidth = (leftPanelWidth + dragAmount.x.toDp())
                                    .coerceIn(300.dp, 800.dp) // Set min and max width
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
                        .background(
                            color = if (isResizing) MaterialTheme.colors.primary else Color.Gray,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
            if(appState.configList.value.isNotEmpty()){
                // Right panel - Configuration
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                        .border(1.dp, Color.Gray)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Configuration", style = MaterialTheme.typography.h6)

                        // Tab selection
                        TabRow(selectedTabIndex = selectedTab) {
                            tabTitles.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { onTabSelected(index) },
                                    text = { Text(title) }
                                )
                            }
                        }

                        // Tab content
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(scrollState)
                                ) {
                                    content()
                                    // Add some bottom padding to ensure content isn't cut off
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Optional: Add a vertical scrollbar
                                VerticalScrollbar(
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    adapter = rememberScrollbarAdapter(scrollState)
                                )
                            }
                        }
                    }
                }
            }else{
                // Right panel - Empty when no config selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                        .border(1.dp, Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select a configuration or create a new one")
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
