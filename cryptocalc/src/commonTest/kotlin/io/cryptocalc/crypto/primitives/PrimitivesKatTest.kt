package io.cryptocalc.crypto.primitives

import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.PaddingMethods
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals

/**
 * Known-answer tests for the pure-Kotlin primitives. These run on the JVM and on Kotlin/Wasm,
 * which is what guarantees the desktop app and the browser tools agree.
 */
class PrimitivesKatTest {

    private fun hex(b: ByteArray) = b.toHexUpper()

    @Test
    fun desFips46_3() {
        // FIPS 46-3 / classic Stallings example
        val des = Des("133457799BBCDFF1".hexToBytes())
        assertEquals("85E813540F0AB405", hex(des.encryptBlock("0123456789ABCDEF".hexToBytes())))
        assertEquals("0123456789ABCDEF", hex(des.decryptBlock("85E813540F0AB405".hexToBytes())))
    }

    @Test
    fun desZeroKeyZeroBlock() {
        val des = Des("0000000000000000".hexToBytes())
        assertEquals("8CA64DE9C1B123A7", hex(des.encryptBlock(ByteArray(8))))
    }

    @Test
    fun tdesTwoKeyMatchesThreeKeyK1K2K1() {
        val k16 = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()
        val k24 = k16 + k16.copyOfRange(0, 8)
        val block = "0123456789ABCDEF".hexToBytes()
        assertContentEquals(Tdes(k16).encryptBlock(block), Tdes(k24).encryptBlock(block))
        assertContentEquals(block, Tdes(k16).decryptBlock(Tdes(k16).encryptBlock(block)))
    }

    @Test
    fun tdesKnownAnswerAndKcv() {
        // Existing engine test vector: DES key 0123456789ABCDEF, ASCII "12345678" -> BD0B1A49070AC376
        val out = Tdes("0123456789ABCDEF".hexToBytes()).encryptBlock("12345678".encodeToByteArray())
        assertEquals("BD0B1A49070AC376", hex(out))
        // NIST SP 800-67 three-key TDES sample
        val k = "0123456789ABCDEF23456789ABCDEF01456789ABCDEF0123".hexToBytes()
        val ct = Tdes(k).encryptBlock("5468652071756663".hexToBytes())
        assertEquals("A826FD8CE53B855F", hex(ct))
        // KCV of the well-known test key 0123...3210
        assertEquals("08D7B4", hex(Mac.kcvTdes("0123456789ABCDEFFEDCBA9876543210".hexToBytes())))
    }

    @Test
    fun aes128Fips197C1() {
        val aes = Aes("000102030405060708090A0B0C0D0E0F".hexToBytes())
        val ct = aes.encryptBlock("00112233445566778899AABBCCDDEEFF".hexToBytes())
        assertEquals("69C4E0D86A7B0430D8CDB78070B4C55A", hex(ct))
        assertEquals("00112233445566778899AABBCCDDEEFF", hex(aes.decryptBlock(ct)))
    }

    @Test
    fun aes192And256Fips197() {
        val pt = "00112233445566778899AABBCCDDEEFF".hexToBytes()
        val aes192 = Aes("000102030405060708090A0B0C0D0E0F1011121314151617".hexToBytes())
        assertEquals("DDA97CA4864CDFE06EAF70A0EC0D7191", hex(aes192.encryptBlock(pt)))
        val aes256 = Aes("000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F".hexToBytes())
        val ct = aes256.encryptBlock(pt)
        assertEquals("8EA2B7CA516745BFEAFC49904B496089", hex(ct))
        assertContentEquals(pt, aes256.decryptBlock(ct))
    }

    @Test
    fun aesCbcSp800_38a() {
        val key = "2B7E151628AED2A6ABF7158809CF4F3C".hexToBytes()
        val iv = "000102030405060708090A0B0C0D0E0F".hexToBytes()
        val pt = ("6BC1BEE22E409F96E93D7E117393172A" + "AE2D8A571E03AC9C9EB76FAC45AF8E51").hexToBytes()
        val ct = Modes.encrypt(Aes(key), pt, CipherMode.CBC, iv)
        assertEquals("7649ABAC8119B246CEE98E9B12E9197D5086CB9B507219EE95DB113A917678B2", hex(ct))
        assertContentEquals(pt, Modes.decrypt(Aes(key), ct, CipherMode.CBC, iv))
        // CFB-128 and OFB from the same NIST appendix
        val cfb = Modes.encrypt(Aes(key), pt, CipherMode.CFB, iv)
        assertEquals("3B3FD92EB72DAD20333449F8E83CFB4AC8A64537A0B3A93FCDE3CDAD9F1CE58B", hex(cfb))
        assertContentEquals(pt, Modes.decrypt(Aes(key), cfb, CipherMode.CFB, iv))
        val ofb = Modes.encrypt(Aes(key), pt, CipherMode.OFB, iv)
        assertEquals("3B3FD92EB72DAD20333449F8E83CFB4A7789508D16918F03F53C52DAC54ED825", hex(ofb))
        assertContentEquals(pt, Modes.decrypt(Aes(key), ofb, CipherMode.OFB, iv))
    }

