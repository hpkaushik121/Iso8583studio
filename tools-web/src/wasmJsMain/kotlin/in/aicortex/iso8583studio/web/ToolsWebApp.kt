package `in`.aicortex.iso8583studio.web

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.analytics.Analytics
import `in`.aicortex.iso8583studio.platform.epochMillis
import `in`.aicortex.iso8583studio.tools.ToolCatalog
import `in`.aicortex.iso8583studio.tools.ToolCategory
import `in`.aicortex.iso8583studio.tools.WebTool
import `in`.aicortex.iso8583studio.ui.WarningYellow
import androidx.compose.runtime.CompositionLocalProvider
import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome
import `in`.aicortex.iso8583studio.web.chrome.GlassChrome
import `in`.aicortex.iso8583studio.web.theme.GlassTheme
import `in`.aicortex.iso8583studio.web.theme.rememberGlassFonts
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * The browser shell: a home grid of the web-ready tools and a hash router (`#/<tool-id>`) so
 * the site's documentation pages can deep-link straight into a calculator.
 */
@Composable
fun ToolsWebApp() {
    var route by remember { mutableStateOf(WebBridge.currentHash()) }

    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { route = WebBridge.currentHash() }
        window.addEventListener("hashchange", listener)
        onDispose { window.removeEventListener("hashchange", listener) }
    }

    fun navigate(toolId: String?) {
        window.location.hash = if (toolId == null) "/" else "/$toolId"
    }

    val tool = ToolCatalog.byId(route)
    LaunchedEffect(tool?.id) { WebBridge.postRoute(tool?.id) }

    // Nothing draws until the bundled fonts are in, so no text ever appears in the fallback face;
    // the page's DOM loader stays up until app_ready.
    val fonts = rememberGlassFonts() ?: return
    LaunchedEffect(Unit) { WebBridge.postReady() }

    GlassTheme(fonts) {
        CompositionLocalProvider(LocalToolChrome provides GlassChrome) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                if (tool == null) {
                    HomeGrid(onOpen = { navigate(it.id) })
                } else {
                    ToolHost(tool, onBack = { navigate(null) })
                }
            }
        }
    }
}

@Composable
private fun ToolHost(tool: WebTool, onBack: () -> Unit) {
    val openedAt = remember(tool.id) { epochMillis() }
    DisposableEffect(tool.id) {
        Analytics.toolOpened(tool.id, tool.label, tool.category.name)
        onDispose { Analytics.toolClosed(tool.id, (epochMillis() - openedAt) / 1000) }
    }
    // The chrome draws the frame, breadcrumb, badge and the reference-implementation banner from this.
    CompositionLocalProvider(LocalToolContext provides tool) {
        Box(modifier = Modifier.fillMaxSize()) { tool.content(onBack) }
    }
}

@Composable
private fun HomeGrid(onOpen: (WebTool) -> Unit) {
    val groups = ToolCategory.entries
        .map { it to ToolCatalog.byCategory(it).filter { t -> t.webReady } }
        .filter { it.second.isNotEmpty() }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("ISO8583Studio Tools", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Payment cryptography calculators running entirely in your browser. Keys and card data never leave this page.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        groups.forEach { (category, tools) ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(category.label, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                    Text(category.description, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
            items(tools, key = { it.id }) { tool -> ToolCard(tool, onOpen) }
        }
    }
}

@Composable
private fun ToolCard(tool: WebTool, onOpen: (WebTool) -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable { onOpen(tool) }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(tool.icon, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(tool.label, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.SemiBold)
                Text(
                    tool.description,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                    maxLines = 2
                )
            }
        }
    }
}
