package `in`.aicortex.iso8583studio.ui.screens.components

import androidx.compose.animation.core.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.aicortex.iso8583studio.data.BitSpecific
import `in`.aicortex.iso8583studio.data.model.CodeFormat
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import javax.swing.JFileChooser
import kotlin.math.absoluteValue

/**
 * File Export Configuration
 */
data class ExportConfig(
    val format: CodeFormat = CodeFormat.JSON,
    val fileName: String = "iso8583_config",
    val includeComments: Boolean = true,
    val prettyFormat: Boolean = true,
    val exportPath: String = System.getProperty("user.home")
)

/**
 * Scrollable File Export Component with Animations
 */
@Composable
fun FileExportComponent(
    bitTemplates: Array<BitSpecific>,
    modifier: Modifier = Modifier,
    onExportComplete: (String) -> Unit = {}
) {
    var exportConfig by remember { mutableStateOf(ExportConfig()) }
    var isExporting by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var previewContent by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val statusCardScale by animateFloatAsState(
        targetValue = if (exportStatus != null) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val exportButtonScale by animateFloatAsState(
        targetValue = if (isExporting) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )

    Dialog(
        onDismissRequest = {
            onExportComplete("")
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(700.dp)
                .clip(shape = RoundedCornerShape(8.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Left Panel - Export Settings
            Card(
                modifier = Modifier
                    .width(700.dp)
                    .heightIn(min = 600.dp),
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    ExportSettingsPanel(
                        exportConfig = exportConfig,
                        onConfigChange = { exportConfig = it },
                        bitTemplates = bitTemplates,
                    )


                    // Status Bar with scale animation
                    if (exportStatus != null) {
                        Card(
                            backgroundColor = if (exportStatus?.contains("successful") == true)
                                Color.Green.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(statusCardScale),
                            elevation = 4.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (exportStatus?.contains("successful") == true)
                                        Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (exportStatus?.contains("successful") == true)
                                        Color.Green.copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = exportStatus ?: "",
                                    color = if (exportStatus?.contains("successful") == true)
                                        Color.Green.copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isExporting = true
                                    exportStatus = null
                                    try {
                                        val result = performExport(exportConfig, bitTemplates)
                                        exportStatus = "Export successful: $result"
                                        onExportComplete(result)
                                    } catch (e: Exception) {
                                        exportStatus = "Export failed: ${e.message}"
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .scale(exportButtonScale),
                            enabled = !isExporting && exportConfig.fileName.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.elevation(6.dp)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colors.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.FileDownload, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isExporting) "Exporting..." else "Export",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enhanced Export Settings Panel with Animations
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExportSettingsPanel(
    exportConfig: ExportConfig,
    onConfigChange: (ExportConfig) -> Unit,
    bitTemplates: Array<BitSpecific>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Export Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )


        // Format Selection
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)){
            Text("Export Format", fontWeight = FontWeight.Medium, fontSize = 16.sp)
            var formatExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = formatExpanded,
                onExpandedChange = { formatExpanded = it }
            ) {
                TextField(
                    readOnly = true,
                    value = exportConfig.format.displayName,
                    onValueChange = { },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                ExposedDropdownMenu(
                    expanded = formatExpanded,
                    onDismissRequest = { formatExpanded = false }
                ) {
                    CodeFormat.values().forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format.displayName) },
                            onClick = {
                                onConfigChange(exportConfig.copy(format = format))
                                formatExpanded = false
                            }
                        )
                    }
                }
            }
        }


        // File Name

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("File Name", fontWeight = FontWeight.Medium, fontSize = 16.sp)
            TextField(
                value = exportConfig.fileName,
                onValueChange = { onConfigChange(exportConfig.copy(fileName = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter file name") },
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }


        // Export Path

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Export Path", fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = exportConfig.exportPath,
                    onValueChange = { onConfigChange(exportConfig.copy(exportPath = it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Select export directory") },
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Button(
                    onClick = {
                        val selectedPath = selectDirectory()
                        if (selectedPath != null) {
                            onConfigChange(exportConfig.copy(exportPath = selectedPath))
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                }
            }
        }


        // Options

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Export Options", fontWeight = FontWeight.Medium, fontSize = 16.sp)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = exportConfig.includeComments,
                    onCheckedChange = { onConfigChange(exportConfig.copy(includeComments = it)) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colors.primary
                    )
                )
                Text("Include comments and documentation", fontSize = 14.sp)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = exportConfig.prettyFormat,
                    onCheckedChange = { onConfigChange(exportConfig.copy(prettyFormat = it)) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colors.primary
                    )
                )
                Text("Pretty format (indented)", fontSize = 14.sp)
            }
        }


        // Export Information

        Card(
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Export Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Fields to export: ${bitTemplates.size}", fontSize = 14.sp)
                Text("Format: ${exportConfig.format.displayName}", fontSize = 14.sp)
                Text(
                    "Output file: ${exportConfig.fileName}.${getFileExtension(exportConfig.format)}",
                    fontSize = 14.sp
                )
            }
        }
        }


    }
}

// Keep all the existing helper functions unchanged
private fun generateExportContent(config: ExportConfig, bitTemplates: Array<BitSpecific>): String {
    return when (config.format) {
        CodeFormat.JSON -> generateJsonConfig(bitTemplates, config)
        CodeFormat.XML -> generateXmlConfig(bitTemplates, config)
        CodeFormat.PLAIN_TEXT -> generateKeyValueConfig(bitTemplates, config)
        else -> ""
    }
}

private fun generateJsonConfig(bitTemplates: Array<BitSpecific>, config: ExportConfig): String {
    val header = if (config.includeComments) {
        """# ISO8583 JSON Format Configuration
# Generated on: ${LocalDateTime.now()}
# Total fields: ${bitTemplates.size}
# 
# This configuration maps ISO8583 fields to JSON structure
# Modify the 'nestedKey' values to change the JSON output structure
# key: key at root
# nestedKey: for key in nesting state depth by dot(.)

"""
    } else ""

    val fieldMappings = bitTemplates.joinToString(",\n") { bit ->
        val fieldNum = bit.bitNumber.toInt().absoluteValue
        val indent = if (config.prettyFormat) "  " else ""
        val comment = if (config.includeComments) " # ${bit.description}" else ""

        """$indent$fieldNum: 
${indent}  key: "field$fieldNum" $comment"""
    }

    val yamlContent = """formatType: JSON
mti:
  key: header.messageType
tpdu:
  key: "tpduHeader"
other:
  - item :
      key : "createdAt" #if at root
      value : "[TIME]"
  - item :
      nestedKey: "userdata.createdAt" # if nested key
      value : "[TIME]"
  - item :
      header: "createdAt" #if key is in header
      value : "[TIME]"
fieldMappings:
$fieldMappings
settings:
  prettyPrint: ${config.prettyFormat}
  encoding: UTF-8"""

    return header + yamlContent
}

private fun generateXmlConfig(bitTemplates: Array<BitSpecific>, config: ExportConfig): String {
    val header = if (config.includeComments) {
        """# ISO8583 XML Format Configuration
# Generated on: ${LocalDateTime.now()}
# 
# This configuration maps ISO8583 fields to XML elements
# Modify the 'key' or 'nestedKey' values to change XML structure
#

"""
    } else ""

    val fieldMappings = bitTemplates.joinToString(",\n") { bit ->
        val fieldNum = bit.bitNumber.toInt().absoluteValue
        val indent = if (config.prettyFormat) "  " else ""

        """$indent"$fieldNum": {
${indent}  "key": "field$fieldNum"
${indent}}"""
    }

    val yamlContent = """formatType: XML
mti:
  key: messageType
tpdu:
  key: "tpduHeader"
other:
  - item :
      key : "createdAt" #if at root
      value : "[TIME]"
  - item :
      nestedKey: "userdata.createdAt" # if nested key
      value : "[TIME]"
  - item :
      header: "createdAt" #if key is in header
      value : "[TIME]"
fieldMappings:
$fieldMappings
settings:
  rootElement: iso8583
  prettyPrint: ${config.prettyFormat}"""

    return header + yamlContent
}

private fun generateKeyValueConfig(bitTemplates: Array<BitSpecific>, config: ExportConfig): String {
    val header = if (config.includeComments) {
        """# ISO8583 Key-Value Format Configuration
# Generated on: ${LocalDateTime.now()}
# 
# This configuration maps ISO8583 fields to key-value pairs
# Output format: KEY1=VALUE1|KEY2=VALUE2|...
#

"""
    } else ""

    val fieldMappings = bitTemplates.joinToString(",\n") { bit ->
        val fieldNum = bit.bitNumber.toInt().absoluteValue
        val indent = if (config.prettyFormat) "  " else ""

        """$indent"$fieldNum": {
${indent}  "key": "F$fieldNum"
${indent}}"""
    }

    val yamlContent = """formatType: KEY_VALUE
mti:
  key: MTI
tpdu:
  key: "tpduHeader"
other:
  - item :
      key : "createdAt" #if at root
      value : "[TIME]"
  - item :
      nestedKey: "userdata.createdAt" # if nested key
      value : "[TIME]"
  - item :
      header: "createdAt" #if key is in header
      value : "[TIME]"
fieldMappings:
$fieldMappings
settings:
  delimiter: "|"
  keyValueSeparator: "=" """

    return header + yamlContent
}

private suspend fun performExport(config: ExportConfig, bitTemplates: Array<BitSpecific>): String {
    val content = generateExportContent(config, bitTemplates)
    val fileName = "${config.fileName}.${getFileExtension(config.format)}"
    val filePath = File(config.exportPath, fileName)

    try {
        filePath.writeText(content)
        return filePath.absolutePath
    } catch (e: Exception) {
        throw Exception("Failed to write file: ${e.message}")
    }
}

private fun getFileExtension(format: CodeFormat): String {
    return when (format) {
        CodeFormat.JSON, CodeFormat.XML, CodeFormat.PLAIN_TEXT -> "yaml"
        CodeFormat.BYTE_ARRAY, CodeFormat.HEX -> "txt"
    }
}

private fun selectDirectory(): String? {
    return try {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Export Directory"
            currentDirectory = File(System.getProperty("user.home"))
        }

        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile.absolutePath
        } else {
            null
        }
    } catch (e: Exception) {
        println("Error selecting directory: ${e.message}")
        null
    }
}

/**
 * Preview composable for testing
 */
@Preview
@Composable
fun FileExportComponentPreview() {
    MaterialTheme {
        // Sample data for preview
        val sampleBitTemplates = arrayOf(
            BitSpecific().apply {
                bitNumber = 2
                description = "Primary Account Number"
            },
            BitSpecific().apply {
                bitNumber = 3
                description = "Processing Code"
            }
        )

        FileExportComponent(
            bitTemplates = sampleBitTemplates,
            modifier = Modifier.fillMaxSize()
        )
    }
}