    @Test
    fun paddingRoundTrips() {
        val data = "0102030405".hexToBytes()
        for (p in listOf(PaddingMethods.PKCS7, PaddingMethods.METHOD_2_ISO_9797, PaddingMethods.ANSI_X923,
            PaddingMethods.METHOD_3_ISO_9797, PaddingMethods.ISO_10126)) {
            val padded = Modes.pad(data, 8, p)
            assertEquals(0, padded.size % 8, "padding $p")
            assertContentEquals(data, Modes.unpad(padded, 8, p), "unpad $p")
        }
        assertEquals("0102030405000000", hex(Modes.pad(data, 8, PaddingMethods.METHOD_1_ISO_9797)))
        assertEquals("0102030405800000", hex(Modes.pad(data, 8, PaddingMethods.METHOD_2_ISO_9797)))
        assertEquals("0102030405060708" + "8000000000000000", hex(Modes.pad("0102030405060708".hexToBytes(), 8, PaddingMethods.METHOD_2_ISO_9797)))
    }

    @Test
    fun aesCmacRfc4493() {
        val cmac = Cmac(Aes("2B7E151628AED2A6ABF7158809CF4F3C".hexToBytes()))
        assertEquals("BB1D6929E95937287FA37D129B756746", hex(cmac.compute(ByteArray(0))))
        assertEquals("070A16B46B4D4144F79BDD9DD04A287C", hex(cmac.compute("6BC1BEE22E409F96E93D7E117393172A".hexToBytes())))
        val m40 = ("6BC1BEE22E409F96E93D7E117393172AAE2D8A571E03AC9C9EB76FAC45AF8E5130C81C46A35CE411").hexToBytes()
        assertEquals("DFA66747DE9AE63030CA32611497C827", hex(cmac.compute(m40)))
        assertEquals("DFA66747DE9AE63030CA3261", hex(cmac.compute(m40, 12)))
    }

    @Test
    fun tdesCmacSp800_38b() {
        // NIST SP 800-38B TDES example (three-key), empty message
        val key = "8AA83BF8CBDA10620BC1BF19FBB6CD58BC313D4A371CA8B5".hexToBytes()
        val cmac = Cmac(Tdes(key))
        assertEquals("B7A688E122FFAF95", hex(cmac.compute(ByteArray(0))))
        assertEquals("8E8F293136283797", hex(cmac.compute("6BC1BEE22E409F96".hexToBytes())))
    }

    @Test
    fun retailMacAndCbcMac() {
        val key = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()
        val data = "0000000000000000".hexToBytes()
        // Algorithm 1 (single DES under KL) of a zero block = DES_KL(0)
        assertEquals(hex(Des(key.copyOfRange(0, 8)).encryptBlock(ByteArray(8))), hex(Mac.ansiX99(key.copyOfRange(0, 8), data)))
        // Retail MAC of a zero block = TDES(KL,KR)(0): single pass then D_KR E_KL
        assertEquals(hex(Tdes(key).encryptBlock(ByteArray(8))), hex(Mac.ansiX919(key, data)))
        // Algorithm 2/5 consistency on two blocks
        val two = "00112233445566778899AABBCCDDEEFF".hexToBytes()
        val a1 = Mac.algorithm1(Des(key.copyOfRange(0, 8)), two)
        assertEquals(hex(Des(key.copyOfRange(8, 16)).encryptBlock(a1)), hex(Mac.algorithm2(Des(key.copyOfRange(0, 8)), Des(key.copyOfRange(8, 16)), two)))
    }

    @Test
    fun digestsAndHmac() {
        assertEquals("A9993E364706816ABA3E25717850C26C9CD0D89D", hex(Digests.sha1("abc".encodeToByteArray())))
        assertEquals("BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD", hex(Digests.sha256("abc".encodeToByteArray())))
        assertEquals("900150983CD24FB0D6963F7D28E17F72", hex(Digests.md5("abc".encodeToByteArray())))
        // RFC 4231 test case 1
        val key = ByteArray(20) { 0x0B }
        assertEquals("B0344C61D8DB38535CA8AFCEAF0BF12B881DC200C9833DA726E9376C2E32CFF7",
            hex(Digests.hmacSha256(key, "Hi There".encodeToByteArray())))
        // RFC 2202 test case 1
        assertEquals("B617318655057264E28BC0B6FB378C8EF146BE00", hex(Digests.hmacSha1(key, "Hi There".encodeToByteArray())))
    }

    @Test
    fun secureRandomProducesDistinctBytes() {
        val a = secureRandomBytes(32)
        val b = secureRandomBytes(32)
        assertEquals(32, a.size)
        assertEquals(false, a.contentEquals(b))
    }
}
