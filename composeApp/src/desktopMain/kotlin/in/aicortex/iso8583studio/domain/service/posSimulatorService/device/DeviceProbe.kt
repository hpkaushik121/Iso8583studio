package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AndroidSdk
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.ProcessRunner
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageRef

/**
 * Reads a device's real specification over adb.
 *
 * This is the only thing that produces [SpecConfidence.VERIFIED_FROM_DEVICE]. Vendor datasheets
 * publish `5" HD` and never the pixel dimensions or density bucket, so every catalog entry starts
 * as a guess — and a guessed replica is not a replica.
 *
 * Probing a real PAX A910S showed why this matters. The datasheet-derived catalog entry was wrong
 * about four things the emulator actually cares about:
 *
 *  - **Android version**: datasheet says 10 (API 29); the unit runs **12 (API 31)**. This is the
 *    good kind of wrong — API 29 has no arm64 system image, but API 31 does, so the emulator can
 *    match the real API level exactly instead of substituting.
 *  - **`ro.product.brand`** is `UNISOC`, not `PAX`. Only `ro.product.manufacturer` is `PAX`.
 *  - **`ro.product.device`/`name`** are the SoC board names (`uis8581e_5h10`), not the model.
 *  - **CPU cores**: 8, not the 4 inferred from "quad-core Cortex-A53".
 *
 * It also found no `android.hardware.nfc` feature despite the terminal having a contactless reader —
 * because on these devices contactless sits behind the vendor DAL, not Android's NFC stack. That is
 * why [toTerminalDraft] infers peripherals from **vendor packages**, never from `pm list features`.
 */
object DeviceProbe {

