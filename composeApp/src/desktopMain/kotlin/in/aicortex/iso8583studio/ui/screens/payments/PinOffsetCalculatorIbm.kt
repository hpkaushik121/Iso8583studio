package `in`.aicortex.iso8583studio.ui.screens.payments

import `in`.aicortex.iso8583studio.data.model.FieldValidation
import `in`.aicortex.iso8583studio.data.model.ValidationState
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

object PinOffsetValidationUtils {
    fun validateHex(value: String, fieldName: String, expectedLength: Int? = null): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$fieldName cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
        }
        if (value.length % 2 != 0 && fieldName != "DECTab") { // DECTab can be odd
            return FieldValidation(ValidationState.ERROR, "$fieldName must have an even number of characters.")
        }
        expectedLength?.let {
            if (value.length != it) return FieldValidation(ValidationState.ERROR, "$fieldName must be $it characters long.")
        }
        return FieldValidation(ValidationState.VALID)
    }

    fun validateNumeric(value: String, fieldName: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$fieldName cannot be empty.")
        if (value.any { !it.isDigit() }) return FieldValidation(ValidationState.ERROR, "$fieldName must be numeric.")
        return FieldValidation(ValidationState.VALID)
    }

    fun validatePan(pan: String): FieldValidation {
        if (pan.length < 12) return FieldValidation(ValidationState.ERROR, "PAN must be at least 12 digits.")
        return validateNumeric(pan, "PAN")
    }
}


// --- PIN OFFSET SCREEN ---

object PinOffsetLogManager {
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

object PinOffsetService {
    // NOTE: This is a placeholder for actual IBM 3624 logic.
    private fun process(pdk: String, pan: String, pinOrOffset: String): String {
        // Mock logic: combine inputs and hash. Real logic is more complex.
        val combined = "$pdk|$pan|$pinOrOffset"
        val hash = combined.hashCode().toString()
        return hash.filter { it.isDigit() }.take(pinOrOffset.length)
    }

    fun generateOffset(pdk: String, pan: String, pin: String): String {
        return process(pdk, pan, pin)
    }

