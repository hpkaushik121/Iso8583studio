package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.BitType
import `in`.aicortex.iso8583studio.data.model.EMVShowOption
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import java.io.InputStream
import java.net.Socket
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Kotlin implementation of Iso8583Data for handling ISO 8583 financial messages
 */
class Iso8583Data(config: GatewayConfig) {
    companion object {
        const val MAX_PACKAGE_SIZE = 10024
        const val MAX_BITS = 128

        val aboutUs: String = "Sourabh Kaushik, sk@aicortex.in"
    }

    private var m_MessageType: String = "0200"
    protected var m_BitAttributes: Array<BitAttribute> = Array(MAX_BITS) { BitAttribute() }
    private var m_TPDU = TPDU()
    private var buffer = ByteArray(MAX_PACKAGE_SIZE)
    private var m_PackageSize: Int = 0
    private var m_LastBitError: Int = 0
    var messageLength: Int = 0
    private var m_BitmapInAscii: Boolean = false
    var hasHeader: Boolean = true
    private var m_LengthInAsc: Boolean = false
    var emvShowOptions: EMVShowOption = EMVShowOption.None

    var lengthInAsc: Boolean
        get() = m_LengthInAsc
        set(value) { m_LengthInAsc = value }

    var bitmapInAscii: Boolean
        get() = m_BitmapInAscii
        set(value) { m_BitmapInAscii = value }


    /**
     * Access BitAttributes by bit number (1-128)
     */
    operator fun get(bitNumber: Int): BitAttribute? {
        if (bitNumber < 1 || bitNumber > MAX_BITS) return null
        return m_BitAttributes[bitNumber - 1]
    }

    /**
     * Set BitAttributes by bit number (1-128)
     */
    operator fun set(bitNumber: Int, value: BitAttribute?) {
        if (bitNumber < 1 || bitNumber > MAX_BITS || value == null) return

        val bitAttribute = m_BitAttributes[bitNumber - 1]
        if (value.data != null) {
            bitAttribute.data = value.data?.clone()
        }
        bitAttribute.length = value.length
        bitAttribute.lengthAttribute = value.lengthAttribute
        bitAttribute.maxLength = value.maxLength
        bitAttribute.typeAtribute = value.typeAtribute
    }

    /**
     * Default constructor
     */
    init{
        m_TPDU = TPDU()
        m_BitAttributes = BitTemplate.getGeneralTemplate()
        hasHeader = !config.doNotUseHeader
        bitmapInAscii = config.bitmapInAscii
    }

    /**
     * Constructor with bit template
     */
    constructor(template: Array<BitSpecific>,config: GatewayConfig): this(config) {
        m_TPDU = TPDU()
        m_BitAttributes = BitTemplate.getBitAttributeArray(template)
        hasHeader = !config.doNotUseHeader
        bitmapInAscii = config.bitmapInAscii

    }

    var bitAttributes: Array<BitAttribute>
        get() = m_BitAttributes
        set(value) { m_BitAttributes = value }

    val tpduHeader: TPDU
        get() = m_TPDU

    /**
     * Pack a string value into a specified bit
     */
    fun packBit(bitNumber: Int, value: String) {
        val index = bitNumber - 1

        when (m_BitAttributes[index].typeAtribute) {
            BitType.AN, BitType.ANS -> {
                m_BitAttributes[index].length = value.length
                m_BitAttributes[index].data = IsoUtil.stringToAsc(value)
            }
            BitType.BCD -> {
                m_BitAttributes[index].length = if (m_BitAttributes[index].lengthAttribute != BitLength.FIXED) {
                    value.length
                } else {
                    m_BitAttributes[index].maxLength
                }
                m_BitAttributes[index].data = IsoUtil.stringToBCD(value, (m_BitAttributes[index].length + 1) / 2)
            }
            BitType.BINARY -> {
                m_BitAttributes[index].length = (value.length + 1) / 2
                m_BitAttributes[index].data = IsoUtil.stringToBCD(value, m_BitAttributes[index].length)
            }
            else -> {}
        }

        when (m_BitAttributes[index].lengthAttribute) {
            BitLength.LLVAR -> {
                if (m_BitAttributes[index].length > 99) {
                    throw Exception("Field ${index + 1}'s length > 99 !!")
                }
            }
            BitLength.LLLVAR -> {
                if (m_BitAttributes[index].length > 999) {
                    throw Exception("Field ${index + 1}'s length > 999 !!")
                }
            }
            else -> {}
        }

        m_BitAttributes[index].isSet = true
    }

