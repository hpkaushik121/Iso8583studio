package `in`.aicortex.iso8583studio.web.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.tools.ToolCatalog
import `in`.aicortex.iso8583studio.tools.ToolCategory
import `in`.aicortex.iso8583studio.tools.WebTool
import `in`.aicortex.iso8583studio.web.chrome.GhostIcon
import `in`.aicortex.iso8583studio.web.chrome.glow
import `in`.aicortex.iso8583studio.web.theme.Glass

internal fun ToolCategory.icon(): ImageVector = when (this) {
    ToolCategory.EMV -> Icons.Default.CreditCard
    ToolCategory.CIPHER -> Icons.Default.Lock
    ToolCategory.KEYS -> Icons.Default.Key
    ToolCategory.PIN -> Icons.Default.Keyboard
    ToolCategory.CARD_VALIDATION -> Icons.Default.Verified
    ToolCategory.DUKPT -> Icons.Default.Settings
    ToolCategory.MAC -> Icons.Default.Shield
    ToolCategory.CONVERTERS -> Icons.Default.SwapHoriz
}

internal val TREE_WIDTH = 268.dp

/**
 * The ISO8583Studio S on its gradient tile: the same two arcs as the site's logo mark
 * (web/src/app/ui/logo-mark.ts, a 48-unit box), the top arc white and the bottom teal.
 */
@Composable
fun BrandMark(size: androidx.compose.ui.unit.Dp) {
    val c = Glass.colors
    Canvas(
        modifier = Modifier.size(size).glow(c.blue.copy(alpha = 0.4f), 8.dp, 14.dp).clip(RoundedCornerShape(size * 0.3f)).background(Brush.verticalGradient(listOf(c.blueHover, c.blue))),
    ) {
        val u = this.size.width / 48f
        val stroke = Stroke(width = 5.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // M35 14 H20 A6 6 0 0 0 20 26 H28
        val top = Path().apply {
            moveTo(35f * u, 14f * u); lineTo(20f * u, 14f * u)
            arcTo(Rect(14f * u, 14f * u, 26f * u, 26f * u), -90f, -180f, false)
            lineTo(28f * u, 26f * u)
        }
        // M28 26 A6 6 0 0 1 28 38 H13
        val bottom = Path().apply {
            moveTo(28f * u, 26f * u)
            arcTo(Rect(22f * u, 26f * u, 34f * u, 38f * u), -90f, 180f, false)
            lineTo(13f * u, 38f * u)
        }
        drawPath(top, c.onAccent, style = stroke)
        drawPath(bottom, c.tealHi, style = stroke)
    }
}

/**
 * The category tree in the left column. A row selects the category (its grid), the caret expands
 * the web-ready tools beneath it so another tool is one click away while one is open. Expansion
 * state is the shell's and survives hub ↔ tool.
 */
@Composable
fun CategoryTree(
    route: String,
    expanded: Set<ToolCategory>,
    onToggle: (ToolCategory) -> Unit,
    onCategory: (ToolCategory) -> Unit,
    onOpen: (WebTool) -> Unit,
    brand: Boolean,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = Glass.colors
    val tool = ToolCatalog.byId(route)
    val selected = if (route.startsWith("c/")) ToolCategory.entries.firstOrNull { it.slug == route.removePrefix("c/") } else null
    val ready = remember { ToolCatalog.webReady }
    Column(
        modifier = modifier.width(TREE_WIDTH).fillMaxHeight().background(c.canvas).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        if (brand) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(26.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("ISO8583Studio", style = MaterialTheme.typography.subtitle1, color = c.text, fontWeight = FontWeight.SemiBold)
                    Text("Professional Payment Testing", style = MaterialTheme.typography.caption, color = c.faint)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("TOOL CATEGORIES", style = MaterialTheme.typography.overline, color = c.faint, modifier = Modifier.weight(1f))
            if (onClose != null) GhostIcon(Icons.Default.Close, "Close tool browser", onClick = onClose)
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ToolCategory.entries.forEach { cat ->
                val tools = ready.filter { it.category == cat } + listOfNotNull(tool?.takeIf { it.category == cat && !it.webReady })
                if (tools.isEmpty() && ToolCatalog.byCategory(cat).isEmpty()) return@forEach
                CategoryRow(
                    cat = cat, on = selected == cat, has = tool?.category == cat, open = cat in expanded, tools = tools, activeId = tool?.id,
                    onSelect = { onCategory(cat) }, onToggle = { onToggle(cat) }, onOpen = onOpen,
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    cat: ToolCategory, on: Boolean, has: Boolean, open: Boolean, tools: List<WebTool>, activeId: String?,
    onSelect: () -> Unit, onToggle: () -> Unit, onOpen: (WebTool) -> Unit,
) {
    val c = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val lit = on || has
    val shape = RoundedCornerShape(9.dp)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clip(shape)
                .background(if (on) c.catOnBg else if (hovered) c.ghostBg else Color.Transparent)
                .border(1.dp, if (on) c.catOnBorder else Color.Transparent, shape)
                .hoverable(interaction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).pointerHoverIcon(PointerIcon.Hand)
                    .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
                    .padding(start = 11.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Icon(cat.icon(), contentDescription = null, tint = if (lit) c.blueHi else c.faint, modifier = Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cat.label, style = MaterialTheme.typography.body2, color = if (lit) c.text else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${tools.count { it.webReady }} tools", style = MaterialTheme.typography.caption, color = c.faint)
                }
            }
            Caret(open, cat.label, onToggle)
        }
        if (open) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 2.dp, bottom = 6.dp).border(width = 0.dp, color = Color.Transparent),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Row {
                    Box(Modifier.width(1.dp).fillMaxHeight().background(c.ghostBorder))
                    Column(modifier = Modifier.padding(start = 8.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        tools.forEach { t -> ToolRow(t, on = t.id == activeId) { onOpen(t) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Caret(open: Boolean, label: String, onClick: () -> Unit) {
    val c = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier.width(30.dp).fillMaxHeight().height(44.dp).clip(RoundedCornerShape(topEnd = 9.dp, bottomEnd = 9.dp))
            .background(if (hovered) c.ghostBg else Color.Transparent)
            .hoverable(interaction).pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.KeyboardArrowRight, contentDescription = if (open) "Collapse $label" else "Expand $label",
            tint = if (hovered) c.text else if (open) c.muted else c.faint, modifier = Modifier.size(14.dp).rotate(if (open) 90f else 0f),
        )
    }
}

@Composable
private fun ToolRow(tool: WebTool, on: Boolean, onClick: () -> Unit) {
    val c = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(7.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(if (on) c.catOnBg else if (hovered) c.ghostBg else Color.Transparent)
            .border(1.dp, if (on) c.catOnBorder else Color.Transparent, shape)
            .hoverable(interaction).pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(tool.icon, contentDescription = null, tint = if (on) c.blueHi else c.faint, modifier = Modifier.size(12.dp))
        Text(tool.label, style = MaterialTheme.typography.body2.copy(fontSize = 12.sp), color = if (on || hovered) c.text else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (tool.referenceImpl) {
            Text("ref", style = Glass.type.small.copy(fontSize = 9.5.sp), color = c.amberHi, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(c.warnBg).padding(horizontal = 4.dp, vertical = 1.dp))
        }
    }
}
