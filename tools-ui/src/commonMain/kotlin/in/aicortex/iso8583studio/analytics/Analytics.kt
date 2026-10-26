package `in`.aicortex.iso8583studio.analytics

/**
 * Where the shared tool screens report usage. The desktop app plugs in its GA4 tracker, the
 * browser shell forwards to the website's analytics through the parent page; a screen never
 * needs to know which.
 *
 * Safety rule: parameters carry tool identity, operation labels, booleans and durations only.
 * Keys, PANs, PINs, cryptograms and other inputs or results must never reach these methods.
 */
interface AnalyticsSink {
    fun calculationRun(operation: String, success: Boolean, durationMs: Long)
    fun toolOpened(toolId: String, label: String, category: String)
    fun toolClosed(toolId: String, durationSeconds: Long)
    fun error(throwable: Throwable)
}

object NoopAnalyticsSink : AnalyticsSink {
    override fun calculationRun(operation: String, success: Boolean, durationMs: Long) = Unit
    override fun toolOpened(toolId: String, label: String, category: String) = Unit
    override fun toolClosed(toolId: String, durationSeconds: Long) = Unit
    override fun error(throwable: Throwable) = Unit
}

/**
 * Facade the screens call. Keeps the fully-qualified name the screens already import, so the
 * tool sources did not change when they moved out of the desktop module.
 */
object Analytics {
    var sink: AnalyticsSink = NoopAnalyticsSink

    /**
     * A calculator ran. [operation] is a screen-local literal such as "HMAC Generation".
     * Inputs and results must never be passed - they hold live keys and card data.
     */
    fun calculationRun(operation: String, success: Boolean, durationMs: Long) =
        sink.calculationRun(operation.take(100), success, durationMs)

    fun toolOpened(toolId: String, label: String, category: String) = sink.toolOpened(toolId, label, category)

    fun toolClosed(toolId: String, durationSeconds: Long) = sink.toolClosed(toolId, durationSeconds)

    /** Only the exception's class name is reported downstream; messages routinely contain key material. */
    fun error(throwable: Throwable) = sink.error(throwable)
}
