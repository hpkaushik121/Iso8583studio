package `in`.aicortex.iso8583studio.license

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Date

/**
 * Central license validation orchestrator.
 *
 * Validation pipeline:
 * 1. Anti-tamper + integrity checks (delegated to AntiTamper / IntegrityVerifier)
 * 2. Load encrypted local state
 * 3. If certificate exists -> local cert chain check -> online validation
 * 4. If no certificate -> trial logic (online registration required)
 * 5. Offline grace handling via cached signed server response
 *
 * Cross-check tokens: each validation path computes a token from
 * (state + machineId + timestamp). Distributed verifiers recompute
 * independently and compare to detect reflection/memory injection.
 */
object LicenseService {

    private const val TRIAL_DAYS = 7
    private const val OFFLINE_GRACE_DAYS = 7
    private const val EXPIRY_WARNING_DAYS = 30
    private const val MILLIS_PER_DAY = 86_400_000L

    private val _state = MutableStateFlow(
        LicenseSnapshot(LicenseState.NOT_FOUND)
    )
    val state: StateFlow<LicenseSnapshot> = _state.asStateFlow()

    val currentSnapshot: LicenseSnapshot get() = _state.value

    /**
     * Full validation pipeline — called at startup and periodically.
     * Returns the computed snapshot. Thread-safe via StateFlow.
     */
    suspend fun validate(): LicenseSnapshot {
        val snapshot = try {
            doValidate()
        } catch (_: Exception) {
            LicenseSnapshot(LicenseState.INVALID, message = "Validation error")
        }
        _state.value = snapshot
        return snapshot
    }

    /**
     * Quick local-only check (no network). Used by inline secondary verifiers
     * that run synchronously inside simulator start paths.
     */
    fun quickCheck(): LicenseSnapshot {
        return try {
            doQuickLocalCheck()
        } catch (_: Exception) {
            LicenseSnapshot(LicenseState.INVALID, message = "Quick check error")
        }
    }

    private suspend fun doValidate(): LicenseSnapshot {
        LicenseStorage.ensureDir()
        val stored = LicenseStorage.load()
        val machineId = MachineFingerprint.compute()
        val now = System.currentTimeMillis()

        if (LicenseStorage.hasCertificate()) {
            return validateWithCertificate(stored, machineId, now)
        }

        return validateTrial(stored, machineId, now)
    }

    private suspend fun validateWithCertificate(
        stored: LicenseStorage.StoredData?,
        machineId: String,
        now: Long
    ): LicenseSnapshot {
        val certPem = LicenseStorage.loadCertificatePem()
            ?: return LicenseSnapshot(LicenseState.NOT_FOUND, message = "Certificate file missing")

        val localResult = verifyCertLocally(certPem)
        if (localResult != null) return localResult

        val onlineResult = tryOnlineValidation(certPem, machineId, now, stored)
        if (onlineResult != null) return onlineResult

        return handleOfflineGrace(stored, now)
    }

    private fun verifyCertLocally(certPem: String): LicenseSnapshot? {
        return try {
            val cert = parseCertificate(certPem)
            val now = Date()
            if (now.after(cert.notAfter)) {
                return LicenseSnapshot(LicenseState.EXPIRED, message = "Certificate has expired")
            }
            if (now.before(cert.notBefore)) {
                return LicenseSnapshot(LicenseState.INVALID, message = "Certificate not yet valid")
            }
            verifyCertChain(cert)
            null
        } catch (_: Exception) {
            LicenseSnapshot(LicenseState.INVALID, message = "Certificate verification failed")
        }
    }

    private fun verifyCertChain(cert: X509Certificate) {
        val caPem = EmbeddedCaCert.reconstruct()
        if (caPem.contains("PLACEHOLDER")) return
        val caCert = parseCertificate(caPem)
        cert.verify(caCert.publicKey)
    }

