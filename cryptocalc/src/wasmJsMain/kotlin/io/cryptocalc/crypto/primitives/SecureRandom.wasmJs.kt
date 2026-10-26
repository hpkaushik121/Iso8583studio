package io.cryptocalc.crypto.primitives

import ai.cortex.core.hexToBytes

/**
 * Kotlin/Wasm JS interop only passes primitives and strings across the boundary, so the
 * browser fills a Uint8Array and hands the bytes back as a hex string.
 */
@Suppress("UNUSED_PARAMETER")
private fun randomHexFromBrowser(count: Int): String =
    js("Array.from(crypto.getRandomValues(new Uint8Array(count)), function (b) { return b.toString(16).padStart(2, '0'); }).join('')")

actual fun secureRandomBytes(count: Int): ByteArray {
    if (count <= 0) return ByteArray(0)
    val out = ByteArray(count)
    var done = 0
    // getRandomValues caps a single call at 65536 bytes.
    while (done < count) {
        val chunk = minOf(65536, count - done)
        randomHexFromBrowser(chunk).hexToBytes().copyInto(out, done)
        done += chunk
    }
    return out
}
