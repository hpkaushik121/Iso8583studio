package io.cryptocalc.emv

import ai.cortex.core.CryptoUtils
import ai.cortex.core.xorWith
import io.cryptocalc.crypto.primitives.Tdes

/**
 * Mastercard Data Storage (EMV Contactless Book C-2 Annex, "DS") one-way hash functions,
 * reference implementation from public descriptions. Not verified against the specification.
 */
object EmvDataStorage {

    /**
     * DS partial key from the 8-byte DS ID: K = oddParity(DSID ‖ DSID XOR FF..FF) with every nibble
     * above 9 folded back into 0..9 (the published nibble transform keeps the key printable).
     */
    fun partialKey(dsId: ByteArray): ByteArray {
        require(dsId.size == 8) { "DS ID must be 8 bytes" }
        fun fold(b: Byte): Byte {
            val hi = (b.toInt() shr 4) and 0x0F
            val lo = b.toInt() and 0x0F
            val h = if (hi > 9) hi - 10 else hi
            val l = if (lo > 9) lo - 10 else lo
            return ((h shl 4) or l).toByte()
        }
        val left = ByteArray(8) { fold(dsId[it]) }
        val right = ByteArray(8) { fold((dsId[it].toInt() xor 0xFF).toByte()) }
        return CryptoUtils.applyParity(left + right, isOdd = true)
    }

    /** OWHF2 (DS digest H): 3DES(K)[M] XOR M over the 8-byte message M. */
    fun owhf2(dspk: ByteArray, message: ByteArray): ByteArray {
        require(dspk.size == 16) { "DSPK must be 16 bytes" }
        require(message.size == 8) { "Message must be 8 bytes" }
        return Tdes(dspk).encryptBlock(message).xorWith(message)
    }

    /** OWHF2 over longer input: the message is processed in CBC-like chaining of OWHF2 blocks. */
    fun owhf2Chained(dspk: ByteArray, message: ByteArray): ByteArray {
        require(message.isNotEmpty() && message.size % 8 == 0) { "Message must be a multiple of 8 bytes" }
        var h = ByteArray(8)
        for (off in message.indices step 8) h = owhf2(dspk, message.copyOfRange(off, off + 8).xorWith(h))
        return h
    }

    /** OWHF1 (DS summary): single-DES based hash H = DES(K_L XOR M_i)[M_i] XOR M_i chained over the 8-byte blocks. */
    fun owhf1(dspk: ByteArray, message: ByteArray): ByteArray {
        require(dspk.size == 16) { "DSPK must be 16 bytes" }
        val padded = if (message.size % 8 == 0 && message.isNotEmpty()) message else message + ByteArray(8 - message.size % 8)
        var h = dspk.copyOfRange(0, 8)
        for (off in padded.indices step 8) {
            val m = padded.copyOfRange(off, off + 8)
            h = Tdes(h.xorWith(m) + dspk.copyOfRange(8, 16)).encryptBlock(m).xorWith(m)
        }
        return h
    }
}
