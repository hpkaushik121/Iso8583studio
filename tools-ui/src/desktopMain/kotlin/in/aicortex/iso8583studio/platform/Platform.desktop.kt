package `in`.aicortex.iso8583studio.platform

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val LOG_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

actual fun nowLogTimestamp(): String = LocalDateTime.now().format(LOG_FORMAT)

actual fun epochMillis(): Long = System.currentTimeMillis()
