package `in`.aicortex.iso8583studio.data

import RestEnabledGatewayHandler
import `in`.aicortex.iso8583studio.data.model.CipherType
import `in`.aicortex.iso8583studio.data.model.ConnectionType
import `in`.aicortex.iso8583studio.data.model.EDialupStatus
import `in`.aicortex.iso8583studio.data.model.EMVShowOption
import `in`.aicortex.iso8583studio.data.model.GatewayMessageType
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.data.model.LoggingOption
import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import `in`.aicortex.iso8583studio.data.model.SignatureChecking
import `in`.aicortex.iso8583studio.data.model.SpecialFeature
import `in`.aicortex.iso8583studio.data.model.TransactionStatus
import `in`.aicortex.iso8583studio.data.model.TransmissionType
import `in`.aicortex.iso8583studio.data.model.VerificationError
import `in`.aicortex.iso8583studio.data.model.VerificationException
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.bcdToBin
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.bcdToString
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.bytesCopy
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.getBytesFromBytes
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.intToMessageLength
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.messageLengthToInt
import `in`.aicortex.iso8583studio.domain.service.SimulatedResponse
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import java.net.Socket
import java.net.SocketException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class GatewayClient {
    private var m_FirstConnection: Socket? = null
    private var m_SecondConnection: Socket? = null
    private var m_SecondRestConnection: HttpClient? = null
    var timeOut: Int = 30
    private var gatewayHandler: GatewayServiceImpl? = null
    private var _cipherType: CipherType = CipherType.DES
    var tags: Any? = null
    private var m_SslIncoming: SSLTcpClient? = null
    private var m_SslOutgoing: SSLTcpClient? = null
    private var m_RemotePort: Int = 0
    private var m_TimeCreated: LocalDateTime = LocalDateTime.now()
    private var m_LastActive: LocalDateTime = LocalDateTime.now()
    private var m_Buffer: ByteArray = ByteArray(10048)
    private var m_Buffer_Receive: ByteArray = ByteArray(10048)
    var remoteIPAddress: String? = null
    var extraData1: Any? = null
    var extraData2: Any? = null
    private var m_Request: TransmittedData? = null
    private var m_Response: TransmittedData? = null
    private var m_LastError: Exception? = null
    private var m_Index: Int = 0
    private var m_BytesReceiveFromSource: Int = 0
    private var m_BytesReceiveFromDestination: Int = 0
    private var m_TotalTransmission: Int = 0
    private var m_ClientID: String = ""
    private var m_LocationID: String = ""
    private var m_CSK: String = ""
    private var m_Status: TransactionStatus = TransactionStatus.NONE
    private var m_BeginTransactionDate: LocalDateTime = LocalDateTime.now()
    private val niiClientMapping = sortedMapOf<Int, TransmittedData>()
    private var specialIso8583Parse: Map<String, BitTemplate>? = null
    private var destinationNII: Int = 0
    private var sourceNII: Int = 0
    private var m_CancelSend: Boolean = false
    private var m_NCCProcess: NCCHandler? = null

    var onReceivedFormSource: ((Iso8583Data?) -> Unit)? = null
    var onSentToSource: ((Iso8583Data?) -> Unit)? = null
    var onReceivedFormDest: ((Iso8583Data?) -> Unit)? = null
    var onSentToDest: ((Iso8583Data?) -> Unit)? = null
    var beforeReceive: ((GatewayClient) -> Unit)? = null
    var onError: ((GatewayClient) -> Unit)? = null
    var onAdminResponse: (suspend (GatewayClient, ByteArray) -> ByteArray?)? = null

    var nccProcess: NCCHandler?
        get() = m_NCCProcess
        set(value) {
            m_NCCProcess = value
        }

    val bytesReceiveFromSource: Int
        get() = m_BytesReceiveFromSource

    val lastActive: LocalDateTime
        get() = m_LastActive

    val timeCreated: LocalDateTime
        get() = m_TimeCreated

    var sslIncoming: SSLTcpClient?
        get() = m_SslIncoming
        set(value) {
            m_SslIncoming = value
        }

    var sslOutgoing: SSLTcpClient?
        get() = m_SslOutgoing
        set(value) {
            m_SslOutgoing = value
        }

    var cancelSend: Boolean
        get() = m_CancelSend
        set(value) {
            m_CancelSend = value
        }

    val bytesRececeivedFromDestination: Int
        get() = m_BytesReceiveFromDestination

    val beginTransactionDate: LocalDateTime
        get() = m_BeginTransactionDate

    val totalTransmission: Int
        get() = m_TotalTransmission

    var status: TransactionStatus
        get() = m_Status
        set(value) {
            if (value == TransactionStatus.NONE) {
                m_Status = value
            } else if (value.ordinal - m_Status.ordinal != 1) {
                m_Status = TransactionStatus.NONE
            } else {
                m_Status = value
                when (m_Status) {
                    TransactionStatus.RECEIVED_REQUEST ->
                        gatewayHandler?.wrongFormatTrans?.set(
                            (gatewayHandler?.wrongFormatTrans?.get() ?: 0) + 1
                        )

                    TransactionStatus.GATEWAY_HEADER_UNPACKED -> {
                        gatewayHandler?.wrongFormatTrans?.set(
                            (gatewayHandler?.wrongFormatTrans?.get() ?: 0) - 1
                        )
                        gatewayHandler?.unauthorisedTrans?.set(
                            (gatewayHandler?.unauthorisedTrans?.get() ?: 0) + 1
                        )
                    }

                    TransactionStatus.AUTHENTICATED -> {
                        gatewayHandler?.unauthorisedTrans?.set(
                            (gatewayHandler?.unauthorisedTrans?.get() ?: 0) - 1
                        )
                        gatewayHandler?.unsentTrans?.set(
                            (gatewayHandler?.unsentTrans?.get() ?: 0) + 1
                        )
                    }

                    TransactionStatus.SENT_TO_DESTINATION -> {
                        gatewayHandler?.unsentTrans?.set(
                            (gatewayHandler?.unsentTrans?.get() ?: 0) - 1
                        )
                        gatewayHandler?.timeoutTrans?.set(
                            (gatewayHandler?.timeoutTrans?.get() ?: 0) + 1
                        )
                    }

                    TransactionStatus.RECEIVED_RESPONSE -> {
                        gatewayHandler?.timeoutTrans?.set(
                            (gatewayHandler?.timeoutTrans?.get() ?: 0) - 1
                        )
                        gatewayHandler?.incompleteTrans?.set(
                            (gatewayHandler?.incompleteTrans?.get() ?: 0) + 1
                        )
                    }

                    TransactionStatus.SUCCESSFUL -> {
                        gatewayHandler?.incompleteTrans?.set(
                            (gatewayHandler?.incompleteTrans?.get() ?: 0) - 1
                        )
                        gatewayHandler?.successfulTrans?.set(
                            (gatewayHandler?.successfulTrans?.get() ?: 0) + 1
                        )
                    }

                    else -> {}
                }
            }
        }

    val myGateway: GatewayServiceImpl?
        get() = gatewayHandler

    private var csk: String
        get() = m_CSK
        set(value) {
            m_CSK = value
        }

    var locationID: String
        get() = m_LocationID
        set(value) {
            m_LocationID = value
        }

    var clientID: String
        get() = m_ClientID
        set(value) {
            m_ClientID = value
        }

    val bytesReceiveFromDestination: Int
        get() = m_BytesReceiveFromDestination

    val index: Int
        get() = m_Index

    var lastError: Exception?
        get() {
            if (m_LastError is SocketException) {
                m_LastError = VerificationException(
                    m_LastError?.message ?: "",
                    VerificationError.SOCKET_ERROR
                )
            }
            return m_LastError
        }
        set(value) {
            m_LastError = value
        }

    val cipherType: CipherType
        get() = _cipherType

    constructor(gatewayServer: String, port: Int) {
        m_FirstConnection = Socket(gatewayServer, port)
    }

    constructor(server: GatewayServiceImpl) {
        gatewayHandler = server
        m_Index = server.connectionCount.get()
        _cipherType = server.configuration.cipherType

        m_NCCProcess = NCCHandler(server.configuration.advanceOptions.getNCCParameterList())

        if (gatewayHandler?.configuration?.transmission == TransmissionType.SYNCHRONOUS) {
            m_NCCProcess?.tolerateInvalidNII = true
        }

        if (m_NCCProcess?.isActive() == true) {
            gatewayHandler?.permanentConnections?.values?.forEach { connection ->
                m_NCCProcess?.get(connection.hostNii)?.pConnection = connection
            }
        }
    }

    constructor()

    companion object {
        val aboutUs: String = "Sourabh Kaushik, sk@aicortex.in"
    }

    fun doReadAsynchronous() {
        firstConnection?.getInputStream()?.let { stream ->
            // Implementation needed for asynchronous reading
        }
    }

    fun processGateway() {
        IsoCoroutine(gatewayHandler).launchSafely {
            doProcessGateway()
        }
    }

    suspend fun doProcessGateway() {
        try {
            beforeReceive?.invoke(this)

            if (gatewayHandler?.configuration?.gatewayType == GatewayType.PROXY) {
                if (m_Request == null) {
                    m_Request = TransmittedData(
                        gatewayHandler?.configuration?.messageLengthTypeSource
                            ?: MessageLengthType.BCD
                    )
                }
                if (m_Response == null) {
                    m_Response = TransmittedData(
                        gatewayHandler?.configuration?.messageLengthTypeSource
                            ?: MessageLengthType.BCD
                    )
                }
            } else {
                if (eRequestData == null) {
                    eRequestData = EncryptedTransmittedData(
                        gatewayHandler?.endeService
                            ?: throw IllegalStateException("ENDEService not initialized"),
                        gatewayHandler?.configuration?.messageLengthTypeSource
                            ?: MessageLengthType.BCD
                    ).apply {
                        clientID = this@GatewayClient.clientID
                        locationID = gatewayHandler?.configuration?.locationID ?: ""
                        messageType = GatewayMessageType.NORMAL_REQUEST
                    }
                }

                if (eResponseData == null) {
                    eResponseData = EncryptedTransmittedData(
                        gatewayHandler?.endeService
                            ?: throw IllegalStateException("ENDEService not initialized"),
                        gatewayHandler?.configuration?.messageLengthTypeSource
                            ?: MessageLengthType.BCD
                    ).apply {
                        clientID = this@GatewayClient.clientID
                        locationID = gatewayHandler?.configuration?.locationID ?: ""
                    }
                }
            }

            when (gatewayHandler?.configuration?.transmission) {
                TransmissionType.SYNCHRONOUS -> {
                    if (gatewayHandler?.configuration?.gatewayType == GatewayType.CLIENT) {
                        authenticate()
                    }
                    while (firstConnection != null) {
                        try {
                            processSynchronous()
                        } catch (ex: VerificationException) {
                            if (ex.error != VerificationError.EXCEPTION_HANDLED) {
                                throw ex
                            }
                        }
                    }
                }

                TransmissionType.ASYNCHRONOUS -> {
                    processAsynchronous()
                }

                else -> {}
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
            onError?.let {
                m_LastError = ex
                it(this)
            }
        }
    }

    suspend fun sendErrorCode(ex: Exception) {
        if (ex !is VerificationException || ex.error == VerificationError.EXCEPTION_HANDLED) {
            return
        }

        if (ex.error == VerificationError.TIMEOUT) {
            return
        }

        try {
            if (gatewayHandler?.configuration?.gatewayType != GatewayType.SERVER) {
                return
            }

            initResponse()
            eResponseData?.messageType = GatewayMessageType.ERROR_RESPONSE
            eRequestData?.error = VerificationError.OTHERS

            eResponseData?.error = ex.error

            sendFormatedData()

        } catch (e: Exception) {
            // Silently catch
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun processAsynchronous() {
        beforeReceive?.invoke(this)

        if (gatewayHandler?.configuration?.gatewayType != GatewayType.PROXY) {
            if (eRequestData == null) {
                eRequestData = EncryptedTransmittedData(
                    gatewayHandler?.endeService
                        ?: throw IllegalStateException("ENDEService not initialized"),
                    gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
                ).apply {
                    clientID = gatewayHandler?.configuration?.clientID ?: ""
                    locationID = gatewayHandler?.configuration?.locationID ?: ""
                }
            }

            if (eResponseData == null) {
                eResponseData = EncryptedTransmittedData(
                    gatewayHandler?.endeService
                        ?: throw IllegalStateException("ENDEService not initialized"),
                    gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
                ).apply {
                    clientID = gatewayHandler?.configuration?.clientID ?: ""
                    locationID = gatewayHandler?.configuration?.locationID ?: ""
                }
            }
        }

        IsoCoroutine(gatewayHandler).launchSafely {
            when (gatewayHandler?.configuration?.gatewayType) {
                GatewayType.CLIENT -> authenticate()
                else -> {}
            }
        }

        // Begin asynchronous reading
        if (gatewayHandler?.configuration?.advanceOptions?.sslServer == true) {
            // Handle SSL stream reading
        } else {
            // Handle regular socket reading
            IsoCoroutine(gatewayHandler).launchSafely {
                receiveFromSourceAsynchronous(null)
            }
        }

        while (firstConnection != null && gatewayHandler?.started?.load() == true) {
            Thread.sleep(30)
        }

        throw VerificationException("", VerificationError.DISCONNECTED_FROM_SOURCE)
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun doReadAsynchronousFoPermanentConnection(hostNII: Int) {
        val re = PermanentConnectionAsynResult()
        while (gatewayHandler?.started?.load() == true || firstConnection != null) {
            try {
                re.messageReceived = nccProcess?.get(hostNII)?.receive(30)
                receiveFromDestinationAsynchronous(re)
            } catch (ex: Exception) {
                ex.toString() // Logging would be better here
            }
        }
    }

    private suspend fun establishSecondConnection() {
        when (gatewayHandler?.configuration?.destinationConnectionType) {
            ConnectionType.TCP_IP -> {
                var needNewConnection = true

                if (nccProcess?.isActive() == true) {
                    val nccParam = nccProcess?.get(destinationNII)

                    when {
                        nccParam?.pConnection != null -> {
                            if (nccParam.sourceNii == 0) {
                                nccParam.registerSourceNii()
                            }
                            needNewConnection = false
                        }

                        nccParam?.connection == null -> {
                            writeServerLog("ESTABLISHING CONNECTION TO ${nccParam?.hostAddress} ON PORT ${nccParam?.port}")
                            nccParam?.connection = Socket(nccParam.hostAddress, nccParam.port)
                        }

                        nccParam.connection?.isConnected == false -> {
                            nccParam.connection?.close()
                            writeServerLog("RE-ESTABLISHING CONNECTION ...")
                            nccParam.connection = Socket(nccParam.hostAddress, nccParam.port)
                        }

                        else -> {
                            needNewConnection = false
                        }
                    }
                } else if (secondConnection == null) {
                    val destServer = gatewayHandler?.configuration?.destinationServer
                    val destPort = gatewayHandler?.configuration?.destinationPort

                    writeServerLog("ESTABLISHING CONNECTION TO $destServer ON PORT $destPort")
                    m_SecondConnection = Socket(destServer, destPort ?: 0)

                    if (gatewayHandler?.configuration?.advanceOptions?.sslClient == true) {
                        m_SslOutgoing = SSLTcpClient.createSSLClient(this, false)
                        if (m_SslOutgoing == null) {
                            throw VerificationException(
                                "SSL AUTHENTICATION STOPPED",
                                VerificationError.SSL_ERROR
                            )
                        }
                    }
                } else if (!(secondConnection?.isConnected ?: false)) {
                    secondConnection?.close()
                    writeServerLog("RE-ESTABLISHING CONNECTION ...")
                    m_SecondConnection = Socket(
                        gatewayHandler?.configuration?.destinationServer,
                        gatewayHandler?.configuration?.destinationPort ?: 0
                    )

                    if (gatewayHandler?.configuration?.advanceOptions?.sslClient == true) {
                        m_SslOutgoing = SSLTcpClient.createSSLClient(this, false)
                        if (m_SslOutgoing == null) {
                            throw VerificationException(
                                "SSL AUTHENTICATION STOPPED",
                                VerificationError.SSL_ERROR
                            )
                        }
                    }
                } else {
                    needNewConnection = false
                }

                if (!needNewConnection ||
                    gatewayHandler?.configuration?.transmission != TransmissionType.ASYNCHRONOUS ||
                    gatewayHandler?.configuration?.gatewayType == GatewayType.CLIENT
                ) {
                    return
                }

                // Setup asynchronous reading from second connection
                // (This would require a more complete implementation)
            }

            ConnectionType.COM -> {
                if (gatewayHandler?.secondRs232Handler != null) {
                    return
                }

                writeServerLog("ESTABLISHING CONNECTION TO ${gatewayHandler?.configuration?.destinationServer} ON PORT ${gatewayHandler?.configuration?.destinationPort}")
                gatewayHandler?.secondRs232Handler = RS232Handler(
                    gatewayHandler?.configuration?.destinationServer ?: "",
                    gatewayHandler?.configuration?.destinationPort ?: 0
                ).apply {
                    readMessageTimeOut = gatewayHandler?.configuration?.transactionTimeOut ?: 30
                    messageLengthType =
                        gatewayHandler?.configuration?.messageLengthTypeSource
                            ?: MessageLengthType.BCD
                    stxEtxRule = true
                }
            }

            ConnectionType.DIAL_UP -> {
                if (gatewayHandler?.secondRs232Handler != null) {
                    return
                }

                writeServerLog("ESTABLISHING CONNECTION TO ${gatewayHandler?.configuration?.destinationServer} ON PORT ${gatewayHandler?.configuration?.destinationPort}")
                val configParts = gatewayHandler?.configuration?.destinationServer?.split(';')

                if (configParts?.size == 2) {
                    gatewayHandler?.secondRs232Handler = DialHandler(configParts[0], 115200).apply {
                        phoneNumber = configParts[1]
                    }
                } else {
                    throw VerificationException(
                        "DESTINATION SETTING IS NOT CORRECT",
                        VerificationError.WRONG_CONFIGURATION
                    )
                }

                gatewayHandler?.secondRs232Handler?.readMessageTimeOut =
                    gatewayHandler?.configuration?.transactionTimeOut ?: 30
            }

            ConnectionType.REST -> {
                m_SecondRestConnection = RestEnabledGatewayHandler(
                    gatewayHandler?.configuration!!
                ).createRestClient()
                cancelSend = false
            }

            else -> {}
        }
    }

    private suspend fun authenticate() {
        status = TransactionStatus.RECEIVED_REQUEST
        status = TransactionStatus.GATEWAY_HEADER_UNPACKED
        status = TransactionStatus.AUTHENTICATED

        eRequestData?.messageType = GatewayMessageType.LOGON_REQUEST
        establishSecondConnection()
        sendFormatedData()
        receiveFormattedData()

        eRequestData?.messageType = GatewayMessageType.NORMAL_REQUEST

        if (gatewayHandler?.configuration?.transmission == TransmissionType.ASYNCHRONOUS) {
            establishSecondConnection()
            // Setup asynchronous reading from second connection
        }
    }

    private suspend fun checkAuthentication() {
        receiveFormattedData()
        eResponseData?.messageType = GatewayMessageType.LOGON_RESPONSE
        status = TransactionStatus.SENT_TO_DESTINATION
        status = TransactionStatus.RECEIVED_RESPONSE
        sendFormatedData()
        establishSecondConnection()
    }

    private suspend fun processSynchronous() {
        status = TransactionStatus.NONE

        when (gatewayHandler?.configuration?.gatewayType) {
            GatewayType.SERVER -> {
                if (gatewayHandler?.configuration?.enableEncDecTransmission == true) {
                    receiveFormattedData()

                    if (!cancelSend) {
                        if (eRequestData?.messageType == GatewayMessageType.NORMAL_REQUEST) {
                            establishSecondConnection()
                            send(eRequestData?.rawMessage, false)
                            val dataResult = receive(secondConnection)
                            eResponseData?.rawMessage = dataResult?.pack()
                        } else {
                            status = TransactionStatus.SENT_TO_DESTINATION
                            status = TransactionStatus.RECEIVED_RESPONSE
                        }

                        initResponse()
                        sendFormatedData()
                    }
                } else {
                    val dataRequest = receive(firstConnection)
                    m_Request?.readMessage = dataRequest?.pack()
                    val response =
                        SimulatedResponse(dataRequest, gatewayHandler!!.configuration, true)
                    if (gatewayHandler?.holdMessage == false) {
                        send(response.pack(), true)
                    } else {
                        gatewayHandler?.sendHoldMessage = {
                            send(response.pack(), true)
                            gatewayHandler?.sendHoldMessage = null
                        }
                    }
                }

            }

            GatewayType.CLIENT -> {
                establishSecondConnection()

                if (!cancelSend) {


                    when (gatewayHandler?.configuration?.destinationConnectionType) {
                        ConnectionType.TCP_IP -> {
                            send(m_Request?.readMessage, false)
                            val dataResponse = receive(secondConnection)
                            m_Response?.readMessage = dataResponse?.pack()
                        }

                        ConnectionType.COM -> {
                            send(m_Request?.readMessage, false)
                            m_Response?.readMessage = receive(gatewayHandler?.secondRs232Handler)
                        }

                        ConnectionType.REST -> {
                            val response = sendRest(m_Request?.readMessage,m_SecondRestConnection)
                            m_Response?.readMessage = response?.pack()
                        }

                        else -> {}
                    }
                }
                secondConnection?.close()
            }

            GatewayType.PROXY -> {
                when (gatewayHandler?.configuration?.serverConnectionType) {
                    ConnectionType.TCP_IP, ConnectionType.REST -> {
                        val dataRequest = receive(firstConnection)
                        m_Request?.readMessage = dataRequest?.pack()
                    }

                    ConnectionType.COM, ConnectionType.DIAL_UP -> {
                        m_Request?.readMessage = receive(gatewayHandler?.firstRs232Handler)
                    }

                    else -> {}
                }

                establishSecondConnection()

                if (!cancelSend) {
                    when (gatewayHandler?.configuration?.destinationConnectionType) {
                        ConnectionType.TCP_IP -> {
                            send(m_Request?.readMessage, false)
                            val dataResponse = receive(secondConnection)
                            m_Response?.readMessage = dataResponse?.pack()
                            send(m_Response?.readMessage, true)
                        }

                        ConnectionType.COM -> {
                            send(m_Request?.readMessage, false)
                            m_Response?.readMessage = receive(gatewayHandler?.secondRs232Handler)
                            send(m_Response?.readMessage, true)
                        }
                        ConnectionType.REST -> {
                            val response = sendRest(m_Request?.readMessage,m_SecondRestConnection)
                            m_Response?.readMessage = response?.pack()
                        }
                        else -> {}
                    }


                }
            }

            else -> {}
        }

        m_Request?.readMessage?.let { m_BytesReceiveFromSource += it.size }
        m_Response?.readMessage?.let { m_BytesReceiveFromDestination += it.size }
        m_TotalTransmission++
    }

    private suspend fun sendRest(
        data: ByteArray?,
        client: HttpClient?,

    ): Iso8583Data? {
        if (client == null || data == null) {
            throw VerificationException(
                "REST client or data is null",
                VerificationError.SOCKET_ERROR
            )
        }
        val restConfig = gatewayHandler?.configuration?.restConfiguration
            ?: throw VerificationException(
                "REST configuration is not set",
                VerificationError.WRONG_CONFIGURATION
            )

        val response = client.request(gatewayHandler!!.configuration.restConfiguration!!.url) {
            method = HttpMethod.parse(restConfig.method.name)

            // Set content type based on message format
            contentType(
                when (restConfig.messageFormat) {
                    RestMessageFormat.JSON -> ContentType.Application.Json
                    RestMessageFormat.XML -> ContentType.Application.Xml
                    RestMessageFormat.HEX, RestMessageFormat.BASE64 -> ContentType.Text.Plain
                    else -> ContentType.Application.OctetStream
                }
            )
            val body = IsoUtil.hexToAscii(data)
            writeServerLog("=========================SENT TO REST SERVER==========================")
            writeServerLog("URL:${gatewayHandler?.configuration?.restConfiguration?.url ?: "N/A"}")
            writeServerLog(body)
            // Add request body
            setBody(body)
            writeServerLog("=========================HEADER INFORMATION==========================")
            // Add correlation ID for tracking
            gatewayHandler!!.configuration.restConfiguration!!.headers.forEach { key, value ->
                writeServerLog("${key.uppercase()}: $value")
                header(key, value)
            }
        }

        if (response.status.isSuccess() == true) {
            val responseBody = response.body<ByteArray>()
            val iso8583Data = parseData(responseBody, false)
            writeServerLog("REST MESSAGE PROCESSED SUCCESSFULLY")
            return iso8583Data
        } else {
            writeServerLog("REST MESSAGE PROCESSING FAILED: ${response.status}")
            throw VerificationException(
                "REST message processing failed: ${response.status}",
                VerificationError.RELEASE_CONNECTION
            )
        }
    }

    private fun initResponse() {
        if (gatewayHandler?.configuration?.nccRule == true && niiClientMapping.containsKey(sourceNII)) {
            val arDest = byteArrayOf(0x60.toByte(), 0, 0, 0, 0)

            niiClientMapping[sourceNII]?.fixedHeader?.let { header ->
                bytesCopy(arDest, header, 1, 3, 2)
                bytesCopy(arDest, header, 3, 1, 2)
            }

            eResponseData?.fixedHeader = arDest
        }

        when (eRequestData?.messageType) {
            GatewayMessageType.NORMAL_REQUEST -> {
                eResponseData?.messageType = GatewayMessageType.NORMAL_RESPONSE
            }

            GatewayMessageType.LOGON_REQUEST -> {
                eResponseData?.messageType = GatewayMessageType.LOGON_RESPONSE
            }

            GatewayMessageType.GET_KEK_REQUEST -> {
                eResponseData?.messageType = GatewayMessageType.GET_KEK_RESPONSE
            }

            else -> {}
        }

        eResponseData?.clientID = eRequestData?.clientID ?: ""
        eResponseData?.locationID = eRequestData?.locationID ?: ""
    }

    suspend fun send(input: ByteArray?, isFirst: Boolean) {
        input ?: return



        if (isFirst) {
            writeServerLog("====================SENT TO SOURCE======================")
            val requestIso = parseData(input, false)
            val modifiedISO = convertToAnother(requestIso)
            var modifiedInput = modifiedISO?.pack()
            writeServerLog(modifiedISO?.logFormat() ?: "")
            if (nccProcess?.isActive() == true) {
                intToMessageLength(
                    modifiedInput!!.size - 2,
                    gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
                ).copyInto(modifiedInput, 0)
            }

            when (gatewayHandler?.configuration?.serverConnectionType) {
                ConnectionType.TCP_IP -> {
                    firstConnection?.getOutputStream()?.write(modifiedInput, 0, modifiedInput!!.size)
                    firstConnection?.getOutputStream()?.flush()
                }

                ConnectionType.REST -> {
                    val httpHeader =  """HTTP/1.1 200 OK${"\r\n"}Content-Type: application/json${"\r\n"}Content-Length: ${modifiedInput!!.size}${"\r\n"}Connection: close${"\r\n"}${"\r\n"}"""
                    writeServerLog(httpHeader)
                    val headerByteArray = httpHeader.toByteArray(Charsets.UTF_8)
                    firstConnection?.getOutputStream()
                        ?.write(headerByteArray, 0, headerByteArray.size)
                    firstConnection?.getOutputStream()?.write(modifiedInput, 0, modifiedInput.size)
                    firstConnection?.getOutputStream()?.flush()
                }

                ConnectionType.COM -> {
                    gatewayHandler?.firstRs232Handler?.sendMessage(
                        modifiedInput!!,
                        MessageLengthType.NONE
                    )
                }

                else -> {}
            }
            m_BytesReceiveFromDestination += modifiedInput!!.size
            gatewayHandler?.bytesOutgoing?.set(
                (gatewayHandler?.bytesOutgoing?.get() ?: 0) + modifiedInput.size
            )

            writeServerLog("SENT BACK TO SOURCE ${input.size} BYTES")
            status = TransactionStatus.SUCCESSFUL
            m_TotalTransmission++
            onSentToSource?.invoke(requestIso)
        } else {
            writeServerLog("====================SENT TO DESTINATION======================")
            val requestIso = parseData(input, true)
            val modifiedISO = convertToAnother(requestIso)
            var modifiedInput = modifiedISO?.pack()
            writeServerLog(modifiedISO?.logFormat() ?: "")
            if (nccProcess?.isActive() == true) {
                val lengthType = nccProcess?.get(destinationNII)?.lengthType

                if (lengthType == MessageLengthType.NONE) {
                    val arDest = ByteArray(input.size - 2)
                    bytesCopy(arDest, input, 0, 2, arDest.size)
                    modifiedInput = arDest
                } else {
                    intToMessageLength(
                        input.size - 2,
                        lengthType ?: MessageLengthType.BCD
                    ).copyInto(input, 0)
                }
            }

            when (gatewayHandler?.configuration?.destinationConnectionType) {
                ConnectionType.TCP_IP -> {
                    if (doesConnectToPermanentConnection()) {
                        nccProcess?.get(destinationNII)?.send(modifiedInput!!)
                    } else {
                        secondConnection?.getOutputStream()
                            ?.write(modifiedInput, 0, modifiedInput!!.size)
                    }
                }

                ConnectionType.COM -> {
                    val rs232Handler = gatewayHandler?.secondRs232Handler
                    rs232Handler?.let {
                        if (!it.waitPendingTrans(
                                gatewayHandler?.configuration?.transactionTimeOut ?: 30
                            )
                        ) {
                            throw VerificationException(
                                " THE PENDING TRANSACTION IS NOT FINISHED YET",
                                VerificationError.TIMEOUT
                            )
                        }
                        it.sendMessage(modifiedInput!!, MessageLengthType.NONE)
                    }
                }

                else -> {}
            }

            writeServerLog("SENT TO DESTINATION ${modifiedInput!!.size} BYTES")
            status = TransactionStatus.SENT_TO_DESTINATION
            onSentToDest?.invoke(modifiedISO)
        }


    }

    fun isTIDialerRule(): Boolean {
        val destServer = gatewayHandler?.configuration?.destinationServer ?: ""
        return destServer.length >= 6 && destServer.substring(4, 6) == "TI"
    }

    fun receive(client: RS232Handler?): ByteArray? {
        client ?: return null

        // Handle dialup connections
        if (client == gatewayHandler?.firstRs232Handler &&
            gatewayHandler?.configuration?.serverConnectionType == ConnectionType.DIAL_UP
        ) {
            val dialHandler = client as? DialHandler
            if (dialHandler?.currentStatus != EDialupStatus.Connected) {
                while (dialHandler?.receiveCall() == true) {
                    Thread.sleep(10)
                }
            }
        }

        if (client == gatewayHandler?.secondRs232Handler &&
            gatewayHandler?.configuration?.serverConnectionType == ConnectionType.DIAL_UP
        ) {
            val dialHandler = client as? DialHandler
            if (dialHandler?.currentStatus != EDialupStatus.Connected &&
                dialHandler?.makeCall() != true
            ) {
                writeServerLog("CANNOT MAKE A CALL TO ${dialHandler?.phoneNumber}")
            }
        }

        var message = client.receiveMessage()

        if (client == gatewayHandler?.firstRs232Handler) {
            status = TransactionStatus.RECEIVED_REQUEST
            status = TransactionStatus.GATEWAY_HEADER_UNPACKED
            status = TransactionStatus.AUTHENTICATED
        } else {
            status = TransactionStatus.RECEIVED_RESPONSE
        }

        while (message == null && client == gatewayHandler?.firstRs232Handler) {
            message = client.receiveMessage()
        }

        if (message == null) {
            throw VerificationException(
                "CAN'T RECEIVED DATA FROM COMPORT",
                VerificationError.TIMEOUT
            )
        }

        writeServerLog("RECEIVED MESSAGE FROM COMPORT${message.size} BYTES ")
        parseData(message, client == gatewayHandler?.firstRs232Handler)

        destinationNII = bcdToBin(getBytesFromBytes(message, 3, 2))

        return message
    }

    fun checkConnectionAlive(client: Socket?): Boolean {
        client ?: return false

        var wasBlocking = false
        var oldTimeout = 30000

        try {
            val buffer = ByteArray(2)
            wasBlocking = client.isInputShutdown // Not a perfect equivalent to Blocking
            oldTimeout = client.soTimeout

            client.soTimeout = 200
            return client.getInputStream().read(buffer, 0, 1) != 0

        } catch (ex: Exception) {
            // Ignore exception
        } finally {
            try {
                client.soTimeout = oldTimeout
            } catch (ex: Exception) {
                // Ignore exception
            }
        }

        return true
    }

    suspend fun receive(client: SSLTcpClient?): ByteArray? {
        client ?: return null

        val now = LocalDateTime.now()

        if (client == sslIncoming) {
            client.sslStream?.readTimeout =
                gatewayHandler?.configuration?.advanceOptions?.timeOutFromSource?.times(1000)
                    ?: 30000
        } else {
            client.sslStream?.readTimeout =
                gatewayHandler?.configuration?.advanceOptions?.timeOutFromDest?.times(1000) ?: 30000
        }

        val length = client.sslStream?.read(m_Buffer, 0, m_Buffer.size - 10) ?: 0

        if (ChronoUnit.SECONDS.between(now, LocalDateTime.now()) >=
            (if (client == sslIncoming)
                gatewayHandler?.configuration?.advanceOptions?.timeOutFromSource?.toLong() ?: 30L
            else
                gatewayHandler?.configuration?.advanceOptions?.timeOutFromDest?.toLong() ?: 30L)
        ) {

            if (client == sslIncoming) {
                throw VerificationException("NO REQUEST RECEIVED ", VerificationError.TIMEOUT)
            } else {
                throw VerificationException("NO RESPONSE ", VerificationError.TIMEOUT)
            }
        }

        if (length < 2) {
            throw VerificationException(
                "CONNECTION MAY BE CLOSED BY REMOTE COMPUTER/TERMINAL ",
                if (client == sslIncoming)
                    VerificationError.DISCONNECTED_FROM_SOURCE
                else
                    VerificationError.DISCONNECTED_FROM_SOURCE
            )
        }

        val input = ByteArray(length)
        bytesCopy(input, m_Buffer, 0, 0, length - 10)

        if (client == sslIncoming) {
            onReceivedFormSource?.invoke(parseData(input, true))
        } else {
            onReceivedFormSource?.invoke(parseData(input, false))
        }

        if (client == sslIncoming) {
            status = TransactionStatus.RECEIVED_REQUEST
            status = TransactionStatus.GATEWAY_HEADER_UNPACKED
            status = TransactionStatus.AUTHENTICATED
            destinationNII = bcdToBin(getBytesFromBytes(input, 3, 2))
        } else {
            status = TransactionStatus.RECEIVED_RESPONSE
        }

        writeServerLog(
            if (client == sslIncoming)
                "RECEIVED MESSAGE FROM SOURCE "
            else
                "RECEIVED MESSAGE FROM DESTINATION ${input.size} BYTES ",

            )

        parseData(input, client == sslIncoming)

        if (client == sslIncoming) {
            m_BytesReceiveFromSource += input.size
            gatewayHandler?.bytesIncoming?.set(
                (gatewayHandler?.bytesIncoming?.get() ?: 0) + input.size
            )
        } else {
            m_BytesReceiveFromDestination += input.size
            gatewayHandler?.bytesOutgoing?.set(
                (gatewayHandler?.bytesOutgoing?.get() ?: 0) + input.size
            )
        }

        return input
    }

    suspend fun receive(client: Socket?): Iso8583Data? {
        client ?: return null

        val now = LocalDateTime.now()
        val timeOut = this.timeOut

        writeServerLog("RECEIVING MESSAGE.......")

        val input: ByteArray =
            if (doesConnectToPermanentConnection() && client == secondConnection) {
                nccProcess?.get(destinationNII)?.receive(timeOut) ?: ByteArray(0)
            } else {
                var length = 0

                if (client.isConnected) {
                    val timeout = if (client == firstConnection)
                        gatewayHandler?.configuration?.advanceOptions?.timeOutFromSource?.times(1000)
                            ?: 30000
                    else
                        gatewayHandler?.configuration?.advanceOptions?.timeOutFromDest?.times(1000)
                            ?: 30000

                    client.soTimeout = timeout
                }

                try {
                    val buffer = ByteArray(m_Buffer.size)
                    var bytesRead: Int

                    do {
                        if (length == buffer.size) {
                            throw VerificationException(
                                "Length exceeds buffer size",
                                VerificationError.OTHERS
                            )
                        }

                        bytesRead =
                            client.getInputStream().read(buffer, length, buffer.size - length)

                        if (bytesRead == 0) {
                            throw VerificationException(
                                "CONNECTION WAS CLOSED BY REMOTE COMPUTER/TERMINAL",
                                if (client == firstConnection)
                                    VerificationError.DISCONNECTED_FROM_SOURCE
                                else
                                    VerificationError.DISCONNECTED_FROM_DESTINATION
                            )
                        }

                        length += bytesRead
                    } while (client.getInputStream().available() > 0)

                    val result = ByteArray(length)
                    buffer.copyInto(result, 0, 0, length)
                    result

                } catch (ex: Exception) {
                    if (client == firstConnection &&
                        ChronoUnit.SECONDS.between(now, LocalDateTime.now()) >=
                        (gatewayHandler?.configuration?.advanceOptions?.timeOutFromSource?.toLong()
                            ?: 30L)
                    ) {
                        throw VerificationException(
                            "NO REQUEST RECEIVED ",
                            VerificationError.TIMEOUT
                        )
                    }

                    if (client == secondConnection &&
                        ChronoUnit.SECONDS.between(now, LocalDateTime.now()) >=
                        (gatewayHandler?.configuration?.advanceOptions?.timeOutFromDest?.toLong()
                            ?: 30L)
                    ) {
                        throw VerificationException("NO RESPONSE ", VerificationError.TIMEOUT)
                    }

                    throw VerificationException(
                        "CONNECTION MAY BE CLOSED BY REMOTE COMPUTER/TERMINAL ",
                        if (client == firstConnection)
                            VerificationError.DISCONNECTED_FROM_SOURCE
                        else
                            VerificationError.DISCONNECTED_FROM_DESTINATION
                    )
                }
            }
        if (client == firstConnection) {
            onReceivedFormSource?.invoke(parseData(input, true))
        } else {
            onReceivedFormDest?.invoke(parseData(input, false))
        }

        if (client == firstConnection) {
            status = TransactionStatus.RECEIVED_REQUEST
            status = TransactionStatus.GATEWAY_HEADER_UNPACKED
            status = TransactionStatus.AUTHENTICATED

            // Handle special features for proxy
            if (gatewayHandler?.configuration?.advanceOptions?.specialFeature == SpecialFeature.SimpleEncryptionForProxy) {
                val encrypted = gatewayHandler?.simpleEncryptionForProxy?.encrypt(input)
                    ?: input
                val result = ByteArray(encrypted.size + 2)

                intToMessageLength(
                    encrypted.size,
                    gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
                ).copyInto(result, 0)

                bytesCopy(result, encrypted, 2, 0, encrypted.size)
                val iso = parseData(result, client == firstConnection)
                return iso
            } else if (gatewayHandler?.configuration?.advanceOptions?.specialFeature == SpecialFeature.SimpleDecryptionForProxy) {
                val decrypted = gatewayHandler?.simpleEncryptionForProxy?.decrypt(
                    input,
                    0,
                    input.size
                ) ?: input
                val msgLength = messageLengthToInt(
                    decrypted,
                    gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
                )

                val result = ByteArray(msgLength + 2)
                intToMessageLength(
                    result.size - 2,
                    gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
                ).copyInto(result, 0)

                bytesCopy(result, decrypted, 2, 2, result.size - 2)
                val iso = parseData(result, client == firstConnection)
                return iso
            } else {
                destinationNII = bcdToBin(getBytesFromBytes(input, 3, 2))
            }
        } else {
            status = TransactionStatus.RECEIVED_RESPONSE

            // Handle special features for proxy
            if (gatewayHandler?.configuration?.advanceOptions?.specialFeature == SpecialFeature.SimpleEncryptionForProxy) {
                val decrypted = gatewayHandler?.simpleEncryptionForProxy?.decrypt(
                    input,
                    0,
                    input.size
                ) ?: input
                val msgLength = messageLengthToInt(
                    decrypted,
                    gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
                )

                val result = ByteArray(msgLength + 2)
                intToMessageLength(
                    result.size - 2,
                    gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
                ).copyInto(result, 0)

                bytesCopy(result, decrypted, 2, 2, result.size - 2)
                val iso = parseData(result, client == firstConnection)
                return iso
            } else if (gatewayHandler?.configuration?.advanceOptions?.specialFeature == SpecialFeature.SimpleDecryptionForProxy) {
                val encrypted = gatewayHandler?.simpleEncryptionForProxy?.encrypt(input)
                    ?: input
                val result = ByteArray(encrypted.size + 2)

                intToMessageLength(
                    encrypted.size,
                    gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
                ).copyInto(result, 0)

                bytesCopy(result, encrypted, 2, 0, encrypted.size)
                val iso = parseData(result, client == firstConnection)
                return iso
            }
        }

        writeServerLog(
            if (client == firstConnection)
                "RECEIVED MESSAGE FROM SOURCE "
            else
                "RECEIVED MESSAGE FROM DESTINATION ${input.size} BYTES ",
        )

        val formattedData = parseData(input, client == firstConnection)
        writeServerLog(formattedData?.logFormat() ?: "Unable to Decrypt data ")
        onReceivedFormDest?.invoke(formattedData)

        if (client == firstConnection) {
            m_BytesReceiveFromSource += input.size
            gatewayHandler?.bytesIncoming?.set(
                (gatewayHandler?.bytesIncoming?.get() ?: 0) + input.size
            )
        } else {
            m_BytesReceiveFromDestination += input.size
            gatewayHandler?.bytesOutgoing?.set(
                (gatewayHandler?.bytesOutgoing?.get() ?: 0) + input.size
            )
        }

        return formattedData
    }

    fun parseData(input: ByteArray?, isFirst: Boolean): Iso8583Data? {
        input ?: return null
        if (input.isEmpty()) {
            return null
        }

        if (gatewayHandler?.configuration?.advanceOptions?.isPartialEncryption() == true) {
            return null
        }

        try {
            if ((gatewayHandler?.configuration?.logOptions
                    ?: LoggingOption.NONE.value) and LoggingOption.PARSED_DATA.value <= LoggingOption.NONE.value
            ) {
                return null
            }

            val iso8583Data = gatewayHandler?.configuration?.bitTemplateSource?.let {
                Iso8583Data(
                    it,
                    gatewayHandler?.configuration!!,
                    isFirst
                )
            }


            iso8583Data?.emvShowOptions =
                gatewayHandler?.configuration?.advanceOptions?.getEMVShowOption()
                    ?: EMVShowOption.None
            iso8583Data?.unpack(input)



            return iso8583Data
        } catch (ex: Exception) {
            writeServerLog("ERROR WHEN PARSING DATA \r\n${ex}")

            if (gatewayHandler?.configuration?.allowWrongParsedData != true) {
                throw VerificationException(
                    "NOT ACCEPT WRONG PARSED DATA",
                    VerificationError.DECLINED
                )
            }
        }
        return null
    }

    fun writeServerLog(s: String) {
        gatewayHandler?.writeLog(s)
    }

    private fun processServerGatewayAsynchronous(receivedData: ByteArray) {
        IsoCoroutine(gatewayHandler).launchSafely {
            if (gatewayHandler?.configuration?.nccRule == true) {
                sourceNII =
                    (receivedData[3].toInt() and 0xFF) * 256 + (receivedData[4].toInt() and 0xFF)

                if (!niiClientMapping.containsKey(sourceNII)) {
                    val newData = EncryptedTransmittedData(
                        gatewayHandler?.endeService
                            ?: throw IllegalStateException("ENDEService not initialized"),
                        gatewayHandler?.configuration?.messageLengthTypeSource
                            ?: MessageLengthType.BCD
                    )
                    niiClientMapping[sourceNII] = newData
                }

                eRequestData?.fixedHeader = getBytesFromBytes(receivedData, 0, 5)
            }

            eRequestData?.rawMessage = null

            val count = messageLengthToInt(
                getBytesFromBytes(m_Buffer, 0, 2),
                gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
            )

            if (receivedData.size < count) {
                writeServerLog("CONTINUE READING")

                firstConnection?.soTimeout = 1000
                val inputStream = firstConnection?.getInputStream()

                if (inputStream?.read(
                        m_Buffer,
                        2 + receivedData.size,
                        count - receivedData.size
                    ) == count - receivedData.size
                ) {
                    val completeData = ByteArray(count)
                    bytesCopy(completeData, m_Buffer, 0, 2, count)
                    eRequestData?.readMessage = completeData
                } else {
                    eRequestData?.readMessage = receivedData
                }
            } else {
                eRequestData?.readMessage = receivedData
            }


            if (eRequestData?.messageType == GatewayMessageType.NORMAL_REQUEST) {
                if (gatewayHandler?.configuration?.nccRule == true) {
                    eRequestData?.rawMessage?.let { rawMsg ->
                        bytesCopy(rawMsg, receivedData, 0, 2, 5)
                    }
                }


                establishSecondConnection()

                if (nccProcess?.isActive() == true) {
                    eRequestData?.rawMessage?.let { rawMsg ->
                        intToMessageLength(
                            rawMsg.size - 2,
                            nccProcess?.get(destinationNII)?.lengthType ?: MessageLengthType.BCD
                        ).copyInto(rawMsg, 0)

                        intToMessageLength(
                            nccProcess?.get(destinationNII)?.niiChanged ?: 0,
                            MessageLengthType.BCD
                        ).copyInto(rawMsg, 3)
                    }
                }

                if (doesConnectToPermanentConnection()) {
                    launch {
                        val connection = nccProcess?.get(destinationNII)?.pConnection
                        val sourceNii = connection?.registerSourceNii() ?: 0

                        nccProcess?.get(destinationNII)?.sourceNii = sourceNii
                        eRequestData?.rawMessage?.let { nccProcess?.get(destinationNII)?.send(it) }

                        doCheckMessageReceiveFromPConnection(
                            arrayOf(destinationNII, sourceNii),
                            false
                        )
                    }
                } else {
                    secondConnection?.getOutputStream()?.write(
                        eRequestData?.rawMessage ?: ByteArray(0),
                        0,
                        eRequestData?.rawMessage?.size ?: 0
                    )
                }
            } else {
                initResponse()
                sendFormatedData()
            }
        }
    }

    private suspend fun doCheckMessageReceiveFromPConnection(obj: Any?, isFirst: Boolean) {
        try {
            val params = obj as? Array<*> ?: return
            val hostNii = params[0] as? Int ?: return
            val sourceNii = params[1] as? Int ?: return

            val response = nccProcess?.get(hostNii)?.pConnection?.receive(
                sourceNii,
                gatewayHandler?.configuration?.transactionTimeOut ?: 30
            ) ?: return

            eResponseData?.rawMessage = response

            writeServerLog("RECEIVED MESSAGE FROM DESTINATION ${response.size} BYTES ")
            parseData(response, isFirst)

            initResponse()
            sendFormatedData()

            m_Request?.readMessage?.let { m_BytesReceiveFromSource += it.size }
            m_BytesReceiveFromDestination += response.size
            m_TotalTransmission++

        } catch (ex: Exception) {
            writeServerLog(ex.toString())
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun receiveFromSourceAsynchronous(re: Any?) {
        var count = 0

        try {
            count = when {
                re is AsyncResult && re.result is ByteArray -> re.result.size
                gatewayHandler?.configuration?.advanceOptions?.sslServer == false ->
                    firstConnection?.getInputStream()?.read(m_Buffer) ?: 0

                else ->
                    sslIncoming?.sslStream?.read(m_Buffer) ?: 0
            }

            if (count > 0) {
                if (nccProcess?.isActive() == true) {
                    destinationNII = bcdToBin(getBytesFromBytes(m_Buffer, 3, 2))
                }

                val input = if (gatewayHandler?.configuration?.gatewayType == GatewayType.SERVER) {
                    val data = ByteArray(count - 2)
                    bytesCopy(data, m_Buffer, 0, 2, count - 2)
                    data
                } else {
                    val data = ByteArray(count)
                    bytesCopy(data, m_Buffer, 0, 0, count)
                    data
                }

                when (gatewayHandler?.configuration?.gatewayType) {
                    GatewayType.SERVER -> {
                        processServerGatewayAsynchronous(input)
                    }

                    GatewayType.CLIENT -> {
                        eRequestData?.rawMessage = input
                        sendFormatedData()
                        establishSecondConnection()
                        secondConnection?.getOutputStream()?.flush()
                    }

                    GatewayType.PROXY -> {
                        // Handle encryption for proxy
                        val processedInput =
                            when (gatewayHandler?.configuration?.advanceOptions?.specialFeature) {
                                SpecialFeature.SimpleEncryptionForProxy -> {
                                    val encrypted =
                                        gatewayHandler?.simpleEncryptionForProxy?.encrypt(input)
                                            ?: input
                                    val result = ByteArray(encrypted.size + 2)

                                    intToMessageLength(
                                        encrypted.size,
                                        gatewayHandler?.configuration?.messageLengthTypeSource
                                            ?: MessageLengthType.BCD
                                    ).copyInto(result, 0)

                                    bytesCopy(result, encrypted, 2, 0, encrypted.size)
                                    result
                                }

                                SpecialFeature.SimpleDecryptionForProxy -> {
                                    val decrypted =
                                        gatewayHandler?.simpleEncryptionForProxy?.decrypt(
                                            input,
                                            2,
                                            input.size - 2
                                        ) ?: input

                                    intToMessageLength(
                                        messageLengthToInt(
                                            decrypted,
                                            gatewayHandler?.configuration?.messageLengthTypeSource
                                                ?: MessageLengthType.BCD
                                        ) + 5,
                                        gatewayHandler?.configuration?.messageLengthTypeSource
                                            ?: MessageLengthType.BCD
                                    ).copyInto(decrypted, 0)

                                    decrypted
                                }

                                else -> input
                            }
                        onReceivedFormSource?.invoke(parseData(input, true))

                        if (!cancelSend) {
                            if (nccProcess?.isActive() == true) {
                                destinationNII = bcdToBin(getBytesFromBytes(input, 3, 2))
                            }

                            establishSecondConnection()

                            if (nccProcess?.isActive() == true) {
                                val nccParam = nccProcess?.get(destinationNII)

                                if (nccParam?.lengthType != gatewayHandler?.configuration?.messageLengthTypeSource) {
                                    intToMessageLength(
                                        input.size - 2,
                                        nccParam?.lengthType ?: MessageLengthType.BCD
                                    ).copyInto(input, 0)
                                }

                                intToMessageLength(
                                    nccParam?.niiChanged ?: 0,
                                    MessageLengthType.BCD
                                ).copyInto(input, 3)
                            }

                            when {
                                gatewayHandler?.configuration?.advanceOptions?.sslClient == true -> {
                                    sslOutgoing?.sslStream?.write(input, 0, input.size)
                                }

                                doesConnectToPermanentConnection() -> {
                                    nccProcess?.get(destinationNII)?.send(input)
                                }

                                else -> {
                                    secondConnection?.getOutputStream()
                                        ?.write(input, 0, input.size)
                                }
                            }
                        }

                        if (!cancelSend) {
                            parseData(input, true)
                        }
                    }

                    else -> {}
                }

                m_BytesReceiveFromSource += input.size

                if (!cancelSend && gatewayHandler?.configuration?.gatewayType == GatewayType.PROXY) {
                    writeServerLog(
                        "RECEIVED FROM SOURCE AND SENT TO DESTINATION ${input.size} BYTES",
                    )
                }

                // Setup next async read
                if (gatewayHandler?.configuration?.advanceOptions?.sslServer == true) {
                    // Handle SSL async read
                } else {
                    // Handle regular async read
                }

            } else {
                firstConnection?.close()
                m_FirstConnection = null
                throw VerificationException(
                    "CAN NOT READ FROM SOURCE",
                    VerificationError.DISCONNECTED_FROM_SOURCE
                )
            }

        } catch (ex: Exception) {
            if (count > 0 && gatewayHandler?.started?.load() == true) {
                if (gatewayHandler?.configuration?.terminateWhenError == false) {
                    try {
                        writeServerLog(ex.message ?: "")
                        writeServerLog(bcdToString(m_Buffer))

                        if (gatewayHandler?.configuration?.gatewayType == GatewayType.SERVER) {
                            eRequestData?.rawMessage = null
                            eResponseData?.rawMessage = null
                        }

                        sendErrorCode(ex)

                        // Setup next async read
                        if (gatewayHandler?.configuration?.advanceOptions?.sslServer == true) {
                            // Handle SSL async read
                            return
                        }

                        // Handle regular async read
                        return

                    } catch (ex2: Exception) {
                        // Ignore
                    }
                }
            }

            onError?.let {
                m_LastError = ex
                if (firstConnection != null) {
                    it(this)
                }
            }
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun receiveFromDestinationAsynchronous(re: Any?) {
        var count = 0
        val connectionAsynResult = re as? PermanentConnectionAsynResult

        try {
            count = when {
                connectionAsynResult != null -> connectionAsynResult.messageReceived?.size ?: 0
                !(gatewayHandler?.configuration?.advanceOptions?.sslClient ?: false) ->
                    secondConnection?.getInputStream()?.read(m_Buffer_Receive) ?: 0

                else ->
                    sslOutgoing?.sslStream?.read(m_Buffer_Receive) ?: 0
            }

            if (count > 0) {
                val input = when {
                    connectionAsynResult != null -> connectionAsynResult.messageReceived
                        ?: ByteArray(0)

                    gatewayHandler?.configuration?.gatewayType == GatewayType.CLIENT -> {
                        val data = ByteArray(count - 2)
                        bytesCopy(data, m_Buffer_Receive, 0, 2, count - 2)
                        data
                    }

                    else -> {
                        val data = ByteArray(count)
                        bytesCopy(data, m_Buffer_Receive, 0, 0, count)
                        data
                    }
                }

                m_BytesReceiveFromDestination += input.size
                writeServerLog(
                    "RECEIVED FROM DESTINATION AND SENT TO SOURCE ${input.size} BYTES",
                )
                parseData(input, false)

                when (gatewayHandler?.configuration?.gatewayType) {
                    GatewayType.SERVER -> {
                        IsoCoroutine(gatewayHandler).launchSafely {
                            eResponseData?.rawMessage = input

                            if (gatewayHandler?.configuration?.nccRule == true) {
                                sourceNII =
                                    (input[3].toInt() and 0xFF) * 256 + (input[4].toInt() and 0xFF)
                            }

                            initResponse()
                            sendFormatedData()
                            secondConnection?.getOutputStream()?.flush()
                        }
                    }

                    GatewayType.CLIENT -> {
                        eResponseData?.readMessage = input

                        eResponseData?.rawMessage?.let { rawMessage ->
                            firstConnection?.getOutputStream()
                                ?.write(rawMessage, 0, rawMessage.size)
                        }
                    }

                    GatewayType.PROXY -> {
                        // Handle encryption for proxy
                        val processedInput =
                            when (gatewayHandler?.configuration?.advanceOptions?.specialFeature) {
                                SpecialFeature.SimpleEncryptionForProxy -> {
                                    val decrypted =
                                        gatewayHandler?.simpleEncryptionForProxy?.decrypt(
                                            input,
                                            2,
                                            input.size - 2
                                        ) ?: input

                                    intToMessageLength(
                                        messageLengthToInt(
                                            decrypted,
                                            gatewayHandler?.configuration?.messageLengthTypeSource
                                                ?: MessageLengthType.BCD
                                        ) + 5,
                                        gatewayHandler?.configuration?.messageLengthTypeSource
                                            ?: MessageLengthType.BCD
                                    ).copyInto(decrypted, 0)

                                    decrypted
                                }

                                SpecialFeature.SimpleDecryptionForProxy -> {
                                    val encrypted =
                                        gatewayHandler?.simpleEncryptionForProxy?.encrypt(input)
                                            ?: input
                                    val result = ByteArray(encrypted.size + 2)

                                    intToMessageLength(
                                        encrypted.size,
                                        gatewayHandler?.configuration?.messageLengthTypeSource
                                            ?: MessageLengthType.BCD
                                    ).copyInto(result, 0)

                                    bytesCopy(result, encrypted, 2, 0, encrypted.size)
                                    result
                                }

                                else -> input
                            }

                        if (gatewayHandler?.configuration?.advanceOptions?.specialFeature == SpecialFeature.SimpleEncryptionForProxy) {
                            firstConnection?.getOutputStream()?.write(
                                processedInput,
                                0,
                                messageLengthToInt(
                                    processedInput,
                                    gatewayHandler?.configuration?.messageLengthTypeSource
                                        ?: MessageLengthType.BCD
                                ) + 2
                            )
                        } else {
                            IsoCoroutine(gatewayHandler).launchSafely {
                                onReceivedFormDest?.invoke(parseData(input, true))
                                if (gatewayHandler?.configuration?.nccRule == true) {
                                    intToMessageLength(
                                        input.size - 2,
                                        gatewayHandler?.configuration?.messageLengthTypeSource
                                            ?: MessageLengthType.BCD
                                    ).copyInto(input, 0)
                                }

                                if (gatewayHandler?.configuration?.advanceOptions?.sslServer == true) {
                                    sslIncoming?.sslStream?.write(
                                        input,
                                        0,
                                        input.size
                                    )
                                } else {
                                    firstConnection?.getOutputStream()
                                        ?.write(input, 0, input.size)
                                }
                            }
                        }
                    }

                    else -> {}
                }

                m_BytesReceiveFromDestination += input.size
                m_TotalTransmission++

                // Setup next asynchronous read
                when {
                    gatewayHandler?.configuration?.advanceOptions?.sslClient == true -> {
                        // Handle SSL async read
                    }

                    connectionAsynResult != null -> {
                        // No need to set up new read for permanent connection
                        return
                    }

                    else -> {
                        // Handle regular async read
                    }
                }

            } else {
                secondConnection?.close()
                m_SecondConnection = null
                throw VerificationException(
                    "CAN NOT READ FROM DESTINATION",
                    VerificationError.DISCONNECTED_FROM_DESTINATION
                )
            }

        } catch (ex: Exception) {
            if (count > 0 && !(gatewayHandler?.configuration?.terminateWhenError ?: false)) {
                if (gatewayHandler?.started?.load() == true) {
                    try {
                        writeServerLog(ex.toString())

                        // Setup next asynchronous read
                        if (gatewayHandler?.configuration?.advanceOptions?.sslClient == true) {
                            // Handle SSL async read
                            return
                        }

                        if (connectionAsynResult != null) {
                            return
                        }

                        // Handle regular async read
                        return

                    } catch (innerEx: Exception) {
                        // Ignore
                    }
                }
            }

            onError?.let {
                m_LastError = VerificationException(
                    "DISCONNECTED FROM DESTINATION ${ex}",
                    VerificationError.DISCONNECTED_FROM_DESTINATION
                )

                if (secondConnection != null) {
                    it(this)
                    m_SecondConnection = null
                }
            }
        }
    }

    var firstConnection: Socket?
        get() = m_FirstConnection
        set(value) {
            m_FirstConnection = value
        }

    private fun doesConnectToPermanentConnection(): Boolean {
        if (!(nccProcess?.isActive() ?: false) || destinationNII < 1 || destinationNII > 999 ||
            nccProcess?.get(destinationNII)?.pConnection == null
        ) {
            return false
        }

        m_Response?.lengthType =
            nccProcess?.get(destinationNII)?.lengthType ?: MessageLengthType.BCD
        return true
    }

    var secondConnection: Socket?
        get() {
            if (!(nccProcess?.isActive() ?: false)) {
                return m_SecondConnection
            }

            if (destinationNII < 1 || destinationNII > 999) {
                return null
            }

            m_Response?.lengthType =
                nccProcess?.get(destinationNII)?.lengthType ?: MessageLengthType.BCD
            return nccProcess?.get(destinationNII)?.connection
        }
        set(value) {
            if (nccProcess?.isActive() == true) {
                nccProcess?.get(destinationNII)?.connection = value
            } else {
                m_SecondConnection = value
            }
        }

    var remotePort: Int
        get() = m_RemotePort
        set(value) {
            m_RemotePort = value
        }

    var eRequestData: EncryptedTransmittedData?
        get() {
            return if (gatewayHandler?.configuration?.transmission == TransmissionType.ASYNCHRONOUS &&
                gatewayHandler?.configuration?.gatewayType == GatewayType.SERVER &&
                niiClientMapping.containsKey(sourceNII)
            ) {
                niiClientMapping[sourceNII] as? EncryptedTransmittedData
            } else {
                m_Request as? EncryptedTransmittedData
            }
        }
        set(value) {
            m_Request = value
        }

    var eResponseData: EncryptedTransmittedData?
        get() = m_Response as? EncryptedTransmittedData
        set(value) {
            m_Response = value
        }

    suspend fun sendFormatedData() {
        if (eRequestData?.messageType == GatewayMessageType.ADMIN_REQUEST && onAdminResponse != null) {
            val customResponseBytes = ByteArray(1)
            val response = onAdminResponse?.invoke(this, customResponseBytes)

            if (response != null) {
                firstConnection?.getOutputStream()?.write(response, 0, response.size)
                status = TransactionStatus.SUCCESSFUL
                return
            }
        }

        val dataToSend = if (gatewayHandler?.configuration?.gatewayType != GatewayType.SERVER) {
            eRequestData
        } else {
            eResponseData
        }

        val encryptedData =
            dataToSend?.encrypt(gatewayHandler?.configuration?.gatewayType ?: GatewayType.SERVER)
                ?: ByteArray(0)

        if (gatewayHandler?.configuration?.gatewayType == GatewayType.SERVER) {
            when (gatewayHandler?.configuration?.serverConnectionType) {
                ConnectionType.TCP_IP,ConnectionType.REST -> {
                    dataToSend?.writtenMessage?.let { written ->
                        if (gatewayHandler?.configuration?.nccRule == true &&
                            dataToSend.messageType == GatewayMessageType.NORMAL_RESPONSE
                        ) {
                            dataToSend.rawMessage?.let { raw ->
                                bytesCopy(written, raw, 0, 2, 5)
                            }
                        } else if (niiClientMapping.containsKey(sourceNII)) {
                            niiClientMapping[sourceNII]?.fixedHeader?.let { header ->
                                bytesCopy(written, header, 1, 3, 2)
                                bytesCopy(written, header, 3, 1, 2)
                            }
                        }
                    }

                    dataToSend?.lengthType =
                        gatewayHandler?.configuration?.messageLengthTypeSource
                            ?: MessageLengthType.BCD
                    firstConnection?.getOutputStream()?.let { dataToSend?.write(it) }
                }

                ConnectionType.COM -> {
                    gatewayHandler?.firstRs232Handler?.sendMessage(
                        encryptedData,
                        MessageLengthType.NONE
                    )
                }

                else -> {}
            }

            writeServerLog("SEND ENCRYPTED MESSAGE TO CLIENT INSTANCE: ${encryptedData.size} BYTES ")
            status = TransactionStatus.SUCCESSFUL
            if(dataToSend?.rawMessage != null){
                onSentToSource?.invoke(parseData(dataToSend.rawMessage, true))
            }


        } else {
            when (gatewayHandler?.configuration?.destinationConnectionType) {
                ConnectionType.TCP_IP -> {
                    secondConnection?.getOutputStream()?.let { dataToSend?.write(it) }
                }
                ConnectionType.REST -> {
                    val response = sendRest(dataToSend!!.readMessage,m_SecondRestConnection)
                    m_Response?.readMessage = response?.pack()
                }
                ConnectionType.COM -> {
                    gatewayHandler?.secondRs232Handler?.sendMessage(
                        encryptedData,
                        MessageLengthType.NONE
                    )
                }

                else -> {}
            }

            writeServerLog("SEND ENCRYPTED MESSAGE TO SERVER INSTANCE: ${encryptedData.size} BYTES ")
            status = TransactionStatus.SENT_TO_DESTINATION
            if(dataToSend?.rawMessage != null){
                onSentToDest?.invoke(parseData(dataToSend.rawMessage, true))
            }
        }

    }

    fun receiveFormattedData() {
        writeServerLog("RECEIVING ENCRYPTED MESSAGE")

        val dataReceived = if (gatewayHandler?.configuration?.gatewayType == GatewayType.SERVER) {
            eRequestData
        } else {
            eResponseData
        }

        if (gatewayHandler?.configuration?.gatewayType == GatewayType.SERVER) {
            when (gatewayHandler?.configuration?.serverConnectionType) {
                ConnectionType.TCP_IP, ConnectionType.REST-> {
                    firstConnection?.let {
                        dataReceived?.read(
                            it,
                            gatewayHandler?.configuration?.advanceOptions?.timeOutFromSource ?: 30,
                            true
                        )
                    }
                }

                ConnectionType.COM -> {
                    dataReceived?.readMessage = gatewayHandler?.firstRs232Handler?.receiveMessage()
                }

                else -> {}
            }

            dataReceived?.readMessage?.let { readMsg ->
                destinationNII = bcdToBin(getBytesFromBytes(readMsg, 1, 2))
                m_BytesReceiveFromSource += readMsg.size
                gatewayHandler?.bytesIncoming?.set(
                    (gatewayHandler?.bytesIncoming?.get() ?: 0) + readMsg.size
                )

                status = TransactionStatus.RECEIVED_REQUEST
                sourceNII = (readMsg[3].toInt() and 0xFF) * 256 + (readMsg[4].toInt() and 0xFF)
            }

        } else {
            when (gatewayHandler?.configuration?.destinationConnectionType) {
                ConnectionType.TCP_IP, ConnectionType.REST -> {
                    secondConnection?.let {
                        dataReceived?.read(
                            it,
                            gatewayHandler?.configuration?.advanceOptions?.timeOutFromDest ?: 30,
                            false
                        )
                    }
                }

                ConnectionType.COM -> {
                    dataReceived?.readMessage = gatewayHandler?.firstRs232Handler?.receiveMessage()
                }

                else -> {}
            }

            dataReceived?.readMessage?.let { readMsg ->
                m_BytesReceiveFromDestination = readMsg.size
                gatewayHandler?.bytesOutgoing?.set(
                    (gatewayHandler?.bytesOutgoing?.get() ?: 0) + readMsg.size
                )

                status = TransactionStatus.RECEIVED_RESPONSE
            }
        }

        m_BeginTransactionDate = LocalDateTime.now()
    }


    fun respondIso8583ErrorAndThrowException(
        iso8583Request: Iso8583Data,
        responseCode: String,
        log: String
    ) {
        if (firstConnection == null) {
            throw VerificationException("", VerificationError.DISCONNECTED_FROM_SOURCE)
        }

        val iso8583Response = Iso8583Data(gatewayHandler?.configuration!!)

        iso8583Response.packBit(3, iso8583Request[3]?.getString() ?: "")
        iso8583Response.packBit(11, iso8583Request[11]?.getString() ?: "")
        iso8583Response.packBit(13, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd")))
        iso8583Response.packBit(
            12,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
        )

        iso8583Response.messageType = iso8583Request.messageType + 10

        if (iso8583Request[41]?.isSet == true) {
            iso8583Request.packBit(41, iso8583Request[41]?.getString() ?: "")
        }

        iso8583Response.tpduHeader.rawTPDU = iso8583Request.tpduHeader.rawTPDU.clone()
        iso8583Response.tpduHeader.swapNII()

        iso8583Response.packBit(39, responseCode)

        val responseData = iso8583Response.pack(
            gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
        )
        firstConnection?.getOutputStream()?.write(responseData)

        writeServerLog(log)
        writeServerLog("RESPOND TO SOURCE...")
        parseData(responseData, true)
        if ((gatewayHandler?.configuration?.logOptions
                ?: LoggingOption.NONE.value) and LoggingOption.PARSED_DATA.value > LoggingOption.NONE.value
        ) {
            writeServerLog(iso8583Response.logFormat())
        }

        throw VerificationException("", VerificationError.EXCEPTION_HANDLED)
    }

    suspend fun sendMessageToSecondConnection(data: Iso8583Data) {
        m_Request = TransmittedData(
            gatewayHandler?.configuration?.messageLengthTypeSource ?: MessageLengthType.BCD
        )
        m_Request?.readMessage = data.pack()
        status = TransactionStatus.SENT_TO_DESTINATION
        processSynchronous()
    }
}

// Supporting classes
class PermanentConnectionAsynResult {
    var messageReceived: ByteArray? = null
}

class AsyncResult(val result: Any?)

