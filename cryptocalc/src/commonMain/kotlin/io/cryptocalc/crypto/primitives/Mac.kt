package io.cryptocalc.crypto.primitives

import ai.cortex.core.types.PaddingMethods

/**
 * ISO/IEC 9797-1 block-cipher MACs and the payment-industry names layered on top of them:
 *
 * - Algorithm 1: plain CBC-MAC (ANSI X9.9 with single DES, AS2805.4.1 Method 1).
 * - Algorithm 2: Algorithm 1 followed by one extra encryption with K'.
 * - Algorithm 3: "Retail MAC" (ANSI X9.19, AS2805.4.1 Method 2): DES-CBC under K, then
 *   decrypt with K' and encrypt with K.
 * - Algorithm 4: initial transformation of the first block with K'', final encryption with K'.
 * - Algorithm 5: two Algorithm-1 MACs (keys K and K') XORed together.
 * - Algorithm 6: two Algorithm-4 MACs with two key sets XORed together.
 *
 * Padding methods 1, 2 and 3 are those of ISO 9797-1. Key check values are here too.
 */
object Mac {

    enum class Padding { METHOD_1, METHOD_2, METHOD_3 }

    private fun toPaddingMethod(p: Padding): PaddingMethods = when (p) {
        Padding.METHOD_1 -> PaddingMethods.METHOD_1_ISO_9797
        Padding.METHOD_2 -> PaddingMethods.METHOD_2_ISO_9797
        Padding.METHOD_3 -> PaddingMethods.METHOD_3_ISO_9797
    }

    /** CBC-MAC core: returns the final block (no padding applied here). */
    private fun cbcMac(cipher: BlockCipher, padded: ByteArray, iv: ByteArray? = null): ByteArray {
        val bs = cipher.blockSize
        require(padded.isNotEmpty() && padded.size % bs == 0) { "MAC input must be a non-empty multiple of $bs bytes" }
        var h = iv?.copyOf() ?: ByteArray(bs)
        val x = ByteArray(bs)
        for (off in padded.indices step bs) {
            for (j in 0 until bs) x[j] = (h[j].toInt() xor padded[off + j].toInt()).toByte()
            h = cipher.encryptBlock(x)
        }
        return h
    }

    /** Algorithm 1. [cipher] may be single DES, TDES or AES. */
    fun algorithm1(cipher: BlockCipher, data: ByteArray, padding: Padding = Padding.METHOD_1, iv: ByteArray? = null): ByteArray =
        cbcMac(cipher, Modes.pad(data, cipher.blockSize, toPaddingMethod(padding)), iv)

    /** Algorithm 2: Algorithm 1 under [k], then one more encryption under [kPrime]. */
    fun algorithm2(k: BlockCipher, kPrime: BlockCipher, data: ByteArray, padding: Padding = Padding.METHOD_1): ByteArray =
        kPrime.encryptBlock(algorithm1(k, data, padding))

    /**
     * Algorithm 3 (Retail MAC). [k] and [kPrime] are the two single-length keys; the chain runs
     * under K, the last block is decrypted under K' and encrypted again under K.
     */
    fun algorithm3(k: BlockCipher, kPrime: BlockCipher, data: ByteArray, padding: Padding = Padding.METHOD_1): ByteArray {
        val h = algorithm1(k, data, padding)
        return k.encryptBlock(kPrime.decryptBlock(h))
    }

    /** Algorithm 4: first block transformed with K'' after K, final block encrypted with K'. */
    fun algorithm4(k: BlockCipher, kPrime: BlockCipher, kDoublePrime: BlockCipher, data: ByteArray,
                   padding: Padding = Padding.METHOD_1): ByteArray {
        val bs = k.blockSize
        val padded = Modes.pad(data, bs, toPaddingMethod(padding))
        require(padded.size >= bs) { "MAC input must be at least one block" }
        // Initial transformation: H1 = e_K''(e_K(D1))
        var h = kDoublePrime.encryptBlock(k.encryptBlock(padded.copyOfRange(0, bs)))
        if (padded.size > bs) h = cbcMac(k, padded.copyOfRange(bs, padded.size), h)
        return kPrime.encryptBlock(h)
    }

