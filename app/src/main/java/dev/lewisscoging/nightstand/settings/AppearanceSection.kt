package dev.lewisscoging.nightstand.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.lewisscoging.nightstand.R
import dev.lewisscoging.nightstand.clock.argb
import dev.lewisscoging.nightstand.clock.toFontFamily
import dev.lewisscoging.nightstand.settings.components.FontChoice
import dev.lewisscoging.nightstand.settings.components.FontChoiceRow
import dev.lewisscoging.nightstand.settings.components.SectionHeader
import dev.lewisscoging.nightstand.settings.components.SegmentedChoiceRow
import dev.lewisscoging.nightstand.settings.components.SliderRow
import dev.lewisscoging.nightstand.settings.components.Swatch
import dev.lewisscoging.nightstand.settings.components.SwatchRow
import dev.lewisscoging.nightstand.settings.components.SwitchRow
import kotlin.math.roundToInt

const val CLOCK_BRIGHTNESS_STEP = 0.05f
private const val PERCENT = 100

/** Stops between the slider's ends, so every stop is a whole multiple of [CLOCK_BRIGHTNESS_STEP]. */
val CLOCK_BRIGHTNESS_SLIDER_STEPS =
    ((MAX_CLOCK_BRIGHTNESS - MIN_CLOCK_BRIGHTNESS) / CLOCK_BRIGHTNESS_STEP).roundToInt() - 1

/** A brightness factor as a whole percentage, eg 0.2 -> 20. */
fun brightnessPercent(brightness: Float): Int = (brightness * PERCENT).roundToInt()

fun LazyListScope.appearanceSection(settings: NightstandSettings, onUpdate: SettingsUpdate) {
    item { SectionHeader(title = stringResource(R.string.settings_appearance_header)) }
    item {
        val formats = HourFormat.entries
        SegmentedChoiceRow(
            title = stringResource(R.string.settings_hour_format_title),
            labels = formats.map { stringResource(it.labelRes()) },
            selectedIndex = formats.indexOf(settings.hourFormat),
            onSelect = { index -> onUpdate { it.copy(hourFormat = formats[index]) } }
        )
    }
    item {
        val fonts = ClockFont.entries
        FontChoiceRow(
            title = stringResource(R.string.settings_clock_font_title),
            choices = fonts.map { FontChoice(it.toFontFamily(), stringResource(it.labelRes())) },
            selectedIndex = fonts.indexOf(settings.clockFont),
            onSelect = { index -> onUpdate { it.copy(clockFont = fonts[index]) } }
        )
    }
    colourItems(settings = settings, onUpdate = onUpdate)
    item {
        ClockBrightnessRow(
            brightness = settings.clockBrightness,
            onBrightnessChange = { brightness ->
                onUpdate { it.copy(clockBrightness = brightness) }
            }
        )
    }
    item {
        SwitchRow(
            title = stringResource(R.string.settings_show_date_title),
            summary = stringResource(R.string.settings_show_date_summary),
            checked = settings.showDate,
            onCheckedChange = { checked -> onUpdate { it.copy(showDate = checked) } }
        )
    }
    item {
        SwitchRow(
            title = stringResource(R.string.settings_burn_in_shift_title),
            summary = stringResource(R.string.settings_burn_in_shift_summary),
            checked = settings.burnInShift,
            onCheckedChange = { checked -> onUpdate { it.copy(burnInShift = checked) } }
        )
    }
}

private fun LazyListScope.colourItems(settings: NightstandSettings, onUpdate: SettingsUpdate) {
    item {
        val colors = TextColor.entries
        SwatchRow(
            title = stringResource(R.string.settings_text_color_title),
            swatches = colors.map { Swatch(Color(it.argb()), stringResource(it.labelRes())) },
            selectedIndex = colors.indexOf(settings.textColor),
            onSelect = { index -> onUpdate { it.copy(textColor = colors[index]) } }
        )
    }
    item {
        val colors = BackgroundColor.entries
        SwatchRow(
            title = stringResource(R.string.settings_background_color_title),
            swatches = colors.map { Swatch(Color(it.argb()), stringResource(it.labelRes())) },
            selectedIndex = colors.indexOf(settings.backgroundColor),
            onSelect = { index -> onUpdate { it.copy(backgroundColor = colors[index]) } }
        )
    }
}

/** Tracks the drag locally and saves once it ends, so DataStore is not written on every frame. */
@Composable
private fun ClockBrightnessRow(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(brightness) { mutableFloatStateOf(brightness) }
    SliderRow(
        title = stringResource(R.string.settings_clock_brightness_title),
        valueLabel = stringResource(
            R.string.settings_clock_brightness_value,
            brightnessPercent(sliderValue)
        ),
        value = sliderValue,
        valueRange = CLOCK_BRIGHTNESS_RANGE,
        steps = CLOCK_BRIGHTNESS_SLIDER_STEPS,
        onValueChange = { sliderValue = it },
        onValueChangeFinished = { onBrightnessChange(sliderValue) },
        modifier = modifier
    )
}

@StringRes
private fun HourFormat.labelRes(): Int = when (this) {
    HourFormat.SYSTEM -> R.string.settings_hour_format_system
    HourFormat.H12 -> R.string.settings_hour_format_12
    HourFormat.H24 -> R.string.settings_hour_format_24
}

@StringRes
private fun ClockFont.labelRes(): Int = when (this) {
    ClockFont.SYSTEM -> R.string.settings_clock_font_system
    ClockFont.DIGITAL -> R.string.settings_clock_font_digital
    ClockFont.BUBBLE -> R.string.settings_clock_font_bubble
}

@StringRes
private fun TextColor.labelRes(): Int = when (this) {
    TextColor.WHITE -> R.string.settings_text_color_white
    TextColor.WARM -> R.string.settings_text_color_warm
    TextColor.RED -> R.string.settings_text_color_red
    TextColor.AMBER -> R.string.settings_text_color_amber
    TextColor.GREEN -> R.string.settings_text_color_green
    TextColor.CYAN -> R.string.settings_text_color_cyan
}

@StringRes
private fun BackgroundColor.labelRes(): Int = when (this) {
    BackgroundColor.OFF -> R.string.settings_background_color_off
    BackgroundColor.DARK_GREY -> R.string.settings_background_color_dark_grey
    BackgroundColor.MIDNIGHT_BLUE -> R.string.settings_background_color_midnight_blue
    BackgroundColor.DEEP_RED -> R.string.settings_background_color_deep_red
}
