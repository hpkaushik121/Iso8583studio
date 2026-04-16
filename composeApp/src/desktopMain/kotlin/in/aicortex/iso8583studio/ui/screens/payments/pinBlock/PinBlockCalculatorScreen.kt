package `in`.aicortex.iso8583studio.ui.screens.generic

import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import `in`.aicortex.iso8583studio.ui.screens.components.PersistentTabContent
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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

// ==================== DATA CLASSES ====================

data class PinBlockEncodeResult(
    val clearPinBlock: String,
    val clearPanBlock: String? = null
)

data class PinBlockDecodeResult(
    val pin: String,
    val pan: String? = null
)

/** Describes which input fields a given PIN block format needs in the UI. */
data class PinBlockFormatConfig(
    val needsPanForEncode: Boolean = false,
    val needsPanForDecode: Boolean = false,
    val needsPad: Boolean = false,
    val needsSeqNum: Boolean = false,
    /** 16 for normal blocks, 32 for ISO-4 */
    val pinBlockHexLen: Int = 16,
    /** ISO-4 decode requires a separate PAN block (32 hex chars), not a raw PAN number */
    val needsPanBlockForDecode: Boolean = false,
    val pinMinLen: Int = 4,
    val pinMaxLen: Int = 12
)

// ==================== FORMAT CONFIG ====================

fun getPinBlockFormatConfig(format: String): PinBlockFormatConfig = when (format) {
    "Format 0 (ISO-0)", "ANSI X9.8", "ECI-1", "VISA-1" ->
        PinBlockFormatConfig(needsPanForEncode = true, needsPanForDecode = true)
    "Format 1 (ISO-1)", "ECI-4" ->
        PinBlockFormatConfig()
    "Format 2 (ISO-2)" ->
        PinBlockFormatConfig()
    "Format 3 (ISO-3)" ->
        PinBlockFormatConfig(needsPanForEncode = true, needsPanForDecode = true)
    "Format 4 (ISO-4)" ->
        PinBlockFormatConfig(
            needsPanForEncode = true,
            pinBlockHexLen = 32,
            needsPanBlockForDecode = true
        )
    "OEM-1 / Diebold / Docutel / NCR" ->
        PinBlockFormatConfig(needsPad = true, pinMaxLen = 12)
    "ECI-2" ->
        PinBlockFormatConfig(pinMinLen = 4, pinMaxLen = 4)
    "ECI-3" ->
        PinBlockFormatConfig(pinMinLen = 4, pinMaxLen = 6)
    "IBM 3621", "IBM 5906" ->
        PinBlockFormatConfig(needsPad = true, needsSeqNum = true, pinMinLen = 1, pinMaxLen = 12)
    "IBM 3624" ->
        PinBlockFormatConfig(needsPad = true, pinMinLen = 1, pinMaxLen = 16)
    "IBM 4704 EPP" ->
        PinBlockFormatConfig(needsSeqNum = true, pinMinLen = 1, pinMaxLen = 13)
    "VISA-2" ->
        PinBlockFormatConfig(needsPad = true, pinMinLen = 4, pinMaxLen = 6)
    "VISA-3" ->
        PinBlockFormatConfig(needsPad = true, pinMaxLen = 12)
    "VISA-4" ->
        PinBlockFormatConfig(needsPanForEncode = true, needsPanForDecode = true)
    "AS2805 Format 1" ->
        PinBlockFormatConfig()
    "AS2805 Format 8" ->
        PinBlockFormatConfig(pinMinLen = 0)  // allows empty PIN (zero PIN block)
    else ->
        PinBlockFormatConfig(needsPanForEncode = true, needsPanForDecode = true)
}

// ==================== VALIDATION ====================

object PinBlockValidationUtils {
    fun validatePan(pan: String): ValidationResult {
        if (pan.isEmpty()) return ValidationResult(ValidationState.EMPTY, "PAN cannot be empty.")
        if (pan.any { !it.isDigit() }) return ValidationResult(ValidationState.ERROR, "PAN must be numeric.")
        if (pan.length < 13) return ValidationResult(ValidationState.ERROR, "PAN must be at least 13 digits.")
        return ValidationResult(ValidationState.VALID)
    }

    fun validatePin(pin: String, minLen: Int = 4, maxLen: Int = 12): ValidationResult {
        if (pin.isEmpty() && minLen == 0) return ValidationResult(ValidationState.VALID, "Empty PIN = zero PIN block.")
        if (pin.isEmpty()) return ValidationResult(ValidationState.EMPTY, "PIN cannot be empty.")
        if (pin.any { !it.isDigit() }) return ValidationResult(ValidationState.ERROR, "PIN must be numeric.")
        if (pin.length < minLen) return ValidationResult(ValidationState.ERROR, "PIN must be at least $minLen digits.")
        if (pin.length > maxLen) return ValidationResult(ValidationState.ERROR, "PIN must be at most $maxLen digits.")
        return ValidationResult(ValidationState.VALID)
    }

