package io.cryptocalc.emv

import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import io.cryptocalc.crypto.primitives.Digests
import io.cryptocalc.math.BigInt
import io.cryptocalc.rsa.Rsa
import io.cryptocalc.rsa.RsaPrivateKey
import io.cryptocalc.rsa.RsaPublicKey

/**
 * Offline data authentication (EMV 4.3 Book 2 §5 and §6): recovery of the issuer and ICC public
 * keys from their certificates, verification of the signed static (SSAD) and dynamic (SDAD)
 * application data, and the issuer-side signing routines that build those objects.
 */
object EmvOda {

    class RecoveredKey(
        val modulus: ByteArray, val exponent: ByteArray, val identifier: String, val expiry: String, val serial: String,
        val hashValid: Boolean, val recovered: ByteArray, val notes: List<String>,
    ) {
        val publicKey: RsaPublicKey get() = RsaPublicKey(BigInt.fromUnsignedBytes(modulus), BigInt.fromUnsignedBytes(exponent))
    }

    class SsadResult(val dac: ByteArray, val hashValid: Boolean, val recovered: ByteArray, val notes: List<String>)
    class SdadResult(val iccDynamicNumber: ByteArray, val dynamicData: ByteArray, val hashValid: Boolean, val recovered: ByteArray, val notes: List<String>)

    private fun recover(certificate: ByteArray, key: RsaPublicKey): ByteArray {
        require(certificate.size == key.byteLength) { "Certificate length (${certificate.size}) must equal the key modulus length (${key.byteLength})" }
        return Rsa.rawEncrypt(key, certificate)
    }

    private fun bcd(bytes: ByteArray): String = bytes.toHexUpper()

    private fun checkFrame(r: ByteArray, format: Int, notes: MutableList<String>) {
        if ((r[0].toInt() and 0xFF) != 0x6A) notes += "Header is ${r[0].toHexUpper()} (expected 6A)"
        if ((r[1].toInt() and 0xFF) != format) notes += "Certificate format is ${r[1].toHexUpper()} (expected ${format.toString(16).padStart(2, '0').uppercase()})"
        if ((r[r.size - 1].toInt() and 0xFF) != 0xBC) notes += "Trailer is ${r[r.size - 1].toHexUpper()} (expected BC)"
    }

    private fun Byte.toHexUpper() = byteArrayOf(this).toHexUpper()

    /**
     * Issuer public key recovery (§6.3): [certificate] is tag 90, [remainder] tag 92, [exponent] tag 9F32,
     * recovered with the CA public key. The leftmost N_CA − 36 bytes come from the certificate.
     */
    fun recoverIssuerPublicKey(caKey: RsaPublicKey, certificate: ByteArray, remainder: ByteArray, exponent: ByteArray): RecoveredKey {
        val r = recover(certificate, caKey)
        val n = r.size
        val notes = mutableListOf<String>()
        checkFrame(r, 0x02, notes)
        val issuerId = bcd(r.copyOfRange(2, 6)).trimEnd('F')
        val expiry = bcd(r.copyOfRange(6, 8))
        val serial = bcd(r.copyOfRange(8, 11))
        if ((r[11].toInt() and 0xFF) != 0x01) notes += "Hash algorithm indicator ${r[11].toHexUpper()} (expected 01 = SHA-1)"
        if ((r[12].toInt() and 0xFF) != 0x01) notes += "Public key algorithm indicator ${r[12].toHexUpper()} (expected 01 = RSA)"
        val keyLen = r[13].toInt() and 0xFF
        val expLen = r[14].toInt() and 0xFF
        if (expLen != exponent.size) notes += "Exponent length in certificate ($expLen) differs from supplied exponent (${exponent.size})"
        val leftmost = r.copyOfRange(15, n - 21)
        val hash = r.copyOfRange(n - 21, n - 1)
        val modulus = (leftmost + remainder).copyOf(keyLen)
        if (keyLen > n - 36 && remainder.size != keyLen - (n - 36)) notes += "Remainder should be ${keyLen - (n - 36)} bytes, got ${remainder.size}"
        val computed = Digests.sha1(r.copyOfRange(1, n - 21) + remainder + exponent)
        val hashValid = computed.contentEquals(hash)
        if (!hashValid) notes += "Hash mismatch: certificate ${hash.toHexUpper()}, computed ${computed.toHexUpper()}"
        return RecoveredKey(modulus, exponent, issuerId, expiry, serial, hashValid, r, notes)
    }

