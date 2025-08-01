package `in`.aicortex.iso8583studio.domain.utils

import ai.cortex.core.IsoUtil
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.updateBit
import `in`.aicortex.iso8583studio.data.model.CodeFormat
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.HttpMethod
import `in`.aicortex.iso8583studio.data.model.HttpInfo
import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.Charset

// Simplified configuration focused on key mapping only
@Serializable
data class FormatMappingConfig(
    val formatType: CodeFormat,
    val mti: MtiMapping? = null,
    val tpdu: TPDUMapping? = null,
    val fieldMappings: Map<String, FieldMapping>? = null,
    val other: List<OtherItem> = emptyList(),
    val entireIsoMessage: EntireIsoMessage? = null,
    val settings: FormatSettings = FormatSettings()
)

@Serializable
data class TPDUMapping(
    val key: String? = null,
    val nestedKey: String? = null,
    val header: String? = null,
    val template: String? = null
)

@Serializable
data class MtiMapping(
    val key: String? = null,
    val nestedKey: String? = null,
    val header: String? = null,
    val template: String? = null
)

@Serializable
data class FieldMapping(
    val key: String? = null,
    val nestedKey: String? = null,
    val header: String? = null,
    val template: String? = null,
    val staticValue: String? = null
)

@Serializable
data class OtherItem(
    val item: OtherItemConfig
)

@Serializable
data class OtherItemConfig(
    val key: String? = null,
    val nestedKey: String? = null,
    val header: String? = null,
)

@Serializable
data class EntireIsoMessage(
    val hex: String? = null,
    val byteArray: String? = null
)

@Serializable
data class FormatSettings(
    val encoding: String = "UTF-8",
    val prettyPrint: Boolean = true,
    val rootElement: String? = null,
    val delimiter: String = "|",
    val keyValueSeparator: String = "="
)

// Enhanced Iso8583Data with format support
fun Iso8583Data.packWithFormat(
    outputFormat: CodeFormat,
    messageLengthType: MessageLengthType,
    mappingConfig: FormatMappingConfig,
    isHttpInfo: Boolean = true
): ByteArray {
    return when (outputFormat) {
        CodeFormat.JSON -> packAsJson(mappingConfig,isHttpInfo).toByteArray(Charset.forName(mappingConfig.settings.encoding))
        CodeFormat.XML -> packAsXml(mappingConfig,isHttpInfo).toByteArray(Charset.forName(mappingConfig.settings.encoding))
        CodeFormat.PLAIN_TEXT -> packAsKeyValue(mappingConfig).toByteArray(
            Charset.forName(
                mappingConfig.settings.encoding
            )
        )

        CodeFormat.HEX -> {
            val binaryData = this.pack(messageLengthType) // Use existing pack method
            IsoUtil.bytesToHexString(binaryData)
                .toByteArray(Charset.forName(mappingConfig.settings.encoding))
        }

        CodeFormat.BYTE_ARRAY -> this.pack(messageLengthType) // Use existing pack method
    }
}

fun Iso8583Data.unpackFromFormat(
    inputData: ByteArray,
    inputFormat: CodeFormat,
    mappingConfig: FormatMappingConfig,
) {
    when (inputFormat) {
        CodeFormat.JSON -> unpackFromJson(
            String(
                inputData,
                Charset.forName(mappingConfig.settings.encoding)
            ), mappingConfig
        )

        CodeFormat.XML -> unpackFromXml(
            String(inputData, Charset.forName(mappingConfig.settings.encoding)),
            mappingConfig
        )

        CodeFormat.PLAIN_TEXT -> unpackFromKeyValue(
            String(
                inputData,
                Charset.forName(mappingConfig.settings.encoding)
            ), mappingConfig
        )

        CodeFormat.HEX -> {
            val hexString = String(inputData, Charset.forName(mappingConfig.settings.encoding))
            val binaryData = IsoUtil.hexStringToBinary(hexString)
            this.unpackByteArray(binaryData) // Use existing unpack method
        }

        CodeFormat.BYTE_ARRAY -> this.unpackByteArray(inputData) // Use existing unpack method
    }
}


// Enhanced JSON Format Implementation with header support
private fun Iso8583Data.packAsJson(config: FormatMappingConfig,isHttpInfo: Boolean): String {
    val objectMapper = ObjectMapper()
    val rootMap = mutableMapOf<String, Any>()
    val headerMap = mutableMapOf<String, String>()
    // Add MTI
    val mtiValue = this.messageType
    when {
        config.mti?.template != null -> {
            val processedTemplate = config.mti?.template!!.replace("{mti}", mtiValue)
            setNestedValue(rootMap, processedTemplate, mtiValue)
        }

        config.mti?.nestedKey != null -> {
            setNestedValue(rootMap, config.mti?.nestedKey!!, mtiValue)
        }

        config.mti?.header != null && mtiValue.isNotEmpty()-> {
            headerMap[config.mti?.header!!] = mtiValue
        }

        config.mti?.key != null && mtiValue.isNotEmpty()-> {
            rootMap[config.mti?.key!!] = mtiValue
        }
    }

    // Add tpdu
    val tpdu = IsoUtil.bytesToHexString(this.tpduHeader.pack())
    when {
        config.tpdu?.template != null -> {
            val processedTemplate = config.tpdu?.template!!.replace("{tpdu}", tpdu)
            setNestedValue(rootMap, processedTemplate, tpdu)
        }

        config.tpdu?.nestedKey != null -> {
            setNestedValue(rootMap, config.tpdu?.nestedKey!!, tpdu)
        }

        config.tpdu?.header != null && tpdu.isNotEmpty()-> {
            headerMap[config.tpdu?.header!!] = tpdu
        }

        config.tpdu?.key != null  && tpdu.isNotEmpty()-> {
            rootMap[config.tpdu?.key!!] = tpdu
        }
    }
    // Add fields
    config.fieldMappings?.forEach { (fieldNum, mapping) ->
        val bitIndex = fieldNum.toInt() - 1
        if (this.isBitSet(bitIndex)) {
            val fieldValue = this.getValue(bitIndex) ?: return@forEach
            when {
                mapping.template != null -> {
                    val processedTemplate = mapping.template!!
                        .replace("{field$fieldNum}", fieldValue)
                        .replace("{data}", fieldValue)
                    val keyPath = extractKeyFromTemplate(processedTemplate)
                    setNestedValue(rootMap, keyPath, fieldValue)
                }

                mapping.nestedKey != null -> {
                    setNestedValue(rootMap, mapping.nestedKey!!, fieldValue)
                }

                mapping.header != null && fieldValue.isNotEmpty()-> {
                    headerMap[mapping.header!!] = fieldValue
                }

                mapping.key != null && fieldValue.isNotEmpty()-> {
                    rootMap[mapping.key!!] = fieldValue
                }
            }
        }
        // Handle static values
        mapping.staticValue?.let { staticVal ->
            when {
                mapping.nestedKey != null -> setNestedValue(rootMap, mapping.nestedKey!!, staticVal)
                mapping.header != null -> headerMap[mapping.header!!] = staticVal
                mapping.key != null -> rootMap[mapping.key!!] = staticVal
            }
        }
    }
    // Add other items
    config.other.forEach { otherItem ->
        when {
            otherItem.item.nestedKey != null -> {
                setNestedValue(
                    rootMap,
                    otherItem.item.nestedKey!!,
                    this.otherKAttributes[otherItem.item.nestedKey] ?: ""
                )
            }

            otherItem.item.header != null && this.otherKAttributes[otherItem.item.key] != null
                    && this.otherKAttributes[otherItem.item.key]!!.isNotEmpty() -> {
                headerMap[otherItem.item.header!!] =
                    this.otherKAttributes[otherItem.item.header]!!
            }

            otherItem.item.key != null && this.otherKAttributes[otherItem.item.key] != null
                    && this.otherKAttributes[otherItem.item.key]!!.isNotEmpty()-> {
                rootMap[otherItem.item.key!!] = this.otherKAttributes[otherItem.item.key]!!
            }
        }
    }
    // Add entire ISO message if configured
    config.entireIsoMessage?.let { entireMsg ->
        entireMsg.hex?.let { hexKey ->
            if (hexKey.isNotBlank()) {
                val hexValue = IsoUtil.bytesToHexString(this.pack())
                rootMap[hexKey] = hexValue
            }
        }
        entireMsg.byteArray?.let { byteArrayKey ->
            if (byteArrayKey.isNotBlank()) {
                val byteArrayValue = this.pack()
                rootMap[byteArrayKey] = byteArrayValue.contentToString()
            }
        }
    }
    headerMap.put("Content-Type","application/json")
    // Add header to root if it has content
    if (headerMap.isNotEmpty() && isHttpInfo) {
        rootMap["HTTP_INFO_STUDIO"] = Json.encodeToString(this.httpInfo?.copy(headers = headerMap) )
    }
    return if (config.settings.prettyPrint) {
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootMap)
    } else {
        objectMapper.writeValueAsString(rootMap)
    }
}

