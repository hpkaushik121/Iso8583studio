package `in`.aicortex.iso8583studio.hsm.payshield10k.data

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Audit log entry
 */
@Serializable
data class AuditEntry(
    val timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val entryType: AuditType,
    val command: String,
    val user: String = "CONSOLE",
    val lmkId: String = "",
    val result: String,
    val details: String = ""
)