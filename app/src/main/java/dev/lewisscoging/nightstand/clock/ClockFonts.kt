package dev.lewisscoging.nightstand.clock

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.lewisscoging.nightstand.R
import dev.lewisscoging.nightstand.settings.ClockFont

private val digitalFontFamily = FontFamily(Font(R.font.clock_digital))
private val bubbleFontFamily = FontFamily(Font(R.font.clock_bubble))

/** The bundled family for a clock font choice; SYSTEM uses the platform default. */
fun ClockFont.toFontFamily(): FontFamily = when (this) {
    ClockFont.SYSTEM -> FontFamily.Default
    ClockFont.DIGITAL -> digitalFontFamily
    ClockFont.BUBBLE -> bubbleFontFamily
}
