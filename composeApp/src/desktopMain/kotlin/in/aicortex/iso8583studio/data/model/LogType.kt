package `in`.aicortex.iso8583studio.logging

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Comprehensive Log Types for ISO8583 Financial Transaction Processing
 */
enum class LogType(
    val displayName: String,
    val icon: ImageVector,
    val color: Color,
    val priority: Int, // 1=Critical, 5=Low
    val category: LogCategory
) {
    // CRITICAL SYSTEM EVENTS (Priority 1-2)
    ERROR("Error", Icons.Default.Error, Color(0xFFF44336), 1, LogCategory.SYSTEM),
    SECURITY("Security", Icons.Default.Security, Color(0xFF9C27B0), 1, LogCategory.SECURITY),

    // HIGH PRIORITY OPERATIONAL (Priority 2-3)
    WARNING("Warning", Icons.Default.Warning, Color(0xFFFF9800), 2, LogCategory.SYSTEM),
    CONNECTION("Connection", Icons.Default.Router, Color(0xFF673AB7), 2, LogCategory.NETWORK),
    TRANSACTION("Transaction", Icons.Default.SwapHoriz, Color(0xFF4CAF50), 2, LogCategory.TRANSACTION),

    // MEDIUM PRIORITY INFORMATIONAL (Priority 3-4)
    INFO("Info", Icons.Default.Info, Color(0xFF2196F3), 3, LogCategory.SYSTEM),
    AUTHORIZATION("Authorization", Icons.Default.VerifiedUser, Color(0xFF00BCD4), 3, LogCategory.TRANSACTION),
    SETTLEMENT("Settlement", Icons.Default.AccountBalance, Color(0xFF009688), 3, LogCategory.TRANSACTION),
    REVERSAL("Reversal", Icons.Default.Undo, Color(0xFFFF5722), 3, LogCategory.TRANSACTION),

    // LOW PRIORITY DETAILED (Priority 4-5)
    VERBOSE("Verbose", Icons.Default.Visibility, Color(0xFF607D8B), 4, LogCategory.SYSTEM),
    DEBUG("Debug", Icons.Default.BugReport, Color(0xFF795548), 4, LogCategory.DEVELOPMENT),
    TRACE("Trace", Icons.Default.Timeline, Color(0xFF9E9E9E), 5, LogCategory.DEVELOPMENT),

    // SPECIALIZED ISO8583 TYPES
    MESSAGE("Message", Icons.Default.Message, Color(0xFF3F51B5), 3, LogCategory.PROTOCOL),
    FIELD("Field", Icons.Default.DataObject, Color(0xFF8BC34A), 4, LogCategory.PROTOCOL),
    BITMAP("Bitmap", Icons.Default.GridOn, Color(0xFFCDDC39), 4, LogCategory.PROTOCOL),
    ENCRYPTION("Encryption", Icons.Default.Lock, Color(0xFFE91E63), 2, LogCategory.SECURITY),
    HSM("HSM", Icons.Default.Hardware, Color(0xFF6A1B9A), 2, LogCategory.SECURITY),

    // NETWORK & COMMUNICATION
    SOCKET("Socket", Icons.Default.Cable, Color(0xFF455A64), 3, LogCategory.NETWORK),
    PROTOCOL("Protocol", Icons.Default.Lan, Color(0xFF37474F), 3, LogCategory.NETWORK),

    // PERFORMANCE & MONITORING
    PERFORMANCE("Performance", Icons.Default.Speed, Color(0xFFFF6F00), 3, LogCategory.MONITORING),
    METRICS("Metrics", Icons.Default.Analytics, Color(0xFF1976D2), 4, LogCategory.MONITORING)
}

/**
 * Log Categories for better organization
 */
enum class LogCategory(val displayName: String, val icon: ImageVector) {
    SYSTEM("System", Icons.Default.Computer),
    TRANSACTION("Transaction", Icons.Default.Payment),
    NETWORK("Network", Icons.Default.NetworkCheck),
    SECURITY("Security", Icons.Default.Shield),
    PROTOCOL("Protocol", Icons.Default.Code),
    DEVELOPMENT("Development", Icons.Default.DeveloperMode),
    MONITORING("Monitoring", Icons.Default.Monitor)
}

/**
 * Enhanced Log Entry with additional metadata
 */
