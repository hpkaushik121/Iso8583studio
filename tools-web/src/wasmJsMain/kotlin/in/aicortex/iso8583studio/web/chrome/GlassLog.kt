package `in`.aicortex.iso8583studio.web.chrome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.delay
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.ui.chrome.ToolLog
import `in`.aicortex.iso8583studio.web.LocalToolContext
import `in`.aicortex.iso8583studio.web.theme.Glass
import `in`.aicortex.iso8583studio.web.theme.GlassShapes

private fun prefersReducedMotion(): Boolean = js("!!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches)")

private enum class LogKind(val tag: String) { IN("IN"), RUN("RUN"), OUT("OUT"), ERR("ERR"), WARN("WARN") }

private fun kindOf(entry: LogEntry): LogKind = when (entry.type.name) {
    "ERROR" -> LogKind.ERR
    "WARNING" -> LogKind.WARN
    "TRANSACTION", "AUTHORIZATION", "SETTLEMENT", "METRICS", "BITMAP", "REVERSAL" -> LogKind.OUT
    "HSM", "ENCRYPTION", "SECURITY", "PERFORMANCE" -> LogKind.RUN
    else -> LogKind.IN
}

/** The one-line summary of an entry: the first result line for OUT, otherwise the first detail line. */
private fun summaryOf(entry: LogEntry, kind: LogKind): String {
    val lines = entry.details?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    if (kind == LogKind.OUT) {
        val i = lines.indexOfFirst { it.startsWith("Result") }
        if (i >= 0 && i + 1 < lines.size) return lines[i + 1]
    }
    if (kind == LogKind.ERR) {
        val i = lines.indexOfFirst { it.startsWith("Error") }
        if (i >= 0) return lines[i].removePrefix("Error:").trim().ifEmpty { lines.getOrElse(i + 1) { "" } }
    }
    return lines.firstOrNull { !it.startsWith("Inputs") && !it.startsWith("Result") } ?: ""
}

private fun lineColours(kind: LogKind, c: `in`.aicortex.iso8583studio.web.theme.GlassColors): Triple<Color, Color, Color> = when (kind) {
    LogKind.IN -> Triple(c.blueHi, c.muted, Color.Transparent)
    LogKind.RUN -> Triple(c.text, c.text, Color.Transparent)
    LogKind.OUT -> Triple(c.tealHi, c.text, c.outWash)
    LogKind.ERR -> Triple(c.redHi, c.redHi, c.errWash)
    LogKind.WARN -> Triple(c.amberHi, c.amberHi, Color.Transparent)
}

private fun stampOf(timestamp: String): String = if (timestamp.length >= 8 && timestamp[2] == ':' && timestamp[5] == ':') timestamp.substring(3, 8) else timestamp.take(5)

/**
 * The `.gp-log` activity log: stamp | tag | text rows with their details inline.
 *
 * Compose for Web cannot copy a drag selection (the selection manager skips the copy key on web
 * and the toolbar only appears in touch mode), so copying is explicit here: click a value in the
 * details to copy it, click a row and press Ctrl/Cmd+C to copy the whole entry, or use the copy
 * icon. The header confirms what was copied.
 */
