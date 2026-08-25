package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device

import kotlinx.serialization.Serializable

/**
 * A reusable Android device definition — the equivalent of an entry in Android Studio's Device
 * Manager, but owned by us.
 *
 * Hardware is a `Map<String, String>` keyed exactly as `hardware-properties.ini` / `config.ini`
 * key them, rather than a typed struct. The schema declares 153 properties and grows with every
 * emulator release, so a struct would need a code change per release and would silently drop
 * anything it did not model. The map is the AVD's native representation, is diffable against an
 * on-disk `config.ini`, and is what lets the property editor be schema-driven. Typed access is a
 * thin façade — see the extension properties at the bottom of this file.
 *
 * Only non-default values are stored; schema defaults are layered in by
 * [in.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdProperties.effective].
 */
@Serializable
data class HardwareProfile(
    val id: String,
    val name: String,
    val manufacturer: String = "User",
    val origin: ProfileOrigin = ProfileOrigin.USER,
    /** The built-in this was forked from, so a catalog fix can still be offered later. */
    val derivedFrom: String? = null,
    val catalogVersion: Int = 1,
    val notes: String = "",
    val properties: Map<String, String> = emptyMap(),
    val skin: SkinRef? = null,
    val provenance: SpecProvenance = SpecProvenance(),
) {
    /** Editing a built-in forks it rather than mutating the shipped catalog. */
    fun fork(newId: String, newName: String = name): HardwareProfile = copy(
        id = newId,
        name = newName,
        origin = ProfileOrigin.USER,
        derivedFrom = derivedFrom ?: id.takeIf { origin == ProfileOrigin.BUILTIN },
    )

    fun withProperty(key: String, value: String): HardwareProfile =
        copy(properties = properties + (key to value))
}

enum class ProfileOrigin(val label: String) {
    /** Compiled into the app; never written to disk. */
    BUILTIN("Built-in"),

    /** Read out of ~/.android/devices.xml or a device pack. */
    IMPORTED("Imported"),

    /** Created or forked by the user. */
    USER("Custom"),
}

/**
 * A skin is a directory holding a `layout` file plus PNGs. [kind] distinguishes a real device frame
 * from a resolution-only skin, which is what `skin.dynamic` encodes in `config.ini`.
 */
@Serializable
data class SkinRef(
    val kind: SkinKind = SkinKind.NONE,
    val name: String = "",
    /** Absolute. For [SkinKind.CUSTOM] this points at our managed copy under ~/.iso8583studio. */
    val path: String = "",
    val showDeviceFrame: Boolean = true,
    val dynamic: Boolean = false,
)

enum class SkinKind { NONE, RESOLUTION_ONLY, BUILTIN, CUSTOM }

/**
 * How much to trust a profile's numbers.
 *
 * Vendor datasheets publish "5\" HD" and "6\" HD" and almost never the exact pixel dimensions or
 * the density bucket — "HD" is 720x1280 *or* 720x1440 depending on the model. Guessing silently
 * corrupts the emulation this tool exists to provide, so an unverified profile is labelled as such
 * in the UI rather than presented as fact.
 *
 * [SpecConfidence.VERIFIED_FROM_DEVICE] is only ever set by a real adb probe against hardware.
 */
@Serializable
data class SpecProvenance(
    val confidence: SpecConfidence = SpecConfidence.PROVISIONAL,
    /** A datasheet URL, or "adb probe <serial>" for a device capture. */
    val source: String = "",
    val verifiedAt: Long = 0L,
) {
    val isTrustworthy: Boolean get() = confidence == SpecConfidence.VERIFIED_FROM_DEVICE
}

enum class SpecConfidence(val label: String, val warnsInUi: Boolean) {
    VERIFIED_FROM_DEVICE("Verified from device", false),
    FROM_DATASHEET("From datasheet", true),
    PROVISIONAL("Unverified", true),
}

// ---------------------------------------------------------------------------------------------
// Typed access. Deliberately extensions over the map rather than fields on the class, so the map
// stays the single source of truth and new properties never need a model change.
// ---------------------------------------------------------------------------------------------

val HardwareProfile.ramMb: Int? get() = properties[Props.RAM]?.trim()?.toIntOrNull()
val HardwareProfile.screenWidth: Int? get() = properties[Props.LCD_WIDTH]?.trim()?.toIntOrNull()
val HardwareProfile.screenHeight: Int? get() = properties[Props.LCD_HEIGHT]?.trim()?.toIntOrNull()
val HardwareProfile.density: Int? get() = properties[Props.LCD_DENSITY]?.trim()?.toIntOrNull()

/** e.g. "720 x 1440 @ 320 dpi", or null when the profile is incomplete. */
val HardwareProfile.resolutionSummary: String?
    get() {
        val w = screenWidth ?: return null
        val h = screenHeight ?: return null
        val d = density ?: return "$w x $h"
        return "$w x $h @ $d dpi"
    }

/** The `config.ini` / `hardware-properties.ini` keys referenced from Kotlin, in one place. */
object Props {
    const val AVD_ID = "AvdId"
    const val DISPLAY_NAME = "avd.ini.displayname"
    const val ENCODING = "avd.ini.encoding"
    const val ABI = "abi.type"
    const val SYSDIR = "image.sysdir.1"
    const val TAG_ID = "tag.id"
    const val TAG_IDS = "tag.ids"
    const val TAG_DISPLAY = "tag.display"
    const val TAG_DISPLAY_NAMES = "tag.displaynames"
    const val PLAY_STORE = "PlayStore.enabled"

    const val RAM = "hw.ramSize"
    const val HEAP = "vm.heapSize"
    const val CPU_ARCH = "hw.cpu.arch"
    const val CPU_CORES = "hw.cpu.ncore"

    const val LCD_WIDTH = "hw.lcd.width"
    const val LCD_HEIGHT = "hw.lcd.height"
    const val LCD_DENSITY = "hw.lcd.density"
    const val ORIENTATION = "hw.initialOrientation"

    const val DATA_PARTITION = "disk.dataPartition.size"
    const val SDCARD_SIZE = "sdcard.size"
    const val SDCARD_PRESENT = "hw.sdCard"

    const val SKIN_NAME = "skin.name"
    const val SKIN_PATH = "skin.path"
    const val SKIN_DYNAMIC = "skin.dynamic"
    const val SHOW_DEVICE_FRAME = "showDeviceFrame"

    const val CAMERA_BACK = "hw.camera.back"
    const val CAMERA_FRONT = "hw.camera.front"

    const val COLD_BOOT = "fastboot.forceColdBoot"
    const val FAST_BOOT = "fastboot.forceFastBoot"
}
