package `in`.aicortex.iso8583studio.ui.screens.keys.hsmKeys

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
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.EmvLogManager
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---

private object SafenetValidationUtils {
    fun validateHex(value: String, friendlyName: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$friendlyName cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        if (value.length % 2 != 0) {
            return FieldValidation(ValidationState.ERROR, "Hex string must have an even number of characters.")
        }
        return FieldValidation(ValidationState.VALID)
    }
}

// --- SAFENET KEYS SCREEN ---

private enum class SafenetKeysTabs(val title: String, val icon: ImageVector) {
    ENCRYPT_DECRYPT("Encryption/Decryption", Icons.Default.SyncAlt),
    KEY_LOOKUP("Key Lookup", Icons.Default.Search)
}

private object SafenetLogManager {
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
                val displayValue = if (key.contains("Key", ignoreCase = true)) "${value.take(16)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(
            LogEntry(timestamp = timestamp, type = logType, message = message, details = details)
        )
    }
}

private object SafenetCryptoService {
    // This is a mock service.
    fun processKey(key: String, kmKey: String, operation: String): String {
        return "$operation Result: ${key.hashCode().toString(16).uppercase()}${kmKey.hashCode().toString(16).uppercase()}"
    }

    fun lookupKey(key: String): Map<String, String> {
        return mapOf(
            "kcv" to Random.nextInt(0, 0xFFFFFF).toString(16).uppercase().padStart(6, '0'),
            "parity" to listOf("Odd", "Even").random()
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SafenetKeysScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = SafenetKeysTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "Safenet Keys Calculator", onBackClick = onBack) },
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
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn()) with
                                    (slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "safenet_keys_tab_transition"
                    ) { tab ->
                        when (tab) {
                            SafenetKeysTabs.ENCRYPT_DECRYPT -> EncryptDecryptTab()
                            SafenetKeysTabs.KEY_LOOKUP -> KeyLookupTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { SafenetLogManager.clearLogs() },
                            logEntries = SafenetLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// --- TABS ---

@Composable
private fun EncryptDecryptTab() {
    val keyFormats = remember { listOf(
        "10 - Single length DES",
        "11 - Double length DES3 (ECB Encrypted)",
        "12 - Triple-length DES3 (ECB Encrypted)",
        "13 - Double length DES3 (CBC Encrypted)",
        "14 - Triple length DES3 (CBC encrypted)"
    )}
    val variants = remember { (0..37).map { it.toString(16).uppercase().padStart(2, '0') }.map {
        when(it) {
            "00" -> "00 - DPK"
            "01" -> "01 - PPK"
            "02" -> "02 - MPK"
            "37" -> "37 - KMC"
            else -> it
        }
    }}
    val inputTypes = remember { listOf("ASCII", "Hexadecimal") }

    var key by remember { mutableStateOf("") }
    var keyFormat by remember { mutableStateOf(keyFormats.first()) }
    var variant by remember { mutableStateOf(variants.first()) }
    var keyInputType by remember { mutableStateOf(inputTypes.last()) }
    var kmKey by remember { mutableStateOf("") }

    val isFormValid = SafenetValidationUtils.validateHex(key, "Key").state == ValidationState.VALID &&
            SafenetValidationUtils.validateHex(kmKey, "KM Key").state == ValidationState.VALID

    ModernCryptoCard(title = "Key Encryption / Decryption", subtitle = "Encrypt or decrypt a key using a KM Key", icon = Icons.Default.SyncAlt) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = key, onValueChange = { key = it.uppercase() }, label = "Key (Hex)", validation = SafenetValidationUtils.validateHex(key, "Key"))
            ModernDropdownField(label = "Key Format", value = keyFormat, options = keyFormats, onSelectionChanged = { keyFormat = keyFormats[it] })
            ModernDropdownField(label = "Variant", value = variant, options = variants, onSelectionChanged = { variant = variants[it] })
            ModernDropdownField(label = "Key Input Format", value = keyInputType, options = inputTypes, onSelectionChanged = { keyInputType = inputTypes[it] })
            EnhancedTextField(value = kmKey, onValueChange = { kmKey = it.uppercase() }, label = "KM Key (Hex)", validation = SafenetValidationUtils.validateHex(kmKey, "KM Key"))

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(
                    text = "Encrypt",
                    onClick = {
                        val inputs = mapOf("Key" to key, "Format" to keyFormat, "Variant" to variant, "KM Key" to kmKey)
                        val result = SafenetCryptoService.processKey(key, kmKey, "Encryption")
                        SafenetLogManager.logOperation("Key Encryption", inputs, result)
                    },
                    enabled = isFormValid,
                    icon = Icons.Default.Lock,
                    modifier = Modifier.weight(1f)
                )
                ModernButton(
                    text = "Decrypt",
                    onClick = {
                        val inputs = mapOf("Key" to key, "Format" to keyFormat, "Variant" to variant, "KM Key" to kmKey)
                        val result = SafenetCryptoService.processKey(key, kmKey, "Decryption")
                        SafenetLogManager.logOperation("Key Decryption", inputs, result)
                    },
                    enabled = isFormValid,
                    icon = Icons.Default.LockOpen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun KeyLookupTab() {
    val keyTypes = remember { listOf("Any", "Futurex", "IBM", "Atalla", "VISA") }
    val parityOptions = remember { listOf("None", "Odd", "Even") }

    var key by remember { mutableStateOf("") }
    var checkKcv by remember { mutableStateOf(true) }
    var keyType by remember { mutableStateOf(keyTypes.first()) }
    var kcv by remember { mutableStateOf("") }
    var parity by remember { mutableStateOf(parityOptions.first()) }
    var kmKey by remember { mutableStateOf("") }

    val isFormValid = SafenetValidationUtils.validateHex(key, "Key").state == ValidationState.VALID &&
            SafenetValidationUtils.validateHex(kmKey, "KM Key").state == ValidationState.VALID

    ModernCryptoCard(title = "Key Lookup", subtitle = "Validate key details and KCV", icon = Icons.Default.Search) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = key, onValueChange = { key = it.uppercase() }, label = "Key (Hex)", validation = SafenetValidationUtils.validateHex(key, "Key"), maxLines = 4)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checkKcv, onCheckedChange = { checkKcv = it })
                Text("Check KCV?", style = MaterialTheme.typography.body2)
            }
            ModernDropdownField(label = "Type", value = keyType, options = keyTypes, onSelectionChanged = { keyType = keyTypes[it] })
            EnhancedTextField(value = kcv, onValueChange = { kcv = it.uppercase() }, label = "KCV (Optional)", validation = SafenetValidationUtils.validateHex(kcv, "KCV"))
            ModernDropdownField(label = "Parity", value = parity, options = parityOptions, onSelectionChanged = { parity = parityOptions[it] })
            EnhancedTextField(value = kmKey, onValueChange = { kmKey = it.uppercase() }, label = "KM Key (Hex)", validation = SafenetValidationUtils.validateHex(kmKey, "KM Key"))

            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Lookup Key",
                onClick = {
                    val inputs = mapOf(
                        "Key" to key, "Check KCV" to checkKcv.toString(), "Type" to keyType, "KCV" to kcv, "Parity" to parity, "KM Key" to kmKey
                    )
                    val resultData = SafenetCryptoService.lookupKey(key)
                    val resultString = "Lookup Result:\n  Type: ${resultData["type"]}\n  Parity: ${resultData["parity"]}\n  KCV: ${resultData["kcv"]}"
                    SafenetLogManager.logOperation("Key Lookup", inputs, resultString)
                },
                enabled = isFormValid,
                icon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth()
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
            isError = validation.state == ValidationState.ERROR
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
                    Text(text = subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "SafenetButtonAnimation") { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
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