data class LogEntry(
    val id: String = generateLogId(),
    val timestamp: String,
    var type: LogType,
    var message: String,
    var details: String? = null,
    var source: String? = null,
    var sessionId: String? = null,
    var correlationId: String? = null,
    var metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        private var logCounter = 0L
        private fun generateLogId(): String = "LOG_${++logCounter}_${System.currentTimeMillis()}"
    }
}

/**
 * Log Entry Builder for easier creation
 */
class LogEntryBuilder {
    private var type: LogType = LogType.INFO
    private var message: String = ""
    private var details: String? = null
    private var source: String? = null
    private var sessionId: String? = null
    private var correlationId: String? = null
    private var metadata: MutableMap<String, Any> = mutableMapOf()

    fun type(type: LogType) = apply { this.type = type }
    fun message(message: String) = apply { this.message = message }
    fun details(details: String) = apply { this.details = details }
    fun source(source: String) = apply { this.source = source }
    fun sessionId(sessionId: String) = apply { this.sessionId = sessionId }
    fun correlationId(correlationId: String) = apply { this.correlationId = correlationId }
    fun metadata(key: String, value: Any) = apply { this.metadata[key] = value }

    fun build(): LogEntry {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
        return LogEntry(
            timestamp = timestamp,
            type = type,
            message = message,
            details = details,
            source = source,
            sessionId = sessionId,
            correlationId = correlationId,
            metadata = metadata.toMap()
        )
    }
}

/**
 * Factory for creating common log entries
 */
object LogEntryFactory {

    // SYSTEM LOGS
    fun systemStarted() = LogEntryBuilder()
        .type(LogType.INFO)
        .message("ISO8583Studio started successfully")
        .source("System")
        .build()

    fun systemShutdown() = LogEntryBuilder()
        .type(LogType.INFO)
        .message("System shutdown initiated")
        .source("System")
        .build()

    fun configurationLoaded(configName: String) = LogEntryBuilder()
        .type(LogType.INFO)
        .message("Configuration loaded")
        .details("Config: $configName")
        .source("ConfigManager")
        .build()

    // CONNECTION LOGS
    fun serverStarted(port: Int, maxConnections: Int) = LogEntryBuilder()
        .type(LogType.CONNECTION)
        .message("Server started on port $port")
        .details("Max connections: $maxConnections")
        .source("GatewayServer")
        .metadata("port", port)
        .metadata("maxConnections", maxConnections)
        .build()

    fun clientConnected(clientIp: String, sessionId: String) = LogEntryBuilder()
        .type(LogType.CONNECTION)
        .message("Client connected")
        .details("IP: $clientIp")
        .source("GatewayServer")
        .sessionId(sessionId)
        .metadata("clientIp", clientIp)
        .build()

    fun clientDisconnected(sessionId: String, reason: String? = null) = LogEntryBuilder()
        .type(LogType.CONNECTION)
        .message("Client disconnected")
        .details(reason ?: "Normal disconnection")
        .source("GatewayServer")
        .sessionId(sessionId)
        .build()

    fun connectionTimeout(sessionId: String, timeoutSeconds: Int) = LogEntryBuilder()
        .type(LogType.WARNING)
        .message("Connection timeout")
        .details("Session: $sessionId, Timeout: ${timeoutSeconds}s")
        .source("GatewayServer")
        .sessionId(sessionId)
        .metadata("timeoutSeconds", timeoutSeconds)
        .build()

    // TRANSACTION LOGS
    fun transactionReceived(mti: String, stan: String, amount: String? = null) = LogEntryBuilder()
        .type(LogType.TRANSACTION)
        .message("Transaction received")
        .details("MTI: $mti, STAN: $stan${amount?.let { ", Amount: $it" } ?: ""}")
        .source("TransactionProcessor")
        .correlationId(stan)
        .metadata("mti", mti)
        .metadata("stan", stan)
        .apply { amount?.let { metadata("amount", it) } }
        .build()

    fun authorizationRequest(pan: String, amount: String, merchantId: String) = LogEntryBuilder()
        .type(LogType.AUTHORIZATION)
        .message("Authorization request")
        .details("PAN: ${maskPan(pan)}, Amount: $amount, Merchant: $merchantId")
        .source("AuthProcessor")
        .metadata("maskedPan", maskPan(pan))
        .metadata("amount", amount)
        .metadata("merchantId", merchantId)
        .build()

    fun authorizationApproved(stan: String, authCode: String, responseCode: String = "00") = LogEntryBuilder()
        .type(LogType.AUTHORIZATION)
        .message("Authorization approved")
        .details("STAN: $stan, Auth Code: $authCode, Response: $responseCode")
        .source("AuthProcessor")
        .correlationId(stan)
        .metadata("authCode", authCode)
        .metadata("responseCode", responseCode)
        .build()

