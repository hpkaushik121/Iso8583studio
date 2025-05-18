package `in`.aicortex.iso8583studio.domain.repository

import `in`.aicortex.iso8583studio.data.model.ConnectionSettings


/**
 * Repository interface for network connections
 */
interface ConnectionRepository {
    /**
     * Get available network interfaces
     */
    suspend fun getNetworkInterfaces(): List<String>

    /**
     * Get available COM ports
     */
    suspend fun getComPorts(): List<String>

    /**
     * Test a connection
     */
    suspend fun testConnection(connectionSettings: ConnectionSettings): Result<Boolean>
}