package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.readText

/**
 * The emulator's declared hardware-property surface, parsed from
 * `$SDK/emulator/lib/hardware-properties.ini`.
 *
 * This is the authoritative schema behind an AVD's `config.ini`. It currently declares ~153
 * properties and grows with every emulator release (`hw.sensor.hinge.*`, `hw.display1..3.*` and
 * `hw.arc` are all recent additions), which is exactly why AVD hardware is modelled as a
 * `Map<String, String>` keyed by these names rather than as a typed struct: a struct would need a
 * code change per emulator release and would silently drop anything it did not model.
 *
 * ## File format (verified against emulator 36.3.10)
 *
 * Records are delimited by the next `name =` line, **not** by blank lines, and the key order
 * within a record is *not* stable — `hw.screen` lists `enum` before `default`, while
 * `disk.cachePartition.size` lists `abstract` before it. So we accumulate keys into a map and
 * flush on the next `name`.
 *
 * Other quirks the parser must tolerate:
 *  - `default =` is frequently present but empty (`hw.cpu.model`, `hw.sdCard.path`).
 *  - `description` is sometimes absent entirely (`hw.touchpad0.width`).
 *  - Comment blocks (`#`) interleave freely between and inside records.
 *  - Three of the eleven `enum` lines end in a literal `...` (`emulated, none, webcam0, ...` and
 *    `freeform, ...`), meaning an open set — those must render as an editable combo, never as a
 *    closed dropdown.
 */
class HardwarePropertiesSchema(val properties: Map<String, HardwareProperty>) {

    operator fun get(name: String): HardwareProperty? = properties[name]

    val names: Set<String> get() = properties.keys

    val size: Int get() = properties.size

    /** Schema-declared defaults, the base layer of `AvdSpec.effectiveProperties`. */
    fun defaults(): Map<String, String> =
        properties.values.filter { it.default.isNotEmpty() }.associate { it.name to it.default }

    /**
     * Canonicalises a value for writing into `config.ini`: booleans become `yes`/`no` regardless of
     * how they were typed, disk sizes become their most compact exact form. Unknown properties and
     * unparseable values pass through untouched so we never destroy something we didn't understand.
     */
    fun normalize(name: String, raw: String): String {
        val prop = properties[name] ?: return raw
        return when (prop.type) {
            PropertyType.BOOLEAN -> parseBoolean(raw)?.let { formatBoolean(it) } ?: raw
            PropertyType.DISK_SIZE -> DiskSize.parse(raw)?.toString() ?: raw
            else -> raw
        }
    }

    companion object {
        /** Location of the schema inside an SDK installation. */
        fun fileIn(sdkRoot: Path): Path = sdkRoot.resolve("emulator/lib/hardware-properties.ini")

        /**
         * Parses the schema, preferring the SDK's own copy so we always match the installed
         * emulator, and falling back to the snapshot bundled in resources when no SDK is present
         * (fresh machine, unit tests, CI).
         *
         * Results are cached on (path, mtime, size) — reparsing 153 records on every recomposition
         * would be visible in the UI.
         */
        fun load(sdkRoot: Path?): HardwarePropertiesSchema {
            val file = sdkRoot?.let(::fileIn)
            if (file != null && file.exists()) {
                val key = CacheKey(
                    file.toString(),
                    runCatching { file.getLastModifiedTime().toMillis() }.getOrDefault(0L),
                    runCatching { Files.size(file) }.getOrDefault(0L),
                )
                cached?.let { if (it.first == key) return it.second }
                val parsed = runCatching { parse(file.readText()) }.getOrNull()
                if (parsed != null && parsed.properties.isNotEmpty()) {
                    cached = key to parsed
                    return parsed
                }
            }
            return bundled()
        }

        /** The snapshot shipped with the app, used when no SDK is installed. */
        fun bundled(): HardwarePropertiesSchema {
            bundledCache?.let { return it }
            val text = HardwarePropertiesSchema::class.java
                .getResourceAsStream(BUNDLED_RESOURCE)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return HardwarePropertiesSchema(emptyMap())
            return parse(text).also { bundledCache = it }
        }

        fun parse(text: String): HardwarePropertiesSchema {
            val result = LinkedHashMap<String, HardwareProperty>()
            var current: MutableMap<String, String>? = null

            fun flush() {
                val record = current ?: return
                val name = record["name"]?.takeIf { it.isNotEmpty() } ?: return
                val (enumValues, openEnded) = parseEnum(record["enum"])
                result[name] = HardwareProperty(
                    name = name,
                    type = PropertyType.from(record["type"]),
                    default = record["default"].orEmpty(),
                    abstract = record["abstract"].orEmpty(),
                    description = record["description"].orEmpty(),
                    enumValues = enumValues,
                    enumOpenEnded = openEnded,
                )
                current = null
            }

            for (rawLine in text.lineSequence()) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) continue

                val separator = line.indexOf('=')
                if (separator < 0) continue
                val key = line.substring(0, separator).trim()
                val value = line.substring(separator + 1).trim()

                if (key == "name") {
                    flush()
                    current = mutableMapOf("name" to value)
                } else {
                    current?.put(key, value)
                }
            }
            flush()
            return HardwarePropertiesSchema(result)
        }

        /**
         * Splits an `enum = a, b, c` line. A trailing `...` marks an open set — the emulator
         * accepts values beyond the listed ones (e.g. `webcam1`), so the UI must not constrain it.
         */
        private fun parseEnum(raw: String?): Pair<List<String>, Boolean> {
            if (raw.isNullOrBlank()) return emptyList<String>() to false
            val tokens = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val openEnded = tokens.any { it == "..." }
            return tokens.filter { it != "..." } to openEnded
        }

        /**
         * Lenient on input, strict on output. `config.ini` uses `yes`/`no`, but `hw.arc = false`
         * occurs in the wild (it is present in a hand-built AVD on this machine), so accept both.
         */
        fun parseBoolean(raw: String): Boolean? = when (raw.trim().lowercase()) {
            "yes", "true", "1", "on" -> true
            "no", "false", "0", "off" -> false
            else -> null
        }

        fun formatBoolean(value: Boolean): String = if (value) "yes" else "no"

        private const val BUNDLED_RESOURCE = "/avd/hardware-properties.ini"

        private data class CacheKey(val path: String, val mtime: Long, val size: Long)

        @Volatile
        private var cached: Pair<CacheKey, HardwarePropertiesSchema>? = null

        @Volatile
        private var bundledCache: HardwarePropertiesSchema? = null
    }
}

data class HardwareProperty(
    val name: String,
    val type: PropertyType,
    val default: String,
    val abstract: String,
    val description: String,
    val enumValues: List<String> = emptyList(),
    /** True when the enum line ended in `...`, i.e. the listed values are not exhaustive. */
    val enumOpenEnded: Boolean = false,
) {
    /** A closed enum can drive a strict dropdown; an open one needs a free-text combo. */
    val isClosedEnum: Boolean get() = enumValues.isNotEmpty() && !enumOpenEnded

    /** Falls back to the property name so the UI always has something to show. */
    val label: String get() = abstract.ifEmpty { name }
}

enum class PropertyType {
    BOOLEAN, INTEGER, STRING, DISK_SIZE;

    companion object {
        /** Unknown types degrade to STRING rather than failing — new emulator releases add types. */
        fun from(raw: String?): PropertyType = when (raw?.trim()?.lowercase()) {
            "boolean" -> BOOLEAN
            "integer" -> INTEGER
            "disksize" -> DISK_SIZE
            else -> STRING
        }
    }
}
