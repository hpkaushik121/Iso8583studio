package `in`.aicortex.iso8583studio.ui.screens.Emv.sda

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// --- COMMON UI & VALIDATION (Can be extracted to a shared module) ---


object SdaValidationUtils {
    fun validateHexString(
        value: String,
        allowEmpty: Boolean = false,
        friendlyName: String = "Field"
    ): ValidationResult {
        if (value.isEmpty()) {
            return if (allowEmpty) ValidationResult(ValidationState.EMPTY, "", "Enter hex characters")
            else ValidationResult(ValidationState.ERROR, "$friendlyName is required", "Enter hex characters")
        }
        if (!value.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed", "${value.length} chars")
        }
        if (value.length % 2 != 0) {
            return ValidationResult(ValidationState.ERROR, "Must have an even number of characters", "${value.length} chars")
        }
        return ValidationResult(ValidationState.VALID, "", "${value.length} chars")
    }
}

// --- SDA SCREEN ---

enum class SdaTabs(val title: String, val icon: ImageVector) {
    RETRIEVE_ISSUER_PK("Retrieve Issuer PK", Icons.Default.VpnKey),
    VERIFY_SSAD("Verify SSAD", Icons.Default.VerifiedUser)
}

object SdaLogManager {
    private val _logEntriesMap = mutableStateMapOf<String, SnapshotStateList<LogEntry>>()

    private fun getLogList(tabTitle: String): SnapshotStateList<LogEntry> {
        return _logEntriesMap.getOrPut(tabTitle) { mutableStateListOf() }
    }

    fun getLogEntries(tabTitle: String): SnapshotStateList<LogEntry> = getLogList(tabTitle)

    fun clearLogs(tabTitle: String) {
        getLogList(tabTitle).clear()
        addLog(
            tabTitle,
            LogEntry(
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
                type = LogType.INFO,
                message = "Log history for this tab cleared",
                details = ""
            )
        )
    }

    private fun addLog(tabTitle: String, entry: LogEntry) {
        val logList = getLogList(tabTitle)
        logList.add(0, entry)
        if (logList.size > 500) logList.removeRange(400, logList.size)
    }

