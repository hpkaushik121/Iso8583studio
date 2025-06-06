package `in`.aicortex.iso8583studio.data


import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import `in`.aicortex.iso8583studio.data.model.VerificationError
import `in`.aicortex.iso8583studio.data.model.VerificationException
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.createLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.logging.Log
import java.net.Socket
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * PermanentConnection class for maintaining a persistent connection to a host
 */
class PermanentConnection(
    private val gatewayHandler: GatewayServiceImpl,
    private val hostIp: String,
    internal val port: Int,
    private val nii: Int
) {
    internal var isConnected = false
    private var socket: Socket? = null
    private var currentNii = 0
    private var connectionNumber = 0
    private var isRunning = false
    private val coroutineScope = IsoCoroutine(gatewayHandler)
    private val connectionMutex = Mutex()

    private val listMessagesReceived = ConcurrentHashMap<Int, MessageForSourceNii>()
    private val buffer = ByteArray(10000)

    val hostNii: Int
        get() = nii

    val isRunningState: Boolean
        get() = isRunning

    private val logPrefix: String
        get() = "[P$nii-$connectionNumber]"

//    constructor(nii: Int, ip: String, port: Int) : this(GatewayServiceImpl(), ip, port, nii)

    fun start() {
        isRunning = true
        coroutineScope.launchSafely {
            processPermanentConnection()
        }
    }

    fun stop() {
        isRunning = false
    }

    private suspend fun processPermanentConnection() {
        var attempts = 0

        while (isRunning && isActive) {
            if (!isConnected) {
                try {
                    attempts++
                    withContext(Dispatchers.IO) {
                        socket = Socket(hostIp, port)
                    }
                    connectionNumber++
                    gatewayHandler.writeLog(createLogEntry(
                        type = LogType.CONNECTION,
                        message = "$logPrefix ESTABLISHED CONNECTION TO $hostIp $port"
                    ))
                    isConnected = true

                    // Start receiving data from the socket
                    coroutineScope.launchSafely {
                        receiveFromDestination()
                    }
                } catch (e: Exception) {
                    if (attempts == 1) {
                        gatewayHandler.writeLog(createLogEntry(
                            type = LogType.CONNECTION,
                            message = "$logPrefix CANNOT CONNECT TO $hostIp $port\r\n${e.message}"
                        ))
                    }
                    if (attempts % 500 == 0) {
                        gatewayHandler.writeLog(
                            createLogEntry(
                                type = LogType.CONNECTION,
                                message = "$logPrefix TRIED $attempts TIMES TO CONNECT BUT FAILED \r\n${e.message}"
                            )
                        )
                    }
                    delay(1000) // Wait before retrying
                }
            }
            delay(100) // Small delay in the connection check loop
        }

        // Cleanup when stopping
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            // Ignore close errors
        }
    }

    private suspend fun receiveFromDestination() {
        try {
            val inputStream = socket?.getInputStream() ?: return

            while (isConnected && isRunning && isActive) {
                try {
                    val len = withContext(Dispatchers.IO) {
                        inputStream.read(buffer)
                    }

                    if (len > 10) {
                        val key = IsoUtil.messageLengthToInt(
                            IsoUtil.getBytesFromBytes(buffer, 3, 2),
                            MessageLengthType.BCD
                        )

                        if (buffer[2] == 0x60.toByte() && listMessagesReceived.containsKey(key)) {
                            listMessagesReceived[key]?.let { messageForNii ->
                                messageForNii.receiveTime = LocalDateTime.now()
                                messageForNii.message = IsoUtil.getBytesFromBytes(buffer, 0, len)
                            }
                        }
                    }

                    if (len <= 0) {
                        throw Exception("Connection closed by remote host")
                    }

                } catch (e: Exception) {
                    throw e
                }
            }
        } catch (e: Exception) {
            try {
                gatewayHandler.writeLog(createLogEntry(
                    type = LogType.ERROR,
                    message = "$logPrefix CONNECTION IS DROPPED: ${e.message}"
                )
                )
                gatewayHandler.writeLog(createLogEntry(
                    type = LogType.DEBUG,
                    message = "$logPrefix TRYING TO RECONNECT"
                )
                )
            } catch (e: Exception) {
                // Ignore logging errors
            } finally {
                isConnected = false
                try {
                    socket?.close()
                    socket = null
                } catch (e: Exception) {
                    // Ignore close errors
                }
            }
        }
    }

    suspend fun registerSourceNii(): Int {
        return connectionMutex.withLock {
            currentNii++
            if (currentNii == 1000) {
                currentNii = 1
            }

            if (listMessagesReceived.containsKey(currentNii)) {
                listMessagesReceived.remove(currentNii)
            }

            listMessagesReceived[currentNii] = MessageForSourceNii()
            currentNii
        }
    }

    suspend fun removeSourceNii(sourceNii: Int) {
        connectionMutex.withLock {
            if (listMessagesReceived.containsKey(sourceNii)) {
                listMessagesReceived.remove(sourceNii)
            }
        }
    }

    suspend fun receive(sourceNii: Int, timeout: Int): ByteArray {
        val startTime = LocalDateTime.now()

        while (listMessagesReceived.containsKey(sourceNii)) {
            val messageForNii = listMessagesReceived[sourceNii] ?: break

            if (messageForNii.receiveTime != null) {
                messageForNii.receiveTime = null
                IsoUtil.bytesCopy(messageForNii.message, messageForNii.originalTpdu, 3, 3, 2)
                IsoUtil.bytesCopy(messageForNii.message, messageForNii.originalTpdu, 5, 1, 2)
                return messageForNii.message
            }

            val elapsedSeconds = Duration.between(startTime, LocalDateTime.now()).seconds
            if (elapsedSeconds > timeout) {
                throw VerificationException("NO RESPONSE", VerificationError.TIMEOUT)
            }

            delay(50) // Small delay while waiting for response
        }

        throw VerificationException("Source NII is wrong", VerificationError.INVALID_NII)
    }

    suspend fun send(sourceNii: Int, dataSent: ByteArray) {
        if (!isRunning || !isConnected) {
            throw VerificationException(
                "CONNECTION TO HOST IS NOT AVAILABLE",
                VerificationError.SOCKET_ERROR
            )
        }

        connectionMutex.withLock {
            val messageForNii = listMessagesReceived[sourceNii] ?: throw VerificationException(
                "Source NII not registered",
                VerificationError.INVALID_NII
            )

            messageForNii.originalTpdu = IsoUtil.getBytesFromBytes(dataSent, 2, 5)

            // Copy source NII to the appropriate position in the data
            IsoUtil.intToMessageLength(sourceNii, MessageLengthType.BCD).copyInto(dataSent, 5)

            // Send the data
            withContext(Dispatchers.IO) {
                try {
                    socket?.getOutputStream()?.write(dataSent)
                    socket?.getOutputStream()?.flush()
                } catch (e: Exception) {
                    isConnected = false
                    throw VerificationException(
                        "ERROR SENDING DATA: ${e.message}",
                        VerificationError.SOCKET_ERROR
                    )
                }
            }
        }
    }

    // Helper class to store message data for a specific source NII
    class MessageForSourceNii {
        var receiveTime: LocalDateTime? = null
        var message: ByteArray = ByteArray(0)
        var originalTpdu: ByteArray = ByteArray(5)
    }
}