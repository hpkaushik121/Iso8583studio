package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import javax.smartcardio.Card
import javax.smartcardio.CardChannel
import javax.smartcardio.CommandAPDU
import javax.smartcardio.TerminalFactory

/**
 * PC/SC reader transport using `javax.smartcardio` (JDK built-in). Drives a real reader such as
 * the ACS ACR39U-I1 — or anything the OS exposes as a PC/SC reader (incl. virtual ones).
 *
 * The blocking smartcardio calls are dispatched on [Dispatchers.IO] so this transport plays nice
 * with the suspending [CardTransport] contract. Field access is single-threaded by virtue of all
 * mutating operations going through suspend functions.
 */
class PcscTransport(
    private val readerName: String,
    private val protocolPreference: String = "T=*",
) : CardTransport {

    override val name: String = "pcsc:$readerName"

    private val _trace = MutableSharedFlow<ApduExchange>(
        replay = 0,
        extraBufferCapacity = 64,
    )

    override val trace: Flow<ApduExchange> = _trace.asSharedFlow()

    private var card: Card? = null
    private var channel: CardChannel? = null

    override suspend fun connect(): ByteArray = withContext(Dispatchers.IO) {
        val terminal = TerminalFactory.getDefault().terminals().getTerminal(readerName)
            ?: error("PC/SC terminal not found: $readerName")
        val c = terminal.connect(protocolPreference)
        card = c
        channel = c.basicChannel
        c.atr.bytes
    }

    override suspend fun transmit(command: CommandApdu): ResponseApdu = withContext(Dispatchers.IO) {
        val ch = channel ?: error("PcscTransport not connected")
        val sentAt = System.currentTimeMillis()
        val raw = ch.transmit(CommandAPDU(command.toBytes())).bytes
        val response = ResponseApdu.parse(raw)
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

    override suspend fun reset(warm: Boolean): ByteArray = withContext(Dispatchers.IO) {
        card?.disconnect(true)
        card = null
        channel = null
        val terminal = TerminalFactory.getDefault().terminals().getTerminal(readerName)
            ?: error("PC/SC terminal not found: $readerName")
        val c = terminal.connect(protocolPreference)
        card = c
        channel = c.basicChannel
        c.atr.bytes
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                card?.disconnect(false)
            } catch (_: Throwable) {
                // Idempotent: ignore failures from already-closed cards / detached readers.
            } finally {
                card = null
                channel = null
            }
        }
    }
}
