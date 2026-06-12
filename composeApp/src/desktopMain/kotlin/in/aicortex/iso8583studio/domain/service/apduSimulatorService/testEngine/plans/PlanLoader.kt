package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.plans

import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.testEngine.TestPlan
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Loads custom JSON test plans from disk. Each "*.json" file in the directory is decoded as a
 * [TestPlan]. Malformed files are skipped (logged to stderr) so a single bad file does not break
 * the UI.
 */
object PlanLoader {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Resolve the directory where custom plans live. If [custom] is blank, falls back to
     * `~/.iso8583studio/test-plans`. The directory is created if it does not yet exist.
     */
    fun resolveDir(custom: String): Path {
        val path = if (custom.isBlank()) {
            Paths.get(System.getProperty("user.home"), ".iso8583studio", "test-plans")
        } else {
            Paths.get(custom)
        }
        runCatching { Files.createDirectories(path) }
        return path
    }

    /** Load every `*.json` file in [dir] as a [TestPlan]. */
    fun load(dir: Path): List<TestPlan> {
        if (!Files.isDirectory(dir)) return emptyList()
        return Files.newDirectoryStream(dir, "*.json").use { stream ->
            stream
                .filter { it.isRegularFile() && it.extension.equals("json", ignoreCase = true) }
                .mapNotNull { path ->
                    try {
                        json.decodeFromString<TestPlan>(path.readText())
                    } catch (t: Throwable) {
                        System.err.println("PlanLoader: skipping ${path.fileName}: ${t.message}")
                        null
                    }
                }
                .sortedBy { it.name }
        }
    }
}
