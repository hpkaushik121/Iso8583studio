package io.cryptocalc.hsm

import ai.cortex.core.CryptoUtils
import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import io.cryptocalc.crypto.primitives.Mac
import io.cryptocalc.crypto.primitives.Tdes

/**
 * Thales payShield "variant" LMK key encryption as described in the payShield 10K manuals:
 * a key is enciphered under an LMK pair modified by a variant byte (0–9) XORed into the first
 * byte, and a per-component scheme variant for double/triple-length keys (schemes U/T).
 */
object ThalesLmk {

    /** The well-known payShield test LMK pairs. */
    val TEST_LMK_PAIRS: Map<String, String> = linkedMapOf(
        "00-01" to "01010101010101017902CD1FD36EF8BA",
        "02-03" to "20202020202020203131313131313131",
        "04-05" to "40404040404040405151515151515151",
        "06-07" to "61616161616161617070707070707070",
        "08-09" to "80808080808080809191919191919191",
        "10-11" to "A1A1A1A1A1A1A1A1B0B0B0B0B0B0B0B0",
        "12-13" to "C1C1010101010101D0D0010101010101",
        "14-15" to "E0E0010101010101F1F1010101010101",
        "16-17" to "0101010101010101F1F1010101010101",
        "18-19" to "3131313131313131F1F1010101010101",
        "20-21" to "4040404040404040F1F1010101010101",
        "22-23" to "61616161616161610101010101010101",
        "24-25" to "80808080808080800101010101010101",
        "26-27" to "91919191919191910101010101010101",
        "28-29" to "A1A1A1A1A1A1A1A10101010101010101",
        "30-31" to "B0B0B0B0B0B0B0B00101010101010101",
        "32-33" to "C1C1C1C1C1C1C1C10101010101010101",
        "34-35" to "D0D0D0D0D0D0D0D00101010101010101",
        "36-37" to "E0E0E0E0E0E0E0E00101010101010101",
        "38-39" to "F1F1F1F1F1F1F1F10101010101010101",
    )

    /** Variant bytes applied to the first byte of the LMK pair for key variants 0–9. */
    val VARIANTS = intArrayOf(0x00, 0xA6, 0x5A, 0x6A, 0xDE, 0x2B, 0x50, 0x74, 0x9C, 0xFA)

    /** Scheme variants for double-length (U) and triple-length (T) keys, applied to the second byte... */
    private val DOUBLE_SCHEME = intArrayOf(0xA6, 0x5A)
    private val TRIPLE_SCHEME = intArrayOf(0x6A, 0xDE, 0x2B)

    enum class Scheme(val tag: Char, val components: Int, val variantBytes: IntArray?) {
        Z('Z', 1, null), U('U', 2, intArrayOf(0xA6, 0x5A)), T('T', 3, intArrayOf(0x6A, 0xDE, 0x2B)),
        X('X', 2, null), Y('Y', 3, null);

        companion object {
            fun of(tag: String): Scheme = entries.first { it.tag == tag.trim().uppercase().first() }
        }
    }

    private fun lmkWithVariant(lmk: ByteArray, variant: Int, schemeVariant: Int?): ByteArray {
        require(lmk.size == 16) { "An LMK pair is 32 hex characters" }
        require(variant in 0..9) { "Variant must be 0..9" }
        val out = lmk.copyOf()
        out[0] = (out[0].toInt() xor VARIANTS[variant]).toByte()
        if (schemeVariant != null) out[8] = (out[8].toInt() xor schemeVariant).toByte()
        return out
    }

    /**
     * Enciphers [key] (8/16/24 bytes) under [lmk] with key [variant] and [scheme]. Each 8-byte
     * component is TDES-ECB enciphered; for U and T the component's scheme variant is XORed
     * into the LMK's right half first.
     */
    fun encryptUnderLmk(key: ByteArray, lmk: ByteArray, variant: Int, scheme: Scheme): ByteArray {
        require(key.size == 8 * scheme.components) { "Scheme ${scheme.tag} expects ${16 * scheme.components} hex characters" }
        val out = ByteArray(key.size)
        for (c in 0 until scheme.components) {
            val sv = scheme.variantBytes?.get(c)
            val cipher = Tdes(lmkWithVariant(lmk, variant, sv))
            cipher.encryptBlock(key, c * 8, out, c * 8)
        }
        return out
    }

    fun decryptUnderLmk(encrypted: ByteArray, lmk: ByteArray, variant: Int, scheme: Scheme): ByteArray {
        require(encrypted.size == 8 * scheme.components) { "Scheme ${scheme.tag} expects ${16 * scheme.components} hex characters" }
        val out = ByteArray(encrypted.size)
        for (c in 0 until scheme.components) {
            val sv = scheme.variantBytes?.get(c)
            val cipher = Tdes(lmkWithVariant(lmk, variant, sv))
            cipher.decryptBlock(encrypted, c * 8, out, c * 8)
        }
        return out
    }

    fun kcv(key: ByteArray): String = Mac.kcvTdes(key).toHexUpper()

    fun hasOddParity(key: ByteArray): Boolean = key.all { (it.toInt() and 0xFF).countOneBits() % 2 == 1 }
    fun hasEvenParity(key: ByteArray): Boolean = key.all { (it.toInt() and 0xFF).countOneBits() % 2 == 0 }

    fun oddParity(key: ByteArray): ByteArray = CryptoUtils.applyParity(key, isOdd = true)

    fun testLmk(pair: String): ByteArray = (TEST_LMK_PAIRS[pair] ?: throw IllegalArgumentException("Unknown LMK pair $pair")).hexToBytes()
}
