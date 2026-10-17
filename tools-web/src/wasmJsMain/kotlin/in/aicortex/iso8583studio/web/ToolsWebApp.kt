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
import `in`.aicortex.iso8583studio.web.home.ToolHub
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

    fun navigate(target: String?) {
        window.location.hash = if (target == null) "/" else "/$target"
    }

    val tool = ToolCatalog.byId(route)
    val category = if (route.startsWith("c/")) ToolCategory.entries.firstOrNull { it.slug == route.removePrefix("c/") } else null
    LaunchedEffect(route) { WebBridge.postRoute(tool?.id ?: route.takeIf { category != null }) }

    // Nothing draws until the bundled fonts are in, so no text ever appears in the fallback face;
    // the page's DOM loader stays up until app_ready.
    val fonts = rememberGlassFonts() ?: return
    LaunchedEffect(Unit) { WebBridge.postReady() }

    GlassTheme(fonts) {
        CompositionLocalProvider(LocalToolChrome provides GlassChrome) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                if (tool == null) {
                    ToolHub(category = category, onCategory = { navigate(it?.let { c -> "c/${c.slug}" }) }, onOpen = { navigate(it.id) })
                } else {
                    ToolHost(tool, onBack = { navigate(if (category != null) "c/${category.slug}" else null) })
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
