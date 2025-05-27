package `in`.aicortex.iso8583studio.data.model

import `in`.aicortex.iso8583studio.data.BitSpecific
import `in`.aicortex.iso8583studio.data.BitTemplate
import `in`.aicortex.iso8583studio.domain.utils.FormatMappingConfig
import `in`.aicortex.iso8583studio.domain.utils.MtiMapping
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.Transaction
import iso8583studio.composeapp.generated.resources.Res
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XML

@Serializable
data class Something(
    val id: Int = 1,
    var str: String = "Data"
)

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
    var logOptions: Int = LoggingOption.PARSED_DATA.value,
    var serverAddress: String = "",
    var baudRate: String = "",
    var serialPort: String = "",
    var dialupNumber: String = "",
    var textEncoding: MessageEncoding = MessageEncoding.ASCII,
    var privateKey: ByteArray? = byteArrayOf(),
    var iv: ByteArray? = byteArrayOf(),
    var checkSignature: SignatureChecking = SignatureChecking.NONE,
    var description: String = "",
    var createDate: Long = System.currentTimeMillis(),
    var createBy: String = "",
    var clientID: String = "",
    var locationID: String = "",
    var password: ByteArray? = byteArrayOf(),
    var acceptClientListOnly: Boolean = false,
    var maxConcurrentConnection: Int = 100,
    var cipherMode: CipherMode = CipherMode.CBC,
    var enable: Boolean = false,
    var enableEncDecTransmission: Boolean = false,
    var autoRestartAfter: Int = 0,
    var nccRule: Boolean = true,
    var terminateWhenError: Boolean = false,
    var monitorAddress: String = "",
    var monitorPort: Int = 0,
    var waitToRestart: Int = 300,
    var hashAlgorithm: HashAlgorithm = HashAlgorithm.SHA1,
    var allowLoadKEK: Boolean = false,
    var allowWrongParsedData: Boolean = false,
    var keyExpireAfter: Int = 0,
    var addNewClientWhenLoadKEK: Boolean = false,
    var advancedOptions: AdvancedOptions? = null,
    var restConfiguration: RestConfiguration? = null,
    var codeFormatDest: CodeFormat? = null,
    var codeFormatSource: CodeFormat? = null,
    private var _logFileName: String = "logs.txt",
    var destinationConnectionType: ConnectionType = ConnectionType.TCP_IP,
    var serverConnectionType: ConnectionType = ConnectionType.TCP_IP,


    //Source
    var simulatedTransactionsToSource: List<Transaction> = emptyList(),
    var formatMappingConfigSource: FormatMappingConfig = FormatMappingConfig(
        formatType = CodeFormat.BYTE_ARRAY,
        fieldMappings = mapOf(),
        mti = MtiMapping(
            key = "msgType"
        )
    ),
    private var gwBitTemplateSource: Array<BitSpecific>? = null,
    var doNotUseHeaderSource: Boolean = false,
    var messageInAsciiSource: Boolean = false,
    var respondIfUnrecognizedSource: Boolean = false,
    var metfoneMesageSource: Boolean = false,
    var notUpdateScreenSource: Boolean = false,
    var customizeMessageSource: Boolean = false,
    var ignoreRequestHeaderSource: Int = 5,
    var fixedResponseHeaderSource: ByteArray? = null,
    var messageLengthTypeSource: MessageLengthType = MessageLengthType.BCD,

    //Destination
    var simulatedTransactionsToDest: List<Transaction> = emptyList(),
    var formatMappingConfigDest: FormatMappingConfig = FormatMappingConfig(
        formatType = CodeFormat.BYTE_ARRAY,
        fieldMappings = mapOf(),
        mti = MtiMapping(
            key = "msgType"
        )
    ),
    private var gwBitTemplateDest: Array<BitSpecific>? = null,
    var doNotUseHeaderDest: Boolean = false,
    var messageInAsciiDest: Boolean = false,
    var respondIfUnrecognizedDest: Boolean = false,
    var metfoneMesageDest: Boolean = false,
    var notUpdateScreenDest: Boolean = false,
    var customizeMessageDest: Boolean = false,
    var ignoreRequestHeaderDest: Int = 5,
    var fixedResponseHeaderDest: ByteArray? = null,
    var messageLengthTypeDest: MessageLengthType = MessageLengthType.BCD
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
    var bitTemplateSource: Array<BitSpecific>
        get() {
            if (gwBitTemplateSource == null) {
                try {
                    gwBitTemplateSource = BitTemplate.getBINARYpecificArray("Iso8583Template.xml")
                } catch (e: Exception) {
                    gwBitTemplateSource = BitTemplate.getTemplate_Standard()
                }
            }
            return gwBitTemplateSource!!
        }
        set(value) {
            gwBitTemplateSource = value
        }

    /**
     * Get/set bit template
     */
    var bitTemplateDest: Array<BitSpecific>
        get() {
            if (gwBitTemplateDest == null) {
                try {
                    gwBitTemplateDest = BitTemplate.getBINARYpecificArray("Iso8583Template.xml")
                } catch (e: Exception) {
                    gwBitTemplateDest = BitTemplate.getTemplate_Standard()
                }
            }
            return gwBitTemplateDest!!
        }
        set(value) {
            gwBitTemplateDest = value
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
        get() = this._logFileName
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


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GatewayConfig

        if (id != other.id) return false
        if (serverPort != other.serverPort) return false
        if (destinationPort != other.destinationPort) return false
        if (maxLogSizeInMB != other.maxLogSizeInMB) return false
        if (transactionTimeOut != other.transactionTimeOut) return false
        if (logOptions != other.logOptions) return false
        if (createDate != other.createDate) return false
        if (acceptClientListOnly != other.acceptClientListOnly) return false
        if (doNotUseHeaderSource != other.doNotUseHeaderSource) return false
        if (messageInAsciiSource != other.messageInAsciiSource) return false
        if (respondIfUnrecognizedSource != other.respondIfUnrecognizedSource) return false
        if (maxConcurrentConnection != other.maxConcurrentConnection) return false
        if (enable != other.enable) return false
        if (autoRestartAfter != other.autoRestartAfter) return false
        if (nccRule != other.nccRule) return false
        if (terminateWhenError != other.terminateWhenError) return false
        if (monitorPort != other.monitorPort) return false
        if (waitToRestart != other.waitToRestart) return false
        if (allowLoadKEK != other.allowLoadKEK) return false
        if (allowWrongParsedData != other.allowWrongParsedData) return false
        if (keyExpireAfter != other.keyExpireAfter) return false
        if (addNewClientWhenLoadKEK != other.addNewClientWhenLoadKEK) return false
        if (name != other.name) return false
        if (cipherType != other.cipherType) return false
        if (destinationServer != other.destinationServer) return false
        if (gatewayType != other.gatewayType) return false
        if (transmissionType != other.transmissionType) return false
        if (serverAddress != other.serverAddress) return false
        if (textEncoding != other.textEncoding) return false
        if (messageLengthTypeSource != other.messageLengthTypeSource) return false
        if (!privateKey.contentEquals(other.privateKey)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (checkSignature != other.checkSignature) return false
        if (description != other.description) return false
        if (createBy != other.createBy) return false
        if (clientID != other.clientID) return false
        if (locationID != other.locationID) return false
        if (!password.contentEquals(other.password)) return false
        if (cipherMode != other.cipherMode) return false
        if (monitorAddress != other.monitorAddress) return false
        if (hashAlgorithm != other.hashAlgorithm) return false
        if (!gwBitTemplateSource.contentEquals(other.gwBitTemplateSource)) return false
        if (advancedOptions != other.advancedOptions) return false
        if (_logFileName != other._logFileName) return false
        if (destinationConnectionType != other.destinationConnectionType) return false
        if (serverConnectionType != other.serverConnectionType) return false
        if (simulatedTransactionsToSource != other.simulatedTransactionsToSource) return false
        if (addNewClientWhenLoadKek != other.addNewClientWhenLoadKek) return false
        if (advanceOptions != other.advanceOptions) return false
        if (!bitTemplateSource.contentEquals(other.bitTemplateSource)) return false
        if (transmission != other.transmission) return false
        if (logFileName != other.logFileName) return false
        if (metfoneMesageSource != other.metfoneMesageSource) return false
        if (notUpdateScreenSource != other.notUpdateScreenSource) return false
        if (customizeMessageSource != other.customizeMessageSource) return false
        if (ignoreRequestHeaderSource != other.ignoreRequestHeaderSource) return false
        if (serialPort != other.serialPort) return false
        if (baudRate != other.baudRate) return false
        if (dialupNumber != other.dialupNumber) return false
        if (formatMappingConfigSource != other.formatMappingConfigSource) return false
        if (codeFormatSource != other.codeFormatSource) return false
        if (codeFormatDest != other.codeFormatDest) return false
        if (fixedResponseHeaderSource != other.fixedResponseHeaderSource) return false
        if (simulatedTransactionsToDest != other.simulatedTransactionsToDest) return false
        if (formatMappingConfigDest != other.formatMappingConfigDest) return false
        if (!gwBitTemplateDest.contentEquals(other.gwBitTemplateDest)) return false
        if (doNotUseHeaderDest != other.doNotUseHeaderDest) return false
        if (messageInAsciiDest != other.messageInAsciiDest) return false
        if (respondIfUnrecognizedDest != other.respondIfUnrecognizedDest) return false
        if (metfoneMesageDest != other.metfoneMesageDest) return false
        if (notUpdateScreenDest != other.notUpdateScreenDest) return false
        if (customizeMessageDest != other.customizeMessageDest) return false
        if (ignoreRequestHeaderDest != other.ignoreRequestHeaderDest) return false
        if (fixedResponseHeaderDest != other.fixedResponseHeaderDest) return false
        if (messageLengthTypeDest != other.messageLengthTypeDest) return false
        if (restConfiguration != other.restConfiguration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + serverPort
        result = 31 * result + destinationPort
        result = 31 * result + maxLogSizeInMB
        result = 31 * result + transactionTimeOut
        result = 31 * result + logOptions
        result = 31 * result + createDate.hashCode()
        result = 31 * result + acceptClientListOnly.hashCode()
        result = 31 * result + doNotUseHeaderSource.hashCode()
        result = 31 * result + messageInAsciiSource.hashCode()
        result = 31 * result + respondIfUnrecognizedSource.hashCode()
        result = 31 * result + maxConcurrentConnection
        result = 31 * result + enable.hashCode()
        result = 31 * result + autoRestartAfter
        result = 31 * result + nccRule.hashCode()
        result = 31 * result + terminateWhenError.hashCode()
        result = 31 * result + monitorPort
        result = 31 * result + waitToRestart
        result = 31 * result + allowLoadKEK.hashCode()
        result = 31 * result + allowWrongParsedData.hashCode()
        result = 31 * result + keyExpireAfter
        result = 31 * result + addNewClientWhenLoadKEK.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + cipherType.hashCode()
        result = 31 * result + destinationServer.hashCode()
        result = 31 * result + gatewayType.hashCode()
        result = 31 * result + transmissionType.hashCode()
        result = 31 * result + serverAddress.hashCode()
        result = 31 * result + textEncoding.hashCode()
        result = 31 * result + messageLengthTypeSource.hashCode()
        result = 31 * result + (privateKey?.contentHashCode() ?: 0)
        result = 31 * result + (iv?.contentHashCode() ?: 0)
        result = 31 * result + checkSignature.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + createBy.hashCode()
        result = 31 * result + clientID.hashCode()
        result = 31 * result + locationID.hashCode()
        result = 31 * result + (password?.contentHashCode() ?: 0)
        result = 31 * result + cipherMode.hashCode()
        result = 31 * result + monitorAddress.hashCode()
        result = 31 * result + hashAlgorithm.hashCode()
        result = 31 * result + (gwBitTemplateSource?.contentHashCode() ?: 0)
        result = 31 * result + (advancedOptions?.hashCode() ?: 0)
        result = 31 * result + _logFileName.hashCode()
        result = 31 * result + destinationConnectionType.hashCode()
        result = 31 * result + serverConnectionType.hashCode()
        result = 31 * result + simulatedTransactionsToSource.hashCode()
        result = 31 * result + addNewClientWhenLoadKek.hashCode()
        result = 31 * result + advanceOptions.hashCode()
        result = 31 * result + bitTemplateSource.contentHashCode()
        result = 31 * result + transmission.hashCode()
        result = 31 * result + logFileName.hashCode()
        result = 31 * result + metfoneMesageSource.hashCode()
        result = 31 * result + notUpdateScreenSource.hashCode()
        result = 31 * result + customizeMessageSource.hashCode()
        result = 31 * result + ignoreRequestHeaderSource.hashCode()
        result = 31 * result + serialPort.hashCode()
        result = 31 * result + baudRate.hashCode()
        result = 31 * result + dialupNumber.hashCode()
        result = 31 * result + formatMappingConfigSource.hashCode()
        result = 31 * result + codeFormatSource.hashCode()
        result = 31 * result + codeFormatDest.hashCode()
        result = 31 * result + fixedResponseHeaderSource.contentHashCode()
        result = 31 * result + simulatedTransactionsToDest.hashCode()
        result = 31 * result + formatMappingConfigDest.hashCode()
        result = 31 * result + (gwBitTemplateDest?.contentHashCode() ?: 0)
        result = 31 * result + doNotUseHeaderDest.hashCode()
        result = 31 * result + messageInAsciiDest.hashCode()
        result = 31 * result + respondIfUnrecognizedDest.hashCode()
        result = 31 * result + metfoneMesageDest.hashCode()
        result = 31 * result + notUpdateScreenDest.hashCode()
        result = 31 * result + customizeMessageDest.hashCode()
        result = 31 * result + ignoreRequestHeaderDest.hashCode()
        result = 31 * result + fixedResponseHeaderDest.contentHashCode()
        result = 31 * result + messageLengthTypeDest.hashCode()
        result = 31 * result + restConfiguration.hashCode()
        return result
    }
}