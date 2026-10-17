package `in`.aicortex.iso8583studio.web.chrome

import `in`.aicortex.iso8583studio.ui.chrome.DesktopChrome
import `in`.aicortex.iso8583studio.ui.chrome.ToolChrome

/** The browser chrome. Starts as the desktop look behind the web theme; the glass parts replace it piece by piece. */
object GlassChrome : ToolChrome by DesktopChrome {
    override val isDesktop: Boolean get() = false
}
