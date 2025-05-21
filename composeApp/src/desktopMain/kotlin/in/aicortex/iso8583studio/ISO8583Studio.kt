package `in`.aicortex.iso8583studio

import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.WindowState
import `in`.aicortex.iso8583studio.data.ExceptionHandler
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.domain.FileImporter
import `in`.aicortex.iso8583studio.domain.ImportResult
import `in`.aicortex.iso8583studio.domain.utils.ExportResult
import `in`.aicortex.iso8583studio.domain.utils.FileExporter
import `in`.aicortex.iso8583studio.ui.AppTheme
import `in`.aicortex.iso8583studio.ui.screens.GatewayConfiguration
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import kotlinx.coroutines.launch

enum class DialogType{
    SUCCESS,ERROR,NONE
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
                    size = DpSize(width = 1800.dp, height = 768.dp),
                )
            }
            val isoCoroutine = rememberCoroutineScope()
            AppTheme {
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "ISO8583Studio",
                    state = windowState,
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
                                            fileDescription = "File"
                                        )
                                        if(file is ExportResult.Success){
                                            appState.resultDialogInterface?.onSuccess { Text("Exported successfully!") }
                                        }else if (file is ExportResult.Cancelled){
                                            println("Import cancelled")
                                        } else{
                                            appState.resultDialogInterface?.onError { Text((file as ExportResult.Error).message)}
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
                                        if(file is ImportResult.Success){
                                            appState.resultDialogInterface?.onSuccess { Text("Imported successfully!") }
                                        }else if (file is ImportResult.Cancelled){
                                            println("Import cancelled")
                                        } else{
                                            appState.resultDialogInterface?.onError { Text((file as ImportResult.Error).message)}
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
                            confirmButton = { Button(
                                onClick = {
                                    showErrorDialog = null
                                }
                            ) {
                                Text("Ok")
                            } },
                            title = { Text("Error!") },
                            text = showErrorDialog!!.second,


                            )

                    }

                    if (showErrorDialog?.first == DialogType.SUCCESS) {
                        AlertDialog(
                            onDismissRequest = { showErrorDialog = null },
                            confirmButton = { Button(
                                onClick = {
                                    showErrorDialog = null
                                }
                            ) {
                                Text("Ok")
                            } },
                            title = { Text("Success!") },
                            text = showErrorDialog!!.second,


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