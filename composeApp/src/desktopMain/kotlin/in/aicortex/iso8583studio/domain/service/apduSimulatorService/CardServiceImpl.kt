package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.domain.utils.ApduUtil
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.ApduCommand
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.ConnectionInterface
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.createLogEntry
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Card Service Implementation following GatewayServiceImpl architecture
 * Handles EMV card simulation, APDU processing, and reader management
 */
@OptIn(ExperimentalAtomicApi::class)
class CardServiceImpl(
    val configuration: APDUSimulatorConfig
) {
    // Core state management (following GatewayServiceImpl pattern)
    val started = AtomicBoolean(false)
    val bytesOutgoing = AtomicLong(0L)
    val bytesIncoming = AtomicLong(0L)
    val sessionCount = AtomicInteger(0)
    val commandCount = AtomicInteger(0)

    // Card-specific state
    val activeReaders = ConcurrentHashMap<String, MockCardReader>()
    val activeSessions = ConcurrentHashMap<String, CardSession>()
    var currentCard: MockCard? = null
    var currentChannel: MockCardChannel? = null
    var isCardPresent = AtomicBoolean(false)
    var lastAtr: MockCardATR? = null

    // UI integration (identical to GatewayServiceImpl)
    var composeWindow: ComposeWindow? = null
    private var showErrorListener: ResultDialogInterface? = null

    // Event handlers (following same pattern as GatewayServiceImpl)
    var onReceiveCommand: ((ApduMessage?) -> Unit)? = null
    var onSendResponse: ((ApduMessage?) -> Unit)? = null
    var onCardInserted: ((ByteArray?) -> Unit)? = null
    var onCardRemoved: (() -> Unit)? = null
    var beforeProcessCommand: (() -> Unit)? = null
    var beforeWriteLog: ((LogEntry) -> Unit)? = null
    var sendHoldCommand: (() -> Unit)? = null

    // Configuration state
    var holdCommand = false
    var autoResponse = true
    var emulateCardType = configuration.cardType

    // Coroutine management (same pattern as GatewayServiceImpl)
    private var serviceScope: CoroutineScope? = null
    private var readerMonitorJob: Job? = null
    private var cardMonitorJob: Job? = null

    // Mock card data for simulation
    private val mockApplications = mutableMapOf<String, MockApplication>()
    private val mockFiles = mutableMapOf<String, ByteArray>()

    init {
        initializeMockCard()
    }

    /**
     * Initialize mock card applications and data
     */
    private fun initializeMockCard() {
        // Initialize default EMV application
        val visaApp = MockApplication(
            aid = "A0000000031010",
            label = "VISA",
            priority = 1,
            files = mutableMapOf(
                "1PAY.SYS.DDF01" to createPseRecord(),
                "2PAY.SYS.DDF01" to createVisaRecord()
            )
        )
        mockApplications[visaApp.aid] = visaApp

        // Add Mastercard application
        val mastercardApp = MockApplication(
            aid = "A0000000041010",
            label = "MASTERCARD",
            priority = 2,
            files = mutableMapOf(
                "1PAY.SYS.DDF01" to createPseRecord(),
                "2PAY.SYS.DDF01" to createMastercardRecord()
            )
        )
        mockApplications[mastercardApp.aid] = mastercardApp
    }

    /**
     * Start the card service (following GatewayServiceImpl.start pattern)
     */
    suspend fun start() {
        if (started.get()) return

        withContext(Dispatchers.IO) {
            try {
                serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

                when (configuration.connectionInterface) {
                    ConnectionInterface.PC_SC -> startPcScService()
                    ConnectionInterface.NFC -> startNfcService()
                    ConnectionInterface.MOCK -> startMockService()
                    ConnectionInterface.USB -> startUsbService()
                }

                started.set(true)
                logInfo("Card service started successfully")

            } catch (e: Exception) {
                logError("Failed to start card service: ${e.message}")
                showErrorListener?.onError {  Text( e.message ?: "Unknown error") }
                throw e
            }
        }
    }

    /**
     * Stop the card service (following GatewayServiceImpl.stop pattern)
     */
    suspend fun stop() {
        if (!started.get()) return

        withContext(Dispatchers.IO) {
            try {
                started.set(false)

                // Disconnect current card
                currentChannel?.close()
                currentCard?.disconnect(false)

                // Cancel monitoring jobs
                readerMonitorJob?.cancel()
                cardMonitorJob?.cancel()

                // Clear active sessions
                activeSessions.clear()
                activeReaders.clear()

                // Cancel service scope
                serviceScope?.cancel()

                logInfo("Card service stopped successfully")

            } catch (e: Exception) {
                logError("Error stopping card service: ${e.message}")
            }
        }
    }

    /**
     * Start PC/SC reader monitoring (Mock implementation for Compose Desktop)
     */
    private suspend fun startPcScService() {
        try {
            // Mock PC/SC service since javax.smartcardio is not available
            logInfo("Starting mock PC/SC service for Compose Desktop")

            // Simulate reader detection
            val mockReaders = listOf("Mock Reader 1", "Mock Reader 2", "ACS ACR122U")

            readerMonitorJob = serviceScope?.launch {
                while (started.get()) {
                    try {
                        // Simulate reader detection
                        mockReaders.forEach { readerName ->
                            if (!activeReaders.containsKey(readerName)) {
                                val mockTerminal = MockCardTerminal(readerName)
                                activeReaders[readerName] = MockCardReader(readerName, mockTerminal)
                                logInfo("Mock reader detected: $readerName")
                            }
                        }

                        // Monitor card insertion/removal for first reader
                        activeReaders.values.firstOrNull()?.let { reader ->
                            monitorCardEvents(reader.terminal)
                        }

                        delay(1000) // Check for readers every second
                    } catch (e: Exception) {
                        logError("PC/SC monitoring error: ${e.message}")
                        delay(5000) // Wait before retry
                    }
                }
            }

        } catch (e: Exception) {
            logError("Failed to initialize mock PC/SC service: ${e.message}")
            throw e
        }
    }

    /**
     * Monitor card insertion and removal events (Mock implementation)
     */
    private suspend fun monitorCardEvents(terminal: MockCardTerminal) {
        cardMonitorJob = serviceScope?.launch {
            while (started.get()) {
                try {
                    if (terminal.isCardPresent() && !isCardPresent.get()) {
                        // Card inserted
                        handleCardInsertion(terminal)
                    } else if (!terminal.isCardPresent() && isCardPresent.get()) {
                        // Card removed
                        handleCardRemoval()
                    }

                    delay(500) // Check card presence every 500ms
                } catch (e: Exception) {
                    logError("Card monitoring error: ${e.message}")
                    delay(2000)
                }
            }
        }
    }

    /**
     * Handle card insertion event (Mock implementation)
     */
    private suspend fun handleCardInsertion(terminal: MockCardTerminal) {
        try {
            currentCard = terminal.connect("T=0")
            currentChannel = currentCard?.getBasicChannel()
            lastAtr = currentCard?.getATR()
            isCardPresent.set(true)

            val atrBytes = lastAtr?.byteArray
            logInfo("Card inserted - ATR: ${ApduUtil.bytesToHexString(atrBytes ?: byteArrayOf())}")

            // Notify UI
            onCardInserted?.invoke(atrBytes)

            // Start APDU processing
            startApduProcessing()

        } catch (e: Exception) {
            logError("Card insertion error: ${e.message}")
            showErrorListener?.onError {  Text("Failed to connect to card: ${e.message}") }
        }
    }

    /**
     * Handle card removal event (Mock implementation)
     */
    private suspend fun handleCardRemoval() {
        try {
            currentChannel?.close()
            currentCard?.disconnect(false)
            currentCard = null
            currentChannel = null
            lastAtr = null
            isCardPresent.set(false)

            logInfo("Card removed")
            onCardRemoved?.invoke()

        } catch (e: Exception) {
            logError("Card removal error: ${e.message}")
        }
    }

    /**
     * Start APDU command processing
     */
    private suspend fun startApduProcessing() {
        serviceScope?.launch {
            while (isCardPresent.get() && started.get()) {
                try {
                    // In real implementation, this would listen for APDU commands
                    // For simulation, we can process pre-configured commands
                    delay(100)
                } catch (e: Exception) {
                    logError("APDU processing error: ${e.message}")
                    delay(1000)
                }
            }
        }
    }

    /**
     * Process APDU command (core functionality)
     */
    suspend fun processApdu(commandBytes: ByteArray): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                beforeProcessCommand?.invoke()

                val apduCommand = ApduMessage.fromBytes(commandBytes)
                logInfo("Processing APDU: ${ApduUtil.bytesToHexString(commandBytes)}")

                // Notify UI of incoming command
                onReceiveCommand?.invoke(apduCommand)

                // Update metrics
                bytesIncoming.addAndGet(commandBytes.size.toLong())
                commandCount.incrementAndGet()

                // Process command based on card type and configuration
                val responseBytes = when (configuration.connectionInterface) {
                    ConnectionInterface.MOCK -> processMockApdu(apduCommand)
                    else -> processRealApdu(apduCommand)
                }

                val apduResponse = ApduMessage.fromResponse(responseBytes)

                // Notify UI of outgoing response
                onSendResponse?.invoke(apduResponse)

                // Update metrics
                bytesOutgoing.addAndGet(responseBytes.size.toLong())

                logInfo("APDU Response: ${ApduUtil.bytesToHexString(responseBytes)}")

                responseBytes

            } catch (e: Exception) {
                logError("APDU processing failed: ${e.message}")
                // Return general error response
                byteArrayOf(0x6F.toByte(), 0x00.toByte()) // SW_UNKNOWN
            }
        }
    }

    /**
     * Process mock APDU for simulation
     */
    private fun processMockApdu(command: ApduMessage): ByteArray {
        return when {
            // SELECT command
            command.ins == 0xA4.toByte() -> handleSelectCommand(command)

            // READ BINARY
            command.ins == 0xB0.toByte() ->  handleReadRecord(command)//handleReadBinary(command)

            // READ RECORD
            command.ins == 0xB2.toByte() -> handleReadRecord(command)

            // GET DATA
            command.ins == 0xCA.toByte() -> handleGetData(command)

            // VERIFY PIN
            command.ins == 0x20.toByte() -> handleVerifyPin(command)

            // GET CHALLENGE
            command.ins == 0x84.toByte() -> handleGetChallenge(command)

            else -> {
                logWarning("Unsupported APDU command: INS=${ApduUtil.byteToHexString(command.ins)}")
                byteArrayOf(0x6D.toByte(), 0x00.toByte()) // SW_INS_NOT_SUPPORTED
            }
        }
    }
    fun showError(item: @Composable () -> Unit) {
        showErrorListener?.onError(item)
    }

    /**
     * Process real APDU using mock card channel
     */
    private suspend fun processRealApdu(command: ApduMessage): ByteArray {
        return try {
            // Since we can't use javax.smartcardio.CommandAPDU, create mock implementation
            val mockCommand = MockCommandAPDU(
                command.cla.toInt(),
                command.ins.toInt(),
                command.p1.toInt(),
                command.p2.toInt(),
                command.data,
                command.le
            )

            val response = currentChannel?.transmit(mockCommand)
            response?.bytes ?: byteArrayOf(0x6F.toByte(), 0x00.toByte())

        } catch (e: Exception) {
            logError("Real APDU transmission failed: ${e.message}")
            byteArrayOf(0x6F.toByte(), 0x00.toByte())
        }
    }

    /**
     * Handle SELECT command
     */
    private fun handleSelectCommand(command: ApduMessage): ByteArray {
        val aid = ApduUtil.bytesToHexString(command.data)

        return when (command.p1.toInt()) {
            0x04 -> { // Select by AID
                if (mockApplications.containsKey(aid)) {
                    val app = mockApplications[aid]!!
                    logInfo("Selected application: ${app.label} (${aid})")

                    // Return FCI template
                    val fci = createFciTemplate(app)
                    fci + byteArrayOf(0x90.toByte(), 0x00.toByte())
                } else {
                    logWarning("Application not found: $aid")
                    byteArrayOf(0x6A.toByte(), 0x82.toByte()) // SW_FILE_NOT_FOUND
                }
            }
            else -> {
                byteArrayOf(0x6A.toByte(), 0x86.toByte()) // SW_INCORRECT_P1P2
            }
        }
    }


    /**
     * Handle READ RECORD command
     */
    private fun handleReadRecord(command: ApduMessage): ByteArray {
        val recordNumber = command.p1.toInt()
        val sfi = (command.p2.toInt() shr 3) and 0x1F

        logInfo("Reading record $recordNumber from SFI $sfi")

        // Return mock record data
        val recordData = createMockRecord(recordNumber, sfi)
        return recordData + byteArrayOf(0x90.toByte(), 0x00.toByte())
    }

    /**
     * Handle GET DATA command
     */
    private fun handleGetData(command: ApduMessage): ByteArray {
        val tag = (command.p1.toInt() shl 8) or command.p2.toInt()

        return when (tag) {
            0x9F13 -> { // Last Online ATC Register
                byteArrayOf(0x00, 0x00) + byteArrayOf(0x90.toByte(), 0x00.toByte())
            }
            0x9F17 -> { // PIN Try Counter
                byteArrayOf(0x03) + byteArrayOf(0x90.toByte(), 0x00.toByte())
            }
            0x9F36 -> { // Application Transaction Counter
                byteArrayOf(0x00, 0x01) + byteArrayOf(0x90.toByte(), 0x00.toByte())
            }
            else -> {
                logWarning("Unsupported GET DATA tag: ${ApduUtil.intToHexString(tag, 4)}")
                byteArrayOf(0x6A.toByte(), 0x88.toByte()) // SW_REFERENCED_DATA_NOT_FOUND
            }
        }
    }

    /**
     * Handle VERIFY PIN command
     */
    private fun handleVerifyPin(command: ApduMessage): ByteArray {
        val pin = ApduUtil.bytesToHexString(command.data)
        logInfo("PIN verification attempt")

        // Mock PIN verification (accept any 4-digit PIN)
        return if (command.data.size == 8 && pin.startsWith("24")) { // Format 2, 4 digits
            logInfo("PIN verification successful")
            byteArrayOf(0x90.toByte(), 0x00.toByte()) // SW_SUCCESS
        } else {
            logWarning("PIN verification failed")
            byteArrayOf(0x63.toByte(), 0xC2.toByte()) // SW_WRONG_PIN (2 tries remaining)
        }
    }

    /**
     * Handle GET CHALLENGE command
     */
    private fun handleGetChallenge(command: ApduMessage): ByteArray {
        val challengeLength = command.le
        val challenge = ByteArray(challengeLength) { (Math.random() * 256).toInt().toByte() }

        logInfo("Generated challenge: ${ApduUtil.bytesToHexString(challenge)}")
        return challenge + byteArrayOf(0x90.toByte(), 0x00.toByte())
    }

    /**
     * Start NFC service
     */
    private suspend fun startNfcService() {
        logInfo("Starting NFC service (simulation)")
        // NFC implementation would go here
        // For now, delegate to mock service
        startMockService()
    }

    /**
     * Start USB service
     */
    private suspend fun startUsbService() {
        logInfo("Starting USB service (simulation)")
        // USB implementation would go here
        // For now, delegate to mock service
        startMockService()
    }

    /**
     * Start mock service for testing
     */
    private suspend fun startMockService() {
        logInfo("Starting mock card service")

        // Simulate card insertion after 2 seconds
        serviceScope?.launch {
            delay(2000)
            if (started.get()) {
                val mockAtr = byteArrayOf(0x3B, 0x6A, 0x00, 0x00, 0x00, 0x31, 0x00, 0x66, 0x01, 0x04, 0x0C, 0x01, 0x6E)
                isCardPresent.set(true)
                lastAtr = MockCardATR(mockAtr)

                logInfo("Mock card inserted - ATR: ${ApduUtil.bytesToHexString(mockAtr)}")
                onCardInserted?.invoke(mockAtr)
            }
        }
    }

    // Utility methods for creating mock data
    private fun createFciTemplate(app: MockApplication): ByteArray {
        val baos = ByteArrayOutputStream()

        // FCI Template (tag 6F)
        baos.write(0x6F)

        val fciData = ByteArrayOutputStream()

        // DF Name (tag 84)
        val aidBytes = ApduUtil.hexStringToBytes(app.aid)
        fciData.write(0x84)
        fciData.write(aidBytes.size)
        fciData.write(aidBytes)

        // Application Label (tag 50)
        val labelBytes = app.label.toByteArray()
        fciData.write(0x50)
        fciData.write(labelBytes.size)
        fciData.write(labelBytes)

        // Priority Indicator (tag 87)
        fciData.write(0x87)
        fciData.write(0x01)
        fciData.write(app.priority)

        val fciDataBytes = fciData.toByteArray()
        baos.write(fciDataBytes.size)
        baos.write(fciDataBytes)

        return baos.toByteArray()
    }

    private fun createPseRecord(): ByteArray {
        return ApduUtil.hexStringToBytes("701A4F07A0000000031010500A4D6173746572436172648701019F12104D6173746572436172642044656269743131")
    }

    private fun createVisaRecord(): ByteArray {
        return ApduUtil.hexStringToBytes("7019500956495341204445424954870101BF0C0A9F4D020B0A9F6E0420003030")
    }

    private fun createMastercardRecord(): ByteArray {
        return ApduUtil.hexStringToBytes("701E4F07A00000000410105010204D6173746572436172642044656269743131870101")
    }

    private fun createMockRecord(recordNumber: Int, sfi: Int): ByteArray {
        // Return mock EMV record data
        return when (sfi) {
            1 -> ApduUtil.hexStringToBytes("70145F2D02656E9F11010157104111111111111111D2512201000000000F")
            2 -> ApduUtil.hexStringToBytes("70129F4C0820000000000000009F4E1456495341204445424954202020202020202020")
            else -> ApduUtil.hexStringToBytes("7000")
        }
    }

    // Logging methods (following GatewayServiceImpl pattern)
    private fun logInfo(message: String) {
        val entry = createLogEntry(
            type = LogType.INFO,
            message = message,
        )
        beforeWriteLog?.invoke(entry)
    }

    private fun logWarning(message: String) {
        val entry = createLogEntry(
            type = LogType.WARNING,
            message = message,
        )
        beforeWriteLog?.invoke(entry)
    }

    private fun logError(message: String) {
        val entry = createLogEntry(
            type = LogType.ERROR,
            message = message,
        )
        beforeWriteLog?.invoke(entry)
    }

    // Error handling (identical to GatewayServiceImpl)
    fun setShowErrorListener(listener: ResultDialogInterface) {
        this.showErrorListener = listener
    }

    // Public API methods
    suspend fun sendCommand(command: ApduCommand): ByteArray {

        return processApdu(command.raw)
    }

    fun getConnectedReaders(): List<String> {
        return activeReaders.keys.toList()
    }

    fun getCurrentCardInfo(): CardInfo? {
        return if (isCardPresent.get()) {
            CardInfo(
                atr = lastAtr?.byteArray,
                protocol = currentCard?.protocol,
                isPresent = true
            )
        } else null
    }
}

