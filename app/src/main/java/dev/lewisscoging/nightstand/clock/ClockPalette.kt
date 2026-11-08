package dev.lewisscoging.nightstand.clock

import dev.lewisscoging.nightstand.settings.BackgroundColor
import dev.lewisscoging.nightstand.settings.CLOCK_BRIGHTNESS_RANGE
import dev.lewisscoging.nightstand.settings.DEFAULT_CLOCK_BRIGHTNESS
import dev.lewisscoging.nightstand.settings.TextColor
import kotlin.math.roundToInt

const val TEXT_WHITE_ARGB = 0xFFFFFFFF.toInt()
const val TEXT_WARM_ARGB = 0xFFFFD8A8.toInt()
const val TEXT_RED_ARGB = 0xFFFF2A1A.toInt()
const val TEXT_AMBER_ARGB = 0xFFFFB000.toInt()
const val TEXT_GREEN_ARGB = 0xFF3DDC84.toInt()
const val TEXT_CYAN_ARGB = 0xFF00E5FF.toInt()

const val BACKGROUND_OFF_ARGB = 0xFF000000.toInt()
const val BACKGROUND_DARK_GREY_ARGB = 0xFF1C1C1E.toInt()
const val BACKGROUND_MIDNIGHT_BLUE_ARGB = 0xFF0B1033.toInt()
const val BACKGROUND_DEEP_RED_ARGB = 0xFF2A0505.toInt()

private const val CHANNEL_MASK = 0xFF
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BLUE_SHIFT = 0

/** The full-brightness ARGB value of a text colour choice. */
fun TextColor.argb(): Int = when (this) {
    TextColor.WHITE -> TEXT_WHITE_ARGB
    TextColor.WARM -> TEXT_WARM_ARGB
    TextColor.RED -> TEXT_RED_ARGB
    TextColor.AMBER -> TEXT_AMBER_ARGB
    TextColor.GREEN -> TEXT_GREEN_ARGB
    TextColor.CYAN -> TEXT_CYAN_ARGB
}

/** The full-brightness ARGB value of a background choice; OFF is true black. */
fun BackgroundColor.argb(): Int = when (this) {
    BackgroundColor.OFF -> BACKGROUND_OFF_ARGB
    BackgroundColor.DARK_GREY -> BACKGROUND_DARK_GREY_ARGB
    BackgroundColor.MIDNIGHT_BLUE -> BACKGROUND_MIDNIGHT_BLUE_ARGB
    BackgroundColor.DEEP_RED -> BACKGROUND_DEEP_RED_ARGB
}

/**
 * Multiplies the RGB channels of [argb] by [factor], clamped to CLOCK_BRIGHTNESS_RANGE
 * (NaN uses the default brightness), keeping alpha unchanged.
 */
fun scaleBrightness(argb: Int, factor: Float): Int {
    val clamped = if (factor.isNaN()) {
        DEFAULT_CLOCK_BRIGHTNESS
    } else {
        factor.coerceIn(CLOCK_BRIGHTNESS_RANGE)
    }
    val alpha = argb.channel(ALPHA_SHIFT)
    val red = (argb.channel(RED_SHIFT) * clamped).roundToInt()
    val green = (argb.channel(GREEN_SHIFT) * clamped).roundToInt()
    val blue = (argb.channel(BLUE_SHIFT) * clamped).roundToInt()
    return (alpha shl ALPHA_SHIFT) or
        (red shl RED_SHIFT) or
        (green shl GREEN_SHIFT) or
        (blue shl BLUE_SHIFT)
}

private fun Int.channel(shift: Int): Int = (this ushr shift) and CHANNEL_MASK
