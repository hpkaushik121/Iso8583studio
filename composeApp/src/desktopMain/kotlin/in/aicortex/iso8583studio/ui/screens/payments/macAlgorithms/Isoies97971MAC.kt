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

object ISO9797ValidationUtils {
    fun validateHex(value: String, fieldName: String, expectedLength: Int? = null): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$fieldName cannot be empty.")

        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
        }
        if (value.length % 2 != 0) {
            return FieldValidation(ValidationState.ERROR, "$fieldName must have an even number of characters.")
        }
        expectedLength?.let {
            if (value.length != it) return FieldValidation(ValidationState.ERROR, "$fieldName must be $it characters long.")
        }
        return FieldValidation(ValidationState.VALID)
    }

    fun validateNumeric(value: String, fieldName: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$fieldName cannot be empty.")
        if(value.any { !it.isDigit() }) return FieldValidation(ValidationState.ERROR, "$fieldName must be numeric.")
        if(value.toIntOrNull() == null || value.toInt() <= 0) return FieldValidation(ValidationState.ERROR, "$fieldName must be a positive number.")

        return FieldValidation(ValidationState.VALID)
    }
}


// --- ISO 9797-1 MAC SCREEN ---

object ISO9797LogManager {
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

object ISO9797Service {
    // NOTE: This is a placeholder for actual ISO 9797-1 cryptographic logic.
    fun generateMac(keys: Map<String, String>, data: String, truncation: Int): String {
        // Mock logic: Combine all non-empty keys and data, hash, and truncate.
        val combinedInput = buildString {
            keys.forEach { (name, value) ->
                if (value.isNotBlank()) append(value)
            }
            append(data)
        }

        val fullMac = combinedInput.hashCode().toString(16).uppercase()
        return fullMac.take(truncation)
    }
}

@Composable
fun ISO9797Screen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "ISO/IEC 9797-1 MAC Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
            ) {
                MacGenerationCard()
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { ISO9797LogManager.clearLogs() },
                        logEntries = ISO9797LogManager.logEntries
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MacGenerationCard() {
    // --- State Management ---
    val macAlgorithms = remember { (1..6).map { "Algorithm $it" } }
    var selectedAlgorithm by remember { mutableStateOf(macAlgorithms.first()) }
    val selectedAlgoNumber = remember(selectedAlgorithm) { selectedAlgorithm.substringAfter(" ").toInt() }

    val paddingMethods = remember { (1..3).map { "Method $it" } }
    var selectedPadding by remember { mutableStateOf(paddingMethods.first()) }

    var keyK1 by remember { mutableStateOf("") }
    var keyK1Prime by remember { mutableStateOf("") }
    var key2K by remember { mutableStateOf("") }
    var key2K1 by remember { mutableStateOf("") }
    var key2K1Prime by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }
    var truncation by remember { mutableStateOf("8") }

    var isLoading by remember { mutableStateOf(false) }

    // --- Dynamic Field Visibility ---
    val showK1 by remember(selectedAlgoNumber) { derivedStateOf { selectedAlgoNumber in 1..6 } }
    val showK1Prime by remember(selectedAlgoNumber) { derivedStateOf { selectedAlgoNumber in 2..6 } }
    val showKey2K by remember(selectedAlgoNumber) { derivedStateOf { selectedAlgoNumber == 3 } }
    val showKey2K1 by remember(selectedAlgoNumber) { derivedStateOf { selectedAlgoNumber in 4..6 } }
    val showKey2K1Prime by remember(selectedAlgoNumber) { derivedStateOf { selectedAlgoNumber in 5..6 } }

    // --- Validation ---
    val keyK1Validation = ISO9797ValidationUtils.validateHex(keyK1, "Key (K')")
    val keyK1PrimeValidation = ISO9797ValidationUtils.validateHex(keyK1Prime, "Key (K'')")
    val key2KValidation = ISO9797ValidationUtils.validateHex(key2K, "Key 2 (K)")
    val key2K1Validation = ISO9797ValidationUtils.validateHex(key2K1, "Key 2 (K')")
    val key2K1PrimeValidation = ISO9797ValidationUtils.validateHex(key2K1Prime, "Key 2 (K'')")
    val dataValidation = ISO9797ValidationUtils.validateHex(data, "Data")
    val truncationValidation = ISO9797ValidationUtils.validateNumeric(truncation, "Truncation")

    val isFormValid = remember(
        keyK1, keyK1Prime, key2K, key2K1, key2K1Prime, data, truncation,
        showK1, showK1Prime, showKey2K, showKey2K1, showKey2K1Prime
    ) {
        data.isNotBlank() && truncation.isNotBlank() &&
                truncationValidation.state != ValidationState.ERROR &&
                dataValidation.state != ValidationState.ERROR &&
                (!showK1 || (keyK1.isNotBlank() && keyK1Validation.state != ValidationState.ERROR)) &&
                (!showK1Prime || (keyK1Prime.isNotBlank() && keyK1PrimeValidation.state != ValidationState.ERROR)) &&
                (!showKey2K || (key2K.isNotBlank() && key2KValidation.state != ValidationState.ERROR)) &&
                (!showKey2K1 || (key2K1.isNotBlank() && key2K1Validation.state != ValidationState.ERROR)) &&
                (!showKey2K1Prime || (key2K1Prime.isNotBlank() && key2K1PrimeValidation.state != ValidationState.ERROR))
    }

    ModernCryptoCard(title = "ISO/IEC 9797-1 MAC", subtitle = "Generate a Message Authentication Code", icon = Icons.Default.VerifiedUser) {
        ModernDropdownField("MAC Algorithm", selectedAlgorithm, macAlgorithms) { selectedAlgorithm = macAlgorithms[it] }
        Spacer(Modifier.height(12.dp))

        // --- Dynamic Key Fields ---
        AnimatedField(visible = showK1) { EnhancedTextField(keyK1, { keyK1 = it }, "Key (K')", validation = keyK1Validation) }
        AnimatedField(visible = showK1Prime) { EnhancedTextField(keyK1Prime, { keyK1Prime = it }, "Key (K'')", validation = keyK1PrimeValidation) }
        AnimatedField(visible = showKey2K) { EnhancedTextField(key2K, { key2K = it }, "Key 2 (K)", validation = key2KValidation) }
        AnimatedField(visible = showKey2K1) { EnhancedTextField(key2K1, { key2K1 = it }, "Key 2 (K')", validation = key2K1Validation) }
        AnimatedField(visible = showKey2K1Prime) { EnhancedTextField(key2K1Prime, { key2K1Prime = it }, "Key 2 (K'')", validation = key2K1PrimeValidation) }

        ModernDropdownField("Padding", selectedPadding, paddingMethods) { selectedPadding = paddingMethods[it] }
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
                    "MAC Algorithm" to selectedAlgorithm,
                    "Padding" to selectedPadding,
                    "Key (K')" to keyK1,
                    "Key (K'')" to keyK1Prime,
                    "Key 2 (K)" to key2K,
                    "Key 2 (K')" to key2K1,
                    "Key 2 (K'')" to key2K1Prime,
                    "Data" to data,
                    "Truncation" to truncation
                )
                val keysForService = inputs.filterKeys { it.startsWith("Key") }
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = ISO9797Service.generateMac(keysForService, data, truncation.toInt())
                        ISO9797LogManager.logOperation("MAC Generation", inputs, "Generated MAC: $result", executionTime = 155)
                    } catch (e: Exception) {
                        ISO9797LogManager.logOperation("MAC Generation", inputs, error = e.message, executionTime = 155)
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

@Composable
private fun AnimatedField(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + expandVertically(animationSpec = tween(300)),
        exit = fadeOut(tween(300)) + shrinkVertically(animationSpec = tween(300))
    ) {
        Column {
            content()
            Spacer(Modifier.height(12.dp))
        }
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "ISO9797ButtonAnimation") { loading ->
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
