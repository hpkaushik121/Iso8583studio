package `in`.aicortex.iso8583studio.data.model

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class DS_EMV_INFO {
    // Collections to store the data
    val EMV_TAGS = mutableListOf<EMV_TAG>()
    val TAG_BITS = mutableListOf<TAG_BIT>()

    fun findByTag( TAG: String): EMV_TAG?{
        return EMV_TAGS.firstOrNull() {
            it.TAG == TAG
        }
    }



    /**
     * Reads XML data from the specified file path.
     * @param filePath Path to the XML file
     * @return 1 if successful
     */
    fun ReadXml(filePath: String): Int {
        try {
            // Clear existing data
            EMV_TAGS.clear()
            TAG_BITS.clear()

            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(File(filePath))

            doc.documentElement.normalize()

            // Read EMV_TAGS nodes
            val tagNodes = doc.getElementsByTagName("EMV_TAGS")
            for (i in 0 until tagNodes.length) {
                val tagNode = tagNodes.item(i)
                if (tagNode.nodeType == Node.ELEMENT_NODE) {
                    val element = tagNode as Element
                    val row = EMV_TAG(
                        TAG = getNodeValue(element, "TAG"),
                        name = getNodeValue(element, "Name"),
                        description = getNodeValue(element, "Description"),
                        type = getNodeValue(element, "Type"),
                        length = getNodeValue(element, "Length").toIntOrNull() ?: 0
                    )
                    EMV_TAGS.add(row)
                }
            }

            // Read TAG_BITS nodes
            val bitNodes = doc.getElementsByTagName("TAG_BITS")
            for (i in 0 until bitNodes.length) {
                val bitNode = bitNodes.item(i)
                if (bitNode.nodeType == Node.ELEMENT_NODE) {
                    val element = bitNode as Element
                    val row = TAG_BIT(
                        getNodeValue(element, "TAG"),
                        getNodeValue(element, "BIT").toIntOrNull() ?: 0,
                        getNodeValue(element, "Description")
                    )
                    TAG_BITS.add(row)
                }
            }

            return 1
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    /**
     * Writes the data to the specified XML file.
     * @param filePath Path where to save the XML file
     */
    fun WriteXml(filePath: String) {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.newDocument()

            // Create root element
            val rootElement = doc.createElement("DS_EMV_INFO")
            rootElement.setAttribute("xmlns", "http://tempuri.org/DS_EMV_INFO.xsd")
            doc.appendChild(rootElement)

            // Add EMV_TAGS nodes
            for (tag in EMV_TAGS) {
                val tagElement = doc.createElement("EMV_TAGS")

                appendChildWithValue(doc, tagElement, "TAG", tag.TAG ?: "")
                appendChildWithValue(doc, tagElement, "Name", tag.name ?: "")
                appendChildWithValue(doc, tagElement, "Description", tag.description ?: "")
                appendChildWithValue(doc, tagElement, "Type", tag.type ?: "")
                appendChildWithValue(doc, tagElement, "Length", tag.length.toString())

                rootElement.appendChild(tagElement)
            }

            // Add TAG_BITS nodes
            for (bit in TAG_BITS) {
                val bitElement = doc.createElement("TAG_BITS")

                appendChildWithValue(doc, bitElement, "TAG", bit.TAG ?: "")
                appendChildWithValue(doc, bitElement, "BIT", bit.BIT.toString())
                appendChildWithValue(doc, bitElement, "Description", bit.description ?: "")

                rootElement.appendChild(bitElement)
            }

            // Write to file
            val transformerFactory = TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

            val source = DOMSource(doc)
            val result = StreamResult(File(filePath))
            transformer.transform(source, result)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Helper method to get node value
    private fun getNodeValue(element: Element, tagName: String): String {
        val nodeList = element.getElementsByTagName(tagName)
        if (nodeList.length > 0) {
            val node = nodeList.item(0)
            if (node.hasChildNodes()) {
                return node.firstChild.nodeValue ?: ""
            }
        }
        return ""
    }

    // Helper method to append child with value
    private fun appendChildWithValue(doc: Document, parent: Element, tagName: String, value: String) {
        val element = doc.createElement(tagName)
        element.appendChild(doc.createTextNode(value))
        parent.appendChild(element)
    }
}