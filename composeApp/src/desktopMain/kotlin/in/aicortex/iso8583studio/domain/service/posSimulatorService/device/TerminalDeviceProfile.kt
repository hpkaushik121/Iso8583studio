package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageRef
import kotlinx.serialization.Serializable

/**
 * The POS semantics of a terminal model — what a `devices.xml` device definition has no vocabulary
 * for: which payment SDK it exposes, which peripherals exist, how wide the printer is.
 *
 * Deliberately kept *parallel* to [HardwareProfile] rather than merged into it. That buys three
 * things: the hardware profile stays losslessly importable from and exportable to `devices.xml`;
 * one hardware profile can back several terminal personalities (running the PAX DAL on a `pixel_6`
 * profile is a genuinely useful way to A/B whether a bug is screen-geometry-related); and adding a
 * vendor touches only this file plus a `device-host/dal-<vendor>` module, never the AVD layer.
 */
@Serializable
data class TerminalDeviceProfile(
    val id: String,
    val vendor: DeviceVendor,
    val model: String,
    val displayName: String = "",
    val origin: ProfileOrigin = ProfileOrigin.BUILTIN,
    val derivedFrom: String? = null,

    val hardwareProfileId: String,
    /** A starting point, not a lock — the user can pick any installed image. */
    val recommendedImage: SystemImageRef = SystemImageRef(),

    val dal: DalFlavor = DalFlavor.NONE,
    val dalStatus: DalStatus = DalStatus.NOT_IMPLEMENTED,
    val features: Set<DeviceFeature> = emptySet(),
    val printer: PrinterSpec = PrinterSpec(),
    val ped: PedSpec = PedSpec(),

    /** `ro.product.*` values injected via `emulator -prop` before init. */
    val bootProps: Map<String, String> = emptyMap(),
    val dalTimeoutsMs: Map<String, Long> = emptyMap(),
    val emulatorArgs: List<String> = emptyList(),

    /** The Android version the *real* device ships, which is often not what we can emulate. */
    val realAndroidVersion: String = "",
    val provenance: SpecProvenance = SpecProvenance(),
    val notes: String = "",
) {
    val label: String get() = displayName.ifEmpty { "${vendor.displayName} $model" }

    fun has(feature: DeviceFeature): Boolean = feature in features

    fun fork(newId: String, newHardwareProfileId: String): TerminalDeviceProfile = copy(
        id = newId,
        hardwareProfileId = newHardwareProfileId,
        origin = ProfileOrigin.USER,
        derivedFrom = derivedFrom ?: id.takeIf { origin == ProfileOrigin.BUILTIN },
    )

    /**
     * Default `ro.product.*` spoofing for this model. Merged under any explicit [bootProps] so a
     * profile can override individual values.
     */
    fun effectiveBootProps(serialNumber: String? = null): Map<String, String> = buildMap {
        put("ro.product.manufacturer", vendor.displayName)
        put("ro.product.brand", vendor.brandProp)
        put("ro.product.model", model)
        put("ro.product.device", model.replace(' ', '_'))
        put("ro.product.name", model.replace(' ', '_'))
        serialNumber?.takeIf { it.isNotBlank() }?.let { put("ro.serialno", it) }
        putAll(bootProps)
    }
}

/**
 * The OEM. [defaultDal] is the SDK family its Android terminals expose; a profile can override it,
 * and [DalFlavor.NONE] is always valid — that is the "hardware only" mode where the emulated device
 * has the right shape but no payment SDK, which is useful on its own.
 */