    /**
     * Reset all bits
     */
    fun reset() {
        for (i in 0 until MAX_BITS) {
            m_BitAttributes[i].isSet = false
        }
    }

    /**
     * Pack bytes into a specified bit
     */
    fun packBit(bitNumber: Int, bytes: ByteArray) {
        val index = bitNumber - 1

        m_BitAttributes[index].data = bytes.clone()

        when (m_BitAttributes[index].typeAtribute) {
            BitType.AN, BitType.ANS -> m_BitAttributes[index].length = bytes.size
            BitType.BCD -> m_BitAttributes[index].length = bytes.size * 2
            BitType.BINARY -> m_BitAttributes[index].length = bytes.size
            else -> {}
        }

        m_BitAttributes[index].isSet = true
    }



    /**
     * Set a bit to active or inactive
     */
    fun bitSet(bitNumber: Int, isSet: Boolean) {
        if (bitNumber < 0 || bitNumber >= MAX_BITS) return
        m_BitAttributes[bitNumber].isSet = isSet
    }

    /**
     * Check if a bit is set
     */
    fun isBitSet(bitNumber: Int): Boolean {
        if (bitNumber < 0 || bitNumber >= MAX_BITS) return false
        return m_BitAttributes[bitNumber].isSet
    }

    var messageType: String
        get() = m_MessageType
        set(value) { m_MessageType = value }

    /**
     * Pack message with no length type
     */
    fun pack(): ByteArray = pack(MessageLengthType.NONE)

    /**
     * Pack message with specified length type
     */
    fun pack(lengthType: MessageLengthType): ByteArray {
        m_PackageSize = when (lengthType) {
            MessageLengthType.NONE -> 0
            MessageLengthType.STRING_4 -> 4
            else -> 2
        }

        if (hasHeader) {
            m_TPDU.pack().copyInto(buffer, m_PackageSize)
            m_PackageSize += 5
        }

        if (m_LengthInAsc) {
            // Format message type as string
            val msgTypeStr = m_MessageType.toString().padStart(4, '0')
            msgTypeStr.toByteArray(Charset.defaultCharset()).copyInto(buffer, m_PackageSize)
            m_PackageSize += 4
        } else {
            // Convert message type to BCD
            IsoUtil.binToBcd(m_MessageType.toInt(), 2).copyInto(buffer, m_PackageSize)
            m_PackageSize += 2
        }

        if (bitmapInAscii) {
            val bitmapStr = IsoUtil.bcdToString(createBitmap())
            bitmapStr.toByteArray(Charset.defaultCharset()).copyInto(buffer, m_PackageSize)
            m_PackageSize += 16

            if (m_BitAttributes[0].isSet) {
                // Secondary bitmap exists
                val secondaryBitmapStr = IsoUtil.bcdToString(m_BitAttributes[0].data ?: ByteArray(0))
                secondaryBitmapStr.toByteArray(Charset.defaultCharset()).copyInto(buffer, m_PackageSize)
                m_PackageSize += 16
            }
        } else {
            createBitmap().copyInto(buffer, m_PackageSize)
            m_PackageSize += 8
        }

        // Determine maximum bit number to process
        val maxBit = if (!m_BitAttributes[0].isSet) 64 else 128

        // Process each bit field
        for (i in 0 until maxBit) {
            if (m_BitAttributes[i].isSet && (i != 0 || !bitmapInAscii)) {
                // Process based on length attribute
                when (m_BitAttributes[i].lengthAttribute) {
                    BitLength.FIXED -> {
                        m_BitAttributes[i].data?.copyInto(buffer, m_PackageSize)
                    }
                    BitLength.LLVAR -> {
                        if (m_LengthInAsc) {
                            // ASCII representation of length
                            val lenStr = m_BitAttributes[i].length.toString().padStart(2, '0')
                            lenStr.toByteArray(Charset.defaultCharset()).copyInto(buffer, m_PackageSize)
                            m_PackageSize += 2
                            m_BitAttributes[i].data?.copyInto(buffer, m_PackageSize)
                        } else {
                            // BCD representation of length
                            IsoUtil.binToBcd(m_BitAttributes[i].length, 1).copyInto(buffer, m_PackageSize)
                            m_PackageSize += 1
                            m_BitAttributes[i].data?.copyInto(buffer, m_PackageSize)
                        }
                    }
                    BitLength.LLLVAR -> {
                        if (m_LengthInAsc) {
                            // ASCII representation of length
                            val lenStr = m_BitAttributes[i].length.toString().padStart(3, '0')
                            lenStr.toByteArray(Charset.defaultCharset()).copyInto(buffer, m_PackageSize)
                            m_PackageSize += 3
                            m_BitAttributes[i].data?.copyInto(buffer, m_PackageSize)
                        } else {
                            // BCD representation of length
                            IsoUtil.binToBcd(m_BitAttributes[i].length, 2).copyInto(buffer, m_PackageSize)
                            m_PackageSize += 2
                            m_BitAttributes[i].data?.copyInto(buffer, m_PackageSize)
                        }
                    }
                }

                // Update package size based on field type
                m_PackageSize += when (m_BitAttributes[i].typeAtribute) {
                    BitType.AN, BitType.ANS -> m_BitAttributes[i].length
                    BitType.BCD -> (m_BitAttributes[i].length + 1) / 2
                    BitType.BINARY -> m_BitAttributes[i].length
                    else -> 0
                }
            }
        }

        // Finalize the message based on length type
        return when (lengthType) {
            MessageLengthType.NONE -> {
                // Return the buffer directly
                buffer.copyOfRange(0, m_PackageSize)
            }
            MessageLengthType.STRING_4 -> {
                // Insert 4-character string length
                val lenStr = (m_PackageSize - 4).toString().padStart(4, '0')
                lenStr.toByteArray(Charset.defaultCharset()).copyInto(buffer, 0)
                buffer.copyOfRange(0, m_PackageSize)
            }
            else -> {
                // Insert binary length
                IsoUtil.intToMessageLength(m_PackageSize - 2, lengthType).copyInto(buffer, 0)
                buffer.copyOfRange(0, m_PackageSize)
            }
        }
    }

