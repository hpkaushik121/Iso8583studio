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
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---

object HMACValidationUtils {
    fun validate(value: String, fieldName: String, inputType: String): ValidationResult {
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


// --- HMAC SCREEN ---

object HMACLogManager {
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

object HMACService {
    // NOTE: This is a placeholder for actual HMAC cryptographic logic.
    // It uses a simplified hashing approach for demonstration.
    fun generateHmac(hashType: String, key: String, keyInputType: String, data: String, dataInputType: String): String {
        // Mock logic combines inputs and hashes them. A real implementation would use the javax.crypto.Mac class.
        val keyBytes = if (keyInputType == "Hexadecimal") key.decodeHex() else key.toByteArray(StandardCharsets.UTF_8)
        val dataBytes = if (dataInputType == "Hexadecimal") data.decodeHex() else data.toByteArray(StandardCharsets.UTF_8)

        // For a real implementation, you'd map hashType to a JCA algorithm name like "HmacSHA256"
        // and use the Mac class. This is a simplified mock.
        val combined = keyBytes + dataBytes
        val digest = MessageDigest.getInstance(hashType.replace("-", "")).digest(combined)
        return digest.toHex().uppercase()
    }

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}

@Composable
fun HMACScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "HMAC Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
            ) {
                HMACGenerationCard()
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { HMACLogManager.clearLogs() },
                        logEntries = HMACLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun HMACGenerationCard() {
    // --- State Management ---
    val hashTypes = remember {
        listOf("MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512", "RIPEMD-160") // Simplified list for real implementation
    }
    var selectedHashType by remember { mutableStateOf("SHA-256") }

    val inputTypes = remember { listOf("ASCII", "Hexadecimal") }
    var keyInputType by remember { mutableStateOf(inputTypes.first()) }
    var dataInputType by remember { mutableStateOf(inputTypes.first()) }

    var hmacKey by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }

    // --- Validation ---
    val keyValidation = HMACValidationUtils.validate(hmacKey, "HMAC Key", keyInputType)
    val dataValidation = HMACValidationUtils.validate(data, "Data", dataInputType)

    val isFormValid = remember(hmacKey, data, keyValidation, dataValidation) {
        hmacKey.isNotBlank() && data.isNotBlank() &&
                keyValidation.state != ValidationState.ERROR &&
                dataValidation.state != ValidationState.ERROR
    }

    ModernCryptoCard(
        title = "HMAC Generation",
        subtitle = "Create a Hash-based Message Authentication Code",
        icon = Icons.Default.Key
    ) {
        ModernDropdownField("Hash Type", selectedHashType, hashTypes) {
            selectedHashType = hashTypes[it]
        }
        Spacer(Modifier.height(12.dp))

        ModernDropdownField("Key Input", keyInputType, inputTypes) {
            keyInputType = inputTypes[it]
        }
        Spacer(Modifier.height(12.dp))

        EnhancedTextField(hmacKey, { hmacKey = it }, "HMAC Key", validation = keyValidation)
        Spacer(Modifier.height(12.dp))

        ModernDropdownField("Data Input", dataInputType, inputTypes) {
            dataInputType = inputTypes[it]
        }
        Spacer(Modifier.height(12.dp))

        EnhancedTextField(data, { data = it }, "Data", validation = dataValidation, maxLines = 5)
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Generate HMAC",
            onClick = {
                isLoading = true
                val inputs = mapOf(
                    "Hash Type" to selectedHashType,
                    "Key Input" to keyInputType,
                    "HMAC Key" to hmacKey,
                    "Data Input" to dataInputType,
                    "Data" to data
                )
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = HMACService.generateHmac(selectedHashType, hmacKey, keyInputType, data, dataInputType)
                        HMACLogManager.logOperation("HMAC Generation", inputs, "Generated HMAC: $result", executionTime = 155)
                    } catch (e: Exception) {
                        HMACLogManager.logOperation("HMAC Generation", inputs, error = "Failed to generate HMAC. Algorithm might not be supported in this mock.", executionTime = 155)
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