    fun authorizationDeclined(stan: String, responseCode: String, reason: String) = LogEntryBuilder()
        .type(LogType.WARNING)
        .message("Authorization declined")
        .details("STAN: $stan, Response: $responseCode - $reason")
        .source("AuthProcessor")
        .correlationId(stan)
        .metadata("responseCode", responseCode)
        .metadata("reason", reason)
        .build()

    fun reversalProcessed(originalStan: String, reversalStan: String) = LogEntryBuilder()
        .type(LogType.REVERSAL)
        .message("Reversal processed")
        .details("Original STAN: $originalStan, Reversal STAN: $reversalStan")
        .source("ReversalProcessor")
        .correlationId(reversalStan)
        .metadata("originalStan", originalStan)
        .metadata("reversalStan", reversalStan)
        .build()

    // ISO8583 MESSAGE LOGS
    fun messageReceived(length: Int, mti: String) = LogEntryBuilder()
        .type(LogType.MESSAGE)
        .message("ISO8583 message received")
        .details("Length: $length bytes, MTI: $mti")
        .source("MessageParser")
        .metadata("messageLength", length)
        .metadata("mti", mti)
        .build()

    fun messageSent(length: Int, mti: String) = LogEntryBuilder()
        .type(LogType.MESSAGE)
        .message("ISO8583 message sent")
        .details("Length: $length bytes, MTI: $mti")
        .source("MessageBuilder")
        .metadata("messageLength", length)
        .metadata("mti", mti)
        .build()

    fun fieldValidationError(fieldNumber: Int, error: String) = LogEntryBuilder()
        .type(LogType.ERROR)
        .message("Field validation error")
        .details("Field $fieldNumber: $error")
        .source("FieldValidator")
        .metadata("fieldNumber", fieldNumber)
        .metadata("validationError", error)
        .build()

    fun bitmapParsed(primaryBitmap: String, secondaryBitmap: String? = null) = LogEntryBuilder()
        .type(LogType.BITMAP)
        .message("Bitmap parsed")
        .details("Primary: $primaryBitmap${secondaryBitmap?.let { ", Secondary: $it" } ?: ""}")
        .source("BitmapParser")
        .metadata("primaryBitmap", primaryBitmap)
        .apply { secondaryBitmap?.let { metadata("secondaryBitmap", it) } }
        .build()

    // SECURITY LOGS
    fun encryptionPerformed(algorithm: String, keyId: String) = LogEntryBuilder()
        .type(LogType.ENCRYPTION)
        .message("Data encrypted")
        .details("Algorithm: $algorithm, Key ID: $keyId")
        .source("CryptoEngine")
        .metadata("algorithm", algorithm)
        .metadata("keyId", keyId)
        .build()

    fun decryptionPerformed(algorithm: String, keyId: String) = LogEntryBuilder()
        .type(LogType.ENCRYPTION)
        .message("Data decrypted")
        .details("Algorithm: $algorithm, Key ID: $keyId")
        .source("CryptoEngine")
        .metadata("algorithm", algorithm)
        .metadata("keyId", keyId)
        .build()

    fun securityViolation(violation: String, clientIp: String) = LogEntryBuilder()
        .type(LogType.SECURITY)
        .message("Security violation detected")
        .details("Violation: $violation, Source IP: $clientIp")
        .source("SecurityManager")
        .metadata("violation", violation)
        .metadata("clientIp", clientIp)
        .build()

    fun hsmOperation(operation: String, slot: Int, result: String) = LogEntryBuilder()
        .type(LogType.HSM)
        .message("HSM operation")
        .details("Operation: $operation, Slot: $slot, Result: $result")
        .source("HSMManager")
        .metadata("operation", operation)
        .metadata("slot", slot)
        .metadata("result", result)
        .build()

    // PERFORMANCE LOGS
    fun performanceMetric(operation: String, duration: Long, throughput: Double? = null) = LogEntryBuilder()
        .type(LogType.PERFORMANCE)
        .message("Performance metric")
        .details("Operation: $operation, Duration: ${duration}ms${throughput?.let { ", Throughput: $it/s" } ?: ""}")
        .source("PerformanceMonitor")
        .metadata("operation", operation)
        .metadata("duration", duration)
        .apply { throughput?.let { metadata("throughput", it) } }
        .build()