    /**
     * Create bitmap representing active bits
     */
    fun createBitmap(): ByteArray {
        val bitmap = ByteArray(8)
        val bitValues = byteArrayOf(
            0x80.toByte(), 0x40.toByte(), 0x20.toByte(), 0x10.toByte(),
            0x08.toByte(), 0x04.toByte(), 0x02.toByte(), 0x01.toByte()
        )

        for (i in 0 until MAX_BITS) {
            if (m_BitAttributes[i].isSet) {
                if (i >= 64) {
                    // Set bit in second bitmap
                    if (this[1]?.data == null) {
                        this[1]?.data = ByteArray(8)
                    }
                    this[1]?.data?.let { secondaryBitmap ->
                        val bitPos = (i - 64) / 8
                        val bitIndex = (i - 64) % 8
                        secondaryBitmap[bitPos] = (secondaryBitmap[bitPos].toInt() or bitValues[bitIndex].toInt()).toByte()
                    }
                } else {
                    // Set bit in primary bitmap
                    val bitPos = i / 8
                    val bitIndex = i % 8
                    bitmap[bitPos] = (bitmap[bitPos].toInt() or bitValues[bitIndex].toInt()).toByte()
                }
            }
        }

        // Set bit 1 if secondary bitmap exists
        if (this[1]?.data != null) {
            bitmap[0] = (bitmap[0].toInt() or 0x80).toByte()
            this[1]?.isSet = true
            this[1]?.length = 8
        }

        return bitmap
    }

    /**
     * Analyze bitmap to determine which bits are set
     */
    private fun analyzeBitmap(array: ByteArray) {
        // Process primary bitmap (first 8 bytes)
        val primaryBytes = array.copyOfRange(0, 8)
        for (i in 0 until 64) {
            val byteIndex = i / 8
            val bitIndex = 7 - (i % 8)  // Bits are ordered from most to least significant
            val bitValue = (primaryBytes[byteIndex].toInt() and (1 shl bitIndex)) != 0
            m_BitAttributes[i].isSet = bitValue
        }

        // Process secondary bitmap if present (next 8 bytes)
        if (array.size >= 16) {
            val secondaryBytes = array.copyOfRange(8, 16)
            for (i in 0 until 64) {
                val byteIndex = i / 8
                val bitIndex = 7 - (i % 8)
                val bitValue = (secondaryBytes[byteIndex].toInt() and (1 shl bitIndex)) != 0
                m_BitAttributes[i + 64].isSet = bitValue
            }
        }
    }

