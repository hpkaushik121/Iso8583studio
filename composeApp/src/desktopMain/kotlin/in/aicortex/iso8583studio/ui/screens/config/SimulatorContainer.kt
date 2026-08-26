package `in`.aicortex.iso8583studio.ui.screens.config

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.IconButton
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.domain.FileImporter
import `in`.aicortex.iso8583studio.domain.ImportResult
import `in`.aicortex.iso8583studio.domain.utils.ExportResult
import `in`.aicortex.iso8583studio.domain.utils.FileExporter
import `in`.aicortex.iso8583studio.ui.BorderLight
import `in`.aicortex.iso8583studio.ui.Studio
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.ProfileTransferResult
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentChip
import `in`.aicortex.iso8583studio.ui.session.SimulatorSessionManager
import kotlinx.coroutines.launch
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

/**
 * Development-status indicator for the panel header — icon only, label on hover.
 *
 * [UnderDevelopmentChip] is the full-size chip used on landing pages; at 12sp with a leading
 * icon it rendered nearly as wide as the panel title and pulled the eye away from it. Reducing
 * it to the status glyph frees the whole row for the title, and the text moves into a tooltip
 * so nothing is lost — status.description carries the longer explanation where one exists.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StatusIcon(status: DevelopmentStatus) {
    TooltipArea(
        delayMillis = 400,
        tooltip = {
            Surface(
                elevation = 4.dp,
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colors.surface
            ) {
                Text(
                    text = status.title,
                    style = MaterialTheme.typography.caption,
                    color = status.color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    ) {
        Icon(
            imageVector = status.icon,
            contentDescription = status.title,
            tint = status.color,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * One icon button in the configuration toolbar.
 *
 * The icon carries no visible label, so [label] does double duty as the hover tooltip and the
 * accessibility description — a disabled action still explains *why* it is disabled.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ManagementAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TooltipArea(
        delayMillis = 500,
        tooltip = {
            Surface(
                elevation = 4.dp,
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colors.surface
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = if (enabled) tint else tint.copy(alpha = 0.35f)
            )
        }
    }
}

/**
 * Turns a profile name into a safe default filename, e.g. "BASE24 POS" -> "host_BASE24_POS".
 */
private fun suggestedProfileFileName(config: SimulatorConfig): String {
    val safe = config.name.trim()
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifEmpty { "profile" }
    return "${config.simulatorType.name.lowercase()}_$safe"
}

/**
 * Write a single profile to a user-chosen file.
 */
private suspend fun exportSingleProfile(config: SimulatorConfig) {
    val appState = Studio.appState.value
    val window = appState.window ?: return

    when (val prepared = appState.exportProfile(config)) {
        is ProfileTransferResult.Error ->
            appState.resultDialogInterface?.onError { Text(prepared.message) }

        is ProfileTransferResult.Success -> {
            val result = FileExporter().exportFile(
                window = window,
                fileName = suggestedProfileFileName(config),
                fileExtension = "json",
                fileContent = prepared.content.toByteArray(),
                fileDescription = "${config.simulatorType.displayName} Profile"
            )
            when (result) {
                is ExportResult.Success ->
                    appState.resultDialogInterface?.onSuccess {
                        Text("Exported profile \"${prepared.profileName}\"")
                    }

                is ExportResult.Cancelled -> Unit

                is ExportResult.Error ->
                    appState.resultDialogInterface?.onError { Text(result.message) }
            }
        }
    }
}

/**
 * Read a single profile from a user-chosen file and add it to the workspace.
 */
private suspend fun importSingleProfile() {
    val appState = Studio.appState.value
    val window = appState.window ?: return

    var imported: ProfileTransferResult.Success? = null
    val result = FileImporter().importFile(
        window = window,
        title = "Select Profile File",
        fileExtensions = listOf("json"),
        fileDescription = "Simulator Profile",
        importLogic = { file ->
            when (val outcome = appState.importProfile(file)) {
                is ProfileTransferResult.Success -> {
                    imported = outcome
                    ImportResult.Success(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        fileExtension = "json",
                        fileContent = ByteArray(0),
                        fileSize = file.length()
                    )
                }

                is ProfileTransferResult.Error -> ImportResult.Error(outcome.message)
            }
        }
    )

    when (result) {
        is ImportResult.Success ->
            appState.resultDialogInterface?.onSuccess {
                val added = imported
                Text(
                    if (added != null)
                        "Imported \"${added.profileName}\" into ${added.simulatorType.displayName}"
                    else "Profile imported"
                )
            }

        is ImportResult.Cancelled -> Unit

        is ImportResult.Error ->
            appState.resultDialogInterface?.onError { Text(result.message) }
    }
}

