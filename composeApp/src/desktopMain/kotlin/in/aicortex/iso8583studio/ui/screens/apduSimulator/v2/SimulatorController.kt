package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.CommandApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core.ResponseApdu
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardProfile
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.CardRuntime
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GenerateAcHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GetChallengeHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GetDataHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.GpoHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.ReadRecordHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.SelectHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.runtime.handlers.VerifyHandler
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.ApduExchange
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.CardTransport
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.LoopbackTransport
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.PcscTransport
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.transport.SerialTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Three operating modes:
 *
 *   - LOOPBACK: in-process card runtime. Useful for developing profiles and writing test plans
 *               without any hardware. Requires an active profile.
 *   - PCSC:     PC/SC reader (e.g. ACS ACR39U) drives a real card or any reader the OS exposes.
 *               No card profile required — the card is whatever's in the reader.
 *   - SERIAL:   USB-CDC bridge to the STM32 firmware. The Studio is the *terminal* sending APDUs
 *               to the firmware; the firmware acts as the card and is read by an external POS via
 *               the XCRFID pinboard. Active profile is pushed to the firmware (TODO; for now the
 *               profile is informational and the firmware uses its own static ATR).
 */
enum class TransportMode { LOOPBACK, PCSC, SERIAL }

enum class ConnState { IDLE, CONNECTING, CONNECTED, ERROR }

/**
 * Holds all UI state for the v2 simulator. Single instance is created in [ApduSimulatorV2Screen]
 * and passed to every tab. All mutating methods are safe to call from a Compose [CoroutineScope].
 *
 * Use [hydrateFromConfig] to apply mode/transport/profile fields from a launched
 * [`in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig].
 */
class SimulatorController(private val scope: CoroutineScope) {
    var mode by mutableStateOf(TransportMode.LOOPBACK)
    var activeProfile by mutableStateOf<CardProfile?>(null)
    var pcscReader by mutableStateOf<String?>(null)
    var serialPort by mutableStateOf<String?>(null)
    var serialBaud by mutableStateOf(115200)

    fun hydrateFromConfig(
        modeName: String,
        pcscReaderName: String?,
        serialPortName: String?,
        baud: Int,
        profile: CardProfile?,
    ) {
        if (conn != ConnState.IDLE) return
        mode = runCatching { TransportMode.valueOf(modeName) }.getOrDefault(TransportMode.LOOPBACK)
        pcscReader = pcscReaderName?.takeIf { it.isNotBlank() }
        serialPort = serialPortName?.takeIf { it.isNotBlank() }
        serialBaud = baud
        activeProfile = profile
    }

    var transport by mutableStateOf<CardTransport?>(null)
        private set
    var conn by mutableStateOf(ConnState.IDLE)
        private set
    var atrHex by mutableStateOf("")
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    val exchanges = mutableStateListOf<ApduExchange>()
    private var traceJob: Job? = null

    fun canConnect(): Boolean = when (mode) {
        TransportMode.LOOPBACK -> activeProfile != null
        TransportMode.PCSC -> !pcscReader.isNullOrBlank()
        TransportMode.SERIAL -> !serialPort.isNullOrBlank()
    }

    fun connect() {
        if (conn == ConnState.CONNECTING || conn == ConnState.CONNECTED) return
        if (!canConnect()) return
        scope.launch {
            conn = ConnState.CONNECTING
            lastError = null
            exchanges.clear()
            runCatching {
                val t = buildTransport()
                transport = t
                traceJob?.cancel()
                traceJob = scope.launch { t.trace.collect { exchanges += it } }
                atrHex = bytesToHex(t.connect())
                conn = ConnState.CONNECTED
            }.onFailure {
                conn = ConnState.ERROR
                lastError = it.message ?: it::class.simpleName
                transport = null
            }
        }
    }

    fun disconnect() {
        val t = transport ?: run { conn = ConnState.IDLE; return }
        scope.launch {
            runCatching { t.disconnect() }.onFailure { lastError = it.message }
            traceJob?.cancel(); traceJob = null
            transport = null
            atrHex = ""
            conn = ConnState.IDLE
        }
    }

    fun reset(warm: Boolean = false) {
        val t = transport ?: return
        scope.launch {
            runCatching { atrHex = bytesToHex(t.reset(warm)) }
                .onFailure { lastError = it.message ?: "reset failed" }
        }
    }

    suspend fun transmit(cmd: CommandApdu): ResponseApdu {
        val t = transport ?: error("not connected")
        return t.transmit(cmd)
    }

    private fun buildTransport(): CardTransport = when (mode) {
        TransportMode.LOOPBACK -> {
            val profile = activeProfile ?: error("no profile selected")
            val runtime = CardRuntime(profile, defaultHandlers())
            LoopbackTransport(runtime)
        }
        TransportMode.PCSC -> {
            val name = pcscReader ?: error("no PC/SC reader selected")
            PcscTransport(name)
        }
        TransportMode.SERIAL -> {
            val port = serialPort ?: error("no serial port selected")
            SerialTransport(port, serialBaud)
        }
    }

    private fun defaultHandlers() = listOf(
        SelectHandler(),
        GpoHandler(),
        ReadRecordHandler(),
        GetDataHandler(),
        GetChallengeHandler(),
        VerifyHandler(),
        GenerateAcHandler(),
    )
}

internal fun bytesToHex(b: ByteArray): String = b.joinToString("") { "%02X".format(it) }
