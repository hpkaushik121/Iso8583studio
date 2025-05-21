package `in`.aicortex.iso8583studio.domain.service

import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import kotlin.random.Random

class SimulatedResponse(
    val dataRequest: Pair<ByteArray?, Iso8583Data?>?,
    val config: GatewayConfig
) {
    private var isoData: Iso8583Data? = null

    init {

        val transaction = config.simulatedTransactions.firstOrNull {
            it.mti == dataRequest?.second?.messageType && it.proCode == dataRequest.second?.getValue(
                3
            )
        }
        transaction?.let {
            val response = Iso8583Data(
                template = transaction.fields!!.toTypedArray(),
                config = config
            )
            response.messageType =
                dataRequest?.second?.messageType?.toIntOrNull()?.plus(10)!!.toString()

            isoData = response
        }

    }

    fun pack(): ByteArray? {
        return isoData?.pack()
    }
}