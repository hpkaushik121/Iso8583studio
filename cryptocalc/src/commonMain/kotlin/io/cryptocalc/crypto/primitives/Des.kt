package io.cryptocalc.crypto.primitives

/**
 * Single DES (FIPS 46-3) block cipher. Pure Kotlin so the same code runs on the JVM and in
 * Kotlin/Wasm. The structure (key schedule and the combined SP boxes) follows the public
 * domain reference used by Bouncy Castle's DESEngine.
 *
 * DES is legacy cryptography kept here because the payment industry still runs on it
 * (DUKPT, PIN blocks, MACs, key check values). Do not use it for anything new.
 */
class Des(key: ByteArray) : BlockCipher {
    override val blockSize: Int get() = 8

    private val workingKey: IntArray

    init {
        require(key.size == 8) { "DES key must be 8 bytes, got ${key.size}" }
        workingKey = generateWorkingKey(key)
    }

    override fun encryptBlock(src: ByteArray, srcOff: Int, dst: ByteArray, dstOff: Int) =
        desFunc(workingKey, false, src, srcOff, dst, dstOff)

    override fun decryptBlock(src: ByteArray, srcOff: Int, dst: ByteArray, dstOff: Int) =
        desFunc(workingKey, true, src, srcOff, dst, dstOff)

