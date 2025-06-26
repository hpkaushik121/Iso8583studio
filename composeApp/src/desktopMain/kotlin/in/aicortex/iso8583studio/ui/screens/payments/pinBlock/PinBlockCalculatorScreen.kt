package `in`.aicortex.iso8583studio.ui.screens.generic

import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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

object PinBlockValidationUtils {
    fun validatePan(pan: String): ValidationResult {
        if (pan.isEmpty()) return ValidationResult(ValidationState.EMPTY, "PAN cannot be empty.")
        if (pan.any { !it.isDigit() }) return ValidationResult(ValidationState.ERROR, "PAN must be numeric.")
        if (pan.length < 12) return ValidationResult(ValidationState.ERROR, "PAN must be at least 12 digits.")
        return ValidationResult(ValidationState.VALID)
    }

    fun validatePin(pin: String): ValidationResult {
        if (pin.isEmpty()) return ValidationResult(ValidationState.EMPTY, "PIN cannot be empty.")
        if (pin.any { !it.isDigit() }) return ValidationResult(ValidationState.ERROR, "PIN must be numeric.")
        if (pin.length < 4 || pin.length > 12) return ValidationResult(ValidationState.ERROR, "PIN must be between 4 and 12 digits.")
        return ValidationResult(ValidationState.VALID)
    }

    fun validatePinBlock(pinBlock: String): ValidationResult {
        if (pinBlock.isEmpty()) return ValidationResult(ValidationState.EMPTY, "PIN Block cannot be empty.")
        if (pinBlock.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "PIN Block must be valid hexadecimal.")
        }
        if (pinBlock.length != 16) return ValidationResult(ValidationState.ERROR, "PIN Block must be 16 hex characters.")
        return ValidationResult(ValidationState.VALID)
    }
}


// --- PIN BLOCK SCREEN ---

object PinBlockLogManager {
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
            inputs.forEach { (key, value) -> if (value.isNotBlank()) append("  $key: $value\n") }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }
        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

object PinBlockService {
    // NOTE: This is a placeholder for actual PIN Block formatting logic.
    fun encode(format: String, pan: String, pin: String, paddingChar: Char): String {
        // Mock logic for ISO-0 format
        val pinLen = pin.length
        val pinPart = "0$pinLen$pin".padEnd(16, paddingChar)

        val panPart = "0000" + pan.takeLast(13).dropLast(1)

        val pinBlockBytes = pinPart.decodeHex()
        val panBytes = panPart.decodeHex()

        val resultBytes = ByteArray(8)
        for (i in resultBytes.indices) {
            resultBytes[i] = (pinBlockBytes[i].toInt() xor panBytes[i].toInt()).toByte()
        }
        return resultBytes.toHex().uppercase()
    }

    fun decode(format: String, pan: String, pinBlock: String): String {
        // Mock logic for ISO-0 format
        val panPart = "0000" + pan.takeLast(13).dropLast(1)
        val panBytes = panPart.decodeHex()
        val pinBlockBytes = pinBlock.decodeHex()

        val resultBytes = ByteArray(8)
        for (i in resultBytes.indices) {
            resultBytes[i] = (pinBlockBytes[i].toInt() xor panBytes[i].toInt()).toByte()
        }

        val pinPart = resultBytes.toHex()
        val pinLen = pinPart.substring(1, 2).toIntOrNull() ?: 0
        if (pinLen in 4..12) {
            return pinPart.substring(2, 2 + pinLen)
        }
        throw IllegalArgumentException("Invalid PIN length decoded from PIN block.")
    }

    private fun String.decodeHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}

