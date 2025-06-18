package `in`.aicortex.iso8583studio.domain.service.hostSimulatorService

/**
 * Extension function for easy access to power operation
 */
private fun Double.pow(exponent: Int): Double {
    return Math.pow(this, exponent.toDouble())
}