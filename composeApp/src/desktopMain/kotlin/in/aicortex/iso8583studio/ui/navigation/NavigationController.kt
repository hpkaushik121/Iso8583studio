package `in`.aicortex.iso8583studio.ui.navigation


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.SecurityKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random


/**
 * Navigation controller for the Security Gateway Configuration app
 * Manages navigation and state between different screens
 */
class NavigationController {
    // Private mutable state
    private val _state = MutableStateFlow(GatewayConfigurationState())

    // Public immutable state for observing
    val state: StateFlow<GatewayConfigurationState> = _state.asStateFlow()

    // Current screen - start with config list
    var currentScreen by mutableStateOf<Screen>(Screen.GatewayType)
        private set

    // Navigation methods
    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    // Tab navigation
    fun selectTab(index: Int) {
        _state.update { it.copy(selectedTabIndex = index) }
        when (index) {
            0 -> navigateTo(Screen.GatewayType)
            1 -> navigateTo(Screen.TransmissionSettings)
            2 -> navigateTo(Screen.KeySettings)
            3 -> navigateTo(Screen.LogSettings)
            4 -> navigateTo(Screen.AdvancedOptions)
        }
    }

    // Config management methods
    fun selectConfig(index: Int) {
        _state.update {
            it.copy(selectedConfigIndex = index)
        }
        // Navigate to Gateway Type tab when selecting a config
        selectTab(0)
    }

    fun addNewConfig() {
        val newConfig = GatewayConfig(
            id = Random.nextInt(),
            name = "Config_${_state.value.configList.value.size + 1}")
        _state.update {
            val newList = it.configList.value + newConfig
            it.apply {
                configList.value = newList
                selectedConfigIndex = newList.size - 1
            }
        }
        // Navigate to Gateway Type tab when creating a new config
        selectTab(0)
    }

    fun deleteCurrentConfig() {
        val currentIndex = _state.value.selectedConfigIndex
        if (currentIndex >= 0) {
            _state.update {
                val newList = it.configList.value.toMutableList().apply {
                    removeAt(currentIndex)
                }

                // Determine the new selected index
                val newSelectedIndex = when {
                    // If there are items before the current one, select the previous item
                    currentIndex > 0 -> currentIndex - 1
                    // If there are items after the current one, keep the same index (next item shifted up)
                    newList.isNotEmpty() -> currentIndex.coerceAtMost(newList.size - 1)
                    // If the list is now empty, no selection
                    else -> -1
                }

                it.apply {
                    configList.value = newList
                    selectedConfigIndex = newSelectedIndex
                }
            }

            navigateTo(Screen.GatewayType)
        }
    }

    fun saveConfig(config: GatewayConfig) {
        val index = _state.value.selectedConfigIndex
        if (index >= 0) {
            _state.update {
                val newList = it.configList.value.toMutableList().apply {
                    set(index, config)
                }
                it.apply { configList.value = newList}
            }
        }
    }

    fun saveAllConfigs() {
        // In a real app, this would save to disk/database
        println("Saving all configurations: ${_state.value.configList.value.size} configs")
    }

    // Key management methods
//    fun updateKeys(keys: List<SecurityKey>) {
//        _state.update { it.copy(keysList = keys) }
//    }
//
//    fun addKey(key: SecurityKey) {
//        _state.update { it.copy(keysList = it.keysList + key) }
//    }
//
//    fun deleteKey(index: Int) {
//        if (index >= 0 && index < _state.value.keysList.size) {
//            _state.update {
//                val newList = it.keysList.toMutableList().apply {
//                    removeAt(index)
//                }
//                it.copy(keysList = newList)
//            }
//        }
//    }

    // Monitor and Host Simulator navigation
    fun openMonitor() {
        navigateTo(Screen.Monitor)
    }

    fun openHostSimulator() {
        navigateTo(Screen.HostSimulator)
    }

    fun goBack() {
        navigateTo(Screen.GatewayType)
    }
}