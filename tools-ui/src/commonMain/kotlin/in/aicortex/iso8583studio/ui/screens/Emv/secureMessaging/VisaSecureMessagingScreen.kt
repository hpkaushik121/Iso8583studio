package `in`.aicortex.iso8583studio.ui.screens.Emv.secureMessaging

import `in`.aicortex.iso8583studio.ui.chrome.toolScroll

import `in`.aicortex.iso8583studio.ui.chrome.ToolTab

import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec

import `in`.aicortex.iso8583studio.ui.chrome.ToolLog

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import io.cryptocalc.emv.EmvSecureMessaging

import io.cryptocalc.emv.EmvSessionKeys

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.PersistentTabContent
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.LogPanelWithAutoScroll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// --- COMMON UI & VALIDATION FOR THIS SCREEN ---

object VisaValidationUtils {
    fun validateHexString(value: String, expectedLength: Int? = null, allowEmpty: Boolean = false, friendlyName: String = "Field"): ValidationResult {
        if (value.isEmpty()) {
            return if (allowEmpty) ValidationResult(ValidationState.EMPTY, "", "Enter hex characters")
            else ValidationResult(ValidationState.ERROR, "$friendlyName is required", "Enter hex characters")
        }
        if (!value.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed", "${value.length} chars")
        }
        if (value.length % 2 != 0) {
            return ValidationResult(ValidationState.ERROR, "Must have an even number of characters", "${value.length} chars")
        }
        expectedLength?.let {
            if (value.length != it) return ValidationResult(ValidationState.ERROR, "Must be exactly $it characters", "${value.length}/$it chars")
        }
        return ValidationResult(ValidationState.VALID, "", "${value.length} chars")
    }
}

// --- VISA SECURE MESSAGING SCREEN ---

enum class VisaSecureMessagingTabs(val title: String, val icon: ImageVector) {
    SESSION_KEY("Session Key", Icons.Default.VpnKey),
    PIN("PIN Block", Icons.Default.Password),
    MAC("MAC", Icons.Default.SyncLock)
}

object VisaLogManager {
    private val _logEntriesMap = mutableStateMapOf<String, SnapshotStateList<LogEntry>>()

    private fun getLogList(tabTitle: String): SnapshotStateList<LogEntry> = _logEntriesMap.getOrPut(tabTitle) { mutableStateListOf() }
    fun getLogEntries(tabTitle: String): SnapshotStateList<LogEntry> = getLogList(tabTitle)

    fun clearLogs(tabTitle: String) {
        getLogList(tabTitle).clear()
    }

    private fun addLog(tabTitle: String, entry: LogEntry) {
        val logList = getLogList(tabTitle)
        logList.add(0, entry)
        if (logList.size > 500) logList.removeRange(400, logList.size)
    }

    fun logOperation(tabTitle: String, operation: String, inputs: Map<String, String>, result: String? = null, error: String? = null, executionTime: Long = 0L) {
        if (result == null && error == null) return
        Analytics.calculationRun(operation, success = result != null, durationMs = executionTime)

        val timestamp = nowLogTimestamp()
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                val displayValue = if (key.contains("key", ignoreCase = true) || key.contains("udk", ignoreCase = true)) "${value.take(16)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(tabTitle,  LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

@Composable
fun VisaSecureMessagingScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = VisaSecureMessagingTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    val body: @Composable () -> Unit = {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        when (tab) {
                            VisaSecureMessagingTabs.SESSION_KEY -> SessionKeyTabVisa()
                            VisaSecureMessagingTabs.PIN -> PinTabVisa()
                            VisaSecureMessagingTabs.MAC -> MacTabVisa()
                        }
                    }
                
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "Visa Secure Messaging", onBack = onBack, tabs = tabList.map { ToolTab(it.title, it.icon) }, selectedTab = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, log = ToolLog(VisaLogManager.getLogEntries(selectedTab.title), onClear = { VisaLogManager.clearLogs(selectedTab.title) })), content = body)
        return
    }
    Scaffold(
        topBar = { AppBarWithBack(title = "Visa Secure Messaging", onBackClick = onBack) },
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
                            onClearClick = { VisaLogManager.clearLogs(selectedTab.title) },
                            logEntries = VisaLogManager.getLogEntries(selectedTab.title)
                        )
                    }
                }
            }
        }
    }
}

// --- TABS for Visa Secure Messaging ---

