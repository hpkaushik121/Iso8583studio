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
import kotlinx.coroutines.Job
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

    // ── FIX: Track the processing coroutine so we can cancel it on close ──
    private var processingJob: Job? = null

    fun processGateway() {
        processingJob = CoroutineScope(Dispatchers.IO).launch {
            val socket = incomingConnection ?: return@launch
            val stream = socket.getInputStream()

            gatewayHandler.writeLog(createLogEntry(type = LogType.INFO, "RECEIVING MESSAGE......."))

            try {
                while (socket.isConnected && !socket.isClosed &&
                    !socket.isInputShutdown && !socket.isOutputShutdown
                ) {
                    val commandBytes: ByteArray = if (tcpLengthHeaderEnabled) {
                        // ── Length-prefixed mode ─────────────────────────────
                        // Read exactly 2-byte big-endian length header, blocking until available.
                        val lenBuf = ByteArray(2)
                        if (!readFully(stream, lenBuf, 2)) break   // EOF while reading header

                        val msgLen = ((lenBuf[0].toInt() and 0xFF) shl 8) or
                                (lenBuf[1].toInt() and 0xFF)

                        if (msgLen <= 0) continue   // zero-length frame — skip silently

                        // Read exactly msgLen bytes, blocking until the full payload arrives.
                        val payload = ByteArray(msgLen)
                        if (!readFully(stream, payload, msgLen)) break  // EOF mid-payload

                        payload

                    } else {
                        // ── No length header ─────────────────────────────────
                        // Block on the first read, then drain anything already
                        // buffered in the socket (handles back-to-back chunks of
                        // a single command arriving in quick succession).
                        val buffer = ByteArray(m_Buffer.size)
                        var total = 0

                        val first = stream.read(buffer, 0, buffer.size)
                        if (first < 0) break    // -1 = remote closed: real EOF, not "no data yet"
                        total = first

                        // Drain bytes that arrived in the same TCP burst
                        while (stream.available() > 0 && total < buffer.size) {
                            val n = stream.read(buffer, total, buffer.size - total)
                            if (n < 0) break
                            total += n
                        }

                        if (total == 0) continue    // nothing useful; keep looping
                        buffer.copyOf(total)
                    }

                    hsmClientListener?.onReceivedFormSource(
                        String(commandBytes, Charsets.ISO_8859_1), this@HsmClient
                    )
                }
            } catch (ex: Exception) {
                // ── Socket closed during read is expected on shutdown — only log unexpected errors ──
                if (incomingConnection?.isClosed != true) {
                    ex.printStackTrace()
                }
            } finally {
                hsmClientListener?.onDisconnected(this@HsmClient)
            }
        }
    }

    /**
     * Reads exactly [needed] bytes from [stream] into [buf], blocking until all
     * bytes are available. Returns false if the stream reaches EOF before [needed]
     * bytes have been read (i.e. the remote side closed the connection mid-message).
     */
    private fun readFully(stream: java.io.InputStream, buf: ByteArray, needed: Int): Boolean {
        var offset = 0
        while (offset < needed) {
            val n = stream.read(buf, offset, needed - offset)
            if (n < 0) return false   // EOF — connection closed
            offset += n
        }
        return true
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

    /**
     * ── FIX: Forcefully close this client's socket and cancel its processing coroutine.
     * Closing the socket causes any blocking read() to throw SocketException,
     * which breaks the processing loop and triggers the finally → onDisconnected callback.
     */
    fun close() {
        try {
            processingJob?.cancel()
            processingJob = null
            incomingConnection?.close()
            incomingConnection = null
        } catch (_: Exception) {
            // Ignore errors during forced shutdown
        }
    }
}