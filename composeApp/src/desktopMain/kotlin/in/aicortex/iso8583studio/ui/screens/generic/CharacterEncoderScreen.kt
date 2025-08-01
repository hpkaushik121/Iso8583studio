package `in`.aicortex.iso8583studio.ui.screens.generic

import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
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


object EncodingValidationUtils {
    fun validate(value: String, type: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY)

        return when (type) {
            "Binary -> Hexadecimal" -> {
                if (value.any { it != '0' && it != '1' }) ValidationResult(ValidationState.ERROR, "Binary input must only contain '0' or '1'.")
                else ValidationResult(ValidationState.VALID)
            }
            "Hexadecimal -> Binary", "Hexadecimal -> ATM ASCII Decimal" -> {
                if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) ValidationResult(ValidationState.ERROR, "Hex input must be valid hexadecimal characters.")
                else if (value.length % 2 != 0) ValidationResult(ValidationState.ERROR, "Hex input must have an even number of characters.")
                else ValidationResult(ValidationState.VALID)
            }
            else -> ValidationResult(ValidationState.VALID) // ASCII, EBCDIC have no strict validation here
        }
    }
}


// --- CHARACTER ENCODING SCREEN ---

object EncodingLogManager {
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
                val displayValue = if (value.length > 200) "${value.take(200)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "Conversion Successful") else (LogType.ERROR to "Conversion Failed")
        addLog( LogEntry(timestamp = timestamp, type= logType, message = message, details = details))
    }
}

object CharacterEncodingService {
    // This is a mock service. A real implementation would use robust libraries.
    fun convert(data: String, conversionType: String): String {
        return when (conversionType) {
            "Binary -> Hexadecimal" -> data.chunked(4).map { it.toInt(2).toString(16) }.joinToString("").uppercase()
            "Hexadecimal -> Binary" -> data.chunked(1).map { it.toInt(16).toString(2).padStart(4, '0') }.joinToString(" ")
            "ASCII -> EBCDIC" -> "EBCDIC conversion is complex and requires a full mapping table. (Mock Result)"
            "EBCDIC -> ASCII" -> "EBCDIC conversion is complex and requires a full mapping table. (Mock Result)"
            "ASCII Text -> Hexadecimal" -> data.map { it.code.toString(16).padStart(2, '0') }.joinToString("").uppercase()
            "ATM ASCII Decimal -> Hexadecimal" -> data.map { it.code.toString(16).padStart(2, '0') }.joinToString("").uppercase()
            "Hexadecimal -> ATM ASCII Decimal" -> data.chunked(2).map { it.toInt(16).toChar() }.joinToString("")
            else -> throw IllegalArgumentException("Unsupported conversion type")
        }
    }
}

@Composable
fun CharacterEncodingScreen( onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "Character Encoding Converter", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left Panel: Input
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                EncodingCard()
            }
            // Right Panel: Logs
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { EncodingLogManager.clearLogs() },
                        logEntries = EncodingLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun EncodingCard() {
    val encodingTypes = remember {
        listOf(
            "Binary -> Hexadecimal", "Hexadecimal -> Binary", "ASCII -> EBCDIC", "EBCDIC -> ASCII",
            "ASCII Text -> Hexadecimal", "ATM ASCII Decimal -> Hexadecimal", "Hexadecimal -> ATM ASCII Decimal"
        )
    }
    var selectedEncoding by remember { mutableStateOf(encodingTypes.first()) }
    var data by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(
            title = "Character Encoding",
            onDismissRequest = { showInfoDialog = false }
        ) {
            Text("This tool converts data between different character encoding schemes commonly used in transaction processing.", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("• Binary/Hexadecimal: Low-level data representations.", style = MaterialTheme.typography.caption)
            Text("• ASCII/EBCDIC: Standard character sets. EBCDIC is primarily used on IBM mainframes.", style = MaterialTheme.typography.caption)
            Text("• ATM ASCII Decimal: A specific ASCII representation for numeric data used by some ATM devices.", style = MaterialTheme.typography.caption)
        }
    }

    val validation = EncodingValidationUtils.validate(data, selectedEncoding)
    val isFormValid = data.isNotBlank() && validation.state != ValidationState.ERROR

    ModernCryptoCard(
        title = "Encoding Converter",
        subtitle = "Convert data between different formats",
        icon = Icons.Default.Transform,
        onInfoClick = { showInfoDialog = true }
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernDropdownField(
                label = "Conversion Type",
                value = selectedEncoding,
                options = encodingTypes,
                onSelectionChanged = { index -> selectedEncoding = encodingTypes[index] }
            )

            EnhancedTextField(
                value = data,
                onValueChange = { data = it },
                label = "Input Data",
                validation = validation,
                maxLines = 10
            )
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Convert",
                onClick = {
                    isLoading = true
                    val inputs = mapOf("Conversion" to selectedEncoding, "Input Data" to data)
                    GlobalScope.launch {
                        delay(200) // Simulate processing time
                        try {
                            val result = CharacterEncodingService.convert(data, selectedEncoding)
                            EncodingLogManager.logOperation("Encoding Conversion", inputs, result = result, executionTime = 210)
                        } catch (e: Exception) {
                            EncodingLogManager.logOperation("Encoding Conversion", inputs, error = e.message ?: "An unknown error occurred.", executionTime = 210)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                enabled = isFormValid,
                icon = Icons.Default.SyncAlt,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "EncodingButtonAnimation") { loading ->
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
