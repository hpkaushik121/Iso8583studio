package `in`.aicortex.iso8583studio.ui

import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.UnifiedSimulatorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object Studio {
    private val _uiState = MutableStateFlow(UnifiedSimulatorState())
    val appState: StateFlow<UnifiedSimulatorState> = _uiState.asStateFlow()
}