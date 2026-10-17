package `in`.aicortex.iso8583studio.tools

import `in`.aicortex.iso8583studio.data.model.StudioTool
import `in`.aicortex.iso8583studio.ui.screens.landing.ToolSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The desktop registry ([StudioTool]) and the shared web catalog ([ToolCatalog]) describe the
 * same screens. Neither derives from the other, so this test keeps them honest.
 */
class ToolCatalogSyncTest {

    /** Desktop-only tools that intentionally have no web entry. */
    private val desktopOnly = setOf(StudioTool.MESSAGE_PARSER)

    @Test
    fun `every catalog entry with a StudioTool name resolves and matches the label`() {
        ToolCatalog.all.filter { it.studioToolName != null }.forEach { tool ->
            val studioTool = StudioTool.valueOf(tool.studioToolName!!)
            assertEquals(studioTool.label, tool.label, "label of ${tool.id}")
        }
    }

    @Test
    fun `every non-simulator StudioTool is in the catalog`() {
        val simulators = ToolSuite.CORE_SIMULATORS.tools.toSet()
        val catalogued = ToolCatalog.all.mapNotNull { it.studioToolName }.toSet()
        val missing = StudioTool.entries
            .filter { it !in simulators && it !in desktopOnly }
            .filter { it.name !in catalogued }
        assertTrue(missing.isEmpty(), "StudioTools missing from ToolCatalog: $missing")
    }

    @Test
    fun `slugs are unique kebab-case`() {
        val ids = ToolCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate slugs")
        ids.forEach { assertTrue(Regex("^[a-z0-9]+(-[a-z0-9]+)*$").matches(it), "slug '$it' is not kebab-case") }
    }
}
