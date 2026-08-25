package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

/**
 * Curation over [HardwarePropertiesSchema].
 *
 * The schema declares 153 properties; a handful are what anyone actually edits when shaping a
 * terminal, and the rest belong in an advanced table. This catalog promotes that handful to
 * first-class UI with better labels, sane widgets and grouping, and — importantly — records which
 * ones can be changed in place versus which force the AVD to be recreated.
 *
 * Facets are additive metadata only. Anything not listed here still renders in the Advanced tab
 * driven purely by the schema, so a new emulator release never hides properties from the user.
 */
object HardwarePropertyCatalog {

    val facets: List<PropertyFacet> = buildList {
        // ---- Memory & storage -------------------------------------------------------------
        add(PropertyFacet("hw.ramSize", PropGroup.MEMORY_STORAGE, 10, "RAM", FacetWidget.MegabytePresets(
            listOf(512, 1024, 2048, 4096, 8192)
        )))
        add(PropertyFacet("vm.heapSize", PropGroup.MEMORY_STORAGE, 20, "VM heap"))
        add(PropertyFacet("disk.dataPartition.size", PropGroup.MEMORY_STORAGE, 30, "Internal storage"))
        add(PropertyFacet("sdcard.size", PropGroup.MEMORY_STORAGE, 40, "SD card size"))
        add(PropertyFacet("hw.sdCard", PropGroup.MEMORY_STORAGE, 50, "SD card present"))

        // ---- Display ----------------------------------------------------------------------
        add(PropertyFacet("hw.lcd.width", PropGroup.DISPLAY, 10, "Screen width (px)"))
        add(PropertyFacet("hw.lcd.height", PropGroup.DISPLAY, 20, "Screen height (px)"))
        // The shipped enum stops at 320; real buckets go to 640, so it must be overridden.
        add(PropertyFacet(
            "hw.lcd.density", PropGroup.DISPLAY, 30, "Density (dpi)",
            enumOverride = listOf("120", "140", "160", "180", "213", "240", "280", "320", "360",
                "400", "420", "440", "480", "560", "640"),
        ))
        add(PropertyFacet("hw.lcd.depth", PropGroup.DISPLAY, 40, "Colour depth"))
        add(PropertyFacet("hw.initialOrientation", PropGroup.DISPLAY, 50, "Initial orientation"))
        add(PropertyFacet("hw.screen", PropGroup.DISPLAY, 60, "Touch screen type"))

        // ---- Skin -------------------------------------------------------------------------
        add(PropertyFacet("skin.name", PropGroup.SKIN, 10, "Skin"))
        add(PropertyFacet("skin.path", PropGroup.SKIN, 20, "Skin path"))
        add(PropertyFacet("skin.dynamic", PropGroup.SKIN, 30, "Resolution-only skin"))
        add(PropertyFacet("showDeviceFrame", PropGroup.SKIN, 40, "Show device frame"))

        // ---- Input ------------------------------------------------------------------------
        add(PropertyFacet("hw.keyboard", PropGroup.INPUT, 10, "Hardware keyboard"))
        add(PropertyFacet("hw.mainKeys", PropGroup.INPUT, 20, "Hardware back/home keys"))
        add(PropertyFacet("hw.dPad", PropGroup.INPUT, 30, "D-pad"))
        add(PropertyFacet("hw.trackBall", PropGroup.INPUT, 40, "Trackball"))

        // ---- Cameras ----------------------------------------------------------------------
        add(PropertyFacet("hw.camera.back", PropGroup.CAMERA, 10, "Rear camera"))
        add(PropertyFacet("hw.camera.front", PropGroup.CAMERA, 20, "Front camera"))

        // ---- Sensors ----------------------------------------------------------------------
        add(PropertyFacet("hw.accelerometer", PropGroup.SENSORS, 10, "Accelerometer"))
        add(PropertyFacet("hw.gyroscope", PropGroup.SENSORS, 20, "Gyroscope"))
        add(PropertyFacet("hw.gps", PropGroup.SENSORS, 30, "GPS"))
        add(PropertyFacet("hw.battery", PropGroup.SENSORS, 40, "Battery"))
        add(PropertyFacet("hw.audioInput", PropGroup.SENSORS, 50, "Microphone"))
        add(PropertyFacet("hw.sensors.proximity", PropGroup.SENSORS, 60, "Proximity"))
        add(PropertyFacet("hw.sensors.light", PropGroup.SENSORS, 70, "Light"))

        // ---- Graphics / CPU ---------------------------------------------------------------
        add(PropertyFacet("hw.gpu.enabled", PropGroup.GRAPHICS, 10, "Hardware GPU"))
        add(PropertyFacet("hw.gpu.mode", PropGroup.GRAPHICS, 20, "GPU mode"))
        add(PropertyFacet("hw.cpu.ncore", PropGroup.GRAPHICS, 30, "CPU cores"))

        // ---- Boot / image -----------------------------------------------------------------
        // These three define which image the AVD boots, so changing them invalidates the disks.
        add(PropertyFacet("abi.type", PropGroup.BOOT, 10, "ABI", changeClass = ChangeClass.COLD))
        add(PropertyFacet("image.sysdir.1", PropGroup.BOOT, 20, "System image", changeClass = ChangeClass.COLD))
        add(PropertyFacet("tag.id", PropGroup.BOOT, 30, "Image tag", changeClass = ChangeClass.COLD))
        add(PropertyFacet("PlayStore.enabled", PropGroup.BOOT, 40, "Play Store", changeClass = ChangeClass.COLD))
        add(PropertyFacet("fastboot.forceColdBoot", PropGroup.BOOT, 50, "Always cold boot"))
        add(PropertyFacet("fastboot.forceFastBoot", PropGroup.BOOT, 60, "Use quick boot"))

        // ---- Network ----------------------------------------------------------------------
        add(PropertyFacet("runtime.network.speed", PropGroup.NETWORK, 10, "Network speed"))
        add(PropertyFacet("runtime.network.latency", PropGroup.NETWORK, 20, "Network latency"))
    }

