package `in`.aicortex.iso8583studio.data.model

import kotlinx.serialization.Serializable

/**
 * Connection settings
 */
@Serializable
data class ConnectionSettings(
    val connectionType: ConnectionType = ConnectionType.TCP_IP,
    val address: String = "127.0.0.1",
    val port: Int = 8080,
    val comPort: String = "COM1",
    val baudRate: Int = 115200,
    val phoneNumber: String = ""
)