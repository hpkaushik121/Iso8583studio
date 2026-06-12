package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile

import kotlinx.serialization.Serializable

/**
 * Issuer-side key material. The runtime never derives UDK on the fly; personalization derives UDK
 * from IMK + PAN/PSN once and stores it here.
 *
 * For symmetric AC keys, [imk] is optional (keep for traceability) and [udk] is what the runtime
 * actually uses for cryptogram generation. For asymmetric (issuer RSA used to sign ICC cert),
 * [issuerRsaModulus] / [issuerRsaPrivExponent] are populated instead.
 */
@Serializable
data class IssuerKey(
    val id: String,
    val kind: KeyKind,
    /** 16/24/32 byte symmetric key — IMK (Issuer Master Key). Hex. Optional for record-keeping. */
    val imk: HexString = "",
    /** Derived UDK (16/24 bytes for 2/3-key TDES; 16/24/32 for AES). */
    val udk: HexString = "",
    val issuerRsaModulus: HexString = "",
    val issuerRsaPrivExponent: HexString = "",
    val issuerRsaPubExponent: HexString = "010001",
)

@Serializable
enum class KeyKind {
    /** TDES UDK used for ARQC/TC/AAC generation. */
    TDES_AC,
    /** AES UDK used for AES-CMAC cryptograms (newer schemes). */
    AES_AC,
    /** Issuer RSA private key used to sign ICC public key certificate. */
    ISSUER_RSA,
}
