package io.cryptocalc.emv

import io.cryptocalc.crypto.primitives.Tdes

/**
 * Mastercard Chip Authentication Program (CAP) token computation from a GENERATE AC response,
 * and the ICC dynamic number used by M/Chip CDA.
 */
object EmvCap {

    class Token(val decimal: String, val bits: String, val maskedData: ByteArray, val psnPrefix: Boolean)

    /**
     * CAP token: the IPB (issuer proprietary bitmap) selects bits from CID ‖ ATC ‖ AC ‖ IAD; the
     * selected bits, in order, form a binary number shown in decimal. When the IAF says so (bit 8 of
     * byte 1), the PAN sequence number is prefixed. [ipb] is right-aligned to the data length.
     */
    fun token(ipb: ByteArray, cid: ByteArray, atc: ByteArray, ac: ByteArray, iad: ByteArray, iaf: ByteArray? = null, psn: String? = null): Token {
        val data = cid + atc + ac + iad
        require(ipb.size <= data.size) { "IPB (${ipb.size} bytes) is longer than CID‖ATC‖AC‖IAD (${data.size} bytes)" }
        val mask = ByteArray(data.size - ipb.size) + ipb
        val sb = StringBuilder()
        for (i in data.indices) for (bit in 7 downTo 0) {
            if ((mask[i].toInt() shr bit) and 1 == 1) sb.append((data[i].toInt() shr bit) and 1)
        }
        val bits = sb.toString()
        require(bits.isNotEmpty()) { "IPB selects no bits" }
        var value = com.ionspin.kotlin.bignum.integer.BigInteger.ZERO
        for (c in bits) value = value.shl(1) + (if (c == '1') 1 else 0)
        val psnPrefix = iaf != null && iaf.isNotEmpty() && (iaf[0].toInt() and 0x80) != 0
        val decimal = (if (psnPrefix && !psn.isNullOrBlank()) psn.filter { it.isDigit() } else "") + value.toString(10)
        return Token(decimal, bits, data.indices.map { (data[it].toInt() and mask[it].toInt()).toByte() }.toByteArray(), psnPrefix)
    }

    /**
     * ICC dynamic number for M/Chip (reference implementation): the DN key is derived from MK-DN
     * with Option A for PAN ‖ PSN, and DN = 3DES(K)[ATC ‖ UN ‖ 00 00]. Verify against the
     * Mastercard specification before relying on it.
     */
    fun iccDynamicNumber(mkDn: ByteArray, pan: String, psn: String, atc: ByteArray, unpredictableNumber: ByteArray): Pair<ByteArray, ByteArray> {
        require(atc.size == 2 && unpredictableNumber.size == 4) { "ATC must be 2 bytes and UN 4 bytes" }
        val key = EmvKeys.deriveUdkOptionA(mkDn, pan, psn)
        val dn = Tdes(key).encryptBlock(atc + unpredictableNumber + byteArrayOf(0, 0))
        return key to dn
    }
}
