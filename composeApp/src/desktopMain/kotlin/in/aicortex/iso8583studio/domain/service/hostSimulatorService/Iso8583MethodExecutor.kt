package `in`.aicortex.iso8583studio.domain.service.hostSimulatorService

import `in`.aicortex.iso8583studio.data.BitAttribute
import `in`.aicortex.iso8583studio.data.getValue
import `in`.aicortex.iso8583studio.data.model.CodeFormat
import java.time.LocalDateTime
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Execution result wrapper - COMPATIBLE with existing CodeEditorDialog
 */
data class ExecutionResult(
    val success: Boolean,
    val output: String,
    val executionTimeMs: Long,
    val error: String? = null,
    val metadata: ExecutionMetadata,
    val generatedFilePath: String? = null
)

data class ExecutionMetadata(
    val methodName: String,
    val format: String,
    val fieldCount: Int,
    val executedAt: LocalDateTime,
    val codeHash: String
)



/**
 * Progress callback interface for execution tracking
 */
interface ExecutionProgressCallback {
    fun onValidating(message: String = "Validating method syntax...")
    fun onGenerating(message: String = "Generating Kotlin script...")
    fun onCompiling(message: String = "Compiling with kotlinc...")
    fun onExecuting(message: String = "Running script with java -jar...")
    fun onCompleted(success: Boolean, message: String)
}

/**
 * ISO8583MethodExecutor - Using actual BitAttribute parsing
 * Direct compilation and execution using kotlinc and java -jar
 */
class ISO8583MethodExecutor {
    private var progressCallback: ExecutionProgressCallback? = null

    companion object {
        private const val EXECUTION_TIMEOUT_SECONDS = 30L
        private const val MAX_OUTPUT_SIZE = 1_000_000
        private const val TEMP_DIR = "temp_iso8583"
    }

    fun setListener(progressCallback: ExecutionProgressCallback? = null): ISO8583MethodExecutor {
        this.progressCallback = progressCallback
        return this
    }


