package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageRef

/**
 * The built-in terminal catalog: vendor → model → variant.
 *
 * ## Read this before adding an entry
 *
 * Vendor datasheets publish *"5\" HD"* and *"6\" HD"* and almost never the exact pixel dimensions,
 * and never the density bucket — "HD" is 720x1280 **or** 720x1440 depending on the model. Guessing
 * silently corrupts the emulation this tool exists to provide, so **every entry is graded** and the
 * UI shows the grade:
 *
 *  - [SpecConfidence.FROM_DATASHEET]  — the vendor publishes it; [SpecProvenance.source] cites it.
 *  - [SpecConfidence.PROVISIONAL]     — inferred (e.g. "5\" HD" -> 720x1280 at 320 dpi).
 *  - [SpecConfidence.VERIFIED_FROM_DEVICE] — **only** ever set by an adb `DeviceProbe` against real
 *    hardware. Never hard-code it here.
 *
 * Built-ins are compiled in and never written to disk. Editing one forks it
 * ([TerminalModel.fork] / [TerminalModel.duplicateVariant]) so a later catalog fix still reaches
 * anyone who has not forked.
 *
 * A common Android version note: most of these terminals ship **Android 10 (API 29)**, for which
 * Google publishes no arm64 system image. [recommendedImage] therefore targets API 30 on every
 * entry; `AvdValidator` explains the substitution when it matters.
 */
object DeviceCatalog {

    // NOTE: these must be declared before `models`. Kotlin initialises an object's properties in
    // declaration order, so a `val` referenced by the model builders but declared below them would
    // still be null when they run — which fails as an ExceptionInInitializerError, not a warning.

    /** API 30 google_apis; the ABI resolves to the host at prepare time. */
    private val defaultImage = SystemImageRef(apiLevel = 30, tag = "google_apis", abi = "")

    private val fullTerminal = setOf(
        DeviceFeature.ICC, DeviceFeature.PICC, DeviceFeature.MSR, DeviceFeature.SAM,
        DeviceFeature.PED, DeviceFeature.PRINTER, DeviceFeature.SCANNER, DeviceFeature.NFC,
        DeviceFeature.BEEPER, DeviceFeature.LED, DeviceFeature.CAMERA, DeviceFeature.WIFI,
    )

    val models: List<TerminalModel> = buildList {
        addAll(pax())
        addAll(ingenico())
        addAll(kozen())
        addAll(newland())
        addAll(sunmi())
        addAll(verifone())
        addAll(nexgo())
        addAll(castles())
        addAll(telpo())
        addAll(generic())
    }

    private val byId: Map<String, TerminalModel> = models.associateBy { it.id }

    operator fun get(modelId: String): TerminalModel? = byId[modelId]

    fun byVendor(vendor: DeviceVendor): List<TerminalModel> =
        models.filter { it.vendor == vendor }.sortedBy { it.model }

    val vendors: List<DeviceVendor>
        get() = models.map { it.vendor }.distinct().sortedBy { it.displayName }

    /** Resolves a `"<modelId>:<variantId>"` identity, falling back to the default variant. */
    fun resolve(terminalId: String): ResolvedTerminal? =
        byId[TerminalId.modelOf(terminalId)]?.resolve(TerminalId.variantOf(terminalId))

    fun search(query: String): List<TerminalModel> {
        if (query.isBlank()) return models
        val q = query.trim().lowercase()
        return models.filter {
            it.model.lowercase().contains(q) ||
                it.vendor.displayName.lowercase().contains(q) ||
                it.label.lowercase().contains(q) ||
                it.variants.any { v -> v.label.lowercase().contains(q) || v.sku.lowercase().contains(q) }
        }
    }

    // -------------------------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------------------------

