package `in`.aicortex.iso8583studio.web.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.tools.ToolCatalog
import `in`.aicortex.iso8583studio.tools.ToolCategory
import `in`.aicortex.iso8583studio.tools.WebTool
import `in`.aicortex.iso8583studio.web.badge
import `in`.aicortex.iso8583studio.web.chrome.glow
import `in`.aicortex.iso8583studio.web.chrome.insetTop
import `in`.aicortex.iso8583studio.web.chrome.ring
import `in`.aicortex.iso8583studio.web.theme.Glass
import `in`.aicortex.iso8583studio.web.theme.GlassShapes

private fun ToolCategory.icon(): ImageVector = when (this) {
    ToolCategory.EMV -> Icons.Default.CreditCard
    ToolCategory.CIPHER -> Icons.Default.Lock
    ToolCategory.KEYS -> Icons.Default.Key
    ToolCategory.PIN -> Icons.Default.Keyboard
    ToolCategory.CARD_VALIDATION -> Icons.Default.Verified
    ToolCategory.DUKPT -> Icons.Default.Settings
    ToolCategory.MAC -> Icons.Default.Shield
    ToolCategory.CONVERTERS -> Icons.Default.SwapHoriz
}

private const val LEDE = "Payment cryptography calculators running entirely in your browser. Keys and card data never leave this page."

/**
 * The home screen, drawn like the site's tool hub: a category sidebar (chips when narrow), a
 * header with search and a count badge, category cards on the all-tools view, and the tiles.
 */
@Composable
fun ToolHub(category: ToolCategory?, onCategory: (ToolCategory?) -> Unit, onOpen: (WebTool) -> Unit) {
    val c = Glass.colors
    var query by remember { mutableStateOf("") }
    val ready = remember { ToolCatalog.webReady }
    val categories = remember { ToolCategory.entries.filter { cat -> ready.any { it.category == cat } } }
    val shown = remember(category, query) {
        ready.filter { category == null || it.category == category }
            .filter { query.isBlank() || it.label.contains(query, true) || it.description.contains(query, true) || it.id.contains(query, true) }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(c.canvas)) {
        val wide = maxWidth >= 900.dp
        val narrow = maxWidth < 600.dp
        val stages = category == null && query.isBlank() && maxWidth >= 700.dp
        Row(modifier = Modifier.fillMaxSize()) {
            if (wide) {
                Sidebar(categories, category, onCategory, ready)
                Box(Modifier.width(1.dp).fillMaxHeight().background(c.line2))
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = if (narrow) 14.dp else 24.dp, vertical = if (narrow) 16.dp else 22.dp)) {
                HubHeader(category, query, { query = it }, shown.size, narrow)
                if (!wide) {
                    Spacer(Modifier.height(12.dp))
                    CategoryChips(categories, category, onCategory, ready)
                }
                Spacer(Modifier.height(18.dp))
                TilesPanel(category, shown, categories, ready, onCategory, onOpen, showStages = stages, narrow = narrow)
            }
        }
    }
}

@Composable
private fun Sidebar(categories: List<ToolCategory>, selected: ToolCategory?, onCategory: (ToolCategory?) -> Unit, ready: List<WebTool>) {
    val c = Glass.colors
    Column(modifier = Modifier.width(268.dp).fillMaxHeight().padding(horizontal = 18.dp, vertical = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(26.dp).glow(c.blue.copy(alpha = 0.4f), 8.dp, 14.dp).clip(RoundedCornerShape(8.dp)).background(Brush.verticalGradient(listOf(c.blueHover, c.blue))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Wifi, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(14.dp)) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("ISO8583Studio", style = MaterialTheme.typography.subtitle1.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified), color = c.text, fontWeight = FontWeight.SemiBold)
                Text("Professional Payment Testing", style = MaterialTheme.typography.caption, color = c.faint)
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("TOOL CATEGORIES", style = MaterialTheme.typography.overline, color = c.faint)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CategoryRow(Icons.Default.Apps, "All tools", "${ready.size} tools", selected == null) { onCategory(null) }
            categories.forEach { cat ->
                CategoryRow(cat.icon(), cat.label, "${ready.count { it.category == cat }} tools", selected == cat) { onCategory(cat) }
            }
        }
    }
}

