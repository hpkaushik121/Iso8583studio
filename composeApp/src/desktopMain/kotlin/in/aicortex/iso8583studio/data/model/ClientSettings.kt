package `in`.aicortex.iso8583studio.data.model

import kotlinx.serialization.Serializable

/**
 * Client-specific settings
 */
@Serializable
data class ClientSettings(
    val clientId: String = "",
    val locationId: String = "",
    val password: String = ""
)