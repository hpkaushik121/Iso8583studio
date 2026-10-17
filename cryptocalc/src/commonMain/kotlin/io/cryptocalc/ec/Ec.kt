package io.cryptocalc.ec

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.cryptocalc.crypto.primitives.Digests
import io.cryptocalc.math.BigInt
import io.cryptocalc.math.pmod

/** A short-Weierstrass prime curve y² = x³ + ax + b over GF(p) with base point G of order n. */
class EcCurve(
    val name: String,
    val p: BigInteger, val a: BigInteger, val b: BigInteger,
    val gx: BigInteger, val gy: BigInteger, val n: BigInteger, val h: Int = 1,
) {
    val byteLength: Int get() = (p.bitLength() + 7) / 8
    val g: EcPoint get() = EcPoint(gx, gy)

    companion object {
        private fun c(name: String, p: String, a: String, b: String, gx: String, gy: String, n: String, h: Int = 1) =
            EcCurve(name, BigInt.fromHex(p), BigInt.fromHex(a), BigInt.fromHex(b), BigInt.fromHex(gx), BigInt.fromHex(gy), BigInt.fromHex(n), h)

        val P256 = c("P-256",
            "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF",
            "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC",
            "5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B",
            "6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296",
            "4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5",
            "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551")
        val P384 = c("P-384",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFF",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFC",
            "B3312FA7E23EE7E4988E056BE3F82D19181D9C6EFE8141120314088F5013875AC656398D8A2ED19D2A85C8EDD3EC2AEF",
            "AA87CA22BE8B05378EB1C71EF320AD746E1D3B628BA79B9859F741E082542A385502F25DBF55296C3A545E3872760AB7",
            "3617DE4A96262C6F5D9E98BF9292DC29F8F41DBD289A147CE9DA3113B5F0B8C00A60B1CE1D7E819D7A431D7C90EA0E5F",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7634D81F4372DDF581A0DB248B0A77AECEC196ACCC52973")
        val P521 = c("P-521",
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC",
            "0051953EB9618E1C9A1F929A21A0B68540EEA2DA725B99B315F3B8B489918EF109E156193951EC7E937B1652C0BD3BB1BF073573DF883D2C34F1EF451FD46B503F00",
            "00C6858E06B70404E9CD9E3ECB662395B4429C648139053FB521F828AF606B4D3DBAA14B5E77EFE75928FE1DC127A2FFA8DE3348B3C1856A429BF97E7E31C2E5BD66",
            "011839296A789A3BC0045C8A5FB42C7D1BD998F54449579B446817AFBD17273E662C97EE72995EF42640C550B9013FAD0761353C7086A272C24088BE94769FD16650",
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFA51868783BF2F966B7FCC0148F709A5D03BB5C9B8899C47AEBB6FB71E91386409")
        val SECP256K1 = c("secp256k1",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F",
            "0", "7",
            "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798",
            "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141")
        val BRAINPOOL_P256R1 = c("brainpoolP256r1",
            "A9FB57DBA1EEA9BC3E660A909D838D726E3BF623D52620282013481D1F6E5377",
            "7D5A0975FC2C3057EEF67530417AFFE7FB8055C126DC5C6CE94A4B44F330B5D9",
            "26DC5C6CE94A4B44F330B5D9BBD77CBF958416295CF7E1CE6BCCDC18FF8C07B6",
            "8BD2AEB9CB7E57CB2C4B482FFC81B7AFB9DE27E1E3BD23C23A4453BD9ACE3262",
            "547EF835C3DAC4FD97F8461A14611DC9C27745132DED8E545C1D54C72F046997",
            "A9FB57DBA1EEA9BC3E660A909D838D718C397AA3B561A6F7901E0E82974856A7")
        val BRAINPOOL_P384R1 = c("brainpoolP384r1",
            "8CB91E82A3386D280F5D6F7E50E641DF152F7109ED5456B412B1DA197FB71123ACD3A729901D1A71874700133107EC53",
            "7BC382C63D8C150C3C72080ACE05AFA0C2BEA28E4FB22787139165EFBA91F90F8AA5814A503AD4EB04A8C7DD22CE2826",
            "04A8C7DD22CE28268B39B55416F0447C2FB77DE107DCD2A62E880EA53EEB62D57CB4390295DBC9943AB78696FA504C11",
            "1D1C64F068CF45FFA2A63A81B7C13F6B8847A3E77EF14FE3DB7FCAFE0CBD10E8E826E03436D646AAEF87B2E247D4AF1E",
            "8ABE1D7520F9C2A45CB1EB8E95CFD55262B70B29FEEC5864E19C054FF99129280E4646217791811142820341263C5315",
            "8CB91E82A3386D280F5D6F7E50E641DF152F7109ED5456B31F166E6CAC0425A7CF3AB6AF6B7FC3103B883202E9046565")
        val BRAINPOOL_P512R1 = c("brainpoolP512r1",
            "AADD9DB8DBE9C48B3FD4E6AE33C9FC07CB308DB3B3C9D20ED6639CCA703308717D4D9B009BC66842AECDA12AE6A380E62881FF2F2D82C68528AA6056583A48F3",
            "7830A3318B603B89E2327145AC234CC594CBDD8D3DF91610A83441CAEA9863BC2DED5D5AA8253AA10A2EF1C98B9AC8B57F1117A72BF2C7B9E7C1AC4D77FC94CA",
            "3DF91610A83441CAEA9863BC2DED5D5AA8253AA10A2EF1C98B9AC8B57F1117A72BF2C7B9E7C1AC4D77FC94CADC083E67984050B75EBAE5DD2809BD638016F723",
            "81AEE4BDD82ED9645A21322E9C4C6A9385ED9F70B5D916C1B43B62EEF4D0098EFF3B1F78E2D0D48D50D1687B93B97D5F7C6D5047406A5E688B352209BCB9F822",
            "7DDE385D566332ECC0EABFA9CF7822FDF209F70024A57B1AA000C55B881F8111B2DCDE494A5F485E5BCA4BD88A2763AED1CA2B2FA8F0540678CD1E0F3AD80892",
            "AADD9DB8DBE9C48B3FD4E6AE33C9FC07CB308DB3B3C9D20ED6639CCA70330870553E5C414CA92619418661197FAC10471DB1D381085DDADDB58796829CA90069")

        val ALL = listOf(P256, P384, P521, SECP256K1, BRAINPOOL_P256R1, BRAINPOOL_P384R1, BRAINPOOL_P512R1)

        /** Looks a curve up by any of its common names ("P-256", "prime256v1", "secp256r1", "NIST P-256 (prime256v1)" …). */
        fun byName(name: String): EcCurve {
            val n = name.lowercase().replace(" ", "").replace("-", "").replace("_", "")
            return when {
                n.contains("256k") -> SECP256K1
                n.contains("brainpoolp256") -> BRAINPOOL_P256R1
                n.contains("brainpoolp384") -> BRAINPOOL_P384R1
                n.contains("brainpoolp512") -> BRAINPOOL_P512R1
                n.contains("256") -> P256
                n.contains("384") -> P384
                n.contains("521") -> P521
                else -> throw IllegalArgumentException("Unknown curve: $name")
            }
        }
    }
}

/** Affine point; [infinity] is the point at infinity. */
class EcPoint(val x: BigInteger, val y: BigInteger, val infinity: Boolean = false) {
    companion object { val INFINITY = EcPoint(BigInt.ZERO, BigInt.ZERO, true) }
    override fun equals(other: Any?): Boolean = other is EcPoint && ((infinity && other.infinity) || (!infinity && !other.infinity && x == other.x && y == other.y))
    override fun hashCode(): Int = if (infinity) 0 else x.hashCode() * 31 + y.hashCode()
}

class EcKeyPair(val curve: EcCurve, val d: BigInteger, val q: EcPoint)

/**
 * Elliptic-curve arithmetic and ECDSA (FIPS 186-4) with RFC 6979 deterministic nonces, in pure
 * Kotlin. Affine coordinates with one modular inversion per operation: plenty for calculators.
 */
object Ec {

    fun isOnCurve(curve: EcCurve, point: EcPoint): Boolean {
        if (point.infinity) return true
        if (point.x.signum() < 0 || point.x >= curve.p || point.y.signum() < 0 || point.y >= curve.p) return false
        val lhs = (point.y * point.y).pmod(curve.p)
        val rhs = (point.x * point.x * point.x + curve.a * point.x + curve.b).pmod(curve.p)
        return lhs == rhs
    }

    fun add(curve: EcCurve, p1: EcPoint, p2: EcPoint): EcPoint {
        if (p1.infinity) return p2
        if (p2.infinity) return p1
        val p = curve.p
        if (p1.x == p2.x) {
            if ((p1.y + p2.y).pmod(p).isZero()) return EcPoint.INFINITY
            return double(curve, p1)
        }
        val lambda = ((p2.y - p1.y).pmod(p) * BigInt.modInverse((p2.x - p1.x).pmod(p), p)).pmod(p)
        val x3 = (lambda * lambda - p1.x - p2.x).pmod(p)
        val y3 = (lambda * (p1.x - x3) - p1.y).pmod(p)
        return EcPoint(x3, y3)
    }

    fun double(curve: EcCurve, pt: EcPoint): EcPoint {
        if (pt.infinity || pt.y.isZero()) return EcPoint.INFINITY
        val p = curve.p
        val three = BigInteger.fromInt(3)
        val lambda = ((three * pt.x * pt.x + curve.a).pmod(p) * BigInt.modInverse((BigInt.TWO * pt.y).pmod(p), p)).pmod(p)
        val x3 = (lambda * lambda - BigInt.TWO * pt.x).pmod(p)
        val y3 = (lambda * (pt.x - x3) - pt.y).pmod(p)
        return EcPoint(x3, y3)
    }

    fun multiply(curve: EcCurve, k: BigInteger, pt: EcPoint): EcPoint {
        var result = EcPoint.INFINITY
        var addend = pt
        var e = k
        while (e.signum() > 0) {
            if (e.bitAt(0)) result = add(curve, result, addend)
            e = e.shr(1)
            if (e.signum() > 0) addend = double(curve, addend)
        }
        return result
    }

    fun publicKey(curve: EcCurve, d: BigInteger): EcPoint = multiply(curve, d, curve.g)

    fun generateKeyPair(curve: EcCurve): EcKeyPair {
        val d = BigInt.randomBelow(curve.n)
        return EcKeyPair(curve, d, publicKey(curve, d))
    }

    fun keyPairFromPrivate(curve: EcCurve, d: BigInteger): EcKeyPair {
        require(d.signum() > 0 && d < curve.n) { "Private key must be in [1, n-1]" }
        return EcKeyPair(curve, d, publicKey(curve, d))
    }

    // ------------------------------------------------------------------ SEC 1 encoding

    fun encodePoint(curve: EcCurve, pt: EcPoint, compressed: Boolean = false): ByteArray {
        require(!pt.infinity) { "Cannot encode the point at infinity" }
        val len = curve.byteLength
        val x = BigInt.toUnsignedBytes(pt.x, len)
        return if (compressed) {
            byteArrayOf(if (pt.y.bitAt(0)) 0x03 else 0x02) + x
        } else {
            byteArrayOf(0x04) + x + BigInt.toUnsignedBytes(pt.y, len)
        }
    }

    fun decodePoint(curve: EcCurve, encoded: ByteArray): EcPoint {
        require(encoded.isNotEmpty()) { "Empty point" }
        val len = curve.byteLength
        return when (encoded[0].toInt()) {
            0x04 -> {
                require(encoded.size == 1 + 2 * len) { "Uncompressed point must be ${1 + 2 * len} bytes" }
                EcPoint(BigInt.fromUnsignedBytes(encoded.copyOfRange(1, 1 + len)), BigInt.fromUnsignedBytes(encoded.copyOfRange(1 + len, 1 + 2 * len)))
            }
            0x02, 0x03 -> {
                require(encoded.size == 1 + len) { "Compressed point must be ${1 + len} bytes" }
                val x = BigInt.fromUnsignedBytes(encoded.copyOfRange(1, 1 + len))
                val rhs = (x * x * x + curve.a * x + curve.b).pmod(curve.p)
                val y = sqrtMod(rhs, curve.p) ?: throw IllegalArgumentException("Point is not on the curve")
                val wantOdd = encoded[0].toInt() == 0x03
                EcPoint(x, if (y.bitAt(0) == wantOdd) y else (curve.p - y).pmod(curve.p))
            }
            else -> throw IllegalArgumentException("Unsupported point encoding ${encoded[0]}")
        }
    }

    /** Square root modulo a prime p ≡ 3 (mod 4) (all supported curves) via a^((p+1)/4). */
    private fun sqrtMod(a: BigInteger, p: BigInteger): BigInteger? {
        val r = BigInt.modPow(a, (p + BigInt.ONE).shr(2), p)
        return if ((r * r).pmod(p) == a.pmod(p)) r else null
    }

    // ------------------------------------------------------------------ ECDSA

    class Signature(val r: BigInteger, val s: BigInteger) {
        /** DER `ECDSA-Sig-Value ::= SEQUENCE { r INTEGER, s INTEGER }`. */
        fun toDer(): ByteArray {
            fun int(v: BigInteger): ByteArray {
                var b = BigInt.toUnsignedBytes(v)
                if (b[0].toInt() and 0x80 != 0) b = byteArrayOf(0) + b
                return byteArrayOf(0x02) + derLength(b.size) + b
            }
            val body = int(r) + int(s)
            return byteArrayOf(0x30) + derLength(body.size) + body
        }

        /** Raw r ‖ s, each padded to the curve size. */
        fun toRaw(curve: EcCurve): ByteArray = BigInt.toUnsignedBytes(r, curve.byteLength) + BigInt.toUnsignedBytes(s, curve.byteLength)

        companion object {
            fun fromDer(der: ByteArray): Signature {
                require(der.size > 2 && der[0] == 0x30.toByte()) { "Not a DER ECDSA signature" }
                var pos = 1
                val (seqLen, l1) = derReadLength(der, pos); pos += l1
                require(pos + seqLen <= der.size) { "Truncated DER signature" }
                fun readInt(): BigInteger {
                    require(der[pos] == 0x02.toByte()) { "Expected INTEGER" }
                    pos++
                    val (len, ll) = derReadLength(der, pos); pos += ll
                    val v = BigInt.fromUnsignedBytes(der.copyOfRange(pos, pos + len)); pos += len
                    return v
                }
                val r = readInt(); val s = readInt()
                return Signature(r, s)
            }

            fun fromRaw(curve: EcCurve, raw: ByteArray): Signature {
                require(raw.size == 2 * curve.byteLength) { "Raw signature must be ${2 * curve.byteLength} bytes" }
                return Signature(BigInt.fromUnsignedBytes(raw.copyOfRange(0, curve.byteLength)), BigInt.fromUnsignedBytes(raw.copyOfRange(curve.byteLength, raw.size)))
            }
        }
    }

    internal fun derLength(n: Int): ByteArray = when {
        n < 0x80 -> byteArrayOf(n.toByte())
        n < 0x100 -> byteArrayOf(0x81.toByte(), n.toByte())
        else -> byteArrayOf(0x82.toByte(), (n ushr 8).toByte(), n.toByte())
    }

    internal fun derReadLength(data: ByteArray, pos: Int): Pair<Int, Int> {
        val first = data[pos].toInt() and 0xFF
        if (first < 0x80) return first to 1
        val count = first and 0x7F
        var len = 0
        for (i in 1..count) len = (len shl 8) or (data[pos + i].toInt() and 0xFF)
        return len to (1 + count)
    }

    /** Hash truncated/reduced to the group size as in FIPS 186-4 §6.4 (leftmost bits of the digest). */
    private fun hashToInt(curve: EcCurve, digest: ByteArray): BigInteger {
        val nBits = curve.n.bitLength()
        var e = BigInt.fromUnsignedBytes(digest)
        val excess = digest.size * 8 - nBits
        if (excess > 0) e = e.shr(excess)
        return e
    }

    /** RFC 6979 deterministic nonce using HMAC-SHA256 (independent of the signing hash). */
    private fun deterministicK(curve: EcCurve, d: BigInteger, h1: BigInteger): BigInteger {
        val qlen = curve.n.bitLength()
        val rlen = (qlen + 7) / 8
        fun bits2octets(v: BigInteger): ByteArray = BigInt.toUnsignedBytes(v.pmod(curve.n), rlen)
        val x = BigInt.toUnsignedBytes(d, rlen)
        val hm = bits2octets(h1)
        var v = ByteArray(32) { 0x01 }
        var k = ByteArray(32)
        k = Digests.hmacSha256(k, v + byteArrayOf(0x00) + x + hm)
        v = Digests.hmacSha256(k, v)
        k = Digests.hmacSha256(k, v + byteArrayOf(0x01) + x + hm)
        v = Digests.hmacSha256(k, v)
        while (true) {
            var t = ByteArray(0)
            while (t.size < rlen) {
                v = Digests.hmacSha256(k, v)
                t += v
            }
            var candidate = BigInt.fromUnsignedBytes(t.copyOf(rlen))
            val excess = rlen * 8 - qlen
            if (excess > 0) candidate = candidate.shr(excess)
            if (candidate.signum() > 0 && candidate < curve.n) return candidate
            k = Digests.hmacSha256(k, v + byteArrayOf(0x00))
            v = Digests.hmacSha256(k, v)
        }
    }

    /** Signs an already computed [digest]. Deterministic (RFC 6979) unless [randomK] is set. */
    fun signDigest(key: EcKeyPair, digest: ByteArray, randomK: Boolean = false): Signature {
        val curve = key.curve
        val e = hashToInt(curve, digest)
        while (true) {
            val k = if (randomK) BigInt.randomBelow(curve.n) else deterministicK(curve, key.d, e)
            val kg = multiply(curve, k, curve.g)
            val r = kg.x.pmod(curve.n)
            if (r.isZero()) continue
            val s = (BigInt.modInverse(k, curve.n) * (e + r * key.d)).pmod(curve.n)
            if (s.isZero()) continue
            return Signature(r, s)
        }
    }

    fun sign(key: EcKeyPair, hash: String, message: ByteArray): Signature = signDigest(key, Digests.hash(hash, message))

    fun verifyDigest(curve: EcCurve, q: EcPoint, digest: ByteArray, sig: Signature): Boolean {
        if (sig.r.signum() <= 0 || sig.r >= curve.n || sig.s.signum() <= 0 || sig.s >= curve.n) return false
        if (!isOnCurve(curve, q) || q.infinity) return false
        val e = hashToInt(curve, digest)
        val w = BigInt.modInverse(sig.s, curve.n)
        val u1 = (e * w).pmod(curve.n)
        val u2 = (sig.r * w).pmod(curve.n)
        val pt = add(curve, multiply(curve, u1, curve.g), multiply(curve, u2, q))
        if (pt.infinity) return false
        return pt.x.pmod(curve.n) == sig.r
    }

    fun verify(curve: EcCurve, q: EcPoint, hash: String, message: ByteArray, sig: Signature): Boolean =
        verifyDigest(curve, q, Digests.hash(hash, message), sig)
}
