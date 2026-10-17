package `in`.aicortex.iso8583studio.ui.screens.thaleskeys

import `in`.aicortex.iso8583studio.ui.chrome.ToolTab

import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec

import `in`.aicortex.iso8583studio.ui.chrome.ToolLog

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import io.cryptocalc.hsm.ThalesLmk

import ai.cortex.core.toHexUpper

import ai.cortex.core.hexToBytes

import `in`.aicortex.iso8583studio.platform.nowLogTimestamp

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PersistentTabContent
import `in`.aicortex.iso8583studio.ui.screens.components.LogPanelWithAutoScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random


private object ThalesValidationUtils {
    fun validateHex(value: String, friendlyName: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$friendlyName cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        if (value.length % 2 != 0) {
            return ValidationResult(ValidationState.ERROR, "Hex string must have an even number of characters.")
        }
        return ValidationResult(ValidationState.VALID)
    }
}

// --- THALES KEYS SCREEN ---

private enum class ThalesKeysTabs(val title: String, val icon: ImageVector) {
    ENCRYPT_DECRYPT("Key Encryption / Decryption", Icons.Default.SyncAlt),
    KEY_LOOKUP("Key Lookup", Icons.Default.Search)
}

private object ThalesLogManager {
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
                val displayValue = if (key.contains("Key", ignoreCase = true)) "${value.take(16)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp, logType, message, details))
    }
}


@Composable
fun ThalesKeysScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = ThalesKeysTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    val body: @Composable () -> Unit = {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        when (tab) {
                            ThalesKeysTabs.ENCRYPT_DECRYPT -> EncryptDecryptTab()
                            ThalesKeysTabs.KEY_LOOKUP -> KeyLookupTab()
                        }
                    }
                
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "Thales Keys Encryption/Decoding", onBack = onBack, tabs = tabList.map { ToolTab(it.title, it.icon) }, selectedTab = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, log = ToolLog(ThalesLogManager.logEntries, onClear = { ThalesLogManager.clearLogs() })), content = body)
        return
    }
    Scaffold(
        topBar = { AppBarWithBack(title = "Thales Keys Encryption/Decoding", onBackClick = onBack) },
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
                        }
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
                            onClearClick = { ThalesLogManager.clearLogs() },
                            logEntries = ThalesLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// --- TABS ---

private fun keyParityText(key: ByteArray): String = when {
    ThalesLmk.hasOddParity(key) -> "Odd"
    ThalesLmk.hasEvenParity(key) -> "Even"
    else -> "Mixed / none"
}

