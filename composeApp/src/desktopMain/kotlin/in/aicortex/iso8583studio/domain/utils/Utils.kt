package `in`.aicortex.iso8583studio.domain.utils

import ai.cortex.core.IsoUtil.bytesToHexString
import ai.cortex.core.IsoUtil.hexStringToBinary
import ai.cortex.core.IsoUtil.stringToAN
import ai.cortex.core.IsoUtil.stringToANS
import ai.cortex.core.IsoUtil.stringToBcd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.BitType
import `in`.aicortex.iso8583studio.data.model.CipherType
import `in`.aicortex.iso8583studio.data.model.MessageLengthType

object Utils {
    fun formatDate(milliseconds: Long, pattern: String = "dd MMM yyyy HH:mm a"): String {
        val date = Date(milliseconds)
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }

    fun messageLengthToInt(bytes: ByteArray, type: MessageLengthType): Int {
        // Implementation depends on the specific format used
        return when (type) {
            MessageLengthType.BCD -> {
                // BCD decoding logic
                val value = bytes[0].toInt() and 0xFF
                value * 100 + ((bytes[1].toInt() and 0xF0) shr 4) * 10 + (bytes[1].toInt() and 0x0F)
            }

            MessageLengthType.HEX_HL -> {
                // High byte, Low byte format
                (bytes[0].toInt() and 0xFF) * 256 + (bytes[1].toInt() and 0xFF)
            }

            MessageLengthType.HEX_LH -> {
                // Low byte, High byte format
                (bytes[1].toInt() and 0xFF) * 256 + (bytes[0].toInt() and 0xFF)
            }

            MessageLengthType.NONE -> TODO()
            MessageLengthType.STRING_4 -> TODO()
        }
    }

    fun intToMessageLength(value: Int, type: MessageLengthType): ByteArray {
        val result = ByteArray(2)
        when (type) {
            MessageLengthType.BCD -> {
                // BCD encoding logic
                result[0] = (value / 100).toByte()
                result[1] = (((value % 100) / 10) shl 4 or (value % 10)).toByte()
            }

            MessageLengthType.HEX_HL -> {
                // High byte, Low byte format
                result[0] = (value shr 8).toByte()
                result[1] = (value and 0xFF).toByte()
            }

            MessageLengthType.HEX_LH -> {
                // Low byte, High byte format
                result[0] = (value and 0xFF).toByte()
                result[1] = (value shr 8).toByte()
            }

            MessageLengthType.NONE -> TODO()
            MessageLengthType.STRING_4 -> TODO()
        }
        return result
    }


    fun getBytesFromBytes(source: ByteArray, offset: Int, length: Int): ByteArray {
        return source.copyOfRange(offset, offset + length)
    }


    fun bytesCopy(
        source: ByteArray,
        destination: ByteArray,
        sourcePos: Int,
        destPos: Int,
        length: Int
    ) {
        source.copyInto(destination, destPos, sourcePos, sourcePos + length)
    }


    fun creatBytesFromArray(src: ByteArray, offset: Int, length: Int): ByteArray {
        return src.copyOfRange(offset, offset + length)
    }

    fun stringToRequiredByteArray(type: BitType, length: BitLength, string: String): ByteArray {
        return when (type) {
            BitType.BINARY -> hexStringToBinary(
                bytesToHexString(
                    stringToBcd(
                        string,
                        string.length
                    )
                )
            )

            BitType.BCD -> stringToBcd(string, string.length)
            BitType.NOT_SPECIFIC -> string.toByteArray()

            BitType.AN -> stringToAN(string, string.length)
            BitType.ANS -> stringToANS(string, string.length)
        }
    }

    fun kvc(key: ByteArray, cipherType: CipherType): ByteArray {
        // Placeholder for KVC calculation
        return ByteArray(3)
    }

    fun convertToTIDIALERRule(buf: ByteArray, lengthType: MessageLengthType): ByteArray {
        val result = ByteArray(buf.size + 2) // STX + ETX
        result[0] = 2 // STX
        buf.copyInto(result, 1, 0, buf.size)
        result[result.size - 1] = 3 // ETX
        return result
    }


}