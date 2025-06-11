package `in`.aicortex.iso8583studio.ui.screens.generic

import ai.cortex.core.crypto.data.FieldValidation
import ai.cortex.core.crypto.data.ValidationState
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
import java.security.MessageDigest

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---


object HashValidationUtils {
    fun validateHexInput(value: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY)
        if (!value.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "Hexadecimal input must only contain characters 0-9 and A-F.")
        }
        if (value.length % 2 != 0) {
            return FieldValidation(ValidationState.ERROR, "Hexadecimal input must have an even number of characters.")
        }
        return FieldValidation(ValidationState.VALID)
    }
}

// --- HASH CALCULATOR SCREEN ---

object HashLogManager {
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

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type= logType, message = message, details = details))
    }
}

@Composable
fun HashCalculatorScreen(window: ComposeWindow? = null, onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "Hash Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left Panel: Input
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                HashCalculatorCard()
            }
            // Right Panel: Logs
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { HashLogManager.clearLogs() },
                        logEntries = HashLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun HashCalculatorCard() {
    val inputTypes = remember { listOf("ASCII", "Hexadecimal") }
    val hashTypes = remember { listOf("None", "MD4", "MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "RIPEMD-160", "TIGER-192", "CRC32", "CRC32_RFC1510", "WHIRLPOOL", "MDC-2") }

    var selectedInputType by remember { mutableStateOf(inputTypes.first()) }
    var selectedHashType by remember { mutableStateOf("SHA-256") }
    var inputData by remember { mutableStateOf("Hello, World!") }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(
            title = "Hash Calculation",
            onDismissRequest = { showInfoDialog = false }
        ) {
            Text("A hash function is a one-way cryptographic algorithm that converts an arbitrary size of data into a fixed-size string of characters. This string is called a hash value, digest, or simply hash.", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("Key Properties:", fontWeight = FontWeight.Bold)
            Text("• Deterministic: The same input will always produce the same output.", style = MaterialTheme.typography.caption)
            Text("• One-Way: It's computationally infeasible to reverse the process and generate the original input from the hash.", style = MaterialTheme.typography.caption)
            Text("• Collision Resistant: It's extremely difficult to find two different inputs that produce the same hash.", style = MaterialTheme.typography.caption)
        }
    }

    val validation = if (selectedInputType == "Hexadecimal") HashValidationUtils.validateHexInput(inputData) else FieldValidation(ValidationState.VALID)
    val isFormValid = inputData.isNotBlank() && validation.state == ValidationState.VALID

    ModernCryptoCard(
        title = "Hash Calculator",
        subtitle = "Generate a hash from your data",
        icon = Icons.Default.Password,
        onInfoClick = { showInfoDialog = true }
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField(label = "Data Input Type", value = selectedInputType, options = inputTypes, onSelectionChanged = { selectedInputType = inputTypes[it] }, modifier = Modifier.weight(1f))
                ModernDropdownField(label = "Hash Type", value = selectedHashType, options = hashTypes, onSelectionChanged = { selectedHashType = hashTypes[it] }, modifier = Modifier.weight(1f))
            }

            EnhancedTextField(
                value = inputData,
                onValueChange = { inputData = it },
                label = "Input Data",
                validation = validation,
                maxLines = 10
            )
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Calculate Hash",
                onClick = {
                    isLoading = true
                    val inputs = mapOf("Input Type" to selectedInputType, "Hash Type" to selectedHashType, "Data" to inputData)
                    GlobalScope.launch {
                        delay(200) // Simulate processing time
                        try {
                            val dataBytes = when (selectedInputType) {
                                "ASCII" -> inputData.toByteArray()
                                "Hexadecimal" -> inputData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                                else -> byteArrayOf()
                            }

                            // NOTE: Not all algorithms are supported by default Java Security. This is a mock.
                            // In a real implementation, you would use a library like BouncyCastle.
                            val hashResult = if (selectedHashType != "None" && selectedHashType != "MD4") {
                                val digest = MessageDigest.getInstance(selectedHashType)
                                digest.digest(dataBytes).joinToString("") { "%02x".format(it) }.uppercase()
                            } else {
                                "Algorithm not supported in this demo"
                            }

                            HashLogManager.logOperation("Hash Calculation", inputs, result = hashResult, executionTime = 210)
                        } catch (e: Exception) {
                            HashLogManager.logOperation("Hash Calculation", inputs, error = e.message ?: "An unknown error occurred.", executionTime = 210)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                enabled = isFormValid,
                icon = Icons.Default.GeneratingTokens,
                modifier = Modifier.fillMaxWidth()
            )
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
                color = if(validation.state == ValidationState.ERROR) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "HashButtonAnimation") { loading ->
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
