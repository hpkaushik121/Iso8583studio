package `in`.aicortex.iso8583studio.hsm.payshield10k.data


/**
 * Command result wrapper
 */
sealed class HsmCommandResult {
    data class Success(val response: String, val data: Map<String, Any> = emptyMap()) : HsmCommandResult()
    data class Error(val errorCode: String, val message: String) : HsmCommandResult()
}