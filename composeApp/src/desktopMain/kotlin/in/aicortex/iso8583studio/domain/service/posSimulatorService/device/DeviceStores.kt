package `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device

import `in`.aicortex.iso8583studio.domain.store.JsonDirStore
import kotlinx.serialization.serializer
import java.nio.file.Path

/**
 * User-owned terminal models — forks of catalog built-ins and hand-authored entries.
 *
 * Built-ins are compiled into [DeviceCatalog] and deliberately never written here, so a shipped
 * catalog fix still reaches anyone who has not forked. [TerminalCatalog] merges the two.
 */
class TerminalModelStore(dir: Path = JsonDirStore.appDir("terminal-models")) :
    JsonDirStore<TerminalModel>(
        dir = dir,
        serializer = serializer(),
        idOf = { it.id },
        sortKey = { it.label },
    )

/**
 * Standalone hardware profiles — imported from `~/.android/devices.xml`, captured by `DeviceProbe`,
 * or created by hand. Kept separate from terminal models because one hardware profile can back
 * several terminal personalities.
 */
class HardwareProfileStore(dir: Path = JsonDirStore.appDir("avd-profiles")) :
    JsonDirStore<HardwareProfile>(
        dir = dir,
        serializer = serializer(),
        idOf = { it.id },
        sortKey = { it.name },
    )

/**
 * The single lookup the UI talks to: built-in catalog entries plus the user's own, with user
 * entries shadowing built-ins of the same id.
 */
class TerminalCatalog(private val store: TerminalModelStore = TerminalModelStore()) {

    fun all(): List<TerminalModel> {
        val user = store.list()
        val userIds = user.map { it.id }.toSet()
        return (DeviceCatalog.models.filterNot { it.id in userIds } + user)
            .sortedWith(compareBy({ it.vendor.displayName }, { it.model }))
    }

    fun byId(modelId: String): TerminalModel? = all().firstOrNull { it.id == modelId }

    fun byVendor(vendor: DeviceVendor): List<TerminalModel> = all().filter { it.vendor == vendor }

    fun vendors(): List<DeviceVendor> = all().map { it.vendor }.distinct().sortedBy { it.displayName }

    /** Resolves a `"<modelId>:<variantId>"` identity across built-ins and user entries. */
    fun resolve(terminalId: String): ResolvedTerminal? =
        byId(TerminalId.modelOf(terminalId))?.resolve(TerminalId.variantOf(terminalId))

    fun search(query: String): List<TerminalModel> {
        if (query.isBlank()) return all()
        val q = query.trim().lowercase()
        return all().filter {
            it.model.lowercase().contains(q) ||
                it.vendor.displayName.lowercase().contains(q) ||
                it.label.lowercase().contains(q) ||
                it.variants.any { v -> v.label.lowercase().contains(q) || v.sku.lowercase().contains(q) }
        }
    }

    /** Forks a built-in into the user store so it becomes editable. */
    fun forkModel(modelId: String, newId: String, newDisplayName: String): TerminalModel? {
        val source = byId(modelId) ?: return null
        return source.fork(newId, newDisplayName).also(store::save)
    }

    fun save(model: TerminalModel) = store.save(model)

    fun delete(modelId: String): Boolean = store.delete(modelId)
}
