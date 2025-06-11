package `in`.aicortex.iso8583studio.ui.screens.rsader

import ai.cortex.core.crypto.data.FieldValidation
import ai.cortex.core.crypto.data.ValidationState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---


private object RsaDerValidationUtils {
    fun validate(value: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "Input cannot be empty.")
        // More specific validation can be added based on encoding type if needed
        return FieldValidation(ValidationState.VALID)
    }
}

// --- RSA DER SCREEN ---

private object RsaDerLogManager {
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
                val displayValue = if (value.length > 100) "${value.take(100)}..." else value
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

private object RsaDerConversionService {
    // This is a mock service. A real implementation would use a robust crypto library like BouncyCastle.
    fun encode(modulus: String, exponent: String, modEncoding: String, expEncoding: String): String {
        return "3082010A0282010100${modulus.filter { it.isLetterOrDigit() }.take(20)}...${exponent.filter { it.isLetterOrDigit() }}..."
    }

    fun decode(data: String): Map<String, String> {
        return mapOf(
            "Modulus" to data.uppercase().filter { it.isLetterOrDigit() }.drop(10).take(128),
            "Exponent" to data.uppercase().filter { it.isLetterOrDigit() }.takeLast(2)
        )
    }
}

private enum class RsaDerTabs(val title: String, val icon: ImageVector) {
    ENCODER("Encoder", Icons.Default.ArrowForward),
    DECODER("Decoder", Icons.Default.ArrowBack),
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RsaDerKeyScreen(window: ComposeWindow? = null, onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = RsaDerTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "RSA DER Public Key Tool", onBackClick = onBack) },
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
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn()) with
                                    (slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "rsa_der_tab_transition"
                    ) { tab ->
                        when (tab) {
                            RsaDerTabs.ENCODER -> EncoderCard()
                            RsaDerTabs.DECODER -> DecoderCard()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { RsaDerLogManager.clearLogs() },
                            logEntries = RsaDerLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EncoderCard() {
    val encodingOptions = remember { listOf("None", "ASCII", "EBCDIC", "BCD", "BCD_LEFT_F", "UTF_8", "ASCII_HEX", "ASCII_BASE64", "EBCDIC_HEX", "ASCII_ZERO_PADDED", "BCD_SIGNED") }
    val negModEncodingOptions = remember { listOf("UNKNOWN", "ENCODING_01_DER_ASN1_PUBLIC_KEY_UNSIGNED", "ENCODING_02_DER_ASN1_PUBLIC_KEY_2S_COMPLIMENT") }

    var modulus by remember { mutableStateOf("0123456789ABCDEF") }
    var modulusEncoding by remember { mutableStateOf(encodingOptions[0]) }
    var exponent by remember { mutableStateOf("03") }
    var exponentEncoding by remember { mutableStateOf(encodingOptions[0]) }
    var isModulusNegative by remember { mutableStateOf(false) }
    var negativeModulusEncoding by remember { mutableStateOf(negModEncodingOptions.first()) }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = RsaDerValidationUtils.validate(modulus).state == ValidationState.VALID && RsaDerValidationUtils.validate(exponent).state == ValidationState.VALID

    ModernCryptoCard(title = "DER Public Key Encoder", subtitle = "Encode a public key into DER format", icon = Icons.Default.Lock) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = modulus, onValueChange = { modulus = it }, label = "Modulus", validation = RsaDerValidationUtils.validate(modulus), maxLines = 5)
            ModernDropdownField(label = "Modulus Encoding", value = modulusEncoding, options = encodingOptions, onSelectionChanged = { modulusEncoding = encodingOptions[it] })

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            EnhancedTextField(value = exponent, onValueChange = { exponent = it }, label = "Exponent", validation = RsaDerValidationUtils.validate(exponent))
            ModernDropdownField(label = "Exponent Encoding", value = exponentEncoding, options = encodingOptions, onSelectionChanged = { exponentEncoding = encodingOptions[it] })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isModulusNegative, onCheckedChange = { isModulusNegative = it })
                Text("Toggle Modulus Negative", style = MaterialTheme.typography.body2)
            }
            AnimatedVisibility(visible = isModulusNegative) {
                ModernDropdownField(label = "Modulus Negative Encoding", value = negativeModulusEncoding, options = negModEncodingOptions, onSelectionChanged = { negativeModulusEncoding = negModEncodingOptions[it] })
            }

            Spacer(Modifier.height(16.dp))
            ModernButton(
                text = "Encode Key",
                onClick = {
                    isLoading = true
                    val inputs = mutableMapOf(
                        "Modulus" to modulus, "Modulus Encoding" to modulusEncoding,
                        "Exponent" to exponent, "Exponent Encoding" to exponentEncoding
                    )
                    if (isModulusNegative) inputs["Negative Modulus Encoding"] = negativeModulusEncoding

                    kotlinx.coroutines.GlobalScope.launch {
                        delay(200)
                        val result = RsaDerConversionService.encode(modulus, exponent, modulusEncoding, exponentEncoding)
                        RsaDerLogManager.logOperation("Encode DER Key", inputs, "DER Encoded Key (Hex): $result", executionTime = 210)
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.EnhancedEncryption, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DecoderCard() {
    val encodingOptions = remember { listOf("None", "ASCII", "EBCDIC", "BCD", "BCD_LEFT_F", "UTF_8", "ASCII_HEX", "ASCII_BASE64", "EBCDIC_HEX", "ASCII_ZERO_PADDED", "BCD_SIGNED") }
    val derEncodingOptions = remember { listOf("UNKNOWN", "ENCODING_01_DER_ASN1_PUBLIC_KEY_UNSIGNED", "ENCODING_02_DER_ASN1_PUBLIC_KEY_2S_COMPLIMENT") }

    var data by remember { mutableStateOf("3082010A0282010100...") }
    var dataEncoding by remember { mutableStateOf(encodingOptions[0]) }
    var derEncoding by remember { mutableStateOf(derEncodingOptions.first()) }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = RsaDerValidationUtils.validate(data).state == ValidationState.VALID

    ModernCryptoCard(title = "DER Public Key Decoder", subtitle = "Decode a DER key to view its components", icon = Icons.Default.LockOpen) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = data, onValueChange = { data = it }, label = "Encoded Data", validation = RsaDerValidationUtils.validate(data), maxLines = 10)
            ModernDropdownField(label = "Data Encoding", value = dataEncoding, options = encodingOptions, onSelectionChanged = { dataEncoding = encodingOptions[it] })
            ModernDropdownField(label = "DER Encoding", value = derEncoding, options = derEncodingOptions, onSelectionChanged = { derEncoding = derEncodingOptions[it] })
            Spacer(Modifier.height(16.dp))
            ModernButton(
                text = "Decode Key",
                onClick = {
                    isLoading = true
                    val inputs = mapOf("Data" to data, "Data Encoding" to dataEncoding, "DER Encoding" to derEncoding)
                    kotlinx.coroutines.GlobalScope.launch {
                        delay(200)
                        val components = RsaDerConversionService.decode(data)
                        val result = "Modulus: ${components["Modulus"]}\nExponent: ${components["Exponent"]}"
                        RsaDerLogManager.logOperation("Decode DER Key", inputs, result, executionTime = 205)
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.VpnKeyOff, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "RsaDerButtonAnimation") { loading ->
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
