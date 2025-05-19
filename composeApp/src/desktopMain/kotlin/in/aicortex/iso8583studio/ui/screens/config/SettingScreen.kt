package `in`.aicortex.iso8583studio.ui.screens.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
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
import `in`.aicortex.iso8583studio.data.updateBit
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.Random
import kotlin.math.absoluteValue

// Data classes to represent the ISO8583 message structure
@Serializable
data class Transaction(
    val id: String,
    val mti: String,
    val proCode: String,
    val description: String,
    @Transient val fields: Iso8583Data? = null
)

@Composable
fun ISO8583SettingsScreen( gw: GatewayServiceImpl) {
    val transactions = remember {
        (gw.configuration.simulatedTransactions ).toMutableStateList()
    }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    // State for edited fields
    val fieldEditStates = remember { mutableStateMapOf<String, BitAttribute?>() }
    val fieldAvailableStates = remember { mutableStateMapOf<String, Boolean>() }
    val descriptionEditStates = remember { mutableStateMapOf<String, String>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Transaction simulation Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel - Transaction list
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                elevation = 4.dp
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
                            modifier = Modifier.weight(0.2f)
                        )
                        Text(
                            text = "ProCode",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.3f)
                        )
                        Text(
                            text = "Description",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.5f)
                        )
                    }
                    var showAddTransactionDialog by remember { mutableStateOf(false) }
                    // Transaction list
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(transactions.size) { i ->
                            TransactionRow(
                                transaction = transactions[i],
                                isSelected = selectedTransaction?.id == transactions[i].id,
                                onClick = { selectedTransaction = it }
                            )
                        }
                    }

                    // Add button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showAddTransactionDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Add")
                        }
                    }

                    if (showAddTransactionDialog) {

                        AddTransactionDialog(
                            onDismiss = {
                                showAddTransactionDialog = false
                            },
                            onSave = {
                                transactions.apply {
                                    add(it)
                                }
                                showAddTransactionDialog = false
                            },
                            gw = gw
                        )
                    }
                }
            }

            // Right panel - Field details
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                elevation = 4.dp
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
                            modifier = Modifier.weight(0.15f)
                        )
                        Text(
                            text = "Data",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.35f)
                        )
                        Text(
                            text = "Description",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.5f)
                        )
                    }

                    // Fields list
                    if (selectedTransaction != null) {
                        if (!selectedTransaction!!.fields!!.bitAttributes.none { it.isSet == true }) {
                            LazyColumn(
                                state = rememberLazyListState(),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(selectedTransaction!!.fields!!.bitAttributes.size) { index ->
                                    val field = selectedTransaction?.fields?.bitAttributes[index]
                                    val fieldKey = "$${index + 1}"

                                    // State for edited fields
                                    fieldAvailableStates[fieldKey] = field?.isSet == true
                                    fieldEditStates[fieldKey] = field
                                    if (fieldAvailableStates[fieldKey] == true) {
                                        FieldRow(
                                            fieldNumber = index + 1,
                                            data = fieldEditStates[fieldKey],
                                            onDataChange = { newValue ->
                                                selectedTransaction?.fields?.bitAttributes[index] =
                                                    newValue!!
                                                fieldEditStates[fieldKey] = newValue
                                            },
                                            onDescriptionChange = { newValue ->
                                                descriptionEditStates[fieldKey] = newValue
                                            },
                                            onRemove = {
                                                selectedTransaction?.fields?.bitAttributes[index]?.isSet =
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
                                Text("No Attributes available")
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
                                   gw.configuration.simulatedTransactions = transactions
                                }
                            ) {
                                Text("Save")
                            }
                        }
                        if(showAddBitSpecificDialog){
                            AddBitSpecificDialog(
                                gw = gw,
                                onSave = { bit ->
                                    selectedTransaction!!.fields!!
                                        .packBit(bit.bitNumber.toInt().absoluteValue,"")
                                    fieldAvailableStates["${bit.bitNumber.toInt().absoluteValue + 1}"]  = true
                                    showAddBitSpecificDialog = false
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
                            Text("Select a transaction to view fields")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBitSpecificDialog(
    onSave: (BitSpecific) -> Unit,
    onDismiss: () -> Unit,
    gw: GatewayServiceImpl
) {
    var bitSpecific by remember { mutableStateOf<BitSpecific?>(null) }
    AlertDialog(
        containerColor = MaterialTheme.colors.surface,
        textContentColor = MaterialTheme.colors.onSurface,
        titleContentColor = MaterialTheme.colors.onSurface,
        onDismissRequest = onDismiss,
        title = {
            Text("Add Field")
        },
        text = {
            LazyColumn {
                items(gw.configuration.bitTemplate.size) { index ->
                    val bit = gw.configuration.bitTemplate[index]
                    Column(
                        modifier = Modifier
                            .background(
                                color = if(bitSpecific?.bitNumber == bit.bitNumber){
                                    MaterialTheme.colors.primary
                                }else{
                                    MaterialTheme.colors.surface
                                }
                            )
                    ) {
                        BitPropertyRow(
                            bit = bit
                        ) {
                            bitSpecific = bit
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    bitSpecific?.let { onSave(it) }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}


@Composable
private fun AddTransactionDialog(
    onSave: (Transaction) -> Unit,
    onDismiss: () -> Unit,
    gw: GatewayServiceImpl
) {
    var mtiValue by remember { mutableStateOf("") }
    var proCode by remember { mutableStateOf("") }
    AlertDialog(
        containerColor = MaterialTheme.colors.surface,
        textContentColor = MaterialTheme.colors.onSurface,
        titleContentColor = MaterialTheme.colors.onSurface,
        onDismissRequest = onDismiss,
        title = {
            Text("Add Field")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // MTI
                    Text(
                        text = "MTI",
                        modifier = Modifier.weight(0.15f)
                    )

                    // Data value
                    TextField(
                        value = mtiValue,
                        onValueChange = {
                            mtiValue = it
                        },
                        modifier = Modifier
                            .weight(0.35f)
                            .padding(horizontal = 4.dp),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Processing Code
                    Text(
                        text = "Processing Code",
                        modifier = Modifier.weight(0.15f)
                    )

                    // Data value
                    TextField(
                        value = proCode,
                        onValueChange = {
                            proCode = it
                        },
                        modifier = Modifier
                            .weight(0.35f)
                            .padding(horizontal = 4.dp),
                        singleLine = true
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
                            description = "",
                            fields = Iso8583Data(config = gw.configuration)
                        )
                    )
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}


@Composable
private fun TransactionRow(
    transaction: Transaction,
    isSelected: Boolean,
    onClick: (Transaction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onClick(transaction) })
            .background(if (isSelected) MaterialTheme.colors.primary else Color.Transparent)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = transaction.mti,
            modifier = Modifier.weight(0.2f)
        )
        Text(
            text = transaction.proCode,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = transaction.description,
            modifier = Modifier.weight(0.5f)
        )
    }
}

@Composable
private fun FieldRow(
    fieldNumber: Int,
    data: BitAttribute?,
    onDataChange: (BitAttribute?) -> Unit,
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
            text = fieldNumber.toString(),
            modifier = Modifier.weight(0.15f)
        )

        // Data value
        TextField(
            value = value,
            onValueChange = {
                value = it
                data?.updateBit(fieldNumber, it)
                onDataChange(data)
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
            Text("X")  // Using Text as a simple remove button, could use an icon
        }
    }
}