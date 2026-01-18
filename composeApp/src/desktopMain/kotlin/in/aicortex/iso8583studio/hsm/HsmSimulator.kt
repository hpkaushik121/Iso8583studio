package `in`.aicortex.iso8583studio.hsm

import `in`.aicortex.iso8583studio.hsm.payshield10k.PayShieldStringCommandProcessor

interface HsmSimulator {
    /**
     * Process HSM command and return response
     */
    fun getProcessor(): PayShieldStringCommandProcessor

    /**
     * Get HSM features
     */
    fun getFeatures(): HsmFeatures

    /**
     * Initialize HSM with configuration
     */
    suspend fun initialize()

    /**
     * Get HSM status
     */
    fun getStatus(): HsmStatus
}