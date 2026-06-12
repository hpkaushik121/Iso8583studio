package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.toHex
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.AcType
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.Phase
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.TerminalRuntime
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.TransactionRequest
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.TransactionStep
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal.TransactionType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Transaction tab — drives the EMV Book 3 state machine end-to-end. Lets the user pick an amount
 * and type, then watch the terminal walk every phase: SELECT, GPO, READ RECORD, ODA, CVM, risk
 * mgmt, terminal action analysis, GAC. Renders TVR/TSI live and the final cryptogram.
 *
 * The TerminalRuntime emits a Flow of [TransactionStep]; this tab groups them by phase, shows
 * APDU exchanges per phase, computed values as notes, and the final outcome.
 */
@Composable
fun TransactionTab(controller: SimulatorController, config: APDUSimulatorConfig) {
    var amount by remember { mutableStateOf("100.00") }
    var type by remember { mutableStateOf(TransactionType.PURCHASE) }
    var forceOnline by remember { mutableStateOf(false) }
    var forceDecline by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val steps = remember { mutableStateListOf<TransactionStep>() }
    val scope = rememberCoroutineScope()

    val outcome by remember { derivedStateOf { steps.filterIsInstance<TransactionStep.Outcome>().lastOrNull() } }
    val abort by remember { derivedStateOf { steps.filterIsInstance<TransactionStep.Aborted>().lastOrNull() } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Request builder
        Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transaction request", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FixedOutlinedTextField(
                        value = amount,
                        onValueChange = { v -> amount = v.filter { it.isDigit() || it == '.' }.take(12) },
                        label = { Text("Amount") },
                        singleLine = true,
                        modifier = Modifier.width(180.dp),
                    )
                    TypeDropdown(type, onPick = { type = it }, modifier = Modifier.width(220.dp))
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Currency: ${config.terminalProfile.transactionCurrencyCode} (exp ${config.terminalProfile.transactionCurrencyExp})",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = forceOnline, onCheckedChange = { forceOnline = it; if (it) forceDecline = false })
                        Text("Force online (ARQC)", style = MaterialTheme.typography.caption)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = forceDecline, onCheckedChange = { forceDecline = it; if (it) forceOnline = false })
                        Text("Force decline (AAC)", style = MaterialTheme.typography.caption)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        enabled = !running && controller.conn == ConnState.CONNECTED,
                        onClick = {
                            scope.launch {
                                running = true
                                steps.clear()
                                runCatching {
                                    val transport = controller.transport ?: error("not connected")
                                    val rt = TerminalRuntime(transport, config.terminalProfile)
                                    val req = TransactionRequest(
                                        amount = amountToMinor(amount, config.terminalProfile.transactionCurrencyExp),
                                        type = type,
                                        forceOnline = forceOnline,
                                        forceDecline = forceDecline,
                                    )
                                    rt.run(req).collect { steps.add(it) }
                                }.onFailure {
                                    steps.add(TransactionStep.Aborted(System.currentTimeMillis(), Phase.OUTCOME, it.message ?: "error"))
                                }
                                running = false
                            }
                        },
                    ) {
                        if (running) CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (running) "Running…" else "Run transaction")
                    }
                }
                if (controller.conn != ConnState.CONNECTED) {
                    Text(
                        "Connect a transport on the Overview tab first.",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }

        // Outcome card (if available)
        outcome?.let { OutcomeCard(it) }
        abort?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp),
                backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.08f),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Transaction aborted", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colors.error)
                    Text(it.reason, style = MaterialTheme.typography.body2, fontFamily = FontFamily.Monospace, color = MaterialTheme.colors.error)
                }
            }
        }

        // Phase stepper
        PhaseStepper(steps, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun TypeDropdown(selected: TransactionType, onPick: (TransactionType) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text("Type", style = MaterialTheme.typography.caption)
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected.label, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            TransactionType.entries.forEach { t ->
                DropdownMenuItem(onClick = { onPick(t); open = false }) { Text(t.label) }
            }
        }
    }
}

