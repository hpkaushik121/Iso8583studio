package `in`.aicortex.iso8583studio.hsm.payshield10k

import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditEntry
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.AuditLog

interface HsmLogsListener {
    fun onAuditLog(auditLog: AuditEntry)
}