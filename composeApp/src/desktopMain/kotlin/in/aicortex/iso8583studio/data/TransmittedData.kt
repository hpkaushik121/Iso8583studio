package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.GWHeaderTAG
import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import `in`.aicortex.iso8583studio.data.model.TagLengthValue
import `in`.aicortex.iso8583studio.data.model.VerificationError
import `in`.aicortex.iso8583studio.data.model.VerificationException
import `in`.aicortex.iso8583studio.domain.utils.Utils.getBytesFromBytes
import `in`.aicortex.iso8583studio.domain.utils.Utils.intToMessageLength
import `in`.aicortex.iso8583studio.domain.utils.Utils.messageLengthToInt
import java.io.IOException
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.time.LocalDateTime


open class TransmittedData(lengthType: MessageLengthType) {
    protected var m_ReadMessage: ByteArray? = null
    protected var m_LengthType: MessageLengthType = lengthType
    protected var m_WrittenMessage: ByteArray? = null
    protected var _FixedHeader: ByteArray = ByteArray(FIXED_HEADER_SIZE)

    var lengthType: MessageLengthType
        get() = m_LengthType
        set(value) { m_LengthType = value }

    var fixedHeader: ByteArray
        get() = _FixedHeader
        set(value) { _FixedHeader = value }

    var writtenMessage: ByteArray?
        get() = m_WrittenMessage
        set(value) { m_WrittenMessage = value }

    var readMessage: ByteArray?
        get() = m_ReadMessage
        set(value) { m_ReadMessage = value }

    companion object {
        const val FIXED_HEADER_SIZE = 5

        @JvmStatic
        fun packAdminResponse(errorCode: String, content: String): ByteArray {
            val tags = sortedMapOf<GWHeaderTAG, ByteArray?>()
            tags[GWHeaderTAG.TAG_MESSAGETYPE] = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(45072.toShort()).array()
            tags[GWHeaderTAG.TAG_AMDIN_CONTENT] = content.toByteArray(Charset.forName("ASCII"))
            tags[GWHeaderTAG.TAG_ERROR_CODE] = byteArrayOf('0'.toByte(), '0'.toByte())

            val packedTags = TagLengthValue.packTAGs(tags)
            val result = ByteArray(packedTags.size + 7)

            intToMessageLength(result.size - 2, MessageLengthType.BCD).copyInto(result, 0)
            packedTags.copyInto(result, 7)

            return result
        }

        @JvmStatic
        fun packAdminRequest(command: String, content: String): ByteArray {
            return packAdminRequest(command, content, MessageLengthType.BCD)
        }

        @JvmStatic
        fun packAdminRequest(command: String, content: String, lengthType: MessageLengthType): ByteArray {
            val tags = sortedMapOf<GWHeaderTAG, ByteArray?>()
            tags[GWHeaderTAG.TAG_MESSAGETYPE] = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(45056.toShort()).array()
            tags[GWHeaderTAG.TAG_ADMIN_SECRET] = KeyManagement.secretKey.encrypt(
                ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(System.currentTimeMillis()).array()
            )
            tags[GWHeaderTAG.TAG_AMDIN_CONTENT] = content.toByteArray(Charset.forName("ASCII"))
            tags[GWHeaderTAG.TAG_ADMIN_COMMAND] = command.toByteArray(Charset.forName("ASCII"))

            val packedTags = TagLengthValue.packTAGs(tags)
            val result = ByteArray(packedTags.size + 7)

            intToMessageLength(result.size - 2, lengthType).copyInto(result, 0)
            packedTags.copyInto(result, 7)

            return result
        }
    }

    private fun read(inputStream: java.io.InputStream, isSource: Boolean) {
        val lengthBytes = ByteArray(2)
        val startTime = LocalDateTime.now()

        if (m_LengthType == MessageLengthType.NONE) {
            val buffer = ByteArray(99999)
            val len = inputStream.read(buffer, 0, 99999)
            m_ReadMessage = getBytesFromBytes(buffer, 0, len)
        } else {
            try {
                if (inputStream.read(lengthBytes, 0, 2) < 2) {
                    val currentMillis = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis()
                    if (currentMillis >= inputStream.available()) {
                        throw VerificationException("CAN NOT READ FROM THE NETWORK STREAM", VerificationError.TIMEOUT)
                    }
                    throw VerificationException(
                        "THE CONNECTION WAS CLOSED BY REMOTE COMPUTER/TERMINAL",
                        if (isSource) VerificationError.DISCONNECTED_FROM_SOURCE else VerificationError.DISCONNECTED_FROM_DESTINATION
                    )
                }
            } catch (ex: IOException) {
                throw VerificationException(
                    ex.message ?: "IO Error",
                    if (isSource) VerificationError.DISCONNECTED_FROM_SOURCE else VerificationError.DISCONNECTED_FROM_DESTINATION
                )
            }

            val messageLength = messageLengthToInt(lengthBytes, m_LengthType)
            m_ReadMessage = ByteArray(messageLength)

            if (inputStream.read(m_ReadMessage, 0, messageLength) < messageLength) {
                val currentMillis = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis()
                if (currentMillis >= inputStream.available().toLong()) {
                    throw VerificationException("CAN NOT READ FROM THE NETWORK STREAM", VerificationError.TIMEOUT)
                }
                throw VerificationException(
                    "THE CONNECTION WAS CLOSED BY REMOTE COMPUTER/TERMINAL",
                    if (isSource) VerificationError.DISCONNECTED_FROM_SOURCE else VerificationError.DISCONNECTED_FROM_DESTINATION
                )
            }

            if (inputStream.available() > 0) {
                throw VerificationException("THERE ARE REDUNDANT BYTES", VerificationError.MESSAGE_LENGTH_ERROR)
            }
        }
    }

    fun read(client: Socket, timeOut: Int, isSource: Boolean) {
        client.soTimeout = timeOut * 1000
        read(client.getInputStream(), isSource)
    }

    fun write(outputStream: java.io.OutputStream) {
        m_WrittenMessage?.let { message ->
            if (m_LengthType != MessageLengthType.NONE) {
                val buffer = ByteArray(message.size + 2)
                intToMessageLength(message.size, m_LengthType).copyInto(buffer, 0)
                message.copyInto(buffer, 2)
                outputStream.write(buffer, 0, buffer.size)
            } else {
                outputStream.write(message, 0, message.size)
            }
        }
    }

    fun write(outputStream: java.io.OutputStream, dataToWrite: ByteArray) {
        m_WrittenMessage = dataToWrite
        write(outputStream)
    }
}