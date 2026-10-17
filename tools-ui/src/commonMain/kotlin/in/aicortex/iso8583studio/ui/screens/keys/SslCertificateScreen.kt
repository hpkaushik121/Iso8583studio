package `in`.aicortex.iso8583studio.ui.screens.keys

import kotlinx.coroutines.withContext

import kotlinx.coroutines.Dispatchers

import androidx.compose.runtime.rememberCoroutineScope

import `in`.aicortex.iso8583studio.platform.epochMillis

import io.cryptocalc.x509.X509

import io.cryptocalc.x509.SigningKey

import io.cryptocalc.x509.DistinguishedName

import io.cryptocalc.x509.CertificateInfo

import io.cryptocalc.rsa.generateRsaKeyPair

import io.cryptocalc.rsa.RsaPublicKey

import io.cryptocalc.ec.EcCurve

import io.cryptocalc.ec.Ec

import io.cryptocalc.asn1.Pkcs

import io.cryptocalc.asn1.Der

import `in`.aicortex.iso8583studio.platform.nowLogTimestamp

import `in`.aicortex.iso8583studio.analytics.Analytics
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

// --- SSL UTILITY SCREEN ---

private enum class SslTabs(val title: String, val icon: ImageVector) {
    KEYS("Keys", Icons.Default.VpnKey),
    CSR("CSRs", Icons.Default.PostAdd),
    READ_CSR("Read CSR", Icons.Default.FindInPage),
    SELF_SIGNED("Self-Signed", Icons.Default.VerifiedUser),
    READ_CERT("Read Certificate", Icons.Default.Plagiarism)
}

private object SslLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
        addLog(LogEntry(nowLogTimestamp(), LogType.INFO, "Log history cleared", ""))
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
                val displayValue = if (value.length > 200) "${value.take(200)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n$it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp, logType, message, details))
    }
}


