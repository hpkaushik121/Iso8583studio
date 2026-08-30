import `in`.aicortex.iso8583studio.data.BitSpecific
import `in`.aicortex.iso8583studio.data.model.ConnectionType
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import `in`.aicortex.iso8583studio.data.model.RestConfiguration
import `in`.aicortex.iso8583studio.data.model.TransmissionType
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json



/**
 * Message formats for REST API communication
 */
enum class RestMessageFormat(val contentType: String, val description: String) {
    JSON("application/json", "JSON format with field mapping"),
    XML("application/xml", "XML format with hierarchical structure"),
    HEX("text/plain", "Hexadecimal string representation"),
    BASE64("text/plain", "Base64 encoded binary data"),
    BINARY("application/octet-stream", "Raw binary ISO8583 data"),
    FORM_DATA("application/x-www-form-urlencoded", "Form encoded key-value pairs")
}

/**
 * Authentication configuration for REST API
 */
@Serializable
data class RestAuthConfig(
    val type: RestAuthType = RestAuthType.NONE,
    val username: String = "",
    val password: String = "",
    val token: String = "",
    val apiKey: String = "",
    val keyHeader: String = "X-API-Key",
    val bearerPrefix: String = "Bearer"
) {
    fun isConfigured(): Boolean {
        return when (type) {
            RestAuthType.NONE -> true
            RestAuthType.BASIC -> username.isNotBlank() && password.isNotBlank()
            RestAuthType.BEARER -> token.isNotBlank()
            RestAuthType.API_KEY -> apiKey.isNotBlank() && keyHeader.isNotBlank()
            RestAuthType.OAUTH2 -> token.isNotBlank()
        }
    }
}

/**
 * Authentication types for REST API
 */
enum class RestAuthType {
    NONE,
    BASIC,
    BEARER,
    API_KEY,
    OAUTH2
}

/**
 * Retry configuration for REST requests
 */
@Serializable
data class RestRetryConfig(
    val maxRetries: Int = 3,
    val retryOnTimeout: Boolean = true,
    val retryOnServerError: Boolean = true,
    val retryDelayMs: Long = 1000L,
    val exponentialBackoff: Boolean = true,
    val maxDelayMs: Long = 10000L
)

/**
 * SSL/TLS configuration for HTTPS connections
 */
@Serializable
data class RestSslConfig(
    val trustAllCertificates: Boolean = false,
    val certificatePath: String = "",
    val keyStorePath: String = "",
    val keyStorePassword: String = "",
    val trustStorePath: String = "",
    val trustStorePassword: String = ""
)

/**
 * ISO8583 message representation for REST processing
 */
@Serializable
data class Iso8583RestMessage(
    val mti: String,
    val bitmap: String? = null,
    val fields: Map<String, String> = emptyMap(),
    val metadata: MessageMetadata? = null
)

/**
 * Message metadata for tracking and debugging
 */
@Serializable
data class MessageMetadata(
    val requestId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val clientId: String? = null,
    val locationId: String? = null,
    val processingCode: String? = null,
    val messageSource: String = "ISO8583Studio"
)

/**
 * REST response wrapper for ISO8583 messages
 */
@Serializable
data class RestResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: RestError? = null,
    val metadata: ResponseMetadata? = null
)

/**
 * Error information for REST responses
 */
@Serializable
data class RestError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

/**
 * Response metadata for tracking
 */
@Serializable
data class ResponseMetadata(
    val requestId: String,
    val processingTime: Long,
    val serverTimestamp: Long = System.currentTimeMillis()
)

/**
 * Extended gateway handler with REST support
 */
