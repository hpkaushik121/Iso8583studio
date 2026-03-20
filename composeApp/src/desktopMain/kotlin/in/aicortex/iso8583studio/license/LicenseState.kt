package `in`.aicortex.iso8583studio.license

/**
 * Represents the current license validation state.
 * Each state carries an opaque cross-check token derived from the validation
 * path that produced it. Distributed verifiers compare tokens to detect
 * state injection via reflection.
 */
enum class LicenseState(val blocking: Boolean) {
    VALID(false),
    TRIAL(false),
    EXPIRING_SOON(false),
    EXPIRED(true),
    INVALID(true),
    NOT_FOUND(true),
    REVOKED(true),
    OFFLINE_GRACE_EXCEEDED(true),
    ACTIVATION_REQUIRED(true),
    TAMPER_DETECTED(true);

    fun isUsable(): Boolean = !blocking
}

/**
 * Immutable snapshot of the license state at a point in time.
 * The [crossCheckToken] is a HMAC derived from the validation code path —
 * secondary verifiers recompute it independently and compare.
 */
data class LicenseSnapshot(
    val state: LicenseState,
    val daysUntilExpiry: Int = -1,
    val message: String = "",
    val crossCheckToken: Long = 0L,
    val validatedAtMillis: Long = System.currentTimeMillis()
) {
    fun isUsable(): Boolean = state.isUsable()
}
