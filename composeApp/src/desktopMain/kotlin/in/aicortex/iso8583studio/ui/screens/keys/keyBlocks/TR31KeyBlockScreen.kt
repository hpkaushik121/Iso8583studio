package `in`.aicortex.iso8583studio.ui.screens.tr31keyblock

import ai.cortex.core.IsoUtil
import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
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
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.components.PersistentTabContent
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



private object Tr31ValidationUtils {
    fun validateHex(value: String, friendlyName: String): ValidationResult {
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

// --- TR-31 KEY BLOCK SCREEN ---

private enum class Tr31KeyBlockTabs(val title: String, val icon: ImageVector) {
    ENCODE("Encode", Icons.Default.Lock),
    DECODE("Decode", Icons.Default.LockOpen)
}

private object Tr31LogManager {
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

    fun logOperation(operation: String, inputs: Map<String, String>, result: String? = null, error: String? = null, executionTime: Long = 0L) {
        if (result == null && error == null) return

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                val displayValue = if (key.contains("Key", ignoreCase = true)) "${value.take(16)}..." else value
                append("  $key: $displayValue\n")
            }
            result?.let { append("\nResult:\n  $it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }

        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp, logType, message, details))
    }
}

/**
 * Real TR-31 (ANSI X9.143) key block implementation.
 *
 * Currently supports the two "Key Derivation Binding" methods, which are the ones
 * in modern use:
 *  - Version "B": TDEA (2-key/3-key TDES) key derivation binding, CMAC-based.
 *  - Version "D": AES key derivation binding, CMAC-based.
 *
 * The variant-binding methods ("A" and "C") use a different (legacy) construction
 * and are rejected with a clear error rather than producing incorrect output.
 *
 * Verified against reference vectors, e.g. KBPK 2F67…92B6 wrapping key 09B4…9A69
 * under header B0096B0AN00E0000 yields KBEK A062…CF0F / KBAK 3FBD…6124.
 */
private object Tr31CryptoService {

    private val secureRandom = SecureRandom()

    data class EncodeResult(
        val keyBlock: String,
        val header: String,
        val kbek: String,
        val kbak: String,
        val k1: String,
        val k2: String,
        val km1: String,
        val km2: String,
        val mac: String
    )

    data class DecodeResult(
        val plainKey: String,
        val header: String,
        val kbek: String,
        val kbak: String,
        val mac: String,
        val macValid: Boolean,
        val keyUsage: String,
        val algorithm: String,
        val modeOfUse: String
    )

    // --- Block-cipher primitives (via JCE) ---

    private fun normalizeTdesKey(key: ByteArray): ByteArray = when (key.size) {
        24 -> key
        16 -> key + key.copyOfRange(0, 8)   // 2-key TDES -> K1|K2|K1
        8 -> key + key + key
        else -> throw IllegalArgumentException("TDES key must be 8, 16 or 24 bytes (got ${key.size}).")
    }

    private fun tdesEcbBlock(key: ByteArray, block: ByteArray): ByteArray =
        Cipher.getInstance("DESede/ECB/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(normalizeTdesKey(key), "DESede"))
            doFinal(block)
        }

    private fun aesEcbBlock(key: ByteArray, block: ByteArray): ByteArray =
        Cipher.getInstance("AES/ECB/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
            doFinal(block)
        }

    private fun cbc(key: ByteArray, iv: ByteArray, data: ByteArray, aes: Boolean, encrypt: Boolean): ByteArray {
        val transform = if (aes) "AES/CBC/NoPadding" else "DESede/CBC/NoPadding"
        val keySpec = if (aes) SecretKeySpec(key, "AES") else SecretKeySpec(normalizeTdesKey(key), "DESede")
        return Cipher.getInstance(transform).run {
            init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
            doFinal(data)
        }
    }

    // --- CMAC (NIST SP 800-38B), block size 8 (TDES) or 16 (AES) ---

    private fun leftShift1(b: ByteArray): ByteArray {
        val out = ByteArray(b.size)
        var carry = 0
        for (i in b.indices.reversed()) {
            val v = b[i].toInt() and 0xFF
            out[i] = ((v shl 1) or carry).toByte()
            carry = (v ushr 7) and 1
        }
        return out
    }

