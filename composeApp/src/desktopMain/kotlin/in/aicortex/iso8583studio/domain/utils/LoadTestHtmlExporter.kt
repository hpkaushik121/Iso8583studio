package `in`.aicortex.iso8583studio.domain.utils

import `in`.aicortex.iso8583studio.data.model.LoadTestConfig
import `in`.aicortex.iso8583studio.data.model.LoadTestStats
import `in`.aicortex.iso8583studio.data.model.RequestResult
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object LoadTestHtmlExporter {

    private val tsFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun export(
        file: File,
        config: LoadTestConfig,
        stats: LoadTestStats,
        results: List<RequestResult>,
        isAsyncMode: Boolean,
        gatewayLabel: String = ""
    ) {
        file.writeText(buildHtml(config, stats, results, isAsyncMode, gatewayLabel))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML assembly
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildHtml(
        config: LoadTestConfig,
        stats: LoadTestStats,
        results: List<RequestResult>,
        isAsyncMode: Boolean,
        gatewayLabel: String
    ): String {
        val runAt = dateFormatter.format(LocalDateTime.now())
        val mode = if (isAsyncMode) "Asynchronous (connection pool)" else "Synchronous (new connection per request)"

        val latencyData = buildLatencyData(results)
        val distData    = buildDistributionData(results)
        val tpsData     = buildTpsData(results)
        val tableRows   = buildTableRows(results)

        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>ISO 8583 Load Test Report</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:'Segoe UI',system-ui,sans-serif;background:#0f1117;color:#e2e8f0;min-height:100vh}
.header{background:linear-gradient(135deg,#1e40af 0%,#7c3aed 100%);padding:32px 40px 28px}
.header h1{font-size:1.8rem;font-weight:700;letter-spacing:-.5px}
.header p{margin-top:6px;font-size:.9rem;opacity:.8}
.badge{display:inline-block;padding:3px 10px;border-radius:20px;font-size:.75rem;font-weight:600;margin-top:10px}
.badge-sync{background:#1e3a8a;color:#93c5fd}
.badge-async{background:#3b1a78;color:#c4b5fd}
.container{max-width:1400px;margin:0 auto;padding:28px 32px}
.section{margin-bottom:32px}
.section-title{font-size:1rem;font-weight:700;letter-spacing:.05em;text-transform:uppercase;color:#94a3b8;margin-bottom:14px;padding-bottom:8px;border-bottom:1px solid #1e293b}
.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:14px}
.card{background:#1e293b;border-radius:10px;padding:18px 16px;text-align:center;border:1px solid #334155}
.card .label{font-size:.72rem;text-transform:uppercase;letter-spacing:.07em;color:#64748b;margin-bottom:4px}
.card .value{font-size:1.5rem;font-weight:700;color:#f1f5f9}
.card .value.green{color:#4ade80}
.card .value.red{color:#f87171}
.card .value.blue{color:#60a5fa}
.card .value.yellow{color:#fbbf24}
.card .value.purple{color:#c084fc}
.card .sub{font-size:.7rem;color:#475569;margin-top:3px}
.chart-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px}
.chart-box{background:#1e293b;border-radius:10px;padding:20px;border:1px solid #334155}
.chart-box h3{font-size:.85rem;font-weight:600;color:#94a3b8;margin-bottom:16px;text-transform:uppercase;letter-spacing:.05em}
.chart-wrapper{position:relative;height:220px}
.config-table{width:100%;border-collapse:collapse}
.config-table td{padding:8px 14px;font-size:.85rem;border-bottom:1px solid #1e293b}
.config-table td:first-child{color:#64748b;width:220px}
.config-table td:last-child{color:#e2e8f0;font-weight:500}
.results-table{width:100%;border-collapse:collapse;font-size:.8rem}
.results-table thead tr{background:#1e293b}
.results-table th{padding:10px 12px;text-align:left;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:.05em;border-bottom:2px solid #334155}
.results-table tbody tr:hover{background:#1e293b55}
.results-table td{padding:8px 12px;border-bottom:1px solid #1e293b20;font-family:'Consolas','Courier New',monospace}
.success{color:#4ade80}
.failure{color:#f87171}
.latency-bar{display:inline-block;height:10px;border-radius:2px;vertical-align:middle;margin-right:6px}
.pct-grid{display:grid;grid-template-columns:repeat(5,1fr);gap:10px}
.pct-card{background:#0f172a;border-radius:8px;padding:12px;text-align:center;border:1px solid #1e293b}
.pct-card .pct-label{font-size:.7rem;color:#475569;margin-bottom:4px;text-transform:uppercase;letter-spacing:.05em}
.pct-card .pct-value{font-size:1.1rem;font-weight:700;color:#e2e8f0}
.footer{text-align:center;padding:20px;color:#334155;font-size:.78rem}
@media(max-width:900px){.chart-grid{grid-template-columns:1fr}.pct-grid{grid-template-columns:repeat(3,1fr)}}
</style>
</head>
<body>
<div class="header">
  <h1>ISO 8583 Studio — Load Test Report</h1>
  <p>Generated: $runAt${if (gatewayLabel.isNotBlank()) " &nbsp;·&nbsp; Gateway: $gatewayLabel" else ""}</p>
  <span class="badge ${if (isAsyncMode) "badge-async" else "badge-sync"}">$mode</span>
</div>

<div class="container">

<!-- ── Summary KPI cards ───────────────────────────────────────── -->
<div class="section">
  <div class="section-title">Test Summary</div>
  <div class="cards">
    <div class="card"><div class="label">Total Requests</div><div class="value blue">${stats.totalRequests}</div><div class="sub">sent</div></div>
    <div class="card"><div class="label">Successful</div><div class="value green">${stats.successCount}</div><div class="sub">${"%.1f".format(100.0 - stats.errorRate)}%</div></div>
    <div class="card"><div class="label">Failed</div><div class="value red">${stats.failureCount}</div><div class="sub">${"%.1f".format(stats.errorRate)}% error rate</div></div>
    <div class="card"><div class="label">Throughput</div><div class="value yellow">${"%.2f".format(stats.throughputTps)}</div><div class="sub">req / sec</div></div>
    <div class="card"><div class="label">Total Duration</div><div class="value">${stats.totalDurationMs}</div><div class="sub">ms</div></div>
    <div class="card"><div class="label">Avg Latency</div><div class="value purple">${"%.1f".format(stats.avgLatencyMs)}</div><div class="sub">ms</div></div>
    <div class="card"><div class="label">Min Latency</div><div class="value green">${stats.minLatencyMs}</div><div class="sub">ms</div></div>
    <div class="card"><div class="label">Max Latency</div><div class="value red">${stats.maxLatencyMs}</div><div class="sub">ms</div></div>
  </div>
</div>

<!-- ── Percentile cards ───────────────────────────────────────── -->
<div class="section">
  <div class="section-title">Latency Percentiles</div>
  <div class="pct-grid">
    <div class="pct-card"><div class="pct-label">P50 (Median)</div><div class="pct-value">${stats.p50LatencyMs} ms</div></div>
    <div class="pct-card"><div class="pct-label">P75</div><div class="pct-value">${stats.p75LatencyMs} ms</div></div>
    <div class="pct-card"><div class="pct-label">P90</div><div class="pct-value">${stats.p90LatencyMs} ms</div></div>
    <div class="pct-card"><div class="pct-label">P95</div><div class="pct-value">${stats.p95LatencyMs} ms</div></div>
    <div class="pct-card"><div class="pct-label">P99</div><div class="pct-value">${stats.p99LatencyMs} ms</div></div>
  </div>
</div>

<!-- ── Charts ─────────────────────────────────────────────────── -->
<div class="section">
  <div class="section-title">Performance Charts</div>
  <div class="chart-grid">
    <div class="chart-box">
      <h3>Latency Over Time (ms)</h3>
      <div class="chart-wrapper"><canvas id="latencyChart"></canvas></div>
    </div>
    <div class="chart-box">
      <h3>Latency Distribution</h3>
      <div class="chart-wrapper"><canvas id="distChart"></canvas></div>
    </div>
    <div class="chart-box">
      <h3>Throughput Over Time (TPS — 1 s buckets)</h3>
      <div class="chart-wrapper"><canvas id="tpsChart"></canvas></div>
    </div>
    <div class="chart-box">
      <h3>Success vs Failure</h3>
      <div class="chart-wrapper"><canvas id="pieChart"></canvas></div>
    </div>
  </div>
</div>

<!-- ── Test Configuration ─────────────────────────────────────── -->
<div class="section">
  <div class="section-title">Test Configuration</div>
  <table class="config-table">
    <tr><td>Mode</td><td>$mode</td></tr>
    <tr><td>Total Requests</td><td>${config.totalRequests}</td></tr>
    <tr><td>Concurrent Users</td><td>${config.concurrentUsers}</td></tr>
    <tr><td>Ramp-Up</td><td>${config.rampUpSeconds} s</td></tr>
    <tr><td>Think Time</td><td>${config.thinkTimeMs} ms</td></tr>
    <tr><td>Warm-Up Requests</td><td>${config.warmUpRequests}</td></tr>
    ${if (isAsyncMode) "<tr><td>Connection Pool Size</td><td>${config.connectionPoolSize}</td></tr>" else ""}
    <tr><td>Request Timeout</td><td>${config.requestTimeoutMs} ms</td></tr>
    <tr><td>Max Duration</td><td>${if (config.maxDurationSeconds > 0) "${config.maxDurationSeconds} s" else "Unlimited"}</td></tr>
    <tr><td>Stop on First Error</td><td>${config.stopOnFirstError}</td></tr>
  </table>
</div>

<!-- ── Request Details ────────────────────────────────────────── -->
<div class="section">
  <div class="section-title">Request Details${if (results.size > 2000) " (first 2 000 of ${results.size})" else " (${results.size} requests)"}</div>
  <table class="results-table">
    <thead><tr>
      <th>#</th>
      <th>Timestamp</th>
      <th>Thread</th>
      ${if (isAsyncMode) "<th>Conn ID</th>" else ""}
      <th>Latency (ms)</th>
      <th>Req (bytes)</th>
      <th>Status</th>
      <th>Error</th>
    </tr></thead>
    <tbody>
$tableRows
    </tbody>
  </table>
</div>

</div><!-- /container -->

<div class="footer">ISO 8583 Studio — Load Test Report &nbsp;·&nbsp; $runAt</div>

<script>
const CHART_OPTS = {
  responsive:true, maintainAspectRatio:false,
  plugins:{legend:{labels:{color:'#94a3b8',font:{size:11}}}},
  scales:{
    x:{ticks:{color:'#475569',font:{size:10}},grid:{color:'#1e293b'}},
    y:{ticks:{color:'#475569',font:{size:10}},grid:{color:'#1e293b'}}
  }
};

// 1. Latency over time
(function(){
  const data = $latencyData;
  const ctx = document.getElementById('latencyChart').getContext('2d');
  new Chart(ctx,{
    type:'line',
    data:{
      labels: data.map(d=>d.x),
      datasets:[{
        label:'Latency (ms)',
        data: data.map(d=>d.y),
        borderColor:'#60a5fa', backgroundColor:'rgba(96,165,250,.08)',
        pointRadius:data.length>200?0:2, borderWidth:1.5, tension:.3
      }]
    },
    options:{...CHART_OPTS, plugins:{...CHART_OPTS.plugins, tooltip:{callbacks:{title:i=>'Request '+data[i[0].dataIndex].idx}}}}
  });
})();

// 2. Distribution histogram
(function(){
  const data = $distData;
  const ctx = document.getElementById('distChart').getContext('2d');
  new Chart(ctx,{
    type:'bar',
    data:{
      labels: data.map(d=>d.label),
      datasets:[{
        label:'Count',
        data: data.map(d=>d.count),
        backgroundColor: data.map((_,i)=>{
          const t=i/data.length;
          const r=Math.round(96+(1-t)*(248-96));
          const g=Math.round(165+t*(113-165));
          const b=Math.round(250+t*(74-250));
          return 'rgba('+r+','+g+','+b+',.85)';
        }),
        borderRadius:4
      }]
    },
    options:{...CHART_OPTS, scales:{...CHART_OPTS.scales, x:{...CHART_OPTS.scales.x, ticks:{...CHART_OPTS.scales.x.ticks, maxRotation:45}}}}
  });
})();

// 3. TPS over time
(function(){
  const data = $tpsData;
  const ctx = document.getElementById('tpsChart').getContext('2d');
  new Chart(ctx,{
    type:'bar',
    data:{
      labels: data.map(d=>d.sec+'s'),
      datasets:[{
        label:'TPS',
        data: data.map(d=>d.count),
        backgroundColor:'rgba(196,168,252,.7)', borderRadius:3
      }]
    },
    options: CHART_OPTS
  });
})();

// 4. Pie — success vs failure
(function(){
  const ctx = document.getElementById('pieChart').getContext('2d');
  new Chart(ctx,{
    type:'doughnut',
    data:{
      labels:['Success','Failure'],
      datasets:[{data:[${stats.successCount},${stats.failureCount}],backgroundColor:['#22c55e','#ef4444'],hoverOffset:4}]
    },
    options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{labels:{color:'#94a3b8',font:{size:12}}}}}
  });
})();
</script>
</body></html>
""".trimIndent()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chart data builders
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildLatencyData(results: List<RequestResult>): String {
        val sorted = results.sortedBy { it.index }
        // Down-sample for very large result sets (keep up to 2000 points)
        val step = (sorted.size / 2000).coerceAtLeast(1)
        val sb = StringBuilder("[")
        sorted.filterIndexed { i, _ -> i % step == 0 }.forEach { r ->
            sb.append("""{"idx":${r.index},"x":${r.index},"y":${r.latencyMs}},""")
        }
        if (sb.endsWith(",")) sb.deleteCharAt(sb.length - 1)
        sb.append("]")
        return sb.toString()
    }

    private fun buildDistributionData(results: List<RequestResult>): String {
        if (results.isEmpty()) return "[]"
        val latencies = results.map { it.latencyMs }
        val min = latencies.min()
        val max = latencies.max()
        val range = (max - min).coerceAtLeast(1)
        val buckets = 20
        val bucketSize = (range.toDouble() / buckets).coerceAtLeast(1.0)
        val counts = IntArray(buckets)
        latencies.forEach { l ->
            val b = ((l - min) / bucketSize).toInt().coerceIn(0, buckets - 1)
            counts[b]++
        }
        val sb = StringBuilder("[")
        counts.forEachIndexed { i, c ->
            val lo = (min + i * bucketSize).toLong()
            val hi = (min + (i + 1) * bucketSize).toLong()
            sb.append("""{"label":"${lo}-${hi}ms","count":$c},""")
        }
        if (sb.endsWith(",")) sb.deleteCharAt(sb.length - 1)
        sb.append("]")
        return sb.toString()
    }

    private fun buildTpsData(results: List<RequestResult>): String {
        if (results.isEmpty()) return "[]"
        val testStart = results.minOf { it.startTimeMs }
        val buckets = mutableMapOf<Long, Int>()
        results.forEach { r ->
            val sec = (r.startTimeMs - testStart) / 1000L
            buckets[sec] = (buckets[sec] ?: 0) + 1
        }
        val sb = StringBuilder("[")
        buckets.entries.sortedBy { it.key }.forEach { (sec, count) ->
            sb.append("""{"sec":$sec,"count":$count},""")
        }
        if (sb.endsWith(",")) sb.deleteCharAt(sb.length - 1)
        sb.append("]")
        return sb.toString()
    }

    private fun buildTableRows(results: List<RequestResult>): String {
        val sorted = results.sortedBy { it.index }
        val capped = if (sorted.size > 2000) sorted.take(2000) else sorted
        val testStart = if (sorted.isNotEmpty()) sorted.first().startTimeMs else 0L
        val maxLatency = sorted.maxOfOrNull { it.latencyMs } ?: 1L
        val sb = StringBuilder()
        capped.forEach { r ->
            val ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(r.startTimeMs), ZoneId.systemDefault())
            val tsStr = tsFormatter.format(ts)
            val offsetMs = r.startTimeMs - testStart
            val statusTd = if (r.success)
                """<td class="success">✓ OK</td>"""
            else
                """<td class="failure">✗ FAIL</td>"""
            val barWidth = (r.latencyMs * 80 / maxLatency).coerceIn(2, 80)
            val barColor = when {
                r.latencyMs < maxLatency * 0.5 -> "#4ade80"
                r.latencyMs < maxLatency * 0.8 -> "#fbbf24"
                else -> "#f87171"
            }
            val errorTd = """<td style="color:#f87171;font-size:.75rem">${r.errorMessage ?: ""}</td>"""
            sb.appendLine("""      <tr>
        <td>${r.index + 1}</td>
        <td>$tsStr<span style="color:#475569;font-size:.7rem"> +${offsetMs}ms</span></td>
        <td>${r.threadId}</td>
        <td><span class="latency-bar" style="width:${barWidth}px;background:$barColor"></span>${r.latencyMs} ms</td>
        <td>${r.requestSizeBytes}</td>
        $statusTd
        $errorTd
      </tr>""")
        }
        return sb.toString()
    }
}
