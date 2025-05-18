package `in`.aicortex.iso8583studio.domain.service

import `in`.aicortex.iso8583studio.data.GatewayClient
import `in`.aicortex.iso8583studio.data.KeyManagement
import `in`.aicortex.iso8583studio.data.PermanentConnection
import `in`.aicortex.iso8583studio.data.model.ConnectionStatus
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.ui.navigation.GatewayConfigurationState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime

/**
* Gateway service interface that defines the functionality of a security gateway
*/
interface GatewayService {
    /**
     * Get the current gateway configuration
     */
    val configuration: GatewayConfig

    /**
     * Set a new gateway configuration
     */
    fun setConfiguration(config: GatewayConfig)

    /**
     * Start the gateway service
     */
    suspend fun start()

    /**
     * Stop the gateway service
     */
    suspend fun stop()

    /**
     * Check if the gateway is currently running
     */
    fun isStarted(): Boolean

    /**
     * Get the number of active connections
     */
    fun getConnectionCount(): Int

    /**
     * Get the list of active client connections
     */
    fun getConnectionList(): List<GatewayClient>

    /**
     * Get the main permanent connection if it exists
     */
    fun getMainPermanentConnection(): PermanentConnection?

    /**
     * Get the total number of bytes received from clients
     */
    fun getBytesIncoming(): Int

    /**
     * Get the total number of bytes sent to clients
     */
    fun getBytesOutgoing(): Int

    /**
     * Get the time when the gateway was started
     */
    fun getStartTime(): LocalDateTime?

    /**
     * Get the time when the gateway was stopped
     */
    fun getStopTime(): LocalDateTime?

    /**
     * Get statistics about the gateway operations
     */
    fun getStatistics(): String

    /**
     * Get details about all client connections
     */
    fun getConnectionListString(): String

    /**
     * Create a new client
     */
    fun createClient(): GatewayClient

    /**
     * Write a log message
     */
    suspend fun writeLog(message: String)

    /**
     * Write a log message with a client reference
     */
    suspend fun writeLog(client: GatewayClient, message: String)

    /**
     * Get the encryption/decryption service
     */
    val endeService: KeyManagement

    /**
     * Add callback for error handling
     */
    fun onError(callback: suspend (Exception) -> Unit)

    /**
     * Add callback for data sent event
     */
    fun onSentFormattedData(callback: suspend (GatewayClient) -> Unit)

    /**
     * Add callback for data received event
     */
    fun onReceivedFormattedData(callback: suspend (GatewayClient) -> Unit)

    /**
     * Add callback for before receive event
     */
    fun beforeReceive(callback: suspend (GatewayClient) -> Unit)

    /**
     * Add callback for client error event
     */
    fun onClientError(callback: suspend (GatewayClient) -> Unit)

    /**
     * Add callback for before write log event
     */
    fun beforeWriteLog(callback: suspend (String) -> Unit)

    /**
     * Add callback for admin response handling
     */
    fun onAdminResponse(callback: suspend (GatewayClient, ByteArray) -> ByteArray?)

    /**
     * Add callback for data received from source
     */
    fun onReceiveFromSource(callback: suspend (GatewayClient, ByteArray) -> ByteArray)

    /**
     * Add callback for data received from destination
     */
    fun onReceiveFromDest(callback: suspend (GatewayClient, ByteArray) -> ByteArray)
}