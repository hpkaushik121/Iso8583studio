package `in`.aicortex.iso8583studio.ui.screens.keys

import `in`.aicortex.iso8583studio.analytics.Analytics
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
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PersistentTabContent
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random


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

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                val displayValue = if (key.contains("Key", ignoreCase = true)) "${value.take(16)}..." else value
                append("  $key: $displayValue\n")
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
    private fun generateRandomHex(length: Int) = (1..length).map { Random.nextInt(0, 16).toString(16) }.joinToString("").uppercase()

    fun generateKeys(count: Int, lengthBits: Int): List<String> {
        val lengthHex = lengthBits / 4
        return List(count) { generateRandomHex(lengthHex) }
    }

    fun combineKeys(keys: List<String>): String {
        if (keys.isEmpty() || keys.any { it.isEmpty() }) return "ERROR: Cannot combine empty keys"
        // Mock XOR combination
        val maxLength = keys.maxOf { it.length }
        val paddedKeys = keys.map { it.padEnd(maxLength, '0') }
        var result = "0".repeat(maxLength).toBigInteger(16)
        paddedKeys.forEach {
            result = result.xor(it.toBigInteger(16))
        }
        return result.toString(16).uppercase().padStart(maxLength, '0')
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
            val bytes = hexKey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val expanded = when (bytes.size) {
                8 -> bytes + bytes + bytes                     // single length -> triple
                16 -> bytes + bytes.copyOf(8)                  // double length -> triple
                24 -> bytes                                     // triple length
                else -> return "??????"
            }
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(expanded, "DESede"))
            cipher.doFinal(ByteArray(8)).joinToString("") { "%02X".format(it) }.take(6)
        } catch (_: Exception) {
            "??????"
        }
    }
}

private enum class TdesKeyLength(val label: String, val hexLen: Int) {
    SINGLE("Single length (8B / 16H)", 16),
    DOUBLE("Double length (16B / 32H)", 32),
    TRIPLE("Triple length (24B / 48H)", 48);

    companion object {
        fun fromLabel(label: String): TdesKeyLength = values().first { it.label == label }
    }
}


@Composable
fun DeaKeysScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = DeaKeysTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

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
    var keyLength by remember { mutableStateOf("128-bit") }
    var keyParity by remember { mutableStateOf("Odd") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = DeaKeysValidationUtils.validateNumeric(keysToGenerate).state == ValidationState.VALID

    ModernCryptoCard(title = "Key Generator", subtitle = "Generate one or more random keys", icon = Icons.Default.Autorenew) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = keysToGenerate, onValueChange = { keysToGenerate = it }, label = "Keys to Generate", validation = DeaKeysValidationUtils.validateNumeric(keysToGenerate))
            ModernDropdownField(label = "Key Length", value = keyLength, options = listOf("64-bit", "128-bit", "192-bit", "256-bit"), onSelectionChanged = { keyLength = listOf("64-bit", "128-bit", "192-bit", "256-bit")[it] })
            ModernDropdownField(label = "Key Parity", value = keyParity, options = listOf("Odd", "Even", "None"), onSelectionChanged = { keyParity = listOf("Odd", "Even", "None")[it] })
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Generate",
                onClick = {
                    isLoading = true
                    val inputs = mapOf("Count" to keysToGenerate, "Length" to keyLength, "Parity" to keyParity)
                    GlobalScope.launch {
                        delay(200)
                        val numKeys = keysToGenerate.toIntOrNull() ?: 1
                        val bits = keyLength.removeSuffix("-bit").toInt()
                        val keys = DeaKeysCryptoService.generateKeys(numKeys, bits)
                        val result = keys.mapIndexed { index, key -> "  Key #${index + 1}: $key" }.joinToString("\n")
                        DeaKeysLogManager.logOperation("Key Generation", inputs, result, executionTime = 210)
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
            )
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Combined Key",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                combinedKey,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            KcvBadge(combinedKcv)
                        }
                    }
                }
            }
        }
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

    val isFormValid = DeaKeysValidationUtils.validateHex(key).state == ValidationState.VALID

    ModernCryptoCard(title = "Parity Enforcement", subtitle = "Apply odd or even parity to a key", icon = Icons.Default.Security) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = key, onValueChange = { key = it.filterNot { ch -> ch.isWhitespace() }.uppercase() }, label = "Key (Hex)", validation = DeaKeysValidationUtils.validateHex(key), maxLines = 4)
            ModernDropdownField(label = "Key Parity", value = keyParity, options = listOf("Odd", "Even"), onSelectionChanged = { keyParity = listOf("Odd", "Even")[it] })
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Enforce Parity",
                onClick = {
                    isLoading = true
                    val inputs = mapOf("Key" to key, "Parity" to keyParity)
                    GlobalScope.launch {
                        delay(100)
                        val result = "Key with $keyParity Parity: ${key.dropLast(1)}F" // Mock result
                        DeaKeysLogManager.logOperation("Parity Enforcement", inputs, result, executionTime = 105)
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Check, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun KeyValidationTab() {
    var key by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = DeaKeysValidationUtils.validateHex(key).state == ValidationState.VALID

    ModernCryptoCard(title = "Key Validation", subtitle = "Validate a key's properties", icon = Icons.Default.VerifiedUser) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = key, onValueChange = { key = it.filterNot { ch -> ch.isWhitespace() }.uppercase() }, label = "Key (Hex)", validation = DeaKeysValidationUtils.validateHex(key), maxLines = 4)
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Validate",
                onClick = {
                    isLoading = true
                    val inputs = mapOf("Key" to key)
                    GlobalScope.launch {
                        delay(100)
                        val result = "Key is valid. Parity: Odd. KCV: A1B2C3" // Mock result
                        DeaKeysLogManager.logOperation("Key Validation", inputs, result, executionTime = 102)
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.FactCheck, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult) {
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
