package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField

/**
 * The 7 EMV quick-action dialogs. Each produces a [CommandApdu] and hands it back via [onSend].
 * Where the EMV spec requires a templated value (PDOL, CDOL1, PIN block) the dialog provides a
 * sensible default plus a raw-hex override, so the user retains full control.
 */

// ---------------------------------------------------------------------------
// SELECT (00 A4 04 00 Lc <AID> Le)
// ---------------------------------------------------------------------------

@Composable
fun SelectDialog(
    config: APDUSimulatorConfig,
    onDismiss: () -> Unit,
    onSend: (CommandApdu) -> Unit,
) {
    val knownAids = config.terminalProfile.perAid.filter { it.aid.isNotBlank() }.map { it.aid to it.label }
    var aid by remember { mutableStateOf(knownAids.firstOrNull()?.first ?: "A0000000031010") }
    val valid = aid.isHex() && aid.length in 10..32 && aid.length % 2 == 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SELECT  •  00 A4 04 00") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Selects an application by Dedicated File name (AID).",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                if (knownAids.isNotEmpty()) {
                    AidPicker(knownAids, aid, onPick = { aid = it })
                }
                FixedOutlinedTextField(
                    value = aid,
                    onValueChange = { aid = it.uppercase().filter { c -> c.isHexChar() } },
                    label = { Text("AID (hex, 5..16 bytes)") },
                    isError = !valid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ApduPreview(buildSelect(aid, valid))
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onSend(buildSelect(aid, true)!!); onDismiss() }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun buildSelect(aidHex: String, valid: Boolean): CommandApdu? = if (!valid) null else {
    CommandApdu(0x00, 0xA4.toByte(), 0x04, 0x00, aidHex.hexToBytes(), le = 256)
}

// ---------------------------------------------------------------------------
// GPO — GET PROCESSING OPTIONS (80 A8 00 00 Lc <PDOL response> Le)
// ---------------------------------------------------------------------------

@Composable
fun GpoDialog(
    config: APDUSimulatorConfig,
    onDismiss: () -> Unit,
    onSend: (CommandApdu) -> Unit,
) {
    var pdolHex by remember {
        // Default: 0x83 (PDOL data) wrapping a zero-amount 9F02-style payload. User can edit.
        mutableStateOf("8300")
    }
    val valid = pdolHex.isHex() && pdolHex.length % 2 == 0 && pdolHex.length in 2..510
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GPO — GET PROCESSING OPTIONS  •  80 A8 00 00") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "PDOL response (tag 0x83 wrapper). Edit to inject custom amount, currency, country, etc.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    "Terminal currency: ${config.terminalProfile.transactionCurrencyCode}, " +
                        "country: ${config.terminalProfile.terminalCountryCode}, " +
                        "type 0x%02X".format(config.terminalProfile.terminalType),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                    fontFamily = FontFamily.Monospace,
                )
                FixedOutlinedTextField(
                    value = pdolHex,
                    onValueChange = { pdolHex = it.uppercase().filter { c -> c.isHexChar() } },
                    label = { Text("PDOL response (hex, 0x83 wrapped)") },
                    isError = !valid,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                ApduPreview(buildGpo(pdolHex, valid))
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onSend(buildGpo(pdolHex, true)!!); onDismiss() }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun buildGpo(pdolHex: String, valid: Boolean): CommandApdu? = if (!valid) null else {
    CommandApdu(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, pdolHex.hexToBytes(), le = 256)
}

// ---------------------------------------------------------------------------
// READ RECORD (00 B2 <rec> <(SFI<<3)|4> Le)
// ---------------------------------------------------------------------------

@Composable
fun ReadRecordDialog(
    onDismiss: () -> Unit,
    onSend: (CommandApdu) -> Unit,
) {
    var recordHex by remember { mutableStateOf("01") }
    var sfiHex by remember { mutableStateOf("01") }
    val record = recordHex.toIntOrNull(16) ?: -1
    val sfi = sfiHex.toIntOrNull(16) ?: -1
    val valid = record in 1..255 && sfi in 1..30
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("READ RECORD  •  00 B2") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Reads one record from a linear file identified by SFI.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FixedOutlinedTextField(
                        value = recordHex,
                        onValueChange = { recordHex = it.uppercase().filter { c -> c.isHexChar() }.take(2) },
                        label = { Text("Record # (hex, 1..FF)") },
                        isError = record !in 1..255,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    FixedOutlinedTextField(
                        value = sfiHex,
                        onValueChange = { sfiHex = it.uppercase().filter { c -> c.isHexChar() }.take(2) },
                        label = { Text("SFI (1..1E)") },
                        isError = sfi !in 1..30,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                ApduPreview(buildReadRecord(record, sfi, valid))
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onSend(buildReadRecord(record, sfi, true)!!); onDismiss() }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun buildReadRecord(record: Int, sfi: Int, valid: Boolean): CommandApdu? = if (!valid) null else {
    CommandApdu(0x00, 0xB2.toByte(), record.toByte(), ((sfi shl 3) or 0x04).toByte(), le = 256)
}

// ---------------------------------------------------------------------------
// GET DATA (80 CA <P1> <P2> Le)
// ---------------------------------------------------------------------------

private val COMMON_GET_DATA_TAGS = listOf(
    "9F36" to "Application Transaction Counter (ATC)",
    "9F13" to "Last Online ATC",
    "9F17" to "PIN Try Counter",
    "9F4F" to "Log Format",
    "9F4D" to "Log Entry",
    "DF8129" to "Application Status",
)

@Composable
fun GetDataDialog(
    onDismiss: () -> Unit,
    onSend: (CommandApdu) -> Unit,
) {
    var tagHex by remember { mutableStateOf("9F36") }
    var open by remember { mutableStateOf(false) }
    val valid = tagHex.isHex() && tagHex.length in 2..6 && tagHex.length % 2 == 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GET DATA  •  80 CA") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Reads a primitive data object by tag.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        COMMON_GET_DATA_TAGS.firstOrNull { it.first == tagHex }
                            ?.let { "${it.first} — ${it.second}" } ?: "Pick common tag",
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    COMMON_GET_DATA_TAGS.forEach { (t, label) ->
                        DropdownMenuItem(onClick = { tagHex = t; open = false }) {
                            Text("$t — $label")
                        }
                    }
                }
                FixedOutlinedTextField(
                    value = tagHex,
                    onValueChange = { tagHex = it.uppercase().filter { c -> c.isHexChar() }.take(6) },
                    label = { Text("Tag (1..3 bytes hex)") },
                    isError = !valid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ApduPreview(buildGetData(tagHex, valid))
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onSend(buildGetData(tagHex, true)!!); onDismiss() }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun buildGetData(tagHex: String, valid: Boolean): CommandApdu? = if (!valid) null else {
    val bytes = tagHex.hexToBytes()
    val (p1, p2) = when (bytes.size) {
        1 -> 0x00.toByte() to bytes[0]
        2 -> bytes[0] to bytes[1]
        else -> bytes[bytes.size - 2] to bytes[bytes.size - 1]
    }
    CommandApdu(0x80.toByte(), 0xCA.toByte(), p1, p2, le = 256)
}

// ---------------------------------------------------------------------------
// GET CHALLENGE (00 84 00 00 Le=08)
// ---------------------------------------------------------------------------

@Composable
fun GetChallengeDialog(
    onDismiss: () -> Unit,
    onSend: (CommandApdu) -> Unit,
) {
    var leText by remember { mutableStateOf("8") }
    val le = leText.toIntOrNull() ?: -1
    val valid = le in 1..256
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GET CHALLENGE  •  00 84 00 00") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Requests a random challenge from the card. Use 8 bytes for EMV online PIN.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                FixedOutlinedTextField(
                    value = leText,
                    onValueChange = { leText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Le (1..256, 0=256)") },
                    isError = !valid,
                    singleLine = true,
                    modifier = Modifier.width(180.dp),
                )
                ApduPreview(buildGetChallenge(le, valid))
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onSend(buildGetChallenge(le, true)!!); onDismiss() }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun buildGetChallenge(le: Int, valid: Boolean): CommandApdu? = if (!valid) null else {
    CommandApdu(0x00, 0x84.toByte(), 0x00, 0x00, le = le)
}

// ---------------------------------------------------------------------------
// VERIFY (00 20 00 80/88 Lc <PIN block>)
// ---------------------------------------------------------------------------

private enum class PinMode(val p2: Int, val label: String) {
    PLAINTEXT(0x80, "Plaintext (P2=80)"),
    ENCIPHERED(0x88, "Enciphered (P2=88)"),
}

@Composable
fun VerifyDialog(
    onDismiss: () -> Unit,
    onSend: (CommandApdu) -> Unit,
) {
    var pin by remember { mutableStateOf("1234") }
    var mode by remember { mutableStateOf(PinMode.PLAINTEXT) }
    val valid = pin.length in 4..12 && pin.all { it.isDigit() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("VERIFY  •  00 20 00 ${"%02X".format(mode.p2)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Submits the cardholder PIN. Plaintext for offline PIN, enciphered for protected.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PinMode.values().forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = mode == m, onClick = { mode = m })
                            Text(m.label, style = MaterialTheme.typography.body2)
                        }
                    }
                }
                FixedOutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(12) },
                    label = { Text("PIN (4..12 digits)") },
                    isError = !valid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ApduPreview(buildVerify(pin, mode, valid))
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onSend(buildVerify(pin, mode, true)!!); onDismiss() }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun buildVerify(pin: String, mode: PinMode, valid: Boolean): CommandApdu? {
    if (!valid) return null
    // ISO-2 PIN block: 2N L D D D D D D D D D D D D F F F (F = filler)
    val length = pin.length
    val firstNibble = 0x20 or (length and 0x0F)
    val nibbles = StringBuilder().apply {
        append("%X".format(firstNibble))
        append(pin)
        repeat(16 - 2 - pin.length) { append("F") }
    }
    val pinBlock = nibbles.toString().hexToBytes()
    return CommandApdu(0x00, 0x20.toByte(), 0x00, mode.p2.toByte(), pinBlock)
}

// ---------------------------------------------------------------------------
// GENERATE AC (80 AE <P1> 00 Lc <CDOL1 response>)
// ---------------------------------------------------------------------------

private enum class AcRequest(val p1: Int, val label: String) {
    AAC(0x00, "AAC — decline"),
    TC(0x40, "TC — approve offline"),
    ARQC(0x80, "ARQC — go online"),
}

@Composable
fun GenerateAcDialog(
    onDismiss: () -> Unit,
    onSend: (CommandApdu) -> Unit,
) {
    var ac by remember { mutableStateOf(AcRequest.ARQC) }
    var cdaRequested by remember { mutableStateOf(false) }
    var cdol1Hex by remember { mutableStateOf("") }
    val valid = cdol1Hex.isHex() && cdol1Hex.length % 2 == 0
    val p1 = ac.p1 or (if (cdaRequested) 0x10 else 0x00)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GENERATE AC  •  80 AE ${"%02X".format(p1)} 00") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Requests an Application Cryptogram. P1 high bits select the cryptogram type; bit 0x10 sets CDA.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                Column {
                    AcRequest.values().forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = ac == r, onClick = { ac = r })
                            Text(r.label, style = MaterialTheme.typography.body2)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material.Checkbox(
                        checked = cdaRequested,
                        onCheckedChange = { cdaRequested = it },
                    )
                    Text("Combined DDA + AC generation (CDA)", style = MaterialTheme.typography.body2)
                }
                FixedOutlinedTextField(
                    value = cdol1Hex,
                    onValueChange = { cdol1Hex = it.uppercase().filter { c -> c.isHexChar() } },
                    label = { Text("CDOL1 response (hex)") },
                    isError = !valid,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "CDOL1 is the data-object list the card returned in the SELECT response. Build the response by " +
                        "concatenating each requested object's value in order.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                )
                ApduPreview(buildGenerateAc(p1, cdol1Hex, valid))
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onSend(buildGenerateAc(p1, cdol1Hex, true)!!); onDismiss() }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun buildGenerateAc(p1: Int, cdol1Hex: String, valid: Boolean): CommandApdu? = if (!valid) null else {
    CommandApdu(0x80.toByte(), 0xAE.toByte(), p1.toByte(), 0x00, cdol1Hex.hexToBytes(), le = 256)
}

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

