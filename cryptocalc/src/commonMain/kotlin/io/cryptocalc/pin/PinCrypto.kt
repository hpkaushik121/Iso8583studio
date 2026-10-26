package io.cryptocalc.pin

import ai.cortex.core.hexToBytes
import ai.cortex.core.toHexUpper
import io.cryptocalc.crypto.primitives.Aes
import io.cryptocalc.crypto.primitives.Tdes
import io.cryptocalc.crypto.primitives.secureRandomBytes

/**
 * PIN verification and PIN block cryptography: Visa PVV, IBM 3624 offsets and the ISO 9564-1
 * format 4 (AES) PIN block. All inputs are decimal/hex strings as typed into the calculators.
 */
object PinCrypto {

    // ------------------------------------------------------------------ decimalisation

    /**
     * Two-pass decimalisation used by Visa PVV and CVV: first every decimal digit of [hex] in
     * order, then, if still short of [count], hex digits A–F mapped to 0–5.
     */
    fun visaDecimalize(hex: String, count: Int): String {
        val out = StringBuilder()
        for (c in hex.uppercase()) if (c.isDigit() && out.length < count) out.append(c)
        if (out.length < count) {
            for (c in hex.uppercase()) {
                if (out.length >= count) break
                if (c in 'A'..'F') out.append((c - 'A'))
            }
        }
        return out.toString()
    }

    /** IBM 3624 decimalisation: each hex digit of [hex] is replaced through a 16-entry [table] (hex position -> digit). */
    fun ibmDecimalize(hex: String, table: String): String {
        require(table.length == 16 && table.all { it.isDigit() }) { "Decimalisation table must be 16 digits" }
        return hex.uppercase().map { table[it.digitToInt(16)] }.joinToString("")
    }

    // ------------------------------------------------------------------ Visa PVV

    /**
     * Visa PIN Verification Value. TSP = 11 rightmost PAN digits excluding the check digit ‖ PVKI ‖
     * leftmost 4 PIN digits; 3DES-ECB under the PVK (16 bytes); 4 digits by two-pass decimalisation.
     */
    fun visaPvv(pvk: ByteArray, pan: String, pin: String, pvki: Int): String {
        require(pvk.size == 16) { "PVK must be 16 bytes (32 hex)" }
        require(pvki in 0..9) { "PVKI must be 0..9" }
        val digits = pan.filter { it.isDigit() }
        require(digits.length >= 12) { "PAN must have at least 12 digits" }
        val pinDigits = pin.filter { it.isDigit() }
        require(pinDigits.length >= 4) { "PIN must have at least 4 digits" }
        val tsp = digits.dropLast(1).takeLast(11) + pvki + pinDigits.take(4)
        val enc = Tdes(pvk).encryptBlock(tsp.hexToBytes())
        return visaDecimalize(enc.toHexUpper(), 4)
    }

    // ------------------------------------------------------------------ IBM 3624

    /**
     * IBM 3624 natural PIN: the validation data (digits of [pan] from [start] (1-based) for
     * [length], padded with [pad] to 16 nibbles, optionally masked with [mask]) is 3DES-ECB
     * encrypted under [pdk] and decimalised with [decTable]; the first [pinLength] digits result.
     */
    fun ibmNaturalPin(
        pdk: ByteArray, pan: String, decTable: String, pinLength: Int,
        start: Int = 1, length: Int = 11, pad: Char = '0', mask: String? = null,
    ): String {
        require(pdk.size == 8 || pdk.size == 16 || pdk.size == 24) { "PDK must be 8, 16 or 24 bytes" }
        require(pinLength in 4..12) { "PIN length must be 4..12" }
        val digits = pan.filter { it.isDigit() }
        require(start >= 1 && start + length - 1 <= digits.length) { "Validation data window is outside the PAN" }
        var validation = digits.substring(start - 1, start - 1 + length).padEnd(16, pad).take(16)
        if (!mask.isNullOrBlank()) {
            val m = mask.uppercase().padEnd(16, '0').take(16)
            // 'F' in the mask keeps the digit, anything else replaces it with the pad character
            validation = validation.mapIndexed { i, c -> if (m[i] == 'F') c else pad }.joinToString("")
        }
        val enc = Tdes(pdk).encryptBlock(validation.hexToBytes())
        return ibmDecimalize(enc.toHexUpper(), decTable).take(pinLength)
    }

