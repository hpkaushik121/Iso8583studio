package io.cryptocalc.crypto.primitives

import ai.cortex.core.toHexUpper

/**
 * Cryptographically secure random bytes from the platform: `java.security.SecureRandom` on the
 * JVM, `crypto.getRandomValues` in the browser. This is the only piece of the crypto layer
 * that is not pure Kotlin.
 */
expect fun secureRandomBytes(count: Int): ByteArray

/** Upper-case hex of [count] random bytes. */
fun secureRandomHex(count: Int): String = secureRandomBytes(count).toHexUpper()