    fun validatePinBlock(pinBlock: String, hexLen: Int = 16): ValidationResult {
        if (pinBlock.isEmpty()) return ValidationResult(ValidationState.EMPTY, "PIN Block cannot be empty.")
        if (pinBlock.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' })
            return ValidationResult(ValidationState.ERROR, "PIN Block must be valid hexadecimal.")
        if (pinBlock.length != hexLen)
            return ValidationResult(ValidationState.ERROR, "PIN Block must be exactly $hexLen hex characters.")
        return ValidationResult(ValidationState.VALID)
    }

    fun validatePad(pad: String): ValidationResult {
        if (pad.isEmpty()) return ValidationResult(ValidationState.EMPTY, "Pad character cannot be empty.")
        if (pad.length != 1) return ValidationResult(ValidationState.ERROR, "Pad must be a single hex character.")
        val c = pad[0].uppercaseChar()
        if (c !in '0'..'9' && c !in 'A'..'F')
            return ValidationResult(ValidationState.ERROR, "Pad must be a valid hex character (0-F).")
        return ValidationResult(ValidationState.VALID)
    }
}

// ==================== LOG MANAGER ====================

object PinBlockLogManager {
    private val _logEntries = mutableStateListOf<LogEntry>()
    val logEntries: SnapshotStateList<LogEntry> get() = _logEntries

    fun clearLogs() { _logEntries.clear() }

    private fun addLog(entry: LogEntry) {
        _logEntries.add(entry)
        if (_logEntries.size > 500) _logEntries.removeRange(400, _logEntries.size)
    }

    fun logEncode(
        format: String, pan: String?, pin: String, pad: String?,
        result: PinBlockEncodeResult? = null, error: String? = null
    ) {
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val sep = "—".repeat(28)
        val details = buildString {
            appendLine("PIN block encrypt operation ${if (result != null) "finished" else "failed"}")
            appendLine("*".repeat(40))
            appendLine("PAN:            ${pan?.takeIf { it.isNotBlank() } ?: "N/A"}")
            appendLine("PIN:            $pin")
            appendLine("PAD:            ${pad?.takeIf { it.isNotBlank() } ?: "N/A"}")
            appendLine("Format:         $format")
            appendLine(sep)
            if (result != null) {
                appendLine("Clear PIN block:${result.clearPinBlock}")
                result.clearPanBlock?.let { appendLine("Clear PAN block:$it") }
            } else {
                appendLine("Error:          $error")
            }
        }
        val type = if (result != null) LogType.TRANSACTION else LogType.ERROR
        val msg = if (result != null) "PIN Block Encode" else "PIN Block Encode Failed"
        addLog(LogEntry(timestamp = ts, type = type, message = msg, details = details))
    }

    fun logDecode(
        format: String, pinBlock: String, pan: String?, panBlock: String?,
        result: PinBlockDecodeResult? = null, error: String? = null
    ) {
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val sep = "—".repeat(28)
        val details = buildString {
            appendLine("PIN block decode operation ${if (result != null) "finished" else "failed"}")
            appendLine("*".repeat(40))
            appendLine("PIN block:      $pinBlock")
            panBlock?.takeIf { it.isNotBlank() }?.let { appendLine("PAN block:      $it") }
            appendLine("PAN:            ${pan?.takeIf { it.isNotBlank() } ?: "N/A"}")
            appendLine("PAD:            N/A")
            appendLine("Format:         $format")
            appendLine(sep)
            if (result != null) {
                appendLine("Decoded PIN:    ${result.pin}")
                result.pan?.let { appendLine("Decoded PAN:    $it") }
            } else {
                appendLine("Error:          $error")
            }
        }
        val type = if (result != null) LogType.TRANSACTION else LogType.ERROR
        val msg = if (result != null) "PIN Block Decode" else "PIN Block Decode Failed"
        addLog(LogEntry(timestamp = ts, type = type, message = msg, details = details))
    }
}

// ==================== PIN BLOCK SERVICE ====================

object PinBlockService {

