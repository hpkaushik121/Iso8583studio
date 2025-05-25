package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl

/**
 * Main composable for ISO8583 message editor dialog
 */
@Composable
fun Iso8583EditorDialog(
    gw: GatewayServiceImpl,
    initialMessage: Iso8583Data? = null,
    onDismiss: () -> Unit,
    onConfirm: (Iso8583Data) -> Unit
) {
    // Create state for the ISO8583 message
    val message = remember {
        mutableStateOf(initialMessage ?: Iso8583Data(gw.configuration).apply {
            messageType = "0200"
            tpduHeader.rawTPDU = IsoUtil.stringToBcd("6000000000", 5)
            bitAttributes.forEach { it.isSet = false }
            packBit(3, "000000")
            packBit(4, "000009999000")
            packBit(11, "000077")
            packBit(12, "111412")
            packBit(13, "0204")
            packBit(22, "0022")
            packBit(25, "00")
            packBit(35, "4541822000289640D100220120389421000000")
            packBit(41, "11111111")
            packBit(42, "111111112345678")
            packBit(60, "000003")
            packBit(62, "000023")
        })
    }

    // Creating a mutable state for bitAttributes so changes trigger recomposition
    val bitAttributes = remember { mutableStateOf(message.value.bitAttributes.clone()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header section with TPDU and MTI
                Iso8583Header(
                    gw = gw,
                    message = message.value,
                    onMessageTypeChanged = {
                        message.value.apply {
                            messageType = it
                        }
                    },
                    onTpduChanged = {
                        message.value.apply {
                            tpduHeader.rawTPDU = IsoUtil.stringToBcd(it, it.length/2)
                        }
                    },
                    onUseSmartlinkChanged = { useSmartlink ->
                        // Handle Smartlink template change if needed
                    }
                )

                // Fields section - FIXED VERSION
                Iso8583FieldsEditor(
                    message = message.value,
                    bitAttributes = bitAttributes,
                    onFieldChanged = { index, newValue ->
                        // Pack the new value into the bit
                        message.value.packBit(index + 1, newValue)

                        // Update the state to trigger recomposition
                        val newBitAttributes = bitAttributes.value.clone()
                        bitAttributes.value = newBitAttributes
                    },
                    onFieldAdded = { bitNumber, value ->
                        // Pack the new bit
                        message.value.packBit(bitNumber, value)

                        // Update the state to trigger recomposition
                        bitAttributes.value = message.value.bitAttributes.clone()
                    },
                    onFieldRemoved = { index ->
                        // Unset the bit
                        val bitNumber = index + 1
                        message.value.bitAttributes[index].isSet = false

                        // Update the state to trigger recomposition
                        bitAttributes.value = message.value.bitAttributes.clone()
                    },
                    modifier = Modifier.weight(1f)
                )

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                ) {
                    Button(
                        onClick = {
                            // Make sure message has the latest bitAttributes before confirming
                            message.value.bitAttributes = bitAttributes.value
                            onConfirm(message.value)
                        }
                    ) {
                        Text("OK")
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.surface,
                            contentColor = MaterialTheme.colors.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

/**
 * Header section of the dialog with TPDU, MTI, and template selection
 */
@Composable
private fun Iso8583Header(
    gw: GatewayServiceImpl,
    message: Iso8583Data,
    onMessageTypeChanged: (String) -> Unit,
    onTpduChanged: (String) -> Unit,
    onUseSmartlinkChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if(!gw.configuration.doNotUseHeaderSource){
            // TPDU Field
            Text("TPDU")
            Spacer(modifier = Modifier.width(8.dp))
            var tpdu by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                tpdu = IsoUtil.bcdToString(message.tpduHeader.rawTPDU)
            }
            TextField(
                value = tpdu,
                onValueChange = {
                    if (it.length <= 10){
                        tpdu = it
                        onTpduChanged(it)
                    }
                },
                modifier = Modifier.width(120.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.width(16.dp))
        }

        var messageType by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            messageType = message.messageType.toString()
        }
        // Message Type Field
        Text("Message Type")
        Spacer(modifier = Modifier.width(8.dp))
        TextField(
            value = messageType,
            onValueChange = {
                if (it.length <= 4){
                    messageType = it
                    onMessageTypeChanged(it)
                }
            },
            modifier = Modifier.width(70.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Smartlink Template Checkbox can be added here if needed
    }
}

/**
 * Fields editor section with table of field numbers and values - FIXED VERSION
 */
@Composable
private fun Iso8583FieldsEditor(
    message: Iso8583Data,
    bitAttributes: MutableState<Array<BitAttribute>>,
    onFieldChanged: (Int, String) -> Unit,
    onFieldAdded: (Int, String) -> Unit,
    onFieldRemoved: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newFieldNumber by remember { mutableStateOf("") }
    var newFieldValue by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        // Table header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Field Number",
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.width(100.dp)
            )
            Text(
                text = "Value",
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.weight(1f)
            )
            // Space for actions
            Spacer(modifier = Modifier.width(80.dp))
        }

        Divider()

        // Field list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(bitAttributes.value.size) { index ->
                if (bitAttributes.value[index].isSet) {
                    var fieldVAttribute by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        fieldVAttribute =  message.getValue(index ) ?: ""
                    }
                    Iso8583FieldRow(
                        bitNumber = index + 1, // Adding 1 because bitNumbers are 1-based
                        value = fieldVAttribute,
                        onValueChanged = { newValue ->
                            fieldVAttribute = newValue
                            onFieldChanged(index, newValue)
                        },
                        onRemove = { onFieldRemoved(index) }
                    )
                    Divider()
                }
            }
        }

        // Add field button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 8.dp)
        ) {
            Text("Add Field")
        }
    }

    // Add field dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Field") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = newFieldNumber,
                        onValueChange = { newFieldNumber = it },
                        label = { Text("Field Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = newFieldValue,
                        onValueChange = { newFieldValue = it },
                        label = { Text("Value") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fieldNumber = newFieldNumber.toIntOrNull()
                        if (fieldNumber != null && fieldNumber > 0 && fieldNumber <= 128) {
                            onFieldAdded(fieldNumber, newFieldValue)
                            newFieldNumber = ""
                            newFieldValue = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Individual field row with number, value, and remove button - FIXED VERSION
 */
@Composable
private fun Iso8583FieldRow(
    bitNumber: Int,
    value: String,
    onValueChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Field number
        Text(
            text = bitNumber.toString(),
            modifier = Modifier.width(100.dp)
        )

        // Field value
        TextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("X")  // Using Text as a simple remove button, could use an icon
        }
    }
}