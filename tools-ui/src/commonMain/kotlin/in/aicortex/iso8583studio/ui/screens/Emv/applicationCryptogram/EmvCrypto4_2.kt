package `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram

import `in`.aicortex.iso8583studio.ui.chrome.toolScroll

import `in`.aicortex.iso8583studio.ui.chrome.ToolTab

import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec

import `in`.aicortex.iso8583studio.ui.chrome.ToolLog

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import io.cryptocalc.crypto.engines.encryption.TdesCalculatorEngine

import io.cryptocalc.crypto.primitives.Mac

import io.cryptocalc.emv.EmvSessionKeys

import io.cryptocalc.emv.EmvCvn

import io.cryptocalc.emv.EmvAc

import io.cryptocalc.emv.EmvKeys

import ai.cortex.core.toHexUpper

import ai.cortex.core.hexToBytes

import `in`.aicortex.iso8583studio.platform.nowLogTimestamp

import `in`.aicortex.iso8583studio.analytics.Analytics
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.PersistentTabContent
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.LogPanelWithAutoScroll
import ai.cortex.core.types.PaddingMethods
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// --- COMMON UI & VALIDATION FOR EMV 4.2 SCREEN ---

object Emv42ValidationUtils {
    fun validateHexString(value: String, expectedLength: Int? = null, allowEmpty: Boolean = false): ValidationResult {
        if (value.isEmpty()) {
            return if (allowEmpty) ValidationResult(ValidationState.EMPTY, "", "Enter hex characters")
            else ValidationResult(ValidationState.ERROR, "Field is required", "Enter hex characters")
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

    fun validatePAN(pan: String): ValidationResult {
        if (pan.isEmpty()) return ValidationResult(ValidationState.ERROR, "PAN is required", "Enter 13-19 digits")
        if (!pan.all { it.isDigit() }) return ValidationResult(ValidationState.ERROR, "PAN must contain only digits", "${pan.length} chars")
        if (pan.length !in 13..19) return ValidationResult(ValidationState.ERROR, "PAN must be 13-19 digits", "${pan.length}/19 digits")
        return ValidationResult(ValidationState.VALID, "", "${pan.length} digits")
    }

    fun validateNumericString(value: String, expectedLength: Int? = null, allowEmpty: Boolean = false): ValidationResult {
        if (value.isEmpty()) {
            return if (allowEmpty) ValidationResult(ValidationState.EMPTY, "", "Enter digits")
            else ValidationResult(ValidationState.ERROR, "Field is required", "Enter digits")
        }
        if (!value.all { it.isDigit() }) return ValidationResult(ValidationState.ERROR, "Only digits allowed", "${value.length} chars")
        expectedLength?.let {
            if (value.length != it) return ValidationResult(ValidationState.ERROR, "Must be exactly $it digits", "${value.length}/$it digits")
        }
        return ValidationResult(ValidationState.VALID, "", "${value.length} digits")
    }
}

// --- EMV 4.2 SCREEN ---

enum class Emv42CryptoTabs(val title: String, val icon: ImageVector) {
    UDK_DERIVATION("UDK Derivation", Icons.Default.Key),
    SESSION_KEYS("Session Keys", Icons.Default.VpnKey),
    CRYPTOGRAM("Cryptogram", Icons.Default.Security),
    ARPC("ARPC", Icons.Default.Reply),
    UTILITIES("Utilities", Icons.Default.Build)
}

object Emv42LogManager {
    private val _logEntriesMap = mutableStateMapOf<String, SnapshotStateList<LogEntry>>()

    private fun getLogList(tabTitle: String): SnapshotStateList<LogEntry> {
        return _logEntriesMap.getOrPut(tabTitle) { mutableStateListOf() }
    }

    fun getLogEntries(tabTitle: String): SnapshotStateList<LogEntry> = getLogList(tabTitle)

    fun clearLogs(tabTitle: String) {
        getLogList(tabTitle).clear()
        addLog(
            tabTitle,
            LogEntry(
                timestamp = nowLogTimestamp(),
                type = LogType.INFO,
                message = "Log history for this tab cleared",
                details = ""
            )
        )
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
                val displayValue = if (key.contains("key", ignoreCase = true) || key.contains("pan", ignoreCase = true) || key.contains("modulus", ignoreCase = true)) {
                    "${value.take(16)}..."
                } else {
                    value
                }
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  ${it.replace("\n", "\n  ")}") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(tabTitle,  LogEntry(timestamp = timestamp, type= logType, message = message, details = details))
    }
}

@Composable
fun EmvCrypto4_2(
    
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = Emv42CryptoTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    val body: @Composable () -> Unit = {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        when (tab) {
                            Emv42CryptoTabs.UDK_DERIVATION -> UdkDerivationTab42()
                            Emv42CryptoTabs.SESSION_KEYS -> SessionKeysTab42()
                            Emv42CryptoTabs.CRYPTOGRAM -> CryptogramTab42()
                            Emv42CryptoTabs.ARPC -> ArpcTab42()
                            Emv42CryptoTabs.UTILITIES -> UtilitiesTab42()
                        }
                    }
                
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "EMV 4.2 Crypto Calculator", onBack = onBack, tabs = tabList.map { ToolTab(it.title, it.icon) }, selectedTab = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, log = ToolLog(Emv42LogManager.getLogEntries(selectedTab.title), onClear = { Emv42LogManager.clearLogs(selectedTab.title) })), content = body)
        return
    }
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "EMV 4.2 Crypto Calculator",
                onBackClick = onBack,
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(modifier = Modifier.customTabIndicatorOffset(tabPositions[selectedTabIndex]), height = 3.dp, color = MaterialTheme.colors.primary)
                }
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
                            onClearClick = { Emv42LogManager.clearLogs(selectedTab.title) },
                            logEntries = Emv42LogManager.getLogEntries(selectedTab.title)
                        )
                    }
                }
            }
        }
    }
}

// --- TABS for EMV 4.2 ---

/** ARC as two ASCII characters ("00", "Z3") or four hex digits ("3030"). */
private fun arcToBytes(arc: String): ByteArray = when {
    arc.length == 2 -> EmvAc.arcBytes(arc.uppercase())
    arc.length == 4 && arc.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' } -> arc.hexToBytes()
    else -> throw IllegalArgumentException("Response code must be 2 characters or 4 hex digits")
}

private fun kcvOf(key: ByteArray): String = TdesCalculatorEngine.calculateKCV(key).toHexUpper()

@Composable
private fun SessionKeysTab42() {
    var masterKey by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var atc by remember { mutableStateOf("0001") }
    var parity by remember { mutableStateOf("Odd") }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(title = "EMV 4.2 Session Key Derivation", onDismissRequest = { showInfoDialog = false }) {
            Text("Common Session Key (EMV 4.2 Book 2 Annex A1.3.1): SK = 3DES(MK)[ATC ‖ F0 ‖ 00×5] ‖ 3DES(MK)[ATC ‖ 0F ‖ 00×5], optionally adjusted to odd parity.", style = MaterialTheme.typography.body2)
        }
    }

    val isFormValid = listOf(Emv42ValidationUtils.validateHexString(masterKey, 32), Emv42ValidationUtils.validateHexString(atc, 4)).none { it.state == ValidationState.ERROR }

    Column(modifier = Modifier.fillMaxSize().toolScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernCryptoCard(title = "Session Key Derivation", subtitle = "EMV 4.2 Common Session Key Method", icon = Icons.Default.VpnKey, onInfoClick = { showInfoDialog = true }) {
            EnhancedTextField(value = masterKey, onValueChange = { masterKey = it.uppercase() }, label = "Master Key (UDK)", validation = Emv42ValidationUtils.validateHexString(masterKey, 32))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "Application Transaction Counter (ATC)", validation = Emv42ValidationUtils.validateHexString(atc, 4))
            Spacer(Modifier.height(4.dp))
            ModernDropdownField(label = "Key Parity", value = parity, options = listOf("Odd", "None"), onSelectionChanged = { parity = if (it == 0) "Odd" else "None" })
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Generate Session Key",
                onClick = {
                    val inputs = mapOf("Master Key" to masterKey, "ATC" to atc, "Parity" to parity)
                    try {
                        val sk = EmvKeys.deriveCommonSessionKey(masterKey.hexToBytes(), atc.hexToBytes(), parity == "Odd")
                        Emv42LogManager.logOperation(Emv42CryptoTabs.SESSION_KEYS.title, "Session Key Generation", inputs, result = "Session Key: ${sk.toHexUpper()}\nKCV: ${kcvOf(sk)}")
                    } catch (e: Exception) {
                        Emv42LogManager.logOperation(Emv42CryptoTabs.SESSION_KEYS.title, "Session Key Generation", inputs, error = e.message)
                    }
                },
                enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun UdkDerivationTab42() {
    var mdk by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var pan by remember { mutableStateOf("43219876543210987") }
    var panSeq by remember { mutableStateOf("00") }
    var option by remember { mutableStateOf("Option A") }
    var parity by remember { mutableStateOf("Odd") }

    val isFormValid = listOf(Emv42ValidationUtils.validateHexString(mdk, 32), Emv42ValidationUtils.validatePAN(pan), Emv42ValidationUtils.validateNumericString(panSeq, 2)).all { it.state != ValidationState.ERROR }

    ModernCryptoCard(title = "UDK Derivation", subtitle = "EMV 4.2 Book 2 Annex A1.4: Option A (PAN ≤ 16 digits) or Option B (SHA-1 decimalisation)", icon = Icons.Default.Key) {
        EnhancedTextField(value = mdk, onValueChange = { mdk = it.uppercase() }, label = "Master Derivation Key (MDK)", validation = Emv42ValidationUtils.validateHexString(mdk, 32))
        Spacer(Modifier.height(4.dp))
        EnhancedTextField(value = pan, onValueChange = { pan = it }, label = "Primary Account Number (PAN)", validation = Emv42ValidationUtils.validatePAN(pan))
        Spacer(Modifier.height(4.dp))
        EnhancedTextField(value = panSeq, onValueChange = { panSeq = it }, label = "PAN Sequence Number", validation = Emv42ValidationUtils.validateNumericString(panSeq, 2))
        Spacer(Modifier.height(4.dp))
        ModernDropdownField(label = "Derivation Option", value = option, options = listOf("Option A", "Option B"), onSelectionChanged = { option = if (it == 0) "Option A" else "Option B" })
        Spacer(Modifier.height(4.dp))
        ModernDropdownField(label = "Key Parity", value = parity, options = listOf("Odd", "None"), onSelectionChanged = { parity = if (it == 0) "Odd" else "None" })
        Spacer(Modifier.height(8.dp))
        ModernButton(
            text = "Calculate UDK",
            onClick = {
                val inputs = mapOf("MDK" to mdk, "PAN" to pan, "PAN Sequence" to panSeq, "Option" to option, "Parity" to parity)
                try {
                    val udk = if (option == "Option A") EmvKeys.deriveUdkOptionA(mdk.hexToBytes(), pan, panSeq, parity == "Odd") else EmvKeys.deriveUdkOptionB(mdk.hexToBytes(), pan, panSeq, parity == "Odd")
                    Emv42LogManager.logOperation(Emv42CryptoTabs.UDK_DERIVATION.title, "UDK Derivation", inputs, result = "UDK: ${udk.toHexUpper()}\nKCV: ${kcvOf(udk)}")
                } catch (e: Exception) {
                    Emv42LogManager.logOperation(Emv42CryptoTabs.UDK_DERIVATION.title, "UDK Derivation", inputs, error = e.message)
                }
            },
            enabled = isFormValid, icon = Icons.Default.Calculate, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CryptogramTab42() {
    var sessionKey by remember { mutableStateOf("D920B6730B9267079220F8491F2FCD68") }
    var terminalData by remember { mutableStateOf("000000010000000000000000084000000080000840250611001A2B3C4D") }
    var iccData by remember { mutableStateOf("7C000001") }
    var paddingMethod by remember { mutableStateOf("Method 2 (80 00..)") }
    val paddings = listOf("Method 1 (00 00..)", "Method 2 (80 00..)")

    val isFormValid = listOf(
        Emv42ValidationUtils.validateHexString(sessionKey, 32),
        Emv42ValidationUtils.validateHexString(terminalData, allowEmpty = true),
        Emv42ValidationUtils.validateHexString(iccData, allowEmpty = true)
    ).all { it.state != ValidationState.ERROR } && (terminalData.isNotEmpty() || iccData.isNotEmpty())

    ModernCryptoCard(title = "Application Cryptogram", subtitle = "ISO 9797-1 Algorithm 3 over the concatenated CDOL values (terminal ‖ ICC)", icon = Icons.Default.Security) {
        EnhancedTextField(value = sessionKey, onValueChange = { sessionKey = it.uppercase() }, label = "Session Key", validation = Emv42ValidationUtils.validateHexString(sessionKey, 32))
        Spacer(Modifier.height(4.dp))
        EnhancedTextField(value = terminalData, onValueChange = { terminalData = it.uppercase() }, label = "Terminal Data (CDOL values: 9F02 9F03 9F1A 95 5F2A 9A 9C 9F37 ...)", validation = Emv42ValidationUtils.validateHexString(terminalData, allowEmpty = true), maxLines = 3)
        Spacer(Modifier.height(4.dp))
        EnhancedTextField(value = iccData, onValueChange = { iccData = it.uppercase() }, label = "ICC Data (82 9F36 ‖ 9F10 / CVR)", validation = Emv42ValidationUtils.validateHexString(iccData, allowEmpty = true), maxLines = 3)
        Spacer(Modifier.height(4.dp))
        ModernDropdownField(label = "Padding Method", value = paddingMethod, options = paddings, onSelectionChanged = { paddingMethod = paddings[it] })
        Spacer(Modifier.height(8.dp))
        ModernButton(
            text = "Generate Cryptogram",
            onClick = {
                val inputs = mapOf("Session Key" to sessionKey, "Terminal Data" to terminalData, "ICC Data" to iccData, "Padding" to paddingMethod)
                try {
                    val data = terminalData.hexToBytes() + iccData.hexToBytes()
                    val ac = EmvAc.generateAc(sessionKey.hexToBytes(), data, if (paddingMethod.startsWith("Method 1")) Mac.Padding.METHOD_1 else Mac.Padding.METHOD_2)
                    Emv42LogManager.logOperation(Emv42CryptoTabs.CRYPTOGRAM.title, "Cryptogram Generation", inputs, result = "Data (${data.size} bytes): ${data.toHexUpper()}\nCryptogram (ARQC/TC/AAC): ${ac.toHexUpper()}")
                } catch (e: Exception) {
                    Emv42LogManager.logOperation(Emv42CryptoTabs.CRYPTOGRAM.title, "Cryptogram Generation", inputs, error = e.message)
                }
            },
            enabled = isFormValid, icon = Icons.Default.Shield, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ArpcTab42() {
    val standardResponseCodes = remember {
        listOf("00: Approved" to "00", "01: Refer to card issuer" to "01", "05: Do not honor" to "05", "51: Not sufficient funds" to "51", "Z1: Offline Approved" to "Z1", "Y1: Offline Approved" to "Y1", "Custom..." to "CUSTOM")
    }
    var sessionKey by remember { mutableStateOf("D920B6730B9267079220F8491F2FCD68") }
    var cryptogram by remember { mutableStateOf("FEDCBA9876543210") }
    var arpcMethod by remember { mutableStateOf("Method 1") }
    var selectedResponseCode by remember { mutableStateOf(standardResponseCodes.first()) }
    var customResponseCode by remember { mutableStateOf("") }
    var csuData by remember { mutableStateOf("00000000") }
    var propAuthData by remember { mutableStateOf("") }

    val responseCode = if (selectedResponseCode.second == "CUSTOM") customResponseCode else selectedResponseCode.second

    val isFormValid = listOf(
        Emv42ValidationUtils.validateHexString(sessionKey, 32),
        Emv42ValidationUtils.validateHexString(cryptogram, 16),
        if (arpcMethod == "Method 2") Emv42ValidationUtils.validateHexString(csuData, 8) else ValidationResult(ValidationState.VALID),
        Emv42ValidationUtils.validateHexString(propAuthData, allowEmpty = true),
    ).all { it.state != ValidationState.ERROR } && (arpcMethod == "Method 2" || responseCode.length in listOf(2, 4))

    ModernCryptoCard(title = "ARPC Generation", subtitle = "Method 1: 3DES(SK)[ARQC ⊕ ARC‖00×6]; Method 2: 4-byte MAC over ARQC ‖ CSU ‖ proprietary data", icon = Icons.Default.Reply) {
        EnhancedTextField(value = sessionKey, onValueChange = { sessionKey = it.uppercase() }, label = "Session Key", validation = Emv42ValidationUtils.validateHexString(sessionKey, 32))
        Spacer(Modifier.height(4.dp))
        EnhancedTextField(value = cryptogram, onValueChange = { cryptogram = it.uppercase() }, label = "AAC/ARQC/TC", validation = Emv42ValidationUtils.validateHexString(cryptogram, 16))
        Spacer(Modifier.height(4.dp))
        ModernDropdownField(label = "ARPC Generation Method", value = arpcMethod, options = listOf("Method 1", "Method 2"), onSelectionChanged = { index -> arpcMethod = if (index == 0) "Method 1" else "Method 2" })
        Spacer(Modifier.height(4.dp))
        AnimatedVisibility(visible = arpcMethod == "Method 1") {
            Column {
                ModernDropdownField(label = "Response Code (ARC)", value = selectedResponseCode.first, options = standardResponseCodes.map { it.first }, onSelectionChanged = { index -> selectedResponseCode = standardResponseCodes[index] })
                AnimatedVisibility(visible = selectedResponseCode.second == "CUSTOM") {
                    EnhancedTextField(value = customResponseCode, onValueChange = { customResponseCode = it.uppercase() }, label = "Custom Response Code (2 chars or 4 hex)", validation = ValidationResult(ValidationState.VALID))
                }
            }
        }
        AnimatedVisibility(visible = arpcMethod == "Method 2") {
            Column {
                EnhancedTextField(value = csuData, onValueChange = { csuData = it.uppercase() }, label = "CSU (Card Status Update, 4 bytes)", validation = Emv42ValidationUtils.validateHexString(csuData, 8))
                Spacer(Modifier.height(4.dp))
                EnhancedTextField(value = propAuthData, onValueChange = { propAuthData = it.uppercase() }, label = "Proprietary Authentication Data (0-8 bytes, optional)", validation = Emv42ValidationUtils.validateHexString(propAuthData, allowEmpty = true))
            }
        }
        Spacer(Modifier.height(8.dp))
        ModernButton(
            text = "Generate ARPC",
            onClick = {
                val inputs = mapOf("Session Key" to sessionKey, "Cryptogram" to cryptogram, "ARPC Method" to arpcMethod, "Response Code" to responseCode, "CSU" to csuData, "Proprietary data" to propAuthData)
                try {
                    val sk = sessionKey.hexToBytes()
                    val result = if (arpcMethod == "Method 1") {
                        val arc = arcToBytes(responseCode)
                        val arpc = EmvAc.arpcMethod1(sk, cryptogram.hexToBytes(), arc)
                        "ARPC: ${arpc.toHexUpper()}\nIssuer Authentication Data (91): ${arpc.toHexUpper()}${arc.toHexUpper()}"
                    } else {
                        val prop = if (propAuthData.isBlank()) ByteArray(0) else propAuthData.hexToBytes()
                        val arpc = EmvAc.arpcMethod2(sk, cryptogram.hexToBytes(), csuData.hexToBytes(), prop)
                        "ARPC: ${arpc.toHexUpper()}\nIssuer Authentication Data (91): ${arpc.toHexUpper()}${csuData.uppercase()}${prop.toHexUpper()}"
                    }
                    Emv42LogManager.logOperation(Emv42CryptoTabs.ARPC.title, "ARPC Generation", inputs, result = result)
                } catch (e: Exception) {
                    Emv42LogManager.logOperation(Emv42CryptoTabs.ARPC.title, "ARPC Generation", inputs, error = e.message)
                }
            },
            enabled = isFormValid, icon = Icons.Default.Security, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun UtilitiesTab42() {
    var key by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    val isFormValid = Emv42ValidationUtils.validateHexString(key, 32).state != ValidationState.ERROR

    ModernCryptoCard(title = "Cryptographic Utilities", subtitle = "Key Check Value: 3DES over a zero block, first 3 bytes", icon = Icons.Default.Build) {
        EnhancedTextField(value = key, onValueChange = { key = it.uppercase() }, label = "Key (16 bytes)", validation = Emv42ValidationUtils.validateHexString(key, 32))
        Spacer(Modifier.height(8.dp))
        ModernButton(
            text = "Calculate KCV",
            onClick = {
                val inputs = mapOf("Key" to key)
                try {
                    val k = key.hexToBytes()
                    Emv42LogManager.logOperation(Emv42CryptoTabs.UTILITIES.title, "KCV Calculation", inputs, result = "KCV: ${kcvOf(k)}\nParity: ${if (EmvKeys.oddParity(k).contentEquals(k)) "odd" else "not odd"}")
                } catch (e: Exception) {
                    Emv42LogManager.logOperation(Emv42CryptoTabs.UTILITIES.title, "KCV Calculation", inputs, error = e.message)
                }
            },
            enabled = isFormValid, icon = Icons.Default.VerifiedUser, modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- SHARED UI COMPONENTS for EMV 4.2 ---

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
                Text(
                    text = validation.message,
                    color = when (validation.state) {
                        ValidationState.ERROR -> MaterialTheme.colors.error
                        ValidationState.WARNING -> Color(0xFF856404)
                        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    },
                    style = MaterialTheme.typography.caption
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = validation.helperText,
                color = if (validation.state == ValidationState.VALID) SuccessGreen else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun ModernCryptoCard(title: String, subtitle: String, icon: ImageVector, onInfoClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Card(title, subtitle, icon, onInfoClick, content); return }
    Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp, shape = MaterialTheme.shapes.medium, backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
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
    Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled && !isLoading, elevation = ButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp, disabledElevation = 0.dp)) {
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "Emv42ButtonAnimation") { loading ->
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
        shape = MaterialTheme.shapes.medium
    )
}

private fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2
    fillMaxWidth().wrapContentSize(Alignment.BottomStart).offset(x = indicatorOffset).width(indicatorWidth)
}
