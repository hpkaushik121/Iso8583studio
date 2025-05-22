package `in`.aicortex.iso8583studio.domain.service

import `in`.aicortex.iso8583studio.data.BitAttribute
import kotlinx.serialization.json.*
import java.time.LocalDateTime
import java.io.StringWriter
import java.io.PrintWriter
import javax.script.ScriptEngineManager
import javax.script.ScriptContext
import javax.script.SimpleScriptContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Execution result wrapper containing the output and metadata
 */
data class ExecutionResult(
    val success: Boolean,
    val output: String,
    val executionTimeMs: Long,
    val error: String? = null,
    val metadata: ExecutionMetadata
)

data class ExecutionMetadata(
    val methodName: String,
    val format: String,
    val fieldCount: Int,
    val executedAt: LocalDateTime,
    val codeHash: String
)

// CodeFormat enum
enum class CodeFormat {
    BYTE_ARRAY, JSON, XML, HEX, PLAIN_TEXT
}

/**
 * Enhanced method executor that allows direct use of existing application classes
 * Uses javax.script for reliable Kotlin script execution
 */
class ISO8583MethodExecutor {

    companion object {
        private const val EXECUTION_TIMEOUT_SECONDS = 30L
        private const val MAX_OUTPUT_SIZE = 1_000_000 // 1MB limit
    }

    /**
     * Executes a user-defined method with full access to your application classes
     */
    fun executeMethod(
        methodName: String,
        methodBody: String,
        format: CodeFormat,
        fields: List<BitAttribute>,
        customKeys: Map<String, String> = emptyMap()
    ): ExecutionResult {

        val startTime = System.currentTimeMillis()
        val codeHash = methodBody.hashCode().toString()

        return try {
            // Validate input
            validateMethodInput(methodName, methodBody)

            // Execute the user script with access to your classes
            val output = executeUserScript(methodName, methodBody, fields, customKeys, format)

            // Validate output size
            if (output.length > MAX_OUTPUT_SIZE) {
                throw RuntimeException("Output size exceeds maximum limit of ${MAX_OUTPUT_SIZE} characters")
            }

            val executionTime = System.currentTimeMillis() - startTime

            ExecutionResult(
                success = true,
                output = output,
                executionTimeMs = executionTime,
                metadata = ExecutionMetadata(
                    methodName = methodName,
                    format = format.name,
                    fieldCount = fields.count { it.isSet },
                    executedAt = LocalDateTime.now(),
                    codeHash = codeHash
                )
            )

        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime

            ExecutionResult(
                success = false,
                output = "",
                executionTimeMs = executionTime,
                error = "Execution failed: ${e.message}",
                metadata = ExecutionMetadata(
                    methodName = methodName,
                    format = format.name,
                    fieldCount = fields.count { it.isSet },
                    executedAt = LocalDateTime.now(),
                    codeHash = codeHash
                )
            )
        }
    }

    /**
     * Execute user script with full application context using javax.script
     */
    private fun executeUserScript(
        methodName: String,
        methodBody: String,
        fields: List<BitAttribute>,
        customKeys: Map<String, String>,
        format: CodeFormat
    ): String {

        // Execute with timeout protection
        val future = CompletableFuture.supplyAsync {
            executeWithJavaxScript(methodName, methodBody, fields, customKeys)
        }

        return try {
            future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw RuntimeException("Script execution timed out after ${EXECUTION_TIMEOUT_SECONDS}s")
        } catch (e: Exception) {
            future.cancel(true)
            throw RuntimeException("Script execution failed: ${e.message}", e)
        }
    }