    private fun xor(a: ByteArray, b: ByteArray): ByteArray =
        ByteArray(a.size) { (a[it].toInt() xor b[it].toInt()).toByte() }

    private fun cmacSubkeys(key: ByteArray, aes: Boolean): Pair<ByteArray, ByteArray> {
        val bs = if (aes) 16 else 8
        val rb = ByteArray(bs).also { it[bs - 1] = if (aes) 0x87.toByte() else 0x1B.toByte() }
        val l = if (aes) aesEcbBlock(key, ByteArray(bs)) else tdesEcbBlock(key, ByteArray(bs))
        fun sub(x: ByteArray): ByteArray {
            val s = leftShift1(x)
            return if (x[0].toInt() and 0x80 != 0) xor(s, rb) else s
        }
        val k1 = sub(l)
        return k1 to sub(k1)
    }

    private fun cmac(key: ByteArray, msg: ByteArray, aes: Boolean): ByteArray {
        val bs = if (aes) 16 else 8
        val ecb: (ByteArray) -> ByteArray = { if (aes) aesEcbBlock(key, it) else tdesEcbBlock(key, it) }
        val (k1, k2) = cmacSubkeys(key, aes)
        val complete = msg.isNotEmpty() && msg.size % bs == 0
        val n = if (msg.isEmpty()) 1 else (msg.size + bs - 1) / bs
        val last: ByteArray = if (complete) {
            xor(msg.copyOfRange((n - 1) * bs, n * bs), k1)
        } else {
            val rem = msg.copyOfRange((n - 1) * bs, msg.size)
            val padded = ByteArray(bs)
            System.arraycopy(rem, 0, padded, 0, rem.size)
            padded[rem.size] = 0x80.toByte()
            xor(padded, k2)
        }
        var x = ByteArray(bs)
        for (i in 0 until n - 1) x = ecb(xor(x, msg.copyOfRange(i * bs, (i + 1) * bs)))
        return ecb(xor(x, last))
    }

    // --- Key-derivation binding (version B / D) ---

    /** usage: 0x0000 = encryption key (KBEK), 0x0001 = MAC key (KBAK). */
    private fun deriveKey(kbpk: ByteArray, usage: Int, aes: Boolean): ByteArray {
        val keyBits = kbpk.size * 8
        // Algorithm indicator + derived-key length depend on the KBPK size.
        val algo = if (aes) when (kbpk.size) {
            16 -> 0x0002
            24 -> 0x0003
            32 -> 0x0004
            else -> throw IllegalArgumentException("AES KBPK must be 16, 24 or 32 bytes.")
        } else when (kbpk.size) {
            16 -> 0x0000
            24 -> 0x0001
            else -> throw IllegalArgumentException("TDES KBPK must be 16 or 24 bytes.")
        }
        val bs = if (aes) 16 else 8
        val out = ByteArray(kbpk.size)
        var produced = 0
        var counter = 1
        while (produced < out.size) {
            val data = byteArrayOf(
                counter.toByte(),
                ((usage ushr 8) and 0xFF).toByte(), (usage and 0xFF).toByte(),
                0x00,
                ((algo ushr 8) and 0xFF).toByte(), (algo and 0xFF).toByte(),
                ((keyBits ushr 8) and 0xFF).toByte(), (keyBits and 0xFF).toByte()
            )
            val block = cmac(kbpk, data, aes)
            val take = minOf(bs, out.size - produced)
            System.arraycopy(block, 0, out, produced, take)
            produced += take
            counter++
        }
        return out
    }

    private fun hex(b: ByteArray) = IsoUtil.bytesToHex(b)

