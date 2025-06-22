package `in`.aicortex.iso8583studio.ui.screens.Emv


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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.FieldValidation
import `in`.aicortex.iso8583studio.data.model.ValidationState

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


object ApduValidationUtils {
    fun validateHexString(value: String, expectedLength: Int): FieldValidation {
        if (value.isEmpty()) {
            return FieldValidation(ValidationState.EMPTY, "Field cannot be empty.", "Enter $expectedLength hex chars")
        }
        if (!value.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.", "${value.length}/$expectedLength")
        }
        if (value.length != expectedLength) {
            return FieldValidation(ValidationState.ERROR, "Must be exactly $expectedLength characters.", "${value.length}/$expectedLength")
        }
        return FieldValidation(ValidationState.VALID, "", "${value.length}/$expectedLength")
    }
}

// --- APDU RESPONSE SERVICE ---
object ApduResponseService {
    private val statusWordMap = mapOf(
        "9000" to "Command successfully executed.",
        "61" to "Command successfully executed; ‘xx’ bytes of data are available and can be requested using GET RESPONSE.",
        "6200" to "No information given (NV-Ram not changed).",
        "6281" to "Part of returned data may be corrupted.",
        "6282" to "End of file/record reached before reading Le bytes.",
        "6283" to "Selected file invalidated.",
        "6284" to "FCI not formatted according to ISO 7816-4.",
        "6300" to "No information given (NV-Ram changed).",
        "63" to "Warning, card status may have changed; ‘xx’ is a card-specific warning.",
        "6400" to "Execution error.",
        "6581" to "Memory failure.",
        "66" to "Security-related issues.",
        "6700" to "Wrong length; no further indication.",
        "6881" to "Logical channel not supported.",
        "6882" to "Secure messaging not supported.",
        "6981" to "Command incompatible with file structure.",
        "6982" to "Security status not satisfied.",
        "6983" to "Authentication method blocked.",
        "6984" to "Referenced data invalidated.",
        "6985" to "Conditions of use not satisfied.",
        "6986" to "Command not allowed (no current EF).",
        "6987" to "Expected SM data objects missing.",
        "6988" to "SM data objects incorrect.",
        "6A80" to "Incorrect parameters in the data field.",
        "6A81" to "Function not supported.",
        "6A82" to "File not found.",
        "6A83" to "Record not found.",
        "6A84" to "Not enough memory space in the file.",
        "6A85" to "Lc inconsistent with TLV structure.",
        "6A86" to "Incorrect parameters P1-P2.",
        "6A87" to "Lc inconsistent with P1-P2.",
        "6A88" to "Referenced data not found.",
        "6B00" to "Wrong parameters P1-P2.",
        "6C" to "Wrong length Le; ‘xx’ is the correct length.",
        "6D00" to "Instruction code not supported or invalid.",
        "6E00" to "Class not supported.",
        "6F00" to "No precise diagnosis."
    )

    fun getDescription(code: String): String {
        val upperCode = code.uppercase()
        // Exact match
        if (statusWordMap.containsKey(upperCode)) {
            return statusWordMap[upperCode]!!
        }
        // Range match (e.g., 61xx, 63xx, 6Cxx)
        val prefix = upperCode.substring(0, 2)
        if (statusWordMap.containsKey(prefix)) {
            val xx = upperCode.substring(2, 4)
            return statusWordMap[prefix]!!.replace("‘xx’", xx)
        }
        return "Status word not found in dictionary."
    }
}

// --- LOG MANAGER ---
object ApduLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
    }

    private fun addLog(entry: LogEntry) {
        _logEntries.add(0, entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, logEntries.size)
    }

    fun logResponse(code: String, description: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val logType = if (code.startsWith("90")) LogType.TRANSACTION else if(code.startsWith("61")) LogType.INFO else LogType.ERROR
        addLog(LogEntry(timestamp = timestamp, type = logType, message = "Status: $code", details = description))
    }
}

// --- MAIN SCREEN ---
@Composable
fun ApduResponseQueryScreen(
    
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "APDU Response Query",
                onBackClick = onBack,
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left Panel: Input
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                ApduQueryCard()
            }
            // Right Panel: Logs
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { ApduLogManager.clearLogs() },
                        logEntries = ApduLogManager.logEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun ApduQueryCard() {
    var apduResponseCode by remember { mutableStateOf("9000") }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(
            title = "APDU Status Words (SW1-SW2)",
            onDismissRequest = { showInfoDialog = false }
        ) {
            Text(
                "APDU Status Words are 2-byte codes returned by a smart card after processing a command. They indicate the outcome of the operation.",
                style = MaterialTheme.typography.body2
            )
            Spacer(Modifier.height(8.dp))
            Text("Common Categories:", fontWeight = FontWeight.Bold)
            Text("• 90 00: Normal processing, successful completion.", style = MaterialTheme.typography.caption)
            Text("• 61 xx: Normal processing, response data is available.", style = MaterialTheme.typography.caption)
            Text("• 63 xx: Warning, state of non-volatile memory may have changed.", style = MaterialTheme.typography.caption)
            Text("• 6A xx: Error, command not allowed or incorrect parameters.", style = MaterialTheme.typography.caption)
            Text("• 6E xx: Error, class not supported.", style = MaterialTheme.typography.caption)
        }
    }

    val validation = ApduValidationUtils.validateHexString(apduResponseCode, 4)
    val isFormValid = validation.state == ValidationState.VALID

    ModernCryptoCard(
        title = "APDU Response Query",
        subtitle = "Get the meaning of an APDU status word",
        icon = Icons.Default.HelpOutline,
        onInfoClick = { showInfoDialog = true }
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(
                value = apduResponseCode,
                onValueChange = { if (it.length <= 4) apduResponseCode = it.uppercase() },
                label = "APDU Status Word (e.g., 9000)",
                validation = validation
            )
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Query Response",
                onClick = {
                    isLoading = true
                    GlobalScope.launch {
                        delay(200) // Simulate a quick lookup
                        val description = ApduResponseService.getDescription(apduResponseCode)
                        ApduLogManager.logResponse(apduResponseCode, description)
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                enabled = isFormValid,
                icon = Icons.Default.Search,
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
        Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (validation.message.isNotEmpty()) {
                Text(
                    text = validation.message,
                    color = if(validation.state == ValidationState.ERROR) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.caption
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = validation.helperText,
                color = if (validation.state == ValidationState.VALID) SuccessGreen else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.End
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ModernButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled && !isLoading, elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)) {
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "ApduButtonAnimation") { loading ->
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
