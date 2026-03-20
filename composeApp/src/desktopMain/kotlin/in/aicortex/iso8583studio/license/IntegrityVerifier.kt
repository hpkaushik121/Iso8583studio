package `in`.aicortex.iso8583studio.license

import java.security.MessageDigest

/**
 * Bytecode integrity verification.
 *
 * At runtime, loads critical class files via getResourceAsStream(),
 * computes their SHA-256 hashes, and compares against expected values.
 * If a mismatch is detected, the binary has been tampered with.
 *
 * Expected hashes are computed at build time and embedded here.
 * For dev builds, verification is skipped (hashes are placeholders).
 */
object IntegrityVerifier {

    @Volatile
    private var integrityOk = true

    val isIntact: Boolean get() = integrityOk

    private val criticalClasses = listOf(
        "in/aicortex/iso8583studio/license/LicenseService.class",
        "in/aicortex/iso8583studio/license/LicenseStorage.class",
        "in/aicortex/iso8583studio/license/LicenseState.class",
        "in/aicortex/iso8583studio/license/LicenseServerClient.class",
        "in/aicortex/iso8583studio/license/AntiTamper.class",
        "in/aicortex/iso8583studio/license/EmbeddedCaCert.class",
        "in/aicortex/iso8583studio/license/MachineFingerprint.class",
    )

    /**
     * Expected SHA-256 hashes of critical class files.
     * PLACEHOLDER values disable verification (dev mode).
     * Replace with actual hashes computed during release build.
     */
    private val expectedHashes = mapOf(
        "in/aicortex/iso8583studio/license/LicenseService.class" to "PLACEHOLDER",
        "in/aicortex/iso8583studio/license/LicenseStorage.class" to "PLACEHOLDER",
        "in/aicortex/iso8583studio/license/LicenseState.class" to "PLACEHOLDER",
        "in/aicortex/iso8583studio/license/LicenseServerClient.class" to "PLACEHOLDER",
        "in/aicortex/iso8583studio/license/AntiTamper.class" to "PLACEHOLDER",
        "in/aicortex/iso8583studio/license/EmbeddedCaCert.class" to "PLACEHOLDER",
        "in/aicortex/iso8583studio/license/MachineFingerprint.class" to "PLACEHOLDER",
    )

    private val isDevMode = expectedHashes.values.all { it == "PLACEHOLDER" }

    fun verifyClasses() {
        if (isDevMode) return
        for (classPath in criticalClasses) {
            val expected = expectedHashes[classPath] ?: continue
            if (expected == "PLACEHOLDER") continue
            val actual = computeClassHash(classPath)
            if (actual != expected) {
                integrityOk = false
                return
            }
        }
    }

    fun recheck() {
        integrityOk = true
        verifyClasses()
    }

    private fun computeClassHash(resourcePath: String): String? {
        return try {
            val classLoader = IntegrityVerifier::class.java.classLoader
            val stream = classLoader.getResourceAsStream(resourcePath) ?: return null
            val bytes = stream.use { it.readBytes() }
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(bytes)
            hash.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }
}