    fun transactionStatistics(total: Int, successful: Int, failed: Int, avgResponseTime: Double) = LogEntryBuilder()
        .type(LogType.METRICS)
        .message("Transaction statistics")
        .details("Total: $total, Success: $successful, Failed: $failed, Avg Response: ${avgResponseTime}ms")
        .source("StatisticsCollector")
        .metadata("total", total)
        .metadata("successful", successful)
        .metadata("failed", failed)
        .metadata("avgResponseTime", avgResponseTime)
        .build()

    // ERROR LOGS
    fun parsingError(error: String, rawData: String? = null) = LogEntryBuilder()
        .type(LogType.ERROR)
        .message("Message parsing error")
        .details("Error: $error${rawData?.let { ", Raw data: ${it.take(100)}..." } ?: ""}")
        .source("MessageParser")
        .metadata("error", error)
        .apply { rawData?.let { metadata("rawData", it) } }
        .build()

    fun networkError(error: String, remoteHost: String? = null) = LogEntryBuilder()
        .type(LogType.ERROR)
        .message("Network error")
        .details("Error: $error${remoteHost?.let { ", Remote: $it" } ?: ""}")
        .source("NetworkManager")
        .metadata("error", error)
        .apply { remoteHost?.let { metadata("remoteHost", it) } }
        .build()

    fun databaseError(operation: String, error: String) = LogEntryBuilder()
        .type(LogType.ERROR)
        .message("Database error")
        .details("Operation: $operation, Error: $error")
        .source("DatabaseManager")
        .metadata("operation", operation)
        .metadata("error", error)
        .build()

    // DEBUG LOGS
    fun debugMessage(component: String, message: String, data: Map<String, Any> = emptyMap()) = LogEntryBuilder()
        .type(LogType.DEBUG)
        .message("Debug: $component")
        .details(message)
        .source(component)
        .apply { data.forEach { (key, value) -> metadata(key, value) } }
        .build()

    fun traceMessage(component: String, message: String, stackTrace: String? = null) = LogEntryBuilder()
        .type(LogType.TRACE)
        .message("Trace: $component")
        .details(message)
        .source(component)
        .apply { stackTrace?.let { metadata("stackTrace", it) } }
        .build()

    // UTILITY FUNCTIONS
    private fun maskPan(pan: String): String {
        return if (pan.length >= 8) {
            "${pan.take(6)}${"*".repeat(pan.length - 10)}${pan.takeLast(4)}"
        } else {
            "*".repeat(pan.length)
        }
    }
}

/**
 * Sample log entries for testing and demonstration
 */
object SampleLogEntries {

    fun getTypicalTransactionFlow(): List<LogEntry> = listOf(
        LogEntryFactory.systemStarted(),
        LogEntryFactory.serverStarted(8080, 100),
        LogEntryFactory.clientConnected("192.168.1.45", "SESSION_001"),
        LogEntryFactory.messageReceived(256, "0200"),
        LogEntryFactory.transactionReceived("0200", "000001", "$50.00"),
        LogEntryFactory.bitmapParsed("F220000100000000"),
        LogEntryFactory.authorizationRequest("4111111111111111", "$50.00", "MERCHANT_001"),
        LogEntryFactory.encryptionPerformed("AES-256", "KEY_001"),
        LogEntryFactory.performanceMetric("authorization", 150),
        LogEntryFactory.authorizationApproved("000001", "123456"),
        LogEntryFactory.messageSent(128, "0210"),
        LogEntryFactory.clientDisconnected("SESSION_001"),
        LogEntryFactory.transactionStatistics(1, 1, 0, 150.0)
    )

    fun getErrorScenarios(): List<LogEntry> = listOf(
        LogEntryFactory.clientConnected("192.168.1.100", "SESSION_002"),
        LogEntryFactory.messageReceived(128, "0200"),
        LogEntryFactory.fieldValidationError(11, "Missing STAN field"),
        LogEntryFactory.parsingError("Invalid field length", "0200B220000100000000164111111111111111"),
        LogEntryFactory.authorizationDeclined("000002", "05", "Do not honor"),
        LogEntryFactory.networkError("Connection reset by peer", "192.168.1.100"),
        LogEntryFactory.securityViolation("Multiple failed login attempts", "192.168.1.100"),
        LogEntryFactory.connectionTimeout("SESSION_002", 30)
    )

