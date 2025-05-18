package `in`.aicortex.iso8583studio.data.model

import kotlinx.datetime.Instant

/**
 * Data class representing gateway monitoring statistics
 */
data class GatewayStatistics(
    val connectionCount: Int = 0,
    val concurrentConnections: Int = 0,
    val authenticationFailures: Int = 0,
    val successfulTransactions: Int = 0,
    val bytesIncoming: Long = 0,
    val bytesOutgoing: Long = 0,
    val startTime: Instant? = null,
    val gatewayType: GatewayType? = null,
    val specialFeature: String? = null
)