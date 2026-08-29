package `in`.aicortex.iso8583studio.data.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Serializable
private data class AppSettingsData(
    val enableGlobalLogging: Boolean = true,
    val autoClearLogsEnabled: Boolean = true,
    val autoClearLogsIntervalMinutes: Int = 5,
    val deleteLogFileOnClear: Boolean = true,
    /** Serialized [AnalyticsConsent]. Stored as a String so unknown future values degrade to UNSET. */
    val analyticsConsent: String = "UNSET",
    /** Random, regenerable device identifier used as the GA4 user_id. Not derived from any hardware ID. */
    val analyticsClientId: String = "",
    val installDate: String = "",
    val launchCount: Int = 0
)

/**
 * Global application settings, persisted to ~/.iso8583studio/app_settings.json.
 * All fields are reactive Compose state so UI recomposes on change.
 */
object AppSettings {

    private val prefsDir = File(System.getProperty("user.home"), ".iso8583studio")
    private val prefsFile = File(prefsDir, "app_settings.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private var _enableGlobalLogging by mutableStateOf(true)
    val enableGlobalLogging: Boolean get() = _enableGlobalLogging

    private var _autoClearLogsEnabled by mutableStateOf(true)
    val autoClearLogsEnabled: Boolean get() = _autoClearLogsEnabled

    private var _autoClearLogsIntervalMinutes by mutableStateOf(5)
    val autoClearLogsIntervalMinutes: Int get() = _autoClearLogsIntervalMinutes

    private var _deleteLogFileOnClear by mutableStateOf(true)
    val deleteLogFileOnClear: Boolean get() = _deleteLogFileOnClear

    private var _analyticsConsent by mutableStateOf(AnalyticsConsent.UNSET)
    val analyticsConsent: AnalyticsConsent get() = _analyticsConsent

    private var _analyticsClientId by mutableStateOf("")
    val analyticsClientId: String get() = _analyticsClientId

    private var _installDate by mutableStateOf("")
    val installDate: String get() = _installDate

    private var _launchCount by mutableStateOf(0)
    val launchCount: Int get() = _launchCount

    init {
        load()
    }

    fun updateEnableGlobalLogging(value: Boolean) {
        _enableGlobalLogging = value
        persistAsync()
    }

    fun updateAutoClearLogsEnabled(value: Boolean) {
        _autoClearLogsEnabled = value
        persistAsync()
    }

    fun updateAutoClearLogsIntervalMinutes(value: Int) {
        _autoClearLogsIntervalMinutes = value.coerceIn(1, 1440)
        persistAsync()
    }

    fun updateDeleteLogFileOnClear(value: Boolean) {
        _deleteLogFileOnClear = value
        persistAsync()
    }

    /**
     * Records the user's analytics choice. Granting also mints a client id if one is not
     * already held, so identity survives even if the tracker library's own cache
     * (which lives in the system temp directory) is purged by the OS.
     */
    fun updateAnalyticsConsent(value: AnalyticsConsent) {
        _analyticsConsent = value
        if (value == AnalyticsConsent.GRANTED && _analyticsClientId.isBlank()) {
            _analyticsClientId = UUID.randomUUID().toString()
        }
        persistAsync()
    }

    /** Mints a fresh device identifier, discarding any association with previously sent data. */
    fun regenerateAnalyticsClientId(): String {
        val id = UUID.randomUUID().toString()
        _analyticsClientId = id
        persistAsync()
        return id
    }

    /** Stamps the install date on first run and counts launches. Safe to call repeatedly. */
    fun recordLaunch() {
        if (_installDate.isBlank()) _installDate = LocalDate.now().toString()
        _launchCount += 1
        persistAsync()
    }

    /** Days since first launch, or 0 if unknown. */
    fun daysSinceInstall(): Long =
        runCatching { ChronoUnit.DAYS.between(LocalDate.parse(_installDate), LocalDate.now()) }
            .getOrDefault(0L)
            .coerceAtLeast(0L)

    private fun load() {
        if (!prefsFile.exists()) return
        try {
            val data = json.decodeFromString<AppSettingsData>(prefsFile.readText())
            _enableGlobalLogging = data.enableGlobalLogging
            _autoClearLogsEnabled = data.autoClearLogsEnabled
            _autoClearLogsIntervalMinutes = data.autoClearLogsIntervalMinutes
            _deleteLogFileOnClear = data.deleteLogFileOnClear
            _analyticsConsent = AnalyticsConsent.parse(data.analyticsConsent)
            _analyticsClientId = data.analyticsClientId
            _installDate = data.installDate
            _launchCount = data.launchCount
        } catch (_: Exception) { }
    }

    private fun persistAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                prefsDir.mkdirs()
                val data = AppSettingsData(
                    enableGlobalLogging = _enableGlobalLogging,
                    autoClearLogsEnabled = _autoClearLogsEnabled,
                    autoClearLogsIntervalMinutes = _autoClearLogsIntervalMinutes,
                    deleteLogFileOnClear = _deleteLogFileOnClear,
                    analyticsConsent = _analyticsConsent.name,
                    analyticsClientId = _analyticsClientId,
                    installDate = _installDate,
                    launchCount = _launchCount
                )
                prefsFile.writeText(json.encodeToString(AppSettingsData.serializer(), data))
            } catch (_: Exception) { }
        }
    }
}