@Composable
private fun EncryptDecryptTab() {
    val lmkPairs = remember { ThalesLmk.TEST_LMK_PAIRS.map { (pair, hex) -> "$pair: $hex" } }
    val variants = remember { (0..9).map { "$it: ${ThalesLmk.VARIANTS[it].toString(16).uppercase().padStart(2, '0')}" } }
    val schemes = remember { listOf("Z - single length (no scheme variant)", "U - double length (A6/5A)", "T - triple length (6A/DE/2B)", "X - double length (no scheme variant)", "Y - triple length (no scheme variant)") }

    var key by remember { mutableStateOf("") }
    var keyScheme by remember { mutableStateOf(schemes[1]) }
    var customLmk by remember { mutableStateOf("") }
    var selectedLmkPair by remember { mutableStateOf(lmkPairs.first()) }
    var selectedVariant by remember { mutableStateOf(variants[0]) }

    val isFormValid = ThalesValidationUtils.validateHex(key, "Key").state == ValidationState.VALID

    fun run(encrypt: Boolean) {
        val op = if (encrypt) "Encryption" else "Decryption"
        val inputs = mapOf("Key" to key, "LMK Pair" to selectedLmkPair, "Custom LMK" to customLmk, "Variant" to selectedVariant, "Scheme" to keyScheme)
        try {
            val lmk = (customLmk.ifBlank { selectedLmkPair.substringAfter(": ") }).hexToBytes()
            val variant = selectedVariant.substringBefore(":").toInt()
            val scheme = ThalesLmk.Scheme.of(keyScheme)
            val input = key.hexToBytes()
            val out = if (encrypt) ThalesLmk.encryptUnderLmk(input, lmk, variant, scheme) else ThalesLmk.decryptUnderLmk(input, lmk, variant, scheme)
            val clear = if (encrypt) input else out
            ThalesLogManager.logOperation(op, inputs, "$op result: ${scheme.tag}${out.toHexUpper()}\nClear key KCV: ${ThalesLmk.kcv(clear)}\nClear key parity: ${keyParityText(clear)}")
        } catch (e: Exception) {
            ThalesLogManager.logOperation(op, inputs, error = e.message)
        }
    }

    ModernCryptoCard(title = "Key Encryption / Decryption", subtitle = "Variant LMK: key under LMK pair ⊕ variant (⊕ scheme variant per component)", icon = Icons.Default.SyncAlt) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = key, onValueChange = { key = it.uppercase().removePrefix("U").removePrefix("T").removePrefix("X").removePrefix("Y").removePrefix("Z") }, label = "Key (Hex, 16/32/48)", validation = ThalesValidationUtils.validateHex(key, "Key"))
            ModernDropdownField(label = "Key Scheme", value = keyScheme, options = schemes, onSelectionChanged = { keyScheme = schemes[it] })
            ModernDropdownField(label = "LMK Pair (payShield test LMK)", value = selectedLmkPair, options = lmkPairs, onSelectionChanged = { selectedLmkPair = lmkPairs[it] })
            EnhancedTextField(value = customLmk, onValueChange = { customLmk = it.uppercase() }, label = "Custom LMK pair (Hex, 32; overrides the selection)", validation = ThalesValidationUtils.validateHex(customLmk, "LMK"))
            ModernDropdownField(label = "Variant", value = selectedVariant, options = variants, onSelectionChanged = { selectedVariant = variants[it] })
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(text = "Encrypt", onClick = { run(true) }, enabled = isFormValid, icon = Icons.Default.Lock, modifier = Modifier.weight(1f))
                ModernButton(text = "Decrypt", onClick = { run(false) }, enabled = isFormValid, icon = Icons.Default.LockOpen, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KeyLookupTab() {
    val parityOptions = remember { listOf("Any", "Odd", "Even") }

    var key by remember { mutableStateOf("") }
    var checkKcv by remember { mutableStateOf(true) }
    var kcv by remember { mutableStateOf("") }
    var parity by remember { mutableStateOf(parityOptions.first()) }
    var scanLmks by remember { mutableStateOf(false) }

    val isFormValid = ThalesValidationUtils.validateHex(key, "Key").state == ValidationState.VALID

    ModernCryptoCard(title = "Key Lookup", subtitle = "KCV and parity of a clear key; optionally find which test LMK pair/variant deciphers a key to odd parity", icon = Icons.Default.Search) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = key, onValueChange = { key = it.uppercase() }, label = "Key (Hex)", validation = ThalesValidationUtils.validateHex(key, "Key"), maxLines = 4)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checkKcv, onCheckedChange = { checkKcv = it })
                Text("Check KCV?", style = MaterialTheme.typography.body2)
            }
            EnhancedTextField(value = kcv, onValueChange = { kcv = it.uppercase() }, label = "Expected KCV (Optional)", validation = ThalesValidationUtils.validateHex(kcv, "KCV"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Parity:", style = MaterialTheme.typography.body2, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(16.dp))
                Row(Modifier.selectableGroup()) {
                    parityOptions.forEach { text ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = parity == text, onClick = { parity = text })
                            Text(text, Modifier.padding(start = 2.dp, end = 16.dp))
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = scanLmks, onCheckedChange = { scanLmks = it })
                Text("Treat as LMK-encrypted: scan test LMK pairs and variants", style = MaterialTheme.typography.body2)
            }
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Lookup Key",
                onClick = {
                    val inputs = mapOf("Key" to key, "Check KCV" to checkKcv.toString(), "KCV" to kcv, "Parity" to parity, "Scan" to scanLmks.toString())
                    try {
                        val bytes = key.hexToBytes()
                        val sb = StringBuilder()
                        val actualKcv = ThalesLmk.kcv(bytes)
                        val actualParity = keyParityText(bytes)
                        sb.append("Length: ${bytes.size * 8} bits\nParity: $actualParity")
                        if (parity != "Any") sb.append(if (actualParity == parity) " (matches)" else " (expected $parity)")
                        sb.append("\nKCV: $actualKcv")
                        if (checkKcv && kcv.isNotBlank()) sb.append(if (actualKcv.startsWith(kcv.uppercase())) " (matches)" else " (expected $kcv)")
                        if (scanLmks) {
                            val scheme = when (bytes.size) { 8 -> ThalesLmk.Scheme.Z; 16 -> ThalesLmk.Scheme.U; else -> ThalesLmk.Scheme.T }
                            val hits = mutableListOf<String>()
                            for ((pair, hex) in ThalesLmk.TEST_LMK_PAIRS) for (v in 0..9) {
                                val clear = runCatching { ThalesLmk.decryptUnderLmk(bytes, hex.hexToBytes(), v, scheme) }.getOrNull() ?: continue
                                val ok = ThalesLmk.hasOddParity(clear) && (kcv.isBlank() || !checkKcv || ThalesLmk.kcv(clear).startsWith(kcv.uppercase()))
                                if (ok) hits += "LMK $pair variant $v → ${clear.toHexUpper()} (KCV ${ThalesLmk.kcv(clear)})"
                            }
                            sb.append("\nScan (${scheme.tag} scheme): " + if (hits.isEmpty()) "no LMK pair/variant gives an odd-parity key" else "\n  " + hits.joinToString("\n  "))
                        }
                        ThalesLogManager.logOperation("Key Lookup", inputs, sb.toString())
                    } catch (e: Exception) {
                        ThalesLogManager.logOperation("Key Lookup", inputs, error = e.message)
                    }
                },
                enabled = isFormValid,
                icon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth()
            )
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "ThalesButtonAnimation") { loading ->
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
