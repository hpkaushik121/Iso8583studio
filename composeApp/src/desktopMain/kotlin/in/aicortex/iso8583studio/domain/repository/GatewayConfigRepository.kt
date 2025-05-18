package `in`.aicortex.iso8583studio.domain.repository

import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import kotlinx.coroutines.flow.Flow


/**
 * Repository interface for gateway configurations
 */
interface GatewayConfigRepository {
    /**
     * Get all gateway configurations
     */
    fun getGatewayConfigs(): Flow<List<GatewayConfig>>

    /**
     * Get a specific gateway configuration by ID
     */
    suspend fun getGatewayConfig(id: String): GatewayConfig?

    /**
     * Save a gateway configuration
     */
    suspend fun saveGatewayConfig(config: GatewayConfig): Result<GatewayConfig>

    /**
     * Delete a gateway configuration
     */
    suspend fun deleteGatewayConfig(id: String): Result<Boolean>

    /**
     * Create a new gateway configuration
     */
    suspend fun createGatewayConfig(config: GatewayConfig): Result<GatewayConfig>
}
