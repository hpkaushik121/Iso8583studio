package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import kotlinx.serialization.Serializable
import java.net.Socket

/**
 * NCCParameter class for Network Control Center parameters
 */
@Serializable
data class NCCParameter(val str: String){
    var nii: Int = 0

    @Transient
    var sourceNii: Int = 0

    var hostAddress: String = ""
    var port: Int = 0

    @Transient
    var connection: Socket? = null

    @Transient
    var pConnection: PermanentConnection? = null

    var niiChanged: Int = 0
    var lengthType: MessageLengthType = MessageLengthType.BCD


    init{
        try {
            val parts = str.split(';')
            nii = if (parts[0].trim() == "*") {
                niiChanged = 9000
                9000
            } else {
                niiChanged = parts[0].trim().toInt()
                parts[0].trim().toInt()
            }

            if (nii < 1 || nii > 999) {
                throw Exception("NII must be 1 - 999")
            }

            hostAddress = parts[1].trim()
            port = parts[2].toInt()
            lengthType = MessageLengthType.fromValue(parts[3].substring(0, 1).toInt())

            if (parts.size > 4) {
                niiChanged = parts[4].toInt()
            }
        } catch (e: Exception) {
            nii = 0
        }
    }

    override fun toString(): String {
        if (nii < 1) {
            return "Not set"
        }

        var str = "${if (nii != 9000) "$nii;" else "*;"}" +
                "$hostAddress;$port;${lengthType.value}_${lengthType.name}"

        if (niiChanged != nii) {
            str = "$str;$niiChanged"
        }

        return str
    }

    suspend fun send(dataSent: ByteArray) {
        pConnection?.send(sourceNii, dataSent)
    }

    suspend fun receive(timeout: Int): ByteArray? {
        return pConnection?.receive(sourceNii, timeout)
    }

    suspend fun registerSourceNii() {
        if (sourceNii != 0) {
            return
        }
        sourceNii = pConnection?.registerSourceNii() ?: 0
    }
}

