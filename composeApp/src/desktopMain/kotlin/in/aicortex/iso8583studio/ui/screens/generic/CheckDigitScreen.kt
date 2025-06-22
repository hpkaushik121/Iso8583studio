package `in`.aicortex.iso8583studio.ui.screens.generic

import `in`.aicortex.iso8583studio.data.model.FieldValidation
import `in`.aicortex.iso8583studio.data.model.ValidationState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
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


object CheckDigitValidationUtils {
    fun validateNumeric(value: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY)
        if (value.any { !it.isDigit() }) {
            return FieldValidation(ValidationState.ERROR, "Input must only contain digits (0-9).")
        }
        return FieldValidation(ValidationState.VALID)
    }
}

// --- CHECK DIGIT SCREEN ---

object CheckDigitLogManager {
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
            inputs.forEach { (key, value) -> append("  $key: $value\n") }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

object CheckDigitService {
    fun calculateLuhn(number: String): Int {
        val digits = number.map { it.toString().toInt() }.reversed()
        val sum = digits.mapIndexed { index, digit ->
            if (index % 2 == 0) { // Double every second digit from the right
                val doubled = digit * 2
                if (doubled > 9) doubled - 9 else doubled
            } else {
                digit
            }
        }.sum()
        val mod10 = sum % 10
        return if (mod10 == 0) 0 else 10 - mod10
    }

    fun validateLuhn(number: String): Boolean {
        if (number.length < 2) return false
        val numberWithoutCheckDigit = number.dropLast(1)
        val checkDigit = number.last().toString().toInt()
        return calculateLuhn(numberWithoutCheckDigit) == checkDigit
    }

    fun calculateMod9(number: String): Int {
        val sum = number.map { it.toString().toInt() }.sum()
        val checkDigit = 9 - (sum % 9)
        return if (checkDigit == 9) 0 else checkDigit
    }

    fun validateMod9(number: String): Boolean {
        if (number.length < 2) return false
        val numberWithoutCheckDigit = number.dropLast(1)
        val checkDigit = number.last().toString().toInt()
        return calculateMod9(numberWithoutCheckDigit) == checkDigit
    }
}


@Composable
fun CheckDigitScreen( onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "Check Digit Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left Panel: Input
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                CheckDigitCalculatorCard()
            }
            // Right Panel: Logs
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { CheckDigitLogManager.clearLogs() },
                        logEntries = CheckDigitLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckDigitCalculatorCard() {
    val algorithms = remember { listOf("Luhn (Mod 10)", "Amex SE (Mod 9)") }
    var selectedAlgorithm by remember { mutableStateOf(algorithms.first()) }
    var data by remember { mutableStateOf("49927398716") }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(
            title = "Check Digit Algorithms",
            onDismissRequest = { showInfoDialog = false }
        ) {
            Text("A check digit is a form of redundancy check used for error detection on identification numbers.", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("Luhn (Mod 10):", fontWeight = FontWeight.Bold)
            Text("A widely used algorithm for validating credit card numbers, IMEI numbers, and more. It works by summing digits in a specific way and calculating a final value modulo 10.", style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(8.dp))
            Text("Amex SE (Mod 9):", fontWeight = FontWeight.Bold)
            Text("A less common algorithm. This implementation uses a simple Modulo 9 check for demonstration.", style = MaterialTheme.typography.caption)
        }
    }

    val validation = CheckDigitValidationUtils.validateNumeric(data)
    val isFormValid = validation.state == ValidationState.VALID

    ModernCryptoCard(
        title = "Check Digit Calculator",
        subtitle = "Generate or validate a check digit",
        icon = Icons.Default.Verified,
        onInfoClick = { showInfoDialog = true }
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(
                value = data,
                onValueChange = { data = it },
                label = "Input Number",
                validation = validation,
                maxLines = 1
            )

            ModernDropdownField(
                label = "Algorithm",
                value = selectedAlgorithm,
                options = algorithms,
                onSelectionChanged = { index -> selectedAlgorithm = algorithms[index] }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(
                    text = "Validate",
                    onClick = {
                        isLoading = true
                        val inputs = mapOf("Algorithm" to selectedAlgorithm, "Number" to data)
                        GlobalScope.launch {
                            delay(100)
                            val isValid = if(selectedAlgorithm == "Luhn (Mod 10)") CheckDigitService.validateLuhn(data) else CheckDigitService.validateMod9(data)
                            val result = if(isValid) "Check digit is VALID." else "Check digit is INVALID."
                            CheckDigitLogManager.logOperation("Validation", inputs, result = result, executionTime = 105)
                            isLoading = false
                        }
                    },
                    isLoading = isLoading,
                    enabled = isFormValid,
                    icon = Icons.Default.CheckCircleOutline,
                    modifier = Modifier.weight(1f)
                )

                ModernButton(
                    text = "Generate",
                    onClick = {
                        isLoading = true
                        val baseNumber = if (data.length > 1) data.dropLast(1) else data
                        val inputs = mapOf("Algorithm" to selectedAlgorithm, "Base Number" to baseNumber)
                        GlobalScope.launch {
                            delay(100)
                            val checkDigit = if(selectedAlgorithm == "Luhn (Mod 10)") CheckDigitService.calculateLuhn(baseNumber) else CheckDigitService.calculateMod9(baseNumber)
                            val result = "Generated Check Digit: $checkDigit\nFull Number: $baseNumber$checkDigit"
                            CheckDigitLogManager.logOperation("Generation", inputs, result = result, executionTime = 102)
                            isLoading = false
                        }
                    },
                    isLoading = isLoading,
                    enabled = data.isNotEmpty() && validation.state != ValidationState.ERROR,
                    icon = Icons.Default.Add,
                    modifier = Modifier.weight(1f)
                )
            }
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
private fun ModernCryptoCard(title: String, subtitle: String, icon: ImageVector, onInfoClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                }
                onInfoClick?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Info, contentDescription = "Information", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ModernDropdownField(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), readOnly = true,
            trailingIcon = { Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expanded = !expanded }) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.wrapContentWidth()) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = {
                    onSelectionChanged(index)
                    expanded = false
                }) {
                    Text(text = option, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ModernButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled && !isLoading, elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)) {
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "CheckDigitButtonAnimation") { loading ->
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

@Composable
private fun InfoDialog(title: String, onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colors.primary)
                Spacer(Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            SelectionContainer {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    content()
                }
            }
        },
        confirmButton = { Button(onClick = onDismissRequest) { Text("OK") } },
        shape = RoundedCornerShape(12.dp)
    )
}
