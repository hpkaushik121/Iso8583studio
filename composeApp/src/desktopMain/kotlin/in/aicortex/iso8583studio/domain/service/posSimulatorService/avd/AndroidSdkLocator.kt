package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.readText

/**
 * Finds the Android SDK and the individual binaries we shell out to.
 *
 * ## The footgun this exists to prevent
 *
 * On this machine `sdkmanager` and `avdmanager` are on PATH via Homebrew and symlink into
 * `/opt/homebrew/share/android-commandlinetools`, which is a *different SDK root* from the real
 * one at `~/Library/Android/sdk`. Consequently:
 *
 *     $ sdkmanager --list_installed                       # reports ONLY "emulator"
 *     $ sdkmanager --sdk_root=~/Library/Android/sdk ...    # reports all 7 system images
 *
 * So every invocation must pass `--sdk_root` *and* export `ANDROID_SDK_ROOT`/`ANDROID_AVD_HOME` to
 * the child process. Omitting either silently creates AVDs in the wrong tree and reports "no system
 * images installed" on a machine that has seven. Use [AndroidSdk.sdkManagerCommand] /
 * [AndroidSdk.avdManagerCommand] and [AndroidSdk.env] rather than building command lines by hand.
 *
 * Binaries are resolved individually, because they are not all in the same place: here `emulator`
 * exists only inside the SDK while `adb`/`avdmanager`/`sdkmanager` are only on PATH.
 */
object AndroidSdkLocator {

    fun locate(explicitPath: String? = null, projectDir: Path? = null): SdkResolution {
        val candidates = candidateRoots(explicitPath, projectDir)
        val root = candidates.firstOrNull { looksLikeSdk(it.path) }

        if (root == null) {
            val tried = candidates.joinToString("\n") { "  • ${it.source.label}: ${it.path}" }
            return SdkResolution.Missing(
                reason = if (candidates.isEmpty()) {
                    "No Android SDK location could be determined."
                } else {
                    "No Android SDK found. Checked:\n$tried"
                },
                remediation = "Set ANDROID_HOME, install the SDK via Android Studio, or choose the " +
                    "SDK folder in Settings. On macOS the default is ~/Library/Android/sdk.",
            )
        }

        val sdk = AndroidSdk(
            root = root.path,
            source = root.source,
            adb = resolveBinary(root.path, "adb", listOf("platform-tools")),
            emulator = resolveBinary(root.path, "emulator", listOf("emulator")),
            avdmanager = resolveBinary(root.path, "avdmanager", CMDLINE_TOOL_DIRS, scriptExtension = true),
            sdkmanager = resolveBinary(root.path, "sdkmanager", CMDLINE_TOOL_DIRS, scriptExtension = true),
            avdHome = avdHome(),
        )

        val warnings = buildList {
            if (sdk.emulator == null) {
                add("`emulator` not found. Install the Emulator package in the SDK Manager.")
            }
            if (sdk.avdmanager == null || sdk.sdkmanager == null) {
                add(
                    "`avdmanager`/`sdkmanager` not found in ${root.path}/cmdline-tools/latest/bin " +
                        "or on PATH. Install \"Android SDK Command-line Tools\" in the SDK Manager."
                )
            }
            if (sdk.adb == null) {
                add("`adb` not found. Install Android SDK Platform-Tools.")
            }
        }
        return SdkResolution.Found(sdk, warnings)
    }

    /** Discovery order, most explicit first. */
    private fun candidateRoots(explicitPath: String?, projectDir: Path?): List<Candidate> = buildList {
        explicitPath?.takeIf { it.isNotBlank() }?.let {
            add(Candidate(Paths.get(it), SdkSource.EXPLICIT))
        }
        System.getenv("ANDROID_HOME")?.takeIf { it.isNotBlank() }?.let {
            add(Candidate(Paths.get(it), SdkSource.ANDROID_HOME))
        }
        System.getenv("ANDROID_SDK_ROOT")?.takeIf { it.isNotBlank() }?.let {
            add(Candidate(Paths.get(it), SdkSource.ANDROID_SDK_ROOT))
        }
        projectDir?.let { dir ->
            localPropertiesSdkDir(dir)?.let { add(Candidate(it, SdkSource.LOCAL_PROPERTIES)) }
        }
        add(Candidate(osDefaultRoot(), SdkSource.OS_DEFAULT))
    }

