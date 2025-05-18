package `in`.aicortex.iso8583studio.data.model

import `in`.aicortex.iso8583studio.data.BitSpecific
import `in`.aicortex.iso8583studio.data.BitTemplate
import iso8583studio.composeapp.generated.resources.Res
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XML
import org.jetbrains.compose.resources.Resource
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.charset.Charset
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher

/**
 * Gateway configuration class
 */
@Serializable
data class GatewayConfig(
    var id: Int = 0,
    var name: String,
    var cipherType: CipherType = CipherType.DES,
    var serverPort: Int = 0,
    var destinationPort: Int = 0,
    var destinationServer: String = "",
    var gatewayType: GatewayType = GatewayType.SERVER,
    var transmissionType: TransmissionType = TransmissionType.SYNCHRONOUS,
    var maxLogSizeInMB: Int = 10,
    var transactionTimeOut: Int = 30,
    var logOptions: Int = 0,
    var serverAddress: String = "",
    var textEncoding: MessageEncoding = MessageEncoding.ASCII,
    var messageLengthType: MessageLengthType = MessageLengthType.BCD,
    var privateKey: ByteArray? = null,
    var iv: ByteArray? = null,
    var checkSignature: SignatureChecking = SignatureChecking.NONE,
    var description: String = "",
    var createDate: LocalDateTime = LocalDateTime.now(),
    var createBy: String = "",
    var clientID: String = "",
    var locationID: String = "",
    var password: ByteArray? = null,
    var acceptClientListOnly: Boolean = false,
    var doNotUseHeader: Boolean = false,
    var bitmapInAscii: Boolean = false,
    var respondIfUnrecognized: Boolean = false,
    var maxConcurrentConnection: Int = 100,
    var cipherMode: CipherMode = CipherMode.CBC,
    var enable: Boolean = false,
    var autoRestartAfter: Int = 0,
    var nccRule: Boolean = true,
    var terminateWhenError: Boolean = false,
    var monitorAddress: String = "",
    var monitorPort: Int = 0,
    var waitToRestart: Int = 300,
    var hashAlgorithm: HashAlgorithm = HashAlgorithm.SHA1,
    var allowLoadKEK: Boolean = false,
    var gwBitTemplate: Array<BitSpecific>? = null,
    var allowWrongParsedData: Boolean = false,
    var keyExpireAfter: Int = 0,
    var addNewClientWhenLoadKEK: Boolean = false,
    var advancedOptions: AdvancedOptions? = null,
    var _logFileName: String = "logs.txt",
    var destinationConnectionType: ConnectionType = ConnectionType.TCP_IP,
    var serverConnectionType: ConnectionType = ConnectionType.TCP_IP
) {




    /**
     * Advanced options getter/setter
     */
    var advanceOptions: AdvancedOptions
        get() {
            if (advancedOptions == null) {
                advancedOptions = AdvancedOptions()
            }
            return advancedOptions!!
        }
        set(value) {
            advancedOptions = value
            if (transactionTimeOut > 0) {
                value.timeOutFromDest = transactionTimeOut
                value.timeOutFromSource = transactionTimeOut
            }
        }

    /**
     * Get/set add new client when load KEK
     */
    var addNewClientWhenLoadKek: Boolean
        get() = addNewClientWhenLoadKEK
        set(value) {
            addNewClientWhenLoadKEK = value
        }

    /**
     * Get/set bit template
     */
    var bitTemplate: Array<BitSpecific>
        get() {
            if (gwBitTemplate == null) {
                try {
                    gwBitTemplate = BitTemplate.getBINARYpecificArray("Iso8583Template.xml")
                } catch (e: Exception) {
                    gwBitTemplate = BitTemplate.getTemplate_Standard()
                }
            }
            return gwBitTemplate!!
        }
        set(value) {
            gwBitTemplate = value
        }

    /**
     * Get/set transmission type
     */
    var transmission: TransmissionType
        get() = this.transmissionType
        set(value) {
            this.transmissionType = value
        }


    /**
     * Get/set log file name
     */
    var logFileName: String
        get() = this._logFileName.replace(
            "date",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy_MM_dd"))
        )
        set(value) {
            this._logFileName = value
        }

    /**
     * Get encoding based on text encoding setting
     */
    fun getEncoding(): String {
        return when (textEncoding) {
            MessageEncoding.ASCII -> "US-ASCII"
            MessageEncoding.BigEndianUnicode -> "UTF-16BE"
            MessageEncoding.ANSI -> "windows-1252"
            MessageEncoding.Unicode -> "UTF-16LE"
            MessageEncoding.UTF32 -> "UTF-32"
            MessageEncoding.UTF7 -> "UTF-7"
            MessageEncoding.UTF8 -> "UTF-8"
        }
    }

    /**
     * Get encoding based on text encoding setting
     */
    fun getEncodingList(): List<MessageEncoding> {
        return listOf(
            MessageEncoding.ASCII,
            MessageEncoding.BigEndianUnicode,
            MessageEncoding.ANSI,
            MessageEncoding.Unicode,
            MessageEncoding.UTF32,
            MessageEncoding.UTF7,
            MessageEncoding.UTF8,
        )
    }

    /**
     * String representation of the configuration
     */
    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("Name = $name\t Gateway type = $gatewayType")
        builder.append("\r\nTransmission Type = $transmissionType\t Description: $description")
        builder.append("\r\nLocal Address = $serverAddress\t Port = $serverPort")
        builder.append("\r\nRemote Address = $destinationServer\t Port = $destinationPort")
        return builder.toString()
    }

    companion object {
        /**
         * Read configuration from file
         */
        suspend fun read(filename: String): GatewayConfig {
            val xmlBytes = Res.readBytes(filename)
            val xmlString = xmlBytes.decodeToString()
            return XML.decodeFromString<GatewayConfig>(xmlString)
        }
    }

    /**
     * Write configuration to file
     */
    fun write(filename: String) {
        try {
            File(filename).outputStream().use { fileOut ->
                ObjectOutputStream(fileOut).use { objectOut ->
                    objectOut.writeObject(this)
                }
            }
        } catch (e: Exception) {
            // Ignore write errors
        }
    }
}