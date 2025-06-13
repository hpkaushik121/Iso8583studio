package `in`.aicortex.iso8583studio.ui.screens.keys

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
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random



private object KeyshareValidationUtils {
    fun validateHex(value: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY)
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        if (value.length % 2 != 0) {
            return FieldValidation(ValidationState.ERROR, "Hex string must have an even number of characters.")
        }
        return FieldValidation(ValidationState.VALID)
    }
}

// --- KEYSHARE SCREEN ---

private enum class KeyshareTabs(val title: String, val icon: ImageVector) {
    INSECURE("Insecure", Icons.Default.LockOpen),
    SECURE("Secure", Icons.Default.Lock),
}

private object KeyshareLogManager {
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
            append("Generated Components:\n")
            inputs.forEach { (key, value) ->
                val displayValue = if (value.length > 32) "${value.take(16)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n$it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type= logType, message = message, details = details))
    }
}

private object KeyshareCryptoService {
    private fun generateRandomHex(length: Int) = (1..length).map { Random.nextInt(0, 16).toString(16) }.joinToString("").uppercase()
    private fun generateRandomPin() = (1..4).map { Random.nextInt(0, 10) }.joinToString("")

    private fun combineKeys(keys: List<String>): Pair<String, String> {
        val validKeys = keys.filter { it.isNotBlank() }
        if (validKeys.isEmpty()) return "ERROR: No keys to combine" to "N/A"

        try {
            val maxLength = validKeys.maxOf { it.length }
            val paddedKeys = validKeys.map { it.padEnd(maxLength, '0') }
            var result = "0".repeat(maxLength).toBigInteger(16)
            paddedKeys.forEach {
                result = result.xor(it.toBigInteger(16))
            }
            val combinedKey = result.toString(16).uppercase().padStart(maxLength, '0')
            val kcv = generateRandomHex(6)
            return combinedKey to kcv
        } catch (e: Exception) {
            return "ERROR: Invalid hex in components" to "N/A"
        }
    }

    fun generateAndCombine(partCount: Int, keyType: String, isSecure: Boolean): Map<String, Any> {
        val keyLengthHex = when (keyType) {
            "DES/TDES" -> 32 // 16 bytes for double-length TDES
            "AES" -> 64      // Assuming AES-256
            else -> 32
        }

        val generatedParts = List(partCount) { generateRandomHex(keyLengthHex) }
        val generatedPins = if (isSecure) List(partCount) { generateRandomPin() } else emptyList()

        val (combinedKey, kcv) = combineKeys(generatedParts)

        return mapOf(
            "parts" to generatedParts,
            "pins" to generatedPins,
            "combinedKey" to combinedKey,
            "kcv" to kcv
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun KeyshareGeneratorScreen(window: ComposeWindow? = null, onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = KeyshareTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "Keyshare Generator", onBackClick = onBack) },
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
                        label = "keyshare_main_tab_transition"
                    ) { tab ->
                        KeyshareTabContent(isSecure = tab == KeyshareTabs.SECURE)
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { KeyshareLogManager.clearLogs() },
                            logEntries = KeyshareLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyshareTabContent(isSecure: Boolean) {
    var parity by remember { mutableStateOf("Ignore") }
    var keyType by remember { mutableStateOf("DES/TDES") }
    var innerSelectedTabIndex by remember { mutableStateOf(0) }
    val innerTabTitles = listOf("2 Parts", "3 Parts")

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernCryptoCard(title = "Global Options", subtitle = "Define settings for this operation", icon = Icons.Default.Tune) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Parity", parity, listOf("Ignore", "ForceOdd"), { parity = if (it == 0) "Ignore" else "ForceOdd" }, Modifier.weight(1f))
                ModernDropdownField("Key Type", keyType, listOf("DES/TDES", "AES"), { keyType = if (it == 0) "DES/TDES" else "AES" }, Modifier.weight(1f))
            }
        }

        TabRow(
            selectedTabIndex = innerSelectedTabIndex,
            backgroundColor = MaterialTheme.colors.surface.copy(alpha=0.5f),
            contentColor = MaterialTheme.colors.primary,
        ) {
            innerTabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = innerSelectedTabIndex == index,
                    onClick = { innerSelectedTabIndex = index },
                    text = { Text(title, fontWeight = if(innerSelectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        if (innerSelectedTabIndex == 0) {
            KeyCombinationCard(partCount = 2, isSecure = isSecure, keyType = keyType)
        } else {
            KeyCombinationCard(partCount = 3, isSecure = isSecure, keyType = keyType)
        }
    }
}

@Composable
private fun KeyCombinationCard(partCount: Int, isSecure: Boolean, keyType: String) {
    val keyParts = remember { mutableStateListOf<String>().apply { repeat(partCount) { add("") } } }
    val pinParts = remember { mutableStateListOf<String>().apply { repeat(partCount) { add("") } } }
    var combinedKey by remember { mutableStateOf("") }
    var kcv by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    ModernCryptoCard(title = "$partCount-Part Key Generation", subtitle = "Generate and combine $partCount key shares", icon = Icons.Default.CallMerge) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            keyParts.forEachIndexed { index, s ->
                KeyPartRow(
                    label = "Part ${index + 1}",
                    value = keyParts[index],
                    onValueChange = { keyParts[index] = it.uppercase() },
                    pin = pinParts[index],
                    onPinChange = { pinParts[index] = it },
                    isSecure = isSecure
                )
            }
            Divider(Modifier.padding(vertical = 8.dp))
            KeyResultRow("Combined Key", combinedKey, kcv)
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Generate $partCount Parts",
                onClick = {
                    isLoading = true
                    GlobalScope.launch {
                        delay(200) // Simulate generation
                        val results = KeyshareCryptoService.generateAndCombine(partCount, keyType, isSecure)

                        val newParts = results["parts"] as List<String>
                        val newPins = results["pins"] as List<String>

                        // Update the state lists
                        newParts.forEachIndexed { index, part -> keyParts[index] = part }
                        if (isSecure) {
                            newPins.forEachIndexed { index, pin -> pinParts[index] = pin }
                        }

                        combinedKey = results["combinedKey"] as String
                        kcv = results["kcv"] as String

                        val inputs = newParts.mapIndexed { index, key -> "Part ${index + 1}" to key }.toMap().toMutableMap()
                        if (isSecure) {
                            newPins.forEachIndexed { index, pin -> inputs["PIN ${index + 1}"] = pin }
                        }
                        val resultString = "Combined Key: $combinedKey\nKCV: $kcv"
                        KeyshareLogManager.logOperation("Generate & Combine", inputs, resultString)

                        isLoading = false
                    }
                },
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun KeyPartRow(label: String, value: String, onValueChange: (String) -> Unit, pin: String, onPinChange: (String) -> Unit, isSecure: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EnhancedTextField(value = value, onValueChange = onValueChange, label = label, modifier = Modifier.weight(if (isSecure) 2f else 1f))
        if (isSecure) {
            EnhancedTextField(value = pin, onValueChange = onPinChange, label = "PIN", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun KeyResultRow(label: String, combinedValue: String, kcvValue: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = combinedValue, onValueChange = {}, label = { Text(label) }, readOnly = true, modifier = Modifier.weight(2f))
        OutlinedTextField(value = kcvValue, onValueChange = {}, label = { Text("KCV") }, readOnly = true, modifier = Modifier.weight(1f))
    }
}


// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier)
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "KeyshareButtonAnimation") { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    icon?.let {
                        Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(18.dp), tint = LocalContentColor.current)
                    }
                    Spacer(Modifier.width(8.dp))
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
