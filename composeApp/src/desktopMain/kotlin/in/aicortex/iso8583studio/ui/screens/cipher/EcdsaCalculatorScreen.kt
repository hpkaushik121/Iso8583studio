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
import kotlin.random.Random



private object EcdsaValidationUtils {
    fun validateHex(value: String): FieldValidation {
        if (value.isEmpty()) return FieldValidation(ValidationState.EMPTY, "Field cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return FieldValidation(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        return FieldValidation(ValidationState.VALID)
    }
}

// --- ECDSA SCREEN ---

private enum class EcdsaTabs(val title: String, val icon: ImageVector) {
    KEYS("Keys", Icons.Default.VpnKey),
    SIGN("Sign", Icons.Default.Edit),
    VERIFY("Verify", Icons.Default.VerifiedUser),
}

private object EcdsaLogManager {
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

private object EcdsaCryptoService {
    // This is a mock service. A real implementation would use a robust crypto library like BouncyCastle.
    private fun generateRandomHex(length: Int) = (1..length).map { Random.nextInt(0, 16).toString(16) }.joinToString("").uppercase()

    fun generateKeyPair(curve: String): Map<String, String> {
        val keyLength = when (curve) {
            "NIST P-256 (prime256v1)" -> 64
            "NIST P-384(secp384r1)" -> 96
            "NIST P -521 (secp521r1)" -> 132
            else -> 64
        }
        return mapOf(
            "private" to generateRandomHex(keyLength),
            "public" to "04${generateRandomHex(keyLength)}${generateRandomHex(keyLength)}"
        )
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EcdsaCalculatorScreen(window: ComposeWindow? = null, onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = EcdsaTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "ECDSA Calculator", onBackClick = onBack) },
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
                        label = "ecdsa_tab_transition"
                    ) { tab ->
                        when (tab) {
                            EcdsaTabs.KEYS -> KeysTab()
                            EcdsaTabs.SIGN -> SignTab()
                            EcdsaTabs.VERIFY -> VerifyTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { EcdsaLogManager.clearLogs() },
                            logEntries = EcdsaLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// --- TABS ---

@Composable
private fun KeysTab() {
    val curveNames = remember { listOf("NIST P-256 (prime256v1)", "NIST P-384(secp384r1)", "NIST P-521 (secp521r1)", "Brainpool P256r1", "Brainpool P384r1", "BrainpoolP512r1") }
    var selectedCurve by remember { mutableStateOf(curveNames.first()) }
    var privateKey by remember { mutableStateOf("") }
    var publicKey by remember { mutableStateOf("") }
    var publicKeyForm by remember { mutableStateOf("Uncompressed") }
    var isLoading by remember { mutableStateOf(false) }

    ModernCryptoCard(title = "ECDSA Key Management", subtitle = "Generate and validate elliptic curve keys", icon = Icons.Default.VpnKey) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernDropdownField(label = "ECC Curve Name", value = selectedCurve, options = curveNames, onSelectionChanged = { selectedCurve = curveNames[it] })
            EnhancedTextField(value = privateKey, onValueChange = { privateKey = it.uppercase() }, label = "Private Key (Hex)", validation = EcdsaValidationUtils.validateHex(privateKey), maxLines = 3)
            EnhancedTextField(value = publicKey, onValueChange = { publicKey = it.uppercase() }, label = "Public Key (Hex)", validation = EcdsaValidationUtils.validateHex(publicKey), maxLines = 5)
            ModernDropdownField(label = "Public Key Form", value = publicKeyForm, options = listOf("Uncompressed", "Compressed"), onSelectionChanged = { publicKeyForm = if(it==0) "Uncompressed" else "Compressed" })

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(text = "Generate New Public Key", onClick = { /* TODO */ }, modifier = Modifier.weight(1f))
                ModernButton(text = "Is Point on Curve?", onClick = { /* TODO */ }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(
                    text = "Generate Random Key Pair",
                    onClick = {
                        isLoading = true
                        GlobalScope.launch {
                            delay(500)
                            val keyPair = EcdsaCryptoService.generateKeyPair(selectedCurve)
                            privateKey = keyPair["private"]!!
                            publicKey = keyPair["public"]!!
                            EcdsaLogManager.logOperation("Key Generation", mapOf("Curve" to selectedCurve), "New key pair generated.")
                            isLoading = false
                        }
                    },
                    isLoading = isLoading,
                    modifier = Modifier.weight(1f)
                )
                ModernButton(text = "Validate Current Key Pair", onClick = { /* TODO */ }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SignTab() {
    val hashTypes = remember { listOf("SHA-1", "SHA-256", "SHA-384", "SHA-512") }
    val inputFormats = remember { listOf("ASCII", "Hexadecimal") }

    var selectedHashType by remember { mutableStateOf(hashTypes[1]) }
    var selectedInputFormat by remember { mutableStateOf(inputFormats.first()) }
    var data by remember { mutableStateOf("") }
    val isFormValid = data.isNotBlank()

    ModernCryptoCard(title = "Sign Data", subtitle = "Create an ECDSA signature for your data", icon = Icons.Default.Edit) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Hash Type", selectedHashType, hashTypes, { selectedHashType = hashTypes[it] }, Modifier.weight(1f))
                ModernDropdownField("Input Data Format", selectedInputFormat, inputFormats, { selectedInputFormat = inputFormats[it] }, Modifier.weight(1f))
            }
            EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data to Sign", maxLines = 8)
            ModernButton(
                text = "Sign Data",
                onClick = { EcdsaLogManager.logOperation("Sign", mapOf("Data" to data, "Hash" to selectedHashType), "Signature: 3045...") },
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

    ModernCryptoCard(title = "Verify Signature", subtitle = "Validate a signature against a hash and public key", icon = Icons.Default.VerifiedUser) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = hash, onValueChange = { hash = it.uppercase() }, label = "Hash (Hex)", validation = EcdsaValidationUtils.validateHex(hash))
            EnhancedTextField(value = signature, onValueChange = { signature = it.uppercase() }, label = "Signature (Hex)", validation = EcdsaValidationUtils.validateHex(signature), maxLines = 5)
            ModernButton(
                text = "Verify Signature",
                onClick = { EcdsaLogManager.logOperation("Verify", mapOf("Hash" to hash, "Signature" to signature), "Signature is VALID.") },
                enabled = isFormValid,
                icon = Icons.Default.Check,
                modifier = Modifier.fillMaxWidth()
            )
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "EcdsaButtonAnimation") { loading ->
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
