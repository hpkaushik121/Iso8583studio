package `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms

import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---

object TDESCBCMACValidationUtils {
    fun validateHex(value: String, fieldName: String, expectedLength: Int? = null): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$fieldName cannot be empty.")

        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
        }
        if (value.length % 2 != 0) {
            return ValidationResult(ValidationState.ERROR, "$fieldName must have an even number of characters.")
        }
        expectedLength?.let {
            if (value.length != it) return ValidationResult(ValidationState.ERROR, "$fieldName must be $it characters long.")
        }
        return ValidationResult(ValidationState.VALID)
    }

    fun validateNumeric(value: String, fieldName: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$fieldName cannot be empty.")
        if (value.any { !it.isDigit() }) return ValidationResult(ValidationState.ERROR, "$fieldName must be numeric.")
        val intValue = value.toIntOrNull()
        if (intValue == null || intValue <= 0) return ValidationResult(ValidationState.ERROR, "$fieldName must be a positive number.")
        if (intValue > 16) return ValidationResult(ValidationState.ERROR, "$fieldName cannot be greater than 16.")

        return ValidationResult(ValidationState.VALID)
    }
}


// --- TDES CBC-MAC SCREEN ---

object TDESCBCMACLogManager {
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

object TDESCBCMACService {
    // NOTE: This is a placeholder for actual TDES CBC-MAC cryptographic logic.
    fun generateMac(key: String, padding: String, data: String, truncation: Int): String {
        // Mock logic: Combine key, data, and padding info, hash, and truncate.
        val combinedInput = "$key|$padding|$data"

        // Simple hash for demonstration
        val fullMac = combinedInput.hashCode().toString(16).uppercase()
        return fullMac.take(truncation)
    }
}

@Composable
fun TDESCBCMACScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "TDES CBC-MAC Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
            ) {
                TDESCBCMACGenerationCard()
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { TDESCBCMACLogManager.clearLogs() },
                        logEntries = TDESCBCMACLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun TDESCBCMACGenerationCard() {
    // --- State Management ---
    val macAlgorithm = "TDES CBC-MAC"
    val paddingMethods = remember { listOf("ISO9797-1 (Padding Method 1)", "ISO9797-1 (Padding Method 2)", "ISO9797-1 (Padding Method 3)") }
    var selectedPadding by remember { mutableStateOf(paddingMethods.first()) }

    var key by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }
    var truncation by remember { mutableStateOf("8") }

    var isLoading by remember { mutableStateOf(false) }

    // --- Validation ---
    val keyValidation = TDESCBCMACValidationUtils.validateHex(key, "Key (K)", 32) // TDES key is 16 bytes (32 hex)
    val dataValidation = TDESCBCMACValidationUtils.validateHex(data, "Data")
    val truncationValidation = TDESCBCMACValidationUtils.validateNumeric(truncation, "Truncation Length")

    val isFormValid = remember(key, data, truncation) {
        key.isNotBlank() && data.isNotBlank() && truncation.isNotBlank() &&
                keyValidation.state != ValidationState.ERROR &&
                dataValidation.state != ValidationState.ERROR &&
                truncationValidation.state != ValidationState.ERROR
    }

    ModernCryptoCard(
        title = "TDES CBC-MAC Generation",
        subtitle = "Generate a Triple DES CBC-MAC",
        icon = Icons.Default.VerifiedUser
    ) {
        // Non-editable field for algorithm consistency
        OutlinedTextField(
            value = macAlgorithm,
            onValueChange = {},
            label = { Text("MAC Algorithm") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true
        )
        Spacer(Modifier.height(12.dp))

        EnhancedTextField(key, { key = it }, "Key (K) - 32 Hex Chars", validation = keyValidation)
        Spacer(Modifier.height(12.dp))

        ModernDropdownField("Padding", selectedPadding, paddingMethods) {
            selectedPadding = paddingMethods[it]
        }
        Spacer(Modifier.height(12.dp))

        EnhancedTextField(data, { data = it }, "Data (Hex)", validation = dataValidation, maxLines = 5)
        Spacer(Modifier.height(12.dp))

        EnhancedTextField(truncation, { truncation = it }, "Truncation Length (Chars)", validation = truncationValidation)
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Generate MAC",
            onClick = {
                isLoading = true
                val inputs = mapOf(
                    "MAC Algorithm" to macAlgorithm,
                    "Key (K)" to key,
                    "Padding" to selectedPadding,
                    "Data" to data,
                    "Truncation" to truncation
                )
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = TDESCBCMACService.generateMac(key, selectedPadding, data, truncation.toInt())
                        TDESCBCMACLogManager.logOperation("MAC Generation", inputs, "Generated MAC: $result", executionTime = 155)
                    } catch (e: Exception) {
                        TDESCBCMACLogManager.logOperation("MAC Generation", inputs, error = e.message, executionTime = 155)
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
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult) {
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
