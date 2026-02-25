package `in`.aicortex.iso8583studio.ui.screens.cipher

import ai.cortex.core.IsoUtil
import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import ai.cortex.core.types.AlgorithmType
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.CryptoAlgorithm
import ai.cortex.core.types.Key
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
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.crypto.engines.encryption.models.SymmetricDecryptionEngineParameters
import io.cryptocalc.crypto.engines.encryption.models.SymmetricEncryptionEngineParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ═══════════════════════════════════════════════════════════════════════════════════
// VALIDATION UTILS
// ═══════════════════════════════════════════════════════════════════════════════════

private object DesValidationUtils {
    fun validateHex(value: String, expectedBytes: Int? = null): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY)
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        if (value.length % 2 != 0) {
            return ValidationResult(ValidationState.ERROR, "Hex string must have an even number of characters.")
        }
        expectedBytes?.let {
            if (value.length != it * 2) {
                return ValidationResult(ValidationState.ERROR, "Expected ${it * 2} hex characters ($it bytes), got ${value.length}.")
            }
        }
        return ValidationResult(ValidationState.VALID, helperText = "${value.length / 2} bytes")
    }

    fun validateHexMultipleOf8(value: String): ValidationResult {
        val baseResult = validateHex(value)
        if (baseResult.state != ValidationState.VALID) return baseResult
        val byteCount = value.length / 2
        if (byteCount % 8 != 0) {
            return ValidationResult(
                ValidationState.WARNING,
                message = "Data is $byteCount bytes — not a multiple of 8. Padding will be applied."
            )
        }
        return ValidationResult(ValidationState.VALID, helperText = "$byteCount bytes (${byteCount / 8} blocks)")
    }

    fun validateKey(value: String, desType: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY)
        val baseResult = validateHex(value)
        if (baseResult.state == ValidationState.ERROR) return baseResult

        val byteCount = value.length / 2
        return when (desType) {
            "DES" -> {
                if (byteCount != 8) {
                    ValidationResult(ValidationState.ERROR, "DES key must be exactly 16 hex characters (8 bytes).")
                } else {
                    ValidationResult(ValidationState.VALID, helperText = "8 bytes — Single DES")
                }
            }
            "3DES" -> {
                when (byteCount) {
                    16 -> ValidationResult(ValidationState.VALID, helperText = "16 bytes — Double-length 3DES (K1-K2-K1)")
                    24 -> ValidationResult(ValidationState.VALID, helperText = "24 bytes — Triple-length 3DES (K1-K2-K3)")
                    else -> ValidationResult(ValidationState.ERROR, "3DES key must be 32 hex (16 bytes) or 48 hex (24 bytes).")
                }
            }
            else -> baseResult
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// LOG MANAGER
// ═══════════════════════════════════════════════════════════════════════════════════

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

    fun logOperation(
        operation: String,
        inputs: Map<String, String>,
        result: String? = null,
        error: String? = null,
        executionTime: Long = 0L
    ) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                val displayValue = if (value.length > 64) "${value.take(64)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null)
            (LogType.TRANSACTION to "$operation completed")
        else
            (LogType.ERROR to "$operation failed")

        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// CRYPTO SERVICE — Delegates to EMVEngines (EncryptionEngine + KeysEngine)
//
// Pattern follows existing codebase:
//   val calculator = EMVEngines()
//   calculator.encryptionEngine.encrypt(CryptoAlgorithm.TDES, SymmetricEncryptionEngineParameters(...))
//
// Padding is handled at this layer because TdesCalculatorEngine uses NoPadding.
// ═══════════════════════════════════════════════════════════════════════════════════

private object DesCryptoService {

    private val emvEngines = EMVEngines()

    // ── Padding ─────────────────────────────────────────────────────────────────
    // TdesCalculatorEngine (inside EMVEngines) uses JCE NoPadding, so all padding
    // must be applied before encrypt and removed after decrypt at this layer.

    fun applyPadding(data: ByteArray, paddingMethod: String, blockSize: Int = 8): ByteArray {
        return when (paddingMethod) {
            "None" -> {
                if (data.size % blockSize != 0) {
                    throw IllegalArgumentException(
                        "Data length (${data.size} bytes) is not a multiple of block size ($blockSize). " +
                                "Select a padding method or provide correctly sized data."
                    )
                }
                data
            }
            "Zeros" -> padToBlockSize(data, blockSize) { ByteArray(it) { 0x00 } }
            "Spaces" -> padToBlockSize(data, blockSize) { ByteArray(it) { 0x20 } }
            "ANSI X9.23" -> {
                val padLen = blockSize - (data.size % blockSize)
                val padding = ByteArray(padLen)
                java.security.SecureRandom().nextBytes(padding)
                padding[padLen - 1] = padLen.toByte()
                data + padding
            }
            "ISO 10126" -> {
                val padLen = blockSize - (data.size % blockSize)
                val padding = ByteArray(padLen)
                java.security.SecureRandom().nextBytes(padding)
                padding[padLen - 1] = padLen.toByte()
                data + padding
            }
            "PKCS#5", "PKCS#7", "Rijndael" -> {
                val padLen = blockSize - (data.size % blockSize)
                data + ByteArray(padLen) { padLen.toByte() }
            }
            "ISO7816-4", "ISO9797-1 (Method 2)" -> {
                val withMandatory = data + byteArrayOf(0x80.toByte())
                val rem = withMandatory.size % blockSize
                if (rem == 0) withMandatory
                else withMandatory + ByteArray(blockSize - rem) { 0x00 }
            }
            "ISO9797-1 (Method 1)" -> padToBlockSize(data, blockSize) { ByteArray(it) { 0x00 } }
            else -> throw IllegalArgumentException("Unknown padding method: $paddingMethod")
        }
    }

    private fun padToBlockSize(data: ByteArray, blockSize: Int, paddingFactory: (Int) -> ByteArray): ByteArray {
        val rem = data.size % blockSize
        if (rem == 0 && data.isNotEmpty()) return data
        return data + paddingFactory(blockSize - rem)
    }

    fun removePadding(data: ByteArray, paddingMethod: String): ByteArray {
        if (data.isEmpty()) return data
        return when (paddingMethod) {
            "None" -> data
            "Zeros", "ISO9797-1 (Method 1)" -> {
                var end = data.size
                while (end > 0 && data[end - 1] == 0x00.toByte()) end--
                data.copyOfRange(0, end)
            }
            "Spaces" -> {
                var end = data.size
                while (end > 0 && data[end - 1] == 0x20.toByte()) end--
                data.copyOfRange(0, end)
            }
            "ANSI X9.23", "ISO 10126", "PKCS#5", "PKCS#7", "Rijndael" -> {
                val padLen = data.last().toInt() and 0xFF
                if (padLen in 1..8 && padLen <= data.size) {
                    data.copyOfRange(0, data.size - padLen)
                } else data
            }
            "ISO7816-4", "ISO9797-1 (Method 2)" -> {
                var end = data.size - 1
                while (end >= 0 && data[end] == 0x00.toByte()) end--
                if (end >= 0 && data[end] == 0x80.toByte()) {
                    data.copyOfRange(0, end)
                } else data
            }
            else -> data
        }
    }

    // ── Mode Mapping ────────────────────────────────────────────────────────────
    // UI modes → ai.cortex.core.types.CipherMode
    // CFB-8/CFB-64 both map to CipherMode.CFB (TdesCalculatorEngine uses JCE CFB)
    // OFB-8/OFB-64 both map to CipherMode.OFB

    private fun mapToCipherMode(uiMode: String): CipherMode {
        return when (uiMode) {
            "ECB" -> CipherMode.ECB
            "CBC" -> CipherMode.CBC
            "CFB-8", "CFB-64" -> CipherMode.CFB
            "OFB-8", "OFB-64" -> CipherMode.OFB
            else -> throw IllegalArgumentException("Unsupported cipher mode: $uiMode")
        }
    }

    // ── Algorithm Selection ─────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun resolveAlgorithm(keySize: Int): CryptoAlgorithm<AlgorithmType.SYMMETRIC_BLOCK> {
        return (if (keySize == 8) CryptoAlgorithm.DES else CryptoAlgorithm.TDES)
    }

    // ── Core Encrypt via EMVEngines ─────────────────────────────────────────────

    suspend fun encrypt(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray?,
        uiMode: String,
        padding: String
    ): ByteArray {
        val paddedData = applyPadding(data, padding)
        val cipherMode = mapToCipherMode(uiMode)
        val algorithm = resolveAlgorithm(key.size)
        val effectiveIv = iv ?: ByteArray(8) { 0 }

        return emvEngines.encryptionEngine.encrypt(
            algorithm = algorithm,
            encryptionEngineParameters = SymmetricEncryptionEngineParameters(
                data = paddedData,
                key = key,
                iv = effectiveIv,
                mode = cipherMode
            )
        )
    }

    // ── Core Decrypt via EMVEngines ─────────────────────────────────────────────

    suspend fun decrypt(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray?,
        uiMode: String,
        padding: String
    ): ByteArray {
        if (data.size % 8 != 0) {
            throw IllegalArgumentException(
                "Ciphertext length (${data.size} bytes) is not a multiple of 8. Ensure valid encrypted data."
            )
        }

        val cipherMode = mapToCipherMode(uiMode)
        val algorithm = resolveAlgorithm(key.size)
        val effectiveIv = iv ?: ByteArray(8) { 0 }

        val decrypted = emvEngines.encryptionEngine.decrypt(
            algorithm = algorithm,
            decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                data = data,
                key = key,
                iv = effectiveIv,
                mode = cipherMode
            )
        )

        return removePadding(decrypted, padding)
    }

    // ── KCV via KeysEngine ──────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    suspend fun calculateKCV(keyBytes: ByteArray): String {
        val algorithm = resolveAlgorithm(keyBytes.size)
        val key = Key(value = keyBytes, cryptoAlgorithm = algorithm)
        val kcvBytes = emvEngines.keysEngine.calculateKcv(key)
        return IsoUtil.bytesToHex(kcvBytes)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun DesCalculatorScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AppBarWithBack(title = "DES/3DES Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel: Calculator
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

// ═══════════════════════════════════════════════════════════════════════════════════
// CALCULATOR CARD
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun DesCalculatorCard() {
    val coroutineScope = rememberCoroutineScope()

    val desTypes = remember { listOf("DES", "3DES") }
    val modes = remember { listOf("ECB", "CBC", "CFB-8", "CFB-64", "OFB-8", "OFB-64") }
    val paddings = remember {
        listOf(
            "None", "Zeros", "Spaces", "ANSI X9.23", "ISO 10126",
            "PKCS#5", "PKCS#7", "ISO7816-4", "Rijndael",
            "ISO9797-1 (Method 1)", "ISO9797-1 (Method 2)"
        )
    }
    val inputTypes = remember { listOf("ASCII", "Hexadecimal") }

    var selectedDesType by remember { mutableStateOf(desTypes.first()) }
    var selectedMode by remember { mutableStateOf(modes.first()) }
    var selectedPadding by remember { mutableStateOf(paddings[5]) } // Default PKCS#5
    var selectedInputType by remember { mutableStateOf(inputTypes.first()) }
    var inputData by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var iv by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var resultIsError by remember { mutableStateOf(false) }
    var lastOperation by remember { mutableStateOf("") }
    var kcvText by remember { mutableStateOf("") }

    val showIvField = selectedMode in listOf("CBC", "CFB-8", "CFB-64", "OFB-8", "OFB-64")

    // Validations
    val dataValidation = when {
        inputData.isEmpty() -> ValidationResult(ValidationState.EMPTY)
        selectedInputType == "Hexadecimal" -> DesValidationUtils.validateHexMultipleOf8(inputData)
        else -> ValidationResult(ValidationState.VALID, helperText = "${inputData.toByteArray().size} bytes")
    }
    val keyValidation = DesValidationUtils.validateKey(key, selectedDesType)
    val ivValidation = if (showIvField) DesValidationUtils.validateHex(iv, 8) else ValidationResult(ValidationState.VALID)

    val isFormValid = inputData.isNotBlank() && key.isNotBlank() &&
            dataValidation.state != ValidationState.ERROR &&
            keyValidation.state == ValidationState.VALID &&
            (!showIvField || ivValidation.state == ValidationState.VALID)

    // Auto-calculate KCV via EMVEngines KeysEngine
    LaunchedEffect(key, selectedDesType) {
        kcvText = if (keyValidation.state == ValidationState.VALID) {
            try {
                DesCryptoService.calculateKCV(IsoUtil.hexToBytes(key))
            } catch (_: Exception) {
                ""
            }
        } else ""
    }

    // ── Execute crypto via EMVEngines (suspend) ─────────────────────────────────
    fun executeCrypto(encrypt: Boolean) {
        isLoading = true
        resultText = ""
        resultIsError = false
        lastOperation = if (encrypt) "Encrypt" else "Decrypt"

        val operationName = if (encrypt)
            "${selectedDesType} Encrypt ($selectedMode)"
        else
            "${selectedDesType} Decrypt ($selectedMode)"

        coroutineScope.launch {
            try {
                val startTime = System.currentTimeMillis()

                val dataBytes = if (encrypt) {
                    when (selectedInputType) {
                        "ASCII" -> inputData.toByteArray(Charsets.UTF_8)
                        "Hexadecimal" -> IsoUtil.hexToBytes(inputData)
                        else -> inputData.toByteArray()
                    }
                } else {
                    IsoUtil.hexToBytes(inputData)
                }

                val keyBytes = IsoUtil.hexToBytes(key)
                val ivBytes = if (showIvField && iv.isNotEmpty()) IsoUtil.hexToBytes(iv) else null

                val output = withContext(Dispatchers.Default) {
                    if (encrypt) {
                        DesCryptoService.encrypt(dataBytes, keyBytes, ivBytes, selectedMode, selectedPadding)
                    } else {
                        DesCryptoService.decrypt(dataBytes, keyBytes, ivBytes, selectedMode, selectedPadding)
                    }
                }

                val elapsed = System.currentTimeMillis() - startTime
                val hexResult = IsoUtil.bytesToHex(output)

                resultText = if (encrypt) {
                    hexResult
                } else {
                    val asciiAttempt = try {
                        val str = String(output, Charsets.UTF_8)
                        if (str.all { it.code in 32..126 || it == '\n' || it == '\r' || it == '\t' }) str else null
                    } catch (_: Exception) { null }

                    buildString {
                        append("HEX: $hexResult")
                        asciiAttempt?.let { append("\nASCII: $it") }
                    }
                }

                DesLogManager.logOperation(
                    operation = operationName,
                    inputs = buildMap {
                        put("Algorithm", selectedDesType)
                        put("Mode", selectedMode)
                        put("Padding", selectedPadding)
                        put("Input Type", if (encrypt) selectedInputType else "Hexadecimal")
                        put("Data", if (encrypt && selectedInputType == "ASCII") inputData else IsoUtil.bytesToHex(dataBytes))
                        put("Data Length", "${dataBytes.size} bytes")
                        put("Key", key)
                        put("KCV", kcvText)
                        if (showIvField) put("IV", iv.ifEmpty { "0000000000000000 (default)" })
                    },
                    result = hexResult,
                    executionTime = elapsed
                )
            } catch (e: Exception) {
                resultText = e.message ?: "Unknown error"
                resultIsError = true

                DesLogManager.logOperation(
                    operation = operationName,
                    inputs = mapOf(
                        "Algorithm" to selectedDesType,
                        "Mode" to selectedMode,
                        "Padding" to selectedPadding,
                        "Data" to inputData.take(64),
                        "Key" to key
                    ),
                    error = e.message ?: "Unknown error"
                )
            } finally {
                isLoading = false
            }
        }
    }

    // ── UI ──────────────────────────────────────────────────────────────────────

    ModernCryptoCard(
        title = "DES/3DES Calculator",
        subtitle = "Encrypt or Decrypt data using DES / Triple DES",
        icon = Icons.Default.Lock,
        onInfoClick = null
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Algorithm + Mode
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField(
                    label = "Algorithm",
                    value = selectedDesType,
                    options = desTypes,
                    onSelectionChanged = {
                        selectedDesType = desTypes[it]
                        key = ""
                    },
                    modifier = Modifier.weight(1f)
                )
                ModernDropdownField(
                    label = "Mode",
                    value = selectedMode,
                    options = modes,
                    onSelectionChanged = { selectedMode = modes[it] },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Padding + Input Type
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField(
                    label = "Padding",
                    value = selectedPadding,
                    options = paddings,
                    onSelectionChanged = { selectedPadding = paddings[it] },
                    modifier = Modifier.weight(1f)
                )
                ModernDropdownField(
                    label = "Data Input Type",
                    value = selectedInputType,
                    options = inputTypes,
                    onSelectionChanged = { selectedInputType = inputTypes[it] },
                    modifier = Modifier.weight(1f)
                )
            }

            // Input Data
            EnhancedTextField(
                value = inputData,
                onValueChange = { inputData = it },
                label = if (selectedInputType == "ASCII") "Input Data (ASCII)" else "Input Data (Hex)",
                validation = dataValidation,
                maxLines = 5,
                placeholder = if (selectedInputType == "ASCII") "Enter plaintext..." else "e.g. 48656C6C6F"
            )

            // Key + KCV
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                EnhancedTextField(
                    value = key,
                    onValueChange = { key = it.uppercase().filter { c -> c in '0'..'9' || c in 'A'..'F' } },
                    label = "Key (Hex)",
                    validation = keyValidation,
                    modifier = Modifier.weight(1f),
                    placeholder = when (selectedDesType) {
                        "DES" -> "16 hex chars (8 bytes)"
                        else -> "32 hex (16B) or 48 hex (24B)"
                    }
                )

                if (kcvText.isNotEmpty()) {
                    Card(
                        modifier = Modifier.width(120.dp).padding(top = 4.dp),
                        elevation = 0.dp,
                        backgroundColor = SuccessGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("KCV", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), fontSize = 10.sp)
                            SelectionContainer {
                                Text(kcvText, style = MaterialTheme.typography.body2, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }
                    }
                }
            }

            // IV field
            AnimatedVisibility(visible = showIvField) {
                EnhancedTextField(
                    value = iv,
                    onValueChange = { iv = it.uppercase().filter { c -> c in '0'..'9' || c in 'A'..'F' } },
                    label = "Initialization Vector (IV) — 16 hex chars (8 bytes)",
                    validation = ivValidation,
                    placeholder = "Leave empty for zero IV (0000000000000000)"
                )
            }

            Spacer(Modifier.height(4.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(
                    text = "Encrypt",
                    onClick = { executeCrypto(encrypt = true) },
                    isLoading = isLoading && lastOperation == "Encrypt",
                    enabled = isFormValid,
                    icon = Icons.Default.Lock,
                    modifier = Modifier.weight(1f)
                )
                ModernButton(
                    text = "Decrypt",
                    onClick = { executeCrypto(encrypt = false) },
                    isLoading = isLoading && lastOperation == "Decrypt",
                    enabled = isFormValid,
                    icon = Icons.Default.LockOpen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Result display
            AnimatedVisibility(
                visible = resultText.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ResultCard(
                    result = resultText,
                    isError = resultIsError,
                    operation = lastOperation,
                    onCopy = {
                        try {
                            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                            clipboard.setContents(
                                java.awt.datatransfer.StringSelection(
                                    resultText.lines().first().removePrefix("HEX: ")
                                ), null
                            )
                        } catch (_: Exception) {}
                    },
                    onClear = { resultText = "" }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// RESULT CARD
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun ResultCard(
    result: String,
    isError: Boolean,
    operation: String,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    val bgColor = if (isError) Color(0xFFF44336).copy(alpha = 0.08f) else SuccessGreen.copy(alpha = 0.08f)
    val accentColor = if (isError) Color(0xFFF44336) else SuccessGreen
    var showCopied by remember { mutableStateOf(false) }

    LaunchedEffect(showCopied) {
        if (showCopied) {
            kotlinx.coroutines.delay(1500)
            showCopied = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = bgColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        if (isError) "Error" else "$operation Result",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                if (!isError) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { onCopy(); showCopied = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (showCopied) SuccessGreen else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Divider(color = accentColor.copy(alpha = 0.2f))

            SelectionContainer {
                Text(
                    result,
                    style = MaterialTheme.typography.body2,
                    fontFamily = if (!isError) FontFamily.Monospace else FontFamily.Default,
                    color = if (isError) Color(0xFFF44336) else MaterialTheme.colors.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!isError) {
                val hexOnly = result.lines().first().removePrefix("HEX: ")
                if (hexOnly.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }) {
                    Text(
                        "${hexOnly.length / 2} bytes (${hexOnly.length / 2 / 8} blocks)",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// SHARED UI COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    validation: ValidationResult,
    placeholder: String = ""
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            maxLines = maxLines,
            isError = validation.state == ValidationState.ERROR,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.primary
                    ValidationState.WARNING -> Color(0xFFFF9800)
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colors.error
                },
                unfocusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    ValidationState.WARNING -> Color(0xFFFF9800).copy(alpha = 0.5f)
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colors.error
                }
            )
        )
        when {
            validation.message.isNotEmpty() -> {
                Text(
                    text = validation.message,
                    color = when (validation.state) {
                        ValidationState.ERROR -> MaterialTheme.colors.error
                        ValidationState.WARNING -> Color(0xFFFF9800)
                        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    },
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            validation.helperText.isNotEmpty() -> {
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
}

@Composable
private fun ModernCryptoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onInfoClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
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
private fun ModernDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = { onSelectionChanged(index); expanded = false }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (option == value) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(16.dp))
                        }
                        Text(text = option, style = MaterialTheme.typography.body2)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ModernButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled && !isLoading,
        elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "DesButtonAnimation"
        ) { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    icon?.let { Icon(imageVector = it, contentDescription = null); Spacer(Modifier.width(8.dp)) }
                    Text(text, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun InfoDialog(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
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
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) { content() }
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