package `in`.aicortex.iso8583studio.ui.screens.cipher

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


private object RsaValidationUtils {
    fun validateHex(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY)
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        return ValidationResult(ValidationState.VALID)
    }
}

// --- RSA CALCULATOR SCREEN ---

private object RsaLogManager {
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
        addLog( LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

private object RsaCryptoService {
    // This is a mock service. A real implementation would use a robust crypto library like BouncyCastle.
    fun generateKeys(length: Int): Map<String, String> {
        return mapOf(
            "modulus" to "BEEF".repeat(length / 16),
            "publicExp" to "010001",
            "privateExp" to "DEAD".repeat(length / 16)
        )
    }
}

private enum class RsaTabs(val title: String, val icon: ImageVector) {
    KEYS("Keys", Icons.Default.VpnKey),
    ENCRYPT("Encrypt", Icons.Default.Lock),
    DECRYPT("Decrypt", Icons.Default.LockOpen),
    SIGN("Sign", Icons.Default.Edit),
    VERIFY("Verify", Icons.Default.VerifiedUser),
    OAEP("OAEP", Icons.Default.EnhancedEncryption)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RsaCalculatorScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = RsaTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "RSA Calculator", onBackClick = onBack) },
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
                        label = "rsa_tab_transition"
                    ) { tab ->
                        when (tab) {
                            RsaTabs.KEYS -> KeysTab()
                            RsaTabs.ENCRYPT -> EncryptTab()
                            RsaTabs.DECRYPT -> DecryptTab()
                            RsaTabs.SIGN -> SignTab()
                            RsaTabs.VERIFY -> VerifyTab()
                            RsaTabs.OAEP -> OaepTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { RsaLogManager.clearLogs() },
                            logEntries = RsaLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeysTab() {
    var modulus by remember { mutableStateOf("") }
    var publicExp by remember { mutableStateOf("") }
    var privateExp by remember { mutableStateOf("") }
    var keyLength by remember { mutableStateOf("2048") }
    var isLoading by remember { mutableStateOf(false) }

    ModernCryptoCard(title = "RSA Key Pair", subtitle = "Generate or inspect an RSA key pair", icon = Icons.Default.VpnKey) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernDropdownField("Key Length (bits)", keyLength, listOf("1024", "2048", "3072", "4096"), onSelectionChanged = { keyLength = listOf("1024", "2048", "3072", "4096")[it] })
            ModernButton(
                text = "Generate Keys",
                onClick = {
                    isLoading = true
                    GlobalScope.launch {
                        delay(1000) // Simulate key generation
                        val keys = RsaCryptoService.generateKeys(keyLength.toInt())
                        modulus = keys["modulus"]!!
                        publicExp = keys["publicExp"]!!
                        privateExp = keys["privateExp"]!!
                        RsaLogManager.logOperation("Key Generation", mapOf("Key Length" to keyLength), "Keys generated successfully.")
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Autorenew
            )
            Divider(Modifier.padding(vertical = 8.dp))
            EnhancedTextField(value = modulus, onValueChange = { modulus = it }, label = "Modulus", maxLines = 5)
            EnhancedTextField(value = publicExp, onValueChange = { publicExp = it }, label = "Public Exponent (e)")
            EnhancedTextField(value = privateExp, onValueChange = { privateExp = it }, label = "Private Exponent (d)", maxLines = 5)
        }
    }
}

@Composable
private fun EncryptTab() {
    var data by remember { mutableStateOf("") }
    var encodingMethod by remember { mutableStateOf("Public") }
    var inputFormat by remember { mutableStateOf("ASCII") }
    var padding by remember { mutableStateOf("PKCS1") }
    val isFormValid = data.isNotBlank()

    ModernCryptoCard(title = "RSA Encryption", subtitle = "Encrypt data with a public or private key", icon = Icons.Default.Lock) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Encoding Method", encodingMethod, listOf("Public", "Private"), { encodingMethod = if(it==0) "Public" else "Private" }, Modifier.weight(1f))
                ModernDropdownField("Padding", padding, listOf("PKCS1", "No Padding"), { padding = if(it==0) "PKCS1" else "No Padding" }, Modifier.weight(1f))
            }
            ModernDropdownField("Input Data Format", inputFormat, listOf("ASCII", "Hexadecimal"), { inputFormat = if(it==0) "ASCII" else "Hexadecimal" })
            EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data to Encrypt", maxLines = 8)
            ModernButton(
                text = "Encrypt",
                onClick = { RsaLogManager.logOperation("Encrypt", mapOf("Data" to data), "Encrypted Data: ABC...") },
                enabled = isFormValid,
                icon = Icons.Default.EnhancedEncryption,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DecryptTab() {
    var data by remember { mutableStateOf("") }
    var decodingMethod by remember { mutableStateOf("Private") }
    var padding by remember { mutableStateOf("PKCS1") }
    val isFormValid = data.isNotBlank()

    ModernCryptoCard(title = "RSA Decryption", subtitle = "Decrypt data with a public or private key", icon = Icons.Default.LockOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Decoding Method", decodingMethod, listOf("Private", "Public"), { decodingMethod = if(it==0) "Private" else "Public" }, Modifier.weight(1f))
                ModernDropdownField("Padding", padding, listOf("PKCS1", "No Padding"), { padding = if(it==0) "PKCS1" else "No Padding" }, Modifier.weight(1f))
            }
            EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data to Decrypt (Hex)", maxLines = 8)
            ModernButton(
                text = "Decrypt",
                onClick = { RsaLogManager.logOperation("Decrypt", mapOf("Data" to data), "Decrypted Data: Hello World!") },
                enabled = isFormValid,
                icon = Icons.Default.VpnKeyOff,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SignTab() {
    var data by remember { mutableStateOf("") }
    var inputFormat by remember { mutableStateOf("ASCII") }
    val isFormValid = data.isNotBlank()

    ModernCryptoCard(title = "RSA Signature", subtitle = "Sign data using a private key", icon = Icons.Default.Edit) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernDropdownField("Input Data Format", inputFormat, listOf("ASCII", "Hexadecimal"), { inputFormat = if(it==0) "ASCII" else "Hexadecimal" })
            EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data to Sign", maxLines = 8)
            ModernButton(
                text = "Sign",
                onClick = { RsaLogManager.logOperation("Sign", mapOf("Data" to data), "Signature: XYZ...") },
                enabled = isFormValid,
                icon = Icons.Default.BorderColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VerifyTab() {
    var hash by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    val isFormValid = hash.isNotBlank() && signature.isNotBlank()

    ModernCryptoCard(title = "RSA Verification", subtitle = "Verify a signature against a hash", icon = Icons.Default.VerifiedUser) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = hash, onValueChange = { hash = it }, label = "Hash (Hex)", maxLines = 4)
            EnhancedTextField(value = signature, onValueChange = { signature = it }, label = "Signature (Hex)", maxLines = 8)
            ModernButton(
                text = "Verify",
                onClick = { RsaLogManager.logOperation("Verify", mapOf("Hash" to hash, "Signature" to signature), "Verification successful!") },
                enabled = isFormValid,
                icon = Icons.Default.Check,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OaepTab() {
    var data by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Encode") }
    var resultLength by remember { mutableStateOf("2048") }
    var hashFunction by remember { mutableStateOf("SHA-256") }
    val hashFunctions = listOf("SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512")
    val isFormValid = data.isNotBlank()

    ModernCryptoCard(title = "OAEP Padding", subtitle = "Optimal Asymmetric Encryption Padding", icon = Icons.Default.EnhancedEncryption) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Method", method, listOf("Encode", "Decode"), { method = if(it==0) "Encode" else "Decode" }, Modifier.weight(1f))
                ModernDropdownField("Hash Function", hashFunction, hashFunctions, { hashFunction = hashFunctions[it] }, Modifier.weight(1f))
            }
            ModernDropdownField("Result Length (bits)", resultLength, listOf("1024", "2048", "4096"), { resultLength = listOf("1024", "2048", "4096")[it] })
            EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data (Hex)")
            EnhancedTextField(value = label, onValueChange = { label = it }, label = "Encoding Parameters (Label, Hex)", validation = RsaValidationUtils.validateHex(label))
            ModernButton(
                text = method,
                onClick = { RsaLogManager.logOperation("OAEP $method", mapOf("Data" to data), "Result: ...") },
                enabled = isFormValid,
                icon = if(method == "Encode") Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult? = null) {
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "RsaButtonAnimation") { loading ->
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
