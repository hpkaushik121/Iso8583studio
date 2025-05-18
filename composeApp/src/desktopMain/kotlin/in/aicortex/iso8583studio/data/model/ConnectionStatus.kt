package `in`.aicortex.iso8583studio.data.model

import org.springframework.context.annotation.Description

enum class ConnectionStatus {
    WAITING,
    STARTED,
    STOPPED,
    CONNECTED,
    UNKNOWN,
    DISCONNECTED,
    ERROR;

    private var _message: String? = null
    private var _description: String? = null
    private var _updateTime: Long = System.currentTimeMillis()

    val message: String?
        get() = _message

    val description: String?
        get() = _description

    val updateTime: Long?
        get() = _updateTime

    fun withError(message: String?, description: String?): ConnectionStatus {
        if (this != ERROR) {
            throw IllegalStateException("withErrorDetails can only be called on ERROR status")
        }
        this._message = message
        this._description = description
        this._updateTime = System.currentTimeMillis()
        return this
    }

    fun withException(message: String?, description: String?): ConnectionStatus {
        this._message = message
        this._description = description
        this._updateTime = System.currentTimeMillis()
        return this
    }

}
