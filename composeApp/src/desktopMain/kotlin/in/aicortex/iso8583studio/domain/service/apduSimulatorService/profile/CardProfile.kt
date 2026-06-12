package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile

import kotlinx.serialization.Serializable

/**
 * Top-level card profile. A profile defines everything a card emulator needs to behave like a
 * specific physical card: ATR, supported applications, file system, keys.
 *
 * Persistence format is JSON. Binary fields (PAN, keys, certificates) are stored as uppercase hex
 * strings via [HexString] aliases so profiles are diff-able and human-editable.
 */
@Serializable
data class CardProfile(
    val id: String,
    val name: String,
    val scheme: Scheme,
    val atr: HexString,
    val applications: List<CardApplication>,
    /** Optional issuer keys this profile carries (referenced by id from CardApplication). */
    val keys: List<IssuerKey> = emptyList(),
    val notes: String = "",
)

@Serializable
enum class Scheme { VISA, MASTERCARD, AMEX, RUPAY, UNIONPAY, JCB, DISCOVER, OTHER }

/**
 * Hex string alias. Validation happens at the boundary (loaders); the runtime works with bytes.
 */
typealias HexString = String
