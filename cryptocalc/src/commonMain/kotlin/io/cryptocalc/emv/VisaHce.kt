package io.cryptocalc.emv

import ai.cortex.core.CryptoUtils
import ai.cortex.core.hexToBytes
import io.cryptocalc.card.CardVerification
import io.cryptocalc.crypto.primitives.Mac
import io.cryptocalc.crypto.primitives.Tdes

/**
 * Visa cloud-based payments (HCE) reference implementation from public descriptions of the
 * limited-use key scheme. Not verified against the Visa specification; the calculator says so.
 */
object VisaHce {

    /**
     * Limited-use key: LUK = 3DES(UDK)[Y ‖ HHHH ‖ CC ‖ 80 00] ‖ 3DES(UDK)[(Y ‖ HHHH ‖ CC ‖ 80 00) XOR FF..FF]
     * where Y is the year (2 digits), HHHH the hour counter (4 digits) and CC the hourly counter.
     */
    fun limitedUseKey(udk: ByteArray, year: String, hours: String, counter: String, parity: Boolean = true): ByteArray {
        require(udk.size == 16) { "UDK must be 16 bytes" }
        val digits = year.filter { it.isDigit() }.padStart(2, '0').takeLast(2) + hours.filter { it.isDigit() }.padStart(4, '0').takeLast(4) + counter.filter { it.isDigit() }.padStart(2, '0').takeLast(2)
        val block = (digits + "8000").hexToBytes()
        val cipher = Tdes(udk)
        val left = cipher.encryptBlock(block)
        val right = cipher.encryptBlock(ByteArray(8) { (block[it].toInt() xor 0xFF).toByte() })
        val luk = left + right
        return if (parity) CryptoUtils.applyParity(luk, isOdd = true) else luk
    }

    /** MSD dynamic CVV over the LUK (ATC-based, service code 999). */
    fun msdDynamicCvv(luk: ByteArray, pan: String, expiryYYMM: String, atcHex: String): String =
        CardVerification.dcvv(luk, pan, expiryYYMM, atcHex, keyIsIssuerMaster = false)

    /** qVSDC cryptogram (CVN 43 style): ISO 9797-1 algorithm 3 over the transaction data with padding method 2 under the LUK. */
    fun qvsdcCryptogram(luk: ByteArray, data: ByteArray): ByteArray = Mac.emvMac(luk, data, Mac.Padding.METHOD_2, 8)
}
