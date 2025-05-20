package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import kotlinx.serialization.Serializable

/**
 * Kotlin implementation of TPDU (Transport Protocol Data Unit) for ISO 8583 messages
 */
@Serializable
data class TPDU(
    var id: TPDUType = TPDUType.Transactions,

    var rawTPDU: ByteArray = byteArrayOf(
        0x60.toByte(), // 96 decimal
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )
) {


    /**
     * Destination Address getter/setter (bytes 1-2 of TPDU)
     */
    var destAddr: Int
        get() = IsoUtil.bcdToBin(IsoUtil.creatBytesFromArray(rawTPDU, 1, 2))
        set(value) {
            IsoUtil.binToBcd(value, 2).copyInto(rawTPDU, 1)
        }

    /**
     * Origin Address getter/setter (bytes 3-4 of TPDU)
     */
    var origAddr: Int
        get() = IsoUtil.bcdToBin(IsoUtil.creatBytesFromArray(rawTPDU, 3, 2))
        set(value) {
            IsoUtil.binToBcd(value, 2).copyInto(rawTPDU, 3)
        }

    /**
     * Pack TPDU into byte array
     */
    fun pack(): ByteArray = rawTPDU.clone()

    /**
     * Unpack TPDU from byte array
     */
    fun unPack(ar: ByteArray) {
        rawTPDU = ar.clone()
    }

    /**
     * Swap Network International Identifier (NII) bytes
     * Swaps bytes at positions 1-2 with 3-4
     */
    fun swapNII() {
        val temp = ByteArray(2)
        IsoUtil.bytesCopy(temp, rawTPDU, 0, 1, 2)  // Copy 1-2 to temp
        IsoUtil.bytesCopy(rawTPDU, rawTPDU, 1, 3, 2)  // Copy 3-4 to 1-2
        IsoUtil.bytesCopy(rawTPDU, temp, 3, 0, 2)  // Copy temp to 3-4
    }

    /**
     * TPDU Type enumeration
     */
    enum class TPDUType {
        Transactions,
        NMS_TNMS
    }

    companion object {
        /**
         * About Us information
         */
        val aboutUs: String = "Sourabh Kaushik, sk@aicortex.in"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TPDU

        if (id != other.id) return false
        if (!rawTPDU.contentEquals(other.rawTPDU)) return false
        if (destAddr != other.destAddr) return false
        if (origAddr != other.origAddr) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + rawTPDU.contentHashCode()
        result = 31 * result + destAddr
        result = 31 * result + origAddr
        return result
    }
}