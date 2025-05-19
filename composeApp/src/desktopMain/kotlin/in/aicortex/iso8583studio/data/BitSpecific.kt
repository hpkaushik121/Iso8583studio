package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.AddtionalOption
import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.BitType
import kotlinx.serialization.Serializable

@Serializable
data class BitSpecific(
    var bitNumber: Byte = 0,
    var bitLength: BitLength = BitLength.FIXED,
    var bitType: BitType = BitType.NOT_SPECIFIC,
    var maxLength: Int = 0,
    var addtionalOption: AddtionalOption = AddtionalOption.None
) {

    var lengthType: BitLength
        get() = bitLength
        set(value) { bitLength = value }

    var formatType: BitType
        get() = bitType
        set(value) { bitType = value }


    override fun toString(): String {
        return "${bitNumber.toString().padEnd(4)}${bitLength.toString().padEnd(10)}${maxLength.toString().padEnd(4)}${bitType.toString().padEnd(10)}"
    }

    // Secondary constructors to match original code
    constructor(bitNo: Byte, bitLenAtrr: BitLength, bitTypeAtrr: BitType, maxLen: Int) : this() {
        bitNumber = bitNo
        bitLength = bitLenAtrr
        bitType = bitTypeAtrr
        maxLength = maxLen
        addtionalOption = AddtionalOption.None
    }
}