    /** The property block every terminal needs; callers add or override from there. */
    private fun props(
        width: Int,
        height: Int,
        density: Int,
        ramMb: Int,
        cores: Int = 4,
        storage: String = "6G",
        backCamera: String = "emulated",
        frontCamera: String = "none",
    ): Map<String, String> = mapOf(
        Props.LCD_WIDTH to width.toString(),
        Props.LCD_HEIGHT to height.toString(),
        Props.LCD_DENSITY to density.toString(),
        Props.RAM to ramMb.toString(),
        Props.CPU_CORES to cores.toString(),
        Props.CPU_ARCH to "arm64",
        Props.DATA_PARTITION to storage,
        Props.ORIENTATION to "portrait",
        Props.CAMERA_BACK to backCamera,
        Props.CAMERA_FRONT to frontCamera,
        "hw.keyboard" to "yes",
        "hw.mainKeys" to "no",
        "hw.dPad" to "no",
        "hw.trackBall" to "no",
        "hw.gpu.enabled" to "yes",
        "hw.gpu.mode" to "auto",
        "hw.battery" to "yes",
        "hw.accelerometer" to "yes",
        "hw.audioInput" to "yes",
        "hw.gps" to "no",
    )

    private fun datasheet(source: String) = SpecProvenance(SpecConfidence.FROM_DATASHEET, source)
    private fun provisional(note: String) = SpecProvenance(SpecConfidence.PROVISIONAL, note)

    // -------------------------------------------------------------------------------------------
    // PAX
    // -------------------------------------------------------------------------------------------

    private fun pax(): List<TerminalModel> = listOf(
        TerminalModel(
            id = "pax-a910s",
            vendor = DeviceVendor.PAX,
            model = "A910S",
            displayName = "PAX A910S",
            // ---------------------------------------------------------------------------------
            // MEASURED from a physical A910S over adb, not inferred. The datasheet was wrong about
            // four things the emulator cares about; see DeviceProbe's KDoc. Notably the unit runs
            // Android 12 (API 31), not the documented 10 — which is fortunate, because API 29 has
            // no arm64 system image and API 31 does, so the replica can match exactly.
            //   fingerprint UNISOC/uis8581e_5h10_Natv/uis8581e_5h10:12/SP1A.210812.016/476:user
            // ---------------------------------------------------------------------------------
            baseProperties = props(
                width = 720, height = 1280, density = 320,
                ramMb = 2048,        // MemTotal 1901628 kB -> 1857 MB usable, 2 GB nominal
                cores = 8,           // 8 cores, not the 4 inferred from "quad-core A53"
                backCamera = "emulated", frontCamera = "emulated",
            ),
            // Peripherals are NOT taken from `pm list features`: the device reports no
            // android.hardware.nfc at all, because contactless sits behind the PAX DAL rather than
            // Android's NFC stack. Vendor packages (com.pax.paxscans, com.pax.paxbtprinter) and the
            // published hardware spec are the real evidence.
            baseFeatures = fullTerminal + DeviceFeature.CELLULAR + DeviceFeature.GPS,
            dal = DalFlavor.PAX_NEPTUNE,
            dalStatus = DalStatus.IN_PROGRESS,
            printer = PrinterSpec(dotsPerLine = 384, paperWidthMm = 58),
            // API 31 AOSP: the device has no Google services (0 GMS packages), so `default` is the
            // closer match as well as the rootable one.
            recommendedImage = SystemImageRef(apiLevel = 31, tag = "default", abi = ""),
            realAndroidVersion = "12",
            // Board names, not the model — ro.product.device/name are the UNISOC SoC identifiers,
            // and ro.product.brand is UNISOC while only ro.product.manufacturer is PAX.
            bootProps = mapOf(
                "ro.product.model" to "A910S",
                "ro.product.manufacturer" to "PAX",
                "ro.product.brand" to "UNISOC",
                "ro.product.device" to "uis8581e_5h10",
                "ro.product.name" to "uis8581e_5h10_Natv",
            ),
            provenance = SpecProvenance(
                confidence = SpecConfidence.VERIFIED_FROM_DEVICE,
                source = "adb probe 2841565824 (A910S, UNISOC uis8581e_5h10, Android 12)",
            ),
            notes = "Contactless is behind the PAX DAL — the device exposes no Android NFC feature. " +
                "ABI list is arm64-v8a,armeabi-v7a,armeabi; API 31+ arm64 emulator images drop " +
                "32-bit ARM, so an armeabi-v7a-only APK will run on the terminal but not the emulator.",
            variants = listOf(
                TerminalVariant(
                    id = "5in",
                    label = "5\" HD · Android 12",
                    isDefault = true,
                    provenance = SpecProvenance(
                        confidence = SpecConfidence.VERIFIED_FROM_DEVICE,
                        source = "adb probe 2841565824 — wm size 720x1280, wm density 320",
                    ),
                ),
                TerminalVariant(
                    id = "5p5in",
                    label = "5.5\" HD+",
                    propertyDeltas = mapOf(Props.LCD_HEIGHT to "1440", Props.LCD_DENSITY to "320"),
                    provenance = provisional("Datasheet lists an optional 5.5\"; 720x1440 inferred, not measured."),
                ),
                TerminalVariant(
                    id = "wifi-only",
                    label = "Wi-Fi only",
                    featuresRemoved = setOf(DeviceFeature.CELLULAR),
                    provenance = provisional("Connectivity SKU; the probed unit has telephony."),
                ),
                TerminalVariant(
                    id = "android10",
                    label = "Android 10 units",
                    // Kept because the datasheet documents Android 10 units. Unlike the probed unit
                    // this cannot be matched exactly: no arm64 image exists for API 29.
                    imageOverride = SystemImageRef(apiLevel = 30, tag = "google_apis", abi = ""),
                    realAndroidVersion = "10",
                    provenance = provisional("Datasheet documents Android 10; API 29 has no arm64 image, so API 30 is substituted."),
                ),
            ),
        ),
        paxSimple("pax-a920", "A920", 720, 1280, 320, 2048, "A920"),
        paxSimple("pax-a920pro", "A920Pro", 720, 1280, 320, 2048, "A920Pro"),
        paxSimple("pax-a80", "A80", 480, 640, 240, 1024, "A80", features = fullTerminal - DeviceFeature.SCANNER),
        paxSimple("pax-im30", "IM30", 720, 1280, 320, 2048, "IM30", features = fullTerminal - DeviceFeature.PRINTER),
    )

