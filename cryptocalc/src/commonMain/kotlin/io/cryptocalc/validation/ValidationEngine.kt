package io.cryptocalc.validation

class ValidationEngine {

    fun validateHex(value: String): Boolean =
        value.matches(Regex("^[0-9A-Fa-f]*$"))

    fun validateBase64(value: String): Boolean =
        value.matches(Regex("^[A-Za-z0-9+/]*={0,2}$"))

    fun validatePAN(pan: String): Boolean {
        // Basic PAN validation (Luhn algorithm)
        if (!pan.matches(Regex("^[0-9]{13,19}$"))) return false

        var sum = 0
        var alternate = false

        for (i in pan.length - 1 downTo 0) {
            var digit = pan[i].digitToInt()

            if (alternate) {
                digit *= 2
                if (digit > 9) digit = digit / 10 + digit % 10
            }

            sum += digit
            alternate = !alternate
        }

        return sum % 10 == 0
    }

    fun validateKeyLength(key: ByteArray, algorithm: String): Boolean {
        return when (algorithm.uppercase()) {
            "AES" -> key.size in listOf(16, 24, 32)
            "DES" -> key.size == 8
            "3DES" -> key.size in listOf(16, 24)
            else -> true
        }
    }
}
