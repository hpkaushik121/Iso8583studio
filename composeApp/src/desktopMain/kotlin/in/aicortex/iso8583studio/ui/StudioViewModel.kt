package `in`.aicortex.iso8583studio.ui

import cafe.adriel.voyager.core.model.ScreenModel
import `in`.aicortex.iso8583studio.ui.navigation.UnifiedSimulatorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object Studio {
    private val _uiState = MutableStateFlow(UnifiedSimulatorState())
    val appState: StateFlow<UnifiedSimulatorState> = _uiState.asStateFlow()
}