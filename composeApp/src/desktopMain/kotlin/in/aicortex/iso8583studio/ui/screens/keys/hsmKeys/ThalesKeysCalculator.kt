package `in`.aicortex.iso8583studio.ui.screens.thaleskeys

import `in`.aicortex.iso8583studio.data.model.FieldValidation
import `in`.aicortex.iso8583studio.data.model.ValidationState
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
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
import kotlin.random.Random


private object ThalesValidationUtils {
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

// --- THALES KEYS SCREEN ---

private enum class ThalesKeysTabs(val title: String, val icon: ImageVector) {
    ENCRYPT_DECRYPT("Key Encryption / Decryption", Icons.Default.SyncAlt),
    KEY_LOOKUP("Key Lookup", Icons.Default.Search)
}

private object ThalesLogManager {
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
        addLog(LogEntry(timestamp, logType, message, details))
    }
}

private object ThalesCryptoService {
    fun processKey(key: String, lmk: String, operation: String): String {
        return "$operation Result: ${key.hashCode().toString(16).uppercase()}${lmk.hashCode().toString(16).uppercase()}"
    }

    fun lookupKey(key: String): Map<String, String> {
        return mapOf("kcv" to Random.nextInt(0, 0xFFFFFF).toString(16).uppercase().padStart(6, '0'))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ThalesKeysScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = ThalesKeysTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "Thales Keys Encryption/Decoding", onBackClick = onBack) },
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
                        }
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
                        label = "thales_keys_tab_transition"
                    ) { tab ->
                        when (tab) {
                            ThalesKeysTabs.ENCRYPT_DECRYPT -> EncryptDecryptTab()
                            ThalesKeysTabs.KEY_LOOKUP -> KeyLookupTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { ThalesLogManager.clearLogs() },
                            logEntries = ThalesLogManager.logEntries
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
    val lmkPairs = remember { listOf(
        "00-01: 01010101010101017902CD1FD36EF8BA",
        "02-03: 20202020202020203131313131313131",
        "04-05: 40404040404040405151515151515151",
        "06-07: 61616161616161617070707070707070",
        "08-09: 80808080808080809191919191919191",
        "10-11: A1A1A1A1A1A1A1A1B0B0B0B0B0B0B0B0",
        "12-13: C1C1010101010101D0D0010101010101",
        "14-15: E0E0010101010101F1F1010101010101"
        // ... more pairs can be added
    )}
    val variants = remember { (0..9).map { "$it: U" } + ('A'..'F').map { "$it: A6" } }

    var key by remember { mutableStateOf("") }
    var keyScheme by remember { mutableStateOf("0") }
    var lmkSize by remember { mutableStateOf("Double") }
    var selectedLmkPair by remember { mutableStateOf(lmkPairs.first()) }
    var selectedVariant by remember { mutableStateOf(variants[1]) }

    val isFormValid = ThalesValidationUtils.validateHex(key, "Key").state == ValidationState.VALID

    ModernCryptoCard(title = "Key Encryption / Decryption", subtitle = "Encrypt or decrypt a key under an LMK", icon = Icons.Default.SyncAlt) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = key, onValueChange = { key = it.uppercase() }, label = "Key (Hex)", validation = ThalesValidationUtils.validateHex(key, "Key"))
            ModernDropdownField(label = "Key Scheme", value = keyScheme, options = listOf("0"), onSelectionChanged = { keyScheme = "0" })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LMK size:", style = MaterialTheme.typography.body2, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(16.dp))
                Row(Modifier.selectableGroup()) {
                    RadioButton(selected = lmkSize == "Double", onClick = { lmkSize = "Double" })
                    Text("Double", Modifier.padding(start = 4.dp, end = 16.dp))
                    RadioButton(selected = lmkSize == "Triple", onClick = { lmkSize = "Triple" })
                    Text("Triple", Modifier.padding(start = 4.dp))
                }
            }

            ModernDropdownField(label = "LMK Pair", value = selectedLmkPair, options = lmkPairs, onSelectionChanged = { selectedLmkPair = lmkPairs[it] })
            ModernDropdownField(label = "Variant", value = selectedVariant, options = variants, onSelectionChanged = { selectedVariant = variants[it] })

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(
                    text = "Encrypt",
                    onClick = {
                        val inputs = mapOf("Key" to key, "LMK Pair" to selectedLmkPair, "Variant" to selectedVariant)
                        val result = ThalesCryptoService.processKey(key, selectedLmkPair, "Encryption")
                        ThalesLogManager.logOperation("Encryption", inputs, result)
                    },
                    enabled = isFormValid,
                    icon = Icons.Default.Lock,
                    modifier = Modifier.weight(1f)
                )
                ModernButton(
                    text = "Decrypt",
                    onClick = {
                        val inputs = mapOf("Key" to key, "LMK Pair" to selectedLmkPair, "Variant" to selectedVariant)
                        val result = ThalesCryptoService.processKey(key, selectedLmkPair, "Decryption")
                        ThalesLogManager.logOperation("Decryption", inputs, result)
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
    val parityOptions = remember { listOf("Any", "Odd", "Even") }

    var key by remember { mutableStateOf("") }
    var checkKcv by remember { mutableStateOf(true) }
    var kcv by remember { mutableStateOf("") }
    var parity by remember { mutableStateOf(parityOptions.first()) }

    val isFormValid = ThalesValidationUtils.validateHex(key, "Key").state == ValidationState.VALID

    ModernCryptoCard(title = "Key Lookup", subtitle = "Validate key details", icon = Icons.Default.Search) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = key, onValueChange = { key = it.uppercase() }, label = "Key (Hex)", validation = ThalesValidationUtils.validateHex(key, "Key"), maxLines = 4)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checkKcv, onCheckedChange = { checkKcv = it })
                Text("Check KCV?", style = MaterialTheme.typography.body2)
            }
            EnhancedTextField(value = kcv, onValueChange = { kcv = it.uppercase() }, label = "KCV (Optional)", validation = ThalesValidationUtils.validateHex(kcv, "KCV"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Parity:", style = MaterialTheme.typography.body2, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(16.dp))
                Row(Modifier.selectableGroup()) {
                    parityOptions.forEach { text ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = parity == text, onClick = { parity = text })
                            Text(text, Modifier.padding(start = 2.dp, end = 16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Lookup Key",
                onClick = {
                    val inputs = mapOf("Key" to key, "Check KCV" to checkKcv.toString(), "KCV" to kcv, "Parity" to parity)
                    val resultData = ThalesCryptoService.lookupKey(key)
                    val resultString = "Lookup Result:\n  Parity: ${resultData["parity"]}\n  KCV: ${resultData["kcv"]}"
                    ThalesLogManager.logOperation("Key Lookup", inputs, resultString)
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "ThalesButtonAnimation") { loading ->
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
