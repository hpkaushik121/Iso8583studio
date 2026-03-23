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

@Serializable
private data class AppSettingsData(
    val enableGlobalLogging: Boolean = true,
    val autoClearLogsEnabled: Boolean = true,
    val autoClearLogsIntervalMinutes: Int = 5,
    val deleteLogFileOnClear: Boolean = true
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

    private fun load() {
        if (!prefsFile.exists()) return
        try {
            val data = json.decodeFromString<AppSettingsData>(prefsFile.readText())
            _enableGlobalLogging = data.enableGlobalLogging
            _autoClearLogsEnabled = data.autoClearLogsEnabled
            _autoClearLogsIntervalMinutes = data.autoClearLogsIntervalMinutes
            _deleteLogFileOnClear = data.deleteLogFileOnClear
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
                    deleteLogFileOnClear = _deleteLogFileOnClear
                )
                prefsFile.writeText(json.encodeToString(AppSettingsData.serializer(), data))
            } catch (_: Exception) { }
        }
    }
}
