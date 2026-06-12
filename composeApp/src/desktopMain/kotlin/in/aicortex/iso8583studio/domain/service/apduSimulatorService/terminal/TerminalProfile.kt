package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.terminal

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.HexString
import kotlinx.serialization.Serializable

/**
 * Acquirer-side terminal configuration. Drives the EMV Book 3 transaction state machine when the
 * simulator is acting as the terminal (PC/SC reader mode, or running a transaction against a
 * loopback profile). Mirrors a real POS terminal's parameter set.
 *
 * Fields named with the EMV tag in parentheses are emitted into PDOL/CDOL responses verbatim.
 */
@Serializable
data class TerminalProfile(
    val id: String = "default-terminal",
    val name: String = "Default attended POS",
    /** 9F35 Terminal Type. 0x22 = attended online merchant, magstripe + IC. */
    val terminalType: Int = 0x22,
    /** 9F33 Terminal Capabilities (3 bytes hex). E0F8C8 = magstripe, IC w/contacts, plaintext PIN, enciphered PIN, signature, online PIN, etc. */
    val terminalCapabilities: HexString = "E0F8C8",
    /** 9F40 Additional Terminal Capabilities (5 bytes hex). 6000F0A001 = goods+services, no admin, all data input. */
    val additionalCapabilities: HexString = "6000F0A001",
    /** 9F1A Terminal Country Code (n3, 2 bytes hex). 0356 = India. */
    val terminalCountryCode: HexString = "0356",
    /** 5F2A Transaction Currency Code (n3, 2 bytes hex). 0356 = INR. */
    val transactionCurrencyCode: HexString = "0356",
    /** 5F36 Transaction Currency Exponent. 2 for INR/USD/EUR. */
    val transactionCurrencyExp: Int = 2,
    /** 9F1E Interface Device (IFD) Serial Number (8 chars ASCII). */
    val ifdSerialNumber: String = "12345678",
    /** 9F15 Merchant Category Code (n4). 5411 = grocery. */
    val merchantCategoryCode: String = "5411",
    /** 9F16 Merchant Identifier (15 chars ASCII). */
    val merchantId: String = "MERCHANT-001  ",
    /** 9F1C Terminal Identification (8 chars ASCII). */
    val terminalIdentification: String = "TERM0001",
    /** Per-AID terminal parameters: TAC bytes, floor limits, kernel selection. */
    val perAid: List<AidTerminalConfig> = emptyList(),
    /** Loaded scheme CA public keys (tag 9F22 references one of these by index+RID). */
    val capks: List<Capk> = emptyList(),
    /** Online-issuer connection settings used when the card returns ARQC. */
    val issuerHost: IssuerHostConfig = IssuerHostConfig(),
)

@Serializable
data class AidTerminalConfig(
    /** AID this row applies to (matches a CardApplication.aid on the card side). */
    val aid: HexString,
    /** Friendly label for the UI. */
    val label: String = "",
    /** Terminal Action Codes — Default. 5 bytes hex. Triggers offline decline if any bit set in TVR. */
    val tacDefault: HexString = "0000000000",
    /** Terminal Action Codes — Denial. 5 bytes hex. Triggers AAC. */
    val tacDenial: HexString = "0000000000",
    /** Terminal Action Codes — Online. 5 bytes hex. Triggers ARQC instead of TC. */
    val tacOnline: HexString = "0000000000",
    /** Floor limit in minor units (paise/cents). Above this, terminal forces online. */
    val floorLimit: Long = 0,
    /** Random selection target percentage (0..99). */
    val targetPercent: Int = 0,
    /** Random selection threshold in minor units. */
    val threshold: Long = 0,
    /** Random selection max target percentage (0..99). */
    val maxTargetPercent: Int = 99,
    /** Cardholder Verification Method capabilities. */
    val cvmOnlinePin: Boolean = true,
    val cvmOfflinePin: Boolean = false,
    val cvmSignature: Boolean = true,
    val cvmNoCvm: Boolean = false,
    /** EMV kernel id. 0 = contact (book 3). 2/3/4/5/6/7 = scheme contactless kernels. */
    val kernelId: Int = 0,
    /** Per-AID enabled flag. */
    val enabled: Boolean = true,
)

/**
 * Scheme CA Public Key entry. The terminal verifies the issuer public-key certificate read from
 * the card with the CA key whose RID + Index match the card's data. Each scheme publishes a list
 * of CA keys (production and test).
 */
@Serializable
data class Capk(
    /** Registered Application Provider Identifier — first 5 bytes of the AID, e.g. A000000003 (Visa). */
    val rid: HexString,
    /** Index byte (1 byte). */
    val index: Int,
    /** RSA modulus (hex). */
    val modulus: HexString,
    /** RSA public exponent (typically 03 or 010001). */
    val exponent: HexString = "03",
    /** Expiry date as YYMMDD (n6). */
    val expiryYyMmDd: String = "991231",
    /** SHA-1 over RID || index || modulus || exponent (hex). */
    val checksum: HexString = "",
    /** Free-text source (e.g. "EMV CA test"). */
    val source: String = "",
)

@Serializable
data class IssuerHostConfig(
    val enabled: Boolean = false,
    val host: String = "127.0.0.1",
    val port: Int = 8583,
    /** Connect timeout in millis. */
    val connectTimeoutMs: Int = 5000,
    /** ARQC->ARPC roundtrip timeout in millis. */
    val responseTimeoutMs: Int = 8000,
)
