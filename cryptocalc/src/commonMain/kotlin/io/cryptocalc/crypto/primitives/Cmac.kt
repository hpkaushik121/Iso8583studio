package io.cryptocalc.crypto.primitives

/**
 * CMAC (NIST SP 800-38B / RFC 4493) over any [BlockCipher]. With [Aes] this is AES-CMAC;
 * with [Tdes] it is the 64-bit-block variant (subkey constant 0x1B) used by TR-31.
 */
class Cmac(private val cipher: BlockCipher) {
    private val bs = cipher.blockSize
    private val k1: ByteArray
    private val k2: ByteArray

    init {
        val l = cipher.encryptBlock(ByteArray(bs))
        k1 = doubleKey(l)
        k2 = doubleKey(k1)
    }

    /** Full-length MAC ([BlockCipher.blockSize] bytes). Use [compute] with `macLength` to truncate. */
    fun compute(data: ByteArray): ByteArray {
        val n = if (data.isEmpty()) 1 else (data.size + bs - 1) / bs
        val lastComplete = data.isNotEmpty() && data.size % bs == 0
        val x = ByteArray(bs)
        val y = ByteArray(bs)
        for (i in 0 until n - 1) {
            for (j in 0 until bs) y[j] = (x[j].toInt() xor data[i * bs + j].toInt()).toByte()
            cipher.encryptBlock(y, 0, x, 0)
        }
        val last = ByteArray(bs)
        val start = (n - 1) * bs
        if (lastComplete) {
            for (j in 0 until bs) last[j] = (data[start + j].toInt() xor k1[j].toInt()).toByte()
        } else {
            val rem = data.size - start
            for (j in 0 until rem) last[j] = data[start + j]
            last[rem] = 0x80.toByte()
            for (j in 0 until bs) last[j] = (last[j].toInt() xor k2[j].toInt()).toByte()
        }
        for (j in 0 until bs) y[j] = (x[j].toInt() xor last[j].toInt()).toByte()
        val out = ByteArray(bs)
        cipher.encryptBlock(y, 0, out, 0)
        return out
    }

    /** Truncated MAC: the leftmost [macLength] bytes (e.g. 12 for AES-CMAC-96, 8 for TR-31 AES). */
    fun compute(data: ByteArray, macLength: Int): ByteArray {
        require(macLength in 1..bs) { "MAC length must be 1..$bs bytes" }
        return compute(data).copyOf(macLength)
    }

    private fun doubleKey(input: ByteArray): ByteArray {
        val out = ByteArray(bs)
        var carry = 0
        for (i in bs - 1 downTo 0) {
            val v = input[i].toInt() and 0xFF
            out[i] = ((v shl 1) or carry).toByte()
            carry = (v ushr 7) and 1
        }
        if (carry == 1) {
            val rb = if (bs == 16) 0x87 else 0x1B
            out[bs - 1] = (out[bs - 1].toInt() xor rb).toByte()
        }
        return out
    }
}
