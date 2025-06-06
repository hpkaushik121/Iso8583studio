package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

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
import androidx.compose.ui.draw.scale
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
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.BitSpecific
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.clone
import `in`.aicortex.iso8583studio.data.getValue
import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.CodeFormat
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.data.rememberIsoCoroutineScope
import `in`.aicortex.iso8583studio.data.updateBit
import `in`.aicortex.iso8583studio.domain.FileImporter
import `in`.aicortex.iso8583studio.domain.ImportResult
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.service.PlaceholderProcessor
import `in`.aicortex.iso8583studio.domain.service.SimulatedRequest
import `in`.aicortex.iso8583studio.domain.utils.ExportResult
import `in`.aicortex.iso8583studio.domain.utils.FileExporter
import `in`.aicortex.iso8583studio.domain.utils.concatPathAndQuery
import `in`.aicortex.iso8583studio.domain.utils.parsePathAndQuery
import `in`.aicortex.iso8583studio.ui.screens.components.FieldInformationDialog
import `in`.aicortex.iso8583studio.ui.screens.components.LabeledSwitch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.absoluteValue

// Data classes
@Serializable
data class Transaction(
    val id: String,
    val mti: String,
    val proCode: String,
    val description: String,
    val fields: MutableList<BitAttribute>? = null,
    val restApiMatching: RestApiMatching = RestApiMatching(),
    val responseMapping: ResponseMapping = ResponseMapping()
)

@Serializable
data class RestApiMatching(
    val enabled: Boolean = false,
    val pathMatching: PathMatching = PathMatching(),
    val keyValueMatching: List<KeyValueMatcher> = emptyList(),
    val responseTemplate: String = "",
    val priority: Int = 0
)

@Serializable
data class PathMatching(
    val enabled: Boolean = false,
    val path: String = "",
    val query:  Map<String, String>? = null,
    val method: String = "POST",
    val exactMatch: Boolean = true
)

@Serializable
data class KeyValueMatcher(
    val key: String,
    val value: String,
    val operator: MatchOperator = MatchOperator.EQUALS
)

@Serializable
data class ResponseMapping(
    val enabled: Boolean = false,
    val responseFields: List<ResponseField> = emptyList(),
)

@Serializable
data class ResponseField(
    val value: String,
    val targetKey: String? = null,
    val targetHeader: String? = null,
)

@Serializable
enum class MatchOperator(val displayName: String, val symbol: String) {
    EQUALS("Equals", "=="),
    CONTAINS("Contains", "contains"),
    STARTS_WITH("Starts With", "startsWith"),
    ENDS_WITH("Ends With", "endsWith"),
    REGEX("Regex", "regex"),
    NOT_EQUALS("Not Equals", "!="),
    GREATER_THAN("Greater Than", ">"),
    LESS_THAN("Less Than", "<")
}

