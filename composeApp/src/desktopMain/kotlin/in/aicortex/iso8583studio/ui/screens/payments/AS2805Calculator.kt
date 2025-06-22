package `in`.aicortex.iso8583studio.ui.screens.payments

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
import kotlin.random.Random



private object As2805ValidationUtils {
    fun validateHex(value: String, friendlyName: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$friendlyName cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        if (value.length % 2 != 0) {
            return FieldValidation(ValidationState.ERROR, "Hex string must have an even number of characters.")
        }
        return FieldValidation(ValidationState.VALID)
    }
    fun validateNumeric(value: String, friendlyName: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$friendlyName cannot be empty.")
        if (value.any { !it.isDigit() }) {
            return FieldValidation(ValidationState.ERROR, "Only digits (0-9) are allowed.")
        }
        return FieldValidation(ValidationState.VALID)
    }
}

// --- AS2805 SCREEN ---

private enum class As2805Tabs(val title: String, val icon: ImageVector) {
    GENERATE_KEYS("Generate Terminal Key Set", Icons.Default.VpnKey),
    TRANSLATE_PIN("Translate PIN Block", Icons.Default.SyncAlt),
    MAC("MAC", Icons.Default.SyncLock),
    OWF("OWF", Icons.Default.Password)
}

private object As2805LogManager {
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

private object As2805CryptoService {
    // This is a mock service.
    private fun generateRandomHex(length: Int) = (1..length).map { Random.nextInt(0, 16).toString(16) }.joinToString("").uppercase()

    fun generateTerminalKeys(): String {
        return "Generated TPK: ${generateRandomHex(32)}\nGenerated TAK: ${generateRandomHex(32)}"
    }

    fun translatePinBlock(pinBlock: String): String {
        return "Translated PIN Block: ${pinBlock.reversed()}"
    }

    fun calculateMac(data: String): String {
        return "MAC: ${generateRandomHex(16)}"
    }

