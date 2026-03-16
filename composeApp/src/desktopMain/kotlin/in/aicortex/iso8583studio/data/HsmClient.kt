package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.VerificationError
import `in`.aicortex.iso8583studio.data.model.VerificationException
import `in`.aicortex.iso8583studio.domain.service.hostSimulatorService.HostSimulator
import `in`.aicortex.iso8583studio.domain.service.hostSimulatorService.Simulator
import `in`.aicortex.iso8583studio.hsm.HsmClientListener
import `in`.aicortex.iso8583studio.hsm.HsmSimulator
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.createLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Socket
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class HsmClient(var gatewayHandler: Simulator) {
    var incomingConnection: Socket? = null
    var remoteIPAddress: String? = null
    var timeOut: Int = 30
    val clientID: String = UUID.randomUUID().toString()
    var hsmClientListener: HsmClientListener? = null
    var tcpLengthHeaderEnabled: Boolean = false
    private var m_Buffer: ByteArray = ByteArray(10048)

    fun processGateway() {
        CoroutineScope(Dispatchers.IO).launch {
            incomingConnection ?: return@launch

            val now = LocalDateTime.now()

            gatewayHandler.writeLog(createLogEntry(type = LogType.INFO, "RECEIVING MESSAGE......."))
            while (incomingConnection?.isConnected == true && incomingConnection?.isClosed == false && incomingConnection?.isInputShutdown == false
                && incomingConnection?.isOutputShutdown == false
            ) {
                var length = 0

                val input: ByteArray = try {
                    val buffer = ByteArray(m_Buffer.size)
                    var bytesRead: Int

                    do {
                        if (length == buffer.size) {
                            throw VerificationException(
                                "Length exceeds buffer size",
                                VerificationError.OTHERS
                            )
                        }

                        bytesRead =
                            incomingConnection!!.getInputStream()
                                .read(buffer, length, buffer.size - length)

                        if (bytesRead == 0) {
                            throw VerificationException(
                                "CONNECTION WAS CLOSED BY REMOTE COMPUTER/TERMINAL",
                                VerificationError.DISCONNECTED_FROM_SOURCE
                            )
                        }

                        length += bytesRead
                    } while (incomingConnection!!.getInputStream().available() > 0)

                    val result = ByteArray(length)
                    buffer.copyInto(result, 0, 0, length)
                    result

                } catch (ex: Exception) {
                    hsmClientListener?.onDisconnected(this@HsmClient)
                    throw VerificationException(
                        "CONNECTION MAY BE CLOSED BY REMOTE COMPUTER/TERMINAL ",
                        VerificationError.DISCONNECTED_FROM_SOURCE
                    )

                }

                val commandBytes = if (tcpLengthHeaderEnabled && input.size >= 2) {
                    input.copyOfRange(2, input.size)
                } else {
                    input
                }
                hsmClientListener?.onReceivedFormSource(String(commandBytes, Charsets.ISO_8859_1), this@HsmClient)
            }


        }
    }

    suspend fun send(response: String?) {
        try {
            response?.let { resp ->
                val respBytes = resp.toByteArray(Charsets.ISO_8859_1)
                val output = if (tcpLengthHeaderEnabled) {
                    val len = respBytes.size
                    byteArrayOf(((len shr 8) and 0xFF).toByte(), (len and 0xFF).toByte()) + respBytes
                } else {
                    respBytes
                }
                incomingConnection!!.getOutputStream().write(output)
            }
            hsmClientListener?.onSentToSource(response)
        } catch (ex: Exception) {
            ex.printStackTrace()
            throw VerificationException(
                "CONNECTION MAY BE CLOSED BY REMOTE COMPUTER/TERMINAL ",
                VerificationError.DISCONNECTED_FROM_SOURCE
            )
        }
    }
}