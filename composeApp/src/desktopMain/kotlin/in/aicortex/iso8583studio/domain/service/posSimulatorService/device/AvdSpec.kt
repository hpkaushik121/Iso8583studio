package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.HardwarePropertiesSchema
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageRef
import kotlinx.serialization.Serializable
import java.security.MessageDigest

/**
 * A named, saveable AVD configuration: a [HardwareProfile] plus a system image plus per-AVD
 * deltas and boot options. This is what "Prepare" turns into a real AVD on disk.
 *
 * Embedded in `POSSimulatorConfig` rather than stored separately, because an AVD name, its ports
 * and its drift fingerprint are meaningless detached from the config that owns them — and
 * embedding avoids a whole class of dangling-reference bugs.
 */
@Serializable
data class AvdSpec(
    /** Must carry [AvdProperties.MANAGED_PREFIX]; that prefix is what protects the user's own AVDs. */
    val avdName: String = "",
    val displayName: String = "",
    val hardwareProfileId: String = "",
    val systemImage: SystemImageRef = SystemImageRef(),

    /** Per-AVD deltas on top of the hardware profile. Same key space as `config.ini`. */
    val overrides: Map<String, String> = emptyMap(),

    val sdCardSize: String = "512M",
    val dataPartitionSize: String = "6G",

    // Boot options. These become emulator argv, not config.ini keys — except the fastboot pair.
    val coldBoot: Boolean = true,
    val noSnapshotSave: Boolean = true,
    /** Required for the DAL install and `ro.product.*` spoofing. */
    val writableSystem: Boolean = true,
    val selinuxPermissive: Boolean = true,
    val headless: Boolean = false,
    /** 0 = auto-allocate a free even port, so several simulators can run at once. */
    val consolePort: Int = 0,
    val extraEmulatorArgs: List<String> = emptyList(),

    /** Empty = ~/.android/avd, where Android Studio can see it. Set for CI isolation. */
    val avdHomeOverride: String = "",
    val ephemeral: Boolean = false,

    // Bookkeeping for drift detection.
    val lastAppliedFingerprint: String = "",
    val lastPreparedAt: Long = 0L,
) {
    val isPrepared: Boolean get() = lastPreparedAt > 0L && lastAppliedFingerprint.isNotEmpty()
}

object AvdProperties {

    /**
     * Every AVD this tool manages carries this prefix, and delete/recreate refuse anything without
     * it. Not cosmetic: `~/.android/avd` on a developer machine is full of hand-built AVDs, and an
     * unguarded delete path would be a data-loss bug.
     */
    const val MANAGED_PREFIX = "ISO8583_"

    fun isManaged(avdName: String): Boolean = avdName.startsWith(MANAGED_PREFIX)

    /** `ISO8583_PAX_A910S` — sanitised to what `avdmanager` accepts. */
    fun suggestAvdName(terminal: TerminalDeviceProfile): String {
        val slug = "${terminal.vendor.name}_${terminal.model}"
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
        return "$MANAGED_PREFIX$slug"
    }

    /**
     * Full property resolution for validation and display: schema defaults, then the hardware
     * profile, then per-AVD overrides, then derived keys.
     *
     * This is *not* what gets written to `config.ini` — see [configIniOverlay]. Real config.ini
     * files carry ~50 keys, not all 153; defaults stay implicit on disk.
     */
    fun effective(
        spec: AvdSpec,
        hardware: HardwareProfile,
        schema: HardwarePropertiesSchema,
    ): Map<String, String> = buildMap {
        putAll(schema.defaults())
        putAll(hardware.properties)
        putAll(spec.overrides)
        putAll(derived(spec, hardware))
    }

    /**
     * The keys we actually write. Excludes schema defaults so we never bloat `config.ini` with 153
     * entries, and so anything we do not set keeps tracking the emulator's own default.
     */
    fun configIniOverlay(
        spec: AvdSpec,
        hardware: HardwareProfile,
    ): Map<String, String> = buildMap {
        putAll(hardware.properties)
        putAll(spec.overrides)
        putAll(derived(spec, hardware))
    }

