package `in`.aicortex.iso8583studio.ui.screens.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.domain.service.CodeFormat
import `in`.aicortex.iso8583studio.domain.service.ExecutionResult
import `in`.aicortex.iso8583studio.domain.service.ISO8583MethodExecutor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.LocalDateTime

data class FieldMapping(
    val bitNumber: Int,
    val name: String,
    val value: String,
    val description: String = ""
)

data class MethodTemplate(
    val methodName: String,
    val methodBody: String,
    val outputKeys: Map<String, String> = emptyMap() // For JSON key customization
)

/**
 * Advanced code editor dialog for ISO8583 message formatting and field mapping
 */
@Composable
fun CodeEditorDialog(
    fields: List<BitAttribute>,
    onCloseRequest: () -> Unit,
    onApplyChanges: (List<FieldMapping>) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(CodeFormat.JSON) }
    var fieldMappings by remember { mutableStateOf(extractFieldMappings(fields)) }
    var showFormatHelp by remember { mutableStateOf(false) }
    var methodTemplate by remember { mutableStateOf(getDefaultMethodTemplate(selectedFormat)) }
    var outputContent by remember { mutableStateOf("") }
    var showOutput by remember { mutableStateOf(false) }
    var executionResult by remember { mutableStateOf<ExecutionResult?>(null) }
    var isExecuting by remember { mutableStateOf(false) }

    // Create executor instance
    val executor = remember { ISO8583MethodExecutor() }

    // Update method template when format changes
    LaunchedEffect(selectedFormat) {
        methodTemplate = getDefaultMethodTemplate(selectedFormat)
    }

    DialogWindow(
        onCloseRequest = onCloseRequest,
        title = "ISO8583 Code Editor & Field Mapper",
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            width = 1200.dp,
            height = 800.dp
        ),
        resizable = true
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with format selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Output Format:",
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            FormatSelector(
                                selectedFormat = selectedFormat,
                                onFormatChange = { selectedFormat = it }
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { showFormatHelp = true }) {
                                Icon(
                                    Icons.Default.Help,
                                    contentDescription = "Format Help",
                                    tint = MaterialTheme.colors.primary
                                )
                            }

                            Button(
                                onClick = { onApplyChanges(fieldMappings) },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Apply",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apply Changes")
                            }

                            OutlinedButton(onClick = onCloseRequest) {
                                Text("Cancel")
                            }
                        }
                    }
                }

                // Main content area
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left panel - Method editor
                    Card(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight(),
                        elevation = 2.dp
                    ) {
                        MethodEditorPanel(
                            methodTemplate = methodTemplate,
                            selectedFormat = selectedFormat,
                            onMethodChange = { methodTemplate = it },
                            onRunMethod = {
                                isExecuting = true
                                try {
                                    executionResult = executor.executeMethod(
                                        methodName = methodTemplate.methodName,
                                        methodBody = methodTemplate.methodBody,
                                        format = selectedFormat,
                                        fields = fields,
                                        customKeys = methodTemplate.outputKeys
                                    )
                                    outputContent = executionResult?.output ?: ""
                                    showOutput = true
                                } finally {
                                    isExecuting = false
                                }
                            },
                            isExecuting = isExecuting
                        )
                    }

                    // Right panel - Output viewer (conditional)
                    if (showOutput) {
                        Card(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxHeight(),
                            elevation = 2.dp
                        ) {
                            OutputPanel(
                                outputContent = outputContent,
                                format = selectedFormat,
                                executionResult = executionResult,
                                onClose = { showOutput = false }
                            )
                        }
                    }
                }
            }
        }
    }

    // Format help dialog
    if (showFormatHelp) {
        FormatHelpDialog(
            onDismiss = { showFormatHelp = false }
        )
    }
}

