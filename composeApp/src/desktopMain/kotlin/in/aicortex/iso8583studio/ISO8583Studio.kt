package `in`.aicortex.iso8583studio

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
                        val poppedOutIds = SimulatorSessionManager.poppedOutSessionIds

                        Column(modifier = Modifier.fillMaxSize()) {
                            // ── Global Tab Bar ──
                            GlobalSimulatorTabBar()

                            // ── Content Area ──
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Layer 1: Main Voyager navigation (always composed, visibility toggled)
                                CurrentScreen()

                                // Layer 2: Inline session content (skip popped-out sessions)
                                SimulatorSessionManager.sessions.forEach { session ->
                                    if (session.id !in poppedOutIds) {
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

                        // ── Pop-out Windows ──
                        // Each pop-out Window has its own composition, so we
                        // wrap content in a Navigator to provide LocalNavigator
                        // (required by HOST sessions for NavigationController).
                        SimulatorSessionManager.sessions
                            .filter { it.id in poppedOutIds }
                            .forEach { session ->
                                key("popout-${session.id}") {
                                    val popoutWindowState = rememberWindowState(
                                        size = DpSize(width = 1400.dp, height = 750.dp)
                                    )
                                    Window(
                                        onCloseRequest = {
                                            SimulatorSessionManager.dockSession(session.id)
                                        },
                                        title = session.displayName,
                                        state = popoutWindowState,
                                        icon = painterResource(Res.drawable.app)
                                    ) {
                                        AppTheme {
                                            Navigator(screen = Destination.Home) {
                                                Column(modifier = Modifier.fillMaxSize()) {
                                                    PopOutWindowTopBar(
                                                        session = session,
                                                        onDock = { SimulatorSessionManager.dockSession(session.id) },
                                                        onClose = { SimulatorSessionManager.closeSession(session.id) }
                                                    )
                                                    Box(modifier = Modifier.fillMaxSize()) {
                                                        SessionContent(
                                                            session = session,
                                                            window = window,
                                                            onBack = {
                                                                SimulatorSessionManager.dockSession(session.id)
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
            // IMPORTANT: Wrap the nested Navigator in an isolated SaveableStateHolder scope.
            //
            // Problem: Voyager's CurrentScreen() calls navigator.saveableState("currentScreen", screen)
            // which calls SaveableStateHolder.SaveableStateProvider("${screen.key}:currentScreen").
            // Both the root Navigator and this nested Navigator share the SAME outer
            // LocalSaveableStateRegistry (they're in the same Compose tree). If both navigators
            // show the same Destination.* screen simultaneously (e.g. Destination.Home — which
            // happens when the user clicks "Launch Simulator" from a config-screen tool tab,
            // pushing Home onto the local navigator while the root also shows Home), BOTH try
            // to register "Destination.Home:currentScreen" in the SAME parent registry → crash:
            //   "Key …Destination.Home:currentScreen was used multiple times"
            //
            // Fix: wrap the nested Navigator in SaveableStateHolder.SaveableStateProvider(session.id).
            // Inside this scope, LocalSaveableStateRegistry.current is an INNER registry managed
            // by the holder, completely separate from the outer one. The nested Navigator's own
            // SaveableStateHolderImpl is created with this inner registry as parent, so all its
            // SaveableStateProvider keys live in a completely isolated namespace.
            val screen = session.toolScreen
            if (screen != null) {
                val toolStateHolder = rememberSaveableStateHolder()
                toolStateHolder.SaveableStateProvider(key = session.id) {
                    Navigator(screen = screen) {
                        CurrentScreen()
                    }
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

/**
 * Compact top bar for pop-out windows with session info, dock-back, and close actions.
 */
@Composable
private fun PopOutWindowTopBar(
    session: `in`.aicortex.iso8583studio.ui.session.SimulatorSession,
    onDock: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = session.displayName,
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onSurface
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onDock, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.PictureInPicture,
                        contentDescription = "Dock back to main window",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close session",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}