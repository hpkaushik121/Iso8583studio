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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec
import `in`.aicortex.iso8583studio.ui.chrome.ToolTab
import `in`.aicortex.iso8583studio.web.LocalToolContext
import `in`.aicortex.iso8583studio.web.badge
import `in`.aicortex.iso8583studio.web.theme.Glass
import `in`.aicortex.iso8583studio.web.theme.GlassShapes
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RRect

/** The glass panel that frames a tool on the web: head, tabs, banner, form and activity log. */
@Composable
fun GlassScreen(spec: ToolScreenSpec, content: @Composable () -> Unit) {
    val c = Glass.colors
    val tool = LocalToolContext.current
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(c.canvas).drawBehind { drawBlobs(c.blobBlue, c.blobTeal, c.blobHi) }
    ) {
        val wide = maxWidth >= 900.dp
        val narrow = maxWidth < 600.dp
        val pad = if (narrow) 12.dp else 16.dp
        val showBadge = maxWidth >= 480.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .clip(GlassShapes.panel)
                .drawBehind {
                    drawRoundRect(
                        Brush.linearGradient(listOf(c.panelTop, c.panelBottom), start = Offset.Zero, end = Offset(size.width * 0.35f, size.height)),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                    )
                }
                .dotGrid(c.dot)
                .border(1.dp, c.panelBorder, GlassShapes.panel)
                .drawWithContent {
                    drawContent()
                    drawRect(c.panelInsetHi, topLeft = Offset(0f, 1f), size = Size(size.width, 1f))
                    drawRect(
                        Brush.horizontalGradient(listOf(Color.Transparent, c.panelHighlight, Color.Transparent), startX = size.width * 0.1f, endX = size.width * 0.9f),
                        size = Size(size.width, 1f),
                    )
                }
        ) {
            // Head: back, window lights, title, breadcrumb, badge, actions.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GhostIcon(Icons.Default.ArrowBack, "Back to all tools", onClick = spec.onBack)
                Spacer(Modifier.width(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(3) { Box(Modifier.size(8.dp).clip(CircleShape).background(c.windowDot)) } }
                Spacer(Modifier.width(10.dp))
                Text(spec.title, style = MaterialTheme.typography.h6, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.weight(1f))
                if (tool != null && !narrow) {
                    val sub = spec.tabs?.getOrNull(spec.selectedTab)?.title ?: tool.description
                    Text("${tool.category.slug} › $sub".uppercase(), style = Glass.type.path, color = c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(2f, fill = false))
                    Spacer(Modifier.width(10.dp))
                }
                if (tool != null && showBadge) {
                    Text(
                        tool.category.badge(), style = MaterialTheme.typography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = c.blueHi,
                        modifier = Modifier.clip(GlassShapes.pill).background(c.badgeBg).border(1.dp, c.badgeBorder, GlassShapes.pill).padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
                spec.actions?.let { actions -> Spacer(Modifier.width(6.dp)); actions() }
            }
            Hairline()
            spec.tabs?.let { tabs ->
                GlassTabs(tabs, spec.selectedTab, spec.onTabSelected)
                Hairline()
            }
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
            // Body: the form keeps a bounded height (as on desktop) so screens scroll themselves.
            if (wide) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) { banner(); content() }
                    spec.log?.let { GlassLogPanel(it, Modifier.width(320.dp).fillMaxHeight()) }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(if (narrow) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) { banner(); content() }
                    spec.log?.let { GlassLogPanel(it, Modifier.fillMaxWidth().height(240.dp)) }
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

/** The tab strip: selected tab underlined in blue with a glow, earlier tabs ticked in teal. */
@Composable
fun GlassTabs(tabs: List<ToolTab>, selected: Int, onSelected: (Int) -> Unit) {
    val c = Glass.colors
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        tabs.forEachIndexed { index, tab ->
            val isOn = index == selected
            val done = index < selected
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val colour = when { isOn -> c.text; done -> c.tealHi; hovered -> c.text; else -> c.muted }
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
                if (done) Icon(Icons.Default.Check, contentDescription = null, tint = colour, modifier = Modifier.size(10.dp))
                Icon(tab.icon, contentDescription = null, tint = colour, modifier = Modifier.size(12.dp))
                Text(tab.title, style = MaterialTheme.typography.body2, color = colour, maxLines = 1)
            }
        }
    }
}
