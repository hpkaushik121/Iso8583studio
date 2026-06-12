package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.Tlv
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.ApduExchange

/**
 * Four-quadrant view of the latest APDU exchange — matches the host simulator's
 * `SimulatorHandlerTab` quadrant layout exactly:
 *
 *   ┌──────────────────────┬──────────────────────┐
 *   │  Formatted Command   │  Formatted Response  │
 *   ├──────────────────────┼──────────────────────┤
 *   │     Raw Command      │     Raw Response     │
 *   └──────────────────────┴──────────────────────┘
 *
 * Each quadrant has the same chrome as host sim's `QuadrantPanel`: tinted header strip with
 * icon + title + char count + minimize-vertical (collapse to a thin bar) + minimize-horizontal
 * (collapse to a 32dp side rail). Clicking a minimized panel restores it.
 */
@Composable
fun CardQuadrantsView(exchange: ApduExchange?, modifier: Modifier = Modifier) {
    val cmdColor = MaterialTheme.colors.primary
    val rspColor = MaterialTheme.colors.secondary

    var fmtCmdMinV by remember { mutableStateOf(false) }
    var fmtCmdMinH by remember { mutableStateOf(false) }
    var fmtRspMinV by remember { mutableStateOf(false) }
    var fmtRspMinH by remember { mutableStateOf(false) }
    var rawCmdMinV by remember { mutableStateOf(false) }
    var rawCmdMinH by remember { mutableStateOf(false) }
    var rawRspMinV by remember { mutableStateOf(false) }
    var rawRspMinH by remember { mutableStateOf(false) }

    val fmtCmd = remember(exchange) { exchange?.command?.let(::formatCommand) ?: "(no command yet)" }
    val fmtRsp = remember(exchange) { exchange?.response?.let(::formatResponse) ?: "(no response yet)" }
    val rawCmd = remember(exchange) { exchange?.command?.toBytes()?.toHexSpaced() ?: "" }
    val rawRsp = remember(exchange) { exchange?.response?.toBytes()?.toHexSpaced() ?: "" }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Top row — formatted views
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(
                    when {
                        rawCmdMinV && rawRspMinV -> 8f
                        rawCmdMinV || rawRspMinV -> 4f
                        else -> 1f
                    }
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuadrantSlot(
                isHorizontalMin = fmtCmdMinH,
                isVerticalMin = fmtCmdMinV,
                title = "Formatted Command",
                icon = Icons.Default.CallMade,
                color = cmdColor,
                content = fmtCmd,
                onMinV = { fmtCmdMinV = true },
                onMinH = { fmtCmdMinH = true },
                onRestore = { fmtCmdMinV = false; fmtCmdMinH = false },
                rowWeight = quadrantWeight(fmtRspMinH, rawCmdMinH, rawRspMinH),
            )
            QuadrantSlot(
                isHorizontalMin = fmtRspMinH,
                isVerticalMin = fmtRspMinV,
                title = "Formatted Response",
                icon = Icons.Default.CallReceived,
                color = rspColor,
                content = fmtRsp,
                onMinV = { fmtRspMinV = true },
                onMinH = { fmtRspMinH = true },
                onRestore = { fmtRspMinV = false; fmtRspMinH = false },
                rowWeight = quadrantWeight(fmtCmdMinH, rawCmdMinH, rawRspMinH),
            )
        }

        // Bottom row — raw views
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(
                    when {
                        fmtCmdMinV && fmtRspMinV -> 8f
                        fmtCmdMinV || fmtRspMinV -> 4f
                        else -> 1f
                    }
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuadrantSlot(
                isHorizontalMin = rawCmdMinH,
                isVerticalMin = rawCmdMinV,
                title = "Raw Command",
                icon = Icons.Default.Code,
                color = cmdColor,
                content = rawCmd,
                onMinV = { rawCmdMinV = true },
                onMinH = { rawCmdMinH = true },
                onRestore = { rawCmdMinV = false; rawCmdMinH = false },
                rowWeight = quadrantWeight(rawRspMinH, fmtCmdMinH, fmtRspMinH),
            )
            QuadrantSlot(
                isHorizontalMin = rawRspMinH,
                isVerticalMin = rawRspMinV,
                title = "Raw Response",
                icon = Icons.Default.Description,
                color = rspColor,
                content = rawRsp,
                onMinV = { rawRspMinV = true },
                onMinH = { rawRspMinH = true },
                onRestore = { rawRspMinV = false; rawRspMinH = false },
                rowWeight = quadrantWeight(rawCmdMinH, fmtCmdMinH, fmtRspMinH),
            )
        }
    }
}

private fun quadrantWeight(siblingHmin: Boolean, otherRowAHmin: Boolean, otherRowBHmin: Boolean): Float = when {
    siblingHmin && (otherRowAHmin || otherRowBHmin) -> 8f
    siblingHmin -> 4f
    else -> 1f
}

/**
 * Single quadrant cell, picks one of the three states: full panel, vertically minimized (thin bar),
 * or horizontally minimized (narrow rail).
 */
