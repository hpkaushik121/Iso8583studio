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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.aicortex.iso8583studio.data.BitSpecific
import `in`.aicortex.iso8583studio.data.model.*
import `in`.aicortex.iso8583studio.domain.utils.FormatMappingConfig
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import `in`.aicortex.iso8583studio.ui.screens.components.FileExportComponent
import `in`.aicortex.iso8583studio.ui.screens.components.FileImportDialog

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
    var bitTemplates by remember { mutableStateOf(config.bitTemplateSource) }
    var selectedBit by remember { mutableStateOf<BitSpecific?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showYamlDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var bitNumberToAdd by remember { mutableStateOf("88") }
    var yamlConfigContent by remember { mutableStateOf("") }
    var yamlFileName by remember { mutableStateOf("") }

    // Determine which sections to show based on gateway type
    val showIncoming = config.gatewayType == GatewayType.PROXY || config.gatewayType == GatewayType.SERVER
    val showOutgoing = config.gatewayType == GatewayType.PROXY || config.gatewayType == GatewayType.CLIENT
    val bothVisible = showIncoming && showOutgoing



    // Collapsible section states
    var incomingExpanded by remember { mutableStateOf(true) }
    var outgoingExpanded by remember { mutableStateOf(false) }

    // Adjust expansion logic when only one section is visible
    val incomingExpandedState = if (bothVisible) incomingExpanded else true
    val outgoingExpandedState = if (bothVisible) outgoingExpanded else true

    // Advanced options state - Source (Incoming)
    var useAsciiSource by remember { mutableStateOf(config.messageInAsciiSource) }
    var codeFormatSource by remember { mutableStateOf(config.codeFormatSource ?: CodeFormat.BYTE_ARRAY) }
    var dontUseTPDUSource by remember { mutableStateOf(config.doNotUseHeaderSource) }
    var respondIfUnrecognizedSource by remember { mutableStateOf(config.respondIfUnrecognizedSource) }
    var metfoneMessageSource by remember { mutableStateOf(config.metfoneMesageSource) }
    var notUpdateScreenSource by remember { mutableStateOf(config.notUpdateScreenSource) }
    var customizedMessageSource by remember { mutableStateOf(config.customizeMessageSource) }
    var ignoreHeaderLengthSource by remember { mutableStateOf(config.ignoreRequestHeaderSource.toString()) }
    var fixedResponseHeaderSource by remember { mutableStateOf(config.fixedResponseHeaderSource ?: byteArrayOf()) }
    var bitTemplatesSource by remember { mutableStateOf(config.bitTemplateSource) }

    // Advanced options state - Dest (Outgoing)
    var useAsciiDest by remember { mutableStateOf(config.messageInAsciiDest ?: config.messageInAsciiSource) }
    var codeFormatDest by remember { mutableStateOf(config.codeFormatDest ?: config.codeFormatSource ?: CodeFormat.BYTE_ARRAY) }
    var dontUseTPDUDest by remember { mutableStateOf(config.doNotUseHeaderDest ?: config.doNotUseHeaderSource) }
    var respondIfUnrecognizedDest by remember { mutableStateOf(config.respondIfUnrecognizedDest ?: config.respondIfUnrecognizedSource) }
    var metfoneMessageDest by remember { mutableStateOf(config.metfoneMesageDest ?: config.metfoneMesageSource) }
    var notUpdateScreenDest by remember { mutableStateOf(config.notUpdateScreenDest ?: config.notUpdateScreenSource) }
    var customizedMessageDest by remember { mutableStateOf(config.customizeMessageDest ?: config.customizeMessageSource) }
    var ignoreHeaderLengthDest by remember { mutableStateOf((config.ignoreRequestHeaderDest ?: config.ignoreRequestHeaderSource).toString()) }
    var fixedResponseHeaderDest by remember { mutableStateOf(config.fixedResponseHeaderDest ?: config.fixedResponseHeaderSource ?: byteArrayOf()) }
    var bitTemplatesDest by remember { mutableStateOf(config.bitTemplateDest ?: config.bitTemplateSource) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main content area
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Incoming Section - Show based on gateway type
                    if (showIncoming) {
                        if (bothVisible) {
                            // Collapsible when both sections are visible
                            CollapsibleSection(
                                title = "📥 Incoming (Source) Configuration",
                                isExpanded = incomingExpandedState,
                                onToggle = {
                                    incomingExpanded = it
                                    if (it) outgoingExpanded = false
                                },
                                modifier = Modifier.weight(if (incomingExpandedState) 1f else 0.1f)
                            ) {
                                IncomingConfigurationContent(
                                    bitTemplatesSource = bitTemplatesSource,
                                    onBitTemplatesSourceChange = { bitTemplatesSource = it },
                                    selectedBit = selectedBit,
                                    onBitSelected = { selectedBit = it; showEditDialog = true },
                                    config = config,
                                    useAsciiSource = useAsciiSource,
                                    codeFormatSource = codeFormatSource,
                                    dontUseTPDUSource = dontUseTPDUSource,
                                    respondIfUnrecognizedSource = respondIfUnrecognizedSource,
                                    metfoneMessageSource = metfoneMessageSource,
                                    notUpdateScreenSource = notUpdateScreenSource,
                                    customizedMessageSource = customizedMessageSource,
                                    ignoreHeaderLengthSource = ignoreHeaderLengthSource,
                                    fixedResponseHeaderSource = fixedResponseHeaderSource,
                                    onUseAsciiSourceChange = { useAsciiSource = it },
                                    onCodeFormatSourceChange = { codeFormatSource = it },
                                    onDontUseTPDUSourceChange = { dontUseTPDUSource = it },
                                    onRespondIfUnrecognizedSourceChange = { respondIfUnrecognizedSource = it },
                                    onMetfoneMessageSourceChange = { metfoneMessageSource = it },
                                    onNotUpdateScreenSourceChange = { notUpdateScreenSource = it },
                                    onCustomizedMessageSourceChange = { customizedMessageSource = it },
                                    onIgnoreHeaderLengthSourceChange = { ignoreHeaderLengthSource = it },
                                    onFixedResponseHeaderSourceChange = { fixedResponseHeaderSource = it },
                                    onShowYamlDialog = { yamlConfigContent = generateYamlTemplate(it, bitTemplatesSource); yamlFileName = "${it.displayName.lowercase()}_source_template.yaml"; showYamlDialog = true },
                                    onShowInfoDialog = { showInfoDialog = true }
                                )
                            }
                        } else {
                            // Non-collapsible when only one section is visible
                            IncomingConfigurationContent(
                                bitTemplatesSource = bitTemplatesSource,
                                onBitTemplatesSourceChange = { bitTemplatesSource = it },
                                selectedBit = selectedBit,
                                onBitSelected = { selectedBit = it; showEditDialog = true },
                                config = config,
                                useAsciiSource = useAsciiSource,
                                codeFormatSource = codeFormatSource,
                                dontUseTPDUSource = dontUseTPDUSource,
                                respondIfUnrecognizedSource = respondIfUnrecognizedSource,
                                metfoneMessageSource = metfoneMessageSource,
                                notUpdateScreenSource = notUpdateScreenSource,
                                customizedMessageSource = customizedMessageSource,
                                ignoreHeaderLengthSource = ignoreHeaderLengthSource,
                                fixedResponseHeaderSource = fixedResponseHeaderSource,
                                onUseAsciiSourceChange = { useAsciiSource = it },
                                onCodeFormatSourceChange = { codeFormatSource = it },
                                onDontUseTPDUSourceChange = { dontUseTPDUSource = it },
                                onRespondIfUnrecognizedSourceChange = { respondIfUnrecognizedSource = it },
                                onMetfoneMessageSourceChange = { metfoneMessageSource = it },
                                onNotUpdateScreenSourceChange = { notUpdateScreenSource = it },
                                onCustomizedMessageSourceChange = { customizedMessageSource = it },
                                onIgnoreHeaderLengthSourceChange = { ignoreHeaderLengthSource = it },
                                onFixedResponseHeaderSourceChange = { fixedResponseHeaderSource = it },
                                onShowYamlDialog = { yamlConfigContent = generateYamlTemplate(it, bitTemplatesSource); yamlFileName = "${it.displayName.lowercase()}_source_template.yaml"; showYamlDialog = true },
                                onShowInfoDialog = { showInfoDialog = true }
                            )
                        }
                    }

                    if (showIncoming && showOutgoing && !incomingExpandedState) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Outgoing Section - Show based on gateway type
                    if (showOutgoing) {
                        if (bothVisible) {
                            // Collapsible when both sections are visible
                            CollapsibleSection(
                                title = "📤 Outgoing (Dest) Configuration",
                                isExpanded = outgoingExpandedState,
                                onToggle = {
                                    outgoingExpanded = it
                                    if (it) incomingExpanded = false
                                },
                                modifier = Modifier.weight(if (outgoingExpandedState) 1f else 0.1f)
                            ) {
                                OutgoingConfigurationContent(
                                    bitTemplatesDest = bitTemplatesDest,
                                    onBitTemplatesDestChange = { bitTemplatesDest = it },
                                    selectedBit = selectedBit,
                                    onBitSelected = { selectedBit = it; showEditDialog = true },
                                    config = config,
                                    useAsciiDest = useAsciiDest,
                                    codeFormatDest = codeFormatDest,
                                    dontUseTPDUDest = dontUseTPDUDest,
                                    respondIfUnrecognizedDest = respondIfUnrecognizedDest,
                                    metfoneMessageDest = metfoneMessageDest,
                                    notUpdateScreenDest = notUpdateScreenDest,
                                    customizedMessageDest = customizedMessageDest,
                                    ignoreHeaderLengthDest = ignoreHeaderLengthDest,
                                    fixedResponseHeaderDest = fixedResponseHeaderDest,
                                    onUseAsciiDestChange = { useAsciiDest = it },
                                    onCodeFormatDestChange = { codeFormatDest = it },
                                    onDontUseTPDUDestChange = { dontUseTPDUDest = it },
                                    onRespondIfUnrecognizedDestChange = { respondIfUnrecognizedDest = it },
                                    onMetfoneMessageDestChange = { metfoneMessageDest = it },
                                    onNotUpdateScreenDestChange = { notUpdateScreenDest = it },
                                    onCustomizedMessageDestChange = { customizedMessageDest = it },
                                    onIgnoreHeaderLengthDestChange = { ignoreHeaderLengthDest = it },
                                    onFixedResponseHeaderDestChange = { fixedResponseHeaderDest = it },
                                    onShowYamlDialog = { yamlConfigContent = generateYamlTemplate(it, bitTemplatesDest); yamlFileName = "${it.displayName.lowercase()}_dest_template.yaml"; showYamlDialog = true },
                                    onShowInfoDialog = { showInfoDialog = true }
                                )
                            }
                        } else {
                            // Non-collapsible when only one section is visible
                            OutgoingConfigurationContent(
                                bitTemplatesDest = bitTemplatesDest,
                                onBitTemplatesDestChange = { bitTemplatesDest = it },
                                selectedBit = selectedBit,
                                onBitSelected = { selectedBit = it; showEditDialog = true },
                                config = config,
                                useAsciiDest = useAsciiDest,
                                codeFormatDest = codeFormatDest,
                                dontUseTPDUDest = dontUseTPDUDest,
                                respondIfUnrecognizedDest = respondIfUnrecognizedDest,
                                metfoneMessageDest = metfoneMessageDest,
                                notUpdateScreenDest = notUpdateScreenDest,
                                customizedMessageDest = customizedMessageDest,
                                ignoreHeaderLengthDest = ignoreHeaderLengthDest,
                                fixedResponseHeaderDest = fixedResponseHeaderDest,
                                onUseAsciiDestChange = { useAsciiDest = it },
                                onCodeFormatDestChange = { codeFormatDest = it },
                                onDontUseTPDUDestChange = { dontUseTPDUDest = it },
                                onRespondIfUnrecognizedDestChange = { respondIfUnrecognizedDest = it },
                                onMetfoneMessageDestChange = { metfoneMessageDest = it },
                                onNotUpdateScreenDestChange = { notUpdateScreenDest = it },
                                onCustomizedMessageDestChange = { customizedMessageDest = it },
                                onIgnoreHeaderLengthDestChange = { ignoreHeaderLengthDest = it },
                                onFixedResponseHeaderDestChange = { fixedResponseHeaderDest = it },
                                onShowYamlDialog = { yamlConfigContent = generateYamlTemplate(it, bitTemplatesDest); yamlFileName = "${it.displayName.lowercase()}_dest_template.yaml"; showYamlDialog = true },
                                onShowInfoDialog = { showInfoDialog = true }
                            )
                        }
                    }

                    if(!incomingExpandedState && !outgoingExpandedState){
                        Spacer(modifier = Modifier.weight(.8f))
                    }
                }
            }

            // Compact Save button - Fixed at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        // Save Source settings
                        if (showIncoming) {
                            config.doNotUseHeaderSource = dontUseTPDUSource
                            config.messageInAsciiSource = useAsciiSource
                            config.respondIfUnrecognizedSource = respondIfUnrecognizedSource
                            config.metfoneMesageSource = metfoneMessageSource
                            config.notUpdateScreenSource = notUpdateScreenSource
                            config.customizeMessageSource = customizedMessageSource
                            config.ignoreRequestHeaderSource = ignoreHeaderLengthSource.toIntOrNull() ?: 5
                            config.fixedResponseHeaderSource = fixedResponseHeaderSource
                            config.codeFormatSource = codeFormatSource
                            config.bitTemplateSource = bitTemplatesSource
                        }

                        // Save Dest settings
                        if (showOutgoing) {
                            config.doNotUseHeaderDest = dontUseTPDUDest
                            config.messageInAsciiDest = useAsciiDest
                            config.respondIfUnrecognizedDest = respondIfUnrecognizedDest
                            config.metfoneMesageDest = metfoneMessageDest
                            config.notUpdateScreenDest = notUpdateScreenDest
                            config.customizeMessageDest = customizedMessageDest
                            config.ignoreRequestHeaderDest = ignoreHeaderLengthDest.toIntOrNull() ?: 5
                            config.fixedResponseHeaderDest = fixedResponseHeaderDest
                            config.codeFormatDest = codeFormatDest
                            config.bitTemplateDest = bitTemplatesDest
                        }

                        onSaveClick()
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", fontSize = 14.sp)
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
                // Update the appropriate bit template array based on which section is expanded
                if (incomingExpanded) {
                    bitTemplatesSource = bitTemplatesSource.map {
                        if (it.bitNumber == updatedBit.bitNumber) updatedBit else it
                    }.toTypedArray()
                    config.bitTemplateSource = bitTemplatesSource
                } else {
                    bitTemplatesDest = bitTemplatesDest.map {
                        if (it.bitNumber == updatedBit.bitNumber) updatedBit else it
                    }.toTypedArray()
                    config.bitTemplateDest = bitTemplatesDest
                }

                // Update simulated transactions if needed
                val simulatedTrans = config.simulatedTransactionsToSource.map {
                    val fields = it.fields?.mapIndexed { i , field ->
                        if(i == (updatedBit.bitNumber.toInt().absoluteValue -1)){
                            field.apply {
                                lengthAttribute = updatedBit.bitLength
                                typeAtribute = updatedBit.bitType
                                maxLength = updatedBit.maxLength
                                additionalOption = updatedBit.addtionalOption
                                isSet = true
                            }
                        }else{
                            field
                        }

                    }
                    it.copy(fields = fields)
                }
                config.simulatedTransactionsToSource = simulatedTrans
                showEditDialog = false
                selectedBit = null
            }
        )
    }

    // YAML content dialog
    if (showYamlDialog) {
        FileExportComponent(
            bitTemplates = if (incomingExpanded) bitTemplatesSource else bitTemplatesDest,
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
    title: String,
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
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

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

/**
 * Property grid for bit templates - like PropertyGrid in original C# code
 */
@Composable
fun BitTemplatePropertyGrid(
    title: String,
    bitTemplates: Array<BitSpecific>,
    onBitSelected: (BitSpecific) -> Unit,
    onBitTemplatesChange: (Array<BitSpecific>) -> Unit,
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
            // Title and Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary)
                    .padding(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bit No.",
                        modifier = Modifier.weight(.1f),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onPrimary
                    )
                    Text(
                        text = "Format Type",
                        modifier = Modifier.weight(.2f),
                        color = MaterialTheme.colors.onPrimary
                    )
                    Text(
                        text = "Length Type",
                        modifier = Modifier.weight(.2f),
                        color = MaterialTheme.colors.onPrimary
                    )
                    Text(
                        text = "Max Length",
                        modifier = Modifier.weight(.1f),
                        color = MaterialTheme.colors.onPrimary
                    )
                    Text(
                        text = "Description",
                        modifier = Modifier.weight(.4f),
                        color = MaterialTheme.colors.onPrimary
                    )
                }
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
 * Advanced Options Card with title parameter for Source/Dest differentiation
 */
@Composable
fun AdvancedOptionsCard(
    title: String,
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
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Advanced options (be careful)",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
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
 * Customized Message Card with title parameter for Source/Dest differentiation
 */
@Composable
fun CustomizedMessageCard(
    title: String,
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
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

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
                    fontWeight = FontWeight.Medium
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

/**
 * Incoming Configuration Content Component
 */
@Composable
fun IncomingConfigurationContent(
    bitTemplatesSource: Array<BitSpecific>,
    onBitTemplatesSourceChange: (Array<BitSpecific>) -> Unit,
    selectedBit: BitSpecific?,
    onBitSelected: (BitSpecific) -> Unit,
    config: GatewayConfig,
    useAsciiSource: Boolean,
    codeFormatSource: CodeFormat,
    dontUseTPDUSource: Boolean,
    respondIfUnrecognizedSource: Boolean,
    metfoneMessageSource: Boolean,
    notUpdateScreenSource: Boolean,
    customizedMessageSource: Boolean,
    ignoreHeaderLengthSource: String,
    fixedResponseHeaderSource: ByteArray,
    onUseAsciiSourceChange: (Boolean) -> Unit,
    onCodeFormatSourceChange: (CodeFormat) -> Unit,
    onDontUseTPDUSourceChange: (Boolean) -> Unit,
    onRespondIfUnrecognizedSourceChange: (Boolean) -> Unit,
    onMetfoneMessageSourceChange: (Boolean) -> Unit,
    onNotUpdateScreenSourceChange: (Boolean) -> Unit,
    onCustomizedMessageSourceChange: (Boolean) -> Unit,
    onIgnoreHeaderLengthSourceChange: (String) -> Unit,
    onFixedResponseHeaderSourceChange: (ByteArray) -> Unit,
    onShowYamlDialog: (CodeFormat) -> Unit,
    onShowInfoDialog: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Left side - Bit Templates
        BitTemplatePropertyGrid(
            title = "Source Bit Templates",
            bitTemplates = bitTemplatesSource,
            onBitSelected = onBitSelected,
            onBitTemplatesChange = onBitTemplatesSourceChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        // Right side - Configuration Settings
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
                // Advanced options group - Source (Incoming)
                AdvancedOptionsCard(
                    title = "Source Advanced Options",
                    useAscii = useAsciiSource,
                    dontUseTPDU = dontUseTPDUSource,
                    respondIfUnrecognized = respondIfUnrecognizedSource,
                    metfoneMessage = metfoneMessageSource,
                    notUpdateScreen = notUpdateScreenSource,
                    onUseAsciiChange = onUseAsciiSourceChange,
                    onDontUseTPDUChange = onDontUseTPDUSourceChange,
                    onRespondIfUnrecognizedChange = onRespondIfUnrecognizedSourceChange,
                    onMetfoneMessageChange = onMetfoneMessageSourceChange,
                    onNotUpdateScreenChange = onNotUpdateScreenSourceChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Message Encoder/Decoder Section - Source
                MessageFormatCard(
                    title = "Source Message Format",
                    config = config,
                    selectedFormat = codeFormatSource,
                    onFormatChange = {
                        onCodeFormatSourceChange(it)
                        config.codeFormatSource = it
                    },
                    onUploadYaml = {
                        config.formatMappingConfigSource = it
                    },
                    onDownloadTemplate = onShowYamlDialog,
                    onShowInfo = onShowInfoDialog
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Customized message group - Source
                CustomizedMessageCard(
                    title = "Source Customized Message",
                    customizedMessage = customizedMessageSource,
                    ignoreHeaderLength = ignoreHeaderLengthSource,
                    fixedResponseHeader = fixedResponseHeaderSource,
                    onCustomizedMessageChange = onCustomizedMessageSourceChange,
                    onIgnoreHeaderLengthChange = onIgnoreHeaderLengthSourceChange,
                    onFixedResponseHeaderChange = onFixedResponseHeaderSourceChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bit manipulation controls for Source
                BitManipulationControls(
                    title = "Source Bit Templates Management",
                    bitTemplates = bitTemplatesSource,
                    onBitTemplatesChange = onBitTemplatesSourceChange
                )
            }
        }
    }
}

/**
 * Outgoing Configuration Content Component
 */
@Composable
fun OutgoingConfigurationContent(
    bitTemplatesDest: Array<BitSpecific>,
    onBitTemplatesDestChange: (Array<BitSpecific>) -> Unit,
    selectedBit: BitSpecific?,
    onBitSelected: (BitSpecific) -> Unit,
    config: GatewayConfig,
    useAsciiDest: Boolean,
    codeFormatDest: CodeFormat,
    dontUseTPDUDest: Boolean,
    respondIfUnrecognizedDest: Boolean,
    metfoneMessageDest: Boolean,
    notUpdateScreenDest: Boolean,
    customizedMessageDest: Boolean,
    ignoreHeaderLengthDest: String,
    fixedResponseHeaderDest: ByteArray,
    onUseAsciiDestChange: (Boolean) -> Unit,
    onCodeFormatDestChange: (CodeFormat) -> Unit,
    onDontUseTPDUDestChange: (Boolean) -> Unit,
    onRespondIfUnrecognizedDestChange: (Boolean) -> Unit,
    onMetfoneMessageDestChange: (Boolean) -> Unit,
    onNotUpdateScreenDestChange: (Boolean) -> Unit,
    onCustomizedMessageDestChange: (Boolean) -> Unit,
    onIgnoreHeaderLengthDestChange: (String) -> Unit,
    onFixedResponseHeaderDestChange: (ByteArray) -> Unit,
    onShowYamlDialog: (CodeFormat) -> Unit,
    onShowInfoDialog: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Left side - Bit Templates
        BitTemplatePropertyGrid(
            title = "Dest Bit Templates",
            bitTemplates = bitTemplatesDest,
            onBitSelected = onBitSelected,
            onBitTemplatesChange = onBitTemplatesDestChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        // Right side - Configuration Settings
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
                // Advanced options group - Dest (Outgoing)
                AdvancedOptionsCard(
                    title = "Dest Advanced Options",
                    useAscii = useAsciiDest,
                    dontUseTPDU = dontUseTPDUDest,
                    respondIfUnrecognized = respondIfUnrecognizedDest,
                    metfoneMessage = metfoneMessageDest,
                    notUpdateScreen = notUpdateScreenDest,
                    onUseAsciiChange = onUseAsciiDestChange,
                    onDontUseTPDUChange = onDontUseTPDUDestChange,
                    onRespondIfUnrecognizedChange = onRespondIfUnrecognizedDestChange,
                    onMetfoneMessageChange = onMetfoneMessageDestChange,
                    onNotUpdateScreenChange = onNotUpdateScreenDestChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Message Encoder/Decoder Section - Dest
                MessageFormatCard(
                    title = "Dest Message Format",
                    config = config,
                    selectedFormat = codeFormatDest,
                    onFormatChange = {
                        onCodeFormatDestChange(it)
                        config.codeFormatDest = it
                    },
                    onUploadYaml = {
                        config.formatMappingConfigDest = it
                    },
                    onDownloadTemplate = onShowYamlDialog,
                    onShowInfo = onShowInfoDialog
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Customized message group - Dest
                CustomizedMessageCard(
                    title = "Dest Customized Message",
                    customizedMessage = customizedMessageDest,
                    ignoreHeaderLength = ignoreHeaderLengthDest,
                    fixedResponseHeader = fixedResponseHeaderDest,
                    onCustomizedMessageChange = onCustomizedMessageDestChange,
                    onIgnoreHeaderLengthChange = onIgnoreHeaderLengthDestChange,
                    onFixedResponseHeaderChange = onFixedResponseHeaderDestChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bit manipulation controls for Dest
                BitManipulationControls(
                    title = "Dest Bit Templates Management",
                    bitTemplates = bitTemplatesDest,
                    onBitTemplatesChange = onBitTemplatesDestChange
                )
            }
        }
    }
}

/**
 * Collapsible Section Component
 */
@Composable
fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isExpanded) Modifier.fillMaxHeight() else Modifier)
        ) {
            // Header with toggle button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(!isExpanded) }
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colors.primary
                )
            }

            // Collapsible content
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Bit Manipulation Controls Component
 */
@Composable
fun BitManipulationControls(
    title: String,
    bitTemplates: Array<BitSpecific>,
    onBitTemplatesChange: (Array<BitSpecific>) -> Unit
) {
    var bitNumberToAdd by remember { mutableStateOf("88") }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

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
                            onBitTemplatesChange(newList.sortedBy { it.bitNumber.toInt().absoluteValue }.toTypedArray())
                        }
                    }
                ) {
                    Text("Add")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val bitNo = bitNumberToAdd.toIntOrNull() ?: return@Button
                        onBitTemplatesChange(bitTemplates.filter { it.bitNumber.toInt().absoluteValue != bitNo }.toTypedArray())
                    }
                ) {
                    Text("Delete")
                }
            }
        }
    }
}