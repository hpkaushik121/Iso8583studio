package `in`.aicortex.iso8583studio.domain.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import `in`.aicortex.iso8583studio.data.Iso8583Data
import `in`.aicortex.iso8583studio.data.model.BitLength
import `in`.aicortex.iso8583studio.data.model.BitType
import `in`.aicortex.iso8583studio.data.updateBit
import `in`.aicortex.iso8583studio.data.model.CodeFormat
import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.data.model.MessageLengthType
import kotlinx.serialization.Serializable
import java.nio.charset.Charset

// Simplified configuration focused on key mapping only
@Serializable
data class FormatMappingConfig(
    val formatType: CodeFormat,
    val mti: MtiMapping,
    val fieldMappings: Map<String, FieldMapping>,
    val settings: FormatSettings = FormatSettings()
)

@Serializable
data class MtiMapping(
    val key: String? = null,                    // Simple key like "msgType"
    val nestedKey: String? = null,      // Nested key like "header.messageType"
    val template: String? = null        // Template like "header.{mti}.type"
)

@Serializable
data class FieldMapping(
    val key: String? = null,            // Simple key like "F002"
    val nestedKey: String? = null,      // Nested key like "card.pan"
    val template: String? = null,       // Template like "card.{field2}.number"
    val staticValue: String? = null     // Static value to always set
)

@Serializable
data class FormatSettings(
    val encoding: String = "UTF-8",
    val prettyPrint: Boolean = true,
    val rootElement: String? = null,    // For XML
    val delimiter: String = "|",        // For Key-Value
    val keyValueSeparator: String = "="
)

// Enhanced Iso8583Data with format support
fun Iso8583Data.packWithFormat(
    outputFormat: CodeFormat,
    messageLengthType: MessageLengthType,
    mappingConfig: FormatMappingConfig
): ByteArray {
    return when (outputFormat) {
        CodeFormat.JSON -> packAsJson(mappingConfig).toByteArray(Charset.forName(mappingConfig.settings.encoding))
        CodeFormat.XML -> packAsXml(mappingConfig).toByteArray(Charset.forName(mappingConfig.settings.encoding))
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
    from: Int = 0, length: Int = inputData.size
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
            this.unpack(binaryData) // Use existing unpack method
        }

        CodeFormat.BYTE_ARRAY -> this.unpackByteArray(inputData,from,length) // Use existing unpack method
    }
}



// JSON Format Implementation
private fun Iso8583Data.packAsJson(config: FormatMappingConfig): String {
    val objectMapper = ObjectMapper()
    val rootMap = mutableMapOf<String, Any>()

    // Add MTI
    val mtiValue = this.messageType
    when {
        config.mti.template != null -> {
            val processedTemplate = config.mti.template!!.replace("{mti}", mtiValue)
            setNestedValue(rootMap, processedTemplate, mtiValue)
        }

        config.mti.nestedKey != null -> {
            setNestedValue(rootMap, config.mti.nestedKey!!, mtiValue)
        }

        else -> {
            rootMap[config.mti.key!!] = mtiValue
        }
    }

    // Add fields
    config.fieldMappings.forEach { (fieldNum, mapping) ->
        val bitIndex = fieldNum.toInt() - 1
        if (this.isBitSet(bitIndex)) {
            val fieldValue = this.getValue(bitIndex) ?: return@forEach

            when {
                mapping.template != null -> {
                    val processedTemplate = mapping.template!!
                        .replace("{field$fieldNum}", fieldValue)
                        .replace("{data}", fieldValue)

                    // Extract key path from template
                    val keyPath = extractKeyFromTemplate(processedTemplate)
                    setNestedValue(rootMap, keyPath, fieldValue)
                }

                mapping.nestedKey != null -> {
                    setNestedValue(rootMap, mapping.nestedKey!!, fieldValue)
                }

                mapping.key != null -> {
                    rootMap[mapping.key!!] = fieldValue
                }
            }
        }

        // Handle static values
        mapping.staticValue?.let { staticVal ->
            when {
                mapping.nestedKey != null -> setNestedValue(rootMap, mapping.nestedKey!!, staticVal)
                mapping.key != null -> rootMap[mapping.key!!] = staticVal
            }
        }
    }

    return if (config.settings.prettyPrint) {
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootMap)
    } else {
        objectMapper.writeValueAsString(rootMap)
    }
}

