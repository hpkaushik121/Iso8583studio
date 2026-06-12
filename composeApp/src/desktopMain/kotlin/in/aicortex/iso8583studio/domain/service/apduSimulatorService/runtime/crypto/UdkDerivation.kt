package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.crypto

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * UDK (Unique Derivation Key) derivation per EMV Book 2 Annex A1.4.1 Method A.
 *
 * Method A is intended for PANs of length <= 16 digits when concatenated with PSN. For longer PANs
 * Method B should be applied first; for our purposes we always use Method A on the rightmost 16
 * digits of (PAN || PSN), which matches the most common Visa/MasterCard personalization recipe.
 */
object UdkDerivation {

    /**
     * Derive a 16-byte 2-key TDES UDK from [imk] (16 or 24 bytes), [pan] (digits) and [psn].
     *
     * The diversification input Y is the rightmost 16 digits of (PAN || PSN-as-2-digits), parsed as
     * BCD. Block A = E_IMK(Y); Block B = E_IMK(Y XOR FF..FF). UDK = (A || B) with odd parity adjusted.
     */
    fun deriveTdesUdk(imk: ByteArray, pan: String, psn: Int): ByteArray {
        val y = buildY(pan, psn)
        val keySpec = SecretKeySpec(normalizeTdesKey(imk), "DESede")
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val a = cipher.doFinal(y)
        val yInv = ByteArray(8) { (y[it].toInt() xor 0xFF).toByte() }
        val b = cipher.doFinal(yInv)
        val udk = a + b
        adjustOddParity(udk)
        return udk
    }

    /**
     * Derive a 16-byte AES-128 UDK using the same diversification scheme but with AES-ECB. Real
     * AES schemes typically use NIST SP 800-108; this is a pragmatic stand-in for emulator use.
     */
    fun deriveAesUdk(imk: ByteArray, pan: String, psn: Int): ByteArray {
        val y8 = buildY(pan, psn)
        // Pad Y to 16 bytes by concatenating with its bitwise inverse to fill an AES block.
        val yInv = ByteArray(8) { (y8[it].toInt() xor 0xFF).toByte() }
        val block = y8 + yInv
        val keySpec = SecretKeySpec(imk.copyOf(16), "AES")
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        return cipher.doFinal(block)
    }

    /** Build the 8-byte diversification value Y = rightmost 16 digits of (PAN || PSN), BCD. */
    private fun buildY(pan: String, psn: Int): ByteArray {
        val digits = pan.filter { it.isDigit() } + "%02d".format(psn and 0xFF)
        val rightmost = if (digits.length >= 16) digits.substring(digits.length - 16) else digits.padStart(16, '0')
        val y = ByteArray(8)
        for (i in 0 until 8) {
            val hi = rightmost[i * 2].digitToInt(10)
            val lo = rightmost[i * 2 + 1].digitToInt(10)
            y[i] = ((hi shl 4) or lo).toByte()
        }
        return y
    }

    /** Expand a 16-byte 2TDES key to 24 bytes (K1||K2||K1) for JCA's DESede transformation. */
    private fun normalizeTdesKey(key: ByteArray): ByteArray = when (key.size) {
        16 -> key + key.copyOfRange(0, 8)
        24 -> key
        else -> error("TDES key must be 16 or 24 bytes, was ${key.size}")
    }

    /** Force each byte to odd parity (low bit set so that total set bits in byte is odd). */
    private fun adjustOddParity(key: ByteArray) {
        for (i in key.indices) {
            var b = key[i].toInt() and 0xFE
            var p = 1
            var v = b
            repeat(7) { p = p xor ((v ushr (it + 1)) and 1) }
            key[i] = (b or p).toByte()
        }
    }
}
