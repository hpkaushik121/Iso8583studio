package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.AddtionalOption
import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.BitType
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.xml.bind.JAXBContext
import kotlin.math.absoluteValue

class BitTemplate {
    companion object {
        @JvmStatic
        val AboutUs: String get() = "Sourabh Kaushik, sk@aicortex.in"

        @JvmStatic
        fun getBINARYpecificArray(filename: String): Array<BitSpecific> {
            FileInputStream(filename).use { fileStream ->
                val ois = ObjectInputStream(fileStream)
                return ois.readObject() as Array<BitSpecific>
            }
        }

        @JvmStatic
        fun writeBitArributeArray(input: Array<BitSpecific>, filename: String) {
            FileOutputStream(filename).use { fileStream ->
                val oos = ObjectOutputStream(fileStream)
                oos.writeObject(input)
            }
        }

        @JvmStatic
        fun getBINARYpecificArray_xml(filename: String): Array<BitSpecific> {
            FileInputStream(filename).use { fileStream ->
                val context = JAXBContext.newInstance(Array<BitSpecific>::class.java)
                val unmarshaller = context.createUnmarshaller()
                return unmarshaller.unmarshal(fileStream) as Array<BitSpecific>
            }
        }

        @JvmStatic
        fun writeBitArributeArray_xml(input: Array<BitSpecific>, filename: String) {
            FileOutputStream(filename).use { fileStream ->
                val context = JAXBContext.newInstance(input.javaClass)
                val marshaller = context.createMarshaller()
                marshaller.marshal(input, fileStream)
            }
        }

        @JvmStatic
        fun getBitAttributeArray(binary: Array<BitSpecific>): Array<BitAttribute> {
            val bitAttributeArray = arrayOfNulls<BitAttribute>(128)

            for (i in binary.indices) {
                val index = binary[i].bitNumber.toInt().absoluteValue -1
                bitAttributeArray[index] = BitAttribute().apply {
                    lengthAttribute = binary[i].bitLength
                    typeAtribute = binary[i].bitType
                    maxLength = binary[i].maxLength
                    additionalOption = binary[i].addtionalOption
                }
            }

            for (i in 0 until 128) {
                if (bitAttributeArray[i] == null) {
                    bitAttributeArray[i] = BitAttribute().apply {
                        typeAtribute = BitType.NOT_SPECIFIC
                    }
                }
            }

            return bitAttributeArray.requireNoNulls()
        }

        @JvmStatic
        fun getTemplate_Standard(): Array<BitSpecific> {
            return arrayOf(
                BitSpecific(1.toByte(), BitLength.FIXED, BitType.BINARY, 8),
                BitSpecific(2.toByte(), BitLength.LLVAR, BitType.BCD, 20),
                BitSpecific(3.toByte(), BitLength.FIXED, BitType.BCD, 6),
                BitSpecific(4.toByte(), BitLength.FIXED, BitType.BCD, 12),
                BitSpecific(5.toByte(), BitLength.FIXED, BitType.BCD, 12),
                BitSpecific(6.toByte(), BitLength.FIXED, BitType.BCD, 12),
                BitSpecific(7.toByte(), BitLength.FIXED, BitType.BCD, 10),
                BitSpecific(8.toByte(), BitLength.FIXED, BitType.BCD, 8),
                BitSpecific(9.toByte(), BitLength.FIXED, BitType.BCD, 8),
                BitSpecific(10.toByte(), BitLength.FIXED, BitType.BCD, 8),
                BitSpecific(11.toByte(), BitLength.FIXED, BitType.BCD, 6),
                BitSpecific(12.toByte(), BitLength.FIXED, BitType.BCD, 12),
                BitSpecific(13.toByte(), BitLength.FIXED, BitType.BCD, 4),
                BitSpecific(14.toByte(), BitLength.FIXED, BitType.BCD, 4),
                BitSpecific(15.toByte(), BitLength.FIXED, BitType.BCD, 4),
                BitSpecific(16.toByte(), BitLength.FIXED, BitType.BCD, 4),
                BitSpecific(17.toByte(), BitLength.FIXED, BitType.BCD, 4),
                BitSpecific(18.toByte(), BitLength.FIXED, BitType.BCD, 4),
                BitSpecific(19.toByte(), BitLength.FIXED, BitType.BCD, 3),
                BitSpecific(20.toByte(), BitLength.FIXED, BitType.BCD, 3),
                BitSpecific(21.toByte(), BitLength.FIXED, BitType.BCD, 3),
                BitSpecific(22.toByte(), BitLength.FIXED, BitType.BCD, 3),
                BitSpecific(23.toByte(), BitLength.FIXED, BitType.BCD, 3),
                BitSpecific(24.toByte(), BitLength.FIXED, BitType.BCD, 3),
                BitSpecific(25.toByte(), BitLength.FIXED, BitType.BCD, 2),
                BitSpecific(26.toByte(), BitLength.FIXED, BitType.BCD, 2),
                BitSpecific(27.toByte(), BitLength.FIXED, BitType.BCD, 1),
                BitSpecific(28.toByte(), BitLength.FIXED, BitType.BCD, 8),
                BitSpecific(29.toByte(), BitLength.FIXED, BitType.BCD, 8),
                BitSpecific(30.toByte(), BitLength.FIXED, BitType.BCD, 8),
                BitSpecific(31.toByte(), BitLength.FIXED, BitType.BCD, 8),
                BitSpecific(32.toByte(), BitLength.LLVAR, BitType.BCD, 11),
                BitSpecific(33.toByte(), BitLength.LLVAR, BitType.BCD, 11),
                BitSpecific(34.toByte(), BitLength.LLVAR, BitType.BCD, 28),
                BitSpecific(35.toByte(), BitLength.LLVAR, BitType.BCD, 37),
                BitSpecific(36.toByte(), BitLength.LLLVAR, BitType.BCD, 104),
                BitSpecific(37.toByte(), BitLength.FIXED, BitType.ANS, 12),
                BitSpecific(38.toByte(), BitLength.FIXED, BitType.AN, 6),
                BitSpecific(39.toByte(), BitLength.FIXED, BitType.AN, 2),
                BitSpecific(40.toByte(), BitLength.LLLVAR, BitType.BINARY, 0),
                BitSpecific(41.toByte(), BitLength.FIXED, BitType.AN, 8),
                BitSpecific(42.toByte(), BitLength.FIXED, BitType.AN, 15),
                BitSpecific(43.toByte(), BitLength.FIXED, BitType.AN, 40),
                BitSpecific(44.toByte(), BitLength.LLVAR, BitType.AN, 0),
                BitSpecific(45.toByte(), BitLength.LLVAR, BitType.AN, 76),
                BitSpecific(46.toByte(), BitLength.LLLVAR, BitType.AN, Byte.MAX_VALUE.toInt()),
                BitSpecific(47.toByte(), BitLength.LLLVAR, BitType.AN, 0),
                BitSpecific(48.toByte(), BitLength.LLLVAR, BitType.AN, 0),
                BitSpecific(49.toByte(), BitLength.FIXED, BitType.ANS, 3),
                BitSpecific(52.toByte(), BitLength.FIXED, BitType.BINARY, 8),
                BitSpecific(53.toByte(), BitLength.FIXED, BitType.BCD, 16),
                BitSpecific(54.toByte(), BitLength.LLLVAR, BitType.AN, 0),
                BitSpecific(55.toByte(), BitLength.LLLVAR, BitType.AN, Byte.MAX_VALUE.toInt()),
                BitSpecific(56.toByte(), BitLength.LLLVAR, BitType.AN, Byte.MAX_VALUE.toInt()),
                BitSpecific(57.toByte(), BitLength.LLLVAR, BitType.ANS, 0),
                BitSpecific(60.toByte(), BitLength.LLLVAR, BitType.ANS, 0),
                BitSpecific(61.toByte(), BitLength.LLLVAR, BitType.ANS, 0),
                BitSpecific(62.toByte(), BitLength.LLLVAR, BitType.ANS, 0),
                BitSpecific(63.toByte(), BitLength.LLLVAR, BitType.ANS, 0),
                BitSpecific(64.toByte(), BitLength.FIXED, BitType.BINARY, 8),
                BitSpecific(70.toByte(), BitLength.FIXED, BitType.BCD, 3),
                BitSpecific(90.toByte(), BitLength.FIXED, BitType.BCD, 42),
                BitSpecific(102.toByte(), BitLength.LLVAR, BitType.ANS, 28),
                BitSpecific(103.toByte(), BitLength.LLVAR, BitType.ANS, 28),
                BitSpecific(105.toByte(), BitLength.LLLVAR, BitType.ANS, 999),
                BitSpecific(120.toByte(), BitLength.LLLVAR, BitType.ANS, 999),
                BitSpecific(128.toByte(), BitLength.FIXED, BitType.BINARY, 8)
            )
        }

        @JvmStatic
        fun getBitAttributeArray(filename: String): Array<BitAttribute> {
            return getBitAttributeArray(getBINARYpecificArray(filename))
        }

        @JvmStatic
        fun getGeneralTemplate(): Array<BitAttribute> {
            return getBitAttributeArray(getTemplate_Standard())
        }

        @JvmStatic
        fun getSmartlinkTemplate(): Array<BitSpecific> {
            return arrayOf(
                BitSpecific(1.toByte(), BitLength.FIXED, BitType.BINARY, 8),
                BitSpecific(2.toByte(), BitLength.LLVAR, BitType.ANS, 20),
                BitSpecific(3.toByte(), BitLength.FIXED, BitType.ANS, 6),
                BitSpecific(4.toByte(), BitLength.FIXED, BitType.ANS, 12),
                BitSpecific(5.toByte(), BitLength.FIXED, BitType.ANS, 12),
                BitSpecific(6.toByte(), BitLength.FIXED, BitType.ANS, 12),
                BitSpecific(7.toByte(), BitLength.FIXED, BitType.ANS, 10),
                BitSpecific(8.toByte(), BitLength.FIXED, BitType.ANS, 8),
                BitSpecific(9.toByte(), BitLength.FIXED, BitType.ANS, 8),
                BitSpecific(10.toByte(), BitLength.FIXED, BitType.ANS, 8),
                BitSpecific(11.toByte(), BitLength.FIXED, BitType.ANS, 6),
                BitSpecific(12.toByte(), BitLength.FIXED, BitType.ANS, 12),
                BitSpecific(13.toByte(), BitLength.FIXED, BitType.ANS, 4),
                BitSpecific(14.toByte(), BitLength.FIXED, BitType.ANS, 4),
                BitSpecific(15.toByte(), BitLength.FIXED, BitType.ANS, 4),
                BitSpecific(16.toByte(), BitLength.FIXED, BitType.ANS, 4),
                BitSpecific(17.toByte(), BitLength.FIXED, BitType.ANS, 4),
                BitSpecific(18.toByte(), BitLength.FIXED, BitType.ANS, 4),
                BitSpecific(19.toByte(), BitLength.FIXED, BitType.ANS, 3),
                BitSpecific(20.toByte(), BitLength.FIXED, BitType.ANS, 3),
                BitSpecific(21.toByte(), BitLength.FIXED, BitType.ANS, 3),
                BitSpecific(22.toByte(), BitLength.FIXED, BitType.ANS, 4),
                BitSpecific(23.toByte(), BitLength.FIXED, BitType.ANS, 3),
                BitSpecific(24.toByte(), BitLength.FIXED, BitType.ANS, 3),
                BitSpecific(25.toByte(), BitLength.FIXED, BitType.ANS, 2),
                BitSpecific(26.toByte(), BitLength.FIXED, BitType.ANS, 2),
                BitSpecific(27.toByte(), BitLength.FIXED, BitType.ANS, 1),
                BitSpecific(28.toByte(), BitLength.FIXED, BitType.ANS, 8),
                BitSpecific(29.toByte(), BitLength.FIXED, BitType.ANS, 8),
                BitSpecific(30.toByte(), BitLength.FIXED, BitType.ANS, 8),
                BitSpecific(31.toByte(), BitLength.FIXED, BitType.ANS, 8),
                BitSpecific(32.toByte(), BitLength.LLVAR, BitType.ANS, 11),
                BitSpecific(33.toByte(), BitLength.LLVAR, BitType.ANS, 11),
                BitSpecific(34.toByte(), BitLength.LLVAR, BitType.ANS, 28),
                BitSpecific(35.toByte(), BitLength.LLVAR, BitType.ANS, 37),
                BitSpecific(36.toByte(), BitLength.LLLVAR, BitType.ANS, 104),
                BitSpecific(37.toByte(), BitLength.FIXED, BitType.AN, 12),
                BitSpecific(38.toByte(), BitLength.FIXED, BitType.AN, 6),
                BitSpecific(39.toByte(), BitLength.FIXED, BitType.AN, 2),
                BitSpecific(40.toByte(), BitLength.LLLVAR, BitType.BINARY, 0),
                BitSpecific(41.toByte(), BitLength.FIXED, BitType.AN, 8),
                BitSpecific(42.toByte(), BitLength.FIXED, BitType.AN, 15),
                BitSpecific(43.toByte(), BitLength.FIXED, BitType.AN, 40),
                BitSpecific(44.toByte(), BitLength.LLVAR, BitType.AN, 0),
                BitSpecific(45.toByte(), BitLength.LLVAR, BitType.AN, 76),
                BitSpecific(46.toByte(), BitLength.LLLVAR, BitType.AN, Byte.MAX_VALUE.toInt()),
                BitSpecific(47.toByte(), BitLength.LLLVAR, BitType.AN, 0),
                BitSpecific(48.toByte(), BitLength.LLLVAR, BitType.AN, 0),
                BitSpecific(49.toByte(), BitLength.FIXED, BitType.ANS, 3),
                BitSpecific(52.toByte(), BitLength.FIXED, BitType.BINARY, 8),
                BitSpecific(53.toByte(), BitLength.FIXED, BitType.ANS, 16),
                BitSpecific(54.toByte(), BitLength.LLLVAR, BitType.AN, 0),
                BitSpecific(55.toByte(), BitLength.LLLVAR, BitType.ANS, Byte.MAX_VALUE.toInt()),
                BitSpecific(56.toByte(), BitLength.LLLVAR, BitType.ANS, Byte.MAX_VALUE.toInt()),
                BitSpecific(57.toByte(), BitLength.LLLVAR, BitType.ANS, 0),
                BitSpecific(60.toByte(), BitLength.LLLVAR, BitType.ANS, 0),
                BitSpecific(61.toByte(), BitLength.LLLVAR, BitType.ANS, 0),
                BitSpecific(62.toByte(), BitLength.LLLVAR, BitType.ANS, 0),
                BitSpecific(63.toByte(), BitLength.LLLVAR, BitType.ANS, 0),
                BitSpecific(64.toByte(), BitLength.FIXED, BitType.BINARY, 8),
                BitSpecific(70.toByte(), BitLength.FIXED, BitType.ANS, 3),
                BitSpecific(90.toByte(), BitLength.FIXED, BitType.ANS, 42),
                BitSpecific(102.toByte(), BitLength.LLVAR, BitType.ANS, 28),
                BitSpecific(103.toByte(), BitLength.LLVAR, BitType.ANS, 28),
                BitSpecific(105.toByte(), BitLength.LLLVAR, BitType.ANS, 999),
                BitSpecific(120.toByte(), BitLength.LLLVAR, BitType.ANS, 999),
                BitSpecific(128.toByte(), BitLength.FIXED, BitType.BINARY, 8)
            )
        }
    }


}