package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.BitSpecific
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.getValue
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.updateBit
import `in`.aicortex.iso8583studio.ui.screens.components.FieldInformationDialog
import kotlinx.serialization.Serializable
import java.util.Random
import kotlin.math.absoluteValue

// Data classes to represent the ISO8583 message structure
@Serializable
data class Transaction(
    val id: String,
    val mti: String,
    val proCode: String,
    val description: String,
    val fields: List<BitAttribute>? = null
)

@Composable
fun ISO8583SettingsScreen(
    gw: GatewayConfig,
    onSaveClick: () -> Unit
) {
    val transactions = remember {
        gw.simulatedTransactionsToSource.toMutableStateList()
    }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    // State for edited fields
    val fieldEditStates = remember { mutableStateMapOf<String, BitAttribute?>() }
    val fieldAvailableStates = remember { mutableStateMapOf<String, Boolean>() }
    val descriptionEditStates = remember { mutableStateMapOf<String, String>() }

    // Dialog states
    var showFieldInfoDialog by remember { mutableStateOf(false) }
//    var showCodeEditorDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showEditTransactionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Transaction simulation Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Code Editor Button
//                IconButton(
//                    onClick = { showCodeEditorDialog = true },
//                    enabled = selectedTransaction != null
//                ) {
//                    Icon(
//                        Icons.Default.Code,
//                        contentDescription = "Code Editor",
//                        tint = if (selectedTransaction != null)
//                            MaterialTheme.colors.primary
//                        else
//                            MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
//                    )
//                }

                // Field Information Button
                IconButton(onClick = { showFieldInfoDialog = true }) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Field Information",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel - Transaction list with enhanced functionality
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.primary)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "MTI",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(0.15f)
                        )
                        Text(
                            text = "ProCode",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(0.2f)
                        )
                        Text(
                            text = "Description",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(0.4f)
                        )
                        Text(
                            text = "Actions",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(0.25f)
                        )
                    }

                    // Transaction list
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(transactions.size) { i ->
                            EnhancedTransactionRow(
                                transaction = transactions[i],
                                isSelected = selectedTransaction?.id == transactions[i].id,
                                onClick = { selectedTransaction = it },
                                onEdit = { transaction ->
                                    transactionToEdit = transaction
                                    showEditTransactionDialog = true
                                },
                                onDelete = { transaction ->
                                    transactionToDelete = transaction
                                    showDeleteConfirmDialog = true
                                }
                            )
                            if (i < transactions.size - 1) {
                                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                            }
                        }
                    }

                    // Add button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { showAddTransactionDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add New Transaction")
                        }
                    }
                }
            }

            // Right panel - Field details
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.primary)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Field",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(0.15f)
                        )
                        Text(
                            text = "Data",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(0.35f)
                        )
                        Text(
                            text = "Description",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(0.5f)
                        )
                    }

                    // Fields list
                    if (selectedTransaction != null) {
                        if (!selectedTransaction!!.fields!!.none { it.isSet }) {
                            LazyColumn(
                                state = rememberLazyListState(),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(selectedTransaction!!.fields!!.size) { index ->
                                    val field = selectedTransaction?.fields?.get(index)
                                    val fieldKey = "${index}"

                                    // State for edited fields
                                    fieldAvailableStates[fieldKey] = field?.isSet == true
                                    fieldEditStates[fieldKey] = field
                                    if (fieldAvailableStates[fieldKey] == true) {
                                        FieldRow(
                                            fieldNumber = index ,
                                            data = fieldEditStates[fieldKey],
                                            onDataChange = { newValue ->
                                                selectedTransaction?.fields?.get(index)?.updateBit(newValue)
                                                fieldEditStates[fieldKey] = selectedTransaction?.fields?.get(index)
                                            },
                                            onDescriptionChange = { newValue ->
                                                descriptionEditStates[fieldKey] = newValue
                                            },
                                            onRemove = {
                                                selectedTransaction?.fields?.get(index)?.isSet =
                                                    false
                                                fieldAvailableStates[fieldKey] = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No Fields Available")
                                    Text(
                                        "Add fields to this transaction to get started",
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        var showAddBitSpecificDialog by remember { mutableStateOf(false) }
                        // Field actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { showAddBitSpecificDialog = true }
                            ) {
                                Text("Add Field")
                            }

                            Button(
                                onClick = {
                                    gw.simulatedTransactionsToSource = transactions
                                    onSaveClick()
                                }
                            ) {
                                Text("Save")
                            }
                        }
                        if (showAddBitSpecificDialog) {
                            AddBitSpecificDialog(
                                gw = gw,
                                onSave = { bitNumbersAdded ->
                                    bitNumbersAdded.forEach { bit ->
                                        selectedTransaction!!.fields!![bit.bitNumber.toInt().absoluteValue-1].updateBit( "")
                                        fieldAvailableStates["${bit.bitNumber.toInt().absoluteValue-1}"] =
                                            true
                                        showAddBitSpecificDialog = false
                                    }

                                },
                                onDismiss = {
                                    showAddBitSpecificDialog = false
                                }
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                                )
                                Text(
                                    "Select a transaction to view fields",
                                    style = MaterialTheme.typography.h6,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    "Choose a transaction from the left panel or create a new one",
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showFieldInfoDialog) {
        FieldInformationDialog(
            onCloseRequest = { showFieldInfoDialog = false }
        )
    }

//    if (showCodeEditorDialog && selectedTransaction != null) {
//        CodeEditorDialog(
//            messageType = selectedTransaction!!.mti,
//            tpdu = if (gw.configuration.doNotUseHeader) "" else IsoUtil.bcdToString(gw.configuration.fixedResponseHeader!!),
//            fields = selectedTransaction!!.fields ?: emptyList(),
//            onCloseRequest = { showCodeEditorDialog = false },
//            onApplyChanges = { fieldMappings ->
//                // Apply field mappings back to the transaction
//                selectedTransaction?.let { transaction ->
//                    fieldMappings.forEach { mapping ->
//                        val bitIndex = mapping.bitNumber - 1
//                        if (bitIndex >= 0 && bitIndex < transaction.fields!!.size) {
//                            transaction.fields!![bitIndex].updateBit(mapping.bitNumber, mapping.value)
//                            fieldAvailableStates["${mapping.bitNumber}"] = true
//                            fieldEditStates["${mapping.bitNumber}"] = transaction.fields!![bitIndex]
//                        }
//                    }
//                }
//                showCodeEditorDialog = false
//            }
//        )
//    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            onDismiss = { showAddTransactionDialog = false },
            onSave = {
                transactions.add(it)
                showAddTransactionDialog = false
                onSaveClick()
            },
            gw = gw
        )
    }

    if (showEditTransactionDialog && transactionToEdit != null) {
        EditTransactionDialog(
            transaction = transactionToEdit!!,
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
                    }
                }
                showEditTransactionDialog = false
                transactionToEdit = null
                onSaveClick()
            }
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
                }
                showDeleteConfirmDialog = false
                transactionToDelete = null
                onSaveClick()
            }
        )
    }
}