    private val byKey: Map<String, PropertyFacet> = facets.associateBy { it.key }

    operator fun get(key: String): PropertyFacet? = byKey[key]

    fun isCurated(key: String): Boolean = byKey.containsKey(key)

    /** Facets for one group, in display order. */
    fun group(group: PropGroup): List<PropertyFacet> =
        facets.filter { it.group == group }.sortedBy { it.order }

    /** Schema properties with no facet — these populate the Advanced table. */
    fun advancedKeys(schema: HardwarePropertiesSchema): List<String> =
        schema.names.filterNot(::isCurated).sorted()

    /**
     * Classifies a set of pending changes. Any COLD key means the AVD must be recreated, which
     * wipes user data — so the UI names the offending keys rather than silently destroying state.
     *
     * Shrinking the data partition is COLD even though the key is otherwise HOT: the emulator
     * cannot truncate an existing userdata image.
     */
    fun classify(changedKeys: Set<String>, before: Map<String, String>, after: Map<String, String>): ChangeClass {
        for (key in changedKeys) {
            if (byKey[key]?.changeClass == ChangeClass.COLD) return ChangeClass.COLD
            if (key == "disk.dataPartition.size" || key == "sdcard.size") {
                val old = before[key]?.let { DiskSize.parse(it) }
                val new = after[key]?.let { DiskSize.parse(it) }
                if (old != null && new != null && new < old) return ChangeClass.COLD
            }
        }
        return ChangeClass.HOT
    }
}

data class PropertyFacet(
    val key: String,
    val group: PropGroup,
    val order: Int,
    /** Friendlier than the schema's `abstract`, which is terse and inconsistent. */
    val label: String,
    val widget: FacetWidget = FacetWidget.Auto,
    /** Used where the schema's own enum is stale, e.g. `hw.lcd.density`. */
    val enumOverride: List<String>? = null,
    val changeClass: ChangeClass = ChangeClass.HOT,
)

enum class PropGroup(val label: String) {
    MEMORY_STORAGE("Memory & Storage"),
    DISPLAY("Display"),
    SKIN("Skin"),
    INPUT("Input"),
    CAMERA("Cameras"),
    SENSORS("Sensors"),
    GRAPHICS("Graphics & CPU"),
    BOOT("Boot & Image"),
    NETWORK("Network"),
}

/** Whether a change can be patched into an existing AVD or forces a destructive recreate. */
enum class ChangeClass { HOT, COLD }

sealed interface FacetWidget {
    /** Derive the control from the schema type. */
    data object Auto : FacetWidget
    /** An integer in megabytes with quick-pick presets. */
    data class MegabytePresets(val presets: List<Int>) : FacetWidget
}
