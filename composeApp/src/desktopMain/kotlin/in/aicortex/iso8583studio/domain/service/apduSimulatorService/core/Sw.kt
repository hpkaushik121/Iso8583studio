package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.core

/**
 * ISO 7816-4 / EMV status words. Values are unsigned 16-bit; stored as Int for ergonomics.
 */
object Sw {
    const val SUCCESS = 0x9000

    // 61xx / 6Cxx returned by handlers signal length-correction; runtime materialises them.
    const val BYTES_REMAINING_00 = 0x6100
    const val WRONG_LENGTH_LE_00 = 0x6C00

    const val WARN_NV_UNCHANGED = 0x6200
    const val WARN_NV_CHANGED = 0x6300

    const val EXEC_ERROR = 0x6400
    const val MEMORY_FAILURE = 0x6581

    const val WRONG_LENGTH = 0x6700
    const val LOGICAL_CHANNEL_NOT_SUPPORTED = 0x6881
    const val SECURE_MESSAGING_NOT_SUPPORTED = 0x6882

    const val COMMAND_NOT_ALLOWED = 0x6900
    const val CMD_INCOMPATIBLE_FILE_STRUCTURE = 0x6981
    const val SECURITY_NOT_SATISFIED = 0x6982
    const val AUTH_METHOD_BLOCKED = 0x6983
    const val REF_DATA_INVALIDATED = 0x6984
    const val CONDITIONS_NOT_SATISFIED = 0x6985
    const val COMMAND_NOT_ALLOWED_NO_EF = 0x6986

    const val WRONG_PARAMS = 0x6A00
    const val WRONG_DATA = 0x6A80
    const val FUNC_NOT_SUPPORTED = 0x6A81
    const val FILE_NOT_FOUND = 0x6A82
    const val RECORD_NOT_FOUND = 0x6A83
    const val INCORRECT_P1P2 = 0x6A86
    const val LC_INCONSISTENT_WITH_P1P2 = 0x6A87
    const val REFERENCED_DATA_NOT_FOUND = 0x6A88

    const val WRONG_P1P2 = 0x6B00
    const val INS_NOT_SUPPORTED = 0x6D00
    const val CLA_NOT_SUPPORTED = 0x6E00
    const val UNKNOWN = 0x6F00

    /** PIN VERIFY remaining tries: 0x63Cx where x = remaining tries (0..15). */
    fun pinTriesRemaining(tries: Int): Int {
        require(tries in 0..0xF) { "PIN tries 0..15, was $tries" }
        return 0x63C0 or tries
    }
}
