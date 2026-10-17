package io.cryptocalc

import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import io.cryptocalc.codec.Codecs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodecsKatTest {

    @Test
    fun base94RoundTripKeepsLeadingZeros() {
        for (hex in listOf("00", "0000FF", "FF", "48656C6C6F", "00000102030405060708090A0B0C0D0E0F", "")) {
            val bytes = hex.hexToBytes()
            val enc = Codecs.Base94.encode(bytes)
            assertContentEquals(bytes, Codecs.Base94.decode(enc), "round trip of $hex via $enc")
        }
        assertEquals("!!", Codecs.Base94.encode("0000".hexToBytes()))
        assertTrue(Codecs.Base94.encode("Hello".encodeToByteArray()).all { it.code in 33..126 })
    }

    @Test
    fun checkDigits() {
        assertEquals(6, Codecs.CheckDigits.luhn("4992739871"))
        assertTrue(Codecs.CheckDigits.luhnValid("49927398716"))
        assertFalse(Codecs.CheckDigits.luhnValid("49927398717"))
        assertEquals('X', Codecs.CheckDigits.mod11("020161622"))      // ISBN 0-201-61622-X
        assertTrue(Codecs.CheckDigits.mod11Valid("020161622X"))
        assertEquals(3, Codecs.CheckDigits.verhoeff("236"))
        assertTrue(Codecs.CheckDigits.verhoeffValid("2363"))
        assertFalse(Codecs.CheckDigits.verhoeffValid("2364"))
        assertEquals(4, Codecs.CheckDigits.damm("572"))
        assertTrue(Codecs.CheckDigits.dammValid("5724"))
        assertTrue(Codecs.CheckDigits.ibanValid("GB82 WEST 1234 5698 7654 32"))
        assertFalse(Codecs.CheckDigits.ibanValid("GB82 WEST 1234 5698 7654 33"))
        assertEquals("82", Codecs.CheckDigits.ibanCheckDigits("GB", "WEST12345698765432"))
        assertTrue(Codecs.CheckDigits.mod97Valid("79444"))           // ISO 7064 MOD 97-10: 794 -> 44
        assertEquals("44", Codecs.CheckDigits.mod97("794"))
    }

    @Test
    fun bcdAndText() {
        assertEquals("0123", Codecs.Bcd.pack("123").toHexUpper())
        assertEquals("123F", Codecs.Bcd.pack("123", padF = true).toHexUpper())
        assertEquals("313233", Codecs.Bcd.unpack("123").toHexUpper())
        assertEquals("1234", Codecs.Bcd.decode("1234".hexToBytes()))
        assertEquals("123", Codecs.Bcd.decode("123F".hexToBytes()))
        assertEquals("C1C2C3", Codecs.Text.asciiToEbcdicHex("ABC"))
        assertEquals("ABC", Codecs.Text.ebcdicHexToAscii("C1C2C3"))
        assertEquals("065066", Codecs.Text.hexToAtmAsciiDecimal("4142"))
        assertEquals("4142", Codecs.Text.atmAsciiDecimalToHex("065066"))
        assertEquals("A5", Codecs.Text.binaryToHex("10100101"))
        assertEquals("0F", Codecs.Text.binaryToHex("1111"))
        assertEquals("10100101", Codecs.Text.hexToBinary("A5"))
    }
}
