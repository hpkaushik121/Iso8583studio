package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * A skin is just a directory containing a `layout` file plus PNGs, so custom terminal skins are
 * dropped in the same way the SDK's own 40 are.
 *
 * [displayWidth]/[displayHeight] come from the layout's `display` block and are what makes the
 * "skin disagrees with hw.lcd.*" validation possible — a device frame drawn for 1080x2400 around a
 * 720x1440 screen looks broken in a way that is otherwise hard to diagnose.
 */
data class SkinInfo(
    val name: String,
    val path: Path,
    val displayWidth: Int? = null,
    val displayHeight: Int? = null,
) {
    val hasDeclaredSize: Boolean get() = displayWidth != null && displayHeight != null

    fun matches(width: Int, height: Int): Boolean =
        !hasDeclaredSize ||
            (displayWidth == width && displayHeight == height) ||
            // A skin declared in one orientation is legitimately used in the other.
            (displayWidth == height && displayHeight == width)
}

object SkinCatalog {

    fun list(sdk: AndroidSdk): List<SkinInfo> = list(sdk.skinsDir)

    fun list(skinsDir: Path): List<SkinInfo> {
        if (!skinsDir.isDirectory()) return emptyList()
        return runCatching { skinsDir.listDirectoryEntries() }.getOrDefault(emptyList())
            .filter { it.isDirectory() && it.resolve(LAYOUT).exists() }
            .map { read(it) }
            .sortedBy { it.name }
    }

    /** Reads a single skin directory, whether it is an SDK built-in or a user-supplied one. */
    fun read(skinDir: Path): SkinInfo {
        val size = parseLayoutDisplay(skinDir.resolve(LAYOUT))
        return SkinInfo(
            name = skinDir.name,
            path = skinDir,
            displayWidth = size?.first,
            displayHeight = size?.second,
        )
    }

    fun isValidSkin(skinDir: Path): Boolean = skinDir.isDirectory() && skinDir.resolve(LAYOUT).exists()

    /**
     * Pulls `width`/`height` out of the first `display { … }` block:
     *
     *     parts {
     *       device {
     *         display {
     *           width 1080
     *           height 2400
     *
     * Only `parts.device.display` declares a size — the later `layouts` blocks reference parts
     * rather than redeclaring dimensions — so taking the first block is sufficient. Returns null
     * for a malformed or unreadable layout; callers treat that as "unknown", not "zero".
     */
    fun parseLayoutDisplay(layoutFile: Path): Pair<Int, Int>? {
        if (!layoutFile.exists()) return null
        val text = runCatching { layoutFile.readText() }.getOrNull() ?: return null

        var inDisplay = false
        var depth = 0
        var width: Int? = null
        var height: Int? = null

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            if (!inDisplay) {
                if (line.startsWith("display") && line.endsWith("{")) {
                    inDisplay = true
                    depth = 1
                }
                continue
            }

            // Track nesting so a nested block cannot end the display block early.
            if (line.endsWith("{")) { depth++; continue }
            if (line.startsWith("}")) {
                depth--
                if (depth <= 0) break
                continue
            }

            val parts = line.split(Regex("\\s+"), limit = 2)
            if (parts.size != 2) continue
            when (parts[0]) {
                "width" -> width = parts[1].trim().toIntOrNull()
                "height" -> height = parts[1].trim().toIntOrNull()
            }
            if (width != null && height != null) break
        }

        val w = width ?: return null
        val h = height ?: return null
        return w to h
    }

    private const val LAYOUT = "layout"
}