    /** Attached devices, from `adb devices -l`. */
    suspend fun listDevices(sdk: AndroidSdk): List<ProbedDevice> {
        val result = ProcessRunner.run(sdk.adbCommand(null, "devices", "-l"), sdk.env(), timeoutMs = 15_000)
        if (!result.ok) return emptyList()
        return result.lines
            .drop(1)
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("*")) return@mapNotNull null
                val parts = trimmed.split(Regex("\\s+"))
                val serial = parts.firstOrNull() ?: return@mapNotNull null
                val state = parts.getOrNull(1) ?: return@mapNotNull null
                ProbedDevice(
                    serial = serial,
                    state = state,
                    model = parts.firstOrNull { it.startsWith("model:") }?.removePrefix("model:").orEmpty(),
                    product = parts.firstOrNull { it.startsWith("product:") }?.removePrefix("product:").orEmpty(),
                    isEmulator = serial.startsWith("emulator-"),
                )
            }
    }

    suspend fun probe(sdk: AndroidSdk, serial: String): ProbeResult {
        suspend fun shell(vararg args: String): String =
            ProcessRunner.run(sdk.adbCommand(serial, "shell", *args), sdk.env(), timeoutMs = 20_000)
                .lines.joinToString("\n").trim()

        val props = PROPS.associateWith { shell("getprop", it).lineSequence().firstOrNull()?.trim().orEmpty() }
        val size = parseWmSize(shell("wm", "size"))
        val density = parseWmDensity(shell("wm", "density"))
        val memTotalKb = parseMemTotal(shell("cat", "/proc/meminfo"))
        val cores = shell("cat /proc/cpuinfo | grep -c ^processor").trim().toIntOrNull()
        val features = shell("pm", "list", "features")
            .lineSequence()
            .map { it.trim().removePrefix("feature:") }
            .filter { it.isNotEmpty() }
            .toSet()
        val packages = shell("pm", "list", "packages")
            .lineSequence()
            .map { it.trim().removePrefix("package:") }
            .filter { it.isNotEmpty() }
            .toList()

        val vendorPackages = packages.filter { pkg ->
            DalFlavor.entries.any { it.detectPackagePrefix.isNotEmpty() && pkg.startsWith(it.detectPackagePrefix) }
        }.sorted()

        val detectedDal = DalFlavor.entries
            .filter { it.detectPackagePrefix.isNotEmpty() }
            .firstOrNull { flavor -> packages.any { it.startsWith(flavor.detectPackagePrefix) } }

        return ProbeResult(
            serial = serial,
            properties = props,
            screenWidth = size?.first,
            screenHeight = size?.second,
            density = density,
            apiLevel = props["ro.build.version.sdk"]?.toIntOrNull(),
            androidRelease = props["ro.build.version.release"].orEmpty(),
            abiList = props["ro.product.cpu.abilist"].orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() },
            memTotalKb = memTotalKb,
            cpuCores = cores,
            features = features,
            vendorPackages = vendorPackages,
            detectedDal = detectedDal,
            buildFingerprint = props["ro.build.fingerprint"].orEmpty(),
        )
    }

    /** A hardware profile carrying the measured values, graded as device-verified. */
    fun toHardwareProfile(result: ProbeResult, id: String, name: String): HardwareProfile {
        val properties = buildMap {
            result.screenWidth?.let { put(Props.LCD_WIDTH, it.toString()) }
            result.screenHeight?.let { put(Props.LCD_HEIGHT, it.toString()) }
            result.density?.let { put(Props.LCD_DENSITY, it.toString()) }
            result.nominalRamMb?.let { put(Props.RAM, it.toString()) }
            result.cpuCores?.let { put(Props.CPU_CORES, it.toString()) }
            put(Props.CPU_ARCH, if (result.primaryAbi.startsWith("arm64")) "arm64" else "x86_64")
            put(Props.ORIENTATION, "portrait")
            put(Props.CAMERA_BACK, if (result.hasFeature("android.hardware.camera")) "emulated" else "none")
            put(Props.CAMERA_FRONT, if (result.hasFeature("android.hardware.camera.front")) "emulated" else "none")
            put("hw.gps", yesNo(result.hasFeature("android.hardware.location.gps")))
            put("hw.accelerometer", yesNo(result.hasFeature("android.hardware.sensor.accelerometer")))
            put("hw.gyroscope", yesNo(result.hasFeature("android.hardware.sensor.gyroscope")))
            put("hw.audioInput", "yes")
            put("hw.battery", "yes")
            put("hw.keyboard", "yes")
            put("hw.mainKeys", "no")
            put("hw.dPad", "no")
            put("hw.trackBall", "no")
            put("hw.gpu.enabled", "yes")
            put("hw.gpu.mode", "auto")
        }

        return HardwareProfile(
            id = id,
            name = name,
            manufacturer = result.properties["ro.product.manufacturer"].orEmpty().ifEmpty { "User" },
            origin = ProfileOrigin.IMPORTED,
            properties = properties,
            provenance = SpecProvenance(
                confidence = SpecConfidence.VERIFIED_FROM_DEVICE,
                source = "adb probe ${result.serial} (${result.properties["ro.product.model"]})",
                verifiedAt = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * A terminal draft from the probe.
     *
     * Peripherals come from **vendor packages**, not `pm list features`. A PAX A910S reports no
     * `android.hardware.nfc` at all yet plainly has a contactless reader — the vendor DAL owns that
     * hardware, so Android never sees it. Trusting the feature list would produce a profile that
     * silently loses ICC, PICC, PED and the printer.
     */
    fun toTerminalDraft(result: ProbeResult, hardwareProfileId: String, base: TerminalModel?): TerminalModel {
        val model = result.properties["ro.product.model"].orEmpty().ifEmpty { "Unknown" }
        val vendor = base?.vendor
            ?: DeviceVendor.entries.firstOrNull {
                it.displayName.equals(result.properties["ro.product.manufacturer"], ignoreCase = true)
            }
            ?: DeviceVendor.GENERIC

        val inferred = buildSet {
            addAll(base?.baseFeatures ?: emptySet())
            if (result.vendorPackages.any { it.contains("scan", true) }) add(DeviceFeature.SCANNER)
            if (result.vendorPackages.any { it.contains("print", true) }) add(DeviceFeature.PRINTER)
            if (result.hasFeature("android.hardware.camera")) add(DeviceFeature.CAMERA)
            if (result.hasFeature("android.hardware.wifi")) add(DeviceFeature.WIFI)
            if (result.hasFeature("android.hardware.telephony")) add(DeviceFeature.CELLULAR)
            if (result.hasFeature("android.hardware.location.gps")) add(DeviceFeature.GPS)
        }

        return TerminalModel(
            id = base?.id ?: "probed-${model.lowercase().replace(Regex("[^a-z0-9]+"), "-")}",
            vendor = vendor,
            model = model,
            displayName = base?.displayName ?: "${vendor.displayName} $model",
            origin = ProfileOrigin.IMPORTED,
            derivedFrom = base?.id,
            baseProperties = toHardwareProfile(result, hardwareProfileId, model).properties,
            baseFeatures = inferred,
            dal = result.detectedDal ?: base?.dal ?: DalFlavor.NONE,
            dalStatus = base?.dalStatus ?: DalStatus.NOT_IMPLEMENTED,
            printer = base?.printer ?: PrinterSpec(),
            ped = base?.ped ?: PedSpec(),
            // Match the real API level when an image for it exists, which is the whole point.
            recommendedImage = SystemImageRef(
                apiLevel = result.apiLevel ?: 30,
                tag = if (result.hasGoogleServices) "google_apis" else "default",
                abi = "",
            ),
            realAndroidVersion = result.androidRelease,
            bootProps = PROPS.filter { it.startsWith("ro.product.") && it != "ro.serialno" }
                .associateWith { result.properties[it].orEmpty() }
                .filterValues { it.isNotEmpty() },
            provenance = SpecProvenance(
                confidence = SpecConfidence.VERIFIED_FROM_DEVICE,
                source = "adb probe ${result.serial}",
                verifiedAt = System.currentTimeMillis(),
            ),
            notes = "Probed from ${result.buildFingerprint}",
        )
    }

    // ---- parsers, kept separate so they are unit-testable without a device ----

    /** `Physical size: 720x1280`, preferring an `Override size:` line when present. */
    internal fun parseWmSize(output: String): Pair<Int, Int>? {
        val line = output.lineSequence().firstOrNull { it.contains("Override size:") }
            ?: output.lineSequence().firstOrNull { it.contains("Physical size:") }
            ?: return null
        val match = Regex("(\\d+)x(\\d+)").find(line) ?: return null
        return match.groupValues[1].toInt() to match.groupValues[2].toInt()
    }

    /** `Physical density: 320`, preferring `Override density:`. */
    internal fun parseWmDensity(output: String): Int? {
        val line = output.lineSequence().firstOrNull { it.contains("Override density:") }
            ?: output.lineSequence().firstOrNull { it.contains("Physical density:") }
            ?: return null
        return Regex("(\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
    }

    /** `MemTotal:        1901628 kB` */
    internal fun parseMemTotal(meminfo: String): Long? =
        meminfo.lineSequence()
            .firstOrNull { it.startsWith("MemTotal:", ignoreCase = true) }
            ?.let { Regex("(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() }

    /**
     * Reported RAM is always below nominal — the kernel and reserved regions are already deducted,
     * so a 2 GB device reports ~1857 MB. Round up to the nearest sane bucket, because the emulator
     * wants the nominal figure.
     */
    internal fun nominalRam(memTotalKb: Long?): Int? {
        val mb = (memTotalKb ?: return null) / 1024
        return listOf(512, 1024, 2048, 3072, 4096, 6144, 8192, 12288, 16384).firstOrNull { it >= mb }
    }

    private fun yesNo(value: Boolean) = if (value) "yes" else "no"

    /** The properties worth capturing; `getprop` is cheap but a full dump is noise. */
    private val PROPS = listOf(
        "ro.product.model", "ro.product.manufacturer", "ro.product.brand",
        "ro.product.device", "ro.product.name", "ro.serialno",
        "ro.build.version.release", "ro.build.version.sdk",
        "ro.product.cpu.abi", "ro.product.cpu.abilist",
        "ro.build.fingerprint", "ro.build.tags", "ro.build.type",
    )
}

data class ProbedDevice(
    val serial: String,
    val state: String,
    val model: String,
    val product: String,
    val isEmulator: Boolean,
) {
    val isUsable: Boolean get() = state == "device"
    val label: String get() = if (model.isBlank()) serial else "$model ($serial)"
}

data class ProbeResult(
    val serial: String,
    val properties: Map<String, String>,
    val screenWidth: Int?,
    val screenHeight: Int?,
    val density: Int?,
    val apiLevel: Int?,
    val androidRelease: String,
    val abiList: List<String>,
    val memTotalKb: Long?,
    val cpuCores: Int?,
    val features: Set<String>,
    val vendorPackages: List<String>,
    val detectedDal: DalFlavor?,
    val buildFingerprint: String,
) {
    val primaryAbi: String get() = abiList.firstOrNull().orEmpty()

    val nominalRamMb: Int? get() = DeviceProbe.nominalRam(memTotalKb)

    fun hasFeature(name: String): Boolean = name in features

    val hasGoogleServices: Boolean
        get() = features.any { it.startsWith("com.google.android.feature") }

    /**
     * True when the device supports 32-bit ARM. API 30+ arm64 emulator images do **not**, so an
     * APK shipping only `armeabi-v7a` natives runs on the real terminal and fails to install on the
     * emulator with `INSTALL_FAILED_NO_MATCHING_ABIS`.
     */
    val supports32BitArm: Boolean get() = abiList.any { it.startsWith("armeabi") }

    val resolutionSummary: String?
        get() {
            val w = screenWidth ?: return null
            val h = screenHeight ?: return null
            return if (density != null) "$w x $h @ $density dpi" else "$w x $h"
        }
}
