
package `in`.aicortex.iso8583studio.domain.utils

import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.Transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random
/**
 * Processes special placeholders in ISO8583 transaction fields
 * Supports [SV], [TIME], and [RAND] placeholders as defined in the FieldInformationDialog
 */
object PlaceholderProcessor {

    val holdersList = listOf("[SV]", "[TIME]", "[RAND]")

    /**
     * Processes all placeholders in transaction fields
     *
     * @param transaction The transaction with fields to process
     * @param requestTransaction Optional request transaction for [SV] placeholder resolution
     * @return Updated transaction with processed placeholder values
     */
    fun processPlaceholders(
        transaction: Transaction,
        requestTransaction: Array<BitAttribute>? = null
    ): Transaction {
        val fields = transaction.fields!!

        fields.forEachIndexed { index, field ->
            if (field.isSet && field.data != null) {
                val originalValue = String(field.data!!)
                val processedValue = processFieldValue(
                    originalValue = originalValue,
                    fieldIndex = index,
                    maxLength = field.maxLength,
                    requestTransaction = requestTransaction?.get(index)
                )

                // Update field data if value was changed
                if (processedValue != originalValue) {
                    field.data = processedValue.toByteArray()
                }
            }
        }

        return transaction
    }

    /**
     * Processes a single field value for placeholders
     */
    private fun processFieldValue(
        originalValue: String,
        fieldIndex: Int,
        maxLength: Int,
        requestTransaction: BitAttribute?
    ): String {
        return when {
            originalValue.equals("[SV]", ignoreCase = true) -> {
                processSourceValue(fieldIndex, requestTransaction)
            }
            originalValue.equals("[TIME]", ignoreCase = true) -> {
                processTimeValue(maxLength)
            }
            originalValue.equals("[RAND]", ignoreCase = true) -> {
                processRandomValue(maxLength)
            }
            else -> originalValue // No placeholder found, return original value
        }
    }

    /**
     * Processes [SV] placeholder - copies value from request transaction
     */
    private fun processSourceValue(
        fieldIndex: Int,
        requestTransaction: BitAttribute?
    ): String {
        if (requestTransaction?.data == null) {
            return "[SV]" // Return original if no request available
        }

        val requestFields = requestTransaction.data

        // Check if the corresponding field exists and is set in the request
        if (fieldIndex < requestFields!!.size && requestTransaction.isSet) {
            val requestFieldData = requestFields
            return  String(requestFieldData)
        }

        return "[SV]" // Return original if field not found in request
    }

    /**
     * Processes [TIME] placeholder - generates current time based on field length
     */
    private fun processTimeValue(maxLength: Int): String {
        val now = LocalDateTime.now()

        return when (maxLength) {
            4 -> {
                // MMDD format
                now.format(DateTimeFormatter.ofPattern("MMdd"))
            }
            6 -> {
                // HHMMSS format
                now.format(DateTimeFormatter.ofPattern("HHmmss"))
            }
            8 -> {
                // MMDDHHSS format
                now.format(DateTimeFormatter.ofPattern("MMddHHss"))
            }
            10 -> {
                // MMDDHHMISS format
                now.format(DateTimeFormatter.ofPattern("MMddHHmmss"))
            }
            12 -> {
                // YYMMDDHHMMSS format
                now.format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
            }
            14 -> {
                // YYYYMMDDHHMMSS format
                now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            }
            else -> {
                // For other lengths, use a default format and pad/truncate as needed
                val defaultTime = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                when {
                    maxLength > defaultTime.length -> defaultTime.padEnd(maxLength, '0')
                    maxLength < defaultTime.length -> defaultTime.take(maxLength)
                    else -> defaultTime
                }
            }
        }
    }

    /**
     * Processes [RAND] placeholder - generates random number based on field length
     */
    private fun processRandomValue(maxLength: Int): String {
        if (maxLength <= 0) return ""

        // Generate random number with the specified length
        val minValue = if (maxLength == 1) 0 else 10.0.pow(maxLength - 1).toInt()
        val maxValue = 10.0.pow(maxLength).toInt() - 1

        val randomNumber = Random.nextInt(minValue, maxValue + 1)

        // Ensure the number is padded to the correct length
        return randomNumber.toString().padStart(maxLength, '0')
    }
}

/**
 * Extension function for easy access to power operation
 */
private fun Double.pow(exponent: Int): Double {
    return Math.pow(this, exponent.toDouble())
}