package dev.lewisscoging.nightstand.dream

import dev.lewisscoging.nightstand.clock.formatClock
import dev.lewisscoging.nightstand.clock.resolveUse24Hour
import dev.lewisscoging.nightstand.core.NowProvider
import dev.lewisscoging.nightstand.settings.NightstandSettings
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Framework-free dream logic: shows the clock on tap and hides it after the configured timeout. */
class DreamController(
    private val scope: CoroutineScope,
    settings: Flow<NightstandSettings>,
    private val nowProvider: NowProvider,
    private val systemIs24Hour: () -> Boolean
) {
    private val latestSettings = settings.stateIn(
        scope,
        SharingStarted.Eagerly,
        NightstandSettings()
    )
    private val mutableUiState = MutableStateFlow(DreamUiState())
    private var hideJob: Job? = null

    val uiState: StateFlow<DreamUiState> = mutableUiState.asStateFlow()

    fun onTap() {
        val currentSettings = latestSettings.value
        val use24Hour = resolveUse24Hour(currentSettings.use24Hour, systemIs24Hour())
        val timeText = formatClock(nowProvider.now().toLocalTime(), use24Hour)
        mutableUiState.update { state -> state.copy(clockVisible = true, timeText = timeText) }
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(currentSettings.clockTimeoutSeconds.seconds)
            mutableUiState.update { state -> state.copy(clockVisible = false) }
        }
    }
}
