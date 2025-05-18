package `in`.aicortex.iso8583studio.domain.service

import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.GatewayStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
* Monitor service interface for gateway monitoring
*/
interface MonitorService {
    /**
     * Start monitoring a gateway with the specified configuration
     * @param config The gateway configuration to monitor
     */
    suspend fun startMonitoring(config: GatewayConfig)

    /**
     * Start monitoring a POS gateway with the specified configuration
     * @param config The gateway configuration to monitor
     */
    suspend fun startPosGatewayMonitoring(config: GatewayConfig)

    /**
     * Start monitoring an EVN gateway with the specified configuration
     * @param config The gateway configuration to monitor
     */
    suspend fun startEvnGatewayMonitoring(config: GatewayConfig)

    /**
     * Stop monitoring and shutdown the gateway
     */
    suspend fun stopMonitoring()

    /**
     * Clear the log buffer
     */
    fun clearLogs()

    /**
     * Observable flow of log entries
     */
    val logFlow: Flow<String>

    /**
     * Observable flow of gateway statistics
     */
    val statisticsFlow: StateFlow<GatewayStatistics>

    /**
     * Check if monitoring is active
     */
    val isMonitoring: StateFlow<Boolean>
}