package `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import `in`.aicortex.iso8583studio.data.HsmClient
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.domain.service.hostSimulatorService.Simulator
import `in`.aicortex.iso8583studio.hsm.HsmClientListener
import `in`.aicortex.iso8583studio.hsm.HsmConfig
import `in`.aicortex.iso8583studio.hsm.HsmSimulator
import `in`.aicortex.iso8583studio.hsm.payshield10k.HsmLogsListener
import `in`.aicortex.iso8583studio.hsm.payshield10k.PayShield10K
import `in`.aicortex.iso8583studio.hsm.payshield10k.PayShield10KFeatures
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditEntry
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuthActivity
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmCommandResult
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMVendor
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.createLogEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.atomics.ExperimentalAtomicApi

// Main HSM Service Implementation
class HsmServiceImpl(
    override var configuration: HSMSimulatorConfig
) : Simulator, HsmClientListener, HsmLogsListener {
    private var serverSocket: ServerSocket? = null
    private var activeHsm: HsmSimulator? = null
    private var selectedHsmType: HSMVendor = HSMVendor.THALES
    private var checkLogFileSize = LocalDateTime.now()

    private var _hsmState = MutableStateFlow(HsmState())
    val hsmState = _hsmState.asStateFlow()

    private var errorCallbacks: (() -> @Composable () -> Unit)? = null
    var receivedFromSource: (String?) -> Unit = {}
    var receivedFromSourceFormatted: (String?) -> Unit = {}
    var sentToSource: (String?) -> Unit = {}
    var sentToSourceFormatted: (String?) -> Unit = {}

    private var startTime: LocalDateTime? = null
    private var stopTime: LocalDateTime? = null
    private var serverJob: Job? = null
    init {
        CoroutineScope(Dispatchers.IO).launch {
            initialize(configuration.vendor,configuration.hsmConfig)
        }
    }

    /**
     * Initialize HSM based on selected type
     */
    suspend fun initialize(hsmType: HSMVendor, config: HsmConfig = HsmConfig()) {
        selectedHsmType = hsmType

        activeHsm = when (hsmType) {
            HSMVendor.THALES -> {
                PayShield10K( config,this).also { it.initialize() }
            }
            // Add other HSM types here
            else -> throw UnsupportedOperationException("HSM type $hsmType not yet implemented")
        }
    }
    /**
     * Get current HSM instance
     */
    fun getHsm(): HsmSimulator? = activeHsm

    /**
     * Get PayShield 10K specific instance (for advanced features)
     */
    fun getPayShield10K(): PayShield10K? {
        return activeHsm as? PayShield10K
    }


    override fun getConnectionCount(): Int {
        return _hsmState.value.connectionCount.get()
    }


    override fun getBytesIncoming(): Int {
       return 0
    }

    override fun getBytesOutgoing(): Int {
        return 0
    }

    override fun writeLog(log: LogEntry) {
        if (configuration.logFileName.isBlank()) {
            return
        }
        // Synchronize log file access
        try {
            val timestamp =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy/MM/dd HH:mm:ss"))
            File(configuration.logFileName).appendText(
                "\r\n${log.message}".replace("\r\n", "\r\n$timestamp  ${log.source}"),
                Charset.forName("UTF-8")
            )
            println( "\r\n${log.message}".replace("\r\n", "\r\n$timestamp  ${log.source}"))
        } catch (e: Exception) {
            // Ignore write errors
        }
        _hsmState.value = _hsmState.value.copy(
            rawRequest = _hsmState.value.rawRequest.apply {
                add(log)
            }
        )
        // Check log file size and rotate if needed
        val now = LocalDateTime.now()
        if (now.isAfter(checkLogFileSize)) {
            checkLogFileSize = now.plusSeconds(5)
            CoroutineScope(Dispatchers.IO).launch {
                checkAndRotateLogFile()
            }

        }
    }

    /**
     * Check log file size and rotate if needed
     */
    private suspend fun checkAndRotateLogFile() = withContext(Dispatchers.IO) {
        val logFile = File(configuration.logFileName)
        if (!logFile.exists()) {
            return@withContext
        }

        val maxSizeBytes = configuration.maxLogSizeInMB * 1024 * 1024
        if (logFile.length() <= maxSizeBytes) {
            return@withContext
        }

        // Get base name and extension
        val parts = logFile.nameWithoutExtension.split('_')
        val baseName = parts[0]
        val extension = logFile.extension

        var rotationIndex = 1

        try {
            // Find next available rotation index
            while (File("${logFile.parent}/$baseName${rotationIndex}.$extension").exists() &&
                rotationIndex != 11
            ) {
                rotationIndex++
            }

            // If all rotation files are used, delete oldest and shift others
            if (rotationIndex == 11) {
                File("${logFile.parent}/$baseName${1}.$extension").delete()

                for (i in 2 until 11) {
                    val oldFile = File("${logFile.parent}/$baseName${i}.$extension")
                    val newFile = File("${logFile.parent}/$baseName${i - 1}.$extension")

                    if (oldFile.exists()) {
                        oldFile.renameTo(newFile)
                    }
                }

                rotationIndex = 10
            }

            // Rename current log file
            logFile.renameTo(File("${logFile.parent}/$baseName${rotationIndex}.$extension"))
        } catch (e: Exception) {
            // Ignore errors during rotation
        }
    }
    override fun showError(item: @Composable (() -> Unit)) {

    }

    override fun showSuccess(item: @Composable (() -> Unit)) {

    }

    override fun showWarning(item: @Composable (() -> Unit)) {

    }

    override fun setShowErrorListener(resultDialogInterface: ResultDialogInterface) {

    }

    override fun <T : SimulatorConfig> setConfiguration(config: T) {
        this.configuration = config as HSMSimulatorConfig
    }

    /**
     * Start the HSM service
     */
    @OptIn(ExperimentalAtomicApi::class, ExperimentalStdlibApi::class)
    override suspend fun start() = withContext(Dispatchers.IO) {
        if (_hsmState.value.started) {
            return@withContext
        }

        // Initialize connections list
        _hsmState.value.activeClients.clear()

        // Initialize server socket for TCP/IP or REST connections
        serverSocket = if (configuration.serverAddress.isNotBlank()) {
            ServerSocket(
                configuration.serverPort,
                50,
                InetAddress.getByName(configuration.serverAddress)
            )
        } else {
            ServerSocket(configuration.serverPort)
        }

        // Log startup
        writeLog(
            createLogEntry(
                type = LogType.SOCKET,
                message = "===============GATEWAY STARTED===============",
                details = configuration.toString()
            )
        )

        // Set start time
        startTime = LocalDateTime.now()

        // Start server thread based on connection type
        _hsmState.value = _hsmState.value.copy(started = true)
        serverJob = CoroutineScope(Dispatchers.IO).launch{
            doListener()
        }
    }

    /**
     * Main listener method that handles incoming connections
     */
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun doListener() {
        while (_hsmState.value.started && NonCancellable.isActive) {
            try {
                processTcpIp()
            } catch (e: Exception) {
                // Handle errors
                writeLog(createLogEntry(type = LogType.ERROR, message = "FATAL ERROR ${e}"))
                writeLog(
                    createLogEntry(
                        type = LogType.DEBUG,
                        message = " THE GATEWAY WILL RESTART IN ${configuration.waitToRestart} seconds "
                    )
                )
                // Wait before restart
                val waitUntil =
                    LocalDateTime.now().plusSeconds(configuration.waitToRestart.toLong())
                while (LocalDateTime.now().isBefore(waitUntil) && _hsmState.value.started) {
                    delay(500)
                }
            }
        }
    }


    /**
     * Process TCP/IP connections
     */
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun processTcpIp() {
        while (_hsmState.value.started && NonCancellable.isActive) {
            try {
                // Wait until we have space for a new connection
                while (_hsmState.value.started && _hsmState.value.activeClients.size >= configuration.maxConcurrentConnection) {
                    delay(30)
                }

                // Accept a new client connection
                val client = createClient()
                val socket = withContext(Dispatchers.IO) {
                    serverSocket?.accept()
                } ?: continue

                client.incomingConnection = socket

                // Increment connection counter
                _hsmState.value = _hsmState.value.copy(connectionCount = _hsmState.value.connectionCount.apply { incrementAndGet() })

                // Get client IP address
                val remoteEndpoint = socket.remoteSocketAddress as InetSocketAddress
                val remoteAddress = remoteEndpoint.address.hostAddress
                val remotePort = remoteEndpoint.port

                client.remoteIPAddress = "$remoteAddress : $remotePort"

                writeLog(
                    createLogEntry(
                        type = LogType.CONNECTION,
                        message = "==========A REMOTE CLIENT CONNECTED FROM IP = ${client.remoteIPAddress}=========="
                    ).apply {
                        source = client.clientID
                    }

                )
                // Add client to active list
                _hsmState.value = _hsmState.value.copy( activeClients = _hsmState.value.activeClients.apply {
                    add(client)
                })
                // Start processing
                client.processGateway()


            } catch (e: Exception) {
                if (_hsmState.value.started) {
                    writeLog(createLogEntry(type = LogType.ERROR, message = "GATEWAY ERROR $e"))

                    // Call error callbacks
                    errorCallbacks?.invoke()

                    delay(3000)
                }
            }
        }
    }
    /**
     * Create a new client
     */
    private fun createClient(): HsmClient {
        val client = HsmClient(this)
        client.hsmClientListener = this
        return client
    }


    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun stop() = withContext(Dispatchers.IO) {
        if (!_hsmState.value.started) {
            return@withContext
        }

        _hsmState.value = _hsmState.value.copy(started = false)
        stopTime = LocalDateTime.now()

        // Close server socket
        serverSocket?.close()
        serverSocket = null

        // Cancel server thread
        serverJob?.cancel()
        serverJob = null

        // Close all client connections
        while (_hsmState.value.activeClients.isNotEmpty()) {
            val client = _hsmState.value.activeClients[0]
        }

        // Log shutdown
        writeLog(
            createLogEntry(
                type = LogType.SOCKET,
                message = "===============GATEWAY STOPPED==============="
            )
        )
    }

    @OptIn(ExperimentalAtomicApi::class)
    override fun isStarted(): Boolean {
        return _hsmState.value.started
    }

    /**
     * Execute an HSM command directly from the GUI (Host Commands tab or Secure Commands tab).
     * Logs both the outbound command and the inbound response to the Logs panel and log file,
     * and fires the handler-tab callbacks so the live request/response view also updates.
     *
     * @param rawCommand  Full wire-format command string (e.g. "0000CA…")
     * @param source      Label shown in the log (e.g. "HOST-CMD" or "SECURE-CMD")
     */
    suspend fun executeSecureCommand(
        rawCommand: String,
        source: String = "GUI-CONSOLE"
    ): String {
        val cmdCode = rawCommand.drop(4).take(2).ifBlank { "??" }

        // Log the outbound command (request)
        writeLog(
            createLogEntry(
                type    = LogType.HSM,
                message = "► [$source] CMD $cmdCode  →  ${rawCommand}"
            ).also { it.source = source }
        )
        receivedFromSource(rawCommand)

        val response = activeHsm?.getProcessor()?.processCommand(rawCommand)
            ?: "ERROR: No active HSM"

        // Derive error-code from wire response (bytes 7-8 after the 4-char header + 2-char resp-code)
        val errCode  = response.drop(6).take(2)
        val logType  = if (errCode == "00") LogType.HSM else LogType.ERROR

        // Log the inbound response
        writeLog(
            createLogEntry(
                type    = logType,
                message = "◄ [$source] RSP $cmdCode  ←  $response"
            ).also { it.source = source }
        )
        sentToSource(response)

        return response
    }

    // ── Key Component Generation (direct — bypasses wire protocol) ───────────

    /**
     * Generates one key component directly, returning structured data.
     * Returns a map with: clearKey, encryptedKey, kcv — or null on failure.
     */
    suspend fun generateKeyComponent(
        keyTypeCode: String = "001",
        scheme: String = "U"
    ): Map<String, String>? {
        writeLog(
            createLogEntry(
                type    = LogType.HSM,
                message = "► [SECURE-CMD] GC  KeyType=$keyTypeCode  Scheme=$scheme"
            ).also { it.source = "SECURE-CMD" }
        )
        val ps = getPayShield10K() ?: return null
        val result = when (val r = ps.generateKeyComponentDirect(keyTypeCode, scheme)) {
            is HsmCommandResult.Success -> r.data.mapValues { it.value.toString() }
            else -> null
        }
        writeLog(
            createLogEntry(
                type    = if (result != null) LogType.HSM else LogType.ERROR,
                message = if (result != null)
                    "◄ [SECURE-CMD] GC  KCV=${result["kcv"]}  EncKey=${result["encryptedKey"]?.take(16)}…"
                else
                    "◄ [SECURE-CMD] GC  FAILED"
            ).also { it.source = "SECURE-CMD" }
        )
        return result
    }

    /**
     * XORs a list of hex-encoded keys of identical length and returns the result as uppercase hex.
     */
    fun xorHexKeys(keys: List<String>): String {
        if (keys.isEmpty()) return ""
        val byteArrays = keys.map { hex ->
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
        val result = byteArrays[0].copyOf()
        for (i in 1 until byteArrays.size) {
            val b = byteArrays[i]
            for (j in result.indices) {
                result[j] = (result[j].toInt() xor b[j].toInt()).toByte()
            }
        }
        return result.joinToString("") { "%02X".format(it) }
    }

    /** Calculates the KCV (3-byte encrypt-8-zeros) for a clear key supplied as hex. */
    fun computeKeyKcv(keyHex: String): String =
        getPayShield10K()?.calculateKcv(keyHex) ?: "??????"

    /**
     * Forms a working key by XORing [componentHexList] directly via the command processor.
     * Returns a map with clearKey, encryptedKey, kcv — or null on failure.
     * Used by the Secure Command GUI (plain key is visible here).
     */
    suspend fun formKeyFromComponents(
        keyTypeCode: String = "001",
        scheme: String = "U",
        componentHexList: List<String>
    ): Map<String, String>? {
        writeLog(
            createLogEntry(
                type    = LogType.HSM,
                message = "► [SECURE-CMD] FK  KeyType=$keyTypeCode  Scheme=$scheme  Components=${componentHexList.size}"
            ).also { it.source = "SECURE-CMD" }
        )
        val ps = getPayShield10K() ?: return null
        val result = when (val r = ps.formKeyFromComponentsDirect(keyTypeCode, scheme, componentHexList)) {
            is HsmCommandResult.Success -> r.data.mapValues { it.value.toString() }
            is HsmCommandResult.Error   -> mapOf("error" to "${r.errorCode}: ${r.message}")
        }
        val isError = result.containsKey("error")
        writeLog(
            createLogEntry(
                type    = if (isError) LogType.ERROR else LogType.HSM,
                message = if (isError)
                    "◄ [SECURE-CMD] FK  FAILED: ${result["error"]}"
                else
                    "◄ [SECURE-CMD] FK  KCV=${result["kcv"]}  EncKey=${result["encryptedKey"]?.take(16)}…"
            ).also { it.source = "SECURE-CMD" }
        )
        return result
    }

    // ── Console Authorization ────────────────────────────────────────────────

    private fun payShieldFeatures(): PayShield10KFeatures? =
        (getPayShield10K()?.getFeatures()) as? PayShield10KFeatures

    private fun lmkId(): String =
        getPayShield10K()?.config?.lmkId ?: "00"

    /**
     * Simulates custodian card insertion — grants all console authorizations.
     *
     * @param durationMs   How long the authorization lasts (default 8 hours)
     * @param officerNames Simulated officer identifiers recorded in the audit log
     */
    fun authorizeConsole(
        activities: List<AuthActivity> = AuthActivity.values().toList(),
        durationMs: Long = 8L * 60 * 60 * 1000,
        officerNames: List<String> = listOf("SIM-OFFICER-1")
    ) {
        payShieldFeatures()?.authorizeActivities(lmkId(), activities, durationMs, officerNames)
    }

    /** Revoke all or specific console authorizations immediately. */
    fun revokeConsole(activities: List<AuthActivity> = AuthActivity.values().toList()) {
        payShieldFeatures()?.revokeAuthorizations(lmkId(), activities)
    }

    /** Returns true if the given activity is currently authorized. */
    fun isConsoleAuthorized(activity: AuthActivity): Boolean =
        payShieldFeatures()?.isAuthorized(lmkId(), activity) ?: false

    /**
     * Returns the expiry epoch-ms for the earliest-expiring authorization among [activities],
     * or null if none are active.
     */
    fun consoleAuthExpiry(
        activities: List<AuthActivity> = AuthActivity.values().toList()
    ): Long? = payShieldFeatures()?.getAuthorizationExpiry(lmkId(), activities)

    fun clearLogs() {
        _hsmState.value = _hsmState.value.copy(rawRequest = mutableStateListOf(),
            formattedRequest = mutableStateListOf(),
            rawResponse = mutableStateListOf(),
            formattedResponse = mutableStateListOf()
        )
    }

    override fun onDisconnected(hsmClient: HsmClient?) {
        writeLog(
            createLogEntry(
                type = LogType.CONNECTION,
                message = "CONNECTION MAY BE CLOSED BY REMOTE COMPUTER/TERMINAL = ${hsmClient?.remoteIPAddress}"
            )
        )
        _hsmState.value = _hsmState.value.copy(activeClients = _hsmState.value.activeClients.apply {
            remove( this.first { it.clientID ==  hsmClient?.clientID})
        },
            connectionCount = _hsmState.value.connectionCount.apply { decrementAndGet() })
    }

    override fun onSentToSource(data: String?) {
        writeLog(
            createLogEntry(
                type = LogType.HSM,
                message = data ?: "NULL"
            )
        )
        sentToSource(data)
    }

    override fun onReceivedFormSource(data: String?,hsmClient : HsmClient?) {
        writeLog(
            createLogEntry(
                type = LogType.INFO,
                message = data ?: "NULL"
            )
        )
        CoroutineScope(Dispatchers.IO).launch {
            receivedFromSource(data)
            val response = activeHsm?.getProcessor()?.processCommand(data!!)
            hsmClient?.send(response)
        }

    }

    override fun onAuditLog(auditLog: AuditEntry) {
        writeLog(
            createLogEntry(
                type = LogType.INFO,
                message = auditLog.toString()
            )
        )
    }

    override fun log(auditLog: String) {
        writeLog(
            createLogEntry(
                type = LogType.INFO,
                message = auditLog.toString()
            )
        )
    }

    override fun onFormattedRequest(log: String) {
        receivedFromSourceFormatted.invoke(log)
    }

    override fun onFormattedResponse(log: String) {
        sentToSourceFormatted.invoke(log)
    }

}