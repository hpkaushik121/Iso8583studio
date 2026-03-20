package `in`.aicortex.iso8583studio.license

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * NTP time cross-check to detect system clock manipulation.
 *
 * Queries NTP servers (time.google.com, pool.ntp.org) and compares
 * the returned time against System.currentTimeMillis().
 * If the delta exceeds the threshold, flags clock as suspicious.
 *
 * Fallback: if NTP is unreachable (offline), compares against the
 * last known server timestamp from cached license response.
 */
object NtpTimeVerifier {

    private const val NTP_PORT = 123
    private const val NTP_PACKET_SIZE = 48
    private const val TIMEOUT_MS = 5000
    private const val MAX_ALLOWED_DRIFT_MS = 5 * 60 * 1000L // 5 minutes

    private val ntpServers = listOf(
        "time.google.com",
        "pool.ntp.org",
        "time.cloudflare.com"
    )

    @Volatile
    private var lastVerifiedTime: Long = 0L

    @Volatile
    private var clockSuspicious = false

    val isClockSuspicious: Boolean get() = clockSuspicious

    fun verify(): Boolean {
        val ntpTime = queryNtpTime()
        if (ntpTime != null) {
            val systemTime = System.currentTimeMillis()
            val drift = kotlin.math.abs(systemTime - ntpTime)
            lastVerifiedTime = ntpTime
            clockSuspicious = drift > MAX_ALLOWED_DRIFT_MS
            return !clockSuspicious
        }

        return verifyAgainstCachedTimestamp()
    }

    private fun verifyAgainstCachedTimestamp(): Boolean {
        val stored = LicenseStorage.load() ?: return true
        if (stored.lastServerTimestamp <= 0L) return true
        val now = System.currentTimeMillis()
        if (now < stored.lastServerTimestamp - MAX_ALLOWED_DRIFT_MS) {
            clockSuspicious = true
            return false
        }
        return true
    }

    private fun queryNtpTime(): Long? {
        for (server in ntpServers) {
            val time = queryServer(server)
            if (time != null) return time
        }
        return null
    }

    private fun queryServer(host: String): Long? {
        return try {
            val address = InetAddress.getByName(host)
            val buffer = ByteArray(NTP_PACKET_SIZE)
            buffer[0] = 0x1B // LI=0, VN=3, Mode=3 (client)

            val socket = DatagramSocket()
            socket.soTimeout = TIMEOUT_MS
            try {
                val request = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
                socket.send(request)

                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)

                val transmitTimestamp = extractTimestamp(buffer, 40)
                transmitTimestamp
            } finally {
                socket.close()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extract a 64-bit NTP timestamp from the buffer at the given offset
     * and convert to Unix millis.
     * NTP epoch: Jan 1, 1900; Unix epoch: Jan 1, 1970.
     */
    private fun extractTimestamp(buffer: ByteArray, offset: Int): Long {
        val seconds = ((buffer[offset].toLong() and 0xFF) shl 24) or
                ((buffer[offset + 1].toLong() and 0xFF) shl 16) or
                ((buffer[offset + 2].toLong() and 0xFF) shl 8) or
                (buffer[offset + 3].toLong() and 0xFF)

        val fraction = ((buffer[offset + 4].toLong() and 0xFF) shl 24) or
                ((buffer[offset + 5].toLong() and 0xFF) shl 16) or
                ((buffer[offset + 6].toLong() and 0xFF) shl 8) or
                (buffer[offset + 7].toLong() and 0xFF)

        val ntpToUnixOffset = 2208988800L
        val unixSeconds = seconds - ntpToUnixOffset
        val millis = (fraction * 1000L) / 0x100000000L
        return unixSeconds * 1000L + millis
    }
}
