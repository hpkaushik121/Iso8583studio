package `in`.aicortex.iso8583studio.ui.screens.apduSimulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.CardServiceImpl
import `in`.aicortex.iso8583studio.ui.screens.apduSimulator.TlvTreeUtils.getAllTags
import kotlinx.coroutines.launch

/**
 * TLV Parser Tab - Professional EMV TLV data analysis interface
 * Features two-panel layout with hierarchical TLV display and tag dictionary
 */
@Composable
fun TlvParserTab(
    cardService: CardServiceImpl,
    selectedTlvTag: MutableState<String?>,
    selectedTlvIndex: MutableState<Int?>,
    showTlvAnalysis: MutableState<Boolean>,
    showApduParser: MutableState<Boolean>,
    animationTrigger: MutableState<Int>,
    rawMessage: MutableState<String>,
    parseError: MutableState<String?>,
    currentTlvData: MutableState<Map<String, String>?>,
    currentApduFields: MutableState<Map<String, String>?>,
    searchQuery: MutableState<String>,
    modifier: Modifier = Modifier
) {
    var inputData by remember { mutableStateOf("") }
    var displayMode by remember { mutableStateOf(TlvDisplayMode.HEX) }
    var parsedTlvTree by remember { mutableStateOf<List<TlvNode>>(emptyList()) }
    var validationResults by remember { mutableStateOf<List<TlvValidationResult>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<TlvNode?>(null) }
    var filterCritical by remember { mutableStateOf(false) }
    var autoValidate by remember { mutableStateOf(true) }
    var expandedNodes by remember { mutableStateOf(setOf<String>()) }

    val coroutineScope = rememberCoroutineScope()

    // Parse TLV data when input changes
    LaunchedEffect(inputData) {
        if (inputData.isNotEmpty()) {
            try {
                val tlvData = TlvParser.parse(inputData)
                parsedTlvTree = TlvTreeBuilder.buildTree(tlvData)

                if (autoValidate) {
                    validationResults = TlvValidator.validate(parsedTlvTree)
                }

                parseError.value = null
                currentTlvData.value = tlvData.associate { it.tag to it.value }
            } catch (e: Exception) {
                parseError.value = e.message
                parsedTlvTree = emptyList()
                validationResults = emptyList()
                currentTlvData.value = null
            }
        } else {
            parsedTlvTree = emptyList()
            validationResults = emptyList()
            parseError.value = null
            currentTlvData.value = null
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Control Panel
        TlvControlPanel(
            inputData = inputData,
            onInputDataChange = { inputData = it },
            displayMode = displayMode,
            onDisplayModeChange = { displayMode = it },
            filterCritical = filterCritical,
            onFilterCriticalChange = { filterCritical = it },
            autoValidate = autoValidate,
            onAutoValidateChange = { autoValidate = it },
            onValidateClick = {
                if (parsedTlvTree.isNotEmpty()) {
                    validationResults = TlvValidator.validate(parsedTlvTree)
                }
            },
            onClearClick = {
                inputData = ""
                selectedTlvTag.value = null
                parseError.value = null
                expandedNodes = emptySet()
            },
            parseError = parseError.value,
            validationResults = validationResults,
            nodeCount = parsedTlvTree.size
        )

        // Main Two-Panel Layout
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT PANEL - TLV Tree (60%)
            Card(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Tree Header
                    TlvTreeHeader(
                        nodeCount = parsedTlvTree.size,
                        validationResults = validationResults,
                        displayMode = displayMode,
                        onExpandAll = {
                            expandedNodes = getAllTags(parsedTlvTree)
                        },
                        onCollapseAll = {
                            expandedNodes = emptySet()
                        }
                    )

                    Divider()

                    // Tree Content
                    if (parsedTlvTree.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(parsedTlvTree) { node ->
                                TlvTreeNodeItem(
                                    node = node,
                                    level = 0,
                                    selectedTag = selectedTlvTag.value,
                                    expandedNodes = expandedNodes,
                                    onNodeClick = { selectedTlvTag.value = it.tag },
                                    onNodeExpand = { tag ->
                                        expandedNodes = if (expandedNodes.contains(tag)) {
                                            expandedNodes - tag
                                        } else {
                                            expandedNodes + tag
                                        }
                                    },
                                    onEditClick = {
                                        editingNode = it
                                        showEditDialog = true
                                    },
                                    displayMode = displayMode,
                                    validationResults = validationResults,
                                    filterCritical = filterCritical
                                )
                            }
                        }
                    } else {
                        TlvEmptyState(
                            parseError = parseError.value,
                            onLoadSampleClick = {
                                inputData = TlvSampleData.getEmvSelectResponse()
                            }
                        )
                    }
                }
            }

            // RIGHT PANEL - Tag Dictionary & Details (40%)
            Card(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Dictionary Header with Search
                    TagDictionaryHeader(
                        searchQuery = searchQuery.value,
                        onSearchQueryChange = { searchQuery.value = it },
                        selectedTag = selectedTlvTag.value,
                        onClearSelection = { selectedTlvTag.value = null }
                    )

                    Divider()

                    // Dictionary Content
                    if (selectedTlvTag.value != null) {
                        TagDetailsPanel(
                            tag = selectedTlvTag.value!!,
                            tlvTree = parsedTlvTree,
                            displayMode = displayMode,
                            validationResults = validationResults
                        )
                    } else {
                        TagDictionaryBrowser(
                            searchQuery = searchQuery.value,
                            onTagSelect = { selectedTlvTag.value = it },
                            filterCritical = filterCritical
                        )
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog && editingNode != null) {
        TlvEditDialog(
            node = editingNode!!,
            onDismiss = {
                showEditDialog = false
                editingNode = null
            },
            onSave = { newValue ->
                // Update the TLV tree with new value
                val updatedTree = TlvTreeUpdater.updateNodeValue(parsedTlvTree, editingNode!!.tag, newValue)
                parsedTlvTree = updatedTree

                // Rebuild input data
                inputData = TlvEncoder.encodeTree(updatedTree)

                showEditDialog = false
                editingNode = null
            }
        )
    }
}

/**
 * TLV Control Panel - Input and configuration controls
 */
@Composable
private fun TlvControlPanel(
    inputData: String,
    onInputDataChange: (String) -> Unit,
    displayMode: TlvDisplayMode,
    onDisplayModeChange: (TlvDisplayMode) -> Unit,
    filterCritical: Boolean,
    onFilterCriticalChange: (Boolean) -> Unit,
    autoValidate: Boolean,
    onAutoValidateChange: (Boolean) -> Unit,
    onValidateClick: () -> Unit,
    onClearClick: () -> Unit,
    parseError: String?,
    validationResults: List<TlvValidationResult>,
    nodeCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with statistics
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "TLV Parser & Analyzer",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                // Statistics
                if (nodeCount > 0) {
                    StatusChip(
                        text = "$nodeCount tags",
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White
                    )
                }

                val errorCount = validationResults.count { !it.isValid }
                if (errorCount > 0) {
                    StatusChip(
                        text = "$errorCount errors",
                        backgroundColor = Color(0xFFF44336),
                        contentColor = Color.White
                    )
                }

                val warningCount = validationResults.count { it.isWarning }
                if (warningCount > 0) {
                    StatusChip(
                        text = "$warningCount warnings",
                        backgroundColor = Color(0xFFFF9800),
                        contentColor = Color.White
                    )
                }

                // Control buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onValidateClick,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Validate")
                    }

                    OutlinedButton(
                        onClick = onClearClick,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }

            // Input field
            OutlinedTextField(
                value = inputData,
                onValueChange = onInputDataChange,
                label = { Text("TLV Data (Hex)") },
                placeholder = { Text("Enter TLV data in hexadecimal format (e.g., 6F1A840E325041592E5359532E4444463031...)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                isError = parseError != null,
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
            )

            // Error display
            if (parseError != null) {
                Card(
                    backgroundColor = Color(0xFFFFEBEE),
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Parse Error",
                                style = MaterialTheme.typography.subtitle2,
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = parseError,
                                style = MaterialTheme.typography.body2,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }

            // Options row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Display mode selection
                Column {
                    Text(
                        text = "Display Mode",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TlvDisplayMode.values().forEach { mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onDisplayModeChange(mode) }
                            ) {
                                RadioButton(
                                    selected = displayMode == mode,
                                    onClick = { onDisplayModeChange(mode) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colors.primary
                                    )
                                )
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Filters and options
                Column {
                    Text(
                        text = "Options",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Critical filter
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = filterCritical,
                                onCheckedChange = onFilterCriticalChange,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colors.primary
                                )
                            )
                            Text(
                                text = "Critical Only",
                                style = MaterialTheme.typography.body2,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        // Auto-validate
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = autoValidate,
                                onCheckedChange = onAutoValidateChange,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colors.primary
                                )
                            )
                            Text(
                                text = "Auto-validate",
                                style = MaterialTheme.typography.body2,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * TLV Tree Header with statistics and controls
 */
@Composable
private fun TlvTreeHeader(
    nodeCount: Int,
    validationResults: List<TlvValidationResult>,
    displayMode: TlvDisplayMode,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = "TLV Structure",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Expand/Collapse controls
            if (nodeCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onExpandAll,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Expand All", style = MaterialTheme.typography.caption)
                    }

                    TextButton(
                        onClick = onCollapseAll,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldLess,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Collapse All", style = MaterialTheme.typography.caption)
                    }
                }
            }

            // Display mode indicator
            StatusChip(
                text = displayMode.displayName,
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = Color.White
            )
        }
    }
}

/**
 * Individual TLV Tree Node with expandable children
 */
@Composable
private fun TlvTreeNodeItem(
    node: TlvNode,
    level: Int,
    selectedTag: String?,
    expandedNodes: Set<String>,
    onNodeClick: (TlvNode) -> Unit,
    onNodeExpand: (String) -> Unit,
    onEditClick: (TlvNode) -> Unit,
    displayMode: TlvDisplayMode,
    validationResults: List<TlvValidationResult>,
    filterCritical: Boolean
) {
    val isSelected = selectedTag == node.tag
    val hasChildren = node.children.isNotEmpty()
    val isExpanded = expandedNodes.contains(node.tag)
    val validation = validationResults.find { it.tag == node.tag }
    val tagInfo = EmvTagDictionary.getTagInfo(node.tag)

    // Filter critical tags if enabled
    val isCritical = tagInfo?.isCritical == true
    if (filterCritical && !isCritical) return

    Column {
        // Node row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 20).dp, top = 1.dp, bottom = 1.dp)
                .clickable { onNodeClick(node) },
            backgroundColor = when {
                isSelected -> MaterialTheme.colors.primary.copy(alpha = 0.15f)
                validation?.isValid == false -> Color(0xFFFFEBEE)
                validation?.isWarning == true -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colors.surface
            },
            elevation = if (isSelected) 3.dp else 1.dp,
            border = if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colors.primary)
            } else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expand/collapse button
                if (hasChildren) {
                    IconButton(
                        onClick = { onNodeExpand(node.tag) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }

                // Validation indicator
                validation?.let { result ->
                    Icon(
                        imageVector = when {
                            !result.isValid -> Icons.Default.Error
                            result.isWarning -> Icons.Default.Warning
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = when {
                            !result.isValid -> Color(0xFFF44336)
                            result.isWarning -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                } ?: Spacer(modifier = Modifier.width(16.dp))

                // Tag information
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tag
                        Text(
                            text = node.tag,
                            style = MaterialTheme.typography.body2,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )

                        // Length
                        StatusChip(
                            text = "${node.length}B",
                            backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )

                        // Tag name
                        tagInfo?.let { info ->
                            Text(
                                text = info.name,
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (info.isCritical) {
                                Icon(
                                    imageVector = Icons.Default.PriorityHigh,
                                    contentDescription = "Critical",
                                    tint = Color(0xFFFF5722),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Value display
                    if (!hasChildren && node.value.isNotEmpty()) {
                        Text(
                            text = TlvValueFormatter.format(node.value, displayMode, tagInfo),
                            style = MaterialTheme.typography.caption,
                            fontFamily = if (displayMode == TlvDisplayMode.HEX) FontFamily.Monospace else FontFamily.Default,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Edit button
                if (!hasChildren) {
                    IconButton(
                        onClick = { onEditClick(node) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Children
        AnimatedVisibility(
            visible = isExpanded && hasChildren,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                node.children.forEach { childNode ->
                    TlvTreeNodeItem(
                        node = childNode,
                        level = level + 1,
                        selectedTag = selectedTag,
                        expandedNodes = expandedNodes,
                        onNodeClick = onNodeClick,
                        onNodeExpand = onNodeExpand,
                        onEditClick = onEditClick,
                        displayMode = displayMode,
                        validationResults = validationResults,
                        filterCritical = filterCritical
                    )
                }
            }
        }
    }
}

/**
 * Empty state for TLV tree
 */
@Composable
private fun TlvEmptyState(
    parseError: String?,
    onLoadSampleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (parseError != null) Icons.Default.ErrorOutline else Icons.Default.AccountTree,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (parseError != null) "Parse Error" else "No TLV Data",
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (parseError != null) {
                "Please check your input format and try again"
            } else {
                "Enter TLV data above to analyze the structure"
            },
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onLoadSampleClick
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load Sample")
            }

            OutlinedButton(
                onClick = { /* Open help dialog */ }
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Help")
            }
        }
    }
}

/**
 * Tag Dictionary Header with search
 */
@Composable
private fun TagDictionaryHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTag: String?,
    onClearSelection: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface
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
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = if (selectedTag != null) "Tag Details" else "EMV Tag Dictionary",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )

                if (selectedTag != null) {
                    StatusChip(
                        text = selectedTag,
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (selectedTag != null) {
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear selection",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Search field (only when browsing dictionary)
            if (selectedTag == null) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search tags") },
                    placeholder = { Text("Tag name, number, or description...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

/**
 * Tag Details Panel - Show detailed information about selected tag
 */
@Composable
private fun TagDetailsPanel(
    tag: String,
    tlvTree: List<TlvNode>,
    displayMode: TlvDisplayMode,
    validationResults: List<TlvValidationResult>
) {
    val tagInfo = EmvTagDictionary.getTagInfo(tag)
    val node = TlvTreeUtils.findNodeByTag(tlvTree, tag)
    val validation = validationResults.find { it.tag == tag }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tag information card
        Card(
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tag Information",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                TagDetailRow("Tag", tag)
                tagInfo?.let { info ->
                    TagDetailRow("Name", info.name)
                    TagDetailRow("Class", info.tagClass)
                    TagDetailRow("Type", if (info.isConstructed) "Constructed" else "Primitive")
                    TagDetailRow("Format", info.format)
                    TagDetailRow("Source", info.source)
                    TagDetailRow("Category", info.category)

                    if (info.isCritical) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Critical EMV Tag",
                                style = MaterialTheme.typography.body2,
                                color = Color(0xFFFF5722),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } ?: run {
                    Card(
                        backgroundColor = Color(0xFFFFF3E0),
                        elevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Unknown tag - not in EMV dictionary",
                                style = MaterialTheme.typography.body2,
                                color = Color(0xFFE65100),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        // Current value card (if node exists)
        node?.let { tlvNode ->
            Card(
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Current Value",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )

                    TagDetailRow("Length", "${tlvNode.length} bytes")

                    // Value in different formats
                    if (tlvNode.value.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))

                        ValueFormatCard(
                            label = "Hexadecimal",
                            value = TlvValueFormatter.formatAsHex(tlvNode.value),
                            isCopyable = true
                        )

                        // ASCII interpretation
                        val asciiValue = TlvValueFormatter.formatAsAscii(tlvNode.value)
                        if (asciiValue.isNotEmpty() && asciiValue != tlvNode.value) {
                            ValueFormatCard(
                                label = "ASCII",
                                value = asciiValue,
                                isCopyable = true
                            )
                        }

                        // Decimal interpretation (for numeric tags)
                        tagInfo?.let { info ->
                            if (info.format.contains("numeric", true) || info.format.contains("amount", true)) {
                                val decimalValue = TlvValueFormatter.formatAsDecimal(tlvNode.value)
                                if (decimalValue.isNotEmpty()) {
                                    ValueFormatCard(
                                        label = "Decimal",
                                        value = decimalValue,
                                        isCopyable = true
                                    )
                                }
                            }
                        }

                        // BCD interpretation (for BCD tags)
                        tagInfo?.let { info ->
                            if (info.format.contains("BCD", true)) {
                                val bcdValue = TlvValueFormatter.formatAsBcd(tlvNode.value)
                                if (bcdValue.isNotEmpty()) {
                                    ValueFormatCard(
                                        label = "BCD",
                                        value = bcdValue,
                                        isCopyable = true
                                    )
                                }
                            }
                        }

                        // Date interpretation (for date tags)
                        if (tag in listOf("9A", "9F21") || tagInfo?.format?.contains("date", true) == true) {
                            val dateValue = TlvValueFormatter.formatAsDate(tlvNode.value)
                            if (dateValue.isNotEmpty()) {
                                ValueFormatCard(
                                    label = "Date",
                                    value = dateValue,
                                    isCopyable = true
                                )
                            }
                        }
                    }
                }
            }
        }

        // Validation results card
        validation?.let { result ->
            Card(
                backgroundColor = when {
                    !result.isValid -> Color(0xFFFFEBEE)
                    result.isWarning -> Color(0xFFFFF3E0)
                    else -> Color(0xFFE8F5E8)
                },
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                !result.isValid -> Icons.Default.Error
                                result.isWarning -> Icons.Default.Warning
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = when {
                                !result.isValid -> Color(0xFFC62828)
                                result.isWarning -> Color(0xFFE65100)
                                else -> Color(0xFF2E7D32)
                            },
                            modifier = Modifier.size(20.dp)
                        )

                        Text(
                            text = "Validation Result",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.body2,
                        color = when {
                            !result.isValid -> Color(0xFFC62828)
                            result.isWarning -> Color(0xFFE65100)
                            else -> Color(0xFF2E7D32)
                        },
                        lineHeight = 18.sp
                    )

                    if (result.suggestions.isNotEmpty()) {
                        Divider()
                        Text(
                            text = "Suggestions:",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold
                        )
                        result.suggestions.forEach { suggestion ->
                            Text(
                                text = "• $suggestion",
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(start = 8.dp),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // EMV specification card
        tagInfo?.let { info ->
            if (info.description.isNotEmpty()) {
                Card(
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "EMV Specification",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )

                        Text(
                            text = info.description,
                            style = MaterialTheme.typography.body2,
                            lineHeight = 20.sp
                        )

                        if (info.constraints.isNotEmpty()) {
                            Divider()
                            Text(
                                text = "Constraints:",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold
                            )
                            info.constraints.forEach { constraint ->
                                Text(
                                    text = "• $constraint",
                                    style = MaterialTheme.typography.caption,
                                    modifier = Modifier.padding(start = 8.dp),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        if (info.examples.isNotEmpty()) {
                            Divider()
                            Text(
                                text = "Examples:",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold
                            )
                            info.examples.forEach { example ->
                                ValueFormatCard(
                                    label = "Example",
                                    value = example,
                                    isCopyable = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tag Dictionary Browser - Browse available tags
 */
@Composable
private fun TagDictionaryBrowser(
    searchQuery: String,
    onTagSelect: (String) -> Unit,
    filterCritical: Boolean
) {
    val filteredTags = remember(searchQuery, filterCritical) {
        val allTags = EmvTagDictionary.getAllTags()
        val filtered = if (searchQuery.isEmpty()) {
            allTags
        } else {
            EmvTagDictionary.searchTags(searchQuery)
        }

        if (filterCritical) {
            filtered.filter { it.isCritical }
        } else {
            filtered
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Category headers
        val groupedTags = filteredTags.groupBy { it.category }

        groupedTags.forEach { (category, tags) ->
            item {
                Card(
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    elevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        StatusChip(
                            text = "${tags.size}",
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = Color.White
                        )
                    }
                }
            }

            items(tags) { tagInfo ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagSelect(tagInfo.tag) },
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = tagInfo.tag,
                                style = MaterialTheme.typography.body2,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )

                            StatusChip(
                                text = tagInfo.tagClass,
                                backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colors.secondary
                            )

                            if (tagInfo.isCritical) {
                                StatusChip(
                                    text = "Critical",
                                    backgroundColor = Color(0xFFFF5722),
                                    contentColor = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Icon(
                                imageVector = if (tagInfo.isConstructed) Icons.Default.AccountTree else Icons.Default.DataObject,
                                contentDescription = if (tagInfo.isConstructed) "Constructed" else "Primitive",
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            text = tagInfo.name,
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (tagInfo.description.isNotEmpty()) {
                            Text(
                                text = tagInfo.description,
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 14.sp
                            )
                        }

                        if (tagInfo.format.isNotEmpty()) {
                            StatusChip(
                                text = tagInfo.format,
                                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                                contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        if (filteredTags.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tags found",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "Try a different search term",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * TLV Edit Dialog for modifying values
 */
@Composable
private fun TlvEditDialog(
    node: TlvNode,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var editValue by remember { mutableStateOf(node.value) }
    var editMode by remember { mutableStateOf(TlvEditMode.HEX) }
    var isValid by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var previewValue by remember { mutableStateOf("") }

    val tagInfo = EmvTagDictionary.getTagInfo(node.tag)

    // Validate input and generate preview
    LaunchedEffect(editValue, editMode) {
        try {
            when (editMode) {
                TlvEditMode.HEX -> {
                    if (editValue.matches(Regex("[0-9A-Fa-f]*")) && editValue.length % 2 == 0) {
                        isValid = true
                        errorMessage = ""
                        previewValue = editValue.uppercase()
                    } else {
                        isValid = false
                        errorMessage = "Invalid hex format - use only 0-9, A-F characters with even length"
                        previewValue = ""
                    }
                }
                TlvEditMode.ASCII -> {
                    isValid = true
                    errorMessage = ""
                    previewValue = TlvValueFormatter.asciiToHex(editValue)
                }
                TlvEditMode.DECIMAL -> {
                    editValue.toLongOrNull()?.let { number ->
                        isValid = true
                        errorMessage = ""
                        previewValue = TlvValueFormatter.decimalToHex(number)
                    } ?: run {
                        isValid = false
                        errorMessage = "Invalid decimal number"
                        previewValue = ""
                    }
                }
            }
        } catch (e: Exception) {
            isValid = false
            errorMessage = e.message ?: "Invalid input"
            previewValue = ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Edit TLV Value",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tag information
                Card(
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Tag:",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = node.tag,
                                style = MaterialTheme.typography.body2,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )
                            if (tagInfo?.isCritical == true) {
                                StatusChip(
                                    text = "Critical",
                                    backgroundColor = Color(0xFFFF5722),
                                    contentColor = Color.White
                                )
                            }
                        }
                        tagInfo?.let {
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Format: ${it.format}",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = "Current length: ${node.length} bytes",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Edit mode selection
                Column {
                    Text(
                        text = "Input Format",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TlvEditMode.values().forEach { mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { editMode = mode }
                            ) {
                                RadioButton(
                                    selected = editMode == mode,
                                    onClick = { editMode = mode },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colors.primary
                                    )
                                )
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Value input
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text("Value (${editMode.displayName})") },
                    placeholder = { Text(getPlaceholderText(editMode)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    isError = !isValid,
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = if (editMode == TlvEditMode.HEX) FontFamily.Monospace else FontFamily.Default
                    )
                )

                // Error message
                if (!isValid) {
                    Card(
                        backgroundColor = Color(0xFFFFEBEE),
                        elevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.caption,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }

                // Preview
                if (isValid && previewValue.isNotEmpty()) {
                    Card(
                        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                        elevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Preview (Hex):",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )
                            SelectionContainer {
                                Text(
                                    text = TlvValueFormatter.formatAsHex(previewValue),
                                    style = MaterialTheme.typography.body2,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "New length: ${previewValue.length / 2} bytes",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onSave(previewValue)
                        },
                        enabled = isValid && previewValue.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

// Helper Composables
@Composable
private fun TagDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.65f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ValueFormatCard(
    label: String,
    value: String,
    isCopyable: Boolean = false
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        elevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isCopyable) {
                SelectionContainer {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.body2,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.body2,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Card(
        backgroundColor = backgroundColor,
        elevation = 0.dp,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// Data Classes and Enums
data class TlvNode(
    val tag: String,
    val length: Int,
    val value: String,
    val children: List<TlvNode> = emptyList(),
    val isConstructed: Boolean = false,
    val rawData: String = ""
)

data class TlvValidationResult(
    val tag: String,
    val isValid: Boolean,
    val isWarning: Boolean = false,
    val message: String,
    val suggestions: List<String> = emptyList()
)

data class TlvTagInfo(
    val tag: String,
    val name: String,
    val description: String,
    val format: String,
    val source: String,
    val tagClass: String,
    val isConstructed: Boolean,
    val isCritical: Boolean,
    val constraints: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
    val category: String = "General"
)

data class TlvParseResult(
    val tag: String,
    val length: Int,
    val value: String,
    val offset: Int,
    val nextOffset: Int
)

enum class TlvDisplayMode(val displayName: String) {
    HEX("Hex"),
    ASCII("ASCII"),
    DECIMAL("Decimal"),
    SMART("Smart")
}

enum class TlvEditMode(val displayName: String) {
    HEX("Hex"),
    ASCII("ASCII"),
    DECIMAL("Decimal")
}

// Utility Objects
object TlvParser {
    fun parse(hexData: String): List<TlvParseResult> {
        val cleanHex = hexData.replace("\\s".toRegex(), "").uppercase()
        val results = mutableListOf<TlvParseResult>()

        var offset = 0
        while (offset < cleanHex.length) {
            try {
                val parseResult = parseNextTlv(cleanHex, offset)
                results.add(parseResult)
                offset = parseResult.nextOffset
            } catch (e: Exception) {
                throw Exception("Parse error at position $offset: ${e.message}")
            }
        }

        return results
    }

    private fun parseNextTlv(data: String, startOffset: Int): TlvParseResult {
        var offset = startOffset

        // Parse tag
        if (offset + 2 > data.length) throw Exception("Incomplete tag")

        val firstTagByte = data.substring(offset, offset + 2).toInt(16)
        var tag = data.substring(offset, offset + 2)
        offset += 2

        // Handle multi-byte tags
        if ((firstTagByte and 0x1F) == 0x1F) {
            while (offset + 2 <= data.length) {
                val nextByte = data.substring(offset, offset + 2).toInt(16)
                tag += data.substring(offset, offset + 2)
                offset += 2
                if ((nextByte and 0x80) == 0) break
            }
        }

        // Parse length
        if (offset + 2 > data.length) throw Exception("Incomplete length")

        val firstLengthByte = data.substring(offset, offset + 2).toInt(16)
        offset += 2

        val length = if ((firstLengthByte and 0x80) == 0) {
            // Short form
            firstLengthByte
        } else {
            // Long form
            val lengthOfLength = firstLengthByte and 0x7F
            if (lengthOfLength == 0) throw Exception("Indefinite length not supported")
            if (offset + (lengthOfLength * 2) > data.length) throw Exception("Incomplete length")

            var calculatedLength = 0
            for (i in 0 until lengthOfLength) {
                calculatedLength = (calculatedLength shl 8) or data.substring(offset, offset + 2).toInt(16)
                offset += 2
            }
            calculatedLength
        }

        // Parse value
        val valueLength = length * 2
        if (offset + valueLength > data.length) throw Exception("Incomplete value")

        val value = data.substring(offset, offset + valueLength)
        offset += valueLength

        return TlvParseResult(
            tag = tag,
            length = length,
            value = value,
            offset = startOffset,
            nextOffset = offset
        )
    }
}

object TlvTreeBuilder {
    fun buildTree(parseResults: List<TlvParseResult>): List<TlvNode> {
        return parseResults.map { result ->
            val tagInfo = EmvTagDictionary.getTagInfo(result.tag)
            val isConstructed = tagInfo?.isConstructed ?: isTagConstructed(result.tag)

            if (isConstructed && result.value.isNotEmpty()) {
                // Parse children
                try {
                    val childResults = TlvParser.parse(result.value)
                    val children = buildTree(childResults)
                    TlvNode(
                        tag = result.tag,
                        length = result.length,
                        value = result.value,
                        children = children,
                        isConstructed = true,
                        rawData = result.value
                    )
                } catch (e: Exception) {
                    // If parsing children fails, treat as primitive
                    TlvNode(
                        tag = result.tag,
                        length = result.length,
                        value = result.value,
                        isConstructed = false,
                        rawData = result.value
                    )
                }
            } else {
                TlvNode(
                    tag = result.tag,
                    length = result.length,
                    value = result.value,
                    isConstructed = false,
                    rawData = result.value
                )
            }
        }
    }

    private fun isTagConstructed(tag: String): Boolean {
        if (tag.length >= 2) {
            val firstByte = tag.substring(0, 2).toInt(16)
            return (firstByte and 0x20) != 0
        }
        return false
    }
}

object TlvValidator {
    fun validate(nodes: List<TlvNode>): List<TlvValidationResult> {
        val results = mutableListOf<TlvValidationResult>()

        nodes.forEach { node ->
            results.addAll(validateNode(node))
        }

        return results
    }

    private fun validateNode(node: TlvNode): List<TlvValidationResult> {
        val results = mutableListOf<TlvValidationResult>()
        val tagInfo = EmvTagDictionary.getTagInfo(node.tag)

        // Validate current node
        when {
            tagInfo == null -> results.add(
                TlvValidationResult(
                    tag = node.tag,
                    isValid = true,
                    isWarning = true,
                    message = "Unknown tag - not in EMV specification",
                    suggestions = listOf(
                        "Verify tag number is correct",
                        "Check for proprietary or issuer-specific tags",
                        "Consult latest EMV specifications"
                    )
                )
            )

            tagInfo.isCritical && node.value.isEmpty() && node.children.isEmpty() -> results.add(
                TlvValidationResult(
                    tag = node.tag,
                    isValid = false,
                    message = "Critical EMV tag cannot be empty",
                    suggestions = listOf("Provide required value for this critical tag")
                )
            )

            tagInfo.isConstructed != node.isConstructed -> results.add(
                TlvValidationResult(
                    tag = node.tag,
                    isValid = false,
                    message = "Tag construction mismatch - expected ${if (tagInfo.isConstructed) "constructed" else "primitive"}",
                    suggestions = listOf("Check tag definition and data structure")
                )
            )

            !validateTagFormat(node, tagInfo) -> results.add(
                TlvValidationResult(
                    tag = node.tag,
                    isValid = false,
                    message = "Invalid format for ${tagInfo.format} data",
                    suggestions = listOf("Check data format according to EMV specification")
                )
            )

            else -> results.add(
                TlvValidationResult(
                    tag = node.tag,
                    isValid = true,
                    message = "Valid EMV tag structure and format"
                )
            )
        }

        // Validate children
        node.children.forEach { child ->
            results.addAll(validateNode(child))
        }

        return results
    }

    private fun validateTagFormat(node: TlvNode, tagInfo: TlvTagInfo): Boolean {
        if (node.value.isEmpty()) return true

        return when {
            tagInfo.format.contains("BCD", true) -> isValidBcd(node.value)
            tagInfo.format.contains("numeric", true) -> isValidNumeric(node.value)
            tagInfo.format.contains("ASCII", true) -> isValidAscii(node.value)
            else -> true // Default to valid for unknown formats
        }
    }

    private fun isValidBcd(hex: String): Boolean {
        return hex.all { it in '0'..'9' || it.uppercaseChar() in 'A'..'F' } &&
                hex.chunked(2).all { byte ->
                    val high = byte[0].digitToIntOrNull(16) ?: return false
                    val low = byte[1].digitToIntOrNull(16) ?: return false
                    high <= 9 && low <= 9
                }
    }

    private fun isValidNumeric(hex: String): Boolean {
        return try {
            hex.toLong(16)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidAscii(hex: String): Boolean {
        return try {
            hex.chunked(2).all { byte ->
                val value = byte.toInt(16)
                value in 0x20..0x7E // Printable ASCII range
            }
        } catch (e: Exception) {
            false
        }
    }
}

object TlvValueFormatter {
    fun format(value: String, mode: TlvDisplayMode, tagInfo: TlvTagInfo?): String {
        return when (mode) {
            TlvDisplayMode.HEX -> formatAsHex(value)
            TlvDisplayMode.ASCII -> formatAsAscii(value)
            TlvDisplayMode.DECIMAL -> formatAsDecimal(value)
            TlvDisplayMode.SMART -> formatSmart(value, tagInfo)
        }
    }

    fun formatAsHex(value: String): String {
        return value.chunked(2).joinToString(" ").uppercase()
    }

    fun formatAsAscii(value: String): String {
        return try {
            value.chunked(2)
                .map { it.toInt(16).toChar() }
                .joinToString("")
                .filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,!?@#$%^&*()-_+=[]{}|;:'\",.<>/" }
        } catch (e: Exception) {
            ""
        }
    }

    fun formatAsDecimal(value: String): String {
        return try {
            value.toLong(16).toString()
        } catch (e: Exception) {
            ""
        }
    }

    fun formatAsBcd(value: String): String {
        return try {
            value.chunked(2)
                .map { byte ->
                    val high = (byte.toInt(16) shr 4) and 0x0F
                    val low = byte.toInt(16) and 0x0F
                    "$high$low"
                }
                .joinToString("")
                .trimEnd('F', 'f')
        } catch (e: Exception) {
            ""
        }
    }

    fun formatAsDate(value: String): String {
        return try {
            when (value.length) {
                6 -> { // YYMMDD
                    val year = "20${value.substring(0, 2)}"
                    val month = value.substring(2, 4)
                    val day = value.substring(4, 6)
                    "$year-$month-$day"
                }
                4 -> { // YYMM
                    val year = "20${value.substring(0, 2)}"
                    val month = value.substring(2, 4)
                    "$year-$month"
                }
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatSmart(value: String, tagInfo: TlvTagInfo?): String {
        return tagInfo?.let { info ->
            when {
                info.format.contains("BCD", true) -> formatAsBcd(value)
                info.format.contains("ASCII", true) -> formatAsAscii(value)
                info.format.contains("numeric", true) -> formatAsDecimal(value)
                info.tag in listOf("9A", "9F21") -> formatAsDate(value) // Date fields
                else -> formatAsHex(value)
            }
        } ?: formatAsHex(value)
    }

    fun asciiToHex(ascii: String): String {
        return ascii.toByteArray().joinToString("") { "%02X".format(it) }
    }

    fun decimalToHex(decimal: Long): String {
        return decimal.toString(16).uppercase().let { hex ->
            if (hex.length % 2 != 0) "0$hex" else hex
        }
    }
}

object TlvTreeUtils {
    fun findNodeByTag(nodes: List<TlvNode>, tag: String): TlvNode? {
        for (node in nodes) {
            if (node.tag == tag) return node
            val found = findNodeByTag(node.children, tag)
            if (found != null) return found
        }
        return null
    }

    fun getAllTags(nodes: List<TlvNode>): Set<String> {
        val tags = mutableSetOf<String>()
        nodes.forEach { node ->
            tags.add(node.tag)
            tags.addAll(getAllTags(node.children))
        }
        return tags
    }
}

object TlvTreeUpdater {
    fun updateNodeValue(nodes: List<TlvNode>, tag: String, newValue: String): List<TlvNode> {
        return nodes.map { node ->
            if (node.tag == tag) {
                node.copy(
                    value = newValue,
                    length = newValue.length / 2,
                    rawData = newValue
                )
            } else {
                node.copy(children = updateNodeValue(node.children, tag, newValue))
            }
        }
    }
}

object TlvEncoder {
    fun encodeTree(nodes: List<TlvNode>): String {
        return nodes.joinToString("") { node ->
            encodeNode(node)
        }
    }

    private fun encodeNode(node: TlvNode): String {
        val value = if (node.isConstructed && node.children.isNotEmpty()) {
            encodeTree(node.children)
        } else {
            node.value
        }

        val length = value.length / 2
        val lengthHex = when {
            length < 0x80 -> String.format("%02X", length)
            length < 0x100 -> "81" + String.format("%02X", length)
            length < 0x10000 -> "82" + String.format("%04X", length)
            else -> throw Exception("Length too large")
        }

        return "${node.tag}${lengthHex}${value}"
    }
}

object TlvSampleData {
    fun getEmvSelectResponse(): String {
        return "6F1A840E325041592E5359532E4444463031A5088801025F2D02656E"
    }

    fun getVisaSelectResponse(): String {
        return "6F2B840E325041592E5359532E4444463031A5198801025F2D02656E9F11010157104111111111111111D25122010000000000F"
    }

    fun getMastercardSelectResponse(): String {
        return "6F2A840E325041592E5359532E4444463031A5188801025F2D046672656E9F11010157104111111111111111D2512201000000000F"
    }
}

// EMV Tag Dictionary
object EmvTagDictionary {
    private val tags = mapOf(
        "6F" to TlvTagInfo(
            tag = "6F",
            name = "File Control Information Template",
            description = "Template containing file control information for a DF or EF",
            format = "Template",
            source = "Card",
            tagClass = "Context-specific",
            isConstructed = true,
            isCritical = true,
            constraints = listOf("Must contain DF name (tag 84)", "Used in SELECT response"),
            examples = listOf("6F1A840E325041592E5359532E4444463031A508"),
            category = "File Management"
        ),
        "84" to TlvTagInfo(
            tag = "84",
            name = "Dedicated File Name",
            description = "Application identifier (AID) of the application",
            format = "Binary",
            source = "Card",
            tagClass = "Context-specific",
            isConstructed = false,
            isCritical = true,
            constraints = listOf("5-16 bytes", "Unique identifier", "RID + PIX format"),
            examples = listOf("325041592E5359532E4444463031"),
            category = "Application Selection"
        ),
        "A5" to TlvTagInfo(
            tag = "A5",
            name = "File Control Information Proprietary Template",
            description = "Proprietary template within FCI",
            format = "Template",
            source = "Card",
            tagClass = "Context-specific",
            isConstructed = true,
            isCritical = false,
            category = "File Management"
        ),
        "9F02" to TlvTagInfo(
            tag = "9F02",
            name = "Amount, Authorised",
            description = "Authorised amount of the transaction",
            format = "Numeric (BCD)",
            source = "Terminal",
            tagClass = "Application",
            isConstructed = false,
            isCritical = true,
            constraints = listOf("6 bytes", "BCD format", "Right justified with leading zeros"),
            examples = listOf("000000001000", "000000050000"),
            category = "Transaction Data"
        ),
        "5F2D" to TlvTagInfo(
            tag = "5F2D",
            name = "Language Preference",
            description = "Language preference for the cardholder",
            format = "Text (ISO 639)",
            source = "Card",
            tagClass = "Application",
            isConstructed = false,
            isCritical = false,
            constraints = listOf("2-8 bytes", "ISO 639 language codes"),
            examples = listOf("656E", "6672656E"),
            category = "Cardholder Data"
        ),
        "9F36" to TlvTagInfo(
            tag = "9F36",
            name = "Application Transaction Counter",
            description = "Counter maintained by the application",
            format = "Binary",
            source = "Card",
            tagClass = "Application",
            isConstructed = false,
            isCritical = true,
            constraints = listOf("2 bytes", "Incremented for each transaction"),
            examples = listOf("0001", "0042"),
            category = "Transaction Data"
        ),
        "9A" to TlvTagInfo(
            tag = "9A",
            name = "Transaction Date",
            description = "Local date that the transaction was authorised",
            format = "Numeric (YYMMDD)",
            source = "Terminal",
            tagClass = "Application",
            isConstructed = false,
            isCritical = true,
            constraints = listOf("3 bytes", "YYMMDD format"),
            examples = listOf("240315", "231225"),
            category = "Transaction Data"
        ),
        "9F21" to TlvTagInfo(
            tag = "9F21",
            name = "Transaction Time",
            description = "Local time that the transaction was authorised",
            format = "Numeric (HHMMSS)",
            source = "Terminal",
            tagClass = "Application",
            isConstructed = false,
            isCritical = false,
            constraints = listOf("3 bytes", "HHMMSS format"),
            examples = listOf("143052", "091530"),
            category = "Transaction Data"
        ),
        "57" to TlvTagInfo(
            tag = "57",
            name = "Track 2 Equivalent Data",
            description = "Track 2 data elements",
            format = "Binary",
            source = "Card",
            tagClass = "Application",
            isConstructed = false,
            isCritical = true,
            constraints = listOf("Up to 19 bytes", "Contains PAN and expiry date"),
            examples = listOf("4111111111111111D25122010000000000F"),
            category = "Cardholder Data"
        ),
        "5A" to TlvTagInfo(
            tag = "5A",
            name = "Application Primary Account Number",
            description = "Valid cardholder account number",
            format = "Compressed numeric",
            source = "Card",
            tagClass = "Application",
            isConstructed = false,
            isCritical = true,
            constraints = listOf("Up to 10 bytes", "BCD encoded PAN"),
            examples = listOf("4111111111111111"),
            category = "Cardholder Data"
        )
    )

    fun getTagInfo(tag: String): TlvTagInfo? = tags[tag.uppercase()]

    fun getAllTags(): List<TlvTagInfo> = tags.values.toList()

    fun searchTags(query: String): List<TlvTagInfo> {
        val searchTerm = query.lowercase()
        return tags.values.filter { tagInfo ->
            tagInfo.tag.lowercase().contains(searchTerm) ||
                    tagInfo.name.lowercase().contains(searchTerm) ||
                    tagInfo.description.lowercase().contains(searchTerm) ||
                    tagInfo.category.lowercase().contains(searchTerm)
        }
    }

    fun getTagsByCategory(category: String): List<TlvTagInfo> {
        return tags.values.filter { it.category == category }
    }
}

// Helper Functions
private fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "File Management" -> Icons.Default.Folder
        "Application Selection" -> Icons.Default.Apps
        "Transaction Data" -> Icons.Default.Payment
        "Cardholder Data" -> Icons.Default.Person
        "Security" -> Icons.Default.Security
        "Terminal Data" -> Icons.Default.Computer
        else -> Icons.Default.Tag
    }
}

private fun getPlaceholderText(mode: TlvEditMode): String {
    return when (mode) {
        TlvEditMode.HEX -> "Enter hexadecimal value (e.g., 4111111111111111)"
        TlvEditMode.ASCII -> "Enter ASCII text"
        TlvEditMode.DECIMAL -> "Enter decimal number"
    }
}