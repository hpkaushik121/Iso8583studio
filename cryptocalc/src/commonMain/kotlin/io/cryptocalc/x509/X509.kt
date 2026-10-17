package io.cryptocalc.x509

import ai.cortex.core.toHexUpper
import io.cryptocalc.asn1.Der
import io.cryptocalc.asn1.DerNode
import io.cryptocalc.asn1.Oids
import io.cryptocalc.asn1.Pkcs
import io.cryptocalc.asn1.PublicKeyInfo
import io.cryptocalc.crypto.primitives.Digests
import io.cryptocalc.crypto.primitives.secureRandomBytes
import io.cryptocalc.ec.Ec
import io.cryptocalc.ec.EcKeyPair
import io.cryptocalc.math.BigInt
import io.cryptocalc.rsa.Rsa
import io.cryptocalc.rsa.RsaPrivateKey

/** A signing key for certificates and requests. */
sealed class SigningKey {
    class Rsa(val key: RsaPrivateKey) : SigningKey()
    class Ec(val key: EcKeyPair) : SigningKey()
}

/** Subject / issuer distinguished name; empty attributes are skipped. */
data class DistinguishedName(
    val commonName: String = "", val organization: String = "", val organizationalUnit: String = "",
    val locality: String = "", val state: String = "", val country: String = "",
) {
    fun toDer(): ByteArray {
        fun rdn(oid: String, value: String, printable: Boolean = false): ByteArray? =
            if (value.isBlank()) null else Der.set(Der.sequence(Der.oid(oid), if (printable) Der.printable(value) else Der.utf8(value)))
        val rdns = listOfNotNull(
            rdn(Oids.C, country.trim(), printable = true), rdn(Oids.ST, state.trim()), rdn(Oids.L, locality.trim()),
            rdn(Oids.O, organization.trim()), rdn(Oids.OU, organizationalUnit.trim()), rdn(Oids.CN, commonName.trim()),
        )
        require(rdns.isNotEmpty()) { "A distinguished name needs at least one attribute" }
        return Der.sequence(*rdns.toTypedArray())
    }

    override fun toString(): String = listOf("CN" to commonName, "OU" to organizationalUnit, "O" to organization, "L" to locality, "ST" to state, "C" to country)
        .filter { it.second.isNotBlank() }.joinToString(", ") { "${it.first}=${it.second}" }

    companion object {
        fun fromDer(node: DerNode): String = node.children.flatMap { set -> set.children }.joinToString(", ") { atv ->
            "${Oids.name(atv.child(0).oid())}=${atv.child(1).string()}"
        }
    }
}

/** Parsed certificate (or request) summary for display. */
data class CertificateInfo(
    val subject: String, val issuer: String?, val serialHex: String?, val notBefore: String?, val notAfter: String?,
    val publicKeyAlgorithm: String, val publicKeySizeBits: Int, val signatureAlgorithm: String,
    val extensions: List<String>, val sha1Fingerprint: String, val sha256Fingerprint: String, val version: Int?,
    val publicKeyDer: ByteArray,
)

/**
 * X.509 v3 self-signed certificates and PKCS#10 certification requests, signed with RSA
 * (PKCS#1 v1.5) or ECDSA, plus parsing of certificates and requests for display.
 */
object X509 {

    private fun signatureOid(key: SigningKey, hash: String): String = when (key) {
        is SigningKey.Rsa -> when (hash.uppercase().replace("-", "")) {
            "SHA1" -> Oids.SHA1_WITH_RSA; "SHA256" -> Oids.SHA256_WITH_RSA; "SHA384" -> Oids.SHA384_WITH_RSA; "SHA512" -> Oids.SHA512_WITH_RSA
            else -> throw IllegalArgumentException("Unsupported hash $hash")
        }
        is SigningKey.Ec -> when (hash.uppercase().replace("-", "")) {
            "SHA256" -> Oids.ECDSA_WITH_SHA256; "SHA384" -> Oids.ECDSA_WITH_SHA384; "SHA512" -> Oids.ECDSA_WITH_SHA512
            else -> throw IllegalArgumentException("Unsupported hash $hash for ECDSA")
        }
    }

    private fun signatureAlgorithmIdentifier(key: SigningKey, hash: String): ByteArray = when (key) {
        is SigningKey.Rsa -> Der.sequence(Der.oid(signatureOid(key, hash)), Der.nullValue())
        is SigningKey.Ec -> Der.sequence(Der.oid(signatureOid(key, hash)))
    }

    private fun sign(key: SigningKey, hash: String, tbs: ByteArray): ByteArray = when (key) {
        is SigningKey.Rsa -> Rsa.signPkcs1(key.key, hash, tbs)
        is SigningKey.Ec -> Ec.sign(key.key, hash, tbs).toDer()
    }

