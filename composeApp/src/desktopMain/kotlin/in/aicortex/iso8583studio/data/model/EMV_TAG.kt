package `in`.aicortex.iso8583studio.data.model

import `in`.aicortex.iso8583studio.data.EMVAnalyzer
import ai.cortex.core.IsoUtil
import `in`.aicortex.iso8583studio.domain.utils.Utils
import java.nio.charset.Charset
import java.util.*

class EMV_TAG(
    var TAG: String? = null,
    var name: String? = null,
    var description: String? = null,
    var type: String? = null,
    var length: Int = 0
) {
    var value: ByteArray? = null
    var HasException: Exception? = null
    private var EMVRow: EMV_TAG? = null
    constructor(source: ByteArray, index: IntArray) : this() {
        try {
            TAG = IsoUtil.bcdToString(Utils.getBytesFromBytes(source, index[0], 2))
            EMVRow = EMVAnalyzer.EMV_INFO.findByTag(TAG!!.substring(0, 2))

            if (EMVRow != null) {
                TAG = TAG!!.substring(0, 2)
                length = source[index[0] + 1].toInt()
                value = ByteArray(length)
                Utils.bytesCopy(value!!, source, 0, index[0] + 2, length)
                index[0] += length + 2
            } else {
                EMVRow = EMVAnalyzer.EMV_INFO.findByTag(TAG!!)
                if (EMVRow == null)
                    throw Exception("Invalid EMV tag $TAG")
                length = source[index[0] + 2].toInt()
                value = ByteArray(length)
                Utils.bytesCopy(value!!, source, 0, index[0] + 3, length)
                index[0] += length + 3
            }
        } catch (ex: Exception) {
            HasException = ex
        }
    }

    fun toString(options: EMVShowOption): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("$TAG Len=${length}")

        if (options.value and EMVShowOption.NAME.value > EMVShowOption.None.value) {
            stringBuilder.append(" ${EMVRow!!.name}")
        }

        if (options.value and EMVShowOption.VALUE.value > EMVShowOption.None.value) {
            if (EMVRow!!.type?.substring(0, 1)?.lowercase() == "a") {
                stringBuilder.append(" \"${String(value!!, Charset.defaultCharset())}\"")
            } else {
                stringBuilder.append(" \"${IsoUtil.bcdToString(value!!)}\"")
            }
        }

        if (options.value and EMVShowOption.DESCRIPTION.value > EMVShowOption.None.value) {
            stringBuilder.append("\r\n  ${EMVRow!!.description}")
        }

//        if (options.value and EMVShowOption.BITS.value > EMVShowOption.None.value) {
//            val tagBitsRows = EMVRow!!.GetTAG_BITSRows()
//            if (tagBitsRows.isNotEmpty()) {
//                val bitArray = BitSet.valueOf(value)
//                for (tagBitsRow in tagBitsRows) {
//                    val isSet = bitArray[tagBitsRow.BIT - 1]
//                    stringBuilder.append("\r\n      ${tagBitsRow.Description}${if (isSet) ": Yes" else ": No"}")
//                }
//            }
//        }

        return stringBuilder.toString()
    }
}