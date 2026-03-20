package `in`.aicortex.iso8583studio.license

import java.lang.management.ManagementFactory

/**
 * Anti-instrumentation and anti-debugging detection.
 *
 * Checks for:
 * - Java agents (-javaagent, -agentlib, -agentpath)
 * - JDWP debugger attachment (-Xdebug, -agentlib:jdwp)
 * - Known bytecode manipulation frameworks in classpath
 *
 * Detection is "silent degradation" — sets an internal flag
 * that causes license validation to fail on the next check cycle,
 * rather than crashing immediately (which helps attackers find the check).
 */
object AntiTamper {

    @Volatile
    private var tamperFlag = false

    @Volatile
    private var initialized = false

    val isTamperDetected: Boolean get() = tamperFlag

    fun initialize() {
        if (initialized) return
        initialized = true
        performChecks()
    }

    fun recheck() {
        performChecks()
    }

    private fun performChecks() {
        if (!BuildConfig.IS_RELEASE) return

        if (detectAgentArgs()) {
            tamperFlag = true
            return
        }
        if (detectDebugger()) {
            tamperFlag = true
            return
        }
        if (detectKnownManipulationLibraries()) {
            tamperFlag = true
            return
        }
    }

    private fun detectAgentArgs(): Boolean {
        return try {
            val args = ManagementFactory.getRuntimeMXBean().inputArguments
            args.any { arg ->
                val lower = arg.lowercase()
                lower.startsWith("-javaagent") ||
                lower.startsWith("-agentlib") ||
                lower.startsWith("-agentpath")
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun detectDebugger(): Boolean {
        return try {
            val args = ManagementFactory.getRuntimeMXBean().inputArguments
            val hasJdwp = args.any { arg ->
                val lower = arg.lowercase()
                lower.contains("jdwp") || lower.contains("-xdebug") ||
                lower.contains("suspend=y") || lower.contains("transport=dt_socket")
            }
            if (hasJdwp) return true

            val threadNames = Thread.getAllStackTraces().keys.map { it.name.lowercase() }
            threadNames.any { it.contains("jdwp") || it.contains("debugger") }
        } catch (_: Exception) {
            false
        }
    }

    private fun detectKnownManipulationLibraries(): Boolean {
        val suspiciousClasses = listOf(
            "net.bytebuddy.agent.ByteBuddyAgent",
            "org.objectweb.asm.ClassVisitor",
            "javassist.CtClass",
            "com.sun.tools.attach.VirtualMachine"
        )
        return suspiciousClasses.any { className ->
            try {
                Class.forName(className, false, ClassLoader.getSystemClassLoader())
                true
            } catch (_: ClassNotFoundException) {
                false
            } catch (_: Exception) {
                false
            }
        }
    }
}
