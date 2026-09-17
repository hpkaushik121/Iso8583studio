package `in`.aicortex.iso8583studio.analytics

/**
 * Supplies a platform advertising identifier, when the platform has one.
 *
 * Google's Advertising ID (GAID/AAID) is exposed by `AdvertisingIdClient` in
 * `com.google.android.gms.ads.identifier`, which ships with Google Play Services and exists
 * only on Android. This project targets desktop JVM, so [DesktopAdvertisingIdProvider]
 * returns null and [Analytics] falls back to the durable random device id.
 *
 * The seam is kept because the tracker library is Kotlin Multiplatform with a working
 * `androidMain`: if an Android target is added later, supplying a provider that calls
 * `AdvertisingIdClient.getAdvertisingIdInfo(context).id` upgrades identity everywhere with
 * no change to any call site.
 */
interface AdvertisingIdProvider {
    /** The advertising id, or null when the platform does not provide one. */
    suspend fun advertisingId(): String?
}

/** Desktop JVM has no advertising id. */
object DesktopAdvertisingIdProvider : AdvertisingIdProvider {
    override suspend fun advertisingId(): String? = null
}
