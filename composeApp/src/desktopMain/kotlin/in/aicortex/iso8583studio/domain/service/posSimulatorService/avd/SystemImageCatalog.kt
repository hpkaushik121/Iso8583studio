package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * Which system image an AVD boots. `abi` empty means "resolve to the host architecture at prepare
 * time", so a profile shared between an Apple Silicon Mac and a Linux CI box does the right thing
 * on both.
 */
@Serializable
data class SystemImageRef(
    val apiLevel: Int = 30,
    val tag: String = "google_apis",
    val abi: String = "",
) {
    val sdkPackage: String get() = "system-images;android-$apiLevel;$tag;$abi"

    /** The value written to `image.sysdir.1`. The trailing slash matches what the SDK writes. */
    val sysdir: String get() = "system-images/android-$apiLevel/$tag/$abi/"

    /**
     * Play Store images are `user` builds: `adb root` is refused, so `/system` cannot be made
     * writable and the whole `-writable-system` + prop-spoofing path fails. Always prefer
     * `google_apis`.
     */
    val isPlayStore: Boolean get() = tag.contains("playstore", ignoreCase = true)

    val hasGoogleApis: Boolean get() = tag.startsWith("google_apis")

    fun withHostAbi(): SystemImageRef =
        if (abi.isNotEmpty()) this else copy(abi = SystemImageCatalog.hostAbi())

    override fun toString(): String = "android-$apiLevel / $tag / ${abi.ifEmpty { "(host)" }}"
}

data class InstalledSystemImage(
    val ref: SystemImageRef,
    val tagDisplay: String,
    val revision: Int,
    val vendor: String?,
    val path: Path,
    val sizeBytes: Long,
    val gpuSupport: Boolean,
)

/**
 * Enumerates installed system images by scanning the SDK filesystem.
 *
 * The filesystem is the primary source rather than `sdkmanager --list`: it is offline, takes a few
 * milliseconds, and is authoritative about what will actually boot. `sdkmanager` is reserved for
 * discovering *downloadable* images, which is network-bound and must never sit on a render path.
 *
 * Layout is `system-images/android-<api>/<tag>/<abi>/source.properties`, with verified keys
 * `AndroidVersion.ApiLevel`, `SystemImage.Abi`, `SystemImage.TagId`, `SystemImage.TagDisplay`,
 * `Pkg.Revision`, `SystemImage.GpuSupport` and `Addon.VendorId`.
 */
object SystemImageCatalog {

    fun scanInstalled(sdk: AndroidSdk): List<InstalledSystemImage> = scanInstalled(sdk.systemImagesDir)

    fun scanInstalled(systemImagesDir: Path): List<InstalledSystemImage> {
        if (!systemImagesDir.isDirectory()) return emptyList()
        val result = mutableListOf<InstalledSystemImage>()

        for (apiDir in systemImagesDir.safeList()) {
            if (!apiDir.isDirectory()) continue
            for (tagDir in apiDir.safeList()) {
                if (!tagDir.isDirectory()) continue
                for (abiDir in tagDir.safeList()) {
                    if (!abiDir.isDirectory()) continue
                    parseImage(abiDir)?.let(result::add)
                }
            }
        }
        return result.sortedWith(
            compareByDescending<InstalledSystemImage> { it.ref.apiLevel }
                .thenBy { it.ref.tag }
                .thenBy { it.ref.abi }
        )
    }

    private fun parseImage(abiDir: Path): InstalledSystemImage? {
        val props = abiDir.resolve("source.properties")
        if (!props.exists()) return null
        val parsed = runCatching { readProperties(props) }.getOrNull() ?: return null

        // Fall back to the directory names, which encode the same triple, so an image with a
        // truncated source.properties still shows up rather than vanishing silently.
        val apiLevel = parsed["AndroidVersion.ApiLevel"]?.toIntOrNull()
            ?: abiDir.parent?.parent?.name?.removePrefix("android-")?.toIntOrNull()
            ?: return null
        val abi = parsed["SystemImage.Abi"] ?: abiDir.name
        val tag = parsed["SystemImage.TagId"] ?: abiDir.parent?.name ?: return null

        return InstalledSystemImage(
            ref = SystemImageRef(apiLevel = apiLevel, tag = tag, abi = abi),
            tagDisplay = parsed["SystemImage.TagDisplay"] ?: tag,
            revision = parsed["Pkg.Revision"]?.substringBefore('.')?.toIntOrNull() ?: 0,
            vendor = parsed["Addon.VendorId"],
            path = abiDir,
            sizeBytes = imageSize(abiDir),
            gpuSupport = parsed["SystemImage.GpuSupport"]?.toBoolean() ?: false,
        )
    }

    /** Sums the top-level `*.img` files — the bulk of the payload, without walking the whole tree. */
    private fun imageSize(dir: Path): Long = dir.safeList()
        .filter { it.name.endsWith(".img") }
        .sumOf { runCatching { it.fileSize() }.getOrDefault(0L) }

    private fun readProperties(path: Path): Map<String, String> =
        path.readText().lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
            .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }

    private fun Path.safeList(): List<Path> =
        runCatching { listDirectoryEntries() }.getOrDefault(emptyList())

    /**
     * The ABI that can actually be hardware-accelerated here. Apple Silicon has no HVF support for
     * x86_64 guests, so an x86_64 image falls back to TCG software emulation — 10–50x slower and
     * effectively unusable. The validator turns a mismatch into an error for that reason.
     */
    fun hostAbi(): String = when (val arch = System.getProperty("os.arch").orEmpty().lowercase()) {
        "aarch64", "arm64" -> "arm64-v8a"
        "x86_64", "amd64" -> "x86_64"
        "x86", "i386", "i686" -> "x86"
        else -> arch
    }
}
