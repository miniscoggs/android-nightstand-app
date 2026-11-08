package dev.lewisscoging.nightstand.dream

import dev.lewisscoging.nightstand.clock.formatClock
import dev.lewisscoging.nightstand.clock.formatDate
import dev.lewisscoging.nightstand.clock.resolveUse24Hour
import dev.lewisscoging.nightstand.clock.toClockStyle
import dev.lewisscoging.nightstand.core.BurnInOffset
import dev.lewisscoging.nightstand.core.NowProvider
import dev.lewisscoging.nightstand.core.burnInOffset
import dev.lewisscoging.nightstand.core.isWithinWindow
import dev.lewisscoging.nightstand.core.toMinutesOfDay
import dev.lewisscoging.nightstand.settings.NightstandSettings
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MILLIS_PER_SECOND = 1_000L
private const val MILLIS_PER_MINUTE = 60_000L
private const val SECONDS_PER_MINUTE = 60L

/** Delay from [now] to the next whole second: always in 1..1000 ms. */
internal fun millisUntilNextSecond(now: ZonedDateTime): Long =
    MILLIS_PER_SECOND - now.get(ChronoField.MILLI_OF_SECOND)

/** Delay from [now] to the next whole minute: always in 1..60000 ms. */
internal fun millisUntilNextMinute(now: ZonedDateTime): Long =
    MILLIS_PER_MINUTE - now.second * MILLIS_PER_SECOND - now.get(ChronoField.MILLI_OF_SECOND)

/**
 * Framework-free dream logic: shows the clock on tap while every gate is open, ticks it each
 * second while visible and hides it after the configured timeout or as soon as a gate closes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DreamController(
    private val scope: CoroutineScope,
    settings: Flow<NightstandSettings>,
    private val nowProvider: NowProvider,
    private val systemIs24Hour: () -> Boolean,
    private val datePattern: () -> String,
    orientation: Flow<Boolean>
) {
    private val latestSettings: StateFlow<NightstandSettings?> =
        settings.stateIn(scope, SharingStarted.Eagerly, null)
    private val loadedSettings: Flow<NightstandSettings> = latestSettings.filterNotNull()
    private val mutableUiState = MutableStateFlow(DreamUiState())
    private var hideJob: Job? = null
    private var tickJob: Job? = null

    val uiState: StateFlow<DreamUiState> = mutableUiState.asStateFlow()

    private val minuteTicks: Flow<ZonedDateTime> = flow {
        while (true) {
            val now = nowProvider.now()
            emit(now)
            delay(millisUntilNextMinute(now))
        }
    }

    private val withinWindow: Flow<Boolean> =
        combine(loadedSettings, minuteTicks) { currentSettings, now ->
            isWithinWindow(
                now.toLocalTime().toMinutesOfDay(),
                currentSettings.startTime,
                currentSettings.endTime
            )
        }

    private val orientationOk: Flow<Boolean> = loadedSettings
        .map { it.requireLandscape }
        .distinctUntilChanged()
        .flatMapLatest { requireLandscape -> if (requireLandscape) orientation else flowOf(true) }

    val tapsAllowed: StateFlow<Boolean> =
        combine(loadedSettings, withinWindow, orientationOk) {
                currentSettings,
                inWindow,
                oriented
            ->
            currentSettings.enabled && inWindow && oriented
        }.stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch {
            tapsAllowed.collect { allowed -> if (!allowed) hideClock() }
        }
        scope.launch {
            loadedSettings.collect { currentSettings ->
                mutableUiState.update { state ->
                    state.copy(style = currentSettings.toClockStyle(state.style.offset))
                }
            }
        }
    }

    fun onTap() {
        if (!tapsAllowed.value) return
        val now = nowProvider.now()
        showTime(now)
        tickJob?.cancel()
        tickJob = scope.launch {
            var lastTick = now
            while (true) {
                delay(millisUntilNextSecond(lastTick))
                lastTick = nowProvider.now()
                showTime(lastTick)
            }
        }
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(currentSettings().clockTimeoutSeconds.seconds)
            hideClock()
        }
    }

    private fun burnInOffsetAt(now: ZonedDateTime): BurnInOffset =
        burnInOffset(Math.floorDiv(now.toEpochSecond(), SECONDS_PER_MINUTE))

    private fun currentSettings(): NightstandSettings = latestSettings.value ?: NightstandSettings()

    private fun hideClock() {
        hideJob?.cancel()
        hideJob = null
        tickJob?.cancel()
        tickJob = null
        mutableUiState.update { state -> state.copy(clockVisible = false) }
    }

    private fun showTime(now: ZonedDateTime) {
        val currentSettings = currentSettings()
        val use24Hour = resolveUse24Hour(currentSettings.use24Hour, systemIs24Hour())
        val timeText = formatClock(now.toLocalTime(), use24Hour)
        val dateText = if (currentSettings.showDate) {
            formatDate(now.toLocalDate(), datePattern(), Locale.getDefault())
        } else {
            null
        }
        mutableUiState.update { state ->
            state.copy(
                clockVisible = true,
                timeText = timeText,
                dateText = dateText,
                style = currentSettings.toClockStyle(burnInOffsetAt(now))
            )
        }
    }
}
