package `in`.aicortex.iso8583studio.ui.screens.Emv.dsPartialKey

import ai.cortex.core.crypto.data.FieldValidation
import ai.cortex.core.crypto.data.ValidationState
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

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---



object DsValidationUtils {
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

// --- DATA STORAGE PARTIAL KEY SCREEN ---

enum class DsPartialKeyTabs(val title: String, val icon: ImageVector) {
    DSPK("DSPK", Icons.Default.VpnKey),
    DS_SUMMARY_OWHF1("DS Summary OWHF1", Icons.Default.Summarize),
    DS_DIGEST_OWHF2("DS Digest OWHF2", Icons.Default.DonutLarge)
}

object DsLogManager {
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
                message = "Log history cleared",
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
                val displayValue = if (key.contains("key", ignoreCase = true) || key.contains("dspk", ignoreCase = true)) {
                    "${value.take(16)}..."
                } else {
                    value
                }
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(tabTitle,  LogEntry(timestamp = timestamp, type= logType, message = message, details = details))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DsPartialKeyScreen(
    window: ComposeWindow? = null,
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = DsPartialKeyTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "Data Storage Partial Key - MasterCard",
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
                        label = "dspk_tab_transition"
                    ) { tab ->
                        when (tab) {
                            DsPartialKeyTabs.DSPK -> DspkTab()
                            DsPartialKeyTabs.DS_SUMMARY_OWHF1 -> DsSummaryOwhf1Tab()
                            DsPartialKeyTabs.DS_DIGEST_OWHF2 -> DsDigestOwhf2Tab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { DsLogManager.clearLogs(selectedTab.title) },
                            logEntries = DsLogManager.getLogEntries(selectedTab.title)
                        )
                    }
                }
            }
        }
    }
}

// --- TABS for Data Storage Partial Key ---

@Composable
private fun DspkTab() {
    var dsId by remember { mutableStateOf("1234567890ABCDEF") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = DsValidationUtils.validateHexString(dsId).state == ValidationState.VALID

    ModernCryptoCard(title = "Data Storage Partial Key (DSPK)", subtitle = "Generate the DSPK from a Data Storage ID", icon = Icons.Default.VpnKey) {
        EnhancedTextField(
            value = dsId,
            onValueChange = { dsId = it.uppercase() },
            label = "DS ID (Data Storage ID)",
            validation = DsValidationUtils.validateHexString(dsId)
        )
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate DSPK",
            onClick = {
                isLoading = true
                val inputs = mapOf("DS ID" to dsId)
                GlobalScope.launch {
                    delay(500)
                    val result = "DSPK: FEDCBA9876543210FEDCBA9876543210"
                    DsLogManager.logOperation(DsPartialKeyTabs.DSPK.title, "DSPK Generation", inputs, result = result, executionTime = 510)
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DsSummaryOwhf1Tab() {
    var dsSummary by remember { mutableStateOf("01") }
    var amountAuth by remember { mutableStateOf("000000012345") }
    var countryCode by remember { mutableStateOf("0840") }
    var refControlParam by remember { mutableStateOf("0000") }
    var genAcIndicator by remember { mutableStateOf("00") }
    var dsUnpredictableNr by remember { mutableStateOf("00000001") }
    var unpredictableNr by remember { mutableStateOf("1A2B3C4D") }
    var dspk by remember { mutableStateOf("FEDCBA9876543210FEDCBA9876543210") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = listOf(dsSummary, amountAuth, countryCode, refControlParam, genAcIndicator, dsUnpredictableNr, unpredictableNr, dspk).all { it.isNotBlank() }

    ModernCryptoCard(title = "DS Summary OWHF1", subtitle = "Generate the first one-way hash function", icon = Icons.Default.Summarize) {
        EnhancedTextField(value = dsSummary, onValueChange = { dsSummary = it.uppercase() }, label = "DS Summary", validation = DsValidationUtils.validateHexString(dsSummary, 2))
        EnhancedTextField(value = amountAuth, onValueChange = { amountAuth = it }, label = "Amount Authorized", validation = DsValidationUtils.validateHexString(amountAuth, 12))
        EnhancedTextField(value = countryCode, onValueChange = { countryCode = it.uppercase() }, label = "Transaction Country Code", validation = DsValidationUtils.validateHexString(countryCode, 4))
        EnhancedTextField(value = refControlParam, onValueChange = { refControlParam = it.uppercase() }, label = "Reference Control Parameter", validation = DsValidationUtils.validateHexString(refControlParam, 4))
        EnhancedTextField(value = genAcIndicator, onValueChange = { genAcIndicator = it.uppercase() }, label = "First/Second Generate AC Indicator", validation = DsValidationUtils.validateHexString(genAcIndicator, 2))
        EnhancedTextField(value = dsUnpredictableNr, onValueChange = { dsUnpredictableNr = it.uppercase() }, label = "DS Unpredictable Number", validation = DsValidationUtils.validateHexString(dsUnpredictableNr, 8))
        EnhancedTextField(value = unpredictableNr, onValueChange = { unpredictableNr = it.uppercase() }, label = "Unpredictable Number", validation = DsValidationUtils.validateHexString(unpredictableNr, 8))
        EnhancedTextField(value = dspk, onValueChange = { dspk = it.uppercase() }, label = "DSPK", validation = DsValidationUtils.validateHexString(dspk, 32))
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate DS Summary Hash",
            onClick = {
                isLoading = true
                val inputs = mapOf("DS Summary" to dsSummary, "Amount" to amountAuth, "Country" to countryCode, "DSPK" to dspk)
                GlobalScope.launch {
                    delay(500)
                    val result = "OWHF1 Result: 1122334455667788"
                    DsLogManager.logOperation(DsPartialKeyTabs.DS_SUMMARY_OWHF1.title, "DS Summary OWHF1", inputs, result = result, executionTime = 525)
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.SyncLock, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DsDigestOwhf2Tab() {
    var dsInput by remember { mutableStateOf("0102030405060708") }
    var dsOperatorId by remember { mutableStateOf("AABBCCDDEEFF1122") }
    var dspk by remember { mutableStateOf("FEDCBA9876543210FEDCBA9876543210") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = listOf(dsInput, dsOperatorId, dspk).all { it.isNotBlank() }

    ModernCryptoCard(title = "DS Digest OWHF2", subtitle = "Generate the second one-way hash function", icon = Icons.Default.DonutLarge) {
        EnhancedTextField(value = dsInput, onValueChange = { dsInput = it.uppercase() }, label = "DS Input", validation = DsValidationUtils.validateHexString(dsInput))
        EnhancedTextField(value = dsOperatorId, onValueChange = { dsOperatorId = it.uppercase() }, label = "DS Requested Operator ID", validation = DsValidationUtils.validateHexString(dsOperatorId))
        EnhancedTextField(value = dspk, onValueChange = { dspk = it.uppercase() }, label = "DSPK", validation = DsValidationUtils.validateHexString(dspk, 32))
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate DS Digest Hash",
            onClick = {
                isLoading = true
                val inputs = mapOf("DS Input" to dsInput, "Operator ID" to dsOperatorId, "DSPK" to dspk)
                GlobalScope.launch {
                    delay(500)
                    val result = "OWHF2 Result: 8877665544332211"
                    DsLogManager.logOperation(DsPartialKeyTabs.DS_DIGEST_OWHF2.title, "DS Digest OWHF2", inputs, result = result, executionTime = 535)
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.SyncLock, modifier = Modifier.fillMaxWidth()
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ModernButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled && !isLoading, elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)) {
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "DsPkButtonAnimation") { loading ->
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
