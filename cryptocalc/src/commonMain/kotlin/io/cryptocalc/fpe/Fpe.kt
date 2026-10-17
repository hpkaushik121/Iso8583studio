package io.cryptocalc.fpe

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.cryptocalc.crypto.primitives.Aes
import io.cryptocalc.math.BigInt
import io.cryptocalc.math.pmod

/**
 * Format-preserving encryption, NIST SP 800-38G rev. 1: FF1 and FF3-1 (plus the withdrawn FF3
 * with its 64-bit tweak, kept for compatibility with older data). Numeral strings are given
 * in an alphabet; the default alphabets cover radix 10, 26 and 36 as the calculator offers.
 */
object Fpe {

    fun defaultAlphabet(radix: Int): String = when (radix) {
        10 -> "0123456789"
        26 -> "abcdefghijklmnopqrstuvwxyz"
        36 -> "0123456789abcdefghijklmnopqrstuvwxyz"
        16 -> "0123456789abcdef"
        else -> throw IllegalArgumentException("No default alphabet for radix $radix")
    }

    fun toNumerals(text: String, alphabet: String): IntArray =
        IntArray(text.length) { i ->
            val idx = alphabet.indexOf(text[i].lowercaseChar()).takeIf { it >= 0 } ?: alphabet.indexOf(text[i])
            require(idx >= 0) { "Character '${text[i]}' is not in the alphabet" }
            idx
        }

    fun fromNumerals(numerals: IntArray, alphabet: String): String = numerals.map { alphabet[it] }.joinToString("")

    // ------------------------------------------------------------------ helpers

    private fun num(numerals: IntArray, radix: Int): BigInteger {
        var x = BigInt.ZERO
        val r = BigInteger.fromInt(radix)
        for (d in numerals) x = x * r + BigInteger.fromInt(d)
        return x
    }

    private fun str(x: BigInteger, radix: Int, length: Int): IntArray {
        val out = IntArray(length)
        var v = x
        val r = BigInteger.fromInt(radix)
        for (i in length - 1 downTo 0) {
            val (q, rem) = v.divideAndRemainder(r)
            out[i] = rem.intValue(false)
            v = q
        }
        return out
    }

    private fun numBytes(b: ByteArray): BigInteger = BigInt.fromUnsignedBytes(b)

    private fun prf(aes: Aes, data: ByteArray): ByteArray {
        var x = ByteArray(16)
        for (off in data.indices step 16) {
            val blk = ByteArray(16) { (x[it].toInt() xor data[off + it].toInt()).toByte() }
            x = aes.encryptBlock(blk)
        }
        return x
    }

    private fun bytesBE(v: Int, len: Int): ByteArray = ByteArray(len) { i -> (v ushr (8 * (len - 1 - i))).toByte() }

    // ------------------------------------------------------------------ FF1

    /** FF1 encryption of [plaintext] numerals. [tweak] may be empty. */
    fun ff1Encrypt(key: ByteArray, tweak: ByteArray, radix: Int, plaintext: IntArray): IntArray = ff1(key, tweak, radix, plaintext, true)

    fun ff1Decrypt(key: ByteArray, tweak: ByteArray, radix: Int, ciphertext: IntArray): IntArray = ff1(key, tweak, radix, ciphertext, false)

