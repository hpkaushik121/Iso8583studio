package `in`.aicortex.iso8583studio.ui.screens.Emv.sda

import `in`.aicortex.iso8583studio.ui.chrome.toolScroll

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


object SdaValidationUtils {
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

// --- SDA SCREEN ---

enum class SdaTabs(val title: String, val icon: ImageVector) {
    RETRIEVE_ISSUER_PK("Retrieve Issuer PK", Icons.Default.VpnKey),
    VERIFY_SSAD("Verify SSAD", Icons.Default.VerifiedUser)
}

object SdaLogManager {
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
        addLog(tabTitle, LogEntry(timestamp = timestamp, type= logType, message = message, details = details))
    }
}

@Composable
fun SdaScreen(
    
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = SdaTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    val body: @Composable () -> Unit = {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        when (tab) {
                            SdaTabs.RETRIEVE_ISSUER_PK -> RetrieveIssuerPkTab()
                            SdaTabs.VERIFY_SSAD -> VerifySsadTab()
                        }
                    }
                
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "SDA - Static Data Authentication", onBack = onBack, tabs = tabList.map { ToolTab(it.title, it.icon) }, selectedTab = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, log = ToolLog(SdaLogManager.getLogEntries(selectedTab.title), onClear = { SdaLogManager.clearLogs(selectedTab.title) })), content = body)
        return
    }
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "SDA - Static Data Authentication",
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
                            onClearClick = { SdaLogManager.clearLogs(selectedTab.title) },
                            logEntries = SdaLogManager.getLogEntries(selectedTab.title)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RetrieveIssuerPkTab() {
    var caPkModulus by remember { mutableStateOf("") }
    var caPkExponent by remember { mutableStateOf("03") }
    var issuerPkCert by remember { mutableStateOf("") }
    var issuerPkRemainder by remember { mutableStateOf("") }
    var issuerPkExponent by remember { mutableStateOf("03") }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(title = "Retrieve Issuer Public Key", onDismissRequest = { showInfoDialog = false }) {
            Text("EMV 4.3 Book 2 §6.3: the Issuer PK Certificate (tag 90) is recovered with the CA public key (RSA public operation). The recovered block is 6A ‖ 02 ‖ issuer id ‖ expiry ‖ serial ‖ 01 ‖ 01 ‖ key length ‖ exponent length ‖ leftmost modulus bytes ‖ SHA-1 ‖ BC. The modulus is completed with the remainder (tag 92) and the hash is checked over the recovered data ‖ remainder ‖ exponent (9F32).", style = MaterialTheme.typography.body2)
        }
    }

    val isFormValid = listOf(caPkModulus, caPkExponent, issuerPkCert, issuerPkExponent).all { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxSize().toolScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernCryptoCard(title = "Retrieve Issuer Public Key", subtitle = "Recover the Issuer PK using the CA Public Key", icon = Icons.Default.VpnKey, onInfoClick = { showInfoDialog = true }) {
            EnhancedTextField(value = caPkModulus, onValueChange = { caPkModulus = it.uppercase() }, label = "CA PK Modulus (Hex)", validation = SdaValidationUtils.validateHexString(caPkModulus), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = caPkExponent, onValueChange = { caPkExponent = it.uppercase() }, label = "CA PK Exponent (Hex, 03 or 010001)", validation = SdaValidationUtils.validateHexString(caPkExponent))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = issuerPkCert, onValueChange = { issuerPkCert = it.uppercase() }, label = "Issuer PK Certificate (tag 90)", validation = SdaValidationUtils.validateHexString(issuerPkCert), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = issuerPkRemainder, onValueChange = { issuerPkRemainder = it.uppercase() }, label = "Issuer PK Remainder (tag 92, optional)", validation = SdaValidationUtils.validateHexString(issuerPkRemainder, allowEmpty = true))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = issuerPkExponent, onValueChange = { issuerPkExponent = it.uppercase() }, label = "Issuer PK Exponent (tag 9F32)", validation = SdaValidationUtils.validateHexString(issuerPkExponent))
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Retrieve Key",
                onClick = {
                    val inputs = mapOf("CA PK Modulus" to caPkModulus, "CA PK Exponent" to caPkExponent, "Issuer PK Certificate" to issuerPkCert, "Remainder" to issuerPkRemainder, "Exponent" to issuerPkExponent)
                    try {
                        val ca = RsaPublicKey(BigInt.fromHex(caPkModulus), BigInt.fromHex(caPkExponent))
                        val r = EmvOda.recoverIssuerPublicKey(ca, issuerPkCert.hexToBytes(), if (issuerPkRemainder.isBlank()) ByteArray(0) else issuerPkRemainder.hexToBytes(), issuerPkExponent.hexToBytes())
                        val text = "Recovered data: ${r.recovered.toHexUpper()}\nIssuer identifier: ${r.identifier}\nExpiry (MMYY): ${r.expiry}\nSerial: ${r.serial}\nIssuer PK modulus (${r.modulus.size} bytes): ${r.modulus.toHexUpper()}\nSHA-1 hash: ${if (r.hashValid) "VALID" else "INVALID"}" + (if (r.notes.isNotEmpty()) "\nNotes: " + r.notes.joinToString("; ") else "")
                        SdaLogManager.logOperation(SdaTabs.RETRIEVE_ISSUER_PK.title, "Retrieve Issuer PK", inputs, result = if (r.hashValid) text else null, error = if (r.hashValid) null else text)
                    } catch (e: Exception) {
                        SdaLogManager.logOperation(SdaTabs.RETRIEVE_ISSUER_PK.title, "Retrieve Issuer PK", inputs, error = e.message)
                    }
                },
                enabled = isFormValid, icon = Icons.Default.EnhancedEncryption, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun VerifySsadTab() {
    var issuerPkModulus by remember { mutableStateOf("") }
    var issuerPkExponent by remember { mutableStateOf("03") }
    var ssad by remember { mutableStateOf("") }
    var staticData by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(title = "Verify Signed Static Application Data (SSAD)", onDismissRequest = { showInfoDialog = false }) {
            Text("EMV 4.3 Book 2 §5.4: the SSAD (tag 93) is recovered with the issuer public key; the block is 6A ‖ 03 ‖ 01 ‖ DAC (9F45) ‖ BB padding ‖ SHA-1 ‖ BC. The hash covers the recovered data (without header, hash and trailer) followed by the static data to be authenticated: the records flagged in the AFL, in order, plus the AIP when 9F4A lists tag 82.", style = MaterialTheme.typography.body2)
        }
    }

    val isFormValid = listOf(issuerPkModulus, issuerPkExponent, ssad).all { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxSize().toolScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernCryptoCard(title = "Verify SSAD", subtitle = "Verify Signed Static Application Data", icon = Icons.Default.VerifiedUser, onInfoClick = { showInfoDialog = true }) {
            EnhancedTextField(value = issuerPkModulus, onValueChange = { issuerPkModulus = it.uppercase() }, label = "Issuer PK Modulus (Hex)", validation = SdaValidationUtils.validateHexString(issuerPkModulus), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = issuerPkExponent, onValueChange = { issuerPkExponent = it.uppercase() }, label = "Issuer PK Exponent (9F32)", validation = SdaValidationUtils.validateHexString(issuerPkExponent))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = ssad, onValueChange = { ssad = it.uppercase() }, label = "SSAD (tag 93)", validation = SdaValidationUtils.validateHexString(ssad), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = staticData, onValueChange = { staticData = it.uppercase() }, label = "Static Data to be Authenticated (AFL records ‖ AIP if listed)", validation = SdaValidationUtils.validateHexString(staticData, allowEmpty = true), maxLines = 4)
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Verify SSAD",
                onClick = {
                    val inputs = mapOf("Issuer PK Modulus" to issuerPkModulus, "SSAD" to ssad, "Static data" to staticData)
                    try {
                        val key = RsaPublicKey(BigInt.fromHex(issuerPkModulus), BigInt.fromHex(issuerPkExponent))
                        val r = EmvOda.verifySsad(key, ssad.hexToBytes(), if (staticData.isBlank()) ByteArray(0) else staticData.hexToBytes())
                        val text = "Recovered data: ${r.recovered.toHexUpper()}\nData Authentication Code (9F45): ${r.dac.toHexUpper()}\nSHA-1 hash: ${if (r.hashValid) "MATCH" else "MISMATCH"}" + (if (r.notes.isNotEmpty()) "\nNotes: " + r.notes.joinToString("; ") else "")
                        SdaLogManager.logOperation(SdaTabs.VERIFY_SSAD.title, "Verify SSAD", inputs, result = if (r.hashValid) text else null, error = if (r.hashValid) null else text)
                    } catch (e: Exception) {
                        SdaLogManager.logOperation(SdaTabs.VERIFY_SSAD.title, "Verify SSAD", inputs, error = e.message)
                    }
                },
                enabled = isFormValid, icon = Icons.Default.Security, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


// --- SHARED UI COMPONENTS (COPIED FOR SDA SCREEN) ---
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
            transitionSpec = { fadeIn() with fadeOut() }
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
                Column(modifier = Modifier.toolScroll(rememberScrollState())) {
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