    /**
     * Encode a plaintext key into a TR-31 key block. [header] is the full ASCII header
     * (16-byte fixed part plus any optional blocks); its length field (chars 1..4) is
     * recomputed here so the caller need not pre-size it.
     */
    fun encode(kbpkHex: String, plainKeyHex: String, header: String): EncodeResult {
        val kbpk = IsoUtil.hexToBytes(kbpkHex)
        val key = IsoUtil.hexToBytes(plainKeyHex)
        require(header.length >= 16) { "Header must be at least 16 characters." }
        val version = header[0]
        val aes = when (version) {
            'B' -> false
            'D' -> true
            'A', 'C' -> throw IllegalArgumentException(
                "Version '$version' (variant binding) is not supported. Use B (TDES) or D (AES) key derivation binding."
            )
            else -> throw IllegalArgumentException("Unsupported key block version '$version'. Use B or D.")
        }
        val bs = if (aes) 16 else 8

        val kbek = deriveKey(kbpk, 0x0000, aes)
        val kbak = deriveKey(kbpk, 0x0001, aes)
        val (k1, k2) = cmacSubkeys(kbpk, aes)
        val (km1, km2) = cmacSubkeys(kbak, aes)

        // Confidential payload = 2-byte key length (bits) + key + random pad to block boundary.
        val bitLen = key.size * 8
        val core = byteArrayOf(((bitLen ushr 8) and 0xFF).toByte(), (bitLen and 0xFF).toByte()) + key
        val padLen = (bs - (core.size % bs)) % bs
        val pad = ByteArray(padLen).also { secureRandom.nextBytes(it) }
        val pt = core + pad

        // Fix the block-length field now, since the MAC authenticates the header.
        val totalLen = header.length + pt.size * 2 + bs * 2
        val fixedHeader = header[0] + totalLen.toString().padStart(4, '0') + header.substring(5)
        val headerBytes = fixedHeader.toByteArray(Charsets.US_ASCII)

        val mac = cmac(kbak, headerBytes + pt, aes)
        val enc = cbc(kbek, mac, pt, aes, encrypt = true)

        val keyBlock = fixedHeader + hex(enc) + hex(mac)
        return EncodeResult(keyBlock, fixedHeader, hex(kbek), hex(kbak), hex(k1), hex(k2), hex(km1), hex(km2), hex(mac))
    }

    /** Decode a TR-31 key block back to its plaintext key, verifying the MAC. */
    fun decode(kbpkHex: String, keyBlock: String): DecodeResult {
        val kbpk = IsoUtil.hexToBytes(kbpkHex)
        require(keyBlock.length >= 16) { "Key block is too short." }
        val version = keyBlock[0]
        val aes = when (version) {
            'B' -> false
            'D' -> true
            'A', 'C' -> throw IllegalArgumentException(
                "Version '$version' (variant binding) is not supported. Use B (TDES) or D (AES) key derivation binding."
            )
            else -> throw IllegalArgumentException("Unsupported key block version '$version'. Use B or D.")
        }
        val bs = if (aes) 16 else 8

        // Walk past any optional blocks so the MAC covers the whole header.
        val numOpt = keyBlock.substring(12, 14).toIntOrNull(16) ?: 0
        var headerLen = 16
        repeat(numOpt) {
            require(keyBlock.length >= headerLen + 4) { "Malformed optional block in header." }
            val blkLen = keyBlock.substring(headerLen + 2, headerLen + 4).toInt(16)
            headerLen += blkLen
        }
        require(keyBlock.length > headerLen + bs * 2) { "Key block payload is too short." }

        val header = keyBlock.substring(0, headerLen)
        val payload = keyBlock.substring(headerLen)
        val macHex = payload.takeLast(bs * 2)
        val encHex = payload.dropLast(bs * 2)

        val kbek = deriveKey(kbpk, 0x0000, aes)
        val kbak = deriveKey(kbpk, 0x0001, aes)
        val mac = IsoUtil.hexToBytes(macHex)
        val pt = cbc(kbek, mac, IsoUtil.hexToBytes(encHex), aes, encrypt = false)

        val macCalc = cmac(kbak, header.toByteArray(Charsets.US_ASCII) + pt, aes)
        val macValid = macCalc.contentEquals(mac)

        val bitLen = ((pt[0].toInt() and 0xFF) shl 8) or (pt[1].toInt() and 0xFF)
        val keyLen = bitLen / 8
        require(keyLen in 1..(pt.size - 2)) { "Decoded key length ($bitLen bits) is invalid — wrong KBPK?" }
        val key = pt.copyOfRange(2, 2 + keyLen)

        return DecodeResult(
            plainKey = hex(key),
            header = header,
            kbek = hex(kbek),
            kbak = hex(kbak),
            mac = hex(mac),
            macValid = macValid,
            keyUsage = keyBlock.substring(5, 7),
            algorithm = keyBlock.substring(7, 8),
            modeOfUse = keyBlock.substring(8, 9)
        )
    }
}

@Composable
fun Tr31KeyBlockScreen( onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = Tr31KeyBlockTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "TR-31 Key block", onBackClick = onBack) },
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
                            Tr31KeyBlockTabs.ENCODE -> EncodeTab()
                            Tr31KeyBlockTabs.DECODE -> DecodeTab()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { Tr31LogManager.clearLogs() },
                            logEntries = Tr31LogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// --- TABS ---

