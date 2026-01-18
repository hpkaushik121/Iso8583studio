package `in`.aicortex.iso8583studio.hsm.payshield10k.data

import kotlinx.serialization.Serializable

/**
 * Terminal Key Profile - stores all keys for a terminal
 */
@Serializable
data class TerminalKeyProfile(
    val terminalId: String,
    val acquirerId: String,

    // Master Keys
    val tmk: ByteArray,                     // Terminal Master Key
    val zmk: ByteArray? = null,             // Zone Master Key

    // Working Keys (derived from TMK)
    val tpk: ByteArray? = null,             // Terminal PIN Key
    val tak: ByteArray? = null,             // Terminal Authentication Key
    val tdk: ByteArray? = null,             // Terminal Data Key

    // DUKPT Configuration
    val dukptProfile: DukptProfile? = null,

    // Metadata
    val lmkId: String = "00",
    val onboardedAt: Long = System.currentTimeMillis(),
    val keyVersion: Int = 1,
    val status: KeyStatus = KeyStatus.ACTIVE
)