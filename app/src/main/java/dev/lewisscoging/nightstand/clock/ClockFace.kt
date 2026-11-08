package dev.lewisscoging.nightstand.clock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

const val CLOCK_FADE_DURATION_MILLIS = 300
private val CLOCK_TEXT_SIZE = 96.sp
private val DATE_TEXT_SIZE = 24.sp
private val CLOCK_TEXT_COLOR = Color.White

/** Centred time with an optional date line; fades in and out and leaves composition when hidden. */
@Composable
fun ClockFace(
    visible: Boolean,
    timeText: String,
    dateText: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = CLOCK_FADE_DURATION_MILLIS)),
        exit = fadeOut(animationSpec = tween(durationMillis = CLOCK_FADE_DURATION_MILLIS))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = timeText, color = CLOCK_TEXT_COLOR, fontSize = CLOCK_TEXT_SIZE)
            if (dateText != null) {
                Text(text = dateText, color = CLOCK_TEXT_COLOR, fontSize = DATE_TEXT_SIZE)
            }
        }
    }
}
