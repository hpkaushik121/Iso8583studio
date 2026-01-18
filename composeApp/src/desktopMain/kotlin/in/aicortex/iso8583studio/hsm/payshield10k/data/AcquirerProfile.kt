package `in`.aicortex.iso8583studio.hsm.payshield10k.data

import kotlinx.serialization.Serializable

/**
 * Acquirer Profile - manages keys for an acquirer
 */
@Serializable
data class AcquirerProfile(
    val acquirerId: String,
    val acquirerName: String,

    // Master Keys
    val zmk: ByteArray,                     // Zone Master Key
    val cvk: ByteArray? = null,             // Card Verification Key
    val pvk: ByteArray? = null,             // PIN Verification Key

    // BDK Registry for DUKPT
    val bdkRegistry: MutableMap<String, ByteArray> = mutableMapOf(),

    // Registered Terminals
    val terminals: MutableMap<String, TerminalKeyProfile> = mutableMapOf(),

    // Encryption Configuration
    val pinBlockFormat: PinBlockFormat = PinBlockFormat.ISO_FORMAT_0,
    val macAlgorithm: String = "ISO9797_ALG3",

    // Routing Configuration
    val primaryEndpoint: String = "",
    val backupEndpoint: String? = null
)