    private fun String.decodeHex(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun xorHex(a: String, b: String): String {
        val aBytes = a.uppercase().decodeHex()
        val bBytes = b.uppercase().decodeHex()
        return ByteArray(aBytes.size) { i ->
            (aBytes[i].toInt() xor bBytes[i].toInt()).toByte()
        }.toHex().uppercase()
    }

    private fun randomNibble(): Char = "0123456789ABCDEF"[Random.nextInt(16)]
    private fun randomNibbles(n: Int) = buildString { repeat(n) { append(randomNibble()) } }

    // ======================== ENCODE ========================

    fun encode(
        format: String,
        pan: String = "",
        pin: String,
        pad: String = "",
        seqNum: String = "0000"
    ): PinBlockEncodeResult = when (format) {
        "Format 0 (ISO-0)", "ANSI X9.8", "ECI-1", "VISA-1" -> encodeISO0(pan, pin)
        "Format 1 (ISO-1)", "ECI-4" -> encodeISO1(pin)
        "Format 2 (ISO-2)" -> encodeISO2(pin)
        "Format 3 (ISO-3)" -> encodeISO3(pan, pin)
        "Format 4 (ISO-4)" -> encodeISO4(pan, pin)
        "OEM-1 / Diebold / Docutel / NCR" -> encodeOEM1(pin, pad)
        "ECI-2" -> encodeECI2(pin)
        "ECI-3" -> encodeECI3(pin)
        "IBM 3621", "IBM 5906" -> encodeIBM3621(pin, pad, seqNum)
        "IBM 3624" -> encodeIBM3624(pin, pad)
        "IBM 4704 EPP" -> encodeIBM4704(pin, seqNum)
        "VISA-2" -> encodeVISA2(pin, pad)
        "VISA-3" -> encodeVISA3(pin, pad)
        "VISA-4" -> encodeVISA4(pan, pin)
        "AS2805 Format 1" -> encodeAS2805_1(pin)
        "AS2805 Format 8" -> encodeAS2805_8(pin)
        else -> encodeISO0(pan, pin)
    }

    /**
     * ISO-0 / ANSI X9.8 / ECI-1 / VISA-1
     * PIN field : 0 L P P P P F F F F F F F F F F  (F = 0xF pad)
     * PAN field : 0 0 0 0 P P P P P P P P P P P P  (rightmost 12 of PAN excl. check digit)
     * Result    : XOR of the two fields
     */
    private fun encodeISO0(pan: String, pin: String): PinBlockEncodeResult {
        val L = pin.length
        val pinField = "0${L.toString(16).uppercase()}$pin".padEnd(16, 'F')
        val panStripped = pan.dropLast(1)               // remove check digit
        val panDigits = panStripped.takeLast(12)
        val panField = "0000$panDigits"
        return PinBlockEncodeResult(clearPinBlock = xorHex(pinField, panField))
    }

    /**
     * ISO-1 / ECI-4
     * 1 L P P P P R R R R R R R R R R  (R = random)
     */
    private fun encodeISO1(pin: String): PinBlockEncodeResult {
        val L = pin.length
        val s = "1${L.toString(16).uppercase()}$pin" + randomNibbles(16 - 2 - L)
        return PinBlockEncodeResult(clearPinBlock = s.uppercase())
    }

    /**
     * ISO-2
     * 2 L P P P P F F F F F F F F F F  (F = 0xF pad)
     */
    private fun encodeISO2(pin: String): PinBlockEncodeResult {
        val L = pin.length
        val s = "2${L.toString(16).uppercase()}$pin".padEnd(16, 'F')
        return PinBlockEncodeResult(clearPinBlock = s.uppercase())
    }

    /**
     * ISO-3
     * Same as ISO-0 but first nibble = 3 and fill = random (not F).
     */
    private fun encodeISO3(pan: String, pin: String): PinBlockEncodeResult {
        val L = pin.length
        val pinField = "3${L.toString(16).uppercase()}$pin" + randomNibbles(16 - 2 - L)
        val panStripped = pan.dropLast(1)
        val panDigits = panStripped.takeLast(12)
        val panField = "0000$panDigits"
        return PinBlockEncodeResult(clearPinBlock = xorHex(pinField.uppercase(), panField))
    }

    /**
     * ISO-4  (clear block only – AES encryption is a separate step)
     *
     * Clear PIN block (32 nibbles):
     *   4 L P...P A...A F F | R R R R R R R R R R R R R R R R
     *   ^1 ^1 ^up-to-12 ^fill-A to pos14 ^FF | ^16 random
     *
     * Clear PAN block (32 nibbles):
     *   M A...A/0 0...0  where M = panLength-12 (0-7), 19 PAN digits right-padded with 0, 12 zeros
     */
    private fun encodeISO4(pan: String, pin: String): PinBlockEncodeResult {
        val L = pin.length.coerceAtMost(12)
        // Spec: "F is fill digit 'A'" — every F in the layout table means 'A', so the entire
        // first 16 nibbles are: 4, L, up to 12 PIN digits, then 'A' fill to complete 16.
        val firstHalf = StringBuilder().apply {
            append('4')
            append(L.toString(16).uppercase())
            append(pin.take(12))
            while (length < 16) append('A')
        }
        val clearPinBlock = (firstHalf.toString() + randomNibbles(16)).uppercase()

        val panLen = pan.length          // ISO-4 includes check digit in the PAN block
        val m = (panLen - 12).coerceIn(0, 7)
        val panField = pan.padEnd(19, '0').take(19)
        val clearPanBlock = "${m.toString(16).uppercase()}$panField${"0".repeat(12)}".uppercase()

        return PinBlockEncodeResult(clearPinBlock = clearPinBlock, clearPanBlock = clearPanBlock)
    }

    /**
     * OEM-1 / Diebold / Docutel / NCR
     * P P P P P/X ... X X X X  (last 4 nibbles always X; X ≠ any PIN digit)
     */
    private fun encodeOEM1(pin: String, pad: String): PinBlockEncodeResult {
        val padChar = pad.firstOrNull()?.uppercaseChar() ?: 'F'
        require(padChar.toString() !in pin.map { it.toString() }) {
            "Pad character '$padChar' must differ from all PIN digits."
        }
        val s = pin.take(12).padEnd(16, padChar)
        return PinBlockEncodeResult(clearPinBlock = s.uppercase())
    }

    /**
     * ECI-2
     * P P P P R R R R R R R R R R R R  (first 4 = PIN exactly, remaining = random)
     */
    private fun encodeECI2(pin: String): PinBlockEncodeResult {
        val s = pin.take(4) + randomNibbles(12)
        return PinBlockEncodeResult(clearPinBlock = s.uppercase())
    }

    /**
     * ECI-3
     * L P P P P P/0 P/0 R R R R R R R R R
     * PIN extended to 6 digits with '0' fill, then 9 random nibbles.
     */
    private fun encodeECI3(pin: String): PinBlockEncodeResult {
        val L = pin.length.coerceIn(4, 6)
        val extended = pin.take(6).padEnd(6, '0')
        val s = "${L.toString(16).uppercase()}$extended" + randomNibbles(9)
        return PinBlockEncodeResult(clearPinBlock = s.uppercase())
    }

    /**
     * IBM 3621 / IBM 5906
     * S S S S P P/X ... X X X X  (4-nibble seq, then PIN, then pad fill)
     */
    private fun encodeIBM3621(pin: String, pad: String, seqNum: String): PinBlockEncodeResult {
        val padChar = pad.firstOrNull()?.uppercaseChar() ?: 'F'
        val seq = seqNum.filter { it.uppercaseChar() in "0123456789ABCDEF" }
            .padStart(4, '0').takeLast(4).uppercase()
        val s = "$seq${pin.take(12)}".padEnd(16, padChar)
        return PinBlockEncodeResult(clearPinBlock = s.uppercase())
    }

    /**
     * IBM 3624
     * P P/X P/X ... P/X  (PIN starts at nibble 0, padded to fill all 16)
     */
    private fun encodeIBM3624(pin: String, pad: String): PinBlockEncodeResult {
        val padChar = pad.firstOrNull()?.uppercaseChar() ?: 'F'
        val s = pin.take(16).padEnd(16, padChar)
        return PinBlockEncodeResult(clearPinBlock = s.uppercase())
    }

    /**
     * IBM 4704 EPP
     * L P P/F ... P/F F F ... S1 S2
     * Nibble 0: L (length). Nibbles 1–13: PIN then F fill. Nibbles 14–15: 1-byte seq.
     */
    private fun encodeIBM4704(pin: String, seqNum: String): PinBlockEncodeResult {
        val L = pin.length.coerceIn(1, 13)
        val body = StringBuilder().apply {
            append(L.toString(16).uppercase())
            append(pin.take(13))
            while (length < 14) append('F')
        }
        val seq = seqNum.filter { it.uppercaseChar() in "0123456789ABCDEF" }
            .padStart(2, '0').takeLast(2).uppercase()
        body.append(seq)
        return PinBlockEncodeResult(clearPinBlock = body.toString().uppercase())
    }

    /**
     * VISA-2
     * L P P P P P/0 P/0 D D D D D D D D D
     * PIN extended to 6 digits (0-padded), then 9 identical decimal pad nibbles.
     */
    private fun encodeVISA2(pin: String, pad: String): PinBlockEncodeResult {
        val padChar = pad.firstOrNull()?.takeIf { it.isDigit() } ?: '0'
        val L = pin.length.coerceIn(4, 6)
        val extended = pin.take(6).padEnd(6, '0')
        val s = "${L.toString(16).uppercase()}$extended".padEnd(16, padChar)
        return PinBlockEncodeResult(clearPinBlock = s.uppercase())
    }

    /**
     * VISA-3
     * P P P P P/F ... F/X X X X
     * PIN digits, then 'F' delimiter immediately after last PIN digit, then pad fill.
     */
    private fun encodeVISA3(pin: String, pad: String): PinBlockEncodeResult {
        val padChar = pad.firstOrNull()?.uppercaseChar() ?: '0'
        val s = "${pin.take(12)}F".padEnd(16, padChar)
        return PinBlockEncodeResult(clearPinBlock = s.uppercase())
    }

    /**
     * VISA-4
     * Same structure as ISO-0 but XOR uses the LEFTMOST 12 digits of PAN (excl. check digit).
     */
    private fun encodeVISA4(pan: String, pin: String): PinBlockEncodeResult {
        val L = pin.length
        val pinField = "0${L.toString(16).uppercase()}$pin".padEnd(16, 'F')
        val panStripped = pan.dropLast(1)
        val panDigits = panStripped.take(12)       // LEFTMOST 12
        val panField = "0000$panDigits"
        return PinBlockEncodeResult(clearPinBlock = xorHex(pinField, panField))
    }

    /**
     * AS2805 Format 1 (similar to ISO-1 but last 2 nibbles are always random)
     * 1 L P P P P P/R ... R R
     */
    private fun encodeAS2805_1(pin: String): PinBlockEncodeResult {
        val L = pin.length
        val sb = StringBuilder().apply {
            append('1')
            append(L.toString(16).uppercase())
            append(pin)
            while (length < 14) append(randomNibble())
            append(randomNibble())   // pos 15 – always random
            append(randomNibble())   // pos 16 – always random
        }
        return PinBlockEncodeResult(clearPinBlock = sb.toString().uppercase())
    }

    /**
     * AS2805 Format 8 (format 46)
     * C L P P P P P/R ... F F
     *   C=0 (standard, like AS2805-1 but first nibble=0 and last 2=FF)
     *   C=8, L=0 → Zero PIN block (all random in body, ends with FF)
     */
    private fun encodeAS2805_8(pin: String): PinBlockEncodeResult {
        if (pin.isEmpty()) {
            // Zero PIN block
            val s = "80" + randomNibbles(12) + "FF"
            return PinBlockEncodeResult(clearPinBlock = s.uppercase())
        }
        val L = pin.length
        val sb = StringBuilder().apply {
            append('0')   // C = 0
            append(L.toString(16).uppercase())
            append(pin)
            while (length < 14) append(randomNibble())
            append("FF")  // last 2 always F
        }
        return PinBlockEncodeResult(clearPinBlock = sb.toString().uppercase())
    }

    // ======================== DECODE ========================

    fun decode(
        format: String,
        pan: String = "",
        pinBlock: String,
        panBlock: String = ""
    ): PinBlockDecodeResult = when (format) {
        "Format 0 (ISO-0)", "ANSI X9.8", "ECI-1", "VISA-1" -> decodeISO0(pan, pinBlock)
        "Format 1 (ISO-1)", "ECI-4" -> decodeISO1(pinBlock)
        "Format 2 (ISO-2)" -> decodeISO2(pinBlock)
        "Format 3 (ISO-3)" -> decodeISO3(pan, pinBlock)
        "Format 4 (ISO-4)" -> decodeISO4(pinBlock, panBlock)
        "OEM-1 / Diebold / Docutel / NCR" -> decodeOEM1(pinBlock)
        "ECI-2" -> decodeECI2(pinBlock)
        "ECI-3" -> decodeECI3(pinBlock)
        "IBM 3621", "IBM 5906" -> decodeIBM3621(pinBlock)
        "IBM 3624" -> decodeIBM3624(pinBlock)
        "IBM 4704 EPP" -> decodeIBM4704(pinBlock)
        "VISA-2" -> decodeVISA2(pinBlock)
        "VISA-3" -> decodeVISA3(pinBlock)
        "VISA-4" -> decodeVISA4(pan, pinBlock)
        "AS2805 Format 1" -> decodeAS2805_1(pinBlock)
        "AS2805 Format 8" -> decodeAS2805_8(pinBlock)
        else -> decodeISO0(pan, pinBlock)
    }

    private fun decodeISO0(pan: String, pinBlock: String): PinBlockDecodeResult {
        val panStripped = pan.dropLast(1)
        val panDigits = panStripped.takeLast(12)
        val panField = "0000$panDigits"
        val decoded = xorHex(pinBlock, panField)
        val L = decoded[1].digitToInt(16)
        return PinBlockDecodeResult(pin = decoded.substring(2, 2 + L))
    }

    private fun decodeISO1(pinBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        if (n[0] != '1') throw IllegalArgumentException("Expected format nibble '1', got '${n[0]}'")
        val L = n[1].digitToInt(16)
        return PinBlockDecodeResult(pin = n.substring(2, 2 + L))
    }

    private fun decodeISO2(pinBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        if (n[0] != '2') throw IllegalArgumentException("Expected format nibble '2', got '${n[0]}'")
        val L = n[1].digitToInt(16)
        return PinBlockDecodeResult(pin = n.substring(2, 2 + L))
    }

    private fun decodeISO3(pan: String, pinBlock: String): PinBlockDecodeResult {
        val panStripped = pan.dropLast(1)
        val panDigits = panStripped.takeLast(12)
        val panField = "0000$panDigits"
        val decoded = xorHex(pinBlock, panField)
        val L = decoded[1].digitToInt(16)
        return PinBlockDecodeResult(pin = decoded.substring(2, 2 + L))
    }

    private fun decodeISO4(pinBlock: String, panBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        if (n[0] != '4') throw IllegalArgumentException("Expected format nibble '4', got '${n[0]}'")
        val L = n[1].digitToInt(16)
        val pin = n.substring(2, 2 + L)

        var pan: String? = null
        if (panBlock.isNotBlank() && panBlock.length == 32) {
            val pn = panBlock.uppercase()
            val m = pn[0].digitToInt(16)
            val panLen = m + 12
            pan = pn.substring(1, 1 + panLen)
        }
        return PinBlockDecodeResult(pin = pin, pan = pan)
    }

    /**
     * OEM-1 / Diebold / IBM 3624 – detect pad from repeating tail
     * The last nibble is always the pad character.
     */
    private fun decodeOEM1(pinBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        val padChar = n.last()
        var end = n.length
        while (end > 0 && n[end - 1] == padChar) end--
        return PinBlockDecodeResult(pin = n.substring(0, end))
    }

    private fun decodeECI2(pinBlock: String): PinBlockDecodeResult =
        PinBlockDecodeResult(pin = pinBlock.substring(0, 4))

    private fun decodeECI3(pinBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        val L = n[0].digitToInt(16).coerceIn(4, 6)
        return PinBlockDecodeResult(pin = n.substring(1, 1 + L))
    }

    private fun decodeIBM3621(pinBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        val data = n.substring(4)   // skip 4-nibble sequence number
        val padChar = data.last()
        var end = data.length
        while (end > 0 && data[end - 1] == padChar) end--
        return PinBlockDecodeResult(pin = data.substring(0, end))
    }

    private fun decodeIBM3624(pinBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        val padChar = n.last()
        var end = n.length
        while (end > 0 && n[end - 1] == padChar) end--
        return PinBlockDecodeResult(pin = n.substring(0, end))
    }

    private fun decodeIBM4704(pinBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        val L = n[0].digitToInt(16)
        return PinBlockDecodeResult(pin = n.substring(1, 1 + L))
    }

    private fun decodeVISA2(pinBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        val L = n[0].digitToInt(16)
        return PinBlockDecodeResult(pin = n.substring(1, 1 + L))
    }

    /** VISA-3: read until 'F' delimiter (PIN digits are 0-9 only, so 'F' is unambiguous). */
    private fun decodeVISA3(pinBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        val delimIdx = n.indexOf('F')
        val pin = if (delimIdx >= 0) n.substring(0, delimIdx) else n.take(12)
        return PinBlockDecodeResult(pin = pin)
    }

    /** VISA-4: same as ISO-0 but uses LEFTMOST 12 of PAN (excl. check digit). */
    private fun decodeVISA4(pan: String, pinBlock: String): PinBlockDecodeResult {
        val panStripped = pan.dropLast(1)
        val panDigits = panStripped.take(12)       // LEFTMOST 12
        val panField = "0000$panDigits"
        val decoded = xorHex(pinBlock, panField)
        val L = decoded[1].digitToInt(16)
        return PinBlockDecodeResult(pin = decoded.substring(2, 2 + L))
    }

    private fun decodeAS2805_1(pinBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        if (n[0] != '1') throw IllegalArgumentException("Expected control nibble '1', got '${n[0]}'")
        val L = n[1].digitToInt(16)
        return PinBlockDecodeResult(pin = n.substring(2, 2 + L))
    }

    private fun decodeAS2805_8(pinBlock: String): PinBlockDecodeResult {
        val n = pinBlock.uppercase()
        if (n[0] == '8') return PinBlockDecodeResult(pin = "")  // Zero PIN block
        val L = n[1].digitToInt(16)
        return PinBlockDecodeResult(pin = n.substring(2, 2 + L))
    }
}

// ==================== TABS ====================

enum class PinBlockTabs(val title: String, val icon: ImageVector) {
    ENCODE("Encode", Icons.Default.Lock),
    DECODE("Decode", Icons.Default.LockOpen),
}

// ==================== SCREEN ====================

@Composable
fun PinBlockGeneralScreen(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val pinBlockFormats = remember {
        listOf(
            "Format 0 (ISO-0)",
            "Format 1 (ISO-1)",
            "Format 2 (ISO-2)",
            "Format 3 (ISO-3)",
            "Format 4 (ISO-4)",
            "ANSI X9.8",
            "OEM-1 / Diebold / Docutel / NCR",
            "ECI-1",
            "ECI-2",
            "ECI-3",
            "ECI-4",
            "IBM 3621",
            "IBM 3624",
            "IBM 4704 EPP",
            "IBM 5906",
            "VISA-1",
            "VISA-2",
            "VISA-3",
            "VISA-4",
            "AS2805 Format 1",
            "AS2805 Format 8"
        )
    }
    var selectedFormat by remember { mutableStateOf(pinBlockFormats.first()) }
    val tabList = PinBlockTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = { AppBarWithBack(title = "PIN Block Calculator", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModernDropdownField("PIN block format", selectedFormat, pinBlockFormats) {
                        selectedFormat = pinBlockFormats[it]
                    }

                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabList.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(tab.title) },
                                icon = { Icon(tab.icon, contentDescription = tab.title) }
                            )
                        }
                    }