    private fun paxSimple(
        id: String,
        model: String,
        w: Int,
        h: Int,
        density: Int,
        ram: Int,
        displaySuffix: String,
        features: Set<DeviceFeature> = fullTerminal,
    ) = TerminalModel(
        id = id,
        vendor = DeviceVendor.PAX,
        model = model,
        displayName = "PAX $displaySuffix",
        baseProperties = props(w, h, density, ram),
        baseFeatures = features,
        dal = DalFlavor.PAX_NEPTUNE,
        dalStatus = DalStatus.NOT_IMPLEMENTED,
        recommendedImage = defaultImage,
        realAndroidVersion = "10",
        provenance = provisional("Geometry inferred from the PAX Android SmartPOS range."),
    )

    // -------------------------------------------------------------------------------------------
    // Ingenico
    // -------------------------------------------------------------------------------------------

    private fun ingenico(): List<TerminalModel> = listOf(
        TerminalModel(
            id = "ingenico-dx8000",
            vendor = DeviceVendor.INGENICO,
            model = "AXIUM DX8000",
            displayName = "Ingenico AXIUM DX8000",
            // The AXIUM DX8000 datasheet documents Android 10, a 6" HD capacitive touchscreen and
            // a quad-core Cortex-A53. Exact pixels and density are not published.
            baseProperties = props(width = 720, height = 1440, density = 320, ramMb = 2048),
            baseFeatures = fullTerminal + DeviceFeature.CELLULAR,
            dal = DalFlavor.INGENICO_AXIUM,
            dalStatus = DalStatus.NOT_IMPLEMENTED,
            recommendedImage = defaultImage,
            realAndroidVersion = "10",
            provenance = datasheet(
                "https://ingenico.com/sites/default/files/resource-document/2023-03/" +
                    "USA-CAN_DATASHEET_AXIUM%20DX8000_ING_230210.pdf"
            ),
            variants = listOf(
                TerminalVariant(
                    id = "dx8000",
                    label = "DX8000",
                    isDefault = true,
                    provenance = provisional("6\" HD per datasheet; 720x1440 @ 320 dpi inferred."),
                ),
                TerminalVariant(
                    id = "dx8005",
                    label = "DX8005",
                    sku = "DX8005-USBLU01A",
                    provenance = provisional("Series SKU; hardware assumed as the DX8000."),
                ),
                TerminalVariant(
                    id = "dx8010",
                    label = "DX8010",
                    provenance = provisional("Series SKU; hardware assumed as the DX8000."),
                ),
            ),
        ),
        TerminalModel(
            id = "ingenico-dx4000",
            vendor = DeviceVendor.INGENICO,
            model = "AXIUM DX4000",
            displayName = "Ingenico AXIUM DX4000",
            baseProperties = props(720, 1280, 320, 2048),
            baseFeatures = fullTerminal - DeviceFeature.SCANNER,
            dal = DalFlavor.INGENICO_AXIUM,
            recommendedImage = defaultImage,
            realAndroidVersion = "10",
            provenance = provisional("Geometry inferred from the AXIUM range."),
        ),
        TerminalModel(
            id = "ingenico-ex8000",
            vendor = DeviceVendor.INGENICO,
            model = "AXIUM EX8000",
            displayName = "Ingenico AXIUM EX8000",
            baseProperties = props(720, 1440, 320, 2048),
            baseFeatures = fullTerminal + DeviceFeature.CELLULAR,
            dal = DalFlavor.INGENICO_AXIUM,
            recommendedImage = defaultImage,
            realAndroidVersion = "10",
            provenance = provisional("Geometry inferred from the AXIUM range."),
        ),
    )

