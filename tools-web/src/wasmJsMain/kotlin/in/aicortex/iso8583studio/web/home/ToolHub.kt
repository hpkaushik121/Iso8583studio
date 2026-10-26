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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.tools.ToolCatalog
import `in`.aicortex.iso8583studio.tools.ToolCategory
import `in`.aicortex.iso8583studio.tools.WebTool
import `in`.aicortex.iso8583studio.web.LocalBrowseTools
import `in`.aicortex.iso8583studio.web.badge
import `in`.aicortex.iso8583studio.web.chrome.GhostIcon
import `in`.aicortex.iso8583studio.web.chrome.glow
import `in`.aicortex.iso8583studio.web.chrome.insetTop
import `in`.aicortex.iso8583studio.web.chrome.ring
import `in`.aicortex.iso8583studio.web.theme.Glass
import `in`.aicortex.iso8583studio.web.theme.GlassShapes

private const val LEDE = "Payment cryptography calculators running entirely in your browser. Keys and card data never leave this page."

/** The rounded inner panel the hub grid and a tool's body sit in. */
internal fun Modifier.hubPanel(c: `in`.aicortex.iso8583studio.web.theme.GlassColors): Modifier = this
    .clip(GlassShapes.panel)
    .drawBehind {
        drawRoundRect(Brush.linearGradient(listOf(c.hubCardTop, c.hubCardBottom), start = Offset.Zero, end = Offset(size.width * 0.35f, size.height)), cornerRadius = CornerRadius(16.dp.toPx()))
        drawRect(Color.White.copy(alpha = 0.035f), size = Size(size.width, 1f))
    }
    .border(1.dp, c.line2, GlassShapes.panel)

/**
 * The home hub in the column beside the category tree: title, search and count, chips when the
 * tree is hidden, then the panel. Home shows the eight category cards sharing the panel's height;
 * a category or a search shows the tool tiles.
 */