@Composable
private fun EncodeTab() {
    var kbpk by remember { mutableStateOf("2F67F1796EA74C01132F808FB3C8AB979E4C6D3EF25192B6") }

    var plainKey by remember { mutableStateOf("09B471B7055CBD564D9596B8B141713C65E4F96AE42E9A69") }
    var header by remember { mutableStateOf("") }
    var versionId by remember { mutableStateOf(versionIdOptions()[1]) }
    var keyUsage by remember { mutableStateOf(keyUsageOptions()[0]) }
    var algorithm by remember { mutableStateOf("A - AES") }
    var modeOfUse by remember { mutableStateOf(modeOfUseOptions()[0]) }
    var keyVersion by remember { mutableStateOf("00") }
    var exportability by remember { mutableStateOf(exportabilityOptions()[0]) }
    var optKeyBlocks by remember { mutableStateOf("00") }
    var reserved by remember { mutableStateOf("00") }
    var optionalHeaders by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = Tr31ValidationUtils.validateHex(plainKey, "Plain Key").state == ValidationState.VALID

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernCryptoCard(title = "TR-31 Key Block", subtitle = "Create an encrypted key block", icon = Icons.Default.Lock) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormRow("KBPK:") { EnhancedTextField(value = kbpk, onValueChange = { kbpk = it.uppercase() }) }
                FormRow("Key Block version") {
                    Row {
                        RadioButton(selected = true, onClick = {})
                        Text("ANSI", Modifier.align(Alignment.CenterVertically))
                    }
                }
                Divider(Modifier.padding(vertical = 8.dp))

                FormRow("Plain Key:") { EnhancedTextField(value = plainKey, onValueChange = { plainKey = it.uppercase() }) }
                FormRow("Header:") { EnhancedTextField(value = header, onValueChange = { header = it.uppercase() }) }
                FormRow("Version Id:") {
                    val opts = versionIdOptions()
                    ModernDropdownField(label = "", value = versionId, options = opts, onSelectionChanged = { versionId = opts[it] })
                }
                FormRow("Key Usage:") {
                    val opts = keyUsageOptions()
                    ModernDropdownField(label = "", value = keyUsage, options = opts, onSelectionChanged = { keyUsage = opts[it] })
                }
                FormRow("Algorithm:") {
                    val opts = listOf("A - AES", "T - Triple DES", "D - Single DES", "E - Elliptic Curve", "H - HMAC", "R - RSA", "S - DSA")
                    ModernDropdownField(label = "", value = algorithm, options = opts, onSelectionChanged = { algorithm = opts[it] })
                }
                FormRow("Mode of Use:") {
                    val opts = modeOfUseOptions()
                    ModernDropdownField(label = "", value = modeOfUse, options = opts, onSelectionChanged = { modeOfUse = opts[it] })
                }
                FormRow("Key version#:") { EnhancedTextField(value = keyVersion, onValueChange = { keyVersion = it }) }
                FormRow("Exportability:") {
                    val opts = exportabilityOptions()
                    ModernDropdownField(label = "", value = exportability, options = opts, onSelectionChanged = { exportability = opts[it] })
                }
                FormRow("# Opt. KeyBlocks:") { EnhancedTextField(value = optKeyBlocks, onValueChange = { optKeyBlocks = it }) }
                FormRow("Reserved:") { EnhancedTextField(value = reserved, onValueChange = { reserved = it }) }
                FormRow("Optional Headers:") { EnhancedTextField(value = optionalHeaders, onValueChange = { optionalHeaders = it }, maxLines = 3) }

                Spacer(Modifier.height(4.dp))
                Tr31ActionButton(
                    text = "Encode Key Block",
                    icon = Icons.Default.Lock,
                    enabled = isFormValid,
                    isLoading = isLoading
                ) {
                    isLoading = true
                    try {
                        // Build the 16-byte fixed header from the structured fields (the length
                        // field is recomputed inside encode()). A non-blank free-form Header
                        // overrides the structured build.
                        val builtHeader = if (header.isNotBlank()) {
                            header
                        } else {
                            buildString {
                                append(versionId.trim().first())          // Version ID
                                append("0000")                             // Block length (placeholder)
                                append(keyUsage.take(2))                   // Key usage
                                append(algorithm.trim().first())           // Algorithm
                                append(modeOfUse.trim().first())           // Mode of use
                                append(keyVersion.padStart(2, '0').takeLast(2)) // Key version number
                                append(exportability.trim().first())       // Exportability
                                append(optKeyBlocks.padStart(2, '0').takeLast(2)) // # optional blocks
                                append(reserved.padStart(2, '0').takeLast(2))     // Reserved
                                append(optionalHeaders)                    // Optional block(s), if any
                            }
                        }
                        val inputs = mapOf("Plain Key" to plainKey, "KBPK" to kbpk, "Version ID" to versionId, "Key Usage" to keyUsage)
                        val r = Tr31CryptoService.encode(kbpk, plainKey, builtHeader)
                        val result = buildString {
                            append("Header: ${r.header}\n")
                            append("  KBEK: ${r.kbek}\n")
                            append("  KBAK: ${r.kbak}\n")
                            append("  K1: ${r.k1}   K2: ${r.k2}\n")
                            append("  KM1: ${r.km1}   KM2: ${r.km2}\n")
                            append("  MAC: ${r.mac}\n")
                            append("  Key Block: ${r.keyBlock}")
                        }
                        Tr31LogManager.logOperation("Encode Key Block", inputs, result)
                    } catch (e: Exception) {
                        Tr31LogManager.logOperation("Encode Key Block", emptyMap(), error = e.message ?: "Unknown error")
                    }
                    isLoading = false
                }
            }
        }
    }
}

