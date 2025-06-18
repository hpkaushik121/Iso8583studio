package `in`.aicortex.iso8583studio.ui.screens.hsmSimulator


import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.HsmServiceImpl
import `in`.aicortex.iso8583studio.domain.FileImporter
import `in`.aicortex.iso8583studio.domain.ImportResult
import `in`.aicortex.iso8583studio.domain.utils.ExportResult
import `in`.aicortex.iso8583studio.domain.utils.FileExporter
import `in`.aicortex.iso8583studio.data.rememberIsoCoroutineScope
import `in`.aicortex.iso8583studio.ui.Studio
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.*

@Composable
fun HsmResponseConfigScreen(
    hsm: HsmServiceImpl,
    onSaveClick: () -> Unit
) {
    // Use mutableStateListOf for proper recomposition
    val hsmCommands = remember {
        hsm.hsmConfiguration.simulatedCommandsToSource.ifEmpty { listOf(HsmCommand(commandCode = "A0",
            commandName = "",
            description = "",
            id = "213")) }.toMutableStateList()
    }

    var selectedCommand by remember { mutableStateOf<HsmCommand?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    // FIX 1: Add recomposition trigger for parameter changes
    var parameterChangeCounter by remember { mutableStateOf(0) }

    // Dialog states
    var showCommandInfoDialog by remember { mutableStateOf(false) }
    var showAddCommandDialog by remember { mutableStateOf(false) }
    var showEditCommandDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var commandToEdit by remember { mutableStateOf<HsmCommand?>(null) }
    var commandToDelete by remember { mutableStateOf<HsmCommand?>(null) }

    // Stage function that updates the HSM config
    val stageCommands = {
        hsm.hsmConfiguration.simulatedCommandsToSource = hsmCommands
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Enhanced Header
        EnhancedHsmHeader(
            hsm = hsm,
            onCommandInfoClick = { showCommandInfoDialog = true },
            onExportClick = { showExportDialog = true },
            onImportClick = { showImportDialog = true },
            onSaveClick = onSaveClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main Content
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Panel - HSM Command List
            EnhancedHsmCommandPanel(
                modifier = Modifier.weight(1f),
                commands = hsmCommands.toList(),
                selectedCommand = selectedCommand,
                onCommandSelect = {
                    selectedCommand = it
                    parameterChangeCounter++ // Force recomposition when selection changes
                },
                onAddCommand = { showAddCommandDialog = true },
                onEditCommand = { command ->
                    commandToEdit = command
                    showEditCommandDialog = true
                },
                onDuplicateCommand = { command ->
                    val duplicatedCommand = command.copy(
                        id = generateHsmCommandId(),
                        description = "${command.description} (Copy)",
                        parameters = command.parameters?.map { it.copy() }?.toMutableList()
                    )
                    hsmCommands.add(duplicatedCommand)
                    stageCommands()
                },
                onDeleteCommand = { command ->
                    commandToDelete = command
                    showDeleteConfirmDialog = true
                }
            )

            // FIX 2: Right Panel with proper recomposition tracking
            EnhancedHsmParameterPanel(
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                selectedCommand = selectedCommand,
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                hsm = hsm,
                onCommandUpdated = { updatedCommand, isRecompose ->
                    val index = hsmCommands.indexOfFirst { it.id == updatedCommand.id }
                    if (index != -1) {
                        hsmCommands[index] = updatedCommand
                        selectedCommand = updatedCommand
                        if (isRecompose) {
                            parameterChangeCounter++ // FIX 3: Increment counter on parameter changes
                        }
                    }
                    stageCommands()
                },
                parameterChangeCounter = parameterChangeCounter // FIX 4: Pass counter to force recomposition
            )
        }
    }

    // Dialogs
    if (showCommandInfoDialog) {
        HsmCommandInformationDialog(
            onCloseRequest = { showCommandInfoDialog = false }
        )
    }

    // Dialogs remain the same but with proper parameter change tracking
    if (showAddCommandDialog) {
        EnhancedAddHsmCommandDialog(
            onDismiss = { showAddCommandDialog = false },
            onSave = { newCommand ->
                hsmCommands.add(newCommand)
                showAddCommandDialog = false
                stageCommands()
            },
            hsm = hsm
        )
    }

    if (showEditCommandDialog && commandToEdit != null) {
        EnhancedAddHsmCommandDialog(
            command = commandToEdit!!,
            isEditing = true,
            onDismiss = {
                showEditCommandDialog = false
                commandToEdit = null
            },
            onSave = { updatedCommand ->
                val index = hsmCommands.indexOfFirst { it.id == updatedCommand.id }
                if (index != -1) {
                    hsmCommands[index] = updatedCommand
                    if (selectedCommand?.id == updatedCommand.id) {
                        selectedCommand = updatedCommand
                        parameterChangeCounter++ // Track changes from edit dialog
                    }
                }
                showEditCommandDialog = false
                commandToEdit = null
                stageCommands()
            },
            hsm = hsm
        )
    }

    if (showDeleteConfirmDialog && commandToDelete != null) {
        HsmDeleteConfirmationDialog(
            command = commandToDelete!!,
            onDismiss = {
                showDeleteConfirmDialog = false
                commandToDelete = null
            },
            onConfirm = {
                hsmCommands.removeIf { it.id == commandToDelete!!.id }
                if (selectedCommand?.id == commandToDelete!!.id) {
                    selectedCommand = null
                    parameterChangeCounter++ // Track changes from deletion
                }
                showDeleteConfirmDialog = false
                commandToDelete = null
                stageCommands()
            }
        )
    }

    val isoCoroutine = rememberCoroutineScope()

    if (showExportDialog) {
        isoCoroutine.launch {
            val file = FileExporter().exportFile(
                window = Studio.appState.value.window!!,
                fileName = "HSM_Commands_Configuration",
                fileExtension = "json",
                fileContent = Json.encodeToString(hsmCommands.toList()).toByteArray(),
                fileDescription = "HSM Commands Configuration File"
            )
            showExportDialog = false
            when (file) {
                is ExportResult.Success -> {
//                    hsm.showSuccess {
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                            verticalArrangement = Arrangement.Center
//                        ) {
//                            Text("HSM configuration exported successfully!")
//                        }
//                    }
                }

                is ExportResult.Cancelled -> {
                    println("Export cancelled")
                }

                is ExportResult.Error -> {
//                    hsm.showError {
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                            verticalArrangement = Arrangement.Center,
//                        ) {
//                            Text((file as ExportResult.Error).message)
//                        }
//                    }
                }
            }
        }
    }

    if (showImportDialog) {
        isoCoroutine.launch {
            val file = FileImporter().importFile(
                window = Studio.appState.value.window!!,
                fileExtensions = listOf("json"),
                importLogic = { file ->
                    try {
                        val data: List<HsmCommand> = Json.decodeFromString(file.readText())
                        hsmCommands.addAll(data)
                        stageCommands()
                        ImportResult.Success(fileContent = file.readBytes())
                    } catch (e: Exception) {
                        ImportResult.Error("Failed to import HSM configuration", e)
                    }
                }
            )
            showImportDialog = false
            when (file) {
                is ImportResult.Success -> {
//                    hsm.showSuccess {
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                            verticalArrangement = Arrangement.Center
//                        ) {
//                            Text("HSM configuration imported successfully!")
//                        }
//                    }
                }

                is ImportResult.Cancelled -> {
                    println("Import cancelled")
                }

                is ImportResult.Error -> {
//                    hsm.showError {
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                            verticalArrangement = Arrangement.Center
//                        ) {
//                            Text((file as ImportResult.Error).message)
//                        }
//                    }
                }
            }
        }
    }
}


@Composable
private fun EnhancedHsmHeader(
    hsm: HsmServiceImpl,
    onCommandInfoClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colors.primary.copy(alpha = 0.1f),
                            MaterialTheme.colors.secondary.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // Title and main actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HSM Command Simulator",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "Configure and manage HSM command templates and responses",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HsmActionButton(
                        icon = Icons.Default.Security,
                        text = "Command Info",
                        onClick = onCommandInfoClick
                    )

                    HsmActionButton(
                        icon = Icons.Default.Upload,
                        text = "Export",
                        onClick = onExportClick
                    )

                    HsmActionButton(
                        icon = Icons.Default.Download,
                        text = "Import",
                        onClick = onImportClick
                    )

                    Button(
                        onClick = onSaveClick
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save All")
                    }
                }
            }
        }
    }
}

