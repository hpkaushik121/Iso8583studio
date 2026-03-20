package `in`.aicortex.iso8583studio.license

import java.io.StringWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

/**
 * Generates an RSA key pair and a PKCS#10 Certificate Signing Request (CSR)
 * for the license activation flow.
 *
 * The CSR embeds the machine fingerprint as the Common Name so the vendor
 * can bind the signed certificate to this specific machine.
 *
 * Uses pure java.security APIs — no Bouncy Castle required.
 */
object CsrGenerator {

    data class CsrResult(
        val csrPem: String,
        val privateKeyPem: String,
        val publicKeyPem: String
    )

    fun generate(machineId: String = MachineFingerprint.compute()): CsrResult {
        val keyPair = generateKeyPair()
        val csrDer = buildPkcs10Csr(keyPair, machineId)
        val csrPem = derToPem(csrDer, "CERTIFICATE REQUEST")
        val privPem = derToPem(keyPair.private.encoded, "PRIVATE KEY")
        val pubPem = derToPem(keyPair.public.encoded, "PUBLIC KEY")
        return CsrResult(csrPem, privPem, pubPem)
    }

    private fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(2048)
        return gen.generateKeyPair()
    }

    /**
     * Builds a minimal PKCS#10 CSR DER encoding manually.
     * Subject: CN=<machineId>, O=ISO8583Studio
     */
    private fun buildPkcs10Csr(keyPair: KeyPair, machineId: String): ByteArray {
        val subject = buildDistinguishedName(machineId)
        val pubKeyInfo = keyPair.public.encoded

        val certRequestInfo = buildSequence(
            buildInteger(0),
            subject,
            pubKeyInfo,
            buildContext0Constructed(byteArrayOf())
        )

        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(keyPair.private)
        signature.update(certRequestInfo)
        val sig = signature.sign()

        val signatureAlgorithm = buildSequence(
            buildOid(byteArrayOf(0x2A.toByte(), 0x86.toByte(), 0x48.toByte(), 0x86.toByte(),
                0xF7.toByte(), 0x0D.toByte(), 0x01.toByte(), 0x01.toByte(), 0x0B.toByte())),
            byteArrayOf(0x05, 0x00)
        )

        val signatureBits = buildBitString(sig)
        return buildSequence(certRequestInfo, signatureAlgorithm, signatureBits)
    }

    private fun buildDistinguishedName(machineId: String): ByteArray {
        val cnOid = byteArrayOf(0x55, 0x04, 0x03)
        val oOid = byteArrayOf(0x55, 0x04, 0x0A)

        val cnValue = buildUtf8String(machineId)
        val cnAttr = buildSequence(buildOid(cnOid), cnValue)
        val cnSet = buildSet(cnAttr)

        val oValue = buildUtf8String("ISO8583Studio")
        val oAttr = buildSequence(buildOid(oOid), oValue)
        val oSet = buildSet(oAttr)

        return buildSequence(cnSet, oSet)
    }

    private fun buildSequence(vararg items: ByteArray): ByteArray {
        val content = items.fold(byteArrayOf()) { acc, b -> acc + b }
        return byteArrayOf(0x30.toByte()) + derLength(content.size) + content
    }

    private fun buildSet(vararg items: ByteArray): ByteArray {
        val content = items.fold(byteArrayOf()) { acc, b -> acc + b }
        return byteArrayOf(0x31.toByte()) + derLength(content.size) + content
    }

    private fun buildInteger(value: Int): ByteArray {
        return byteArrayOf(0x02, 0x01, value.toByte())
    }

    private fun buildOid(oidBytes: ByteArray): ByteArray {
        return byteArrayOf(0x06, oidBytes.size.toByte()) + oidBytes
    }

    private fun buildUtf8String(value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        return byteArrayOf(0x0C.toByte()) + derLength(bytes.size) + bytes
    }

    private fun buildBitString(data: ByteArray): ByteArray {
        val content = byteArrayOf(0x00) + data
        return byteArrayOf(0x03.toByte()) + derLength(content.size) + content
    }

    private fun buildContext0Constructed(data: ByteArray): ByteArray {
        return byteArrayOf(0xA0.toByte()) + derLength(data.size) + data
    }

    private fun derLength(length: Int): ByteArray {
        return when {
            length < 0x80 -> byteArrayOf(length.toByte())
            length <= 0xFF -> byteArrayOf(0x81.toByte(), length.toByte())
            length <= 0xFFFF -> byteArrayOf(0x82.toByte(), (length shr 8).toByte(), (length and 0xFF).toByte())
            else -> byteArrayOf(
                0x83.toByte(),
                (length shr 16).toByte(),
                ((length shr 8) and 0xFF).toByte(),
                (length and 0xFF).toByte()
            )
        }
    }

    private fun derToPem(der: ByteArray, type: String): String {
        val b64 = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(der)
        return "-----BEGIN $type-----\n$b64\n-----END $type-----"
    }
}
