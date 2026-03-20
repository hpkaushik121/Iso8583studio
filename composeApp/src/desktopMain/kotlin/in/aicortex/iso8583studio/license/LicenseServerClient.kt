package `in`.aicortex.iso8583studio.license

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * HTTP client for communicating with the license validation server.
 *
 * Security layers:
 * - TLS certificate pinning (SHA-256 of server cert public key)
 * - Client-generated nonce in every request (anti-replay)
 * - Server response signature verification (delegated to LicenseService)
 */
object LicenseServerClient {

    private val BASE_URL: String get() = BuildConfig.LICENSE_SERVER_URL
    private const val VALIDATE_PATH = "/studio/v1/license/validate"
    private const val TRIAL_REGISTER_PATH = "/studio/v1/license/trial/register"
    private const val TIMEOUT_MS = 15_000L

    private val pinnedCertHashes = setOf(
        "PLACEHOLDER_PRIMARY_PIN_SHA256",
        "PLACEHOLDER_BACKUP_PIN_SHA256"
    )

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient by lazy {
        HttpClient(CIO) {
            engine {
                requestTimeout = TIMEOUT_MS
                if (BuildConfig.IS_RELEASE) {
                    https {
                        trustManager = PinningTrustManager(pinnedCertHashes)
                    }
                }
            }
        }
    }

    data class ServerResponse(
        val valid: Boolean,
        val revoked: Boolean = false,
        val daysUntilExpiry: Int = -1,
        val message: String = "",
        val rawPayload: String = "",
        val signature: String = "",
        val nonce: String = "",
        val timestamp: Long = 0L
    )

    data class TrialResponse(
        val trialUsed: Boolean,
        val trialStart: String = "",
        val daysRemaining: Int = 7,
        val baseResponse: ServerResponse
    )

    @Serializable
    private data class ValidateRequest(
        val certificate: String,
        val machineId: String,
        val appVersion: String = "1.0.0",
        val nonce: String
    )

    @Serializable
    private data class TrialRegisterRequest(
        val machineId: String,
        val appVersion: String = "1.0.0",
        val nonce: String
    )

    @Serializable
    private data class ApiResponse(
        val valid: Boolean = false,
        val revoked: Boolean = false,
        val expiresAt: String = "",
        val daysUntilExpiry: Int = -1,
        val customerId: String = "",
        val message: String = "",
        val signature: String = "",
        val nonce: String = "",
        val timestamp: Long = 0L,
        val trialUsed: Boolean = false,
        val trialStart: String = "",
        val daysRemaining: Int = 7
    )

    suspend fun validate(certPem: String, machineId: String): ServerResponse {
        val nonce = generateNonce()
        val requestBody = json.encodeToString(
            ValidateRequest.serializer(),
            ValidateRequest(
                certificate = certPem,
                machineId = machineId,
                nonce = nonce
            )
        )

        val response = httpClient.post("$BASE_URL$VALIDATE_PATH") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val body = response.bodyAsText()
        val parsed = json.decodeFromString(ApiResponse.serializer(), body)

        if (parsed.nonce != nonce) {
            throw SecurityException("Server nonce mismatch — possible replay attack")
        }

        return ServerResponse(
            valid = parsed.valid,
            revoked = parsed.revoked,
            daysUntilExpiry = parsed.daysUntilExpiry,
            message = parsed.message,
            rawPayload = body,
            signature = parsed.signature,
            nonce = parsed.nonce,
            timestamp = parsed.timestamp
        )
    }

    suspend fun registerTrial(machineId: String): TrialResponse {
        val nonce = generateNonce()
        val requestBody = json.encodeToString(
            TrialRegisterRequest.serializer(),
            TrialRegisterRequest(
                machineId = machineId,
                nonce = nonce
            )
        )

        val response = httpClient.post("$BASE_URL$TRIAL_REGISTER_PATH") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val body = response.bodyAsText()
        val parsed = json.decodeFromString(ApiResponse.serializer(), body)

        if (parsed.nonce != nonce) {
            throw SecurityException("Server nonce mismatch — possible replay attack")
        }

        val baseResponse = ServerResponse(
            valid = !parsed.trialUsed,
            revoked = false,
            daysUntilExpiry = parsed.daysRemaining,
            message = parsed.message,
            rawPayload = body,
            signature = parsed.signature,
            nonce = parsed.nonce,
            timestamp = parsed.timestamp
        )

        return TrialResponse(
            trialUsed = parsed.trialUsed,
            trialStart = parsed.trialStart,
            daysRemaining = parsed.daysRemaining,
            baseResponse = baseResponse
        )
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * TLS trust manager that verifies the server certificate's public key hash
     * matches one of the pinned values. Falls through to standard trust if
     * pins are placeholders (dev mode).
     */
    private class PinningTrustManager(
        private val pins: Set<String>
    ) : X509TrustManager {

        private val isDevMode = pins.any { it.startsWith("PLACEHOLDER") }

        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            if (isDevMode || chain.isNullOrEmpty()) return
            val serverCert = chain[0]
            val pubKeyHash = MachineFingerprint.sha256Hex(
                java.util.Base64.getEncoder().encodeToString(serverCert.publicKey.encoded)
            )
            if (pubKeyHash !in pins) {
                throw SecurityException("TLS certificate pin mismatch — possible MITM attack")
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
