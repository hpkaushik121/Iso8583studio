package `in`.aicortex.iso8583studio.ui.screens.payments.dukpt

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import io.cryptocalc.crypto.engines.encryption.AesCalculatorEngine
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// --- COMMON UI & VALIDATION FOR THIS SCREEN ---

object DUKPTAESValidationUtils {
    fun getExpectedLength(keyType: String): Int {
        return when (keyType) {
            "AES-128", "2TDEA" -> 32
            "AES-192", "3TDEA" -> 48
            "AES-256" -> 64
            else -> 0
        }
    }

    fun validate(value: String, fieldName: String, keyType: String? = null): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$fieldName cannot be empty.")

        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "$fieldName must be valid hexadecimal.")
        }
        if (value.length % 2 != 0) {
            return ValidationResult(ValidationState.ERROR, "$fieldName must have an even number of characters.")
        }

        keyType?.let {
            val expectedLength = getExpectedLength(it)
            if (expectedLength > 0 && value.length != expectedLength) {
                return ValidationResult(ValidationState.ERROR, "$fieldName for $keyType must be $expectedLength characters long.")
            }
        }

        return ValidationResult(ValidationState.VALID)
    }
}


// --- DUKPT AES SCREEN ---

object DUKPTAESLogManager {
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

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                append("  $key: $value\n")
            }
            result?.let { append("\nResult:\n$it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

/**
 * AES DUKPT Service — ANSI X9.24-3:2017
 *
 * Key derivation hierarchy:
 *   BDK → IK (Initial Key) via NIST SP 800-108 CMAC-KDF
 *   IK  → Transaction Key via DUKPT tree (CMAC-based NRKGP)
 *   TK  → Working Keys via key usage derivation:
 *         PIN Encryption  (keyUsage = 0x1000)
 *         MAC Generation  (keyUsage = 0x2000)
 *         Data Encryption (keyUsage = 0x3000)
 */
object DUKPTAESService {

    // ── Key Usage constants per ANSI X9.24-3 Table 3 ──
    private const val KEY_DERIVATION: Int = 0x0000   // intermediate derivation
    private const val PIN_ENCRYPTION: Int = 0x1000
    private const val MAC_GENERATION: Int = 0x2000
    private const val DATA_ENCRYPTION_ENCRYPT: Int = 0x3000
    private const val DATA_ENCRYPTION_DECRYPT: Int = 0x3001
    private const val DATA_ENCRYPTION_BOTH: Int = 0x3002

    // ── NIST SP 800-108 CMAC-KDF (counter mode) ──
    // derivation_data = [01] || key_usage(2) || separator(1) || key_derivation_key_length(2)
    private fun deriveKeyFromKdk(kdk: ByteArray, keyUsage: Int, derivedKeyLen: Int = kdk.size): ByteArray {
        val L = derivedKeyLen * 8 // length in bits
        val blocksNeeded = (derivedKeyLen + 15) / 16
        val derived = ByteArray(blocksNeeded * 16)
        for (counter in 1..blocksNeeded) {
            val derivationData = byteArrayOf(
                counter.toByte(),                            // counter [1]
                (keyUsage shr 8).toByte(),                   // key usage high byte
                (keyUsage and 0xFF).toByte(),                // key usage low byte
                0x00,                                        // separator
                (L shr 8).toByte(),                          // derived key length (bits) high
                (L and 0xFF).toByte()                        // derived key length (bits) low
            )
            val block = AesCalculatorEngine.computeCmac(derivationData, kdk)
            block.copyInto(derived, (counter - 1) * 16)
        }
        return derived.copyOf(derivedKeyLen)
    }

    // ── BDK → Initial Key (IK) ──
    // IK = KDF(BDK, derivation_data) where derivation_data includes the Initial Key ID (IKSN)
    private fun deriveInitialKey(bdk: ByteArray, ksn: ByteArray, ikLen: Int = bdk.size): ByteArray {
        // IKSN = KSN with counter bits zeroed (last 4 bytes, low 32 bits of counter)
        val iksn = ksn.copyOf()
        // For AES DUKPT: 32-bit counter in the rightmost 4 bytes
        // Zero out the counter: last 4 bytes
        val ksnLen = iksn.size
        iksn[ksnLen - 4] = 0
        iksn[ksnLen - 3] = 0
        iksn[ksnLen - 2] = 0
        iksn[ksnLen - 1] = 0

        // Derivation data for IK: [01] || IKSN(8) || KEY_DERIVATION(2) || separator(1) || L(2)
        val L = ikLen * 8
        val blocksNeeded = (ikLen + 15) / 16
        val derived = ByteArray(blocksNeeded * 16)
        for (counter in 1..blocksNeeded) {
            val derivationData = byteArrayOf(counter.toByte()) + iksn +
                byteArrayOf(
                    (KEY_DERIVATION shr 8).toByte(), (KEY_DERIVATION and 0xFF).toByte(),
                    0x00,
                    (L shr 8).toByte(), (L and 0xFF).toByte()
                )
            val block = AesCalculatorEngine.computeCmac(derivationData, bdk)
            block.copyInto(derived, (counter - 1) * 16)
        }
        return derived.copyOf(ikLen)
    }

    // ── IK → Transaction Key via DUKPT tree ──
    // Walk the counter bits from MSB to LSB, deriving intermediate keys
    private fun deriveTransactionKey(ik: ByteArray, ksn: ByteArray): ByteArray {
        val ksnLen = ksn.size
        // Extract the 32-bit counter from the last 4 bytes
        val counter = ((ksn[ksnLen - 4].toInt() and 0xFF) shl 24) or
                ((ksn[ksnLen - 3].toInt() and 0xFF) shl 16) or
                ((ksn[ksnLen - 2].toInt() and 0xFF) shl 8) or
                (ksn[ksnLen - 1].toInt() and 0xFF)

        // Build KSN with counter zeroed for tree walking
        val baseKsn = ksn.copyOf()
        baseKsn[ksnLen - 4] = 0
        baseKsn[ksnLen - 3] = 0
        baseKsn[ksnLen - 2] = 0
        baseKsn[ksnLen - 1] = 0

        var currentKey = ik.copyOf()

        // Walk each set bit from MSB to LSB (32-bit counter)
        for (shiftReg in 31 downTo 0) {
            val bit = 1 shl shiftReg
            if (counter and bit != 0) {
                // Set this bit in baseKsn counter
                val currentCounter = ((baseKsn[ksnLen - 4].toInt() and 0xFF) shl 24) or
                        ((baseKsn[ksnLen - 3].toInt() and 0xFF) shl 16) or
                        ((baseKsn[ksnLen - 2].toInt() and 0xFF) shl 8) or
                        (baseKsn[ksnLen - 1].toInt() and 0xFF)
                val newCounter = currentCounter or bit
                baseKsn[ksnLen - 4] = (newCounter shr 24).toByte()
                baseKsn[ksnLen - 3] = (newCounter shr 16).toByte()
                baseKsn[ksnLen - 2] = (newCounter shr 8).toByte()
                baseKsn[ksnLen - 1] = newCounter.toByte()

                // Derive next level key: KDF(currentKey, baseKsn)
                currentKey = deriveIntermediateKey(currentKey, baseKsn)
            }
        }
        return currentKey
    }

    // Derive intermediate/leaf key using CMAC-KDF with KSN as derivation context
    private fun deriveIntermediateKey(parentKey: ByteArray, ksn: ByteArray): ByteArray {
        val keyLen = parentKey.size
        val L = keyLen * 8
        val blocksNeeded = (keyLen + 15) / 16
        val derived = ByteArray(blocksNeeded * 16)
        for (counter in 1..blocksNeeded) {
            val derivationData = byteArrayOf(counter.toByte()) + ksn +
                byteArrayOf(
                    (KEY_DERIVATION shr 8).toByte(), (KEY_DERIVATION and 0xFF).toByte(),
                    0x00,
                    (L shr 8).toByte(), (L and 0xFF).toByte()
                )
            val block = AesCalculatorEngine.computeCmac(derivationData, parentKey)
            block.copyInto(derived, (counter - 1) * 16)
        }
        return derived.copyOf(keyLen)
    }

    // ── Public API ──

    fun deriveKeys(
        baseKey: String, ksn: String, inputKeyDesignation: String,
        initialKeyType: String, workingKeyType: String
    ): Map<String, String> {
        val baseKeyBytes = baseKey.decodeHex()
        val ksnBytes = ksn.decodeHex()

        // Step 1: Get Initial Key
        val ik = if (inputKeyDesignation == "IK") {
            baseKeyBytes
        } else {
            deriveInitialKey(baseKeyBytes, ksnBytes, baseKeyBytes.size)
        }

        // Step 2: Derive Transaction Key from IK via tree
        val txKey = deriveTransactionKey(ik, ksnBytes)

        // Step 3: Derive working keys from Transaction Key
        val workingKeyLen = DUKPTAESValidationUtils.getExpectedLength(workingKeyType) / 2
        val pek = deriveKeyFromKdk(txKey, PIN_ENCRYPTION, workingKeyLen)
        val macKey = deriveKeyFromKdk(txKey, MAC_GENERATION, workingKeyLen)
        val dek = deriveKeyFromKdk(txKey, DATA_ENCRYPTION_BOTH, workingKeyLen)

        return mapOf(
            "Initial Key (IK)" to ik.toHex().uppercase(),
            "IK KCV" to computeKcv(ik),
            "Transaction Key" to txKey.toHex().uppercase(),
            "TK KCV" to computeKcv(txKey),
            "PIN Encryption Key (PEK)" to pek.toHex().uppercase(),
            "PEK KCV" to computeKcv(pek),
            "MAC Key" to macKey.toHex().uppercase(),
            "MAC KCV" to computeKcv(macKey),
            "Data Encryption Key (DEK)" to dek.toHex().uppercase(),
            "DEK KCV" to computeKcv(dek)
        )
    }

    private fun computeKcv(key: ByteArray): String {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(ByteArray(16)).toHex().uppercase().take(6)
    }

    // ── PIN: AES ECB ──

    fun encryptPinBlock(pek: String, pinBlock: String): String {
        val pekBytes = pek.decodeHex()
        val pinBlockBytes = pinBlock.decodeHex()
        val padded = if (pinBlockBytes.size % 16 != 0) {
            pinBlockBytes + ByteArray(16 - pinBlockBytes.size % 16)
        } else pinBlockBytes
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(pekBytes, "AES"))
        return cipher.doFinal(padded).toHex().uppercase()
    }

    fun decryptPinBlock(pek: String, encryptedPinBlock: String): String {
        val pekBytes = pek.decodeHex()
        val encBytes = encryptedPinBlock.decodeHex()
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(pekBytes, "AES"))
        return cipher.doFinal(encBytes).toHex().uppercase()
    }

    // ── MAC: AES-CMAC (RFC 4493) ──

    fun generateMac(macKey: String, data: String): String {
        val keyBytes = macKey.decodeHex()
        val dataBytes = data.decodeHex()
        val cmac = AesCalculatorEngine.computeCmac(dataBytes, keyBytes)
        return cmac.toHex().uppercase()
    }

    // ── Data: AES-CBC with zero IV ──

    fun encryptData(dek: String, data: String, inputType: String): String {
        val dataBytes = if (inputType == "ASCII") data.toByteArray() else data.decodeHex()
        val dekBytes = dek.decodeHex()
        val padded = if (dataBytes.size % 16 != 0) {
            dataBytes + ByteArray(16 - dataBytes.size % 16)
        } else dataBytes
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(dekBytes, "AES"), IvParameterSpec(ByteArray(16)))
        return cipher.doFinal(padded).toHex().uppercase()
    }

    fun decryptData(dek: String, data: String): String {
        val dataBytes = data.decodeHex()
        val dekBytes = dek.decodeHex()
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(dekBytes, "AES"), IvParameterSpec(ByteArray(16)))
        return cipher.doFinal(dataBytes).toHex().uppercase()
    }

    // ── Helpers ──

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }
}

