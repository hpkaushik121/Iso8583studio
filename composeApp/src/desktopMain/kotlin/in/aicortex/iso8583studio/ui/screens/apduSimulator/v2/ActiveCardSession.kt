package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Tlv
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.ApduExchange
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2.SectionCard
import kotlinx.coroutines.launch

/**
 * Active card session — used in LOOPBACK and READ modes where Studio is the terminal driving the
 * card. Layout: 7-button quick-action toolbar at the top, raw APDU composer below, last-exchange
 * split panel at the bottom. Each quick action opens a parameter dialog that builds the APDU and
 * sends it; the raw composer accepts any pasted hex.
 */
@Composable
fun ActiveCardSession(controller: SimulatorController, config: APDUSimulatorConfig) {
    val scope = rememberCoroutineScope()
    var lastExchange by remember { mutableStateOf<ApduExchange?>(null) }
    var sending by remember { mutableStateOf(false) }
    var inFlightError by remember { mutableStateOf<String?>(null) }
    var openDialog by remember { mutableStateOf<QuickAction?>(null) }
    var rawCommand by remember { mutableStateOf("00A4040007A0000000031010") }

    val send: (CommandApdu) -> Unit = { cmd ->
        if (controller.conn == ConnState.CONNECTED && !sending) {
            scope.launch {
                sending = true
                inFlightError = null
                runCatching {
                    val started = System.currentTimeMillis()
                    val rsp = controller.transmit(cmd)
                    val finished = System.currentTimeMillis()
                    lastExchange = ApduExchange(started, finished, cmd, rsp, controller.transport?.name ?: "")
                }.onFailure { inFlightError = it.message ?: it::class.simpleName }
                sending = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Always show the connection bar — the colour + label reflect IDLE / CONNECTING /
        // CONNECTED / ERROR so the user always knows the state.
        CompactConnectionBar(
            state = controller.conn,
            transport = transportSummary(controller),
            profile = controller.activeProfile?.let { "${it.name} · ${it.scheme.name}" } ?: "(no profile)",
            atrHex = controller.atrHex,
            exchanges = controller.exchanges.size,
        )
        CompactCardControlPanel(
            state = controller.conn,
            canConnect = controller.canConnect(),
            onConnect = { controller.connect() },
            onReset = { controller.reset() },
            onDisconnect = { controller.disconnect() },
            onClear = { controller.exchanges.clear(); lastExchange = null; inFlightError = null },
            lastError = controller.lastError ?: inFlightError,
        )

        Spacer(Modifier.size(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        QuickActionToolbar(
            connected = controller.conn == ConnState.CONNECTED && !sending,
            onAction = { openDialog = it },
        )

        SectionCard(
            icon = Icons.Default.Send,
            title = "Raw APDU composer",
            subtitle = "Paste a full APDU as hex. Spaces are ignored. Length-correction (61xx/6Cxx) is auto-handled.",
        ) {
            FixedOutlinedTextField(
                value = rawCommand,
                onValueChange = { rawCommand = it.filter { c -> c.isHexChar() || c == ' ' }.uppercase() },
                label = { Text("APDU hex") },
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val parsed = remember(rawCommand) {
                    runCatching { CommandApdu.parse(rawCommand.replace(" ", "").hexToBytes()) }.getOrNull()
                }
                Text(
                    parsed?.let { "Parsed: CLA=%02X INS=%02X P1=%02X P2=%02X${if (it.data != null) " Lc=%02X".format(it.data!!.size) else ""}${it.le?.let { l -> " Le=%02X".format(if (l == 256) 0 else l) } ?: ""}".format(it.cla, it.ins, it.p1, it.p2) }
                        ?: "Parse error",
                    style = MaterialTheme.typography.caption,
                    color = if (parsed == null) MaterialTheme.colors.error
                    else MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = parsed != null && controller.conn == ConnState.CONNECTED && !sending,
                    onClick = { parsed?.let(send) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = SuccessGreen, contentColor = Color.White),
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (sending) "Sending…" else "Send")
                }
            }
            inFlightError?.let {
                Text("⚠ $it", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
            }
        }

        CardQuadrantsView(lastExchange, modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }

    when (openDialog) {
        QuickAction.SELECT -> SelectDialog(config, { openDialog = null }) { send(it); openDialog = null }
        QuickAction.GPO -> GpoDialog(config, { openDialog = null }) { send(it); openDialog = null }
        QuickAction.READ_RECORD -> ReadRecordDialog({ openDialog = null }) { send(it); openDialog = null }
        QuickAction.GET_DATA -> GetDataDialog({ openDialog = null }) { send(it); openDialog = null }
        QuickAction.GET_CHALLENGE -> GetChallengeDialog({ openDialog = null }) { send(it); openDialog = null }
        QuickAction.VERIFY -> VerifyDialog({ openDialog = null }) { send(it); openDialog = null }
        QuickAction.GENERATE_AC -> GenerateAcDialog({ openDialog = null }) { send(it); openDialog = null }
        null -> Unit
    }
}

private enum class QuickAction(val label: String, val icon: ImageVector) {
    SELECT("SELECT", Icons.Default.Folder),
    GPO("GPO", Icons.Default.PlayArrow),
    READ_RECORD("READ RECORD", Icons.Default.Description),
    GET_DATA("GET DATA", Icons.Default.Tune),
    GET_CHALLENGE("GET CHALLENGE", Icons.Default.Casino),
    VERIFY("VERIFY", Icons.Default.Lock),
    GENERATE_AC("GENERATE AC", Icons.Default.VpnKey),
}

@Composable
private fun QuickActionToolbar(connected: Boolean, onAction: (QuickAction) -> Unit) {
    SectionCard(
        icon = Icons.Default.Bolt,
        title = "Quick actions",
        subtitle = "Build any of the 7 EMV commands with a guided dialog. Use the raw composer below for anything else.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickAction.entries.forEach { action ->
                OutlinedButton(
                    onClick = { onAction(action) },
                    enabled = connected,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(action.icon, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(
                            action.label,
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        if (!connected) {
            Text(
                "Connect on the status banner above to enable actions.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Last exchange split panel — parsed command on the left, parsed response on the right
// ---------------------------------------------------------------------------

@Composable
private fun ExchangeViewPanel(exchange: ApduExchange?, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
        if (exchange == null) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No APDU sent yet — pick a quick action or compose one above.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
            return@Card
        }
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PaneHeader("→ Command", swColor = MaterialTheme.colors.primary)
                CommandView(exchange.command)
            }
            Divider(modifier = Modifier.fillMaxSize().width(1.dp), color = MaterialTheme.colors.onSurface.copy(alpha = 0.10f))
            Column(modifier = Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val swColor = if (exchange.response.isSuccess) SuccessGreen else MaterialTheme.colors.error
                PaneHeader("← Response  •  ${exchange.durationMs}ms", swColor = swColor)
                ResponseView(exchange.response)
            }
        }
    }
}

@Composable
private fun PaneHeader(label: String, swColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold,
            color = swColor,
        )
    }
    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.10f))
}

@Composable
private fun CommandView(cmd: CommandApdu) {
    KvLine("Hex", cmd.toBytes().toHexSpaced())
    KvLine("CLA", "%02X".format(cmd.cla))
    KvLine("INS", "%02X  %s".format(cmd.ins, insName(cmd.ins.toInt() and 0xFF)))
    KvLine("P1", "%02X".format(cmd.p1))
    KvLine("P2", "%02X".format(cmd.p2))
    cmd.data?.let {
        KvLine("Lc", "%02X (%d bytes)".format(it.size, it.size))
        Text("Data", style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold,
             color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
        Text(it.toHexSpaced(), style = MaterialTheme.typography.caption, fontFamily = FontFamily.Monospace)
        TlvBlock(it)
    }
    cmd.le?.let { KvLine("Le", "%02X (expect ${if (it == 256) 256 else it} bytes)".format(if (it == 256) 0 else it)) }
}

@Composable
private fun ResponseView(rsp: ResponseApdu) {
    val swColor = if (rsp.isSuccess) SuccessGreen else MaterialTheme.colors.error
    Surface(color = swColor.copy(alpha = 0.10f), shape = RoundedCornerShape(6.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "SW = %04X".format(rsp.sw),
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = swColor,
            )
            Text(
                swMeaning(rsp.sw),
                style = MaterialTheme.typography.caption,
                color = swColor,
            )
        }
    }
    if (rsp.data.isNotEmpty()) {
        KvLine("Length", "${rsp.data.size} bytes")
        Text("Data", style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold,
             color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
        Text(rsp.data.toHexSpaced(), style = MaterialTheme.typography.caption, fontFamily = FontFamily.Monospace)
        TlvBlock(rsp.data)
    }
}

@Composable
private fun KvLine(label: String, value: String) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(48.dp),
        )
        Text(value, style = MaterialTheme.typography.caption, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun TlvBlock(bytes: ByteArray) {
    val tlvs = remember(bytes) { runCatching { Tlv.parseAll(bytes) }.getOrNull().orEmpty() }
    if (tlvs.isEmpty()) return
    Surface(
        color = MaterialTheme.colors.primary.copy(alpha = 0.05f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        LazyColumn(modifier = Modifier.heightIn(max = 220.dp).padding(6.dp)) {
            items(tlvs) { TlvRow(it, depth = 0) }
        }
    }
}

@Composable
private fun TlvRow(tlv: Tlv, depth: Int) {
    Column(modifier = Modifier.padding(start = (depth * 12).dp)) {
        Row {
            Text(
                "%X".format(tlv.tag),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(72.dp),
            )
            Text(
                tagName(tlv.tag),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.width(180.dp),
            )
            Text(
                if (tlv.isConstructed) "(${tlv.value.size}B)"
                else tlv.value.toHexSpaced().take(80) + if (tlv.value.size > 26) "…" else "",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (tlv.isConstructed) {
            val children = remember(tlv) { runCatching { Tlv.parseAll(tlv.value) }.getOrNull().orEmpty() }
            children.forEach { TlvRow(it, depth + 1) }
        }
    }
}
