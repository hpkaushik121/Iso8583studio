package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over a smart-card connection. Three implementations live in this package family:
 *
 *   - LoopbackTransport: in-process, drives the runtime directly. Fastest dev loop and the basis
 *                        for unit / integration tests.
 *   - PcscTransport:     PC/SC reader (e.g. ACS ACR39U) talking to a real card or to a card
 *                        emulator the OS exposes as a reader.
 *   - SerialTransport:   USB-CDC link to the STM32 firmware acting as the card. The PC is the
 *                        terminal driver; the firmware bridges APDUs to ISO 7816-3 lines.
 *
 * Implementations are responsible for connect/disconnect and for surfacing ATR + transmission
 * trace. They do NOT interpret APDUs.
 */
interface CardTransport {
    val name: String

    /** Power on the card / open the channel. Returns the ATR (raw historical+TS bytes). */
    suspend fun connect(): ByteArray

    /** Send one C-APDU and receive the R-APDU. Length-correction (61xx/6Cxx) handled by callers. */
    suspend fun transmit(command: CommandApdu): ResponseApdu

    /** Cold or warm reset depending on implementation; returns new ATR. */
    suspend fun reset(warm: Boolean = false): ByteArray

    /** Power off / close. Idempotent. */
    suspend fun disconnect()

    /**
     * Stream of every APDU exchange that crossed the wire, including the ones produced by
     * connect/reset (e.g. PPS). Subscribers see exchanges in real time and can persist them.
     */
    val trace: Flow<ApduExchange>
}

/**
 * One captured request/response pair. Timestamps are epoch millis.
 */
data class ApduExchange(
    val sentAt: Long,
    val receivedAt: Long,
    val command: CommandApdu,
    val response: ResponseApdu,
    val transportName: String,
) {
    val durationMs: Long get() = receivedAt - sentAt
}
