package io.cryptocalc

import ai.cortex.core.Ebcdic
import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import io.cryptocalc.asn1.Pkcs
import io.cryptocalc.crypto.primitives.ExtraDigests
import io.cryptocalc.hsm.ThalesRsaEncoding
import io.cryptocalc.hsm.Tr31KeyBlock
import io.cryptocalc.math.BigInt
import io.cryptocalc.rsa.RsaPublicKey
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyManagementKatTest {

    @Test
    fun md4Rfc1320Vectors() {
        assertEquals("31D6CFE0D16AE931B73C59D7E0C089C0", ExtraDigests.md4(ByteArray(0)).toHexUpper())
        assertEquals("A448017AAF21D8525FC10AE87AA6729D", ExtraDigests.md4("abc".encodeToByteArray()).toHexUpper())
        assertEquals("E33B4DDC9C38F2199C3E7B164FCC0536", ExtraDigests.md4("12345678901234567890123456789012345678901234567890123456789012345678901234567890".encodeToByteArray()).toHexUpper())
    }

    @Test
    fun crc32CheckValues() {
        assertEquals("CBF43926", ExtraDigests.crc32("123456789".encodeToByteArray()).toHexUpper())
        assertEquals("00000000", ExtraDigests.crc32(ByteArray(0)).toHexUpper())
        // RFC 1510 §6.4.2: "foo" -> 33 bc 32 73, "test0123456789" -> d6 88 3e b8
        assertEquals("33BC3273", ExtraDigests.crc32Rfc1510("foo".encodeToByteArray()).toHexUpper())
        assertEquals("D6883EB8", ExtraDigests.crc32Rfc1510("test0123456789".encodeToByteArray()).toHexUpper())
    }

    @Test
    fun ebcdicCp037() {
        assertEquals("C1C2C3F1F2F340", Ebcdic.encode("ABC123 ").toHexUpper())
        assertEquals("abc", Ebcdic.decode("818283".hexToBytes()))
        assertEquals("Hello", Ebcdic.decode(Ebcdic.encode("Hello")))
    }

    @Test
    fun tr31VersionBUnwrapsPsecBlockAndRoundTrips() {
        // Block produced by the independent psec library (Python) for this KBPK and key.
        val kbpk = "2F67D82B0E6B27B2B9F2AC2CD3A092B6".hexToBytes()
        val key = "09B4CF4B25FBC5A4C8A06CB55A4C9A69".hexToBytes()
        val psecBlock = "B0112B0AN00N0000BB1691840E22194507674397D201033389151A24C1D6945C042123BF912DA237BFFB769A4D526FEDA61150FEE424531D"
        val u = Tr31KeyBlock.unwrap(kbpk, psecBlock)
        assertTrue(u.macValid, "MAC of the psec block must verify")
        assertContentEquals(key, u.key)
        assertEquals("B0", u.keyUsage)
        // psec masks the key length with extra padding (112 chars); this engine uses the minimum (80).
        val wrapped = Tr31KeyBlock.wrap(kbpk, "B0000B0AN00N0000", key)
        assertEquals("B0080B0AN00N0000", wrapped.header)
        assertEquals(80, wrapped.keyBlock.length)
        assertContentEquals(key, Tr31KeyBlock.unwrap(kbpk, wrapped.keyBlock).key)
        // A tampered block must fail the MAC.
        val tampered = wrapped.keyBlock.substring(0, 20) + (if (wrapped.keyBlock[20] == '0') "1" else "0") + wrapped.keyBlock.substring(21)
        assertFalse(Tr31KeyBlock.unwrap(kbpk, tampered).macValid)
    }

    @Test
    fun tr31VersionDUnwrapsPsecBlockAndRoundTrips() {
        val kbpk = "88E1AB2A2E3DD38C1FA039A536500CC8A87AB9D62DC92C01058FA79F44657DE6".hexToBytes()
        val key = "3F419E1CB7079442AA37474C2EFBF8B8".hexToBytes()
        val psecBlock = "D0112P0TE00N0000C89816AAAB336AD0ECC8252E44F6C941555DC0E65DE28B906B3C9833CFBC9D10C37C85FA4E902150E93839F4961A42D0"
        val u = Tr31KeyBlock.unwrap(kbpk, psecBlock, aes = true)
        assertTrue(u.macValid)
        assertContentEquals(key, u.key)
        val wrapped = Tr31KeyBlock.wrap(kbpk, "D0000P0TE00N0000", key, aes = true)
        assertEquals("D0112P0TE00N0000", wrapped.header)  // 2+16 -> 32-byte payload + 16-byte MAC
        assertContentEquals(key, Tr31KeyBlock.unwrap(kbpk, wrapped.keyBlock).key)
    }

    @Test
    fun thalesRsaEncodings() {
        // Modulus with the top bit set: encoding 02 carries a leading 00, encoding 01 does not.
        val key = RsaPublicKey(BigInt.fromHex("C0FFEE"), BigInt.fromHex("010001"))
        assertEquals("300B020400C0FFEE0203010001", ThalesRsaEncoding.rsaPublicKeyDer(key, unsigned = false).toHexUpper())
        assertEquals("300A0203C0FFEE0203010001", ThalesRsaEncoding.rsaPublicKeyDer(key, unsigned = true).toHexUpper())
        val parsed = Pkcs.parseRsaPublicKey(ThalesRsaEncoding.rsaPublicKeyDer(key, unsigned = false))
        assertEquals("C0FFEE", BigInt.toHex(parsed.n))
    }
}
