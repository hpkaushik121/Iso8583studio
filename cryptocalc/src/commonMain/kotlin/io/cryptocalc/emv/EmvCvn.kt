package io.cryptocalc.emv

import io.cryptocalc.crypto.primitives.Mac

/**
 * Cryptogram Version Number profiles: which session key, which data layout and which padding a
 * scheme uses for the application cryptogram, and which ARPC method answers it.
 *
 * The transaction data always starts with the EMV minimum set
 * 9F02 ‖ 9F03 ‖ 9F1A ‖ 95 ‖ 5F2A ‖ 9A ‖ 9C ‖ 9F37 ‖ 82 ‖ 9F36, followed by scheme data.
 */
object EmvCvn {

    enum class SessionKeyMethod { NONE, COMMON, MASTERCARD_SKD }
    enum class ArpcMethod { METHOD_1, METHOD_2 }

    class Profile(
        val name: String,
        val scheme: String,
        val sessionKey: SessionKeyMethod,
        val padding: Mac.Padding,
        val arpc: ArpcMethod,
        /** What follows the minimum data set: "CVR4" = 4-byte CVR from IAD, "CVR6" = 6-byte CVR, "IAD" = whole 9F10, "+COUNTERS" adds the 8-byte counters. */
        val tail: String,
        val referenceOnly: Boolean = false,
    )

    val VISA_CVN10 = Profile("Visa CVN 10", "Visa", SessionKeyMethod.NONE, Mac.Padding.METHOD_1, ArpcMethod.METHOD_1, "CVR4")
    val VISA_CVN14 = Profile("Visa CVN 14", "Visa", SessionKeyMethod.COMMON, Mac.Padding.METHOD_1, ArpcMethod.METHOD_1, "IAD", referenceOnly = true)
    val VISA_CVN17 = Profile("Visa CVN 17 (qVSDC)", "Visa", SessionKeyMethod.NONE, Mac.Padding.METHOD_1, ArpcMethod.METHOD_1, "CVR4", referenceOnly = true)
    val VISA_CVN18 = Profile("Visa CVN 18", "Visa", SessionKeyMethod.COMMON, Mac.Padding.METHOD_2, ArpcMethod.METHOD_2, "IAD")
    val VISA_CVN22 = Profile("Visa CVN 22", "Visa", SessionKeyMethod.COMMON, Mac.Padding.METHOD_2, ArpcMethod.METHOD_2, "IAD")
    val MC_CVN10 = Profile("Mastercard CVN 10", "Mastercard", SessionKeyMethod.MASTERCARD_SKD, Mac.Padding.METHOD_2, ArpcMethod.METHOD_1, "CVR6")
    val MC_CVN11 = Profile("Mastercard CVN 11", "Mastercard", SessionKeyMethod.MASTERCARD_SKD, Mac.Padding.METHOD_2, ArpcMethod.METHOD_1, "CVR6+COUNTERS")
    val MC_CVN16 = Profile("Mastercard CVN 16", "Mastercard", SessionKeyMethod.MASTERCARD_SKD, Mac.Padding.METHOD_2, ArpcMethod.METHOD_1, "CVR6")
    val MC_CVN17 = Profile("Mastercard CVN 17", "Mastercard", SessionKeyMethod.MASTERCARD_SKD, Mac.Padding.METHOD_2, ArpcMethod.METHOD_1, "CVR6+COUNTERS")
    val MC_CVN20 = Profile("Mastercard CVN 20", "Mastercard", SessionKeyMethod.COMMON, Mac.Padding.METHOD_2, ArpcMethod.METHOD_1, "CVR6")
    val MC_CVN21 = Profile("Mastercard CVN 21", "Mastercard", SessionKeyMethod.COMMON, Mac.Padding.METHOD_2, ArpcMethod.METHOD_1, "CVR6+COUNTERS")

    val VISA = listOf(VISA_CVN10, VISA_CVN14, VISA_CVN17, VISA_CVN18, VISA_CVN22)
    val MASTERCARD = listOf(MC_CVN10, MC_CVN11, MC_CVN16, MC_CVN17, MC_CVN20, MC_CVN21)

