package `in`.aicortex.iso8583studio.domain.service.hostSimulatorService

import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.HttpMethod
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.MatchOperator
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.Transaction

class SimulatedResponse(
    val dataRequest: Iso8583Data?,
    val config: GatewayConfig,
    val isFirst: Boolean
) {
    private var isoData: Iso8583Data? = null

    init {

        val transaction = getMatchedSimulatedTransaction()
        transaction?.let {
            val processedTransaction = PlaceholderProcessor.processPlaceholders(
                it.fields!!.toTypedArray(),
                dataRequest?.bitAttributes
            )
            val response = Iso8583Data(
                template = processedTransaction,
                config = config,
                isFirst = isFirst
            )
            response.httpInfo = dataRequest?.httpInfo
            if(it.responseMapping.enabled){
                response.otherKAttributes = PlaceholderProcessor.processPlaceholders(
                    requestTransaction = dataRequest?.otherKAttributes,
                    responseFields = it.responseMapping.responseFields
                )
            }
            response.messageType =
                dataRequest?.messageType?.toIntOrNull()?.plus(10)?.toString() ?: ""
            isoData = response
        }

    }

    private fun getMatchedSimulatedTransaction(): Transaction? {
        return config.getSimulatedTransaction(isFirst).firstOrNull {
            (if (it.restApiMatching.enabled) {
                if (it.restApiMatching.pathMatching.enabled) {
                    if (it.restApiMatching.pathMatching.exactMatch) {
                        dataRequest?.httpInfo?.path == it.restApiMatching.pathMatching.path
                                && dataRequest.httpInfo?.method == HttpMethod.valueOf(it.restApiMatching.pathMatching.method)
                                && dataRequest.httpInfo?.queryParams?.map { param ->
                                   param.value == it.restApiMatching.pathMatching.query?.get(param.key)
                        }?.any { it == false } == false
                    } else {
                        val pathMatching = it.restApiMatching.pathMatching.path.split("/")
                        dataRequest?.httpInfo?.path?.split("/")?.mapIndexed { i,requestPath ->
                            if( pathMatching.size > i) {
                                requestPath == pathMatching[i] || pathMatching[i] == "*"
                            } else {
                                false
                            }
                        }?.any{ it == false } == false && dataRequest.httpInfo?.method == HttpMethod.valueOf(it.restApiMatching.pathMatching.method)
                                && dataRequest.httpInfo?.queryParams?.map { param ->
                            param.value == it.restApiMatching.pathMatching.query?.get(param.key) || it.restApiMatching.pathMatching.query?.get(param.key) == "*"
                        }?.any { it == false } == false
                    }
                } else true && it.restApiMatching.keyValueMatching.map { matcher ->
                    if (matcher.key.startsWith("header")) {
                        dataRequest?.httpInfo?.headers?.filter { map ->
                            matcher.key == "header.${map.key}" && matchValues(
                                matcher.value,
                                map.value,
                                matcher.operator
                            )
                        }.isNullOrEmpty() == false
                    } else {
                        dataRequest?.otherKAttributes?.filter { map ->
                            matcher.key == map.key && matchValues(
                                matcher.value,
                                map.value ?: "",
                                matcher.operator
                            )
                        }.isNullOrEmpty() == false
                    }

                }.any { it == false } == false
            } else {
                it.mti == dataRequest?.messageType && (
                        it.proCode == dataRequest.getValue(
                            2
                        ) || it.proCode == "*"
                        )
            }) == true
        }?.copy()

    }

    private fun matchValues(
        s1: String,
        s2: String,
        operator: MatchOperator
    ) =
        when (operator) {
            MatchOperator.EQUALS -> s1 == s2
            MatchOperator.NOT_EQUALS -> s1 != s2
            MatchOperator.STARTS_WITH -> s2.startsWith(s1)
            MatchOperator.ENDS_WITH -> s2.endsWith(s1)
            MatchOperator.CONTAINS -> s2.contains(s1)
            MatchOperator.REGEX -> {
                val regex = Regex(s1)
                regex.matches(s2)
            }

            MatchOperator.GREATER_THAN -> {
                val value1 = s1.toDoubleOrNull()
                val value2 = s2.toDoubleOrNull()
                if (value1 != null && value2 != null) {
                    value2 > value1
                } else {
                    throw IllegalArgumentException("Cannot compare non-numeric values: $s1 and $s2")
                }
            }

            MatchOperator.LESS_THAN -> {
                val value1 = s1.toDoubleOrNull()
                val value2 = s2.toDoubleOrNull()
                if (value1 != null && value2 != null) {
                    value2 < value1
                } else {
                    throw IllegalArgumentException("Cannot compare non-numeric values: $s1 and $s2")
                }
            }
        }


    fun pack(): ByteArray? {
        return isoData?.pack()
    }
}