@Composable
fun SslUtilityScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = SslTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "SSL Certificate (X.509) Utility", onBackClick = onBack) },
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
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    PersistentTabContent(
                        selectedTab = selectedTab,
                        tabs = tabList
                    ) { tab ->
                        when (tab) {
                            SslTabs.KEYS -> KeysTab()
                            SslTabs.CSR -> CsrTab()
                            SslTabs.READ_CSR -> ReadCsrTab()
                            SslTabs.SELF_SIGNED -> SelfSignedCertTab()
                            SslTabs.READ_CERT -> ReadCertTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { SslLogManager.clearLogs() },
                            logEntries = SslLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// --- TABS ---

private object SslSupport {
    val signatureAlgorithms = listOf("SHA256withRSA", "SHA384withRSA", "SHA512withRSA", "SHA256withECDSA", "SHA384withECDSA", "SHA512withECDSA")

    fun hashOf(algorithm: String): String = when {
        algorithm.startsWith("SHA384") -> "SHA-384"
        algorithm.startsWith("SHA512") -> "SHA-512"
        else -> "SHA-256"
    }

    fun signingKey(pem: String, algorithm: String): SigningKey = when (val k = Pkcs.parsePrivateKey(pem)) {
        is Pkcs.PrivateKeyInfo.Rsa -> { require(algorithm.endsWith("RSA")) { "$algorithm needs an EC private key" }; SigningKey.Rsa(k.key) }
        is Pkcs.PrivateKeyInfo.Ec -> { require(algorithm.endsWith("ECDSA")) { "$algorithm needs an RSA private key" }; SigningKey.Ec(k.key) }
    }

    fun describe(info: CertificateInfo, isCert: Boolean): String = buildString {
        if (isCert) {
            appendLine("Version: ${info.version ?: "?"}")
            appendLine("Serial: ${info.serialHex}")
            appendLine("Issuer: ${info.issuer}")
        }
        appendLine("Subject: ${info.subject}")
        if (isCert) {
            appendLine("Valid from: ${info.notBefore}")
            appendLine("Valid until: ${info.notAfter}")
        }
        appendLine("Public key: ${info.publicKeyAlgorithm} (${info.publicKeySizeBits} bits)")
        appendLine("Signature algorithm: ${info.signatureAlgorithm}")
        if (info.extensions.isNotEmpty()) appendLine("Extensions: ${info.extensions.joinToString(", ")}")
        appendLine("SHA-1 fingerprint: ${info.sha1Fingerprint}")
        append("SHA-256 fingerprint: ${info.sha256Fingerprint}")
    }
}

@Composable
private fun KeysTab() {
    var keyType by remember { mutableStateOf("RSA") }
    val rsaKeyLengths = remember { listOf("2048", "3072", "4096") }
    val eccCurves = remember { listOf("NIST P-256", "NIST P-384", "NIST P-521", "secp256k1") }
    var selectedRsaLength by remember { mutableStateOf(rsaKeyLengths.first()) }
    var selectedEccCurve by remember { mutableStateOf(eccCurves.first()) }
    var publicKey by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModernCryptoCard(title = "Key Pair Generation", subtitle = "RSA or EC key pair as SubjectPublicKeyInfo / PKCS#8 PEM", icon = Icons.Default.VpnKey) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernDropdownField(label = "Key Type", value = keyType, options = listOf("RSA", "ECC"), onSelectionChanged = { keyType = if(it==0) "RSA" else "ECC"})
            AnimatedContent(targetState = keyType, label = "key_type_options") { type ->
                when(type) {
                    "RSA" -> ModernDropdownField(label = "Key Length (bits)", value = selectedRsaLength, options = rsaKeyLengths, onSelectionChanged = { selectedRsaLength = rsaKeyLengths[it]})
                    "ECC" -> ModernDropdownField(label = "Curve Name", value = selectedEccCurve, options = eccCurves, onSelectionChanged = { selectedEccCurve = eccCurves[it]})
                }
            }
            EnhancedTextField(value = publicKey, onValueChange = { publicKey = it }, label = "Public Key (PEM)", maxLines = 6)
            EnhancedTextField(value = privateKey, onValueChange = { privateKey = it }, label = "Private Key (PEM, PKCS#8)", maxLines = 10)
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Generate Key Pair",
                onClick = {
                    isLoading = true
                    val keySize = if(keyType == "RSA") selectedRsaLength else selectedEccCurve
                    val inputs = mapOf("Type" to keyType, "Size/Curve" to keySize)
                    scope.launch {
                        val start = epochMillis()
                        try {
                            if (keyType == "RSA") {
                                val pair = withContext(Dispatchers.Default) { generateRsaKeyPair(selectedRsaLength.toInt()) }
                                val priv = pair.privateKey
                                publicKey = Der.toPem("PUBLIC KEY", Pkcs.subjectPublicKeyInfo(RsaPublicKey(priv.n, priv.e)))
                                privateKey = Der.toPem("PRIVATE KEY", Pkcs.pkcs8(priv))
                            } else {
                                val curve = EcCurve.byName(selectedEccCurve)
                                val kp = withContext(Dispatchers.Default) { Ec.generateKeyPair(curve) }
                                publicKey = Der.toPem("PUBLIC KEY", Pkcs.subjectPublicKeyInfo(curve, kp.q))
                                privateKey = Der.toPem("PRIVATE KEY", Pkcs.pkcs8(kp))
                            }
                            SslLogManager.logOperation("Key Pair Generation", inputs, "$publicKey\n$privateKey", executionTime = epochMillis() - start)
                        } catch (e: Exception) {
                            SslLogManager.logOperation("Key Pair Generation", inputs, error = e.message)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Autorenew
            )
        }
    }
}

@Composable
private fun CsrTab() {
    var privateKey by remember { mutableStateOf("") }
    var signatureAlgorithm by remember { mutableStateOf(SslSupport.signatureAlgorithms.first()) }
    var commonName by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    var orgUnit by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    ModernCryptoCard(title = "Certificate Signing Request", subtitle = "PKCS#10 request signed with the private key", icon = Icons.Default.PostAdd) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = privateKey, onValueChange = { privateKey = it }, label = "Private Key (PEM: PKCS#8, PKCS#1 or EC)", maxLines = 5)
            ModernDropdownField("Signature Algorithm", signatureAlgorithm, SslSupport.signatureAlgorithms, { signatureAlgorithm = SslSupport.signatureAlgorithms[it] })
            Divider(Modifier.padding(vertical = 8.dp))
            Text("Subject Information", style = MaterialTheme.typography.subtitle2)
            EnhancedTextField(value = commonName, onValueChange = { commonName = it }, label = "Common Name (e.g., yourdomain.com)")
            EnhancedTextField(value = organization, onValueChange = { organization = it }, label = "Organization")
            EnhancedTextField(value = orgUnit, onValueChange = { orgUnit = it }, label = "Organizational Unit")
            EnhancedTextField(value = city, onValueChange = { city = it }, label = "City/Locality")
            EnhancedTextField(value = state, onValueChange = { state = it }, label = "State/Province")
            EnhancedTextField(value = country, onValueChange = { country = it.uppercase().take(2) }, label = "Country (2-letter code)")
            Spacer(Modifier.height(8.dp))
            ModernButton("Generate CSR", onClick = {
                val inputs = mapOf("Common Name" to commonName, "Organization" to organization, "Algorithm" to signatureAlgorithm)
                try {
                    val key = SslSupport.signingKey(privateKey, signatureAlgorithm)
                    val dn = DistinguishedName(commonName, organization, orgUnit, city, state, country)
                    val csr = X509.certificationRequest(key, dn, SslSupport.hashOf(signatureAlgorithm))
                    SslLogManager.logOperation("CSR Generation", inputs, Der.toPem("CERTIFICATE REQUEST", csr))
                } catch (e: Exception) {
                    SslLogManager.logOperation("CSR Generation", inputs, error = e.message)
                }
            }, enabled = privateKey.isNotBlank() && commonName.isNotBlank())
        }
    }
}

