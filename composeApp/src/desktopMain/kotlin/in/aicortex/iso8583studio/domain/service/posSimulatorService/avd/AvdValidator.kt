package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

import kotlin.math.sqrt

/**
 * Catches the AVD misconfigurations that otherwise present as a ten-minute non-boot or as a
 * mysterious runtime failure much later.
 *
 * Every issue carries a remediation string, because the whole point is that the UI can tell the
 * user what to do rather than just going red. Some carry a [ValidationIssue.fix] producing the
 * property changes that resolve it, so the UI can offer a one-click correction.
 */
object AvdValidator {

    /** Below this the system will not boot at all. */
    private const val MIN_RAM_MB = 512L

    /** Below this an API 30+ image boots but thrashes. */
    private const val COMFORTABLE_RAM_MB = 1024L

    /** The emulator refuses to create an SD card smaller than this. */
    private val MIN_SDCARD = DiskSize.parse("9M")!!

    private val AVD_NAME = Regex("[A-Za-z0-9._-]+")

    fun validate(input: AvdValidationInput): List<ValidationIssue> = buildList {
        validateName(input, this)
        validateMemory(input, this)
        validateDisplay(input, this)
        validateStorage(input, this)
        validateImage(input, this)
        validateGraphics(input, this)
        validateSkin(input, this)
    }

    private fun validateName(input: AvdValidationInput, out: MutableList<ValidationIssue>) {
        val name = input.avdName
        if (name.isBlank()) {
            out += ValidationIssue(Severity.ERROR, null, "AVD name is empty.", "Give the AVD a name.")
            return
        }
        if (!AVD_NAME.matches(name)) {
            // avdmanager rejects spaces outright; a real device definition on this machine is
            // literally named `KIOSK 27"`, which is why we never derive AVD names from those.
            out += ValidationIssue(
                Severity.ERROR, null,
                "AVD name \"$name\" contains characters avdmanager rejects.",
                "Use only letters, digits, dot, underscore and hyphen.",
            )
        }
    }

    private fun validateMemory(input: AvdValidationInput, out: MutableList<ValidationIssue>) {
        val ramMb = input.int("hw.ramSize")
        if (ramMb == null) {
            out += ValidationIssue(Severity.WARNING, "hw.ramSize", "RAM is unset.",
                "The emulator will guess from the screen size; set it explicitly.")
        } else {
            if (ramMb < MIN_RAM_MB) {
                out += ValidationIssue(Severity.ERROR, "hw.ramSize",
                    "RAM of ${ramMb} MB is below the ${MIN_RAM_MB} MB minimum.",
                    "Raise RAM to at least 1024 MB.",
                    fix = { mapOf("hw.ramSize" to "1024") })
            } else if (ramMb < COMFORTABLE_RAM_MB && input.image.apiLevel >= 30) {
                out += ValidationIssue(Severity.WARNING, "hw.ramSize",
                    "RAM of ${ramMb} MB is low for Android ${input.image.apiLevel}.",
                    "Android 11+ thrashes below 1024 MB. 2048 MB is a good default.",
                    fix = { mapOf("hw.ramSize" to "2048") })
            }
            input.hostRamMb?.let { hostRam ->
                if (ramMb > hostRam * 4 / 10) {
                    out += ValidationIssue(Severity.WARNING, "hw.ramSize",
                        "RAM of ${ramMb} MB is over 40% of this machine's ${hostRam} MB.",
                        "Running several simulators at once will exhaust host memory.")
                }
            }
        }

        val heap = input.int("vm.heapSize")
        if (ramMb != null && heap != null && heap > ramMb / 2) {
            out += ValidationIssue(Severity.WARNING, "vm.heapSize",
                "VM heap (${heap} MB) is more than half of RAM (${ramMb} MB).",
                "Lower the heap, or raise RAM.")
        }
    }

