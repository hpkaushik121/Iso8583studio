package `in`.aicortex.iso8583studio.ui.screens.generic

import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec

import `in`.aicortex.iso8583studio.ui.chrome.ToolLog

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import io.cryptocalc.codec.Codecs

import ai.cortex.core.toHexUpper

import ai.cortex.core.hexToBytes

import `in`.aicortex.iso8583studio.platform.nowLogTimestamp

import `in`.aicortex.iso8583studio.analytics.Analytics
import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.LogPanelWithAutoScroll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// --- COMMON UI & VALIDATION FOR THIS SCREEN ---


object CheckDigitValidationUtils {
    fun validateNumeric(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY)
        if (value.any { !it.isDigit() }) {
            return ValidationResult(ValidationState.ERROR, "Input must only contain digits (0-9).")
        }
        return ValidationResult(ValidationState.VALID)
    }
}

// --- CHECK DIGIT SCREEN ---

object CheckDigitLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
    }

    private fun addLog(entry: LogEntry) {
        _logEntries.add(entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, _logEntries.size)
    }

    fun logOperation(operation: String, inputs: Map<String, String>, result: String? = null, error: String? = null, executionTime: Long = 0L) {
        if (result == null && error == null) return
        Analytics.calculationRun(operation, success = result != null, durationMs = executionTime)

        val timestamp = nowLogTimestamp()
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) -> append("  $key: $value\n") }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

object CheckDigitService {
    val algorithms = listOf("Luhn (Mod 10)", "Verhoeff", "Damm", "Mod 11 (ISBN-10)", "Mod 97 (ISO 7064 MOD 97-10)", "IBAN", "Amex SE (Mod 9)")

    /** Check digit(s) for [base] (the number without its check digit(s)). */
    fun generate(algorithm: String, base: String): String = when (algorithm) {
        "Luhn (Mod 10)" -> Codecs.CheckDigits.luhn(base).toString()
        "Verhoeff" -> Codecs.CheckDigits.verhoeff(base).toString()
        "Damm" -> Codecs.CheckDigits.damm(base).toString()
        "Mod 11 (ISBN-10)" -> Codecs.CheckDigits.mod11(base).toString()
        "Mod 97 (ISO 7064 MOD 97-10)" -> Codecs.CheckDigits.mod97(base)
        "IBAN" -> { require(base.length > 2) { "Give the country code followed by the BBAN, e.g. GBWEST12345698765432" }; Codecs.CheckDigits.ibanCheckDigits(base.take(2), base.drop(2)) }
        "Amex SE (Mod 9)" -> Codecs.CheckDigits.mod9(base).toString()
        else -> throw IllegalArgumentException("Unknown algorithm")
    }

    /** Validates a complete number. */
    fun validate(algorithm: String, number: String): Boolean = when (algorithm) {
        "Luhn (Mod 10)" -> Codecs.CheckDigits.luhnValid(number)
        "Verhoeff" -> Codecs.CheckDigits.verhoeffValid(number)
        "Damm" -> Codecs.CheckDigits.dammValid(number)
        "Mod 11 (ISBN-10)" -> Codecs.CheckDigits.mod11Valid(number)
        "Mod 97 (ISO 7064 MOD 97-10)" -> Codecs.CheckDigits.mod97Valid(number)
        "IBAN" -> Codecs.CheckDigits.ibanValid(number)
        "Amex SE (Mod 9)" -> Codecs.CheckDigits.mod9Valid(number)
        else -> throw IllegalArgumentException("Unknown algorithm")
    }

    /** How many trailing characters are the check characters. */
    fun checkLength(algorithm: String): Int = if (algorithm.startsWith("Mod 97") || algorithm == "IBAN") 2 else 1

    /** Complete number formed from a base and its check characters (IBAN: CC ‖ check ‖ BBAN). */
    fun complete(algorithm: String, base: String, check: String): String = if (algorithm == "IBAN") base.take(2).uppercase() + check + base.drop(2).uppercase() else base + check

    fun calculateLuhn(number: String): Int {
        val digits = number.map { it.toString().toInt() }.reversed()
        val sum = digits.mapIndexed { index, digit ->
            if (index % 2 == 0) { // Double every second digit from the right
                val doubled = digit * 2
                if (doubled > 9) doubled - 9 else doubled
            } else {
                digit
            }
        }.sum()
        val mod10 = sum % 10
        return if (mod10 == 0) 0 else 10 - mod10
    }

    fun validateLuhn(number: String): Boolean {
        if (number.length < 2) return false
        val numberWithoutCheckDigit = number.dropLast(1)
        val checkDigit = number.last().toString().toInt()
        return calculateLuhn(numberWithoutCheckDigit) == checkDigit
    }

    fun calculateMod9(number: String): Int {
        val sum = number.map { it.toString().toInt() }.sum()
        val checkDigit = 9 - (sum % 9)
        return if (checkDigit == 9) 0 else checkDigit
    }

    fun validateMod9(number: String): Boolean {
        if (number.length < 2) return false
        val numberWithoutCheckDigit = number.dropLast(1)
        val checkDigit = number.last().toString().toInt()
        return calculateMod9(numberWithoutCheckDigit) == checkDigit
    }
}