// Keep all the existing components (EnhancedTransactionRow, EditTransactionDialog, etc.)
@Composable
private fun EnhancedTransactionRow(
    transaction: Transaction,
    isSelected: Boolean,
    onClick: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onClick(transaction) })
            .background(
                if (isSelected)
                    MaterialTheme.colors.primary.copy(alpha = 0.1f)
                else
                    Color.Transparent
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = transaction.mti,
            modifier = Modifier.weight(0.15f),
            fontSize = 14.sp
        )
        Text(
            text = transaction.proCode,
            modifier = Modifier.weight(0.2f),
            fontSize = 14.sp
        )
        Text(
            text = transaction.description.ifEmpty { "No description" },
            modifier = Modifier.weight(0.4f),
            fontSize = 14.sp,
            color = if (transaction.description.isEmpty())
                MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            else
                MaterialTheme.colors.onSurface
        )

        // Action buttons
        Row(
            modifier = Modifier.weight(0.25f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onEdit(transaction) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Transaction",
                    tint = MaterialTheme.colors.primary
                )
            }

            IconButton(
                onClick = { onDelete(transaction) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Transaction",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}

// Keep all other existing components unchanged...
@Composable
private fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var mtiValue by remember { mutableStateOf(transaction.mti) }
    var proCode by remember { mutableStateOf(transaction.proCode) }
    var description by remember { mutableStateOf(transaction.description) }

    AlertDialog(
        containerColor = MaterialTheme.colors.surface,
        textContentColor = MaterialTheme.colors.onSurface,
        titleContentColor = MaterialTheme.colors.onSurface,
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Message Type Indicator (MTI)",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )
                    TextField(
                        value = mtiValue,
                        onValueChange = { mtiValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g., 0200") }
                    )
                }

                Column {
                    Text(
                        text = "Processing Code",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )
                    TextField(
                        value = proCode,
                        onValueChange = { proCode = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g., 000000") }
                    )
                }

                Column {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Transaction description") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        transaction.copy(
                            mti = mtiValue,
                            proCode = proCode,
                            description = description
                        )
                    )
                },
                enabled = mtiValue.isNotBlank() && proCode.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        containerColor = MaterialTheme.colors.surface,
        textContentColor = MaterialTheme.colors.onSurface,
        titleContentColor = MaterialTheme.colors.onSurface,
        onDismissRequest = onDismiss,
        title = { Text("Delete Transaction") },
        text = {
            Column {
                Text("Are you sure you want to delete this transaction?")
                Text(
                    text = "\nMTI: ${transaction.mti}\nProcessing Code: ${transaction.proCode}",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.error
                )
            ) {
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

@Composable
private fun AddBitSpecificDialog(
    onSave: (List<BitSpecific>) -> Unit, // Changed to accept a list
    onDismiss: () -> Unit,
    gw: GatewayConfig
) {
    // Changed to support multiple selections
    var selectedBitSpecifics by remember { mutableStateOf(setOf<BitSpecific>()) }
    var multiSelectMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter bits based on search query
    val filteredBits = remember(searchQuery, gw.bitTemplateSource) {
        if (searchQuery.isBlank()) {
            gw.bitTemplateSource
        } else {
            gw.bitTemplateSource.filter { bit ->
                // Search in bit number, description, and data type
                bit.bitNumber.toString().contains(searchQuery, ignoreCase = true) ||
                        bit.description.contains(searchQuery, ignoreCase = true) == true ||
                        bit.bitType.name.contains(searchQuery, ignoreCase = true) == true
            }.toTypedArray()
        }
    }

    AlertDialog(
        containerColor = MaterialTheme.colors.surface,
        textContentColor = MaterialTheme.colors.onSurface,
        titleContentColor = MaterialTheme.colors.onSurface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Add Field${if (selectedBitSpecifics.size > 1) "s" else ""}")
                if (selectedBitSpecifics.isNotEmpty()) {
                    Text(
                        text = "${selectedBitSpecifics.size} field${if (selectedBitSpecifics.size > 1) "s" else ""} selected",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary
                    )
                }

                // Multi-select toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Multi-select mode",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    androidx.compose.material.Switch(
                        checked = multiSelectMode,
                        onCheckedChange = {
                            multiSelectMode = it
                            if (!it) {
                                // When turning off multi-select, keep only the first selected item
                                selectedBitSpecifics = selectedBitSpecifics.take(1).toSet()
                            }
                        },
                        colors = androidx.compose.material.SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colors.primary
                        )
                    )
                }
            }
        },
        text = {
            Column {
                // Search bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("Search by bit number, description, or type...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = androidx.compose.material.TextFieldDefaults.textFieldColors(
                        backgroundColor = MaterialTheme.colors.surface,
                        focusedIndicatorColor = MaterialTheme.colors.primary
                    )
                )

                // Results count
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "${filteredBits.size} of ${gw.bitTemplateSource.size} fields found",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Fields list
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredBits.size) { index ->
                        val bit = filteredBits[index]
                        val isSelected = selectedBitSpecifics.contains(bit)

                        BitPropertyRow(
                            bit = bit,
                            isSelected = isSelected,
                            multiSelectMode = multiSelectMode,
                            searchQuery = searchQuery, // Pass search query for highlighting
                            onClick = {
                                selectedBitSpecifics = if (multiSelectMode) {
                                    // Multi-select mode: toggle selection
                                    if (isSelected) {
                                        selectedBitSpecifics - bit
                                    } else {
                                        selectedBitSpecifics + bit
                                    }
                                } else {
                                    // Single select mode: replace selection
                                    if (isSelected) {
                                        emptySet() // Deselect if already selected
                                    } else {
                                        setOf(bit) // Select only this item
                                    }
                                }
                            }
                        )
                    }

                    // Show message when no results found
                    if (filteredBits.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                    )
                                    Text(
                                        text = "No fields found",
                                        style = MaterialTheme.typography.h6,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "Try adjusting your search terms",
                                        style = MaterialTheme.typography.body2,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedBitSpecifics.isNotEmpty()) {
                        onSave(selectedBitSpecifics.toList())
                    }
                },
                enabled = selectedBitSpecifics.isNotEmpty()
            ) {
                Text("Add ${if (selectedBitSpecifics.size > 1) "${selectedBitSpecifics.size} Fields" else "Field"}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BitPropertyRow(
    bit: BitSpecific,
    isSelected: Boolean = false,
    multiSelectMode: Boolean = false,
    searchQuery: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (isSelected) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                    },
                    shape = if (multiSelectMode) RoundedCornerShape(4.dp) else androidx.compose.foundation.shape.CircleShape
                )
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Highlighted bit number
            HighlightedText(
                text = "Bit ${bit.bitNumber}",
                searchQuery = searchQuery,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )

            // Highlighted description
            HighlightedText(
                text = bit.description ?: "No description",
                searchQuery = searchQuery,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Highlighted type and length info
            HighlightedText(
                text = "Type: ${bit.bitType.name}, Length: ${bit.maxLength}",
                searchQuery = searchQuery,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }

        // Visual indicator for multi-select mode
        if (multiSelectMode) {
            Icon(
                imageVector = Icons.Default.CheckBox,
                contentDescription = "Multi-select enabled",
                tint = MaterialTheme.colors.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    searchQuery: String,
    color: Color = MaterialTheme.colors.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.body2,
    fontWeight: FontWeight? = null
) {
    if (searchQuery.isBlank()) {
        Text(
            text = text,
            color = color,
            style = style,
            fontWeight = fontWeight
        )
    } else {
        val startIndex = text.indexOf(searchQuery, ignoreCase = true)
        if (startIndex >= 0) {
            val endIndex = startIndex + searchQuery.length
            val beforeMatch = text.substring(0, startIndex)
            val match = text.substring(startIndex, endIndex)
            val afterMatch = text.substring(endIndex)

            Row {
                Text(
                    text = beforeMatch,
                    color = color,
                    style = style,
                    fontWeight = fontWeight
                )
                Text(
                    text = match,
                    color = MaterialTheme.colors.primary,
                    style = style,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(
                        MaterialTheme.colors.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(2.dp)
                    ).padding(horizontal = 2.dp)
                )
                Text(
                    text = afterMatch,
                    color = color,
                    style = style,
                    fontWeight = fontWeight
                )
            }
        } else {
            Text(
                text = text,
                color = color,
                style = style,
                fontWeight = fontWeight
            )
        }
    }
}

@Composable
private fun AddTransactionDialog(
    onSave: (Transaction) -> Unit,
    onDismiss: () -> Unit,
    gw: GatewayConfig
) {
    var mtiValue by remember { mutableStateOf("") }
    var proCode by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        containerColor = MaterialTheme.colors.surface,
        textContentColor = MaterialTheme.colors.onSurface,
        titleContentColor = MaterialTheme.colors.onSurface,
        onDismissRequest = onDismiss,
        title = { Text("Add New Transaction") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Message Type Indicator (MTI)",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )
                    TextField(
                        value = mtiValue,
                        onValueChange = { mtiValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g., 0200") }
                    )
                }

                Column {
                    Text(
                        text = "Processing Code",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )
                    TextField(
                        value = proCode,
                        onValueChange = { proCode = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g., 000000") }
                    )
                }

                Column {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Transaction description") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Transaction(
                            id = Random().nextInt().toString(),
                            mti = mtiValue,
                            proCode = proCode,
                            description = description,
                            fields = Iso8583Data(config = gw).bitAttributes.toMutableList(),
                        )
                    )
                },
                enabled = mtiValue.isNotBlank() && proCode.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FieldRow(
    fieldNumber: Int,
    data: BitAttribute?,
    onDataChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRemove: (Int) -> Unit
) {
    var value by remember { mutableStateOf(data?.getValue() ?: "") }
    var desc by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Field number
        Text(
            text = (fieldNumber+1).toString(),
            modifier = Modifier.weight(0.15f)
        )

        // Data value
        TextField(
            value = value,
            onValueChange = {
                value = it

                onDataChange(it)
            },
            modifier = Modifier
                .weight(0.35f)
                .padding(horizontal = 4.dp),
            singleLine = true
        )

        // Description
        TextField(
            value = desc,
            onValueChange = onDescriptionChange,
            modifier = Modifier
                .weight(0.5f)
                .padding(horizontal = 4.dp),
            singleLine = true
        )
        IconButton(
            onClick = {
                onRemove(fieldNumber)
            },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("X")
        }
    }
}