package `in`.aicortex.iso8583studio.ui.screens.generic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.ResultDialogInterface
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.Panel
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.UnsolicitedMessageTab

@Composable
fun MessageParserScreen(
    window: ComposeWindow,
    navigationController: NavigationController,
    onError: ResultDialogInterface,
    onBack: () -> Unit
) {
    var selectedField = remember { mutableStateOf<BitAttribute?>(null) }
    var selectedFieldIndex = remember { mutableStateOf<Int?>(null) }
    var showBitmapAnalysis = remember { mutableStateOf(false) }
    var showMessageParser = remember { mutableStateOf(true) }
    var isFirst = remember {
        mutableStateOf(true)
    }
    var animationTrigger = remember { mutableStateOf(0) }
    var rawMessage = remember { mutableStateOf("") }
    var parseError = remember { mutableStateOf<String?>(null) }
    var currentFields = remember { mutableStateOf<Array<BitAttribute>?>(null) }
    var currentBitmap = remember { mutableStateOf<ByteArray?>(null) }
    var searchQuery = remember { mutableStateOf("") }
    Scaffold(
        topBar = { AppBarWithBack(title = "Message Parser", onBackClick = onBack) },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            navigationController.getManagedGatewayService(
                window,
                onError = onError
            )?.let {
                UnsolicitedMessageTab(
                    gw = it,
                    selectedField = selectedField,
                    selectedFieldIndex = selectedFieldIndex,
                    showBitmapAnalysis = showBitmapAnalysis,
                    showMessageParser = showMessageParser,
                    isFirst = isFirst,
                    animationTrigger = animationTrigger,
                    rawMessage = rawMessage,
                    parseError = parseError,
                    currentFields = currentFields,
                    currentBitmap = currentBitmap,
                    searchQuery = searchQuery
                )
            }
        }
    }
}