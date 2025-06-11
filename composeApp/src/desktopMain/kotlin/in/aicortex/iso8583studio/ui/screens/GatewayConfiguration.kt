package `in`.aicortex.iso8583studio.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.ui.navigation.GatewayConfigurationState
import `in`.aicortex.iso8583studio.ui.navigation.NavigationController
import `in`.aicortex.iso8583studio.ui.navigation.Screen
import `in`.aicortex.iso8583studio.ui.screens.Emv.AtrParserScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.EmvCrypto4_1
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.MastercardCryptoScreen
import `in`.aicortex.iso8583studio.ui.screens.config.AdvancedOptionsTab
import `in`.aicortex.iso8583studio.ui.screens.config.GatewayTypeTab
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.HostSimulatorScreen
import `in`.aicortex.iso8583studio.ui.screens.config.LogSettingsTab
import `in`.aicortex.iso8583studio.ui.screens.monitor.MonitorScreen
import `in`.aicortex.iso8583studio.ui.screens.config.TabContainer
import `in`.aicortex.iso8583studio.ui.screens.config.TransmissionSettingsTab
import `in`.aicortex.iso8583studio.ui.screens.Emv.dda.DdaScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.sda.SdaScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.EmvCrypto4_2
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.VsdcCryptoScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.dsPartialKey.DsPartialKeyScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.iccDynamicNumber.IccDynamicNumberScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.secureMessaging.MastercardSecureMessagingScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.secureMessaging.VisaSecureMessagingScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.hce.VisaHceCryptoScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.CapTokenComputationScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.EmvDataParserScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.EmvTagDictionaryScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.AesCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.apduquery.ApduResponseQueryScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.DesCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.Base94EncoderDecoderScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.Base64EncoderDecoderScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.CheckDigitScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.BcdConverterScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.CharacterEncodingScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.HashCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.RsaCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.rsader.RsaDerKeyScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.ThalesRsaScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.EcdsaCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.fpe.FpeCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.MessageParserScreen
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.HostSimulatorTabs
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.UnsolicitedMessageTab
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun GatewayConfiguration(
    navigationController: NavigationController,
    appState: GatewayConfigurationState,
    window: ComposeWindow
) {

    // Create navigation controller

    when (val screen = navigationController.currentScreen) {
        is Screen.GatewayType -> selectTab(
            navigationController = navigationController,
            appState = appState,
            index = 0,
            content = {
                appState.currentConfig?.let { config ->
                    GatewayTypeTab(config = config) { updatedConfig ->
                        navigationController.saveConfig(updatedConfig)
                    }
                }
            })

        is Screen.TransmissionSettings -> selectTab(
            navigationController = navigationController,
            appState = appState,
            index = 1,
            content = {
                appState.currentConfig?.let { config ->
                    TransmissionSettingsTab(config = config) { updatedConfig ->
                        navigationController.saveConfig(updatedConfig)
                    }
                }
            })

        is Screen.LogSettings -> selectTab(
            navigationController = navigationController,
            appState = appState,
            index = 2,
            content = {
                appState.currentConfig?.let { config ->
                    LogSettingsTab(config = config) { updatedConfig ->
                        navigationController.saveConfig(updatedConfig)
                    }
                }
            })

        is Screen.AdvancedOptions -> selectTab(
            navigationController = navigationController,
            appState = appState,
            index = 3,
            content = {
                appState.currentConfig?.let { config ->
                    AdvancedOptionsTab()
                }
            })

        is Screen.Monitor -> MonitorScreen(
            config = appState.currentConfig,
            onBack = { navigationController.goBack() }
        )

        is Screen.HostSimulator -> HostSimulatorScreen(
            window = window,
            config = appState.currentConfig,
            navigationController = navigationController,
            onBack = { navigationController.goBack() },
            onError = appState.resultDialogInterface!!,
            onSaveClick = {
                appState.save()
            }
        )

        is Screen.KeySettings -> selectTab(
            navigationController = navigationController,
            appState = appState,
            index = 4,
            content = {
//                KeysSettingTab(keysList = emptyList()) { updatedKeys ->
//                    navigationController.updateKeys(updatedKeys)
//                }
            })

        Screen.EMV4_1 -> EmvCrypto4_1(
            window = window,
            onBack = {
                navigationController.goBack()
            }
        )

        Screen.EMV4_2 -> EmvCrypto4_2(
            window = window,
            onBack = {
                navigationController.goBack()
            }
        )

        Screen.EMVMasterCardCrypto -> MastercardCryptoScreen(
            window = window,
            onBack = {
                navigationController.goBack()
            }
        )

        Screen.EMVVsdcCrypto -> VsdcCryptoScreen(
            window = window,
            onBack = {
                navigationController.goBack()
            }
        )

        Screen.SDA -> SdaScreen(
            window = window,
            onBack = {
                navigationController.goBack()
            }
        )

        Screen.CapTokenComputation -> CapTokenComputationScreen(window = window) { navigationController.goBack() }
        Screen.DDA -> DdaScreen(
            window = window,
            onBack = {
                navigationController.goBack()
            }
        )

        Screen.DataStoragePartialKeyMaterCard -> DsPartialKeyScreen(
            window = window,
            onBack = {
                navigationController.goBack()
            }
        )
        Screen.HceVisa -> VisaHceCryptoScreen(window = window) { navigationController.goBack() }
        Screen.ICCDynamicNumberMasterCard -> IccDynamicNumberScreen(window = window,
            onBack = {
                navigationController.goBack()
            })
        Screen.SecureMessagingMasterCard -> MastercardSecureMessagingScreen(window = window) { navigationController.goBack()  }
        Screen.SecureMessagingVisa -> VisaSecureMessagingScreen(window = window) { navigationController.goBack() }
        Screen.AtrParser -> AtrParserScreen(window = window) { navigationController.goBack() }
        Screen.EmvDataParser -> EmvDataParserScreen(window = window) { navigationController.goBack() }
        Screen.EmvTagDictionary -> EmvTagDictionaryScreen(window = window) { navigationController.goBack() }
        Screen.ApduResponseQuery -> ApduResponseQueryScreen(window = window) { navigationController.goBack() }
        Screen.HashCalculator -> HashCalculatorScreen(window = window) { navigationController.goBack() }
        Screen.CharacterEncoder -> CharacterEncodingScreen(window = window) { navigationController.goBack() }
        Screen.BcdCalculator -> BcdConverterScreen(window = window) { navigationController.goBack() }
        Screen.CheckDigit -> CheckDigitScreen(window = window) { navigationController.goBack() }
        Screen.Base64Calculator -> Base64EncoderDecoderScreen (window = window) { navigationController.goBack() }
        Screen.MessageParser -> MessageParserScreen(
            window = window,
            navigationController = navigationController,
            onError = appState.resultDialogInterface!!,
            onBack = { navigationController.goBack() }
        )
        Screen.Base94Calculator -> Base94EncoderDecoderScreen (window = window) { navigationController.goBack() }
        Screen.RsaDerPubKeyCalculator -> RsaDerKeyScreen (window = window) { navigationController.goBack() }
        Screen.AesCalculator -> AesCalculatorScreen(window = window) { navigationController.goBack() }
        Screen.DesCalculator -> DesCalculatorScreen(window = window) { navigationController.goBack() }
        Screen.RsaCalculator -> RsaCalculatorScreen (window = window) { navigationController.goBack() }
        Screen.ThalesRsaCalculator -> ThalesRsaScreen(window = window) { navigationController.goBack() }
        Screen.EcdsaCalculator -> EcdsaCalculatorScreen(window = window) { navigationController.goBack() }
        Screen.FpeCalculator -> FpeCalculatorScreen (window = window) { navigationController.goBack() }
    }
}

@Composable
private fun selectTab(
    navigationController: NavigationController, index: Int, appState: GatewayConfigurationState,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    TabContainer(
        appState = appState,
        selectedTab = index,
        onSelectConfig = { navigationController.selectConfig(it) },
        onAddConfig = { navigationController.addNewConfig() },
        onDeleteConfig = {
            scope.launch {
                navigationController.deleteCurrentConfig()
            }
        },
        onSaveAllConfigs = { navigationController.saveAllConfigs() },
        onOpenHostSimulator = { navigationController.navigateTo(Screen.HostSimulator) },
        onTabSelected = { navigationController.selectTab(it) },
        content = content
    )
}