package `in`.aicortex.iso8583studio.ui.screens.cipher

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


private object ThalesRsaValidationUtils {
    fun validateHex(value: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "Field cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        return FieldValidation(ValidationState.VALID)
    }
    fun validateNumeric(value: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY)
        if (value.any { !it.isDigit() }) {
            return FieldValidation(ValidationState.ERROR, "Input must only contain digits (0-9).")
        }
        return FieldValidation(ValidationState.VALID)
    }
}

// --- THALES RSA SCREEN ---

private enum class ThalesRsaTabs(val title: String, val icon: ImageVector) {
    GENERATE("Generate", Icons.Default.Autorenew),
    KEY_BLOCK("Thales Key Block", Icons.Default.ViewModule),
    LMK_VARIANT("Thales LMK Variant", Icons.Default.VpnKey)
}

private object ThalesRsaLogManager {
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
            result?.let { append("\nResult:\n$it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

private object ThalesRsaCryptoService {
    // This is a mock service.
    fun generateRandomKey(length: Int): Map<String, String> {
        return mapOf(
            "d" to "PRIVATE_EXPONENT_".plus("D".repeat(length / 16)),
            "p" to "PRIME1_".plus("P".repeat(length / 32)),
            "q" to "PRIME2_".plus("Q".repeat(length / 32)),
            "dModP1" to "EXP1_".plus("1".repeat(length / 32)),
            "dModQ1" to "EXP2_".plus("2".repeat(length / 32)),
            "iqmp" to "COEFF_".plus("C".repeat(length / 32))
        )
    }

    fun generateDFromComponents(p: String, q: String): String {
        return "GENERATED_D_FROM_${p.take(4)}_${q.take(4)}"
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ThalesRsaScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = ThalesRsaTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "Thales RSA Calculator", onBackClick = onBack) },
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
                        label = "thales_rsa_tab_transition"
                    ) { tab ->
                        when (tab) {
                            ThalesRsaTabs.GENERATE -> GenerateTab()
                            ThalesRsaTabs.KEY_BLOCK -> KeyBlockTab()
                            ThalesRsaTabs.LMK_VARIANT -> LmkVariantTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { ThalesRsaLogManager.clearLogs() },
                            logEntries = ThalesRsaLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// --- TABS ---

@Composable
private fun GenerateTab() {
    var privateExp by remember { mutableStateOf("") }
    var prime1 by remember { mutableStateOf("") }
    var prime2 by remember { mutableStateOf("") }
    var exponent1 by remember { mutableStateOf("") }
    var exponent2 by remember { mutableStateOf("") }
    var coefficient by remember { mutableStateOf("") }
    var keyLength by remember { mutableStateOf("2048") }
    var isLoading by remember { mutableStateOf(false) }

    ModernCryptoCard(title = "Generate RSA Key Components", subtitle = "Generate a random key or derive 'd' from components", icon = Icons.Default.Autorenew) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = privateExp, onValueChange = { privateExp = it.uppercase() }, label = "Private Exp. (d)", validation = ThalesRsaValidationUtils.validateHex(privateExp), maxLines = 4)
            EnhancedTextField(value = prime1, onValueChange = { prime1 = it.uppercase() }, label = "Prime 1 (p)", validation = ThalesRsaValidationUtils.validateHex(prime1), maxLines = 4)
            EnhancedTextField(value = prime2, onValueChange = { prime2 = it.uppercase() }, label = "Prime 2 (q)", validation = ThalesRsaValidationUtils.validateHex(prime2), maxLines = 4)
            EnhancedTextField(value = exponent1, onValueChange = { exponent1 = it.uppercase() }, label = "Exponent 1 (dModP1)", validation = ThalesRsaValidationUtils.validateHex(exponent1), maxLines = 4)
            EnhancedTextField(value = exponent2, onValueChange = { exponent2 = it.uppercase() }, label = "Exponent 2 (dModQ1)", validation = ThalesRsaValidationUtils.validateHex(exponent2), maxLines = 4)
            EnhancedTextField(value = coefficient, onValueChange = { coefficient = it.uppercase() }, label = "Coefficient (iqmp)", validation = ThalesRsaValidationUtils.validateHex(coefficient), maxLines = 4)

            Divider(Modifier.padding(vertical=8.dp))

            EnhancedTextField(value = keyLength, onValueChange = { keyLength = it }, label = "Key Length (1-4096)", validation = ThalesRsaValidationUtils.validateNumeric(keyLength))

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(
                    text = "Generate (d) from Components",
                    onClick = {
                        val inputs = mapOf("Prime 1" to prime1, "Prime 2" to prime2)
                        privateExp = ThalesRsaCryptoService.generateDFromComponents(prime1, prime2)
                        ThalesRsaLogManager.logOperation("Generate 'd'", inputs, "Private Exponent (d): $privateExp")
                    },
                    enabled = prime1.isNotBlank() && prime2.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
                ModernButton(
                    text = "Generate New Random Key",
                    onClick = {
                        isLoading = true
                        GlobalScope.launch {
                            delay(1000)
                            val length = keyLength.toIntOrNull() ?: 2048
                            val keys = ThalesRsaCryptoService.generateRandomKey(length)
                            privateExp = keys["d"]!!
                            prime1 = keys["p"]!!
                            prime2 = keys["q"]!!
                            exponent1 = keys["dModP1"]!!
                            exponent2 = keys["dModQ1"]!!
                            coefficient = keys["iqmp"]!!
                            ThalesRsaLogManager.logOperation("Generate Random Key", mapOf("Key Length" to keyLength), "New key components generated.")
                            isLoading = false
                        }
                    },
                    isLoading = isLoading,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun KeyBlockTab() {
    var desKbpk by remember { mutableStateOf("") }
    var aesKbpk by remember { mutableStateOf("") }
    var publicKeyHeader by remember { mutableStateOf("") }
    var privateKeyHeader by remember { mutableStateOf("") }
    var preferredEncryption by remember { mutableStateOf("AES") }
    var inputFormat by remember { mutableStateOf("Hexadecimal") }
    var publicKey by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }

    ModernCryptoCard(title = "Thales Key Block", subtitle = "Wrap or unwrap a Thales key block", icon = Icons.Default.ViewModule) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = desKbpk, onValueChange = { desKbpk = it.uppercase() }, label = "DES KBPK", validation = ThalesRsaValidationUtils.validateHex(desKbpk))
            EnhancedTextField(value = aesKbpk, onValueChange = { aesKbpk = it.uppercase() }, label = "AES KBPK", validation = ThalesRsaValidationUtils.validateHex(aesKbpk))
            EnhancedTextField(value = publicKeyHeader, onValueChange = { publicKeyHeader = it.uppercase() }, label = "Public Key Header", validation = ThalesRsaValidationUtils.validateHex(publicKeyHeader))
            EnhancedTextField(value = privateKeyHeader, onValueChange = { privateKeyHeader = it.uppercase() }, label = "Private Key Header", validation = ThalesRsaValidationUtils.validateHex(privateKeyHeader))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Key Block Encryption", preferredEncryption, listOf("AES", "DES"), { preferredEncryption = if(it==0) "AES" else "DES" }, Modifier.weight(1f))
                ModernDropdownField("Input Format", inputFormat, listOf("ASCII", "Hexadecimal"), { inputFormat = if(it==0) "ASCII" else "Hexadecimal" }, Modifier.weight(1f))
            }
            EnhancedTextField(value = publicKey, onValueChange = { publicKey = it }, label = "Public Key", maxLines = 4)
            EnhancedTextField(value = privateKey, onValueChange = { privateKey = it }, label = "Private Key", maxLines = 4)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(text = "Wrap Key Block", onClick = { /* TODO */}, modifier = Modifier.weight(1f))
                ModernButton(text = "Unwrap Key Block", onClick = { /* TODO */}, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LmkVariantTab() {
    val modulusEncodings = remember { listOf(
        "DEC enc. for ASN.1 Public Key (INTEGER using unsigned representation)",
        "DER ec. for ASN.1 Public Key (Integer using 2's complement representation)"
    ) }
    var lmk3435 by remember { mutableStateOf("") }
    var lmk3637 by remember { mutableStateOf("") }
    var authData by remember { mutableStateOf("") }
    var selectedModEncoding by remember { mutableStateOf(modulusEncodings.first()) }
    var publicKey by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }

    ModernCryptoCard(title = "Thales LMK Variant", subtitle = "Process Thales LMK key variants", icon = Icons.Default.VpnKey) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = lmk3435, onValueChange = { lmk3435 = it.uppercase() }, label = "LMK Pair 34-35", validation = ThalesRsaValidationUtils.validateHex(lmk3435))
            EnhancedTextField(value = lmk3637, onValueChange = { lmk3637 = it.uppercase() }, label = "LMK Pair 36-37", validation = ThalesRsaValidationUtils.validateHex(lmk3637))
            EnhancedTextField(value = authData, onValueChange = { authData = it }, label = "Authentication Data", validation = ThalesRsaValidationUtils.validateHex(authData))
            ModernDropdownField("Modulus Encoding", selectedModEncoding, modulusEncodings, { selectedModEncoding = modulusEncodings[it] })
            EnhancedTextField(value = publicKey, onValueChange = { publicKey = it }, label = "Public Key", maxLines = 4)
            EnhancedTextField(value = privateKey, onValueChange = { privateKey = it }, label = "Private Key", maxLines = 4)
            Spacer(Modifier.height(8.dp))
            ModernButton(text = "Process LMK Variant", onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth())
        }
    }
}


// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: FieldValidation? = null) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), maxLines = maxLines,
            isError = validation?.state == ValidationState.ERROR,
        )
        if (validation?.message?.isNotEmpty() == true) {
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "ThalesRsaButtonAnimation") { loading ->
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
