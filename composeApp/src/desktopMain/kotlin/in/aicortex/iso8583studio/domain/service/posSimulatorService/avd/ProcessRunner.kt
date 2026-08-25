package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Runs external SDK tools (`avdmanager`, `adb`, `emulator`, `sdkmanager`).
 *
 * Modelled on the house pattern in `FirmwareTab.runPio` — argv as a `List<String>` (never a shell
 * string), `redirectErrorStream(true)` with a single reader loop, all inside
 * `withContext(Dispatchers.IO)` — but with the two things that pattern lacks, both of which are
 * harmless for a short-lived `pio` invocation and destructive for a 2 GB emulator VM:
 *
 * 1. **The child is always destroyed.** `runPio` calls `proc.waitFor()` with no `try/finally`, so
 *    cancelling the coroutine leaves the process running: `readLine()` on a process pipe is a
 *    blocking native call that ignores coroutine cancellation.
 * 2. **Timeouts actually kill.** `withTimeout` is deliberately not used here, for the reason
 *    `SerialTransport` documents about jSerialComm: it cannot interrupt a blocking native read. We
 *    track a wall-clock deadline and call `destroyForcibly()` ourselves.
 *
 * Long-lived processes (the emulator) are started with [start] and registered in [ProcessRegistry],
 * which a JVM shutdown hook drains — otherwise a qemu process outlives the app that spawned it.
 */
object ProcessRunner {

    /** No timeout. For processes whose lifetime is managed elsewhere. */
    const val NO_TIMEOUT = 0L

    /**
     * Runs to completion and returns the result. Every line of merged stdout/stderr is handed to
     * [onLine] as it arrives, so callers can stream it into a log without waiting for exit.
     */
    suspend fun run(
        argv: List<String>,
        env: Map<String, String> = emptyMap(),
        cwd: Path? = null,
        timeoutMs: Long = 60_000L,
        /**
         * Written to the child's stdin, which is then closed.
         *
         * Needed because `avdmanager create avd` asks "Do you wish to create a custom hardware
         * profile?" whenever no `--device` is passed — and we deliberately never pass one, since
         * `-d` cannot set RAM or geometry anyway and stamps a `hw.device.hash2` that then drifts
         * against our model forever. Feeding it "no" keeps the call non-interactive.
         */
        stdin: String? = null,
        onLine: (String) -> Unit = {},
    ): ProcessResult = withContext(Dispatchers.IO) {
        require(argv.isNotEmpty()) { "argv must not be empty" }

        val started = System.currentTimeMillis()
        val lines = java.util.Collections.synchronizedList(mutableListOf<String>())
        var process: Process? = null

        try {
            process = launch(argv, env, cwd)
            val handle = process

            // Always close stdin, even when empty: a tool waiting on input it will never get would
            // otherwise hang until the timeout.
            runCatching {
                handle.outputStream.use { out ->
                    if (!stdin.isNullOrEmpty()) out.write(stdin.toByteArray())
                }
            }

            // The read MUST happen off this thread. `readLine()` on a process pipe blocks until a
            // line arrives or the stream closes, so checking a deadline around it never fires — a
            // child that prints nothing and sleeps would pin us for its whole lifetime. Process
            // output is pumped by a daemon thread; the deadline is enforced by `waitFor(timeout)`,
            // which is a genuinely bounded wait.
            val pump = Thread({
                runCatching {
                    handle.inputStream.bufferedReader().use { reader ->
                        while (true) {
                            val line = reader.readLine() ?: break
                            lines += line
                            onLine(line)
                        }
                    }
                }
            }, "process-run-${argv.first()}").apply { isDaemon = true; start() }

            val exited = if (timeoutMs == NO_TIMEOUT) {
                handle.waitFor(); true
            } else {
                handle.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            }

            if (!exited) {
                handle.destroyForcibly()
                // Let the pump drain whatever was already buffered before reporting.
                pump.join(250)
                ProcessResult(argv, TIMEOUT_EXIT, lines.toList(), System.currentTimeMillis() - started, true)
            } else {
                // Exited cleanly: give the pump a moment to finish the tail of the stream.
                pump.join(1_000)
                ProcessResult(argv, handle.exitValue(), lines.toList(), System.currentTimeMillis() - started, false)
            }
        } catch (t: Throwable) {
            ProcessResult(
                argv = argv,
                exitCode = LAUNCH_FAILED_EXIT,
                lines = lines + "Execution failed: ${t.message ?: t::class.simpleName}",
                durationMs = System.currentTimeMillis() - started,
                timedOut = false,
                failure = t,
            )
        } finally {
            // The guarantee runPio does not make: whatever happened — normal return, timeout,
            // coroutine cancellation, exception — the child does not survive this call.
            process?.let { if (it.isAlive) it.destroy() }
        }
    }