enum class DeviceVendor(val displayName: String, val brandProp: String, val defaultDal: DalFlavor) {
    PAX("PAX Technology", "PAX", DalFlavor.PAX_NEPTUNE),
    INGENICO("Ingenico", "Ingenico", DalFlavor.INGENICO_AXIUM),
    KOZEN("Kozen", "Kozen", DalFlavor.KOZEN_SMARTPOS),
    NEWLAND("Newland", "Newland", DalFlavor.NEWLAND_PAYMENT),
    SUNMI("Sunmi", "SUNMI", DalFlavor.SUNMI_PAY_KERNEL),
    VERIFONE("Verifone", "Verifone", DalFlavor.VERIFONE_ANDROID),
    NEXGO("NexGo", "NEXGO", DalFlavor.NEXGO_SDK),
    CASTLES("Castles Technology", "Castles", DalFlavor.CASTLES_SDK),
    TELPO("Telpo", "TELPO", DalFlavor.TELPO_SDK),
    GENERIC("Generic", "generic", DalFlavor.NONE),
}

/**
 * Which vendor SDK the in-AVD device host impersonates.
 *
 * [entryPointHint] is what M0 recon decompiles to find the binding seam, and [detectPackagePrefix]
 * is what [DeviceProbe] greps for in `pm list packages` on a real device — a hit both confirms the
 * flavor and tells recon which package to pull apart.
 */
enum class DalFlavor(
    val displayName: String,
    val entryPointHint: String = "",
    val detectPackagePrefix: String = "",
) {
    NONE("None (hardware only)"),
    PAX_NEPTUNE(
        "PAX Neptune Lite",
        "com.pax.neptunelite.api.NeptuneLiteUser.getInstance().getDal(ctx)",
        "com.pax",
    ),
    INGENICO_AXIUM("Ingenico AXIUM", "", "com.ingenico"),
    KOZEN_SMARTPOS("Kozen SmartPOS", "", "com.kozen"),
    NEWLAND_PAYMENT("Newland Payment", "", "com.nld"),
    SUNMI_PAY_KERNEL("Sunmi PayKernel", "SunmiPayKernel / com.sunmi.pay.hardware.aidl*", "com.sunmi"),
    VERIFONE_ANDROID("Verifone Android", "", "com.verifone"),
    NEXGO_SDK("NexGo SDK", "", "com.nexgo"),
    CASTLES_SDK("Castles SDK", "", "com.ctl"),
    TELPO_SDK("Telpo SDK", "", "com.telpo"),
}

/**
 * Whether the in-AVD device host can actually serve this vendor's SDK yet.
 *
 * The distinction matters and is surfaced per model in the UI: choosing any model gives correct
 * emulated *hardware* immediately, but running that vendor's SDK APK needs a `dal-<vendor>` module
 * and its own recon pass.
 */
enum class DalStatus(val label: String) {
    IMPLEMENTED("DAL implemented"),
    IN_PROGRESS("DAL in progress"),
    NOT_IMPLEMENTED("Hardware only"),
}

enum class DeviceFeature(val label: String) {
    ICC("Contact card reader"),
    PICC("Contactless reader"),
    MSR("Magnetic stripe reader"),
    SAM("SAM slots"),
    PED("PIN entry device"),
    PRINTER("Thermal printer"),
    SCANNER("Barcode scanner"),
    NFC("NFC"),
    BEEPER("Beeper"),
    LED("Status LEDs"),
    CAMERA("Camera"),
    CELLULAR("Cellular"),
    WIFI("Wi-Fi"),
    GPS("GPS"),
}

@Serializable
data class PrinterSpec(
    val present: Boolean = true,
    /** 384 dots is the near-universal 58 mm thermal head. */
    val dotsPerLine: Int = 384,
    val paperWidthMm: Int = 58,
    val grayscale: Boolean = false,
)

@Serializable
data class PedSpec(
    val present: Boolean = true,
    /** Format names exactly as PinBlockService.encode accepts them. */
    val supportedPinBlockFormats: List<String> = listOf("ISO-0", "ISO-1", "ISO-3", "ISO-4"),
    val keySlots: Int = 100,
    val supportsDukpt: Boolean = true,
    val supportsOfflinePin: Boolean = true,
)
