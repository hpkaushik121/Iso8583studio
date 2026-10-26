package io.cryptocalc.mac

import io.cryptocalc.crypto.primitives.Des

/**
 * MDC-2 and MDC-4 (ISO/IEC 10118-2), the DES-based Modification Detection Codes, plus the
 * single-length MDC-1 variant. Input must already be padded to a multiple of 8 bytes.
 */
object MdcHash {

    enum class Variant { MDC1, MDC2, MDC4 }

    private val IV_A = byteArrayOf(0x52, 0x52, 0x52, 0x52, 0x52, 0x52, 0x52, 0x52)
    private val IV_B = byteArrayOf(0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25)

    /**
     * Key preparation of ISO 10118-2: the second bit of the first byte is forced to 1 for the
     * A chain and 0 for the B chain, and odd parity is kept; the "modified" form additionally
     * fixes the key's low-order bits as some IBM implementations do.
     */
    private fun prepareKey(block: ByteArray, chainA: Boolean, modified: Boolean): ByteArray {
        val k = block.copyOf()
        val b0 = k[0].toInt() and 0xFF
        k[0] = (if (chainA) (b0 and 0x9F) or 0x40 else (b0 and 0x9F) or 0x20).toByte()
        if (modified) {
            // Force odd parity on every byte so the schedule matches hardware MDC units.
            for (i in k.indices) {
                val v = k[i].toInt() and 0xFE
                k[i] = (if (Integer_bitCount(v) % 2 == 0) v or 1 else v).toByte()
            }
        }
        return k
    }

    private fun Integer_bitCount(v: Int): Int = (v and 0xFF).countOneBits()

    private fun step(g: ByteArray, h: ByteArray, m: ByteArray, modified: Boolean): Pair<ByteArray, ByteArray> {
        val a = Des(prepareKey(g, true, modified)).encryptBlock(m)
        val b = Des(prepareKey(h, false, modified)).encryptBlock(m)
        val aX = ByteArray(8) { (a[it].toInt() xor m[it].toInt()).toByte() }
        val bX = ByteArray(8) { (b[it].toInt() xor m[it].toInt()).toByte() }
        // Swap the right halves
        val g2 = aX.copyOfRange(0, 4) + bX.copyOfRange(4, 8)
        val h2 = bX.copyOfRange(0, 4) + aX.copyOfRange(4, 8)
        return g2 to h2
    }

    /** Computes the MDC over [data] (length must be a multiple of 8 bytes). MDC-1 returns 8 bytes, the others 16. */
    fun compute(data: ByteArray, variant: Variant, modified: Boolean = false): ByteArray {
        require(data.isNotEmpty() && data.size % 8 == 0) { "MDC input must be a non-empty multiple of 8 bytes" }
        var g = IV_A.copyOf()
        var h = IV_B.copyOf()
        for (off in data.indices step 8) {
            val m = data.copyOfRange(off, off + 8)
            val (g2, h2) = step(g, h, m, modified)
            if (variant == Variant.MDC4) {
                // Second round of ISO 10118-2 MDC-4: the new chain keys encrypt the previous
                // chain values crosswise, then the right halves are swapped again.
                val prevG = g
                val prevH = h
                val a = Des(prepareKey(g2, true, modified)).encryptBlock(prevH)
                val b = Des(prepareKey(h2, false, modified)).encryptBlock(prevG)
                val aX = ByteArray(8) { (a[it].toInt() xor prevH[it].toInt()).toByte() }
                val bX = ByteArray(8) { (b[it].toInt() xor prevG[it].toInt()).toByte() }
                g = aX.copyOfRange(0, 4) + bX.copyOfRange(4, 8)
                h = bX.copyOfRange(0, 4) + aX.copyOfRange(4, 8)
            } else {
                g = g2; h = h2
            }
        }
        return if (variant == Variant.MDC1) g else g + h
    }
}
