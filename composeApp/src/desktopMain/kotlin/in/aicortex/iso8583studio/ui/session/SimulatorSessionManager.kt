package `in`.aicortex.iso8583studio.ui.session

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.screen.Screen
import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.StudioTool
import `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService.HsmServiceImpl
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Minimal SimulatorConfig implementation for non-simulator tool tabs.
 * Tool tabs don't have real server configurations; this stub satisfies the interface.
 */
data class ToolTabConfig(val tool: StudioTool) : SimulatorConfig {
    override val id: String = "tool-${tool.name}"
    override val name: String = tool.label
    override val description: String = tool.description
    override val serverAddress: String = ""
    override val serverPort: Int = 0
    override val simulatorType: SimulatorType = SimulatorType.TOOL
    override val enabled: Boolean = true
    override val createdDate: Long = System.currentTimeMillis()
    override val modifiedDate: Long = System.currentTimeMillis()
    override val version: String = "1.0"
}

/**
 * Represents a single running simulator session or an open tool tab.
 * Each session holds its own service instance and lifecycle state,
 * enabling true parallel execution of multiple simulators.
 */
data class SimulatorSession(
    val id: String,
    val config: SimulatorConfig,
    val simulatorType: SimulatorType,
    val displayName: String,
    val launchedAt: LocalDateTime = LocalDateTime.now(),
    val hsmService: HsmServiceImpl? = null,
    /** Non-null for TOOL-type sessions — the Voyager Screen to render inside this tab. */
    val toolScreen: Screen? = null,
    /** The StudioTool this tab represents (for TOOL sessions). */
    val studioTool: StudioTool? = null,
) {
    val shortName: String
        get() = when (simulatorType) {
            SimulatorType.HOST -> "HOST"
            SimulatorType.HSM -> "HSM"
            SimulatorType.POS -> "POS"
            SimulatorType.APDU -> "APDU"
            SimulatorType.TOOL -> studioTool?.label?.take(6) ?: "TOOL"
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

    /** Session IDs that are currently displayed in their own pop-out window */
    val poppedOutSessionIds = mutableStateListOf<String>()

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
        // Guard: never create a duplicate session for the same config.
        // Match by config ID (exact) OR by simulatorType + config name as a fallback,
        // so even if the config object was re-created with the same logical identity
        // we reuse the existing tab instead of crashing.
        val existing = sessions.find { session ->
            session.config.id == config.id ||
            (session.simulatorType == config.simulatorType &&
             session.config.name == config.name &&
             session.simulatorType != SimulatorType.TOOL)
        }
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
     * Close a simulator session and stop its server.
     *
     * Stopping is done in two complementary ways:
     *  1. The session's stored service reference is stopped immediately here (covers
     *     cases where the composable DisposableEffect scope may already be cancelled).
     *  2. Removing the session from [sessions] disposes its composable key, so the
     *     screen's own DisposableEffect also fires — catching any service instance
     *     created locally via `remember` inside the screen.
     */
    fun closeSession(sessionId: String) {
        val session = sessions.find { it.id == sessionId } ?: return

        // Stop the stored service reference on an IO coroutine
        session.hsmService?.let { svc ->
            CoroutineScope(Dispatchers.IO).launch { svc.stop() }
        }

        poppedOutSessionIds.remove(sessionId)

        // Removing the session causes the key() block in ISO8583Studio.kt to dispose,
        // which triggers DisposableEffect inside HsmSimulatorScreen / HostSimulatorScreen.
        sessions.removeAll { it.id == sessionId }

        // If we closed the active session, switch back to main content or most recent session
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
     * Open a StudioTool in a new global tab.
     *
     * If the tool is already open in a tab, switches to the existing tab instead.
     * Records the usage for dynamic Quick Access ordering.
     *
     * @return The session ID of the created (or existing) tool tab
     */
    fun openTool(tool: StudioTool): String {
        ToolUsageTracker.recordUsage(tool.label)

        // Reuse existing tab for the same tool
        val existing = sessions.find {
            it.simulatorType == SimulatorType.TOOL && it.studioTool == tool
        }
        if (existing != null) {
            activeSessionId.value = existing.id
            return existing.id
        }

        val sessionId = "TOOL-${tool.name}-${System.currentTimeMillis()}"
        val session = SimulatorSession(
            id = sessionId,
            config = ToolTabConfig(tool),
            simulatorType = SimulatorType.TOOL,
            displayName = tool.label,
            toolScreen = tool.screen,
            studioTool = tool,
        )
        sessions.add(session)
        activeSessionId.value = sessionId
        return sessionId
    }

    /**
     * Pop a session out into its own separate window.
     * The session remains running but is no longer rendered in the main window.
     * If it was the active session, switch to the next available or main content.
     */
    fun popOutSession(sessionId: String) {
        if (sessions.none { it.id == sessionId }) return
        if (sessionId in poppedOutSessionIds) return

        poppedOutSessionIds.add(sessionId)

        if (activeSessionId.value == sessionId) {
            val nextInline = sessions.firstOrNull { it.id != sessionId && it.id !in poppedOutSessionIds }
            activeSessionId.value = nextInline?.id
        }
    }

    /**
     * Dock a popped-out session back into the main window tab bar.
     * The session becomes the active tab.
     */
    fun dockSession(sessionId: String) {
        poppedOutSessionIds.remove(sessionId)
        if (sessions.any { it.id == sessionId }) {
            activeSessionId.value = sessionId
        }
    }

    /** Whether a session is currently displayed in its own pop-out window */
    fun isPoppedOut(sessionId: String): Boolean = sessionId in poppedOutSessionIds

    /**
     * Get count of running sessions
     */
    val sessionCount: Int
        get() = sessions.size
}