@Composable
private fun MethodEditorPanel(
    methodTemplate: MethodTemplate,
    selectedFormat: CodeFormat,
    onMethodChange: (MethodTemplate) -> Unit,
    onRunMethod: () -> Unit,
    isExecuting: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Method Editor",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRunMethod,
                    enabled = !isExecuting,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isExecuting) MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        else MaterialTheme.colors.secondary
                    )
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colors.onSurface
                        )
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Run",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isExecuting) "Running..." else "Run")
                }

                IconButton(
                    onClick = {
                        onMethodChange(getDefaultMethodTemplate(selectedFormat))
                    }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset to default",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Method name editor
        OutlinedTextField(
            value = methodTemplate.methodName,
            onValueChange = {
                onMethodChange(methodTemplate.copy(methodName = it))
            },
            label = { Text("Method Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // JSON Key customization (only for JSON format)
        if (selectedFormat == CodeFormat.JSON) {
            Text(
                "JSON Key Mappings (Output Customization)",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.height(150.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(4) { index ->
                    val keys = listOf("messageType", "timestamp", "fields", "metadata")
                    val currentKey = keys[index]
                    val currentValue = methodTemplate.outputKeys[currentKey] ?: currentKey

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            currentKey,
                            modifier = Modifier.weight(0.4f),
                            style = MaterialTheme.typography.body2
                        )

                        Text("→", style = MaterialTheme.typography.body2)

                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { newValue ->
                                val updatedKeys = methodTemplate.outputKeys.toMutableMap()
                                updatedKeys[currentKey] = newValue
                                onMethodChange(methodTemplate.copy(outputKeys = updatedKeys))
                            },
                            modifier = Modifier.weight(0.6f),
                            textStyle = TextStyle(fontSize = 12.sp),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            "Method Body",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Method body editor
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.3f)),
            color = Color(0xFF1E1E1E)
        ) {
            BasicTextField(
                value = methodTemplate.methodBody,
                onValueChange = {
                    onMethodChange(methodTemplate.copy(methodBody = it))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
                textStyle = TextStyle(
                    color = Color(0xFF9CDCFE),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                ),
                cursorBrush = SolidColor(Color.White)
            )
        }
    }
}

@Composable
private fun OutputPanel(
    outputContent: String,
    format: CodeFormat,
    executionResult: ExecutionResult?,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Output",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        // Copy to clipboard logic
                    }
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colors.primary
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colors.onSurface
                    )
                }
            }
        }

        // Execution metadata
        executionResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = 1.dp,
                backgroundColor = if (result.success)
                    MaterialTheme.colors.primary.copy(alpha = 0.1f)
                else
                    MaterialTheme.colors.error.copy(alpha = 0.1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (result.success) "✓ Execution Successful" else "✗ Execution Failed",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold,
                            color = if (result.success) MaterialTheme.colors.primary else MaterialTheme.colors.error
                        )

                        Text(
                            "${result.executionTimeMs}ms",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    if (!result.success && result.error != null) {
                        Text(
                            result.error,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.error
                        )
                    }

                    Text(
                        "Method: ${result.metadata.methodName} | Fields: ${result.metadata.fieldCount}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.3f)),
            color = Color(0xFF1E1E1E)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text(
                    text = outputContent,
                    color = getTextColor(format),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
private fun FormatSelector(
    selectedFormat: CodeFormat,
    onFormatChange: (CodeFormat) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colors.primary
            )
        ) {
            Text(selectedFormat.name.replace("_", " "))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Select format"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CodeFormat.values().forEach { format ->
                DropdownMenuItem(onClick = {
                    onFormatChange(format)
                    expanded = false
                }) {
                    Text(format.name.replace("_", " "))
                }
            }
        }
    }
}

@Composable
private fun FormatHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Format Information") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Available Output Formats:",
                    fontWeight = FontWeight.Bold
                )

                FormatHelpItem("JSON", "JavaScript Object Notation - structured key-value pairs")
                FormatHelpItem("XML", "Extensible Markup Language - hierarchical structure")
                FormatHelpItem("HEX", "Hexadecimal representation of binary data")
                FormatHelpItem("BYTE_ARRAY", "Raw byte array representation")
                FormatHelpItem("PLAIN_TEXT", "Simple text format with field mappings")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun FormatHelpItem(name: String, description: String) {
    Column {
        Text(
            text = name,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
    }
}

// Utility functions
private fun extractFieldMappings(fields: List<BitAttribute>): List<FieldMapping> {
    return fields.mapIndexedNotNull { index, field ->
        if (field.isSet && field.data != null) {
            FieldMapping(
                bitNumber = index,
                name = "field_${index}",
                value = String(field.data!!),
                description = "Bit ${index} data"
            )
        } else null
    }
}

private fun getDefaultMethodTemplate(format: CodeFormat): MethodTemplate {
    val executor = ISO8583MethodExecutor() // Temporary instance to get sample
    return when (format) {
        CodeFormat.JSON -> MethodTemplate(
            methodName = "processToJson",
            methodBody = executor.createSampleMethod(CodeFormat.JSON),
            outputKeys = mapOf(
                "messageType" to "messageType",
                "timestamp" to "timestamp",
                "fields" to "processedFields",
                "metadata" to "metadata"
            )
        )

        CodeFormat.XML -> MethodTemplate(
            methodName = "processToXml",
            methodBody = executor.createSampleMethod(CodeFormat.XML)
        )

        CodeFormat.HEX -> MethodTemplate(
            methodName = "processToHex",
            methodBody = executor.createSampleMethod(CodeFormat.HEX)
        )

        CodeFormat.BYTE_ARRAY -> MethodTemplate(
            methodName = "processByteArrays",
            methodBody = executor.createSampleMethod(CodeFormat.BYTE_ARRAY)
        )

        CodeFormat.PLAIN_TEXT -> MethodTemplate(
            methodName = "processToText",
            methodBody = executor.createSampleMethod(CodeFormat.PLAIN_TEXT)
        )
    }
}

private fun getTextColor(format: CodeFormat): Color {
    return when (format) {
        CodeFormat.JSON -> Color(0xFF9CDCFE) // Light blue for JSON
        CodeFormat.XML -> Color(0xFF4EC9B0) // Teal for XML
        CodeFormat.HEX -> Color(0xFFD7BA7D) // Yellow for HEX
        CodeFormat.BYTE_ARRAY -> Color(0xFFDCDCAA) // Light yellow for byte arrays
        CodeFormat.PLAIN_TEXT -> Color(0xFFD4D4D4) // Light gray for plain text
    }
}

// Extension function for string multiplication
private operator fun String.times(count: Int): String = this.repeat(count)