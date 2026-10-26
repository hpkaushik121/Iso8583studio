package `in`.aicortex.iso8583studio.web.chrome

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec
import `in`.aicortex.iso8583studio.ui.chrome.ToolTab
import `in`.aicortex.iso8583studio.web.LocalBrowseTools
import `in`.aicortex.iso8583studio.web.LocalToolContext
import `in`.aicortex.iso8583studio.web.home.hubPanel
import `in`.aicortex.iso8583studio.web.badge
import `in`.aicortex.iso8583studio.web.theme.Glass
import `in`.aicortex.iso8583studio.web.theme.GlassShapes
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RRect

/**
 * A tool on the web: a title row (back, browse when the tree is hidden, title, category and
 * description, badge, actions) and one rounded panel holding the tabs, the form and the activity
 * log, beside each other from 760dp and stacked with the log as a drawer below that.
 */
@Composable
fun GlassScreen(spec: ToolScreenSpec, content: @Composable () -> Unit) {
    val c = Glass.colors
    val tool = LocalToolContext.current
    val browse = LocalBrowseTools.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(c.canvas)) {
        val twoCol = maxWidth >= 760.dp
        val narrow = maxWidth < 600.dp
        val showBadge = maxWidth >= 480.dp
        val drawerHeight = minOf(260.dp, maxHeight * 0.42f)
        Column(modifier = Modifier.fillMaxSize()) {
            // Title row.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GhostIcon(Icons.Default.ArrowBack, "Back to all tools", onClick = spec.onBack)
                if (browse != null) GhostIcon(Icons.Default.Menu, "Browse tools", onClick = browse)
                Column(modifier = Modifier.weight(1f)) {
                    Text(spec.title, style = MaterialTheme.typography.h5.copy(fontSize = 20.sp, letterSpacing = TextUnit(-0.02f, TextUnitType.Em)), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (tool != null && !narrow) {
                        Spacer(Modifier.height(2.dp))
                        Text("${tool.category.label} · ${tool.description}", style = MaterialTheme.typography.body2.copy(fontSize = 12.sp), color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                spec.actions?.let { actions -> actions() }
                if (tool != null && showBadge) {
                    Text(
                        tool.category.badge(), style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold), color = c.blueHi,
                        modifier = Modifier.clip(GlassShapes.pill).background(c.badgeBg).border(1.dp, c.badgeBorder, GlassShapes.pill).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Hairline()
            val banner: @Composable () -> Unit = {
                if (tool?.referenceImpl == true) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(9.dp)).background(c.warnBg).border(1.dp, c.warnBorder, RoundedCornerShape(9.dp)).padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = c.amberHi, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reference implementation: follows public references for a vendor-specific scheme and has not been verified against the vendor's own specification.", style = MaterialTheme.typography.caption, color = c.amberHi)
                    }
                }
            }
            // The panel: tabs, then the form beside the log or stacked over a log drawer.
            Column(
                modifier = Modifier.fillMaxSize().padding(start = if (narrow) 14.dp else 20.dp, end = if (narrow) 14.dp else 20.dp, top = if (narrow) 12.dp else 16.dp, bottom = if (narrow) 14.dp else 20.dp).hubPanel(c),
            ) {
                spec.tabs?.let { tabs ->
                    GlassTabs(tabs, spec.selectedTab, spec.onTabSelected)
                    Hairline()
                }
                if (twoCol) {
                    // The form column scrolls itself: not every screen brings a scroll container, and a
                    // screen that does nests harmlessly (its column just takes its content height).
                    Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { banner(); content() }
                        spec.log?.let { GlassLogPanel(it, Modifier.width(320.dp).fillMaxHeight()) }
                    }
                } else {
                    val count = spec.log?.entries?.size ?: 0
                    var logOpen by remember { mutableStateOf(false) }
                    LaunchedEffect(count) { if (count > 0) logOpen = true }
                    Column(modifier = Modifier.fillMaxSize().padding(if (narrow) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) { banner(); content() }
                        spec.log?.let { log ->
                            if (logOpen) GlassLogPanel(log, Modifier.fillMaxWidth().height(drawerHeight), onCollapse = { logOpen = false })
                            else GlassLogBar(log, onExpand = { logOpen = true })
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Hairline() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Glass.colors.line2))
}

/** A small round icon button without Material's 48dp footprint. */
@Composable
internal fun GhostIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String?, size: androidx.compose.ui.unit.Dp = 24.dp, onClick: () -> Unit) {
    val c = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(if (hovered) c.ghostBg else Color.Transparent)
            .hoverable(interaction).pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (hovered) c.text else c.faint, modifier = Modifier.size(size / 2 + 1.dp))
    }
}

/** The tab strip: the selected tab underlined in blue with a glow, the others muted. */
@Composable
fun GlassTabs(tabs: List<ToolTab>, selected: Int, onSelected: (Int) -> Unit) {
    val c = Glass.colors
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        tabs.forEachIndexed { index, tab ->
            val isOn = index == selected
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val colour = if (isOn || hovered) c.text else c.muted
            Row(
                modifier = Modifier
                    .then(if (isOn) Modifier.drawBehind {
                        val l = 10.dp.toPx(); val h = 2.dp.toPx(); val y = size.height - h
                        val paint = Paint().apply { color = c.blueHi.toArgb(); maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, 5f) }
                        drawContext.canvas.nativeCanvas.drawRRect(RRect.makeLTRB(l, y, size.width - l, y + h, h), paint)
                        drawRoundRect(c.blueHi, Offset(l, y), Size(size.width - 2 * l, h), CornerRadius(h))
                    } else Modifier)
                    .hoverable(interaction).pointerHoverIcon(PointerIcon.Hand)
                    .clickable(interactionSource = interaction, indication = null) { onSelected(index) }
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(tab.icon, contentDescription = null, tint = colour, modifier = Modifier.size(12.dp))
                Text(tab.title, style = MaterialTheme.typography.body2, color = colour, maxLines = 1)
            }
        }
    }
}
