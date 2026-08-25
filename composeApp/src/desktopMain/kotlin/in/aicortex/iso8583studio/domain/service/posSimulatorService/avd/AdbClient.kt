package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

import kotlinx.coroutines.delay

/**
 * adb, pinned to one device.
 *
 * Every call carries `-s <serial>`. Several POS simulators can run at once, and an unpinned adb
 * command silently picks whichever device it feels like — which produces failures that look like
 * device bugs.
 *
 * Waits use explicit wall-clock deadlines rather than `withTimeout`, for the reason `SerialTransport`
 * documents: the work happens in a child process whose blocking reads do not observe coroutine
 * cancellation. [ProcessRunner] enforces per-command timeouts by killing the process.
 */
class AdbClient(private val sdk: AndroidSdk, val serial: String) {

    suspend fun raw(vararg args: String, timeoutMs: Long = 30_000): ProcessResult =
        ProcessRunner.run(sdk.adbCommand(serial, *args), sdk.env(), timeoutMs = timeoutMs)

    /** Runs a shell command and returns its trimmed stdout. */
    suspend fun shell(vararg args: String, timeoutMs: Long = 30_000): String =
        raw("shell", *args, timeoutMs = timeoutMs).lines.joinToString("\n").trim()

    suspend fun getProp(name: String): String =
        shell("getprop", name).lineSequence().firstOrNull()?.trim().orEmpty()

    /** True once the framework reports a completed boot. */
    suspend fun isBootCompleted(): Boolean = getProp("sys.boot_completed") == "1"

    /** The boot animation stops slightly after `sys.boot_completed`; both together mean usable. */
    suspend fun isBootAnimationDone(): Boolean = getProp("init.svc.bootanim") == "stopped"

    suspend fun isOnline(): Boolean =
        ProcessRunner.run(sdk.adbCommand(null, "devices"), sdk.env(), timeoutMs = 10_000)
            .lines.any { it.startsWith(serial) && it.trim().endsWith("device") }

    suspend fun root(): ProcessResult = raw("root", timeoutMs = 30_000)

    /**
     * Mounts the read-only partitions writable.
     *
     * On `android-31;default` this succeeds directly via overlayfs — the
     * `disable-verity` → reboot → `remount` sequence needed on some API 30 images is not required,
     * and attempting it just costs a reboot.
     */
    suspend fun remount(): ProcessResult = raw("remount", timeoutMs = 60_000)

    suspend fun reboot(): ProcessResult = raw("reboot", timeoutMs = 30_000)

    suspend fun pull(remote: String, localPath: String): ProcessResult =
        raw("pull", remote, localPath, timeoutMs = 60_000)

    suspend fun push(localPath: String, remote: String): ProcessResult =
        raw("push", localPath, remote, timeoutMs = 120_000)

    suspend fun install(apkPath: String, reinstall: Boolean = true): ProcessResult =
        if (reinstall) raw("install", "-r", "-t", apkPath, timeoutMs = 300_000)
        else raw("install", "-t", apkPath, timeoutMs = 300_000)

    /** Stops the emulator through its console rather than killing the process. */
    suspend fun emuKill(): ProcessResult = raw("emu", "kill", timeoutMs = 15_000)

    /**
     * Polls until the device reports a completed boot, or the deadline passes.
     *
     * [onTick] receives elapsed seconds so a UI can show progress instead of a frozen spinner —
     * a cold boot can legitimately take a couple of minutes on a loaded machine.
     */
    suspend fun awaitBoot(
        timeoutMs: Long = 180_000,
        pollMs: Long = 2_000,
        onTick: suspend (elapsedMs: Long) -> Unit = {},
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        val started = System.currentTimeMillis()
        while (System.currentTimeMillis() < deadline) {
            if (isBootCompleted()) return true
            onTick(System.currentTimeMillis() - started)
            delay(pollMs)
        }
        return false
    }

    /** Waits for the boot animation to stop. Best-effort: a false result is not fatal. */
    suspend fun awaitBootAnimation(timeoutMs: Long = 60_000, pollMs: Long = 2_000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (isBootAnimationDone()) return true
            delay(pollMs)
        }
        return false
    }

    /** Waits for the device to appear in `adb devices` at all — used after a reboot. */
    suspend fun awaitOnline(timeoutMs: Long = 120_000, pollMs: Long = 1_500): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (isOnline()) return true
            delay(pollMs)
        }
        return false
    }
}