private fun versionIdOptions(): List<String> = listOf(
    "A - Key Variant Binding Method",
    "B - TDEA Key Derivation Binding Method",
    "C - TDEA Key Variant Binding Method",
    "D - AES Key Derivation Binding Method",
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
)

private fun modeOfUseOptions(): List<String> = listOf(
    "B - Both Encrypt & Decrypt / Wrap & Unwrap",
    "C - Both Generate & Verify",
    "D - Decrypt / Unwrap Only",
    "E - Encrypt / Wrap Only",
    "G - Generate Only",
    "N - No special restrictions",
    "S - Signature Only",
    "T - Both Sign & Decrypt",
    "V - Verify Only",
    "X - Key used to derive other key(s)",
    "Y - Key used to create key variants",
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
)

private fun keyUsageOptions(): List<String> = listOf(
    "B0 - BDK Base Derivation Key",
    "B1 - Initial DUKPT Key (IKEY/IPEK)",
    "B2 - Base Key Variant Key",
    "C0 - CVK Card Verification Key",
    "D0 - Symmetric Key for Data Encryption",
    "D1 - Asymmetric Key for Data Encryption",
    "D2 - Data Encryption Key for Decimalization Table",
    "E0 - EMV/Chip Issuer MK: Application Cryptograms",
    "E1 - EMV/Chip Issuer MK: Secc Msg. for Confidentiality",
    "E2 - EMV/Chip Issuer MK: Secc Msg. for Integrity",
    "E3 - EMV/Chip Issuer MK: Data Authentication Code",
    "E4 - EMV/Chip Issuer MK: Dynamic Numbers",
    "E5 - EMV/Chip Issuer MK: Card Personalization",
    "E6 - EMV/Chip Issuer MK: Other",
    "I0 - Initialization Vector (IV)",
    "K0 - Key Encryption or wrapping",
    "K1 - TR-31 Key Block Protection Key",
    "K2 - TR-34 Asymmetric key",
    "K3 - Asymmetric key for key agreement/key wrapping",
    "M0 - ISO 16609 MAC algorithm 1 (using TDEA)",
    "M1 - ISO 9797-1 MAC Algorithm 1",
    "M2 - ISO 9797-1 MAC Algorithm 2",
    "M3 - ISO 9797-1 MAC Algorithm 3",
    "M4 - ISO 9797-1 MAC Algorithm 4",
    "M5 - ISO 9797-1:1999 MAC Algorithm 5",
    "M6 - ISO 9797-1:2011 MAC Algorithm 5/CMAC",
    "M7 - HMAC",
    "M8 - ISO 9797-1:2011 MAC Algorithm 6",
    "P0 - PIN Encryption Key",
    "S0 - Asymmetric key pair for digital signature"
)

