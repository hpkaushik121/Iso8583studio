import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilder
import org.xml.sax.InputSource

/**
 * XML Content Formatter for ISO8583Studio
 * Provides comprehensive XML parsing, formatting, and manipulation capabilities
 */
class XmlContentFormatter {

    companion object {
        private const val DEFAULT_INDENT = "  " // 2 spaces
        private const val XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    }

    /**
     * Configuration class for XML formatting options
     */
    data class FormatConfig(
        val prettyPrint: Boolean = true,
        val indent: String = DEFAULT_INDENT,
        val omitXmlDeclaration: Boolean = false,
        val encoding: String = "UTF-8",
        val standalone: Boolean = false
    )

    /**
     * Format XML string with pretty printing
     */
    fun formatXml(xmlContent: String, config: FormatConfig = FormatConfig()): String {
        return try {
            val document = parseXmlString(xmlContent)
            formatDocument(document, config)
        } catch (e: Exception) {
            throw XmlFormattingException("Failed to format XML: ${e.message}", e)
        }
    }

    /**
     * Parse XML string into Document object
     */
    private fun parseXmlString(xmlContent: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.isValidating = false

        val builder: DocumentBuilder = factory.newDocumentBuilder()
        val inputSource = InputSource(StringReader(xmlContent.trim()))

        return builder.parse(inputSource)
    }

    /**
     * Format Document object into formatted XML string
     */
    private fun formatDocument(document: Document, config: FormatConfig): String {
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()

        // Set formatting properties
        transformer.setOutputProperty(OutputKeys.INDENT, if (config.prettyPrint) "yes" else "no")
        transformer.setOutputProperty(OutputKeys.ENCODING, config.encoding)
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, if (config.omitXmlDeclaration) "yes" else "no")
        transformer.setOutputProperty(OutputKeys.STANDALONE, if (config.standalone) "yes" else "no")

        // Set indentation amount (works with most implementations)
        try {
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        } catch (e: Exception) {
            // Ignore if not supported
        }

        val source = DOMSource(document)
        val writer = StringWriter()
        val result = StreamResult(writer)

        transformer.transform(source, result)