// Supporting data classes (Mock implementations for Compose Desktop)
data class MockCardReader(
    val name: String,
    val terminal: MockCardTerminal
)

data class CardSession(
    val sessionId: String,
    val startTime: Long,
    val reader: MockCardReader,
    var lastActivity: Long = System.currentTimeMillis()
)

data class MockApplication(
    val aid: String,
    val label: String,
    val priority: Int,
    val files: MutableMap<String, ByteArray>
)

data class CardInfo(
    val atr: ByteArray?,
    val protocol: String?,
    val isPresent: Boolean
)

/**
 * Mock Card Terminal implementation for Compose Desktop
 */
class MockCardTerminal(private val name: String) {
    private var cardPresent = false

    fun getName(): String = name

    fun isCardPresent(): Boolean = cardPresent

    fun connect(protocol: String): MockCard {
        cardPresent = true
        return MockCard(protocol)
    }

    fun waitForCardPresent(timeout: Long): Boolean {
        // Simulate card insertion after a delay
        Thread.sleep(minOf(timeout, 1000))
        cardPresent = true
        return true
    }

    fun waitForCardAbsent(timeout: Long): Boolean {
        // Simulate card removal
        Thread.sleep(minOf(timeout, 1000))
        cardPresent = false
        return true
    }
}