/**
 * Parse raw HTTP request string into HttpRequestInfo
 */
private fun parseHttpRequest(rawRequest: String): HttpInfo {
    val lines = rawRequest.trim().split("\r\n", "\n")
//    if (lines.isEmpty()) {
//        throw IllegalArgumentException("Invalid HTTP request: empty content")
//    }

    // Parse request line (first line)
    val requestLine = lines[0].trim()
    val requestParts = requestLine.split(" ")

//    if (requestParts.size < 3) {
//        throw IllegalArgumentException("Invalid HTTP request line: $requestLine")
//    }

    val method = try {
        HttpMethod.valueOf(requestParts[0].uppercase())
    } catch (e: IllegalArgumentException) {
        null
    }

    val pathAndQuery = requestParts.getOrNull(1)
    val (path, queryParams) = parsePathAndQuery(pathAndQuery)
    val version = requestParts.getOrNull(2)

    // Parse headers
    val headers = mutableMapOf<String, String>()
    var bodyStartIndex = -1
    if (method != null) {
        for (i in 1 until lines.size) {
            val line = lines[i].trim()

            if (line.isEmpty()) {
                bodyStartIndex = i + 1
                break
            }

            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val headerName = line.substring(0, colonIndex).trim()
                val headerValue = line.substring(colonIndex + 1).trim()
                headers[headerName] = headerValue
            }
        }
    } else {
        bodyStartIndex = 0
    }


    // Parse body
    val body = if (bodyStartIndex >= 0 && bodyStartIndex < lines.size) {
        lines.subList(bodyStartIndex, lines.size).joinToString("\n")
    } else {
        ""
    }

    return method?.let {
        HttpInfo(
            method = method,
            path = path!!,
            version = version!!,
            headers = headers,
            body = body,
            queryParams = queryParams!!
        )
    } ?: run {
        HttpInfo(
            path = path ?: "/",
            version = "HTTP/1.1",
            body = body,
            headers = headers
        )
    }
}
/**
 * Utility functions
 */
fun concatPathAndQuery(path: String?,query:Map<String, String>?):  String {
    val parts = StringBuilder()
    parts.append(path)
    if(!query.isNullOrEmpty()){
        parts.append("?")
    }
    query?.forEach {
        parts.append("${it.key}=${it.value}")
    }
    parts.trimEnd { it == '&' }
    return parts.toString()
}


/**
 * Utility functions
 */
fun parsePathAndQuery(pathAndQuery: String?): Pair<String?, Map<String, String>?> {
    val parts = pathAndQuery?.split("?", limit = 2)
    val path = parts?.getOrNull(0)
    val queryParams = if ((parts?.size ?: 0) > 1) {
        parseQueryParams(parts?.getOrNull(1))
    } else {
        emptyMap()
    }
    return Pair(path, queryParams)
}

private fun parseQueryParams(queryString: String?): Map<String, String>? {
    return queryString?.split("&")
        ?.mapNotNull { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                parts[0] to parts[1]
            } else {
                null
            }
        }
        ?.toMap()
}


