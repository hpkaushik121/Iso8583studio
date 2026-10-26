package io.cryptocalc.emv

import ai.cortex.core.hexToBytes
import ai.cortex.core.types.CipherMode
import ai.cortex.core.types.PaddingMethods
import ai.cortex.core.xorWith
import io.cryptocalc.crypto.primitives.Mac
import io.cryptocalc.crypto.primitives.Modes
import io.cryptocalc.crypto.primitives.Tdes

/**
 * Issuer script secure messaging (EMV 4.3 Book 2 §9, Annex A1.1/A1.2): command MACs, command
 * data encipherment and the PIN block formats used by PIN change scripts.
 */
object EmvSecureMessaging {

    enum class EncryptionType { VISA, MASTERCARD, EMV }

    /** ISO 9797-1 algorithm 3 MAC with padding method 2 over [command] (header ‖ optional ATC ‖ ARQC ‖ data). */
    fun commandMac(skSmi: ByteArray, command: ByteArray, length: Int = 8): ByteArray {
        require(skSmi.size == 16) { "Session key must be 16 bytes" }
        return Mac.emvMac(skSmi, command, Mac.Padding.METHOD_2, length)
    }

    /**
     * Enciphers command data. VISA: length byte prepended, padding method 2, TDES-ECB.
     * MASTERCARD: padding method 2 only if needed, TDES-CBC zero IV. EMV: padding method 2, TDES-CBC.
     */
    fun encryptCommandData(skSmc: ByteArray, data: ByteArray, type: EncryptionType): ByteArray {
        require(skSmc.size == 16) { "Session key must be 16 bytes" }
        val cipher = Tdes(skSmc)
        return when (type) {
            EncryptionType.VISA -> Modes.encrypt(cipher, Modes.pad(byteArrayOf(data.size.toByte()) + data, 8, PaddingMethods.METHOD_2_ISO_9797), CipherMode.ECB)
            EncryptionType.MASTERCARD -> Modes.encrypt(cipher, if (data.size % 8 == 0) data else Modes.pad(data, 8, PaddingMethods.METHOD_2_ISO_9797), CipherMode.CBC, ByteArray(8))
            EncryptionType.EMV -> Modes.encrypt(cipher, Modes.pad(data, 8, PaddingMethods.METHOD_2_ISO_9797), CipherMode.CBC, ByteArray(8))
        }
    }

    fun decryptCommandData(skSmc: ByteArray, data: ByteArray, type: EncryptionType): ByteArray {
        require(skSmc.size == 16) { "Session key must be 16 bytes" }
        val cipher = Tdes(skSmc)
        return when (type) {
            EncryptionType.VISA -> {
                val clear = Modes.decrypt(cipher, data, CipherMode.ECB)
                val len = clear[0].toInt() and 0xFF
                clear.copyOfRange(1, 1 + len)
            }
            else -> Modes.decrypt(cipher, data, CipherMode.CBC, ByteArray(8))
        }
    }

    /** ISO 9564-1 format 2 PIN block: 2N ‖ PIN ‖ F padding. */
    fun iso2PinBlock(pin: String): ByteArray {
        require(pin.length in 4..12 && pin.all { it.isDigit() }) { "PIN must be 4-12 digits" }
        return ((0x20 + pin.length).toString(16) + pin + "F".repeat(14 - pin.length)).hexToBytes()
    }

    /**
     * Visa (VIS) PIN block: (00×4 ‖ 4 rightmost bytes of key A of the ICC MK-AC) XOR
     * (length ‖ PIN ‖ F padding), optionally XORed with the current PIN left-justified, 0 padded.
     */
    fun visPinBlock(iccMkAc: ByteArray, pin: String, currentPin: String? = null): ByteArray {
        require(iccMkAc.size == 16) { "ICC MK-AC must be 16 bytes" }
        require(pin.length in 4..12 && pin.all { it.isDigit() }) { "PIN must be 4-12 digits" }
        val blockA = ByteArray(4) + iccMkAc.copyOfRange(4, 8)
        val blockB = (pin.length.toString(16).padStart(2, '0') + pin + "F".repeat(14 - pin.length)).hexToBytes()
        var block = blockA.xorWith(blockB)
        if (!currentPin.isNullOrBlank()) {
            require(currentPin.length in 4..12 && currentPin.all { it.isDigit() }) { "Current PIN must be 4-12 digits" }
            block = block.xorWith((currentPin + "0".repeat(16 - currentPin.length)).hexToBytes())
        }
        return block
    }

    /**
     * Visa-style PIN change block under UDK-ENC (payShield "PIN block format 41/42" family):
     * ISO format 2 block XORed with the rightmost 8 bytes of the UDK-ENC before encipherment.
     * Reference implementation: verify against the Visa specification.
     */
    fun visaPinChangeBlock(udkEnc: ByteArray, pin: String): ByteArray {
        require(udkEnc.size == 16) { "UDK-ENC must be 16 bytes" }
        return iso2PinBlock(pin).xorWith(udkEnc.copyOfRange(8, 16))
    }
}