    /**
     * MAIN METHOD - Run by compiling to jar and executing
     * Uses actual BitAttribute parsing logic
     */
    suspend fun executeMethod(
        methodName: String,
        methodBody: String,
        format: CodeFormat,
        fields: List<BitAttribute>, messageType: String, tpdu: String,
        customKeys: Map<String, String> = emptyMap(),
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val codeHash = methodBody.hashCode().toString()

        try {


            // Convert BitAttributes to field map using actual BitAttribute logic
            val fieldMap = convertBitAttributesToFieldMap(fields)

            // Validate user method
            validateUserMethod(methodName, methodBody)

            // Create temporary directory
            val tempDir = File(TEMP_DIR)
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }

            // Generate standalone Kotlin file using real BitAttribute data
            val kotlinFile = createStandaloneKotlinFileFromBitAttributes(
                methodName,
                methodBody,
                fields, messageType, tpdu,
                tempDir.absolutePath,
                format
            )

            // Compile and execute
            val output = compileAndExecuteKotlinFile(kotlinFile, methodName)

            val executionTime = System.currentTimeMillis() - startTime

            ExecutionResult(
                success = true,
                output = output,
                executionTimeMs = executionTime,
                metadata = ExecutionMetadata(
                    methodName = methodName,
                    format = format.name,
                    fieldCount = fieldMap.size,
                    executedAt = LocalDateTime.now(),
                    codeHash = codeHash
                ),
                generatedFilePath = kotlinFile.absolutePath
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
     * Generate standalone Kotlin file only (no execution)
     */
    fun generateStandaloneKotlinFile(
        methodName: String,
        methodBody: String,
        format: CodeFormat,
        fields: List<BitAttribute>, messageType: String, tpdu: String,
        outputDir: String = "generated",
        customKeys: Map<String, String> = emptyMap()
    ): ExecutionResult {

        val startTime = System.currentTimeMillis()
        val codeHash = methodBody.hashCode().toString()

        return try {
            val fieldMap = convertBitAttributesToFieldMap(fields)

            validateUserMethod(methodName, methodBody)

            val generatedFile = createStandaloneKotlinFileFromBitAttributes(
                methodName,
                methodBody,
                fields, messageType, tpdu,
                outputDir,
                format
            )

            val executionTime = System.currentTimeMillis() - startTime

            ExecutionResult(
                success = true,
                output = "Standalone Kotlin file generated successfully!\n\nFile: ${generatedFile.absolutePath}\n\nTo compile and run:\n1. kotlinc ${generatedFile.name} -include-runtime -d ${methodName}.jar\n2. java -jar ${methodName}.jar",
                executionTimeMs = executionTime,
                metadata = ExecutionMetadata(
                    methodName = methodName,
                    format = format.name,
                    fieldCount = fieldMap.size,
                    executedAt = LocalDateTime.now(),
                    codeHash = codeHash
                ),
                generatedFilePath = generatedFile.absolutePath
            )

        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime

            ExecutionResult(
                success = false,
                output = "",
                executionTimeMs = executionTime,
                error = "File generation failed: ${e.message}",
                metadata = ExecutionMetadata(
                    methodName = methodName,
                    format = format.name,
                    fieldCount = 0,
                    executedAt = LocalDateTime.now(),
                    codeHash = codeHash
                )
            )
        }
    }

    /**
     * Compile and execute Kotlin file using kotlinc and java -jar
     */
    private fun compileAndExecuteKotlinFile(kotlinFile: File, methodName: String): String {
        val jarFileName = "field__parser.jar"
        val jarFile = File(kotlinFile.parent, jarFileName)


        try {
            // Step 1: Compile Kotlin file to JAR
            progressCallback?.onCompiling("Compiling Kotlin file to JAR...")

            // Step 1: Compile Kotlin file to JAR
            val compileCommand = listOf(
                "kotlinc",
                kotlinFile.absolutePath,
                "-include-runtime",
                "-d",
                jarFile.absolutePath
            )

            val compileProcess = ProcessBuilder(compileCommand)
                .directory(kotlinFile.parentFile)
                .redirectErrorStream(true)
                .start()

            val compileOutput = compileProcess.inputStream.bufferedReader().readText()
            val compileExitCode = compileProcess.waitFor()

            if (compileExitCode != 0) {
                throw RuntimeException("Kotlin compilation failed (exit code: $compileExitCode):\n$compileOutput")
            }

            // Verify JAR file was created
            if (!jarFile.exists()) {
                throw RuntimeException("JAR file was not created: ${jarFile.absolutePath}")
            }
            // Step 2: Execute the JAR file
            progressCallback?.onExecuting("Executing JAR file...")

            // Step 2: Execute the JAR file
            val executeCommand = listOf("java", "-jar", jarFile.absolutePath)

            val executeProcess = ProcessBuilder(executeCommand)
                .directory(jarFile.parentFile)
                .start()

            // Set up timeout for execution
            val executionFuture = CompletableFuture.supplyAsync {
                val output = executeProcess.inputStream.bufferedReader().readText()
                val errorOutput = executeProcess.errorStream.bufferedReader().readText()
                val exitCode = executeProcess.waitFor()

                if (exitCode != 0) {
                    throw RuntimeException("Script execution failed (exit code: $exitCode):\n$errorOutput")
                }

                output
            }

            val output = try {
                executionFuture.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                executeProcess.destroyForcibly()
                throw RuntimeException("Script execution timed out after ${EXECUTION_TIMEOUT_SECONDS}s")
            }

            progressCallback?.onCompleted(true, "Execution completed successfully!")
            return output

        } catch (e: Exception) {
            progressCallback?.onCompleted(false, "Execution failed: ${e.message}")
            throw RuntimeException("Failed to compile/execute Kotlin script: ${e.message}", e)
        } finally {
            // Clean up generated files
            try {
                if (jarFile.exists()) {
                    jarFile.delete()
                }
                if (kotlinFile.exists()) {
                    kotlinFile.delete()
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    /**
     * Convert BitAttributes to Map<Int, String> using actual BitAttribute logic
     */
    private fun convertBitAttributesToFieldMap(fields: List<BitAttribute>): Map<Int, String> {
        val fieldMap = mutableMapOf<Int, String>()

        // Step 1: Validation
        progressCallback?.onValidating()

        fields.forEachIndexed { index, bitAttribute ->
            if (bitAttribute.isSet && bitAttribute.data != null) {
                try {
                    // Use the actual BitAttribute.getValue() method
                    val value = bitAttribute.getValue()
                    if (!value.isNullOrEmpty()) {
                        fieldMap[index] = value // Bit numbers are 1-based
                    }
                } catch (e: Exception) {
                    try {
                        // Fallback to getString() method
                        val value = bitAttribute.getString()
                        if (value.isNotEmpty()) {
                            fieldMap[index] = value
                        }
                    } catch (e2: Exception) {
                        // Last resort: convert raw bytes to string
                        fieldMap[index] = String(bitAttribute.data!!)
                    }
                }
            }
        }

        return fieldMap
    }

    /**
     * Create standalone Kotlin file using actual BitAttribute data
     */
    private fun createStandaloneKotlinFileFromBitAttributes(
        methodName: String,
        userMethodBody: String,
        fields: List<BitAttribute>,
        messageType: String,
        tpdu: String,
        outputDir: String,
        format: CodeFormat
    ): File {
        progressCallback?.onGenerating()

        val outputDirectory = File(outputDir)
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val fileName = "field__parser.kt"
        val file = File(outputDirectory, fileName)

        val kotlinCode = buildCompleteKotlinScriptFromBitAttributes(
            methodName,
            userMethodBody,
            fields,
            messageType,
            tpdu,
            format
        )

        file.writeText(kotlinCode)
        return file
    }

    /**
     * Build complete Kotlin script using actual BitAttribute data
     */
    private fun buildCompleteKotlinScriptFromBitAttributes(
        methodName: String,
        userMethodBody: String,
        fields: List<BitAttribute>,
        messageType: String,
        tpdu: String,
        format: CodeFormat
    ): String {

        // Create field data from actual BitAttributes
        val fieldDataEntries = mutableListOf<String>()

        fields.forEachIndexed { index, bitAttribute ->
            if (bitAttribute.isSet && bitAttribute.data != null) {
                try {
                    val value = bitAttribute.getValue() ?: bitAttribute.getString()
                    if (value.isNotEmpty()) {
                        // Escape quotes in the value
                        val escapedValue = value.replace("\"", "\\\"")
                        fieldDataEntries.add("        ${index} to \"$escapedValue\"")
                    }
                } catch (e: Exception) {
                    // Skip fields that can't be converted
                }
            }
        }

        val fieldDataString = if (fieldDataEntries.isNotEmpty()) {
            fieldDataEntries.joinToString(",\n")
        } else {
            "        1 to \"0200\",\n        2 to \"4111111111111111\",\n        4 to \"000000010000\",\n        11 to \"123456\""
        }

        return """
import java.time.LocalDateTime

// =====================================================
// FIELD DATA (FROM YOUR ACTUAL BITATTRIBUTES)
// =====================================================
val messageType ="$messageType"
val tpdu = "$tpdu"
/**
 * Field data extracted from your BitAttributes
 * This represents the actual ISO8583 message fields that were set
 */
fun getActualFieldData(): Map<Int, String> {
    return mapOf(
$fieldDataString
    )
}

// =====================================================
// USER METHOD AREA (WHAT USER WRITES)
// =====================================================

$userMethodBody

// =====================================================
// MAIN EXECUTION FUNCTION
// =====================================================

fun main() {
    println("ISO8583 Method Executor - Generated Script")
    println("=" + "=".repeat(60))
    println("Generated at: ${'$'}{LocalDateTime.now()}")
    println("Method: $methodName")
    println("Format: ${format.name}")
    println("=" + "=".repeat(60))
    println()
    
    // Get the actual field data from your BitAttributes
    val fields = getActualFieldData()
    
    println("Loaded field data from BitAttributes:")
    fields.toSortedMap().forEach { (bit, value) ->
        val displayValue = if (value.length > 50) {
            value.take(47) + "..."
        } else value
        println("  Bit ${'$'}{bit.toString().padStart(3, '0')}: ${'$'}displayValue")
    }
    println()
    println("Total fields: ${'$'}{fields.size}")
    println()
    
    // Execute user method
    println("Executing user method: $methodName")
    println("-".repeat(60))
    
    try {
        val startTime = System.currentTimeMillis()
        val result = $methodName(fields,messageType,tpdu)
        val executionTime = System.currentTimeMillis() - startTime
        
        println("✓ Execution successful (${'$'}executionTime ms)")
        println()
        println("Method Output:")
        println("-".repeat(60))
        println(result)
        
    } catch (e: Exception) {
        println("✗ Execution failed: ${'$'}{e.message}")
        e.printStackTrace()
        kotlin.system.exitProcess(1)
    }
    
    println()
    println("=" + "=".repeat(60))
    println("Script execution completed successfully")
}
        """.trimIndent()
    }

    /**
     * Validate user method for security and syntax
     */
    private fun validateUserMethod(methodName: String, methodBody: String) {
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
            "java.net.Socket",
            "Class.forName",
            "exec(",
            "deleteRecursively"
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

        // Ensure method accepts Map<Int, String> parameter
        if (!methodBody.contains("Map<Int, String>") && !methodBody.contains("fields:")) {
            throw IllegalArgumentException("Method must accept 'fields' parameter of type Map<Int, String>")
        }
    }

    /**
     * Create sample methods - EXACTLY matching your dialog's expectations
     */
    fun createSampleMethod(format: CodeFormat): String {
        return when (format) {
            CodeFormat.JSON -> """
                /**
                 * Process ISO8583 fields to JSON format
                 * @param fields Map<Int, String> where key is bit number and value is field value
                 */
                fun processToJson(fields: Map<Int, String>,messageType:String,tpdu:String): String {
                    val result = StringBuilder()
                    result.appendLine("{")
                    result.appendLine("  \"messageType\": \"${'$'}{fields[1] ?: "Unknown"}\",")
                    result.appendLine("  \"timestamp\": \"${'$'}{java.time.LocalDateTime.now()}\",")
                    result.appendLine("  \"transaction\": {")
                    
                    fields[4]?.let { result.appendLine("    \"amount\": \"${'$'}it\",") }
                    fields[3]?.let { result.appendLine("    \"processingCode\": \"${'$'}it\",") }
                    fields[11]?.let { result.appendLine("    \"traceNumber\": \"${'$'}it\"") }
                    
                    result.appendLine("  },")
                    result.appendLine("  \"card\": {")
                    
                    fields[2]?.let { 
                        val maskedPan = if (it.length >= 10) {
                            it.take(6) + "*".repeat(it.length - 10) + it.takeLast(4)
                        } else it
                        result.appendLine("    \"pan\": \"${'$'}maskedPan\",")
                    }
                    fields[37]?.let { result.appendLine("    \"referenceNumber\": \"${'$'}it\"") }
                    
                    result.appendLine("  },")
                    result.appendLine("  \"response\": {")
                    fields[39]?.let { result.appendLine("    \"responseCode\": \"${'$'}it\",") }
                    fields[38]?.let { result.appendLine("    \"authCode\": \"${'$'}it\"") }
                    result.appendLine("  },")
                    result.appendLine("  \"terminal\": {")
                    fields[41]?.let { result.appendLine("    \"terminalId\": \"${'$'}it\",") }
                    fields[42]?.let { result.appendLine("    \"merchantId\": \"${'$'}it\"") }
                    result.appendLine("  },")
                    result.appendLine("  \"metadata\": {")
                    result.appendLine("    \"totalFields\": ${'$'}{fields.size},")
                    result.appendLine("    \"processedAt\": \"${'$'}{java.time.LocalDateTime.now()}\"")
                    result.appendLine("  }")
                    result.appendLine("}")
                    
                    return result.toString()
                }
            """.trimIndent()

            CodeFormat.XML -> """
                /**
                 * Process ISO8583 fields to XML format
                 */
                fun processToXml(fields: Map<Int, String>,messageType:String,tpdu:String): String {
                    val result = StringBuilder()
                    result.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    result.appendLine("<iso8583Message timestamp=\"${'$'}{java.time.LocalDateTime.now()}\">")
                    
                    fields[1]?.let { result.appendLine("  <messageType>${'$'}it</messageType>") }
                    
                    result.appendLine("  <transaction>")
                    fields[4]?.let { result.appendLine("    <amount>${'$'}it</amount>") }
                    fields[3]?.let { result.appendLine("    <processingCode>${'$'}it</processingCode>") }
                    fields[11]?.let { result.appendLine("    <traceNumber>${'$'}it</traceNumber>") }
                    fields[7]?.let { result.appendLine("    <transmissionDateTime>${'$'}it</transmissionDateTime>") }
                    result.appendLine("  </transaction>")
                    
                    result.appendLine("  <cardInfo>")
                    fields[2]?.let { 
                        val maskedPan = if (it.length >= 10) {
                            it.take(6) + "*".repeat(it.length - 10) + it.takeLast(4)
                        } else it
                        result.appendLine("    <pan>${'$'}maskedPan</pan>")
                    }
                    fields[14]?.let { result.appendLine("    <expiryDate>${'$'}it</expiryDate>") }
                    result.appendLine("  </cardInfo>")
                    
                    result.appendLine("  <terminal>")
                    fields[41]?.let { result.appendLine("    <terminalId>${'$'}it</terminalId>") }
                    fields[42]?.let { result.appendLine("    <merchantId>${'$'}it</merchantId>") }
                    fields[43]?.let { result.appendLine("    <merchantName>${'$'}{it.replace("&", "&amp;").replace("<", "&lt;")}</merchantName>") }
                    result.appendLine("  </terminal>")
                    
                    result.appendLine("  <response>")
                    fields[39]?.let { result.appendLine("    <code>${'$'}it</code>") }
                    fields[38]?.let { result.appendLine("    <authCode>${'$'}it</authCode>") }
                    result.appendLine("  </response>")
                    
                    result.appendLine("  <allFields>")
                    fields.toSortedMap().forEach { (bit, value) ->
                        val escapedValue = value.replace("&", "&amp;").replace("<", "&lt;")
                        result.appendLine("    <field bit=\"${'$'}bit\" description=\"${'$'}{getFieldDescription(bit)}\">${'$'}escapedValue</field>")
                    }
                    result.appendLine("  </allFields>")
                    
                    result.appendLine("</iso8583Message>")
                    return result.toString()
                }
                
                fun getFieldDescription(bit: Int): String {
                    return when(bit) {
                        1 -> "Message Type Indicator"
                        2 -> "Primary Account Number"
                        3 -> "Processing Code"
                        4 -> "Transaction Amount"
                        7 -> "Transmission Date/Time"
                        11 -> "System Trace Audit Number"
                        12 -> "Local Transaction Time"
                        13 -> "Local Transaction Date"
                        14 -> "Expiration Date"
                        37 -> "Retrieval Reference Number"
                        38 -> "Authorization Code"
                        39 -> "Response Code"
                        41 -> "Card Acceptor Terminal ID"
                        42 -> "Card Acceptor ID Code"
                        43 -> "Card Acceptor Name/Location"
                        else -> "Field ${'$'}bit"
                    }
                }
            """.trimIndent()

            CodeFormat.PLAIN_TEXT -> """
                /**
                 * Process ISO8583 fields to plain text report
                 */
                fun processToText(fields: Map<Int, String>,messageType:String,tpdu:String): String {
                    val result = StringBuilder()
                    result.appendLine("ISO 8583 MESSAGE ANALYSIS REPORT")
                    result.appendLine("${'$'}{"=".repeat(50)}")
                    result.appendLine("Generated: ${'$'}{java.time.LocalDateTime.now()}")
                    result.appendLine()
                    
                    result.appendLine("MESSAGE SUMMARY:")
                    result.appendLine("- Message Type: ${'$'}{fields[1] ?: "Unknown"}")
                    result.appendLine("- Transaction Amount: ${'$'}{fields[4] ?: "N/A"}")
                    result.appendLine("- Response Code: ${'$'}{fields[39] ?: "N/A"}")
                    result.appendLine("- Total Fields: ${'$'}{fields.size}")
                    result.appendLine()
                    
                    result.appendLine("TRANSACTION DETAILS:")
                    fields[2]?.let { 
                        val maskedPan = if (it.length >= 10) {
                            it.take(6) + "*".repeat(it.length - 10) + it.takeLast(4)
                        } else it
                        result.appendLine("- Card Number: ${'$'}maskedPan")
                    }
                    fields[3]?.let { result.appendLine("- Processing Code: ${'$'}it") }
                    fields[11]?.let { result.appendLine("- Trace Number: ${'$'}it") }
                    fields[37]?.let { result.appendLine("- Reference: ${'$'}it") }
                    fields[41]?.let { result.appendLine("- Terminal ID: ${'$'}it") }
                    fields[42]?.let { result.appendLine("- Merchant ID: ${'$'}it") }
                    result.appendLine()
                    
                    result.appendLine("DETAILED FIELD ANALYSIS:")
                    result.appendLine("${'$'}{"-".repeat(70)}")
                    
                    fields.toSortedMap().forEach { (bit, value) ->
                        result.appendLine("Bit ${'$'}{bit.toString().padStart(3, '0')}: ${'$'}value")
                        result.appendLine("  └─ Length: ${'$'}{value.length} characters")
                        result.appendLine("  └─ Type: ${'$'}{analyzeFieldType(value)}")
                        result.appendLine("  └─ Description: ${'$'}{getFieldDescription(bit)}")
                        result.appendLine()
                    }
                    
                    return result.toString()
                }
                
                fun analyzeFieldType(value: String): String {
                    return when {
                        value.all { it.isDigit() } -> "Numeric"
                        value.all { it.isLetterOrDigit() } -> "Alphanumeric"
                        value.contains('=') -> "Track Data"
                        value.length == 4 && value.all { it.isDigit() } -> "Date (MMYY)"
                        else -> "Mixed/Special Characters"
                    }
                }
                
                fun getFieldDescription(bit: Int): String {
                    return when(bit) {
                        1 -> "Message Type Indicator"
                        2 -> "Primary Account Number (PAN)"
                        3 -> "Processing Code"
                        4 -> "Transaction Amount"
                        7 -> "Transmission Date/Time"
                        11 -> "System Trace Audit Number"
                        12 -> "Local Transaction Time"
                        13 -> "Local Transaction Date"
                        14 -> "Expiration Date"
                        37 -> "Retrieval Reference Number"
                        38 -> "Authorization Identification Response"
                        39 -> "Response Code"
                        41 -> "Card Acceptor Terminal ID"
                        42 -> "Card Acceptor ID Code"
                        43 -> "Card Acceptor Name/Location"
                        else -> "ISO 8583 Data Element ${'$'}bit"
                    }
                }
            """.trimIndent()

            CodeFormat.HEX -> """
                /**
                 * Process ISO8583 fields to hexadecimal representation
                 */
                fun processToHex(fields: Map<Int, String>,messageType:String,tpdu:String): String {
                    val result = StringBuilder()
                    result.appendLine("// ISO 8583 Field Hex Dump")
                    result.appendLine("// Generated: ${'$'}{java.time.LocalDateTime.now()}")
                    result.appendLine("// Source: Actual BitAttribute data")
                    result.appendLine()
                    
                    fields.toSortedMap().forEach { (bit, value) ->
                        val hexString = value.toByteArray().joinToString("") { "%02X".format(it) }
                        val readableHex = hexString.chunked(2).joinToString(" ")
                        val asciiDump = value.map { 
                            if (it.isLetterOrDigit() || it in " .-=") it else '.'
                        }.joinToString("")
                        
                        result.appendLine("// Bit ${'$'}bit: \"${'$'}value\"")
                        result.appendLine("// ASCII: ${'$'}asciiDump")
                        result.appendLine("// Hex:   ${'$'}readableHex")
                        result.appendLine("const val BIT_${'$'}{bit.toString().padStart(3, '0')}_HEX = \"${'$'}hexString\"")
                        result.appendLine("const val BIT_${'$'}{bit.toString().padStart(3, '0')}_STR = \"${'$'}{value.replace("\"", "\\\"")}\"")
                        result.appendLine()
                    }
                    
                    result.appendLine("// Utility maps")
                    result.appendLine("val allFieldsHex = mapOf(")
                    fields.toSortedMap().forEach { (bit, value) ->
                        val hexString = value.toByteArray().joinToString("") { "%02X".format(it) }
                        result.appendLine("    ${'$'}bit to \"${'$'}hexString\",")
                    }
                    result.appendLine(")")
                    
                    result.appendLine()
                    result.appendLine("val allFieldsString = mapOf(")
                    fields.toSortedMap().forEach { (bit, value) ->
                        result.appendLine("    ${'$'}bit to \"${'$'}{value.replace("\"", "\\\"")}\",")
                    }
                    result.appendLine(")")
                    
                    return result.toString()
                }
            """.trimIndent()

            CodeFormat.BYTE_ARRAY -> """
                /**
                 * Process ISO8583 fields to byte array representations
                 */
                fun processByteArrays(fields: Map<Int, String>,messageType:String,tpdu:String): String {
                    val result = StringBuilder()
                    result.appendLine("// ISO 8583 Field Byte Arrays")
                    result.appendLine("// Generated: ${'$'}{java.time.LocalDateTime.now()}")
                    result.appendLine("// Source: Actual BitAttribute data")
                    result.appendLine()
                    
                    fields.toSortedMap().forEach { (bit, value) ->
                        val bytes = value.toByteArray()
                        val byteString = bytes.joinToString(", ") { "0x%02X".format(it) }
                        val hexRepresentation = bytes.joinToString("") { "%02X".format(it) }
                        
                        result.appendLine("// Bit ${'$'}bit: \"${'$'}value\"")
                        result.appendLine("// Length: ${'$'}{bytes.size} bytes")
                        result.appendLine("// Hex: ${'$'}hexRepresentation")
                        result.appendLine("val bit${'$'}{bit.toString().padStart(3, '0')}Bytes = byteArrayOf(${'$'}byteString)")
                        result.appendLine("val bit${'$'}{bit.toString().padStart(3, '0')}String = String(bit${'$'}{bit.toString().padStart(3, '0')}Bytes)")
                        result.appendLine()
                    }
                    
                    result.appendLine("// Utility functions")
                    result.appendLine("fun getAllFieldBytes(): Map<Int, ByteArray> {")
                    result.appendLine("    return mapOf(")
                    fields.keys.sorted().forEach { bit ->
                        result.appendLine("        ${'$'}bit to bit${'$'}{bit.toString().padStart(3, '0')}Bytes,")
                    }
                    result.appendLine("    )")
                    result.appendLine("}")
                    result.appendLine()
                    result.appendLine("fun getAllFieldStrings(): Map<Int, String> {")
                    result.appendLine("    return mapOf(")
                    fields.toSortedMap().forEach { (bit, value) ->
                        result.appendLine("        ${'$'}bit to \"${'$'}{value.replace("\"", "\\\"")}\",")
                    }
                    result.appendLine("    )")
                    result.appendLine("}")
                    result.appendLine()
                    result.appendLine("fun getTotalByteSize(): Int {")
                    result.appendLine("    return getAllFieldBytes().values.sumOf { it.size }")
                    result.appendLine("}")
                    result.appendLine()
                    result.appendLine("fun getFieldSummary(): String {")
                    result.appendLine("    return buildString {")
                    result.appendLine("        appendLine(\"Field Summary:\")")
                    result.appendLine("        getAllFieldBytes().forEach { (bit, bytes) ->")
                    result.appendLine("            appendLine(\"  Bit ${'$'}{bit.toString().padStart(3, '0')}: ${'$'}{bytes.size} bytes\")")
                    result.appendLine("        }")
                    result.appendLine("        appendLine(\"Total: ${'$'}{getTotalByteSize()} bytes\")")
                    result.appendLine("    }")
                    result.appendLine("}")
                    
                    return result.toString()
                }
            """.trimIndent()
        }
    }
}
