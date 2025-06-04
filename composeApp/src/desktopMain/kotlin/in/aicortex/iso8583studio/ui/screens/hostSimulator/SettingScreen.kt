package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import `in`.aicortex.iso8583studio.data.model.CodeFormat
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.updateBit
import `in`.aicortex.iso8583studio.domain.utils.FormatMappingConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FieldInformationDialog
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
    val staticValues: List<StaticResponseValue> = emptyList()
)

@Serializable
data class ResponseField(
    val sourceField: String,
    val targetKey: String? = null,
    val targetNestedKey: String? = null,
    val targetHeader: String? = null,
    val transformation: FieldTransformation = FieldTransformation.COPY
)

@Serializable
data class StaticResponseValue(
    val targetKey: String? = null,
    val targetNestedKey: String? = null,
    val targetHeader: String? = null,
    val value: String
)

@Serializable
enum class FieldTransformation(val displayName: String) {
    COPY("Copy as-is"),
    REVERSE("Reverse value"),
    TIMESTAMP("Current timestamp"),
    SUCCESS_CODE("Success code (00)"),
    ERROR_CODE("Error code (99)"),
    ECHO_BACK("Echo request value"),
    GENERATE_REFERENCE("Generate reference number")
}

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
    gw: GatewayConfig,
    onSaveClick: () -> Unit
) {
    // Use mutableStateListOf for proper recomposition
    val transactions = remember {
        gw.simulatedTransactionsToSource.map { transaction ->
            transaction.copy(
                fields = transaction.fields?.toMutableList()
                    ?: Iso8583Data(config = gw).bitAttributes.toMutableList()
            )
        }.toMutableStateList()
    }

    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    // FIX 1: Add recomposition trigger for field changes
    var fieldChangeCounter by remember { mutableIntStateOf(0) }

    // Dialog states
    var showFieldInfoDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showEditTransactionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }


    // Save function that updates the gateway config
    val saveTransactions = {
        gw.simulatedTransactionsToSource = transactions.map { transaction ->
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
            onFieldInfoClick = { showFieldInfoDialog = true },
            onExportClick = { showExportDialog = true },
            onSaveClick = {
                onSaveClick()
            }
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
                    saveTransactions()
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
                gw = gw,
                onTransactionUpdated = { updatedTransaction ->
                    val index = transactions.indexOfFirst { it.id == updatedTransaction.id }
                    if (index != -1) {
                        transactions[index] = updatedTransaction
                        selectedTransaction = updatedTransaction
                        fieldChangeCounter++ // FIX 3: Increment counter on field changes
                    }
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
                saveTransactions()
            },
            gw = gw
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
                saveTransactions()
            },
            gw = gw
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
                saveTransactions()
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            transactions = transactions,
            onDismiss = { showExportDialog = false }
        )
    }
}


