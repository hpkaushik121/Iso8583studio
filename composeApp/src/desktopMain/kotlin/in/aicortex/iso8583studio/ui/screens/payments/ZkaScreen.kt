package `in`.aicortex.iso8583studio.ui.screens.payments

import ai.cortex.core.crypto.data.FieldValidation
import ai.cortex.core.crypto.data.ValidationState
import androidx.compose.animation.*
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

object ZKAValidationUtils {
    fun validateHex(value: String, fieldName: String, expectedLength: Int? = null): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "$fieldName cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
        }
        if (value.length % 2 != 0) {
            return FieldValidation(ValidationState.ERROR, "$fieldName must have an even number of characters.")
        }
        expectedLength?.let {
            if (value.length != it) return FieldValidation(ValidationState.ERROR, "$fieldName must be $it characters long.")
        }
        return FieldValidation(ValidationState.VALID)
    }
}


// --- ZKA SCREEN ---

object ZKALogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() { _logEntries.clear() }

    private fun addLog(entry: LogEntry) {
        _logEntries.add(0, entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, _logEntries.size)
    }

    fun logOperation(operation: String, inputs: Map<String, String>, result: String? = null, error: String? = null, executionTime: Long = 0L) {
        if (result == null && error == null) return
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) -> if (value.isNotBlank()) append("  $key: $value\n") }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }
        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

object ZKAService {
    // NOTE: This is a placeholder for actual ZKA cryptographic logic.
    private fun String.decodeHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    fun deriveSessionKey(macKey: String, data: String): String {
        // Mock logic: XOR key and data
        val keyBytes = macKey.decodeHex()
        val dataBytes = data.decodeHex()
        val sk = ByteArray(keyBytes.size)
        for (i in sk.indices) {
            sk[i] = (keyBytes[i].toInt() xor dataBytes[i % dataBytes.size].toInt()).toByte()
        }
        return sk.toHex().uppercase()
    }

    fun processPinBlock(skPac: String, pinBlock: String): String {
        // Mock logic: XOR SK with PIN block
        val skBytes = skPac.decodeHex()
        val pinBlockBytes = pinBlock.decodeHex()
        val result = ByteArray(pinBlockBytes.size)
        for (i in result.indices) {
            result[i] = (skBytes[i % skBytes.size].toInt() xor pinBlockBytes[i].toInt()).toByte()
        }
        return result.toHex().uppercase()
    }

    fun generateMac(macKey: String, data: String): String {
        val combined = "$macKey|$data"
        return combined.hashCode().toString(16).take(16).uppercase()
    }
}

