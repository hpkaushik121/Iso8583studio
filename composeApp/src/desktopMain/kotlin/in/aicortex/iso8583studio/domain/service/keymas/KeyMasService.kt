package `in`.aicortex.iso8583studio.domain.service.keymas

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * KeyMAS — Key Management as a Service
 *
 * Exposes a REST API (matching the BharatPe EDC KeyMAS API contract) and internally
 * connects to the HSM Simulator via TCP/IP to execute PayShield 10K commands.
 *
 * Architecture:
 *   REST Client → [KeyMAS HTTP Server] → [TCP/IP] → [HSM Simulator]
 *
 * Online commands (accessible via REST API):
 *   POST /api/v1/pin/translate      → G0 (DUKPT PIN translation BDK→ZPK)
 *   POST /api/v1/mac/generate       → M6 (Generate MAC)
 *   POST /api/v1/mac/validate       → M8 (Verify MAC)
 *   POST /api/v1/decrypt/card-data  → M2 (Decrypt DUKPT card data)
 *   POST /api/v1/data/translate     → M4 (Translate data BDK→ZPK)
 *   POST /api/v1/dukpt/derive-ipek  → A0 mode A (Derive IPEK from BDK)
 *   POST /api/v1/encrypt/for-acquirer → M0 (Encrypt data)
 *   GET  /api/v1/health/hsm         → NC (HSM diagnostic)
 */
