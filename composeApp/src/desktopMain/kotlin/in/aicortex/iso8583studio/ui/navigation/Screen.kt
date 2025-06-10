package `in`.aicortex.iso8583studio.ui.navigation

/**
 * Navigation destinations for the app
 */
sealed class Screen {
    object GatewayType : Screen()
    object TransmissionSettings : Screen()
    object KeySettings : Screen()
    object LogSettings : Screen()
    object AdvancedOptions : Screen()
    object Monitor : Screen()
    object HostSimulator : Screen()
    object EMV4_1 : Screen()
}