    /** Offset = (customer PIN − natural PIN) mod 10, digit by digit. */
    fun ibmOffset(naturalPin: String, customerPin: String): String {
        require(customerPin.length >= naturalPin.length) { "PIN shorter than the natural PIN" }
        return naturalPin.indices.joinToString("") { i ->
            ((customerPin[i].digitToInt() - naturalPin[i].digitToInt() + 10) % 10).toString()
        }
    }

    /** Customer PIN = (natural PIN + offset) mod 10, digit by digit. */
    fun ibmPinFromOffset(naturalPin: String, offset: String): String {
        require(offset.length >= naturalPin.length) { "Offset shorter than the natural PIN" }
        return naturalPin.indices.joinToString("") { i ->
            ((naturalPin[i].digitToInt() + offset[i].digitToInt()) % 10).toString()
        }
    }

    // ------------------------------------------------------------------ ISO 9564-1 format 4 (AES)

    /** Plain 16-byte PIN field: 4 ‖ L ‖ PIN ‖ 'A' fill to 16 nibbles ‖ 8 random bytes. */
    fun iso4PinField(pin: String, random: ByteArray = secureRandomBytes(8)): ByteArray {
        val digits = pin.filter { it.isDigit() }
        require(digits.length in 4..12) { "PIN must be 4..12 digits" }
        require(random.size == 8) { "Random fill must be 8 bytes" }
        val hex = ("4" + digits.length.toString(16).uppercase() + digits).padEnd(16, 'A')
        return hex.hexToBytes() + random
    }

    /** 16-byte PAN field: M (= PAN length − 12) ‖ PAN ‖ zero fill. */
    fun iso4PanField(pan: String): ByteArray {
        val digits = pan.filter { it.isDigit() }
        require(digits.length in 12..19) { "PAN must be 12..19 digits" }
        val hex = ((digits.length - 12).toString(16).uppercase() + digits).padEnd(32, '0')
        return hex.hexToBytes()
    }

    /** Encrypted ISO-4 block: AES_K( AES_K(PIN field) XOR PAN field ). */
    fun iso4Encrypt(key: ByteArray, pin: String, pan: String, random: ByteArray = secureRandomBytes(8)): ByteArray {
        val aes = Aes(key)
        val a = aes.encryptBlock(iso4PinField(pin, random))
        val panField = iso4PanField(pan)
        val x = ByteArray(16) { (a[it].toInt() xor panField[it].toInt()).toByte() }
        return aes.encryptBlock(x)
    }

    /** Recovers the PIN from an ISO-4 block: decrypt, XOR with the PAN field, decrypt again, parse. */
    fun iso4Decrypt(key: ByteArray, block: ByteArray, pan: String): String {
        require(block.size == 16) { "PIN block must be 16 bytes" }
        val aes = Aes(key)
        val panField = iso4PanField(pan)
        val a = aes.decryptBlock(block)
        val x = ByteArray(16) { (a[it].toInt() xor panField[it].toInt()).toByte() }
        val pinField = aes.decryptBlock(x).toHexUpper()
        require(pinField[0] == '4') { "Not an ISO format 4 PIN block (control nibble ${pinField[0]})" }
        val len = pinField[1].digitToInt(16)
        require(len in 4..12) { "Invalid PIN length $len in block" }
        val pin = pinField.substring(2, 2 + len)
        require(pin.all { it.isDigit() }) { "Decrypted PIN field is not numeric (wrong key or PAN?)" }
        return pin
    }
}
