package `in`.aicortex.iso8583studio.ui.screens.tr31keyblock

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



private object Tr31ValidationUtils {
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

// --- TR-31 KEY BLOCK SCREEN ---

private enum class Tr31KeyBlockTabs(val title: String, val icon: ImageVector) {
    ENCODE("Encode", Icons.Default.Lock),
    DECODE("Decode", Icons.Default.LockOpen)
}

private object Tr31LogManager {
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

private object Tr31CryptoService {
    // This is a mock service.
    fun encode(plainKey: String): String {
        return "A0096K0TD12S0100KS1800604B120F929280000015BE1EA22731B03647031CEA17F516A5B7B14FC7D08BAA4377B803E1"
    }

    fun decode(keyBlock: String): String {
        return "F039121BEC83D26B169BDCD5B22AAF8F"
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Tr31KeyBlockScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = Tr31KeyBlockTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "TR-31 Key block", onBackClick = onBack) },
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
                        label = "tr31_tab_transition"
                    ) { tab ->
                        when (tab) {
                            Tr31KeyBlockTabs.ENCODE -> EncodeTab()
                            Tr31KeyBlockTabs.DECODE -> DecodeTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { Tr31LogManager.clearLogs() },
                            logEntries = Tr31LogManager.logEntries
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
    var kbpk by remember { mutableStateOf("89E88CF7931444F334BD7547FC3F380C") }
    var keyBlockVersion by remember { mutableStateOf("ANSI") }

    var plainKey by remember { mutableStateOf("F039121BEC83D26B169BDCD5B22AAF8F") }
    var header by remember { mutableStateOf("") }
    var versionId by remember { mutableStateOf("A - Key Variant Binding Method") }
    var keyUsage by remember { mutableStateOf("B0 - BDK Base Derivation Key") }
    var algorithm by remember { mutableStateOf("A - AES") }
    var modeOfUse by remember { mutableStateOf("B - Both Encrypt & Decrypt / Wrap & Unwrap") }
    var keyVersion by remember { mutableStateOf("00") }
    var exportability by remember { mutableStateOf("E - Exportable u. a KEK (meeting req. of X9.24 Pt. 1 or 2)") }
    var optKeyBlocks by remember { mutableStateOf("00") }
    var reserved by remember { mutableStateOf("00") }
    var optionalHeaders by remember { mutableStateOf("") }

    val isFormValid = Tr31ValidationUtils.validateHex(plainKey, "Plain Key").state == ValidationState.VALID

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernCryptoCard(title = "TR-31 Key Block", subtitle = "Create an encrypted key block", icon = Icons.Default.Lock) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormRow("KBPK:") { EnhancedTextField(value = kbpk, onValueChange = { kbpk = it.uppercase() }) }
                FormRow("Key Block version") {
                    Row {
                        RadioButton(selected = true, onClick = {})
                        Text("ANSI", Modifier.align(Alignment.CenterVertically))
                    }
                }
                Divider(Modifier.padding(vertical = 8.dp))

                FormRow("Plain Key:") { EnhancedTextField(value = plainKey, onValueChange = { plainKey = it.uppercase() }) }
                FormRow("Header:") { EnhancedTextField(value = header, onValueChange = { header = it.uppercase() }) }
                FormRow("Version Id:") { ModernDropdownField(value = versionId, options = listOf("A - Key Variant Binding Method", "B - Another method"), onSelectionChanged = { versionId = if(it==0) "A - Key Variant Binding Method" else "B - Another method" }) }
                FormRow("Key Usage:") { ModernDropdownField(value = keyUsage, options = listOf("B0 - BDK Base Derivation Key", "P0 - PIN Encryption Key"), onSelectionChanged = { keyUsage = if(it==0) "B0 - BDK Base Derivation Key" else "P0 - PIN Encryption Key" }) }
                FormRow("Algorithm:") { ModernDropdownField(value = algorithm, options = listOf("A - AES", "T - Triple DES"), onSelectionChanged = { algorithm = if(it==0) "A - AES" else "T - Triple DES" }) }
                FormRow("Mode of Use:") { ModernDropdownField(value = modeOfUse, options = listOf("B - Both Encrypt & Decrypt / Wrap & Unwrap", "E - Encrypt Only"), onSelectionChanged = { modeOfUse = if(it==0) "B - Both Encrypt & Decrypt / Wrap & Unwrap" else "E - Encrypt Only" }) }
                FormRow("Key version#:") { EnhancedTextField(value = keyVersion, onValueChange = { keyVersion = it }) }
                FormRow("Exportability:") { ModernDropdownField(value = exportability, options = listOf("E - Exportable u. a KEK (meeting req. of X9.24 Pt. 1 or 2)", "N - Not Exportable"), onSelectionChanged = { exportability = if(it==0) "E - Exportable u. a KEK (meeting req. of X9.24 Pt. 1 or 2)" else "N - Not Exportable" }) }
                FormRow("# Opt. KeyBlocks:") { EnhancedTextField(value = optKeyBlocks, onValueChange = { optKeyBlocks = it }) }
                FormRow("Reserved:") { EnhancedTextField(value = reserved, onValueChange = { reserved = it }) }
                FormRow("Optional Headers:") { EnhancedTextField(value = optionalHeaders, onValueChange = { optionalHeaders = it }, maxLines = 3) }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    IconButton(
                        onClick = {
                            val inputs = mapOf("Plain Key" to plainKey, "KBPK" to kbpk, "Version ID" to versionId, "Key Usage" to keyUsage)
                            val result = Tr31CryptoService.encode(plainKey)
                            Tr31LogManager.logOperation("Encode Key Block", inputs, "Key Block: $result")
                        },
                        enabled = isFormValid
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Encode", modifier = Modifier.size(32.dp), tint = if(isFormValid) MaterialTheme.colors.primary else Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun DecodeTab() {
    var keyBlock by remember { mutableStateOf("") }
    var dataInput by remember { mutableStateOf("ASCII") }

    val isFormValid = Tr31ValidationUtils.validateHex(keyBlock, "Key Block").state == ValidationState.VALID

    ModernCryptoCard(title = "Decode Key Block", subtitle = "Extract a key from a TR-31 key block", icon = Icons.Default.LockOpen) {
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
                        val result = Tr31CryptoService.decode(keyBlock)
                        Tr31LogManager.logOperation("Decode Key Block", inputs, "Plain Key: $result")
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
        modifier = modifier.fillMaxWidth(),
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
private fun ModernDropdownField(label:String? = null, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, label = label?.let{{Text(label)}}, modifier = Modifier.fillMaxWidth(), readOnly = true,
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

private fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2
    fillMaxWidth().wrapContentSize(Alignment.BottomStart).offset(x = indicatorOffset).width(indicatorWidth)
}
