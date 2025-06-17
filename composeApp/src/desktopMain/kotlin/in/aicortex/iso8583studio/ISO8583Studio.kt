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
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import `in`.aicortex.iso8583studio.data.ExceptionHandler
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.domain.FileImporter
import `in`.aicortex.iso8583studio.domain.ImportResult
import `in`.aicortex.iso8583studio.domain.utils.ExportResult
import `in`.aicortex.iso8583studio.domain.utils.FileExporter
import `in`.aicortex.iso8583studio.ui.AppTheme
import `in`.aicortex.iso8583studio.ui.ErrorRed
import `in`.aicortex.iso8583studio.ui.Studio.appState
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.navigation.Destination
import `in`.aicortex.iso8583studio.ui.screens.components.StatusBadge
import `in`.aicortex.iso8583studio.ui.screens.about.AboutDialog
import `in`.aicortex.iso8583studio.ui.navigation.rememberNavigationController
//import `in`.aicortex.iso8583studio.ui.screens.StudioMain
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

            // Set the custom exception handler for the current thread
            Thread.currentThread().uncaughtExceptionHandler = ExceptionHandler()

            // Alternatively, set it for all threads
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

                                                is ExportResult.Cancelled -> {
                                                    println("Export cancelled")
                                                }

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
                                    Item(
                                        text = "Import"
                                    ) {
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

                                                is ImportResult.Cancelled -> {
                                                    println("Import cancelled")
                                                }

                                                is ImportResult.Error -> {
                                                    appState.value.resultDialogInterface?.onError {
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
                                if (!Desktop.isDesktopSupported()) {
                                    Item("About") {
                                        showAboutDialog = true
                                    }
                                }

                                Item("Quit") {
                                    exitApplication()
                                }
                            }


                            Menu(text = "Generic") {
                                Item(text = "Hashes") { navigationController.navigateTo(Destination.HashCalculator) }
                                Item(text = "Character Encoding") {
                                    navigationController.navigateTo(
                                        Destination.CharacterEncoder
                                    )
                                }
                                Item(text = "BCD") { navigationController.navigateTo(Destination.BcdCalculator) }
                                Item(text = "Check Digits") {
                                    navigationController.navigateTo(
                                        Destination.CheckDigit
                                    )
                                }
                                Item(text = "Base64") { navigationController.navigateTo(Destination.Base64Calculator) }
                                Item(text = "Base94") { navigationController.navigateTo(Destination.Base94Calculator) }
                                Item(text = "Message Parser") {
                                    navigationController.navigateTo(
                                        Destination.MessageParser
                                    )
                                }
                                Item(text = "RSA DER Public Key") {
                                    navigationController.navigateTo(
                                        Destination.RsaDerPubKeyCalculator
                                    )
                                }
                            }

                            Menu(text = "Payments") {
                                Item("AS2805") { navigationController.navigateTo(Destination.As2805Calculator) }
                                Item("Bitmap") { navigationController.navigateTo(Destination.BitmapCalculator) }
                                Menu("Card Validation") {
                                    Item("CVVs") { navigationController.navigateTo(Destination.CvvCalculator) }
                                    Item("AMEX CSCs") { navigationController.navigateTo(Destination.AmexCscCalculator) }
                                    Item("MasterCard dynamic CVC3") { navigationController.navigateTo(Destination.Cvc3MasterCardScreen)  }
                                }
                                Menu("DUKPT") {
                                    Item("ISO 9797") { navigationController.navigateTo(Destination.DukptIso9797) }
                                    Item("AES") { navigationController.navigateTo(Destination.DukptIsoAES) }
                                }
                                Menu("MAC Algorithms") {
                                    Item("ISO/IEC 9797-1") { navigationController.navigateTo(Destination.Isoies97971mac) }
                                    Item("ANSI X9.9 & X9.19") { navigationController.navigateTo(Destination.AnsiMac) }
                                    Item("AS2805 4.1") { navigationController.navigateTo(Destination.AS2805MacScreen)  }
                                    Item("TDES CBC-MAC") { navigationController.navigateTo(Destination.TDESCBCMACScreen) }
                                    Item("HMAC") { navigationController.navigateTo(Destination.HMACScreen) }
                                    Item("CMAC") { navigationController.navigateTo(Destination.CMACScreen) }
                                    Item("Retail") { navigationController.navigateTo(Destination.RetailMACScreen) }
                                }
                                Item("MDC Hash") { navigationController.navigateTo(Destination.MdcHashCalculatorScreen) }
                                Menu("PIN Blocks") {
                                    Item("General") { navigationController.navigateTo(Destination.PinBlockGeneralScreen) }
                                    Item("AES") { navigationController.navigateTo(Destination.AESPinBlockScreen) }
                                }
                                Item("PIN Offset") { navigationController.navigateTo(Destination.PinOffsetScreen)  }
                                Item("PIN PVV") { navigationController.navigateTo(Destination.PinPvvScreen) }
                                Item("ZKA") { navigationController.navigateTo(Destination.ZKAScreen) }
                            }

                            Menu(text = "EMV") {
                                Menu(text = "Application Cryptogram") {
                                    Item("EMV v4.1") {
                                        navigationController.navigateTo(Destination.EMV4_1)
                                    }
                                    Item("EMV v4.2") {
                                        navigationController.navigateTo(Destination.EMV4_2)
                                    }
                                    Item("MasterCard") {
                                        navigationController.navigateTo(Destination.EMVMasterCardCrypto)
                                    }
                                    Item("VSDC") {
                                        navigationController.navigateTo(Destination.EMVVsdcCrypto)
                                    }
                                }
                                Item(text = "SDA") {
                                    navigationController.navigateTo(Destination.SDA)
                                }

                                Item(text = "DDA") {
                                    navigationController.navigateTo(Destination.DDA)
                                }

                                Menu(text = "ICC Dynamic Number") {
                                    Item("MasterCard (EMV 3.1.1)") {
                                        navigationController.navigateTo(Destination.ICCDynamicNumberMasterCard)
                                    }
                                }

                                Menu(text = "Data Storage Partial Key") {
                                    Item("MasterCard") {
                                        navigationController.navigateTo(Destination.DataStoragePartialKeyMaterCard)
                                    }
                                }
                                Menu(text = "Secure Messaging") {
                                    Item("MasterCard") {
                                        navigationController.navigateTo(Destination.SecureMessagingMasterCard)
                                    }
                                    Item("Visa") {
                                        navigationController.navigateTo(Destination.SecureMessagingVisa)
                                    }
                                }
                                Menu(text = "HCE") {
                                    Item("Visa") {
                                        navigationController.navigateTo(Destination.HceVisa)
                                    }
                                }
                                Item(text = "CAP Token Computation") {
                                    navigationController.navigateTo(Destination.CapTokenComputation)
                                }
                                Separator()
                                Item(text = "ATR Parser") {
                                    navigationController.navigateTo(Destination.AtrParser)
                                }

                                Item(text = "EMV Data Parser") {
                                    navigationController.navigateTo(Destination.EmvDataParser)
                                }

                                Item(text = "EMV Tag Dictionary") {
                                    navigationController.navigateTo(Destination.EmvTagDictionary)
                                }

                                Item(text = "APDU response query") {
                                    navigationController.navigateTo(Destination.ApduResponseQuery)
                                }
                            }

                            Menu(text = "Cipher") {
                                Item(text = "AES") { navigationController.navigateTo(Destination.AesCalculator) }
                                Item(text = "DES") { navigationController.navigateTo(Destination.DesCalculator) }
                                Item(text = "RSA") { navigationController.navigateTo(Destination.RsaCalculator) }
                                Item(text = "Thales RSA") {
                                    navigationController.navigateTo(
                                        Destination.ThalesRsaCalculator
                                    )
                                }
                                Item(text = "ECDSA") { navigationController.navigateTo(Destination.EcdsaCalculator) }
                                Item(text = "FPE") { navigationController.navigateTo(Destination.FpeCalculator) }
                            }

                            Menu(text = "Keys") {
                                Item("Keys DEA") { navigationController.navigateTo(Destination.DeaKeyCalculator) }
                                Item("Keyshare Generator") {
                                    navigationController.navigateTo(
                                        Destination.KeyshareGenerator
                                    )
                                }
                                Menu("Keys HSM") {
                                    Item("Futurex") { navigationController.navigateTo(Destination.FuturexKeyCalculator) }
                                    Item("Atalla") { navigationController.navigateTo(Destination.AtallaKeyCalculator) }
                                    Item("SafeNet") { navigationController.navigateTo(Destination.SafeNetKeyCalculator) }
                                    Item("Thales") { navigationController.navigateTo(Destination.ThalesKeyCalculator) }
                                }
                                Menu("Key Blocks") {
                                    Item("Thales") { navigationController.navigateTo(Destination.ThalesKeyBlockCalculator) }
                                    Item("TR-31") { navigationController.navigateTo(Destination.TR31KeyBlockCalculator) }
                                }
                                Item("SSL Certificates") {
                                    navigationController.navigateTo(
                                        Destination.SslCertificate
                                    )
                                }
                            }

//                            Menu("Development") {
//                                Item("Secure Padding") { }
//                                Item("String Builder") { }
//                                Item("Trace Parser") { }
//                                Item("Bit Shift") { }
//                            }
                        }
                        if (showAboutDialog) {
                            AboutDialog(onCloseRequest = {
                                showAboutDialog = false
                            })
                        }

                        var showErrorDialog by remember {
                            mutableStateOf<Pair<DialogType, @Composable (() -> Unit)>?>(
                                null
                            )
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
                        appState.value.setComposableWindow(window)
                        CurrentScreen()
//
//                        StudioMain(
//                            navigationController,
//                            appState.value,
//                            window
//                        )
                    }
                }
            }
        }
    }
}