class KeyMasService(
    private val hsmHost: String = "localhost",
    private val hsmPort: Int = 1500,
    private val restApiPort: Int = 8080,
    private val lmkId: String = "00",
    private val msgHeader: String = "KMS1"
) {
    private val jackson = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private var httpServer: HttpServer? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(KeyMasState())
    val state = _state.asStateFlow()

    var onLog: (String) -> Unit = {}
    var onRequestLog: (String) -> Unit = {}
    var onResponseLog: (String) -> Unit = {}

    // ====================================================================================================
    // Lifecycle
    // ====================================================================================================

    fun start() {
        if (_state.value.running) return
        httpServer = HttpServer.create(InetSocketAddress(restApiPort), 0)
        registerEndpoints()
        httpServer!!.executor = java.util.concurrent.Executors.newFixedThreadPool(10)
        httpServer!!.start()
        _state.value = _state.value.copy(running = true, startedAt = LocalDateTime.now())
        log("KeyMAS REST API started on port $restApiPort — connecting to HSM at $hsmHost:$hsmPort")
    }

    fun stop() {
        httpServer?.stop(0)
        httpServer = null
        _state.value = _state.value.copy(running = false, startedAt = null)
        log("KeyMAS REST API stopped")
    }

    // ====================================================================================================
    // Endpoint Registration
    // ====================================================================================================

    private fun registerEndpoints() {
        val server = httpServer!!

        // Health
        server.createContext("/api/v1/health/hsm") { exchange ->
            handle(exchange, "GET") { _ -> handleHealthCheck() }
        }

        // PIN Translation (DUKPT → ZPK) — uses G0 command
        server.createContext("/api/v1/pin/translate") { exchange ->
            handle(exchange, "POST") { body -> handlePinTranslate(body) }
        }

        // MAC Generate — uses M6 command
        server.createContext("/api/v1/mac/generate") { exchange ->
            handle(exchange, "POST") { body -> handleMacGenerate(body) }
        }

        // MAC Validate — uses M8 command
        server.createContext("/api/v1/mac/validate") { exchange ->
            handle(exchange, "POST") { body -> handleMacValidate(body) }
        }

        // Decrypt DUKPT card data — uses M2 command
        server.createContext("/api/v1/decrypt/card-data") { exchange ->
            handle(exchange, "POST") { body -> handleDecryptCardData(body) }
        }

        // Translate DUKPT data to acquirer zone — uses M4 command
        server.createContext("/api/v1/data/translate") { exchange ->
            handle(exchange, "POST") { body -> handleDataTranslate(body) }
        }

        // Derive IPEK from BDK — uses A0 command (mode A)
        server.createContext("/api/v1/dukpt/derive-ipek") { exchange ->
            handle(exchange, "POST") { body -> handleDeriveIpek(body) }
        }

        // Encrypt data for acquirer — uses M0 command
        server.createContext("/api/v1/encrypt/for-acquirer") { exchange ->
            handle(exchange, "POST") { body -> handleEncryptForAcquirer(body) }
        }

        // Terminal key provisioning — uses A0 command (generate BDKs + derive IPEKs)
        server.createContext("/api/v1/terminal/provision-keys") { exchange ->
            handle(exchange, "POST") { body -> handleTerminalProvision(body) }
        }
    }

    // ====================================================================================================
    // HTTP helpers
    // ====================================================================================================

    private fun handle(exchange: HttpExchange, method: String, block: (Map<String, Any?>) -> Any) {
        try {
            if (exchange.requestMethod != method) {
                exchange.sendResponseHeaders(405, -1)
                return
            }

            val body: Map<String, Any?> = if (method == "POST") {
                val json = exchange.requestBody.bufferedReader().readText()
                onRequestLog(json)
                @Suppress("UNCHECKED_CAST")
                jackson.readValue(json, Map::class.java) as Map<String, Any?>
            } else emptyMap()

            val result = block(body)
            val json = jackson.writeValueAsString(result)
            onResponseLog(json)

            val bytes = json.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }

            incrementRequestCount()

        } catch (e: Exception) {
            log("ERROR in ${exchange.requestURI}: ${e.message}")
            val error = jackson.writeValueAsString(mapOf(
                "errorCode" to "INTERNAL_ERROR",
                "message" to (e.message ?: "Unknown error"),
                "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ))
            val bytes = error.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            try {
                exchange.sendResponseHeaders(500, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (_: IOException) {}
        }
    }

    // ====================================================================================================
    // Handler Implementations
    // ====================================================================================================

    private fun handleHealthCheck(): Map<String, Any> {
        return try {
            val response = runBlocking { sendHsmCommand("${msgHeader}NC") }
            val healthy = response.contains("ND00")
            mapOf(
                "status" to if (healthy) "UP" else "DEGRADED",
                "hsmResponse" to response,
                "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            mapOf("status" to "DOWN", "error" to (e.message ?: "HSM unreachable"))
        }
    }

    /**
     * POST /api/v1/pin/translate
     * Body: { bptid, acquirerCode, pinBlock (16H), ksn (20H), pan, destPinBlockFormat, correlationId }
     *
     * Maps to G0 command:
     *   G0 ~ [BDK_SCHEME+BDK] [ZPK_SCHEME+ZPK] [KSN_DESC] [KSN] [PIN_BLOCK] [SRC_FMT] [DST_FMT] [ACCOUNT]
     */
    private fun handlePinTranslate(body: Map<String, Any?>): Map<String, Any> {
        val bptid = body["bptid"]?.toString() ?: error("bptid required")
        val acquirerCode = body["acquirerCode"]?.toString() ?: error("acquirerCode required")
        val pinBlock = body["pinBlock"]?.toString()?.uppercase() ?: error("pinBlock required (16H)")
        val ksn = body["ksn"]?.toString()?.uppercase() ?: error("ksn required (20H)")
        val pan = body["pan"]?.toString() ?: error("pan required")
        val destFmt = body["destPinBlockFormat"]?.toString() ?: "ISO_FORMAT_0"
        val correlationId = body["correlationId"]?.toString() ?: java.util.UUID.randomUUID().toString()

        require(pinBlock.length == 16) { "pinBlock must be 16 hex chars" }
        require(ksn.length == 20) { "ksn must be 20 hex chars" }

        // Look up terminal's PIN BDK and acquirer's ZPK from storage (simulated — use test values)
        val pinBdk = lookupTerminalPinBdk(bptid)
        val zpk = lookupAcquirerZpk(acquirerCode)

        // Extract 12 rightmost digits of PAN excluding check digit
        val account = pan.takeLast(13).dropLast(1)

        // Map destFmt to PayShield format code
        val dstFmtCode = when (destFmt) {
            "ISO_FORMAT_1" -> "05"
            "ISO_FORMAT_3" -> "47"
            else -> "01" // ISO_FORMAT_0
        }

        // Build G0 command
        // G0 ~ [BDK_SCHEME BDK_32H] [ZPK_SCHEME ZPK_32H] [KSN_DESC] [KSN] [PIN_BLOCK] [01] [DST] [ACCOUNT]
        val cmd = buildString {
            append(msgHeader)
            append("G0")
            append("~")               // BDK flag = Type 2
            append("U")               // BDK scheme = double-length TDES
            append(pinBdk)            // BDK under LMK (32H)
            append("U")               // ZPK scheme
            append(zpk)               // ZPK under LMK (32H)
            append("609")             // KSN descriptor (6,0,9 = 6H BDK ID, 0 sub-key, 9H device ID)
            append(ksn)               // KSN (20H)
            append(pinBlock)          // Source PIN Block (16H)
            append("01")              // Source format = ISO Format 0
            append(dstFmtCode)        // Destination format
            append(account)           // Account number (12N)
            if (lmkId != "00") append("%$lmkId")
        }

        log("PIN Translate → G0 command for bptid=$bptid acquirer=$acquirerCode")
        val response = runBlocking { sendHsmCommand(cmd) }

        // Parse G1 response: [HDR][G1][00][PIN_LEN_2N][PIN_BLOCK_16H][FMT_2N]
        return parseG0Response(response, correlationId)
    }

    /**
     * POST /api/v1/mac/generate
     * Body: { acquirerCode, data (hex), correlationId }
     *
     * Maps to M6 command:
     *   M6 [ZAK_SCHEME+ZAK] [MODE] [MSG_LEN_4H] [MSG]
     */
    private fun handleMacGenerate(body: Map<String, Any?>): Map<String, Any> {
        val acquirerCode = body["acquirerCode"]?.toString() ?: error("acquirerCode required")
        val data = body["data"]?.toString()?.uppercase() ?: error("data required")
        val correlationId = body["correlationId"]?.toString() ?: java.util.UUID.randomUUID().toString()

        val zak = lookupAcquirerZak(acquirerCode)
        val msgBytes = data.chunked(2).size
        val msgLenHex = msgBytes.toString(16).padStart(4, '0').uppercase()

        val cmd = buildString {
            append(msgHeader)
            append("M6")
            append("U")            // ZAK scheme
            append(zak)            // ZAK under LMK (32H)
            append("0")            // Mode: generate MAC on single block
            append(msgLenHex)      // Message length (4H hex = bytes)
            append(data)           // Message data (hex)
            if (lmkId != "00") append("%$lmkId")
        }

        log("MAC Generate → M6 for acquirer=$acquirerCode dataLen=${msgBytes}B")
        val response = runBlocking { sendHsmCommand(cmd) }

        return parseMacResponse(response, "generate", correlationId)
    }

    /**
     * POST /api/v1/mac/validate
     * Body: { bptid, data (hex), mac (8H), correlationId }
     *
     * Maps to M8 command:
     *   M8 [TAK_SCHEME+TAK] [MODE] [MSG_LEN_4H] [MSG] [MAC_8H]
     */
    private fun handleMacValidate(body: Map<String, Any?>): Map<String, Any> {
        val bptid = body["bptid"]?.toString() ?: error("bptid required")
        val data = body["data"]?.toString()?.uppercase() ?: error("data required")
        val mac = body["mac"]?.toString()?.uppercase() ?: error("mac required")
        val correlationId = body["correlationId"]?.toString() ?: java.util.UUID.randomUUID().toString()

        val tak = lookupTerminalMacKey(bptid)
        val msgBytes = data.chunked(2).size
        val msgLenHex = msgBytes.toString(16).padStart(4, '0').uppercase()

        val cmd = buildString {
            append(msgHeader)
            append("M8")
            append("U")            // TAK/ZAK scheme
            append(tak)            // Key under LMK (32H)
            append("0")            // Mode
            append(msgLenHex)
            append(data)
            append(mac)            // MAC to verify (16H = 8 bytes)
            if (lmkId != "00") append("%$lmkId")
        }

        log("MAC Validate → M8 for bptid=$bptid")
        val response = runBlocking { sendHsmCommand(cmd) }

        return parseMacValidateResponse(response, correlationId)
    }

    /**
     * POST /api/v1/decrypt/card-data
     * Body: { bptid, encryptedData (hex), ksn (20H), correlationId }
     *
     * Maps to M2 command (Decrypt Data using DUKPT DATA BDK):
     *   M2 [MODE_2N] [INPUT_FMT_1N] [OUTPUT_FMT_1N]
     *      [KEY_TYPE_3H] [DATA_BDK_SCHEME+BDK]
     *      [KSN_DESC] [KSN] [DATA_LEN_4H] [ENCRYPTED_DATA]
     */
    private fun handleDecryptCardData(body: Map<String, Any?>): Map<String, Any> {
        val bptid = body["bptid"]?.toString() ?: error("bptid required")
        val encryptedData = body["encryptedData"]?.toString()?.uppercase() ?: error("encryptedData required")
        val ksn = body["ksn"]?.toString()?.uppercase() ?: error("ksn required")
        val correlationId = body["correlationId"]?.toString() ?: java.util.UUID.randomUUID().toString()

        val dataBdk = lookupTerminalDataBdk(bptid)
        val dataLen = encryptedData.length / 2
        val dataLenHex = dataLen.toString(16).padStart(4, '0').uppercase()

        // M2 command format (decrypt data block)
        val cmd = buildString {
            append(msgHeader)
            append("M2")
            append("02")            // Mode 02 = DUKPT 3DES CBC decrypt
            append("2")             // Input format: 2 = CBC encrypted
            append("1")             // Output format: 1 = plain data
            append("009")           // Key type: DATA BDK (type 009 = BDK)
            append("U")             // BDK scheme = double-length TDES
            append(dataBdk)         // DATA BDK under LMK (32H)
            append("609")           // KSN descriptor
            append(ksn)             // KSN (20H)
            append(dataLenHex)      // Data length (4H)
            append(encryptedData)   // Encrypted data
            if (lmkId != "00") append("%$lmkId")
        }

        log("Decrypt Card Data → M2 for bptid=$bptid ksn=$ksn")
        val response = runBlocking { sendHsmCommand(cmd) }

        return parseM2Response(response, correlationId)
    }

    /**
     * POST /api/v1/data/translate
     * Body: { bptid, acquirerCode, ksn (20H), encryptedData (hex), correlationId }
     *
     * Maps to M4 command (Translate data from DUKPT DATA zone to acquirer ZPK zone)
     */
    private fun handleDataTranslate(body: Map<String, Any?>): Map<String, Any> {
        val bptid = body["bptid"]?.toString() ?: error("bptid required")
        val acquirerCode = body["acquirerCode"]?.toString() ?: error("acquirerCode required")
        val ksn = body["ksn"]?.toString()?.uppercase() ?: error("ksn required")
        val encryptedData = body["encryptedData"]?.toString()?.uppercase() ?: error("encryptedData required")
        val correlationId = body["correlationId"]?.toString() ?: java.util.UUID.randomUUID().toString()

        val dataBdk = lookupTerminalDataBdk(bptid)
        val zpk = lookupAcquirerZpk(acquirerCode)
        val dataLen = encryptedData.length / 2
        val dataLenHex = dataLen.toString(16).padStart(4, '0').uppercase()

        // M4 command: translate from DUKPT DATA BDK to acquirer ZPK
        val cmd = buildString {
            append(msgHeader)
            append("M4")
            append("02")            // Mode: DUKPT 3DES CBC decrypt + re-encrypt
            append("2")             // Input format: CBC
            append("1")             // Output format: ECB
            append("009")           // Source key type: DATA BDK
            append("U")             // Source BDK scheme
            append(dataBdk)         // DATA BDK under LMK
            append("001")           // Dest key type: ZPK
            append("U")             // Dest ZPK scheme
            append(zpk)             // Acquirer ZPK under LMK
            append("609")           // KSN descriptor
            append(ksn)             // KSN
            append(dataLenHex)      // Data length
            append(encryptedData)   // Encrypted data
            if (lmkId != "00") append("%$lmkId")
        }

        log("Data Translate → M4 for bptid=$bptid acquirer=$acquirerCode")
        val response = runBlocking { sendHsmCommand(cmd) }

        return parseM4Response(response, correlationId)
    }

    /**
     * POST /api/v1/dukpt/derive-ipek
     * Body: { orgTerminalId, ksn (20H), bdkReference, correlationId }
     *
     * Maps to A0 command (mode A = derive IKEY/IPEK from BDK):
     *   A0 A [KEY_TYPE_302U] [DERIVE_MODE_0] [MASTER_KEY_TYPE_2] [BDK_SCHEME+BDK] [KSN]
     */
    private fun handleDeriveIpek(body: Map<String, Any?>): Map<String, Any> {
        val orgTerminalId = body["orgTerminalId"]?.toString() ?: error("orgTerminalId required")
        val ksn = body["ksn"]?.toString()?.uppercase() ?: error("ksn required (20H)")
        val bdkRef = body["bdkReference"]?.toString() ?: ""
        val correlationId = body["correlationId"]?.toString() ?: java.util.UUID.randomUUID().toString()

        require(ksn.length == 20) { "ksn must be 20 hex chars (10 bytes)" }

        val bdk = lookupBdkByReference(bdkRef)

        // A0 command in mode A (derive IPEK from BDK)
        val cmd = buildString {
            append(msgHeader)
            append("A0")
            append("A")     // Mode A = derive IKEY/IPEK from BDK
            append("302")   // Key type 302 = IKEY/IPEK under LMK pair 14-15
            append("U")     // Output key scheme = double-length TDES
            append("0")     // Derive mode 0 = derive IKEY/IPEK from BDK
            append("2")     // DUKPT master key type = Type 2 BDK
            append("U")     // BDK scheme = double-length TDES
            append(bdk)     // BDK under LMK (32H)
            append(ksn)     // KSN without transaction counter (first 15H)
            if (lmkId != "00") append("%$lmkId")
        }

        log("Derive IPEK → A0 mode A for terminal=$orgTerminalId ksn=$ksn")
        val response = runBlocking { sendHsmCommand(cmd) }

        return parseA0Response(response, "ipek_derivation", correlationId)
    }

    /**
     * POST /api/v1/encrypt/for-acquirer
     * Body: { acquirerCode, plainData (hex), tid, correlationId }
     *
     * Maps to M0 command (Encrypt data under acquirer ZPK):
     *   M0 [ZPK_SCHEME+ZPK] [MSG_LEN_4H] [MSG]
     */
    private fun handleEncryptForAcquirer(body: Map<String, Any?>): Map<String, Any> {
        val acquirerCode = body["acquirerCode"]?.toString() ?: error("acquirerCode required")
        val plainData = body["plainData"]?.toString()?.uppercase() ?: error("plainData required")
        val tid = body["tid"]?.toString() ?: ""
        val correlationId = body["correlationId"]?.toString() ?: java.util.UUID.randomUUID().toString()

        val zpk = lookupAcquirerZpk(acquirerCode)
        val dataLen = plainData.length / 2
        val dataLenHex = dataLen.toString(16).padStart(4, '0').uppercase()

        val cmd = buildString {
            append(msgHeader)
            append("M0")
            append("U")            // ZPK scheme
            append(zpk)            // ZPK under LMK
            append(dataLenHex)
            append(plainData)
            if (lmkId != "00") append("%$lmkId")
        }

        log("Encrypt for Acquirer → M0 for acquirer=$acquirerCode")
        val response = runBlocking { sendHsmCommand(cmd) }

        return parseM0Response(response, correlationId)
    }

    /**
     * POST /api/v1/terminal/provision-keys
     * Body: { orgTerminalId, deviceSerial, acquirerTid, oemId, keyManagementType, initialKsn, requestedBy }
     *
     * Generates BDKs for PIN/MAC/DATA and derives IPEKs using A0 command
     */
    private fun handleTerminalProvision(body: Map<String, Any?>): Map<String, Any> {
        val orgTerminalId = body["orgTerminalId"]?.toString() ?: error("orgTerminalId required")
        val deviceSerial = body["deviceSerial"]?.toString() ?: error("deviceSerial required")
        val keyMgmtType = body["keyManagementType"]?.toString() ?: "PIN_ONLY"
        val initialKsn = body["initialKsn"]?.toString() ?: generateKsn(deviceSerial)
        val requestedBy = body["requestedBy"]?.toString() ?: "system"

        // Generate PIN BDK (A0 mode 0 = generate key only under LMK)
        val pinBdkCmd = "${msgHeader}A00009U%$lmkId"
        val pinBdkResponse = runBlocking { sendHsmCommand(pinBdkCmd) }
        val (pinBdk, pinBdkKcv) = parseA0KeyResponse(pinBdkResponse)

        // Derive PIN IPEK from BDK
        val pinIpekResponse = runBlocking {
            sendHsmCommand("${msgHeader}A0A302U02U${pinBdk}${initialKsn}%$lmkId")
        }
        val (pinIpek, pinIpekKcv) = parseA0KeyResponse(pinIpekResponse)

        val result = mutableMapOf<String, Any>(
            "orgTerminalId" to orgTerminalId,
            "pinIpekUnderLmk" to pinIpek,
            "pinIpekKcv" to pinIpekKcv,
            "success" to true,
            "message" to "Terminal keys provisioned successfully"
        )

        if (keyMgmtType == "PIN_DATA") {
            // Generate MAC BDK
            val macBdkCmd = "${msgHeader}A00009U%$lmkId"
            val macBdkResponse = runBlocking { sendHsmCommand(macBdkCmd) }
            val (macBdk, _) = parseA0KeyResponse(macBdkResponse)

            val macIpekResponse = runBlocking {
                sendHsmCommand("${msgHeader}A0A302U02U${macBdk}${initialKsn}%$lmkId")
            }
            val (macIpek, macIpekKcv) = parseA0KeyResponse(macIpekResponse)

            // Generate DATA BDK
            val dataBdkCmd = "${msgHeader}A00009U%$lmkId"
            val dataBdkResponse = runBlocking { sendHsmCommand(dataBdkCmd) }
            val (dataBdk, _) = parseA0KeyResponse(dataBdkResponse)

            val dataIpekResponse = runBlocking {
                sendHsmCommand("${msgHeader}A0A302U02U${dataBdk}${initialKsn}%$lmkId")
            }
            val (dataIpek, dataIpekKcv) = parseA0KeyResponse(dataIpekResponse)

            result["macIpekUnderLmk"] = macIpek
            result["macIpekKcv"] = macIpekKcv
            result["dataIpekUnderLmk"] = dataIpek
            result["dataIpekKcv"] = dataIpekKcv
        }

        log("Terminal provisioned: $orgTerminalId (serial=$deviceSerial, type=$keyMgmtType)")
        return result
    }

    // ====================================================================================================
    // HSM TCP Communication
    // ====================================================================================================

    private suspend fun sendHsmCommand(command: String): String = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket(hsmHost, hsmPort)
            socket.soTimeout = 5000

            val cmdBytes = command.toByteArray(Charsets.US_ASCII)

            // The HSM simulator reads raw bytes (no length prefix in current impl)
            socket.getOutputStream().write(cmdBytes)
            socket.getOutputStream().flush()

            // Read response
            val buffer = ByteArray(4096)
            val bytesRead = socket.getInputStream().read(buffer)
            if (bytesRead <= 0) throw IOException("HSM returned empty response")
            String(buffer, 0, bytesRead, Charsets.US_ASCII)

        } catch (e: Exception) {
            log("HSM TCP error for command '${command.take(8)}...': ${e.message}")
            throw e
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    // ====================================================================================================
    // Response Parsers
    // ====================================================================================================

    private fun parseG0Response(response: String, correlationId: String): Map<String, Any> {
        // Response: [HDR][G1][ERROR][PIN_LEN_2N][PIN_BLOCK_16H][FMT_2N]
        val hdrLen = msgHeader.length
        val respCode = if (response.length >= hdrLen + 2) response.substring(hdrLen, hdrLen + 2) else ""
        val errorCode = if (response.length >= hdrLen + 4) response.substring(hdrLen + 2, hdrLen + 4) else "99"

        return if (respCode == "G1" && errorCode == "00") {
            val data = response.substring(hdrLen + 4)
            val pinBlock = if (data.length >= 18) data.substring(2, 18) else data
            mapOf(
                "success" to true,
                "translatedPinBlock" to pinBlock,
                "correlationId" to correlationId
            )
        } else {
            mapOf(
                "success" to false,
                "errorCode" to errorCode,
                "message" to "HSM rejected G0 command with error: $errorCode",
                "correlationId" to correlationId
            )
        }
    }

    private fun parseMacResponse(response: String, operation: String, correlationId: String): Map<String, Any> {
        val hdrLen = msgHeader.length
        val errorCode = if (response.length >= hdrLen + 4) response.substring(hdrLen + 2, hdrLen + 4) else "99"

        return if (errorCode == "00") {
            val mac = if (response.length >= hdrLen + 20) response.substring(hdrLen + 4, hdrLen + 20) else ""
            mapOf("success" to true, "mac" to mac, "valid" to true, "correlationId" to correlationId)
        } else {
            mapOf("success" to false, "mac" to "", "valid" to false,
                "errorCode" to errorCode, "correlationId" to correlationId)
        }
    }

    private fun parseMacValidateResponse(response: String, correlationId: String): Map<String, Any> {
        val hdrLen = msgHeader.length
        val errorCode = if (response.length >= hdrLen + 4) response.substring(hdrLen + 2, hdrLen + 4) else "99"
        val valid = errorCode == "00"
        return mapOf("success" to valid, "valid" to valid, "mac" to "", "correlationId" to correlationId)
    }

    private fun parseM2Response(response: String, correlationId: String): Map<String, Any> {
        val hdrLen = msgHeader.length
        val errorCode = if (response.length >= hdrLen + 4) response.substring(hdrLen + 2, hdrLen + 4) else "99"

        return if (errorCode == "00") {
            val decryptedData = response.substring(hdrLen + 4)
            mapOf("success" to true, "decryptedData" to decryptedData, "correlationId" to correlationId)
        } else {
            mapOf("success" to false, "decryptedData" to "", "errorCode" to errorCode, "correlationId" to correlationId)
        }
    }

    private fun parseM4Response(response: String, correlationId: String): Map<String, Any> {
        val hdrLen = msgHeader.length
        val errorCode = if (response.length >= hdrLen + 4) response.substring(hdrLen + 2, hdrLen + 4) else "99"

        return if (errorCode == "00") {
            val translatedData = response.substring(hdrLen + 8) // skip 4H length field
            mapOf("success" to true, "encryptedDataForAcquirer" to translatedData,
                "correlationId" to correlationId, "errorMessage" to "")
        } else {
            mapOf("success" to false, "encryptedDataForAcquirer" to "",
                "errorCode" to errorCode, "correlationId" to correlationId, "errorMessage" to "M4 failed: $errorCode")
        }
    }

    private fun parseA0Response(response: String, operation: String, correlationId: String): Map<String, Any> {
        val hdrLen = msgHeader.length
        val errorCode = if (response.length >= hdrLen + 4) response.substring(hdrLen + 2, hdrLen + 4) else "99"

        return if (errorCode == "00") {
            // A1 response: [HDR][A1][00][SCHEME_KEY_33H][KCV_6H]
            val data = response.substring(hdrLen + 4)
            val keyRef = if (data.length >= 33) data.substring(0, 33) else data
            val kcv = if (data.length >= 39) data.substring(33, 39) else ""
            mapOf("success" to true, "keyId" to keyRef, "hsmSlotReference" to keyRef,
                "kcv" to kcv, "message" to "$operation completed", "correlationId" to correlationId)
        } else {
            mapOf("success" to false, "errorCode" to errorCode,
                "message" to "HSM rejected A0 command: $errorCode", "correlationId" to correlationId)
        }
    }

    private fun parseM0Response(response: String, correlationId: String): Map<String, Any> {
        val hdrLen = msgHeader.length
        val errorCode = if (response.length >= hdrLen + 4) response.substring(hdrLen + 2, hdrLen + 4) else "99"

        return if (errorCode == "00") {
            val encryptedData = response.substring(hdrLen + 4)
            mapOf("success" to true, "encryptedData" to encryptedData, "correlationId" to correlationId)
        } else {
            mapOf("success" to false, "encryptedData" to "", "errorCode" to errorCode, "correlationId" to correlationId)
        }
    }

    private fun parseA0KeyResponse(response: String): Pair<String, String> {
        val hdrLen = msgHeader.length
        val errorCode = if (response.length >= hdrLen + 4) response.substring(hdrLen + 2, hdrLen + 4) else "99"
        if (errorCode != "00") return Pair("", "")
        val data = response.substring(hdrLen + 4)
        val key = if (data.length >= 33) data.substring(1, 33) else data // skip scheme char
        val kcv = if (data.length >= 39) data.substring(33, 39) else ""
        return Pair(key, kcv)
    }

    // ====================================================================================================
    // Key Storage / Lookup (Simulated — in production these come from a DB)
    //
    // In a real deployment these would query edc_terminal_encryption_details and
    // edc_acquirer_encryption_details tables. For the simulator we use configurable defaults.
    // ====================================================================================================

    private val terminalKeys = mutableMapOf<String, TerminalKeyRecord>()
    private val acquirerKeys = mutableMapOf<String, AcquirerKeyRecord>()
    private val bdkStore = mutableMapOf<String, String>()

    data class TerminalKeyRecord(
        val bptid: String,
        val pinBdk: String,        // 32H — BDK under LMK
        val macBdk: String = "",
        val dataBdk: String = ""
    )

    data class AcquirerKeyRecord(
        val acquirerCode: String,
        val zpk: String,           // 32H — ZPK under LMK
        val zak: String = ""       // 32H — ZAK under LMK
    )

    /** Register terminal keys (called from KeyMAS admin UI) */
    fun registerTerminal(bptid: String, pinBdk: String, macBdk: String = "", dataBdk: String = "") {
        terminalKeys[bptid] = TerminalKeyRecord(bptid, pinBdk, macBdk, dataBdk)
        log("Terminal registered: $bptid")
    }

    /** Register acquirer keys (called after key exchange ceremony) */
    fun registerAcquirer(acquirerCode: String, zpk: String, zak: String = "") {
        acquirerKeys[acquirerCode] = AcquirerKeyRecord(acquirerCode, zpk, zak)
        log("Acquirer registered: $acquirerCode")
    }

    fun registerBdk(reference: String, bdk: String) {
        bdkStore[reference] = bdk
    }

    private fun lookupTerminalPinBdk(bptid: String): String =
        terminalKeys[bptid]?.pinBdk ?: "0000000000000000000000000000000F" // test default

    private fun lookupTerminalMacKey(bptid: String): String =
        terminalKeys[bptid]?.macBdk?.ifBlank { lookupTerminalPinBdk(bptid) } ?: "0000000000000000000000000000000F"

    private fun lookupTerminalDataBdk(bptid: String): String =
        terminalKeys[bptid]?.dataBdk?.ifBlank { lookupTerminalPinBdk(bptid) } ?: "0000000000000000000000000000000F"

    private fun lookupAcquirerZpk(acquirerCode: String): String =
        acquirerKeys[acquirerCode]?.zpk ?: "0000000000000000000000000000000F" // test default

    private fun lookupAcquirerZak(acquirerCode: String): String =
        acquirerKeys[acquirerCode]?.zak?.ifBlank { lookupAcquirerZpk(acquirerCode) } ?: "0000000000000000000000000000000F"

    private fun lookupBdkByReference(ref: String): String =
        bdkStore[ref] ?: "0000000000000000000000000000000F"

    private fun generateKsn(deviceSerial: String): String {
        val devId = deviceSerial.takeLast(9).padStart(9, '0')
        return "FFFF01${devId}00000"  // 20H KSN
    }

    // ====================================================================================================
    // Helpers
    // ====================================================================================================

    private fun log(msg: String) {
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        onLog("[$ts] $msg")
        _state.value = _state.value.copy(
            logs = (_state.value.logs + "[$ts] $msg").takeLast(500)
        )
    }

    private fun incrementRequestCount() {
        _state.value = _state.value.copy(totalRequests = _state.value.totalRequests + 1)
    }
}

data class KeyMasState(
    val running: Boolean = false,
    val startedAt: LocalDateTime? = null,
    val totalRequests: Long = 0L,
    val logs: List<String> = emptyList()
)
