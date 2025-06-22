package `in`.aicortex.iso8583studio.ui.screens.generic

import `in`.aicortex.iso8583studio.data.model.FieldValidation
import `in`.aicortex.iso8583studio.data.model.ValidationState
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
import kotlin.random.Random

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---

object AESPinBlockValidationUtils {
    fun validateKey(key: String): FieldValidation {
        if (key.isEmpty()) return FieldValidation(ValidationState.EMPTY, "Key cannot be empty.")
        if (key.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "Key must be valid hexadecimal.")
        }
        if (key.length != 32) { // AES-128
            return FieldValidation(ValidationState.ERROR, "AES Key must be 32 hex characters (16 bytes).")
        }
        return FieldValidation(ValidationState.VALID)
    }

    fun validatePan(pan: String): FieldValidation {
        if (pan.isEmpty()) return FieldValidation(ValidationState.EMPTY, "PAN cannot be empty.")
        if (pan.any { !it.isDigit() }) return FieldValidation(ValidationState.ERROR, "PAN must be numeric.")
        if (pan.length < 12) return FieldValidation(ValidationState.ERROR, "PAN must be at least 12 digits.")
        return FieldValidation(ValidationState.VALID)
    }

    fun validatePin(pin: String): FieldValidation {
        if (pin.isEmpty()) return FieldValidation(ValidationState.EMPTY, "PIN cannot be empty.")
        if (pin.any { !it.isDigit() }) return FieldValidation(ValidationState.ERROR, "PIN must be numeric.")
        if (pin.length < 4 || pin.length > 12) return FieldValidation(ValidationState.ERROR, "PIN must be between 4 and 12 digits.")
        return FieldValidation(ValidationState.VALID)
    }

    fun validatePinBlock(pinBlock: String): FieldValidation {
        if (pinBlock.isEmpty()) return FieldValidation(ValidationState.EMPTY, "PIN Block cannot be empty.")
        if (pinBlock.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "PIN Block must be valid hexadecimal.")
        }
        if (pinBlock.length != 32) return FieldValidation(ValidationState.ERROR, "PIN Block must be 32 hex characters.")
        return FieldValidation(ValidationState.VALID)
    }
}


// --- AES PIN BLOCK SCREEN ---

object AESPinBlockLogManager {
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

object AESPinBlockService {
    // NOTE: This is a placeholder for actual AES PIN Block formatting logic.
    // The mock XOR logic is symmetrical, so the same function is used for encryption and decryption.