/**
 * Mock Card implementation
 */
class MockCard(val protocol: String) {
    private val atr = MockCardATR(byteArrayOf(0x3B, 0x6A, 0x00, 0x00, 0x00, 0x31, 0x00, 0x66, 0x01, 0x04, 0x0C, 0x01, 0x6E))
    private val basicChannel = MockCardChannel()

    fun getATR(): MockCardATR = atr

    fun getBasicChannel(): MockCardChannel = basicChannel

    fun disconnect(reset: Boolean) {
        // Mock disconnect
    }
}

/**
 * Mock Card Channel implementation
 */
class MockCardChannel {
    fun transmit(command: MockCommandAPDU): MockResponseAPDU {
        // Mock APDU transmission - return success response
        return MockResponseAPDU(byteArrayOf(0x90.toByte(), 0x00.toByte()))
    }

    fun close() {
        // Mock close
    }
}

/**
 * Mock ATR (Answer To Reset) implementation
 */
class MockCardATR(val byteArray: ByteArray) {

    fun getHistoricalBytes(): ByteArray {
        return if (byteArray.size > 2) {
            byteArray.sliceArray(2 until byteArray.size - 2)
        } else {
            byteArrayOf()
        }
    }
}

/**
 * Mock Command APDU implementation
 */
class MockCommandAPDU(
    val cla: Int,
    val ins: Int,
    val p1: Int,
    val p2: Int,
    val data: ByteArray,
    val le: Int
) {
    fun getBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.write(cla)
        baos.write(ins)
        baos.write(p1)
        baos.write(p2)

        if (data.isNotEmpty()) {
            baos.write(data.size)
            baos.write(data)
        }

        if (le > 0) {
            baos.write(le)
        }

        return baos.toByteArray()
    }
}

