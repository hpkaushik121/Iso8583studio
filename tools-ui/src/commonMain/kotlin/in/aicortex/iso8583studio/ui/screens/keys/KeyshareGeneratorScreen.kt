package `in`.aicortex.iso8583studio.ui.screens.keys

import `in`.aicortex.iso8583studio.ui.chrome.toolScroll

import `in`.aicortex.iso8583studio.ui.chrome.ToolTab

import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec

import `in`.aicortex.iso8583studio.ui.chrome.ToolLog

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import io.cryptocalc.crypto.primitives.secureRandomBytes

import io.cryptocalc.crypto.primitives.Digests

import ai.cortex.core.types.CipherMode

import ai.cortex.core.CryptoUtils

import ai.cortex.core.hexToBytes

import ai.cortex.core.xorWith

import io.cryptocalc.crypto.engines.encryption.AesCalculatorEngine

import io.cryptocalc.crypto.engines.encryption.TdesCalculatorEngine

import ai.cortex.core.toHexUpper

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
import kotlin.random.Random



private object KeyshareValidationUtils {
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
}

// --- KEYSHARE SCREEN ---

private enum class KeyshareTabs(val title: String, val icon: ImageVector) {
    INSECURE("Insecure", Icons.Default.LockOpen),
    SECURE("Secure", Icons.Default.Lock),
}

private object KeyshareLogManager {
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
            append("Generated Components:\n")
            inputs.forEach { (key, value) ->
                val displayValue = if (value.length > 32) "${value.take(16)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n$it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type= logType, message = message, details = details))
    }
}

/**
 * Key-share generation and combination. Shares are XOR components of the full key
 * (the usual TR-31/X9.24 "key component" convention). With ForceOdd parity, every DES/TDES
 * component and the combined key get odd parity. In the Secure tab each share is protected
 * with AES-128-CTR under a key derived from its PIN (SHA-256 of "iso8583studio-keyshare:" + PIN,
 * first 16 bytes, zero IV); this is a transport convenience for this tool, not a standard.
 */
private object KeyshareCryptoService {

    fun keyLengthOptions(keyType: String): List<String> =
        if (keyType == "AES") listOf("AES-128 (32 hex)", "AES-192 (48 hex)", "AES-256 (64 hex)")
        else listOf("Single DES (16 hex)", "Double TDES (32 hex)", "Triple TDES (48 hex)")

    private fun bytesFor(option: String): Int = option.substringAfter("(").substringBefore(" hex").toInt() / 2

    fun computeKcv(key: ByteArray, keyType: String): String = try {
        if (keyType == "AES") AesCalculatorEngine.calculateKCV(key).toHexUpper()
        else TdesCalculatorEngine.calculateKCV(key).toHexUpper()
    } catch (_: Exception) { "N/A" }

    private fun pinKey(pin: String): ByteArray = Digests.sha256(("iso8583studio-keyshare:$pin").encodeToByteArray()).copyOf(16)

    fun protect(share: ByteArray, pin: String): ByteArray = AesCalculatorEngine.encrypt(share, pinKey(pin), CipherMode.CTR, ByteArray(16))
    fun unprotect(protected: ByteArray, pin: String): ByteArray = AesCalculatorEngine.decrypt(protected, pinKey(pin), CipherMode.CTR, ByteArray(16))

    fun generateParts(partCount: Int, keyType: String, lengthOption: String, forceOdd: Boolean): List<ByteArray> {
        val n = bytesFor(lengthOption)
        return List(partCount) {
            val b = secureRandomBytes(n)
            if (forceOdd && keyType != "AES") CryptoUtils.applyParity(b, isOdd = true) else b
        }
    }

    fun combine(parts: List<ByteArray>, keyType: String, forceOdd: Boolean): ByteArray {
        require(parts.isNotEmpty()) { "No key parts to combine" }
        val n = parts.first().size
        require(parts.all { it.size == n }) { "All key parts must have the same length" }
        var result = ByteArray(n)
        parts.forEach { result = result.xorWith(it) }
        return if (forceOdd && keyType != "AES") CryptoUtils.applyParity(result, isOdd = true) else result
    }

