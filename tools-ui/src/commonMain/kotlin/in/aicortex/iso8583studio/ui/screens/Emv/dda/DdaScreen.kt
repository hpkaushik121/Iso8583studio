package `in`.aicortex.iso8583studio.ui.screens.Emv.dda

import `in`.aicortex.iso8583studio.ui.chrome.ToolTab

import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec

import `in`.aicortex.iso8583studio.ui.chrome.ToolLog

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import io.cryptocalc.rsa.RsaPublicKey

import io.cryptocalc.math.BigInt

import io.cryptocalc.emv.EmvOda

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
// --- COMMON UI & VALIDATION (Can be extracted to a shared module) ---


object DdaValidationUtils {
    fun validateHexString(
        value: String,
        allowEmpty: Boolean = false,
        friendlyName: String = "Field"
    ): ValidationResult {
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
        return ValidationResult(ValidationState.VALID, "", "${value.length} chars")
    }
}

// --- DDA SCREEN ---

enum class DdaTabs(val title: String, val icon: ImageVector) {
    RETRIEVE_ISSUER_PK("Retrieve Issuer PK", Icons.Default.VpnKey),
    RETRIEVE_ICC_PK("Retrieve ICC PK", Icons.Default.Contactless),
    VERIFY_SDAD("Verify SDAD", Icons.Default.VerifiedUser)
}

object DdaLogManager {
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

