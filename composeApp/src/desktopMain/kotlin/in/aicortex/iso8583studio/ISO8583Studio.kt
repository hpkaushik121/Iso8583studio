package `in`.aicortex.iso8583studio

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.aicortex.iso8583studio.data.ExceptionHandler
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.domain.FileImporter
import `in`.aicortex.iso8583studio.domain.ImportResult
import `in`.aicortex.iso8583studio.domain.utils.ExportResult
import `in`.aicortex.iso8583studio.domain.utils.FileExporter
import `in`.aicortex.iso8583studio.ui.AppTheme
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.Studio.appState
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsm.HSMSimulatorConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import `in`.aicortex.iso8583studio.ui.screens.components.StatusBadge
import `in`.aicortex.iso8583studio.ui.screens.about.AboutDialog
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.HostSimulatorScreen
import `in`.aicortex.iso8583studio.ui.screens.hsmSimulator.HsmSimulatorScreen
import `in`.aicortex.iso8583studio.ui.screens.posTerminal.POSTerminalSimulatorScreen
import `in`.aicortex.iso8583studio.ui.navigation.rememberNavigationController
import `in`.aicortex.iso8583studio.ui.session.GlobalSimulatorTabBar
import `in`.aicortex.iso8583studio.ui.session.SimulatorSessionManager
import iso8583studio.composeapp.generated.resources.Res
import iso8583studio.composeapp.generated.resources.app
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import java.awt.Desktop

enum class DialogType {
    SUCCESS, ERROR, NONE
}

class ISO8583Studio {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = application {

            Thread.currentThread().uncaughtExceptionHandler = ExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler())

            val windowState = remember {
                WindowState(
                    size = DpSize(width = 1800.dp, height = 800.dp),
                )
            }
            var showAboutDialog by remember { mutableStateOf(false) }
            val isoCoroutine = rememberCoroutineScope()

