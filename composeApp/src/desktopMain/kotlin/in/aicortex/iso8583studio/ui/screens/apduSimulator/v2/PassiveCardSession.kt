package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.ApduExchange
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.v2.SectionCard

/**
 * Passive card session — used in EMULATE (USB-CDC → STM32) mode where the external POS terminal
 * drives the card and Studio is just observing/answering. No composer here; the controller's
 * [SimulatorController.exchanges] flow is what we render. The "Hold next APDU" toggle is a UI
 * placeholder for the firmware-side interception that lands with the EMULATE bridge in step 7.
 */
@Composable
fun PassiveCardSession(controller: SimulatorController, config: APDUSimulatorConfig) {
    var holdNext by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<ApduExchange?>(null) }

    val lastSelectAid by remember {
        derivedStateOf {
            controller.exchanges.lastOrNull { ex ->
                (ex.command.ins.toInt() and 0xFF) == 0xA4 && (ex.command.p1.toInt() and 0xFF) == 0x04
            }?.command?.data?.toHexSpaced()
        }
    }
    val phase by remember {
        derivedStateOf { inferPhase(controller.exchanges) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Always show the connection bar — colour reflects state at a glance.
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
            onClear = { controller.exchanges.clear() },
            lastError = controller.lastError,
            trailing = { HoldSwitchTrailing(checked = holdNext, onChange = { holdNext = it }) },
        )

        Spacer(Modifier.size(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp,
            shape = RoundedCornerShape(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBlock("Status", terminalStatus(controller.conn, controller.exchanges.size))
                StatusBlock("Phase", phase)
                StatusBlock(
                    "Last AID",
                    lastSelectAid ?: "—",
                    mono = lastSelectAid != null,
                )
                StatusBlock("Exchanges", controller.exchanges.size.toString(), mono = true)
            }
        }

        Spacer(Modifier.size(8.dp))

        // 4-quadrant view of the latest exchange — same chrome as the host sim's live tab.
        CardQuadrantsView(
            exchange = controller.exchanges.lastOrNull(),
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }

    detail?.let { ex ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text("Exchange  •  ${ex.durationMs}ms") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("→ ${ex.command.toBytes().toHexSpaced()}",
                         style = MaterialTheme.typography.caption, fontFamily = FontFamily.Monospace)
                    Text("INS: %02X — %s".format(ex.command.ins.toInt() and 0xFF, insName(ex.command.ins.toInt() and 0xFF)),
                         style = MaterialTheme.typography.caption,
                         color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                    Divider()
                    Text("← ${if (ex.response.data.isEmpty()) "" else ex.response.data.toHexSpaced() + "  "}SW=${"%04X".format(ex.response.sw)}",
                         style = MaterialTheme.typography.caption, fontFamily = FontFamily.Monospace,
                         color = if (ex.response.isSuccess) SuccessGreen else MaterialTheme.colors.error)
                    Text(swMeaning(ex.response.sw),
                         style = MaterialTheme.typography.caption,
                         color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                }
            },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun StatusBlock(label: String, value: String, mono: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.overline,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
            fontWeight = FontWeight.Bold,
        )
        Text(
            value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

private fun terminalStatus(conn: ConnState, count: Int): String = when {
    conn != ConnState.CONNECTED -> "Idle"
    count == 0 -> "Waiting for terminal…"
    else -> "Terminal active"
}

private fun inferPhase(exchanges: List<ApduExchange>): String {
    val lastIns = exchanges.lastOrNull()?.command?.ins?.toInt()?.and(0xFF) ?: return "—"
    return when (lastIns) {
        0xA4 -> "Application selection"
        0xA8 -> "Initiate application processing"
        0xB2 -> "Reading records"
        0xCA -> "Reading data"
        0x84 -> "Online auth challenge"
        0x88 -> "Offline data auth"
        0x20 -> "Cardholder verification"
        0xAE -> "Cryptogram"
        else -> "Custom command (INS %02X)".format(lastIns)
    }
}

@Composable
private fun ExchangeStream(
    exchanges: List<ApduExchange>,
    onSelect: (ApduExchange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(exchanges.size) {
        if (exchanges.isNotEmpty()) listState.animateScrollToItem(exchanges.size - 1)
    }
    Card(modifier = modifier, elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
        if (exchanges.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No APDUs yet — connect the firmware and present the card to a terminal.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(exchanges) { ex -> ExchangeRow(ex, onClick = { onSelect(ex) }) }
            }
        }
    }
}

@Composable
private fun ExchangeRow(ex: ApduExchange, onClick: () -> Unit) {
    val swColor = if (ex.response.isSuccess) SuccessGreen else MaterialTheme.colors.error
    val ins = ex.command.ins.toInt() and 0xFF
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colors.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(swColor, RoundedCornerShape(4.dp)),
                )
                Text(
                    "%02X %s".format(ins, insName(ins)),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(220.dp),
                )
                Text(
                    "→ ${ex.command.toBytes().toHexSpaced().take(48)}${if (ex.command.toBytes().size > 16) "…" else ""}",
                    style = MaterialTheme.typography.caption,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%04X".format(ex.response.sw),
                    style = MaterialTheme.typography.caption,
                    fontFamily = FontFamily.Monospace,
                    color = swColor,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${ex.durationMs}ms",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                )
            }
        }
    }
}
