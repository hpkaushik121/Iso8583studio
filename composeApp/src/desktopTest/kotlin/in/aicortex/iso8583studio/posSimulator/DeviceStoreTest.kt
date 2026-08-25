package `in`.aicortex.iso8583studio.posSimulator

import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceVendor
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.HardwareProfile
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.HardwareProfileStore
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ProfileOrigin
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.Props
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.TerminalCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.TerminalModelStore
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ramMb
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.screenHeight
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceStoreTest {

    private fun tempTerminalStore() = TerminalModelStore(Files.createTempDirectory("terminal-models"))
    private fun tempHardwareStore() = HardwareProfileStore(Files.createTempDirectory("avd-profiles"))

    @Test
    fun `terminal models round trip through json with variants intact`() {
        val store = tempTerminalStore()
        val source = DeviceCatalog["pax-a910s"]!!
        store.save(source)

        val loaded = store.load(source.id)
        assertEquals(source.id, loaded.id)
        assertEquals(source.variants.size, loaded.variants.size)
        assertEquals(source.variants.map { it.id }, loaded.variants.map { it.id })
        // Deltas survive serialization — this is what the whole variant design rests on.
        assertEquals("1440", loaded.variant("5p5in")!!.propertyDeltas[Props.LCD_HEIGHT])
        assertEquals(1440, loaded.resolve("5p5in").hardware.screenHeight)
    }

    @Test
    fun `hardware profiles round trip and sort by name`() {
        val store = tempHardwareStore()
        store.save(HardwareProfile(id = "b", name = "Beta", properties = mapOf(Props.RAM to "2048")))
        store.save(HardwareProfile(id = "a", name = "Alpha", properties = mapOf(Props.RAM to "1024")))

        assertEquals(listOf("Alpha", "Beta"), store.list().map { it.name })
        assertEquals(1024, store.load("a").ramMb)
    }

    @Test
    fun `find and exists tolerate a missing id where load throws`() {
        val store = tempHardwareStore()
        assertNull(store.find("nope"))
        assertFalse(store.exists("nope"))
        store.save(HardwareProfile(id = "yes", name = "Yes"))
        assertNotNull(store.find("yes"))
        assertTrue(store.exists("yes"))
    }

    @Test
    fun `rename moves the file instead of orphaning the old one`() {
        // The original ProfileStore had no rename, so editing an id in the profile editor left the
        // previous file behind as a duplicate. That is the bug this method exists to prevent.
        val store = tempHardwareStore()
        store.save(HardwareProfile(id = "old", name = "Old"))
        store.rename("old", HardwareProfile(id = "new", name = "New"))

        assertFalse(store.exists("old"))
        assertTrue(store.exists("new"))
        assertEquals(1, store.list().size)
    }

    @Test
    fun `rename to the same id is not a delete`() {
        val store = tempHardwareStore()
        store.save(HardwareProfile(id = "same", name = "Before"))
        store.rename("same", HardwareProfile(id = "same", name = "After"))
        assertTrue(store.exists("same"))
        assertEquals("After", store.load("same").name)
    }

    @Test
    fun `unparseable files are skipped rather than breaking the whole list`() {
        val dir = Files.createTempDirectory("broken")
        val store = HardwareProfileStore(dir)
        store.save(HardwareProfile(id = "good", name = "Good"))
        dir.resolve("garbage.json").toFile().writeText("{ this is not json")

        assertEquals(listOf("Good"), store.list().map { it.name })
    }

    @Test
    fun `seedIfEmpty only seeds an empty directory`() {
        val store = tempHardwareStore()
        store.seedIfEmpty { listOf(HardwareProfile(id = "seed", name = "Seeded")) }
        assertEquals(1, store.list().size)

        store.seedIfEmpty { listOf(HardwareProfile(id = "second", name = "Second")) }
        assertEquals(1, store.list().size, "a non-empty store must not be reseeded")
    }

    // ---------- TerminalCatalog: built-ins merged with user entries ----------

    @Test
    fun `catalog exposes builtins when the user store is empty`() {
        val catalog = TerminalCatalog(tempTerminalStore())
        assertEquals(DeviceCatalog.models.size, catalog.all().size)
        assertNotNull(catalog.byId("pax-a910s"))
        assertTrue(catalog.byVendor(DeviceVendor.PAX).isNotEmpty())
    }

    @Test
    fun `a user entry shadows a builtin of the same id without duplicating it`() {
        val store = tempTerminalStore()
        val catalog = TerminalCatalog(store)
        val edited = DeviceCatalog["pax-a910s"]!!.copy(displayName = "My A910S")
        store.save(edited)

        assertEquals(DeviceCatalog.models.size, catalog.all().size, "shadowing must not add a row")
        assertEquals("My A910S", catalog.byId("pax-a910s")!!.displayName)
        assertEquals(1, catalog.all().count { it.id == "pax-a910s" })
    }

    @Test
    fun `forking a builtin writes a user copy and leaves the original reachable`() {
        val store = tempTerminalStore()
        val catalog = TerminalCatalog(store)

        val forked = catalog.forkModel("pax-a910s", "my-a910s", "My A910S")!!
        assertEquals(ProfileOrigin.USER, forked.origin)
        assertEquals("pax-a910s", forked.derivedFrom)
        assertTrue(store.exists("my-a910s"))

        // Both are now selectable, and the built-in is untouched.
        assertNotNull(catalog.byId("pax-a910s"))
        assertNotNull(catalog.byId("my-a910s"))
        assertEquals(ProfileOrigin.BUILTIN, catalog.byId("pax-a910s")!!.origin)
        assertEquals(DeviceCatalog.models.size + 1, catalog.all().size)

        assertNull(catalog.forkModel("no-such-model", "x", "X"))
    }

    @Test
    fun `catalog resolve and search span builtins and user entries`() {
        val store = tempTerminalStore()
        val catalog = TerminalCatalog(store)
        catalog.forkModel("pax-a910s", "my-a910s", "Bench A910S")

        assertEquals("5p5in", catalog.resolve("my-a910s:5p5in")!!.variantId)
        assertTrue(catalog.search("Bench").any { it.id == "my-a910s" })
        assertTrue(catalog.search("DX8005-USBLU01A").any { it.id == "ingenico-dx8000" })
        assertTrue(catalog.vendors().contains(DeviceVendor.PAX))
    }

    @Test
    fun `deleting a user entry restores the builtin underneath`() {
        val store = tempTerminalStore()
        val catalog = TerminalCatalog(store)
        store.save(DeviceCatalog["pax-a910s"]!!.copy(displayName = "Shadowed"))
        assertEquals("Shadowed", catalog.byId("pax-a910s")!!.displayName)

        catalog.delete("pax-a910s")
        assertEquals("PAX A910S", catalog.byId("pax-a910s")!!.displayName)
    }
}
