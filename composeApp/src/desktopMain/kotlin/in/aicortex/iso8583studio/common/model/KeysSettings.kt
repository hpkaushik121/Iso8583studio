package `in`.aicortex.iso8583studio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class KeysSettings(
    val enabled: Boolean = false,
    val selectedIndex: Int = 0,
    val securityKeys: List<SecurityKey> = emptyList()
)