@Composable
fun GlassLogPanel(log: ToolLog, modifier: Modifier = Modifier, onCollapse: (() -> Unit)? = null) {
    val c = Glass.colors
    val entries = log.entries
    val listState = rememberLazyListState()
    var autoScroll by remember { mutableStateOf(true) }
    LaunchedEffect(entries.size, autoScroll) {
        if (autoScroll && entries.isNotEmpty()) listState.scrollToItem(entries.size - 1)
    }
    val atBottom by remember { derivedStateOf { val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1; entries.isEmpty() || last >= entries.size - 2 } }
    var scrolled by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) scrolled = true else if (scrolled) autoScroll = atBottom
    }
    LaunchedEffect(entries.isEmpty()) { if (entries.isEmpty()) autoScroll = true }

    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf<String?>(null) }
    var copyTick by remember { mutableStateOf(0) }
    LaunchedEffect(copyTick) { if (copyTick > 0) { delay(1600); copied = null } }
    val copy: (String, String) -> Unit = { label, text -> clipboard.setText(AnnotatedString(text)); copied = label; copyTick++ }
    var activeId by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier.clip(GlassShapes.tile).background(c.logBg).border(1.dp, c.logBorder, GlassShapes.tile).insetTop(c.boxInset.copy(alpha = 0.35f))
            .focusRequester(focusRequester).focusable()
            .onKeyEvent { event ->
                val isCopy = event.type == KeyEventType.KeyDown && event.key == Key.C && (event.isCtrlPressed || event.isMetaPressed)
                val entry = entries.firstOrNull { it.id == activeId } ?: entries.lastOrNull()
                if (isCopy && entry != null) { copy("entry", entryText(entry)); true } else false
            },
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text((log.label ?: "Activity log").uppercase(), style = MaterialTheme.typography.overline, color = c.faint)
            Spacer(Modifier.weight(1f))
            val note = copied
            if (note != null) {
                Icon(Icons.Default.Check, contentDescription = null, tint = c.tealHi, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("copied $note", style = Glass.type.small, color = c.tealHi)
            } else {
                Text(when (entries.size) { 0 -> "idle"; 1 -> "1 entry"; else -> "${entries.size} entries" }, style = Glass.type.small, color = c.faint)
            }
            if (entries.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                GhostIcon(Icons.Default.Delete, "Clear log", size = 20.dp, onClick = log.onClear)
            }
            if (onCollapse != null) {
                Spacer(Modifier.width(4.dp))
                GhostIcon(Icons.Default.KeyboardArrowDown, "Hide log", size = 20.dp, onClick = onCollapse)
            }
        }
        Hairline()
        if (entries.isEmpty()) {
            IdleLines()
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(entries, key = { it.id }) { entry ->
                    LogLine(
                        entry = entry,
                        active = entry.id == activeId,
                        onActivate = { activeId = entry.id; focusRequester.requestFocus() },
                        onCopy = copy,
                    )
                }
            }
        }
    }
}

/** What Ctrl/Cmd+C and the copy icon put on the clipboard for an entry. */
private fun entryText(entry: LogEntry): String = entry.details?.takeIf { it.isNotBlank() } ?: entry.message

/** The collapsed activity log on stacked layouts: one row with the newest result that opens the drawer. */
@Composable
fun GlassLogBar(log: ToolLog, onExpand: () -> Unit) {
    val c = Glass.colors
    val entries = log.entries
    val last = entries.lastOrNull()
    Row(
        modifier = Modifier.fillMaxWidth().clip(GlassShapes.tile).background(c.logBg).border(1.dp, c.logBorder, GlassShapes.tile)
            .pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onExpand).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text((log.label ?: "Activity log").uppercase(), style = MaterialTheme.typography.overline, color = c.faint)
        Spacer(Modifier.width(12.dp))
        if (last != null) {
            val kind = kindOf(last)
            Text(kind.tag, style = Glass.type.small.copy(fontWeight = FontWeight.SemiBold), color = lineColours(kind, c).first)
            Spacer(Modifier.width(8.dp))
            Text(summaryOf(last, kind), style = Glass.type.small, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.width(8.dp))
        Text(when (entries.size) { 0 -> "idle"; 1 -> "1 entry"; else -> "${entries.size} entries" }, style = Glass.type.small, color = c.faint)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Show log", tint = c.faint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun IdleLines() {
    val c = Glass.colors
    val tool = LocalToolContext.current
    val still = remember { prefersReducedMotion() }
    val transition = rememberInfiniteTransition(label = "caret")
    val animated by transition.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes { durationMillis = 1000; 1f at 0 using LinearEasing; 1f at 499; 0f at 500; 0f at 999 }),
        label = "caret",
    )
    val blink = if (still) 1f else animated
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
        Text("› session opened · ${tool?.label?.lowercase() ?: "calculator"}", style = Glass.type.log, color = c.faint)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("› waiting for input", style = Glass.type.log, color = c.faint)
            Spacer(Modifier.width(3.dp))
            Box(Modifier.size(6.dp, 11.dp).alpha(blink).background(c.blueHi))
        }
    }
}

