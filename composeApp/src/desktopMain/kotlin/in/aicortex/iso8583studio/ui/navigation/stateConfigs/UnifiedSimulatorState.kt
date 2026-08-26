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
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmCommandConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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
    ISSUER("Issuer Simulator", "Card Issuing Bank Simulator"),
    HSM_COMMAND("HSM Host Console", "HSM Host Console Client"),
    /** Lightweight tab for any non-simulator studio tool (converters, parsers, etc.) */
    TOOL("Tool", "Studio Tool")
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
    val hsmCommandConfigs: MutableState<List<HsmCommandConfig>> = mutableStateOf(emptyList()),
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
        // A config written by a newer build must still load here: an unknown key would otherwise
        // throw, import() would swallow it, and the next save() would overwrite every config with
        // an empty list.
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            polymorphic(SimulatorConfig::class) {
                subclass(GatewayConfig::class)
                subclass(HSMSimulatorConfig::class)
                subclass(APDUSimulatorConfig::class)
                subclass(POSSimulatorConfig::class)
                subclass(HsmCommandConfig::class)
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
        return hostConfigs.value + hsmConfigs.value + apduConfigs.value + posConfigs.value + hsmCommandConfigs.value
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
            SimulatorType.HSM_COMMAND -> hsmCommandConfigs.value
            else -> emptyList()
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

            is HsmCommandConfig -> {
                hsmCommandConfigs.value = hsmCommandConfigs.value + config
                selectedConfigIndex.value[config.simulatorType] = hsmCommandConfigs.value.size - 1
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

            is HsmCommandConfig -> {
                val index = hsmCommandConfigs.value.indexOfFirst { it.id == config.id }
                if (index >= 0) {
                    val newList = hsmCommandConfigs.value.toMutableList()
                    newList[index] = config.copy(modifiedDate = System.currentTimeMillis())
                    hsmCommandConfigs.value = newList
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
            SimulatorType.HSM_COMMAND -> {
                val idx = hsmCommandConfigs.value.indexOfFirst { it.id == configId }
                if (idx >= 0) {
                    hsmCommandConfigs.value = hsmCommandConfigs.value.filter { it.id != configId }
                    selectedConfigIndex.value[type] = if (hsmCommandConfigs.value.isNotEmpty()) {
                        minOf(selectedConfigIndex.value[type]!!, hsmCommandConfigs.value.size - 1)
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
            SimulatorType.TOOL -> { /* Tool tabs have no persisted config */ }
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

            is HsmCommandConfig -> config.copy(
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

            is HsmCommandConfig -> originalConfig.copy(
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
                hsmCommandConfigs = hsmCommandConfigs.value,
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
     * Serialize a single configuration to a self-describing envelope.
     *
     * Unlike [export] (whole workspace) and [exportByType] (every profile of one type),
     * this produces exactly one profile. The envelope records the simulator type so that
     * [importProfile] can route it back without the user having to say where it belongs.
     */
    fun exportProfile(config: SimulatorConfig): ProfileTransferResult {
        return try {
            val profile = when (config) {
                is GatewayConfig -> json.encodeToJsonElement(GatewayConfig.serializer(), config)
                is HSMSimulatorConfig -> json.encodeToJsonElement(HSMSimulatorConfig.serializer(), config)
                is APDUSimulatorConfig -> json.encodeToJsonElement(APDUSimulatorConfig.serializer(), config)
                is POSSimulatorConfig -> json.encodeToJsonElement(POSSimulatorConfig.serializer(), config)
                is HsmCommandConfig -> json.encodeToJsonElement(HsmCommandConfig.serializer(), config)
                else -> return ProfileTransferResult.Error(
                    "${config.simulatorType.displayName} profiles cannot be exported yet."
                )
            }
            val envelope = SimulatorProfileExport(
                simulatorType = config.simulatorType,
                profileName = config.name,
                exportedAt = System.currentTimeMillis(),
                version = PROFILE_EXPORT_VERSION,
                profile = profile
            )
            ProfileTransferResult.Success(
                content = json.encodeToString(SimulatorProfileExport.serializer(), envelope),
                profileName = config.name,
                simulatorType = config.simulatorType
            )
        } catch (e: Throwable) {
            // Throwable, not Exception: kotlinx-serialization resolves the generated child
            // serializers lazily, so a stale or mismatched build surfaces as NoClassDefFoundError
            // (an Error). Letting that escape kills the AWT event thread and terminates the app.
            ProfileTransferResult.Error(describeTransferFailure("export", e))
        }
    }

    /**
     * Import a single profile previously written by [exportProfile].
     *
     * The profile is *added* rather than replacing the existing list, and is given a fresh
     * id plus a de-duplicated name so that importing a profile twice — or importing one that
     * originated on this machine — cannot collide with or silently overwrite what is already there.
     */
    fun importProfile(file: File): ProfileTransferResult {
        return try {
            if (!file.exists()) return ProfileTransferResult.Error("The selected file does not exist.")

            val envelope = json.decodeFromString(
                SimulatorProfileExport.serializer(),
                String(Files.readAllBytes(file.toPath()))
            )
            val name = uniqueProfileName(envelope.simulatorType, envelope.profileName)
            val imported: SimulatorConfig = when (envelope.simulatorType) {
                SimulatorType.HOST ->
                    json.decodeFromJsonElement(GatewayConfig.serializer(), envelope.profile)
                        .copy(id = generateConfigId(), name = name, modifiedDate = System.currentTimeMillis())

                SimulatorType.HSM ->
                    json.decodeFromJsonElement(HSMSimulatorConfig.serializer(), envelope.profile)
                        .copy(id = generateConfigId(), name = name, modifiedDate = System.currentTimeMillis())

                SimulatorType.APDU ->
                    json.decodeFromJsonElement(APDUSimulatorConfig.serializer(), envelope.profile)
                        .copy(id = generateConfigId(), name = name, modifiedDate = System.currentTimeMillis())

                SimulatorType.POS ->
                    json.decodeFromJsonElement(POSSimulatorConfig.serializer(), envelope.profile)
                        .copy(id = generateConfigId(), name = name, modifiedDate = System.currentTimeMillis())

                SimulatorType.HSM_COMMAND ->
                    json.decodeFromJsonElement(HsmCommandConfig.serializer(), envelope.profile)
                        .copy(id = generateConfigId(), name = name, modifiedDate = System.currentTimeMillis())

                else -> return ProfileTransferResult.Error(
                    "${envelope.simulatorType.displayName} profiles cannot be imported yet."
                )
            }

            addConfig(imported)
            save()
            ProfileTransferResult.Success(
                content = "",
                profileName = imported.name,
                simulatorType = envelope.simulatorType
            )
        } catch (e: Throwable) {
            ProfileTransferResult.Error(describeTransferFailure("import", e))
        }
    }

    /**
     * Builds a message the user can act on.
     *
     * [LinkageError] here means the running build is inconsistent with the compiled serializers
     * rather than the file being wrong, and saying "not a valid file" would send the user off
     * editing perfectly good JSON.
     */
    private fun describeTransferFailure(action: String, e: Throwable): String = when (e) {
        is LinkageError -> "Could not $action this profile because the application build is out " +
                "of date (${e.javaClass.simpleName}: ${e.message}). Rebuild and restart, e.g. " +
                "./gradlew clean :composeApp:run"

        else -> if (action == "import")
            "Not a valid single-profile file. ${e.message ?: e.javaClass.simpleName}".trim()
        else
            e.message ?: "Unable to $action profile"
    }

    /**
     * Returns [desired] if unused for this simulator type, otherwise appends a counter.
     */
    private fun uniqueProfileName(type: SimulatorType, desired: String): String {
        val taken = getConfigsByType(type).map { it.name }.toSet()
        if (desired !in taken) return desired
        var suffix = 2
        while ("$desired ($suffix)" in taken) suffix++
        return "$desired ($suffix)"
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
                hsmCommandConfigs = hsmCommandConfigs.value,
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
                    SimulatorType.HSM_COMMAND -> {
                        hsmCommandConfigs.value = configCollection.hsmCommandConfigs
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
                    SimulatorType.TOOL -> { /* Tool tabs have no persisted config */ }
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
    val hsmCommandConfigs: List<HsmCommandConfig> = emptyList(),
    val ecrConfigs: List<String>,
    val atmConfigs: List<String>,
    val cardConfigs: List<String>,
    val switchConfigs: List<String>,
    val acquirerConfigs: List<String>,
    val issuerConfigs: List<String>,
    val exportedAt: Long,
    val version: String
)

/** Schema version for single-profile export files. */
const val PROFILE_EXPORT_VERSION = "1.0"

/**
 * Envelope for a single exported simulator profile.
 *
 * The payload is kept as a raw [JsonElement] rather than a typed field so that one envelope
 * shape serves every simulator, and so an envelope produced by a newer build still parses far
 * enough to report a useful error instead of failing opaquely.
 */
@Serializable
data class SimulatorProfileExport(
    val simulatorType: SimulatorType,
    val profileName: String,
    val exportedAt: Long,
    val version: String = PROFILE_EXPORT_VERSION,
    val profile: JsonElement
)

/**
 * Outcome of a single-profile export or import.
 *
 * Deliberately a sealed type rather than a bare String: the older [UnifiedSimulatorState.export]
 * returns its error text in place of the payload, which means a failed export silently writes the
 * error message into the user's .json file.
 */
sealed class ProfileTransferResult {
    data class Success(
        val content: String,
        val profileName: String,
        val simulatorType: SimulatorType
    ) : ProfileTransferResult()

    data class Error(val message: String) : ProfileTransferResult()
}

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