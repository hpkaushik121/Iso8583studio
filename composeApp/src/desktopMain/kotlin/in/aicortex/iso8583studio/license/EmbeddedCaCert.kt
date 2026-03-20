package `in`.aicortex.iso8583studio.license

/**
 * CA public certificate fragments. Split across multiple constants and
 * reconstructed at runtime to resist trivial extraction / replacement.
 *
 * IMPORTANT: Replace these placeholder fragments with the real CA cert
 * PEM content before production release. The vendor's CA private key
 * must NEVER appear here.
 */
internal object EmbeddedCaCert {

    private val f1 = "MIIEpDCCAoygAwIBAgIUY2xpY2stbGljZW5zZS1jYTAeFw0yNTAxMDEw"
    private val f2 = "MDAwMFoXDTM1MDEwMTAwMDAwMFowRTELMAkGA1UEBhMCSU4xFDASBgNV"
    private val f3 = "BAoMC0FJQ29ydGV4IENBMSAwHgYDVQQDDBdJU084NTgzU3R1ZGlvIExp"
    private val f4 = "Y2Vuc2UgQ0EwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC+"
    private val f5 = "PLACEHOLDER_CA_CERT_CONTENT_REPLACE_BEFORE_RELEASE"

    fun reconstruct(): String {
        return "-----BEGIN CERTIFICATE-----\n" +
                f1 + f2 + f3 + f4 + "\n" + f5 +
                "\n-----END CERTIFICATE-----"
    }
}

/**
 * Server response signing public key fragments.
 * This key is used to verify that license server responses are authentic
 * and have not been forged by a fake server.
 */
internal object EmbeddedServerSigningKey {

    private val k1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA"
    private val k2 = "PLACEHOLDER_SERVER_SIGNING_PUBLIC_KEY_REPLACE_BEFORE_RELEASE"

    fun reconstruct(): String {
        return "-----BEGIN PUBLIC KEY-----\n" +
                k1 + k2 +
                "\n-----END PUBLIC KEY-----"
    }
}
