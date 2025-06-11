package `in`.aicortex.iso8583studio

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
import `in`.aicortex.iso8583studio.data.ExceptionHandler
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.domain.FileImporter
import `in`.aicortex.iso8583studio.domain.ImportResult
import `in`.aicortex.iso8583studio.domain.utils.ExportResult
import `in`.aicortex.iso8583studio.domain.utils.FileExporter
import `in`.aicortex.iso8583studio.ui.AppTheme
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.StatusBadge
import `in`.aicortex.iso8583studio.ui.screens.about.AboutDialog
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.Screen
import `in`.aicortex.iso8583studio.ui.screens.GatewayConfiguration
import iso8583studio.composeapp.generated.resources.Res
import iso8583studio.composeapp.generated.resources.app
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

enum class DialogType {
    SUCCESS, ERROR, NONE
}

class ISO8583Studio {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = application {

            // Set the custom exception handler for the current thread
            Thread.currentThread().uncaughtExceptionHandler = ExceptionHandler()

            // Alternatively, set it for all threads
            Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler())

            val navigationController = remember { NavigationController() }
            val appState by navigationController.state.collectAsState()
            val windowState = remember {
                WindowState(
                    size = DpSize(width = 1800.dp, height = 800.dp),
                )
            }
            val isoCoroutine = rememberCoroutineScope()


