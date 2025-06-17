package `in`.aicortex.iso8583studio.ui.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.ui.Studio.appState
import `in`.aicortex.iso8583studio.ui.screens.Emv.AtrParserScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.CapTokenComputationScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.EmvDataParserScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.EmvTagDictionaryScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.EmvCrypto4_1
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.EmvCrypto4_2
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.MastercardCryptoScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.applicationCryptogram.VsdcCryptoScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.dda.DdaScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.dsPartialKey.DsPartialKeyScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.hce.VisaHceCryptoScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.iccDynamicNumber.IccDynamicNumberScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.sda.SdaScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.secureMessaging.MastercardSecureMessagingScreen
import `in`.aicortex.iso8583studio.ui.screens.Emv.secureMessaging.VisaSecureMessagingScreen
import `in`.aicortex.iso8583studio.ui.screens.HomeScreenViewModel
import `in`.aicortex.iso8583studio.ui.screens.apduquery.ApduResponseQueryScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.AesCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.DesCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.EcdsaCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.RsaCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.cipher.ThalesRsaScreen
import `in`.aicortex.iso8583studio.ui.screens.config.acquirerGateway.AcquiringGatewayConfigScreen
import `in`.aicortex.iso8583studio.ui.screens.config.apduSimulator.ApduSimulatorConfigScreen
import `in`.aicortex.iso8583studio.ui.screens.config.atmSimulator.AtmSimulatorConfigScreen
import `in`.aicortex.iso8583studio.ui.screens.config.ecrSimulator.EcrSimulatorConfigScreen
import `in`.aicortex.iso8583studio.ui.screens.config.hostSimulator.HostSimulatorConfigScreen
import `in`.aicortex.iso8583studio.ui.screens.config.hsmSimulator.HsmSimulatorConfigScreen
import `in`.aicortex.iso8583studio.ui.screens.config.issuerSystem.IssuerSystemConfigScreen
import `in`.aicortex.iso8583studio.ui.screens.config.paymentSwitch.PaymentSwitchConfigScreen
import `in`.aicortex.iso8583studio.ui.screens.config.posTerminal.PosTerminalConfigScreen
import `in`.aicortex.iso8583studio.ui.screens.fpe.FpeCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.AESPinBlockScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.Base64EncoderDecoderScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.Base94EncoderDecoderScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.BcdConverterScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.CharacterEncodingScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.CheckDigitScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.dukpt.DukptIso9797Screen
import `in`.aicortex.iso8583studio.ui.screens.generic.HashCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.MessageParserScreen
import `in`.aicortex.iso8583studio.ui.screens.generic.PinBlockGeneralScreen
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.HostSimulatorScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.DeaKeysScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.KeyshareGeneratorScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.SslUtilityScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.hsmKeys.AtallaKeysScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.hsmKeys.FuturexKeysScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.hsmKeys.SafenetKeysScreen
import `in`.aicortex.iso8583studio.ui.screens.keys.keyBlocks.ThalesKeyBlockScreen
import `in`.aicortex.iso8583studio.ui.screens.landing.HomeScreen
import `in`.aicortex.iso8583studio.ui.screens.monitor.MonitorScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.As2805CalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.BitmapScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.MDCHashScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.PinOffsetScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.PinPvvScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.ZKAScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.cardValidation.AmexCvvScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.cardValidation.CVC3Screen
import `in`.aicortex.iso8583studio.ui.screens.payments.cardValidation.CvvCalculatorScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.dukpt.DUKPTAESScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.ANSIMACScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.AS2805MACScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.CMACScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.HMACScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.ISO9797Screen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.RetailMACScreen
import `in`.aicortex.iso8583studio.ui.screens.payments.macAlgorithms.TDESCBCMACScreen
import `in`.aicortex.iso8583studio.ui.screens.rsader.RsaDerKeyScreen
import `in`.aicortex.iso8583studio.ui.screens.thaleskeys.ThalesKeysScreen
import `in`.aicortex.iso8583studio.ui.screens.tr31keyblock.Tr31KeyBlockScreen

/**
 * Navigation destinations for the app, converted to the Voyager pattern.
 * Each screen object is self-contained and defines its own content.
 */
sealed class Destination : Screen {

