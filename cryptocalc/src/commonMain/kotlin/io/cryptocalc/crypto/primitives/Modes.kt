package io.cryptocalc.crypto.primitives

import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.PaddingMethods

/**
 * Block cipher modes of operation (ECB, CBC, CFB, OFB) and the padding schemes the tools expose.
 * CFB and OFB are full-block variants (CFB-64 for DES, CFB-128 for AES), which is what the
 * JCE "CFB"/"OFB" transformations the desktop app used to call produce.
 */
object Modes {

    /** Apply [padding] to [data] for a cipher with [blockSize]. Unknown/irrelevant methods are treated as NONE. */
    fun pad(data: ByteArray, blockSize: Int, padding: PaddingMethods): ByteArray {
        val rem = data.size % blockSize
        return when (padding) {
            PaddingMethods.NONE -> data
            PaddingMethods.METHOD_1_ISO_9797, PaddingMethods.ZERO_PADDING, PaddingMethods.NULL_PADDING ->
                if (rem == 0) data else data.copyOf(data.size + (blockSize - rem))
            PaddingMethods.METHOD_2_ISO_9797, PaddingMethods.ISO_7816_PADDING, PaddingMethods.EMV_PADDING_METHOD_2,
            PaddingMethods.MAC_PADDING_ISO_9797_1 -> {
                val out = data.copyOf(data.size + (blockSize - rem))
                out[data.size] = 0x80.toByte()
                out
            }
            PaddingMethods.METHOD_3_ISO_9797 -> {
                // 64-bit (or 128-bit) big-endian bit length block, then data, then zeros
                val lenBlock = ByteArray(blockSize)
                val bits = data.size.toLong() * 8
                for (i in 0 until 8) lenBlock[blockSize - 1 - i] = (bits ushr (8 * i)).toByte()
                val body = if (rem == 0) data else data.copyOf(data.size + (blockSize - rem))
                lenBlock + body
            }
            PaddingMethods.PKCS5, PaddingMethods.PKCS7, PaddingMethods.AES_CBC_PADDING,
            PaddingMethods.DES_CBC_PADDING, PaddingMethods.TRIPLE_DES_PADDING -> {
                val n = blockSize - rem
                val out = data.copyOf(data.size + n)
                for (i in data.size until out.size) out[i] = n.toByte()
                out
            }
            PaddingMethods.ANSI_X923 -> {
                val n = blockSize - rem
                val out = data.copyOf(data.size + n)
                out[out.size - 1] = n.toByte()
                out
            }
            PaddingMethods.ISO_10126 -> {
                val n = blockSize - rem
                val out = data.copyOf(data.size + n)
                val rnd = secureRandomBytes(n - 1)
                rnd.copyInto(out, data.size)
                out[out.size - 1] = n.toByte()
                out
            }
            else -> data
        }
    }

    /** Remove [padding] previously applied by [pad]. Returns [data] unchanged for methods that cannot be reversed. */
    fun unpad(data: ByteArray, blockSize: Int, padding: PaddingMethods): ByteArray {
        if (data.isEmpty()) return data
        return when (padding) {
            PaddingMethods.METHOD_1_ISO_9797, PaddingMethods.ZERO_PADDING, PaddingMethods.NULL_PADDING -> {
                var end = data.size
                while (end > 0 && data[end - 1] == 0.toByte()) end--
                data.copyOf(end)
            }
            PaddingMethods.METHOD_2_ISO_9797, PaddingMethods.ISO_7816_PADDING, PaddingMethods.EMV_PADDING_METHOD_2,
            PaddingMethods.MAC_PADDING_ISO_9797_1 -> {
                var end = data.size
                while (end > 0 && data[end - 1] == 0.toByte()) end--
                if (end > 0 && data[end - 1] == 0x80.toByte()) end-- else return data
                data.copyOf(end)
            }
            PaddingMethods.METHOD_3_ISO_9797 -> {
                if (data.size < blockSize) return data
                var bits = 0L
                for (i in 0 until 8) bits = (bits shl 8) or (data[blockSize - 8 + i].toLong() and 0xFF)
                val len = (bits / 8).toInt()
                if (len < 0 || blockSize + len > data.size) return data
                data.copyOfRange(blockSize, blockSize + len)
            }
            PaddingMethods.PKCS5, PaddingMethods.PKCS7, PaddingMethods.AES_CBC_PADDING,
            PaddingMethods.DES_CBC_PADDING, PaddingMethods.TRIPLE_DES_PADDING, PaddingMethods.ANSI_X923,
            PaddingMethods.ISO_10126 -> {
                val n = data.last().toInt() and 0xFF
                require(n in 1..blockSize && n <= data.size) { "Invalid padding" }
                data.copyOf(data.size - n)
            }
            else -> data
        }
    }