enum class ZKATabs(val title: String, val icon: ImageVector) {
    SK_DERIVATION("SK Derivation", Icons.Default.VpnKey),
    PIN("PIN", Icons.Default.Pin),
    MAC("MAC", Icons.Default.VerifiedUser),
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ZKAScreen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = ZKATabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "ZKA (Zone Key Admin)", onBackClick = onBack) },
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
                        label = "zka_tab_transition"
                    ) { tab ->
                        when (tab) {
                            ZKATabs.SK_DERIVATION -> SKDerivationCard()
                            ZKATabs.PIN -> PINCard()
                            ZKATabs.MAC -> MACCard()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { ZKALogManager.clearLogs() },
                            logEntries = ZKALogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SKDerivationCard() {
    var macKey by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val macKeyValidation = ZKAValidationUtils.validateHex(macKey, "MAC Key", 32)
    val dataValidation = ZKAValidationUtils.validateHex(data, "Data")
    val isFormValid = macKeyValidation.isValid() && dataValidation.isValid()

    ModernCryptoCard(title = "Session Key Derivation", subtitle = "Derive an SK from a MAC Key", icon = ZKATabs.SK_DERIVATION.icon) {
        EnhancedTextField(macKey, { macKey = it }, "MAC Key (32 Hex Chars)", validation = macKeyValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(data, { data = it }, "Data (Hex)", validation = dataValidation)
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Derive SK",
            onClick = {
                if(isFormValid) {
                    isLoading = true
                    val inputs = mapOf("MAC Key" to macKey, "Data" to data)
                    GlobalScope.launch {
                        delay(150)
                        val result = ZKAService.deriveSessionKey(macKey, data)
                        ZKALogManager.logOperation("SK Derivation", inputs, result = "Derived SK: $result", executionTime = 155)
                        isLoading = false
                    }
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.ArrowForward, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PINCard() {
    var skPac by remember { mutableStateOf("") }
    var pinBlock by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val skPacValidation = ZKAValidationUtils.validateHex(skPac, "SK-pac", 32)
    val pinBlockValidation = ZKAValidationUtils.validateHex(pinBlock, "PIN Block", 16)
    val isFormValid = skPacValidation.isValid() && pinBlockValidation.isValid()

    ModernCryptoCard(title = "ZKA PIN Operations", subtitle = "Encode or Decode a PIN block with SK", icon = ZKATabs.PIN.icon) {
        EnhancedTextField(skPac, { skPac = it }, "SK-pac (32 Hex Chars)", validation = skPacValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(pinBlock, { pinBlock = it }, "PIN Block (16 Hex Chars)", validation = pinBlockValidation)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernButton(
                text = "Encode",
                onClick = {
                    if(isFormValid) {
                        isLoading = true
                        val inputs = mapOf("SK-pac" to skPac, "PIN Block" to pinBlock)
                        GlobalScope.launch {
                            delay(150)
                            val result = ZKAService.processPinBlock(skPac, pinBlock)
                            ZKALogManager.logOperation("PIN Encode", inputs, "Encoded PIN Block: $result", "155")
                            isLoading = false
                        }
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Lock, modifier = Modifier.weight(1f)
            )
            ModernButton(
                text = "Decode",
                onClick = {
                    if(isFormValid) {
                        isLoading = true
                        val inputs = mapOf("SK-pac" to skPac, "Encoded PIN Block" to pinBlock)
                        GlobalScope.launch {
                            delay(150)
                            val result = ZKAService.processPinBlock(skPac, pinBlock)
                            ZKALogManager.logOperation("PIN Decode", inputs, "Decoded PIN Block: $result", "155")
                            isLoading = false
                        }
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.LockOpen, modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MACCard() {
    var macKey by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val macKeyValidation = ZKAValidationUtils.validateHex(macKey, "MAC Key", 32)
    val dataValidation = ZKAValidationUtils.validateHex(data, "Data")
    val isFormValid = macKeyValidation.isValid() && dataValidation.isValid()

    ModernCryptoCard(title = "ZKA MAC Generation", subtitle = "Generate a MAC with a ZKA MAC Key", icon = ZKATabs.MAC.icon) {
        EnhancedTextField(macKey, { macKey = it }, "MAC Key (32 Hex Chars)", validation = macKeyValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(data, { data = it }, "Data (Hex)", validation = dataValidation)
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Generate MAC",
            onClick = {
                if(isFormValid) {
                    isLoading = true
                    val inputs = mapOf("MAC Key" to macKey, "Data" to data)
                    GlobalScope.launch {
                        delay(150)
                        val result = ZKAService.generateMac(macKey, data)
                        ZKALogManager.logOperation("MAC Generation", inputs, "Generated MAC: $result", "155")
                        isLoading = false
                    }
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.CheckCircle, modifier = Modifier.fillMaxWidth()
        )
    }
}


// --- SHARED UI COMPONENTS ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, validation: FieldValidation) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
            isError = validation.state == ValidationState.ERROR,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = when (validation.state) { ValidationState.VALID -> MaterialTheme.colors.primary; ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f); else -> MaterialTheme.colors.error },
                unfocusedBorderColor = when (validation.state) { ValidationState.VALID -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f); ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f); else -> MaterialTheme.colors.error }
            )
        )
        if (validation.message.isNotEmpty()) {
            Text(validation.message, color = if (validation.state == ValidationState.ERROR) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.caption, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

@Composable
private fun ModernCryptoCard(title: String, subtitle: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }) { loading ->
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon?.let { Icon(it, null); Spacer(Modifier.width(8.dp)) }
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