@Composable
private fun HsmActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick
    ) {
        Icon(
            icon,
            contentDescription = text,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 14.sp)
    }
}

@Composable
private fun EnhancedHsmCommandPanel(
    modifier: Modifier,
    commands: List<HsmCommand>,
    selectedCommand: HsmCommand?,
    onCommandSelect: (HsmCommand) -> Unit,
    onAddCommand: () -> Unit,
    onEditCommand: (HsmCommand) -> Unit,
    onDuplicateCommand: (HsmCommand) -> Unit,
    onDeleteCommand: (HsmCommand) -> Unit
) {
    // Use derivedStateOf for computed values to optimize recomposition
    val filteredCommands by remember {
        derivedStateOf {
            commands
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Header with stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HSM Commands",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (filteredCommands.size == commands.size) {
                            "${commands.size} total"
                        } else {
                            "${filteredCommands.size} of ${commands.size}"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                FloatingActionButton(
                    onClick = onAddCommand,
                    modifier = Modifier.size(40.dp),
                    backgroundColor = Color.White,
                    contentColor = MaterialTheme.colors.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add HSM Command",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // HSM Command List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = filteredCommands,
                    key = { it.id } // FIX 9: Proper key for LazyColumn items
                ) { command ->
                    EnhancedHsmCommandCard(
                        command = command,
                        isSelected = selectedCommand?.id == command.id,
                        onClick = { onCommandSelect(command) },
                        onEdit = { onEditCommand(command) },
                        onDuplicate = { onDuplicateCommand(command) },
                        onDelete = { onDeleteCommand(command) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedHsmCommandCard(
    command: HsmCommand,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = if (isSelected) BorderStroke(2.dp, color = MaterialTheme.colors.primary) else null,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Main Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (command.keyMatching.enabled) {
                                Color.Green
                            } else {
                                MaterialTheme.colors.primary
                            },
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // HSM Command info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = command.description.ifEmpty { "Unnamed HSM Command" },
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (isSelected) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSurface
                        }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (command.commandCode.isNotEmpty()) {
                            HsmCommandBadge("CMD", command.commandCode)
                        }
                        if (command.commandName.isNotEmpty()) {
                            HsmCommandBadge("TYPE", command.commandName)
                        }
                        if (command.keyMatching.enabled) {
                            HsmCommandBadge("KEY", "✓", Color.Green)
                        }
                    }
                }

                // Actions
                Row {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expanded actions
            AnimatedVisibility(
                visible = expanded,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.surface.copy(alpha = 0.5f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HsmActionChip(
                        icon = Icons.Default.Edit,
                        text = "Edit",
                        onClick = onEdit
                    )

                    HsmActionChip(
                        icon = Icons.Default.FileCopy,
                        text = "Duplicate",
                        onClick = onDuplicate
                    )

                    HsmActionChip(
                        icon = Icons.Default.Delete,
                        text = "Delete",
                        onClick = onDelete,
                        isDestructive = true
                    )
                }
            }
        }
    }
}

@Composable
private fun HsmCommandBadge(
    label: String,
    value: String,
    color: Color = MaterialTheme.colors.secondary
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "$label: $value",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HsmActionChip(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val color = if (isDestructive) {
        MaterialTheme.colors.error
    } else {
        MaterialTheme.colors.primary
    }

    Surface(
        modifier = Modifier.clickable { onClick() },
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text = text,
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EnhancedHsmParameterPanel(
    modifier: Modifier,
    selectedCommand: HsmCommand?,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    hsm: HsmServiceImpl,
    onCommandUpdated: (HsmCommand, Boolean) -> Unit,
    parameterChangeCounter: Int = 0 // FIX 6: Add parameter change counter parameter
) {
    Card(
        modifier = modifier,
        elevation = 6.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            if (selectedCommand != null) {
                // FIX 7: Use key with parameter change counter to force recomposition
                key(selectedCommand.id, parameterChangeCounter) {
                    // Tab Header
                    TabRow(
                        selectedTabIndex = selectedTab,
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { onTabChange(0) },
                            text = { Text("Parameters", fontSize = 14.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { onTabChange(1) },
                            text = { Text("Config", fontSize = 14.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { onTabChange(2) },
                            text = { Text("Keys", fontSize = 14.sp) }
                        )
                    }

                    // Tab Content
                    when (selectedTab) {
                        0 -> HsmParametersTab(
                            command = selectedCommand,
                            hsm = hsm,
                            onCommandUpdated = { onCommandUpdated(it, true) },
                            parameterChangeCounter = parameterChangeCounter // FIX 8: Pass counter down
                        )

                        1 -> HsmConfigurationTab(selectedCommand)
                        2 -> HsmKeyConfigurationTab(
                            command = selectedCommand,
                            onCommandUpdated = { onCommandUpdated(it, false) }
                        )
                    }
                }
            } else {
                EmptyHsmSelectionPanel()
            }
        }
    }
}

@Composable
private fun HsmParametersTab(
    command: HsmCommand,
    hsm: HsmServiceImpl,
    onCommandUpdated: (HsmCommand) -> Unit,
    parameterChangeCounter: Int = 0 // FIX 10: Add parameter change counter
) {
    // FIX 11: Create computed properties that depend on parameter change counter
    val activeParametersCount by remember(command.parameters, parameterChangeCounter) {
        derivedStateOf {
            command.parameters?.count { it.value.isNotEmpty() } ?: 0
        }
    }

    val activeParameters by remember(command.parameters, parameterChangeCounter) {
        derivedStateOf {
            command.parameters?.withIndex()?.filter { it.value.value.isNotEmpty() } ?: emptyList()
        }
    }

    // FIX 12: Use key with multiple dependencies to force complete recomposition
    key(command.id, parameterChangeCounter, activeParametersCount) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Parameters header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.surface.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HSM Parameters",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = "$activeParametersCount parameters configured",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            // Quick Add Parameters Bar
            QuickAddHsmParametersBar(
                commandCode = command.commandCode,
                existingParameters = activeParameters.map { it.value.name }.toSet(),
                onParametersAdded = { parameterTypes ->
                    val updatedParameters = command.parameters?.toMutableList() ?: mutableListOf()
                    var hasChanges = false
                    parameterTypes.forEach { type ->
                        val existingParam = updatedParameters.find { it.name == type.displayName }
                        if (existingParam == null) {
                            updatedParameters.add(
                                HsmParameter(
                                    name = type.displayName,
                                    type = type,
                                    required = isRequiredParameter(type),
                                    description = getParameterDescription(type),
                                    value = "",
                                    maxLength = getParameterMaxLength(type),
                                    format = getParameterFormat(type)
                                )
                            )
                            hasChanges = true
                        }
                    }
                    if (hasChanges) {
                        onCommandUpdated(command.copy(parameters = updatedParameters))
                    }
                }
            )

            // Parameters List
            if (activeParameters.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = activeParameters,
                        key = { _, (index, parameter) ->
                            "${command.id}_param_${index}_${parameter.value.isNotEmpty()}_${parameterChangeCounter}"
                        }
                    ) { _, (index, parameter) ->
                        EnhancedHsmParameterRow(
                            parameter = parameter,
                            onParameterChange = { newValue ->
                                val updatedParameters =
                                    command.parameters?.toMutableList() ?: mutableListOf()
                                if (index < updatedParameters.size) {
                                    updatedParameters[index] =
                                        updatedParameters[index].copy(value = newValue)
                                    onCommandUpdated(command.copy(parameters = updatedParameters))
                                }
                            },
                            onRemove = {
                                val updatedParameters =
                                    command.parameters?.toMutableList() ?: mutableListOf()
                                if (index < updatedParameters.size) {
                                    updatedParameters[index] =
                                        updatedParameters[index].copy(value = "")
                                    onCommandUpdated(command.copy(parameters = updatedParameters))
                                }
                            },
                            updateKey = parameterChangeCounter
                        )
                    }
                }
            } else {
                EmptyHsmParametersList()
            }

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.surface.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                var showAddParameterDialog by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showAddParameterDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Parameter")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Parameters")
                }

                if (showAddParameterDialog) {
                    EnhancedAddHsmParameterDialog(
                        commandCode = command.commandCode,
                        existingParameters = activeParameters.map { it.value.name }.toSet(),
                        onSave = { parameterTypes ->
                            val updatedParameters =
                                command.parameters?.toMutableList() ?: mutableListOf()
                            var hasChanges = false
                            parameterTypes.forEach { type ->
                                val existingParam =
                                    updatedParameters.find { it.name == type.displayName }
                                if (existingParam == null) {
                                    updatedParameters.add(
                                        HsmParameter(
                                            name = type.displayName,
                                            type = type,
                                            required = isRequiredParameter(type),
                                            description = getParameterDescription(type),
                                            value = "",
                                            maxLength = getParameterMaxLength(type),
                                            format = getParameterFormat(type)
                                        )
                                    )
                                    hasChanges = true
                                }
                            }
                            if (hasChanges) {
                                onCommandUpdated(command.copy(parameters = updatedParameters))
                            }
                            showAddParameterDialog = false
                        },
                        onDismiss = { showAddParameterDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddHsmParametersBar(
    commandCode: String,
    existingParameters: Set<String> = emptySet(),
    onParametersAdded: (List<HsmParameterType>) -> Unit
) {
    val commonParameters = remember(commandCode) {
        when (commandCode.uppercase()) {
            "A0" -> listOf( // Generate Key
                HsmParameterType.KEY_ID,
                HsmParameterType.KEY_DATA,
                HsmParameterType.STATUS_CODE
            )

            "A2" -> listOf( // Encrypt Data
                HsmParameterType.KEY_ID,
                HsmParameterType.DATA_BLOCK,
                HsmParameterType.STATUS_CODE
            )

            "A4" -> listOf( // Decrypt Data
                HsmParameterType.KEY_ID,
                HsmParameterType.DATA_BLOCK,
                HsmParameterType.STATUS_CODE
            )

            "DA" -> listOf( // Verify PIN
                HsmParameterType.PIN_BLOCK,
                HsmParameterType.KEY_ID,
                HsmParameterType.STATUS_CODE
            )

            "CA" -> listOf( // Translate PIN
                HsmParameterType.PIN_BLOCK,
                HsmParameterType.KEY_ID,
                HsmParameterType.STATUS_CODE
            )

            "M0" -> listOf( // Generate MAC
                HsmParameterType.MAC_DATA,
                HsmParameterType.KEY_ID,
                HsmParameterType.STATUS_CODE
            )

            else -> listOf(
                HsmParameterType.COMMAND_DATA,
                HsmParameterType.STATUS_CODE,
                HsmParameterType.SESSION_ID
            )
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary.copy(alpha = 0.05f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(commonParameters) { paramType ->
            val isAlreadyAdded = existingParameters.contains(paramType.displayName)

            Surface(
                modifier = Modifier.clickable(enabled = !isAlreadyAdded) {
                    if (!isAlreadyAdded) {
                        onParametersAdded(listOf(paramType))
                    }
                },
                color = if (isAlreadyAdded) {
                    MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colors.primary.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = paramType.displayName,
                        fontSize = 10.sp,
                        color = if (isAlreadyAdded) {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colors.onSurface
                        }
                    )
                    if (isAlreadyAdded) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Already added",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHsmParametersList() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = "No Parameters Configured",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Use quick add above or click 'Add Parameters' to get started",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EnhancedHsmParameterRow(
    parameter: HsmParameter,
    onParameterChange: (String) -> Unit,
    onRemove: () -> Unit,
    updateKey: Int = 0,
) {
    // Use update key in the key function to force recomposition
    key(parameter.name, parameter.value, updateKey) {
        var value by remember(parameter.value, updateKey) {
            mutableStateOf(parameter.value)
        }

        // Track if we're actively editing to prevent accidental collapse
        var isEditing by remember { mutableStateOf(false) }
        var isExpanded by remember { mutableStateOf(false) }
        var isFocused by remember { mutableStateOf(false) }

        // Update local state when parameter changes
        LaunchedEffect(parameter.value, updateKey) {
            val newValue = parameter.value
            if (value != newValue) {
                value = newValue
            }
        }

        // Animation states
        val cardElevation by animateFloatAsState(
            targetValue = if (isExpanded) 8f else 2f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )

        Card(
            elevation = cardElevation.dp,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
        ) {
            Column {
                // Main header row (non-clickable when expanded and editing)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            // Only make clickable if not actively editing
                            if (!isEditing && !isFocused) {
                                Modifier.clickable {
                                    if (!isEditing && !isFocused) {
                                        isExpanded = (!isExpanded)
                                    }
                                }
                            } else {
                                Modifier
                            }
                        ),
                    color = if (isExpanded)
                        MaterialTheme.colors.primary.copy(alpha = 0.05f)
                    else
                        Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Enhanced parameter type badge
                        Card(
                            backgroundColor = if (isExpanded)
                                MaterialTheme.colors.primary
                            else
                                MaterialTheme.colors.secondary,
                            shape = RoundedCornerShape(8.dp),
                            elevation = if (isExpanded) 4.dp else 2.dp
                        ) {
                            Text(
                                text = parameter.type.name.take(3),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Enhanced parameter info with status indicators
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = parameter.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = if (isExpanded)
                                        MaterialTheme.colors.primary
                                    else
                                        MaterialTheme.colors.onSurface
                                )

                                // Status indicator
                                if (value.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Has value",
                                            tint = MaterialTheme.colors.primary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(2.dp)
                                        )
                                    }
                                }

                                // Required indicator
                                if (parameter.required) {
                                    Surface(
                                        color = MaterialTheme.colors.error.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "*",
                                            color = MaterialTheme.colors.error,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }

                            // Value preview (only when collapsed and has value)
                            AnimatedVisibility(
                                visible = !isExpanded && value.isNotEmpty(),
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                Text(
                                    text = if (value.length > 30) "${value.take(30)}..." else value,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = MaterialTheme.typography.caption
                                )
                            }
                        }

                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Remove button
                            IconButton(
                                onClick = onRemove,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove Parameter",
                                    tint = MaterialTheme.colors.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Expand/collapse button (separate from row click)
                            IconButton(
                                onClick = {
                                    if (!isEditing && !isFocused) {
                                        isExpanded = (!isExpanded)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                AnimatedContent(
                                    targetState = isExpanded,
                                    transitionSpec = {
                                        scaleIn() + fadeIn() with scaleOut() + fadeOut()
                                    }
                                ) { expanded ->
                                    Icon(
                                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (expanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Enhanced expanded content
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = slideInVertically(
                        initialOffsetY = { -it / 2 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { -it / 2 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
                    ) + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 0.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        ),
                        backgroundColor = MaterialTheme.colors.background,
                        elevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Enhanced parameter metadata
                            Text(
                                text = "Parameter Specifications",
                                style = MaterialTheme.typography.subtitle2,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                item {
                                    EnhancedHsmMetadataChip(
                                        icon = Icons.Default.Category,
                                        label = "Type",
                                        value = parameter.type.displayName,
                                        color = MaterialTheme.colors.primary
                                    )
                                }
                                item {
                                    EnhancedHsmMetadataChip(
                                        icon = Icons.Default.Straighten,
                                        label = "Max Length",
                                        value = if (parameter.maxLength > 0)
                                            parameter.maxLength.toString()
                                        else
                                            "Variable",
                                        color = MaterialTheme.colors.secondary
                                    )
                                }
                                item {
                                    EnhancedHsmMetadataChip(
                                        icon = Icons.Default.FormatSize,
                                        label = "Format",
                                        value = parameter.format.displayName,
                                        color = MaterialTheme.colors.primary
                                    )
                                }
                                item {
                                    EnhancedHsmMetadataChip(
                                        icon = if (parameter.required) Icons.Default.Star else Icons.Default.StarBorder,
                                        label = "Required",
                                        value = if (parameter.required) "Yes" else "No",
                                        color = if (parameter.required) MaterialTheme.colors.error else MaterialTheme.colors.secondary
                                    )
                                }
                                if (parameter.description.isNotEmpty()) {
                                    item {
                                        EnhancedHsmMetadataChip(
                                            icon = Icons.Default.Info,
                                            label = "Description",
                                            value = parameter.description,
                                            color = MaterialTheme.colors.secondary,
                                            expanded = true
                                        )
                                    }
                                }
                            }

                            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))

                            // Enhanced value input section
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Parameter Value",
                                        style = MaterialTheme.typography.subtitle2,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                                    )
                                    if (value.isNotEmpty()) {
                                        Text(
                                            text = "(${value.length} chars)",
                                            style = MaterialTheme.typography.caption,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                // Enhanced text field with proper focus handling
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { newValue ->
                                        value = newValue
                                        onParameterChange(newValue)
                                        isEditing = true
                                    },
                                    label = {
                                        Text("Enter value for ${parameter.name}")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            isFocused = focusState.isFocused
                                            if (!focusState.isFocused) {
                                                // Small delay to allow for value processing
                                                isEditing = false
                                            }
                                        },
                                    placeholder = {
                                        Text(
                                            getParameterPlaceholder(parameter.type),
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                                        )
                                    },
                                    singleLine = parameter.type != HsmParameterType.CERTIFICATE,
                                    maxLines = if (parameter.type == HsmParameterType.CERTIFICATE) 6 else 1,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = MaterialTheme.colors.primary,
                                        cursorColor = MaterialTheme.colors.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    trailingIcon = {
                                        if (value.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    value = ""
                                                    onParameterChange("")
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Clear value",
                                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                )

                                // Value validation and helpers
                                if (value.isNotEmpty()) {
                                    val isValidLength = when {
                                        parameter.maxLength <= 0 -> true
                                        parameter.required -> value.length <= parameter.maxLength && value.isNotEmpty()
                                        else -> value.length <= parameter.maxLength
                                    }

                                    val isValidFormat =
                                        validateParameterFormat(value, parameter.format)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                if (isValidLength && isValidFormat) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = if (isValidLength && isValidFormat)
                                                    MaterialTheme.colors.primary
                                                else
                                                    MaterialTheme.colors.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (isValidLength && isValidFormat) "Valid" else "Invalid",
                                                style = MaterialTheme.typography.caption,
                                                color = if (isValidLength && isValidFormat)
                                                    MaterialTheme.colors.primary
                                                else
                                                    MaterialTheme.colors.error
                                            )
                                        }

                                        if (parameter.maxLength > 0) {
                                            Text(
                                                text = "${value.length}/${parameter.maxLength}",
                                                style = MaterialTheme.typography.caption,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedHsmMetadataChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    expanded: Boolean = false
) {
    Card(
        backgroundColor = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (expanded) 12.dp else 8.dp,
                vertical = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            if (!expanded) {
                Text(
                    text = "$label:",
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = if (expanded && value.length > 20) "${value.take(20)}..." else value,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HsmConfigurationTab(command: HsmCommand) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HsmConfigSection("Basic Information") {
                HsmConfigRow("Command ID", command.id)
                HsmConfigRow("Description", command.description)
                if (command.commandCode.isNotEmpty()) {
                    HsmConfigRow("Command Code", command.commandCode)
                }
                if (command.commandName.isNotEmpty()) {
                    HsmConfigRow("Command Name", command.commandName)
                }
            }
        }

        item {
            HsmConfigSection("Parameter Statistics") {
                val parameterCount = command.parameters?.count { it.value.isNotEmpty() } ?: 0
                val totalParameters = command.parameters?.size ?: 0
                val requiredParams = command.parameters?.count { it.required } ?: 0
                HsmConfigRow("Parameters Configured", "$parameterCount of $totalParameters")
                HsmConfigRow("Required Parameters", "$requiredParams")
                HsmConfigRow(
                    "Configuration Type",
                    if (command.keyMatching.enabled) "Advanced (Key Matching)" else "Basic (HSM Command)"
                )
            }
        }

        if (command.keyMatching.enabled) {
            item {
                HsmConfigSection("Key Matching Configuration") {
                    HsmConfigRow("Priority", command.keyMatching.priority.toString())
                    HsmConfigRow(
                        "Key ID Matching",
                        if (command.keyMatching.keyIdMatching.enabled) "Enabled" else "Disabled"
                    )
                    if (command.keyMatching.keyIdMatching.enabled) {
                        HsmConfigRow("Key Type", command.keyMatching.keyIdMatching.keyType)
                        HsmConfigRow("Key ID", command.keyMatching.keyIdMatching.keyId)
                    }
                    HsmConfigRow(
                        "Session Matchers",
                        "${command.keyMatching.sessionMatching.size} configured"
                    )
                }
            }
        }
    }
}

@Composable
private fun HsmConfigSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun HsmConfigRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value.ifEmpty { "Not set" },
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium,
            color = if (value.isEmpty()) {
                MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colors.onSurface
            }
        )
    }
}

@Composable
private fun HsmKeyConfigurationTab(
    command: HsmCommand,
    onCommandUpdated: (HsmCommand) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Key Matching Section
            HsmKeyMatchingCard(
                keyMatching = command.keyMatching,
                onKeyMatchingChange = { newMatching ->
                    onCommandUpdated(command.copy(keyMatching = newMatching))
                }
            )
        }

        item {
            // Response Mapping Section
            HsmResponseMappingCard(
                responseMapping = command.responseMapping,
                onResponseMappingChange = { newMapping ->
                    onCommandUpdated(command.copy(responseMapping = newMapping))
                }
            )
        }
    }
}

@Composable
private fun HsmKeyMatchingCard(
    keyMatching: KeyMatching,
    onKeyMatchingChange: (KeyMatching) -> Unit
) {
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Key Matching",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                Switch(
                    checked = keyMatching.enabled,
                    onCheckedChange = {
                        onKeyMatchingChange(keyMatching.copy(enabled = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary
                    )
                )
            }

            if (keyMatching.enabled) {
                // Priority setting
                HsmPrioritySelector(
                    priority = keyMatching.priority,
                    onPriorityChange = {
                        onKeyMatchingChange(keyMatching.copy(priority = it))
                    }
                )

                Divider()

                // Key ID matching
                HsmKeyIdMatchingSection(
                    keyIdMatching = keyMatching.keyIdMatching,
                    onKeyIdMatchingChange = {
                        onKeyMatchingChange(keyMatching.copy(keyIdMatching = it))
                    }
                )

                Divider()

                // Session matchers
                HsmSessionMatchersSection(
                    sessionMatchers = keyMatching.sessionMatching,
                    onSessionMatchersChange = {
                        onKeyMatchingChange(keyMatching.copy(sessionMatching = it))
                    }
                )
            } else {
                Text(
                    text = "Enable key matching to configure advanced HSM command routing based on key identifiers and session attributes.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun HsmResponseMappingCard(
    responseMapping: HsmResponseMapping,
    onResponseMappingChange: (HsmResponseMapping) -> Unit
) {
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Response Mapping",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                Switch(
                    checked = responseMapping.enabled,
                    onCheckedChange = {
                        onResponseMappingChange(responseMapping.copy(enabled = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary
                    )
                )
            }

            if (responseMapping.enabled) {
                // Response fields
                HsmResponseFieldsSection(
                    responseFields = responseMapping.responseFields,
                    onResponseFieldsChange = {
                        onResponseMappingChange(responseMapping.copy(responseFields = it))
                    }
                )
            } else {
                Text(
                    text = "Enable response mapping to customize how HSM command responses are formatted and which parameters are included.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun HsmPrioritySelector(
    priority: Int,
    onPriorityChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Priority Level",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Higher priority commands are matched first",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { onPriorityChange(maxOf(0, priority - 1)) }
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease Priority")
            }

            Surface(
                color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = priority.toString(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }

            IconButton(
                onClick = { onPriorityChange(priority + 1) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase Priority")
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun HsmKeyIdMatchingSection(
    keyIdMatching: KeyIdMatching,
    onKeyIdMatchingChange: (KeyIdMatching) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Key ID Matching",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )

            Switch(
                checked = keyIdMatching.enabled,
                onCheckedChange = {
                    onKeyIdMatchingChange(keyIdMatching.copy(enabled = it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colors.primary
                )
            )
        }

        if (keyIdMatching.enabled) {
            // Key Type Selection
            Text(
                text = "Key Type:",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(listOf("AES", "DES", "RSA", "PIN", "MAC", "KEK")) { keyType ->
                    FilterChip(
                        selected = keyIdMatching.keyType == keyType,
                        onClick = {
                            onKeyIdMatchingChange(keyIdMatching.copy(keyType = keyType))
                        },
                        modifier = Modifier.height(28.dp)
                    ) { Text(keyType, fontSize = 10.sp) }
                }
            }

            // Key ID Input
            OutlinedTextField(
                value = keyIdMatching.keyId,
                onValueChange = {
                    onKeyIdMatchingChange(keyIdMatching.copy(keyId = it))
                },
                label = { Text("Key Identifier") },
                placeholder = { Text("e.g., MASTER_KEY_001") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.VpnKey, contentDescription = "Key ID")
                },
                trailingIcon = {
                    if (keyIdMatching.keyId.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onKeyIdMatchingChange(keyIdMatching.copy(keyId = ""))
                            }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            // Exact Match Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = keyIdMatching.exactMatch,
                    onCheckedChange = {
                        onKeyIdMatchingChange(keyIdMatching.copy(exactMatch = it))
                    }
                )
                Text(
                    text = "Exact key ID match (uncheck for pattern matching with *)",
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}

@Composable
private fun HsmSessionMatchersSection(
    sessionMatchers: List<SessionMatcher>,
    onSessionMatchersChange: (List<SessionMatcher>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Session Attribute Matching",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    onSessionMatchersChange(
                        sessionMatchers + SessionMatcher(
                            attribute = "",
                            value = "",
                            operator = HsmMatchOperator.EQUALS
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Matcher")
            }
        }

        if (sessionMatchers.isEmpty()) {
            Text(
                text = "No session attribute matchers configured. Add matchers to check specific session attributes.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        } else {
            sessionMatchers.forEachIndexed { index, sessionMatcher ->
                HsmSessionMatcherRow(
                    matcher = sessionMatcher,
                    onMatcherChange = { newMatcher ->
                        val updatedList = sessionMatchers.toMutableList()
                        updatedList[index] = newMatcher
                        onSessionMatchersChange(updatedList)
                    },
                    onRemove = {
                        onSessionMatchersChange(
                            sessionMatchers.filterIndexed { i, _ -> i != index }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HsmSessionMatcherRow(
    matcher: SessionMatcher,
    onMatcherChange: (SessionMatcher) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Session Matcher ${if (matcher.attribute.isNotBlank()) "- ${matcher.attribute}" else ""}",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attribute input
                OutlinedTextField(
                    value = matcher.attribute,
                    onValueChange = { onMatcherChange(matcher.copy(attribute = it)) },
                    label = { Text("Attribute") },
                    placeholder = { Text("e.g., session.user_id") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Operator selection
                var operatorExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        onClick = { if (!operatorExpanded) operatorExpanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = if (!operatorExpanded) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(
                                alpha = 0.4f
                            )
                        )
                    ) {
                        Text(matcher.operator.displayName)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Operator"
                        )
                    }
                    DropdownMenu(
                        expanded = operatorExpanded,
                        onDismissRequest = { operatorExpanded = false }
                    ) {
                        HsmMatchOperator.values().forEach { operator ->
                            DropdownMenuItem(
                                onClick = {
                                    onMatcherChange(matcher.copy(operator = operator))
                                    operatorExpanded = false
                                }
                            ) {
                                Text("${operator.displayName} (${operator.symbol})")
                            }
                        }
                    }
                }

                // Value input
                OutlinedTextField(
                    value = matcher.value,
                    onValueChange = { onMatcherChange(matcher.copy(value = it)) },
                    label = { Text("Value") },
                    placeholder = { Text("Expected value") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun HsmResponseFieldsSection(
    responseFields: List<HsmResponseField>,
    onResponseFieldsChange: (List<HsmResponseField>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Response Parameter Mappings",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    onResponseFieldsChange(
                        responseFields + HsmResponseField(
                            value = "",
                            targetParameter = "",
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Response Field")
            }
        }

        if (responseFields.isEmpty()) {
            Text(
                text = "No response parameter mappings configured. Add mappings to transform command parameters into response.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        } else {
            responseFields.forEachIndexed { index, field ->
                HsmResponseFieldRow(
                    responseField = field,
                    onResponseFieldChange = { newField ->
                        val updatedList = responseFields.toMutableList()
                        updatedList[index] = newField
                        onResponseFieldsChange(updatedList)
                    },
                    onRemove = {
                        onResponseFieldsChange(
                            responseFields.filterIndexed { i, _ -> i != index }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HsmResponseFieldRow(
    responseField: HsmResponseField,
    onResponseFieldChange: (HsmResponseField) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Parameter Mapping",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Target parameter
                OutlinedTextField(
                    value = responseField.targetParameter ?: "",
                    onValueChange = { onResponseFieldChange(responseField.copy(targetParameter = it.takeIf { it.isNotBlank() })) },
                    label = { Text("Parameter Path") },
                    placeholder = { Text("e.g., status.code") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Value
                OutlinedTextField(
                    value = responseField.value,
                    onValueChange = { onResponseFieldChange(responseField.copy(value = it)) },
                    label = { Text("Value") },
                    placeholder = { Text("e.g., 00, [KEY_ID]") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun EmptyHsmSelectionPanel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )

            Text(
                text = "Select an HSM Command",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Text(
                text = "Choose an HSM command from the list to view and edit its parameters and configuration",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EnhancedAddHsmCommandDialog(
    command: HsmCommand? = null,
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (HsmCommand) -> Unit,
    hsm: HsmServiceImpl
) {
    var commandCode by remember { mutableStateOf(command?.commandCode ?: "") }
    var commandName by remember { mutableStateOf(command?.commandName ?: "") }
    var description by remember { mutableStateOf(command?.description ?: "") }
    var keyMatching by remember {
        mutableStateOf(
            command?.keyMatching ?: KeyMatching()
        )
    }
    var responseMapping by remember {
        mutableStateOf(
            command?.responseMapping ?: HsmResponseMapping()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    color = MaterialTheme.colors.primary,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditing) "Edit HSM Command" else "Create New HSM Command",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        IconButton(
                            onClick = onDismiss
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HsmCommandBasicInformationCard(
                            description = description,
                            onDescriptionChange = { description = it },
                            commandCode = commandCode,
                            onCommandCodeChange = { commandCode = it },
                            commandName = commandName,
                            onCommandNameChange = { commandName = it }
                        )
                    }

                    item {
                        HsmKeyConfigurationCard(
                            keyMatching = keyMatching,
                            onKeyMatchingChange = { keyMatching = it }
                        )
                    }

                    item {
                        HsmResponseConfigurationCard(
                            responseMapping = responseMapping,
                            onResponseMappingChange = { responseMapping = it }
                        )
                    }
                }

                // Footer Actions
                Surface(
                    color = MaterialTheme.colors.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val newCommand = HsmCommand(
                                    id = command?.id ?: generateHsmCommandId(),
                                    commandCode = commandCode,
                                    commandName = commandName,
                                    description = description,
                                    parameters = command?.parameters?.toMutableList()
                                        ?: getDefaultHsmParameters(commandCode).toMutableList(),
                                    keyMatching = keyMatching,
                                    responseMapping = responseMapping
                                )
                                onSave(newCommand)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = description.isNotBlank() && commandCode.isNotBlank()
                        ) {
                            Icon(
                                if (isEditing) Icons.Default.Save else Icons.Default.Add,
                                contentDescription = if (isEditing) "Update" else "Create"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEditing) "Update" else "Create")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HsmCommandBasicInformationCard(
    description: String,
    onDescriptionChange: (String) -> Unit,
    commandCode: String,
    onCommandCodeChange: (String) -> Unit,
    commandName: String,
    onCommandNameChange: (String) -> Unit
) {
    Card(elevation = 2.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description *") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter HSM command description") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = "Description")
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commandCode,
                    onValueChange = onCommandCodeChange,
                    label = { Text("Command Code *") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("A0") },
                    leadingIcon = {
                        Icon(Icons.Default.Code, contentDescription = "Command Code")
                    }
                )

                OutlinedTextField(
                    value = commandName,
                    onValueChange = onCommandNameChange,
                    label = { Text("Command Name") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Generate Key") },
                    leadingIcon = {
                        Icon(Icons.Default.Security, contentDescription = "Command Name")
                    }
                )
            }
        }
    }
}

@Composable
private fun HsmKeyConfigurationCard(
    keyMatching: KeyMatching,
    onKeyMatchingChange: (KeyMatching) -> Unit
) {
    Card(elevation = 2.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Key Matching",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                Switch(
                    checked = keyMatching.enabled,
                    onCheckedChange = {
                        onKeyMatchingChange(keyMatching.copy(enabled = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary
                    )
                )
            }

            if (keyMatching.enabled) {
                Text(
                    text = "This command will use advanced key matching rules. Configure detailed settings after creation.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun HsmResponseConfigurationCard(
    responseMapping: HsmResponseMapping,
    onResponseMappingChange: (HsmResponseMapping) -> Unit
) {
    Card(elevation = 2.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Response Mapping",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                Switch(
                    checked = responseMapping.enabled,
                    onCheckedChange = {
                        onResponseMappingChange(responseMapping.copy(enabled = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary
                    )
                )
            }

            if (responseMapping.enabled) {
                Text(
                    text = "Custom response mapping will be applied. Configure detailed parameter mappings after creation.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun HsmCommandInformationDialog(onCloseRequest: () -> Unit) {
    Dialog(
        onDismissRequest = onCloseRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HSM Command Information",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "This section allows you to define and manage custom HSM (Hardware Security Module) commands and their expected responses. You can simulate various cryptographic operations, key management functions, and data processing tasks.",
                            style = MaterialTheme.typography.body1,
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        Text(
                            text = "Key Features:",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    item { HsmInfoBullet("Define custom HSM command codes (e.g., 'A0' for Generate Key).") }
                    item { HsmInfoBullet("Specify input parameters for each command, including their type, format (Hex, ASCII, etc.), and maximum length.") }
                    item { HsmInfoBullet("Configure key matching rules to simulate specific key identifiers and attributes.") }
                    item { HsmInfoBullet("Map command parameters to dynamic response fields to create realistic HSM responses.") }
                    item { HsmInfoBullet("Import and export HSM command configurations for easy sharing and backup.") }
                    item {
                        Text(
                            text = "Usage Tips:",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    item { HsmInfoBullet("Use the 'Quick Add Parameters' bar to quickly add common parameters for selected command codes.") }
                    item { HsmInfoBullet("Set 'Priority Level' for key matching rules to define the order of matching precedence.") }
                    item { HsmInfoBullet("Leverage 'Session Attribute Matching' for advanced routing based on dynamic session data.") }
                    item { HsmInfoBullet("Use placeholders like `[KEY_ID]` in response fields to dynamically inject values from the request.") }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onCloseRequest) {
                    Text("Got It!")
                }
            }
        }
    }
}

@Composable
private fun HsmInfoBullet(text: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Text("• ", style = MaterialTheme.typography.body2, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.body2)
    }
}

@Composable
private fun HsmDeleteConfirmationDialog(
    command: HsmCommand,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete HSM Command") },
        text = {
            Text(
                "Are you sure you want to delete the command '${command.description}' (${command.commandCode})?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EnhancedAddHsmParameterDialog(
    commandCode: String,
    existingParameters: Set<String>,
    onSave: (List<HsmParameterType>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedParameters by remember { mutableStateOf<Set<HsmParameterType>>(emptySet()) }
    val allParameters = HsmParameterType.values()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Parameters") },
        text = {
            LazyColumn {
                items(allParameters) { paramType ->
                    val isExisting = existingParameters.contains(paramType.displayName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isExisting) {
                                if (!isExisting) {
                                    selectedParameters = if (selectedParameters.contains(paramType)) {
                                        selectedParameters - paramType
                                    } else {
                                        selectedParameters + paramType
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedParameters.contains(paramType) || isExisting,
                            onCheckedChange = null, // Controlled by row click
                            enabled = !isExisting
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = paramType.displayName,
                            color = if (isExisting) MaterialTheme.colors.onSurface.copy(alpha = 0.5f) else MaterialTheme.colors.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedParameters.toList())
                },
                enabled = selectedParameters.isNotEmpty()
            ) {
                Text("Add Selected")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// --- Helper Functions (keep them private as they are internal logic) ---

private fun generateHsmCommandId(): String {
    return "hsm_cmd_${System.currentTimeMillis()}"
}

private fun getDefaultHsmParameters(commandCode: String): List<HsmParameter> {
    return when (commandCode.uppercase()) {
        "A0" -> listOf( // Generate Key
            HsmParameter(
                name = "Key Type",
                type = HsmParameterType.KEY_ID,
                required = true,
                maxLength = 2,
                format = HsmDataFormat.ASCII,
                description = "Type of key to generate (e.g., 00 for ZMK, 01 for ZPK)"
            ),
            HsmParameter(
                name = "Key Scheme",
                type = HsmParameterType.KEY_DATA,
                required = true,
                maxLength = 1,
                format = HsmDataFormat.ASCII,
                description = "Key scheme (e.g., X for X9.19, U for UDK)"
            ),
            HsmParameter(
                name = "LMK ID",
                type = HsmParameterType.KEY_ID,
                required = false,
                maxLength = 4,
                format = HsmDataFormat.HEX,
                description = "LMK Identifier"
            ),
            HsmParameter(
                name = "Error Code",
                type = HsmParameterType.ERROR_CODE,
                required = false,
                maxLength = 2,
                format = HsmDataFormat.HEX,
                description = "HSM Error Code"
            )
        )

        "A2" -> listOf( // Encrypt Data
            HsmParameter(
                name = "Key Identifier",
                type = HsmParameterType.KEY_ID,
                required = true,
                maxLength = 32,
                format = HsmDataFormat.HEX,
                description = "Identifier of the key to use for encryption"
            ),
            HsmParameter(
                name = "Data to Encrypt",
                type = HsmParameterType.DATA_BLOCK,
                required = true,
                format = HsmDataFormat.HEX,
                description = "Data block to be encrypted"
            ),
            HsmParameter(
                name = "Block Format",
                type = HsmParameterType.COMMAND_DATA,
                required = false,
                maxLength = 2,
                format = HsmDataFormat.ASCII,
                description = "Format of the data block"
            ),
            HsmParameter(
                name = "Initialization Vector",
                type = HsmParameterType.RANDOM_DATA,
                required = false,
                maxLength = 16,
                format = HsmDataFormat.HEX,
                description = "Optional IV for CBC mode"
            ),
            HsmParameter(
                name = "Error Code",
                type = HsmParameterType.ERROR_CODE,
                required = false,
                maxLength = 2,
                format = HsmDataFormat.HEX,
                description = "HSM Error Code"
            )
        )

        "A4" -> listOf( // Decrypt Data
            HsmParameter(
                name = "Key Identifier",
                type = HsmParameterType.KEY_ID,
                required = true,
                maxLength = 32,
                format = HsmDataFormat.HEX,
                description = "Identifier of the key to use for decryption"
            ),
            HsmParameter(
                name = "Data to Decrypt",
                type = HsmParameterType.DATA_BLOCK,
                required = true,
                format = HsmDataFormat.HEX,
                description = "Data block to be decrypted"
            ),
            HsmParameter(
                name = "Block Format",
                type = HsmParameterType.COMMAND_DATA,
                required = false,
                maxLength = 2,
                format = HsmDataFormat.ASCII,
                description = "Format of the data block"
            ),
            HsmParameter(
                name = "Initialization Vector",
                type = HsmParameterType.RANDOM_DATA,
                required = false,
                maxLength = 16,
                format = HsmDataFormat.HEX,
                description = "Optional IV for CBC mode"
            ),
            HsmParameter(
                name = "Error Code",
                type = HsmParameterType.ERROR_CODE,
                required = false,
                maxLength = 2,
                format = HsmDataFormat.HEX,
                description = "HSM Error Code"
            )
        )

        "DA" -> listOf( // Verify PIN
            HsmParameter(
                name = "PIN Block",
                type = HsmParameterType.PIN_BLOCK,
                required = true,
                maxLength = 16,
                format = HsmDataFormat.HEX,
                description = "PIN block to verify"
            ),
            HsmParameter(
                name = "PIN Key",
                type = HsmParameterType.KEY_ID,
                required = true,
                maxLength = 32,
                format = HsmDataFormat.HEX,
                description = "Key used to encrypt PIN block"
            ),
            HsmParameter(
                name = "Account Number",
                type = HsmParameterType.COMMAND_DATA,
                required = true,
                maxLength = 19,
                format = HsmDataFormat.ASCII,
                description = "Primary Account Number (PAN)"
            ),
            HsmParameter(
                name = "PIN Validation Data",
                type = HsmParameterType.DATA_BLOCK,
                required = false,
                format = HsmDataFormat.HEX,
                description = "Data used for PIN validation (e.g., PVV, natural PIN)"
            ),
            HsmParameter(
                name = "Error Code",
                type = HsmParameterType.ERROR_CODE,
                required = false,
                maxLength = 2,
                format = HsmDataFormat.HEX,
                description = "HSM Error Code"
            )
        )

        "CA" -> listOf( // Translate PIN
            HsmParameter(
                name = "Input PIN Block",
                type = HsmParameterType.PIN_BLOCK,
                required = true,
                maxLength = 16,
                format = HsmDataFormat.HEX,
                description = "PIN block under source key"
            ),
            HsmParameter(
                name = "Source PIN Key",
                type = HsmParameterType.KEY_ID,
                required = true,
                maxLength = 32,
                format = HsmDataFormat.HEX,
                description = "Key used for the input PIN block"
            ),
            HsmParameter(
                name = "Destination PIN Key",
                type = HsmParameterType.KEY_ID,
                required = true,
                maxLength = 32,
                format = HsmDataFormat.HEX,
                description = "Key to translate the PIN block to"
            ),
            HsmParameter(
                name = "Account Number",
                type = HsmParameterType.COMMAND_DATA,
                required = true,
                maxLength = 19,
                format = HsmDataFormat.ASCII,
                description = "Primary Account Number (PAN)"
            ),
            HsmParameter(
                name = "Error Code",
                type = HsmParameterType.ERROR_CODE,
                required = false,
                maxLength = 2,
                format = HsmDataFormat.HEX,
                description = "HSM Error Code"
            )
        )

        "M0" -> listOf( // Generate MAC
            HsmParameter(
                name = "MAC Key",
                type = HsmParameterType.KEY_ID,
                required = true,
                maxLength = 32,
                format = HsmDataFormat.HEX,
                description = "Key to use for MAC generation"
            ),
            HsmParameter(
                name = "MAC Data",
                type = HsmParameterType.MAC_DATA,
                required = true,
                format = HsmDataFormat.HEX,
                description = "Data over which to compute the MAC"
            ),
            HsmParameter(
                name = "Error Code",
                type = HsmParameterType.ERROR_CODE,
                required = false,
                maxLength = 2,
                format = HsmDataFormat.HEX,
                description = "HSM Error Code"
            )
        )

        else -> listOf(
            HsmParameter(
                name = "Command Data",
                type = HsmParameterType.COMMAND_DATA,
                required = true,
                format = HsmDataFormat.HEX,
                description = "Full command data payload"
            ),
            HsmParameter(
                name = "Error Code",
                type = HsmParameterType.ERROR_CODE,
                required = false,
                maxLength = 2,
                format = HsmDataFormat.HEX,
                description = "HSM Error Code"
            )
        )
    }
}

private fun isRequiredParameter(type: HsmParameterType): Boolean {
    return when (type) {
        HsmParameterType.KEY_ID,
        HsmParameterType.KEY_DATA,
        HsmParameterType.DATA_BLOCK,
        HsmParameterType.PIN_BLOCK,
        HsmParameterType.MAC_DATA,
        HsmParameterType.COMMAND_DATA -> true
        else -> false
    }
}

private fun getParameterDescription(type: HsmParameterType): String {
    return when (type) {
        HsmParameterType.KEY_ID -> "Identifier for a cryptographic key"
        HsmParameterType.KEY_DATA -> "The actual cryptographic key material"
        HsmParameterType.DATA_BLOCK -> "A block of data for processing"
        HsmParameterType.PIN_BLOCK -> "An encrypted Personal Identification Number"
        HsmParameterType.MAC_DATA -> "Data for Message Authentication Code generation/verification"
        HsmParameterType.CERTIFICATE -> "A digital certificate in PEM or DER format"
        HsmParameterType.SESSION_ID -> "Identifier for the current communication session"
        HsmParameterType.COMMAND_DATA -> "A generic block of command data"
        HsmParameterType.STATUS_CODE -> "The status code of the HSM operation"
        HsmParameterType.ERROR_CODE -> "A code indicating an error condition"
        HsmParameterType.TIMESTAMP -> "A timestamp for the operation"
        HsmParameterType.RANDOM_DATA -> "Randomly generated data, like an IV or nonce"
    }
}

private fun getParameterMaxLength(type: HsmParameterType): Int {
    return when (type) {
        HsmParameterType.KEY_ID -> 64
        HsmParameterType.PIN_BLOCK -> 16
        HsmParameterType.STATUS_CODE, HsmParameterType.ERROR_CODE -> 2
        HsmParameterType.TIMESTAMP -> 14
        HsmParameterType.KEY_DATA, HsmParameterType.DATA_BLOCK, HsmParameterType.MAC_DATA -> 2048
        else -> 0 // 0 for variable length
    }
}

private fun getParameterFormat(type: HsmParameterType): HsmDataFormat {
    return when (type) {
        HsmParameterType.CERTIFICATE -> HsmDataFormat.PEM
        HsmParameterType.TIMESTAMP -> HsmDataFormat.ASCII
        else -> HsmDataFormat.HEX
    }
}

private fun getParameterPlaceholder(type: HsmParameterType): String {
    return when (type) {
        HsmParameterType.KEY_ID -> "e.g., KEK_01_A"
        HsmParameterType.KEY_DATA -> "e.g., 1122334455667788AABBCCDDEEFF0011"
        HsmParameterType.DATA_BLOCK -> "e.g., 0123456789ABCDEF..."
        HsmParameterType.PIN_BLOCK -> "e.g., 1234567890123456"
        HsmParameterType.MAC_DATA -> "e.g., Data to be authenticated..."
        HsmParameterType.CERTIFICATE -> "-----BEGIN CERTIFICATE-----..."
        HsmParameterType.SESSION_ID -> "e.g., session-xyz-123"
        HsmParameterType.COMMAND_DATA -> "e.g., A00101X..."
        HsmParameterType.STATUS_CODE -> "e.g., 00 for success"
        HsmParameterType.ERROR_CODE -> "e.g., 01 for error"
        HsmParameterType.TIMESTAMP -> "e.g., YYYYMMDDHHMMSS"
        HsmParameterType.RANDOM_DATA -> "e.g., F0F1F2F3F4F5F6F7"
    }
}

private fun validateParameterFormat(value: String, format: HsmDataFormat): Boolean {
    if (value.isEmpty()) return true
    return when (format) {
        HsmDataFormat.HEX -> value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' } && value.length % 2 == 0
        HsmDataFormat.ASCII -> value.all { it.code in 32..126 }
        HsmDataFormat.BASE64 -> try {
            java.util.Base64.getDecoder().decode(value)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
        HsmDataFormat.PEM -> value.startsWith("-----BEGIN") && value.contains("-----END")
        HsmDataFormat.DER, HsmDataFormat.BINARY -> true // More complex validation needed
    }
}
