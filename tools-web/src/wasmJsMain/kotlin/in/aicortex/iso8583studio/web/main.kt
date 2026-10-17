package `in`.aicortex.iso8583studio.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import `in`.aicortex.iso8583studio.analytics.Analytics
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Analytics.sink = WebAnalyticsSink
    // The embedding page drives deep links and the browser's back button through `navigate`
    // messages; listen before composing so nothing sent in reply to app_ready is missed.
    WebBridge.listenForParentNavigation()
    ComposeViewport(document.body!!) {
        ToolsWebApp()
    }
    WebBridge.postReady()
}