private fun exportabilityOptions(): List<String> = listOf(
    "E - Exportable u. a KEK (meeting req. of X9.24 Pt. 1 or 2)",
    "N - Non-exportable by the rcv. of the KB, or from storage",
    "S - Sensitive, exp. u. KEK (not meeting the req. of X9.24)",
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
)

@Composable
private fun DecodeTab() {
    var kbpk by remember { mutableStateOf("2F67F1796EA74C01132F808FB3C8AB979E4C6D3EF25192B6") }
    var keyBlock by remember { mutableStateOf("") }
    var dataInput by remember { mutableStateOf("ASCII") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = Tr31ValidationUtils.validateHex(kbpk, "KBPK").state == ValidationState.VALID &&
            keyBlock.isNotBlank()

    ModernCryptoCard(title = "Decode Key Block", subtitle = "Extract a key from a TR-31 key block", icon = Icons.Default.LockOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnhancedTextField(value = kbpk, onValueChange = { kbpk = it.uppercase() }, label = "KBPK (Hex)")
            EnhancedTextField(value = keyBlock, onValueChange = { keyBlock = it.uppercase() }, label = "Key Block (Hex)", maxLines = 8)

            Text("Data Input", style = MaterialTheme.typography.subtitle2)
            Row(Modifier.selectableGroup()) {
                RadioButton(selected = dataInput == "ASCII", onClick = { dataInput = "ASCII" })
                Text("ASCII", Modifier.padding(start = 4.dp, end = 16.dp))
                RadioButton(selected = dataInput == "Hexadecimal", onClick = { dataInput = "Hexadecimal" })
                Text("Hexadecimal", Modifier.padding(start = 4.dp))
            }

            Tr31ActionButton(
                text = "Decode Key Block",
                icon = Icons.Default.LockOpen,
                enabled = isFormValid,
                isLoading = isLoading
            ) {
                isLoading = true
                try {
                    val inputs = mapOf("KBPK" to kbpk, "Key Block" to keyBlock, "Data Input" to dataInput)
                    val r = Tr31CryptoService.decode(kbpk, keyBlock)
                    val result = buildString {
                        append("Header: ${r.header}\n")
                        append("  Key Usage: ${r.keyUsage}   Algorithm: ${r.algorithm}   Mode: ${r.modeOfUse}\n")
                        append("  KBEK: ${r.kbek}\n")
                        append("  KBAK: ${r.kbak}\n")
                        append("  MAC: ${r.mac}  (${if (r.macValid) "VALID" else "INVALID — wrong KBPK or corrupt block"})\n")
                        append("  Plain Key: ${r.plainKey}")
                    }
                    Tr31LogManager.logOperation("Decode Key Block", inputs, result)
                } catch (e: Exception) {
                    Tr31LogManager.logOperation("Decode Key Block", emptyMap(), error = e.message ?: "Unknown error")
                }
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Tr31ActionButton(text: String, icon: ImageVector, enabled: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp)
    ) {
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "tr31BtnAnim") { loading ->
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


// --- SHARED UI COMPONENTS (PRIVATE TO THIS FILE) ---
@Composable
private fun FormRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.body2, modifier = Modifier.width(120.dp))
        content()
    }
}

@Composable
private fun EnhancedTextField(value: String, onValueChange: (String) -> Unit, label: String? = null, modifier: Modifier = Modifier, maxLines: Int = 1) {
    FixedOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { {Text(it)} },
        modifier = modifier.fillMaxWidth(),
        maxLines = maxLines,
        singleLine = maxLines == 1
    )
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

private fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2
    fillMaxWidth().wrapContentSize(Alignment.BottomStart).offset(x = indicatorOffset).width(indicatorWidth)
}
