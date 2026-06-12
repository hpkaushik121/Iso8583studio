package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.crypto

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Session-key derivation. Different EMV CVNs use different session-key methods:
 *  - Visa CVN10: no session derivation; the UDK itself is used directly as the AC key.
 *  - Visa CVN18 / MasterCard CVN1 ("EMV Common Session Key Derivation"): derive a fresh 16-byte
 *    session key per transaction, using the 2-byte ATC (and a divergence byte) encrypted with each
 *    half of the UDK.
 */
object SessionKey {

    /** Visa CVN10 — UDK is used directly as session key. */
    fun visaCommon(udk: ByteArray, atc: Int, un: ByteArray): ByteArray = udk.copyOf()

    /**
     * EMV Common SKD (used by Visa CVN18, MasterCard CVN1 and others). Constructs two 8-byte
     * diversifiers F1 = ATC || F0 || 00.. and F2 = ATC || 0F || 00.., then encrypts each with the
     * appropriate UDK half (TDES single-DES on the half) and concatenates: SK = E_K1(F1) || E_K2(F2).
     */
    fun emvCommon(udk: ByteArray, atc: Int): ByteArray {
        require(udk.size >= 16) { "UDK must be at least 16 bytes for EMV common SKD" }
        val k1 = udk.copyOfRange(0, 8)
        val k2 = udk.copyOfRange(8, 16)
        val atcHi = ((atc ushr 8) and 0xFF).toByte()
        val atcLo = (atc and 0xFF).toByte()
        val f1 = byteArrayOf(atcHi, atcLo, 0xF0.toByte(), 0, 0, 0, 0, 0)
        val f2 = byteArrayOf(atcHi, atcLo, 0x0F.toByte(), 0, 0, 0, 0, 0)
        // Use full TDES (K||K||K) on each diversifier to keep with JCA's DESede primitive.
        val sk1 = desEcbEncrypt(k1, f1)
        val sk2 = desEcbEncrypt(k2, f2)
        return sk1 + sk2
    }

    /** Single-DES ECB encrypt one 8-byte block with an 8-byte key, using DESede with K||K||K. */
    private fun desEcbEncrypt(key8: ByteArray, block: ByteArray): ByteArray {
        require(key8.size == 8) { "key must be 8 bytes" }
        require(block.size == 8) { "block must be 8 bytes" }
        val tripled = key8 + key8 + key8
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(tripled, "DESede"))
        return cipher.doFinal(block)
    }
}
