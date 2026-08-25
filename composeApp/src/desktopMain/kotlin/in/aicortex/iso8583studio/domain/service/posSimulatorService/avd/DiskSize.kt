package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

/**
 * A partition / SD-card size as the emulator expresses it.
 *
 * AVD tooling is inconsistent about the form it writes: Android Studio emits `sdcard.size = 512M`
 * but `disk.dataPartition.size = 6442450944`, and both are accepted on input. We parse both and
 * emit the most compact exact unit.
 *
 * Units are binary (1 K = 1024), matching the emulator: a real `config.ini` written by Studio for
 * a 6 GB data partition contains exactly 6442450944 = 6 * 1024^3.
 *
 * Note for callers: [ConfigIni] deliberately preserves the raw strings it read, so round-tripping
 * an untouched file does not silently rewrite `6442450944` as `6G`. Normalisation happens only for
 * values we actually change.
 */
@JvmInline
value class DiskSize(val bytes: Long) : Comparable<DiskSize> {

    val megabytes: Long get() = bytes / MB

    override fun compareTo(other: DiskSize): Int = bytes.compareTo(other.bytes)

    /** The most compact exact representation, e.g. 6442450944 -> "6G", 0 -> "0". */
    override fun toString(): String {
        if (bytes == 0L) return "0"
        for ((unit, suffix) in UNITS) {
            if (bytes % unit == 0L) return "${bytes / unit}$suffix"
        }
        return bytes.toString()
    }

    companion object {
        const val KB = 1024L
        const val MB = KB * 1024
        const val GB = MB * 1024
        const val TB = GB * 1024

        /** Largest first, so [toString] picks the most compact exact unit. */
        private val UNITS = listOf(TB to "T", GB to "G", MB to "M", KB to "K")

        fun ofMegabytes(mb: Long) = DiskSize(mb * MB)

        /**
         * Accepts `512M`, `6G`, `66MB`, `9m`, `6442450944`, `0`. Case-insensitive, an optional
         * trailing `B` is ignored, surrounding whitespace is tolerated.
         *
         * Returns null for anything unparseable — callers surface that as a validation error
         * rather than silently defaulting, because a mistyped partition size otherwise shows up
         * as a mysterious boot failure.
         */
        fun parse(raw: String): DiskSize? {
            val s = raw.trim()
            if (s.isEmpty()) return null

            var end = s.length
            // Strip an optional trailing "b"/"B" so "66MB" and "66M" both work.
            if (s[end - 1].lowercaseChar() == 'b') end--
            if (end == 0) return null

            val multiplier = when (s[end - 1].lowercaseChar()) {
                'k' -> KB
                'm' -> MB
                'g' -> GB
                't' -> TB
                else -> null
            }
            val digits = if (multiplier != null) s.substring(0, end - 1) else s.substring(0, end)
            val value = digits.trim().toLongOrNull() ?: return null
            if (value < 0) return null

            val factor = multiplier ?: 1L
            // Guard against overflow on absurd input like "99999999999999T".
            if (value != 0L && value > Long.MAX_VALUE / factor) return null
            return DiskSize(value * factor)
        }
    }
}
