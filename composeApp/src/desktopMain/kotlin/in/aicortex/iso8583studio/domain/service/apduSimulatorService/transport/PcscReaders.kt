package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport

import javax.smartcardio.CardException
import javax.smartcardio.TerminalFactory

/**
 * Helpers around PC/SC reader discovery. Kept as a top-level object so the UI / config layers can
 * populate a reader picker without instantiating a transport.
 */
object PcscReaders {
    /**
     * Returns the names of every PC/SC reader the OS currently advertises. Returns an empty list
     * if the PC/SC service is not available on this machine (e.g. pcscd not running on Linux,
     * no reader drivers installed).
     */
    fun list(): List<String> = try {
        TerminalFactory.getDefault().terminals().list().map { it.name }
    } catch (_: CardException) {
        emptyList()
    }
}
