package `in`.aicortex.iso8583studio.domain.service

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.awt.ComposeWindow
import `in`.aicortex.iso8583studio.data.EncrDecrHandler
import `in`.aicortex.iso8583studio.data.DialHandler
import `in`.aicortex.iso8583studio.data.GatewayClient
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.KeyManagement
import `in`.aicortex.iso8583studio.data.PartialISO8583Encryption
import `in`.aicortex.iso8583studio.data.PermanentConnection
import `in`.aicortex.iso8583studio.data.RS232Handler
import `in`.aicortex.iso8583studio.data.SSLTcpClient
import `in`.aicortex.iso8583studio.data.model.ActionWhenDisconnect
import `in`.aicortex.iso8583studio.data.model.CipherMode
import `in`.aicortex.iso8583studio.data.model.CipherType
import `in`.aicortex.iso8583studio.data.model.ConnectionType
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.data.model.SignatureChecking
import `in`.aicortex.iso8583studio.data.model.SpecialFeature
import `in`.aicortex.iso8583studio.data.model.TransmissionType
import `in`.aicortex.iso8583studio.data.model.UnauthorizedAccessException
import `in`.aicortex.iso8583studio.data.model.VerificationError
import `in`.aicortex.iso8583studio.data.model.VerificationException
import `in`.aicortex.iso8583studio.domain.utils.isIpMatched
import `in`.aicortex.iso8583studio.data.IsoCoroutine
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Implementation of the Gateway service
 */
class GatewayServiceImpl : GatewayService {
    private lateinit var _configuration: GatewayConfig
    override val configuration: GatewayConfig
        get() = _configuration

    @OptIn(ExperimentalAtomicApi::class)
    internal val started = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var sslServerSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var monitorJob: Job? = null
    var holdMessage: Boolean = false

    internal val activeClients = mutableListOf<GatewayClient>()
    private val clientsMutex = Mutex()

    internal val permanentConnections = ConcurrentHashMap<Int, PermanentConnection>()

    var firstRs232Handler: RS232Handler? = null
    var secondRs232Handler: RS232Handler? = null

    private var partialEncryption: PartialISO8583Encryption? = null
    internal var simpleEncryptionForProxy: EncrDecrHandler? = null

    internal var bytesIncoming = AtomicInteger(0)
    internal var bytesOutgoing = AtomicInteger(0)
    internal var connectionCount = AtomicInteger(0)

    internal var wrongFormatTrans = AtomicInteger(0)
    internal var unauthorisedTrans = AtomicInteger(0)
    internal var successfulTrans = AtomicInteger(0)
    internal var unsentTrans = AtomicInteger(0)
    internal var incompleteTrans = AtomicInteger(0)
    internal var timeoutTrans = AtomicInteger(0)
    internal var composeWindow = ComposeWindow()
    private var monitorClient: Socket? = null
    private var monitorServerWait = LocalDateTime.now()
    private var checkLogFileSize = LocalDateTime.now()
    private val monitorMessageBuilder = StringBuilder()
    private val monitorMessageMutex = Mutex()

    private var startTime: LocalDateTime? = null
    private var stopTime: LocalDateTime? = null

    var resultDialogInterface: ResultDialogInterface? = null

    private var _endeService: KeyManagement? = null
    override val endeService: KeyManagement
        get() {
            if (_endeService == null) {
                initializeEncryptionService()
            }
            return _endeService!!
        }

    private val coroutineScope = IsoCoroutine(this)

    // Callback lists
    private var errorCallbacks: (() -> @Composable () -> Unit)? = null
    private var receivedFromDest: (Iso8583Data?) -> Unit = {}
    private var receivedFromSource: (Iso8583Data?) -> Unit = {}
    private var sentToSource: (Iso8583Data?) -> Unit = {}
    private var sentToDest: (Iso8583Data?) -> Unit = {}
    private var beforeReceiveCallbacks: (GatewayClient) -> Unit = {}
    private var beforeWriteLogCallbacks: (String) -> Unit = {}
    private var adminResponseCallbacks: suspend (GatewayClient, ByteArray) -> ByteArray? =
        { _, _ -> null }
    var sendHoldMessage: (suspend () -> Unit)? = null

    constructor() {
        // Default constructor
    }

    constructor(config: GatewayConfig) {
        _configuration = config
    }

