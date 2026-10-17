package io.cryptocalc

import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import io.cryptocalc.emv.Atr
import io.cryptocalc.emv.EmvCap
import io.cryptocalc.emv.EmvCvn
import io.cryptocalc.emv.EmvDecoders
import io.cryptocalc.emv.EmvOda
import io.cryptocalc.emv.EmvSecureMessaging
import io.cryptocalc.emv.EmvSessionKeys
import io.cryptocalc.math.BigInt
import io.cryptocalc.rsa.RsaPrivateKey
import io.cryptocalc.rsa.RsaPublicKey
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Oracle values come from pyemv (kd/ac/sm) unless stated otherwise. */
class EmvEnginesKatTest {

    private val mk = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()

    @Test
    fun emv2000TreeSessionKey() {
        val icc = "C9B406126D931ED05965C81E78F3D20B".hexToBytes()
        assertEquals("E90E2AE63158AB548CD69BD69D686D38", EmvSessionKeys.deriveEmv2000TreeSessionKey(icc, "0003".hexToBytes(), 8, 4).toHexUpper())
        assertEquals("70571668FD583108A416920B7FA18FBA", EmvSessionKeys.deriveEmv2000TreeSessionKey(icc, "1234".hexToBytes(), 16, 2).toHexUpper())
    }

    @Test
    fun visaSecureMessagingSessionKey() {
        assertEquals("0123456789ABCDF2FEDCBA987654CDF2", EmvSessionKeys.deriveVisaSmSessionKey(mk, "001C".hexToBytes()).toHexUpper())
    }

    @Test
    fun commandMacAndEncryption() {
        assertEquals("0BFFF5DF3FAA24E1", EmvSecureMessaging.commandMac(mk, "8424000008".hexToBytes()).toHexUpper())
        val pb = EmvSecureMessaging.visPinBlock(mk, "1234")
        assertEquals("041234FF76543210", pb.toHexUpper())
        assertEquals("9D8B34FF76543210", EmvSecureMessaging.visPinBlock(mk, "1234", "9999").toHexUpper())
        assertEquals("2C123456789012FF", EmvSecureMessaging.iso2PinBlock("123456789012").toHexUpper())
        assertEquals("C0E4AE3CFFD7981BE060630D4454E2A3", EmvSecureMessaging.encryptCommandData(mk, pb, EmvSecureMessaging.EncryptionType.VISA).toHexUpper())
        assertEquals("61D7A63AAFAE36D7", EmvSecureMessaging.encryptCommandData(mk, pb, EmvSecureMessaging.EncryptionType.MASTERCARD).toHexUpper())
        assertEquals("04B50570EE790EB3", EmvSecureMessaging.encryptCommandData(mk, pb.copyOf(5), EmvSecureMessaging.EncryptionType.MASTERCARD).toHexUpper())
        assertEquals("61D7A63AAFAE36D7C343A1D2D54EBA43", EmvSecureMessaging.encryptCommandData(mk, pb, EmvSecureMessaging.EncryptionType.EMV).toHexUpper())
        assertContentEquals(pb, EmvSecureMessaging.decryptCommandData(mk, EmvSecureMessaging.encryptCommandData(mk, pb, EmvSecureMessaging.EncryptionType.VISA), EmvSecureMessaging.EncryptionType.VISA))
    }

    private val txn = EmvCvn.TransactionData(
        "000000009999".hexToBytes(), "000000000000".hexToBytes(), "0840".hexToBytes(), "8000048000".hexToBytes(), "0840".hexToBytes(),
        "991231".hexToBytes(), "01".hexToBytes(), "52BF4585".hexToBytes(), "1800".hexToBytes(), "001C".hexToBytes(),
    )

    @Test
    fun visaCvn10Cryptogram() {
        val iad = "06011203A0B800".hexToBytes()
        val sk = EmvCvn.sessionKey(EmvCvn.VISA_CVN10, mk, txn)
        assertContentEquals(mk, sk)
        assertEquals("E56C2401D80CB05E", EmvCvn.generateAc(EmvCvn.VISA_CVN10, sk, EmvCvn.cryptogramData(EmvCvn.VISA_CVN10, txn, iad)).toHexUpper())
    }