        return if (config.prettyPrint) {
            customIndentFormatting(writer.toString(), config.indent)
        } else {
            writer.toString()
        }
    }

    /**
     * Apply custom indentation formatting for better control
     */
    private fun customIndentFormatting(xml: String, indent: String): String {
        val lines = xml.split("\n")
        val result = StringBuilder()
        var indentLevel = 0

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            // Handle different types of XML lines
            when {
                // Closing tags - decrease indent before adding the line
                trimmedLine.startsWith("</") -> {
                    indentLevel = maxOf(0, indentLevel - 1)
                    result.append(indent.repeat(indentLevel))
                    result.append(trimmedLine)
                    result.append("\n")
                }

                // Self-closing tags - no indent change
                trimmedLine.startsWith("<") && trimmedLine.endsWith("/>") -> {
                    result.append(indent.repeat(indentLevel))
                    result.append(trimmedLine)
                    result.append("\n")
                }

                // XML declarations and processing instructions - no indent change
                trimmedLine.startsWith("<?") -> {
                    result.append(indent.repeat(indentLevel))
                    result.append(trimmedLine)
                    result.append("\n")
                }

                // Comments - no indent change
                trimmedLine.startsWith("<!--") -> {
                    result.append(indent.repeat(indentLevel))
                    result.append(trimmedLine)
                    result.append("\n")
                }

                // Opening tags with content on same line
                trimmedLine.startsWith("<") && trimmedLine.contains("</") -> {
                    result.append(indent.repeat(indentLevel))
                    result.append(trimmedLine)
                    result.append("\n")
                    // No indent change as it's a complete element on one line
                }

                // Opening tags - increase indent after adding the line
                trimmedLine.startsWith("<") -> {
                    result.append(indent.repeat(indentLevel))
                    result.append(trimmedLine)
                    result.append("\n")
                    indentLevel++
                }

                // Text content - use current indent level
                else -> {
                    result.append(indent.repeat(indentLevel))
                    result.append(trimmedLine)
                    result.append("\n")
                }
            }
        }

        return result.toString().trimEnd()
    }

    /**
     * Minify XML by removing unnecessary whitespace
     */
    fun minifyXml(xmlContent: String): String {
        val config = FormatConfig(prettyPrint = false, omitXmlDeclaration = false)
        return formatXml(xmlContent, config).replace(Regex("\\s+"), " ")
    }

    /**
     * Validate XML content
     */
    fun validateXml(xmlContent: String): XMLValidationResult {
        return try {
            parseXmlString(xmlContent)
            XMLValidationResult(true, "XML is valid")
        } catch (e: Exception) {
            XMLValidationResult(false, "XML validation failed: ${e.message}")
        }
    }

    /**
     * Convert ISO8583 data to XML format
     * Useful for ISO8583Studio XML format support
     */
    fun convertIso8583ToXml(
        mti: String,
        fields: Map<String, String>,
        fieldDescriptions: Map<String, String> = emptyMap()
    ): String {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.newDocument()

        // Root element
        val root = document.createElement("ISO8583Message")
        document.appendChild(root)

        // Add MTI (Message Type Indicator)
        val mtiElement = document.createElement("MTI")
        mtiElement.setAttribute("value", mti)
        mtiElement.textContent = mti
        root.appendChild(mtiElement)

        // Add fields
        val fieldsElement = document.createElement("Fields")
        root.appendChild(fieldsElement)

        fields.toSortedMap().forEach { (fieldNumber, value) ->
            val fieldElement = document.createElement("Field")
            fieldElement.setAttribute("number", fieldNumber)
            fieldElement.setAttribute("value", value)

            fieldDescriptions[fieldNumber]?.let { description ->
                fieldElement.setAttribute("description", description)
            }

            fieldElement.textContent = value
            fieldsElement.appendChild(fieldElement)
        }

        return formatDocument(document, FormatConfig())
    }

    /**
     * Parse XML back to ISO8583 field map
     */
    fun parseXmlToIso8583(xmlContent: String): Iso8583Data {
        val document = parseXmlString(xmlContent)
        val root = document.documentElement

        if (root.nodeName != "ISO8583Message") {
            throw XmlFormattingException("Invalid ISO8583 XML format: Root element must be 'ISO8583Message'")
        }

        val mtiNodes = root.getElementsByTagName("MTI")
        val mti = if (mtiNodes.length > 0) {
            mtiNodes.item(0).textContent
        } else {
            throw XmlFormattingException("MTI element not found")
        }

        val fields = mutableMapOf<String, String>()
        val descriptions = mutableMapOf<String, String>()

        val fieldNodes = root.getElementsByTagName("Field")
        for (i in 0 until fieldNodes.length) {
            val fieldNode = fieldNodes.item(i) as Element
            val fieldNumber = fieldNode.getAttribute("number")
            val fieldValue = fieldNode.getAttribute("value")
            val fieldDescription = fieldNode.getAttribute("description")

            if (fieldNumber.isNotEmpty() && fieldValue.isNotEmpty()) {
                fields[fieldNumber] = fieldValue
                if (fieldDescription.isNotEmpty()) {
                    descriptions[fieldNumber] = fieldDescription
                }
            }
        }

        return Iso8583Data(mti, fields, descriptions)
    }

    /**
     * Create XML template for ISO8583 message configuration
     */
    fun createIso8583XmlTemplate(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<ISO8583Message>
    <MTI value="0200">0200</MTI>
    <Fields>
        <Field number="2" value="4111111111111111" description="Primary Account Number">4111111111111111</Field>
        <Field number="3" value="000000" description="Processing Code">000000</Field>
        <Field number="4" value="000000001000" description="Transaction Amount">000000001000</Field>
        <Field number="11" value="000001" description="Systems Trace Audit Number">000001</Field>
        <Field number="12" value="121530" description="Time, Local Transaction">121530</Field>
        <Field number="13" value="0521" description="Date, Local Transaction">0521</Field>
        <Field number="22" value="022" description="POS Entry Mode">022</Field>
        <Field number="25" value="00" description="POS Condition Code">00</Field>
        <Field number="41" value="TERM0001" description="Card Acceptor Terminal ID">TERM0001</Field>
        <Field number="42" value="MERCHANT001     " description="Card Acceptor ID Code">MERCHANT001     </Field>
    </Fields>
</ISO8583Message>"""
    }

    /**
     * Enhanced XML formatting with better tag handling
     */
    fun formatXmlAdvanced(xmlContent: String, config: FormatConfig = FormatConfig()): String {
        return try {
            val document = parseXmlString(xmlContent)
            if (config.prettyPrint) {
                formatDocumentAdvanced(document, config)
            } else {
                formatDocument(document, config)
            }
        } catch (e: Exception) {
            throw XmlFormattingException("Failed to format XML: ${e.message}", e)
        }
    }

    /**
     * Advanced document formatting with proper DOM traversal
     */
    private fun formatDocumentAdvanced(document: Document, config: FormatConfig): String {
        val result = StringBuilder()

        if (!config.omitXmlDeclaration) {
            result.append("<?xml version=\"1.0\" encoding=\"${config.encoding}\"?>")
            result.append("\n")
        }

        formatNodeAdvanced(document.documentElement, result, 0, config.indent)

        return result.toString().trimEnd()
    }

    /**
     * Recursively format XML nodes with proper indentation
     */
    private fun formatNodeAdvanced(node: Node, result: StringBuilder, depth: Int, indent: String) {
        when (node.nodeType) {
            Node.ELEMENT_NODE -> {
                val element = node as Element
                val indentation = indent.repeat(depth)

                // Opening tag
                result.append(indentation)
                result.append("<${element.tagName}")

                // Add attributes
                val attributes = element.attributes
                for (i in 0 until attributes.length) {
                    val attr = attributes.item(i)
                    result.append(" ${attr.nodeName}=\"${escapeXmlAttribute(attr.nodeValue)}\"")
                }

                val hasChildElements = hasChildElements(element)
                val hasTextContent = element.textContent?.trim()?.isNotEmpty() == true && !hasChildElements

                if (element.childNodes.length == 0) {
                    // Self-closing tag
                    result.append("/>")
                    result.append("\n")
                } else if (hasTextContent && !hasChildElements) {
                    // Element with only text content
                    result.append(">")
                    result.append(escapeXmlText(element.textContent.trim()))
                    result.append("</${element.tagName}>")
                    result.append("\n")
                } else {
                    // Element with child elements
                    result.append(">")
                    result.append("\n")

                    // Process child nodes
                    val childNodes = element.childNodes
                    for (i in 0 until childNodes.length) {
                        val child = childNodes.item(i)
                        when (child.nodeType) {
                            Node.ELEMENT_NODE -> formatNodeAdvanced(child, result, depth + 1, indent)
                            Node.TEXT_NODE -> {
                                val text = child.textContent?.trim()
                                if (!text.isNullOrEmpty()) {
                                    result.append(indent.repeat(depth + 1))
                                    result.append(escapeXmlText(text))
                                    result.append("\n")
                                }
                            }
                            Node.CDATA_SECTION_NODE -> {
                                result.append(indent.repeat(depth + 1))
                                result.append("<![CDATA[${child.textContent}]]>")
                                result.append("\n")
                            }
                        }
                    }

                    // Closing tag
                    result.append(indentation)
                    result.append("</${element.tagName}>")
                    result.append("\n")
                }
            }
        }
    }

    /**
     * Check if element has child elements (not just text nodes)
     */
    private fun hasChildElements(element: Element): Boolean {
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            if (childNodes.item(i).nodeType == Node.ELEMENT_NODE) {
                return true
            }
        }
        return false
    }

    /**
     * Escape XML text content
     */
    private fun escapeXmlText(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    /**
     * Escape XML attribute values
     */
    private fun escapeXmlAttribute(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Sort XML attributes alphabetically
     */
    private fun sortXmlAttributes(xml: String): String {
        // This is a simplified implementation
        // In production, you might want to use a more robust XML parser
        return xml.lines().map { line ->
            if (line.trim().startsWith("<") && line.contains("=")) {
                sortAttributesInLine(line)
            } else {
                line
            }
        }.joinToString("\n")
    }

    private fun sortAttributesInLine(line: String): String {
        val tagMatch = Regex("<([^\\s>]+)([^>]*)>").find(line.trim())
        if (tagMatch != null) {
            val tagName = tagMatch.groupValues[1]
            val attributesPart = tagMatch.groupValues[2].trim()

            if (attributesPart.isNotEmpty()) {
                val attributes = parseAttributes(attributesPart)
                val sortedAttributes = attributes.toSortedMap()
                val formattedAttributes = sortedAttributes.map { "${it.key}=\"${it.value}\"" }.joinToString(" ")
                return line.replace(tagMatch.value, "<$tagName $formattedAttributes>")
            }
        }
        return line
    }

    private fun parseAttributes(attributesPart: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        val regex = Regex("(\\w+)=\"([^\"]*?)\"")
        regex.findAll(attributesPart).forEach { match ->
            attributes[match.groupValues[1]] = match.groupValues[2]
        }
        return attributes
    }

    private fun addAttributeSpacing(xml: String): String {
        return xml.replace(Regex("(\\w+=\"[^\"]*\")\\s*(\\w+=\")"), "$1 $2")
    }
}

/**
 * Data class for ISO8583 message data
 */
data class Iso8583Data(
    val mti: String,
    val fields: Map<String, String>,
    val fieldDescriptions: Map<String, String> = emptyMap()
)

/**
 * XML validation result
 */
data class XMLValidationResult(
    val isValid: Boolean,
    val message: String,
    val errors: List<String> = emptyList()
)

/**
 * Custom exception for XML formatting errors
 */
class XmlFormattingException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Usage examples and utility functions
 */
object XmlFormatterExamples {

    fun demonstrateBasicFormatting() {
        val formatter = XmlContentFormatter()

        // Test with unformatted XML that has nested elements
        val unformattedXml = """<root><header><messageType>0200</messageType><timestamp>2024-05-25T10:30:00</timestamp></header><body><field id="2">4111111111111111</field><field id="3">000000</field><field id="4">000000001000</field></body></root>"""

        println("Original XML:")
        println(unformattedXml)
        println("\n" + "=".repeat(50) + "\n")

        // Basic formatting
        val formatted = formatter.formatXml(unformattedXml)
        println("Basic formatted XML:")
        println(formatted)
        println("\n" + "=".repeat(50) + "\n")

        // Advanced formatting
        val advancedFormatted = formatter.formatXmlAdvanced(unformattedXml)
        println("Advanced formatted XML:")
        println(advancedFormatted)
        println("\n" + "=".repeat(50) + "\n")

        // Custom formatting configuration
        val customConfig = XmlContentFormatter.FormatConfig(
            prettyPrint = true,
            indent = "    ", // 4 spaces
            omitXmlDeclaration = true
        )
        val customFormatted = formatter.formatXmlAdvanced(unformattedXml, customConfig)
        println("Custom formatted XML (4 spaces, no declaration):")
        println(customFormatted)
    }

    fun demonstrateComplexXmlFormatting() {
        val formatter = XmlContentFormatter()

        // Complex XML with mixed content
        val complexXml = """<ISO8583Message version="1.0"><MTI value="0200">0200</MTI><Fields><Field number="2" type="LLVAR" description="Primary Account Number">4111111111111111</Field><Field number="3" type="FIXED" description="Processing Code">000000</Field><Field number="4" type="FIXED" description="Transaction Amount"><Amount currency="USD">000000001000</Amount></Field><Field number="11" type="FIXED" description="STAN">000001</Field></Fields><Security><Encryption algorithm="AES-256"><Key>ABC123DEF456</Key></Encryption></Security></ISO8583Message>"""

        println("Complex XML formatting:")
        println("Original:")
        println(complexXml)
        println("\nFormatted:")
        println(formatter.formatXmlAdvanced(complexXml))
    }

    fun demonstrateEdgeCases() {
        val formatter = XmlContentFormatter()

        // Self-closing tags
        val selfClosingXml = """<root><item1/><item2 value="test"/><item3>content</item3></root>"""
        println("Self-closing tags:")
        println(formatter.formatXmlAdvanced(selfClosingXml))
        println()

        // XML with CDATA
        val cdataXml = """<root><data><![CDATA[Some <special> content & more]]></data><normal>regular content</normal></root>"""
        println("XML with CDATA:")
        println(formatter.formatXmlAdvanced(cdataXml))
        println()

        // Empty elements
        val emptyXml = """<root><empty></empty><selfClose/><withContent>text</withContent></root>"""
        println("Empty elements:")
        println(formatter.formatXmlAdvanced(emptyXml))
    }

    fun demonstrateIso8583Conversion() {
        val formatter = XmlContentFormatter()

        // Convert ISO8583 data to XML
        val fields = mapOf(
            "2" to "4111111111111111",
            "3" to "000000",
            "4" to "000000001000",
            "11" to "000001"
        )

        val descriptions = mapOf(
            "2" to "Primary Account Number",
            "3" to "Processing Code",
            "4" to "Transaction Amount",
            "11" to "Systems Trace Audit Number"
        )

        val xmlOutput = formatter.convertIso8583ToXml("0200", fields, descriptions)
        println("ISO8583 as XML:")
        println(xmlOutput)

        // Parse XML back to ISO8583 data
        val parsedData = formatter.parseXmlToIso8583(xmlOutput)
        println("\nParsed back - MTI: ${parsedData.mti}")
        println("Fields: ${parsedData.fields}")
    }

    fun demonstrateValidation() {
        val formatter = XmlContentFormatter()

        // Valid XML
        val validXml = "<root><item>value</item></root>"
        val validResult = formatter.validateXml(validXml)
        println("Valid XML result: ${validResult.message}")

        // Invalid XML - missing closing tag
        val invalidXml = "<root><item>value</item>"
        val invalidResult = formatter.validateXml(invalidXml)
        println("Invalid XML result: ${invalidResult.message}")

        // Invalid XML - malformed tag
        val malformedXml = "<root><item>value</iterm></root>"
        val malformedResult = formatter.validateXml(malformedXml)
        println("Malformed XML result: ${malformedResult.message}")
    }
}