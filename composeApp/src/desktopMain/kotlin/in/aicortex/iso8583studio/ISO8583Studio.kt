package `in`.aicortex.iso8583studio

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import `in`.aicortex.iso8583studio.ui.components.StatusBadge
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.screens.GatewayConfiguration
import iso8583studio.composeapp.generated.resources.Res
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
                    state = windowState
                ) {
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
                                            fileExtension = "cfg",
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
                                                        StatusBadge(
                                                            text = "SUCCESS",
                                                            color = SuccessGreen,
                                                            modifier = Modifier.padding(bottom = 8.dp)
                                                        )
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
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        StatusBadge(
                                                            text = "ERROR",
                                                            color = ErrorRed,
                                                            modifier = Modifier.padding(bottom = 8.dp)
                                                        )
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
                                            fileExtensions = listOf("cfg"),
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
                                                        StatusBadge(
                                                            text = "SUCCESS",
                                                            color = SuccessGreen,
                                                            modifier = Modifier.padding(bottom = 8.dp)
                                                        )
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
                                                        StatusBadge(
                                                            text = "ERROR",
                                                            color = ErrorRed,
                                                            modifier = Modifier.padding(bottom = 8.dp)
                                                        )
                                                        Text((file as ImportResult.Error).message)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
                                Text(
                                    "Error",
                                    fontWeight = FontWeight.Bold
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
                                Text(
                                    "Success",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = showErrorDialog!!.second,
                            shape = MaterialTheme.shapes.medium,
                            backgroundColor = MaterialTheme.colors.surface
                        )
                    }

                    GatewayConfiguration(
                        navigationController,
                        appState
                    )
                }
            }
        }
    }
}