package `in`.aicortex.iso8583studio.domain.service.hsmSimulatorService

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import `in`.aicortex.iso8583studio.data.HsmClient
import `in`.aicortex.iso8583studio.logging.LogEntry
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi


data class HsmState constructor(
    var started: Boolean = false,
    var connectionCount: AtomicInteger = AtomicInteger(0),
    var activeClients: SnapshotStateList<HsmClient> = SnapshotStateList<HsmClient>(),
    var rawRequest: SnapshotStateList<LogEntry> = SnapshotStateList(),
    var formattedRequest:  SnapshotStateList<LogEntry> = mutableStateListOf(),
    var rawResponse:  SnapshotStateList<LogEntry> = mutableStateListOf(),
    var formattedResponse:  SnapshotStateList<LogEntry> = mutableStateListOf(),
    var onHold: Boolean = false,
    var timeout: String = "",
) {
}