package `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.emvCrypto41

import ai.cortex.core.ValidationState
import ai.cortex.core.ValidationUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.VerifiedUser
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun UtilitiesTab(calculatorLogManager: CalculatorLogManager,calculatorTab: CalculatorTab) {
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