    /** Algorithm 5: MAC1(K) XOR MAC1(K'). */
    fun algorithm5(k: BlockCipher, kPrime: BlockCipher, data: ByteArray, padding: Padding = Padding.METHOD_1): ByteArray {
        val a = algorithm1(k, data, padding)
        val b = algorithm1(kPrime, data, padding)
        return ByteArray(a.size) { (a[it].toInt() xor b[it].toInt()).toByte() }
    }

    /** Algorithm 6: MAC4(K, K', K'') XOR MAC4(K2, K2', K2''). */
    fun algorithm6(k: BlockCipher, kPrime: BlockCipher, kDoublePrime: BlockCipher,
                   k2: BlockCipher, k2Prime: BlockCipher, k2DoublePrime: BlockCipher,
                   data: ByteArray, padding: Padding = Padding.METHOD_1): ByteArray {
        val a = algorithm4(k, kPrime, kDoublePrime, data, padding)
        val b = algorithm4(k2, k2Prime, k2DoublePrime, data, padding)
        return ByteArray(a.size) { (a[it].toInt() xor b[it].toInt()).toByte() }
    }

    /** ANSI X9.9: single-DES CBC-MAC with zero padding. [key] is 8 bytes. */
    fun ansiX99(key: ByteArray, data: ByteArray): ByteArray = algorithm1(Des(key), data, Padding.METHOD_1)

    /** ANSI X9.19 / Retail MAC with a 16-byte double-length key (KL ‖ KR). */
    fun ansiX919(key: ByteArray, data: ByteArray, padding: Padding = Padding.METHOD_1): ByteArray {
        require(key.size == 16) { "Retail MAC requires a 16-byte key" }
        return algorithm3(Des(key.copyOfRange(0, 8)), Des(key.copyOfRange(8, 16)), data, padding)
    }

    /** CBC-MAC with full TDES on every block (ISO 9797-1 Algorithm 1 with a TDES cipher). */
    fun tdesCbcMac(key: ByteArray, data: ByteArray, padding: Padding = Padding.METHOD_1): ByteArray =
        algorithm1(Tdes(key), data, padding)

    /**
     * EMV application-cryptogram MAC (EMV 4.x Book 2 Annex A1.2): ISO 9797-1 Algorithm 3 with
     * the 16-byte session key split into KL/KR and the requested [macLength] (4..8 bytes).
     */
    fun emvMac(sessionKey: ByteArray, data: ByteArray, padding: Padding = Padding.METHOD_2, macLength: Int = 8): ByteArray {
        require(sessionKey.size == 16) { "EMV MAC requires a 16-byte session key" }
        require(macLength in 4..8) { "MAC length must be 4..8 bytes" }
        return ansiX919(sessionKey, data, padding).copyOf(macLength)
    }

    /** Key check value: encrypt a zero block under the key, return the first [length] bytes (3 by default). */
    fun kcvTdes(key: ByteArray, length: Int = 3): ByteArray = Tdes(key).encryptBlock(ByteArray(8)).copyOf(length)

    /** AES key check value (ANSI X9.24-3 / TR-31 "CMAC" style is different; this is the encrypt-zeros KCV). */
    fun kcvAes(key: ByteArray, length: Int = 3): ByteArray = Aes(key).encryptBlock(ByteArray(16)).copyOf(length)

    /** AES CMAC-based KCV as defined in TR-31 / X9.24-3: CMAC over one zero block, truncated. */
    fun kcvAesCmac(key: ByteArray, length: Int = 3): ByteArray = Cmac(Aes(key)).compute(ByteArray(16)).copyOf(length)
}
