package `in`.aicortex.iso8583studio


import `in`.aicortex.iso8583studio.data.NCCParameter
import `in`.aicortex.iso8583studio.data.PermanentConnection
import `in`.aicortex.iso8583studio.data.model.VerificationError
import `in`.aicortex.iso8583studio.data.model.VerificationException
import java.net.Socket

class NCCHandler(private val m_NCCParameters: Array<NCCParameter>?) {
    private var m_ANY_NII: NCCParameter? = null
    private var m_TolerateInvalidNII: Boolean = false

    init {
        if (m_NCCParameters != null) {
            for (i in m_NCCParameters.indices) {
                if (9000 == m_NCCParameters[i].nii) {
                    m_ANY_NII = m_NCCParameters[i]
                }
            }
        }
    }

    fun isActive(): Boolean = m_NCCParameters != null

    var tolerateInvalidNII: Boolean
        get() = m_TolerateInvalidNII
        set(value) { m_TolerateInvalidNII = value }

    operator fun get(nii: Int): NCCParameter {
        if (nii < 1 || nii > 999) {
            throw VerificationException("NII is invalid", VerificationError.INVALID_NII)
        }

        m_NCCParameters?.let { params ->
            for (i in params.indices) {
                if (nii == params[i].nii) {
                    return params[i]
                }
            }

            m_ANY_NII?.let { return it }

            if (m_TolerateInvalidNII) {
                for (i in params.indices) {
                    if (params[i].connection != null) {
                        return params[i]
                    }
                }
            }
        }

        throw VerificationException("NII is invalid", VerificationError.INVALID_NII)
    }

    suspend fun close() {
        m_NCCParameters?.let { params ->
            for (i in params.indices) {
                val connection = params[i].connection

                if (params[i].pConnection != null) {
                    params[i].pConnection?.removeSourceNii(params[i].sourceNii)
                } else if (connection != null) {
                    try {
                        connection.shutdownInput()
                        connection.shutdownOutput()
                        connection.close()
                    } catch (e: Exception) {
                        // Handle exception if needed
                    }
                }
            }
        }
    }

    companion object {
        val aboutUs: String = "Sourabh Kaushik, sk@aicortex.in"
    }
}


