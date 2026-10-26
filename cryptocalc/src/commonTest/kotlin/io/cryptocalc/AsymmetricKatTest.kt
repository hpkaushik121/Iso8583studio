package io.cryptocalc

import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import io.cryptocalc.asn1.Der
import io.cryptocalc.asn1.Pkcs
import io.cryptocalc.asn1.PublicKeyInfo
import io.cryptocalc.ec.Ec
import io.cryptocalc.ec.EcCurve
import io.cryptocalc.fpe.Fpe
import io.cryptocalc.hsm.ThalesLmk
import io.cryptocalc.math.BigInt
import io.cryptocalc.rsa.Rsa
import io.cryptocalc.x509.DistinguishedName
import io.cryptocalc.x509.SigningKey
import io.cryptocalc.x509.X509
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsymmetricKatTest {

    @Test
    fun bigIntModPowAndPrimes() {
        assertEquals(BigInt.fromDecimal("445"), BigInt.modPow(BigInt.fromDecimal("4"), BigInt.fromDecimal("13"), BigInt.fromDecimal("497")))
        assertTrue(BigInt.isProbablePrime(BigInt.fromDecimal("2147483647")))
        assertFalse(BigInt.isProbablePrime(BigInt.fromDecimal("2147483649")))
        assertEquals("00FF", BigInt.toUnsignedBytes(BigInt.fromHex("FF"), 2).toHexUpper())
    }

    @Test
    fun ecdsaRfc6979P256() {
        // RFC 6979 A.2.5: P-256, SHA-256, message "sample"
        val d = BigInt.fromHex("C9AFA9D845BA75166B5C215767B1D6934E50C3DB36E89B127B8A622B120F6721")
        val key = Ec.keyPairFromPrivate(EcCurve.P256, d)
        assertEquals("60FED4BA255A9D31C961EB74C6356D68C049B8923B61FA6CE669622E60F29FB6", BigInt.toHex(key.q.x, 64))
        assertEquals("7903FE1008B8BC99A41AE9E95628BC64F2F1B20C2D7E9F5177A3C294D4462299", BigInt.toHex(key.q.y, 64))
        val sig = Ec.sign(key, "SHA-256", "sample".encodeToByteArray())
        assertEquals("EFD48B2AACB6A8FD1140DD9CD45E81D69D2C877B56AAF991C34D0EA84EAF3716", BigInt.toHex(sig.r, 64))
        assertEquals("F7CB1C942D657C41D436C7A1B6E29F65F3E900DBB9AFF4064DC4AB2F843ACDA8", BigInt.toHex(sig.s, 64))
        assertTrue(Ec.verify(EcCurve.P256, key.q, "SHA-256", "sample".encodeToByteArray(), sig))
        assertFalse(Ec.verify(EcCurve.P256, key.q, "SHA-256", "sampl".encodeToByteArray(), sig))
        val der = sig.toDer()
        assertEquals(sig.r, Ec.Signature.fromDer(der).r)
        val compressed = Ec.encodePoint(EcCurve.P256, key.q, compressed = true)
        assertEquals(key.q, Ec.decodePoint(EcCurve.P256, compressed))
        assertTrue(Ec.isOnCurve(EcCurve.P256, key.q))
    }

    @Test
    fun ecdsaOtherCurvesRoundTrip() {
        for (curve in listOf(EcCurve.P384, EcCurve.P521, EcCurve.SECP256K1, EcCurve.BRAINPOOL_P256R1)) {
            try {
                assertTrue(Ec.isOnCurve(curve, curve.g), "${curve.name} generator")
                val kp = Ec.generateKeyPair(curve)
                assertTrue(Ec.isOnCurve(curve, kp.q), curve.name)
                val sig = Ec.sign(kp, "SHA-256", "hello".encodeToByteArray())
                assertTrue(Ec.verify(curve, kp.q, "SHA-256", "hello".encodeToByteArray(), sig), curve.name)
            } catch (e: Throwable) {
                throw AssertionError("${curve.name}: ${e.stackTraceToString().lines().take(8).joinToString(" | ")}", e)
            }
        }
    }

    @Test
    fun ff1NistSamples() {
        val key = "2B7E151628AED2A6ABF7158809CF4F3C".hexToBytes()
        val pt = Fpe.toNumerals("0123456789", "0123456789")
        val ct = Fpe.ff1Encrypt(key, ByteArray(0), 10, pt)
        assertEquals("2433477484", Fpe.fromNumerals(ct, "0123456789"))
        assertEquals("0123456789", Fpe.fromNumerals(Fpe.ff1Decrypt(key, ByteArray(0), 10, ct), "0123456789"))
        // Sample 2: tweak 39383736353433323130
        val ct2 = Fpe.ff1Encrypt(key, "39383736353433323130".hexToBytes(), 10, pt)
        assertEquals("6124200773", Fpe.fromNumerals(ct2, "0123456789"))
        // Sample 3: radix 36
        val a36 = Fpe.defaultAlphabet(36)
        val ct3 = Fpe.ff1Encrypt(key, "3737373770717273373737".hexToBytes(), 36, Fpe.toNumerals("0123456789abcdefghi", a36))
        assertEquals("a9tv40mll9kdu509eum", Fpe.fromNumerals(ct3, a36))
    }

    @Test
    fun ff3Samples() {
        // NIST FF3 sample 1 (64-bit tweak)
        val key = "EF4359D8D580AA4F7F036D6F04FC6A94".hexToBytes()
        val pt = Fpe.toNumerals("890121234567890000", "0123456789")
        val ct = Fpe.ff3Encrypt(key, "D8E7920AFA330A73".hexToBytes(), 10, pt)
        assertEquals("750918814058654607", Fpe.fromNumerals(ct, "0123456789"))
        assertEquals("890121234567890000", Fpe.fromNumerals(Fpe.ff3Decrypt(key, "D8E7920AFA330A73".hexToBytes(), 10, ct), "0123456789"))
        // FF3-1 (56-bit tweak); reference value from the Python `ff3` package
        val ct31 = Fpe.ff3Encrypt(key, "D8E7920AFA330A".hexToBytes(), 10, pt)
        assertEquals("477064185124354662", Fpe.fromNumerals(ct31, "0123456789"))
        assertEquals("890121234567890000", Fpe.fromNumerals(Fpe.ff3Decrypt(key, "D8E7920AFA330A".hexToBytes(), 10, ct31), "0123456789"))
    }

    @Test
    fun rsaRoundTrips() {
        val kp = Rsa.generateKeyPairPure(512)
        val pub = kp.publicKey
        val msg = "hello rsa".encodeToByteArray()
        assertTrue(msg.contentEquals(Rsa.decryptPkcs1(kp.privateKey, Rsa.encryptPkcs1(pub, msg))))
        assertTrue(msg.contentEquals(Rsa.decryptOaep(kp.privateKey, Rsa.encryptOaep(pub, msg, "SHA-1"), "SHA-1")))
        val sig = Rsa.signPkcs1(kp.privateKey, "SHA-256", msg)
        assertTrue(Rsa.verifyPkcs1(pub, "SHA-256", msg, sig))
        assertFalse(Rsa.verifyPkcs1(pub, "SHA-256", "x".encodeToByteArray(), sig))
        val pss = Rsa.signPss(kp.privateKey, "SHA-1", msg)
        assertTrue(Rsa.verifyPss(pub, "SHA-1", msg, pss))
        // Raw: public(private(m)) == m
        val m = BigInt.fromDecimal("123456789")
        assertEquals(m, Rsa.rawPublic(pub, Rsa.rawPrivate(kp.privateKey, m)))
        // Encodings round trip
        val parsed = Pkcs.parseRsaPrivateKey(Pkcs.rsaPrivateKeyDer(kp.privateKey))
        assertEquals(kp.privateKey.n, parsed.n)
        val spki = Pkcs.parseSubjectPublicKeyInfo(Pkcs.subjectPublicKeyInfo(pub)) as PublicKeyInfo.Rsa
        assertEquals(pub.n, spki.key.n)
        val pem = Der.toPem("PRIVATE KEY", Pkcs.pkcs8(kp.privateKey))
        val back = Pkcs.parsePrivateKey(pem) as Pkcs.PrivateKeyInfo.Rsa
        assertEquals(kp.privateKey.d, back.key.d)
    }

    @Test
    fun oaepKnownAnswerSeeded() {
        // Deterministic OAEP encode/decode with a fixed seed (structure check).
        val em = Rsa.oaepEncode("abc".encodeToByteArray(), 64, "SHA-1", ByteArray(0), ByteArray(20) { 0x11 })
        assertEquals(64, em.size)
        assertEquals("abc", Rsa.oaepDecode(em, "SHA-1").decodeToString())
    }

    @Test
    fun derOidRoundTrip() {
        for (oid in listOf("1.2.840.113549.1.1.1", "2.5.4.3", "1.3.132.0.34", "2.16.840.1.101.3.4.2.1")) {
            assertEquals(oid, Der.decode(Der.oid(oid)).oid())
        }
        assertEquals("06092A864886F70D010101", Der.oid("1.2.840.113549.1.1.1").toHexUpper())
    }

    @Test
    fun selfSignedCertificateAndCsr() {
        val kp = Ec.generateKeyPair(EcCurve.P256)
        val dn = DistinguishedName(commonName = "test.local", organization = "ISO8583Studio", country = "IN")
        val cert = X509.selfSignedCertificate(SigningKey.Ec(kp), dn, "SHA-256", notBeforeEpochSeconds = 1_760_000_000L, validityDays = 30)
        val pem = Der.toPem("CERTIFICATE", cert)
        val info = X509.parseCertificate(pem)
        assertEquals("C=IN, O=ISO8583Studio, CN=test.local", info.subject)
        assertEquals(info.subject, info.issuer)
        assertEquals("EC P-256", info.publicKeyAlgorithm)
        assertEquals(3, info.version)
        assertTrue(info.notAfter!!.startsWith("2025-11-08") || info.notAfter!!.startsWith("2025-11-07"), info.notAfter!!)
        assertTrue(X509.verifySelfSignature(pem))
        val csr = X509.certificationRequest(SigningKey.Ec(kp), dn)
        val csrInfo = X509.parseCertificationRequest(Der.toPem("CERTIFICATE REQUEST", csr))
        assertEquals(info.subject, csrInfo.subject)
        assertTrue(X509.verifySelfSignature(Der.toPem("CERTIFICATE REQUEST", csr)))
    }

    @Test
    fun thalesVariantLmk() {
        val lmk = ThalesLmk.testLmk("00-01")
        val key = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()
        val enc = ThalesLmk.encryptUnderLmk(key, lmk, 0, ThalesLmk.Scheme.U)
        assertEquals(key.toHexUpper(), ThalesLmk.decryptUnderLmk(enc, lmk, 0, ThalesLmk.Scheme.U).toHexUpper())
        assertTrue(enc.toHexUpper() != ThalesLmk.encryptUnderLmk(key, lmk, 1, ThalesLmk.Scheme.U).toHexUpper())
        assertEquals("08D7B4", ThalesLmk.kcv(key))
    }
}