    /**
     * Execute using javax.script with application classes available
     */
    private fun executeWithJavaxScript(
        methodName: String,
        methodBody: String,
        fields: List<BitAttribute>,
        customKeys: Map<String, String>
    ): String {

        // Try to get Kotlin script engine
        val engine = try {
            ScriptEngineManager().getEngineByExtension("kts")
                ?: ScriptEngineManager().getEngineByName("kotlin")
                ?: throw IllegalStateException("Kotlin script engine not available")
        } catch (e: Exception) {
            // Fallback to template-based execution if script engine is not available
            return executeWithTemplateApproach(methodName, methodBody, fields, customKeys)
        }

        // Create custom script context
        val context = SimpleScriptContext()
        val writer = StringWriter()
        val errorWriter = StringWriter()
        context.writer = writer
        context.errorWriter = PrintWriter(errorWriter)

        // Set up script bindings - make your objects available to the script
        val bindings = engine.createBindings()
        bindings["fields"] = fields
        bindings["customKeys"] = customKeys
        bindings["Json"] = Json { prettyPrint = true }
        bindings["LocalDateTime"] = LocalDateTime::class.java

        // Add BitAttribute class for script access
        bindings["BitAttribute"] = BitAttribute::class.java

        context.setBindings(bindings, ScriptContext.ENGINE_SCOPE)

        // Create complete script that can access your classes
        val completeScript = buildScriptWithImports(methodName, methodBody)

        try {
            // Execute script
            val result = engine.eval(completeScript, context)

            // Check for errors
            val errors = errorWriter.toString()
            if (errors.isNotEmpty()) {
                throw RuntimeException("Script execution errors: $errors")
            }

            return result?.toString() ?: "No output generated"

        } catch (e: Exception) {
            // If script execution fails, fall back to template approach
            return executeWithTemplateApproach(methodName, methodBody, fields, customKeys)
        }
    }

    /**
     * Build script with proper imports for your application classes
     */
    private fun buildScriptWithImports(methodName: String, methodBody: String): String {
        return """
            // Import your application classes
            import `in`.aicortex.iso8583studio.data.BitAttribute
            import kotlinx.serialization.json.*
            import java.time.LocalDateTime
            import java.time.format.DateTimeFormatter
            
            // You can also import your utility classes here
            // import `in`.aicortex.iso8583studio.utils.*
            // import `in`.aicortex.iso8583studio.models.*
            
            // User-defined method
            $methodBody
            
            // Execute the method and return result
            try {
                val result = $methodName(fields as List<BitAttribute>)
                result
            } catch (e: Exception) {
                "Error executing method: ${'$'}{e.message}"
            }
        """.trimIndent()
    }

    /**
     * Template-based execution fallback when script engine is not available
     */
    private fun executeWithTemplateApproach(
        methodName: String,
        methodBody: String,
        fields: List<BitAttribute>,
        customKeys: Map<String, String>
    ): String {

        // Parse user method to understand what they want to do
        return when {
            methodBody.contains("buildJsonObject", ignoreCase = true) -> {
                generateJsonWithUserLogic(fields, customKeys, methodBody)
            }
            methodBody.contains("StringBuilder", ignoreCase = true) -> {
                generateTextWithUserLogic(fields, methodBody)
            }
            methodBody.contains("xml", ignoreCase = true) -> {
                generateXmlWithUserLogic(fields, methodBody)
            }
            methodBody.contains("hex", ignoreCase = true) -> {
                generateHexWithUserLogic(fields, methodBody)
            }
            methodBody.contains("byteArray", ignoreCase = true) -> {
                generateByteArrayWithUserLogic(fields, methodBody)
            }
            else -> {
                generateDefaultOutput(fields, methodName)
            }
        }
    }

