package ai.cortex.core.crypto

/**
 * MAC calculation utilities
 */
object MacCalculator {
    fun calculateMac(data: ByteArray, key: ByteArray, cipher: SymmetricCipher): ByteArray {
        val paddedData = padData(data)
        var mac = ByteArray(8)

        for (i in paddedData.indices step 8) {
            val block = paddedData.copyOfRange(i, i + 8)
            val xorResult = block.zip(mac) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
            mac = cipher.encryptEcb(xorResult, key)
        }
        return mac
    }

    fun calculate3DesMac(data: ByteArray, key: ByteArray, cipher: SymmetricCipher): ByteArray {
        require(key.size == 16 || key.size == 24) { "Key must be 16 or 24 bytes for 3DES" }

        val paddedData = padData(data)
        val keyA = key.copyOfRange(0, 8)
        val keyB = if (key.size == 16) key.copyOfRange(8, 16) else key.copyOfRange(16, 24)

        var mac = ByteArray(8)

        // DES encrypt with keyA for all blocks except last
        for (i in 0 until paddedData.size - 8 step 8) {
            val block = paddedData.copyOfRange(i, i + 8)
            val xorResult = block.zip(mac) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
            mac = cipher.encryptEcb(xorResult, keyA)
        }

        // 3DES for last block: DES decrypt with keyB, then DES encrypt with keyA
        val lastBlock = paddedData.copyOfRange(paddedData.size - 8, paddedData.size)
        val xorResult = lastBlock.zip(mac) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
        val decrypted = cipher.decryptEcb(xorResult, keyB)
        return cipher.encryptEcb(decrypted, keyA)
    }

    private fun padData(data: ByteArray): ByteArray {
        val paddedList = data.toMutableList()
        paddedList.add(0x80.toByte()) // ISO 9797-1 Method 2 padding
        while (paddedList.size % 8 != 0) {
            paddedList.add(0x00.toByte())
        }
        return paddedList.toByteArray()
    }
}