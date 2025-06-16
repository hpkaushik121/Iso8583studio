package `in`.aicortex.iso8583studio.ui.screens.Emv.iccDynamicNumber

import ai.cortex.core.crypto.data.FieldValidation
import ai.cortex.core.crypto.data.ValidationState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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



object IccDnValidationUtils {
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

// --- ICC DYNAMIC NUMBER SCREEN ---

object IccDnLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
        addLog(
            LogEntry(
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
                type = LogType.INFO,
                message = "Log history cleared",
                details = ""
            )
        )
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
                val displayValue = if (key.contains("key", ignoreCase = true) || key.contains("mk-dn", ignoreCase = true)) {
                    "${value.take(16)}..."
                } else {
                    value
                }
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  ICC Dynamic Number: $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog( LogEntry(timestamp = timestamp, type= logType, message = message, details = details))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IccDynamicNumberScreen(
    
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "ICC Dynamic Number - MasterCard",
                onBackClick = onBack,
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left Panel: Calculation
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                IccDnCalculationCard()
            }
            // Right Panel: Logs
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { IccDnLogManager.clearLogs() },
                        logEntries = IccDnLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun IccDnCalculationCard() {
    var mkdn by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var panSeqNo by remember { mutableStateOf("541333001111222201") }
    var atc by remember { mutableStateOf("000A") }
    var unpredictableNumber by remember { mutableStateOf("12345678") }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(
            title = "MasterCard ICC Dynamic Number",
            onDismissRequest = { showInfoDialog = false }
        ) {
            Text("The ICC Dynamic Number is a MasterCard-specific value used in M/Chip 4 transactions to provide a dynamic element for cryptogram calculation.", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("Process:", fontWeight = FontWeight.Bold)
            Text("1. A data block is formed by concatenating the Application Transaction Counter (ATC) and the Unpredictable Number.", style = MaterialTheme.typography.caption)
            Text("2. This concatenated data is then encrypted using a unique session key.", style = MaterialTheme.typography.caption)
            Text("3. The session key itself is derived from the Master Key for Dynamic Number (MK-DN) and the PAN + PAN Sequence number.", style = MaterialTheme.typography.caption)
            Text("4. The result of the encryption is the 8-byte ICC Dynamic Number.", style = MaterialTheme.typography.caption)
        }
    }

    val isFormValid = listOf(
        IccDnValidationUtils.validateHexString(mkdn, 32),
        IccDnValidationUtils.validateHexString(panSeqNo),
        IccDnValidationUtils.validateHexString(atc, 4),
        IccDnValidationUtils.validateHexString(unpredictableNumber, 8)
    ).all { it.state == ValidationState.VALID }

    ModernCryptoCard(
        title = "ICC Dynamic Number",
        subtitle = "MasterCard M/Chip Calculation",
        icon = Icons.Default.DynamicForm,
        onInfoClick = { showInfoDialog = true }
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(
                value = mkdn,
                onValueChange = { mkdn = it.uppercase() },
                label = "MK-DN (Master Key for Dynamic Number)",
                validation = IccDnValidationUtils.validateHexString(mkdn, 32, friendlyName = "MK-DN")
            )
            EnhancedTextField(
                value = panSeqNo,
                onValueChange = { panSeqNo = it.uppercase() },
                label = "PN/PAN + Seq No.",
                validation = IccDnValidationUtils.validateHexString(panSeqNo, friendlyName = "PAN + Seq No.")
            )
            EnhancedTextField(
                value = atc,
                onValueChange = { atc = it.uppercase() },
                label = "Application Transaction Counter (ATC)",
                validation = IccDnValidationUtils.validateHexString(atc, 4, friendlyName = "ATC")
            )
            EnhancedTextField(
                value = unpredictableNumber,
                onValueChange = { unpredictableNumber = it.uppercase() },
                label = "Unpredictable Number",
                validation = IccDnValidationUtils.validateHexString(unpredictableNumber, 8, friendlyName = "Unpredictable Number")
            )
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Generate Dynamic Number",
                onClick = {
                    isLoading = true
                    val inputs = mapOf(
                        "MK-DN" to mkdn,
                        "PN/PAN Seq No." to panSeqNo,
                        "ATC" to atc,
                        "Unpredictable Number" to unpredictableNumber
                    )
                    GlobalScope.launch {
                        delay(500)
                        val result = "A1B2C3D4E5F67890"
                        IccDnLogManager.logOperation("ICC Dynamic Number", inputs, result = result, executionTime = 510)
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                enabled = isFormValid,
                icon = Icons.Default.GeneratingTokens,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "IccDnButtonAnimation") { loading ->
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