    companion object {
        private val BYTEBIT = shortArrayOf(0x80, 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01)

        private val BIGBYTE = intArrayOf(
            0x800000, 0x400000, 0x200000, 0x100000, 0x080000, 0x040000, 0x020000, 0x010000,
            0x008000, 0x004000, 0x002000, 0x001000, 0x000800, 0x000400, 0x000200, 0x000100,
            0x000080, 0x000040, 0x000020, 0x000010, 0x000008, 0x000004, 0x000002, 0x000001
        )

        private val PC1 = byteArrayOf(
            56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17,
            9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35,
            62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21,
            13, 5, 60, 52, 44, 36, 28, 20, 12, 4, 27, 19, 11, 3
        )

        private val TOTROT = byteArrayOf(1, 2, 4, 6, 8, 10, 12, 14, 15, 17, 19, 21, 23, 25, 27, 28)

        private val PC2 = byteArrayOf(
            13, 16, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9,
            22, 18, 11, 3, 25, 7, 15, 6, 26, 19, 12, 1,
            40, 51, 30, 36, 46, 54, 29, 39, 50, 44, 32, 47,
            43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31
        )

        private val SP1 = intArrayOf(
            0x01010400, 0x00000000, 0x00010000, 0x01010404, 0x01010004, 0x00010404, 0x00000004, 0x00010000,
            0x00000400, 0x01010400, 0x01010404, 0x00000400, 0x01000404, 0x01010004, 0x01000000, 0x00000004,
            0x00000404, 0x01000400, 0x01000400, 0x00010400, 0x00010400, 0x01010000, 0x01010000, 0x01000404,
            0x00010004, 0x01000004, 0x01000004, 0x00010004, 0x00000000, 0x00000404, 0x00010404, 0x01000000,
            0x00010000, 0x01010404, 0x00000004, 0x01010000, 0x01010400, 0x01000000, 0x01000000, 0x00000400,
            0x01010004, 0x00010000, 0x00010400, 0x01000004, 0x00000400, 0x00000004, 0x01000404, 0x00010404,
            0x01010404, 0x00010004, 0x01010000, 0x01000404, 0x01000004, 0x00000404, 0x00010404, 0x01010400,
            0x00000404, 0x01000400, 0x01000400, 0x00000000, 0x00010004, 0x00010400, 0x00000000, 0x01010004
        )

        private val SP2 = intArrayOf(
            0x80108020.toInt(), 0x80008000.toInt(), 0x00008000, 0x00108020, 0x00100000, 0x00000020, 0x80100020.toInt(), 0x80008020.toInt(),
            0x80000020.toInt(), 0x80108020.toInt(), 0x80108000.toInt(), 0x80000000.toInt(), 0x80008000.toInt(), 0x00100000, 0x00000020, 0x80100020.toInt(),
            0x00108000, 0x00100020, 0x80008020.toInt(), 0x00000000, 0x80000000.toInt(), 0x00008000, 0x00108020, 0x80100000.toInt(),
            0x00100020, 0x80000020.toInt(), 0x00000000, 0x00108000, 0x00008020, 0x80108000.toInt(), 0x80100000.toInt(), 0x00008020,
            0x00000000, 0x00108020, 0x80100020.toInt(), 0x00100000, 0x80008020.toInt(), 0x80100000.toInt(), 0x80108000.toInt(), 0x00008000,
            0x80100000.toInt(), 0x80008000.toInt(), 0x00000020, 0x80108020.toInt(), 0x00108020, 0x00000020, 0x00008000, 0x80000000.toInt(),
            0x00008020, 0x80108000.toInt(), 0x00100000, 0x80000020.toInt(), 0x00100020, 0x80008020.toInt(), 0x80000020.toInt(), 0x00100020,
            0x00108000, 0x00000000, 0x80008000.toInt(), 0x00008020, 0x80000000.toInt(), 0x80100020.toInt(), 0x80108020.toInt(), 0x00108000
        )

        private val SP3 = intArrayOf(
            0x00000208, 0x08020200, 0x00000000, 0x08020008, 0x08000200, 0x00000000, 0x00020208, 0x08000200,
            0x00020008, 0x08000008, 0x08000008, 0x00020000, 0x08020208, 0x00020008, 0x08020000, 0x00000208,
            0x08000000, 0x00000008, 0x08020200, 0x00000200, 0x00020200, 0x08020000, 0x08020008, 0x00020208,
            0x08000208, 0x00020200, 0x00020000, 0x08000208, 0x00000008, 0x08020208, 0x00000200, 0x08000000,
            0x08020200, 0x08000000, 0x00020008, 0x00000208, 0x00020000, 0x08020200, 0x08000200, 0x00000000,
            0x00000200, 0x00020008, 0x08020208, 0x08000200, 0x08000008, 0x00000200, 0x00000000, 0x08020008,
            0x08000208, 0x00020000, 0x08000000, 0x08020208, 0x00000008, 0x00020208, 0x00020200, 0x08000008,
            0x08020000, 0x08000208, 0x00000208, 0x08020000, 0x00020208, 0x00000008, 0x08020008, 0x00020200
        )

        private val SP4 = intArrayOf(
            0x00802001, 0x00002081, 0x00002081, 0x00000080, 0x00802080, 0x00800081, 0x00800001, 0x00002001,
            0x00000000, 0x00802000, 0x00802000, 0x00802081, 0x00000081, 0x00000000, 0x00800080, 0x00800001,
            0x00000001, 0x00002000, 0x00800000, 0x00802001, 0x00000080, 0x00800000, 0x00002001, 0x00002080,
            0x00800081, 0x00000001, 0x00002080, 0x00800080, 0x00002000, 0x00802080, 0x00802081, 0x00000081,
            0x00800080, 0x00800001, 0x00802000, 0x00802081, 0x00000081, 0x00000000, 0x00000000, 0x00802000,
            0x00002080, 0x00800080, 0x00800081, 0x00000001, 0x00802001, 0x00002081, 0x00002081, 0x00000080,
            0x00802081, 0x00000081, 0x00000001, 0x00002000, 0x00800001, 0x00002001, 0x00802080, 0x00800081,
            0x00002001, 0x00002080, 0x00800000, 0x00802001, 0x00000080, 0x00800000, 0x00002000, 0x00802080
        )

        private val SP5 = intArrayOf(
            0x00000100, 0x02080100, 0x02080000, 0x42000100, 0x00080000, 0x00000100, 0x40000000, 0x02080000,
            0x40080100, 0x00080000, 0x02000100, 0x40080100, 0x42000100, 0x42080000, 0x00080100, 0x40000000,
            0x02000000, 0x40080000, 0x40080000, 0x00000000, 0x40000100, 0x42080100, 0x42080100, 0x02000100,
            0x42080000, 0x40000100, 0x00000000, 0x42000000, 0x02080100, 0x02000000, 0x42000000, 0x00080100,
            0x00080000, 0x42000100, 0x00000100, 0x02000000, 0x40000000, 0x02080000, 0x42000100, 0x40080100,
            0x02000100, 0x40000000, 0x42080000, 0x02080100, 0x40080100, 0x00000100, 0x02000000, 0x42080000,
            0x42080100, 0x00080100, 0x42000000, 0x42080100, 0x02080000, 0x00000000, 0x40080000, 0x42000000,
            0x00080100, 0x02000100, 0x40000100, 0x00080000, 0x00000000, 0x40080000, 0x02080100, 0x40000100
        )

        private val SP6 = intArrayOf(
            0x20000010, 0x20400000, 0x00004000, 0x20404010, 0x20400000, 0x00000010, 0x20404010, 0x00400000,
            0x20004000, 0x00404010, 0x00400000, 0x20000010, 0x00400010, 0x20004000, 0x20000000, 0x00004010,
            0x00000000, 0x00400010, 0x20004010, 0x00004000, 0x00404000, 0x20004010, 0x00000010, 0x20400010,
            0x20400010, 0x00000000, 0x00404010, 0x20404000, 0x00004010, 0x00404000, 0x20404000, 0x20000000,
            0x20004000, 0x00000010, 0x20400010, 0x00404000, 0x20404010, 0x00400000, 0x00004010, 0x20000010,
            0x00400000, 0x20004000, 0x20000000, 0x00004010, 0x20000010, 0x20404010, 0x00404000, 0x20400000,
            0x00404010, 0x20404000, 0x00000000, 0x20400010, 0x00000010, 0x00004000, 0x20400000, 0x00404010,
            0x00004000, 0x00400010, 0x20004010, 0x00000000, 0x20404000, 0x20000000, 0x00400010, 0x20004010
        )

        private val SP7 = intArrayOf(
            0x00200000, 0x04200002, 0x04000802, 0x00000000, 0x00000800, 0x04000802, 0x00200802, 0x04200800,
            0x04200802, 0x00200000, 0x00000000, 0x04000002, 0x00000002, 0x04000000, 0x04200002, 0x00000802,
            0x04000800, 0x00200802, 0x00200002, 0x04000800, 0x04000002, 0x04200000, 0x04200800, 0x00200002,
            0x04200000, 0x00000800, 0x00000802, 0x04200802, 0x00200800, 0x00000002, 0x04000000, 0x00200800,
            0x04000000, 0x00200800, 0x00200000, 0x04000802, 0x04000802, 0x04200002, 0x04200002, 0x00000002,
            0x00200002, 0x04000000, 0x04000800, 0x00200000, 0x04200800, 0x00000802, 0x00200802, 0x04200800,
            0x00000802, 0x04000002, 0x04200802, 0x04200000, 0x00200800, 0x00000000, 0x00000002, 0x04200802,
            0x00000000, 0x00200802, 0x04200000, 0x00000800, 0x04000002, 0x04000800, 0x00000800, 0x00200002
        )

        private val SP8 = intArrayOf(
            0x10001040, 0x00001000, 0x00040000, 0x10041040, 0x10000000, 0x10001040, 0x00000040, 0x10000000,
            0x00040040, 0x10040000, 0x10041040, 0x00041000, 0x10041000, 0x00041040, 0x00001000, 0x00000040,
            0x10040000, 0x10000040, 0x10001000, 0x00001040, 0x00041000, 0x00040040, 0x10040040, 0x10041000,
            0x00001040, 0x00000000, 0x00000000, 0x10040040, 0x10000040, 0x10001000, 0x00041040, 0x00040000,
            0x00041040, 0x00040000, 0x10041000, 0x00001000, 0x00000040, 0x10040040, 0x00001000, 0x00041040,
            0x10001000, 0x00000040, 0x10000040, 0x10040000, 0x10040040, 0x10000000, 0x00040000, 0x10001040,
            0x00000000, 0x10041040, 0x00040040, 0x10000040, 0x10040000, 0x10001000, 0x10001040, 0x00000000,
            0x10041040, 0x00041000, 0x00041000, 0x00001040, 0x00001040, 0x00040040, 0x10000000, 0x10041000
        )

        private fun generateWorkingKey(key: ByteArray): IntArray {
            val newKey = IntArray(32)
            val pc1m = BooleanArray(56)
            val pcr = BooleanArray(56)

            for (j in 0 until 56) {
                val l = PC1[j].toInt()
                pc1m[j] = (key[l ushr 3].toInt() and BYTEBIT[l and 0x07].toInt()) != 0
            }

            for (i in 0 until 16) {
                val m = i shl 1
                val n = m + 1
                newKey[m] = 0
                newKey[n] = 0

                for (j in 0 until 28) {
                    val l = j + TOTROT[i]
                    pcr[j] = if (l < 28) pc1m[l] else pc1m[l - 28]
                }
                for (j in 28 until 56) {
                    val l = j + TOTROT[i]
                    pcr[j] = if (l < 56) pc1m[l] else pc1m[l - 28]
                }

                for (j in 0 until 24) {
                    if (pcr[PC2[j].toInt()]) newKey[m] = newKey[m] or BIGBYTE[j]
                    if (pcr[PC2[j + 24].toInt()]) newKey[n] = newKey[n] or BIGBYTE[j]
                }
            }

            // Store the raw key for the 32 integer values of the key schedule
            for (i in 0 until 32 step 2) {
                val i1 = newKey[i]
                val i2 = newKey[i + 1]
                newKey[i] = ((i1 and 0x00fc0000) shl 6) or ((i1 and 0x00000fc0) shl 10) or
                        ((i2 and 0x00fc0000) ushr 10) or ((i2 and 0x00000fc0) ushr 6)
                newKey[i + 1] = ((i1 and 0x0003f000) shl 12) or ((i1 and 0x0000003f) shl 16) or
                        ((i2 and 0x0003f000) ushr 4) or (i2 and 0x0000003f)
            }
            return newKey
        }

        private fun desFunc(wKey: IntArray, decrypt: Boolean, input: ByteArray, inOff: Int, out: ByteArray, outOff: Int) {
            var left = bigEndianToInt(input, inOff)
            var right = bigEndianToInt(input, inOff + 4)
            var work: Int

            work = ((left ushr 4) xor right) and 0x0f0f0f0f
            right = right xor work
            left = left xor (work shl 4)
            work = ((left ushr 16) xor right) and 0x0000ffff
            right = right xor work
            left = left xor (work shl 16)
            work = ((right ushr 2) xor left) and 0x33333333
            left = left xor work
            right = right xor (work shl 2)
            work = ((right ushr 8) xor left) and 0x00ff00ff
            left = left xor work
            right = right xor (work shl 8)
            right = (right shl 1) or (right ushr 31)
            work = (left xor right) and 0xaaaaaaaa.toInt()
            left = left xor work
            right = right xor work
            left = (left shl 1) or (left ushr 31)

            for (round in 0 until 8) {
                // Encryption walks the schedule forwards in pairs of words; decryption walks it
                // backwards, pair by pair, keeping the word order inside each pair.
                val kA = if (decrypt) 30 - (round shl 2) else round shl 2
                val kB = if (decrypt) kA - 2 else kA + 2
                work = (right shl 28) or (right ushr 4)
                work = work xor wKey[kA]
                var fval = SP7[work and 0x3f]
                fval = fval or SP5[(work ushr 8) and 0x3f]
                fval = fval or SP3[(work ushr 16) and 0x3f]
                fval = fval or SP1[(work ushr 24) and 0x3f]
                work = right xor wKey[kA + 1]
                fval = fval or SP8[work and 0x3f]
                fval = fval or SP6[(work ushr 8) and 0x3f]
                fval = fval or SP4[(work ushr 16) and 0x3f]
                fval = fval or SP2[(work ushr 24) and 0x3f]
                left = left xor fval
                work = (left shl 28) or (left ushr 4)
                work = work xor wKey[kB]
                fval = SP7[work and 0x3f]
                fval = fval or SP5[(work ushr 8) and 0x3f]
                fval = fval or SP3[(work ushr 16) and 0x3f]
                fval = fval or SP1[(work ushr 24) and 0x3f]
                work = left xor wKey[kB + 1]
                fval = fval or SP8[work and 0x3f]
                fval = fval or SP6[(work ushr 8) and 0x3f]
                fval = fval or SP4[(work ushr 16) and 0x3f]
                fval = fval or SP2[(work ushr 24) and 0x3f]
                right = right xor fval
            }

            right = (right shl 31) or (right ushr 1)
            work = (left xor right) and 0xaaaaaaaa.toInt()
            left = left xor work
            right = right xor work
            left = (left shl 31) or (left ushr 1)
            work = ((left ushr 8) xor right) and 0x00ff00ff
            right = right xor work
            left = left xor (work shl 8)
            work = ((left ushr 2) xor right) and 0x33333333
            right = right xor work
            left = left xor (work shl 2)
            work = ((right ushr 16) xor left) and 0x0000ffff
            left = left xor work
            right = right xor (work shl 16)
            work = ((right ushr 4) xor left) and 0x0f0f0f0f
            left = left xor work
            right = right xor (work shl 4)

            intToBigEndian(right, out, outOff)
            intToBigEndian(left, out, outOff + 4)
        }

        private fun bigEndianToInt(bs: ByteArray, off: Int): Int =
            (bs[off].toInt() shl 24) or ((bs[off + 1].toInt() and 0xff) shl 16) or
                    ((bs[off + 2].toInt() and 0xff) shl 8) or (bs[off + 3].toInt() and 0xff)

        private fun intToBigEndian(n: Int, bs: ByteArray, off: Int) {
            bs[off] = (n ushr 24).toByte()
            bs[off + 1] = (n ushr 16).toByte()
            bs[off + 2] = (n ushr 8).toByte()
            bs[off + 3] = n.toByte()
        }
    }
}

