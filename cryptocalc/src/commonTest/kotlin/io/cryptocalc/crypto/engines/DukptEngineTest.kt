package io.cryptocalc.crypto.engines

import ai.cortex.core.CryptoUtils
import ai.cortex.core.IsoUtil
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DukptEngineTest {

    private val bdk16 = IsoUtil.hexStringToBytes("0123456789ABCDEFFEDCBA9876543210")
    private val bdk24 = IsoUtil.hexStringToBytes("0123456789ABCDEFFEDCBA98765432100011223344556677")
    private val ksn = IsoUtil.hexStringToBytes("FFFF9876543210E00001")

    private fun hex(b: ByteArray) = IsoUtil.bytesToHexString(b).uppercase()

    @Test
    fun `double-length BDK matches ANSI X9_24 test vector`() {
        val ipek = DukptEngine.deriveIpek(bdk16, ksn)
        assertEquals("6AC292FAA1315B4D858AB3A3D7D5933A", hex(ipek))
        val session = DukptEngine.deriveSessionKey(ipek, ksn)
        assertEquals("042666B49184CFA368DE9628D0397BC9", hex(session))
    }

    @Test
    fun `triple-length BDK of form K1-K2-K1 derives the same IPEK as the double-length BDK`() {
        val k1k2k1 = bdk16 + bdk16.copyOfRange(0, 8)
        assertContentEquals(DukptEngine.deriveIpek(bdk16, ksn), DukptEngine.deriveIpek(k1k2k1, ksn))
    }

    @Test
    fun `triple-length BDK derives a 16-byte IPEK and session key`() {
        val ipek = DukptEngine.deriveIpek(bdk24, ksn)
        assertEquals(16, ipek.size)
        val session = DukptEngine.deriveSessionKey(ipek, ksn)
        assertEquals(16, session.size)
        // Reference values computed independently (pycryptodome)
        assertEquals("A602C6A66B5430108C23440A59BBD437", hex(ipek))
        assertEquals("1A26AD4C5A2FADD5FBC7167BF59CE855", hex(session))
    }

    @Test
    fun `unsupported BDK lengths are rejected`() {
        assertFailsWith<IllegalArgumentException> { DukptEngine.deriveIpek(ByteArray(8), ksn) }
        assertFailsWith<IllegalArgumentException> { DukptEngine.deriveIpek(ByteArray(32), ksn) }
    }

    @Test
    fun `variantForKeySize repeats the 8-byte pattern`() {
        assertEquals("00000000000000FF".repeat(3), hex(DukptEngine.variantForKeySize(DukptEngine.PEK_VARIANT, 24)))
        assertEquals("000000000000FF00".repeat(2), hex(DukptEngine.variantForKeySize(DukptEngine.MAC_VARIANT, 16)))
        assertContentEquals(DukptEngine.DATA_VARIANT, DukptEngine.variantForKeySize(DukptEngine.DATA_VARIANT, 16))
    }

    @Test
    fun `one-way function handles 16 and 24-byte variant keys`() {
        val vk16 = DukptEngine.xorBytes(bdk16, DukptEngine.DATA_VARIANT)
        val out16 = DukptEngine.applyVariantEncryption(vk16)
        assertEquals(16, out16.size)
        // K1-K2-K1 input must produce out16 + out16[0..8)
        val out24Degenerate = DukptEngine.applyVariantEncryption(vk16 + vk16.copyOfRange(0, 8))
        assertContentEquals(out16 + out16.copyOfRange(0, 8), out24Degenerate)

        val vk24 = DukptEngine.xorBytes(bdk24, DukptEngine.variantForKeySize(DukptEngine.DATA_VARIANT, 24))
        assertEquals("012345678954CDEFFEDCBA9876AB32100011223344AA6677", hex(vk24))
        assertEquals("7DF6E5D1F0198068B98D7F003D71152F4A10A8ADE0496893", hex(DukptEngine.applyVariantEncryption(vk24)))
    }

    @Test
    fun `applyParity forces odd parity on every byte of a 24-byte key`() {
        val odd = CryptoUtils.applyParity(bdk24, isOdd = true)
        assertEquals(24, odd.size)
        assertTrue(odd.all { (it.toInt() and 0xFF).countOneBits() % 2 == 1 })
        val even = CryptoUtils.applyParity(bdk24, isOdd = false)
        assertTrue(even.all { (it.toInt() and 0xFF).countOneBits() % 2 == 0 })
        // Only the low bit of each byte may differ from the input
        odd.zip(bdk24.toList()).forEach { (a, b) -> assertEquals(0, (a.toInt() xor b.toInt()) and 0xFE) }
    }
}
