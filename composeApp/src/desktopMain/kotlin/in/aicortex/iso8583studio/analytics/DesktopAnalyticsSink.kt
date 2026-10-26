package `in`.aicortex.iso8583studio.analytics

import `in`.aicortex.iso8583studio.data.model.StudioTool

/**
 * Routes the shared tool screens' events into the desktop GA4 tracker. Tool open/close are
 * already reported by the session manager with the full [StudioTool], so only calculation
 * runs and errors are forwarded here; the rest would double count.
 */
object DesktopAnalyticsSink : AnalyticsSink {
    override fun calculationRun(operation: String, success: Boolean, durationMs: Long) =
        StudioAnalytics.calculationRun(operation, success, durationMs)

    override fun toolOpened(toolId: String, label: String, category: String) = Unit

    override fun toolClosed(toolId: String, durationSeconds: Long) = Unit

    override fun error(throwable: Throwable) = StudioAnalytics.error(throwable)
}
