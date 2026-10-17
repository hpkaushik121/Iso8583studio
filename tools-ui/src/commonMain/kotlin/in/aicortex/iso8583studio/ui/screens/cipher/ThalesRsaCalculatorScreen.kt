package `in`.aicortex.iso8583studio.ui.screens.cipher

import `in`.aicortex.iso8583studio.ui.chrome.ToolTab

import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec

import `in`.aicortex.iso8583studio.ui.chrome.ToolLog

import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome

import kotlinx.coroutines.withContext

import kotlinx.coroutines.Dispatchers

import androidx.compose.runtime.rememberCoroutineScope

import io.cryptocalc.crypto.engines.encryption.TdesCalculatorEngine

import io.cryptocalc.crypto.primitives.Tdes

import io.cryptocalc.crypto.primitives.Modes

import io.cryptocalc.crypto.primitives.Mac

import io.cryptocalc.hsm.ThalesRsaEncoding

import io.cryptocalc.hsm.Tr31KeyBlock

import io.cryptocalc.asn1.PublicKeyInfo

import io.cryptocalc.asn1.Pkcs

import io.cryptocalc.rsa.generateRsaKeyPair

import io.cryptocalc.rsa.RsaPrivateKey

import io.cryptocalc.rsa.Rsa

import io.cryptocalc.math.BigInt

