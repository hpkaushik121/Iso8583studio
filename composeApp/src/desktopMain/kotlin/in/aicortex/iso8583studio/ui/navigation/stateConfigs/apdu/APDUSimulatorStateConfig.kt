package `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.apdu

import `in`.aicortex.iso8583studio.data.SimulatorConfig
import `in`.aicortex.iso8583studio.domain.utils.ApduUtil
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import kotlinx.serialization.Serializable

/**
 * APDU Simulator Configuration
 */
@Serializable
data class APDUSimulatorConfig(
    override val id: String,
    override val name: String,
    override val description: String = "",
    override val simulatorType: SimulatorType = SimulatorType.APDU,
    override val enabled: Boolean = true,
    override val createdDate: Long = System.currentTimeMillis(),
    override val modifiedDate: Long = System.currentTimeMillis(),
    override val version: String = "1.0",

    // APDU-specific properties
    val cardType: CardType = CardType.EMV_CONTACT,
    val atr: String = "3B9F95801FC78031E073FE211B63004C45544F4E",
    val applications: List<CardApplication> = emptyList(),
    val fileSystem: CardFileSystem = CardFileSystem(),
    val securityDomain: SecurityDomain = SecurityDomain(),
    val scriptCommands: List<APDUScript> = emptyList(),

    val connectionInterface: ConnectionInterface,
    val readerName: String = "",
    val applicationAid: String = "",
    val maxSessionTime: Int = 300,
    val logFileName: String = "card_simulator.log",
    val maxLogSizeInMB: Int = 10,
    val tlvTemplate: Map<String, String> = emptyMap(),
    val apduCommandSet: List<ApduCommand> = emptyList(),
    var emvVersion: String = "EMV 4.3",
    var cardNumber: String = "",
    var expiryDate: String = "", // MM/YY format
    var cvv: String = "",
    var cardholderName: String = "",
    // Status
    var isTestProfile: Boolean = false,
    var inUse: Boolean = false,
    // Interface Config
    var contactEnabled: Boolean = true,
    var contactlessEnabled: Boolean = true,
    var magstripeEnabled: Boolean = false,
    val initialState: String = "Active",
    val pinAttemptsRemaining: String = "3",
    val blockOnPinExhaustion: Boolean = true,
    val blockOnTransactionLimit: Boolean = false,
) : SimulatorConfig

enum class ConnectionInterface {
    PC_SC,     // PC/SC smart card readers
    NFC,       // NFC communication
    MOCK,      // Mock/simulated interface
    USB        // USB card readers
}

@Serializable
// Supporting data classes
data class ApduCommand(
    val cla: Byte,
    val ins: Byte,
    val p1: Byte,
    val p2: Byte,
    val lc: Int,
    val data: ByteArray,
    val le: Int,
    val raw: ByteArray
) {
    fun toHexString(): String = ApduUtil.bytesToHexString(raw)

    fun getInstructionName(): String = ApduUtil.getInstructionDescription(ins)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApduCommand

        if (cla != other.cla) return false
        if (ins != other.ins) return false
        if (p1 != other.p1) return false
        if (p2 != other.p2) return false
        if (lc != other.lc) return false
        if (!data.contentEquals(other.data)) return false
        if (le != other.le) return false
        if (!raw.contentEquals(other.raw)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cla.toInt()
        result = 31 * result + ins.toInt()
        result = 31 * result + p1.toInt()
        result = 31 * result + p2.toInt()
        result = 31 * result + lc
        result = 31 * result + data.contentHashCode()
        result = 31 * result + le
        result = 31 * result + raw.contentHashCode()
        return result
    }
}

// Supporting data classes for APDU
@Serializable
enum class CardType {
    EMV_CONTACT,
    EMV_CONTACTLESS,
    MIFARE_CLASSIC,
    MIFARE_DESFIRE,
    JAVA_CARD,
    CUSTOM,
}

@Serializable
data class CardApplication(
    val aid: Int,
    val name: String,
    val version: String,
    val priority: Int = 0,
    val selectable: Boolean = true
)

@Serializable
data class CardFileSystem(
    val masterFile: String = "3F00",
    val dedicatedFiles: List<String> = emptyList(),
    val elementaryFiles: List<String> = emptyList()
)

@Serializable
data class SecurityDomain(
    val aid: String = "A000000003000000",
    val privileges: List<String> = emptyList(),
    val keyVersionNumber: Int = 1
)

@Serializable
data class APDUScript(
    val name: String,
    val description: String,
    val commands: List<APDUCommand>
)

@Serializable
data class APDUCommand(
    val cla: String,
    val ins: String,
    val p1: String,
    val p2: String,
    val data: String = "",
    val le: String = "",
    val expectedSw: String = "9000"
)