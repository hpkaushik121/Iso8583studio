package `in`.aicortex.iso8583studio.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import `in`.aicortex.iso8583studio.analytics.AnalyticsSink
import `in`.aicortex.iso8583studio.tools.ToolCatalog
import kotlinx.browser.window

/**
 * Messages to the page that embeds the app (the /tools/app page of the site). The wasm app
 * never loads analytics itself: the parent owns consent, the GA4 stream and the URL bar.
 * Only same-origin parents receive anything.
 */
@Suppress("UNUSED_PARAMETER")
private fun postToParent(json: String): Unit =
    js("{ try { if (window.parent && window.parent !== window) { window.parent.postMessage(JSON.parse(json), window.location.origin); } } catch (e) {} }")

@Suppress("UNUSED_PARAMETER")
private fun postToSelf(json: String): Unit =
    js("{ try { var l = document.getElementById('loader'); if (l) l.hidden = true; window.postMessage(JSON.parse(json), window.location.origin); } catch (e) {} }")

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

    /** True while the embedding page shows the frame full screen; the app then carries its own brand row. */
    var fullscreen: Boolean by mutableStateOf(false)
        private set

    /** True when the app is the top document (opened at /app/ directly) rather than embedded in the site. */
    val standalone: Boolean = isTopWindow()

    /** Posted to the embedding page and to this window itself, so the page's own loader can hide when the app runs standalone. */
    fun postReady() {
        post("app_ready", mapOf("tools" to ToolCatalog.webReady.size))
        postToSelf("{\"source\":\"$SOURCE\",\"type\":\"app_ready\"}")
    }

    fun postRoute(toolId: String?) = post("route", mapOf("tool" to (toolId ?: "")))

    /** Marks the wheel event being dispatched as unused by the app; the page's listener then scrolls the page by it. */
    fun flagUnusedWheel() = flagUnusedWheelJs()

    /** Per-browser preferences (localStorage; absent or blocked storage reads as null). */
    fun readPref(key: String): String? = readPrefJs(key)
    fun writePref(key: String, value: String) = writePrefJs(key, value)

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
            when (fullscreenFrom(event)) { 1 -> fullscreen = true; 0 -> fullscreen = false }
        })
    }
}

@Suppress("UNUSED_PARAMETER")
private fun readPrefJs(key: String): String? = js("(function(){ try { return localStorage.getItem(key); } catch (e) { return null; } })()")

@Suppress("UNUSED_PARAMETER")
private fun writePrefJs(key: String, value: String): Unit = js("{ try { localStorage.setItem(key, value); } catch (e) {} }")

private fun flagUnusedWheelJs(): Unit = js("{ window.__iso8583UnusedWheel = true; }")

private fun isTopWindow(): Boolean = js("(function(){ try { return window.parent === window; } catch (e) { return true; } })()")

/** 1 or 0 for a parent `{type:'fullscreen', on}` message, -1 when the event is not one. */
@Suppress("UNUSED_PARAMETER")
private fun fullscreenFrom(event: JsAny?): Int =
    js("(function(){ try { var e = event; if (!e || e.origin !== window.location.origin) return -1; var d = e.data; if (!d || d.source !== 'iso8583-site' || d.type !== 'fullscreen') return -1; return d.on ? 1 : 0; } catch (x) { return -1; } })()")

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