    // -------------------------------------------------------------------------------------------
    // Kozen
    // -------------------------------------------------------------------------------------------

    private fun kozen(): List<TerminalModel> = listOf(
        TerminalModel(
            id = "kozen-n2",
            vendor = DeviceVendor.KOZEN,
            model = "N2",
            displayName = "Kozen N2",
            // 480x480 @ 160 dpi / 2048 MB matches a hand-built AVD found on a developer machine,
            // which is a strong hint but not a probe result — so it stays PROVISIONAL until
            // DeviceProbe confirms it against real hardware.
            baseProperties = props(width = 480, height = 480, density = 160, ramMb = 2048),
            baseFeatures = setOf(
                DeviceFeature.ICC, DeviceFeature.PICC, DeviceFeature.MSR, DeviceFeature.PED,
                DeviceFeature.NFC, DeviceFeature.BEEPER, DeviceFeature.LED, DeviceFeature.WIFI,
            ),
            dal = DalFlavor.KOZEN_SMARTPOS,
            printer = PrinterSpec(present = false),
            recommendedImage = defaultImage,
            provenance = provisional("Matches a hand-built Kozen_N2 AVD; not device-verified."),
            variants = listOf(
                TerminalVariant(id = "base", label = "Square 480x480", isDefault = true),
                TerminalVariant(
                    id = "printer",
                    label = "With printer",
                    featuresAdded = setOf(DeviceFeature.PRINTER),
                    printerOverride = PrinterSpec(dotsPerLine = 384, paperWidthMm = 58),
                    provenance = provisional("Printer SKU."),
                ),
            ),
        ),
        kozenSimple("kozen-p10", "P10", 720, 1280, 320),
        kozenSimple("kozen-p12", "P12", 720, 1440, 320),
    )

    private fun kozenSimple(id: String, model: String, w: Int, h: Int, d: Int) = TerminalModel(
        id = id,
        vendor = DeviceVendor.KOZEN,
        model = model,
        displayName = "Kozen $model",
        baseProperties = props(w, h, d, 2048),
        baseFeatures = fullTerminal,
        dal = DalFlavor.KOZEN_SMARTPOS,
        recommendedImage = defaultImage,
        provenance = provisional("Geometry inferred from the Kozen SmartPOS range."),
    )

