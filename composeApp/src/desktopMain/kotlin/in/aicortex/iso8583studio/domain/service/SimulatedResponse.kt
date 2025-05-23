package `in`.aicortex.iso8583studio.domain.service

import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.Transaction

class SimulatedResponse(
    val dataRequest: Pair<ByteArray?, Iso8583Data?>?,
    val config: GatewayConfig
) {
    private var isoData: Iso8583Data? = null

    init {

        val transaction = config.simulatedTransactions.firstOrNull {
            it.mti == dataRequest?.second?.messageType && (
                    it.proCode == dataRequest.second?.getValue(
                        3
                    ) || it.proCode == "*"
                    )
        }?.copy()
        transaction?.let {
            val processedTransaction = PlaceholderProcessor.processPlaceholders(it.fields!!.toTypedArray(), dataRequest?.second?.bitAttributes)
            val response = Iso8583Data(
                template = processedTransaction,
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