    object Home :Screen{
        @Composable
        override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            HomeScreen(
                viewModel = rememberScreenModel { HomeScreenViewModel() },
                onToolSelected = { navigationController.navigateTo(it.screen) },
                onGetStarted = { navigationController.navigateTo(HostSimulatorConfig) }
            )
        }
    }

    object Monitor :Screen{
        @Composable
        override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            MonitorScreen(appState = appState.value, onBack = { navigationController.goBack() })
        }
    }

    object HostSimulatorConfig :Screen{
        @Composable
        override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            HostSimulatorConfigScreen(navigationController = navigationController,
                appState = appState.value)
        }
    }

    object HSMSimulatorConfig :Screen{
        @Composable
        override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            HsmSimulatorConfigScreen(navigationController = navigationController,
                appState = appState.value)
        }
    }

    object HostSimulator :Screen{
        @Composable
        override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            appState.value.window?.let {
                HostSimulatorScreen(
                    window = it,
                    config = appState.value.currentConfig as GatewayConfig,
                    navigationController = navigationController,
                    onBack = { navigationController.goBack() },
                    onError = appState.value.resultDialogInterface!!,
                    onSaveClick = { appState.value.save() }
                )
            }
        }
    }

    object EMV4_1 :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            EmvCrypto4_1(onBack = { navigationController.goBack() })
        }
    }

    object EMV4_2 :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            EmvCrypto4_2(onBack = { navigationController.goBack() })
        }
    }

    object EMVMasterCardCrypto :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            MastercardCryptoScreen(onBack = { navigationController.goBack() })
        }
    }

    object EMVVsdcCrypto :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            VsdcCryptoScreen(onBack = { navigationController.goBack() })
        }
    }

    object SDA :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            SdaScreen(onBack = { navigationController.goBack() })
        }
    }

    object CapTokenComputation :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            CapTokenComputationScreen { navigationController.goBack() }
        }
    }

    object DDA :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            DdaScreen(onBack = { navigationController.goBack() })
        }
    }

    object DataStoragePartialKeyMaterCard :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            DsPartialKeyScreen(onBack = { navigationController.goBack() })
        }
    }

    object HceVisa :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            VisaHceCryptoScreen() { navigationController.goBack() }
        }
    }

    object ICCDynamicNumberMasterCard :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            IccDynamicNumberScreen(onBack = { navigationController.goBack() })
        }
    }

    object SecureMessagingMasterCard :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            MastercardSecureMessagingScreen() { navigationController.goBack() }
        }
    }

    object SecureMessagingVisa :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            VisaSecureMessagingScreen() { navigationController.goBack() }
        }
    }

    object AtrParser :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            AtrParserScreen() { navigationController.goBack() }
        }
    }

    object EmvDataParser :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            EmvDataParserScreen() { navigationController.goBack() }
        }
    }

    object EmvTagDictionary :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            EmvTagDictionaryScreen() { navigationController.goBack() }
        }
    }

    object ApduResponseQuery :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            ApduResponseQueryScreen() { navigationController.goBack() }
        }
    }

    object HashCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            HashCalculatorScreen() { navigationController.goBack() }
        }
    }

    object CharacterEncoder :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            CharacterEncodingScreen() { navigationController.goBack() }
        }
    }

    object BcdCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            BcdConverterScreen() { navigationController.goBack() }
        }
    }

    object CheckDigit :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            CheckDigitScreen() { navigationController.goBack() }
        }
    }

    object Base64Calculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            Base64EncoderDecoderScreen() { navigationController.goBack() }
        }
    }

    object MessageParser :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            MessageParserScreen(
                navigationController = navigationController,
                onError = appState.value.resultDialogInterface!!,
                onBack = { navigationController.goBack() }
            )
        }
    }

    object Base94Calculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            Base94EncoderDecoderScreen() { navigationController.goBack() }
        }
    }

    object RsaDerPubKeyCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            RsaDerKeyScreen() { navigationController.goBack() }
        }
    }

    object AesCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            AesCalculatorScreen() { navigationController.goBack() }
        }
    }

    object DesCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            DesCalculatorScreen() { navigationController.goBack() }
        }
    }

    object RsaCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            RsaCalculatorScreen() { navigationController.goBack() }
        }
    }

    object ThalesRsaCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            ThalesRsaScreen() { navigationController.goBack() }
        }
    }

    object EcdsaCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            EcdsaCalculatorScreen() { navigationController.goBack() }
        }
    }

    object FpeCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            FpeCalculatorScreen() { navigationController.goBack() }
        }
    }

    object DeaKeyCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            DeaKeysScreen() { navigationController.goBack() }
        }
    }

    object KeyshareGenerator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            KeyshareGeneratorScreen() { navigationController.goBack() }
        }
    }

    object FuturexKeyCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            FuturexKeysScreen() { navigationController.goBack() }
        }
    }

    object AtallaKeyCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            AtallaKeysScreen() { navigationController.goBack() }
        }
    }

    object SafeNetKeyCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            SafenetKeysScreen() { navigationController.goBack() }
        }
    }

    object ThalesKeyCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            ThalesKeysScreen() { navigationController.goBack() }
        }
    }

    object ThalesKeyBlockCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            ThalesKeyBlockScreen() { navigationController.goBack() }
        }
    }

    object TR31KeyBlockCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            Tr31KeyBlockScreen() { navigationController.goBack() }
        }
    }

    object SslCertificate :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            SslUtilityScreen() { navigationController.goBack() }
        }
    }

    object As2805Calculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            As2805CalculatorScreen() { navigationController.goBack() }
        }
    }

    object BitmapCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            BitmapScreen() { navigationController.goBack() }
        }
    }

    object CvvCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            CvvCalculatorScreen() { navigationController.goBack() }
        }
    }

    object DukptIso9797 :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            DukptIso9797Screen { navigationController.goBack() }
        }
    }

    object DukptIsoAES :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            DUKPTAESScreen { navigationController.goBack() }
        }
    }

    object Isoies97971mac :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            ISO9797Screen { navigationController.goBack() }
        }
    }
    object AnsiMac :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            ANSIMACScreen { navigationController.goBack() }
        }
    }
    object AS2805MacScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            AS2805MACScreen { navigationController.goBack() }
        }
    }

    object TDESCBCMACScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            TDESCBCMACScreen { navigationController.goBack() }
        }
    }

    object HMACScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            HMACScreen { navigationController.goBack() }
        }
    }

    object CMACScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            CMACScreen { navigationController.goBack() }
        }
    }

    object RetailMACScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            RetailMACScreen { navigationController.goBack() }
        }
    }
    object MdcHashCalculatorScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            MDCHashScreen { navigationController.goBack() }
        }
    }
    object PinBlockGeneralScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            PinBlockGeneralScreen { navigationController.goBack() }
        }
    }

    object AESPinBlockScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            AESPinBlockScreen { navigationController.goBack() }
        }
    }
    object PinOffsetScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            PinOffsetScreen { navigationController.goBack() }
        }
    }
    object PinPvvScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            PinPvvScreen { navigationController.goBack() }
        }
    }

    object ZKAScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            ZKAScreen { navigationController.goBack() }
        }
    }

    object AmexCscCalculator :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            AmexCvvScreen() { navigationController.goBack() }
        }
    }


    object Cvc3MasterCardScreen : Screen {
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            CVC3Screen{
                navigationController.goBack()
            }
        }
    }

    object HSMSimulator :Screen{
        @Composable override fun Content() {
            // TODO: Implement HSMSimulator Screen
        }
    }

    object POSTerminal :Screen{
        @Composable override fun Content() {
            // TODO: Implement POSTerminal Screen
        }
    }

    object POSTerminalConfig :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            PosTerminalConfigScreen(
                navigationController = navigationController,
                appState = appState.value
            )
        }
    }
    object EcrSimulatorConfigScreen :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            EcrSimulatorConfigScreen(
                navigationController = navigationController,
                appState = appState.value
            )
        }
    }

    object ATMSimulatorConfig :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            AtmSimulatorConfigScreen(
                navigationController = navigationController,
                appState = appState.value
            )
        }
    }

    object PaymentSwitchConfig :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            PaymentSwitchConfigScreen(
                navigationController = navigationController,
                appState = appState.value
            )
        }
    }

    object AcquirerGatewayConfig :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            AcquiringGatewayConfigScreen(
                navigationController = navigationController,
                appState.value
            )
        }
    }

    object IssuerSystemConfig :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            IssuerSystemConfigScreen(navigationController = navigationController,
                appState = appState.value)
        }
    }

    object ApduSimulatorConfig :Screen{
        @Composable override fun Content() {
            val navigationController = rememberNavigationController(LocalNavigator.currentOrThrow)
            ApduSimulatorConfigScreen(navigationController = navigationController,
                appState = appState.value)
        }
    }
    object ATMSimulator :Screen{
        @Composable override fun Content() {
            // TODO: Implement ATMSimulator Screen
        }
    }

    object PaymentSwitch :Screen{
        @Composable override fun Content() {
            // TODO: Implement PaymentSwitch Screen
        }
    }

    object AcquirerGateway :Screen{
        @Composable override fun Content() {
            // TODO: Implement AcquirerGateway Screen
        }
    }

    object IssuerSystem :Screen{
        @Composable override fun Content() {
            // TODO: Implement IssuerSystem Screen
        }
    }

    object ApduSimulator :Screen{
        @Composable override fun Content() {
            // TODO: Implement ApduSimulator Screen
        }
    }
}