@Composable
private fun SessionKeyTabVisa() {
    val keyTargets = remember { listOf("SK-SMI (MAC)", "SK-SMC (ENC)") }
    var udk by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var atc by remember { mutableStateOf("001A") }
    var keyTarget by remember { mutableStateOf(keyTargets.first()) }

    val isFormValid = VisaValidationUtils.validateHexString(udk, 32).state == ValidationState.VALID && VisaValidationUtils.validateHexString(atc, 4).state == ValidationState.VALID

    ModernCryptoCard(title = "Session Key Generation", subtitle = "Visa SM session key: ATC XORed into the last 2 bytes of key A, its complement into key B, odd parity (CVN 10/18)", icon = Icons.Default.VpnKey) {
        EnhancedTextField(value = udk, onValueChange = { udk = it.uppercase() }, label = "UDK (ICC MK-SMI or MK-SMC)", validation = VisaValidationUtils.validateHexString(udk, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC (9F36)", validation = VisaValidationUtils.validateHexString(atc, 4))
        Spacer(Modifier.height(8.dp))
        ModernDropdownField(label = "Key Target (label only)", value = keyTarget, options = keyTargets, onSelectionChanged = { keyTarget = keyTargets[it] })
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate Session Key",
            onClick = {
                val inputs = mapOf("UDK" to udk, "ATC" to atc, "Key Target" to keyTarget)
                try {
                    val sk = EmvSessionKeys.deriveVisaSmSessionKey(udk.hexToBytes(), atc.hexToBytes())
                    VisaLogManager.logOperation(VisaSecureMessagingTabs.SESSION_KEY.title, "Session Key Generation", inputs, result = "$keyTarget: ${sk.toHexUpper()}")
                } catch (e: Exception) { VisaLogManager.logOperation(VisaSecureMessagingTabs.SESSION_KEY.title, "Session Key Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PinTabVisa() {
    var skEnc by remember { mutableStateOf("A1B2C3D4E5F67890A1B2C3D4E5F67890") }
    var udkAc by remember { mutableStateOf("11223344556677881122334455667788") }
    var newPin by remember { mutableStateOf("1234") }
    var currentPin by remember { mutableStateOf("") }

    val isFormValid = VisaValidationUtils.validateHexString(skEnc, 32).state == ValidationState.VALID && VisaValidationUtils.validateHexString(udkAc, 32).state == ValidationState.VALID && newPin.length in 4..12 && newPin.all { it.isDigit() }

    ModernCryptoCard(title = "PIN Block Generation", subtitle = "VIS PIN block (length ‖ PIN ‖ F.. XOR 00×4 ‖ bytes 4-7 of UDK-AC key A, optionally XOR current PIN), then length byte ‖ block, padding method 2, TDES-ECB under SK-SMC", icon = Icons.Default.Password) {
        EnhancedTextField(value = skEnc, onValueChange = { skEnc = it.uppercase() }, label = "SK-SMC (session key for confidentiality)", validation = VisaValidationUtils.validateHexString(skEnc, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = udkAc, onValueChange = { udkAc = it.uppercase() }, label = "UDK-AC (ICC master key for AC)", validation = VisaValidationUtils.validateHexString(udkAc, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = newPin, onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(12) }, label = "New PIN (4-12 digits)", validation = ValidationResult(if (newPin.length in 4..12) ValidationState.VALID else ValidationState.ERROR, "4-12 digits"))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = currentPin, onValueChange = { currentPin = it.filter { c -> c.isDigit() }.take(12) }, label = "Current PIN (optional, for PIN change with verification)", validation = ValidationResult(ValidationState.VALID))
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate PIN Block",
            onClick = {
                val inputs = mapOf("SK-SMC" to skEnc, "UDK-AC" to udkAc, "New PIN" to "****", "Current PIN" to (if (currentPin.isBlank()) "" else "****"))
                try {
                    val block = EmvSecureMessaging.visPinBlock(udkAc.hexToBytes(), newPin, currentPin.ifBlank { null })
                    val enc = EmvSecureMessaging.encryptCommandData(skEnc.hexToBytes(), block, EmvSecureMessaging.EncryptionType.VISA)
                    val header = if (currentPin.isBlank()) "8424000218" else "8424000118"
                    VisaLogManager.logOperation(VisaSecureMessagingTabs.PIN.title, "PIN Block Generation", inputs, result = "VIS PIN block: ${block.toHexUpper()}\nEnciphered: ${enc.toHexUpper()}\nPIN CHANGE/UNBLOCK command: $header${enc.toHexUpper()} ‖ MAC")
                } catch (e: Exception) { VisaLogManager.logOperation(VisaSecureMessagingTabs.PIN.title, "PIN Block Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.EnhancedEncryption, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MacTabVisa() {
    var skMac by remember { mutableStateOf("A1B2C3D4E5F67890A1B2C3D4E5F67890") }
    var cla by remember { mutableStateOf("84") }
    var ins by remember { mutableStateOf("D8") }
    var p1 by remember { mutableStateOf("50") }
    var p2 by remember { mutableStateOf("00") }
    var atc by remember { mutableStateOf("001A") }
    var ac by remember { mutableStateOf("1234567890ABCDEF") }
    var payload by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var macLength by remember { mutableStateOf("8") }

    val isFormValid = listOf(skMac to 32, cla to 2, ins to 2, p1 to 2, p2 to 2, atc to 4, ac to 16).all { (v, l) -> VisaValidationUtils.validateHexString(v, l).state == ValidationState.VALID } && VisaValidationUtils.validateHexString(payload, allowEmpty = true).state == ValidationState.VALID

    ModernCryptoCard(title = "MAC Calculation", subtitle = "ISO 9797-1 algorithm 3, padding method 2, over CLA INS P1 P2 Lc ‖ ATC ‖ ARQC ‖ data under SK-SMI", icon = Icons.Default.SyncLock) {
        EnhancedTextField(value = skMac, onValueChange = { skMac = it.uppercase() }, label = "SK-SMI", validation = VisaValidationUtils.validateHexString(skMac, 32))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnhancedTextField(value = cla, onValueChange = { cla = it.uppercase() }, label = "CLA", validation = VisaValidationUtils.validateHexString(cla, 2), modifier = Modifier.weight(1f))
            EnhancedTextField(value = ins, onValueChange = { ins = it.uppercase() }, label = "INS", validation = VisaValidationUtils.validateHexString(ins, 2), modifier = Modifier.weight(1f))
            EnhancedTextField(value = p1, onValueChange = { p1 = it.uppercase() }, label = "P1", validation = VisaValidationUtils.validateHexString(p1, 2), modifier = Modifier.weight(1f))
            EnhancedTextField(value = p2, onValueChange = { p2 = it.uppercase() }, label = "P2", validation = VisaValidationUtils.validateHexString(p2, 2), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC (9F36)", validation = VisaValidationUtils.validateHexString(atc, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = ac, onValueChange = { ac = it.uppercase() }, label = "ARQC (9F26)", validation = VisaValidationUtils.validateHexString(ac, 16), modifier = Modifier.weight(2f))
        }
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = payload, onValueChange = { payload = it.uppercase() }, label = "Command Data (optional)", validation = VisaValidationUtils.validateHexString(payload, allowEmpty = true), maxLines = 3)
        Spacer(Modifier.height(8.dp))
        ModernDropdownField(label = "MAC length", value = macLength, options = listOf("4", "8"), onSelectionChanged = { macLength = listOf("4", "8")[it] })
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate MAC",
            onClick = {
                val inputs = mapOf("SK-SMI" to skMac, "CLA" to cla, "INS" to ins, "P1" to p1, "P2" to p2, "ATC" to atc, "ARQC" to ac, "Data" to payload)
                try {
                    val data = if (payload.isBlank()) ByteArray(0) else payload.hexToBytes()
                    val len = macLength.toInt()
                    val header = (cla + ins + p1 + p2).hexToBytes() + byteArrayOf((data.size + len).toByte())
                    val macInput = header + atc.hexToBytes() + ac.hexToBytes() + data
                    val mac = EmvSecureMessaging.commandMac(skMac.hexToBytes(), macInput, len)
                    VisaLogManager.logOperation(VisaSecureMessagingTabs.MAC.title, "MAC Generation", inputs, result = "MAC input: ${macInput.toHexUpper()}\nMAC: ${mac.toHexUpper()}\nFull command: ${header.toHexUpper()}${data.toHexUpper()}${mac.toHexUpper()}")
                } catch (e: Exception) { VisaLogManager.logOperation(VisaSecureMessagingTabs.MAC.title, "MAC Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.Shield, modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- SHARED UI COMPONENTS ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.TextField(value, onValueChange, label, modifier, maxLines, validation, ""); return }
    Column(modifier = modifier) {
        FixedOutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), maxLines = maxLines,
            isError = validation.state == ValidationState.ERROR,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.primary
                    ValidationState.WARNING -> Color(0xFFFFC107)
                    ValidationState.ERROR -> MaterialTheme.colors.error
                    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                },
                unfocusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    ValidationState.WARNING -> Color(0xFFFFC107)
                    ValidationState.ERROR -> MaterialTheme.colors.error
                    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                }
            )
        )
        Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (validation.message.isNotEmpty()) {
                Text(text = validation.message, color = when (validation.state) {
                    ValidationState.ERROR -> MaterialTheme.colors.error
                    ValidationState.WARNING -> Color(0xFF856404)
                    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                }, style = MaterialTheme.typography.caption)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = validation.helperText, color = if (validation.state == ValidationState.VALID) SuccessGreen else MaterialTheme.colors.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.caption, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun ModernCryptoCard(title: String, subtitle: String, icon: ImageVector, onInfoClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Card(title, subtitle, icon, onInfoClick, content); return }
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "VisaSecureMessagingButtonAnimation") { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun InfoDialog(title: String, onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.InfoDialog(title, onDismissRequest, content); return }
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
                Column(modifier = Modifier.toolScroll(rememberScrollState())) {
                    content()
                }
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