    @Test
    fun mastercardCvn16And17Cryptograms() {
        val sk = EmvCvn.sessionKey(EmvCvn.MC_CVN16, mk, txn)
        assertEquals("7FDC1C0794E97C5BD061FD49C1A283F4", sk.toHexUpper())
        val iad16 = "0110A50000000000".hexToBytes()
        assertEquals("480637A4ED46D36E", EmvCvn.generateAc(EmvCvn.MC_CVN16, sk, EmvCvn.cryptogramData(EmvCvn.MC_CVN16, txn, iad16)).toHexUpper())
        val iad17 = "0111A5000000000000000000000001FF".hexToBytes()
        assertEquals("275EED3113BFF82B", EmvCvn.generateAc(EmvCvn.MC_CVN17, sk, EmvCvn.cryptogramData(EmvCvn.MC_CVN17, txn, iad17)).toHexUpper())
    }

    private val caKey = RsaPrivateKey(BigInt.fromHex("960BE1F37B94CC029CE84EDF2826F6F619D5A4B8BF054E5B60D903CA2AA610FC204EAE9FAB45E0171AF1906DC632792FED31231311D7A94E1DE278A36EA67375BC07ED5F493EAA4AC5F5B82B92DD2FAA311BBE7ECFAE664D07109E043F53F89532D20E3D81851190F1A6E9EB0E2169BDDD4C4816E48E6EF527F1BB79E3F6FFF5"), BigInt.fromHex("03"), BigInt.fromHex("6407EBF7A7B88801BDF03494C56F4F4EBBE3C325D4AE343CEB3B57DC1C6EB5FD6ADF1F151CD9400F674BB59E8421A61FF37617620BE51B896941A5C249C44CF82288B3519ABD93077FEBEFA1ACB5B39A28D911FF913CB0A08E4BE1ECFEE1FD93F43945796BDA227D8A707A390ED3F449E6E9C26CEC1D80C76F01E7F52EA511B3"), BigInt.fromHex("BD19A3C5E58416810FF3638E6C620C7EC469D6A5D85601604E086240FD0348877F501CB5C249B6E4E5D4B8DEEBAB763C919444004F9B16F18A72E6880C9225B3"), BigInt.fromHex("CB213C9EFB9E373E76206D2AA36A95C42F6C4CD99D7D5BFBE39668DFC3FDB3AFC52C09519D7426EFBC2979B68C3805127159607332C716D876FBF902116D3FB7")).withCrt()
    private val issuerKey = RsaPrivateKey(BigInt.fromHex("ED398894E69186C8701C53FD4029CC6C86B8557721BD9B05D8865029DDD0C00772C5771CF3EE4F4A3632468F6095D1F5E0C76A8D5095209548E4A470662280E252A2C2B381349C9DC1A177A8C7FAFAC2BFD0F9F4947B51A3DF027E551ADFA3F406F1AFB9AD732C61002934EE83BABA69"), BigInt.fromHex("03"), BigInt.fromHex("9E265B0DEF0BAF304ABD8D53801BDD9DAF258E4F6BD3BCAE90598AC693E08004F72E4F68A29EDF86CECC2F0A4063E14E95DA4708E0636B0CE7FEF8880D5619C7AAA9A59A50F365F370B48253D89A6C5B7F37DF74DF59CA952B73AFB9904E6D9DF965E3756FD0071DE3DF7214F6043FAB"), BigInt.fromHex("F3A53D5DF2AA5030953EF3F7A9780A68201E22CCCA9CA9E3C43F13C32C3E7F6D6D3DCFF96AAD826ECFC8E5E924A1E8C3892070E39926A899"), BigInt.fromHex("F940F2465F770A063D6556545E4F79487874915E3876AE55BCBE170219362256B09726C557BC7D18410FF4A0611938F0A13998EB798DB251")).withCrt()
    private val iccKey = RsaPrivateKey(BigInt.fromHex("98C79A19818E0AEE8110AEDCEFA84AC863CD4721A380A1B394DD3A3B116D586662B6BD305AF95E9B751CD5B4CC6C229E4140294742EF0730FA8F7FAB6E475B368EC5D47AB376C3A245A571F8E301E4A27596CADB73D5AC5D87E4DA75002F1A3B"), BigInt.fromHex("03"), BigInt.fromHex("65DA66BBABB40749AB6074934A7031DAED3384C117AB1677B8937C2760F3904441CF28CAE750E9BCF8BDE3CDDD9D6C6875E824E399A216DFEF18F6DCEF77B373E47A2B62BC958F7F75EBA98E99F9A9D8A64A15E8C6D0628C31DC0BEF56EF090B"), BigInt.fromHex("E748B99A1568A6E9601183EF03BA14132E4490EF742C82DF3478505085445A14C9FA1B64C8F4C6DCBB109531A6C2ED49"), BigInt.fromHex("A91B3857C7133DF7B3D889710359B9F589CA02772469E983E04BA35276C70BC8B22D8E9980A851AE820A335C57059F63")).withCrt()
    private fun RsaPrivateKey.pub() = RsaPublicKey(n, e)
    private fun RsaPrivateKey.modulusBytes() = BigInt.toUnsignedBytes(n, byteLength)