@Composable
fun ISO8583SettingsScreen(
    gw: GatewayServiceImpl,
    onSaveClick: () -> Unit
) {
    // Use mutableStateListOf for proper recomposition
    val transactions = remember {
        gw.configuration.simulatedTransactionsToSource.map { transaction ->
            transaction.copy(
                fields = transaction.fields?.toMutableList()
                    ?: Iso8583Data(config = gw.configuration).bitAttributes.toMutableList()
            )
        }.toMutableStateList()
    }

    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    // FIX 1: Add recomposition trigger for field changes
    var fieldChangeCounter by remember { mutableIntStateOf(0) }

    // Dialog states
    var showFieldInfoDialog by remember { mutableStateOf(false) }
    var isFirst by remember { mutableStateOf(gw.configuration.gatewayType == GatewayType.SERVER) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showEditTransactionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }


    // Stage function that updates the gateway config
    val stageTransactions = {
        gw.configuration.simulatedTransactionsToSource = transactions.map { transaction ->
            transaction.copy(
                fields = transaction.fields?.map { bitAttr ->
                    clone(bitAttr) // Create a copy to ensure immutability
                }?.toMutableList()
            )
        }

    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Enhanced Header
        EnhancedHeader(
            gw = gw,
            onFieldInfoClick = { showFieldInfoDialog = true },
            onExportClick = { showExportDialog = true },
            onImportClick = { showImportDialog = true },
            onSaveClick = {
                onSaveClick()
            },
            onToggle = {
                isFirst = it
            },
            isFirst = isFirst
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main Content
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Panel - Transaction List
            EnhancedTransactionPanel(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                transactions = transactions,
                selectedTransaction = selectedTransaction,
                onTransactionSelect = {
                    selectedTransaction = it
                    fieldChangeCounter++ // Force recomposition when selection changes
                },
                onAddTransaction = { showAddTransactionDialog = true },
                onEditTransaction = { transaction ->
                    transactionToEdit = transaction
                    showEditTransactionDialog = true
                },
                onDuplicateTransaction = { transaction ->
                    val duplicatedTransaction = transaction.copy(
                        id = generateTransactionId(),
                        description = "${transaction.description} (Copy)",
                        fields = transaction.fields?.map { clone(it) }?.toMutableList()
                    )
                    transactions.add(duplicatedTransaction)
                    stageTransactions()
                },
                onDeleteTransaction = { transaction ->
                    transactionToDelete = transaction
                    showDeleteConfirmDialog = true
                }
            )

            // FIX 2: Right Panel with proper recomposition tracking
            EnhancedFieldPanel(
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                selectedTransaction = selectedTransaction,
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                gw = gw.configuration,
                onTransactionUpdated = { updatedTransaction,isRecompose ->
                    val index = transactions.indexOfFirst { it.id == updatedTransaction.id }
                    if (index != -1) {
                        transactions[index] = updatedTransaction
                        selectedTransaction = updatedTransaction
                        if(isRecompose){
                            fieldChangeCounter++ // FIX 3: Increment counter on field changes
                        }
                    }
                    stageTransactions()
                },
                fieldChangeCounter = fieldChangeCounter // FIX 4: Pass counter to force recomposition
            )
        }
    }
    // Dialogs
    if (showFieldInfoDialog) {
        FieldInformationDialog(
            onCloseRequest = { showFieldInfoDialog = false }
        )
    }

    // Dialogs remain the same but with proper field change tracking
    if (showAddTransactionDialog) {
        EnhancedAddTransactionDialog(
            onDismiss = { showAddTransactionDialog = false },
            onSave = { newTransaction ->
                transactions.add(newTransaction)
                showAddTransactionDialog = false
                stageTransactions()
            },
            gw = gw.configuration
        )
    }

    if (showEditTransactionDialog && transactionToEdit != null) {
        EnhancedAddTransactionDialog(
            transaction = transactionToEdit!!,
            isEditing = true,
            onDismiss = {
                showEditTransactionDialog = false
                transactionToEdit = null
            },
            onSave = { updatedTransaction ->
                val index = transactions.indexOfFirst { it.id == updatedTransaction.id }
                if (index != -1) {
                    transactions[index] = updatedTransaction
                    if (selectedTransaction?.id == updatedTransaction.id) {
                        selectedTransaction = updatedTransaction
                        fieldChangeCounter++ // Track changes from edit dialog
                    }
                }
                showEditTransactionDialog = false
                transactionToEdit = null
                stageTransactions()
            },
            gw = gw.configuration
        )
    }

    if (showDeleteConfirmDialog && transactionToDelete != null) {
        DeleteConfirmationDialog(
            transaction = transactionToDelete!!,
            onDismiss = {
                showDeleteConfirmDialog = false
                transactionToDelete = null
            },
            onConfirm = {
                transactions.removeIf { it.id == transactionToDelete!!.id }
                if (selectedTransaction?.id == transactionToDelete!!.id) {
                    selectedTransaction = null
                    fieldChangeCounter++ // Track changes from deletion
                }
                showDeleteConfirmDialog = false
                transactionToDelete = null
                stageTransactions()
            }
        )
    }
    val isoCoroutine = rememberIsoCoroutineScope(gw)

    if (showExportDialog) {
        isoCoroutine.launch {
            val file = FileExporter().exportFile(
                window = gw.composeWindow,
                fileName = "ISO8583Studio_Transactions",
                fileExtension = "json",
                fileContent = Json.encodeToString(transactions.toList()).toByteArray(),
                fileDescription = "Transaction Configuration File"
            )
            showExportDialog = false
            when (file) {
                is ExportResult.Success -> {
                    gw.showSuccess {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Configuration exported successfully!")
                        }
                    }
                }

                is ExportResult.Cancelled -> {
                    println("Export cancelled")
                }

                is ExportResult.Error -> {
                    gw.showError {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text((file as ExportResult.Error).message)
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        isoCoroutine.launch {
            val file = FileImporter().importFile(
                window = gw.composeWindow,
                fileExtensions = listOf("json"),
                importLogic = { file ->
                    try {
                        val data: List<Transaction> = Json.decodeFromString(file.readText())
                        transactions.addAll(data)
                        stageTransactions()
                        ImportResult.Success(fileContent = file.readBytes())
                    } catch (e: Exception) {
                        ImportResult.Error("failed to import", e)
                    }

                }
            )
            showImportDialog = false
            when (file) {
                is ImportResult.Success -> {
                    gw.showSuccess {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Text("Configuration imported successfully!")
                        }
                    }
                }

                is ImportResult.Cancelled -> {
                    println("Import cancelled")
                }

                is ImportResult.Error -> {
                    gw.showError {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Text((file as ImportResult.Error).message)
                        }
                    }
                }
            }
        }

    }
}


@Composable
private fun EnhancedHeader(
    gw: GatewayServiceImpl,
    onFieldInfoClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onSaveClick: () -> Unit,
    onToggle:(Boolean) -> Unit,
    isFirst:Boolean ,
) {
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
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
                        text = "Transaction Simulator",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "Configure and manage ISO8583 transaction templates",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (gw.configuration.gatewayType == GatewayType.PROXY) {
                        LabeledSwitch(
                            checked = isFirst,
                            onCheckedChange = onToggle,
                            label = "Source"
                        )
                    }


                    ActionButton(
                        icon = Icons.Default.Info,
                        text = "Field Info",
                        onClick = onFieldInfoClick
                    )

                    ActionButton(
                        icon = Icons.Default.Upload,
                        text = "Export",
                        onClick = onExportClick
                    )

                    ActionButton(
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
private fun ActionButton(
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
private fun EnhancedTransactionPanel(
    modifier: Modifier,
    transactions: List<Transaction>,
    selectedTransaction: Transaction?,
    onTransactionSelect: (Transaction) -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDuplicateTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    // Use derivedStateOf for computed values to optimize recomposition
    val filteredTransactions by remember {
        derivedStateOf {
            transactions
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
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
                        text = "Transactions",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (filteredTransactions.size == transactions.size) {
                            "${transactions.size} total"
                        } else {
                            "${filteredTransactions.size} of ${transactions.size}"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                FloatingActionButton(
                    onClick = onAddTransaction,
                    modifier = Modifier.size(40.dp),
                    backgroundColor = Color.White,
                    contentColor = MaterialTheme.colors.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Transaction",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Transaction List

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = filteredTransactions,
                    key = { it.id } // FIX 9: Proper key for LazyColumn items
                ) { transaction ->
                    EnhancedTransactionCard(
                        transaction = transaction,
                        isSelected = selectedTransaction?.id == transaction.id,
                        onClick = { onTransactionSelect(transaction) },
                        onEdit = { onEditTransaction(transaction) },
                        onDuplicate = { onDuplicateTransaction(transaction) },
                        onDelete = { onDeleteTransaction(transaction) }
                    )
                }
            }

        }
    }
}


@Composable
private fun EnhancedTransactionCard(
    transaction: Transaction,
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
        border =  if (isSelected) BorderStroke(2.dp, color = MaterialTheme.colors.primary ) else null,
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
                            if (transaction.restApiMatching.enabled) {
                                Color.Green
                            } else {
                                MaterialTheme.colors.primary
                            },
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Transaction info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description.ifEmpty { "Unnamed Transaction" },
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
                        if (transaction.mti.isNotEmpty()) {
                            TransactionBadge("MTI", transaction.mti)
                        }
                        if (transaction.proCode.isNotEmpty()) {
                            TransactionBadge("PC", transaction.proCode)
                        }
                        if (transaction.restApiMatching.enabled) {
                            TransactionBadge("API", "✓", Color.Green)
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
                    ActionChip(
                        icon = Icons.Default.Edit,
                        text = "Edit",
                        onClick = onEdit
                    )

                    ActionChip(
                        icon = Icons.Default.FileCopy,
                        text = "Duplicate",
                        onClick = onDuplicate
                    )

                    ActionChip(
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
private fun TransactionBadge(
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
private fun ActionChip(
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
private fun EnhancedFieldPanel(
    modifier: Modifier,
    selectedTransaction: Transaction?,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    gw: GatewayConfig,
    onTransactionUpdated: (Transaction, Boolean) -> Unit,
    fieldChangeCounter: Int = 0 // FIX 6: Add field change counter parameter
) {
    Card(
        modifier = modifier,
        elevation = 6.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            if (selectedTransaction != null) {
                // FIX 7: Use key with field change counter to force recomposition
                key(selectedTransaction.id, fieldChangeCounter) {
                    // Tab Header
                    TabRow(
                        selectedTabIndex = selectedTab,
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { onTabChange(0) },
                            text = { Text("Fields", fontSize = 14.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { onTabChange(1) },
                            text = { Text("Config", fontSize = 14.sp) }
                        )
                        if (gw.codeFormatSource?.requiresYamlConfig == true) {
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { onTabChange(2) },
                                text = { Text("API", fontSize = 14.sp) }
                            )
                        }
                    }

                    // Tab Content
                    when (selectedTab) {
                        0 -> FieldsTab(
                            transaction = selectedTransaction,
                            gw = gw,
                            onTransactionUpdated = { onTransactionUpdated(it,true)},
                            fieldChangeCounter = fieldChangeCounter // FIX 8: Pass counter down
                        )

                        1 -> ConfigurationTab(selectedTransaction)
                        2 -> ApiConfigurationTab(
                            transaction = selectedTransaction,
                            onTransactionUpdated =  { onTransactionUpdated(it,false)}
                        )
                    }
                }
            } else {
                EmptySelectionPanel()
            }
        }
    }
}


@Composable
private fun FieldsTab(
    transaction: Transaction,
    gw: GatewayConfig,
    onTransactionUpdated: (Transaction) -> Unit,
    fieldChangeCounter: Int = 0 // FIX 10: Add field change counter
) {
    // FIX 11: Create computed properties that depend on field change counter
    val activeFieldsCount by remember(transaction.fields, fieldChangeCounter) {
        derivedStateOf {
            transaction.fields?.count { it.isSet } ?: 0
        }
    }

    val activeFields by remember(transaction.fields, fieldChangeCounter) {
        derivedStateOf {
            transaction.fields?.withIndex()?.filter { it.value.isSet } ?: emptyList()
        }
    }

    // FIX 12: Use key with multiple dependencies to force complete recomposition
    key(transaction.id, fieldChangeCounter, activeFieldsCount) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fields header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.surface.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ISO8583 Fields",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = "$activeFieldsCount fields configured",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            // Quick Add Fields Bar
            QuickAddFieldsBar(
                gw = gw,
                existingFields = activeFields.map { it.index + 1 }.toSet(),
                onFieldsAdded = { bitNumbers ->
                    val updatedFields = transaction.fields?.toMutableList() ?: mutableListOf()
                    var hasChanges = false
                    bitNumbers.forEach { bit ->
                        val index = bit.bitNumber.toInt().absoluteValue - 1
                        if (index < updatedFields.size && !updatedFields[index].isSet) {
                            updatedFields[index].updateBit("")
                            hasChanges = true
                        }
                    }
                    if (hasChanges) {
                        onTransactionUpdated(transaction.copy(fields = updatedFields))
                    }
                }
            )

            // Fields List
            if (activeFields.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = activeFields,
                        key = { _, (index, field) ->
                            "${transaction.id}_field_${index}_${field.isSet}_${fieldChangeCounter}"
                        }
                    ) { _, (index, field) ->
                        EnhancedFieldRow(
                            fieldNumber = index + 1,
                            data = field,
                            onDataChange = { newValue ->
                                val updatedFields =
                                    transaction.fields?.toMutableList() ?: mutableListOf()
                                updatedFields[index].updateBit(newValue)
//                                onTransactionUpdated(transaction.copy(fields = updatedFields))
                            },
                            onRemove = {
                                val updatedFields =
                                    transaction.fields?.toMutableList() ?: mutableListOf()
                                updatedFields[index].isSet = false
                                onTransactionUpdated(transaction.copy(fields = updatedFields))
                            },
                            gw = gw,
                            updateKey = fieldChangeCounter
                        )
                    }
                }
            } else {
                EmptyFieldsList()
            }

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.surface.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                var showAddFieldDialog by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showAddFieldDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Field")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Fields")
                }

                if (showAddFieldDialog) {
                    EnhancedAddFieldDialog(
                        gw = gw,
                        existingFields = activeFields.map { it.index + 1 }.toSet(),
                        onSave = { bitNumbers ->
                            val updatedFields =
                                transaction.fields?.toMutableList() ?: mutableListOf()
                            var hasChanges = false
                            bitNumbers.forEach { bit ->
                                val index = bit.bitNumber.toInt().absoluteValue - 1
                                if (index < updatedFields.size && !updatedFields[index].isSet) {
                                    updatedFields[index].updateBit("")
                                    hasChanges = true
                                }
                            }
                            if (hasChanges) {
                                onTransactionUpdated(transaction.copy(fields = updatedFields))
                            }
                            showAddFieldDialog = false
                        },
                        onDismiss = { showAddFieldDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddFieldsBar(
    gw: GatewayConfig,
    existingFields: Set<Int> = emptySet(),
    onFieldsAdded: (List<BitSpecific>) -> Unit
) {
    val commonFields = remember {
        listOf(
            Pair(2, "PAN"),
            Pair(3, "Processing Code"),
            Pair(4, "Amount"),
            Pair(11, "STAN"),
            Pair(12, "Time"),
            Pair(13, "Date"),
            Pair(22, "POS Entry Mode"),
            Pair(37, "Retrieval Ref"),
            Pair(41, "Terminal ID"),
            Pair(42, "Merchant ID")
        )
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary.copy(alpha = 0.05f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(commonFields) { (bitNumber, name) ->
            val bitSpecific = gw.bitTemplateSource.find { it.bitNumber.toInt() == bitNumber }
            val isAlreadyAdded = existingFields.contains(bitNumber)

            if (bitSpecific != null) {
                Surface(
                    modifier = Modifier.clickable(enabled = !isAlreadyAdded) {
                        if (!isAlreadyAdded) {
                            onFieldsAdded(listOf(bitSpecific))
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
                            text = "$bitNumber",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAlreadyAdded) {
                                MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colors.primary
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = name,
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
}

@Composable
private fun EmptyFieldsList() {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = "No Fields Configured",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Use quick add above or click 'Add Fields' to get started",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EnhancedFieldRow(
    fieldNumber: Int,
    data: BitAttribute,
    onDataChange: (String) -> Unit,
    onRemove: () -> Unit,
    gw: GatewayConfig,
    updateKey: Int = 0,
) {
    // Use update key in the key function to force recomposition
    key(fieldNumber, data.getValue(), data.isSet, updateKey) {
        var value by remember(data.getValue(), updateKey) {
            mutableStateOf(data.getValue() ?: "")
        }

        // Track if we're actively editing to prevent accidental collapse
        var isEditing by remember { mutableStateOf(false) }
        var isExpanded by remember { mutableStateOf(false) }
        var isFocused by remember { mutableStateOf(false) }

        // Update local state when data changes
        LaunchedEffect(data.getValue(), updateKey) {
            val newValue = data.getValue() ?: ""
            if (value != newValue) {
                value = newValue
            }
        }

        val bitInfo = remember(gw.bitTemplateSource, fieldNumber) {
            gw.bitTemplateSource.find { it.bitNumber.toInt() == fieldNumber }
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
                                    if(!isEditing && !isFocused){
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
                        // Enhanced field number badge
                        Card(
                            backgroundColor = if (isExpanded)
                                MaterialTheme.colors.primary
                            else
                                MaterialTheme.colors.secondary,
                            shape = RoundedCornerShape(8.dp),
                            elevation = if (isExpanded) 4.dp else 2.dp
                        ) {
                            Text(
                                text = "F$fieldNumber",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Enhanced field info with status indicators
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = bitInfo?.description ?: "Field $fieldNumber",
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
                                    contentDescription = "Remove Field",
                                    tint = MaterialTheme.colors.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Expand/collapse button (separate from row click)
                            IconButton(
                                onClick = { if(!isEditing && !isFocused){
                                    isExpanded = (!isExpanded)
                                } },
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
                            // Enhanced field metadata
                            if (bitInfo != null) {
                                Text(
                                    text = "Field Specifications",
                                    style = MaterialTheme.typography.subtitle2,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    item {
                                        EnhancedMetadataChip(
                                            icon = Icons.Default.Category,
                                            label = "Type",
                                            value = bitInfo.bitType.name,
                                            color = MaterialTheme.colors.primary
                                        )
                                    }
                                    item {
                                        EnhancedMetadataChip(
                                            icon = Icons.Default.Straighten,
                                            label = "Max Length",
                                            value = if (bitInfo.maxLength > 0)
                                                bitInfo.maxLength.toString()
                                            else
                                                "Variable",
                                            color = MaterialTheme.colors.secondary
                                        )
                                    }
                                    item {
                                        EnhancedMetadataChip(
                                            icon = Icons.Default.FormatSize,
                                            label = "Length Type",
                                            value = bitInfo.bitLength.name,
                                            color = MaterialTheme.colors.onPrimary
                                        )
                                    }
                                    if (bitInfo.description?.isNotEmpty() == true) {
                                        item {
                                            EnhancedMetadataChip(
                                                icon = Icons.Default.Info,
                                                label = "Description",
                                                value = bitInfo.description ?: "",
                                                color = MaterialTheme.colors.onSecondary,
                                                expanded = true
                                            )
                                        }
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
                                        text = "Field Value",
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
                                        onDataChange(newValue)
                                        isEditing = true
                                    },
                                    label = {
                                        Text("Enter value for field $fieldNumber")
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
                                            "Enter field value...",
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                                        )
                                    },
                                    singleLine = false,
                                    maxLines = 4,
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
                                                    onDataChange("")
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
                                if (bitInfo != null && value.isNotEmpty()) {
                                    val isValidLength = when (bitInfo.lengthType){
                                        BitLength.LLLVAR,
                                            BitLength.LLVAR->{
                                            bitInfo.maxLength <= 0 || value.length <= bitInfo.maxLength
                                            }

                                        BitLength.FIXED -> {
                                            value.length == bitInfo.maxLength  || PlaceholderProcessor.holdersList.contains(value)
                                        }
                                    }

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
                                                if (isValidLength) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = if (isValidLength)
                                                    MaterialTheme.colors.primary
                                                else
                                                    MaterialTheme.colors.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (isValidLength) "Valid" else "Invalid",
                                                style = MaterialTheme.typography.caption,
                                                color = if (isValidLength)
                                                    MaterialTheme.colors.primary
                                                else
                                                    MaterialTheme.colors.error
                                            )
                                        }

                                        if (bitInfo.maxLength > 0) {
                                            Text(
                                                text = "${value.length}/${bitInfo.maxLength}",
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
private fun EnhancedMetadataChip(
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
private fun ConfigurationTab(transaction: Transaction) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ConfigSection("Basic Information") {
                ConfigRow("Transaction ID", transaction.id)
                ConfigRow("Description", transaction.description)
                if (transaction.mti.isNotEmpty()) {
                    ConfigRow("Message Type Indicator", transaction.mti)
                }
                if (transaction.proCode.isNotEmpty()) {
                    ConfigRow("Processing Code", transaction.proCode)
                }
            }
        }

        item {
            ConfigSection("Field Statistics") {
                val fieldCount = transaction.fields?.count { it.isSet } ?: 0
                val totalFields = transaction.fields?.size ?: 0
                ConfigRow("Fields Configured", "$fieldCount of $totalFields")
                ConfigRow(
                    "Configuration Type",
                    if (transaction.restApiMatching.enabled) "Advanced (REST API)" else "Basic (ISO8583)"
                )
            }
        }

        if (transaction.restApiMatching.enabled) {
            item {
                ConfigSection("REST API Configuration") {
                    ConfigRow("Priority", transaction.restApiMatching.priority.toString())
                    ConfigRow(
                        "Path Matching",
                        if (transaction.restApiMatching.pathMatching.enabled) "Enabled" else "Disabled"
                    )
                    if (transaction.restApiMatching.pathMatching.enabled) {
                        ConfigRow("HTTP Method", transaction.restApiMatching.pathMatching.method)
                        ConfigRow("Path", transaction.restApiMatching.pathMatching.path)
                    }
                    ConfigRow(
                        "Key-Value Matchers",
                        "${transaction.restApiMatching.keyValueMatching.size} configured"
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigSection(
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
private fun ConfigRow(label: String, value: String) {
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
private fun ApiConfigurationTab(
    transaction: Transaction,
    onTransactionUpdated: (Transaction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // REST API Matching Section
            RestApiMatchingCard(
                restApiMatching = transaction.restApiMatching,
                onRestApiMatchingChange = { newMatching ->
                    onTransactionUpdated(transaction.copy(restApiMatching = newMatching))
                }
            )
        }

        item {
            // Response Mapping Section
            ResponseMappingCard(
                responseMapping = transaction.responseMapping,
                onResponseMappingChange = { newMapping ->
                    onTransactionUpdated(transaction.copy(responseMapping = newMapping))
                }
            )
        }
    }
}

@Composable
private fun RestApiMatchingCard(
    restApiMatching: RestApiMatching,
    onRestApiMatchingChange: (RestApiMatching) -> Unit
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
                    text = "REST API Matching",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                Switch(
                    checked = restApiMatching.enabled,
                    onCheckedChange = {
                        onRestApiMatchingChange(restApiMatching.copy(enabled = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary
                    )
                )
            }

            if (restApiMatching.enabled) {
                // Priority setting
                PrioritySelector(
                    priority = restApiMatching.priority,
                    onPriorityChange = {
                        onRestApiMatchingChange(restApiMatching.copy(priority = it))
                    }
                )

                Divider()

                // Path matching
                PathMatchingSection(
                    pathMatching = restApiMatching.pathMatching,
                    onPathMatchingChange = {
                        onRestApiMatchingChange(restApiMatching.copy(pathMatching = it))
                    }
                )

                Divider()

                // Key-value matchers
                KeyValueMatchersSection(
                    keyValueMatchers = restApiMatching.keyValueMatching,
                    onKeyValueMatchersChange = {
                        onRestApiMatchingChange(restApiMatching.copy(keyValueMatching = it))
                    }
                )
            } else {
                Text(
                    text = "Enable REST API matching to configure advanced request routing based on HTTP paths and request content.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ResponseMappingCard(
    responseMapping: ResponseMapping,
    onResponseMappingChange: (ResponseMapping) -> Unit
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
                ResponseFieldsSection(
                    responseFields = responseMapping.responseFields,
                    onResponseFieldsChange = {
                        onResponseMappingChange(responseMapping.copy(responseFields = it))
                    }
                )

            } else {
                Text(
                    text = "Enable response mapping to customize how transaction responses are formatted and which fields are included.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun PrioritySelector(
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
                text = "Higher priority transactions are matched first",
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
private fun PathMatchingSection(
    pathMatching: PathMatching,
    onPathMatchingChange: (PathMatching) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Path Matching",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )

            Switch(
                checked = pathMatching.enabled,
                onCheckedChange = {
                    onPathMatchingChange(pathMatching.copy(enabled = it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colors.primary
                )
            )
        }

        if (pathMatching.enabled) {
            // HTTP Method Selection
            Text(
                text = "HTTP Method:",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(listOf("GET", "POST", "PUT", "DELETE", "PATCH")) { method ->
                    FilterChip(
                        selected = pathMatching.method == method,
                        onClick = {
                            onPathMatchingChange(pathMatching.copy(method = method))
                        },
                        modifier = Modifier.height(28.dp)
                    ) { Text(method, fontSize = 10.sp) }
                }
            }

            // Path Input
            PathTextFieldWithDebounce(
                pathMatching = pathMatching,
                onPathMatchingChange = onPathMatchingChange
            )

            // Exact Match Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = pathMatching.exactMatch,
                    onCheckedChange = {
                        onPathMatchingChange(pathMatching.copy(exactMatch = it))
                    }
                )
                Text(
                    text = "Exact path match (uncheck for pattern matching with *)",
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}
// Alternative approach using a debounced parsing strategy
@Composable
fun PathTextFieldWithDebounce(
    pathMatching: PathMatching,
    onPathMatchingChange: (PathMatching) -> Unit,
    modifier: Modifier = Modifier,
    debounceDelayMs: Long = 500L // Wait 500ms after user stops typing
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(
            text = concatPathAndQuery(pathMatching.path, pathMatching.query),
            selection = TextRange.Zero
        ))
    }

    // Debounced parsing effect
    LaunchedEffect(textFieldValue.text) {
        delay(debounceDelayMs)

        // Only parse if the text hasn't changed during the delay
        val currentText = textFieldValue.text
        if (currentText == textFieldValue.text) {
            val (path, query) = parsePathAndQuery(currentText.trim())
            onPathMatchingChange(pathMatching.copy(
                path = path ?: "",
                query = query
            ))
        }
    }

    // Update local state when external pathMatching changes
    LaunchedEffect(pathMatching) {
        val newText = concatPathAndQuery(pathMatching.path, pathMatching.query)
        if (textFieldValue.text != newText) {
            textFieldValue = textFieldValue.copy(text = newText)
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
        },
        label = { Text("API Path") },
        placeholder = { Text("/api/payment?amount=100&currency=USD") },
        modifier = modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(Icons.Default.Link, contentDescription = "Path")
        },
        trailingIcon = {
            if (textFieldValue.text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        textFieldValue = TextFieldValue("")
                        onPathMatchingChange(pathMatching.copy(path = "", query = null))
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        isError = textFieldValue.text.isNotEmpty() && !isValidPathFormat(textFieldValue.text),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                // Force immediate parsing when user is done
                val (path, query) = parsePathAndQuery(textFieldValue.text.trim())
                onPathMatchingChange(pathMatching.copy(
                    path = path ?: "",
                    query = query
                ))
            }
        )
    )
}

// Utility function to validate path format without parsing
private fun isValidPathFormat(text: String): Boolean {
    return text.startsWith("/") && !text.contains("//") && !text.contains(" ")
}


@Composable
private fun KeyValueMatchersSection(
    keyValueMatchers: List<KeyValueMatcher>,
    onKeyValueMatchersChange: (List<KeyValueMatcher>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Key-Value Matching",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    onKeyValueMatchersChange(
                        keyValueMatchers + KeyValueMatcher(
                            key = "",
                            value = "",
                            operator = MatchOperator.EQUALS
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Matcher")
            }
        }

        if (keyValueMatchers.isEmpty()) {
            Text(
                text = "No key-value matchers configured. Add matchers to check specific fields in requests.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        } else {
            keyValueMatchers.forEachIndexed { index, keyValueMatcher ->
                Column {
                    KeyValueMatcherRow(
                        matcher = keyValueMatchers[index],
                        onMatcherChange = { newMatcher ->
                            val updatedList = keyValueMatchers.toMutableList()
                            updatedList[index] = newMatcher
                            onKeyValueMatchersChange(updatedList)
                        },
                        onRemove = {
                            onKeyValueMatchersChange(
                                keyValueMatchers.filterIndexed { i, _ -> i != index }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyValueMatcherRow(
    matcher: KeyValueMatcher,
    onMatcherChange: (KeyValueMatcher) -> Unit,
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
                    text = "Matcher ${if (matcher.key.isNotBlank()) "- ${matcher.key}" else ""}",
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

            // Key input


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Operator selection
                var operatorExpanded by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = matcher.key,
                    onValueChange = { onMatcherChange(matcher.copy(key = it)) },
                    label = { Text("Key Path") },
                    placeholder = { Text("e.g., transaction.type") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

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
                        MatchOperator.values().forEach { operator ->
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
private fun ResponseFieldsSection(
    responseFields: List<ResponseField>,
    onResponseFieldsChange: (List<ResponseField>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Response Field Mappings",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    onResponseFieldsChange(
                        responseFields + ResponseField(
                            value = "",
                            targetKey = "",
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Response Field")
            }
        }

        if (responseFields.isEmpty()) {
            Text(
                text = "No response field mappings configured. Add mappings to transform request fields into response.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        } else {
            responseFields.forEachIndexed { index, field ->
                Column {
                    ResponseFieldRow(
                        responseField = responseFields[index],
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
}

@Composable
private fun ResponseFieldRow(
    responseField: ResponseField,
    onResponseFieldChange: (ResponseField) -> Unit,
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
                    text = "Field Mapping",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove", tint = MaterialTheme.colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Target key
                OutlinedTextField(
                    value = responseField.targetKey ?: "",
                    onValueChange = { onResponseFieldChange(responseField.copy(targetKey = it.takeIf { it.isNotBlank() })) },
                    label = { Text("Key Path") },
                    placeholder = { Text("e.g., header.mti") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Target nested key
                OutlinedTextField(
                    value = responseField.value,
                    onValueChange = { onResponseFieldChange(responseField.copy(value = it)) },
                    label = { Text("Value") },
                    placeholder = { Text("e.g., 00000, [SV]") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

        }
    }
}



@Composable
private fun EmptySelectionPanel() {
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
                Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )

            Text(
                text = "Select a Transaction",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Text(
                text = "Choose a transaction from the list to view and edit its fields and configuration",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )

        }
    }
}

@Composable
private fun EnhancedAddTransactionDialog(
    transaction: Transaction? = null,
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    gw: GatewayConfig
) {
    val codeFormat = gw.codeFormatSource ?: CodeFormat.BYTE_ARRAY
    val isAdvancedMode = codeFormat.requiresYamlConfig

    var mti by remember { mutableStateOf(transaction?.mti ?: "") }
    var proCode by remember { mutableStateOf(transaction?.proCode ?: "") }
    var description by remember { mutableStateOf(transaction?.description ?: "") }
    var restApiMatching by remember {
        mutableStateOf(
            transaction?.restApiMatching ?: RestApiMatching()
        )
    }
    var responseMapping by remember {
        mutableStateOf(
            transaction?.responseMapping ?: ResponseMapping()
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
                            text = if (isEditing) "Edit Transaction" else "Create New Transaction",
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
                        FormatIndicatorCard(codeFormat, isAdvancedMode)
                    }

                    item {
                        BasicInformationCard(
                            description = description,
                            onDescriptionChange = { description = it },
                            mti = mti,
                            onMtiChange = { mti = it },
                            proCode = proCode,
                            onProCodeChange = { proCode = it },
                            isAdvancedMode = isAdvancedMode
                        )
                    }

                    if (isAdvancedMode) {
                        item {
                            RestApiConfigurationCard(
                                restApiMatching = restApiMatching,
                                onRestApiMatchingChange = { restApiMatching = it }
                            )
                        }

                        item {
                            ResponseConfigurationCard(
                                responseMapping = responseMapping,
                                onResponseMappingChange = { responseMapping = it }
                            )
                        }
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
                                val newTransaction = Transaction(
                                    id = transaction?.id ?: generateTransactionId(),
                                    mti = mti,
                                    proCode = proCode,
                                    description = description,
                                    fields = transaction?.fields?.toMutableList() ?: Iso8583Data(
                                        config = gw
                                    ).bitAttributes.toMutableList(),
                                    restApiMatching = if (isAdvancedMode) restApiMatching else RestApiMatching(),
                                    responseMapping = if (isAdvancedMode) responseMapping else ResponseMapping()
                                )
                                onSave(newTransaction)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = description.isNotBlank() &&
                                    (isAdvancedMode || (mti.isNotBlank() && proCode.isNotBlank()))
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
private fun FormatIndicatorCard(codeFormat: CodeFormat, isAdvancedMode: Boolean) {
    Card(
        elevation = 2.dp,
        backgroundColor = if (isAdvancedMode) {
            Color(0xFF4CAF50).copy(alpha = 0.1f)
        } else {
            MaterialTheme.colors.primary.copy(alpha = 0.1f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isAdvancedMode) Icons.Default.CloudQueue else Icons.Default.Code,
                contentDescription = "Format Type",
                tint = if (isAdvancedMode) Color(0xFF4CAF50) else MaterialTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Format: ${codeFormat.displayName}",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = if (isAdvancedMode) Color(0xFF4CAF50) else MaterialTheme.colors.primary
                )
                Text(
                    text = if (isAdvancedMode) {
                        "Advanced mode with REST API support"
                    } else {
                        "Basic ISO8583 format"
                    },
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun BasicInformationCard(
    description: String,
    onDescriptionChange: (String) -> Unit,
    mti: String,
    onMtiChange: (String) -> Unit,
    proCode: String,
    onProCodeChange: (String) -> Unit,
    isAdvancedMode: Boolean
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
                placeholder = { Text("Enter transaction description") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = "Description")
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = mti,
                    onValueChange = onMtiChange,
                    label = { if (!isAdvancedMode) Text("MTI *") else Text("MTI") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("0200") },
                    leadingIcon = {
                        Icon(Icons.Default.Code, contentDescription = "MTI")
                    }
                )

                OutlinedTextField(
                    value = proCode,
                    onValueChange = onProCodeChange,
                    label = { if (!isAdvancedMode) Text("Processing Code *") else Text("Processing Code ") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("000000") },
                    leadingIcon = {
                        Icon(Icons.Default.CheckBox, contentDescription = "Processing Code")
                    }
                )
            }

        }
    }
}

@Composable
private fun RestApiConfigurationCard(
    restApiMatching: RestApiMatching,
    onRestApiMatchingChange: (RestApiMatching) -> Unit
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
                    text = "REST API Matching",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                Switch(
                    checked = restApiMatching.enabled,
                    onCheckedChange = {
                        onRestApiMatchingChange(restApiMatching.copy(enabled = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary
                    )
                )
            }

            if (restApiMatching.enabled) {
                Text(
                    text = "This transaction will use advanced REST API matching rules. Configure detailed settings after creation.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ResponseConfigurationCard(
    responseMapping: ResponseMapping,
    onResponseMappingChange: (ResponseMapping) -> Unit
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
                    text = "Custom response mapping will be applied. Configure detailed field mappings after creation.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
private fun EnhancedAddFieldDialog(
    gw: GatewayConfig,
    existingFields: Set<Int> = emptySet(),
    onSave: (List<BitSpecific>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBitSpecifics by remember { mutableStateOf(setOf<BitSpecific>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Animation states
    val slideInAnimation = slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(400, easing = FastOutSlowInEasing)
    )
    val fadeInAnimation = fadeIn(animationSpec = tween(300))

    val categories = listOf(
        "All" to Icons.Default.List,
        "Common" to Icons.Default.Star,
        "PAN & Security" to Icons.Default.Security,
        "Amount & Date" to Icons.Default.Payment,
        "Terminal & Merchant" to Icons.Default.Store,
        "Network" to Icons.Default.NetworkCheck
    )

    val categorizedBits = remember(gw.bitTemplateSource, selectedCategory, existingFields) {
        val commonBits = setOf(2, 3, 4, 11, 12, 13, 22, 37, 41, 42)
        val panSecurityBits = setOf(2, 14, 22, 23, 35, 45, 52, 53, 55)
        val amountDateBits = setOf(4, 5, 6, 9, 10, 12, 13, 16, 50, 51)
        val terminalBits = setOf(18, 19, 25, 32, 33, 41, 42, 43, 49)
        val networkBits = setOf(1, 7, 8, 11, 15, 24, 38, 39, 70, 95)

        gw.bitTemplateSource.filter { bit ->
            val bitNum = bit.bitNumber.toInt()
            if (existingFields.contains(bitNum)) return@filter false

            when (selectedCategory) {
                "Common" -> bitNum in commonBits
                "PAN & Security" -> bitNum in panSecurityBits
                "Amount & Date" -> bitNum in amountDateBits
                "Terminal & Merchant" -> bitNum in terminalBits
                "Network" -> bitNum in networkBits
                else -> true
            }
        }.filter { bit ->
            if (searchQuery.isBlank()) true
            else {
                bit.bitNumber.toString().contains(searchQuery, ignoreCase = true) ||
                        bit.description?.contains(searchQuery, ignoreCase = true) == true ||
                        bit.bitType.name.contains(searchQuery, ignoreCase = true)
            }
        }.sortedBy { it.bitNumber.toInt() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInAnimation + fadeInAnimation
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colors.surface,
                elevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.background(color = MaterialTheme.colors.surface)
                ) {
                    // Enhanced Header with gradient background
                    Box {
                        Surface(
                            color = MaterialTheme.colors.primary,
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Gradient overlay effect
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colors.primary,
                                                MaterialTheme.colors.primary.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "Add ISO8583 Fields",
                                                style = MaterialTheme.typography.h6,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = selectedBitSpecifics.isNotEmpty(),
                                            enter = fadeIn() + expandVertically(),
                                            exit = fadeOut() + shrinkVertically()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(top = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "${selectedBitSpecifics.size} field(s) selected",
                                                    style = MaterialTheme.typography.caption,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .background(
                                                Color.White.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Enhanced Search and Filter Section
                    Column(
                        modifier = Modifier.padding(20.dp)
                            ,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Smart Search Bar
                        Card(
                            elevation = if (isSearchActive) 8.dp else 2.dp,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.animateContentSize()
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isSearchActive = it.isFocused },
                                placeholder = {
                                    Text(
                                        "Search by field number, description, or type...",
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = if (isSearchActive) MaterialTheme.colors.primary
                                        else MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                    )
                                },
                                trailingIcon = {
                                    AnimatedVisibility(
                                        visible = searchQuery.isNotEmpty(),
                                        enter = fadeIn() + scaleIn(),
                                        exit = fadeOut() + scaleOut()
                                    ) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colors.primary,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Enhanced Category Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(categories) { (category, icon) ->
                                val isSelected = selectedCategory == category

                                Card(
                                    elevation = if (isSelected) 8.dp else 2.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    backgroundColor = if (isSelected)
                                        MaterialTheme.colors.primary
                                    else
                                        MaterialTheme.colors.surface,
                                    modifier = Modifier
                                        .clickable { selectedCategory = category }
                                        .animateContentSize()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White
                                            else MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = category,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White
                                            else MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Enhanced Status Row
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
                                    Icons.Default.List,
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${categorizedBits.size} fields available",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            AnimatedVisibility(
                                visible = existingFields.isNotEmpty(),
                                enter = fadeIn() + slideInHorizontally(),
                                exit = fadeOut() + slideOutHorizontally()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${existingFields.size} already added",
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.secondary
                                    )
                                }
                            }
                        }
                    }

                    // Enhanced Fields List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categorizedBits) { bit ->
                            val isSelected = selectedBitSpecifics.contains(bit)

                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically(),
                                modifier = Modifier.animateItem()
                            ) {
                                EnhancedFieldCard(
                                    bit = bit,
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedBitSpecifics = if (isSelected) {
                                            selectedBitSpecifics - bit
                                        } else {
                                            selectedBitSpecifics + bit
                                        }
                                    }
                                )
                            }
                        }

                        // Empty state
                        if (categorizedBits.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    message = if (searchQuery.isNotEmpty())
                                        "No fields match your search criteria"
                                    else
                                        "No fields available in this category",
                                    onReset = {
                                        searchQuery = ""
                                        selectedCategory = "All"
                                    }
                                )
                            }
                        }
                    }

                    // Enhanced Action Bar
                    Surface(
                        color = MaterialTheme.colors.background,
                        elevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel", fontWeight = FontWeight.Medium)
                            }

                            Button(
                                onClick = {
                                    if (selectedBitSpecifics.isNotEmpty()) {
                                        onSave(selectedBitSpecifics.toList())
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = selectedBitSpecifics.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                elevation = ButtonDefaults.elevation(
                                    defaultElevation = if (selectedBitSpecifics.isNotEmpty()) 4.dp else 0.dp
                                )
                            ) {
                                AnimatedContent(
                                    targetState = selectedBitSpecifics.size,
                                    transitionSpec = { fadeIn() with fadeOut() }
                                ) { count ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            if (count > 0) Icons.Default.Add else Icons.Default.AddCircleOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (count > 0) "Add $count Field(s)" else "Add Fields",
                                            fontWeight = FontWeight.Bold
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EnhancedFieldCard(
    bit: BitSpecific,
    isSelected: Boolean,
    onClick: () -> Unit
) {


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border =  if (isSelected) BorderStroke(2.dp, color = MaterialTheme.colors.primary ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enhanced Selection Indicator
            Card(
                shape = CircleShape,
                backgroundColor = if (isSelected)
                    MaterialTheme.colors.primary
                else
                    MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                elevation = if (isSelected) 4.dp else 0.dp
            ) {
                AnimatedContent(
                    targetState = isSelected,
                    transitionSpec = { scaleIn() + fadeIn() with scaleOut() + fadeOut() }
                ) { selected ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Enhanced Field Information
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Field Number Badge
                    Surface(
                        color = if (isSelected)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.secondary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "F${bit.bitNumber.toInt().absoluteValue}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Field Description
                    Text(
                        text = bit.description ?: "Field ${bit.bitNumber}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = if (isSelected)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Enhanced Metadata Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetadataChip(
                        label = "Type",
                        value = bit.bitType.name,
                        color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    )
                    MetadataChip(
                        label = "Max Length",
                        value = if (bit.maxLength > 0) bit.maxLength.toString() else "Variable",
                        color = MaterialTheme.colors.secondary.copy(alpha = 0.1f)
                    )
                    MetadataChip(
                        label = "Length Type ",
                        value = bit.lengthType.name,
                        color = MaterialTheme.colors.secondary.copy(alpha = 0.1f)
                    )
                    MetadataChip(
                        label = "Format",
                        value = bit.bitLength.name,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                    )
                }
            }

            // Selection Arrow
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally()
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MetadataChip(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label:",
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    message: String,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = message,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            TextButton(
                onClick = onReset,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear Filters")
            }
        }
    }
}



@Composable
private fun DeleteConfirmationDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colors.error
                )
                Text("Delete Transaction")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Are you sure you want to delete this transaction?")

                Card(
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                    elevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = transaction.description.ifEmpty { "Unnamed Transaction" },
                            fontWeight = FontWeight.Bold
                        )
                        if (transaction.mti.isNotEmpty()) {
                            Text(
                                "MTI: ${transaction.mti}",
                                style = MaterialTheme.typography.caption
                            )
                        }
                        if (transaction.proCode.isNotEmpty()) {
                            Text(
                                "Processing Code: ${transaction.proCode}",
                                style = MaterialTheme.typography.caption
                            )
                        }
                        Text(
                            "Fields: ${transaction.fields?.count { it.isSet } ?: 0} configured",
                            style = MaterialTheme.typography.caption
                        )
                    }
                }

            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun generateTransactionId(): String {
    return "TXN_${System.currentTimeMillis()}_${(1000..9999).random()}"
}