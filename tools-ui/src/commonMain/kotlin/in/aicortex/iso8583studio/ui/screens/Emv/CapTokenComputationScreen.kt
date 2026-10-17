package `in`.aicortex.iso8583studio.ui.screens.Emv

import io.cryptocalc.emv.EmvCap

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
import `in`.aicortex.iso8583studio.ui.screens.components.LogPanelWithAutoScroll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// --- COMMON UI & VALIDATION FOR THIS SCREEN ---

object CapValidationUtils {
    fun validateHexString(value: String, expectedLength: Int? = null, allowEmpty: Boolean = false, friendlyName: String = "Field"): ValidationResult {
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
        expectedLength?.let {
            if (value.length != it) return ValidationResult(ValidationState.ERROR, "Must be exactly $it characters", "${value.length}/$it chars")
        }
        return ValidationResult(ValidationState.VALID, "", "${value.length} chars")
    }
}

// --- CAP TOKEN COMPUTATION SCREEN ---

object CapLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
    }

    private fun addLog(entry: LogEntry) {
        _logEntries.add(entry)
    }

    fun logOperation(operation: String, inputs: Map<String, String>, result: String? = null, error: String? = null, executionTime: Long = 0L) {
        if (result == null && error == null) return
        Analytics.calculationRun(operation, success = result != null, durationMs = executionTime)

        val timestamp = nowLogTimestamp()
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                val displayValue = if (key.contains("key", ignoreCase = true) || key.contains("IPB", ignoreCase = true)) {
                    "${value.take(16)}..."
                } else {
                    value
                }
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  CAP Token: $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog( LogEntry(timestamp = timestamp, type= logType, message = message, details = details))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CapTokenComputationScreen(
    
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "CAP Token Computation",
                onBackClick = onBack,
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left Panel: Calculation
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                CapCalculationCard()
            }
            // Right Panel: Logs
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { CapLogManager.clearLogs() },
                        logEntries = CapLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun CapCalculationCard() {
    var ipb by remember { mutableStateOf("FFFF00000000000000FF") }
    var iaf by remember { mutableStateOf("00") }
    var panSn by remember { mutableStateOf("541333001111222201") }
    var cid by remember { mutableStateOf("80") }
    var atc by remember { mutableStateOf("001A") }
    var ac by remember { mutableStateOf("A1B2C3D4E5F67890") }
    var iad by remember { mutableStateOf("0110A00003220000000000000000000000FF") }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(title = "CAP Token Computation", onDismissRequest = { showInfoDialog = false }) {
            Text("The Chip Authentication Program token is built from the GENERATE AC response: the Issuer Proprietary Bitmap (IPB, tag 9F56) selects bits from CID ‖ ATC ‖ AC ‖ IAD; the selected bits, kept in order, form a binary number displayed in decimal. The IPB is right-aligned to the data. When the Issuer Authentication Flags (IAF, 9F55) have bit 8 of byte 1 set, the PAN sequence number is prefixed to the token.", style = MaterialTheme.typography.body2)
        }
    }

    val isFormValid = listOf(ipb, cid, atc, ac, iad).all { CapValidationUtils.validateHexString(it).state == ValidationState.VALID } && cid.length == 2 && atc.length == 4 && ac.length == 16

    ModernCryptoCard(title = "CAP Token Computation", subtitle = "Mastercard Chip Authentication Program (IPB bit filter)", icon = Icons.Default.Token, onInfoClick = { showInfoDialog = true }) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = ipb, onValueChange = { ipb = it.uppercase() }, label = "IPB (Issuer Proprietary Bitmap, 9F56)", validation = CapValidationUtils.validateHexString(ipb))
            EnhancedTextField(value = iaf, onValueChange = { iaf = it.uppercase() }, label = "IAF (Issuer Authentication Flags, 9F55; bit 8 = prefix PSN)", validation = CapValidationUtils.validateHexString(iaf, allowEmpty = true))
            EnhancedTextField(value = panSn, onValueChange = { panSn = it.filter { c -> c.isDigit() } }, label = "PAN + Sequence Number (last 2 digits = PSN)", validation = ValidationResult(ValidationState.VALID))
            EnhancedTextField(value = cid, onValueChange = { cid = it.uppercase() }, label = "CID (9F27)", validation = CapValidationUtils.validateHexString(cid, 2))
            EnhancedTextField(value = atc, onValueChange = { atc = it.uppercase() }, label = "ATC (9F36)", validation = CapValidationUtils.validateHexString(atc, 4))
            EnhancedTextField(value = ac, onValueChange = { ac = it.uppercase() }, label = "AC (9F26)", validation = CapValidationUtils.validateHexString(ac, 16))
            EnhancedTextField(value = iad, onValueChange = { iad = it.uppercase() }, label = "IAD (9F10)", validation = CapValidationUtils.validateHexString(iad))
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Generate Token",
                onClick = {
                    val inputs = mapOf("IPB" to ipb, "IAF" to iaf, "PAN+SN" to panSn, "CID" to cid, "ATC" to atc, "AC" to ac, "IAD" to iad)
                    try {
                        val t = EmvCap.token(ipb.hexToBytes(), cid.hexToBytes(), atc.hexToBytes(), ac.hexToBytes(), iad.hexToBytes(), if (iaf.isBlank()) null else iaf.hexToBytes(), panSn.takeLast(2))
                        CapLogManager.logOperation("CAP Token Computation", inputs, result = "Data (CID‖ATC‖AC‖IAD): ${(cid + atc + ac + iad).uppercase()}\nMasked data: ${t.maskedData.toHexUpper()}\nSelected bits (${t.bits.length}): ${t.bits}\n${if (t.psnPrefix) "PSN prefixed per IAF\n" else ""}Token: ${t.decimal}")
                    } catch (e: Exception) {
                        CapLogManager.logOperation("CAP Token Computation", inputs, error = e.message)
                    }
                },
                enabled = isFormValid, icon = Icons.Default.GeneratingTokens, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


// --- SHARED UI COMPONENTS ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult) {
    Column(modifier = modifier) {
        FixedOutlinedTextField(
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "CapButtonAnimation") { loading ->
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
