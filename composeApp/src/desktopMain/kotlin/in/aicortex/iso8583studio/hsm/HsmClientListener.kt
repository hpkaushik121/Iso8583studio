package `in`.aicortex.iso8583studio.hsm

import `in`.aicortex.iso8583studio.data.HsmClient

interface HsmClientListener {
    fun onDisconnected(hsmClient : HsmClient?)
    fun onSentToSource(data : String?)
    fun onReceivedFormSource(data : String?,hsmClient : HsmClient?)
}