    @Test
    fun offlineDataAuthenticationRoundTrip() {
        val exp = "03".hexToBytes()
        val staticData = "5F2A0208405F34010182027C008407A0000000031010".hexToBytes()
        val (issuerCert, issuerRem) = EmvOda.issueIssuerCertificate(caKey, issuerKey.modulusBytes(), exp, "499999", "1230", "000123")
        val issuer = EmvOda.recoverIssuerPublicKey(caKey.pub(), issuerCert, issuerRem, exp)
        assertTrue(issuer.hashValid, issuer.notes.joinToString())
        assertTrue(issuer.notes.isEmpty(), issuer.notes.joinToString())
        assertContentEquals(issuerKey.modulusBytes(), issuer.modulus)
        assertEquals("499999", issuer.identifier)
        assertEquals("1230", issuer.expiry)

        val (iccCert, iccRem) = EmvOda.issueIccCertificate(issuerKey, iccKey.modulusBytes(), exp, "4999990011112222", "1230", "000001", staticData)
        val icc = EmvOda.recoverIccPublicKey(issuer.publicKey, iccCert, iccRem, exp, staticData)
        assertTrue(icc.hashValid, icc.notes.joinToString())
        assertContentEquals(iccKey.modulusBytes(), icc.modulus)
        assertEquals("4999990011112222", icc.identifier)
        // Wrong static data must break the hash.
        assertFalse(EmvOda.recoverIccPublicKey(issuer.publicKey, iccCert, iccRem, exp, staticData + byteArrayOf(1)).hashValid)

        val ssad = EmvOda.signStaticData(issuerKey, "1234".hexToBytes(), staticData)
        val ssadResult = EmvOda.verifySsad(issuer.publicKey, ssad, staticData)
        assertTrue(ssadResult.hashValid)
        assertEquals("1234", ssadResult.dac.toHexUpper())

        val sdad = EmvOda.signDynamicData(iccKey, "0102030405060708".hexToBytes(), "11223344".hexToBytes())
        val sdadResult = EmvOda.verifySdad(icc.publicKey, sdad, "11223344".hexToBytes())
        assertTrue(sdadResult.hashValid)
        assertEquals("0102030405060708", sdadResult.iccDynamicNumber.toHexUpper())
        assertFalse(EmvOda.verifySdad(icc.publicKey, sdad, "11223345".hexToBytes()).hashValid)
    }

    @Test
    fun capTokenSelectsIpbBits() {
        // IPB selects the low byte of the ATC and the first AC byte.
        val t = EmvCap.token(ipb = "00FF" .hexToBytes() + "FF00000000000000".hexToBytes() + "00".hexToBytes(), cid = "80".hexToBytes(), atc = "001A".hexToBytes(), ac = "A1B2C3D4E5F67890".hexToBytes(), iad = "0B".hexToBytes())
        assertEquals("0001101010100001", t.bits)
        assertEquals("6817", t.decimal)
    }

    @Test
    fun atrParsing() {
        val parsed = Atr.parse("3B8F8001804F0CA000000306030001000000006A".hexToBytes())
        assertEquals("Direct (3B)", parsed.convention)
        assertEquals(listOf(0, 1), parsed.protocols)
        assertEquals(true, parsed.tckValid)
        assertEquals(15, parsed.historicalBytes.size)
        assertTrue(parsed.warnings.isEmpty(), parsed.warnings.joinToString())
        val t0Only = Atr.parse("3B0A0102030405060708090A".hexToBytes())
        assertEquals(10, t0Only.historicalBytes.size)
        assertEquals(null, t0Only.tck)
    }

    @Test
    fun tvrDecoding() {
        val text = EmvDecoders.tvr("8000048000".hexToBytes())
        assertTrue(text.contains("Offline data authentication was not performed"))
        assertTrue(text.contains("Online PIN entered"))
        assertTrue(text.contains("Transaction exceeds floor limit"))
    }
}