@Composable
fun SimulatorConfigLayout(
    config: ContainerConfig,
    onSelectConfig: (SimulatorConfig) -> Unit,
    createNewConfig: () -> Unit,
    onDeleteConfig: (SimulatorConfig) -> Unit,
    onSaveAllConfigs: () -> Unit,
    onLaunchSimulator: (SimulatorConfig) -> Unit,
) {
    val scope = rememberCoroutineScope()
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
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header: icon, name and status chip on one line. The "Configurations"
                        // subtitle is dropped because the screen's own app bar already reads
                        // "<Simulator> Configuration"; repeating it here only cost vertical space.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = config.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colors.primary
                            )
                            Text(
                                config.label,
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            StatusIcon(config.containerStatus)
                        }

                        val selected = config.currentConfig()

                        // Section header: a quiet label carrying the count, with the row-level
                        // actions docked to its right. Putting them here rather than at the panel
                        // foot keeps them adjacent to the thing they act on, and leaves the
                        // bottom of the panel for the two primary commands.
                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PROFILES · ${config.simulatorConfigs.size}",
                                style = MaterialTheme.typography.overline,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            ManagementAction(
                                icon = Icons.Default.Add,
                                label = "New configuration",
                                tint = MaterialTheme.colors.primary
                            ) {
                                createNewConfig()
                                changeCount += 1
                            }
                            // Single-profile transfer. Lives here rather than in each simulator's
                            // screen so every simulator using this layout gets it for free.
                            ManagementAction(
                                icon = Icons.Default.FileUpload,
                                label = "Import a profile from file",
                                tint = MaterialTheme.colors.primary
                            ) {
                                scope.launch {
                                    importSingleProfile()
                                    changeCount += 1
                                }
                            }
                            ManagementAction(
                                icon = Icons.Default.FileDownload,
                                label = if (selected != null)
                                    "Export \"${selected.name}\" to file"
                                else "Select a configuration to export",
                                tint = MaterialTheme.colors.primary,
                                enabled = selected != null
                            ) {
                                selected?.let { current -> scope.launch { exportSingleProfile(current) } }
                            }
                            ManagementAction(
                                icon = Icons.Default.Delete,
                                label = if (selected != null)
                                    "Delete \"${selected.name}\""
                                else "Select a configuration to delete",
                                tint = MaterialTheme.colors.error,
                                enabled = selected != null
                            ) {
                                selected?.let {
                                    onDeleteConfig(it)
                                    changeCount += 1
                                }
                            }
                        }

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
                                    .padding(6.dp)
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    config.simulatorConfigs.forEach { simConfig ->
                                        SimulatorConfigItem(
                                            config = simConfig,
                                            isSelected = simConfig.id == selected?.id,
                                            onClick = {
                                                onSelectConfig(simConfig)
                                                changeCount += 1
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))

                        // Save is secondary to Launch, so it reads as an outlined button above
                        // the filled primary rather than competing with it.
                        OutlinedButton(
                            onClick = { onSaveAllConfigs() },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colors.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save All Configurations", style = MaterialTheme.typography.caption)
                        }

                        // Launch action.
                        //
                        // Previously a status card carrying a heading, the selected config's name
                        // and a button (~128dp). The heading and name were both redundant — the
                        // list above already highlights the selection — so the running state is
                        // now carried by this button's own colour, icon and label.
                        val currentCfg = config.currentConfig()
                        if (currentCfg != null) {
                            // Check live whether this config already has a running session
                            val existingSession =
                                SimulatorSessionManager.sessions.find { it.config.id == currentCfg.id }
                            val isAlreadyRunning = existingSession != null

                            Button(
                                onClick = {
                                    if (isAlreadyRunning) {
                                        existingSession?.let {
                                            SimulatorSessionManager.activateSession(it.id)
                                        }
                                    } else {
                                        onLaunchSimulator(currentCfg)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (isAlreadyRunning)
                                        Color(0xFF388E3C)   // green — already running
                                    else
                                        MaterialTheme.colors.secondary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = if (isAlreadyRunning)
                                        Icons.Default.Layers else Icons.Default.Terminal,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAlreadyRunning)
                                        "Switch to Running Tab"
                                    else "Launch ${config.label}",
                                    style = MaterialTheme.typography.button,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Terminal else Icons.Default.Computer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) Color.White else MaterialTheme.colors.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        config.name,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (config.description.isNotBlank()) {
                        // Clamped to one line: descriptions are free text and can run to many
                        // paragraphs (an imported spec profile documents its whole dialect here),
                        // which would otherwise let a single entry crowd out the rest of the list.
                        Text(
                            config.description.lineSequence().first { it.isNotBlank() },
                            style = MaterialTheme.typography.caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected)
                                Color.White.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
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

/**
 * Extension function to set the cursor for horizontal resize
 */
fun Modifier.cursorForHorizontalResize(): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this.hoverable(interactionSource)
        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
}