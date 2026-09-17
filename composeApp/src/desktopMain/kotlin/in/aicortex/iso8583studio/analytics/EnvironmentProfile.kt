package `in`.aicortex.iso8583studio.analytics

import `in`.aicortex.iso8583studio.StudioVersion
import `in`.aicortex.iso8583studio.data.model.AppSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.GraphicsEnvironment
import java.time.ZoneId
import java.util.Locale

/**
 * Approximate, city-level location for a GA4 `user_location` block.
 *
 * [publicIp] is used solely as the `ip_override` field so Google can resolve geo the same
 * way it does for a browser hit. It is never sent as an event parameter or user property:
 * uploading IP addresses as analytics attributes violates the Google Analytics terms.
 * Latitude/longitude are deliberately not modelled here - GA4 cannot store them.
 */
data class GeoProfile(
    val publicIp: String? = null,
    val city: String? = null,
    /** ISO-3166-2, e.g. "US-CA". GA4 rejects a bare country code in this field. */
    val regionId: String? = null,
    /** ISO-3166-1 alpha-2, e.g. "US". */
    val countryId: String? = null,
)

/**
 * Machine and environment facts reported as GA4 user properties, plus the resolved [geo].
 *
 * Built once per launch, after consent has been granted - [resolve] performs a network call
 * and must never run for a user who has declined or not yet been asked.
 */
data class EnvironmentProfile(
    val appVersion: String,
    val osName: String,
    val osVersion: String,
    val osArch: String,
    val jvmVersion: String,
    val locale: String,
    val timezone: String,
    val screenResolution: String,
    val cpuCores: Int,
    val maxHeapMb: Long,
    val daysSinceInstall: Long,
    val launchCount: Int,
    val geo: GeoProfile = GeoProfile(),
) {
    /**
     * GA4 user properties. Keys are <= 24 chars and values are truncated to 36 by the caller,
     * per the Measurement Protocol limits.
     */
    fun asUserProperties(): Map<String, String> = buildMap {
        // The website reports into the same GA4 property; this keeps the two separable.
        put("stream_source", "desktop_app")
        put("app_version", appVersion)
        put("os_name", osName)
        put("os_version", osVersion)
        put("os_arch", osArch)
        put("jvm_version", jvmVersion)
        put("locale", locale)
        put("timezone", timezone)
        put("screen_resolution", screenResolution)
        put("cpu_cores", cpuCores.toString())
        put("max_heap_mb", maxHeapMb.toString())
        put("days_since_install", daysSinceInstall.toString())
        put("launch_count", launchCount.toString())
        geo.city?.let { put("geo_city", it) }
        geo.regionId?.let { put("geo_region", it) }
        geo.countryId?.let { put("geo_country", it) }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Everything obtainable without touching the network. */
        fun local(): EnvironmentProfile {
            val screen = runCatching {
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .defaultScreenDevice.displayMode
                    .let { "${it.width}x${it.height}" }
            }.getOrDefault("unknown")

            return EnvironmentProfile(
                appVersion = StudioVersion,
                osName = System.getProperty("os.name") ?: "unknown",
                osVersion = System.getProperty("os.version") ?: "unknown",
                osArch = System.getProperty("os.arch") ?: "unknown",
                jvmVersion = System.getProperty("java.version") ?: "unknown",
                locale = Locale.getDefault().toLanguageTag(),
                timezone = runCatching { ZoneId.systemDefault().id }.getOrDefault("unknown"),
                screenResolution = screen,
                cpuCores = Runtime.getRuntime().availableProcessors(),
                maxHeapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024),
                daysSinceInstall = AppSettings.daysSinceInstall(),
                launchCount = AppSettings.launchCount,
            )
        }

        /**
         * [local] plus a best-effort IP geolocation lookup. Failure is non-fatal and simply
         * leaves [GeoProfile] empty; analytics must never delay or break startup.
         */
        suspend fun resolve(): EnvironmentProfile {
            val base = local()
            val geo = withTimeoutOrNull(GEO_TIMEOUT_MS) { lookupGeo() } ?: GeoProfile()
            return base.copy(geo = geo)
        }

        private const val GEO_TIMEOUT_MS = 3_000L

        private suspend fun lookupGeo(): GeoProfile? {
            val client = runCatching { HttpClient(CIO) }.getOrNull() ?: return null
            return try {
                parseIpapi(client.get("https://ipapi.co/json/").bodyAsText())
                    ?: parseIpinfo(client.get("https://ipinfo.io/json").bodyAsText())
            } catch (_: Throwable) {
                null
            } finally {
                runCatching { client.close() }
            }
        }

        private fun str(text: String, key: String): String? =
            runCatching {
                json.parseToJsonElement(text).jsonObject[key]?.jsonPrimitive?.content
                    ?.takeIf { it.isNotBlank() && it != "null" }
            }.getOrNull()

        /** ipapi.co: { "ip", "city", "region_code", "country_code", "latitude", "longitude" } */
        private fun parseIpapi(text: String): GeoProfile? {
            val country = str(text, "country_code") ?: return null
            val region = str(text, "region_code")
            return GeoProfile(
                publicIp = str(text, "ip"),
                city = str(text, "city"),
                regionId = region?.let { "$country-$it" },
                countryId = country,
            )
        }

        /** ipinfo.io: { "ip", "city", "region", "country", "loc" } - `region` is a full name. */
        private fun parseIpinfo(text: String): GeoProfile? {
            val country = str(text, "country") ?: return null
            return GeoProfile(
                publicIp = str(text, "ip"),
                city = str(text, "city"),
                // ipinfo returns a region name, not an ISO-3166-2 subdivision code, so it is
                // omitted rather than sent in a format GA4 would reject.
                regionId = null,
                countryId = country,
            )
        }
    }
}
