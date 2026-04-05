package `in`.aicortex.iso8583studio.domain.service.hsmCommandService

import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.*
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.SSLTLSVersion
import `in`.aicortex.iso8583studio.ui.screens.hsmCommand.ThalesErrorCodes
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.createLogEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.*

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class HsmCommandResult(
    val success: Boolean,
    val rawRequest: ByteArray,
    val rawResponse: ByteArray,
    val formattedRequest: String,
    val formattedResponse: String,
    val elapsedMs: Long,
    val errorMessage: String? = null,
)

data class LoadTestStats(
    val totalSent: Long = 0,
    val totalReceived: Long = 0,
    val successCount: Long = 0,
    val failureCount: Long = 0,
    val avgResponseTimeMs: Double = 0.0,
    val minResponseTimeMs: Long = Long.MAX_VALUE,
    val maxResponseTimeMs: Long = 0,
    val currentTps: Double = 0.0,
    val elapsedSeconds: Long = 0,
    val running: Boolean = false,
)

class HsmCommandClientService(val config: HsmCommandConfig) {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var socket: Socket? = null
    private var sslSocket: SSLSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    var beforeWriteLog: ((LogEntry) -> Unit)? = null

    private var loadTestJob: Job? = null
    private val _loadTestStats = MutableStateFlow(LoadTestStats())
    val loadTestStats: StateFlow<LoadTestStats> = _loadTestStats.asStateFlow()

    fun writeLog(log: LogEntry) {
        beforeWriteLog?.invoke(log)
    }

