package `in`.aicortex.iso8583studio.domain.repository

import `in`.aicortex.iso8583studio.data.model.ConnectionSettings
import java.net.NetworkInterface


/**
 * Implementation of ConnectionRepository
 */
class ConnectionRepositoryImpl : ConnectionRepository {

    override suspend fun getNetworkInterfaces(): List<String> {
        return try {
            val interfaces = mutableListOf<String>()
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()

            while (networkInterfaces.hasMoreElements()) {
                val ni = networkInterfaces.nextElement()
                val addresses = ni.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress) {
                        interfaces.add(address.hostAddress)
                    }
                }
            }

            interfaces
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getComPorts(): List<String> {
        // This is platform-specific - would need a proper implementation
        // for each platform. Simplified example:
        return listOf("COM1", "COM2", "COM3", "COM4", "COM5")
    }

    override suspend fun testConnection(connectionSettings: ConnectionSettings): Result<Boolean> {
        // This would contain the actual connection test logic
        return Result.success(true)
    }
}