package `in`.aicortex.iso8583studio.hsm.payshield10k

import ai.cortex.core.IsoUtil
import `in`.aicortex.iso8583studio.hsm.EncryptionKeyType
import `in`.aicortex.iso8583studio.hsm.HsmConfig
import `in`.aicortex.iso8583studio.hsm.HsmResponse
import `in`.aicortex.iso8583studio.hsm.HsmSimulator
import `in`.aicortex.iso8583studio.hsm.HsmStatus
import `in`.aicortex.iso8583studio.hsm.MacTranslationRequest
import `in`.aicortex.iso8583studio.hsm.MacTranslationResponse
import `in`.aicortex.iso8583studio.hsm.PinTranslationRequest
import `in`.aicortex.iso8583studio.hsm.PinTranslationResponse
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.DukptKeyUsage
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.DukptProfile
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmCommandResult
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.PinBlockFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


// ====================================================================================================
// PAYSHIELD 10K ADAPTER - BRIDGES HsmService → PayShield10K Simulator
// ====================================================================================================

/**
 * Adapter that connects HsmServiceImpl to PayShield 10K Simulator
 * This is the BRIDGE between your ISO8583Studio and the PayShield simulator
 */
class PayShield10K(val config: HsmConfig,val hsmLogsListener: HsmLogsListener) : HsmSimulator {

    // PayShield 10K components
    private val payShield = PayShield10KFeatures(config,hsmLogsListener)
    private val commands = PayShield10KCommandProcessor(payShield)
    private val stringCommands = PayShieldStringCommandProcessor(payShield)
    private val advanced = PayShield10KAdvancedFeatures(payShield, commands)

    // State management
    private val _status = MutableStateFlow(
        HsmStatus(
        state = "OFFLINE",
        lmkLoaded = false
    )
    )
    val status: StateFlow<HsmStatus> = _status
    override fun getProcessor(): PayShieldStringCommandProcessor {
        return stringCommands
    }


    /**
     * Initialize the HSM
     */
    override fun initialize() {
        // Auto-load LMK if not loaded
        if (config.lmkStorage.getLmk(config.lmkId) == null) {
            autoLoadLmk(config.lmkId)
        }else{
            config.lmkStorage.liveLmks.forEach {
                payShield.slotManager.allocateLmkSlot(it.key,it.value,isDefault = true)
            }
        }

        updateStatus()
    }

    /**
     * Auto-load LMK with default components (for simulation)
     */
    private fun autoLoadLmk(lmkId: String) {
        try {
            // Generate 3 components
            val comp1 = payShield.executeGenerateLmkComponent(lmkId, 1)
            val comp2 = payShield.executeGenerateLmkComponent(lmkId, 2)
            val comp3 = payShield.executeGenerateLmkComponent(lmkId, 3)

            // Extract and load
            val components = listOf(
                IsoUtil.hexToBytes((comp1 as HsmCommandResult.Success).data["clearComponent"] as String),
                IsoUtil.hexToBytes((comp2 as HsmCommandResult.Success).data["clearComponent"] as String),
                IsoUtil.hexToBytes((comp3 as HsmCommandResult.Success).data["clearComponent"] as String)
            )

            payShield.executeLoadLmk(lmkId, components)

            updateStatus()
        } catch (e: Exception) {
            println("Auto-load LMK failed: ${e.message}")
        }
    }

    /**
     * Get HSM status
     */
    override fun getStatus(): HsmStatus {
        return _status.value
    }

    /**
     * Update HSM status
     */
    private fun updateStatus() {
        val lmk = payShield.lmkStorage.getLmk(config?.lmkId ?: "00")
        _status.value = HsmStatus(
            state = payShield.currentState.name,
            lmkLoaded = lmk != null,
            lmkCheckValue = lmk?.checkValue ?: "",
            terminalsOnboarded = advanced.terminalProfiles.size,
            acquirersRegistered = advanced.acquirerProfiles.size
        )
    }
}