package `in`.aicortex.iso8583studio.ui.screens.hsmCommand

import `in`.aicortex.iso8583studio.domain.service.hsmCommandService.HsmCommandClientService
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Builds the same strings shown in the Console Request/Response (and Raw / Parsed) areas,
 * and appends them to a session log file under `~/.iso8583studio/logs/`.
 */
object HsmCommandExchangeLog {

    private val fileTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    private fun logDirectory(): File {
        val dir = File(System.getProperty("user.home"), ".iso8583studio/logs")
        dir.mkdirs()
        return dir
    }

    fun sanitizedSessionName(name: String): String {
        val s = name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80)
        return s.ifEmpty { "session" }
    }

    /** Matches the **Formatted** tab → **REQUEST** panel. */
    fun formattedRequestPanelText(
        session: CommandConsoleSessionState,
        service: HsmCommandClientService,
    ): String =
        session.bpFormattedRequest.ifBlank {
            session.lastResult
                ?.takeIf { it.rawRequest.isNotEmpty() }
                ?.let { service.formatOutboundCommandForDisplay(it.rawRequest) }
                .orEmpty()
        }

    /** Matches the **Formatted** tab → **RESPONSE** panel. */
    fun formattedResponsePanelText(session: CommandConsoleSessionState): String =
        session.bpFormattedResponse.ifBlank {
            session.lastResult?.formattedResponse.orEmpty()
        }

    /** Matches the **Raw Hex** tab → **RAW REQUEST** panel. */
    fun rawRequestHex(session: CommandConsoleSessionState): String =
        session.lastResult?.rawRequest?.let { HsmCommandClientService.bytesToHex(it) }.orEmpty()

    /** Matches the **Raw Hex** tab → **RAW RESPONSE** panel. */
    fun rawResponseHex(session: CommandConsoleSessionState): String =
        session.lastResult?.rawResponse?.let { HsmCommandClientService.bytesToHex(it) }.orEmpty()

    /** Matches the **Parsed** tab (field lines). */
    fun parsedResponseText(session: CommandConsoleSessionState): String =
        if (session.parsedResponse.isEmpty()) "(no parsed fields)"
        else session.parsedResponse.joinToString("\n") { (field, value) ->
            "${field.name} = [$value]"
        }

    fun buildExchangeLogBody(
        session: CommandConsoleSessionState,
        service: HsmCommandClientService,
    ): String = buildString {
        appendLine(" REQUEST (Formatted panel)")
        appendLine(" ----------------------------------------")
        appendLine(formattedRequestPanelText(session, service).ifBlank { "(empty)" })
        appendLine()
        appendLine(" RESPONSE (Formatted panel)")
        appendLine(" ----------------------------------------")
        appendLine(formattedResponsePanelText(session).ifBlank { "(empty)" })
        appendLine()
        appendLine(" RAW REQUEST (hex)")
        appendLine(" ----------------------------------------")
        appendLine(rawRequestHex(session).ifBlank { "(empty)" })
        appendLine()
        appendLine(" RAW RESPONSE (hex)")
        appendLine(" ----------------------------------------")
        appendLine(rawResponseHex(session).ifBlank { "(empty)" })
        appendLine()
        appendLine(" PARSED (Parsed tab)")
        appendLine(" ----------------------------------------")
        appendLine(parsedResponseText(session))
    }

    fun appendToFile(sessionName: String, body: String) {
        val f = File(logDirectory(), "hsm-commander-${sanitizedSessionName(sessionName)}.log")
        val header = "[${LocalDateTime.now().format(fileTimestamp)}] HSM Commander exchange\n"
        f.appendText(header + body + "\n\n")
    }
}