    /**
     * ICC public key recovery (§6.4): [certificate] is tag 9F46, [remainder] 9F48, [exponent] 9F47,
     * recovered with the issuer public key. The hash also covers [staticData] (and the AIP when it
     * is in the SDA tag list; append it to [staticData]).
     */
    fun recoverIccPublicKey(issuerKey: RsaPublicKey, certificate: ByteArray, remainder: ByteArray, exponent: ByteArray, staticData: ByteArray): RecoveredKey {
        val r = recover(certificate, issuerKey)
        val n = r.size
        val notes = mutableListOf<String>()
        checkFrame(r, 0x04, notes)
        val pan = bcd(r.copyOfRange(2, 12)).trimEnd('F')
        val expiry = bcd(r.copyOfRange(12, 14))
        val serial = bcd(r.copyOfRange(14, 17))
        if ((r[17].toInt() and 0xFF) != 0x01) notes += "Hash algorithm indicator ${r[17].toHexUpper()} (expected 01)"
        if ((r[18].toInt() and 0xFF) != 0x01) notes += "Public key algorithm indicator ${r[18].toHexUpper()} (expected 01)"
        val keyLen = r[19].toInt() and 0xFF
        val expLen = r[20].toInt() and 0xFF
        if (expLen != exponent.size) notes += "Exponent length in certificate ($expLen) differs from supplied exponent (${exponent.size})"
        val leftmost = r.copyOfRange(21, n - 21)
        val hash = r.copyOfRange(n - 21, n - 1)
        val modulus = (leftmost + remainder).copyOf(keyLen)
        val computed = Digests.sha1(r.copyOfRange(1, n - 21) + remainder + exponent + staticData)
        val hashValid = computed.contentEquals(hash)
        if (!hashValid) notes += "Hash mismatch: certificate ${hash.toHexUpper()}, computed ${computed.toHexUpper()}"
        return RecoveredKey(modulus, exponent, pan, expiry, serial, hashValid, r, notes)
    }

    /** SSAD verification (§5.4): tag 93 recovered with the issuer public key; the hash covers [staticData]. */
    fun verifySsad(issuerKey: RsaPublicKey, ssad: ByteArray, staticData: ByteArray): SsadResult {
        val r = recover(ssad, issuerKey)
        val n = r.size
        val notes = mutableListOf<String>()
        checkFrame(r, 0x03, notes)
        if ((r[2].toInt() and 0xFF) != 0x01) notes += "Hash algorithm indicator ${r[2].toHexUpper()} (expected 01)"
        val dac = r.copyOfRange(3, 5)
        val hash = r.copyOfRange(n - 21, n - 1)
        val computed = Digests.sha1(r.copyOfRange(1, n - 21) + staticData)
        val hashValid = computed.contentEquals(hash)
        if (!hashValid) notes += "Hash mismatch: SSAD ${hash.toHexUpper()}, computed ${computed.toHexUpper()}"
        return SsadResult(dac, hashValid, r, notes)
    }

    /** SDAD verification (§6.5): tag 9F4B recovered with the ICC public key; the hash covers the DDOL data [terminalDynamicData]. */
    fun verifySdad(iccKey: RsaPublicKey, sdad: ByteArray, terminalDynamicData: ByteArray): SdadResult {
        val r = recover(sdad, iccKey)
        val n = r.size
        val notes = mutableListOf<String>()
        checkFrame(r, 0x05, notes)
        if ((r[2].toInt() and 0xFF) != 0x01) notes += "Hash algorithm indicator ${r[2].toHexUpper()} (expected 01)"
        val ldd = r[3].toInt() and 0xFF
        val dynamicData = r.copyOfRange(4, 4 + ldd)
        val idnLen = if (ldd > 0) dynamicData[0].toInt() and 0xFF else 0
        val idn = if (ldd > 0) dynamicData.copyOfRange(1, 1 + idnLen) else ByteArray(0)
        val hash = r.copyOfRange(n - 21, n - 1)
        val computed = Digests.sha1(r.copyOfRange(1, n - 21) + terminalDynamicData)
        val hashValid = computed.contentEquals(hash)
        if (!hashValid) notes += "Hash mismatch: SDAD ${hash.toHexUpper()}, computed ${computed.toHexUpper()}"
        return SdadResult(idn, dynamicData, hashValid, r, notes)
    }

