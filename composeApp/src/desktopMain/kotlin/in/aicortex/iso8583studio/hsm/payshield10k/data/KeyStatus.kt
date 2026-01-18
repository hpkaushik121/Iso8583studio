package `in`.aicortex.iso8583studio.hsm.payshield10k.data


/**
 * Key Status
 */
enum class KeyStatus {
    ACTIVE,
    EXPIRED,
    REVOKED,
    PENDING_ACTIVATION,
    COMPROMISED
}