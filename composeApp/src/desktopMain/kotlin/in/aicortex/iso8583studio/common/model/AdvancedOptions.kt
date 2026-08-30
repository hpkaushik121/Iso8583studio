package `in`.aicortex.iso8583studio.data.model

import `in`.aicortex.iso8583studio.data.NCCParameter
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.bcdToString
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.creatBytesFromArray
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.kvc
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.stringToBCD
import kotlinx.serialization.Serializable

/**
 * Advanced gateway options
 */
@Serializable
class AdvancedOptions{
    private var m_TimeOutFromSource: Int = 30
    private var m_TimeOutFromDest: Int = 30
    private var m_ActionWhenDisconnect: ActionWhenDisconnect = ActionWhenDisconnect.DisconnectFromBoth
    private var m_PermanentConnectionToHost: String = ""
    private var m_IPsDenied: String = ""
    private var m_OnlyAllowIPs: String = ""
    private var m_LogTransmissionToDatabase: Boolean = true
    private var m_SpecialFearure: SpecialFeature = SpecialFeature.None // Assuming default value
    private var m_ParsingFeature: ParsingFeature = ParsingFeature.NONE // Assuming default value
    private var m_SecretKey: String = ""
    private var m_NCCParameter1: NCCParameter = NCCParameter("")
    private var m_NCCParameter2: NCCParameter = NCCParameter("")
    private var m_NCCParameter3: NCCParameter = NCCParameter("")
    private var m_NCCParameter4: NCCParameter = NCCParameter("")
    private var m_NCCParameter5: NCCParameter = NCCParameter("")
    private var m_NCCParameter6: NCCParameter = NCCParameter("")
    private var m_NCCParameter7: NCCParameter = NCCParameter("")
    private var m_NCCParameter8: NCCParameter = NCCParameter("")
    private var m_NCCParameter9: NCCParameter = NCCParameter("")
    private var m_EMVShowOption: EMVShowOption = EMVShowOption.None // Assuming default value
    private var m_AcceptVersion: String = ""
    private var m_SslIncomming: Boolean = false
    var m_SslServerCertificatePath: String = ""
    private var m_SslOutgoing: Boolean = false
    private var m_SslCheckRemoteCertificate: Boolean = false
    var m_SslClientCertificatePath: String = ""
    private var m_ObscuredBitsInISO8583: String? = null
    private var m_ObscureType: ObscureType = ObscureType.None // Assuming default value

    var timeOutFromSource: Int
        get() = m_TimeOutFromSource
        set(value) { m_TimeOutFromSource = value }

    var timeOutFromDest: Int
        get() = m_TimeOutFromDest
        set(value) { m_TimeOutFromDest = value }

    var actionWhenDisconnect: ActionWhenDisconnect
        get() = m_ActionWhenDisconnect
        set(value) { m_ActionWhenDisconnect = value }

    var permanentConnectionToHost: String
        get() {
            if (m_PermanentConnectionToHost.isEmpty()) {
                return "Not set.Format: Host NII1,Host NII2, Host NII3"
            }
            return m_PermanentConnectionToHost
        }
        set(value) {
            val str = value
            for (s in str.split(',')) {
                val result = s.toIntOrNull()
                if (result == null || result < 1 || result > 999) {
                    m_PermanentConnectionToHost = ""
                    return
                }
            }
            m_PermanentConnectionToHost = value
        }

    fun getPermanentConnectionHost(): List<Int> {
        val permanentConnectionHost = mutableListOf<Int>()
        val connectionToHost = m_PermanentConnectionToHost

        for (s in connectionToHost.split(',')) {
            val result = s.toIntOrNull()
            if (result != null && result >= 1 && result <= 999) {
                permanentConnectionHost.add(result)
            }
        }

        return permanentConnectionHost
    }

    var iPsDenied: String
        get() = m_IPsDenied
        set(value) { m_IPsDenied = value }

    var onlyAllowIps: String
        get() = m_OnlyAllowIPs
        set(value) { m_OnlyAllowIPs = value }

    var logTransmissionToDatabase: Boolean
        get() = m_LogTransmissionToDatabase
        set(value) { m_LogTransmissionToDatabase = value }

    var specialFeature: SpecialFeature
        get() = m_SpecialFearure
        set(value) { m_SpecialFearure = value }

    var parsingFeature: ParsingFeature
        get() = m_ParsingFeature
        set(value) { m_ParsingFeature = value }

    fun getSecretKey(): ByteArray {
        return try {
            stringToBCD(m_SecretKey, m_SecretKey.length / 2)
        } catch (e: Exception) {
            byteArrayOf(1, 1, 1, 1, 1, 1, 1, 1)
        }
    }

