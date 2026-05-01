package `in`.aicortex.iso8583studio.ui.screens.generic

import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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

// ─── Models ──────────────────────────────────────────────────────────

/**
 * Decoded Track 2 data. Field separator is `=` (ASCII) or `D` (BCD/hex).
 * Format: PAN [sep] EXP(YYMM) SVC(3) DISCRETIONARY [End Sentinel ?] [LRC]
 */
data class Track2Decoded(
    val rawNormalized: String,        // sentinels stripped, pad nibble removed
    val format: String,               // "ASCII" | "BCD/Hex"
    val hadStartSentinel: Boolean,
    val hadEndSentinel: Boolean,
    val pan: String,
    val panLength: Int,
    val panLuhnValid: Boolean,
    val separator: Char,
    val expiryYyMm: String,
    val expiryFormatted: String,
    val serviceCode: String,
    val serviceCodeBreakdown: List<String>,
    val discretionaryData: String,
    val paddingNibble: Char?,
    val lrc: String?
)

// ─── Service Code lookup tables (ISO/IEC 7813) ───────────────────────

private val SVC_POS1 = mapOf(
    '1' to "International interchange",
    '2' to "International interchange, IC preferred",
    '5' to "National interchange only",
    '6' to "National interchange, IC preferred",
    '7' to "Private (no interchange)",
    '9' to "Test"
)
private val SVC_POS2 = mapOf(
    '0' to "Normal authorization",
    '2' to "Authorize by issuer (online)",
    '4' to "Authorize by issuer with exceptions"
)
private val SVC_POS3 = mapOf(
    '0' to "No restrictions, PIN required",
    '1' to "No restrictions",
    '2' to "Goods and services only",
    '3' to "ATM only, PIN required",
    '4' to "Cash only",
    '5' to "Goods and services only, PIN required",
    '6' to "No restrictions, use PIN where feasible",
    '7' to "Goods and services only, use PIN where feasible"
)

// ─── Validation ──────────────────────────────────────────────────────

object Track2ValidationUtils {
    fun validatePan(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "PAN is required.")
        if (value.any { !it.isDigit() }) return ValidationResult(ValidationState.ERROR, "PAN must be digits only.")
        if (value.length !in 8..19) return ValidationResult(ValidationState.ERROR, "PAN must be 8-19 digits.")
        return ValidationResult(ValidationState.VALID)
    }

    fun validateExpiry(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "Expiry is required.")
        if (value.length != 4 || value.any { !it.isDigit() })
            return ValidationResult(ValidationState.ERROR, "Expiry must be 4 digits (YYMM).")
        val mm = value.substring(2).toInt()
        if (mm !in 1..12) return ValidationResult(ValidationState.ERROR, "Month must be 01-12.")
        return ValidationResult(ValidationState.VALID)
    }

    fun validateServiceCode(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "Service code is required.")
        if (value.length != 3 || value.any { !it.isDigit() })
            return ValidationResult(ValidationState.ERROR, "Service code must be exactly 3 digits.")
        return ValidationResult(ValidationState.VALID)
    }

    fun validateDiscretionary(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.VALID) // optional
        if (value.any { !it.isDigit() })
            return ValidationResult(ValidationState.ERROR, "Discretionary data must be digits only.")
        return ValidationResult(ValidationState.VALID)
    }

    fun validateRawTrack2(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult(ValidationState.EMPTY, "Track 2 data is required.")
        // Allow ; ? = D 0-9 A-F (and a-f) and whitespace
        val cleaned = value.replace(" ", "").replace("\t", "")
        if (cleaned.any { it !in '0'..'9' && it !in "=;?DdFfAaBbCcEe" })
            return ValidationResult(ValidationState.ERROR, "Invalid character. Allowed: 0-9, A-F, =, ;, ?")
        return ValidationResult(ValidationState.VALID)
    }
}

// ─── Service ─────────────────────────────────────────────────────────

object Track2Service {

    private fun luhnValid(pan: String): Boolean {
        if (pan.isEmpty() || pan.any { !it.isDigit() }) return false
        var sum = 0
        var alt = false
        for (i in pan.length - 1 downTo 0) {
            var d = pan[i] - '0'
            if (alt) { d *= 2; if (d > 9) d -= 9 }
            sum += d
            alt = !alt
        }
        return sum % 10 == 0
    }