    private fun ff1(key: ByteArray, tweak: ByteArray, radix: Int, input: IntArray, encrypt: Boolean): IntArray {
        require(radix in 2..65536) { "Radix must be 2..65536" }
        val n = input.size
        require(n >= 2) { "Input must have at least 2 numerals" }
        require(BigInteger.fromInt(radix).pow(n) >= BigInteger.fromInt(1_000_000)) { "radix^length must be at least 1,000,000 for FF1" }
        val aes = Aes(key)
        val t = tweak.size
        val u = n / 2
        val v = n - u
        var a = input.copyOfRange(0, u)
        var b = input.copyOfRange(u, n)
        val bBits = kotlin.math.ceil(v * kotlin.math.log2(radix.toDouble())).toInt()
        val bLen = (bBits + 7) / 8
        val d = 4 * ((bLen + 3) / 4) + 4
        val p = byteArrayOf(1, 2, 1) + bytesBE(radix, 3) + byteArrayOf(10, u.toByte()) + bytesBE(n, 4) + bytesBE(t, 4)
        val radixBig = BigInteger.fromInt(radix)
        for (round in 0 until 10) {
            val i = if (encrypt) round else 9 - round
            val padLen = (-t - bLen - 1).mod(16)
            // Encryption feeds B into the round function and updates A; decryption the reverse.
            val src = if (encrypt) b else a
            val dst = if (encrypt) a else b
            val q = tweak + ByteArray(padLen) + byteArrayOf(i.toByte()) + BigInt.toUnsignedBytes(num(src, radix), bLen)
            val r = prf(aes, p + q)
            var s = r
            var j = 1
            while (s.size < d) {
                val blk = ByteArray(16) { (r[it].toInt() xor bytesBE(j, 16)[it].toInt()).toByte() }
                s += aes.encryptBlock(blk)
                j++
            }
            s = s.copyOf(d)
            val y = numBytes(s)
            val m = if (i % 2 == 0) u else v
            val modulus = radixBig.pow(m)
            val c = if (encrypt) (num(dst, radix) + y).pmod(modulus) else (num(dst, radix) - y).pmod(modulus)
            val cNum = str(c, radix, m)
            if (encrypt) { a = b; b = cNum } else { b = a; a = cNum }
        }
        return a + b
    }

    // ------------------------------------------------------------------ FF3-1 / FF3

    private fun rev(x: IntArray): IntArray = x.reversedArray()
    private fun revBytes(x: ByteArray): ByteArray = x.reversedArray()

    /** FF3-1 (56-bit tweak) or, when [tweak] is 8 bytes, the original FF3. */
    fun ff3Encrypt(key: ByteArray, tweak: ByteArray, radix: Int, plaintext: IntArray): IntArray = ff3(key, tweak, radix, plaintext, true)

    fun ff3Decrypt(key: ByteArray, tweak: ByteArray, radix: Int, ciphertext: IntArray): IntArray = ff3(key, tweak, radix, ciphertext, false)

    private fun ff3(key: ByteArray, tweak: ByteArray, radix: Int, input: IntArray, encrypt: Boolean): IntArray {
        require(radix in 2..65536) { "Radix must be 2..65536" }
        val n = input.size
        require(n >= 2) { "Input must have at least 2 numerals" }
        val maxLen = (2 * kotlin.math.floor(kotlin.math.log2(2.0.pow(96)) / kotlin.math.log2(radix.toDouble()))).toInt()
        require(n <= maxLen) { "Input too long for FF3 with radix $radix (max $maxLen numerals)" }
        val (tl, tr) = when (tweak.size) {
            7 -> { // FF3-1: TL = T[0..27] ‖ 0000, TR = T[32..55] ‖ T[28..31] ‖ 0000
                val tl = byteArrayOf(tweak[0], tweak[1], tweak[2], (tweak[3].toInt() and 0xF0).toByte())
                val tr = byteArrayOf(tweak[4], tweak[5], tweak[6], ((tweak[3].toInt() and 0x0F) shl 4).toByte())
                tl to tr
            }
            8 -> tweak.copyOfRange(0, 4) to tweak.copyOfRange(4, 8) // FF3
            else -> throw IllegalArgumentException("Tweak must be 7 bytes (FF3-1) or 8 bytes (FF3)")
        }
        val aes = Aes(revBytes(key))
        val u = (n + 1) / 2
        val v = n - u
        var a = input.copyOfRange(0, u)
        var b = input.copyOfRange(u, n)
        val radixBig = BigInteger.fromInt(radix)
        for (round in 0 until 8) {
            val i = if (encrypt) round else 7 - round
            val (m, w) = if (i % 2 == 0) u to tr else v to tl
            val src = if (encrypt) b else a
            val dst = if (encrypt) a else b
            val p = ByteArray(4) { (w[it].toInt() xor (if (it == 3) i else 0)).toByte() } +
                    BigInt.toUnsignedBytes(num(rev(src), radix), 12)
            val s = revBytes(aes.encryptBlock(revBytes(p)))
            val y = numBytes(s)
            val modulus = radixBig.pow(m)
            val c = if (encrypt) (num(rev(dst), radix) + y).pmod(modulus) else (num(rev(dst), radix) - y).pmod(modulus)
            val cNum = rev(str(c, radix, m))
            if (encrypt) { a = b; b = cNum } else { b = a; a = cNum }
        }
        return a + b
    }

    private fun Double.pow(e: Int): Double { var r = 1.0; repeat(e) { r *= this }; return r }
}