            AppTheme {
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "ISO8583Studio",
                    state = windowState,
                    icon = painterResource(Res.drawable.app)
                ) {
                    Navigator(screen = Destination.Home) { navigator ->
                        val navigationController = rememberNavigationController(navigator)

                        // ─── Menu Bar (unchanged) ───────────────────────
                        MenuBar {
                            Menu(text = "File") {
                                Menu(text = "Configuration") {
                                    Item(text = "Export") {
                                        isoCoroutine.launch {
                                            val file = FileExporter().exportFile(
                                                window = window,
                                                fileName = "ISO8583Studio",
                                                fileExtension = "json",
                                                fileContent = appState.value.export().toByteArray(),
                                                fileDescription = "Configuration File"
                                            )
                                            when (file) {
                                                is ExportResult.Success -> {
                                                    appState.value.resultDialogInterface?.onSuccess {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Text("Configuration exported successfully!")
                                                        }
                                                    }
                                                }
                                                is ExportResult.Cancelled -> println("Export cancelled")
                                                is ExportResult.Error -> {
                                                    appState.value.resultDialogInterface?.onError {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center,
                                                        ) {
                                                            Text((file as ExportResult.Error).message)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Item(text = "Import") {
                                        isoCoroutine.launch {
                                            val file = FileImporter().importFile(
                                                window = window,
                                                fileExtensions = listOf("json"),
                                                importLogic = { file ->
                                                    appState.value.import(file)
                                                }
                                            )
                                            when (file) {
                                                is ImportResult.Success -> {
                                                    appState.value.resultDialogInterface?.onSuccess {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Text("Configuration imported successfully!")
                                                        }
                                                    }
                                                }
                                                is ImportResult.Cancelled -> println("Import cancelled")
                                                is ImportResult.Error -> {
                                                    appState.value.resultDialogInterface?.onError {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Text(file.message)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Separator()
                                Item(text = "Exit") { exitApplication() }
                            }

                            Menu(text = "Simulator") {
                                Item(text = "Host Simulator") {
                                    navigationController.navigateTo(Destination.HostSimulatorConfig)
                                }
                                Item(text = "HSM Simulator") {
                                    navigationController.navigateTo(Destination.HSMSimulatorConfig)
                                }
                                Item(text = "POS Terminal") {
                                    navigationController.navigateTo(Destination.POSTerminalConfig)
                                }
                                Item(text = "APDU Simulator") {
                                    navigationController.navigateTo(Destination.ApduSimulatorConfig)
                                }
                                Separator()
                                Item(text = "ECR Simulator") {
                                    navigationController.navigateTo(Destination.EcrSimulatorConfigScreen)
                                }
                                Item(text = "ATM Simulator") {
                                    navigationController.navigateTo(Destination.ATMSimulatorConfig)
                                }
                                Separator()
                                Item(text = "Payment Switch") {
                                    navigationController.navigateTo(Destination.PaymentSwitchConfig)
                                }
                                Item(text = "Acquirer Gateway") {
                                    navigationController.navigateTo(Destination.AcquirerGatewayConfig)
                                }
                                Item(text = "Issuer System") {
                                    navigationController.navigateTo(Destination.IssuerSystemConfig)
                                }
                            }

                            // NOTE: All other menu items (Payments, EMV, Cipher, Keys, etc.)
                            // remain exactly as they were — omitted here for brevity.
                            // Copy them from the original ISO8583Studio.kt unchanged.
                            // Only the main content area rendering logic changes below.

                            Menu(text = "Help") {
                                Item(text = "About") { showAboutDialog = true }
                            }
                        }

                        // ─── About Dialog ───────────────────────────────
                        if (showAboutDialog) {
                            AboutDialog(onCloseRequest = { showAboutDialog = false })
                        }

                        // ─── Error/Success Dialog ───────────────────────
                        var showErrorDialog by remember {
                            mutableStateOf<Pair<DialogType, @Composable (() -> Unit)>?>(null)
                        }

                        appState.value.resultDialogInterface = object : ResultDialogInterface {
                            override fun onError(item: @Composable (() -> Unit)) {
                                showErrorDialog = Pair(DialogType.ERROR, item)
                            }
                            override fun onSuccess(item: @Composable (() -> Unit)) {
                                showErrorDialog = Pair(DialogType.SUCCESS, item)
                            }
                        }

                        if (showErrorDialog?.first == DialogType.ERROR) {
                            AlertDialog(
                                onDismissRequest = { showErrorDialog = null },
                                confirmButton = {
                                    Button(
                                        onClick = { showErrorDialog = null },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                                    ) { Text("OK") }
                                },
                                title = {
                                    StatusBadge(text = "ERROR", color = ErrorRed, modifier = Modifier.padding(bottom = 8.dp))
                                },
                                text = showErrorDialog!!.second,
                                shape = MaterialTheme.shapes.medium,
                                backgroundColor = MaterialTheme.colors.surface
                            )
                        }

                        if (showErrorDialog?.first == DialogType.SUCCESS) {
                            AlertDialog(
                                onDismissRequest = { showErrorDialog = null },
                                confirmButton = {
                                    Button(
                                        onClick = { showErrorDialog = null },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                                    ) { Text("OK") }
                                },
                                title = {
                                    StatusBadge(text = "SUCCESS", color = SuccessGreen, modifier = Modifier.padding(bottom = 8.dp))
                                },
                                text = showErrorDialog!!.second,
                                shape = MaterialTheme.shapes.medium,
                                backgroundColor = MaterialTheme.colors.surface
                            )
                        }

                        appState.value.setComposableWindow(window)

                        // ═══════════════════════════════════════════════════
                        //  CORE CHANGE: Global Tab Bar + Session-Aware Content
                        // ═══════════════════════════════════════════════════
                        //
                        // Architecture:
                        //   ┌──────────────────────────────────────────┐
                        //   │  Global Tab Bar (session tabs)           │
                        //   ├──────────────────────────────────────────┤
                        //   │                                          │
                        //   │  Content Area:                           │
                        //   │    - Main Navigation (Voyager) OR        │
                        //   │    - Active Simulator Session            │
                        //   │                                          │
                        //   └──────────────────────────────────────────┘
                        //
                        // Running simulators persist in memory via
                        // SimulatorSessionManager even when the user
                        // navigates to tools/calculators/config screens.

                        val activeSessionId by SimulatorSessionManager.activeSessionId

                        Column(modifier = Modifier.fillMaxSize()) {
                            // ── Global Tab Bar ──
                            GlobalSimulatorTabBar()

                            // ── Content Area ──
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Layer 1: Main Voyager navigation (always composed, visibility toggled)
                                // We keep it composed so navigation state is preserved
                                CurrentScreen()

                                // Layer 2: Simulator/tool session content
                                // Sessions are ALWAYS composed (never conditional) so that:
                                //   a) TCP servers/state survive tab switches
                                //   b) DisposableEffect cleanup fires when a session is closed
                                // Visibility is controlled purely via Modifier size (0dp when hidden).
                                SimulatorSessionManager.sessions.forEach { session ->
                                    key(session.id) {
                                        val isVisible = session.id == activeSessionId
                                        Box(
                                            modifier = if (isVisible) Modifier.fillMaxSize()
                                                       else Modifier.requiredSize(0.dp)
                                        ) {
                                            SessionContent(
                                                session = session,
                                                window = window,
                                                onBack = {
                                                    SimulatorSessionManager.activateMainContent()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders the appropriate simulator screen for a given session.
 * Each session type gets its own composable with the service instance
 * from the session, ensuring state persistence across tab switches.
 */
@Composable
private fun SessionContent(
    session: `in`.aicortex.iso8583studio.ui.session.SimulatorSession,
    window: java.awt.Window,
    onBack: () -> Unit
) {
    when (session.simulatorType) {
        SimulatorType.HSM -> {
            val config = session.config as HSMSimulatorConfig
            HsmSimulatorScreen(
                config = config,
                onBack = onBack,
                service = session.hsmService  // reuse session-owned service; closeSession() stops it
            )
        }

        SimulatorType.HOST -> {
            val config = session.config as GatewayConfig
            val navigationController = `in`.aicortex.iso8583studio.ui.navigation.rememberNavigationController(
                cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow
            )
            HostSimulatorScreen(
                window = window as androidx.compose.ui.awt.ComposeWindow,
                config = config,
                navigationController = navigationController,
                onBack = onBack,
                onError = appState.value.resultDialogInterface!!,
                onSaveClick = { appState.value.save() }
            )
        }

        SimulatorType.POS -> {
            val config = session.config as POSSimulatorConfig
            POSTerminalSimulatorScreen(
                window = window as androidx.compose.ui.awt.ComposeWindow,
                config = config,
                onBack = onBack,
                onSaveClick = { appState.value.save() }
            )
        }

        SimulatorType.TOOL -> {
            // Render the tool's Voyager Screen in an isolated local navigator.
            // The X button on the tab closes this session; back navigation within the tool
            // uses the local navigator stack.
            val screen = session.toolScreen
            if (screen != null) {
                Navigator(screen = screen) { localNavigator ->
                    CurrentScreen()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tool unavailable")
                }
            }
        }

        else -> {
            // Placeholder for future simulator types
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("${session.simulatorType.displayName} session is running")
            }
        }
    }
}