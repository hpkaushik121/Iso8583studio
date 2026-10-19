package `in`.aicortex.iso8583studio.web

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.analytics.Analytics
import `in`.aicortex.iso8583studio.platform.epochMillis
import `in`.aicortex.iso8583studio.tools.ToolCatalog
import `in`.aicortex.iso8583studio.tools.ToolCategory
import `in`.aicortex.iso8583studio.tools.WebTool
import `in`.aicortex.iso8583studio.ui.chrome.LocalToolChrome
import `in`.aicortex.iso8583studio.web.chrome.GlassChrome
import `in`.aicortex.iso8583studio.web.home.CategoryTree
import `in`.aicortex.iso8583studio.web.home.TREE_WIDTH
import `in`.aicortex.iso8583studio.web.home.ToolHub
import `in`.aicortex.iso8583studio.web.theme.Glass
import `in`.aicortex.iso8583studio.web.theme.GlassTheme
import `in`.aicortex.iso8583studio.web.theme.rememberGlassFonts
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * The browser shell: a hash router (`#/`, `#/c/<category-slug>`, `#/<tool-id>`) so the site's
 * pages can deep-link straight into a calculator, with the category tree beside both the hub and
 * an open tool from 900dp and reachable through a drawer below that.
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
                Shell(route, tool, category, ::navigate)
            }
        }
    }
}

@Composable
private fun Shell(route: String, tool: WebTool?, category: ToolCategory?, navigate: (String?) -> Unit) {
    val c = Glass.colors
    val routeCategory = tool?.category ?: category
    var expanded by remember { mutableStateOf(setOfNotNull(routeCategory)) }
    var drawer by remember { mutableStateOf(false) }
    LaunchedEffect(route) { routeCategory?.let { expanded = expanded + it }; drawer = false }
    // The app's own brand row only when the site nav is not above it: full screen, or opened standalone.
    val brand = WebBridge.fullscreen || WebBridge.standalone

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(c.canvas)) {
        val wide = maxWidth >= 900.dp
        val drawerWidth = minOf(300.dp, maxWidth * 0.86f)
        LaunchedEffect(wide) { if (wide) drawer = false }
        val tree: @Composable (onClose: (() -> Unit)?, Modifier) -> Unit = { onClose, m ->
            CategoryTree(
                route = route, expanded = expanded, onToggle = { cat -> expanded = if (cat in expanded) expanded - cat else expanded + cat },
                onCategory = { navigate("c/${it.slug}") }, onOpen = { navigate(it.id) }, brand = brand, onClose = onClose, modifier = m,
            )
        }
        Row(modifier = Modifier.fillMaxSize()) {
            if (wide) {
                tree(null, Modifier)
                Box(Modifier.width(1.dp).fillMaxHeight().background(c.line2))
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                CompositionLocalProvider(LocalBrowseTools provides if (wide) null else ({ drawer = true })) {
                    if (tool == null) {
                        ToolHub(category = category, onCategory = { navigate(it?.let { c -> "c/${c.slug}" }) }, onOpen = { navigate(it.id) }, wide = wide)
                    } else {
                        ToolHost(tool, onBack = { navigate(null) })
                    }
                }
            }
        }
        if (drawer && !wide) {
            Row(modifier = Modifier.fillMaxSize()) {
                tree({ drawer = false }, Modifier.width(drawerWidth))
                Box(Modifier.weight(1f).fillMaxHeight().background(c.overlay).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { drawer = false })
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
    // The chrome draws the title row, badge and the reference-implementation banner from this.
    CompositionLocalProvider(LocalToolContext provides tool) {
        Box(modifier = Modifier.fillMaxSize()) { tool.content(onBack) }
    }
}
