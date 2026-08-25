package `in`.aicortex.iso8583studio.domain.store

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Filesystem-backed JSON store: one entity per file, named `<id>.json`.
 *
 * Generalised from `apduSimulatorService.profile.ProfileStore`, which was the third place this
 * exact shape was about to be written (card profiles, hardware profiles, terminal models). That
 * class is now a thin subclass, so none of its call sites changed.
 *
 * Intentionally dumb: no caching, no locking, no watch. Callers handle concurrency.
 */
open class JsonDirStore<T : Any>(
    protected val dir: Path,
    private val serializer: KSerializer<T>,
    private val idOf: (T) -> String,
    private val sortKey: (T) -> String = idOf,
) {
    init {
        Files.createDirectories(dir)
    }

    protected val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Every parseable `*.json` in the directory. Unparseable files are skipped, not fatal. */
    fun list(): List<T> {
        Files.newDirectoryStream(dir, "*.json").use { stream ->
            return stream
                .filter { it.isRegularFile() && it.extension.equals("json", ignoreCase = true) }
                .mapNotNull { path -> runCatching { decode(path.readText()) }.getOrNull() }
                .sortedBy(sortKey)
        }
    }

    /** Throws if absent — callers that tolerate a miss should use [find]. */
    fun load(id: String): T = decode(pathFor(id).readText())

    fun find(id: String): T? = runCatching { load(id) }.getOrNull()

    fun exists(id: String): Boolean = Files.exists(pathFor(id))

    fun save(value: T) {
        pathFor(idOf(value)).writeText(encode(value))
    }

    fun saveAll(values: Iterable<T>) = values.forEach(::save)

    fun delete(id: String): Boolean = Files.deleteIfExists(pathFor(id))

    /**
     * Writes the new id's file and removes the old one.
     *
     * Worth having as a first-class operation: the original `ProfileStore` had no rename, so
     * editing an id in the profile editor silently orphaned the previous file instead of moving it.
     */
    fun rename(oldId: String, updated: T): Boolean {
        save(updated)
        return if (idOf(updated) != oldId) delete(oldId) else false
    }

    /** Seeds built-in samples only when the directory is empty. */
    fun seedIfEmpty(samples: () -> List<T>) {
        if (list().isEmpty()) saveAll(samples())
    }

    fun encode(value: T): String = json.encodeToString(serializer, value)

    fun decode(text: String): T = json.decodeFromString(serializer, text)

    private fun pathFor(id: String): Path = dir.resolve("$id.json")

    companion object {
        /** Everything this app persists lives under `~/.iso8583studio/`. */
        fun appDir(vararg segments: String): Path =
            Path.of(System.getProperty("user.home"), ".iso8583studio", *segments)
    }
}
