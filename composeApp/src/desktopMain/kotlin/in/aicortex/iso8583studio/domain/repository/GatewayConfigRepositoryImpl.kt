package `in`.aicortex.iso8583studio.domain.repository

import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import kotlinx.coroutines.flow.Flow

class GatewayConfigRepositoryImpl : GatewayConfigRepository{
    override fun getGatewayConfigs(): Flow<List<GatewayConfig>> {
        TODO("Not yet implemented")
    }

    override suspend fun getGatewayConfig(id: String): GatewayConfig? {
        TODO("Not yet implemented")
    }

    override suspend fun saveGatewayConfig(config: GatewayConfig): Result<GatewayConfig> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteGatewayConfig(id: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun createGatewayConfig(config: GatewayConfig): Result<GatewayConfig> {
        TODO("Not yet implemented")
    }

}