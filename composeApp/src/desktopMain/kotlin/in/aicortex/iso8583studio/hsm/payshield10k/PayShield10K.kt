package `in`.aicortex.iso8583studio.hsm.payshield10k

import ai.cortex.core.IsoUtil
import `in`.aicortex.iso8583studio.hsm.EncryptionKeyType
import `in`.aicortex.iso8583studio.hsm.HsmConfig
import `in`.aicortex.iso8583studio.hsm.HsmFeatures
import `in`.aicortex.iso8583studio.hsm.HsmResponse
import `in`.aicortex.iso8583studio.hsm.HsmSimulator
import `in`.aicortex.iso8583studio.hsm.HsmStatus
import `in`.aicortex.iso8583studio.hsm.MacTranslationRequest
import `in`.aicortex.iso8583studio.hsm.MacTranslationResponse
import `in`.aicortex.iso8583studio.hsm.PinTranslationRequest
import `in`.aicortex.iso8583studio.hsm.PinTranslationResponse
import `in`.aicortex.iso8583studio.hsm.payshield10k.commands.A0GenerateKeyCommand
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.DukptKeyUsage
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.DukptProfile
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmCommandResult
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.KeyLength
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.KeyType
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.PinBlockFormat
import ai.cortex.core.types.CryptoAlgorithm
import io.cryptocalc.crypto.engines.encryption.EMVEngines
import io.cryptocalc.crypto.engines.encryption.models.SymmetricDecryptionEngineParameters
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
    private val commands = PayShield10KCommandProcessor(payShield,hsmLogsListener)
    private val stringCommands = PayShieldStringCommandProcessor(payShield,hsmLogsListener)
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

    override fun getFeatures(): HsmFeatures {
        return payShield
    }


    /**
     * Initialize the HSM
     */
    override suspend fun initialize() {
        // Auto-load LMK if not loaded
        if (config.lmkStorage.getLmk(config.lmkId) == null) {
            autoLoadLmk(config.lmkId)
        }else{
            config.lmkStorage.liveLmks.forEach {
                payShield.getSlotManager().allocateLmkSlot(it.key,it.value,isDefault = true)
            }
        }

        updateStatus()
    }

    /**
     * Auto-load LMK with default components (for simulation)
     */
    private suspend fun autoLoadLmk(lmkId: String) {
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
     * Directly generate a single key component, bypassing wire format encoding.
     * Returns the raw HsmCommandResult so the caller can access clearKey, encryptedKey and kcv.
     */
    suspend fun generateKeyComponentDirect(
        keyTypeCode: String = "001",
        scheme: String = "U"
    ): HsmCommandResult {
        val keyLength = when (scheme.uppercase()) {
            "T" -> KeyLength.SINGLE
            "X" -> KeyLength.TRIPLE
            else -> KeyLength.DOUBLE
        }
        val keyType = KeyType.values().find { it.code == keyTypeCode } ?: KeyType.TYPE_001
        return commands.executeGenerateKeyComponent(
            lmkId = config.lmkId,
            keyLength = keyLength,
            keyType = keyType
        )
    }

    /**
     * Form a working key from N clear hex components, bypassing wire format.
     * Returns the raw HsmCommandResult whose data map contains clearKey, encryptedKey and kcv.
     */
    suspend fun formKeyFromComponentsDirect(
        keyTypeCode: String = "001",
        scheme: String = "U",
        componentHexList: List<String>
    ): HsmCommandResult {
        val keyType = KeyType.values().find { it.code == keyTypeCode } ?: KeyType.TYPE_001
        val components = componentHexList.map { hex ->
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
        return commands.executeFormKeyFromComponents(
            lmkId = config.lmkId,
            keyType = keyType,
            components = components
        )
    }

    /**
     * Decrypt a key encrypted under the LMK to obtain the clear (plain) key.
     *
     * Strips any scheme prefix (U/X/T/Y/S/R), looks up the LMK pair and variant
     * for the given [keyTypeCode], decrypts using TDES ECB, and returns a map
     * with: clearKey, encryptedKey, kcv, keyType, lmkPair, variant.
     */
    suspend fun decryptKeyUnderLmkDirect(
        encryptedKeyHex: String,
        keyTypeCode: String
    ): HsmCommandResult {
        val keyTypeInfo = A0GenerateKeyCommand.KEY_TYPE_LMK_MAP[keyTypeCode]
            ?: return HsmCommandResult.Error("68", "Invalid key type code: $keyTypeCode")

        val lmk = payShield.lmkStorage.getLmk(config.lmkId)
            ?: return HsmCommandResult.Error("13", "LMK not loaded")

        val lmkPair = lmk.getPair(keyTypeInfo.lmkPairNumber)
            ?: return HsmCommandResult.Error("13", "LMK pair ${keyTypeInfo.lmkPairNumber} not available")

        val stripped = if (encryptedKeyHex.isNotEmpty() &&
            encryptedKeyHex[0].uppercaseChar() in "UXTYSR"
        ) encryptedKeyHex.substring(1) else encryptedKeyHex

        val encBytes = payShield.hexToBytes(stripped)

        val variantMask = A0GenerateKeyCommand.LMK_VARIANTS[keyTypeInfo.variant] ?: 0
        val lmkKey = lmkPair.getCombinedKey().copyOf()
        if (variantMask != 0) {
            lmkKey[0] = (lmkKey[0].toInt() xor variantMask).toByte()
        }

        val clearBytes = try {
            val engine = EMVEngines()
            engine.encryptionEngine.decrypt(
                algorithm = CryptoAlgorithm.TDES,
                decryptionEngineParameters = SymmetricDecryptionEngineParameters(
                    key = lmkKey,
                    data = encBytes,
                    mode = ai.cortex.core.types.CipherMode.ECB
                )
            )
        } catch (e: Exception) {
            return HsmCommandResult.Error("20", "Decryption failed: ${e.message}")
        }
        val clearHex = payShield.bytesToHex(clearBytes)
        val kcv = payShield.calculateKeyCheckValue(clearBytes)

        return HsmCommandResult.Success(
            response = clearHex,
            data = mapOf(
                "clearKey" to clearHex,
                "encryptedKey" to encryptedKeyHex,
                "kcv" to kcv,
                "keyType" to keyTypeCode,
                "keyTypeDescription" to keyTypeInfo.description,
                "lmkPair" to "${keyTypeInfo.lmkPairNumber}-${keyTypeInfo.lmkPairNumber + 1}",
                "variant" to keyTypeInfo.variant.toString()
            )
        )
    }

    /** Calculate the KCV for an arbitrary clear key supplied as hex. */
    fun calculateKcv(keyHex: String): String {
        return try {
            val bytes = payShield.hexToBytes(keyHex)
            payShield.calculateKeyCheckValue(bytes)
        } catch (_: Exception) { "??????" }
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