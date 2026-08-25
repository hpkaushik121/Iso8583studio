package `in`.aicortex.iso8583studio.domain.service.posSimulatorService

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AdbClient
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AndroidSdk
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AndroidSdkLocator
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdManagerService
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdPhase
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.AvdStep
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.DeviceBootstrapper
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.EmulatorLauncher
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.ProcessRegistry
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.avd.SdkResolution
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.AvdProperties
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.DeviceCatalog
import `in`.aicortex.iso8583studio.domain.service.posSimulatorService.device.ResolvedTerminal
import `in`.aicortex.iso8583studio.ui.navigation.stateConfigs.pos.POSSimulatorConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Owns one emulated terminal: its AVD, its emulator process, and the state a UI renders.
 *
 * State lives here rather than in composables so it survives tab switches — the runtime screen only
 * holds primitives. Long work runs on a service-owned scope that [stop] cancels, mirroring
 * `HsmServiceImpl`.
 */
class POSSimulatorService(val config: POSSimulatorConfig) {

    private val _state = MutableStateFlow(PosDeviceState())
    val state: StateFlow<PosDeviceState> = _state.asStateFlow()

    /** Every step of the current prepare/boot, for the phase stepper. */
    val steps: SnapshotStateList<AvdStep> = mutableStateListOf()

    private var scope: CoroutineScope? = null
    private var job: Job? = null

    val resolved: ResolvedTerminal? get() = DeviceCatalog.resolve(config.terminalProfileId)

    val sdk: AndroidSdk? get() = (AndroidSdkLocator.locate() as? SdkResolution.Found)?.sdk

    /** The AVD name this config will use, defaulted from the terminal when unset. */
    val avdName: String
        get() = config.avd.avdName.ifBlank {
            resolved?.terminal?.let { AvdProperties.suggestAvdName(it) }.orEmpty()
        }

    val isPrepared: Boolean
        get() = sdk?.let { AvdManagerService(it).exists(avdName) } ?: false

    /** True once prerequisites are satisfiable — the gate on the Power on button. */
    val canPowerOn: Boolean
        get() = sdk?.emulator != null && sdk?.adb != null && resolved != null && !state.value.busy

    fun prepare() = launchExclusive { sdkNow, terminal ->
        _state.value = PosDeviceState(phase = PosPhase.PREPARING)
        val spec = config.avd.copy(
            avdName = avdName,
            systemImage = config.avd.systemImage.takeIf { it.apiLevel > 0 }
                ?: terminal.terminal.recommendedImage,
        )
        AvdManagerService(sdkNow).prepare(spec, terminal.hardware).collect(::record)
        _state.value = _state.value.copy(
            phase = if (lastAbort() == null) PosPhase.PREPARED else PosPhase.ERROR,
        )
    }

    fun powerOn() = launchExclusive { sdkNow, terminal ->
        _state.value = PosDeviceState(phase = PosPhase.PREPARING)
        val spec = config.avd.copy(
            avdName = avdName,
            systemImage = config.avd.systemImage.takeIf { it.apiLevel > 0 }
                ?: terminal.terminal.recommendedImage,
        )

        // Auto-prepare, per the chosen launch flow. Validation runs first, so a misconfiguration
        // surfaces as a remediation card instead of spawning a 2 GB VM behind a spinner.
        val manager = AvdManagerService(sdkNow)
        manager.prepare(spec, terminal.hardware).collect(::record)
        if (lastAbort() != null) {
            _state.value = _state.value.copy(phase = PosPhase.ERROR)
            return@launchExclusive
        }

        _state.value = _state.value.copy(phase = PosPhase.BOOTING)
        DeviceBootstrapper(sdkNow, manager).boot(spec, terminal).collect(::record)

        val abort = lastAbort()
        _state.value = if (abort != null) {
            _state.value.copy(phase = PosPhase.ERROR, error = abort.reason, remediation = abort.remediation)
        } else {
            val serial = steps.filterIsInstance<AvdStep.Line>()
                .firstNotNullOfOrNull { Regex("emulator-\\d+").find(it.text)?.value }
            val identity = steps.filterIsInstance<AvdStep.Line>()
                .lastOrNull { it.text.contains("identity properties match") }
                ?.let { Regex("(\\d+) of (\\d+)").find(it.text) }
            _state.value.copy(
                phase = PosPhase.READY,
                serial = serial,
                identityMatched = identity?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                identityTotal = identity?.groupValues?.get(2)?.toIntOrNull() ?: 0,
            )
        }
    }

    fun powerOff() {
        val serial = _state.value.serial
        val sdkNow = sdk
        scope?.launch {
            if (serial != null && sdkNow != null) {
                runCatching { AdbClient(sdkNow, serial).emuKill() }
            }
            ProcessRegistry.stopAll(graceMs = 5_000)
            _state.value = PosDeviceState(phase = PosPhase.IDLE)
        }
    }

    /** Cancels in-flight work and stops the emulator. Called when the session closes. */
    fun stop() {
        job?.cancel()
        val serial = _state.value.serial
        val sdkNow = sdk
        if (serial != null && sdkNow != null) {
            runCatching { runBlocking { AdbClient(sdkNow, serial).emuKill() } }
        }
        ProcessRegistry.stopAll(graceMs = 3_000)
        scope?.cancel()
        scope = null
        job = null
        _state.value = PosDeviceState(phase = PosPhase.IDLE)
    }

    // ---- internals ----

    private fun launchExclusive(block: suspend (AndroidSdk, ResolvedTerminal) -> Unit) {
        if (_state.value.busy) return
        val sdkNow = sdk ?: run {
            _state.value = _state.value.copy(
                phase = PosPhase.ERROR,
                error = "No Android SDK found.",
                remediation = "Set ANDROID_HOME, or choose the SDK folder in Settings.",
            )
            return
        }
        val terminal = resolved ?: run {
            _state.value = _state.value.copy(
                phase = PosPhase.ERROR,
                error = "Unknown terminal \"${config.terminalProfileId}\".",
                remediation = "Pick a model on the Device tab.",
            )
            return
        }
        val active = scope ?: CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scope = it }
        steps.clear()
        job = active.launch { runCatching { block(sdkNow, terminal) } }
    }

    private fun record(step: AvdStep) {
        steps += step
    }

    private fun lastAbort(): AvdStep.Aborted? = steps.filterIsInstance<AvdStep.Aborted>().lastOrNull()
}

data class PosDeviceState(
    val phase: PosPhase = PosPhase.IDLE,
    val serial: String? = null,
    val identityMatched: Int = 0,
    val identityTotal: Int = 0,
    val error: String? = null,
    val remediation: String? = null,
) {
    val busy: Boolean get() = phase == PosPhase.PREPARING || phase == PosPhase.BOOTING
    val running: Boolean get() = phase == PosPhase.READY
}

enum class PosPhase(val label: String) {
    IDLE("Not running"),
    PREPARING("Preparing AVD"),
    PREPARED("AVD ready"),
    BOOTING("Booting"),
    READY("Running"),
    ERROR("Failed"),
}
