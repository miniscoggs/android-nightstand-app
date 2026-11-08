package dev.lewisscoging.nightstand.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockBrightnessSliderTest {
    @Test
    fun sliderStepsLandOnEveryFivePercent() {
        assertEquals(18, CLOCK_BRIGHTNESS_SLIDER_STEPS)
        val stops = CLOCK_BRIGHTNESS_SLIDER_STEPS + 2
        assertEquals(
            MAX_CLOCK_BRIGHTNESS - MIN_CLOCK_BRIGHTNESS,
            (stops - 1) * CLOCK_BRIGHTNESS_STEP,
            1e-6f
        )
    }

    @Test
    fun brightnessIsShownAsAWholePercentage() {
        assertEquals(5, brightnessPercent(MIN_CLOCK_BRIGHTNESS))
        assertEquals(20, brightnessPercent(DEFAULT_CLOCK_BRIGHTNESS))
        assertEquals(100, brightnessPercent(MAX_CLOCK_BRIGHTNESS))
        assertEquals(35, brightnessPercent(0.35000002f))
    }
}
