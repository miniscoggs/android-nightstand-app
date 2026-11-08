package dev.lewisscoging.nightstand.settings

import android.text.format.DateFormat
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.lewisscoging.nightstand.R
import dev.lewisscoging.nightstand.settings.components.SectionHeader
import dev.lewisscoging.nightstand.settings.components.SliderRow
import dev.lewisscoging.nightstand.settings.components.SwitchRow
import dev.lewisscoging.nightstand.settings.components.TimeRow
import kotlin.math.roundToInt

const val CLOCK_TIMEOUT_STEP_SECONDS = 5

/** Stops between the slider's ends, so every stop is a whole multiple of [CLOCK_TIMEOUT_STEP_SECONDS]. */
const val CLOCK_TIMEOUT_SLIDER_STEPS =
    (MAX_CLOCK_TIMEOUT_SECONDS - MIN_CLOCK_TIMEOUT_SECONDS) / CLOCK_TIMEOUT_STEP_SECONDS - 1

private val clockTimeoutSliderRange =
    MIN_CLOCK_TIMEOUT_SECONDS.toFloat()..MAX_CLOCK_TIMEOUT_SECONDS.toFloat()

fun LazyListScope.behaviourSection(settings: NightstandSettings, actions: SettingsActions) {
    item { SectionHeader(title = stringResource(R.string.settings_behaviour_header)) }
    item {
        TimeRow(
            title = stringResource(R.string.settings_start_time_title),
            minutesOfDay = settings.startTime,
            is24Hour = systemIs24Hour(),
            onTimeChange = actions::setStartTime
        )
    }
    item {
        TimeRow(
            title = stringResource(R.string.settings_end_time_title),
            minutesOfDay = settings.endTime,
            is24Hour = systemIs24Hour(),
            onTimeChange = actions::setEndTime
        )
    }
    item {
        ClockTimeoutRow(
            seconds = settings.clockTimeoutSeconds,
            onSecondsChange = actions::setClockTimeoutSeconds
        )
    }
    item {
        SwitchRow(
            title = stringResource(R.string.settings_require_landscape_title),
            summary = stringResource(R.string.settings_require_landscape_summary),
            checked = settings.requireLandscape,
            onCheckedChange = actions::setRequireLandscape
        )
    }
}

@Composable
private fun systemIs24Hour(): Boolean = DateFormat.is24HourFormat(LocalContext.current)

/** Tracks the drag locally and saves once it ends, so DataStore is not written on every frame. */
@Composable
private fun ClockTimeoutRow(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(seconds) { mutableFloatStateOf(seconds.toFloat()) }
    val shownSeconds = sliderValue.roundToInt()
    SliderRow(
        title = stringResource(R.string.settings_clock_timeout_title),
        valueLabel = pluralStringResource(
            R.plurals.settings_clock_timeout_value,
            shownSeconds,
            shownSeconds
        ),
        value = sliderValue,
        valueRange = clockTimeoutSliderRange,
        steps = CLOCK_TIMEOUT_SLIDER_STEPS,
        onValueChange = { sliderValue = it },
        onValueChangeFinished = { onSecondsChange(sliderValue.roundToInt()) },
        modifier = modifier
    )
}