private fun Iso8583Data.unpackFromJson(jsonString: String, config: FormatMappingConfig) {
    try {
        val requestInfo = parseHttpRequest(jsonString)
        if(this.httpInfo == null){
            this.httpInfo = requestInfo
        }
        val cleanJsonString = extractJsonFromMixedContent(requestInfo.body)
        if (cleanJsonString.isBlank()) {
            throw IllegalArgumentException("No valid JSON content found in input")
        }
        val objectMapper = ObjectMapper().apply {
            configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
        val jsonNode = objectMapper.readTree(cleanJsonString)
        val headerNode = requestInfo.headers
        // Extract MTI
        val mti = when {
            config.mti?.nestedKey != null -> getNestedValue(jsonNode, config.mti?.nestedKey!!)
            config.mti?.header != null -> headerNode[config.mti?.header]
            config.mti?.key != null -> jsonNode.get(config.mti?.key!!)?.asText()
            else -> null
        }
        mti?.let { this.messageType = it }

        // Extract TPDU
        val tpdu = when {
            config.tpdu?.nestedKey != null -> getNestedValue(jsonNode, config.tpdu?.nestedKey!!)
            config.tpdu?.header != null -> headerNode[config.tpdu?.header]
            config.tpdu?.key != null -> jsonNode.get(config.tpdu?.key!!)?.asText()
            else -> null
        }
        tpdu?.let { this.tpduHeader.rawTPDU = IsoUtil.stringToBcd(it) }

        // Extract fields
        config.fieldMappings?.forEach { (fieldNum, mapping) ->
            val fieldValue = when {
                mapping.nestedKey != null -> getNestedValue(jsonNode, mapping.nestedKey!!)
                mapping.header != null -> headerNode[mapping.header]
                mapping.key != null -> jsonNode.get(mapping.key!!)?.asText()
                else -> null
            }
            fieldValue?.let {
                val bitIndex = fieldNum.toInt() - 1
                this[bitIndex + 1]?.updateBit(it)
            }
        }

        config.entireIsoMessage?.let { isoMessage ->
            isoMessage.hex?.let {
                this.unpackByteArray(IsoUtil.hexStringToBinary(it))
            }

            isoMessage.byteArray?.let {
                val byteArrayValue = jsonNode.get(it)?.asText()
                    ?.toByteArray(Charset.forName(config.settings.encoding))
                byteArrayValue?.let { bytes ->
                    this.unpackByteArray(bytes)
                }
            }
        }

        // Process other items for reverse mapping if needed
        config.other.forEach { otherItem ->
            // This could be used for validation or additional processing
            val otherValue = when {
                otherItem.item.nestedKey != null -> getNestedValue(
                    jsonNode,
                    otherItem.item.nestedKey!!
                )

                otherItem.item.header != null -> headerNode?.get(otherItem.item.header!!)
                otherItem.item.key != null -> jsonNode.get(otherItem.item.key!!)?.asText()
                else -> null
            }

            // Store or validate otherValue if needed
            val key = otherItem.item.nestedKey ?: otherItem.item.header ?: otherItem.item.key
            otherKAttributes.put(key, otherValue)
        }
    } catch (e: JsonProcessingException) {
        throw IllegalArgumentException("Invalid JSON format: ${e.message}", e)
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to parse JSON: ${e.message}", e)
    }
}

/**
 * Extracts JSON content from mixed content that may include HTTP headers
 * Handles various scenarios like HTTP responses, multipart content, etc.
 */
private fun extractJsonFromMixedContent(input: String): String {
    val trimmedInput = input.trim()

    // Case 1: Pure JSON (starts with { or [)
    if (trimmedInput.startsWith("{") || trimmedInput.startsWith("[")) {
        return findCompleteJson(trimmedInput)
    }

    // Case 2: HTTP Response format
    if (isHttpResponse(trimmedInput)) {
        return extractJsonFromHttpResponse(trimmedInput)
    }

    // Case 3: Mixed content with JSON somewhere
    return findJsonInMixedContent(trimmedInput)
}

/**
 * Checks if the content looks like an HTTP response
 */
private fun isHttpResponse(content: String): Boolean {
    val httpStatusPattern = Regex("^HTTP/\\d\\.\\d\\s+\\d{3}\\s+.+", RegexOption.IGNORE_CASE)
    val httpRequestPattern = Regex(
        "^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\\s+.+HTTP/\\d\\.\\d",
        RegexOption.IGNORE_CASE
    )

    return httpStatusPattern.containsMatchIn(content) ||
            httpRequestPattern.containsMatchIn(content) ||
            content.contains("Content-Type:", ignoreCase = true) ||
            content.contains("Content-Length:", ignoreCase = true)
}

/**
 * Extracts JSON from HTTP response format
 */
private fun extractJsonFromHttpResponse(httpContent: String): String {
    val lines = httpContent.lines()
    var bodyStartIndex = -1

    // Find where headers end (empty line indicates start of body)
    for (i in lines.indices) {
        if (lines[i].trim().isEmpty()) {
            bodyStartIndex = i + 1
            break
        }
    }

    if (bodyStartIndex == -1) {
        // No empty line found, try to find JSON pattern
        return findJsonInMixedContent(httpContent)
    }

    // Extract body content
    val bodyLines = lines.drop(bodyStartIndex)
    val bodyContent = bodyLines.joinToString("\n").trim()

    // Handle chunked encoding or other HTTP body formats
    return when {
        bodyContent.startsWith("{") || bodyContent.startsWith("[") -> {
            findCompleteJson(bodyContent)
        }
        // Handle chunked encoding (hex numbers followed by content)
        bodyContent.contains("\n") && bodyContent.lines()
            .any { it.matches(Regex("^[0-9a-fA-F]+$")) } -> {
            extractJsonFromChunkedBody(bodyContent)
        }

        else -> {
            findJsonInMixedContent(bodyContent)
        }
    }
}

/**
 * Extracts JSON from chunked HTTP body
 */
private fun extractJsonFromChunkedBody(chunkedBody: String): String {
    val lines = chunkedBody.lines()
    val jsonParts = mutableListOf<String>()

    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()

        // Skip chunk size lines (hex numbers)
        if (line.matches(Regex("^[0-9a-fA-F]+$"))) {
            i++
            continue
        }

        // Skip empty lines
        if (line.isEmpty()) {
            i++
            continue
        }

        // Add content lines
        if (line.startsWith("{") || line.startsWith("[") ||
            line.contains("\"") || jsonParts.isNotEmpty()
        ) {
            jsonParts.add(line)
        }

        i++
    }

    val reconstructedJson = jsonParts.joinToString("")
    return findCompleteJson(reconstructedJson)
}

/**
 * Finds JSON content within mixed text content
 */
private fun findJsonInMixedContent(content: String): String {
    // Try to find JSON object boundaries
    val jsonObjectPattern = Regex("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}")
    val jsonArrayPattern = Regex("\\[[^\\[\\]]*(?:\\[[^\\[\\]]*\\][^\\[\\]]*)*\\]")

    // Look for complete JSON objects first
    val objectMatch = jsonObjectPattern.find(content)
    if (objectMatch != null) {
        val potentialJson = objectMatch.value
        if (isValidJson(potentialJson)) {
            return potentialJson
        }
    }

    // Look for JSON arrays
    val arrayMatch = jsonArrayPattern.find(content)
    if (arrayMatch != null) {
        val potentialJson = arrayMatch.value
        if (isValidJson(potentialJson)) {
            return potentialJson
        }
    }

    // More aggressive search - look for anything between { and }
    val startIndex = content.indexOf('{')
    val endIndex = content.lastIndexOf('}')

    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
        val potentialJson = content.substring(startIndex, endIndex + 1)
        if (isValidJson(potentialJson)) {
            return potentialJson
        }
    }

    // Last resort - try to find any JSON-like structure
    return findJsonLikeStructure(content)
}

/**
 * Finds complete JSON from content that starts with JSON
 */
private fun findCompleteJson(jsonContent: String): String {
    if (jsonContent.startsWith("{")) {
        return findCompleteJsonObject(jsonContent)
    } else if (jsonContent.startsWith("[")) {
        return findCompleteJsonArray(jsonContent)
    }
    return jsonContent
}

/**
 * Finds complete JSON object by balancing braces
 */
private fun findCompleteJsonObject(content: String): String {
    var braceCount = 0
    var inString = false
    var escaped = false

    for (i in content.indices) {
        val char = content[i]

        when {
            escaped -> {
                escaped = false
            }

            char == '\\' && inString -> {
                escaped = true
            }

            char == '"' -> {
                inString = !inString
            }

            !inString -> {
                when (char) {
                    '{' -> braceCount++
                    '}' -> {
                        braceCount--
                        if (braceCount == 0) {
                            return content.substring(0, i + 1)
                        }
                    }
                }
            }
        }
    }

    return content // Return as-is if no complete object found
}

/**
 * Finds complete JSON array by balancing brackets
 */
private fun findCompleteJsonArray(content: String): String {
    var bracketCount = 0
    var inString = false
    var escaped = false

    for (i in content.indices) {
        val char = content[i]

        when {
            escaped -> {
                escaped = false
            }

            char == '\\' && inString -> {
                escaped = true
            }

            char == '"' -> {
                inString = !inString
            }

            !inString -> {
                when (char) {
                    '[' -> bracketCount++
                    ']' -> {
                        bracketCount--
                        if (bracketCount == 0) {
                            return content.substring(0, i + 1)
                        }
                    }
                }
            }
        }
    }

    return content // Return as-is if no complete array found
}

/**
 * Validates if a string is valid JSON
 */
