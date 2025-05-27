package `in`.aicortex.iso8583studio.domain.service

import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.model.GatewayConfig

class SimulatedResponse(
    val dataRequest: Iso8583Data?,
    val config: GatewayConfig,
    val isFirst: Boolean
) {
    private var isoData: Iso8583Data? = null

    init {

        val transaction = config.simulatedTransactionsToSource.firstOrNull {
            it.mti == dataRequest?.messageType && (
                    it.proCode == dataRequest.getValue(
                       2
                    ) || it.proCode == "*"
                    )
        }?.copy()
        transaction?.let {
            val processedTransaction = PlaceholderProcessor.processPlaceholders(it.fields!!.toTypedArray(), dataRequest?.bitAttributes)
            val response = Iso8583Data(
                template = processedTransaction,
                config = config,
                isFirst
            )
            response.messageType =
                dataRequest?.messageType?.toIntOrNull()?.plus(10)!!.toString()
            isoData = response
        }

    }

    fun pack(): ByteArray? {
        return isoData?.pack()
    }
}