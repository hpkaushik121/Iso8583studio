package `in`.aicortex.iso8583studio.ui.screens.payments.dukpt

import ai.cortex.core.crypto.data.FieldValidation
import ai.cortex.core.crypto.data.ValidationState
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

object DUKPTAESValidationUtils {
    fun getExpectedLength(keyType: String): Int {
        return when (keyType) {
            "AES-128", "2TDEA" -> 32
            "AES-192", "3TDEA" -> 48
            "AES-256" -> 64
            else -> 0
        }
    }

    fun validate(value: String, fieldName: String, keyType: String? = null): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$fieldName cannot be empty.")

        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
        }
        if (value.length % 2 != 0) {
            return FieldValidation(ValidationState.ERROR, "$fieldName must have an even number of characters.")
        }

        keyType?.let {
            val expectedLength = getExpectedLength(it)
            if (expectedLength > 0 && value.length != expectedLength) {
                return FieldValidation(ValidationState.ERROR, "$fieldName for $keyType must be $expectedLength characters long.")
            }
        }

        return FieldValidation(ValidationState.VALID)
    }
}


// --- DUKPT AES SCREEN ---

object DUKPTAESLogManager {
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
                append("  $key: $value\n")
            }
            result?.let { append("\nResult:\n$it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

object DUKPTAESService {
    // NOTE: This is a placeholder for actual DUKPT AES cryptographic logic.
    // The results are for demonstration and are not cryptographically secure.

    private fun mockDerive(baseKey: String, ksn: String, keyType: String): String {
        val keyLength = DUKPTAESValidationUtils.getExpectedLength(keyType) / 2
        val baseKeyBytes = baseKey.decodeHex()
        val ksnBytes = ksn.decodeHex()
        val result = ByteArray(keyLength)
        for (i in result.indices) {
            result[i] = (baseKeyBytes[i % baseKeyBytes.size].toInt() xor ksnBytes[i % ksnBytes.size].toInt()).toByte()
        }
        return result.toHex().uppercase()
    }

    fun deriveKeys(bdk: String, ksn: String, workingKeyType: String): Map<String, String> {
        val pek = mockDerive(bdk, ksn, workingKeyType)
        val macKey = mockDerive(pek, ksn, workingKeyType) // Deriving from PEK for variety
        val dek = mockDerive(macKey, ksn, workingKeyType) // Deriving from MAC key
        return mapOf(
            "PEK" to pek,
            "MAC Key" to macKey,
            "DEK" to dek
        )
    }

    fun processPinBlock(pek: String, pinBlock: String): String {
        // Mock symmetrical XOR logic
        return mockDerive(pek, pinBlock, "AES-128") // Using pin block as KSN for mock
    }

    fun generateMac(macKey: String, data: String): String {
        val combined = "$macKey|$data"
        return combined.hashCode().toString(16).take(16).uppercase() // 16 hex char MAC
    }

    fun processData(dek: String, data: String, inputType: String): String {
        val dataBytes = if (inputType == "ASCII") data.toByteArray() else data.decodeHex()
        val dekBytes = dek.decodeHex()

        val result = ByteArray(dataBytes.size)
        for(i in result.indices){
            result[i] = (dataBytes[i].toInt() xor dekBytes[i % dekBytes.size].toInt()).toByte()
        }
        return result.toHex().uppercase()
    }

    // Helper extensions
    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}

enum class DUKPTAESTabs(val title: String, val icon: ImageVector) {
    KEY_DERIVATION("Key Derivation", Icons.Default.VpnKey),
    DUKPT_PIN("DUKPT PIN", Icons.Default.Pin),
    DUKPT_MAC("DUKPT MAC", Icons.Default.Verified),
    DUKPT_DATA("DUKPT Data", Icons.Default.SyncAlt)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DUKPTAESScreen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = DUKPTAESTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "DUKPT AES Utilities", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary,
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
                        label = "dukpt_aes_tab_transition"
                    ) { tab ->
                        when (tab) {
                            DUKPTAESTabs.KEY_DERIVATION -> KeyDerivationCard()
                            DUKPTAESTabs.DUKPT_PIN -> DukptPinCard()
                            DUKPTAESTabs.DUKPT_MAC -> DukptMacCard()
                            DUKPTAESTabs.DUKPT_DATA -> DukptDataCard()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { DUKPTAESLogManager.clearLogs() },
                            logEntries = DUKPTAESLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyDerivationCard() {
    val inputKeyDesignations = remember { listOf("BDK", "IK") }
    var selectedInputKeyDesignation by remember { mutableStateOf(inputKeyDesignations.first()) }

    val initialKeyTypes = remember { listOf("AES-128", "AES-192", "AES-256") }
    var selectedInitialKeyType by remember { mutableStateOf(initialKeyTypes.first()) }

    val workingKeyTypes = remember { listOf("2TDEA", "3TDEA", "AES-128", "AES-192", "AES-256") }
    var selectedWorkingKeyType by remember { mutableStateOf(workingKeyTypes[2]) }

    var bdk by remember { mutableStateOf("") }
    var ksn by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val bdkValidation = DUKPTAESValidationUtils.validate(bdk, "BDK", selectedInitialKeyType)
    val ksnValidation = DUKPTAESValidationUtils.validate(ksn, "KSN")

    val isFormValid = bdk.isNotBlank() && ksn.isNotBlank() &&
            bdkValidation.state != ValidationState.ERROR && ksnValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "AES Key Derivation", subtitle = "Derive working keys from a Base Key", icon = DUKPTAESTabs.KEY_DERIVATION.icon) {
        ModernDropdownField("Input Key Designation", selectedInputKeyDesignation, inputKeyDesignations) { selectedInputKeyDesignation = inputKeyDesignations[it] }
        Spacer(Modifier.height(12.dp))
        ModernDropdownField("Initial Key Type", selectedInitialKeyType, initialKeyTypes) { selectedInitialKeyType = initialKeyTypes[it] }
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = bdk, onValueChange = { bdk = it }, label = "BDK / IK", validation = bdkValidation)
        Spacer(Modifier.height(12.dp))
        ModernDropdownField("Working Key Type", selectedWorkingKeyType, workingKeyTypes) { selectedWorkingKeyType = workingKeyTypes[it] }
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = ksn, onValueChange = { ksn = it }, label = "KSN", validation = ksnValidation)
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Derive Keys", onClick = {
                isLoading = true
                val inputs = mapOf(
                    "Input Key Designation" to selectedInputKeyDesignation,
                    "Initial Key Type" to selectedInitialKeyType,
                    "BDK/IK" to bdk,
                    "Working Key Type" to selectedWorkingKeyType,
                    "KSN" to ksn
                )
                GlobalScope.launch {
                    delay(200)
                    try {
                        val derivedKeys = DUKPTAESService.deriveKeys(bdk, ksn, selectedWorkingKeyType)
                        val resultString = derivedKeys.entries.joinToString(separator = "\n") { "  ${it.key}: ${it.value}" }
                        DUKPTAESLogManager.logOperation("Key Derivation", inputs, result = resultString, executionTime = 205)
                    } catch (e: Exception) {
                        DUKPTAESLogManager.logOperation("Key Derivation", inputs, error = e.message, executionTime = 205)
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

    val pekValidation = DUKPTAESValidationUtils.validate(pek, "PEK")
    val pinBlockValidation = DUKPTAESValidationUtils.validate(pinBlock, "PIN Block")

    val isFormValid = pek.isNotBlank() && pinBlock.isNotBlank() &&
            pekValidation.state != ValidationState.ERROR && pinBlockValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "DUKPT AES PIN", subtitle = "Encrypt or Decrypt a PIN block", icon = DUKPTAESTabs.DUKPT_PIN.icon) {
        EnhancedTextField(value = pek, onValueChange = { pek = it }, label = "PEK (Pin Encryption Key)", validation = pekValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = pinBlock, onValueChange = { pinBlock = it }, label = "PIN Block", validation = pinBlockValidation)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernButton(
                text = "Encrypt", onClick = {
                    isLoading = true
                    val inputs = mapOf("PEK" to pek, "PIN Block" to pinBlock)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = DUKPTAESService.processPinBlock(pek, pinBlock)
                            DUKPTAESLogManager.logOperation("PIN Encryption", inputs, result = "  Encrypted PIN Block: $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTAESLogManager.logOperation("PIN Encryption", inputs, error = e.message, executionTime = 155)
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
                            val result = DUKPTAESService.processPinBlock(pek, pinBlock)
                            DUKPTAESLogManager.logOperation("PIN Decryption", inputs, result = "  Decrypted PIN Block: $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTAESLogManager.logOperation("PIN Decryption", inputs, error = e.message, executionTime = 155)
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
    var macKey by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val macKeyValidation = DUKPTAESValidationUtils.validate(macKey, "Mac Gen Key")
    val dataValidation = DUKPTAESValidationUtils.validate(data, "Data")

    val isFormValid = macKey.isNotBlank() && data.isNotBlank() &&
            macKeyValidation.state != ValidationState.ERROR && dataValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "DUKPT AES MAC", subtitle = "Generate a Message Authentication Code", icon = DUKPTAESTabs.DUKPT_MAC.icon) {
        EnhancedTextField(value = macKey, onValueChange = { macKey = it }, label = "MAC Generation Key", validation = macKeyValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data (Hex)", validation = dataValidation, maxLines = 5)
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate MAC", onClick = {
                isLoading = true
                val inputs = mapOf("MAC Key" to macKey, "Data" to data)
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = DUKPTAESService.generateMac(macKey, data)
                        DUKPTAESLogManager.logOperation("MAC Generation", inputs, result = "  Generated MAC: $result", executionTime = 155)
                    } catch (e: Exception) {
                        DUKPTAESLogManager.logOperation("MAC Generation", inputs, error = e.message, executionTime = 155)
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
    var dek by remember { mutableStateOf("") }
    val dataTypes = remember { listOf("ASCII", "Hexadecimal") }
    var selectedDataType by remember { mutableStateOf(dataTypes.first()) }
    var data by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val dekValidation = DUKPTAESValidationUtils.validate(dek, "DEK")
    val dataValidation = DUKPTAESValidationUtils.validate(data, "Data")

    val isFormValid = dek.isNotBlank() && data.isNotBlank() &&
            dekValidation.state != ValidationState.ERROR && dataValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "DUKPT AES Data", subtitle = "Encrypt or Decrypt data", icon = DUKPTAESTabs.DUKPT_DATA.icon) {
        EnhancedTextField(value = dek, onValueChange = { dek = it }, label = "DEK (Data Encryption Key)", validation = dekValidation)
        Spacer(Modifier.height(12.dp))
        ModernDropdownField("Data Input Type", selectedDataType, dataTypes) { selectedDataType = dataTypes[it] }
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data", validation = dataValidation, maxLines = 5)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernButton(
                text = "Encrypt", onClick = {
                    isLoading = true
                    val inputs = mapOf("DEK" to dek, "Data Input Type" to selectedDataType, "Data" to data)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = DUKPTAESService.processData(dek, data, selectedDataType)
                            DUKPTAESLogManager.logOperation("Data Encryption", inputs, result = "  Encrypted Data (Hex): $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTAESLogManager.logOperation("Data Encryption", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Lock, modifier = Modifier.weight(1f)
            )
            ModernButton(
                text = "Decrypt", onClick = {
                    isLoading = true
                    // Decryption input is always Hex
                    val inputs = mapOf("DEK" to dek, "Encrypted Data" to data)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = DUKPTAESService.processData(dek, data, "Hexadecimal")
                            DUKPTAESLogManager.logOperation("Data Decryption", inputs, result = "  Decrypted Data (Hex): $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTAESLogManager.logOperation("Data Decryption", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.LockOpen, modifier = Modifier.weight(1f)
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
private fun ModernCryptoCard(title: String, subtitle: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f), maxLines = 1)
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
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = {
                    onSelectionChanged(index)
                    expanded = false
                }) {
                    Text(text = option)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ModernButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled && !isLoading, elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)) {
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "DUKPTAESButtonAnimation") { loading ->
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
