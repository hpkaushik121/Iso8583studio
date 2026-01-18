package `in`.aicortex.iso8583studio.hsm.payshield10k.data

import kotlinx.serialization.Serializable

/**
 * Smartcard representation
 */
@Serializable
data class Smartcard(
    val cardId: String,
    val cardType: SmartcardType,
    val pin: String,                        // Hashed PIN
    val data: ByteArray = ByteArray(0),     // Card data (LMK components, etc.)
    val isLocked: Boolean = false,
    val failedAttempts: Int = 0
)
