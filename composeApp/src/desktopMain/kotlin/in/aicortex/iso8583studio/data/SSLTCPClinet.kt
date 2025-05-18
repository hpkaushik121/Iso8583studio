package `in`.aicortex.iso8583studio.data


import java.io.FileInputStream
import java.io.IOException
import java.net.Socket
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Kotlin implementation of SSLTcpClient that wraps SSL functionality around a Socket
 */
class SSLTcpClient private constructor(private val client: GatewayClient, bIsIncoming: Boolean) {
    private val m_IsServer: Boolean = bIsIncoming
    private lateinit var sslContext: SSLContext
    private var _sslStream: SslStream? = null

    /**
     * Creates an SslStream wrapper that provides similar functionality to .NET's SslStream
     */
    inner class SslStream(private val socket: Socket) {
        private val sslSocket: SSLSocket

        init {
            try {
                // Create the SSL socket factory
                val socketFactory = when {
                    m_IsServer -> {
                        sslContext.serverSocketFactory.createServerSocket(socket.localPort)
                        throw UnsupportedOperationException("Server socket not supported in this implementation")
                    }
                    else -> sslContext.socketFactory
                }

                // Create SSL Socket by wrapping the existing socket
                sslSocket = if (socket is SSLSocket) {
                    socket
                } else {
                    socketFactory.createSocket(
                        socket,
                        socket.inetAddress.hostAddress,
                        socket.port,
                        true
                    ) as SSLSocket
                }

                // Configure SSL parameters
                sslSocket.useClientMode = !m_IsServer
                sslSocket.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.1", "TLSv1")
                sslSocket.enabledCipherSuites = sslSocket.supportedCipherSuites

                // Start SSL handshake
                sslSocket.startHandshake()

            } catch (e: IOException) {
                throw SSLException("Failed to initialize SSL socket: ${e.message}", e)
            }
        }

        // Properties to mimic .NET's SslStream
        var readTimeout: Int
            get() = sslSocket.soTimeout
            set(value) { sslSocket.soTimeout = value }

        val isAuthenticated: Boolean
            get() = sslSocket.isConnected

        val isEncrypted: Boolean
            get() = true

        val isMutuallyAuthenticated: Boolean
            get() = sslSocket.needClientAuth

        val isSigned: Boolean
            get() = true

        val isServer: Boolean
            get() = !sslSocket.useClientMode

        // Methods to read and write data
        fun read(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size - offset): Int {
            return sslSocket.inputStream.read(buffer, offset, length)
        }

        fun write(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size - offset) {
            sslSocket.outputStream.write(buffer, offset, length)
            sslSocket.outputStream.flush()
        }

        fun close() {
            try {
                sslSocket.close()
            } catch (e: IOException) {
                // Ignore closing errors
            }
        }
    }

    // Public property to access the SSL stream
    val sslStream: SslStream?
        get() = _sslStream

    init {
        try {
            setupSslContext(bIsIncoming)

            // Create SSL stream based on whether this is an incoming or outgoing connection
            _sslStream = if (bIsIncoming) {
                authenticateAsServer()
            } else {
                authenticateAsClient()
            }

            // Log authentication results
            client.writeServerLog(
                "SSL AUTHENTICATION RESULT: " +
                        "AUTHENTICATED:${_sslStream?.isAuthenticated} , " +
                        "ENCRYPTED:${_sslStream?.isEncrypted} , " +
                        "MUTUALLY AUTHENTICATED:${_sslStream?.isMutuallyAuthenticated} , " +
                        "SIGNED:${_sslStream?.isSigned} , " +
                        "IS SERVER:${_sslStream?.isServer}"
            )

        } catch (e: Exception) {
            throw SSLException("SSL setup failed: ${e.message}", e)
        }
    }

    private fun setupSslContext(bIsIncoming: Boolean) {
        sslContext = SSLContext.getInstance("TLS")

        if (bIsIncoming) {
            // Server setup - load the server certificate
            val certificatePath = client.myGateway?.configuration?.advanceOptions?.m_SslServerCertificatePath
                ?: throw SSLException("Server certificate path not specified")

            val keyManagers = loadKeyManagers(certificatePath)
            val trustManagers = if (client.myGateway?.configuration?.advanceOptions?.sslCheckRemoteCertificate == true) {
                null // Use default trust managers
            } else {
                createTrustAllTrustManager()
            }

            sslContext.init(keyManagers, trustManagers, null)

        } else {
            // Client setup
            val certificatePath = client.myGateway?.configuration?.advanceOptions?.m_SslClientCertificatePath

            val keyManagers = if (!certificatePath.isNullOrEmpty()) {
                loadKeyManagers(certificatePath)
            } else {
                null
            }

            // For client, we usually want to trust the server
            val trustManagers = if (client.myGateway?.configuration?.advanceOptions?.sslCheckRemoteCertificate == true) {
                null // Use default trust managers
            } else {
                createTrustAllTrustManager()
            }

            sslContext.init(keyManagers, trustManagers, null)
        }
    }

    private fun authenticateAsServer(): SslStream {
        val socket = client.firstConnection ?: throw SSLException("No connection available for server")
        return SslStream(socket)
    }

    private fun authenticateAsClient(): SslStream {
        val socket = client.secondConnection ?: throw SSLException("No connection available for client")
        return SslStream(socket)
    }

    private fun loadKeyManagers(certificatePath: String): Array<KeyManager> {
        try {
            val keyStore = KeyStore.getInstance("PKCS12")
            val parts = certificatePath.split(';')

            if (parts.size > 1) {
                // Certificate with password
                val certPath = parts[0]
                val password = parts[1]

                FileInputStream(certPath).use { fis ->
                    keyStore.load(fis, password.toCharArray())
                }
            } else {
                // Certificate without password
                FileInputStream(certificatePath).use { fis ->
                    keyStore.load(fis, null)
                }
            }

            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, parts.getOrNull(1)?.toCharArray())

            return keyManagerFactory.keyManagers

        } catch (e: Exception) {
            throw SSLException("Failed to load certificate: ${e.message}", e)
        }
    }

    private fun createTrustAllTrustManager(): Array<TrustManager> {
        return arrayOf(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                // Trust all clients
                logCertificateInfo(chain)
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                // Trust all servers
                logCertificateInfo(chain)
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }

            private fun logCertificateInfo(chain: Array<X509Certificate>?) {
                chain?.firstOrNull()?.let { certificate ->
                    client.writeServerLog(
                        "Remote Certificate's Information: Subject = \"${certificate.subjectDN}\" " +
                                "Issuer = \"${certificate.issuerDN}\""
                    )
                }
            }
        })
    }

    val isServer: Boolean
        get() = m_IsServer

    companion object {
        /**
         * Creates an SSLTcpClient instance
         *
         * @param client The GatewayClient instance to wrap
         * @param bIsIncoming True if this is an incoming connection (server mode), false for outgoing (client mode)
         * @return SSLTcpClient instance or null if creation fails
         */
        @JvmStatic
        fun createSSLClient(client: GatewayClient, bIsIncoming: Boolean): SSLTcpClient? {
            return try {
                SSLTcpClient(client, bIsIncoming)
            } catch (ex: Exception) {
                client.writeServerLog("SSL AUTHENTICATION FAILED: ${ex.message}")
                null
            }
        }
    }
}

/**
 * Custom exception for SSL-related errors
 */
class SSLException(message: String, cause: Throwable? = null) : Exception(message, cause)