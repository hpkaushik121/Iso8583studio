package `in`.aicortex.iso8583studio.platform

private fun jsNow(): Double = js("Date.now()")
private fun jsHours(): Int = js("new Date().getHours()")
private fun jsMinutes(): Int = js("new Date().getMinutes()")
private fun jsSeconds(): Int = js("new Date().getSeconds()")
private fun jsMillis(): Int = js("new Date().getMilliseconds()")

actual fun nowLogTimestamp(): String {
    fun two(v: Int) = v.toString().padStart(2, '0')
    return "${two(jsHours())}:${two(jsMinutes())}:${two(jsSeconds())}.${jsMillis().toString().padStart(3, '0')}"
}

actual fun epochMillis(): Long = jsNow().toLong()
