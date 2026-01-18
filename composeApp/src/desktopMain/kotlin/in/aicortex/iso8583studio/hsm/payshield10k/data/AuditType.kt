package `in`.aicortex.iso8583studio.hsm.payshield10k.data

enum class AuditType {
    USER_ACTION,
    HOST_COMMAND,
    CONSOLE_COMMAND,
    ERROR_RESPONSE,
    SELF_TEST,
    KEY_OPERATION,
    AUTHORIZATION,
    CONFIGURATION_CHANGE
}
