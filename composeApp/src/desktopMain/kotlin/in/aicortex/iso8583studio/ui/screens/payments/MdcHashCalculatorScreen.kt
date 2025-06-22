package `in`.aicortex.iso8583studio.ui.screens.payments

import `in`.aicortex.iso8583studio.data.model.FieldValidation
import `in`.aicortex.iso8583studio.data.model.ValidationState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---

object MDCHashValidationUtils {
    fun validate(value: String, fieldName: String, inputType: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$fieldName cannot be empty.")

        if (inputType == "Hexadecimal") {
            if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
                return FieldValidation(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
            }
            if (value.length % 2 != 0) {
                return FieldValidation(ValidationState.ERROR, "$fieldName must have an even number of characters.")
            }
        }
        return FieldValidation(ValidationState.VALID)
    }
}


// --- MDC HASH SCREEN ---

object MDCHashLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
    }

    private fun addLog(entry: LogEntry) {
        _logEntries.add(0, entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, _logEntries.size)
    }

    fun logOperation(operation: String, inputs: Map<String, String>, result: String? = null, error: String? = null, executionTime: Long = 0L) {
        if (result == null && error == null) return

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                if (value.isNotBlank()) append("  $key: $value\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

object MDCHashService {
    // NOTE: This is a placeholder for actual MDC Hash cryptographic logic.
    fun generateMdcHash(
        hashType: String,
        padding: String,
        data: String,
        isModified: Boolean
    ): String {
        // Mock logic: Combine all inputs, hash, and return a portion.
        val combinedInput = "$hashType|$padding|$data|$isModified"

        val digest = MessageDigest.getInstance("SHA-256").digest(combinedInput.toByteArray())
        return digest.toHex().uppercase()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}

@Composable
fun MDCHashScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "MDC Hash Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
            ) {
                MDCHashGenerationCard()
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { MDCHashLogManager.clearLogs() },
                        logEntries = MDCHashLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun MDCHashGenerationCard() {
    // --- State Management ---
    val algorithm = "DES"
    val dataInputTypes = remember { listOf("ASCII", "Hexadecimal") }
    var selectedDataInputType by remember { mutableStateOf(dataInputTypes.first()) }

    val hashTypes = remember { listOf("MDC-1", "MDC-2", "MDC-4") }
    var selectedHashType by remember { mutableStateOf(hashTypes.first()) }

    val paddingMethods = remember {
        listOf(
            "None", "Zeros", "Spaces", "ANSI X9.23", "ISO 10126", "PKCS#5", "PKCS#7",
            "ISO 7816-4", "Rijndael", "ISO9797-1 (Padding method 1)", "ISO9797-1 (Padding method 2)",
            "ISO9797-1 (Padding method 3)", "ISO9807 (SafeNet)", "Mod. ANSI X9.23 (n 0xFF + 0xLL)"
        )
    }
    var selectedPadding by remember { mutableStateOf(paddingMethods.first()) }

    var inputData by remember { mutableStateOf("") }
    var isModified by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // --- Validation ---
    val dataValidation = MDCHashValidationUtils.validate(inputData, "Input Data", selectedDataInputType)

    val isFormValid = remember(inputData, dataValidation) {
        inputData.isNotBlank() && dataValidation.state != ValidationState.ERROR
    }

    ModernCryptoCard(
        title = "MDC Hash Generation",
        subtitle = "Generate a Modification Detection Code",
        icon = Icons.Default.Password
    ) {
        OutlinedTextField(
            value = algorithm,
            onValueChange = {},
            label = { Text("Algorithm") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true
        )
        Spacer(Modifier.height(12.dp))

        ModernDropdownField("Data Input", selectedDataInputType, dataInputTypes) {
            selectedDataInputType = dataInputTypes[it]
        }
        Spacer(Modifier.height(12.dp))

        ModernDropdownField("Hash Type", selectedHashType, hashTypes) {
            selectedHashType = hashTypes[it]
        }
        Spacer(Modifier.height(12.dp))

        EnhancedTextField(inputData, { inputData = it }, "Input Data", validation = dataValidation, maxLines = 5)
        Spacer(Modifier.height(12.dp))

        ModernDropdownField("Padding", selectedPadding, paddingMethods) {
            selectedPadding = paddingMethods[it]
        }
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { isModified = !isModified }
        ) {
            Checkbox(checked = isModified, onCheckedChange = { isModified = it })
            Text("Modified?", style = MaterialTheme.typography.body1)
        }
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Generate Hash",
            onClick = {
                isLoading = true
                val inputs = mapOf(
                    "Algorithm" to algorithm,
                    "Data Input" to selectedDataInputType,
                    "Hash Type" to selectedHashType,
                    "Input Data" to inputData,
                    "Padding" to selectedPadding,
                    "Modified?" to isModified.toString()
                )
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = MDCHashService.generateMdcHash(selectedHashType, selectedPadding, inputData, isModified)
                        MDCHashLogManager.logOperation("MDC Hash Generation", inputs, "Generated Hash: $result", executionTime = 155)
                    } catch (e: Exception) {
                        MDCHashLogManager.logOperation("MDC Hash Generation", inputs, error = e.message, executionTime = 155)
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading,
            enabled = isFormValid,
            icon = Icons.Default.CheckCircle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- SHARED UI COMPONENTS ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: FieldValidation) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), maxLines = maxLines,
            isError = validation.state == ValidationState.ERROR,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.primary
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colors.error
                },
                unfocusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colors.error
                }
            )
        )
        if (validation.message.isNotEmpty()) {
            Text(
                text = validation.message,
                color = if (validation.state == ValidationState.ERROR) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun ModernCryptoCard(title: String, subtitle: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f), maxLines = 1)
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ModernDropdownField(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), readOnly = true,
            trailingIcon = { Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expanded = !expanded }) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = {
                    onSelectionChanged(index)
                    expanded = false
                }) {
                    Text(text = option)
                }
            }
        }
    }
}

@Composable
private fun ModernButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled && !isLoading, elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Processing...")
            } else {
                icon?.let {
                    Icon(imageVector = it, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, fontWeight = FontWeight.Medium)
            }
        }
    }
}
