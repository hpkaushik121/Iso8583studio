package `in`.aicortex.iso8583studio.domain.utils

import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.ApduCommand
import java.io.ByteArrayOutputStream
import java.security.SecureRandom

/**
 * Professional APDU utility class for EMV card processing
 * Provides comprehensive APDU manipulation, parsing, and formatting capabilities
 */
object ApduUtil {

    // Standard EMV/ISO7816 Status Words
    object StatusWords {
        const val SW_SUCCESS = "9000"
        const val SW_BYTES_REMAINING_00 = "6100"
        const val SW_WARNING_STATE_UNCHANGED = "6200"
        const val SW_WARNING_CORRUPTED = "6281"
        const val SW_WARNING_EOF = "6282"
        const val SW_WARNING_FILE_DEACTIVATED = "6283"
        const val SW_WARNING_WRONG_FCI = "6284"
        const val SW_WARNING_TERMINATED = "6285"
        const val SW_EXECUTION_ERROR = "6400"
        const val SW_EXECUTION_ERROR_MEMORY = "6581"
        const val SW_WRONG_LENGTH = "6700"
        const val SW_LOGICAL_CHANNEL_NOT_SUPPORTED = "6881"
        const val SW_SECURE_MESSAGING_NOT_SUPPORTED = "6882"
        const val SW_COMMAND_NOT_ALLOWED = "6900"
        const val SW_COMMAND_NOT_ALLOWED_INCOMPATIBLE = "6981"
        const val SW_SECURITY_STATUS_NOT_SATISFIED = "6982"
        const val SW_AUTHENTICATION_METHOD_BLOCKED = "6983"
        const val SW_REFERENCED_DATA_INVALIDATED = "6984"
        const val SW_CONDITIONS_NOT_SATISFIED = "6985"
        const val SW_COMMAND_NOT_ALLOWED_NO_EF = "6986"
        const val SW_EXPECTED_SM_DATA_MISSING = "6987"
        const val SW_SM_DATA_INCORRECT = "6988"
        const val SW_WRONG_PARAMETERS = "6A00"
        const val SW_WRONG_PARAMETERS_FUNCTION_NOT_SUPPORTED = "6A81"
        const val SW_FILE_NOT_FOUND = "6A82"
        const val SW_RECORD_NOT_FOUND = "6A83"
        const val SW_NOT_ENOUGH_MEMORY_SPACE = "6A84"
        const val SW_NC_INCONSISTENT_TLV = "6A85"
        const val SW_INCORRECT_P1P2 = "6A86"
        const val SW_NC_INCONSISTENT_P1P2 = "6A87"
        const val SW_REFERENCED_DATA_NOT_FOUND = "6A88"
        const val SW_FILE_ALREADY_EXISTS = "6A89"
        const val SW_DF_NAME_ALREADY_EXISTS = "6A8A"
        const val SW_WRONG_P1P2 = "6B00"
        const val SW_CORRECT_LENGTH_00 = "6C00"
        const val SW_INS_NOT_SUPPORTED = "6D00"
        const val SW_CLA_NOT_SUPPORTED = "6E00"
        const val SW_UNKNOWN = "6F00"
    }

    // EMV Command Classes
    object CommandClass {
        const val CLA_ISO = 0x00.toByte()
        const val CLA_PROPRIETARY = 0x80.toByte()
        const val CLA_SECURE_MESSAGING = 0x0C.toByte()
    }

    // EMV Instructions
    object Instructions {
        const val INS_SELECT = 0xA4.toByte()
        const val INS_READ_BINARY = 0xB0.toByte()
        const val INS_UPDATE_BINARY = 0xD6.toByte()
        const val INS_READ_RECORD = 0xB2.toByte()
        const val INS_UPDATE_RECORD = 0xDC.toByte()
        const val INS_APPEND_RECORD = 0xE2.toByte()
        const val INS_GET_DATA = 0xCA.toByte()
        const val INS_PUT_DATA = 0xDA.toByte()
        const val INS_VERIFY = 0x20.toByte()
        const val INS_CHANGE_PIN = 0x24.toByte()
        const val INS_DISABLE_PIN = 0x26.toByte()
        const val INS_ENABLE_PIN = 0x28.toByte()
        const val INS_UNBLOCK_PIN = 0x2C.toByte()
        const val INS_GET_CHALLENGE = 0x84.toByte()
        const val INS_EXTERNAL_AUTHENTICATE = 0x82.toByte()
        const val INS_INTERNAL_AUTHENTICATE = 0x88.toByte()
        const val INS_GENERATE_AC = 0xAE.toByte()
        const val INS_GET_PROCESSING_OPTIONS = 0xA8.toByte()
        const val INS_MANAGE_CHANNEL = 0x70.toByte()
    }