    private suspend fun tryOnlineValidation(
        certPem: String,
        machineId: String,
        now: Long,
        stored: LicenseStorage.StoredData?
    ): LicenseSnapshot? {
        return try {
            val response = LicenseServerClient.validate(certPem, machineId)

            if (!verifyServerResponse(response)) {
                return LicenseSnapshot(LicenseState.INVALID, message = "Server response signature invalid")
            }

            val updatedData = (stored ?: LicenseStorage.StoredData()).copy(
                lastValidatedAt = now,
                lastServerResponse = response.rawPayload,
                lastServerResponseSignature = response.signature,
                lastServerNonce = response.nonce,
                lastServerTimestamp = response.timestamp
            )
            LicenseStorage.save(updatedData)

            when {
                response.revoked -> LicenseSnapshot(LicenseState.REVOKED, message = response.message)
                !response.valid -> LicenseSnapshot(LicenseState.INVALID, message = response.message)
                response.daysUntilExpiry in 1..EXPIRY_WARNING_DAYS ->
                    LicenseSnapshot(LicenseState.EXPIRING_SOON,
                        daysUntilExpiry = response.daysUntilExpiry,
                        message = "License expires in ${response.daysUntilExpiry} days",
                        crossCheckToken = computeCrossCheckToken(LicenseState.EXPIRING_SOON, machineId, now))
                else ->
                    LicenseSnapshot(LicenseState.VALID,
                        daysUntilExpiry = response.daysUntilExpiry,
                        crossCheckToken = computeCrossCheckToken(LicenseState.VALID, machineId, now))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun handleOfflineGrace(stored: LicenseStorage.StoredData?, now: Long): LicenseSnapshot {
        if (stored == null || stored.lastValidatedAt == 0L) {
            return LicenseSnapshot(LicenseState.OFFLINE_GRACE_EXCEEDED,
                message = "No previous validation — connect to internet")
        }

        if (stored.lastServerTimestamp > 0 && now < stored.lastServerTimestamp) {
            return LicenseSnapshot(LicenseState.INVALID,
                message = "System clock appears to have been set backwards")
        }

        val daysSince = (now - stored.lastValidatedAt) / MILLIS_PER_DAY
        return if (daysSince <= OFFLINE_GRACE_DAYS) {
            val verifiedCachedResponse = verifyCachedServerResponse(stored)
            if (!verifiedCachedResponse) {
                LicenseSnapshot(LicenseState.INVALID, message = "Cached response tampered")
            } else {
                val machineId = MachineFingerprint.compute()
                LicenseSnapshot(LicenseState.VALID,
                    message = "Offline grace: ${OFFLINE_GRACE_DAYS - daysSince} days remaining",
                    crossCheckToken = computeCrossCheckToken(LicenseState.VALID, machineId, now))
            }
        } else {
            LicenseSnapshot(LicenseState.OFFLINE_GRACE_EXCEEDED,
                message = "Offline grace period exceeded — connect to internet")
        }
    }

    private suspend fun validateTrial(
        stored: LicenseStorage.StoredData?,
        machineId: String,
        now: Long
    ): LicenseSnapshot {
        val trialResult = tryOnlineTrialRegistration(stored, machineId, now)
        if (trialResult != null) return trialResult

        return handleOfflineTrial(stored, machineId, now)
    }

    private suspend fun tryOnlineTrialRegistration(
        stored: LicenseStorage.StoredData?,
        machineId: String,
        now: Long
    ): LicenseSnapshot? {
        return try {
            val response = LicenseServerClient.registerTrial(machineId)

            if (!verifyServerResponse(response.baseResponse)) {
                return LicenseSnapshot(LicenseState.INVALID, message = "Trial response signature invalid")
            }

            if (response.trialUsed) {
                val updatedData = (stored ?: LicenseStorage.StoredData()).copy(
                    trialRegistered = true,
                    lastValidatedAt = now,
                    lastServerTimestamp = response.baseResponse.timestamp
                )
                LicenseStorage.save(updatedData)
                return LicenseSnapshot(LicenseState.ACTIVATION_REQUIRED,
                    message = "Trial already used for this machine")
            }

            val firstRun = if (stored?.firstRunAt ?: 0L > 0L) stored!!.firstRunAt else now
            val updatedData = (stored ?: LicenseStorage.StoredData()).copy(
                firstRunAt = firstRun,
                lastValidatedAt = now,
                trialRegistered = true,
                lastServerTimestamp = response.baseResponse.timestamp,
                lastServerResponse = response.baseResponse.rawPayload,
                lastServerResponseSignature = response.baseResponse.signature,
                lastServerNonce = response.baseResponse.nonce
            )
            LicenseStorage.save(updatedData)

            val daysUsed = ((now - firstRun) / MILLIS_PER_DAY).toInt()
            if (daysUsed >= TRIAL_DAYS) {
                LicenseSnapshot(LicenseState.ACTIVATION_REQUIRED,
                    message = "Trial period expired")
            } else {
                LicenseSnapshot(LicenseState.TRIAL,
                    daysUntilExpiry = TRIAL_DAYS - daysUsed,
                    message = "${TRIAL_DAYS - daysUsed} trial days remaining",
                    crossCheckToken = computeCrossCheckToken(LicenseState.TRIAL, machineId, now))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun handleOfflineTrial(
        stored: LicenseStorage.StoredData?,
        machineId: String,
        now: Long
    ): LicenseSnapshot {
        if (stored == null || stored.firstRunAt == 0L) {
            val newData = LicenseStorage.StoredData(firstRunAt = now)
            LicenseStorage.save(newData)
            return LicenseSnapshot(LicenseState.TRIAL,
                daysUntilExpiry = TRIAL_DAYS,
                message = "Internet required for full trial activation. $TRIAL_DAYS days offline trial.",
                crossCheckToken = computeCrossCheckToken(LicenseState.TRIAL, machineId, now))
        }

        if (stored.lastServerTimestamp > 0 && now < stored.lastServerTimestamp) {
            return LicenseSnapshot(LicenseState.INVALID,
                message = "System clock appears to have been set backwards")
        }

        val daysUsed = ((now - stored.firstRunAt) / MILLIS_PER_DAY).toInt()
        return if (daysUsed >= TRIAL_DAYS) {
            LicenseSnapshot(LicenseState.ACTIVATION_REQUIRED,
                message = "Trial period expired — activation required")
        } else {
            LicenseSnapshot(LicenseState.TRIAL,
                daysUntilExpiry = TRIAL_DAYS - daysUsed,
                message = "${TRIAL_DAYS - daysUsed} trial days remaining",
                crossCheckToken = computeCrossCheckToken(LicenseState.TRIAL, machineId, now))
        }
    }

    private fun doQuickLocalCheck(): LicenseSnapshot {
        val stored = LicenseStorage.load()
        val machineId = MachineFingerprint.compute()
        val now = System.currentTimeMillis()

        if (LicenseStorage.hasCertificate()) {
            val certPem = LicenseStorage.loadCertificatePem() ?: return LicenseSnapshot(LicenseState.NOT_FOUND)
            val localErr = verifyCertLocally(certPem)
            if (localErr != null) return localErr
            return handleOfflineGrace(stored, now)
        }

        return handleOfflineTrial(stored, machineId, now)
    }

    // --- Crypto helpers ---

    internal fun parseCertificate(pem: String): X509Certificate {
        val cf = CertificateFactory.getInstance("X.509")
        val cleaned = pem.replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\\s".toRegex(), "")
        val der = Base64.getDecoder().decode(cleaned)
        return cf.generateCertificate(ByteArrayInputStream(der)) as X509Certificate
    }

    internal fun verifyServerResponse(response: LicenseServerClient.ServerResponse): Boolean {
        if (response.signature.isBlank()) return false
        return try {
            val keyPem = EmbeddedServerSigningKey.reconstruct()
            if (keyPem.contains("PLACEHOLDER")) return true
            val keyBytes = keyPem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "")
            val publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(keyBytes)))
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(publicKey)
            val dataToVerify = response.rawPayload + response.nonce + response.timestamp.toString()
            sig.update(dataToVerify.toByteArray(Charsets.UTF_8))
            sig.verify(Base64.getDecoder().decode(response.signature))
        } catch (_: Exception) {
            false
        }
    }

    private fun verifyCachedServerResponse(stored: LicenseStorage.StoredData): Boolean {
        if (stored.lastServerResponseSignature.isBlank()) return false
        val mockResponse = LicenseServerClient.ServerResponse(
            valid = true,
            revoked = false,
            daysUntilExpiry = -1,
            message = "",
            rawPayload = stored.lastServerResponse,
            signature = stored.lastServerResponseSignature,
            nonce = stored.lastServerNonce,
            timestamp = stored.lastServerTimestamp
        )
        return verifyServerResponse(mockResponse)
    }

    internal fun computeCrossCheckToken(state: LicenseState, machineId: String, timestamp: Long): Long {
        val input = "${state.name}|$machineId|$timestamp"
        var hash = -3750763034362895579L // FNV-1a offset basis
        val prime = 1099511628211L        // FNV-1a prime
        for (b in input.toByteArray()) {
            hash = hash xor b.toLong()
            hash *= prime
        }
        return hash
    }
}
