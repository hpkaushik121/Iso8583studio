package `in`.aicortex.iso8583studio.ui.navigation


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.SecurityKey
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random


/**
 * Navigation controller for the Security Gateway Configuration app.
 * Manages navigation and state between different screens using a stack.
 */
class NavigationController {
    // Private mutable state
    private val _state = MutableStateFlow(GatewayConfigurationState())

    // Public immutable state for observing
    val state: StateFlow<GatewayConfigurationState> = _state.asStateFlow()

    // Cache to hold stateful GatewayServiceImpl instances. The key is the GatewayConfig ID.
    private val gatewayServices = mutableMapOf<Int, GatewayServiceImpl>()

    // --- Stack-based navigation ---
    private val navigationStack = mutableStateListOf<Screen>(Screen.GatewayType)

    // The current screen is always the last item in the stack.
    val currentScreen: Screen
        get() = navigationStack.last()

    /**
     * Retrieves a managed instance of GatewayServiceImpl for the currently selected configuration.
     * This function ensures that the same service instance is used for a given configuration,
     * preserving its state (like active connections) across navigation events.
     *
     * @param window The ComposeWindow required by the service.
     * @param onError The error handling interface.
     * @return A stateful GatewayServiceImpl instance, or null if no configuration is selected.
     */
    fun getManagedGatewayService(window: ComposeWindow, onError: ResultDialogInterface?): GatewayServiceImpl? {
        val config = _state.value.configList.value.getOrNull(_state.value.selectedConfigIndex) ?: return null

        // Get the existing service from the cache, or create a new one if it doesn't exist.
        val service = gatewayServices.getOrPut(config.id) {
            GatewayServiceImpl(config)
        }

        // Always ensure the latest window and listeners are attached.
        service.composeWindow = window
        if (onError != null) {
            service.setShowErrorListener(onError)
        }

        return service
    }

    /**
     * Stops all running gateway services and clears the cache.
     * Should be called when the application is closing to ensure graceful shutdown.
     */
    suspend fun stopAndClearAllServices() {
        gatewayServices.values.forEach { it.stop() }
        gatewayServices.clear()
    }

    /**
     * Navigates to a given screen.
     * If the screen is already in the back stack, it's moved to the top.
     * Otherwise, it's added to the top of the stack.
     * Example: a -> b -> c. Navigating to 'b' results in: a -> c -> b.
     */
    fun navigateTo(screen: Screen) {
        // If the screen is already in the stack, remove its previous instance.
        navigationStack.remove(screen)
        // Add the screen to the end of the list, making it the new "top" of the stack.
        navigationStack.add(screen)
    }

    /**
     * Navigates to the previous screen in the stack.
     * If there's only one screen, this does nothing.
     */
    fun goBack() {
        if (navigationStack.size > 1) {
            navigationStack.removeLast()
        }
    }

    // Tab navigation
    fun selectTab(index: Int) {
        _state.update { it.copy(selectedTabIndex = index) }
        when (index) {
            0 -> navigateTo(Screen.GatewayType)
            1 -> navigateTo(Screen.TransmissionSettings)
//            2 -> navigateTo(Screen.KeySettings)
            2 -> navigateTo(Screen.LogSettings)
            3 -> navigateTo(Screen.AdvancedOptions)
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
        val newList = _state.value.configList.value + newConfig
        _state.value.configList.value = newList
        _state.value.selectedConfigIndex = newList.size -1

        // Navigate to Gateway Type tab when creating a new config
        selectTab(0)
    }

    suspend fun deleteCurrentConfig() {
        val currentIndex = _state.value.selectedConfigIndex
        if (currentIndex >= 0) {
            // Stop and remove the service associated with the config being deleted.
            val configId = _state.value.configList.value.getOrNull(currentIndex)?.id
            if (configId != null) {
                gatewayServices[configId]?.stop()
                gatewayServices.remove(configId)
            }

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

            // After deleting, reset the navigation stack to the main screen.
            navigationStack.clear()
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
        _state.value.save()
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
}
