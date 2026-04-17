package `in`.aicortex.iso8583studio.data.model

// ─────────────────────────────────────────────────────────────────────────────
// Configuration
// ─────────────────────────────────────────────────────────────────────────────

data class LoadTestConfig(
    /** Total number of requests to send. */
    var totalRequests: Int = 100,
    /** Maximum number of requests in-flight simultaneously. */
    var concurrentUsers: Int = 10,
    /** Seconds over which load is gradually ramped from 1 → concurrentUsers. */
    var rampUpSeconds: Int = 5,
    /** Fixed wait between consecutive sends per virtual user (ms). */
    var thinkTimeMs: Long = 0L,
    /** Requests to send before the timed test begins (not counted in results). */
    var warmUpRequests: Int = 0,
    /** ASYNC mode only: number of persistent connections in the pool. */
    var connectionPoolSize: Int = 5,
    /** Per-request socket timeout (ms). 0 = use gateway default. */
    var requestTimeoutMs: Int = 30_000,
    /** Abort the entire test after the first failed request. */
    var stopOnFirstError: Boolean = false,
    /** Hard wall-clock ceiling for the test (seconds). 0 = no limit. */
    var maxDurationSeconds: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// Per-request result
// ─────────────────────────────────────────────────────────────────────────────

data class RequestResult(
    val index: Int,
    val threadId: Long,
    /** Epoch-ms when the send started. */
    val startTimeMs: Long,
    /** Epoch-ms when the response was received (or the error was caught). */
    val endTimeMs: Long,
    val latencyMs: Long,
    val success: Boolean,
    val errorMessage: String? = null,
    val requestSizeBytes: Int = 0,
    val responseSizeBytes: Int = 0,
    /** Which pool slot handled this request (ASYNC mode). */
    val connectionId: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// Aggregate statistics
// ─────────────────────────────────────────────────────────────────────────────

data class LoadTestStats(
    val totalRequests: Int,
    val successCount: Int,
    val failureCount: Int,
    val totalDurationMs: Long,
    val throughputTps: Double,
    val avgLatencyMs: Double,
    val minLatencyMs: Long,
    val maxLatencyMs: Long,
    val p50LatencyMs: Long,
    val p75LatencyMs: Long,
    val p90LatencyMs: Long,
    val p95LatencyMs: Long,
    val p99LatencyMs: Long,
    val errorRate: Double
) {
    companion object {
        val EMPTY = LoadTestStats(0, 0, 0, 0L, 0.0, 0.0, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0.0)

        fun compute(results: List<RequestResult>, totalDurationMs: Long): LoadTestStats {
            if (results.isEmpty()) return EMPTY
            val latencies = results.map { it.latencyMs }.sorted()
            val n = latencies.size
            fun pct(p: Double) = latencies[(n * p).toInt().coerceIn(0, n - 1)]
            return LoadTestStats(
                totalRequests = n,
                successCount = results.count { it.success },
                failureCount = results.count { !it.success },
                totalDurationMs = totalDurationMs,
                throughputTps = if (totalDurationMs > 0) results.count { it.success } * 1000.0 / totalDurationMs else 0.0,
                avgLatencyMs = latencies.average(),
                minLatencyMs = latencies.first(),
                maxLatencyMs = latencies.last(),
                p50LatencyMs = pct(0.50),
                p75LatencyMs = pct(0.75),
                p90LatencyMs = pct(0.90),
                p95LatencyMs = pct(0.95),
                p99LatencyMs = pct(0.99),
                errorRate = results.count { !it.success } * 100.0 / n
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Live-progress snapshot (polled by the UI)
// ─────────────────────────────────────────────────────────────────────────────

enum class LoadTestStatus { IDLE, WARMING_UP, RUNNING, STOPPING, COMPLETED, FAILED }

data class LoadTestProgress(
    val status: LoadTestStatus = LoadTestStatus.IDLE,
    val sent: Int = 0,
    val success: Int = 0,
    val failure: Int = 0,
    val currentTps: Double = 0.0,
    val elapsedMs: Long = 0L,
    val totalRequests: Int = 0,
    val message: String = ""
) {
    val progressFraction: Float
        get() = if (totalRequests > 0) sent.toFloat() / totalRequests else 0f
}