/**
 * Mock Response APDU implementation
 */
class MockResponseAPDU(val bytes: ByteArray) {
    fun getData(): ByteArray {
        return if (bytes.size > 2) {
            bytes.sliceArray(0 until bytes.size - 2)
        } else {
            byteArrayOf()
        }
    }

    fun getSW(): Int {
        return if (bytes.size >= 2) {
            ((bytes[bytes.size - 2].toInt() and 0xFF) shl 8) or (bytes[bytes.size - 1].toInt() and 0xFF)
        } else {
            0x6F00
        }
    }

    fun getSW1(): Int {
        return if (bytes.size >= 2) {
            bytes[bytes.size - 2].toInt() and 0xFF
        } else {
            0x6F
        }
    }

    fun getSW2(): Int {
        return if (bytes.size >= 1) {
            bytes[bytes.size - 1].toInt() and 0xFF
        } else {
            0x00
        }
    }
}

/**
 * APDU Message class for command/response handling
 */
data class ApduMessage(
    val cla: Byte,
    val ins: Byte,
    val p1: Byte,
    val p2: Byte,
    val data: ByteArray,
    val le: Int,
    val rawCommand: ByteArray? = null,
    val rawResponse: ByteArray? = null,
    val statusWord: String? = null
) {
    companion object {
        fun fromBytes(bytes: ByteArray): ApduMessage {
            return if (bytes.size >= 4) {
                val cla = bytes[0]
                val ins = bytes[1]
                val p1 = bytes[2]
                val p2 = bytes[3]

                val (data, le) = when {
                    bytes.size == 4 -> Pair(byteArrayOf(), 0)
                    bytes.size == 5 -> Pair(byteArrayOf(), bytes[4].toInt() and 0xFF)
                    else -> {
                        val lc = bytes[4].toInt() and 0xFF
                        val cmdData = if (lc > 0) bytes.sliceArray(5 until 5 + lc) else byteArrayOf()
                        val expectedLe = if (bytes.size > 5 + lc) bytes[5 + lc].toInt() and 0xFF else 0
                        Pair(cmdData, expectedLe)
                    }
                }

                ApduMessage(cla, ins, p1, p2, data, le, bytes)
            } else {
                ApduMessage(0, 0, 0, 0, byteArrayOf(), 0, bytes)
            }
        }

        fun fromResponse(bytes: ByteArray): ApduMessage {
            val sw = if (bytes.size >= 2) {
                val sw1 = bytes[bytes.size - 2].toInt() and 0xFF
                val sw2 = bytes[bytes.size - 1].toInt() and 0xFF
                "${ApduUtil.intToHexString(sw1, 2)}${ApduUtil.intToHexString(sw2, 2)}"
            } else "0000"

            val responseData = if (bytes.size > 2) bytes.sliceArray(0 until bytes.size - 2) else byteArrayOf()

            return ApduMessage(0, 0, 0, 0, responseData, 0, rawResponse = bytes, statusWord = sw)
        }
    }

    fun logFormat(): String {
        return if (rawCommand != null) {
            "Command: ${ApduUtil.bytesToHexString(rawCommand)}"
        } else if (rawResponse != null) {
            "Response: ${ApduUtil.bytesToHexString(rawResponse)} (SW: $statusWord)"
        } else {
            "APDU: CLA=${ApduUtil.byteToHexString(cla)} INS=${ApduUtil.byteToHexString(ins)} P1=${ApduUtil.byteToHexString(p1)} P2=${ApduUtil.byteToHexString(p2)}"
        }
    }
}