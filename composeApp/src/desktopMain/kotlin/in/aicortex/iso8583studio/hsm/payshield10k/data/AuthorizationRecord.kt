package `in`.aicortex.iso8583studio.hsm.payshield10k.data

import kotlinx.serialization.Serializable

/**
 * Authorization record
 */
@Serializable
data class AuthorizationRecord(
    val lmkId: String,
    val activity: AuthActivity,
    val authorizedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (12 * 60 * 60 * 1000), // 12 hours
    val authorizedBy: List<String> = emptyList()  // Officer card IDs
)