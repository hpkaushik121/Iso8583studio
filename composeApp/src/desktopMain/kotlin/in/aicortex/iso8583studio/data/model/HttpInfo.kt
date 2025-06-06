package `in`.aicortex.iso8583studio.data.model

import kotlinx.serialization.Serializable

/**
 * HTTP Request Information extracted from raw HTTP request
 */
@Serializable
data class HttpInfo(
    val method: HttpMethod?=null,
    val path: String,
    val version: String = "HTTP/1.1",
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
    val queryParams: Map<String, String> = emptyMap()
) {

    /**
     * Get specific header value (case-insensitive)
     */
    fun getHeader(name: String): String? {
        return headers.entries.find { it.key.equals(name, ignoreCase = true) }?.value
    }

    /**
     * Get content type from headers
     */
    fun getContentType(): String? {
        return getHeader("Content-Type")
    }

    /**
     * Get authorization token from headers
     */
    fun getAuthorizationToken(): String? {
        val authHeader = getHeader("Authorization")
        return when {
            authHeader?.startsWith("Bearer ", ignoreCase = true) == true -> {
                authHeader.substring(7).trim()
            }
            authHeader?.startsWith("Token ", ignoreCase = true) == true -> {
                authHeader.substring(6).trim()
            }
            else -> getHeader("Postman-Token") ?: getHeader("X-Auth-Token")
        }
    }

    /**
     * Check if request contains JSON payload
     */
    fun isJsonContent(): Boolean {
        return getContentType()?.contains("application/json", ignoreCase = true) == true
    }

}
