package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile

import kotlinx.serialization.Serializable

/**
 * One EMV application (one AID) within a card. The card may host several — e.g. a co-badged
 * Visa+RuPay card has two CardApplication entries.
 */
@Serializable
data class CardApplication(
    val aid: HexString,
    val label: String,
    val priority: Int = 1,
    /** Application Interchange Profile, 2 bytes hex. */
    val aip: HexString,
    /** Application File Locator, multiple of 4 bytes hex (each entry: SFI<<3, first, last, sda). */
    val afl: HexString,
    /** Records keyed by SFI then record number, value is BER-TLV-encoded record body (tag 70 inner). */
    val records: List<AppRecord>,
    /** Processing Options Data Object List (PDOL), tag-length list bytes hex. May be empty. */
    val pdol: HexString = "",
    /** CDOL1 / CDOL2 tag-length lists. Required if the application supports GENERATE AC. */
    val cdol1: HexString = "",
    val cdol2: HexString = "",
    val pan: HexString,
    val panSequenceNumber: Int = 0,
    val expiryYyMmDd: String,
    val track2Equivalent: HexString,
    val cardholderName: String = "",
    /** id of the IssuerKey entry to use for AC generation; null = SDA-only / static card. */
    val issuerKeyId: String? = null,
    /** EMV CVN (10/18 for Visa, 1 for MasterCard CVC3-style). */
    val cvn: Int = 18,
    /** Initial Application Transaction Counter. */
    val atcStart: Int = 0,
    /** PIN reference data; null = no offline PIN. Stored as hex (PIN block, format-2). */
    val offlinePinBlock: HexString? = null,
    val pinTryLimit: Int = 3,
    /** ICC public key certificate + remainder + exponent for DDA/CDA. Optional. */
    val iccPublicKey: IccPublicKey? = null,
)

/**
 * One record under an EMV application. The runtime returns the record content wrapped in tag 70.
 */
@Serializable
data class AppRecord(
    val sfi: Int,
    val record: Int,
    /** Raw BER-TLV bytes that go inside the tag-70 record template. */
    val tlvHex: HexString,
)

/**
 * ICC RSA key material for DDA/CDA. The certificate is signed by the issuer public key, which is
 * itself signed by the scheme CA. CA index is part of the certificate's data, not stored here.
 */
@Serializable
data class IccPublicKey(
    val modulus: HexString,
    val exponent: HexString,
    /** ICC Public Key Certificate (tag 9F46). */
    val certificate: HexString,
    /** ICC Public Key Remainder (tag 9F48), if modulus exceeds Nca. */
    val remainder: HexString = "",
)