    private fun validateDisplay(input: AvdValidationInput, out: MutableList<ValidationIssue>) {
        val width = input.int("hw.lcd.width")
        val height = input.int("hw.lcd.height")
        val density = input.int("hw.lcd.density")

        if (width == null || height == null) {
            out += ValidationIssue(Severity.ERROR, "hw.lcd.width",
                "Screen resolution is not set.", "Set both hw.lcd.width and hw.lcd.height.")
            return
        }
        if (width <= 0 || height <= 0) {
            out += ValidationIssue(Severity.ERROR, "hw.lcd.width",
                "Screen resolution ${width}x${height} is invalid.", "Both dimensions must be positive.")
            return
        }
        if (density == null || density <= 0) {
            out += ValidationIssue(Severity.ERROR, "hw.lcd.density",
                "Screen density is not set.", "Set hw.lcd.density, e.g. 320 for a 720x1440 terminal.")
            return
        }

        // Implied physical size. A 480x480 @ 160 dpi device is 4.2" and entirely legitimate — the
        // Kozen N2 profile on this machine is exactly that — so only flag genuinely absurd results.
        val diagonalIn = sqrt((width.toDouble() * width + height.toDouble() * height)) / density
        if (diagonalIn < 2.0 || diagonalIn > 15.0) {
            out += ValidationIssue(Severity.WARNING, "hw.lcd.density",
                "Resolution ${width}x${height} at ${density} dpi implies a %.1f\" screen."
                    .format(diagonalIn),
                "Check density against resolution — one of them is probably wrong.")
        }
    }

    private fun validateStorage(input: AvdValidationInput, out: MutableList<ValidationIssue>) {
        for (key in listOf("disk.dataPartition.size", "sdcard.size")) {
            val raw = input.properties[key]?.takeIf { it.isNotBlank() } ?: continue
            val parsed = DiskSize.parse(raw)
            if (parsed == null) {
                out += ValidationIssue(Severity.ERROR, key,
                    "\"$raw\" is not a valid size.",
                    "Use a plain byte count or a suffixed value such as 512M, 2G, 6G.")
                continue
            }
            if (key == "sdcard.size" && parsed.bytes > 0 && parsed < MIN_SDCARD) {
                out += ValidationIssue(Severity.ERROR, key,
                    "SD card of $raw is below the 9M minimum.",
                    "The emulator refuses to create it. Use at least 9M, or disable the SD card.",
                    fix = { mapOf("sdcard.size" to "512M") })
            }
            if (key == "disk.dataPartition.size" && input.image.apiLevel >= 30 &&
                parsed.bytes in 1 until 2 * DiskSize.GB
            ) {
                out += ValidationIssue(Severity.WARNING, key,
                    "Internal storage of $raw is small for Android ${input.image.apiLevel}.",
                    "Installing an APK plus user data will fill it. 4G or more is safer.",
                    fix = { mapOf("disk.dataPartition.size" to "6G") })
            }
        }
    }

    private fun validateImage(input: AvdValidationInput, out: MutableList<ValidationIssue>) {
        val image = input.image.withHostAbi()

        if (image.abi.isNotEmpty() && image.abi != input.hostAbi) {
            out += ValidationIssue(Severity.ERROR, "abi.type",
                "Image ABI ${image.abi} does not match this host (${input.hostAbi}).",
                "There is no hardware acceleration for a foreign ABI — the emulator falls back to " +
                    "software emulation, 10-50x slower and effectively unusable. Pick an " +
                    "${input.hostAbi} image.")
        }

        if (input.installedImages.none { it.apiLevel == image.apiLevel && it.tag == image.tag && it.abi == image.abi }) {
            out += ValidationIssue(Severity.ERROR, "image.sysdir.1",
                "System image ${image.sdkPackage} is not installed.",
                "Download it from the System Image tab, or run: sdkmanager \"${image.sdkPackage}\"")
        }

        if (image.isPlayStore) {
            // Not merely advisory: the entire device-emulation approach depends on a writable
            // /system, so this one silently breaks everything downstream.
            out += ValidationIssue(Severity.ERROR, "tag.id",
                "Play Store images are `user` builds and cannot be rooted.",
                "`adb root` is refused, so -writable-system and ro.product.* spoofing both fail. " +
                    "Use the google_apis image for the same API level.",
                fix = { mapOf("tag.id" to "google_apis", "PlayStore.enabled" to "no") })
        }

        val playStoreEnabled = input.bool("PlayStore.enabled") ?: false
        if (playStoreEnabled && !image.isPlayStore) {
            out += ValidationIssue(Severity.ERROR, "PlayStore.enabled",
                "PlayStore.enabled is set but the image tag is \"${image.tag}\".",
                "Play Store requires a google_apis_playstore image.",
                fix = { mapOf("PlayStore.enabled" to "no") })
        }

        if (!image.hasGoogleApis && !image.isPlayStore) {
            out += ValidationIssue(Severity.INFO, "tag.id",
                "The \"${image.tag}\" image has no Google APIs.",
                "Fine for most terminal apps; switch to google_apis if the payload APK needs them.")
        }

        // A910S, DX8000 and most Android terminals are Android 10 (API 29), for which no arm64
        // image is published. Say so once, here, rather than letting it surface as a boot failure.
        if (image.apiLevel < 30 && image.abi == "arm64-v8a") {
            out += ValidationIssue(Severity.ERROR, "image.sysdir.1",
                "No arm64-v8a system image exists for API ${image.apiLevel}.",
                "Android 10 terminals have no arm64 emulator image upstream. Use API 30, or run " +
                    "x86_64 on an Intel/Linux host.")
        }
    }

