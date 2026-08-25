package `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos

import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdSpec
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu.CardType
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.Transaction
import kotlinx.serialization.Serializable

/**
 * POS Simulator Configuration
 */
@Serializable
data class POSSimulatorConfig(
    override val id: String,
    override val name: String,
    override val description: String,
    override val simulatorType: SimulatorType = SimulatorType.POS,
    override val enabled: Boolean = true,
    override val createdDate: Long,
    override val modifiedDate: Long,
    override val version: String = "1.0",

    // POS-specific properties
    val terminalid: Int,
    val merchantid: Int,
    val acquirerid: Int,
    val supportedCards: List<CardType> = emptyList(),
    val simulatedTransactionsToDest: List<Transaction> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val emvConfig: EMVConfig = EMVConfig(),
    val contactlessConfig: ContactlessConfig = ContactlessConfig(),
    val pinpadConfig: PinpadConfig = PinpadConfig(),
    var profileName: String = "New POS Profile",
    // Hardware
    var pinEntryOptions: String = "Integrated PIN pad",
    var cardReaderTypes: String = "Triple-head reader (MSR + chip + contactless)",
    var displayConfig: String = "Dual displays (merchant + customer)",
    var receiptPrinting: String = "Thermal receipt printer",
    // Transaction
    var terminalCapabilities: Set<String> = setOf(
        "Offline transaction processing",
        "Void and refund processing"
    ),
    var transactionLimits: String = "Standard Retail Limits",
    // Security
    var encryptionSecurity: Set<String> = setOf(
        "End-to-end encryption (E2EE)",
        "PCI DSS compliance level"
    ),
    var authMethods: String = "PIN verification",
    // Network & Software
    var connectivity: String = "Ethernet/LAN connection",
    var osType: String = "Proprietary OS",
    override val serverAddress: String = "localhost",
    override val serverPort: Int = 8080,

    // ---------------------------------------------------------------------------------------
    // v2 — Android device emulation. Defaults preserve compatibility with previously-saved
    // configs: UnifiedSimulatorState reads with `ignoreUnknownKeys = true`, so this block must
    // stay purely additive. Never rename or remove a field above.
    // ---------------------------------------------------------------------------------------

    /** `"<modelId>:<variantId>"` into the terminal catalog, e.g. `pax-a910s:5p5in`. */
    val terminalProfileId: String = "pax-a910s:5in",
    val avd: AvdSpec = AvdSpec(),

    /** Card source. Field names deliberately match APDUSimulatorConfig so the two are learnable. */
    val transportMode: String = "LOOPBACK",
    val pcscReaderName: String = "",
    val serialPortName: String = "",
    val serialBaudRate: Int = 115200,
    /** Id of the CardProfile served in LOOPBACK mode. */
    val activeProfileId: String = "",

    /** The payment APK installed into the emulator, and the package auto-launched after boot. */
    val payloadApkPath: String = "",
    val autoLaunchPackage: String = "",
) : SimulatorConfig


// Supporting data classes for POS
@Serializable
enum class PaymentMethod {
    CONTACT_EMV, CONTACTLESS_EMV, MAGNETIC_STRIPE, QR_CODE, NFC, MOBILE_PAYMENT
}

@Serializable
data class EMVConfig(
    val terminalType: String = "22",
    val terminalCapabilities: String = "E0F8C8",
    val additionalTerminalCapabilities: String = "6000F0A001",
    val applicationVersionNumber: String = "0096"
)

@Serializable
data class ContactlessConfig(
    val enabled: Boolean = true,
    val transactionLimit: Long = 5000L, // in cents
    val clessCapabilities: String = "E0F8C8",
    val kernel2Support: Boolean = true,
    val kernel3Support: Boolean = true
)

@Serializable
data class PinpadConfig(
    val enabled: Boolean = true,
    val pinpadType: String = "INTERNAL",
    val encryptionKey: String = "",
    val offlinePinSupport: Boolean = true
)