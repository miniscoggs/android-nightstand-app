package dev.lewisscoging.nightstand.clock

import dev.lewisscoging.nightstand.core.BurnInOffset
import dev.lewisscoging.nightstand.settings.ClockFont
import dev.lewisscoging.nightstand.settings.NightstandSettings

val NO_BURN_IN_OFFSET = BurnInOffset(x = 0, y = 0)

/** How the dream draws the clock: colours are ARGB after brightness scaling, offset is in px. */
data class ClockStyle(
    val font: ClockFont,
    val textArgb: Int,
    val backgroundArgb: Int,
    val offset: BurnInOffset
)

/** The clock style for these settings; [offset] is replaced by zero when burn-in shift is off. */
fun NightstandSettings.toClockStyle(offset: BurnInOffset): ClockStyle = ClockStyle(
    font = clockFont,
    textArgb = scaleBrightness(textColor.argb(), clockBrightness),
    backgroundArgb = scaleBrightness(backgroundColor.argb(), clockBrightness),
    offset = if (burnInShift) offset else NO_BURN_IN_OFFSET
)