@Composable
private fun LogLine(entry: LogEntry, active: Boolean, onActivate: () -> Unit, onCopy: (String, String) -> Unit) {
    val c = Glass.colors
    val kind = remember(entry) { kindOf(entry) }
    val summary = remember(entry) { summaryOf(entry, kind) }
    val hasDetails = !entry.details.isNullOrBlank()
    var expanded by remember { mutableStateOf(true) }
    val (tagColour, textColour, wash) = lineColours(kind, c)
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = Modifier.fillMaxWidth().clip(shape).background(wash)
            .then(if (active) Modifier.border(1.dp, c.lineHi, shape) else Modifier)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onActivate)
            .padding(horizontal = 5.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(stampOf(entry.timestamp), style = Glass.type.log, color = c.faint, modifier = Modifier.width(42.dp))
            Text(kind.tag, style = Glass.type.log.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.06.em), color = tagColour, modifier = Modifier.width(34.dp))
            Text(
                buildAnnotatedString {
                    if (kind == LogKind.OUT || kind == LogKind.ERR) withStyle(SpanStyle(color = tagColour, fontWeight = FontWeight.Medium)) { append(entry.message); append("  ") }
                    append(summary)
                },
                style = Glass.type.log, color = textColour,
                maxLines = if (expanded) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (hasDetails) {
                Spacer(Modifier.width(4.dp))
                GhostIcon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, if (expanded) "Hide details" else "Show details", size = 18.dp) { expanded = !expanded }
            }
        }
        if (expanded && hasDetails) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(10.dp)).background(c.insetBoxBg).border(1.dp, c.insetBoxBorder, RoundedCornerShape(10.dp)).padding(horizontal = 11.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DETAILS", style = Glass.type.path, color = c.faint)
                    Spacer(Modifier.weight(1f))
                    Text("click a value to copy it", style = Glass.type.small, color = c.faint)
                    Spacer(Modifier.width(8.dp))
                    GhostIcon(Icons.Default.ContentCopy, "Copy all details", size = 20.dp) { onCopy("details", entry.details ?: "") }
                }
                Spacer(Modifier.height(6.dp))
                entry.details.orEmpty().lines().forEach { line -> DetailLine(line, onCopy) }
            }
        }
    }
}

private val KEY_VALUE = Regex("""^(\s*)([^:]{1,40}):\s+(\S.*)$""")

/** One line of an entry's details: `Key: value` lines get a click-to-copy value, others stay plain text. */
@Composable
private fun DetailLine(line: String, onCopy: (String, String) -> Unit) {
    val c = Glass.colors
    val match = KEY_VALUE.find(line)
    if (match == null) {
        Text(line, style = Glass.type.details, color = c.muted)
        return
    }
    val (indent, key, value) = match.destructured
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(verticalAlignment = Alignment.Top) {
        Text("$indent$key: ", style = Glass.type.details, color = c.muted)
        Row(
            modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(4.dp)).background(if (hovered) c.ghostBg else Color.Transparent)
                .hoverable(interaction).pointerHoverIcon(PointerIcon.Hand)
                .clickable(interactionSource = interaction, indication = null) { onCopy(key.trim(), value) }
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(value, style = Glass.type.details, color = if (hovered) c.text else c.muted, modifier = Modifier.weight(1f, fill = false))
            if (hovered) {
                Spacer(Modifier.width(5.dp))
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy $key", tint = c.faint, modifier = Modifier.size(11.dp).padding(top = 1.dp))
            }
        }
    }
}
