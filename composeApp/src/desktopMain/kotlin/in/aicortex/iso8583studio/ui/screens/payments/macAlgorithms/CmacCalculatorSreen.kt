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
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---

object CMACValidationUtils {
    fun validateKey(value: String, fieldName: String, inputType: String, encryptionType: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$fieldName cannot be empty.")

        val expectedLength = when (encryptionType) {
            "3DES" -> 32 // 16 bytes
            "AES" -> 32  // AES-128 is common for CMAC, 16 bytes
            else -> 0
        } * if (inputType == "Hexadecimal") 1 else 0

        if (inputType == "Hexadecimal") {
            if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
                return ValidationResult(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
            }
            if (value.length % 2 != 0) {
                return ValidationResult(ValidationState.ERROR, "$fieldName must have an even number of characters.")
            }
            if (expectedLength > 0 && value.length != expectedLength) {
                return ValidationResult(ValidationState.ERROR, "$fieldName for $encryptionType must be $expectedLength hex characters.")
            }
        }
        return ValidationResult(ValidationState.VALID)
    }

    fun validateData(value: String, fieldName: String, inputType: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$fieldName cannot be empty.")
        if (inputType == "Hexadecimal") {
            if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
                return ValidationResult(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
            }
            if (value.length % 2 != 0) {
                return ValidationResult(ValidationState.ERROR, "$fieldName must have an even number of characters.")
            }
        }
        return ValidationResult(ValidationState.VALID)
    }
}


// --- CMAC SCREEN ---

object CMACLogManager {
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

object CMACService {
    // NOTE: This is a placeholder for actual CMAC cryptographic logic.
    fun generateCmac(
        encryptionType: String,
        key: String,
        keyInputType: String,
        data: String,
        dataInputType: String,
        isAes96: Boolean
    ): String {
        // Mock logic combines inputs and hashes them.
        val combinedInput = "$encryptionType|$key|$keyInputType|$data|$dataInputType|$isAes96"

        val fullMac = MessageDigest.getInstance("SHA-256").digest(combinedInput.toByteArray()).toHex().uppercase()

        // Truncate if AES CMAC 96 is selected
        return if (isAes96 && encryptionType == "AES") {
            fullMac.take(24) // 96 bits = 12 bytes = 24 hex characters
        } else {
            fullMac.take(32) // Default to 128-bit MAC
        }
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}

@Composable
fun CMACScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "CMAC Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
            ) {
                CMACGenerationCard()
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { CMACLogManager.clearLogs() },
                        logEntries = CMACLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun CMACGenerationCard() {
    // --- State Management ---
    val encryptionTypes = remember { listOf("3DES", "AES") }
    var selectedEncryptionType by remember { mutableStateOf(encryptionTypes.first()) }

    val inputTypes = remember { listOf("ASCII", "Hexadecimal") }
    var keyInputType by remember { mutableStateOf(inputTypes.first()) }
    var dataInputType by remember { mutableStateOf(inputTypes.first()) }

    var cmacKey by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }
    var isAesCmac96 by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    // --- Validation ---
    val keyValidation = CMACValidationUtils.validateKey(cmacKey, "CMAC Key", keyInputType, selectedEncryptionType)
    val dataValidation = CMACValidationUtils.validateData(data, "Data", dataInputType)

    val isFormValid = remember(cmacKey, data, keyValidation, dataValidation) {
        cmacKey.isNotBlank() && data.isNotBlank() &&
                keyValidation.state != ValidationState.ERROR &&
                dataValidation.state != ValidationState.ERROR
    }

    ModernCryptoCard(
        title = "CMAC Generation",
        subtitle = "Cipher-based Message Authentication Code",
        icon = Icons.Default.Key
    ) {
        ModernDropdownField("Encryption Type", selectedEncryptionType, encryptionTypes) {
            selectedEncryptionType = encryptionTypes[it]
        }
        Spacer(Modifier.height(12.dp))

        ModernDropdownField("Key Input", keyInputType, inputTypes) {
            keyInputType = inputTypes[it]
        }
        Spacer(Modifier.height(12.dp))

        EnhancedTextField(cmacKey, { cmacKey = it }, "CMAC Key", validation = keyValidation)
        Spacer(Modifier.height(12.dp))

        ModernDropdownField("Data Input", dataInputType, inputTypes) {
            dataInputType = inputTypes[it]
        }
        Spacer(Modifier.height(12.dp))

        EnhancedTextField(data, { data = it }, "Data", validation = dataValidation, maxLines = 5)
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { isAesCmac96 = !isAesCmac96 }
        ) {
            Checkbox(
                checked = isAesCmac96,
                onCheckedChange = { isAesCmac96 = it },
                enabled = selectedEncryptionType == "AES" // Only enable for AES
            )
            Text("AES CMAC 96?", style = MaterialTheme.typography.body1)
        }
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Generate CMAC",
            onClick = {
                isLoading = true
                val inputs = mapOf(
                    "Encryption Type" to selectedEncryptionType,
                    "Key Input" to keyInputType,
                    "CMAC Key" to cmacKey,
                    "Data Input" to dataInputType,
                    "Data" to data,
                    "AES CMAC 96?" to if (selectedEncryptionType == "AES") isAesCmac96.toString() else "N/A"
                )
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = CMACService.generateCmac(
                            selectedEncryptionType,
                            cmacKey,
                            keyInputType,
                            data,
                            dataInputType,
                            isAesCmac96
                        )
                        CMACLogManager.logOperation("CMAC Generation", inputs, "Generated CMAC: $result", executionTime = 155)
                    } catch (e: Exception) {
                        CMACLogManager.logOperation("CMAC Generation", inputs, error = e.message, executionTime = 155)
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
