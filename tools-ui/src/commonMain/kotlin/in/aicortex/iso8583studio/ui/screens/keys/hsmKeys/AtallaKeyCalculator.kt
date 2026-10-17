package `in`.aicortex.iso8583studio.ui.screens.keys.hsmKeys

import `in`.aicortex.iso8583studio.ui.chrome.ToolTab

import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec

import `in`.aicortex.iso8583studio.ui.chrome.ToolLog

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import io.cryptocalc.hsm.VendorKeys

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
import `in`.aicortex.iso8583studio.ui.screens.Emv.AtrParserLogManager
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PersistentTabContent
import `in`.aicortex.iso8583studio.ui.screens.components.LogPanelWithAutoScroll
import kotlin.random.Random


private object AtallaValidationUtils {
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

// --- ATALLA KEYS SCREEN ---

private enum class AtallaKeysTabs(val title: String, val icon: ImageVector) {
    KEY_ENCRYPTION("Key Encryption", Icons.Default.Lock),
    AKB_DECODE("AKB Decode", Icons.Default.LockOpen),
}

private object AtallaLogManager {
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
        addLog(
            LogEntry(
                timestamp = timestamp,
                type = logType,
                message = message,
                details = details
            )
        )
    }
}


@Composable
fun AtallaKeysScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = AtallaKeysTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    val body: @Composable () -> Unit = {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        when (tab) {
                            AtallaKeysTabs.KEY_ENCRYPTION -> KeyEncryptionTab()
                            AtallaKeysTabs.AKB_DECODE -> AkbDecodeTab()
                        }
                    }
                
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "Atalla Keys Calculator", onBack = onBack, tabs = tabList.map { ToolTab(it.title, it.icon) }, selectedTab = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, log = ToolLog(AtallaLogManager.logEntries, onClear = { AtallaLogManager.clearLogs() })), content = body)
        return
    }
    Scaffold(
        topBar = { AppBarWithBack(title = "Atalla Keys Calculator", onBackClick = onBack) },
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
                            onClearClick = { AtallaLogManager.clearLogs() },
                            logEntries = AtallaLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// --- TABS ---

private fun atallaParity(key: ByteArray): String = when {
    ThalesLmk.hasOddParity(key) -> "Odd"
    ThalesLmk.hasEvenParity(key) -> "Even"
    else -> "Mixed / none"
}

private fun validateAkbHeader(header: String): ValidationResult = when {
    header.isEmpty() -> ValidationResult(ValidationState.EMPTY, "AKB header is required")
    header.length != 8 -> ValidationResult(ValidationState.ERROR, "AKB header must be 8 ASCII characters (e.g. 1PUNE000)")
    !header.all { it.code in 0x20..0x7E } -> ValidationResult(ValidationState.ERROR, "AKB header must be printable ASCII")
    else -> ValidationResult(ValidationState.VALID)
}

@Composable
private fun KeyEncryptionTab() {
    var key by remember { mutableStateOf("") }
    var akbHeader by remember { mutableStateOf("1PUNE000") }
    var mfkKey by remember { mutableStateOf("") }

    val isFormValid = AtallaValidationUtils.validateHex(key, "Key").state == ValidationState.VALID &&
            validateAkbHeader(akbHeader).state == ValidationState.VALID &&
            AtallaValidationUtils.validateHex(mfkKey, "MFK").state == ValidationState.VALID

    ModernCryptoCard(title = "Key Encryption", subtitle = "Reference implementation of the Atalla Key Block (header,key,MAC) - verify against the Atalla specification", icon = Icons.Default.Lock) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = key, onValueChange = { key = it.uppercase() }, label = "Key (Hex, 16/32/48)", validation = AtallaValidationUtils.validateHex(key, "Key"))
            EnhancedTextField(value = akbHeader, onValueChange = { akbHeader = it.uppercase().take(8) }, label = "AKB Header (8 ASCII chars)", validation = validateAkbHeader(akbHeader))
            EnhancedTextField(value = mfkKey, onValueChange = { mfkKey = it.uppercase() }, label = "MFK (Hex, 32/48)", validation = AtallaValidationUtils.validateHex(mfkKey, "MFK Key"))
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Encrypt Key",
                onClick = {
                    val inputs = mapOf("Key" to key, "AKB Header" to akbHeader, "MFK Key" to mfkKey)
                    try {
                        val keyBytes = key.hexToBytes()
                        val akb = VendorKeys.atallaEncode(keyBytes, akbHeader, mfkKey.hexToBytes())
                        AtallaLogManager.logOperation("Key Encryption", inputs, "AKB: $akb\nClear key KCV: ${ThalesLmk.kcv(keyBytes)}\nClear key parity: ${atallaParity(keyBytes)}")
                    } catch (e: Exception) {
                        AtallaLogManager.logOperation("Key Encryption", inputs, error = e.message)
                    }
                },
                enabled = isFormValid,
                icon = Icons.Default.EnhancedEncryption,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AkbDecodeTab() {
    val parityOptions = remember { listOf("None", "Odd", "Even") }

    var akb by remember { mutableStateOf("") }
    var checkKcv by remember { mutableStateOf(true) }
    var kcv by remember { mutableStateOf("") }
    var parity by remember { mutableStateOf(parityOptions.first()) }
    var mfkKey by remember { mutableStateOf("") }

    val isFormValid = akb.count { it == ',' } == 2 && AtallaValidationUtils.validateHex(mfkKey, "MFK Key").state == ValidationState.VALID

    ModernCryptoCard(title = "AKB Decode", subtitle = "Decrypt an Atalla Key Block and verify its MAC", icon = Icons.Default.LockOpen) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = akb, onValueChange = { akb = it.uppercase() }, label = "AKB (header,encryptedKey,mac)", maxLines = 4)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checkKcv, onCheckedChange = { checkKcv = it })
                Text("Check KCV?", style = MaterialTheme.typography.body2)
            }
            EnhancedTextField(value = kcv, onValueChange = { kcv = it.uppercase() }, label = "Expected KCV (Optional)", validation = AtallaValidationUtils.validateHex(kcv, "KCV"))
            ModernDropdownField(label = "Expected Parity", value = parity, options = parityOptions, onSelectionChanged = { parity = parityOptions[it] })
            EnhancedTextField(value = mfkKey, onValueChange = { mfkKey = it.uppercase() }, label = "MFK (Hex, 32/48)", validation = AtallaValidationUtils.validateHex(mfkKey, "MFK Key"))
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Decode AKB",
                onClick = {
                    val inputs = mapOf("AKB" to akb, "MFK Key" to mfkKey, "Parity" to parity, "KCV" to kcv)
                    try {
                        val d = VendorKeys.atallaDecode(akb, mfkKey.hexToBytes())
                        val actualKcv = ThalesLmk.kcv(d.key)
                        val actualParity = atallaParity(d.key)
                        val sb = StringBuilder("Header: ${d.header}\nMAC valid: ${d.macValid}\nDecrypted key: ${d.key.toHexUpper()}\nParity: $actualParity")
                        if (parity != "None") sb.append(if (actualParity == parity) " (matches)" else " (expected $parity)")
                        sb.append("\nKCV: $actualKcv")
                        if (checkKcv && kcv.isNotBlank()) sb.append(if (actualKcv.startsWith(kcv.uppercase())) " (matches)" else " (expected $kcv)")
                        AtallaLogManager.logOperation("AKB Decode", inputs, sb.toString())
                    } catch (e: Exception) {
                        AtallaLogManager.logOperation("AKB Decode", inputs, error = e.message)
                    }
                },
                enabled = isFormValid,
                icon = Icons.Default.VpnKeyOff,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult? = null) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.TextField(value, onValueChange, label, modifier, maxLines, validation, ""); return }
    Column(modifier = modifier) {
        FixedOutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), maxLines = maxLines,
            isError = validation?.state == ValidationState.ERROR,
        )
        if (validation?.message?.isNotEmpty() == true) {
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "AtallaButtonAnimation") { loading ->
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
