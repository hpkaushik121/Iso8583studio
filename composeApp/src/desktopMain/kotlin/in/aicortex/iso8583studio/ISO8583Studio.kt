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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
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
import `in`.aicortex.iso8583studio.ui.session.ToolUsageTracker
import `in`.aicortex.iso8583studio.data.model.StudioTool
import iso8583studio.composeapp.generated.resources.Res
import iso8583studio.composeapp.generated.resources.app
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import `in`.aicortex.iso8583studio.ui.GlassBackdrop
import `in`.aicortex.iso8583studio.ui.GlassWindowEffect
import `in`.aicortex.iso8583studio.ui.GlossyMenuBar
import `in`.aicortex.iso8583studio.ui.MenuBarItem
import `in`.aicortex.iso8583studio.ui.MenuBarMenu
import `in`.aicortex.iso8583studio.ui.MenuBarSeparator
import `in`.aicortex.iso8583studio.ui.MenuBarSubMenu
import `in`.aicortex.iso8583studio.ui.detectWindowsDarkMode
import java.awt.Desktop
import java.awt.desktop.AboutHandler
import androidx.compose.foundation.isSystemInDarkTheme

enum class DialogType {
    SUCCESS, ERROR, NONE
}

class ISO8583Studio {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Set system look-and-feel before creating any windows
            try {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName())
            } catch (_: Throwable) { }

            application {

            Thread.currentThread().uncaughtExceptionHandler = ExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler())

            val windowState = remember {
                WindowState(
                    size = DpSize(width = 1800.dp, height = 800.dp),
                )
            }
            var showAboutDialog by remember { mutableStateOf(false) }
            val isoCoroutine = rememberCoroutineScope()

            // Override macOS application menu "About" to show our custom About dialog
            LaunchedEffect(Unit) {
                if (System.getProperty("os.name").lowercase().contains("mac")) {
                    try {
                        Desktop.getDesktop().setAboutHandler(AboutHandler { showAboutDialog = true })
                    } catch (_: Exception) {
                        // setAboutHandler not supported on this platform
                    }
                }
            }

            // Detect OS dark mode (registry fallback for Windows)
            val osDarkMode = remember {
                detectWindowsDarkMode() ?: false
            }

            AppTheme {
                val isDark = isSystemInDarkTheme() || osDarkMode
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "ISO8583Studio",
                    state = windowState,
                    icon = painterResource(Res.drawable.app),
                    transparent = false,
                    undecorated = false,
                ) {
                    GlassWindowEffect(
                        window = window,
                        backdrop = GlassBackdrop.MICA,
                        darkMode = isDark,
                    )
                    Navigator(screen = Destination.Home) { navigator ->
                        val navigationController = rememberNavigationController(navigator)

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
                            // ── Custom Menu Bar (themed, cross-platform) ──
                            GlossyMenuBar {
                                // Quick Access
                                MenuBarMenu("Quick Access", 0) {
                                    val topLabels = ToolUsageTracker.topLabels(8)
                                    val allTools = StudioTool.values()
                                    if (topLabels.isNotEmpty()) {
                                        for (label in topLabels) {
                                            val tool = allTools.firstOrNull { it.label == label }
                                            if (tool != null) {
                                                MenuBarItem(tool.label) { SimulatorSessionManager.openTool(tool) }
                                            }
                                        }
                                        MenuBarSeparator()
                                    }
                                    MenuBarItem("Home") {
                                        SimulatorSessionManager.activateMainContent()
                                        navigationController.navigateTo(Destination.Home)
                                    }
                                }
                                // Simulators
                                MenuBarMenu("Simulators", 1) {
                                    MenuBarSubMenu("Transaction Processing") {
                                        MenuBarItem("Host Simulator") { SimulatorSessionManager.openTool(StudioTool.HOST_SIMULATOR) }
                                        MenuBarItem("POS Terminal") { SimulatorSessionManager.openTool(StudioTool.POS_TERMINAL) }
                                        MenuBarItem("ATM Simulator") { SimulatorSessionManager.openTool(StudioTool.ATM_SIMULATOR) }
                                        MenuBarItem("ECR Simulator") { SimulatorSessionManager.openTool(StudioTool.ECR_SIMULATOR) }
                                    }
                                    MenuBarSubMenu("HSM & Security") {
                                        MenuBarItem("HSM Simulator") { SimulatorSessionManager.openTool(StudioTool.HSM_SIMULATOR) }
                                        MenuBarItem("HSM Host Console") { SimulatorSessionManager.openTool(StudioTool.HSM_COMMAND) }
                                        MenuBarItem("APDU Simulator") { SimulatorSessionManager.openTool(StudioTool.APDU_SIMULATOR) }
                                    }
                                    MenuBarSubMenu("Network & Switching") {
                                        MenuBarItem("Payment Switch") { SimulatorSessionManager.openTool(StudioTool.PAYMENT_SWITCH) }
                                        MenuBarItem("Acquirer Gateway") { SimulatorSessionManager.openTool(StudioTool.ACQUIRER_GATEWAY) }
                                        MenuBarItem("Issuer System") { SimulatorSessionManager.openTool(StudioTool.ISSUER_SYSTEM) }
                                    }
                                }
                                // Tools
                                MenuBarMenu("Tools", 2) {
                                    MenuBarSubMenu("EMV & Card Tools") {
                                        MenuBarItem("EMV 4.1 Crypto") { SimulatorSessionManager.openTool(StudioTool.EMV_41_CRYPTO) }
                                        MenuBarItem("EMV 4.2 Crypto") { SimulatorSessionManager.openTool(StudioTool.EMV_42_CRYPTO) }
                                        MenuBarItem("MasterCard Crypto") { SimulatorSessionManager.openTool(StudioTool.MASTERCARD_CRYPTO) }
                                        MenuBarItem("VSDC Crypto") { SimulatorSessionManager.openTool(StudioTool.VSDC_CRYPTO) }
                                        MenuBarItem("SDA Verification") { SimulatorSessionManager.openTool(StudioTool.SDA_VERIFICATION) }
                                        MenuBarItem("DDA Verification") { SimulatorSessionManager.openTool(StudioTool.DDA_VERIFICATION) }
                                        MenuBarSeparator()
                                        MenuBarItem("CAP Token") { SimulatorSessionManager.openTool(StudioTool.CAP_TOKEN) }
                                        MenuBarItem("HCE Visa") { SimulatorSessionManager.openTool(StudioTool.HCE_VISA) }
                                        MenuBarItem("Secure Messaging") { SimulatorSessionManager.openTool(StudioTool.SECURE_MESSAGING) }
                                        MenuBarSeparator()
                                        MenuBarItem("ATR Parser") { SimulatorSessionManager.openTool(StudioTool.ATR_PARSER) }
                                        MenuBarItem("EMV Data Parser") { SimulatorSessionManager.openTool(StudioTool.EMV_DATA_PARSER) }
                                        MenuBarItem("EMV Tag Dictionary") { SimulatorSessionManager.openTool(StudioTool.EMV_TAG_DICTIONARY) }
                                    }
                                    MenuBarSubMenu("Cryptography") {
                                        MenuBarItem("AES Calculator") { SimulatorSessionManager.openTool(StudioTool.AES_CALCULATOR) }
                                        MenuBarItem("DES/3DES Calculator") { SimulatorSessionManager.openTool(StudioTool.DES_CALCULATOR) }
                                        MenuBarItem("RSA Calculator") { SimulatorSessionManager.openTool(StudioTool.RSA_CALCULATOR) }
                                        MenuBarItem("ECDSA Calculator") { SimulatorSessionManager.openTool(StudioTool.ECDSA_CALCULATOR) }
                                        MenuBarItem("FPE Calculator") { SimulatorSessionManager.openTool(StudioTool.FPE_CALCULATOR) }
                                        MenuBarSeparator()
                                        MenuBarItem("Hash Calculator") { SimulatorSessionManager.openTool(StudioTool.HASH_CALCULATOR) }
                                        MenuBarItem("Thales RSA") { SimulatorSessionManager.openTool(StudioTool.THALES_RSA) }
                                    }
                                    MenuBarSubMenu("Key Management") {
                                        MenuBarItem("DEA Keys") { SimulatorSessionManager.openTool(StudioTool.DEA_KEYS) }
                                        MenuBarItem("Keyshare Generator") { SimulatorSessionManager.openTool(StudioTool.KEYSHARE_GENERATOR) }
                                        MenuBarSeparator()
                                        MenuBarItem("Thales Keys") { SimulatorSessionManager.openTool(StudioTool.THALES_KEYS) }
                                        MenuBarItem("Futurex Keys") { SimulatorSessionManager.openTool(StudioTool.FUTUREX_KEYS) }
                                        MenuBarItem("Atalla Keys") { SimulatorSessionManager.openTool(StudioTool.ATALLA_KEYS) }
                                        MenuBarItem("SafeNet Keys") { SimulatorSessionManager.openTool(StudioTool.SAFENET_KEYS) }
                                        MenuBarSeparator()
                                        MenuBarItem("Thales Key Blocks") { SimulatorSessionManager.openTool(StudioTool.THALES_KEY_BLOCKS) }
                                        MenuBarItem("TR-31 Key Blocks") { SimulatorSessionManager.openTool(StudioTool.TR31_KEY_BLOCKS) }
                                        MenuBarItem("RSA DER Keys") { SimulatorSessionManager.openTool(StudioTool.RSA_DER_KEYS) }
                                        MenuBarItem("SSL Certificate") { SimulatorSessionManager.openTool(StudioTool.SSL_CERTIFICATE) }
                                    }
                                    MenuBarSubMenu("Payment & MAC") {
                                        MenuBarItem("CVV Calculator") { SimulatorSessionManager.openTool(StudioTool.CVV_CALCULATOR) }
                                        MenuBarItem("Amex CSC") { SimulatorSessionManager.openTool(StudioTool.AMEX_CSC) }
                                        MenuBarItem("MasterCard CVC3") { SimulatorSessionManager.openTool(StudioTool.MASTERCARD_CSC) }
                                        MenuBarSeparator()
                                        MenuBarItem("DUKPT ISO 9797") { SimulatorSessionManager.openTool(StudioTool.DUKPT_ISO_9797) }
                                        MenuBarItem("DUKPT AES") { SimulatorSessionManager.openTool(StudioTool.DUKPT_ISO_AES) }
                                        MenuBarItem("ISO/IES 9797-1 MAC") { SimulatorSessionManager.openTool(StudioTool.ISO_IES_9797_1_MAC) }
                                        MenuBarItem("ANSI MAC") { SimulatorSessionManager.openTool(StudioTool.ANSI_MAC) }
                                        MenuBarItem("AS2805 MAC") { SimulatorSessionManager.openTool(StudioTool.AS2805_MAC) }
                                        MenuBarItem("3DES CBC MAC") { SimulatorSessionManager.openTool(StudioTool.TDES_CBC_MAC) }
                                        MenuBarItem("HMAC") { SimulatorSessionManager.openTool(StudioTool.HMAC_MAC) }
                                        MenuBarItem("CMAC") { SimulatorSessionManager.openTool(StudioTool.CMAC_MAC) }
                                        MenuBarItem("Retail MAC") { SimulatorSessionManager.openTool(StudioTool.RETAIL_MAC) }
                                        MenuBarItem("MDC Hash") { SimulatorSessionManager.openTool(StudioTool.MDC_HASH) }
                                        MenuBarSeparator()
                                        MenuBarItem("PIN Block General") { SimulatorSessionManager.openTool(StudioTool.PIN_BLOCK_GENERAL) }
                                        MenuBarItem("AES PIN Block") { SimulatorSessionManager.openTool(StudioTool.PIN_BLOCK_AES) }
                                        MenuBarItem("PIN Offset (IBM)") { SimulatorSessionManager.openTool(StudioTool.PIN_OFFSET_IBM) }
                                        MenuBarItem("PIN PVV") { SimulatorSessionManager.openTool(StudioTool.PIN_PVV) }
                                        MenuBarItem("ZKA") { SimulatorSessionManager.openTool(StudioTool.ZKA) }
                                    }
                                    MenuBarSubMenu("Data Converters") {
                                        MenuBarItem("Base64 Encoder") { SimulatorSessionManager.openTool(StudioTool.BASE64_ENCODER) }
                                        MenuBarItem("Base94 Encoder") { SimulatorSessionManager.openTool(StudioTool.BASE94_ENCODER) }
                                        MenuBarItem("BCD Converter") { SimulatorSessionManager.openTool(StudioTool.BCD_CONVERTER) }
                                        MenuBarItem("Character Encoder") { SimulatorSessionManager.openTool(StudioTool.CHARACTER_ENCODER) }
                                        MenuBarItem("Check Digit") { SimulatorSessionManager.openTool(StudioTool.CHECK_DIGIT) }
                                        MenuBarSeparator()
                                        MenuBarItem("Message Parser") { SimulatorSessionManager.openTool(StudioTool.MESSAGE_PARSER) }
                                        MenuBarItem("AS2805 Calculator") { SimulatorSessionManager.openTool(StudioTool.AS2805_CALCULATOR) }
                                        MenuBarItem("Bitmap Calculator") { SimulatorSessionManager.openTool(StudioTool.BITMAP_CALCULATOR) }
                                        MenuBarItem("APDU Response Query") { SimulatorSessionManager.openTool(StudioTool.APDU_RESPONSE_QUERY) }
                                    }
                                }
                                // Configuration
                                MenuBarMenu("Configuration", 3) {
                                    MenuBarItem("Export Configuration") {
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
                                                        ) { Text("Configuration exported successfully!") }
                                                    }
                                                }
                                                is ExportResult.Cancelled -> println("Export cancelled")
                                                is ExportResult.Error -> {
                                                    appState.value.resultDialogInterface?.onError {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center,
                                                        ) { Text((file as ExportResult.Error).message) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    MenuBarItem("Import Configuration") {
                                        isoCoroutine.launch {
                                            val file = FileImporter().importFile(
                                                window = window,
                                                fileExtensions = listOf("json"),
                                                importLogic = { f -> appState.value.import(f) }
                                            )
                                            when (file) {
                                                is ImportResult.Success -> {
                                                    appState.value.resultDialogInterface?.onSuccess {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) { Text("Configuration imported successfully!") }
                                                    }
                                                }
                                                is ImportResult.Cancelled -> println("Import cancelled")
                                                is ImportResult.Error -> {
                                                    appState.value.resultDialogInterface?.onError {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) { Text(file.message) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    MenuBarSeparator()
                                    MenuBarItem("Settings") { navigationController.navigateTo(Destination.GlobalSettings) }
                                }
                                // Help
                                MenuBarMenu("Help", 4) {
                                    MenuBarItem("About ISO8583 Studio") { showAboutDialog = true }
                                    MenuBarSeparator()
                                    MenuBarItem("Exit") { exitApplication() }
                                }
                            }

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
                                                        val configDest = when (session.simulatorType) {
                                                            SimulatorType.HOST        -> Destination.HostSimulatorConfig
                                                            SimulatorType.HSM         -> Destination.HSMSimulatorConfig
                                                            SimulatorType.POS         -> Destination.POSTerminalConfig
                                                            SimulatorType.APDU        -> Destination.ApduSimulatorConfig
                                                            SimulatorType.ECR         -> Destination.EcrSimulatorConfigScreen
                                                            SimulatorType.ATM         -> Destination.ATMSimulatorConfig
                                                            SimulatorType.HSM_COMMAND -> Destination.HsmCommandConfig
                                                            SimulatorType.SWITCH      -> Destination.PaymentSwitchConfig
                                                            SimulatorType.ACQUIRER    -> Destination.AcquirerGatewayConfig
                                                            SimulatorType.ISSUER      -> Destination.IssuerSystemConfig
                                                            else -> Destination.Home
                                                        }
                                                        navigationController.navigateTo(configDest)
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
                                        GlassWindowEffect(
                                            window = window,
                                            backdrop = GlassBackdrop.MICA,
                                            darkMode = isDark,
                                        )
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

        SimulatorType.HSM_COMMAND -> {
            val config = session.config as `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.hsmCommand.HsmCommandConfig
            `in`.aicortex.iso8583studio.ui.screens.hsmCommand.HsmCommandScreen(
                config = config,
                onBack = onBack,
                service = session.hsmCommandService,
                onSaveConfig = { updatedConfig ->
                    appState.value.updateConfig(updatedConfig)
                    appState.value.save()
                }
            )
        }

        SimulatorType.TOOL -> {
            val screen = session.toolScreen
            if (screen != null) {
                Navigator(screen = screen) { nav ->
                    // Voyager 1.1.0-beta02 uses `providesDefault` for LocalNavigatorStateHolder,
                    // so all nested Navigators share the root's SaveableStateHolder. CurrentScreen()
                    // always registers key "${screen.key}:currentScreen" in that shared holder.
                    // If this nested navigator and the root both show the same screen (e.g.
                    // Destination.Home), the duplicate key crashes the app.
                    //
                    // Fix: call saveableState with a session-unique suffix instead of the fixed
                    // "currentScreen" used by CurrentScreen(). This keeps all lifecycle/state
                    // management intact while guaranteeing key uniqueness in the shared holder.
                    nav.saveableState("session_${session.id}") {
                        nav.lastItem.Content()
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