    fun recoverPin(pdk: String, pan: String, offset: String): String {
        return process(pdk, pan, offset)
    }
}

enum class PinOffsetTabs(val title: String, val icon: ImageVector) {
    OFFSET("Offset", Icons.Default.Password),
    PIN("PIN", Icons.Default.Pin),
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PinOffsetScreen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = PinOffsetTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "PIN Offset (IBM 3624)", onBackClick = onBack) },
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
                        },
                        selectedContentColor = MaterialTheme.colors.primary,
                        unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
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
                        label = "pinoffset_tab_transition"
                    ) { tab ->
                        when (tab) {
                            PinOffsetTabs.OFFSET -> OffsetCard()
                            PinOffsetTabs.PIN -> PinCard()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { PinOffsetLogManager.clearLogs() },
                            logEntries = PinOffsetLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedPinOffsetFields(
    pdk: String, onPdkChange: (String) -> Unit, pdkValidation: FieldValidation,
    pan: String, onPanChange: (String) -> Unit, panValidation: FieldValidation,
    decTab: String, onDecTabChange: (String) -> Unit, decTabValidation: FieldValidation,
    useValidationParams: Boolean, onUseValidationParamsChange: (Boolean) -> Unit,
    useValidationMask: Boolean, onUseValidationMaskChange: (Boolean) -> Unit,
    start: String, onStartChange: (String) -> Unit, startValidation: FieldValidation,
    length: String, onLengthChange: (String) -> Unit, lengthValidation: FieldValidation,
    pad: String, onPadChange: (String) -> Unit, padValidation: FieldValidation,
    pinLength: String, onPinLengthChange: (String) -> Unit, pinLengthValidation: FieldValidation,
    validationMask: String, onValidationMaskChange: (String) -> Unit, maskValidation: FieldValidation
) {
    EnhancedTextField(pdk, onPdkChange, "PDK (32 Hex Chars)", validation = pdkValidation)
    Spacer(Modifier.height(12.dp))
    EnhancedTextField(pan, onPanChange, "PAN", validation = panValidation)
    Spacer(Modifier.height(12.dp))
    EnhancedTextField(decTab, onDecTabChange, "DECTab", validation = decTabValidation)
    Spacer(Modifier.height(12.dp))

    Text("Validation Data", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onUseValidationParamsChange(!useValidationParams) }) {
        Checkbox(checked = useValidationParams, onCheckedChange = onUseValidationParamsChange)
        Text("Use validation data parameters")
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onUseValidationMaskChange(!useValidationMask) }) {
        Checkbox(checked = useValidationMask, onCheckedChange = onUseValidationMaskChange)
        Text("Use validation data mask")
    }
    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EnhancedTextField(start, onStartChange, "Start", validation = startValidation, modifier = Modifier.weight(1f))
        EnhancedTextField(length, onLengthChange, "Length", validation = lengthValidation, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    EnhancedTextField(pad, onPadChange, "Pad", validation = padValidation)
    Spacer(Modifier.height(12.dp))
    EnhancedTextField(pinLength, onPinLengthChange, "PIN Length", validation = pinLengthValidation)
    Spacer(Modifier.height(12.dp))

    AnimatedVisibility(visible = useValidationMask) {
        EnhancedTextField(validationMask, onValidationMaskChange, "Validation Data Mask", validation = maskValidation)
    }
}

@Composable
private fun OffsetCard() {
    var state by remember { mutableStateOf(PinOffsetState()) }
    var isLoading by remember { mutableStateOf(false) }

    val validations = PinOffsetState.getValidations(state, isForOffset = true)
    val isFormValid = validations.all { it.value.isValid() }

    ModernCryptoCard(title = "Generate PIN Offset", subtitle = "Calculate offset from a known PIN", icon = PinOffsetTabs.OFFSET.icon) {
        EnhancedTextField(state.pin, { state = state.copy(pin = it) }, "PIN", validation = validations["pin"]!!)
        Spacer(Modifier.height(12.dp))

        SharedPinOffsetFields(
            pdk = state.pdk, onPdkChange = { state = state.copy(pdk = it) }, pdkValidation = validations["pdk"]!!,
            pan = state.pan, onPanChange = { state = state.copy(pan = it) }, panValidation = validations["pan"]!!,
            decTab = state.decTab, onDecTabChange = { state = state.copy(decTab = it) }, decTabValidation = validations["decTab"]!!,
            useValidationParams = state.useValidationParams, onUseValidationParamsChange = { state = state.copy(useValidationParams = it) },
            useValidationMask = state.useValidationMask, onUseValidationMaskChange = { state = state.copy(useValidationMask = it) },
            start = state.start, onStartChange = { state = state.copy(start = it) }, startValidation = validations["start"]!!,
            length = state.length, onLengthChange = { state = state.copy(length = it) }, lengthValidation = validations["length"]!!,
            pad = state.pad, onPadChange = { state = state.copy(pad = it) }, padValidation = validations["pad"]!!,
            pinLength = state.pinLength, onPinLengthChange = { state = state.copy(pinLength = it) }, pinLengthValidation = validations["pinLength"]!!,
            validationMask = state.validationMask, onValidationMaskChange = { state = state.copy(validationMask = it) }, maskValidation = validations["validationMask"]!!
        )
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate Offset",
            onClick = {
                if(isFormValid) {
                    isLoading = true
                    GlobalScope.launch {
                        delay(150)
                        val result = PinOffsetService.generateOffset(state.pdk, state.pan, state.pin)
                        PinOffsetLogManager.logOperation("Generate Offset", state.asMap(isForOffset = true), result = "PIN Offset: $result", executionTime = 155)
                        isLoading = false
                    }
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.ArrowForward, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PinCard() {
    var state by remember { mutableStateOf(PinOffsetState()) }
    var isLoading by remember { mutableStateOf(false) }

    val validations = PinOffsetState.getValidations(state, isForOffset = false)
    val isFormValid = validations.all { it.value.isValid() }

    ModernCryptoCard(title = "Recover PIN", subtitle = "Calculate PIN from a known offset", icon = PinOffsetTabs.PIN.icon) {
        EnhancedTextField(state.pinOffset, { state = state.copy(pinOffset = it) }, "PIN Offset", validation = validations["pinOffset"]!!)
        Spacer(Modifier.height(12.dp))

        SharedPinOffsetFields(
            pdk = state.pdk, onPdkChange = { state = state.copy(pdk = it) }, pdkValidation = validations["pdk"]!!,
            pan = state.pan, onPanChange = { state = state.copy(pan = it) }, panValidation = validations["pan"]!!,
            decTab = state.decTab, onDecTabChange = { state = state.copy(decTab = it) }, decTabValidation = validations["decTab"]!!,
            useValidationParams = state.useValidationParams, onUseValidationParamsChange = { state = state.copy(useValidationParams = it) },
            useValidationMask = state.useValidationMask, onUseValidationMaskChange = { state = state.copy(useValidationMask = it) },
            start = state.start, onStartChange = { state = state.copy(start = it) }, startValidation = validations["start"]!!,
            length = state.length, onLengthChange = { state = state.copy(length = it) }, lengthValidation = validations["length"]!!,
            pad = state.pad, onPadChange = { state = state.copy(pad = it) }, padValidation = validations["pad"]!!,
            pinLength = state.pinLength, onPinLengthChange = { state = state.copy(pinLength = it) }, pinLengthValidation = validations["pinLength"]!!,
            validationMask = state.validationMask, onValidationMaskChange = { state = state.copy(validationMask = it) }, maskValidation = validations["validationMask"]!!
        )
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Recover PIN",
            onClick = {
                if(isFormValid) {
                    isLoading = true
                    GlobalScope.launch {
                        delay(150)
                        val result = PinOffsetService.recoverPin(state.pdk, state.pan, state.pinOffset)
                        PinOffsetLogManager.logOperation("Recover PIN", state.asMap(isForOffset = false), result = "Recovered PIN: $result", executionTime = 155)
                        isLoading = false
                    }
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.ArrowBack, modifier = Modifier.fillMaxWidth()
        )
    }
}


data class PinOffsetState(
    val pdk: String = "",
    val pan: String = "",
    val pin: String = "",
    val pinOffset: String = "",
    val decTab: String = "",
    val useValidationParams: Boolean = true,
    val useValidationMask: Boolean = false,
    val start: String = "1",
    val length: String = "11",
    val pad: String = "0",
    val pinLength: String = "4",
    val validationMask: String = ""
) {
    fun asMap(isForOffset: Boolean): Map<String, String> {
        return mapOf(
            "PDK" to pdk, "PAN" to pan,
            (if(isForOffset) "PIN" else "PIN Offset") to (if(isForOffset) pin else pinOffset),
            "DECTab" to decTab, "Use Validation Params" to useValidationParams.toString(),
            "Use Validation Mask" to useValidationMask.toString(), "Start" to start,
            "Length" to length, "Pad" to pad, "PIN Length" to pinLength,
            "Validation Data Mask" to validationMask
        )
    }

    companion object {
        fun getValidations(state: PinOffsetState, isForOffset: Boolean): Map<String, FieldValidation> {
            return mapOf(
                "pdk" to PinOffsetValidationUtils.validateHex(state.pdk, "PDK", 32),
                "pan" to PinOffsetValidationUtils.validatePan(state.pan),
                "pin" to if (isForOffset) PinOffsetValidationUtils.validateNumeric(state.pin, "PIN") else FieldValidation(ValidationState.VALID),
                "pinOffset" to if (!isForOffset) PinOffsetValidationUtils.validateNumeric(state.pinOffset, "PIN Offset") else FieldValidation(ValidationState.VALID),
                "decTab" to PinOffsetValidationUtils.validateHex(state.decTab, "DECTab"),
                "start" to PinOffsetValidationUtils.validateNumeric(state.start, "Start"),
                "length" to PinOffsetValidationUtils.validateNumeric(state.length, "Length"),
                "pad" to PinOffsetValidationUtils.validateHex(state.pad, "Pad", 1),
                "pinLength" to PinOffsetValidationUtils.validateNumeric(state.pinLength, "PIN Length"),
                "validationMask" to if (state.useValidationMask) PinOffsetValidationUtils.validateHex(state.validationMask, "Validation Mask") else FieldValidation(ValidationState.VALID)
            )
        }
    }
}

// --- SHARED UI COMPONENTS ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, validation: FieldValidation) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
            isError = validation.state == ValidationState.ERROR,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.primary
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colors.error
                },
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
private fun ModernButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled && !isLoading, elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                icon?.let { Icon(it, null); Spacer(Modifier.width(8.dp)) }
                Text(text, fontWeight = FontWeight.Medium)
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