    fun byName(name: String): Profile = (VISA + MASTERCARD).first { it.name == name }

    /** Transaction data common to every CVN. All values are the tag values, no TLV wrapping. */
    class TransactionData(
        val amount: ByteArray, val amountOther: ByteArray, val terminalCountry: ByteArray, val tvr: ByteArray,
        val currency: ByteArray, val date: ByteArray, val type: ByteArray, val unpredictableNumber: ByteArray,
        val aip: ByteArray, val atc: ByteArray,
    ) {
        fun minimumSet(): ByteArray = amount + amountOther + terminalCountry + tvr + currency + date + type + unpredictableNumber + aip + atc
    }

    /** The session key for [profile] from the ICC master key (UDK); the ATC/UN come from [data]. */
    fun sessionKey(profile: Profile, udk: ByteArray, data: TransactionData): ByteArray = when (profile.sessionKey) {
        SessionKeyMethod.NONE -> udk
        SessionKeyMethod.COMMON -> EmvKeys.deriveCommonSessionKey(udk, data.atc, parity = true)
        SessionKeyMethod.MASTERCARD_SKD -> EmvKeys.deriveMastercardSessionKey(udk, data.atc, data.unpredictableNumber, parity = true)
    }

    /**
     * The data the cryptogram is computed over for [profile]. [iad] is the full 9F10; CVRs are
     * extracted from it (Visa: bytes 4..7, Mastercard: bytes 3..8) unless [cvrOverride] is given.
     */
    fun cryptogramData(profile: Profile, data: TransactionData, iad: ByteArray, cvrOverride: ByteArray? = null, counters: ByteArray? = null): ByteArray {
        val base = data.minimumSet()
        return when {
            profile.tail == "IAD" -> base + iad
            profile.tail.startsWith("CVR4") -> base + (cvrOverride ?: visaCvr(iad))
            profile.tail.startsWith("CVR6") -> {
                val cvr = cvrOverride ?: mastercardCvr(iad)
                if (profile.tail.endsWith("+COUNTERS")) base + cvr + (counters ?: mastercardCounters(iad)) else base + cvr
            }
            else -> base + iad
        }
    }

    /** Visa IAD: length byte, derivation key index, CVN, then the 4-byte CVR. */
    fun visaCvr(iad: ByteArray): ByteArray {
        require(iad.size >= 7) { "Visa IAD must be at least 7 bytes (06 ‖ DKI ‖ CVN ‖ CVR)" }
        return iad.copyOfRange(3, 7)
    }

    /** Mastercard IAD: KDI ‖ CVN ‖ CVR (6 bytes) ‖ counters. */
    fun mastercardCvr(iad: ByteArray): ByteArray {
        require(iad.size >= 8) { "Mastercard IAD must be at least 8 bytes (KDI ‖ CVN ‖ CVR)" }
        return iad.copyOfRange(2, 8)
    }

    fun mastercardCounters(iad: ByteArray): ByteArray {
        require(iad.size >= 16) { "Mastercard IAD with counters must be at least 16 bytes" }
        return iad.copyOfRange(8, 16)
    }

    fun generateAc(profile: Profile, sessionKey: ByteArray, cryptogramData: ByteArray): ByteArray =
        EmvAc.generateAc(sessionKey, cryptogramData, profile.padding, 8)

    /** ARPC for [profile]; [arc] is the 2-byte ARC (method 1) or [csu] the 4-byte CSU (method 2). */
    fun generateArpc(profile: Profile, sessionKey: ByteArray, arqc: ByteArray, arc: ByteArray?, csu: ByteArray?, proprietaryAuthData: ByteArray = ByteArray(0)): ByteArray =
        when (profile.arpc) {
            ArpcMethod.METHOD_1 -> EmvAc.arpcMethod1(sessionKey, arqc, arc ?: throw IllegalArgumentException("ARC is required for ARPC method 1"))
            ArpcMethod.METHOD_2 -> EmvAc.arpcMethod2(sessionKey, arqc, csu ?: throw IllegalArgumentException("CSU is required for ARPC method 2"), proprietaryAuthData)
        }
}
