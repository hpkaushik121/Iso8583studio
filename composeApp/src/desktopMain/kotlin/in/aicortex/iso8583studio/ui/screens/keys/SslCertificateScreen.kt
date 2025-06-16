package `in`.aicortex.iso8583studio.ui.screens.keys

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
        addLog(LogEntry(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")), LogType.INFO, "Log history cleared", ""))
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

private object SslCryptoService {
    // This is a mock service.
    private fun generateRandomHex(length: Int) = (1..length).map { Random.nextInt(0, 16).toString(16) }.joinToString("").uppercase()

    fun generateRsaKeyPair(bits: Int): Map<String, String> {
        return mapOf(
            "public" to "-----BEGIN PUBLIC KEY-----\n${generateRandomHex(bits/4)}\n-----END PUBLIC KEY-----",
            "private" to "-----BEGIN RSA PRIVATE KEY-----\n${generateRandomHex(bits/2)}\n-----END RSA PRIVATE KEY-----"
        )
    }

    fun generateCsr(commonName: String): String {
        return "-----BEGIN CERTIFICATE SIGNING REQUEST-----\nCN=${commonName}, O=Mock Org, L=Mock City, C=US\n${generateRandomHex(256)}\n-----END CERTIFICATE SIGNING REQUEST-----"
    }
}

@OptIn(ExperimentalAnimationApi::class)
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
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn()) with
                                    (slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "ssl_utility_tab_transition"
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

@Composable
private fun KeysTab() {
    var keyType by remember { mutableStateOf("RSA") }
    val rsaKeyLengths = remember { listOf("2048", "3072", "4096") }
    val eccCurves = remember { listOf("NIST P-256", "NIST P-384", "NIST P-521") }
    var selectedRsaLength by remember { mutableStateOf(rsaKeyLengths.first()) }
    var selectedEccCurve by remember { mutableStateOf(eccCurves.first()) }
    var publicKey by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    ModernCryptoCard(title = "Key Pair Generation", subtitle = "Create a new public/private key pair", icon = Icons.Default.VpnKey) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernDropdownField(label = "Key Type", value = keyType, options = listOf("RSA", "ECC"), onSelectionChanged = { keyType = if(it==0) "RSA" else "ECC"})

            AnimatedContent(targetState = keyType, label = "key_type_options") { type ->
                when(type) {
                    "RSA" -> ModernDropdownField(label = "Key Length (bits)", value = selectedRsaLength, options = rsaKeyLengths, onSelectionChanged = { selectedRsaLength = rsaKeyLengths[it]})
                    "ECC" -> ModernDropdownField(label = "Curve Name", value = selectedEccCurve, options = eccCurves, onSelectionChanged = { selectedEccCurve = eccCurves[it]})
                }
            }

            EnhancedTextField(value = publicKey, onValueChange = { publicKey = it }, label = "Public Key", maxLines = 6)
            EnhancedTextField(value = privateKey, onValueChange = { privateKey = it }, label = "Private Key", maxLines = 10)

            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Generate Key Pair",
                onClick = {
                    isLoading = true
                    val keySize = if(keyType == "RSA") selectedRsaLength else selectedEccCurve
                    val inputs = mapOf("Type" to keyType, "Size/Curve" to keySize)
                    GlobalScope.launch {
                        delay(500)
                        val keys = SslCryptoService.generateRsaKeyPair(keySize.split(" ")[0].toIntOrNull() ?: 2048)
                        publicKey = keys["public"]!!
                        privateKey = keys["private"]!!
                        SslLogManager.logOperation("Key Pair Generation", inputs, "Successfully generated new key pair.")
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
    var signatureAlgorithm by remember { mutableStateOf("SHA256withRSA") }
    var commonName by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    var orgUnit by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    ModernCryptoCard(title = "Certificate Signing Request", subtitle = "Generate a CSR to send to a Certificate Authority", icon = Icons.Default.PostAdd) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = privateKey, onValueChange = { privateKey = it }, label = "Private Key (PEM Format)", maxLines = 5)
            ModernDropdownField("Signature Algorithm", signatureAlgorithm, listOf("SHA256withRSA", "SHA512withRSA"), { signatureAlgorithm = if(it==0) "SHA256withRSA" else "SHA512withRSA" })

            Divider(Modifier.padding(vertical = 8.dp))
            Text("Subject Information", style = MaterialTheme.typography.subtitle2)

            EnhancedTextField(value = commonName, onValueChange = { commonName = it }, label = "Common Name (e.g., yourdomain.com)")
            EnhancedTextField(value = organization, onValueChange = { organization = it }, label = "Organization")
            EnhancedTextField(value = orgUnit, onValueChange = { orgUnit = it }, label = "Organizational Unit")
            EnhancedTextField(value = city, onValueChange = { city = it }, label = "City/Locality")
            EnhancedTextField(value = state, onValueChange = { state = it }, label = "State/Province")
            EnhancedTextField(value = country, onValueChange = { country = it }, label = "Country (2-letter code)")

            Spacer(Modifier.height(8.dp))
            ModernButton("Generate CSR", onClick = {
                val inputs = mapOf("Common Name" to commonName, "Organization" to organization)
                val result = SslCryptoService.generateCsr(commonName)
                SslLogManager.logOperation("CSR Generation", inputs, result)
            })
        }
    }
}

@Composable
private fun ReadCsrTab() {
    var csrData by remember { mutableStateOf("") }
    ModernCryptoCard(title = "Read CSR", subtitle = "Parse a Certificate Signing Request", icon = Icons.Default.FindInPage) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = csrData, onValueChange = { csrData = it }, label = "CSR Data (PEM Format)", maxLines = 15)
            ModernButton("Parse CSR", onClick = {
                SslLogManager.logOperation("Parse CSR", mapOf("CSR" to csrData), "Parsed Details:\n  Subject: CN=example.com\n  Signature Algorithm: SHA256withRSA")
            }, enabled = csrData.isNotBlank())
        }
    }
}

