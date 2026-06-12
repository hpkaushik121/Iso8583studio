package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport

import com.fazecast.jSerialComm.SerialPort
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.toHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * USB-CDC link to the STM32 firmware acting as a card. Wraps [com.fazecast.jSerialComm.SerialPort]
 * with the framed protocol defined in `firmware/stm32-card`.
 */
class SerialTransport(
    private val portName: String,
    private val baudRate: Int = 115200,
) : CardTransport {

    override val name: String = "serial:$portName"

    private val _trace = MutableSharedFlow<ApduExchange>(
        replay = 0,
        extraBufferCapacity = 64,
    )

    override val trace: Flow<ApduExchange> = _trace.asSharedFlow()

    private val ioMutex = Mutex()
    private val decoder = SerialFraming.Decoder()

    @Volatile
    private var serialPort: SerialPort? = null

    private companion object {
        const val TYPE_APDU_CMD: Byte = 0x01
        const val TYPE_APDU_RSP: Byte = 0x02
        const val TYPE_ATR: Byte = 0x10
        const val TYPE_CARD_EVENT: Byte = 0x20
        const val TYPE_ERROR: Byte = 0xFE.toByte()
        const val TYPE_PING: Byte = 0xFF.toByte()

        const val ATR_TIMEOUT_MS: Long = 2_000L
        const val APDU_TIMEOUT_MS: Long = 5_000L
    }

    override suspend fun connect(): ByteArray = ioMutex.withLock {
        withContext(Dispatchers.IO) {
            if (serialPort?.isOpen == true) {
                error("SerialTransport($portName) is already connected")
            }
            val port = SerialPort.getCommPort(portName)
            port.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY)
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 0)
            if (!port.openPort()) {
                throw IOException("Failed to open serial port: $portName")
            }
            serialPort = port
            try {
                writeFrame(port, SerialFraming.encode(TYPE_PING, ByteArray(0)))
                val atrFrame = awaitFrame(port, ATR_TIMEOUT_MS) { it.type == TYPE_ATR }
                atrFrame.body
            } catch (t: Throwable) {
                runCatching { port.closePort() }
                serialPort = null
                throw t
            }
        }
    }

    override suspend fun transmit(command: CommandApdu): ResponseApdu = ioMutex.withLock {
        val port = serialPort ?: error("SerialTransport($portName) is not connected")
        withContext(Dispatchers.IO) {
            val sentAt = System.currentTimeMillis()
            writeFrame(port, SerialFraming.encode(TYPE_APDU_CMD, command.toBytes()))
            val rspFrame = awaitFrame(port, APDU_TIMEOUT_MS) {
                it.type == TYPE_APDU_RSP || it.type == TYPE_ERROR
            }
            if (rspFrame.type == TYPE_ERROR) {
                throw IOException("Firmware reported error: ${rspFrame.body.toHex()}")
            }
            val response = ResponseApdu.parse(rspFrame.body)
            val receivedAt = System.currentTimeMillis()
            _trace.tryEmit(
                ApduExchange(
                    sentAt = sentAt,
                    receivedAt = receivedAt,
                    command = command,
                    response = response,
                    transportName = name,
                )
            )
            response
        }
    }

    override suspend fun reset(warm: Boolean): ByteArray = ioMutex.withLock {
        val port = serialPort ?: error("SerialTransport($portName) is not connected")
        withContext(Dispatchers.IO) {
            val payload = byteArrayOf(if (warm) 0x01 else 0x00)
            writeFrame(port, SerialFraming.encode(TYPE_CARD_EVENT, payload))
            val atrFrame = awaitFrame(port, ATR_TIMEOUT_MS) { it.type == TYPE_ATR }
            atrFrame.body
        }
    }

    override suspend fun disconnect() {
        ioMutex.withLock {
            withContext(Dispatchers.IO) {
                serialPort?.let { runCatching { it.closePort() } }
                serialPort = null
            }
        }
    }

    private fun writeFrame(port: SerialPort, frame: ByteArray) {
        var offset = 0
        while (offset < frame.size) {
            val remaining = (frame.size - offset).toLong()
            val written = port.writeBytes(frame, remaining, offset.toLong())
            if (written < 0) throw IOException("Serial write failed on $portName")
            if (written == 0) throw IOException("Serial write returned 0 bytes on $portName")
            offset += written
        }
    }

    /**
     * Pump bytes from the port, feeding the decoder until a frame matching [predicate] is
     * produced or [timeoutMs] elapses. We track the deadline ourselves rather than relying on
     * coroutine [withTimeout], because the underlying jSerialComm `readBytes()` is a blocking
     * native call that does not cooperate with coroutine cancellation. Without this explicit
     * deadline check, a non-responsive firmware would loop here forever even though the
     * coroutine was cancelled.
     */
    private fun awaitFrame(
        port: SerialPort,
        timeoutMs: Long,
        predicate: (SerialFraming.Frame) -> Boolean,
    ): SerialFraming.Frame {
        val deadline = System.currentTimeMillis() + timeoutMs
        val buf = ByteArray(256)
        while (System.currentTimeMillis() < deadline) {
            val read = port.readBytes(buf, buf.size.toLong())
            if (read < 0) throw IOException("Serial read failed on $portName")
            for (i in 0 until read) {
                val frame = decoder.feed(buf[i])
                if (frame != null && predicate(frame)) return frame
            }
        }
        throw IOException("Timed out after ${timeoutMs}ms waiting for serial frame on $portName")
    }
}
