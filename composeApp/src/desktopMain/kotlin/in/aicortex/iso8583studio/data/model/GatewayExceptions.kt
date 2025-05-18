package `in`.aicortex.iso8583studio.data.model

/**
 * Exception class for verification errors
 */
class VerificationException(message: String, val error: VerificationError) : Exception(message)
class UnauthorizedAccessException(message: String, val error: VerificationError) : Exception(message)
