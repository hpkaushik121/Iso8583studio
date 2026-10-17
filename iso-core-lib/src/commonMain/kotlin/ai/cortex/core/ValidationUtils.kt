package ai.cortex.core



enum class ValidationState {
    VALID, WARNING, ERROR, EMPTY
}

data class ValidationResult(
    val state: ValidationState,
    val message: String = "",
    val helperText: String = ""
){
    fun isValid(): Boolean{
        return true
    }
}

object ValidationUtils {

    fun validateHexString(
        value: String,
        expectedLength: Int? = null,
        allowEmpty: Boolean = false
    ): ValidationResult {
        if (value.isEmpty()) {
            return if (allowEmpty) {
                ValidationResult(ValidationState.EMPTY, "", "Enter hex characters")
            } else {
                ValidationResult(ValidationState.ERROR, "Field is required", "Enter hex characters")
            }
        }

        if (!value.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
            return ValidationResult(
                ValidationState.ERROR,
                "Only hex characters (0-9, A-F) allowed",
                "${value.length} characters"
            )
        }

        if (value.length % 2 != 0) {
            return ValidationResult(
                ValidationState.ERROR,
                "Hex string must have even number of characters",
                "${value.length} characters (needs even number)"
            )
        }

        expectedLength?.let { expected ->
            return when {
                value.length < expected -> ValidationResult(
                    ValidationState.ERROR,
                    "Must be exactly $expected characters",
                    "${value.length}/$expected characters"
                )

                value.length > expected -> ValidationResult(
                    ValidationState.ERROR,
                    "Must be exactly $expected characters",
                    "${value.length}/$expected characters (too long)"
                )

                else -> ValidationResult(
                    ValidationState.VALID,
                    "",
                    "${value.length}/$expected characters"
                )
            }
        }

        return ValidationResult(
            ValidationState.VALID,
            "",
            "${value.length} characters"
        )
    }

    fun validatePAN(pan: String): ValidationResult {
        if (pan.isEmpty()) {
            return ValidationResult(ValidationState.ERROR, "PAN is required", "Enter 13-19 digits")
        }

        if (!pan.all { it.isDigit() }) {
            return ValidationResult(
                ValidationState.ERROR,
                "PAN must contain only digits",
                "${pan.length} characters"
            )
        }

        if (pan.length !in 13..19) {
            return ValidationResult(
                ValidationState.ERROR,
                "PAN must be 13-19 digits",
                "${pan.length}/19 digits"
            )
        }

        // Luhn check
        val isLuhnValid = validateLuhn(pan)
        return if (isLuhnValid) {
            ValidationResult(
                ValidationState.VALID,
                "",
                "${pan.length}/19 digits - Luhn valid ✓"
            )
        } else {
            ValidationResult(
                ValidationState.WARNING,
                "Luhn checksum validation failed",
                "${pan.length}/19 digits - Luhn invalid ⚠"
            )
        }
    }

    fun validateNumericString(
        value: String,
        expectedLength: Int? = null,
        allowEmpty: Boolean = false
    ): ValidationResult {
        if (value.isEmpty()) {
            return if (allowEmpty) {
                ValidationResult(ValidationState.EMPTY, "", "Enter digits")
            } else {
                ValidationResult(
                    ValidationState.ERROR,
                    "Field is required",
                    "Enter digits"
                )
            }
        }

        if (!value.all { it.isDigit() }) {
            return ValidationResult(
                ValidationState.ERROR,
                "Only digits allowed",
                "${value.length} characters"
            )
        }

        expectedLength?.let {
            return when {
                value.length < expectedLength -> ValidationResult(
                    ValidationState.ERROR,
                    "Must be exactly $expectedLength digits",
                    "${value.length}/$expectedLength digits"
                )

                value.length > expectedLength -> ValidationResult(
                    ValidationState.ERROR,
                    "Must be exactly $expectedLength digits",
                    "${value.length}/$expectedLength digits (too long)"
                )

                else -> ValidationResult(
                    ValidationState.VALID,
                    "",
                    "${value.length}/$expectedLength digits"
                )
            }
        }

        return ValidationResult(
            ValidationState.VALID,
            "",
            "${value.length} digits"
        )
    }

    fun validateAlphanumeric(
        value: String,
        expectedLength: Int,
        allowEmpty: Boolean = false
    ): ValidationResult {
        if (value.isEmpty()) {
            return if (allowEmpty) {
                ValidationResult(ValidationState.EMPTY, "", "Enter $expectedLength characters")
            } else {
                ValidationResult(
                    ValidationState.ERROR,
                    "Field is required",
                    "Enter $expectedLength characters"
                )
            }
        }

        if (!value.all { it.isLetterOrDigit() }) {
            return ValidationResult(
                ValidationState.ERROR,
                "Only letters and digits allowed",
                "${value.length} characters"
            )
        }

        return when {
            value.length < expectedLength -> ValidationResult(
                ValidationState.ERROR,
                "Must be exactly $expectedLength characters",
                "${value.length}/$expectedLength characters"
            )

            value.length > expectedLength -> ValidationResult(
                ValidationState.ERROR,
                "Must be exactly $expectedLength characters",
                "${value.length}/$expectedLength characters (too long)"
            )

            else -> ValidationResult(
                ValidationState.VALID,
                "",
                "${value.length}/$expectedLength characters"
            )
        }
    }

    private fun validateLuhn(number: String): Boolean {
        if (number.length < 2) return false

        val digits = number.map { it.digitToInt() }
        var sum = 0
        var isSecond = false

        for (i in digits.size - 1 downTo 0) {
            var digit = digits[i]

            if (isSecond) {
                digit *= 2
                if (digit > 9) {
                    digit = digit / 10 + digit % 10
                }
            }

            sum += digit
            isSecond = !isSecond
        }

        return sum % 10 == 0
    }
}