    /**
     * Set a new gateway configuration
     */
    override fun setConfiguration(config: GatewayConfig) {
        _configuration = config

        // Initialize encryption service if needed
        if (config.checkSignature == SignatureChecking.ONE_PASSWORD) {
            endeService.defaultSignature = config.password!!
        }

        // Set up partial encryption if configured
        if (config.advancedOptions?.isPartialEncryption() == true) {
            partialEncryption = PartialISO8583Encryption(
                config.advancedOptions?.obscureType!!,
                config.advancedOptions?.getObscuredBits()!!
            )
            partialEncryption?.setActive(this)
        }
    }

    /**
     * Start the gateway service
     */
    @OptIn(ExperimentalAtomicApi::class, ExperimentalStdlibApi::class)
    override suspend fun start() = withContext(Dispatchers.IO) {
        if (started.load()) {
            return@withContext
        }

        // Initialize connections list
        activeClients.clear()

        // Initialize server socket for TCP/IP or REST connections
        if ((configuration.gatewayType == GatewayType.SERVER && configuration.serverConnectionType == ConnectionType.TCP_IP)||
            (configuration.gatewayType == GatewayType.SERVER && configuration.serverConnectionType == ConnectionType.REST)) {
            serverSocket = if (configuration.serverAddress.isNotBlank()) {
                ServerSocket(
                    configuration.serverPort,
                    50,
                    InetAddress.getByName(configuration.serverAddress)
                )
            } else {
                ServerSocket(configuration.serverPort)
            }
        }

        // Initialize special features if configured
        if (configuration.advancedOptions?.specialFeature == SpecialFeature.SimpleEncryptionForProxy ||
            configuration.advancedOptions?.specialFeature == SpecialFeature.SimpleDecryptionForProxy
        ) {

            simpleEncryptionForProxy = EncrDecrHandler(
                CipherType.DES,
                configuration.advancedOptions?.secretKey?.hexToByteArray() ?: byteArrayOf(),
                ByteArray(8),
                CipherMode.ECB
            )
        }

        // Start monitor thread if configured
        if (configuration.monitorAddress.isNotBlank()) {
            monitorJob = coroutineScope.launchSafely {
                doSendToMonitor()
            }
        }

        // Log startup
        writeLog("===============GATEWAY STARTED===============")
        writeLog(configuration.toString())
        writeLog("=============================================")

        // Set start time
        startTime = LocalDateTime.now()

        // Setup permanent connections
        setupPermanentConnections()

        // Start server thread based on connection type
        started.store(true)
        serverJob = coroutineScope.launchSafely {
            doListener()
        }
    }

    /**
     * Stop the gateway service
     */
    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun stop() = withContext(Dispatchers.IO) {
        if (!started.load()) {
            return@withContext
        }

        started.store(false)
        stopTime = LocalDateTime.now()

        // Close server socket
        if ((configuration.gatewayType == GatewayType.SERVER && configuration.serverConnectionType == ConnectionType.TCP_IP)||
            (configuration.gatewayType == GatewayType.SERVER && configuration.serverConnectionType == ConnectionType.REST)) {
            serverSocket?.close()
            serverSocket = null

            sslServerSocket?.close()
            sslServerSocket = null
        }

        // Cancel server thread
        serverJob?.cancel()
        serverJob = null

        // Cancel monitor thread
        monitorJob?.cancel()
        monitorJob = null

        // Close all client connections
        clientsMutex.withLock {
            while (activeClients.isNotEmpty()) {
                val client = activeClients[0]
                client.lastError = VerificationException(
                    "GATEWAY REQUEST TO STOP...",
                    VerificationError.OTHERS
                )
                clientError(client)
            }
        }

        // Close RS232 handlers
        firstRs232Handler?.let {
            if (it.isOpen()) {
                it.close()
            }
        }
        firstRs232Handler = null

        secondRs232Handler?.let {
            if (it.isOpen()) {
                it.close()
            }
        }
        secondRs232Handler = null

        // Stop all permanent connections
        permanentConnections.values.forEach { it.stop() }
        permanentConnections.clear()

        // Log shutdown
        writeLog("=============================================")
        writeLog("===============GATEWAY STOPPED===============")
    }

    /**
     * Check if the gateway is currently running
     */
    @OptIn(ExperimentalAtomicApi::class)
    override fun isStarted(): Boolean {
        return started.load()
    }