    /**
     * Keys computed from a single source of truth, applied last so they always win.
     *
     * They override user-supplied values deliberately: `abi.type`, `image.sysdir.1` and the `tag.*`
     * family all derive from [AvdSpec.systemImage]. Letting a stale override survive would produce
     * an AVD whose ABI disagrees with its system image directory — which boots to a black screen
     * with no useful error.
     */
    private fun derived(spec: AvdSpec, hardware: HardwareProfile): Map<String, String> = buildMap {
        val image = spec.systemImage.withHostAbi()

        if (spec.avdName.isNotEmpty()) put(Props.AVD_ID, spec.avdName)
        val display = spec.displayName.ifEmpty { spec.avdName }
        if (display.isNotEmpty()) put(Props.DISPLAY_NAME, display)
        put(Props.ENCODING, "UTF-8")

        put(Props.ABI, image.abi)
        put(Props.SYSDIR, image.sysdir)
        put(Props.TAG_ID, image.tag)
        put(Props.TAG_IDS, image.tag)
        val tagDisplay = tagDisplayName(image.tag)
        put(Props.TAG_DISPLAY, tagDisplay)
        put(Props.TAG_DISPLAY_NAMES, tagDisplay)
        put(Props.PLAY_STORE, image.isPlayStore.toString())

        if (spec.dataPartitionSize.isNotBlank()) put(Props.DATA_PARTITION, spec.dataPartitionSize)
        if (spec.sdCardSize.isNotBlank()) {
            put(Props.SDCARD_SIZE, spec.sdCardSize)
            put(Props.SDCARD_PRESENT, "yes")
        } else {
            put(Props.SDCARD_PRESENT, "no")
        }

        // Boot behaviour: a payment-terminal debugging session wants a deterministic cold boot far
        // more often than a fast one, and snapshots silently revert a writable /system overlay.
        put(Props.COLD_BOOT, yesNo(spec.coldBoot))
        put(Props.FAST_BOOT, yesNo(!spec.coldBoot))

        putAll(skinKeys(hardware.skin))
    }

    private fun skinKeys(skin: SkinRef?): Map<String, String> = when (skin?.kind) {
        null, SkinKind.NONE -> mapOf(
            Props.SHOW_DEVICE_FRAME to "no",
            Props.SKIN_DYNAMIC to "yes",
        )
        SkinKind.RESOLUTION_ONLY -> mapOf(
            Props.SHOW_DEVICE_FRAME to "no",
            Props.SKIN_DYNAMIC to "yes",
        )
        SkinKind.BUILTIN, SkinKind.CUSTOM -> buildMap {
            if (skin.name.isNotBlank()) put(Props.SKIN_NAME, skin.name)
            if (skin.path.isNotBlank()) put(Props.SKIN_PATH, skin.path)
            put(Props.SKIN_DYNAMIC, yesNo(skin.dynamic))
            put(Props.SHOW_DEVICE_FRAME, yesNo(skin.showDeviceFrame))
        }
    }

    private fun tagDisplayName(tag: String): String = when (tag) {
        "google_apis" -> "Google APIs"
        "google_apis_playstore" -> "Google Play"
        "default" -> "Default Android System Image"
        else -> tag
    }

    private fun yesNo(value: Boolean) = if (value) "yes" else "no"

    /**
     * Identity of a prepared AVD. Stored in [AvdSpec.lastAppliedFingerprint]; a mismatch means the
     * spec changed since Prepare, which the UI turns into "Apply changes" or "Recreate required"
     * depending on whether the changed keys are HOT or COLD.
     *
     * Deliberately covers only what lands on disk, so cosmetic edits (a renamed display name in our
     * own model, notes) do not spuriously report drift.
     */
    fun fingerprint(spec: AvdSpec, hardware: HardwareProfile): String {
        val canonical = configIniOverlay(spec, hardware)
            .toSortedMap()
            .entries
            .joinToString("\n") { "${it.key}=${it.value}" }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
