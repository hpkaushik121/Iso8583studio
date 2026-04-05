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

                        // ─── Menu Bar ─────────────────────────────────
                        MenuBar {
                            // ══════════════════════════════════════════
                            //  Quick Access — dynamic, most-used tools
                            // ══════════════════════════════════════════
                            Menu(text = "Quick Access") {
                                val topLabels = ToolUsageTracker.topLabels(8)
                                val allTools = StudioTool.values()
                                if (topLabels.isNotEmpty()) {
                                    for (label in topLabels) {
                                        val tool = allTools.firstOrNull { it.label == label }
                                        if (tool != null) {
                                            Item(text = tool.label) {
                                                SimulatorSessionManager.openTool(tool)
                                            }
                                        }
                                    }
                                    Separator()
                                }
                                Item(text = "Home") {
                                    SimulatorSessionManager.activateMainContent()
                                    navigationController.navigateTo(Destination.Home)
                                }
                            }

                            // ══════════════════════════════════════════
                            //  Simulators — grouped by type
                            // ══════════════════════════════════════════
                            Menu(text = "Simulators") {
                                Menu(text = "Transaction Processing") {
                                    Item(text = "Host Simulator") {
                                        SimulatorSessionManager.openTool(StudioTool.HOST_SIMULATOR)
                                    }
                                    Item(text = "POS Terminal") {
                                        SimulatorSessionManager.openTool(StudioTool.POS_TERMINAL)
                                    }
                                    Item(text = "ATM Simulator") {
                                        SimulatorSessionManager.openTool(StudioTool.ATM_SIMULATOR)
                                    }
                                    Item(text = "ECR Simulator") {
                                        SimulatorSessionManager.openTool(StudioTool.ECR_SIMULATOR)
                                    }
                                }
                                Menu(text = "HSM & Security") {
                                    Item(text = "HSM Simulator") {
                                        SimulatorSessionManager.openTool(StudioTool.HSM_SIMULATOR)
                                    }
                                    Item(text = "HSM Host Console") {
                                        SimulatorSessionManager.openTool(StudioTool.HSM_COMMAND)
                                    }
                                    Item(text = "APDU Simulator") {
                                        SimulatorSessionManager.openTool(StudioTool.APDU_SIMULATOR)
                                    }
                                }
                                Menu(text = "Network & Switching") {
                                    Item(text = "Payment Switch") {
                                        SimulatorSessionManager.openTool(StudioTool.PAYMENT_SWITCH)
                                    }
                                    Item(text = "Acquirer Gateway") {
                                        SimulatorSessionManager.openTool(StudioTool.ACQUIRER_GATEWAY)
                                    }
                                    Item(text = "Issuer System") {
                                        SimulatorSessionManager.openTool(StudioTool.ISSUER_SYSTEM)
                                    }
                                }
                            }

                            // ══════════════════════════════════════════
                            //  Tools — organized by ToolSuite categories
                            // ══════════════════════════════════════════
                            Menu(text = "Tools") {
                                Menu(text = "EMV & Card Tools") {
                                    Item(text = "EMV 4.1 Crypto") { SimulatorSessionManager.openTool(StudioTool.EMV_41_CRYPTO) }
                                    Item(text = "EMV 4.2 Crypto") { SimulatorSessionManager.openTool(StudioTool.EMV_42_CRYPTO) }
                                    Item(text = "MasterCard Crypto") { SimulatorSessionManager.openTool(StudioTool.MASTERCARD_CRYPTO) }
                                    Item(text = "VSDC Crypto") { SimulatorSessionManager.openTool(StudioTool.VSDC_CRYPTO) }
                                    Item(text = "SDA Verification") { SimulatorSessionManager.openTool(StudioTool.SDA_VERIFICATION) }
                                    Item(text = "DDA Verification") { SimulatorSessionManager.openTool(StudioTool.DDA_VERIFICATION) }
                                    Separator()
                                    Item(text = "CAP Token") { SimulatorSessionManager.openTool(StudioTool.CAP_TOKEN) }
                                    Item(text = "HCE Visa") { SimulatorSessionManager.openTool(StudioTool.HCE_VISA) }
                                    Item(text = "Secure Messaging") { SimulatorSessionManager.openTool(StudioTool.SECURE_MESSAGING) }
                                    Separator()
                                    Item(text = "ATR Parser") { SimulatorSessionManager.openTool(StudioTool.ATR_PARSER) }
                                    Item(text = "EMV Data Parser") { SimulatorSessionManager.openTool(StudioTool.EMV_DATA_PARSER) }
                                    Item(text = "EMV Tag Dictionary") { SimulatorSessionManager.openTool(StudioTool.EMV_TAG_DICTIONARY) }
                                }

                                Menu(text = "Cryptography") {
                                    Item(text = "AES Calculator") { SimulatorSessionManager.openTool(StudioTool.AES_CALCULATOR) }
                                    Item(text = "DES/3DES Calculator") { SimulatorSessionManager.openTool(StudioTool.DES_CALCULATOR) }
                                    Item(text = "RSA Calculator") { SimulatorSessionManager.openTool(StudioTool.RSA_CALCULATOR) }
                                    Item(text = "ECDSA Calculator") { SimulatorSessionManager.openTool(StudioTool.ECDSA_CALCULATOR) }
                                    Item(text = "FPE Calculator") { SimulatorSessionManager.openTool(StudioTool.FPE_CALCULATOR) }
                                    Separator()
                                    Item(text = "Hash Calculator") { SimulatorSessionManager.openTool(StudioTool.HASH_CALCULATOR) }
                                    Item(text = "Thales RSA") { SimulatorSessionManager.openTool(StudioTool.THALES_RSA) }
                                }

                                Menu(text = "Key Management") {
                                    Item(text = "DEA Keys") { SimulatorSessionManager.openTool(StudioTool.DEA_KEYS) }
                                    Item(text = "Keyshare Generator") { SimulatorSessionManager.openTool(StudioTool.KEYSHARE_GENERATOR) }
                                    Separator()
                                    Item(text = "Thales Keys") { SimulatorSessionManager.openTool(StudioTool.THALES_KEYS) }
                                    Item(text = "Futurex Keys") { SimulatorSessionManager.openTool(StudioTool.FUTUREX_KEYS) }
                                    Item(text = "Atalla Keys") { SimulatorSessionManager.openTool(StudioTool.ATALLA_KEYS) }
                                    Item(text = "SafeNet Keys") { SimulatorSessionManager.openTool(StudioTool.SAFENET_KEYS) }
                                    Separator()
                                    Item(text = "Thales Key Blocks") { SimulatorSessionManager.openTool(StudioTool.THALES_KEY_BLOCKS) }
                                    Item(text = "TR-31 Key Blocks") { SimulatorSessionManager.openTool(StudioTool.TR31_KEY_BLOCKS) }
                                    Item(text = "RSA DER Keys") { SimulatorSessionManager.openTool(StudioTool.RSA_DER_KEYS) }
                                    Item(text = "SSL Certificate") { SimulatorSessionManager.openTool(StudioTool.SSL_CERTIFICATE) }
                                }

                                Menu(text = "Payment & MAC") {
                                    Item(text = "CVV Calculator") { SimulatorSessionManager.openTool(StudioTool.CVV_CALCULATOR) }
                                    Item(text = "Amex CSC") { SimulatorSessionManager.openTool(StudioTool.AMEX_CSC) }
                                    Item(text = "MasterCard CVC3") { SimulatorSessionManager.openTool(StudioTool.MASTERCARD_CSC) }
                                    Separator()
                                    Item(text = "DUKPT ISO 9797") { SimulatorSessionManager.openTool(StudioTool.DUKPT_ISO_9797) }
                                    Item(text = "DUKPT AES") { SimulatorSessionManager.openTool(StudioTool.DUKPT_ISO_AES) }
                                    Item(text = "ISO/IES 9797-1 MAC") { SimulatorSessionManager.openTool(StudioTool.ISO_IES_9797_1_MAC) }
                                    Item(text = "ANSI MAC") { SimulatorSessionManager.openTool(StudioTool.ANSI_MAC) }
                                    Item(text = "AS2805 MAC") { SimulatorSessionManager.openTool(StudioTool.AS2805_MAC) }
                                    Item(text = "3DES CBC MAC") { SimulatorSessionManager.openTool(StudioTool.TDES_CBC_MAC) }
                                    Item(text = "HMAC") { SimulatorSessionManager.openTool(StudioTool.HMAC_MAC) }
                                    Item(text = "CMAC") { SimulatorSessionManager.openTool(StudioTool.CMAC_MAC) }
                                    Item(text = "Retail MAC") { SimulatorSessionManager.openTool(StudioTool.RETAIL_MAC) }
                                    Item(text = "MDC Hash") { SimulatorSessionManager.openTool(StudioTool.MDC_HASH) }
                                    Separator()
                                    Item(text = "PIN Block General") { SimulatorSessionManager.openTool(StudioTool.PIN_BLOCK_GENERAL) }
                                    Item(text = "AES PIN Block") { SimulatorSessionManager.openTool(StudioTool.PIN_BLOCK_AES) }
                                    Item(text = "PIN Offset (IBM)") { SimulatorSessionManager.openTool(StudioTool.PIN_OFFSET_IBM) }
                                    Item(text = "PIN PVV") { SimulatorSessionManager.openTool(StudioTool.PIN_PVV) }
                                    Item(text = "ZKA") { SimulatorSessionManager.openTool(StudioTool.ZKA) }
                                }

                                Menu(text = "Data Converters") {
                                    Item(text = "Base64 Encoder") { SimulatorSessionManager.openTool(StudioTool.BASE64_ENCODER) }
                                    Item(text = "Base94 Encoder") { SimulatorSessionManager.openTool(StudioTool.BASE94_ENCODER) }
                                    Item(text = "BCD Converter") { SimulatorSessionManager.openTool(StudioTool.BCD_CONVERTER) }
                                    Item(text = "Character Encoder") { SimulatorSessionManager.openTool(StudioTool.CHARACTER_ENCODER) }
                                    Item(text = "Check Digit") { SimulatorSessionManager.openTool(StudioTool.CHECK_DIGIT) }
                                    Separator()
                                    Item(text = "Message Parser") { SimulatorSessionManager.openTool(StudioTool.MESSAGE_PARSER) }
                                    Item(text = "AS2805 Calculator") { SimulatorSessionManager.openTool(StudioTool.AS2805_CALCULATOR) }
                                    Item(text = "Bitmap Calculator") { SimulatorSessionManager.openTool(StudioTool.BITMAP_CALCULATOR) }
                                    Item(text = "APDU Response Query") { SimulatorSessionManager.openTool(StudioTool.APDU_RESPONSE_QUERY) }
                                }
                            }

                            // ══════════════════════════════════════════
                            //  Configuration — import/export/settings
                            // ══════════════════════════════════════════
                            Menu(text = "Configuration") {
                                Item(text = "Export Configuration") {
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
                                Item(text = "Import Configuration") {
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
                                Separator()
                                Item(text = "Settings") {
                                    navigationController.navigateTo(Destination.GlobalSettings)
                                }
                            }

                            // ══════════════════════════════════════════
                            //  Help
                            // ══════════════════════════════════════════
                            Menu(text = "Help") {
                                Item(text = "About ISO8583 Studio") { showAboutDialog = true }
                                Separator()
                                Item(text = "Exit") { exitApplication() }
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