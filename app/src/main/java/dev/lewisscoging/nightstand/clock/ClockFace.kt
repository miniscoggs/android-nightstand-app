package dev.lewisscoging.nightstand.clock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp

const val CLOCK_FADE_DURATION_MILLIS = 300
private val CLOCK_TEXT_SIZE = 96.sp
private val DATE_TEXT_SIZE = 24.sp

/**
 * Centred time with an optional date line in the given [style]; fades in and out and leaves
 * composition when hidden. The date line always uses the system font.
 */
@Composable
fun ClockFace(
    visible: Boolean,
    timeText: String,
    dateText: String?,
    style: ClockStyle,
    modifier: Modifier = Modifier
) {
    val textColor = Color(style.textArgb)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = CLOCK_FADE_DURATION_MILLIS)),
        exit = fadeOut(animationSpec = tween(durationMillis = CLOCK_FADE_DURATION_MILLIS))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(style.offset.x, style.offset.y) },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeText,
                color = textColor,
                fontSize = CLOCK_TEXT_SIZE,
                fontFamily = style.font.toFontFamily()
            )
            if (dateText != null) {
                Text(text = dateText, color = textColor, fontSize = DATE_TEXT_SIZE)
            }
        }
    }
}
