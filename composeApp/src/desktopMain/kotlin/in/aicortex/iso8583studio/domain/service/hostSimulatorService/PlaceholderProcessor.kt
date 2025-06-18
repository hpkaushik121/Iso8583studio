package `in`.aicortex.iso8583studio.domain.service.hostSimulatorService

import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.clone
import `in`.aicortex.iso8583studio.data.getValue
import `in`.aicortex.iso8583studio.data.updateBit
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.ResponseField
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
     * Processes placeholders in transaction fields
     * @param responseFields List of ResponseField containing transaction fields to process
     * @param requestTransaction Optional array of BitAttribute representing request transaction for [SV] placeholder
     * @return Array of BitAttribute with processed placeholders
     */
    fun processPlaceholders(
        responseFields: List<ResponseField>,
        requestTransaction: MutableMap<String?, String?>? = null
    ): MutableMap<String?, String?> {

        val processedFields = mutableMapOf<String?, String?>()

        responseFields.forEachIndexed { index, field ->
            // Create a deep copy of the field
            val requestField = requestTransaction?.get(field.targetKey)
            val processedValue = processFieldValue(source = requestField ?: "<Error: No value>",
                originalValue = field.value)
            processedFields.put(field.targetKey, processedValue ?: field.value)
        }

        return processedFields
    }

    /**
     * Alternative method that explicitly takes and returns copies
     */
    fun processPlaceholders(
        transactionFields: Array<BitAttribute>,
        requestTransaction: Array<BitAttribute>? = null
    ): Array<BitAttribute> {

        return transactionFields.mapIndexed { index, field ->
            // Create a deep copy
            val fieldCopy = clone(field)
            val requestField = requestTransaction?.getOrNull(index)

            if (fieldCopy.isSet && fieldCopy.data != null) {
                val originalValue = fieldCopy.getValue()!!
                val processedValue = processFieldValue(
                    originalValue = originalValue,
                    fieldIndex = index,
                    maxLength = fieldCopy.maxLength,
                    requestTransaction = requestField
                )

                processedValue?.let { fieldCopy.updateBit(it,processedValue.length) } ?: run {
                    fieldCopy.isSet = false
                    fieldCopy.data = null
                }
            } else {
                fieldCopy.isSet = false
                fieldCopy.data = null
            }
            fieldCopy
        }.toTypedArray()
    }

    /**
     * Processes a single field value for placeholders
     */
    private fun processFieldValue(
        source: String,
        originalValue: String,
    ): String? {
        return when {
            originalValue.equals("[SV]", ignoreCase = true) -> {
                source
            }
            originalValue.equals("[TIME]", ignoreCase = true) -> {
                processTimeValue(6)
            }
            originalValue.equals("[RAND]", ignoreCase = true) -> {
                processRandomValue(12)
            }
            else -> originalValue // No placeholder found, return original value
        }
    }


    /**
     * Processes a single field value for placeholders
     */
    private fun processFieldValue(
        originalValue: String,
        fieldIndex: Int,
        maxLength: Int,
        requestTransaction: BitAttribute?
    ): String? {
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
    ): String? {
        if (requestTransaction?.data == null) {
            return null // Return original if no request available
        }


        // Check if the corresponding field exists and is set in the request
        if (requestTransaction.isSet) {
            return requestTransaction.getValue()
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

        // Handle single digit case
        if (maxLength == 1) {
            return Random.Default.nextInt(0, 10).toString()
        }

        // For multi-digit numbers, generate each digit individually to avoid overflow
        val result = StringBuilder()

        // First digit (1-9, can't be 0 for multi-digit numbers)
        result.append(Random.Default.nextInt(1, 10))

        // Remaining digits (0-9)
        repeat(maxLength - 1) {
            result.append(Random.Default.nextInt(0, 10))
        }

        return result.toString()
    }
}