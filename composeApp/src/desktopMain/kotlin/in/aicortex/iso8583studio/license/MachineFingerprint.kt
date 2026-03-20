package `in`.aicortex.iso8583studio.license

import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest

/**
 * Computes a hardware-derived machine fingerprint that survives
 * application reinstalls and data directory deletion.
 *
 * Platform-specific primary sources:
 *  - macOS:   IOPlatformUUID via system_profiler
 *  - Linux:   /etc/machine-id or /var/lib/dbus/machine-id
 *  - Windows: HKLM\SOFTWARE\Microsoft\Cryptography\MachineGuid
 *
 * Additional entropy: CPU identifier, hostname.
 * Output: 64-char hex SHA-256.
 */
object MachineFingerprint {

    @Volatile
    private var cached: String? = null

    fun compute(): String {
        cached?.let { return it }
        val fp = doCompute()
        cached = fp
        return fp
    }

    private fun doCompute(): String {
        val os = System.getProperty("os.name", "").lowercase()
        val primary = when {
            os.contains("mac") -> macUUID()
            os.contains("linux") -> linuxMachineId()
            os.contains("win") -> windowsMachineGuid()
            else -> ""
        }
        val cpu = cpuIdentifier()
        val host = runCatching { java.net.InetAddress.getLocalHost().hostName }.getOrDefault("")
        val raw = listOf(primary, cpu, host)
            .filter { it.isNotBlank() }
            .joinToString("|")
        return sha256Hex(raw)
    }

    private fun macUUID(): String {
        return try {
            val result = execCommand("ioreg", "-rd1", "-c", "IOPlatformExpertDevice")
            val regex = Regex("\"IOPlatformUUID\"\\s*=\\s*\"([^\"]+)\"")
            regex.find(result)?.groupValues?.get(1) ?: ""
        } catch (_: Exception) {
            try {
                val result = execCommand("system_profiler", "SPHardwareDataType")
                val regex = Regex("Hardware UUID:\\s*(\\S+)")
                regex.find(result)?.groupValues?.get(1) ?: ""
            } catch (_: Exception) { "" }
        }
    }

    private fun linuxMachineId(): String {
        return try {
            java.io.File("/etc/machine-id").readText().trim()
        } catch (_: Exception) {
            try {
                java.io.File("/var/lib/dbus/machine-id").readText().trim()
            } catch (_: Exception) { "" }
        }
    }

    private fun windowsMachineGuid(): String {
        return try {
            val result = execCommand(
                "reg", "query",
                "HKLM\\SOFTWARE\\Microsoft\\Cryptography",
                "/v", "MachineGuid"
            )
            val regex = Regex("MachineGuid\\s+REG_SZ\\s+(\\S+)")
            regex.find(result)?.groupValues?.get(1) ?: ""
        } catch (_: Exception) { "" }
    }

    private fun cpuIdentifier(): String {
        val os = System.getProperty("os.name", "").lowercase()
        return try {
            when {
                os.contains("mac") || os.contains("linux") -> {
                    execCommand("uname", "-m").trim()
                }
                os.contains("win") -> {
                    System.getenv("PROCESSOR_IDENTIFIER") ?: ""
                }
                else -> ""
            }
        } catch (_: Exception) { "" }
    }

    private fun execCommand(vararg cmd: String): String {
        val process = ProcessBuilder(*cmd)
            .redirectErrorStream(true)
            .start()
        val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
        process.waitFor()
        return output
    }

    internal fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