@Composable
private fun androidx.compose.foundation.layout.RowScope.QuadrantSlot(
    isHorizontalMin: Boolean,
    isVerticalMin: Boolean,
    title: String,
    icon: ImageVector,
    color: Color,
    content: String,
    onMinV: () -> Unit,
    onMinH: () -> Unit,
    onRestore: () -> Unit,
    rowWeight: Float,
) {
    when {
        isHorizontalMin -> ApduMinimizedHorizontalQuadrant(title, icon, color, onClick = onRestore)
        isVerticalMin -> ApduMinimizedVerticalQuadrant(
            modifier = Modifier.weight(rowWeight),
            title = title,
            icon = icon,
            color = color,
            onClick = onRestore,
        )
        else -> ApduQuadrantPanel(
            modifier = Modifier.weight(rowWeight),
            title = title,
            icon = icon,
            color = color,
            content = content,
            onMinimizeVertical = onMinV,
            onMinimizeHorizontal = onMinH,
        )
    }
}

@Composable
private fun ApduQuadrantPanel(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    content: String,
    onMinimizeVertical: () -> Unit,
    onMinimizeHorizontal: () -> Unit,
) {
    Surface(modifier = modifier.fillMaxHeight(), elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.10f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    title,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.caption,
                    color = color,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${content.length}",
                    style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                    color = color.copy(alpha = 0.7f),
                )
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onMinimizeVertical, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.ExpandLess, "Minimize Height", tint = color, modifier = Modifier.size(12.dp))
                }
                IconButton(onClick = onMinimizeHorizontal, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.ChevronLeft, "Minimize Width", tint = color, modifier = Modifier.size(12.dp))
                }
            }
            TextField(
                value = content,
                readOnly = true,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            )
        }
    }
}

@Composable
private fun ApduMinimizedVerticalQuadrant(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    val isTopPanel = title.contains("Formatted")
    Column(modifier = modifier.fillMaxHeight()) {
        if (!isTopPanel) Spacer(Modifier.weight(1f))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clickable(onClick = onClick),
            elevation = 2.dp,
            shape = RoundedCornerShape(6.dp),
            color = color.copy(alpha = 0.10f),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
                    Text(title, style = MaterialTheme.typography.caption.copy(fontSize = 11.sp),
                         color = color, fontWeight = FontWeight.Medium)
                }
                Icon(
                    if (isTopPanel) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    null, tint = color, modifier = Modifier.size(10.dp),
                )
            }
        }
        if (isTopPanel) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ApduMinimizedHorizontalQuadrant(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(32.dp).fillMaxHeight().clickable(onClick = onClick),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.10f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Text(
                title.first().toString(),
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Formatters — produce the multiline strings rendered inside formatted quadrants
// ---------------------------------------------------------------------------

internal fun formatCommand(cmd: CommandApdu): String = buildString {
    val ins = cmd.ins.toInt() and 0xFF
    appendLine("CLA  : %02X".format(cmd.cla))
    appendLine("INS  : %02X  %s".format(ins, insName(ins)))
    appendLine("P1   : %02X".format(cmd.p1))
    appendLine("P2   : %02X".format(cmd.p2))
    cmd.data?.let {
        appendLine("Lc   : %02X  (%d bytes)".format(it.size, it.size))
        appendLine("Data : ${it.toHexSpaced()}")
        val tlvs = runCatching { Tlv.parseAll(it) }.getOrNull().orEmpty()
        if (tlvs.isNotEmpty()) {
            appendLine()
            appendLine("TLV:")
            tlvs.forEach { tlv -> appendTlv(tlv, depth = 1) }
        }
    }
    cmd.le?.let { append("Le   : %02X  (expect ${if (it == 256) 256 else it} bytes)".format(if (it == 256) 0 else it)) }
}

internal fun formatResponse(rsp: ResponseApdu): String = buildString {
    appendLine("SW   : %04X  %s".format(rsp.sw, swMeaning(rsp.sw)))
    if (rsp.data.isNotEmpty()) {
        appendLine("Len  : ${rsp.data.size} bytes")
        appendLine("Data : ${rsp.data.toHexSpaced()}")
        val tlvs = runCatching { Tlv.parseAll(rsp.data) }.getOrNull().orEmpty()
        if (tlvs.isNotEmpty()) {
            appendLine()
            appendLine("TLV:")
            tlvs.forEach { tlv -> appendTlv(tlv, depth = 1) }
        }
    }
}

private fun StringBuilder.appendTlv(tlv: Tlv, depth: Int) {
    val indent = "  ".repeat(depth)
    val name = tagName(tlv.tag)
    if (tlv.isConstructed) {
        appendLine("$indent%X  $name".format(tlv.tag))
        val children = runCatching { Tlv.parseAll(tlv.value) }.getOrNull().orEmpty()
        children.forEach { child -> appendTlv(child, depth + 1) }
    } else {
        appendLine("$indent%X  $name".format(tlv.tag))
        appendLine("$indent  ${tlv.value.toHexSpaced()}")
    }
}