@Composable
private fun ReadCsrTab() {
    var csrData by remember { mutableStateOf("") }
    ModernCryptoCard(title = "Read CSR", subtitle = "Parse a PKCS#10 request (PEM or DER hex)", icon = Icons.Default.FindInPage) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = csrData, onValueChange = { csrData = it }, label = "CSR Data (PEM Format)", maxLines = 15)
            ModernButton("Parse CSR", onClick = {
                try {
                    val info = X509.parseCertificationRequest(csrData)
                    SslLogManager.logOperation("Parse CSR", mapOf("CSR" to csrData), SslSupport.describe(info, isCert = false))
                } catch (e: Exception) {
                    SslLogManager.logOperation("Parse CSR", mapOf("CSR" to csrData), error = e.message)
                }
            }, enabled = csrData.isNotBlank())
        }
    }
}

@Composable
private fun SelfSignedCertTab() {
    var privateKey by remember { mutableStateOf("") }
    var signatureAlgorithm by remember { mutableStateOf(SslSupport.signatureAlgorithms.first()) }
    var validityDays by remember { mutableStateOf("365") }
    var commonName by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    ModernCryptoCard(title = "Self-Signed Certificate", subtitle = "X.509 v3 certificate with basicConstraints CA and a random serial", icon = Icons.Default.VerifiedUser) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = privateKey, onValueChange = { privateKey = it }, label = "Private Key (PEM Format)", maxLines = 5)
            ModernDropdownField("Signature Algorithm", signatureAlgorithm, SslSupport.signatureAlgorithms, { signatureAlgorithm = SslSupport.signatureAlgorithms[it] })
            EnhancedTextField(value = validityDays, onValueChange = { validityDays = it.filter { c -> c.isDigit() } }, label = "Validity (Days)")
            Divider(Modifier.padding(vertical = 8.dp))
            EnhancedTextField(value = commonName, onValueChange = { commonName = it }, label = "Common Name (e.g., localhost)")
            EnhancedTextField(value = organization, onValueChange = { organization = it }, label = "Organization")
            EnhancedTextField(value = country, onValueChange = { country = it.uppercase().take(2) }, label = "Country (2-letter code, optional)")
            ModernButton("Generate Self-Signed Certificate", onClick = {
                val inputs = mapOf("Common Name" to commonName, "Validity" to validityDays, "Algorithm" to signatureAlgorithm)
                try {
                    val key = SslSupport.signingKey(privateKey, signatureAlgorithm)
                    val dn = DistinguishedName(commonName = commonName, organization = organization, country = country)
                    val der = X509.selfSignedCertificate(key, dn, SslSupport.hashOf(signatureAlgorithm), epochMillis() / 1000, validityDays.toIntOrNull() ?: 365)
                    val pem = Der.toPem("CERTIFICATE", der)
                    SslLogManager.logOperation("Self-Signed Generation", inputs, pem + "\n\n" + SslSupport.describe(X509.parseCertificate(pem), isCert = true))
                } catch (e: Exception) {
                    SslLogManager.logOperation("Self-Signed Generation", inputs, error = e.message)
                }
            }, enabled = privateKey.isNotBlank() && commonName.isNotBlank())
        }
    }
}

@Composable
private fun ReadCertTab() {
    var certData by remember { mutableStateOf("") }
    ModernCryptoCard(title = "Read Certificate", subtitle = "Parse an X.509 certificate (PEM or DER hex) and check a self-signature", icon = Icons.Default.Plagiarism) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = certData, onValueChange = { certData = it }, label = "Certificate Data (PEM Format)", maxLines = 15)
            ModernButton("Parse Certificate", onClick = {
                try {
                    val info = X509.parseCertificate(certData)
                    val selfSig = runCatching { X509.verifySelfSignature(certData) }.getOrNull()
                    val extra = when (selfSig) { true -> "\nSelf-signature: VALID"; false -> "\nSelf-signature: not valid (or not self-signed)"; null -> "" }
                    SslLogManager.logOperation("Parse Certificate", mapOf("Certificate" to certData), SslSupport.describe(info, isCert = true) + extra)
                } catch (e: Exception) {
                    SslLogManager.logOperation("Parse Certificate", mapOf("Certificate" to certData), error = e.message)
                }
            }, enabled = certData.isNotBlank())
        }
    }
}

// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1) {
    FixedOutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier.fillMaxWidth(), maxLines = maxLines)
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
private fun ModernDropdownField(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(modifier = Modifier.onGloballyPositioned { textFieldWidth = it.size.width }) {
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "SslButtonAnimation") { loading ->
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