@Composable
fun ToolHub(category: ToolCategory?, onCategory: (ToolCategory?) -> Unit, onOpen: (WebTool) -> Unit, wide: Boolean) {
    val c = Glass.colors
    var query by remember { mutableStateOf("") }
    val ready = remember { ToolCatalog.webReady }
    val categories = remember { ToolCategory.entries.filter { cat -> ready.any { it.category == cat } } }
    val needle = query.trim()
    val stages = category == null && needle.isEmpty()
    val shown = remember(category, needle) {
        if (category == null && needle.isEmpty()) emptyList()
        else ready.filter { category == null || it.category == category }
            .filter { needle.isEmpty() || it.label.contains(needle, true) || it.description.contains(needle, true) || it.id.contains(needle, true) }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(c.canvas)) {
        val narrow = maxWidth < 600.dp
        val innerW = maxWidth - (if (narrow) 28.dp else 48.dp) - 40.dp
        val cols = when { innerW >= 620.dp -> 4; innerW >= 500.dp -> 3; innerW >= 360.dp -> 2; else -> 1 }
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (narrow) 14.dp else 24.dp, vertical = if (narrow) 16.dp else 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LocalBrowseTools.current?.let { browse ->
                    GhostIcon(Icons.Default.Menu, if (browse.shown) "Hide tool categories" else "Show tool categories", onClick = browse.toggle)
                    Spacer(Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(category?.label ?: "Tool categories", style = MaterialTheme.typography.h5.copy(letterSpacing = TextUnit(-0.02f, TextUnitType.Em)), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(category?.description ?: LEDE, style = MaterialTheme.typography.body2, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (!narrow) {
                    Spacer(Modifier.width(16.dp))
                    SearchPill(query, { query = it }, Modifier.width(240.dp))
                    Spacer(Modifier.width(10.dp))
                    CountBadge(if (stages) ready.size else shown.size)
                }
            }
            if (narrow) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SearchPill(query, { query = it }, Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    CountBadge(if (stages) ready.size else shown.size)
                }
            }
            if (!wide) {
                Spacer(Modifier.height(12.dp))
                CategoryChips(categories, category, { query = ""; onCategory(it) }, ready)
            }
            Spacer(Modifier.height(18.dp))
            Box(modifier = Modifier.fillMaxSize().hubPanel(c)) {
                if (stages && cols > 1) StageGrid(categories, cols, ready) { query = ""; onCategory(it) }
                else TileGrid(shown, if (stages) categories else emptyList(), ready, { query = ""; onCategory(it) }, onOpen)
            }
        }
    }
}

/** Home on a wide enough panel: the category cards share the height, no inner scroll. */
@Composable
private fun StageGrid(categories: List<ToolCategory>, cols: Int, ready: List<WebTool>, onCategory: (ToolCategory) -> Unit) {
    val c = Glass.colors
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categories.chunked(cols).forEach { row ->
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { cat -> Box(Modifier.weight(1f).fillMaxHeight()) { StageCard(cat, ready.count { it.category == cat }, ToolCatalog.byCategory(cat).size, big = true, fill = true) { onCategory(cat) } } }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        FootNote(ready.size, Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun TileGrid(shown: List<WebTool>, stageCategories: List<ToolCategory>, ready: List<WebTool>, onCategory: (ToolCategory) -> Unit, onOpen: (WebTool) -> Unit) {
    val c = Glass.colors
    LazyVerticalGrid(
        columns = GridCells.Adaptive(220.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(stageCategories, key = { "c-" + it.slug }) { cat -> StageCard(cat, ready.count { it.category == cat }, ToolCatalog.byCategory(cat).size, big = false, fill = false) { onCategory(cat) } }
        if (stageCategories.isEmpty() && shown.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Text("No tool matches that search.", style = MaterialTheme.typography.body2, color = c.muted, modifier = Modifier.padding(vertical = 24.dp)) }
        }
        items(shown, key = { it.id }) { tool -> ToolTile(tool) { onOpen(tool) } }
        item(span = { GridItemSpan(maxLineSpan) }) { FootNote(ready.size, Modifier.padding(top = 10.dp)) }
    }
}

@Composable
private fun FootNote(count: Int, modifier: Modifier = Modifier) {
    val c = Glass.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("›", style = Glass.type.small, color = c.blueHi)
        Spacer(Modifier.width(6.dp))
        Text("Keys and card data never leave this page · $count tools run in WebAssembly", style = Glass.type.small, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CategoryChips(categories: List<ToolCategory>, selected: ToolCategory?, onCategory: (ToolCategory?) -> Unit, ready: List<WebTool>) {
    val c = Glass.colors
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        categories.forEach { cat ->
            val on = cat == selected
            Row(
                modifier = Modifier.clip(GlassShapes.pill).background(if (on) c.catOnBg else c.ghostBg).border(1.dp, if (on) c.catOnBorder else c.ghostBorder, GlassShapes.pill)
                    .pointerHoverIcon(PointerIcon.Hand).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onCategory(if (on) null else cat) }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(cat.icon(), contentDescription = null, tint = if (on) c.blueHi else c.faint, modifier = Modifier.size(12.dp))
                Text(cat.label, style = MaterialTheme.typography.caption, color = if (on) c.text else c.muted, maxLines = 1)
                Text("${ready.count { it.category == cat }}", style = Glass.type.small, color = c.faint)
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
private fun StageCard(cat: ToolCategory, ready: Int, total: Int, big: Boolean, fill: Boolean, onClick: () -> Unit) {
    val c = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier = Modifier.fillMaxWidth().then(if (fill) Modifier.fillMaxHeight() else Modifier)
            .then(if (hovered) Modifier.glow(c.tileOnGlow, 12.dp, 24.dp) else Modifier)
            .clip(GlassShapes.tile).background(if (hovered) c.tileOnBg else c.tileBg).border(1.dp, if (hovered) c.tileOnBorder.copy(alpha = 0.28f) else c.tileBorder, GlassShapes.tile)
            .hoverable(interaction).pointerHoverIcon(PointerIcon.Hand).clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (big) 18.dp else 14.dp, vertical = if (big) 16.dp else 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (big) {
                Icon(cat.icon(), contentDescription = null, tint = if (hovered) c.blueHi else c.faint, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text("0${ToolCategory.entries.indexOf(cat) + 1}", style = Glass.type.small.copy(letterSpacing = TextUnit(0.1f, TextUnitType.Em)), color = if (hovered) c.blueHi else c.faint)
            Spacer(Modifier.weight(1f))
            Text("$ready ${if (ready == 1) "tool" else "tools"}", style = MaterialTheme.typography.caption, color = c.faint)
        }
        Spacer(Modifier.height(if (big) 14.dp else 8.dp))
        Text(cat.label, style = if (big) MaterialTheme.typography.subtitle1.copy(fontSize = 15.sp) else MaterialTheme.typography.subtitle1, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(if (big) 3.dp else 1.dp))
        Text(cat.description, style = if (big) MaterialTheme.typography.body2 else MaterialTheme.typography.caption, color = c.muted, maxLines = if (big) 2 else 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        if (fill) Spacer(Modifier.weight(1f))
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
