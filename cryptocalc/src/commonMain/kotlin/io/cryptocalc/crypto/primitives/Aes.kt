package io.cryptocalc.crypto.primitives

/**
 * AES (FIPS 197) block cipher for 128/192/256-bit keys. Pure Kotlin, byte-oriented
 * implementation (S-box + xtime), fast enough for calculator workloads on every platform.
 */
class Aes(key: ByteArray) : BlockCipher {
    override val blockSize: Int get() = 16

    private val rounds: Int
    private val roundKeys: ByteArray // (rounds + 1) * 16 bytes

    init {
        require(key.size == 16 || key.size == 24 || key.size == 32) {
            "AES key must be 16, 24 or 32 bytes, got ${key.size}"
        }
        val nk = key.size / 4
        rounds = nk + 6
        roundKeys = expandKey(key, nk, rounds)
    }

    override fun encryptBlock(src: ByteArray, srcOff: Int, dst: ByteArray, dstOff: Int) {
        val s = ByteArray(16)
        src.copyInto(s, 0, srcOff, srcOff + 16)
        addRoundKey(s, 0)
        for (r in 1 until rounds) {
            subBytes(s, SBOX)
            shiftRows(s)
            mixColumns(s)
            addRoundKey(s, r)
        }
        subBytes(s, SBOX)
        shiftRows(s)
        addRoundKey(s, rounds)
        s.copyInto(dst, dstOff)
    }

    override fun decryptBlock(src: ByteArray, srcOff: Int, dst: ByteArray, dstOff: Int) {
        val s = ByteArray(16)
        src.copyInto(s, 0, srcOff, srcOff + 16)
        addRoundKey(s, rounds)
        for (r in rounds - 1 downTo 1) {
            invShiftRows(s)
            subBytes(s, INV_SBOX)
            addRoundKey(s, r)
            invMixColumns(s)
        }
        invShiftRows(s)
        subBytes(s, INV_SBOX)
        addRoundKey(s, 0)
        s.copyInto(dst, dstOff)
    }

    private fun addRoundKey(s: ByteArray, round: Int) {
        val off = round * 16
        for (i in 0 until 16) s[i] = (s[i].toInt() xor roundKeys[off + i].toInt()).toByte()
    }

    private fun subBytes(s: ByteArray, box: IntArray) {
        for (i in 0 until 16) s[i] = box[s[i].toInt() and 0xFF].toByte()
    }

    // State is column-major: s[c*4 + r]
    private fun shiftRows(s: ByteArray) {
        var t: Byte
        // row 1: shift left by 1
        t = s[1]; s[1] = s[5]; s[5] = s[9]; s[9] = s[13]; s[13] = t
        // row 2: shift left by 2
        t = s[2]; s[2] = s[10]; s[10] = t
        t = s[6]; s[6] = s[14]; s[14] = t
        // row 3: shift left by 3 (= right by 1)
        t = s[15]; s[15] = s[11]; s[11] = s[7]; s[7] = s[3]; s[3] = t
    }

    private fun invShiftRows(s: ByteArray) {
        var t: Byte
        t = s[13]; s[13] = s[9]; s[9] = s[5]; s[5] = s[1]; s[1] = t
        t = s[2]; s[2] = s[10]; s[10] = t
        t = s[6]; s[6] = s[14]; s[14] = t
        t = s[3]; s[3] = s[7]; s[7] = s[11]; s[11] = s[15]; s[15] = t
    }

    private fun mixColumns(s: ByteArray) {
        for (c in 0 until 4) {
            val i = c * 4
            val a0 = s[i].toInt() and 0xFF
            val a1 = s[i + 1].toInt() and 0xFF
            val a2 = s[i + 2].toInt() and 0xFF
            val a3 = s[i + 3].toInt() and 0xFF
            s[i] = (mul2(a0) xor mul3(a1) xor a2 xor a3).toByte()
            s[i + 1] = (a0 xor mul2(a1) xor mul3(a2) xor a3).toByte()
            s[i + 2] = (a0 xor a1 xor mul2(a2) xor mul3(a3)).toByte()
            s[i + 3] = (mul3(a0) xor a1 xor a2 xor mul2(a3)).toByte()
        }
    }

    private fun invMixColumns(s: ByteArray) {
        for (c in 0 until 4) {
            val i = c * 4
            val a0 = s[i].toInt() and 0xFF
            val a1 = s[i + 1].toInt() and 0xFF
            val a2 = s[i + 2].toInt() and 0xFF
            val a3 = s[i + 3].toInt() and 0xFF
            s[i] = (mul(a0, 14) xor mul(a1, 11) xor mul(a2, 13) xor mul(a3, 9)).toByte()
            s[i + 1] = (mul(a0, 9) xor mul(a1, 14) xor mul(a2, 11) xor mul(a3, 13)).toByte()
            s[i + 2] = (mul(a0, 13) xor mul(a1, 9) xor mul(a2, 14) xor mul(a3, 11)).toByte()
            s[i + 3] = (mul(a0, 11) xor mul(a1, 13) xor mul(a2, 9) xor mul(a3, 14)).toByte()
        }
    }

    companion object {
        private fun mul2(a: Int): Int = ((a shl 1) xor (if (a and 0x80 != 0) 0x1B else 0)) and 0xFF
        private fun mul3(a: Int): Int = mul2(a) xor a
        private fun mul(a: Int, b: Int): Int {
            var x = a
            var y = b
            var p = 0
            while (y != 0) {
                if (y and 1 != 0) p = p xor x
                x = mul2(x)
                y = y ushr 1
            }
            return p and 0xFF
        }

        private fun expandKey(key: ByteArray, nk: Int, rounds: Int): ByteArray {
            val total = 4 * (rounds + 1) // words
            val w = IntArray(total)
            for (i in 0 until nk) {
                w[i] = ((key[4 * i].toInt() and 0xFF) shl 24) or ((key[4 * i + 1].toInt() and 0xFF) shl 16) or
                        ((key[4 * i + 2].toInt() and 0xFF) shl 8) or (key[4 * i + 3].toInt() and 0xFF)
            }
            var rcon = 1
            for (i in nk until total) {
                var temp = w[i - 1]
                if (i % nk == 0) {
                    temp = subWord(rotWord(temp)) xor (rcon shl 24)
                    rcon = mul2(rcon)
                } else if (nk > 6 && i % nk == 4) {
                    temp = subWord(temp)
                }
                w[i] = w[i - nk] xor temp
            }
            val out = ByteArray(total * 4)
            for (i in 0 until total) {
                out[4 * i] = (w[i] ushr 24).toByte()
                out[4 * i + 1] = (w[i] ushr 16).toByte()
                out[4 * i + 2] = (w[i] ushr 8).toByte()
                out[4 * i + 3] = w[i].toByte()
            }
            return out
        }

        private fun rotWord(w: Int): Int = (w shl 8) or (w ushr 24)
        private fun subWord(w: Int): Int =
            (SBOX[(w ushr 24) and 0xFF] shl 24) or (SBOX[(w ushr 16) and 0xFF] shl 16) or
                    (SBOX[(w ushr 8) and 0xFF] shl 8) or SBOX[w and 0xFF]

        private val SBOX = intArrayOf(
            0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
            0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
            0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
            0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
            0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
            0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
            0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
            0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
            0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
            0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
            0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
            0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
            0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
            0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
            0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
            0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
        )

        private val INV_SBOX = IntArray(256).also { inv -> for (i in 0 until 256) inv[SBOX[i]] = i }
    }
}