    fun getDebugFlow(): List<LogEntry> = listOf(
        LogEntryFactory.debugMessage("GatewayServer", "Socket listener initialized", mapOf("port" to 8080)),
        LogEntryFactory.traceMessage("MessageParser", "Parsing MTI field"),
        LogEntryFactory.debugMessage("FieldValidator", "Validating mandatory fields", mapOf("fieldCount" to 12)),
        LogEntryFactory.traceMessage("BitmapParser", "Processing secondary bitmap"),
        LogEntryFactory.debugMessage("CryptoEngine", "Key rotation scheduled", mapOf("nextRotation" to "2024-12-31"))
    )

    fun getSecurityAuditTrail(): List<LogEntry> = listOf(
        LogEntryFactory.hsmOperation("KEY_GENERATION", 1, "SUCCESS"),
        LogEntryFactory.encryptionPerformed("3DES", "KEY_MASTER"),
        LogEntryFactory.decryptionPerformed("3DES", "KEY_MASTER"),
        LogEntryFactory.securityViolation("Invalid certificate", "192.168.1.200"),
        LogEntryFactory.hsmOperation("KEY_ROTATION", 1, "SUCCESS")
    )

    fun getAllSampleEntries(): List<LogEntry> =
        getTypicalTransactionFlow() +
                getErrorScenarios() +
                getDebugFlow() +
                getSecurityAuditTrail()
}



// Suggested Color Palette for ISO8583Studio Theme
object ISO8583Colors {
    // System Colors
    val InfoBlue = Color(0xFF42A5F5) // Blue 400
    val WarningOrange = Color(0xFFFF9800) // Orange 500
    val ErrorRed = Color(0xFFF44336) // Red 500
    val VerboseGrey = Color(0xFF78909C) // Blue Grey 400
    val DebugBrown = Color(0xFF8D6E63) // Brown 300
    val TraceLightGrey = Color(0xFFBDBDBD) // Grey 400

    // Transaction Colors
    val TransactionBlue = Color(0xFF1E88E5) // Blue 600
    val AuthGreen = Color(0xFF43A047) // Green 600
    val SettlementTeal = Color(0xFF00ACC1) // Cyan 600
    val ReversalOrange = Color(0xFFFF7043) // Deep Orange 400

    // Security Colors
    val SecurityPurple = Color(0xFF8E24AA) // Purple 600
    val EncryptionPink = Color(0xFFE91E63) // Pink 500
    val HSMDeepPurple = Color(0xFF5E35B1) // Deep Purple 600

    // Network Colors
    val ConnectionPurple = Color(0xFF673AB7) // Deep Purple 500
    val SocketIndigo = Color(0xFF3F51B5) // Indigo 500
    val ProtocolBlue = Color(0xFF2196F3) // Blue 500

    // Protocol Colors
    val MessageDarkBlue = Color(0xFF1565C0) // Blue 800
    val FieldGreen = Color(0xFF689F38) // Light Green 600
    val BitmapLime = Color(0xFFAFB42B) // Lime 600

    // Monitoring Colors
    val PerformanceOrange = Color(0xFFFF6F00) // Orange A700
    val MetricsLightBlue = Color(0xFF039BE5) // Light Blue 600
}



// Alternative icons for better visual variety
val alternativeIcons = mapOf(
    LogType.INFO to Icons.Default.Info,
    LogType.WARNING to Icons.Default.ReportProblem,
    LogType.ERROR to Icons.Default.ErrorOutline,
    LogType.VERBOSE to Icons.Default.VisibilityOff,
    LogType.DEBUG to Icons.Default.Construction,
    LogType.TRACE to Icons.Default.Route,

    LogType.TRANSACTION to Icons.Default.Payment,
    LogType.AUTHORIZATION to Icons.Default.GppGood,
    LogType.SETTLEMENT to Icons.Default.CurrencyExchange,
    LogType.REVERSAL to Icons.Default.RestoreFromTrash,

    LogType.SECURITY to Icons.Default.Shield,
    LogType.ENCRYPTION to Icons.Default.Key,
    LogType.HSM to Icons.Default.Memory,

    LogType.CONNECTION to Icons.Default.NetworkWifi,
    LogType.SOCKET to Icons.Default.Power,
    LogType.PROTOCOL to Icons.Default.Api,

    LogType.MESSAGE to Icons.Default.Email,
    LogType.FIELD to Icons.Default.Input,
    LogType.BITMAP to Icons.Default.ViewModule,

    LogType.PERFORMANCE to Icons.Default.Dashboard,
    LogType.METRICS to Icons.Default.Insights
)