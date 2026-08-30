package `in`.aicortex.iso8583studio.domain.utils

fun isIpMatched(ip: String, pattern: String): Boolean {
    if (ip == "127.0.0.1") return false
    if (pattern == "*") return true

    pattern.split(",", ";").forEach { segment ->
        val range = segment.split("-")

        if (range.size == 2) {
            // IP range check
            if (compareIp(convertIpTo12Digit(range[0]), convertIpTo12Digit(ip)) <= 0 &&
                compareIp(convertIpTo12Digit(ip), convertIpTo12Digit(range[1])) <= 0) {
                return true
            }
        } else if (range.size == 1 && range[0] == ip) {
            // Exact match
            return true
        }
    }

    return false
}

private fun convertIpTo12Digit(ip: String): String {
    return ip.split(".").joinToString(".") { segment ->
        segment.padStart(3, '0')
    }
}

private fun compareIp(ip1: String, ip2: String): Int {
    return ip1.compareTo(ip2)
}