package io.cryptocalc.hsm

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.cryptocalc.asn1.Der
import io.cryptocalc.math.BigInt
import io.cryptocalc.rsa.RsaPublicKey

/**
 * payShield public-key encodings for RSA keys (Host Programmer's Manual, "Public Key Encoding"):
 *
 * - Encoding 01: DER `RSAPublicKey` with INTEGERs in unsigned representation (no leading 0x00
 *   even when the top bit is set).
 * - Encoding 02: standard DER two's complement INTEGERs (a leading 0x00 when the top bit is set).
 */
object ThalesRsaEncoding {

    private fun unsignedInteger(value: BigInteger): ByteArray = Der.tlv(Der.INTEGER, BigInt.toUnsignedBytes(value))

    fun rsaPublicKeyDer(key: RsaPublicKey, unsigned: Boolean): ByteArray =
        if (unsigned) Der.sequence(unsignedInteger(key.n), unsignedInteger(key.e))
        else Der.sequence(Der.integer(key.n), Der.integer(key.e))
}