    private fun validateGraphics(input: AvdValidationInput, out: MutableList<ValidationIssue>) {
        val gpuEnabled = input.bool("hw.gpu.enabled")
        val gpuMode = input.properties["hw.gpu.mode"]?.takeIf { it.isNotBlank() }
        if (gpuEnabled == false && gpuMode != null && gpuMode != "off" && gpuMode != "guest") {
            out += ValidationIssue(Severity.WARNING, "hw.gpu.mode",
                "GPU is disabled but hw.gpu.mode is \"$gpuMode\".",
                "The mode is ignored while hw.gpu.enabled is no. Enable the GPU or clear the mode.")
        }

        input.int("hw.cpu.ncore")?.let { cores ->
            if (cores < 1) {
                out += ValidationIssue(Severity.ERROR, "hw.cpu.ncore",
                    "CPU core count must be at least 1.", "Set hw.cpu.ncore to 2 or 4.",
                    fix = { mapOf("hw.cpu.ncore" to "4") })
            }
        }
    }

    private fun validateSkin(input: AvdValidationInput, out: MutableList<ValidationIssue>) {
        val skin = input.skin ?: return
        val width = input.int("hw.lcd.width") ?: return
        val height = input.int("hw.lcd.height") ?: return
        val frameShown = input.bool("showDeviceFrame") ?: true

        if (frameShown && !skin.matches(width.toInt(), height.toInt())) {
            out += ValidationIssue(Severity.WARNING, "skin.name",
                "Skin \"${skin.name}\" declares ${skin.displayWidth}x${skin.displayHeight} but the " +
                    "screen is ${width}x${height}.",
                "The device frame will be drawn around the wrong-sized screen. Pick a matching " +
                    "skin, or turn off the device frame.",
                fix = { mapOf("showDeviceFrame" to "no") })
        }
    }
}

data class AvdValidationInput(
    val avdName: String,
    /** Effective properties: schema defaults, then hardware profile, then per-AVD overrides. */
    val properties: Map<String, String>,
    val image: SystemImageRef,
    val installedImages: List<SystemImageRef> = emptyList(),
    val skin: SkinInfo? = null,
    val hostAbi: String = SystemImageCatalog.hostAbi(),
    val hostRamMb: Long? = HostInfo.physicalRamMb(),
) {
    internal fun int(key: String): Long? = properties[key]?.trim()?.toLongOrNull()
    internal fun bool(key: String): Boolean? =
        properties[key]?.let { HardwarePropertiesSchema.parseBoolean(it) }
}

data class ValidationIssue(
    val severity: Severity,
    /** The property this is about, or null for AVD-level issues. */
    val key: String?,
    val message: String,
    val remediation: String,
    /** Property changes that resolve the issue, for a one-click fix. */
    val fix: (() -> Map<String, String>)? = null,
)

enum class Severity { ERROR, WARNING, INFO }

object HostInfo {
    /**
     * Physical RAM, via `com.sun.management.OperatingSystemMXBean`, reached reflectively so we do
     * not take a compile dependency on a JDK-internal module.
     *
     * Note `Runtime.maxMemory()` is emphatically not this — it is the JVM heap ceiling, which would
     * make every RAM comparison wrong.
     */
    fun physicalRamMb(): Long? = runCatching {
        val bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean()
        val method = bean.javaClass.methods.firstOrNull {
            it.name == "getTotalMemorySize" || it.name == "getTotalPhysicalMemorySize"
        } ?: return null
        method.isAccessible = true
        (method.invoke(bean) as? Long)?.let { it / DiskSize.MB }
    }.getOrNull()
}