    fun encrypt(cipher: BlockCipher, data: ByteArray, mode: CipherMode, iv: ByteArray? = null,
                padding: PaddingMethods = PaddingMethods.NONE): ByteArray {
        val bs = cipher.blockSize
        val input = pad(data, bs, padding)
        return when (mode) {
            CipherMode.ECB -> {
                requireMultiple(input.size, bs)
                val out = ByteArray(input.size)
                for (off in input.indices step bs) cipher.encryptBlock(input, off, out, off)
                out
            }
            CipherMode.CBC -> {
                requireMultiple(input.size, bs)
                val out = ByteArray(input.size)
                var prev = checkIv(iv, bs)
                val x = ByteArray(bs)
                for (off in input.indices step bs) {
                    for (j in 0 until bs) x[j] = (input[off + j].toInt() xor prev[j].toInt()).toByte()
                    cipher.encryptBlock(x, 0, out, off)
                    prev = out.copyOfRange(off, off + bs)
                }
                out
            }
            CipherMode.CFB -> {
                val out = ByteArray(input.size)
                var feedback = checkIv(iv, bs)
                val ks = ByteArray(bs)
                var off = 0
                while (off < input.size) {
                    cipher.encryptBlock(feedback, 0, ks, 0)
                    val n = minOf(bs, input.size - off)
                    for (j in 0 until n) out[off + j] = (input[off + j].toInt() xor ks[j].toInt()).toByte()
                    feedback = if (n == bs) out.copyOfRange(off, off + bs) else out.copyOfRange(off, off + n) + ks.copyOfRange(n, bs)
                    off += n
                }
                out
            }
            CipherMode.OFB -> {
                val out = ByteArray(input.size)
                var ks = checkIv(iv, bs)
                var off = 0
                while (off < input.size) {
                    ks = cipher.encryptBlock(ks)
                    val n = minOf(bs, input.size - off)
                    for (j in 0 until n) out[off + j] = (input[off + j].toInt() xor ks[j].toInt()).toByte()
                    off += n
                }
                out
            }
            CipherMode.CTR -> {
                val out = ByteArray(input.size)
                val counter = checkIv(iv, bs).copyOf()
                val ks = ByteArray(bs)
                var off = 0
                while (off < input.size) {
                    cipher.encryptBlock(counter, 0, ks, 0)
                    val n = minOf(bs, input.size - off)
                    for (j in 0 until n) out[off + j] = (input[off + j].toInt() xor ks[j].toInt()).toByte()
                    incrementCounter(counter)
                    off += n
                }
                out
            }
            CipherMode.GCM -> throw UnsupportedOperationException("GCM is not supported by this engine")
        }
    }

    fun decrypt(cipher: BlockCipher, data: ByteArray, mode: CipherMode, iv: ByteArray? = null,
                padding: PaddingMethods = PaddingMethods.NONE): ByteArray {
        val bs = cipher.blockSize
        val out = when (mode) {
            CipherMode.ECB -> {
                requireMultiple(data.size, bs)
                val o = ByteArray(data.size)
                for (off in data.indices step bs) cipher.decryptBlock(data, off, o, off)
                o
            }
            CipherMode.CBC -> {
                requireMultiple(data.size, bs)
                val o = ByteArray(data.size)
                var prev = checkIv(iv, bs)
                val x = ByteArray(bs)
                for (off in data.indices step bs) {
                    cipher.decryptBlock(data, off, x, 0)
                    for (j in 0 until bs) o[off + j] = (x[j].toInt() xor prev[j].toInt()).toByte()
                    prev = data.copyOfRange(off, off + bs)
                }
                o
            }
            CipherMode.CFB -> {
                val o = ByteArray(data.size)
                var feedback = checkIv(iv, bs)
                val ks = ByteArray(bs)
                var off = 0
                while (off < data.size) {
                    cipher.encryptBlock(feedback, 0, ks, 0)
                    val n = minOf(bs, data.size - off)
                    for (j in 0 until n) o[off + j] = (data[off + j].toInt() xor ks[j].toInt()).toByte()
                    feedback = if (n == bs) data.copyOfRange(off, off + bs) else data.copyOfRange(off, off + n) + ks.copyOfRange(n, bs)
                    off += n
                }
                o
            }
            // OFB and CTR are symmetric
            CipherMode.OFB, CipherMode.CTR -> encrypt(cipher, data, mode, iv, PaddingMethods.NONE)
            CipherMode.GCM -> throw UnsupportedOperationException("GCM is not supported by this engine")
        }
        return unpad(out, bs, padding)
    }

    private fun requireMultiple(size: Int, bs: Int) {
        require(size % bs == 0) { "Data length $size is not a multiple of the block size $bs (choose a padding method)" }
    }

    private fun checkIv(iv: ByteArray?, bs: Int): ByteArray {
        if (iv == null) return ByteArray(bs)
        require(iv.size == bs) { "IV must be $bs bytes, got ${iv.size}" }
        return iv
    }

    private fun incrementCounter(c: ByteArray) {
        for (i in c.indices.reversed()) {
            c[i] = (c[i] + 1).toByte()
            if (c[i] != 0.toByte()) break
        }
    }
}
