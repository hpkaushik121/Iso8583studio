package `in`.aicortex.iso8583studio.data.model

import kotlinx.serialization.Serializable

/**
 * Logging configuration
 */
@Serializable
data class LogSettings(
    val fileName: String = "log.txt",
    val maxSizeMB: Int = 10,
    val loggingOption: LoggingOption = LoggingOption.NONE,
    val protocolType: String = "ISO8583",
    val encodings: List<MessageEncoding> = listOf(
        MessageEncoding.ASCII,
        MessageEncoding.BigEndianUnicode,
        MessageEncoding.ANSI,
        MessageEncoding.Unicode,
        MessageEncoding.UTF32,
        MessageEncoding.UTF7,
        MessageEncoding.UTF8
    ),
    val templateFile: String = ""
)