    fun logOperation(
        tabTitle: String,
        operation: String,
        inputs: Map<String, String>,
        result: String? = null,
        error: String? = null,
        executionTime: Long = 0L
    ) {
        if (result == null && error == null) return
        Analytics.calculationRun(operation, success = result != null, durationMs = executionTime)

        val timestamp = nowLogTimestamp()
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                val displayValue = if (key.contains("key", ignoreCase = true) || key.contains("modulus", ignoreCase = true) || key.contains("certificate", ignoreCase = true)) {
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
        addLog(
            tabTitle,
            LogEntry(timestamp = timestamp, type = logType, message = message, details = details)
        )
    }
}

@Composable
fun DdaScreen(
    
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = DdaTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    val body: @Composable () -> Unit = {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        when (tab) {
                            DdaTabs.RETRIEVE_ISSUER_PK -> RetrieveIssuerPkTabDda()
                            DdaTabs.RETRIEVE_ICC_PK -> RetrieveIccPkTab()
                            DdaTabs.VERIFY_SDAD -> VerifySdadTabDda()
                        }
                    }
                
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "DDA - Dynamic Data Authentication", onBack = onBack, tabs = tabList.map { ToolTab(it.title, it.icon) }, selectedTab = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, log = ToolLog(DdaLogManager.getLogEntries(selectedTab.title), onClear = { DdaLogManager.clearLogs(selectedTab.title) })), content = body)
        return
    }
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "DDA - Dynamic Data Authentication",
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
                            onClearClick = { DdaLogManager.clearLogs(selectedTab.title) },
                            logEntries = DdaLogManager.getLogEntries(selectedTab.title)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RetrieveIssuerPkTabDda() {
    var caPkModulus by remember { mutableStateOf("") }
    var caPkExponent by remember { mutableStateOf("03") }
    var issuerPkCert by remember { mutableStateOf("") }
    var issuerPkRemainder by remember { mutableStateOf("") }
    var issuerPkExponent by remember { mutableStateOf("03") }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(title = "Retrieve Issuer Public Key", onDismissRequest = { showInfoDialog = false }) {
            Text("EMV 4.3 Book 2 §6.3: the Issuer PK Certificate (tag 90) is recovered with the CA public key, the modulus completed with the remainder (tag 92) and the SHA-1 hash checked over the recovered data ‖ remainder ‖ exponent (9F32).", style = MaterialTheme.typography.body2)
        }
    }

    val isFormValid = listOf(caPkModulus, caPkExponent, issuerPkCert, issuerPkExponent).all { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernCryptoCard(title = "Retrieve Issuer Public Key", subtitle = "Recover the Issuer PK using the CA Public Key", icon = Icons.Default.VpnKey, onInfoClick = { showInfoDialog = true }) {
            EnhancedTextField(value = caPkModulus, onValueChange = { caPkModulus = it.uppercase() }, label = "CA PK Modulus (Hex)", validation = DdaValidationUtils.validateHexString(caPkModulus), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = caPkExponent, onValueChange = { caPkExponent = it.uppercase() }, label = "CA PK Exponent (Hex)", validation = DdaValidationUtils.validateHexString(caPkExponent))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = issuerPkCert, onValueChange = { issuerPkCert = it.uppercase() }, label = "Issuer PK Certificate (tag 90)", validation = DdaValidationUtils.validateHexString(issuerPkCert), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = issuerPkRemainder, onValueChange = { issuerPkRemainder = it.uppercase() }, label = "Issuer PK Remainder (tag 92, optional)", validation = DdaValidationUtils.validateHexString(issuerPkRemainder, allowEmpty = true))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = issuerPkExponent, onValueChange = { issuerPkExponent = it.uppercase() }, label = "Issuer PK Exponent (9F32)", validation = DdaValidationUtils.validateHexString(issuerPkExponent))
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Retrieve Key",
                onClick = {
                    val inputs = mapOf("CA PK Modulus" to caPkModulus, "Issuer PK Certificate" to issuerPkCert, "Remainder" to issuerPkRemainder, "Exponent" to issuerPkExponent)
                    try {
                        val ca = RsaPublicKey(BigInt.fromHex(caPkModulus), BigInt.fromHex(caPkExponent))
                        val r = EmvOda.recoverIssuerPublicKey(ca, issuerPkCert.hexToBytes(), if (issuerPkRemainder.isBlank()) ByteArray(0) else issuerPkRemainder.hexToBytes(), issuerPkExponent.hexToBytes())
                        val text = "Recovered data: ${r.recovered.toHexUpper()}\nIssuer identifier: ${r.identifier}\nExpiry (MMYY): ${r.expiry}\nSerial: ${r.serial}\nIssuer PK modulus (${r.modulus.size} bytes): ${r.modulus.toHexUpper()}\nSHA-1 hash: ${if (r.hashValid) "VALID" else "INVALID"}" + (if (r.notes.isNotEmpty()) "\nNotes: " + r.notes.joinToString("; ") else "")
                        DdaLogManager.logOperation(DdaTabs.RETRIEVE_ISSUER_PK.title, "Retrieve Issuer PK", inputs, result = if (r.hashValid) text else null, error = if (r.hashValid) null else text)
                    } catch (e: Exception) {
                        DdaLogManager.logOperation(DdaTabs.RETRIEVE_ISSUER_PK.title, "Retrieve Issuer PK", inputs, error = e.message)
                    }
                },
                enabled = isFormValid, icon = Icons.Default.EnhancedEncryption, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RetrieveIccPkTab() {
    var issuerPkModulus by remember { mutableStateOf("") }
    var issuerPkExponent by remember { mutableStateOf("03") }
    var iccPkCert by remember { mutableStateOf("") }
    var iccPkRemainder by remember { mutableStateOf("") }
    var iccPkExponent by remember { mutableStateOf("03") }
    var staticData by remember { mutableStateOf("") }
    var aip by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(title = "Retrieve ICC Public Key", onDismissRequest = { showInfoDialog = false }) {
            Text("EMV 4.3 Book 2 §6.4: the ICC PK Certificate (9F46) is recovered with the issuer public key; the block is 6A ‖ 04 ‖ PAN ‖ expiry ‖ serial ‖ 01 ‖ 01 ‖ key length ‖ exponent length ‖ leftmost modulus ‖ SHA-1 ‖ BC. The hash covers the recovered data ‖ remainder (9F48) ‖ exponent (9F47) ‖ static data to be authenticated (‖ AIP when 9F4A lists tag 82).", style = MaterialTheme.typography.body2)
        }
    }

    val isFormValid = listOf(issuerPkModulus, issuerPkExponent, iccPkCert, iccPkExponent).all { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernCryptoCard(title = "Retrieve ICC Public Key", subtitle = "Recover the card's unique public key", icon = Icons.Default.Contactless, onInfoClick = { showInfoDialog = true }) {
            EnhancedTextField(value = issuerPkModulus, onValueChange = { issuerPkModulus = it.uppercase() }, label = "Issuer PK Modulus (Hex)", validation = DdaValidationUtils.validateHexString(issuerPkModulus), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = issuerPkExponent, onValueChange = { issuerPkExponent = it.uppercase() }, label = "Issuer PK Exponent (9F32)", validation = DdaValidationUtils.validateHexString(issuerPkExponent))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = iccPkCert, onValueChange = { iccPkCert = it.uppercase() }, label = "ICC PK Certificate (9F46)", validation = DdaValidationUtils.validateHexString(iccPkCert), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = iccPkRemainder, onValueChange = { iccPkRemainder = it.uppercase() }, label = "ICC PK Remainder (9F48, optional)", validation = DdaValidationUtils.validateHexString(iccPkRemainder, allowEmpty = true))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = iccPkExponent, onValueChange = { iccPkExponent = it.uppercase() }, label = "ICC PK Exponent (9F47)", validation = DdaValidationUtils.validateHexString(iccPkExponent))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = staticData, onValueChange = { staticData = it.uppercase() }, label = "Static Data to be Authenticated (AFL records)", validation = DdaValidationUtils.validateHexString(staticData, allowEmpty = true), maxLines = 3)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = aip, onValueChange = { aip = it.uppercase() }, label = "AIP (82) - only if 9F4A lists it", validation = DdaValidationUtils.validateHexString(aip, allowEmpty = true))
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Retrieve ICC Key",
                onClick = {
                    val inputs = mapOf("Issuer PK Modulus" to issuerPkModulus, "ICC PK Certificate" to iccPkCert, "Remainder" to iccPkRemainder, "Exponent" to iccPkExponent, "Static Data" to staticData, "AIP" to aip)
                    try {
                        val issuer = RsaPublicKey(BigInt.fromHex(issuerPkModulus), BigInt.fromHex(issuerPkExponent))
                        val sd = (if (staticData.isBlank()) ByteArray(0) else staticData.hexToBytes()) + (if (aip.isBlank()) ByteArray(0) else aip.hexToBytes())
                        val r = EmvOda.recoverIccPublicKey(issuer, iccPkCert.hexToBytes(), if (iccPkRemainder.isBlank()) ByteArray(0) else iccPkRemainder.hexToBytes(), iccPkExponent.hexToBytes(), sd)
                        val text = "Recovered data: ${r.recovered.toHexUpper()}\nPAN: ${r.identifier}\nExpiry (MMYY): ${r.expiry}\nSerial: ${r.serial}\nICC PK modulus (${r.modulus.size} bytes): ${r.modulus.toHexUpper()}\nSHA-1 hash: ${if (r.hashValid) "VALID" else "INVALID"}" + (if (r.notes.isNotEmpty()) "\nNotes: " + r.notes.joinToString("; ") else "")
                        DdaLogManager.logOperation(DdaTabs.RETRIEVE_ICC_PK.title, "Retrieve ICC PK", inputs, result = if (r.hashValid) text else null, error = if (r.hashValid) null else text)
                    } catch (e: Exception) {
                        DdaLogManager.logOperation(DdaTabs.RETRIEVE_ICC_PK.title, "Retrieve ICC PK", inputs, error = e.message)
                    }
                },
                enabled = isFormValid, icon = Icons.Default.EnhancedEncryption, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun VerifySdadTabDda() {
    var iccPkModulus by remember { mutableStateOf("") }
    var iccPkExponent by remember { mutableStateOf("03") }
    var sdad by remember { mutableStateOf("") }
    var dynamicData by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(title = "Verify Signed Dynamic Application Data (SDAD)", onDismissRequest = { showInfoDialog = false }) {
            Text("EMV 4.3 Book 2 §6.5: the SDAD (9F4B) is recovered with the ICC public key; the block is 6A ‖ 05 ‖ 01 ‖ LDD ‖ ICC dynamic data (IDN length ‖ IDN ‖ ...) ‖ BB padding ‖ SHA-1 ‖ BC. The hash covers the recovered data followed by the terminal dynamic data built from the DDOL (typically the unpredictable number 9F37).", style = MaterialTheme.typography.body2)
        }
    }

    val isFormValid = listOf(iccPkModulus, iccPkExponent, sdad).all { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernCryptoCard(title = "Verify SDAD (Dynamic Signature)", subtitle = "Verify the card's dynamic signature", icon = Icons.Default.VerifiedUser, onInfoClick = { showInfoDialog = true }) {
            EnhancedTextField(value = iccPkModulus, onValueChange = { iccPkModulus = it.uppercase() }, label = "ICC PK Modulus (Hex)", validation = DdaValidationUtils.validateHexString(iccPkModulus), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = iccPkExponent, onValueChange = { iccPkExponent = it.uppercase() }, label = "ICC PK Exponent (9F47)", validation = DdaValidationUtils.validateHexString(iccPkExponent))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = sdad, onValueChange = { sdad = it.uppercase() }, label = "SDAD (9F4B)", validation = DdaValidationUtils.validateHexString(sdad), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = dynamicData, onValueChange = { dynamicData = it.uppercase() }, label = "Terminal Dynamic Data (DDOL values, e.g. 9F37)", validation = DdaValidationUtils.validateHexString(dynamicData, allowEmpty = true))
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Verify Dynamic Signature",
                onClick = {
                    val inputs = mapOf("ICC PK Modulus" to iccPkModulus, "SDAD" to sdad, "Dynamic Data" to dynamicData)
                    try {
                        val key = RsaPublicKey(BigInt.fromHex(iccPkModulus), BigInt.fromHex(iccPkExponent))
                        val r = EmvOda.verifySdad(key, sdad.hexToBytes(), if (dynamicData.isBlank()) ByteArray(0) else dynamicData.hexToBytes())
                        val text = "Recovered data: ${r.recovered.toHexUpper()}\nICC dynamic data: ${r.dynamicData.toHexUpper()}\nICC Dynamic Number (9F4C): ${r.iccDynamicNumber.toHexUpper()}\nSHA-1 hash: ${if (r.hashValid) "MATCH" else "MISMATCH"}" + (if (r.notes.isNotEmpty()) "\nNotes: " + r.notes.joinToString("; ") else "")
                        DdaLogManager.logOperation(DdaTabs.VERIFY_SDAD.title, "Verify SDAD", inputs, result = if (r.hashValid) text else null, error = if (r.hashValid) null else text)
                    } catch (e: Exception) {
                        DdaLogManager.logOperation(DdaTabs.VERIFY_SDAD.title, "Verify SDAD", inputs, error = e.message)
                    }
                },
                enabled = isFormValid, icon = Icons.Default.Security, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


// --- SHARED UI COMPONENTS ---
@Composable
fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    validation: ValidationResult
) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.TextField(value, onValueChange, label, modifier, maxLines, validation, ""); return }
    Column(modifier = modifier) {
        FixedOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = maxLines,
            isError = validation.state == ValidationState.ERROR,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.primary
                    ValidationState.WARNING -> Color(0xFFFFC107)
                    ValidationState.ERROR -> MaterialTheme.colors.error
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                },
                unfocusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    ValidationState.WARNING -> Color(0xFFFFC107)
                    ValidationState.ERROR -> MaterialTheme.colors.error
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                }
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                color = when (validation.state) {
                    ValidationState.VALID -> SuccessGreen
                    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                },
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun ModernCryptoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onInfoClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Card(title, subtitle, icon, onInfoClick, content); return }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.colors.surface
    ) {
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Button(text, onClick, modifier, isLoading, enabled, icon); return }
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled && !isLoading,
        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp, disabledElevation = 0.dp)
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn() with fadeOut() },
            label = "DdaButtonAnimation"
        ) { loading ->
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
fun InfoDialog(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
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
        confirmButton = {
            Button(onClick = onDismissRequest) { Text("OK") }
        },
        shape = MaterialTheme.shapes.medium
    )
}

fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2

    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset)
        .width(indicatorWidth)
}
