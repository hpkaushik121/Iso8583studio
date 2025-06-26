package `in`.aicortex.iso8583studio.ui.screens.payments.dukpt

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

object DUKPTValidationUtils {
    fun validate(value: String, fieldName: String, isHex: Boolean = true, length: Int? = null): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$fieldName cannot be empty.")

        if (isHex) {
            if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
                return ValidationResult(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
            }
            if (value.length % 2 != 0) {
                return ValidationResult(ValidationState.ERROR, "$fieldName must have an even number of characters.")
            }
        }

        length?.let {
            if (value.length != it) {
                return ValidationResult(ValidationState.ERROR, "$fieldName must be $it characters long.")
            }
        }

        return ValidationResult(ValidationState.VALID)
    }
}


// --- DUKPT SCREEN ---

object DUKPTLogManager {
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

object DUKPTService {
    // NOTE: This is a placeholder for actual DUKPT cryptographic logic.
    // The results are for demonstration and are not cryptographically secure.
    // The mock XOR logic is symmetrical, so the same function works for encryption and decryption.

    fun derivePek(bdk: String, ksn: String, inputKeyType: String): String {
        val bdkBytes = bdk.decodeHex()
        val ksnBytes = ksn.decodeHex()
        val result = ByteArray(bdkBytes.size)
        for (i in result.indices) {
            result[i] = (bdkBytes[i].toInt() xor ksnBytes[i % ksnBytes.size].toInt()).toByte()
        }
        return result.toHex().uppercase().take(16) // Simulate a derived PEK
    }

    fun processPinBlock(pek: String, pinBlock: String): String {
        val pekBytes = pek.decodeHex()
        val pinBlockBytes = pinBlock.decodeHex()
        val result = ByteArray(pinBlockBytes.size)
        for (i in result.indices) {
            result[i] = (pekBytes[i % pekBytes.size].toInt() xor pinBlockBytes[i].toInt()).toByte()
        }
        return result.toHex().uppercase()
    }

    fun generateMac(pek: String, algorithm: String, data: String): String {
        val combined = "$pek|$algorithm|$data"
        return combined.hashCode().toString(16).uppercase().take(8)
    }

    fun processData(pek: String, isDataVariant: Boolean, dataInputType: String, data: String): String {
        val dataBytes = if (dataInputType == "ASCII") data.toByteArray() else data.decodeHex()
        val pekBytes = pek.decodeHex()
        val keyModifier = if (isDataVariant) "FFFFFFFFFFFFFFFF".decodeHex() else "0000000000000000".decodeHex()

        val result = ByteArray(dataBytes.size)
        for(i in result.indices){
            result[i] = (dataBytes[i].toInt() xor pekBytes[i % pekBytes.size].toInt() xor keyModifier[i % keyModifier.size].toInt()).toByte()
        }
        return result.toHex().uppercase()
    }

