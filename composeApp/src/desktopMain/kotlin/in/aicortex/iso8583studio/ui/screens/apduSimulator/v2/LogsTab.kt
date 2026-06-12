package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.ApduExchange
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogTab
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Trace Log tab — common log view across all APDU simulator modes. Delegates to the host
 * simulator's [LogTab] so the chrome (filter dropdown, auto-scroll, floating stats overlay,
 * type-aware coloring) is identical across simulators.
 *
 * Each [ApduExchange] is projected into a single [LogEntry]:
 *   - type = TRANSACTION on success, ERROR on non-9000 SW
 *   - message = INS name + AID/tag context
 *   - details = formatted command + formatted response (multiline, monospace-friendly)
 *   - correlationId = 1-based exchange index
 *
 * The floating stats overlay shows APDU-relevant counters: total exchanges, active session,
 * bytes in/out summed across all exchanges.
 */
@Composable
fun LogsTab(controller: SimulatorController) {
    val entries = remember { mutableStateListOf<LogEntry>() }

    // Project new exchanges into LogEntry as they arrive. We track how many we've already
    // converted so re-renders don't duplicate.
    LaunchedEffect(controller.exchanges.size) {
        while (entries.size < controller.exchanges.size) {
            entries += controller.exchanges[entries.size].toLogEntry(seq = entries.size + 1)
        }
        // Honor "Clear" on the controller (which empties exchanges).
        if (controller.exchanges.isEmpty() && entries.isNotEmpty()) entries.clear()
    }

    val bytesOut by remember {
        derivedStateOf { controller.exchanges.sumOf { it.command.toBytes().size.toLong() } }
    }
    val bytesIn by remember {
        derivedStateOf { controller.exchanges.sumOf { it.response.toBytes().size.toLong() } }
    }

    LogTab(
        label = "APDU exchange log",
        onClearClick = {
            controller.exchanges.clear()
            entries.clear()
        },
        connectionCount = controller.exchanges.size,
        concurrentConnections = if (controller.conn == ConnState.CONNECTED) 1 else 0,
        bytesIncoming = bytesIn,
        bytesOutgoing = bytesOut,
        logEntries = entries,
    )
}

private val timestampFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

private fun ApduExchange.toLogEntry(seq: Int): LogEntry {
    val ins = command.ins.toInt() and 0xFF
    val swOk = response.isSuccess
    val type = when {
        !swOk -> LogType.ERROR
        ins in setOf(0xA4, 0xA8, 0xAE) -> LogType.TRANSACTION
        else -> LogType.MESSAGE
    }
    val cmdHex = command.toBytes().toHexSpaced()
    val rspHex = response.toBytes().toHexSpaced()
    val message = "%02X %s  →  SW %04X".format(ins, insName(ins), response.sw)
    val details = buildString {
        appendLine("→ Command")
        appendLine(formatCommand(command).trimEnd())
        appendLine()
        appendLine("← Response  (${durationMs} ms)")
        appendLine(formatResponse(response).trimEnd())
        appendLine()
        appendLine("Command hex : $cmdHex")
        append("Response hex: $rspHex")
    }
    return LogEntry(
        timestamp = LocalDateTime.now().format(timestampFmt),
        type = type,
        message = message,
        details = details,
        source = "APDU/$transportName",
        correlationId = "#$seq",
    )
}