private fun isValidJson(jsonString: String): Boolean {
    return try {
        ObjectMapper().readTree(jsonString)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Last resort method to find JSON-like structures
 */
private fun findJsonLikeStructure(content: String): String {
    // Look for patterns that might be JSON fields
    val jsonFieldPattern = Regex("\"[^\"]+\"\\s*:\\s*\"[^\"]*\"")
    val matches = jsonFieldPattern.findAll(content).toList()

    if (matches.isNotEmpty()) {
        // Try to reconstruct a JSON object from found fields
        val fields = matches.map { it.value }
        val reconstructed = "{${fields.joinToString(",")}}"

        if (isValidJson(reconstructed)) {
            return reconstructed
        }
    }

    // If all else fails, return empty JSON object
    return "{}"
}


// Enhanced XML Format Implementation with header support
private fun Iso8583Data.packAsXml(config: FormatMappingConfig,isHttpInfo: Boolean): String {
    val xmlMapper = XmlMapper()
    val rootMap = mutableMapOf<String, Any>()
    val headerMap = mutableMapOf<String, String>()
    // Add MTI
    val mtiValue = this.messageType
    when {
        config.mti?.template != null -> {
            val processedTemplate = config.mti?.template!!.replace("{mti}", mtiValue)
            val keyPath = extractKeyFromTemplate(processedTemplate)
            setNestedValue(rootMap, keyPath, mtiValue)
        }

        config.mti?.nestedKey != null -> {
            setNestedValue(rootMap, config.mti?.nestedKey!!, mtiValue)
        }

        config.mti?.header != null && mtiValue.isNotEmpty()-> {
            headerMap[config.mti?.header!!] = mtiValue
        }

        config.mti?.key != null && mtiValue.isNotEmpty()-> {
            rootMap[config.mti?.key!!] = mtiValue
        }
    }
    // Add tpdu
    val tpdu = IsoUtil.bytesToHexString(this.tpduHeader.pack())
    when {
        config.tpdu?.template != null -> {
            val processedTemplate = config.tpdu?.template!!.replace("{tpdu}", tpdu)
            setNestedValue(rootMap, processedTemplate, tpdu)
        }

        config.tpdu?.nestedKey != null -> {
            setNestedValue(rootMap, config.tpdu?.nestedKey!!, tpdu)
        }

        config.tpdu?.header != null && tpdu.isNotEmpty()-> {
            headerMap[config.tpdu?.header!!] = tpdu
        }

        config.tpdu?.key != null && tpdu.isNotEmpty()-> {
            rootMap[config.tpdu?.key!!] = tpdu
        }
    }
    // Add fields
    config.fieldMappings?.forEach { (fieldNum, mapping) ->
        val bitIndex = fieldNum.toInt() - 1
        if (this.isBitSet(bitIndex)) {
            val fieldValue = this.getValue(bitIndex) ?: return@forEach
            when {
                mapping.template != null -> {
                    val processedTemplate = mapping.template!!
                        .replace("{field$fieldNum}", fieldValue)
                        .replace("{data}", fieldValue)
                    val keyPath = extractKeyFromTemplate(processedTemplate)
                    setNestedValue(rootMap, keyPath, fieldValue)
                }

                mapping.nestedKey != null -> {
                    setNestedValue(rootMap, mapping.nestedKey!!, fieldValue)
                }

                mapping.header != null && fieldValue.isNotEmpty()-> {
                    headerMap[mapping.header!!] = fieldValue
                }

                mapping.key != null && fieldValue.isNotEmpty()-> {
                    rootMap[mapping.key!!] = fieldValue
                }
            }
        }
        // Handle static values
        mapping.staticValue?.let { staticVal ->
            when {
                mapping.nestedKey != null -> setNestedValue(rootMap, mapping.nestedKey!!, staticVal)
                mapping.header != null -> headerMap[mapping.header!!] = staticVal
                mapping.key != null -> rootMap[mapping.key!!] = staticVal
            }
        }
    }
    // Add other items
    config.other.forEach { otherItem ->
        when {
            otherItem.item.nestedKey != null -> {
                setNestedValue(
                    rootMap,
                    otherItem.item.nestedKey!!,
                    this.otherKAttributes[otherItem.item.nestedKey] ?: ""
                )
            }

            otherItem.item.header != null && this.otherKAttributes[otherItem.item.key] != null
                    && this.otherKAttributes[otherItem.item.key]!!.isNotEmpty()-> {
                headerMap[otherItem.item.header!!] =
                    this.otherKAttributes[otherItem.item.header]!!
            }

            otherItem.item.key != null && this.otherKAttributes[otherItem.item.key] != null
                    && this.otherKAttributes[otherItem.item.key]!!.isNotEmpty()-> {
                rootMap[otherItem.item.key!!] = this.otherKAttributes[otherItem.item.key]!!
            }
        }
    }
    // Add entire ISO message if configured
    config.entireIsoMessage?.let { entireMsg ->
        entireMsg.hex?.let { hexKey ->
            if (hexKey.isNotBlank()) {
                val hexValue = IsoUtil.bytesToHexString(this.pack())
                rootMap[hexKey] = hexValue
            }
        }
        entireMsg.byteArray?.let { byteArrayKey ->
            if (byteArrayKey.isNotBlank()) {
                val byteArrayValue = this.pack()
                rootMap[byteArrayKey] = byteArrayValue.contentToString()
            }
        }
    }
    headerMap.put("Content-Type","application/xml")
    // Add header to root if it has content
    if (headerMap.isNotEmpty() && isHttpInfo) {
        rootMap["HTTP_INFO_STUDIO"] = Json.encodeToString(this.httpInfo?.copy(headers = headerMap) )
    }
    // Wrap in root element if configured
    val finalMap = config.settings.rootElement?.let { rootElement ->
        mapOf(rootElement to rootMap)
    } ?: rootMap
    return if (config.settings.prettyPrint) {
        xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalMap)
    } else {
        xmlMapper.writeValueAsString(finalMap)
    }
}

private fun Iso8583Data.unpackFromXml(xmlString: String, config: FormatMappingConfig) {
    val requestInfo = parseHttpRequest(xmlString)
    if(this.httpInfo == null){
        this.httpInfo = requestInfo
    }
    val xmlMapper = XmlMapper()
    val xmlNode = xmlMapper.readTree(requestInfo.body)
    // Navigate to root element if configured
    val workingNode = config.settings.rootElement?.let { rootElement ->
        xmlNode.get(rootElement) ?: xmlNode
    } ?: xmlNode
    val headerNode = requestInfo.headers
    // Extract MTI
    val mti = when {
        config.mti?.nestedKey != null -> getNestedValue(workingNode, config.mti?.nestedKey!!)
        config.mti?.header != null -> headerNode[config.mti?.header]
        config.mti?.key != null -> workingNode.get(config.mti?.key!!)?.asText()
        else -> null
    }
    mti?.let { this.messageType = it }

    // Extract TPDU
    val tpdu = when {
        config.tpdu?.nestedKey != null -> getNestedValue(workingNode, config.tpdu?.nestedKey!!)
        config.tpdu?.header != null -> headerNode[config.tpdu?.header]
        config.tpdu?.key != null -> workingNode.get(config.tpdu?.key!!)?.asText()
        else -> null
    }
    tpdu?.let { this.tpduHeader.rawTPDU = IsoUtil.stringToBcd(it) }

    // Extract fields
    config.fieldMappings?.forEach { (fieldNum, mapping) ->
        val fieldValue = when {
            mapping.nestedKey != null -> getNestedValue(workingNode, mapping.nestedKey!!)
            mapping.header != null -> headerNode[mapping.header]
            mapping.key != null -> workingNode.get(mapping.key!!)?.asText()
            else -> null
        }
        fieldValue?.let {
            val bitIndex = fieldNum.toInt() - 1
            this[bitIndex + 1]?.updateBit(it)
        }
    }

    config.entireIsoMessage?.let { isoMessage ->
        isoMessage.hex?.let {
            this.unpackByteArray(IsoUtil.hexStringToBinary(it))
        }

        isoMessage.byteArray?.let {
            val byteArrayValue = workingNode.get(it)?.asText()
                ?.toByteArray(Charset.forName(config.settings.encoding))
            byteArrayValue?.let { bytes ->
                this.unpackByteArray(bytes)
            }
        }
    }

    // Process other items for reverse mapping if needed
    config.other.forEach { otherItem ->
        val otherValue = when {
            otherItem.item.nestedKey != null -> getNestedValue(
                workingNode,
                otherItem.item.nestedKey!!
            )

            otherItem.item.header != null -> headerNode?.get(otherItem.item.header!!)
            otherItem.item.key != null -> workingNode.get(otherItem.item.key!!)?.asText()
            else -> null
        }
        // Store or validate otherValue if needed
        val key = otherItem.item.nestedKey ?: otherItem.item.header ?: otherItem.item.key
        otherKAttributes.put(key, otherValue)
    }
}


// Key-Value Format Implementation
private fun Iso8583Data.packAsKeyValue(config: FormatMappingConfig): String {
    val pairs = mutableListOf<String>()
    val delimiter = config.settings.delimiter
    val kvSeparator = config.settings.keyValueSeparator

    // Add MTI
    val mtiKey = config.mti?.key
    pairs.add("$mtiKey$kvSeparator${this.messageType}")

    // Add fields
    config.fieldMappings?.forEach { (fieldNum, mapping) ->
        val bitIndex = fieldNum.toInt() - 1
        if (this.isBitSet(bitIndex)) {
            val fieldValue = this.getValue(bitIndex) ?: return@forEach
            val key = mapping.key ?: "F$fieldNum"
            pairs.add("$key$kvSeparator$fieldValue")
        }
    }

    return pairs.joinToString(delimiter)
}

private fun Iso8583Data.unpackFromKeyValue(kvString: String, config: FormatMappingConfig) {
    val delimiter = config.settings.delimiter
    val kvSeparator = config.settings.keyValueSeparator

    val pairs = kvString.split(delimiter)
    val dataMap = mutableMapOf<String, String>()

    pairs.forEach { pair ->
        val parts = pair.split(kvSeparator, limit = 2)
        if (parts.size == 2) {
            dataMap[parts[0].trim()] = parts[1].trim()
        }
    }

    // Extract MTI
    dataMap[config.mti?.key]?.let { this.messageType = it }

    // Extract fields
    config.fieldMappings?.forEach { (fieldNum, mapping) ->
        val key = mapping.key ?: "F$fieldNum"
        dataMap[key]?.let { value ->
            val bitIndex = fieldNum.toInt() - 1
            this[bitIndex + 1]?.updateBit(value)
        }
    }
}

// Utility functions for nested key handling
private fun setNestedValue(map: MutableMap<String, Any>, path: String, value: String) {
    if(value.isEmpty()) {
        return // Skip empty values
    }
    val keys = path.split(".")
    var current = map

    for (i in 0 until keys.size - 1) {
        val key = keys[i]
        if (current[key] !is MutableMap<*, *>) {
            current[key] = mutableMapOf<String, Any>()
        }
        @Suppress("UNCHECKED_CAST")
        current = current[key] as MutableMap<String, Any>
    }

    current[keys.last()] = value
}

private fun getNestedValue(jsonNode: JsonNode, path: String): String? {
    val keys = path.split(".")
    var current = jsonNode

    for (key in keys) {
        current = current.get(key) ?: return null
    }

    return current.asText()?.takeIf { it.isNotEmpty() }
}

private fun extractKeyFromTemplate(template: String): String {
    // Extract the key path from template, removing the placeholder parts
    return template.substringBefore("{").trim('.')
}

// Configuration Manager
class FormatConfigManager {
    private val configs = mutableMapOf<String, FormatMappingConfig>()

    fun loadConfig(configName: String, yamlContent: String) {
        val yamlMapper = ObjectMapper(YAMLFactory()).apply {
            registerModule(KotlinModule.Builder().build())
        }
        configs[configName] = yamlMapper.readValue(yamlContent, FormatMappingConfig::class.java)
    }

    fun getConfig(configName: String): FormatMappingConfig? {
        return configs[configName]
    }

    fun getAllConfigs(): Map<String, FormatMappingConfig> {
        return configs.toMap()
    }
}

// Enhanced Iso8583Data Factory
class EnhancedIso8583Factory(private val configManager: FormatConfigManager) {

    fun createFromFormat(
        inputData: ByteArray,
        inputFormat: CodeFormat,
        configName: String,
        gatewayConfig: GatewayConfig
    ): Iso8583Data {
        val mappingConfig = configManager.getConfig(configName)
            ?: throw IllegalArgumentException("Configuration not found: $configName")

        val iso8583Data = Iso8583Data(config = gatewayConfig)
        iso8583Data.unpackFromFormat(inputData, inputFormat, mappingConfig)
        return iso8583Data
    }

    fun convertFormat(
        iso8583Data: Iso8583Data,
        outputFormat: CodeFormat,
        configName: String
    ): ByteArray {
        val mappingConfig = configManager.getConfig(configName)
            ?: throw IllegalArgumentException("Configuration not found: $configName")

        return iso8583Data.packWithFormat(outputFormat, MessageLengthType.HEX_HL, mappingConfig)
    }
}


/**
 * package `in`.aicortex.iso8583studio.domain.utils
 *
 * import com.fasterxml.jackson.core.JsonParser
 * import com.fasterxml.jackson.core.JsonProcessingException
 * import com.fasterxml.jackson.databind.DeserializationFeature
 * import com.fasterxml.jackson.databind.JsonNode
 * import com.fasterxml.jackson.databind.ObjectMapper
 * import com.fasterxml.jackson.dataformat.xml.XmlMapper
 * import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
 * import com.fasterxml.jackson.module.kotlin.KotlinModule
 * import `in`.aicortex.iso8583studio.data.BitSpecific
 * import `in`.aicortex.iso8583studio.data.Iso8583Data
 * import `in`.aicortex.iso8583studio.data.TPDU
 * import `in`.aicortex.iso8583studio.data.model.BitLength
 * import `in`.aicortex.iso8583studio.data.model.BitType
 * import `in`.aicortex.iso8583studio.data.updateBit
 * import `in`.aicortex.iso8583studio.data.model.CodeFormat
 * import `in`.aicortex.iso8583studio.data.model.GatewayConfig
 * import `in`.aicortex.iso8583studio.data.model.GatewayType
 * import `in`.aicortex.iso8583studio.data.model.MessageLengthType
 * import kotlinx.serialization.Serializable
 * import java.nio.charset.Charset
 * import java.time.LocalDateTime
 * import java.time.format.DateTimeFormatter
 *
 * // Enhanced configuration with header and other support
 * @Serializable
 * data class FormatMappingConfig(
 *     val formatType: CodeFormat,
 *     val mti: MtiMapping,
 *     val fieldMappings: Map<String, FieldMapping>,
 *     val other: List<OtherItem> = emptyList(),
 *     val entireIsoMessage: EntireIsoMessage? = null,
 *     val settings: FormatSettings = FormatSettings()
 * )
 *
 * @Serializable
 * data class MtiMapping(
 *     val key: String? = null,
 *     val nestedKey: String? = null,
 *     val header: String? = null,
 *     val template: String? = null
 * )
 *
 * @Serializable
 * data class FieldMapping(
 *     val key: String? = null,
 *     val nestedKey: String? = null,
 *     val header: String? = null,
 *     val template: String? = null,
 *     val staticValue: String? = null
 * )
 *
 * @Serializable
 * data class OtherItem(
 *     val item: OtherItemConfig
 * )
 *
 * @Serializable
 * data class OtherItemConfig(
 *     val key: String? = null,
 *     val nestedKey: String? = null,
 *     val header: String? = null,
 *     val value: String
 * )
 *
 * @Serializable
 * data class EntireIsoMessage(
 *     val hex: String? = null,
 *     val byteArray: String? = null
 * )
 *
 * @Serializable
 * data class FormatSettings(
 *     val encoding: String = "UTF-8",
 *     val prettyPrint: Boolean = true,
 *     val rootElement: String? = null,
 *     val delimiter: String = "|",
 *     val keyValueSeparator: String = "="
 * )
 *
 * // Enhanced JSON Format Implementation with header support
 * private fun Iso8583Data.packAsJson(config: FormatMappingConfig): String {
 *     val objectMapper = ObjectMapper()
 *     val rootMap = mutableMapOf<String, Any>()
 *     val headerMap = mutableMapOf<String, Any>()
 *
 *     // Add MTI
 *     val mtiValue = this.messageType
 *     when {
 *         config.mti?.template != null -> {
 *             val processedTemplate = config.mti?.template!!.replace("{mti}", mtiValue)
 *             setNestedValue(rootMap, processedTemplate, mtiValue)
 *         }
 *         config.mti?.nestedKey != null -> {
 *             setNestedValue(rootMap, config.mti?.nestedKey!!, mtiValue)
 *         }
 *         config.mti?.header != null -> {
 *             headerMap[config.mti?.header!!] = mtiValue
 *         }
 *         config.mti?.key != null -> {
 *             rootMap[config.mti?.key!!] = mtiValue
 *         }
 *     }
 *
 *     // Add fields
 *     config.fieldMappings.forEach { (fieldNum, mapping) ->
 *         val bitIndex = fieldNum.toInt() - 1
 *         if (this.isBitSet(bitIndex)) {
 *             val fieldValue = this.getValue(bitIndex) ?: return@forEach
 *
 *             when {
 *                 mapping.template != null -> {
 *                     val processedTemplate = mapping.template!!
 *                         .replace("{field$fieldNum}", fieldValue)
 *                         .replace("{data}", fieldValue)
 *                     val keyPath = extractKeyFromTemplate(processedTemplate)
 *                     setNestedValue(rootMap, keyPath, fieldValue)
 *                 }
 *                 mapping.nestedKey != null -> {
 *                     setNestedValue(rootMap, mapping.nestedKey!!, fieldValue)
 *                 }
 *                 mapping.header != null -> {
 *                     headerMap[mapping.header!!] = fieldValue
 *                 }
 *                 mapping.key != null -> {
 *                     rootMap[mapping.key!!] = fieldValue
 *                 }
 *             }
 *         }
 *
 *         // Handle static values
 *         mapping.staticValue?.let { staticVal ->
 *             when {
 *                 mapping.nestedKey != null -> setNestedValue(rootMap, mapping.nestedKey!!, staticVal)
 *                 mapping.header != null -> headerMap[mapping.header!!] = staticVal
 *                 mapping.key != null -> rootMap[mapping.key!!] = staticVal
 *             }
 *         }
 *     }
 *
 *     // Add other items
 *     config.other.forEach { otherItem ->
 *         val processedValue = processPlaceholders(otherItem.item.value, this)
 *
 *         when {
 *             otherItem.item.nestedKey != null -> {
 *                 setNestedValue(rootMap, otherItem.item.nestedKey!!, processedValue)
 *             }
 *             otherItem.item.header != null -> {
 *                 headerMap[otherItem.item.header!!] = processedValue
 *             }
 *             otherItem.item.key != null -> {
 *                 rootMap[otherItem.item.key!!] = processedValue
 *             }
 *         }
 *     }
 *
 *     // Add entire ISO message if configured
 *     config.entireIsoMessage?.let { entireMsg ->
 *         entireMsg.hex?.let { hexKey ->
 *             if (hexKey.isNotBlank()) {
 *                 val hexValue = IsoUtil.bytesToHexString(this.pack())
 *                 rootMap[hexKey] = hexValue
 *             }
 *         }
 *         entireMsg.byteArray?.let { byteArrayKey ->
 *             if (byteArrayKey.isNotBlank()) {
 *                 val byteArrayValue = this.pack()
 *                 rootMap[byteArrayKey] = byteArrayValue.contentToString()
 *             }
 *         }
 *     }
 *
 *     // Add header to root if it has content
 *     if (headerMap.isNotEmpty()) {
 *         rootMap["header"] = headerMap
 *     }
 *
 *     return if (config.settings.prettyPrint) {
 *         objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootMap)
 *     } else {
 *         objectMapper.writeValueAsString(rootMap)
 *     }
 * }
 *
 * private fun Iso8583Data.unpackFromJson(jsonString: String, config: FormatMappingConfig) {
 *     try {
 *         val cleanJsonString = extractJsonFromMixedContent(jsonString)
 *
 *         if (cleanJsonString.isBlank()) {
 *             throw IllegalArgumentException("No valid JSON content found in input")
 *         }
 *
 *         val objectMapper = ObjectMapper().apply {
 *             configure(JsonParser.Feature.ALLOW_COMMENTS, true)
 *             configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
 *             configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
 *             configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
 *         }
 *
 *         val jsonNode = objectMapper.readTree(cleanJsonString)
 *         val headerNode = jsonNode.get("header")
 *
 *         // Extract MTI
 *         val mti = when {
 *             config.mti?.nestedKey != null -> getNestedValue(jsonNode, config.mti?.nestedKey!!)
 *             config.mti?.header != null -> headerNode?.get(config.mti?.header!!)?.asText()
 *             config.mti?.key != null -> jsonNode.get(config.mti?.key!!)?.asText()
 *             else -> null
 *         }
 *         mti?.let { this.messageType = it }
 *
 *         // Extract fields
 *         config.fieldMappings.forEach { (fieldNum, mapping) ->
 *             val fieldValue = when {
 *                 mapping.nestedKey != null -> getNestedValue(jsonNode, mapping.nestedKey!!)
 *                 mapping.header != null -> headerNode?.get(mapping.header!!)?.asText()
 *                 mapping.key != null -> jsonNode.get(mapping.key!!)?.asText()
 *                 else -> null
 *             }
 *
 *             fieldValue?.let {
 *                 val bitIndex = fieldNum.toInt() - 1
 *                 this[bitIndex + 1]?.updateBit(it)
 *             }
 *         }
 *
 *         // Process other items for reverse mapping if needed
 *         config.other.forEach { otherItem ->
 *             // This could be used for validation or additional processing
 *             val otherValue = when {
 *                 otherItem.item.nestedKey != null -> getNestedValue(jsonNode, otherItem.item.nestedKey!!)
 *                 otherItem.item.header != null -> headerNode?.get(otherItem.item.header!!)?.asText()
 *                 otherItem.item.key != null -> jsonNode.get(otherItem.item.key!!)?.asText()
 *                 else -> null
 *             }
 *             // Store or validate otherValue if needed
 *         }
 *
 *     } catch (e: JsonProcessingException) {
 *         throw IllegalArgumentException("Invalid JSON format: ${e.message}", e)
 *     } catch (e: Exception) {
 *         throw IllegalArgumentException("Failed to parse JSON: ${e.message}", e)
 *     }
 * }
 *
 * // Enhanced XML Format Implementation with header support
 * private fun Iso8583Data.packAsXml(config: FormatMappingConfig): String {
 *     val xmlMapper = XmlMapper()
 *     val rootMap = mutableMapOf<String, Any>()
 *     val headerMap = mutableMapOf<String, Any>()
 *
 *     // Add MTI
 *     val mtiValue = this.messageType
 *     when {
 *         config.mti?.template != null -> {
 *             val processedTemplate = config.mti?.template!!.replace("{mti}", mtiValue)
 *             val keyPath = extractKeyFromTemplate(processedTemplate)
 *             setNestedValue(rootMap, keyPath, mtiValue)
 *         }
 *         config.mti?.nestedKey != null -> {
 *             setNestedValue(rootMap, config.mti?.nestedKey!!, mtiValue)
 *         }
 *         config.mti?.header != null -> {
 *             headerMap[config.mti?.header!!] = mtiValue
 *         }
 *         config.mti?.key != null -> {
 *             rootMap[config.mti?.key!!] = mtiValue
 *         }
 *     }
 *
 *     // Add fields
 *     config.fieldMappings.forEach { (fieldNum, mapping) ->
 *         val bitIndex = fieldNum.toInt() - 1
 *         if (this.isBitSet(bitIndex)) {
 *             val fieldValue = this.getValue(bitIndex) ?: return@forEach
 *
 *             when {
 *                 mapping.template != null -> {
 *                     val processedTemplate = mapping.template!!
 *                         .replace("{field$fieldNum}", fieldValue)
 *                         .replace("{data}", fieldValue)
 *                     val keyPath = extractKeyFromTemplate(processedTemplate)
 *                     setNestedValue(rootMap, keyPath, fieldValue)
 *                 }
 *                 mapping.nestedKey != null -> {
 *                     setNestedValue(rootMap, mapping.nestedKey!!, fieldValue)
 *                 }
 *                 mapping.header != null -> {
 *                     headerMap[mapping.header!!] = fieldValue
 *                 }
 *                 mapping.key != null -> {
 *                     rootMap[mapping.key!!] = fieldValue
 *                 }
 *             }
 *         }
 *
 *         // Handle static values
 *         mapping.staticValue?.let { staticVal ->
 *             when {
 *                 mapping.nestedKey != null -> setNestedValue(rootMap, mapping.nestedKey!!, staticVal)
 *                 mapping.header != null -> headerMap[mapping.header!!] = staticVal
 *                 mapping.key != null -> rootMap[mapping.key!!] = staticVal
 *             }
 *         }
 *     }
 *
 *     // Add other items
 *     config.other.forEach { otherItem ->
 *         val processedValue = processPlaceholders(otherItem.item.value, this)
 *
 *         when {
 *             otherItem.item.nestedKey != null -> {
 *                 setNestedValue(rootMap, otherItem.item.nestedKey!!, processedValue)
 *             }
 *             otherItem.item.header != null -> {
 *                 headerMap[otherItem.item.header!!] = processedValue
 *             }
 *             otherItem.item.key != null -> {
 *                 rootMap[otherItem.item.key!!] = processedValue
 *             }
 *         }
 *     }
 *
 *     // Add entire ISO message if configured
 *     config.entireIsoMessage?.let { entireMsg ->
 *         entireMsg.hex?.let { hexKey ->
 *             if (hexKey.isNotBlank()) {
 *                 val hexValue = IsoUtil.bytesToHexString(this.pack())
 *                 rootMap[hexKey] = hexValue
 *             }
 *         }
 *         entireMsg.byteArray?.let { byteArrayKey ->
 *             if (byteArrayKey.isNotBlank()) {
 *                 val byteArrayValue = this.pack()
 *                 rootMap[byteArrayKey] = byteArrayValue.contentToString()
 *             }
 *         }
 *     }
 *
 *     // Add header to root if it has content
 *     if (headerMap.isNotEmpty()) {
 *         rootMap["header"] = headerMap
 *     }
 *
 *     // Wrap in root element if configured
 *     val finalMap = config.settings.rootElement?.let { rootElement ->
 *         mapOf(rootElement to rootMap)
 *     } ?: rootMap
 *
 *     return if (config.settings.prettyPrint) {
 *         xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalMap)
 *     } else {
 *         xmlMapper.writeValueAsString(finalMap)
 *     }
 * }
 *
 * private fun Iso8583Data.unpackFromXml(xmlString: String, config: FormatMappingConfig) {
 *     val xmlMapper = XmlMapper()
 *     val xmlNode = xmlMapper.readTree(xmlString)
 *
 *     // Navigate to root element if configured
 *     val workingNode = config.settings.rootElement?.let { rootElement ->
 *         xmlNode.get(rootElement) ?: xmlNode
 *     } ?: xmlNode
 *
 *     val headerNode = workingNode.get("header")
 *
 *     // Extract MTI
 *     val mti = when {
 *         config.mti?.nestedKey != null -> getNestedValue(workingNode, config.mti?.nestedKey!!)
 *         config.mti?.header != null -> headerNode?.get(config.mti?.header!!)?.asText()
 *         config.mti?.key != null -> workingNode.get(config.mti?.key!!)?.asText()
 *         else -> null
 *     }
 *     mti?.let { this.messageType = it }
 *
 *     // Extract fields
 *     config.fieldMappings.forEach { (fieldNum, mapping) ->
 *         val fieldValue = when {
 *             mapping.nestedKey != null -> getNestedValue(workingNode, mapping.nestedKey!!)
 *             mapping.header != null -> headerNode?.get(mapping.header!!)?.asText()
 *             mapping.key != null -> workingNode.get(mapping.key!!)?.asText()
 *             else -> null
 *         }
 *
 *         fieldValue?.let {
 *             val bitIndex = fieldNum.toInt() - 1
 *             this[bitIndex + 1]?.updateBit(it)
 *         }
 *     }
 *
 *     // Process other items for reverse mapping if needed
 *     config.other.forEach { otherItem ->
 *         val otherValue = when {
 *             otherItem.item.nestedKey != null -> getNestedValue(workingNode, otherItem.item.nestedKey!!)
 *             otherItem.item.header != null -> headerNode?.get(otherItem.item.header!!)?.asText()
 *             otherItem.item.key != null -> workingNode.get(otherItem.item.key!!)?.asText()
 *             else -> null
 *         }
 *         // Store or validate otherValue if needed
 *     }
 * }
 *
 * // Utility function to process placeholders in values
 * private fun processPlaceholders(value: String, iso8583Data: Iso8583Data): String {
 *     return when {
 *         value == "[TIME]" -> LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
 *         value == "[DATE]" -> LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
 *         value == "[TIMESTAMP]" -> System.currentTimeMillis().toString()
 *         value == "[MTI]" -> iso8583Data.messageType
 *         value.startsWith("[FIELD_") && value.endsWith("]") -> {
 *             val fieldNum = value.substringAfter("[FIELD_").substringBefore("]").toIntOrNull()
 *             fieldNum?.let {
 *                 val bitIndex = it - 1
 *                 if (iso8583Data.isBitSet(bitIndex)) {
 *                     iso8583Data.getValue(bitIndex) ?: value
 *                 } else {
 *                     value
 *                 }
 *             } ?: value
 *         }
 *         value.startsWith("[TPDU") -> {
 *             // Handle TPDU related placeholders
 *             iso8583Data.tpdu?.toString() ?: value
 *         }
 *         else -> value
 *     }
 * }
 *
 * // Enhanced utility functions for nested key handling
 * private fun setNestedValue(map: MutableMap<String, Any>, path: String, value: String) {
 *     val keys = path.split(".")
 *     var current = map
 *
 *     for (i in 0 until keys.size - 1) {
 *         val key = keys[i]
 *         if (current[key] !is MutableMap<*, *>) {
 *             current[key] = mutableMapOf<String, Any>()
 *         }
 *         @Suppress("UNCHECKED_CAST")
 *         current = current[key] as MutableMap<String, Any>
 *     }
 *
 *     current[keys.last()] = value
 * }
 *
 * private fun getNestedValue(jsonNode: JsonNode, path: String): String? {
 *     val keys = path.split(".")
 *     var current = jsonNode
 *
 *     for (key in keys) {
 *         current = current.get(key) ?: return null
 *     }
 *
 *     return current.asText()
 * }
 *
 * private fun extractKeyFromTemplate(template: String): String {
 *     // Extract the key path from template, removing the placeholder parts
 *     return template.substringBefore("{").trim('.')
 * }
 *
 * // Rest of the existing code remains the same...
 * // (Include all the JSON extraction utility functions from the original code)
 *
 * /**
 *  * Extracts JSON content from mixed content that may include HTTP headers
 *  */
 * private fun extractJsonFromMixedContent(input: String): String {
 *     val trimmedInput = input.trim()
 *
 *     // Case 1: Pure JSON (starts with { or [)
 *     if (trimmedInput.startsWith("{") || trimmedInput.startsWith("[")) {
 *         return findCompleteJson(trimmedInput)
 *     }
 *
 *     // Case 2: HTTP Response format
 *     if (isHttpResponse(trimmedInput)) {
 *         return extractJsonFromHttpResponse(trimmedInput)
 *     }
 *
 *     // Case 3: Mixed content with JSON somewhere
 *     return findJsonInMixedContent(trimmedInput)
 * }
 *
 * private fun isHttpResponse(content: String): Boolean {
 *     val httpStatusPattern = Regex("^HTTP/\\d\\.\\d\\s+\\d{3}\\s+.+", RegexOption.IGNORE_CASE)
 *     val httpRequestPattern = Regex(
 *         "^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\\s+.+HTTP/\\d\\.\\d",
 *         RegexOption.IGNORE_CASE
 *     )
 *
 *     return httpStatusPattern.containsMatchIn(content) ||
 *             httpRequestPattern.containsMatchIn(content) ||
 *             content.contains("Content-Type:", ignoreCase = true) ||
 *             content.contains("Content-Length:", ignoreCase = true)
 * }
 *
 * private fun extractJsonFromHttpResponse(httpContent: String): String {
 *     val lines = httpContent.lines()
 *     var bodyStartIndex = -1
 *
 *     for (i in lines.indices) {
 *         if (lines[i].trim().isEmpty()) {
 *             bodyStartIndex = i + 1
 *             break
 *         }
 *     }
 *
 *     if (bodyStartIndex == -1) {
 *         return findJsonInMixedContent(httpContent)
 *     }
 *
 *     val bodyLines = lines.drop(bodyStartIndex)
 *     val bodyContent = bodyLines.joinToString("\n").trim()
 *
 *     return when {
 *         bodyContent.startsWith("{") || bodyContent.startsWith("[") -> {
 *             findCompleteJson(bodyContent)
 *         }
 *         bodyContent.contains("\n") && bodyContent.lines()
 *             .any { it.matches(Regex("^[0-9a-fA-F]+$")) } -> {
 *             extractJsonFromChunkedBody(bodyContent)
 *         }
 *         else -> {
 *             findJsonInMixedContent(bodyContent)
 *         }
 *     }
 * }
 *
 * private fun extractJsonFromChunkedBody(chunkedBody: String): String {
 *     val lines = chunkedBody.lines()
 *     val jsonParts = mutableListOf<String>()
 *
 *     var i = 0
 *     while (i < lines.size) {
 *         val line = lines[i].trim()
 *
 *         if (line.matches(Regex("^[0-9a-fA-F]+$"))) {
 *             i++
 *             continue
 *         }
 *
 *         if (line.isEmpty()) {
 *             i++
 *             continue
 *         }
 *
 *         if (line.startsWith("{") || line.startsWith("[") ||
 *             line.contains("\"") || jsonParts.isNotEmpty()
 *         ) {
 *             jsonParts.add(line)
 *         }
 *
 *         i++
 *     }
 *
 *     val reconstructedJson = jsonParts.joinToString("")
 *     return findCompleteJson(reconstructedJson)
 * }
 *
 * private fun findJsonInMixedContent(content: String): String {
 *     val jsonObjectPattern = Regex("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}")
 *     val jsonArrayPattern = Regex("\\[[^\\[\\]]*(?:\\[[^\\[\\]]*\\][^\\[\\]]*)*\\]")
 *
 *     val objectMatch = jsonObjectPattern.find(content)
 *     if (objectMatch != null) {
 *         val potentialJson = objectMatch.value
 *         if (isValidJson(potentialJson)) {
 *             return potentialJson
 *         }
 *     }
 *
 *     val arrayMatch = jsonArrayPattern.find(content)
 *     if (arrayMatch != null) {
 *         val potentialJson = arrayMatch.value
 *         if (isValidJson(potentialJson)) {
 *             return potentialJson
 *         }
 *     }
 *
 *     val startIndex = content.indexOf('{')
 *     val endIndex = content.lastIndexOf('}')
 *
 *     if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
 *         val potentialJson = content.substring(startIndex, endIndex + 1)
 *         if (isValidJson(potentialJson)) {
 *             return potentialJson
 *         }
 *     }
 *
 *     return findJsonLikeStructure(content)
 * }
 *
 * private fun findCompleteJson(jsonContent: String): String {
 *     if (jsonContent.startsWith("{")) {
 *         return findCompleteJsonObject(jsonContent)
 *     } else if (jsonContent.startsWith("[")) {
 *         return findCompleteJsonArray(jsonContent)
 *     }
 *     return jsonContent
 * }
 *
 * private fun findCompleteJsonObject(content: String): String {
 *     var braceCount = 0
 *     var inString = false
 *     var escaped = false
 *
 *     for (i in content.indices) {
 *         val char = content[i]
 *
 *         when {
 *             escaped -> {
 *                 escaped = false
 *             }
 *             char == '\\' && inString -> {
 *                 escaped = true
 *             }
 *             char == '"' -> {
 *                 inString = !inString
 *             }
 *             !inString -> {
 *                 when (char) {
 *                     '{' -> braceCount++
 *                     '}' -> {
 *                         braceCount--
 *                         if (braceCount == 0) {
 *                             return content.substring(0, i + 1)
 *                         }
 *                     }
 *                 }
 *             }
 *         }
 *     }
 *
 *     return content
 * }
 *
 * private fun findCompleteJsonArray(content: String): String {
 *     var bracketCount = 0
 *     var inString = false
 *     var escaped = false
 *
 *     for (i in content.indices) {
 *         val char = content[i]
 *
 *         when {
 *             escaped -> {
 *                 escaped = false
 *             }
 *             char == '\\' && inString -> {
 *                 escaped = true
 *             }
 *             char == '"' -> {
 *                 inString = !inString
 *             }
 *             !inString -> {
 *                 when (char) {
 *                     '[' -> bracketCount++
 *                     ']' -> {
 *                         bracketCount--
 *                         if (bracketCount == 0) {
 *                             return content.substring(0, i + 1)
 *                         }
 *                     }
 *                 }
 *             }
 *         }
 *     }
 *
 *     return content
 * }
 *
 * private fun isValidJson(jsonString: String): Boolean {
 *     return try {
 *         ObjectMapper().readTree(jsonString)
 *         true
 *     } catch (e: Exception) {
 *         false
 *     }
 * }
 *
 * private fun findJsonLikeStructure(content: String): String {
 *     val jsonFieldPattern = Regex("\"[^\"]+\"\\s*:\\s*\"[^\"]*\"")
 *     val matches = jsonFieldPattern.findAll(content).toList()
 *
 *     if (matches.isNotEmpty()) {
 *         val fields = matches.map { it.value }
 *         val reconstructed = "{${fields.joinToString(",")}}"
 *
 *         if (isValidJson(reconstructed)) {
 *             return reconstructed
 *         }
 *     }
 *
 *     return "{}"
 * }
 */