    // Helper extensions
    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}

enum class DUKPTTabs(val title: String, val icon: ImageVector) {
    PEK_DERIVATION("PEK Derivation", Icons.Default.VpnKey),
    DUKPT_PIN("DUKPT PIN", Icons.Default.Pin),
    DUKPT_MAC("DUKPT MAC", Icons.Default.Verified),
    DUKPT_DATA("DUKPT Data", Icons.Default.SyncAlt)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DukptIso9797Screen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = DUKPTTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "DUKPT Utilities", onBackClick = onBack) },
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Icon(imageVector = tab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tab.title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
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
                        label = "dukpt_tab_transition"
                    ) { tab ->
                        when (tab) {
                            DUKPTTabs.PEK_DERIVATION -> PekDerivationCard()
                            DUKPTTabs.DUKPT_PIN -> DukptPinCard()
                            DUKPTTabs.DUKPT_MAC -> DukptMacCard()
                            DUKPTTabs.DUKPT_DATA -> DukptDataCard()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { DUKPTLogManager.clearLogs() },
                            logEntries = DUKPTLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PekDerivationCard() {
    val keyTypes = remember { listOf("BDK", "IPEK") }
    var selectedKeyType by remember { mutableStateOf(keyTypes.first()) }
    var bdk by remember { mutableStateOf("") }
    var ksn by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val bdkValidation = DUKPTValidationUtils.validate(bdk, "BDK", length = 32)
    val ksnValidation = DUKPTValidationUtils.validate(ksn, "KSN", length = 20)

    val isFormValid = bdk.isNotBlank() && ksn.isNotBlank() &&
            bdkValidation.state != ValidationState.ERROR && ksnValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "PEK Derivation", subtitle = "Derive Pin Entry Key from BDK/IPEK", icon = DUKPTTabs.PEK_DERIVATION.icon) {
        ModernDropdownField(
            label = "Input Key Designation",
            value = selectedKeyType,
            options = keyTypes,
            onSelectionChanged = { index -> selectedKeyType = keyTypes[index] }
        )
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = bdk, onValueChange = { bdk = it }, label = "BDK (32 Hex Chars)", validation = bdkValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = ksn, onValueChange = { ksn = it }, label = "KSN (20 Hex Chars)", validation = ksnValidation)
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Derive PEK", onClick = {
                isLoading = true
                val inputs = mapOf("Input Key Type" to selectedKeyType, "BDK" to bdk, "KSN" to ksn)
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = DUKPTService.derivePek(bdk, ksn, selectedKeyType)
                        DUKPTLogManager.logOperation("PEK Derivation", inputs, result = "Derived PEK: $result", executionTime = 155)
                    } catch (e: Exception) {
                        DUKPTLogManager.logOperation("PEK Derivation", inputs, error = e.message, executionTime = 155)
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.ArrowForward, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DukptPinCard() {
    var pek by remember { mutableStateOf("") }
    var pinBlock by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val pekValidation = DUKPTValidationUtils.validate(pek, "PEK", length = 16)
    val pinBlockValidation = DUKPTValidationUtils.validate(pinBlock, "PIN Block", length = 16)

    val isFormValid = pek.isNotBlank() && pinBlock.isNotBlank() &&
            pekValidation.state != ValidationState.ERROR && pinBlockValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "DUKPT PIN", subtitle = "Encrypt or Decrypt a PIN block using a PEK", icon = DUKPTTabs.DUKPT_PIN.icon) {
        EnhancedTextField(value = pek, onValueChange = { pek = it }, label = "PEK (16 Hex Chars)", validation = pekValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = pinBlock, onValueChange = { pinBlock = it }, label = "PIN Block (16 Hex Chars)", validation = pinBlockValidation)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernButton(
                text = "Encrypt", onClick = {
                    isLoading = true
                    val inputs = mapOf("PEK" to pek, "PIN Block" to pinBlock)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = DUKPTService.processPinBlock(pek, pinBlock)
                            DUKPTLogManager.logOperation("PIN Encryption", inputs, result = "Encrypted PIN Block: $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTLogManager.logOperation("PIN Encryption", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Lock, modifier = Modifier.weight(1f)
            )
            ModernButton(
                text = "Decrypt", onClick = {
                    isLoading = true
                    val inputs = mapOf("PEK" to pek, "Encrypted PIN Block" to pinBlock)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = DUKPTService.processPinBlock(pek, pinBlock)
                            DUKPTLogManager.logOperation("PIN Decryption", inputs, result = "Decrypted PIN Block: $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTLogManager.logOperation("PIN Decryption", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.LockOpen, modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DukptMacCard() {
    var pek by remember { mutableStateOf("") }
    val algorithms = remember { listOf("DES", "3DES") }
    var selectedAlgorithm by remember { mutableStateOf(algorithms.first()) }
    var data by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val pekValidation = DUKPTValidationUtils.validate(pek, "PEK", length = 16)
    val dataValidation = DUKPTValidationUtils.validate(data, "Data", isHex = true)

    val isFormValid = pek.isNotBlank() && data.isNotBlank() &&
            pekValidation.state != ValidationState.ERROR && dataValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "DUKPT MAC Generation", subtitle = "Generate a MAC for data using a PEK", icon = DUKPTTabs.DUKPT_MAC.icon) {
        EnhancedTextField(value = pek, onValueChange = { pek = it }, label = "PEK (16 Hex Chars)", validation = pekValidation)
        Spacer(Modifier.height(12.dp))
        Text("Algorithm", style = MaterialTheme.typography.body2, fontWeight = FontWeight.Medium)
        Row {
            algorithms.forEach { algorithm ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (algorithm == selectedAlgorithm),
                        onClick = { selectedAlgorithm = algorithm }
                    )
                    Text(text = algorithm, style = MaterialTheme.typography.body1, modifier = Modifier.clickable { selectedAlgorithm = algorithm })
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data (Hex)", validation = dataValidation, maxLines = 5)
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate MAC", onClick = {
                isLoading = true
                val inputs = mapOf("PEK" to pek, "Algorithm" to selectedAlgorithm, "Data" to data)
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = DUKPTService.generateMac(pek, selectedAlgorithm, data)
                        DUKPTLogManager.logOperation("MAC Generation", inputs, result = "Generated MAC: $result", executionTime = 155)
                    } catch (e: Exception) {
                        DUKPTLogManager.logOperation("MAC Generation", inputs, error = e.message, executionTime = 155)
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.CheckCircle, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DukptDataCard() {
    var pek by remember { mutableStateOf("") }
    var isDataVariant by remember { mutableStateOf(false) }
    val dataTypes = remember { listOf("ASCII", "Hexadecimal") }
    var selectedDataType by remember { mutableStateOf(dataTypes.first()) }
    var data by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val pekValidation = DUKPTValidationUtils.validate(pek, "PEK", length = 16)
    val dataValidation = DUKPTValidationUtils.validate(data, "Data", isHex = selectedDataType == "Hexadecimal")

    val isFormValid = pek.isNotBlank() && data.isNotBlank() &&
            pekValidation.state != ValidationState.ERROR && dataValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "DUKPT Data", subtitle = "Encrypt or Decrypt data using a PEK", icon = DUKPTTabs.DUKPT_DATA.icon) {
        EnhancedTextField(value = pek, onValueChange = { pek = it }, label = "PEK (16 Hex Chars)", validation = pekValidation)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isDataVariant = !isDataVariant }) {
            Switch(checked = isDataVariant, onCheckedChange = { isDataVariant = it })
            Spacer(Modifier.width(8.dp))
            Text("Use Data Variant Key", style = MaterialTheme.typography.body1)
        }
        Spacer(Modifier.height(12.dp))

        ModernDropdownField(
            label = "Data Input Type",
            value = selectedDataType,
            options = dataTypes,
            onSelectionChanged = { index -> selectedDataType = dataTypes[index] }
        )
        Spacer(Modifier.height(12.dp))

        EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data", validation = dataValidation, maxLines = 5)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernButton(
                text = "Encrypt", onClick = {
                    isLoading = true
                    val inputs = mapOf("PEK" to pek, "Is Data Variant" to isDataVariant.toString(), "Data Input Type" to selectedDataType, "Data" to data)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = DUKPTService.processData(pek, isDataVariant, selectedDataType, data)
                            DUKPTLogManager.logOperation("Data Encryption", inputs, result = "Encrypted Data (Hex): $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTLogManager.logOperation("Data Encryption", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Lock, modifier = Modifier.weight(1f)
            )
            ModernButton(
                text = "Decrypt", onClick = {
                    isLoading = true
                    // For decryption, data must be Hex. The dropdown is ignored.
                    val inputs = mapOf("PEK" to pek, "Is Data Variant" to isDataVariant.toString(), "Encrypted Data" to data)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = DUKPTService.processData(pek, isDataVariant, "Hexadecimal", data)
                            DUKPTLogManager.logOperation("Data Decryption", inputs, result = "Decrypted Data (Hex): $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTLogManager.logOperation("Data Decryption", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.LockOpen, modifier = Modifier.weight(1f)
            )
        }
    }
}


// --- SHARED UI COMPONENTS (from original file) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult) {
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
                    Text(text = subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f), maxLines = 1)
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "DUKPTButtonAnimation") { loading ->
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