    /**
     * Get the number of active connections
     */
    override fun getConnectionCount(): Int {
        return connectionCount.get()
    }

    /**
     * Get the list of active client connections
     */
    override fun getConnectionList(): List<GatewayClient> {
        return activeClients.toList()
    }

    /**
     * Get the main permanent connection if it exists
     */
    override fun getMainPermanentConnection(): PermanentConnection? {
        return permanentConnections[9999]
    }

    /**
     * Get the total number of bytes received from clients
     */
    override fun getBytesIncoming(): Int {
        return bytesIncoming.get()
    }

    /**
     * Get the total number of bytes sent to clients
     */
    override fun getBytesOutgoing(): Int {
        return bytesOutgoing.get()
    }

    /**
     * Get the time when the gateway was started
     */
    override fun getStartTime(): LocalDateTime? {
        return startTime
    }

    /**
     * Get the time when the gateway was stopped
     */
    override fun getStopTime(): LocalDateTime? {
        return stopTime
    }

    /**
     * Create a new client
     */
    override fun createClient(): GatewayClient {
        val client = GatewayClient(this)
        client.clientID = configuration.clientID

        if (configuration.gatewayType == GatewayType.CLIENT) {
            if (activeClients.size > 1) {
                client.clientID += connectionCount.get().toString()
            }

            if (!endeService.containsKey(client.clientID)) {
                endeService.newClientKeys(client.clientID)
            }
        }

        client.locationID = configuration.locationID
        client.onError = {
            coroutineScope.launchSafely {
                clientError(it)
            }
        }

        client.onReceivedFormSource = {
            coroutineScope.launchSafely {
                receivedFromSource(it)
            }
        }

        client.beforeReceive = {
            coroutineScope.launchSafely {
                beforeReceiveCallbacks(it)
            }
        }

        client.onReceivedFormDest = { b ->
            coroutineScope.launchSafely {
                receivedFromDest( b)
            }
        }

        client.onAdminResponse = { cl, data ->
            adminResponseCallbacks(cl, data)
        }
        client.onSentToSource = { data -> sentToSource(data) }

        client.onSentToDest = { data -> sentToDest(data) }

        client.timeOut = configuration.transactionTimeOut

        return client
    }

    /**
     * Add a client to the active clients list
     */
    suspend fun addClient(client: GatewayClient) {
        clientsMutex.withLock {
            activeClients.add(client)
        }
    }

    /**
     * Remove a client from the active clients list
     */
    suspend fun removeClient(client: GatewayClient) {
        clientsMutex.withLock {
            activeClients.remove(client)
        }
    }

    /**
     * Write a log message
     */
    override fun writeLog(message: String) {
        writeLog(message, "")
    }

    /**
     * Write a log message with a client reference
     */
    override fun writeLog(client: GatewayClient, message: String) {
        writeLog(message, "ID[${client.index.toString().padStart(8, '0')}] ")
    }

