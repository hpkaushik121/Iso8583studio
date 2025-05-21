package `in`.aicortex.iso8583studio.ui.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.model.ConnectionStatus
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.domain.ImportResult
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.serialization.XML
import java.io.File
import java.nio.file.Files
private var isLoaded = false
/**
 * Application state for the Gateway Configuration
 */
data class GatewayConfigurationState(
    val configList: MutableState<List<GatewayConfig>> = mutableStateOf(emptyList()),
    var resultDialogInterface: ResultDialogInterface? = null,
    var selectedConfigIndex: Int = -1,
    val selectedTabIndex: Int = 0,
    var panelWidth: Dp = 300.dp,
    var connectionStatus: ConnectionStatus? = null
) {

    private val name = "Iso8583Studio"
    fun export(): String {
        try {
            save()
            val config = Json.encodeToString(configList.value)
            return config
        } catch (e: Exception) {
            // Ignore write errors
            return e.message ?: "Unable to export"
        }

    }

    init {
        if (!isLoaded) {
            load()
            isLoaded = true
        }
    }

    fun load() {
        val file = File("${name}.cfg")
        import(file)
    }

    fun save(): Boolean {
        try {
            val file = File("${name}.cfg")
            val config = Json.encodeToString(configList.value)
            file.writeBytes(config.toByteArray())
            return true
        } catch (e: Exception) {
            // Ignore write errors
            e.printStackTrace()
        }
        return false
    }

    fun import(file: File): ImportResult {
        return try {
            val fileContent = Files.readAllBytes(file.toPath())
            val confList = Json.decodeFromString<List<GatewayConfig>>(String(fileContent))
            if (confList.isNotEmpty()) {
                configList.value = confList
                selectedConfigIndex = 0
            }
            save()
            ImportResult.Success(
                fileExtension = file.extension,
                fileName = file.name,
                fileSize = file.length(),
                fileContent = fileContent
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult.Error(
                message = e.message ?: "Unknown error",
                exception = e
            )
        }

    }


    val currentConfig: GatewayConfig?
        get() = if (selectedConfigIndex >= 0 && selectedConfigIndex < configList.value.size) {
            configList.value[selectedConfigIndex]
        } else {
            null
        }
}

