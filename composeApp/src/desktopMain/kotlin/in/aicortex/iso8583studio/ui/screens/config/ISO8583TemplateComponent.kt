package `in`.aicortex.iso8583studio.ui.screens.config

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.aicortex.iso8583studio.data.BitSpecific
import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.BitType
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import kotlin.math.absoluteValue


/**
 * Main composable for ISO8583 Template tab
 */
@Composable
fun Iso8583TemplateScreen(config: GatewayConfig) {
    var bitTemplates by remember { mutableStateOf(config.bitTemplate) }
    var selectedBit by remember { mutableStateOf<BitSpecific?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var bitNumberToAdd by remember { mutableStateOf("88") }

    // Advanced options state
    var useAscii by remember { mutableStateOf(config.bitmapInAscii) }
    var dontUseTPDU by remember { mutableStateOf(config.doNotUseHeader) }
    var respondIfUnrecognized by remember { mutableStateOf(config.respondIfUnrecognized) }
    var metfoneMessage by remember { mutableStateOf(false) }
    var notUpdateScreen by remember { mutableStateOf(false) }
    var customizedMessage by remember { mutableStateOf(false) }
    var ignoreHeaderLength by remember { mutableStateOf("5") }
    var fixedResponseHeader by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Left side - Property Grid equivalent
        BitTemplatePropertyGrid(
            bitTemplates = bitTemplates,
            onBitSelected = {
                selectedBit = it
                showEditDialog = true
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        // Right side - Controls and settings
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 16.dp)
        ) {
            // Advanced options group
            AdvancedOptionsCard(
                useAscii = useAscii,
                dontUseTPDU = dontUseTPDU,
                respondIfUnrecognized = respondIfUnrecognized,
                metfoneMessage = metfoneMessage,
                notUpdateScreen = notUpdateScreen,
                onUseAsciiChange = { useAscii = it },
                onDontUseTPDUChange = {
                    dontUseTPDU = it
                },
                onRespondIfUnrecognizedChange = { respondIfUnrecognized = it },
                onMetfoneMessageChange = { metfoneMessage = it },
                onNotUpdateScreenChange = { notUpdateScreen = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Customized message group
            CustomizedMessageCard(
                customizedMessage = customizedMessage,
                ignoreHeaderLength = ignoreHeaderLength,
                fixedResponseHeader = fixedResponseHeader,
                onCustomizedMessageChange = { customizedMessage = it },
                onIgnoreHeaderLengthChange = { ignoreHeaderLength = it },
                onFixedResponseHeaderChange = { fixedResponseHeader = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bit manipulation controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Bit number",
                    modifier = Modifier.padding(end = 8.dp)
                )

                TextField(
                    value = bitNumberToAdd,
                    onValueChange = { bitNumberToAdd = it },
                    modifier = Modifier.width(80.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val bitNo = bitNumberToAdd.toIntOrNull() ?: return@Button
                        // Check if bit already exists
                        if (bitTemplates.none { it.bitNumber.toInt().absoluteValue == bitNo }) {
                            val newBit = BitSpecific(
                                bitNo = bitNo.toUByte().toByte(),
                                bitLenAtrr = BitLength.LLVAR,
                                bitTypeAtrr = BitType.ANS,
                                maxLen = 999
                            )
                            // Add to list in sorted order
                            val newList = (bitTemplates + newBit).sortedBy { it.bitNumber }
                            bitTemplates = newList.sortedBy { it.bitNumber.toInt().absoluteValue }.toTypedArray()
                        }
                    }
                ) {
                    Text("Add")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val bitNo = bitNumberToAdd.toIntOrNull() ?: return@Button
                        bitTemplates = bitTemplates.filter { it.bitNumber.toInt().absoluteValue != bitNo }.toTypedArray()
                    }
                ) {
                    Text("Delete")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    config.doNotUseHeader = dontUseTPDU
                    config.bitmapInAscii = useAscii
                    config.respondIfUnrecognized = respondIfUnrecognized
                },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("Save")
            }
        }
    }

    // Edit dialog for bit properties
    if (showEditDialog && selectedBit != null) {
        BitEditDialog(
            bit = selectedBit!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedBit ->
                bitTemplates = bitTemplates.map {
                    if (it.bitNumber == updatedBit.bitNumber) updatedBit else it
                }.toTypedArray()
                config.bitTemplate = bitTemplates
                showEditDialog = false
                selectedBit = null
            }
        )
    }
}

/**
 * Property grid for bit templates - like PropertyGrid in original C# code
 */
@Composable
fun BitTemplatePropertyGrid(
    bitTemplates: Array<BitSpecific>,
    onBitSelected: (BitSpecific) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Bit number
                Text(
                    text = "Bit No. ",
                    modifier = Modifier,
                    fontWeight = FontWeight.Bold
                )

                // Type
                Text(
                    text = "Format Type",
                    modifier = Modifier
                )

                // Length
                Text(
                    text = "Length Type",
                    modifier = Modifier
                )

                // Max length
                Text(
                    text = "Max Length",
                    modifier = Modifier
                )

            }

            // List of bit templates
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(bitTemplates.size) { index ->
                    BitPropertyRow(
                        bit = bitTemplates[index],
                        onClick = { onBitSelected(bitTemplates[index]) }
                    )
                    Divider()
                }
            }
        }
    }
}