    private fun computeLrc(track: String): String {
        // ISO/IEC 7811 LRC: XOR of 5-bit characters of the data + end sentinel.
        // Common practice — XOR of ASCII bytes.
        var lrc = 0
        track.forEach { lrc = lrc xor it.code }
        return "%02X".format(lrc and 0xFF)
    }

    private fun formatExpiry(yymm: String): String {
        if (yymm.length != 4) return yymm
        val yy = yymm.substring(0, 2)
        val mm = yymm.substring(2, 4)
        return "20$yy-$mm (MM/YY: $mm/$yy)"
    }

    private fun describeServiceCode(sc: String): List<String> {
        if (sc.length != 3) return listOf("Invalid service code")
        return listOf(
            "Position 1 (${sc[0]}): ${SVC_POS1[sc[0]] ?: "Reserved/Unknown"}",
            "Position 2 (${sc[1]}): ${SVC_POS2[sc[1]] ?: "Reserved/Unknown"}",
            "Position 3 (${sc[2]}): ${SVC_POS3[sc[2]] ?: "Reserved/Unknown"}"
        )
    }

    /**
     * Decode a Track 2 string. Auto-detects:
     *   - ASCII raw magstripe with sentinels (`;...?LRC`)
     *   - ASCII without sentinels (PAN=...)
     *   - BCD/hex (EMV tag 57) using `D` as separator and trailing `F` pad
     */
    fun decode(input: String): Track2Decoded {
        val cleanedRaw = input.trim().replace(" ", "").replace("\t", "")
        require(cleanedRaw.isNotEmpty()) { "Empty input" }

        val upper = cleanedRaw.uppercase()
        val hasStart = upper.startsWith(";")
        val hasEnd = upper.contains("?")

        // Strip start sentinel and (LRC after end sentinel)
        var body: String = upper
        if (hasStart) body = body.removePrefix(";")
        var lrc: String? = null
        val qIdx = body.indexOf('?')
        if (qIdx >= 0) {
            val after = body.substring(qIdx + 1)
            body = body.substring(0, qIdx)
            // After '?' may be a 1-char LRC (5-bit) or 2-hex-char LRC
            if (after.isNotEmpty()) lrc = after
        }

        // Detect format
        val isAscii = body.contains('=')
        val sep = if (isAscii) '=' else 'D'
        val format = if (isAscii) "ASCII" else "BCD/Hex"

        val sepIdx = body.indexOf(sep)
        require(sepIdx > 0) { "Missing field separator '$sep'" }

        val pan = body.substring(0, sepIdx)
        require(pan.isNotEmpty() && pan.all { it.isDigit() }) { "Invalid PAN" }

        var afterSep = body.substring(sepIdx + 1)
        var pad: Char? = null
        if (!isAscii && afterSep.isNotEmpty()) {
            // Strip trailing F padding (odd-length BCD)
            val last = afterSep.last()
            if (last == 'F') {
                pad = 'F'
                afterSep = afterSep.dropLast(1)
            }
        }
        require(afterSep.length >= 7) { "Track 2 too short — need YYMM (4) + service code (3)" }
        require(afterSep.all { it.isDigit() }) { "Non-digit data after separator" }

        val expiry = afterSep.substring(0, 4)
        val svc = afterSep.substring(4, 7)
        val disc = if (afterSep.length > 7) afterSep.substring(7) else ""

        val normalized = "$pan$sep$expiry$svc$disc" + (pad?.toString() ?: "")

        return Track2Decoded(
            rawNormalized = normalized,
            format = format,
            hadStartSentinel = hasStart,
            hadEndSentinel = hasEnd,
            pan = pan,
            panLength = pan.length,
            panLuhnValid = luhnValid(pan),
            separator = sep,
            expiryYyMm = expiry,
            expiryFormatted = formatExpiry(expiry),
            serviceCode = svc,
            serviceCodeBreakdown = describeServiceCode(svc),
            discretionaryData = disc,
            paddingNibble = pad,
            lrc = lrc
        )
    }

