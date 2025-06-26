package `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.emvCrypto41

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.CalculatorTab
import `in`.aicortex.iso8583studio.ui.screens.components.CalculatorView


// Enhanced EMV Tab enum
enum class EmvCryptoTabs(val title: String, val icon: ImageVector) {
    UDK_DERIVATION("UDK Derivation", Icons.Default.Key),
    SESSION_KEYS("Session Keys", Icons.Default.VpnKey),
    CRYPTOGRAM("Cryptogram", Icons.Default.Security),
    ARPC("ARPC", Icons.Default.Reply),
    UTILITIES("Utilities", Icons.Default.Build)
}



@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EmvCrypto4_1(
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabList = EmvCryptoTabs.values().toList()
    val selectedTab = tabList[selectedTabIndex]

    Scaffold(
        topBar = {
            AppBarWithBack(
                title = "EMV 4.1 Crypto Calculator",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { /* No action needed */ }) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Export Logs",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { /* No action needed */ }) {
                        Icon(
                            Icons.Default.Help,
                            contentDescription = "Help",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CalculatorView(
                tabList = listOf(
                    CalculatorTab(
                        title = "UDK Derviation",
                        icon = Icons.Default.Key,
                        content = { calculatorLogManager, tab ->
                            UdkDerivationTab(calculatorLogManager,tab)
                        }
                    ),
                    CalculatorTab(
                        title = "Session Keys",
                        icon = Icons.Default.VpnKey,
                        content = { calculatorLogManager, tab ->
                            SessionKeysTab(calculatorLogManager,tab)
                        }
                    ),
                    CalculatorTab(
                        title = "Cryptogram",
                        icon = Icons.Default.Security,
                        content = { calculatorLogManager, tab ->
                            CryptogramTab(calculatorLogManager,tab)
                        }
                    ),
                    CalculatorTab(
                        title = "ARPC",
                        icon = Icons.Default.Reply,
                        content = { calculatorLogManager, tab ->
                            ArpcTab(calculatorLogManager,tab)
                        }
                    ),
                    CalculatorTab(
                        title = "Utilities",
                        icon = Icons.Default.Build,
                        content = { calculatorLogManager, tab ->
                            UtilitiesTab(calculatorLogManager,tab)
                        }
                    )
                ),
                onTabSelected = { selectedTabIndex = it }
            )
        }
    }
}