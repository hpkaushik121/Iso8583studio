package `in`.aicortex.iso8583studio.ui.screens.keys.keyBlocks

import `in`.aicortex.iso8583studio.data.model.FieldValidation
import `in`.aicortex.iso8583studio.data.model.ValidationState
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


private object ThalesKbValidationUtils {
    fun validateHex(value: String, friendlyName: String = "Field"): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$friendlyName cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        if (value.length % 2 != 0) {
            return FieldValidation(ValidationState.ERROR, "Hex string must have an even number of characters.")
        }
        return FieldValidation(ValidationState.VALID)
    }
}

// --- THALES KEY BLOCK SCREEN ---

private enum class ThalesKeyBlockTabs(val title: String, val icon: ImageVector) {
    ENCODE("Encode", Icons.Default.Lock),
    DECODE("Decode", Icons.Default.LockOpen)
}

private object ThalesKbLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
        addLog(LogEntry(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")), LogType.INFO, "Log history cleared", ""))
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

private object ThalesKbCryptoService {
    // This is a mock service.
    fun encode(plainKey: String): String {
        return "0072M3TC00E00025C8579B75703DE77E52D82258CB3F68BC0EFF357D1F46C19C956FC32"
    }

    fun decode(keyBlock: String): String {
        return "735B3125EFF2E04ABFBFA1670180A168"
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ThalesKeyBlockScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = ThalesKeyBlockTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "Thales Key Block", onBackClick = onBack) },
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
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn()) with
                                    (slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "thales_kb_tab_transition"
                    ) { tab ->
                        when (tab) {
                            ThalesKeyBlockTabs.ENCODE -> EncodeTab()
                            ThalesKeyBlockTabs.DECODE -> DecodeTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { ThalesKbLogManager.clearLogs() },
                            logEntries = ThalesKbLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// --- TABS ---

@Composable
private fun EncodeTab() {
    var desKbpk by remember { mutableStateOf("0123456789ABCDEF8080808080808080") }
    var desKcv by remember { mutableStateOf("8E0EC0") }
    var aesKbpk by remember { mutableStateOf("9B71333A13F9FAE72F9D0E2DAB4AAD6784") }
    var aesKcv by remember { mutableStateOf("DB3FB6") }

    var plainKey by remember { mutableStateOf("735B3125EFF2E04ABFBFA1670180A168") }
    var versionId by remember { mutableStateOf("1 - AES KBPK") }
    var keyUsage by remember { mutableStateOf("B0 - Base Derivation Key (BDK-1)") }
    var algorithm by remember { mutableStateOf("T - Triple DES") }
    var modeOfUse by remember { mutableStateOf("N - No special restrictions or not applicable") }
    var keyVersion by remember { mutableStateOf("00") }
    var exportability by remember { mutableStateOf("E - May only be exported in a trusted key block") }
    var optKeyBlocks by remember { mutableStateOf("00") }
    var lmkId by remember { mutableStateOf("02") }
    var optionalHeaders by remember { mutableStateOf("") }
    var randomPadding by remember { mutableStateOf("") }

    val isFormValid = ThalesKbValidationUtils.validateHex(plainKey, "Plain Key").state == ValidationState.VALID

    ModernCryptoCard(title = "Encode Key Block", subtitle = "Create a Thales encrypted key block", icon = Icons.Default.Lock) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KbpkRow("3DES KBPK:", desKbpk, { desKbpk = it }, desKcv, { desKcv = it })
            KbpkRow("AES KBPK:", aesKbpk, { aesKbpk = it }, aesKcv, { aesKcv = it })

            Divider(Modifier.padding(vertical=8.dp))

            FormRow("Plain Key:") { EnhancedTextField(value = plainKey, onValueChange = { plainKey = it.uppercase() }) }
            FormRow(label = "Version Id:") { ModernDropdownField(
                value = versionId,
                options = listOf("1 - AES KBPK", "0 - 3DES KBPK"),
                onSelectionChanged = {
                    versionId = if (it == 0) "1 - AES KBPK" else "0 - 3DES KBPK"
                }
            ) }
            FormRow("Key Usage:") { ModernDropdownField(value = keyUsage, options = remember { createKeyUsageOptions() }, onSelectionChanged = { keyUsage = createKeyUsageOptions()[it] }) }
            FormRow("Algorithm:") { ModernDropdownField(value = algorithm, options = remember { createAlgorithmOptions() }, onSelectionChanged = { algorithm = createAlgorithmOptions()[it] }) }
            FormRow("Mode of Use:") { ModernDropdownField(value = modeOfUse, options = remember { createModeOfUseOptions() }, onSelectionChanged = { modeOfUse = createModeOfUseOptions()[it] }) }
            FormRow("Key version#:") { EnhancedTextField(value = keyVersion, onValueChange = { keyVersion = it }) }
            FormRow("Exportability:") { ModernDropdownField(value = exportability, options = remember { createExportabilityOptions() }, onSelectionChanged = { exportability = createExportabilityOptions()[it] }) }
            FormRow("# Opt. KeyBlocks:") { EnhancedTextField(value = optKeyBlocks, onValueChange = { optKeyBlocks = it }) }
            FormRow("LMK ID:") { EnhancedTextField(value = lmkId, onValueChange = { lmkId = it }) }
            FormRow("Optional Headers:") { EnhancedTextField(value = optionalHeaders, onValueChange = { optionalHeaders = it }, maxLines = 3) }
            FormRow("Random Padding:") { EnhancedTextField(value = randomPadding, onValueChange = { randomPadding = it }) }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                IconButton(
                    onClick = {
                        val inputs = mapOf("Plain Key" to plainKey, "Version ID" to versionId, "Key Usage" to keyUsage)
                        val result = ThalesKbCryptoService.encode(plainKey)
                        ThalesKbLogManager.logOperation("Encode Key Block", inputs, "Key Block: $result")
                    },
                    enabled = isFormValid
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Encode", modifier = Modifier.size(32.dp), tint = if(isFormValid) MaterialTheme.colors.primary else Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun DecodeTab() {
    var keyBlock by remember { mutableStateOf("") }
    var dataInput by remember { mutableStateOf("ASCII") }

    val isFormValid = ThalesKbValidationUtils.validateHex(keyBlock, "Key Block").state == ValidationState.VALID

    ModernCryptoCard(title = "Decode Key Block", subtitle = "Extract a key from a Thales key block", icon = Icons.Default.LockOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = keyBlock, onValueChange = { keyBlock = it.uppercase() }, label = "Key Block (Hex)", maxLines = 8)

            Text("Data Input", style = MaterialTheme.typography.subtitle2)
            Row(Modifier.selectableGroup()) {
                RadioButton(selected = dataInput == "ASCII", onClick = { dataInput = "ASCII" })
                Text("ASCII", Modifier.padding(start = 4.dp, end = 16.dp))
                RadioButton(selected = dataInput == "Hexadecimal", onClick = { dataInput = "Hexadecimal" })
                Text("Hexadecimal", Modifier.padding(start = 4.dp))
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                IconButton(
                    onClick = {
                        val inputs = mapOf("Key Block" to keyBlock, "Data Input" to dataInput)
                        val result = ThalesKbCryptoService.decode(keyBlock)
                        ThalesKbLogManager.logOperation("Decode Key Block", inputs, "Plain Key: $result")
                    },
                    enabled = isFormValid
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = "Decode", modifier = Modifier.size(32.dp), tint = if(isFormValid) MaterialTheme.colors.primary else Color.Gray)
                }
            }
        }
    }
}


// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun KbpkRow(label: String, keyValue: String, onKeyChange: (String) -> Unit, kcvValue: String, onKcvChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.body2, modifier = Modifier.width(80.dp))
        EnhancedTextField(value = keyValue, onValueChange = onKeyChange, modifier = Modifier.weight(1f))
        EnhancedTextField(value = kcvValue, onValueChange = onKcvChange, modifier = Modifier.width(100.dp))
    }
}

