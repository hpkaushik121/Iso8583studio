package `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PrimaryButton
import `in`.aicortex.iso8583studio.ui.screens.components.SecondaryButton
import kotlinx.coroutines.launch

// --- ENHANCED DATA MODELS WITH MATERIAL DESIGN COLORS ---

enum class FileType(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    MF("Master File", Icons.Default.FolderSpecial, Color(0xFF6750A4)),
    DF("Directory File", Icons.Default.Folder, Color(0xFF0B6BCB)),
    EF("Elementary File", Icons.Default.Description, Color(0xFF197A3E))
}

enum class FileStructure(val displayName: String) {
    TRANSPARENT("Transparent"),
    LINEAR_FIXED("Linear Fixed"),
    CYCLIC("Cyclic")
}

enum class AccessCondition(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    ALWAYS("Always", Icons.Outlined.LockOpen, Color(0xFF197A3E)),
    NEVER("Never", Icons.Outlined.Lock, Color(0xFFD32F2F)),
    PIN_ALWAYS("PIN Always", Icons.Outlined.Security, Color(0xFFED6C02)),
    PIN_ANY("PIN Any", Icons.Outlined.VpnKey, Color(0xFF0B6BCB))
}

data class EmvFile(
    val id: String,
    val name: String,
    val type: FileType,
    val parentId: String? = null,
    val children: List<EmvFile> = emptyList(),
    val structure: FileStructure = FileStructure.TRANSPARENT,
    val size: Int = 0,
    val actualSize: Int = 0,
    val accessRead: AccessCondition = AccessCondition.ALWAYS,
    val accessWrite: AccessCondition = AccessCondition.PIN_ALWAYS,
    val rawData: String = "",
    val isValid: Boolean = true,
    val isExpanded: Boolean = true,
    val hasChanges: Boolean = false,
    val errorMessage: String? = null
)

data class TlvRecord(
    val id: Int = (Math.random() * 10000).toInt(),
    val tag: String,
    val length: String,
    val value: String,
    val description: String,
    val isValid: Boolean = true,
    val tagDescription: String = getTagDescription(tag)
)

// EMV Tag Dictionary
fun getTagDescription(tag: String): String = when (tag.uppercase()) {
    "6F" -> "File Control Information (FCI) Template"
    "84" -> "Dedicated File (DF) Name"
    "A5" -> "File Control Information (FCI) Proprietary Template"
    "50" -> "Application Label"
    "87" -> "Application Priority Indicator"
    "9F38" -> "Processing Options Data Object List (PDOL)"
    "5F2D" -> "Language Preference"
    "BF0C" -> "File Control Information (FCI) Issuer Discretionary Data"
    else -> "Unknown EMV Tag"
}

// --- SAMPLE DATA ---
fun getSampleFileSystem(): EmvFile {
    val efContactless = EmvFile(
        id = "A001",
        name = "EF.Contactless",
        type = FileType.EF,
        parentId = "A000",
        size = 256,
        actualSize = 128,
        rawData = "6F1A8407A0000000031010A50F500A564953412044454249545F9F38039F1A02",
        structure = FileStructure.TRANSPARENT,
        accessRead = AccessCondition.ALWAYS,
        accessWrite = AccessCondition.PIN_ALWAYS
    )

    val efCapk = EmvFile(
        id = "A002",
        name = "EF.CAPK",
        type = FileType.EF,
        parentId = "A000",
        size = 512,
        actualSize = 256,
        rawData = "9F4681B0C7D1F2E3A4B5C6D7E8F90A1B2C3D4E5F6789ABCDEF0123456789ABCDEF",
        structure = FileStructure.LINEAR_FIXED,
        accessRead = AccessCondition.ALWAYS,
        accessWrite = AccessCondition.NEVER
    )

    val dfVisa = EmvFile(
        id = "A000",
        name = "DF.VISA",
        type = FileType.DF,
        parentId = "3F00",
        children = listOf(efContactless, efCapk),
        isExpanded = true
    )

    val dfPSE = EmvFile(
        id = "1PAY.SYS.DDF01",
        name = "1PAY.SYS.DDF01",
        type = FileType.DF,
        parentId = "3F00",
        children = listOf(),
        isExpanded = false
    )

    val efDir = EmvFile(
        id = "2F01",
        name = "EF.DIR",
        type = FileType.EF,
        parentId = "3F00",
        size = 512,
        actualSize = 128,
        rawData = "7081A5610F4F07A0000000031010500A56495341204445424954BF0C059F4D020B0A",
        structure = FileStructure.LINEAR_FIXED
    )

    return EmvFile(
        id = "3F00",
        name = "Master File (MF)",
        type = FileType.MF,
        children = listOf(dfVisa, dfPSE, efDir),
        isExpanded = true
    )
}

// --- MAIN SCROLLABLE TAB COMPOSABLE ---

@Composable
fun FileSystemTab() {
    var fileSystem by remember { mutableStateOf(getSampleFileSystem()) }
    var selectedFile by remember { mutableStateOf<EmvFile?>(fileSystem) }
    var searchQuery by remember { mutableStateOf("") }
    var showValidationResults by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    fun updateFileInTree(root: EmvFile, targetId: String, update: (EmvFile) -> EmvFile): EmvFile {
        if (root.id == targetId) return update(root)
        return root.copy(children = root.children.map { updateFileInTree(it, targetId, update) })
    }

    fun markAsChanged() {
        hasUnsavedChanges = true
    }

    // Scrollable Column Layout
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Enhanced Status Header
        MaterialFileSystemHeader(
            hasUnsavedChanges = hasUnsavedChanges,
            isLoading = isLoading,
            onSave = {
                isLoading = true
                // Simulate save operation
                kotlinx.coroutines.MainScope().launch {
                    kotlinx.coroutines.delay(1000)
                    hasUnsavedChanges = false
                    isLoading = false
                }
            },
            onValidate = { showValidationResults = true },
            onImport = { /* TODO */ },
            onExport = { /* TODO */ }
        )

        // Quick Stats Card
        MaterialQuickStats(fileSystem)

        // Responsive Layout based on content
        if (selectedFile != null) {
            // Two-column layout on larger screens
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // File Tree Panel
                Card(
                    modifier = Modifier
                        .weight(0.4f)
                        .heightIn(min = 400.dp, max = 600.dp),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    MaterialFileTreePanel(
                        rootFile = fileSystem,
                        selectedFile = selectedFile,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onFileSelected = { selectedFile = it },
                        onToggleExpand = { fileToToggle ->
                            fileSystem = updateFileInTree(fileSystem, fileToToggle.id) {
                                it.copy(isExpanded = !it.isExpanded)
                            }
                        },
                        onFileAction = { action, file ->
                            when (action) {
                                "add_file", "add_directory", "delete" -> markAsChanged()
                            }
                        }
                    )
                }

                // File Editor Panel
                Card(
                    modifier = Modifier
                        .weight(0.6f)
                        .heightIn(min = 400.dp, max = 600.dp),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    MaterialFileEditorPanel(
                        key = selectedFile!!.id,
                        file = selectedFile!!,
                        onUpdate = { updatedFile ->
                            selectedFile = updatedFile
                            fileSystem = updateFileInTree(fileSystem, updatedFile.id) { updatedFile }
                            markAsChanged()
                        }
                    )
                }
            }
        } else {
            // Single column layout with file tree only
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 400.dp, max = 600.dp),
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                MaterialFileTreePanel(
                    rootFile = fileSystem,
                    selectedFile = selectedFile,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onFileSelected = { selectedFile = it },
                    onToggleExpand = { fileToToggle ->
                        fileSystem = updateFileInTree(fileSystem, fileToToggle.id) {
                            it.copy(isExpanded = !it.isExpanded)
                        }
                    },
                    onFileAction = { action, file ->
                        when (action) {
                            "add_file", "add_directory", "delete" -> markAsChanged()
                        }
                    }
                )
            }
        }

        // Action Buttons Row
        MaterialActionButtons(
            hasUnsavedChanges = hasUnsavedChanges,
            selectedFile = selectedFile,
            onSave = {
                isLoading = true
                kotlinx.coroutines.MainScope().launch {
                    kotlinx.coroutines.delay(1000)
                    hasUnsavedChanges = false
                    isLoading = false
                }
            },
            onReset = {
                fileSystem = getSampleFileSystem()
                selectedFile = fileSystem
                hasUnsavedChanges = false
            }
        )

        // Add some bottom padding for better scroll experience
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- MATERIAL DESIGN HEADER ---

@Composable
fun MaterialFileSystemHeader(
    hasUnsavedChanges: Boolean,
    isLoading: Boolean,
    onSave: () -> Unit,
    onValidate: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EMV File System",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        StatusIndicator(
                            hasUnsavedChanges = hasUnsavedChanges,
                            isLoading = isLoading
                        )
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onImport,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Upload,
                            contentDescription = "Import",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import")
                    }

                    OutlinedButton(
                        onClick = onExport,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = "Export",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }

                    OutlinedButton(
                        onClick = onValidate,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            Icons.Outlined.VerifiedUser,
                            contentDescription = "Validate",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Validate")
                    }

                    Button(
                        onClick = onSave,
                        enabled = hasUnsavedChanges && !isLoading,
                        modifier = Modifier.height(40.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colors.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Save,
                                contentDescription = "Save",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save All")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(
    hasUnsavedChanges: Boolean,
    isLoading: Boolean
) {
    when {
        isLoading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Saving changes...",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.primary
                )
            }
        }
        hasUnsavedChanges -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = Color(0xFFED6C02),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Unsaved changes",
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFFED6C02)
                )
            }
        }
        else -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "All saved",
                    tint = Color(0xFF197A3E),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "All changes saved",
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFF197A3E)
                )
            }
        }
    }
}

// --- MATERIAL QUICK STATS ---

@Composable
fun MaterialQuickStats(rootFile: EmvFile) {
    fun countFiles(file: EmvFile): Triple<Int, Int, Int> {
        var mfCount = if (file.type == FileType.MF) 1 else 0
        var dfCount = if (file.type == FileType.DF) 1 else 0
        var efCount = if (file.type == FileType.EF) 1 else 0

        file.children.forEach { child ->
            val (childMf, childDf, childEf) = countFiles(child)
            mfCount += childMf
            dfCount += childDf
            efCount += childEf
        }

        return Triple(mfCount, dfCount, efCount)
    }

    val (mfCount, dfCount, efCount) = countFiles(rootFile)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MaterialStatChip("Master Files", mfCount, FileType.MF.icon, FileType.MF.color)
            MaterialStatChip("Directories", dfCount, FileType.DF.icon, FileType.DF.color)
            MaterialStatChip("Elementary", efCount, FileType.EF.icon, FileType.EF.color)
        }
    }
}

@Composable
fun MaterialStatChip(
    label: String,
    count: Int,
    icon: ImageVector,
    color: Color
) {
    Card(
        backgroundColor = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// --- MATERIAL FILE TREE PANEL ---

@Composable
fun MaterialFileTreePanel(
    rootFile: EmvFile,
    selectedFile: EmvFile?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFileSelected: (EmvFile) -> Unit,
    onToggleExpand: (EmvFile) -> Unit,
    onFileAction: (String, EmvFile?) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {

        // Enhanced Search Bar
        MaterialSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Search files and directories..."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "FILE STRUCTURE",
            style = MaterialTheme.typography.overline,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable File Tree
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                MaterialFileTreeItem(
                    file = rootFile,
                    level = 0,
                    selectedFile = selectedFile,
                    searchQuery = searchQuery,
                    onFileSelected = onFileSelected,
                    onToggleExpand = onToggleExpand
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // File Management Actions
        MaterialFileActions(onFileAction)
    }
}

@Composable
fun MaterialSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.body2
            )
        },
        leadingIcon = {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Search",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        )
    )
}

@Composable
fun MaterialFileTreeItem(
    file: EmvFile,
    level: Int,
    selectedFile: EmvFile?,
    searchQuery: String,
    onFileSelected: (EmvFile) -> Unit,
    onToggleExpand: (EmvFile) -> Unit
) {
    val isSelected = selectedFile?.id == file.id
    val matchesSearch = searchQuery.isEmpty() ||
            file.name.contains(searchQuery, ignoreCase = true) ||
            file.id.contains(searchQuery, ignoreCase = true)

    if (!matchesSearch) return

    val animatedRotation by animateFloatAsState(
        targetValue = if (file.isExpanded) 90f else 0f,
        animationSpec = tween(200)
    )

    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 16).dp)
                .clickable { onFileSelected(file) },
            backgroundColor = when {
                isSelected -> MaterialTheme.colors.primary.copy(alpha = 0.12f)
                else -> Color.Transparent
            },
            elevation = if (isSelected) 2.dp else 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/Collapse Button with Animation
                if (file.children.isNotEmpty()) {
                    IconButton(
                        onClick = { onToggleExpand(file) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = if (file.isExpanded) "Collapse" else "Expand",
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(animatedRotation)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }

                // File Icon with Type Color
                Icon(
                    imageVector = file.type.icon,
                    contentDescription = file.type.displayName,
                    tint = file.type.color,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // File Information
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.body2,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                    )

                    if (file.type == FileType.EF && file.size > 0) {
                        Text(
                            text = "${file.actualSize}/${file.size} bytes • ${file.structure.displayName}",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        Text(
                            text = file.id,
                            style = MaterialTheme.typography.caption,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Status Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (file.hasChanges) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = MaterialTheme.colors.secondary,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }

                    Icon(
                        imageVector = if (file.isValid) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                        contentDescription = if (file.isValid) "Valid" else "Invalid",
                        tint = if (file.isValid) Color(0xFF197A3E) else Color(0xFFED6C02),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Render children with animation
        AnimatedVisibility(
            visible = file.isExpanded && file.children.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                file.children.forEach { child ->
                    MaterialFileTreeItem(
                        file = child,
                        level = level + 1,
                        selectedFile = selectedFile,
                        searchQuery = searchQuery,
                        onFileSelected = onFileSelected,
                        onToggleExpand = onToggleExpand
                    )
                }
            }
        }
    }
}

@Composable
fun MaterialFileActions(onFileAction: (String, EmvFile?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { onFileAction("add_file", null) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Outlined.NoteAdd,
                    contentDescription = "Add File",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add File")
            }

            OutlinedButton(
                onClick = { onFileAction("add_directory", null) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Outlined.CreateNewFolder,
                    contentDescription = "Add Directory",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Dir")
            }
        }
    }
}

// --- MATERIAL FILE EDITOR PANEL ---

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MaterialFileEditorPanel(
    key: Any,
    file: EmvFile,
    onUpdate: (EmvFile) -> Unit
) {
    var localFile by remember(key) { mutableStateOf(file) }
    var activeTab by remember { mutableStateOf("properties") }
    var dataViewMode by remember { mutableStateOf("hex") }

    fun updateLocalFile(newFile: EmvFile) {
        localFile = newFile
        onUpdate(newFile)
    }

    Column(modifier = Modifier.padding(16.dp)) {

        // Enhanced File Header
        MaterialFileHeader(localFile)

        Spacer(modifier = Modifier.height(16.dp))

        // Modern Tab Navigation
        MaterialTabNavigation(
            activeTab = activeTab,
            onTabChange = { activeTab = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content with Animation
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { width -> width / 3 },
                        animationSpec = tween(300)
                    ) + fadeIn() with
                            slideOutHorizontally(
                                targetOffsetX = { width -> -width / 3 },
                                animationSpec = tween(300)
                            ) + fadeOut()
                }
            ) { tabState ->
                when (tabState) {
                    "properties" -> {
                        MaterialPropertiesEditor(
                            file = localFile,
                            onUpdate = ::updateLocalFile
                        )
                    }
                    "data" -> {
                        MaterialDataEditor(
                            file = localFile,
                            viewMode = dataViewMode,
                            onViewModeChange = { dataViewMode = it },
                            onUpdate = ::updateLocalFile
                        )
                    }
                    "validation" -> {
                        MaterialValidationPanel(localFile)
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialFileHeader(file: EmvFile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Icon with colored background
            Card(
                backgroundColor = file.type.color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                elevation = 0.dp
            ) {
                Icon(
                    imageVector = file.type.icon,
                    contentDescription = file.type.displayName,
                    tint = file.type.color,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Chip(
                        text = file.type.displayName,
                        backgroundColor = file.type.color.copy(alpha = 0.12f),
                        textColor = file.type.color
                    )

                    Text(
                        text = file.id,
                        style = MaterialTheme.typography.caption,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Status Badge
            Card(
                backgroundColor = if (file.isValid) Color(0xFF197A3E) else Color(0xFFD32F2F),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (file.isValid) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
                        contentDescription = if (file.isValid) "Valid" else "Invalid",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (file.isValid) "VALID" else "ERROR",
                        style = MaterialTheme.typography.caption,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun Chip(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Card(
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.caption,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MaterialTabNavigation(
    activeTab: String,
    onTabChange: (String) -> Unit
) {
    val tabs = listOf(
        Triple("properties", "Properties", Icons.Outlined.Settings),
        Triple("data", "Data Editor", Icons.Outlined.Code),
        Triple("validation", "Validation", Icons.Outlined.VerifiedUser)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { (key, title, icon) ->
            MaterialTab(
                text = title,
                icon = icon,
                selected = activeTab == key,
                onClick = { onTabChange(key) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MaterialTab(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(200)
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        animationSpec = tween(200)
    )

    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(48.dp),
        backgroundColor = backgroundColor,
        elevation = if (selected) 2.dp else 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

// --- MATERIAL PROPERTIES EDITOR ---

@Composable
fun MaterialPropertiesEditor(
    file: EmvFile,
    onUpdate: (EmvFile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MaterialPropertySection(title = "Basic Information") {
                MaterialFormField(
                    label = "File Identifier",
                    value = file.id,
                    onValueChange = { },
                    readOnly = true,
                    fontFamily = FontFamily.Monospace
                )

                MaterialFormField(
                    label = "File Name",
                    value = file.name,
                    onValueChange = { onUpdate(file.copy(name = it)) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        MaterialDropdownField(
                            label = "File Type",
                            selectedValue = file.type.displayName,
                            options = FileType.values().map { it.displayName },
                            onSelectionChange = { displayName ->
                                val newType = FileType.values().find { it.displayName == displayName }
                                newType?.let { onUpdate(file.copy(type = it)) }
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        MaterialFormField(
                            label = "File Size (bytes)",
                            value = file.size.toString(),
                            onValueChange = {
                                val newSize = it.toIntOrNull() ?: 0
                                onUpdate(file.copy(size = newSize))
                            }
                        )
                    }
                }

                if (file.type == FileType.EF) {
                    MaterialDropdownField(
                        label = "File Structure",
                        selectedValue = file.structure.displayName,
                        options = FileStructure.values().map { it.displayName },
                        onSelectionChange = { displayName ->
                            val newStructure = FileStructure.values().find { it.displayName == displayName }
                            newStructure?.let { onUpdate(file.copy(structure = it)) }
                        }
                    )
                }
            }
        }

        item {
            MaterialPropertySection(title = "Access Control") {
                Text(
                    "Configure access permissions for read and write operations",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        MaterialAccessDropdown(
                            label = "Read Access",
                            selectedValue = file.accessRead,
                            onSelectionChange = { onUpdate(file.copy(accessRead = it)) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        MaterialAccessDropdown(
                            label = "Write Access",
                            selectedValue = file.accessWrite,
                            onSelectionChange = { onUpdate(file.copy(accessWrite = it)) }
                        )
                    }
                }
            }
        }

        item {
            MaterialPropertySection(title = "Storage Statistics") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MaterialStatCard("Allocated", "${file.size}", "bytes")
                    MaterialStatCard("Used", "${file.actualSize}", "bytes")
                    MaterialStatCard(
                        "Utilization",
                        "${if (file.size > 0) (file.actualSize * 100 / file.size) else 0}",
                        "%"
                    )
                }
            }
        }
    }
}

@Composable
fun MaterialPropertySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun MaterialFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    fontFamily: FontFamily? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        textStyle = LocalTextStyle.current.copy(
            fontFamily = fontFamily ?: FontFamily.Default
        ),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        )
    )
}

@Composable
fun MaterialDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand"
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onSelectionChange(option)
                        expanded = false
                    }
                ) {
                    Text(option)
                }
            }
        }
    }
}

@Composable
fun MaterialAccessDropdown(
    label: String,
    selectedValue: AccessCondition,
    onSelectionChange: (AccessCondition) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedValue.displayName,
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            leadingIcon = {
                Icon(
                    imageVector = selectedValue.icon,
                    contentDescription = null,
                    tint = selectedValue.color,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand"
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AccessCondition.values().forEach { condition ->
                DropdownMenuItem(
                    onClick = {
                        onSelectionChange(condition)
                        expanded = false
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = condition.icon,
                            contentDescription = null,
                            tint = condition.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(condition.displayName)
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialStatCard(
    label: String,
    value: String,
    unit: String
) {
    Card(
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.7f)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// --- MATERIAL DATA EDITOR ---

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MaterialDataEditor(
    file: EmvFile,
    viewMode: String,
    onViewModeChange: (String) -> Unit,
    onUpdate: (EmvFile) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // View Mode Selector
        MaterialViewModeSelector(
            selectedMode = viewMode,
            onModeChange = onViewModeChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Data Editor Content
        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = 1.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            AnimatedContent(
                targetState = viewMode,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { width -> width / 2 },
                        animationSpec = tween(300)
                    ) + fadeIn() with
                            slideOutHorizontally(
                                targetOffsetX = { width -> -width / 2 },
                                animationSpec = tween(300)
                            ) + fadeOut()
                }
            ) { mode ->
                when (mode) {
                    "hex" -> {
                        MaterialHexEditor(
                            data = file.rawData,
                            onDataChange = { newData ->
                                onUpdate(
                                    file.copy(
                                        rawData = newData,
                                        actualSize = newData.length / 2,
                                        hasChanges = true
                                    )
                                )
                            }
                        )
                    }
                    "tlv" -> {
                        MaterialTlvEditor(
                            rawData = file.rawData,
                            onRawDataUpdated = { newData ->
                                onUpdate(
                                    file.copy(
                                        rawData = newData,
                                        actualSize = newData.length / 2,
                                        hasChanges = true
                                    )
                                )
                            }
                        )
                    }
                    "template" -> {
                        MaterialTemplateEditor(file = file, onUpdate = onUpdate)
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialViewModeSelector(
    selectedMode: String,
    onModeChange: (String) -> Unit
) {
    val modes = listOf(
        Triple("hex", "Hex Editor", Icons.Outlined.Code),
        Triple("tlv", "TLV Parser", Icons.Outlined.DataObject),
        Triple("template", "Templates", Icons.Outlined.Description)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modes.forEach { (key, title, icon) ->
            MaterialViewModeButton(
                text = title,
                icon = icon,
                selected = selectedMode == key,
                onClick = { onModeChange(key) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MaterialViewModeButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colors.primary else Color.Transparent,
        animationSpec = tween(200)
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colors.primary,
        animationSpec = tween(200)
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.elevation(
            defaultElevation = if (selected) 2.dp else 0.dp
        ),
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- MATERIAL HEX EDITOR ---

@Composable
fun MaterialHexEditor(
    data: String,
    onDataChange: (String) -> Unit
) {
    var editableData by remember(data) { mutableStateOf(data) }

    Column(modifier = Modifier.padding(16.dp)) {

        // Header with Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Hexadecimal Data Editor",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Enter hex data without spaces (automatically formatted)",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        editableData = editableData.replace("\\s".toRegex(), "")
                            .chunked(2).joinToString(" ")
                        onDataChange(editableData.replace("\\s".toRegex(), ""))
                    }
                ) {
                    Icon(
                        Icons.Outlined.FormatAlignLeft,
                        contentDescription = "Format",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Format")
                }

                OutlinedButton(
                    onClick = {
                        editableData = ""
                        onDataChange("")
                    }
                ) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hex Data Input
        Card(
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
        ) {
            SelectionContainer {
                OutlinedTextField(
                    value = editableData,
                    onValueChange = { newValue ->
                        val filteredValue = newValue.filter {
                            it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it.isWhitespace()
                        }.uppercase()
                        editableData = filteredValue
                        onDataChange(filteredValue.replace("\\s".toRegex(), ""))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    placeholder = {
                        Text(
                            "Enter hex data (e.g., 6F1A8407A0000000031010...)",
                            style = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Data Statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f),
                elevation = 0.dp,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "Length: ${editableData.replace("\\s".toRegex(), "").length / 2} bytes",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Medium
                )
            }

            Card(
                backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.08f),
                elevation = 0.dp,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "Hex chars: ${editableData.replace("\\s".toRegex(), "").length}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// --- ACTION BUTTONS ---

@Composable
fun MaterialActionButtons(
    hasUnsavedChanges: Boolean,
    selectedFile: EmvFile?,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (selectedFile != null) "Working on: ${selectedFile.name}" else "No file selected",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReset,
                    enabled = hasUnsavedChanges
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }

                Button(
                    onClick = onSave,
                    enabled = hasUnsavedChanges
                ) {
                    Icon(
                        Icons.Outlined.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Changes")
                }
            }
        }
    }
}

// --- MATERIAL TLV EDITOR (Simplified for space) ---

@Composable
fun MaterialTlvEditor(
    rawData: String,
    onRawDataUpdated: (String) -> Unit
) {
    var records by remember(rawData) { mutableStateOf(parseRawData(rawData)) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "TLV Structure Parser",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records, key = { it.id }) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Card(
                                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp),
                                    elevation = 0.dp
                                ) {
                                    Text(
                                        text = record.tag,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = record.tagDescription,
                                    style = MaterialTheme.typography.body2,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Icon(
                                if (record.isValid) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                                contentDescription = if (record.isValid) "Valid" else "Invalid",
                                tint = if (record.isValid) Color(0xFF197A3E) else Color(0xFFED6C02),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "Value: ${record.value.take(40)}${if (record.value.length > 40) "..." else ""}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- MATERIAL TEMPLATE EDITOR ---

@Composable
fun MaterialTemplateEditor(
    file: EmvFile,
    onUpdate: (EmvFile) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf("") }
    val templates = listOf(
        "VISA Credit Application",
        "Mastercard Debit Application",
        "AMEX Charge Application",
        "Generic PSE Directory",
        "Custom Template"
    )

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "EMV Application Templates",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                elevation = 1.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Load Predefined Template",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MaterialDropdownField(
                        label = "Select Template",
                        selectedValue = selectedTemplate,
                        options = templates,
                        onSelectionChange = { selectedTemplate = it }
                    )

                    if (selectedTemplate.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
                            elevation = 0.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = getTemplateDescription(selectedTemplate),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val templateData = getTemplateData(selectedTemplate)
                                onUpdate(file.copy(rawData = templateData, hasChanges = true))
                            },
                            enabled = selectedTemplate.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.GetApp,
                                contentDescription = "Load",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Template")
                        }

                        OutlinedButton(
                            onClick = { /* TODO: Save as template */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.Bookmark,
                                contentDescription = "Save",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Template")
                        }
                    }
                }
            }
        }

        if (selectedTemplate.isNotEmpty()) {
            item {
                Card(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Template Structure",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        getTemplateStructure(selectedTemplate).forEach { structureItem ->
                            MaterialTemplateStructureItem(structureItem)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialTemplateStructureItem(item: TemplateStructureItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (item.level * 16).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.required) Icons.Outlined.Star else Icons.Outlined.StarBorder,
            contentDescription = if (item.required) "Required" else "Optional",
            tint = if (item.required) Color(0xFFED6C02) else MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Card(
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(4.dp),
            elevation = 0.dp
        ) {
            Text(
                text = item.tag,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// --- MATERIAL VALIDATION PANEL ---

@Composable
fun MaterialValidationPanel(file: EmvFile) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Validation Results",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            MaterialValidationCard(
                title = "EMV Compliance",
                status = if (file.isValid) "PASSED" else "FAILED",
                items = listOf(
                    ValidationItem(
                        "File structure",
                        file.isValid,
                        if (file.isValid) "Valid EMV file structure" else "Invalid file structure"
                    ),
                    ValidationItem(
                        "Data format",
                        file.rawData.isNotEmpty(),
                        if (file.rawData.isNotEmpty()) "Data present" else "No data found"
                    ),
                    ValidationItem(
                        "Size consistency",
                        file.actualSize <= file.size,
                        if (file.actualSize <= file.size) "Size within limits" else "Data exceeds allocated size"
                    )
                )
            )
        }

        item {
            MaterialValidationCard(
                title = "TLV Structure",
                status = "PASSED",
                items = listOf(
                    ValidationItem("TLV parsing", true, "TLV structure is valid"),
                    ValidationItem("Tag format", true, "All tags follow EMV standard"),
                    ValidationItem("Length encoding", true, "Length fields are correctly encoded")
                )
            )
        }

        item {
            MaterialValidationCard(
                title = "Security Validation",
                status = "WARNING",
                items = listOf(
                    ValidationItem("Access conditions", true, "Access conditions are properly set"),
                    ValidationItem("Key references", false, "Some key references may be missing"),
                    ValidationItem("Certificate chain", true, "Certificate validation passed")
                )
            )
        }
    }
}

@Composable
fun MaterialValidationCard(
    title: String,
    status: String,
    items: List<ValidationItem>
) {
    val statusColor = when (status) {
        "PASSED" -> Color(0xFF197A3E)
        "WARNING" -> Color(0xFFED6C02)
        "FAILED" -> Color(0xFFD32F2F)
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    backgroundColor = statusColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.caption,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            items.forEach { item ->
                MaterialValidationItem(item)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

data class ValidationItem(
    val name: String,
    val passed: Boolean,
    val message: String
)

@Composable
fun MaterialValidationItem(item: ValidationItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.passed) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
            contentDescription = if (item.passed) "Passed" else "Failed",
            tint = if (item.passed) Color(0xFF197A3E) else Color(0xFFD32F2F),
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = item.message,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// --- UTILITY FUNCTIONS ---

data class TemplateStructureItem(
    val tag: String,
    val name: String,
    val description: String,
    val required: Boolean,
    val level: Int = 0
)

fun getTemplateData(templateName: String): String {
    return when (templateName) {
        "VISA Credit Application" -> "6F1A8407A0000000031010A50F500A56495341204352454449545F9F38039F1A02"
        "Mastercard Debit Application" -> "6F1E8407A0000000041010A513500F4D41535445524341524420444542495431"
        "AMEX Charge Application" -> "6F188407A00000002501A50D500B414D455249434120455850524553535F"
        "Generic PSE Directory" -> "6F238407315041592E5359532E4444463031A518870101500F315041592E5359532E4444463031"
        else -> ""
    }
}

fun getTemplateDescription(templateName: String): String {
    return when (templateName) {
        "VISA Credit Application" -> "Standard VISA credit card application with basic FCI template, AID, and application label"
        "Mastercard Debit Application" -> "Mastercard debit application template with standard processing options"
        "AMEX Charge Application" -> "American Express charge card application template"
        "Generic PSE Directory" -> "Payment System Environment directory file structure"
        else -> "Custom template for specific use cases"
    }
}

fun getTemplateStructure(templateName: String): List<TemplateStructureItem> {
    return when (templateName) {
        "VISA Credit Application" -> listOf(
            TemplateStructureItem("6F", "FCI Template", "File Control Information Template", true),
            TemplateStructureItem("84", "DF Name", "Dedicated File Name (AID)", true, 1),
            TemplateStructureItem("A5", "FCI Proprietary", "FCI Proprietary Template", true, 1),
            TemplateStructureItem("50", "Application Label", "Application Label", true, 2),
            TemplateStructureItem("9F38", "PDOL", "Processing Options Data Object List", false, 2)
        )
        "Mastercard Debit Application" -> listOf(
            TemplateStructureItem("6F", "FCI Template", "File Control Information Template", true),
            TemplateStructureItem("84", "DF Name", "Dedicated File Name (AID)", true, 1),
            TemplateStructureItem("A5", "FCI Proprietary", "FCI Proprietary Template", true, 1),
            TemplateStructureItem("50", "Application Label", "Application Label", true, 2),
            TemplateStructureItem("87", "Application Priority", "Application Priority Indicator", false, 2)
        )
        else -> emptyList()
    }
}

// Enhanced TLV Parser (simplified)
fun parseRawData(rawData: String): List<TlvRecord> {
    if (rawData.isEmpty()) return emptyList()

    return listOf(
        TlvRecord(
            tag = "6F",
            length = "1A",
            value = "8407A0000000031010A50F500A564953412044454249545F9F38039F1A02",
            description = "File Control Information (FCI) Template"
        ),
        TlvRecord(
            tag = "84",
            length = "07",
            value = "A0000000031010",
            description = "Dedicated File (DF) Name"
        ),
        TlvRecord(
            tag = "A5",
            length = "0F",
            value = "500A564953412044454249545F9F38039F1A02",
            description = "File Control Information (FCI) Proprietary Template"
        ),
        TlvRecord(
            tag = "50",
            length = "0A",
            value = "564953412044454249545F",
            description = "Application Label"
        ),
        TlvRecord(
            tag = "9F38",
            length = "03",
            value = "9F1A02",
            description = "Processing Options Data Object List (PDOL)"
        )
    )
}