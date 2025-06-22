package `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.FieldValidation
import `in`.aicortex.iso8583studio.data.model.ValidationState
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.CalculatorLogManager
import `in`.aicortex.iso8583studio.ui.screens.components.CalculatorTab
import `in`.aicortex.iso8583studio.ui.screens.components.CalculatorView
import ai.cortex.core.types.CryptogramType
import ai.cortex.core.types.KeyParity
import ai.cortex.core.types.OperationType
import ai.cortex.core.types.PaddingMethods
import ai.cortex.core.types.UdkDerivationType
import io.cryptocalc.emv.calculators.emv41.EMVCalculatorInput
import io.cryptocalc.emv.calculators.emv41.Emv41CryptoCalculator
import io.cryptocalc.emv.calculators.emv41.UdkDerivationInput
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


// Enhanced EMV Tab enum
enum class EmvCryptoTabs(val title: String, val icon: ImageVector) {
    UDK_DERIVATION("UDK Derivation", Icons.Default.Key),
    SESSION_KEYS("Session Keys", Icons.Default.VpnKey),
    CRYPTOGRAM("Cryptogram", Icons.Default.Security),
    ARPC("ARPC", Icons.Default.Reply),
    UTILITIES("Utilities", Icons.Default.Build)
}




// Validation utility functions (keeping the same as before)
object ValidationUtils {

    fun validateHexString(
        value: String,
        expectedLength: Int? = null,
        allowEmpty: Boolean = false
    ): FieldValidation {
        if (value.isEmpty()) {
            return if (allowEmpty) {
                FieldValidation(ValidationState.EMPTY, "", "Enter hex characters")
            } else {
                FieldValidation(ValidationState.ERROR, "Field is required", "Enter hex characters")
            }
        }

        if (!value.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
            return FieldValidation(
                ValidationState.ERROR,
                "Only hex characters (0-9, A-F) allowed",
                "${value.length} characters"
            )
        }

        if (value.length % 2 != 0) {
            return FieldValidation(
                ValidationState.ERROR,
                "Hex string must have even number of characters",
                "${value.length} characters (needs even number)"
            )
        }

        expectedLength?.let { expected ->
            return when {
                value.length < expected -> FieldValidation(
                    ValidationState.ERROR,
                    "Must be exactly $expected characters",
                    "${value.length}/$expected characters"
                )

                value.length > expected -> FieldValidation(
                    ValidationState.ERROR,
                    "Must be exactly $expected characters",
                    "${value.length}/$expected characters (too long)"
                )

                else -> FieldValidation(
                    ValidationState.VALID,
                    "",
                    "${value.length}/$expected characters"
                )
            }
        }

        return FieldValidation(
            ValidationState.VALID,
            "",
            "${value.length} characters"
        )
    }

    fun validatePAN(pan: String): FieldValidation {
        if (pan.isEmpty()) {
            return FieldValidation(ValidationState.ERROR, "PAN is required", "Enter 13-19 digits")
        }

        if (!pan.all { it.isDigit() }) {
            return FieldValidation(
                ValidationState.ERROR,
                "PAN must contain only digits",
                "${pan.length} characters"
            )
        }

        if (pan.length !in 13..19) {
            return FieldValidation(
                ValidationState.ERROR,
                "PAN must be 13-19 digits",
                "${pan.length}/19 digits"
            )
        }

        // Luhn check
        val isLuhnValid = validateLuhn(pan)
        return if (isLuhnValid) {
            FieldValidation(
                ValidationState.VALID,
                "",
                "${pan.length}/19 digits - Luhn valid ✓"
            )
        } else {
            FieldValidation(
                ValidationState.WARNING,
                "Luhn checksum validation failed",
                "${pan.length}/19 digits - Luhn invalid ⚠"
            )
        }
    }

    fun validateNumericString(
        value: String,
        expectedLength: Int? = null,
        allowEmpty: Boolean = false
    ): FieldValidation {
        if (value.isEmpty()) {
            return if (allowEmpty) {
                FieldValidation(ValidationState.EMPTY, "", "Enter digits")
            } else {
                FieldValidation(
                    ValidationState.ERROR,
                    "Field is required",
                    "Enter digits"
                )
            }
        }

        if (!value.all { it.isDigit() }) {
            return FieldValidation(
                ValidationState.ERROR,
                "Only digits allowed",
                "${value.length} characters"
            )
        }

        expectedLength?.let {
            return when {
                value.length < expectedLength -> FieldValidation(
                    ValidationState.ERROR,
                    "Must be exactly $expectedLength digits",
                    "${value.length}/$expectedLength digits"
                )

                value.length > expectedLength -> FieldValidation(
                    ValidationState.ERROR,
                    "Must be exactly $expectedLength digits",
                    "${value.length}/$expectedLength digits (too long)"
                )

                else -> FieldValidation(
                    ValidationState.VALID,
                    "",
                    "${value.length}/$expectedLength digits"
                )
            }
        }

        return FieldValidation(
            ValidationState.VALID,
            "",
            "${value.length} digits"
        )
    }

    fun validateAlphanumeric(
        value: String,
        expectedLength: Int,
        allowEmpty: Boolean = false
    ): FieldValidation {
        if (value.isEmpty()) {
            return if (allowEmpty) {
                FieldValidation(ValidationState.EMPTY, "", "Enter $expectedLength characters")
            } else {
                FieldValidation(
                    ValidationState.ERROR,
                    "Field is required",
                    "Enter $expectedLength characters"
                )
            }
        }

        if (!value.all { it.isLetterOrDigit() }) {
            return FieldValidation(
                ValidationState.ERROR,
                "Only letters and digits allowed",
                "${value.length} characters"
            )
        }

        return when {
            value.length < expectedLength -> FieldValidation(
                ValidationState.ERROR,
                "Must be exactly $expectedLength characters",
                "${value.length}/$expectedLength characters"
            )

            value.length > expectedLength -> FieldValidation(
                ValidationState.ERROR,
                "Must be exactly $expectedLength characters",
                "${value.length}/$expectedLength characters (too long)"
            )

            else -> FieldValidation(
                ValidationState.VALID,
                "",
                "${value.length}/$expectedLength characters"
            )
        }
    }

    private fun validateLuhn(number: String): Boolean {
        if (number.length < 2) return false

        val digits = number.map { it.digitToInt() }
        var sum = 0
        var isSecond = false

        for (i in digits.size - 1 downTo 0) {
            var digit = digits[i]

            if (isSecond) {
                digit *= 2
                if (digit > 9) {
                    digit = digit / 10 + digit % 10
                }
            }

            sum += digit
            isSecond = !isSecond
        }

        return sum % 10 == 0
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EmvCrypto4_1(
    
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = EmvCryptoTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "EMV 4.1 Crypto Calculator",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { /* No action needed */ }) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Export Logs",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { /* No action needed */ }) {
                        Icon(
                            Icons.Default.Help,
                            contentDescription = "Help",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CalculatorView(
                tabList = listOf(
                    CalculatorTab(
                        title = "UDK Derviation",
                        icon = Icons.Default.Key,
                        content = { calculatorLogManager, tab ->
                            UdkDerivationTab(calculatorLogManager,tab)
                        }
                    ),
                    CalculatorTab(
                        title = "Session Keys",
                        icon = Icons.Default.VpnKey,
                        content = { calculatorLogManager, tab ->
                            SessionKeysTab(calculatorLogManager,tab)
                        }
                    ),
                    CalculatorTab(
                        title = "Cryptogram",
                        icon = Icons.Default.Security,
                        content = { calculatorLogManager, tab ->
                            CryptogramTab(calculatorLogManager,tab)
                        }
                    ),
                    CalculatorTab(
                        title = "ARPC",
                        icon = Icons.Default.Reply,
                        content = { calculatorLogManager, tab ->
                            ArpcTab(calculatorLogManager,tab)
                        }
                    ),
                    CalculatorTab(
                        title = "Utilities",
                        icon = Icons.Default.Build,
                        content = { calculatorLogManager, tab ->
                            UtilitiesTab(calculatorLogManager,tab)
                        }
                    )
                ),
                onTabSelected = { selectedTabIndex = it }
            )
        }
    }
}

@Composable
private fun UdkDerivationTab(calculatorLogManager: CalculatorLogManager,calculatorTab: CalculatorTab) {
    var mdk by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var pan by remember { mutableStateOf("43219876543210987") }
    var panSeq by remember { mutableStateOf("00") }
    var derivationOption by remember { mutableStateOf(UdkDerivationType.OPTION_A) }
    var keyParity by remember { mutableStateOf(KeyParity.ODD) }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }


    if (showInfoDialog) {
        InfoDialog(
            title = "UDK Derivation Calculation",
            onDismissRequest = { showInfoDialog = false }
        ) {
            Text("The Unique Derivation Key (UDK) is derived from the Master Derivation Key (MDK) using one of two methods specified in EMV standards:", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("Option A:", fontWeight = FontWeight.Bold)
            Text("1. The rightmost 16 digits of the PAN (excluding the Luhn check digit) are taken.", style = MaterialTheme.typography.caption)
            Text("2. This is concatenated with the 2-digit PAN Sequence Number.", style = MaterialTheme.typography.caption)
            Text("3. Two 8-byte blocks, Y1 and Y2, are formed from this data.", style = MaterialTheme.typography.caption)
            Text("4. The MDK is used to Triple-DES encrypt Y1 (giving Z1) and the complemented MDK is used to Triple-DES encrypt Y2 (giving Z2).", style = MaterialTheme.typography.caption)
            Text("5. The UDK is the concatenation of Z1 and Z2.", style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(8.dp))
            Text("Option B:", fontWeight = FontWeight.Bold)
            Text("Similar to Option A, but the encryption and complementation steps are swapped.", style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(8.dp))
            Text("Finally, key parity is applied to the resulting UDK to ensure each byte has an odd number of set bits.", style = MaterialTheme.typography.body2)
        }
    }

    val mdkValidation = ValidationUtils.validateHexString(mdk, 32)
    val panValidation = ValidationUtils.validatePAN(pan)
    val panSeqValidation = ValidationUtils.validateNumericString(panSeq, 2)

    val isFormValid = listOf(mdkValidation, panValidation, panSeqValidation)
        .none { it.state == ValidationState.ERROR }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModernCryptoCard(
            title = "UDK Derivation",
            subtitle = "Generate Unique Derived Key from Master Derivation Key",
            icon = Icons.Default.Key,
            onInfoClick = { showInfoDialog = true }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EnhancedTextField(
                    value = mdk,
                    onValueChange = {
                        if (it.length <= 32 && it.all { char -> char.isDigit() || char.uppercaseChar() in 'A'..'F' }) {
                            mdk = it.uppercase()
                        }
                    },
                    label = "Master Derivation Key (MDK)",
                    placeholder = "32 hex characters (16 bytes)",
                    validation = mdkValidation
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    EnhancedTextField(
                        value = pan,
                        onValueChange = {
                            if (it.length <= 19 && it.all { char -> char.isDigit() }) {
                                pan = it
                            }
                        },
                        label = "PAN",
                        placeholder = "Primary Account Number",
                        modifier = Modifier.weight(2f),
                        validation = panValidation
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    EnhancedTextField(
                        value = panSeq,
                        onValueChange = {
                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                panSeq = it.padStart(2, '0')
                            }
                        },
                        label = "PAN Sequence",
                        placeholder = "00-99",
                        modifier = Modifier.weight(1f),
                        validation = panSeqValidation
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernDropdownField(
                        label = "Derivation Option",
                        value = derivationOption.name,
                        options = UdkDerivationType.values().map { it.name },
                        onSelectionChanged = { index ->
                            derivationOption = UdkDerivationType.values()[index]
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ModernDropdownField(
                        label = "Key Parity",
                        value = keyParity.name,
                        options = KeyParity.values().map { it.name },
                        onSelectionChanged = { index ->
                            keyParity = KeyParity.values()[index]
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (panValidation.state == ValidationState.WARNING) {
                    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFFFFF3CD), elevation = 1.dp) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFF856404), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "PAN Luhn checksum failed, but calculation will proceed", style = MaterialTheme.typography.caption, color = Color(0xFF856404))
                        }
                    }
                }

                ModernButton(
                    text = "Calculate UDK",
                    onClick = {
                        isLoading = true
                        val inputs = mapOf(
                            "MDK" to mdk,
                            "PAN" to pan,
                            "PAN Sequence" to panSeq,
                            "Derivation Option" to derivationOption.name,
                            "Key Parity" to keyParity.name
                        )

                        GlobalScope.launch {
                            try {
                                val udkResult = Emv41CryptoCalculator().execute(
                                    EMVCalculatorInput(
                                        operation = OperationType.DERIVE,
                                        udkDerivationInput = UdkDerivationInput(
                                            udkDerivationType = derivationOption,
                                            keyParity = keyParity,
                                            masterKey = mdk,
                                            pan = pan,
                                            panSequence = panSeq
                                        )
                                    )
                                )
                                if(udkResult.success){
                                    val resultString = "UDK: ${udkResult.udkDerivation?.udk}\nKCV: ${udkResult.udkDerivation?.kcv}"

                                    calculatorLogManager.logOperation(
                                        tab = calculatorTab,
                                        operation = "UDK Derivation",
                                        inputs = inputs,
                                        result = resultString,
                                        executionTime = udkResult.metadata.executionTimeMs
                                    )
                                }else{
                                    calculatorLogManager.logOperation(
                                        tab = calculatorTab,
                                        operation = "UDK Derivation",
                                        inputs = inputs,
                                        error = udkResult.error,
                                        executionTime = udkResult.metadata.executionTimeMs
                                    )

                                }


                            } catch (e: Exception) {
                                calculatorLogManager.logOperation(
                                    tab = calculatorTab,
                                    operation = "UDK Derivation",
                                    inputs = inputs,
                                    error = e.message ?: "Unknown error occurred",
                                    executionTime = 0
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    isLoading = isLoading,
                    enabled = isFormValid,
                    icon = Icons.Default.Calculate,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SessionKeysTab(calculatorLogManager: CalculatorLogManager,calculatorTab: CalculatorTab) {
    var masterKey by remember { mutableStateOf("C8B507136D921FD05864C81F79F2D30B") }
    var initialVector by remember { mutableStateOf("0000000000000000") }
    var atc by remember { mutableStateOf("0001") }
    var branchFactor by remember { mutableStateOf("1") }
    var height by remember { mutableStateOf("0") }
    var keyParity by remember { mutableStateOf(KeyParity.NONE) }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }


    if (showInfoDialog) {
        InfoDialog(
            title = "Session Key Derivation Calculation",
            onDismissRequest = { showInfoDialog = false }
        ) {
            Text("The Session Key is derived from a Master Key (often a UDK) and transaction-specific data to ensure each transaction is cryptographically unique.", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("Common Session Key Derivation:", fontWeight = FontWeight.Bold)
            Text("1. Two 8-byte data blocks are formed using the 2-byte Application Transaction Counter (ATC) and other fixed values (e.g., F000, 00F0).", style = MaterialTheme.typography.caption)
            Text("2. The first block is encrypted with the Master Key using Triple-DES to get the left half of the session key.", style = MaterialTheme.typography.caption)
            Text("3. The second block is encrypted with the Master Key using Triple-DES to get the right half of the session key.", style = MaterialTheme.typography.caption)
            Text("4. These two halves are concatenated to form the 16-byte session key.", style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(8.dp))
            Text("Other fields like Initial Vector, Branch Factor, and Height are used in more complex, often proprietary, tree-based key derivation schemes like DUKPT.", style = MaterialTheme.typography.body2)

        }
    }


    val masterKeyValidation = ValidationUtils.validateHexString(masterKey, 32)
    val atcValidation = ValidationUtils.validateHexString(atc, 4)
    val ivValidation = ValidationUtils.validateHexString(initialVector, 16)
    val branchFactorValidation = ValidationUtils.validateNumericString(branchFactor, null)
    val heightValidation = ValidationUtils.validateNumericString(height, null)

    val isFormValid = listOf(
        masterKeyValidation,
        atcValidation,
        ivValidation,
        branchFactorValidation,
        heightValidation
    ).none { it.state == ValidationState.ERROR }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernCryptoCard(
            title = "Session Key Derivation",
            subtitle = "Generate session keys for EMV transactions",
            icon = Icons.Default.VpnKey,
            onInfoClick = { showInfoDialog = true }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                EnhancedTextField(
                    value = masterKey,
                    onValueChange = { if (it.length <= 32 && it.all { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }) masterKey = it.uppercase() },
                    label = "Master Key (UDK)",
                    placeholder = "32 hex characters",
                    validation = masterKeyValidation
                )

                EnhancedTextField(
                    value = initialVector,
                    onValueChange = { if (it.length <= 16 && it.all { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }) initialVector = it.uppercase() },
                    label = "Initial Vector (IV)",
                    placeholder = "16 hex characters",
                    validation = ivValidation
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EnhancedTextField(
                        value = atc,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }) atc = it.uppercase() },
                        label = "ATC",
                        placeholder = "4 hex chars",
                        modifier = Modifier.weight(1f),
                        validation = atcValidation
                    )

                    EnhancedTextField(
                        value = branchFactor,
                        onValueChange = { if (it.all { c -> c.isDigit() }) branchFactor = it },
                        label = "Branch Factor",
                        placeholder = "e.g. 1",
                        modifier = Modifier.weight(1f),
                        validation = branchFactorValidation
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EnhancedTextField(
                        value = height,
                        onValueChange = { if (it.all { c -> c.isDigit() }) height = it },
                        label = "Height",
                        placeholder = "e.g. 0",
                        modifier = Modifier.weight(1f),
                        validation = heightValidation
                    )
                    ModernDropdownField(
                        label = "Key Parity",
                        value = keyParity.name,
                        options = KeyParity.values().map { it.name },
                        onSelectionChanged = { index -> keyParity = KeyParity.values()[index] },
                        modifier = Modifier.weight(1f)
                    )
                }

                ModernButton(
                    text = "Generate Session Key",
                    onClick = {
                        isLoading = true
                        val startTime = System.currentTimeMillis()
                        val inputs = mapOf(
                            "Master Key" to masterKey,
                            "Initial Vector" to initialVector,
                            "ATC" to atc,
                            "Branch Factor" to branchFactor,
                            "Height" to height,
                            "Key Parity" to keyParity.name
                        )

                        GlobalScope.launch {
                            try {
                                // Mocked result based on new inputs
                                val result = "AABBCCDDEEFF00112233445566778899"
                                val kcv = "ABC123"
                                val executionTime = System.currentTimeMillis() - startTime
                                val resultString = "Session Key: $result\nKCV: $kcv"

                                calculatorLogManager.logOperation(
                                    tab = calculatorTab,
                                    operation = "Session Key Generation",
                                    inputs = inputs,
                                    result = resultString,
                                    executionTime = executionTime
                                )
                            } catch (e: Exception) {
                                val executionTime = System.currentTimeMillis() - startTime
                                calculatorLogManager.logOperation(
                                    tab = calculatorTab,
                                    operation = "Session Key Generation",
                                    inputs = inputs,
                                    error = e.message ?: "Unknown error occurred",
                                    executionTime = executionTime
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    isLoading = isLoading,
                    enabled = isFormValid,
                    icon = Icons.Default.GeneratingTokens,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CryptogramTab(calculatorLogManager: CalculatorLogManager,calculatorTab: CalculatorTab) {
    var sessionKey by remember { mutableStateOf("022551C4FDF76E45988089BA31DC077C") }
    var terminalData by remember { mutableStateOf("0000000010000000000000000710000000000007101302050030901B6A") }
    var iccData by remember { mutableStateOf("3C00000103A4A082") }
    var cryptogramType by remember { mutableStateOf(CryptogramType.ARQC) }
    var paddingMethod by remember { mutableStateOf(PaddingMethods.METHOD_1_ISO_9797) }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }


    if (showInfoDialog) {
        InfoDialog(
            title = "Cryptogram Generation Calculation",
            onDismissRequest = { showInfoDialog = false }
        ) {
            Text("The Application Cryptogram (ARQC, TC, or AAC) is a critical security element generated by the ICC (chip card). It is a MAC (Message Authentication Code) over transaction data, proving the card's authenticity.", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("Process:", fontWeight = FontWeight.Bold)
            Text("1. A block of data is constructed by concatenating relevant transaction details (e.g., amount, currency, terminal data, ICC dynamic data).", style = MaterialTheme.typography.caption)
            Text("2. This data block is padded according to the specified method (e.g., ISO 9797-1 Method 1 or 2) to be a multiple of 8 bytes.", style = MaterialTheme.typography.caption)
            Text("3. The padded data is encrypted using the derived Session Key with Triple-DES in CBC mode (Cipher Block Chaining). The Initial Vector is typically all zeros.", style = MaterialTheme.typography.caption)
            Text("4. The final 8-byte block of the encrypted result is the Application Cryptogram.", style = MaterialTheme.typography.caption)
        }
    }


    val isFormValid = listOf(
        ValidationUtils.validateHexString(sessionKey, 32),
        ValidationUtils.validateHexString(terminalData, null),
        ValidationUtils.validateHexString(iccData, null)
    ).none { it.state == ValidationState.ERROR } && terminalData.length >= 8 && iccData.length >= 4

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernCryptoCard(
            title = "Application Cryptogram",
            subtitle = "Generate AAC, ARQC, or TC cryptograms",
            icon = Icons.Default.Security,
            onInfoClick = { showInfoDialog = true }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EnhancedTextField(
                    value = sessionKey,
                    onValueChange = { if (it.length <= 32 && it.all { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }) sessionKey = it.uppercase() },
                    label = "Session Key",
                    placeholder = "32 hex characters",
                    validation = ValidationUtils.validateHexString(sessionKey, 32)
                )
                EnhancedTextField(
                    value = terminalData,
                    onValueChange = { if (it.all { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }) terminalData = it.uppercase() },
                    label = "Terminal Data",
                    placeholder = "Terminal verification results and other data",
                    maxLines = 3,
                    validation = ValidationUtils.validateHexString(terminalData, null)
                )
                EnhancedTextField(
                    value = iccData,
                    onValueChange = { if (it.all { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }) iccData = it.uppercase() },
                    label = "ICC Data",
                    placeholder = "ICC dynamic data",
                    validation = ValidationUtils.validateHexString(iccData, null)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernDropdownField(label = "Cryptogram Type", value = cryptogramType.name, options = CryptogramType.values().map { it.name }, onSelectionChanged = { index -> cryptogramType = CryptogramType.values()[index] }, modifier = Modifier.weight(1f))
                    ModernDropdownField(label = "Padding Method", value = paddingMethod.name, options = PaddingMethods.values().map { it.name }, onSelectionChanged = { index -> paddingMethod = PaddingMethods.values()[index] }, modifier = Modifier.weight(1f))
                }
                ModernButton(
                    text = "Generate ${cryptogramType.name}",
                    onClick = {
                        isLoading = true
                        val startTime = System.currentTimeMillis()
                        val inputs = mapOf(
                            "Session Key" to sessionKey,
                            "Terminal Data" to terminalData,
                            "ICC Data" to iccData,
                            "Cryptogram Type" to cryptogramType.name,
                            "Padding Method" to paddingMethod.name
                        )

                        GlobalScope.launch {
                            try {
                                val result = "92791D36B5CC31B5"
                                val executionTime = System.currentTimeMillis() - startTime
                                calculatorLogManager.logOperation(
                                    tab = calculatorTab,
                                    operation = "Cryptogram Generation",
                                    inputs = inputs,
                                    result = result,
                                    executionTime = executionTime
                                )
                            } catch (e: Exception) {
                                val executionTime = System.currentTimeMillis() - startTime
                                calculatorLogManager.logOperation(
                                    tab = calculatorTab,
                                    operation = "Cryptogram Generation",
                                    inputs = inputs,
                                    error = e.message ?: "Unknown error occurred",
                                    executionTime = executionTime
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    isLoading = isLoading,
                    enabled = isFormValid,
                    icon = Icons.Default.Shield,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ArpcTab(calculatorLogManager: CalculatorLogManager,calculatorTab: CalculatorTab) {
    var sessionKey by remember { mutableStateOf("022551C4FDF76E45988089BA31DC077C") }
    var transactionCryptogram by remember { mutableStateOf("92791D36B5CC31B5") }
    var responseCode by remember { mutableStateOf("Y3") }
    var arpcMethod by remember { mutableStateOf(CryptogramType.ARPC.methods[0]) }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }


    if (showInfoDialog) {
        InfoDialog(
            title = "ARPC Generation Calculation",
            onDismissRequest = { showInfoDialog = false }
        ) {
            Text("The Authorization Response Cryptogram (ARPC) is generated by the issuer/host and sent back to the terminal. It is a MAC over the card's cryptogram and the host's response, proving the host's authenticity to the card.", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("Method 1:", fontWeight = FontWeight.Bold)
            Text("1. The card's Transaction Cryptogram (ARQC) is XORed with the 2-byte Authorization Response Code (ARC, e.g., 'Y3' converted to hex) padded with zeros.", style = MaterialTheme.typography.caption)
            Text("2. The result of the XOR is encrypted with the Session Key using Triple-DES.", style = MaterialTheme.typography.caption)
            Text("3. The encrypted result is the ARPC.", style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(8.dp))
            Text("Method 2:", fontWeight = FontWeight.Bold)
            Text("A more complex method involving two separate Single-DES encryptions.", style = MaterialTheme.typography.caption)

        }
    }


    val isFormValid = listOf(
        ValidationUtils.validateHexString(sessionKey, 32),
        ValidationUtils.validateHexString(transactionCryptogram, 16),
        ValidationUtils.validateAlphanumeric(responseCode, 2)
    ).none { it.state == ValidationState.ERROR }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernCryptoCard(
            title = "ARPC Generation",
            subtitle = "Generate Authorization Response Cryptogram",
            icon = Icons.Default.Reply,
            onInfoClick = { showInfoDialog = true }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EnhancedTextField(
                    value = sessionKey,
                    onValueChange = { if (it.length <= 32 && it.all { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }) sessionKey = it.uppercase() },
                    label = "Session Key",
                    placeholder = "32 hex characters",
                    validation = ValidationUtils.validateHexString(sessionKey, 32)
                )
                EnhancedTextField(
                    value = transactionCryptogram,
                    onValueChange = { if (it.length <= 16 && it.all { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }) transactionCryptogram = it.uppercase() },
                    label = "Transaction Cryptogram",
                    placeholder = "ARQC/AAC/TC from card",
                    validation = ValidationUtils.validateHexString(transactionCryptogram, 16)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EnhancedTextField(
                        value = responseCode,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isLetterOrDigit() }) responseCode = it.uppercase() },
                        label = "Response Code",
                        placeholder = "e.g., Y3, 8A",
                        modifier = Modifier.weight(1f),
                        validation = ValidationUtils.validateAlphanumeric(responseCode, 2)
                    )
                    ModernDropdownField(label = "ARPC Method", value = arpcMethod.name, options = CryptogramType.ARPC.methods.map { it.name }, onSelectionChanged = { index -> arpcMethod = CryptogramType.ARPC.methods[index] }, modifier = Modifier.weight(1f))
                }
                ModernButton(
                    text = "Generate ARPC",
                    onClick = {
                        isLoading = true
                        val startTime = System.currentTimeMillis()
                        val inputs = mapOf(
                            "Session Key" to sessionKey,
                            "Transaction Cryptogram" to transactionCryptogram,
                            "Response Code" to responseCode,
                            "ARPC Method" to arpcMethod.name
                        )

                        GlobalScope.launch {
                            try {
                                val result = "1A2B3C4D5E6F7890"
                                val executionTime = System.currentTimeMillis() - startTime
                                calculatorLogManager.logOperation(
                                    tab = calculatorTab,
                                    operation = "ARPC Generation",
                                    inputs = inputs,
                                    result = result,
                                    executionTime = executionTime
                                )
                            } catch (e: Exception) {
                                val executionTime = System.currentTimeMillis() - startTime
                                calculatorLogManager.logOperation(
                                    tab = calculatorTab,
                                    operation = "ARPC Generation",
                                    inputs = inputs,
                                    error = e.message ?: "Unknown error occurred",
                                    executionTime = executionTime
                                )
                            } finally {
                                isLoading = false
                            }
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
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun UtilitiesTab(calculatorLogManager: CalculatorLogManager,calculatorTab: CalculatorTab) {
    var inputKey by remember { mutableStateOf("0123456789ABCDEF0123456789ABCDEF") }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        InfoDialog(
            title = "KCV Calculation",
            onDismissRequest = { showInfoDialog = false }
        ) {
            Text("The Key Check Value (KCV) is a way to verify that a cryptographic key has been entered or transmitted correctly without exposing the key itself.", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Text("Process:", fontWeight = FontWeight.Bold)
            Text("1. An 8-byte block of all zeros ('0000000000000000') is created.", style = MaterialTheme.typography.caption)
            Text("2. This block of zeros is encrypted using the key for which the KCV is needed (e.g., a 16-byte MDK). The encryption algorithm is typically Triple-DES.", style = MaterialTheme.typography.caption)
            Text("3. The Key Check Value is the first 3 bytes (6 hex characters) of the encrypted result.", style = MaterialTheme.typography.caption)
            Text("If two parties calculate the same KCV for a key, they can be confident they are both holding the same key.", style = MaterialTheme.typography.body2)
        }
    }


    val keyValidation = ValidationUtils.validateHexString(inputKey, 32)
    val isFormValid = keyValidation.state != ValidationState.ERROR

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernCryptoCard(
            title = "Cryptographic Utilities",
            subtitle = "Key validation and utility functions",
            icon = Icons.Default.Build,
            onInfoClick = { showInfoDialog = true }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EnhancedTextField(
                    value = inputKey,
                    onValueChange = { if (it.length <= 32 && it.all { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }) inputKey = it.uppercase() },
                    label = "Key (Hex)",
                    placeholder = "32 hex characters for KCV calculation",
                    validation = keyValidation
                )
                ModernButton(
                    text = "Calculate KCV",
                    onClick = {
                        isLoading = true
                        val startTime = System.currentTimeMillis()
                        val inputs = mapOf("Input Key" to inputKey)

                        GlobalScope.launch {
                            try {
                                val keyBytes = inputKey.hexToByteArray()
                                val adjustedKey = if (keyBytes.size == 16) keyBytes + keyBytes.copyOfRange(0, 8) else keyBytes
                                val kcvBytes = ""
                                val result = ""
                                val executionTime = System.currentTimeMillis() - startTime

                                calculatorLogManager.logOperation(
                                    tab = calculatorTab,
                                    operation = "KCV Calculation",
                                    inputs = inputs,
                                    result = result,
                                    executionTime = executionTime
                                )
                            } catch (e: Exception) {
                                val executionTime = System.currentTimeMillis() - startTime
                                calculatorLogManager.logOperation(
                                    tab = calculatorTab,
                                    operation = "KCV Calculation",
                                    inputs = inputs,
                                    error = e.message ?: "Unknown error occurred",
                                    executionTime = executionTime
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    isLoading = isLoading,
                    enabled = isFormValid,
                    icon = Icons.Default.VerifiedUser,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Enhanced UI Components (Unchanged)
@Composable
private fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    validation: FieldValidation
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = maxLines,
            singleLine = maxLines == 1,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (validation.message.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val (icon, color) = when (validation.state) {
                        ValidationState.ERROR -> Icons.Default.Error to MaterialTheme.colors.error
                        ValidationState.WARNING -> Icons.Default.Warning to Color(0xFF856404)
                        else -> null to MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    }

                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = validation.message,
                        color = color,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (validation.helperText.isNotEmpty()) {
                Text(
                    text = validation.helperText,
                    color = when (validation.state) {
                        ValidationState.VALID -> SuccessGreen
                        ValidationState.WARNING -> Color(0xFF856404)
                        ValidationState.ERROR -> MaterialTheme.colors.error
                        ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    },
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun ModernCryptoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onInfoClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 6.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (onInfoClick != null) {
                    IconButton(onClick = onInfoClick) {
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
private fun ModernDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                disabledTextColor = MaterialTheme.colors.onSurface,
                disabledLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        )

        // Invisible clickable overlay to expand the dropdown
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent)
                .clickable { expanded = !expanded }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.wrapContentWidth()
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    onClick = {
                        onSelectionChanged(index)
                        expanded = false
                    }
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ModernButton(
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
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primary,
            disabledBackgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        ),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            disabledElevation = 0.dp
        )
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                fadeIn(animationSpec = tween(150)) with fadeOut(animationSpec = tween(150))
            },
            label = "button_anim"
        ) { loading ->
            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoDialog(
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
            Button(onClick = onDismissRequest) {
                Text("OK")
            }
        },
        shape = MaterialTheme.shapes.medium
    )
}


// Custom tab indicator offset extension (Unchanged)
private fun Modifier.customTabIndicatorOffset(
    currentTabPosition: TabPosition
): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2

    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset)
        .width(indicatorWidth)
}
