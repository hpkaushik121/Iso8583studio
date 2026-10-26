package `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram

import `in`.aicortex.iso8583studio.ui.chrome.toolScroll

import `in`.aicortex.iso8583studio.ui.chrome.ToolTab

import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec

import `in`.aicortex.iso8583studio.ui.chrome.ToolLog

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import ai.cortex.core.CryptoUtils

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
import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.PersistentTabContent
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.LogPanelWithAutoScroll
import ai.cortex.core.types.KeyParity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
object MastercardValidationUtils {
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

// --- MASTERCARD SCREEN ---

enum class MastercardCryptoTabs(val title: String, val icon: ImageVector) {
    UDK("UDK", Icons.Default.VpnKey),
    SESSION_KEY_EMV_2000("Session Key (EMV 2000)", Icons.Default.Key),
    SESSION_KEYS("Session Keys", Icons.Default.Lock),
    CRYPTOGRAM("AAC/ARQC/TC", Icons.Default.Security),
    ARPC("ARPC", Icons.Default.Reply)
}

object MastercardLogManager {
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
fun MastercardCryptoScreen(
    
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = MastercardCryptoTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    val body: @Composable () -> Unit = {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        when (tab) {
                            MastercardCryptoTabs.UDK -> UdkTab()
                            MastercardCryptoTabs.SESSION_KEY_EMV_2000 -> SessionKeyEmv2000Tab()
                            MastercardCryptoTabs.SESSION_KEYS -> SessionKeysTab()
                            MastercardCryptoTabs.CRYPTOGRAM -> CryptogramTab()
                            MastercardCryptoTabs.ARPC -> ArpcTab()
                        }
                    }
                
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "MasterCard M/Chip Crypto Calculator", onBack = onBack, tabs = tabList.map { ToolTab(it.title, it.icon) }, selectedTab = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, log = ToolLog(MastercardLogManager.getLogEntries(selectedTab.title), onClear = { MastercardLogManager.clearLogs(selectedTab.title) })), content = body)
        return
    }
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "MasterCard M/Chip Crypto Calculator",
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
                            onClearClick = { MastercardLogManager.clearLogs(selectedTab.title) },
                            logEntries = MastercardLogManager.getLogEntries(selectedTab.title)
                        )
                    }
                }
            }
        }
    }
}

// --- TABS for MasterCard ---

/** ARC as two ASCII characters ("00", "Z3") or four hex digits ("3030"). */
private fun arcToBytes(arc: String): ByteArray = when {
    arc.length == 2 -> EmvAc.arcBytes(arc.uppercase())
    arc.length == 4 && arc.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' } -> arc.hexToBytes()
    else -> throw IllegalArgumentException("Response code must be 2 characters or 4 hex digits")
}

private fun mcKcv(key: ByteArray): String = TdesCalculatorEngine.calculateKCV(key).toHexUpper()

@Composable
private fun UdkTab() {
    var mdk by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var pan by remember { mutableStateOf("5413330011112222") }
    var panSeq by remember { mutableStateOf("01") }
    var udkOption by remember { mutableStateOf("Option A") }
    var keyParity by remember { mutableStateOf(KeyParity.ODD) }

    val isFormValid = MastercardValidationUtils.validateHexString(mdk, 32).state == ValidationState.VALID && pan.length in 12..19 && pan.all { it.isDigit() } && panSeq.length == 2

    ModernCryptoCard(title = "UDK Derivation", subtitle = "ICC master key from the issuer master key (EMV Book 2 A1.4, Option A/B)", icon = Icons.Default.VpnKey) {
        EnhancedTextField(value = mdk, onValueChange = { mdk = it.uppercase() }, label = "MDK (IMK-AC / IMK-SMI / IMK-SMC)", validation = MastercardValidationUtils.validateHexString(mdk, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = pan, onValueChange = { pan = it.filter { c -> c.isDigit() } }, label = "PAN (12-19 digits)", validation = ValidationResult(if (pan.length in 12..19) ValidationState.VALID else ValidationState.ERROR, "PAN must be 12-19 digits"))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = panSeq, onValueChange = { panSeq = it.filter { c -> c.isDigit() }.take(2) }, label = "PAN Sequence No.", validation = ValidationResult(if (panSeq.length == 2) ValidationState.VALID else ValidationState.ERROR, "2 digits"))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernDropdownField(label = "UDK Derivation Option", value = udkOption, options = listOf("Option A", "Option B"), onSelectionChanged = { udkOption = if (it == 0) "Option A" else "Option B" }, modifier = Modifier.weight(1f))
            ModernDropdownField(label = "Key Parity", value = keyParity.name, options = KeyParity.values().map { it.name }, onSelectionChanged = { keyParity = KeyParity.values()[it] }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate UDK",
            onClick = {
                val inputs = mapOf("MDK" to mdk, "PAN" to pan, "PAN Seq" to panSeq, "Option" to udkOption, "Parity" to keyParity.name)
                try {
                    var udk = if (udkOption == "Option A") EmvKeys.deriveUdkOptionA(mdk.hexToBytes(), pan, panSeq, keyParity == KeyParity.ODD) else EmvKeys.deriveUdkOptionB(mdk.hexToBytes(), pan, panSeq, keyParity == KeyParity.ODD)
                    if (keyParity == KeyParity.EVEN) udk = CryptoUtils.applyParity(udk, isOdd = false)
                    MastercardLogManager.logOperation(MastercardCryptoTabs.UDK.title, "UDK Generation", inputs, result = "UDK: ${udk.toHexUpper()}\nKCV: ${mcKcv(udk)}")
                } catch (e: Exception) { MastercardLogManager.logOperation(MastercardCryptoTabs.UDK.title, "UDK Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SessionKeyEmv2000Tab() {
    var masterKey by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var initialVector by remember { mutableStateOf("00000000000000000000000000000000") }
    var atc by remember { mutableStateOf("0001") }
    var branchFactor by remember { mutableStateOf("4") }
    var height by remember { mutableStateOf("8") }
    var keyParity by remember { mutableStateOf("Odd") }

    val isFormValid = MastercardValidationUtils.validateHexString(masterKey, 32).state == ValidationState.VALID && MastercardValidationUtils.validateHexString(initialVector, 32).state == ValidationState.VALID &&
            MastercardValidationUtils.validateHexString(atc, 4).state == ValidationState.VALID && branchFactor.toIntOrNull() != null && height.toIntOrNull() != null

    ModernCryptoCard(title = "Session Key (EMV 2000)", subtitle = "Tree-based derivation (EMV 4.1 Book 2 A1.3, superseded by SU-46); branch factor 4 / height 8 or 2 / 16", icon = Icons.Default.Key) {
        EnhancedTextField(value = masterKey, onValueChange = { masterKey = it.uppercase() }, label = "Master Key", validation = MastercardValidationUtils.validateHexString(masterKey, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = initialVector, onValueChange = { initialVector = it.uppercase() }, label = "Initial Vector (16 bytes)", validation = MastercardValidationUtils.validateHexString(initialVector, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC", validation = MastercardValidationUtils.validateHexString(atc, 4))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = branchFactor, onValueChange = { branchFactor = it.filter { c -> c.isDigit() } }, label = "Branch Factor (decimal)", validation = ValidationResult(if (branchFactor.toIntOrNull() != null) ValidationState.VALID else ValidationState.ERROR, "Number"), modifier = Modifier.weight(1f))
            EnhancedTextField(value = height, onValueChange = { height = it.filter { c -> c.isDigit() } }, label = "Height (decimal)", validation = ValidationResult(if (height.toIntOrNull() != null) ValidationState.VALID else ValidationState.ERROR, "Number"), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        ModernDropdownField(label = "Key Parity", value = keyParity, options = listOf("Odd", "None"), onSelectionChanged = { keyParity = listOf("Odd", "None")[it] })
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate Session Key",
            onClick = {
                val inputs = mapOf("Master Key" to masterKey, "Initial Vector" to initialVector, "ATC" to atc, "Branch Factor" to branchFactor, "Height" to height, "Parity" to keyParity)
                try {
                    val sk = EmvSessionKeys.deriveEmv2000TreeSessionKey(masterKey.hexToBytes(), atc.hexToBytes(), height.toInt(), branchFactor.toInt(), initialVector.hexToBytes(), keyParity == "Odd")
                    MastercardLogManager.logOperation(MastercardCryptoTabs.SESSION_KEY_EMV_2000.title, "Session Key (EMV 2000)", inputs, result = "Session Key: ${sk.toHexUpper()}\nKCV: ${mcKcv(sk)}")
                } catch (e: Exception) { MastercardLogManager.logOperation(MastercardCryptoTabs.SESSION_KEY_EMV_2000.title, "Session Key (EMV 2000)", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SessionKeysTab() {
    val methods = listOf("Mastercard SKD (ATC ‖ UN) - CVN 10/11/16/17", "Common session key (ATC) - CVN 20/21", "Common session key with R = ARQC (secure messaging)")
    var udk by remember { mutableStateOf("112233445566778899AABBCCDDEEFF00") }
    var atc by remember { mutableStateOf("0001") }
    var unpredictableNumber by remember { mutableStateOf("1A2B3C4D") }
    var arqc by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(methods.first()) }

    val idx = methods.indexOf(method)
    val isFormValid = MastercardValidationUtils.validateHexString(udk, 32).state == ValidationState.VALID &&
            (idx == 2 && MastercardValidationUtils.validateHexString(arqc, 16).state == ValidationState.VALID ||
             idx != 2 && MastercardValidationUtils.validateHexString(atc, 4).state == ValidationState.VALID && (idx == 1 || MastercardValidationUtils.validateHexString(unpredictableNumber, 8).state == ValidationState.VALID))

    ModernCryptoCard(title = "Session Keys", subtitle = "Derive the session key from the UDK", icon = Icons.Default.Lock) {
        ModernDropdownField(label = "Derivation", value = method, options = methods, onSelectionChanged = { method = methods[it] })
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = udk, onValueChange = { udk = it.uppercase() }, label = "UDK", validation = MastercardValidationUtils.validateHexString(udk, 32))
        Spacer(Modifier.height(8.dp))
        if (idx == 2) {
            EnhancedTextField(value = arqc, onValueChange = { arqc = it.uppercase() }, label = "ARQC (9F26)", validation = MastercardValidationUtils.validateHexString(arqc, 16))
        } else {
            EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC", validation = MastercardValidationUtils.validateHexString(atc, 4))
            if (idx == 0) {
                Spacer(Modifier.height(8.dp))
                EnhancedTextField(value = unpredictableNumber, onValueChange = { unpredictableNumber = it.uppercase() }, label = "Unpredictable Number", validation = MastercardValidationUtils.validateHexString(unpredictableNumber, 8))
            }
        }
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate Session Key",
            onClick = {
                val inputs = mapOf("UDK" to udk, "ATC" to atc, "Unpredictable No." to unpredictableNumber, "ARQC" to arqc, "Method" to method)
                try {
                    val sk = when (idx) {
                        0 -> EmvKeys.deriveMastercardSessionKey(udk.hexToBytes(), atc.hexToBytes(), unpredictableNumber.hexToBytes(), parity = true)
                        1 -> EmvKeys.deriveCommonSessionKey(udk.hexToBytes(), atc.hexToBytes(), parity = true)
                        else -> EmvSessionKeys.deriveCommonSessionKeyR(udk.hexToBytes(), arqc.hexToBytes())
                    }
                    MastercardLogManager.logOperation(MastercardCryptoTabs.SESSION_KEYS.title, "Session Key Generation", inputs, result = "Session Key: ${sk.toHexUpper()}\nKCV: ${mcKcv(sk)}")
                } catch (e: Exception) { MastercardLogManager.logOperation(MastercardCryptoTabs.SESSION_KEYS.title, "Session Key Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CryptogramTab() {
    val cvnOptions = remember { EmvCvn.MASTERCARD.map { it.name } }
    var selectedCvn by remember { mutableStateOf("Mastercard CVN 16") }
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
    var iad by remember { mutableStateOf("0110A50000000000000000000000FF") }

    val profile = EmvCvn.byName(selectedCvn)
    val isFormValid = listOf(udk to 32, amount to 12, amountOther to 12, terminalCountry to 4, tvr to 10, currencyCode to 4, transDate to 6, transType to 2, un to 8, aip to 4, atc to 4)
        .all { (v, l) -> MastercardValidationUtils.validateHexString(v, l).state == ValidationState.VALID } && MastercardValidationUtils.validateHexString(iad).state == ValidationState.VALID

    ModernCryptoCard(title = "AAC/ARQC/TC Generation", subtitle = "${profile.name}: ${if (profile.sessionKey == EmvCvn.SessionKeyMethod.MASTERCARD_SKD) "SKD session key (ATC ‖ UN)" else "common session key"}, padding method 2, data ends with the 6-byte CVR${if (profile.tail.endsWith("COUNTERS")) " and the 8-byte counters" else ""}", icon = Icons.Default.Security) {
        ModernDropdownField(label = "Cryptogram Version (CVN)", value = selectedCvn, options = cvnOptions, onSelectionChanged = { selectedCvn = cvnOptions[it] })
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = udk, onValueChange = { udk = it.uppercase() }, label = "UDK (ICC Master Key AC)", validation = MastercardValidationUtils.validateHexString(udk, 32))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = amount, onValueChange = { amount = it }, label = "Amount (9F02)", validation = MastercardValidationUtils.validateHexString(amount, 12), modifier = Modifier.weight(1f))
            EnhancedTextField(value = amountOther, onValueChange = { amountOther = it }, label = "Amount, Other (9F03)", validation = MastercardValidationUtils.validateHexString(amountOther, 12), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = terminalCountry, onValueChange = { terminalCountry = it.uppercase() }, label = "Terminal Country (9F1A)", validation = MastercardValidationUtils.validateHexString(terminalCountry, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = tvr, onValueChange = { tvr = it.uppercase() }, label = "TVR (95)", validation = MastercardValidationUtils.validateHexString(tvr, 10), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = currencyCode, onValueChange = { currencyCode = it.uppercase() }, label = "Currency Code (5F2A)", validation = MastercardValidationUtils.validateHexString(currencyCode, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = transDate, onValueChange = { transDate = it.uppercase() }, label = "Transaction Date (9A)", validation = MastercardValidationUtils.validateHexString(transDate, 6), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = transType, onValueChange = { transType = it.uppercase() }, label = "Transaction Type (9C)", validation = MastercardValidationUtils.validateHexString(transType, 2), modifier = Modifier.weight(1f))
            EnhancedTextField(value = un, onValueChange = { un = it.uppercase() }, label = "Unpredictable No. (9F37)", validation = MastercardValidationUtils.validateHexString(un, 8), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = aip, onValueChange = { aip = it.uppercase() }, label = "AIP (82)", validation = MastercardValidationUtils.validateHexString(aip, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC (9F36)", validation = MastercardValidationUtils.validateHexString(atc, 4), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = iad, onValueChange = { iad = it.uppercase() }, label = "Issuer Application Data (9F10): KDI ‖ CVN ‖ CVR(6) ‖ counters(8) ...", validation = MastercardValidationUtils.validateHexString(iad))
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
                    MastercardLogManager.logOperation(MastercardCryptoTabs.CRYPTOGRAM.title, "Cryptogram Generation", inputs, result = "Session key: ${sk.toHexUpper()}\nData (${data.size} bytes): ${data.toHexUpper()}\nCryptogram: ${ac.toHexUpper()}")
                } catch (e: Exception) { MastercardLogManager.logOperation(MastercardCryptoTabs.CRYPTOGRAM.title, "Cryptogram Generation", inputs, error = e.message) }
            },
            enabled = isFormValid, icon = Icons.Default.Shield, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ArpcTab() {
    val cvnOptions = remember { EmvCvn.MASTERCARD.map { it.name } }
    val standardResponseCodes = remember { listOf("00: Approved", "01: Refer to issuer", "05: Do not honor", "51: Insufficient funds", "Z1: Offline Approved", "Custom...") }
    var selectedCvn by remember { mutableStateOf("Mastercard CVN 16") }
    var udk by remember { mutableStateOf("112233445566778899AABBCCDDEEFF00") }
    var atc by remember { mutableStateOf("0001") }
    var un by remember { mutableStateOf("1A2B3C4D") }
    var cryptogram by remember { mutableStateOf("FEDCBA9876543210") }
    var skChoice by remember { mutableStateOf("ICC master key (M/Chip 4: CVN 10-17)") }
    val skChoices = listOf("ICC master key (M/Chip 4: CVN 10-17)", "Session key per CVN")
    var methodChoice by remember { mutableStateOf("Method 1") }
    var selectedResponseCode by remember { mutableStateOf(standardResponseCodes.first()) }
    var customResponseCode by remember { mutableStateOf("") }
    var csuData by remember { mutableStateOf("00000000") }

    val profile = EmvCvn.byName(selectedCvn)
    val responseCode = if (selectedResponseCode == "Custom...") customResponseCode else selectedResponseCode.split(":")[0]
    val isFormValid = MastercardValidationUtils.validateHexString(udk, 32).state == ValidationState.VALID && MastercardValidationUtils.validateHexString(cryptogram, 16).state == ValidationState.VALID &&
            (methodChoice == "Method 2" && MastercardValidationUtils.validateHexString(csuData, 8).state == ValidationState.VALID || methodChoice == "Method 1" && responseCode.length in listOf(2, 4))

    ModernCryptoCard(title = "ARPC Generation", subtitle = "M/Chip 4 answers with ARPC method 1 under the ICC master key; M/Chip Advance profiles may use the session key", icon = Icons.Default.Reply) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernDropdownField(label = "CVN", value = selectedCvn, options = cvnOptions, onSelectionChanged = { selectedCvn = cvnOptions[it] }, modifier = Modifier.weight(1f))
            ModernDropdownField(label = "ARPC Key", value = skChoice, options = skChoices, onSelectionChanged = { skChoice = skChoices[it] }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = udk, onValueChange = { udk = it.uppercase() }, label = "UDK (ICC Master Key AC)", validation = MastercardValidationUtils.validateHexString(udk, 32))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC (9F36)", validation = MastercardValidationUtils.validateHexString(atc, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = un, onValueChange = { un = it.uppercase() }, label = "Unpredictable No. (9F37)", validation = MastercardValidationUtils.validateHexString(un, 8), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = cryptogram, onValueChange = { cryptogram = it.uppercase() }, label = "ARQC (9F26)", validation = MastercardValidationUtils.validateHexString(cryptogram, 16))
        Spacer(Modifier.height(8.dp))
        ModernDropdownField(label = "ARPC Generation Method", value = methodChoice, options = listOf("Method 1", "Method 2"), onSelectionChanged = { methodChoice = if (it == 0) "Method 1" else "Method 2" })
        Spacer(Modifier.height(8.dp))
        if (methodChoice == "Method 1") {
            ModernDropdownField(label = "Response Code", value = selectedResponseCode, options = standardResponseCodes, onSelectionChanged = { selectedResponseCode = standardResponseCodes[it] })
            AnimatedVisibility(visible = selectedResponseCode == "Custom...") {
                EnhancedTextField(value = customResponseCode, onValueChange = { customResponseCode = it.uppercase() }, label = "Custom Response Code (2 chars or 4 hex)", validation = ValidationResult(ValidationState.VALID))
            }
        } else {
            EnhancedTextField(value = csuData, onValueChange = { csuData = it.uppercase() }, label = "CSU (4 bytes)", validation = MastercardValidationUtils.validateHexString(csuData, 8))
        }
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate ARPC",
            onClick = {
                val inputs = mapOf("CVN" to selectedCvn, "UDK" to udk, "ATC" to atc, "UN" to un, "Cryptogram" to cryptogram, "Key" to skChoice, "Method" to methodChoice, "Response Code" to responseCode, "CSU" to csuData)
                try {
                    val txn = EmvCvn.TransactionData(ByteArray(6), ByteArray(6), ByteArray(2), ByteArray(5), ByteArray(2), ByteArray(3), ByteArray(1), un.hexToBytes(), ByteArray(2), atc.hexToBytes())
                    val key = if (skChoice == skChoices[0]) udk.hexToBytes() else EmvCvn.sessionKey(profile, udk.hexToBytes(), txn)
                    val arqc = cryptogram.hexToBytes()
                    val result = if (methodChoice == "Method 1") {
                        val arc = arcToBytes(responseCode)
                        val arpc = EmvAc.arpcMethod1(key, arqc, arc)
                        "ARPC key: ${key.toHexUpper()}\nARPC: ${arpc.toHexUpper()}\nIssuer Authentication Data (91): ${arpc.toHexUpper()}${arc.toHexUpper()}"
                    } else {
                        val arpc = EmvAc.arpcMethod2(key, arqc, csuData.hexToBytes())
                        "ARPC key: ${key.toHexUpper()}\nARPC: ${arpc.toHexUpper()}\nIssuer Authentication Data (91): ${arpc.toHexUpper()}${csuData.uppercase()}"
                    }
                    MastercardLogManager.logOperation(MastercardCryptoTabs.ARPC.title, "ARPC Generation", inputs, result = result)
                } catch (e: Exception) { MastercardLogManager.logOperation(MastercardCryptoTabs.ARPC.title, "ARPC Generation", inputs, error = e.message) }
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "MastercardButtonAnimation") { loading ->
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
