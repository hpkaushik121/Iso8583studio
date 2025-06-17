package `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms

import ai.cortex.core.crypto.data.FieldValidation
import ai.cortex.core.crypto.data.ValidationState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.with
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

object AS2805MACValidationUtils {
    fun validateHex(value: String, fieldName: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$fieldName cannot be empty.")

        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
        }
        if (value.length % 2 != 0) {
            return FieldValidation(ValidationState.ERROR, "$fieldName must have an even number of characters.")
        }
        return FieldValidation(ValidationState.VALID)
    }

    fun validateNumeric(value: String, fieldName: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$fieldName cannot be empty.")
        if (value.any { !it.isDigit() }) return FieldValidation(ValidationState.ERROR, "$fieldName must be numeric.")
        val intValue = value.toIntOrNull()
        if (intValue == null || intValue <= 0) return FieldValidation(ValidationState.ERROR, "$fieldName must be a positive number.")
        if (intValue > 16) return FieldValidation(ValidationState.ERROR, "$fieldName cannot be greater than 16.")

        return FieldValidation(ValidationState.VALID)
    }
}


// --- AS2805 MAC SCREEN ---

object AS2805MACLogManager {
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

object AS2805MACService {
    // NOTE: This is a placeholder for actual AS2805 MAC cryptographic logic.
    fun generateMac(keyKL: String, keyKR: String, data: String, algorithm: String, truncation: Int): String {
        // Mock logic: Combine keys based on algorithm, hash with data, and truncate.
        val combinedInput = buildString {
            append(keyKL)
            if (algorithm.contains("Method 2") && keyKR.isNotBlank()) {
                append(keyKR)
            }
            append(data)
        }

        // Simple hash for demonstration
        val fullMac = combinedInput.hashCode().toString(16).uppercase()
        return fullMac.take(truncation)
    }
}

@Composable
fun AS2805MACScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "AS2805.4.1 MAC Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
            ) {
                AS2805MacGenerationCard()
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { AS2805MACLogManager.clearLogs() },
                        logEntries = AS2805MACLogManager.logEntries
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AS2805MacGenerationCard() {
    // --- State Management ---
    val macAlgorithms = remember { listOf("AS2805.4.1 MAC Method 1", "AS2805.4.1 MAC Method 2") }
    var selectedAlgorithm by remember { mutableStateOf(macAlgorithms.first()) }

    var keyKL by remember { mutableStateOf("") }
    var keyKR by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }
    var truncation by remember { mutableStateOf("8") }

    var isLoading by remember { mutableStateOf(false) }

    // --- Dynamic Field Visibility ---
    val showKeyKR by remember(selectedAlgorithm) {
        derivedStateOf { selectedAlgorithm.contains("Method 2") }
    }

    // --- Validation ---
    val keyKLValidation = AS2805MACValidationUtils.validateHex(keyKL, "Key (K|KL)")
    val keyKRValidation = AS2805MACValidationUtils.validateHex(keyKR, "Key (KR)")
    val dataValidation = AS2805MACValidationUtils.validateHex(data, "Data")
    val truncationValidation = AS2805MACValidationUtils.validateNumeric(truncation, "Truncation Length")

    val isFormValid = remember(keyKL, keyKR, data, truncation, showKeyKR) {
        keyKL.isNotBlank() && data.isNotBlank() && truncation.isNotBlank() &&
                keyKLValidation.state != ValidationState.ERROR &&
                dataValidation.state != ValidationState.ERROR &&
                truncationValidation.state != ValidationState.ERROR &&
                (!showKeyKR || (keyKR.isNotBlank() && keyKRValidation.state != ValidationState.ERROR))
    }

    ModernCryptoCard(
        title = "AS2805 MAC Generation",
        subtitle = "Generate an Australian Standard MAC",
        icon = Icons.Default.VerifiedUser
    ) {
        ModernDropdownField("MAC Algorithm", selectedAlgorithm, macAlgorithms) {
            selectedAlgorithm = macAlgorithms[it]
        }
        Spacer(Modifier.height(12.dp))

        EnhancedTextField(keyKL, { keyKL = it }, "Key (K|KL)", validation = keyKLValidation)
        Spacer(Modifier.height(12.dp))

        // Dynamic Key (KR) field
        AnimatedVisibility(
            visible = showKeyKR,
            enter = fadeIn(tween(300)) + expandVertically(animationSpec = tween(300)),
            exit = fadeOut(tween(300)) + shrinkVertically(animationSpec = tween(300))
        ) {
            Column {
                EnhancedTextField(keyKR, { keyKR = it }, "Key (KR)", validation = keyKRValidation)
                Spacer(Modifier.height(12.dp))
            }
        }

        EnhancedTextField(data, { data = it }, "Data (Hex)", validation = dataValidation, maxLines = 5)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(truncation, { truncation = it }, "Truncation Length (Chars)", validation = truncationValidation)
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Generate MAC",
            onClick = {
                isLoading = true
                val inputs = mapOf(
                    "MAC Algorithm" to selectedAlgorithm,
                    "Key (K|KL)" to keyKL,
                    "Key (KR)" to if (showKeyKR) keyKR else "",
                    "Data" to data,
                    "Truncation" to truncation
                )
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = AS2805MACService.generateMac(keyKL, keyKR, data, selectedAlgorithm, truncation.toInt())
                        AS2805MACLogManager.logOperation("MAC Generation", inputs, "Generated MAC: $result", executionTime = 155)
                    } catch (e: Exception) {
                        AS2805MACLogManager.logOperation("MAC Generation", inputs, error = e.message, executionTime = 155)
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ModernButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled && !isLoading, elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)) {
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "AS2805MACButtonAnimation") { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon?.let {
                        Icon(imageVector = it, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
