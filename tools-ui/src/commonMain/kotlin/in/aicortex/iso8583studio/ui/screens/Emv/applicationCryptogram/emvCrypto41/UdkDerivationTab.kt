package `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.emvCrypto41

import ai.cortex.core.ValidationState
import ai.cortex.core.ValidationUtils
import ai.cortex.core.types.KeyParity
import ai.cortex.core.types.OperationType
import ai.cortex.core.types.UdkDerivationType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.screens.components.CalculatorLogManager
import `in`.aicortex.iso8583studio.ui.screens.components.CalculatorTab
import `in`.aicortex.iso8583studio.ui.screens.components.EnhancedTextField
import `in`.aicortex.iso8583studio.ui.screens.components.InfoDialog
import `in`.aicortex.iso8583studio.ui.screens.components.ModernButton
import `in`.aicortex.iso8583studio.ui.screens.components.ModernCryptoCard
import `in`.aicortex.iso8583studio.ui.screens.components.ModernDropdownField
import io.cryptocalc.emv.calculators.emv41.EMVCalculatorInput
import io.cryptocalc.emv.calculators.emv41.Emv41CryptoCalculator
import io.cryptocalc.emv.calculators.emv41.UdkDerivationInput
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
fun UdkDerivationTab(calculatorLogManager: CalculatorLogManager, calculatorTab: CalculatorTab) {
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