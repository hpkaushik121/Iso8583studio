package `in`.aicortex.iso8583studio.analytics

import cafe.adriel.voyager.core.screen.Screen
import io.github.frankois944.googleAnalyticsKMPTracker.Tracker
import `in`.aicortex.iso8583studio.data.model.AnalyticsConsent
import `in`.aicortex.iso8583studio.data.model.AppSettings
import `in`.aicortex.iso8583studio.data.model.StudioTool
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.SimulatorType
import `in`.aicortex.iso8583studio.ui.screens.landing.ToolSuite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Where a screen view originated. The app hosts three nested Voyager navigators (the root
 * window, each popped-out window, and each tool tab), so the same screen legitimately reports
 * more than once; this parameter keeps those distinguishable rather than deduplicated.
 */
enum class NavSource(val value: String) {
    ROOT("root"),
    POPOUT("popout"),
    TOOL_TAB("tool_tab"),
}

/**
 * Facade over the GA4 tracker. Nothing else in the app talks to [Tracker] directly - this is
 * the single place where analytics is enabled, named and constrained.
 *
 * Safety rule enforced here: event parameters carry tool and screen identity, type enums,
 * durations and booleans only. Card data, PANs, PINs, keys, cryptograms, message contents,
 * file paths, hostnames and user-supplied profile names must never reach these methods.
 */
object Analytics {

    // ---- Configuration -----------------------------------------------------------------
    // The desktop app reports to its own GA4 data stream, separate from the website's
    // (the site uses G-445XQ0W2Q4).
    //
    // An API secret is scoped to a single data stream. This secret was verified NOT to
    // authenticate against G-445XQ0W2Q4 - hits were accepted with HTTP 204 and then
    // silently discarded, which is how the Measurement Protocol reports every auth
    // failure. Fill in the desktop stream's own Measurement ID below:
    // Admin > Data streams > (desktop stream) > Measurement ID, top right.
    //
    // Until then MEASUREMENT_ID stays a placeholder on purpose, so isConfigured() is false
    // and the app sends nothing - preferable to shipping a mismatched pair that fails
    // invisibly.
    private const val MEASUREMENT_ID = "G-XXXXXXXXXX"
    private const val API_SECRET = "CZH19WjNQ0mOv7hCnezdPQ"

    /** Point at [Ga4Dispatcher.DEBUG_ENDPOINT] and flip this to validate payloads locally. */
    private const val DEBUG_MODE = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var tracker: Tracker? = null
    private var dispatcher: Ga4Dispatcher? = null
    private var environment: EnvironmentProfile? = null
    private var advertisingIdProvider: AdvertisingIdProvider = DesktopAdvertisingIdProvider

    /** Last screen reported, so calculation events can attribute themselves to the active tool. */
    @Volatile
    private var currentScreenName: String = "Unknown"

    private val isConfigured: Boolean
        get() = !MEASUREMENT_ID.contains("XXXX") && API_SECRET != "REPLACE_ME"

    private val enabled: Boolean
        get() = tracker != null && AppSettings.analyticsConsent.isGranted

    /** Tools keyed by their Voyager screen, so both entry paths name a tool identically. */
    private val toolsByScreen: Map<Screen, StudioTool> by lazy {
        StudioTool.entries.associateBy { it.screen }
    }

    /** Reverse index of [ToolSuite.tools], giving each tool a reportable category. */
    private val suiteByTool: Map<StudioTool, ToolSuite> by lazy {
        ToolSuite.entries.flatMap { suite -> suite.tools.map { it to suite } }.toMap()
    }

    // ---- Lifecycle ---------------------------------------------------------------------

    /**
     * Starts (or stops) reporting to match [consent].
     *
     * Consent and identity are read from [AppSettings] rather than the tracker's own store:
     * the library keeps its state in the system temp directory, which the OS may purge - and
     * a purge must never silently turn tracking back on for someone who declined.
     */
    fun init(consent: AnalyticsConsent = AppSettings.analyticsConsent) {
        if (!isConfigured) return
        if (!consent.isGranted) {
            setEnabled(false)
            return
        }
        scope.launch {
            runCatching {
                if (tracker == null) {
                    val ga4 = Ga4Dispatcher(
                        baseURL = if (DEBUG_MODE) Ga4Dispatcher.DEBUG_ENDPOINT else Ga4Dispatcher.ENDPOINT,
                        apiSecret = API_SECRET,
                        measurementId = MEASUREMENT_ID,
                        debugMode = DEBUG_MODE,
                        environment = { environment },
                    )
                    dispatcher = ga4
                    tracker = Tracker.create(
                        apiSecret = API_SECRET,
                        measurementId = MEASUREMENT_ID,
                        isOptedOut = false,
                        context = null,
                        customDispatcher = ga4,
                    )
                }

                val active = tracker ?: return@runCatching
                active.setOptOut(false)
                active.setUserId(resolveIdentity())
                // Consent signals for any ad-identity use; mirrored into the payload's
                // `consent` block by Ga4Dispatcher.
                active.enableAdUserData(true)
                active.enableAdPersonalization(true)

                // Network-touching work happens only after consent is confirmed granted.
                val resolved = EnvironmentProfile.resolve()
                environment = resolved
                resolved.asUserProperties().forEach { (key, value) ->
                    active.setUserProperty(key.take(24), value.take(36))
                }
                active.setUserProperty("id_source", idSource)
            }
        }
    }

