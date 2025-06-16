package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.ui.navigation.SimulatorType

/**
 * Base interface for all simulator configurations
 */
interface SimulatorConfig {
    val id: String
    val name: String
    val description: String
    val simulatorType: SimulatorType
    val enabled: Boolean
    val createdDate: Long
    val modifiedDate: Long
    val version: String
}