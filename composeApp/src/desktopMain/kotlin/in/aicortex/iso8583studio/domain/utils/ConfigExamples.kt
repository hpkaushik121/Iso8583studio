package `in`.aicortex.iso8583studio.domain.utils

import `in`.aicortex.iso8583studio.data.model.CodeFormat
import `in`.aicortex.iso8583studio.data.model.GatewayConfig

// Usage Example with YAML Configuration
object ConfigExamples {

    fun getNestedJsonConfig(): String = """
formatType: "JSON"
mti:
  key: "msgType"
fieldMappings:
  "2":
    key: "F002"
  "14":
    key: "F014"
  "3":
    key: "F003"
  "4":
    nestedKey: "transaction.amount"
  "7":
    nestedKey: "header.timestamp"
  "11":
    nestedKey: "header.traceNumber"
  "37":
    nestedKey: "response.retrievalRef"
  "39":
    nestedKey: "response.code"
  "41":
    nestedKey: "terminal.id"
  "42":
    nestedKey: "merchant.id"
settings:
  prettyPrint: true
""".trimIndent()

    fun getTemplateJsonConfig(): String = """
formatType: "JSON"
mti:
  template: "header.{mti}.type"
fieldMappings:
  "2":
    template: "card.{data}"
  "14":
    template: "card.expiry"
  "3":
    nestedKey: "transaction.code"
  "4":
    template: "transaction.amount.{data}"
  "39":
    key: "responseCode"
    staticValue: "00"
settings:
  prettyPrint: true
""".trimIndent()

    fun getSimpleXmlConfig(): String = """
formatType: "XML"
mti:
  key: "messageType"
fieldMappings:
  "2":
    nestedKey: "cardData.number"
  "3":
    key: "processingCode"
  "4":
    key: "amount"
  "39":
    key: "responseCode"
settings:
  rootElement: "iso8583"
  prettyPrint: true
""".trimIndent()

    fun getKeyValueConfig(): String = """
formatType: "PLAIN_TEXT"
mti:
  key: "MTI"
fieldMappings:
  "2":
    key: "PAN"
  "3":
    key: "PROC_CODE"
  "4":
    key: "AMOUNT"
  "11":
    key: "TRACE"
  "39":
    key: "RESP_CODE"
settings:
  delimiter: "|"
  keyValueSeparator: "="
""".trimIndent()
}

// Example usage demonstrating the complete flow
fun demonstrateUsage() {
    val configManager = FormatConfigManager()

    // Load configurations
    configManager.loadConfig("NESTED_JSON", ConfigExamples.getNestedJsonConfig())
    configManager.loadConfig("SIMPLE_XML", ConfigExamples.getSimpleXmlConfig())
    configManager.loadConfig("KEY_VALUE", ConfigExamples.getKeyValueConfig())

    val factory = EnhancedIso8583Factory(configManager)

    // Example: Convert from JSON to XML to Key-Value
    val jsonInput = """
    {
        "msgType":"0200",
        "F002": "4111111111111111",
        "transaction": {
            "processingCode": "000000",
            "amount": "000000050000"
        },
        "terminal": {
            "id": "TERM0001"
        },
        "merchant": {
            "id": "MERCH000001"
        }
    }
    """.trimIndent()

    // Create gateway config (you'll need to implement this based on your existing structure)
    val gatewayConfig = createDefaultGatewayConfig()

    // Parse JSON input
    val iso8583Data = factory.createFromFormat(
        jsonInput.toByteArray(),
        CodeFormat.JSON,
        "NESTED_JSON",
        gatewayConfig
    )
    // Convert to XML
    val jsonOutput = factory.convertFormat(iso8583Data, CodeFormat.JSON, "NESTED_JSON")
    println("Json Output:")
    println(String(jsonOutput))

    // Convert to XML
    val xmlOutput = factory.convertFormat(iso8583Data, CodeFormat.XML, "SIMPLE_XML")
    println("XML Output:")
    println(String(xmlOutput))

    // Convert to Key-Value
    val kvOutput = factory.convertFormat(iso8583Data, CodeFormat.PLAIN_TEXT, "KEY_VALUE")
    println("Key-Value Output:")
    println(String(kvOutput))

    // Convert to HEX
    val hexOutput = factory.convertFormat(iso8583Data, CodeFormat.HEX, "NESTED_JSON")
    println("HEX Output:")
    println(String(hexOutput))
}

fun createDefaultGatewayConfig(): GatewayConfig {
    return GatewayConfig(
        id = 1,
        name = "Test"
    )
}

fun main(){
    demonstrateUsage()
}
