package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageRef
import kotlinx.serialization.Serializable

/**
 * A terminal model and its variants — the third selection axis, below vendor and above the AVD.
 *
 * Variants are not cosmetic. The PAX A910S ships with a 5" HD display *or* an optional 5.5" one,
 * and Ingenico's AXIUM DX8000 series has DX8000/DX8005/DX8010 SKUs; those change screen geometry
 * and peripherals, so they cannot be a single profile. But neither should each variant restate the
 * whole spec, so a [TerminalVariant] carries **deltas only** and inherits everything else.
 *
 * [resolve] flattens a (model, variant) pair into the pre-existing [HardwareProfile] and
 * [TerminalDeviceProfile] types, so nothing downstream — the AVD layer, the validator, the config
 * renderer — needs to know variants exist.
 */
@Serializable
data class TerminalModel(
    val id: String,
    val vendor: DeviceVendor,
    val model: String,
    val displayName: String = "",
    val origin: ProfileOrigin = ProfileOrigin.BUILTIN,
    val derivedFrom: String? = null,
    val catalogVersion: Int = 1,

    // ---- base spec, inherited by every variant ----
    /** Any of the emulator's ~153 `config.ini` keys. Only non-default values belong here. */
    val baseProperties: Map<String, String> = emptyMap(),
    val baseFeatures: Set<DeviceFeature> = emptySet(),
    val dal: DalFlavor = DalFlavor.NONE,
    val dalStatus: DalStatus = DalStatus.NOT_IMPLEMENTED,
    val printer: PrinterSpec = PrinterSpec(),
    val ped: PedSpec = PedSpec(),
    val recommendedImage: SystemImageRef = SystemImageRef(),
    /** The Android version the *real* device ships, which is often not what we can emulate. */
    val realAndroidVersion: String = "",
    val bootProps: Map<String, String> = emptyMap(),
    val skin: SkinRef? = null,
    val emulatorArgs: List<String> = emptyList(),
    val dalTimeoutsMs: Map<String, Long> = emptyMap(),
    val provenance: SpecProvenance = SpecProvenance(),

    val variants: List<TerminalVariant> = emptyList(),
    val notes: String = "",
) {
    val label: String get() = displayName.ifEmpty { "${vendor.displayName} $model" }

    val defaultVariant: TerminalVariant?
        get() = variants.firstOrNull { it.isDefault } ?: variants.firstOrNull()

    fun variant(variantId: String?): TerminalVariant? =
        variantId?.takeIf { it.isNotBlank() }?.let { id -> variants.firstOrNull { it.id == id } }

    /**
     * Flattens to the types the rest of the system already speaks. A null or unknown [variantId]
     * falls back to the default variant, and a model with no variants resolves to its base spec.
     */
    fun resolve(variantId: String? = null): ResolvedTerminal {
        val v = variant(variantId) ?: defaultVariant
        val resolvedId = TerminalId.of(id, v?.id)

        val properties = baseProperties + (v?.propertyDeltas ?: emptyMap())
        val features = (baseFeatures + (v?.featuresAdded ?: emptySet())) - (v?.featuresRemoved ?: emptySet())
        val image = v?.imageOverride ?: recommendedImage
        val resolvedSkin = v?.skinOverride ?: skin

        // A variant is only as trustworthy as the weaker of the two inputs: a datasheet-graded
        // 5.5" delta on a device-verified base is still unverified for the thing it changed.
        val resolvedProvenance = weaker(provenance, v?.provenance)

        val hardware = HardwareProfile(
            id = "$resolvedId#hw",
            name = v?.let { "$label (${it.label})" } ?: label,
            manufacturer = vendor.displayName,
            origin = origin,
            derivedFrom = derivedFrom,
            catalogVersion = catalogVersion,
            notes = notes,
            properties = properties,
            skin = resolvedSkin,
            provenance = resolvedProvenance,
        )

        val terminal = TerminalDeviceProfile(
            id = resolvedId,
            vendor = vendor,
            model = model,
            displayName = v?.let { "$label — ${it.label}" } ?: label,
            origin = origin,
            derivedFrom = derivedFrom,
            hardwareProfileId = hardware.id,
            recommendedImage = image,
            dal = dal,
            dalStatus = dalStatus,
            features = features,
            printer = v?.printerOverride ?: printer,
            ped = v?.pedOverride ?: ped,
            bootProps = bootProps + (v?.bootPropDeltas ?: emptyMap()),
            dalTimeoutsMs = dalTimeoutsMs + (v?.dalTimeoutDeltas ?: emptyMap()),
            emulatorArgs = emulatorArgs + (v?.emulatorArgsExtra ?: emptyList()),
            realAndroidVersion = v?.realAndroidVersion?.ifEmpty { null } ?: realAndroidVersion,
            provenance = resolvedProvenance,
            notes = v?.notes?.ifEmpty { null } ?: notes,
        )

        return ResolvedTerminal(modelId = id, variantId = v?.id.orEmpty(), hardware = hardware, terminal = terminal)
    }

    /** Editing a built-in forks the whole model, variants included. */
    fun fork(newId: String, newDisplayName: String = displayName): TerminalModel = copy(
        id = newId,
        displayName = newDisplayName,
        origin = ProfileOrigin.USER,
        derivedFrom = derivedFrom ?: id.takeIf { origin == ProfileOrigin.BUILTIN },
    )

    /** Adds a copy of an existing variant so the user can edit its deltas. */
    fun duplicateVariant(sourceVariantId: String, newId: String, newLabel: String): TerminalModel {
        val source = variant(sourceVariantId) ?: return this
        val copy = source.copy(id = newId, label = newLabel, isDefault = false, sku = "")
        return copy(variants = variants + copy)
    }

    private fun weaker(a: SpecProvenance, b: SpecProvenance?): SpecProvenance {
        if (b == null) return a
        // Higher ordinal == less trustworthy (VERIFIED_FROM_DEVICE, FROM_DATASHEET, PROVISIONAL).
        return if (b.confidence.ordinal >= a.confidence.ordinal) b else a
    }
}

