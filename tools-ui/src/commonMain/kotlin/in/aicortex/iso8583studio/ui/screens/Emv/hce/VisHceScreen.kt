package `in`.aicortex.iso8583studio.ui.screens.Emv.hce

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import io.cryptocalc.crypto.engines.encryption.TdesCalculatorEngine

import io.cryptocalc.emv.VisaHce

import io.cryptocalc.emv.EmvKeys

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

object VisaHceValidationUtils {
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

// --- VISA HCE SCREEN ---

enum class VisaHceCryptoTabs(val title: String, val icon: ImageVector) {
    UDK("UDK", Icons.Default.VpnKey),
    LUK_KEY("LUK Key", Icons.Default.Key),
    MSD("MSD", Icons.Default.SdStorage),
    QVSDC("qVSDC", Icons.Default.Speed)
}

object VisaHceLogManager {
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
                val displayValue = if (key.contains("key", ignoreCase = true) || key.contains("udk", ignoreCase = true) || key.contains("mk", ignoreCase = true)) "${value.take(16)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(tabTitle, LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

@Composable
fun VisaHceCryptoScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = VisaHceCryptoTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "Visa HCE Crypto Calculator", onBackClick = onBack) },
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
                            VisaHceCryptoTabs.UDK -> UdkTabVisaHce()
                            VisaHceCryptoTabs.LUK_KEY -> LukKeyTab()
                            VisaHceCryptoTabs.MSD -> MsdTab()
                            VisaHceCryptoTabs.QVSDC -> QvsdcTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { VisaHceLogManager.clearLogs(selectedTab.title) },
                            logEntries = VisaHceLogManager.getLogEntries(selectedTab.title)
                        )
                    }
                }
            }
        }
    }
}

// --- TABS for Visa HCE ---