import ai.cortex.core.types.PaddingMethods

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
private object ThalesRsaValidationUtils {
    fun validateHex(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "Field cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        return ValidationResult(ValidationState.VALID)
    }
    fun validateNumeric(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY)
        if (value.any { !it.isDigit() }) {
            return ValidationResult(ValidationState.ERROR, "Input must only contain digits (0-9).")
        }
        return ValidationResult(ValidationState.VALID)
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
            result?.let { append("\nResult:\n$it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}


@Composable
fun ThalesRsaScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = ThalesRsaTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    val body: @Composable () -> Unit = {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        when (tab) {
                            ThalesRsaTabs.GENERATE -> GenerateTab()
                            ThalesRsaTabs.KEY_BLOCK -> KeyBlockTab()
                            ThalesRsaTabs.LMK_VARIANT -> LmkVariantTab()
                        }
                    }
                
    }
    val chrome = LocalToolChrome.current
    if (!chrome.isDesktop) {
        chrome.Screen(ToolScreenSpec(title = "Thales RSA Calculator", onBack = onBack, tabs = tabList.map { ToolTab(it.title, it.icon) }, selectedTab = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, log = ToolLog(ThalesRsaLogManager.logEntries, onClear = { ThalesRsaLogManager.clearLogs() })), content = body)
        return
    }
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
            body()
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
private fun rsaKeyFromText(text: String): RsaPrivateKey = when (val k = Pkcs.parsePrivateKey(text)) {
    is Pkcs.PrivateKeyInfo.Rsa -> k.key
    else -> throw IllegalArgumentException("Not an RSA private key")
}

@Composable
private fun GenerateTab() {
    var privateExp by remember { mutableStateOf("") }
    var prime1 by remember { mutableStateOf("") }
    var prime2 by remember { mutableStateOf("") }
    var exponent1 by remember { mutableStateOf("") }
    var exponent2 by remember { mutableStateOf("") }
    var coefficient by remember { mutableStateOf("") }
    var publicExp by remember { mutableStateOf("010001") }
    var modulus by remember { mutableStateOf("") }
    var keyLength by remember { mutableStateOf("2048") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun show(k: RsaPrivateKey) {
        val c = k.withCrt()
        modulus = BigInt.toHexEven(c.n); publicExp = BigInt.toHexEven(c.e); privateExp = BigInt.toHexEven(c.d)
        prime1 = c.p?.let { BigInt.toHexEven(it) } ?: ""; prime2 = c.q?.let { BigInt.toHexEven(it) } ?: ""
        exponent1 = c.dP?.let { BigInt.toHexEven(it) } ?: ""; exponent2 = c.dQ?.let { BigInt.toHexEven(it) } ?: ""; coefficient = c.qInv?.let { BigInt.toHexEven(it) } ?: ""
    }
    ModernCryptoCard(title = "Generate RSA Key Components", subtitle = "Generate a random key or derive the CRT components from p and q", icon = Icons.Default.Autorenew) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = modulus, onValueChange = { modulus = it.uppercase() }, label = "Modulus (n)", validation = ThalesRsaValidationUtils.validateHex(modulus), maxLines = 4)
            EnhancedTextField(value = publicExp, onValueChange = { publicExp = it.uppercase() }, label = "Public Exp. (e)", validation = ThalesRsaValidationUtils.validateHex(publicExp))
            EnhancedTextField(value = privateExp, onValueChange = { privateExp = it.uppercase() }, label = "Private Exp. (d)", validation = ThalesRsaValidationUtils.validateHex(privateExp), maxLines = 4)
            EnhancedTextField(value = prime1, onValueChange = { prime1 = it.uppercase() }, label = "Prime 1 (p)", validation = ThalesRsaValidationUtils.validateHex(prime1), maxLines = 4)
            EnhancedTextField(value = prime2, onValueChange = { prime2 = it.uppercase() }, label = "Prime 2 (q)", validation = ThalesRsaValidationUtils.validateHex(prime2), maxLines = 4)
            EnhancedTextField(value = exponent1, onValueChange = { exponent1 = it.uppercase() }, label = "Exponent 1 (dModP1)", validation = ThalesRsaValidationUtils.validateHex(exponent1), maxLines = 4)
            EnhancedTextField(value = exponent2, onValueChange = { exponent2 = it.uppercase() }, label = "Exponent 2 (dModQ1)", validation = ThalesRsaValidationUtils.validateHex(exponent2), maxLines = 4)
            EnhancedTextField(value = coefficient, onValueChange = { coefficient = it.uppercase() }, label = "Coefficient (iqmp)", validation = ThalesRsaValidationUtils.validateHex(coefficient), maxLines = 4)
            Divider(Modifier.padding(vertical=8.dp))
            EnhancedTextField(value = keyLength, onValueChange = { keyLength = it }, label = "Key Length (512-4096)", validation = ThalesRsaValidationUtils.validateNumeric(keyLength))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(
                    text = "Derive from p, q, e",
                    onClick = {
                        val inputs = mapOf("Prime 1" to prime1, "Prime 2" to prime2, "e" to publicExp)
                        try {
                            val k = Rsa.fromPrimes(BigInt.fromHex(prime1), BigInt.fromHex(prime2), BigInt.fromHex(publicExp.ifBlank { "010001" }))
                            show(k)
                            ThalesRsaLogManager.logOperation("Derive components", inputs, "Modulus (n): $modulus\nPrivate Exponent (d): $privateExp\ndP: $exponent1\ndQ: $exponent2\nqInv: $coefficient")
                        } catch (e: Exception) { ThalesRsaLogManager.logOperation("Derive components", inputs, error = e.message) }
                    },
                    enabled = prime1.isNotBlank() && prime2.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
                ModernButton(
                    text = "Generate New Random Key",
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val length = keyLength.toIntOrNull() ?: 2048
                            try {
                                val pair = withContext(Dispatchers.Default) { generateRsaKeyPair(length) }
                                show(pair.privateKey)
                                ThalesRsaLogManager.logOperation("Generate Random Key", mapOf("Key Length" to keyLength), "New $length-bit key generated.\nModulus: $modulus")
                            } catch (e: Exception) { ThalesRsaLogManager.logOperation("Generate Random Key", mapOf("Key Length" to keyLength), error = e.message) }
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
    var publicKeyHeader by remember { mutableStateOf("D0000S0TN00N0000") }
    var privateKeyHeader by remember { mutableStateOf("D0000S0TN00N0000") }
    var preferredEncryption by remember { mutableStateOf("AES") }
    var inputFormat by remember { mutableStateOf("Hexadecimal") }
    var publicKey by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    ModernCryptoCard(title = "Thales Key Block", subtitle = "Wrap an RSA key into a TR-31 style key block under a KBPK (reference implementation)", icon = Icons.Default.ViewModule) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = desKbpk, onValueChange = { desKbpk = it.uppercase() }, label = "DES KBPK (Hex)", validation = ThalesRsaValidationUtils.validateHex(desKbpk))
            EnhancedTextField(value = aesKbpk, onValueChange = { aesKbpk = it.uppercase() }, label = "AES KBPK (Hex)", validation = ThalesRsaValidationUtils.validateHex(aesKbpk))
            EnhancedTextField(value = publicKeyHeader, onValueChange = { publicKeyHeader = it.uppercase() }, label = "Public Key Header (16 ASCII chars, version B=TDES / D=AES)")
            EnhancedTextField(value = privateKeyHeader, onValueChange = { privateKeyHeader = it.uppercase() }, label = "Private Key Header (16 ASCII chars)")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Key Block Encryption", preferredEncryption, listOf("AES", "DES"), { preferredEncryption = if(it==0) "AES" else "DES" }, Modifier.weight(1f))
                ModernDropdownField("Input Format", inputFormat, listOf("PEM", "Hexadecimal"), { inputFormat = if(it==0) "PEM" else "Hexadecimal" }, Modifier.weight(1f))
            }
            EnhancedTextField(value = publicKey, onValueChange = { publicKey = it }, label = "Public Key (PEM or DER hex)", maxLines = 4)
            EnhancedTextField(value = privateKey, onValueChange = { privateKey = it }, label = "Private Key (PEM or DER hex)", maxLines = 4)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernButton(text = "Wrap Key Block", onClick = {
                    val inputs = mapOf("Encryption" to preferredEncryption, "Public header" to publicKeyHeader, "Private header" to privateKeyHeader)
                    try {
                        val kbpk = (if (preferredEncryption == "AES") aesKbpk else desKbpk).hexToBytes()
                        val pub = Pkcs.parsePublicKey(publicKey)
                        require(pub is PublicKeyInfo.Rsa) { "Public key is not RSA" }
                        val pubDer = Pkcs.rsaPublicKeyDer(pub.key)
                        val privDer = Pkcs.rsaPrivateKeyDer(rsaKeyFromText(privateKey))
                        val pubBlock = Tr31KeyBlock.wrap(kbpk, publicKeyHeader, pubDer, preferredEncryption == "AES").keyBlock
                        val privBlock = Tr31KeyBlock.wrap(kbpk, privateKeyHeader, privDer, preferredEncryption == "AES").keyBlock
                        ThalesRsaLogManager.logOperation("Wrap Key Block", inputs, "Public key block:\n$pubBlock\n\nPrivate key block:\n$privBlock")
                    } catch (e: Exception) { ThalesRsaLogManager.logOperation("Wrap Key Block", inputs, error = e.message) }
                }, modifier = Modifier.weight(1f))
                ModernButton(text = "Unwrap Key Block", onClick = {
                    val inputs = mapOf("Encryption" to preferredEncryption)
                    try {
                        val kbpk = (if (preferredEncryption == "AES") aesKbpk else desKbpk).hexToBytes()
                        val block = privateKey.ifBlank { publicKey }.trim()
                        val u = Tr31KeyBlock.unwrap(kbpk, block, preferredEncryption == "AES")
                        ThalesRsaLogManager.logOperation("Unwrap Key Block", inputs, "Header: ${u.header}\nMAC valid: ${u.macValid}\nKey (DER hex): ${u.key.toHexUpper()}")
                    } catch (e: Exception) { ThalesRsaLogManager.logOperation("Unwrap Key Block", inputs, error = e.message) }
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LmkVariantTab() {
    val modulusEncodings = remember { listOf(
        "Encoding 01: ASN.1 INTEGER, unsigned representation",
        "Encoding 02: ASN.1 INTEGER, two's complement representation"
    ) }
    var lmk3435 by remember { mutableStateOf("") }
    var lmk3637 by remember { mutableStateOf("") }
    var authData by remember { mutableStateOf("") }
    var selectedModEncoding by remember { mutableStateOf(modulusEncodings.first()) }
    var publicKey by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    ModernCryptoCard(title = "Thales LMK Variant", subtitle = "Private key under LMK 34-35, public key MAC under LMK 36-37 (payShield EI/EM semantics)", icon = Icons.Default.VpnKey) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = lmk3435, onValueChange = { lmk3435 = it.uppercase() }, label = "LMK Pair 34-35 (Hex)", validation = ThalesRsaValidationUtils.validateHex(lmk3435))
            EnhancedTextField(value = lmk3637, onValueChange = { lmk3637 = it.uppercase() }, label = "LMK Pair 36-37 (Hex)", validation = ThalesRsaValidationUtils.validateHex(lmk3637))
            EnhancedTextField(value = authData, onValueChange = { authData = it.uppercase() }, label = "Authentication Data (Hex, optional)", validation = ThalesRsaValidationUtils.validateHex(authData))
            ModernDropdownField("Modulus Encoding", selectedModEncoding, modulusEncodings, { selectedModEncoding = modulusEncodings[it] })
            EnhancedTextField(value = publicKey, onValueChange = { publicKey = it }, label = "Public Key (PEM or DER hex)", maxLines = 4)
            EnhancedTextField(value = privateKey, onValueChange = { privateKey = it }, label = "Private Key (PEM or DER hex)", maxLines = 4)
            Spacer(Modifier.height(8.dp))
            ModernButton(text = "Process LMK Variant", onClick = {
                val inputs = mapOf("Modulus encoding" to selectedModEncoding, "Auth data" to authData)
                try {
                    val pub = Pkcs.parsePublicKey(publicKey)
                    require(pub is PublicKeyInfo.Rsa) { "Public key is not RSA" }
                    val unsigned = selectedModEncoding.startsWith("Encoding 01")
                    val pubDer = ThalesRsaEncoding.rsaPublicKeyDer(pub.key, unsigned)
                    val macKey = lmk3637.hexToBytes()
                    val mac = Mac.algorithm1(Tdes(macKey), pubDer + (if (authData.isBlank()) ByteArray(0) else authData.hexToBytes()), Mac.Padding.METHOD_1)
                    val sb = StringBuilder()
                    sb.append("Public key (DER, encoding ${if (unsigned) "01" else "02"}): ${pubDer.toHexUpper()}\n")
                    sb.append("Public key MAC under LMK 36-37: ${mac.toHexUpper().take(8)}\n")
                    if (privateKey.isNotBlank()) {
                        val priv = rsaKeyFromText(privateKey)
                        val privDer = Pkcs.rsaPrivateKeyDer(priv)
                        val padded = Modes.pad(privDer, 8, PaddingMethods.METHOD_2_ISO_9797)
                        val enc = TdesCalculatorEngine.encryptCBC(padded, lmk3435.hexToBytes(), ByteArray(8))
                        sb.append("Private key under LMK 34-35 (TDES-CBC, length ${enc.size}): ${enc.toHexUpper()}")
                    }
                    ThalesRsaLogManager.logOperation("LMK Variant", inputs, sb.toString())
                } catch (e: Exception) { ThalesRsaLogManager.logOperation("LMK Variant", inputs, error = e.message) }
            }, modifier = Modifier.fillMaxWidth())
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
