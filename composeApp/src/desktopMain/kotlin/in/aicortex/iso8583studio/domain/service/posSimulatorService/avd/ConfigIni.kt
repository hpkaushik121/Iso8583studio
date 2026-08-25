package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * An AVD's `config.ini` (and the sibling pointer `<name>.ini`), read/overlaid/written without
 * losing anything we did not author.
 *
 * ## Why this preserves rather than regenerates
 *
 * We create AVDs with a bare `avdmanager create avd` and then write the real hardware settings
 * ourselves, because `avdmanager` cannot set RAM or screen geometry. That means round-tripping a
 * file the tooling just wrote, so anything we do not model must survive untouched — otherwise a
 * newer emulator's new keys get silently dropped.
 *
 * ## Format facts, verified against real AVDs on this machine
 *
 * **The separator style varies.** Android Studio wrote `Pixel_6` as `AvdId = Pixel_6` (spaces),
 * while `Kozen_N2` is `AvdId=Custom` (no spaces). Both are valid and both occur, so the style is
 * detected on read and reused on write. Our flow always creates via `avdmanager`, reads the result,
 * overlays and writes back — so we inherit whatever style the installed tooling emits.
 *
 * Other verified facts: keys are ASCII-sorted, line endings are LF (no CR in any of the three AVDs
 * inspected), and empty values occur and must be preserved (`fastboot.chosenSnapshotFile=`).
 *
 * Values are kept as the raw strings that were read. `disk.dataPartition.size` appears as
 * `6442450944` in one AVD and `6G` in another; both are accepted by the emulator, so normalising on
 * read would rewrite files we only meant to inspect. Normalisation happens in [withOverlay], and
 * only for the keys actually being changed.
 */
class ConfigIni private constructor(
    private val entries: LinkedHashMap<String, String>,
    val separator: String,
) {

    operator fun get(key: String): String? = entries[key]

    operator fun contains(key: String): Boolean = entries.containsKey(key)

    val keys: Set<String> get() = entries.keys

    val size: Int get() = entries.size

    fun toMap(): Map<String, String> = LinkedHashMap(entries)

    /**
     * Returns a copy with [values] applied on top. Keys absent from [values] are left exactly as
     * they were — that is the forward-compatibility guarantee. A null value removes the key.
     *
     * Pass a [schema] to canonicalise the incoming values (booleans to `yes`/`no`, disk sizes to
     * their compact form). Values already present and unchanged are never rewritten.
     */
    fun withOverlay(values: Map<String, String?>, schema: HardwarePropertiesSchema? = null): ConfigIni {
        val merged = LinkedHashMap(entries)
        for ((key, raw) in values) {
            if (raw == null) {
                merged.remove(key)
            } else {
                merged[key] = schema?.normalize(key, raw) ?: raw
            }
        }
        return ConfigIni(merged, separator)
    }

    /**
     * Keys whose values differ between this file and [other], as `key -> (this, other)`.
     * Drives the on-disk drift diff, where the user edited an AVD in Android Studio behind us.
     */
    fun diff(other: ConfigIni): Map<String, Pair<String?, String?>> {
        val result = LinkedHashMap<String, Pair<String?, String?>>()
        for (key in (entries.keys + other.entries.keys).sorted()) {
            val mine = entries[key]
            val theirs = other.entries[key]
            if (mine != theirs) result[key] = mine to theirs
        }
        return result
    }

    /** ASCII-sorted, LF-terminated, matching what the SDK tooling itself produces. */
    fun render(): String = buildString {
        for (key in entries.keys.sortedWith(ASCII)) {
            append(key).append(separator).append(entries[key].orEmpty()).append('\n')
        }
    }

    fun write(path: Path) {
        path.createParentDirectories()
        path.writeText(render())
    }

    override fun toString(): String = "ConfigIni(${entries.size} keys, separator='$separator')"

    companion object {
        /** `avdmanager`'s style, and the one we use for files we author from scratch. */
        const val COMPACT_SEPARATOR = "="

        /** Android Studio's style on newer versions. */
        const val SPACED_SEPARATOR = " = "

        /**
         * Plain lexicographic ordering. Key names are ASCII, so this matches `LC_ALL=C sort`,
         * which is what produced the on-disk ordering we are reproducing.
         */
        private val ASCII = Comparator<String> { a, b -> a.compareTo(b) }

        fun parse(text: String): ConfigIni {
            val entries = LinkedHashMap<String, String>()
            var separator: String? = null

            for (rawLine in text.lineSequence()) {
                val line = rawLine.trim()
                // `#` and `;` comments do not appear in files the SDK writes, but tolerate them
                // rather than turning a hand-annotated file into a key named "# note".
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue

                val at = line.indexOf('=')
                if (at < 0) continue

                val rawKey = line.substring(0, at)
                val rawValue = line.substring(at + 1)
                if (separator == null) {
                    separator = if (rawKey.endsWith(" ") && rawValue.startsWith(" ")) {
                        SPACED_SEPARATOR
                    } else {
                        COMPACT_SEPARATOR
                    }
                }

                val key = rawKey.trim()
                if (key.isEmpty()) continue
                entries[key] = rawValue.trim()
            }
            return ConfigIni(entries, separator ?: COMPACT_SEPARATOR)
        }

        fun read(path: Path): ConfigIni =
            if (path.exists()) parse(path.readText()) else ConfigIni(LinkedHashMap(), COMPACT_SEPARATOR)

        fun of(
            values: Map<String, String>,
            separator: String = COMPACT_SEPARATOR,
        ): ConfigIni = ConfigIni(LinkedHashMap(values), separator)

        /**
         * The pointer file `<avdHome>/<name>.ini` that makes an AVD visible to `avdmanager list avd`
         * and to Android Studio.
         *
         * Worth knowing: `Kozen_N2` on this machine has a `.avd` directory but *no* pointer file, so
         * it does not appear in `avdmanager list avd` at all. Code that enumerates AVDs must scan
         * directories as well as pointer files, or it will silently miss AVDs the user can see in
         * their filesystem.
         */
        fun pointerIni(avdDir: Path, apiLevel: Int): ConfigIni = of(
            linkedMapOf(
                "avd.ini.encoding" to "UTF-8",
                "path" to avdDir.toAbsolutePath().toString(),
                "path.rel" to "avd/${avdDir.fileName}",
                "target" to "android-$apiLevel",
            )
        )
    }
}