    /** `sdk.dir=` from a project's gitignored `local.properties`. */
    private fun localPropertiesSdkDir(projectDir: Path): Path? {
        val file = projectDir.resolve("local.properties")
        if (!file.exists()) return null
        return runCatching {
            file.readText().lineSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("sdk.dir=") }
                ?.substringAfter("sdk.dir=")
                ?.trim()
                // local.properties escapes Windows separators as `C\:\\Users\\...`.
                ?.replace("\\:", ":")
                ?.replace("\\\\", "\\")
                ?.takeIf { it.isNotEmpty() }
                ?.let(Paths::get)
        }.getOrNull()
    }

    private fun osDefaultRoot(): Path {
        val home = System.getProperty("user.home")
        return when {
            isWindows -> {
                val localAppData = System.getenv("LOCALAPPDATA") ?: "$home\\AppData\\Local"
                Paths.get(localAppData, "Android", "Sdk")
            }
            isMac -> Paths.get(home, "Library", "Android", "sdk")
            else -> Paths.get(home, "Android", "Sdk")
        }
    }

    /**
     * `ANDROID_AVD_HOME` wins, then `~/.android/avd` — the location Android Studio's Device Manager
     * reads, which is deliberate: seeing our AVDs in Studio is the point.
     */
    fun avdHome(): Path {
        System.getenv("ANDROID_AVD_HOME")?.takeIf { it.isNotBlank() }?.let { return Paths.get(it) }
        return Paths.get(System.getProperty("user.home"), ".android", "avd")
    }

    /**
     * An SDK root is credible if it holds at least one of the directories the SDK actually creates.
     * Deliberately lenient — a machine with only `cmdline-tools` installed is a valid starting
     * point from which we can download everything else.
     */
    private fun looksLikeSdk(path: Path): Boolean =
        path.isDirectory() && MARKER_DIRS.any { path.resolve(it).isDirectory() }

    /**
     * Prefers the SDK's own copy so the binary matches the installed packages, then falls back to
     * PATH. Both halves are needed: on this machine `emulator` is SDK-only and `avdmanager` is
     * PATH-only.
     */
    private fun resolveBinary(
        root: Path,
        name: String,
        relativeDirs: List<String>,
        scriptExtension: Boolean = false,
    ): Path? {
        val fileNames = when {
            !isWindows -> listOf(name)
            scriptExtension -> listOf("$name.bat", "$name.exe", name)
            else -> listOf("$name.exe", name)
        }
        for (dir in relativeDirs) {
            for (fileName in fileNames) {
                val candidate = root.resolve(dir).resolve(fileName)
                if (candidate.exists()) return candidate
            }
        }
        return onPath(fileNames)
    }

    private fun onPath(fileNames: List<String>): Path? {
        val pathEnv = System.getenv("PATH") ?: return null
        for (dir in pathEnv.split(File_PATH_SEPARATOR)) {
            if (dir.isBlank()) continue
            for (fileName in fileNames) {
                val candidate = runCatching { Paths.get(dir, fileName) }.getOrNull() ?: continue
                if (candidate.exists() && (isWindows || candidate.isExecutable())) return candidate
            }
        }
        return null
    }

    private val File_PATH_SEPARATOR: String get() = System.getProperty("path.separator") ?: ":"

    private val osName: String get() = System.getProperty("os.name").orEmpty().lowercase()
    private val isWindows: Boolean get() = osName.contains("win")
    private val isMac: Boolean get() = osName.contains("mac") || osName.contains("darwin")

    private val MARKER_DIRS = listOf("platform-tools", "emulator", "system-images", "cmdline-tools", "platforms")

    /** `latest` first; versioned dirs and the legacy `tools/bin` are fallbacks. */
    private val CMDLINE_TOOL_DIRS = listOf(
        "cmdline-tools/latest/bin",
        "cmdline-tools/bin",
        "tools/bin",
    )

    private data class Candidate(val path: Path, val source: SdkSource)
}

