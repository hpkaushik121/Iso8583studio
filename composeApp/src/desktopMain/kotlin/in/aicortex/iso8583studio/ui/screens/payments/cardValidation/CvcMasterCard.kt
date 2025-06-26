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


object CVC3ValidationUtils {
    fun validate(value: String, fieldName: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$fieldName cannot be empty.")

        // Basic hex validation for relevant fields
        if (fieldName in listOf("IMK", "Track 1/2 Data", "Unpredictable Num", "Dynamic CVC3")) {
            if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
                return ValidationResult(ValidationState.ERROR, "$fieldName must be valid hexadecimal characters.")
            }
        }

        // Basic numeric validation for other fields
        if (fieldName in listOf("PAN", "PAN Seq No", "ATC")) {
            if (value.any { !it.isDigit() }) {
                return ValidationResult(ValidationState.ERROR, "$fieldName must contain only digits.")
            }
        }
        return ValidationResult(ValidationState.VALID)
    }
}


// --- CVC3 SCREEN ---

object CVC3LogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
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
                val displayValue = if (value.length > 200) "${value.take(200)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

object CVC3Service {
    // NOTE: This is a placeholder for the actual CVC3 cryptographic logic.
    fun generate(imk: String, pan: String, panSeqNo: String, trackData: String, unpredictableNum: String, atc: String, cvc3Type: String): String {
        // Mock logic: Combine inputs and hash them (not cryptographically secure)
        val combined = "$imk|$pan|$panSeqNo|$trackData|$unpredictableNum|$atc|$cvc3Type"
        return combined.hashCode().toString(16).take(8).uppercase()
    }

    // NOTE: This is a placeholder for the actual CVC3 validation logic.
    fun validate(imk: String, pan: String, panSeqNo: String, trackData: String, unpredictableNum: String, atc: String, cvc3Type: String, dynamicCVC3: String): Boolean {
        // Mock validation: Re-generate and compare
        val generatedCvc3 = generate(imk, pan, panSeqNo, trackData, unpredictableNum, atc, cvc3Type)
        return generatedCvc3 == dynamicCVC3.uppercase()
    }
}

