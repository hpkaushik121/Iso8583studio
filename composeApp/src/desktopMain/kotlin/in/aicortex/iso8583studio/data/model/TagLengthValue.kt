package `in`.aicortex.iso8583studio.data.model

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.SortedMap


class TagLengthValue(private val buffer: ByteArray) {
    var currentPosition: Int = 0

    companion object {
        const val BUFFER_LENGTH = 1024
        private var _AllowedTAGs: Array<UShort>? = null

        val allowedTAGs: Array<UShort>
            get() {
                if (_AllowedTAGs == null) {
                    val values = GWHeaderTAG.values()
                    _AllowedTAGs = Array(values.size) { i -> values[i].value }
                }
                return _AllowedTAGs!!
            }

        fun getTAG(stream: InputStream): Pair<GWHeaderTAG, ByteArray?> {
            val buffer = ByteArray(2)
            val bytesRead = stream.read(buffer, 0, 2)

            if (bytesRead < 2) {
                throw VerificationException("CAN NOT READ THE NETWORK STREAM", VerificationError.WRONG_HEADER)
            }

            val tagValue = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).short.toUShort()
            val tag = GWHeaderTAG.values().find { it.value == tagValue }
                ?: throw VerificationException("TAG $tagValue IS INVALID", VerificationError.WRONG_HEADER)

            if (allowedTAGs.binarySearch(tagValue) < 0) {
                throw VerificationException("TAG ${tag.name} IS INVALID", VerificationError.WRONG_HEADER)
            }

            var value: ByteArray? = null
            if (tag == GWHeaderTAG.TAG_START_INDICATOR || tag == GWHeaderTAG.TAG_END_INDICATOR) {
                val count = stream.read()
                if (count < 1) {
                    throw VerificationException("CAN NOT READ THE NETWORK STREAM", VerificationError.WRONG_HEADER)
                }

                value = ByteArray(count)
                if (stream.read(value, 0, count) < count) {
                    throw VerificationException("CAN NOT READ THE NETWORK STREAM", VerificationError.WRONG_HEADER)
                }
            }

            return Pair(tag, value)
        }

        fun getTAGs(buf: ByteArray, start: Int): Pair<SortedMap<GWHeaderTAG, ByteArray?>, Int> {
            val tags = sortedMapOf<GWHeaderTAG, ByteArray?>()
            val tlv = TagLengthValue(buf)
            tlv.currentPosition = start

            var tagResult = tlv.getTAG()
            if (tagResult.first != GWHeaderTAG.TAG_START_INDICATOR) {
                throw VerificationException("NOT FOUND START INDICATOR", VerificationError.WRONG_HEADER)
            }

            do {
                tagResult = tlv.getTAG()
                val (tag, value) = tagResult

                if (tags.containsKey(tag)) {
                    throw VerificationException("TAG ${tag.value.toString(16).uppercase()} ALREADY EXISTS", VerificationError.WRONG_HEADER)
                }

                tags[tag] = value
            } while (tag != GWHeaderTAG.TAG_END_INDICATOR)

            return Pair(tags, tlv.currentPosition)
        }

        fun packTAGs(tags: SortedMap<GWHeaderTAG, ByteArray?>): ByteArray {
            var totalSize = 0
            for (value in tags.values) {
                totalSize += 3 + (value?.size ?: 0)
            }

            val buf = ByteArray(totalSize + 4)
            val tlv = TagLengthValue(buf)

            tlv.setTAG(GWHeaderTAG.TAG_START_INDICATOR, null)

            for ((key, value) in tags) {
                tlv.setTAG(key, value)
            }

            tlv.setTAG(GWHeaderTAG.TAG_END_INDICATOR, null)

            return buf
        }
    }

    fun setTAG(tag: GWHeaderTAG, value: ByteArray?) {
        buffer[currentPosition] = (tag.value.toInt() / 256).toByte()
        buffer[currentPosition + 1] = (tag.value.toInt() % 256).toByte()
        currentPosition += 2

        if (value != null) {
            buffer[currentPosition] = value.size.toByte()
            currentPosition++
            value.copyInto(buffer, currentPosition, 0, value.size)
            currentPosition += value.size
        }
    }

    fun getTAG(): Pair<GWHeaderTAG, ByteArray?> {
        val tagValue = ((buffer[currentPosition].toInt() and 0xFF) * 256 +
                (buffer[currentPosition + 1].toInt() and 0xFF)).toUShort()
        currentPosition += 2

        val tag = GWHeaderTAG.values().find { it.value == tagValue }
            ?: throw VerificationException("Invalid TAG: $tagValue", VerificationError.WRONG_HEADER)

        var value: ByteArray? = null

        if (tag != GWHeaderTAG.TAG_START_INDICATOR && tag != GWHeaderTAG.TAG_END_INDICATOR) {
            val count = buffer[currentPosition].toInt() and 0xFF
            currentPosition++

            value = ByteArray(count)
            for (i in 0 until count) {
                value[i] = buffer[currentPosition + i]
            }
            currentPosition += count
        }

        return Pair(tag, value)
    }
}