                    PersistentTabContent(selectedTab = selectedTab, tabs = tabList) { tab ->
                        when (tab) {
                            PinBlockTabs.ENCODE -> EncodeCard(selectedFormat)
                            PinBlockTabs.DECODE -> DecodeCard(selectedFormat)
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Panel {
                    LogPanelWithAutoScroll(
                        onClearClick = { PinBlockLogManager.clearLogs() },
                        logEntries = PinBlockLogManager.logEntries
                    )
                }
            }
        }
    }
}

// ==================== ENCODE CARD ====================

@Composable
private fun EncodeCard(format: String) {
    val config = remember(format) { getPinBlockFormatConfig(format) }

    var pan by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pad by remember { mutableStateOf("F") }
    var seqNum by remember { mutableStateOf("0000") }
    var isLoading by remember { mutableStateOf(false) }

    val panValidation = when {
        !config.needsPanForEncode -> ValidationResult(ValidationState.VALID)
        pan.isEmpty() -> ValidationResult(ValidationState.EMPTY, "PAN is required for this format.")
        else -> PinBlockValidationUtils.validatePan(pan)
    }
    val pinValidation = PinBlockValidationUtils.validatePin(pin, config.pinMinLen, config.pinMaxLen)
    val padValidation = if (config.needsPad) PinBlockValidationUtils.validatePad(pad)
                        else ValidationResult(ValidationState.VALID)

    val isFormValid = pinValidation.state == ValidationState.VALID &&
        (!config.needsPanForEncode || panValidation.state == ValidationState.VALID) &&
        (!config.needsPad || padValidation.state == ValidationState.VALID)

    ModernCryptoCard(
        title = "Encode PIN Block",
        subtitle = "Create a formatted PIN block",
        icon = Icons.Default.VpnKey
    ) {
        if (config.needsPanForEncode) {
            EnhancedTextField(
                value = pan,
                onValueChange = { pan = it.filter(Char::isDigit) },
                label = if (format == "Format 4 (ISO-4)") "PAN (including check digit)" else "PAN",
                validation = panValidation
            )
            Spacer(Modifier.height(12.dp))
        }

        EnhancedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(config.pinMaxLen) },
            label = if (config.pinMinLen == 0) "PIN (leave empty for zero PIN block)" else "PIN",
            validation = pinValidation
        )

        if (config.needsPad) {
            Spacer(Modifier.height(12.dp))
            EnhancedTextField(
                value = pad,
                onValueChange = { v ->
                    if (v.isEmpty() || (v.length == 1 && v[0].uppercaseChar() in "0123456789ABCDEF"))
                        pad = v.uppercase()
                },
                label = "Pad Character (single hex digit, e.g. F)",
                validation = padValidation
            )
        }

        if (config.needsSeqNum) {
            Spacer(Modifier.height(12.dp))
            val seqLabel = if (format == "IBM 4704 EPP") "Sequence Number (2 hex chars, e.g. 00)"
                           else "Sequence Number (4 hex chars, e.g. 0000)"
            EnhancedTextField(
                value = seqNum,
                onValueChange = { v ->
                    seqNum = v.uppercase().filter { it in "0123456789ABCDEF" }.take(4)
                },
                label = seqLabel,
                validation = ValidationResult(ValidationState.VALID)
            )
        }

        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Encode",
            onClick = {
                isLoading = true
                GlobalScope.launch {
                    delay(100)
                    try {
                        val result = PinBlockService.encode(
                            format = format,
                            pan = pan,
                            pin = pin,
                            pad = pad,
                            seqNum = seqNum
                        )
                        PinBlockLogManager.logEncode(
                            format = format,
                            pan = pan.takeIf { config.needsPanForEncode && it.isNotBlank() },
                            pin = pin,
                            pad = pad.takeIf { config.needsPad },
                            result = result
                        )
                    } catch (e: Exception) {
                        PinBlockLogManager.logEncode(
                            format = format,
                            pan = pan.takeIf { config.needsPanForEncode && it.isNotBlank() },
                            pin = pin,
                            pad = pad.takeIf { config.needsPad },
                            result = null,
                            error = e.message ?: "Unknown error"
                        )
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading,
            enabled = isFormValid,
            icon = Icons.Default.ArrowForward,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== DECODE CARD ====================

@Composable
private fun DecodeCard(format: String) {
    val config = remember(format) { getPinBlockFormatConfig(format) }

    var pinBlock by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var panBlock by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val pinBlockValidation = PinBlockValidationUtils.validatePinBlock(pinBlock, config.pinBlockHexLen)
    val panValidation = when {
        !config.needsPanForDecode -> ValidationResult(ValidationState.VALID)
        pan.isEmpty() -> ValidationResult(ValidationState.EMPTY, "PAN is required to decode this format.")
        else -> PinBlockValidationUtils.validatePan(pan)
    }

    val isFormValid = pinBlockValidation.state == ValidationState.VALID &&
        (!config.needsPanForDecode || panValidation.state == ValidationState.VALID)

    ModernCryptoCard(
        title = "Decode PIN Block",
        subtitle = "Extract PIN from a formatted PIN block",
        icon = Icons.Default.LockOpen
    ) {
        val pinBlockLabel = if (config.pinBlockHexLen == 32)
            "PIN Block (32 hex chars)" else "PIN Block (16 hex chars)"
        EnhancedTextField(
            value = pinBlock,
            onValueChange = { v ->
                pinBlock = v.uppercase()
                    .filter { it in "0123456789ABCDEF" }
                    .take(config.pinBlockHexLen)
            },
            label = pinBlockLabel,
            validation = pinBlockValidation
        )

        if (config.needsPanForDecode) {
            Spacer(Modifier.height(12.dp))
            EnhancedTextField(
                value = pan,
                onValueChange = { pan = it.filter(Char::isDigit) },
                label = "PAN",
                validation = panValidation
            )
        }

        if (config.needsPanBlockForDecode) {
            Spacer(Modifier.height(12.dp))
            EnhancedTextField(
                value = panBlock,
                onValueChange = { v ->
                    panBlock = v.uppercase().filter { it in "0123456789ABCDEF" }.take(32)
                },
                label = "PAN Block (32 hex chars, optional – decodes PAN)",
                validation = ValidationResult(ValidationState.VALID)
            )
        }

        Spacer(Modifier.height(16.dp))

        ModernButton(
            text = "Decode",
            onClick = {
                isLoading = true
                GlobalScope.launch {
                    delay(100)
                    try {
                        val result = PinBlockService.decode(
                            format = format,
                            pan = pan,
                            pinBlock = pinBlock,
                            panBlock = panBlock
                        )
                        PinBlockLogManager.logDecode(
                            format = format,
                            pinBlock = pinBlock,
                            pan = pan.takeIf { config.needsPanForDecode && it.isNotBlank() },
                            panBlock = panBlock.takeIf { config.needsPanBlockForDecode && it.isNotBlank() },
                            result = result
                        )
                    } catch (e: Exception) {
                        PinBlockLogManager.logDecode(
                            format = format,
                            pinBlock = pinBlock,
                            pan = pan.takeIf { config.needsPanForDecode && it.isNotBlank() },
                            panBlock = panBlock.takeIf { config.needsPanBlockForDecode && it.isNotBlank() },
                            result = null,
                            error = e.message ?: "Unknown error"
                        )
                    }
                    isLoading = false
                }
            },
            isLoading = isLoading,
            enabled = isFormValid,
            icon = Icons.Default.ArrowBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== SHARED UI COMPONENTS ====================

@Composable
private fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    validation: ValidationResult
) {
    Column(modifier = modifier) {
        FixedOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = maxLines,
            isError = validation.state == ValidationState.ERROR,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = when (validation.state) {
                    ValidationState.VALID -> MaterialTheme.colors.primary
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
                color = if (validation.state == ValidationState.ERROR)
                    MaterialTheme.colors.error
                else
                    MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun ModernCryptoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
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
    onSelectionChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(modifier = Modifier.onGloballyPositioned { textFieldWidth = it.size.width }) {
        FixedOutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = !expanded })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(density) { textFieldWidth.toDp() }).heightIn(max = 300.dp)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = { onSelectionChanged(index); expanded = false }) {
                    Text(text = option, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}

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
        elevation = ButtonDefaults.elevation(
            defaultElevation = 2.dp, pressedElevation = 4.dp, disabledElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Processing...")
            } else {
                icon?.let {
                    Icon(imageVector = it, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, fontWeight = FontWeight.Medium)
            }
        }
    }
}
