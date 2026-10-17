package `in`.aicortex.iso8583studio.web

import `in`.aicortex.iso8583studio.analytics.AnalyticsSink
import kotlinx.browser.window

/**
 * Messages to the page that embeds the app (the /tools/app page of the site). The wasm app
 * never loads analytics itself: the parent owns consent, the GA4 stream and the URL bar.
 * Only same-origin parents receive anything.
 */
@Suppress("UNUSED_PARAMETER")
private fun postToParent(json: String): Unit =
    js("{ try { if (window.parent && window.parent !== window) { window.parent.postMessage(JSON.parse(json), window.location.origin); } } catch (e) {} }")

object WebBridge {
    private const val SOURCE = "iso8583-tools"

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")

    fun post(type: String, fields: Map<String, Any?> = emptyMap()) {
        val body = buildString {
            append("{\"source\":\"").append(SOURCE).append("\",\"type\":\"").append(escape(type)).append('"')
            fields.forEach { (k, v) ->
                append(",\"").append(escape(k)).append("\":")
                when (v) {
                    null -> append("null")
                    is Boolean, is Int, is Long, is Double -> append(v.toString())
                    else -> append('"').append(escape(v.toString())).append('"')
                }
            }
            append('}')
        }
        postToParent(body)
    }

    fun postReady() = post("app_ready")

    fun postRoute(toolId: String?) = post("route", mapOf("tool" to (toolId ?: "")))

    /** The current hash without the leading "#/" (or "#"). */
    fun currentHash(): String = window.location.hash.removePrefix("#").removePrefix("/")

    /**
     * `{source:'iso8583-site', type:'navigate', tool}` from the same-origin parent sets the
     * route; the hash change then flows through the app's normal router.
     */
    fun listenForParentNavigation() {
        window.addEventListener("message", { event ->
            val tool = navigateTargetFrom(event)
            if (tool != null) {
                val target = if (tool.isEmpty()) "/" else "/$tool"
                if (currentHash() != tool) window.location.hash = target
            }
        })
    }
}

/** Returns the tool slug of a parent `navigate` message, "" for home, or null when the event is not one. */
@Suppress("UNUSED_PARAMETER")
private fun navigateTargetFrom(event: JsAny?): String? =
    js("(function(){ try { var e = event; if (!e || e.origin !== window.location.origin) return null; var d = e.data; if (!d || d.source !== 'iso8583-site' || d.type !== 'navigate') return null; return String(d.tool || ''); } catch (x) { return null; } })()")

/** Forwards the shared screens' events to the parent page, which applies the site's consent rules. */
object WebAnalyticsSink : AnalyticsSink {
    override fun calculationRun(operation: String, success: Boolean, durationMs: Long) =
        WebBridge.post("calculation_run", mapOf("operation" to operation, "success" to success, "duration_ms" to durationMs))

    override fun toolOpened(toolId: String, label: String, category: String) =
        WebBridge.post("tool_open", mapOf("tool_id" to toolId, "tool_name" to label, "tool_category" to category))

    override fun toolClosed(toolId: String, durationSeconds: Long) =
        WebBridge.post("tool_close", mapOf("tool_id" to toolId, "duration_seconds" to durationSeconds))

    override fun error(throwable: Throwable) =
        WebBridge.post("app_error", mapOf("error_type" to (throwable::class.simpleName ?: "Unknown")))
}