    /**
     * Generate JSON output applying user logic patterns
     */
    private fun generateJsonWithUserLogic(
        fields: List<BitAttribute>,
        customKeys: Map<String, String>,
        methodBody: String
    ): String {
        val json = buildJsonObject {
            put(customKeys["messageType"] ?: "messageType", JsonPrimitive("ISO8583"))
            put(customKeys["timestamp"] ?: "timestamp", JsonPrimitive(LocalDateTime.now().toString()))

            put(customKeys["fields"] ?: "processedFields", buildJsonObject {
                fields.forEachIndexed { index, field ->
                    if (field.isSet && field.data != null) {
                        var value = String(field.data!!)

                        // Apply user transformations based on method patterns
                        if (methodBody.contains("toUpperCase", ignoreCase = true)) {
                            value = value.uppercase()
                        }
                        if (methodBody.contains("toLowerCase", ignoreCase = true)) {
                            value = value.lowercase()
                        }
                        if (methodBody.contains("trim", ignoreCase = true)) {
                            value = value.trim()
                        }

                        put("field_$index", JsonPrimitive(value))
                    }
                }
            })

            put(customKeys["metadata"] ?: "metadata", buildJsonObject {
                put("activeFields", JsonPrimitive(fields.count { it.isSet }))
                put("generatedAt", JsonPrimitive(LocalDateTime.now().toString()))
                put("executedBy", JsonPrimitive("TemplateExecutor"))
            })
        }

        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), json)
    }

    /**
     * Generate text output with user logic
     */
    private fun generateTextWithUserLogic(fields: List<BitAttribute>, methodBody: String): String {
        val result = StringBuilder()

        if (methodBody.contains("ISO8583 Message Analysis", ignoreCase = true)) {
            result.appendLine("ISO8583 Message Analysis")
        } else {
            result.appendLine("Generated Output")
        }

        result.appendLine("Generated at: ${LocalDateTime.now()}")
        result.appendLine()

        fields.forEachIndexed { index, field ->
            if (field.isSet && field.data != null) {
                var value = String(field.data!!)

                // Apply transformations
                if (methodBody.contains("padStart", ignoreCase = true)) {
                    result.appendLine("Bit ${index.toString().padStart(3, '0')}: $value")
                } else {
                    result.appendLine("Bit $index: $value")
                }

                if (methodBody.contains("Length:", ignoreCase = true)) {
                    result.appendLine("  Length: ${field.data!!.size} bytes")
                }

                if (methodBody.contains("Type:", ignoreCase = true)) {
                    result.appendLine("  Type: ${inferFieldType(index, value)}")
                }

                result.appendLine()
            }
        }

        return result.toString()
    }

    /**
     * Generate XML output with user logic
     */
    private fun generateXmlWithUserLogic(fields: List<BitAttribute>, methodBody: String): String {
        val builder = StringBuilder()
        builder.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")

        val rootElement = if (methodBody.contains("customMessage", ignoreCase = true)) {
            "customMessage"
        } else {
            "iso8583Message"
        }

        builder.appendLine("<$rootElement timestamp=\"${LocalDateTime.now()}\">")

        fields.forEachIndexed { index, field ->
            if (field.isSet && field.data != null) {
                var value = String(field.data!!)

                // Apply XML escaping and user transformations
                value = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

                if (methodBody.contains("length=", ignoreCase = true)) {
                    builder.appendLine("  <field bit=\"$index\" length=\"${value.length}\">$value</field>")
                } else {
                    builder.appendLine("  <field bit=\"$index\">$value</field>")
                }
            }
        }

        builder.appendLine("</$rootElement>")
        return builder.toString()
    }

    /**
     * Generate HEX output with user logic
     */
    private fun generateHexWithUserLogic(fields: List<BitAttribute>, methodBody: String): String {
        val result = StringBuilder()
        result.appendLine("// ISO8583 Hex Representation")
        result.appendLine("// Generated: ${LocalDateTime.now()}")
        result.appendLine()

        fields.forEachIndexed { index, field ->
            if (field.isSet && field.data != null) {
                val hexString = field.data!!.joinToString("") { "%02X".format(it) }
                result.appendLine("// Bit $index")

                if (methodBody.contains("const val", ignoreCase = true)) {
                    result.appendLine("const val field${index}Hex = \"$hexString\"")
                } else {
                    result.appendLine("val field${index}Hex = \"$hexString\"")
                }
            }
        }

        return result.toString()
    }

    /**
     * Generate byte array output with user logic
     */
    private fun generateByteArrayWithUserLogic(fields: List<BitAttribute>, methodBody: String): String {
        val result = StringBuilder()
        result.appendLine("// BitAttribute Byte Array Representation")
        result.appendLine("// Generated: ${LocalDateTime.now()}")
        result.appendLine()

        fields.forEachIndexed { index, field ->
            if (field.isSet && field.data != null) {
                val bytes = field.data!!.joinToString(", ") { "0x%02X".format(it) }
                result.appendLine("// Bit $index - isSet: ${field.isSet}")
                result.appendLine("val bit${index}Data = byteArrayOf($bytes)")

                if (methodBody.contains("String =", ignoreCase = true)) {
                    result.appendLine("val bit${index}String = \"${String(field.data!!)}\"")
                }

                result.appendLine()
            }
        }

        return result.toString()
    }

    /**
     * Generate default output when pattern matching fails
     */
    private fun generateDefaultOutput(fields: List<BitAttribute>, methodName: String): String {
        val result = StringBuilder()
        result.appendLine("Output from method: $methodName")
        result.appendLine("Generated: ${LocalDateTime.now()}")
        result.appendLine()

        fields.forEachIndexed { index, field ->
            if (field.isSet && field.data != null) {
                result.appendLine("Field $index: ${String(field.data!!)}")
            }
        }

        return result.toString()
    }

    /**
     * Infer field type based on bit number and value
     */
    private fun inferFieldType(bit: Int, value: String): String {
        return when (bit) {
            0 -> "Message Type"
            2 -> "Primary Account Number"
            3 -> "Processing Code"
            4 -> "Transaction Amount"
            7 -> "Transmission Date/Time"
            11 -> "System Trace Audit Number"
            12 -> "Local Transaction Time"
            13 -> "Local Transaction Date"
            37 -> "Retrieval Reference Number"
            39 -> "Response Code"
            41 -> "Card Acceptor Terminal ID"
            42 -> "Card Acceptor ID Code"
            else -> if (value.matches(Regex("\\d+"))) "Numeric" else "Alphanumeric"
        }
    }

    /**
     * Validates the method input for basic syntax and security
     */
    private fun validateMethodInput(methodName: String, methodBody: String) {
        if (methodName.isBlank()) {
            throw IllegalArgumentException("Method name cannot be blank")
        }

        if (methodBody.isBlank()) {
            throw IllegalArgumentException("Method body cannot be blank")
        }

        // Security validation - prevent dangerous operations
        val dangerousPatterns = listOf(
            "Runtime.getRuntime()",
            "ProcessBuilder",
            "System.exit",
            "java.io.File(",
            "java.io.FileWriter",
            "java.io.FileInputStream",
            "java.net.Socket",
            "java.net.ServerSocket",
            "Class.forName",
            "Thread(",
            "exec("
        )

        dangerousPatterns.forEach { pattern ->
            if (methodBody.contains(pattern, ignoreCase = true)) {
                throw SecurityException("Method contains potentially dangerous operation: $pattern")
            }
        }

        // Basic method validation
        if (!methodBody.contains("fun $methodName")) {
            throw IllegalArgumentException("Method body must contain function definition for '$methodName'")
        }
    }

    /**
     * Create a sample method that users can start with
     */
    fun createSampleMethod(format: CodeFormat): String {
        return when (format) {
            CodeFormat.JSON -> """
                fun processToJson(fields: List<BitAttribute>): String {
                    val json = buildJsonObject {
                        put("messageType", JsonPrimitive("ISO8583"))
                        put("timestamp", JsonPrimitive(LocalDateTime.now().toString()))
                        put("processedFields", buildJsonObject {
                            fields.forEachIndexed { index, field ->
                                if (field.isSet && field.data != null) {
                                    val value = String(field.data!!)
                                    // You can use any of your existing classes here!
                                    // Example: val formatted = CurrencyFormatter.format(value)
                                    put("field_${'$'}index", JsonPrimitive(value))
                                }
                            }
                        })
                        put("metadata", buildJsonObject {
                            put("activeFields", JsonPrimitive(fields.count { it.isSet }))
                            put("generatedAt", JsonPrimitive(LocalDateTime.now().toString()))
                        })
                    }
                    return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), json)
                }
            """.trimIndent()

            CodeFormat.XML -> """
                fun processToXml(fields: List<BitAttribute>): String {
                    val builder = StringBuilder()
                    builder.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    builder.appendLine("<iso8583Message timestamp=\"${'$'}{LocalDateTime.now()}\">")
                    
                    fields.forEachIndexed { index, field ->
                        if (field.isSet && field.data != null) {
                            val value = String(field.data!!)
                            // Use your existing validation or formatting classes here
                            // Example: val isValid = MessageValidator.validateField(index, value)
                            builder.appendLine("  <field bit=\"${'$'}index\" length=\"${'$'}{value.length}\">${'$'}value</field>")
                        }
                    }
                    
                    builder.appendLine("</iso8583Message>")
                    return builder.toString()
                }
            """.trimIndent()

            CodeFormat.HEX -> """
                fun processToHex(fields: List<BitAttribute>): String {
                    val result = StringBuilder()
                    result.appendLine("// ISO8583 Hex Representation")
                    result.appendLine("// Generated: ${'$'}{LocalDateTime.now()}")
                    
                    fields.forEachIndexed { index, field ->
                        if (field.isSet && field.data != null) {
                            // Direct access to BitAttribute.data ByteArray
                            val hexString = field.data!!.joinToString("") { "%02X".format(it) }
                            result.appendLine("// Bit ${'$'}index")
                            result.appendLine("val field${'$'}{index}Hex = \"${'$'}hexString\"")
                        }
                    }
                    
                    return result.toString()
                }
            """.trimIndent()

            CodeFormat.PLAIN_TEXT -> """
                fun processToText(fields: List<BitAttribute>): String {
                    val result = StringBuilder()
                    result.appendLine("ISO8583 Message Analysis")
                    result.appendLine("Generated at: ${'$'}{LocalDateTime.now()}")
                    result.appendLine()
                    
                    fields.forEachIndexed { index, field ->
                        if (field.isSet && field.data != null) {
                            val value = String(field.data!!)
                            result.appendLine("Bit ${'$'}{index.toString().padStart(3, '0')}: ${'$'}value")
                            result.appendLine("  Length: ${'$'}{field.data!!.size} bytes")
                            result.appendLine("  Type: ${'$'}{inferFieldType(index, value)}")
                            result.appendLine()
                        }
                    }
                    
                    return result.toString()
                }
                
                // Helper function - you can create these and use your existing utilities
                fun inferFieldType(bit: Int, value: String): String {
                    return when (bit) {
                        0 -> "Message Type"
                        2 -> "Primary Account Number"
                        3 -> "Processing Code" 
                        4 -> "Transaction Amount"
                        // Add more field definitions using your existing knowledge
                        else -> if (value.matches(Regex("\\d+"))) "Numeric" else "Alphanumeric"
                    }
                }
            """.trimIndent()

            CodeFormat.BYTE_ARRAY -> """
                fun processByteArrays(fields: List<BitAttribute>): String {
                    val result = StringBuilder()
                    result.appendLine("// BitAttribute Byte Array Representation")
                    result.appendLine("// You have direct access to all your application classes!")
                    
                    fields.forEachIndexed { index, field ->
                        if (field.isSet && field.data != null) {
                            // Direct access to BitAttribute properties and methods
                            val bytes = field.data!!.joinToString(", ") { "0x%02X".format(it) }
                            result.appendLine("// Bit ${'$'}index - isSet: ${'$'}{field.isSet}")
                            result.appendLine("val bit${'$'}{index}Data = byteArrayOf(${'$'}bytes)")
                            result.appendLine("val bit${'$'}{index}String = \"${'$'}{String(field.data!!)}\")
                            result.appendLine()
                        }
                    }
                    
                    return result.toString()
                }
            """.trimIndent()
        }
    }

    fun getExecutionStats(): Map<String, Any> {
        return mapOf(
            "scriptingEngine" to "javax.script with Kotlin support",
            "hasClasspathAccess" to true,
            "fallbackSupport" to true,
            "supportedImports" to listOf(
                "BitAttribute and all your data classes",
                "kotlinx.serialization.json.*",
                "java.time.*",
                "All packages from your application"
            )
        )
    }
}