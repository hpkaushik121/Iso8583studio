package `in`.aicortex.iso8583studio.ui.navigation.stateConfigs

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.data.model.ConnectionStatus
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.domain.ImportResult
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.APDUSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.io.File
import java.nio.file.Files
import kotlin.String
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private var isLoaded = false

/**
 * Enum representing different types of simulators
 */
@Serializable
enum class SimulatorType(val displayName: String, val description: String) {
    HOST("Host Simulator", "ISO8583 Host Response Simulator"),
    HSM("HSM Simulator", "Hardware Security Module Simulator"),
    APDU("APDU Simulator", "Smart Card APDU Command Simulator"),
    POS("POS Simulator", "Point of Sale Terminal Simulator"),
    ECR("ECR Simulator", "Point of Sale Terminal Simulator"),
    ATM("ATM Simulator", "Automated Teller Machine Simulator"),
    CARD("Card Simulator", "Payment Card Simulator"),
    SWITCH("Switch Simulator", "Payment Network Switch Simulator"),
    ACQUIRER("Acquirer Simulator", "Acquiring Bank Simulator"),
    ISSUER("Issuer Simulator", "Card Issuing Bank Simulator")
}


/**
 * Unified Application State for all Simulator Configurations
 */
data class UnifiedSimulatorState(
    // All simulator configurations grouped by type
    val hostConfigs: MutableState<List<GatewayConfig>> = mutableStateOf(emptyList()),
    val hsmConfigs: MutableState<List<HSMSimulatorConfig>> = mutableStateOf(emptyList()),
    val apduConfigs: MutableState<List<APDUSimulatorConfig>> = mutableStateOf(emptyList()),
    val posConfigs: MutableState<List<POSSimulatorConfig>> = mutableStateOf(emptyList()),
    val ecrConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),
    val atmConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),
    val cardConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),
    val switchConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),
    val acquirerConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),
    val issuerConfigs: MutableState<List<String>> = mutableStateOf(emptyList()),

    // General state
    var resultDialogInterface: ResultDialogInterface? = null,
    var selectedConfigIndex: MutableState<MutableMap<SimulatorType, Int>> = mutableStateOf(mutableMapOf()),
    val selectedTabIndex: Int = 0,
    var panelWidth: Dp = 340.dp,
    var connectionStatus: ConnectionStatus? = null,
    var window: ComposeWindow? = null
) {
    private val name = "Iso8583Studio"

    // JSON configuration with polymorphic serialization
    private val json = Json {
        prettyPrint = true
        serializersModule = SerializersModule {
            polymorphic(SimulatorConfig::class) {
                subclass(GatewayConfig::class)
                subclass(HSMSimulatorConfig::class)
                subclass(APDUSimulatorConfig::class)
                subclass(POSSimulatorConfig::class)
            }
        }
    }

    init {
        if (!isLoaded) {
            load()
            isLoaded = true
        }
        SimulatorType.values().forEach {
                type -> selectedConfigIndex.value[type] = 0
        }
    }

    /**
     * Get all configurations as a unified list
     */
    fun getAllConfigs(): List<SimulatorConfig> {
        return hostConfigs.value + hsmConfigs.value + apduConfigs.value + posConfigs.value
    }

    /**
     * Get configurations by type
     */
    fun getConfigsByType(type: SimulatorType): List<SimulatorConfig> {
        return when (type) {
            SimulatorType.HOST -> hostConfigs.value
            SimulatorType.HSM -> hsmConfigs.value
            SimulatorType.APDU -> apduConfigs.value
            SimulatorType.POS -> posConfigs.value
            else -> emptyList() // For future simulator types
        }
    }
    
   
    /**
     * Get current selected configuration
     */
    fun currentConfig(simulatorType: SimulatorType): SimulatorConfig? {
        val configs = getConfigsByType(simulatorType)
        return if (selectedConfigIndex.value[simulatorType]!!>= 0 && selectedConfigIndex.value[simulatorType]!!< configs.size) {
            configs[selectedConfigIndex.value[simulatorType]!!]
        } else if (configs.isNotEmpty()) {
            selectedConfigIndex.value[simulatorType] = 0
            configs[0]
        } else {
            null
        }
    }

    /**
     * Add a new configuration (automatically detects type)
     */
    fun addConfig(config: SimulatorConfig) {
        when (config) {
            is GatewayConfig -> {
                hostConfigs.value = hostConfigs.value + config
                selectedConfigIndex.value[config.simulatorType] = hostConfigs.value.size - 1
            }

            is HSMSimulatorConfig -> {
                hsmConfigs.value = hsmConfigs.value + config
                selectedConfigIndex.value[config.simulatorType] = hsmConfigs.value.size - 1
            }

            is APDUSimulatorConfig -> {
                apduConfigs.value = apduConfigs.value + config
                selectedConfigIndex.value[config.simulatorType] = apduConfigs.value.size - 1
            }

            is POSSimulatorConfig -> {
                posConfigs.value = posConfigs.value + config
                selectedConfigIndex.value[config.simulatorType] = posConfigs.value.size - 1
            }
        }
        save()
    }

    /**
     * Update an existing configuration (automatically detects type)
     */
    fun updateConfig(config: SimulatorConfig?) {
        when (config) {
            is GatewayConfig -> {
                val index = hostConfigs.value.indexOfFirst { it.id == config.id }
                if (index >= 0) {
                    val newList = hostConfigs.value.toMutableList()
                    newList[index] = config.copy(modifiedDate = System.currentTimeMillis())
                    hostConfigs.value = newList
                }
            }

            is HSMSimulatorConfig -> {
                val index = hsmConfigs.value.indexOfFirst { it.id == config.id }
                if (index >= 0) {
                    val newList = hsmConfigs.value.toMutableList()
                    newList[index] = config.copy(modifiedDate = System.currentTimeMillis())
                    hsmConfigs.value = newList
                }
            }

            is APDUSimulatorConfig -> {
                val index = apduConfigs.value.indexOfFirst { it.id == config.id }
                if (index >= 0) {
                    val newList = apduConfigs.value.toMutableList()
                    newList[index] = config.copy(modifiedDate = System.currentTimeMillis())
                    apduConfigs.value = newList
                }
            }

            is POSSimulatorConfig -> {
                val index = posConfigs.value.indexOfFirst { it.id == config.id }
                if (index >= 0) {
                    val newList = posConfigs.value.toMutableList()
                    newList[index] = config.copy(modifiedDate = System.currentTimeMillis())
                    posConfigs.value = newList
                }
            }
        }
        save()
    }

    /**
     * Delete a configuration by ID (automatically detects type)
     */
    fun deleteConfig(configId: String): Boolean {
        // Try to find and delete from each type
        var found = false
        val type = getConfigType(configId)
        when(type){
            SimulatorType.HOST -> {
                // Check HOST configs
                val hostIndex = hostConfigs.value.indexOfFirst { it.id == configId }
                if (hostIndex >= 0) {
                    hostConfigs.value = hostConfigs.value.filter { it.id != configId }
                    selectedConfigIndex.value[type] = if (hostConfigs.value.isNotEmpty()) {
                        minOf(selectedConfigIndex.value[type]!!, hostConfigs.value.size - 1)
                    } else -1
                    found = true
                }
            }
            SimulatorType.HSM -> {
                // Check HSM configs
                val hsmIndex = hsmConfigs.value.indexOfFirst { it.id == configId }
                if (hsmIndex >= 0) {
                    hsmConfigs.value = hsmConfigs.value.filter { it.id != configId }
                    selectedConfigIndex.value[type] = if (hsmConfigs.value.isNotEmpty()) {
                        minOf(selectedConfigIndex.value[type]!!, hsmConfigs.value.size - 1)
                    } else -1
                    found = true
                }
            }
            SimulatorType.APDU -> {

                // Check APDU configs
                val apduIndex = apduConfigs.value.indexOfFirst { it.id == configId }
                if (apduIndex >= 0) {
                    apduConfigs.value = apduConfigs.value.filter { it.id != configId }
                    selectedConfigIndex.value[type] = if (apduConfigs.value.isNotEmpty()) {
                        minOf(selectedConfigIndex.value[type]!!, apduConfigs.value.size - 1)
                    } else -1
                    found = true
                }
            }
            SimulatorType.POS -> {
                // Check POS configs
                val posIndex = posConfigs.value.indexOfFirst { it.id == configId }
                if (posIndex >= 0) {
                    posConfigs.value = posConfigs.value.filter { it.id != configId }
                    selectedConfigIndex.value[type] = if (posConfigs.value.isNotEmpty()) {
                        minOf(selectedConfigIndex.value[type]!!, posConfigs.value.size - 1)
                    } else -1
                    found = true
                }

            }
            SimulatorType.ECR -> TODO()
            SimulatorType.ATM -> TODO()
            SimulatorType.CARD -> TODO()
            SimulatorType.SWITCH -> TODO()
            SimulatorType.ACQUIRER -> TODO()
            SimulatorType.ISSUER -> TODO()
            null -> TODO()
        }


        if (found) {
            save()
        }

        return found
    }

    /**
     * Delete the currently selected configuration
     */
    fun deleteCurrentConfig(simulatorType: SimulatorType): Boolean {
        return currentConfig(simulatorType)?.let { config ->
            deleteConfig(config.id)
        } ?: false
    }

    /**
     * Find configuration by ID across all types
     */
    fun findConfigById(configId: String): SimulatorConfig? {
        return getAllConfigs().find { it.id == configId }
    }

    /**
     * Check if configuration exists by ID
     */
    fun configExists(configId: String): Boolean {
        return findConfigById(configId) != null
    }

    /**
     * Get configuration type by ID
     */
    fun getConfigType(configId: String): SimulatorType? {
        return findConfigById(configId)?.simulatorType
    }

    /**
     * Enable/disable configuration by ID
     */
    fun setConfigEnabled(configId: String, enabled: Boolean): Boolean {
        val config = findConfigById(configId) ?: return false

        val updatedConfig = when (config) {
            is GatewayConfig -> config.copy(
                enabled = enabled,
                modifiedDate = System.currentTimeMillis()
            )

            is HSMSimulatorConfig -> config.copy(
                enabled = enabled,
                modifiedDate = System.currentTimeMillis()
            )

            is APDUSimulatorConfig -> config.copy(
                enabled = enabled,
                modifiedDate = System.currentTimeMillis()
            )

            is POSSimulatorConfig -> config.copy(
                enabled = enabled,
                modifiedDate = System.currentTimeMillis()
            )

            else -> null
        }

        updateConfig(updatedConfig)
        return true
    }

    /**
     * Duplicate a configuration with a new ID and name
     */
    fun duplicateConfig(configId: String, newName: String? = null): SimulatorConfig? {
        val originalConfig = findConfigById(configId) ?: return null
        val timestamp = System.currentTimeMillis()
        val duplicatedName = newName ?: "${originalConfig.name} (Copy)"

        val duplicatedConfig = when (originalConfig) {
            is GatewayConfig -> originalConfig.copy(
                id = generateConfigId(),
                name = duplicatedName,
                createdDate = timestamp,
                modifiedDate = timestamp
            )

            is HSMSimulatorConfig -> originalConfig.copy(
                id = generateConfigId(),
                name = duplicatedName,
                createdDate = timestamp,
                modifiedDate = timestamp
            )

            is APDUSimulatorConfig -> originalConfig.copy(
                id = generateConfigId(),
                name = duplicatedName,
                createdDate = timestamp,
                modifiedDate = timestamp
            )

            is POSSimulatorConfig -> originalConfig.copy(
                id = generateConfigId(),
                name = duplicatedName,
                createdDate = timestamp,
                modifiedDate = timestamp
            )

            else -> null
        }

        duplicatedConfig?.let { addConfig(it) }
        return duplicatedConfig
    }

    /**
     * Batch operations for multiple configurations
     */
    fun deleteConfigs(configIds: List<String>): Int {
        var deletedCount = 0
        configIds.forEach { configId ->
            if (deleteConfig(configId)) {
                deletedCount++
            }
        }
        return deletedCount
    }

    /**
     * Enable/disable multiple configurations
     */
    fun setConfigsEnabled(configIds: List<String>, enabled: Boolean): Int {
        var updatedCount = 0
        configIds.forEach { configId ->
            if (setConfigEnabled(configId, enabled)) {
                updatedCount++
            }
        }
        return updatedCount
    }

    /**
     * Get configurations by enabled status
     */
    fun getEnabledConfigs(): List<SimulatorConfig> {
        return getAllConfigs().filter { it.enabled }
    }

    fun getDisabledConfigs(): List<SimulatorConfig> {
        return getAllConfigs().filter { !it.enabled }
    }

    /**
     * Search configurations by name or description
     */
    fun searchConfigs(query: String): List<SimulatorConfig> {
        val lowercaseQuery = query.lowercase()
        return getAllConfigs().filter { config ->
            config.name.lowercase().contains(lowercaseQuery) ||
                    config.description.lowercase().contains(lowercaseQuery)
        }
    }

    /**
     * Get configurations sorted by various criteria
     */
    fun getConfigsSortedByName(): List<SimulatorConfig> {
        return getAllConfigs().sortedBy { it.name.lowercase() }
    }

    fun getConfigsSortedByType(): List<SimulatorConfig> {
        return getAllConfigs().sortedBy { it.simulatorType.name }
    }

    fun getConfigsSortedByModified(): List<SimulatorConfig> {
        return getAllConfigs().sortedByDescending { it.modifiedDate }
    }

    fun getConfigsSortedByCreated(): List<SimulatorConfig> {
        return getAllConfigs().sortedByDescending { it.createdDate }
    }

    /**
     * Select configuration by ID (automatically sets type and index)
     */
    fun selectConfig(configId: String): Boolean {
        val config = findConfigById(configId) ?: return false
        val configs = getConfigsByType(config.simulatorType)
        selectedConfigIndex.value[config.simulatorType] = configs.indexOfFirst { it.id == configId }

        return selectedConfigIndex.value[config.simulatorType]!! >= 0
    }

    /**
     * Generate a unique configuration ID
     */
    @OptIn(ExperimentalUuidApi::class)
    fun generateConfigId(): String {
        return Uuid.random().toHexDashString()
    }

    /**
     * Validate configuration before saving
     */
    private fun validateConfig(config: SimulatorConfig): Boolean {
        return config.name.isNotBlank() && config.id.isNotEmpty()
    }

    /**
     * Export all configurations
     */
    fun export(): String {
        return try {
            save()
            val allConfigs = SimulatorConfigCollection(
                hostConfigs = hostConfigs.value,
                hsmConfigs = hsmConfigs.value,
                apduConfigs = apduConfigs.value,
                posConfigs = posConfigs.value,
                ecrConfigs = ecrConfigs.value,
                atmConfigs = atmConfigs.value,
                cardConfigs = cardConfigs.value,
                switchConfigs = switchConfigs.value,
                issuerConfigs = issuerConfigs.value,
                acquirerConfigs = acquirerConfigs.value,
                exportedAt = System.currentTimeMillis(),
                version = "1.0"
            )
            json.encodeToString(SimulatorConfigCollection.serializer(), allConfigs)
        } catch (e: Exception) {
            e.message ?: "Unable to export configurations"
        }
    }

    /**
     * Export specific simulator type configurations
     */
    fun exportByType(type: SimulatorType): String {
        return try {
            val configs = getConfigsByType(type)
            json.encodeToString(configs)
        } catch (e: Exception) {
            e.message ?: "Unable to export $type configurations"
        }
    }

    /**
     * Load configurations from file
     */
    fun load() {
        val file = File("${name}_simulators.json")
        import(file)
    }

    /**
     * Save all configurations to file
     */
    fun save(): Boolean {
        return try {
            val file = File("${name}_simulators.json")
            val allConfigs = SimulatorConfigCollection(
                hostConfigs = hostConfigs.value,
                hsmConfigs = hsmConfigs.value,
                apduConfigs = apduConfigs.value,
                posConfigs = posConfigs.value,
                ecrConfigs = ecrConfigs.value,
                atmConfigs = atmConfigs.value,
                cardConfigs = cardConfigs.value,
                switchConfigs = switchConfigs.value,
                acquirerConfigs = acquirerConfigs.value,
                issuerConfigs = issuerConfigs.value,
                exportedAt = System.currentTimeMillis(),
                version = "1.0"
            )
            val configJson = json.encodeToString(SimulatorConfigCollection.serializer(), allConfigs)
            file.writeBytes(configJson.toByteArray())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Import configurations from file
     */
    fun import(file: File): ImportResult {
        return try {
            if (!file.exists()) {
                return ImportResult.Success(
                    fileExtension = "json",
                    fileName = file.name,
                    fileSize = 0L,
                    fileContent = ByteArray(0)
                )
            }

            val fileContent = Files.readAllBytes(file.toPath())
            val configCollection =
                json.decodeFromString<SimulatorConfigCollection>(String(fileContent))

            // Load configurations
            SimulatorType.values().forEach {
                when(it){
                    SimulatorType.HOST -> {
                        hostConfigs.value = configCollection.hostConfigs
                    }
                    SimulatorType.HSM -> {
                        hsmConfigs.value = configCollection.hsmConfigs
                    }
                    SimulatorType.APDU -> {
                        apduConfigs.value = configCollection.apduConfigs
                    }
                    SimulatorType.POS -> {
                        posConfigs.value = configCollection.posConfigs
                    }

                    SimulatorType.ECR -> {
                        ecrConfigs.value = configCollection.ecrConfigs
                    }
                    SimulatorType.ATM -> {
                        atmConfigs.value = configCollection.atmConfigs
                    }
                    SimulatorType.CARD -> {
                        cardConfigs.value = configCollection.cardConfigs
                    }
                    SimulatorType.SWITCH -> {
                        switchConfigs.value = configCollection.switchConfigs
                    }
                    SimulatorType.ACQUIRER -> {
                        acquirerConfigs.value = configCollection.acquirerConfigs
                    }
                    SimulatorType.ISSUER -> {
                        issuerConfigs.value = configCollection.issuerConfigs
                    }
                }
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

    /**
     * Import specific simulator type configurations
     */
    fun importByType(file: File, type: SimulatorType): ImportResult {
        return try {
            val fileContent = Files.readAllBytes(file.toPath())

            when (type) {
                SimulatorType.HOST -> {
                    val configs = json.decodeFromString<List<GatewayConfig>>(String(fileContent))
                    hostConfigs.value = hostConfigs.value + configs
                }

                SimulatorType.HSM -> {
                    val configs =
                        json.decodeFromString<List<HSMSimulatorConfig>>(String(fileContent))
                    hsmConfigs.value = hsmConfigs.value + configs
                }

                SimulatorType.APDU -> {
                    val configs =
                        json.decodeFromString<List<APDUSimulatorConfig>>(String(fileContent))
                    apduConfigs.value = apduConfigs.value + configs
                }

                SimulatorType.POS -> {
                    val configs =
                        json.decodeFromString<List<POSSimulatorConfig>>(String(fileContent))
                    posConfigs.value = posConfigs.value + configs
                }

                else -> {
                    return ImportResult.Error(
                        "Unsupported simulator type: $type",
                        IllegalArgumentException()
                    )
                }
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

    /**
     * Get configuration statistics
     */
    fun getStatistics(): ConfigurationStatistics {
        return ConfigurationStatistics(
            totalConfigs = getAllConfigs().size,
            hostConfigCount = hostConfigs.value.size,
            hsmConfigCount = hsmConfigs.value.size,
            apduConfigCount = apduConfigs.value.size,
            posConfigCount = posConfigs.value.size,
            enabledConfigs = getAllConfigs().count { it.enabled },
            lastModified = getAllConfigs().maxOfOrNull { it.modifiedDate } ?: 0L
        )
    }

    fun setComposableWindow(window: ComposeWindow) {
        this.window = window
    }
}

/**
 * Collection wrapper for serialization
 */
@Serializable
data class SimulatorConfigCollection(
    val hostConfigs: List<GatewayConfig>,
    val hsmConfigs: List<HSMSimulatorConfig>,
    val apduConfigs: List<APDUSimulatorConfig>,
    val posConfigs: List<POSSimulatorConfig>,
    val ecrConfigs: List<String>,
    val atmConfigs: List<String>,
    val cardConfigs: List<String>,
    val switchConfigs: List<String>,
    val acquirerConfigs: List<String>,
    val issuerConfigs: List<String>,
    val exportedAt: Long,
    val version: String
)

/**
 * Configuration statistics for dashboard/overview
 */
data class ConfigurationStatistics(
    val totalConfigs: Int,
    val hostConfigCount: Int,
    val hsmConfigCount: Int,
    val apduConfigCount: Int,
    val posConfigCount: Int,
    val enabledConfigs: Int,
    val lastModified: Long
)