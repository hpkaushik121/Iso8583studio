package `in`.aicortex.iso8583studio.data.model

/**
 * Whether the user has agreed to anonymous usage analytics.
 *
 * Three states rather than a boolean: [UNSET] must be distinguishable from [DENIED],
 * otherwise the first-run consent dialog would reappear on every launch for someone
 * who already declined.
 */
enum class AnalyticsConsent {
    UNSET,
    GRANTED,
    DENIED;

    val isGranted: Boolean get() = this == GRANTED

    companion object {
        fun parse(value: String?): AnalyticsConsent =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNSET
    }
}