@Composable
private fun UdkTabVisaHce() {
    var mk by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var pan by remember { mutableStateOf("4999990011112222") }
    var panSeq by remember { mutableStateOf("01") }
    var udkOption by remember { mutableStateOf("Option A") }
    var keyParity by remember { mutableStateOf("Odd") }

    val isFormValid = VisaHceValidationUtils.validateHexString(mk, 32).state == ValidationState.VALID && pan.length in 12..19 && panSeq.length == 2

    ModernCryptoCard(title = "UDK Derivation", subtitle = "ICC master key from the issuer master key (EMV Book 2 A1.4)", icon = Icons.Default.VpnKey) {
        EnhancedTextField(value = mk, onValueChange = { mk = it.uppercase() }, label = "MK", validation = VisaHceValidationUtils.validateHexString(mk, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = pan, onValueChange = { pan = it.filter { c -> c.isDigit() } }, label = "PAN", validation = ValidationResult(if (pan.length in 12..19) ValidationState.VALID else ValidationState.ERROR, "12-19 digits"))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = panSeq, onValueChange = { panSeq = it.filter { c -> c.isDigit() }.take(2) }, label = "PAN Sequence No.", validation = ValidationResult(if (panSeq.length == 2) ValidationState.VALID else ValidationState.ERROR, "2 digits"))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernDropdownField(label = "UDK Derivation Option", value = udkOption, options = listOf("Option A", "Option B"), onSelectionChanged = { udkOption = if (it == 0) "Option A" else "Option B" }, modifier = Modifier.weight(1f))
            ModernDropdownField(label = "Key Parity", value = keyParity, options = listOf("Odd", "None"), onSelectionChanged = { keyParity = listOf("Odd", "None")[it] }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate UDK",
            onClick = {
                val inputs = mapOf("MK" to mk, "PAN" to pan, "PAN Seq" to panSeq, "Option" to udkOption, "Parity" to keyParity)
                try {
                    val udk = if (udkOption == "Option A") EmvKeys.deriveUdkOptionA(mk.hexToBytes(), pan, panSeq, keyParity == "Odd") else EmvKeys.deriveUdkOptionB(mk.hexToBytes(), pan, panSeq, keyParity == "Odd")
                    VisaHceLogManager.logOperation(VisaHceCryptoTabs.UDK.title, "UDK Generation", inputs, result = "UDK: ${udk.toHexUpper()}\nKCV: ${TdesCalculatorEngine.calculateKCV(udk).toHexUpper()}")
                } catch (e: Exception) { VisaHceLogManager.logOperation(VisaHceCryptoTabs.UDK.title, "UDK Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LukKeyTab() {
    var udk by remember { mutableStateOf("112233445566778899AABBCCDDEEFF00") }
    var currentYear by remember { mutableStateOf("25") }
    var currentHours by remember { mutableStateOf("0015") }
    var hourlyCounter by remember { mutableStateOf("01") }

    val isFormValid = VisaHceValidationUtils.validateHexString(udk, 32).state == ValidationState.VALID && currentYear.length == 2 && currentHours.length in 1..4 && hourlyCounter.length in 1..2

    ModernCryptoCard(title = "LUK Key Generation", subtitle = "Reference implementation: LUK = 3DES(UDK)[YY ‖ HHHH ‖ CC ‖ 8000] ‖ 3DES(UDK)[same ⊕ FF..FF] - verify against the Visa specification", icon = Icons.Default.Key) {
        EnhancedTextField(value = udk, onValueChange = { udk = it.uppercase() }, label = "UDK", validation = VisaHceValidationUtils.validateHexString(udk, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = currentYear, onValueChange = { currentYear = it.filter { c -> c.isDigit() }.take(2) }, label = "Current Year (YY)", validation = ValidationResult(if (currentYear.length == 2) ValidationState.VALID else ValidationState.ERROR, "2 digits"))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = currentHours, onValueChange = { currentHours = it.filter { c -> c.isDigit() }.take(4) }, label = "Hour of Year (HHHH, 0000-8783)", validation = ValidationResult(if (currentHours.isNotEmpty()) ValidationState.VALID else ValidationState.ERROR, "digits"))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = hourlyCounter, onValueChange = { hourlyCounter = it.filter { c -> c.isDigit() }.take(2) }, label = "Hourly Counter (CC)", validation = ValidationResult(if (hourlyCounter.isNotEmpty()) ValidationState.VALID else ValidationState.ERROR, "digits"))
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate LUK",
            onClick = {
                val inputs = mapOf("UDK" to udk, "Year" to currentYear, "Hours" to currentHours, "Counter" to hourlyCounter)
                try {
                    val luk = VisaHce.limitedUseKey(udk.hexToBytes(), currentYear, currentHours, hourlyCounter)
                    VisaHceLogManager.logOperation(VisaHceCryptoTabs.LUK_KEY.title, "LUK Generation", inputs, result = "LUK: ${luk.toHexUpper()}\nKCV: ${TdesCalculatorEngine.calculateKCV(luk).toHexUpper()}")
                } catch (e: Exception) { VisaHceLogManager.logOperation(VisaHceCryptoTabs.LUK_KEY.title, "LUK Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MsdTab() {
    var luk by remember { mutableStateOf("AABBCCDDEEFF00112233445566778899") }
    var pan by remember { mutableStateOf("4999990011112222") }
    var expiry by remember { mutableStateOf("2512") }
    var atc by remember { mutableStateOf("001A") }

    val isFormValid = VisaHceValidationUtils.validateHexString(luk, 32).state == ValidationState.VALID && pan.length in 12..19 && expiry.length == 4 && VisaHceValidationUtils.validateHexString(atc, 4).state == ValidationState.VALID

    ModernCryptoCard(title = "MSD Cryptogram (dCVV)", subtitle = "Dynamic CVV over the LUK: PAN ‖ ATC ‖ expiry ‖ service code 999, CVV decimalisation", icon = Icons.Default.SdStorage) {
        EnhancedTextField(value = luk, onValueChange = { luk = it.uppercase() }, label = "LUK", validation = VisaHceValidationUtils.validateHexString(luk, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = pan, onValueChange = { pan = it.filter { c -> c.isDigit() } }, label = "PAN", validation = ValidationResult(if (pan.length in 12..19) ValidationState.VALID else ValidationState.ERROR, "12-19 digits"))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = expiry, onValueChange = { expiry = it.filter { c -> c.isDigit() }.take(4) }, label = "Expiry (YYMM)", validation = ValidationResult(if (expiry.length == 4) ValidationState.VALID else ValidationState.ERROR, "YYMM"), modifier = Modifier.weight(1f))
            EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC (hex)", validation = VisaHceValidationUtils.validateHexString(atc, 4), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate MSD Cryptogram",
            onClick = {
                val inputs = mapOf("LUK" to luk, "PAN" to pan, "Expiry" to expiry, "ATC" to atc)
                try {
                    val dcvv = VisaHce.msdDynamicCvv(luk.hexToBytes(), pan, expiry, atc)
                    VisaHceLogManager.logOperation(VisaHceCryptoTabs.MSD.title, "MSD Generation", inputs, result = "dCVV: $dcvv")
                } catch (e: Exception) { VisaHceLogManager.logOperation(VisaHceCryptoTabs.MSD.title, "MSD Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.Shield, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QvsdcTab() {
    var luk by remember { mutableStateOf("AABBCCDDEEFF00112233445566778899") }
    var amount by remember { mutableStateOf("000000012345") }
    var amountOther by remember { mutableStateOf("000000000000") }
    var countryCode by remember { mutableStateOf("0840") }
    var tvr by remember { mutableStateOf("0000008000") }
    var currencyCode by remember { mutableStateOf("0840") }
    var transDate by remember { mutableStateOf("250611") }
    var transType by remember { mutableStateOf("00") }
    var un by remember { mutableStateOf("1A2B3C4D") }
    var aip by remember { mutableStateOf("7C00") }
    var atc by remember { mutableStateOf("001A") }
    var cvr by remember { mutableStateOf("0200") }

    val isFormValid = listOf(luk to 32, amount to 12, amountOther to 12, countryCode to 4, tvr to 10, currencyCode to 4, transDate to 6, transType to 2, un to 8, aip to 4, atc to 4).all { (v, l) -> VisaHceValidationUtils.validateHexString(v, l).state == ValidationState.VALID } && VisaHceValidationUtils.validateHexString(cvr, allowEmpty = true).state == ValidationState.VALID

    ModernCryptoCard(title = "qVSDC Cryptogram", subtitle = "ISO 9797-1 algorithm 3 (padding method 2) under the LUK over the CDOL data ‖ CVR", icon = Icons.Default.Speed) {
        EnhancedTextField(value = luk, onValueChange = { luk = it.uppercase() }, label = "LUK", validation = VisaHceValidationUtils.validateHexString(luk, 32))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = amount, onValueChange = { amount = it }, label = "Amount (9F02)", validation = VisaHceValidationUtils.validateHexString(amount, 12), modifier = Modifier.weight(1f))
            EnhancedTextField(value = amountOther, onValueChange = { amountOther = it }, label = "Amount, Other (9F03)", validation = VisaHceValidationUtils.validateHexString(amountOther, 12), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = countryCode, onValueChange = { countryCode = it.uppercase() }, label = "Country Code (9F1A)", validation = VisaHceValidationUtils.validateHexString(countryCode, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = tvr, onValueChange = { tvr = it.uppercase() }, label = "TVR (95)", validation = VisaHceValidationUtils.validateHexString(tvr, 10), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = currencyCode, onValueChange = { currencyCode = it.uppercase() }, label = "Currency Code (5F2A)", validation = VisaHceValidationUtils.validateHexString(currencyCode, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = transDate, onValueChange = { transDate = it.uppercase() }, label = "Transaction Date (9A)", validation = VisaHceValidationUtils.validateHexString(transDate, 6), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = transType, onValueChange = { transType = it.uppercase() }, label = "Transaction Type (9C)", validation = VisaHceValidationUtils.validateHexString(transType, 2), modifier = Modifier.weight(1f))
            EnhancedTextField(value = un, onValueChange = { un = it.uppercase() }, label = "Unpredictable No. (9F37)", validation = VisaHceValidationUtils.validateHexString(un, 8), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = aip, onValueChange = { aip = it.uppercase() }, label = "AIP (82)", validation = VisaHceValidationUtils.validateHexString(aip, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC (9F36)", validation = VisaHceValidationUtils.validateHexString(atc, 4), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = cvr, onValueChange = { cvr = it.uppercase() }, label = "CVR / IAD tail (from 9F10)", validation = VisaHceValidationUtils.validateHexString(cvr, allowEmpty = true))
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate qVSDC Cryptogram",
            onClick = {
                val inputs = mapOf("LUK" to luk, "Amount" to amount, "TVR" to tvr, "ATC" to atc, "CVR" to cvr)
                try {
                    val data = (amount + amountOther + countryCode + tvr + currencyCode + transDate + transType + un + aip + atc + cvr).hexToBytes()
                    val ac = VisaHce.qvsdcCryptogram(luk.hexToBytes(), data)
                    VisaHceLogManager.logOperation(VisaHceCryptoTabs.QVSDC.title, "qVSDC Generation", inputs, result = "Data (${data.size} bytes): ${data.toHexUpper()}\nqVSDC Cryptogram: ${ac.toHexUpper()}")
                } catch (e: Exception) { VisaHceLogManager.logOperation(VisaHceCryptoTabs.QVSDC.title, "qVSDC Generation", inputs, error = e.message) }
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "VisaHceButtonAnimation") { loading ->
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
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
