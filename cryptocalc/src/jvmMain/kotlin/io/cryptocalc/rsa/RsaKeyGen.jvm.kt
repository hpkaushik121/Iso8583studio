package io.cryptocalc.rsa

import io.cryptocalc.math.BigInt
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateCrtKey

actual suspend fun generateRsaKeyPair(bits: Int): RsaKeyPair {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(bits)
    val pair = generator.generateKeyPair()
    val priv = pair.private as RSAPrivateCrtKey
    fun conv(v: java.math.BigInteger) = BigInt.fromUnsignedBytes(v.toByteArray())
    return RsaKeyPair(
        RsaPrivateKey(
            n = conv(priv.modulus), e = conv(priv.publicExponent), d = conv(priv.privateExponent),
            p = conv(priv.primeP), q = conv(priv.primeQ),
            dP = conv(priv.primeExponentP), dQ = conv(priv.primeExponentQ), qInv = conv(priv.crtCoefficient),
        )
    )
}
