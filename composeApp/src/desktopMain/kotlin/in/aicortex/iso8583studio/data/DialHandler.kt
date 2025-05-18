package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.EDialupStatus
import `in`.aicortex.iso8583studio.data.model.MessageLengthType

internal class DialHandler(comPort: String, baudRate: Int) : RS232Handler(comPort, baudRate) {
    private var m_PhoneNumber: String = ""
    private var m_CurrentStatus: EDialupStatus = EDialupStatus.Nothing

    val currentStatus: EDialupStatus
        get() = m_CurrentStatus

    var phoneNumber: String
        get() = m_PhoneNumber
        set(value) { m_PhoneNumber = value }

    fun sendAT(command: String): String {
        writeString("$command\r\n")
        receiveString() // Discard first response
        return receiveString()
    }

    fun sendAT(command: String, expectedRe: String): String {
        writeString("$command\r\n")
        var response: String
        do {
            response = receiveString()
        } while (!response.contains(expectedRe) && !response.contains("NO"))

        return response
    }

    fun makeCall(): Boolean {
        m_CurrentStatus = EDialupStatus.Dialing
        val isConnected = sendAT("ATDT$m_PhoneNumber").uppercase().trim() == "CONNECT"
        m_CurrentStatus = EDialupStatus.Connected
        return isConnected
    }

    fun receiveCall(): Boolean {
        try {
            sendAT("AT")
            m_CurrentStatus = EDialupStatus.WaitForCall

            if (receiveString() != "RING" || !sendAT("ATA").contains("CONNECT")) {
                return false
            }

            m_CurrentStatus = EDialupStatus.Connected
            return true
        } catch (e: Exception) {
            // Silent catch
            return false
        }
    }

    override fun receiveMessage(): ByteArray? {
        val message = super.receiveMessage()
        if (message == null) {
            m_CurrentStatus = EDialupStatus.Nothing
        }
        return message
    }

    // Helper method for writing strings - not in original code but needed for Kotlin implementation
    private fun writeString(data: String) {
        val bytes = data.toByteArray()
        sendMessage(bytes, MessageLengthType.NONE)
    }
}