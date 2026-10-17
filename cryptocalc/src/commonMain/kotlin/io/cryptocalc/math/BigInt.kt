package io.cryptocalc.math

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import io.cryptocalc.crypto.primitives.secureRandomBytes

/**
 * Big-integer helpers on top of the multiplatform `bignum` library: unsigned byte/hex
 * conversions, modular exponentiation and probable-prime testing, which the library lacks.
 */
object BigInt {
    val ZERO: BigInteger = BigInteger.ZERO
    val ONE: BigInteger = BigInteger.ONE
    val TWO: BigInteger = BigInteger.TWO

    fun fromHex(hex: String): BigInteger {
        // Leading zeros are stripped first: bignum's parser mis-sizes values that start with them.
        val clean = hex.filterNot { it.isWhitespace() }.trimStart('0')
        if (clean.isEmpty()) return ZERO
        return BigInteger.parseString(clean, 16)
    }

    fun fromDecimal(dec: String): BigInteger = BigInteger.parseString(dec.trim(), 10)

    /** Unsigned big-endian bytes to a non-negative integer. */
    fun fromUnsignedBytes(bytes: ByteArray): BigInteger {
        if (bytes.isEmpty()) return ZERO
        var start = 0
        while (start < bytes.size - 1 && bytes[start] == 0.toByte()) start++
        val trimmed = if (start == 0) bytes else bytes.copyOfRange(start, bytes.size)
        if (trimmed.size == 1 && trimmed[0] == 0.toByte()) return ZERO
        return BigInteger.fromByteArray(trimmed, Sign.POSITIVE)
    }

    /** Unsigned big-endian bytes, left-padded (or required to fit) to [length] when given. */
    fun toUnsignedBytes(value: BigInteger, length: Int? = null): ByteArray {
        require(value.signum() >= 0) { "Value must be non-negative" }
        var bytes = if (value.isZero()) ByteArray(1) else value.toByteArray()
        // bignum's toByteArray() is unsigned magnitude; strip any leading zero it may include.
        var start = 0
        while (start < bytes.size - 1 && bytes[start] == 0.toByte()) start++
        bytes = bytes.copyOfRange(start, bytes.size)
        if (length == null) return bytes
        require(bytes.size <= length) { "Value does not fit in $length bytes" }
        return ByteArray(length - bytes.size) + bytes
    }

    fun toHex(value: BigInteger, minDigits: Int = 0): String {
        val hex = value.toString(16).uppercase()
        return if (hex.length >= minDigits) hex else hex.padStart(minDigits, '0')
    }

    fun toHexEven(value: BigInteger): String {
        val hex = toHex(value)
        return if (hex.length % 2 == 0) hex else "0$hex"
    }

    /** Non-negative remainder, whatever the sign of [a]. */
    fun pmod(a: BigInteger, m: BigInteger): BigInteger {
        val r = a.rem(m)
        return if (r.signum() < 0) r + m else r
    }

    /** base^exp mod m by square-and-multiply (right to left, no bit indexing beyond the word count). */
    fun modPow(base: BigInteger, exp: BigInteger, m: BigInteger): BigInteger {
        require(m.signum() > 0) { "Modulus must be positive" }
        require(exp.signum() >= 0) { "Exponent must be non-negative" }
        if (m == ONE) return ZERO
        var result = ONE
        var b = pmod(base, m)
        var e = exp
        while (e.signum() > 0) {
            if (e.bitAt(0)) result = pmod(result * b, m)
            e = e.shr(1)
            if (e.signum() > 0) b = pmod(b * b, m)
        }
        return result
    }

    fun modInverse(a: BigInteger, m: BigInteger): BigInteger = pmod(a, m).modInverse(m)

    /** Uniform random integer in [0, 2^bits). */
    fun randomBits(bits: Int): BigInteger {
        require(bits > 0)
        val bytes = secureRandomBytes((bits + 7) / 8)
        val excess = bytes.size * 8 - bits
        if (excess > 0) bytes[0] = (bytes[0].toInt() and (0xFF ushr excess)).toByte()
        return fromUnsignedBytes(bytes)
    }

    /** Uniform random integer in [1, max). */
    fun randomBelow(max: BigInteger): BigInteger {
        val bits = max.bitLength()
        while (true) {
            val r = randomBits(bits)
            if (r.signum() > 0 && r < max) return r
        }
    }

    private val SMALL_PRIMES = intArrayOf(3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97,
        101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229,
        233, 239, 241, 251)

    /** Miller–Rabin with [rounds] random bases after trial division. */
    fun isProbablePrime(n: BigInteger, rounds: Int = 20): Boolean {
        if (n < TWO) return false
        if (n == TWO) return true
        if (!n.bitAt(0)) return false
        for (p in SMALL_PRIMES) {
            val bp = BigInteger.fromInt(p)
            if (n == bp) return true
            if (pmod(n, bp).isZero()) return false
        }
        val nMinus1 = n - ONE
        var d = nMinus1
        var r = 0
        while (!d.bitAt(0)) { d = d.shr(1); r++ }
        repeat(rounds) {
            val a = TWO + randomBelow(n - BigInteger.fromInt(3))
            var x = modPow(a, d, n)
            if (x == ONE || x == nMinus1) return@repeat
            var composite = true
            for (i in 1 until r) {
                x = pmod(x * x, n)
                if (x == nMinus1) { composite = false; break }
            }
            if (composite) return false
        }
        return true
    }

    /** Random probable prime of exactly [bits] bits (top two bits set so products reach the full size). */
    fun randomPrime(bits: Int): BigInteger {
        require(bits >= 16)
        // bignum uses 63-bit limbs, so bit positions near the top are set with shifts, not setBitAt.
        val topBits = ONE.shl(bits - 1) or ONE.shl(bits - 2)
        while (true) {
            val candidate = randomBits(bits) or topBits or ONE
            if (isProbablePrime(candidate, 12)) return candidate
        }
    }

    fun gcd(a: BigInteger, b: BigInteger): BigInteger = a.gcd(b)
}

/** Non-negative remainder; `bignum`'s own `mod` follows the dividend's sign. */
infix fun BigInteger.pmod(m: BigInteger): BigInteger = BigInt.pmod(this, m)
