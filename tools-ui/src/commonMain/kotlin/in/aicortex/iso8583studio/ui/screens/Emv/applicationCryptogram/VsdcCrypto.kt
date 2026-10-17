package `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram

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
import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
// --- COMMON UI & VALIDATION FOR VSDC SCREEN ---


object VsdcValidationUtils {
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

// --- VSDC SCREEN ---

enum class VsdcCryptoTabs(val title: String, val icon: ImageVector) {
    UDK("UDK", Icons.Default.VpnKey),
    SESSION_KEYS("Session Keys", Icons.Default.Lock),
    CRYPTOGRAM("AAC/ARQC/TC", Icons.Default.Security),
    ARPC("ARPC", Icons.Default.Reply)
}

object VsdcLogManager {
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
                val displayValue = if (key.contains("key", ignoreCase = true) || key.contains("mdk", ignoreCase = true) || key.contains("pan", ignoreCase = true)) {
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
        addLog(tabTitle, LogEntry(timestamp = timestamp, type= logType, message = message, details = details))
    }
}

@Composable
fun VsdcCryptoScreen(
    
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = VsdcCryptoTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    val body: @Composable () -> Unit = {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        when (tab) {
                            VsdcCryptoTabs.UDK -> UdkTabVsdc()
                            VsdcCryptoTabs.SESSION_KEYS -> SessionKeysTabVsdc()
                            VsdcCryptoTabs.CRYPTOGRAM -> CryptogramTabVsdc()
                            VsdcCryptoTabs.ARPC -> ArpcTabVsdc()
                        }
                    }
                
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "VSDC Crypto Calculator", onBack = onBack, tabs = tabList.map { ToolTab(it.title, it.icon) }, selectedTab = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, log = ToolLog(VsdcLogManager.getLogEntries(selectedTab.title), onClear = { VsdcLogManager.clearLogs(selectedTab.title) })), content = body)
        return
    }
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "VSDC Crypto Calculator",
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
                            onClearClick = { VsdcLogManager.clearLogs(selectedTab.title) },
                            logEntries = VsdcLogManager.getLogEntries(selectedTab.title)
                        )
                    }
                }
            }
        }
    }
}

// --- TABS for VSDC ---

/** ARC as two ASCII characters ("00", "Z3") or four hex digits ("3030"). */
private fun arcToBytes(arc: String): ByteArray = when {
    arc.length == 2 -> EmvAc.arcBytes(arc.uppercase())
    arc.length == 4 && arc.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' } -> arc.hexToBytes()
    else -> throw IllegalArgumentException("Response code must be 2 characters or 4 hex digits")
}

private fun vsdcKcv(key: ByteArray): String = TdesCalculatorEngine.calculateKCV(key).toHexUpper()

@Composable
private fun UdkTabVsdc() {
    var mdk by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var pan by remember { mutableStateOf("4999990011112222") }
    var panSeq by remember { mutableStateOf("01") }
    var udkOption by remember { mutableStateOf("Option A") }
    var keyParity by remember { mutableStateOf("Odd") }

    val isFormValid = VsdcValidationUtils.validateHexString(mdk, 32).state == ValidationState.VALID && pan.length in 12..19 && pan.all { it.isDigit() } && panSeq.length == 2

    ModernCryptoCard(title = "UDK Derivation", subtitle = "Generate the ICC master key (UDK) from the issuer MDK", icon = Icons.Default.VpnKey) {
        EnhancedTextField(value = mdk, onValueChange = { mdk = it.uppercase() }, label = "MDK", validation = VsdcValidationUtils.validateHexString(mdk, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = pan, onValueChange = { pan = it.filter { c -> c.isDigit() } }, label = "PAN (12-19 digits)", validation = ValidationResult(if (pan.length in 12..19) ValidationState.VALID else ValidationState.ERROR, "PAN must be 12-19 digits"))
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
                val inputs = mapOf("MDK" to mdk, "PAN" to pan, "PAN Seq" to panSeq, "Option" to udkOption, "Parity" to keyParity)
                try {
                    val udk = if (udkOption == "Option A") EmvKeys.deriveUdkOptionA(mdk.hexToBytes(), pan, panSeq, keyParity == "Odd") else EmvKeys.deriveUdkOptionB(mdk.hexToBytes(), pan, panSeq, keyParity == "Odd")
                    VsdcLogManager.logOperation(VsdcCryptoTabs.UDK.title, "UDK Generation", inputs, result = "UDK: ${udk.toHexUpper()}\nKCV: ${vsdcKcv(udk)}")
                } catch (e: Exception) { VsdcLogManager.logOperation(VsdcCryptoTabs.UDK.title, "UDK Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SessionKeysTabVsdc() {
    val methods = listOf("Common session key (CVN 18/22, ATC)", "Visa secure messaging session key (ATC)", "None - CVN 10 uses the UDK directly")
    var udk by remember { mutableStateOf("112233445566778899AABBCCDDEEFF00") }
    var atc by remember { mutableStateOf("0001") }
    var method by remember { mutableStateOf(methods.first()) }

    val isFormValid = VsdcValidationUtils.validateHexString(udk, 32).state == ValidationState.VALID && VsdcValidationUtils.validateHexString(atc, 4).state == ValidationState.VALID

    ModernCryptoCard(title = "Session Keys (VSDC)", subtitle = "Derive the AC or secure-messaging session key from the UDK", icon = Icons.Default.Lock) {
        EnhancedTextField(value = udk, onValueChange = { udk = it.uppercase() }, label = "UDK", validation = VsdcValidationUtils.validateHexString(udk, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC", validation = VsdcValidationUtils.validateHexString(atc, 4))
        Spacer(Modifier.height(8.dp))
        ModernDropdownField(label = "Derivation", value = method, options = methods, onSelectionChanged = { method = methods[it] })
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate Session Key",
            onClick = {
                val inputs = mapOf("UDK" to udk, "ATC" to atc, "Method" to method)
                try {
                    val sk = when (methods.indexOf(method)) {
                        0 -> EmvKeys.deriveCommonSessionKey(udk.hexToBytes(), atc.hexToBytes(), parity = true)
                        1 -> EmvSessionKeys.deriveVisaSmSessionKey(udk.hexToBytes(), atc.hexToBytes())
                        else -> udk.hexToBytes()
                    }
                    VsdcLogManager.logOperation(VsdcCryptoTabs.SESSION_KEYS.title, "Session Key Generation", inputs, result = "Session Key: ${sk.toHexUpper()}\nKCV: ${vsdcKcv(sk)}")
                } catch (e: Exception) { VsdcLogManager.logOperation(VsdcCryptoTabs.SESSION_KEYS.title, "Session Key Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CryptogramTabVsdc() {
    val cvnOptions = remember { EmvCvn.VISA.map { it.name } }
    var selectedCvn by remember { mutableStateOf(cvnOptions.first()) }
    var udk by remember { mutableStateOf("112233445566778899AABBCCDDEEFF00") }
    var amount by remember { mutableStateOf("000000010000") }
    var amountOther by remember { mutableStateOf("000000000000") }
    var terminalCountry by remember { mutableStateOf("0840") }
    var tvr by remember { mutableStateOf("0000008000") }
    var currencyCode by remember { mutableStateOf("0840") }
    var transDate by remember { mutableStateOf("250611") }
    var transType by remember { mutableStateOf("00") }
    var un by remember { mutableStateOf("1A2B3C4D") }
    var aip by remember { mutableStateOf("7C00") }
    var atc by remember { mutableStateOf("0001") }
    var iad by remember { mutableStateOf("06010A03A00000") }

    val profile = EmvCvn.byName(selectedCvn)
    val isFormValid = listOf(udk to 32, amount to 12, amountOther to 12, terminalCountry to 4, tvr to 10, currencyCode to 4, transDate to 6, transType to 2, un to 8, aip to 4, atc to 4)
        .all { (v, l) -> VsdcValidationUtils.validateHexString(v, l).state == ValidationState.VALID } && VsdcValidationUtils.validateHexString(iad).state == ValidationState.VALID

    ModernCryptoCard(title = "AAC/ARQC/TC Generation (VSDC)", subtitle = "${profile.name}: ${if (profile.sessionKey == EmvCvn.SessionKeyMethod.NONE) "UDK" else "common session key"}, padding ${if (profile.padding == Mac.Padding.METHOD_1) "method 1" else "method 2"}, data ends with ${if (profile.tail == "IAD") "the full IAD" else "the 4-byte CVR"}" + if (profile.referenceOnly) " - reference implementation" else "", icon = Icons.Default.Security) {
        ModernDropdownField(label = "Cryptogram Version (CVN)", value = selectedCvn, options = cvnOptions, onSelectionChanged = { selectedCvn = cvnOptions[it] })
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = udk, onValueChange = { udk = it.uppercase() }, label = "UDK (ICC Master Key AC)", validation = VsdcValidationUtils.validateHexString(udk, 32))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = amount, onValueChange = { amount = it }, label = "Amount (9F02)", validation = VsdcValidationUtils.validateHexString(amount, 12), modifier = Modifier.weight(1f))
            EnhancedTextField(value = amountOther, onValueChange = { amountOther = it }, label = "Amount, Other (9F03)", validation = VsdcValidationUtils.validateHexString(amountOther, 12), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = terminalCountry, onValueChange = { terminalCountry = it.uppercase() }, label = "Terminal Country (9F1A)", validation = VsdcValidationUtils.validateHexString(terminalCountry, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = tvr, onValueChange = { tvr = it.uppercase() }, label = "TVR (95)", validation = VsdcValidationUtils.validateHexString(tvr, 10), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = currencyCode, onValueChange = { currencyCode = it.uppercase() }, label = "Currency Code (5F2A)", validation = VsdcValidationUtils.validateHexString(currencyCode, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = transDate, onValueChange = { transDate = it.uppercase() }, label = "Transaction Date (9A)", validation = VsdcValidationUtils.validateHexString(transDate, 6), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = transType, onValueChange = { transType = it.uppercase() }, label = "Transaction Type (9C)", validation = VsdcValidationUtils.validateHexString(transType, 2), modifier = Modifier.weight(1f))
            EnhancedTextField(value = un, onValueChange = { un = it.uppercase() }, label = "Unpredictable No. (9F37)", validation = VsdcValidationUtils.validateHexString(un, 8), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = aip, onValueChange = { aip = it.uppercase() }, label = "AIP (82)", validation = VsdcValidationUtils.validateHexString(aip, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC (9F36)", validation = VsdcValidationUtils.validateHexString(atc, 4), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = iad, onValueChange = { iad = it.uppercase() }, label = "Issuer Application Data (9F10): 06 ‖ DKI ‖ CVN ‖ CVR(4) ‖ ...", validation = VsdcValidationUtils.validateHexString(iad))
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate Cryptogram",
            onClick = {
                val inputs = mapOf("CVN" to selectedCvn, "UDK" to udk, "Amount" to amount, "TVR" to tvr, "ATC" to atc, "IAD" to iad)
                try {
                    val txn = EmvCvn.TransactionData(amount.hexToBytes(), amountOther.hexToBytes(), terminalCountry.hexToBytes(), tvr.hexToBytes(), currencyCode.hexToBytes(), transDate.hexToBytes(), transType.hexToBytes(), un.hexToBytes(), aip.hexToBytes(), atc.hexToBytes())
                    val sk = EmvCvn.sessionKey(profile, udk.hexToBytes(), txn)
                    val data = EmvCvn.cryptogramData(profile, txn, iad.hexToBytes())
                    val ac = EmvCvn.generateAc(profile, sk, data)
                    VsdcLogManager.logOperation(VsdcCryptoTabs.CRYPTOGRAM.title, "Cryptogram Generation", inputs, result = "Session key: ${sk.toHexUpper()}\nData (${data.size} bytes): ${data.toHexUpper()}\nCryptogram: ${ac.toHexUpper()}")
                } catch (e: Exception) { VsdcLogManager.logOperation(VsdcCryptoTabs.CRYPTOGRAM.title, "Cryptogram Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.Shield, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ArpcTabVsdc() {
    val cvnOptions = remember { EmvCvn.VISA.map { it.name } }
    val standardResponseCodes = remember { listOf("00: Approved", "01: Refer to issuer", "05: Do not honor", "51: Insufficient funds", "Z1: Offline Approved", "Custom...") }
    var selectedCvn by remember { mutableStateOf(cvnOptions.first()) }
    var udk by remember { mutableStateOf("112233445566778899AABBCCDDEEFF00") }
    var atc by remember { mutableStateOf("0001") }
    var cryptogram by remember { mutableStateOf("1020304050607080") }
    var methodChoice by remember { mutableStateOf("Per CVN") }
    var selectedResponseCode by remember { mutableStateOf(standardResponseCodes.first()) }
    var customResponseCode by remember { mutableStateOf("") }
    var csuData by remember { mutableStateOf("00000000") }
    var propAuthData by remember { mutableStateOf("") }

    val profile = EmvCvn.byName(selectedCvn)
    val method = when (methodChoice) { "Method 1" -> EmvCvn.ArpcMethod.METHOD_1; "Method 2" -> EmvCvn.ArpcMethod.METHOD_2; else -> profile.arpc }
    val responseCode = if (selectedResponseCode == "Custom...") customResponseCode else selectedResponseCode.split(":")[0]
    val isFormValid = VsdcValidationUtils.validateHexString(udk, 32).state == ValidationState.VALID && VsdcValidationUtils.validateHexString(cryptogram, 16).state == ValidationState.VALID &&
            VsdcValidationUtils.validateHexString(atc, 4).state == ValidationState.VALID &&
            (if (method == EmvCvn.ArpcMethod.METHOD_2) VsdcValidationUtils.validateHexString(csuData, 8).state == ValidationState.VALID else responseCode.length in listOf(2, 4))

    ModernCryptoCard(title = "ARPC Generation", subtitle = "${profile.name} uses ARPC ${if (profile.arpc == EmvCvn.ArpcMethod.METHOD_1) "method 1 (ARC)" else "method 2 (CSU)"}", icon = Icons.Default.Reply) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernDropdownField(label = "CVN", value = selectedCvn, options = cvnOptions, onSelectionChanged = { selectedCvn = cvnOptions[it] }, modifier = Modifier.weight(1f))
            ModernDropdownField(label = "ARPC Method", value = methodChoice, options = listOf("Per CVN", "Method 1", "Method 2"), onSelectionChanged = { methodChoice = listOf("Per CVN", "Method 1", "Method 2")[it] }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = udk, onValueChange = { udk = it.uppercase() }, label = "UDK (ICC Master Key AC)", validation = VsdcValidationUtils.validateHexString(udk, 32))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC (9F36)", validation = VsdcValidationUtils.validateHexString(atc, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = cryptogram, onValueChange = { cryptogram = it.uppercase() }, label = "ARQC (9F26)", validation = VsdcValidationUtils.validateHexString(cryptogram, 16), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        if (method == EmvCvn.ArpcMethod.METHOD_1) {
            ModernDropdownField(label = "Response Code", value = selectedResponseCode, options = standardResponseCodes, onSelectionChanged = { selectedResponseCode = standardResponseCodes[it] })
            AnimatedVisibility(visible = selectedResponseCode == "Custom...") {
                EnhancedTextField(value = customResponseCode, onValueChange = { customResponseCode = it.uppercase() }, label = "Custom Response Code (2 chars or 4 hex)", validation = ValidationResult(ValidationState.VALID))
            }
        } else {
            EnhancedTextField(value = csuData, onValueChange = { csuData = it.uppercase() }, label = "CSU (4 bytes)", validation = VsdcValidationUtils.validateHexString(csuData, 8))
            Spacer(Modifier.height(8.dp))
            EnhancedTextField(value = propAuthData, onValueChange = { propAuthData = it.uppercase() }, label = "Proprietary Authentication Data (optional)", validation = VsdcValidationUtils.validateHexString(propAuthData, allowEmpty = true))
        }
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate ARPC",
            onClick = {
                val inputs = mapOf("CVN" to selectedCvn, "UDK" to udk, "ATC" to atc, "Cryptogram" to cryptogram, "Method" to method.name, "Response Code" to responseCode, "CSU" to csuData)
                try {
                    val txn = EmvCvn.TransactionData(ByteArray(6), ByteArray(6), ByteArray(2), ByteArray(5), ByteArray(2), ByteArray(3), ByteArray(1), ByteArray(4), ByteArray(2), atc.hexToBytes())
                    val sk = EmvCvn.sessionKey(profile, udk.hexToBytes(), txn)
                    val arqc = cryptogram.hexToBytes()
                    val result = if (method == EmvCvn.ArpcMethod.METHOD_1) {
                        val arc = arcToBytes(responseCode)
                        val arpc = EmvAc.arpcMethod1(sk, arqc, arc)
                        "Session key: ${sk.toHexUpper()}\nARPC: ${arpc.toHexUpper()}\nIssuer Authentication Data (91): ${arpc.toHexUpper()}${arc.toHexUpper()}"
                    } else {
                        val prop = if (propAuthData.isBlank()) ByteArray(0) else propAuthData.hexToBytes()
                        val arpc = EmvAc.arpcMethod2(sk, arqc, csuData.hexToBytes(), prop)
                        "Session key: ${sk.toHexUpper()}\nARPC: ${arpc.toHexUpper()}\nIssuer Authentication Data (91): ${arpc.toHexUpper()}${csuData.uppercase()}${prop.toHexUpper()}"
                    }
                    VsdcLogManager.logOperation(VsdcCryptoTabs.ARPC.title, "ARPC Generation", inputs, result = result)
                } catch (e: Exception) { VsdcLogManager.logOperation(VsdcCryptoTabs.ARPC.title, "ARPC Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.Security, modifier = Modifier.fillMaxWidth()
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "VsdcButtonAnimation") { loading ->
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
