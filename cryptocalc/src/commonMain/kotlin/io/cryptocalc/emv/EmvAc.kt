package io.cryptocalc.emv

import io.cryptocalc.crypto.primitives.Mac
import io.cryptocalc.crypto.primitives.Tdes

/**
 * EMV application cryptograms (EMV 4.x Book 2 §8): ARQC/TC/AAC generation and the two ARPC
 * methods. The session key is 16 bytes; [data] is the concatenated CDOL values, never TLV.
 */
object EmvAc {

    /** ARQC / TC / AAC: ISO 9797-1 Algorithm 3 over [data] with [padding], [macLength] bytes (8 by default). */
    fun generateAc(sessionKey: ByteArray, data: ByteArray, padding: Mac.Padding = Mac.Padding.METHOD_2, macLength: Int = 8): ByteArray =
        Mac.emvMac(sessionKey, data, padding, macLength)

    /**
     * ARPC Method 1: 3DES(SK)[ ARQC XOR (ARC ‖ 00 00 00 00 00 00) ]. [arc] is the 2-byte
     * authorisation response code (the ASCII of "00" is 30 30).
     */
    fun arpcMethod1(sessionKey: ByteArray, arqc: ByteArray, arc: ByteArray): ByteArray {
        require(arqc.size == 8) { "ARQC must be 8 bytes" }
        require(arc.size == 2) { "ARC must be 2 bytes" }
        val x = ByteArray(8) { i -> (arqc[i].toInt() xor (if (i < 2) arc[i].toInt() else 0)).toByte() }
        return Tdes(sessionKey).encryptBlock(x)
    }

    /**
     * ARPC Method 2: the leftmost 4 bytes of the Algorithm 3 MAC (method 2 padding) over
     * ARQC ‖ CSU (4 bytes) ‖ proprietary authentication data (0..8 bytes).
     */
    fun arpcMethod2(sessionKey: ByteArray, arqc: ByteArray, csu: ByteArray, proprietaryAuthData: ByteArray = ByteArray(0)): ByteArray {
        require(arqc.size == 8) { "ARQC must be 8 bytes" }
        require(csu.size == 4) { "CSU must be 4 bytes" }
        require(proprietaryAuthData.size <= 8) { "Proprietary authentication data is at most 8 bytes" }
        return Mac.emvMac(sessionKey, arqc + csu + proprietaryAuthData, Mac.Padding.METHOD_2, 4)
    }

    /** ARC given as text such as "00" or "Z1": its two ASCII bytes. */
    fun arcBytes(arc: String): ByteArray {
        require(arc.length == 2) { "ARC must be two characters" }
        return arc.encodeToByteArray()
    }
}
