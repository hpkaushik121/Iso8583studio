package io.cryptocalc.asn1

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.cryptocalc.ec.Ec
import io.cryptocalc.ec.EcCurve
import io.cryptocalc.ec.EcKeyPair
import io.cryptocalc.ec.EcPoint
import io.cryptocalc.math.BigInt
import io.cryptocalc.math.pmod
import io.cryptocalc.rsa.RsaPrivateKey
import io.cryptocalc.rsa.RsaPublicKey

/** Well-known object identifiers. */
object Oids {
    const val RSA_ENCRYPTION = "1.2.840.113549.1.1.1"
    const val SHA1_WITH_RSA = "1.2.840.113549.1.1.5"
    const val SHA256_WITH_RSA = "1.2.840.113549.1.1.11"
    const val SHA384_WITH_RSA = "1.2.840.113549.1.1.12"
    const val SHA512_WITH_RSA = "1.2.840.113549.1.1.13"
    const val EC_PUBLIC_KEY = "1.2.840.10045.2.1"
    const val ECDSA_WITH_SHA256 = "1.2.840.10045.4.3.2"
    const val ECDSA_WITH_SHA384 = "1.2.840.10045.4.3.3"
    const val ECDSA_WITH_SHA512 = "1.2.840.10045.4.3.4"
    const val P256 = "1.2.840.10045.3.1.7"
    const val P384 = "1.3.132.0.34"
    const val P521 = "1.3.132.0.35"
    const val SECP256K1 = "1.3.132.0.10"
    const val BRAINPOOL_P256R1 = "1.3.36.3.3.2.8.1.1.7"
    const val BRAINPOOL_P384R1 = "1.3.36.3.3.2.8.1.1.11"
    const val BRAINPOOL_P512R1 = "1.3.36.3.3.2.8.1.1.13"
    const val CN = "2.5.4.3"
    const val O = "2.5.4.10"
    const val OU = "2.5.4.11"
    const val L = "2.5.4.7"
    const val ST = "2.5.4.8"
    const val C = "2.5.4.6"
    const val BASIC_CONSTRAINTS = "2.5.29.19"
    const val SUBJECT_KEY_ID = "2.5.29.14"
    const val KEY_USAGE = "2.5.29.15"
    const val SUBJECT_ALT_NAME = "2.5.29.17"
    const val EXT_REQUEST = "1.2.840.113549.1.9.14"

    fun curveOid(curve: EcCurve): String = when (curve.name) {
        "P-256" -> P256; "P-384" -> P384; "P-521" -> P521; "secp256k1" -> SECP256K1
        "brainpoolP256r1" -> BRAINPOOL_P256R1; "brainpoolP384r1" -> BRAINPOOL_P384R1; "brainpoolP512r1" -> BRAINPOOL_P512R1
        else -> throw IllegalArgumentException("No OID for ${curve.name}")
    }

    fun curveByOid(oid: String): EcCurve = when (oid) {
        P256 -> EcCurve.P256; P384 -> EcCurve.P384; P521 -> EcCurve.P521; SECP256K1 -> EcCurve.SECP256K1
        BRAINPOOL_P256R1 -> EcCurve.BRAINPOOL_P256R1; BRAINPOOL_P384R1 -> EcCurve.BRAINPOOL_P384R1; BRAINPOOL_P512R1 -> EcCurve.BRAINPOOL_P512R1
        else -> throw IllegalArgumentException("Unknown curve OID $oid")
    }

    val NAMES: Map<String, String> = mapOf(
        RSA_ENCRYPTION to "rsaEncryption", SHA1_WITH_RSA to "sha1WithRSAEncryption", SHA256_WITH_RSA to "sha256WithRSAEncryption",
        SHA384_WITH_RSA to "sha384WithRSAEncryption", SHA512_WITH_RSA to "sha512WithRSAEncryption",
        EC_PUBLIC_KEY to "ecPublicKey", ECDSA_WITH_SHA256 to "ecdsa-with-SHA256", ECDSA_WITH_SHA384 to "ecdsa-with-SHA384", ECDSA_WITH_SHA512 to "ecdsa-with-SHA512",
        P256 to "prime256v1", P384 to "secp384r1", P521 to "secp521r1", SECP256K1 to "secp256k1",
        CN to "CN", O to "O", OU to "OU", L to "L", ST to "ST", C to "C",
        BASIC_CONSTRAINTS to "basicConstraints", SUBJECT_KEY_ID to "subjectKeyIdentifier", KEY_USAGE to "keyUsage", SUBJECT_ALT_NAME to "subjectAltName",
        "1.2.840.113549.1.9.1" to "emailAddress", "2.5.29.35" to "authorityKeyIdentifier", "2.5.29.37" to "extKeyUsage",
        "2.5.29.31" to "cRLDistributionPoints", "1.3.6.1.5.5.7.1.1" to "authorityInfoAccess", "2.5.29.32" to "certificatePolicies",
    )

