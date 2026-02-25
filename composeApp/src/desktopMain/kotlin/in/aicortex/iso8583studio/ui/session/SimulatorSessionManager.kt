package `in`.aicortex.iso8583studio.ui.session

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.HsmServiceImpl
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import java.time.LocalDateTime

/**
 * Represents a single running simulator session.
 * Each session holds its own service instance and lifecycle state,
 * enabling true parallel execution of multiple simulators.
 */
data class SimulatorSession(
    val id: String,
    val config: SimulatorConfig,
    val simulatorType: SimulatorType,
    val displayName: String,
    val launchedAt: LocalDateTime = LocalDateTime.now(),
    val hsmService: HsmServiceImpl? = null, // Only for HSM sessions
    // Future: hostService, posService, etc.
) {
    val shortName: String
        get() = when (simulatorType) {
            SimulatorType.HOST -> "HOST"
            SimulatorType.HSM -> "HSM"
            SimulatorType.POS -> "POS"
            SimulatorType.APDU -> "APDU"
            else -> simulatorType.name.take(4)
        }
}

/**
 * Global Simulator Session Manager — Singleton
 *
 * Manages the lifecycle of all running simulator instances.
 * Simulators registered here persist across navigation changes,
 * allowing the user to switch between running simulators and other tools
 * without losing state.
 *
 * Design rationale:
 * - In a real payment testing lab, you need Host + HSM + POS running simultaneously
 * - Each session owns its service (TCP server, crypto engine, etc.)
 * - Sessions are independent — stopping one doesn't affect others
 * - The global tab bar reads from this manager to render session tabs
 */
object SimulatorSessionManager {

    /** All currently running simulator sessions */
    val sessions = mutableStateListOf<SimulatorSession>()

    /** The currently active/visible session ID (null = showing main navigation content) */
    val activeSessionId = mutableStateOf<String?>(null)

    /** Whether the main navigation content (tools, config screens) is visible */
    val isMainContentActive: Boolean
        get() = activeSessionId.value == null

    /**
     * Launch a new simulator session.
     * Creates the service instance and adds it to the session list.
     * If a session with the same config ID already exists, switches to it instead.
     *
     * @return The session ID of the launched (or existing) session
     */
    fun launchSimulator(config: SimulatorConfig): String {
        // Check for existing session with same config
        val existing = sessions.find { it.config.id == config.id }
        if (existing != null) {
            activeSessionId.value = existing.id
            return existing.id
        }

        val sessionId = "${config.simulatorType.name}-${config.id}-${System.currentTimeMillis()}"
        val displayName = "${config.simulatorType.displayName} - ${config.name}"

        val session = when (config) {
            is HSMSimulatorConfig -> SimulatorSession(
                id = sessionId,
                config = config,
                simulatorType = SimulatorType.HSM,
                displayName = displayName,
                hsmService = HsmServiceImpl(config)
            )

            is GatewayConfig -> SimulatorSession(
                id = sessionId,
                config = config,
                simulatorType = SimulatorType.HOST,
                displayName = displayName
            )

            is POSSimulatorConfig -> SimulatorSession(
                id = sessionId,
                config = config,
                simulatorType = SimulatorType.POS,
                displayName = displayName
            )

            else -> SimulatorSession(
                id = sessionId,
                config = config,
                simulatorType = config.simulatorType,
                displayName = displayName
            )
        }

        sessions.add(session)
        activeSessionId.value = sessionId
        return sessionId
    }

    /**
     * Close a simulator session and clean up resources.
     * If this was the active session, switches back to main content.
     */
    fun closeSession(sessionId: String) {
        val session = sessions.find { it.id == sessionId } ?: return

        // Cleanup service resources
        // Note: The actual stop() call should be handled by the UI before closing
        sessions.removeAll { it.id == sessionId }

        // If we closed the active session, go back to main content
        if (activeSessionId.value == sessionId) {
            activeSessionId.value = sessions.lastOrNull()?.id
        }
    }

    /**
     * Switch to a specific session tab
     */
    fun activateSession(sessionId: String) {
        if (sessions.any { it.id == sessionId }) {
            activeSessionId.value = sessionId
        }
    }

    /**
     * Switch back to the main navigation content (tools, configs, home)
     */
    fun activateMainContent() {
        activeSessionId.value = null
    }

    /**
     * Get the currently active session (if any)
     */
    fun getActiveSession(): SimulatorSession? {
        return activeSessionId.value?.let { id ->
            sessions.find { it.id == id }
        }
    }

    /**
     * Check if any simulator of a given type is currently running
     */
    fun hasRunningSimulator(type: SimulatorType): Boolean {
        return sessions.any { it.simulatorType == type }
    }

    /**
     * Get all sessions of a specific simulator type
     */
    fun getSessionsByType(type: SimulatorType): List<SimulatorSession> {
        return sessions.filter { it.simulatorType == type }
    }

    /**
     * Get count of running sessions
     */
    val sessionCount: Int
        get() = sessions.size
}