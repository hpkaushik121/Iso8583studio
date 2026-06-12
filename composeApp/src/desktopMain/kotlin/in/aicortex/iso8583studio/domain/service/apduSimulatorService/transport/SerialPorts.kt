package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport

import com.fazecast.jSerialComm.SerialPort

/**
 * Helper for enumerating system serial ports. Used by UI to populate a port chooser when a user
 * wants to connect over USB-CDC to the STM32 firmware.
 */
object SerialPorts {
    fun list(): List<String> = SerialPort.getCommPorts().map { it.systemPortName }
}
