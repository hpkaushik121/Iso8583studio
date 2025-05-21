package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.CipherMode
import `in`.aicortex.iso8583studio.data.model.CipherType
import `in`.aicortex.iso8583studio.data.model.HashAlgorithm
import `in`.aicortex.iso8583studio.data.model.KeysRecord
import `in`.aicortex.iso8583studio.data.model.VerificationError
import `in`.aicortex.iso8583studio.data.model.VerificationException
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.bytesCopy
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.bytesEqualled
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.bytesToHexString
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.creatBytesFromArray
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.getBytesFromBytes
import `in`.aicortex.iso8583studio.domain.utils.IsoUtil.kvc
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom


class KeyManagement(
    cipherType: CipherType,
    KEK: ByteArray,
    IV: ByteArray,
    mode: CipherMode,
    hashAlgorithm: HashAlgorithm
) {
    var keyExpireAfterTimes: Int = 0
    protected val keyList = sortedMapOf<String, KeysRecord>()
    var _KEK: ByteArray = KEK
    var _InitialVector: ByteArray = IV
    protected var _DefaultSignature: ByteArray = ByteArray(keyLength)
    protected var _CipherType: CipherType = cipherType
    protected var _CipherMode: CipherMode = mode
    protected var _RandomGenerator: SecureRandom = SecureRandom()
    protected var cryptoHandler: EncrDecrHandler = EncrDecrHandler(_CipherType, _KEK, _InitialVector, mode)

    // Use Java's MessageDigest instead of .NET's HashAlgorithm
    protected var _HashHandler: MessageDigest = when (hashAlgorithm) {
        HashAlgorithm.SHA1 -> MessageDigest.getInstance("SHA-1")
        HashAlgorithm.MD5 -> MessageDigest.getInstance("MD5")
        else -> MessageDigest.getInstance("SHA-1")
    }

    protected var desHandler: EncrDecrHandler = EncrDecrHandler(
        CipherType.DES,
        byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8),
        ByteArray(8),
        CipherMode.CBC
    )

    // Event for different KEK
    var setDifferentKEK: ((String) -> ByteArray?)? = null

    companion object {
        val KEY_VERSION: String = "Sourabh Kaushik, sk@aicortex.in"

        val kekEncrypter: EncrDecrHandler = EncrDecrHandler(
            CipherType.TRIPLE_DES,
            byteArrayOf(
                4, 13, -95, 14, -80, -98, -51, -38,
                -32, -27, -2, -98, -29, 7, -95, -111,
                -116, 112, 41, -99, 73, -105, 97, -32
            ),
            ByteArray(8),
            CipherMode.CBC
        )

        private var _SecretKey: EncrDecrHandler? = null
        private var _EncryptLogHandler: EncrDecrHandler? = null

//        val isHSMMode: Boolean
//            get() {
//                return try {
//                    PKCS11Session.HSMModule != null
//                } catch (e: Exception) {
//                    false
//                }
//            }

        val secretKey: EncrDecrHandler
            get() {
                if (_SecretKey == null) {
                    _SecretKey = EncrDecrHandler(
                        CipherType.DES,
                        byteArrayOf(67, 34, 17, 17, 86, 17, 17, 17),
                        ByteArray(8),
                        CipherMode.ECB
                    )
                }
                return _SecretKey!!
            }

        val encryptLogHandler: EncrDecrHandler
            get() {
                if (_EncryptLogHandler == null) {
                    _EncryptLogHandler = EncrDecrHandler(
                        CipherType.TRIPLE_DES,
                        byteArrayOf(
                            67, 34, 17, 104, 86, 17, 50, -17,
                            67, 34, 68, -8, 86, 17, -30, 31
                        ),
                        ByteArray(8),
                        CipherMode.CBC
                    )
                }
                return _EncryptLogHandler!!
            }

        fun keyInfo(): String {
            return "$KEY_VERSION with KVC = ${
                bytesToHexString(
                    getBytesFromBytes(
                        kvc(kekEncrypter.key, kekEncrypter.cipherType), 0, 3
                    )
                )
            }"
        }
    }

    // Constructors
    protected constructor() : this(
        CipherType.DES,
        ByteArray(8),
        ByteArray(8),
        CipherMode.CBC,
        HashAlgorithm.SHA1
    )

    // Properties
    var defaultSignature: ByteArray
        get() = _DefaultSignature
        set(value) {
            _DefaultSignature = ByteArray(keyLength)
            val copyLength = minOf(_DefaultSignature.size, value.size)
            bytesCopy(_DefaultSignature, value, 0, 0, copyLength)
        }

    val keyLength: Int
        get() = _KEK.size

    val blockLength: Int
        get() = cryptoHandler.blockSize

    // Public methods
    fun getEncryptedPassword(password: String): ByteArray {
        // Using Java's MessageDigest instead of .NET's HashAlgorithm
        val hash = _HashHandler.digest(password.toByteArray(StandardCharsets.US_ASCII))
        val ar = ByteArray(32)
        hash.copyInto(ar, 0, 0, hash.size)
        return creatBytesFromArray(ar, 0, keyLength)
    }

    @Synchronized
    fun generateRandomKey(clientId: String) {
        if (keyExpireAfterTimes <= 0) return

        keyList[clientId]?.let { record ->
            record.timesRemaining--
            if (record.timesRemaining <= 0) {
                _RandomGenerator.nextBytes(record.dek)
                _RandomGenerator.nextBytes(record.mpk)
                record.timesRemaining = keyExpireAfterTimes
            }
        }
    }

    @Synchronized
    fun getRMACByMPK(clientId: String, input: ByteArray): ByteArray {
        keyList[clientId]?.let { record ->
            desHandler.key = getBytesFromBytes(record.mpk, 0, 8)
            val encrypted1 = desHandler.encrypt(input)
            val result = getBytesFromBytes(encrypted1, encrypted1.size - 8, 8)

            if (record.mpk.size == 8) return result

            desHandler.key = getBytesFromBytes(record.mpk, 8, 8)
            val decrypted = desHandler.decrypt(result)

            desHandler.key = when (record.mpk.size) {
                16 -> getBytesFromBytes(record.mpk, 0, 8)
                24 -> getBytesFromBytes(record.mpk, 16, 8)
                else -> getBytesFromBytes(record.mpk, 0, 8)
            }

            return desHandler.encrypt(decrypted)
        }

        throw IllegalStateException("Client ID $clientId not found")
    }

    @Synchronized
    fun newClientKeys(clientID: String) {
        if (keyList.containsKey(clientID)) return

        val newRecord = KeysRecord().apply {
            dek = ByteArray(keyLength)
            mpk = ByteArray(keyLength)
            _RandomGenerator.nextBytes(dek)
            _RandomGenerator.nextBytes(mpk)
            csk = _DefaultSignature.clone()
            timesRemaining = keyExpireAfterTimes
        }

        keyList[clientID] = newRecord
    }

    @Synchronized
    fun containsKey(keyId: String): Boolean {
        return keyList.containsKey(keyId)
    }

    fun generateMAC(clientID: String, rawMessage: ByteArray): ByteArray {
        keyList[clientID]?.let { record ->
            return cryptoHandler.encrypt(_HashHandler.digest(rawMessage), record.mpk)
        }
        throw IllegalStateException("Client ID $clientID not found")
    }

    @Synchronized
    fun decryptMessage(clientId: String, input: ByteArray, from: Int, count: Int): ByteArray {
        keyList[clientId]?.let { record ->
            cryptoHandler.key = record.dek
            val output = ByteArray(count)
            val decrypted = cryptoHandler.decrypt(input, from, roundedKeySize(count))
            bytesCopy(output, decrypted, 0, 0, count)
            return output
        }
        throw IllegalStateException("Client ID $clientId not found")
    }

    fun roundedKeySize(messageLen: Int): Int {
        return ((messageLen + blockLength - 1) / blockLength) * blockLength
    }

    fun encrypteMessage(clientId: String, input: ByteArray, from: Int, count: Int): ByteArray {
        keyList[clientId]?.let { record ->
            return cryptoHandler.encrypt(input, record.dek)
        }
        throw IllegalStateException("Client ID $clientId not found")
    }

    @Synchronized
    fun getEncrypteMPK(clientId: String): ByteArray {
        keyList[clientId]?.let { record ->
            return cryptoHandler.encrypt(record.mpk, getKEK(clientId))
        }
        throw IllegalStateException("Client ID $clientId not found")
    }

    @Synchronized
    fun getEncryptedDEK(clientId: String): ByteArray {
        keyList[clientId]?.let { record ->
            return cryptoHandler.encrypt(record.dek, getKEK(clientId))
        }
        throw IllegalStateException("Client ID $clientId not found")
    }

    @Synchronized
    fun getEncryptedKEK(): ByteArray {
        return kekEncrypter.encrypt(_KEK)
    }

    @Synchronized
    fun setEncryptedMPK(clientId: String, key: ByteArray) {
        keyList[clientId]?.mpk = cryptoHandler.decrypt(key, _KEK)
    }

    @Synchronized
    fun setEncryptedDEK(clientId: String, key: ByteArray) {
        keyList[clientId]?.dek = cryptoHandler.decrypt(key, _KEK)
    }

    @Synchronized
    fun getEncryptedCSK(clientId: String, isEncryptedByDEK: Boolean): ByteArray {
        keyList[clientId]?.let { record ->
            return try {
                if (!isEncryptedByDEK) {
                    cryptoHandler.encrypt(record.csk, getKEK(clientId))
                } else {
                    cryptoHandler.encrypt(record.csk, record.dek)
                }
            } catch (ex: Exception) {
                throw ex
            }
        }
        throw IllegalStateException("Client ID $clientId not found")
    }

    fun getKEK(clientId: String): ByteArray {
        val kek = setDifferentKEK?.invoke(clientId)
        return kek ?: _KEK
    }

    @Synchronized
    fun checkEnable(clientId: String) {
        keyList[clientId]?.let { record ->
            if (!record.enable) {
                throw VerificationException("CLIENT $clientId IS NOT ENABLED", VerificationError.DECLINED)
            }
        }
    }

    @Synchronized
    fun verifyCSK(clientId: String, keyToVerify: ByteArray, isEncryptedByDEK: Boolean): Boolean {
        keyList[clientId]?.let { record ->
            if (!record.enable) {
                throw VerificationException("NOT ENABLED", VerificationError.DECLINED)
            }

            val decrypted = if (isEncryptedByDEK) {
                cryptoHandler.decrypt(keyToVerify, record.dek)
            } else {
                cryptoHandler.decrypt(keyToVerify, _KEK)
            }

            return bytesEqualled(decrypted, record.csk)
        }
        throw IllegalStateException("Client ID $clientId not found")
    }

    fun getCipherInfo(): ByteArray {
        return byteArrayOf(
            (_CipherType.ordinal + 1).toByte(),
            0,
            17
        )
    }

    @Synchronized
    fun remove(keyId: String) {
        keyList.remove(keyId)
    }

    @Synchronized
    fun add(keyId: String, rec: KeysRecord) {
        keyList[keyId] = rec
    }

    @Synchronized
    fun getKeyRecord(keyId: String): KeysRecord {
        keyList[keyId]?.let { record ->
            return KeysRecord().apply {
                csk = record.csk.clone()
                dek = record.dek.clone()
                enable = record.enable
                mpk = record.mpk.clone()
                timesRemaining = record.timesRemaining
            }
        }
        throw IllegalStateException("Client ID $keyId not found")
    }
}
