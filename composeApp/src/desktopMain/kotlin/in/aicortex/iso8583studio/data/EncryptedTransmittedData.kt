package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.GWHeaderTAG
import `in`.aicortex.iso8583studio.data.model.GatewayMessageType
import `in`.aicortex.iso8583studio.data.model.GatewayType
import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import `in`.aicortex.iso8583studio.data.model.TagLengthValue
import `in`.aicortex.iso8583studio.data.model.VerificationError
import `in`.aicortex.iso8583studio.data.model.VerificationException
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.bytesCopy
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.bytesEqualled
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.intToMessageLength
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.messageLengthToInt
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.SortedMap

class EncryptedTransmittedData(
    private val endeService: KeyManagement,
    lengthType: MessageLengthType
) : TransmittedData(lengthType) {

    var messageLength: Int = 0
    var mac: ByteArray? = null
    var clientID: String = ""
    var locationID: String = ""
    var rawMessage: ByteArray? = null
    var signatureVerified: Boolean = false
    var adminCommand: String = ""
    var adminContent: String = ""
    var error: VerificationError = VerificationError.DECLINED // Default value
    var clientVersion: String = ""
    var messageType: GatewayMessageType = GatewayMessageType.NORMAL_REQUEST // Default value

    fun logFormat(): String = ""

    private fun unpackAdmin(tags: SortedMap<GWHeaderTAG, ByteArray?>) {
        adminCommand = String(tags[GWHeaderTAG.TAG_ADMIN_COMMAND]!!, Charset.defaultCharset())
        adminContent = String(tags[GWHeaderTAG.TAG_AMDIN_CONTENT]!!, Charset.defaultCharset())

        val secretDecrypted = KeyManagement.secretKey.decrypt(tags[GWHeaderTAG.TAG_ADMIN_SECRET]!!)
        val timestamp = ByteBuffer.wrap(secretDecrypted).long

        if (System.currentTimeMillis() - timestamp > 100000000L) {
            throw Exception("THE ADMIN-REQUEST HAS WRONG SIGNATURE")
        }
    }

    private fun unPack(tags: SortedMap<GWHeaderTAG, ByteArray?>) {
        synchronized(endeService) {
            messageType = GatewayMessageType.fromValue(
                ByteBuffer.wrap(tags[GWHeaderTAG.TAG_MESSAGETYPE]).short.toInt()
            )

            clientID = String(tags[GWHeaderTAG.TAG_CLIENTID]!!, Charset.defaultCharset())
            messageLength = 0

            if (!endeService.containsKey(clientID)) {
                if (messageType == GatewayMessageType.NORMAL_REQUEST) {
                    throw VerificationException("THIS CLIENT DIDN'T LOGON BEFORE", VerificationError.NOT_SEND_LOGON_BEFORE)
                }
                endeService.newClientKeys(clientID)
            }

            locationID = String(tags[GWHeaderTAG.TAG_MERCHANTID]!!, Charset.defaultCharset())

            when (messageType) {
                GatewayMessageType.NORMAL_REQUEST -> {
                    endeService.checkEnable(clientID)
                    signatureVerified = endeService.verifyCSK(clientID, tags[GWHeaderTAG.TAG_ENCRYPTED_CSK]!!, true)
                    mac = tags[GWHeaderTAG.TAG_ENCRYPTED_MAC]!!.clone()
                    messageLength = messageLengthToInt(tags[GWHeaderTAG.TAG_LENGTH_OF_MESSAGE]!!, MessageLengthType.BCD)
                }

                GatewayMessageType.NORMAL_RESPONSE -> {
                    endeService.setEncryptedDEK(clientID, tags[GWHeaderTAG.TAG_Encrypted_DEK]!!)
                    endeService.setEncryptedMPK(clientID, tags[GWHeaderTAG.TAG_ENCRYPTED_MPK]!!)
                    messageLength = messageLengthToInt(tags[GWHeaderTAG.TAG_LENGTH_OF_MESSAGE]!!, MessageLengthType.BCD)
                }

                GatewayMessageType.LOGON_REQUEST -> {
                    if (!endeService.containsKey(clientID)) {
                        endeService.newClientKeys(clientID)
                    }
                }

                GatewayMessageType.LOGON_RESPONSE -> {
                    endeService.setEncryptedDEK(clientID, tags[GWHeaderTAG.TAG_Encrypted_DEK]!!)
                    endeService.setEncryptedMPK(clientID, tags[GWHeaderTAG.TAG_ENCRYPTED_MPK]!!)
                    messageLength = messageLengthToInt(tags[GWHeaderTAG.TAG_LENGTH_OF_MESSAGE]!!, MessageLengthType.BCD)
                }

                GatewayMessageType.GET_KEK_REQUEST -> {
                    signatureVerified = endeService.verifyCSK(clientID, tags[GWHeaderTAG.TAG_CSK]!!, false)
                    clientVersion = ""

                    if (tags.containsKey(GWHeaderTAG.TAG_CLIENT_VERSION)) {
                        clientVersion = String(
                            KeyManagement.secretKey.decrypt(tags[GWHeaderTAG.TAG_CLIENT_VERSION]!!),
                            Charset.forName("ASCII")
                        ).trim()
                    }
                }

                else -> { /* Do nothing for other message types */ }
            }
        }
    }

    fun unpackMessage(gatewayType: GatewayType) {
        val sourceIndex = 0
        readMessage?.let { message ->
            bytesCopy(fixedHeader, message, 0, sourceIndex, fixedHeader.size)
            var start = sourceIndex + fixedHeader.size

            val tags = TagLengthValue.getTAGs(message, start).first
            start = TagLengthValue.getTAGs(message, start).second

            messageType = GatewayMessageType.fromValue(
                ByteBuffer.wrap(tags[GWHeaderTAG.TAG_MESSAGETYPE]).short.toInt()
            )

            when (messageType) {
                GatewayMessageType.ADMIN_REQUEST, GatewayMessageType.ADMIN_RESPONSE -> {
                    unpackAdmin(tags)
                    signatureVerified = true
                }
                else -> {
                    unPack(tags)
                    if ((messageLength == 0) ||
                        ((message.size - start) % endeService.blockLength == 0 &&
                                messageLength <= message.size - start)) {
                        // Valid message length
                    } else {
                        throw VerificationException(
                            "ENCRYPTED DATA'S LENGTH IS NOT CORRECT: encrypted${message.size} position:$start MessageLength$messageLength",
                            VerificationError.PACK_DATA_ERROR
                        )
                    }
                }
            }
        }
    }

    fun decryptMessage() {
        if (messageLength == 0) return

        readMessage?.let { message ->
            rawMessage = endeService.decryptMessage(
                clientID,
                message,
                message.size - endeService.roundedKeySize(messageLength),
                messageLength
            )
        }
    }

    fun verifyMAC(): Boolean {
        rawMessage?.let { rawMsg ->
            val generatedMAC = endeService.generateMAC(clientID, rawMsg)
            mac?.let { storedMAC ->
                return bytesEqualled(generatedMAC, storedMAC)
            }
        }
        return false
    }

    fun encrypt(gatewayType: GatewayType): ByteArray {
        try {
            val packedTags = TagLengthValue.packTAGs(pack())

            val messageSize = if (rawMessage != null) {
                fixedHeader.size + packedTags.size + endeService.roundedKeySize(rawMessage!!.size)
            } else {
                fixedHeader.size + packedTags.size
            }

            val writtenMessage = ByteArray(messageSize)
            var index = 0

            if (messageType == GatewayMessageType.NORMAL_REQUEST) {
                rawMessage?.let { rawMsg ->
                    bytesCopy(fixedHeader, rawMsg, 0, 2, 5)
                }
            }

            fixedHeader.copyInto(writtenMessage, index)
            index += fixedHeader.size

            packedTags.copyInto(writtenMessage, index)
            index += packedTags.size

            if (messageType == GatewayMessageType.NORMAL_REQUEST || messageType == GatewayMessageType.NORMAL_RESPONSE) {
                rawMessage?.let { rawMsg ->
                    val encrypted = endeService.encrypteMessage(clientID, rawMsg, 0, messageLength)
                    encrypted.copyInto(writtenMessage, index)
                }
            }

            this.writtenMessage = writtenMessage
            return writtenMessage

        } catch (ex: Exception) {
            throw VerificationException(ex.toString(), VerificationError.PACK_DATA_ERROR)
        }
    }

    fun headerString(): String {
        return "PARSED HEADER: ${messageType.name} LOCATIONID = $locationID CLIENTID = $clientID"
    }

    private fun packError(): SortedMap<GWHeaderTAG, ByteArray?> {
        val map = sortedMapOf<GWHeaderTAG, ByteArray>()

        map[GWHeaderTAG.TAG_MESSAGETYPE] = ByteBuffer.allocate(2)
            .putShort(messageType.value.toShort())
            .array()

        map[GWHeaderTAG.TAG_ERROR_CODE] = ByteBuffer.allocate(2)
            .putShort(error.ordinal.toShort())
            .array()

        return map as SortedMap<GWHeaderTAG, ByteArray?>
    }

    private fun pack(): SortedMap<GWHeaderTAG, ByteArray?> {
        val sortedMap = sortedMapOf<GWHeaderTAG, ByteArray>()

        synchronized(endeService) {
            if (messageType == GatewayMessageType.ERROR_RESPONSE) {
                return packError()
            }

            sortedMap[GWHeaderTAG.TAG_MESSAGETYPE] = ByteBuffer.allocate(2)
                .putShort(messageType.value.toShort())
                .array()

            sortedMap[GWHeaderTAG.TAG_CIPHER_INFO] = endeService.getCipherInfo()
            sortedMap[GWHeaderTAG.TAG_CLIENTID] = clientID.toByteArray(Charset.forName("ASCII"))
            sortedMap[GWHeaderTAG.TAG_MERCHANTID] = locationID.toByteArray(Charset.forName("ASCII"))
            sortedMap[GWHeaderTAG.TAG_ERROR_CODE] = ByteBuffer.allocate(2).putShort(0).array()

            when (messageType) {
                GatewayMessageType.NORMAL_REQUEST -> {
                    sortedMap[GWHeaderTAG.TAG_ENCRYPTED_CSK] = endeService.getEncryptedCSK(clientID, true)
                    rawMessage?.let { rawMsg ->
                        sortedMap[GWHeaderTAG.TAG_ENCRYPTED_MAC] = endeService.generateMAC(clientID, rawMsg)
                    }
                }

                GatewayMessageType.NORMAL_RESPONSE, GatewayMessageType.LOGON_RESPONSE -> {
                    endeService.generateRandomKey(clientID)
                    sortedMap[GWHeaderTAG.TAG_Encrypted_DEK] = endeService.getEncryptedDEK(clientID)
                    sortedMap[GWHeaderTAG.TAG_ENCRYPTED_MPK] = endeService.getEncrypteMPK(clientID)
                }

                GatewayMessageType.LOGON_REQUEST -> {
                    sortedMap[GWHeaderTAG.TAG_ENCRYPTED_CSK] = endeService.getEncryptedCSK(clientID, false)
                }

                GatewayMessageType.GET_KEK_RESPONSE -> {
                    sortedMap[GWHeaderTAG.TAG_ENCRYPTED_KEK] = endeService.getEncryptedKEK()
                    sortedMap[GWHeaderTAG.TAG_IV] = endeService._InitialVector
                }

                else -> { /* Do nothing for other message types */ }
            }

            sortedMap[GWHeaderTAG.TAG_LENGTH_OF_MESSAGE] = intToMessageLength(
                rawMessage?.size ?: 0,
                MessageLengthType.BCD
            )
        }

        return sortedMap as SortedMap<GWHeaderTAG, ByteArray?>
    }
}