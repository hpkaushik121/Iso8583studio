package io.cryptocalc.crypto.primitives

import java.security.SecureRandom

private val rng: SecureRandom by lazy { SecureRandom() }

actual fun secureRandomBytes(count: Int): ByteArray = ByteArray(count).also { rng.nextBytes(it) }