    // P1 Parameters for SELECT command
    object SelectP1 {
        const val SELECT_BY_FID = 0x00
        const val SELECT_CHILD_DF = 0x01
        const val SELECT_CHILD_EF = 0x02
        const val SELECT_PARENT_DF = 0x03
        const val SELECT_BY_AID = 0x04
        const val SELECT_FROM_MF = 0x08
        const val SELECT_FROM_CURRENT_DF = 0x09
    }

    /**
     * Convert byte array to hexadecimal string
     */
    fun bytesToHexString(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)

        for (byte in bytes) {
            val octet = byte.toInt()
            val firstIndex = (octet and 0xF0) ushr 4
            val secondIndex = octet and 0x0F
            result.append(hexChars[firstIndex])
            result.append(hexChars[secondIndex])
        }

        return result.toString()
    }

    /**
     * Convert hexadecimal string to byte array
     */
    fun hexStringToBytes(hex: String): ByteArray {
        val cleanHex = hex.replace("\\s".toRegex(), "").uppercase()

        if (cleanHex.length % 2 != 0) {
            throw IllegalArgumentException("Hex string must have even length")
        }

        val result = ByteArray(cleanHex.length / 2)

        for (i in cleanHex.indices step 2) {
            val firstDigit = cleanHex[i].digitToIntOrNull(16)
                ?: throw IllegalArgumentException("Invalid hex character: ${cleanHex[i]}")
            val secondDigit = cleanHex[i + 1].digitToIntOrNull(16)
                ?: throw IllegalArgumentException("Invalid hex character: ${cleanHex[i + 1]}")

            result[i / 2] = ((firstDigit shl 4) + secondDigit).toByte()
        }

        return result
    }

    /**
     * Convert single byte to hex string
     */
    fun byteToHexString(byte: Byte): String {
        return String.format("%02X", byte.toInt() and 0xFF)
    }

    /**
     * Convert integer to hex string with specified width
     */
    fun intToHexString(value: Int, width: Int): String {
        return String.format("%0${width}X", value)
    }

    /**
     * Create APDU command from hex string parameters
     */
    fun createCommandApdu(
        cla: String,
        ins: String,
        p1: String,
        p2: String,
        data: String = "",
        le: String = ""
    ): ByteArray {
        val baos = ByteArrayOutputStream()

        // Add header (CLA INS P1 P2)
        baos.write(hexStringToBytes(cla))
        baos.write(hexStringToBytes(ins))
        baos.write(hexStringToBytes(p1))
        baos.write(hexStringToBytes(p2))

        val dataBytes = if (data.isNotEmpty()) hexStringToBytes(data) else byteArrayOf()
        val leValue = if (le.isNotEmpty()) hexStringToBytes(le)[0].toInt() and 0xFF else 0

        when {
            // Case 1: No data, no Le
            dataBytes.isEmpty() && leValue == 0 -> {
                // Header only
            }

            // Case 2: No data, with Le
            dataBytes.isEmpty() && leValue > 0 -> {
                baos.write(leValue)
            }

            // Case 3: With data, no Le
            dataBytes.isNotEmpty() && leValue == 0 -> {
                baos.write(dataBytes.size)
                baos.write(dataBytes)
            }

            // Case 4: With data and Le
            dataBytes.isNotEmpty() && leValue > 0 -> {
                baos.write(dataBytes.size)
                baos.write(dataBytes)
                baos.write(leValue)
            }
        }

        return baos.toByteArray()
    }

    /**
     * Parse APDU command from byte array
     */
    fun parseCommandApdu(apdu: ByteArray): ApduCommand? {
        if (apdu.size < 4) return null

        val cla = apdu[0]
        val ins = apdu[1]
        val p1 = apdu[2]
        val p2 = apdu[3]

        var lc = 0
        var data = byteArrayOf()
        var le = 0

        when (apdu.size) {
            4 -> {
                // Case 1: Header only
            }
            5 -> {
                // Case 2: Header + Le
                le = apdu[4].toInt() and 0xFF
            }
            else -> {
                // Case 3 or 4: Header + Lc + Data [+ Le]
                lc = apdu[4].toInt() and 0xFF
                if (lc > 0 && apdu.size >= 5 + lc) {
                    data = apdu.sliceArray(5 until 5 + lc)
                }
                if (apdu.size == 5 + lc + 1) {
                    // Case 4: Has Le
                    le = apdu[5 + lc].toInt() and 0xFF
                }
            }
        }

        return ApduCommand(
            cla = cla,
            ins = ins,
            p1 = p1,
            p2 = p2,
            lc = lc,
            data = data,
            le = le,
            raw = apdu
        )
    }

    /**
     * Parse APDU response
     */
    fun parseResponseApdu(response: ByteArray): ApduResponse? {
        if (response.size < 2) return null

        val data = if (response.size > 2) {
            response.sliceArray(0 until response.size - 2)
        } else {
            byteArrayOf()
        }

        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        val sw = (sw1 shl 8) or sw2

        return ApduResponse(
            data = data,
            sw1 = sw1.toByte(),
            sw2 = sw2.toByte(),
            sw = sw,
            raw = response
        )
    }

    /**
     * Check if status word indicates success
     */
    fun isSuccessResponse(sw: ByteArray): Boolean {
        if (sw.size != 2) return false
        val sw1 = sw[0].toInt() and 0xFF
        val sw2 = sw[1].toInt() and 0xFF
        return sw1 == 0x90 && sw2 == 0x00
    }

    /**
     * Check if status word indicates success
     */
    fun isSuccessResponse(sw: Int): Boolean {
        return sw == 0x9000
    }

    /**
     * Get human-readable status word description
     */
    fun getStatusWordDescription(sw: String): String {
        return when (sw.uppercase()) {
            StatusWords.SW_SUCCESS -> "Success"
            StatusWords.SW_WRONG_LENGTH -> "Wrong length"
            StatusWords.SW_LOGICAL_CHANNEL_NOT_SUPPORTED -> "Logical channel not supported"
            StatusWords.SW_SECURE_MESSAGING_NOT_SUPPORTED -> "Secure messaging not supported"
            StatusWords.SW_COMMAND_NOT_ALLOWED -> "Command not allowed"
            StatusWords.SW_SECURITY_STATUS_NOT_SATISFIED -> "Security status not satisfied"
            StatusWords.SW_AUTHENTICATION_METHOD_BLOCKED -> "Authentication method blocked"
            StatusWords.SW_CONDITIONS_NOT_SATISFIED -> "Conditions not satisfied"
            StatusWords.SW_FILE_NOT_FOUND -> "File not found"
            StatusWords.SW_RECORD_NOT_FOUND -> "Record not found"
            StatusWords.SW_INCORRECT_P1P2 -> "Incorrect P1/P2 parameters"
            StatusWords.SW_REFERENCED_DATA_NOT_FOUND -> "Referenced data not found"
            StatusWords.SW_INS_NOT_SUPPORTED -> "Instruction not supported"
            StatusWords.SW_CLA_NOT_SUPPORTED -> "Class not supported"
            StatusWords.SW_UNKNOWN -> "Unknown error"
            else -> {
                when {
                    sw.startsWith("61") -> "Response bytes available: ${sw.substring(2)}"
                    sw.startsWith("62") -> "Warning: State unchanged"
                    sw.startsWith("63") -> "Warning: State changed"
                    sw.startsWith("64") -> "Execution error"
                    sw.startsWith("65") -> "Execution error: Memory failure"
                    sw.startsWith("69") -> "Command not allowed"
                    sw.startsWith("6A") -> "Wrong parameters"
                    sw.startsWith("6C") -> "Wrong length (correct length: ${sw.substring(2)})"
                    else -> "Unknown status: $sw"
                }
            }
        }
    }

    /**
     * Format APDU for logging
     */
    fun formatApduForLogging(apdu: ByteArray, isCommand: Boolean = true): String {
        if (apdu.isEmpty()) return "Empty APDU"

        val hex = bytesToHexString(apdu)

        return if (isCommand) {
            if (apdu.size >= 4) {
                val cla = byteToHexString(apdu[0])
                val ins = byteToHexString(apdu[1])
                val p1 = byteToHexString(apdu[2])
                val p2 = byteToHexString(apdu[3])
                val insDesc = getInstructionDescription(apdu[1])

                "Command: $hex (CLA=$cla INS=$ins($insDesc) P1=$p1 P2=$p2)"
            } else {
                "Command: $hex (Invalid length)"
            }
        } else {
            if (apdu.size >= 2) {
                val sw = hex.takeLast(4)
                val data = if (apdu.size > 2) hex.dropLast(4) else ""
                val swDesc = getStatusWordDescription(sw)

                if (data.isNotEmpty()) {
                    "Response: $data + $sw ($swDesc)"
                } else {
                    "Response: $sw ($swDesc)"
                }
            } else {
                "Response: $hex (Invalid length)"
            }
        }
    }

    /**
     * Get instruction description
     */
    fun getInstructionDescription(ins: Byte): String {
        return when (ins) {
            Instructions.INS_SELECT -> "SELECT"
            Instructions.INS_READ_BINARY -> "READ BINARY"
            Instructions.INS_UPDATE_BINARY -> "UPDATE BINARY"
            Instructions.INS_READ_RECORD -> "READ RECORD"
            Instructions.INS_UPDATE_RECORD -> "UPDATE RECORD"
            Instructions.INS_APPEND_RECORD -> "APPEND RECORD"
            Instructions.INS_GET_DATA -> "GET DATA"
            Instructions.INS_PUT_DATA -> "PUT DATA"
            Instructions.INS_VERIFY -> "VERIFY"
            Instructions.INS_CHANGE_PIN -> "CHANGE PIN"
            Instructions.INS_DISABLE_PIN -> "DISABLE PIN"
            Instructions.INS_ENABLE_PIN -> "ENABLE PIN"
            Instructions.INS_UNBLOCK_PIN -> "UNBLOCK PIN"
            Instructions.INS_GET_CHALLENGE -> "GET CHALLENGE"
            Instructions.INS_EXTERNAL_AUTHENTICATE -> "EXTERNAL AUTHENTICATE"
            Instructions.INS_INTERNAL_AUTHENTICATE -> "INTERNAL AUTHENTICATE"
            Instructions.INS_GENERATE_AC -> "GENERATE AC"
            Instructions.INS_GET_PROCESSING_OPTIONS -> "GET PROCESSING OPTIONS"
            Instructions.INS_MANAGE_CHANNEL -> "MANAGE CHANNEL"
            else -> "UNKNOWN"
        }
    }

    /**
     * Generate secure random challenge
     */
    fun generateChallenge(length: Int): ByteArray {
        val random = SecureRandom()
        val challenge = ByteArray(length)
        random.nextBytes(challenge)
        return challenge
    }

    /**
     * Create SELECT command by AID
     */
    fun createSelectByAid(aid: String): ByteArray {
        return createCommandApdu(
            cla = "00",
            ins = "A4",
            p1 = "04",
            p2 = "00",
            data = aid,
            le = "00"
        )
    }

    /**
     * Create GET PROCESSING OPTIONS command
     */
    fun createGetProcessingOptions(pdol: String = ""): ByteArray {
        val data = if (pdol.isNotEmpty()) {
            "83${String.format("%02X", pdol.length / 2)}$pdol"
        } else {
            "8300"
        }

        return createCommandApdu(
            cla = "00",
            ins = "A8",
            p1 = "00",
            p2 = "00",
            data = data,
            le = "00"
        )
    }

    /**
     * Create READ RECORD command
     */
    fun createReadRecord(recordNumber: Int, sfi: Int): ByteArray {
        val p2 = ((sfi and 0x1F) shl 3) or 0x04

        return createCommandApdu(
            cla = "00",
            ins = "B2",
            p1 = String.format("%02X", recordNumber),
            p2 = String.format("%02X", p2),
            le = "00"
        )
    }

    /**
     * Create GET DATA command
     */
    fun createGetData(tag: String): ByteArray {
        val tagBytes = hexStringToBytes(tag)
        val p1 = if (tagBytes.isNotEmpty()) String.format("%02X", tagBytes[0].toInt() and 0xFF) else "00"
        val p2 = if (tagBytes.size > 1) String.format("%02X", tagBytes[1].toInt() and 0xFF) else "00"

        return createCommandApdu(
            cla = "00",
            ins = "CA",
            p1 = p1,
            p2 = p2,
            le = "00"
        )
    }

    /**
     * Create VERIFY PIN command
     */
    fun createVerifyPin(pin: String): ByteArray {
        // Format PIN according to EMV specification
        val pinData = formatPinBlock(pin)

        return createCommandApdu(
            cla = "00",
            ins = "20",
            p1 = "00",
            p2 = "80",
            data = pinData
        )
    }

    /**
     * Format PIN block according to EMV specification
     */
    private fun formatPinBlock(pin: String): String {
        if (pin.length > 12) {
            throw IllegalArgumentException("PIN too long")
        }

        val pinLength = String.format("%02X", pin.length)
        val paddedPin = pin.padEnd(12, 'F')

        return "2$pinLength$paddedPin"
    }

    /**
     * Extract AID from SELECT command
     */
    fun extractAidFromSelect(apdu: ByteArray): String? {
        val command = parseCommandApdu(apdu) ?: return null

        return if (command.ins == Instructions.INS_SELECT &&
            command.p1.toInt() and 0xFF == SelectP1.SELECT_BY_AID) {
            bytesToHexString(command.data)
        } else {
            null
        }
    }

    /**
     * Validate APDU structure
     */
    fun validateApdu(apdu: ByteArray): ApduValidationResult {
        return when {
            apdu.size < 4 -> ApduValidationResult(false, "APDU too short (minimum 4 bytes)")
            apdu.size > 65544 -> ApduValidationResult(false, "APDU too long (maximum 65544 bytes)")
            else -> {
                val command = parseCommandApdu(apdu)
                if (command != null) {
                    ApduValidationResult(true, "Valid APDU")
                } else {
                    ApduValidationResult(false, "Invalid APDU structure")
                }
            }
        }
    }

    /**
     * Create success response with data
     */
    fun createSuccessResponse(data: ByteArray = byteArrayOf()): ByteArray {
        return data + byteArrayOf(0x90.toByte(), 0x00.toByte())
    }

    /**
     * Create error response with status word
     */
    fun createErrorResponse(sw1: Byte, sw2: Byte): ByteArray {
        return byteArrayOf(sw1, sw2)
    }

    /**
     * Create error response with status word string
     */
    fun createErrorResponse(sw: String): ByteArray {
        val swBytes = hexStringToBytes(sw)
        return if (swBytes.size == 2) swBytes else byteArrayOf(0x6F.toByte(), 0x00.toByte())
    }
}

data class ApduResponse(
    val data: ByteArray,
    val sw1: Byte,
    val sw2: Byte,
    val sw: Int,
    val raw: ByteArray
) {
    fun toHexString(): String = ApduUtil.bytesToHexString(raw)

    fun getStatusWordString(): String = String.format("%04X", sw)

    fun getStatusWordDescription(): String = ApduUtil.getStatusWordDescription(getStatusWordString())

    fun isSuccess(): Boolean = ApduUtil.isSuccessResponse(sw)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApduResponse

        if (!data.contentEquals(other.data)) return false
        if (sw1 != other.sw1) return false
        if (sw2 != other.sw2) return false
        if (sw != other.sw) return false
        if (!raw.contentEquals(other.raw)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + sw1.toInt()
        result = 31 * result + sw2.toInt()
        result = 31 * result + sw
        result = 31 * result + raw.contentHashCode()
        return result
    }
}

data class ApduValidationResult(
    val isValid: Boolean,
    val message: String
)