enum class DUKPTAESTabs(val title: String, val icon: ImageVector) {
    KEY_DERIVATION("Key Derivation", Icons.Default.VpnKey),
    DUKPT_PIN("DUKPT PIN", Icons.Default.Pin),
    DUKPT_MAC("DUKPT MAC", Icons.Default.Verified),
    DUKPT_DATA("DUKPT Data", Icons.Default.SyncAlt)
}

private class AesKeyDerivationState {
    var selectedInputKeyDesignation by mutableStateOf("BDK")
    var selectedInitialKeyType by mutableStateOf("AES-128")
    var selectedWorkingKeyType by mutableStateOf("AES-128")
    var bdk by mutableStateOf("")
    var ksn by mutableStateOf("")
    var isLoading by mutableStateOf(false)
}

private class AesPinState {
    var pek by mutableStateOf("")
    var pinBlock by mutableStateOf("")
    var isLoading by mutableStateOf(false)
}

private class AesMacState {
    var macKey by mutableStateOf("")
    var data by mutableStateOf("")
    var isLoading by mutableStateOf(false)
}

private class AesDataState {
    var dek by mutableStateOf("")
    var selectedDataType by mutableStateOf("ASCII")
    var data by mutableStateOf("")
    var isLoading by mutableStateOf(false)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DUKPTAESScreen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = DUKPTAESTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]
    val keyDerivState = remember { AesKeyDerivationState() }
    val pinState = remember { AesPinState() }
    val macState = remember { AesMacState() }
    val dataState = remember { AesDataState() }

    Scaffold(
        topBar = { AppBarWithBack(title = "DUKPT AES Utilities", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary,
            ) {
                tabList.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Icon(imageVector = tab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tab.title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                            }
                        },
                    )
                }
            }
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn()) with
                                    (slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "dukpt_aes_tab_transition"
                    ) { tab ->
                        when (tab) {
                            DUKPTAESTabs.KEY_DERIVATION -> KeyDerivationCard(keyDerivState)
                            DUKPTAESTabs.DUKPT_PIN -> DukptPinCard(pinState)
                            DUKPTAESTabs.DUKPT_MAC -> DukptMacCard(macState)
                            DUKPTAESTabs.DUKPT_DATA -> DukptDataCard(dataState)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { DUKPTAESLogManager.clearLogs() },
                            logEntries = DUKPTAESLogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyDerivationCard(state: AesKeyDerivationState) { with(state) {
    val inputKeyDesignations = remember { listOf("BDK", "IK") }

    val initialKeyTypes = remember { listOf("AES-128", "AES-192", "AES-256") }

    val workingKeyTypes = remember { listOf("2TDEA", "3TDEA", "AES-128", "AES-192", "AES-256") }

    val bdkValidation = DUKPTAESValidationUtils.validate(bdk, "BDK", selectedInitialKeyType)
    val ksnValidation = DUKPTAESValidationUtils.validate(ksn, "KSN")

    val isFormValid = bdk.isNotBlank() && ksn.isNotBlank() &&
            bdkValidation.state != ValidationState.ERROR && ksnValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "AES Key Derivation", subtitle = "Derive working keys from a Base Key", icon = DUKPTAESTabs.KEY_DERIVATION.icon) {
        ModernDropdownField("Input Key Designation", selectedInputKeyDesignation, inputKeyDesignations) { selectedInputKeyDesignation = inputKeyDesignations[it] }
        Spacer(Modifier.height(12.dp))
        ModernDropdownField("Initial Key Type", selectedInitialKeyType, initialKeyTypes) { selectedInitialKeyType = initialKeyTypes[it] }
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = bdk, onValueChange = { bdk = it }, label = "BDK / IK", validation = bdkValidation)
        Spacer(Modifier.height(12.dp))
        ModernDropdownField("Working Key Type", selectedWorkingKeyType, workingKeyTypes) { selectedWorkingKeyType = workingKeyTypes[it] }
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = ksn, onValueChange = { ksn = it }, label = "KSN", validation = ksnValidation)
        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Derive Keys", onClick = {
                isLoading = true
                val inputs = mapOf(
                    "Input Key Designation" to selectedInputKeyDesignation,
                    "Initial Key Type" to selectedInitialKeyType,
                    "BDK/IK" to bdk,
                    "Working Key Type" to selectedWorkingKeyType,
                    "KSN" to ksn
                )
                GlobalScope.launch {
                    delay(200)
                    try {
                        val derivedKeys = DUKPTAESService.deriveKeys(bdk, ksn, selectedInputKeyDesignation, selectedInitialKeyType, selectedWorkingKeyType)
                        val resultString = derivedKeys.entries.joinToString(separator = "\n") { "  ${it.key}: ${it.value}" }
                        DUKPTAESLogManager.logOperation("Key Derivation", inputs, result = resultString, executionTime = 205)
                    } catch (e: Exception) {
                        DUKPTAESLogManager.logOperation("Key Derivation", inputs, error = e.message, executionTime = 205)
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.ArrowForward, modifier = Modifier.fillMaxWidth()
        )
    }
}}

