package `in`.aicortex.iso8583studio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.delay
import javax.swing.JComponent

// ──────────────────────────────────────────────────────────────────────────────
//  Windows 11 Mica / Acrylic glass backdrop via DWM APIs (JNA)
//  macOS translucent window support + OS dark mode detection
//  Safe on all platforms — only activates where supported
// ──────────────────────────────────────────────────────────────────────────────

enum class GlassBackdrop { MICA, ACRYLIC, TABBED_MICA }

/** Global flag: true if glass effect was successfully applied to at least one window. */
val isGlassEffectActive = mutableStateOf(false)

private val osName = System.getProperty("os.name").lowercase()
private val isWindows = osName.contains("win")
private val isMac = osName.contains("mac")

// DWM attribute constants (Windows 11)
private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
private const val DWMWA_SYSTEMBACKDROP_TYPE = 38
private const val DWMSBT_MAINWINDOW = 2       // Mica
private const val DWMSBT_TRANSIENTWINDOW = 3   // Acrylic
private const val DWMSBT_TABBEDWINDOW = 4      // Tabbed Mica

/**
 * Detect if Windows is using dark app mode via the registry.
 * Returns true for dark mode, false for light mode, null if detection fails.
 */
fun detectWindowsDarkMode(): Boolean? {
    if (!isWindows) return null
    return try {
        val process = ProcessBuilder(
            "reg", "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "/v", "AppsUseLightTheme"
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        // Output contains "AppsUseLightTheme    REG_DWORD    0x0" for dark mode
        // 0x0 = dark, 0x1 = light
        when {
            output.contains("0x0") -> true   // dark mode
            output.contains("0x1") -> false  // light mode
            else -> null
        }
    } catch (_: Throwable) {
        null
    }
}

/**
 * Apply Windows 11 glass/Mica backdrop effect to a ComposeWindow.
 * On macOS, makes the window background translucent for a vibrancy-like look.
 * Returns true if successfully applied, false otherwise.
 * Safe on all platforms.
 */
fun applyGlassEffect(
    window: ComposeWindow,
    backdrop: GlassBackdrop = GlassBackdrop.MICA,
    darkMode: Boolean = true,
): Boolean {
    return when {
        isWindows -> applyWindowsGlass(window, backdrop, darkMode)
        isMac -> applyMacTranslucency(window)
        else -> false
    }
}

private fun applyWindowsGlass(
    window: ComposeWindow,
    backdrop: GlassBackdrop,
    darkMode: Boolean,
): Boolean {
    return try {
        val dwmSetAttr = com.sun.jna.Function.getFunction("dwmapi", "DwmSetWindowAttribute")

        // Get HWND — try getWindowID first, fall back to FindWindow
        val hwnd: com.sun.jna.platform.win32.WinDef.HWND = try {
            val hwndLong = com.sun.jna.Native.getWindowID(window)
            if (hwndLong != 0L) {
                com.sun.jna.platform.win32.WinDef.HWND(com.sun.jna.Pointer(hwndLong))
            } else {
                throw RuntimeException("getWindowID returned 0")
            }
        } catch (_: Throwable) {
            val found = com.sun.jna.platform.win32.User32.INSTANCE.FindWindow(null, window.title)
                ?: return false
            found
        }

        // ── 1. Set dark/light title bar to follow OS theme ──
        val darkVal = com.sun.jna.Memory(4).apply { setInt(0, if (darkMode) 1 else 0) }
        val darkResult = dwmSetAttr.invoke(
            Int::class.java,
            arrayOf(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, darkVal, 4)
        ) as Int
        println("[Glass] DwmSetWindowAttribute(DARK_MODE=$darkMode) result: $darkResult (0=OK)")

        // ── 2. Set Mica / Acrylic backdrop on the title bar ──
        val backdropType = when (backdrop) {
            GlassBackdrop.MICA -> DWMSBT_MAINWINDOW
            GlassBackdrop.ACRYLIC -> DWMSBT_TRANSIENTWINDOW
            GlassBackdrop.TABBED_MICA -> DWMSBT_TABBEDWINDOW
        }
        val backdropVal = com.sun.jna.Memory(4).apply { setInt(0, backdropType) }
        val backdropResult = dwmSetAttr.invoke(
            Int::class.java,
            arrayOf(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, backdropVal, 4)
        ) as Int
        println("[Glass] DwmSetWindowAttribute(BACKDROP=$backdropType) result: $backdropResult (0=OK)")

        // ── 3. Extend glass into client area only for transparent/undecorated windows ──
        val isTransparent = window.isUndecorated
        if (isTransparent) {
            val dwmExtend = com.sun.jna.Function.getFunction("dwmapi", "DwmExtendFrameIntoClientArea")
            // Make AWT layers non-opaque so DWM backdrop shows through content
            window.background = java.awt.Color(0, 0, 0, 0)
            window.rootPane.isOpaque = false
            window.rootPane.background = java.awt.Color(0, 0, 0, 0)
            (window.contentPane as? JComponent)?.isOpaque = false
            (window.contentPane as? JComponent)?.background = java.awt.Color(0, 0, 0, 0)

            val margins = com.sun.jna.Memory(16).apply {
                setInt(0, -1); setInt(4, -1); setInt(8, -1); setInt(12, -1)
            }
            val extResult = dwmExtend.invoke(Int::class.java, arrayOf(hwnd, margins)) as Int
            println("[Glass] DwmExtendFrameIntoClientArea result: $extResult (0=OK)")
            isGlassEffectActive.value = true
        }

        // ── 4. Force the title bar to repaint with new attributes ──
        // Toggling the window size forces Windows to redraw the non-client area
        val bounds = window.bounds
        com.sun.jna.platform.win32.User32.INSTANCE.SetWindowPos(
            hwnd, null,
            bounds.x, bounds.y, bounds.width, bounds.height,
            0x0020 /* SWP_FRAMECHANGED */ or 0x0002 /* SWP_NOMOVE */ or 0x0001 /* SWP_NOSIZE */ or 0x0004 /* SWP_NOZORDER */
        )
        window.repaint()

        println("[Glass] Title bar styled successfully (dark=$darkMode, backdrop=$backdropType, fullGlass=$isTransparent)")
        true
    } catch (e: Throwable) {
        println("[Glass] Failed to apply glass effect: ${e.message}")
        e.printStackTrace()
        false
    }
}

private fun applyMacTranslucency(window: ComposeWindow): Boolean {
    return try {
        window.background = java.awt.Color(0, 0, 0, 0)
        window.rootPane.isOpaque = false
        window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
        window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
        isGlassEffectActive.value = true
        true
    } catch (_: Throwable) {
        false
    }
}

/**
 * Composable effect that applies glass backdrop to the current window.
 * Waits briefly for the window to be fully realized before applying.
 */
@Composable
fun GlassWindowEffect(
    window: ComposeWindow,
    backdrop: GlassBackdrop = GlassBackdrop.MICA,
    darkMode: Boolean = true,
) {
    LaunchedEffect(window, backdrop, darkMode) {
        // Wait for window to be fully realized and visible
        delay(500)
        val success = applyGlassEffect(window, backdrop, darkMode)
        if (!success) {
            // Retry once after a longer delay if first attempt fails
            delay(1000)
            applyGlassEffect(window, backdrop, darkMode)
        }
    }
}
