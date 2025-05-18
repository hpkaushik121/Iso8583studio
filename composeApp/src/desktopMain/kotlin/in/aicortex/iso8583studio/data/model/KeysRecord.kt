package `in`.aicortex.iso8583studio.data.model

import javax.crypto.SecretKey

class KeysRecord {
    var dek: ByteArray = ByteArray(0)
    var mpk: ByteArray = ByteArray(0)
    var csk: ByteArray = ByteArray(0)
    var timesRemaining: Int = 0
    var enable: Boolean = true
    var dekHandle: SecretKey? = null
    var mpkHandle: SecretKey? = null
}