package `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram

import ai.cortex.core.crypto.data.FieldValidation
import ai.cortex.core.crypto.data.KeyParity
import ai.cortex.core.crypto.data.ValidationState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object MastercardValidationUtils {
    fun validateHexString(value: String, expectedLength: Int? = null, allowEmpty: Boolean = false, friendlyName: String = "Field"): FieldValidation {
        if (value.isEmpty()) {
            return if (allowEmpty) FieldValidation(ValidationState.EMPTY, "", "Enter hex characters")
            else FieldValidation(ValidationState.ERROR, "$friendlyName is required", "Enter hex characters")
        }
        if (!value.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed", "${value.length} chars")
        }
        if (value.length % 2 != 0) {
            return FieldValidation(ValidationState.ERROR, "Must have an even number of characters", "${value.length} chars")
        }
        expectedLength?.let {
            if (value.length != it) return FieldValidation(ValidationState.ERROR, "Must be exactly $it characters", "${value.length}/$it chars")
        }
        return FieldValidation(ValidationState.VALID, "", "${value.length} chars")
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
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
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

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MastercardCryptoScreen(
    
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = MastercardCryptoTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

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
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn()) with
                                    (slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "mastercard_tab_transition"
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

@Composable
private fun UdkTab() {
    var mdk by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var pan by remember { mutableStateOf("5413330011112222") }
    var panSeq by remember { mutableStateOf("01") }
    var udkOption by remember { mutableStateOf("Option A") }
    var keyParity by remember { mutableStateOf(KeyParity.NONE) }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = listOf(mdk, pan, panSeq).all { it.isNotBlank() }

    ModernCryptoCard(title = "UDK Derivation", subtitle = "Generate Unique Derived Key", icon = Icons.Default.VpnKey) {
        EnhancedTextField(value = mdk, onValueChange = { mdk = it.uppercase() }, label = "MDK", validation = MastercardValidationUtils.validateHexString(mdk, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = pan, onValueChange = { pan = it }, label = "PAN", validation = MastercardValidationUtils.validateHexString(pan, 16))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = panSeq, onValueChange = { panSeq = it }, label = "PAN Sequence No.", validation = MastercardValidationUtils.validateHexString(panSeq, 2))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernDropdownField(label = "UDK Derivation Option", value = udkOption, options = listOf("Option A", "Option B"), onSelectionChanged = { udkOption = if (it == 0) "Option A" else "Option B" }, modifier = Modifier.weight(1f))
            ModernDropdownField(label = "Key Parity", value = keyParity.name, options = KeyParity.values().map { it.name }, onSelectionChanged = {
                keyParity = KeyParity.values()[it]
            }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate UDK",
            onClick = {
                isLoading = true
                val inputs = mapOf("MDK" to mdk, "PAN" to pan, "PAN Seq" to panSeq, "Option" to udkOption, "Parity" to keyParity.name)
                GlobalScope.launch {
                    delay(500)
                    val result = "UDK: 112233445566778899AABBCCDDEEFF00\nKCV: A1B2C3"
                    MastercardLogManager.logOperation(MastercardCryptoTabs.UDK.title, "UDK Generation", inputs, result = result, executionTime = 520)
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SessionKeyEmv2000Tab() {
    // Fields for EMV 2000 Session Key
    var masterKey by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var initialVector by remember { mutableStateOf("0000000000000000") }
    var atc by remember { mutableStateOf("0001") }
    var branchFactor by remember { mutableStateOf("1") }
    var height by remember { mutableStateOf("0") }
    var keyParity by remember { mutableStateOf("Odd") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = listOf(masterKey, initialVector, atc, branchFactor, height).all { it.isNotBlank() }

    ModernCryptoCard(title = "Session Key (EMV 2000)", subtitle = "Derive session key using EMV 2000 method", icon = Icons.Default.Key) {
        EnhancedTextField(value = masterKey, onValueChange = { masterKey = it.uppercase() }, label = "Master Key", validation = MastercardValidationUtils.validateHexString(masterKey, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = initialVector, onValueChange = { initialVector = it.uppercase() }, label = "Initial Vector", validation = MastercardValidationUtils.validateHexString(initialVector, 16))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC", validation = MastercardValidationUtils.validateHexString(atc, 4))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = branchFactor, onValueChange = { branchFactor = it }, label = "Branch Factor", validation = MastercardValidationUtils.validateHexString(branchFactor), modifier = Modifier.weight(1f))
            EnhancedTextField(value = height, onValueChange = { height = it }, label = "Height", validation = MastercardValidationUtils.validateHexString(height), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        ModernDropdownField(label = "Key Parity", value = keyParity, options = listOf("Odd", "Even", "None"), onSelectionChanged = { keyParity = listOf("Odd", "Even", "None")[it] })
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate Session Key",
            onClick = {
                isLoading = true
                val inputs = mapOf("Master Key" to masterKey, "Initial Vector" to initialVector, "ATC" to atc, "Branch Factor" to branchFactor, "Height" to height, "Parity" to keyParity)
                GlobalScope.launch {
                    delay(500)
                    val result = "Session Key: AABBCCDDEEFF00112233445566778899\nKCV: D4E5F6"
                    MastercardLogManager.logOperation(MastercardCryptoTabs.SESSION_KEY_EMV_2000.title, "Session Key (EMV 2000)", inputs, result = result, executionTime = 530)
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SessionKeysTab() {
    var udk by remember { mutableStateOf("112233445566778899AABBCCDDEEFF00") }
    var atc by remember { mutableStateOf("0001") }
    var unpredictableNumber by remember { mutableStateOf("1A2B3C4D") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = listOf(udk, atc, unpredictableNumber).all { it.isNotBlank() }

    ModernCryptoCard(title = "Session Keys", subtitle = "Derive session key from UDK", icon = Icons.Default.Lock) {
        EnhancedTextField(value = udk, onValueChange = { udk = it.uppercase() }, label = "UDK", validation = MastercardValidationUtils.validateHexString(udk, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC", validation = MastercardValidationUtils.validateHexString(atc, 4))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = unpredictableNumber, onValueChange = { unpredictableNumber = it.uppercase() }, label = "Unpredictable Number", validation = MastercardValidationUtils.validateHexString(unpredictableNumber, 8))
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate Session Key",
            onClick = {
                isLoading = true
                val inputs = mapOf("UDK" to udk, "ATC" to atc, "Unpredictable No." to unpredictableNumber)
                GlobalScope.launch {
                    delay(500)
                    val result = "Session Key: CCDDEEFF00112233445566778899AABB\nKCV: E5F6A1"
                    MastercardLogManager.logOperation(MastercardCryptoTabs.SESSION_KEYS.title, "Session Key Generation", inputs, result = result, executionTime = 510)
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CryptogramTab() {
    var sessionKey by remember { mutableStateOf("CCDDEEFF00112233445566778899AABB") }
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
    var cvr by remember { mutableStateOf("020000000000") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = listOf(sessionKey, amount, amountOther, terminalCountry, tvr, currencyCode, transDate, transType, un, aip, atc, cvr).all { it.isNotBlank() }

    ModernCryptoCard(title = "AAC/ARQC/TC Generation", subtitle = "Generate application cryptogram", icon = Icons.Default.Security) {
        EnhancedTextField(value = sessionKey, onValueChange = { sessionKey = it.uppercase() }, label = "Session Key", validation = MastercardValidationUtils.validateHexString(sessionKey, 32))
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
        EnhancedTextField(value = cvr, onValueChange = { cvr = it.uppercase() }, label = "CVR (from 9F10)", validation = MastercardValidationUtils.validateHexString(cvr, 12))
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate Cryptogram",
            onClick = {
                isLoading = true
                val inputs = mapOf("Session Key" to sessionKey, "Amount" to amount, "TVR" to tvr, "ATC" to atc)
                GlobalScope.launch {
                    delay(500)
                    val result = "ARQC: FEDCBA9876543210"
                    MastercardLogManager.logOperation(MastercardCryptoTabs.CRYPTOGRAM.title, "Cryptogram Generation", inputs, result = result, executionTime = 515)
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Shield, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ArpcTab() {
    val standardResponseCodes = remember { listOf("00: Approved", "01: Refer to issuer", "05: Do not honor", "51: Insufficient funds", "Z1: Offline Approved", "Custom...") }
    var sessionKey by remember { mutableStateOf("CCDDEEFF00112233445566778899AABB") }
    var cryptogram by remember { mutableStateOf("FEDCBA9876543210") }
    var arpcMethod by remember { mutableStateOf("Method 1") }
    var selectedResponseCode by remember { mutableStateOf(standardResponseCodes.first()) }
    var customResponseCode by remember { mutableStateOf("") }
    var csuData by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = listOf(sessionKey, cryptogram, csuData).all { it.isNotBlank() } && (selectedResponseCode != "Custom..." || customResponseCode.length == 2)

    ModernCryptoCard(title = "ARPC Generation", subtitle = "Generate Authorization Response Cryptogram", icon = Icons.Default.Reply) {
        EnhancedTextField(value = sessionKey, onValueChange = { sessionKey = it.uppercase() }, label = "Session Key", validation = MastercardValidationUtils.validateHexString(sessionKey, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = cryptogram, onValueChange = { cryptogram = it.uppercase() }, label = "AAC/ARQC/TC", validation = MastercardValidationUtils.validateHexString(cryptogram, 16))
        Spacer(Modifier.height(8.dp))
        ModernDropdownField(label = "ARPC Generation Method", value = arpcMethod, options = listOf("Method 1", "Method 2"), onSelectionChanged = { arpcMethod = if (it == 0) "Method 1" else "Method 2" })
        Spacer(Modifier.height(8.dp))
        ModernDropdownField(label = "Response Code", value = selectedResponseCode, options = standardResponseCodes, onSelectionChanged = { selectedResponseCode = standardResponseCodes[it] })
        AnimatedVisibility(visible = selectedResponseCode == "Custom...") {
            EnhancedTextField(value = customResponseCode, onValueChange = { customResponseCode = it.uppercase() }, label = "Custom Response Code", validation = MastercardValidationUtils.validateHexString(customResponseCode, 2))
        }
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = csuData, onValueChange = { csuData = it.uppercase() }, label = "CSU / Proprietary Auth Data", validation = MastercardValidationUtils.validateHexString(csuData, allowEmpty = true))
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate ARPC",
            onClick = {
                isLoading = true
                val responseCode = if (selectedResponseCode == "Custom...") customResponseCode else selectedResponseCode.split(":")[0]
                val inputs = mapOf("Session Key" to sessionKey, "Cryptogram" to cryptogram, "Method" to arpcMethod, "Response Code" to responseCode, "CSU Data" to csuData)
                GlobalScope.launch {
                    delay(500)
                    val result = "ARPC: 0123456789ABCDEF"
                    MastercardLogManager.logOperation(MastercardCryptoTabs.ARPC.title, "ARPC Generation", inputs, result = result, executionTime = 512)
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Security, modifier = Modifier.fillMaxWidth()
        )
    }
}


// --- SHARED UI COMPONENTS ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: FieldValidation) {
    Column(modifier = modifier) {
        OutlinedTextField(
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
