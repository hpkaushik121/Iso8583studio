package `in`.aicortex.iso8583studio.license

/**
 * Build configuration — detects debug vs release at runtime.
 *
 * Detection: packaged distributions (composeApp:package*) set
 * -Dlicense.release=true via nativeDistributions.jvmArgs.
 * When running via composeApp:run, that property is absent
 * so we fall back to checking if the code source is inside
 * a .app / .exe / installed path (packaged) vs build/classes (dev).
 *
 * Debug  (composeApp:run):  localhost:8080, IS_RELEASE=false
 * Release (composeApp:package*): production URL, IS_RELEASE=true
 */
object BuildConfig {

    val IS_RELEASE: Boolean = detectRelease()

    val LICENSE_SERVER_URL: String =
        System.getProperty("license.server.url")
            ?: if (IS_RELEASE) "https://license.iso8583.studio" else "http://localhost:8080"

    private fun detectRelease(): Boolean {
        val explicit = System.getProperty("license.release")
        if (explicit != null) return explicit.toBoolean()

        val location = BuildConfig::class.java.protectionDomain?.codeSource?.location?.path ?: ""
        return location.contains(".app/") ||
                location.contains("\\Program Files") ||
                location.contains("/opt/") ||
                location.contains("/usr/")
    }
}
