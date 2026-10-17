package `in`.aicortex.iso8583studio.ui.screens.keys

import `in`.aicortex.iso8583studio.ui.chrome.ToolTab

import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec

import `in`.aicortex.iso8583studio.ui.chrome.ToolLog

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import ai.cortex.core.xorWith

import io.cryptocalc.crypto.engines.encryption.TdesCalculatorEngine

import io.cryptocalc.crypto.primitives.secureRandomBytes

import ai.cortex.core.toHexUpper

import `in`.aicortex.iso8583studio.platform.nowLogTimestamp

import `in`.aicortex.iso8583studio.analytics.Analytics
import ai.cortex.core.CryptoUtils
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
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PersistentTabContent
import `in`.aicortex.iso8583studio.ui.screens.components.LogPanelWithAutoScroll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
private object DeaKeysValidationUtils {
    fun validateHex(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY)
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        if (value.length % 2 != 0) {
            return ValidationResult(ValidationState.ERROR, "Hex string must have an even number of characters.")
        }
        return ValidationResult(ValidationState.VALID)
    }

    fun validateNumeric(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY)
        if (value.any { !it.isDigit() }) {
            return ValidationResult(ValidationState.ERROR, "Input must only contain digits.")
        }
        return ValidationResult(ValidationState.VALID)
    }

    /** Hex check plus TDES length check: 16H (single), 32H (double) or 48H (triple). */
    fun validateTdesKey(value: String): ValidationResult {
        val hex = validateHex(value)
        if (hex.state != ValidationState.VALID) return hex
        val length = TdesKeyLength.fromHexLen(value.length)
            ?: return ValidationResult(ValidationState.ERROR, "TDES key must be 16, 32 or 48 hex chars (8/16/24 bytes).")
        return ValidationResult(ValidationState.VALID, "TDES — ${length.label}")
    }
}

// --- DEA KEYS SCREEN ---

private enum class DeaKeysTabs(val title: String, val icon: ImageVector) {
    KEY_GENERATOR("Key Generator", Icons.Default.Autorenew),
    KEY_COMBINATION("Key Combination", Icons.Default.CallMerge),
    PARITY_ENFORCEMENT("Parity Enforcement", Icons.Default.Security),
    KEY_VALIDATION("Key Validation", Icons.Default.VerifiedUser)
}

