package `in`.aicortex.iso8583studio.license

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Centralized license warning logger.
 * Writes to ~/.iso8583studio/license_warnings.log and provides
 * callbacks for simulators to inject warnings into their own logs.
 */
object LicenseWarningLogger {

    private val logFile = File(System.getProperty("user.home"), ".iso8583studio/license_warnings.log")
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Volatile
    var onWarningCallback: ((String) -> Unit)? = null

    fun logWarning(message: String) {
        val timestamp = LocalDateTime.now().format(formatter)
        val entry = "[$timestamp] WARNING: $message"

        try {
            logFile.parentFile?.mkdirs()
            logFile.appendText("$entry\n")
        } catch (_: Exception) { }

        onWarningCallback?.invoke(message)
    }

    fun logError(message: String) {
        val timestamp = LocalDateTime.now().format(formatter)
        val entry = "[$timestamp] ERROR: $message"

        try {
            logFile.parentFile?.mkdirs()
            logFile.appendText("$entry\n")
        } catch (_: Exception) { }
    }

    fun logInfo(message: String) {
        val timestamp = LocalDateTime.now().format(formatter)
        val entry = "[$timestamp] INFO: $message"

        try {
            logFile.parentFile?.mkdirs()
            logFile.appendText("$entry\n")
        } catch (_: Exception) { }
    }
}