    private fun spki(key: SigningKey): ByteArray = when (key) {
        is SigningKey.Rsa -> Pkcs.subjectPublicKeyInfo(key.key.publicKey)
        is SigningKey.Ec -> Pkcs.subjectPublicKeyInfo(key.key.curve, key.key.q)
    }

    /** `YYMMDDHHMMSSZ` for UTCTime from a UTC epoch-seconds value. */
    private fun utcTime(epochSeconds: Long): ByteArray {
        val (y, mo, d, h, mi, s) = civil(epochSeconds)
        return if (y in 1950..2049) Der.utcTime("${(y % 100).pad2()}${mo.pad2()}${d.pad2()}${h.pad2()}${mi.pad2()}${s.pad2()}Z")
        else Der.generalizedTime("$y${mo.pad2()}${d.pad2()}${h.pad2()}${mi.pad2()}${s.pad2()}Z")
    }

    private fun Int.pad2() = toString().padStart(2, '0')

    private data class Civil(val y: Int, val mo: Int, val d: Int, val h: Int, val mi: Int, val s: Int)

    /** Days-from-civil inverse (Howard Hinnant's algorithm), no time zone. */
    private fun civil(epochSeconds: Long): Civil {
        val days = Math_floorDiv(epochSeconds, 86400L)
        val rem = epochSeconds - days * 86400L
        val z = days + 719468
        val era = Math_floorDiv(z, 146097L)
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val year = (if (m <= 2) y + 1 else y).toInt()
        return Civil(year, m, d, (rem / 3600).toInt(), ((rem % 3600) / 60).toInt(), (rem % 60).toInt())
    }

    private fun Math_floorDiv(a: Long, b: Long): Long { val q = a / b; return if ((a % b != 0L) && ((a < 0) != (b < 0))) q - 1 else q }

    /**
     * Self-signed X.509 v3 certificate. [notBeforeEpochSeconds] defaults to now as supplied by the
     * caller (the engine has no clock); validity is [validityDays].
     */
    fun selfSignedCertificate(
        key: SigningKey, subject: DistinguishedName, hash: String = "SHA-256",
        notBeforeEpochSeconds: Long, validityDays: Int = 365, serial: ByteArray = secureRandomBytes(16),
    ): ByteArray {
        val serialInt = BigInt.fromUnsignedBytes(serial.also { it[0] = (it[0].toInt() and 0x7F).toByte() })
        val name = subject.toDer()
        val spkiDer = spki(key)
        val skid = Digests.sha1(Der.decode(spkiDer).child(1).bitStringBytes())
        val extensions = Der.explicit(3, Der.sequence(
            Der.sequence(Der.oid(Oids.BASIC_CONSTRAINTS), Der.boolean(true), Der.octetString(Der.sequence(Der.boolean(true)))),
            Der.sequence(Der.oid(Oids.SUBJECT_KEY_ID), Der.octetString(Der.octetString(skid))),
        ))
        val tbs = Der.sequence(
            Der.explicit(0, Der.integer(2)),
            Der.integer(serialInt),
            signatureAlgorithmIdentifier(key, hash),
            name,
            Der.sequence(utcTime(notBeforeEpochSeconds), utcTime(notBeforeEpochSeconds + validityDays.toLong() * 86400L)),
            name,
            spkiDer,
            extensions,
        )
        return Der.sequence(tbs, signatureAlgorithmIdentifier(key, hash), Der.bitString(sign(key, hash, tbs)))
    }

    /** PKCS#10 certification request (RFC 2986) with an empty attribute set. */
    fun certificationRequest(key: SigningKey, subject: DistinguishedName, hash: String = "SHA-256"): ByteArray {
        val cri = Der.sequence(Der.integer(0), subject.toDer(), spki(key), Der.explicit(0, ByteArray(0)))
        return Der.sequence(cri, signatureAlgorithmIdentifier(key, hash), Der.bitString(sign(key, hash, cri)))
    }

    // ------------------------------------------------------------------ parsing

    private fun timeString(node: DerNode): String {
        val s = node.string()
        return if (node.tag == Der.UTC_TIME && s.length >= 12) {
            val yy = s.substring(0, 2).toInt()
            val year = if (yy < 50) 2000 + yy else 1900 + yy
            "$year-${s.substring(2, 4)}-${s.substring(4, 6)} ${s.substring(6, 8)}:${s.substring(8, 10)}:${s.substring(10, 12)} UTC"
        } else if (s.length >= 14) {
            "${s.substring(0, 4)}-${s.substring(4, 6)}-${s.substring(6, 8)} ${s.substring(8, 10)}:${s.substring(10, 12)}:${s.substring(12, 14)} UTC"
        } else s
    }

    private fun describe(pk: PublicKeyInfo): Pair<String, Int> = when (pk) {
        is PublicKeyInfo.Rsa -> "RSA" to pk.key.bitLength
        is PublicKeyInfo.Ec -> "EC ${pk.curve.name}" to pk.curve.p.bitLength()
    }