class RestEnabledGatewayHandler(
    val configuration: GatewayConfig
) {
    var restClient: HttpClient? = null
    var restMonitoringJob: Job? = null

    /**
     * Create configured HTTP client for REST communication
     */
    fun createRestClient(): HttpClient {
        val restConfig = configuration.restConfiguration
            ?: throw IllegalStateException("REST configuration not found")

        return HttpClient(CIO) {
            // Configure timeouts
            install(HttpTimeout) {
                requestTimeoutMillis = configuration.transactionTimeOut * 1000L
                connectTimeoutMillis = configuration.transactionTimeOut * 1000L
                socketTimeoutMillis = configuration.transactionTimeOut * 1000L
            }

            // Configure content negotiation
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    prettyPrint = false
                    useAlternativeNames = false
                })
            }

            // Configure logging
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
                filter { request ->
                    request.url.host.contains("localhost") ||
                            request.url.host.contains("127.0.0.1")
                }
            }

            // Configure authentication
            restConfig.authConfig?.let { authConfig ->
                if (authConfig.isConfigured()) {
                    install(Auth) {
                        when (authConfig.type) {
                            RestAuthType.BASIC -> {
                                basic {
                                    credentials {
                                        BasicAuthCredentials(
                                            username = authConfig.username,
                                            password = authConfig.password
                                        )
                                    }
                                }
                            }
                            RestAuthType.BEARER -> {
                                bearer {
                                    loadTokens {
                                        BearerTokens(
                                            accessToken = authConfig.token,
                                            refreshToken = ""
                                        )
                                    }
                                }
                            }
                            else -> { /* API Key handled in headers */ }
                        }
                    }
                }
            }

            // Configure retry mechanism
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = restConfig.retryConfig.maxRetries)
                retryOnException(
                    maxRetries = restConfig.retryConfig.maxRetries,
                    retryOnTimeout = restConfig.retryConfig.retryOnTimeout
                )

                if (restConfig.retryConfig.exponentialBackoff) {
                    exponentialDelay(
                        base = restConfig.retryConfig.retryDelayMs.toDouble(),
                        maxDelayMs = restConfig.retryConfig.maxDelayMs
                    )
                } else {
                    constantDelay(restConfig.retryConfig.retryDelayMs)
                }
            }

            // Configure default request
            defaultRequest {
                // Add configured headers
                restConfig.headers.forEach { (key, value) ->
                    header(key, value)
                }

                // Add authentication headers
                restConfig.authConfig?.let { authConfig ->
                    when (authConfig.type) {
                        RestAuthType.API_KEY -> {
                            header(authConfig.keyHeader, authConfig.apiKey)
                        }
                        RestAuthType.BEARER -> {
                            header("Authorization", "${authConfig.bearerPrefix} ${authConfig.token}")
                        }
                        else -> { /* Handled by Auth plugin */ }
                    }
                }

                // Add standard headers
                header("User-Agent", "ISO8583Studio/2.0")
                header("Accept", restConfig.messageFormat.contentType)
            }
        }
    }

    /**
     * Close REST client and cleanup resources
     */
    suspend fun closeRestClient() {
        restMonitoringJob?.cancel()
        restClient?.close()
        restClient = null
    }
}

/**
 * Enhanced gateway configuration with REST support
 */
data class EnhancedGatewayConfig(
    val name: String,
    val gatewayType: GatewayType,
    val serverAddress: String,
    val serverPort: Int,
    val destinationServer: String,
    val destinationPort: Int,
    val destinationConnectionType: ConnectionType,
    val maxConcurrentConnection: Int,
    val transactionTimeOut: Int,
    val transmissionType: TransmissionType,
    val connectionType: ConnectionType,
    val logFileName: String,
    val maxLogSizeInMB: Int,
    val bitTemplate: Array<BitSpecific>,
    val restConfiguration: RestConfiguration? = null,
    val clientId: String? = null,
    val locationId: String? = null,
    val messageLengthTypeSource: MessageLengthType? = null,
    val advanceOptions: AdvanceOptions? = null
) {
    fun isRestEnabled(): Boolean {
        return destinationConnectionType == ConnectionType.REST &&
                restConfiguration?.isValid() == true
    }
}

/**
 * Advanced options for gateway configuration
 */
data class AdvanceOptions(
    val sslClient: Boolean = false,
    val enableKeepAlive: Boolean = true,
    val enableCompression: Boolean = false,
    val customProperties: Map<String, Any> = emptyMap()
)

/**
 * Connection health status for monitoring
 */
enum class ConnectionHealth {
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    UNKNOWN
}

/**
 * Connection status information
 */
data class ConnectionStatus(
    val type: ConnectionType,
    val health: ConnectionHealth,
    val lastChecked: Long = System.currentTimeMillis(),
    val errorCount: Int = 0,
    val uptime: Long = 0,
    val details: Map<String, Any> = emptyMap()
)

/**
 * REST-specific connection metrics
 */
data class RestConnectionMetrics(
    val totalRequests: Long = 0,
    val successfulRequests: Long = 0,
    val failedRequests: Long = 0,
    val averageResponseTime: Double = 0.0,
    val lastRequestTime: Long = 0,
    val activeConnections: Int = 0
) {
    val successRate: Double
        get() = if (totalRequests > 0) {
            (successfulRequests.toDouble() / totalRequests) * 100
        } else 0.0
}