    /**
     * Get raw message from buffer
     */
    val rawMessage: ByteArray
        get() {
            val result = ByteArray(messageLength)
            IsoUtil.bytesCopy(result, buffer, 0, 0, messageLength)
            return result
        }

    /**
     * Unpack ISO 8583 message
     */
    open fun unpack(input: ByteArray) = unpack(input, 0, input.size)

    /**
     * Unpack ISO 8583 message with offset and length
     */
    open fun unpack(input: ByteArray, from: Int, length: Int) {
        messageLength = length + from
        input.copyInto(buffer, 0, 0, messageLength)

        var position = from

        // Process header if present
        if (hasHeader) {
            val headerBytes = ByteArray(5)
            buffer.copyInto(headerBytes, 0, position, position + 5)
            position += 5
            m_TPDU.unPack(headerBytes)
        }

        // Extract message type
        if (m_LengthInAsc) {
            // ASCII format
            val msgTypeStr = buffer.copyOfRange(position, position + 4)
                .toString(Charset.defaultCharset())
            m_MessageType = msgTypeStr
            position += 4
        } else {
            // BCD format
            val msgTypeBytes = ByteArray(2)
            buffer.copyInto(msgTypeBytes, 0, position, position + 2)
            m_MessageType = IsoUtil.bcdToBin(msgTypeBytes).toString()
            position += 2
        }

        // Process bitmap
        if (bitmapInAscii) {
            // ASCII bitmap format
            val bitmapBytes = ByteArray(16)
            m_BitAttributes[0].maxLength = 16

            // Convert ASCII bitmap to BCD
            val asciiPrimaryBitmap = String(input, position, 16, Charset.defaultCharset())
            IsoUtil.stringToBCD(asciiPrimaryBitmap, 8).copyInto(bitmapBytes, 0)

            // Check if secondary bitmap exists (bit 1 is set)
            if ((bitmapBytes[0].toInt() and 0x80) > 0) {
                val asciiSecondaryBitmap = String(input, position + 16, 16, Charset.defaultCharset())
                IsoUtil.stringToBCD(asciiSecondaryBitmap, 8).copyInto(bitmapBytes, 8)
            }

            analyzeBitmap(bitmapBytes)
            position += 16

        } else {
            // Binary bitmap format
            if ((input[position].toInt() and 0x80) > 0) {
                // Secondary bitmap exists
                analyzeBitmap(input.copyOfRange(position, position + 16))
            } else {
                // Only primary bitmap
                analyzeBitmap(input.copyOfRange(position, position + 8))
            }
            position += 8
        }

        // Process data fields
        val maxBit = if (!m_BitAttributes[0].isSet) 64 else 128

        for (i in 0 until maxBit) {
            if (m_BitAttributes[i].isSet) {
                m_LastBitError = i

                // Determine field length
                when (m_BitAttributes[i].lengthAttribute) {
                    BitLength.FIXED -> {
                        m_BitAttributes[i].length = m_BitAttributes[i].maxLength
                    }
                    BitLength.LLVAR -> {
                        if (m_LengthInAsc) {
                            // Length in ASCII format (2 characters)
                            val lenStr = String(buffer, position, 2, Charset.defaultCharset())
                            m_BitAttributes[i].length = lenStr.toInt()
                            position += 2
                        } else {
                            // Length in BCD format (1 byte)
                            val lenByte = ByteArray(1)
                            buffer.copyInto(lenByte, 0, position, position + 1)
                            m_BitAttributes[i].length = IsoUtil.bcdToBin(lenByte)
                            position += 1
                        }
                    }
                    BitLength.LLLVAR -> {
                        if (m_LengthInAsc) {
                            // Length in ASCII format (3 characters)
                            val lenStr = String(buffer, position, 3, Charset.defaultCharset())
                            m_BitAttributes[i].length = lenStr.toInt()
                            position += 3
                        } else {
                            // Length in BCD format (2 bytes)
                            val lenBytes = ByteArray(2)
                            buffer.copyInto(lenBytes, 0, position, position + 2)
                            m_BitAttributes[i].length = IsoUtil.bcdToBin(lenBytes)
                            position += 2
                        }
                    }
                }

                // Extract field data based on type
                when (m_BitAttributes[i].typeAtribute) {
                    BitType.AN, BitType.ANS -> {
                        // ASCII/EBCDIC character data
                        m_BitAttributes[i].data = ByteArray(m_BitAttributes[i].length)
                        buffer.copyInto(m_BitAttributes[i].data!!, 0, position, position + m_BitAttributes[i].length)
                        position += m_BitAttributes[i].length
                    }
                    BitType.BCD -> {
                        // BCD numeric data
                        val dataLength = (m_BitAttributes[i].length + 1) / 2
                        m_BitAttributes[i].data = ByteArray(dataLength)
                        buffer.copyInto(m_BitAttributes[i].data!!, 0, position, position + dataLength)
                        position += dataLength
                    }
                    BitType.BINARY -> {
                        // Binary data
                        m_BitAttributes[i].data = ByteArray(m_BitAttributes[i].length)
                        buffer.copyInto(m_BitAttributes[i].data!!, 0, position, position + m_BitAttributes[i].length)
                        position += m_BitAttributes[i].length
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Wait for data from a stream (with timeout)
     */
    private fun waitForData(stream: InputStream, timeout: Int) {
        if (stream !is Socket) return

        val socket = stream
        val startTime = LocalDateTime.now()

        while (socket.inputStream.available() == 0) {
            Thread.sleep(20)

            if (ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()) >= timeout) {
                return
            }
        }
    }

    /**
     * Generate log format of the message
     */
    fun logFormat(): String = logFormat(MAX_BITS)

    /**
     * Generate log format up to specified bit
     */
    fun logFormat(endBits: Int): String {
        val sb = StringBuilder()
        sb.append("Message Type = $m_MessageType\r\n")

        for (i in 0 until endBits) {
            if (m_BitAttributes[i].isSet) {
                sb.append("Field ")
                sb.append(i + 1)
                sb.append(" = \"")
                sb.append(m_BitAttributes[i].toString())
                sb.append("\"\r\n")

                // Handle EMV fields if needed
                if ((i == 54 || i == 55) && emvShowOptions != EMVShowOption.None) {
                    m_BitAttributes[i].data?.let {
                        sb.append(EMVAnalyzer.getFullDescription(
                            it,emvShowOptions
                        ))
                    }
                }
            }
        }

        return sb.toString()
    }
    fun getValue(index: Int) : String? {
        val bitAttribute = bitAttributes[index]
        return try {
            bitAttribute.getValue()
        }catch (e: Exception){
            ""
        }
    }

    /**
     * Get the last bit that had an error during processing
     */
    val lastBitError: Int
        get() = m_LastBitError
}
fun BitAttribute.updateBit(index: Int,value: String){

    when (typeAtribute) {
        BitType.AN, BitType.ANS -> {
            length = value.length
            data = IsoUtil.stringToAsc(value)
        }
        BitType.BCD -> {
            length = if (lengthAttribute != BitLength.FIXED) {
                value.length
            } else {
                maxLength
            }
            data = IsoUtil.stringToBCD(value, (length + 1) / 2)
        }
        BitType.BINARY -> {
            length = (value.length + 1) / 2
            data = IsoUtil.stringToBCD(value, length)
        }
        else -> {}
    }

    when (lengthAttribute) {
        BitLength.LLVAR -> {
            if (length > 99) {
                throw Exception("Field ${index + 1}'s length > 99 !!")
            }
        }
        BitLength.LLLVAR -> {
            if (length > 999) {
                throw Exception("Field ${index + 1}'s length > 999 !!")
            }
        }
        else -> {}
    }

    isSet = true
}


fun BitAttribute.getValue(): String? {

    return when (typeAtribute) {
        BitType.AN, BitType.ANS -> {
            IsoUtil.ascToString(data!!)
        }

        BitType.BCD,
        BitType.BINARY -> {
            IsoUtil.bcdToString(data!!)
        }

        else -> {
            null
        }
    }
}