package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdSpec
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ResolvedTerminal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Boots a prepared AVD into a running replica of the target terminal.
 *
 * The identity step is the interesting one. `emulator -prop ro.product.model=…` **does not work** —
 * verified against `android-31;default`, where all five product properties came back stock. `ro.*`
 * is immutable once init has read it, so runtime `setprop` fails too. The only mechanism that works
 * is rewriting the partition `build.prop` files and rebooting, which costs one extra boot cycle and
 * is why [BootResult.identityApplied] exists.
 */
class DeviceBootstrapper(
    private val sdk: AndroidSdk,
    private val avdManager: AvdManagerService = AvdManagerService(sdk),
) {

    fun boot(spec: AvdSpec, terminal: ResolvedTerminal): Flow<AvdStep> = flow {
        // ---- locate SDK ----
        emit(AvdStep.PhaseStart(now(), AvdPhase.LOCATE_SDK))
        if (sdk.emulator == null) {
            abort(AvdPhase.LOCATE_SDK, "The emulator binary was not found.",
                "Install the Emulator package in the SDK Manager.")
            return@flow
        }
        if (sdk.adb == null) {
            abort(AvdPhase.LOCATE_SDK, "adb was not found.", "Install Android SDK Platform-Tools.")
            return@flow
        }
        emit(AvdStep.Line(now(), AvdPhase.LOCATE_SDK, "SDK ${sdk.root}"))
        emit(AvdStep.PhaseEnd(now(), AvdPhase.LOCATE_SDK, ok = true))

        // ---- prepared? ----
        emit(AvdStep.PhaseStart(now(), AvdPhase.VERIFY_PREPARED))
        if (!avdManager.exists(spec.avdName)) {
            abort(AvdPhase.VERIFY_PREPARED, "AVD \"${spec.avdName}\" does not exist.",
                "Run Prepare first, or enable auto-prepare on launch.")
            return@flow
        }
        emit(AvdStep.Line(now(), AvdPhase.VERIFY_PREPARED, "${spec.avdName} is present."))
        emit(AvdStep.PhaseEnd(now(), AvdPhase.VERIFY_PREPARED, ok = true))

        // ---- start ----
        emit(AvdStep.PhaseStart(now(), AvdPhase.START_EMULATOR))
        val port = runCatching { EmulatorLauncher.allocatePort(spec.consolePort) }.getOrElse {
            abort(AvdPhase.START_EMULATOR, it.message ?: "No free console port.",
                "Close another running emulator and try again.")
            return@flow
        }
        val serial = EmulatorLauncher.serialFor(port)
        val argv = EmulatorLauncher.buildArgv(sdk, spec, port)
        emit(AvdStep.Command(now(), AvdPhase.START_EMULATOR, argv))

        val bootLines = java.util.Collections.synchronizedList(mutableListOf<String>())
        val process = EmulatorLauncher.launch(sdk, spec, port) { bootLines += it }
        emit(AvdStep.Line(now(), AvdPhase.START_EMULATOR, "Started as $serial"))
        emit(AvdStep.PhaseEnd(now(), AvdPhase.START_EMULATOR, ok = true))

        val adb = AdbClient(sdk, serial)

        // ---- wait for boot ----
        emit(AvdStep.PhaseStart(now(), AvdPhase.WAIT_BOOT))
        var lastReported = -1L
        val booted = adb.awaitBoot(timeoutMs = BOOT_TIMEOUT_MS) { elapsed ->
            val seconds = elapsed / 10_000
            if (seconds != lastReported) {
                lastReported = seconds
                emit(AvdStep.Line(now(), AvdPhase.WAIT_BOOT, "waiting… ${elapsed / 1000}s"))
            }
        }
        if (!booted) {
            // Deliberately leave the emulator running: its window and logs are the only way to
            // diagnose a boot hang, and killing it destroys the evidence.
            synchronized(bootLines) { bootLines.takeLast(20) }
                .forEach { emit(AvdStep.Line(now(), AvdPhase.WAIT_BOOT, it, true)) }
            abort(
                AvdPhase.WAIT_BOOT,
                "Boot did not complete within ${BOOT_TIMEOUT_MS / 1000}s.",
                "The emulator is still running so you can inspect it — open its window, or run " +
                    "`adb -s $serial logcat`.",
            )
            return@flow
        }
        adb.awaitBootAnimation()
        emit(AvdStep.Line(now(), AvdPhase.WAIT_BOOT, "Boot completed."))
        emit(AvdStep.PhaseEnd(now(), AvdPhase.WAIT_BOOT, ok = true))

        // ---- identity ----
        emit(AvdStep.PhaseStart(now(), AvdPhase.VERIFY_IDENTITY))
        val wanted = terminal.terminal.effectiveBootProps()
        val before = wanted.keys.associateWith { adb.getProp(it) }
        val needsPatch = wanted.any { (k, v) -> before[k] != v }

        var identityApplied = false
        if (!needsPatch) {
            emit(AvdStep.Line(now(), AvdPhase.VERIFY_IDENTITY, "Identity already matches."))
            identityApplied = true
        } else if (!spec.writableSystem) {
            emit(AvdStep.Line(now(), AvdPhase.VERIFY_IDENTITY,
                "Skipping identity patch: /system is not writable for this AVD.", true))
        } else {
            emit(AvdStep.Line(now(), AvdPhase.VERIFY_IDENTITY, "Patching build.prop (adb -prop cannot set ro.product.*)"))
            identityApplied = patchIdentity(adb, wanted)

            if (identityApplied) {
                emit(AvdStep.Line(now(), AvdPhase.VERIFY_IDENTITY, "Rebooting so init re-reads the properties…"))
                adb.reboot()
                if (!adb.awaitOnline() || !adb.awaitBoot(timeoutMs = BOOT_TIMEOUT_MS)) {
                    abort(AvdPhase.VERIFY_IDENTITY, "The device did not come back after the identity reboot.",
                        "Try Power off, then Power on again.")
                    return@flow
                }
                adb.awaitBootAnimation()
            }
        }

        val after = wanted.keys.associateWith { adb.getProp(it) }
        wanted.forEach { (key, want) ->
            val got = after[key].orEmpty()
            emit(AvdStep.Line(now(), AvdPhase.VERIFY_IDENTITY,
                "  $key = $got${if (got == want) "" else "   (wanted $want)"}", got != want))
        }
        val matched = wanted.count { (k, v) -> after[k] == v }
        emit(AvdStep.Line(now(), AvdPhase.VERIFY_IDENTITY, "$matched of ${wanted.size} identity properties match."))
        emit(AvdStep.PhaseEnd(now(), AvdPhase.VERIFY_IDENTITY, ok = true))

        emit(
            AvdStep.Done(
                now(),
                summary = "${terminal.terminal.displayName} running as $serial " +
                    "($matched/${wanted.size} identity properties matched" +
                    (if (identityApplied) "" else ", identity not applied") + ")",
            )
        )
    }

    /**
     * Rewrites the product properties in every partition `build.prop`.
     *
     * Two details that make the difference between working and silently doing nothing:
     *
     *  - The resolved `ro.product.<key>` comes from the **per-partition** properties
     *    (`ro.product.vendor.model` and friends). On this image `ro.product.property_source_order`
     *    is empty and `ro.product.system.model` held an unrelated value, so patching only the plain
     *    key changes nothing.
     *  - For `ro.*` the **first** definition wins. Lines must be replaced in place; appending is
     *    ignored without any warning.
     */
    private suspend fun patchIdentity(adb: AdbClient, wanted: Map<String, String>): Boolean {
        if (!adb.root().ok) return false
        // overlayfs on android-31;default — no disable-verity/reboot dance needed.
        if (!adb.remount().ok) return false

        val temp = Files.createTempDirectory("iso8583-buildprop")
        var patchedAny = false
        try {
            for (partition in PARTITIONS) {
                val remote = "/$partition/build.prop"
                val local = temp.resolve("$partition.build.prop")
                if (!adb.pull(remote, local.toString()).ok) continue

                val original = runCatching { local.readText() }.getOrNull() ?: continue
                val patched = rewriteProps(original, partition, wanted)
                if (patched == original) continue

                local.writeText(patched)
                if (adb.push(local.toString(), remote).ok) {
                    adb.shell("chmod", "644", remote)
                    patchedAny = true
                }
            }
        } finally {
            runCatching { Files.walk(temp).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
        }
        return patchedAny
    }

    /** Pure so it can be tested without a device. */
    internal fun rewriteProps(content: String, partition: String, wanted: Map<String, String>): String {
        var result = content
        for ((fullKey, value) in wanted) {
            val suffix = fullKey.removePrefix("ro.product.")
            for (key in listOf("ro.product.$partition.$suffix", fullKey)) {
                val pattern = Regex("(?m)^${Regex.escape(key)}=.*$")
                result = if (pattern.containsMatchIn(result)) {
                    pattern.replace(result, "$key=$value")
                } else {
                    result.trimEnd() + "\n$key=$value\n"
                }
            }
        }
        return result
    }

    private suspend fun FlowCollector<AvdStep>.abort(phase: AvdPhase, reason: String, remediation: String) {
        emit(AvdStep.PhaseEnd(now(), phase, ok = false))
        emit(AvdStep.Aborted(now(), phase, reason, remediation))
    }

    private fun now() = System.currentTimeMillis()

    companion object {
        const val BOOT_TIMEOUT_MS = 180_000L

        /** Partitions that carry a build.prop on emulator images. */
        private val PARTITIONS = listOf("system", "vendor", "product", "system_ext", "odm")
    }
}

data class BootResult(
    val serial: String,
    val port: Int,
    val identityApplied: Boolean,
    val process: ManagedProcess?,
)