enum class CVC3Tabs(val title: String, val icon: ImageVector) {
    GENERATE("Generate", Icons.Default.AddCard),
    VALIDATE("Validate", Icons.Default.DomainVerification),
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CVC3Screen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = CVC3Tabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "MasterCard CVC3", onBackClick = onBack) },
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
                        label = "cvc3_tab_transition"
                    ) { tab ->
                        when (tab) {
                            CVC3Tabs.GENERATE -> GenerateCard()
                            CVC3Tabs.VALIDATE -> ValidateCard()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { CVC3LogManager.clearLogs() },
                            logEntries = CVC3LogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerateCard() {
    var imk by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var panSeqNo by remember { mutableStateOf("") }
    var trackData by remember { mutableStateOf("") }
    var unpredictableNum by remember { mutableStateOf("") }
    var atc by remember { mutableStateOf("") }
    val cvc3Types = remember { listOf("Dynamic CVC3", "PIN-CVC3") }
    var selectedCvc3Type by remember { mutableStateOf(cvc3Types.first()) }
    var isLoading by remember { mutableStateOf(false) }

    val validations = mapOf(
        "IMK" to CVC3ValidationUtils.validate(imk, "IMK"),
        "PAN" to CVC3ValidationUtils.validate(pan, "PAN"),
        "PAN Seq No" to CVC3ValidationUtils.validate(panSeqNo, "PAN Seq No"),
        "Track 1/2 Data" to CVC3ValidationUtils.validate(trackData, "Track 1/2 Data"),
        "Unpredictable Num" to CVC3ValidationUtils.validate(unpredictableNum, "Unpredictable Num"),
        "ATC" to CVC3ValidationUtils.validate(atc, "ATC")
    )

    val isFormValid = validations.values.all { it.state != ValidationState.ERROR } &&
            listOf(imk, pan, panSeqNo, trackData, unpredictableNum, atc).all { it.isNotBlank() }

    ModernCryptoCard(title = "Generate CVC3", subtitle = "Generate a MasterCard CVC3 value", icon = Icons.Default.VpnKey) {
        val fields = listOf(
            "IMK" to Pair<String,(String) -> Unit>(imk, { imk = it }),
            "PAN" to Pair<String,(String) -> Unit>(pan, { pan = it }),
            "PAN Seq No" to Pair<String,(String) -> Unit>(panSeqNo, { panSeqNo = it }),
            "Track 1/2 Data" to Pair<String,(String) -> Unit>(trackData, { trackData = it }),
            "Unpredictable Num" to Pair<String,(String) -> Unit>(unpredictableNum, { unpredictableNum = it }),
            "ATC" to Pair<String,(String) -> Unit>(atc, { atc = it })
        )

        fields.forEach { (label, state) ->
            EnhancedTextField(
                value = state.first,
                onValueChange = state.second,
                label = label,
                validation = validations[label]!!
            )
            Spacer(Modifier.height(12.dp))
        }

        ModernDropdownField(
            label = "CVC3 Type",
            value = selectedCvc3Type,
            options = cvc3Types,
            onSelectionChanged = { index -> selectedCvc3Type = cvc3Types[index] }
        )
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate",
            onClick = {
                isLoading = true
                val inputs = mapOf(
                    "IMK" to imk, "PAN" to pan, "PAN Seq No" to panSeqNo,
                    "Track 1/2 Data" to trackData, "Unpredictable Number" to unpredictableNum,
                    "ATC" to atc, "CVC3 Type" to selectedCvc3Type
                )
                GlobalScope.launch {
                    delay(200) // Simulate cryptographic operation
                    try {
                        val result = CVC3Service.generate(imk, pan, panSeqNo, trackData, unpredictableNum, atc, selectedCvc3Type)
                        CVC3LogManager.logOperation("Generate", inputs, result = "Generated CVC3: $result", executionTime = 205)
                    } catch (e: Exception) {
                        CVC3LogManager.logOperation("Generate", inputs, error = e.message, executionTime = 205)
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.ArrowForward, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ValidateCard() {
    var imk by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var panSeqNo by remember { mutableStateOf("") }
    var trackData by remember { mutableStateOf("") }
    var unpredictableNum by remember { mutableStateOf("") }
    var atc by remember { mutableStateOf("") }
    var dynamicCvc3 by remember { mutableStateOf("") }
    val cvc3Types = remember { listOf("Dynamic CVC3", "PIN-CVC3") }
    var selectedCvc3Type by remember { mutableStateOf(cvc3Types.first()) }
    var isLoading by remember { mutableStateOf(false) }

    val validations = mapOf(
        "IMK" to CVC3ValidationUtils.validate(imk, "IMK"),
        "PAN" to CVC3ValidationUtils.validate(pan, "PAN"),
        "PAN Seq No" to CVC3ValidationUtils.validate(panSeqNo, "PAN Seq No"),
        "Track 1/2 Data" to CVC3ValidationUtils.validate(trackData, "Track 1/2 Data"),
        "Unpredictable Num" to CVC3ValidationUtils.validate(unpredictableNum, "Unpredictable Num"),
        "ATC" to CVC3ValidationUtils.validate(atc, "ATC"),
        "Dynamic CVC3" to CVC3ValidationUtils.validate(dynamicCvc3, "Dynamic CVC3")
    )

    val isFormValid = validations.values.all { it.state != ValidationState.ERROR } &&
            listOf(imk, pan, panSeqNo, trackData, unpredictableNum, atc, dynamicCvc3).all { it.isNotBlank() }

    ModernCryptoCard(title = "Validate CVC3", subtitle = "Validate a MasterCard CVC3 value", icon = Icons.Default.Security) {
        val fields = listOf(
            "IMK" to Pair<String,(String) -> Unit>(imk) { imk = it },
            "PAN" to Pair<String,(String) -> Unit>(pan, { pan = it }),
            "PAN Seq No" to Pair<String,(String) -> Unit>(panSeqNo, { panSeqNo = it }),
            "Track 1/2 Data" to Pair<String,(String) -> Unit>(trackData, { trackData = it }),
            "Unpredictable Num" to Pair<String,(String) -> Unit>(unpredictableNum, { unpredictableNum = it }),
            "ATC" to Pair<String,(String) -> Unit>(atc, { atc = it }),
            "Dynamic CVC3" to Pair<String,(String) -> Unit>(dynamicCvc3, { dynamicCvc3 = it })
        )

        fields.forEach { (label, state) ->
            EnhancedTextField(
                value = state.first,
                onValueChange = state.second,
                label = label,
                validation = validations[label]!!
            )
            Spacer(Modifier.height(12.dp))
        }

        ModernDropdownField(
            label = "CVC3 Type",
            value = selectedCvc3Type,
            options = cvc3Types,
            onSelectionChanged = { index -> selectedCvc3Type = cvc3Types[index] }
        )
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Validate",
            onClick = {
                isLoading = true
                val inputs = mapOf(
                    "IMK" to imk, "PAN" to pan, "PAN Seq No" to panSeqNo,
                    "Track 1/2 Data" to trackData, "Unpredictable Number" to unpredictableNum,
                    "ATC" to atc, "CVC3 Type" to selectedCvc3Type, "Dynamic CVC3" to dynamicCvc3
                )
                GlobalScope.launch {
                    delay(200) // Simulate cryptographic operation
                    try {
                        val isValid = CVC3Service.validate(imk, pan, panSeqNo, trackData, unpredictableNum, atc, selectedCvc3Type, dynamicCvc3)
                        val resultMessage = if (isValid) "CVC3 is VALID" else "CVC3 is INVALID"
                        CVC3LogManager.logOperation("Validate", inputs, result = resultMessage, executionTime = 205)
                    } catch (e: Exception) {
                        CVC3LogManager.logOperation("Validate", inputs, error = e.message, executionTime = 205)
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.ArrowBack, modifier = Modifier.fillMaxWidth()
        )
    }
}


// --- SHARED UI COMPONENTS (from original file, no changes needed) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, validation: ValidationResult) {
    Column{
        OutlinedTextField(
            value = value, onValueChange = onValueChange, label = {
                Text(label) }, modifier = Modifier.fillMaxWidth(), maxLines = 1,
            isError = validation.state == ValidationState.ERROR,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.primary
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colors.error
                },
                unfocusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colors.error
                }
            )
        )
        if (validation.message.isNotEmpty()) {
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
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "CVC3ButtonAnimation") { loading ->
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

private fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2
    fillMaxWidth().wrapContentSize(Alignment.BottomStart).offset(x = indicatorOffset).width(indicatorWidth)
}