package `in`.aicortex.iso8583studio.ui.screens.cipher

import `in`.aicortex.iso8583studio.data.model.FieldValidation
import `in`.aicortex.iso8583studio.data.model.ValidationState
import androidx.compose.animation.*
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
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---


private object DesValidationUtils {
    fun validateHex(value: String, expectedBytes: Int? = null): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY)
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        if (value.length % 2 != 0) {
            return FieldValidation(ValidationState.ERROR, "Hex string must have an even number of characters.")
        }
        expectedBytes?.let {
            if (value.length != it * 2) {
                return FieldValidation(ValidationState.ERROR, "Key must be exactly ${it * 2} hex characters ($it bytes).")
            }
        }
        return FieldValidation(ValidationState.VALID, helperText = "${value.length / 2} bytes")
    }
}

// --- DES SCREEN ---

private object DesLogManager {
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
                val displayValue = if (key.contains("Key", ignoreCase = true) || value.length > 200) "${value.take(32)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

private object DesCryptoService {
    // This is a mock service. A real implementation would be more robust and use a proper crypto library.
    fun process(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray?,
        mode: String,
        padding: String,
        operation: Int // Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
    ): String {
        val prefix = if(operation == Cipher.ENCRYPT_MODE) "ENCRYPTED" else "DECRYPTED"
        val mockData = MessageDigest.getInstance("SHA-1").digest(data + key + (iv ?: byteArrayOf()))
        return "$prefix($mode/$padding): ${mockData.joinToString("") { "%02X".format(it) }}"
    }
}

@Composable
fun DesCalculatorScreen( onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "DES/3DES Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left Panel: Input
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                DesCalculatorCard()
            }
            // Right Panel: Logs
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { DesLogManager.clearLogs() },
                        logEntries = DesLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun DesCalculatorCard() {
    val desTypes = remember { listOf("DES", "3DES") }
    val modes = remember { listOf("ECB", "CBC", "CFB-8", "CFB-64", "OFB-8", "OFB-64") }
    val paddings = remember { listOf("None", "Zeros", "Spaces", "ANSI X9.23", "ISO 10126", "PKCS#5", "PKCS#7", "ISO7816-4", "Rijndael", "ISO9797-1 (Method 1)", "ISO9797-1 (Method 2)") }
    val inputTypes = remember { listOf("ASCII", "Hexadecimal") }

    var selectedDesType by remember { mutableStateOf(desTypes.first()) }
    var selectedMode by remember { mutableStateOf(modes.first()) }
    var selectedPadding by remember { mutableStateOf(paddings.first()) }
    var selectedInputType by remember { mutableStateOf(inputTypes.first()) }
    var inputData by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var iv by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }

    val keyBytes = when(selectedDesType) {
        "DES" -> 8
        "3DES" -> 24
        else -> 8
    }

    val showIvField = selectedMode in listOf("CBC", "CFB-8", "CFB-64", "OFB-8", "OFB-64")

    val dataValidation = if (selectedInputType == "Hexadecimal") DesValidationUtils.validateHex(inputData) else FieldValidation(ValidationState.VALID)
    val keyValidation = DesValidationUtils.validateHex(key, keyBytes)
    val ivValidation = if (showIvField) DesValidationUtils.validateHex(iv, 8) else FieldValidation(ValidationState.VALID)

    val isFormValid = inputData.isNotBlank() && key.isNotBlank() &&
            dataValidation.state == ValidationState.VALID &&
            keyValidation.state == ValidationState.VALID &&
            (!showIvField || ivValidation.state == ValidationState.VALID)

    ModernCryptoCard(title = "DES/3DES Calculator", subtitle = "Encrypt or Decrypt data", icon = Icons.Default.Lock) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField(label = "DES Type", value = selectedDesType, options = desTypes, onSelectionChanged = { selectedDesType = desTypes[it] }, modifier = Modifier.weight(1f))
                ModernDropdownField(label = "Mode", value = selectedMode, options = modes, onSelectionChanged = { selectedMode = modes[it] }, modifier = Modifier.weight(1f))
            }
            ModernDropdownField(label = "Padding", value = selectedPadding, options = paddings, onSelectionChanged = { selectedPadding = paddings[it] })
            ModernDropdownField(label = "Data Input Type", value = selectedInputType, options = inputTypes, onSelectionChanged = { selectedInputType = inputTypes[it] })

            EnhancedTextField(value = inputData, onValueChange = { inputData = it }, label = "Input Data", validation = dataValidation, maxLines = 5)
            EnhancedTextField(value = key, onValueChange = { key = it.uppercase() }, label = "Key (Hex)", validation = keyValidation)

            AnimatedVisibility(visible = showIvField) {
                EnhancedTextField(value = iv, onValueChange = { iv = it.uppercase() }, label = "Initial Vector (IV) (Hex)", validation = ivValidation)
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(
                    text = "Encrypt",
                    onClick = { /* Handle Encrypt */ },
                    isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Lock, modifier = Modifier.weight(1f)
                )
                ModernButton(
                    text = "Decrypt",
                    onClick = { /* Handle Decrypt */ },
                    isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.LockOpen, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

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
        } else if (validation.helperText.isNotEmpty()) {
            Text(
                text = validation.helperText,
                color = SuccessGreen,
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(end = 16.dp, top = 4.dp)
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "DesButtonAnimation") { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
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

private fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2
    fillMaxWidth().wrapContentSize(Alignment.BottomStart).offset(x = indicatorOffset).width(indicatorWidth)
}