@Composable
private fun DukptPinCard(state: AesPinState) { with(state) {

    val pekValidation = DUKPTAESValidationUtils.validate(pek, "PEK")
    val pinBlockValidation = DUKPTAESValidationUtils.validate(pinBlock, "PIN Block")

    val isFormValid = pek.isNotBlank() && pinBlock.isNotBlank() &&
            pekValidation.state != ValidationState.ERROR && pinBlockValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "DUKPT AES PIN", subtitle = "Encrypt or Decrypt a PIN block", icon = DUKPTAESTabs.DUKPT_PIN.icon) {
        EnhancedTextField(value = pek, onValueChange = { pek = it }, label = "PEK (Pin Encryption Key)", validation = pekValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = pinBlock, onValueChange = { pinBlock = it }, label = "PIN Block", validation = pinBlockValidation)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernButton(
                text = "Encrypt", onClick = {
                    isLoading = true
                    val inputs = mapOf("PEK" to pek, "PIN Block" to pinBlock)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = DUKPTAESService.encryptPinBlock(pek, pinBlock)
                            DUKPTAESLogManager.logOperation("PIN Encryption", inputs, result = "  Encrypted PIN Block: $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTAESLogManager.logOperation("PIN Encryption", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Lock, modifier = Modifier.weight(1f)
            )
            ModernButton(
                text = "Decrypt", onClick = {
                    isLoading = true
                    val inputs = mapOf("PEK" to pek, "Encrypted PIN Block" to pinBlock)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = DUKPTAESService.decryptPinBlock(pek, pinBlock)
                            DUKPTAESLogManager.logOperation("PIN Decryption", inputs, result = "  Decrypted PIN Block: $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTAESLogManager.logOperation("PIN Decryption", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.LockOpen, modifier = Modifier.weight(1f)
            )
        }
    }
}}

@Composable
private fun DukptMacCard(state: AesMacState) { with(state) {

    val macKeyValidation = DUKPTAESValidationUtils.validate(macKey, "Mac Gen Key")
    val dataValidation = DUKPTAESValidationUtils.validate(data, "Data")

    val isFormValid = macKey.isNotBlank() && data.isNotBlank() &&
            macKeyValidation.state != ValidationState.ERROR && dataValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "DUKPT AES MAC", subtitle = "Generate a Message Authentication Code", icon = DUKPTAESTabs.DUKPT_MAC.icon) {
        EnhancedTextField(value = macKey, onValueChange = { macKey = it }, label = "MAC Generation Key", validation = macKeyValidation)
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data (Hex)", validation = dataValidation, maxLines = 5)
        Spacer(Modifier.height(16.dp))
        ModernButton(
            text = "Generate MAC", onClick = {
                isLoading = true
                val inputs = mapOf("MAC Key" to macKey, "Data" to data)
                GlobalScope.launch {
                    delay(150)
                    try {
                        val result = DUKPTAESService.generateMac(macKey, data)
                        DUKPTAESLogManager.logOperation("MAC Generation", inputs, result = "  Generated MAC: $result", executionTime = 155)
                    } catch (e: Exception) {
                        DUKPTAESLogManager.logOperation("MAC Generation", inputs, error = e.message, executionTime = 155)
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.CheckCircle, modifier = Modifier.fillMaxWidth()
        )
    }
}}

@Composable
private fun DukptDataCard(state: AesDataState) { with(state) {
    val dataTypes = remember { listOf("ASCII", "Hexadecimal") }

    val dekValidation = DUKPTAESValidationUtils.validate(dek, "DEK")
    val dataValidation = DUKPTAESValidationUtils.validate(data, "Data")

    val isFormValid = dek.isNotBlank() && data.isNotBlank() &&
            dekValidation.state != ValidationState.ERROR && dataValidation.state != ValidationState.ERROR

    ModernCryptoCard(title = "DUKPT AES Data", subtitle = "Encrypt or Decrypt data", icon = DUKPTAESTabs.DUKPT_DATA.icon) {
        EnhancedTextField(value = dek, onValueChange = { dek = it }, label = "DEK (Data Encryption Key)", validation = dekValidation)
        Spacer(Modifier.height(12.dp))
        ModernDropdownField("Data Input Type", selectedDataType, dataTypes) { selectedDataType = dataTypes[it] }
        Spacer(Modifier.height(12.dp))
        EnhancedTextField(value = data, onValueChange = { data = it }, label = "Data", validation = dataValidation, maxLines = 5)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernButton(
                text = "Encrypt", onClick = {
                    isLoading = true
                    val inputs = mapOf("DEK" to dek, "Data Input Type" to selectedDataType, "Data" to data)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = DUKPTAESService.encryptData(dek, data, selectedDataType)
                            DUKPTAESLogManager.logOperation("Data Encryption", inputs, result = "  Encrypted Data (Hex): $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTAESLogManager.logOperation("Data Encryption", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.Lock, modifier = Modifier.weight(1f)
            )
            ModernButton(
                text = "Decrypt", onClick = {
                    isLoading = true
                    // Decryption input is always Hex
                    val inputs = mapOf("DEK" to dek, "Encrypted Data" to data)
                    GlobalScope.launch {
                        delay(150)
                        try {
                            val result = DUKPTAESService.decryptData(dek, data)
                            DUKPTAESLogManager.logOperation("Data Decryption", inputs, result = "  Decrypted Data (Hex): $result", executionTime = 155)
                        } catch (e: Exception) {
                            DUKPTAESLogManager.logOperation("Data Decryption", inputs, error = e.message, executionTime = 155)
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading, enabled = isFormValid, icon = Icons.Default.LockOpen, modifier = Modifier.weight(1f)
            )
        }
    }
}}


// --- SHARED UI COMPONENTS ---

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult) {
    Column(modifier = modifier) {
        FixedOutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), maxLines = maxLines,
            isError = validation.state == ValidationState.ERROR,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.primary
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colors.error
                },
                unfocusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colors.error
                }
            )
        )
        if (validation.message.isNotEmpty()) {
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
private fun ModernCryptoCard(title: String, subtitle: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f), maxLines = 1)
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
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "DUKPTAESButtonAnimation") { loading ->
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