@Composable
private fun CategoryRow(icon: ImageVector, name: String, count: String, on: Boolean, onClick: () -> Unit) {
    val c = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
            .background(if (on) c.catOnBg else if (hovered) c.ghostBg else Color.Transparent)
            .border(1.dp, if (on) c.catOnBorder else Color.Transparent, RoundedCornerShape(9.dp))
            .hoverable(interaction).pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (on) c.blueHi else c.faint, modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.body2, color = if (on) c.text else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(count, style = MaterialTheme.typography.caption, color = c.faint)
        }
    }
}

@Composable
private fun CategoryChips(categories: List<ToolCategory>, selected: ToolCategory?, onCategory: (ToolCategory?) -> Unit, ready: List<WebTool>) {
    val c = Glass.colors
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (listOf<ToolCategory?>(null) + categories).forEach { cat ->
            val on = cat == selected
            Row(
                modifier = Modifier.clip(GlassShapes.pill).background(if (on) c.catOnBg else c.ghostBg).border(1.dp, if (on) c.catOnBorder else c.ghostBorder, GlassShapes.pill)
                    .pointerHoverIcon(PointerIcon.Hand).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onCategory(cat) }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(cat?.icon() ?: Icons.Default.Apps, contentDescription = null, tint = if (on) c.blueHi else c.faint, modifier = Modifier.size(12.dp))
                Text(cat?.label ?: "All", style = MaterialTheme.typography.caption, color = if (on) c.text else c.muted, maxLines = 1)
                Text("${if (cat == null) ready.size else ready.count { it.category == cat }}", style = Glass.type.small, color = c.faint)
            }
        }
    }
}

@Composable
private fun HubHeader(category: ToolCategory?, query: String, onQuery: (String) -> Unit, count: Int, narrow: Boolean) {
    val c = Glass.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category?.label ?: "All tools", style = MaterialTheme.typography.h5, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(category?.description ?: LEDE, style = MaterialTheme.typography.body2, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (!narrow) {
                Spacer(Modifier.width(16.dp))
                SearchPill(query, onQuery, Modifier.width(240.dp))
                Spacer(Modifier.width(10.dp))
                CountBadge(count)
            }
        }
        if (narrow) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchPill(query, onQuery, Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                CountBadge(count)
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    val c = Glass.colors
    Text(
        "$count ${if (count == 1) "tool" else "tools"}", style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold), color = c.blueHi, maxLines = 1,
        modifier = Modifier.clip(GlassShapes.pill).background(c.badgeBg).border(1.dp, c.badgeBorder, GlassShapes.pill).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun SearchPill(query: String, onQuery: (String) -> Unit, modifier: Modifier) {
    val c = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    BasicTextField(
        value = query, onValueChange = onQuery, singleLine = true, interactionSource = interaction,
        textStyle = MaterialTheme.typography.body2.copy(color = c.text), cursorBrush = SolidColor(c.blueHi),
        modifier = modifier.height(36.dp).clip(GlassShapes.pill).background(c.boxBg).border(1.dp, c.boxBorder, GlassShapes.pill).insetTop(c.boxInset).pointerHoverIcon(PointerIcon.Text),
        decorationBox = { inner ->
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = c.faint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(9.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search tools…", style = MaterialTheme.typography.body2, color = c.faint, maxLines = 1)
                    inner()
                }
            }
        },
    )
}

@Composable
private fun TilesPanel(
    category: ToolCategory?, shown: List<WebTool>, categories: List<ToolCategory>, ready: List<WebTool>,
    onCategory: (ToolCategory?) -> Unit, onOpen: (WebTool) -> Unit, showStages: Boolean, narrow: Boolean,
) {
    val c = Glass.colors
    Column(
        modifier = Modifier.fillMaxSize().clip(GlassShapes.panel)
            .drawBehind { drawRoundRect(Brush.linearGradient(listOf(c.hubCardTop, c.hubCardBottom), start = Offset.Zero, end = Offset(size.width * 0.35f, size.height)), cornerRadius = CornerRadius(16.dp.toPx())); drawRect(Color.White.copy(alpha = 0.035f), size = Size(size.width, 1f)) }
            .border(1.dp, c.line2, GlassShapes.panel),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(220.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (showStages) "Categories" else category?.label ?: "All tools", style = MaterialTheme.typography.subtitle1, color = c.text)
                        Text(if (showStages) "Pick a group, or search above" else "${shown.size} ${if (shown.size == 1) "tool" else "tools"} · every value stays in your browser", style = MaterialTheme.typography.caption, color = c.muted)
                    }
                    if (!narrow) Text("HEX-DRIVEN · AUDIT LOG", style = MaterialTheme.typography.overline, color = c.faint)
                }
            }
            if (showStages) {
                items(categories, key = { "c-" + it.slug }) { cat -> StageCard(cat, ready.count { it.category == cat }, ToolCatalog.byCategory(cat).size) { onCategory(cat) } }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)) {
                        Text("All tools", style = MaterialTheme.typography.subtitle1, color = c.text)
                        Text("${shown.size} tools · every value stays in your browser", style = MaterialTheme.typography.caption, color = c.muted)
                    }
                }
            }
            if (shown.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { Text("No tool matches that search.", style = MaterialTheme.typography.body2, color = c.muted, modifier = Modifier.padding(vertical = 24.dp)) }
            }
            items(shown, key = { it.id }) { tool -> ToolTile(tool) { onOpen(tool) } }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("›", style = Glass.type.small, color = c.blueHi)
                    Spacer(Modifier.width(6.dp))
                    Text("Keys and card data never leave this page · ${ready.size} tools run in WebAssembly", style = Glass.type.small, color = c.muted)
                }
            }
        }
    }
}