@Composable
private fun AidPicker(known: List<Pair<String, String>>, current: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Text(
            known.firstOrNull { it.first == current }?.let { "${it.first}  ${it.second}" } ?: "Pick from terminal AIDs",
            modifier = Modifier.weight(1f),
            fontFamily = FontFamily.Monospace,
        )
        Icon(Icons.Default.ArrowDropDown, null)
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        known.forEach { (a, label) ->
            DropdownMenuItem(onClick = { onPick(a); open = false }) {
                Text("$a — $label", fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun ApduPreview(cmd: CommandApdu?) {
    val text = cmd?.let { it.toBytes().toHexSpaced() } ?: "(invalid input)"
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            "APDU preview",
            style = MaterialTheme.typography.overline,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text,
            style = MaterialTheme.typography.caption,
            fontFamily = FontFamily.Monospace,
            color = if (cmd == null) MaterialTheme.colors.error
            else MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
        )
    }
}

internal fun String.isHex(): Boolean = isNotEmpty() && all { it.isHexChar() }
internal fun Char.isHexChar(): Boolean = this in '0'..'9' || this in 'A'..'F' || this in 'a'..'f'

internal fun String.hexToBytes(): ByteArray {
    require(length % 2 == 0) { "hex must be even length" }
    val out = ByteArray(length / 2)
    for (i in out.indices) {
        out[i] = ((Character.digit(this[i * 2], 16) shl 4) + Character.digit(this[i * 2 + 1], 16)).toByte()
    }
    return out
}

internal fun ByteArray.toHexSpaced(): String =
    joinToString(" ") { "%02X".format(it) }