    fun randomPin(): String = secureRandomBytes(4).joinToString("") { ((it.toInt() and 0xFF) % 10).toString() }
}

@Composable
fun KeyshareGeneratorScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = KeyshareTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    val body: @Composable () -> Unit = {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        KeyshareTabContent(isSecure = tab == KeyshareTabs.SECURE)
                    }
                
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "Keyshare Generator", onBack = onBack, tabs = tabList.map { ToolTab(it.title, it.icon) }, selectedTab = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, log = ToolLog(KeyshareLogManager.logEntries, onClear = { KeyshareLogManager.clearLogs() })), content = body)
        return
    }
    Scaffold(
        topBar = { AppBarWithBack(title = "Keyshare Generator", onBackClick = onBack) },
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
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            body()
        }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { KeyshareLogManager.clearLogs() },
                            logEntries = KeyshareLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyshareTabContent(isSecure: Boolean) {
    var parity by remember { mutableStateOf("ForceOdd") }
    var keyType by remember { mutableStateOf("DES/TDES") }
    var keyLength by remember { mutableStateOf(KeyshareCryptoService.keyLengthOptions("DES/TDES")[1]) }
    var innerSelectedTabIndex by remember { mutableStateOf(0) }
    val innerTabTitles = listOf("2 Parts", "3 Parts")
    val lengthOptions = KeyshareCryptoService.keyLengthOptions(keyType)

    Column(modifier = Modifier.toolScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernCryptoCard(title = "Global Options", subtitle = if (isSecure) "Shares are PIN-protected (AES-128-CTR under a PIN-derived key)" else "Define settings for this operation", icon = Icons.Default.Tune) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Parity", parity, listOf("Ignore", "ForceOdd"), { parity = if (it == 0) "Ignore" else "ForceOdd" }, Modifier.weight(1f))
                ModernDropdownField("Key Type", keyType, listOf("DES/TDES", "AES"), {
                    keyType = if (it == 0) "DES/TDES" else "AES"
                    keyLength = KeyshareCryptoService.keyLengthOptions(keyType)[1]
                }, Modifier.weight(1f))
                ModernDropdownField("Key Length", keyLength, lengthOptions, { keyLength = lengthOptions[it] }, Modifier.weight(1f))
            }
        }

        TabRow(
            selectedTabIndex = innerSelectedTabIndex,
            backgroundColor = MaterialTheme.colors.surface.copy(alpha=0.5f),
            contentColor = MaterialTheme.colors.primary,
        ) {
            innerTabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = innerSelectedTabIndex == index,
                    onClick = { innerSelectedTabIndex = index },
                    text = { Text(title, fontWeight = if(innerSelectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        if (innerSelectedTabIndex == 0) {
            KeyCombinationCard(partCount = 2, isSecure = isSecure, keyType = keyType, keyLength = keyLength, forceOdd = parity == "ForceOdd")
        } else {
            KeyCombinationCard(partCount = 3, isSecure = isSecure, keyType = keyType, keyLength = keyLength, forceOdd = parity == "ForceOdd")
        }
    }
}

@Composable
private fun KeyCombinationCard(partCount: Int, isSecure: Boolean, keyType: String, keyLength: String, forceOdd: Boolean) {
    val keyParts = remember { mutableStateListOf<String>().apply { repeat(partCount) { add("") } } }
    val pinParts = remember { mutableStateListOf<String>().apply { repeat(partCount) { add("") } } }
    var combinedKey by remember { mutableStateOf("") }
    var kcv by remember { mutableStateOf("") }

    fun combineEntered(operation: String) {
        val inputs = keyParts.mapIndexed { index, key -> "Part ${index + 1}" to key }.toMap().toMutableMap()
        if (isSecure) pinParts.forEachIndexed { index, pin -> inputs["PIN ${index + 1}"] = pin }
        try {
            val parts = keyParts.mapIndexed { i, p ->
                require(p.isNotBlank()) { "Part ${i + 1} is empty" }
                val raw = p.hexToBytes()
                if (isSecure) { require(pinParts[i].isNotBlank()) { "PIN ${i + 1} is empty" }; KeyshareCryptoService.unprotect(raw, pinParts[i]) } else raw
            }
            val combined = KeyshareCryptoService.combine(parts, keyType, forceOdd)
            combinedKey = combined.toHexUpper()
            kcv = KeyshareCryptoService.computeKcv(combined, keyType)
            val partKcvs = parts.mapIndexed { i, b -> "Part ${i + 1} KCV: ${KeyshareCryptoService.computeKcv(b, keyType)}" }.joinToString("\n")
            KeyshareLogManager.logOperation(operation, inputs, "$partKcvs\nCombined Key: $combinedKey\nKCV: $kcv")
        } catch (e: Exception) {
            KeyshareLogManager.logOperation(operation, inputs, error = e.message)
        }
    }

    ModernCryptoCard(title = "$partCount-Part Key Generation", subtitle = "Generate random shares, or paste your own and combine them (XOR)", icon = Icons.Default.CallMerge) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            keyParts.forEachIndexed { index, _ ->
                KeyPartRow(
                    label = "Part ${index + 1}",
                    value = keyParts[index],
                    onValueChange = { keyParts[index] = it.uppercase() },
                    pin = pinParts[index],
                    onPinChange = { pinParts[index] = it.filter { c -> c.isDigit() } },
                    isSecure = isSecure
                )
            }
            Divider(Modifier.padding(vertical = 8.dp))
            KeyResultRow("Combined Key", combinedKey, kcv)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(
                    text = "Generate $partCount Parts",
                    onClick = {
                        val parts = KeyshareCryptoService.generateParts(partCount, keyType, keyLength, forceOdd)
                        parts.forEachIndexed { index, part ->
                            if (isSecure) {
                                val pin = KeyshareCryptoService.randomPin()
                                pinParts[index] = pin
                                keyParts[index] = KeyshareCryptoService.protect(part, pin).toHexUpper()
                            } else {
                                keyParts[index] = part.toHexUpper()
                            }
                        }
                        combineEntered("Generate & Combine")
                    },
                    modifier = Modifier.weight(1f)
                )
                ModernButton(
                    text = "Combine Entered Parts",
                    onClick = { combineEntered("Combine") },
                    enabled = keyParts.all { it.isNotBlank() },
                    icon = Icons.Default.CallMerge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun KeyPartRow(label: String, value: String, onValueChange: (String) -> Unit, pin: String, onPinChange: (String) -> Unit, isSecure: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EnhancedTextField(value = value, onValueChange = onValueChange, label = label, modifier = Modifier.weight(if (isSecure) 2f else 1f))
        if (isSecure) {
            EnhancedTextField(value = pin, onValueChange = onPinChange, label = "PIN", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun KeyResultRow(label: String, combinedValue: String, kcvValue: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FixedOutlinedTextField(value = combinedValue, onValueChange = {}, label = { Text(label) }, readOnly = true, modifier = Modifier.weight(2f))
        FixedOutlinedTextField(value = kcvValue, onValueChange = {}, label = { Text("KCV") }, readOnly = true, modifier = Modifier.weight(1f))
    }
}


// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.TextField(value, onValueChange, label, modifier, 1, null, ""); return }
    FixedOutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier)
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
private fun ModernDropdownField(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit, modifier: Modifier = Modifier) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Dropdown(label, value, options, onSelectionChanged, modifier); return }
    var expanded by remember { mutableStateOf(false) }
    var textFieldWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(modifier = modifier.onGloballyPositioned { textFieldWidth = it.size.width }) {
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "KeyshareButtonAnimation") { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    icon?.let {
                        Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(18.dp), tint = LocalContentColor.current)
                    }
                    Spacer(Modifier.width(8.dp))
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
