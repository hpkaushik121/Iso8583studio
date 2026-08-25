package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdSpec
import java.net.ServerSocket

/**
 * Launches and owns an emulator instance.
 *
 * The handle is registered with [ProcessRegistry], so a qemu VM cannot survive the app exiting —
 * previously nothing in the codebase guarded against that, and an abandoned emulator holds a CPU
 * core and its full RAM allocation indefinitely.
 */
object EmulatorLauncher {

    /**
     * The emulator's console port range. Ports are even; adb uses the odd port immediately above,
     * and the device serial becomes `emulator-<consolePort>`.
     */
    private const val FIRST_PORT = 5554
    private const val LAST_PORT = 5682

    /**
     * Finds a free even console port so several simulators can run side by side.
     *
     * Both the console port and the adb port above it must be free — binding only the even one
     * would let two emulators collide on adb.
     */
    fun allocatePort(preferred: Int = 0): Int {
        if (preferred >= FIRST_PORT && preferred % 2 == 0 && isPairFree(preferred)) return preferred
        var port = FIRST_PORT
        while (port <= LAST_PORT) {
            if (isPairFree(port)) return port
            port += 2
        }
        error("No free emulator console port in $FIRST_PORT..$LAST_PORT — too many emulators running.")
    }

    private fun isPairFree(port: Int): Boolean =
        isFree(port) && isFree(port + 1)

    private fun isFree(port: Int): Boolean = runCatching {
        ServerSocket(port).use { true }
    }.getOrDefault(false)

    fun serialFor(port: Int): String = "emulator-$port"

    /**
     * Builds the argv.
     *
     * Note what is deliberately **absent**: `-prop ro.product.*`. Measured against
     * `android-31;default`, those are ignored — the emulator reports its stock identity regardless.
     * Identity spoofing happens after boot by rewriting the partition `build.prop` files; see
     * [DeviceBootstrapper].
     */
    fun buildArgv(sdk: AndroidSdk, spec: AvdSpec, port: Int): List<String> {
        val emulator = sdk.emulator ?: error("emulator binary not found under ${sdk.root}")
        return buildList {
            add(emulator.toString())
            add("-avd"); add(spec.avdName)
            add("-port"); add(port.toString())

            // Required for the identity patch and, later, installing the device host into /system.
            if (spec.writableSystem) add("-writable-system")
            if (spec.selinuxPermissive) { add("-selinux"); add("permissive") }

            // Snapshots silently discard the writable-system overlay, taking the identity patch
            // with it — so a device replica must always cold boot.
            if (spec.coldBoot) add("-no-snapshot-load")
            if (spec.noSnapshotSave) add("-no-snapshot-save")

            if (spec.headless) add("-no-window")
            add("-no-audio")
            add("-no-boot-anim")

            addAll(spec.extraEmulatorArgs)
        }
    }

    fun launch(
        sdk: AndroidSdk,
        spec: AvdSpec,
        port: Int,
        onLine: (String) -> Unit = {},
    ): ManagedProcess = ProcessRunner.start(
        argv = buildArgv(sdk, spec, port),
        env = sdk.env(),
        label = "emulator-${spec.avdName}",
        onLine = onLine,
    )
}
