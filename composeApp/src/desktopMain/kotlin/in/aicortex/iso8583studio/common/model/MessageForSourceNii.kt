package `in`.aicortex.iso8583studio.data.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class MessageForSourceNii(
    var receiveTime: LocalDateTime? = null,
    var startTime: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    var message: ByteArray = ByteArray(0),
    var originalTpdu: ByteArray = ByteArray(5)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageForSourceNii

        if (receiveTime != other.receiveTime) return false
        if (startTime != other.startTime) return false
        if (!message.contentEquals(other.message)) return false
        if (!originalTpdu.contentEquals(other.originalTpdu)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = receiveTime?.hashCode() ?: 0
        result = 31 * result + startTime.hashCode()
        result = 31 * result + message.contentHashCode()
        result = 31 * result + originalTpdu.contentHashCode()
        return result
    }
}