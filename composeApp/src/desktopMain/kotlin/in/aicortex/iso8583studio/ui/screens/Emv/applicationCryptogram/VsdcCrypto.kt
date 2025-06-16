package `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram

import ai.cortex.core.crypto.data.FieldValidation
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

// --- COMMON UI & VALIDATION FOR VSDC SCREEN ---


object VsdcValidationUtils {
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
fun VsdcCryptoScreen(
    
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = VsdcCryptoTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

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
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn()) with
                                    (slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "vsdc_tab_transition"
                    ) { tab ->
                        when (tab) {
                            VsdcCryptoTabs.UDK -> UdkTabVsdc()
                            VsdcCryptoTabs.SESSION_KEYS -> SessionKeysTabVsdc()
                            VsdcCryptoTabs.CRYPTOGRAM -> CryptogramTabVsdc()
                            VsdcCryptoTabs.ARPC -> ArpcTabVsdc()
                        }
                    }
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

@Composable
private fun UdkTabVsdc() {
    // This is generally scheme-agnostic, reusing layout
    var mdk by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var pan by remember { mutableStateOf("4999990011112222") }
    var panSeq by remember { mutableStateOf("01") }
    var udkOption by remember { mutableStateOf("Option A") }
    var keyParity by remember { mutableStateOf("Odd") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = listOf(mdk, pan, panSeq).all { it.isNotBlank() }

    ModernCryptoCard(title = "UDK Derivation", subtitle = "Generate Unique Derived Key", icon = Icons.Default.VpnKey) {
        EnhancedTextField(value = mdk, onValueChange = { mdk = it.uppercase() }, label = "MDK", validation = VsdcValidationUtils.validateHexString(mdk, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = pan, onValueChange = { pan = it }, label = "PAN", validation = VsdcValidationUtils.validateHexString(pan, 16))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = panSeq, onValueChange = { panSeq = it }, label = "PAN Sequence No.", validation = VsdcValidationUtils.validateHexString(panSeq, 2))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernDropdownField(label = "UDK Derivation Option", value = udkOption, options = listOf("Option A", "Option B"), onSelectionChanged = { udkOption = if (it == 0) "Option A" else "Option B" }, modifier = Modifier.weight(1f))
            ModernDropdownField(label = "Key Parity", value = keyParity, options = listOf("Odd", "Even", "None"), onSelectionChanged = { keyParity = listOf("Odd", "Even", "None")[it] }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate UDK",
            onClick = {
                isLoading = true
                val inputs = mapOf("MDK" to mdk, "PAN" to pan, "PAN Seq" to panSeq, "Option" to udkOption, "Parity" to keyParity)
                GlobalScope.launch {
                    delay(500)
                    val result = "UDK: 112233445566778899AABBCCDDEEFF00\nKCV: A1B2C3"
                    VsdcLogManager.logOperation(VsdcCryptoTabs.UDK.title, "UDK Generation", inputs, result = result, executionTime = 520)
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
private fun SessionKeysTabVsdc() {
    // Visa-specific session key derivation
    var udk by remember { mutableStateOf("112233445566778899AABBCCDDEEFF00") }
    var atc by remember { mutableStateOf("0001") }
    var unpredictableNumber by remember { mutableStateOf("1A2B3C4D") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = listOf(udk, atc, unpredictableNumber).all { it.isNotBlank() }

    ModernCryptoCard(title = "Session Keys (VSDC)", subtitle = "Derive session key from UDK", icon = Icons.Default.Lock) {
        EnhancedTextField(value = udk, onValueChange = { udk = it.uppercase() }, label = "UDK", validation = VsdcValidationUtils.validateHexString(udk, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC", validation = VsdcValidationUtils.validateHexString(atc, 4))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = unpredictableNumber, onValueChange = { unpredictableNumber = it.uppercase() }, label = "Unpredictable Number", validation = VsdcValidationUtils.validateHexString(unpredictableNumber, 8))
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate Session Key",
            onClick = {
                isLoading = true
                val inputs = mapOf("UDK" to udk, "ATC" to atc, "Unpredictable No." to unpredictableNumber)
                GlobalScope.launch {
                    delay(500)
                    val result = "Session Key: CCDDEEFF00112233445566778899AABB\nKCV: E5F6A1"
                    VsdcLogManager.logOperation(VsdcCryptoTabs.SESSION_KEYS.title, "Session Key Generation", inputs, result = result, executionTime = 510)
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CryptogramTabVsdc() {
    val cvnOptions = remember { listOf("CVN 10", "CVN 14", "CVN 17", "CVN 18") }
    var selectedCvn by remember { mutableStateOf(cvnOptions.first()) }

    var sessionKey by remember { mutableStateOf("CCDDEEFF00112233445566778899AABB") }
    var amount by remember { mutableStateOf("000000010000") }
    var amountOther by remember { mutableStateOf("000000000000") }
    var tvr by remember { mutableStateOf("0000008000") }
    var currencyCode by remember { mutableStateOf("0840") }
    var transDate by remember { mutableStateOf("250611") }
    var un by remember { mutableStateOf("1A2B3C4D") }
    var aip by remember { mutableStateOf("7C00") }
    var atc by remember { mutableStateOf("0001") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = listOf(sessionKey, amount, amountOther, tvr, currencyCode, transDate, un, aip, atc).all { it.isNotBlank() }

    ModernCryptoCard(title = "AAC/ARQC/TC Generation (VSDC)", subtitle = "Generate application cryptogram for Visa", icon = Icons.Default.Security) {
        ModernDropdownField(label = "Cryptogram Version (CVN)", value = selectedCvn, options = cvnOptions, onSelectionChanged = { selectedCvn = cvnOptions[it] })
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = sessionKey, onValueChange = { sessionKey = it.uppercase() }, label = "Session Key", validation = VsdcValidationUtils.validateHexString(sessionKey, 32))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = amount, onValueChange = { amount = it }, label = "Amount (9F02)", validation = VsdcValidationUtils.validateHexString(amount, 12), modifier = Modifier.weight(1f))
            EnhancedTextField(value = amountOther, onValueChange = { amountOther = it }, label = "Amount, Other (9F03)", validation = VsdcValidationUtils.validateHexString(amountOther, 12), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = tvr, onValueChange = { tvr = it.uppercase() }, label = "TVR (95)", validation = VsdcValidationUtils.validateHexString(tvr, 10), modifier = Modifier.weight(1f))
            EnhancedTextField(value = currencyCode, onValueChange = { currencyCode = it.uppercase() }, label = "Currency Code (5F2A)", validation = VsdcValidationUtils.validateHexString(currencyCode, 4), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = transDate, onValueChange = { transDate = it.uppercase() }, label = "Transaction Date (9A)", validation = VsdcValidationUtils.validateHexString(transDate, 6), modifier = Modifier.weight(1f))
            EnhancedTextField(value = un, onValueChange = { un = it.uppercase() }, label = "Unpredictable No. (9F37)", validation = VsdcValidationUtils.validateHexString(un, 8), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = aip, onValueChange = { aip = it.uppercase() }, label = "AIP (82)", validation = VsdcValidationUtils.validateHexString(aip, 4), modifier = Modifier.weight(1f))
            EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC (9F36)", validation = VsdcValidationUtils.validateHexString(atc, 4), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate Cryptogram",
            onClick = {
                isLoading = true
                val inputs = mapOf("CVN" to selectedCvn, "Session Key" to sessionKey, "Amount" to amount, "TVR" to tvr, "ATC" to atc)
                GlobalScope.launch {
                    delay(500)
                    val result = "ARQC: 1020304050607080"
                    VsdcLogManager.logOperation(VsdcCryptoTabs.CRYPTOGRAM.title, "Cryptogram Generation", inputs, result = result, executionTime = 515)
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Shield, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ArpcTabVsdc() {
    val standardResponseCodes = remember { listOf("00: Approved", "01: Refer to issuer", "05: Do not honor", "51: Insufficient funds", "Z1: Offline Approved", "Custom...") }
    var sessionKey by remember { mutableStateOf("CCDDEEFF00112233445566778899AABB") }
    var cryptogram by remember { mutableStateOf("1020304050607080") }
    var arpcMethod by remember { mutableStateOf("Method 1") }
    var selectedResponseCode by remember { mutableStateOf(standardResponseCodes.first()) }
    var customResponseCode by remember { mutableStateOf("") }
    var csuData by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = listOf(sessionKey, cryptogram, csuData).all { it.isNotBlank() } && (selectedResponseCode != "Custom..." || customResponseCode.length == 2)

    ModernCryptoCard(title = "ARPC Generation", subtitle = "Generate Authorization Response Cryptogram", icon = Icons.Default.Reply) {
        EnhancedTextField(value = sessionKey, onValueChange = { sessionKey = it.uppercase() }, label = "Session Key", validation = VsdcValidationUtils.validateHexString(sessionKey, 32))
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = cryptogram, onValueChange = { cryptogram = it.uppercase() }, label = "AAC/ARQC/TC", validation = VsdcValidationUtils.validateHexString(cryptogram, 16))
        Spacer(Modifier.height(8.dp))
        ModernDropdownField(label = "ARPC Generation Method", value = arpcMethod, options = listOf("Method 1", "Method 2"), onSelectionChanged = { arpcMethod = if (it == 0) "Method 1" else "Method 2" })
        Spacer(Modifier.height(8.dp))
        ModernDropdownField(label = "Response Code", value = selectedResponseCode, options = standardResponseCodes, onSelectionChanged = { selectedResponseCode = standardResponseCodes[it] })
        AnimatedVisibility(visible = selectedResponseCode == "Custom...") {
            EnhancedTextField(value = customResponseCode, onValueChange = { customResponseCode = it.uppercase() }, label = "Custom Response Code", validation = VsdcValidationUtils.validateHexString(customResponseCode, 2))
        }
        Spacer(Modifier.height(8.dp))
        EnhancedTextField(value = csuData, onValueChange = { csuData = it.uppercase() }, label = "CSU / Proprietary Auth Data", validation = VsdcValidationUtils.validateHexString(csuData, allowEmpty = true))
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate ARPC",
            onClick = {
                isLoading = true
                val responseCode = if (selectedResponseCode == "Custom...") customResponseCode else selectedResponseCode.split(":")[0]
                val inputs = mapOf("Session Key" to sessionKey, "Cryptogram" to cryptogram, "Method" to arpcMethod, "Response Code" to responseCode, "CSU Data" to csuData)
                GlobalScope.launch {
                    delay(500)
                    val result = "ARPC: FEDCBA9876543210"
                    VsdcLogManager.logOperation(VsdcCryptoTabs.ARPC.title, "ARPC Generation", inputs, result = result, executionTime = 512)
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