    fun parseCertificate(text: String): CertificateInfo {
        val (der, _) = Der.fromPemOrDer(text)
        val cert = Der.decode(der)
        require(cert.isSequence && cert.children.size == 3) { "Not an X.509 certificate" }
        val tbs = cert.child(0)
        var i = 0
        val version = if (tbs.child(0).tag == 0xA0) { i = 1; tbs.child(0).child(0).integer().intValue(false) + 1 } else 1
        val serial = tbs.child(i).integer()
        val sigAlg = Oids.name(tbs.child(i + 1).child(0).oid())
        val issuer = DistinguishedName.fromDer(tbs.child(i + 2))
        val validity = tbs.child(i + 3)
        val subject = DistinguishedName.fromDer(tbs.child(i + 4))
        val spkiNode = tbs.child(i + 5)
        val pk = Pkcs.parseSubjectPublicKeyInfo(spkiNode)
        val (alg, bits) = describe(pk)
        val extensions = tbs.children.firstOrNull { it.tag == 0xA3 }?.child(0)?.children?.map { ext ->
            val critical = ext.children.size == 3 && ext.child(1).tag == Der.BOOLEAN
            Oids.name(ext.child(0).oid()) + if (critical) " (critical)" else ""
        } ?: emptyList()
        return CertificateInfo(
            subject = subject, issuer = issuer, serialHex = BigInt.toHexEven(serial),
            notBefore = timeString(validity.child(0)), notAfter = timeString(validity.child(1)),
            publicKeyAlgorithm = alg, publicKeySizeBits = bits, signatureAlgorithm = sigAlg,
            extensions = extensions,
            sha1Fingerprint = Digests.sha1(der).toHexUpper().chunked(2).joinToString(":"),
            sha256Fingerprint = Digests.sha256(der).toHexUpper().chunked(2).joinToString(":"),
            version = version, publicKeyDer = spkiNode.encoded,
        )
    }

    fun parseCertificationRequest(text: String): CertificateInfo {
        val (der, _) = Der.fromPemOrDer(text)
        val csr = Der.decode(der)
        require(csr.isSequence && csr.children.size == 3) { "Not a PKCS#10 request" }
        val cri = csr.child(0)
        val subject = DistinguishedName.fromDer(cri.child(1))
        val pk = Pkcs.parseSubjectPublicKeyInfo(cri.child(2))
        val (alg, bits) = describe(pk)
        val sigAlg = Oids.name(csr.child(1).child(0).oid())
        val attrs = cri.children.firstOrNull { it.tag == 0xA0 }?.children?.map { Oids.name(it.child(0).oid()) } ?: emptyList()
        return CertificateInfo(
            subject = subject, issuer = null, serialHex = null, notBefore = null, notAfter = null,
            publicKeyAlgorithm = alg, publicKeySizeBits = bits, signatureAlgorithm = sigAlg, extensions = attrs,
            sha1Fingerprint = Digests.sha1(der).toHexUpper().chunked(2).joinToString(":"),
            sha256Fingerprint = Digests.sha256(der).toHexUpper().chunked(2).joinToString(":"),
            version = cri.child(0).integer().intValue(false) + 1, publicKeyDer = cri.child(2).encoded,
        )
    }

    /** Verifies the signature of a self-signed certificate or a CSR with the public key it carries. */
    fun verifySelfSignature(text: String): Boolean {
        val (der, _) = Der.fromPemOrDer(text)
        val outer = Der.decode(der)
        val body = outer.child(0)
        val sigOid = outer.child(1).child(0).oid()
        val signature = outer.child(2).bitStringBytes()
        val spkiNode = body.children.firstOrNull { c -> c.isSequence && c.children.size == 2 && c.child(0).isSequence && c.child(1).tag == Der.BIT_STRING &&
            runCatching { c.child(0).child(0).oid() in setOf(Oids.RSA_ENCRYPTION, Oids.EC_PUBLIC_KEY) }.getOrDefault(false) }
            ?: return false
        val pk = Pkcs.parseSubjectPublicKeyInfo(spkiNode)
        val hash = when (sigOid) {
            Oids.SHA1_WITH_RSA -> "SHA-1"; Oids.SHA256_WITH_RSA, Oids.ECDSA_WITH_SHA256 -> "SHA-256"
            Oids.SHA384_WITH_RSA, Oids.ECDSA_WITH_SHA384 -> "SHA-384"; Oids.SHA512_WITH_RSA, Oids.ECDSA_WITH_SHA512 -> "SHA-512"
            else -> return false
        }
        return when (pk) {
            is PublicKeyInfo.Rsa -> Rsa.verifyPkcs1(pk.key, hash, body.encoded, signature)
            is PublicKeyInfo.Ec -> runCatching { Ec.verify(pk.curve, pk.point, hash, body.encoded, Ec.Signature.fromDer(signature)) }.getOrDefault(false)
        }
    }
}
