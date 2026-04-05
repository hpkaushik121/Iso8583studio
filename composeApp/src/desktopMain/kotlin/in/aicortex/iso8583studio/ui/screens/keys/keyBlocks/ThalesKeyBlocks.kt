package `in`.aicortex.iso8583studio.ui.screens.keys.keyBlocks

import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.ui.composed
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PersistentTabContent
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import io.cryptocalc.crypto.engines.encryption.AesCalculatorEngine
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


private object ThalesKbValidationUtils {
    fun validateHex(value: String, friendlyName: String = "Field"): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "$friendlyName cannot be empty.")
        if (value.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
            return ValidationResult(ValidationState.ERROR, "Only hex characters (0-9, A-F) allowed.")
        }
        if (value.length % 2 != 0) {
            return ValidationResult(ValidationState.ERROR, "Hex string must have an even number of characters.")
        }
        return ValidationResult(ValidationState.VALID)
    }
}

private enum class ThalesKeyBlockTabs(val title: String, val icon: ImageVector) {
    ENCODE("Encode", Icons.Default.Lock),
    DECODE("Decode", Icons.Default.LockOpen)
}

private object ThalesKbLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() {
        _logEntries.clear()
        addLog(LogEntry(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")), LogType.INFO, "Log history cleared", ""))
    }

    private fun addLog(entry: LogEntry) {
        _logEntries.add(entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, _logEntries.size)
    }

    fun logOperation(operation: String, details: String, isError: Boolean = false) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val logType = if (isError) LogType.ERROR else LogType.TRANSACTION
        val message = if (isError) "$operation Failed" else "$operation Result"
        addLog(LogEntry(timestamp, logType, message, details))
    }
}

// ─── Thales Key Block Crypto Engine (standalone, no HSM required) ───

private object ThalesKbCryptoService {

    fun hexToBytes(hex: String): ByteArray =
        hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02X".format(it) }