    // -------------------------------------------------------------------------------------------
    // Remaining vendors — one representative model each until someone probes real hardware.
    // -------------------------------------------------------------------------------------------

    private fun newland(): List<TerminalModel> = listOf(
        simple("newland-n910", DeviceVendor.NEWLAND, "N910", 720, 1280, 320, 2048, fullTerminal),
        simple("newland-nquire750", DeviceVendor.NEWLAND, "NQuire 750", 800, 1280, 240, 2048,
            fullTerminal - DeviceFeature.PRINTER - DeviceFeature.MSR),
    )

    private fun sunmi(): List<TerminalModel> = listOf(
        // Sunmi's PayKernel is plain, publicly documented AIDL with no proprietary jar, which makes
        // it the cheapest second DAL to implement after PAX.
        simple("sunmi-p2", DeviceVendor.SUNMI, "P2", 720, 1280, 320, 2048, fullTerminal),
        simple("sunmi-p2pro", DeviceVendor.SUNMI, "P2 Pro", 720, 1440, 320, 3072, fullTerminal),
        simple("sunmi-v2", DeviceVendor.SUNMI, "V2", 720, 1280, 320, 2048,
            fullTerminal - DeviceFeature.MSR),
    )

    private fun verifone(): List<TerminalModel> = listOf(
        simple("verifone-t650c", DeviceVendor.VERIFONE, "T650c", 720, 1280, 320, 2048, fullTerminal),
        simple("verifone-x990", DeviceVendor.VERIFONE, "X990", 720, 1440, 320, 2048, fullTerminal),
    )

    private fun nexgo(): List<TerminalModel> = listOf(
        simple("nexgo-n86", DeviceVendor.NEXGO, "N86", 720, 1280, 320, 2048, fullTerminal),
    )

    private fun castles(): List<TerminalModel> = listOf(
        simple("castles-s1f2", DeviceVendor.CASTLES, "S1F2", 720, 1280, 320, 2048, fullTerminal),
    )

    private fun telpo(): List<TerminalModel> = listOf(
        simple("telpo-m1", DeviceVendor.TELPO, "M1", 720, 1280, 320, 2048, fullTerminal),
    )

    private fun generic(): List<TerminalModel> = listOf(
        TerminalModel(
            id = "generic-terminal-720x1440",
            vendor = DeviceVendor.GENERIC,
            model = "Terminal 720x1440",
            displayName = "Generic Android terminal (720x1440)",
            baseProperties = props(720, 1440, 320, 2048),
            baseFeatures = fullTerminal,
            dal = DalFlavor.NONE,
            recommendedImage = defaultImage,
            provenance = provisional("Generic shape; not modelled on a specific device."),
            notes = "Hardware-only: correct device shape with no vendor SDK. Runs any ordinary APK.",
        ),
        TerminalModel(
            id = "generic-square-480",
            vendor = DeviceVendor.GENERIC,
            model = "Square 480x480",
            displayName = "Generic square terminal (480x480)",
            baseProperties = props(480, 480, 160, 2048),
            baseFeatures = setOf(
                DeviceFeature.ICC, DeviceFeature.PICC, DeviceFeature.PED, DeviceFeature.NFC,
                DeviceFeature.BEEPER, DeviceFeature.LED,
            ),
            dal = DalFlavor.NONE,
            printer = PrinterSpec(present = false),
            recommendedImage = defaultImage,
            provenance = provisional("Generic shape; not modelled on a specific device."),
        ),
    )

    private fun simple(
        id: String,
        vendor: DeviceVendor,
        model: String,
        w: Int,
        h: Int,
        d: Int,
        ram: Int,
        features: Set<DeviceFeature>,
    ) = TerminalModel(
        id = id,
        vendor = vendor,
        model = model,
        displayName = "${vendor.displayName} $model",
        baseProperties = props(w, h, d, ram),
        baseFeatures = features,
        dal = vendor.defaultDal,
        dalStatus = DalStatus.NOT_IMPLEMENTED,
        recommendedImage = defaultImage,
        provenance = provisional("Geometry inferred; verify with a device probe before relying on it."),
    )
}
