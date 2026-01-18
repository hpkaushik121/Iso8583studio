package `in`.aicortex.iso8583studio.hsm.payshield10k.data

import `in`.aicortex.iso8583studio.hsm.payshield10k.HsmLogsListener

/**
 * Audit log manager
 */
class AuditLog(private val hsmLogsListener:HsmLogsListener) {
    private val entries = mutableListOf<AuditEntry>()
    private var auditCounter = 0L

    fun addEntry(entry: AuditEntry) {
        entries.add(entry)
        auditCounter++
        hsmLogsListener.onAuditLog(entry)
    }

    fun getEntries(filter: AuditType? = null): List<AuditEntry> {
        return if (filter != null) {
            entries.filter { it.entryType == filter }
        } else {
            entries.toList()
        }
    }

    fun clear() {
        entries.clear()
    }

    fun getCounter() = auditCounter
}