package `in`.aicortex.iso8583studio.posSimulator

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AndroidSdkLocator
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SdkResolution
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SkinCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SystemImageRef
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SdkCatalogTest {

    // ---------- SystemImageRef ----------

    @Test
    fun `builds the sdk package id and sysdir the tooling expects`() {
        val ref = SystemImageRef(apiLevel = 30, tag = "google_apis", abi = "arm64-v8a")
        assertEquals("system-images;android-30;google_apis;arm64-v8a", ref.sdkPackage)
        assertEquals("system-images/android-30/google_apis/arm64-v8a/", ref.sysdir)
    }

    @Test
    fun `flags play store images because they cannot be rooted`() {
        assertTrue(SystemImageRef(tag = "google_apis_playstore").isPlayStore)
        assertFalse(SystemImageRef(tag = "google_apis").isPlayStore)
        assertTrue(SystemImageRef(tag = "google_apis").hasGoogleApis)
        assertFalse(SystemImageRef(tag = "default").hasGoogleApis)
    }

    @Test
    fun `resolves an empty abi to the host so profiles are portable`() {
        assertEquals(SystemImageCatalog.hostAbi(), SystemImageRef(abi = "").withHostAbi().abi)
        // An explicit ABI is never overridden.
        assertEquals("x86_64", SystemImageRef(abi = "x86_64").withHostAbi().abi)
    }

    @Test
    fun `host abi maps jvm arch names to android abi names`() {
        val abi = SystemImageCatalog.hostAbi()
        assertTrue(abi in setOf("arm64-v8a", "x86_64", "x86"), "unexpected host abi: $abi")
    }

    // ---------- SystemImageCatalog ----------

    @Test
    fun `scans a synthetic system images tree`() {
        val tmp = Files.createTempDirectory("sysimg")
        val dir = tmp.resolve("android-30/google_apis/arm64-v8a").also { it.createDirectories() }
        dir.resolve("source.properties").writeText(
            """
            Pkg.Desc=System Image arm64-v8a with Google APIs.
            Pkg.Revision=16
            AndroidVersion.ApiLevel=30
            SystemImage.Abi=arm64-v8a
            SystemImage.TagId=google_apis
            SystemImage.TagDisplay=Google APIs
            SystemImage.GpuSupport=true
            Addon.VendorId=google
            """.trimIndent()
        )
        dir.resolve("system.img").writeText("x".repeat(100))

        val images = SystemImageCatalog.scanInstalled(tmp)
        assertEquals(1, images.size)
        val image = images.single()
        assertEquals(30, image.ref.apiLevel)
        assertEquals("google_apis", image.ref.tag)
        assertEquals("arm64-v8a", image.ref.abi)
        assertEquals("Google APIs", image.tagDisplay)
        assertEquals(16, image.revision)
        assertEquals("google", image.vendor)
        assertTrue(image.gpuSupport)
        assertEquals(100L, image.sizeBytes)
    }

    @Test
    fun `ignores directories without source properties and never throws on a missing tree`() {
        val tmp = Files.createTempDirectory("sysimg-empty")
        tmp.resolve("android-30/google_apis/arm64-v8a").createDirectories()
        assertTrue(SystemImageCatalog.scanInstalled(tmp).isEmpty())
        assertTrue(SystemImageCatalog.scanInstalled(tmp.resolve("does-not-exist")).isEmpty())
    }

    @Test
    fun `finds the real installed images when an SDK is present`() {
        val sdk = (AndroidSdkLocator.locate() as? SdkResolution.Found)?.sdk ?: return
        val images = SystemImageCatalog.scanInstalled(sdk)
        if (images.isEmpty()) return // SDK present but no images installed.

        // Sorted newest API first, and every entry is self-consistent.
        assertEquals(images.map { it.ref.apiLevel }.sortedDescending(), images.map { it.ref.apiLevel })
        for (image in images) {
            assertTrue(image.ref.apiLevel > 0)
            assertTrue(image.ref.abi.isNotEmpty())
            assertTrue(image.ref.tag.isNotEmpty())
        }
    }

    // ---------- SkinCatalog ----------

    @Test
    fun `parses the display block out of a skin layout`() {
        val tmp = Files.createTempDirectory("skin")
        val skin = tmp.resolve("pax_a910s").also { it.createDirectories() }
        skin.resolve("layout").writeText(
            """
            parts {
              device {
                display {
                  width 720
                  height 1440
                  x 0
                  y 0
                }
              }
            }
            layouts {
              portrait {
                width 800
                height 1600
              }
            }
            """.trimIndent()
        )

        val skins = SkinCatalog.list(tmp)
        assertEquals(1, skins.size)
        val info = skins.single()
        assertEquals("pax_a910s", info.name)
        // Must take parts.device.display (720x1440), not the later layouts block (800x1600).
        assertEquals(720, info.displayWidth)
        assertEquals(1440, info.displayHeight)
        assertTrue(info.hasDeclaredSize)
    }

    @Test
    fun `skin matching accepts either orientation and is permissive when size is unknown`() {
        val known = SkinCatalog.read(
            Files.createTempDirectory("skin2").resolve("s").also {
                it.createDirectories()
                it.resolve("layout").writeText("display {\n  width 720\n  height 1440\n}\n")
            }
        )
        assertTrue(known.matches(720, 1440))
        assertTrue(known.matches(1440, 720), "a landscape use of a portrait skin is legitimate")
        assertFalse(known.matches(1080, 2400))

        // No layout size declared -> we must not invent a mismatch warning.
        val unknown = SkinCatalog.read(
            Files.createTempDirectory("skin3").resolve("s").also {
                it.createDirectories()
                it.resolve("layout").writeText("parts {\n}\n")
            }
        )
        assertFalse(unknown.hasDeclaredSize)
        assertTrue(unknown.matches(999, 999))
    }

    @Test
    fun `a directory without a layout file is not a skin`() {
        val tmp = Files.createTempDirectory("skin4")
        tmp.resolve("not_a_skin").createDirectories()
        assertTrue(SkinCatalog.list(tmp).isEmpty())
        assertFalse(SkinCatalog.isValidSkin(tmp.resolve("not_a_skin")))
        assertNull(SkinCatalog.parseLayoutDisplay(tmp.resolve("not_a_skin/layout")))
    }

    @Test
    fun `reads this machine's real skins when the SDK is present`() {
        val sdk = (AndroidSdkLocator.locate() as? SdkResolution.Found)?.sdk ?: return
        val skins = SkinCatalog.list(sdk)
        if (skins.isEmpty()) return

        val pixel6 = skins.firstOrNull { it.name == "pixel_6" } ?: return
        // Verified against $SDK/skins/pixel_6/layout.
        assertEquals(1080, pixel6.displayWidth)
        assertEquals(2400, pixel6.displayHeight)
    }

    // ---------- AndroidSdkLocator ----------

    @Test
    fun `commands always carry sdk_root and the env always pins the sdk and avd home`() {
        val sdk = (AndroidSdkLocator.locate() as? SdkResolution.Found)?.sdk ?: return

        val env = sdk.env()
        assertEquals(sdk.root.toAbsolutePath().toString(), env["ANDROID_SDK_ROOT"])
        assertEquals(sdk.root.toAbsolutePath().toString(), env["ANDROID_HOME"])
        assertTrue(env.containsKey("ANDROID_AVD_HOME"))

        // The two tools differ, and getting this backwards breaks both of them:
        //
        //   sdkmanager  REQUIRES --sdk_root. Without it, a PATH-resolved (Homebrew) sdkmanager
        //               inspects its own install tree and reports almost nothing installed —
        //               measured here as 0 system images versus 8 with the flag.
        //   avdmanager  REJECTS it outright: "Flag '--sdk_root=…' is not valid for 'create avd'".
        //               It also ignores ANDROID_SDK_ROOT, deriving the root from its own location,
        //               so it cannot be pointed at a different SDK at all. That is why AvdManagerService
        //               creates AVDs directly instead of shelling out to it.
        if (sdk.sdkmanager != null) {
            val cmd = sdk.sdkManagerCommand("--list_installed")
            assertTrue(cmd.any { it.startsWith("--sdk_root=") }, "sdkmanager command lost --sdk_root")
        }
        if (sdk.avdmanager != null) {
            val cmd = sdk.avdManagerCommand("list", "avd")
            assertFalse(
                cmd.any { it.startsWith("--sdk_root=") },
                "avdmanager rejects --sdk_root; passing it makes every call fail",
            )
        }
    }

    @Test
    fun `adb commands pin the serial so concurrent emulators never cross talk`() {
        val sdk = (AndroidSdkLocator.locate() as? SdkResolution.Found)?.sdk ?: return
        if (sdk.adb == null) return

        val cmd = sdk.adbCommand("emulator-5554", "shell", "getprop", "ro.product.model")
        assertEquals(listOf("-s", "emulator-5554"), cmd.subList(1, 3))
        // Omitting the serial must not emit a dangling -s.
        assertFalse(sdk.adbCommand(null, "devices").contains("-s"))
    }

    @Test
    fun `reports an actionable failure when pointed at a directory that is not an SDK`() {
        val empty = Files.createTempDirectory("not-an-sdk")
        // Neutralise the real SDK so the OS-default candidate cannot rescue the lookup.
        val result = AndroidSdkLocator.locate(explicitPath = empty.toString())
        if (result is SdkResolution.Found) {
            // A real SDK exists at the OS default location, so discovery correctly fell through.
            assertTrue(result.sdk.root != empty)
        } else {
            result as SdkResolution.Missing
            assertTrue(result.remediation.contains("ANDROID_HOME"))
            assertTrue(result.reason.isNotBlank())
        }
    }

    @Test
    fun `avd home defaults to the location android studio reads`() {
        val expected = Paths.get(System.getProperty("user.home"), ".android", "avd")
        val actual = AndroidSdkLocator.avdHome()
        // ANDROID_AVD_HOME wins if the developer set it; otherwise it must be Studio's location,
        // because having our AVDs visible in Device Manager is a deliberate design goal.
        if (System.getenv("ANDROID_AVD_HOME").isNullOrBlank()) {
            assertEquals(expected, actual)
        }
    }
}
