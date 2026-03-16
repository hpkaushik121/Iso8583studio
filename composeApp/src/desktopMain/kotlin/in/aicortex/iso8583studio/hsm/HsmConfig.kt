package `in`.aicortex.iso8583studio.hsm

import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AcquirerProfile
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkStorage
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.TerminalKeyProfile
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMVendor
import kotlinx.serialization.Serializable

/**
 * HSM Configuration
 */
@Serializable
data class HsmConfig(
    val hsmType: HSMVendor = HSMVendor.THALES,
    val lmkStorage: LmkStorage = LmkStorage(),
    val lmkId: String = "00",
    val tcpLengthHeaderEnabled: Boolean = false,
    val messageHeaderLength: Int = 4,
    // Terminal & Acquirer Management
    val terminalProfiles: MutableMap<String, TerminalKeyProfile> = mutableMapOf<String, TerminalKeyProfile>(),
    val acquirerProfiles: MutableMap<String, AcquirerProfile> = mutableMapOf<String, AcquirerProfile>()
)

/**
 * PIN Translation Request
 * Client → Middleware → Acquirer translation
 */
data class PinTranslationRequest(
    // Source (Client)
    val sourceEncryptedPinBlock: ByteArray,
    val sourceKeyType: EncryptionKeyType,
    val sourceKey: ByteArray? = null,        // For TPK/ZPK
    val sourceDukptKsn: String? = null,      // For DUKPT
    val sourcePinBlockFormat: String = "01", // ISO Format 0

    // Destination (Acquirer)
    val destKeyType: EncryptionKeyType,
    val destKey: ByteArray? = null,
    val destDukptBdk: ByteArray? = null,
    val destPinBlockFormat: String = "01",

    // Transaction data
    val accountNumber: String,
    val terminalId: String? = null,
    val acquirerId: String? = null
)

/**
 * PIN Translation Response
 */
data class PinTranslationResponse(
    val success: Boolean,
    val destEncryptedPinBlock: ByteArray? = null,
    val destKsn: String? = null,
    val errorCode: String = "00",
    val errorMessage: String = "",
    val kcv: String = ""
)

/**
 * MAC Translation Request
 */
data class MacTranslationRequest(
    val data: ByteArray,
    val sourceMac: ByteArray,
    val sourceTak: ByteArray,
    val destTak: ByteArray,
    val macAlgorithm: String = "ISO9797_ALG3"
)

/**
 * MAC Translation Response
 */
data class MacTranslationResponse(
    val success: Boolean,
    val destMac: ByteArray? = null,
    val errorCode: String = "00",
    val errorMessage: String = ""
)

/**
 * Encryption Key Types
 */
enum class EncryptionKeyType {
    TPK,    // Terminal PIN Key
    ZPK,    // Zone PIN Key
    DUKPT,  // DUKPT (BDK-based)
    LMK     // Local Master Key (internal use)
}

/**
 * HSM Response wrapper
 */
data class HsmResponse(
    val success: Boolean,
    val responseCode: String,
    val responseData: Map<String, Any> = emptyMap(),
    val errorMessage: String = ""
)

/**
 * HSM Status
 */
data class HsmStatus(
    val state: String,
    val lmkLoaded: Boolean,
    val lmkCheckValue: String = "",
    val terminalsOnboarded: Int = 0,
    val acquirersRegistered: Int = 0
)