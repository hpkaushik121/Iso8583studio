package io.cryptocalc.crypto.primitives

import org.kotlincrypto.hash.md.MD5
import org.kotlincrypto.hash.sha1.SHA1
import org.kotlincrypto.hash.sha2.SHA224
import org.kotlincrypto.hash.sha2.SHA256
import org.kotlincrypto.hash.sha2.SHA384
import org.kotlincrypto.hash.sha2.SHA512
import org.kotlincrypto.macs.hmac.md.HmacMD5
import org.kotlincrypto.macs.hmac.sha1.HmacSHA1
import org.kotlincrypto.macs.hmac.sha2.HmacSHA224
import org.kotlincrypto.macs.hmac.sha2.HmacSHA256
import org.kotlincrypto.macs.hmac.sha2.HmacSHA384
import org.kotlincrypto.macs.hmac.sha2.HmacSHA512

/**
 * Hash and HMAC facade. The implementations come from KotlinCrypto (pure Kotlin, multiplatform);
 * this object only gives the tools one place to look algorithms up by name.
 */
object Digests {
    val HASH_ALGORITHMS: List<String> = listOf("MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512")

    fun md5(data: ByteArray): ByteArray = MD5().digest(data)
    fun sha1(data: ByteArray): ByteArray = SHA1().digest(data)
    fun sha224(data: ByteArray): ByteArray = SHA224().digest(data)
    fun sha256(data: ByteArray): ByteArray = SHA256().digest(data)
    fun sha384(data: ByteArray): ByteArray = SHA384().digest(data)
    fun sha512(data: ByteArray): ByteArray = SHA512().digest(data)

    /** Hash by JCA-style name ("SHA-256", "sha256", "SHA256" all accepted). */
    fun hash(algorithm: String, data: ByteArray): ByteArray = when (normalize(algorithm)) {
        "MD5" -> md5(data)
        "SHA1" -> sha1(data)
        "SHA224" -> sha224(data)
        "SHA256" -> sha256(data)
        "SHA384" -> sha384(data)
        "SHA512" -> sha512(data)
        else -> throw IllegalArgumentException("Unsupported hash algorithm: $algorithm")
    }

    /** Output size in bytes for a supported hash name. */
    fun digestSize(algorithm: String): Int = when (normalize(algorithm)) {
        "MD5" -> 16
        "SHA1" -> 20
        "SHA224" -> 28
        "SHA256" -> 32
        "SHA384" -> 48
        "SHA512" -> 64
        else -> throw IllegalArgumentException("Unsupported hash algorithm: $algorithm")
    }

    fun hmacMd5(key: ByteArray, data: ByteArray): ByteArray = HmacMD5(key).doFinal(data)
    fun hmacSha1(key: ByteArray, data: ByteArray): ByteArray = HmacSHA1(key).doFinal(data)
    fun hmacSha224(key: ByteArray, data: ByteArray): ByteArray = HmacSHA224(key).doFinal(data)
    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray = HmacSHA256(key).doFinal(data)
    fun hmacSha384(key: ByteArray, data: ByteArray): ByteArray = HmacSHA384(key).doFinal(data)
    fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray = HmacSHA512(key).doFinal(data)

    /** HMAC by hash name ("SHA-256") or JCA mac name ("HmacSHA256"). */
    fun hmac(algorithm: String, key: ByteArray, data: ByteArray): ByteArray = when (normalize(algorithm)) {
        "MD5" -> hmacMd5(key, data)
        "SHA1" -> hmacSha1(key, data)
        "SHA224" -> hmacSha224(key, data)
        "SHA256" -> hmacSha256(key, data)
        "SHA384" -> hmacSha384(key, data)
        "SHA512" -> hmacSha512(key, data)
        else -> throw IllegalArgumentException("Unsupported HMAC algorithm: $algorithm")
    }

    private fun normalize(name: String): String =
        name.uppercase().removePrefix("HMAC").replace("-", "").replace("_", "").replace("WITH", "")
}