    fun calculateOwf(key: String, data: String): String {
        return "OWF Result: ${key.hashCode().toString(16)}${data.hashCode().toString(16)}".uppercase()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun As2805CalculatorScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = As2805Tabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "AS2805 Calculator", onBackClick = onBack) },
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
                                Text(tab.title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
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
                        label = "as2805_tab_transition"
                    ) { tab ->
                        when (tab) {
                            As2805Tabs.GENERATE_KEYS -> GenerateTerminalKeySetTab()
                            As2805Tabs.TRANSLATE_PIN -> TranslatePinBlockTab()
                            As2805Tabs.MAC -> MacTab()
                            As2805Tabs.OWF -> OwfTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { As2805LogManager.clearLogs() },
                            logEntries = As2805LogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// --- TABS ---

@Composable
private fun GenerateTerminalKeySetTab() {
    var keyFlag by remember { mutableStateOf("1") }
    var kekrKey by remember { mutableStateOf("") }
    var kekeScheme by remember { mutableStateOf("B") }
    var lmkScheme by remember { mutableStateOf("Z") }
    var kcvType by remember { mutableStateOf("1") }

    val isFormValid = As2805ValidationUtils.validateHex(kekrKey, "KEKr Key").state == ValidationState.VALID

    ModernCryptoCard(title = "Generate Terminal Key Set", subtitle = "Create Terminal Keys (TPK, TAK)", icon = Icons.Default.VpnKey) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernDropdownField("Key Flag", keyFlag, listOf("1", "2", "3"), { keyFlag = (it + 1).toString() })
            EnhancedTextField(value = kekrKey, onValueChange = { kekrKey = it.uppercase() }, label = "KEKr Key (Hex)", validation = As2805ValidationUtils.validateHex(kekrKey, "KEKr Key"))
            ModernDropdownField("Key Scheme KEKE", kekeScheme, listOf("B", "C", "H", "F", "G"), { kekeScheme = listOf("B", "C", "H", "F", "G")[it] })
            ModernDropdownField("Key Scheme LMK", lmkScheme, listOf("0", "T", "U", "X", "Y", "Z"), { lmkScheme = listOf("0", "T", "U", "X", "Y", "Z")[it] })
            ModernDropdownField("Key Check Value Type", kcvType, listOf("0", "1", "2"), { kcvType = it.toString() })

            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Generate Keys",
                onClick = {
                    val inputs = mapOf("Key Flag" to keyFlag, "KEKr Key" to kekrKey, "KEKE Scheme" to kekeScheme, "LMK Scheme" to lmkScheme, "KCV Type" to kcvType)
                    val result = As2805CryptoService.generateTerminalKeys()
                    As2805LogManager.logOperation("Generate Terminal Keys", inputs, result)
                },
                enabled = isFormValid,
                icon = Icons.Default.GeneratingTokens,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TranslatePinBlockTab() {
    var systemZpk by remember { mutableStateOf("") }
    var terminalTpk by remember { mutableStateOf("") }
    var stan by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var incomingFormat by remember { mutableStateOf("01") }
    var outgoingFormat by remember { mutableStateOf("01") }
    var incomingPinBlock by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }

    val isFormValid = listOf(systemZpk, terminalTpk, stan, amount, incomingPinBlock, accountNumber).all { it.isNotBlank() }

    ModernCryptoCard(title = "Translate PIN Block", subtitle = "Translate a PIN block from TPK to ZPK", icon = Icons.Default.SyncAlt) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = systemZpk, onValueChange = { systemZpk = it.uppercase() }, label = "System ZPK (Hex)", validation = As2805ValidationUtils.validateHex(systemZpk, "System ZPK"))
            EnhancedTextField(value = terminalTpk, onValueChange = { terminalTpk = it.uppercase() }, label = "Terminal TPK (Hex)", validation = As2805ValidationUtils.validateHex(terminalTpk, "Terminal TPK"))
            EnhancedTextField(value = stan, onValueChange = { stan = it }, label = "STAN", validation = As2805ValidationUtils.validateNumeric(stan, "STAN"))
            EnhancedTextField(value = amount, onValueChange = { amount = it }, label = "Transaction Amount", validation = As2805ValidationUtils.validateNumeric(amount, "Amount"))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)){
                ModernDropdownField("Incoming PIN Block Format", incomingFormat, listOf("01", "46"), { incomingFormat = if(it==0) "01" else "46" }, Modifier.weight(1f))
                ModernDropdownField("Outgoing PIN Block Format", outgoingFormat, listOf("01", "46"), { outgoingFormat = if(it==0) "01" else "46" }, Modifier.weight(1f))
            }
            EnhancedTextField(value = incomingPinBlock, onValueChange = { incomingPinBlock = it.uppercase() }, label = "Incoming PIN Block (Hex)", validation = As2805ValidationUtils.validateHex(incomingPinBlock, "Incoming PIN Block"))
            EnhancedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = "Account Number", validation = As2805ValidationUtils.validateNumeric(accountNumber, "Account Number"))
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Translate",
                onClick = {
                    val inputs = mapOf("System ZPK" to systemZpk, "Terminal TPK" to terminalTpk, "Incoming PIN" to incomingPinBlock)
                    val result = As2805CryptoService.translatePinBlock(incomingPinBlock)
                    As2805LogManager.logOperation("Translate PIN Block", inputs, result)
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MacTab() {
    var key by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }

    ModernCryptoCard(title = "MAC", subtitle = "Calculate Message Authentication Code", icon = Icons.Default.SyncLock) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = key, onValueChange = { key = it.uppercase() }, label = "Key (Hex)", validation = As2805ValidationUtils.validateHex(key, "Key"))
            EnhancedTextField(value = data, onValueChange = { data = it.uppercase() }, label = "Data (Hex)", validation = As2805ValidationUtils.validateHex(data, "Data"), maxLines = 8)
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Calculate MAC",
                onClick = {
                    val inputs = mapOf("Key" to key, "Data" to data)
                    val result = As2805CryptoService.calculateMac(data)
                    As2805LogManager.logOperation("MAC Calculation", inputs, result)
                },
                enabled = key.isNotBlank() && data.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OwfTab() {
    var key by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }

    ModernCryptoCard(title = "One-Way Function (OWF)", subtitle = "Calculate a one-way hash", icon = Icons.Default.Password) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = key, onValueChange = { key = it.uppercase() }, label = "Key (Hex)", validation = As2805ValidationUtils.validateHex(key, "Key"))
            EnhancedTextField(value = data, onValueChange = { data = it.uppercase() }, label = "Data (Hex)", validation = As2805ValidationUtils.validateHex(data, "Data"), maxLines = 8)
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Calculate OWF",
                onClick = {
                    val inputs = mapOf("Key" to key, "Data" to data)
                    val result = As2805CryptoService.calculateOwf(key, data)
                    As2805LogManager.logOperation("OWF Calculation", inputs, result)
                },
                enabled = key.isNotBlank() && data.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: FieldValidation? = null) {
    Column(modifier = modifier) {
        OutlinedTextField(
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
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), readOnly = true,
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "As2805ButtonAnimation") { loading ->
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
