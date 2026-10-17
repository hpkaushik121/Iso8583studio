package `in`.aicortex.iso8583studio.web.chrome

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import `in`.aicortex.iso8583studio.ui.chrome.DesktopChrome
import `in`.aicortex.iso8583studio.ui.chrome.ToolChrome
import `in`.aicortex.iso8583studio.ui.chrome.ToolLog
import `in`.aicortex.iso8583studio.ui.chrome.ToolScreenSpec
import `in`.aicortex.iso8583studio.ui.chrome.ToolTab

/** The browser chrome: the site's glass panels. Pieces not yet replaced fall back to the desktop look. */
object GlassChrome : ToolChrome by DesktopChrome {
    override val isDesktop: Boolean get() = false

    @Composable
    override fun Screen(spec: ToolScreenSpec, content: @Composable () -> Unit) = GlassScreen(spec, content)

    @Composable
    override fun Tabs(tabs: List<ToolTab>, selected: Int, onSelected: (Int) -> Unit) = GlassTabs(tabs, selected, onSelected)

    @Composable
    override fun LogPanel(log: ToolLog, modifier: Modifier) = GlassLogPanel(log, modifier)
}