    fun calculateKcv(key: ByteArray): String {
        val zeros = ByteArray(if (key.size > 24) 16 else 8)
        return if (key.size > 24) {
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
            bytesToHex(cipher.doFinal(zeros)).take(6)
        } else {
            val expandedKey = when (key.size) {
                8 -> key
                16 -> key + key.copyOf(8)
                else -> key.copyOf(24)
            }
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(expandedKey, "DESede"))
            bytesToHex(cipher.doFinal(zeros)).take(6)
        }
    }

    fun calculateAesKcv(key: ByteArray): String {
        // CKCV = AES-CMAC(key, 16 zero bytes), first 5 bytes
        val zeros = ByteArray(16)
        val cmac = AesCalculatorEngine.computeCmac(zeros, key)
        return bytesToHex(cmac).take(10)
    }

    // ── Version 0 key derivation ──

    private fun deriveV0Key(kbpk: ByteArray, xorByte: Int): ByteArray {
        val xored = ByteArray(kbpk.size) { i -> (kbpk[i].toInt() xor xorByte).toByte() }
        return when {
            xored.size >= 24 -> xored.copyOf(24)
            xored.size == 16 -> xored + xored.copyOf(8)
            else -> (xored + xored + xored).copyOf(24)
        }
    }

    private fun tdesEcbEncrypt(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.copyOf(24), "DESede"))
        return cipher.doFinal(data)
    }

    private fun tdesCbcEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("DESede/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.copyOf(24), "DESede"),
            javax.crypto.spec.IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    private fun tdesCbcDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("DESede/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.copyOf(24), "DESede"),
            javax.crypto.spec.IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    private fun tdesCbcMac(data: ByteArray, key: ByteArray): ByteArray {
        val blockSize = 8
        val paddedLen = ((data.size + blockSize - 1) / blockSize) * blockSize
        val padded = data.copyOf(paddedLen)
        var mac = ByteArray(blockSize)
        for (i in 0 until padded.size step blockSize) {
            val block = padded.sliceArray(i until i + blockSize)
            val xored = ByteArray(blockSize) { j -> (mac[j].toInt() xor block[j].toInt()).toByte() }
            mac = tdesEcbEncrypt(xored, key)
        }
        return mac
    }

    // ── Version 1 (AES) key derivation via NIST SP 800-108 CMAC-KDF ──

    private fun deriveV1Key(kbpk: ByteArray, keyUsageCode: Int): ByteArray {
        val (algoCode, lengthBits) = when (kbpk.size) {
            16 -> 0x0002 to 0x0080
            24 -> 0x0003 to 0x00C0
            32 -> 0x0004 to 0x0100
            else -> 0x0002 to 0x0080
        }
        val blocksNeeded = (kbpk.size + 15) / 16
        val derivedBytes = ByteArray(blocksNeeded * 16)
        for (counter in 1..blocksNeeded) {
            val derivationData = byteArrayOf(
                counter.toByte(),
                (keyUsageCode shr 8).toByte(), (keyUsageCode and 0xFF).toByte(),
                0x00,
                (algoCode shr 8).toByte(), (algoCode and 0xFF).toByte(),
                (lengthBits shr 8).toByte(), (lengthBits and 0xFF).toByte()
            )
            val block = AesCalculatorEngine.computeCmac(derivationData, kbpk)
            block.copyInto(derivedBytes, (counter - 1) * 16)
        }
        return derivedBytes.copyOf(kbpk.size)
    }

    // ── Encode ──

    fun encode(
        kbpkHex: String, plainKeyHex: String, versionId: Int,
        keyUsage: String, algorithm: String, modeOfUse: String,
        keyVersion: String, exportability: String, numOptBlocks: String, lmkId: String
    ): String {
        val kbpk = hexToBytes(kbpkHex)
        val clearKey = hexToBytes(plainKeyHex)
        val keyVersionNum = keyVersion.take(2).padStart(2, '0')
        val reserved = lmkId.padStart(2, '0').take(2)

        return if (versionId == 1) {
            encodeVersion1(kbpk, clearKey, keyUsage, algorithm, modeOfUse, keyVersionNum, exportability, numOptBlocks, reserved)
        } else {
            encodeVersion0(kbpk, clearKey, keyUsage, algorithm, modeOfUse, keyVersionNum, exportability, numOptBlocks, reserved)
        }
    }

    private fun encodeVersion0(
        kbpk: ByteArray, clearKey: ByteArray,
        keyUsage: String, algorithm: String, modeOfUse: String,
        keyVersionNum: String, exportability: String, numOptBlocks: String, reserved: String
    ): String {
        val kbek = deriveV0Key(kbpk, 0x45)
        val kbak = deriveV0Key(kbpk, 0x4D)

        val keyLenBits = clearKey.size * 8
        val lenPrefix = byteArrayOf((keyLenBits shr 8).toByte(), (keyLenBits and 0xFF).toByte())
        val payload = lenPrefix + clearKey
        val paddedLen = ((payload.size + 7) / 8) * 8
        val plaintext = payload.copyOf(paddedLen)
        if (paddedLen > payload.size) {
            val random = java.security.SecureRandom()
            val pad = ByteArray(paddedLen - payload.size)
            random.nextBytes(pad)
            pad.copyInto(plaintext, payload.size)
        }

        val encKeyHex0 = bytesToHex(plaintext)
        val macHexLen = 8
        val blockLen = 16 + encKeyHex0.length + macHexLen
        val blockLenStr = blockLen.toString().padStart(4, '0')
        val header = "0$blockLenStr$keyUsage$algorithm$modeOfUse${keyVersionNum}$exportability$numOptBlocks$reserved"

        val iv = header.toByteArray(Charsets.US_ASCII).copyOf(8)
        val encryptedKey = tdesCbcEncrypt(plaintext, kbek, iv)
        val encKeyHex = bytesToHex(encryptedKey)

        // MAC = CBC-MAC(KBAK, header_ASCII_bytes || encrypted_key_BINARY_bytes)
        val macInput = header.toByteArray(Charsets.US_ASCII) + encryptedKey
        val fullMac = tdesCbcMac(macInput, kbak)
        val macHex = bytesToHex(fullMac).take(8)

        return "S$header$encKeyHex$macHex"
    }

    private fun encodeVersion1(
        kbpk: ByteArray, clearKey: ByteArray,
        keyUsage: String, algorithm: String, modeOfUse: String,
        keyVersionNum: String, exportability: String, numOptBlocks: String, reserved: String
    ): String {
        val kbek = deriveV1Key(kbpk, 0x0000)
        val kbmk = deriveV1Key(kbpk, 0x0001)

        // Plaintext: 2-byte key length (bits, BE) + clear key + random padding to 16-byte boundary
        val keyLenBits = clearKey.size * 8
        val lenPrefix = byteArrayOf((keyLenBits shr 8).toByte(), (keyLenBits and 0xFF).toByte())
        val unpadded = lenPrefix + clearKey
        val padLen = if (unpadded.size % 16 == 0) 0 else 16 - (unpadded.size % 16)
        val randomPad = ByteArray(padLen).also { java.security.SecureRandom().nextBytes(it) }
        val clearPayload = unpadded + randomPad

        val encKeyHexLen = clearPayload.size * 2
        val macHexLen = 16  // 8 bytes = 16 hex (Thales S-block)
        val blockLen = 16 + encKeyHexLen + macHexLen
        val blockLenStr = blockLen.toString().padStart(4, '0')
        val header = "1$blockLenStr$keyUsage$algorithm$modeOfUse${keyVersionNum}$exportability$numOptBlocks$reserved"
        val headerBytes = header.toByteArray(Charsets.US_ASCII)

        // Thales S-block V1: Encrypt-then-MAC (same pattern as V0)
        // 1. Encrypt with IV = header[0..15]
        val iv = headerBytes.copyOf(16)
        val encryptedKey = AesCalculatorEngine.encryptCBC(clearPayload, kbek, iv)

        // 2. MAC over header + encrypted key bytes
        val macInput = headerBytes + encryptedKey
        val fullCmac = AesCalculatorEngine.computeCmac(macInput, kbmk)
        val mac = fullCmac.copyOf(8)
        val encKeyHex = bytesToHex(encryptedKey)
        val macHex = bytesToHex(mac)

        return "S$header$encKeyHex$macHex"
    }

    private fun applyPkcs7Padding(data: ByteArray, blockSize: Int): ByteArray {
        val padLen = blockSize - (data.size % blockSize)
        val padded = ByteArray(data.size + padLen)
        data.copyInto(padded)
        for (i in data.size until padded.size) padded[i] = padLen.toByte()
        return padded
    }

    // ── Decode ──

    data class DecodeResult(
        val log: String,
        val plainKeyHex: String,
        val kcv: String
    )

    fun decode(kbpkHex: String, keyBlockStr: String): DecodeResult {
        require(keyBlockStr.isNotEmpty() && keyBlockStr[0] == 'S') { "Not an S-block key" }

        val version = keyBlockStr[1].digitToInt()
        val blockLen = keyBlockStr.substring(2, 6).toInt()
        val totalBlockChars = 1 + blockLen

        val baseHeaderLen = 17
        val baseHeader = keyBlockStr.substring(1, baseHeaderLen)

        val keyUsage = baseHeader.substring(5, 7)
        val algorithm = baseHeader.substring(7, 8)
        val modeOfUse = baseHeader.substring(8, 9)
        val keyVersionNum = baseHeader.substring(9, 11)
        val exportability = baseHeader.substring(11, 12)
        val numOptBlocks = baseHeader.substring(12, 14)
        val lmkIdField = baseHeader.substring(14, 16)

        val numOpt = numOptBlocks.toIntOrNull() ?: 0
        var headerLen = baseHeaderLen
        var pos = baseHeaderLen
        for (i in 0 until numOpt) {
            if (pos + 4 > keyBlockStr.length) break
            val optBlockLen = keyBlockStr.substring(pos + 2, pos + 4).toIntOrNull() ?: 0
            pos += 4 + optBlockLen
        }
        headerLen = pos
        val header = keyBlockStr.substring(1, headerLen)

        val kbpk = hexToBytes(kbpkHex)
        val kbpkKcv = if (version == 1) calculateAesKcv(kbpk) else calculateKcv(kbpk)

        return if (version == 1) {
            decodeVersion1(kbpk, kbpkKcv, keyBlockStr, header, headerLen, totalBlockChars,
                keyUsage, algorithm, modeOfUse, keyVersionNum, exportability, numOptBlocks, lmkIdField)
        } else {
            decodeVersion0(kbpk, kbpkKcv, keyBlockStr, header, headerLen, totalBlockChars,
                keyUsage, algorithm, modeOfUse, keyVersionNum, exportability, numOptBlocks, lmkIdField)
        }
    }

    private fun decodeVersion0(
        kbpk: ByteArray, kbpkKcv: String, keyBlockStr: String, header: String,
        headerLen: Int, totalBlockChars: Int,
        keyUsage: String, algorithm: String, modeOfUse: String,
        keyVersionNum: String, exportability: String, numOptBlocks: String, lmkIdField: String
    ): DecodeResult {
        val macHexLen = 8
        val encKeyHex = keyBlockStr.substring(headerLen, totalBlockChars - macHexLen)
        val macHex = keyBlockStr.substring(totalBlockChars - macHexLen, totalBlockChars)
        val encryptedKey = hexToBytes(encKeyHex)

        val kbek = deriveV0Key(kbpk, 0x45)
        val kbak = deriveV0Key(kbpk, 0x4D)

        val iv = header.toByteArray(Charsets.US_ASCII).copyOf(8)
        val decrypted = tdesCbcDecrypt(encryptedKey, kbek, iv)
        val keyLenBits = ((decrypted[0].toInt() and 0xFF) shl 8) or (decrypted[1].toInt() and 0xFF)
        val keyLenBytes = keyLenBits / 8
        require(keyLenBytes in 1..(decrypted.size - 2)) {
            "Decryption produced invalid key length ($keyLenBits bits / $keyLenBytes bytes). " +
                "Check that the KBPK is correct for this key block."
        }
        val clearKey = decrypted.copyOfRange(2, 2 + keyLenBytes)
        val kcv = calculateKcv(clearKey)

        val log = buildString {
            appendLine("Thales Key Block: Key block decode operation finished")
            appendLine("****************************************")
            appendLine("KBPK:\t\t\t${bytesToHex(kbpk)}")
            appendLine("KCV:\t\t\t$kbpkKcv")
            appendLine("Thales Key block:\t$keyBlockStr")
            appendLine("----------------------------------------")
            appendLine("Thales Header:\t\t$header")
            appendLine("----------------------------------------")
            appendLine("  Version Id:\t\t0 - 3DES KBPK")
            appendLine("  Block Length:\t\t${header.substring(1, 5)}")
            appendLine("  Key Usage:\t\t$keyUsage")
            appendLine("  Algorithm:\t\t$algorithm")
            appendLine("  Mode of Use:\t\t$modeOfUse")
            appendLine("  Key Version No.:\t$keyVersionNum")
            appendLine("  Exportability:\t$exportability")
            appendLine("  Num. of Opt. blocks:\t$numOptBlocks")
            appendLine("  LMK ID:\t\t$lmkIdField")
            appendLine("Thales Encrypted key:\t$encKeyHex")
            appendLine("Thales MAC:\t\t$macHex")
            appendLine("----------------------------------------")
            appendLine("KBEK:\t\t\t${bytesToHex(kbek)}")
            appendLine("KBAK:\t\t\t${bytesToHex(kbak)}")
            appendLine("----------------------------------------")
            appendLine("Plain Key Block:\t${bytesToHex(decrypted)}")
            appendLine("Plain Key:\t\t${bytesToHex(clearKey)}")
            appendLine("KCV:\t\t\t$kcv")
        }

        return DecodeResult(log, bytesToHex(clearKey), kcv)
    }

    private fun decodeVersion1(
        kbpk: ByteArray, kbpkKcv: String, keyBlockStr: String, header: String,
        headerLen: Int, totalBlockChars: Int,
        keyUsage: String, algorithm: String, modeOfUse: String,
        keyVersionNum: String, exportability: String, numOptBlocks: String, lmkIdField: String
    ): DecodeResult {
        val macHexLen = 16  // 8 bytes = 16 hex (Thales S-block)
        val encKeyHex = keyBlockStr.substring(headerLen, totalBlockChars - macHexLen)
        val macHex = keyBlockStr.substring(totalBlockChars - macHexLen, totalBlockChars)
        val encryptedKey = hexToBytes(encKeyHex)
        val storedMac = hexToBytes(macHex)

        val kbek = deriveV1Key(kbpk, 0x0000)
        val kbmk = deriveV1Key(kbpk, 0x0001)

        // Decrypt with IV = header[0..15]
        val iv = header.toByteArray(Charsets.US_ASCII).copyOf(16)
        val decrypted = AesCalculatorEngine.decryptCBC(encryptedKey, kbek, iv)
        // Extract key using 2-byte length prefix (padding is random, not PKCS#7)
        val keyLenBits = ((decrypted[0].toInt() and 0xFF) shl 8) or (decrypted[1].toInt() and 0xFF)
        val keyLenBytes = keyLenBits / 8
        require(keyLenBytes in 1..(decrypted.size - 2)) {
            "Decryption produced invalid key length ($keyLenBits bits / $keyLenBytes bytes). " +
                "Check that the KBPK is correct for this key block."
        }
        val clearKey = decrypted.copyOfRange(2, 2 + keyLenBytes)
        val kcv = calculateKcv(clearKey)

        // Verify MAC: encrypt-then-MAC — CMAC over header + encrypted key bytes
        val headerBytes = header.toByteArray(Charsets.US_ASCII)
        val recomputedCmac = AesCalculatorEngine.computeCmac(headerBytes + encryptedKey, kbmk)
        val recomputedMac = recomputedCmac.copyOf(8)

        val log = buildString {
            appendLine("Thales Key Block: Key block decode operation finished")
            appendLine("****************************************")
            appendLine("KBPK:\t\t\t${bytesToHex(kbpk)}")
            appendLine("CKCV (AES):\t\t$kbpkKcv")
            appendLine("Thales Key block:\t$keyBlockStr")
            appendLine("----------------------------------------")
            appendLine("Thales Header:\t\t$header")
            appendLine("----------------------------------------")
            appendLine("  Version Id:\t\t1 - AES KBPK")
            appendLine("  Block Length:\t\t${header.substring(1, 5)}")
            appendLine("  Key Usage:\t\t$keyUsage")
            appendLine("  Algorithm:\t\t$algorithm")
            appendLine("  Mode of Use:\t\t$modeOfUse")
            appendLine("  Key Version No.:\t$keyVersionNum")
            appendLine("  Exportability:\t$exportability")
            appendLine("  Num. of Opt. blocks:\t$numOptBlocks")
            appendLine("  LMK ID:\t\t$lmkIdField")
            appendLine("Thales Encrypted key:\t$encKeyHex")
            appendLine("Thales MAC:\t\t$macHex")
            appendLine("----------------------------------------")
            appendLine("KBEK:\t\t\t${bytesToHex(kbek)}")
            appendLine("KBMK:\t\t\t${bytesToHex(kbmk)}")
            appendLine("----------------------------------------")
            appendLine("Plain Key Block:\t${bytesToHex(decrypted)}")
            appendLine("Key Length:\t\t$keyLenBits bits ($keyLenBytes bytes)")
            appendLine("Plain Key:\t\t${bytesToHex(clearKey)}")
            appendLine("KCV:\t\t\t$kcv")
            appendLine("CKCV (AES):\t\t${calculateAesKcv(clearKey)}")
            appendLine("----------------------------------------")
            appendLine("MAC Verify:\t\t${if (storedMac.contentEquals(recomputedMac)) "OK" else "MISMATCH"}")
            appendLine("  Stored MAC:\t\t$macHex")
            appendLine("  Recomputed MAC:\t${bytesToHex(recomputedMac)}")
        }

        return DecodeResult(log, bytesToHex(clearKey), kcv)
    }

    private fun removePkcs7Padding(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val padLen = data.last().toInt() and 0xFF
        if (padLen < 1 || padLen > data.size || padLen > 16) return data
        for (i in data.size - padLen until data.size) {
            if ((data[i].toInt() and 0xFF) != padLen) return data
        }
        return data.copyOf(data.size - padLen)
    }
}

// ═══════════════════════════════════════════════════════════
//  UI
// ═══════════════════════════════════════════════════════════

@Composable
fun ThalesKeyBlockScreen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = ThalesKeyBlockTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "Thales Key Block", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Left: Encode / Decode form ──
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        backgroundColor = MaterialTheme.colors.surface,
                        contentColor = MaterialTheme.colors.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.customTabIndicatorOffset(tabPositions[selectedTabIndex]),
                                height = 3.dp,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    ) {
                        tabList.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            tab.title,
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            )
                        }
                    }

                    PersistentTabContent(selectedTab = selectedTab, tabs = tabList) { tab ->
                        when (tab) {
                            ThalesKeyBlockTabs.ENCODE -> EncodeTab()
                            ThalesKeyBlockTabs.DECODE -> DecodeTab()
                        }
                    }
                }
            }

            // ── Right: Log panel ──
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { ThalesKbLogManager.clearLogs() },
                        logEntries = ThalesKbLogManager.logEntries
                    )
                }
            }
        }
    }
}

