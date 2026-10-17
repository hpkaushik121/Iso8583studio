package `in`.aicortex.iso8583studio.ui.screens.cipher

import kotlinx.coroutines.withContext

import kotlinx.coroutines.Dispatchers

import androidx.compose.runtime.rememberCoroutineScope

import `in`.aicortex.iso8583studio.platform.epochMillis

import io.cryptocalc.crypto.primitives.Digests

import io.cryptocalc.rsa.generateRsaKeyPair

import io.cryptocalc.rsa.RsaPublicKey

import io.cryptocalc.rsa.RsaPrivateKey

import io.cryptocalc.rsa.Rsa

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
        addLog( LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
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
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
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


/** Key material shared by every tab of the calculator. */
private object RsaKeyState {
    var modulus by mutableStateOf("")
    var publicExp by mutableStateOf("010001")
    var privateExp by mutableStateOf("")
    var primeP by mutableStateOf("")
    var primeQ by mutableStateOf("")

    fun publicKey(): RsaPublicKey {
        require(modulus.isNotBlank() && publicExp.isNotBlank()) { "Enter or generate a key on the Keys tab first" }
        return RsaPublicKey(BigInt.fromHex(modulus), BigInt.fromHex(publicExp))
    }

    fun privateKey(): RsaPrivateKey {
        require(modulus.isNotBlank() && privateExp.isNotBlank()) { "Enter or generate a private key on the Keys tab first" }
        val p = primeP.takeIf { it.isNotBlank() }?.let { BigInt.fromHex(it) }
        val q = primeQ.takeIf { it.isNotBlank() }?.let { BigInt.fromHex(it) }
        return RsaPrivateKey(BigInt.fromHex(modulus), BigInt.fromHex(publicExp.ifBlank { "010001" }), BigInt.fromHex(privateExp), p, q).withCrt()
    }
}

private fun rsaInputBytes(data: String, format: String): ByteArray =
    if (format == "Hexadecimal") data.hexToBytes() else data.encodeToByteArray()

@Composable
private fun KeysTab() {
    var keyLength by remember { mutableStateOf("2048") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ModernCryptoCard(title = "RSA Key Pair", subtitle = "Generate or paste an RSA key pair (hex)", icon = Icons.Default.VpnKey) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernDropdownField("Key Length (bits)", keyLength, listOf("1024", "2048", "3072", "4096"), onSelectionChanged = { keyLength = listOf("1024", "2048", "3072", "4096")[it] })
            ModernButton(
                text = "Generate Keys",
                onClick = {
                    isLoading = true
                    scope.launch {
                        val start = epochMillis()
                        try {
                            val pair = withContext(Dispatchers.Default) { generateRsaKeyPair(keyLength.toInt()) }
                            val k = pair.privateKey
                            RsaKeyState.modulus = BigInt.toHexEven(k.n)
                            RsaKeyState.publicExp = BigInt.toHexEven(k.e)
                            RsaKeyState.privateExp = BigInt.toHexEven(k.d)
                            RsaKeyState.primeP = k.p?.let { BigInt.toHexEven(it) } ?: ""
                            RsaKeyState.primeQ = k.q?.let { BigInt.toHexEven(it) } ?: ""
                            RsaLogManager.logOperation("Key Generation", mapOf("Key Length" to keyLength),
                                "Modulus (n): ${RsaKeyState.modulus}\nPublic exponent (e): ${RsaKeyState.publicExp}\nPrivate exponent (d): ${RsaKeyState.privateExp}\np: ${RsaKeyState.primeP}\nq: ${RsaKeyState.primeQ}",
                                executionTime = epochMillis() - start)
                        } catch (e: Exception) {
                            RsaLogManager.logOperation("Key Generation", mapOf("Key Length" to keyLength), error = e.message)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Autorenew
            )
            Divider(Modifier.padding(vertical = 8.dp))
            EnhancedTextField(value = RsaKeyState.modulus, onValueChange = { RsaKeyState.modulus = it.uppercase() }, label = "Modulus (n, Hex)", maxLines = 5)
            EnhancedTextField(value = RsaKeyState.publicExp, onValueChange = { RsaKeyState.publicExp = it.uppercase() }, label = "Public Exponent (e, Hex)")
            EnhancedTextField(value = RsaKeyState.privateExp, onValueChange = { RsaKeyState.privateExp = it.uppercase() }, label = "Private Exponent (d, Hex)", maxLines = 5)
            EnhancedTextField(value = RsaKeyState.primeP, onValueChange = { RsaKeyState.primeP = it.uppercase() }, label = "Prime p (Hex, optional, speeds up private operations)", maxLines = 3)
            EnhancedTextField(value = RsaKeyState.primeQ, onValueChange = { RsaKeyState.primeQ = it.uppercase() }, label = "Prime q (Hex, optional)", maxLines = 3)
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
                ModernDropdownField("Padding", padding, listOf("PKCS1", "OAEP (SHA-256)", "No Padding"), { padding = listOf("PKCS1", "OAEP (SHA-256)", "No Padding")[it] }, Modifier.weight(1f))
            }
            ModernDropdownField("Input Data Format", inputFormat, listOf("ASCII", "Hexadecimal"), { inputFormat = if(it==0) "ASCII" else "Hexadecimal" })
            EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data to Encrypt", maxLines = 8)
            ModernButton(
                text = "Encrypt",
                onClick = {
                    val inputs = mapOf("Data" to data, "Method" to encodingMethod, "Padding" to padding)
                    try {
                        val bytes = rsaInputBytes(data, inputFormat)
                        val out = when {
                            encodingMethod == "Private" -> {
                                val key = RsaKeyState.privateKey()
                                when (padding) {
                                    "PKCS1" -> Rsa.rawDecrypt(key, Rsa.pkcs1Type1Pad(bytes, key.byteLength))
                                    "No Padding" -> Rsa.rawDecrypt(key, bytes)
                                    else -> throw IllegalArgumentException("OAEP is only defined for public-key encryption")
                                }
                            }
                            else -> {
                                val key = RsaKeyState.publicKey()
                                when (padding) {
                                    "PKCS1" -> Rsa.encryptPkcs1(key, bytes)
                                    "OAEP (SHA-256)" -> Rsa.encryptOaep(key, bytes, "SHA-256")
                                    else -> Rsa.rawEncrypt(key, bytes)
                                }
                            }
                        }
                        RsaLogManager.logOperation("Encrypt", inputs, "Encrypted Data: ${out.toHexUpper()}")
                    } catch (e: Exception) {
                        RsaLogManager.logOperation("Encrypt", inputs, error = e.message)
                    }
                },
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
                ModernDropdownField("Padding", padding, listOf("PKCS1", "OAEP (SHA-256)", "No Padding"), { padding = listOf("PKCS1", "OAEP (SHA-256)", "No Padding")[it] }, Modifier.weight(1f))
            }
            EnhancedTextField(value = data, onValueChange = { data = it.uppercase() }, label = "Data to Decrypt (Hex)", maxLines = 8)
            ModernButton(
                text = "Decrypt",
                onClick = {
                    val inputs = mapOf("Data" to data, "Method" to decodingMethod, "Padding" to padding)
                    try {
                        val bytes = data.hexToBytes()
                        val out = if (decodingMethod == "Private") {
                            val key = RsaKeyState.privateKey()
                            when (padding) {
                                "PKCS1" -> Rsa.decryptPkcs1(key, bytes)
                                "OAEP (SHA-256)" -> Rsa.decryptOaep(key, bytes, "SHA-256")
                                else -> Rsa.rawDecrypt(key, bytes)
                            }
                        } else {
                            val key = RsaKeyState.publicKey()
                            when (padding) {
                                "PKCS1" -> Rsa.pkcs1Unpad(Rsa.rawEncrypt(key, bytes), 1)
                                "No Padding" -> Rsa.rawEncrypt(key, bytes)
                                else -> throw IllegalArgumentException("OAEP is only defined for private-key decryption")
                            }
                        }
                        val ascii = out.decodeToString().takeIf { s -> s.all { it.code in 32..126 } }
                        RsaLogManager.logOperation("Decrypt", inputs, "Decrypted Data (Hex): ${out.toHexUpper()}" + (ascii?.let { "\nASCII: $it" } ?: ""))
                    } catch (e: Exception) {
                        RsaLogManager.logOperation("Decrypt", inputs, error = e.message)
                    }
                },
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
    var hashType by remember { mutableStateOf("SHA-256") }
    var scheme by remember { mutableStateOf("PKCS#1 v1.5") }
    val hashes = listOf("SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512")
    val isFormValid = data.isNotBlank()
    ModernCryptoCard(title = "RSA Signature", subtitle = "Sign data using the private key", icon = Icons.Default.Edit) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Hash", hashType, hashes, { hashType = hashes[it] }, Modifier.weight(1f))
                ModernDropdownField("Scheme", scheme, listOf("PKCS#1 v1.5", "PSS"), { scheme = if (it == 0) "PKCS#1 v1.5" else "PSS" }, Modifier.weight(1f))
            }
            ModernDropdownField("Input Data Format", inputFormat, listOf("ASCII", "Hexadecimal"), { inputFormat = if(it==0) "ASCII" else "Hexadecimal" })
            EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data to Sign", maxLines = 8)
            ModernButton(
                text = "Sign",
                onClick = {
                    val inputs = mapOf("Data" to data, "Hash" to hashType, "Scheme" to scheme)
                    try {
                        val key = RsaKeyState.privateKey()
                        val bytes = rsaInputBytes(data, inputFormat)
                        val sig = if (scheme == "PSS") Rsa.signPss(key, hashType, bytes) else Rsa.signPkcs1(key, hashType, bytes)
                        RsaLogManager.logOperation("Sign", inputs, "Hash: ${Digests.hash(hashType, bytes).toHexUpper()}\nSignature: ${sig.toHexUpper()}")
                    } catch (e: Exception) {
                        RsaLogManager.logOperation("Sign", inputs, error = e.message)
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
    var hashType by remember { mutableStateOf("SHA-256") }
    var scheme by remember { mutableStateOf("PKCS#1 v1.5") }
    val hashes = listOf("SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512")
    val isFormValid = hash.isNotBlank() && signature.isNotBlank()
    ModernCryptoCard(title = "RSA Verification", subtitle = "Verify a signature against a hash with the public key", icon = Icons.Default.VerifiedUser) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Hash", hashType, hashes, { hashType = hashes[it] }, Modifier.weight(1f))
                ModernDropdownField("Scheme", scheme, listOf("PKCS#1 v1.5", "PSS (needs data)"), { scheme = if (it == 0) "PKCS#1 v1.5" else "PSS (needs data)" }, Modifier.weight(1f))
            }
            EnhancedTextField(value = hash, onValueChange = { hash = it.uppercase() }, label = if (scheme.startsWith("PSS")) "Signed Data (Hex)" else "Hash (Hex)", maxLines = 4)
            EnhancedTextField(value = signature, onValueChange = { signature = it.uppercase() }, label = "Signature (Hex)", maxLines = 8)
            ModernButton(
                text = "Verify",
                onClick = {
                    val inputs = mapOf("Hash" to hash, "Signature" to signature, "Hash type" to hashType, "Scheme" to scheme)
                    try {
                        val key = RsaKeyState.publicKey()
                        val ok = if (scheme.startsWith("PSS")) Rsa.verifyPss(key, hashType, hash.hexToBytes(), signature.hexToBytes())
                                 else Rsa.verifyPkcs1Digest(key, hashType, hash.hexToBytes(), signature.hexToBytes())
                        RsaLogManager.logOperation("Verify", inputs, if (ok) "Verification successful: signature is VALID" else "Verification FAILED: signature does not match")
                    } catch (e: Exception) {
                        RsaLogManager.logOperation("Verify", inputs, error = e.message)
                    }
                },
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
    ModernCryptoCard(title = "OAEP Padding", subtitle = "EME-OAEP encoding and decoding (RFC 8017 §7.1), without the RSA step", icon = Icons.Default.EnhancedEncryption) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernDropdownField("Method", method, listOf("Encode", "Decode"), { method = if(it==0) "Encode" else "Decode" }, Modifier.weight(1f))
                ModernDropdownField("Hash Function", hashFunction, hashFunctions, { hashFunction = hashFunctions[it] }, Modifier.weight(1f))
            }
            ModernDropdownField("Result Length (bits)", resultLength, listOf("1024", "2048", "4096"), { resultLength = listOf("1024", "2048", "4096")[it] })
            EnhancedTextField(value = data, onValueChange = { data = it.uppercase() }, label = "Data (Hex)", maxLines = 6)
            EnhancedTextField(value = label, onValueChange = { label = it.uppercase() }, label = "Encoding Parameters (Label, Hex)", validation = RsaValidationUtils.validateHex(label))
            ModernButton(
                text = method,
                onClick = {
                    val inputs = mapOf("Data" to data, "Hash" to hashFunction, "Length" to resultLength, "Label" to label)
                    try {
                        val l = if (label.isBlank()) ByteArray(0) else label.hexToBytes()
                        val out = if (method == "Encode") Rsa.oaepEncode(data.hexToBytes(), resultLength.toInt() / 8, hashFunction, l)
                                  else Rsa.oaepDecode(data.hexToBytes(), hashFunction, l)
                        RsaLogManager.logOperation("OAEP $method", inputs, "Result: ${out.toHexUpper()}")
                    } catch (e: Exception) {
                        RsaLogManager.logOperation("OAEP $method", inputs, error = e.message)
                    }
                },
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