    /**
     * Starts a long-lived process and returns immediately. The caller owns its lifetime and must
     * eventually call [ManagedProcess.stop]; until then the shutdown hook is the backstop.
     */
    fun start(
        argv: List<String>,
        env: Map<String, String> = emptyMap(),
        cwd: Path? = null,
        label: String = argv.first(),
        onLine: (String) -> Unit = {},
    ): ManagedProcess {
        require(argv.isNotEmpty()) { "argv must not be empty" }
        val process = launch(argv, env, cwd)
        val managed = ManagedProcess(label, argv, process, onLine)
        ProcessRegistry.register(managed)
        return managed
    }

    private fun launch(argv: List<String>, env: Map<String, String>, cwd: Path?): Process {
        val builder = ProcessBuilder(argv).redirectErrorStream(true)
        cwd?.let { builder.directory(it.toFile()) }
        // putAll, not putIfAbsent: the environment is pre-seeded from the parent, and these values
        // (ANDROID_SDK_ROOT, ANDROID_AVD_HOME) are meant to override whatever the shell had.
        if (env.isNotEmpty()) builder.environment().putAll(env)
        return builder.start()
    }

    const val TIMEOUT_EXIT = -2
    const val LAUNCH_FAILED_EXIT = -1
}

data class ProcessResult(
    val argv: List<String>,
    val exitCode: Int,
    val lines: List<String>,
    val durationMs: Long,
    val timedOut: Boolean,
    val failure: Throwable? = null,
) {
    val ok: Boolean get() = exitCode == 0
    val output: String get() = lines.joinToString("\n")

    /** `avdmanager create avd -n ISO8583_PAX_A910S …`, for the command line shown in the stepper. */
    val commandLine: String get() = argv.joinToString(" ")
}

/**
 * A running child process the app is responsible for. Output is pumped on a daemon thread rather
 * than a coroutine, because the read is a blocking native call and a daemon thread will not hold
 * the JVM open if something goes wrong.
 */
class ManagedProcess internal constructor(
    val label: String,
    val argv: List<String>,
    private val process: Process,
    onLine: (String) -> Unit,
) {
    val id: Long = nextId.incrementAndGet()

    val isAlive: Boolean get() = process.isAlive

    val exitCode: Int? get() = if (process.isAlive) null else process.exitValue()

    private val pump = Thread({
        runCatching {
            process.inputStream.bufferedReader().use { reader ->
                while (true) onLine(reader.readLine() ?: break)
            }
        }
    }, "process-pump-$label-$id").apply {
        isDaemon = true
        start()
    }

    /**
     * Graceful stop, then force. [graceMs] gives the process a chance to shut down cleanly — the
     * emulator flushes its disk images on SIGTERM, so killing it immediately can corrupt userdata.
     */
    fun stop(graceMs: Long = 5_000L) {
        try {
            if (process.isAlive) {
                process.destroy()
                if (!process.waitFor(graceMs, TimeUnit.MILLISECONDS)) process.destroyForcibly()
            }
        } catch (_: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
        } finally {
            ProcessRegistry.unregister(this)
        }
    }

    private companion object {
        val nextId = AtomicLong(0)
    }
}

/**
 * Every long-lived child the app has started.
 *
 * The repo had no shutdown hook at all before this, and `onCloseRequest = ::exitApplication` does
 * no cleanup — so an emulator started from Studio would keep a CPU core and its RAM after Studio
 * exited. [installShutdownHook] is idempotent and should be called once during startup.
 */
object ProcessRegistry {

    private val live = ConcurrentHashMap<Long, ManagedProcess>()

    @Volatile
    private var hookInstalled = false

    val count: Int get() = live.size

    fun register(process: ManagedProcess) {
        live[process.id] = process
    }

    fun unregister(process: ManagedProcess) {
        live.remove(process.id)
    }

    fun installShutdownHook() {
        synchronized(this) {
            if (hookInstalled) return
            hookInstalled = true
            Runtime.getRuntime().addShutdownHook(
                Thread({ stopAll(graceMs = 2_000L) }, "iso8583studio-process-reaper")
            )
        }
    }

    /** Stops everything still running. Safe to call repeatedly. */
    fun stopAll(graceMs: Long = 5_000L) {
        // Snapshot first: stop() removes from the map as it goes.
        live.values.toList().forEach { runCatching { it.stop(graceMs) } }
    }
}