    fun name(oid: String): String = NAMES[oid] ?: oid
}

/** Either an RSA or an EC public key, as found in a SubjectPublicKeyInfo. */
sealed class PublicKeyInfo {
    class Rsa(val key: RsaPublicKey) : PublicKeyInfo()
    class Ec(val curve: EcCurve, val point: EcPoint) : PublicKeyInfo()
    val algorithm: String get() = if (this is Rsa) "RSA" else "EC"
    val sizeBits: Int get() = when (this) { is Rsa -> key.bitLength; is Ec -> curve.p.bitLength() }
}

/** PKCS#1, PKCS#8, SubjectPublicKeyInfo and SEC1 key encodings (RFC 8017, 5208, 5480, 5915). */
object Pkcs {

    // ------------------------------------------------------------------ RSA

    /** PKCS#1 `RSAPublicKey ::= SEQUENCE { modulus INTEGER, publicExponent INTEGER }`. */
    fun rsaPublicKeyDer(key: RsaPublicKey): ByteArray = Der.sequence(Der.integer(key.n), Der.integer(key.e))

    fun parseRsaPublicKey(der: ByteArray): RsaPublicKey {
        val node = Der.decode(der)
        require(node.isSequence && node.children.size == 2) { "Not a PKCS#1 RSAPublicKey" }
        return RsaPublicKey(node.child(0).integer(), node.child(1).integer())
    }

    /** PKCS#1 `RSAPrivateKey` (version 0, CRT components included). */
    fun rsaPrivateKeyDer(key: RsaPrivateKey): ByteArray {
        val k = key.withCrt()
        requireNotNull(k.p); requireNotNull(k.q)
        return Der.sequence(
            Der.integer(0), Der.integer(k.n), Der.integer(k.e), Der.integer(k.d),
            Der.integer(k.p!!), Der.integer(k.q!!), Der.integer(k.dP!!), Der.integer(k.dQ!!), Der.integer(k.qInv!!)
        )
    }

    fun parseRsaPrivateKey(der: ByteArray): RsaPrivateKey {
        val node = Der.decode(der)
        require(node.isSequence && node.children.size >= 9) { "Not a PKCS#1 RSAPrivateKey" }
        val c = node.children
        return RsaPrivateKey(c[1].integer(), c[2].integer(), c[3].integer(), c[4].integer(), c[5].integer(), c[6].integer(), c[7].integer(), c[8].integer())
    }

    fun rsaAlgorithmIdentifier(): ByteArray = Der.sequence(Der.oid(Oids.RSA_ENCRYPTION), Der.nullValue())

    // ------------------------------------------------------------------ EC

    fun ecAlgorithmIdentifier(curve: EcCurve): ByteArray = Der.sequence(Der.oid(Oids.EC_PUBLIC_KEY), Der.oid(Oids.curveOid(curve)))

    /** SEC1 / RFC 5915 `ECPrivateKey`. */
    fun ecPrivateKeyDer(key: EcKeyPair, includeCurve: Boolean = true): ByteArray {
        val items = ArrayList<ByteArray>()
        items.add(Der.integer(1))
        items.add(Der.octetString(BigInt.toUnsignedBytes(key.d, key.curve.byteLength)))
        if (includeCurve) items.add(Der.explicit(0, Der.oid(Oids.curveOid(key.curve))))
        items.add(Der.explicit(1, Der.bitString(Ec.encodePoint(key.curve, key.q))))
        return Der.sequence(*items.toTypedArray())
    }

    fun parseEcPrivateKey(der: ByteArray, curveHint: EcCurve? = null): EcKeyPair {
        val node = Der.decode(der)
        require(node.isSequence && node.children.size >= 2) { "Not an ECPrivateKey" }
        val d = BigInt.fromUnsignedBytes(node.child(1).content)
        val curve = node.children.firstOrNull { it.tag == 0xA0 }?.children?.firstOrNull()?.let { Oids.curveByOid(it.oid()) }
            ?: curveHint ?: throw IllegalArgumentException("ECPrivateKey has no curve parameters")
        return Ec.keyPairFromPrivate(curve, d)
    }

