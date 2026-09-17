package `in`.aicortex.iso8583studio.analytics

import io.github.frankois944.googleAnalyticsKMPTracker.core.Event
import io.github.frankois944.googleAnalyticsKMPTracker.core.Visitor
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the two reasons [Ga4Dispatcher] exists at all, both of which are silent failures:
 * a stray `debug_mode` keeps every event out of standard GA4 reports, and a missing
 * `session_id`/`engagement_time_msec` pair makes GA4 report zero users while still
 * returning 204.
 */
class Ga4DispatcherTest {

    private val geo = GeoProfile(
        publicIp = "203.0.113.7",
        city = "Bengaluru",
        regionId = "IN-KA",
        countryId = "IN",
    )

    private fun environment() = EnvironmentProfile.local().copy(geo = geo)

    private fun dispatcher(debug: Boolean = false) = Ga4Dispatcher(
        apiSecret = "test-secret",
        measurementId = "G-TEST",
        debugMode = debug,
        environment = { environment() },
    )

    private fun event(name: String = "screen_view") = Event(
        uuid = "11111111-1111-1111-1111-111111111111",
        dateCreatedInMs = 1_700_000_010_000,
        visitor = Visitor(clientId = "123.456", userId = "user-abc"),
        properties = """[{"name":"app_version","value":"1.0.14"}]""",
        screenResolutionWidth = 1920,
        screenResolutionHeight = 1080,
        lastEventTimeStampInMs = 1_700_000_000_000,
        sessionId = 1_700_000_000,
        measurementId = "G-TEST",
        language = "en-IN",
        eventName = name,
        adUserData = true,
        adPersonalization = false,
        params = """{"screen_name":"AesCalculator","nav_source":"root"}""",
    )

    @Test
    fun `omits debug_mode so events reach standard reports`() {
        val params = dispatcher().buildPayload(listOf(event()))["events"]!!
            .jsonArray[0].jsonObject["params"]!!.jsonObject

        assertFalse(
            params.containsKey("debug_mode"),
            "debug_mode routes events to DebugView only and excludes them from GA4 reports",
        )
    }

    @Test
    fun `emits debug_mode when explicitly validating`() {
        val body = dispatcher(debug = true).buildPayload(listOf(event()))
        val params = body["events"]!!.jsonArray[0].jsonObject["params"]!!.jsonObject

        assertTrue(params.containsKey("debug_mode"))
        assertEquals("ENFORCE_RECOMMENDATIONS", body["validation_behavior"]?.jsonPrimitive?.content)
    }

    @Test
    fun `carries the fields GA4 needs to attribute a user`() {
        val params = dispatcher().buildPayload(listOf(event()))["events"]!!
            .jsonArray[0].jsonObject["params"]!!.jsonObject

        assertEquals(1_700_000_000L, params["session_id"]!!.jsonPrimitive.content.toLong())
        assertEquals(10_000L, params["engagement_time_msec"]!!.jsonPrimitive.content.toLong())
    }

    @Test
    fun `sends resolved geo rather than the OS locale`() {
        val body = dispatcher().buildPayload(listOf(event()))

        assertEquals("203.0.113.7", body["ip_override"]?.jsonPrimitive?.content)
        val location = body["user_location"]!!.jsonObject
        assertEquals("Bengaluru", location["city"]?.jsonPrimitive?.content)
        // ISO-3166-2, not a bare country code - GA4 rejects the latter in region_id.
        assertEquals("IN-KA", location["region_id"]?.jsonPrimitive?.content)
        assertEquals("IN", location["country_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `carries identity, consent and event params`() {
        val body = dispatcher().buildPayload(listOf(event()))

        assertEquals("123.456", body["client_id"]?.jsonPrimitive?.content)
        assertEquals("user-abc", body["user_id"]?.jsonPrimitive?.content)

        val consent = body["consent"]!!.jsonObject
        assertEquals("GRANTED", consent["ad_user_data"]?.jsonPrimitive?.content)
        assertEquals("DENIED", consent["ad_personalization"]?.jsonPrimitive?.content)

        val params = body["events"]!!.jsonArray[0].jsonObject["params"]!!.jsonObject
        assertEquals("AesCalculator", params["screen_name"]?.jsonPrimitive?.content)

        val properties = body["user_properties"]!!.jsonObject
        assertEquals("1.0.14", properties["app_version"]!!.jsonObject["value"]?.jsonPrimitive?.content)
    }

    @Test
    fun `never leaks the public ip as a user property`() {
        val serialized = dispatcher().buildPayload(listOf(event()))["user_properties"].toString()
        assertFalse(
            serialized.contains("203.0.113.7"),
            "the IP may only appear as ip_override, never as a stored analytics attribute",
        )
    }
}
