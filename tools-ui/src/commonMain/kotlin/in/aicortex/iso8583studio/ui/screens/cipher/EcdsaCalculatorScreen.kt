package `in`.aicortex.iso8583studio.ui.screens.cipher

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import kotlinx.coroutines.withContext

import kotlinx.coroutines.Dispatchers

import androidx.compose.runtime.rememberCoroutineScope

import io.cryptocalc.crypto.primitives.Digests

import io.cryptocalc.ec.EcPoint

import io.cryptocalc.ec.EcKeyPair

import io.cryptocalc.ec.EcCurve

import io.cryptocalc.ec.Ec

import io.cryptocalc.math.BigInt

import ai.cortex.core.toHexUpper

import ai.cortex.core.hexToBytes

import `in`.aicortex.iso8583studio.platform.nowLogTimestamp

import `in`.aicortex.iso8583studio.analytics.Analytics
import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PersistentTabContent
import `in`.aicortex.iso8583studio.ui.screens.components.LogPanelWithAutoScroll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random



private object EcdsaValidationUtils {
    fun validateHex(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "Field cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        return ValidationResult(ValidationState.VALID)
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
        _logEntries.add(entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, _logEntries.size)
    }

    fun logOperation(operation: String, inputs: Map<String, String>, result: String? = null, error: String? = null, executionTime: Long = 0L) {
        if (result == null && error == null) return
        Analytics.calculationRun(operation, success = result != null, durationMs = executionTime)

        val timestamp = nowLogTimestamp()
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



@Composable
fun EcdsaCalculatorScreen( onBack: () -> Unit) {
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
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
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


/** Curve and key pair shared by the three tabs. */
private object EcKeyState {
    val curveNames = listOf("NIST P-256 (prime256v1)", "NIST P-384 (secp384r1)", "NIST P-521 (secp521r1)", "secp256k1", "Brainpool P256r1", "Brainpool P384r1", "Brainpool P512r1")
    var selectedCurve by mutableStateOf(curveNames.first())
    var privateKey by mutableStateOf("")
    var publicKey by mutableStateOf("")
    fun curve(): EcCurve = EcCurve.byName(selectedCurve)
    fun keyPair(): EcKeyPair {
        require(privateKey.isNotBlank()) { "Enter or generate a private key on the Keys tab first" }
        return Ec.keyPairFromPrivate(curve(), BigInt.fromHex(privateKey))
    }
    fun publicPoint(): EcPoint {
        require(publicKey.isNotBlank()) { "Enter or generate a public key on the Keys tab first" }
        return Ec.decodePoint(curve(), publicKey.hexToBytes())
    }
}

@Composable
private fun KeysTab() {
    var publicKeyForm by remember { mutableStateOf("Uncompressed") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun log(op: String, result: String? = null, error: String? = null) =
        EcdsaLogManager.logOperation(op, mapOf("Curve" to EcKeyState.selectedCurve, "Private Key" to EcKeyState.privateKey, "Public Key" to EcKeyState.publicKey), result, error)
    ModernCryptoCard(title = "ECDSA Key Management", subtitle = "Generate and validate elliptic curve keys", icon = Icons.Default.VpnKey) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernDropdownField(label = "ECC Curve Name", value = EcKeyState.selectedCurve, options = EcKeyState.curveNames, onSelectionChanged = { EcKeyState.selectedCurve = EcKeyState.curveNames[it] })
            EnhancedTextField(value = EcKeyState.privateKey, onValueChange = { EcKeyState.privateKey = it.uppercase() }, label = "Private Key (Hex)", validation = EcdsaValidationUtils.validateHex(EcKeyState.privateKey), maxLines = 3)
            EnhancedTextField(value = EcKeyState.publicKey, onValueChange = { EcKeyState.publicKey = it.uppercase() }, label = "Public Key (Hex, SEC1)", validation = EcdsaValidationUtils.validateHex(EcKeyState.publicKey), maxLines = 5)
            ModernDropdownField(label = "Public Key Form", value = publicKeyForm, options = listOf("Uncompressed", "Compressed"), onSelectionChanged = { publicKeyForm = if(it==0) "Uncompressed" else "Compressed" })
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(text = "Derive Public Key", onClick = {
                    try {
                        val kp = EcKeyState.keyPair()
                        EcKeyState.publicKey = Ec.encodePoint(kp.curve, kp.q, publicKeyForm == "Compressed").toHexUpper()
                        log("Derive Public Key", "Public key: ${EcKeyState.publicKey}")
                    } catch (e: Exception) { log("Derive Public Key", error = e.message) }
                }, modifier = Modifier.weight(1f))
                ModernButton(text = "Is Point on Curve?", onClick = {
                    try {
                        val on = Ec.isOnCurve(EcKeyState.curve(), EcKeyState.publicPoint())
                        log("Point Check", if (on) "The public key is a valid point on ${EcKeyState.curve().name}" else "The point is NOT on the curve")
                    } catch (e: Exception) { log("Point Check", error = e.message) }
                }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(
                    text = "Generate Random Key Pair",
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val kp = withContext(Dispatchers.Default) { Ec.generateKeyPair(EcKeyState.curve()) }
                                EcKeyState.privateKey = BigInt.toHex(kp.d, kp.curve.byteLength * 2)
                                EcKeyState.publicKey = Ec.encodePoint(kp.curve, kp.q, publicKeyForm == "Compressed").toHexUpper()
                                log("Key Generation", "New key pair generated on ${kp.curve.name}")
                            } catch (e: Exception) { log("Key Generation", error = e.message) }
                            isLoading = false
                        }
                    },
                    isLoading = isLoading,
                    modifier = Modifier.weight(1f)
                )
                ModernButton(text = "Validate Current Key Pair", onClick = {
                    try {
                        val kp = EcKeyState.keyPair()
                        val q = EcKeyState.publicPoint()
                        log("Key Pair Validation", if (q == kp.q) "Public key matches the private key" else "Public key does NOT correspond to the private key")
                    } catch (e: Exception) { log("Key Pair Validation", error = e.message) }
                }, modifier = Modifier.weight(1f))
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
    ModernCryptoCard(title = "Sign Data", subtitle = "ECDSA with a deterministic nonce (RFC 6979); DER and raw r‖s output", icon = Icons.Default.Edit) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Hash Type", selectedHashType, hashTypes, { selectedHashType = hashTypes[it] }, Modifier.weight(1f))
                ModernDropdownField("Input Data Format", selectedInputFormat, inputFormats, { selectedInputFormat = inputFormats[it] }, Modifier.weight(1f))
            }
            EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data to Sign", maxLines = 8)
            ModernButton(
                text = "Sign Data",
                onClick = {
                    val inputs = mapOf("Data" to data, "Hash" to selectedHashType, "Curve" to EcKeyState.selectedCurve)
                    try {
                        val bytes = if (selectedInputFormat == "Hexadecimal") data.hexToBytes() else data.encodeToByteArray()
                        val kp = EcKeyState.keyPair()
                        val digest = Digests.hash(selectedHashType, bytes)
                        val sig = Ec.signDigest(kp, digest)
                        EcdsaLogManager.logOperation("Sign", inputs, "Hash: ${digest.toHexUpper()}\nSignature (DER): ${sig.toDer().toHexUpper()}\nr: ${BigInt.toHex(sig.r)}\ns: ${BigInt.toHex(sig.s)}")
                    } catch (e: Exception) {
                        EcdsaLogManager.logOperation("Sign", inputs, error = e.message)
                    }
                },
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
    ModernCryptoCard(title = "Verify Signature", subtitle = "Validate a DER or raw signature against a hash and the public key", icon = Icons.Default.VerifiedUser) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = hash, onValueChange = { hash = it.uppercase() }, label = "Hash (Hex)", validation = EcdsaValidationUtils.validateHex(hash))
            EnhancedTextField(value = signature, onValueChange = { signature = it.uppercase() }, label = "Signature (Hex, DER or r‖s)", validation = EcdsaValidationUtils.validateHex(signature), maxLines = 5)
            ModernButton(
                text = "Verify Signature",
                onClick = {
                    val inputs = mapOf("Hash" to hash, "Signature" to signature, "Curve" to EcKeyState.selectedCurve)
                    try {
                        val curve = EcKeyState.curve()
                        val q = EcKeyState.publicPoint()
                        val sigBytes = signature.hexToBytes()
                        val sig = if (sigBytes.isNotEmpty() && sigBytes[0] == 0x30.toByte()) Ec.Signature.fromDer(sigBytes) else Ec.Signature.fromRaw(curve, sigBytes)
                        val ok = Ec.verifyDigest(curve, q, hash.hexToBytes(), sig)
                        EcdsaLogManager.logOperation("Verify", inputs, if (ok) "Signature is VALID." else "Signature is INVALID.")
                    } catch (e: Exception) {
                        EcdsaLogManager.logOperation("Verify", inputs, error = e.message)
                    }
                },
                enabled = isFormValid,
                icon = Icons.Default.Check,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult? = null) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.TextField(value, onValueChange, label, modifier, maxLines, validation, ""); return }
    Column(modifier = modifier) {
        FixedOutlinedTextField(
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
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Card(title, subtitle, icon, onInfoClick, content); return }
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
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Dropdown(label, value, options, onSelectionChanged, modifier); return }
    var expanded by remember { mutableStateOf(false) }
    var textFieldWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(modifier = modifier.onGloballyPositioned { textFieldWidth = it.size.width }) {
        FixedOutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), readOnly = true,
            trailingIcon = { Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null) },
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = !expanded })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.width(with(density) { textFieldWidth.toDp() }).heightIn(max = 300.dp)) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = { onSelectionChanged(index); expanded = false }) {
                    Text(text = option, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ModernButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) { chrome.Button(text, onClick, modifier, isLoading, enabled, icon); return }
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