    /**
     * Encode Track 2 in the requested output format.
     *
     *   ASCII_RAW       — `;PAN=YYMMSCDDDD?LRC`
     *   ASCII_NO_SENT   — `PAN=YYMMSCDDDD`
     *   BCD             — `PANDYYMMSCDDDD[F]`  (pads odd length with F)
     */
    fun encode(
        pan: String,
        expiryYyMm: String,
        serviceCode: String,
        discretionary: String,
        outputFormat: String
    ): String {
        require(pan.all { it.isDigit() } && pan.length in 8..19) { "Invalid PAN" }
        require(expiryYyMm.length == 4 && expiryYyMm.all { it.isDigit() }) { "Invalid expiry YYMM" }
        require(serviceCode.length == 3 && serviceCode.all { it.isDigit() }) { "Invalid service code" }
        require(discretionary.all { it.isDigit() }) { "Discretionary data must be digits" }

        return when (outputFormat) {
            "BCD/Hex (EMV Tag 57)" -> {
                val core = "${pan}D${expiryYyMm}${serviceCode}${discretionary}"
                if (core.length % 2 != 0) "${core}F" else core
            }
            "ASCII (no sentinels)" -> "${pan}=${expiryYyMm}${serviceCode}${discretionary}"
            else -> {
                // ASCII raw magstripe with sentinels and LRC
                val data = ";${pan}=${expiryYyMm}${serviceCode}${discretionary}?"
                val lrc = computeLrc(data)
                "$data$lrc"
            }
        }
    }
}

// ─── Logging ─────────────────────────────────────────────────────────

object Track2LogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() { _logEntries.clear() }

    private fun addLog(entry: LogEntry) {
        _logEntries.add(entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, _logEntries.size)
    }

    fun logOperation(operation: String, inputs: Map<String, String>, result: String? = null, error: String? = null, executionTime: Long = 0L) {
        if (result == null && error == null) return
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (k, v) -> append("  $k: $v\n") }
            result?.let { append("\nResult:\n$it") }
            error?.let { append("\nError:\n  Message: $it") }
            if (executionTime > 0) append("\n\nExecution time: ${executionTime}ms")
        }
        val (logType, message) = if (result != null) (LogType.TRANSACTION to "$operation Result") else (LogType.ERROR to "$operation Failed")
        addLog(LogEntry(timestamp = timestamp, type = logType, message = message, details = details))
    }
}

// ─── Tabs ────────────────────────────────────────────────────────────

private enum class Track2Tab(val title: String, val icon: ImageVector) {
    DECODE("Decode", Icons.Default.Search),
    ENCODE("Encode", Icons.Default.Build)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Track2Screen(onBack: () -> Unit) {
    var selectedIndex by remember { mutableStateOf(0) }
    val tabs = Track2Tab.values().toList()

    Scaffold(
        topBar = { AppBarWithBack(title = "Track 2 Encoder / Decoder", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tab.title, fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (tabs[selectedIndex]) {
                        Track2Tab.DECODE -> Track2DecodeCard()
                        Track2Tab.ENCODE -> Track2EncodeCard()
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = { Track2LogManager.clearLogs() },
                            logEntries = Track2LogManager.logEntries
                        )
                    }
                }
            }
        }
    }
}

// ─── Decode card ─────────────────────────────────────────────────────