@Composable
fun CheckDigitScreen( onBack: () -> Unit) {
    val body: @Composable () -> Unit = {
                CheckDigitCalculatorCard()
            
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "Check Digit Calculator", onBack = onBack, log = ToolLog(CheckDigitLogManager.logEntries, onClear = { CheckDigitLogManager.clearLogs() })), content = body)
        return
    }
    Scaffold(
        topBar = { AppBarWithBack(title = "Check Digit Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left Panel: Input
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            body()
        }
            // Right Panel: Logs
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { CheckDigitLogManager.clearLogs() },
                        logEntries = CheckDigitLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckDigitCalculatorCard() {
    val algorithms = remember { CheckDigitService.algorithms }
    var selectedAlgorithm by remember { mutableStateOf(algorithms.first()) }
    var data by remember { mutableStateOf("49927398716") }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(
            title = "Check Digit Algorithms",
            onDismissRequest = { showInfoDialog = false }
        ) {
            Text("A check digit is a form of redundancy check used for error detection on identification numbers.", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("Luhn (Mod 10):", fontWeight = FontWeight.Bold)
            Text("A widely used algorithm for validating credit card numbers, IMEI numbers, and more. It works by summing digits in a specific way and calculating a final value modulo 10.", style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(8.dp))
            Text("Verhoeff / Damm:", fontWeight = FontWeight.Bold)
            Text("Dihedral-group and quasigroup check digits that catch all single-digit errors and adjacent transpositions.", style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(8.dp))
            Text("Mod 11 (ISBN-10), Mod 97 (ISO 7064), IBAN:", fontWeight = FontWeight.Bold)
            Text("Weighted modulo 11 with 'X' for 10; ISO 7064 MOD 97-10 two-character check; IBAN validation and check-digit generation (enter country code + BBAN to generate).", style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(8.dp))
            Text("Amex SE (Mod 9):", fontWeight = FontWeight.Bold)
            Text("Digital-root style modulo 9 check (9 minus the digit sum mod 9) used in this tool for Amex SE numbers; verify against the Amex specification before relying on it.", style = MaterialTheme.typography.caption)
        }
    }

    val alnum = selectedAlgorithm == "IBAN" || selectedAlgorithm.startsWith("Mod 97") || selectedAlgorithm.startsWith("Mod 11")
    val cleaned = data.filter { !it.isWhitespace() }
    val validation = if (alnum) (if (cleaned.isEmpty()) ValidationResult(ValidationState.EMPTY) else if (cleaned.all { it.isLetterOrDigit() }) ValidationResult(ValidationState.VALID) else ValidationResult(ValidationState.ERROR, "Letters and digits only")) else CheckDigitValidationUtils.validateNumeric(cleaned)
    val isFormValid = validation.state == ValidationState.VALID

    ModernCryptoCard(
        title = "Check Digit Calculator",
        subtitle = "Validate a complete number, or generate the check digit(s) for a base number",
        icon = Icons.Default.Verified,
        onInfoClick = { showInfoDialog = true }
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(
                value = data,
                onValueChange = { data = it },
                label = if (selectedAlgorithm == "IBAN") "IBAN (validate) or country code + BBAN (generate)" else "Input Number",
                validation = validation,
                maxLines = 1
            )

            ModernDropdownField(
                label = "Algorithm",
                value = selectedAlgorithm,
                options = algorithms,
                onSelectionChanged = { index -> selectedAlgorithm = algorithms[index] }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(
                    text = "Validate",
                    onClick = {
                        isLoading = true
                        val inputs = mapOf("Algorithm" to selectedAlgorithm, "Number" to cleaned)
                        try {
                            val isValid = CheckDigitService.validate(selectedAlgorithm, cleaned)
                            val result = if (isValid) "Check digit is VALID." else "Check digit is INVALID."
                            CheckDigitLogManager.logOperation("Validation", inputs, result = result)
                        } catch (e: Exception) {
                            CheckDigitLogManager.logOperation("Validation", inputs, error = e.message)
                        }
                        isLoading = false
                    },
                    isLoading = isLoading,
                    enabled = isFormValid,
                    icon = Icons.Default.CheckCircleOutline,
                    modifier = Modifier.weight(1f)
                )

                ModernButton(
                    text = "Generate",
                    onClick = {
                        isLoading = true
                        val baseNumber = cleaned
                        val inputs = mapOf("Algorithm" to selectedAlgorithm, "Base Number" to baseNumber)
                        try {
                            val check = CheckDigitService.generate(selectedAlgorithm, baseNumber)
                            val result = "Generated Check Digit(s): $check\nFull Number: ${CheckDigitService.complete(selectedAlgorithm, baseNumber, check)}"
                            CheckDigitLogManager.logOperation("Generation", inputs, result = result)
                        } catch (e: Exception) {
                            CheckDigitLogManager.logOperation("Generation", inputs, error = e.message)
                        }
                        isLoading = false
                    },
                    isLoading = isLoading,
                    enabled = data.isNotEmpty() && validation.state != ValidationState.ERROR,
                    icon = Icons.Default.Add,
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
private fun ModernDropdownField(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Dropdown(label, value, options, onSelectionChanged, Modifier); return }
    var expanded by remember { mutableStateOf(false) }
    var textFieldWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(modifier = Modifier.onGloballyPositioned { textFieldWidth = it.size.width }) {
        FixedOutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), readOnly = true,
            trailingIcon = { Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null) },
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = !expanded })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.width(kotlin.with(density) { textFieldWidth.toDp() }).heightIn(max = 300.dp)) {
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "CheckDigitButtonAnimation") { loading ->
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
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    content()
                }
            }
        },
        confirmButton = { Button(onClick = onDismissRequest) { Text("OK") } },
        shape = RoundedCornerShape(12.dp)
    )
}
