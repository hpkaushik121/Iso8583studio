package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.hexToBytes
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.CardRuntime
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-process transport that drives a [CardRuntime] directly. No reader, no wire — the cheapest
 * way to exercise handlers from terminal-side code, and the basis for unit/integration tests.
 */
class LoopbackTransport(private val runtime: CardRuntime) : CardTransport {
    override val name: String = "loopback"

    private val _trace = MutableSharedFlow<ApduExchange>(
        replay = 0,
        extraBufferCapacity = 64,
    )

    override val trace: Flow<ApduExchange> = _trace.asSharedFlow()

    override suspend fun connect(): ByteArray {
        return runtime.session.profile.atr.hexToBytes()
    }

    override suspend fun transmit(command: CommandApdu): ResponseApdu {
        val sentAt = System.currentTimeMillis()
        val response = runtime.process(command)
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
        return response
    }

    override suspend fun reset(warm: Boolean): ByteArray {
        runtime.session.state = SessionState()
        return runtime.session.profile.atr.hexToBytes()
    }

    override suspend fun disconnect() {
        // No-op for in-process transport.
    }
}
