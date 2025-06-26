package `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.emvCrypto41

import ai.cortex.core.IsoUtil
import ai.cortex.core.ValidationState
import ai.cortex.core.ValidationUtils
import ai.cortex.core.types.KeyParity
import ai.cortex.core.types.OperationType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.screens.components.CalculatorLogManager
import `in`.aicortex.iso8583studio.ui.screens.components.CalculatorTab
import `in`.aicortex.iso8583studio.ui.screens.components.EnhancedTextField
import `in`.aicortex.iso8583studio.ui.screens.components.InfoDialog
import `in`.aicortex.iso8583studio.ui.screens.components.ModernButton
import `in`.aicortex.iso8583studio.ui.screens.components.ModernCryptoCard
import `in`.aicortex.iso8583studio.ui.screens.components.ModernDropdownField
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.emv.calculators.emv41.EMVCalculatorInput
import io.cryptocalc.emv.calculators.emv41.Emv41CryptoCalculator
import io.cryptocalc.emv.calculators.emv41.SessionKeyInput
import io.cryptocalc.emv.calculators.emv41.SessionKeyType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@Composable
fun SessionKeysTab(calculatorLogManager: CalculatorLogManager,calculatorTab: CalculatorTab) {
    var masterKey by remember { mutableStateOf("C9B406126D931ED05965C81E78F3D20B") }
    var initialVector by remember { mutableStateOf("00000000000000000000000000000000") }
    var atc by remember { mutableStateOf("0003") }
    var branchFactor by remember { mutableStateOf("50") }
    var height by remember { mutableStateOf("8") }
    var keyParity by remember { mutableStateOf(KeyParity.ODD) }
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
            Text("For more information see:\n" +
                    "        - EMV 4.1 Book 2 Annex A 1.3 Session Key Derivation\n" +
                    "        - EMV 4.1 Book 2 Annex A 1.3.1 Description\n" +
                    "        - EMV 4.1 Book 2 Annex A 1.3.2 Implementation\n" +
                    "\n" +
                    "    This method was replaced by common session key derivation in 2005\n" +
                    "    and should not be used for new development.\n" +
                    "    See EMVCo specification update bulletin 46 (SU-46).\n" +
                    "\n" +
                    "    Recommended branch factor and tree height combinations are as follow.\n" +
                    "    Both combinations produce enough session keys for every possible ATC value.\n" +
                    "        - Branch factor 2 and tree height 16\n" +
                    "        - Branch factor 4 and tree height 8", style = MaterialTheme.typography.body2)

        }
    }


    val masterKeyValidation = ValidationUtils.validateHexString(masterKey, 32)
    val atcValidation = ValidationUtils.validateHexString(atc, 4)
    val ivValidation = ValidationUtils.validateHexString(initialVector, 32)
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
                    onValueChange = { if (it.length <= 32 && it.all { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }) initialVector = it.uppercase() },
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
                                val calculator = Emv41CryptoCalculator()
                                val result = calculator.execute(
                                    input = EMVCalculatorInput(
                                        operation = OperationType.SESSION,
                                        sessionKeyInput = SessionKeyInput(
                                            masterKey = IsoUtil.hexStringToBytes(masterKey),
                                            iv = IsoUtil.hexStringToBytes(initialVector),
                                            atc = atc,
                                            branchFactor = branchFactor.toIntOrNull() ?: 1,
                                            height = height.toIntOrNull() ?: 0,
                                            keyParity = keyParity,
                                            sessionKeyType = SessionKeyType.APPLICATION_CRYPTOGRAM
                                        )
                                    )
                                )
                                val resultString = "Session Key: ${result.sessionDerivation?.sessionKey}\nKCV: ${result.sessionDerivation?.kcv}"

                                calculatorLogManager.logOperation(
                                    tab = calculatorTab,
                                    operation = "Session Key Generation",
                                    inputs = inputs,
                                    result = resultString,
                                    executionTime = result.metadata.executionTimeMs
                                )
                            } catch (e: Exception) {
                                calculatorLogManager.logOperation(
                                    tab = calculatorTab,
                                    operation = "Session Key Generation",
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
                    icon = Icons.Default.GeneratingTokens,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}