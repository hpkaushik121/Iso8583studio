package `in`.aicortex.iso8583studio.posSimulator

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.ProcessRegistry
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.ProcessRunner
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * These run real child processes, using POSIX utilities present on macOS and Linux. That is
 * deliberate — the whole point of this class is the interaction with a real OS process, and a mocked
 * `Process` would test nothing that matters.
 */
class ProcessRunnerTest {

    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("win")

    @Test
    fun `captures merged output and a zero exit code`() = runBlocking {
        if (isWindows) return@runBlocking
        val streamed = mutableListOf<String>()
        val result = ProcessRunner.run(listOf("echo", "hello world"), onLine = { streamed += it })

        assertTrue(result.ok)
        assertEquals(0, result.exitCode)
        assertEquals(listOf("hello world"), result.lines)
        // Lines arrive through the callback as they are read, not only in the final result.
        assertEquals(listOf("hello world"), streamed)
        assertFalse(result.timedOut)
    }

    @Test
    fun `reports a non-zero exit code without throwing`() = runBlocking {
        if (isWindows) return@runBlocking
        val result = ProcessRunner.run(listOf("sh", "-c", "exit 3"))
        assertFalse(result.ok)
        assertEquals(3, result.exitCode)
    }

    @Test
    fun `merges stderr into the output stream`() = runBlocking {
        if (isWindows) return@runBlocking
        // Without redirectErrorStream a full stderr pipe deadlocks the reader; this proves the merge.
        val result = ProcessRunner.run(listOf("sh", "-c", "echo out; echo err 1>&2"))
        assertTrue(result.ok)
        assertContains(result.lines, "out")
        assertContains(result.lines, "err")
    }

    @Test
    fun `a launch failure becomes a result rather than an exception`() = runBlocking {
        val result = ProcessRunner.run(listOf("definitely-not-a-real-binary-xyz"))
        assertEquals(ProcessRunner.LAUNCH_FAILED_EXIT, result.exitCode)
        assertNotNull(result.failure)
        assertTrue(result.lines.any { it.startsWith("Execution failed:") })
    }

    @Test
    fun `a hung child is killed on timeout and leaves no orphan`() = runBlocking {
        if (isWindows) return@runBlocking
        // The gap this class exists to close: FirmwareTab.runPio would block here forever, and
        // cancelling its coroutine would leave `sleep` running.
        val marker = "iso8583-timeout-probe-${System.nanoTime()}"
        val result = ProcessRunner.run(
            argv = listOf("sh", "-c", "echo $marker; sleep 30"),
            timeoutMs = 700L,
        )

        assertTrue(result.timedOut, "expected the run to time out")
        assertEquals(ProcessRunner.TIMEOUT_EXIT, result.exitCode)
        assertTrue(result.durationMs < 10_000, "took ${result.durationMs}ms — the kill did not work")

        // And the child is actually gone, not merely abandoned.
        Thread.sleep(300)
        val survivors = ProcessRunner.run(listOf("sh", "-c", "ps -ax -o command | grep -c '[i]so8583-timeout-probe' || true"))
        assertEquals("0", survivors.lines.firstOrNull()?.trim(), "orphaned process survived the timeout")
    }

    @Test
    fun `environment overrides reach the child`() = runBlocking {
        if (isWindows) return@runBlocking
        // putAll, not putIfAbsent: ANDROID_SDK_ROOT must override whatever the parent shell had.
        val result = ProcessRunner.run(
            argv = listOf("sh", "-c", "echo \$ANDROID_SDK_ROOT"),
            env = mapOf("ANDROID_SDK_ROOT" to "/tmp/fake-sdk"),
        )
        assertEquals("/tmp/fake-sdk", result.lines.firstOrNull())
    }

    @Test
    fun `working directory is honoured`() = runBlocking {
        if (isWindows) return@runBlocking
        val dir = java.nio.file.Files.createTempDirectory("cwd-test").toRealPath()
        val result = ProcessRunner.run(listOf("pwd"), cwd = dir)
        assertEquals(dir.toString(), result.lines.firstOrNull())
    }

    @Test
    fun `commandLine renders the argv for display`() = runBlocking {
        if (isWindows) return@runBlocking
        val result = ProcessRunner.run(listOf("echo", "a"))
        assertEquals("echo a", result.commandLine)
    }

    // ---------- long-lived processes and the orphan guard ----------

    @Test
    fun `a started process is registered and deregistered on stop`() {
        if (isWindows) return
        val before = ProcessRegistry.count
        val proc = ProcessRunner.start(listOf("sh", "-c", "sleep 30"), label = "test-sleep")

        assertTrue(proc.isAlive)
        assertEquals(before + 1, ProcessRegistry.count)
        assertEquals(null, proc.exitCode)

        proc.stop(graceMs = 500)
        assertFalse(proc.isAlive)
        assertEquals(before, ProcessRegistry.count, "stop must deregister so the reaper stays accurate")
    }

    @Test
    fun `stopAll clears everything still running`() {
        if (isWindows) return
        val before = ProcessRegistry.count
        repeat(3) { ProcessRunner.start(listOf("sh", "-c", "sleep 30"), label = "test-bulk-$it") }
        assertEquals(before + 3, ProcessRegistry.count)

        ProcessRegistry.stopAll(graceMs = 500)
        assertEquals(0, ProcessRegistry.count)
    }

    @Test
    fun `stopping twice is safe`() {
        if (isWindows) return
        val proc = ProcessRunner.start(listOf("sh", "-c", "sleep 30"), label = "test-double-stop")
        proc.stop(graceMs = 500)
        proc.stop(graceMs = 500)
        assertFalse(proc.isAlive)
    }

    @Test
    fun `a started process streams its output`() {
        if (isWindows) return
        val lines = mutableListOf<String>()
        val proc = ProcessRunner.start(
            argv = listOf("sh", "-c", "echo first; echo second; sleep 5"),
            label = "test-stream",
            onLine = { synchronized(lines) { lines += it } },
        )
        // The pump runs on a daemon thread; give it a moment to drain the first writes.
        Thread.sleep(600)
        proc.stop(graceMs = 500)

        synchronized(lines) {
            assertContains(lines, "first")
            assertContains(lines, "second")
        }
    }

    @Test
    fun `installing the shutdown hook is idempotent`() {
        // Called once at startup, but a double call must not add a second reaper thread.
        ProcessRegistry.installShutdownHook()
        ProcessRegistry.installShutdownHook()
    }
}