@Composable
private fun SelfSignedCertTab() {
    // Similar fields to CSR tab, plus validity
    var privateKey by remember { mutableStateOf("") }
    var signatureAlgorithm by remember { mutableStateOf("SHA256withRSA") }
    var validityDays by remember { mutableStateOf("365") }
    var commonName by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }

    ModernCryptoCard(title = "Self-Signed Certificate", subtitle = "Create a certificate for testing or development", icon = Icons.Default.VerifiedUser) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = privateKey, onValueChange = { privateKey = it }, label = "Private Key (PEM Format)", maxLines = 5)
            ModernDropdownField("Signature Algorithm", signatureAlgorithm, listOf("SHA256withRSA", "SHA512withRSA"), { signatureAlgorithm = if(it==0) "SHA256withRSA" else "SHA512withRSA" })
            EnhancedTextField(value = validityDays, onValueChange = { validityDays = it }, label = "Validity (Days)")
            Divider(Modifier.padding(vertical = 8.dp))
            EnhancedTextField(value = commonName, onValueChange = { commonName = it }, label = "Common Name (e.g., localhost)")
            EnhancedTextField(value = organization, onValueChange = { organization = it }, label = "Organization")
            ModernButton("Generate Self-Signed Certificate", onClick = {
                val result = "-----BEGIN CERTIFICATE-----\nSELF_SIGNED_CERT_FOR_${commonName}\n-----END CERTIFICATE-----"
                SslLogManager.logOperation("Self-Signed Generation", mapOf("Common Name" to commonName), result)
            })
        }
    }
}

@Composable
private fun ReadCertTab() {
    var certData by remember { mutableStateOf("") }
    ModernCryptoCard(title = "Read Certificate", subtitle = "Parse a certificate in PEM format", icon = Icons.Default.Plagiarism) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnhancedTextField(value = certData, onValueChange = { certData = it }, label = "Certificate Data (PEM Format)", maxLines = 15)
            ModernButton("Parse Certificate", onClick = {
                SslLogManager.logOperation("Parse Certificate", mapOf("Certificate" to certData), "Parsed Details:\n  Issuer: CN=My Test CA\n  Subject: CN=localhost\n  Valid Until: ...")
            }, enabled = certData.isNotBlank())
        }
    }
}


// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier.fillMaxWidth(), maxLines = maxLines)
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
