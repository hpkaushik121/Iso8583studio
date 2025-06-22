package `in`.aicortex.iso8583studio.data

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent
import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import `in`.aicortex.iso8583studio.data.model.TransmissionType
import `in`.aicortex.iso8583studio.domain.utils.Utils.bytesCopy
import `in`.aicortex.iso8583studio.domain.utils.Utils.convertToTIDIALERRule
import `in`.aicortex.iso8583studio.domain.utils.Utils.messageLengthToInt
import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


open class RS232Handler {
    private val port: SerialPort
    private var m_MessageLengthType: MessageLengthType = MessageLengthType.BCD
    private var _lastError: Exception? = null
    private val buffer: ByteArray = ByteArray(4096)
    private var response: ByteArray? = null
    private var m_ReadMessageTimeOut: Int = 30
    protected var receiveFromHost: Boolean = false
    private var m_STX_ETXRule: Boolean = true
    private var m_PendingTrans: Boolean = false
    private var m_TransmissionType: TransmissionType = TransmissionType.SYNCHRONOUS // Default value
    private var lineRead: String = ""
    private val lock = ReentrantLock()

    var onReceivedMessage: ((ByteArray) -> Unit)? = null

    var transmissionType: TransmissionType
        get() = m_TransmissionType
        set(value) {
            m_TransmissionType = value
        }

    var readMessageTimeOut: Int
        get() = m_ReadMessageTimeOut
        set(value) {
            m_ReadMessageTimeOut = value
        }

    var stxEtxRule: Boolean
        get() = m_STX_ETXRule
        set(value) {
            m_STX_ETXRule = value
        }

    var messageLengthType: MessageLengthType
        get() = m_MessageLengthType
        set(value) {
            m_MessageLengthType = value
        }

    constructor(portName: String) {
        port = SerialPort.getCommPort(portName)
        initialize()
    }

    constructor(portName: String, baudRate: Int) {
        port = SerialPort.getCommPort(portName)
        port.baudRate = baudRate
        initialize()
    }

    // Default constructor
    constructor() {
        // Get first available port or a default one
        val ports = SerialPort.getCommPorts()
        port = if (ports.isNotEmpty()) ports[0] else SerialPort.getCommPort("COM1")
        initialize()
    }

    private fun initialize() {
        // Set default parameters
        port.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY)
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 1000)

        // Add data listener
        port.addDataListener(object : SerialPortDataListener {
            override fun getListeningEvents() = SerialPort.LISTENING_EVENT_DATA_AVAILABLE

            override fun serialEvent(event: SerialPortEvent) {
                if (event.eventType == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    try {
                        handleDataReceived()
                    } catch (ex: Exception) {
                        _lastError = ex
                    }
                }
            }
        })
    }

    fun waitPendingTrans(timeout: Int): Boolean {
        val endTime = LocalDateTime.now().plusSeconds(timeout.toLong())
        while (LocalDateTime.now().isBefore(endTime) && m_PendingTrans) {
            Thread.sleep(100)
        }
        return LocalDateTime.now().isBefore(endTime)
    }

    fun sendMessage(buf: ByteArray, lengthType: MessageLengthType) {
        if (!port.isOpen) {
            port.openPort()
        }

        m_PendingTrans = true
        port.flushIOBuffers()

        if (m_STX_ETXRule) {
            val tidialerRule = convertToTIDIALERRule(buf, lengthType)

            if (port.baudRate < 10000) {
                port.writeBytes(
                    tidialerRule.copyOfRange(1, tidialerRule.size),
                    (tidialerRule.size - 1).toLong()
                )
            } else {
                port.writeBytes(tidialerRule, tidialerRule.size.toLong())
            }
        } else {
            port.writeBytes(buf, buf.size.toLong())
        }
    }

    private fun handleDataReceived() {
        try {
            var messageLength = 0

            if (stxEtxRule) {
                val stxByteArray = ByteArray(1)
                if (port.readBytes(stxByteArray, 1) != 1 || stxByteArray[0].toInt() != 2) { // STX
                    port.flushIOBuffers()
                    return
                }

                // Echo STX
                port.writeBytes(byteArrayOf(2), 1)

                // Read length bytes
                val lengthBytes = ByteArray(2)
                if (port.readBytes(lengthBytes, 2) != 2) {
                    port.flushIOBuffers()
                    return
                }

                messageLength = messageLengthToInt(lengthBytes, messageLengthType)

                // Copy length bytes to buffer
                lengthBytes.copyInto(buffer, 0, 0, 2)

                // Read message content
                val messageContent = ByteArray(messageLength)
                if (port.readBytes(messageContent, messageLength.toLong()) != messageLength) {
                    port.flushIOBuffers()
                    return
                }
                messageContent.copyInto(buffer, 2, 0, messageLength)

                // Read ETX
                val etxByteArray = ByteArray(1)
                if (port.readBytes(etxByteArray, 1) != 1 || etxByteArray[0].toInt() != 3) { // ETX
                    port.flushIOBuffers()
                    return
                }

                port.flushIOBuffers()
            }

            response = ByteArray(messageLength + 2)
            bytesCopy(response!!, buffer, 0, 0, response!!.size)

            onReceivedMessage?.invoke(response!!)
            receiveFromHost = true

        } catch (ex: Exception) {
            _lastError = ex
        }
    }

    fun receiveString(): String {
        val endTime = LocalDateTime.now().plusSeconds(readMessageTimeOut.toLong())
        stxEtxRule = false
        lineRead = ""

        return lock.withLock {
            _lastError = null
            receiveFromHost = false

            while (!receiveFromHost) {
                val now = LocalDateTime.now()
                if (now.isBefore(endTime) && _lastError == null) {
                    if (!port.isOpen) {
                        port.openPort()
                    }
                    Thread.sleep(100)
                } else {
                    break
                }
            }

            m_PendingTrans = false
            val now = LocalDateTime.now()

            if (now.isAfter(endTime) || _lastError != null) "" else lineRead
        }
    }

    open fun receiveMessage(): ByteArray? {
        val endTime = LocalDateTime.now().plusSeconds(readMessageTimeOut.toLong())

        return lock.withLock {
            _lastError = null
            receiveFromHost = false

            while (!receiveFromHost) {
                val now = LocalDateTime.now()
                if (now.isBefore(endTime) && _lastError == null) {
                    if (!port.isOpen) {
                        port.openPort()
                    }
                    Thread.sleep(100)
                } else {
                    break
                }
            }

            m_PendingTrans = false
            val now = LocalDateTime.now()

            if (now.isAfter(endTime) || _lastError != null) {
                null
            } else {
                receiveFromHost = false
                response
            }
        }
    }

    fun close() {
        if (port.isOpen) {
            port.closePort()
        }
    }

    fun open(){
        port.openPort()
    }
    var isOpen = {
        port.isOpen
    }

    // Utility extension methods for jSerialComm
    private fun SerialPort.flushIOBuffers() {
        this.clearDTR()
        this.clearRTS()
        this.closePort()
        this.clearBreak()
    }
}
