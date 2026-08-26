package `in`.aicortex.iso8583studio.config

import `in`.aicortex.iso8583studio.data.model.GatewayConfig
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.ProfileTransferResult
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.UnifiedSimulatorState
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Covers single-profile export/import (as opposed to whole-workspace export/import).
 *
 * NOTE: [UnifiedSimulatorState.save] writes `Iso8583Studio_simulators.json` into the process
 * working directory, which under Gradle is `composeApp/` — a real, version-controlled workspace
 * file. `importProfile` calls `save()`, so these tests would otherwise clobber it. Each test
 * therefore snapshots that file up front and restores it afterwards.
 */
class SingleProfileTransferTest {

    private val workspaceFile = File("Iso8583Studio_simulators.json")
    private var workspaceBackup: ByteArray? = null

    @BeforeTest
    fun snapshotWorkspace() {
        workspaceBackup = if (workspaceFile.exists()) workspaceFile.readBytes() else null
    }

    @AfterTest
    fun restoreWorkspace() {
        val backup = workspaceBackup
        if (backup != null) workspaceFile.writeBytes(backup) else workspaceFile.delete()
    }

    private fun tempFile(contents: String): File =
        File.createTempFile("profile-transfer", ".json").apply {
            writeText(contents)
            deleteOnExit()
        }

    @Test
    fun `export then import adds a copy with a fresh id and a de-duplicated name`() {
        val state = UnifiedSimulatorState()
        val before = state.hostConfigs.value.size

        val original = GatewayConfig(id = state.generateConfigId(), name = "BASE24 POS")
        state.addConfig(original)

        val exported = state.exportProfile(original)
        assertTrue(exported is ProfileTransferResult.Success, "export should succeed")
        assertEquals(SimulatorType.HOST, exported.simulatorType)

        val result = state.importProfile(tempFile(exported.content))
        assertTrue(result is ProfileTransferResult.Success, "import should succeed")

        val hosts = state.hostConfigs.value
        assertEquals(before + 2, hosts.size, "import must add, not replace")

        val copy = hosts.last()
        assertNotEquals(original.id, copy.id, "imported profile needs its own id")
        assertEquals("BASE24 POS (2)", copy.name, "name collision should be resolved")
    }

    @Test
    fun `importing the same file twice keeps incrementing the name suffix`() {
        val state = UnifiedSimulatorState()
        val original = GatewayConfig(id = state.generateConfigId(), name = "Dup Check")
        state.addConfig(original)

        val exported = state.exportProfile(original) as ProfileTransferResult.Success
        val file = tempFile(exported.content)

        state.importProfile(file)
        state.importProfile(file)

        val names = state.hostConfigs.value.map { it.name }
        assertTrue("Dup Check (2)" in names, "expected first collision rename, got $names")
        assertTrue("Dup Check (3)" in names, "expected second collision rename, got $names")
        assertEquals(names.size, names.toSet().size, "profile names must stay unique")
    }

    @Test
    fun `exported envelope round-trips field level data`() {
        val state = UnifiedSimulatorState()
        val original = GatewayConfig(
            id = state.generateConfigId(),
            name = "Field Fidelity",
            serverPort = 9187
        )
        state.addConfig(original)

        val exported = state.exportProfile(original) as ProfileTransferResult.Success
        state.importProfile(tempFile(exported.content))

        val copy = state.hostConfigs.value.last()
        assertEquals(9187, copy.serverPort, "non-default fields must survive the round trip")
    }

    @Test
    fun `a whole-workspace export file is rejected as a single profile`() {
        val state = UnifiedSimulatorState()
        // export() emits a SimulatorConfigCollection, not a SimulatorProfileExport envelope.
        val result = state.importProfile(tempFile(state.export()))
        assertTrue(result is ProfileTransferResult.Error, "should not silently accept the wrong shape")
    }

    @Test
    fun `malformed input reports an error instead of throwing`() {
        val state = UnifiedSimulatorState()
        val result = state.importProfile(tempFile("{ this is not json"))
        assertTrue(result is ProfileTransferResult.Error)
    }

    @Test
    fun `a missing file reports an error`() {
        val state = UnifiedSimulatorState()
        val missing = File.createTempFile("gone", ".json").apply { delete() }
        val result = state.importProfile(missing)
        assertTrue(result is ProfileTransferResult.Error)
    }
}
