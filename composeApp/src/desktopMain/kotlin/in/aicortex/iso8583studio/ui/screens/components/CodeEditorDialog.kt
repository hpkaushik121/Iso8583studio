package `in`.aicortex.iso8583studio.ui.screens.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
import `in`.aicortex.iso8583studio.data.getValue
import `in`.aicortex.iso8583studio.data.model.CodeFormat
import `in`.aicortex.iso8583studio.domain.service.ExecutionProgressCallback
import `in`.aicortex.iso8583studio.domain.service.ExecutionResult
import `in`.aicortex.iso8583studio.domain.service.ISO8583MethodExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

// Execution states for better UX
enum class ExecutionState {
    IDLE, VALIDATING, GENERATING, COMPILING, EXECUTING, COMPLETED, FAILED
}

/**
 * Enhanced code editor dialog with processing animations and better UX
 */
@Composable
fun CodeEditorDialog(
    fields: List<BitAttribute>, messageType: String, tpdu: String,
    onCloseRequest: () -> Unit,
    onApplyChanges: (List<FieldMapping>) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(CodeFormat.BYTE_ARRAY) }
    var fieldMappings by remember { mutableStateOf(extractFieldMappings(fields)) }
    var showFormatHelp by remember { mutableStateOf(false) }
    var methodTemplate by remember { mutableStateOf(getDefaultMethodTemplate(selectedFormat)) }
    var outputContent by remember { mutableStateOf("") }
    var showOutput by remember { mutableStateOf(false) }
    var executionResult by remember { mutableStateOf<ExecutionResult?>(null) }
    var executionState by remember { mutableStateOf(ExecutionState.IDLE) }
    var executionProgress by remember { mutableStateOf("") }

    // Create executor instance
    val executor = remember { ISO8583MethodExecutor() }
    val coroutineScope = rememberCoroutineScope()

    // Update method template when format changes
    LaunchedEffect(selectedFormat) {
        methodTemplate = getDefaultMethodTemplate(selectedFormat)
    }

    DialogWindow(
        onCloseRequest = onCloseRequest,
        title = "ISO8583 Code Editor & Method Executor",
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
                // Header with format selection and field info
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
                        Column {
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
                                    onFormatChange = { selectedFormat = it },
                                    enabled = executionState == ExecutionState.IDLE
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "Active Fields: ${fields.count { it.isSet }} | " +
                                        "Total: ${fields.size} | " +
                                        "Data Size: ${
                                            fields.filter { it.isSet && it.data != null }
                                                .sumOf { it.data!!.size }
                                        } bytes",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
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
                                enabled = executionState == ExecutionState.IDLE,
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

                            OutlinedButton(
                                onClick = onCloseRequest,
                                enabled = executionState == ExecutionState.IDLE
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }

                // Execution Progress Bar (when running)
                AnimatedVisibility(
                    visible = executionState != ExecutionState.IDLE,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    ExecutionProgressBar(
                        executionState = executionState,
                        progressText = executionProgress
                    )
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
                            messageType = messageType,
                            tpdu = tpdu,
                            methodTemplate = methodTemplate,
                            selectedFormat = selectedFormat,
                            onMethodChange = { methodTemplate = it },
                            onRunMethod = {
                                coroutineScope.launch {
                                    try {
                                        // Create progress callback
                                        val progressCallback = object : ExecutionProgressCallback {

                                            override fun onValidating(message: String) {
                                                executionState = ExecutionState.VALIDATING
                                                executionProgress = message
                                            }

                                            override fun onGenerating(message: String) {
                                                executionState = ExecutionState.GENERATING
                                                executionProgress = message
                                            }

                                            override fun onCompiling(message: String) {
                                                executionState = ExecutionState.COMPILING
                                                executionProgress = message
                                            }

                                            override fun onExecuting(message: String) {
                                                executionState = ExecutionState.EXECUTING
                                                executionProgress = message
                                            }

                                            override fun onCompleted(
                                                success: Boolean,
                                                message: String
                                            ) {
                                                executionState = if (success) {
                                                    ExecutionState.COMPLETED
                                                } else {
                                                    ExecutionState.FAILED
                                                }
                                                executionProgress = message

                                                executionState = ExecutionState.IDLE
                                            }
                                        }

                                        executionResult = executor
                                            .setListener(progressCallback).executeMethod(
                                                methodName = methodTemplate.methodName,
                                                methodBody = methodTemplate.methodBody,
                                                format = selectedFormat,
                                                fields = fields,
                                                customKeys = methodTemplate.outputKeys,
                                                messageType = messageType,
                                                tpdu = tpdu
                                            )

                                        outputContent = executionResult?.output ?: ""
                                        showOutput = true

                                    } catch (e: Exception) {
                                        executionState = ExecutionState.FAILED
                                        executionProgress = "Error: ${e.message}"
                                        delay(3000)
                                        executionState = ExecutionState.IDLE
                                    }
                                }
                            },
                            executionState = executionState,
                            fields = fields
                        )
                    }

                    // Right panel - Output viewer (conditional)
                    AnimatedVisibility(
                        visible = showOutput,
                        enter = slideInHorizontally { it } + fadeIn(),
                        exit = slideOutHorizontally { it } + fadeOut()
                    ) {
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
private fun ExecutionProgressBar(
    executionState: ExecutionState,
    progressText: String
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val progressValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        backgroundColor = when (executionState) {
            ExecutionState.COMPLETED -> MaterialTheme.colors.primary.copy(alpha = 0.1f)
            ExecutionState.FAILED -> MaterialTheme.colors.error.copy(alpha = 0.1f)
            else -> MaterialTheme.colors.secondary.copy(alpha = 0.1f)
        },
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated icon based on state
            when (executionState) {
                ExecutionState.VALIDATING -> {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Validating",
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation),
                        tint = MaterialTheme.colors.secondary
                    )
                }

                ExecutionState.GENERATING -> {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Generating",
                        modifier = Modifier
                            .size(24.dp)
                            .scale(
                                1f + 0.2f * kotlin.math.sin(rotation * kotlin.math.PI / 180)
                                    .toFloat()
                            ),
                        tint = MaterialTheme.colors.secondary
                    )
                }

                ExecutionState.COMPILING -> {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Compiling",
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation),
                        tint = MaterialTheme.colors.secondary
                    )
                }

                ExecutionState.EXECUTING -> {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Executing",
                        modifier = Modifier
                            .size(24.dp)
                            .alpha(
                                0.5f + 0.5f * kotlin.math.sin(rotation * kotlin.math.PI / 180)
                                    .toFloat()
                            ),
                        tint = MaterialTheme.colors.secondary
                    )
                }

                ExecutionState.COMPLETED -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colors.primary
                    )
                }

                ExecutionState.FAILED -> {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Failed",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colors.error
                    )
                }

                else -> {}
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = when (executionState) {
                        ExecutionState.COMPLETED -> MaterialTheme.colors.primary
                        ExecutionState.FAILED -> MaterialTheme.colors.error
                        else -> MaterialTheme.colors.onSurface
                    }
                )

                if (executionState in listOf(
                        ExecutionState.VALIDATING,
                        ExecutionState.GENERATING,
                        ExecutionState.COMPILING,
                        ExecutionState.EXECUTING
                    )
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = progressValue,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colors.secondary
                    )
                }
            }

            // Step indicator
            Text(
                text = when (executionState) {
                    ExecutionState.VALIDATING -> "1/4"
                    ExecutionState.GENERATING -> "2/4"
                    ExecutionState.COMPILING -> "3/4"
                    ExecutionState.EXECUTING -> "4/4"
                    ExecutionState.COMPLETED -> "✓"
                    ExecutionState.FAILED -> "✗"
                    else -> ""
                },
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MethodEditorPanel(
    methodTemplate: MethodTemplate,
    selectedFormat: CodeFormat,
    onMethodChange: (MethodTemplate) -> Unit,
    onRunMethod: () -> Unit,
    executionState: ExecutionState,
    fields: List<BitAttribute>,
    messageType: String,
    tpdu: String
) {
    val scope = rememberCoroutineScope()

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
                // Main RUN button with enhanced animation
                Button(
                    onClick = onRunMethod,
                    enabled = executionState == ExecutionState.IDLE,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = when (executionState) {
                            ExecutionState.IDLE -> MaterialTheme.colors.secondary
                            ExecutionState.COMPLETED -> MaterialTheme.colors.primary
                            ExecutionState.FAILED -> MaterialTheme.colors.error
                            else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        }
                    ),
                    modifier = Modifier.animateContentSize()
                ) {
                    when (executionState) {
                        ExecutionState.IDLE -> {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Run Script",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Script")
                        }

                        ExecutionState.COMPLETED -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Completed")
                        }

                        ExecutionState.FAILED -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Failed",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Failed")
                        }

                        else -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colors.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing...")
                        }
                    }
                }

                // Generate File button
                var isGenerating by remember { mutableStateOf(false) }
                var generationResult by remember { mutableStateOf<String?>(null) }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            try {
                                val executor = ISO8583MethodExecutor()
                                val result = executor.generateStandaloneKotlinFile(
                                    methodName = methodTemplate.methodName,
                                    methodBody = methodTemplate.methodBody,
                                    format = selectedFormat,
                                    fields = fields,
                                    outputDir = "generated",
                                    customKeys = methodTemplate.outputKeys,
                                    messageType = messageType,
                                    tpdu = tpdu
                                )

                                generationResult = if (result.success) {
                                    "✓ Kotlin file generated successfully!\n\nFile: ${result.generatedFilePath}\n\nTo compile and run:\n1. cd generated\n2. kotlinc ${methodTemplate.methodName}_*.kt -include-runtime -d ${methodTemplate.methodName}.jar\n3. java -jar ${methodTemplate.methodName}.jar"
                                } else {
                                    "✗ Generation failed: ${result.error}"
                                }
                            } catch (e: Exception) {
                                generationResult = "✗ Error: ${e.message}"
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    enabled = !isGenerating && executionState == ExecutionState.IDLE
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating...")
                    } else {
                        Icon(
                            Icons.Default.GetApp,
                            contentDescription = "Generate File",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate .kt")
                    }
                }

                // Show generation result dialog
                if (generationResult != null) {
                    AlertDialog(
                        onDismissRequest = { generationResult = null },
                        title = { Text("Kotlin File Generation") },
                        text = {
                            Text(generationResult!!)
                        },
                        confirmButton = {
                            Button(onClick = { generationResult = null }) {
                                Text("OK")
                            }
                        }
                    )
                }

                // Reset button
                IconButton(
                    onClick = {
                        onMethodChange(getDefaultMethodTemplate(selectedFormat))
                    },
                    enabled = executionState == ExecutionState.IDLE
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset to default",
                        tint = if (executionState == ExecutionState.IDLE)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
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
            singleLine = true,
            enabled = executionState == ExecutionState.IDLE
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Method Body",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Method body editor with overlay when executing
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.3f)),
                color = Color(0xFF1E1E1E)
            ) {
                BasicTextField(
                    value = methodTemplate.methodBody,
                    onValueChange = {
                        if (executionState == ExecutionState.IDLE) {
                            onMethodChange(methodTemplate.copy(methodBody = it))
                        }
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
                    cursorBrush = SolidColor(Color.White),
                    readOnly = executionState != ExecutionState.IDLE
                )
            }

            // Execution overlay
            if (executionState != ExecutionState.IDLE) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.3f),
                    color = MaterialTheme.colors.surface
                ) {}
            }
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
                "Execution Output",
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

        // Execution metadata with enhanced styling
        executionResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = 2.dp,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (result.success) MaterialTheme.colors.primary else MaterialTheme.colors.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (result.success) "Compilation & Execution Successful" else "Compilation/Execution Failed",
                                style = MaterialTheme.typography.subtitle2,
                                fontWeight = FontWeight.Bold,
                                color = if (result.success) MaterialTheme.colors.primary else MaterialTheme.colors.error
                            )
                        }

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

                    if (result.generatedFilePath != null) {
                        Text(
                            "Generated: ${result.generatedFilePath}",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
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
fun FormatSelector(
    selectedFormat: CodeFormat,
    onFormatChange: (CodeFormat) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = if (enabled) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(
                    alpha = 0.4f
                )
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
        title = { Text("Format Information & Execution Process") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Available Output Formats:",
                    fontWeight = FontWeight.Bold
                )

                FormatHelpItem(
                    "JSON",
                    "JavaScript Object Notation - structured data with actual BitAttribute values"
                )
                FormatHelpItem(
                    "XML",
                    "Extensible Markup Language - hierarchical ISO8583 message structure"
                )
                FormatHelpItem("HEX", "Hexadecimal representation of real field data")
                FormatHelpItem(
                    "BYTE_ARRAY",
                    "Kotlin byte array declarations from BitAttribute data"
                )
                FormatHelpItem("PLAIN_TEXT", "Human-readable report with field analysis")

                Divider()

                Text(
                    "Execution Process:",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "1. Validating - Check method syntax and security\n" +
                            "2. Generating - Create standalone Kotlin script\n" +
                            "3. Compiling - Run kotlinc to create JAR file\n" +
                            "4. Executing - Run java -jar to get output\n\n" +
                            "The process uses your actual BitAttribute data and provides real compilation/execution feedback.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )
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
            try {
                val value = field.getValue() ?: field.getString()
                FieldMapping(
                    bitNumber = index + 1,
                    name = "field_${index + 1}",
                    value = value,
                    description = "Bit ${index + 1} data"
                )
            } catch (e: Exception) {
                FieldMapping(
                    bitNumber = index + 1,
                    name = "field_${index + 1}",
                    value = String(field.data!!),
                    description = "Bit ${index + 1} data (raw)"
                )
            }
        } else null
    }
}

private fun getDefaultMethodTemplate(format: CodeFormat): MethodTemplate {
    val executor = ISO8583MethodExecutor()
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