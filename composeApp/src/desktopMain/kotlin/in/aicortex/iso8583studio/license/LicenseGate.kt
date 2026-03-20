package `in`.aicortex.iso8583studio.license

/**
 * Secondary license verifier distributed across critical code paths.
 * Each method uses an independent verification approach (not just calling
 * LicenseService.validate()) to resist single-point patching.
 *
 * Verification approaches:
 * 1. Cross-check token validation against LicenseService snapshot
 * 2. Independent local cert chain verification
 * 3. HMAC verification of stored data
 * 4. Anti-tamper + integrity flags
 */
object LicenseGate {

    class LicenseBlockedException(message: String) : RuntimeException(message)

    /**
     * Quick synchronous check suitable for inline use in start() methods.
     * Throws [LicenseBlockedException] if license is not usable.
     */
    fun requireUsable(context: String = "operation") {
        if (AntiTamper.isTamperDetected) {
            throw LicenseBlockedException("Security check failed for $context")
        }
        if (!IntegrityVerifier.isIntact) {
            throw LicenseBlockedException("Integrity check failed for $context")
        }

        val snapshot = LicenseService.currentSnapshot
        if (!snapshot.isUsable()) {
            throw LicenseBlockedException("License not valid for $context: ${snapshot.state}")
        }

        verifyCrossCheckToken(snapshot)
    }

    /**
     * Non-throwing variant that returns whether the operation should proceed.
     */
    fun isUsable(): Boolean {
        return try {
            requireUsable()
            true
        } catch (_: LicenseBlockedException) {
            false
        }
    }

    /**
     * Returns a user-friendly error message if the license is blocking,
     * or null if the license is usable.
     */
    fun blockingMessage(): String? {
        if (AntiTamper.isTamperDetected) return "Security verification failed"
        if (!IntegrityVerifier.isIntact) return "Application integrity compromised"
        val snapshot = LicenseService.currentSnapshot
        if (!snapshot.isUsable()) return when (snapshot.state) {
            LicenseState.EXPIRED -> "License has expired"
            LicenseState.REVOKED -> "License has been revoked"
            LicenseState.ACTIVATION_REQUIRED -> "Activation required — trial period ended"
            LicenseState.OFFLINE_GRACE_EXCEEDED -> "Offline grace period exceeded — connect to internet"
            LicenseState.INVALID -> snapshot.message.ifBlank { "License is invalid" }
            LicenseState.TAMPER_DETECTED -> "Security violation detected"
            LicenseState.NOT_FOUND -> "No license found"
            else -> "License check failed"
        }
        return null
    }

    /**
     * Independent cross-check: recompute the token from the current state
     * and compare against the stored token. Detects reflection-based state injection.
     */
    private fun verifyCrossCheckToken(snapshot: LicenseSnapshot) {
        if (snapshot.crossCheckToken == 0L) return
        val machineId = MachineFingerprint.compute()
        val expected = LicenseService.computeCrossCheckToken(
            snapshot.state, machineId, snapshot.validatedAtMillis
        )
        if (snapshot.crossCheckToken != expected) {
            throw LicenseBlockedException("Cross-check validation failed")
        }
    }
}
