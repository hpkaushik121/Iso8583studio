package `in`.aicortex.iso8583studio.hsm.payshield10k.data

/**
 * HSM Error Codes
 */
object HsmErrorCodes {
    const val SUCCESS = "00"
    const val VERIFICATION_FAILURE = "01"
    const val KEY_CHECK_VALUE_FAILURE = "02"
    const val ALGORITHM_NOT_SUPPORTED = "04"
    const val PIN_BLOCK_LENGTH_ERROR = "10"
    const val INVALID_INPUT_DATA = "15"
    const val MESSAGE_LENGTH_ERROR = "17"
    const val PIN_LENGTH_ERROR = "23"
    const val INVALID_LMK_IDENTIFIER = "26"
    const val LMK_CHECK_VALUE_FAILURE = "27"
    const val AUTHORIZATION_REQUIRED = "39"
    const val INVALID_LMK_TYPE = "42"
    const val COMMAND_DISABLED = "68"
    const val DATA_PARITY_ERROR = "74"
    const val INVALID_MESSAGE_LENGTH = "75"
    const val INVALID_COMMAND_FORMAT = "80"
    const val FUNCTION_NOT_PERMITTED = "82"
    const val CONSOLE_NOT_AUTHORIZED = "A1"
    const val HSM_NOT_IN_AUTHORIZED_STATE = "A2"
    const val COMMAND_ONLY_ALLOWED_IN_SECURE_STATE = "A3"
    const val INVALID_KEY_TYPE_CODE = "A4"
    const val INVALID_KEY_SCHEME = "B1"
    const val LMK_NOT_LOADED = "C1"
}
