package `in`.aicortex.iso8583studio.data

import `in`.aicortex.iso8583studio.data.model.DS_EMV_INFO
import `in`.aicortex.iso8583studio.data.model.EMVShowOption
import `in`.aicortex.iso8583studio.data.model.EMV_TAG


class EMVAnalyzer {
    companion object {
        private var m_EMV_INFO: DS_EMV_INFO? = null

        val EMV_INFO: DS_EMV_INFO
            get() {
                if (m_EMV_INFO == null) {
                    m_EMV_INFO = DS_EMV_INFO()
                    try {
                        m_EMV_INFO!!.ReadXml("emvinfo.xml")
                    } catch (e: Exception) {
                        // Empty catch block, same as original
                    }
                }
                return m_EMV_INFO!!
            }

        fun SaveEMVInfo(data: DS_EMV_INFO) {
            data.WriteXml("emvinfo.xml")
        }

        fun GetEMVTAGs(source: ByteArray): List<EMV_TAG> {
            val emvTags = mutableListOf<EMV_TAG>()
            val index = intArrayOf(0) // Using array to simulate 'ref' parameter

            while (index[0] + 3 < source.size) {
                val emvTag = EMV_TAG(source, index)
                emvTags.add(emvTag)
            }

            return emvTags
        }

        fun getFullDescription(source: ByteArray, options: EMVShowOption): String {
            val stringBuilder = StringBuilder()

            for (emvTag in GetEMVTAGs(source)) {
                stringBuilder.append(emvTag.toString(options))
                stringBuilder.append("\r\n")
            }

            return stringBuilder.toString()
        }
    }
}