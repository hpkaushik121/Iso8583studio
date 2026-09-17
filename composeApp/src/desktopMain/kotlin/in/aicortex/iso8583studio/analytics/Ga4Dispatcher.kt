package `in`.aicortex.iso8583studio.analytics

import io.github.frankois944.googleAnalyticsKMPTracker.core.Event
import io.github.frankois944.googleAnalyticsKMPTracker.dispatcher.Dispatcher
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Builds and POSTs the GA4 Measurement Protocol payload ourselves instead of using the
 * tracker library's built-in HTTP dispatcher.
 *
 * This exists because the library's own body builder (v0.0.1) has two defects that make it
 * unusable as shipped:
 *
 *  1. It hardcodes `debug_mode: true` on every event. GA4 routes debug traffic to DebugView
 *     and *excludes it from standard reports*, so data would never reach Realtime, Engagement
 *     or any report the user actually looks at.
 *  2. Its desktop `user_location` is derived from `Locale.getDefault().country` - the OS
 *     locale, not the machine's actual location - and it puts a bare country code in
 *     `region_id`, which expects ISO-3166-2. It also has no `ip_override`, so Google cannot
 *     resolve geo server-side the way it does for a browser hit.
 *
 * `Dispatcher` is a public extension point on `Tracker.create(customDispatcher = ...)` and
 * `Event` exposes every field we need, so this is a supported customisation, not a fork.
 *
 * `session_id` and `engagement_time_msec` are carried through exactly as the library computes
 * them - GA4 reports zero users without both, and there is no reason to reinvent them.
 */
class Ga4Dispatcher(
    override val baseURL: String = ENDPOINT,
    override val apiSecret: String,
    private val measurementId: String,
    /** Emits `debug_mode` and targets validation semantics. Only for local verification. */
    private val debugMode: Boolean = false,
    private val environment: () -> EnvironmentProfile?,
    private val onLog: (String) -> Unit = {},
) : Dispatcher {

    private val client: HttpClient by lazy { HttpClient(CIO) }

    override suspend fun sendSingleEvent(event: Event) = sendBulkEvent(listOf(event))

    override suspend fun sendBulkEvent(events: List<Event>) {
        if (events.isEmpty()) return
        // GA4 accepts at most 25 events per request.
        events.chunked(MAX_EVENTS_PER_REQUEST).forEach { chunk ->
            val body = buildPayload(chunk)
            val response = client.post(baseURL) {
                parameter("api_secret", apiSecret)
                parameter("measurement_id", measurementId)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(JsonObject.serializer(), body))
            }
            if (debugMode) {
                onLog("GA4 validation response: ${response.bodyAsText()}")
            }
        }
    }

    internal fun buildPayload(events: List<Event>): JsonObject {
        val first = events.first()
        val env = environment()
        return buildJsonObject {
            first.visitor?.let { visitor ->
                put("client_id", visitor.clientId)
                visitor.userId?.takeIf { it.isNotBlank() }?.let { put("user_id", it) }
            }
            env?.geo?.publicIp?.let { put("ip_override", it) }
            env?.geo?.let { geo ->
                if (geo.city != null || geo.regionId != null || geo.countryId != null) {
                    put("user_location", buildJsonObject {
                        geo.city?.let { put("city", it) }
                        geo.regionId?.let { put("region_id", it) }
                        geo.countryId?.let { put("country_id", it) }
                    })
                }
            }
            putUserProperties(first)
            putConsent(first)
            put("events", buildJsonArray { events.forEach { add(buildEvent(it)) } })
            if (debugMode) put("validation_behavior", "ENFORCE_RECOMMENDATIONS")
        }
    }

    private fun JsonObjectBuilder.putUserProperties(event: Event) {
        val parsed = runCatching { json.parseToJsonElement(event.properties).jsonArray }.getOrNull()
            ?: return
        if (parsed.isEmpty()) return
        put("user_properties", buildJsonObject {
            parsed.forEach { element ->
                val item = element.jsonObject
                val name = item["name"]?.jsonPrimitive?.content ?: return@forEach
                val value = item["value"] ?: return@forEach
                put(name, buildJsonObject { put("value", value) })
            }
        })
    }

    private fun JsonObjectBuilder.putConsent(event: Event) {
        val adUserData = event.adUserData
        val adPersonalization = event.adPersonalization
        if (adUserData == null && adPersonalization == null) return
        put("consent", buildJsonObject {
            adUserData?.let { put("ad_user_data", if (it) "GRANTED" else "DENIED") }
            adPersonalization?.let { put("ad_personalization", if (it) "GRANTED" else "DENIED") }
        })
    }

    private fun buildEvent(event: Event): JsonObject = buildJsonObject {
        put("name", event.eventName)
        put("timestamp_micros", event.dateCreatedInMs * 1_000)
        put("params", buildJsonObject {
            // Both are mandatory: without them GA4 accepts the hit but attributes no user,
            // and nothing shows up in Realtime.
            put("session_id", event.sessionId)
            put(
                "engagement_time_msec",
                (event.dateCreatedInMs - event.lastEventTimeStampInMs).coerceAtLeast(1L),
            )
            if (debugMode) put("debug_mode", JsonPrimitive(true))

            runCatching { json.parseToJsonElement(event.params).jsonObject }
                .getOrNull()
                ?.entries
                ?.take(MAX_PARAMS_PER_EVENT)
                ?.forEach { (key, value) -> put(key, value) }
        })
    }

    fun close() {
        runCatching { client.close() }
    }

    companion object {
        const val ENDPOINT = "https://www.google-analytics.com/mp/collect"

        /** Use with [debugMode] to validate payloads; returns `validationMessages`. */
        const val DEBUG_ENDPOINT = "https://www.google-analytics.com/debug/mp/collect"

        private const val MAX_EVENTS_PER_REQUEST = 25
        private const val MAX_PARAMS_PER_EVENT = 25

        private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true; explicitNulls = false }
    }
}