    /**
     * Write a log message with a prefix
     */
    private fun writeLog(message: String, prefix: String) {
        if (configuration.logFileName.isBlank()) {
            return
        }

        val formattedMessage = if (prefix.uppercase() != "NULL") {
            val timestamp =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy/MM/dd HH:mm:ss"))
            "\r\n$message".replace("\r\n", "\r\n$timestamp  $prefix")
        } else {
            message
        }

        // Synchronize log file access
        try {
            File(configuration.logFileName).appendText(
                formattedMessage,
                Charset.forName(configuration.getEncoding())
            )
        } catch (e: Exception) {
            // Ignore write errors
        }
        println(formattedMessage)
        // Call before write log callbacks
        beforeWriteLogCallbacks(formattedMessage)


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

    /**
     * Get statistics about gateway operations
     */
    override fun getStatistics(): String {
        val statsBuilder = StringBuilder()

        statsBuilder.append("|STA|Connection: ${connectionCount.get()}\r\n")
        statsBuilder.append("Concurrent: ${activeClients.size}")

        if (configuration.transmissionType == TransmissionType.SYNCHRONOUS) {
            statsBuilder.append("\r\nWrong Format: ${wrongFormatTrans.get()}")
            statsBuilder.append("\r\nUnauthorised: ${unauthorisedTrans.get()}")
            statsBuilder.append("\r\nUnsent: ${unsentTrans.get()}")
            statsBuilder.append("\r\nTimeout: ${timeoutTrans.get()}")
            statsBuilder.append("\r\nIncomplete: ${incompleteTrans.get()}")
            statsBuilder.append("\r\nSuccessful: ${successfulTrans.get()}")
            statsBuilder.append("\r\nTotal Trans: ${getTotalTrans()}")
            statsBuilder.append("\r\nFrom Source: ${bytesIncoming.get()}")
            statsBuilder.append("\r\nFrom Destination: ${bytesOutgoing.get()}")
        }

        // Add permanent connection status
        for (conn in permanentConnections.values) {
            statsBuilder.append("\r\nPermanent Connection to ${conn.hostNii},${conn.port}")
            statsBuilder.append(if (conn.isConnected) ": OK" else ": DROPPED")
        }

        return statsBuilder.toString()
    }

    /**
     * Get the total number of transactions
     */
    private fun getTotalTrans(): Int {
        return wrongFormatTrans.get() +
                unauthorisedTrans.get() +
                unsentTrans.get() +
                timeoutTrans.get() +
                incompleteTrans.get() +
                successfulTrans.get()
    }

    /**
     * Get a string representation of all client connections
     */
    override fun getConnectionListString(): String {
        val builder = StringBuilder("|CON|")

        // Check for stale connections
        val now = LocalDateTime.now()

        // Add connection details
        for (client in activeClients) {
            builder.append(client.index.toString())
            builder.append(";")
            builder.append(client.timeCreated.format(DateTimeFormatter.ofPattern("dd/MM hh:mm:ss")))
            builder.append(";")
            builder.append(client.remoteIPAddress)
            builder.append(";")
            builder.append(client.totalTransmission.toString())
            builder.append(";")
            builder.append(client.bytesReceiveFromSource.toString())
            builder.append(";")
            builder.append(client.bytesRececeivedFromDestination.toString())
            builder.append("|")
        }

        return builder.toString()
    }

    /**
     * Main listener method that handles incoming connections
     */
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun doListener() {
        while (started.load() && isActive) {
            try {
                when (configuration.serverConnectionType) {
                    ConnectionType.TCP_IP,ConnectionType.REST  -> processTcpIp()
                    ConnectionType.COM -> processRs232()
                    ConnectionType.DIAL_UP -> processRs232()
                }
            } catch (e: Exception) {
                // Handle errors
                writeLog("FATAL ERROR ${e}")
                writeLog("=====================================================")
                writeLog(" THE GATEWAY WILL RESTART IN ${configuration.waitToRestart} seconds ")
                writeLog("=====================================================")

                // Wait before restart
                val waitUntil =
                    LocalDateTime.now().plusSeconds(configuration.waitToRestart.toLong())
                while (LocalDateTime.now().isBefore(waitUntil) && started.load()) {
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
        while (started.load() && isActive) {
            try {
                // Wait until we have space for a new connection
                while (started.load() && activeClients.size >= configuration.maxConcurrentConnection) {
                    delay(30)
                }

                // Accept a new client connection
                val client = createClient()
                val socket = withContext(Dispatchers.IO) {
                    serverSocket?.accept()
                } ?: continue

                val clientSocket = Socket()
                clientSocket.soTimeout = 0
                clientSocket.tcpNoDelay = true

                client.firstConnection = socket

                // Increment connection counter
                connectionCount.incrementAndGet()

                // Get client IP address
                val remoteEndpoint = socket.remoteSocketAddress as InetSocketAddress
                val remoteAddress = remoteEndpoint.address.hostAddress
                val remotePort = remoteEndpoint.port

                client.remoteIPAddress = "$remoteAddress : $remotePort"

                writeLog(
                    client,
                    "==========A REMOTE CLIENT CONNECTED FROM IP = ${client.remoteIPAddress}=========="
                )

                // Check IP deny list
                if (!configuration.advancedOptions?.iPsDenied.isNullOrBlank() &&
                    isIpMatched(
                        remoteAddress,
                        configuration.advancedOptions?.iPsDenied?.trim() ?: ""
                    )
                ) {

                    writeLog(client, "THE IP IS DENIED")
                    socket.close()
                    continue
                }

                // Check IP allow list
                if (!configuration.advancedOptions?.onlyAllowIps.isNullOrBlank() &&
                    !isIpMatched(remoteAddress, configuration.advancedOptions?.onlyAllowIps?.trim() ?: "")) {

                    writeLog(client, "THE IP IS NOT ALLOWED")
                    socket.close()
                    continue
                }

                // Handle SSL if configured
                if (configuration.advancedOptions?.sslServer == true) {
                    val sslClient = SSLTcpClient.createSSLClient(client, true)

                    if (sslClient == null) {
                        writeLog(
                            client,
                            "==========CONNECTION TERMINATED BECAUSE OF SSL AUTHENTICATION FAILURE${client.remoteIPAddress}=========="
                        )
                        socket.close()
                        continue
                    }

                    client.sslIncoming = sslClient
                }

                // Add client to active list
                addClient(client)
                // Start processing
                client.processGateway()


            } catch (e: Exception) {
                if (started.load()) {
                    writeLog("GATEWAY ERROR $e")

                    // Call error callbacks
                    errorCallbacks?.invoke()

                    delay(3000)
                }
            }
        }
    }

    /**
     * Process REST connections
     */
    private suspend fun processRest() {
        // Implement REST processing logic here
    }

    /**
     * Process RS232 connections
     */
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun processRs232() {
        while (started.load() && isActive) {
            if (firstRs232Handler == null) {
                // Initialize RS232 handler
                firstRs232Handler =
                    if (configuration.serverConnectionType != ConnectionType.DIAL_UP) {
                        RS232Handler(configuration.serverAddress, configuration.serverPort)
                    } else {
                        DialHandler(
                            configuration.serverAddress.substring(1),
                            configuration.serverPort
                        )
                    }

                firstRs232Handler?.readMessageTimeOut = 100000000
                firstRs232Handler?.open()
                firstRs232Handler?.stxEtxRule = true

                // Create and add client
                val client = createClient()
                addClient(client)
            }

            // Process gateway
            clientsMutex.withLock {
                if (activeClients.isNotEmpty()) {
                    activeClients[0].doProcessGateway()
                }
            }

            delay(100)
        }
    }

    /**
     * Initialize the encryption service
     */
    private fun initializeEncryptionService() {
        if (_endeService != null) {
            return
        }

        if (configuration.privateKey == null) {
            throw VerificationException("KEY IS NOT CONFIGURED", VerificationError.DECLINED)
        }

//        _endeService = if (!KeyManagement.isHSMMode) {
//            KeyManagement(
//                configuration.cipherType,
//                configuration.privateKey!!,
//                configuration.iv!!,
//                configuration.cipherMode,
//                configuration.hashAlgorithm
//            )
//        } else {
//            KeyManagementHsm(
//                configuration.privateKey!!,
//                ByteArray(8),
//                this,
//                configuration.hashAlgorithm
//            )
//        }

        _endeService = KeyManagement(
            configuration.cipherType,
            configuration.privateKey!!,
            configuration.iv!!,
            configuration.cipherMode,
            configuration.hashAlgorithm
        )

        if (configuration.checkSignature == SignatureChecking.ONE_PASSWORD) {
            _endeService?.defaultSignature = configuration.password!!
        } else if (configuration.gatewayType == GatewayType.CLIENT) {
            _endeService?.defaultSignature = configuration.password!!
        }

        _endeService?.keyExpireAfterTimes = configuration.keyExpireAfter
    }

    /**
     * Set up permanent connections
     */
    private fun setupPermanentConnections() {
        val nccParameters = configuration.advancedOptions?.getNCCParameterList()

        if (nccParameters.isNullOrEmpty()) {
            // Check for global permanent connection
            if (configuration.advancedOptions?.permanentConnectionToHost == "*") {
                val connection = PermanentConnection(
                    this,
                    configuration.destinationServer,
                    configuration.destinationPort,
                    9999
                )
                permanentConnections[9999] = connection
                connection.start()
            }
        } else {
            // Set up permanent connections for each NCC parameter
            val permanentHosts = configuration.advancedOptions?.getPermanentConnectionHost()

            for (nccParam in nccParameters) {
                if (permanentHosts != null) {
                    for (nii in permanentHosts) {
                        if (nii == nccParam.nii) {
                            val connection = PermanentConnection(
                                this,
                                nccParam.hostAddress,
                                nccParam.port,
                                nccParam.nii
                            )
                            permanentConnections[nii] = connection
                            connection.start()
                        }
                    }
                }
            }
        }
    }

    /**
     * Send to monitor server
     */
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun doSendToMonitor() {
        while (started.load() && isActive) {
            delay(1000) // Check every second

            val now = LocalDateTime.now()
            if (now.isAfter(monitorServerWait)) {
                monitorMessageMutex.withLock {
                    if (monitorMessageBuilder.isNotEmpty()) {
                        try {
                            // Create or reconnect monitor client if needed
                            if (monitorClient == null || !monitorClient!!.isConnected) {
                                monitorClient = Socket()
                                monitorClient!!.connect(
                                    InetSocketAddress(
                                        configuration.monitorAddress,
                                        configuration.monitorPort
                                    )
                                )
                            }

                            // Send message
                            val message = monitorMessageBuilder.toString()
                            monitorMessageBuilder.clear()

                            withContext(Dispatchers.IO) {
                                val bytes =
                                    message.toByteArray(Charset.forName(configuration.getEncoding()))
                                monitorClient?.getOutputStream()?.write(bytes)
                                monitorClient?.getOutputStream()?.flush()
                            }

                            // Add statistics and connection list
                            addToMonitorMessage(getStatistics())
                            addToMonitorMessage(getConnectionListString())

                            // Send updated message
                            withContext(Dispatchers.IO) {
                                val bytes = monitorMessageBuilder.toString().toByteArray(
                                    Charset.forName(configuration.getEncoding())
                                )
                                monitorClient?.getOutputStream()?.write(bytes)
                                monitorClient?.getOutputStream()?.flush()
                            }
                        } catch (e: Exception) {
                            try {
                                monitorClient?.close()
                                monitorClient = null
                                monitorServerWait = LocalDateTime.now().plusSeconds(20)
                            } catch (e2: Exception) {
                                // Ignore errors during cleanup
                            }
                        }
                    }
                }
            }

            // Clear monitor message buffer periodically
            monitorMessageMutex.withLock {
                monitorMessageBuilder.clear()
            }
        }
    }

    /**
     * Add a message to the monitor buffer
     */
    private suspend fun addToMonitorMessage(message: String) {
        if (monitorJob == null) {
            return
        }

        monitorMessageMutex.withLock {
            monitorMessageBuilder.append("<M>")
            monitorMessageBuilder.append(message)
            monitorMessageBuilder.append("<M>")
        }
    }

    /**
     * Handle client error
     */
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun clientError(client: GatewayClient) {
        try {
            if (started.load()) {
                writeLog(client, client.lastError.toString())
            }

            // Handle unauthorized access exception
            if (client.lastError is UnauthorizedAccessException) {
                firstRs232Handler?.let {
                    if (it.isOpen()) {
                        it.close()
                        firstRs232Handler = null
                    }
                }

                secondRs232Handler?.let {
                    if (it.isOpen()) {
                        it.close()
                        secondRs232Handler = null
                    }
                }
            }

            // Handle serial connection types
            if (configuration.serverConnectionType == ConnectionType.COM ||
                configuration.serverConnectionType == ConnectionType.DIAL_UP
            ) {

                if (!started.load()) {
                    firstRs232Handler?.let {
                        if (it.isOpen()) {
                            it.close()
                            firstRs232Handler = null
                        }
                    }

                    removeClient(client)
                    writeLog(client, "===============CONNECTION TERMINATED===============")
                }
                return
            }

            // Handle TCP/IP connections
            var mustCloseConnection = true
            val verification = client.lastError as? VerificationException

            // Handle NCC process
            if (client.nccProcess?.isActive() == true) {
                if (configuration.terminateWhenError || mustCloseConnection || !started.load()) {
                    client.nccProcess?.close()
                }
            }
            // Handle second connection
            else if (client.secondConnection != null) {
                val secondConnectionAlive = client.checkConnectionAlive(client.secondConnection) &&
                        client.secondConnection!!.isConnected == true

                val shouldDisconnect = (secondConnectionAlive &&
                        (configuration.advancedOptions?.actionWhenDisconnect == ActionWhenDisconnect.DisconnectFromDestOnly ||
                                configuration.advancedOptions?.actionWhenDisconnect == ActionWhenDisconnect.DisconnectFromBoth) ||
                        !secondConnectionAlive && configuration.terminateWhenError ||
                        mustCloseConnection ||
                        !started.load())

                if (shouldDisconnect) {
                    // Close second connection
                    try {
                        client.secondConnection?.close()
                        client.secondConnection = null
                    } catch (e: Exception) {
                        // Ignore errors during close
                    }
                }
            }

            // Handle first connection
            if (client.firstConnection != null) {
                client.lastError?.let { client.sendErrorCode(it) }

                if (verification != null) {
                    mustCloseConnection =
                        verification.error == VerificationError.DISCONNECTED_FROM_SOURCE

                    // Special handling for some error types in synchronous mode
                    if ((verification.error == VerificationError.NOT_SEND_LOGON_BEFORE ||
                                verification.error == VerificationError.WRONG_MAC) &&
                        configuration.transmissionType == TransmissionType.SYNCHRONOUS
                    ) {
                        client.processGateway()
                        return
                    }
                }

                val firstConnectionAlive = client.checkConnectionAlive(client.firstConnection!!)

                val shouldDisconnect = (firstConnectionAlive &&
                        (configuration.advancedOptions?.actionWhenDisconnect == ActionWhenDisconnect.DisconnectFromSourceOnly ||
                                configuration.advancedOptions?.actionWhenDisconnect == ActionWhenDisconnect.DisconnectFromBoth) ||
                        !firstConnectionAlive && configuration.terminateWhenError ||
                        mustCloseConnection ||
                        !started.load())

                if (shouldDisconnect) {
                    // Close first connection
                    try {
                        client.firstConnection?.close()
                        client.firstConnection = null
                    } catch (e: Exception) {
                        // Ignore errors during close
                    }
                }
            }

            // Clean up if first connection is gone
            if (client.firstConnection == null) {
                removeClient(client)
                writeLog(client, "===============CONNECTION TERMINATED===============")
            }
            // Handle special cases
            else if (client.secondConnection != null &&
                configuration.transmissionType == TransmissionType.SYNCHRONOUS
            ) {
                client.processGateway()
            } else if (client.secondConnection == null) {
                writeLog(client, "CONNECTION TO DESTINATION WILL ESTABLISH ONCE RECEIVING REQUEST")

                if (configuration.transmissionType == TransmissionType.SYNCHRONOUS) {
                    client.processGateway()
                }
            }
        } catch (e: Exception) {
            // Handle fatal errors
            removeClient(client)

            if (e !is VerificationException) {
                writeLog(client, "FATAL ERROR $e")
                writeLog(client, "===============CONNECTION TERMINATED===============")
            }
        }
    }

    /**
     * Event handlers
     */
    override fun showError(item: @Composable () -> Unit) {
        resultDialogInterface?.onError(item)
    }

    override fun showSuccess(item: @Composable () -> Unit) {
        resultDialogInterface?.onSuccess(item)
    }

    override fun showWarning(item: @Composable () -> Unit) {
        TODO("Not yet implemented")
    }

    override fun setShowErrorListener(resultDialogInterface: ResultDialogInterface) {
        this.resultDialogInterface = resultDialogInterface
    }

    override fun onSentToDest(callback: (Iso8583Data?) -> Unit) {
        this.sentToDest = callback
    }

    override fun onSentToSource(callback: (Iso8583Data?) -> Unit) {
        this.sentToSource = callback
    }


    override fun beforeReceive(callback:  (GatewayClient) -> Unit) {
        beforeReceiveCallbacks = callback
    }

    override fun beforeWriteLog(callback: (String) -> Unit) {
        beforeWriteLogCallbacks = callback
    }

    override fun onAdminResponse(callback: suspend (GatewayClient, ByteArray) -> ByteArray?) {
        adminResponseCallbacks = callback
    }

    override fun onReceiveFromSource(callback: (Iso8583Data?) -> Unit) {
        receivedFromSource = callback
    }

    override fun onReceiveFromDest(callback: (Iso8583Data?) -> Unit) {
        receivedFromDest = callback
    }

    suspend fun sendToSecondConnection(data: Iso8583Data) {
        try {
            val client = createClient()
            client.sendMessageToSecondConnection(data)
        }catch (e: Exception){
            writeLog("Error sending data to second connection: ${e.message}")
        }
    }

    companion object {
        /**
         * About information
         */
        val aboutUs: String
            get() = "DTT Solution, service@dttsolution.com"

        /**
         * Check license
         */
        fun checkLicense(): Boolean {
            return false
        }
    }
}
