package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.crypto

import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Application Cryptogram (AC) MAC primitives.
 */
object AcGeneration {

    /**
     * ISO 9797-1 MAC algorithm 3 (a.k.a. Retail MAC) with padding method 2 ("80 00 ...").
     *
     *  * The data is padded with 0x80 followed by 0x00 bytes to a multiple of 8.
     *  * All blocks except the last are CBC-encrypted with single-DES under K1.
     *  * The last block is encrypted with K1, decrypted with K2, then re-encrypted with K1.
     *  * The 8-byte output is the MAC; AC is the leftmost 8 bytes.
     */
    fun computeTdesMac(key: ByteArray, data: ByteArray): ByteArray {
        require(key.size == 16 || key.size == 24) { "TDES MAC key must be 16 or 24 bytes" }
        val k1 = key.copyOfRange(0, 8)
        val k2 = key.copyOfRange(8, 16)
        val padded = padMethod2(data)

        // Single-DES CBC over all blocks but the last with K1.
        var iv = ByteArray(8)
        val lastIdx = padded.size - 8
        if (lastIdx > 0) {
            val cbc = Cipher.getInstance("DESede/CBC/NoPadding")
            cbc.init(Cipher.ENCRYPT_MODE, SecretKeySpec(k1 + k1 + k1, "DESede"), IvParameterSpec(iv))
            val intermediate = cbc.doFinal(padded.copyOfRange(0, lastIdx))
            iv = intermediate.copyOfRange(intermediate.size - 8, intermediate.size)
        }

        // Final block: XOR with iv, then E_K1, D_K2, E_K1.
        val lastBlock = ByteArray(8) { (padded[lastIdx + it].toInt() xor iv[it].toInt()).toByte() }
        val eK1 = desSingle(k1, lastBlock, encrypt = true)
        val dK2 = desSingle(k2, eK1, encrypt = false)
        val eK1Final = desSingle(k1, dK2, encrypt = true)
        return eK1Final
    }

    /** AES-CMAC (RFC 4493) — leftmost 8 bytes are returned for AC use. */
    fun computeAesCmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("AESCMAC")
        mac.init(SecretKeySpec(key, "AES"))
        val full = mac.doFinal(data)
        return full.copyOfRange(0, 8)
    }

    private fun padMethod2(data: ByteArray): ByteArray {
        val padLen = 8 - (data.size % 8)
        val out = ByteArray(data.size + padLen)
        data.copyInto(out)
        out[data.size] = 0x80.toByte()
        // remaining bytes already 0
        return out
    }

    private fun desSingle(k8: ByteArray, block: ByteArray, encrypt: Boolean): ByteArray {
        require(k8.size == 8) { "single-DES key must be 8 bytes" }
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        val tripled = k8 + k8 + k8
        cipher.init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, SecretKeySpec(tripled, "DESede"))
        return cipher.doFinal(block)
    }
}
