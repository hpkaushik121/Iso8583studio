package io.cryptocalc.codec

import ai.cortex.core.Ebcdic
import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import com.ionspin.kotlin.bignum.integer.BigInteger
import io.cryptocalc.math.BigInt

/** Data converters used by the utility tools: Base94, check digits, BCD and character encodings. */
object Codecs {

    /** Reversible radix-94 encoding of a byte string: big-endian integer in base 94, each leading zero byte becomes a leading '!' (digit 0). */
    object Base94 {
        const val ALPHABET = "!\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
        private val NINETY_FOUR = BigInteger.fromInt(94)

        fun encode(bytes: ByteArray): String {
            val leadingZeros = bytes.takeWhile { it.toInt() == 0 }.size
            var v = BigInt.fromUnsignedBytes(bytes)
            val sb = StringBuilder()
            while (v.signum() > 0) {
                val (q, r) = v.divideAndRemainder(NINETY_FOUR)
                sb.append(ALPHABET[r.intValue(false)])
                v = q
            }
            return "!".repeat(leadingZeros) + sb.reverse().toString()
        }

        fun decode(text: String): ByteArray {
            require(text.all { it in ALPHABET }) { "Not a Base94 string (only printable ASCII 0x21-0x7E)" }
            val leadingZeros = text.takeWhile { it == '!' }.length
            var v = BigInt.ZERO
            for (c in text) v = v * NINETY_FOUR + BigInteger.fromInt(ALPHABET.indexOf(c))
            val body = if (v.signum() == 0) ByteArray(0) else BigInt.toUnsignedBytes(v)
            return ByteArray(leadingZeros) + body
        }
    }

    object CheckDigits {
        private fun digits(s: String): List<Int> {
            require(s.isNotEmpty() && s.all { it.isDigit() }) { "Input must be decimal digits" }
            return s.map { it - '0' }
        }

        /** Luhn (ISO/IEC 7812-1) check digit for [number] without its check digit. */
        fun luhn(number: String): Int {
            val d = digits(number).reversed()
            val sum = d.mapIndexed { i, x -> if (i % 2 == 0) (x * 2).let { if (it > 9) it - 9 else it } else x }.sum()
            return (10 - sum % 10) % 10
        }
        fun luhnValid(number: String): Boolean = number.length >= 2 && luhn(number.dropLast(1)) == number.last() - '0'

        /** Digital-root style modulo 9 check used for Amex SE numbers in this tool: 9 − (sum of digits mod 9), 9 → 0. */
        fun mod9(number: String): Int = (9 - digits(number).sum() % 9).let { if (it == 9) 0 else it }
        fun mod9Valid(number: String): Boolean = number.length >= 2 && mod9(number.dropLast(1)) == number.last() - '0'

        /** ISO 7064 MOD 11-2 as used by ISBN-10: weights 10..2, result 0-9 or 'X'. */
        fun mod11(number: String): Char {
            val d = digits(number)
            require(d.size <= 9) { "Mod 11 (ISBN-10) takes up to 9 digits" }
            val sum = d.mapIndexed { i, x -> x * (d.size + 1 - i) }.sum()
            val check = (11 - sum % 11) % 11
            return if (check == 10) 'X' else ('0' + check)
        }
        fun mod11Valid(number: String): Boolean {
            if (number.length < 2) return false
            val body = number.dropLast(1); val last = number.last().uppercaseChar()
            return body.all { it.isDigit() } && mod11(body) == last
        }

        /** ISO 7064 MOD 97-10 check characters for a numeric string (letters are expanded A=10..Z=35). */
        fun mod97(input: String): String {
            val expanded = expandAlnum(input)
            val remainder = modulo97(expanded + "00")
            return (98 - remainder).toString().padStart(2, '0')
        }
        fun mod97Valid(input: String): Boolean = modulo97(expandAlnum(input)) == 1

        /** IBAN check digits for [countryCode] and the BBAN; the IBAN is CC ‖ check ‖ BBAN. */
        fun ibanCheckDigits(countryCode: String, bban: String): String = mod97(bban + countryCode.uppercase() + "00").let { it } .let { _ ->
            val remainder = modulo97(expandAlnum(bban + countryCode.uppercase() + "00"))
            (98 - remainder).toString().padStart(2, '0')
        }
        fun ibanValid(iban: String): Boolean {
            val s = iban.filter { !it.isWhitespace() }.uppercase()
            if (s.length < 5) return false
            return modulo97(expandAlnum(s.substring(4) + s.substring(0, 4))) == 1
        }

        private fun expandAlnum(s: String): String = buildString { for (c in s.uppercase()) { require(c.isLetterOrDigit()) { "Only letters and digits are allowed" }; append(if (c.isDigit()) c.toString() else (c - 'A' + 10).toString()) } }
        private fun modulo97(numeric: String): Int { var r = 0; for (c in numeric) r = (r * 10 + (c - '0')) % 97; return r }

        private val VERHOEFF_D = arrayOf(
            intArrayOf(0,1,2,3,4,5,6,7,8,9), intArrayOf(1,2,3,4,0,6,7,8,9,5), intArrayOf(2,3,4,0,1,7,8,9,5,6), intArrayOf(3,4,0,1,2,8,9,5,6,7), intArrayOf(4,0,1,2,3,9,5,6,7,8),
            intArrayOf(5,9,8,7,6,0,4,3,2,1), intArrayOf(6,5,9,8,7,1,0,4,3,2), intArrayOf(7,6,5,9,8,2,1,0,4,3), intArrayOf(8,7,6,5,9,3,2,1,0,4), intArrayOf(9,8,7,6,5,4,3,2,1,0))
        private val VERHOEFF_P = arrayOf(
            intArrayOf(0,1,2,3,4,5,6,7,8,9), intArrayOf(1,5,7,6,2,8,3,0,9,4), intArrayOf(5,8,0,3,7,9,6,1,4,2), intArrayOf(8,9,1,6,0,4,3,5,2,7),
            intArrayOf(9,4,5,3,1,2,6,8,7,0), intArrayOf(4,2,8,6,5,7,3,9,0,1), intArrayOf(2,7,9,3,8,0,6,4,1,5), intArrayOf(7,0,4,6,9,1,3,2,5,8))
        private val VERHOEFF_INV = intArrayOf(0,4,3,2,1,5,6,7,8,9)