    fun logOperation(
        tabTitle: String,
        operation: String,
        inputs: Map<String, String>,
        result: String? = null,
        error: String? = null,
        executionTime: Long = 0L
    ) {
        if (result == null && error == null) return

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                val displayValue = if (key.contains("key", ignoreCase = true) || key.contains("modulus", ignoreCase = true) || key.contains("certificate", ignoreCase = true)) {
                    "${value.take(16)}..."
                } else {
                    value
                }
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  ${it.replace("\n", "\n  ")}") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(tabTitle, LogEntry(timestamp = timestamp, type= logType, message = message, details = details))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SdaScreen(
    
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = SdaTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "SDA - Static Data Authentication",
                onBackClick = onBack,
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(modifier = Modifier.customTabIndicatorOffset(tabPositions[selectedTabIndex]), height = 3.dp, color = MaterialTheme.colors.primary)
                }
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
                        label = "sda_tab_transition"
                    ) { tab ->
                        when (tab) {
                            SdaTabs.RETRIEVE_ISSUER_PK -> RetrieveIssuerPkTab()
                            SdaTabs.VERIFY_SSAD -> VerifySsadTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { SdaLogManager.clearLogs(selectedTab.title) },
                            logEntries = SdaLogManager.getLogEntries(selectedTab.title)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RetrieveIssuerPkTab() {
    var caPkModulus by remember { mutableStateOf("996AF2472A5954B51393A369C52B355610266A0C83A6488A06626606AAB930B025C1E745348F2964C674212AB41A96FCD88960541E2400913A3543A45A62191942699318E237C8265F366A0E46313D46369242914041113B395856416A5480682245353597F5632D6E334E67E77E49E08611667A555989AD528F6E6577D56B388D38332A3326A2A84F8D95E397B6B5994F389979392D418A331A7") }
    var caPkExponent by remember { mutableStateOf("03") }
    var issuerPkCert by remember { mutableStateOf("9125D77263204A81E833215266A233342933391A25C84931A99E335424D996328A69335552A339343339304314A51A33439433333033A334354333333394326") }
    var issuerPkRemainder by remember { mutableStateOf("A255A33333334335") }
    var issuerPkExponent by remember { mutableStateOf("03") }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(title = "Retrieve Issuer Public Key", onDismissRequest = { showInfoDialog = false }) {
            Text("This process recovers the Issuer's Public Key, which is used to verify the card's authenticity. It's a fundamental step in offline card authentication.", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("Process:", fontWeight = FontWeight.Bold)
            Text("1. The Issuer PK Certificate is decrypted using the Certification Authority (CA) Public Key (Modulus and Exponent) via RSA.", style = MaterialTheme.typography.caption)
            Text("2. The decrypted data is parsed to verify its format and check for consistency (e.g., header, trailer, certificate format).", style = MaterialTheme.typography.caption)
            Text("3. The Issuer's Public Key is reconstructed by concatenating the recovered part from the certificate with the Issuer PK Remainder.", style = MaterialTheme.typography.caption)
            Text("4. A SHA-1 hash is computed on parts of the decrypted data and compared with the hash included in the certificate to ensure integrity.", style = MaterialTheme.typography.caption)
        }
    }

    val isFormValid = listOf(caPkModulus, caPkExponent, issuerPkCert, issuerPkExponent).all { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernCryptoCard(
            title = "Retrieve Issuer Public Key",
            subtitle = "Recover the Issuer PK using the CA Public Key",
            icon = Icons.Default.VpnKey,
            onInfoClick = { showInfoDialog = true }
        ) {
            EnhancedTextField(value = caPkModulus, onValueChange = { caPkModulus = it.uppercase() }, label = "CA PK Modulus", validation = SdaValidationUtils.validateHexString(caPkModulus), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = caPkExponent, onValueChange = { caPkExponent = it.uppercase() }, label = "CA PK Exponent", validation = SdaValidationUtils.validateHexString(caPkExponent))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = issuerPkCert, onValueChange = { issuerPkCert = it.uppercase() }, label = "Issuer PK Certificate", validation = SdaValidationUtils.validateHexString(issuerPkCert), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = issuerPkRemainder, onValueChange = { issuerPkRemainder = it.uppercase() }, label = "Issuer PK Remainder (Optional)", validation = SdaValidationUtils.validateHexString(issuerPkRemainder, allowEmpty = true))
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = issuerPkExponent, onValueChange = { issuerPkExponent = it.uppercase() }, label = "Issuer PK Exponent", validation = SdaValidationUtils.validateHexString(issuerPkExponent))
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Retrieve Key",
                onClick = {
                    isLoading = true
                    val inputs = mapOf("CA PK Modulus" to caPkModulus, "Issuer PK Certificate" to issuerPkCert)
                    // Mock operation
                    GlobalScope.launch {
                        delay(500)
                        val result = "Recovered Issuer PK Modulus: B0...\nSHA-1 Hash: Verified"
                        SdaLogManager.logOperation(SdaTabs.RETRIEVE_ISSUER_PK.title, "Retrieve Issuer PK", inputs, result = result, executionTime = 512)
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                enabled = isFormValid,
                icon = Icons.Default.EnhancedEncryption,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun VerifySsadTab() {
    var issuerPkModulus by remember { mutableStateOf("B0F39937E69A731F29295A23933C5233266A2346694D6A2333339333833313936634346333333933333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333") }
    var ssad by remember { mutableStateOf("29295A23933C5233266A2346694D6A233333933383331393663434633333339333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333.BC") }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(title = "Verify Signed Static Application Data (SSAD)", onDismissRequest = { showInfoDialog = false }) {
            Text("This process verifies the SSAD, which is a digital signature created by the issuer over static card data. It proves that the card data has not been tampered with and originates from a legitimate issuer.", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("Process:", fontWeight = FontWeight.Bold)
            Text("1. The SSAD is decrypted using the Issuer Public Key via RSA.", style = MaterialTheme.typography.caption)
            Text("2. The decrypted data is parsed to verify its format (header, trailer, data format markers).", style = MaterialTheme.typography.caption)
            Text("3. A SHA-1 hash is computed on the static application data that was signed (e.g., PAN, cardholder name, etc., which are not fields here but part of the process).", style = MaterialTheme.typography.caption)
            Text("4. The calculated hash is compared with the hash recovered from the decrypted SSAD. A match confirms the data's integrity and authenticity.", style = MaterialTheme.typography.caption)
        }
    }

    val isFormValid = listOf(issuerPkModulus, ssad).all { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernCryptoCard(
            title = "Verify SSAD",
            subtitle = "Verify Signed Static Application Data",
            icon = Icons.Default.VerifiedUser,
            onInfoClick = { showInfoDialog = true }
        ) {
            EnhancedTextField(value = issuerPkModulus, onValueChange = { issuerPkModulus = it.uppercase() }, label = "Issuer PK Modulus", validation = SdaValidationUtils.validateHexString(issuerPkModulus), maxLines = 4)
            Spacer(Modifier.height(4.dp))
            EnhancedTextField(value = ssad, onValueChange = { ssad = it.uppercase() }, label = "SSAD (Signed Static Data)", validation = SdaValidationUtils.validateHexString(ssad), maxLines = 4)
            Spacer(Modifier.height(8.dp))
            ModernButton(
                text = "Verify SSAD",
                onClick = {
                    isLoading = true
                    val inputs = mapOf("Issuer PK Modulus" to issuerPkModulus, "SSAD" to ssad)
                    // Mock operation
                    GlobalScope.launch {
                        delay(500)
                        val result = "SSAD Verification Successful\nSHA-1 Hash: Match"
                        SdaLogManager.logOperation(SdaTabs.VERIFY_SSAD.title, "Verify SSAD", inputs, result = result, executionTime = 520)
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                enabled = isFormValid,
                icon = Icons.Default.Security,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- SHARED UI COMPONENTS (COPIED FOR SDA SCREEN) ---
@Composable
fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    validation: ValidationResult
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = maxLines,
            isError = validation.state == ValidationState.ERROR,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.primary
                    ValidationState.WARNING -> Color(0xFFFFC107)
                    ValidationState.ERROR -> MaterialTheme.colors.error
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                },
                unfocusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    ValidationState.WARNING -> Color(0xFFFFC107)
                    ValidationState.ERROR -> MaterialTheme.colors.error
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                }
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (validation.message.isNotEmpty()) {
                Text(
                    text = validation.message,
                    color = when (validation.state) {
                        ValidationState.ERROR -> MaterialTheme.colors.error
                        ValidationState.WARNING -> Color(0xFF856404)
                        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    },
                    style = MaterialTheme.typography.caption
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = validation.helperText,
                color = when (validation.state) {
                    ValidationState.VALID -> SuccessGreen
                    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                },
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun ModernCryptoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onInfoClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled && !isLoading,
        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp, disabledElevation = 0.dp)
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn() with fadeOut() }
        ) { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
fun InfoDialog(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colors.primary)
                Spacer(Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            SelectionContainer {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    content()
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) { Text("OK") }
        },
        shape = MaterialTheme.shapes.medium
    )
}

fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2

    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset)
        .width(indicatorWidth)
}
