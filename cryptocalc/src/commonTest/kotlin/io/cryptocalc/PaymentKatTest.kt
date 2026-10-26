package io.cryptocalc

import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import io.cryptocalc.card.CardVerification
import io.cryptocalc.crypto.primitives.Des
import io.cryptocalc.crypto.primitives.Mac
import io.cryptocalc.emv.EmvAc
import io.cryptocalc.emv.EmvKeys
import io.cryptocalc.mac.MdcHash
import io.cryptocalc.pin.PinCrypto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Known answers for the payment-level engines. Vectors come from the EMV books and the
 * pyemv reference library's worked examples (pyemv-master/ in the repository).
 */
class PaymentKatTest {

    private val mk = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()

    @Test
    fun udkOptionA() {
        assertEquals("67F8292358083E5EA7AB7FDA58D53B6B", EmvKeys.deriveUdkOptionA(mk, "99012345678901234", "45").toHexUpper())
        assertEquals("73AD54688CEF2934B0979857E3C719F1", EmvKeys.deriveUdkOptionA(mk, "12345678901234567", "01").toHexUpper())
    }

    @Test
    fun udkOptionB() {
        assertEquals("AD406D7F6D7570916D75E5DCAB8CF737", EmvKeys.deriveUdkOptionB(mk, "12345678901234567", "01").toHexUpper())
    }

    @Test
    fun commonSessionKey() {
        assertEquals("E9FB384AF807B940FEDCEA613461B0C4", EmvKeys.deriveCommonSessionKey(mk, "001C".hexToBytes(), parity = true).toHexUpper())
    }

    @Test
    fun iso9797Algorithm3() {
        val k1 = "022551C4FDF76E45".hexToBytes()
        val k2 = "988089BA31DC077C".hexToBytes()
        val data = "3C00000103A4A0820000000010000000000000000710000000000007101302050030901B6A".hexToBytes()
        // pyemv computes this value for its own worked example (the docstring there is stale).
        assertEquals("E425285FAAFC1535", Mac.algorithm3(Des(k1), Des(k2), data, Mac.Padding.METHOD_2).toHexUpper())
        val ka = ByteArray(8) { 0xAA.toByte() }
        val kb = ByteArray(8) { 0xBB.toByte() }
        assertEquals("78DA3D3DEB48FD4D", Mac.algorithm3(Des(ka), Des(kb), "hello world".encodeToByteArray(), Mac.Padding.METHOD_1).toHexUpper())
        assertEquals("2BC2D9EDE0CF31F6", Mac.algorithm3(Des(ka), Des(kb), "hello world".encodeToByteArray(), Mac.Padding.METHOD_2).toHexUpper())
    }

    @Test
    fun applicationCryptogramAndArpc() {
        val sk = "29B33180E567CE38EA4CBC9D753B0E61".hexToBytes()
        val arqc = EmvAc.generateAc(sk, "0123456789ABCDEF0123456789ABCDEF".hexToBytes())
        assertEquals("FA624250B008B59A", arqc.toHexUpper())
        assertEquals("45D4255EEF10C920", EmvAc.arpcMethod1(sk, arqc, "0000".hexToBytes()).toHexUpper())
        assertEquals("CB56FA40", EmvAc.arpcMethod2(sk, arqc, "00000000".hexToBytes()).toHexUpper())
        val sk2 = "AAAAAAAAAAAAAAAABBBBBBBBBBBBBBBB".hexToBytes()
        val arqc2 = "1234567890ABCDEF".hexToBytes()
        assertEquals("F5E6F44147E2F1B0", EmvAc.arpcMethod1(sk2, arqc2, "0000".hexToBytes()).toHexUpper())
        assertEquals("9308BEBC", EmvAc.arpcMethod2(sk2, arqc2, "00000000".hexToBytes()).toHexUpper())
    }

    @Test
    fun mastercardCvc3() {
        val issuerKey = "01234567899876543210012345678998".hexToBytes()
        val track2 = "5123456789012345D35121010000000000000F".hexToBytes()
        val cvc3 = CardVerification.cvc3(issuerKey, "5123456789012345", "00", track2, "00000899", "005E", digits = 5)
        assertEquals("29488", cvc3)
    }

    @Test
    fun visaCvv() {
        // Widely published Visa CVV example: CVK A ‖ B, PAN 4123456789012345, expiry 8701, service code 101.
        val cvk = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()
        assertEquals("561", CardVerification.cvv(cvk, "4123456789012345", "8701", "101"))
        // CVV2 and iCVV only change the service code position; they must still be 3 digits.
        assertEquals(3, CardVerification.cvv(cvk, "4123456789012345", "8701", "101", CardVerification.CvvKind.CVV2).length)
        assertEquals(3, CardVerification.cvv(cvk, "4123456789012345", "8701", "101", CardVerification.CvvKind.ICVV).length)
    }

    @Test
    fun visaPvvIsDeterministicAndFourDigits() {
        val pvk = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()
        val a = PinCrypto.visaPvv(pvk, "4123456789012345", "1234", 1)
        // Reference value computed with an independent Python implementation (cryptography package).
        assertEquals("1894", a)
        assertTrue(a.all { it.isDigit() })
        assertEquals(a, PinCrypto.visaPvv(pvk, "4123456789012345", "1234", 1))
        assertTrue(a != PinCrypto.visaPvv(pvk, "4123456789012345", "1235", 1))
    }

    @Test
    fun ibm3624OffsetRoundTrip() {
        val pdk = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()
        val natural = PinCrypto.ibmNaturalPin(pdk, "4123456789012345", "0123456789012345", 4)
        // Reference values computed with an independent Python implementation.
        assertEquals("3004", natural)
        val offset = PinCrypto.ibmOffset(natural, "1234")
        assertEquals("8230", offset)
        assertEquals("1234", PinCrypto.ibmPinFromOffset(natural, offset))
        assertEquals("0000", PinCrypto.ibmOffset(natural, natural))
    }

    @Test
    fun iso4PinBlockRoundTrip() {
        val key = "000102030405060708090A0B0C0D0E0F".hexToBytes()
        val fill = "0123456789ABCDEF".hexToBytes()
        assertEquals("441234AAAAAAAAAA0123456789ABCDEF", PinCrypto.iso4PinField("1234", fill).toHexUpper())
        assertEquals("44123456789012345000000000000000".let { "4" + it.drop(1) }, PinCrypto.iso4PanField("4123456789012345").toHexUpper())
        val block = PinCrypto.iso4Encrypt(key, "1234", "4123456789012345", fill)
        assertEquals(16, block.size)
        assertEquals("1234", PinCrypto.iso4Decrypt(key, block, "4123456789012345"))
    }

    @Test
    fun mdc2KnownAnswer() {
        // Wikipedia / OpenSSL MDC-2 reference values (zero padding to the block size).
        val fox = "The quick brown fox jumps over the lazy dog".encodeToByteArray()
        val padded = fox.copyOf(((fox.size + 7) / 8) * 8)
        assertEquals("000ED54E093D61679AEFBEAE05BFE33A", MdcHash.compute(padded, MdcHash.Variant.MDC2).toHexUpper())
        assertEquals(8, MdcHash.compute(padded, MdcHash.Variant.MDC1).size)
        assertEquals(16, MdcHash.compute(padded, MdcHash.Variant.MDC4).size)
    }
}
