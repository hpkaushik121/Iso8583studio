package `in`.aicortex.iso8583studio.hsm

import `in`.aicortex.iso8583studio.hsm.payshield10k.PayShieldStringCommandProcessor

interface HsmSimulator {
    /**
     * Process HSM command and return response
     */
    fun getProcessor(): PayShieldStringCommandProcessor

    /**
     * Initialize HSM with configuration
     */
    fun initialize()

    /**
     * Get HSM status
     */
    fun getStatus(): HsmStatus
}