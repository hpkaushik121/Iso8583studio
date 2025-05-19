package `in`.aicortex.iso8583studio

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.WindowState
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.di.appModule
import `in`.aicortex.iso8583studio.ui.AppTheme
import `in`.aicortex.iso8583studio.ui.screens.GatewayConfiguration
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import org.koin.core.context.startKoin

class ISO8583Studio {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) = application {
            // Initialize dependency injection
            startKoin {
                modules(appModule)
            }

            val navigationController = remember { NavigationController() }
            val appState by navigationController.state.collectAsState()
            val windowState = remember {
                WindowState(
                    size = DpSize(width = 1800.dp, height = 768.dp),
                )
            }
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
                                    val path = GatewayConfig(
                                        id = 1,
                                        name = "Test"
                                    ).export()
                                    println(path)
                                }
                                Item(
                                    text = "Import"
                                ) {

                                }
                            }
                        }
                    }

                    GatewayConfiguration(navigationController,
                        appState)
                }
            }
        }
    }
}