/**
 * Triple DES (EDE) with 8, 16 or 24-byte keys. 8 bytes degrades to single DES, 16 bytes is
 * two-key TDES (K1 K2 K1), 24 bytes is three-key TDES. Callers no longer need to expand
 * double-length keys to 24 bytes themselves.
 */
class Tdes(key: ByteArray) : BlockCipher {
    override val blockSize: Int get() = 8

    private val k1: Des
    private val k2: Des
    private val k3: Des

    init {
        require(key.size == 8 || key.size == 16 || key.size == 24) {
            "TDES key must be 8, 16 or 24 bytes, got ${key.size}"
        }
        k1 = Des(key.copyOfRange(0, 8))
        k2 = if (key.size >= 16) Des(key.copyOfRange(8, 16)) else k1
        k3 = if (key.size == 24) Des(key.copyOfRange(16, 24)) else k1
    }

    override fun encryptBlock(src: ByteArray, srcOff: Int, dst: ByteArray, dstOff: Int) {
        val tmp = ByteArray(8)
        k1.encryptBlock(src, srcOff, tmp, 0)
        k2.decryptBlock(tmp, 0, tmp, 0)
        k3.encryptBlock(tmp, 0, dst, dstOff)
    }

    override fun decryptBlock(src: ByteArray, srcOff: Int, dst: ByteArray, dstOff: Int) {
        val tmp = ByteArray(8)
        k3.decryptBlock(src, srcOff, tmp, 0)
        k2.encryptBlock(tmp, 0, tmp, 0)
        k1.decryptBlock(tmp, 0, dst, dstOff)
    }

    companion object {
        /** Known weak and semi-weak single-DES keys (parity-adjusted form). */
        val WEAK_KEYS: Set<String> = setOf(
            "0101010101010101", "FEFEFEFEFEFEFEFE", "E0E0E0E0F1F1F1F1", "1F1F1F1F0E0E0E0E",
            "011F011F010E010E", "1F011F010E010E01", "01E001E001F101F1", "E001E001F101F101",
            "01FE01FE01FE01FE", "FE01FE01FE01FE01", "1FE01FE00EF10EF1", "E01FE01FF10EF10E",
            "1FFE1FFE0EFE0EFE", "FE1FFE1FFE0EFE0E", "E0FEE0FEF1FEF1FE", "FEE0FEE0FEF1FEF1"
        )
    }
}
