package `in`.aicortex.iso8583studio.ui.screens.payments.cardValidation

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random


private object AmexCvvValidationUtils {
    fun validateHex(value: String, friendlyName: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$friendlyName cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        if (value.length % 2 != 0) {
            return ValidationResult(ValidationState.ERROR, "Hex string must have an even number of characters.")
        }
        return ValidationResult(ValidationState.VALID)
    }
    fun validateNumeric(value: String, friendlyName: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$friendlyName cannot be empty.")
        if (value.any { !it.isDigit() }) {
            return ValidationResult(ValidationState.ERROR, "Only digits (0-9) are allowed.")
        }
        return ValidationResult(ValidationState.VALID)
    }
}

// --- AMEX CVV SCREEN ---

private enum class AmexCvvTabs(val title: String, val icon: ImageVector) {
    GENERATE("Generate", Icons.Default.Add),
    VALIDATE("Validate", Icons.Default.Check)
}

private object AmexCvvLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
        addLog(LogEntry(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")), LogType.INFO, "Log history cleared", ""))
    }

    private fun addLog(entry: LogEntry) {
        _logEntries.add(0, entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, _logEntries.size)
    }

    fun logOperation(operation: String, inputs: Map<String, String>, result: String? = null, error: String? = null, executionTime: Long = 0L) {
        if (result == null && error == null) return

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                val displayValue = if (key.contains("Key", ignoreCase = true)) "${value.take(16)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp, logType, message, details))
    }
}

private object AmexCvvCryptoService {
    // This is a mock service.
    fun generateCsc(): Map<String, String> {
        return mapOf(
            "csc3" to Random.nextInt(100, 999).toString(),
            "csc4" to Random.nextInt(1000, 9999).toString(),
            "csc5" to Random.nextInt(10000, 99999).toString()
        )
    }

    fun validateCsc(csc3: String, csc4: String, csc5: String): Boolean {
        // Mock validation logic
        return csc3.length == 3 || csc4.length == 4 || csc5.length == 5
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AmexCvvScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = AmexCvvTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "Amex CSC Calculator", onBackClick = onBack) },
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
                        }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn()) with
                                    (slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "amex_cvv_tab_transition"
                    ) { tab ->
                        when (tab) {
                            AmexCvvTabs.GENERATE -> CvvForm(isValidation = false)
                            AmexCvvTabs.VALIDATE -> CvvForm(isValidation = true)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { AmexCvvLogManager.clearLogs() },
                            logEntries = AmexCvvLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// --- Reusable Form for Generate & Validate ---
@Composable
private fun CvvForm(isValidation: Boolean) {
    val verificationTypes = remember { listOf("CSC", "Magstripe only", "Contact/Contactless chip card", "Contact chip iCSC", "Contactless chip iCSC") }

    var cscVersion by remember { mutableStateOf("Version 1") }
    var cscKey by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var expDate by remember { mutableStateOf("") }
    var serviceCode by remember { mutableStateOf("") }
    var selectedVerificationType by remember { mutableStateOf(verificationTypes.first()) }

    // Validation-specific fields
    var csc5 by remember { mutableStateOf("") }
    var csc4 by remember { mutableStateOf("") }
    var csc3 by remember { mutableStateOf("") }

    val isFormValid = listOf(cscKey, pan, expDate, serviceCode).all { it.isNotBlank() } &&
            if(isValidation) (csc3.isNotBlank() || csc4.isNotBlank() || csc5.isNotBlank()) else true

    ModernCryptoCard(
        title = if (isValidation) "Validate CSC" else "Generate CSC",
        subtitle = "Generate or validate an Amex Card Security Code",
        icon = if (isValidation) Icons.Default.VerifiedUser else Icons.Default.CreditCard
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernDropdownField("CSC Version", cscVersion, listOf("Version 1", "Version 2"), { cscVersion = if(it==0) "Version 1" else "Version 2" })
            EnhancedTextField(value = cscKey, onValueChange = { cscKey = it.uppercase() }, label = "CSC Key (Hex)", validation = AmexCvvValidationUtils.validateHex(cscKey, "CSC Key"))
            EnhancedTextField(value = pan, onValueChange = { pan = it }, label = "PAN", validation = AmexCvvValidationUtils.validateNumeric(pan, "PAN"))
            EnhancedTextField(value = expDate, onValueChange = { expDate = it }, label = "Expiration Date (YYMM)", validation = AmexCvvValidationUtils.validateNumeric(expDate, "Expiry Date"))
            EnhancedTextField(value = serviceCode, onValueChange = { serviceCode = it }, label = "Service Code", validation = AmexCvvValidationUtils.validateNumeric(serviceCode, "Service Code"))
            ModernDropdownField("Verification Value Type", selectedVerificationType, verificationTypes, { selectedVerificationType = verificationTypes[it] })

            if (isValidation) {
                Divider(Modifier.padding(vertical = 8.dp))
                Text("Values to Validate", style = MaterialTheme.typography.subtitle2)
                EnhancedTextField(value = csc5, onValueChange = { csc5 = it }, label = "CSC-5", validation = AmexCvvValidationUtils.validateNumeric(csc5, "CSC-5"))
                EnhancedTextField(value = csc4, onValueChange = { csc4 = it }, label = "CSC-4", validation = AmexCvvValidationUtils.validateNumeric(csc4, "CSC-4"))
                EnhancedTextField(value = csc3, onValueChange = { csc3 = it }, label = "CSC-3", validation = AmexCvvValidationUtils.validateNumeric(csc3, "CSC-3"))
            }

            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = if (isValidation) "Validate" else "Generate",
                onClick = {
                    val inputs = mapOf(
                        "CSC Version" to cscVersion, "CSC Key" to cscKey, "PAN" to pan, "Expiry" to expDate, "Service Code" to serviceCode, "Type" to selectedVerificationType
                    )
                    if (isValidation) {
                        val isValid = AmexCvvCryptoService.validateCsc(csc3, csc4, csc5)
                        val result = if (isValid) "Validation SUCCESSFUL" else "Validation FAILED"
                        AmexCvvLogManager.logOperation("Validate CSC", inputs + mapOf("CSC-3" to csc3, "CSC-4" to csc4, "CSC-5" to csc5), result)
                    } else {
                        val results = AmexCvvCryptoService.generateCsc()
                        val resultString = "Generated CSC-3: ${results["csc3"]}\nGenerated CSC-4: ${results["csc4"]}\nGenerated CSC-5: ${results["csc5"]}"
                        AmexCvvLogManager.logOperation("Generate CSC", inputs, resultString)
                    }
                },
                enabled = isFormValid,
                icon = if (isValidation) Icons.Default.Check else Icons.Default.GeneratingTokens,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult? = null) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), maxLines = maxLines,
            isError = validation?.state == ValidationState.ERROR,
        )
        if (validation?.message?.isNotEmpty() == true) {
            Text(
                text = validation.message,
                color = if (validation.state == ValidationState.ERROR) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun ModernCryptoCard(title: String, subtitle: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "AmexCvvButtonAnimation") { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
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

private fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2
    fillMaxWidth().wrapContentSize(Alignment.BottomStart).offset(x = indicatorOffset).width(indicatorWidth)
}
