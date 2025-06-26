package io.cryptocalc.crypto.engines

import ai.cortex.core.IsoUtil
import io.cryptocalc.crypto.engines.encryption.TdesCalculatorEngine
import org.junit.Test
import kotlin.test.assertEquals

class TdesCalculatorEngineTest {

    @Test
    fun testTdesEncryption() {
        // Example test case for TDES encryption
        val key = "0123456789ABCDEF"
        val data = "12345678"
        val iv = "12345678"

        // Convert hex strings to byte arrays
        val keyBytes = IsoUtil.hexStringToBytes(key)
        val dataBytes = data.toByteArray()
        val ivBytes = iv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        // Call the TDES encryption method (to be implemented)
         val encryptedData = TdesCalculatorEngine.encryptECB(dataBytes, keyBytes)

        assertEquals("BD0B1A49070AC376", IsoUtil.bytesToHexString(encryptedData))
    }

}