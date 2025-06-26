package `in`.aicortex.iso8583studio.ui.screens.payments

import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import androidx.compose.animation.*
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
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---

object PinPvvValidationUtils {
    fun validateHex(value: String, fieldName: String, expectedLength: Int? = null): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$fieldName cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
        }
        if (value.length % 2 != 0) {
            return ValidationResult(ValidationState.ERROR, "$fieldName must have an even number of characters.")
        }
        expectedLength?.let {
            if (value.length != it) return ValidationResult(ValidationState.ERROR, "$fieldName must be $it characters long.")
        }
        return ValidationResult(ValidationState.VALID)
    }

    fun validateNumeric(value: String, fieldName: String, minLen: Int? = null, maxLen: Int? = null): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$fieldName cannot be empty.")
        if (value.any { !it.isDigit() }) return ValidationResult(ValidationState.ERROR, "$fieldName must be numeric.")
        minLen?.let { if (value.length < it) return ValidationResult(ValidationState.ERROR, "$fieldName must be at least $it digits.") }
        maxLen?.let { if (value.length > it) return ValidationResult(ValidationState.ERROR, "$fieldName must be at most $it digits.") }
        return ValidationResult(ValidationState.VALID)
    }

    fun validatePan(pan: String) = validateNumeric(pan, "PAN", 12)
    fun validatePin(pin: String) = validateNumeric(pin, "PIN", 4, 12)
    fun validatePvv(pvv: String) = validateNumeric(pvv, "PVV", 4, 4)
    fun validatePdk(pdk: String) = validateHex(pdk, "PDK", 32)
}


// --- PIN PVV SCREEN ---

object PinPvvLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() { _logEntries.clear() }

    private fun addLog(entry: LogEntry) {
        _logEntries.add(0, entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, _logEntries.size)
    }

    fun logOperation(operation: String, inputs: Map<String, String>, result: String? = null, error: String? = null, executionTime: Long = 0L) {
        if (result == null && error == null) return
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) -> if (value.isNotBlank()) append("  $key: $value\n") }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }
        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

object PinPvvService {
    // NOTE: This is a placeholder for actual PVV cryptographic logic.
    fun generatePvv(pdk: String, pan: String, pin: String, pvki: Int): String {
        // Mock logic: combine inputs and hash. Real logic is more complex.
        val combined = "$pdk|$pan|$pin|$pvki"
        val hash = combined.hashCode().toString()
        return hash.filter { it.isDigit() }.take(4).padStart(4, '0')
    }

    fun validatePvv(pdk: String, pan: String, pin: String, pvki: Int, pvvToCompare: String): Boolean {
        val generatedPvv = generatePvv(pdk, pan, pin, pvki)
        return generatedPvv == pvvToCompare
    }
}

