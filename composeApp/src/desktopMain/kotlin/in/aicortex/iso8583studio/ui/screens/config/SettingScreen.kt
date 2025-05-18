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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.BitSpecific
import `in`.aicortex.iso8583studio.data.BitTemplate
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil

// Data classes to represent the ISO8583 message structure
data class Transaction(
    val id: String,
    val mti: String,
    val proCode: String,
    val description: String,
    val fields: Array<BitAttribute>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Transaction

        if (id != other.id) return false
        if (mti != other.mti) return false
        if (proCode != other.proCode) return false
        if (description != other.description) return false
        if (!fields.contentEquals(other.fields)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + mti.hashCode()
        result = 31 * result + proCode.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + fields.contentHashCode()
        return result
    }
}


@Composable
fun ISO8583SettingsScreen(transactions: List<Transaction> = listOf(
    Transaction(
        id = "1",
        mti = "234",
        proCode = "0000",
        description = "",
        fields = BitTemplate.getGeneralTemplate()
    ),
    Transaction(
        id = "2",
        mti = "234",
        proCode = "0000",
        description = "",
        fields = BitTemplate.getGeneralTemplate()
    ),
    Transaction(
        id = "3",
        mti = "234",
        proCode = "0000",
        description = "",
        fields = BitTemplate.getGeneralTemplate()
    ),
    Transaction(
        id = "4",
        mti = "234",
        proCode = "0000",
        description = "",
        fields = BitTemplate.getGeneralTemplate()
    ),
    Transaction(
        id = "5",
        mti = "234",
        proCode = "0000",
        description = "",
        fields = BitTemplate.getGeneralTemplate()
    )
)) {
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    // State for edited fields
    val fieldEditStates = remember { mutableStateMapOf<String, String>() }
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
                            onClick = { /* TODO: Show add transaction dialog */ },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Add")
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
                        LazyColumn(
                            state = rememberLazyListState(),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(selectedTransaction?.fields?.size ?: 0) { index ->
                                val field = selectedTransaction?.fields[index]
                                if(field?.isSet == true){
                                    val fieldKey = "$${index+1}"

                                    FieldRow(
                                        fieldNumber = index+1,
                                        data = field.data,
                                        onDataChange = { newValue ->
                                            fieldEditStates[fieldKey] = newValue
                                        },
                                        onDescriptionChange = { newValue ->
                                            descriptionEditStates[fieldKey] = newValue
                                        }
                                    )
                                }

                            }
                        }

                        // Field actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { /* TODO: Show add field dialog */ }
                            ) {
                                Text("Add Field")
                            }

                            Button(
                                onClick = {
                                    // Save all edited fields
                                    fieldEditStates.forEach { (key, value) ->
                                        val parts = key.split("-")
                                        if (parts.size == 2) {
                                            val transactionId = parts[0]
                                            val fieldNumber = parts[1].toIntOrNull() ?: return@forEach
                                            val description = descriptionEditStates[key]

                                        }
                                    }

                                }
                            ) {
                                Text("Save")
                            }
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
private fun TransactionRow(
    transaction: Transaction,
    isSelected: Boolean,
    onClick: (Transaction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {onClick(transaction)})
            .background(if (isSelected) Color.LightGray.copy(alpha = 0.3f) else Color.Transparent)
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
    data: ByteArray?,
    onDataChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    var value by remember { mutableStateOf(IsoUtil.bcdToString(data?: byteArrayOf())) }
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
            onValueChange = onDataChange,
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
    }
}