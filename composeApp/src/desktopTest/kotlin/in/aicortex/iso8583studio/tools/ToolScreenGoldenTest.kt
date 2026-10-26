package `in`.aicortex.iso8583studio.tools

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import `in`.aicortex.iso8583studio.ui.AppTheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.fail

/**
 * Desktop parity gate for the shared tool screens. Renders every catalogued tool (light and
 * dark, every tab) through the real desktop theme and either records the frames as goldens
 * (`-Pgolden=record`) or compares them pixel for pixel (`-Pgolden=compare`). The web chrome work
 * must leave desktop untouched, so the comparison tolerates zero differing pixels. Goldens are
 * rasterised on one machine; without the property the test does nothing, so CI is unaffected.
 */
@OptIn(ExperimentalTestApi::class)
class ToolScreenGoldenTest {

    private val mode = System.getProperty("golden.mode") ?: ""
    private val goldenDir = File(System.getProperty("golden.dir") ?: "src/desktopTest/resources/golden")
    private val reportDir = File(System.getProperty("golden.report") ?: "build/reports/golden")

    @Test
    fun toolScreensMatchGoldens() {
        if (mode != "record" && mode != "compare") return
        goldenDir.mkdirs(); reportDir.mkdirs()
        val failures = mutableListOf<String>()
        for (tool in ToolCatalog.all) for (dark in listOf(false, true)) {
            runDesktopComposeUiTest(width = 1400, height = 900) {
                setContent { AppTheme(darkTheme = dark) { tool.content {} } }
                waitForIdle()
                val base = "${tool.id}-${if (dark) "dark" else "light"}"
                check(base, onRoot().captureToImage().toAwtImage(), failures)
                // Every tab of a tabbed screen: Material Tab nodes are selectable.
                val tabs = onAllNodes(isSelectable()).fetchSemanticsNodes().size
                for (i in 1 until tabs) {
                    onAllNodes(isSelectable())[i].performClick()
                    waitForIdle()
                    check("$base-tab$i", onRoot().captureToImage().toAwtImage(), failures)
                }
            }
        }
        if (failures.isNotEmpty()) fail("Golden mismatch for ${failures.size} frame(s):\n" + failures.joinToString("\n"))
    }

    private fun check(name: String, actual: BufferedImage, failures: MutableList<String>) {
        val file = File(goldenDir, "$name.png")
        if (mode == "record") { ImageIO.write(actual, "png", file); return }
        if (!file.exists()) { failures += "$name: no golden recorded"; return }
        val expected = ImageIO.read(file)
        if (expected.width != actual.width || expected.height != actual.height) {
            failures += "$name: size ${actual.width}x${actual.height} vs ${expected.width}x${expected.height}"; return
        }
        var diff = 0
        val diffImage = BufferedImage(actual.width, actual.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until actual.height) for (x in 0 until actual.width) {
            val same = expected.getRGB(x, y) == actual.getRGB(x, y)
            if (!same) diff++
            diffImage.setRGB(x, y, if (same) (actual.getRGB(x, y) and 0x40FFFFFF.toInt()) or 0x40000000 else 0xFFFF0000.toInt())
        }
        if (diff > 0) {
            ImageIO.write(actual, "png", File(reportDir, "$name.actual.png"))
            ImageIO.write(diffImage, "png", File(reportDir, "$name.diff.png"))
            failures += "$name: $diff pixel(s) differ (see ${reportDir.path})"
        }
    }
}