// ─── ENCODE TAB ───

@Composable
private fun EncodeTab() {
    var versionId by remember { mutableStateOf(0) } // 0 = 3DES, 1 = AES
    var kbpk by remember { mutableStateOf("") }
    var kcv by remember { mutableStateOf("") }
    var plainKey by remember { mutableStateOf("") }
    var keyUsage by remember { mutableStateOf("B0") }
    var algorithm by remember { mutableStateOf("T") }
    var modeOfUse by remember { mutableStateOf("N") }
    var keyVersion by remember { mutableStateOf("00") }
    var exportability by remember { mutableStateOf("E") }
    var optKeyBlocks by remember { mutableStateOf("00") }
    var lmkId by remember { mutableStateOf("00") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(kbpk, versionId) {
        kcv = try {
            val validLens = if (versionId == 1) listOf(32, 48, 64) else listOf(16, 32, 48)
            if (kbpk.length in validLens) {
                val keyBytes = ThalesKbCryptoService.hexToBytes(kbpk)
                if (versionId == 1) ThalesKbCryptoService.calculateAesKcv(keyBytes)
                else ThalesKbCryptoService.calculateKcv(keyBytes)
            } else ""
        } catch (_: Exception) { "" }
    }

    val isFormValid = ThalesKbValidationUtils.validateHex(plainKey, "Plain Key").state == ValidationState.VALID
            && ThalesKbValidationUtils.validateHex(kbpk, "KBPK").state == ValidationState.VALID

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // ── Version selector ──
        SectionCard {
            SectionLabel("Version")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VersionChip("0 - 3DES KBPK", selected = versionId == 0) { versionId = 0 }
                VersionChip("1 - AES KBPK", selected = versionId == 1) { versionId = 1 }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── KBPK ──
        SectionCard {
            SectionLabel("Key Block Protection Key")
            KbpkInputRow(
                kbpk = kbpk,
                onKbpkChange = { kbpk = it.uppercase() },
                kcv = kcv,
                hint = if (versionId == 0) "32H / 48H (Double / Triple DES)" else "32H / 48H / 64H (AES-128/192/256)"
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Plain Key ──
        SectionCard {
            SectionLabel("Clear Key")
            FixedOutlinedTextField(
                value = plainKey,
                onValueChange = { plainKey = it.uppercase() },
                label = { Text("Plain Key (hex)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Header attributes ──
        SectionCard {
            SectionLabel("Key Block Header Attributes")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactDropdown("Key Usage", keyUsageDisplayFor(keyUsage), createKeyUsageOptions(), Modifier.weight(1f)) {
                    keyUsage = createKeyUsageOptions()[it].take(2)
                }
                CompactDropdown("Algorithm", algorithmDisplayFor(algorithm), createAlgorithmOptions(), Modifier.weight(1f)) {
                    algorithm = createAlgorithmOptions()[it].take(1)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactDropdown("Mode of Use", modeOfUseDisplayFor(modeOfUse), createModeOfUseOptions(), Modifier.weight(1f)) {
                    modeOfUse = createModeOfUseOptions()[it].take(1)
                }
                CompactDropdown("Exportability", exportabilityDisplayFor(exportability), createExportabilityOptions(), Modifier.weight(1f)) {
                    exportability = createExportabilityOptions()[it].take(1)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FixedOutlinedTextField(
                    value = keyVersion, onValueChange = { keyVersion = it },
                    label = { Text("Key Version") }, modifier = Modifier.weight(1f), singleLine = true
                )
                FixedOutlinedTextField(
                    value = optKeyBlocks, onValueChange = { optKeyBlocks = it },
                    label = { Text("Opt Blocks") }, modifier = Modifier.weight(1f), singleLine = true
                )
                FixedOutlinedTextField(
                    value = lmkId, onValueChange = { lmkId = it },
                    label = { Text("LMK ID") }, modifier = Modifier.weight(1f), singleLine = true
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Encode button ──
        EncodeDecodeButton(
            text = "Encode Key Block",
            icon = Icons.Default.Lock,
            enabled = isFormValid,
            isLoading = isLoading
        ) {
            isLoading = true
            try {
                val result = ThalesKbCryptoService.encode(
                    kbpkHex = kbpk, plainKeyHex = plainKey, versionId = versionId,
                    keyUsage = keyUsage, algorithm = algorithm, modeOfUse = modeOfUse,
                    keyVersion = keyVersion, exportability = exportability,
                    numOptBlocks = optKeyBlocks, lmkId = lmkId
                )
                val decoded = ThalesKbCryptoService.decode(kbpk, result)
                ThalesKbLogManager.logOperation(
                    "Encode Key Block",
                    "Thales Key Block: Key block encode operation finished\n" +
                        "****************************************\n" +
                        "Key Block:\t$result\n\n" + decoded.log
                )
            } catch (e: Exception) {
                ThalesKbLogManager.logOperation("Encode Key Block", "Error: ${e.message}", isError = true)
            }
            isLoading = false
        }
    }
}

// ─── DECODE TAB ───

@Composable
private fun DecodeTab() {
    var kbpkType by remember { mutableStateOf(0) } // 0 = 3DES, 1 = AES
    var desKbpk by remember { mutableStateOf("") }
    var desKcv by remember { mutableStateOf("") }
    var aesKbpk by remember { mutableStateOf("") }
    var aesKcv by remember { mutableStateOf("") }
    var keyBlock by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Auto-detect version from key block and sync KBPK type
    val detectedVersion = remember(keyBlock) {
        if (keyBlock.length > 1 && keyBlock.startsWith("S")) {
            try { keyBlock[1].digitToInt() } catch (_: Exception) { -1 }
        } else -1
    }
    LaunchedEffect(detectedVersion) {
        if (detectedVersion == 0) kbpkType = 0
        else if (detectedVersion == 1) kbpkType = 1
    }
    val versionLabel = when (detectedVersion) {
        0 -> "Version 0 (3DES)"
        1 -> "Version 1 (AES)"
        else -> null
    }

    // Auto-calculate KCV
    LaunchedEffect(desKbpk) {
        desKcv = try {
            if (desKbpk.length in listOf(16, 32, 48))
                ThalesKbCryptoService.calculateKcv(ThalesKbCryptoService.hexToBytes(desKbpk)) else ""
        } catch (_: Exception) { "" }
    }
    LaunchedEffect(aesKbpk) {
        aesKcv = try {
            if (aesKbpk.length in listOf(32, 48, 64))
                ThalesKbCryptoService.calculateAesKcv(ThalesKbCryptoService.hexToBytes(aesKbpk)) else ""
        } catch (_: Exception) { "" }
    }

    val activeKbpk = if (kbpkType == 1) aesKbpk else desKbpk
    val isFormValid = keyBlock.isNotEmpty() && keyBlock.startsWith("S") && keyBlock.length > 17
            && ThalesKbValidationUtils.validateHex(activeKbpk, "KBPK").state == ValidationState.VALID

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // ── KBPK type + inputs ──
        SectionCard {
            SectionLabel("Key Block Protection Key")
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VersionChip("3DES KBPK", selected = kbpkType == 0) { kbpkType = 0 }
                VersionChip("AES KBPK", selected = kbpkType == 1) { kbpkType = 1 }
            }
            if (kbpkType == 0) {
                KbpkInputRow(
                    kbpk = desKbpk,
                    onKbpkChange = { desKbpk = it.uppercase() },
                    kcv = desKcv,
                    hint = "32H / 48H (Double / Triple DES)"
                )
            } else {
                KbpkInputRow(
                    kbpk = aesKbpk,
                    onKbpkChange = { aesKbpk = it.uppercase() },
                    kcv = aesKcv,
                    hint = "32H / 48H / 64H (AES-128/192/256)"
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Key Block input ──
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Key Block")
                if (versionLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colors.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            versionLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            FixedOutlinedTextField(
                value = keyBlock,
                onValueChange = { keyBlock = it.uppercase() },
                label = { Text("S-block key (e.g. S00072B1TX00S00FF...)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                singleLine = false
            )

            // ── Parsed header preview ──
            if (keyBlock.length > 17 && keyBlock.startsWith("S")) {
                Spacer(Modifier.height(8.dp))
                HeaderPreview(keyBlock)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Decode button ──
        EncodeDecodeButton(
            text = "Decode Key Block",
            icon = Icons.Default.LockOpen,
            enabled = isFormValid,
            isLoading = isLoading
        ) {
            isLoading = true
            try {
                val result = ThalesKbCryptoService.decode(activeKbpk, keyBlock)
                ThalesKbLogManager.logOperation("Decode Key Block", result.log)
            } catch (e: Exception) {
                ThalesKbLogManager.logOperation("Decode Key Block", "Error: ${e.message}", isError = true)
            }
            isLoading = false
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  Shared UI Components
// ═══════════════════════════════════════════════════════════

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colors.primary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun KbpkInputRow(kbpk: String, onKbpkChange: (String) -> Unit, kcv: String, hint: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FixedOutlinedTextField(
            value = kbpk,
            onValueChange = onKbpkChange,
            label = { Text(hint) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("KCV", style = MaterialTheme.typography.overline, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (kcv.isNotEmpty()) MaterialTheme.colors.primary.copy(alpha = 0.08f)
                else MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
                modifier = Modifier.width(72.dp)
            ) {
                Text(
                    text = kcv.ifEmpty { "------" },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.body2,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = if (kcv.isNotEmpty()) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun VersionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.12f)
        else MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )
            Text(
                label,
                style = MaterialTheme.typography.body2,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HeaderPreview(keyBlock: String) {
    val baseHeader = keyBlock.substring(1, 17)
    val version = baseHeader[0]
    val blockLen = baseHeader.substring(1, 5)
    val keyUsage = baseHeader.substring(5, 7)
    val algo = baseHeader.substring(7, 8)
    val mode = baseHeader.substring(8, 9)
    val keyVer = baseHeader.substring(9, 11)
    val export = baseHeader.substring(11, 12)
    val numOpt = baseHeader.substring(12, 14)
    val lmkId = baseHeader.substring(14, 16)

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.04f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderField("Ver", "$version")
            HeaderField("Len", blockLen)
            HeaderField("Usage", keyUsage)
            HeaderField("Algo", algo)
            HeaderField("Mode", mode)
            HeaderField("KeyVer", keyVer)
            HeaderField("Exp", export)
            HeaderField("Opt", numOpt)
            HeaderField("LMK", lmkId)
        }
    }
}

@Composable
private fun HeaderField(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.overline, color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f), fontSize = 9.sp)
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colors.onSurface)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EncodeDecodeButton(text: String, icon: ImageVector, enabled: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)
    ) {
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "btnAnim") { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CompactDropdown(label: String, value: String, options: List<String>, modifier: Modifier = Modifier, onSelectionChanged: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(modifier = modifier.onGloballyPositioned { textFieldWidth = it.size.width }) {
        FixedOutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), readOnly = true,
            trailingIcon = { Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null) }
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = !expanded })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(density) { textFieldWidth.toDp() }).heightIn(max = 300.dp)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = { onSelectionChanged(index); expanded = false }) {
                    Text(option, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}

// ─── Lookup helpers ───

private fun keyUsageDisplayFor(code: String): String =
    createKeyUsageOptions().firstOrNull { it.startsWith(code) } ?: code

private fun algorithmDisplayFor(code: String): String =
    createAlgorithmOptions().firstOrNull { it.startsWith(code) } ?: code

private fun modeOfUseDisplayFor(code: String): String =
    createModeOfUseOptions().firstOrNull { it.startsWith(code) } ?: code

private fun exportabilityDisplayFor(code: String): String =
    createExportabilityOptions().firstOrNull { it.startsWith(code) } ?: code

private fun createKeyUsageOptions(): List<String> {
    return listOf(
        "B0 - Base Derivation Key (BDK-1)", "B1 - DUKPT Initial Key (IKEY/IPEK)", "C0 - Card Verification Key (Generic)",
        "E0 - EMV/Chip card MK: App. Cryptogram (MKAC)", "E1 - EMV/Chip card MK: Sec. Mesg for Conf. (MKSMC)", "E2 - EMV/Chip card MK: Sec. Mesg for Int. (MKSMI)",
        "K0 - Key Encryption / Wrapping (KEK/ZMK)", "K1 - TR-31 Key Block Protection Key",
        "M0 - MAC Key (ISO 9797-1)", "M3 - MAC Key (ISO 9797-1 Alg 3)",
        "P0 - PIN Encryption Key (Generic)", "51 - Terminal Key Encryption (TMK)",
        "V0 - PIN Verification Key (Generic)", "V2 - PIN Verification Key (IBM 3624)",
        "01 - WatchWord Key (WWK)", "63 - HMAC key (using SHA-256)"
    )
}

private fun createAlgorithmOptions(): List<String> {
    return listOf("T - Triple DES", "A - AES", "D - Single DES", "E - Elliptic Curve", "H - HMAC", "R - RSA", "S - DSA")
}

private fun createModeOfUseOptions(): List<String> {
    return listOf(
        "N - No special restrictions or not applicable", "B - Both encryption and decryption", "C - Both generate and verify MAC",
        "D - Decrypt only", "E - Encrypt only", "G - Generate MAC only", "S - Signature creation only", "V - Verify signature only",
        "X - Derivation of other keys only"
    )
}

private fun createExportabilityOptions(): List<String> {
    return listOf(
        "E - May only be exported in a trusted key block", "N - No export permitted", "S - Sensitive"
    )
}

private fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2
    fillMaxWidth().wrapContentSize(Alignment.BottomStart).offset(x = indicatorOffset).width(indicatorWidth)
}