@Composable
private fun FormRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.body2, modifier = Modifier.width(120.dp))
        content()
    }
}


@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String? = null, modifier: Modifier = Modifier, maxLines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { {Text(it)} },
        modifier = modifier,
        maxLines = maxLines,
        singleLine = maxLines == 1
    )
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
private fun ModernDropdownField(label: String?=null, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label ?: "") }, modifier = Modifier.fillMaxWidth(), readOnly = true,
            trailingIcon = { Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expanded = !expanded }) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.wrapContentWidth()) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = {
                    onSelectionChanged(index)
                    expanded = false
                }) {
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "ThalesKbButtonAnimation") { loading ->
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

// Helper functions to create dropdown options based on screenshots
private fun createKeyUsageOptions(): List<String> {
    return listOf(
        "B0 - Base Derivation Key (BDK-1)", "B1 - DUKPT Initial Key (IKEY/IPEK)", "C0 - Card Verification Key (Generic)",
        "E0 - EMV/Chip card MK: App. Cryptogram (MKAC)", "E1 - EMV/Chip card MK: Sec. Mesg for Conf. (MKSMC)", "E2 - EMV/Chip card MK: Sec. Mesg for Int. (MKSMI)",
        "P0 - PIN Encryption Key (Generic)", "V0 - PIN Verification Key (Generic)", "01 - WatchWord Key (WWK)",
        "63 - HMAC key (using SHA-256)"
        //... add all other key usages
    )
}

private fun createAlgorithmOptions(): List<String> {
    return listOf("T - Triple DES", "A - AES", "D - Single DES", "E - Elliptic Curve", "H - HMAC", "R - RSA", "S - DSA")
}

private fun createModeOfUseOptions(): List<String> {
    return listOf(
        "N - No special restrictions or not applicable", "B - Both encryption and decryption", "C - Both generate and verify MAC",
        "D - Decrypt only", "E - Encrypt only", "G - Generate MAC only", "S - Signature creation only", "V - Verify signature only",
        "X - Derivation of other keys only"
    )
}

private fun createExportabilityOptions(): List<String> {
    return listOf(
        "E - May only be exported in a trusted key block", "N - No export permitted", "S - Sensitive"
    )
}
