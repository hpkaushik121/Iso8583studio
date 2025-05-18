package `in`.aicortex.iso8583studio

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import `in`.aicortex.iso8583studio.di.appModule
import `in`.aicortex.iso8583studio.ui.AppTheme
import `in`.aicortex.iso8583studio.ui.screens.GatewayConfiguration
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import org.koin.core.context.startKoin

fun main() = application {

    // Initialize dependency injection
    startKoin {
        modules(appModule)
    }

    val navigationController = remember { NavigationController() }
    val windowState = remember {
        WindowState(
            size = DpSize(width = 1800.dp, height = 768.dp)
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "ISO8583Studio",
        state = windowState
    ) {
        AppTheme {
            GatewayConfiguration(navigationController)
        }
    }
}