@Composable
private fun Track2DecodeCard() {
    var rawTrack by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var decoded by remember { mutableStateOf<Track2Decoded?>(null) }
    var showInfo by remember { mutableStateOf(false) }

    if (showInfo) {
        Track2InfoDialog(onDismiss = { showInfo = false })
    }

    val validation = Track2ValidationUtils.validateRawTrack2(rawTrack)
    val isValid = rawTrack.isNotBlank() && validation.state != ValidationState.ERROR

    Track2Card(
        title = "Decode Track 2",
        subtitle = "Auto-detects ASCII magstripe and BCD/hex (EMV Tag 57)",
        icon = Icons.Default.Search,
        onInfoClick = { showInfo = true }
    ) {
        Track2TextField(
            value = rawTrack,
            onValueChange = { rawTrack = it },
            label = "Track 2 Data (any format)",
            validation = validation,
            maxLines = 3
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Examples:\n" +
                    "  • ASCII raw:        ;1234567890123456=2512201123456789?\n" +
                    "  • ASCII no sent.:   1234567890123456=2512201123456789\n" +
                    "  • BCD/Hex (Tag 57): 1234567890123456D2512201123456789F",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Track2Button(
            text = "Decode",
            onClick = {
                isLoading = true
                val inputs = mapOf("Track 2" to rawTrack)
                GlobalScope.launch {
                    delay(80)
                    try {
                        val result = Track2Service.decode(rawTrack)
                        decoded = result
                        Track2LogManager.logOperation("Decode", inputs, result = result.toLogString(), executionTime = 80)
                    } catch (e: Exception) {
                        decoded = null
                        Track2LogManager.logOperation("Decode", inputs, error = e.message ?: "Decode failed", executionTime = 80)
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading,
            enabled = isValid,
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.fillMaxWidth()
        )

        decoded?.let {
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            Track2DecodedView(it)
        }
    }
}

@Composable
private fun Track2DecodedView(d: Track2Decoded) {
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Decoded Result", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Track2Field("Format", d.format)
            Track2Field("Start sentinel", if (d.hadStartSentinel) "; (present)" else "(absent)")
            Track2Field("End sentinel", if (d.hadEndSentinel) "? (present)" else "(absent)")
            Track2Field("Field separator", "${d.separator}")
            Track2Field("PAN", "${d.pan}  (${d.panLength} digits)")
            Track2Field("Luhn check", if (d.panLuhnValid) "VALID" else "INVALID")
            Track2Field("Expiry (YYMM)", "${d.expiryYyMm}  →  ${d.expiryFormatted}")
            Track2Field("Service code", d.serviceCode)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                d.serviceCodeBreakdown.forEach {
                    Text("• $it", style = MaterialTheme.typography.caption)
                }
            }
            Track2Field(
                "Discretionary data",
                if (d.discretionaryData.isEmpty()) "(none)" else "${d.discretionaryData}  (${d.discretionaryData.length} digits)"
            )
            d.paddingNibble?.let { Track2Field("Pad nibble", "$it") }
            d.lrc?.let { Track2Field("LRC / trailer", it) }
            Track2Field("Normalized", d.rawNormalized)
        }
    }
}

@Composable
private fun Track2Field(label: String, value: String) {
    Row {
        Text("$label: ", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.body2)
        Text(value, style = MaterialTheme.typography.body2)
    }
}

private fun Track2Decoded.toLogString(): String = buildString {
    append("  Format: $format\n")
    append("  PAN: $pan ($panLength digits, Luhn=${if (panLuhnValid) "VALID" else "INVALID"})\n")
    append("  Separator: $separator\n")
    append("  Expiry: $expiryYyMm → $expiryFormatted\n")
    append("  Service Code: $serviceCode\n")
    serviceCodeBreakdown.forEach { append("    - $it\n") }
    append("  Discretionary: ${if (discretionaryData.isEmpty()) "(none)" else discretionaryData}\n")
    paddingNibble?.let { append("  Pad: $it\n") }
    lrc?.let { append("  LRC: $it\n") }
    append("  Normalized: $rawNormalized")
}

// ─── Encode card ─────────────────────────────────────────────────────

@Composable
private fun Track2EncodeCard() {
    val formats = remember { listOf("ASCII raw (sentinels + LRC)", "ASCII (no sentinels)", "BCD/Hex (EMV Tag 57)") }
    var selectedFormat by remember { mutableStateOf(formats.last()) }
    var pan by remember { mutableStateOf("1234567890123456") }
    var expiry by remember { mutableStateOf("2512") }
    var serviceCode by remember { mutableStateOf("201") }
    var discretionary by remember { mutableStateOf("123456789") }
    var isLoading by remember { mutableStateOf(false) }
    var encoded by remember { mutableStateOf("") }

    val panV = Track2ValidationUtils.validatePan(pan)
    val expV = Track2ValidationUtils.validateExpiry(expiry)
    val svcV = Track2ValidationUtils.validateServiceCode(serviceCode)
    val discV = Track2ValidationUtils.validateDiscretionary(discretionary)
    val isFormValid = listOf(panV, expV, svcV, discV).none { it.state == ValidationState.ERROR } &&
            pan.isNotBlank() && expiry.isNotBlank() && serviceCode.isNotBlank()

    Track2Card(
        title = "Encode Track 2",
        subtitle = "Build Track 2 from PAN, expiry, service code & discretionary data",
        icon = Icons.Default.Build
    ) {
        Track2TextField(value = pan, onValueChange = { pan = it.filter { c -> c.isDigit() } }, label = "PAN (8-19 digits)", validation = panV)
        Spacer(Modifier.height(12.dp))
        Track2TextField(value = expiry, onValueChange = { expiry = it.filter { c -> c.isDigit() }.take(4) }, label = "Expiry YYMM (4 digits)", validation = expV)
        Spacer(Modifier.height(12.dp))
        Track2TextField(value = serviceCode, onValueChange = { serviceCode = it.filter { c -> c.isDigit() }.take(3) }, label = "Service Code (3 digits)", validation = svcV)
        Spacer(Modifier.height(12.dp))
        Track2TextField(value = discretionary, onValueChange = { discretionary = it.filter { c -> c.isDigit() } }, label = "Discretionary Data (digits, optional)", validation = discV)
        Spacer(Modifier.height(12.dp))
        Track2Dropdown(
            label = "Output Format",
            value = selectedFormat,
            options = formats,
            onSelected = { selectedFormat = formats[it] }
        )
        Spacer(Modifier.height(16.dp))
        Track2Button(
            text = "Encode",
            onClick = {
                isLoading = true
                val inputs = mapOf(
                    "PAN" to pan, "Expiry" to expiry, "Service Code" to serviceCode,
                    "Discretionary" to discretionary, "Format" to selectedFormat
                )
                GlobalScope.launch {
                    delay(60)
                    try {
                        val out = Track2Service.encode(pan, expiry, serviceCode, discretionary, selectedFormat)
                        encoded = out
                        Track2LogManager.logOperation("Encode", inputs, result = "Track 2: $out", executionTime = 60)
                    } catch (e: Exception) {
                        encoded = ""
                        Track2LogManager.logOperation("Encode", inputs, error = e.message ?: "Encode failed", executionTime = 60)
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading,
            enabled = isFormValid,
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.fillMaxWidth()
        )
        if (encoded.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            Text("Encoded Track 2", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            SelectionContainer { Text(encoded, style = MaterialTheme.typography.body1) }
        }
    }
}

// ─── Shared UI ───────────────────────────────────────────────────────

@Composable
private fun Track2Card(title: String, subtitle: String, icon: ImageVector, onInfoClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                }
                onInfoClick?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun Track2TextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLines: Int = 1, validation: ValidationResult) {
    Column(modifier = modifier) {
        FixedOutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) },
            modifier = Modifier.fillMaxWidth(), maxLines = maxLines,
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
private fun Track2Dropdown(label: String, value: String, options: List<String>, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FixedOutlinedTextField(
            value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), readOnly = true,
            trailingIcon = { Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null) }
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = !expanded })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { idx, opt ->
                DropdownMenuItem(onClick = { onSelected(idx); expanded = false }) {
                    Text(opt, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Track2Button(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled && !isLoading) {
        AnimatedContent(targetState = isLoading, transitionSpec = { fadeIn() with fadeOut() }, label = "Track2ButtonAnim") { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon?.let {
                        Icon(it, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun Track2InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colors.primary)
                Spacer(Modifier.width(8.dp))
                Text("Track 2 Formats", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            SelectionContainer {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Track 2 layout (ISO/IEC 7813):", fontWeight = FontWeight.Bold)
                    Text("PAN [SEP] YYMM SVC DISCRETIONARY", style = MaterialTheme.typography.caption)
                    Spacer(Modifier.height(8.dp))
                    Text("Supported input formats:", fontWeight = FontWeight.Bold)
                    Text("• ASCII raw magstripe — `;PAN=YYMMSVCDD?LRC`", style = MaterialTheme.typography.caption)
                    Text("• ASCII without sentinels — `PAN=YYMMSVCDD`", style = MaterialTheme.typography.caption)
                    Text("• BCD/Hex (EMV Tag 57) — `PANDYYMMSVCDD[F]` (separator `D`, optional `F` pad)", style = MaterialTheme.typography.caption)
                    Spacer(Modifier.height(8.dp))
                    Text("Service code positions:", fontWeight = FontWeight.Bold)
                    Text("Pos 1 — interchange (1=intl, 2=intl+IC, 5=natl, 6=natl+IC, 7=private, 9=test)", style = MaterialTheme.typography.caption)
                    Text("Pos 2 — authorization (0=normal, 2=by issuer, 4=issuer w/ exceptions)", style = MaterialTheme.typography.caption)
                    Text("Pos 3 — services (0=PIN req, 1=none, 2=goods/svc, 3=ATM PIN, 4=cash, 5=goods PIN, 6=PIN feasible, 7=goods PIN feasible)", style = MaterialTheme.typography.caption)
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } },
        shape = RoundedCornerShape(12.dp)
    )
}
