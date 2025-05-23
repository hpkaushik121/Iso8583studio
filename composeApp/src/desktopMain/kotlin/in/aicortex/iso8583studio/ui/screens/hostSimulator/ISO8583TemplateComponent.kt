package `in`.aicortex.iso8583studio.ui.screens.hostSimulator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.aicortex.iso8583studio.data.BitSpecific
import `in`.aicortex.iso8583studio.data.model.*
import `in`.aicortex.iso8583studio.domain.utils.FileExporter
import `in`.aicortex.iso8583studio.domain.utils.FormatMappingConfig
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import `in`.aicortex.iso8583studio.ui.screens.components.FileExportComponent
import `in`.aicortex.iso8583studio.ui.screens.components.FileImportDialog

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue



/**
 * Main composable for ISO8583 Template tab with format support
 */
@Composable
fun Iso8583TemplateScreen(
    config: GatewayConfig,
    onSaveClick: () -> Unit,
    onConfigChange: (GatewayConfig) -> Unit = {}
) {
    var bitTemplates by remember { mutableStateOf(config.bitTemplate) }
    var selectedBit by remember { mutableStateOf<BitSpecific?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showYamlDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var bitNumberToAdd by remember { mutableStateOf("88") }
    var yamlConfigContent by remember { mutableStateOf("") }
    var yamlFileName by remember { mutableStateOf("") }

    // Advanced options state
    var useAscii by remember { mutableStateOf(config.lengthInAscii) }
    var codeFormat by remember { mutableStateOf(config.codeFormat ?: CodeFormat.BYTE_ARRAY) }
    var dontUseTPDU by remember { mutableStateOf(config.doNotUseHeader) }
    var respondIfUnrecognized by remember { mutableStateOf(config.respondIfUnrecognized) }
    var metfoneMessage by remember { mutableStateOf(config.metfoneMesage) }
    var notUpdateScreen by remember { mutableStateOf(config.notUpdateScreen) }
    var customizedMessage by remember { mutableStateOf(config.customizeMessage) }
    var ignoreHeaderLength by remember { mutableStateOf(config.ignoreRequestHeader.toString()) }
    var fixedResponseHeader by remember { mutableStateOf(config.fixedResponseHeader ?: byteArrayOf()) }

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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
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
                    onDontUseTPDUChange = { dontUseTPDU = it },
                    onRespondIfUnrecognizedChange = { respondIfUnrecognized = it },
                    onMetfoneMessageChange = { metfoneMessage = it },
                    onNotUpdateScreenChange = { notUpdateScreen = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                val coroutineScope = rememberCoroutineScope()
                // Message Encoder/Decoder Section
                MessageFormatCard(
                    config = config,
                    selectedFormat = codeFormat,
                    onFormatChange = { codeFormat = it
                        config.codeFormat = it},
                    onUploadYaml = {
                        config.formatMappingConfig = it
                    },
                    onDownloadTemplate = { format ->
                        yamlConfigContent = generateYamlTemplate(format, bitTemplates)
                        yamlFileName = "${format.displayName.lowercase()}_template.yaml"
                        showYamlDialog = true
                    },
                    onShowInfo = { showInfoDialog = true }
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
                            if (bitTemplates.none { it.bitNumber.toInt().absoluteValue == bitNo }) {
                                val newBit = BitSpecific(
                                    bitNo = bitNo.toUByte().toByte(),
                                    bitLenAtrr = BitLength.LLVAR,
                                    bitTypeAtrr = BitType.ANS,
                                    maxLen = 999
                                )
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
                        config.lengthInAscii = useAscii
                        config.respondIfUnrecognized = respondIfUnrecognized
                        config.metfoneMesage = metfoneMessage
                        config.notUpdateScreen = notUpdateScreen
                        config.customizeMessage = customizedMessage
                        config.ignoreRequestHeader = ignoreHeaderLength.toIntOrNull() ?: 5
                        config.fixedResponseHeader = fixedResponseHeader
                        onSaveClick()
                    },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Save")
                }
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

    // YAML content dialog
    if (showYamlDialog) {
        FileExportComponent(
            bitTemplates = config.bitTemplate,
            onExportComplete = {
                showYamlDialog = false
            }
        )
    }

    // Info dialog
    if (showInfoDialog) {
        YamlInfoDialog(
            onDismiss = { showInfoDialog = false }
        )
    }
}

/**
 * Message Format Configuration Card
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageFormatCard(
    config: GatewayConfig,
    selectedFormat: CodeFormat,
    onFormatChange: (CodeFormat) -> Unit,
    onUploadYaml: (FormatMappingConfig) -> Unit,
    onDownloadTemplate: (CodeFormat) -> Unit,
    onShowInfo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Format Selection Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Message Format:",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.3f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Format Selector
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(0.7f)
                ) {
                    TextField(
                        readOnly = true,
                        value = selectedFormat.displayName,
                        onValueChange = { },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        CodeFormat.values().forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format.displayName) },
                                onClick = {
                                    onFormatChange(format)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // YAML Configuration Section (only for formats that require YAML)
            if (selectedFormat.requiresYamlConfig) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "YAML Configuration",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                var showFileImportDialog by remember { mutableStateOf(false) }
                if(showFileImportDialog){
                    FileImportDialog(
                        gatewayConfig = config,
                        onDismiss = {
                            showFileImportDialog = false
                        },
                        onImportComplete = {
                            showFileImportDialog = false
                            onUploadYaml(it)
                        }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Upload YAML Button
                    Button(
                        onClick = {
                            showFileImportDialog = true

                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload YAML")
                    }

                    // Download Template Button
                    Button(
                        onClick = { onDownloadTemplate(selectedFormat) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download Template")
                    }

                    // Info Button
                    IconButton(
                        onClick = onShowInfo
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "YAML Configuration Info")
                    }
                }

                // Format-specific information
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = getFormatDescription(selectedFormat),
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Binary/Hex format uses your existing ISO8583 pack/unpack methods. No YAML configuration needed.",
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}


/**
 * YAML Configuration Information Dialog
 */
@Composable
fun YamlInfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "YAML Configuration Guide",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        YamlInfoSection(
                            title = "Configuration Structure",
                            content = """
                                formatType: Specifies the output format (JSON, XML, KEY_VALUE)
                                mti: Message Type Indicator mapping configuration
                                fieldMappings: Maps ISO8583 fields to format-specific keys
                                settings: Format-specific settings and options
                            """.trimIndent()
                        )
                    }

                    item {
                        YamlInfoSection(
                            title = "MTI Configuration",
                            content = """
                                key: Simple key name (e.g., "msgType")
                                nestedKey: Nested path (e.g., "header.messageType")
                                template: Dynamic template (e.g., "header.{mti}.type")
                            """.trimIndent()
                        )
                    }

                    item {
                        YamlInfoSection(
                            title = "Field Mappings",
                            content = """
                                "2": # ISO8583 Field number
                                  key: "pan"              # Simple key
                                  nestedKey: "card.pan"   # Nested structure
                                  staticValue: "00"       # Fixed value
                                  template: "card.{data}" # Dynamic template
                            """.trimIndent()
                        )
                    }

                    item {
                        YamlInfoSection(
                            title = "JSON Example Output",
                            content = """
                                nestedKey: "card.pan" produces:
                                {
                                  "card": {
                                    "pan": "4111111111111111"
                                  }
                                }
                            """.trimIndent()
                        )
                    }

                    item {
                        YamlInfoSection(
                            title = "XML Example Output",
                            content = """
                                nestedKey: "cardData.number" produces:
                                <iso8583>
                                  <cardData>
                                    <number>4111111111111111</number>
                                  </cardData>
                                </iso8583>
                            """.trimIndent()
                        )
                    }

                    item {
                        YamlInfoSection(
                            title = "Key-Value Example Output",
                            content = """
                                key: "PAN" produces:
                                MTI=0200|PAN=4111111111111111|AMOUNT=50000
                            """.trimIndent()
                        )
                    }

                    item {
                        YamlInfoSection(
                            title = "Settings Options",
                            content = """
                                prettyPrint: Format output with indentation
                                encoding: Character encoding (UTF-8, ASCII)
                                rootElement: XML root element name
                                delimiter: Key-Value pair separator
                                keyValueSeparator: Key-value separator character
                            """.trimIndent()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun YamlInfoSection(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = content,
                style = MaterialTheme.typography.body2,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

// Helper functions
private fun getFormatDescription(format: CodeFormat): String {
    return when (format) {
        CodeFormat.JSON -> "JSON format allows nested structures and flexible key naming. Upload YAML to define field mappings."
        CodeFormat.XML -> "XML format supports hierarchical data with elements and attributes. Configure structure via YAML."
        CodeFormat.PLAIN_TEXT -> "Key-Value format uses delimited pairs. Define keys and separators in YAML configuration."
        CodeFormat.BYTE_ARRAY -> "Binary format uses existing ISO8583 pack/unpack methods."
        CodeFormat.HEX -> "Hex format uses existing ISO8583 pack/unpack methods."
    }
}

private fun getSampleYamlConfig(format: CodeFormat): String {
    return when (format) {
        CodeFormat.JSON -> """
formatType: JSON
mti:
  nestedKey: header.messageType
fieldMappings:
  "2":
    nestedKey: card.pan
  "3":
    key: processingCode
  "4":
    nestedKey: transaction.amount
  "11":
    nestedKey: header.traceNumber
settings:
  prettyPrint: true
  encoding: UTF-8
        """.trimIndent()

        CodeFormat.XML -> """
formatType: XML
mti:
  key: messageType
fieldMappings:
  "2":
    nestedKey: cardData.number
  "3":
    key: processingCode
  "4":
    key: amount
settings:
  rootElement: iso8583
  prettyPrint: true
        """.trimIndent()

        CodeFormat.PLAIN_TEXT -> """
formatType: PLAIN_TEXT
mti:
  key: MTI
fieldMappings:
  "2":
    key: PAN
  "3":
    key: PROC_CODE
  "4":
    key: AMOUNT
settings:
  delimiter: "|"
  keyValueSeparator: "="
        """.trimIndent()

        else -> ""
    }
}

private fun generateYamlTemplate(format: CodeFormat, bitTemplates: Array<BitSpecific>): String {
    val fieldMappings = bitTemplates.joinToString("\n") { bit ->
        val fieldNum = bit.bitNumber.toInt().absoluteValue
        when (format) {
            CodeFormat.JSON -> """  "$fieldNum":
    nestedKey: field$fieldNum.value"""
            CodeFormat.XML -> """  "$fieldNum":
    key: field$fieldNum"""
            CodeFormat.PLAIN_TEXT -> """  "$fieldNum":
    key: F$fieldNum"""
            else -> ""
        }
    }

    return when (format) {
        CodeFormat.JSON -> """
formatType: JSON
mti:
  nestedKey: header.messageType
fieldMappings:
$fieldMappings
settings:
  prettyPrint: true
  encoding: UTF-8
        """.trimIndent()

        CodeFormat.XML -> """
formatType: XML
mti:
  key: messageType
fieldMappings:
$fieldMappings
settings:
  rootElement: iso8583
  prettyPrint: true
        """.trimIndent()

        CodeFormat.PLAIN_TEXT -> """
formatType: PLAIN_TEXT
mti:
  key: MTI
fieldMappings:
$fieldMappings
settings:
  delimiter: "|"
  keyValueSeparator: "="
        """.trimIndent()

        else -> ""
    }
}

// Keep all existing composables (BitTemplatePropertyGrid, BitPropertyRow, BitEditDialog, etc.)
// ... [Rest of the existing code remains the same]

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
                Text(
                    text = "Bit No. ",
                    modifier = Modifier.weight(.1f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Format Type",
                    modifier = Modifier.weight(.2f)
                )
                Text(
                    text = "Length Type",
                    modifier = Modifier.weight(.2f)
                )
                Text(
                    text = "Max Length",
                    modifier = Modifier.weight(.1f)
                )
                Text(
                    text = "Description",
                    modifier = Modifier.weight(.4f)
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
        Text(
            text = "Bit ${bit.bitNumber.toInt().absoluteValue}",
            modifier = Modifier.weight(.1f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = bit.bitType.name,
            modifier = Modifier.weight(.2f)
        )
        Text(
            text = bit.bitLength.name,
            modifier = Modifier.weight(.2f)
        )
        Text(
            text = bit.maxLength.toString(),
            modifier = Modifier.weight(.1f)
        )
        Text(
            text = bit.description,
            modifier = Modifier.weight(.4f)
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
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
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
    fixedResponseHeader: ByteArray,
    onCustomizedMessageChange: (Boolean) -> Unit,
    onIgnoreHeaderLengthChange: (String) -> Unit,
    onFixedResponseHeaderChange: (ByteArray) -> Unit
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
                var fixedResponseHeaderText by remember { mutableStateOf(IsoUtil.bcdToString(fixedResponseHeader)) }
                TextField(
                    value = fixedResponseHeaderText,
                    onValueChange = {
                        fixedResponseHeaderText = it
                        onFixedResponseHeaderChange(IsoUtil.stringToBCD(it, ignoreHeaderLength.toIntOrNull() ?: 5))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}