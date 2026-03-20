package `in`.aicortex.iso8583studio.license

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted + HMAC-signed local license storage.
 *
 * All license data is stored in a single AES-GCM encrypted blob at
 * ~/.iso8583studio/.license_data. The encryption key is derived from
 * PBKDF2(machineFingerprint + appSecret), making the blob unreadable
 * on a different machine and resistant to plain-text editing.
 *
 * Format: [16 bytes IV][32 bytes HMAC-SHA256][N bytes AES-GCM ciphertext]
 *
 * If HMAC verification fails on load, all data is treated as tampered
 * and the caller must force online validation.
 */
object LicenseStorage {

    private const val APP_SECRET = "IS08583Studi0-L1c3ns3-S4lt"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_IV_LENGTH = 16
    private const val GCM_TAG_LENGTH = 128
    private const val HMAC_LENGTH = 32

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val storageDir = File(System.getProperty("user.home"), ".iso8583studio")
    private val dataFile = File(storageDir, ".license_data")
    private val certFile = File(storageDir, "license.cer")
    private val keyFile = File(storageDir, ".license_key")

    @Serializable
    data class StoredData(
        val firstRunAt: Long = 0L,
        val lastValidatedAt: Long = 0L,
        val lastServerResponse: String = "",
        val lastServerResponseSignature: String = "",
        val lastServerNonce: String = "",
        val lastServerTimestamp: Long = 0L,
        val trialRegistered: Boolean = false,
    )

    fun ensureDir() {
        if (!storageDir.exists()) storageDir.mkdirs()
    }

    fun load(): StoredData? {
        if (!dataFile.exists()) return null
        return try {
            val raw = dataFile.readBytes()
            if (raw.size < GCM_IV_LENGTH + HMAC_LENGTH + 1) return null
            val iv = raw.copyOfRange(0, GCM_IV_LENGTH)
            val storedHmac = raw.copyOfRange(GCM_IV_LENGTH, GCM_IV_LENGTH + HMAC_LENGTH)
            val ciphertext = raw.copyOfRange(GCM_IV_LENGTH + HMAC_LENGTH, raw.size)

            val derivedKey = deriveKey()
            val computedHmac = computeHmac(derivedKey, iv + ciphertext)
            if (!storedHmac.contentEquals(computedHmac)) return null

            val plaintext = decrypt(derivedKey, iv, ciphertext)
            json.decodeFromString<StoredData>(String(plaintext, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }

    fun save(data: StoredData) {
        ensureDir()
        val plaintext = json.encodeToString(data).toByteArray(Charsets.UTF_8)
        val derivedKey = deriveKey()
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val ciphertext = encrypt(derivedKey, iv, plaintext)
        val hmac = computeHmac(derivedKey, iv + ciphertext)
        dataFile.writeBytes(iv + hmac + ciphertext)
    }

    fun loadCertificatePem(): String? {
        return if (certFile.exists()) certFile.readText().trim() else null
    }

    fun saveCertificatePem(pem: String) {
        ensureDir()
        certFile.writeText(pem)
    }

    fun hasCertificate(): Boolean = certFile.exists() && certFile.length() > 0

    fun savePrivateKeyPem(pem: String) {
        ensureDir()
        keyFile.writeText(pem)
    }

    fun loadPrivateKeyPem(): String? {
        return if (keyFile.exists()) keyFile.readText().trim() else null
    }

    fun deleteAll() {
        dataFile.delete()
        certFile.delete()
        keyFile.delete()
    }

    private fun deriveKey(): ByteArray {
        val machineId = MachineFingerprint.compute()
        val salt = (machineId + APP_SECRET).toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(APP_SECRET.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun encrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(plaintext)
    }

    private fun decrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun computeHmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}
