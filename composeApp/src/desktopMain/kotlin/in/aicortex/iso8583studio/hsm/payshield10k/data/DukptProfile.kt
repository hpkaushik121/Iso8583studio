package `in`.aicortex.iso8583studio.hsm.payshield10k.data

import kotlinx.serialization.Serializable

/**
 * DUKPT Profile for a terminal
 */
@Serializable
data class DukptProfile(
    val initialKey: ByteArray,              // IK - Base Derivation Key
    val keySerialNumber: String,            // KSN (10 bytes hex)
    var currentCounter: Long = 0,           // Transaction counter
    val maxCounter: Long = 1_048_576,       // 2^20 (default DUKPT counter limit)
    val scheme: DukptScheme = DukptScheme.ANSI_X9_24_3DES,
    val bdk: ByteArray,                     // Base Derivation Key
    val counterBits: Int = 21               // Counter bit-width from KSN descriptor
)