    suspend fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED) return
        _connectionState.value = ConnectionState.CONNECTING
        writeLog(createLogEntry(LogType.CONNECTION, "Connecting to ${config.ipAddress}:${config.port}..."))

        try {
            withContext(Dispatchers.IO) {
                val rawSocket = Socket()
                rawSocket.soTimeout = config.timeout * 1000
                rawSocket.connect(InetSocketAddress(config.ipAddress, config.port), config.timeout * 1000)

                if (config.sslConfig.enabled) {
                    val sslCtx = buildSslContext()
                    val factory = sslCtx.socketFactory
                    val wrapped = factory.createSocket(rawSocket, config.ipAddress, config.port, true) as SSLSocket
                    wrapped.useClientMode = true
                    wrapped.enabledProtocols = resolveProtocols()
                    wrapped.startHandshake()
                    sslSocket = wrapped
                    outputStream = wrapped.outputStream
                    inputStream = wrapped.inputStream
                    writeLog(createLogEntry(LogType.INFO, "SSL/TLS handshake complete (${wrapped.session.protocol})"))
                } else {
                    socket = rawSocket
                    outputStream = rawSocket.outputStream
                    inputStream = rawSocket.inputStream
                }
            }
            _connectionState.value = ConnectionState.CONNECTED
            writeLog(createLogEntry(LogType.CONNECTION, "Connected to ${config.ipAddress}:${config.port}"))
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            writeLog(createLogEntry(LogType.ERROR, "Connection failed: ${e.message}"))
            throw e
        }
    }

    suspend fun disconnect() {
        stopLoadTest()
        try {
            withContext(Dispatchers.IO) {
                sslSocket?.close()
                socket?.close()
            }
        } catch (_: Exception) { }
        sslSocket = null
        socket = null
        outputStream = null
        inputStream = null
        _connectionState.value = ConnectionState.DISCONNECTED
        writeLog(createLogEntry(LogType.CONNECTION, "Disconnected"))
    }

    private fun isConnectionLost(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return e is java.io.EOFException ||
                e is java.net.SocketException ||
                msg.contains("broken pipe") ||
                msg.contains("connection reset") ||
                msg.contains("connection closed") ||
                msg.contains("socket closed") ||
                msg.contains("unexpected eof")
    }

    private fun cleanupSocket() {
        try { sslSocket?.close() } catch (_: Exception) { }
        try { socket?.close() } catch (_: Exception) { }
        sslSocket = null
        socket = null
        outputStream = null
        inputStream = null
    }

    suspend fun sendCommand(commandText: String): HsmCommandResult {
        val os = outputStream ?: throw IllegalStateException("Not connected")
        val iStream = inputStream ?: throw IllegalStateException("Not connected")

        val commandBytes = commandText.toByteArray(Charsets.US_ASCII)
        val frame = buildFrame(commandBytes)

        val start = System.currentTimeMillis()
        return withContext(Dispatchers.IO) {
            try {
                os.write(frame)
                os.flush()
                writeLog(createLogEntry(LogType.MESSAGE, "SENT ${frame.size} bytes"))

                val responseBytes = readResponse(iStream)
                val elapsed = System.currentTimeMillis() - start

                val responsePayload = extractPayload(responseBytes)
                writeLog(createLogEntry(LogType.MESSAGE, "RECV ${responseBytes.size} bytes in ${elapsed}ms"))

                val headerLen = config.headerValue.length
                val bodyForCheck = if (headerLen > 0 && responsePayload.size > headerLen)
                    responsePayload.copyOfRange(headerLen, responsePayload.size)
                else responsePayload

                val hsmErrorCode = if (bodyForCheck.size >= 4)
                    String(bodyForCheck, 2, 2, Charsets.US_ASCII) else null
                val hsmSuccess = hsmErrorCode == null || ThalesErrorCodes.isSuccess(hsmErrorCode)
                val hsmErrorMsg = if (!hsmSuccess && hsmErrorCode != null)
                    "HSM Error $hsmErrorCode: ${ThalesErrorCodes.getDescription(hsmErrorCode)}" else null

                HsmCommandResult(
                    success = hsmSuccess,
                    rawRequest = frame,
                    rawResponse = responseBytes,
                    formattedRequest = formatCommand(commandBytes),
                    formattedResponse = formatResponse(responsePayload),
                    elapsedMs = elapsed,
                    errorMessage = hsmErrorMsg,
                )
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - start
                writeLog(createLogEntry(LogType.ERROR, "Command failed: ${e.message}"))
                if (isConnectionLost(e)) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    writeLog(createLogEntry(LogType.CONNECTION, "Connection lost: ${e.message}"))
                    cleanupSocket()
                }
                HsmCommandResult(
                    success = false,
                    rawRequest = frame,
                    rawResponse = ByteArray(0),
                    formattedRequest = formatCommand(commandBytes),
                    formattedResponse = "",
                    elapsedMs = elapsed,
                    errorMessage = e.message,
                )
            }
        }
    }

    fun startLoadTest(
        commandText: String,
        scope: CoroutineScope,
        durationSeconds: Int = config.loadTestDurationSeconds,
        concurrency: Int = config.loadTestConcurrentConnections,
        commandsPerSecond: Int = config.loadTestCommandsPerSecond,
    ) {
        if (loadTestJob?.isActive == true) return
        _loadTestStats.value = LoadTestStats(running = true)

        loadTestJob = scope.launch(Dispatchers.IO) {
            val totalSent = AtomicLong(0)
            val totalRecv = AtomicLong(0)
            val successCnt = AtomicLong(0)
            val failureCnt = AtomicLong(0)
            val totalTime = AtomicLong(0)
            val minTime = AtomicLong(Long.MAX_VALUE)
            val maxTime = AtomicLong(0)
            val startTime = System.currentTimeMillis()

            val delayBetweenCommands = if (commandsPerSecond > 0)
                (1000L / commandsPerSecond) else 100L

            val workers = (1..concurrency).map {
                launch {
                    try {
                        while (isActive) {
                            val elapsed = (System.currentTimeMillis() - startTime) / 1000
                            if (elapsed >= durationSeconds) break

                            try {
                                totalSent.incrementAndGet()
                                val result = sendCommand(commandText)
                                totalRecv.incrementAndGet()
                                if (result.success) successCnt.incrementAndGet() else failureCnt.incrementAndGet()
                                totalTime.addAndGet(result.elapsedMs)
                                minTime.updateAndGet { t -> minOf(t, result.elapsedMs) }
                                maxTime.updateAndGet { t -> maxOf(t, result.elapsedMs) }
                            } catch (_: Exception) {
                                failureCnt.incrementAndGet()
                            }

                            delay(delayBetweenCommands)

                            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000).coerceAtLeast(1)
                            _loadTestStats.value = LoadTestStats(
                                totalSent = totalSent.get(),
                                totalReceived = totalRecv.get(),
                                successCount = successCnt.get(),
                                failureCount = failureCnt.get(),
                                avgResponseTimeMs = if (totalRecv.get() > 0) totalTime.get().toDouble() / totalRecv.get() else 0.0,
                                minResponseTimeMs = minTime.get(),
                                maxResponseTimeMs = maxTime.get(),
                                currentTps = totalRecv.get().toDouble() / elapsedSec,
                                elapsedSeconds = elapsedSec,
                                running = true,
                            )
                        }
                    } catch (_: CancellationException) { }
                }
            }
            workers.joinAll()
            _loadTestStats.value = _loadTestStats.value.copy(running = false)
            writeLog(createLogEntry(LogType.INFO, "Load test completed: ${successCnt.get()} success, ${failureCnt.get()} failures"))
        }
    }

    /**
     * Start a load test that round-robins through a list of commands.
     * Each worker cycles through commands sequentially.
     */
    fun startLoadTestMulti(
        commands: List<String>,
        scope: CoroutineScope,
        durationSeconds: Int = config.loadTestDurationSeconds,
        concurrency: Int = config.loadTestConcurrentConnections,
        commandsPerSecond: Int = config.loadTestCommandsPerSecond,
    ) {
        if (commands.isEmpty()) return
        if (commands.size == 1) { startLoadTest(commands.first(), scope, durationSeconds, concurrency, commandsPerSecond); return }
        if (loadTestJob?.isActive == true) return
        _loadTestStats.value = LoadTestStats(running = true)

        loadTestJob = scope.launch(Dispatchers.IO) {
            val totalSent = AtomicLong(0)
            val totalRecv = AtomicLong(0)
            val successCnt = AtomicLong(0)
            val failureCnt = AtomicLong(0)
            val totalTime = AtomicLong(0)
            val minTime = AtomicLong(Long.MAX_VALUE)
            val maxTime = AtomicLong(0)
            val startTime = System.currentTimeMillis()

            val delayBetweenCommands = if (commandsPerSecond > 0)
                (1000L / commandsPerSecond) else 100L

            val workers = (1..concurrency).map { workerIdx ->
                launch {
                    var cmdIndex = workerIdx % commands.size
                    try {
                        while (isActive) {
                            val elapsed = (System.currentTimeMillis() - startTime) / 1000
                            if (elapsed >= durationSeconds) break

                            val cmd = commands[cmdIndex % commands.size]
                            cmdIndex++

                            try {
                                totalSent.incrementAndGet()
                                val result = sendCommand(cmd)
                                totalRecv.incrementAndGet()
                                if (result.success) successCnt.incrementAndGet() else failureCnt.incrementAndGet()
                                totalTime.addAndGet(result.elapsedMs)
                                minTime.updateAndGet { t -> minOf(t, result.elapsedMs) }
                                maxTime.updateAndGet { t -> maxOf(t, result.elapsedMs) }
                            } catch (_: Exception) {
                                failureCnt.incrementAndGet()
                            }

                            delay(delayBetweenCommands)

                            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000).coerceAtLeast(1)
                            _loadTestStats.value = LoadTestStats(
                                totalSent = totalSent.get(),
                                totalReceived = totalRecv.get(),
                                successCount = successCnt.get(),
                                failureCount = failureCnt.get(),
                                avgResponseTimeMs = if (totalRecv.get() > 0) totalTime.get().toDouble() / totalRecv.get() else 0.0,
                                minResponseTimeMs = minTime.get(),
                                maxResponseTimeMs = maxTime.get(),
                                currentTps = totalRecv.get().toDouble() / elapsedSec,
                                elapsedSeconds = elapsedSec,
                                running = true,
                            )
                        }
                    } catch (_: CancellationException) { }
                }
            }
            workers.joinAll()
            _loadTestStats.value = _loadTestStats.value.copy(running = false)
            writeLog(createLogEntry(LogType.INFO, "Load test completed: ${successCnt.get()} success, ${failureCnt.get()} failures (${commands.size} commands)"))
        }
    }

    fun stopLoadTest() {
        loadTestJob?.cancel()
        loadTestJob = null
        _loadTestStats.value = _loadTestStats.value.copy(running = false)
    }

    // --- Frame building ---

    private fun buildFrame(payload: ByteArray): ByteArray {
        val header = if (config.headerValue.isNotBlank()) config.headerValue.toByteArray(Charsets.US_ASCII) else ByteArray(0)
        val trailer = if (config.trailerValue.isNotBlank()) config.trailerValue.toByteArray(Charsets.US_ASCII) else ByteArray(0)
        val body = header + payload + trailer

        return when (config.hsmVendor.headerFormat) {
            HeaderFormat.TWO_BYTE_LENGTH -> {
                if (config.tcpLengthHeaderEnabled) {
                    val len = body.size
                    byteArrayOf(((len shr 8) and 0xFF).toByte(), (len and 0xFF).toByte()) + body
                } else body
            }
            HeaderFormat.FOUR_BYTE_ASCII_LENGTH -> {
                val lenStr = String.format("%04d", body.size)
                lenStr.toByteArray(Charsets.US_ASCII) + body
            }
            HeaderFormat.STX_ETX -> {
                byteArrayOf(0x02) + body + byteArrayOf(0x03)
            }
            HeaderFormat.NONE, HeaderFormat.CUSTOM -> body
        }
    }

    private fun readResponse(stream: InputStream): ByteArray {
        return when (config.hsmVendor.headerFormat) {
            HeaderFormat.TWO_BYTE_LENGTH -> {
                if (config.tcpLengthHeaderEnabled) {
                    val lenBuf = ByteArray(2)
                    readFully(stream, lenBuf)
                    val len = ((lenBuf[0].toInt() and 0xFF) shl 8) or (lenBuf[1].toInt() and 0xFF)
                    val payload = ByteArray(len)
                    readFully(stream, payload)
                    lenBuf + payload
                } else readAvailable(stream)
            }
            HeaderFormat.FOUR_BYTE_ASCII_LENGTH -> {
                val lenBuf = ByteArray(4)
                readFully(stream, lenBuf)
                val len = String(lenBuf, Charsets.US_ASCII).trim().toIntOrNull() ?: 0
                val payload = ByteArray(len)
                readFully(stream, payload)
                lenBuf + payload
            }
            HeaderFormat.STX_ETX -> {
                val buf = mutableListOf<Byte>()
                var b = stream.read()
                if (b == 0x02) b = stream.read()
                while (b != -1 && b != 0x03) {
                    buf.add(b.toByte())
                    b = stream.read()
                }
                buf.toByteArray()
            }
            HeaderFormat.NONE, HeaderFormat.CUSTOM -> readAvailable(stream)
        }
    }

    private fun extractPayload(raw: ByteArray): ByteArray {
        return when (config.hsmVendor.headerFormat) {
            HeaderFormat.TWO_BYTE_LENGTH -> if (config.tcpLengthHeaderEnabled && raw.size > 2) raw.copyOfRange(2, raw.size) else raw
            HeaderFormat.FOUR_BYTE_ASCII_LENGTH -> if (raw.size > 4) raw.copyOfRange(4, raw.size) else raw
            else -> raw
        }
    }

    private fun readFully(stream: InputStream, buf: ByteArray) {
        var off = 0
        while (off < buf.size) {
            val n = stream.read(buf, off, buf.size - off)
            if (n < 0) throw java.io.IOException("Unexpected EOF")
            off += n
        }
    }

    private fun readAvailable(stream: InputStream): ByteArray {
        val buf = ByteArray(8192)
        val n = stream.read(buf)
        return if (n > 0) buf.copyOf(n) else ByteArray(0)
    }

    // --- Formatting ---

    /**
     * Recovers the ASCII command bytes from the outbound wire [frame] (inverse of [buildFrame]).
     * Used so the Console "REQUEST" view only ever reflects what was sent, never the response.
     */
    fun extractOutboundCommandBytes(frame: ByteArray): ByteArray {
        if (frame.isEmpty()) return frame
        var rest = frame
        when (config.hsmVendor.headerFormat) {
            HeaderFormat.TWO_BYTE_LENGTH -> {
                if (config.tcpLengthHeaderEnabled && rest.size >= 2) {
                    rest = rest.copyOfRange(2, rest.size)
                }
            }
            HeaderFormat.FOUR_BYTE_ASCII_LENGTH -> {
                if (rest.size >= 4) rest = rest.copyOfRange(4, rest.size)
            }
            HeaderFormat.STX_ETX -> {
                if (rest.size >= 2 && rest[0] == 0x02.toByte() && rest[rest.lastIndex] == 0x03.toByte()) {
                    rest = rest.copyOfRange(1, rest.lastIndex)
                }
            }
            HeaderFormat.NONE, HeaderFormat.CUSTOM -> { }
        }
        val header = if (config.headerValue.isNotBlank()) config.headerValue.toByteArray(Charsets.US_ASCII) else ByteArray(0)
        val trailer = if (config.trailerValue.isNotBlank()) config.trailerValue.toByteArray(Charsets.US_ASCII) else ByteArray(0)
        var payload = rest
        if (header.isNotEmpty() && payload.size >= header.size) {
            val head = payload.copyOfRange(0, header.size)
            if (head.contentEquals(header)) {
                payload = payload.copyOfRange(header.size, payload.size)
            }
        }
        if (trailer.isNotEmpty() && payload.size >= trailer.size) {
            val tail = payload.copyOfRange(payload.size - trailer.size, payload.size)
            if (tail.contentEquals(trailer)) {
                payload = payload.copyOfRange(0, payload.size - trailer.size)
            }
        }
        return payload
    }

    fun formatOutboundCommandForDisplay(frame: ByteArray): String {
        if (frame.isEmpty()) return ""
        return formatCommand(extractOutboundCommandBytes(frame))
    }

    fun formatCommand(payload: ByteArray): String {
        if (payload.isEmpty()) return "(empty)"
        val headerLen = config.headerValue.length
        val sb = StringBuilder()

        if (headerLen > 0 && payload.size > headerLen) {
            sb.appendLine("Header: ${String(payload, 0, headerLen, Charsets.US_ASCII)}")
            val cmdBody = payload.copyOfRange(headerLen, payload.size)
            if (cmdBody.size >= 2) {
                val cmdCode = String(cmdBody, 0, 2, Charsets.US_ASCII)
                sb.appendLine("Command Code: $cmdCode")
                if (cmdBody.size > 2) {
                    sb.appendLine("Data: ${String(cmdBody, 2, cmdBody.size - 2, Charsets.US_ASCII)}")
                }
            } else {
                sb.appendLine("Data: ${String(cmdBody, Charsets.US_ASCII)}")
            }
        } else if (payload.size >= 2) {
            val cmdCode = String(payload, 0, 2, Charsets.US_ASCII)
            sb.appendLine("Command Code: $cmdCode")
            if (payload.size > 2) {
                sb.appendLine("Data: ${String(payload, 2, payload.size - 2, Charsets.US_ASCII)}")
            }
        } else {
            sb.appendLine("Data: ${String(payload, Charsets.US_ASCII)}")
        }
        return sb.toString().trimEnd()
    }

    fun formatResponse(payload: ByteArray): String {
        if (payload.isEmpty()) return "(empty)"
        val headerLen = config.headerValue.length
        val sb = StringBuilder()

        val body = if (headerLen > 0 && payload.size > headerLen) {
            sb.appendLine("Header: ${String(payload, 0, headerLen, Charsets.US_ASCII)}")
            payload.copyOfRange(headerLen, payload.size)
        } else payload

        if (body.size >= 2) {
            val respCode = String(body, 0, 2, Charsets.US_ASCII)
            sb.appendLine("Response Code: $respCode")
            if (body.size >= 4) {
                val errorCode = String(body, 2, 2, Charsets.US_ASCII)
                val errorDesc = ThalesErrorCodes.getDescription(errorCode)
                sb.appendLine("Error Code: $errorCode ($errorDesc)")
                if (body.size > 4) {
                    sb.appendLine("Data: ${String(body, 4, body.size - 4, Charsets.US_ASCII)}")
                }
            } else if (body.size > 2) {
                sb.appendLine("Data: ${String(body, 2, body.size - 2, Charsets.US_ASCII)}")
            }
        } else {
            sb.appendLine("Data: ${String(body, Charsets.US_ASCII)}")
        }
        return sb.toString().trimEnd()
    }

    // --- SSL ---

    private fun buildSslContext(): SSLContext {
        val ctx = SSLContext.getInstance("TLS")
        val keyManagers = loadClientKeyManagers()
        val trustManagers = buildTrustManagers()
        ctx.init(keyManagers, trustManagers, null)
        return ctx
    }

    private fun loadClientKeyManagers(): Array<KeyManager>? {
        val certPath = config.sslConfig.clientPublicCertPath
        if (certPath.isBlank()) return null
        val ks = KeyStore.getInstance(config.sslConfig.certificateType.name)
        val password = config.sslConfig.keyStorePassword.toCharArray()
        FileInputStream(certPath).use { ks.load(it, password) }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, password)
        return kmf.keyManagers
    }

    private fun buildTrustManagers(): Array<TrustManager>? {
        return when (config.sslConfig.certificateVerification) {
            CertVerificationMethod.NONE, CertVerificationMethod.TRUST_ALL -> {
                arrayOf(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                })
            }
            CertVerificationMethod.CA_SIGNED -> null
            CertVerificationMethod.CUSTOM_CA -> {
                val caPath = config.sslConfig.caAuthorityPath
                if (caPath.isBlank()) return null
                val ts = KeyStore.getInstance(KeyStore.getDefaultType())
                ts.load(null, null)
                val cf = java.security.cert.CertificateFactory.getInstance("X.509")
                FileInputStream(caPath).use { ts.setCertificateEntry("custom-ca", cf.generateCertificate(it)) }
                val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                tmf.init(ts)
                tmf.trustManagers
            }
        }
    }

    private fun resolveProtocols(): Array<String> {
        return when (config.sslConfig.tlsVersion) {
            SSLTLSVersion.TLS_1_3 -> arrayOf("TLSv1.3")
            SSLTLSVersion.TLS_1_2 -> arrayOf("TLSv1.2")
            SSLTLSVersion.TLS_1_1 -> arrayOf("TLSv1.1")
            SSLTLSVersion.TLS_1_0 -> arrayOf("TLSv1")
            SSLTLSVersion.SSL_3_0 -> arrayOf("SSLv3")
        }
    }

    companion object {
        fun hexToBytes(hex: String): ByteArray {
            val clean = hex.replace("\\s".toRegex(), "")
            if (clean.isEmpty()) return ByteArray(0)
            return ByteArray(clean.length / 2) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }

        fun bytesToHex(bytes: ByteArray): String =
            bytes.joinToString("") { "%02X".format(it) }
    }
}