private object DeaKeysLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
    }

    private fun addLog(entry: LogEntry) {
        _logEntries.add(entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, _logEntries.size)
    }

    fun logOperation(operation: String, inputs: Map<String, String>, result: String? = null, error: String? = null, executionTime: Long = 0L) {
        if (result == null && error == null) return
        Analytics.calculationRun(operation, success = result != null, durationMs = executionTime)

        val timestamp = nowLogTimestamp()
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                append("  $key: $value\n")
            }
            result?.let { append("\nResult:\n$it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

private object DeaKeysCryptoService {

    fun hexToBytes(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    fun bytesToHex(bytes: ByteArray): String = bytes.toHexUpper()

    /**
     * Generates [count] random TDES keys of the given [length] using SecureRandom.
     * [parity] is "Odd", "Even" or "None"; odd/even parity is enforced on every byte.
     */
    fun generateKeys(count: Int, length: TdesKeyLength, parity: String): List<String> =
        List(count) {
            val raw = secureRandomBytes(length.bytes)
            val adjusted = when (parity) {
                "Odd" -> CryptoUtils.applyParity(raw, isOdd = true)
                "Even" -> CryptoUtils.applyParity(raw, isOdd = false)
                else -> raw
            }
            bytesToHex(adjusted)
        }

    /** Forces odd or even parity on every byte of a hex key (any length). */
    fun enforceParity(hexKey: String, odd: Boolean): String =
        bytesToHex(CryptoUtils.applyParity(hexToBytes(hexKey), isOdd = odd))

    /** Returns "Odd", "Even" or "Mixed" depending on the parity of each byte. */
    fun checkParity(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "Mixed"
        val oddBytes = bytes.count { (it.toInt() and 0xFF).countOneBits() % 2 == 1 }
        return when (oddBytes) {
            bytes.size -> "Odd"
            0 -> "Even"
            else -> "Mixed"
        }
    }

    fun combineKeys(keys: List<String>): String {
        if (keys.isEmpty() || keys.any { it.isEmpty() }) return "ERROR: Cannot combine empty keys"
        val maxLength = keys.maxOf { it.length }
        val paddedKeys = keys.map { it.padEnd(maxLength, '0') }
        var result = ByteArray(maxLength / 2)
        paddedKeys.forEach { result = result.xorWith(hexToBytes(it)) }
        return bytesToHex(result).padStart(maxLength, '0')
    }

    /**
     * TDES KCV = first 3 bytes (6 hex) of DESede-ECB encryption of 8 zero bytes.
     * Accepts 16 hex (single), 32 hex (double) and 48 hex (triple length) keys.
     * Returns "" for empty input and "??????" for malformed input.
     */
    fun calculateTdesKcv(hexKey: String): String {
        if (hexKey.isEmpty()) return ""
        return try {
            if (hexKey.length % 2 != 0 || hexKey.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
                return "??????"
            }
            val bytes = hexToBytes(hexKey)
            val expanded = when (bytes.size) {
                8 -> bytes + bytes + bytes                     // single length -> triple
                16 -> bytes + bytes.copyOf(8)                  // double length -> triple (K1|K2|K1)
                24 -> bytes                                     // triple length
                else -> return "??????"
            }
            bytesToHex(TdesCalculatorEngine.calculateKCV(expanded))
        } catch (_: Exception) {
            "??????"
        }
    }

    data class KeyValidationReport(
        val length: TdesKeyLength,
        val parity: String,
        val kcv: String,
        val issues: List<String>
    ) {
        val isValid: Boolean get() = issues.isEmpty()

        fun asText(): String = buildString {
            append("  Length: ${length}\n")
            append("  Parity: $parity\n")
            append("  KCV: $kcv\n")
            if (isValid) append("  Status: Key is valid")
            else {
                append("  Status: Key has issues:\n")
                issues.forEach { append("    - $it\n") }
            }
        }.trimEnd()
    }

    /**
     * Validates a 16/32/48 hex TDES key: length, byte parity, KCV and component structure
     * (equal K1/K2/K3 components reduce the effective strength).
     */
    fun validateKey(hexKey: String): KeyValidationReport {
        val length = requireNotNull(TdesKeyLength.fromHexLen(hexKey.length)) { "Unsupported key length" }
        val bytes = hexToBytes(hexKey)
        val parity = checkParity(bytes)
        val issues = mutableListOf<String>()
        if (parity != "Odd") issues += "Parity is $parity (DEA keys are expected to have odd parity)"

        val parts = bytes.toList().chunked(8)
        when (parts.size) {
            2 -> if (parts[0] == parts[1]) issues += "K1 == K2: effective single-DES strength"
            3 -> when {
                parts[0] == parts[1] && parts[1] == parts[2] -> issues += "K1 == K2 == K3: effective single-DES strength"
                parts[0] == parts[1] || parts[1] == parts[2] -> issues += "Adjacent components are equal: effective single-DES strength"
                parts[0] == parts[2] -> issues += "K1 == K3: equivalent to a double-length key (K1-K2-K1)"
            }
        }
        return KeyValidationReport(length, parity, calculateTdesKcv(hexKey), issues)
    }
}

private enum class TdesKeyLength(val label: String, val hexLen: Int) {
    SINGLE("Single length (8B / 16H)", 16),
    DOUBLE("Double length (16B / 32H)", 32),
    TRIPLE("Triple length (24B / 48H)", 48);

    val bytes: Int get() = hexLen / 2

    companion object {
        fun fromLabel(label: String): TdesKeyLength = values().first { it.label == label }
        fun fromHexLen(hexLen: Int): TdesKeyLength? = values().firstOrNull { it.hexLen == hexLen }
    }
}


@Composable
fun DeaKeysScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = DeaKeysTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    val body: @Composable () -> Unit = {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        when (tab) {
                            DeaKeysTabs.KEY_GENERATOR -> KeyGeneratorTab()
                            DeaKeysTabs.KEY_COMBINATION -> KeyCombinationTab()
                            DeaKeysTabs.PARITY_ENFORCEMENT -> ParityEnforcementTab()
                            DeaKeysTabs.KEY_VALIDATION -> KeyValidationTab()
                        }
                    }
                
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "DEA Keys Calculator", onBack = onBack, tabs = tabList.map { ToolTab(it.title, it.icon) }, selectedTab = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, log = ToolLog(DeaKeysLogManager.logEntries, onClear = { DeaKeysLogManager.clearLogs() })), content = body)
        return
    }
    Scaffold(
        topBar = { AppBarWithBack(title = "DEA Keys Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary,
                indicator = { tabPositions -> TabRowDefaults.Indicator(modifier = Modifier.customTabIndicatorOffset(tabPositions[selectedTabIndex]), height = 3.dp, color = MaterialTheme.colors.primary) }
            ) {
                tabList.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(imageVector = tab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tab.title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        selectedContentColor = MaterialTheme.colors.primary,
                        unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            body()
        }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { DeaKeysLogManager.clearLogs() },
                            logEntries = DeaKeysLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// --- TABS ---

@Composable
private fun KeyGeneratorTab() {
    var keysToGenerate by remember { mutableStateOf("1") }
    var keyLength by remember { mutableStateOf(TdesKeyLength.DOUBLE) }
    var keyParity by remember { mutableStateOf("Odd") }
    var isLoading by remember { mutableStateOf(false) }
    var generatedKeys by remember { mutableStateOf<List<String>>(emptyList()) }

    val countValidation = DeaKeysValidationUtils.validateNumeric(keysToGenerate)
    val isFormValid = countValidation.state == ValidationState.VALID && (keysToGenerate.toIntOrNull() ?: 0) in 1..100

    ModernCryptoCard(title = "Key Generator", subtitle = "Generate one or more random TDES keys (single, double or triple length)", icon = Icons.Default.Autorenew) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(
                value = keysToGenerate,
                onValueChange = { keysToGenerate = it },
                label = "Keys to Generate (1-100)",
                validation = if (countValidation.state == ValidationState.VALID && !isFormValid)
                    ValidationResult(ValidationState.ERROR, "Enter a value between 1 and 100.")
                else countValidation
            )
            ModernDropdownField(
                label = "Key Type / Length",
                value = "TDES — ${keyLength.label}",
                options = TdesKeyLength.values().map { "TDES — ${it.label}" },
                onSelectionChanged = {
                    keyLength = TdesKeyLength.values()[it]
                    generatedKeys = emptyList()
                }
            )
            ModernDropdownField(label = "Key Parity", value = keyParity, options = listOf("Odd", "Even", "None"), onSelectionChanged = { keyParity = listOf("Odd", "Even", "None")[it]; generatedKeys = emptyList() })
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Generate",
                onClick = {
                    isLoading = true
                    val inputs = mapOf("Count" to keysToGenerate, "Type" to "TDES", "Length" to keyLength.label, "Parity" to keyParity)
                    GlobalScope.launch {
                        delay(200)
                        val numKeys = (keysToGenerate.toIntOrNull() ?: 1).coerceIn(1, 100)
                        val keys = DeaKeysCryptoService.generateKeys(numKeys, keyLength, keyParity)
                        generatedKeys = keys
                        val result = keys.mapIndexed { index, key ->
                            "  Key #${index + 1}: $key  (KCV: ${DeaKeysCryptoService.calculateTdesKcv(key)})"
                        }.joinToString("\n")
                        DeaKeysLogManager.logOperation("Key Generation", inputs, result, executionTime = 210)
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
            )

            if (generatedKeys.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                ResultSurface(title = "Generated Keys") {
                    generatedKeys.forEachIndexed { index, key ->
                        KeyWithKcvRow(label = "Key #${index + 1}", key = key, kcv = DeaKeysCryptoService.calculateTdesKcv(key))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyCombinationTab() {
    val keyComponents = remember { mutableStateListOf("", "", "", "", "", "", "", "", "") }
    var isLoading by remember { mutableStateOf(false) }
    var keyLength by remember { mutableStateOf(TdesKeyLength.DOUBLE) }
    var combinedKey by remember { mutableStateOf("") }
    var combinedKcv by remember { mutableStateOf("") }

    val validKeys = keyComponents.filter { it.isNotBlank() }
    val expectedLen = keyLength.hexLen
    val isFormValid = validKeys.isNotEmpty() &&
        validKeys.all {
            DeaKeysValidationUtils.validateHex(it).state == ValidationState.VALID && it.length == expectedLen
        }

    // Recompute KCV whenever inputs or length change; clears any stale result on edit.
    LaunchedEffect(keyLength) {
        combinedKey = ""
        combinedKcv = ""
    }

    ModernCryptoCard(
        title = "Key Combination",
        subtitle = "Combine TDES key components via XOR (KCV shown per component and for the result)",
        icon = Icons.Default.CallMerge
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernDropdownField(
                label = "Key Type / Length",
                value = "TDES — ${keyLength.label}",
                options = TdesKeyLength.values().map { "TDES — ${it.label}" },
                onSelectionChanged = { idx ->
                    keyLength = TdesKeyLength.values()[idx]
                    // Clear stale result when length changes
                    combinedKey = ""
                    combinedKcv = ""
                }
            )

            Divider(Modifier.padding(vertical = 4.dp))

            (0..8).forEach { index ->
                val comp = keyComponents[index]
                val validation = DeaKeysValidationUtils.validateHex(comp)
                val lengthOk = comp.isEmpty() || comp.length == expectedLen
                val kcv = if (comp.isNotEmpty() && validation.state == ValidationState.VALID && lengthOk) {
                    DeaKeysCryptoService.calculateTdesKcv(comp)
                } else ""

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        EnhancedTextField(
                            value = comp,
                            onValueChange = {
                                keyComponents[index] = it.filterNot { ch -> ch.isWhitespace() }.uppercase()
                                combinedKey = ""
                                combinedKcv = ""
                            },
                            label = "Key #${index + 1} (Hex, ${expectedLen}H)",
                            validation = if (!lengthOk)
                                ValidationResult(ValidationState.ERROR, "Expected ${expectedLen} hex chars.")
                            else validation
                        )
                    }
                    KcvBadge(kcv, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Spacer(Modifier.height(4.dp))
            ModernButton(
                text = "Combine",
                onClick = {
                    isLoading = true
                    val inputs = validKeys.mapIndexed { index, key -> "Key ${index + 1}" to key }.toMap()
                    GlobalScope.launch {
                        delay(200)
                        val result = DeaKeysCryptoService.combineKeys(validKeys)
                        val kcv = DeaKeysCryptoService.calculateTdesKcv(result)
                        combinedKey = result
                        combinedKcv = kcv
                        DeaKeysLogManager.logOperation(
                            "Key Combination",
                            inputs + mapOf("Type" to "TDES", "Length" to keyLength.label),
                            "Combined Key: $result\nKCV: $kcv",
                            executionTime = 205
                        )
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Link, modifier = Modifier.fillMaxWidth()
            )

            if (combinedKey.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                ResultSurface(title = "Combined Key") {
                    KeyWithKcvRow(key = combinedKey, kcv = combinedKcv)
                }
            }
        }
    }
}

@Composable
private fun ResultSurface(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun KeyWithKcvRow(key: String, kcv: String, label: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            label?.let {
                Text(it, style = MaterialTheme.typography.overline, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
            }
            Text(
                key,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
        KcvBadge(kcv)
    }
}

@Composable
private fun KcvBadge(kcv: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            "KCV",
            style = MaterialTheme.typography.overline,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        )
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (kcv.isNotEmpty() && kcv != "??????")
                MaterialTheme.colors.primary.copy(alpha = 0.10f)
            else MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
            modifier = Modifier.width(80.dp)
        ) {
            Text(
                text = kcv.ifEmpty { "------" },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                style = MaterialTheme.typography.body2,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = when {
                    kcv.isEmpty() -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    kcv == "??????" -> MaterialTheme.colors.error
                    else -> MaterialTheme.colors.primary
                }
            )
        }
    }
}

@Composable
private fun ParityEnforcementTab() {
    var key by remember { mutableStateOf("") }
    var keyParity by remember { mutableStateOf("Odd") }
    var isLoading by remember { mutableStateOf(false) }
    var resultKey by remember { mutableStateOf("") }
    var resultKcv by remember { mutableStateOf("") }
    var resultNote by remember { mutableStateOf("") }

    val validation = DeaKeysValidationUtils.validateTdesKey(key)
    val isFormValid = validation.state == ValidationState.VALID
    val inputKcv = if (isFormValid) DeaKeysCryptoService.calculateTdesKcv(key) else ""

    ModernCryptoCard(title = "Parity Enforcement", subtitle = "Apply odd or even parity to a TDES key (16H / 32H / 48H)", icon = Icons.Default.Security) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    EnhancedTextField(
                        value = key,
                        onValueChange = {
                            key = it.filterNot { ch -> ch.isWhitespace() }.uppercase()
                            resultKey = ""; resultKcv = ""; resultNote = ""
                        },
                        label = "Key (Hex, 16/32/48H)",
                        validation = validation,
                        maxLines = 4
                    )
                }
                KcvBadge(inputKcv, modifier = Modifier.padding(top = 8.dp))
            }
            ModernDropdownField(label = "Key Parity", value = keyParity, options = listOf("Odd", "Even"), onSelectionChanged = { keyParity = listOf("Odd", "Even")[it]; resultKey = ""; resultKcv = ""; resultNote = "" })
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Enforce Parity",
                onClick = {
                    isLoading = true
                    val length = TdesKeyLength.fromHexLen(key.length)?.label ?: "?"
                    val inputs = mapOf("Key" to key, "Type" to "TDES", "Length" to length, "Parity" to keyParity)
                    GlobalScope.launch {
                        delay(100)
                        try {
                            val output = DeaKeysCryptoService.enforceParity(key, odd = keyParity == "Odd")
                            val outputKcv = DeaKeysCryptoService.calculateTdesKcv(output)
                            val changedBytes = key.chunked(2).zip(output.chunked(2)).count { (a, b) -> !a.equals(b, ignoreCase = true) }
                            val note = if (changedBytes == 0) "Key already had $keyParity parity (unchanged)"
                                       else "$changedBytes of ${key.length / 2} bytes adjusted"
                            resultKey = output
                            resultKcv = outputKcv
                            resultNote = note
                            val result = buildString {
                                append("  Input KCV: ${DeaKeysCryptoService.calculateTdesKcv(key)}\n")
                                append("  Output Key: $output\n")
                                append("  Output KCV: $outputKcv\n")
                                append("  $note")
                            }
                            DeaKeysLogManager.logOperation("Parity Enforcement", inputs, result, executionTime = 105)
                        } catch (e: Exception) {
                            DeaKeysLogManager.logOperation("Parity Enforcement", inputs, error = e.message ?: "Unknown error")
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Check, modifier = Modifier.fillMaxWidth()
            )

            if (resultKey.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                ResultSurface(title = "Key with $keyParity Parity") {
                    KeyWithKcvRow(key = resultKey, kcv = resultKcv)
                    Text(resultNote, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun KeyValidationTab() {
    var key by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<DeaKeysCryptoService.KeyValidationReport?>(null) }

    val validation = DeaKeysValidationUtils.validateTdesKey(key)
    val isFormValid = validation.state == ValidationState.VALID

    ModernCryptoCard(title = "Key Validation", subtitle = "Check length, parity, KCV and component structure of a TDES key", icon = Icons.Default.VerifiedUser) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(
                value = key,
                onValueChange = { key = it.filterNot { ch -> ch.isWhitespace() }.uppercase(); report = null },
                label = "Key (Hex, 16/32/48H)",
                validation = validation,
                maxLines = 4
            )
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Validate",
                onClick = {
                    isLoading = true
                    val inputs = mapOf("Key" to key, "Type" to "TDES")
                    GlobalScope.launch {
                        delay(100)
                        try {
                            val r = DeaKeysCryptoService.validateKey(key)
                            report = r
                            DeaKeysLogManager.logOperation("Key Validation", inputs, r.asText(), executionTime = 102)
                        } catch (e: Exception) {
                            DeaKeysLogManager.logOperation("Key Validation", inputs, error = e.message ?: "Unknown error")
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.FactCheck, modifier = Modifier.fillMaxWidth()
            )

            report?.let { r ->
                Spacer(Modifier.height(4.dp))
                ResultSurface(title = if (r.isValid) "Key is valid" else "Key has issues") {
                    KeyWithKcvRow(key = key, kcv = r.kcv)
                    Text("Length: ${r.length.label}", style = MaterialTheme.typography.body2)
                    Text("Parity: ${r.parity}", style = MaterialTheme.typography.body2)
                    r.issues.forEach { issue ->
                        Text("• $issue", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.error)
                    }
                }
            }
        }
    }
}

// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.TextField(value, onValueChange, label, modifier, maxLines, validation, ""); return }
    Column(modifier = modifier) {
        FixedOutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), maxLines = maxLines,
            isError = validation.state == ValidationState.ERROR
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
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Card(title, subtitle, icon, null, content); return }
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ModernDropdownField(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Dropdown(label, value, options, onSelectionChanged, Modifier); return }
    var expanded by remember { mutableStateOf(false) }
    var textFieldWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(modifier = Modifier.onGloballyPositioned { textFieldWidth = it.size.width }) {
        FixedOutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), readOnly = true,
            trailingIcon = { Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null) },
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = !expanded })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.width(with(density) { textFieldWidth.toDp() }).heightIn(max = 300.dp)) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = { onSelectionChanged(index); expanded = false }) {
                    Text(text = option, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ModernButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Button(text, onClick, modifier, isLoading, enabled, icon); return }
    Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled && !isLoading, elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)) {
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "DeaKeysButtonAnimation") { loading ->
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

private fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2
    fillMaxWidth().wrapContentSize(Alignment.BottomStart).offset(x = indicatorOffset).width(indicatorWidth)
}
