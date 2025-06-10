package ai.cortex.core.crypto

import ai.cortex.core.crypto.data.KeyParity


/**
 * Converts a HEX string to a ByteArray.
 */
fun String.hexToByteArray(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

/**
 * Converts a ByteArray to a HEX string.
 */
fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}

/**
 * Adjusts the parity of a key.
 */
fun ByteArray.adjustParity(parity: KeyParity): ByteArray {
    if (parity == KeyParity.NONE) {
        return this
    }
    val wantOdd = parity == KeyParity.RIGHT_ODD
    return this.map { byte ->
        var b = byte.toInt()
        val bitCount = Integer.bitCount(b and 0xFF)
        val hasOddParity = bitCount % 2 != 0

        if ((wantOdd && !hasOddParity) || (!wantOdd && hasOddParity)) {
            b = b xor 1 // Flip the LSB to change parity
        }
        b.toByte()
    }.toByteArray()
}


/**
 * Calculate MAC using various algorithms
 */
fun calculateMac(
    dataHex: String,
    keyHex: String,
    algorithm: String = "3DES"
): String {
    val data = dataHex.hexToByteArray()
    val key = keyHex.hexToByteArray()

    val macBytes = when (algorithm.uppercase()) {
        "3DES" -> MacCalculator.calculate3DesMac(
            data, key, getSymmetricCipher()
        )
        else -> throw IllegalArgumentException("Unsupported MAC algorithm: $algorithm")
    }

    return macBytes.toHexString().uppercase()
}

/**
 * Encrypt data using 3DES
 */
fun encrypt3Des(
    dataHex: String,
    keyHex: String,
    mode: String = "ECB"
): String {
    val data = dataHex.hexToByteArray()
    val key = keyHex.hexToByteArray()
    val cipher = getSymmetricCipher()

    val encryptedBytes = when (mode.uppercase()) {
        "ECB" -> cipher.encryptEcb(data, key)
        "CBC" -> cipher.encrypt(data, key)
        else -> throw IllegalArgumentException("Unsupported mode: $mode")
    }

    return encryptedBytes.toHexString().uppercase()
}

/**
 * Decrypt data using 3DES
 */
fun decrypt3Des(
    dataHex: String,
    keyHex: String,
    mode: String = "ECB"
): String {
    val data = dataHex.hexToByteArray()
    val key = keyHex.hexToByteArray()
    val cipher = getSymmetricCipher()

    val decryptedBytes = when (mode.uppercase()) {
        "ECB" -> cipher.decryptEcb(data, key)
        "CBC" -> cipher.decrypt(data, key)
        else -> throw IllegalArgumentException("Unsupported mode: $mode")
    }

    return decryptedBytes.toHexString().uppercase()
}