    var secretKey: String
        get() {
            try {
                if (m_SecretKey.length == 16) {
                    return bcdToString(
                        creatBytesFromArray(
                            kvc(stringToBCD(m_SecretKey, 8), CipherType.DES), 0, 3
                        )
                    )
                } else if (m_SecretKey.length == 32) {
                    return bcdToString(
                        creatBytesFromArray(
                            kvc(stringToBCD(m_SecretKey, 16), CipherType.TRIPLE_DES), 0, 3
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore exception
            }
            return "NONE"
        }
        set(value) {
            try {
                m_SecretKey = value
            } catch (e: Exception) {
                m_SecretKey = ""
            }
        }

    var nccParameter1: String
        get() = m_NCCParameter1.toString()
        set(value) { m_NCCParameter1 = NCCParameter(value) }

    var nccParameter2: String
        get() = m_NCCParameter2.toString()
        set(value) { m_NCCParameter2 = NCCParameter(value) }

    var nccParameter3: String
        get() = m_NCCParameter3.toString()
        set(value) { m_NCCParameter3 = NCCParameter(value) }

    var nccParameter4: String
        get() = m_NCCParameter4.toString()
        set(value) { m_NCCParameter4 = NCCParameter(value) }

    var nccParameter5: String
        get() = m_NCCParameter5.toString()
        set(value) { m_NCCParameter5 = NCCParameter(value) }

    var nccParameter6: String
        get() = m_NCCParameter6.toString()
        set(value) { m_NCCParameter6 = NCCParameter(value) }

    var nccParameter7: String
        get() = m_NCCParameter7.toString()
        set(value) { m_NCCParameter7 = NCCParameter(value) }

    var nccParameter8: String
        get() = m_NCCParameter8.toString()
        set(value) { m_NCCParameter8 = NCCParameter(value) }

    var nccParameter9: String
        get() = m_NCCParameter9.toString()
        set(value) { m_NCCParameter9 = NCCParameter(value) }

    fun getNCCParameterList(): Array<NCCParameter>? {
        val nccParameterArray = arrayOfNulls<NCCParameter>(10)
        var length = 0

        if (m_NCCParameter1.nii > 0) {
            nccParameterArray[length] = NCCParameter(m_NCCParameter1.toString())
            length++
        }
        if (m_NCCParameter2.nii > 0) {
            nccParameterArray[length] = NCCParameter(m_NCCParameter2.toString())
            length++
        }
        if (m_NCCParameter3.nii > 0) {
            nccParameterArray[length] = NCCParameter(m_NCCParameter3.toString())
            length++
        }
        if (m_NCCParameter4.nii > 0) {
            nccParameterArray[length] = NCCParameter(m_NCCParameter4.toString())
            length++
        }
        if (m_NCCParameter5.nii > 0) {
            nccParameterArray[length] = NCCParameter(m_NCCParameter5.toString())
            length++
        }
        if (m_NCCParameter6.nii > 0) {
            nccParameterArray[length] = NCCParameter(m_NCCParameter6.toString())
            length++
        }
        if (m_NCCParameter7.nii > 0) {
            nccParameterArray[length] = NCCParameter(m_NCCParameter7.toString())
            length++
        }
        if (m_NCCParameter8.nii > 0) {
            nccParameterArray[length] = NCCParameter(m_NCCParameter8.toString())
            length++
        }
        if (m_NCCParameter9.nii > 0) {
            nccParameterArray[length] = NCCParameter(m_NCCParameter9.toString())
            length++
        }

        if (length == 0) {
            return null
        }

        return Array(length) { i -> nccParameterArray[i]!! }
    }

    fun getEMVShowOption(): EMVShowOption = m_EMVShowOption

    var emvShowOption: String
        get() = "${m_EMVShowOption.ordinal} ${m_EMVShowOption.name}"
        set(value) {
            try {
                var s = value
                if (value.length == 1) {
                    s = value + " "
                }
                m_EMVShowOption = EMVShowOption.values()[s.trim().toInt()]
            } catch (e: Exception) {
                m_EMVShowOption = EMVShowOption.None
            }
        }

    var acceptVersion: String
        set(value) { m_AcceptVersion = value }
        get() = m_AcceptVersion

    var sslServer: Boolean
        set(value) { m_SslIncomming = value }
        get() = m_SslIncomming

    var sslServerCertificatePath: String
        set(value) {
            if ("*****" in value) return
            m_SslServerCertificatePath = value
        }
        get() {
            val parts = m_SslServerCertificatePath.split(';')
            return if (parts.size > 1) {
                "${parts[0]};**********"
            } else {
                m_SslServerCertificatePath
            }
        }

    var sslClient: Boolean
        set(value) { m_SslOutgoing = value }
        get() = m_SslOutgoing

    var sslCheckRemoteCertificate: Boolean
        set(value) { m_SslCheckRemoteCertificate = value }
        get() = m_SslCheckRemoteCertificate

    var sslClientCertificatePath: String
        set(value) {
            if ("*****" in value) return
            m_SslClientCertificatePath = value
        }
        get() {
            val parts = m_SslClientCertificatePath.split(';')
            return if (parts.size > 1) {
                "${parts[0]};**********"
            } else {
                m_SslClientCertificatePath
            }
        }

    var obscuredBitsInISO8583: String
        get() {
            if (m_ObscuredBitsInISO8583 != "" && m_ObscuredBitsInISO8583 != null) {
                return m_ObscuredBitsInISO8583!!
            }
            m_ObscuredBitsInISO8583 = ""
            return "Specify ISO8583Bits to be obscured here"
        }
        set(value) {
            try {
                val str = value.replace(" ", "")
                for (s in str.split(';')) {
                    val bitNumber = s.toInt()
                    if (bitNumber < 0 || bitNumber > 128) {
                        throw Exception("")
                    }
                }
                m_ObscuredBitsInISO8583 = value.replace(" ", "")
            } catch (e: Exception) {
                m_ObscuredBitsInISO8583 = ""
            }
        }

    var obscureType: ObscureType
        get() = m_ObscureType
        set(value) { m_ObscureType = value }

    fun isPartialEncryption(): Boolean {
        return obscureType != ObscureType.None && obscuredBitsInISO8583 != ""
    }

    fun getObscuredBits(): IntArray {
        val parts = m_ObscuredBitsInISO8583?.split(';', ',') ?: return IntArray(0)
        return IntArray(parts.size) { i -> parts[i].toInt() }
    }
}