/**
 * Individual row in the property grid
 */
@Composable
fun BitPropertyRow(
    bit: BitSpecific,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = false, onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        // Bit number
        Text(
            text = "Bit ${bit.bitNumber.toInt().absoluteValue}",
            modifier = Modifier,
            fontWeight = FontWeight.Bold
        )

        // Type
        Text(
            text = bit.bitType.name,
            modifier = Modifier
        )

        // Length
        Text(
            text = bit.bitLength.name,
            modifier = Modifier
        )

        // Max length
        Text(
            text = bit.maxLength.toString(),
            modifier = Modifier
        )

    }
}

/**
 * Dialog for editing bit properties
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BitEditDialog(
    bit: BitSpecific,
    onDismiss: () -> Unit,
    onSave: (BitSpecific) -> Unit
) {
    // Create a mutable copy of the bit to edit
    var editedBit = remember { bit.copy() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Edit Bit ${bit.bitNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bit Length dropdown
                Text("Bit Length", fontWeight = FontWeight.Medium)
                var bitLengthExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = bitLengthExpanded,
                    onExpandedChange = { bitLengthExpanded = it }
                ) {
                    TextField(
                        readOnly = true,
                        value = editedBit.bitLength.name,
                        onValueChange = { },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bitLengthExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = bitLengthExpanded,
                        onDismissRequest = { bitLengthExpanded = false }
                    ) {
                        BitLength.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    editedBit.bitLength = option
                                    bitLengthExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bit Type dropdown
                Text("Bit Type", fontWeight = FontWeight.Medium)
                var bitTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = bitTypeExpanded,
                    onExpandedChange = { bitTypeExpanded = it }
                ) {
                    TextField(
                        readOnly = true,
                        value = editedBit.bitType.name,
                        onValueChange = { },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bitTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = bitTypeExpanded,
                        onDismissRequest = { bitTypeExpanded = false }
                    ) {
                        BitType.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    editedBit.bitType = option
                                    bitTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Max Length field
                Text("Max Length", fontWeight = FontWeight.Medium)
                var maxLength by remember { mutableStateOf(editedBit.maxLength.toString()) }
                TextField(
                    value = maxLength,
                    onValueChange = {
                        maxLength = it
                        editedBit = editedBit.copy(
                            maxLength = it.toIntOrNull() ?: 0
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )



                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(editedBit) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Advanced Options Card
 */
@Composable
fun AdvancedOptionsCard(
    useAscii: Boolean,
    dontUseTPDU: Boolean,
    respondIfUnrecognized: Boolean,
    metfoneMessage: Boolean,
    notUpdateScreen: Boolean,
    onUseAsciiChange: (Boolean) -> Unit,
    onDontUseTPDUChange: (Boolean) -> Unit,
    onRespondIfUnrecognizedChange: (Boolean) -> Unit,
    onMetfoneMessageChange: (Boolean) -> Unit,
    onNotUpdateScreenChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Advanced options (be careful)",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = useAscii,
                    onCheckedChange = onUseAsciiChange
                )
                Text("Iso8583 use Ascii")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = dontUseTPDU,
                    onCheckedChange = onDontUseTPDUChange
                )
                Text("Don't use TPDU Header")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = respondIfUnrecognized,
                    onCheckedChange = onRespondIfUnrecognizedChange
                )
                Text("Respond same message if unrecognized")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = metfoneMessage,
                    onCheckedChange = onMetfoneMessageChange
                )
                Text("Metfone message")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = notUpdateScreen,
                    onCheckedChange = onNotUpdateScreenChange
                )
                Text("Not update screen")
            }
        }
    }
}

/**
 * Customized Message Card
 */
@Composable
fun CustomizedMessageCard(
    customizedMessage: Boolean,
    ignoreHeaderLength: String,
    fixedResponseHeader: String,
    onCustomizedMessageChange: (Boolean) -> Unit,
    onIgnoreHeaderLengthChange: (String) -> Unit,
    onFixedResponseHeaderChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = customizedMessage,
                    onCheckedChange = onCustomizedMessageChange
                )
                Text(
                    text = "Customized Message",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ignore Request header",
                    modifier = Modifier.width(150.dp)
                )

                TextField(
                    value = ignoreHeaderLength,
                    onValueChange = onIgnoreHeaderLengthChange,
                    modifier = Modifier.width(60.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("bytes")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Fixed response Header",
                    modifier = Modifier.width(150.dp)
                )

                TextField(
                    value = fixedResponseHeader,
                    onValueChange = onFixedResponseHeaderChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}
