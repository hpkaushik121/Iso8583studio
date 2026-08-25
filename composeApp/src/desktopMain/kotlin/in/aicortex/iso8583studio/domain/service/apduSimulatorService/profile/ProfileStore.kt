package `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile

import `in`.aicortex.iso8583studio.domain.store.JsonDirStore
import kotlinx.serialization.serializer
import java.nio.file.Path

/**
 * Filesystem-backed JSON store for [CardProfile]s. One profile per file, file name is the
 * profile id with a ".json" suffix. The store is intentionally dumb: no caching, no locking;
 * callers handle concurrency.
 *
 * Now a thin [JsonDirStore] subclass — the same shape was needed for hardware profiles and
 * terminal models. The public API (`list`/`load`/`save`/`delete`) is unchanged, and `rename`,
 * `find`, `exists` and `seedIfEmpty` come along for free.
 */
class ProfileStore(dir: Path) : JsonDirStore<CardProfile>(
    dir = dir,
    serializer = serializer(),
    idOf = { it.id },
    sortKey = { it.name },
)