        fun verhoeff(number: String): Int {
            var c = 0
            val rev = digits(number).reversed()
            for ((i, x) in rev.withIndex()) c = VERHOEFF_D[c][VERHOEFF_P[(i + 1) % 8][x]]
            return VERHOEFF_INV[c]
        }
        fun verhoeffValid(number: String): Boolean {
            if (number.length < 2) return false
            var c = 0
            val rev = digits(number).reversed()
            for ((i, x) in rev.withIndex()) c = VERHOEFF_D[c][VERHOEFF_P[i % 8][x]]
            return c == 0
        }

        private val DAMM = arrayOf(
            intArrayOf(0,3,1,7,5,9,8,6,4,2), intArrayOf(7,0,9,2,1,5,4,8,6,3), intArrayOf(4,2,0,6,8,7,1,3,5,9), intArrayOf(1,7,5,0,9,8,3,4,2,6), intArrayOf(6,1,2,3,0,4,5,9,7,8),
            intArrayOf(3,6,7,4,2,0,9,5,8,1), intArrayOf(5,8,6,9,7,2,0,1,3,4), intArrayOf(8,9,4,5,3,6,2,0,1,7), intArrayOf(9,4,3,8,6,1,7,2,0,5), intArrayOf(2,5,8,1,4,3,6,7,9,0))

        fun damm(number: String): Int { var interim = 0; for (x in digits(number)) interim = DAMM[interim][x]; return interim }
        fun dammValid(number: String): Boolean = number.length >= 2 && damm(number) == 0
    }

    object Bcd {
        /** Packed BCD: two digits per byte; an odd count is padded on the left with 0 or, when [padF], on the right with F. */
        fun pack(digits: String, padF: Boolean = false): ByteArray {
            require(digits.isNotEmpty() && digits.all { it.isDigit() }) { "Input must be decimal digits" }
            val s = if (digits.length % 2 == 0) digits else if (padF) digits + "F" else "0$digits"
            return s.hexToBytes()
        }
        /** Unpacked (zoned) BCD: one digit per byte, 0x30 + digit (ASCII) or 0x0n when [zoned] is false. */
        fun unpack(digits: String, zoned: Boolean = true): ByteArray {
            require(digits.isNotEmpty() && digits.all { it.isDigit() }) { "Input must be decimal digits" }
            return ByteArray(digits.length) { ((if (zoned) 0x30 else 0) + (digits[it] - '0')).toByte() }
        }
        /** Digits of packed BCD; F nibbles are accepted as padding, anything else above 9 is an error. */
        fun decode(packed: ByteArray): String = buildString {
            for (b in packed) for (n in intArrayOf((b.toInt() shr 4) and 0x0F, b.toInt() and 0x0F)) {
                when { n <= 9 -> append('0' + n); n == 0x0F -> {}; else -> throw IllegalArgumentException("Nibble ${n.toString(16).uppercase()} is not a BCD digit") }
            }
        }
    }

    object Text {
        fun binaryToHex(binary: String): String {
            val b = binary.filter { !it.isWhitespace() }
            require(b.isNotEmpty() && b.all { it == '0' || it == '1' }) { "Binary input must be 0/1" }
            val padded = b.padStart((b.length + 7) / 8 * 8, '0')
            return padded.chunked(8).joinToString("") { it.toInt(2).toString(16).padStart(2, '0') }.uppercase()
        }
        fun hexToBinary(hex: String): String = hex.hexToBytes().joinToString(" ") { (it.toInt() and 0xFF).toString(2).padStart(8, '0') }
        fun asciiToEbcdicHex(text: String): String = Ebcdic.encode(text).toHexUpper()
        fun ebcdicHexToAscii(hex: String): String = Ebcdic.decode(hex.hexToBytes())
        fun asciiToHex(text: String): String = text.encodeToByteArray().toHexUpper()
        fun hexToAscii(hex: String): String = hex.hexToBytes().decodeToString()
        /** ATM "ASCII decimal": each byte written as three decimal digits (e.g. 065 for 'A'). */
        fun hexToAtmAsciiDecimal(hex: String): String = hex.hexToBytes().joinToString("") { (it.toInt() and 0xFF).toString().padStart(3, '0') }
        fun atmAsciiDecimalToHex(decimal: String): String {
            val d = decimal.filter { it.isDigit() }
            require(d.isNotEmpty() && d.length % 3 == 0) { "ATM ASCII decimal is groups of 3 digits per byte" }
            return ByteArray(d.length / 3) { d.substring(3 * it, 3 * it + 3).toInt().also { v -> require(v <= 255) { "Byte value $v is out of range" } }.toByte() }.toHexUpper()
        }
        fun utf16BeHex(text: String): String = ByteArray(text.length * 2).also { for (i in text.indices) { it[2 * i] = (text[i].code shr 8).toByte(); it[2 * i + 1] = text[i].code.toByte() } }.toHexUpper()
        fun latin1Hex(text: String): String = ByteArray(text.length) { text[it].code.toByte() }.toHexUpper()
    }
}
