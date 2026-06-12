package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Filesystem-backed JSON store for [CardProfile]s. One profile per file, file name is the
 * profile id with a ".json" suffix. The store is intentionally dumb: no caching, no locking;
 * callers handle concurrency.
 */
class ProfileStore(private val dir: Path) {

    init {
        Files.createDirectories(dir)
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Load every "*.json" in [dir] as a [CardProfile]. Files that fail to parse are skipped. */
    fun list(): List<CardProfile> {
        Files.newDirectoryStream(dir, "*.json").use { stream ->
            return stream
                .filter { it.isRegularFile() && it.extension.equals("json", ignoreCase = true) }
                .mapNotNull { path ->
                    runCatching { json.decodeFromString<CardProfile>(path.readText()) }.getOrNull()
                }
                .sortedBy { it.name }
        }
    }

    fun load(id: String): CardProfile {
        val path = dir.resolve("$id.json")
        return json.decodeFromString(path.readText())
    }

    fun save(p: CardProfile) {
        val path = dir.resolve("${p.id}.json")
        path.writeText(json.encodeToString(p))
    }

    fun delete(id: String) {
        Files.deleteIfExists(dir.resolve("$id.json"))
    }
}
