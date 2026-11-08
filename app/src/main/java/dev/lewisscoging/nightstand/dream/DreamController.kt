package dev.lewisscoging.nightstand.dream

import dev.lewisscoging.nightstand.clock.formatClock
import dev.lewisscoging.nightstand.clock.formatDate
import dev.lewisscoging.nightstand.clock.resolveUse24Hour
import dev.lewisscoging.nightstand.core.NowProvider
import dev.lewisscoging.nightstand.settings.NightstandSettings
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.util.Locale
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

private const val MILLIS_PER_SECOND = 1_000L

/** Delay from [now] to the next whole second: always in 1..1000 ms. */
internal fun millisUntilNextSecond(now: ZonedDateTime): Long =
    MILLIS_PER_SECOND - now.get(ChronoField.MILLI_OF_SECOND)

/**
 * Framework-free dream logic: shows the clock on tap, ticks it each second while visible and hides
 * it after the configured timeout.
 */
class DreamController(
    private val scope: CoroutineScope,
    settings: Flow<NightstandSettings>,
    private val nowProvider: NowProvider,
    private val systemIs24Hour: () -> Boolean,
    private val datePattern: () -> String
) {
    private val latestSettings = settings.stateIn(
        scope,
        SharingStarted.Eagerly,
        NightstandSettings()
    )
    private val mutableUiState = MutableStateFlow(DreamUiState())
    private var hideJob: Job? = null
    private var tickJob: Job? = null

    val uiState: StateFlow<DreamUiState> = mutableUiState.asStateFlow()

    fun onTap() {
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
            delay(latestSettings.value.clockTimeoutSeconds.seconds)
            tickJob?.cancel()
            tickJob = null
            mutableUiState.update { state -> state.copy(clockVisible = false) }
        }
    }

    private fun showTime(now: ZonedDateTime) {
        val currentSettings = latestSettings.value
        val use24Hour = resolveUse24Hour(currentSettings.use24Hour, systemIs24Hour())
        val timeText = formatClock(now.toLocalTime(), use24Hour)
        val dateText = if (currentSettings.showDate) {
            formatDate(now.toLocalDate(), datePattern(), Locale.getDefault())
        } else {
            null
        }
        mutableUiState.update { state ->
            state.copy(clockVisible = true, timeText = timeText, dateText = dateText)
        }
    }
}
