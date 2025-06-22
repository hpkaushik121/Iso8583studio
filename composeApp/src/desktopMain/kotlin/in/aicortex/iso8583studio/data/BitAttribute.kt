package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.AddtionalOption
import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.BitType
import `in`.aicortex.iso8583studio.domain.utils.EMVTag
import `in`.aicortex.iso8583studio.domain.utils.EMVTagParser
import ai.cortex.core.IsoUtil.ascToString
import ai.cortex.core.IsoUtil.bcdToString
import ai.cortex.core.IsoUtil.bytesToHexString
import kotlinx.serialization.Serializable
import org.springframework.context.annotation.Description
import java.nio.charset.Charset

@Serializable
class BitAttribute {
    private var m_LengthAttribute: BitLength = BitLength.FIXED
    private var m_MaxLength: Int = 0
    private var m_Length: Int = 0
    private var m_Data: ByteArray? = null
    private var m_BitType: BitType = BitType.NOT_SPECIFIC
    private var m_IsSet: Boolean = false
    private var m_Option: AddtionalOption = AddtionalOption.None
    private var m_description: String = ""

    var description: String
        get() = m_description
        set(value) {m_description = value}

    var isSet: Boolean
        get() = m_IsSet
        set(value) { m_IsSet = value }

    var lengthAttribute: BitLength
        get() = m_LengthAttribute
        set(value) { m_LengthAttribute = value }

    var maxLength: Int
        get() = m_MaxLength
        set(value) { m_MaxLength = value }

    var length: Int
        get() = m_Length
        set(value) { m_Length = value }

    val aboutUs: String
        get() = "Sourabh Kaushik, sk@aicortex.in"

    var typeAtribute: BitType
        get() = m_BitType
        set(value) { m_BitType = value }

    var data: ByteArray?
        get() = m_Data
        set(value) {
            m_Data = value
        }

    var additionalOption: AddtionalOption
        get() = m_Option
        set(value) { m_Option = value }



    fun getString(): String {
        if (m_Data == null)
            return ""

        return if (typeAtribute != BitType.BCD) {
            ascToString(m_Data!!)
        } else {
            val str = bcdToString(m_Data!!)
            if (length % 2 == 1) {
                if (length >= 6) {
                    str.substring(0, str.length - 1)
                } else {
                    str.substring(1, str.length - 1)
                }
            } else {
                str
            }
        }
    }

    fun getInt(): Int {
        return if (typeAtribute == BitType.BCD) {
            bcdToString(m_Data!!).toInt()
        } else {
            ascToString(m_Data!!).toInt()
        }
    }

    override fun toString(): String {
        if (m_Option == AddtionalOption.HideAll)
            return "*"

        return when (typeAtribute) {
            BitType.AN, BitType.ANS -> {
                m_Data?.let {
                    var str = String(it, Charsets.US_ASCII).replace(0.toChar(), '.')
                    if (m_Option == AddtionalOption.Hide12DigitsOfTrack2 && str.length > 12) {
                        str = "${str.substring(0, 6)}**********${str.substring(12, if (str.length > 21) 21 else str.length)}"
                    }
                    str
                } ?: ""

            }
            BitType.BCD -> {
                m_Data?.let {
                    var str = bcdToString(it)
                    if (m_Option == AddtionalOption.Hide12DigitsOfTrack2 && str.length > 12) {
                        str = "${str.substring(0, 6)}**********${str.substring(12, if (str.length > 21) 21 else str.length)}"
                    }
                    str
                } ?: ""

            }
            BitType.BINARY -> {
                m_Data?.let { bytesToHexString(it, 32, false) } ?: ""
            }
            else -> ""
        }
    }

    fun getEmvTags(): EMVTagParser.EMVParseResult{
        val result = data?.let { EMVTagParser.parseEMVTags(it) }
        return result ?: EMVTagParser.EMVParseResult(
            tags = emptyList(),
            totalBytesProcessed = 0
        )
    }
}

/**
 * Creates a deep copy of a BitAttribute
 */
fun clone(bitAttribute: BitAttribute): BitAttribute {
    return BitAttribute().apply {
        isSet = bitAttribute.isSet
        data = bitAttribute.data?.clone()
        maxLength = bitAttribute.maxLength
        typeAtribute = bitAttribute.typeAtribute
        lengthAttribute = bitAttribute.lengthAttribute
        length = bitAttribute.length
        additionalOption = bitAttribute.additionalOption
        description = bitAttribute.description

    }
}