            AppTheme {
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "ISO8583Studio",
                    state = windowState,
                    icon = painterResource(Res.drawable.app)
                ) {
                    var showAboutDialog by remember { mutableStateOf(false) }
                    MenuBar {
                        Menu(
                            text = "File"
                        ) {
                            Menu(text = "Configuration") {
                                Item(
                                    text = "Export"
                                ) {
                                    isoCoroutine.launch {
                                        val file = FileExporter().exportFile(
                                            window = window,
                                            fileName = "ISO8583Studio",
                                            fileExtension = "json",
                                            fileContent = appState.export().toByteArray(),
                                            fileDescription = "Configuration File"
                                        )
                                        when (file) {
                                            is ExportResult.Success -> {
                                                appState.resultDialogInterface?.onSuccess {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text("Configuration exported successfully!")
                                                    }
                                                }
                                            }
                                            is ExportResult.Cancelled -> {
                                                println("Export cancelled")
                                            }
                                            is ExportResult.Error -> {
                                                appState.resultDialogInterface?.onError {
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
                                Item(
                                    text = "Import"
                                ) {
                                    isoCoroutine.launch {
                                        val file = FileImporter().importFile(
                                            window = window,
                                            fileExtensions = listOf("json"),
                                            importLogic = { file ->
                                                appState.import(file)
                                            }
                                        )
                                        when (file) {
                                            is ImportResult.Success -> {
                                                appState.resultDialogInterface?.onSuccess {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {

                                                        Text("Configuration imported successfully!")
                                                    }
                                                }
                                            }
                                            is ImportResult.Cancelled -> {
                                                println("Import cancelled")
                                            }
                                            is ImportResult.Error -> {
                                                appState.resultDialogInterface?.onError {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {

                                                        Text((file as ImportResult.Error).message)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                            Item("About") {
                                showAboutDialog = true
                            }
                            Item("Quit") {
                                exitApplication()
                            }
                        }


                        Menu(text = "Generic") {
                            Item(text = "Hashes") {  }
                            Item(text = "Character Encoding") {  }
                            Item(text = "BCD") {  }
                            Item(text = "Check Digits") {  }
                            Item(text = "Base64") {  }
                            Item(text = "Base94") {  }
                            Item(text = "RSA DER Public Key") {  }
                            Item(text = "UUID") {  }
                        }

                        Menu(text = "Payments") {
                            Item("AS2805") { }
                            Item("Bitmap") { }
                            Menu ("Card Validation") {
                                Item("CVVs") {  }
                                Item("AMEX CSCs") {  }
                                Item("MasterCard dynamic CVC3") {  }
                            }
                            Menu("DUKPT") {
                                Item("ISO 9797") {  }
                                Item("AES") {  }
                            }
                            Menu("MAC Algorithms") {
                                Item("ISO/IEC 9797-1") {  }
                                Item("ANSI X9.9 & X9.19") {  }
                                Item("AS2805 4.1") {  }
                                Item("TDES CBC-MAC") {  }
                                Item("HMAC") {  }
                                Item("CMAC") {  }
                                Item("Retail") {  }
                            }
                            Item("MDC Hash") {  }
                            Menu("PIN Blocks") {
                                Item("General") {  }
                                Item("AES") {  }
                            }
                            Item("PIN Offset") {  }
                            Item("PIN PVV") {  }
                            Item("Visa Certificates") {  }
                            Item("ZKA") {  }
                        }

                        Menu(text = "EMV"){
                            Menu(text = "Application Cryptogram") {
                                Item("EMV v4.1") {
                                    navigationController.navigateTo(Screen.EMV4_1)
                                }
                                Item("EMV v4.2") {
                                    navigationController.navigateTo(Screen.EMV4_2)
                                }
                                Item("MasterCard") {
                                    navigationController.navigateTo(Screen.EMVMasterCardCrypto)
                                }
                                Item("VSDC") {
                                    navigationController.navigateTo(Screen.EMVVsdcCrypto)
                                }
                            }
                            Item(text = "SDA"){
                                navigationController.navigateTo(Screen.SDA)
                            }

                            Item(text = "DDA"){
                                navigationController.navigateTo(Screen.DDA)
                            }

                            Menu(text = "ICC Dynamic Number"){
                                Item("MasterCard (EMV 3.1.1)") {
                                    navigationController.navigateTo(Screen.ICCDynamicNumberMasterCard)
                                }
                            }

                            Menu(text = "Data Storage Partial Key"){
                                Item("MasterCard") {
                                    navigationController.navigateTo(Screen.DataStoragePartialKeyMaterCard)
                                }
                            }
                            Menu(text = "Secure Messaging"){
                                Item("MasterCard") {
                                    navigationController.navigateTo(Screen.SecureMessagingMasterCard)
                                }
                                Item("Visa") {
                                    navigationController.navigateTo(Screen.SecureMessagingVisa)
                                }
                            }
                            Menu(text = "HCE"){
                                Item("Visa") {
                                    navigationController.navigateTo(Screen.HceVisa)
                                }
                            }
                            Item(text = "CAP Token Computation"){
                                navigationController.navigateTo(Screen.CapTokenComputation)
                            }
                            Separator()
                            Item(text = "ATR Parser"){
                                navigationController.navigateTo(Screen.AtrParser)
                            }

                            Item(text = "EMV Data Parser"){
                                navigationController.navigateTo(Screen.EmvDataParser)
                            }

                            Item(text = "EMV Tag Dictionary"){
                                navigationController.navigateTo(Screen.EmvTagDictionary)
                            }

                            Item(text = "APDU response query"){
                                navigationController.navigateTo(Screen.ApduResponseQuery)
                            }
                        }

                        Menu(text = "Cipher") {
                            Item(text = "AES") {  }
                            Item(text = "DES") {  }
                            Item(text = "RSA") {  }
                            Item(text = "Thales RSA") {  }
                            Item(text = "ECDSA") {  }
                            Item(text = "FPE") {  }
                        }

                        Menu(text = "Keys") {
                            Item("Keys DEA") {  }
                            Item("Keyshare Generator") {  }
                            Menu("Keys HSM") {
                                Item("Futurex") {  }
                                Item("Atalla") {  }
                                Item("SafeNet") {  }
                                Item("Thales") {  }
                            }
                            Menu("Key Blocks") {
                                Item("Thales") {  }
                                Item("TR-31") {  }
                            }
                            Menu("SSL Certificates") {  }
                        }

                        Menu("Development") {
                            Item("Secure Padding") {  }
                            Item("String Builder") {  }
                            Item("Trace Parser") {  }
                            Item("Bit Shift") {  }
                        }
                    }
                    if(showAboutDialog){
                        AboutDialog(onCloseRequest = {
                            showAboutDialog = false
                        })
                    }

                    var showErrorDialog by remember { mutableStateOf<Pair<DialogType, @Composable (() -> Unit)>?>(null) }

                    appState.resultDialogInterface = object : ResultDialogInterface {
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
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = MaterialTheme.colors.primary
                                    )
                                ) {
                                    Text("OK")
                                }
                            },
                            title = {
                                StatusBadge(
                                    text = "ERROR",
                                    color = ErrorRed,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
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
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = MaterialTheme.colors.primary
                                    )
                                ) {
                                    Text("OK")
                                }
                            },
                            title = {
                                StatusBadge(
                                    text = "SUCCESS",
                                    color = SuccessGreen,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            },
                            text = showErrorDialog!!.second,
                            shape = MaterialTheme.shapes.medium,
                            backgroundColor = MaterialTheme.colors.surface
                        )
                    }

                    GatewayConfiguration(
                        navigationController,
                        appState,
                        window
                    )
                }
            }
        }
    }
}