/**
 * A delta against its [TerminalModel]. Every field is optional; an empty variant resolves to the
 * base spec unchanged.
 *
 * Deliberately not restricted to a fixed set of axes — [propertyDeltas] can override any emulator
 * property, so a variant can differ by screen, RAM, storage, sensors, cameras or anything else the
 * schema exposes, and the structured overrides cover the things that are not `config.ini` keys.
 */
@Serializable
data class TerminalVariant(
    val id: String,
    /** Human label, e.g. `5.5" · 4G · 3GB/32GB`. */
    val label: String,
    /** Vendor SKU where one is known, e.g. `DX8005-USBLU01A`. */
    val sku: String = "",
    val isDefault: Boolean = false,

    val propertyDeltas: Map<String, String> = emptyMap(),
    val featuresAdded: Set<DeviceFeature> = emptySet(),
    val featuresRemoved: Set<DeviceFeature> = emptySet(),
    val imageOverride: SystemImageRef? = null,
    val printerOverride: PrinterSpec? = null,
    val pedOverride: PedSpec? = null,
    val skinOverride: SkinRef? = null,
    val bootPropDeltas: Map<String, String> = emptyMap(),
    val emulatorArgsExtra: List<String> = emptyList(),
    val dalTimeoutDeltas: Map<String, Long> = emptyMap(),
    val realAndroidVersion: String = "",
    val provenance: SpecProvenance = SpecProvenance(),
    val notes: String = "",
) {
    /** For the picker: `5.5" · 4G` or `5.5" · 4G (DX8005-USBLU01A)`. */
    val displayLabel: String get() = if (sku.isBlank()) label else "$label ($sku)"
}

/** A flattened (model, variant) pair, in the types the rest of the system already uses. */
data class ResolvedTerminal(
    val modelId: String,
    val variantId: String,
    val hardware: HardwareProfile,
    val terminal: TerminalDeviceProfile,
) {
    val id: String get() = TerminalId.of(modelId, variantId)
}

/**
 * `"<modelId>:<variantId>"`, stored in `POSSimulatorConfig.terminalProfileId`. Keeping it a single
 * string means adding variants needed no config-schema change — the field was already there.
 */
object TerminalId {
    private const val SEPARATOR = ':'

    fun of(modelId: String, variantId: String?): String =
        if (variantId.isNullOrBlank()) modelId else "$modelId$SEPARATOR$variantId"

    fun modelOf(terminalId: String): String = terminalId.substringBefore(SEPARATOR)

    /** Empty when the id names a model with no variant. */
    fun variantOf(terminalId: String): String =
        if (SEPARATOR in terminalId) terminalId.substringAfter(SEPARATOR) else ""
}
