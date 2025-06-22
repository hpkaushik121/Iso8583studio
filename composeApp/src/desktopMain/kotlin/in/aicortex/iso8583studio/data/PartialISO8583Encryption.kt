package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.BitType
import `in`.aicortex.iso8583studio.data.model.ObscureType
import `in`.aicortex.iso8583studio.data.model.VerificationError
import `in`.aicortex.iso8583studio.data.model.VerificationException
import `in`.aicortex.iso8583studio.domain.service.hostSimulatorService.HostSimulator
import ai.cortex.core.IsoUtil
import `in`.aicortex.iso8583studio.domain.utils.Utils


/**
 * PartialISO8583Encryption class for handling ISO8583 message encryption/decryption
 */
class PartialISO8583Encryption(
    private val m_ObscureType: ObscureType,
    private val ObscuredBits: IntArray
) {
    private var m_EncryptionDataBit: Int = 40
    private lateinit var GatewayHandler: HostSimulator

    /**
     * Decodes the encrypted ISO8583 message
     */
    fun decode(input: Iso8583Data, trippleDesKey: ByteArray) {
        if (input[m_EncryptionDataBit]?.isSet == false &&
            (m_ObscureType == ObscureType.ReplacedByEncryptedData ||
                    m_ObscureType == ObscureType.ReplacedByZero)) {
            throw VerificationException("NO ENCRYPTION DATA", VerificationError.DECLINED)
        }

        val str = input[41]?.getString()
        str?.let { GatewayHandler.endeService.containsKey(it) }

        val numArray1 = arrayOfNulls<ByteArray>(ObscuredBits.size)
        val numArray2 = arrayOfNulls<ByteArray>(ObscuredBits.size)
        val data1 = input[m_EncryptionDataBit]?.data ?: byteArrayOf(0)

        for (index in ObscuredBits.indices) {
            if (input[ObscuredBits[index]]?.isSet == true) {
                val data2 = input[ObscuredBits[index]]?.data
                val length = ((data2?.size ?: 0) + 7) / 8 * 8
                numArray2[index] = ByteArray(length)
                data2?.copyInto(numArray2[index]!!, 0)
            } else {
                numArray1[index] = ByteArray(0)
                numArray2[index] = ByteArray(0)
            }
        }

        var index1 = 0
        var len1: Int
        var offset: Int

        if (m_ObscureType == ObscureType.ReplacedByEncryptedDataSimple) {
            for (index2 in ObscuredBits.indices) {
                val obscuredBit = ObscuredBits[index2]
                if (input[obscuredBit]?.isSet == true) {
                    numArray2[index2] = input[obscuredBit]?.data

                    val decryptedData = GatewayHandler.endeService.decryptMessage(
                        str!!,
                        numArray2[index2]!!,
                        0,
                        numArray2[index2]!!.size
                    )
                    numArray1[index2] = decryptedData

                    val _input = numArray1[index2]!!
                    var _arInput = ByteArray(0)
                    var len2 = _input.size

                    if (input[obscuredBit]?.lengthAttribute == BitLength.LLVAR) {
                        _arInput = Utils.getBytesFromBytes(_input, 0, 1)
                        len2 = IsoUtil.bcdToBin(_arInput)
                    } else if (input[obscuredBit]?.lengthAttribute == BitLength.LLLVAR) {
                        _arInput = Utils.getBytesFromBytes(_input, 0, 2)
                        len2 = IsoUtil.bcdToBin(_arInput)
                    }

                    input[obscuredBit]?.length = len2
                    if (input[obscuredBit]?.typeAtribute == BitType.BCD) {
                        len2 = (len2 + 1) / 2
                    }
                    input[obscuredBit]?.data = Utils.getBytesFromBytes(_input, _arInput.size, len2)
                }
            }
        } else {
            index1 = 0
            while (index1 < data1.size) {
                val bitnumber = data1[index1].toInt()
                var index3 = index1 + 1

                var index4 = 0
                while (index4 < ObscuredBits.size && ObscuredBits[index4] != bitnumber) {
                    ++index4
                }

                len1 = data1[index3].toInt()
                offset = index3 + 1

                if (index4 < ObscuredBits.size) {
                    if (m_ObscureType == ObscureType.ReplacedByEncryptedData && input[bitnumber]?.isSet == true) {
                        Utils.getBytesFromBytes(data1, offset, len1)
                            .copyInto(numArray2[index4]!!, numArray2[index4]!!.size - len1)

                        val decryptedData = GatewayHandler.endeService.decryptMessage(
                            str!!,
                            numArray2[index4]!!,
                            0,
                            numArray2[index4]!!.size,
                        )
                        numArray1[index4] = decryptedData

                        Utils.getBytesFromBytes(numArray1[index4]!!, 0, input[bitnumber]?.data?.size ?:0)
                            .copyInto(input[bitnumber]?.data ?: byteArrayOf(0), 0)
                    } else if (m_ObscureType == ObscureType.ReplacedByZero && input[bitnumber]?.isSet ==true) {
                        Utils.getBytesFromBytes(data1, offset, len1)
                            .copyInto(numArray2[index4]!!, 0)

                        val decryptedData = GatewayHandler.endeService.decryptMessage(
                            str!!,
                            numArray2[index4]!!,
                            0,
                            numArray2[index4]!!.size,
                        )
                        numArray1[index4] = decryptedData

                        Utils.getBytesFromBytes(numArray1[index4]!!, 0, input[bitnumber]?.data?.size ?: 0)
                            .copyInto(input[bitnumber]?.data ?: byteArrayOf(0), 0)
                    }
                }

                index1 = offset + len1
            }
        }

        input[m_EncryptionDataBit]?.isSet = false
        input[64]?.isSet = false
    }

    /**
     * Encodes the ISO8583 message with encryption
     */
    fun Encode(input: Iso8583Data, trippleDesKey: ByteArray) {
        val numArray1 = arrayOfNulls<ByteArray>(ObscuredBits.size)
        val numArray2 = arrayOfNulls<ByteArray>(ObscuredBits.size)
        val _input = ByteArray(800)
        var index1 = 0
        val clientId = input[41]?.getString()

        for (index2 in ObscuredBits.indices) {
            if (input[ObscuredBits[index2]]?.isSet == true) {
                val obscuredBit = ObscuredBits[index2]
                var numArray3 = input[ObscuredBits[index2]]?.data

                if (m_ObscureType == ObscureType.ReplacedByEncryptedDataSimple) {
                    if (input[obscuredBit]?.lengthAttribute == BitLength.LLVAR) {
                        val newArray = ByteArray((numArray3?.size ?: 0) + 1)
                        input[obscuredBit]?.data?.copyInto(newArray, 1)
                        IsoUtil.binToBcd(input[obscuredBit]?.length ?: 0, 1).copyInto(newArray, 0)
                        numArray3 = newArray
                    } else if (input[obscuredBit]?.lengthAttribute == BitLength.LLLVAR) {
                        val newArray = ByteArray((numArray3?.size ?: 0) + 2)
                        input[obscuredBit]?.data?.copyInto(newArray, 2)
                        IsoUtil.binToBcd(input[obscuredBit]?.length ?: 0, 2).copyInto(newArray, 0)
                        numArray3 = newArray
                    }
                }

                val length = ((numArray3?.size ?: 0) + 7) / 8 * 8
                numArray1[index2] = ByteArray(length)
                numArray3?.copyInto(numArray1[index2]!!, 0)

                val encryptedData = GatewayHandler.endeService.encrypteMessage(
                    clientId!!,
                    numArray1[index2]!!,
                    0,
                    numArray1[index2]!!.size
                )
                numArray2[index2] = encryptedData

                if (m_ObscureType == ObscureType.ReplacedByEncryptedDataSimple) {
                    input[obscuredBit]?.data = numArray2[index2]!!
                    input[obscuredBit]?.length = input[obscuredBit]?.data?.size ?: 0
                    if (input[obscuredBit]?.typeAtribute == BitType.BCD) {
                        input[obscuredBit]?.length *= 2
                    }
                } else if (m_ObscureType == ObscureType.ReplacedByEncryptedData) {
                    _input[index1] = ObscuredBits[index2].toByte()
                    val index3 = index1 + 1
                    _input[index3] = (length - (numArray3?.size ?: 0)).toByte()
                    index1 = index3 + 1

                    if (length - (numArray3?.size ?: 0) > 0) {
                        Utils.getBytesFromBytes(numArray2[index2]!!, 0, numArray3?.size ?: 0)
                            .copyInto(numArray3 ?: byteArrayOf(0), 0)
                        Utils.getBytesFromBytes(numArray2[index2]!!, numArray3?.size ?: 0, length - (numArray3?.size ?: 0))
                            .copyInto(_input, index1)
                        index1 += length - (numArray3?.size ?: 0)
                    }
                } else if (m_ObscureType == ObscureType.ReplacedByZero) {
                    _input[index1] = ObscuredBits[index2].toByte()
                    val index4 = index1 + 1
                    _input[index4] = length.toByte()
                    val index5 = index4 + 1

                    ByteArray(numArray3?.size ?: 0).copyInto(numArray3 ?: byteArrayOf(0), 0)
                    numArray2[index2]!!.copyInto(_input, index5)
                    index1 = index5 + length
                }
            } else {
                numArray1[index2] = ByteArray(0)
                numArray2[index2] = ByteArray(0)
            }
        }

        if (index1 > 0) {
            input.packBit(m_EncryptionDataBit, Utils.getBytesFromBytes(_input, 0, index1))
        }
    }

    /**
     * Sets the Gateway instance and registers event handlers
     */
    fun setActive(gw: HostSimulator) {
        GatewayHandler = gw
//        gw.onReceiveFromSource(::gw_OnReceiveFromSource)
//        gw.onReceiveFromDest (::gw_OnReceiveFromDest)
    }

    /**
     * Handler for receiving responses from the destination
     */
//    private fun gw_OnReceiveFromDest( _response: ByteArray): ByteArray {
//        var response = _response
//
//        if ((GatewayHandler.configuration.logOptions and LoggingOption.RAW_DATA.value) > LoggingOption.NONE.value) {
//            trans.writeServerLog(IsoUtil.bytesToHexString(response, 32, false))
//        }
//
//        val input = Iso8583Data(GatewayHandler.configuration.bitTemplateSource,GatewayHandler.configuration)
//        input.unpack(response, 2, response.size - 2)
//
//        if ((GatewayHandler.configuration.logOptions and LoggingOption.PARSED_DATA.value) > LoggingOption.NONE.value) {
//            trans.writeServerLog(input.logFormat())
//        }
//
//        Encode(input, GatewayHandler.configuration.advanceOptions.getSecretKey())
//        trans.writeServerLog("SEND RESPONSE")
//
//        val clientId = input[41]?.getString()
//        input.packBit(64, ByteArray(8))
//        response = input.pack(GatewayHandler.configuration.messageLengthTypeSource)
//
//        val rmacByMpk = GatewayHandler.endeService.getRMACByMPK(
//            clientId!!,
//            IsoUtil.getBytesFromBytes(response, 7, response.size - 7 - 8)
//        )
//        rmacByMpk.copyInto(response, response.size - 8)
//        input[64]?.data = rmacByMpk
//
//        return response
//    }

    /**
     * Handler for receiving messages from the source
     */
//    private suspend fun gw_OnReceiveFromSource( _response: ByteArray): ByteArray {
//        var response = _response
//
//        if ((GatewayHandler.configuration.logOptions and LoggingOption.RAW_DATA.value) > LoggingOption.NONE.value) {
//            trans.writeServerLog(IsoUtil.bytesToHexString(response, 32, false))
//        }
//
//        val iso8583Data = Iso8583Data(GatewayHandler.configuration.bitTemplateSource,GatewayHandler.configuration)
//        iso8583Data.unpack(response, 2, response.size - 2)
//
//        val terminalId = iso8583Data[41]?.getString()
//        trans.writeServerLog("RECIEVED MESSAGE")
//
//        if ((GatewayHandler.configuration.logOptions and LoggingOption.PARSED_DATA.value) > LoggingOption.NONE.value) {
//            trans.writeServerLog(iso8583Data.logFormat())
//        }
//
//        if (iso8583Data.messageType == "0800" && iso8583Data[3]?.getString() == "850000") {
//            trans.cancelSend = true
//            GatewayHandler.endeService.newClientKeys(terminalId!!)
//            GatewayHandler.endeService.generateRandomKey(terminalId)
//
//            var configStr = "${m_ObscureType.ordinal};"
//            for (index in ObscuredBits.indices) {
//                if (index > 0) {
//                    configStr += ","
//                }
//                configStr += ObscuredBits[index].toString()
//            }
//
//            GatewayHandler.configuration.advanceOptions.getSecretKey()
//            GatewayHandler.configuration.advanceOptions.getSecretKey()
//
//            val encryptedDEK = IsoUtil.bcdToString(GatewayHandler.endeService.getEncryptedDEK(terminalId))
//            val encryptedMPK = IsoUtil.bcdToString(GatewayHandler.endeService.getEncrypteMPK(terminalId))
//            val fieldValue = "$configStr;$encryptedDEK;$encryptedMPK"
//
//            iso8583Data.tpduHeader.swapNII()
//            iso8583Data.messageType = "0810"
//            iso8583Data.packBit(60, fieldValue)
//            iso8583Data.packBit(39, "00")
//
//            trans.writeServerLog("PROCESSED GET ENCRYPTION INFO")
//            response = iso8583Data.pack(GatewayHandler.configuration.messageLengthTypeSource)
//            trans.send(response,true)
//
//            if ((GatewayHandler.configuration.logOptions and LoggingOption.RAW_DATA.value) > LoggingOption.NONE.value) {
//                trans.writeServerLog(IsoUtil.bytesToHexString(response, 32, false))
//            }
//
//            if ((GatewayHandler.configuration.logOptions and LoggingOption.PARSED_DATA.value) > LoggingOption.NONE.value) {
//                trans.writeServerLog(iso8583Data.logFormat())
//            }
//        } else {
//            if (!GatewayHandler.endeService.containsKey(terminalId!!)) {
//                trans.respondIso8583ErrorAndThrowException(iso8583Data, "P2", "NOT LOGON BEFORE")
//            }
//
//            val rmacByMpk = GatewayHandler.endeService.getRMACByMPK(
//                terminalId,
//                IsoUtil.getBytesFromBytes(response, 7, response.size - 7 - 8)
//            )
//
//            if (iso8583Data[64]?.isSet == false) {
//                trans.respondIso8583ErrorAndThrowException(iso8583Data, "P2", "MAC (Bit 64) NOT SENT")
//            }
//
//            val data = iso8583Data[64]?.data ?:byteArrayOf(0)
//            if (!IsoUtil.bytesEqualled(rmacByMpk, data)) {
//                trans.respondIso8583ErrorAndThrowException(iso8583Data, "P2", "MAC IS INVALID")
//            }
//
//            decode(iso8583Data, GatewayHandler.configuration.advanceOptions.getSecretKey())
//            response = iso8583Data.pack(GatewayHandler.configuration.messageLengthTypeSource)
//        }
//
//        return response
//    }
}