private fun Iso8583Data.unpackFromJson(jsonString: String, config: FormatMappingConfig) {
    val objectMapper = ObjectMapper()
    val jsonNode = objectMapper.readTree(jsonString)

    // Extract MTI
    val mti = when {
        config.mti.nestedKey != null -> getNestedValue(jsonNode, config.mti.nestedKey!!)
        else -> jsonNode.get(config.mti.key)?.asText()
    }
    mti?.let { this.messageType = it }

    // Extract fields
    config.fieldMappings.forEach { (fieldNum, mapping) ->
        val fieldValue = when {
            mapping.nestedKey != null -> getNestedValue(jsonNode, mapping.nestedKey!!)
            mapping.key != null -> jsonNode.get(mapping.key!!)?.asText()
            else -> null
        }

        fieldValue?.let {
            val bitIndex = fieldNum.toInt() - 1
            this[bitIndex + 1]?.updateBit(bitIndex, it)
        }
    }
}

// XML Format Implementation
private fun Iso8583Data.packAsXml(config: FormatMappingConfig): String {
    val xmlMapper = XmlMapper()
    val rootMap = mutableMapOf<String, Any>()

    // Add MTI
    val mtiKey = config.mti.nestedKey?.split(".")?.last() ?: config.mti.key
    rootMap[mtiKey!!] = this.messageType

    // Add fields
    config.fieldMappings.forEach { (fieldNum, mapping) ->
        val bitIndex = fieldNum.toInt() - 1
        if (this.isBitSet(bitIndex)) {
            val fieldValue = this.getValue(bitIndex) ?: return@forEach
            val xmlKey = mapping.nestedKey?.split(".")?.last() ?: mapping.key ?: "field_$fieldNum"

            if (mapping.nestedKey?.contains(".") == true) {
                // Handle nested XML structure
                val parts = mapping.nestedKey!!.split(".")
                val parentKey = parts[0]
                val childKey = parts[1]

                if (rootMap[parentKey] !is MutableMap<*, *>) {
                    rootMap[parentKey] = mutableMapOf<String, Any>()
                }
                @Suppress("UNCHECKED_CAST")
                (rootMap[parentKey] as MutableMap<String, Any>)[childKey] = fieldValue
            } else {
                rootMap[xmlKey] = fieldValue
            }
        }
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
    val xmlMapper = XmlMapper()
    val xmlNode = xmlMapper.readTree(xmlString)

    // Navigate to root element if configured
    val workingNode = config.settings.rootElement?.let { rootElement ->
        xmlNode.get(rootElement) ?: xmlNode
    } ?: xmlNode

    // Extract MTI
    val mtiKey = config.mti.nestedKey?.split(".")?.last() ?: config.mti.key
    val mti = workingNode.get(mtiKey)?.asText()
    mti?.let { this.messageType = it }

    // Extract fields
    config.fieldMappings.forEach { (fieldNum, mapping) ->
        val fieldValue = if (mapping.nestedKey?.contains(".") == true) {
            val parts = mapping.nestedKey!!.split(".")
            val parent = workingNode.get(parts[0])
            parent?.get(parts[1])?.asText()
        } else {
            val xmlKey = mapping.nestedKey?.split(".")?.last() ?: mapping.key ?: "field_$fieldNum"
            workingNode.get(xmlKey)?.asText()
        }

        fieldValue?.let {
            val bitIndex = fieldNum.toInt() - 1
            this[bitIndex + 1]?.updateBit(bitIndex, it)
        }
    }
}

// Key-Value Format Implementation
private fun Iso8583Data.packAsKeyValue(config: FormatMappingConfig): String {
    val pairs = mutableListOf<String>()
    val delimiter = config.settings.delimiter
    val kvSeparator = config.settings.keyValueSeparator

    // Add MTI
    val mtiKey = config.mti.key
    pairs.add("$mtiKey$kvSeparator${this.messageType}")

    // Add fields
    config.fieldMappings.forEach { (fieldNum, mapping) ->
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
    dataMap[config.mti.key]?.let { this.messageType = it }

    // Extract fields
    config.fieldMappings.forEach { (fieldNum, mapping) ->
        val key = mapping.key ?: "F$fieldNum"
        dataMap[key]?.let { value ->
            val bitIndex = fieldNum.toInt() - 1
            this[bitIndex + 1]?.updateBit(bitIndex, value)
        }
    }
}

// Utility functions for nested key handling
private fun setNestedValue(map: MutableMap<String, Any>, path: String, value: String) {
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

    return current.asText()
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