enum class PinBlockTabs(val title: String, val icon: ImageVector) {
    ENCODE("Encode", Icons.Default.Lock),
    DECODE("Decode", Icons.Default.LockOpen),
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PinBlockGeneralScreen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val pinBlockFormats = remember {
        listOf(
            "Format 0 (ISO-0)", "Format 1 (ISO-1)", "Format 2 (ISO-2)", "Format 3 (ISO-3)",
            "Format 4 (ISO-4)", "ANSI X9.8", "Docutel & Diebold & NCR ATMs", "ECI-1", "ECI-2",
            "ECI-3", "ECI-4", "IBM 3621", "IBM 3624", "IBM 4704 Encr. PIN Pad", "IBM 5906",
            "VISA-1", "VISA-2", "VISA-3", "VISA-4", "Europay/MasterCard (Pay Now & Pay Later)"
        )
    }
    var selectedFormat by remember { mutableStateOf(pinBlockFormats.first()) }
    val tabList = PinBlockTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "PIN Block Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Global PIN Block Format Dropdown
                    ModernDropdownField("PIN block format", selectedFormat, pinBlockFormats) {
                        selectedFormat = pinBlockFormats[it]
                    }

                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabList.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(tab.title) },
                                icon = { Icon(tab.icon, contentDescription = tab.title) }
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn()) with
                                    (slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "pinblock_tab_transition"
                    ) { tab ->
                        when (tab) {
                            PinBlockTabs.ENCODE -> EncodeCard(selectedFormat)
                            PinBlockTabs.DECODE -> DecodeCard(selectedFormat)
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { PinBlockLogManager.clearLogs() },
                        logEntries = PinBlockLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun EncodeCard(format: String) {
    var pan by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    val paddingChars = remember { (('0'..'9') + ('A'..'F')).map { it.toString() } }
    var selectedPaddingChar by remember { mutableStateOf("F") }
    var isLoading by remember { mutableStateOf(false) }

    val panValidation = PinBlockValidationUtils.validatePan(pan)
    val pinValidation = PinBlockValidationUtils.validatePin(pin)
    val isFormValid = pan.isNotBlank() && pin.isNotBlank() && panValidation.state != ValidationState.ERROR && pinValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "Encode PIN Block", subtitle = "Create a formatted PIN block", icon = Icons.Default.VpnKey) {
        EnhancedTextField(pan, { pan = it }, "PAN", validation = panValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(pin, { pin = it }, "PIN", validation = pinValidation)
        Spacer(Modifier.height(12.dp))
        ModernDropdownField("Padding Character", selectedPaddingChar, paddingChars) {
            selectedPaddingChar = paddingChars[it]
        }
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Encode",
            onClick = {
                isLoading = true
                val inputs = mapOf("Format" to format, "PAN" to pan, "PIN" to pin, "Padding" to selectedPaddingChar)
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = PinBlockService.encode(format, pan, pin, selectedPaddingChar.first())
                        PinBlockLogManager.logOperation("Encode", inputs, "PIN Block: $result", "155")
                    } catch(e: Exception) {
                        PinBlockLogManager.logOperation("Encode", inputs, error = e.message, executionTime = 155)
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.ArrowForward, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DecodeCard(format: String) {
    var pinBlock by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val pinBlockValidation = PinBlockValidationUtils.validatePinBlock(pinBlock)
    val panValidation = PinBlockValidationUtils.validatePan(pan)
    val isFormValid = pan.isNotBlank() && pinBlock.isNotBlank() && panValidation.state != ValidationState.ERROR && pinBlockValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "Decode PIN Block", subtitle = "Extract PIN from a formatted block", icon = Icons.Default.VpnKey) {
        EnhancedTextField(pinBlock, { pinBlock = it }, "PIN Block", validation = pinBlockValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(pan, { pan = it }, "PAN", validation = panValidation)
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Decode",
            onClick = {
                isLoading = true
                val inputs = mapOf("Format" to format, "PIN Block" to pinBlock, "PAN" to pan)
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = PinBlockService.decode(format, pan, pinBlock)
                        PinBlockLogManager.logOperation("Decode", inputs, "Decoded PIN: $result", "155")
                    } catch(e: Exception) {
                        PinBlockLogManager.logOperation("Decode", inputs, error = e.message, executionTime = 155)
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.ArrowBack, modifier = Modifier.fillMaxWidth()
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
