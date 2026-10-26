package io.cryptocalc.rsa

import io.cryptocalc.math.BigInt
import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

/**
 * WebCrypto generates the key (native speed) and exports it as a JWK; the components come
 * back as one JSON string because Kotlin/Wasm JS interop only passes primitives and strings.
 */
@Suppress("UNUSED_PARAMETER")
private fun generateRsaJwk(bits: Int): Promise<JsAny?> =
    js("crypto.subtle.generateKey({ name: 'RSA-OAEP', modulusLength: bits, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' }, true, ['encrypt', 'decrypt']).then(function (k) { return crypto.subtle.exportKey('jwk', k.privateKey); }).then(function (j) { return [j.n, j.e, j.d, j.p, j.q, j.dp, j.dq, j.qi].join('|'); })")

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun generateRsaKeyPair(bits: Int): RsaKeyPair {
    val joined = try {
        generateRsaJwk(bits).await<JsAny?>()?.toString()
    } catch (e: Throwable) {
        null
    }
    if (joined == null) return Rsa.generateKeyPairPure(bits)
    val parts = joined.split('|')
    require(parts.size == 8) { "Unexpected JWK export" }
    fun field(i: Int) = BigInt.fromUnsignedBytes(Base64.UrlSafe.decode(parts[i].padEnd((parts[i].length + 3) / 4 * 4, '=')))
    return RsaKeyPair(
        RsaPrivateKey(
            n = field(0), e = field(1), d = field(2), p = field(3), q = field(4),
            dP = field(5), dQ = field(6), qInv = field(7),
        )
    )
}