enum class PinPvvTabs(val title: String, val icon: ImageVector) {
    ENCODE("Encode", Icons.Default.Lock),
    DECODE("Decode", Icons.Default.LockOpen),
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PinPvvScreen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = PinPvvTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "PIN PVV Calculator", onBackClick = onBack) },
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
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn()) with
                                    (slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "pin_pvv_tab_transition"
                    ) { tab ->
                        when (tab) {
                            PinPvvTabs.ENCODE -> EncodeCard()
                            PinPvvTabs.DECODE -> DecodeCard()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { PinPvvLogManager.clearLogs() },
                            logEntries = PinPvvLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EncodeCard() {
    var pdk by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    val pvkiOptions = remember { (0..9).map { it.toString() } }
    var selectedPvki by remember { mutableStateOf("1") }
    var isLoading by remember { mutableStateOf(false) }

    val pdkValidation = PinPvvValidationUtils.validatePdk(pdk)
    val panValidation = PinPvvValidationUtils.validatePan(pan)
    val pinValidation = PinPvvValidationUtils.validatePin(pin)

    val isFormValid = pdkValidation.isValid() && panValidation.isValid() && pinValidation.isValid()

    ModernCryptoCard(title = "Encode PVV", subtitle = "Generate PIN Verification Value", icon = Icons.Default.Password) {
        EnhancedTextField(pdk, { pdk = it }, "PDK (32 Hex Chars)", validation = pdkValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(pan, { pan = it }, "PAN", validation = panValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(pin, { pin = it }, "PIN", validation = pinValidation)
        Spacer(Modifier.height(12.dp))
        ModernDropdownField("PVKI", selectedPvki, pvkiOptions) { selectedPvki = pvkiOptions[it] }
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Generate PVV",
            onClick = {
                if(isFormValid) {
                    isLoading = true
                    val inputs = mapOf("PDK" to pdk, "PAN" to pan, "PIN" to pin, "PVKI" to selectedPvki)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = PinPvvService.generatePvv(pdk, pan, pin, selectedPvki.toInt())
                            PinPvvLogManager.logOperation("Generate PVV", inputs, result = "Generated PVV: $result", executionTime = 155)
                        } catch (e: Exception) {
                            PinPvvLogManager.logOperation("Generate PVV", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.ArrowForward, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DecodeCard() {
    var pdk by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var pvv by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") } // Added PIN for validation
    val pvkiOptions = remember { (0..9).map { it.toString() } }
    var selectedPvki by remember { mutableStateOf("1") }
    var isLoading by remember { mutableStateOf(false) }

    val pdkValidation = PinPvvValidationUtils.validatePdk(pdk)
    val panValidation = PinPvvValidationUtils.validatePan(pan)
    val pvvValidation = PinPvvValidationUtils.validatePvv(pvv)
    val pinValidation = PinPvvValidationUtils.validatePin(pin)

    val isFormValid = pdkValidation.isValid() && panValidation.isValid() && pvvValidation.isValid() && pinValidation.isValid()

    ModernCryptoCard(title = "Decode (Validate) PVV", subtitle = "Validate a PIN against a PVV", icon = Icons.Default.VerifiedUser) {
        EnhancedTextField(pdk, { pdk = it }, "PDK (32 Hex Chars)", validation = pdkValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(pan, { pan = it }, "PAN", validation = panValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(pvv, { pvv = it }, "PVV (4 digits)", validation = pvvValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(pin, { pin = it }, "PIN to Validate", validation = pinValidation)
        Spacer(Modifier.height(12.dp))
        ModernDropdownField("PVKI", selectedPvki, pvkiOptions) { selectedPvki = pvkiOptions[it] }
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Validate PVV",
            onClick = {
                if(isFormValid) {
                    isLoading = true
                    val inputs = mapOf("PDK" to pdk, "PAN" to pan, "PVV to Check" to pvv, "PIN" to pin, "PVKI" to selectedPvki)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val isValid = PinPvvService.validatePvv(pdk, pan, pin, selectedPvki.toInt(), pvv)
                            val resultMessage = if (isValid) "Validation Success: PVV is VALID" else "Validation Failed: PVV is INVALID"
                            PinPvvLogManager.logOperation("Validate PVV", inputs, result = resultMessage, executionTime = 155)
                        } catch(e: Exception) {
                            PinPvvLogManager.logOperation("Validate PVV", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.CheckCircle, modifier = Modifier.fillMaxWidth()
        )
    }
}


// --- SHARED UI COMPONENTS ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, validation: ValidationResult) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
            isError = validation.state == ValidationState.ERROR,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = when (validation.state) { ValidationState.VALID -> MaterialTheme.colors.primary; ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f); else -> MaterialTheme.colors.error },
                unfocusedBorderColor = when (validation.state) { ValidationState.VALID -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f); ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f); else -> MaterialTheme.colors.error }
            )
        )
        if (validation.message.isNotEmpty()) {
            Text(validation.message, color = if (validation.state == ValidationState.ERROR) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.caption, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

@Composable
private fun ModernCryptoCard(title: String, subtitle: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ModernDropdownField(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), readOnly = true,
            trailingIcon = { Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expanded = !expanded }) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option -> DropdownMenuItem(onClick = { onSelectionChanged(index); expanded = false }) { Text(text = option) } }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ModernButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled && !isLoading, elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)) {
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }) { loading ->
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon?.let { Icon(it, null); Spacer(Modifier.width(8.dp)) }
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