@Composable
private fun OutcomeCard(o: TransactionStep.Outcome) {
    val color = when (o.acType) {
        AcType.TC -> MaterialTheme.colors.primary
        AcType.ARQC -> MaterialTheme.colors.secondary
        AcType.AAC -> MaterialTheme.colors.error
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = color.copy(alpha = 0.08f),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Outcome: ${o.acType}", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, color = color)
                Spacer(Modifier.weight(1f))
                Text("ATC ${"%04X".format(o.atc)}    CID ${"%02X".format(o.cid)}", style = MaterialTheme.typography.caption, fontFamily = FontFamily.Monospace)
            }
            Text("AC : ${o.ac.toHex()}", style = MaterialTheme.typography.body2, fontFamily = FontFamily.Monospace)
            Text("IAD: ${if (o.iad.isEmpty()) "(empty)" else o.iad.toHex()}", style = MaterialTheme.typography.body2, fontFamily = FontFamily.Monospace)
            Text("TVR: ${o.tvr.toHex()}    TSI: ${o.tsi.toHex()}", style = MaterialTheme.typography.body2, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PhaseStepper(steps: List<TransactionStep>, modifier: Modifier = Modifier) {
    // Group steps into phase blocks. Each block: PhaseStart..PhaseEnd inclusive (plus any trailing
    // Outcome/Aborted at OUTCOME phase).
    val blocks = remember(steps.size) { groupByPhase(steps) }
    val listState = rememberLazyListState()
    LaunchedEffect(steps.size) {
        if (blocks.isNotEmpty()) listState.animateScrollToItem(blocks.size - 1)
    }
    Card(modifier = modifier, elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
        Box(Modifier.fillMaxSize().padding(8.dp)) {
            if (blocks.isEmpty()) {
                Text(
                    "Run a transaction to see the EMV Book 3 phase trace.",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            } else {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(blocks, key = { it.phase.name + it.startTime }) { block -> PhaseBlock(block) }
                }
            }
        }
    }
}

private data class PhaseBlock(
    val phase: Phase,
    val startTime: Long,
    val ok: Boolean?,                         // null = still running
    val notes: List<TransactionStep.Note>,
    val exchanges: List<TransactionStep.Exchange>,
    val flags: TransactionStep.Flags?,
)

private fun groupByPhase(steps: List<TransactionStep>): List<PhaseBlock> {
    val out = mutableListOf<PhaseBlock>()
    var current: MutableList<TransactionStep>? = null
    var currentPhase: Phase? = null
    fun flush() {
        val cur = current ?: return
        val phase = currentPhase ?: return
        val start = cur.first().time
        val ok = cur.firstNotNullOfOrNull { (it as? TransactionStep.PhaseEnd)?.ok }
        val notes = cur.filterIsInstance<TransactionStep.Note>()
        val exch = cur.filterIsInstance<TransactionStep.Exchange>()
        val flags = cur.filterIsInstance<TransactionStep.Flags>().lastOrNull()
        out += PhaseBlock(phase, start, ok, notes, exch, flags)
    }
    for (s in steps) {
        when (s) {
            is TransactionStep.PhaseStart -> {
                flush()
                current = mutableListOf(s)
                currentPhase = s.phase
            }
            is TransactionStep.PhaseEnd -> {
                current?.add(s)
            }
            is TransactionStep.Outcome, is TransactionStep.Aborted -> {
                // Render in their own block
                flush()
                current = mutableListOf(s)
                currentPhase = (s as? TransactionStep.Outcome)?.phase ?: (s as? TransactionStep.Aborted)?.phase
            }
            else -> current?.add(s)
        }
    }
    flush()
    return out
}

@Composable
private fun PhaseBlock(block: PhaseBlock) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(6.dp),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                val (icon, color) = when (block.ok) {
                    true -> Icons.Default.Check to MaterialTheme.colors.primary
                    false -> Icons.Default.Close to MaterialTheme.colors.error
                    null -> Icons.Default.PlayArrow to MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                }
                Icon(icon, null, tint = color)
                Spacer(Modifier.width(6.dp))
                Text(
                    block.phase.label,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${block.exchanges.size} APDU(s) — ${block.notes.size} note(s)",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
                OutlinedButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide" else "Show") }
            }
            if (expanded) {
                Column(Modifier.padding(start = 28.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    block.notes.forEach { n ->
                        Text(
                            n.message,
                            style = MaterialTheme.typography.caption,
                            color = if (n.isError) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    block.exchanges.forEach { ex ->
                        Text(
                            "→ ${ex.command.toBytes().toHex()}",
                            style = MaterialTheme.typography.caption,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            "← ${if (ex.response.data.isEmpty()) "" else "${ex.response.data.toHex()}  "}SW=${"%04X".format(ex.response.sw)}",
                            style = MaterialTheme.typography.caption,
                            fontFamily = FontFamily.Monospace,
                            color = if (ex.response.isSuccess) MaterialTheme.colors.onSurface else MaterialTheme.colors.error,
                        )
                    }
                    block.flags?.let { f ->
                        Text(
                            "TVR: ${f.tvr.toHex()}    TSI: ${f.tsi.toHex()}",
                            style = MaterialTheme.typography.caption,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colors.primary,
                        )
                    }
                }
            }
        }
    }
}

private fun amountToMinor(text: String, exp: Int): Long {
    val parts = text.split(".")
    val whole = parts.getOrNull(0)?.toLongOrNull() ?: 0
    val fracRaw = parts.getOrNull(1) ?: ""
    val frac = (fracRaw + "0".repeat(exp)).take(exp).toLongOrNull() ?: 0
    var minor = whole
    repeat(exp) { minor *= 10 }
    return minor + frac
}
