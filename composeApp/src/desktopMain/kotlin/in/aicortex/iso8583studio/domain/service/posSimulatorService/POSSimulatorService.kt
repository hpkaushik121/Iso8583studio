package `in`.aicortex.iso8583studio.domain.service.posSimulatorService

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.navigation.POSSimulatorConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Dedicated service for POS client logic
class POSSimulatorService(
    private val isoConfig: POSSimulatorConfig // For packing/unpacking messages
) {
    var hostAddress by mutableStateOf("127.0.0.1")
    var hostPort by mutableStateOf(8080)

    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var isConnected by mutableStateOf(false)
        private set

    // --- UI Callbacks ---
    var onLog: (LogEntry) -> Unit = {}
    var onRequestSent: (String) -> Unit = {}
    var onResponseReceived: (String) -> Unit = {}
    var onConnectionStateChange: (Boolean) -> Unit = {}




    private fun createLog(type: LogType, message: String, details: String? = null): LogEntry {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        return LogEntry(timestamp, type, message, details)
    }
}