    // ------------------------------------------------------------------ SubjectPublicKeyInfo / PKCS#8

    fun subjectPublicKeyInfo(key: RsaPublicKey): ByteArray = Der.sequence(rsaAlgorithmIdentifier(), Der.bitString(rsaPublicKeyDer(key)))

    fun subjectPublicKeyInfo(curve: EcCurve, point: EcPoint): ByteArray =
        Der.sequence(ecAlgorithmIdentifier(curve), Der.bitString(Ec.encodePoint(curve, point)))

    fun parseSubjectPublicKeyInfo(der: ByteArray): PublicKeyInfo = parseSubjectPublicKeyInfo(Der.decode(der))

    fun parseSubjectPublicKeyInfo(node: DerNode): PublicKeyInfo {
        require(node.isSequence && node.children.size == 2) { "Not a SubjectPublicKeyInfo" }
        val alg = node.child(0)
        val oid = alg.child(0).oid()
        val keyBits = node.child(1).bitStringBytes()
        return when (oid) {
            Oids.RSA_ENCRYPTION -> PublicKeyInfo.Rsa(parseRsaPublicKey(keyBits))
            Oids.EC_PUBLIC_KEY -> {
                val curve = Oids.curveByOid(alg.child(1).oid())
                PublicKeyInfo.Ec(curve, Ec.decodePoint(curve, keyBits))
            }
            else -> throw IllegalArgumentException("Unsupported public key algorithm ${Oids.name(oid)}")
        }
    }

    /** PKCS#8 `PrivateKeyInfo` for an RSA key. */
    fun pkcs8(key: RsaPrivateKey): ByteArray = Der.sequence(Der.integer(0), rsaAlgorithmIdentifier(), Der.octetString(rsaPrivateKeyDer(key)))

    /** PKCS#8 `PrivateKeyInfo` for an EC key. */
    fun pkcs8(key: EcKeyPair): ByteArray = Der.sequence(Der.integer(0), ecAlgorithmIdentifier(key.curve), Der.octetString(ecPrivateKeyDer(key, includeCurve = false)))

    sealed class PrivateKeyInfo {
        class Rsa(val key: RsaPrivateKey) : PrivateKeyInfo()
        class Ec(val key: EcKeyPair) : PrivateKeyInfo()
    }

    /** Parses PKCS#8 (`PRIVATE KEY`), PKCS#1 (`RSA PRIVATE KEY`) or SEC1 (`EC PRIVATE KEY`) PEM/DER. */
    fun parsePrivateKey(text: String): PrivateKeyInfo {
        val (der, label) = Der.fromPemOrDer(text)
        return when (label) {
            "RSA PRIVATE KEY" -> PrivateKeyInfo.Rsa(parseRsaPrivateKey(der))
            "EC PRIVATE KEY" -> PrivateKeyInfo.Ec(parseEcPrivateKey(der))
            else -> {
                val node = Der.decode(der)
                if (node.isSequence && node.children.size >= 3 && node.child(1).isSequence) {
                    val oid = node.child(1).child(0).oid()
                    val inner = node.child(2).content
                    when (oid) {
                        Oids.RSA_ENCRYPTION -> PrivateKeyInfo.Rsa(parseRsaPrivateKey(inner))
                        Oids.EC_PUBLIC_KEY -> PrivateKeyInfo.Ec(parseEcPrivateKey(inner, Oids.curveByOid(node.child(1).child(1).oid())))
                        else -> throw IllegalArgumentException("Unsupported private key algorithm ${Oids.name(oid)}")
                    }
                } else if (node.isSequence && node.children.size >= 9) {
                    PrivateKeyInfo.Rsa(parseRsaPrivateKey(der))
                } else {
                    PrivateKeyInfo.Ec(parseEcPrivateKey(der))
                }
            }
        }
    }

    fun parsePublicKey(text: String): PublicKeyInfo {
        val (der, label) = Der.fromPemOrDer(text)
        return if (label == "RSA PUBLIC KEY") PublicKeyInfo.Rsa(parseRsaPublicKey(der)) else parseSubjectPublicKeyInfo(der)
    }
}