    // ------------------------------------------------------------------ issuer / CA side

    private fun sign(key: RsaPrivateKey, block: ByteArray): ByteArray = Rsa.rawDecrypt(key, block)

    private fun bcdBytes(digits: String, length: Int): ByteArray =
        digits.filter { it.isDigit() }.padEnd(length * 2, 'F').take(length * 2).hexToBytes()

    /** Builds an issuer public key certificate (tag 90) and remainder (tag 92) signed with the CA private key. */
    fun issueIssuerCertificate(caKey: RsaPrivateKey, issuerModulus: ByteArray, issuerExponent: ByteArray, issuerId: String, expiryMMYY: String, serialHex: String): Pair<ByteArray, ByteArray> {
        val nCa = caKey.byteLength
        val leftLen = nCa - 36
        val leftmost = if (issuerModulus.size <= leftLen) issuerModulus + ByteArray(leftLen - issuerModulus.size) { 0xBB.toByte() } else issuerModulus.copyOf(leftLen)
        val remainder = if (issuerModulus.size > leftLen) issuerModulus.copyOfRange(leftLen, issuerModulus.size) else ByteArray(0)
        val body = byteArrayOf(0x02) + bcdBytes(issuerId, 4) + (expiryMMYY).hexToBytes() + (serialHex.padStart(6, '0')).hexToBytes() +
                byteArrayOf(0x01, 0x01, issuerModulus.size.toByte(), issuerExponent.size.toByte()) + leftmost
        val hash = Digests.sha1(body + remainder + issuerExponent)
        return sign(caKey, byteArrayOf(0x6A) + body + hash + byteArrayOf(0xBC.toByte())) to remainder
    }

    /** Builds an ICC public key certificate (9F46) and remainder (9F48) signed with the issuer private key. */
    fun issueIccCertificate(issuerKey: RsaPrivateKey, iccModulus: ByteArray, iccExponent: ByteArray, pan: String, expiryMMYY: String, serialHex: String, staticData: ByteArray): Pair<ByteArray, ByteArray> {
        val nI = issuerKey.byteLength
        val leftLen = nI - 42
        val leftmost = if (iccModulus.size <= leftLen) iccModulus + ByteArray(leftLen - iccModulus.size) { 0xBB.toByte() } else iccModulus.copyOf(leftLen)
        val remainder = if (iccModulus.size > leftLen) iccModulus.copyOfRange(leftLen, iccModulus.size) else ByteArray(0)
        val body = byteArrayOf(0x04) + bcdBytes(pan, 10) + (expiryMMYY).hexToBytes() + (serialHex.padStart(6, '0')).hexToBytes() +
                byteArrayOf(0x01, 0x01, iccModulus.size.toByte(), iccExponent.size.toByte()) + leftmost
        val hash = Digests.sha1(body + remainder + iccExponent + staticData)
        return sign(issuerKey, byteArrayOf(0x6A) + body + hash + byteArrayOf(0xBC.toByte())) to remainder
    }

    /** Signs static application data (tag 93) with the issuer private key. */
    fun signStaticData(issuerKey: RsaPrivateKey, dac: ByteArray, staticData: ByteArray): ByteArray {
        val nI = issuerKey.byteLength
        val body = byteArrayOf(0x03, 0x01) + dac + ByteArray(nI - 26) { 0xBB.toByte() }
        val hash = Digests.sha1(body + staticData)
        return sign(issuerKey, byteArrayOf(0x6A) + body + hash + byteArrayOf(0xBC.toByte()))
    }

    /** Signs dynamic application data (tag 9F4B) with the ICC private key; [idn] is the ICC dynamic number. */
    fun signDynamicData(iccKey: RsaPrivateKey, idn: ByteArray, terminalDynamicData: ByteArray): ByteArray {
        val nIc = iccKey.byteLength
        val dynamicData = byteArrayOf(idn.size.toByte()) + idn
        val body = byteArrayOf(0x05, 0x01, dynamicData.size.toByte()) + dynamicData + ByteArray(nIc - 25 - dynamicData.size) { 0xBB.toByte() }
        val hash = Digests.sha1(body + terminalDynamicData)
        return sign(iccKey, byteArrayOf(0x6A) + body + hash + byteArrayOf(0xBC.toByte()))
    }
}
