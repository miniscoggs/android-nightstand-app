package dev.lewisscoging.nightstand.settings

import dev.lewisscoging.nightstand.core.LAST_MINUTE_OF_DAY
import dev.lewisscoging.nightstand.core.MINUTES_PER_HOUR

const val DEFAULT_ENABLED = true
const val DEFAULT_START_MINUTES = 22 * MINUTES_PER_HOUR
const val DEFAULT_END_MINUTES = 7 * MINUTES_PER_HOUR
const val DEFAULT_CLOCK_TIMEOUT_SECONDS = 15
const val DEFAULT_REQUIRE_LANDSCAPE = true
const val DEFAULT_CLOCK_BRIGHTNESS = 0.2f
const val DEFAULT_SHOW_DATE = false
const val DEFAULT_BURN_IN_SHIFT = true

val DEFAULT_HOUR_FORMAT = HourFormat.SYSTEM
val DEFAULT_CLOCK_FONT = ClockFont.SYSTEM
val DEFAULT_TEXT_COLOR = TextColor.WHITE
val DEFAULT_BACKGROUND_COLOR = BackgroundColor.OFF

const val MIN_CLOCK_TIMEOUT_SECONDS = 5
const val MAX_CLOCK_TIMEOUT_SECONDS = 120
const val MIN_CLOCK_BRIGHTNESS = 0.05f
const val MAX_CLOCK_BRIGHTNESS = 1.0f

val CLOCK_TIMEOUT_RANGE = MIN_CLOCK_TIMEOUT_SECONDS..MAX_CLOCK_TIMEOUT_SECONDS
val CLOCK_BRIGHTNESS_RANGE = MIN_CLOCK_BRIGHTNESS..MAX_CLOCK_BRIGHTNESS
val MINUTE_OF_DAY_RANGE = 0..LAST_MINUTE_OF_DAY

enum class HourFormat { SYSTEM, H12, H24 }

enum class ClockFont { SYSTEM, DIGITAL, BUBBLE }

enum class TextColor { WHITE, WARM, RED, AMBER, GREEN, CYAN }

enum class BackgroundColor { OFF, DARK_GREY, MIDNIGHT_BLUE, DEEP_RED }

/** Every user setting; times are minutes since midnight. */
data class NightstandSettings(
    val enabled: Boolean = DEFAULT_ENABLED,
    val startTime: Int = DEFAULT_START_MINUTES,
    val endTime: Int = DEFAULT_END_MINUTES,
    val clockTimeoutSeconds: Int = DEFAULT_CLOCK_TIMEOUT_SECONDS,
    val requireLandscape: Boolean = DEFAULT_REQUIRE_LANDSCAPE,
    val hourFormat: HourFormat = DEFAULT_HOUR_FORMAT,
    val clockFont: ClockFont = DEFAULT_CLOCK_FONT,
    val textColor: TextColor = DEFAULT_TEXT_COLOR,
    val backgroundColor: BackgroundColor = DEFAULT_BACKGROUND_COLOR,
    val clockBrightness: Float = DEFAULT_CLOCK_BRIGHTNESS,
    val showDate: Boolean = DEFAULT_SHOW_DATE,
    val burnInShift: Boolean = DEFAULT_BURN_IN_SHIFT
) {
    /** A copy with every ranged value clamped into its allowed range. */
    fun sanitised(): NightstandSettings = copy(
        startTime = startTime.coerceIn(MINUTE_OF_DAY_RANGE),
        endTime = endTime.coerceIn(MINUTE_OF_DAY_RANGE),
        clockTimeoutSeconds = clockTimeoutSeconds.coerceIn(CLOCK_TIMEOUT_RANGE),
        clockBrightness = if (clockBrightness.isNaN()) {
            DEFAULT_CLOCK_BRIGHTNESS
        } else {
            clockBrightness.coerceIn(CLOCK_BRIGHTNESS_RANGE)
        }
    )
}