@Composable
private fun EnhancedHeader(
    onFieldInfoClick: () -> Unit,
    onExportClick: () -> Unit,
    onSaveClick: () -> Unit
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
                        onClick = onExportClick
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
        backgroundColor = if (isSelected) {
            MaterialTheme.colors.primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colors.surface
        },
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
    onTransactionUpdated: (Transaction) -> Unit,
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
                            onTransactionUpdated = onTransactionUpdated,
                            fieldChangeCounter = fieldChangeCounter // FIX 8: Pass counter down
                        )

                        1 -> ConfigurationTab(selectedTransaction)
                        2 -> ApiConfigurationTab(
                            transaction = selectedTransaction,
                            onTransactionUpdated = onTransactionUpdated
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
                                onTransactionUpdated(transaction.copy(fields = updatedFields))
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

@Composable
private fun EnhancedFieldRow(
    fieldNumber: Int,
    data: BitAttribute,
    onDataChange: (String) -> Unit,
    onRemove: () -> Unit,
    gw: GatewayConfig,
    updateKey: Int = 0 // FIX 15: Add update key parameter
) {
    // FIX 16: Use update key in the key function to force recomposition
    key(fieldNumber, data.getValue(), data.isSet, updateKey) {
        var value by remember(data.getValue(), updateKey) {
            mutableStateOf(data.getValue() ?: "")
        }
        var isExpanded by remember { mutableStateOf(false) }

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

        Card(
            elevation = 2.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column {
                // Main row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Field number badge
                    Surface(
                        color = MaterialTheme.colors.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = fieldNumber.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Field info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bitInfo?.description ?: "Field $fieldNumber",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        if (value.isNotEmpty()) {
                            Text(
                                text = if (value.length > 20) "${value.take(20)}..." else value,
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Actions
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove Field",
                            tint = MaterialTheme.colors.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Expanded content
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.surface.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Field details
                        if (bitInfo != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LabelChip("Type", bitInfo.bitType.name)
                                LabelChip("Length", bitInfo.maxLength.toString())
                                LabelChip("Format", bitInfo.bitLength.name)
                            }
                        }

                        // Value input
                        OutlinedTextField(
                            value = value,
                            onValueChange = {
                                value = it
                                onDataChange(it)
                            },
                            label = { Text("Field Value") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter field value...") },
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun LabelChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colors.secondary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colors.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurface
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

                Divider()

                // Static values
                StaticValuesSection(
                    staticValues = responseMapping.staticValues,
                    onStaticValuesChange = {
                        onResponseMappingChange(responseMapping.copy(staticValues = it))
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
            OutlinedTextField(
                value = pathMatching.path,
                onValueChange = {
                    onPathMatchingChange(pathMatching.copy(path = it))
                },
                label = { Text("API Path") },
                placeholder = { Text("/api/payment or /transactions/*") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = "Path")
                }
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
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(keyValueMatchers.size) { index ->
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
            OutlinedTextField(
                value = matcher.key,
                onValueChange = { onMatcherChange(matcher.copy(key = it)) },
                label = { Text("Key Path") },
                placeholder = { Text("e.g., transaction.type") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Operator selection
                var operatorExpanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = matcher.operator.displayName,
                        onValueChange = { },
                        label = { Text("Operator") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { operatorExpanded = true },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Operator"
                            )
                        }
                    )

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
                            sourceField = "",
                            targetKey = "",
                            transformation = FieldTransformation.COPY
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
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(responseFields.size) { index ->
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

// Source field
            OutlinedTextField(
                value = responseField.sourceField,
                onValueChange = { onResponseFieldChange(responseField.copy(sourceField = it)) },
                label = { Text("Source Field") },
                placeholder = { Text("e.g., field2, request.header.messageType") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Target key
                OutlinedTextField(
                    value = responseField.targetKey ?: "",
                    onValueChange = { onResponseFieldChange(responseField.copy(targetKey = it.takeIf { it.isNotBlank() })) },
                    label = { Text("Target Key") },
                    placeholder = { Text("e.g., responseCode") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Target nested key
                OutlinedTextField(
                    value = responseField.targetNestedKey ?: "",
                    onValueChange = { onResponseFieldChange(responseField.copy(targetNestedKey = it.takeIf { it.isNotBlank() })) },
                    label = { Text("Target Nested Key") },
                    placeholder = { Text("e.g., header.responseCode") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

// Transformation
            var transformationExpanded by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = responseField.transformation.displayName,
                    onValueChange = { },
                    label = { Text("Transformation") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { transformationExpanded = true },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Transformation"
                        )
                    }
                )

                DropdownMenu(
                    expanded = transformationExpanded,
                    onDismissRequest = { transformationExpanded = false }
                ) {
                    FieldTransformation.values().forEach { transformation ->
                        DropdownMenuItem(
                            onClick = {
                                onResponseFieldChange(responseField.copy(transformation = transformation))
                                transformationExpanded = false
                            }
                        ) {
                            Text(transformation.displayName)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaticValuesSection(
    staticValues: List<StaticResponseValue>,
    onStaticValuesChange: (List<StaticResponseValue>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Static Response Values",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    onStaticValuesChange(
                        staticValues + StaticResponseValue(
                            targetKey = "",
                            value = ""
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Static Value")
            }
        }

        if (staticValues.isEmpty()) {
            Text(
                text = "No static values configured. Add static values for fixed response fields.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 150.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(staticValues.size) { index ->
                    StaticValueRow(
                        staticValue = staticValues[index],
                        onStaticValueChange = { newValue ->
                            val updatedList = staticValues.toMutableList()
                            updatedList[index] = newValue
                            onStaticValuesChange(updatedList)
                        },
                        onRemove = {
                            onStaticValuesChange(
                                staticValues.filterIndexed { i, _ -> i != index }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StaticValueRow(
    staticValue: StaticResponseValue,
    onStaticValueChange: (StaticResponseValue) -> Unit,
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
                    text = "Static Value",
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
                // Target key
                OutlinedTextField(
                    value = staticValue.targetKey ?: "",
                    onValueChange = { onStaticValueChange(staticValue.copy(targetKey = it.takeIf { it.isNotBlank() })) },
                    label = { Text("Target Key") },
                    placeholder = { Text("e.g., status") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Target nested key
                OutlinedTextField(
                    value = staticValue.targetNestedKey ?: "",
                    onValueChange = { onStaticValueChange(staticValue.copy(targetNestedKey = it.takeIf { it.isNotBlank() })) },
                    label = { Text("Target Nested Key") },
                    placeholder = { Text("e.g., header.status") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Value
            OutlinedTextField(
                value = staticValue.value,
                onValueChange = { onStaticValueChange(staticValue.copy(value = it)) },
                label = { Text("Value") },
                placeholder = { Text("Static value to set") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
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

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 Pro Tip",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "Use the search bar to quickly find transactions, or create a new one using the + button",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
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

@OptIn(ExperimentalMaterialApi::class)
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

    val categories = listOf(
        "All", "Common", "PAN & Security", "Amount & Date", "Terminal & Merchant", "Network"
    )

    val categorizedBits = remember(gw.bitTemplateSource, selectedCategory, existingFields) {
        val commonBits = setOf(2, 3, 4, 11, 12, 13, 22, 37, 41, 42)
        val panSecurityBits = setOf(2, 14, 22, 23, 35, 45, 52, 53, 55)
        val amountDateBits = setOf(4, 5, 6, 9, 10, 12, 13, 16, 50, 51)
        val terminalBits = setOf(18, 19, 25, 32, 33, 41, 42, 43, 49)
        val networkBits = setOf(1, 7, 8, 11, 15, 24, 38, 39, 70, 95)

        gw.bitTemplateSource.filter { bit ->
            val bitNum = bit.bitNumber.toInt()
            // Filter out existing fields
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
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colors.surface
        ) {
            Column {
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
                        Column {
                            Text(
                                text = "Add Fields",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (selectedBitSpecifics.isNotEmpty()) {
                                Text(
                                    text = "${selectedBitSpecifics.size} field(s) selected",
                                    style = MaterialTheme.typography.caption,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Search and Filter
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search fields...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    // Category filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            ) { Text(category, fontSize = 12.sp) }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${categorizedBits.size} fields available",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        if (existingFields.isNotEmpty()) {
                            Text(
                                text = "${existingFields.size} already added",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.secondary
                            )
                        }
                    }
                }

                // Fields list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categorizedBits) { bit ->
                        val isSelected = selectedBitSpecifics.contains(bit)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBitSpecifics = if (isSelected) {
                                        selectedBitSpecifics - bit
                                    } else {
                                        selectedBitSpecifics + bit
                                    }
                                },
                            backgroundColor = if (isSelected) {
                                MaterialTheme.colors.primary.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colors.surface
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Selection indicator
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Field info
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colors.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = bit.bitNumber.toString(),
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                ),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = bit.description ?: "Field ${bit.bitNumber}",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LabelChip("Type", bit.bitType.name)
                                        LabelChip("Length", bit.maxLength.toString())
                                    }
                                }
                            }
                        }
                    }
                }

                // Action buttons
                Surface(
                    color = MaterialTheme.colors.surface.copy(alpha = 0.5f)
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
                                if (selectedBitSpecifics.isNotEmpty()) {
                                    onSave(selectedBitSpecifics.toList())
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedBitSpecifics.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add ${selectedBitSpecifics.size} Field(s)")
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ExportDialog(
    transactions: List<Transaction>,
    onDismiss: () -> Unit
) {
    var exportFormat by remember { mutableStateOf("") }
    var exportType by remember { mutableStateOf("") }
    var includeFields by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        title = { Text("Export Transactions") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Export format selection
                Column {
                    Text(
                        text = "Export Format",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("JSON", "CSV", "XML").forEach { format ->
                            FilterChip(
                                selected = exportFormat == format,
                                onClick = { exportFormat = format },
                            ) { Text(format) }
                        }
                    }
                }

                // Export type selection
                Column {
                    Text(
                        text = "Export Type",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Configuration Only", "Fields Only").forEach { type ->
                            FilterChip(
                                selected = exportType == type,
                                onClick = { exportType = type },
                            ) { Text(type, fontSize = 10.sp) }
                        }
                    }
                }

                // Options
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeFields,
                        onCheckedChange = { includeFields = it }
                    )
                    Text(
                        text = "Include field values",
                        style = MaterialTheme.typography.body2
                    )
                }

                // Preview
                Text(
                    text = "Ready to export ${transactions.size} transaction(s) in $exportFormat format",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Export logic here
                    performExport(transactions, exportFormat, exportType, includeFields)
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.GetApp, contentDescription = "Export")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun performExport(
    transactions: List<Transaction>,
    format: String,
    type: String,
    includeFields: Boolean
) {
    // Export implementation would go here
    // This could save to file system or copy to clipboard
    when (format) {
        "JSON" -> {
            val json = Json { prettyPrint = true }
            val exportData = when (type) {
                "Configuration Only" -> transactions.map { it.copy(fields = null) }
                "Fields Only" -> transactions.map {
                    Transaction(
                        id = it.id,
                        mti = it.mti,
                        proCode = it.proCode,
                        description = it.description,
                        fields = if (includeFields) it.fields else null
                    )
                }

                else -> transactions
            }
            val jsonString = json.encodeToString(exportData)
            // Save or copy jsonString
            println("Exported JSON: $jsonString")
        }

        "CSV" -> {
            // CSV export implementation
            println("Exported CSV")
        }

        "XML" -> {
            // XML export implementation
            println("Exported XML")
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