@Composable
private fun StageCard(cat: ToolCategory, ready: Int, total: Int, onClick: () -> Unit) {
    val c = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier = Modifier.fillMaxWidth()
            .then(if (hovered) Modifier.glow(c.tileOnGlow, 12.dp, 24.dp) else Modifier)
            .clip(GlassShapes.tile).background(if (hovered) c.tileOnBg else c.tileBg).border(1.dp, if (hovered) c.tileOnBorder.copy(alpha = 0.28f) else c.tileBorder, GlassShapes.tile)
            .hoverable(interaction).pointerHoverIcon(PointerIcon.Hand).clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("0${ToolCategory.entries.indexOf(cat) + 1}", style = Glass.type.small.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(0.1f, androidx.compose.ui.unit.TextUnitType.Em)), color = if (hovered) c.blueHi else c.faint)
            Spacer(Modifier.weight(1f))
            Text("$ready ${if (ready == 1) "tool" else "tools"}", style = MaterialTheme.typography.caption, color = c.faint)
        }
        Spacer(Modifier.height(8.dp))
        Text(cat.label, style = MaterialTheme.typography.subtitle1, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(cat.description, style = MaterialTheme.typography.caption, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.line)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(if (total == 0) 0f else ready.toFloat() / total).glow(c.blue.copy(alpha = 0.6f), 2.dp, 8.dp).background(Brush.horizontalGradient(listOf(c.blue, c.tealHi))))
        }
    }
}

@Composable
private fun ToolTile(tool: WebTool, onClick: () -> Unit) {
    val c = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp)
            .then(if (hovered) Modifier.glow(c.tileOnGlow, 12.dp, 26.dp).ring(c.tileOnRing, 1.dp, 12.dp) else Modifier)
            .clip(GlassShapes.tile).background(if (hovered) c.tileOnBg else c.tileBg).border(1.dp, if (hovered) c.tileOnBorder else c.tileBorder, GlassShapes.tile)
            .hoverable(interaction).pointerHoverIcon(PointerIcon.Hand).clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(tool.icon, contentDescription = null, tint = if (hovered) c.blueHi else c.faint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(tool.label, style = MaterialTheme.typography.subtitle2, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            if (tool.referenceImpl) {
                Text("ref", style = Glass.type.small, color = c.amberHi, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(c.warnBg).padding(horizontal = 5.dp, vertical = 1.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(tool.category.badge(), style = Glass.type.small, color = if (hovered) c.blueHi else c.faint)
        }
        Spacer(Modifier.height(6.dp))
        Text(tool.description, style = MaterialTheme.typography.caption, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