    private fun processBlock(key: ByteArray, data: ByteArray): ByteArray {
        // Mock AES by XORing key with data
        val result = ByteArray(data.size)
        for(i in result.indices) {
            result[i] = (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return result
    }

    fun encode(key: String, pan: String, pin: String): String {
        // 1. Construct Plaintext PIN field
        val pinLenHex = pin.length.toString(16).uppercase()
        val pinField = "4$pinLenHex$pin" // Format 4
        val paddedPinField = pinField.padEnd(32, Random.nextInt(0, 16).toString(16).first())

        // 2. Construct PAN field
        val panField = pan.takeLast(13).dropLast(1).padStart(16, '0')

        // 3. XOR PIN field and PAN field
        val xorResult = xorHexStrings(paddedPinField, panField)

        // 4. Encrypt with AES Key (mocked)
        val encryptedBlock = processBlock(key.decodeHex(), xorResult.decodeHex())

        return encryptedBlock.toHex().uppercase()
    }

    fun decode(key: String, pan: String, pinBlock: String): String {
        // 1. Decrypt PIN Block with AES Key (mocked)
        val decryptedBlock = processBlock(key.decodeHex(), pinBlock.decodeHex())

        // 2. Construct PAN field
        val panField = pan.takeLast(13).dropLast(1).padStart(16, '0')

        // 3. XOR decrypted block with PAN field to get Plaintext PIN field
        val pinField = xorHexStrings(decryptedBlock.toHex(), panField)

        // 4. Parse the PIN field
        if (pinField.first() != '4') throw IllegalArgumentException("PIN Block is not Format 4.")
        val pinLen = pinField.substring(1, 2).toIntOrNull(16) ?: 0
        if (pinLen in 4..12) {
            return pinField.substring(2, 2 + pinLen)
        }
        throw IllegalArgumentException("Invalid PIN length decoded from block.")
    }

    private fun xorHexStrings(a: String, b: String): String {
        val result = StringBuilder()
        for (i in a.indices) {
            val xorVal = a[i].digitToInt(16) xor b[i].digitToInt(16)
            result.append(xorVal.toString(16))
        }
        return result.toString().uppercase()
    }

    private fun String.decodeHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}


@Composable
fun AESPinBlockScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "AES PIN Block (Format 4)", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
            ) {
                AESPinBlockCard()
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { AESPinBlockLogManager.clearLogs() },
                        logEntries = AESPinBlockLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun AESPinBlockCard() {
    var key by remember { mutableStateOf("") }
    var pinOrBlock by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Validation states will be updated on button click
    var keyValidation by remember { mutableStateOf(FieldValidation(ValidationState.EMPTY)) }
    var pinOrBlockValidation by remember { mutableStateOf(FieldValidation(ValidationState.EMPTY)) }
    var panValidation by remember { mutableStateOf(FieldValidation(ValidationState.EMPTY)) }

    ModernCryptoCard(
        title = "AES PIN Block Operations",
        subtitle = "Encode or Decode a Format 4 PIN block",
        icon = Icons.Default.VpnKey
    ) {
        EnhancedTextField(key, { key = it }, "Key (32 Hex Chars)", validation = keyValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(pinOrBlock, { pinOrBlock = it }, "PIN (Encode) / PIN Block (Decode)", validation = pinOrBlockValidation, maxLines = 2)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(pan, { pan = it }, "PAN", validation = panValidation)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernButton(
                text = "Encode",
                onClick = {
                    keyValidation = AESPinBlockValidationUtils.validateKey(key)
                    pinOrBlockValidation = AESPinBlockValidationUtils.validatePin(pinOrBlock)
                    panValidation = AESPinBlockValidationUtils.validatePan(pan)

                    if (keyValidation.isValid() && pinOrBlockValidation.isValid() && panValidation.isValid()) {
                        isLoading = true
                        val inputs = mapOf("Key" to key, "PIN" to pinOrBlock, "PAN" to pan)
                        GlobalScope.launch {
                            delay(150)
                            try {
                                val result = AESPinBlockService.encode(key, pan, pinOrBlock)
                                AESPinBlockLogManager.logOperation("Encode", inputs, "PIN Block: $result", "155")
                            } catch(e: Exception) {
                                AESPinBlockLogManager.logOperation("Encode", inputs, error = e.message, executionTime = 155)
                            }
                            isLoading = false
                        }
                    }
                },
                isLoading = isLoading, icon = Icons.Default.Lock, modifier = Modifier.weight(1f)
            )
            ModernButton(
                text = "Decode",
                onClick = {
                    keyValidation = AESPinBlockValidationUtils.validateKey(key)
                    pinOrBlockValidation = AESPinBlockValidationUtils.validatePinBlock(pinOrBlock)
                    panValidation = AESPinBlockValidationUtils.validatePan(pan)

                    if (keyValidation.isValid() && pinOrBlockValidation.isValid() && panValidation.isValid()) {
                        isLoading = true
                        val inputs = mapOf("Key" to key, "PIN Block" to pinOrBlock, "PAN" to pan)
                        GlobalScope.launch {
                            delay(150)
                            try {
                                val result = AESPinBlockService.decode(key, pan, pinOrBlock)
                                AESPinBlockLogManager.logOperation("Decode", inputs, "Decoded PIN: $result", "155")
                            } catch(e: Exception) {
                                AESPinBlockLogManager.logOperation("Decode", inputs, error = e.message, executionTime = 155)
                            }
                            isLoading = false
                        }
                    }
                },
                isLoading = isLoading, icon = Icons.Default.LockOpen, modifier = Modifier.weight(1f)
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