    /** Reflects the Settings toggle. Disabling stops sends immediately. */
    fun setEnabled(value: Boolean) {
        scope.launch {
            runCatching {
                tracker?.setOptOut(!value)
            }
        }
    }

    /** Discards the association with previously reported data and starts a fresh identity. */
    fun resetIdentity() {
        scope.launch {
            runCatching {
                val id = AppSettings.regenerateAnalyticsClientId()
                tracker?.reset()
                tracker?.setOptOut(!AppSettings.analyticsConsent.isGranted)
                tracker?.setUserId(id)
            }
        }
    }

    private var idSource: String = "uuid"

    private suspend fun resolveIdentity(): String {
        val advertisingId = runCatching { advertisingIdProvider.advertisingId() }.getOrNull()
        if (!advertisingId.isNullOrBlank()) {
            idSource = "gaid"
            return advertisingId
        }
        idSource = "uuid"
        return AppSettings.analyticsClientId.ifBlank { AppSettings.regenerateAnalyticsClientId() }
    }

    // ---- Events ------------------------------------------------------------------------

    fun appOpen() = event("app_open", emptyMap())

    fun screenView(screen: Screen, source: NavSource) {
        val tool = toolsByScreen[screen]
        screenView(
            name = tool?.label ?: screenNameOf(screen),
            screenClass = tool?.let(::categoryOf) ?: "Navigation",
            source = source,
        )
    }

    fun screenView(name: String, screenClass: String, source: NavSource) {
        currentScreenName = name
        event(
            "screen_view",
            mapOf(
                "screen_name" to name,
                "screen_class" to screenClass,
                "nav_source" to source.value,
            ),
        )
    }

    fun toolOpened(tool: StudioTool) {
        currentScreenName = tool.label
        event(
            "tool_open",
            mapOf(
                "tool_name" to tool.label,
                "tool_key" to tool.name,
                "tool_category" to categoryOf(tool),
            ),
        )
        screenView(tool.label, categoryOf(tool), NavSource.TOOL_TAB)
    }

    fun toolClosed(tool: StudioTool, durationSeconds: Long) = event(
        "tool_close",
        mapOf(
            "tool_name" to tool.label,
            "tool_key" to tool.name,
            "duration_seconds" to durationSeconds,
        ),
    )

    fun simulatorStarted(type: SimulatorType) {
        currentScreenName = type.name
        // Only the type enum - never the profile name, host or port, which are user data.
        event("simulator_start", mapOf("simulator_type" to type.name))
        screenView(type.name, "Simulator", NavSource.ROOT)
    }

    fun simulatorStopped(type: SimulatorType, durationSeconds: Long) = event(
        "simulator_stop",
        mapOf("simulator_type" to type.name, "duration_seconds" to durationSeconds),
    )

    fun windowPoppedOut(type: SimulatorType) =
        event("window_popout", mapOf("simulator_type" to type.name))

    fun windowDocked(type: SimulatorType) =
        event("window_dock", mapOf("simulator_type" to type.name))

    /**
     * A calculator ran. [operation] is a screen-local literal such as "HMAC Generation".
     * Inputs and results must never be passed - they hold live keys and card data.
     */
    fun calculationRun(operation: String, success: Boolean, durationMs: Long) = event(
        "calculation_run",
        mapOf(
            "operation" to operation.take(100),
            "tool_name" to currentScreenName,
            "success" to success,
            "duration_ms" to durationMs,
        ),
    )

    fun configExported(simulatorType: String, success: Boolean) = event(
        "config_export",
        mapOf("simulator_type" to simulatorType, "success" to success),
    )

    fun configImported(simulatorType: String, success: Boolean) = event(
        "config_import",
        mapOf("simulator_type" to simulatorType, "success" to success),
    )

    /**
     * An unexpected error occurred. Only the exception's class name is reported - messages and
     * stack traces routinely contain key material and message buffers.
     */
    fun error(throwable: Throwable) = event(
        "app_error",
        mapOf("error_type" to (throwable::class.simpleName ?: "Unknown")),
    )

    private fun event(name: String, params: Map<String, Any>) {
        if (!enabled) return
        runCatching { tracker?.trackEvent(name, params) }
    }

    // ---- Naming ------------------------------------------------------------------------

    /**
     * Voyager keys `object` screens by fully-qualified class name, e.g.
     * `in.aicortex...Destination$AesCalculator`. Taking the trailing segment yields a stable,
     * readable screen name for all 82 destinations without editing a single screen file.
     */
    internal fun screenNameOf(screen: Screen): String =
        screen.key.substringAfterLast('$').substringAfterLast('.').ifBlank { "Unknown" }

    private fun categoryOf(tool: StudioTool): String =
        suiteByTool[tool]?.name ?: "Tool"
}
