package io.cryptocalc.card

import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import io.cryptocalc.crypto.primitives.Des
import io.cryptocalc.crypto.primitives.Mac
import io.cryptocalc.crypto.primitives.Tdes
import io.cryptocalc.emv.EmvKeys
import io.cryptocalc.pin.PinCrypto

/**
 * Card verification values: Visa CVV / Mastercard CVC (magstripe, CVV2, iCVV, dCVV), the
 * Mastercard contactless CVC3, and a reference implementation of the Amex CSC.
 */
object CardVerification {

    enum class CvvKind(val label: String, val serviceCodeOverride: String?) {
        CVV("CVV/CVC", null),
        ICVV("iCVV", "999"),
        CVV2("CVV2/CVC2", "000"),
        DCVV("dCVV", null),
    }

    /**
     * Visa CVV / Mastercard CVC1 (and the CVV2 / iCVV variants, which only change the service
     * code): PAN ‖ YYMM ‖ service code zero-padded to 32 digits, DES_KA over the first block,
     * XOR with the second block, 3DES under KA‖KB, then two-pass decimalisation to 3 digits.
     *
     * [cvk] is the 16-byte CVK A‖B.
     */
    fun cvv(cvk: ByteArray, pan: String, expiryYYMM: String, serviceCode: String, kind: CvvKind = CvvKind.CVV): String {
        require(cvk.size == 16) { "CVK must be 16 bytes (32 hex: CVK A ‖ CVK B)" }
        val digits = pan.filter { it.isDigit() }
        require(digits.length in 12..19) { "PAN must be 12..19 digits" }
        val exp = expiryYYMM.filter { it.isDigit() }
        require(exp.length == 4) { "Expiry must be YYMM" }
        val sc = (kind.serviceCodeOverride ?: serviceCode).filter { it.isDigit() }
        require(sc.length == 3) { "Service code must be 3 digits" }
        return cvvCore(cvk, digits + exp + sc)
    }

    /**
     * Dynamic CVV (Visa dCVV / MSD): the ATC replaces the service code position and the key is
     * the card's unique key (derived from the issuer master key with EMV Option A when
     * [keyIsIssuerMaster] is set). Input block: PAN ‖ YYMM ‖ ATC(4 digits) as for CVV.
     */
    fun dcvv(key: ByteArray, pan: String, expiryYYMM: String, atcHex: String, psn: String = "00", keyIsIssuerMaster: Boolean = true): String {
        val udk = if (keyIsIssuerMaster) EmvKeys.deriveUdkOptionA(key, pan, psn) else key
        val digits = pan.filter { it.isDigit() }
        val exp = expiryYYMM.filter { it.isDigit() }
        require(exp.length == 4) { "Expiry must be YYMM" }
        val atc = atcHex.hexToBytes()
        require(atc.size == 2) { "ATC must be 2 bytes" }
        val atcDecimal = ((atc[0].toInt() and 0xFF) * 256 + (atc[1].toInt() and 0xFF)).toString().padStart(5, '0')
        // dCVV: the ATC (as decimal) takes the place of the service code in the CVV input.
        return cvvCore(udk, digits + exp + atcDecimal.takeLast(3))
    }

    private fun cvvCore(cvk: ByteArray, input: String): String {
        val padded = input.padEnd(32, '0').take(32)
        val block1 = padded.substring(0, 16).hexToBytes()
        val block2 = padded.substring(16, 32).hexToBytes()
        val ka = cvk.copyOfRange(0, 8)
        val step1 = Des(ka).encryptBlock(block1)
        val x = ByteArray(8) { (step1[it].toInt() xor block2[it].toInt()).toByte() }
        val step2 = Tdes(cvk).encryptBlock(x)
        return PinCrypto.visaDecimalize(step2.toHexUpper(), 3)
    }

    // ------------------------------------------------------------------ Mastercard CVC3

    /**
     * IVCVC3: the last two bytes of the ISO 9797-1 Algorithm 3 MAC (method 2 padding) over the
     * track template under the card's CVC3 key.
     */
    fun ivcvc3(cvc3Key: ByteArray, trackData: ByteArray): ByteArray =
        Mac.ansiX919(cvc3Key, trackData, Mac.Padding.METHOD_2).copyOfRange(6, 8)

    /**
     * Dynamic CVC3 (Mastercard PayPass magstripe): 3DES under the card's CVC3 key over
     * IVCVC3 ‖ UN (4 bytes) ‖ ATC (2 bytes); the last two bytes read as a number give the value,
     * cut to [digits] decimal digits.
     *
     * [key] is the issuer master key when [keyIsIssuerMaster] is set (card key derived with
     * EMV Option A from PAN ‖ PSN), otherwise the card key itself.
     */
    fun cvc3(
        key: ByteArray, pan: String, psn: String, trackData: ByteArray, unHex: String, atcHex: String,
        digits: Int = 3, keyIsIssuerMaster: Boolean = true,
    ): String {
        val cardKey = if (keyIsIssuerMaster) EmvKeys.deriveUdkOptionA(key, pan, psn) else key
        val un = unHex.hexToBytes()
        val atc = atcHex.hexToBytes()
        require(un.size == 4) { "Unpredictable number must be 4 bytes" }
        require(atc.size == 2) { "ATC must be 2 bytes" }
        val iv = ivcvc3(cardKey, trackData)
        val enc = Tdes(cardKey).encryptBlock(iv + un + atc)
        val value = ((enc[6].toInt() and 0xFF) shl 8) or (enc[7].toInt() and 0xFF)
        // The full value is at most 5 digits; the card presents its least significant [digits].
        return value.toString().padStart(5, '0').takeLast(digits)
    }

    // ------------------------------------------------------------------ Amex CSC (reference)

    /**
     * American Express Card Security Code, reference implementation from public descriptions
     * (not verified against the Amex specification): version 1 enciphers PAN ‖ YYMM, version 2
     * PAN ‖ YYMM ‖ service code, under the CSC key with the CVV construction; the 5-, 4- and
     * 3-digit values are consecutive groups of the decimalised result.
     */
    fun amexCsc(cscKey: ByteArray, pan: String, expiryYYMM: String, serviceCode: String, version2: Boolean): Triple<String, String, String> {
        require(cscKey.size == 16) { "CSC key must be 16 bytes" }
        val digits = pan.filter { it.isDigit() }
        val exp = expiryYYMM.filter { it.isDigit() }
        require(exp.length == 4) { "Expiry must be YYMM" }
        val input = digits + exp + (if (version2) serviceCode.filter { it.isDigit() }.padStart(3, '0').take(3) else "")
        val padded = input.padEnd(32, '0').take(32)
        val block1 = padded.substring(0, 16).hexToBytes()
        val block2 = padded.substring(16, 32).hexToBytes()
        val step1 = Des(cscKey.copyOfRange(0, 8)).encryptBlock(block1)
        val x = ByteArray(8) { (step1[it].toInt() xor block2[it].toInt()).toByte() }
        val enc = Tdes(cscKey).encryptBlock(x)
        val dec = PinCrypto.visaDecimalize(enc.toHexUpper(), 12).padEnd(12, '0')
        return Triple(dec.substring(0, 5), dec.substring(5, 9), dec.substring(9, 12))
    }
}