data class AndroidSdk(
    val root: Path,
    val source: SdkSource,
    val adb: Path?,
    val emulator: Path?,
    val avdmanager: Path?,
    val sdkmanager: Path?,
    val avdHome: Path,
) {
    val systemImagesDir: Path get() = root.resolve("system-images")
    val skinsDir: Path get() = root.resolve("skins")
    val licensesDir: Path get() = root.resolve("licenses")

    /** True once every binary the AVD pipeline needs is present. */
    val isComplete: Boolean get() = adb != null && emulator != null && avdmanager != null && sdkmanager != null

    /** Licences must be accepted before `sdkmanager` will install anything. */
    val licensesAccepted: Boolean get() = licensesDir.resolve("android-sdk-license").exists()

    /**
     * Environment for every child process. Both `ANDROID_SDK_ROOT` and the legacy `ANDROID_HOME`
     * are set because different tool versions read different ones, and `ANDROID_AVD_HOME` pins
     * where AVDs land.
     */
    fun env(): Map<String, String> = mapOf(
        "ANDROID_SDK_ROOT" to root.toAbsolutePath().toString(),
        "ANDROID_HOME" to root.toAbsolutePath().toString(),
        "ANDROID_AVD_HOME" to avdHome.toAbsolutePath().toString(),
    )

    /**
     * Always carries `--sdk_root`. Without it a PATH-resolved `sdkmanager` inspects its own
     * install tree and reports almost nothing installed.
     */
    fun sdkManagerCommand(vararg args: String): List<String> {
        val binary = sdkmanager ?: error("sdkmanager not found in $root or on PATH")
        return listOf(binary.toString(), "--sdk_root=${root.toAbsolutePath()}") + args
    }

    /**
     * **Unlike [sdkManagerCommand], this must NOT carry `--sdk_root`** — `avdmanager` rejects it
     * outright (`Flag '--sdk_root=…' is not valid for 'create avd'`).
     *
     * Worse, `avdmanager` cannot be pointed at a different SDK at all: it derives the root from its
     * own install location via `-Dcom.android.sdkmanager.toolsdir`, and ignores `ANDROID_SDK_ROOT`.
     * On a machine where the cmdline-tools come from Homebrew but the images live in
     * `~/Library/Android/sdk`, it therefore reports zero installed system images.
     *
     * That is why AVD creation and deletion are done directly by [AvdManagerService] rather than
     * through this tool — see its KDoc. This builder remains only for incidental read-only queries.
     */
    fun avdManagerCommand(vararg args: String): List<String> {
        val binary = avdmanager ?: error("avdmanager not found in $root or on PATH")
        return listOf(binary.toString(), "--silent") + args
    }

    fun adbCommand(serial: String?, vararg args: String): List<String> {
        val binary = adb ?: error("adb not found in $root or on PATH")
        return buildList {
            add(binary.toString())
            if (!serial.isNullOrBlank()) { add("-s"); add(serial) }
            addAll(args)
        }
    }
}

enum class SdkSource(val label: String) {
    EXPLICIT("Configured in Settings"),
    ANDROID_HOME("ANDROID_HOME"),
    ANDROID_SDK_ROOT("ANDROID_SDK_ROOT"),
    LOCAL_PROPERTIES("local.properties sdk.dir"),
    OS_DEFAULT("Default install location"),
}

sealed interface SdkResolution {
    data class Found(val sdk: AndroidSdk, val warnings: List<String> = emptyList()) : SdkResolution
    data class Missing(val reason: String, val remediation: String) : SdkResolution
}
